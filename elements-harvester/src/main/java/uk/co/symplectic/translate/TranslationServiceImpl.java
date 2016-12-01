/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.translate;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.utils.ExecutorServiceUtils;
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsRdfStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Static implementation of an Executor based translation service.
 *
 * Package private, as this is not part of the public API.
 *
 * Users should access via the TranslationService() object.
 */
final class TranslationServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(TranslationServiceImpl.class);

    private static final ExecutorServiceUtils.ExecutorServiceWrapper wrapper = ExecutorServiceUtils.newFixedThreadPool("TranslationService", Boolean.class);

    private TranslationServiceImpl() {}

    static Templates compileSource(Source source) {
        try {
            return TranslationServiceImpl.getFactory().newTemplates(source);
        } catch (TransformerConfigurationException e) {
            log.error("Unable to compile ", e);
            throw new IllegalStateException("");
        }
    }

    static TransformerFactory getFactory() {
        TransformerFactory factory = null;
        try {
            factory =  TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        } catch (TransformerFactoryConfigurationError transformerFactoryConfigurationError) {
            log.warn("Unable to obtain Saxon XSLT factory. Attempting fallback to default.", transformerFactoryConfigurationError);
        }
        //TODO: remove this fallback - xalan WILL NOT WORK!
        if (null == factory) {
            factory = TransformerFactory.newInstance();
        }

        if (null == factory) {
            throw new IllegalStateException("Unable to obtain a TransformerFactory instance");
        }

        return factory;
    }

    static void translate(TranslationServiceConfig config, ElementsStoredItem input, ElementsItemStore output, StorableResourceType outputType, TemplatesHolder translationTemplates, Map<String, Object> extraParams) {
        Future<Boolean> result = wrapper.submit(new ItemTranslateTask(config, input, output, outputType, translationTemplates, extraParams));
    }

    static void awaitShutdown() {
        wrapper.awaitShutdown();
    }

    static class ItemTranslateTask implements Callable<Boolean>{

        private final TemplatesHolder translationTemplates;
        //TODO: unstitch config layer?
        private final TranslationServiceConfig config;
        private final Map<String, Object> extraParams;
        private final ElementsStoredItem inputItem;
        private final ElementsItemStore outputStore;
        private final StorableResourceType outputType;

        private InputStream getInputStream() throws IOException{
            return inputItem.getInputStream();
        }

        private String getInputDescription(){
            return inputItem.getItemInfo().getItemId().toString();
        }

        private void storeOutput(byte[] translatedData) throws IOException{
            outputStore.storeItem(inputItem.getItemInfo(), outputType, translatedData);
        }
        private String getOutputDescription(){return "RDF store";}

        ItemTranslateTask(TranslationServiceConfig config, ElementsStoredItem inputItem, ElementsItemStore outputStore,
                          StorableResourceType outputType, TemplatesHolder translationTemplates, Map<String, Object> extraParams) {
            if(translationTemplates == null) throw new NullArgumentException("translationTemplates");
            if(config == null) throw new NullArgumentException("config");
            if(inputItem == null) throw new NullArgumentException("inputItem");
            if(outputStore == null) throw new NullArgumentException("outputStore");
            if(outputType == null) throw new NullArgumentException("outputType");
            if(inputItem.getResourceType().getKeyItemType() != outputType.getKeyItemType()) throw new IllegalArgumentException("outputType must be compatible with input item type");

            this.inputItem = inputItem;
            this.outputStore = outputStore;
            this.outputType = outputType;
            this.translationTemplates = translationTemplates;
            this.config = config;
            this.extraParams = extraParams;
        }

        public Boolean call() throws Exception {
            Boolean retCode = Boolean.TRUE;
            StreamSource xmlSource = null;

            boolean tolerateIOErrors = config.getTolerateIndividualIOErrors();
            boolean tolerateTransformErrors = config.getTolerateIndividualTransformErrors();

            try {
                xmlSource = new StreamSource(getInputStream());
            } catch (IOException e) {
                log.error(MessageFormat.format("Unable to open input stream on {0}", getInputDescription()), e);
                if (!tolerateIOErrors) throw e;
                //otherwise
                retCode = Boolean.FALSE;
            }

            if (xmlSource != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    StreamResult outputResult = new StreamResult(baos);

                    Transformer transformer = translationTemplates.getTemplates().newTransformer();
                    transformer.setErrorListener(new TranslateTaskErrorListener(config));

                    Map<String, Object> parameters = new HashMap<String, Object>();
                    parameters.putAll(config.getXslParameters());
                    if(extraParams != null) parameters.putAll(extraParams);

                    for (String key : parameters.keySet()) {
                        try {
                            transformer.setParameter(key, parameters.get(key));
                        } catch (RuntimeException re) {
                            //TODO : handle better here?
                        }
                    }

                    transformer.transform(xmlSource, outputResult);

                    String xml = baos.toString("utf-8");
                    if (!config.getUseFullUTF8()) {
                        xml = xml.replaceAll("[^\\u0000-\\uFFFF]", "\uFFFD");
                    }
                    storeOutput(xml.getBytes("utf-8"));

                } catch (IOException e) {
                    log.error(MessageFormat.format("Unable to write to {0}", getOutputDescription()), e);
                    if (!tolerateIOErrors) throw e;
                    retCode = Boolean.FALSE;
                } catch (TransformerException e) {
                    log.error(MessageFormat.format("Unable to perform translation on {0}", getInputDescription()), e);
                    if (!tolerateTransformErrors) throw e;
                    retCode = Boolean.FALSE;
                } catch (IndexOutOfBoundsException e){
                    log.error(MessageFormat.format("Unexpected error performing translation on {0}",getInputDescription()),e);
                    //TODO: SOLVE THE ISSUES SAXON SEEMS TO HAVE? OR MAKE THIS AN OFFICIAL SWITCH?
                    //throw e;
                }
                finally {
                    try {
                        xmlSource.getInputStream().close();
                    } catch (IOException e) {
                        log.error(MessageFormat.format("Unable to close input stream on {0}", getInputDescription()), e);
                        if (!tolerateIOErrors) throw e;
                        retCode = Boolean.FALSE;
                    }
                }
            }
            return retCode;
        }

        //TODO : remove this entirely?
        private class TranslateTaskErrorListener implements ErrorListener {
            TranslationServiceConfig config;

            TranslateTaskErrorListener(TranslationServiceConfig config) {
                this.config = config == null ? new TranslationServiceConfig() : config;
            }

            @Override
            public void warning(TransformerException exception) throws TransformerException {
                throw exception;
            }

            @Override
            public void error(TransformerException exception) throws TransformerException {
                Throwable cause = exception.getCause();
                if (config.getIgnoreFileNotFound() && cause instanceof FileNotFoundException) {
                    log.trace("Ignoring file not found in transform");
                } else {
                    log.error("Transformer Exception", exception);
                    throw exception;
                }
            }

            @Override
            public void fatalError(TransformerException exception) throws TransformerException {
                throw exception;
            }
        }
    }
}
