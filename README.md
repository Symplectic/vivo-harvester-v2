# Symplectic Elements Harvester for VIVO  
This is a significantly updated version of the VIVO Harvester originally shared with the community in 2013 and subsequently updated in 2016.
Since 2016, Symplectic have continued to work with the connector in support of a number of VIVO projects that used Symplectic Elements as the sole data source for VIVO. At the time of release this version of the connector is in use in at least 3 production VIVO sites (including  https://scholars.latrobe.edu.au) as well as in several other projects.  

This version of the connector is based on the earlier open source versions, and leverages a similar XSLT pipeline to perform the main translations. Nonetheless it has been significantly updated to support delta based updates from the Elements API (to reduce load on the Elements server), transfer of Elements group/group membership information and also to address various process complexity and performance issues.

For more information about the design and philosophy of this project please refer to the *"Description & Overview"* pdf in the [release documentation](https://github.com/Symplectic/New_Vivo_Harvester/releases).

_**Note: Symplectic is no longer actively developing/maintaining this codebase.**  
Nonetheless if you discover any bugs/issues you feel should be corrected in this canonical version please get in touch with Symplectic directly._

## Deploying From Binaries
If you just want to make use of the harvester, you should refer to the [release documentation](https://github.com/Symplectic/New_Vivo_Harvester/releases).
We strongly recommend you read the _"Description & Overview"_ and _"Installation Guide"_ documents in full to be sure that you understand how the harvester works and what is involved in setting it up.

_**Note**: In particular, please be aware that the "Initial load" of translated data into Vivo can be very slow (potentially lasting days, depending on data volume).
The "Installation Guide" contains advice on how to minimise this one of initial cost (e.g database configuration, temporarily disabling inferencing and indexing, etc)._ 

The remainder of this readme focuses on how to set up a development environment for working with the harvester codebase.
Aspects of this are useful if you are planning to develop or customise your XSLT crosswalks, but otherwise will primarily be of interest to developers.

# Setting up a Development Environment.
  
## Prerequisites  
You must have the Java Development Kit and Maven 2+ installed.  
    
**IMPORTANT**: It has been found that JDK 1.7.0 (runtime build 1.7.0-b147) has a problem with the concurrency methods used. Please ensure that a more recent JDK is used (1.7.0_03 and upwards have been tested with no apparent issues).  
  
## Development Environment  
Typically, we use IntelliJ IDEA for development (there is a free community edition as well as the commercial release). This IDE includes direct support for Maven projects. So all you need to do is open the "pom.xml" file within the elements-harvester directory. This will create an IntelliJ project, provide access to all of the code, and set up all of the third party dependencies as external libraries.
  
## Description  
The solution fundamentally consists of two components:  
  
1. The ***Harvester*** (ElementsFetchAndTranslate.java)  
2. The ***Fragment loader*** (FragmentLoader.java)  
  
### Harvester  
The harvester performs several functions:  
  
* Harvesting data from the elements API  
* Translating the harvested data to a VIVO compatible RDF representation (triples)  
* Loading those triples into a temporary triple store (Jena TDB)  
* Creating change-sets (diffs) by comparing the current temporary triple store to the temporary triple store from the previous run (if present)  
* Turning those change-sets into small fragment files (by default <2MB)
  
The harvester is designed to minimize the load placed on the Elements API by making use of *delta updates* when pulling data from the Elements API (i.e. it will try to only pull data that has changed in Elements since the previous harvest). There are, however, various areas where this is not possible (e.g. Elements group/group membership information). 

***Note:** If you are connecting to an Elements API running an API Endpoint Specification earlier than v5.5 there can be some issues using delta updates. A small fraction of changed items can be missed and these will never appear in VIVO until a "full" re-fetch of all the data in Elements is run (these issues typically occur when data is being modified in Elements whilst a delta harvest is being run).*  
  
#### Harvest Modes
The harvester has multiple modes it can be run in by passing arguments on the command line:  
  
1. Default (No argument) : This mode will run a full harvest on the first run and a delta on subsequent runs  
2. --full : Forces the harvester to perform a full re-harvest, re-fetching all the data from Elements  
3. --skipgroups : Instructs a delta run **not** to re-process the Elements group/group membership structures. Instead the harvester relies on a cache of group membership information from the previous run.  
4. --reprocess : Reprocesses the existing cache of raw data using the current XSLT mappings without touching the Elements API (useful when developing custom mappings).  
  
It is expected that these different modes will be combined to create a harvest schedule using a scheduling utility such as cron, e.g:

* Run a --skipgroups delta every 3 hours.  
* Run a normal delta every day at 4 am.  
* Run a --full on the last Sunday of each month.  
  
### Fragment Loader  
The Fragment loader meanwhile has just one function. To load any fragments generated by the harvester in to VIVO via the Sparql update API. The fragment files generated by the harvester are timestamped and indexed so that they form a queue of data, which the fragment loader works through one at a time.  
***Note**: The fragment loader is designed to be run as a daemon process and as such there are example files for integrating it with SystemD in the examples/example-integrations/systemd folder.*  
  
## Development Usage  

### Running from IntelliJ IDEA.  
Typically you will want to run/debug the harvester whilst developing, as it is the main process that queries the Elements API, retrieves all of the objects, and translates them into VIVO ontology RDF files. Therefore, it is useful to have this step setup for execution within the IDE.  
  
In IntelliJ IDEA:  
  
1. Open the drop-down next to the run / debug buttons in the tool bar.  
2. Select 'edit configurations'  
3. In the dialog, click the "+" icon at the top of the tree on the left. Choose 'Application'  
4. On the right, change the name to 'ElementsFetchAndTranslate'  
5. Set the main class to: uk.co.symplectic.vivoweb.harvester.app.ElementsFetchAndTranslate  
6. Set the VM options to: -Xms256m -Xmx10g -server -d64 -XX:+UseParallelOldGC -XX:+DisableExplicitGC -XX:+UseAdaptiveGCBoundary -XX:-UseGCOverheadLimit -XX:SurvivorRatio=16 -Xnoclassgc -XX:ParallelGCThreads=3  
7. Set working directory to: %project-dir%/elements-harvester  
8. Save this configuration (click OK).  
  
You may additionally wish to set the *Program Arguments* value, e.g:
    
    --full, --skipgroups, --reprocess, etc
  
***Note** : The -Xms and -Xmx options configured in Step 6 relate to the amount of ram assigned to the Java VM. This typically needs to be a large amount (see the Performance Considerations section). With the configuration indicated above, the VM can consume up to 10Gb of RAM.*   
  
You should now be able to run and/or debug the Elements harvester.  
  
### Configuration within the IDE
You will need to configure the harvester. By default the harvester looks for a file named *"elementfetch.properties"* (a basic java properties file) somewhere in its classpath.
The file in *"src/main/resources"* is what you want to use when running the harvester in the IDE.
Alternatively you can add a file named *"developer-elementfetch.properties"* to the resources folder, and add your configuration there. The harvester will preferentially load its configuration from the *developer* file, but the build process will ignore it completely, meaning you can have you debug configuration set up without affecting the shipped defaults.

At a minimum you will need to configure the harvester to know how to reach the Elements API being harvested from. This will involve setting at least the *apiEndpoint* and *apiVersion* parameters. Secure APIs will additionally need the *apiUsername* and *apiPassword* parameters.  
  
You will also need to specify the XSLT pipeline's entry point via the *xslTemplate* parameter.
The default setting (*scripts/example-elements/elements-to-vivo.xsl*) deliberately points to a non-existent file. This works with the "init" scripts" in a manner that minimises the chance of overwriting custom work if the harvester binaries are updated in a typical installation.

To make use of the default XSL crosswalks shipped with the toolkit you will need to alter the *xslTemplate* parameter to:

    example-scripts/example-elements/elements-to-vivo.xsl
    
***Note:** You may additionally wish to set the zipFiles parameter to false if you are intending to develop changes to the XSL crosswalks, although be aware that this will significantly increase the storage requirements of the harvester's intermediate output*
  
When you run the harvester for the first time (assuming you have not altered the configuration), it will create a *data* subdirectory within the *%project-dir%/elements-harvester* directory.  
Inside this *data* directory will be:  
  * *raw-records* (containing the XML data as retrieved from the Elements API).  
  * *translated-records* (containing the translated VIVO triples in RDF-XML).  
  * *harvestedImages* (containing any processed user photos pulled from Elements).  
  * *tdb-output* (containing the temporary triple stores, change sets and the fragment files).  
  * *other-data* (containing cached group membership information).  
  
Additionally a *state.txt* file will appear in the %project-dir%/elements-harvester' directory which is how the harvester tracks the current state of the system.  
  
### Developing Translations to the VIVO model  
  
As each installation of Elements can capture data in slightly different ways, the key customization for anyone wanting to implement a VIVO instance with Symplectic Elements will be the translation of the Elements data to the VIVO model.  
  
The Elements API returns records in an XML format, and an XSLT pipeline is used to convert that data into the VIVO compatible RDF data (triples), in RDF-XML format.
With IntelliJ IDEA and its XSLT-Debugger plugin, it is possible to run the XSLT translations directly within the IDE, and even offers a step-by-step debugger for the translation (*your mileage may vary on the usability of this for complex stylesheets*).
  
To make use of this feature, you should run the harvester to populate the *data/raw-records* directory with input files. By default the harvester compresses (gzips) these intermediate output files. When running the XSLT's directly within the IDE, however, you need uncompressed input. Therefore, if you are planning to use this feature, it is important that your initial fetch of data is run with the *zipFiles* parameter set to false in *elementfetch.properties*.
***Note:** this will inevitably significantly increase the storage requirements for the "data" directory*

#### Additional Data
The harvester often passes extra data (over and above the main input file) to the XSLT pipeline via XSL parameters. For example when processing Elements relationship data it sometimes passes in an **extraObjects** parameter containing the raw API XML representations of both the objects in that relationship. 
The example crosswalks have been designed to make it possible to do the same by passing in parameters to the XSLT pipeline from a *Run Configuration* setup in the IDE. 

#### Setting up an XSL "Run Configuration" 
Once you have a cache of harvested data to work from then:  
  1. Open the drop-down next to the run / debug buttons in the tool bar.  
  2. Select 'Edit configurations'.  
  3. In the dialog, click the + icon at the top of the tree on the left. Choose 'XSLT'.  
  4. On the right, give this configuration a name.  
  5. Set the XSLT script file to: %project-dir%/example-scripts/example-elements/elements-to-vivo.xsl  
  6. Set Choose XML input file to:  *%project-dir%/example-scripts/example-elements/data/raw-records/....* (choose a user / publication / relationship file, depending on the translation you are working on)  
  7. Uncheck the 'Make' checkbox (you don't need to rebuild the code when running the XSLT translation).  
  8. In the parameters section click the + icon, set the Name Column to "useRawDataFiles" and the Value column to "true".  
  9. Save the configuration (click OK).  
  
You should now be able to run the configuration, and see the translated result appear in a 'console' tab. The *useRawDataFiles* option instructs the default crosswalks to retrieve the *extraObject* data direct from the filesystem (assuming a certain layout of the directories and files beneath the "*data*" directory.

Group and Group Membership transformations are special and the harvester passes in specific XSL parameters to make them possible: 

  * **includedParentGroup** (Group translation)
  * **userGroupMembershipProcessing** and **userGroups** (Group Membership translation)

Full details of this can be found in the *"Crosswalk Development Guide"* pdf in the [release documentation](https://github.com/Symplectic/New_Vivo_Harvester/releases).  
***Note:** the example crosswalks provide the capability for the value of these parameters to be the "path" to a file on disk which contains XML data. This makes it possible to use these parameters from an XSL Run configuration in the IDE (which types all XSL parameters as simple string values). You can actually do the same with the "extraObjects" parameter if you don't want to use the useRawDataF*

#### Saxon Vs Xalan
If you have issues with the XSL translation failing with errors mentioning "xalan", it may be an issue where IntelliJ defaults to using the Xalan XSLT engine, which only supports XSLT 1.0. The default crosswalks require an engine that supports XSLT 2.0.

Ideally you want to use Saxon HE as that is the engine used internally by the harvester.
To force IntelliJ to use Saxon, first ensure that it is set up as a dependency for your project:

    File-> Project Structure-> Modules (on the LHS)-> Dependencies tab (on RHS)

From that dialog add a dependency on  the *saxon9he.jar* jar file, which you should find in the *plugins/xslt-debugger/rt* directory within your IntelliJ installation.
 Once that is done edit your XSL Run configuration(s), on the Advanced tab, input 

     -Dxslt.transformer.type=saxon9 

in the VM Arguments field. Additionally you should ensure you have selected "From Module" in the "Classpath and JDK" section.
  
## Packaging and Deployment  
  
When you are ready to move from your workstation to a server, you will need to package up harvester, to ready it for installation on a server. To do this you need to run 

    mvn clean package
  
Once Maven finishes executing, a *.tar.gz* file will be created in the *target* directory.  
***Note:** you can run these same Maven commends (clean and package) from within your IDE if you prefer.*  

For details of how to deploy the built package please refer to the *"Installation Guide"* pdf in the [release documentation](https://github.com/Symplectic/New_Vivo_Harvester/releases).  

## Performance Considerations  
  
### Disk IO  
  
The Harvester can be disk intensive. The disk is constantly being read from and written to during many operations:  
  
  * Harvesting raw data.  
  * Writing translated data.  
  * Populating temporary triple store.  
  * Writing Change sets and fragments.  
  
This is particularly true during loading of the temporary triple store where, for a typically sized institution, several gigabytes of data can be being read from the cache of translated triples and written into the on disk TDB triple store. As this process has to be performed every time the harvester is run (even on a delta update) disk IO is critical to the overall performance of the harvester.  

**We very strongly recommend you use SSD based storage on any server where the harvester will be used.**  
  
### Memory  
  
The harvester can be very memory intensive, this is particularly true during the diff operation where the current temporary triple store is compared to the equivalent store from the previous run.  
During this process in both copies of the triple store can be loaded into ram. To accommodate this the default elementsfetch.sh script (used in a deployed instance to launch the harvester) ensures that the Java Virtual Machine (JVM) being used to run the process has access to up to 10 gigabytes of ram.
  
If your datasets are significantly larger than 5Gb you may end up with poor performance (paging) or crashes during the diff operation (e.g. taking longer than 5-10 minutes). If this occurs you may need to increase the amount of RAM assigned to the JVM for the FetchAndTranslate operation.  
Similarly if your dataset is much smaller than 5Gb you may be able to reduce the amount of RAM being assigned to the JVM.  
  
## Acknowledgements  
  
The first version of the Elements-VIVO Harvester was developed by Ian Boston, and was originally available at: https://github.com/ieb/symplectic-harvester.  
  
The second version was developed by Graham Triggs (whilst working for Symplectic), and can be found at https://github.com/Symplectic/VIVO. Additional thanks to Daniel Grant from Emory University for his contributions to the second version.
  
This version was developed by Andrew Cockbill (whilst working for Symplectic).
