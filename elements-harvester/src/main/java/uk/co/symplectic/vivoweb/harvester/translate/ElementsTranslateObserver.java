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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.translate.TemplatesHolder;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsGroupCollection;
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsObjectKeyedCollection;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
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

    protected void translate(ElementsStoredItem item, Map<String, Object> extraParams){
        translationService.translate(item, rdfStore, templatesHolder, extraParams);
    }

    public static class Objects extends ElementsTranslateObserver{
        public Objects(ElementsRdfStore rdfStore, String xslFilename){super(rdfStore, xslFilename, StorableResourceType.RAW_OBJECT); }
        @Override
        protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item) { translate(item, null); }
    }

    public static class Relationships extends ElementsTranslateObserver{

        private static final Logger log = LoggerFactory.getLogger(Relationships.class);
        private final ElementsObjectFileStore rawDataStore;

        public Relationships(ElementsObjectFileStore rawDataStore, ElementsRdfStore rdfStore, String xslFilename){
            super(rdfStore, xslFilename, StorableResourceType.RAW_RELATIONSHIP);
            if(rawDataStore == null) throw new NullArgumentException("rawDataStore");
            this.rawDataStore = rawDataStore;
        }
        @Override
        protected void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItem item) {
            Map<String, Object> extraXSLTParameters = new HashMap<String, Object>();
            extraXSLTParameters.put("extraObjects", getExtraObjectsDescription(info));
            translate(item, extraXSLTParameters);
        }

        private Document getExtraObjectsDescription(ElementsRelationshipInfo info) {

            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.newDocument();
                Element mainDocRootElement = doc.createElement("extraObjects");
                doc.appendChild(mainDocRootElement);
                for (ElementsItemId.ObjectId id : info.getObjectIds()) {
                    ElementsItemInfo storedItemInfo = ElementsItemInfo.createObjectItem(id.getCategory(), id.getId());
                    ElementsStoredItem storedRawObject = rawDataStore.retrieveItem(storedItemInfo, StorableResourceType.RAW_OBJECT);
                    InputStream storedItemInputStream = null;
                    try {
                        storedItemInputStream = storedRawObject.getInputStream();
                        Document storedObjectDoc = docBuilder.parse(storedRawObject.getInputStream());
                        Element storedObjectRootElement = storedObjectDoc.getDocumentElement();
                        Node importedNode = doc.importNode(storedObjectRootElement, true);
                        mainDocRootElement.appendChild(importedNode);
                    }
                    catch(FileNotFoundException fnfe){
                        //todo: decide if this is desirable or not - needed to avoid failures on relations to cata categories you are not really processing at the moment.
                        if(storedRawObject instanceof ElementsStoredItem.InFile) {
                            ElementsStoredItem.InFile fileStoredItem = (ElementsStoredItem.InFile) storedRawObject;
                            log.warn(MessageFormat.format("File {0} for extra object {1} not found when processing {2}", fileStoredItem.getFile().getPath(), id, info.getItemId()));
                        }
                    }
                    finally{
                        if(storedItemInputStream != null){
                            storedItemInputStream.close();
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

    public static class Groups extends ElementsTranslateObserver {

        private final ElementsGroupCollection groupCache;
        private final ElementsObjectKeyedCollection.ObjectInfo includedUsers;

        public Groups(ElementsRdfStore rdfStore, String xslFilename, ElementsGroupCollection groupCache, ElementsObjectKeyedCollection.ObjectInfo includedUsers){
            super(rdfStore, xslFilename, StorableResourceType.RAW_GROUP);
            if (groupCache == null) throw new NullArgumentException("groupCache");
            if (includedUsers == null) throw new NullArgumentException("includedUsers");
            this.groupCache = groupCache;
            this.includedUsers = includedUsers;
        }
        @Override
        protected void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItem item) {
            Map<String, Object> extraXSLTParameters = new HashMap<String, Object>();
            extraXSLTParameters.put("groupMembers", getGroupMembershipDescription(info));
            translate(item, extraXSLTParameters);
        }

        private Document getGroupMembershipDescription(ElementsGroupInfo info){
            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.newDocument();
                Element rootElement = doc.createElement("groupMembers");
                doc.appendChild(rootElement);
                ElementsGroupInfo.GroupHierarchyWrapper groupDescription = groupCache.getGroup(info.getItemId().getId());
                for(ElementsItemId.ObjectId user : groupDescription.getExplicitUsers()){
                    //if the group member is in the set of included users we need to inform the translation stage about that membership.
                    if(includedUsers.keySet().contains(user)) {
                        //create an Element to reference the user we are processing.
                        ElementsUserInfo userInfo = (ElementsUserInfo) includedUsers.get(user);
                        Element userElement = doc.createElement("user");

                        //create id  and username attributes on our user Element
                        userElement.setAttribute("id", Integer.toString(userInfo.getObjectId().getId()));
                        userElement.setAttribute("username", userInfo.getUsername());

                        //add our  user Element to the root of the doc;
                        rootElement.appendChild(userElement);
                    }
                }
                return doc;
            }
            catch (ParserConfigurationException pce) {
                throw new IllegalStateException(pce);
            }
        }
    }
}

