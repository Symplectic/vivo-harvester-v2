/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
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

public class ElementsRelationshipInfo extends ElementsItemInfo{

    public abstract static class Extractor extends XMLEventProcessor.ItemExtractingFilter<ElementsRelationshipInfo>{

        private static DocumentLocation fileEntryLocation = new DocumentLocation(new QName(atomNS, "entry"), new QName(apiNS, "relationship"));
        private static DocumentLocation feedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "relationship"));
        private static DocumentLocation feedDeletedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "deleted-relationship"));

        public static class FromFeed extends Extractor {
            public FromFeed(){ this(0); }
            public FromFeed(int maximumAmountExpected){ super(feedEntryLocation, maximumAmountExpected); }
        }

        public static class DeletedFromFeed extends Extractor {
            public DeletedFromFeed(){ this(0); }
            public DeletedFromFeed(int maximumAmountExpected){ super(feedDeletedEntryLocation, maximumAmountExpected); }
        }

        public static class FromFile extends Extractor {
            public FromFile(){ this(0); }
            public FromFile(int maximumAmountExpected){ super(fileEntryLocation, maximumAmountExpected); }
        }

        private ElementsRelationshipInfo workspace = null;

        protected Extractor(DocumentLocation location, int maximumAmountExpected){
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
                //only pull type id for "relationship" not "deleted-relationship" where it is not present.
                if(name.equals(new QName(apiNS, "relationship"))){
                    workspace.setType(startElement.getAttributeByName(new QName("type")).getValue());
                }
                else if (name.equals(new QName(apiNS, "object"))) {
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

    //default visible to "true" so that relationships that are not marked as visible at all (e.g those between a grant and a publication) are definitely included.
    private boolean isVisible = true;
    private String type = null;
    private final List<ElementsItemId.ObjectId> objectIds = new ArrayList<ElementsItemId.ObjectId>();

    //package private as should only ever be constructed by create calls into superclass
    ElementsRelationshipInfo(int id) { super(ElementsItemId.createRelationshipId(id)); }

    public String getType() {
        if(type == null) throw new IllegalAccessError("typeId has not been initialised");
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public boolean getIsVisible() {
        return isVisible;
    }
    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public List<ElementsItemId.ObjectId> getUserIds() {
        List<ElementsItemId.ObjectId> userIds = new ArrayList<ElementsItemId.ObjectId>();
        for(ElementsItemId.ObjectId id : objectIds){
            if(id.getItemSubType() == ElementsObjectCategory.USER) userIds.add(id);
        }
        return userIds;
    }

    public void addObjectId(ElementsItemId.ObjectId id) { objectIds.add(id); }

    public List<ElementsItemId.ObjectId> getObjectIds() { return Collections.unmodifiableList(objectIds); }
}
