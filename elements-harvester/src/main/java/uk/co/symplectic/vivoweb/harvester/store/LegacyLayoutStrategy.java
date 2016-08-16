/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class LegacyLayoutStrategy implements LayoutStrategy {

    Map<ElementsItemType, StorableResourceType> mainResourceTypes = new HashMap<ElementsItemType, StorableResourceType>();
    Set<StorableResourceType> resourceTypesWithOwnDirectory = new HashSet<StorableResourceType>();

    public LegacyLayoutStrategy(StorableResourceType[] mainResourceTypes, StorableResourceType[] resourceTypesWithOwnDirectory){
        if(mainResourceTypes != null) {
            for (StorableResourceType type : mainResourceTypes) {
                if (this.mainResourceTypes.put(type.getKeyItemType(), type) != null)
                    throw new IllegalArgumentException("cannot have multiple mainTypes of the same ElementsItemType in a layoutStrategy");
            }
        }
        if(resourceTypesWithOwnDirectory != null)
            this.resourceTypesWithOwnDirectory.addAll(Arrays.asList(resourceTypesWithOwnDirectory));
    }

    @Override
    public File getItemFile(File storeDir, ElementsItemInfo itemInfo, StorableResourceType resourceType) {
        if(mainResourceTypes.containsValue(resourceType))
            return getObjectExtraFile(storeDir, itemInfo.getItemDescriptor(), itemInfo.getId(), null);
        if(resourceTypesWithOwnDirectory.contains(resourceType))
            return getResourceFile(storeDir, itemInfo.getItemDescriptor(), resourceType.getName(), itemInfo.getId());
        return getObjectExtraFile(storeDir, itemInfo.getItemDescriptor(), itemInfo.getId(), resourceType.getName());
    }


    public Collection<File> getAllExistingFilesOfType(File storeDir, StorableResourceType resourceType) {
        List<File> filesOfType = new ArrayList<File>();
        for (String categoryDescriptor : ElementsItemInfo.validItemDescriptorsForType(resourceType.getKeyItemType())) {
            if (resourceTypesWithOwnDirectory.contains(resourceType)) {
                File dir = storeDir;
                if (dir.exists()) {
                    final String filePrefix = categoryDescriptor + "-" + resourceType.getName();
                    filesOfType.addAll(Arrays.asList(dir.listFiles(
                            new FilenameFilter() {
                                @Override
                                public boolean accept(File dir, String name) {
                                    return name.startsWith(filePrefix);
                                }
                            }
                    )));
                }
            }
            if (mainResourceTypes.containsValue(resourceType)) {
                File dir = storeDir;
                if (dir.exists()) {
                    final String filePrefix = categoryDescriptor;
                    filesOfType.addAll(Arrays.asList(dir.listFiles(
                            new FilenameFilter() {
                                @Override
                                public boolean accept(File dir, String name) {
                                    return name.startsWith(filePrefix) && !name.contains("-");
                                }
                            }
                    )));
                }
            } else {
                File dir = storeDir;
                if (dir.exists()) {
                    final String filePrefix = categoryDescriptor;
                    final String fileSuffix = "-" + resourceType.getName();
                    filesOfType.addAll(Arrays.asList(dir.listFiles(
                            new FilenameFilter() {
                                @Override
                                public boolean accept(File dir, String name) {
                                    return name.startsWith(filePrefix) && name.endsWith(fileSuffix);
                                }
                            }
                    )));
                }
            }
        }
        return filesOfType;
    }

    private File getObjectExtraFile(File storeDir, String categoryDescriptor, String id, String type) {
        if (storeDir == null || categoryDescriptor == null) {
            throw new IllegalStateException();
        }

        if (!storeDir.exists()) {
            storeDir.mkdirs();
        }

        if (!StringUtils.isEmpty(type)) {
            return new File(storeDir, categoryDescriptor + id + "-" + type);
        } else {
            return new File(storeDir, categoryDescriptor + id);
        }
    }


    private File getResourceFile(File storeDir, String categoryDescriptor, String resourceLabel, String id) {
        if (storeDir == null || categoryDescriptor == null) {
            throw new IllegalStateException();
        }

        if (!storeDir.exists()) {
            storeDir.mkdirs();
        }

        return new File(storeDir, categoryDescriptor + "-" + resourceLabel + id);
    }

    public String getRootNodeForType(String type) {
        return type;
    }
}
