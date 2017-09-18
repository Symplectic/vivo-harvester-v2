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
CP="${HOME_PATH}:${HOME_PATH}/lib:${HOME_PATH}/lib/*"

#change to working directory
cd ${HOME_PATH}

#pass incoming params to java program
java -cp "${CP}" $OPTS uk.co.symplectic.vivoweb.harvester.app.ElementsFetchAndTranslate "$@"
