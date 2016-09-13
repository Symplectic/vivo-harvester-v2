/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.fetch;

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsObjectStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ajpc2_000 on 19/08/2016.
 */

//TODO : rationalise with ObjectCollection somehow?
public abstract class ElementsObjectKeyedCollection<T> {
    private final Map<ElementsItemId.ObjectId, T> objectItemMap = new ConcurrentHashMap<ElementsItemId.ObjectId, T>();
    private List<ElementsObjectCategory> allowedCategories = new ArrayList<ElementsObjectCategory>();

    public ElementsObjectKeyedCollection(ElementsObjectCategory... allowedCategories){
        if(allowedCategories != null) this.allowedCategories.addAll(Arrays.asList(allowedCategories));
    }

    public synchronized void put(ElementsItemId.ObjectId key, T item) {
        if (key != null){
            if(!allowedCategories.isEmpty() && !allowedCategories.contains(key.getCategory())){
                throw new IllegalStateException(MessageFormat.format("this collection does not support items of category {0}", key.getCategory()));
            }
            if(item != null) objectItemMap.put(key, item);
        }
    }

    public synchronized T remove(ElementsItemId.ObjectId key) {
        if (key != null) {
            return objectItemMap.remove(key);
        }
        return null;
    }

    public synchronized void removeAll(Collection<ElementsItemId.ObjectId> keys) {
        if (keys != null) {
            for (ElementsItemId.ObjectId key : keys) {
                remove(key);
            }
        }
    }

    public Set<ElementsItemId.ObjectId> keySet(){
        return objectItemMap.keySet();
    }
    public Collection<T> values(){ return objectItemMap.values();}

    public T get(ElementsItemId.ObjectId key){
        if(key == null) return null;
        return objectItemMap.get(key);
    }

    public synchronized void clear() {
        objectItemMap.clear();
    }

    public ElementsObjectStore getStoreWrapper() {
        return new StoreWrapper();
    }


    protected abstract T getItemToStore(ElementsObjectInfo objectInfo, StorableResourceType resourceType, byte[] data);

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
            put(objectInfo.getObjectId(), getItemToStore(objectInfo, resourceType, data));
            return null;
        }
    }

    public static class ObjectInfo extends ElementsObjectKeyedCollection<ElementsObjectInfo>{
        public ObjectInfo(ElementsObjectCategory... allowedCategories){ super(allowedCategories); }

        @Override
        protected ElementsObjectInfo getItemToStore(ElementsObjectInfo objectInfo, StorableResourceType resourceType, byte[] data) {
            return objectInfo;
        }
    }

    public static class Data extends ElementsObjectKeyedCollection<byte[]>{
        public Data(ElementsObjectCategory... allowedCategories){ super(allowedCategories); }
        @Override
        protected byte[] getItemToStore(ElementsObjectInfo objectInfo, StorableResourceType resourceType, byte[] data) {
            return data;
        }
    }

}


