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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.utils.LoggingUtils;
import uk.co.symplectic.utils.configuration.ConfigParser;
import uk.co.symplectic.utils.triplestore.*;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.utils.http.HttpClient;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


//import org.vivoweb.harvester.util.args.UsageException;

public class ElementsFetchAndTranslate {
    /**
     * SLF4J Logger
     */
    final private static Logger log = LoggerFactory.getLogger(ElementsFetchAndTranslate.class);


    /**
     * Date format that is used within state file to keep track of when the last successfully completed run occurred.
     */
    final private static SimpleDateFormat lastRunDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");


    /**
     * StateType keeps track of whether this is an odd or an even run (i.e if this process has been run n times, is n%2 0 or 1)
     */
    private enum StateType{
        ODD,
        EVEN
    }

    /**
     * Main entry method for ElementsFetchAndTranslate
     * valid args defined by Configuration class
     *
     * The Purpose of process is to update a local cache of Elements data to the current time.
     * The data in this cache is translated (to the Vivo ontology) using the configured XSLT scripts.
     * The process then calculates what data is to be sent to vivo given the current configuration
     * it then populates a TDB triple store with that data.
     * This temporary triple store is  compared to the equivalent output from the previous run and difference files created.
     * These additions and subtractions files are then split into fragments.
     * These fragments can then be loaded into a live vivo using a FragmentLoader monitor process.
     *
     * @param args commandline arguments
     */
    public static void main(String[] args) {
        Throwable caught = null;
        try {
            try {

                LoggingUtils.initialise("eft_logback.xml");
                log.info(MessageFormat.format("running {0}", "ElementsFetchAndTranslate"));
                Configuration.parse("elementsfetch.properties");
                log.info(Configuration.getConfiguredValues());

                boolean forceFullPull = args.length != 0 && args[0].equals("--full");
                boolean updateLocalTDB = true;

                //when did we start this run (i.e how "up to date" can we claim to be at the end).
                Date runStartedAt = new Date();
                //todo: move to config?
                File stateFile = new File("state.txt");
                //how many times has this process been run?
                int runCount = 0;
                //initially we assume there never has been a previous run (or at least we don't know when it was).
                //TODO: decide if having no prior run should remove any prior loads in the TDB layer..?
                Date lastRunDate = null;

                //TODO: improve logging about loading state file... improve it when writing it too.
                BufferedReader reader = null;
                if(stateFile.exists()) {
                    try {
                        reader = new BufferedReader(new InputStreamReader(new FileInputStream(stateFile), "utf-8"));
                        String str;
                        int counter = 0;
                        while ((str = reader.readLine()) != null) {
                            if (counter == 0) {
                                //load number of last completed run
                                runCount = Integer.parseInt(str);
                                //increment for this run.
                                runCount++;
                            } else if (counter == 1) {
                                lastRunDate = lastRunDateFormat.parse(str);
                            } else {
                                log.debug("state.txt file appears to be corrupt - too many lines detected");
                                throw new IllegalStateException("state.txt is corrupt");
                            }
                            counter++;
                        }

                    } catch (IOException e) {
                        log.debug("Could not successfully load information from state.txt file.");
                        throw new IllegalStateException("state.txt is corrupt", e);
                    } catch (NumberFormatException e) {
                        log.debug("Could not successfully load run count from state.txt file. ");
                        throw new IllegalStateException("state.txt is corrupt", e);
                    } catch (ParseException e) {
                        log.debug("Could not successfully load last run date from state.txt file. ");
                        throw new IllegalStateException("state.txt is corrupt", e);
                    } finally {
                        if (reader != null) reader.close();
                    }
                }

                //test if we have successfully loaded a run count, if not set it to zero and initiate a full;
                StateType runType = (runCount%2 == 0) ? StateType.EVEN : StateType.ODD;

                //if we are pulling all data then we want to force the run to be a full reload of the on disk cache.
                if(forceFullPull) lastRunDate = null;

                if(lastRunDate == null){
                    if(forceFullPull) {
                        log.info("Performing forced full pull of data (--full).");
                    } else {
                        log.info("Performing initial full pull of data.");
                    }
                } else {
                    log.info(MessageFormat.format("Performing differential pull (processing changes since {0}).", lastRunDateFormat.format(lastRunDate)));
                }

                //hacks for testing
                //from next line read the date time
                //String aString = "2016-11-23T17:41:39+0000";
                //for melbourne testing
                //aString = "2016-10-10T17:41:39+0000"; //old date - 50000 pubs 200000 affected rels
                //aString = "2016-10-13T16:01:39+0000"; //much smaller diff date - 7500 off pubs
                //Date aDate = lastRunDateFormat.parse(aString);

                //runType = StateType.ODD;
                //end of hacks for testing.

                //TODO: ensure that configured directories are valid (either already exist or can be created?)
                File interimTdbDirectory = Configuration.getTdbOutputDir();
                interimTdbDirectory.mkdirs();
                File currentTdbStore = new File(interimTdbDirectory, runType == StateType.EVEN ? "0" : "1");
                File previousTdbStore = new File(interimTdbDirectory, runType == StateType.ODD ? "0" : "1");

                log.debug("ElementsFetchAndTranslate: Start");



                if(updateLocalTDB) {
                    //Set up the services that will be used to do asynchronous work
                    //TODO: move these elsewhere, or remove entirely?
                    setExecutorServiceMaxThreadsForPool("TranslationService", Configuration.getMaxThreadsXsl());
                    setExecutorServiceMaxThreadsForPool("ResourceFetchService", Configuration.getMaxThreadsResource());

                    //TODO: make these configurable?
                    Set<String> relationshipTypesNeedingObjectsForTranslation = new HashSet<String>(Arrays.asList("activity-user-association", "user-teaching-association"));

                    //Set up the Elements API and a fetcher that uses it.
                    ElementsAPI elementsAPI = ElementsFetchAndTranslate.getElementsAPI();
                    ElementsFetch elementsFetcher = new ElementsFetch(elementsAPI);


                    ElementsRdfStore rdfStore = ElementsStoreFactory.getRdfStore();
                    //Set up the objectStore (for raw Elements API data) and the rdfStore (for translated RDF XML data)
                    ElementsItemFileStore objectStore = ElementsStoreFactory.getObjectStore();

                    //Get some config needed for wiring up the translation observers
                    String xslFilename = Configuration.getXslTemplate();
                    File vivoImageDir = ElementsFetchAndTranslate.getDirectoryFromPath(Configuration.getVivoImageDir());
                    String vivoBaseURI = Configuration.getBaseURI();

                    //Hook item observers to the object store so that translations happen for any objects and relationships that arrive in that store
                    //translations are configured to output to the rdfStore;
                    objectStore.addItemObserver(new ElementsObjectTranslateObserver(rdfStore, xslFilename));
                    objectStore.addItemObserver(new ElementsRelationshipTranslateObserver(objectStore, rdfStore, xslFilename, relationshipTypesNeedingObjectsForTranslation));

                    //TODO: work out how to marshall user photos into a web accessible area in a sensible manner based on included user set...
                    //Hook a photo retrieval observer onto the rdf store so that photos will be fetched and dropped in the object store for any translated users.
                    objectStore.addItemObserver(new ElementsUserPhotoRetrievalObserver(elementsAPI, objectStore));
                    //Hook a photo RDF generating observer onto the object store so that any fetched photos have corresponding "rdf" created in the translated output.
                    objectStore.addItemObserver(new ElementsUserPhotoRdfGeneratingObserver(rdfStore, vivoImageDir, vivoBaseURI, null));

                    //Now we have wired up all our store observers perform the main fetches
                    //NOTE: order is absolutely vital (relationship translation scripts can rely on raw object data having been fetched)
                    processObjects(objectStore, elementsFetcher, lastRunDate);
                    //fetch relationships.
                    //TODO: alter processRelationships call to just do re-processing of the the relevant links when re-pulling is not necessary.. (do based on API version?)
                    //processRelationships(objectStore, elementsFetcher, aDate, true, relationshipTypesNeedingObjectsForTranslation);
                    processRelationships(objectStore, elementsFetcher, lastRunDate, true, relationshipTypesNeedingObjectsForTranslation);


                    //load the user cache from the now up to date full cache of user definitions on disk ..(they MUST be present)..
                    ElementsItemKeyedCollection.ItemRestrictor restrictToUsers = new ElementsItemKeyedCollection.RestrictToSubTypes(ElementsObjectCategory.USER);
                    ElementsItemKeyedCollection.ItemInfo userInfoCache = new ElementsItemKeyedCollection.ItemInfo(restrictToUsers);
                    for (StoredData.InFile userData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_OBJECT, ElementsObjectCategory.USER)) {
                        ElementsStoredItem userItem = ElementsStoredItem.InFile.loadRawObject(userData.getFile(), userData.isZipped());
                        userInfoCache.put(userItem.getItemInfo().getItemId(), userItem.getItemInfo());
                    }

                    //Now query the groups, post processing to build a group hierarchy containing users.
                    ElementsGroupCollection groupCache = new ElementsGroupCollection();
                    elementsFetcher.execute(new ElementsFetch.GroupConfig(), groupCache.getStoreWrapper());
                    groupCache.constructHierarchy();
                    groupCache.populateUserMembership(elementsFetcher, userInfoCache.keySet());

                    //work out the included users
                    ElementsItemKeyedCollection.ItemInfo includedUsers = CalculateIncludedUsers(userInfoCache, groupCache);
                    //work out the included groups too..
                    ElementsItemKeyedCollection.ItemInfo includedGroups = CalculateIncludedGroups(groupCache);

                    //Wire up the group translation observer...(needs group cache to work out members Ids and included users to get the user info of those members)
                    objectStore.addItemObserver(new ElementsGroupTranslateObserver(rdfStore, xslFilename, groupCache, includedUsers));
                    //fetch the groups and translate them
                    log.debug("Clearing down old group cache");
                    objectStore.cleardown(StorableResourceType.RAW_GROUP);
                    elementsFetcher.execute(new ElementsFetch.GroupConfig(), objectStore);

                    //Initiate the shutdown of the asynchronous translation engine - note this will actually block until
                    //the engine has completed all its enqueued tasks - think of it as "await completion".
                    TranslationService.awaitShutdown();

                    //changes towards making include monitoring a separate step in the process?

                    //Hook a monitor up to work out which objects and relationships we want to send to vivo
                    // when building a triple store to represent the current state after this connector run.
                    //write out a list of translated rdf files that ought to be included
                    log.debug("ElementsFetchAndTranslate: Processing cached relationships to establish which items to include in final output");
                    //TODO: test performance against spinning rust...

                    boolean visibleLinksOnly = Configuration.getVisibleLinksOnly();
                    ElementsVivoIncludeMonitor monitor = new ElementsVivoIncludeMonitor(includedUsers.keySet(), includedGroups.keySet(), visibleLinksOnly);

                    int counter = 0;
                    for (StoredData.InFile relData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_RELATIONSHIP)) {
                        ElementsStoredItem relItem = ElementsStoredItem.InFile.loadRawRelationship(relData.getFile(), relData.isZipped());
                        monitor.observe(relItem);
                        counter++;
                        if (counter % 10000 == 0)
                            log.debug(MessageFormat.format("ElementsFetchAndTranslate: {0} relationships processed from cache", counter));
                    }
                    log.debug(MessageFormat.format("ElementsFetchAndTranslate: finished processing relationships from cache, {0} items processed in total", counter));

                    log.debug("ElementsFetchAndTranslate: Calculating output files related to included objects");
                    List<StoredData.InFile> filesToProcess = new ArrayList<StoredData.InFile>();
                    BufferedWriter writer = null;
                    try {
                        File transferListFile = new File(interimTdbDirectory, "fileList.txt");
                        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(transferListFile), "utf-8"));

                        //TODO: make ElementsItemCollections order things better - particularly within the objects type
                        for (ElementsItemType type : ElementsItemType.values()) {
                            for (ElementsItemId includedItem : monitor.getIncludedItems().get(type)) {
                                //TODO: restrict to certain output resource types?
                                for (BasicElementsStoredItem item : rdfStore.retrieveAllItems(includedItem)) {
                                    filesToProcess.add((StoredData.InFile) item.getStoredData());
                                    writer.write(item.getStoredData().getAddress());
                                    writer.newLine();
                                }
                            }
                        }
                    } finally {
                        if (writer != null) writer.close();
                    }
                    log.debug("ElementsFetchAndTranslate: Finished calculating output files related to included objects");
                    //end of changes towards making include monitoring a separate step in the process

