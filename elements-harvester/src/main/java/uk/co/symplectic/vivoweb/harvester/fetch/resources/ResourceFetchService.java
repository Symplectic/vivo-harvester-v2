/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.fetch.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.utils.ImageUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemFileStore;

import java.io.File;
import java.net.MalformedURLException;

public final class ResourceFetchService {
    private static Logger log = LoggerFactory.getLogger(ResourceFetchService.class);

    public ResourceFetchService() {

    }

//    public void fetchUserPhoto(ElementsAPI api, ElementsUserInfo userInfo, ElementsItemFileStore objectStore) throws MalformedURLException {
//        fetchUserPhoto(api, ImageUtils.PhotoType., userInfo, objectStore);
//    }

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
