/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.model;

import uk.co.symplectic.xml.XMLEventProcessor;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElementsRelationshipInfo extends ElementsItemInfo{

    public static class Extractor extends XMLEventProcessor.ItemExtractingFilter<ElementsRelationshipInfo>{

        public static DocumentLocation fileEntryLocation = new DocumentLocation(new QName(atomNS, "entry"), new QName(apiNS, "relationship"));
        public static DocumentLocation feedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "relationship"));
        public static DocumentLocation feedDeletedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "deleted-relationship"));

        private ElementsRelationshipInfo workspace = null;

        public Extractor(DocumentLocation location, int maximumAmountExpected){
            super(location, maximumAmountExpected);
        }

        @Override
        protected void initialiseItemExtraction(StartElement initialElement, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            String id = initialElement.getAttributeByName(new QName("id")).getValue();
            workspace = ElementsItemInfo.createRelationshipItem(Integer.parseInt(id));
        }

        @Override
        protected void processEvent(XMLEvent event, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                QName name = startElement.getName();
                if (name.equals(new QName(apiNS, "object"))) {
                    ElementsObjectCategory objectCategory = ElementsObjectCategory.valueOf(startElement.getAttributeByName(new QName("category")).getValue());
                    int objectID = Integer.parseInt(startElement.getAttributeByName(new QName("id")).getValue());
                    workspace.addObjectId(ElementsItemId.createObjectId(objectCategory, objectID));
                }
                else if(name.equals(new QName(apiNS, "is-visible"))){
                    XMLEvent nextEvent = readerProxy.peek();
                    if (nextEvent.isCharacters())
                        workspace.setIsVisible(Boolean.parseBoolean(nextEvent.asCharacters().getData()));
                }
            }
        }

        @Override
        protected ElementsRelationshipInfo finaliseItemExtraction(EndElement finalElement, XMLEventProcessor.ReaderProxy readerProxy){
            return workspace;
        }
    }

    private boolean isVisible = true;
    private final List<ElementsItemId.ObjectId> objectIds = new ArrayList<ElementsItemId.ObjectId>();

    //package private as should only ever be constructed by create calls into superclass
    ElementsRelationshipInfo(int id) { super(ElementsItemId.createRelationshipId(id)); }

    public boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public List<ElementsItemId.ObjectId> getUserIds() {
        List<ElementsItemId.ObjectId> userIds = new ArrayList<ElementsItemId.ObjectId>();
        for(ElementsItemId.ObjectId id : objectIds){
            if(id.getCategory() == ElementsObjectCategory.USER) userIds.add(id);
        }
        return userIds;
    }

    public void addObjectId(ElementsItemId.ObjectId id) { objectIds.add(id); }

    public List<ElementsItemId.ObjectId> getObjectIds() { return Collections.unmodifiableList(objectIds); }
}
