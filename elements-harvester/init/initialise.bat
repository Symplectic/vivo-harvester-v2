:: *******************************************************************************
::   Copyright (c) 2017 Symplectic. All rights reserved.
::   This Source Code Form is subject to the terms of the Mozilla Public
::   License, v. 2.0. If a copy of the MPL was not distributed with this
::   file, You can obtain one at http://mozilla.org/MPL/2.0/.
:: *******************************************************************************

ECHO OFF

SET HOME_PATH=%~dp0

IF NOT EXIST %HOME_PATH%\..\scripts (
    copy /-Y %HOME_PATH%\..\example-config\* %HOME_PATH%\..\
    mkdir %HOME_PATH%\..\scripts
    echo d | xcopy /E %HOME_PATH%\..\example-scripts\example-elements %HOME_PATH%\..\scripts\example-elements
) ELSE (
    echo This Harvester already seems to be initialised.
)