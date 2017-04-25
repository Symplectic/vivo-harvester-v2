ECHO OFF

SET HOME_PATH=%~dp0

IF NOT EXIST %HOME_PATH%\..\scripts (
    copy /-Y %HOME_PATH%\..\example-config\* %HOME_PATH%\..\
    mkdir %HOME_PATH%\..\scripts
    echo d | xcopy /E %HOME_PATH%\..\example-scripts\example-elements %HOME_PATH%\..\scripts\example-elements
) ELSE (
    echo This Harvester already seems to be initialised.
)