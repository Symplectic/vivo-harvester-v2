# Organisation Matching branch
This is an experimental branch created to explore the issues associated with the differences between how Elements and Vivo represent "Organisation" data.

Much of this work consists of changes to the crosswalking layer, but there are additional changes to the core harvester code to facilitate better matching to Elements groups, you can see the changes by comparing to the develop branch.

|                         Important Notice|
|----------------------------------------------------------------------------------------------------------------------------------|
|Development on this branch is incomplete and has never been used on a production site. It was not taken further as we found that divergence between the data in the source system, *Elements*, and the display layer, *Vivo*, (such as caused by having this type of complex logic in the translation layer) can lead to confusion, both for end users and site administrators. It is provided here solely as a potentially useful base for further work. |

## The Problem
Within Elements (as of late 2019) external organisations are often represented in "Addresses" or other simple field data, held within other objects, (e.g. User profile appointment, degree and employment lists, various professional activity objects etc). Elements provides drop down lists to provide auto complete information (based on GriD data), and will (depending on subscription) also attempt to resolve a "GriD identifier" for any manually entered data, but these often only represent the parent organisation of a particular department or school.
Internal organisational structures meanwhile can be represented as user groups and/or by organisational structure objects.

In Vivo meanwhile, organisations are generally first class objects, which related activities are expected to be linked to. 

In bridging these gap the harvester has to create a "URI" to represent any given organisation. Often this URI ends up being constructed based on information held within Elements about the organisation's name(s). This can result in two related but different issues:

1. **Duplicate Organisation objects in Vivo**  
This occurs when the name information within two addresses representing the same organisation was different enough that the constructed URI's used with Vivo are different e.g. "*Cambridge University*" vs "*The University of Cambridge*").
2. **Organisation's with multiple "label" values in Vivo**
This occurs when the name information in two addresses is very similar, but varies in terms of punctuation, etc. In this case the constructed URI's will be the same as invalid characters are stripped from the generated URI, but each address will contribute a distinct "label", any of which can end up being displayed within Vivo.

## Attempted Improvement
This branch is an initial pass at one possible approach to try and address these issues, by providing the ability for the canonical representation of a given organisation to be influenced through configuration. Specifically it allows for:

1. The ability to define a canonical "Name" that will be used as a given organisation's label within Vivo, *and optionally to specify the "type" of Vivo organisation that item will be represented as.*
2. Automatic matching of Elements organisation data (e.g. addresses) to these configured names using a simple heuristic based word stemming and comparison engine, as part of the crosswalking process.

The code also attempts to match Elements organisation data to Elements "groups", to allow information like a user's degrees to be wired up to the "group based" organisations within Vivo. This also allows the harvester to try and assign "position" labels within the "group" organisations based on the user's appointment information.
*To facilitate this feature the harvester's generation of its internal group and user cache moves from being the last thing done in a typical harvest to the very start, this allows the harvester to know which Elements groups are going to be represented in Vivo throughout the harvest pass, It then makes this information available to all stages of crosswalking via an additional XSLT parameter - "**elementsGroupList**"*

In addition to allowing the user to define the "canonical name", the configuration allows users to:
1. Define "aliases" to manually account for any matches missed by the automated matching.
2. The ability to define an "iso-code" to help distinguish between organisations with similar names in different countries (if defined this is then obeyed by the matching processes).
3. Automatic "correction" of some words that are often mis-spelt in manually entered data.

### Example Configuration

    <config:org-unit name="Newcastle University" type="http://vivoweb.org/ontology/core#University" iso-code="GB">
        <config:alias>Newcstle University</config:alias>
        <config:department-overrides>  
            <config:org-unit name="History Department" type="http://vivoweb.org/ontology/core#AcademicDepartment">
                <config:alias>Historical Sciences</config:alias>
            </config:org-unit>
        </config:department-overrides>
    </config:org-unit>

The configuration above shows how to define the canonical name to represent a given institution and a department within it. It also shows how aliases can be defined for both levels.

#### Home Institutes
You can also define the configured org-units to be considered as a "home institute":

    <config:org-unit name="Lilliput University" home-institute="true" />
    
This feature exists to allow you to influence how address data will be matched to Elements groups through configuration. By default only the top level *Organisation* group (id = 1) is allowed to match to data present in the "*institution*" field of an address. Lower level (non-organisation) groups can only match to the "department" field of the address - and even then only if the institution field also matches to a group.

This is done on the assumption that for address data like:

* Department: "*Department of Physics*", Institution: "*Lilliput University*".

you only want to have "*Department of Physics*" matched to your Elements group structure if you really are "Lilliput University". 

The "home-institute" attribute allows you to mark various names as being valid for matching to Elements groups when that name appears in the institution field. This allows you to widen the scope of the matching so that addresses like:

* Institution: "*Department of Physics*".

can be matched, this obviously comes at the risk of getting unintentional matches - so it should be used with care. ***Please note**: This feature is very experimental and may well have unintended side effects...*

***Note**: This concept could possibly be extended/adapted/reworked if the crosswalks were upgraded to process the "institutional-appointments" field from the Elements profile, on the assumption that any data present there ought to represent an appointment within the current institution.*

## Additional Tools
In addition to the reworked harvester code and crosswalks, this branch also contains some files that can be used to extract information about the current state of Vivo and help analyse it to try and boot-strap the org-unit configuration.

* org-sparql.txt
* elements-harvester/example-scripts/example-elements/org-name-processing.xsl

The former is a Sparql query that can be used to extract information about the current organisations in Vivo and the latter an XSL file designed to assist with de-duplicating the output from this query (using similar matching techniques to the crosswalks). The xsl file sits alongside the crosswalking scripts so that it can easily leverage the utility functions and matching configuration defined there. 
*Please note, running the processing xsl file can take a long time (e.g. > 20 minutes even for relatively small sample data sets).*

Note that this process clearly cannot help with assigning aliases (to deal with missed valid matches) nor will it help in determining the appropriate canonical version of the institution's name, they merely represent a starting point.

***Please note**: these files are provided as is at the point development on this branch stopped, simply with some anonymization applied. No guidance on using them is provided here.*
