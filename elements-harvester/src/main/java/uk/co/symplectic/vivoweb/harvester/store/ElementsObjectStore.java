/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.xpath.operations.Mult;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ajpc2_000 on 12/08/2016.
 */
public interface ElementsObjectStore {
    ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, String data) throws IOException;
    ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException;

    public class MultiStore implements ElementsObjectStore{
        List<ElementsObjectStore> stores = new ArrayList<ElementsObjectStore>();

        public MultiStore(ElementsObjectStore... stores){
            if(stores == null || stores.length == 0) throw new IllegalArgumentException("stores must not be null or empty");
            this.stores.addAll(Arrays.asList(stores));
        }

        @Override
        public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, String data) throws IOException {
            for(ElementsObjectStore store : stores){
                store.storeItem(itemInfo, resourceType, data);
            }
            //TODO: (stop returning an item? - not using it and it confuses the interfaces).
            return null;
        }

        @Override
        public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
            for(ElementsObjectStore store : stores){
                store.storeItem(itemInfo, resourceType, data);
            }
            //TODO: (stop returning an item? - not using it and it confuses the interfaces).
            return null;
        }
    }
}
