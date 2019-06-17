#!/usr/bin/perl


# Primary Config parameter ########################################################

    #Edit this to indicate where you have installed the harvester
    $harvesterInstallDirectory='/usr/local/vivo/harvester/';

# Other parameters ################################################################

    #NOTE! You should not typically need to alter these

    #Required for json/harvests.json script
    $harvestsRunLog= "$harvesterInstallDirectory/logs/harvests_run.log";

    #Required for log & json/log.json scripts
    $harvestsLogDirectory= "$harvesterInstallDirectory/logs";

    #Required for json/fragmentloader.json script
    $fragmentLoaderLog = "$harvesterInstallDirectory/logs/fragment-loader.log";
    $fragmentsDirectory = "$harvesterInstallDirectory/data/tdb-output/fragments";

    #Required for data script
    $harvesterDataDirectory = "$harvesterInstallDirectory/data";
    $harvesterRawDataDirectory= "$harvesterDataDirectory/raw-records";
    $harvesterTranslatedDataDirectory= "$harvesterDataDirectory/translated-records";
    $harvesterTdbOutputDirectory= "$harvesterDataDirectory/tdb-output";
    $harvesterOtherDataDirectory= "$harvesterDataDirectory/other-data";
    $relationshipsFile = "$harvesterTdbOutputDirectory/relationships.txt";
    $includedFilesFile = "$harvesterTdbOutputDirectory/fileList.txt";

    #Required for initiateHarvest script
    $workQueuePath= "$harvesterInstallDirectory/control/work-requests" ;

####################################################################################

    #Code to ensure path params don't end up containing "//" and that directory params don't end with a trailing "/"
    #Done to ensure consistency for scripts using these values
    #Don't alter this unless you are adding new configs

    $harvesterInstallDirectory =~ s|//+|/|g;
    $harvestsRunLog =~ s|//+|/|g;
    $harvestsLogDirectory =~ s|//+|/|g;
    $fragmentLoaderLog =~ s|//+|/|g;
    $fragmentsDirectory =~ s|//+|/|g;
    $harvesterDataDirectory =~ s|//+|/|g;
    $harvesterRawDataDirectory =~ s|//+|/|g;
    $harvesterTranslatedDataDirectory =~ s|//+|/|g;
    $harvesterTdbOutputDirectory =~ s|//+|/|g;
    $harvesterOtherDataDirectory =~ s|//+|/|g;
    $relationshipsFile =~ s|//+|/|g;
    $includedFilesFile =~ s|//+|/|g;
    $workQueuePath =~ s|//+|/|g;


    #Code to remove trailing "/" from any paths
    $harvesterInstallDirectory =~ s|/\$||g;
    $harvestsLogDirectory =~ s|/\$||g;
    $fragmentsDirectory =~ s|/\$||g;
    $harvesterDataDirectory =~ s|/\$||g;
    $harvesterRawDataDirectory =~ s|/\$||g;
    $harvesterTranslatedDataDirectory =~ s|/\$||g;
    $harvesterTdbOutputDirectory =~ s|/\$||g;
    $harvesterOtherDataDirectory =~ s|/\$||g;
    $workQueuePath =~ s|/\$||g;

    #Return 1 to make it work as a Perl module
    1;


