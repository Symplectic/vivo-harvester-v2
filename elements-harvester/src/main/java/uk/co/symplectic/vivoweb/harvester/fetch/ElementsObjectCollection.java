/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.fetch;

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsObjectStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ElementsObjectCollection implements ElementsObjectStore {
    private final Map<ElementsObjectCategory, Set<ElementsObjectId>> objectCategoryIdMap = new ConcurrentHashMap<ElementsObjectCategory, Set<ElementsObjectId>>();

    public synchronized void add(ElementsObjectId id) {
        Set<ElementsObjectId> idSet = getIdSet(id.getCategory());
        idSet.add(id);
    }

    public Set<ElementsObjectId> get(ElementsObjectCategory category) { return Collections.unmodifiableSet(getIdSet(category)); }

    private Set<ElementsObjectId> getIdSet(ElementsObjectCategory category) {
        Set<ElementsObjectId> idSet = objectCategoryIdMap.get(category);
        if (idSet == null) {
            idSet = new HashSet<ElementsObjectId>();
            objectCategoryIdMap.put(category, idSet);
        }
        return idSet;
    }

    @Override
    public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, String data) throws IOException {
        return storeItem(itemInfo, resourceType, (byte[]) null);
    }

    @Override
    public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
        if(!itemInfo.isObjectInfo()) throw new IllegalStateException("ElementsObjectCollection can only store Object items");
        ElementsObjectInfo objectInfo = itemInfo.asObjectInfo();
        this.add(objectInfo.getObjectId());
        return null;
    }
}
