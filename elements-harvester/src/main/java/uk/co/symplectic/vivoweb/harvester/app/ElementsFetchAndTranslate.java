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
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsGroupCollection;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.*;
import uk.co.symplectic.vivoweb.harvester.translate.*;

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
        //TODO: fix issues with Config taking ages to load if data is present in the source directory, -needs to work for deltas
        Throwable caught = null;
        try {
            try {
                Configuration.parse("ElementsFetchAndTranslate", args);

                log.debug("ElementsFetchAndTranslate: Start");

                setExecutorServiceMaxThreadsForPool("TranslationService",   Configuration.getMaxThreadsXsl());
                setExecutorServiceMaxThreadsForPool("ResourceFetchService", Configuration.getMaxThreadsResource());

                ElementsAPI elementsAPI = ElementsFetchAndTranslate.getElementsAPI();
                ElementsFetch elementsFetcher = new ElementsFetch(elementsAPI);

                //hack
                ///*
                //start by querying all users - to build a user cache, keep the data for now to avoid needing to re-query users at translation stage
                //TODO: optimise to make this do the initial user translation once translations are independent of excluded users, etc.
                ElementsObjectKeyedCollection.ObjectInfo userInfoCache = new ElementsObjectKeyedCollection.ObjectInfo(ElementsObjectCategory.USER);
                ElementsObjectKeyedCollection.Data userDataCache = new ElementsObjectKeyedCollection.Data(ElementsObjectCategory.USER);
                ElementsFetch.ObjectConfig userConfig = new ElementsFetch.ObjectConfig(true, 25, null, ElementsObjectCategory.USER);
                elementsFetcher.execute(userConfig, new ElementsObjectStore.MultiStore(userInfoCache.getStoreWrapper(), userDataCache.getStoreWrapper()));

                //Now query the groups, post processing to build a group hierarchy containing users.
                //TODO: only process group membership ? and maybe even user cache above for harvested groups?
                //TODO: - note user cache would break optimisation to not process them later when pulling objects (need full set during translation stage)
                ElementsGroupCollection groupCache = new ElementsGroupCollection();
                elementsFetcher.execute(new ElementsFetch.GroupConfig(), groupCache);
                ElementsGroupInfo.GroupHierarchyWrapper groupHierarchy = groupCache.constructHierarchy();
                //todo : undo hack to not populate membership for speed of testing
                groupCache.populateUserMembership(elementsFetcher, userInfoCache.keySet());


                //TODO : move academics into a config item.
                boolean academicsOnly = false; //Configuration.getAcademicsOnly();
                boolean currentStaffOnly = Configuration.getCurrentStaffOnly();


                //Work out which users we are planning to include (i.e who is in the included set, not in the excluded set
                ElementsObjectKeyedCollection.ObjectInfo includedUsers = new ElementsObjectKeyedCollection.ObjectInfo(ElementsObjectCategory.USER);
                if(Configuration.getGroupsToHarvest().isEmpty()) {
                    for(ElementsItemId.ObjectId userId : userInfoCache.keySet()) {
                        includedUsers.put(userId, userInfoCache.get(userId));
                    }
                }
                else {
                    for (Integer groupId : Configuration.getGroupsToHarvest()) {
                        //todo: ensure boxing can't fail here?
                        ElementsGroupInfo.GroupHierarchyWrapper group = groupCache.getGroup(groupId.intValue());
                        for(ElementsItemId.ObjectId userId : group.getImplicitUsers()){
                            includedUsers.put(userId, userInfoCache.get(userId));
                        }
                    }
                }

                for(Integer groupId : Configuration.getGroupsToExclude()){
                    //todo: ensure boxing can't fail here?
                    ElementsGroupInfo.GroupHierarchyWrapper group = groupCache.getGroup(groupId.intValue());
                    includedUsers.removeAll(group.getImplicitUsers());
                }

                List<ElementsItemId.ObjectId> invalidUsers = new ArrayList<ElementsItemId.ObjectId>();
                for(ElementsObjectInfo objInfo : includedUsers.values()){
                    ElementsUserInfo userInfo = (ElementsUserInfo) objInfo;
                    //if user is not currentStaff then we don't want them
                    if (currentStaffOnly && !userInfo.getIsCurrentStaff()) invalidUsers.add(userInfo.getObjectId());
                    if (academicsOnly && !userInfo.getIsAcademic()) invalidUsers.add(userInfo.getObjectId());
                }
                for(ElementsItemId.ObjectId userId : invalidUsers) includedUsers.remove(userId);
                //*/

                //hack pt 2
                /*
                ElementsObjectCollection includedUsers = new ElementsObjectCollection();
                includedUsers.add(new ElementsObjectId(ElementsObjectCategory.USER, 23));
                //*/
                //end hack

                ElementsObjectFileStore objectStore = ElementsStoreFactory.getObjectStore();
                ElementsRdfStore rdfStore = ElementsStoreFactory.getRdfStore();

                boolean visibleLinksOnly = Configuration.getVisibleLinksOnly();

                String xslFilename = Configuration.getXslTemplate();
                File vivoImageDir = ElementsFetchAndTranslate.getVivoImageDir(Configuration.getVivoImageDir());
                String vivoBaseURI = Configuration.getBaseURI();

                //Hook item observers to the object store so that translations happen for any objects and relationships that arrive in that store
                //translations are configured to output to the rdfStore;
                //todo: test the new translators and remove redundant code.
//                ElementsObjectTranslateObserver objectTranslator = new ElementsObjectTranslateObserver(rdfStore, xslFilename);
//                ElementsRelationshipTranslateObserver relationshipTranslator = new ElementsRelationshipTranslateObserver(rdfStore, xslFilename);
//                ElementsGroupTranslateObserver groupTranslator = new ElementsGroupTranslateObserver(rdfStore, xslFilename, groupCache, includedUsers);
//                objectStore.addItemObserver(objectTranslator);
//                objectStore.addItemObserver(relationshipTranslator);
//                objectStore.addItemObserver(groupTranslator);

                //new style translators
                ElementsTranslateObserver n_objectTranslator = new ElementsTranslateObserver.Objects(rdfStore, xslFilename);
                ElementsTranslateObserver n_relationshipTranslator = new ElementsTranslateObserver.Relationships(objectStore, rdfStore, xslFilename);
                ElementsTranslateObserver n_groupTranslator = new ElementsTranslateObserver.Groups(rdfStore, xslFilename, groupCache, includedUsers);
                objectStore.addItemObserver(n_objectTranslator);
                objectStore.addItemObserver(n_relationshipTranslator);
                objectStore.addItemObserver(n_groupTranslator);

                //Hook a photo retrieval observer onto the rdf store so that photos will be fetched and dropped in the object store for any translated users.
                rdfStore.addItemObserver(new ElementsUserPhotoRetrievalObserver(elementsAPI, objectStore));
                //Hook a photo RDF generating observer onto the object store so that any fetched photos have corresponding "rdf" created in the translated output.
                ElementsUserPhotoRdfGeneratingObserver userExtraPhotoRdfGenerator = new ElementsUserPhotoRdfGeneratingObserver(rdfStore, vivoImageDir, vivoBaseURI, null);
                objectStore.addItemObserver(userExtraPhotoRdfGenerator);

                //Hook a monitor to the object store to work out which objects and relationships we want to send to vivo
                //TODO: move monitor to file reading approach to be delta usable.
                ElementsVivoIncludeMonitor monitor = new ElementsVivoIncludeMonitor(includedUsers.keySet(), visibleLinksOnly);
                objectStore.addItemObserver(monitor);

                //Now we have wired up all our store observers perform the main fetches

                //already fetched user data so use that now to avoid re-fetching.
                log.info("Processing previously cached users");
                for(ElementsObjectInfo info : userInfoCache.values()){
                    objectStore.storeItem(info, StorableResourceType.RAW_OBJECT, userDataCache.get(info.getObjectId()));
                }

                //get list of categories we will process - remove users to avoid fetching them twice
                List<ElementsObjectCategory> categories = new ArrayList<ElementsObjectCategory>();
                categories.addAll(Configuration.getCategoriesToHarvest());
                categories.remove(ElementsObjectCategory.USER);
                userDataCache.clear();

                //TODO : make groups output something based on cache? possibly doing something nice around user membership via cache and includedUser set
                //TODO : ? should do this last or at least section clearly into things that ARE reloaded every time and things that are not.
                elementsFetcher.execute(new ElementsFetch.GroupConfig(), objectStore);

                ElementsFetch.ObjectConfig objConfig = new ElementsFetch.ObjectConfig(true, Configuration.getApiObjectsPerPage(), null, categories);
                elementsFetcher.execute(objConfig, objectStore);

                ElementsFetch.RelationshipConfig relConfig = new ElementsFetch.RelationshipConfig(Configuration.getApiRelationshipsPerPage());
                elementsFetcher.execute(relConfig, objectStore);

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
                    //TODO: check this all works as expected and move file location to config remove prune stuff
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("fileList.txt"), "utf-8"));
                    //collects all objects in relationships - if want obj not in rels (e.g. users in include with no links) then need to add to this output..
                    for (ElementsObjectCategory category : Configuration.getCategoriesToHarvest()) {
                        for (ElementsItemId.ObjectId id : monitor.getIncludedObjects().get(category)) {
                            ElementsObjectInfo objInfo = ElementsObjectInfoCache.get(id);
                            if (objInfo == null)
                                objInfo = ElementsItemInfo.createObjectItem(id.getCategory(), id.getId());
                            //TODO: restrict to certain output resource types?
                            for (ElementsStoredItem item : rdfStore.retrieveAllItems(objInfo)) {
                                writer.write(item.getAddress());
                                writer.newLine();
                            }
                        }
                    }

                    //add included relationships.
                    for (ElementsRelationshipInfo relInfo : monitor.getIncludedRelationships()) {
                        //TODO: restrict to certain output resource types?
                        for (ElementsStoredItem item : rdfStore.retrieveAllItems(relInfo)) {
                            writer.write(item.getAddress());
                            writer.newLine();
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
