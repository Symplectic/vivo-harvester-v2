/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.translate.TemplatesHolder;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.model.ElementsGroupInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsRelationshipInfo;
import uk.co.symplectic.vivoweb.harvester.store.*;

//TODO : ? merge this and object translate as now so similar?
public class ElementsRelationshipTranslateObserver extends IElementsStoredItemObserver.ElementsStoredResourceObserverAdapter {
    //TODO: fix object as static thing here
    private final TranslationService translationService = new TranslationService();
    private TemplatesHolder templatesHolder = null;
    private ElementsRdfStore rdfStore = null;

    public ElementsRelationshipTranslateObserver(ElementsRdfStore rdfStore, String xslFilename) {
        super(StorableResourceType.RAW_RELATIONSHIP);
        if(rdfStore == null) throw new NullArgumentException("rdfStore");
        if(xslFilename == null) throw new NullArgumentException("xslFilename");

        this.rdfStore = rdfStore;

        //TODO: is this sensible - may want an ILLEGAL ARG instead
        if (!StringUtils.isEmpty(xslFilename)) {
            templatesHolder = new TemplatesHolder(xslFilename);
            translationService.getConfig().setIgnoreFileNotFound(true);
            //TODO : migrate these Configuration access bits somehow?
            translationService.getConfig().addXslParameter("baseURI", Configuration.getBaseURI());
            //translationService.getConfig().addXslParameter("recordDir", Configuration.getRawOutputDir());
            translationService.getConfig().setUseFullUTF8(Configuration.getUseFullUTF8());
        }
    }

    @Override
    protected void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItem item) {
        translationService.translate(item, rdfStore, templatesHolder);
    }
}
