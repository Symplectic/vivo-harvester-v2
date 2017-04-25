#!/bin/bash

HOME_PATH=$(dirname "$0")
#echo ${HOME_PATH}

if [ ! -d ${HOME_PATH}/../scripts ];
then
    cp ${HOME_PATH}/../example-config/* ${HOME_PATH}/../
    echo "Config initialised."
    mkdir ${HOME_PATH}/../scripts
    cp -r ${HOME_PATH}/../example-scripts/example-elements ${HOME_PATH}/../scripts
    echo "Scripts initialised."
else
    echo "This Harvester already seems to be initialised."
fi
