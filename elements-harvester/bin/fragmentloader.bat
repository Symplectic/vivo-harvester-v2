:: *******************************************************************************
::   Copyright (c) 2017 Symplectic. All rights reserved.
::   This Source Code Form is subject to the terms of the Mozilla Public
::   License, v. 2.0. If a copy of the MPL was not distributed with this
::   file, You can obtain one at http://mozilla.org/MPL/2.0/.
:: *******************************************************************************

::configure class path relative to this scripts path (assumes default install lib conf directory layout.
SET HOME_PATH=%~dp0
ECHO %HOME_PATH%

SET CP=%HOME_PATH%;%HOME_PATH%lib;%HOME_PATH%lib\*
::ECHO %CP%

::change to working directory
cd %HOME_PATH%

::pass incoming params to java program
java -cp "%CP%" %OPTS% uk.co.symplectic.vivoweb.harvester.app.FragmentLoader %*
