/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.utils.xml.StAXUtils;
import uk.co.symplectic.utils.xml.XMLEventProcessor;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

public class ElementsStoredItem {
    private final ElementsItemInfo itemInfo;
    protected final BasicElementsStoredItem innerItem;

    protected ElementsStoredItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, StoredData data) {
        if (itemInfo == null) throw new NullArgumentException("itemInfo");
        if (resourceType == null) throw new NullArgumentException("resourceType");
        if (data == null) throw new NullArgumentException("data");

        innerItem = new BasicElementsStoredItem(itemInfo.getItemId(), resourceType, data);
        this.itemInfo = itemInfo;
    }

    public ElementsItemInfo getItemInfo() {
        return itemInfo;
    }

    public StorableResourceType getResourceType() {
        return innerItem.getResourceType();
    }

    public InputStream getInputStream() throws IOException{
        return innerItem.getStoredData().getInputStream();
    }

    public String getAddress(){
        return innerItem.getStoredData().getAddress();
    }

    public static class InRam extends ElementsStoredItem{
        public InRam(byte[] data, ElementsItemInfo itemInfo, StorableResourceType resourceType) {
            super(itemInfo, resourceType, new StoredData.InRam(data));
        }

        public byte[] getBytes() {
            return ((StoredData.InRam) innerItem.getStoredData()).getBytes();
        }
    }

    public static class InFile extends ElementsStoredItem {
        public InFile(File file, ElementsItemInfo itemInfo, StorableResourceType resourceType, boolean isZipped) {
            super(itemInfo, resourceType, new StoredData.InFile(file, isZipped));
        }

        public File getFile() {
            return ((StoredData.InFile) innerItem.getStoredData()).getFile();
        }

        private synchronized static <T> T loadFromFile(File file, XMLEventProcessor.ItemExtractingFilter<T> extractor, boolean zipped) {
            if (file == null) throw new NullArgumentException("file");
            InputStream inputStream = null;
            try {
                //TODO: check UTF-8 behaviour here.
                StoredData data = new StoredData.InFile(file, zipped);
                inputStream = data.getInputStream();
                XMLInputFactory xmlInputFactory = StAXUtils.getXMLInputFactory();
                XMLEventProcessor processor = new XMLEventProcessor(extractor);
                processor.process(xmlInputFactory.createXMLEventReader(inputStream));

                return extractor.getExtractedItem();
            } catch (FileNotFoundException fileNotFoundException) {
                throw new IllegalStateException("Catastrophic failure reading files - abandoning", fileNotFoundException);
            } catch (IOException ioException) {
                throw new IllegalStateException("Catastrophic failure reading files - abandoning", ioException);
            } catch (XMLStreamException xmlStreamException) {
                throw new IllegalStateException("Catastrophic failure reading files - abandoning", xmlStreamException);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        throw new IllegalStateException("Catastrophic failure closing stream after reading files - abandoning", e);
                    }
                }
            }
        }

        public synchronized static ElementsStoredItem loadRawObject(File file) {
            return loadRawObject(file, false);
        }

        public synchronized static ElementsStoredItem loadRawObject(File file, boolean zipped) {
            return loadRawObject(file, null, zipped);
        }

        public synchronized static ElementsStoredItem loadRawObject(File file, ElementsItemId.ObjectId idToCompareTo, boolean zipped) {
            ElementsObjectInfo objectInfo = loadFromFile(file, new ElementsObjectInfo.Extractor.FromFile(1), zipped);

            //TODO: decide if this is still sensible
            //take the extracted object info, put it in the cache and ensure that we use the cached one if it is present
            //ElementsObjectInfoCache.put(objectInfo);
            //ElementsObjectInfo cachedInfo = ElementsObjectInfoCache.get(objectInfo.getObjectId());
            //objectInfo = cachedInfo == null ? objectInfo : cachedInfo;

            if (idToCompareTo != null && !idToCompareTo.equals(objectInfo.getObjectId())) {
                String message = MessageFormat.format("Elements object loaded from file \"{0}\" ({1}) does not match supplied check value ({2})",
                        file.getName(), objectInfo.getItemId().toString(), idToCompareTo.toString());
                throw new IllegalStateException(message);
            }
            return new InFile(file, objectInfo, StorableResourceType.RAW_OBJECT, zipped);
        }

        public synchronized static ElementsStoredItem loadRawRelationship(File file) {
            return loadRawRelationship(file, false);
        }

        public synchronized static ElementsStoredItem loadRawRelationship(File file, boolean zipped) {
            return loadRawRelationship(file, null, zipped);
        }

        public synchronized static ElementsStoredItem loadRawRelationship(File file, ElementsItemId.RelationshipId idToCompareTo, boolean zipped) {
            ElementsRelationshipInfo relationshipInfo = loadFromFile(file, new ElementsRelationshipInfo.Extractor.FromFile(1), zipped);

            if (idToCompareTo != null && !idToCompareTo.equals(relationshipInfo.getItemId())) {
                String message = MessageFormat.format("Elements relationship loaded from file \"{0}\" ({1}) does not match supplied check values ({2})",
                    file.getName(), relationshipInfo.getItemId().toString(), idToCompareTo.toString());
                throw new IllegalStateException(message);
            }
            return new InFile(file, relationshipInfo, StorableResourceType.RAW_RELATIONSHIP, zipped);
        }
    }
}







