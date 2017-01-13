/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.store;

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;

import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;

public interface ElementsItemStore {
    ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException;

//    class MultiStore implements ElementsItemStore {
//        List<ElementsItemStore> stores = new ArrayList<ElementsItemStore>();
//
//        public MultiStore(ElementsItemStore... stores) {
//            if (stores == null || stores.length == 0)
//                throw new IllegalArgumentException("stores must not be null or empty");
//            this.stores.addAll(Arrays.asList(stores));
//        }
//
//        @Override
//        public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
//            for (ElementsItemStore store : stores) {
//                store.storeItem(itemInfo, resourceType, data);
//            }
//            return null;
//        }
//    }

    interface ElementsDeletableItemStore extends ElementsItemStore{
        //TODO move touch, its not in a good place.
        ElementsStoredItem touchItem(ElementsItemInfo itemInfo, StorableResourceType resourceType) throws IOException;
        void deleteItem(ElementsItemId itemId, StorableResourceType resourceType) throws IOException;
        void cleardown(StorableResourceType resourceType, boolean followObservers) throws IOException;
    }
}
