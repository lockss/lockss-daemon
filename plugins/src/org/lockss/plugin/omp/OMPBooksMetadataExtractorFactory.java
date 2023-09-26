/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.omp;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class OMPBooksMetadataExtractorFactory implements FileMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(OMPBooksMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new Emerald2020MetadataExtractor();
  }

  /*
  view-source:https://books.ed.ac.uk/edinburgh-diamond/catalog/book/ed-9781912669325

  <meta name="generator" content="Open Monograph Press 3.3.0.14">
  <meta name="gs_meta_revision" content="1.1"/>
  <meta name="citation_title" content="We Have Great Stuff: Colouring Book Volume 1"/>
  <meta name="citation_language" content="en"/>
  <meta name="citation_publication_date" content="2019-04-01"/>
  <meta name="citation_author" content="Stewart Lamb Cromar"/>
  <meta name="citation_author" content="Jackie Aim"/>
  <meta name="citation_author" content="Beth Rossi"/>
  <meta name="citation_author" content="Laura Beattie"/>
  <meta name="citation_author" content="Chinyere Herbert"/>
  <meta name="citation_author" content="Tracey McShane"/>
  <meta name="citation_author" content="Marta Christiansen"/>
  <meta name="citation_author" content="Kirsty McNab"/>
  <meta name="citation_author" content="Sarah Thomas"/>
  <meta name="citation_author" content="Qi Li"/>
  <meta name="citation_author" content="Asthana Devika"/>
  <meta name="citation_author" content="Emily Tanner"/>
  <meta name="citation_author" content="Crystal Check"/>
  <meta name="citation_abstract" xml:lang="en" content="The University of Edinburgh as well as having great students and staff, has great stuff. Since its very beginnings, the University has collected books, art, archives, manuscripts, musical instruments and objects to inspire its community in learning, teaching and research.
  One of the world’s great collections has been built up over hundreds of years and constantly surprises those who come to view and enjoy the items.
  The illustrations in this book are inspired by items and images within the collections and were collated by students during the Festival of Creative Learning Week 2019, and by staff in the Information Services Group.
  We hope you enjoy and become curious to find out more."/>
  <meta name="citation_doi" content="10.2218/ED.9781912669325"/>
  <meta name="citation_keywords" xml:lang="en" content="Colouring book"/>
  <meta name="citation_keywords" xml:lang="en" content="University of Edinburgh"/>
  <meta name="citation_keywords" xml:lang="en" content="Edinburgh"/>
  <meta name="citation_pdf_url" content="//books.ed.ac.uk/edinburgh-diamond/catalog/download/7/13/379"/>
  <meta name="citation_publisher" content="Edinburgh Diamond | Books"/>
  <link rel="schema.DC" href="http://purl.org/dc/elements/1.1/" />
  <meta name="DC.Creator.PersonalName" content="Stewart Lamb Cromar"/>
  <meta name="DC.Creator.PersonalName" content="Jackie Aim"/>
  <meta name="DC.Creator.PersonalName" content="Beth Rossi"/>
  <meta name="DC.Creator.PersonalName" content="Laura Beattie"/>
  <meta name="DC.Creator.PersonalName" content="Chinyere Herbert"/>
  <meta name="DC.Creator.PersonalName" content="Tracey McShane"/>
  <meta name="DC.Creator.PersonalName" content="Marta Christiansen"/>
  <meta name="DC.Creator.PersonalName" content="Kirsty McNab"/>
  <meta name="DC.Creator.PersonalName" content="Sarah Thomas"/>
  <meta name="DC.Creator.PersonalName" content="Qi Li"/>
  <meta name="DC.Creator.PersonalName" content="Asthana Devika"/>
  <meta name="DC.Creator.PersonalName" content="Emily Tanner"/>
  <meta name="DC.Creator.PersonalName" content="Crystal Check"/>
  <meta name="DC.Date.created" scheme="ISO8601" content="2019-04-01"/>
  <meta name="DC.Date.dateSubmitted" scheme="ISO8601" content="2022-12-14"/>
  <meta name="DC.Date.modified" scheme="ISO8601" content="2022-12-16"/>
  <meta name="DC.Description" xml:lang="en" content="The University of Edinburgh as well as having great students and staff, has great stuff. Since its very beginnings, the University has collected books, art, archives, manuscripts, musical instruments and objects to inspire its community in learning, teaching and research.
  One of the world’s great collections has been built up over hundreds of years and constantly surprises those who come to view and enjoy the items.
  The illustrations in this book are inspired by items and images within the collections and were collated by students during the Festival of Creative Learning Week 2019, and by staff in the Information Services Group.
  We hope you enjoy and become curious to find out more."/>
  <meta name="DC.Identifier" content="ed-9781912669325"/>
  <meta name="DC.Identifier.DOI" content="10.2218/ED.9781912669325"/>
  <meta name="DC.Identifier.URI" content="//books.ed.ac.uk/edinburgh-diamond/catalog/book/ed-9781912669325"/>
  <meta name="DC.Language" scheme="ISO639-1" content="en"/>
  <meta name="DC.Rights" content="Copyright (c) 2022 Stewart Lamb Cromar; Jackie Aim, Beth Rossi, Laura Beattie, Chinyere Herbert, Tracey McShane, Marta Christiansen, Kirsty McNab, Sarah Thomas (Illustrator); Qi Li; Asthana Devika, Emily Tanner, Crystal Check (Illustrator)"/>
  <meta name="DC.Rights" content="https://creativecommons.org/licenses/by/4.0"/>
  <meta name="DC.Source" content="Edinburgh Diamond | Books"/>
  <meta name="DC.Source.URI" content="//books.ed.ac.uk/edinburgh-diamond"/>
  <meta name="DC.Subject" xml:lang="en" content="Colouring book"/>
  <meta name="DC.Subject" xml:lang="en" content="University of Edinburgh"/>
  <meta name="DC.Subject" xml:lang="en" content="Edinburgh"/>
  <meta name="DC.Title" content="We Have Great Stuff: Colouring Book Volume 1"/>
  <meta name="DC.Type" content="Text.Book"/>
   */

  public static class Emerald2020MetadataExtractor
          implements FileMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);

      tagMap.put("DC.creator", MetadataField.DC_FIELD_CREATOR);
      tagMap.put("DC.title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("DC.type", MetadataField.DC_FIELD_TYPE);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {
      ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

      if (am.isEmpty()) {
        return;
      }
      am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);
      emitter.emitMetadata(cu, am);
    }
  }
}
