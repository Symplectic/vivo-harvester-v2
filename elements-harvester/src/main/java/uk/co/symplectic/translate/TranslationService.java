/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.translate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.vivoweb.harvester.store.ElementsRdfStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.Map;


/**
 * Public interface to the Translation service.
 *
 * Wraps the static implementation in an object, so that it can be mocked / substituted.
 */
public final class TranslationService {
    private static Logger log = LoggerFactory.getLogger(TranslationService.class);

    private TranslationServiceConfig config = new TranslationServiceConfig();

    public TranslationService() {}

    public TranslationServiceConfig getConfig(){ return config; }

    public static Templates compileSource(File file) {
        return TranslationServiceImpl.compileSource(new StreamSource(file));
    }

    public void translate(ElementsStoredItem input, ElementsRdfStore output, TemplatesHolder translationTemplates) {
        translate(input, output, translationTemplates, null);
    }

    public void translate(ElementsStoredItem input, ElementsRdfStore output, TemplatesHolder translationTemplates, Map<String, Object> extraParams) {
        TranslationServiceImpl.translate(config, input, output, translationTemplates, extraParams);
    }

    public static void shutdown() {
        TranslationServiceImpl.shutdown();
    }
}
