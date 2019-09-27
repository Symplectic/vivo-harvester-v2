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
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:score="http://vivoweb.org/ontology/score#"
                xmlns:ufVivo="http://vivo.ufl.edu/ontology/vivo-ufl/"
                xmlns:vcard="http://www.w3.org/2006/vcard/ns#"
                xmlns:vitro="http://vitro.mannlib.cornell.edu/ns/vitro/0.7#"
                xmlns:api="http://www.symplectic.co.uk/publications/api"
                xmlns:symp="http://www.symplectic.co.uk/vivo/"
                xmlns:svfn="http://www.symplectic.co.uk/vivo/namespaces/functions"
                xmlns:config="http://www.symplectic.co.uk/vivo/namespaces/config"
                xmlns:obo="http://purl.obolibrary.org/obo/"
                exclude-result-prefixes="rdf rdfs bibo vivo foaf score ufVivo vitro api obo symp svfn config xs"
        >

    <!-- Import XSLT files that are used -->
    <xsl:import href="elements-to-vivo-utils.xsl" />

    <xsl:template match="api:relationship[@type='activity-user-association' and api:related/api:object[@category='activity' and @type='event-administration']]" mode="visible-relationship">
        <xsl:variable name="contextURI" select="svfn:relationshipURI(.,'relationship')" />

        <xsl:variable name="activityObj" select="svfn:fullObject(api:related/api:object[@category='activity'])" />
        <xsl:variable name="userObj" select="svfn:fullObject(api:related/api:object[@category='user'])" />

        <xsl:variable name="eventName" select="svfn:getRecordField($activityObj,'description')" />
        <xsl:if test="$eventName/api:text">
            <xsl:variable name="eventURI"><xsl:value-of select="svfn:makeURI('event-',$eventName/api:text)" /></xsl:variable>

            <xsl:variable name="userURI" select="svfn:userURI($userObj)" />

            <!-- An Event -->
            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$eventURI" />
                <xsl:with-param name="rdfNodes">
                    <!-- TODO Map event-type to rdf:type -->
                    <rdf:type rdf:resource="http://purl.org/NET/c4dm/event.owl#Event"/>
                    <xsl:copy-of select="svfn:renderPropertyFromField($activityObj,'rdfs:label','description')" />
                    <obo:BFO_0000055 rdf:resource="{$contextURI}"/><!-- Context object -->
                </xsl:with-param>
            </xsl:call-template>

            <!-- Context Object -->
            <xsl:variable name="startDate" select="svfn:getRecordField($activityObj,'start-date')" />
            <xsl:variable name="endDate" select="svfn:getRecordField($activityObj,'end-date')" />
            <!-- render datetime interval to intermediate variable, retrieve uri for reference purposes and then render variable contents-->
            <xsl:variable name="dateInterval" select ="svfn:renderDateInterval($contextURI, $startDate, $endDate, '', false())" />
            <xsl:variable name="dateIntervalURI" select="svfn:retrieveDateIntervalUri($dateInterval)" />
            <xsl:copy-of select="$dateInterval" />

            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$contextURI" />
                <xsl:with-param name="rdfNodes">
                    <rdf:type rdf:resource="http://vivoweb.org/ontology/core#OrganizerRole"/>
                    <xsl:copy-of select="svfn:renderPropertyFromField($activityObj,'rdfs:label','administrative-role')" />
                    <obo:BFO_0000054 rdf:resource="{$eventURI}" />
                    <obo:RO_0000052 rdf:resource="{$userURI}"/><!-- User -->
                    <xsl:if test="$dateInterval/*">
                        <vivo:dateTimeInterval rdf:resource="{$dateIntervalURI}"/><!-- Years Inclusive -->
                    </xsl:if>
                </xsl:with-param>
            </xsl:call-template>

            <!-- Relate user to context-->
            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$userURI" />
                <xsl:with-param name="rdfNodes">
                    <obo:RO_0000053 rdf:resource="{$contextURI}"/>
                </xsl:with-param>
            </xsl:call-template>

        </xsl:if>
    </xsl:template>
</xsl:stylesheet>