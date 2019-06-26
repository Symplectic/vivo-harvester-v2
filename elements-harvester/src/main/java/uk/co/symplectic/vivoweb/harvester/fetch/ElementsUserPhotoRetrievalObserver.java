/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.fetch;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.utils.ImageUtils;
import uk.co.symplectic.vivoweb.harvester.fetch.resources.ResourceFetchService;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.store.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.MessageFormat;

/**
 * An ElementsStoreOutputItemObserver that monitors for RAW_OBJECT files being added to the store.
 * If the file being added represents a user, and that user has an associated "photo" in Elements
 * The observer enqueues a request in the ResourceFetchService to fetch that user's photo from Elements and place the
 * retrieved image into the store as a RAW_USER_PHOTO.
 *
 * This is abstract, the actual functionality is implemented by the concrete static inner classes below
 */

@SuppressWarnings("WeakerAccess")
public abstract class ElementsUserPhotoRetrievalObserver extends ElementsStoreOutputItemObserver {

    private final ImageUtils.PhotoType photoType;
    protected ImageUtils.PhotoType getPhotoType(){return photoType;}

    public ElementsUserPhotoRetrievalObserver(ImageUtils.PhotoType photoType, ElementsItemFileStore objectStore) {
        super(objectStore, StorableResourceType.RAW_OBJECT, StorableResourceType.RAW_USER_PHOTO, false);
        this.photoType = photoType; //can be set to null harmlessly - will act as if set to ImageUtils.PhotoType.Profile.
    }

    @Override
    protected void observeObjectDeletion(ElementsItemId.ObjectId objectId, StorableResourceType type){
        if (objectId.getItemSubType() == ElementsObjectCategory.USER) {
            safelyDeleteItem(objectId, MessageFormat.format("Unable to delete user-photo for user {0}", objectId.toString()));
        }
    }


    /**
     * A concrete implementation of ElementsUserPhotoRetrievalObserver that actually re-fetches the photo from the
     * Elements API
     */
    public static class FetchingObserver extends ElementsUserPhotoRetrievalObserver{
        //TODO: Sort out static as object behaviour here
        private final ResourceFetchService fetchService = new ResourceFetchService();
        private final ElementsAPI elementsApi;

        public FetchingObserver(ElementsAPI elementsApi, ImageUtils.PhotoType photoType, ElementsItemFileStore objectStore) {
            super(photoType, objectStore);
            if(elementsApi == null) throw new NullArgumentException("elementsApi");
            this.elementsApi  = elementsApi;
        }

        @Override
        protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItemInfo item) {
            if (info instanceof ElementsUserInfo) {
                ElementsUserInfo userInfo = (ElementsUserInfo) info;
                //will do nothing for a photoType of NONE..
                if (!StringUtils.isEmpty(userInfo.getPhotoUrl(getPhotoType()))) {
                    try {
                        fetchService.fetchUserPhoto(elementsApi, getPhotoType(), userInfo, getStore());
                    } catch (MalformedURLException mue) {
                        // TODO: Log error
                    }
                }
            }
        }

    }

    /**
     * A concrete implementation of ElementsUserPhotoRetrievalObserver that does not contact the Elements API.
     * If there is an existing RAW_USER_PHOTO already present in the store, it is "touched" to trigger any ItemObservers
     * As if it was and item that had been retrieved and newly added/updated in the store
     */
    public static class ReprocessingObserver extends ElementsUserPhotoRetrievalObserver{

        public ReprocessingObserver(ImageUtils.PhotoType photoType, ElementsItemFileStore objectStore){
            super(photoType, objectStore);
        }

        protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItemInfo item) {
            if (info instanceof ElementsUserInfo) {
                ElementsUserInfo userInfo = (ElementsUserInfo) info;
                if (!StringUtils.isEmpty(userInfo.getPhotoUrl(getPhotoType()))) {
                    try {
                        getStore().touchItem(info, getOutputType());
                    } catch (IOException e) {
                        // TODO: Log error
                    }
                }
            }
        }
    }
}
