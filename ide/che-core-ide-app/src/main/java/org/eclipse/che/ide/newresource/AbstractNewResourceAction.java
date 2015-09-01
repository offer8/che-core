/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.newresource;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.json.JsonHelper;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.project.node.ItemReferenceBasedNode;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.eclipse.che.ide.ui.dialogs.input.InputValidator;
import org.eclipse.che.ide.util.NameUtils;
import org.eclipse.che.ide.util.loging.Log;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

import static org.eclipse.che.ide.api.event.ItemEvent.ItemOperation.CREATED;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Implementation of an {@link Action} that provides an ability to create new resource (e.g. file, folder).
 * After performing this action, it asks user for the resource's name
 * and then creates resource in the selected folder.
 *
 * @author Artem Zatsarynnyy
 * @author Dmitry Shnurenko
 */
public abstract class AbstractNewResourceAction extends AbstractPerspectiveAction {
    protected final InputValidator           fileNameValidator;
    protected final InputValidator           folderNameValidator;
    protected final String                   title;
    protected       SelectionAgent           selectionAgent;
    protected       EditorAgent              editorAgent;
    protected       ProjectServiceClient     projectServiceClient;
    protected       EventBus                 eventBus;
    protected       AppContext               appContext;
    protected       AnalyticsEventLogger     eventLogger;
    protected       DtoUnmarshallerFactory   dtoUnmarshallerFactory;
    protected       DialogFactory            dialogFactory;
    protected       CoreLocalizationConstant coreLocalizationConstant;

    /**
     * Creates new action.
     *
     * @param title
     *         action's title
     * @param description
     *         action's description
     * @param svgIcon
     *         action's SVG icon
     */
    public AbstractNewResourceAction(String title, String description, @Nullable SVGResource svgIcon) {
        super(Arrays.asList(PROJECT_PERSPECTIVE_ID), title, description, null, svgIcon);
        fileNameValidator = new FileNameValidator();
        folderNameValidator = new FolderNameValidator();
        this.title = title;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (eventLogger != null) {
            eventLogger.log(this);
        }

        InputDialog inputDialog = dialogFactory.createInputDialog(
                coreLocalizationConstant.newResourceTitle(title),
                coreLocalizationConstant.newResourceLabel(title.toLowerCase()),
                new InputCallback() {
                    @Override
                    public void accepted(String value) {
                        onAccepted(value);
                    }
                }, null).withValidator(fileNameValidator);
        inputDialog.show();
    }

    private void onAccepted(String value) {
        final String name = getExtension().isEmpty() ? value : value + '.' + getExtension();
        final ResourceBasedNode<?> parent = getResourceBasedNode();

        if (parent == null) {
            throw new IllegalStateException("Invalid parent node.");
        }

        projectServiceClient.createFile(((HasStorablePath)parent).getStorablePath(),
                                        name,
                                        getDefaultContent(),
                                        getMimeType(),
                                        createCallback(parent));
    }

    protected AsyncRequestCallback<ItemReference> createCallback(final ResourceBasedNode<?> parent) {
        return new AsyncRequestCallback<ItemReference>(dtoUnmarshallerFactory.newUnmarshaller(ItemReference.class)) {
            @Override
            protected void onSuccess(ItemReference itemReference) {
                projectExplorer.reloadChildren(parent, itemReference, "file".equals(itemReference.getType()));
            }

            @Override
            protected void onFailure(Throwable exception) {
                dialogFactory.createMessageDialog("", JsonHelper.parseJsonMessage(exception.getMessage()), null).show();
            }
        };
    }

    protected void getCreatedItem(ResourceBasedNode<?> parent, ItemReference item) {
        nodeManager.getChildren(((HasStorablePath)parent).getStorablePath(),
                                parent.getProjectDescriptor(),
                                parent.getSettings())
                   .then(iterateAndFindCreatedNode(item))
                   .then(fireNodeCreated(parent));
    }

    @Nonnull
    protected Function<List<Node>, ItemReferenceBasedNode> iterateAndFindCreatedNode(@Nonnull final ItemReference itemReference) {
        return new Function<List<Node>, ItemReferenceBasedNode>() {
            @Override
            public ItemReferenceBasedNode apply(List<Node> nodes) throws FunctionException {
                if (nodes.isEmpty()) {
                    return null;
                }

                for (Node node : nodes) {
                    if (node instanceof FileReferenceNode && ((FileReferenceNode)node).getData().equals(itemReference)) {
                        return (FileReferenceNode)node;
                    }
                }

                return null;
            }
        };
    }

