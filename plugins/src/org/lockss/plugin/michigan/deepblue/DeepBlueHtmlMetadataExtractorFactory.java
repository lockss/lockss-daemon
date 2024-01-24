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

package org.lockss.plugin.michigan.deepblue;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class DeepBlueHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(DeepBlueHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                               String contentType)
      throws PluginException {
    return new DeepBlueOaiHtmlMetadataExtractor();
  }

  /*
  <meta name="DC.creator" content="Boyer, Douglas M." />
  <meta name="DC.creator" content="Gingerich, Philip D." />
  <meta name="DCTERMS.dateAccepted" content="2019-10-11T13:56:39Z" scheme="DCTERMS.W3CDTF" />
  <meta name="DCTERMS.available" content="2019-10-11T13:56:39Z" scheme="DCTERMS.W3CDTF" />
  <meta name="DCTERMS.issued" content="2019-10-10" scheme="DCTERMS.W3CDTF" />
  <meta name="DCTERMS.bibliographicCitation" content="Papers No. 38" xml:lang="en_US" />
  <meta name="DC.identifier" content="https://hdl.handle.net/2027.42/151767" scheme="DCTERMS.URI" />
  <meta name="DCTERMS.extent" content="274" xml:lang="en_US" />
  <meta name="DC.publisher" content="Museum of Paleontology, The University of Michigan" />
  <meta name="DC.title" content="Skeleton of Late Paleocene Plesiadapis Cookei (Mammalia, Euarchonta): Life History, Locomotion, and Phylogenetic Relationships" xml:lang="en_US" />
  <meta name="DC.type" content="Book" xml:lang="en_US" />
  <meta name="DC.subject" content="Geology and Earth Sciences" />
  <meta name="DC.subject" content="Anthropology and Archaeology" />
  <meta name="DC.subject" content="Science" />
  <meta name="DC.subject" content="Social Sciences" />
  <meta name="DC.contributor" content="Museum of Paleontology, The University of Michigan,  Research Museum Center, 3600 Varsity Drive, Ann Arbor, MI 48108, U.S.A." xml:lang="en_US" />
  <meta name="DC.contributor" content="Dept of Evolutionary Anthropology, Duke University, Biological Science Bldg, 130 Science Drive, Durham, NC, 27708, U.S.A." xml:lang="en_US" />
  <meta name="DC.contributor" content="Ann Arbor" />
  <meta name="DC.description" content="https://deepblue.lib.umich.edu/bitstream/2027.42/151767/1/Papers on Paleontology 38 10-10-2019 - High Res.pdf" />
  <meta name="DC.description" content="https://deepblue.lib.umich.edu/bitstream/2027.42/151767/2/Papers on Paleontology 38 10-10-2019 - low res.pdf" />
  <meta name="DC.description" content="Description of Papers on Paleontology 38 10-10-2019 - High Res.pdf : Papers 38 - High Resolution" />
  <meta name="DC.description" content="Description of Papers on Paleontology 38 10-10-2019 - low res.pdf : Papers 38 - Low Resolution" />
  <meta content="Book" name="citation_keywords">
  <meta content="Skeleton of Late Paleocene Plesiadapis Cookei (Mammalia, Euarchonta): Life History, Locomotion, and Phylogenetic Relationships" name="citation_title">
  <meta content="Museum of Paleontology, The University of Michigan" name="citation_publisher">
  <meta content="Boyer, Douglas M." name="citation_author">
  <meta content="Gingerich, Philip D." name="citation_author">
  <meta content="http://deepblue.lib.umich.edu/bitstream/2027.42/151767/1/Papers%20on%20Paleontology%2038%2010-10-2019%20-%20High%20Res.pdf" name="citation_pdf_url">
  <meta content="2019-10-10" name="citation_date">
  <meta content="http://deepblue.lib.umich.edu/handle/2027.42/151767" name="citation_abstract_html_url">
   */

  public static class DeepBlueOaiHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_abtract_html_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);

      tagMap.put("DC.title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("DC.publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("DC.identifier", MetadataField.DC_FIELD_IDENTIFIER);

      //tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
      //tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      //tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      //tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      //tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      //tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
    throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      String url = am.get(MetadataField.FIELD_ACCESS_URL);
      ArchivalUnit au = cu.getArchivalUnit();
      if (url == null || url.isEmpty() || !au.makeCachedUrl(url).hasContent()) {
        url = cu.getUrl();
      }
      am.replace(MetadataField.FIELD_ACCESS_URL,
                 AuUtil.normalizeHttpHttpsFromBaseUrl(au, url));
      return am;
    }
  }
}
