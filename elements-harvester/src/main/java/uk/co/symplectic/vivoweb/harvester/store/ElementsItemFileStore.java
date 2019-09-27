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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * This class allows you to create a generic disk backed store of data for essentially any raw data that corresponds to
 * a StorableResourceType. Implements the ElementsDeletableItemStore interface so has store, touch, and delete methods
 * for a given resource associated with a known item  and cleardown to delete all data about a particular resource type
 * for all items.
 * Importantly provides an Observer pattern whereby IElementsStoredItemObserver objects can be added to the store.
 * These observers are triggered for any of the "store" methods listed above (store, delete, touch & cleardown).
 *
 * Additionally adds extra methods to allow retrieval of items and all types of items from the store, as well as
 * tracking which items in the store have been "affected" during the objects lifetime.
 *
 *
 *
 * Offers various options for how files are handled: Layout strategy, handling of empty files, zipping of files, etc
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ElementsItemFileStore implements ElementsItemStore.ElementsDeletableItemStore {
    private List<StorableResourceType> supportedTypes = new ArrayList<StorableResourceType>();
    private File dir = null;
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
        //initialise affected item lists for each resource type
        for(StorableResourceType type : supportedTypes){
            affectedItems.put(type, new HashSet<ElementsItemId>());
        }
    }

    /**
     * Method to retrieve information about all the resource in this store, of a particular StorableResourceType,
     * that have been "affected" during the lifetime of this object
     * @param resourceType the StorableResourceType of items you are interested in
     * @return  Set<ElementsItemId> of all the items for which the resourceType has been affected
     */
    public Set<ElementsItemId> getAffectedItems(StorableResourceType resourceType){
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        return Collections.unmodifiableSet(affectedItems.get(resourceType));
    }

    /**
     * Internal method to mark an item as affected
     * @param resourceType the StorableResourceType of the resource in the store that was affected
     * @param itemId the Elements item that the resource was associated with
     * @return true if the item is newly added to the affected list for this resource type
     *         false if it was already on the affected list
     */
    private boolean markItemAsAffected(StorableResourceType resourceType, ElementsItemId itemId) {
        //TODO: do checks that resource type is valid for store and item? its only ever called from places that already do their own checks?
        Set<ElementsItemId> currentList = affectedItems.get(resourceType);
        return currentList != null && currentList.add(itemId);
    }

    /**
     * Add an IElementsStoredItemObserver to this store
     * @param observer the observer to add
     */
    public void addItemObserver(IElementsStoredItemObserver observer){ itemObservers.add(observer); }

    /**
     * Remove an IElementsStoredItemObserver to this store
     * @param observer the observer to remove
     */
    public void removeItemObserver(IElementsStoredItemObserver observer){
        itemObservers.remove(observer);
    }

    /**
     * Method to fetch all resources in this store that are keyed to a specific Elements item
     * regardless of StorableResourceType
     * @param itemId the id of the Elements item you are interested in
     * @return Collection<BasicElementsStoredItem> of all the stored items related to itemId
     */
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
     * @param itemId The Elements Item for which you want to retrieve a resource from the Store
     * @param resourceType The Type of resource you want to retrieve
     * @return A BasicElementsStoredItem object to provide access to the relevant resource (or null if nothing available)
     */
    public BasicElementsStoredItem retrieveItem(ElementsItemId itemId, StorableResourceType resourceType){
        if(!resourceType.isAppropriateForItem(itemId))  throw new IllegalStateException("resourceType is incompatible with item");
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        File file = layoutStrategy.getItemFile(dir, itemId, resourceType);
        //return file == null ? null : new ElementsStoredItemInfo.InFile(file, itemInfo, resourceType, shouldZipResourceFile(resourceType));
        return file == null || !file.exists() ? null : new BasicElementsStoredItem(itemId, resourceType, new StoredData.InFile(file, shouldZipResourceFile(resourceType)));
    }

    /**
     * Method to retrieve all StoredItem of a particular resourceType that exist in this store.
     * @param resourceType The Type of resource you want to retrieve
     * @return A Collection of StoredData.InFile objects to provide access to the relevant resources
     */
    public Collection<StoredData.InFile> getAllExistingFilesOfType(StorableResourceType resourceType){
        return getAllExistingFilesOfType(resourceType, null);
    }

    /**
     * Method to retrieve all StoredItem of a particular resourceType that exist in this store.
     * @param resourceType The Type of resource you want to retrieve
     * @param subType The subtype of resource you want to retrieve
     * @return A Collection of StoredData.InFile objects to provide access to the relevant resources
     */
    public Collection<StoredData.InFile> getAllExistingFilesOfType(StorableResourceType resourceType, ElementsItemType.SubType subType){
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        Collection<File> files = subType == null ? layoutStrategy.getAllExistingFilesOfType(dir,resourceType) : layoutStrategy.getAllExistingFilesOfType(dir, resourceType, subType);
        boolean isZipped = shouldZipResourceFile(resourceType);
        Collection<StoredData.InFile> data = new ArrayList<StoredData.InFile>();
        for(File file : files) data.add(new StoredData.InFile(file, isZipped));
        return data;
    }

    //See interface for javadoc
    @Override
    public ElementsStoredItemInfo storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException{
        //TODO: do something better here with error message?
        if(!resourceType.isAppropriateForItem(itemInfo.getItemId())) throw new IllegalStateException("resourceType is incompatible with item");
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        File file = layoutStrategy.getItemFile(dir, itemInfo.getItemId(), resourceType);
        ElementsStoredItemInfo storedItem = new ElementsStoredItemInfo(itemInfo, resourceType, new StoredData.InFile(file, shouldZipResourceFile(resourceType)));
        store(file, data, zipFiles && resourceType.shouldZip());

        //flag the item as having been affected during this run
        //if it is a newly affected item, or if the data is actually being updated during this processing run then process any observers
        if(markItemAsAffected(resourceType, itemInfo.getItemId())) {
            for (IElementsStoredItemObserver observer : itemObservers) {
                observer.observe(storedItem);
            }
        }
        return storedItem;
    }

    //See interface for javadoc
    @Override
    public ElementsStoredItemInfo touchItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, IElementsStoredItemObserver... explicitObservers) throws IOException{
        //TODO: do something better here with error message?
        if(!resourceType.isAppropriateForItem(itemInfo.getItemId())) throw new IllegalStateException("resourceType is incompatible with item");
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        File file = layoutStrategy.getItemFile(dir, itemInfo.getItemId(), resourceType);
        ElementsStoredItemInfo storedItem = new ElementsStoredItemInfo(itemInfo, resourceType, new StoredData.InFile(file, shouldZipResourceFile(resourceType)));
        if(!file.exists()){ throw new FileNotFoundException(file.getAbsolutePath()); }


        if(explicitObservers != null && explicitObservers.length != 0) {
            //reprocess any specifically requested observers regardless of whether the item has been "affected" already.
            for (IElementsStoredItemObserver observer : explicitObservers) {
                observer.observe(storedItem);
            }
        }

        //flag the item as having been affected during this run
        //if it is a newly affected item, or if the data is actually being updated during this processing run then process any observers
        if(markItemAsAffected(resourceType, itemInfo.getItemId())) {
            for (IElementsStoredItemObserver observer : itemObservers) {
                observer.observe(storedItem);
            }
        }
        return storedItem;
    }

    //See interface for javadoc
    @Override
    public void deleteItem(ElementsItemId itemId, StorableResourceType resourceType) throws IOException{
        //TODO: do something better here with error message?
        if(!resourceType.isAppropriateForItem(itemId)) throw new IllegalStateException("resourceType is incompatible with item");
        if(!supportedTypes.contains(resourceType)) throw new IllegalStateException("resourceType is incompatible with store");
        File file = layoutStrategy.getItemFile(dir, itemId, resourceType);
        //TODO: should this log if there is nothing to delete?, note that file would not always be present, e.g. for a translated prof-activity?
        if(file.exists())
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        //TODO: should this use markAsAffected?
        for(IElementsStoredItemObserver observer : itemObservers) {
            observer.observeDeletion(itemId, resourceType);
        }
    }

    public void cleardown(StorableResourceType resourceType) throws IOException{
        cleardown(resourceType, true);
    }

    //See interface for javadoc
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
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
