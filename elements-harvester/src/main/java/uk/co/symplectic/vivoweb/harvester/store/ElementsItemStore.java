/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.store;

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;

import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;


/**
 * Interface representing the concept of a "Store" for ElementsItem related resources.
 */
public interface ElementsItemStore {

    /**
     * The basic interface exposes just one method - store - which starts the data that represents a paricular resource
     * associated with a particular Elements item.
     * @param itemInfo
     * @param resourceType
     * @param data
     * @return
     * @throws IOException
     */
    ElementsStoredItemInfo storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException;

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
//        public ElementsStoredItemInfo storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
//            for (ElementsItemStore store : stores) {
//                store.storeItem(itemInfo, resourceType, data);
//            }
//            return null;
//        }
//    }

    /**
     * Richer interface the expands the definition of a store
     * Adds methods to expose ability to "delete" stored data as well as methods to cleardown all data of one type.
     * Additionally adds a method intended to "touch" a particular item.
     * Unlike the concept of "touch" for filesystems, this operation has no actual effect on the stored data.
     * It exists primarily to allow invocation of any "observers" for a particular item's specified resource.
     */
    interface ElementsDeletableItemStore extends ElementsItemStore{

        /**
         * the touch item
         * @param itemInfo the source elements item.
         * @param resourceType the resource type of the item.
         * @return
         * @throws IOException
         */
        ElementsStoredItemInfo touchItem(ElementsItemInfo itemInfo, StorableResourceType resourceType) throws IOException;

        /**
         * delete removes any data about the specified resource type for the specified item from the store.
         * @param itemId the source elements item.
         * @param resourceType the resource type of data to delete.
         * @throws IOException
         */
        void deleteItem(ElementsItemId itemId, StorableResourceType resourceType) throws IOException;

        /**
         * cleardown deletes ALL data of a particular resource type across all items in the store.
         * @param resourceType the resource type to clear down.
         * @param followObservers whether any "observers" should be informed of the cleardown operation.
         * @throws IOException
         */
        void cleardown(StorableResourceType resourceType, boolean followObservers) throws IOException;
    }
}
