/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.app;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import uk.co.symplectic.utils.LoggingUtils;
import uk.co.symplectic.utils.configuration.ConfigParser;
import uk.co.symplectic.utils.triplestore.*;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.utils.http.HttpClient;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.utils.ExecutorServiceUtils;
import uk.co.symplectic.vivoweb.harvester.config.StateManagement;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.fetch.*;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.*;
import uk.co.symplectic.vivoweb.harvester.translate.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;


//import org.vivoweb.harvester.util.args.UsageException;

public class ElementsFetchAndTranslate {
    /**
     * SLF4J Logger
     */
    final private static Logger log = LoggerFactory.getLogger(ElementsFetchAndTranslate.class);

    final private static String additionsFileName = "additions.n3";
    final private static String subtractionsFileName = "subtractions.n3";
    final private static String fragmentsDirName = "fragments";
    final private static String groupCacheFileName = "group-membership-cache.xml";

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

        //todo: move state file location to config?
        StateManagement stateManager = new StateManagement(new File("state.txt"));
        //assume default state (initial run of number 0 with no previous errors) until we have read the state file (or failed to...)
        StateManagement.State state = new StateManagement.State();

        boolean begunProcessing = false;
        try {
            LoggingUtils.initialise("eft_logback.xml");
            log.info(MessageFormat.format("running {0}", "ElementsFetchAndTranslate"));
            Configuration.parse("elementsfetch.properties");
            log.info(Configuration.getConfiguredValues());

            //only one of these will be true, both can be false
            boolean forceFullPull = args.length != 0 && args[0].equals("--full");
            boolean reprocessTranslations = args.length != 0 && args[0].equals("--reprocess");

            boolean skipGroups = args.length != 0 && ArrayUtils.contains(args, "--skipgroups");

            boolean updateLocalTDB = true;

            //load the state from the state file
            state = stateManager.loadState(forceFullPull, reprocessTranslations);

            //test if we have successfully loaded a run count, if not set it to zero and initiate a full;
            StateManagement.StateType currentRunType = state.getCurrentRunType();
            StateManagement.RunClassification currentRunClassification = state.getRunClassification();

            //if we are pulling all data then we want to force the run to be a full reload of the on disk cache.
            Date pullNewDataSinceDate = null;

            switch(currentRunClassification){
                case FORCED_FULL:
                    if(state.getPreviousRunClassification() == StateManagement.PriorRunClassification.FAILED_FULL){
                        log.warn("Performing full pull of data to attempt to correct the raw data cache after a previous full harvest failed.");
                        log.warn("A delta cannot now be run until a full refresh has been completed.");
                    }
                    log.info("Performing forced full pull of data (--full).");
                    break;
                case REPROCESSING:
                    if(state.getPreviousRunClassification() == StateManagement.PriorRunClassification.FAILED_REPROCESS){
                        log.warn("Performing reprocess of cached data to attempt to correct the translated data cache after a previous reprocessing run failed.");
                        log.warn("A delta cannot now be run until either a reprocess or a full refresh has been completed.");
                    }
                    log.info("Reprocessing all data in cache, to update existing translated data cache based on current mappings");
                    break;
                case INITIAL:
                    log.info("Performing initial full pull of data.");
                    break;
                case DELTA:
                    pullNewDataSinceDate = state.getLastRunDate();
                    log.info(MessageFormat.format("Performing differential pull (processing changes since {0}).", state.getLastRunDateAsString()));
                    break;
                default:
                    throw new IllegalStateException("Invalid RunClassification");
            }

            if(skipGroups){
                if(currentRunClassification == StateManagement.RunClassification.DELTA) {
                    log.info("Groups and group membership will not be updated this run (--skipgroups).");
                } else {
                    log.warn("Skip groups request (--skipgroups) ignored as this is not a delta update run.");
                    skipGroups = false;
                }
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

            //TODO: should we ensure that other configured directories are valid (either already exist or can be created at this point?
            File interimTdbDirectory = Configuration.getTdbOutputDir();
            interimTdbDirectory.mkdirs();
            File currentTdbStore = new File(interimTdbDirectory, currentRunType == StateManagement.StateType.EVEN ? "0" : "1");
            File previousTdbStore = new File(interimTdbDirectory, currentRunType == StateManagement.StateType.ODD ? "0" : "1");

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
                File processedImageDir = ElementsFetchAndTranslate.getDirectoryFromPath(Configuration.getVivoImageDir());
                //String vivoBaseURI = Configuration.getBaseURI();

                //Hook item observers to the object store so that translations happen for any objects and relationships that arrive in that store
                //translations are configured to output to the rdfStore;
                objectStore.addItemObserver(new ElementsObjectTranslateObserver(rdfStore, xslFilename));
                objectStore.addItemObserver(new ElementsRelationshipTranslateObserver(objectStore, rdfStore, xslFilename, relationshipTypesNeedingObjectsForTranslation));

                //TODO: work out how to marshall user photos into a web accessible area in a sensible manner based on included user set...
                //Hook a photo retrieval observer onto the rdf store so that photos will be fetched and dropped in the object store for any translated users.
                if(currentRunClassification != StateManagement.RunClassification.REPROCESSING) {
                    objectStore.addItemObserver(new ElementsUserPhotoRetrievalObserver(elementsAPI, Configuration.getImageType(), objectStore));
                }
                else{
                    objectStore.addItemObserver(new ElementsUserPhotoRetrievalObserver.ReprocessingObserver(elementsAPI, Configuration.getImageType(), objectStore));
                }
                //Hook a photo RDF generating observer onto the object store so that any fetched photos have corresponding "rdf" created in the translated output.
                objectStore.addItemObserver(new ElementsUserPhotoRdfGeneratingObserver(rdfStore, xslFilename, processedImageDir, Configuration.getVivoImageBasePath()));


                //we are about to start doing things that affect out caches..
                begunProcessing = true;

                //Now we have wired up all our store observers perform the main fetches
                //NOTE: order is absolutely vital (relationship translation scripts can rely on raw object data having been fetched)
                if(currentRunClassification != StateManagement.RunClassification.REPROCESSING){
                    //start by creating a cache of "relationship types" that we are interested in..
                    ElementsItemKeyedCollection.ItemRestrictor restrictToRelationshipTypes = new ElementsItemKeyedCollection.RestrictToSubTypes(ElementsItemType.AllRelationshipTypes);
                    ElementsItemKeyedCollection.ItemInfo relationshipTypeCache = new ElementsItemKeyedCollection.ItemInfo(restrictToRelationshipTypes);
                    elementsFetcher.execute(new ElementsFetch.RelationshipTypesConfig(), relationshipTypeCache.getStoreWrapper());
                    Set<ElementsItemId> relationshipTypesToInclude = new HashSet<ElementsItemId>();
                    for(ElementsItemInfo itemInfo : relationshipTypeCache.values()){
                        if(itemInfo.isRelationshipTypeInfo()){
                            ElementsRelationshipTypeInfo relTypeInfo = itemInfo.asRelationshipTypeInfo();
                            if(relTypeInfo.isComplete()){
                                ElementsObjectCategory fromCat = relTypeInfo.getFromCategory();
                                if(fromCat == ElementsObjectCategory.USER ||  Configuration.getCategoriesToHarvest().contains(fromCat)){
                                    ElementsObjectCategory toCat = relTypeInfo.getToCategory();
                                    if(toCat == ElementsObjectCategory.USER ||  Configuration.getCategoriesToHarvest().contains(toCat)){
                                        relationshipTypesToInclude.add(relTypeInfo.getItemId());
                                    }
                                }
                            }
                        }
                    }

                    processObjects(objectStore, elementsFetcher, pullNewDataSinceDate);
                    //fetch relationships.
                    //TODO: alter this processRelationships call to just do re-processing of the the relevant links when re-pulling is not necessary.. (do based on API version?)
                    //processRelationships(objectStore, elementsFetcher, aDate, true, relationshipTypesNeedingObjectsForTranslation);
                    processRelationships(objectStore, elementsFetcher, pullNewDataSinceDate, relationshipTypesToInclude, true,  relationshipTypesNeedingObjectsForTranslation);
                }
                else{
                    reprocessCachedItems(objectStore, StorableResourceType.RAW_OBJECT);
                    reprocessCachedItems(objectStore, StorableResourceType.RAW_RELATIONSHIP);
                }

                //load the user cache from the now up to date full cache of user definitions on disk ..(they MUST be present)..
                ElementsItemKeyedCollection.ItemRestrictor restrictToUsers = new ElementsItemKeyedCollection.RestrictToSubTypes(ElementsObjectCategory.USER);
                ElementsItemKeyedCollection.ItemInfo userInfoCache = new ElementsItemKeyedCollection.ItemInfo(restrictToUsers);
                for (StoredData.InFile userData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_OBJECT, ElementsObjectCategory.USER)) {
                    ElementsStoredItemInfo userItem = ElementsStoredItemInfo.loadStoredResource(userData, StorableResourceType.RAW_OBJECT);
                    userInfoCache.put(userItem.getItemInfo().getItemId(), userItem.getItemInfo());
                }

                //Now query the groups, post processing to build a group hierarchy containing users.
                ElementsGroupCollection groupCache;
                if(currentRunClassification == StateManagement.RunClassification.REPROCESSING || (skipGroups && currentRunClassification == StateManagement.RunClassification.DELTA)) {
                    groupCache = createGroupCache(objectStore, userInfoCache.keySet());
                }
                else{
                    groupCache = new ElementsGroupCollection();
                    elementsFetcher.execute(new ElementsFetch.GroupConfig(), groupCache.getStoreWrapper());
                    groupCache.constructHierarchy();
                    groupCache.populateUserMembership(elementsFetcher, userInfoCache.keySet());
                    createGroupMembershipDocument(groupCache);
                }

                //work out the included users
                ElementsItemKeyedCollection.ItemInfo includedUsers = CalculateIncludedUsers(userInfoCache, groupCache);
                //work out the included groups too..
                ElementsItemKeyedCollection.ItemInfo includedGroups = CalculateIncludedGroups(groupCache);

                //Wire up the group translation observer...(needs group cache to work out members Ids and included users to get the user info of those members)
                objectStore.addItemObserver(new ElementsGroupTranslateObserver(rdfStore, xslFilename, groupCache, includedUsers));

                if(currentRunClassification == StateManagement.RunClassification.REPROCESSING || (skipGroups && currentRunClassification == StateManagement.RunClassification.DELTA)) {
                    reprocessCachedItems(objectStore, StorableResourceType.RAW_GROUP);
                }
                else{
                    //fetch the groups and translate them
                    log.debug("Clearing down old group cache");
                    objectStore.cleardown(StorableResourceType.RAW_GROUP);
                    elementsFetcher.execute(new ElementsFetch.GroupConfig(), objectStore);
                }

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
                ElementsVivoIncludeMonitor monitor = new ElementsVivoIncludeMonitor(includedUsers.keySet(), includedGroups.keySet(), Configuration.getCategoriesToHarvest(), visibleLinksOnly);

                int counter = 0;
                for (StoredData.InFile relData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_RELATIONSHIP)) {
                    ElementsStoredItemInfo relItem = ElementsStoredItemInfo.loadStoredResource(relData, StorableResourceType.RAW_RELATIONSHIP);
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
                            for (BasicElementsStoredItem item : rdfStore.retrieveAllRelatedResources(includedItem)) {
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
            File additionsFile = new File(interimTdbDirectory, additionsFileName);
            ModelOutput additionsOutput = new ModelOutput.FileOutput(additionsFile);
            DiffUtility.diff(currentJC, previousJC, additionsOutput);
            log.debug("ElementsFetchAndTranslate: Calculating subtractions based on comparison to previous run");

            File subtractionsFile = new File(interimTdbDirectory, subtractionsFileName);
            ModelOutput subtractionsOutput = new ModelOutput.FileOutput(subtractionsFile);
            DiffUtility.diff(previousJC, currentJC, subtractionsOutput);

            //TODO: ? make this action configurable?
            File fragmentStore = new File(interimTdbDirectory, fragmentsDirName);
            FileSplitter splitter = new FileSplitter.NTriplesSplitter(fragmentStore, Configuration.getMaxFragmentFileSize());
            splitter.split(additionsFile, state.getCurrentRunStartedAt(), FileSplitter.Type.Additions);
            splitter.split(subtractionsFile, state.getCurrentRunStartedAt(), FileSplitter.Type.Subtractions);

            //if completed successfully manage state file..
            stateManager.manageStateForCompleteRun(state);

        }
        catch (ConfigParser.UsageException e) {
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
            caught = e;
            log.error("Unhandled Exception occurred during processing - terminating application", e);
        }
        finally {
            //unless our logging is not working then log that we are exiting.
            if (caught == null || !(caught instanceof LoggingUtils.LoggingInitialisationException)) {
                log.debug("ElementsFetch: End");
            }

            if (caught != null) {
                //if we are specifically in the middle of a something that goes wrong, we may need to tag our state file to allow for error correction
                //if an initial fails there is no state, and thats fine for next attempt
                //if a delta fails it is safe to leave the file alone (will repull all data from time in current state)
                //if an error correcting full fails then next time we want to try again to error correct - which we will do with the current state
                if(begunProcessing){
                    stateManager.manageStateForIncompleteRun(state);
                }
                //exit with an error code.
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
        }

        ElementsFetch.ObjectConfig objConfig = new ElementsFetch.ObjectConfig(true, modifiedSince, categories);
        elementsFetcher.execute(objConfig, objectStore);
    }


    private static void reprocessCachedItems(ElementsItemFileStore objectStore, StorableResourceType type){
        String typeNameForLog = type.getKeyItemType().getName();
        String pluralTypeNameForLog = type.getKeyItemType().getPluralName();
        log.info(MessageFormat.format("Reprocessing Elements {0} from cache", pluralTypeNameForLog));
        int counter = 0;
        for (StoredData.InFile data : objectStore.getAllExistingFilesOfType(type)) {
            ElementsStoredItemInfo item = ElementsStoredItemInfo.loadStoredResource(data, type);
            try {
                objectStore.touchItem(item.getItemInfo(), type);
                counter++;
                if(counter % 1000 == 0) log.info(MessageFormat.format("{0} {1} enqueued for re-processing", counter, pluralTypeNameForLog));
            }
            catch(IOException e){
                log.warn(MessageFormat.format("Error re-processing cached {0} {1}", typeNameForLog, item.getItemInfo().getItemId()));
            }
        }
        log.info(MessageFormat.format("Reprocessing complete, {0} {1} enqueued for re-processing in total", counter, pluralTypeNameForLog));
    }

    private static void processRelationships(ElementsItemFileStore objectStore, ElementsFetch elementsFetcher, Date modifiedSince, Set<ElementsItemId> relationshipTypesToInclude,
                                             boolean repullAllRelationshipsForModifiedObjects, Set<String> relationshipTypesToReprocess) throws IOException{
        if(modifiedSince == null) {
            log.debug("Clearing down relationship cache (Full pull) - this may take some time..");
            objectStore.cleardown(StorableResourceType.RAW_RELATIONSHIP);
        }

        //bring relationship cache up to date
        ElementsFetch.RelationshipConfig relConfig = new ElementsFetch.RelationshipConfig(modifiedSince, relationshipTypesToInclude);
        elementsFetcher.execute(relConfig, objectStore);

        //handle issues with simple update of relationships not being enough
        if(modifiedSince != null){
            //Work out if we need to do any re-processing
            Set<ElementsItemId> modifiedObjects = objectStore.getAffectedItems(StorableResourceType.RAW_OBJECT);

            if(modifiedObjects.size() > 0) {
                //Note these sections below are kept separate to facilitate easy removal of repull All when API supports better behaviour
                //if we are going to repull everything (to avoid issues with visibility not showing up correctly).
                if (repullAllRelationshipsForModifiedObjects) {
                    log.debug(MessageFormat.format("ElementsFetchAndTranslate: Processing relationship cache to establish which to re-pull based on the {0} objects modified this run", modifiedObjects.size()));
                    Set<ElementsItemId> relationshipsToRepull = new HashSet<ElementsItemId>();
                    //loop over the current state of our raw object cache (which is up to date on this thread) to establish which relationships are related to the recently modified objects
                    int counter = 0;
                    for (StoredData.InFile relData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_RELATIONSHIP)) {
                        ElementsStoredItemInfo relItem = ElementsStoredItemInfo.loadStoredResource(relData, StorableResourceType.RAW_RELATIONSHIP);
                        //check if the object on either side is one that has been modified, if so then flag this relationship as needing re-pulling
                        //TODO: could not do this for user objects? - or is that unsafe?
                        for (ElementsItemId.ObjectId objectId : relItem.getItemInfo().asRelationshipInfo().getObjectIds()) {
                            if (modifiedObjects.contains(objectId)) {
                                //if we are dealing with a relationship type that we want to always reprocess then we want to redo it whenever either item has been altered
                                if (relationshipTypesToReprocess.contains("all") || relationshipTypesToReprocess.contains(relItem.getItemInfo().asRelationshipInfo().getType())) {
                                    relationshipsToRepull.add(relItem.getItemInfo().getItemId());
                                    break;
                                }
                                //otherwise we only want to reprocess rels linked to modified non users as a hack to ensure we pick up any visibility changes...
                                else if (objectId.getItemSubType() != ElementsObjectCategory.USER) {
                                    relationshipsToRepull.add(relItem.getItemInfo().getItemId());
                                    break;
                                }
                            }
                        }
                        counter++;
                        if (counter % 10000 == 0)
                            log.debug(MessageFormat.format("ElementsFetchAndTranslate: {0} relationships processed from cache", counter));
                    }

                    log.debug(MessageFormat.format("ElementsFetchAndTranslate: finished processing relationships from cache, {0} items processed in total", counter));
                    //re-pull data for those relationships batched up sensibly.
                    if (!relationshipsToRepull.isEmpty()) {
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
                        ElementsStoredItemInfo relItem = ElementsStoredItemInfo.loadStoredResource(relData, StorableResourceType.RAW_RELATIONSHIP);
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
                        if (counter % 10000 == 0)
                            log.debug(MessageFormat.format("ElementsFetchAndTranslate: {0} relationships processed from cache", counter));
                    }
                    log.debug(MessageFormat.format("ElementsFetchAndTranslate: finished processing relationships from cache, {0} items processed in total", counter));

                    //re-pull data for those relationships batched up sensibly.
                    for (ElementsItemInfo relInfo : relationshipsToReprocess) {
                        objectStore.touchItem(relInfo, StorableResourceType.RAW_RELATIONSHIP);
                    }

                    log.debug(MessageFormat.format("ElementsFetchAndTranslate: Enqueued {0} relationships for reprocessing", relationshipsToReprocess.size()));
                }
            }
            else {
                log.debug("ElementsFetchAndTranslate: No objects modified this run so no need to repull/reprocess any relationships");
            }
        }
    }

