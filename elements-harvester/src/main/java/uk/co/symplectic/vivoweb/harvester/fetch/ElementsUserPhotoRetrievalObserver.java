/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.fetch;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.vivoweb.harvester.fetch.resources.ResourceFetchService;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.store.*;
import java.net.MalformedURLException;
import java.text.MessageFormat;

public class ElementsUserPhotoRetrievalObserver extends ElementsStoreOutputItemObserver {

    //TODO: Sort out static as object behaviour here
    private final ResourceFetchService fetchService = new ResourceFetchService();
    private final ElementsAPI elementsApi;

    public ElementsUserPhotoRetrievalObserver(ElementsAPI elementsApi, ElementsItemFileStore objectStore) {
        super(objectStore, StorableResourceType.RAW_OBJECT, StorableResourceType.RAW_USER_PHOTO, false);
        if(elementsApi == null) throw new NullArgumentException("elementsApi");
        this.elementsApi  = elementsApi;
    }

    @Override
    protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item) {
        if (info instanceof ElementsUserInfo) {
            ElementsUserInfo userInfo = (ElementsUserInfo) info;
            if (!StringUtils.isEmpty(userInfo.getPhotoUrl())) {
                try {
                    fetchService.fetchUserPhoto(elementsApi, userInfo, getStore());
                } catch (MalformedURLException mue) {
                    // TODO: Log error
                }
            }
        }
    }

    @Override
    protected void observeObjectDeletion(ElementsItemId.ObjectId objectId, StorableResourceType type){
        if (objectId.getItemSubType() == ElementsObjectCategory.USER) {
            safelyDeleteItem(objectId, MessageFormat.format("Unable to delete user-photo for user {0}", objectId.toString()));
        }
    }
}
