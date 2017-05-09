/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.translate;

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsRdfStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItemInfo;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;
import java.text.MessageFormat;

public class ElementsObjectTranslateObserver extends ElementsTranslateObserver{
    public ElementsObjectTranslateObserver(ElementsRdfStore rdfStore, String xslFilename){
        super(rdfStore, xslFilename, StorableResourceType.RAW_OBJECT, StorableResourceType.TRANSLATED_OBJECT);
    }

    @Override
    protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItemInfo item) { translate(item, null); }

    @Override
    protected void observeObjectDeletion(ElementsItemId.ObjectId objectId, StorableResourceType type){
        safelyDeleteItem(objectId, MessageFormat.format("Unable to delete translated-rdf for object {0}", objectId.toString()));
    }
}
