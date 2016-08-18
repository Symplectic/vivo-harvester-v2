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
import sun.plugin.dom.exception.InvalidStateException;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.xml.StAXUtils;
import uk.co.symplectic.xml.XMLEventProcessor;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.text.MessageFormat;
import java.util.zip.GZIPInputStream;

/**
 * Created by ajpc2_000 on 09/08/2016.
 */
public class ElementsStoredItem {

    private final File file;
    private final ElementsItemInfo itemInfo;
    private final StorableResourceType resourceType;


    public ElementsStoredItem(File file, ElementsItemInfo itemInfo, StorableResourceType resourceType) {
        if (file == null) throw new NullArgumentException("file");
        if (itemInfo == null) throw new NullArgumentException("objectInfo");
        if (resourceType == null) throw new NullArgumentException("resourceType");

        if (!resourceType.isAppropriateForItem(itemInfo))
            throw new IllegalArgumentException("itemInfo does not support resourceType");

        this.file = file;
        this.itemInfo = itemInfo;
        this.resourceType = resourceType;
    }

    public ElementsItemInfo getItemInfo() {
        return itemInfo;
    }

    public File getFile() {
        return file;
    }

    public StorableResourceType getResourceType() {
        return resourceType;
    }

    private synchronized static <T> T loadFromFile(File file, XMLEventProcessor.ItemExtractingFilter<T> extractor,  boolean zipped){
        if(file == null) throw new NullArgumentException("file");
        InputStream inputStream = null;
        try {
            //TODO: check UTF-8 behaviour here.
            inputStream = new BufferedInputStream(new FileInputStream(file));
            if(zipped) inputStream = new GZIPInputStream(inputStream);
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
                try { inputStream.close(); }
                catch (IOException e) {
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

    public synchronized static ElementsStoredItem loadRawObject(File file, ElementsObjectId idToCompareTo, boolean zipped) {
        ElementsObjectInfo objectInfo = loadFromFile(file, new ElementsObjectInfo.Extractor(ElementsObjectInfo.Extractor.fileEntryLocation, 1), zipped);

        //TODO: decide if this is still sensible
        //take the extracted object info, put it in the cache and ensure that we use the cached one if it is present
        ElementsObjectInfoCache.put(objectInfo);
        ElementsObjectInfo cachedInfo = ElementsObjectInfoCache.get(objectInfo.getCategory(), objectInfo.getId());
        objectInfo = cachedInfo == null ? objectInfo : cachedInfo;

        if (idToCompareTo != null && !idToCompareTo.equals(objectInfo.getObjectId())) {
            String message = MessageFormat.format("Elements object loaded from file ({0}:{1}) does not match supplied check values ({2}:{3})",
                    objectInfo.getCategory().getSingular(), objectInfo.getId(), idToCompareTo.getCategory().getSingular(), idToCompareTo.getId());
            throw new InvalidStateException(message);
        }
        return new ElementsStoredItem(file, objectInfo, StorableResourceType.RAW_OBJECT);
    }

    public synchronized static ElementsStoredItem loadRawRelationship(File file) {
        return loadRawRelationship(file, false);
    }
    public synchronized static ElementsStoredItem loadRawRelationship(File file, boolean zipped) {
        return loadRawRelationship(file, null, zipped);
    }
    public synchronized static ElementsStoredItem loadRawRelationship(File file, String idToCompareTo, boolean zipped) {
        ElementsRelationshipInfo relationshipInfo = loadFromFile(file, new ElementsRelationshipInfo.Extractor(ElementsRelationshipInfo.Extractor.fileEntryLocation, 1), zipped);

        if (idToCompareTo != null && !idToCompareTo.equals(relationshipInfo.getId())) {
            String message = MessageFormat.format("Elements relationship loaded from file ({0}) does not match supplied check values ({1})",
                    relationshipInfo.getId(), idToCompareTo);
            throw new InvalidStateException(message);
        }
        return new ElementsStoredItem(file, relationshipInfo, StorableResourceType.RAW_RELATIONSHIP);
    }
}


