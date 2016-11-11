/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.store;

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;

import java.io.File;
import java.util.Collection;

public interface LayoutStrategy {

    public File getItemFile(File storeDir, ElementsItemId itemId, StorableResourceType resourceType);

    public Collection<File> getAllExistingFilesOfType(File storeDir, StorableResourceType resourceType);

    //todo: remove this!? once decide what to do with legacy layout?
    public String getRootNodeForType(String type);
}
