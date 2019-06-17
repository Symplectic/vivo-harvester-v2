# Cron integration

If you make use of Cron to schedule regular harvests you need to create a "crontab" to run the appropraite harvests. The *vivo-harvester-cron* file in this directory contains a set of example crontab schedules that you can use.

**PLEASE NOTE:**
  - *All the schedules in the file are commented out, you will need to remove the # to uncomment the relevant lines when setting up an actual crontab.*
  - *The examples assume you have installed the harvester at /usr/local/vivo/harvester, it this is not the case, you will need to adjust as appropriate*

## Configuring Crontab
To set up your crontab you will need to either:

* Edit a specific user's crontab file  (sudo su **** then crontab -e)
* Add a file to the system's cron schedule in /etc/cron.d/

The system's cron jobs are run as root, so if you want to use a different user account to run the
harvester process you will need to use the former option. If this is the case, take care that you do not remove any
existing scheduled in the user's crontab that may exist for other processes.

If you are using the root user to run the harvester you can still edit the root user's crontab, but it is simpler to
just add a file into */etc/cron.d/* (e.g. just copy the, suitably edited, *vivo-harvester-cron* file from this directory).
All files in */etc/cron.d/* are read when setting up the system's cron schedule, so you can create a file just for your harvest schedule.

## Setting the "Harvest Origin"
At the top of the example crontab file is the line:
  - HARVEST_ORIGIN=CRON
This is present to set the "HARVEST_ORIGIN" environment variable and ensures that any harvest's run by cron are reported as such in the log of harvests run (e.g. as seen in the web interface if it is configured).

Not all distributions support the abilty to set environment variables in this manner in a crontab. If your distibution does not you should prepend the command in every schedule line of your crontab with:
  - export HARVEST_ORIGIN=CRON &&

e.g.

       00 04 * * * export HARVEST_ORIGIN=CRON && /usr/local/vivo/harvester/elementsfetch.sh



