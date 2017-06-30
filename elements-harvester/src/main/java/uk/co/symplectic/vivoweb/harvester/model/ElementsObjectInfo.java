/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.model;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.utils.xml.XMLEventProcessor;
import uk.co.symplectic.vivoweb.harvester.app.ElementsFetchAndTranslate;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import java.text.MessageFormat;

import static uk.co.symplectic.elements.api.ElementsAPI.apiNS;
import static uk.co.symplectic.elements.api.ElementsAPI.atomNS;

public class ElementsObjectInfo extends ElementsItemInfo{

    public static class Extractor extends XMLEventProcessor.ItemExtractingFilter<ElementsItemInfo>{

        final private static Logger log = LoggerFactory.getLogger(Extractor.class);

        private static DocumentLocation fileEntryLocation = new DocumentLocation(new QName(atomNS, "entry"), new QName(apiNS, "object"));
        private static DocumentLocation feedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "object"));
        private static DocumentLocation feedDeletedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "deleted-object"));

        public static Extractor getExtractor(ElementsItemInfo.ExtractionSource source, int maximumExpected){
            switch(source) {
                case FEED : return new Extractor(feedEntryLocation, maximumExpected);
                case DELETED_FEED : return new Extractor(feedDeletedEntryLocation, maximumExpected);
                case FILE : return new Extractor(fileEntryLocation, maximumExpected);
                default : throw new IllegalStateException("invalid extractor source type requested");
            }
        }

        public static void InitialiseGenericFieldExtraction(String aGenericFieldName){
            String trimmedValue = StringUtils.trimToNull(aGenericFieldName);
            if(trimmedValue == null) log.info("Generic field extraction (for inclusion calculations) is disabled.");
            genericFieldName = trimmedValue;
        }

        public static void InitialiseLabelSchemeExtraction(String aLabelSchemeName){
            String trimmedValue = StringUtils.trimToNull(aLabelSchemeName);
            if(trimmedValue == null) log.info("User Label Scheme extraction (for inclusion calculations) is disabled.");
            labelSchemeName = trimmedValue;
        }

        private static String labelSchemeName = null;
        private static String genericFieldName = null;

        private ElementsObjectInfo workspace  = null;
        private ElementsUserInfo.UserExtraData additionalUserData = null;

        private DocumentLocation labelLocation = new DocumentLocation(new QName(apiNS, "all-labels"), new QName(apiNS, "keywords"), new QName(apiNS, "keyword"));

        protected  Extractor(DocumentLocation location, int maximumAmountExpected){
            super(location, maximumAmountExpected);
        }

        private ElementsUserInfo.UserExtraData getAdditionalUserData() {
            if (additionalUserData == null) additionalUserData = new ElementsUserInfo.UserExtraData();
            return additionalUserData;
        }

        @Override
        protected void initialiseItemExtraction(StartElement initialElement, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException{
            ElementsObjectCategory objectCategory = ElementsObjectCategory.valueOf(initialElement.getAttributeByName(new QName("category")).getValue());
            int objectId = Integer.parseInt(initialElement.getAttributeByName(new QName("id")).getValue());
            workspace = ElementsItemInfo.createObjectItem(objectCategory, objectId);
            //reset additional Data..
            additionalUserData = null;
            if(workspace.getItemId().getItemSubType() == ElementsObjectCategory.USER ) {
                getAdditionalUserData().setUsername(initialElement.getAttributeByName(new QName("username")).getValue());
                Attribute pidAtt = initialElement.getAttributeByName(new QName("proprietary-id"));
                if(pidAtt != null){
                    getAdditionalUserData().setProprietaryID(pidAtt.getValue());
                }
            }
        }

        @Override
        protected void processEvent(XMLEvent event, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            if (workspace.getItemId().getItemSubType() == ElementsObjectCategory.USER) {
                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    QName name = startElement.getName();
                    if (name.equals(new QName(apiNS, "is-current-staff"))) {
                        XMLEvent nextEvent = readerProxy.peek();
                        if (nextEvent.isCharacters())
                            getAdditionalUserData().setIsCurrentStaff(Boolean.parseBoolean(nextEvent.asCharacters().getData()));
                    } else if (name.equals(new QName(apiNS, "is-academic"))) {
                        XMLEvent nextEvent = readerProxy.peek();
                        if (nextEvent.isCharacters())
                            getAdditionalUserData().setIsAcademic(Boolean.parseBoolean(nextEvent.asCharacters().getData()));
                    } else if (name.equals(new QName(apiNS, "photo"))) {
                        getAdditionalUserData().setPhotoUrl(startElement.getAttributeByName(new QName("href")).getValue());
                    }
                    else if (genericFieldName != null && name.equals(new QName(apiNS, "organisation-defined-data"))) {
                        String fieldName = startElement.getAttributeByName(new QName("field-name")).getValue();
                        if(fieldName.equals(genericFieldName)){
                            XMLEvent nextEvent = readerProxy.peek();
                            if (nextEvent.isCharacters())
                                getAdditionalUserData().setGenericFieldValue(nextEvent.asCharacters().getData());
                        }
                    }
                    else if (labelSchemeName != null && name.equals(new QName(apiNS, "keyword"))) {
                        //if we are at the label location try and extract one
                        if (isAtLocation(labelLocation)) {
                            String originValue = startElement.getAttributeByName(new QName("origin")).getValue();
                            String schemeValue = startElement.getAttributeByName(new QName("scheme")).getValue();
                            //if the label in question is both from object data and is the requested scheme we store the value
                            if (originValue.equals("object-data") && schemeValue.equals(labelSchemeName)) {
                                XMLEvent nextEvent = readerProxy.peek();
                                if (nextEvent.isCharacters())
                                    getAdditionalUserData().addLabelSchemeValue(nextEvent.asCharacters().getData());
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected ElementsObjectInfo finaliseItemExtraction(EndElement finalElement, XMLEventProcessor.ReaderProxy readerProxy){
            if (workspace instanceof ElementsUserInfo) {
                ElementsUserInfo userInfo = (ElementsUserInfo) workspace;
                userInfo.addExtraData(additionalUserData);
            }
            return workspace;
        }
    }

    //package private as should only ever be constructed by create calls into superclass
    protected ElementsObjectInfo(ElementsObjectCategory category, int id) {
        super(ElementsItemId.createObjectId(category, id));
    }

    public ElementsItemId.ObjectId getObjectId(){return (ElementsItemId.ObjectId) getItemId();}

}
