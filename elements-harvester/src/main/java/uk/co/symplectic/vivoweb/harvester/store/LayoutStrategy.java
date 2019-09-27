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

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.io.File;
import java.util.Collection;

/**
 * Interface to define methods that affect how a Store places data on disk for Elements items assuming a given
 * a base directory. Additional methods to search for all files of a particular Type/SubType held in the store
 */

interface LayoutStrategy {

    File getItemFile(File storeDir, ElementsItemId itemId, StorableResourceType resourceType);

    Collection<File> getAllExistingFilesOfType(File storeDir, StorableResourceType resourceType);

    Collection<File> getAllExistingFilesOfType(File storeDir, StorableResourceType resourceType, ElementsItemType.SubType subType);

}
