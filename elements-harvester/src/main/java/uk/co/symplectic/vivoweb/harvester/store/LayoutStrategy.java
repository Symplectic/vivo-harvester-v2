/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.store;

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.io.File;
import java.util.Collection;

public interface LayoutStrategy {

    File getItemFile(File storeDir, ElementsItemId itemId, StorableResourceType resourceType);

    Collection<File> getAllExistingFilesOfType(File storeDir, StorableResourceType resourceType);

    Collection<File> getAllExistingFilesOfType(File storeDir, StorableResourceType resourceType, ElementsItemType.SubType subType);

    //todo: remove this!? once decide what to do with legacy layout?
    String getRootNodeForType(String type);
}
