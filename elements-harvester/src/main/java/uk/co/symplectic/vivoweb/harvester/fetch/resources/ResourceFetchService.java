/*
 * ******************************************************************************
 *   Copyright (c) 2019 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.fetch.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.utils.ImageUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemFileStore;
import java.net.MalformedURLException;

/**
 * Public interface to the Resource Fetch service, which acts to make asynchronous calls to retrieve secondary resources
 * from an External API. Not really expecting to handle XML data here.
 * Wraps the static implementation in an object, so that it can be mocked / substituted.
 */
@SuppressWarnings("unused")
public final class ResourceFetchService {
    private static Logger log = LoggerFactory.getLogger(ResourceFetchService.class);

    public ResourceFetchService() {

    }

    public void fetchUserPhoto(ElementsAPI api, ImageUtils.PhotoType photoType, ElementsUserInfo userInfo, ElementsItemFileStore objectStore) throws MalformedURLException {
        ResourceFetchServiceImpl.fetchUserPhoto(api, photoType, userInfo, objectStore);
    }

//    public void fetchExternal(String url, File outputFile) throws MalformedURLException {
//        ResourceFetchServiceImpl.fetchExternal(url, outputFile);
//    }

    public static void awaitShutdown() {
        ResourceFetchServiceImpl.awaitShutdown();
    }
}
