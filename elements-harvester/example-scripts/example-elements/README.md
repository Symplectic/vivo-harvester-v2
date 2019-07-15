# Default Crosswalks (VIVO 1.6+)

This directory contains the default XSLT crosswalks to map from Elements API XML to VIVO compatible RDF-XML (ISF Ontology). 
These example crosswalks contain transformations for Elements *Users*, *Publications*, *Grants* and a small subset 
of *Professional* and *Teaching Activity* object types.
In addition they contain mappings for Elements *group* and *group membership* information.

***Note:** These default crosswalks  are not meant to represent a “complete” or “perfect” mapping in any way but rather
 a good starting point. They hopefully serve as a useful illustration/framework, as well as providing a set of tools and
 utilities to develop from.*

## Structure
The main entry point is the file ***elements-to-vivo.xsl***. 
Beneath this the crosswalks are broken out into multiple files containing templates/functions that handle the translation
 of different concepts/types of input data. the basic structure (in terms of imports/includes) is indicated below:

  * elements-to-vivo.xsl *(Main entry point)*
    * elements-to-vivo-config.xsl
      * elements-to-vivo-config.xml *(Main crosswalk configuration file)*
    * elements-to-vivo-object.xsl *(Translation of Elements "Object" types)*
      * elements-to-vivo-object-user.xsl
        * elements-to-vivo-object-user-vcard.xsl
      * elements-to-vivo-object-publication.xsl
      * elements-to-vivo-object-grant.xsl
      * elements-to-vivo-object-professional-activity.xsl
      * elements-to-vivo-object-teaching-activity.xsl
    * elements-to-vivo-user-photo.xsl *(Special translation of processed photo information)*
    * elements-to-vivo-relationship.xsl *(Translation of Elements "Relationship" types)*
      * elements-to-vivo-relationship-publication-author.xsl
      * elements-to-vivo-relationship-publication-editor.xsl
      * elements-to-vivo-relationship-publication-translator.xsl
      * elements-to-vivo-relationship-publication-grant.xsl
      * elements-to-vivo-relationship-user-grant.xsl
      * elements-to-vivo-relationship-collaborator.xsl
      * elements-to-vivo-relationship-professional-activity.xsl
      * elements-to-vivo-relationship-professional-activity-committee-membership.xsl
      * elements-to-vivo-relationship-professional-activity-distinction.xsl
      * elements-to-vivo-relationship-professional-activity-event-administration.xsl
      * elements-to-vivo-relationship-professional-activity-event-participation.xsl
      * elements-to-vivo-relationship-professional-activity-fellowship.xsl
      * elements-to-vivo-relationship-professional-activity-membership.xsl
      * elements-to-vivo-relationship-teaching-activity.xsl
      * elements-to-vivo-relationship-teaching-activity-course-developed.xsl
      * Elements-to-vivo-relationship-teaching-activity-course-taught.xsl
    * elements-to-vivo-group.xsl *(Translation of Elements "Group" information)*
    * elements-to-vivo-group-membership.xsl *(Translation of Elements "Group Membership" information)*
    * elements-to-vivo-utils.xsl *(General utility functions for mapping Elements data to Vivo RDF-XML)*
      * elements-to-vivo-datatypes.xsl
        * elements-to-vivo-utils-2.xsl *(broken out to avoid cyclic import chains)*
      * elements-to-vivo-datatypes-matching.xsl
      * elements-to-vivo-fuzzy-matching.xsl
    * elements-to-vivo-template-overrides.xsl *(Override files to allow overriding of templates/functions)*
    * elements-to-vivo-util-overrides.xsl

For the most part the primary input, fed to the crosswalks by the Harvester, will be a single Elements "item" e.g an Object, 
A relationship, etc in the Elements API XML representation. In several cases, however, the Harvester provides additional data
to inform/alter the translation via XSL parameters.

Please see the _Crosswalk Development Guide_ in the release documentation for more information about how the crosswalks
 and the harvester interact.

## Notes on "VCard" Context Objects
When creating "context" objects (e.g. authorships/editorships/roles), these crosswalks make use of "VCard" objects
to represent people that do not have a profile in Vivo (e.g. any co-authors of an academic paper from another institution).

* (_vcard<--authorship-->publication_) **Vs** (_user<--authorship-->publication_)

This is in line with what Vivo expects :

  * https://wiki.duraspace.org/display/VTDA/The+W3C+vCard+ontology+in+VIVO
    
In fact, these crosswalks deliberately create a VCard linked to the context object regardless of whether there is also 
a link to an actual Vivo user:

  * (_vcard<--authorship-->publication_) **Vs** (_user + vcard <--authorship-->publication_)
                                         
This is done to ensure that all authors/editors/etc are listed even if a user's relationship with a publication is marked
as "hidden". It also allows for situations where the published name, as listed on the paper, does not match the user's 
name as it appears on their profile.
 
Unfortunately Vivo's out of the box support for "VCard" objects in context objects is not as complete as it might be,
for example it does not list "VCard's" in "editorship" objects at all. Additionally not all aspects of Vivo cope well
with context objects containing links to both a user object and an equivalent Vcard representation of the linked user.  
These issues mostly relate to how various "listViewConfigs" process data:

* **listViewConfig-informationResourceInEditorship.xml** does not handle "VCard" data at all. 
* **listViewConfig-informationResourceInAuthorship.xml** will always list the user's "label" rather than the VCard name (hiding the published name if different)
* **listViewConfig-relatedRole.xml** behaviour is buggy and inconsistent in terms of which names gets listed and which profiles get linked.

These problems can be addressed in a variety of ways, one way is to customise the listViewConfigs appropriately:
See the "example-integrations/vivo-list-view-configs" directory for some examples for Vivo v1.9.3.

## Notes on Controlled Vocabularies  
These crosswalks contain mappings for labels in the MeSH, Science Metrix and Field of Research schemes. In order to display correctly, you need to add the contents of "add-to-vocabularySource.n3" to the file
 
    <vivo>/home/rdf/abox/filegraph/vocabularySource.n3

You will need to add similar lines for any "custom" label schemes where you want the "Vocabulary Service" name to be listed in VIVO.