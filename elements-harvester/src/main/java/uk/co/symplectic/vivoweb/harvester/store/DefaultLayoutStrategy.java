/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class DefaultLayoutStrategy implements LayoutStrategy {

    Map<ElementsItemType, StorableResourceType> mainResourceTypes = new HashMap<ElementsItemType, StorableResourceType>();
    Set<StorableResourceType> resourceTypesWithOwnDirectory = new HashSet<StorableResourceType>();

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
        final String resourceTypeDescriptor = resourceType.getName();
        for(String categoryDescriptor : ElementsItemInfo.validItemDescriptorsForType(resourceType.getKeyItemType())){
            if(resourceTypesWithOwnDirectory.contains(resourceType)){
                File dir = new File(storeDir, categoryDescriptor + "-" + resourceTypeDescriptor);
                if(dir.exists())
                    filesOfType.addAll(Arrays.asList(dir.listFiles()));
            }
            if(mainResourceTypes.containsValue(resourceType)){
                File dir = new File(storeDir, categoryDescriptor);
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
                File dir = new File(storeDir, categoryDescriptor);
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
            file.mkdirs();
        }

        return new File(file, id);
    }

    public String getRootNodeForType(String type) {
        return "entry";
    }

}
