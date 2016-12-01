/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;

import java.io.IOException;

public class ElementsRdfStore extends ElementsItemFileStore {

    private static LayoutStrategy layoutStrategy = new DefaultLayoutStrategy(
            new StorableResourceType[]{StorableResourceType.TRANSLATED_OBJECT, StorableResourceType.TRANSLATED_RELATIONSHIP, StorableResourceType.TRANSLATED_GROUP}, null);

    public ElementsRdfStore(String dir){ this(dir, false, false); }

    public ElementsRdfStore(String dir, boolean keepEmpty, boolean zipFiles){
        super(dir, keepEmpty, zipFiles, ElementsRdfStore.layoutStrategy,
            StorableResourceType.TRANSLATED_OBJECT, StorableResourceType.TRANSLATED_RELATIONSHIP, StorableResourceType.TRANSLATED_GROUP, StorableResourceType.TRANSLATED_USER_PHOTO_DESCRIPTION);
    }

    //wrapper to decide on the stored resource type...
//    public void storeTranslatedItem(ElementsItemInfo itemInfo, byte[] translatedData) throws IOException{
//        if(itemInfo == null) throw new NullArgumentException("itemInfo");
//
//        if(itemInfo.isObjectInfo())
//            storeItem(itemInfo, StorableResourceType.TRANSLATED_OBJECT, translatedData);
//        else if(itemInfo.isRelationshipInfo())
//            storeItem(itemInfo, StorableResourceType.TRANSLATED_RELATIONSHIP, translatedData);
//        else if(itemInfo.isGroupInfo())
//            storeItem(itemInfo, StorableResourceType.TRANSLATED_GROUP, translatedData);
//        else
//            throw new IllegalStateException("Unstorable item translated");
//    }
}
