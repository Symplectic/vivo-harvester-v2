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
                xmlns:symp="http://www.symplectic.co.uk/vivo/"
                xmlns:svfn="http://www.symplectic.co.uk/vivo/namespaces/functions"
                xmlns:config="http://www.symplectic.co.uk/vivo/namespaces/config"
                xmlns:obo="http://purl.obolibrary.org/obo/"
                exclude-result-prefixes="rdf rdfs bibo vivo foaf score ufVivo vitro api symp svfn config xs"
        >

    <!--
        Main import for Group processing (publication, user, etc.)
    -->

    <xsl:param name="groupMembers">
        <!--<xsl:element name="groupMembers">-->
            <!--<xsl:element name="user">-->
                <!--<xsl:attribute name="id">123</xsl:attribute>-->
                <!--<xsl:attribute name="username">bob</xsl:attribute>-->
            <!--</xsl:element>-->
            <!--<xsl:element name="user">-->
                <!--<xsl:attribute name="id">456</xsl:attribute>-->
                <!--<xsl:attribute name="username">bill</xsl:attribute>-->
            <!--</xsl:element>-->
            <!--<xsl:element name="user">-->
                <!--<xsl:attribute name="id">987</xsl:attribute>-->
                <!--<xsl:attribute name="username">jenny</xsl:attribute>-->
            <!--</xsl:element>-->
        <!--</xsl:element>-->
    </xsl:param>

    <xsl:param name="includedParentGroupId" />

    <!-- Default template - output group -->
    <xsl:template match="api:user-group" >

        <!-- create the URI that we will use for this group -->
        <xsl:variable name="groupID" select="@id" />
        <xsl:variable name="groupURI" select="svfn:makeURI('institutional-user-group-', $groupID)" />
        <!-- work out what "VIVO type" of group we will treat this group as -->

        <xsl:variable name="defaultGroupType">
            <xsl:choose>
                <!-- perhaps should make use svfn:getOrganizationType instead? -->
                <xsl:when test="@id = 1"><xsl:text>http://vivoweb.org/ontology/core#University</xsl:text></xsl:when>
                <xsl:otherwise><xsl:text>http://vivoweb.org/ontology/core#AcademicDepartment</xsl:text></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="groupType" select="svfn:getOrganizationType(api:name, $defaultGroupType)" />
        <!--<xsl:variable name="groupType" select="$defaultGroupType" />-->

        <!-- render triples to describe this group -->
        <xsl:call-template name="render_rdf_object">
            <xsl:with-param name="objectURI" select="$groupURI" />
            <xsl:with-param name="rdfNodes">
                <!-- TODO Implement dictionary to determine department type -->
                <rdf:type rdf:resource="{$groupType}"/>
                <xsl:if test="$internalClass!=''"><rdf:type rdf:resource="{$internalClass}" /></xsl:if>
                <rdfs:label><xsl:value-of select="api:name"/></rdfs:label>
                <vivo:overview><xsl:value-of select="api:group-description"/></vivo:overview>

                <xsl:if test="$includedParentGroupId">
                    <!-- will currently create crud in the vivo db for any groups that are NOT included....sigh -->
                    <obo:BFO_0000050 rdf:resource="{svfn:makeURI('institutional-user-group-', $includedParentGroupId)}" />
                </xsl:if>

                <!--<xsl:if test="api:parents/api:parent[1]">-->
                    <!--&lt;!&ndash; will currently create crud in the vivo db for any groups that are NOT included....sigh &ndash;&gt;-->
                    <!--<obo:BFO_0000050 rdf:resource="{svfn:makeURI('institutional-user-group-', api:parents/api:parent[1]/@id)}" />-->
                <!--</xsl:if>-->
            </xsl:with-param>
        </xsl:call-template>

        <!--todo: should the parent group have its children set too? are inferred - rightly or wrongly...-->

        <!-- for each group member render info about their membership of this group -->
        <xsl:for-each select="$groupMembers/groupMembers/user">
            <xsl:variable name="membershipURI" select="svfn:objectToObjectURI('group-user-membership-', $groupID, @id)" />
            <xsl:variable name="userURI" select="svfn:userURI(.)" />
            <!-- Output RDF for vivo:NonAcademicPosition individual -->
            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$membershipURI" />
                <xsl:with-param name="rdfNodes">
                    <!-- XXX: vivo:Position is the "Other" select option in VIVO 1.7 user interface. This
                             could also be vivo:FacultyPosition, vivo:FacultyAdministrativePosition,
                             vivo:LibrarianPosition, vivo:NonFacultyAcademicPosition, vivo:PostdocPosition,
                             or vivo:PrimaryPosition -->
                    <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Position" />
                    <rdfs:label>Member</rdfs:label>
                    <vivo:relates rdf:resource="{$groupURI}" />
                    <vivo:relates rdf:resource="{$userURI}" />
                </xsl:with-param>
            </xsl:call-template>

            <xsl:call-template name="render_rdf_object">
            <xsl:with-param name="objectURI" select="$groupURI" />
                <xsl:with-param name="rdfNodes">
                    <vivo:relatedBy rdf:resource="{$membershipURI}" />
                </xsl:with-param>
            </xsl:call-template>

            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$userURI" />
                <xsl:with-param name="rdfNodes">
                    <vivo:relatedBy rdf:resource="{$membershipURI}" />
                </xsl:with-param>
            </xsl:call-template>

        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
