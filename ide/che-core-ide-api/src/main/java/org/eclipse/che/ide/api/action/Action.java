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
package org.eclipse.che.ide.api.action;


import org.eclipse.che.ide.util.StringUtils;
import com.google.gwt.resources.client.ImageResource;

import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Represents an entity that has a state, a presentation and can be performed.
 * <p/>
 * For an action to be useful, you need to implement {@link Action#actionPerformed}
 * and optionally to override {@link Action#update}. By overriding the
 * {@link Action#update} method you can dynamically change action's presentation.
 * <p/>
 * The same action can have various presentations.
 *
 * @author Evgen Vidolob
 * @author Dmitry Shnurenko
 * @author Vitaliy Guliy
 */
public abstract class Action {

    private final Presentation presentation = new Presentation();


    /** Creates a new action with its text, description and icon set to <code>null</code>. */
    public Action() {
        this(null, null, null, null);
    }

    /**
     * Creates a new action with <code>icon</code> provided. Its text, description set to <code>null</code>.
     *
     * @param icon
     *         Default icon to appear in toolbars and menus (Note some platform don't have icons in menu).
     */
    public Action(ImageResource icon) {
        this(null, null, icon, null);
    }

    /**
     * Creates a new action with the specified text. Description and icon are
     * set to <code>null</code>.
     *
     * @param text
     *         Serves as a tooltip when the presentation is a button and the name of the
     *         menu item when the presentation is a menu item.
     */
    public Action(String text) {
        this(text, null, null, null);
    }

    /**
     * Constructs a new action with the specified text, description.
     *
     * @param text
     *         Serves as a tooltip when the presentation is a button and the name of the
     *         menu item when the presentation is a menu item
     * @param description
     *         Describes current action, this description will appear on
     *         the status bar when presentation has focus
     */
    public Action(String text, String description) {
        this(text, description, null, null);
    }

    /**
     * Constructs a new action with the specified text, description and icon.
     *
     * @param text
     *         Serves as a tooltip when the presentation is a button and the name of the
     *         menu item when the presentation is a menu item
     * @param description
     *         Describes current action, this description will appear on
     *         the status bar when presentation has focus
     * @param icon
     *         Action's icon
     */
    public Action(String text, String description, ImageResource icon) {
        this(text, description, icon, null);
    }

    /**
     * Constructs a new action with the specified text, description and icon.
     *
     * @param text
     *         Serves as a tooltip when the presentation is a button and the name of the
     *         menu item when the presentation is a menu item
     * @param description
     *         Describes current action, this description will appear on
     *         the status bar when presentation has focus
     * @param icon
     *         Action's icon
     * @param svgIcon
     *         Action's SVG icon
     */
    public Action(String text, String description, ImageResource icon, SVGResource svgIcon) {
        presentation.setText(text);
        presentation.setDescription(description);
        presentation.setIcon(icon);
        presentation.setSVGIcon(svgIcon);
    }

    /**
     * Updates the state of the action. Default implementation does nothing.
     * Override this method to provide the ability to dynamically change action's
     * state and(or) presentation depending on the context (For example
     * when your action state depends on the selection you can check for
     * selection and change the state accordingly).
     * This method can be called frequently, for instance, if an action is added to a toolbar,
     * it will be updated twice a second. This means that this method is supposed to work really fast,
     * no real work should be done at this phase. For example, checking selection in a tree or a list,
     * is considered valid, but working with a file system is not. If you cannot understand the state of
     * the action fast you should do it in the {@link #actionPerformed(ActionEvent)} method and notify
     * the user that action cannot be executed if it's the case.
     *
     * @param e
     *         Carries information on the invocation place and data available
     */
    public void update(ActionEvent e) {
    }

    /**
     * Returns a template presentation that will be used
     * as a template for created presentations.
     *
     * @return template presentation
     */
    public final Presentation getTemplatePresentation() {
        return presentation;
    }

    /**
     * Implement this method to provide your action handler.
     *
     * @param e
     *         Carries information on the invocation place
     */
    public abstract void actionPerformed(ActionEvent e);

    @Override
    public String toString() {
        return getTemplatePresentation().toString();
    }

}
