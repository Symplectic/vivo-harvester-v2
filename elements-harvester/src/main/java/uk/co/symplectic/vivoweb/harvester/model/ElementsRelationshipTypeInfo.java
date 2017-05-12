/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.model;

import uk.co.symplectic.utils.xml.XMLEventProcessor;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static uk.co.symplectic.elements.api.ElementsAPI.apiNS;
import static uk.co.symplectic.elements.api.ElementsAPI.atomNS;

public class ElementsRelationshipTypeInfo extends ElementsItemInfo{

    public static class Extractor extends XMLEventProcessor.ItemExtractingFilter<ElementsItemInfo>{

        private static DocumentLocation fileEntryLocation = new DocumentLocation(new QName(atomNS, "entry"), new QName(apiNS, "relationship-type"));
        private static DocumentLocation feedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "relationship-type"));

        public static Extractor getExtractor(ElementsItemInfo.ExtractionSource source, int maximumExpected){
            switch(source) {
                case FEED : return new Extractor(feedEntryLocation, maximumExpected);
                case FILE : return new Extractor(fileEntryLocation, maximumExpected);
                default : throw new IllegalStateException("invalid extractor source type requested");
            }
        }

        private ElementsRelationshipTypeInfo workspace = null;

        private Extractor(DocumentLocation location, int maximumAmountExpected){
            super(location, maximumAmountExpected);
        }

        @Override
        protected void initialiseItemExtraction(StartElement initialElement, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            String id = initialElement.getAttributeByName(new QName("id")).getValue();
            workspace = ElementsItemInfo.createRelationshipTypeItem(Integer.parseInt(id));
        }

        @Override
        protected void processEvent(XMLEvent event, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                QName name = startElement.getName();
                if (name.equals(new QName(apiNS, "from-object"))) {
                    try {
                        ElementsObjectCategory objectCategory = ElementsObjectCategory.valueOf(startElement.getAttributeByName(new QName("category")).getValue());
                        workspace.setFromCategory(objectCategory);
                    }
                    catch(IndexOutOfBoundsException e){
                        //do nothing - this is just a relationship to an object type we don't know how to handle yet..
                        //will result in an "incomplete" relationship type
                    }
                }
                else if(name.equals(new QName(apiNS, "to-object"))){
                    try {
                        ElementsObjectCategory objectCategory = ElementsObjectCategory.valueOf(startElement.getAttributeByName(new QName("category")).getValue());
                        workspace.setToCategory(objectCategory);
                    }
                    catch(IndexOutOfBoundsException e){
                        //do nothing - this is just a relationship to an object type we don't know how to handle yet..
                        //will result in an "incomplete" relationship type
                    }
                }
            }
        }

        @Override
        protected ElementsRelationshipTypeInfo finaliseItemExtraction(EndElement finalElement, XMLEventProcessor.ReaderProxy readerProxy){
            return workspace;
        }
    }

    private ElementsObjectCategory fromCategory;
    private ElementsObjectCategory toCategory;

    //package private as should only ever be constructed by create calls into superclass
    ElementsRelationshipTypeInfo(int id) { super(ElementsItemId.createRelationshipTypeId(id)); }

    public ElementsObjectCategory getFromCategory() {
        return fromCategory;
    }

    public void setFromCategory(ElementsObjectCategory fromCategory) {
        this.fromCategory = fromCategory;
    }

    public ElementsObjectCategory getToCategory() {
        return toCategory;
    }

    public void setToCategory(ElementsObjectCategory toCategory) {
        this.toCategory = toCategory;
    }

    public boolean isComplete(){
        if(getFromCategory() == null || getToCategory() == null){
            return false;
        }
        return true;
    }
}
