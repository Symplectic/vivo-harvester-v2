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
import java.util.List;

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
        protected void initialiseItemExtraction(XMLEventProcessor.WrappedXmlEvent initialEvent) throws XMLStreamException{
            ElementsObjectCategory objectCategory = ElementsObjectCategory.valueOf(initialEvent.getAttribute("category"));
            int objectId = Integer.parseInt(initialEvent.getAttribute("id"));
            workspace = ElementsItemInfo.createObjectItem(objectCategory, objectId);
            //reset additional Data..
            additionalUserData = null;
            if(workspace.getItemId().getItemSubType() == ElementsObjectCategory.USER ) {
                getAdditionalUserData().setUsername(initialEvent.getAttribute("username"));
                if(initialEvent.hasAttribute("proprietary-id"))
                    getAdditionalUserData().setProprietaryID(initialEvent.getAttribute("proprietary-id"));
            }
        }

        @Override
        protected void processEvent(XMLEventProcessor.WrappedXmlEvent event, List<QName> relativeLocation) throws XMLStreamException {
            if (workspace.getItemId().getItemSubType() == ElementsObjectCategory.USER) {
                if (event.isRelevantForExtraction()) {
                    QName name = event.getName();
                    if (name.equals(new QName(apiNS, "is-current-staff"))) {
                        //I really want there to be a value here..
                        getAdditionalUserData().setIsCurrentStaff(Boolean.parseBoolean(event.getRequiredValue()));
                    } else if (name.equals(new QName(apiNS, "is-academic"))) {
                        getAdditionalUserData().setIsAcademic(Boolean.parseBoolean(event.getRequiredValue()));
                    } else if (name.equals(new QName(apiNS, "photo"))) {
                        getAdditionalUserData().setPhotoUrl(event.getAttribute("href"));
                    }
                    else if (genericFieldName != null && name.equals(new QName(apiNS, "organisation-defined-data"))) {
                        if(event.hasAttribute("field-name") && genericFieldName.equals(event.getAttribute("field-name"))){
                            getAdditionalUserData().setGenericFieldValue(event.getValueOrNull());
                        }
                    }
                    else if (labelSchemeName != null && labelLocation.matches(relativeLocation)) {
                        //if we are at the label location try and extract one
                        if(event.hasAttribute("origin") && event.hasAttribute("scheme")){
                            if("object-data".equals(event.getAttribute("origin")) && labelSchemeName.equals(event.getAttribute("scheme"))) {
                                getAdditionalUserData().addLabelSchemeValue(event.getValueOrNull());
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected ElementsObjectInfo finaliseItemExtraction(XMLEventProcessor.WrappedXmlEvent finalEvent){
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
