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

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/*
 * Metadata on abstract page:
 * http://www.pensoft.net/journals/ZZZ/article/1548/abstract/Article-title-for-this-Example
<meta name="Allow-search" content="yes">
<meta name="Audience" content="all">
<meta name="Rating" content="all">
<meta name="Voluntary content rating" content="all">
<meta name="resource-type" content="document">
<meta name="revisit-after" content="1 day">
<meta name="distribution" content="global">
<meta name="robots" content="index, follow">
<meta name="keywords" content="woodborers; Palearctic; Oriental; Hyperxiphia">
<meta name="description" content="Five species of Euxiphydria are recognized, E. leucopoda Takeuchi, 1938, from Japan, E. potanini (Jakovlev, 1891) from Japan, Russia, Korea, and China, E. pseud">
<meta name="title" content="Review of Article title for this Example"/><meta name="citation_pdf_url" content="http://www.pensoft.net/inc/journals/download.php?fileTable=J_GALLEYS&fileId=3070"/><meta name="citation_xml_url" content="http://www.pensoft.net/inc/journals/download.php?fileTable=J_GALLEYS&fileId=3069"/><meta name="citation_fulltext_html_url" content="http://www.pensoft.net/journals/ZZZ/article/1548/Article-title-for-this-Example"/>
<meta name="citation_abstract_html_url" content="http://www.pensoft.net/journals/ZZZ/article/1548/abstract/Article-title-for-this-Example"/>
<meta name="dc.title" content="Review of Article title for this Example" />
<meta name="dc.creator" content="Smith Davids" />
<meta name="dc.contributor" content="Smith Davids" />
<meta name="dc.creator" content="Abcde  Stuvwxy" />
<meta name="dc.contributor" content="Abcde  Stuvwxy" />
<meta name="dc.type" content="Research Article" />
<meta name="dc.source" content="Journal of ZZZ Research 2011 23: 1" />
<meta name="dc.date" content="2011-10-21" />
<meta name="dc.identifier" content="10.3897/ZZZ.23.1548" />
<meta name="dc.publisher" content="Pensoft Publishers" />
<meta name="dc.rights" content="http://creativecommons.org/licenses/by/3.0/" />
<meta name="dc.format" content="text/html" />
<meta name="dc.language" content="en" />

<meta name="prism.publicationName" content="Journal of ZZZ Research" />
<meta name="prism.issn" content="1314-2607" />
<meta name="prism.publicationDate" content="2011-10-21" /> 
<meta name="prism.volume" content="23" />

<meta name="prism.doi" content="10.3897/ZZZ.23.1548" />
<meta name="prism.section" content="Research Article" />
<meta name="prism.startingPage" content="1" />
<meta name="prism.endingPage" content="22" />
<meta name="prism.copyright" content="2011 Smith Davids, Abcde  Stuvwxy" />
<meta name="prism.rightsAgent" content="Journal of Hymenoptera Research@pensoft.net" />

<meta name="eprints.title" content="Review Review of Article title for this Example" />
<meta name="eprints.creators_name" content="Davids, Smith " /> <meta name="eprints.creators_name" content="Stuvwxy, Abcde " /> 
<meta name="eprints.type" content="Research Article" />
<meta name="eprints.datestamp" content="2011-10-21" />
<meta name="eprints.ispublished" content="pub" />
<meta name="eprints.date" content="2011" />
<meta name="eprints.date_type" content="published" />
<meta name="eprints.publication" content="Pensoft Publishers" />
<meta name="eprints.volume" content="23" />
<meta name="eprints.pagerange" content="1-22" />

<meta name="citation_journal_title" content="Journal of Hymenoptera Research" />
<meta name="citation_publisher" content="Pensoft Publishers" />
<meta name="citation_author" content="Smith Davids" /> <meta name="citation_author" content="Abcde  Stuvwxy" /> 
<meta name="citation_title" content="Review of Article title for this Example" />
<meta name="citation_volume" content="23" />

<meta name="citation_firstpage" content="1" />
<meta name="citation_lastpage" content="22" />
<meta name="citation_doi" content="10.3897/ZZZ.23.1548" />
<meta name="citation_issn" content="1314-2607" />
<meta name="citation_date" content="2011/10/21" />

 */
public class PensoftHtmlMetadataExtractorFactory implements
    FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("PensoftMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new PensoftHtmlMetadataExtractor();
  }

  public static class PensoftHtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map BePress-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {

      tagMap.put("citation_author", new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(",")));
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_issn", MetadataField.FIELD_EISSN);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);         
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      
      tagMap.put("dc.title", MetadataField.FIELD_JOURNAL_TITLE); 
      tagMap.put("dc.creator", MetadataField.DC_FIELD_CREATOR); 
      tagMap.put("dc.contributor", MetadataField.DC_FIELD_CONTRIBUTOR); 
      tagMap.put("dc.type", MetadataField.DC_FIELD_TYPE); 
      tagMap.put("dc.source", MetadataField.DC_FIELD_SOURCE); 
      tagMap.put("dc.date", MetadataField.DC_FIELD_DATE); 
      tagMap.put("dc.identifier", MetadataField.DC_FIELD_IDENTIFIER); 
      //tagMap.put("dc.publisher", MetadataField.DC_FIELD_PUBLISHER); 
      tagMap.put("dc.rights", MetadataField.DC_FIELD_RIGHTS); 
      tagMap.put("dc.format", MetadataField.DC_FIELD_FORMAT); 
      tagMap.put("dc.language", MetadataField.DC_FIELD_LANGUAGE); 
    
      //tagMap.put("keywords", MetadataField.FIELD_KEYWORDS); 

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
 
