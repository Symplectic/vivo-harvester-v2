/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.store;

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;

import java.io.IOException;

/**
 * Created by ajpc2_000 on 12/08/2016.
 */
public interface ElementsItemStore {
    ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException;

//    class MultiStore implements ElementsItemStore {
//        List<ElementsItemStore> stores = new ArrayList<ElementsItemStore>();
//
//        public MultiStore(ElementsItemStore... stores){
//            if(stores == null || stores.length == 0) throw new IllegalArgumentException("stores must not be null or empty");
//            this.stores.addAll(Arrays.asList(stores));
//        }
//
//        @Override
//        public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
//            for(ElementsItemStore store : stores){
//                store.storeItem(itemInfo, resourceType, data);
//            }
//            return null;
//        }
//    }
}
