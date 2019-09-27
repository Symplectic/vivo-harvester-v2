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

    <!-- This file exists to allow easy overriding of any of the templates in the elements-to-vivo pipeline -->
    <!--<xsl:template match="api:object[@category='user']">-->
        <!--&lt;!&ndash;<xsl:variable name="lastCharOfPid" select =  substring($s&ndash;&gt;-->
        <!--&lt;!&ndash;<xsl:if test = substring($s, string-length($s) - string-length($t) +1)&ndash;&gt;-->
        <!--<xsl:if test=".[ends-with(@proprietary-id,'A')] or .[ends-with(@proprietary-id,'a')]">-->
            <!--<xsl:variable name="prefix" select="'profile-pictures/'" />-->
            <!--<xsl:variable name="userPid" select="translate(./@proprietary-id, 'Aa', '')" />-->
            <!--<xsl:variable name="fullImageFilename" select="concat($userPid, 'picture.jpg')" />-->
            <!--<xsl:variable name="fullImagePath" select="concat($prefix, $userPid, 'picture')" />-->
            <!--<xsl:variable name="thumbnailImageFilename" select="concat('thumbnail', $userPid, 'picture.jpg')" />-->
            <!--<xsl:variable name="thumbnailImagePath" select="concat($prefix, 'thumbnail', $userPid, 'picture.jpg')" />-->

            <!--<xsl:copy-of select="svfn:userPhotoDescription(svfn:userURI(.), $fullImageFilename, $fullImagePath,-->
         <!--$thumbnailImageFilename, $thumbnailImagePath)" />-->

        <!--</xsl:if>-->
        <!--<xsl:apply-imports/>-->
    <!--</xsl:template>-->

</xsl:stylesheet>
