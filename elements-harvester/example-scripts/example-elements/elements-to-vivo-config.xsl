<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ *******************************************************************************
  ~   Copyright (c) 2017 Symplectic. All rights reserved.
  ~   This Source Code Form is subject to the terms of the Mozilla Public
  ~   License, v. 2.0. If a copy of the MPL was not distributed with this
  ~   file, You can obtain one at http://mozilla.org/MPL/2.0/.
  ~ *******************************************************************************
  ~   Version :  ${git.branch}:${git.commit.id}
  ~ *******************************************************************************
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
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                exclude-result-prefixes="rdf rdfs bibo vivo config foaf score ufVivo vitro api svfn symp xs"
        >


    <xsl:param name="configFile">elements-to-vivo-config.xml</xsl:param>
    <!-- where the framework should pass in the extra data when useRawDataFiles is set to false -->
    <xsl:param name="extraObjects"></xsl:param>

    <xsl:variable name="loaded-config" select="document($configFile)//config:main" />

    <!-- The base URI you are using for VIVO identifiers : passed in from java framework -->
    <xsl:param name="baseURI">
        <xsl:value-of select="$loaded-config/config:defaultbaseURI" />
    </xsl:param>

    <xsl:variable name="validatedBaseURI">
        <xsl:variable name="baseUriValidation" select="fn:resolve-uri('', fn:normalize-space($baseURI))" />
        <xsl:choose>
            <xsl:when test="ends-with($baseUriValidation, '/')"><xsl:value-of select="$baseUriValidation" /></xsl:when>
            <xsl:otherwise><xsl:value-of select="concat($baseUriValidation, '/')" /></xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <!-- Harvested by statement for the URI (set to blank if not required) -->
    <xsl:variable name="harvestedBy" select="fn:normalize-space($loaded-config/config:harvestedBy)" />

    <!-- the internal class that internal users and groups should be flagged as -->
    <xsl:param name="internalClass">
        <xsl:value-of select="fn:normalize-space($loaded-config/config:internalClass)" />
    </xsl:param>

    <!-- whether to create an organisation object at the "department level" when creating objects to represent addresses -->
    <xsl:variable name="includeDept" select="fn:normalize-space($loaded-config/config:includeDept)" />

    <!-- whether to use the Symplectic namespace extensions - deprecated -->
    <xsl:variable name="useSympNS" select="fn:normalize-space($loaded-config/config:useSympNS)" />

    <!-- DO NOT TOUCH! extra object params : i.e. how relationship templates that need the raw data about the objects in the link access that data -->
    <!-- whether to use raw files from disk to build the "extraObjects" or not -->
    <xsl:param name="useRawDataFiles">false</xsl:param>

    <!-- where the raw data should be retrieved from if useRawDataFiles is true-->
    <xsl:param name="recordDir">data/raw-records/</xsl:param>

    <!-- DO NOT TOUCH: Read the organization types, record and journal precedence configurations and label scheme config into variables for processing -->
    <!-- Read the publication types XML configuration file -->
    <xsl:variable name="publication-types" select="$loaded-config/config:publication-types" />
    <xsl:variable name="organization-types" select="$loaded-config/config:organization-types" />
    <xsl:variable name="country-types" select="$loaded-config/config:country-types" />
    <xsl:variable name="record-precedences" select="$loaded-config/config:record-precedences/config:record-precedences" />
    <xsl:variable name="data-exclusions" select="$loaded-config/config:data-exclusions/config:data-exclusions" />
    <xsl:variable name="journal-precedence" select="$loaded-config/config:journal-precedences" />
    <xsl:variable name="label-schemes" select="$loaded-config/config:label-schemes/config:label-schemes" />
    <xsl:variable name="label-overrides" select="$loaded-config/config:label-overrides/config:label-override" />
    <xsl:variable name="htmlProperties" select="$loaded-config/config:htmlProperties/config:htmlProperty" />
    <xsl:variable name="uriAliasses" select="$loaded-config/config:uriAliasses/config:uri" />

    <!-- testing tool for config -->
    <!--<xsl:template match="/">-->
        <!--<root>-->
            <!--<xsl:if test="$internalClass!=''"><proof><xsl:value-of select="$internalClass" /></proof></xsl:if>-->



            <!--<baseURI><xsl:value-of select="$baseURI" /></baseURI>-->
            <!--<harvestedBy><xsl:value-of select="$harvestedBy" /></harvestedBy>-->
            <!--<internalClass><xsl:value-of select="$internalClass" /></internalClass>-->
            <!--<includeDept><xsl:value-of select="$includeDept" /></includeDept>-->
            <!--<useSympNS><xsl:value-of select="$useSympNS" /></useSympNS>-->
            <!--<useRawDataFiles><xsl:value-of select="$useRawDataFiles" /></useRawDataFiles>-->
            <!--<recordDir><xsl:value-of select="$recordDir" /></recordDir>-->
            <!--<xsl:copy-of select="$publication-types" />-->
            <!--<xsl:copy-of select="$organization-types" />-->
            <!--<xsl:copy-of select="$record-precedences" />-->
            <!--<xsl:copy-of select="$data-exclusions" />-->
            <!--<xsl:copy-of select="$journal-precedence" />-->
            <!--<xsl:copy-of select="$label-schemes" />-->
        <!--</root>-->
    <!--</xsl:template>-->

</xsl:stylesheet>
