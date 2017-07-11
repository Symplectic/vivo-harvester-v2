/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.config;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class to handle management of the "state.txt" file that represents what state the harvester is currently in.
 */
public class StateManagement {
    public enum StateType{
        ODD,
        EVEN
    }

    public enum RunClassification{
        INITIAL,
        DELTA,
        FORCED_FULL,
        REPROCESSING,
    }

    public enum PriorRunClassification{
        FAILED_FULL,
        FAILED_REPROCESS,
    }

    public static class State{
        //Current Run classification - always assume initial until have loaded state..
        private final int previousRunCount;
        private final Date lastRunDate;
        private final Date runStartedAt;
        private final RunClassification runClassification;
        private final PriorRunClassification previousRunClassification;

        public int getPreviousRunCount(){ return previousRunCount; }
        public int getCurrentRunCount(){ return previousRunCount + 1; }
        public StateType getCurrentRunType(){ return (getCurrentRunCount()%2 == 0) ? StateType.EVEN : StateType.ODD; }
        public Date getLastRunDate(){ return lastRunDate; }
        public String getLastRunDateAsString(){ return lastRunDateFormat.format(lastRunDate); }
        public Date getCurrentRunStartedAt(){ return runStartedAt; }
        public RunClassification getRunClassification(){ return runClassification; }
        public PriorRunClassification getPreviousRunClassification(){ return previousRunClassification; }


        /*
        Default state - initial run with current run number of 0
         */
        public State(){
            this(-1, null, RunClassification.INITIAL, null);
        }

        public State(int previousRunCount, Date lastRunDate, RunClassification runClassification, PriorRunClassification previousRunClassification){
            if(runClassification == null) throw new NullArgumentException("runClassification");
            if(lastRunDate == null && runClassification != RunClassification.INITIAL) throw new IllegalArgumentException("lastRunDate cannot be null except for the initial run");

            //priorRunclassification can legitimately be set to null..

            this.previousRunCount = previousRunCount;
            this.lastRunDate = lastRunDate;
            this.runClassification = runClassification;
            this.previousRunClassification = previousRunClassification;
            this.runStartedAt = new Date();
        }
    }

    /**
     * SLF4J Logger
     */
    final private static Logger log = LoggerFactory.getLogger(StateManagement.class);


    /**
     * Descriptors used in the state file if the last attempted harvest was a full or a reprocess that failed mid processing
     */
    final private String failedFullHarvestDescriptor = "FAILED_FULL";
    final private String failedReprocessDescriptor = "FAILED_REPROCESS";

    /**
     * Date format that is used within state file to keep track of when the last successfully completed run occurred.
     */
    final private static SimpleDateFormat lastRunDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /*
        The file holding the state information for this process
     */
    final private File stateFile;

    public StateManagement(File stateFile){
        if(stateFile == null) throw new NullArgumentException("stateFile");
        this.stateFile = stateFile;
    }

