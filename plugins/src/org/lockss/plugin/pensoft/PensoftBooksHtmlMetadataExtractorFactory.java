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

package org.lockss.plugin.pensoft;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class PensoftBooksHtmlMetadataExtractorFactory implements
    FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(PensoftBooksHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new PensoftHtmlMetadataExtractor();
  }

  /*
    This publisher uses "ariticle" for "book" on intention, see Jira
    so the metadata field maybe shared between journals and book, pick wisefully

    <meta name="citation_journal_title" content="Advanced Books"/>
	<meta name="citation_publisher" content="Pensoft Publishers"/>
	<meta name="citation_title" content="A practical guide to DNA-based methods for biodiversity assessment"/>
	<meta name="citation_volume" content="1"/>
	<meta name="citation_issue" content=""/>
	<meta name="citation_pdf_url" content="https://ab.pensoft.net/article/68634/download/pdf/"/>
	<meta name="citation_firstpage" content="e68634"/>
	<meta name="citation_lastpage" content=""/>
	<meta name="citation_doi" content="10.3897/ab.e68634"/>
	<meta name="citation_issn" content=""/>
	<meta name="citation_date" content="02/12/2021 17:00:00"/>
	<meta name="citation_author" content="Kat Bruce"/>
	<meta name="citation_author" content="Rosetta Blackman"/>
   */

  public static class PensoftHtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();
    static {

      tagMap.put("citation_author", new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(",")));
      tagMap.put("citation_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      // assign issn to isbn on purpose, since publisher use "article" for "book" on intention, see Jira
      tagMap.put("citation_issn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_eissn", MetadataField.FIELD_EISBN);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);         
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_journal_title", MetadataField.FIELD_SERIES_TITLE);
      tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
      
      tagMap.put("dc.title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("dc.creator", MetadataField.DC_FIELD_CREATOR); 
      tagMap.put("dc.contributor", MetadataField.DC_FIELD_CONTRIBUTOR); 
      tagMap.put("dc.type", MetadataField.DC_FIELD_TYPE); 
      tagMap.put("dc.source", MetadataField.DC_FIELD_SOURCE); 
      tagMap.put("dc.date", MetadataField.DC_FIELD_DATE); 
      tagMap.put("dc.identifier", MetadataField.DC_FIELD_IDENTIFIER); 
      tagMap.put("dc.publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("dc.rights", MetadataField.DC_FIELD_RIGHTS); 
      tagMap.put("dc.format", MetadataField.DC_FIELD_FORMAT); 
      tagMap.put("dc.language", MetadataField.DC_FIELD_LANGUAGE); 

     }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      return am;
    }
  }
}
 
