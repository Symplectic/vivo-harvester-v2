/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.fetch.resources;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.utils.ExecutorServiceUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsObjectFileStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public final class ResourceFetchServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(ResourceFetchServiceImpl.class);

    private static final ExecutorServiceUtils.ExecutorServiceWrapper wrapper = ExecutorServiceUtils.newFixedThreadPool("ResourceFetchService", Boolean.class);

    private ResourceFetchServiceImpl() {}

    static void fetchUserPhoto(ElementsAPI api, ElementsUserInfo userInfo, ElementsObjectFileStore objectStore) throws MalformedURLException {
        Future<Boolean> result = wrapper.submit(new UserPhotoFetchTask(api, userInfo, objectStore));
    }

    static void fetchExternal(String url, File outputFile) throws MalformedURLException {
        Future<Boolean> result = wrapper.submit(new ExternalFetchTask(new URL(url), outputFile));
    }

    static void shutdown() {
        wrapper.shutdown();
    }

    static class UserPhotoFetchTask implements Callable<Boolean> {
        private ElementsAPI api;
        ElementsUserInfo userInfo;
        private ElementsObjectFileStore objectStore;

        UserPhotoFetchTask(ElementsAPI api, ElementsUserInfo userInfo, ElementsObjectFileStore objectStore) {
            if(api == null) throw new NullArgumentException("api");
            if(userInfo == null) throw new NullArgumentException("userInfo");
            if(objectStore == null) throw new NullArgumentException("objectStore");

            this.api = api;
            this.userInfo = userInfo;
            this.objectStore = objectStore;
        }

        @Override
        public Boolean call() throws Exception {
            Boolean retCode = Boolean.TRUE;

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            retCode = api.fetchResource(userInfo.getPhotoUrl(), os);
            ElementsStoredItem storedItem = objectStore.storeItem(userInfo, StorableResourceType.RAW_USER_PHOTO, os.toByteArray());
            //TODO: better error handling here?
            return retCode;
        }
    }

    static class ExternalFetchTask implements Callable<Boolean> {
        private URL url;
        private File outputFile;

        ExternalFetchTask(URL url, File outputFile) {
            this.url = url;
            this.outputFile = outputFile;
        }

        @Override
        public Boolean call() throws Exception {
            // Not implemented yet
            return Boolean.TRUE;
        }
    }

}
