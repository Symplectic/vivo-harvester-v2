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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import uk.co.symplectic.translate.TranslationDocumentProvider;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsRelationshipInfo;
import uk.co.symplectic.vivoweb.harvester.store.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

/**
 * Concrete subclass of ElementsTranslateObserver for translating Elements objects from RAW_RELATIONSHIP data to
 * TRANSLATED_RELATIONSHIP data. Overrides the relevant observeStoredRelationship and observeRelationshipDeletion methods
 * and calls into the * the translate/delete methods provided by the super classes to perform the actual work.
 *
 * For the observeStoredRelationship call the method  can pass in an ExtraObjectsDocument as an extraXSLTParameter
 * This document contains raw XML descriptions of the Elements items in the relationship, pulled from the rawDataStore.
 * This action is only performed for relationships of the types listed in "relationshipTypesNeedingObjectsForTranslation". *
 */
public class ElementsRelationshipTranslateObserver extends ElementsTranslateObserver{

    private static final Logger log = LoggerFactory.getLogger(ElementsRelationshipTranslateObserver.class);
    private final ElementsItemFileStore rawDataStore;
    private final Set<String> relationshipTypesNeedingObjectsForTranslation;

    public ElementsRelationshipTranslateObserver(ElementsItemFileStore rawDataStore, ElementsRdfStore rdfStore, String xslFilename, Set<String> relationshipTypesNeedingObjectsForTranslation){
        super(rdfStore, xslFilename, StorableResourceType.RAW_RELATIONSHIP, StorableResourceType.TRANSLATED_RELATIONSHIP);
        if(rawDataStore == null) throw new NullArgumentException("rawDataStore");
        this.rawDataStore = rawDataStore;
        if(relationshipTypesNeedingObjectsForTranslation != null && ! relationshipTypesNeedingObjectsForTranslation.isEmpty()){
            this.relationshipTypesNeedingObjectsForTranslation = new HashSet<String>(relationshipTypesNeedingObjectsForTranslation);
        }else {
            this.relationshipTypesNeedingObjectsForTranslation = null;
        }
    }
    @Override
    protected void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItemInfo item) {
        //If we don't think this is a complete relationships then the current code base can't process it effectively - no point translating it..
        if(!info.getIsComplete()){
            log.warn(MessageFormat.format("{0} appears to be incomplete, this may indicate new Elements object categories have been added.", info.getItemId()));
            return;
        }

        Map<String, Object> extraXSLTParameters = new HashMap<String, Object>();
        if(this.relationshipTypesNeedingObjectsForTranslation == null || this.relationshipTypesNeedingObjectsForTranslation.contains(info.getType())) {
            extraXSLTParameters.put("extraObjects", getExtraObjectsDescription(info));
        }
        //extraXSLTParameters.put("extraObjects", getExtraObjectsDescription(info));
        translate(item, extraXSLTParameters);
    }

    private ExtraObjectsDocument getExtraObjectsDescription(ElementsRelationshipInfo info) {
        return ExtraObjectsDocument.createExtraObjectsDocument(info, rawDataStore);
    }

    @Override
    protected void observeRelationshipDeletion(ElementsItemId.RelationshipId relationshipId, StorableResourceType type){
        safelyDeleteItem(relationshipId, MessageFormat.format("Unable to delete translated-rdf for relationship {0}", relationshipId.toString()));
    }

    /**
     * Class to represent the Extra Object data that is sometimes provided when an ElementsRelationshipTranslateObserver
     * enqueues work with the TranslationService. This is implemented as a subclass of TranslationDocumentProvider
     * To ensure that the extra object data is not loaded into ram until it is actually needed.
     */
    private static class ExtraObjectsDocument implements TranslationDocumentProvider {
        private final List<BasicElementsStoredItem> items = new ArrayList<BasicElementsStoredItem>();
        private final String context;

        private static ExtraObjectsDocument createExtraObjectsDocument(ElementsRelationshipInfo info, ElementsItemFileStore rawDataStore){
            List<BasicElementsStoredItem> items = new ArrayList<BasicElementsStoredItem>();
            for (ElementsItemId.ObjectId id : info.getObjectIds()) {
                BasicElementsStoredItem storedRawObject = rawDataStore.retrieveItem(id, StorableResourceType.RAW_OBJECT);
                if(storedRawObject != null) {
                    items.add(storedRawObject);
                }
                else{
                    log.warn(MessageFormat.format("Extra object {0} not found in raw-record cache when processing {1}", id, info.getItemId()));
                }
            }
            BasicElementsStoredItem itemArray[] = new BasicElementsStoredItem[2];
            itemArray = items.toArray(itemArray);
            return new ExtraObjectsDocument(info.getItemId().toString(), itemArray);
        }

        private ExtraObjectsDocument(String context, BasicElementsStoredItem... items){
            if(items != null) this.items.addAll(Arrays.asList(items));
            this.context = context;
        }

        public Document getDocument() {
            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.newDocument();
                Element mainDocRootElement = doc.createElement("extraObjects");
                doc.appendChild(mainDocRootElement);
                for (BasicElementsStoredItem storedRawObject : items) {
                    if(storedRawObject != null) {
                        StoredData storedRawData = storedRawObject.getStoredData();
                        InputStream storedItemInputStream = null;
                        try {
                            storedItemInputStream = storedRawData.getInputStream();
                            Document storedObjectDoc = docBuilder.parse(storedRawData.getInputStream());
                            Element storedObjectRootElement = storedObjectDoc.getDocumentElement();
                            Node importedNode = doc.importNode(storedObjectRootElement, true);
                            mainDocRootElement.appendChild(importedNode);
                        } catch (FileNotFoundException doh) {
                            //todo: decide if this is desirable or not - needed to avoid failures in relation to data categories you are not really processing at the moment.
                            if (storedRawData instanceof StoredData.InFile) {
                                StoredData.InFile fileStoredData = (StoredData.InFile) storedRawData;
                                log.warn(MessageFormat.format("File {0} for extra object {1} not found when processing {2}", fileStoredData.getFile().getPath(), storedRawObject.getItemId(), context));
                            }
                        } finally {
                            if (storedItemInputStream != null) {
                                storedItemInputStream.close();
                            }
                        }
                    }
                }
                return doc;
            }
            catch(IOException ioe){
                throw new IllegalStateException(ioe);
            }
            catch(SAXException se){
                throw new IllegalStateException(se);
            }
            catch (ParserConfigurationException pce) {
                throw new IllegalStateException(pce);
            }
        }
    }
}

