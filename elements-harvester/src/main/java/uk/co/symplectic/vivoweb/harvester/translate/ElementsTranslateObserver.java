/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.translate.TemplatesHolder;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.store.ElementsRdfStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.IElementsStoredItemObserver;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.util.Map;

/**
 * Created by ajpc2_000 on 07/09/2016.
 */
public abstract class ElementsTranslateObserver extends IElementsStoredItemObserver.ElementsStoredResourceObserverAdapter {
    //TODO: fix object as static thing here
    private final TranslationService translationService = new TranslationService();
    private TemplatesHolder templatesHolder = null;
    private ElementsRdfStore rdfStore = null;

    protected ElementsTranslateObserver(ElementsRdfStore rdfStore, String xslFilename, StorableResourceType resourceType) {
        super(resourceType);
        if(rdfStore == null) throw new NullArgumentException("rdfStore");
        if(StringUtils.trimToNull(xslFilename) == null) throw new NullArgumentException("xslFilename");

        this.rdfStore = rdfStore;

        templatesHolder = new TemplatesHolder(xslFilename);
        //TODO : migrate these Configuration access bits somehow?
        translationService.getConfig().setIgnoreFileNotFound(true);
        translationService.getConfig().addXslParameter("baseURI", Configuration.getBaseURI());
        //translationService.getConfig().addXslParameter("recordDir", Configuration.getRawOutputDir());
        translationService.getConfig().setUseFullUTF8(Configuration.getUseFullUTF8());

    }

    protected void translate(ElementsStoredItem item, Map<String, Object> extraParams){
        translationService.translate(item, rdfStore, templatesHolder, extraParams);
    }
}

