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


    <!-- Import XSLT files that are used -->
    <xsl:import href="elements-to-vivo-fuzzy-matching.xsl" />

    <!-- declare parameter containing the list of groups for a user being processed passed in from framework-->
    <xsl:param name="userGroups" select="/.."/>

    <!-- template to supress all output except users -->
    <xsl:template match="api:object" mode="userGroupMembershipProcessing" />

    <!-- template to process group membership for users -->
    <xsl:template match="api:object[@category='user']" mode="userGroupMembershipProcessing" >

        <xsl:variable name="user" select="." />
        <xsl:variable name="userURI" select="svfn:userURI(.)" />
        <xsl:variable name="userID" select="@id" />

        <xsl:variable name="requiredDiff" select="0.95" />

        <xsl:for-each select="$userGroups/usersGroups/group">

            <xsl:variable name="positionTitle">
                <xsl:text>Member</xsl:text>
            </xsl:variable>

            <xsl:variable name="membershipURI" select="svfn:objectToObjectURI('group-user-membership-', @id, $userID)" />
            <!--<xsl:variable name="groupURI" select="svfn:makeURI('institutional-user-group-', @id)" />-->
            <xsl:variable name="groupURI" select="svfn:groupURI(@id, @name)" />
            <!-- Output RDF for vivo:NonAcademicPosition individual -->
            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$membershipURI" />
                <xsl:with-param name="rdfNodes">
                    <!-- XXX: vivo:Position is the "Other" select option in VIVO 1.7 user interface. This
                             could also be vivo:FacultyPosition, vivo:FacultyAdministrativePosition,
                             vivo:LibrarianPosition, vivo:NonFacultyAcademicPosition, vivo:PostdocPosition,
                             or vivo:PrimaryPosition -->
                    <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Position" />
                    <xsl:variable name="positionLabelContent" select="svfn:positionLabelContent($user, svfn:compare-strings(@name, $user/api:department) &gt;= $requiredDiff )" />
                    <xsl:if test="$positionLabelContent and normalize-space($positionLabelContent) != ''">
                        <rdfs:label><xsl:value-of select="$positionLabelContent"/></rdfs:label>
                    </xsl:if>
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

        <!--todo: should we pick up some "position" information and try to match against user groups (e.g. institutional-appointments?)...-->
    </xsl:template>

    <xsl:function name="svfn:positionLabelContent">
        <xsl:param name="user" as="node()" />
        <xsl:param name="isDepartment" as="xs:boolean" />

        <xsl:variable name="defaultPositionTitle">
            <xsl:text>Member</xsl:text>
        </xsl:variable>

        <xsl:variable name="mainPositionName" select="$user/api:position" />

        <xsl:choose>
            <xsl:when test="$isDepartment">
                <xsl:value-of select="normalize-space($mainPositionName)" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="normalize-space($defaultPositionTitle)" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

</xsl:stylesheet>
