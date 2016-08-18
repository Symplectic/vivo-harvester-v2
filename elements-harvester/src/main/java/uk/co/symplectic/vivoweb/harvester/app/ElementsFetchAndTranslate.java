/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.app;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.args.UsageException;
import uk.co.symplectic.elements.api.*;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.utils.ExecutorServiceUtils;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.fetch.*;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.*;
import uk.co.symplectic.vivoweb.harvester.translate.ElementsObjectTranslateObserver;
import uk.co.symplectic.vivoweb.harvester.translate.ElementsRelationshipTranslateObserver;
import uk.co.symplectic.vivoweb.harvester.translate.ElementsVivoIncludeMonitor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ElementsFetchAndTranslate {
    /**
     * SLF4J Logger
     */
    private static final Logger log = LoggerFactory.getLogger(ElementsFetchAndTranslate.class);

    /**
     * Main method
     * @param args commandline arguments
     */
    public static void main(String[] args) {
        Throwable caught = null;
        try {
            try {
                Configuration.parse("ElementsFetchAndTranslate", args);

                log.debug("ElementsFetchAndTranslate: Start");

                setExecutorServiceMaxThreadsForPool("TranslationService",   Configuration.getMaxThreadsXsl());
                setExecutorServiceMaxThreadsForPool("ResourceFetchService", Configuration.getMaxThreadsResource());

                ElementsAPI elementsAPI = ElementsFetchAndTranslate.getElementsAPI();

                List<ElementsObjectCategory> aList = new ArrayList<ElementsObjectCategory>();
                aList.add(ElementsObjectCategory.USER);

                ElementsObjectCollection excludedUserStore = new ElementsObjectCollection();
                if(!StringUtils.isEmpty(Configuration.getGroupsToExclude())){
                    ElementsFetch.ObjectConfig objConfig = new ElementsFetch.ObjectConfig(100, ElementsObjectCategory.USER);
                    ElementsFetch excludedUserFetcher = new ElementsFetch(elementsAPI, excludedUserStore, objConfig, null, Configuration.getGroupsToExclude(), false);
                    excludedUserFetcher.execute();
                }
                Set<ElementsObjectId> excludedUsers = excludedUserStore.get(ElementsObjectCategory.USER);

                ElementsObjectFileStore objectStore = ElementsStoreFactory.getObjectStore();
                ElementsRdfStore rdfStore = ElementsStoreFactory.getRdfStore();

                boolean currentStaffOnly = Configuration.getCurrentStaffOnly();
                boolean visibleLinksOnly = Configuration.getVisibleLinksOnly();

                String xslFilename = Configuration.getXslTemplate();
                File vivoImageDir = ElementsFetchAndTranslate.getVivoImageDir(Configuration.getVivoImageDir());
                String vivoBaseURI = Configuration.getBaseURI();

                ElementsFetch.ObjectConfig objConfig = new ElementsFetch.ObjectConfig(Configuration.getApiObjectsPerPage(), Configuration.getCategoriesToHarvest());
                ElementsFetch.RelationshipConfig relConfig = new ElementsFetch.RelationshipConfig(Configuration.getApiRelationshipsPerPage());
                //Configure a fetcher to go to the elements API, pull across the requested data and drop the outputs into the object store
                ElementsFetch fetcher = new ElementsFetch(elementsAPI, objectStore, objConfig, relConfig, Configuration.getGroupsToHarvest(), true);

                //Hook item observers to the object store so that translations happen for any objects and relationships that arrive in that store
                //translations are configured to output to the rdfStore;
                ElementsObjectTranslateObserver objectObserver = new ElementsObjectTranslateObserver(rdfStore, xslFilename, currentStaffOnly, excludedUsers);
                ElementsRelationshipTranslateObserver relationshipObserver = new ElementsRelationshipTranslateObserver(rdfStore, xslFilename, currentStaffOnly, visibleLinksOnly);
                objectStore.addItemObserver(objectObserver);
                objectStore.addItemObserver(relationshipObserver);

                //Hook a photo retrieval observer onto the rdf store so that photos will be fetched and dropped in the object store for any translated users.
                rdfStore.addItemObserver(new ElementsUserPhotoRetrievalObserver(elementsAPI, objectStore));
                //Hook a photo RDF generating observer onto the object store so that any fetched photos have corresponding "rdf" created in the translated output.
                ElementsUserPhotoRdfGeneratingObserver userExtraPhotoRdfGenerator = new ElementsUserPhotoRdfGeneratingObserver(rdfStore, vivoImageDir, vivoBaseURI, null);
                objectStore.addItemObserver(userExtraPhotoRdfGenerator);

                //Hook a monitor to the object store to work out which objects and relationships we want to send to vivo
                ElementsVivoIncludeMonitor monitor = new ElementsVivoIncludeMonitor(excludedUsers, currentStaffOnly, visibleLinksOnly);
                objectStore.addItemObserver(monitor);

                //Now we have wired up all
                fetcher.execute();

                //Initiate the shutdown of the asynchronous translation engine - note this will actually block until
                //the engine has completed all its enqueued tasks - think of it as "await completion".
                TranslationService.shutdown();

                //TODO: make this cleanly use layout strategy instead of being hacky and make prune relationships properly too now
//                for (ElementsObjectCategory category : Configuration.getCategoriesToHarvest()) {
//                    if (category != null && category != ElementsObjectCategory.USER) {
//                        // Delete the RDF objects not marked to be kept
//                        rdfStore.pruneExcept(category, monitor.getIncludedObjects().get(category));
//                    }
//                }



                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("fileList.txt"), "utf-8"));
                    //collects all objects in relationships - if want obj not in rels (e.g. users in include with no links) then need to add to this output..
                    for (ElementsObjectCategory category : Configuration.getCategoriesToHarvest()) {
                        for (ElementsObjectId id : monitor.getIncludedObjects().get(category)) {
                            ElementsObjectInfo objInfo = ElementsObjectInfoCache.get(id.getCategory(), id.getId());
                            if (objInfo == null)
                                objInfo = ElementsItemInfo.createObjectItem(id.getCategory(), id.getId());
                            //TODO: restrict to certain output resource types?
                            for (ElementsStoredItem item : rdfStore.retrieveAllItems(objInfo)) {
                                writer.write(item.getFile().getAbsolutePath());
                            }
                        }
                    }

                    //add included relationships.
                    for (ElementsRelationshipInfo relInfo : monitor.getIncludedRelationships()) {
                        //TODO: restrict to certain output resource types?
                        for (ElementsStoredItem item : rdfStore.retrieveAllItems(relInfo)) {
                            writer.write(item.getFile().getAbsolutePath());
                        }
                    }

                }
                finally{
                    if(writer != null) writer.close();
                }

            } catch (IOException e) {
                System.err.println("Caught IOException initialising ElementsFetchAndTranslate");
                e.printStackTrace(System.err);
                caught = e;
            }

        } catch (UsageException e) {
            caught = e;
            if (!Configuration.isConfigured()) {
                System.out.println(Configuration.getUsage());
            } else {
                System.err.println("Caught UsageException initialising ElementsFetchAndTranslate");
                e.printStackTrace(System.err);
            }
        } catch(Exception e) {
            log.error("Unhandled Exception occurred during processing - terminating application", e);
            caught = e;
        }
        finally {
            log.debug("ElementsFetch: End");
            if (caught != null) {
                System.exit(1);
            }
        }
    }

    private static ElementsAPI getElementsAPI() {
        if (Configuration.getIgnoreSSLErrors()) {
            Protocol.registerProtocol("https", new Protocol("https", new IgnoreSSLErrorsProtocolSocketFactory(), 443));
        }

        String apiEndpoint = Configuration.getApiEndpoint();
        ElementsAPIVersion apiVersion = Configuration.getApiVersion();

        String apiUsername = Configuration.getApiUsername();
        String apiPassword = Configuration.getApiPassword();

        int soTimeout = Configuration.getApiSoTimeout();
        if (soTimeout > 4999 && soTimeout < (30 * 60 * 1000)) {
            ElementsAPIHttpClient.setSocketTimeout(soTimeout);
        }

        int requestDelay = Configuration.getApiRequestDelay();
        if (requestDelay > -1 && requestDelay < (5 * 60 * 1000)) {
            ElementsAPIHttpClient.setRequestDelay(requestDelay);
        }

        return new ElementsAPI(apiVersion, apiEndpoint, apiUsername, apiPassword);
    }

    private static File getVivoImageDir(String imageDir) {
        File vivoImageDir = null;
        // TODO: This should be a required configuration parameter that specifies a path accessible by the VIVO web container
        if (!StringUtils.isEmpty(imageDir)) {
            vivoImageDir = new File(imageDir);
            if (vivoImageDir.exists()) {
                if (!vivoImageDir.isDirectory()) {
                    vivoImageDir = null;
                }
            } else {
                vivoImageDir.mkdirs();
            }
        }

        return vivoImageDir;
    }

    private static void setExecutorServiceMaxThreadsForPool(String poolName, int maxThreads) {
        if (maxThreads > 0) {
            ExecutorServiceUtils.setMaxProcessorsForPool(poolName, maxThreads);
        }
    }
}
