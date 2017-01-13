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
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemFileStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public final class ResourceFetchServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(ResourceFetchServiceImpl.class);

    private static final ExecutorServiceUtils.ExecutorServiceWrapper<Boolean> wrapper = ExecutorServiceUtils.newFixedThreadPool("ResourceFetchService");

    private ResourceFetchServiceImpl() {}

    static void fetchUserPhoto(ElementsAPI api, ElementsUserInfo userInfo, ElementsItemFileStore objectStore) throws MalformedURLException {
        wrapper.submit(new UserPhotoFetchTask(api, userInfo, objectStore));
    }

//    static void fetchExternal(String url, File outputFile) throws MalformedURLException {
//        wrapper.submit(new ExternalFetchTask(new URL(url), outputFile));
//    }

    static void awaitShutdown() {
        wrapper.awaitShutdown();
    }

    private static class UserPhotoFetchTask implements Callable<Boolean> {
        private ElementsAPI api;
        ElementsUserInfo userInfo;
        private ElementsItemFileStore objectStore;

        UserPhotoFetchTask(ElementsAPI api, ElementsUserInfo userInfo, ElementsItemFileStore objectStore) {
            if(api == null) throw new NullArgumentException("api");
            if(userInfo == null) throw new NullArgumentException("userInfo");
            if(objectStore == null) throw new NullArgumentException("objectStore");

            this.api = api;
            this.userInfo = userInfo;
            this.objectStore = objectStore;
        }

        @Override
        public Boolean call() throws Exception {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Boolean retCode = api.fetchResource(userInfo.getPhotoUrl(), os);
            objectStore.storeItem(userInfo, StorableResourceType.RAW_USER_PHOTO, os.toByteArray());
            //TODO: better error handling here?
            return retCode;
        }
    }

//    private static class ExternalFetchTask implements Callable<Boolean> {
//        private URL url;
//        private File outputFile;
//
//        ExternalFetchTask(URL url, File outputFile) {
//            this.url = url;
//            this.outputFile = outputFile;
//        }
//
//        @Override
//        public Boolean call() throws Exception {
//            // Not implemented yet
//            return Boolean.TRUE;
//        }
//    }

}
