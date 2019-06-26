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
                xmlns:ufVivo="http://vivo.ufl.edu/ontology/vivo-ufl/"
                xmlns:vitro="http://vitro.mannlib.cornell.edu/ns/vitro/0.7#"
                xmlns:api="http://www.symplectic.co.uk/publications/api"
                xmlns:symp="http://www.symplectic.co.uk/ontology/elements/"
                xmlns:svfn="http://www.symplectic.co.uk/vivo/namespaces/functions"
                exclude-result-prefixes="rdf rdfs bibo vivo foaf score ufVivo vitro api symp svfn fn xs"
        >

    <!--
        Main XSLT file for applying Elements field type value matching
        ==========================================================
    -->

    <!--
        Render a keyword field to a named property. The name of the VIVO property (namespace:element) is passed in propertyName,
        the field name is Elements is passed in fieldName.
    -->
    <xsl:function name="svfn:_fieldMatchesValue" as="xs:boolean">
        <xsl:param name="field" />
        <xsl:param name="valueToCompare" as="xs:string" />

        <xsl:choose>
            <xsl:when test="$field/@type = 'text' and $field/api:text[text() = $valueToCompare]">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:when test="$field/@type = 'boolean' and $field/api:boolean[text() = $valueToCompare]">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:when test="$field/@type = 'keyword-list' and $field/api:keywords/api:keyword[text() = $valueToCompare]">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:when test="$field/@type = 'list' and $field/api:items/api:item[text() = $valueToCompare]">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="svfn:_fieldLikeValue" as="xs:boolean">
        <xsl:param name="field" />
        <xsl:param name="valueToCompare" as="xs:string" />

        <xsl:choose>
            <xsl:when test="$field/@type = 'text' and $field/api:text[fn:matches(text(), $valueToCompare)]">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:when test="$field/@type = 'keyword-list' and $field/api:keywords/api:keyword[fn:matches(text(), $valueToCompare)]">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:when test="$field/@type = 'list' and $field/api:items/api:item[fn:matches(text(), $valueToCompare)]">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="svfn:fieldMatches" as="xs:boolean" >
        <xsl:param name="object" />
        <xsl:param name="fieldName" as="xs:string" />
        <xsl:param name="valueToCompare" as="xs:string" />
        <xsl:param name="records" />

        <xsl:variable name="any-record" select="normalize-space($records) = ''" />
        <xsl:variable name="record-sources" select="fn:tokenize($records, ',')" />

        <!-- look at making more controllable -->
        <xsl:choose>
            <xsl:when test="$object/api:records/api:record[$any-record or @source-name=$record-sources]/api:native/api:field[@name = $fieldName and svfn:_fieldMatchesValue(., $valueToCompare)]">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="svfn:fieldLike" as="xs:boolean" >
        <xsl:param name="object" />
        <xsl:param name="fieldName" as="xs:string" />
        <xsl:param name="valueToCompare" as="xs:string" />
        <xsl:param name="records" />

        <xsl:variable name="any-record" select="normalize-space($records) = ''" />
        <xsl:variable name="record-sources" select="fn:tokenize($records, ',')" />

        <!-- look at making more controllable -->
        <xsl:choose>
            <xsl:when test="$object/api:records/api:record[$any-record or @source-name=$record-sources]/api:native/api:field[@name = $fieldName and svfn:_fieldLikeValue(., $valueToCompare)]">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="svfn:notCondition" as="xs:boolean">
        <xsl:param name="object" />
        <xsl:param name="condition" />
        <xsl:value-of select="not(svfn:evaluateCondition($object, $condition/*[1]))" />
    </xsl:function>

    <xsl:function name="svfn:orCondition" as="xs:boolean">
        <xsl:param name="object" />
        <xsl:param name="condition" />
        <xsl:param name="position"  />

        <xsl:variable name="currentSubCondition" select="$condition/*[$position]" />

        <xsl:choose>
            <xsl:when test="$currentSubCondition">
                <xsl:choose>
                    <xsl:when test="svfn:evaluateCondition($object, $currentSubCondition)">
                        <xsl:value-of select="true()" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="svfn:orCondition($object, $condition, $position + 1)" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()" />
            </xsl:otherwise>
        </xsl:choose>

    </xsl:function>


    <xsl:function name="svfn:andCondition" as="xs:boolean">
        <xsl:param name="object" />
        <xsl:param name="condition" />
        <xsl:param name="position" />

        <xsl:variable name="currentSubCondition" select="$condition/*[$position]" />

        <xsl:choose>
            <xsl:when test="$currentSubCondition">
                <xsl:choose>
                    <xsl:when test="not(svfn:evaluateCondition($object, $currentSubCondition))">
                        <xsl:value-of select="false()" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="svfn:andCondition($object, $condition, $position + 1)" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="count($condition/*)">
                <xsl:value-of select="true()" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()" />
            </xsl:otherwise>
        </xsl:choose>

    </xsl:function>

    <xsl:function name="svfn:evaluateCondition" as="xs:boolean" >
        <xsl:param name="object" />
        <xsl:param name="condition" />

        <!-- look at making more controllable -->
        <xsl:choose>
            <xsl:when test="name($condition) = 'config:field-matches'">
                <xsl:value-of select="svfn:fieldMatches($object, $condition/@field, $condition/@value, $condition/@in-records)" />
            </xsl:when>
            <xsl:when test="name($condition) = 'config:field-like'">
                <xsl:value-of select="svfn:fieldLike($object, $condition/@field, $condition/@value, $condition/@in-records)" />
            </xsl:when>
            <xsl:when test="name($condition) = 'config:not'">
                <xsl:value-of select="svfn:notCondition($object, $condition)" />
            </xsl:when>
            <xsl:when test="name($condition) = 'config:or'">
                <xsl:value-of select="svfn:orCondition($object, $condition, 1)" />
            </xsl:when>
            <xsl:when test="name($condition) = 'config:and'">
                <xsl:value-of select="svfn:andCondition($object, $condition, 1)" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()" />
            </xsl:otherwise>
        </xsl:choose>

    </xsl:function>

</xsl:stylesheet>