    public State loadState(boolean forceFullPull, boolean reprocessTranslations){
        BufferedReader reader = null;
        if(stateFile.exists()) {

            //placeholders.
            RunClassification runClassification;
            Date lastRunDate = null;
            int previousRunCount = -1;
            PriorRunClassification previousRunClassification = null;

            //if we have a state file and we are not forcing a full, then its a delta.
            if(forceFullPull) {
                runClassification = RunClassification.FORCED_FULL;
            }
            else if(reprocessTranslations) {
                runClassification = RunClassification.REPROCESSING;
            }
            else{
                runClassification = RunClassification.DELTA;
            }

            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(stateFile), "utf-8"));
                String str;
                int counter = 0;
                while ((str = reader.readLine()) != null) {
                    if (counter == 0) {
                        //load number of last completed run
                        previousRunCount = Integer.parseInt(str);
                    } else if (counter == 1) {
                        lastRunDate = lastRunDateFormat.parse(str);
                    } else {
                        if (counter == 2 && str.equals(failedFullHarvestDescriptor)) {
                            previousRunClassification = PriorRunClassification.FAILED_FULL;
                        }
                        else if (counter == 2 && str.equals(failedReprocessDescriptor)) {
                            previousRunClassification = PriorRunClassification.FAILED_REPROCESS;
                        }
                        else {
                            log.warn("state.txt file appears to be corrupt - too many lines detected");
                            throw new IllegalStateException("state.txt is corrupt");
                        }
                    }
                    counter++;
                }

            } catch (IOException e) {
                log.warn("Could not successfully load information from state.txt file.");
                throw new IllegalStateException("state.txt is corrupt", e);
            } catch (NumberFormatException e) {
                log.warn("Could not successfully load run count from state.txt file. ");
                throw new IllegalStateException("state.txt is corrupt", e);
            } catch (ParseException e) {
                log.warn("Could not successfully load last run date from state.txt file. ");
                throw new IllegalStateException("state.txt is corrupt", e);
            } finally {
                try {
                    if (reader != null) reader.close();
                }
                catch(IOException e){
                    throw new IllegalStateException("state.txt could not be closed after loading", e);
                }
            }

            //regardless of what was requested, if last run was a failed full we must error correct the raw cache.
            if(previousRunClassification == PriorRunClassification.FAILED_FULL)
                runClassification = RunClassification.FORCED_FULL;
                //if we are planning to do a full, then its irrelevant that last reprocess failed - we will correct the full translated cache
                //if its a delta then we need to correct the translated cache, so try a reprocess instead.
            else if(runClassification == RunClassification.DELTA && previousRunClassification == PriorRunClassification.FAILED_REPROCESS){
                runClassification = RunClassification.REPROCESSING;
            }
            return new State(previousRunCount, lastRunDate, runClassification, previousRunClassification);
        }
        //if no state file load deault state - corresponds to initial run of number 0
        return new State();
    }

    public boolean manageStateForCompleteRun(State state){
        //we don't update the date in teh file if we are repreocessing as the data in the raw cache has not been altered..
        Date dateToWrite = state.getRunClassification() == RunClassification.REPROCESSING ? state.getLastRunDate() : state.getCurrentRunStartedAt();
        return writeStateFile(state.getCurrentRunCount(), dateToWrite, null);
    }

    public boolean manageStateForIncompleteRun(State state){
        //Note, should ensure this never writes something for the initial run..
        String errorMessage = null;
        switch(state.getRunClassification()){
            case FORCED_FULL:
                errorMessage = failedFullHarvestDescriptor;
                break;
            case REPROCESSING:
                errorMessage = failedReprocessDescriptor;
                break;
        }
        //only worth re-writing the state file if there is a message to write as otherwise the state file should be identical to the existing file.
        if(errorMessage != null) {
            return writeStateFile(state.getPreviousRunCount(), state.getLastRunDate(), errorMessage);
        }
        return true;
    }

    private boolean writeStateFile(int runCount, Date runDate, String errorMessage){
//if completed successfully manage state file..
        //TODO: worry about how to handle failures that mean the state file is not updated after the diff phase.
        boolean stateFileManagementErrorDetected = false;
        //manage state file
        BufferedWriter stateWriter = null;
        try {
            stateWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(stateFile), "utf-8"));
            stateWriter.write(Integer.toString(runCount));
            stateWriter.newLine();
            stateWriter.write(lastRunDateFormat.format(runDate));
            String trimmedErrorMessage = StringUtils.trimToNull(errorMessage);
            if(trimmedErrorMessage != null){
                stateWriter.newLine();
                stateWriter.write(trimmedErrorMessage);
            }
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
            log.error("FATAL ERROR MANAGING STATE FILE - STATE MAY BE IRRETREIVABLY CORRUPT");
        }

        //return true if successful and false if not.
        return !stateFileManagementErrorDetected;
    }







}
