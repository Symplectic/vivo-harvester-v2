# Systemd integration

If you make use of the FragmentLoader process to pass data to Vivo over it's Sparql update API,
then you will want the FragmentLoader to run as a daemon process.

The .service file in this directory is a template for what you will need to integrate the FragmentLoader as a Systemd unit.
To make use of it you will need to:

* Update the .service file to replace %HARVESTER_INSTALL_DIR% with the rooted path where you have installed this harvester.
* Copy the file into /lib/systemd/system (this step may be distro dependant)
* Enable the new unit, i.e. run : systemctl enable fragmentloader.service

Once this is done you should be able to control the FragmentLoader process as a normal Systemd service i.e using:

* systemctl start fragmentloader.service
* systemctl stop fragmentloader.service

Note: you should still monitor the process in case the FragmentLoader experiences issues it cannot recover from.


