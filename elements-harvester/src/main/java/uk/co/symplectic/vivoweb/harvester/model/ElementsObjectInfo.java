/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.model;

import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.xml.XMLEventProcessor;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import static uk.co.symplectic.elements.api.ElementsAPI.apiNS;
import static uk.co.symplectic.elements.api.ElementsAPI.atomNS;

public class ElementsObjectInfo extends ElementsItemInfo{

    public static class Extractor extends XMLEventProcessor.ItemExtractingFilter<ElementsObjectInfo>{

        public static DocumentLocation fileEntryLocation = new DocumentLocation(new QName(atomNS, "entry"), new QName(apiNS, "object"));
        public static DocumentLocation feedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "object"));
        public static DocumentLocation feedDeletedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "deleted-object"));

        private ElementsObjectInfo workspace  = null;
        private ElementsUserInfo.UserExtraData additionalUserData = null;

        public Extractor(DocumentLocation location, int maximumAmountExpected){
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
