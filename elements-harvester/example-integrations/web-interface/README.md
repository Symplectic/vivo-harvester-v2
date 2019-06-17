# Web interface integration

This directory contains instructions for setting up an apache CGI based web interface for the Elements Vivo harvester. This interface allows you to:
  * Manually initiate a harvest
  * See the status of, and log files from, recent harvests
  * See the status of the fragment loader
  * Browse data in the harvester's internal cache

**Please note: Installation of these scripts is non-trivial**

## Dependencies
Ensure you have installed all these modules:
     *incron, perl-File-Touch, perl-File-ReadBackwards, perl-CGI, perl-XML-Tidy*

The exact names of these modules and the commands needed to install them will vary between distributions and versions, below are two examples
  * CENTOS7	: yum install incron perl-File-Touch perl-File-ReadBackwards perl-CGI perl-XML-Tidy
  * DEBIAN	: sudo apt install libfile-touch-perl libfile-readbackwards-perl libcgi-pm-perl libxml-tidy-perl

Once the packages are installed ensure that incron is registered as a daemon with SystemD and that it is running:

    systemctl enable incrond.service
    systemctl start incrond

***Note:** This is not intended to be a guide to installing and using incron - please refer to online resources if you have difficulties.*

## Apache configuration

Use of these scripts assumes you are using apache as a reverse proxy in front of the java servlet container running Vivo (e.g. Tomcat). To use these scripts you need to adapt the Virtual Host that performs this proxying to:
    1. Disable the proxy for your selected subpath (typically "/harvesterControl").
    2. Alias your selected subpath to the "cgi-bin".

***Note:** If you have configured Apache and Vivo so that Vivo appears directly at the root "/" path, it is important to ensure that your selected "path" does not clash with any paths actually used by your Vivo instance.*

***Note:** The location of the default "cgi-bin" varies in different distributions, but ultimately it depends on the configuration of the virtual host, within Apache, that you use to control the reverse proxy to Vivo*

Typical changes to the apache virtual host are shown below. Depending on your pre existing configuration you may already have some of these aspects configured, so take care not to duplicate anything.

### Disable proxy for your selected path
These lines need to go ahead of any other proxypass rules in your virtual host config

    ProxyPass /harvesterControl !
    ProxyPass /harvesterControl/ !

### Configure the RewiteEngine
This is done so that the "default" script is called if you hit the your selected path (e.g. harvesterControl):

    RewriteEngine  on
    RewriteRule "^/harvesterControl/?$" "/harvesterControl/default" [R]

### Configure the ScriptAlias
This ensures that requests to your selected path are mapped to the cgi-bin

    ScriptAlias "/harvesterControl/" "/var/www/cgi-bin/"

### Restrict access to your selected path (e.g. */harvesterControl*)
Note, the "Directory" section for cgi-bin is typically already set up in most apache Virtual hosts, to set up the
SSLOptions and StdEnvVars properly, we are adding the *Auth...* and *Require...* settings.
***Note:** you need to additionally create the AuthUserFile and add the relevant user credentials using htpasswd. You should make sure you have set up sensible access permission for this file (e.g. only make it readable by users in the apache group)*

    <Directory "/var/www/cgi-bin">
        SSLOptions+StdEnvVars

        AuthType Basic
        AuthName "Restricted Files"
        AuthBasicProvider file
        AuthUserFile "/usr/local/apache/passwd/passwords"
        Require user ???????
    </Directory>

  ***Note:** Make sure you only configure authentication if you have SSL configured in your Virtual host, or you will be passing credentials over the network in the clear*

## CGI Script Deployment
Copy all the files and directories within the folder cgi-scripts into your configured cgi-bin.
For example, if the cgi-bin configured in your virtual host is at /var/www/cgi-bin:

    cp -r cgi-scripts/* /var/www/cgi-bin

Once the files are present ensure that the user that apache runs as has execute permissions on everything except the files in the "lib" folder. Exactly how you achieve this is up to you. Typically you will want to make all the files owned by a suitable user, which is most suitable will depend on your apache configuration, for example:

    chown-R root:root /var/www/cgi-bin
    chown -R apache:apache /var/www/cgi-bin

You will then want to make sure that all the items that are not part of the "lib" folder are executable by that user.
For example:

    ls -1 /var/www/cgi-bin | grep -v lib | xargs chmod -R u+x

### CGI Script configuration
The main config file for the CGI scripts is in lib/config.pl. Assuming you have installed your harvester at the recommended default of */usr/local/vivo/harvester* then you may not need to configure anything.
If you installed somewhere else you will definitely need to set the main config parameter ($harvesterInstallDirectory) within this file.

     $harvesterInstallDirectory='/usr/local/vivo/harvester/';

For a typical installation of the control scripts $harvesterInstallDirectory is the only parameter you are likely to need to set, although if you install the "control" directory in a non-standard location (see below) you may also need to set $workQueuePath.

You should now be able to reach the web interface from a browser at your chosen path, assuming your apache and CGI configurations are correct. You should be able to see a list of recent harvests, the fragment loader's status, view logs of those harvests and use the "data" url to browse the data in the internal cache.
At this point however, the facility to initiate a harvest will be non-functional.

## Control Script deployment
To enable the web interface to initiate harvests you need to:
  1. Copy the "control" directory from this folder into the main harvester installation directory (i.e. alongside elementsfetch.sh).
  2. Ensure that the file harvesterControl.sh in your new folder is executable by the user you wish to run the harvester (e.g. root).
  3. Ensure that the "work-requests" sub folder within your new folder can be written into by the user that runs Apache. Here it is worth using group permissions to ensure write access is reasonably restricted.
  4. Configure incron to monitor the "work-requests" sub folder within your new folder and call the harvesterControl script as the user you wish to run the harvester.

The file *vivo-harvester-incron* in this folder contains an example of the configuration needed in the appropriate user's incrontab. The configuration instructs incron to monitor the "work-request" folder for the creation of new files. When it detects a new file in the work requests folder it calls the harvesterControl script passing in the path and the name of the new file. The Apache cgi scripts write files into this monitored folder to enqueue "work requests", so the user that run's apache must have write permissions on this folder.

 You will need to set this up in the incrontab of the user that you wish to run the harvester using "su" and then "incrontab -e". If you are running the harvester as "root" then it is much easier to simply create a file in /etc/incron.d. Analogously to cron any files in this directory are used to set up the system "incron" configuration.
You can simply copy over the *vivo-harvester-incron* from this folder if everything is installed at the default locations.

**Once the control scripts are deployed and incron is configured you should be able to initiate harvest's from the web interface.**

### Non standard installation
If you do not install the control directory in the root of your harvester installation you will need to edit:
  1. The harvesterControl.sh script so it can locate the main elementsfetch.sh file.
  2. The incron configuration so that you have the correct paths to the work-requests directory and the harvesterControl.sh script.
  3. The lib/config.pl script within cgi-bin to ensure that $workQueuePath is configured so that apache knows the correct path to the work-requests directory.

