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

import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * Class that defines the DefaultLayoutStrategy as used by the ElementsItemFileStores used in this project.
 * Specifies where the files representing different types (StorableResourceTypes) of Elements data are placed on disk.
 * Additionally provides search utilities to identify all files of a given Elements Type/SubType
 */

@SuppressWarnings("WeakerAccess")
public class DefaultLayoutStrategy implements LayoutStrategy {

    private Map<ElementsItemType, StorableResourceType> mainResourceTypes = new HashMap<ElementsItemType, StorableResourceType>();
    private Set<StorableResourceType> resourceTypesWithOwnDirectory = new HashSet<StorableResourceType>();

    public DefaultLayoutStrategy(){
        this(null, null);
    }
    public DefaultLayoutStrategy(StorableResourceType[] mainResourceTypes, StorableResourceType[] resourceTypesWithOwnDirectory){
        if(mainResourceTypes != null) {
            for (StorableResourceType type : mainResourceTypes) {
                //put returns null if the key currently does not have a value..
                if (this.mainResourceTypes.put(type.getKeyItemType(), type) != null)
                    throw new IllegalArgumentException("cannot have multiple mainTypes of the same ElementsItemType in a layoutStrategy");
            }
        }
        if(resourceTypesWithOwnDirectory != null)
            this.resourceTypesWithOwnDirectory.addAll(Arrays.asList(resourceTypesWithOwnDirectory));
    }

    @Override
    public File getItemFile(File storeDir, ElementsItemId itemId, StorableResourceType resourceType) {
        if(mainResourceTypes.containsValue(resourceType))
            return getObjectExtraFile(storeDir, itemId.getItemDescriptor(), Integer.toString(itemId.getId()), null);
        if(resourceTypesWithOwnDirectory.contains(resourceType))
            return getResourceFile(storeDir, itemId.getItemDescriptor(), resourceType.getName(), Integer.toString(itemId.getId()));
        return getObjectExtraFile(storeDir, itemId.getItemDescriptor(), Integer.toString(itemId.getId()), resourceType.getName());
    }

    @Override
    public Collection<File> getAllExistingFilesOfType(File storeDir, StorableResourceType resourceType) {
        List<File> filesOfType = new ArrayList<File>();
        for(ElementsItemType.SubType subType : resourceType.getSupportedSubTypes()){
            filesOfType.addAll(getAllExistingFilesOfType(storeDir, resourceType, subType));
        }
        return filesOfType;
    }

    @SuppressWarnings("ConstantConditions")
    public Collection<File> getAllExistingFilesOfType(File storeDir, StorableResourceType resourceType, ElementsItemType.SubType subType) {
        if(subType.getMainType() != resourceType.getKeyItemType()) throw new IllegalStateException("requested subtype must match resource item type");
        List<File> filesOfType = new ArrayList<File>();
        final String resourceTypeDescriptor = resourceType.getName();
        String subTypeDescriptor = subType.getSingular();
        if(resourceTypesWithOwnDirectory.contains(resourceType)){
            File dir = new File(storeDir, subTypeDescriptor + "-" + resourceTypeDescriptor);
            if(dir.exists())
                filesOfType.addAll(Arrays.asList(dir.listFiles()));
        }
        if(mainResourceTypes.containsValue(resourceType)){
            File dir = new File(storeDir, subTypeDescriptor);
            if(dir.exists()) {
                filesOfType.addAll(Arrays.asList(dir.listFiles(
                        new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return !name.contains("-");
                            }
                        }
                )));
            }
        }
        else{
            File dir = new File(storeDir, subTypeDescriptor);
            if(dir.exists()) {
                filesOfType.addAll(Arrays.asList(dir.listFiles(
                        new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.endsWith("-" + resourceTypeDescriptor);
                            }
                        }
                )));
            }
        }

        return filesOfType;
    }

    private File getObjectExtraFile(File storeDir, String categoryDescriptor, String id, String type) {
        File file = storeDir;
        if (storeDir == null || categoryDescriptor == null) {
            throw new IllegalStateException();
        }

        file = new File(file, categoryDescriptor);
        if (!file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.mkdirs();
        }

        if (!StringUtils.isEmpty(type)) {
            return new File(file, id + "-" + type);
        } else {
            return new File(file, id);
        }
    }


    private File getResourceFile(File storeDir, String categoryDescriptor, String resourceLabel, String id) {
        File file = storeDir;
        if (storeDir == null || categoryDescriptor == null) {
            throw new IllegalStateException();
        }

        file = new File(file, categoryDescriptor + "-" + resourceLabel);
        if (!file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.mkdirs();
        }

        return new File(file, id);
    }
}
