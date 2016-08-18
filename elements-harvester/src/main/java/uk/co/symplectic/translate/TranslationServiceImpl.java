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
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.store.*;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

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

        if (null == factory) {
            factory = TransformerFactory.newInstance();
        }

        if (null == factory) {
            throw new IllegalStateException("Unable to obtain a TransformerFactory instance");
        }

        return factory;
    }

    static void translate(TranslationServiceConfig config, ElementsStoredItem input, ElementsRdfStore output, TemplatesHolder translationTemplates, boolean isZipped) {
        Future<Boolean> result = wrapper.submit(new ItemTranslateTask(config, input, output, translationTemplates, isZipped));
    }

//    static void translate(TranslationServiceConfig config, ElementsStoredObject input, ElementsRdfStore output, TemplatesHolder translationTemplates) {
//        Future<Boolean> result = wrapper.submit(new ObjectTranslateTask(config, input, output, translationTemplates));
//    }

//    static void translate(TranslationServiceConfig config, ElementsStoredRelationship input, ElementsRdfStore output, TemplatesHolder translationTemplates) {
//        Future<Boolean> result = wrapper.submit(new RelationshipTranslateTask(config, input, output, translationTemplates));
//    }

    static void shutdown() {
        wrapper.shutdown();
    }

    static abstract class AbstractTranslateTask implements Callable<Boolean>{

        private TemplatesHolder translationTemplates;
        //TODO: unstitch config layer?
        private TranslationServiceConfig config;

        protected abstract InputStream getInputStream() throws IOException;
        protected String getInputDescription(){return "input";}

        protected abstract void storeOutput(byte[] translatedData) throws IOException;
        protected String getOutputDescription(){return "output";}

        protected AbstractTranslateTask(TranslationServiceConfig config, TemplatesHolder translationTemplates){
            if(translationTemplates == null) throw new NullArgumentException("translationTemplates");
            if(config == null) throw new NullArgumentException("config");
            this.translationTemplates = translationTemplates;
            this.config = config;
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

                    for (String key : config.getXslParameters().keySet()) {
                        try {
                            transformer.setParameter(key, config.getXslParameters().get(key));
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

    static class ItemTranslateTask extends AbstractTranslateTask{
        private ElementsStoredItem inputItem;
        private ElementsRdfStore outputStore;
        private boolean inputIszipped = true;

        ItemTranslateTask(TranslationServiceConfig config, ElementsStoredItem inputItem, ElementsRdfStore outputStore, TemplatesHolder translationTemplates, boolean inputIsZipped) {
            super(config, translationTemplates);
            if(inputItem == null) throw new NullArgumentException("inputItem");
            if(outputStore == null) throw new NullArgumentException("outputStore");

            this.inputItem = inputItem;
            this.outputStore = outputStore;
            this.inputIszipped = inputIsZipped;
        }

        @Override
        protected String getInputDescription(){
            return inputItem.getItemInfo().getItemDescriptor() + ":" + inputItem.getItemInfo().getId();
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            InputStream stream = new BufferedInputStream(new FileInputStream(inputItem.getFile()));
            if(inputIszipped) stream = new GZIPInputStream(stream);
            return stream;
        }

        @Override
        protected String getOutputDescription(){
            return "RDF store";
        }

        @Override
        protected void storeOutput(byte[] translatedData) throws IOException{
            if(inputItem.getItemInfo().isObjectInfo())
                outputStore.storeItem(inputItem.getItemInfo(), StorableResourceType.TRANSLATED_OBJECT, translatedData);
            else if(inputItem.getItemInfo().isRelationshipInfo())
                outputStore.storeItem(inputItem.getItemInfo(), StorableResourceType.TRANSLATED_RELATIONSHIP, translatedData);
            else
                throw new IllegalStateException("Unstorable item translated");
        }
    }

}
