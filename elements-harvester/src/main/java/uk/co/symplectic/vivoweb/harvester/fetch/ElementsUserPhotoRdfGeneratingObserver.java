/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.fetch;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.utils.ImageUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.store.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;

public class ElementsUserPhotoRdfGeneratingObserver extends ElementsStoreOutputItemObserver {
    private File vivoImageDir;
    private String imageUrlBase = "/harvestedImages/";

    private final String baseUrl;

    private static int VIVO_THUMBNAIL_WIDTH = 200;
    private static int VIVO_THUMBNAIL_HEIGHT = 200;

    public ElementsUserPhotoRdfGeneratingObserver(ElementsRdfStore rdfStore, File vivoImageDir, String vivoBaseURI, String imageUrlBase) {
        //todo move tolerate individual io errors somewhere sensible..
        super(rdfStore, StorableResourceType.RAW_USER_PHOTO, StorableResourceType.TRANSLATED_USER_PHOTO_DESCRIPTION, false);
        if(vivoImageDir == null) throw new NullArgumentException("vivoImageDir");
        if(StringUtils.trimToNull(vivoBaseURI) == null) throw new IllegalArgumentException("Parameter vivoBaseURI must not be null or empty");

        this.vivoImageDir = vivoImageDir;
        this.baseUrl = vivoBaseURI;

        // TODO: The only caller of this method hard-codes this parameter to a null value
        if (imageUrlBase != null) {
            this.imageUrlBase = imageUrlBase;
        }
    }

    @Override
    protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item) {
        //TODO: migrate this resizing code to elsewhere...Monitor process? difficulties exist here around "deleting" extraneous resources linked to an item that might be deleted.
        if (info.getItemId().getItemSubType() == ElementsObjectCategory.USER) {
            ElementsUserInfo userInfo = (ElementsUserInfo) info;

            String uriUserName = userInfo.getUsername().toLowerCase().replaceAll("\\a+", "-").replaceAll("[^a-z0-9\\-]", "");
            BufferedImage image = null;
            try {
                //readFile will close the stream for us..
                image = ImageUtils.readFile(item.getInputStream());
            }
            catch (IOException ioe){}
            if (image != null) {
                // Write out full size image
                File fullImageDir = new File(new File(vivoImageDir, "harvestedImages"), "fullImages");
                if (!fullImageDir.exists()) {
                    fullImageDir.mkdirs();
                }

                ImageUtils.writeFile(image, new File(fullImageDir, uriUserName + ".jpg"), "jpeg");

                // Write out thumbnail
                File thumbnailDir = new File(new File(vivoImageDir, "harvestedImages"), "thumbnails");
                if (!thumbnailDir.exists()) {
                    thumbnailDir.mkdirs();
                }

                int targetHeight = ImageUtils.getTargetHeight(image.getWidth(), image.getHeight(), VIVO_THUMBNAIL_WIDTH);
                ImageUtils.writeFile(ImageUtils.getScaledInstance(image, VIVO_THUMBNAIL_WIDTH, targetHeight, true), new File(thumbnailDir, uriUserName + ".thumbnail.jpg"), "jpeg");

                // Write out XML
                Writer photoXml = new StringWriter();
                try {
                    photoXml.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    photoXml.write("<rdf:RDF xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:score=\"http://vivoweb.org/ontology/score#\" xmlns:vitro=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#\" xmlns:bibo=\"http://purl.org/ontology/bibo/\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:ufVivo=\"http://vivo.ufl.edu/ontology/vivo-ufl/\" xmlns:owlPlus=\"http://www.w3.org/2006/12/owl2-xml#\" xmlns:svo=\"http://www.symplectic.co.uk/vivo/\" xmlns:skos=\"http://www.w3.org/2008/05/skos#\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema#\" xmlns:api=\"http://www.symplectic.co.uk/publications/api\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:vitro-public=\"http://vitro.mannlib.cornell.edu/ns/vitro/public#\" xmlns:vocab=\"http://purl.org/vocab/vann/\" xmlns:core=\"http://vivoweb.org/ontology/core#\" xmlns:swvocab=\"http://www.w3.org/2003/06/sw-vocab-status/ns#\">");

                    photoXml.write("<rdf:Description rdf:about=\"" + baseUrl + uriUserName + "\">");
                    photoXml.write("    <vitro-public:mainImage rdf:resource=\"" + baseUrl + uriUserName + "-image\"/>");
                    photoXml.write("</rdf:Description>");

                    photoXml.write("<rdf:Description rdf:about=\"" + baseUrl + uriUserName + "-image\">");
                    photoXml.write("    <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Thing\"/>");
                    photoXml.write("    <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/public#File\"/>");
                    photoXml.write("    <vitro-public:downloadLocation rdf:resource=\"" + baseUrl + uriUserName + "-imageDownload\"/>");
                    photoXml.write("    <vitro-public:thumbnailImage rdf:resource=\"" + baseUrl + uriUserName + "-imageThumbnail\"/>");
                    photoXml.write("    <vitro-public:filename>" + uriUserName + ".jpg</vitro-public:filename>");
                    photoXml.write("    <vitro-public:mimeType>image/jpeg</vitro-public:mimeType>");
                    photoXml.write("</rdf:Description>");

                    photoXml.write("<rdf:Description rdf:about=\"" + baseUrl + uriUserName + "-imageDownload\">");
                    photoXml.write("    <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/public#FileByteStream\"/>");
                    photoXml.write("    <vitro-public:directDownloadUrl>" + imageUrlBase + "fullImages/" + uriUserName + ".jpg</vitro-public:directDownloadUrl>");
                    photoXml.write("</rdf:Description>");

                    photoXml.write("<rdf:Description rdf:about=\"" + baseUrl + uriUserName + "-imageThumbnail\">");
                    photoXml.write("    <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Thing\"/>");
                    photoXml.write("    <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/public#File\"/>");
                    photoXml.write("    <vitro-public:downloadLocation rdf:resource=\"" + baseUrl + uriUserName + "-imageThumbnailDownload\"/>");
                    photoXml.write("    <vitro-public:filename>" + uriUserName + ".thumbnail.jpg</vitro-public:filename>");
                    photoXml.write("    <vitro-public:mimeType>image/jpeg</vitro-public:mimeType>");
                    photoXml.write("</rdf:Description>");

                    photoXml.write("<rdf:Description rdf:about=\"" + baseUrl + uriUserName + "-imageThumbnailDownload\">");
                    photoXml.write("    <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/public#FileByteStream\"/>");
                    photoXml.write("    <vitro-public:directDownloadUrl>" + imageUrlBase + "thumbnails/" + uriUserName + ".thumbnail.jpg</vitro-public:directDownloadUrl>");
                    photoXml.write("</rdf:Description>");

                    photoXml.write("</rdf:RDF>");

                    getStore().storeItem(userInfo, getOutputType(), photoXml.toString().getBytes("utf-8"));
                } catch (IOException e) {

                } finally {
                }

            }
        }
    }

    @Override
    protected void observeObjectDeletion(ElementsItemId.ObjectId objectId, StorableResourceType type){
        if (objectId.getItemSubType() == ElementsObjectCategory.USER) {
            safelyDeleteItem(objectId, MessageFormat.format("Unable to delete translated user-photo-rdf for user {0}", objectId.toString()));
        }
        //todo: make this marshall the resized and copied out photos somehow?
    }
}


