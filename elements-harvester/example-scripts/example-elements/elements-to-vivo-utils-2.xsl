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

    <xsl:import href="elements-to-vivo-config.xsl" />

     <!-- ======================================
         Function Library
         ======================================- -->

    <!-- date and html functions, broken into own file to avoid cyclic inclusion dependencies in elements-to-vivo-datatypes.xsl -->

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


    <xsl:function name="svfn:shouldConvertToHTML">
        <xsl:param name="propertyName" as="xs:string" />

        <xsl:choose>
            <!--suppress Annotator -->
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


 </xsl:stylesheet>

