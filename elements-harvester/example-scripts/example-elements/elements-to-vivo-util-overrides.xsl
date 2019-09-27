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
                xmlns:api="http://www.symplectic.co.uk/publications/api"
                xmlns:config="http://www.symplectic.co.uk/vivo/namespaces/config"
                xmlns:symp="http://www.symplectic.co.uk/ontology/elements/"
                xmlns:svfn="http://www.symplectic.co.uk/vivo/namespaces/functions"
                xmlns:obo="http://purl.obolibrary.org/obo/"
                exclude-result-prefixes="api config xs fn svfn"
>

    <!-- This file exists to allow easy overriding of the functions within elements-to-vivo-utils.xsl -->

    <!--
    svfn:objectURI
    ==============
    Create a URI for the RDF objects based on the passed Elements object
    -->

    <!--<xsl:function name="svfn:objectURI" as="xs:string">-->
        <!--<xsl:param name="object" />-->
        <!--<xsl:choose>-->
            <!--<xsl:when test="$object/@category = 'publication'">-->
                <!--<xsl:value-of select="concat($baseURI,svfn:stringToURI($object/@category), 'S',svfn:stringToURI($object/@id))" />-->
            <!--</xsl:when>-->
            <!--<xsl:otherwise>-->
                <!--<xsl:value-of select="svfn:makeURI($object/@category,$object/@id)" />-->
            <!--</xsl:otherwise>-->
        <!--</xsl:choose>-->
    <!--</xsl:function>-->

    <!--
        svfn:userURI
        ============
        Create a URI for a user based on the passed Elements object
    -->
    <!--<xsl:function name="svfn:userURI" as="xs:string">-->
        <!--<xsl:param name="object" />-->
        <!--<xsl:variable name="selected-user-id">-->
            <!--<xsl:choose>-->
                <!--<xsl:when test="not(fn:normalize-space($object/@proprietary-id) = '')">-->
                    <!--<xsl:value-of select="fn:normalize-space($object/@proprietary-id)" />-->
                <!--</xsl:when>-->
                <!--<xsl:otherwise>-->
                    <!--<xsl:value-of select="concat($object/@id, 'E')" />-->
                <!--</xsl:otherwise>-->
            <!--</xsl:choose>-->
        <!--</xsl:variable>-->
        <!--<xsl:value-of select="svfn:makeURI('person', translate($selected-user-id, 'Aa', ''))" />-->
    <!--</xsl:function>-->

    <!--<xsl:function name="svfn:getOrganizationType">-->
        <!--<xsl:param name="name" />-->
        <!--<xsl:param name="default" />-->

        <!--<xsl:choose>-->
            <!--<xsl:when test="$organization-types/config:organization-type[@name=$name]"><xsl:value-of select="($organization-types/config:organization-type[@name=$name])[1]/@type" /></xsl:when>-->
            <!--<xsl:when test="contains($name,'University')"><xsl:text>http://vivoweb.org/ontology/core#University</xsl:text></xsl:when>-->
            <!--<xsl:when test="contains($name,'College')"><xsl:text>http://vivoweb.org/ontology/core#College</xsl:text></xsl:when>-->
            <!--<xsl:when test="contains($name,'Museum')"><xsl:text>http://vivoweb.org/ontology/core#Museum</xsl:text></xsl:when>-->
            <!--<xsl:when test="contains($name,'Hospital')"><xsl:text>http://vivoweb.org/ontology/core#Hospital</xsl:text></xsl:when>-->
            <!--<xsl:when test="contains($name,'Institute')"><xsl:text>http://vivoweb.org/ontology/core#Institute</xsl:text></xsl:when>-->
            <!--<xsl:when test="contains($name,'School')"><xsl:text>http://vivoweb.org/ontology/core#School</xsl:text></xsl:when>-->
            <!--<xsl:when test="contains($name,'Association')"><xsl:text>http://vivoweb.org/ontology/core#Association</xsl:text></xsl:when>-->
            <!--<xsl:when test="contains($name,'Library')"><xsl:text>http://vivoweb.org/ontology/core#Library</xsl:text></xsl:when>-->
            <!--<xsl:when test="contains($name,'Foundation')"><xsl:text>http://vivoweb.org/ontology/core#Foundation</xsl:text></xsl:when>-->
            <!--<xsl:when test="contains($name,'Ltd')"><xsl:text>http://vivoweb.org/ontology/core#PrivateCompany</xsl:text></xsl:when>-->
            <!--<xsl:when test="starts-with(fn:normalize-space($name),'SRI ') or starts-with(fn:normalize-space($name),'SRN ') or contains($name, 'Interdisciplinary Research Centre')">-->
                <!--<xsl:text>http://vivoweb.org/ontology/core#Consortium</xsl:text>-->
            <!--</xsl:when>-->
            <!--<xsl:otherwise><xsl:value-of select="$default" /></xsl:otherwise>-->
        <!--</xsl:choose>-->
    <!--</xsl:function>-->

</xsl:stylesheet>