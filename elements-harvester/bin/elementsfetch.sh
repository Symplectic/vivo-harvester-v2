#!/bin/bash

#
# *******************************************************************************
#   Copyright (c) 2017 Symplectic. All rights reserved.
#   This Source Code Form is subject to the terms of the Mozilla Public
#   License, v. 2.0. If a copy of the MPL was not distributed with this
#   file, You can obtain one at http://mozilla.org/MPL/2.0/.
# *******************************************************************************
#   Version :  ${git.branch}:${git.commit.id}
# *******************************************************************************
#

#update memory to match your hardware -- ideally set both to be the same, in general the more memory the better, but too much can cause errors as well.
#8G-12G on large vivo's seems to work well
MIN_MEM=256m
MAX_MEM=10g

#Where any harvests initiated via this batch file should be logged..
LOG_FILE_PATH_FRAGMENT="logs/harvests_run.log"

#Variable for optimizations to the Java virtual machine. (Borrowed from Vivo Harvester Diff script)
#-server                                                Run in server mode, which takes longer to start but runs faster
#-d64                                                   Use 64-bit JVM
#-XX:+UseParallelOldGC                  Use high throughput parallel GC on old generation
#-XX:+DisableExplicitGC                 Prevent direct calls to garbage collection in the code
#-XX:+UseAdaptiveGCBoundary             Allow young/old boundary to move
#-XX:-UseGCOverheadLimit                Limit the amount of time that Java will stay in Garbage Collection before throwing an out of memory exception
#-XX:SurvivorRatio=16                   Shrink eden slightly (Normal is 25)
#-Xnoclassgc                                    Disable collection of class objects
#-XX:ParallelGCThreads=3                Maximum number of Parallel garbage collection tasks

HARVESTER_JAVA_OPTS="-server -d64 -XX:+UseParallelOldGC -XX:+DisableExplicitGC -XX:+UseAdaptiveGCBoundary -XX:-UseGCOverheadLimit -XX:SurvivorRatio=16 -Xnoclassgc -XX:ParallelGCThreads=3"
#OPTS="-Xms$MIN_MEM -Xmx$MAX_MEM $HARVESTER_JAVA_OPTS -Dharvester-task=$HARVEST_NAME.$DATE"
OPTS="-Xms$MIN_MEM -Xmx$MAX_MEM $HARVESTER_JAVA_OPTS"

HOME_PATH=$(dirname "$0")
#set runtime variables based on home path
LOG_FILE="${HOME_PATH}/${LOG_FILE_PATH_FRAGMENT}"
CP="${HOME_PATH}:${HOME_PATH}/lib:${HOME_PATH}/lib/*"

#ensure that the log file is present..
mkdir -p "$(dirname "$LOG_FILE")" && touch $LOG_FILE

#change to working directory
cd ${HOME_PATH}

RUN_DESCRIPTOR="differential"
if [[ $1 = "--skipgroups" ]]
then
    RUN_DESCRIPTOR="skipgroups diff"
elif [[ $1 = "--full" ]]
then
    RUN_DESCRIPTOR="full"
elif [[ $1 = "--reprocess" ]]
then
    RUN_DESCRIPTOR="reprocessing"
fi

if [[ -z $HARVEST_ORIGIN ]]
then
    HARVEST_ORIGIN="SERVER"
fi

#run process ensuring we avoid concurrent processes, no matter their source.
{
    START_TIME="$(date +"%Y/%m/%d %T")"
    flock -xn 200
    if [[ $? != 0 ]]
    then
        echo "$START_TIME    :    $HARVEST_ORIGIN	: $RUN_DESCRIPTOR harvest request ignored as a concurrent harvest is already running" >> $LOG_FILE
    else
        echo "$START_TIME    :    $HARVEST_ORIGIN	: $RUN_DESCRIPTOR harvest initiated" >> $LOG_FILE
        #pass incoming params to java program
        java -cp "${CP}" $OPTS uk.co.symplectic.vivoweb.harvester.app.ElementsFetchAndTranslate "$@"
        STATUS=${?}
        if [[ $STATUS = 0 ]]
        then
            echo "$(date +"%Y/%m/%d %T")    :    $HARVEST_ORIGIN	: $RUN_DESCRIPTOR harvest initiated at $START_TIME completed successfully" >> $LOG_FILE
        else
            echo "$(date +"%Y/%m/%d %T")    :    $HARVEST_ORIGIN	: WARNING $RUN_DESCRIPTOR harvest initiated at $START_TIME failed" >> $LOG_FILE
        fi
    fi
} 200>elementsfetchlock.lck
