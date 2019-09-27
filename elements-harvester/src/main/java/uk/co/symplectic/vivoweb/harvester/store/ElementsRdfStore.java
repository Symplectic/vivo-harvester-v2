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
 * to act as the output rdf data store for the harvester
 */
@SuppressWarnings("unused")
public class ElementsRdfStore extends ElementsItemFileStore {

    private static LayoutStrategy layoutStrategy = new DefaultLayoutStrategy(
            new StorableResourceType[]{StorableResourceType.TRANSLATED_OBJECT, StorableResourceType.TRANSLATED_RELATIONSHIP, StorableResourceType.TRANSLATED_GROUP}, null);

    public ElementsRdfStore(File dir){ this(dir, false, false); }

    public ElementsRdfStore(File dir, boolean keepEmpty, boolean zipFiles){
        super(dir, keepEmpty, zipFiles, ElementsRdfStore.layoutStrategy,
            StorableResourceType.TRANSLATED_OBJECT, StorableResourceType.TRANSLATED_RELATIONSHIP, StorableResourceType.TRANSLATED_GROUP,
                StorableResourceType.TRANSLATED_USER_PHOTO_DESCRIPTION, StorableResourceType.TRANSLATED_USER_GROUP_MEMBERSHIP);
    }
}
