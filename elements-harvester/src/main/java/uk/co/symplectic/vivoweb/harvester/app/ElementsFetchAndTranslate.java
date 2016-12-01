/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.app;

import org.apache.axis2.jaxws.marshaller.impl.alt.Element;
import org.apache.axis2.jaxws.message.Message;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.args.UsageException;
import org.vivoweb.harvester.util.repo.JenaConnect;
import org.vivoweb.harvester.util.repo.TDBJenaConnect;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.elements.api.ElementsAPIHttpClient;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.elements.api.IgnoreSSLErrorsProtocolSocketFactory;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.utils.ExecutorServiceUtils;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.fetch.*;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.*;
import uk.co.symplectic.vivoweb.harvester.translate.ElementsGroupTranslateObserver;
import uk.co.symplectic.vivoweb.harvester.translate.ElementsObjectTranslateObserver;
import uk.co.symplectic.vivoweb.harvester.translate.ElementsRelationshipTranslateObserver;
import uk.co.symplectic.vivoweb.harvester.translate.ElementsVivoIncludeMonitor;

import java.io.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
                //TODO: move working data directory to operate as a simple string passed in and have it delete that here too.
                //TODO: ensure that configured directories are valid (either already exist or can be created?)
                String interimTdbDirectory = Configuration.getTdbOutputDir();

                log.debug("ElementsFetchAndTranslate: Start");

                //Set up the services that will be used to do asynchronous work
                //TODO: move these elsewhere, or remove entirely?
                setExecutorServiceMaxThreadsForPool("TranslationService",   Configuration.getMaxThreadsXsl());
                setExecutorServiceMaxThreadsForPool("ResourceFetchService", Configuration.getMaxThreadsResource());

                //Set up the Elements API and a fetcher that uses it.
                ElementsAPI elementsAPI = ElementsFetchAndTranslate.getElementsAPI();
                ElementsFetch elementsFetcher = new ElementsFetch(elementsAPI);

                //Set up the objectStore (for raw Elements API data) and the rdfStore (for translated RDF XML data)
                ElementsItemFileStore objectStore = ElementsStoreFactory.getObjectStore();
                ElementsRdfStore rdfStore = ElementsStoreFactory.getRdfStore();

                //Get some config needed for wiring up the translation observers
                String xslFilename = Configuration.getXslTemplate();
                File vivoImageDir = ElementsFetchAndTranslate.getDirectoryFromPath(Configuration.getVivoImageDir());
                String vivoBaseURI = Configuration.getBaseURI();

                //Hook item observers to the object store so that translations happen for any objects and relationships that arrive in that store
                //translations are configured to output to the rdfStore;
                objectStore.addItemObserver(new ElementsObjectTranslateObserver(rdfStore, xslFilename));
                objectStore.addItemObserver(new ElementsRelationshipTranslateObserver(objectStore, rdfStore, xslFilename));

                //TODO: decide if this pattern is necessary - exists to minimise putting photos of users that are not to be included in a web acessible area..
                //TODO: this is inaccurate though as "translated" has nothing to do with "included" anymore.
                //Hook a photo retrieval observer onto the rdf store so that photos will be fetched and dropped in the object store for any translated users.
                objectStore.addItemObserver(new ElementsUserPhotoRetrievalObserver(elementsAPI, objectStore));
                //Hook a photo RDF generating observer onto the object store so that any fetched photos have corresponding "rdf" created in the translated output.
                objectStore.addItemObserver(new ElementsUserPhotoRdfGeneratingObserver(rdfStore, vivoImageDir, vivoBaseURI, null));

                //TODO: decide if a full harvest should still work this way...
                //Hook a monitor to the object store to work out which objects and relationships we want to send to vivo
                //ElementsVivoIncludeMonitor monitor = new ElementsVivoIncludeMonitor(includedUsers.keySet(), includedGroups.keySet(), visibleLinksOnly);
                //objectStore.addItemObserver(monitor);

                String aString = "2016-11-23T17:41:39+0000";
                //for melbourne testing
                aString = "2016-10-10T17:41:39+0000";
                SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                Date aDate = dateParser.parse(aString);
                //aDate = null;

                //TODO: make it configurable whether do deltas or not for different sections..
                //Now we have wired up all our store observers perform the main fetches
                processObjects(objectStore, elementsFetcher, aDate);
                //fetch relationships.
                processRelationships(objectStore, elementsFetcher, aDate);

                //load the user cache from the now up to date full cache of user definitions on disk ..(they MUST be present)..
                ElementsItemKeyedCollection.ItemRestrictor restrictToUsers = new ElementsItemKeyedCollection.RestrictToSubTypes(ElementsObjectCategory.USER);
                ElementsItemKeyedCollection.ItemInfo userInfoCache = new ElementsItemKeyedCollection.ItemInfo(restrictToUsers);
                for (StoredData.InFile userData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_OBJECT, ElementsObjectCategory.USER)){
                    ElementsStoredItem userItem = ElementsStoredItem.InFile.loadRawObject(userData.getFile(), userData.isZipped());
                    userInfoCache.put(userItem.getItemInfo().getItemId(), userItem.getItemInfo());
                }

                //Now query the groups, post processing to build a group hierarchy containing users.
                //TODO: only process group membership ? and maybe even user cache above for harvested groups?
                //TODO: - note user cache would break optimisation to not process them later when pulling objects (need full set during translation stage)
                ElementsGroupCollection groupCache = new ElementsGroupCollection();
                elementsFetcher.execute(new ElementsFetch.GroupConfig(), groupCache.getStoreWrapper());
                ElementsGroupInfo.GroupHierarchyWrapper groupHierarchy = groupCache.constructHierarchy();
                groupCache.populateUserMembership(elementsFetcher, userInfoCache.keySet());

                //work out the included users
                ElementsItemKeyedCollection.ItemInfo includedUsers = CalculateIncludedUsers(userInfoCache, groupCache);
                //work out the included groups too..
                ElementsItemKeyedCollection.ItemInfo includedGroups = CalculateIncludedGroups(groupCache);

                //Wire up the group translation observer...(needs group cache to work out members Ids and included users to get the user info of those members)
                objectStore.addItemObserver(new ElementsGroupTranslateObserver(rdfStore, xslFilename, groupCache, includedUsers));
                //fetch the groups and translate them
                objectStore.cleardown(StorableResourceType.RAW_GROUP);
                elementsFetcher.execute(new ElementsFetch.GroupConfig(), objectStore);

                //Initiate the shutdown of the asynchronous translation engine - note this will actually block until
                //the engine has completed all its enqueued tasks - think of it as "await completion".
                TranslationService.awaitShutdown();

                //changes towards making include monitoring a separate step in the process?

                //Hook a monitor up to work out which objects and relationships we want to send to vivo
                // when building a triple store to represent the current state after this connector run.
                //write out a list of translated rdf files that ought to be included
                //TODO: establish if a full load should do this in line as it used to....
                log.debug("ElementsFetchAndTranslate: Processing all relationships to establish which items to include in final output");
                //TODO: better logging and also test performance against spinning rust...

                boolean visibleLinksOnly = Configuration.getVisibleLinksOnly();
                ElementsVivoIncludeMonitor monitor = new ElementsVivoIncludeMonitor(includedUsers.keySet(), includedGroups.keySet(), visibleLinksOnly);

                for(StoredData.InFile relData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_RELATIONSHIP)){
                    ElementsStoredItem relItem = ElementsStoredItem.InFile.loadRawRelationship(relData.getFile(), relData.isZipped());
                    monitor.observe(relItem);
                }

                List<StoredData.InFile> filesToProcess = new ArrayList<StoredData.InFile>();
                BufferedWriter writer = null;
                try {
                    //TODO: check this all works as expected and move file location to config remove prune stuff
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("fileList.txt"), "utf-8"));

                    //TODO: make ElementsItemCollections order things better - particularly within the objects type
                    for(ElementsItemType type : ElementsItemType.values()) {
                        for (ElementsItemId includedItem : monitor.getIncludedItems().get(type)) {
                            //TODO: restrict to certain output resource types?
                            for (BasicElementsStoredItem item : rdfStore.retrieveAllItems(includedItem)) {
                                filesToProcess.add((StoredData.InFile) item.getStoredData());
                                writer.write(item.getStoredData().getAddress());
                                writer.newLine();
                            }
                        }
                    }
                }
                finally{
                    if(writer != null) writer.close();
                }
                log.debug("ElementsFetchAndTranslate: Finished processing relationships");
                //end of changes towards making include monitoring a separate step in the process

                //todo: move this to configuration
                FileUtils.deleteDirectory(new File(interimTdbDirectory));
                log.debug(MessageFormat.format("ElementsFetchAndTranslate: Transferring data to triplestore \"{0}\"", interimTdbDirectory));
                JenaConnect jc = new TDBJenaConnect(interimTdbDirectory);
                //JenaConnect jc = new TDBJenaConnect(interimTdbDirectory, "http://vitro.mannlib.cornell.edu/default/vitro-kb-2");
                TDBLoadUtility.load(jc, filesToProcess.iterator());
                log.debug("ElementsFetchAndTranslate: Finished transferring data to triplestore");


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

    private static void processObjects(ElementsItemFileStore objectStore, ElementsFetch elementsFetcher, Date modifiedSince) throws IOException{
        //fetch all configured categories - ensure that users ARE fetched regardless of configuration
        //TODO: decide if not having users in configured categories should result in different behaviour in the monitor
        List<ElementsObjectCategory> categories = new ArrayList<ElementsObjectCategory>();
        categories.addAll(Configuration.getCategoriesToHarvest());
        if(!categories.contains(ElementsObjectCategory.USER)) categories.add(0, ElementsObjectCategory.USER);

        if(modifiedSince == null) {
            //TODO: make this at least log what is happening ..check is doing deletes of additional resources correctly.
            objectStore.cleardown(StorableResourceType.RAW_OBJECT);
            ElementsFetch.ObjectConfig objConfig = new ElementsFetch.ObjectConfig(true, Configuration.getApiObjectsPerPage(), null, categories);
            elementsFetcher.execute(objConfig, objectStore);
        }
        else{
            ElementsFetch.ObjectConfig objConfig = new ElementsFetch.ObjectConfig(true, Configuration.getApiObjectsPerPage(), modifiedSince, categories);
            //todo : move higher value per page to config somehow
            ElementsFetch.ObjectConfig delObjConfig = new ElementsFetch.DeletedObjectConfig(true, 100, modifiedSince, categories);
            elementsFetcher.execute(objConfig, objectStore);
            elementsFetcher.execute(delObjConfig, objectStore);
            //todo: need to capture all delta affected objects and update their corresponding relationships to handle translation dependancies (and also to workaround issues in API)
        }
    }

    private static void processRelationships(ElementsItemFileStore objectStore, ElementsFetch elementsFetcher, Date modifiedSince) throws IOException{
        if(modifiedSince == null) {
            //TODO: make this at least log what is happening
            objectStore.cleardown(StorableResourceType.RAW_RELATIONSHIP);
            //fetch relationships.
            ElementsFetch.RelationshipConfig relConfig = new ElementsFetch.RelationshipConfig(null, Configuration.getApiRelationshipsPerPage());
            elementsFetcher.execute(relConfig, objectStore);
        }
        else{
            ElementsFetch.RelationshipConfig relConfig = new ElementsFetch.RelationshipConfig(modifiedSince, Configuration.getApiRelationshipsPerPage());
            ElementsFetch.RelationshipConfig delRelConfig = new ElementsFetch.DeletedRelationshipConfig(modifiedSince, Configuration.getApiRelationshipsPerPage());
            ElementsFetch.ObjectsRelationshipsConfig reprocessForModifiedObjectsConfig = new ElementsFetch.ObjectsRelationshipsConfig(objectStore.getAffectedItems(StorableResourceType.RAW_OBJECT), Configuration.getApiRelationshipsPerPage());
            elementsFetcher.execute(relConfig, objectStore);
            //TODO : make this not translate if already processed relationship this run?
            elementsFetcher.execute(reprocessForModifiedObjectsConfig, objectStore);
            elementsFetcher.execute(delRelConfig, objectStore);
        }
    }


    private static ElementsItemKeyedCollection.ItemInfo CalculateIncludedGroups(ElementsGroupCollection groupCache) {
        ElementsItemKeyedCollection.ItemRestrictor restrictToGroupsOnly = new ElementsItemKeyedCollection.RestrictToType(ElementsItemType.GROUP);
        ElementsItemKeyedCollection.ItemInfo includedGroups = new ElementsItemKeyedCollection.ItemInfo(restrictToGroupsOnly);

        ElementsItemId.GroupId topLevelGroupId = (ElementsItemId.GroupId) groupCache.GetTopLevel().getGroupInfo().getItemId();
        //if there are no groups configured to be harvested or if the set includes the top level then we just need everything.
        if(Configuration.getGroupsToHarvest().isEmpty() || Configuration.getGroupsToHarvest().contains(topLevelGroupId)) {
            for(ElementsGroupInfo.GroupHierarchyWrapper groupWrapper : groupCache.values()) {
                ElementsGroupInfo info = groupWrapper.getGroupInfo();
                includedGroups.put(info.getItemId(), info);
            }
        }
        else {
            for (ElementsItemId.GroupId groupId : Configuration.getGroupsToHarvest()) {
                ElementsGroupInfo.GroupHierarchyWrapper currentGroup = groupCache.get(groupId);
                ElementsGroupInfo info = currentGroup.getGroupInfo();
                includedGroups.put(info.getItemId(), info);
                for(ElementsGroupInfo.GroupHierarchyWrapper childGroupWrapper : currentGroup.getAllChildren()){
                    ElementsGroupInfo childInfo = childGroupWrapper.getGroupInfo();
                    includedGroups.put(childInfo.getItemId(), childInfo);
                }
            }
        }

        for(ElementsItemId.GroupId groupId : Configuration.getGroupsToExclude()){
            //todo: ensure boxing can't fail here?
            ElementsGroupInfo.GroupHierarchyWrapper currentGroup = groupCache.get(groupId);
            ElementsGroupInfo info = currentGroup.getGroupInfo();
            includedGroups.remove(info.getItemId());
            for(ElementsGroupInfo.GroupHierarchyWrapper childGroupWrapper : currentGroup.getAllChildren()){
                ElementsGroupInfo childInfo = childGroupWrapper.getGroupInfo();
                includedGroups.remove(childInfo.getItemId());
            }
        }

        return includedGroups;
    }

    private static ElementsItemKeyedCollection.ItemInfo CalculateIncludedUsers(ElementsItemKeyedCollection.ItemInfo userInfoCache, ElementsGroupCollection groupCache){
        //TODO : move academics into a config item.
        boolean academicsOnly = false; //Configuration.getAcademicsOnly();
        boolean currentStaffOnly = Configuration.getCurrentStaffOnly();

        //find the users who are defnitely not to be included based on their raw metadata
        List<ElementsItemId> invalidUsers = new ArrayList<ElementsItemId>();
        for(ElementsItemInfo objInfo : userInfoCache.values()){
            ElementsUserInfo userInfo = (ElementsUserInfo) objInfo;
            //if user is not currentStaff then we don't want them
            if (currentStaffOnly && !userInfo.getIsCurrentStaff()) invalidUsers.add(userInfo.getObjectId());
            if (academicsOnly && !userInfo.getIsAcademic()) invalidUsers.add(userInfo.getObjectId());
        }

        //Work out which users we are planning to include (i.e who is in the included set, not in the excluded set

        ElementsItemKeyedCollection.ItemRestrictor restrictToUsersOnly = new ElementsItemKeyedCollection.RestrictToSubTypes(ElementsObjectCategory.USER);
        ElementsItemKeyedCollection.ItemInfo includedUsers = new ElementsItemKeyedCollection.ItemInfo(restrictToUsersOnly);
        if(Configuration.getGroupsToHarvest().isEmpty()) {
            //we need to copy the users over as we do more filtering below
            for(ElementsItemInfo userInfo : userInfoCache.values()) {
                if(!invalidUsers.contains(userInfo.getItemId()))
                    includedUsers.put(userInfo.getItemId(), userInfo);
            }
        }
        else {
            for (ElementsItemId.GroupId groupId : Configuration.getGroupsToHarvest()) {
                ElementsGroupInfo.GroupHierarchyWrapper group = groupCache.get(groupId);
                for(ElementsItemId userId : group.getImplicitUsers()){
                    if(!invalidUsers.contains(userId))
                        includedUsers.put(userId, userInfoCache.get(userId));
                }
            }
        }

        for(ElementsItemId.GroupId groupId : Configuration.getGroupsToExclude()){
            //todo: ensure boxing can't fail here?
            ElementsGroupInfo.GroupHierarchyWrapper group = groupCache.get(groupId);
            includedUsers.removeAll(group.getImplicitUsers());
        }

        return includedUsers;
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

    private static File getDirectoryFromPath(String path) {
        File file = null;
        // TODO: This should be a required configuration parameter that specifies a path accessible by the VIVO web container
        if (!StringUtils.isEmpty(path)) {
            file = new File(path);
            if (file.exists()) {
                if (!file.isDirectory()) {
                    throw new IllegalStateException(MessageFormat.format("Path {0} is not a directory", path));
                }
            } else {
                file.mkdirs();
            }
        }
        return file;
    }

    private static void setExecutorServiceMaxThreadsForPool(String poolName, int maxThreads) {
        if (maxThreads > 0) {
            ExecutorServiceUtils.setMaxProcessorsForPool(poolName, maxThreads);
        }
    }
}
