/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.translate;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.utils.ExecutorServiceUtils;
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItemInfo;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Static implementation of an Executor based translation service.
 *
 * Package private, as this is not part of the public API.
 *
 * Users should access via the TranslationService() object.
 */
final class TranslationServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(TranslationServiceImpl.class);

    private static final ExecutorServiceUtils.ExecutorServiceWrapper<Boolean> wrapper = ExecutorServiceUtils.newFixedThreadPool("TranslationService");

    private TranslationServiceImpl() {}

    static Templates compileSource(Source source) {
        try {
            return TranslationServiceImpl.getFactory().newTemplates(source);
        } catch (TransformerConfigurationException e) {
            log.error("Unable to compile ", e);
            throw new IllegalStateException("");
        }
    }

    private static TransformerFactory getFactory() {
        TransformerFactory factory = null;
        try {
            factory =  TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        } catch (TransformerFactoryConfigurationError transformerFactoryConfigurationError) {
            log.error("Unable to obtain Saxon XSLT factory.", transformerFactoryConfigurationError);
        }

        if (null == factory) {
            throw new IllegalStateException("Unable to obtain a TransformerFactory instance");
        }
        return factory;
    }

    static void translate(TranslationServiceConfig config, ElementsStoredItemInfo input, Source inputSource, ElementsItemStore output, StorableResourceType outputType, TemplatesHolder translationTemplates, Map<String, Object> extraParams) {
        wrapper.submit(new ItemTranslateTask(config, input, inputSource, output, outputType, translationTemplates, extraParams));
    }

    static void awaitShutdown() {
        wrapper.awaitShutdown();
    }


    static abstract class AbstractTranslateTask implements Callable<Boolean>{

        private final TemplatesHolder translationTemplates;
        //TODO: unstitch config layer?
        private final TranslationServiceConfig config;
        private final Map<String, Object> extraParams;

        protected abstract Source getInputSource() throws IOException;
        protected abstract String getInputDescription();

        protected abstract void storeOutput(byte[] translatedData) throws IOException;
        protected abstract String getOutputDescription();

        AbstractTranslateTask(TranslationServiceConfig config, TemplatesHolder translationTemplates, Map<String, Object> extraParams) {
            if(translationTemplates == null) throw new NullArgumentException("translationTemplates");
            if(config == null) throw new NullArgumentException("config");

            this.translationTemplates = translationTemplates;
            this.config = config;
            this.extraParams = extraParams;
        }

        public Boolean call() throws Exception {
            Boolean retCode = Boolean.TRUE;
            Source xmlSource = null;

            boolean tolerateIOErrors = config.getTolerateIndividualIOErrors();
            boolean tolerateTransformErrors = config.getTolerateIndividualTransformErrors();

            try {
                xmlSource = getInputSource();
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

                    //work around saxon oddness
                    if(xml.equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")) {
                        storeOutput(null);
//                        log.info(MessageFormat.format("no translated output for item {0}", inputItem.getItemInfo().getItemId()));
                    }
                    else {
                        storeOutput(xml.getBytes("utf-8"));
                    }

                } catch (IOException e) {
                    log.error(MessageFormat.format("Unable to write to {0}", getOutputDescription()), e);
                    if (!tolerateIOErrors) throw e;
                    retCode = Boolean.FALSE;
                } catch (TransformerException e) {
                    log.error(MessageFormat.format("Unable to perform translation on {0}", getInputDescription()), e);
                    if (!tolerateTransformErrors) throw e;
                    retCode = Boolean.FALSE;
                }
                finally {
                    try {
                        if(xmlSource instanceof StreamSource) {
                            ((StreamSource) xmlSource).getInputStream().close();
                        }
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

    static class ItemTranslateTask extends AbstractTranslateTask{

        private final ElementsStoredItemInfo inputItem;
        private final Source inputSource;
        private final ElementsItemStore outputStore;
        private final StorableResourceType outputType;

        @Override
        protected Source getInputSource() throws IOException{
            if(inputSource != null)
                return inputSource;
            return new StreamSource(inputItem.getInputStream());
        }

        @Override
        protected String getInputDescription(){
            return inputItem.getItemInfo().getItemId().toString();
        }

        @Override
        protected void storeOutput(byte[] translatedData) throws IOException{
            outputStore.storeItem(inputItem.getItemInfo(), outputType, translatedData);
        }

        @Override
        protected String getOutputDescription(){return "RDF store";}

        ItemTranslateTask(TranslationServiceConfig config, ElementsStoredItemInfo inputItem, Source inputSource, ElementsItemStore outputStore,
                          StorableResourceType outputType, TemplatesHolder translationTemplates, Map<String, Object> extraParams) {
            super(config, translationTemplates, extraParams);
            if(inputItem == null) throw new NullArgumentException("inputItem");
            if(outputStore == null) throw new NullArgumentException("outputStore");
            if(outputType == null) throw new NullArgumentException("outputType");
            if(inputItem.getResourceType().getKeyItemType() != outputType.getKeyItemType()) throw new IllegalArgumentException("outputType must be compatible with input item type");

            this.inputItem = inputItem;
            this.inputSource = inputSource;
            this.outputStore = outputStore;
            this.outputType = outputType;
        }
    }
}
