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
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItemInfo;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;
import uk.co.symplectic.vivoweb.harvester.store.StoredData;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        public ElementsStoredItemInfo storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
            put(itemInfo.getItemId(), getItemToStore(itemInfo, resourceType, data));
            return null;
        }
    }

    public interface ItemRestrictor{
        //checkItemId should throw an IllegalStateException if the item key is invalid
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

    public static class RestrictToSubTypes implements ItemRestrictor{
        private List<ElementsItemType.SubType> mAllowedCategories = new ArrayList<ElementsItemType.SubType>();

        public RestrictToSubTypes(ElementsItemType.SubType... allowedSubTypes){
            if(allowedSubTypes != null) mAllowedCategories.addAll(Arrays.asList(allowedSubTypes));
            if(!mAllowedCategories.isEmpty()){
                ElementsItemType.SubType firstSubType =  mAllowedCategories.get(0);
                for(ElementsItemType.SubType currentSubType : mAllowedCategories){
                    if(currentSubType.getMainType() != firstSubType.getMainType())
                        throw new IllegalStateException("Subtypes must be of the same main type");
                }
            }
        }

        public void checkItemIdIsValid(ElementsItemId itemId){
            //we now know it IS an item of type OBJECT
            ElementsItemType.SubType itemSubType = itemId.getItemSubType();
            if(!mAllowedCategories.contains(itemSubType)) {
                throw new IllegalStateException(MessageFormat.format("This collection does not support objects of sub type {0}", itemSubType.getSingular()));
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

    public static class StoredItem extends ElementsItemKeyedCollection<ElementsStoredItemInfo>{
        public StoredItem(){ super(null); }
        public StoredItem(ItemRestrictor restrictor){ super(restrictor); }

        @Override
        protected ElementsStoredItemInfo getItemToStore(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) {
            return new ElementsStoredItemInfo(itemInfo, resourceType, new StoredData.InRam(data));
        }
    }
}


