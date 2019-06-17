#!/bin/bash

HARVESTER_LOCATION="/usr/local/vivo/harvester"
FILE="$1/$2"

SG_CMD_FILENAME="run-sg-diff"
DIFF_CMD_FILENAME="run-diff"
FULL_CMD_FILENAME="run-full"
REPROCESSING_CMD_FILENAME="run-reprocess"

export HARVEST_ORIGIN="WEB"

sleep 1
rm -f $FILE

if [[ $2 = $SG_CMD_FILENAME ]]
then
    ${HARVESTER_LOCATION}/elementsfetch.sh --skipgroups
elif [[ $2 = $DIFF_CMD_FILENAME ]]
then
    ${HARVESTER_LOCATION}/elementsfetch.sh
elif [[ $2 = $FULL_CMD_FILENAME ]]
then
    ${HARVESTER_LOCATION}/elementsfetch.sh --full
elif [[ $2 = $REPROCESSING_CMD_FILENAME ]]
then
    ${HARVESTER_LOCATION}/elementsfetch.sh --reprocess
fi


