/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.machine.shared.dto.recipe;

import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author Eugene Voevodin
 */
@DTO
public interface GroupDescriptor extends Group {

    String getName();

    void setName(String name);

    GroupDescriptor withName(String name);

    String getUnit();

    void setUnit(String unit);

    GroupDescriptor withUnit(String unit);

    List<String> getAcl();

    void setAcl(List<String> acl);

    GroupDescriptor withAcl(List<String> acl);
}
