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
<!--suppress XmlUnusedNamespaceDeclaration -->
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
    <xsl:import href="elements-to-vivo-datatypes-matching.xsl" />
    <xsl:import href="elements-to-vivo-fuzzy-matching.xsl" />

    <!-- ======================================
         Function Library
         ======================================- -->

    <!--
        svfn:boolValue
        ==============
        Attempt to extract a suitable boolean from the incoming param, whether it is a string or something else..
        Treats the string "true" ignoring casing as true and all other strings as false..
     -->
    <xsl:function name="svfn:boolValue">
        <xsl:param name="input" as="item()" />
        <xsl:choose>
            <xsl:when test="$input instance of xs:string">
                <xsl:copy-of select="fn:lower-case(fn:normalize-space($input)) = 'true'" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="boolean($input)" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>


    <!--
        svfn:getOrganizationType
        ========================
        Get mapped Org type based on configuration - if no specific mapping found try to infer.

        NOTE: not currently used - could however be swapped in within svfn:inferGroupType if so desired..
    -->
    <xsl:function name="svfn:getOrganizationType">
        <xsl:param name="name" as="xs:string?"/>
        <xsl:param name="orgOverridesToConsider" />
        <xsl:param name="defaultType" />

        <xsl:variable name="orgOverride" select="svfn:getOrganisationOverride($name, $orgOverridesToConsider)" />

        <xsl:variable name="intermediateOrgName">
            <xsl:choose>
                <xsl:when test="$orgOverride/@name"><xsl:value-of select="$orgOverride/@name" /></xsl:when>
                <xsl:otherwise><xsl:value-of select="$name" /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="$orgOverride/@type"><xsl:value-of select="$orgOverride/@type" /></xsl:when>
            <xsl:otherwise><xsl:value-of select="svfn:inferOrganizationType($intermediateOrgName, $defaultType)" /></xsl:otherwise>
        </xsl:choose>
    </xsl:function>


    <!--
       svfn:_inferOrganizationType
       ===========================
       Internal function to infer Vivo org type for the incoming "name" based on pre-defined rules.
   -->
    <xsl:function name="svfn:_inferOrganizationType">
        <xsl:param name="name" />

        <xsl:variable name="candidateTypes">
            <candidateTypes>
                <candidateType name="University" uri="http://vivoweb.org/ontology/core#University" />
                <candidateType name="College" uri="http://vivoweb.org/ontology/core#College" />
                <candidateType name="Museum" uri="http://vivoweb.org/ontology/core#Museum" />
                <candidateType name="Hospital" uri="http://vivoweb.org/ontology/core#Hospital" />
                <candidateType name="Institute" uri="http://vivoweb.org/ontology/core#Institute" />
                <candidateType name="School" uri="http://vivoweb.org/ontology/core#School" />
                <candidateType name="Association" uri="http://vivoweb.org/ontology/core#Association" />
                <candidateType name="Library" uri="http://vivoweb.org/ontology/core#Library" />
                <candidateType name="Foundation" uri="http://vivoweb.org/ontology/core#Foundation" />
                <candidateType name="Ltd" uri="http://vivoweb.org/ontology/core#PrivateCompany" />
            </candidateTypes>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="$candidateTypes/candidateTypes/candidateType[contains($name,@name)]">
                <xsl:copy-of select="$candidateTypes/candidateTypes/candidateType[contains($name,@name)][1]" />
            </xsl:when>
            <xsl:otherwise><xsl:copy-of select="/.." /></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
       svfn:inferOrganizationType
       ===========================
       Function to infer Vivo org type for the incoming "name" based on pre-defined rules.
       Will return the passed in "defaultType" if no type can be inferred..
   -->
    <xsl:function name="svfn:inferOrganizationType">
        <xsl:param name="name" />
        <xsl:param name="defaultType" />

        <xsl:variable name="inferredType" select="svfn:_inferOrganizationType($name)" />
        <xsl:choose>
            <xsl:when test="$inferredType/@uri"><xsl:value-of select ="$inferredType/@uri" /></xsl:when>
            <xsl:otherwise><xsl:value-of select="$defaultType" /></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
        svfn:orgNameDifference
        ======================
        Function to inspect two strings (represnting organisation names) and analyse them for similarity.
        This operates by stemming common "stop words" and punctuation from the incoming strings, sorting the remaining words alphabetically.
        The number of remaining words from the two incoming strings are calculated and a numerical representation of the similarity
        of the remaining words is calculated.
        This information is then returned as an XML element:
           <difference noOfSignificantWords1="#coutn1" noOfSignificantWords2="#count2">#similarityScore</difference>
        The similarity score is calculated based on svfn:compare-strings from elements-to-vivo-fuzzy-matching.xsl

        This behaviour is designed to deal with representations like "Lilliput University" and "The University of Lilliput"
        but will no doubt introduce a certain rate of false positives, and potentially other issues.
    -->
    <xsl:function name="svfn:orgNameDifference">
        <xsl:param name="orgName" as="xs:string" />
        <xsl:param name="comparisonName" as="xs:string" />

        <xsl:variable name="stopWords">
            <stop-words>
                <stop-word>the</stop-word>
                <stop-word>and</stop-word>
                <stop-word>a</stop-word>
                <stop-word>or</stop-word>
                <stop-word>of</stop-word>
                <stop-word>for</stop-word>
                <stop-word>&amp;</stop-word>
                <stop-word>ltd</stop-word>
            </stop-words>
        </xsl:variable>

        <!--<xsl:variable name="name1Tokens" select="(fn:tokenize(fn:lower-case(fn:normalize-space($orgName)), '[\s\.\(\),-_]'))" />-->
        <xsl:variable name="name1Tokens" select="(fn:tokenize(fn:lower-case(fn:normalize-space($orgName)), $wordBreakRegex))" />
        <xsl:variable name="name1PatternPrimer">
            <xsl:for-each select="$name1Tokens">
                <xsl:sort select="." />
                <xsl:if test="not($stopWords/stop-words/stop-word[text() = current()])">
                    <xsl:value-of select="." />
                </xsl:if>
                <xsl:text> </xsl:text>
            </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="name1PatternIntermediate" select="fn:replace($name1PatternPrimer, concat($extraneousPunctuation, '+'), '')" />
        <xsl:variable name="name1WordCount" select="count(fn:tokenize(fn:normalize-space($name1PatternIntermediate), ' '))" />
        <xsl:variable name="name1Pattern" select="fn:replace($name1PatternIntermediate, ' ', '')" />

        <!--<xsl:variable name="name2Tokens" select="fn:tokenize(fn:lower-case(fn:normalize-space($comparisonName)), '[\s\.\(\),-_]')" />-->
        <xsl:variable name="name2Tokens" select="fn:tokenize(fn:lower-case(fn:normalize-space($comparisonName)), $wordBreakRegex)" />
        <xsl:variable name="name2PatternPrimer">
            <xsl:for-each select="$name2Tokens">
                <xsl:sort select="." />
                <xsl:if test="not($stopWords/stop-words/stop-word[text() = current()])">
                    <xsl:value-of select="." />
                </xsl:if>
                <xsl:text> </xsl:text>
            </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="name2PatternIntermediate" select="fn:replace($name2PatternPrimer, concat($extraneousPunctuation, '+'), '')" />
        <xsl:variable name="name2WordCount" select="count(fn:tokenize(fn:normalize-space($name2PatternIntermediate), ' '))" />
        <xsl:variable name="name2Pattern" select="fn:replace($name2PatternIntermediate, ' ', '')" />

        <!--<xsl:value-of select="svfn:compare-strings($name1Pattern, $name2Pattern)" />-->
        <difference noOfSignificantWords1="{$name1WordCount}" noOfSignificantWords2="{$name2WordCount}"><xsl:value-of select="svfn:compare-strings($name1Pattern, $name2Pattern)" /></difference>
    </xsl:function>


    <!--
        svfn:isNameDifferenceCloseEnough
        ======================
        Helper function to decide if a name "difference"(as defined by svfn:orgNameDifference) should be considered close enough
        to be treated as a match. "fuzzyMatchLoose" and "fuzzyMatchTight" parameters are the barriers applied depending on
        whether the compared words contained the same number of "meaningful" words or not...
        Note: This could be adapted to instead use a mathematical function based on the difference in the number of words..
    -->
    <xsl:function name="svfn:isNameDifferenceCloseEnough" as="xs:boolean">
        <xsl:param name="difference" />
        <xsl:param name="fuzzyMatchLoose" as="xs:float" />
        <xsl:param name="fuzzyMatchTight" as="xs:float"/>

        <xsl:choose>
            <xsl:when test="$difference/@noOfSignificantWords1 = $difference/noOfSignificantWords2">
                <xsl:value-of select="$difference/text() &gt;= $fuzzyMatchLoose" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$difference/text() &gt;= $fuzzyMatchTight" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
        svfn:compareOrgNames
        ====================
        Function to compare two strings representing organisation names and decide if they should be treated as a "match"
        Operates in two stages:
        1) Are the raw names, as presented close enough to be thought of as a match
        2) If one name contains a word that can be used to infer the "type" of the organisation, but the other does not..
           Tack the inferred "type" from one onto the other and see if that is a close enough match

        This behaviour is designed to deal with representations like "Lilliput University" and "Lilliput",
        but will no doubt introduce a certain rate of false positives, and potentially other issues.
    -->
    <xsl:function name="svfn:compareOrgNames" as="xs:boolean">
        <xsl:param name="orgName" as="xs:string" />
        <xsl:param name="comparisonName" as="xs:string" />

        <xsl:variable name="fuzzyMatchLoose" select="0.8" />
        <xsl:variable name="fuzzyMatchTight" select="0.95" />

        <xsl:variable name="result1" select="svfn:orgNameDifference($orgName, $comparisonName)" />
        <xsl:choose>
            <xsl:when test="svfn:isNameDifferenceCloseEnough($result1, $fuzzyMatchLoose, $fuzzyMatchTight)">
                <xsl:value-of  select="true()" />
            </xsl:when>
            <xsl:otherwise>
                <!-- deliberately use internal "svfn:_inferOrganizationType" so that we get nothing back if no inference can be made -->
                <xsl:variable name="orgInferredType" select="svfn:_inferOrganizationType($orgName)" />
                <xsl:variable name="compInferredType" select="svfn:_inferOrganizationType($comparisonName)" />
                <xsl:choose>
                    <xsl:when test="$orgInferredType and not($compInferredType)">
                        <xsl:variable name="result2" select="svfn:orgNameDifference($orgName, concat($comparisonName, ' ', $orgInferredType/@name))" />
                        <xsl:value-of select="svfn:isNameDifferenceCloseEnough($result2, $fuzzyMatchLoose, $fuzzyMatchTight)" />
                    </xsl:when>
                    <xsl:when test="$compInferredType and not($orgInferredType)">
                        <xsl:variable name="result3" select="svfn:orgNameDifference(concat($orgName, ' ', $compInferredType/@name), $comparisonName)" />
                        <xsl:value-of select="svfn:isNameDifferenceCloseEnough($result3, $fuzzyMatchLoose, $fuzzyMatchTight)" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of  select="false()" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
        svfn:getOrganisationOverride
        =======================
        Function to analyse the incoming "orgName" argument and compare it to the org-unit configuration settings in "orgOverridesToConsider"
        Will always return the full XML description of the matching org-unit if one is found - or an empty sequence if no match is found.
        Matching occurs in three stages with later stages only attempted if earlier ones fail:
        1) Exact match between "orgName" and a unit's "name" (case insensitive)
        2) Exact ma of one of a unit's "alias" child elements (case insensitive)
        3) Fuzzy match to the "orgName" as long as there are no matches between "orgName and the value of the values of any child "non-match" elements (case insensitive)
        The fuzzy matching occurs according to the logic within svfn:compareOrgNames
    -->
    <xsl:function name="svfn:getOrganisationOverride">
        <xsl:param name="orgName" as="xs:string?"/>
        <xsl:param name="orgOverridesToConsider" />

        <xsl:choose>
            <!-- if there is an override with the same name (case insensitive match) then use that -->
            <xsl:when test="$orgOverridesToConsider/config:org-unit[fn:lower-case(@name) = fn:lower-case($orgName)]">
                <xsl:copy-of select="$orgOverridesToConsider/config:org-unit[fn:lower-case(@name) = fn:lower-case($orgName)][1]" />
            </xsl:when>
            <!-- if there is an override with an alias (case insensitive match) then use that -->
            <xsl:when test="$orgOverridesToConsider/config:org-unit[config:alias/fn:lower-case(text()) = fn:lower-case($orgName)]">
                <xsl:copy-of select="$orgOverridesToConsider/config:org-unit[config:alias/fn:lower-case(text()) = fn:lower-case($orgName)][1]" />
            </xsl:when>
            <xsl:when test="$orgOverridesToConsider/config:org-unit[count(config:non-match[fn:lower-case(text()) = fn:lower-case($orgName)]) = 0 and svfn:compareOrgNames(@name, $orgName)]">
                <xsl:copy-of select="$orgOverridesToConsider/config:org-unit[count(config:non-match[fn:lower-case(text()) = fn:lower-case($orgName)]) = 0 and svfn:compareOrgNames(@name, $orgName)][1]" />
            </xsl:when>
        </xsl:choose>

    </xsl:function>

    <!--
        svfn:getOrgInfoFromName
        =======================
        Function to analyse the incoming "name" argument and compare it to the org-unit configuration settings in "orgOverridesToConsider"
        Will always return an "org-info" elements with a "name" attribute containing the "resolved" name after any matching has occurred.
        (Note: matching defined by behaviour of "getOrganisationOverride")...
        If the resolved name also matches to an Elements "group" it will also report matched-group-name and matched-group-id attributes
        (Note : this is only attempted if "matchToGroups" is true)...
        If a matching "org-unit" was discovered in config, it's XML description will be returned (in full) within the org-info element.
        Note: if the incoming "name" is empty - this will ultimately return an empty sequence..
    -->
    <xsl:function name="svfn:getOrgInfoFromName">
        <xsl:param name="name" as="xs:string?" />
        <xsl:param name="orgOverridesToConsider" />
        <xsl:param name="matchToGroups" as="xs:boolean" />

        <xsl:variable name="groupsToMatchAgainst" select="svfn:getNodeOrLoad($elementsGroupList)"/>

        <xsl:if test="$name">
            <xsl:variable name="orgOverride" select="svfn:getOrganisationOverride($name, $orgOverridesToConsider)" />

            <xsl:variable name="intermediateOrgName">
                <xsl:choose>
                    <xsl:when test="$orgOverride/@name"><xsl:value-of select="$orgOverride/@name" /></xsl:when>
                    <xsl:otherwise><xsl:value-of select="$name" /></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:variable name="matchedGroup">
                <xsl:choose>
                    <xsl:when test="$matchToGroups and $groupsToMatchAgainst/descendant::group">
                        <xsl:choose>
                            <xsl:when test="$groupsToMatchAgainst/descendant::group[fn:lower-case(@name) = fn:lower-case($intermediateOrgName)]">
                                <xsl:sequence select="$groupsToMatchAgainst/descendant::group[fn:lower-case(@name) = fn:lower-case($intermediateOrgName)][1]" />
                            </xsl:when>
                            <xsl:otherwise>
                                <!--<xsl:sequence select="$groupsToMatchAgainst/descendant::group[svfn:compare-strings(@name, $intermediateOrgName) > 0.95][1]" />-->
                                <xsl:sequence select="$groupsToMatchAgainst/descendant::group[svfn:orgNameDifference(@name, $intermediateOrgName) > 0.95][1]" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:sequence select="/.." />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:if test="normalize-space($intermediateOrgName) != ''">
                <orgInfo name="{$intermediateOrgName}">
                    <xsl:if test="$matchedGroup/group/@id">
                        <xsl:attribute name="matched-group-name" select="$matchedGroup/group/@name" />
                        <xsl:attribute name="matched-group-id" select="$matchedGroup/group/@id" />
                    </xsl:if>
                    <xsl:copy-of select="$orgOverride" />
                </orgInfo>
            </xsl:if>
        </xsl:if>
    </xsl:function>

    <!-- Get publication type statements from the XML configuration (for the type supplied as a parameter) -->
    <xsl:function name="svfn:getTypesForPublication">
        <xsl:param name="object"  />

        <xsl:variable name="type" select="$object/@type" />

        <xsl:variable name="chosenRecordPrecedenceName" select="$publication-types/@recordPrecedenceToUse" />
        <xsl:variable name="matchAcrossRecordsAsBackstop">
            <xsl:choose>
                <xsl:when test="$publication-types/@matchAcrossRecordsAsBackstop">
                    <xsl:value-of select="svfn:boolValue($publication-types/@matchAcrossRecordsAsBackstop)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="false()" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="precedence-to-use">
            <xsl:choose>
                <xsl:when test="$record-precedences[@for=$chosenRecordPrecedenceName]"><xsl:value-of select="$chosenRecordPrecedenceName" /></xsl:when>
                <xsl:when test="$record-precedences[@for=$object/@category]"><xsl:value-of select="$object/@category" /></xsl:when>
                <xsl:otherwise><xsl:value-of select="'default'" /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

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
                <xsl:variable name="requiredVerificationStatus">
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
                    <xsl:when test="$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificationStatus = 'any' or api:verification-status = $requiredVerificationStatus)]" >
                        <xsl:variable name="restricted-object">
                            <api:object>
                                <api:records>
                                    <xsl:copy-of select= "$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificationStatus = 'any' or api:verification-status = $requiredVerificationStatus)][1]" />
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
            <xsl:when test="$uriAliases[@alias=$calculatedURISuffix]">
                <xsl:value-of select="concat($validatedBaseURI, $uriAliases[@alias=$calculatedURISuffix][1])" />
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
        <xsl:param name="objectId1" as="xs:string" />
        <xsl:param name="objectId2" as="xs:string" />

        <xsl:value-of select="svfn:makeURI($prefix,concat($objectId1,'-',$objectId2))" />
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


    <!--
    svfn:usersPreferredNickName
    ======================
    The user's preferred first name
    -->
    <xsl:function name="svfn:usersPreferredNickName">
        <xsl:param name="user" />
        <xsl:choose>
            <xsl:when test="$user/api:user-preferred-first-name and normalize-space($user/api:user-preferred-first-name) != ''">
                <xsl:value-of select="$user/api:user-preferred-first-name" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$user/api:known-as" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
    svfn:usersPreferredFirstName
    ======================
    The user's preferred first name
    -->
    <xsl:function name="svfn:usersPreferredFirstName">
        <xsl:param name="user" />

        <xsl:variable name="usersPreferredNickName" select="svfn:usersPreferredNickName($user)" />
        <xsl:choose>
            <xsl:when test="$usersPreferredNickName and normalize-space($usersPreferredNickName) != ''">
                <xsl:value-of select="$usersPreferredNickName" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$user/api:first-name" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
    svfn:usersPreferredLastName
    ======================
    default of how a user should be described in a "label"
    -->
    <xsl:function name="svfn:usersPreferredLastName">
        <xsl:param name="user" />
        <!--<xsl:variable name="generic-pref-surname-field-number" select="4" />-->
        <xsl:choose>
            <xsl:when test="$user/api:user-preferred-last-name and normalize-space($user/api:user-preferred-last-name) != ''">
                <xsl:value-of select="$user/api:user-preferred-last-name" />
            </xsl:when>
            <!--<xsl:when test="$user/api:organisation-defined-data[@field-number = $generic-pref-surname-field-number] and normalize-space($user/api:organisation-defined-data[@field-number = $generic-pref-surname-field-number]) != ''">-->
                <!--<xsl:value-of select="$user/api:organisation-defined-data[@field-number = $generic-pref-surname-field-number]" />-->
            <!--</xsl:when>-->
            <xsl:otherwise>
                <xsl:value-of select="$user/api:last-name" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
    svfn:userLabel
    ======================
    default of how a user should be described in a "label"
    -->
    <xsl:function name="svfn:userLabel">
        <xsl:param name="user" />
        <xsl:value-of select="concat(svfn:usersPreferredLastName($user), ', ', svfn:usersPreferredFirstName($user))" />
    </xsl:function>


    <!--
        _applyCorrections
        ======================
        Recursive template used to apply a set of corrections to the passed in "stringToCorrect" based on the data in "correctionsToApply"
        The "correctionsToApply" should be provided as a "corrections" XML element like this:
         <corrections>
            <correction original= "Univerity" altered="University" />
            <correction original= "Univeristy" altered="University" />
        </corrections>
    -->
    <xsl:template name="_applyCorrections">
        <xsl:param name="stringToCorrect" as="xs:string"/>
        <xsl:param name="correctionsToApply" />
        <xsl:param name="position" as="xs:integer" select="1"/>

        <xsl:variable name="noOfCorrections" select="count($correctionsToApply/corrections/correction)" />

        <xsl:choose>
            <xsl:when test="$position &lt;= $noOfCorrections">
                <xsl:variable name="correction" select="$correctionsToApply/corrections/correction[$position]" />
                <xsl:variable name="partiallyCorrectedString" select="fn:replace($stringToCorrect, fn:concat('(^|',$wordBreakRegex,')',$correction/@original, '($|',$wordBreakRegex,')'), fn:concat('$1', $correction/@altered,'$2'))" />

                <xsl:call-template name="_applyCorrections">
                    <xsl:with-param name="stringToCorrect" select="$partiallyCorrectedString" />
                    <xsl:with-param name="correctionsToApply" select="$correctionsToApply" />
                    <xsl:with-param name="position" select="$position + 1" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$stringToCorrect" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!--
        svfn:applyAddressDataEntryCorrections
        =====================================
        Applies a set of corrections to the incoming "stringToCorrect" that are meant to be relevant to manually entered
        address data...the current set are defined in the variable "address-data-entry-corrections" in ...-config.xsl
    -->
    <xsl:function name="svfn:applyAddressDataEntryCorrections">
        <xsl:param name="stringToCorrect" as="xs:string"/>

        <xsl:call-template name="_applyCorrections">
            <xsl:with-param name="stringToCorrect" select="$stringToCorrect" />
            <xsl:with-param name="correctionsToApply" select="$address-data-entry-corrections" />
        </xsl:call-template>
    </xsl:function>

    <!--
        svfn:extractDeptNameFromAddress
        ===============================
        Function to define how to extract department name information, for later use in matching, from an Elements address
    -->
    <xsl:function name="svfn:extractDeptNameFromAddress">
        <xsl:param name="address" />
        <xsl:value-of select="$address/api:line[@type='suborganisation']" />
    </xsl:function>

    <!--
        svfn:extractInstNameFromAddress
        ===============================
        Function to define how to extract institution name information, for later use in matching, from an Elements address
    -->
    <xsl:function name="svfn:extractInstNameFromAddress">
        <xsl:param name="address" />
        <xsl:variable name="intermediateInstName">
            <xsl:choose>
                <xsl:when test="$address/api:line[@type='organisation']">
                    <xsl:value-of select="$address/api:line[@type='organisation']" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$address/api:line[@type='name']" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:value-of select="svfn:applyAddressDataEntryCorrections($intermediateInstName)" />
    </xsl:function>

    <!--
        svfn:extractCountryCodeFromAddress
        ==================================
        Function to define how to extract country iso code information, for later use in matching, from an Elements address
    -->
    <xsl:function name="svfn:extractCountryCodeFromAddress">
        <xsl:param name="address" />
        <xsl:value-of select="fn:normalize-space($address/@iso-country-code)" />
    </xsl:function>


    <!--
        svfn:getOrgInfosFromAddress
        ===========================
        NEEDS REWORK? - LIKELY COULD BE COMBINED WITH svfn:organisationObjects

        Function to take an incoming Elements address field and match/compare it to the configured org-units and return information about
        the org-info's that are in this address.

        Operates by extracting institution and department name information and country-code info from the address (using the extract functions above.
        The country-code info (if present) is used to restrict the set of configured org-units to compare against..
        svfn:getOrgInfoFromName is then used to get an orgInfo from the instName and deptName.
        (matching to the configured org-units and Elements groups happens here)
        Note: for deptName the set of potential org-units is restricted to the child org-units of the matched org-info of the instName.

        The function always returns an orgInfos element like this:
         <orgInfos deptName="#extractedDeptName" instName="#extractedInstName" country-code="#extractedCountryCode">
            ...see below...
         </orgInfos>
         Note: the attributes of the top level function relate to the raw extracted data from the address - nothing to do with comparison or matching.

        For the content:
        If all the available data (noting that deptName may not always exist) has an org-Info and they are all matched to an Elements group.
            The code will perform some checks:
                that the group representing the institution is a "valid" institution level group
                that the deptGroup is a child of the instGroup, etc.
            If these pass then a "fullMatch" child element is returned containing the "org-info" of the item matched to the lower level Elements group.
            (Here we expect the translation of the group hierarchy from Elements to handle what the "parent" of the matched group should be in Vivo).
        If not all the available data has matched to an Elements group (noting that dept should never do so unless the inst has)
            The system will return a child element for each available piece of data:
                 "matchedDept" for the dept.
                 "matchedInst" for the inst.
            both of which simply contain a copy of the relevant "org-info".
            Note: group matching information is always stripped from the "org-info" in this case apart from in the case where
            the instName has matched to a "valid" institutional level group.

        Note: By default only the top level "Organisation" Elements group is considered to be a valid target for matching to an institution.
        but this can be influenced by configuring "home-institutes" in the config...
    -->
    <xsl:function name="svfn:getOrgInfosFromAddress">
        <xsl:param name="address" />
        <xsl:param name="tryMatchDepartment" as="xs:boolean" />

        <xsl:variable name="countryCode" select="svfn:extractCountryCodeFromAddress($address)" />
        <xsl:variable name="deptName" select="svfn:extractDeptNameFromAddress($address)" />
        <xsl:variable name="instName" select="svfn:extractInstNameFromAddress($address)" />

        <xsl:variable name="overridesToConsider">
            <xsl:choose>
                <xsl:when test="not($countryCode) or $countryCode = ''">
                    <xsl:sequence select="$organization-overrides/config:org-unit" />
                </xsl:when>
                <xsl:otherwise>
                    <!-- order here is so that any that any that actually match the incoming country code from the address will be earlier in the list -->
                    <xsl:sequence select="$organization-overrides/config:org-unit[fn:lower-case(@iso-code) = fn:lower-case($countryCode)]" />
                    <xsl:sequence select="$organization-overrides/config:org-unit[not(@iso-code)]" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="instInfo" select="svfn:getOrgInfoFromName($instName, $overridesToConsider, true())" />
        <!-- for the department we only consider the org-units defined as department-overrides within the matched inst -->
        <xsl:variable name="deptInfo" select="svfn:getOrgInfoFromName($deptName, $instInfo/config:org-unit/config:department-overrides, true())" />

        <xsl:variable name="groupsAllowedToMatchToInst">
            <ids>
                <id>1</id>
            </ids>
        </xsl:variable>

        <!--<xsl:variable name="homeInstitutes" select="$organization-overrides/config:org-unit[@home-institute = 'true']" />-->

        <!-- we only allow matching to the inst group if either it is in the list above, or if there is a matching org-unit flagged as a "home-institute" -->
        <!-- TODO: should this instead just look at the orgInfo in the match to see if it itself is a home-institute -->
        <xsl:variable name="matchedInstIsValid" select="($groupsAllowedToMatchToInst/ids/id[text() = $instInfo/@matched-group-id]) or $organization-overrides/config:org-unit[@home-institute = 'true' and @name = $instInfo/@matched-group-name]" />
        <xsl:variable name="matchedDeptIsValid" select="$matchedInstIsValid and $tryMatchDepartment and $deptInfo/@matched-group-id" />

        <xsl:variable name="groupsToMatchAgainst" select="svfn:getNodeOrLoad($elementsGroupList)"/>
        <xsl:variable name="validParentChild" select = "($instInfo/@matched-group-id = $deptInfo/@matched-group-id) or ($groupsToMatchAgainst/descendant::group[@id = $instInfo/@matched-group-id]/descendant::group[@id = $deptInfo/@matched-group-id])" />

        <!--<xsl:variable name="deptIsChildOfInst" select="$matchedInstIsValid and $deptInfo/@matched-group-id" />-->

        <orgInfos deptName="{$deptName}" instName="{$instName}" country-code="{$countryCode}">
            <xsl:choose>
                <xsl:when test="$matchedInstIsValid and $matchedDeptIsValid and $validParentChild">
                    <fullMatch level="dept"><xsl:copy-of select="$deptInfo" /></fullMatch>
                </xsl:when>
                <xsl:when test="$matchedInstIsValid and ( not($tryMatchDepartment) or not($deptInfo) )">
                    <fullMatch level="inst"><xsl:copy-of select="$instInfo" /></fullMatch>
                </xsl:when>
                <xsl:otherwise>
                    <!-- dept is definitely not to be treated as matched -->
                    <xsl:if test="$tryMatchDepartment">
                        <matchedDept>
                            <xsl:if test="$deptInfo/@name">
                                <orgInfo name="{$deptInfo/@name}" >
                                    <xsl:sequence select="$deptInfo/config:org-unit" />
                                </orgInfo>
                            </xsl:if>
                        </matchedDept>
                    </xsl:if>
                    <matchedInst>
                        <xsl:choose>
                            <xsl:when test="$matchedInstIsValid">
                                <xsl:copy-of select="$instInfo" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:if test="$instInfo/@name">
                                    <orgInfo name="{$instInfo/@name}">
                                        <xsl:sequence select="$instInfo/config:org-unit" />
                                    </orgInfo>
                                </xsl:if>
                            </xsl:otherwise>
                        </xsl:choose>
                    </matchedInst>
                </xsl:otherwise>
            </xsl:choose>
        </orgInfos>
    </xsl:function>


    <!--
        svfn:organisationObjects
        ========================
        Create objects representing an an institution and any sub organisation if present from an api:address or api:institution object
        allows you to individually specify if you also want the "department" level address information to be turned into an org object

        Uses svfn:getOrgInfosFromAddress internally to get information about the org-infos after comparison and matching to the configured
        "org-units". Generates up to two "rdf:description" objects based on this matching, 1 for the department (if present) and 1 for the institution.
        Depending on the output of svfn:getOrgInfosFromAddress either of these may be a very sparse description referencing simply the relevant URI for
        the Elements group that has been inferred as a good match.

        If both the dept and inst are represented here, the dept is always listed first, this is important for the correct operation of
        svfn:organisationObjectsMainURI
    -->
    <xsl:function name="svfn:organisationObjects">
        <xsl:param name="address" />
        <xsl:param name="createDepartment" as="xs:boolean" />

        <xsl:variable name="userGroupMatch" select="svfn:getOrgInfosFromAddress($address, $createDepartment)" />

        <xsl:choose>
            <xsl:when test="$userGroupMatch/fullMatch">
                <xsl:variable name="groupURI" select="svfn:groupURI($userGroupMatch/fullMatch/orgInfo/@matched-group-id, $userGroupMatch/fullMatch/orgInfo/@matched-group-name)" />
                <rdf:Description rdf:about="{$groupURI}" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="extractedDeptName" select="$userGroupMatch/@deptName" />
                <xsl:variable name="extractedInstName" select="$userGroupMatch/@instName" />
                <xsl:variable name="countryCode" select="$userGroupMatch/@country-code" />
                <xsl:variable name="countryUri" select="fn:normalize-space($country-types/config:country-type[$countryCode and $countryCode != '' and fn:lower-case(@iso-code) = fn:lower-case($countryCode)][1]/@uri)" />

                <xsl:variable name="deptInfo" select="$userGroupMatch/matchedDept/orgInfo" />
                <xsl:variable name="instInfo" select="$userGroupMatch/matchedInst/orgInfo" />

                <xsl:variable name="deptURI">
                    <xsl:choose>
                        <xsl:when test="($deptInfo and not($deptInfo/@name='')) and ($instInfo and not($instInfo/@name = ''))"><xsl:value-of select="svfn:makeURI('dept-',concat(fn:substring($deptInfo/@name,1,100),'-',fn:substring($instInfo/@name,1,50)))" /></xsl:when>
                        <xsl:when test="$deptInfo and not($deptInfo/@name='')"><xsl:value-of select="svfn:makeURI('dept-', fn:concat(fn:substring($deptInfo/@name,1,150), ' '))" /></xsl:when>
                    </xsl:choose>
                </xsl:variable>

                <xsl:variable name="instURI" >
                    <xsl:choose>
                        <xsl:when test="$instInfo/@matched-group-id"><xsl:value-of select="svfn:groupURI($instInfo/@matched-group-id,$instInfo/@matched-group-name)" /></xsl:when>
                        <xsl:when test="$instInfo and not($instInfo/@name='')"><xsl:value-of select="svfn:makeURI('institution-',fn:concat($instInfo/@name, ' '))" /></xsl:when>
                    </xsl:choose>
                </xsl:variable>

                <xsl:if test="not($deptURI = '')">
                    <xsl:variable name="deptType">
                        <xsl:choose>
                            <xsl:when test="$deptInfo/config:org-unit/@type"><xsl:value-of select="$deptInfo/config:org-unit/@type" /></xsl:when>
                            <xsl:otherwise><xsl:value-of select="'http://vivoweb.org/ontology/core#AcademicDepartment'" /></xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>

                    <xsl:call-template name="render_rdf_object">
                        <xsl:with-param name="objectURI" select="$deptURI" />
                        <xsl:with-param name="rdfNodes">
                            <!-- TODO Implement dictionary to determine department type -->
                            <rdf:type rdf:resource="{$deptType}"/>
                            <rdfs:label><xsl:value-of select="$deptInfo/@name" /></rdfs:label>
                            <xsl:if test="not($instURI='')">
                                <obo:BFO_0000050 rdf:resource="{$instURI}" />
                            </xsl:if>
                            <xsl:element namespace="{$validatedLocalOntologyURI}" name="raw-organisation-name">
                                <xsl:value-of select="$extractedDeptName" />
                            </xsl:element>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="not($instURI='')">
                    <xsl:choose>
                        <xsl:when test="$instInfo/@matched-group-id">
                            <rdf:Description rdf:about="{$instURI}" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:variable name="instType">
                                <xsl:choose>
                                    <xsl:when test="$instInfo/config:org-unit/@type"><xsl:value-of select="$instInfo/config:org-unit/@type" /></xsl:when>
                                    <xsl:otherwise><xsl:value-of select="svfn:inferOrganizationType($instInfo/@name, 'http://vivoweb.org/ontology/core#University')" /></xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>

                            <xsl:call-template name="render_rdf_object">
                                <xsl:with-param name="objectURI" select="$instURI" />
                                <xsl:with-param name="rdfNodes">
                                    <rdf:type rdf:resource="{$instType}" />
                                    <rdfs:label><xsl:value-of select="$instInfo/@name" /></rdfs:label>
                                    <xsl:if test="not($deptURI='') and $createDepartment">
                                        <obo:BFO_0000051 rdf:resource="{$deptURI}" />
                                    </xsl:if>
                                    <xsl:if test="$countryUri and $countryUri != ''">
                                        <vivo:GeographicLocation><xsl:value-of select="$countryUri" /></vivo:GeographicLocation>
                                    </xsl:if>
                                    <xsl:element namespace="{$validatedLocalOntologyURI}" name="raw-organisation-name">
                                        <xsl:value-of select="$extractedInstName" />
                                    </xsl:element>
                                </xsl:with-param>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>

            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
        svfn:organisationObjects
        ========================
        Overloaded version of the main svfn:organisationObjects function (above) to default whether to try and match department level
        information based on the config variable $includeDept.
    -->
    <xsl:function name="svfn:organisationObjects">
        <xsl:param name="address" />
        <xsl:copy-of select="svfn:organisationObjects($address, $includeDept)" />
    </xsl:function>


    <!--
        svfn:organisationObjectsMainURI
        ===============================
        Function to retrieve the URI of the first "organisation" object in a sequence  ir rdf-descriptions
        (as potentially generated by "svfn:organisationObjects" above)..
        will adapt if passed a sequence or a single address in different contexts.
        will return an empty string if no appropraite item with an "rdf:about" attribute it detected.
    -->
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
        svfn:stringToURI
        ================
        Convert a string into a URI-friendly form (for identifiers)
    -->
    <xsl:function name="svfn:stringToURI">
        <xsl:param name="string" as="xs:string" />

        <xsl:value-of select="fn:encode-for-uri(fn:replace(fn:replace(fn:lower-case(fn:normalize-space($string)), '\s', '-'), '[^a-z0-9\-]', ''))" />
    </xsl:function>

    <!--
        svfn:getNodeOrLoad
        ==================
        returns the input if it is a node(), id it is a string looks for an XML doc of the same name, loads it and returns that
    -->

    <xsl:function name="svfn:getNodeOrLoad" as="node()?">
        <xsl:param name="input" as="item()?" />
        <xsl:choose>
            <xsl:when test="$input and $input instance of node()">
                <xsl:sequence select="$input" />
            </xsl:when>
            <xsl:when test="$input and $input instance of xs:string and fn:doc-available($input)">
                <xsl:sequence select="fn:doc($input)" />
            </xsl:when>
        </xsl:choose>
    </xsl:function>

    <!--
        svfn:fullObject
        ===============
        Load the XML for the full object, given an Elements object reference
    -->
    <xsl:function name="svfn:fullObject">
        <xsl:param name="object" />
        <xsl:variable name="extraObjectsNode" select="svfn:getNodeOrLoad($extraObjects)" />
        <xsl:choose>
            <xsl:when test="$extraObjectsNode and $extraObjectsNode/descendant::api:object[(@category=$object/@category) and (@id=$object/@id)]">
                <xsl:copy-of select="$extraObjectsNode/descendant::api:object[(@category=$object/@category) and (@id=$object/@id)]" />
            </xsl:when>
            <!--<xsl:when test="$extraObjects instance of xs:string and fn:doc-available($extraObjects) and fn:doc($extraObjects)/descendant::api:object[(@category=$object/@category) and (@id=$object/@id)]">-->
                <!--<xsl:copy-of select="fn:doc($extraObjects)/descendant::api:object[(@category=$object/@category) and (@id=$object/@id)]" />-->
            <!--</xsl:when>-->
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
                <xsl:copy-of select="svfn:renderPropertyFromFieldNode($propertyName, $recordField)" />
            </xsl:when>
            <xsl:when test="$default instance of element()">
                <xsl:copy-of select="svfn:renderPropertyFromFieldNode($propertyName, $default)" />
            </xsl:when>
            <xsl:when test="$default">
                <xsl:variable name="dummy-field">
                    <api:text name="{$propertyName}">
                        <xsl:value-of select="$default" />
                    </api:text>
                </xsl:variable>
                <xsl:copy-of select="svfn:renderPropertyFromFieldNode($propertyName, $dummy-field)" />
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

        <xsl:copy-of select="svfn:renderPropertyFromFieldNode($propertyName, svfn:getRecordField($object, $fieldName))" />
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

        <xsl:copy-of select="svfn:renderPropertyFromFieldNode($propertyName, svfn:getRecordField($object, $fieldName, $records))" />
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

        <xsl:copy-of select="svfn:renderPropertyFromFieldNode($propertyName, svfn:getRecordField($object, $fieldName, $records, $select-by, $useUnlistedSources))" />
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

    <xsl:function name="svfn:renderPropertyFromFieldNode">
        <xsl:param name="propertyName" as="xs:string" />
        <xsl:param name="fieldNode" as="node()?" />

        <xsl:apply-templates select="$fieldNode" mode="renderForProperty">
            <xsl:with-param name="propertyName" select="$propertyName" />
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
                <xsl:variable name="requiredVerificationStatus">
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
                    <xsl:when test="$exclusions/config:record-exclusion[text()=$currentSourceName and (text() != 'manual' or (not(@verification-status) or @verification-status = 'any' or @verification-status = $requiredVerificationStatus))]">
                        <xsl:copy-of select="svfn:_getRecordField($object,$fieldName,$records,$select-by,$position+1,$useUnlistedSources,$useExcludedData)" />
                    </xsl:when>
                    <!-- don't use fields that are restricted -->
                    <xsl:when test="$exclusions/config:field-exclusions[@for-source=$currentSourceName and (text() != 'manual' or (not(@verification-status) or @verification-status = 'any' or @verification-status = $requiredVerificationStatus))]/config:excluded-field[text()=$fieldName]">
                        <xsl:copy-of select="svfn:_getRecordField($object,$fieldName,$records,$select-by,$position+1,$useUnlistedSources,$useExcludedData)" />
                    </xsl:when>
                    <xsl:when test="$select-by='field'">
                        <xsl:choose>
                            <xsl:when test="$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificationStatus = 'any' or api:verification-status = $requiredVerificationStatus)]/api:native/api:field[@name=$fieldName]">
                                <xsl:copy-of select="$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificationStatus = 'any' or api:verification-status = $requiredVerificationStatus)][1]/api:native/api:field[@name=$fieldName]" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:copy-of select="svfn:_getRecordField($object,$fieldName,$records,$select-by,$position+1,$useUnlistedSources,$useExcludedData)" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when test="$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificationStatus = 'any' or api:verification-status = $requiredVerificationStatus)]/api:native">
                                <xsl:copy-of select="$object/api:records/api:record[@source-name=$currentSourceName and ($requiredVerificationStatus = 'any' or api:verification-status = $requiredVerificationStatus)][1]/api:native/api:field[@name=$fieldName]" />
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
            <!-- we are looping through the set of records in the object, until we find something we like, or run out of records to consider-->
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
        <rdf:RDF />
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

