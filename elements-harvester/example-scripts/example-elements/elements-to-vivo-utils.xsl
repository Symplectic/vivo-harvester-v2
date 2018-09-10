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
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:bibo="http://purl.org/ontology/bibo/"
                xmlns:vivo="http://vivoweb.org/ontology/core#"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:score="http://vivoweb.org/ontology/score#"
                xmlns:vcard="http://www.w3.org/2006/vcard/ns#"
                xmlns:ufVivo="http://vivo.ufl.edu/ontology/vivo-ufl/"
                xmlns:vitro="http://vitro.mannlib.cornell.edu/ns/vitro/0.7#"
                xmlns:vitro-public="http://vitro.mannlib.cornell.edu/ns/vitro/public#"
                xmlns:api="http://www.symplectic.co.uk/publications/api"
                xmlns:config="http://www.symplectic.co.uk/vivo/namespaces/config"
                xmlns:symp="http://www.symplectic.co.uk/ontology/elements/"
                xmlns:svfn="http://www.symplectic.co.uk/vivo/namespaces/functions"
                xmlns:obo="http://purl.obolibrary.org/obo/"
                exclude-result-prefixes="api config xs fn svfn"
                >

    <xsl:import href="elements-to-vivo-datatypes.xsl" />

    <!-- ======================================
         Function Library
         ======================================- -->

    <!--
        svfn:getOrganizationType
        ========================
        Get mapped Org type based on configuration - if no specific mapping found try to infer.
    -->
    <xsl:function name="svfn:getOrganizationType">
        <xsl:param name="name" />
        <xsl:param name="default" />

        <xsl:choose>
            <xsl:when test="$organization-types/config:organization-type[@name=$name]"><xsl:value-of select="($organization-types/config:organization-type[@name=$name])[1]/@type" /></xsl:when>
            <xsl:when test="contains($name,'University')"><xsl:text>http://vivoweb.org/ontology/core#University</xsl:text></xsl:when>
            <xsl:when test="contains($name,'College')"><xsl:text>http://vivoweb.org/ontology/core#College</xsl:text></xsl:when>
            <xsl:when test="contains($name,'Museum')"><xsl:text>http://vivoweb.org/ontology/core#Museum</xsl:text></xsl:when>
            <xsl:when test="contains($name,'Hospital')"><xsl:text>http://vivoweb.org/ontology/core#Hospital</xsl:text></xsl:when>
            <xsl:when test="contains($name,'Institute')"><xsl:text>http://vivoweb.org/ontology/core#Institute</xsl:text></xsl:when>
            <xsl:when test="contains($name,'School')"><xsl:text>http://vivoweb.org/ontology/core#School</xsl:text></xsl:when>
            <xsl:when test="contains($name,'Association')"><xsl:text>http://vivoweb.org/ontology/core#Association</xsl:text></xsl:when>
            <xsl:when test="contains($name,'Library')"><xsl:text>http://vivoweb.org/ontology/core#Library</xsl:text></xsl:when>
            <xsl:when test="contains($name,'Foundation')"><xsl:text>http://vivoweb.org/ontology/core#Foundation</xsl:text></xsl:when>
            <xsl:when test="contains($name,'Ltd')"><xsl:text>http://vivoweb.org/ontology/core#PrivateCompany</xsl:text></xsl:when>
            <xsl:otherwise><xsl:value-of select="$default" /></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Get publication type statements from the XML configuration (for the type supplied as a parameter) -->
    <xsl:function name="svfn:getTypesForPublication">
        <xsl:param name="object"  />

        <xsl:variable name="type" select="$object/@type" />

        <xsl:variable name="chosenRecordPrecedenceName" select="$publication-types/@recordPrecedenceToUse" />
        <xsl:variable name="matchAcrossRecordsAsBackstop">
            <xsl:choose>
                <xsl:when test="$publication-types/@matchAcrossRecordsAsBackstop">
                    <xsl:value-of select="xs:boolean($publication-types/@matchAcrossRecordsAsBackstop)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="false()" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <testcp><xsl:value-of select="$chosenRecordPrecedenceName" /></testcp>

        <xsl:variable name="precedence-to-use">
            <xsl:choose>
                <xsl:when test="$record-precedences[@for=$chosenRecordPrecedenceName]"><xsl:value-of select="$chosenRecordPrecedenceName" /></xsl:when>
                <xsl:when test="$record-precedences[@for=$object/@category]"><xsl:value-of select="$object/@category" /></xsl:when>
                <xsl:otherwise><xsl:value-of select="'default'" /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <ptu><xsl:value-of select="$precedence-to-use" /></ptu>

        <xsl:variable name="record-precedence" select="$record-precedences[@for=$precedence-to-use]/config:record-precedence" />
        <xsl:variable name="record-precedence-use-unlisted" select="$record-precedences[@for=$precedence-to-use]/@use-unlisted-sources != 'false'" />

        <xsl:variable name="publication-type">
            <xsl:sequence select="svfn:_getMatchingConditionalTypes($object, $record-precedence, 1, $record-precedence-use-unlisted, $matchAcrossRecordsAsBackstop)" />
         </xsl:variable>

        <xsl:choose>
            <!-- if the configuration specifies a most specific type, copy that -->
            <xsl:when test="$publication-type/vitro:mostSpecificType">
                <vitro:mostSpecificType rdf:resource="{svfn:expandSpecialNames($publication-type/vitro:mostSpecificType/@rdf:resource)}" />
            </xsl:when>
            <!-- no most specific type designated, so use the first type listed -->
            <xsl:when test="count($publication-type/rdf:type) &gt; 1">
                <vitro:mostSpecificType rdf:resource="{svfn:expandSpecialNames($publication-type/rdf:type[1]/@rdf:resource)}" />
            </xsl:when>
        </xsl:choose>
        <!-- Copy all of the rdf:type statements from the selected configuration to the output -->
        <xsl:for-each select="$publication-type/rdf:type">
            <rdf:type rdf:resource="{svfn:expandSpecialNames(@rdf:resource)}" />
        </xsl:for-each>

    </xsl:function>

    <xsl:function name="svfn:_getMatchingConditionalTypes">
        <xsl:param name="object" />
        <xsl:param name="records" />
        <xsl:param name="position" as="xs:integer" />
        <xsl:param name="useUnlistedSources" as="xs:boolean" />
        <xsl:param name="matchAcrossRecordsAsBackstop" as="xs:boolean" />

        <xsl:variable name="type" select="$object/@type" />
        <xsl:choose>
            <!-- Whilst looping through the list of record precedences, try to grab a value from the current source being processed -->
            <xsl:when test="$records[$position]">

                <xsl:variable name="currentSourceName">
                    <xsl:choose>
                        <xsl:when test="$records[$position] = 'verified-manual'">
                            <xsl:text>manual</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$records[$position]" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="requiredVerificatonStatus">
                    <xsl:choose>
                        <xsl:when test="$records[$position] = 'verified-manual'">
                            <xsl:text>verified</xsl:text>
                        </xsl:when>
                        <xsl:when test="$records[$position] = 'manual' and $records[$position]/@verification-status">
                            <xsl:value-of select="$records[$position]/@verification-status" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>any</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <xsl:choose>
                    <xsl:when test="$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificatonStatus = 'any' or api:verification-status = $requiredVerificatonStatus)]" >
                        <xsl:variable name="restricted-object">
                            <api:object>
                                <api:records>
                                    <xsl:copy-of select= "$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificatonStatus = 'any' or api:verification-status = $requiredVerificatonStatus)][1]" />
                                </api:records>
                            </api:object>
                        </xsl:variable>
                        <xsl:choose>
                            <xsl:when test="$publication-types/config:publication-type[@type=$type]/config:on-condition[svfn:evaluateCondition($restricted-object/api:object, config:condition[1]/*[1])]">
                                <xsl:copy-of select="$publication-types/config:publication-type[@type=$type]/config:on-condition[svfn:evaluateCondition($restricted-object/api:object, config:condition[1]/*[1])][1]/*[self::vitro:mostSpecificType or self::rdf:type]" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:copy-of select="svfn:_getMatchingConditionalTypes($object,$records,$position+1,$useUnlistedSources, $matchAcrossRecordsAsBackstop)" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="svfn:_getMatchingConditionalTypes($object,$records,$position+1,$useUnlistedSources, $matchAcrossRecordsAsBackstop)" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <!-- if we are past the end of our list of precedences, just grab something unless we would be selecting forbidden data..-->
                <xsl:choose>
                    <!-- this first when is inefficient as we have already tested all the "listed" records in the precedence ordering - but it is simple and it works -->
                    <xsl:when test="$useUnlistedSources">
                        <xsl:variable name="unlisted-records" select="$object/api:records/api:record[not($records=@source-name)]/@source-name" />
                        <xsl:copy-of select="svfn:_getMatchingConditionalTypes($object,$unlisted-records,1,false(), $matchAcrossRecordsAsBackstop)" />
                    </xsl:when>
                    <xsl:when test="$matchAcrossRecordsAsBackstop and $publication-types/config:publication-type[@type=$type]/config:on-condition[svfn:evaluateCondition($object, config:condition[1]/*[1])]">
                    <xsl:copy-of select="$publication-types/config:publication-type[@type=$type]/config:on-condition[svfn:evaluateCondition($object, config:condition[1]/*[1])][1]/*[self::vitro:mostSpecificType or self::rdf:type]" />
                    </xsl:when>
                    <xsl:when test="$publication-types/config:publication-type[@type=$type]"><xsl:copy-of select="$publication-types/config:publication-type[@type=$type]/*[self::vitro:mostSpecificType or self::rdf:type]" /></xsl:when>
                    <xsl:when test="$publication-types/config:publication-type[@type='z-default']"><xsl:copy-of select="$publication-types/config:publication-type[@type='z-default']/*[self::vitro:mostSpecificType or self::rdf:type]" /></xsl:when>
                    <xsl:otherwise><xsl:copy-of select="$publication-types/config:publication-type[1]/*[self::vitro:mostSpecificType or self::rdf:type]" /></xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>




    <xsl:function name="svfn:translationContext">
        <xsl:param name="rdfTypes"  />

        <xsl:variable name="contextLookups">
            <types>
                <typeset treat-as="presentation">
                    <type uri="http://vivoweb.org/ontology/core#Presentation" />
                    <type uri="http://vivoweb.org/ontology/core#InvitedTalk" />
                </typeset>
                <typeset treat-as="event">
                    <type uri="http://vivoweb.org/ontology/core#Exhibit" />
                    <type uri="http://purl.org/ontology/bibo/Performance" />
                    <type uri="http://purl.org/NET/c4dm/event.owl#Event" />
                </typeset>
                <default treat-as="publication" />
            </types>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="$contextLookups/types/typeset[type/@uri = $rdfTypes/@rdf:resource]">
                <xsl:value-of select="$contextLookups/types/typeset[type/@uri = $rdfTypes/@rdf:resource][1]/@treat-as" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$contextLookups/types/default[1]/@treat-as" />
            </xsl:otherwise>
        </xsl:choose>

    </xsl:function>

    <xsl:function name="svfn:shouldConvertToHTML">
        <xsl:param name="propertyName" as="xs:string" />

        <xsl:choose>
            <xsl:when test="$htmlProperties[@name=$propertyName]">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- templates to inject html line breaks if required, these need to be templates as functions end up evaluating the
 result in a value -of which strips the desired <br/> tags..-->

    <!-- template to take in input containing line breaks /r/n /r or /n and turn them into html containing <br/> tags -->
    <xsl:function name="svfn:injectHTMLLinesBreaks">
        <xsl:param name="input" as="xs:string" />
        <xsl:variable name="step1" select="replace($input, '&#13;&#10;', '&#10;')" />
        <xsl:variable name="step2" select="replace($step1, '&#13;', '&#10;')" />
        <xsl:value-of select="replace($step2, '&#10;', '&lt;br/&gt;')" />
    </xsl:function>


    <xsl:function name="svfn:injectHtmlAnchors">
        <xsl:param name="input" as="xs:string" />
        <xsl:value-of select="replace($input, '(^|\n|\s)https?://(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}(\?[-a-zA-Z0-9@:%_\+.~#?&amp;//=]*)?($|\n|\s)', '&lt;a href=&#34;$0&#34; &gt;$0&lt;/a&gt;')" />
    </xsl:function>

    <!--
        svfn:makeURI
        ============
        Format a URI
    -->
    <xsl:function name="svfn:makeURI">
        <xsl:param name="prefix" as="xs:string" />
        <xsl:param name="id" as="xs:string" />

        <xsl:variable name="calculatedURISuffix" select="concat(svfn:stringToURI($prefix),svfn:stringToURI($id))" />

        <xsl:choose>
            <xsl:when test="$uriAliasses[@alias=$calculatedURISuffix]">
                <xsl:value-of select="concat($validatedBaseURI, $uriAliasses[@alias=$calculatedURISuffix][1])" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat($validatedBaseURI,$calculatedURISuffix)" />
            </xsl:otherwise>
        </xsl:choose>

    </xsl:function>


    <!--
        svfn:nonBaseUriFragment
        ============
    -->
    <xsl:function name="svfn:nonBaseUriFragment">
        <xsl:param name="uri" as="xs:string" />
        <xsl:if test="starts-with($uri,$validatedBaseURI)">
            <xsl:value-of select="substring-after($uri,$validatedBaseURI)" />
        </xsl:if>
    </xsl:function>


    <!--
        svfn:objectURI
        ==============
        Create a URI for the RDF objects based on the passed Elements object
    -->
    <xsl:function name="svfn:objectURI" as="xs:string">
        <xsl:param name="object" />

        <xsl:value-of select="svfn:makeURI($object/@category,$object/@id)" />
    </xsl:function>

    <!--
        svfn:userURI
        ============
        Create a URI for a user based on the passed Elements object
    -->
    <xsl:function name="svfn:userURI" as="xs:string">
        <xsl:param name="object" />

        <xsl:value-of select="svfn:makeURI('',$object/@username)" />
    </xsl:function>


    <!--
        svfn:groupURI
        ============
        Create a URI for a group based on the passed in Elements group object (or equivalent proxy object)
        Note: if you use anything other then id or name to create the user's uri
        you will have issues with group membership as the group proxy objects passed as a param there
        only contains those attributes.
    -->
    <xsl:function name="svfn:groupURI" as="xs:string">
        <xsl:param name="id" as="xs:integer"/>
        <xsl:param name="name" as="xs:string"/>
        <!--<xsl:value-of select="svfn:makeURI('institutional-user-group-', string($id))" />-->
        <xsl:value-of select="svfn:makeURI('g-', concat(string($id), '-', $name))" />
    </xsl:function>


    <!--
        svfn:objectToObjectURI
        ======================
    -->
    <xsl:function name="svfn:objectToObjectURI" as="xs:string">
        <xsl:param name="prefix" as="xs:string" />
        <xsl:param name="objectid1" as="xs:string" />
        <xsl:param name="objectid2" as="xs:string" />

        <xsl:value-of select="svfn:makeURI($prefix,concat($objectid1,'-',$objectid2))" />
    </xsl:function>

    <!--
        svfn:relationshipURI
        ====================
        Create a URI for a relationship object, based on the given Elements relationship object
    -->
    <xsl:function name="svfn:relationshipURI" as="xs:string">
        <xsl:param name="relationship" />
        <xsl:param name="type" />

        <xsl:value-of select="svfn:makeURI($type,$relationship/@id)" />
    </xsl:function>


    <xsl:function name="svfn:organisationObjects">
        <xsl:param name="address" />
        <xsl:copy-of select="svfn:organisationObjects($address, $includeDept)" />
    </xsl:function>

    <!--
    svfn:userLabel
    ======================
    default of how a user should be described in a "label"
    -->
    <xsl:function name="svfn:userLabel">
        <xsl:param name="user" />
        <xsl:variable name="firstName">
            <xsl:choose>
                <xsl:when test="$user/api:known-as and normalize-space($user/api:known-as) != ''">
                    <xsl:value-of select="$user/api:known-as" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$user/api:first-name" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="lastName"><xsl:value-of select="$user/api:last-name" /></xsl:variable>
        <xsl:value-of select="$lastName, $firstName" separator=", " />
    </xsl:function>


    <!--
        svfn:organisationObjects
        ====================
        Create objects representing an an institution and any sub organisation if present from an api:address or api:institution object
    -->
    <xsl:function name="svfn:organisationObjects">
        <xsl:param name="address" />
        <xsl:param name="createDepartment" as="xs:boolean" />

        <xsl:variable name="deptName"><xsl:value-of select="$address/api:line[@type='suborganisation']" /></xsl:variable>
        <xsl:variable name="instName">
            <xsl:choose>
                <xsl:when test="$address/api:line[@type='organisation']">
                    <xsl:value-of select="$address/api:line[@type='organisation']" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$address/api:line[@type='name']" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="deptURI">
            <xsl:choose>
                <xsl:when test="not($deptName='') and not($instName='')"><xsl:value-of select="svfn:makeURI('dept-',concat(fn:substring($deptName,1,100),'-',fn:substring($instName,1,50)))" /></xsl:when>
                <xsl:when test="not($deptName='')"><xsl:value-of select="svfn:makeURI('dept-',fn:substring($deptName,1,150))" /></xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="instURI" >
            <xsl:choose>
                <xsl:when test="not($instName='')"><xsl:value-of select="svfn:makeURI('institution-',$instName)" /></xsl:when>
            </xsl:choose>
        </xsl:variable>

        <xsl:if test="$createDepartment">
            <xsl:if test="not($deptURI = '')">
                <xsl:call-template name="render_rdf_object">
                    <xsl:with-param name="objectURI" select="$deptURI" />
                    <xsl:with-param name="rdfNodes">
                        <!-- TODO Implement dictionary to determine department type -->
                        <rdf:type rdf:resource="http://vivoweb.org/ontology/core#AcademicDepartment"/>
                        <rdfs:label><xsl:value-of select="$deptName" /></rdfs:label>
                        <xsl:if test="not($instURI='')">
                            <obo:BFO_0000050 rdf:resource="{$instURI}" />
                        </xsl:if>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:if>
        </xsl:if>

        <xsl:if test="not($instURI='')">
            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$instURI" />
                <xsl:with-param name="rdfNodes">
                    <rdf:type rdf:resource="{svfn:getOrganizationType($instName,'http://vivoweb.org/ontology/core#University')}" />
                    <rdfs:label><xsl:value-of select="$instName" /></rdfs:label>
                    <xsl:if test="not($deptURI='') and $createDepartment">
                        <obo:BFO_0000051 rdf:resource="{$deptURI}" />
                    </xsl:if>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:function>

    <xsl:function name="svfn:organisationObjectsMainURI" as="xs:string">
        <xsl:param name="orgObjects" />
        <xsl:variable name="uri-to-use">
            <xsl:choose>
                <xsl:when test="$orgObjects[@rdf:about][1]/@rdf:about"><xsl:value-of select="$orgObjects[@rdf:about][1]/@rdf:about" /></xsl:when>
                <xsl:when test="$orgObjects/@rdf:about"><xsl:value-of select="$orgObjects/@rdf:about" /></xsl:when>
                <xsl:otherwise><xsl:text /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="fn:normalize-space($uri-to-use)" />
    </xsl:function>

    <!--
        svfn:renderDateObject
        =====================
        Generate a VIVO date object for the supplied date object
    -->
    <xsl:function name="svfn:renderDateObject">
        <xsl:param name="dateObjectURI" as="xs:string" />
        <xsl:param name="date" />
        <xsl:param name="precision" as="xs:string"/>

        <!-- only output if the date exists -->
        <xsl:if test="$date">
            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$dateObjectURI" />
                <!-- generate property statements - concat generated statements with fixed statements, as they are only required if the generated statements are output successfully -->
                <xsl:with-param name="rdfNodes">
                    <xsl:call-template name="_concat_nodes_if">
                        <xsl:with-param name="nodesRequired">
                            <xsl:apply-templates select="$date">
                                <xsl:with-param name="precision" select="$precision" />
                            </xsl:apply-templates>
                        </xsl:with-param>
                        <xsl:with-param name="nodesToAdd">
                            <rdf:type rdf:resource="http://vivoweb.org/ontology/core#DateTimeValue"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:function>

    <!--  svfn:renderDateInterval
        =====================
        Generate a VIVO date interval along with the relevant VIVO date objects for the supplied date range -->
    <xsl:function name="svfn:renderDateInterval">
        <xsl:param name="context" as="xs:string" />
        <xsl:param name="startDate" />
        <xsl:param name="endDate" />
        <xsl:param name="precision" as="xs:string"/>
        <xsl:param name="forceEndDate" as="xs:boolean"/>

        <xsl:variable name="startDateURI" select="concat($context,'-intervalStartDate')" />
        <xsl:variable name="endDateURI" select="concat($context,'-intervalEndDate')" />
        <xsl:variable name="dateIntervalURI" select="concat($context,'-dateInterval')" />
        <!--<xsl:variable name="dateIntervalURI">-->
            <!--<xsl:choose>-->
                <!--<xsl:when test="$endDate">-->
                    <!--<xsl:value-of select="concat($context,'-dateInterval-',svfn:dateYear($startDate),'-',svfn:dateYear($endDate))" />-->
                <!--</xsl:when>-->
                <!--<xsl:otherwise>-->
                    <!--<xsl:value-of select="concat($context,'-dateInterval-',svfn:dateYear($startDate))" />-->
                <!--</xsl:otherwise>-->
            <!--</xsl:choose>-->
        <!--</xsl:variable>-->

        <xsl:if test="$startDate or $endDate">
            <xsl:copy-of select="svfn:renderDateObject($startDateURI,$startDate, $precision)" />
            <xsl:copy-of select="svfn:renderDateObject($endDateURI,$endDate, $precision)" />

            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$dateIntervalURI" />
                <xsl:with-param name="rdfNodes">
                    <rdf:type rdf:resource="http://vivoweb.org/ontology/core#DateTimeInterval" />
                    <xsl:if test="$startDate">
                        <vivo:start rdf:resource="{$startDateURI}" />
                    </xsl:if>
                    <xsl:choose>
                        <xsl:when test="$endDate">
                            <vivo:end rdf:resource="{$endDateURI}" />
                        </xsl:when>
                        <xsl:when test="$forceEndDate">
                            <vivo:end rdf:resource="{$startDateURI}" />
                        </xsl:when>
                    </xsl:choose>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:function>

    <xsl:function name="svfn:retrieveDateIntervalUri" as="xs:string">
        <xsl:param name="vivoDateInterval" />
        <xsl:value-of select="svfn:retrieveUri($vivoDateInterval, 'http://vivoweb.org/ontology/core#DateTimeInterval' )" />
    </xsl:function>

    <xsl:function name="svfn:retrieveUri" as="xs:string">
        <xsl:param name="rdfFragment" />
        <xsl:param name="rdfType" />
        <xsl:variable name="uri-to-use">
            <xsl:choose>
            <xsl:when test="$rdfFragment and $rdfFragment[rdf:type/@rdf:resource=$rdfType]">
                <xsl:value-of select="$rdfFragment[rdf:type/@rdf:resource=$rdfType][1]/@rdf:about" />
            </xsl:when>
            <xsl:when test="$rdfFragment and $rdfFragment/rdf:Description[rdf:type/@rdf:resource=$rdfType]">
                <xsl:value-of select="$rdfFragment/rdf:Description[rdf:type/@rdf:resource=$rdfType][1]/@rdf:about" />
            </xsl:when>
            <xsl:otherwise><xsl:value-of select="''" /></xsl:otherwise>
        </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="fn:normalize-space($uri-to-use)" />
    </xsl:function>

    <!--
        svfn:datePrecision
        ==================
        Determine how precise the Elements date object is
    -->
    <xsl:function name="svfn:datePrecision" as="xs:string">
        <xsl:param name="date" />
        <xsl:param name="precision" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="$precision = ''">
                <xsl:choose>
                    <xsl:when test="string($date/api:day) and string($date/api:month) and string($date/api:year)">yearMonthDayPrecision</xsl:when>
                    <xsl:when test="string($date/api:month) and string($date/api:year)">yearMonthPrecision</xsl:when>
                    <xsl:when test="string($date/api:year)">yearPrecision</xsl:when>
                    <xsl:otherwise>none</xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="($precision = 'yearMonthDayPrecision') or ($precision = 'yearMonthPrecision') or ($precision = 'yearPrecision')">
                        <xsl:value-of select="$precision" />
                    </xsl:when>
                    <xsl:otherwise>none</xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:function>

    <!--
        svfn:dateYear
        =============
        Return the formatted year
    -->
    <xsl:function name="svfn:dateYear">
        <xsl:param name="date" />

        <xsl:value-of select="$date/api:year" />
    </xsl:function>

    <!--
        svfn:dateMonth
        ==============
        Return the formatted month
    -->
    <xsl:function name="svfn:dateMonth">
        <xsl:param name="date" />

        <xsl:choose>
            <xsl:when test="string-length($date/api:month)=1">0<xsl:value-of select="$date/api:month" /></xsl:when>
            <xsl:when test="string-length($date/api:month)=2"><xsl:value-of select="$date/api:month" /></xsl:when>
            <xsl:otherwise>01</xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
        svfn:dateDay
        ============
        Return the formatted day
    -->
    <xsl:function name="svfn:dateDay">
        <xsl:param name="date" />

        <xsl:choose>
            <xsl:when test="string-length($date/api:day)=1">0<xsl:value-of select="$date/api:day" /></xsl:when>
            <xsl:when test="string-length($date/api:day)=2"><xsl:value-of select="$date/api:day" /></xsl:when>
            <xsl:otherwise>01</xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
        svfn:stringToURI
        ================
        Convert a string into a URI-friendly form (for identifiers)
    -->
    <xsl:function name="svfn:stringToURI">
        <xsl:param name="string" as="xs:string" />

        <xsl:value-of select="fn:encode-for-uri(fn:replace(fn:replace(fn:lower-case(fn:normalize-space($string)), '\s', '-'), '[^a-z0-9\-]', ''))" />
    </xsl:function>

    <!--
        svfn:fullObject
        ===============
        Load the XML for the full object, given an Elements object reference
    -->
    <xsl:function name="svfn:fullObject">
        <xsl:param name="object" />
        <xsl:choose>
            <xsl:when test="$extraObjects and $extraObjects/descendant::api:object[(@category=$object/@category) and (@id=$object/@id)]">
                <xsl:copy-of select="$extraObjects/descendant::api:object[(@category=$object/@category) and (@id=$object/@id)]" />
            </xsl:when>
            <xsl:when test="$useRawDataFiles = 'true'">
                <!-- this exists to enable easier testing only = requires that recordDir is set to point at a directory containing an unzipped set of raw records -->
                <xsl:variable name="filename" select="concat($recordDir,$object/@category,'/',$object/@id)" />
                <xsl:choose>
                    <xsl:when test="fn:doc-available($filename)">
                        <xsl:copy-of select="document($filename)//api:object" />
                    </xsl:when>
                    <xsl:when test="fn:doc-available(concat($filename,'.xml'))">
                        <xsl:copy-of select="document(concat($filename,'.xml'))//api:object" />
                    </xsl:when>
                </xsl:choose>
            </xsl:when>
        </xsl:choose>
    </xsl:function>

    <!--
        svfn:renderPropertyFromFieldOrFirst
        ===================================
        Function to retrieve the specified Elements field (fieldName) from the most preferred record,
        and render it as the RDF property (propertyName)
        If the field is not present in any preferred records, output the value in the first record present.
    -->
    <xsl:function name="svfn:renderPropertyFromFieldOrFirst">
        <xsl:param name="object" />
        <xsl:param name="propertyName" as="xs:string" />
        <xsl:param name="fieldName" as="xs:string" />
        <xsl:param name="default" as="item()"/>


        <xsl:variable name="recordField" select="svfn:getRecordFieldOrFirst($object, $fieldName)" />
        <xsl:choose>
            <xsl:when test="$recordField">
                <xsl:copy-of select="svfn:_renderPropertyFromField($object, $propertyName, $fieldName, $recordField)" />
            </xsl:when>
            <xsl:when test="$default instance of element()">
                <xsl:copy-of select="svfn:_renderPropertyFromField($object, $propertyName, $fieldName, $default)" />
            </xsl:when>
            <xsl:when test="$default">
                <xsl:variable name="dummy-field">
                    <api:text name="{$propertyName}">
                        <xsl:value-of select="$default" />
                    </api:text>
                </xsl:variable>
                <xsl:copy-of select="svfn:_renderPropertyFromField($object, $propertyName, $fieldName, $dummy-field)" />
            </xsl:when>
        </xsl:choose>
    </xsl:function>

    <!--
        svfn:renderPropertyFromField
        ============================
        Function to retrieve the specified Elements field (fieldName) from the most preferred record,
        and render it as the RDF property (propertyName)
    -->
    <xsl:function name="svfn:renderPropertyFromField">
        <xsl:param name="object" />
        <xsl:param name="propertyName" as="xs:string" />
        <xsl:param name="fieldName" as="xs:string" />

        <xsl:copy-of select="svfn:_renderPropertyFromField($object, $propertyName, $fieldName, svfn:getRecordField($object, $fieldName))" />
    </xsl:function>

    <!--
    svfn:renderPropertyFromField
    ============================
    Function to retrieve the specified Elements field (fieldName) from the most preferred record,
    and render it as the RDF property (propertyName)
    Overloaded method that takes a comma delimited list of record names to use as the preference order (override the central configuration)
