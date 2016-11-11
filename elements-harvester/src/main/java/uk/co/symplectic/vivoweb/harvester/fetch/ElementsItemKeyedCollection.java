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
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ajpc2_000 on 19/08/2016.
 */
public abstract class ElementsItemKeyedCollection<T> {

    private final Map<ElementsItemId, T> mData = new ConcurrentHashMap<ElementsItemId, T>();
    private ItemRestrictor mRestrictor = null;

    protected ElementsItemKeyedCollection(ItemRestrictor restrictor){
        mRestrictor = restrictor;
    }

    public synchronized void put(ElementsItemId key, T item) {
        if (key != null){
            if(mRestrictor != null) mRestrictor.checkItemIdIsValid(key); //throws IllegalStateException if not valid.
            if(item != null) mData.put(key, item);
        }
    }

    public synchronized T remove(ElementsItemId key) {
        if(key == null) return null;
        return mData.remove(key);
    }

    public synchronized void removeAll(Collection<ElementsItemId> keys) {
        if (keys != null) {
            for (ElementsItemId key : keys) {
                if (key != null)
                    mData.remove(key);
            }
        }
    }

    public synchronized Set<ElementsItemId> keySet(){
        return mData.keySet();
    }
    public synchronized Collection<T> values(){ return mData.values();}

    public synchronized T get(ElementsItemId key){
        if(key == null) return null;
        return mData.get(key);
    }

    public synchronized void clear() {
        mData.clear();
    }

    public ElementsItemStore getStoreWrapper() {
        return new StoreWrapper();
    }

    protected abstract T getItemToStore(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data);

    private class StoreWrapper implements ElementsItemStore {
        @Override
        public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
            put(itemInfo.getItemId(), getItemToStore(itemInfo, resourceType, data));
            return null;
        }
    }

    public static interface ItemRestrictor{
        //checkItemId shuold throw an IllegalStateException if the item key is invalid
        void checkItemIdIsValid(ElementsItemId itemId);
    }

    public static class RestrictToType implements ItemRestrictor{
        private final ElementsItemType mType;

        public RestrictToType(ElementsItemType type){
            mType = type;
        }

        public void checkItemIdIsValid(ElementsItemId itemId){
            if(itemId.getItemType() != mType) {
                throw new IllegalStateException(MessageFormat.format("This collection does not support items of type {0}", itemId.getItemType().getName()));
            }
        }
    }

    public static class RestrictToCategories extends RestrictToType{
        private List<ElementsObjectCategory> mAllowedCategories = new ArrayList<ElementsObjectCategory>();

        public RestrictToCategories(ElementsObjectCategory... allowedCategories){
            super(ElementsItemType.OBJECT);
            if(allowedCategories != null) mAllowedCategories.addAll(Arrays.asList(allowedCategories));
        }

        public void checkItemIdIsValid(ElementsItemId itemId){
            super.checkItemIdIsValid(itemId);
            //we now know it IS an item of type OBJECT
            ElementsObjectCategory itemCategory = ((ElementsItemId.ObjectId) itemId).getCategory();
            if(!mAllowedCategories.contains(itemCategory)) {
                throw new IllegalStateException(MessageFormat.format("This collection does not support objects of category {0}", itemCategory));
            }
        }
    }

    public static class ItemInfo extends ElementsItemKeyedCollection<ElementsItemInfo>{
        public ItemInfo(){ super(null); }
        public ItemInfo(ItemRestrictor restrictor){ super(restrictor); }

        @Override
        protected ElementsItemInfo getItemToStore(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) {
            return itemInfo;
        }
    }

    public static class StoredItem extends ElementsItemKeyedCollection<ElementsStoredItem.InRam>{
        public StoredItem(){ super(null); }
        public StoredItem(ItemRestrictor restrictor){ super(restrictor); }

        @Override
        protected ElementsStoredItem.InRam getItemToStore(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) {
            return new ElementsStoredItem.InRam(data, itemInfo, resourceType);
        }
    }
}


