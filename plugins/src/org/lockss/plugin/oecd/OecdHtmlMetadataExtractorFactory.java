/*
Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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
package org.lockss.plugin.oecd;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

import java.io.IOException;

public class OecdHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
/*
<meta name="dc.title" content="The world and “The world business cycle chronology”">
<meta name="dc.type" scheme="DCMIType" content="Text">
<meta name="dc.publisher" content="OECD">
<meta name="author" content="Allan Layton|Anirvan Banerji|Lakshman Achuthan">
<meta name="dc.creator" content="Allan Layton">
<meta name="dc.creator" content="Anirvan Banerji">
<meta name="dc.creator" content="Lakshman Achuthan">
<meta name="dc.identifier" content="doi:https://doi.org/10.1787/jbcma-2015-5jrtfl953jxp">
<meta name="dc.date" content="2015/09/25">
<meta name="description" property="og:description" content="Twenty-one individual country business cycle chronologies, maintained and updated by the Economic Cycle Research Institute (ECRI), are analysed for their degree of synchronisation with a proposed “world business cycle chronology”. Several key results...">
--<meta name="citation_journal_title" content="OECD Journal: Journal of Business Cycle Measurement and Analysis">
--<meta name="citation_issn" content="19952899">
--<meta name="citation_doi" content="https://doi.org/10.1787/jbcma-2015-5jrtfl953jxp">
--<meta name="citation_publication_date" content="2015/10/30">
<meta name="citation_online_date" content="2015/09/25">
<meta name="citation_year" content="2015">
--<meta name="citation_title" content="The world and “The world business cycle chronology”">
--<meta name="citation_author" content="Allan Layton">
--<meta name="citation_author" content="Anirvan Banerji">
--<meta name="citation_author" content="Lakshman Achuthan">
--<meta name="citation_volume" content="2015">
--<meta name="citation_issue" content="1">
--<meta name="citation_firstpage" content="23">
--<meta name="citation_lastpage" content="40">
--<meta name="citation_publisher" content="OECD">
--<meta name="citation_language" content="en">
--<meta name="citation_abstract" content="Twenty-one individual country business cycle chronologies, maintained and updated by the Economic Cycle Research Institute (ECRI), are analysed for their degree of synchronisation with a proposed “world business cycle chronology”. Several key results emerge. First, perhaps not surprisingly, the world’s four 20th Century locomotor economies of the US, UK, Germany and Japan are statistically significantly and reasonably strongly positively synchronised with the world cycle. Second, European countries in the sample are either positively synchronised with the world cycle at zero lag or with a lag of around three months. Third, the NAFTA countries (US, Canada and Mexico) are, perhaps again not unexpectedly, quite strongly positively synchronised with the world cycle at zero lag and with each other. Fourth, the single South American country included in the sample, Brazil, is strongly positively synchronised with the world cycle at zero lag as well as with the NAFTA countries – but behaves very differently from China and India with respect to the world cycle. Fifth, interestingly, the newly industrialized East Asian countries included in the sample appear to lead the world cycle by about three to nine months. Finally, and very interestingly, there appears to be some a priori evidence of a long leading negative synchronisation between the commodity exporting countries in the sample and the world cycle. Key words: world business cycle, synchronisation JEL classification: E32, E37">
--<meta name="citation_abstract_html_url" content="https://www.oecd-ilibrary.org/economics/the-world-and-the-world-business-cycle-chronology_jbcma-2015-5jrtfl953jxp">
 */

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType)
      throws PluginException {
    return new OecdHtmlMetadataExtractor();
  }

  public static class OecdHtmlMetadataExtractor
      implements FileMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("dc.title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_abstract", MetadataField.FIELD_ABSTRACT);
      tagMap.put("description", MetadataField.FIELD_ABSTRACT);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_abstract_html_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("citation_language", MetadataField.FIELD_LANGUAGE);
      tagMap.put("dc.identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);

    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am =
          new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      emitter.emitMetadata(cu, am);
    }

  }
}