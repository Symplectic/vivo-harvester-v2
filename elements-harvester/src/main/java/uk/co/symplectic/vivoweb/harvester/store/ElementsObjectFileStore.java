/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfoCache;

import java.io.*;
import java.util.*;

public class ElementsObjectFileStore implements ElementsObjectStore{
    List<StorableResourceType> supportedTypes = new ArrayList<StorableResourceType>();
    protected File dir = null;
    final private LayoutStrategy layoutStrategy;
    private List<IElementsStoredItemObserver> itemObservers = new ArrayList<IElementsStoredItemObserver>();
    private boolean keepEmpty = false;

    public List<StorableResourceType> getSupportedTypes(){return Collections.unmodifiableList(supportedTypes);}

    public ElementsObjectFileStore(String dir, boolean keepEmpty, LayoutStrategy layoutStrategy, StorableResourceType... supportedTypes) {
        if(dir == null) throw new NullArgumentException("dir");
        if(supportedTypes == null || supportedTypes.length == 0) throw new IllegalArgumentException("supportedTypes must not be null or empty");

        this.dir = new File(dir);
        this.keepEmpty = keepEmpty;

        this.layoutStrategy = layoutStrategy != null ? layoutStrategy : new DefaultLayoutStrategy();
        this.supportedTypes.addAll(Arrays.asList(supportedTypes));
    }

    public void addItemObserver(IElementsStoredItemObserver observer){ itemObservers.add(observer); }

    /*
    public ElementsStoredObject retrieveObject(ElementsObjectCategory category, String id) {
        File objectFile = getObjectFile(category, id);
        return objectFile == null ? null : new ElementsStoredObject(objectFile, category, id);
    }
    }

    public ElementsStoredRelationship retrieveRelationship(String id) {
        File relationshipFile = getRelationshipFile(id);
        return relationshipFile == null ? null : new ElementsStoredRelationship(relationshipFile, id);
    }*/


    //NOTE : returns all "possible" items without checking if item is actually present in the store - is this best?
    public Collection<ElementsStoredItem> retrieveAllItems(ElementsItemInfo itemInfo){
        List<ElementsStoredItem> items = new ArrayList<ElementsStoredItem>();
        for(StorableResourceType resourceType : supportedTypes){
            if(resourceType.isAppropriateForItem(itemInfo)) {
                ElementsStoredItem item = retrieveItem(itemInfo, resourceType);
                if (item != null) items.add(item);
            }
        }
        return items;
    }

    /**
     * Method to retrieve a StoredItem representing a particular resource
     * The corresponding file may or may not exist in this store - no guarantees are made about it by this call.
     * @param itemInfo
     * @param resourceType
     * @return
     */
    public ElementsStoredItem retrieveItem(ElementsItemInfo itemInfo, StorableResourceType resourceType){
        if(!resourceType.isAppropriateForItem(itemInfo))  throw new IllegalStateException("resourceType is incompatible with item");
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        File file = layoutStrategy.getItemFile(dir, itemInfo, resourceType);
        return file == null ? null : new ElementsStoredItem(file, itemInfo, resourceType);
    }

    public Collection<File> getAllExistingFilesOfType(StorableResourceType resourceType){
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        return layoutStrategy.getAllExistingFilesOfType(dir,resourceType);
    }

    public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, String data) throws IOException{
        return storeItem(itemInfo, resourceType, data.getBytes("utf-8"));
    }

    public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException{
        //TODO: do something better here with error message?
        if(!resourceType.isAppropriateForItem(itemInfo)) throw new IllegalStateException("resourceType is incompatible with item");
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        File file = layoutStrategy.getItemFile(dir, itemInfo, resourceType);
        ElementsStoredItem storedItem = new ElementsStoredItem(file, itemInfo, resourceType);
        //TODO: move this to an observer?
        if(itemInfo.isObjectInfo()) ElementsObjectInfoCache.put(itemInfo.asObjectInfo());
        store(file, data);
        for(IElementsStoredItemObserver observer : itemObservers)
            observer.observe(storedItem);
        return storedItem;
    }

    private void store(File file, byte[] data) throws IOException{
        byte[] dataToStore = data == null ? new byte[0] : data;
        if (keepEmpty || dataToStore.length > 0) {
            OutputStream outputStream = null;
            try {
                outputStream = (new BufferedOutputStream(new FileOutputStream(file)));
                IOUtils.copy(new ByteArrayInputStream(dataToStore), outputStream);
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        }
        //if not keeping empties and the file is empty
        else if(file.exists()){
            file.delete();
        }
    }

    public static class ElementsRawDataStore extends ElementsObjectFileStore {
        private static LayoutStrategy layoutStrategy = new DefaultLayoutStrategy(
            new StorableResourceType[]{StorableResourceType.RAW_OBJECT, StorableResourceType.RAW_RELATIONSHIP},
            new StorableResourceType[]{StorableResourceType.RAW_USER_PHOTO}
        );

        public ElementsRawDataStore(String dir) {
            this(dir, false);
        }

        public ElementsRawDataStore(String dir, boolean keepEmpty){
            super(dir, keepEmpty, ElementsRawDataStore.layoutStrategy,
                    StorableResourceType.RAW_OBJECT, StorableResourceType.RAW_RELATIONSHIP, StorableResourceType.RAW_USER_PHOTO);
        }
    }
}
