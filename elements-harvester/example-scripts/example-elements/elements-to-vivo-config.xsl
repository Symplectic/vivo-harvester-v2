<?xml version="1.0" encoding="UTF-8"?>
<!--
 | Copyright (c) 2012 Symplectic Limited. All rights reserved.
 | This Source Code Form is subject to the terms of the Mozilla Public
 | License, v. 2.0. If a copy of the MPL was not distributed with this
 | file, You can obtain one at http://mozilla.org/MPL/2.0/.
 -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:bibo="http://purl.org/ontology/bibo/"
                xmlns:vivo="http://vivoweb.org/ontology/core#"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:score="http://vivoweb.org/ontology/score#"
                xmlns:ufVivo="http://vivo.ufl.edu/ontology/vivo-ufl/"
                xmlns:vitro="http://vitro.mannlib.cornell.edu/ns/vitro/0.7#"
                xmlns:api="http://www.symplectic.co.uk/publications/api"
                xmlns:symp="http://www.symplectic.co.uk/ontology/elements/"
                xmlns:svfn="http://www.symplectic.co.uk/vivo/namespaces/functions"
                xmlns:config="http://www.symplectic.co.uk/vivo/namespaces/config"
                exclude-result-prefixes="rdf rdfs bibo vivo config foaf score ufVivo vitro api svfn symp xs"
        >

    <!-- The base URI you are using for VIVO identifiers : passed in from java framework -->
    <xsl:param name="baseURI">http://localhost:8080/vivo/individual/</xsl:param>

    <!-- Harvested by statement for the URI (set to blank if not required) -->
    <xsl:param name="harvestedBy" />

    <!-- the internal class that internal users and groups should be flagged as -->
    <xsl:param name="internalClass" />

    <!-- whether to create an organisation object at the "department level" when creating objects to represent addresses -->
    <xsl:param name="includeDept">true</xsl:param>

    <!-- whether to use the Symplectic namespace extensions - deprecated -->
    <xsl:param name="useSympNS" />

    <!-- DO NOT TOUCH! extra object params : i.e. how relationship templates that need the raw data about the objects in the link access that data -->
    <xsl:param name="useRawDataFiles">false</xsl:param> <!-- whether to use raw files from disk to build the "extraObjects" or not -->
    <xsl:param name="recordDir">data/raw-records/</xsl:param> <!-- where the raw data should be retrieved from if useRawDataFiles is true-->
    <xsl:param name="extraObjects"></xsl:param> <!-- where the framework should pass in the extra data if useRawDataFiles is set to false -->

    <!-- DO NOT TOUCH: Read the organization types, record and journal precedence configurations and label scheme config into variables for processing -->
    <!-- Read the publication types XML configuration file -->
    <xsl:variable name="publication-types" select="document('elements-to-vivo-config-publication-types.xml')//config:publication-types" />
    <xsl:variable name="organization-types" select="document('elements-to-vivo-config-organization-types.xml')//config:organization-types" />
    <xsl:variable name="record-precedences" select="document('')//config:record-precedences/config:record-precedences" />
    <xsl:variable name="data-exclusions" select="document('')//config:data-exclusions/config:data-exclusions" />
    <xsl:variable name="journal-precedence" select="document('')//config:journal-precedences" />
    <xsl:variable name="label-schemes" select="document('')//config:label-schemes/config:label-schemes" />

    <!--
        Configure precedence for records
        ================================

        The "for" attribute determines which object type that set of precedences applies to. "default" is used for
        objects where the type does not have it's own configuration.

        Use select-by="field" attribute to choose the field from the highest precedence record in which it occurs.

        Otherwise, it will select the highest precedence record that is present, regardless of whether the desired field exists in that record.

        If a record from a source not listed is present, it will generally still be used (if non of those listed are present) unless it is listed in the config:data-exclusions section.
        to disable this set use-unlisted-sources=false.

        Note that data in records being excluded (either by settings use-unlisted-source=false or by using the config:data-exclusions)
        could still be used if the "fallback to first record" functions are used in the mappings.
    -->

    <config:record-precedences>
        <config:record-precedences for="publication" select-by="field" use-unlisted-sources="true">
            <config:record-precedence>pubmed</config:record-precedence>
            <config:record-precedence>epmc</config:record-precedence>
            <config:record-precedence>crossref</config:record-precedence>
            <config:record-precedence>scopus</config:record-precedence>
            <config:record-precedence>repec</config:record-precedence>
            <config:record-precedence>arxiv</config:record-precedence>
            <config:record-precedence>orcid</config:record-precedence>
            <config:record-precedence>dblp</config:record-precedence>
            <config:record-precedence>figshare</config:record-precedence>
            <config:record-precedence>manual</config:record-precedence>
            <config:record-precedence>c-inst-1</config:record-precedence>
            <config:record-precedence>c-inst-2</config:record-precedence>
        </config:record-precedences>
        <config:record-precedences for="grant" select-by="field" use-unlisted-sources="true">
            <config:record-precedence>dimensions</config:record-precedence>
            <config:record-precedence>source-3</config:record-precedence>
            <config:record-precedence>manual</config:record-precedence>
            <config:record-precedence>c-inst-1</config:record-precedence>
            <config:record-precedence>c-inst-2</config:record-precedence>
        </config:record-precedences>
        <config:record-precedences for="default" select-by="field" use-unlisted-sources="true">
            <config:record-precedence>manual</config:record-precedence>
            <config:record-precedence>c-inst-1</config:record-precedence>
            <config:record-precedence>c-inst-2</config:record-precedence>
        </config:record-precedences>
    </config:record-precedences>

    <!--
        The "for" attribute determines which object type a set of exclusions applies to (note "default" does not apply here)
        <record-exclusion> elements mean no data from that record in the elements value will be prevented from being transferred to vivo.
        <field-exclusions> elements apply to the record listed in the @for-source attribute their effect is that
                           data from the fields listed as "config:excluded-field" child elements will be prevented from being transferred to vivo.
        Note that these exclusions can be ignored if the "fallback to first record" functions are used in the mappings

    -->
    <config:data-exclusions>
        <config:data-exclusions for="publication">
            <config:record-exclusion>wos</config:record-exclusion>
            <config:field-exclusions for-source="scopus">
                <config:excluded-field>abstract</config:excluded-field>
            </config:field-exclusions>
        </config:data-exclusions>
    </config:data-exclusions>

    <!--
        Configure precedence for retrieving journal names
        =================================================

        The journal name will be extracted from the authority data by preference and if that leads to nothing then from the record data.
        The order in which authorities should be preferred us defined in the config:journal-authority-precedences section below.
        By default if there is data from an authority not present in the list it will be used in preference to the record data
        To disable this set use-unlisted-source="false" on the config:journal-authority-precedences element

        If no title is extracted based on the authority data then the raw record data will be inspected.
        The order in which records should be preferred for this task is defined in the config:journal-record-precedences section below.
        the attribute "field" on each config:journal-record-precedence specified the record field to use (defaults to "journal").

        By default if the data contains a record present from a source not listed below it may still be used as if no journal title
        is found in any of the sources listed below then the system falls back to extracting any "journal" field value according to the
        general record-precedences defined above.
        To disable this set use-unlisted-source="false" on the config:journal-record-precedences element

    -->
    <config:journal-precedences>
        <config:journal-authority-precedences use-unlisted-sources='true'>
            <config:journal-authority-precedence>era2012</config:journal-authority-precedence>
            <config:journal-authority-precedence>snip</config:journal-authority-precedence>s
            <config:journal-authority-precedence>era2010</config:journal-authority-precedence>
            <config:journal-authority-precedence>science-metrix</config:journal-authority-precedence>
            <config:journal-authority-precedence>sjr</config:journal-authority-precedence>
            <config:journal-authority-precedence>jcr</config:journal-authority-precedence>
            <config:journal-authority-precedence>institutional-source</config:journal-authority-precedence>
            <config:journal-authority-precedence>sherpa-romeo</config:journal-authority-precedence>
            <config:journal-authority-precedence>doaj</config:journal-authority-precedence>
        </config:journal-authority-precedences>
        <config:journal-record-precedences use-unlisted-sources='true'>
            <config:journal-record-precedence field="journal">pubmed</config:journal-record-precedence>
            <config:journal-record-precedence field="journal">manual</config:journal-record-precedence>
            <config:journal-record-precedence field="journal">arxiv</config:journal-record-precedence>
        </config:journal-record-precedences>
    </config:journal-precedences>

    <!--
        To use Labels, as well as having the schemes defined below, you must add data to vocabularySource.n3 in vivo/home/rdf/abox/filegraph

        For example for Mesh, ScienceMetrix and For label schemes you need to add this (note how the "defined-by" attribute from the label-schemes defs below is being used).

        <http://www.nlm.nih.gov/mesh> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Thing> .
        <http://www.nlm.nih.gov/mesh> <http://www.w3.org/2000/01/rdf-schema#label> "MeSH"^^<http://www.w3.org/2001/XMLSchema#string>  .

        <http://www.science-metrix.com/> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Thing> .
        <http://www.science-metrix.com/> <http://www.w3.org/2000/01/rdf-schema#label> "Science Metrix"^^<http://www.w3.org/2001/XMLSchema#string>  .

        <http://www.arc.gov.au/era/for> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Thing> .
        <http://www.arc.gov.au/era/for> <http://www.w3.org/2000/01/rdf-schema#label> "FoR"^^<http://www.w3.org/2001/XMLSchema#string>  .
    -->

    <config:label-schemes>
        <config:label-schemes for="publication">
            <config:label-scheme name="mesh" defined-by="http://www.nlm.nih.gov/mesh" />
            <config:label-scheme name="science-metrix" defined-by="http://www.science-metrix.com/" />
            <config:label-scheme name="for" defined-by="http://www.arc.gov.au/era/for" />
            <!--<config:label-scheme name="c-seo-post-2008" defined-by="{baseURI}c-seo-post-2008"/>-->
        </config:label-schemes>
        <config:label-schemes for="user">
            <config:label-scheme name="mesh" defined-by="http://www.nlm.nih.gov/mesh" />
            <config:label-scheme name="science-metrix" defined-by="http://www.science-metrix.com/"/>
            <config:label-scheme name="for" defined-by="http://www.arc.gov.au/era/for"/>
            <!--<config:label-scheme name="c-availability" defined-by="{baseURI}c-availability"/>-->
            <!--<config:label-scheme name="c-seo-post-2008" defined-by="{baseURI}c-seo-post-2008"/>-->
        </config:label-schemes>
    </config:label-schemes>

</xsl:stylesheet>
