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
import uk.co.symplectic.vivoweb.harvester.model.ElementsGroupInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsRelationshipInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.fetch.resources.ResourceFetchService;
import uk.co.symplectic.vivoweb.harvester.store.*;
import java.net.MalformedURLException;

public class ElementsUserPhotoRetrievalObserver extends IElementsStoredItemObserver.ElementsStoredResourceObserverAdapter {
    //TODO: Sort out static as object behaviour here
    private final ResourceFetchService fetchService = new ResourceFetchService();
    private final ElementsObjectFileStore objectStore;
    private final ElementsAPI elementsApi;

    public ElementsUserPhotoRetrievalObserver(ElementsAPI elementsApi, ElementsObjectFileStore objectStore) {
        super(StorableResourceType.TRANSLATED_OBJECT);
        if(elementsApi == null) throw new NullArgumentException("elementsApi");
        if(objectStore == null) throw new NullArgumentException("objectStore");
        this.elementsApi  = elementsApi;
        this.objectStore = objectStore;
    }

    @Override
    public void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item) {
        if (info instanceof ElementsUserInfo) {
            ElementsUserInfo userInfo = (ElementsUserInfo) info;
            if (!StringUtils.isEmpty(userInfo.getPhotoUrl())) {
                try {
                    fetchService.fetchUserPhoto(elementsApi, userInfo, objectStore);
                } catch (MalformedURLException mue) {
                    // TODO: Log error
                }
            }
        }
    }
}
