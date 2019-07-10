#Init scripts
This folder contains scripts designed to initialise an instance of the harvester that has been deployed from a *.tar.gz install package.

The folder offers two *initialise* scripts, one for Linux (*initialise.sh*) and one for Windows (*initialise.bat*).

These scripts set up a newly deployed harvester by copying the relevant *bin*, *config* and *script* items, for your environment, out of the examples folders. The chosen "initialise" script should only be run once, immediately after initial deployment.

This *init* based design ensures that you can update the harvester binaries without needing to worry about deleting your configuration/crosswalk changes.

***Note:** The design does not, however, guarantee that your configuration/other changes will be compatible with any newly deployed code* 

## General Configuration
You will still need to apply relevant configuration changes to the various "properties" files, etc after running the initialise script, as you still need to configure your instance appropriately.
The init process is simply a necessary preparatory step.
