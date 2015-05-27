/*******************************************************************************
 * Copyright (c) 2014-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/

package org.eclipse.che.ide.jseditor.client.link;

import org.eclipse.che.ide.api.text.Position;

import java.util.List;

/**
 * @author Evgen Vidolob
 */
public interface LinkedModelGroup {

    void setData(LinkedModelData data);

    void setPositions(List<Position> positions);
}