    @Nonnull
    protected Operation<ItemReferenceBasedNode> fireNodeCreated(@Nonnull final ResourceBasedNode<?> parent) {
        return new Operation<ItemReferenceBasedNode>() {
            @Override
            public void apply(final ItemReferenceBasedNode newItemReferenceNode) throws OperationException {
                if (newItemReferenceNode == null) {
                    return;
                }


                if (newItemReferenceNode instanceof FileReferenceNode) {
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            editorAgent.openEditor((FileReferenceNode)newItemReferenceNode);
                        }
                    });
                }
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@Nonnull ActionEvent event) {
        event.getPresentation().setEnabled(getNewResourceParent() != null);
    }

    /**
     * Returns extension (without dot) for a new resource.
     * By default, returns an empty string.
     */
    protected String getExtension() {
        return "";
    }

    /**
     * Returns default content for a new resource.
     * By default, returns an empty string.
     */
    protected String getDefaultContent() {
        return "";
    }

    /**
     * Returns MIME-type for a new resource.
     * By default, returns {@code null}.
     */
    protected String getMimeType() {
        return null;
    }

    /** Returns parent for creating new item or {@code null} if resource can not be created. */
    @Nullable
    protected ResourceBasedNode<?> getResourceBasedNode() {
        List<?> selection = projectExplorer.getSelection().getAllElements();
        //we should be sure that user selected single element to work with it
        if (selection != null && selection.isEmpty() || selection.size() > 1) {
            return null;
        }

        Object o = selection.get(0);

        if (o instanceof ResourceBasedNode<?>) {
            ResourceBasedNode<?> node = (ResourceBasedNode<?>)o;
            //it may be file node, so we should take parent node
            if (node.isLeaf() && isResourceAndStorableNode(node.getParent())) {
                return (ResourceBasedNode<?>)node.getParent();
            }

            return isResourceAndStorableNode(node) ? node : null;
        }

        return null;
    }

    protected boolean isResourceAndStorableNode(@Nullable Node node) {
        return node != null && node instanceof ResourceBasedNode<?> && node instanceof HasStorablePath;
    }

    @Inject
    private void init(NewProjectExplorerPresenter projectExplorer,
                      EditorAgent editorAgent,
                      ProjectServiceClient projectServiceClient,
                      EventBus eventBus,
                      AppContext appContext,
                      AnalyticsEventLogger eventLogger,
                      DtoUnmarshallerFactory dtoUnmarshallerFactory,
                      DialogFactory dialogFactory,
                      CoreLocalizationConstant coreLocalizationConstant,
                      NodeManager nodeManager) {
        this.projectExplorer = projectExplorer;
        this.editorAgent = editorAgent;
        this.projectServiceClient = projectServiceClient;
        this.eventBus = eventBus;
        this.appContext = appContext;
        this.eventLogger = eventLogger;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dialogFactory = dialogFactory;
        this.coreLocalizationConstant = coreLocalizationConstant;
        this.nodeManager = nodeManager;
    }

    private class FileNameValidator implements InputValidator {
        /** {@inheritDoc} */
        @Nullable
        @Override
        public Violation validate(String value) {
            if (!NameUtils.checkFileName(value)) {
                return new Violation() {
                    @Override
                    public String getMessage() {
                        return coreLocalizationConstant.invalidName();
                    }

                    @Nullable
                    @Override
                    public String getCorrectedValue() {
                        return null;
                    }
                };
            }
            return null;
        }
    }

    private class FolderNameValidator implements InputValidator {
        /** {@inheritDoc} */
        @Nullable
        @Override
        public Violation validate(String value) {
            if (!NameUtils.checkFolderName(value)) {
                return new Violation() {
                    @Override
                    public String getMessage() {
                        return coreLocalizationConstant.invalidName();
                    }

                    @Nullable
                    @Override
                    public String getCorrectedValue() {
                        return null;
                    }
                };
            }
            return null;
        }
    }
}
