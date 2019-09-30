# Example Integrations
This folder contains details of various ways that the harvester can be integrated with other systems when deployed on a 
server.

## Apache Proxy
This does not strictly relate to the harvester, in that it provides example configuration files for setting up Apache as
a reverse proxy in front of the Java Servlet container where you have deployed VIVO. It is relevant to the harvester though,
as having this reverse proxy configuration is essential if you wish to use the "web interface" integration, a specific 
example configuration file is provided for this use case.

## Cron
This provides examples of how to setup a CRONTAB to automatically run the harvester on a defined schedule, so that the data
 in your VIVO instance is kept up to date with changes in Elements.

## SystemD
This provides details of how to integrate the FragmentLoader process as a SystemD controlled daemon process.

## Vivo List View Config
This contains a set of example list view configs to improve Vivo's handling of "VCard" information in context objects 
(e.g. authorships/editorships/roles/etc). These are particularly useful if your crosswalks are based on the default set.

## Web Interface
This provides information, and a set of CGI scripts, which can be used with Apache to enable a simple "web interface", to both 
control and monitor the harvester.