-->
    <xsl:function name="svfn:renderPropertyFromField">
        <xsl:param name="object" />
        <xsl:param name="propertyName" as="xs:string" />
        <xsl:param name="fieldName" as="xs:string" />
        <xsl:param name="records" as="xs:string" />

        <xsl:copy-of select="svfn:_renderPropertyFromField($object, $propertyName, $fieldName, svfn:getRecordField($object, $fieldName, $records))" />
    </xsl:function>


    <!--
        svfn:renderPropertyFromField
        ============================
        Function to retrieve the specified Elements field (fieldName) from the most preferred record,
        and render it as the RDF property (propertyName)
        Overloaded method that takes a comma delimited list of record names to use as the preference order (override the central configuration)
        additionally takes "select-by and useUnlistedSources" to allow complete control of how record precedence is calculated
        select-by should be 'field' (find first occurrence of the field) or 'record' (use first preferred record, even if the field is not present)
        useUnlistedSources is a boolean true() or false()
    -->
    <xsl:function name="svfn:renderPropertyFromField">
        <xsl:param name="object" />
        <xsl:param name="propertyName" as="xs:string" />
        <xsl:param name="fieldName" as="xs:string" />
        <xsl:param name="records" as="xs:string" />
        <xsl:param name="select-by" as="xs:string" />
        <xsl:param name="useUnlistedSources" as="xs:boolean" />

        <xsl:copy-of select="svfn:_renderPropertyFromField($object, $propertyName, $fieldName, svfn:getRecordField($object, $fieldName, $records, $select-by, $useUnlistedSources))" />
    </xsl:function>

    <!--
        svfn:getRecordFieldOrFirst
        ==========================
        Function to retrieve the specified Elements field (fieldName) from the most preferred record,
        If the field is not present in any preferred records, output the value in the first record present.
    -->
    <xsl:function name="svfn:getRecordFieldOrFirst">
        <xsl:param name="object" />
        <xsl:param name="fieldName" as="xs:string" />


        <xsl:variable name="precedence-to-use">
            <xsl:choose>
                <xsl:when test="$record-precedences[@for=$object/@category]"><xsl:value-of select="$object/@category" /></xsl:when>
                <xsl:otherwise><xsl:value-of select="'default'" /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="record-precedence" select="$record-precedences[@for=$precedence-to-use]/config:record-precedence" />
        <xsl:variable name="record-precedence-select-by" select="$record-precedences[@for=$precedence-to-use]/@select-by" />
        <xsl:variable name="record-precedence-use-unlisted" select="$record-precedences[@for=$precedence-to-use]/@use-unlisted-sources != 'false'" />

        <xsl:copy-of select="svfn:_getRecordField($object, $fieldName, $record-precedence, $record-precedence-select-by, 1, $record-precedence-use-unlisted, true())" />
    </xsl:function>

    <!--
        svfn:getRecordField
        ================
        Function to retrieve the specified Elements field (fieldName) from the most preferred record,
    -->
    <xsl:function name="svfn:getRecordField">
        <xsl:param name="object" />
        <xsl:param name="fieldName" as="xs:string" />

        <xsl:variable name="precedence-to-use">
            <xsl:choose>
                <xsl:when test="$record-precedences[@for=$object/@category]"><xsl:value-of select="$object/@category" /></xsl:when>
                <xsl:otherwise><xsl:value-of select="'default'" /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="record-precedence" select="$record-precedences[@for=$precedence-to-use]/config:record-precedence" />
        <xsl:variable name="record-precedence-select-by" select="$record-precedences[@for=$precedence-to-use]/@select-by" />
        <xsl:variable name="record-precedence-use-unlisted" select="$record-precedences[@for=$precedence-to-use]/@use-unlisted-sources != 'false'" />

        <xsl:copy-of select="svfn:_getRecordField($object, $fieldName, $record-precedence, $record-precedence-select-by, 1, $record-precedence-use-unlisted, false())" />
    </xsl:function>

    <!--
        svfn:getRecordField
        ================
        Function to retrieve the specified Elements field (fieldName) from the most preferred record,
        Overloaded method that takes a comma delimited list of record names to use as the preference order (override the central configuration)
    -->
    <xsl:function name="svfn:getRecordField">
        <xsl:param name="object" />
        <xsl:param name="fieldName" as="xs:string" />
        <xsl:param name="records" as="xs:string" />

        <xsl:variable name="precedence-to-use">
            <xsl:choose>
                <xsl:when test="$record-precedences[@for=$object/@category]"><xsl:value-of select="$object/@category" /></xsl:when>
                <xsl:otherwise><xsl:value-of select="'default'" /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="record-precedence-select-by" select="$record-precedences[@for=$precedence-to-use]/@select-by" />
        <xsl:variable name="record-precedence-use-unlisted" select="$record-precedences[@for=$precedence-to-use]/@use-unlisted-sources != 'false'" />

        <xsl:copy-of select="svfn:_getRecordField($object, $fieldName, fn:tokenize($records,','), $record-precedence-select-by, 1, $record-precedence-use-unlisted, false())" />
    </xsl:function>

    <!--
        svfn:getRecordField
        ================
        Function to retrieve the specified Elements field (fieldName) from the most preferred record,
        Overloaded method that takes a comma delimited list of record names to use as the preference order (override the central configuration)
        additionally takes "select-by and useUnlistedSources" to allow complete control of how record precedence is calculated
        select-by should be 'field' (find first occurrence of the field) or 'record' (use first preferred record, even if the field is not present)
        useUnlistedSources is a boolean true() or false()
    -->
    <xsl:function name="svfn:getRecordField">
        <xsl:param name="object" />
        <xsl:param name="fieldName" as="xs:string" />
        <xsl:param name="records" as="xs:string" />
        <xsl:param name="select-by" as="xs:string" />
        <xsl:param name="useUnlistedSources" as="xs:boolean" />

        <xsl:copy-of select="svfn:_getRecordField($object, $fieldName, fn:tokenize($records,','), $select-by, 1, $useUnlistedSources, false())" />
    </xsl:function>

    <!--
        Internal XSLT Functions (should not be called from outside this file)
    -->

    <xsl:function name="svfn:_renderPropertyFromField">
        <xsl:param name="object" />
        <xsl:param name="propertyName" as="xs:string" />
        <xsl:param name="fieldName" as="xs:string" />
        <xsl:param name="fieldNode" />

        <xsl:apply-templates select="$fieldNode" mode="renderForProperty">
            <xsl:with-param name="propertyName" select="$propertyName" />
            <xsl:with-param name="fieldName" select="$fieldName" />
        </xsl:apply-templates>
    </xsl:function>

    <xsl:function name="svfn:_getRecordField">
        <xsl:param name="object" />
        <xsl:param name="fieldName" as="xs:string" />
        <xsl:param name="records" />
        <xsl:param name="select-by" />
        <xsl:param name="position" as="xs:integer" />
        <xsl:param name="useUnlistedSources" as="xs:boolean" />
        <xsl:param name="useExcludedData" as="xs:boolean" />

        <xsl:variable name="exclusions-to-use">
            <xsl:choose>
                <xsl:when test="$data-exclusions[@for=$object/@category]"><xsl:value-of select="$object/@category" /></xsl:when>
                <xsl:otherwise><xsl:value-of select="'default'" /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="exclusions" select="$data-exclusions[@for=$exclusions-to-use]" />

        <xsl:choose>
            <!-- Whilst looping through the list of record precedences, try to grab a value from the current source being processed -->
            <xsl:when test="$records[$position]">

                <xsl:variable name="currentSourceName">
                    <xsl:choose>
                        <xsl:when test="$records[$position] = 'verified-manual'">
                            <xsl:text>manual</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$records[$position]" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="requiredVerificatonStatus">
                    <xsl:choose>
                        <xsl:when test="$records[$position] = 'verified-manual'">
                            <xsl:text>verified</xsl:text>
                        </xsl:when>
                        <xsl:when test="$records[$position] = 'manual' and $records[$position]/@verification-status">
                            <xsl:value-of select="$records[$position]/@verification-status" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>any</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <xsl:choose>
                    <!-- Don't use records that are restricted -->
                    <xsl:when test="$exclusions/config:record-exclusion[text()=$currentSourceName and (text() != 'manual' or (not(@verification-status) or @verification-status = 'any' or @verification-status = $requiredVerificatonStatus))]">
                        <xsl:copy-of select="svfn:_getRecordField($object,$fieldName,$records,$select-by,$position+1,$useUnlistedSources,$useExcludedData)" />
                    </xsl:when>
                    <!-- don't use fields that are restricted -->
                    <xsl:when test="$exclusions/config:field-exclusions[@for-source=$currentSourceName and (text() != 'manual' or (not(@verification-status) or @verification-status = 'any' or @verification-status = $requiredVerificatonStatus))]/config:excluded-field[text()=$fieldName]">
                        <xsl:copy-of select="svfn:_getRecordField($object,$fieldName,$records,$select-by,$position+1,$useUnlistedSources,$useExcludedData)" />
                    </xsl:when>
                    <xsl:when test="$select-by='field'">
                        <xsl:choose>
                            <xsl:when test="$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificatonStatus = 'any' or api:verification-status = $requiredVerificatonStatus)]/api:native/api:field[@name=$fieldName]">
                                <xsl:copy-of select="$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificatonStatus = 'any' or api:verification-status = $requiredVerificatonStatus)][1]/api:native/api:field[@name=$fieldName]" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:copy-of select="svfn:_getRecordField($object,$fieldName,$records,$select-by,$position+1,$useUnlistedSources,$useExcludedData)" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when test="$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificatonStatus = 'any' or api:verification-status = $requiredVerificatonStatus)]/api:native">
                                <xsl:copy-of select="$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificatonStatus = 'any' or api:verification-status = $requiredVerificatonStatus)][1]/api:native/api:field[@name=$fieldName]" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:copy-of select="svfn:_getRecordField($object,$fieldName,$records,$select-by,$position+1,$useUnlistedSources,$useExcludedData)" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <!-- if we are past the end of our list of precedences, just grab something unless we would be selecting forbidden data..-->
                <xsl:choose>
                    <!-- if we are allowed to use excluded data (useFieldOrFirst) then just get first value-->
                    <xsl:when test="$useExcludedData">
                        <!--<xsl:copy-of select="svfn:_getFirstNonExcludedRecord($object, $fieldName, $select-by, 1, true())" />-->
                        <xsl:variable name="result-not-using-excluded-data">
                            <xsl:copy-of select="svfn:_getFirstNonExcludedRecord($object, $fieldName, $select-by, $exclusions, 1)" />
                        </xsl:variable>
                        <xsl:choose>
                            <xsl:when test="$result-not-using-excluded-data/*">
                                <xsl:copy-of select="$result-not-using-excluded-data" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:variable name="no-exclusions" select="/.." />
                                <xsl:copy-of select="svfn:_getFirstNonExcludedRecord($object, $fieldName, $select-by, $no-exclusions, 1)" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:when test="$useUnlistedSources">
                        <xsl:copy-of select="svfn:_getFirstNonExcludedRecord($object, $fieldName, $select-by, $exclusions, 1)" />
                    </xsl:when>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="svfn:_getFirstNonExcludedRecord">
        <xsl:param name="object" />
        <xsl:param name="fieldName" as="xs:string" />
        <xsl:param name="select-by" />
        <xsl:param name="exclusions" />
        <xsl:param name="position" as="xs:integer" />

        <xsl:choose>
            <!-- we are looping through the set of records in the object, until we find somethign we like, or run out of records to consider-->
            <xsl:when test="$object/api:records/api:record[$position]">
                <xsl:variable name="current-record" select="$object/api:records/api:record[$position]" />
                <xsl:choose>
                    <!-- Don't use records that are restricted unless otherwise instructed -->
                    <xsl:when test="$exclusions/config:record-exclusion[text()=$current-record/@source-name and (text() != 'manual' or (not(@verification-status) or @verification-status = 'any' or @verification-status = $current-record/api:verification-status))]">
                        <xsl:copy-of select="svfn:_getFirstNonExcludedRecord($object,$fieldName, $select-by, $exclusions,$position+1)" />
                    </xsl:when>
                    <!-- don't use fields that are restricted, unless otherwise instructed-->
                    <xsl:when test="$exclusions/config:field-exclusions[@for-source=$current-record/@source-name and (text() != 'manual' or (not(@verification-status) or @verification-status = 'any' or @verification-status = $current-record/api:verification-status))]/config:excluded-field[text()=$fieldName]">
                        <xsl:copy-of select="svfn:_getFirstNonExcludedRecord($object,$fieldName, $select-by, $exclusions,$position+1)" />
                    </xsl:when>
                    <!-- or we are selecting by field and there is no field value in this record -->
                    <xsl:when test="$select-by='field' and not($current-record/api:native/api:field[@name=$fieldName])">
                        <xsl:copy-of select="svfn:_getFirstNonExcludedRecord($object,$fieldName, $select-by,$exclusions,$position+1)" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="$current-record/api:native/api:field[@name=$fieldName]" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
        </xsl:choose>
    </xsl:function>


    <!--<xsl:function name="svfn:renderLinksAndExternalPeople">-->
        <!--<xsl:param name="people" />-->
        <!--<xsl:param name="linkedId" as="xs:string" />-->
        <!--<xsl:param name="linkedUri" as="xs:string" />-->

        <!--<xsl:choose>-->
            <!--<xsl:when test="$people/@name='authors'">-->
                <!--<xsl:copy-of select="svfn:renderLinksAndExternalPeople($people, $linkedId, $linkedUri, 'http://vivoweb.org/ontology/core#Authorship')" />-->
            <!--</xsl:when>-->
            <!--<xsl:when test="$people/@name='editors'">-->
                <!--<xsl:copy-of select="svfn:renderLinksAndExternalPeople($people, $linkedId, $linkedUri, 'http://vivoweb.org/ontology/core#Editorship')" />-->
            <!--</xsl:when>-->
        <!--</xsl:choose>-->
    <!--</xsl:function>-->

    <!--<xsl:function name="svfn:renderLinksAndExternalPeople">-->
    <!--<xsl:param name="people" />-->
    <!--<xsl:param name="linkedId" as="xs:string" />-->
    <!--<xsl:param name="linkedUri" as="xs:string" />-->
    <!--<xsl:param name="contextType" as="xs:string" />-->
        <!--<xsl:copy-of select="svfn:renderLinksAndExternalPeople($people, $linkedId, $linkedUri, $contextType, '')" />-->
    <!--</xsl:function>-->

    <xsl:function name="svfn:renderLinksAndExternalPeople">
        <xsl:param name="people" />
        <xsl:param name="linkedId" as="xs:string" />
        <xsl:param name="linkedUri" as="xs:string" />
        <xsl:param name="contextType" as="xs:string" />
        <xsl:param name="descriptor" as="xs:string" />

        <xsl:variable name="context-lu" select="$contextPropertyLookup/context-lookups/context-lookup[@type-uri=$contextType]" />
        <xsl:if test="$context-lu">
            <xsl:copy-of select="svfn:renderLinksAndExternalPeople($people, $linkedId, $linkedUri, $context-lu/@uriFragment, $context-lu/@type-uri, $context-lu/@contextToUser, $context-lu/@contextToObject, $context-lu/@objectToContext, $descriptor)" />
        </xsl:if>

    </xsl:function>

    <!--
    -->
    <xsl:function name="svfn:renderLinksAndExternalPeople">
        <xsl:param name="people" />
        <xsl:param name="linkedId" as="xs:string" />
        <xsl:param name="linkedUri" as="xs:string" />
        <xsl:param name="linkUriPrefix" as="xs:string" />
        <xsl:param name="contextObjectTypeUri" as="xs:string" />
        <xsl:param name="contextVCardLinkProperty" as="xs:string" />
        <xsl:param name="contextToObjectLinkProperty" as="xs:string" />
        <xsl:param name="objectToContextLinkProperty" as="xs:string" />
        <xsl:param name="descriptor" as="xs:string" />

        <xsl:if test="not($linkUriPrefix='')">
            <xsl:for-each select="$people/api:people/api:person">
                <xsl:variable name="contextUri">
                    <xsl:choose>
                        <xsl:when test="api:links/api:link/@type='elements/user'">
                            <xsl:value-of select="svfn:objectToObjectURI($linkUriPrefix,$linkedId,api:links/api:link[@type='elements/user']/@id)" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:variable name="personId">
                                <xsl:choose>
                                    <xsl:when test="api:initials and not(api:initials='')">
                                        <xsl:value-of select="concat(fn:lower-case(fn:normalize-space(api:last-name)),'-',fn:lower-case(fn:normalize-space(api:initials)))" />
                                    </xsl:when>
                                    <xsl:when test="api:first-names and not(api:first-names='')">
                                        <xsl:value-of select="concat(fn:lower-case(fn:normalize-space(api:last-name)),'-',fn:substring(fn:lower-case(fn:normalize-space(api:first-names)),1,1))" />
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="fn:lower-case(fn:normalize-space(api:last-name))" />
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <xsl:variable name="authorshipId" select="concat($personId, '-', position())" />
                            <xsl:value-of select="svfn:objectToObjectURI($linkUriPrefix, $linkedId, $authorshipId)" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <xsl:variable name="vCardRdf" select="svfn:renderVcardFromApiPerson(., $contextUri)" />
                <xsl:copy-of select="$vCardRdf" />
                <xsl:variable name="vCardUri" select="svfn:retrieveUri($vCardRdf, 'http://www.w3.org/2006/vcard/ns#Individual')" />

                <xsl:call-template name="render_rdf_object">
                    <xsl:with-param name="objectURI" select="$contextUri" />
                    <xsl:with-param name="rdfNodes">
                        <rdf:type rdf:resource="{$contextObjectTypeUri}"/>
                        <xsl:element name="{$contextVCardLinkProperty}">
                            <xsl:attribute name="rdf:resource" select="$vCardUri" />
                        </xsl:element>
                        <xsl:element name="{$contextToObjectLinkProperty}">
                            <xsl:attribute name="rdf:resource" select="$linkedUri" />
                        </xsl:element>
                        <xsl:if test="normalize-space($descriptor) != ''">
                            <rdfs:label><xsl:value-of select="$descriptor" /></rdfs:label>
                        </xsl:if>
                        <vivo:rank rdf:datatype="http://www.w3.org/2001/XMLSchema#int"><xsl:value-of select="position()" /></vivo:rank>
                    </xsl:with-param>
                </xsl:call-template>

                <xsl:call-template name="render_rdf_object">
                    <xsl:with-param name="objectURI" select="$linkedUri" />
                    <xsl:with-param name="rdfNodes">
                        <xsl:element name="{$objectToContextLinkProperty}">
                            <xsl:attribute name="rdf:resource" select="$contextUri" />
                        </xsl:element>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:for-each>
        </xsl:if>
    </xsl:function>

    <xsl:function name="svfn:renderVcardFromApiPerson">
        <xsl:param name="person" />
        <xsl:param name="personUri" />

        <xsl:if test="$person/* and $personUri">
            <xsl:variable name="vcardUri" select="concat($personUri, '-vcard')" />
            <xsl:variable name="vcardNameUri" select="concat($personUri,'-vcardname')" />
            <xsl:variable name="vcardFormattedNameUri" select="concat($personUri,'-vcardfname')" />
            <xsl:variable name="vcardFormattedNameObject" select="svfn:renderVcardFormattedNameObject($person/api:first-names[1], $person/api:last-name, $person/api:initials, $vcardFormattedNameUri)" />

            <!-- Create person vcard object -->
            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$vcardUri" />
                <xsl:with-param name="rdfNodes">
                    <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Individual"/>
                    <vcard:hasName rdf:resource="{$vcardNameUri}"/>
                    <xsl:if test="$vcardFormattedNameObject">
                        <vcard:hasFormattedName rdf:resource="{$vcardFormattedNameUri}" />
                    </xsl:if>
                </xsl:with-param>
            </xsl:call-template>

            <!-- write out the formatted name if present -->
            <xsl:copy-of select="$vcardFormattedNameObject" />

            <!-- Create person vcard name object -->
            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$vcardNameUri" />
                <xsl:with-param name="rdfNodes">
                    <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Name"/>
                    <xsl:choose>
                        <xsl:when test="$person/api:first-names">
                            <vcard:givenName><xsl:value-of select="$person/api:first-names" /></vcard:givenName>
                        </xsl:when>
                        <xsl:when test="$person/api:initials">
                            <vcard:givenName><xsl:value-of select="$person/api:initials" /></vcard:givenName>
                        </xsl:when>
                    </xsl:choose>
                    <vcard:familyName><xsl:value-of select="$person/api:last-name" /></vcard:familyName>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:function>

    <xsl:function name="svfn:renderVcardFormattedNameObject">
        <xsl:param name="firstName" as="xs:string?"  />
        <xsl:param name="lastName" as="xs:string" />
        <xsl:param name="initials" as="xs:string?" />
        <xsl:param name="vcardFormattedNameURI" as="xs:string" />

        <xsl:call-template name="render_rdf_object">
            <xsl:with-param name="objectURI" select="$vcardFormattedNameURI" />
            <xsl:with-param name="rdfNodes">
                <xsl:call-template name="_concat_nodes_if">
                    <xsl:with-param name="nodesRequired" select="/..">
                        <!--<vcard:formattedName><xsl:value-of select="normalize-space(concat($lastName, ' ', $initials))" /></vcard:formattedName>-->
                    </xsl:with-param>
                    <xsl:with-param name="nodesToAdd">
                        <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#FormattedName" />
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:function>

    <xsl:function name="svfn:renderControlledSubjectLinks">
        <xsl:param name="object" />
        <xsl:param name="objectUri" as="xs:string" />
        <xsl:param name="origin" />
        <!-- grab the labels off the object in question -->
        <xsl:variable name="allLabels" select="$object/api:all-labels" />
        <!-- if there are no mapped schemes then the for each will not trigger -->
        <xsl:for-each select="$label-schemes[@for=$object/@category]/config:label-scheme">
            <xsl:variable name="scheme-name" select="fn:normalize-space(./@name)" />
            <xsl:variable name="target-type" select="fn:normalize-space(./@target)" />
            <xsl:variable name="target-property" select="fn:normalize-space(./@target-property)" />
            <xsl:variable name="target-ontology" select="fn:normalize-space(svfn:expandSpecialNames(./@target-ontology))" />

            <xsl:for-each select="fn:distinct-values($allLabels/api:keywords/api:keyword[@scheme=$scheme-name and ($origin = '' or @origin = $origin)])">
                <xsl:variable name="targetObjectUri" >
                    <xsl:choose>
                        <xsl:when test="$target-type = 'geographic-focus'">
                            <xsl:variable name="label-value" select="." />
                            <xsl:value-of select="$country-types/config:country-type[@name = $label-value][1]/@uri" />
                            <!--<xsl:value-of select="$country-types/config:country-type[@name = $label-value]/@uri" />-->
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="svfn:makeURI(concat('vocab-',$scheme-name,'-'),.)" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:if test="fn:normalize-space($targetObjectUri) != ''">
                    <xsl:choose>
                        <xsl:when test="$target-type = 'research-areas'">
                            <vivo:hasResearchArea>
                                <xsl:call-template name="render_rdf_object">
                                    <xsl:with-param name="objectURI" select="$targetObjectUri" />
                                    <xsl:with-param name="rdfNodes">
                                        <vivo:researchAreaOf rdf:resource="{$objectUri}" />
                                    </xsl:with-param>
                                </xsl:call-template>
                            </vivo:hasResearchArea>
                        </xsl:when>
                        <xsl:when test="$target-type = 'geographic-focus'">
                            <vivo:geographicFocus>
                                <xsl:call-template name="render_rdf_object">
                                    <xsl:with-param name="objectURI" select="$targetObjectUri" />
                                    <xsl:with-param name="rdfNodes">
                                        <vivo:geographicFocusOf rdf:resource="{$objectUri}" />
                                    </xsl:with-param>
                                </xsl:call-template>
                            </vivo:geographicFocus>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:choose>
                                <xsl:when test="$target-property and $target-ontology">
                                    <xsl:element namespace="{$target-ontology}" name="{$target-property}">
                                        <xsl:call-template name="render_rdf_object">
                                            <xsl:with-param name="objectURI" select="$targetObjectUri" />
                                            <xsl:with-param name="rdfNodes">
                                                <!-- we do not know the inverse property - so rely on inferencing -->
                                                <!-- add type here to ensure we output something from render_rdf_object -->
                                                <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#Concept" />
                                            </xsl:with-param>
                                        </xsl:call-template>
                                    </xsl:element>
                                </xsl:when>
                                <xsl:otherwise>
                                    <vivo:hasSubjectArea>
                                        <xsl:call-template name="render_rdf_object">
                                            <xsl:with-param name="objectURI" select="$targetObjectUri" />
                                            <xsl:with-param name="rdfNodes">
                                                <vivo:subjectAreaOf rdf:resource="{$objectUri}" />
                                            </xsl:with-param>
                                        </xsl:call-template>
                                    </vivo:hasSubjectArea>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:function>

    <xsl:function name="svfn:expandSpecialNames">
        <xsl:param name="input" />
        <xsl:choose>
            <xsl:when test="starts-with($input, '$$base-uri-prefix$$')">
                <xsl:value-of select="concat($validatedBaseURIPrefix, fn:substring-after($input, '$$base-uri-prefix$$'))" />
            </xsl:when>
            <xsl:when test="starts-with($input, '$$base-uri$$')">
                <xsl:value-of select="concat($validatedBaseURI, fn:substring-after($input, '$$base-uri$$'))" />
            </xsl:when>
            <xsl:when test="starts-with($input, '$$local$$')">
                <xsl:value-of select="concat($validatedLocalOntologyURI, fn:substring-after($input, '$$local$$'))" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$input" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="svfn:renderControlledSubjectObjects">
        <xsl:param name="object" />
        <!-- grab the labels off the object in question -->
        <xsl:variable name="allLabels" select="$object/api:all-labels" />
        <!-- if there are no mapped schemes then the for each will not trigger -->
        <xsl:for-each select="$label-schemes[@for=$object/@category]/config:label-scheme">
            <xsl:variable name="schemeDefinedBy" select="svfn:expandSpecialNames(./@defined-by)" />
            <xsl:if test="not($schemeDefinedBy='')">
                <xsl:variable name="scheme-name" select="./@name" />
                <xsl:variable name="target" select="./@target" />
                <xsl:if test="not($target) or $target != 'geographic-focus'">
                    <xsl:for-each select="fn:distinct-values($allLabels/api:keywords/api:keyword[@scheme=$scheme-name])">
                        <xsl:variable name="definitionUri" select="svfn:makeURI(concat('vocab-',$scheme-name,'-'),.)" />
                        <xsl:call-template name="render_rdf_object">
                            <xsl:with-param name="objectURI" select="$definitionUri" />
                            <xsl:with-param name="rdfNodes">
                                <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#Concept" />
                                <rdfs:label>
                                    <xsl:value-of select="svfn:getLabelOrOverride($definitionUri ,.)" />
                                </rdfs:label>
                                <rdfs:isDefinedBy rdf:resource="{$schemeDefinedBy}" />
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:for-each>
                </xsl:if>
            </xsl:if>
        </xsl:for-each>
    </xsl:function>

    <xsl:function name="svfn:getLabelOrOverride">
        <xsl:param name="objectUri" />
        <xsl:param name="defaultLabelValue" />

        <xsl:variable name="shortenedUri" select="substring-after($objectUri, $validatedBaseURI )" />
        <xsl:choose>
            <xsl:when test="$label-overrides[@uri=$shortenedUri]/@label">
                <xsl:value-of select="$label-overrides[@uri=$shortenedUri][1]/@label" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$defaultLabelValue" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="svfn:userPhotoDescription">
        <xsl:param name="userURI" />
        <xsl:param name="fullImageFilename" />
        <xsl:param name="fullImagePathUrl" />
        <xsl:param name="thumbnailImageFilename" />
        <xsl:param name="thumbnailImagePathUrl" />

        <rdf:Description rdf:about="{$userURI}">
            <vitro-public:mainImage rdf:resource="{$userURI}-image"/>
        </rdf:Description>
        <rdf:Description rdf:about="{$userURI}-image">
            <rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
            <rdf:type rdf:resource="http://vitro.mannlib.cornell.edu/ns/vitro/public#File" />
            <vitro-public:downloadLocation rdf:resource="{$userURI}-imageDownload"/>
            <vitro-public:thumbnailImage rdf:resource="{$userURI}-imageThumbnail"/>
            <vitro-public:filename><xsl:value-of select="$fullImageFilename" /></vitro-public:filename>
            <vitro-public:mimeType>image/jpeg</vitro-public:mimeType>
        </rdf:Description>
        <rdf:Description rdf:about="{$userURI}-imageDownload">
            <rdf:type rdf:resource="http://vitro.mannlib.cornell.edu/ns/vitro/public#FileByteStream" />
            <vitro-public:directDownloadUrl><xsl:value-of select="$fullImagePathUrl" /></vitro-public:directDownloadUrl>
        </rdf:Description>
        <rdf:Description rdf:about="{$userURI}-imageThumbnail">
            <rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
            <rdf:type rdf:resource="http://vitro.mannlib.cornell.edu/ns/vitro/public#File"/>
            <vitro-public:downloadLocation rdf:resource="{$userURI}-imageThumbnailDownload"/>
            <vitro-public:filename><xsl:value-of select="$thumbnailImageFilename" /></vitro-public:filename>
            <vitro-public:mimeType>image/jpeg</vitro-public:mimeType>
        </rdf:Description>
        <rdf:Description rdf:about="{$userURI}-imageThumbnailDownload">
            <rdf:type rdf:resource="http://vitro.mannlib.cornell.edu/ns/vitro/public#FileByteStream"/>
            <vitro-public:directDownloadUrl><xsl:value-of select="$thumbnailImagePathUrl" /></vitro-public:directDownloadUrl>
        </rdf:Description>
    </xsl:function>

    <!-- ======================================
         Template Library
         ======================================- -->

    <!-- _render_rdf_document -->
    <xsl:template name="render_rdf_document">
        <xsl:param name="rdfNodes" />

        <xsl:if test="$rdfNodes/*">
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                     xmlns:vivo="http://vivoweb.org/ontology/core#"
                     xmlns:dc="http://purl.org/dc/elements/1.1/"
                     xmlns:foaf="http://xmlns.com/foaf/0.1/"
                     xmlns:owl="http://www.w3.org/2002/07/owl#"
                     xmlns:owlPlus="http://www.w3.org/2006/12/owl2-xml#"
                     xmlns:score="http://vivoweb.org/ontology/score#"
                     xmlns:skos="http://www.w3.org/2008/05/skos#"
                     xmlns:swvocab="http://www.w3.org/2003/06/sw-vocab-status/ns#"
                     xmlns:ufVivo="http://vivo.ufl.edu/ontology/vivo-ufl/"
                     xmlns:vitro="http://vitro.mannlib.cornell.edu/ns/vitro/0.7#"
                     xmlns:vitro-public="http://vitro.mannlib.cornell.edu/ns/vitro/public#"
                     xmlns:vocab="http://purl.org/vocab/vann/"
                     xmlns:symp="http://www.symplectic.co.uk/ontology/elements/"
                    >
                <xsl:copy-of select="$rdfNodes" />
            </rdf:RDF>
        </xsl:if>
    </xsl:template>

    <!-- _render_rdf_object -->
    <xsl:template name="render_rdf_object">
        <xsl:param name="rdfNodes" />
        <xsl:param name="objectURI" />

        <xsl:if test="$rdfNodes/*">
            <rdf:Description rdf:about="{$objectURI}">
                <xsl:copy-of select="$rdfNodes" />
                <xsl:if test="$harvestedBy!=''">
                    <ufVivo:harvestedBy><xsl:value-of select="$harvestedBy" /></ufVivo:harvestedBy>
                </xsl:if>
            </rdf:Description>
        </xsl:if>
    </xsl:template>

    <xsl:template name="render_empty_rdf">
        <rdf:RDF></rdf:RDF>
    </xsl:template>

    <xsl:template name="_concat_nodes_if">
        <xsl:param name="nodesRequired" />
        <xsl:param name="nodesToAdd" />

        <xsl:if test="$nodesRequired/*">
            <xsl:copy-of select="$nodesRequired" />
            <xsl:copy-of select="$nodesToAdd" />
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>

