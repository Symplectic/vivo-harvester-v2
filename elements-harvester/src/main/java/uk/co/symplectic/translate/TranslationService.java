/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.translate;

import uk.co.symplectic.vivoweb.harvester.store.ElementsItemStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItemInfo;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.Map;


/**
 * Public interface to the Translation service, which acts to translate any passed in data asynchronously.
 * Wraps the static implementation in an object, so that it can be mocked / substituted.
 */
@SuppressWarnings("unused")
public final class TranslationService {

    private TranslationServiceConfig config = new TranslationServiceConfig();

    public TranslationService() {}

    public TranslationServiceConfig getConfig(){ return config; }

    static Templates compileSource(File file) {
        return TranslationServiceImpl.compileSource(new StreamSource(file));
    }

    public void translate(ElementsStoredItemInfo input, ElementsItemStore output, StorableResourceType outputType, TemplatesHolder translationTemplates) {
        translate(input, null, output, outputType, translationTemplates, null);
    }

    public void translate(ElementsStoredItemInfo input, Source inputSource, ElementsItemStore output, StorableResourceType outputType, TemplatesHolder translationTemplates) {
        translate(input, inputSource, output, outputType, translationTemplates, null);
    }

    public void translate(ElementsStoredItemInfo input, ElementsItemStore output, StorableResourceType outputType, TemplatesHolder translationTemplates, Map<String, Object> extraParams) {
        translate(input, null, output, outputType, translationTemplates, extraParams);
    }

    public void translate(ElementsStoredItemInfo input, Source inputSource, ElementsItemStore output, StorableResourceType outputType, TemplatesHolder translationTemplates, Map<String, Object> extraParams) {
        TranslationServiceImpl.translate(config, input, inputSource, output, outputType, translationTemplates, extraParams);
    }

    public static void awaitShutdown() {
        TranslationServiceImpl.awaitShutdown();
    }
}
