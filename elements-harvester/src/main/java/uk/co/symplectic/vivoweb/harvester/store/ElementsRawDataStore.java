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

import java.io.File;

/**
 * An ElementsItemFileStore, specifically configured (in terms of accepted types, layout strategy, etc)
 * to act as the raw data store for the harvester
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ElementsRawDataStore extends ElementsItemFileStore {
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
