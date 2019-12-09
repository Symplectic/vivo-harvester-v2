<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ *******************************************************************************
  ~   Copyright (c) 2019 Symplectic. All rights reserved.
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
                xmlns:vcard="http://www.w3.org/2006/vcard/ns#"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:score="http://vivoweb.org/ontology/score#"
                xmlns:ufVivo="http://vivo.ufl.edu/ontology/vivo-ufl/"
                xmlns:vitro="http://vitro.mannlib.cornell.edu/ns/vitro/0.7#"
                xmlns:api="http://www.symplectic.co.uk/publications/api"
                xmlns:symp="http://www.symplectic.co.uk/vivo/"
                xmlns:svfn="http://www.symplectic.co.uk/vivo/namespaces/functions"
                xmlns:config="http://www.symplectic.co.uk/vivo/namespaces/config"
                xmlns:obo="http://purl.obolibrary.org/obo/"
                exclude-result-prefixes="xsl xs rdf rdfs bibo vivo vcard foaf score ufVivo vitro api symp svfn config obo"
        >

    <xsl:import href="elements-to-vivo-utils.xsl" />

    <!-- Match Elements objects of category 'grant' -->
    <xsl:template match="api:object[@category='grant']">
        <xsl:variable name="grantURI"><xsl:value-of select="svfn:objectURI(.)" /></xsl:variable>
        <xsl:variable name="funderNameField" select="svfn:getRecordField(.,'funder-name')" />
        <!-- funders will not be groups -->
        <xsl:variable name="funderInfo" select="svfn:getOrgInfoFromName($funderNameField/api:text, $organization-overrides, false())" />
        <xsl:variable name="funderURI"><xsl:if test="$funderInfo/@name"><xsl:value-of select="svfn:makeURI('funder-',$funderInfo/@name)" /></xsl:if></xsl:variable>

        <!-- render datetime interval to intermediate variable, retrieve uri for reference purposes and then render variable contents-->
        <xsl:variable name="startDate" select="svfn:getRecordField(.,'start-date')" />
        <xsl:variable name="endDate" select="svfn:getRecordField(.,'end-date')" />
        <xsl:variable name="dateInterval" select ="svfn:renderDateInterval($grantURI, $startDate, $endDate, '', false())" />
        <xsl:variable name="dateIntervalURI" select="svfn:retrieveDateIntervalUri($dateInterval)" />
        <xsl:copy-of select="$dateInterval" />

        <xsl:variable name="defaultGrantTitle" select="concat('Default title for grant:', @id)" />

        <xsl:call-template name="render_rdf_object">
            <xsl:with-param name="objectURI" select="$grantURI" />
            <xsl:with-param name="rdfNodes">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Grant"/>
                <xsl:copy-of select="svfn:renderPropertyFromFieldOrFirst(.,'rdfs:label','title', $defaultGrantTitle)" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:abstract','abstract')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:abstract','description')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'vivo:sponsorAwardId','funder-reference')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'vivo:localAwardId','institution-reference')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'vivo:totalAwardAmount','amount')" />
                <xsl:if test="$dateInterval/*">
                    <vivo:dateTimeInterval rdf:resource="{$dateIntervalURI}" />
                </xsl:if>
                <xsl:if test="$funderInfo">
                    <vivo:assignedBy rdf:resource="{$funderURI}" />
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>

        <xsl:if test="$funderInfo">
            <xsl:variable name="funderType">
                <xsl:choose>
                    <xsl:when test="$funderInfo/config:org-unit/@type"><xsl:value-of select="$funderInfo/config:org-unit/@type" /></xsl:when>
                    <xsl:otherwise><xsl:value-of select="svfn:inferOrganizationType($funderInfo/@name, 'http://vivoweb.org/ontology/core#FundingOrganization')" /></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$funderURI" />
                <xsl:with-param name="rdfNodes">
                    <rdf:type rdf:resource="{$funderType}"/>
                    <!-- rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Thing"/ -->
                    <rdfs:label><xsl:value-of select="$funderInfo/@name"/></rdfs:label>
                    <vivo:assigns rdf:resource="{$grantURI}"/>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
