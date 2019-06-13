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
                xmlns:obo="http://purl.obolibrary.org/obo/"
                xmlns:score="http://vivoweb.org/ontology/score#"
                xmlns:ufVivo="http://vivo.ufl.edu/ontology/vivo-ufl/"
                xmlns:vitro="http://vitro.mannlib.cornell.edu/ns/vitro/0.7#"
                xmlns:api="http://www.symplectic.co.uk/publications/api"
                xmlns:symp="http://www.symplectic.co.uk/vivo/"
                xmlns:svfn="http://www.symplectic.co.uk/vivo/namespaces/functions"
                xmlns:config="http://www.symplectic.co.uk/vivo/namespaces/config"
                exclude-result-prefixes="rdf rdfs bibo vivo foaf obo score ufVivo vitro api symp svfn config xs"
>

    <!-- Import XSLT files that are used -->
    <xsl:import href="elements-to-vivo-utils.xsl" />

    <!-- The "Vivo" type that represents a researcher role - defined here as a constant for comparisons elsewhere in this file
         comparisons to this are used to alter the translations, e.g. to decide whether a label should be provided, etc -->
    <xsl:variable name="vivo-researcher-role" select="'http://vivoweb.org/ontology/core#ResearcherRole'" />

    <!-- configuration of how different Elements grant-user relationship types should be treated -->
    <xsl:variable name="grant-relationship-types">
        <rel-types default-vivo-type="http://vivoweb.org/ontology/core#ResearcherRole">
            <rel-type vivo-type="http://vivoweb.org/ontology/core#PrincipalInvestigatorRole">user-grant-primary-investigation</rel-type>
            <rel-type vivo-type="http://vivoweb.org/ontology/core#PrincipalInvestigatorRole">user-grant-principal-investigation</rel-type>
            <rel-type vivo-type="http://vivoweb.org/ontology/core#CoPrincipalInvestigatorRole">user-grant-co-primary-investigation</rel-type>
            <rel-type vivo-type="http://vivoweb.org/ontology/core#CoPrincipalInvestigatorRole">user-grant-co-principal-investigation</rel-type>
            <rel-type vivo-type="http://vivoweb.org/ontology/core#CoPrincipalInvestigatorRole">user-grant-multi-pi</rel-type>
            <rel-type vivo-type="http://vivoweb.org/ontology/core#InvestigatorRole">user-grant-secondary-investigation</rel-type>
            <rel-type vivo-type="http://vivoweb.org/ontology/core#InvestigatorRole">user-grant-co-investigation</rel-type>
            <rel-type label="Funded by">grant-user-funding</rel-type>
            <rel-type label="Funded by">user-grant-sponsorship</rel-type>
            <rel-type label="Sub Project Principal Investigator">user-grant-primary-investigation-sub-project</rel-type>
            <rel-type label="Sub Project Investigator">user-grant-secondary-investigation-sub-project</rel-type>
            <rel-type label="Senior Personnel">user-grant-senior-key-personnel</rel-type>
            <rel-type label="Personnel">user-grant-personnel</rel-type>
            <rel-type label="Sub Project Co-Leader">user-grant-project-co-leadership</rel-type>
            <rel-type label="Site Principal Investigator">user-grant-site-pi-investigation</rel-type>
            <rel-type label="Site Investigator">user-grant-site-investigation</rel-type>
            <rel-type label="Consultant">user-grant-consulting</rel-type>
            <rel-type label="Collaborator">user-grant-collaboration</rel-type>
            <rel-type label="Clinical Evaluator">user-grant-clinical-evaluation</rel-type>
            <rel-type label="Mentor">user-grant-mentoring</rel-type>
            <rel-type label="Project Co-ordinator">user-grant-program-coordination</rel-type>
            <rel-type label="Project Leader/Director">user-grant-project-leadership</rel-type>
            <rel-type label="Project Leader/Director">user-grant-program-direction</rel-type>
            <rel-type label="Researcher">user-grant-research</rel-type>
            <rel-type label="Statistician">user-grant-statistics</rel-type>
            <rel-type>user-grant-other-contribution</rel-type>
        </rel-types>
    </xsl:variable>

    <!--
        Output as part of relationship - Supports publication
        <vivo:supportedInformationResource rdf:resource="http://vivo.mydomain.edu/individual/n4893"/>
    -->
    <xsl:template match="api:relationship[$grant-relationship-types/rel-types/rel-type[text() = current()/@type]]" mode="visible-relationship invisible-relationship">

        <xsl:variable name="selectedVivoGrantType" select="$grant-relationship-types/rel-types/rel-type[text() = current()/@type][1]" />

        <xsl:variable name="vivo-relationship-type">
            <xsl:choose>
                <xsl:when test="$selectedVivoGrantType/@vivo-type">
                    <xsl:value-of select = "$selectedVivoGrantType/@vivo-type" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$grant-relationship-types/rel-types/@default-vivo-type" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="investigatorURI" select="svfn:relationshipURI(.,'investigator')" />

        <!-- Get the user object reference from the relationship -->
        <xsl:variable name="user" select="api:related/api:object[@category='user']" />

        <!-- Get the user object reference from the relationship -->
        <xsl:variable name="grant" select="api:related/api:object[@category='grant']" />

        <!-- Create a Role -->
        <xsl:call-template name="render_rdf_object">
            <xsl:with-param name="objectURI" select="$investigatorURI" />
            <xsl:with-param name="rdfNodes">

                <rdf:type rdf:resource="{$vivo-relationship-type}" />
                <!-- we only apply labels if we are doing a researcher role and one is available -->
                <xsl:if test="$vivo-relationship-type = $vivo-researcher-role">
                    <xsl:choose>
                        <xsl:when test="$selectedVivoGrantType/@label">
                            <rdfs:label><xsl:value-of select="$selectedVivoGrantType/@label" /></rdfs:label>
                        </xsl:when>
                        <xsl:otherwise>
                            <rdfs:label><xsl:text>Other Contribution</xsl:text></rdfs:label>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>

                <vivo:relatedBy rdf:resource="{svfn:objectURI($grant)}"/><!-- link to grant -->
                <obo:RO_0000052 rdf:resource="{svfn:userURI($user)}"/><!-- link to user -->

                <!-- vivo:dateTimeInterval rdf:resource="http://vivo.mydomain.edu/individual/n6127" / - link to date / time -->
                <xsl:if test="api:is-visible='false'">
                    <vivo:hideFromDisplay>true</vivo:hideFromDisplay>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>

        <!-- Add a reference to the role object from the grant object -->
        <xsl:call-template name="render_rdf_object">
            <xsl:with-param name="objectURI" select="svfn:objectURI($grant)" />
            <xsl:with-param name="rdfNodes">
                <vivo:relates rdf:resource="{$investigatorURI}"/><!-- link to role -->
                <!-- if its an investigator role type we create an additional "relates" link directly to the user -->
                <!-- no idea why - - it's just what Vivo itself does if you do manual entry...-->
                <xsl:if test="$vivo-relationship-type != $vivo-researcher-role">
                    <vivo:relates rdf:resource="{svfn:userURI($user)}" /><!-- link to user -->
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>

        <!-- Add a reference to the role object from the user object -->
        <xsl:call-template name="render_rdf_object">
            <xsl:with-param name="objectURI" select="svfn:userURI($user)" />
            <xsl:with-param name="rdfNodes">
                <obo:RO_0000053 rdf:resource="{$investigatorURI}"/><!-- link to role -->
                <!-- if its an investigator role type we create an additional "related by" link to the grant -->
                <!-- no idea why - it's just what Vivo itself does if you do manual entry...-->
                <xsl:if test="$vivo-relationship-type != $vivo-researcher-role">
                    <vivo:relatedBy rdf:resource="{svfn:objectURI($grant)}" /><!-- link to grant -->
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
</xsl:stylesheet>
