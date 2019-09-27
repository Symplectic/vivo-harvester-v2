/*
 * ******************************************************************************
 *   Copyright (c) 2019 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.store;

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;

import java.io.IOException;

/**
 * Interfaces representing the concept of a "Store" for ElementsItem related resources.
 */
@SuppressWarnings("unused")
public interface ElementsItemStore {

    /**
     * The basic interface exposes just one method - store - which starts the data that represents a particular resource
     * associated with a particular Elements item.
     * @param itemInfo An ElementsItemInfo object representing the item to be stored (this includes the item id).
     * @param resourceType The "type" of the resource being stored.
     * @param data The raw byte array that should be stored
     * @return an ElementsStoredItemInfo object that provides access to the newly stored item.
     * @throws IOException if the item cannot be stored for some reason
     */
    @SuppressWarnings("UnusedReturnValue")
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
    @SuppressWarnings({"RedundantThrows", "UnusedReturnValue"})
    interface ElementsDeletableItemStore extends ElementsItemStore{
        /**
         * touch item and force or request processing of the specific observers requested
         * @param itemInfo the source elements item.
         * @param resourceType the resource type of the item.
         * @param explicitObservers A list of explicit observers that should be processed for the item being touched
         *                          over and above any observers already registered with the store.
         * @return an ElementsStoredItemInfo object that provides access to the stored item being touched.
         * @throws IOException if errors occur.
         */
        ElementsStoredItemInfo touchItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, IElementsStoredItemObserver... explicitObservers) throws IOException;


        /**
         * delete removes any data about the specified resource type for the specified item from the store.
         * @param itemId the source elements item.
         * @param resourceType the resource type of data to delete.
         * @throws IOException if errors occur
         */
        void deleteItem(ElementsItemId itemId, StorableResourceType resourceType) throws IOException;

        /**
         * cleardown deletes ALL data of a particular resource type across all items in the store.
         * @param resourceType the resource type to clear down.
         * @param followObservers whether any "observers" should be informed of the cleardown operation.
         * @throws IOException if errors occur
         */
        void cleardown(StorableResourceType resourceType, boolean followObservers) throws IOException;
    }
}
