<!--
  ~ /*******************************************************************************
  ~  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
  ~  * This Source Code Form is subject to the terms of the Mozilla Public
  ~  * License, v. 2.0. If a copy of the MPL was not distributed with this
  ~  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
  ~  ******************************************************************************/
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

    <!--&lt;!&ndash;-->
        <!--svfn:userURI-->
        <!--============-->
        <!--Create a URI for a user based on the passed Elements object-->
    <!--&ndash;&gt;-->
    <!--<xsl:function name="svfn:userURI" as="xs:string">-->
        <!--<xsl:param name="object" />-->
        <!--<xsl:value-of select="svfn:makeURI('person', translate($object/@proprietary-id, 'Aa', ''))" />-->
    <!--</xsl:function>-->

</xsl:stylesheet>