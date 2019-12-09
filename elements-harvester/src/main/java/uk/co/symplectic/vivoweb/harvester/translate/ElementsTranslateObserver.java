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

package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.translate.TemplatesHolder;
import uk.co.symplectic.translate.TranslationDocumentProvider;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.store.*;

import javax.xml.transform.Source;
import java.util.Map;

/**
 * Abstract sub class of ElementsStoreOutputItemObserver that represents the concept of an observer that monitors
 * for a particular type of data (StorableResourceType inputType) being added to the store it is observing,
 * translates that data to a different type of data (StorableResourceType outputType) and stores the translated output
 * in another store (ElementsRdfStore rdfStore)
 *
 * Sets sensible default configuration up in the TranslationService (e.g. setting globally applied xsl parameters)
 * Alo provides two translate methods to easily enqueue work  in the TranslationService, but does not override any of the
 * "observe" methods from its parent class.
 *
 * Concrete subclasses provide actual implementation details of the translations to be enqueued.
 */
abstract class ElementsTranslateObserver extends ElementsStoreOutputItemObserver {

    //TODO: fix object as static thing here
    private final TranslationService translationService = new TranslationService();
    private TemplatesHolder templatesHolder = null;

    ElementsTranslateObserver(ElementsRdfStore rdfStore, String xslFilename, TranslationDocumentProvider groupListDocument, StorableResourceType inputType, StorableResourceType outputType) {
        //todo: can't reference translationService before super has been called... need to move tolerateIOErrors somewhere better..
        super(rdfStore, inputType, outputType, false);
        if(StringUtils.trimToNull(xslFilename) == null) throw new NullArgumentException("xslFilename");

        templatesHolder = new TemplatesHolder(xslFilename);
        //TODO : migrate these Configuration access bits somehow?
        translationService.getConfig().setIgnoreFileNotFound(true);
        if(groupListDocument != null){
            translationService.getConfig().addXslParameter("elementsGroupList", groupListDocument);
        }
        //translationService.getConfig().addXslParameter("baseURI", Configuration.getBaseURI());
        Map<String, String> params = Configuration.getXslParameters();
        for(String paramName : params.keySet()){
            String paramValue = params.get(paramName);
            translationService.getConfig().addXslParameter(paramName, paramValue);
        }
        //translationService.getConfig().addXslParameter("recordDir", Configuration.getRawOutputDir());
        translationService.getConfig().setUseFullUTF8(Configuration.getUseFullUTF8());

    }

    void translate(ElementsStoredItemInfo item, Map<String, Object> extraParams){
        translationService.translate(item, getStore(), getOutputType(), templatesHolder, extraParams);
    }

    @SuppressWarnings("SameParameterValue")
    void translate(ElementsStoredItemInfo item, Source inputSource, Map<String, Object> extraParams){
        translationService.translate(item, inputSource, getStore(), getOutputType(), templatesHolder, extraParams);
    }
}

