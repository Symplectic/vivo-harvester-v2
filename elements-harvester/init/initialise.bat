:: *******************************************************************************
::   Copyright (c) 2019 Symplectic. All rights reserved.
::   This Source Code Form is subject to the terms of the Mozilla Public
::   License, v. 2.0. If a copy of the MPL was not distributed with this
::   file, You can obtain one at http://mozilla.org/MPL/2.0/.
:: *******************************************************************************
::   Version :  ${git.branch}:${git.commit.id}
:: *******************************************************************************

ECHO OFF

SET HOME_PATH=%~dp0
ECHO %HOME_PATH%

IF NOT EXIST "%HOME_PATH%\..\scripts" (
    mkdir "%HOME_PATH%\..\scripts"
    copy /-Y "%HOME_PATH%\..\examples\example-config\*" "%HOME_PATH%\..\"
    copy /-Y "%HOME_PATH%\..\examples\example-bin\*.bat" "%HOME_PATH%\..\"
    echo d | xcopy /E "%HOME_PATH%\..\examples\example-scripts\example-elements" "%HOME_PATH%\..\scripts\example-elements"
) ELSE (
    echo This Harvester already seems to be initialised.
)