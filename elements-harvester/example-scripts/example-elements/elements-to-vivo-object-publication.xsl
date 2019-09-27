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
<!-- noinspection XsltUnusedDeclaration -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:bibo="http://purl.org/ontology/bibo/"
                xmlns:obo="http://purl.obolibrary.org/obo/"
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
                exclude-result-prefixes="rdf rdfs fn bibo obo vivo vcard foaf score ufVivo vitro api symp svfn config xs"
>

    <!--
        XSLT for generating VIVO representation of a publication object (journal article, conference preceding, etc.)
    -->

    <!-- Import the utility XSLT -->
    <xsl:import href="elements-to-vivo-utils.xsl" />
    <xsl:import href="elements-to-vivo-config.xsl" />

    <!-- Match objects of type 'publication' -->
    <xsl:template match="api:object[@category='publication']">
        <xsl:variable name="publicationId" select="@id" />
        <xsl:variable name="publicationUri" select="svfn:objectURI(.)" />

        <!-- Labels -->
        <xsl:variable name="allLabels" select="api:all-labels" />

        <!-- Attempt to generate a URI for the publication date object -->
        <xsl:variable name="publicationDateURI" select="concat(svfn:objectURI(.),'-publicationDate')" />

        <xsl:variable name="pubDateField" select="svfn:getRecordField(.,'publication-date')" />
        <xsl:variable name="selectedDate">
            <xsl:choose>
                <xsl:when test="$pubDateField">
                    <xsl:copy-of select="$pubDateField" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="svfn:getRecordField(.,'start-date')" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="initialDateToUse" select="$selectedDate/api:field[1]" />

        <!-- Generate the publication date object. Custom XSLT 2 function that takes the URI for the date, the field that holds the publication date, and what precision you would like to output the date in -->
        <xsl:variable name="publicationDateObject" select="svfn:renderDateObject($publicationDateURI, $initialDateToUse, '')" />

        <!-- Generate a publication date interval object. -->
        <xsl:variable name="publicationDateIntervalObject" select="svfn:renderDateInterval($publicationUri, $initialDateToUse, svfn:getRecordField(.,'end-date'), '', false())" />
        <xsl:variable name="publicationDateIntervalURI" select="svfn:retrieveDateIntervalUri($publicationDateIntervalObject)" />

        <!-- Attempt to generate a URI for the filed date object -->
        <xsl:variable name="filedDateURI" select="concat(svfn:objectURI(.),'-filedDate')" />

        <!-- Generate the filed date object. Custom XSLT 2 function that takes the URI for the date, the field that holds the filed date and what precision you would like to output the date in  -->
        <xsl:variable name="filedDateObject" select="svfn:renderDateObject($filedDateURI,svfn:getRecordField(.,'filed-date'), '')" />

        <!-- Attempt to get a journal title for the article -->
        <xsl:variable name="publicationVenueTitle" select="svfn:selectJournalTitle(.)" />

        <!-- Generate a publication venue object URI from the journal title -->
        <xsl:variable name="publicationVenueURI" select="svfn:makeURI('journal-',$publicationVenueTitle)" />

        <!-- Generate the publication venue object. Custom XSLT 2 function that takes the current object, journal URI and journal title. -->
        <xsl:variable name="publicationVenueObject" select="svfn:renderPublicationVenueObject(.,$allLabels,$publicationVenueURI,$publicationVenueTitle,svfn:objectURI(.))" />

        <!-- Attempt to get the name for a conference -->
        <xsl:variable name="conferenceName" select="svfn:selectConferenceName(.)" />

        <!-- Generate a conference object URI from the conference name -->
        <xsl:variable name="conferenceURI" select="svfn:makeURI('conference-',$conferenceName)" />

        <xsl:variable name="rdfTypes" select="svfn:getTypesForPublication(.)" />

        <xsl:variable name="translationContext" select="svfn:translationContext($rdfTypes)" />

        <!-- Generate the conference object -->
        <xsl:variable name="conferenceObject" select="svfn:renderConferenceObject(.,$conferenceURI,$conferenceName,svfn:objectURI(.), $translationContext)" />

        <!-- Get the authors -->
        <xsl:variable name="authors" select="svfn:getRecordField(.,'authors')" />
        <xsl:variable name="editors" select="svfn:getRecordField(.,'editors')" />

        <!-- Web pages -->
        <xsl:variable name="arxivPdfUrl" select="svfn:getRecordField(.,'arxiv-pdf-url')" />
        <xsl:variable name="authorUrl" select="svfn:getRecordField(.,'author-url')" />
        <xsl:variable name="publisherUrl" select="svfn:getRecordField(.,'publisher-url')" />
        <xsl:variable name="repositoryUrl" select="(api:repository-items/api:repository-item/api:public-url)[1]" />
        <!-- specifically try and grab out a "Web of Science" url, which may match the author URL depending on record precedence and exclusions-->
        <!-- note do not force it with FieldOrFirst as we only want to provide a WoS link if we might be showing WoS data -->
        <xsl:variable name="wosUrl" select="svfn:getRecordField(.,'author-url', 'wos,wos-lite', 'field', false())" />

        <xsl:variable name="contactInfo" select="svfn:renderPublicationContactInfo(.)" />

        <xsl:variable name="publicationStatus" select="svfn:getRecordField(.,'publication-status')" />

        <xsl:variable name="externalIdentifiers" select="svfn:getRecordField(.,'external-identifiers')" />

        <!-- Render an RDF object -->
        <xsl:call-template name="render_rdf_object">
            <!-- Generate a URI for the current publication -->
            <xsl:with-param name="objectURI" select="$publicationUri" />
            <!--
                Generate the RDF property statements for the publication object.
                Uses the getTypesForPublication custom XSLT function to retrieve all the type statements (including mostSpecificType)
                The renderPropertyFromField function creates a property from the specified Elements data field, restricted to your configured preferred records
                - it takes the full namespace/element name for the resulting property, and the field name in Elements
                  The resulting property comes from an apply-templates on the selected field, so you can override how the data is rendered
                  by overloading those templates
                The renderPropertyFromFieldOrFirst operates the same way, except if none of you preferred data sources are present, it takes the field from the first record
                - this is used only for the title, so that there will always be some information about the items that could not be transferred fully, because of the records available
                Also, the properties to reference the publication date and venue are output, if the objects have been created (test for child nodes)
            -->
            <xsl:with-param name="rdfNodes">
                <xsl:copy-of select="$rdfTypes" />
                <xsl:copy-of select="svfn:renderPropertyFromFieldOrFirst(.,'rdfs:label','title', '')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:abstract','abstract')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:doi','doi')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:edition','edition')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:issue','issue')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:pageStart','pagination')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:pageEnd','pagination')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:volume','volume')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:isbn10','isbn-10')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'bibo:isbn13','isbn-13')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'vivo:freetextKeyword','keywords')" />
                <xsl:copy-of select="svfn:renderPropertyFromField(.,'vivo:patentNumber','patent-number')" />
                <xsl:for-each select="$externalIdentifiers/api:identifiers/api:identifier">
                    <xsl:choose>
                        <xsl:when test="@scheme='pubmed'"><bibo:pmid><xsl:value-of select="." /></bibo:pmid></xsl:when>
                    </xsl:choose>
                </xsl:for-each>
                <xsl:if test="not($useSympNS='')">
                    <!-- Note, this is legacy code, these properties ("symp:authors" etc) have to be manually added to Vivo
                     if you wish to make use of it -->
                    <xsl:copy-of select="svfn:renderPropertyFromField(.,'symp:authors','authors')" />
                    <xsl:copy-of select="svfn:renderPropertyFromField(.,'symp:language','language')" />
                    <xsl:copy-of select="svfn:renderPropertyFromField(.,'symp:location','location')" />
                    <xsl:copy-of select="svfn:renderPropertyFromField(.,'symp:notes','notes')" />
                    <xsl:copy-of select="svfn:renderPropertyFromField(.,'symp:pii','pii')" />
                </xsl:if>
                <xsl:if test="$filedDateObject/*"><vivo:dateFiled rdf:resource="{$filedDateURI}" /></xsl:if>
                <xsl:choose>
                    <xsl:when test="$translationContext = 'event' or $translationContext = 'presentation'">
                        <xsl:if test="$publicationDateIntervalURI">
                            <vivo:dateTimeInterval rdf:resource="{$publicationDateIntervalURI}" />
                        </xsl:if>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:if test="$publicationDateObject/*">
                            <vivo:dateTimeValue rdf:resource="{$publicationDateURI}" />
                        </xsl:if>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:if test="$publicationVenueObject/*"><vivo:hasPublicationVenue rdf:resource="{$publicationVenueURI}" /></xsl:if>
                <xsl:if test="$conferenceObject/*">
                    <xsl:choose>
                        <xsl:when test="$translationContext = 'event' or $translationContext = 'presentation'">
                            <obo:BFO_0000050 rdf:resource="{$conferenceURI}" />
                        </xsl:when>
                        <xsl:otherwise>
                            <bibo:presentedAt rdf:resource="{$conferenceURI}" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>

                <xsl:if test="$contactInfo/*">
                    <obo:ARG_2000028>
                        <xsl:copy-of select="$contactInfo" />
                    </obo:ARG_2000028>
                </xsl:if>

                <xsl:choose>
                    <xsl:when test="$publicationStatus/api:text='Accepted'"><bibo:status rdf:resource="http://purl.org/ontology/bibo/accepted" /></xsl:when>
                    <xsl:when test="$publicationStatus/api:text='In preparation'"><bibo:status rdf:resource="http://vivoweb.org/ontology/core#inPress" /></xsl:when>
                    <xsl:when test="$publicationStatus/api:text='Published'"><bibo:status rdf:resource="http://purl.org/ontology/bibo/published" /></xsl:when>
                    <xsl:when test="$publicationStatus/api:text='Submitted'"><bibo:status rdf:resource="http://vivoweb.org/ontology/core#submitted" /></xsl:when>
                    <xsl:when test="$publicationStatus/api:text='Unpublished'"><bibo:status rdf:resource="http://purl.org/ontology/bibo/unpublished" /></xsl:when>
                </xsl:choose>
                <!-- add label links -->
                <xsl:copy-of select="svfn:renderControlledSubjectLinks(., $publicationUri, '')" />
                <xsl:apply-templates select="." mode="customAdditions" />
            </xsl:with-param>
        </xsl:call-template>

        <!-- Output the publication date and venue objects. If they are empty, nothing will be output -->
        <xsl:copy-of select="$filedDateObject" />
        <xsl:choose>
            <xsl:when test="$translationContext = 'event' or $translationContext = 'presentation'">
                <xsl:copy-of select="$publicationDateIntervalObject" />
            </xsl:when>
            <xsl:otherwise><xsl:copy-of select="$publicationDateObject" /></xsl:otherwise>
        </xsl:choose>

        <xsl:copy-of select="$publicationVenueObject" />
        <xsl:copy-of select="$conferenceObject" />

        <xsl:choose>
            <xsl:when test="$translationContext = 'presentation'">
                <xsl:copy-of select="svfn:renderLinksAndExternalPeople($authors, $publicationId, $publicationUri, 'http://vivoweb.org/ontology/core#PresenterRole', 'Presenter')" />
            </xsl:when>
            <xsl:when test="$translationContext = 'event'">
                <xsl:copy-of select="svfn:renderLinksAndExternalPeople($authors, $publicationId, $publicationUri, 'http://vivoweb.org/ontology/core#ResearcherRole', 'Creator')" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="svfn:renderLinksAndExternalPeople($authors, $publicationId, $publicationUri, 'http://vivoweb.org/ontology/core#Authorship', '')" />
            </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="$translationContext='publication'">
            <xsl:copy-of select="svfn:renderLinksAndExternalPeople($editors, $publicationId, $publicationUri, 'http://vivoweb.org/ontology/core#Editorship', '')" />
        </xsl:if>

        <!-- add label objects -->
        <xsl:copy-of select="svfn:renderControlledSubjectObjects(.)" />
    </xsl:template>

    <!-- ====================================================
         XSLT Function Library
         ==================================================== -->

    <xsl:function name="svfn:selectConferenceName">
        <xsl:param name="object" />
        <xsl:variable name="conferenceName" select="svfn:getRecordField($object,'name-of-conference')" />
        <xsl:choose>
            <xsl:when test="$conferenceName/api:text"><xsl:copy-of select="string($conferenceName/api:text)" /></xsl:when>
            <xsl:otherwise><xsl:copy-of select="''" /></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Select the journal title for the publication. Delegates to an internal function for iteration -->
    <xsl:function name="svfn:selectJournalTitle">
        <xsl:param name="object" />
        <!--<xsl:copy-of select="string(svfn:_selectAuthorityJournalTitle($object, 1))" />-->
        <xsl:variable name="authority-title">
            <xsl:copy-of select="string(svfn:_selectAuthorityJournalTitle($object, 1))" />
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$authority-title != ''"><xsl:value-of select="$authority-title" /></xsl:when>
            <xsl:otherwise><xsl:copy-of select="string(svfn:_selectRecordJournalTitle($object, 1))" /></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Iterating function to select the most appropriate journal title -->
    <xsl:function name="svfn:_selectAuthorityJournalTitle">
        <xsl:param name="object" />
        <xsl:param name="position" as="xs:integer" />
        <xsl:variable name="current-precedence" select="$journal-precedence/config:journal-authority-precedences/config:journal-authority-precedence[$position]" />
        <!-- Ensure that we haven't reached the end of the precedence settings -->
        <xsl:choose>
            <xsl:when test="$current-precedence">
                <xsl:choose>
                    <!-- If the precedence is for an authority record, and we have that authority record in the API feed, use the journal title in the preferred authority source -->
                    <xsl:when test="$object/api:journal/api:records/api:record[@source-name=$current-precedence]/api:title">
                        <xsl:value-of select="$object/api:journal/api:records/api:record[@source-name=$current-precedence][1]/api:title" />
                    </xsl:when>
                    <!-- No title found for the current precedence setting, try again with the next entry -->
                    <xsl:otherwise>
                        <xsl:copy-of select="svfn:_selectAuthorityJournalTitle($object, $position+1)" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="$journal-precedence/config:journal-authority-precedences/@use-unlisted-sources != 'false'">
                    <xsl:value-of select="$object/api:journal/api:records/api:record[1]/api:title" />
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="svfn:_selectRecordJournalTitle">
        <xsl:param name="object" />
        <xsl:param name="position" as="xs:integer" />

        <xsl:variable name="current-precedence" select="$journal-precedence/config:journal-record-precedences/config:journal-record-precedence[$position]" />
        <!-- Ensure that we haven't reached the end of the precedence settings -->
        <xsl:choose>
            <xsl:when test="$current-precedence">
                <xsl:variable name="field-name-to-use">
                    <xsl:choose>
                        <xsl:when test="$current-precedence/@field"><xsl:value-of select="$current-precedence/@field" /></xsl:when>
                        <xsl:otherwise><xsl:value-of select="'journal'" /></xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:choose>
                    <!-- If the precedence if for a record value from a specific field, use that value if it exists -->
                    <xsl:when test="$object/api:records/api:record[@source-name=$current-precedence]/api:native/api:field[@name=$field-name-to-use]/api:text">
                        <xsl:value-of select="$object/api:records/api:record[@source-name=$current-precedence][1]/api:native/api:field[@name=$field-name-to-use]/api:text" />
                    </xsl:when>
                    <!-- No title found for the current precedence setting, try again with the next entry -->
                    <xsl:otherwise>
                        <xsl:copy-of select="svfn:_selectRecordJournalTitle($object, $position+1)" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="$journal-precedence/config:journal-record-precedences/@use-unlisted-sources != 'false'">
                    <xsl:variable name="default-precedence-field" select = "svfn:getRecordField($object,'journal')" />
                    <xsl:value-of select="$default-precedence-field/api:text" />
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>


    <!-- Render the RDF object for the publication venue (journal) -->
    <xsl:function name="svfn:renderPublicationVenueObject">
        <xsl:param name="object" />
        <xsl:param name="allLabels" />
        <xsl:param name="journalObjectURI" as="xs:string" />
        <xsl:param name="journalTitle" as="xs:string" />
        <xsl:param name="publicationURI" as="xs:string" />

        <!-- Only create the object if we have a journal title -->
        <xsl:if test="$journalTitle">
            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$journalObjectURI" />
                <xsl:with-param name="rdfNodes">
                    <rdfs:label><xsl:value-of select="$journalTitle" /></rdfs:label>
                    <rdf:type rdf:resource="http://purl.org/ontology/bibo/Journal"/>
                    <vivo:publicationVenueFor rdf:resource="{$publicationURI}" />
                    <xsl:copy-of select="svfn:renderPropertyFromField($object,'bibo:issn','issn')" />
                    <xsl:copy-of select="svfn:renderPropertyFromField($object,'bibo:eissn','eissn')" />
                    <!-- add label links-->
                    <xsl:copy-of select="svfn:renderControlledSubjectLinks($object, $journalObjectURI, 'issn-inferred')" />
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:function>

    <xsl:function name="svfn:renderConferenceObject">
        <xsl:param name="object" />
        <xsl:param name="conferenceObjectURI" as="xs:string" />
        <xsl:param name="confName" as="xs:string" />
        <xsl:param name="publicationURI" as="xs:string" />
        <xsl:param name="translationContext" as="xs:string" />

        <xsl:if test="$confName">
            <xsl:call-template name="render_rdf_object">
                <xsl:with-param name="objectURI" select="$conferenceObjectURI" />
                <xsl:with-param name="rdfNodes">
                    <rdfs:label><xsl:value-of select="$confName" /></rdfs:label>
                    <rdf:type rdf:resource="http://purl.org/ontology/bibo/Conference"/>
                    <xsl:choose>
                        <xsl:when test="$translationContext = 'event' or $translationContext = 'presentation'">
                            <obo:BFO_0000051 rdf:resource="{$publicationURI}" />
                        </xsl:when>
                        <xsl:otherwise>
                            <bibo:presents rdf:resource="{$publicationURI}" />
                        </xsl:otherwise>
                    </xsl:choose>
                    <!-- obo:RO_0001025 rdf:resource="" / --><!-- location -->
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:function>

    <xsl:function name="svfn:renderPublicationContactInfo">
        <xsl:param name="object" />

        <!-- Web pages -->
        <xsl:variable name="arxivPdfUrl" select="svfn:getRecordField($object,'arxiv-pdf-url')" />
        <xsl:variable name="authorUrl" select="svfn:getRecordField($object,'author-url')" />
        <xsl:variable name="publisherUrl" select="svfn:getRecordField($object,'publisher-url')" />
        <xsl:variable name="wosUrl" select="svfn:getRecordField($object,'author-url', 'wos,wos-lite', 'field', false())" />
        <xsl:variable name="rt1RepositoryUrl" select="($object/api:repository-items/api:repository-item/api:public-url)[1]" />

        <!-- specifically try and grab out a "Web of Science" url, which may match the author URL depending on record precedence and exclusions-->
        <!-- note do not force it with FieldOrFirst as we only want to provide a WoS link if we might be showing WoS data -->

        <xsl:call-template name="render_rdf_object">
            <xsl:with-param name="objectURI" select="concat(svfn:objectURI($object),'-webpages')" />
            <xsl:with-param name="rdfNodes">
                <xsl:call-template name="_concat_nodes_if">
                    <xsl:with-param name="nodesRequired">
                        <xsl:if test="$arxivPdfUrl/*">
                            <vcard:hasURL>
                                <xsl:call-template name="render_rdf_object">
                                    <xsl:with-param name="objectURI" select="concat(svfn:objectURI($object),'-webpages-arxiv')" />
                                    <xsl:with-param name="rdfNodes">
                                        <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#URL" />
                                        <rdfs:label>ArXiv PDF</rdfs:label>
                                        <vcard:url><xsl:value-of select="$arxivPdfUrl/api:text" /></vcard:url>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </vcard:hasURL>
                        </xsl:if>

                        <xsl:if test="$publisherUrl/*">
                            <vcard:hasURL>
                                <xsl:call-template name="render_rdf_object">
                                    <xsl:with-param name="objectURI" select="concat(svfn:objectURI($object),'-webpages-publisher')" />
                                    <xsl:with-param name="rdfNodes">
                                        <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#URL" />
                                        <rdfs:label>Publisher's Version</rdfs:label>
                                        <vcard:url><xsl:value-of select="$publisherUrl/api:text" /></vcard:url>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </vcard:hasURL>
                        </xsl:if>

                        <xsl:if test="$rt1RepositoryUrl">
                            <vcard:hasURL>
                                <xsl:call-template name="render_rdf_object">
                                    <xsl:with-param name="objectURI" select="concat(svfn:objectURI($object),'-webpages-repository')" />
                                    <xsl:with-param name="rdfNodes">
                                        <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#URL" />
                                        <rdfs:label>Repository Version</rdfs:label>
                                        <vcard:url><xsl:value-of select="$rt1RepositoryUrl" /></vcard:url>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </vcard:hasURL>
                        </xsl:if>

                        <!-- only include the author url if it is distinct from any wos URL we may have to include -->
                        <xsl:if test="$authorUrl/* and (not($wosUrl/*) or $authorUrl/api:text != $wosUrl/api:text)">
                            <vcard:hasURL>
                                <xsl:call-template name="render_rdf_object">
                                    <xsl:with-param name="objectURI" select="concat(svfn:objectURI($object),'-webpages-author')" />
                                    <xsl:with-param name="rdfNodes">
                                        <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#URL" />
                                        <rdfs:label>Author's Version</rdfs:label>
                                        <vcard:url><xsl:value-of select="$authorUrl/api:text" /></vcard:url>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </vcard:hasURL>
                        </xsl:if>

                        <xsl:if test="$wosUrl/*">
                            <vcard:hasURL>
                                <xsl:call-template name="render_rdf_object">
                                    <xsl:with-param name="objectURI" select="concat(svfn:objectURI($object),'-webpages-wos')" />
                                    <xsl:with-param name="rdfNodes">
                                        <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#URL" />
                                        <rdfs:label>View record in Web of Science ®</rdfs:label>
                                        <vcard:url><xsl:value-of select="$wosUrl/api:text" /></vcard:url>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </vcard:hasURL>
                        </xsl:if>

                        <xsl:for-each select="$object/api:records/api:record[api:native/api:field/@name='public-url']">
                            <xsl:variable name="rt2SourceName" select="./@source-name" />
                            <xsl:variable name="rt2SourceDisplayName" select="./@source-display-name" />
                            <xsl:variable name="publicUrlField" select="svfn:getRecordField($object,'public-url', $rt2SourceName)" />

                            <xsl:if test="$publicUrlField/*">
                                <vcard:hasURL>
                                    <xsl:call-template name="render_rdf_object">
                                        <xsl:with-param name="objectURI" select="concat(svfn:objectURI($object),'-webpages-', svfn:stringToURI($rt2SourceName))" />
                                        <xsl:with-param name="rdfNodes">
                                            <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#URL" />
                                            <rdfs:label>View record in <xsl:value-of select="$rt2SourceDisplayName" /> repository</rdfs:label>
                                            <vcard:url><xsl:value-of select="$publicUrlField/api:text" /></vcard:url>
                                        </xsl:with-param>
                                    </xsl:call-template>
                                </vcard:hasURL>
                            </xsl:if>
                        </xsl:for-each>

                    </xsl:with-param>
                    <xsl:with-param name="nodesToAdd">
                        <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Kind" />
                        <obo:ARG_2000029 rdf:resource="{svfn:objectURI($object)}" />
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>

    </xsl:function>

    <!-- template to "write nothing" for custom additions calls unless explicitly overridden somewhere -->
    <!-- currently only called into from elements-to-vivo-object-publication and elements-to-vivo-object-user.xsl -->
    <!-- but could be used more widely -->
    <xsl:template match="api:object[@category='publication']" mode="customAdditions" />

</xsl:stylesheet>

