/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.translate.TemplatesHolder;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.store.*;

import javax.xml.transform.Source;
import java.util.Map;

public abstract class ElementsTranslateObserver extends ElementsStoreOutputItemObserver {

    private static final Logger log = LoggerFactory.getLogger(ElementsTranslateObserver.class);

    //TODO: fix object as static thing here
    private final TranslationService translationService = new TranslationService();
    private TemplatesHolder templatesHolder = null;

    protected ElementsTranslateObserver(ElementsRdfStore rdfStore, String xslFilename, StorableResourceType inputType, StorableResourceType outputType) {
        //todo: can't reference translationService before super has been called... need to move tolerateIOErrors somewhere better..
        super(rdfStore, inputType, outputType, false);
        if(StringUtils.trimToNull(xslFilename) == null) throw new NullArgumentException("xslFilename");

        templatesHolder = new TemplatesHolder(xslFilename);
        //TODO : migrate these Configuration access bits somehow?
        translationService.getConfig().setIgnoreFileNotFound(true);
        //translationService.getConfig().addXslParameter("baseURI", Configuration.getBaseURI());
        Map<String, String> params = Configuration.getXslParameters();
        for(String paramName : params.keySet()){
            String paramValue = params.get(paramName);
            translationService.getConfig().addXslParameter(paramName, paramValue);
        }
        //translationService.getConfig().addXslParameter("recordDir", Configuration.getRawOutputDir());
        translationService.getConfig().setUseFullUTF8(Configuration.getUseFullUTF8());

    }

    protected void translate(ElementsStoredItemInfo item, Map<String, Object> extraParams){
        translationService.translate(item, getStore(), getOutputType(), templatesHolder, extraParams);
    }

    protected void translate(ElementsStoredItemInfo item, Source inputSource, Map<String, Object> extraParams){
        translationService.translate(item, inputSource, getStore(), getOutputType(), templatesHolder, extraParams);
    }
}

