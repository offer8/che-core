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
package org.eclipse.che.ide.part.explorer.project.synchronize;

import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.model.workspace.ProjectProblem;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.Constants;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.ConfigureProjectEvent;
import org.eclipse.che.ide.api.event.project.CurrentProjectChangedEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.wizard.Wizard;
import org.eclipse.che.ide.projectimport.wizard.ProjectImporter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class ProjectConfigSynchronizationListenerTest {

    private static final String WORKSPACE_ID = "wsId";
    private static final String PROJECT_NAME = "name";

    //constructor mocks
    @Mock
    private AppContext               appContext;
    @Mock
    private DialogFactory            dialogFactory;
    @Mock
    private ProjectImporter          projectImporter;
    @Mock
    private CoreLocalizationConstant locale;
    @Mock
    private ProjectServiceClient     projectService;
    @Mock
    private NotificationManager      notificationManager;
    @Mock
    private EventBus                 eventBus;
    @Mock
    private ChangeLocationWidget     changeLocationWidget;
    @Mock
    private CancelCallback           cancelCallback;
    @Mock
    private DtoUnmarshallerFactory   factory;

    //additional mocks
    @Mock
    private ProjectConfigDto           projectConfig;
    @Mock
    private CurrentProjectChangedEvent event;
    @Mock
    private ConfirmDialog              confirmDialog;
    @Mock
    private SourceStorageDto           sourceStorage;

    @Captor
    private ArgumentCaptor<ConfirmCallback>            confirmCaptor;
    @Captor
    private ArgumentCaptor<CancelCallback>             cancelCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Void>> deleteCaptor;

    private List<ProjectProblem> problems;

    private ProjectConfigSynchronizationListener listener;

    @Before
    public void setUp() {
        when(appContext.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(event.getProjectConfig()).thenReturn(projectConfig);

        when(dialogFactory.createConfirmDialog(anyString(),
                                               anyString(),
                                               anyString(),
                                               anyString(),
                                               Matchers.<ConfirmCallback>anyObject(),
                                               Matchers.<CancelCallback>anyObject())).thenReturn(confirmDialog);
        when(dialogFactory.createConfirmDialog(anyString(),
                                               eq(changeLocationWidget),
                                               Matchers.<ConfirmCallback>anyObject(),
                                               Matchers.<CancelCallback>anyObject())).thenReturn(confirmDialog);

        problems = new ArrayList<>();
        when(projectConfig.getProblems()).thenReturn(problems);
        when(projectConfig.getName()).thenReturn(PROJECT_NAME);
        when(projectConfig.getSource()).thenReturn(sourceStorage);

        listener = new ProjectConfigSynchronizationListener(projectImporter,
                                                            eventBus,
                                                            dialogFactory,
                                                            locale,
                                                            projectService,
                                                            appContext,
                                                            notificationManager,
                                                            changeLocationWidget,
                                                            factory);
    }

    @Test
    public void constructorShouldBeInitialized() {
        verify(appContext).getWorkspaceId();
        verify(eventBus).addHandler(CurrentProjectChangedEvent.TYPE, listener);
    }

    @Test
    public void nothingShouldHappenWhenProjectDoesNotHaveAnyProblem() {
        listener.onCurrentProjectChanged(event);

        verify(dialogFactory, never()).createConfirmDialog(anyString(),
                                                           anyString(),
                                                           anyString(),
                                                           anyString(),
                                                           Matchers.<ConfirmCallback>anyObject(),
                                                           Matchers.<CancelCallback>anyObject());
    }

    @Test
    public void projectExistInWSButAbsentOnVFSDialogShouldBeShown() {
        ProjectProblem problem = newDto(ProjectProblem.class).withCode(10);

        problems.add(problem);

        listener.onCurrentProjectChanged(event);

        verify(locale).synchronizeDialogTitle();
        verify(locale).existInWorkspaceDialogContent(PROJECT_NAME);
        verify(projectConfig).getName();
        verify(locale).buttonImport();
        verify(locale).buttonRemove();

        verify(dialogFactory).createConfirmDialog(anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  Matchers.<ConfirmCallback>anyObject(),
                                                  Matchers.<CancelCallback>anyObject());
        verify(confirmDialog).show();
    }

    @Test
    public void changeLocationDialogShouldBeShownIfProjectHasNoLocationOrLocationIsIncorrectAndProjectShouldBeImported() {
        when(sourceStorage.getLocation()).thenReturn(null);
        ProjectProblem problem = newDto(ProjectProblem.class).withCode(10);

        problems.add(problem);

        listener.onCurrentProjectChanged(event);

        verify(dialogFactory).createConfirmDialog(anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  confirmCaptor.capture(),
                                                  Matchers.<CancelCallback>anyObject());
        confirmCaptor.getValue().accepted();

        verify(locale).locationDialogTitle();
        verify(dialogFactory).createConfirmDialog(anyString(),
                                                  eq(changeLocationWidget),
                                                  confirmCaptor.capture(),
                                                  Matchers.<CancelCallback>anyObject());

        confirmCaptor.getValue().accepted();

        verify(changeLocationWidget).getText();
        verify(sourceStorage).setLocation(anyString());

        verify(projectImporter).checkFolderExistenceAndImport(Matchers.<Wizard.CompleteCallback>anyObject(), eq(projectConfig));
    }

    @Test
    public void projectExistOnVFSButAbsentInWSDialogShouldBeShown() {
        ProjectProblem problem = newDto(ProjectProblem.class).withCode(9);

        problems.add(problem);

        listener.onCurrentProjectChanged(event);

        verify(locale).synchronizeDialogTitle();
        verify(locale).existInFileSystemDialogContent(PROJECT_NAME);
        verify(locale).buttonConfigure();
        verify(locale).buttonRemove();

        verify(dialogFactory).createConfirmDialog(anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  confirmCaptor.capture(),
                                                  Matchers.<CancelCallback>anyObject());
        confirmCaptor.getValue().accepted();

        verify(eventBus).fireEvent(Matchers.<ConfigureProjectEvent>anyObject());
    }

    @Test
    public void projectShouldBeDeletedFromWorkspaceWhenWeRemoveId() {
        ProjectProblem problem = newDto(ProjectProblem.class).withCode(9);

        problems.add(problem);

        listener.onCurrentProjectChanged(event);

        verify(dialogFactory).createConfirmDialog(anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  confirmCaptor.capture(),
                                                  cancelCaptor.capture());
        cancelCaptor.getValue().cancelled();

        //noinspection unchecked
        verify(projectService).delete(eq(WORKSPACE_ID), anyString(), Matchers.<AsyncRequestCallback>anyObject());
    }

    @Test
    public void projectConfigurationChangedDialogShouldBeShown() {
        ProjectProblem problem = newDto(ProjectProblem.class).withCode(8);

        problems.add(problem);

        listener.onCurrentProjectChanged(event);

        verify(locale).synchronizeDialogTitle();
        verify(locale).projectConfigurationChanged();
        verify(locale).buttonConfigure();
        verify(locale).buttonKeepBlank();

        verify(dialogFactory).createConfirmDialog(anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  confirmCaptor.capture(),
                                                  Matchers.<CancelCallback>anyObject());
        confirmCaptor.getValue().accepted();

        verify(eventBus).fireEvent(Matchers.<ConfigureProjectEvent>anyObject());
    }

    @Test
    public void projectShouldBeUpdatedAsBlankWhenProjectConfigurationChanged() {
        ProjectProblem problem = newDto(ProjectProblem.class).withCode(8);

        problems.add(problem);

        listener.onCurrentProjectChanged(event);

        verify(dialogFactory).createConfirmDialog(anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  anyString(),
                                                  confirmCaptor.capture(),
                                                  cancelCaptor.capture());
        cancelCaptor.getValue().cancelled();

        verify(projectConfig).setType(Constants.BLANK_ID);

        //noinspection unchecked
        verify(projectService).updateProject(eq(WORKSPACE_ID), anyString(), eq(projectConfig), Matchers.<AsyncRequestCallback>anyObject());
    }
}