/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.translate.TemplatesHolder;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.store.*;

import java.util.Set;

public class ElementsObjectTranslateObserver extends IElementsStoredItemObserver.ElementsStoredResourceObserverAdapter {
    //TODO: fix object as static thing here
    private final TranslationService translationService = new TranslationService();
    private TemplatesHolder templatesHolder = null;
    private ElementsRdfStore rdfStore = null;

    public ElementsObjectTranslateObserver(ElementsRdfStore rdfStore, String xslFilename) {
        super(StorableResourceType.RAW_OBJECT);
        if(rdfStore == null) throw new NullArgumentException("rdfStore");
        if(xslFilename == null) throw new NullArgumentException("xslFilename");

        this.rdfStore = rdfStore;

        //TODO : is this sensible - ILLEGAL ARG instead?
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
    protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item) {
        translationService.translate(item, rdfStore, templatesHolder);
    }
}
