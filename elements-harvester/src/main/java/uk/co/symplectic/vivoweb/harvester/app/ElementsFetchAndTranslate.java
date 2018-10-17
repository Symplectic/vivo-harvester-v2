/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
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
import uk.co.symplectic.vivoweb.harvester.config.EligibilityFilter;
import uk.co.symplectic.vivoweb.harvester.config.StateManagement;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.fetch.*;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.*;
import uk.co.symplectic.vivoweb.harvester.translate.*;
import uk.co.symplectic.vivoweb.harvester.utils.ElementsGroupCollection;
import uk.co.symplectic.vivoweb.harvester.utils.ElementsItemKeyedCollection;
import uk.co.symplectic.vivoweb.harvester.utils.IncludedGroups;

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
     * The Purpose of process is to update a local cache (ElementsRawDataStore) of Elements data to the current time.
     * The data in this cache is translated (to the Vivo ontology) using the configured XSLT scripts and stored in a secondary cache (ElementsRDFStore)
     * The process then calculates what data is to be sent to vivo given the current configuration (ElementsVivoIncludeMonitor)
     * and then populates a TDB triple store with that data (TDBLoadUtility).
     * This temporary triple store is  compared to the equivalent output from the previous run and difference files created (DiffUtility)
     * These additions and subtractions files are then split into fragments (NTriplesSplitter)
     * These fragments can then be loaded into a live Vivo using a FragmentLoader monitor process.
     *
     * @param args commandline arguments
     */
    public static void main(String[] args) {
        Throwable caught = null;

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

            boolean ignoreChangeProtectionThisRun = args.length != 0 && ArrayUtils.contains(args, "--disableChangeProtection");

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

            if(!Configuration.getChangeProtectionEnabled() && currentRunClassification != StateManagement.RunClassification.INITIAL)
                log.warn("CHANGE PROTECTION DISABLED : Changes will be passed to Vivo in fragments, regardless of how much data has changed since the last harvest run.");
            else
                log.info(MessageFormat.format("Change Protection Enabled : Users - Max {0}% difference; Non Users - Max {1}% difference.",
                        Configuration.getAllowedUserChangeFraction()*100, Configuration.getAllowedNonUserChangeFraction()*100));
            //hacks for testing
            //from next line read the date time
            //String aString = "2016-11-23T17:41:39+0000";
            //for melbourne testing
            //aString = "2016-10-10T17:41:39+0000"; //old date - 50000 pubs 200000 affected rels
            //aString = "2016-10-13T16:01:39+0000"; //much smaller diff date - 7500 off pubs
            //Date aDate = lastRunDateFormat.parse(aString);

            //runType = StateType.ODD;
            //end of hacks for testing.

            EligibilityFilter eligibilityFilter = Configuration.getElligibilityFilter();
            //Set up the Elements API and check that the configured eligibility settings make sense for that API version.
            ElementsAPI elementsAPI = null;
            if(currentRunClassification != StateManagement.RunClassification.REPROCESSING) {
                elementsAPI = ElementsFetchAndTranslate.getElementsAPI();
                if (elementsAPI.getVersion().lessThan(ElementsAPIVersion.VERSION_5_5) && eligibilityFilter.filtersOutNonPublicStaff()) {
                    log.error(
                            MessageFormat.format("An Elements API running the (v{0}) Endpoint spec is incompatible with filtering out public staff (publicStaffOnly = true) as a v4.9 spec API does not provide this information.", elementsAPI.getVersion())
                    );
                    log.error("You should either upgrade to a v5.5 (or later) spec API endpoint or specifically set \"publicStaffOnly = false\" in the config file.");
                    throw new IllegalStateException("Invalid configuration for API Version - see previous error messages.");
                }
            }

            //TODO: should we ensure that other configured directories are valid (either already exist or can be created at this point?
            File interimTdbDirectory = Configuration.getTdbOutputDir();
            interimTdbDirectory.mkdirs();
            File currentTdbStore = new File(interimTdbDirectory, currentRunType == StateManagement.StateType.EVEN ? "0" : "1");
            File previousTdbStore = new File(interimTdbDirectory, currentRunType == StateManagement.StateType.ODD ? "0" : "1");

            log.info("ElementsFetchAndTranslate: Start");

            int includedUserCount = 0;
            int includedObjectCount = 0;

            if(updateLocalTDB) {
                //Set up the services that will be used to do asynchronous work
                //TODO: move these elsewhere, or remove entirely?
                setExecutorServiceMaxThreadsForPool("TranslationService", Configuration.getMaxThreadsXsl());
                setExecutorServiceMaxThreadsForPool("ResourceFetchService", Configuration.getMaxThreadsResource());

                Set<String> relationshipTypesNeedingObjectsForTranslation = Configuration.getRelTypesToReprocess();

                //Set up a fetcher that uses the Elements API.
                ElementsFetch elementsFetcher = currentRunClassification != StateManagement.RunClassification.REPROCESSING ? new ElementsFetch(elementsAPI) : null;

                //Configure extraction of extra data that can be used to establish if users should be included.
                if(eligibilityFilter instanceof EligibilityFilter.LabelSchemeFilter){
                    ElementsObjectInfo.Extractor.InitialiseLabelSchemeExtraction(((EligibilityFilter.InclusionExclusionFilter) eligibilityFilter).getName());
                }
                else if(eligibilityFilter instanceof EligibilityFilter.GenericFieldFilter){
                    ElementsObjectInfo.Extractor.InitialiseGenericFieldExtraction(((EligibilityFilter.InclusionExclusionFilter) eligibilityFilter).getName());
                }

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
                ElementsUserPhotoRetrievalObserver photoRetrievalObserver = null;
                if(currentRunClassification != StateManagement.RunClassification.REPROCESSING) {
                    photoRetrievalObserver = new ElementsUserPhotoRetrievalObserver.FetchingObserver(elementsAPI, Configuration.getImageType(), objectStore);
                }
                else{
                    photoRetrievalObserver = new ElementsUserPhotoRetrievalObserver.ReprocessingObserver(Configuration.getImageType(), objectStore);
                }
                objectStore.addItemObserver(photoRetrievalObserver);
                //Hook a photo RDF generating observer onto the object store so that any fetched photos have corresponding "rdf" created in the translated output.
                objectStore.addItemObserver(new ElementsUserPhotoRdfGeneratingObserver(rdfStore, xslFilename, processedImageDir, Configuration.getVivoImageBasePath()));


                //we are about to start doing things that affect out caches..


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
                    //building the in memory relationship cache does not affect our disk caches so...
                    begunProcessing = true;

                    processObjects(objectStore, elementsFetcher, pullNewDataSinceDate);
                    //fetch relationships.
                    boolean repullRelsForVis = Configuration.getShouldRepullRelsToCorrectVisibility();
                    processRelationships(objectStore, elementsFetcher, pullNewDataSinceDate, relationshipTypesToInclude, repullRelsForVis, relationshipTypesNeedingObjectsForTranslation);
                }
                else{
                    begunProcessing = true; //not sure we need to flag this..
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
                IncludedGroups includedGroups = CalculateIncludedGroups(groupCache, includedUsers);

                //set up some nicely uniqueified names for the groups we are about to send out - to make URI construction easier in the crosswalks.
                groupCache.createCanonicalNames(includedGroups.getIncludedGroups().keySet());

                //Wire up the group translation observer...(needs group cache to work out members Ids and included users to get the user info of those members)
                objectStore.addItemObserver(new ElementsGroupTranslateObserver(rdfStore, xslFilename, groupCache, includedGroups));

                if(currentRunClassification == StateManagement.RunClassification.REPROCESSING || (skipGroups && currentRunClassification == StateManagement.RunClassification.DELTA)) {
                    reprocessCachedItems(objectStore, StorableResourceType.RAW_GROUP);
                }
                else{
                    //fetch the groups and translate them
                    log.info("Clearing down old group cache");
                    objectStore.cleardown(StorableResourceType.RAW_GROUP);
                    elementsFetcher.execute(new ElementsFetch.GroupConfig(), objectStore);
                }

                log.info("ElementsFetchAndTranslate: Translating Group Memberships");
                //we need to do this regardless of run type
                //reprocessing - the mapping files may have changed.
                //skipgroups delta - there may be users in the top level group that weren't present before (could optimise).
                //delta - group memberships may have changed.
                //full - group memberships may have changed.
                //TODO : method to decide if we need to do group membership this way?

                //Note all "enqueings" should have been done on this thread - only the translations and retrievals of photos should be off main thread
                //so we can happily remove observers like this..
                objectStore.removeItemObserver(photoRetrievalObserver);

                ElementsGroupMembershipTranslateObserver groupMembershipTranslateObserver =
                        new ElementsGroupMembershipTranslateObserver(rdfStore, xslFilename, groupCache, includedGroups);
                //cleardown the group memberships
                rdfStore.cleardown(StorableResourceType.TRANSLATED_USER_GROUP_MEMBERSHIP);
                //and recalc them for the included users.
                int counter = 0;
                for(ElementsItemInfo info :includedUsers.values()){
                    //should run the calc even if there are no included groups as it could be doing the memebership parts based on positions, etc...
                    objectStore.touchItem(info, StorableResourceType.RAW_OBJECT, groupMembershipTranslateObserver);
                    counter++;
                    if(counter % 1000 == 0) log.info(MessageFormat.format("{0} user's group-membership processed", counter));
                }

                //Initiate the shutdown of the asynchronous translation engine - note this will actually block until
                //the engine has completed all its enqueued tasks - think of it as "await completion".
                log.info("Waiting for enqueued translations to complete");
                TranslationService.awaitShutdown();

                //changes towards making include monitoring a separate step in the process?

                //Hook a monitor up to work out which objects and relationships we want to send to vivo
                // when building a triple store to represent the current state after this connector run.
                //write out a list of translated rdf files that ought to be included
                log.info("ElementsFetchAndTranslate: Processing cached relationships to establish which items to include in final output");
                //TODO: test performance against spinning rust...

                boolean visibleLinksOnly = Configuration.getVisibleLinksOnly();
                ElementsVivoIncludeMonitor monitor = new ElementsVivoIncludeMonitor(includedUsers.keySet(), includedGroups.getIncludedGroups().keySet(), Configuration.getCategoriesToHarvest(), rdfStore, visibleLinksOnly);

                counter = 0;
                BufferedWriter relWriter = null;
                BufferedWriter incRelWriter = null;
                try {
                    File relationshipListFile = new File(interimTdbDirectory, "relationships.txt");
                    relWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(relationshipListFile), "utf-8"));

                    File incRelationshipListFile = new File(interimTdbDirectory, "includedRelationships.txt");
                    incRelWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(incRelationshipListFile), "utf-8"));

                    for (StoredData.InFile relData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_RELATIONSHIP)) {
                        ElementsStoredItemInfo relItem = ElementsStoredItemInfo.loadStoredResource(relData, StorableResourceType.RAW_RELATIONSHIP);
                        monitor.observe(relItem);

                        relWriter.write(relItem.getItemInfo().toString());
                        relWriter.newLine();
                        if(monitor.getIncludedItems().get(ElementsItemType.RELATIONSHIP).contains(relItem.getItemInfo().getItemId())){
                            incRelWriter.write(relItem.getItemInfo().toString());
                            incRelWriter.newLine();
                        }

                        counter++;
                        if (counter % 10000 == 0)
                            log.info(MessageFormat.format("ElementsFetchAndTranslate: {0} relationships processed from cache", counter));
                    }
                    log.info(MessageFormat.format("ElementsFetchAndTranslate: finished processing relationships from cache, {0} items processed in total", counter));
                }
                finally{
                    if (relWriter != null) relWriter.close();
                    if (incRelWriter != null) incRelWriter.close();
                }

                //update object counts
                includedUserCount = includedUsers.values().size();
                includedObjectCount = monitor.getIncludedItems().get(ElementsItemType.OBJECT).size() - includedUserCount;

                int previousUserCount = state.getPreviousUserCount();
                int previousObjectCount = state.getPreviousObjectCount();

                //check if amount of users changed is within allowed limits
                double allowedFraction = Configuration.getAllowedUserChangeFraction();
                double difference = previousUserCount <= 0 ? 0 : ((double) Math.abs(includedUserCount-previousUserCount))/previousUserCount;
                if(Configuration.getChangeProtectionEnabled() && difference > allowedFraction) {
                    if(ignoreChangeProtectionThisRun)
                        log.warn(MessageFormat.format(
                            "ElementsFetchAndTranslate: {0} users being sent to vivo, despite exceeding change protection vs last run ({1}, {2}%) [--disableChangeProtection]",
                            includedUserCount, previousUserCount, allowedFraction*100)
                        );
                    else
                        throw new IllegalStateException(MessageFormat.format("Number of Users being sent to vivo ({0} vs {1}) changed by more than {2}% since last run. Use --disableChangeProtection to force change.", includedUserCount, previousUserCount, allowedFraction*100));
                }
                else
                    log.info(MessageFormat.format("ElementsFetchAndTranslate: {0} users being sent to vivo", includedUserCount));

                //check if amount of non user objects changed is within allowed limits
                allowedFraction = Configuration.getAllowedNonUserChangeFraction();
                difference = previousObjectCount <= 0 ? 0 : ((double) Math.abs(includedObjectCount-previousObjectCount))/previousObjectCount;
                if(Configuration.getChangeProtectionEnabled() && difference > allowedFraction) {
                    if(ignoreChangeProtectionThisRun)
                        log.warn(
                            MessageFormat.format("ElementsFetchAndTranslate: {0} non-user objects being sent to vivo, despite exceeding change protection vs last run ({1}, {2}%) [--disableChangeProtection)]",
                            includedObjectCount, previousObjectCount, allowedFraction*100)
                        );
                    else
                        throw new IllegalStateException(MessageFormat.format("Number of non-user objects being sent to vivo ({0} vs {1}) changed by more than {2}% since last run. Use --disableChangeProtection to force change", includedObjectCount, previousObjectCount, allowedFraction*100));
                }
                else
                    log.info(MessageFormat.format("ElementsFetchAndTranslate: {0} non-user objects being sent to vivo", includedObjectCount));

                log.info("ElementsFetchAndTranslate: Calculating output files related to included objects");
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
                log.info("ElementsFetchAndTranslate: Finished calculating output files related to included objects");
                //end of changes towards making include monitoring a separate step in the process

                currentTdbStore.mkdirs();
                previousTdbStore.mkdirs();

                log.info(MessageFormat.format("ElementsFetchAndTranslate: Clearing down triplestore \"{0}\"", currentTdbStore.getAbsolutePath()));
                //clear the current store ahead of re-loading
                FileUtils.deleteDirectory(currentTdbStore);
                //TODO: ensure that comparison store is nulled out too if we are starting with no state....?

                log.info(MessageFormat.format("ElementsFetchAndTranslate: Transferring output data to triplestore \"{0}\"", currentTdbStore.getAbsolutePath()));
                TDBConnect currentJC = new TDBConnect(currentTdbStore);
                //JenaConnect jc = new TDBJenaConnect(currentStore.getAbsolutePath(), "http://vitro.mannlib.cornell.edu/default/vitro-kb-2");
                TDBLoadUtility.load(currentJC, filesToProcess.iterator());
                log.info("ElementsFetchAndTranslate: Finished transferring data to triplestore");
            }

            log.info("ElementsFetchAndTranslate: Calculating additions based on comparison to previous run");
            TDBConnect currentJC = new TDBConnect(currentTdbStore);
            TDBConnect previousJC = new TDBConnect(previousTdbStore);
            File additionsFile = new File(interimTdbDirectory, additionsFileName);
            ModelOutput additionsOutput = new ModelOutput.FileOutput(additionsFile);
            DiffUtility.diff(currentJC, previousJC, additionsOutput);
            log.info("ElementsFetchAndTranslate: Calculating subtractions based on comparison to previous run");

            File subtractionsFile = new File(interimTdbDirectory, subtractionsFileName);
            ModelOutput subtractionsOutput = new ModelOutput.FileOutput(subtractionsFile);
            DiffUtility.diff(previousJC, currentJC, subtractionsOutput);

            log.info("ElementsFetchAndTranslate: Generating fragments");
            //TODO: ? make this action configurable?
            File fragmentStore = new File(interimTdbDirectory, fragmentsDirName);
            FileSplitter splitter = new FileSplitter.NTriplesSplitter(fragmentStore, Configuration.getMaxFragmentFileSize());
            splitter.split(additionsFile, state.getCurrentRunStartedAt(), FileSplitter.Type.Additions);
            splitter.split(subtractionsFile, state.getCurrentRunStartedAt(), FileSplitter.Type.Subtractions);

            //if completed successfully manage state file..
            stateManager.manageStateForCompleteRun(state, includedUserCount, includedObjectCount);

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
        catch(OutOfMemoryError e){
            caught = e;
            log.error("Out Of Memory you will need to assign more memory to the JVM - terminating application", e);
            log.warn("Cannot continue - Note state may be out of sync with data stores.");
            System.exit(1);
        }
        finally {
            //unless our logging is not working then log that we are exiting.
            if (caught == null || !(caught instanceof LoggingUtils.LoggingInitialisationException)) {
                log.info("ElementsFetch: End");
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

    /**
     * Method to loop through each configured category of objects to be fetched from Elements and bring the local cache
     * of raw data (objectStore) up to date with changes in Elements.
     * @param objectStore the local cache of raw data.
     * @param elementsFetcher an ElementsFetch helper class designed to facilitate fetching data from the Elements API.
     * @param modifiedSince the date time that the cache was last updated, process will run a "delta" if this is
     *                      supplied, and a "full" if it is null.
     * @throws IOException
     */
    private static void processObjects(ElementsItemFileStore objectStore, ElementsFetch elementsFetcher, Date modifiedSince) throws IOException{
        //fetch all configured categories - ensure that users ARE fetched regardless of configuration
        //TODO: decide if not having users in configured categories should result in different behaviour in the monitor
        List<ElementsObjectCategory> categories = new ArrayList<ElementsObjectCategory>();
        categories.addAll(Configuration.getCategoriesToHarvest());
        if(!categories.contains(ElementsObjectCategory.USER)) categories.add(0, ElementsObjectCategory.USER);

        if(modifiedSince == null) {
            log.info("Clearing down object cache (Full pull) - this may take some time..");
            objectStore.cleardown(StorableResourceType.RAW_OBJECT);
        }

        ElementsFetch.ObjectConfig objConfig = new ElementsFetch.ObjectConfig(true, modifiedSince, categories);
        elementsFetcher.execute(objConfig, objectStore);
    }

    /**
     * Method to "reprocess" any items of a particular type in the local cache of raw data (objectStore).
     * This method will "touch" all the items in the raw cache, which will invoke any observers hooked onto the store,
     * it will therefore trigger the items to be re-translated using the currently configured XSLT mapping scripts
     * if the typical translate observers are in use.
     * @param objectStore the local cache of raw data.
     * @param type The type of data to reprocess.
     */
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


    /**
     * Method to loop through all Elements relationships, of the types specified in relationshipTypesToInclude,
     * and bring the local cache of raw data (objectStore) up to date with changes in Elements.
     * If the
     * @param objectStore the local cache of raw data.
     * @param elementsFetcher an ElementsFetch helper class designed to facilitate fetching data from the Elements API.
     * @param modifiedSince the date time that the cache was last updated, process will run a "delta" if this is
     *                      supplied, and a "full" if it is null.
     * @param relationshipTypesToInclude the types of Elements relationship type (int) to be processed.
     * @param repullRelsToCorrectVisibility whether unmodified relationships that involve objects modified since "modifiedSince".
     *                                      should be updated to ensure that visibility is correctly updated in Vivo.
     * @param relationshipTypesToReprocess Any types of relationship (string) that should be reprocessed regardless of
     *                                     visibility concerns if the linked objects are changed
     *                                     e.g. when the translation of objects occurs when the relationship is processed.
     * @throws IOException
     */
    private static void processRelationships(ElementsItemFileStore objectStore, ElementsFetch elementsFetcher, Date modifiedSince, Set<ElementsItemId> relationshipTypesToInclude,
                                             boolean repullRelsToCorrectVisibility, Set<String> relationshipTypesToReprocess) throws IOException{
        if(modifiedSince == null) {
            log.info("Clearing down relationship cache (Full pull) - this may take some time..");
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

                //get the set of relationships we have already updated this run, as these have already been re-pulled and therefore re-processed.
                Set<ElementsItemId> modifiedRelationships = objectStore.getAffectedItems(StorableResourceType.RAW_RELATIONSHIP);
                log.info(MessageFormat.format("ElementsFetchAndTranslate: Processing relationship cache to establish which to re-pull/re-process based on the {0} objects modified this run", modifiedObjects.size()));
                Set<ElementsItemId> relationshipsToRepull = new HashSet<ElementsItemId>();
                Set<ElementsItemInfo> relationshipsToReprocess = new HashSet<ElementsItemInfo>();

                //loop over the current state of our raw object cache (which is up to date on this thread) to establish which relationships are related to the recently modified objects
                int counter = 0;
                for (StoredData.InFile relData : objectStore.getAllExistingFilesOfType(StorableResourceType.RAW_RELATIONSHIP)) {
                    ElementsStoredItemInfo relItem = ElementsStoredItemInfo.loadStoredResource(relData, StorableResourceType.RAW_RELATIONSHIP);

                    //check if we have already processed the relationship this run, so then we can ignore it.
                    if(!modifiedRelationships.contains(relItem.getItemInfo().getItemId())) {
                        //check if the object on either side is one that has been modified, if so then flag this relationship as needing re-pulling
                        //otherwise we only want to reprocess rels linked to modified non users as a hack to ensure we pick up any visibility changes...
                        boolean shouldRepull = false;
                        boolean shouldReprocess = false;
                        for (ElementsItemId.ObjectId objectId : relItem.getItemInfo().asRelationshipInfo().getObjectIds()) {
                            if (modifiedObjects.contains(objectId)) {
                                //if the unmodified relationship contains a modified object that is not a user it may have an altered visibility flag
                                //so we mark it for re-pulling if we are worried about that for this API version.
                                if (repullRelsToCorrectVisibility && objectId.getItemSubType() != ElementsObjectCategory.USER) {
                                    shouldRepull = true;
                                    break;
                                }
                                //if the unmodified relationship contains any modified objects
                                // and is of a type that we need to reprocess (i.e. one where the translation of the objects occurs within the relationship)
                                //then we will need to re-translate the item. we may not need to re-pull it (e.g if that is turned off or if only the user has been changed)
                                if(relationshipTypesToReprocess != null && !relationshipTypesToReprocess.isEmpty()) {
                                    if (relationshipTypesToReprocess.contains("all") || relationshipTypesToReprocess.contains(relItem.getItemInfo().asRelationshipInfo().getType())) {
                                        shouldReprocess = true;
                                        //deliberately don't break the loop over objects in the link, as the second object could be the one that triggers a repull
                                    }
                                }
                            }
                        }

                        //put link into appropriate bin, repull if requested, if not being repulled then reprocess if requested.
                        if(shouldRepull) relationshipsToRepull.add(relItem.getItemInfo().getItemId());
                        else if (shouldReprocess) relationshipsToReprocess.add(relItem.getItemInfo());
                    }
                    counter++;
                    if (counter % 1000 == 0)
                        log.info(MessageFormat.format("ElementsFetchAndTranslate: {0} relationships processed from cache", counter));
                }

                log.info(MessageFormat.format("ElementsFetchAndTranslate: finished processing relationships from cache, {0} items processed in total", counter));


                //re-pull data for those relationships batched up sensibly.
                if (!relationshipsToRepull.isEmpty()) {
                    ElementsFetch.RelationshipsListConfig repullForModifiedObjectsConfig = new ElementsFetch.RelationshipsListConfig(relationshipsToRepull);
                    elementsFetcher.execute(repullForModifiedObjectsConfig, objectStore);
                }

                if(!relationshipsToReprocess.isEmpty()) {
                    counter = 0;
                    //re-process data for relationships that need to be re-processed
                    for (ElementsItemInfo relInfo : relationshipsToReprocess) {
                        objectStore.touchItem(relInfo, StorableResourceType.RAW_RELATIONSHIP);
                        counter++;
                        if(counter % 1000 == 0) log.info(MessageFormat.format("ElementsFetchAndTranslate: Enqueued {0} relationships for re-processing because of modified objects", counter));
                    }
                    log.info(MessageFormat.format("ElementsFetchAndTranslate: Reprocessing complete, {0} relationships enqueued for re-processing in total", counter));
                }
            }
            else {
                log.info("ElementsFetchAndTranslate: No objects modified this run so no need to repull/reprocess any relationships");
            }
        }
    }

    /**
     * Helper method to load the previously persisted cache of of user group membership in the source Elements system.
     * Note that any new users that have been created in the source system since the cache of of user group membership
     * was created will not have the correct group memberships until a non group-skipping update is completed.
     * Any New users will sinply appear as members of the top level "organisation" group if it is transferred to Vivo.
     * @param objectStore the local cache of raw data.
     * @param systemUsers Set of ElementsItemId's representing all the users in the source Elements system.
     * @return ElementsGroupCollection
     */
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

    /**
     * Helper method to write out an XML file cache representing the harvester's current understanding of
     * user group membership from the source Elements system
     * @param groupCache an ElementsGroupCollection representing the current understanding of users and groups.
     * @return ElementsGroupCollection
     */
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


    /**
     * Helper method to work out which Element's user groups are going to be sent to Vivo based on the current configuration.
     * @param groupCache the current
     * @return ElementsItemKeyedCollection.ItemInfo containing Groups to be included in Vivo.
     */
    private static IncludedGroups CalculateIncludedGroups(ElementsGroupCollection groupCache, ElementsItemKeyedCollection.ItemInfo includedUsers) {
        IncludedGroups includedGroups = new IncludedGroups();

        //only assume we include the org group by default if no groups (or child groups) are specified as to be included
        boolean assumeIncludeOrgGroup = !Configuration.getGroupsToHarvestMatcher().isActive() && !Configuration.getGroupsToIncludeChildrenOfMatcher().isActive();
        GroupAction assumedTopLevelAction = assumeIncludeOrgGroup ? GroupAction.INCLUDE : GroupAction.EXCLUDE;
        getIncludedGroups(groupCache.GetTopLevel(), includedGroups, includedUsers, assumedTopLevelAction);

        return includedGroups;
    }

    /**
     * what action should be taken for a particular group in elements with regard to Vivo
     */
    private static enum GroupAction {
        INCLUDE, //group will appear in Vivo with its members wired up to it
        EXCLUDE, //group will not appear in Vivo, its members will be wired up to the nearest parent group being send to Vivo (if one exists)
        EXCISE //group will not appear in Vivo, memberships will not appear in Vivo anywhere.
    }

    /**
     * internal helper to walk the GroupHierarchyWrapper tree and work out which groups to include.
     * @param group current node in the tree being processed.
     * @param includedGroups the set of included groups.
     * @param assumeAction whether we are currently including, excluding or excising discovered nodes.
     */
    private static void getIncludedGroups(ElementsGroupInfo.GroupHierarchyWrapper group, IncludedGroups includedGroups, ElementsItemKeyedCollection.ItemInfo includedUsers, GroupAction assumeAction){

        boolean shouldRecurse = true;

        ElementsGroupInfo groupInfo = group.getGroupInfo();
        //ElementsItemId.GroupId groupId = (ElementsItemId.GroupId) groupInfo.getItemId();

        GroupAction actionToTake = assumeAction;
        if(Configuration.getGroupsToHarvestMatcher().isMatch(groupInfo)){
            actionToTake = GroupAction.INCLUDE;
        }
        else if(Configuration.getGroupsToExcludeMatcher().isMatch(groupInfo)){
            actionToTake = GroupAction.EXCLUDE;
        }
        else if (Configuration.getGroupsToExciseMatcher().isMatch(groupInfo)){
            actionToTake = GroupAction.EXCISE;
        }
        // if no explicit instructions about how to handle the group then decide based on whether we are set up to include "empty" groups
        // empty being defined as having no included users in the set of implicit users of the group.
        else if (!Configuration.getIncludeEmptyGroups()){
            boolean groupContainsIncludedUsers = false;
            if(group.getImplicitUsers().size() < includedUsers.keySet().size()){
                for(ElementsItemId userID : group.getImplicitUsers()){
                    if(includedUsers.keySet().contains(userID)){
                        groupContainsIncludedUsers = true;
                     break;
                    }
                }
            }
            else {
                for(ElementsItemId userID : includedUsers.keySet()){
                    if(group.getImplicitUsers().contains(userID)){
                        groupContainsIncludedUsers = true;
                        break;
                    }
                }
            }

            if(!groupContainsIncludedUsers){
                //No need to worry about excision - we are dealing with empty groups - no memberships to worry about.
                actionToTake = GroupAction.EXCISE;
                shouldRecurse = false;
            }
        }

        switch(actionToTake){
            case INCLUDE:
                includedGroups.getIncludedGroups().put(groupInfo.getItemId(), groupInfo);
                break;
            case EXCLUDE: //do nothing
                break;
            case EXCISE:
                includedGroups.getExcisedGroups().put(groupInfo.getItemId(), groupInfo);
                break;
            default :
                throw new IllegalStateException("Invalid GroupAction type detected");
        }

        //only worth recursing if we are really going to find anything
        // e.g. we won't if we are excluding this group as it is empty of included users - as that means all this
        // groups children must also be empty of included users..
        if(shouldRecurse) {
            //default to assuming children should behave like their parent ..
            GroupAction actionToAssumeForChildGroups = actionToTake;
            if (Configuration.getGroupsToIncludeChildrenOfMatcher().isMatch(groupInfo)) {
                actionToAssumeForChildGroups = GroupAction.INCLUDE;
            } else if (Configuration.getGroupsToExcludeChildrenOfMatcher().isMatch(groupInfo)) {
                actionToAssumeForChildGroups = GroupAction.EXCLUDE;
            } else if (Configuration.getGroupsToExciseChildrenOfMatcher().isMatch(groupInfo)){
                actionToAssumeForChildGroups = GroupAction.EXCISE;
            }

            for (ElementsGroupInfo.GroupHierarchyWrapper child : group.getChildren()) {
                getIncludedGroups(child, includedGroups, includedUsers, actionToAssumeForChildGroups);
            }
        }
    }

    /**
     * Method to calculate which Elements users should be included in the output sent to Vivo
     * This is based on the configured ElligibilityFilters (current, academic, and custom generic fiels/label scheme)
     * And secondartily on the configured usergroups to include and or exclude.
     * @param userInfoCache Cache of data about all Elements users.
     * @param groupCache The cache of information about our current understanding of user group memberships.
     *                   (which might be based on an out of date cache if this is a --skipgroups run)
     * @return
     */
    private static ElementsItemKeyedCollection.ItemInfo CalculateIncludedUsers(ElementsItemKeyedCollection.ItemInfo userInfoCache, ElementsGroupCollection groupCache){

        EligibilityFilter filter = Configuration.getElligibilityFilter();

        //find the users who are definitely not going to be included based on their raw metadata
        List<ElementsItemId> invalidUsers = new ArrayList<ElementsItemId>();
        for(ElementsItemInfo objInfo : userInfoCache.values()){
            ElementsUserInfo userInfo = (ElementsUserInfo) objInfo;
            //if user is not eligible then we don't want them..
            if (!filter.isUserEligible(userInfo)) invalidUsers.add(userInfo.getObjectId());
        }

        //create list of group ids that are definitively being excluded or included
        Set<ElementsItemId.GroupId> includedGroups = new HashSet<ElementsItemId.GroupId>();
        Set<ElementsItemId.GroupId> excludedGroups = new HashSet<ElementsItemId.GroupId>();
        for(ElementsGroupInfo.GroupHierarchyWrapper groupWrapper : groupCache.values()){
            if(Configuration.getGroupsOfUsersToHarvestMatcher().isMatch(groupWrapper.getGroupInfo())) {
                includedGroups.add((ElementsItemId.GroupId) groupWrapper.getGroupInfo().getItemId());
            }
            if(Configuration.getGroupsOfUsersToExcludeMatcher().isMatch(groupWrapper.getGroupInfo())){
                excludedGroups.add((ElementsItemId.GroupId) groupWrapper.getGroupInfo().getItemId());
            }
        }
        //Work out which users we are planning to include (i.e who is in the included set, not in the excluded set

        ElementsItemKeyedCollection.ItemRestrictor restrictToUsersOnly = new ElementsItemKeyedCollection.RestrictToSubTypes(ElementsObjectCategory.USER);
        ElementsItemKeyedCollection.ItemInfo includedUsers = new ElementsItemKeyedCollection.ItemInfo(restrictToUsersOnly);
        if(includedGroups.isEmpty()) {
            //we need to copy the users over as we do more filtering below
            for(ElementsItemInfo userInfo : userInfoCache.values()) {
                if(!invalidUsers.contains(userInfo.getItemId()))
                    includedUsers.put(userInfo.getItemId(), userInfo);
            }
        }
        else {
            for (ElementsItemId.GroupId groupId : includedGroups) {
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

        for(ElementsItemId.GroupId groupId : excludedGroups){
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

    /**
     * Helper method to retrieve a correctly configured ElementsAPI object.
     * @return
     */
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

    /**
     * Helper method to retrieve a File object given a path.
     * @param path
     * @return
     */
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

    /**
     * Helper method to configure the number of threads for any Exevutor services (asynchronous task execution engines).
     * We use these for the TranslationService (that performs XSLt translations)
     * and the FetchService (that fetches extra data from the Elements API, e.g. photos).
     * @param poolName
     * @param maxThreads
     */
    private static void setExecutorServiceMaxThreadsForPool(String poolName, int maxThreads) {
        if (maxThreads > 0) {
            ExecutorServiceUtils.setMaxProcessorsForPool(poolName, maxThreads);
        }
    }
}

