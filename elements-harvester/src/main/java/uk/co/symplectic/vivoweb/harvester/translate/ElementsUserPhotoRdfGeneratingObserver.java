/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.utils.ImageUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsRdfStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItemInfo;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;


/**
 * Specialised Concrete subclass of ElementsTranslateObserver.
 * Overrides observeStoredObject/observeObjectDeletion to observe the arrival of RAW_USER_PHOTO data (photo image data)
 * in the rawDataStore. Based on that data it generates two jpegs suitable for use with Vivo and writes those to disk.
 * It then generates a TRANSLATED_USER_PHOTO_DESCRIPTION document of RDF that contains triples describing
 * where the generated photos are expected to be made available from within the JAVA servlet container hosting Vivo.
 *
 * The generation of the TRANSLATED_USER_PHOTO_DESCRIPTION RDF document is done by passing a custom input document to
 * the TranslationService. The custom input document being created internally here by the method getUserXMLDescription
 *
 * Note: the generated photos are NOT deleted when a deletion of the RAW_USER_PHOTO is observed in the rawDataStore.
 * This is to avoid (for example) having user photos go missing during "full" harvests.
 *
 */
public class ElementsUserPhotoRdfGeneratingObserver extends ElementsTranslateObserver{

    private static final Logger log = LoggerFactory.getLogger(ElementsUserPhotoRdfGeneratingObserver.class);

    @SuppressWarnings("FieldCanBeLocal")
    private static int VIVO_THUMBNAIL_WIDTH = 200;
    @SuppressWarnings("unused")
    private static int VIVO_THUMBNAIL_HEIGHT = 200;

    private final File processedImageDir;
    private final String vivoImageBasePath;

    public ElementsUserPhotoRdfGeneratingObserver(ElementsRdfStore rdfStore, String xslFilename, File processedImageDir, String vivoImageBasePath){
        super(rdfStore, xslFilename, StorableResourceType.RAW_USER_PHOTO, StorableResourceType.TRANSLATED_USER_PHOTO_DESCRIPTION);

        if(processedImageDir == null) throw new NullArgumentException("processedImageDir");
        if(StringUtils.trimToNull(vivoImageBasePath) == null) throw new IllegalArgumentException("vivoImageBasePath must not be null or empty");

        this.processedImageDir = processedImageDir;
        if(vivoImageBasePath.endsWith("/")){ this.vivoImageBasePath = vivoImageBasePath; }
        else{ this.vivoImageBasePath  = vivoImageBasePath + "/"; }
    }


    private String getVivoFullImageFileName(ElementsItemId.ObjectId userID){return userID.getId() + ".jpg";}

    private String getVivoThumbnailImageFileName(ElementsItemId.ObjectId userID){return userID.getId() + ".thumbnail.jpg";}

    private String getVivoFullImagePathFragment(ElementsItemId.ObjectId userID){
        return "fullImages/" + getVivoFullImageFileName(userID);
    }
    private String getVivoThumbnailImagePathFragment(ElementsItemId.ObjectId userID){
        return "thumbnails/" + getVivoThumbnailImageFileName(userID);
    }

    private File getFullImageFile(ElementsItemId.ObjectId userID){
        return new File(processedImageDir, getVivoFullImagePathFragment(userID));
    }

    private File getThumbnailImageFile(ElementsItemId.ObjectId userID){
        return new File(processedImageDir, getVivoThumbnailImagePathFragment(userID));
    }


    private Document getUserXMLDescription(ElementsUserInfo userInfo){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElementNS(ElementsAPI.atomNS, "entry");
            doc.appendChild(rootElement);
            Element userElement = doc.createElement("user-with-photo");
            //create id  and username attributes on our user Element
            userElement.setAttribute("id", Integer.toString(userInfo.getObjectId().getId()));
            userElement.setAttribute("username", userInfo.getUsername());
            if(userInfo.getProprietaryID() != null) userElement.setAttribute("proprietary-id", userInfo.getProprietaryID());
            //add our  user Element to the root of the doc;

            Element element = doc.createElement("full-image-path-fragment");
            element.setTextContent(vivoImageBasePath + getVivoFullImagePathFragment(userInfo.getObjectId()));
            userElement.appendChild(element);

            element = doc.createElement("full-image-fileName");
            element.setTextContent(getVivoFullImageFileName(userInfo.getObjectId()));
            userElement.appendChild(element);

            element = doc.createElement("thumbnail-image-path-fragment");
            element.setTextContent(vivoImageBasePath + getVivoThumbnailImagePathFragment(userInfo.getObjectId()));
            userElement.appendChild(element);

            element = doc.createElement("thumbnail-image-fileName");
            element.setTextContent(getVivoThumbnailImageFileName(userInfo.getObjectId()));
            userElement.appendChild(element);

            rootElement.appendChild(userElement);
            return doc;
        }
        catch (ParserConfigurationException pce) {
            throw new IllegalStateException(pce);
        }
    }

    private boolean processFiles(ElementsUserInfo userInfo, ElementsStoredItemInfo item){
        //TODO: migrate this resizing code to elsewhere...Monitor process? difficulties exist here around "deleting" extraneous resources linked to an item that might be deleted.
        BufferedImage image = null;
        try {
            //readFile will close the stream for us..
            image = ImageUtils.readFile(item.getInputStream());
        }
        catch (IOException ioe){
            //let failure return false out of bottom of method - caller will log
        }
        if (image != null) {
            // Write out full size image
            File fullImageFile = getFullImageFile(userInfo.getObjectId());
            File fullImageDir = fullImageFile.getParentFile();
            if (fullImageDir != null && !fullImageDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                fullImageDir.mkdirs();
            }

            //image = ImageUtils.cropToSquare(image);
            boolean returnValue = ImageUtils.writeFile(image, fullImageFile, "jpeg");

            // Write out thumbnail
            File thumbnailImageFile = getThumbnailImageFile(userInfo.getObjectId());
            File thumbnailDir = thumbnailImageFile.getParentFile();
            if (thumbnailDir != null && !thumbnailDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                thumbnailDir.mkdirs();
            }

            int targetHeight = ImageUtils.getTargetHeight(image.getWidth(), image.getHeight(), VIVO_THUMBNAIL_WIDTH);
            BufferedImage scaledImage = ImageUtils.getScaledInstance(image, VIVO_THUMBNAIL_WIDTH, targetHeight, true);
            returnValue = returnValue && ImageUtils.writeFile(scaledImage, thumbnailImageFile, "jpeg");

            return returnValue;
        }
        return false;
    }


    @Override
    protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItemInfo item) {
        if (info.getItemId().getItemSubType() == ElementsObjectCategory.USER) {

            ElementsUserInfo userInfo = (ElementsUserInfo) info;
            if(processFiles(userInfo, item)) {
                translate(item, new DOMSource(getUserXMLDescription(userInfo)), null);
            }
            else{
                log.warn(MessageFormat.format("Could not process image file for {0} ({1})", userInfo.getItemId(), userInfo.getUsername()));
            }
        }
    }

    @Override
    protected void observeObjectDeletion(ElementsItemId.ObjectId objectId, StorableResourceType type){
        if (objectId.getItemSubType() == ElementsObjectCategory.USER) {
            //deleting these files here is poor as the correlated change in vivo does not happen until much later.
            File[] extraFilesToDelete = {
                //getFullImageFile(objectId),
                //getThumbnailImageFile(objectId)
            };
            safelyDeleteItem(objectId, extraFilesToDelete, MessageFormat.format("Unable to delete translated user-photo-rdf for user {0}", objectId.toString()));
        }
    }
}