    private static ElementsGroupCollection createGroupCache(ElementsItemFileStore objectStore, Set<ElementsItemId> systemUsers){
        try {
            log.info("Recreating Groups information from cache");
            StorableResourceType type = StorableResourceType.RAW_GROUP;
            ElementsGroupCollection groupCache = new ElementsGroupCollection();

            int counter = 0;
            for (StoredData.InFile data : objectStore.getAllExistingFilesOfType(type)) {
                ElementsStoredItemInfo item = ElementsStoredItemInfo.loadStoredResource(data, StorableResourceType.RAW_GROUP);
                ElementsGroupInfo groupInfo = item.getItemInfo().asGroupInfo();
                groupCache.put(item.getItemInfo().getItemId(), new ElementsGroupInfo.GroupHierarchyWrapper(groupInfo));
                counter++;
                if(counter % 1000 == 0) log.info(MessageFormat.format("{0} groups added to cache", counter));
            }

            groupCache.constructHierarchy();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(Configuration.getOtherOutputDir(), groupCacheFileName));

            Map<ElementsItemId, Set<ElementsItemId>> groupUserMap = new HashMap<ElementsItemId, Set<ElementsItemId>>();
            Element root = doc.getDocumentElement();
            NodeList groupNodes = root.getElementsByTagName("group");
            for(int i = 0; i < groupNodes.getLength(); i++){
                Element groupNode = (Element) groupNodes.item(i);
                NodeList userNodes = groupNode.getElementsByTagName("user");
                if(userNodes.getLength() > 0){
                    ElementsItemId.GroupId groupId = ElementsItemId.createGroupId(Integer.parseInt(groupNode.getAttribute("id")));
                    Set userSet = new HashSet<ElementsItemId>();
                    for(int j = 0; j < userNodes.getLength(); j++) {
                        Element userNode = (Element) userNodes.item(j);
                        ElementsItemId userID = ElementsItemId.createObjectId(ElementsObjectCategory.USER, Integer.parseInt(userNode.getTextContent()));
                        userSet.add(userID);
                    }
                    groupUserMap.put(groupId, userSet);
                }
            }
            groupCache.populateUserMembership(groupUserMap, systemUsers);

            return groupCache;
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    private static void createGroupMembershipDocument(ElementsGroupCollection groupCache){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            //Element rootElement = doc.createElementNS("blahblahblah", "entry");
            Element rootElement = doc.createElement("groups");
            doc.appendChild(rootElement);
            for(ElementsGroupInfo.GroupHierarchyWrapper group : groupCache.values()){
                Element groupElement = doc.createElement("group");
                groupElement.setAttribute("id", Integer.toString(group.getGroupInfo().getItemId().getId()));
                for(ElementsItemId user : group.getExplicitUsers()) {
                    Element userElement = doc.createElement("user");
                    userElement.setTextContent(Integer.toString(user.getId()));
                    groupElement.appendChild(userElement);
                }
                rootElement.appendChild(groupElement);
            }

            try {
                TransformerFactory tFactory = TransformerFactory.newInstance();
                Transformer transformer = tFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(Configuration.getOtherOutputDir(), groupCacheFileName));
                transformer.transform(source, result);
            }
            catch(Exception e){
                throw new IllegalStateException(e);
            }
        }
        catch (ParserConfigurationException pce) {
            throw new IllegalStateException(pce);
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
                if(currentGroup != null) {
                    ElementsGroupInfo info = currentGroup.getGroupInfo();
                    includedGroups.put(info.getItemId(), info);
                    for (ElementsGroupInfo.GroupHierarchyWrapper childGroupWrapper : currentGroup.getAllChildren()) {
                        ElementsGroupInfo childInfo = childGroupWrapper.getGroupInfo();
                        includedGroups.put(childInfo.getItemId(), childInfo);
                    }
                }
                else {
                    log.warn(MessageFormat.format("Configured group to include ({0}) does not exist in targeted Elements system.", groupId));
                }
            }
        }

        for(ElementsItemId.GroupId groupId : Configuration.getGroupsToExclude()){
            ElementsGroupInfo.GroupHierarchyWrapper currentGroup = groupCache.get(groupId);
            if(currentGroup != null) {
                ElementsGroupInfo info = currentGroup.getGroupInfo();
                includedGroups.remove(info.getItemId());
                for (ElementsGroupInfo.GroupHierarchyWrapper childGroupWrapper : currentGroup.getAllChildren()) {
                    ElementsGroupInfo childInfo = childGroupWrapper.getGroupInfo();
                    includedGroups.remove(childInfo.getItemId());
                }
            }
            else {
                log.warn(MessageFormat.format("Configured group to exclude ({0}) does not exist in targeted Elements system.", groupId));
            }
        }

        return includedGroups;
    }

    private static ElementsItemKeyedCollection.ItemInfo CalculateIncludedUsers(ElementsItemKeyedCollection.ItemInfo userInfoCache, ElementsGroupCollection groupCache){
        //TODO : move academicsOnly into a config item.
        boolean academicsOnly = Configuration.getAcademicsOnly();
        boolean currentStaffOnly = Configuration.getCurrentStaffOnly();

        //find the users who are definitely not going to be included based on their raw metadata
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
                if(group != null) {
                    for (ElementsItemId userId : group.getImplicitUsers()) {
                        if (!invalidUsers.contains(userId))
                            includedUsers.put(userId, userInfoCache.get(userId));
                    }
                }
                else {
                    log.warn(MessageFormat.format("Configured group of users to include ({0}) does not exist in targeted Elements system.", groupId));
                }
            }
        }

        for(ElementsItemId.GroupId groupId : Configuration.getGroupsOfUsersToExclude()){
            ElementsGroupInfo.GroupHierarchyWrapper group = groupCache.get(groupId);
            if(group != null) {
                includedUsers.removeAll(group.getImplicitUsers());
            }
            else{
                log.warn(MessageFormat.format("Configured user group of users to exclude ({0}) does not exist in targeted Elements system.", groupId));
            }
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

