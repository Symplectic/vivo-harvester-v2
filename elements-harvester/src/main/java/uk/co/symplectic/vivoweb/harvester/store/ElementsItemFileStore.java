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
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;


public class ElementsItemFileStore implements ElementsItemStore.ElementsDeletableItemStore {
    List<StorableResourceType> supportedTypes = new ArrayList<StorableResourceType>();
    protected File dir = null;
    final private LayoutStrategy layoutStrategy;
    private List<IElementsStoredItemObserver> itemObservers = new ArrayList<IElementsStoredItemObserver>();
    private final Map<StorableResourceType, Set<ElementsItemId>> affectedItems = new HashMap<StorableResourceType, Set<ElementsItemId>>();
    private boolean keepEmpty = false;
    private boolean zipFiles = true;

    public List<StorableResourceType> getSupportedTypes(){return Collections.unmodifiableList(supportedTypes);}

    public ElementsItemFileStore(File dir, boolean keepEmpty, boolean zipFiles, LayoutStrategy layoutStrategy, StorableResourceType... supportedTypes) {
        if(dir == null) throw new NullArgumentException("dir");
        if(supportedTypes == null || supportedTypes.length == 0) throw new IllegalArgumentException("supportedTypes must not be null or empty");

        this.dir = dir;
        this.keepEmpty = keepEmpty;
        this.zipFiles = zipFiles;

        this.layoutStrategy = layoutStrategy != null ? layoutStrategy : new DefaultLayoutStrategy();
        this.supportedTypes.addAll(Arrays.asList(supportedTypes));
        //intialise affected item lists for each resource type
        for(StorableResourceType type : supportedTypes){
            affectedItems.put(type, new HashSet<ElementsItemId>());
        }
    }

    public Set<ElementsItemId> getAffectedItems(StorableResourceType resourceType){
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        return Collections.unmodifiableSet(affectedItems.get(resourceType));
    }

    //returns true if the item has already been affected
    private boolean markItemAsAffected(StorableResourceType resourceType, ElementsItemId itemId) {
        //TODO: do checks that resource type is valid for store and item? its only ever called from places that already do their own checks?
        Set<ElementsItemId> currentList = affectedItems.get(resourceType);
        return currentList != null && currentList.add(itemId);
    }

    public void addItemObserver(IElementsStoredItemObserver observer){ itemObservers.add(observer); }

    public Collection<BasicElementsStoredItem> retrieveAllRelatedResources(ElementsItemId itemId){
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
        //return file == null ? null : new ElementsStoredItemInfo.InFile(file, itemInfo, resourceType, shouldZipResourceFile(resourceType));
        return file == null || !file.exists() ? null : new BasicElementsStoredItem(itemId, resourceType, new StoredData.InFile(file, shouldZipResourceFile(resourceType)));
    }

    public Collection<StoredData.InFile> getAllExistingFilesOfType(StorableResourceType resourceType){
        return getAllExistingFilesOfType(resourceType, null);
    }

    public Collection<StoredData.InFile> getAllExistingFilesOfType(StorableResourceType resourceType, ElementsItemType.SubType subType){
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        Collection<File> files = subType == null ? layoutStrategy.getAllExistingFilesOfType(dir,resourceType) : layoutStrategy.getAllExistingFilesOfType(dir, resourceType, subType);
        boolean isZipped = shouldZipResourceFile(resourceType);
        Collection<StoredData.InFile> data = new ArrayList<StoredData.InFile>();
        for(File file : files) data.add(new StoredData.InFile(file, isZipped));
        return data;
    }

    public ElementsStoredItemInfo innerStoreItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data, boolean actuallyStoreData) throws IOException{
        //TODO: do something better here with error message?
        if(!resourceType.isAppropriateForItem(itemInfo.getItemId())) throw new IllegalStateException("resourceType is incompatible with item");
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        File file = layoutStrategy.getItemFile(dir, itemInfo.getItemId(), resourceType);
        ElementsStoredItemInfo storedItem = new ElementsStoredItemInfo(itemInfo, resourceType, new StoredData.InFile(file, shouldZipResourceFile(resourceType)));
        if(actuallyStoreData){ store(file, data, zipFiles && resourceType.shouldZip()); }
        else if(!file.exists()){ throw new FileNotFoundException(file.getAbsolutePath()); }

        //flag the item as having been affected during this run
        //if it is a newly affected item, or if the data is actually being updated during this processing run then process any observers
        if(markItemAsAffected(resourceType, itemInfo.getItemId()) || actuallyStoreData) {
            for (IElementsStoredItemObserver observer : itemObservers) {
                observer.observe(storedItem);
            }
        }
        return storedItem;
    }

    @Override
    public ElementsStoredItemInfo storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException{
        return innerStoreItem(itemInfo, resourceType, data, true);
    }

    @Override
    public ElementsStoredItemInfo touchItem(ElementsItemInfo itemInfo, StorableResourceType resourceType) throws IOException{
        return innerStoreItem(itemInfo, resourceType, null, false);
    }

    @Override
    public void deleteItem(ElementsItemId itemId, StorableResourceType resourceType) throws IOException{
        //TODO: do something better here with error message?
        if(!resourceType.isAppropriateForItem(itemId)) throw new IllegalStateException("resourceType is incompatible with item");
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        File file = layoutStrategy.getItemFile(dir, itemId, resourceType);
        //TODO: should this log if there is nothing to delete?, note that file would not always be present, e.g. for a translated prof-activity?
        if(file.exists())
            file.delete();
        //TODO: should this use markAsAffected?
        for(IElementsStoredItemObserver observer : itemObservers) {
            observer.observeDeletion(itemId, resourceType);
        }
    }

    public void cleardown(StorableResourceType resourceType) throws IOException{
        cleardown(resourceType, true);
    }

    @Override
    public void cleardown(StorableResourceType resourceType, boolean followObservers) throws IOException {
        for (StoredData.InFile data : getAllExistingFilesOfType(resourceType)) {
            data.delete();
        }
        if (followObservers){
            for (IElementsStoredItemObserver observer : itemObservers) {
                observer.observeCleardown(resourceType, this);
            }
        }
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

        public ElementsRawDataStore(File dir) {
            this(dir, false, false);
        }

        public ElementsRawDataStore(File dir, boolean keepEmpty, boolean zipFiles){
            super(dir, keepEmpty, zipFiles, ElementsRawDataStore.layoutStrategy,
                    StorableResourceType.RAW_OBJECT, StorableResourceType.RAW_RELATIONSHIP, StorableResourceType.RAW_USER_PHOTO, StorableResourceType.RAW_GROUP);
        }
    }
}
