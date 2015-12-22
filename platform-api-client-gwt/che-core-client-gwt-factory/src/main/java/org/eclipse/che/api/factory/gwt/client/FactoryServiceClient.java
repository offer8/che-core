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
package org.eclipse.che.api.factory.gwt.client;

import org.eclipse.che.api.factory.server.FactoryService;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.util.Pair;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Client for IDE3 Factory service.
 *
 * @author Vladyslav Zhukovskii
 */
public interface FactoryServiceClient {
    /**
     * Get valid JSON factory object based on input factory ID
     *
     * @param factoryId
     *         factory ID to retrieve
     * @param validate
     *         indicates whether or not factory should be validated by accept validator
     * @param callback
     *         callback which return valid JSON object of factory or exception if occurred
     *
     */
    void getFactory(@NotNull String factoryId, boolean validate, @NotNull AsyncRequestCallback<Factory> callback);
    
    /**
     * @param factoryId Factory's id
     * @param type snippent's type (markdown, html, etc)
     * @param callback callback which returns snippet of the factory or exception if occurred
     */
    void getFactorySnippet(@NotNull String factoryId, @NotNull String type, @NotNull AsyncRequestCallback<String> callback);


    /**
     * Retrieves factory object prototype for given project with it's attributes. It's not the stored factory object.
     * @param workspaceId workspace id
     * @param path project path
     * @param callback callback which returns snippet of the factory or exception if occurred
     */
    void getFactoryJson(@NotNull String workspaceId, @NotNull String path, @NotNull AsyncRequestCallback<Factory> callback);

    /**
     * Save factory to storage.
     *
     * @see FactoryService#getFactoryJson(String, String)
     */
    Promise<Factory> getFactoryJson(@NotNull String workspaceId, @Nullable String path);

    /**
     * Save factory to storage.
     *
     * @see FactoryService#saveFactory(Factory)
     */
    Promise<Factory> saveFactory(@NotNull Factory factory);

    /**
     * Save factory to storage.
     *
     * @see FactoryService#getFactoryByAttribute(Integer, Integer, UriInfo)
     */
    Promise<List<Factory>> findFactory(Integer skipCount, Integer maxItems, List<Pair<String, String>> params);
}
