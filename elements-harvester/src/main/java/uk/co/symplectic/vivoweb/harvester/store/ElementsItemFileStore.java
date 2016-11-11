/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfoCache;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;


public class ElementsItemFileStore implements ElementsItemStore {
    List<StorableResourceType> supportedTypes = new ArrayList<StorableResourceType>();
    protected File dir = null;
    final private LayoutStrategy layoutStrategy;
    private List<IElementsStoredItemObserver> itemObservers = new ArrayList<IElementsStoredItemObserver>();
    private boolean keepEmpty = false;
    private boolean zipFiles = true;

    public List<StorableResourceType> getSupportedTypes(){return Collections.unmodifiableList(supportedTypes);}

    public ElementsItemFileStore(String dir, boolean keepEmpty, boolean zipFiles, LayoutStrategy layoutStrategy, StorableResourceType... supportedTypes) {
        if(dir == null) throw new NullArgumentException("dir");
        if(supportedTypes == null || supportedTypes.length == 0) throw new IllegalArgumentException("supportedTypes must not be null or empty");

        this.dir = new File(dir);
        this.keepEmpty = keepEmpty;
        this.zipFiles = zipFiles;

        this.layoutStrategy = layoutStrategy != null ? layoutStrategy : new DefaultLayoutStrategy();
        this.supportedTypes.addAll(Arrays.asList(supportedTypes));
    }

    public void addItemObserver(IElementsStoredItemObserver observer){ itemObservers.add(observer); }

    //todo : returns all "possible" items without checking if item is actually present in the store - decide if this is for the best?
    public Collection<BasicElementsStoredItem> retrieveAllItems(ElementsItemId itemId){
        List<BasicElementsStoredItem> items = new ArrayList<BasicElementsStoredItem>();
        for(StorableResourceType resourceType : supportedTypes){
            if(resourceType.isAppropriateForItem(itemId)) {
                BasicElementsStoredItem item = retrieveItem(itemId, resourceType);
                if (item != null) items.add(item);
            }
        }
        return items;
    }

    /**
     * Method to retrieve a StoredItem representing a particular resource
     * The corresponding file may or may not exist in this store - no guarantees are made about it by this call.
     * @param itemId
     * @param resourceType
     * @return
     */
    public BasicElementsStoredItem retrieveItem(ElementsItemId itemId, StorableResourceType resourceType){
        if(!resourceType.isAppropriateForItem(itemId))  throw new IllegalStateException("resourceType is incompatible with item");
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        File file = layoutStrategy.getItemFile(dir, itemId, resourceType);
        //return file == null ? null : new ElementsStoredItem.InFile(file, itemInfo, resourceType, shouldZipResourceFile(resourceType));
        return file == null || !file.exists() ? null : new BasicElementsStoredItem(itemId, resourceType, new StoredData.InFile(file, shouldZipResourceFile(resourceType)));
    }

    public Collection<StoredData.InFile> getAllExistingFilesOfType(StorableResourceType resourceType){
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        Collection<File> files = layoutStrategy.getAllExistingFilesOfType(dir,resourceType);
        boolean isZipped = shouldZipResourceFile(resourceType);
        Collection<StoredData.InFile> data = new ArrayList<StoredData.InFile>();
        for(File file : files) data.add(new StoredData.InFile(file, isZipped));
        return data;
    }

    public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException{
        //TODO: do something better here with error message?
        if(!resourceType.isAppropriateForItem(itemInfo.getItemId())) throw new IllegalStateException("resourceType is incompatible with item");
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        File file = layoutStrategy.getItemFile(dir, itemInfo.getItemId(), resourceType);
        ElementsStoredItem storedItem = new ElementsStoredItem.InFile(file, itemInfo, resourceType, shouldZipResourceFile(resourceType));
        //TODO: move this to an observer?
        if(itemInfo.isObjectInfo()) ElementsObjectInfoCache.put(itemInfo.asObjectInfo());
        store(file, data, zipFiles && resourceType.shouldZip());
        for(IElementsStoredItemObserver observer : itemObservers)
            observer.observe(storedItem);
        return storedItem;
    }

    private boolean shouldZipResourceFile(StorableResourceType resourceType) {return zipFiles && resourceType.shouldZip();}

    private void store(File file, byte[] data, boolean shouldZip) throws IOException{
        byte[] dataToStore = data == null ? new byte[0] : data;
        if (keepEmpty || dataToStore.length > 0) {
            OutputStream outputStream = null;
            try {
                outputStream = (new BufferedOutputStream(new FileOutputStream(file)));
                if(shouldZip) outputStream = new GZIPOutputStream(outputStream);
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

    public static class ElementsRawDataStore extends ElementsItemFileStore {
        private static LayoutStrategy layoutStrategy = new DefaultLayoutStrategy(
                new StorableResourceType[]{StorableResourceType.RAW_OBJECT, StorableResourceType.RAW_RELATIONSHIP, StorableResourceType.RAW_GROUP},
                new StorableResourceType[]{StorableResourceType.RAW_USER_PHOTO}
        );

        public ElementsRawDataStore(String dir) {
            this(dir, false, false);
        }

        public ElementsRawDataStore(String dir, boolean keepEmpty, boolean zipFiles){
            super(dir, keepEmpty, zipFiles, ElementsRawDataStore.layoutStrategy,
                    StorableResourceType.RAW_OBJECT, StorableResourceType.RAW_RELATIONSHIP, StorableResourceType.RAW_USER_PHOTO, StorableResourceType.RAW_GROUP);
        }
    }
}
