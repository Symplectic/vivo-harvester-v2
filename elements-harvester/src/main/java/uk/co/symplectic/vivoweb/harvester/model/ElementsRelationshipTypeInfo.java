/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
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
        protected void initialiseItemExtraction(XMLEventProcessor.WrappedXmlEvent initialEvent) throws XMLStreamException {
            int id = Integer.parseInt(initialEvent.getAttribute("id"));
            workspace = ElementsItemInfo.createRelationshipTypeItem(id);
        }

        @Override
        protected void processEvent(XMLEventProcessor.WrappedXmlEvent event, List<QName> relativeLocation) throws XMLStreamException {
            if (event.isRelevantForExtraction()) {
                QName name = event.getName();
                if (name.equals(new QName(apiNS, "from-object"))) {
                    try {
                        ElementsObjectCategory objectCategory = ElementsObjectCategory.valueOf(event.getAttribute("category"));
                        workspace.setFromCategory(objectCategory);
                    }
                    catch(IndexOutOfBoundsException e){
                        //do nothing - this is just a relationship to an object type we don't know how to handle yet..
                        //will result in an "incomplete" relationship type
                    }
                }
                else if(name.equals(new QName(apiNS, "to-object"))){
                    try {
                        ElementsObjectCategory objectCategory = ElementsObjectCategory.valueOf(event.getAttribute("category"));
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
        protected ElementsRelationshipTypeInfo finaliseItemExtraction(XMLEventProcessor.WrappedXmlEvent finalElement){
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
