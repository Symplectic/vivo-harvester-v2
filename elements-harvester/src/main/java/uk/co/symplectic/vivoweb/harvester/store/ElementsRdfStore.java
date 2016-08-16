/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.utils.DeletionService;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class ElementsRdfStore extends ElementsObjectFileStore {

    private static LayoutStrategy layoutStrategy = new DefaultLayoutStrategy(
            new StorableResourceType[]{StorableResourceType.TRANSLATED_OBJECT, StorableResourceType.TRANSLATED_RELATIONSHIP}, null);

    private DeletionService deletionService = new DeletionService();

    public ElementsRdfStore(String dir){ this(dir, false); }

    public ElementsRdfStore(String dir, boolean keepEmpty){
        super(dir, keepEmpty, ElementsRdfStore.layoutStrategy,
            StorableResourceType.TRANSLATED_OBJECT, StorableResourceType.TRANSLATED_RELATIONSHIP, StorableResourceType.TRANSLATED_USER_PHOTO_DESCRIPTION);
    }

    public void pruneExcept(ElementsObjectCategory category, Set<ElementsObjectId> idsToKeep) {
        Set<String> stringIdsToKeep = new HashSet<String>();
        for(ElementsObjectId id : idsToKeep){
            if(id.getCategory() != category) throw new IllegalStateException();
            ElementsObjectInfo objectInfo = ElementsObjectInfoCache.get(category, id.getId());
            if(objectInfo == null) objectInfo = ElementsItemInfo.createObjectItem(category, id.getId());
            for(ElementsStoredItem item : retrieveAllItems(objectInfo)){
                stringIdsToKeep.add(item.getFile().getName());
            }
        }
        if (dir != null) {
            File objectDir = new File(dir, category.getSingular());
            if (objectDir.exists()) {
                pruneIn(objectDir, stringIdsToKeep, null);
            } else {
                pruneIn(dir, stringIdsToKeep, category.getSingular());
            }
        }
    }

    private void pruneIn(File dir, Set<String> idsToKeep, String prefix) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    pruneIn(file, idsToKeep, prefix);
                } else if (StringUtils.isEmpty(prefix)) {
                    if (idsToKeep.contains(file.getName())) {
                        deletionService.keep(file);
                    } else {
                        deletionService.deleteOnExit(file);
                    }
                } else if (file.getName().startsWith(prefix)) {
                    boolean keepFile = false;
                    for (String id : idsToKeep) {
                        if (file.getName().equals(prefix + id)) {
                            keepFile = true;
                        }
                    }

                    if (keepFile) {
                        deletionService.keep(file);
                    } else {
                        deletionService.deleteOnExit(file);
                    }
                }
            }
        }
    }

}
