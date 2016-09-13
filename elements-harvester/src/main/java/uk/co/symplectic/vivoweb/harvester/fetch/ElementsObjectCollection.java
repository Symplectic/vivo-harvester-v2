/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.fetch;

import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.ElementsObjectStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

    public class ElementsObjectCollection {
        private final Map<ElementsObjectCategory, Set<ElementsItemId.ObjectId>> categoryItemMap = new ConcurrentHashMap<ElementsObjectCategory, Set<ElementsItemId.ObjectId>>();

        public synchronized void add(ElementsItemId.ObjectId item) {
            if (item != null) {
                Set<ElementsItemId.ObjectId> itemSet = getOrCreateSetForCategory(item.getCategory());
                itemSet.add(item);
            }
        }

        public synchronized void addAll(Collection<ElementsItemId.ObjectId> items) {
            if (items != null) {
                for (ElementsItemId.ObjectId item : items) {
                    add(item);
                }
            }
        }

        protected Set<ElementsItemId.ObjectId> getOrCreateSetForCategory(ElementsObjectCategory category) {
            Set<ElementsItemId.ObjectId> itemSet = categoryItemMap.get(category);
            if (itemSet == null) {
                itemSet = new HashSet<ElementsItemId.ObjectId>();
                categoryItemMap.put(category, itemSet);
            }
            return itemSet;
        }

        public synchronized void remove(ElementsItemId.ObjectId item) {
            if (item != null) {
                Set<ElementsItemId.ObjectId> itemSet = categoryItemMap.get(item.getCategory());
                if (itemSet != null) {
                    itemSet.remove(item);
                }
                if (itemSet.isEmpty()) {
                    categoryItemMap.remove(item.getCategory());
                }
            }
        }

        public synchronized void removeAll(Collection<ElementsItemId.ObjectId> items) {
            if (items != null) {
                for (ElementsItemId.ObjectId item : items) {
                    remove(item);
                }
            }
        }

        //TODO: abstract to a proper retrieveable ram based store?
        public Set<ElementsItemId.ObjectId> get(ElementsObjectCategory category) {
            Set<ElementsItemId.ObjectId> resultSet = Collections.unmodifiableSet(categoryItemMap.get(category));
            return resultSet == null ? new HashSet<ElementsItemId.ObjectId>() : resultSet;
        }

        public ElementsObjectStore getStoreWrapper() {
            return new StoreWrapper();
        }


        private class StoreWrapper implements ElementsObjectStore {
            @Override
            public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, String data) throws IOException {
                return storeItem(itemInfo, resourceType, (byte[]) null);
            }

            @Override
            public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
                if (!itemInfo.isObjectInfo())
                    throw new IllegalStateException("ElementsObjectKeyedCollection can only store Object items");
                ElementsObjectInfo objectInfo = itemInfo.asObjectInfo();
                add(objectInfo.getObjectId());
                return null;
            }
        }
    }

