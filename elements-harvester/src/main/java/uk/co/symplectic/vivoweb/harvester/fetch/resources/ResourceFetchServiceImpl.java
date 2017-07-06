/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.fetch.resources;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.utils.ExecutorServiceUtils;
import uk.co.symplectic.utils.ImageUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemFileStore;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

public final class ResourceFetchServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ResourceFetchServiceImpl.class);

    private static final ExecutorServiceUtils.ExecutorServiceWrapper<Boolean> wrapper = ExecutorServiceUtils.newFixedThreadPool("ResourceFetchService");

    private ResourceFetchServiceImpl() {}

    static void fetchUserPhoto(ElementsAPI api, ImageUtils.PhotoType photoType, ElementsUserInfo userInfo, ElementsItemFileStore objectStore) throws MalformedURLException {
        wrapper.submit(new UserPhotoFetchTask(api, photoType, userInfo, objectStore));
    }

//    static void fetchExternal(String url, File outputFile) throws MalformedURLException {
//        wrapper.submit(new ExternalFetchTask(new URL(url), outputFile));
//    }

    static void awaitShutdown() {
        wrapper.awaitShutdown();
    }

    private static class UserPhotoFetchTask implements Callable<Boolean> {
        private final ElementsAPI api;
        private final ElementsUserInfo userInfo;
        private final ElementsItemFileStore objectStore;
        private final ImageUtils.PhotoType type;

        UserPhotoFetchTask(ElementsAPI api, ImageUtils.PhotoType photoType, ElementsUserInfo userInfo, ElementsItemFileStore objectStore) {
            if(api == null) throw new NullArgumentException("api");
            if(userInfo == null) throw new NullArgumentException("userInfo");
            if(objectStore == null) throw new NullArgumentException("objectStore");

            this.api = api;
            this.userInfo = userInfo;
            this.objectStore = objectStore;
            this.type = photoType;
        }

        @Override
        public Boolean call() throws Exception {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            String url = userInfo.getPhotoUrl(type);
            Boolean retCode = api.fetchResource(url, os);
            byte[] data = os.toByteArray();
            if(data == null || data.length == 0)
                log.warn(MessageFormat.format("Failed to retrieve photo for {0} from url {1}", userInfo.getItemId(), url));
            else
                objectStore.storeItem(userInfo, StorableResourceType.RAW_USER_PHOTO, data);
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
