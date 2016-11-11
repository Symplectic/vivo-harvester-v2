/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.translate;

import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsRdfStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

public class ElementsObjectTranslateObserver extends ElementsTranslateObserver{
    public ElementsObjectTranslateObserver(ElementsRdfStore rdfStore, String xslFilename){super(rdfStore, xslFilename, StorableResourceType.RAW_OBJECT); }
    @Override
    protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item) { translate(item, null); }
}
