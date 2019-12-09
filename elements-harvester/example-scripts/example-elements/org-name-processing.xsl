<?xml version="1.0" encoding="UTF-8"?>

<!--
  ~ *******************************************************************************
  ~   Copyright (c) 2017 Symplectic. All rights reserved.
  ~   This Source Code Form is subject to the terms of the Mozilla Public
  ~   License, v. 2.0. If a copy of the MPL was not distributed with this
  ~   file, You can obtain one at http://mozilla.org/MPL/2.0/.
  ~ *******************************************************************************
  ~   Version :  develop:8dc2808ad07dd084949ada36af5ef6e022effe33
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
                xmlns:config="http://www.symplectic.co.uk/vivo/namespaces/config"
                xmlns:svfn="http://www.symplectic.co.uk/vivo/namespaces/functions"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:sxl="http://www.w3.org/1999/XSL/Transform" xmlns:csl="http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes="rdf rdfs bibo vivo foaf score ufVivo vitro api symp xs"
>

    <!--
        Import general utility and configuration XSLT files
    -->
    <xsl:import href="elements-to-vivo-config.xsl" />
    <xsl:import href="elements-to-vivo-datatypes.xsl" />
    <xsl:import href="elements-to-vivo-datatypes-matching.xsl" />
    <xsl:import href="elements-to-vivo-utils.xsl" />
    <!--
        Import XSLT for object and relationship transformations
    -->

    <!--
        Default template - matches the root, to output an RDF document tag around any RDF objects that are output
    -->
    <xsl:variable name="fuzzyMatchLimit" select="0.85" />

    <xsl:template match="/">
        <xsl:variable name="orgOverrideSet">
        <org-names>
            <xsl:for-each select="$organization-overrides/config:org-unit">
                <org-name><xsl:value-of select="./@name" /></org-name>
            </xsl:for-each>
        </org-names>
        </xsl:variable>

        <!--<xsl:apply-templates select="$orgOverrideSet/org-names" />-->
        <!--<xsl:apply-templates select="org-names" mode="dedup"/>-->

        <!-- TYPICAL USAGE -->
        <xsl:apply-templates select="./*" />

        <!--<test>-->
            <!--<xsl:copy-of select="svfn:orgNameDifference('Ruhr University Bochum','Ruhr''University Bochum')" />-->
        <!--</test>-->

    </xsl:template>


    <xsl:template match="rdf:RDF">
        <xsl:variable name="processedNamesFromRDF">
            <org-names>
                <xsl:for-each select="rdf:Description">
                        <xsl:variable name="count" select="vivo:count" />
                    <xsl:for-each select="vivo:org-name">
                        <org-name count="{$count}"><xsl:value-of select="." /></org-name>
                    </xsl:for-each>
                </xsl:for-each>
            </org-names>
        </xsl:variable>

        <xsl:apply-templates select="$processedNamesFromRDF" />
    </xsl:template>

    <xsl:template match="org-names" mode="dedup">
        <xsl:variable name="names" select = "." />

        <test>
        <xsl:for-each select="distinct-values(./org-name/text())">
        <xsl:variable name="currentName" select="." />
        <name-with-count count="{count($names/org-name[text() = $currentName])}">
            <xsl:value-of select="$currentName" />
        </name-with-count>
        </xsl:for-each>
        </test>
    </xsl:template>

    <xsl:template match="org-names">

        <xsl:variable name="names" >
            <xsl:sequence select="./org-name" />
        </xsl:variable>

        <xsl:variable name="non-matches">
            <xsl:call-template name="getNonMatches">
                <xsl:with-param name="namesToTest" select="$names" />
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="namesToStrip">
            <xsl:for-each select="$non-matches/non-matched-org">
                <nameToStrip><xsl:value-of select="./@name" /></nameToStrip>
                <xsl:for-each select="./non-matched-org/non-matched-org-alias">
                    <nameToStrip><xsl:value-of select="./@name" /></nameToStrip>
                </xsl:for-each>
            </xsl:for-each>
        </xsl:variable>

        <xsl:variable name="reducedNamesToTest">
            <xsl:sequence select="$names/org-name[not(text() = $namesToStrip/nameToStrip/text())]" />
        </xsl:variable>

        <xsl:variable name="matches">
            <xsl:call-template name="getMatches">
                <xsl:with-param name="namesToTest" select="$reducedNamesToTest" />
            </xsl:call-template>
        </xsl:variable>

        <org-name-analysis>
            <xsl:copy-of select="$matches" />
            <xsl:comment>Unique orgs with no matches below here</xsl:comment>
            <xsl:copy-of select="$non-matches" />
        </org-name-analysis>
    </xsl:template>


    <xsl:template name="getNonMatches">
        <xsl:param name="namesToTest" />
        <xsl:variable name="orgName" select="$namesToTest/org-name[1]" />

        <xsl:for-each select="distinct-values($namesToTest/org-name/text())">
            <xsl:variable name="orgName" select="." />
            <xsl:variable name="candidatesToTest">
                <xsl:sequence select="$namesToTest/org-name[$orgName != text()]" />
            </xsl:variable>
            <!--<xsl:if test="not($candidatesToTest/org-name[svfn:orgNameDifference($orgName/text(), text()) &gt;= $fuzzyMatchLimit])">-->
            <xsl:if test="not($candidatesToTest/org-name[svfn:compareOrgsForDeDup($orgName, text())])">
                <non-matched-org name="{$orgName}">
                    <!--<matched-org-alias name="{$orgName}" difference="1"/>-->
                </non-matched-org>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="getMatches">
        <xsl:param name="namesToTest" />

        <xsl:variable name="orgName" select="$namesToTest/org-name[1]" />

        <xsl:variable name="candidatesToTest">
            <xsl:sequence select="$namesToTest/org-name[$orgName/text() != text()]" />
        </xsl:variable>

        <xsl:variable name="matchedOrgs">
            <matched-org name="{$orgName/text()}">
                <xsl:variable name="matches">
                    <matched-org-alias name="{$orgName/text()}" difference="1" count=""/>
                    <!--<matched-org-alias name="{$orgName}" difference="1"/>-->
                    <xsl:for-each select="distinct-values($candidatesToTest/org-name/text())">
                        <xsl:variable name="compOrgName" select="." />
                        <xsl:variable name="difference" select="svfn:orgNameDifference($orgName/text(), $compOrgName)" />
                        <!--<xsl:variable name="difference" select="'N/A'" />-->
                        <xsl:if test="svfn:compareOrgsForDeDup($orgName/text(), $compOrgName)" >
                            <matched-org-alias name="{$compOrgName}" difference="{$difference}"/>
                        </xsl:if>
                    </xsl:for-each>
                </xsl:variable>

                <!--<xsl:call-template name="deDupeMatches">-->
                    <!--<xsl:with-param name="matches" select="$matches" />-->
                <!--</xsl:call-template>-->
                <xsl:copy-of select="$matches" />

            </matched-org>
        </xsl:variable>
        <xsl:copy-of select="$matchedOrgs" />

        <xsl:variable name="namesToStrip">
            <nameToStrip><xsl:value-of select="$matchedOrgs/matched-org/@name" /></nameToStrip>
            <xsl:for-each select="$matchedOrgs/matched-org/matched-org-alias">
                <nameToStrip><xsl:value-of select="./@name" /></nameToStrip>
            </xsl:for-each>
        </xsl:variable>

        <xsl:variable name="reducedNamesToTest">
            <xsl:sequence select="$namesToTest/org-name[not(text() = $namesToStrip/nameToStrip/text())]" />
        </xsl:variable>

        <xsl:if test="count($reducedNamesToTest/org-name) > 0">
            <xsl:call-template name="getMatches">
                <xsl:with-param name="namesToTest" select="$reducedNamesToTest" />
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:function name="svfn:compareOrgsForDeDup" as="xs:boolean">
        <xsl:param name="orgName" as="xs:string" />
        <xsl:param name="comparisonName" as="xs:string" />
        <xsl:value-of select="svfn:compareOrgNames(svfn:applyAddressDataEntryCorrections($orgName), svfn:applyAddressDataEntryCorrections($comparisonName))" />
    </xsl:function>

</xsl:stylesheet>

