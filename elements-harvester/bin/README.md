# Example "bin" scripts
This folder contains example scripts for running the Harvester (elementsfetch) and the FragmentLoader processes from the command line.
Scripts are provided for both Linux (*.sh) and Windows (*.bat).

***Note**: The harvester has primarily been developed and tested on linux. The windows scripts are only provided for completeness. The windows scripts are not as fully featured as the linux scripts, 
for example they do not contain locking sections to ensure that only one harvest is running at a time, nor do they log harvest events to a log file.*

## Ensuring only one Harvest is running
The linux version of the script relies on the file locking utility _"flock"_ to ensure that only one Harvest process can run at a time.
This utility comes pre-installed with most linux distributions (even minimal ones). If, however, it is not present it can usually be installed from the repositories with a simple install command, e.g.:
  * apt install flock
  * yum install flock
  
## Initialisation
In a typical installation you will not interact with this folder directly as the relevant scripts for your environment will be copied out of here when you run the _init_ script.