                    currentTdbStore.mkdirs();
                    previousTdbStore.mkdirs();

                    //clear the current store ahead of re-loading
                    FileUtils.deleteDirectory(currentTdbStore);
                    //TODO: ensure that comparison store is nulled out too if we are starting with no state....?

                    log.debug(MessageFormat.format("ElementsFetchAndTranslate: Transferring output data to triplestore \"{0}\"", currentTdbStore.getAbsolutePath()));
                    TDBConnect currentJC = new TDBConnect(currentTdbStore);
                    //JenaConnect jc = new TDBJenaConnect(currentStore.getAbsolutePath(), "http://vitro.mannlib.cornell.edu/default/vitro-kb-2");
                    TDBLoadUtility.load(currentJC, filesToProcess.iterator());
                    log.debug("ElementsFetchAndTranslate: Finished transferring data to triplestore");
                }

                log.debug("ElementsFetchAndTranslate: Calculating additions based on comparison to previous run");
                TDBConnect currentJC = new TDBConnect(currentTdbStore);
                TDBConnect previousJC = new TDBConnect(previousTdbStore);
                File additionsFile = new File(interimTdbDirectory, "additions.n3");
                ModelOutput additionsOutput = new ModelOutput.FileOutput(additionsFile);
                DiffUtility.diff(currentJC, previousJC, additionsOutput);
                log.debug("ElementsFetchAndTranslate: Calculating subtractions based on comparison to previous run");

                File subtractionsFile = new File(interimTdbDirectory, "subtractions.n3");
                ModelOutput subtractionsOutput = new ModelOutput.FileOutput(subtractionsFile);
                DiffUtility.diff(previousJC, currentJC, subtractionsOutput);

                //TODO: ? make this action configurable?
                File fragmentStore = new File(interimTdbDirectory, "fragments");
                //FileSplitter splitter = new FileSplitter(fragmentStore);
                FileSplitter splitter = new FileSplitter.NTriplesSplitter(fragmentStore);
                splitter.split(additionsFile, runStartedAt, FileSplitter.Type.Additions);
                splitter.split(subtractionsFile, runStartedAt, FileSplitter.Type.Subtractions);

                //if completed successfully manage state file..
                //TODO: worry about how to handle failures that mean the state file is not updated after the diff phase.
                boolean stateFileManagementErrorDetected = false;
                //manage state file
                BufferedWriter stateWriter = null;
                try {
                    stateWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(stateFile), "utf-8"));
                    stateWriter.write(Integer.toString(runCount));
                    stateWriter.newLine();
                    stateWriter.write(lastRunDateFormat.format(runStartedAt));
                }
                catch(IOException e){
                    stateFileManagementErrorDetected = true;
                }
                finally{
                    try {
                        if(stateWriter != null) stateWriter.close();
                    }catch(IOException e){
                        stateFileManagementErrorDetected = true;
                    }
                }

                if(stateFileManagementErrorDetected){
                    //todo : make it do something about state file errors - what is a good question...
                }

            } catch (IOException e) {
                System.err.println("Caught IOException initialising ElementsFetchAndTranslate");
                e.printStackTrace(System.err);
                caught = e;
            }
        } catch (ConfigParser.UsageException e) {
            caught = e;
            if (!Configuration.isConfigured()) {
                System.out.println(Configuration.getUsage());
            } else {
                System.err.println("Caught UsageException initialising ElementsFetchAndTranslate");
                e.printStackTrace(System.err);
            }
        }
        catch(LoggingUtils.LoggingInitialisationException e) {
            caught = e;
            System.out.println("Logging Error detected");
            caught.printStackTrace(System.out);
        }
        catch(Exception e) {
            log.error("Unhandled Exception occurred during processing - terminating application", e);
            caught = e;
        }
        finally {
            if (caught == null || !(caught instanceof LoggingUtils.LoggingInitialisationException)) {
                log.debug("ElementsFetch: End");
            }
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
            log.debug("Clearing down object cache (Full pull) - this may take some time..");
            objectStore.cleardown(StorableResourceType.RAW_OBJECT);
            ElementsFetch.ObjectConfig objConfig = new ElementsFetch.ObjectConfig(true, null, categories);
            elementsFetcher.execute(objConfig, objectStore);
        }
        else{
            ElementsFetch.ObjectConfig objConfig = new ElementsFetch.ObjectConfig(true, modifiedSince, categories);
            ElementsFetch.ObjectConfig delObjConfig = new ElementsFetch.DeletedObjectConfig(true, modifiedSince, categories);
            elementsFetcher.execute(objConfig, objectStore);
            elementsFetcher.execute(delObjConfig, objectStore);
        }
    }

    private static void processRelationships(ElementsItemFileStore objectStore, ElementsFetch elementsFetcher, Date modifiedSince,
                                             boolean repullAllRelationshipsForModifiedObjects, Set<String> relationshipTypesToReprocess) throws IOException{
        if(modifiedSince == null) {
            log.debug("Clearing down relationship cache (Full pull) - this may take some time..");
            objectStore.cleardown(StorableResourceType.RAW_RELATIONSHIP);
            //fetch relationships.
            ElementsFetch.RelationshipConfig relConfig = new ElementsFetch.RelationshipConfig(null);
            elementsFetcher.execute(relConfig, objectStore);
        }
        else {
            ElementsFetch.RelationshipConfig relConfig = new ElementsFetch.RelationshipConfig(modifiedSince);
            ElementsFetch.RelationshipConfig delRelConfig = new ElementsFetch.DeletedRelationshipConfig(modifiedSince);
            elementsFetcher.execute(relConfig, objectStore);
            elementsFetcher.execute(delRelConfig, objectStore);

            //Work out if we need to do any re-processing
            Set<ElementsItemId> modifiedObjects = objectStore.getAffectedItems(StorableResourceType.RAW_OBJECT);

            //Note these sections below are kept separate to facilitate easy removal of repull All when API supports better behaviour
            //if we are going to repull everything (to avoid issues with visibility not showing up correctly).
            if (repullAllRelationshipsForModifiedObjects) {
                log.debug("ElementsFetchAndTranslate: Processing relationship cache to establish which to repull based on objects modified this run");
                Set<ElementsItemId> relationshipsToRepull = new HashSet<ElementsItemId>();
                //loop over the current state of our raw object cache (which is up to date on this thread) to establish which relationships are related to the recently modified objects
                int counter = 0;
                for (StoredData.InFile relData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_RELATIONSHIP)) {
                    ElementsStoredItem relItem = ElementsStoredItem.InFile.loadRawRelationship(relData.getFile(), relData.isZipped());
                    //check if the object on either side is one that has been modified, if so then flag this relationship as needing re-pulling
                    for (ElementsItemId.ObjectId objectId : relItem.getItemInfo().asRelationshipInfo().getObjectIds()) {
                        if (modifiedObjects.contains(objectId)) {
                            relationshipsToRepull.add(relItem.getItemInfo().getItemId());
                            break;
                        }
                    }
                    counter++;
                    if(counter % 10000 == 0) log.debug(MessageFormat.format("ElementsFetchAndTranslate: {0} relationships processed from cache", counter));
                }
                log.debug(MessageFormat.format("ElementsFetchAndTranslate: finished processing relationships from cache, {0} items processed in total", counter));
                //re-pull data for those relationships batched up sensibly.
                if(!relationshipsToRepull.isEmpty()) {
                    ElementsFetch.RelationshipsListConfig reprocessForModifiedObjectsConfig = new ElementsFetch.RelationshipsListConfig(relationshipsToRepull);
                    elementsFetcher.execute(reprocessForModifiedObjectsConfig, objectStore);
                }
            }
            //if not repulling all relationships for modified objects and if set up to re-process certain relationship types then do that re-processing
            else if (relationshipTypesToReprocess != null && !relationshipTypesToReprocess.isEmpty()) {
                log.debug("ElementsFetchAndTranslate: Processing relationship cache to establish which to re-process based on objects modified this run");
                Set<ElementsItemInfo> relationshipsToReprocess = new HashSet<ElementsItemInfo>();
                int counter = 0;
                for (StoredData.InFile relData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_RELATIONSHIP)) {
                    ElementsStoredItem relItem = ElementsStoredItem.InFile.loadRawRelationship(relData.getFile(), relData.isZipped());
                    //if this relationship is of a type we may need to reprocess
                    if (relationshipTypesToReprocess.contains("all") || relationshipTypesToReprocess.contains(relItem.getItemInfo().asRelationshipInfo().getType())) {
                        //check if the object on either side is one that has been modified, if so then flag the relationship as needing re-processing
                        for (ElementsItemId.ObjectId objectId : relItem.getItemInfo().asRelationshipInfo().getObjectIds()) {
                            if (modifiedObjects.contains(objectId)) {
                                relationshipsToReprocess.add(relItem.getItemInfo());
                                break;
                            }
                        }
                    }
                    counter++;
                    if(counter % 10000 == 0) log.debug(MessageFormat.format("ElementsFetchAndTranslate: {0} relationships processed from cache", counter));
                }
                log.debug(MessageFormat.format("ElementsFetchAndTranslate: finished processing relationships from cache, {0} items processed in total", counter));

                //re-pull data for those relationships batched up sensibly.
                for(ElementsItemInfo relInfo : relationshipsToReprocess){
                    objectStore.touchItem(relInfo, StorableResourceType.RAW_RELATIONSHIP);
                }

                log.debug(MessageFormat.format("ElementsFetchAndTranslate: Enqueued {0} relationships for reprocessing", relationshipsToReprocess.size()));
            }
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
        //TODO : move academicsOnly into a config item.
        boolean academicsOnly = Configuration.getAcademicsOnly();
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
        if(Configuration.getGroupsOfUsersToHarvest().isEmpty()) {
            //we need to copy the users over as we do more filtering below
            for(ElementsItemInfo userInfo : userInfoCache.values()) {
                if(!invalidUsers.contains(userInfo.getItemId()))
                    includedUsers.put(userInfo.getItemId(), userInfo);
            }
        }
        else {
            for (ElementsItemId.GroupId groupId : Configuration.getGroupsOfUsersToHarvest()) {
                ElementsGroupInfo.GroupHierarchyWrapper group = groupCache.get(groupId);
                for(ElementsItemId userId : group.getImplicitUsers()){
                    if(!invalidUsers.contains(userId))
                        includedUsers.put(userId, userInfoCache.get(userId));
                }
            }
        }

        for(ElementsItemId.GroupId groupId : Configuration.getGroupsOfUsersToExclude()){
            ElementsGroupInfo.GroupHierarchyWrapper group = groupCache.get(groupId);
            includedUsers.removeAll(group.getImplicitUsers());
        }

        return includedUsers;
    }

    private static ElementsAPI getElementsAPI() {
        String apiEndpoint = Configuration.getApiEndpoint();
        ElementsAPIVersion apiVersion = Configuration.getApiVersion();

        String apiUsername = Configuration.getApiUsername();
        String apiPassword = Configuration.getApiPassword();

        if (Configuration.getIgnoreSSLErrors()) {
            HttpClient.ignoreSslErrors();
        }

        int soTimeout = Configuration.getApiSoTimeout();
        if (soTimeout > 4999 && soTimeout < (30 * 60 * 1000)) {
            HttpClient.setSocketTimeout(soTimeout);
        }

        int requestDelay = Configuration.getApiRequestDelay();
        if (requestDelay > -1 && requestDelay < (5 * 60 * 1000)) {
            HttpClient.setRequestDelay(requestDelay);
        }

        int fullDetailPerPage = Configuration.getFullDetailPerPage();
        int refDetailPerPage = Configuration.getRefDetailPerPage();
        boolean rewriteMismatchedUrls = Configuration.getRewriteMismatchedUrls();
        ElementsAPI.ProcessingDefaults defaults = new ElementsAPI.ProcessingDefaults(true, fullDetailPerPage, refDetailPerPage);
        return new ElementsAPI(apiVersion, apiEndpoint, apiUsername, apiPassword, rewriteMismatchedUrls, defaults);

    }

    private static File getDirectoryFromPath(String path) {
        File file = null;
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

