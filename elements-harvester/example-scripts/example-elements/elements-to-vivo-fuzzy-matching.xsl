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

<!-- Based on Roger L. Cauvin http://www.biglist.com/lists/lists.mulberrytech.com/xsl-list/archives/201301/msg00164.html -->
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

    <!-- ======================================
         Function Library
         ======================================- -->

    <xsl:variable name="lower-case-letters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
    <xsl:variable name="upper-case-letters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

    <!-- Utility function to compare two strings for similarity - will return a value between 0 and 1, higher being more similar
    we could potentially optimise this to an injected java function if it is not very performant -->
    <xsl:function name="svfn:compare-strings">
        <xsl:param name="string1"/>
        <xsl:param name="string2"/>

        <xsl:variable name="pairs1">
            <xsl:call-template name="get-word-letter-pairs">
                <xsl:with-param name="string" select="normalize-space(translate($string1, $lower-case-letters, $upper-case-letters))"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="pairs2">
            <xsl:call-template name="get-word-letter-pairs">
                <xsl:with-param name="string" select="normalize-space(translate($string2, $lower-case-letters, $upper-case-letters))"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:call-template name="compare-pairs">
            <xsl:with-param name="pairs1" select="$pairs1"/>
            <xsl:with-param name="pairs2" select="$pairs2"/>
        </xsl:call-template>
    </xsl:function>

    <xsl:template name="compare-pairs">
        <xsl:param name="pairs1"/>
        <xsl:param name="pairs2"/>

        <xsl:variable name="num-pairs1" select="string-length($pairs1) div 3"/>
        <xsl:variable name="num-pairs2" select="string-length($pairs2) div 3"/>
        <xsl:variable name="union" select="$num-pairs1 + $num-pairs2"/>

        <xsl:variable name="intersection">
            <xsl:call-template name="intersect-remaining-pairs">
                <xsl:with-param name="pairs1" select="$pairs1"/>
                <xsl:with-param name="pairs2" select="$pairs2"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:value-of select="2.0 * $intersection div $union"/>
    </xsl:template>

    <xsl:template name="intersect-remaining-pairs">
        <xsl:param name="pairs1"/>
        <xsl:param name="pairs2"/>
        <xsl:param name="intersection">0</xsl:param>

        <xsl:variable name="pair" select="substring-before($pairs1, ' ')"/>
        <xsl:choose>
            <xsl:when test="$pair = ''">
                <xsl:value-of select="$intersection"/>
            </xsl:when>
            <xsl:when test="contains($pairs2, $pair)">
                <xsl:call-template name="intersect-remaining-pairs">
                    <xsl:with-param name="pairs1" select="substring-after($pairs1, ' ')"/>
                    <xsl:with-param name="pairs2" select="concat(substring-before($pairs2, $pair), substring-after($pairs2, concat($pair, ' ')))"/>
                    <xsl:with-param name="intersection" select="$intersection + 1"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="intersect-remaining-pairs">
                    <xsl:with-param name="pairs1" select="substring-after($pairs1, ' ')"/>
                    <xsl:with-param name="pairs2" select="$pairs2"/>
                    <xsl:with-param name="intersection" select="$intersection"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="get-word-letter-pairs">
        <xsl:param name="string"/>
        <xsl:param name="pairs"></xsl:param>

        <xsl:choose>
            <xsl:when test="$string = ''">
                <xsl:value-of select="$pairs"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="word">
                    <xsl:choose>
                        <xsl:when test="contains($string, ' ')">
                            <xsl:value-of select="substring-before($string, ' ')"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$string"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="letter-pairs">
                    <xsl:call-template name="get-letter-pairs">
                        <xsl:with-param name="word" select="$word"/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:call-template name="get-word-letter-pairs">
                    <xsl:with-param name="string" select="substring-after($string, ' ')"/>
                    <xsl:with-param name="pairs" select="concat($pairs, $letter-pairs)"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template name="get-letter-pairs">
        <xsl:param name="word"/>
        <xsl:param name="pairs"></xsl:param>

        <xsl:choose>
            <xsl:when test="string-length($word) &lt; 2">
                <xsl:value-of select="$pairs"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="get-letter-pairs">
                    <xsl:with-param name="word" select="substring($word, 2, string-length($word) - 1)"/>
                    <xsl:with-param name="pairs" select="concat($pairs, substring($word, 1, 2), ' ')"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
