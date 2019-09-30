# Issues with VCard context objects

When creating "context" objects (e.g. authorships/editorships/roles), the default crosswalks make use of "VCard" objects
to represent people that do not have a profile in Vivo (e.g. any co-authors of an academic paper from another institution).

* (_vcard<--authorship-->publication_) **Vs** (_user<--authorship-->publication_)

This is in line with what Vivo expects :

  * https://wiki.duraspace.org/display/VTDA/The+W3C+vCard+ontology+in+VIVO
    
In fact, the default crosswalks deliberately create a VCard connected to the context object regardless of whether 
there is an also a link to an actual Vivo user:

  * (_vcard<--authorship-->publication_) **Vs** (_user + vcard <--authorship-->publication_)
                                         
This is done to ensure that all authors/editors/etc are listed even if a user's relationship with a publication is marked
as "hidden". It also allows for situations where the published name, as listed on the paper, does not match the user's 
name as it appears on their profile.
 
Vivo does not offer complete out of the box support for "VCards" in context objects, as there are issues relating to how various "listViewConfigs" process VCard data.  
For example, it does not list "VCard's" in "editorship" objects at all, nor does it cope well with context objects containing links to both a user object and an equivalent Vcard representation of the associated user.  


* **listViewConfig-informationResourceInEditorship.xml** does not handle "VCard" data at all. 
* **listViewConfig-informationResourceInAuthorship.xml** will always list the user's "label" rather than the VCard name 
    (hiding the published name if different)
* **listViewConfig-relatedRole.xml** behaviour is buggy and inconsistent in terms of which names gets listed and which 
    profiles get linked.

## Examples for Vivo 1.9.3
This directory contains both the _original_ version and a suggested _updated_ version of the 3 files listed above in the
respective folders. For the "listViewConfig-informationResourceInEditorship.xml" the corresponding _freemarker_ file
 "propStatement-informationResourceInEditorship.ftl" also needs updating, so that is also present.  

_**Note**: The updated files are designed to always use the name from the VCard object even if there is also a user object
 connected to the authorship, they should therefore present the author's name as it was "published"._
 
To make use of the updated versions the listViewConfig xml files should be copied into the "config" directory of the live Vivo 
webapp running in its Java servlet container (e.g. Tomcat). The freemarker (.ftl) file meanwhile should just be added to
your theme's "templates" directory. 
