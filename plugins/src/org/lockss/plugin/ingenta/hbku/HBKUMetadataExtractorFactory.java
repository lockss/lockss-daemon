/*
 * $Id$
 */

/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.ingenta.hbku;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

import java.io.IOException;

public class HBKUMetadataExtractorFactory implements FileMetadataExtractorFactory {

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new HBKUMetadataExtractorFactory.HBKUMetadataExtractor();
  }

  /*
  <meta name="dc.title" content="A new journal is born" />
  <meta name="dc.publisher" content="Hamad bin Khalifa University Press (HBKU Press)"/>
  <meta name="dc.type" scheme="DCMIType" content="Text"/>
  <meta name="author" content="Magdi Yacoub" />
  <meta name="description" content="QScience.com is the innovative and collaborative, peer-reviewed, online publishing platform from Hamad bin Khalifa University Press (HBKU Press). It offers a fast and transparent Open Access scholarly publishing process, which is centered on the author, bringing their research to a global audience." />
  <meta name="dc.creator" content="Magdi Yacoub" />
  <meta name="dc.identifier" content="doi:10.5339/ahcsps.2011.1"/>
  <meta name="dc.date" content="2011/04/14" />
  <meta name="CRAWLER.fullTextLink" content="https://www.qscience.com/content/journals/10.5339/ahcsps.2011.1?crawler=true"/>
  <meta name="CRAWLER.indexEntryLink" content="https://www.qscience.com/content/journals/10.5339/ahcsps.2011.1"/>
  <meta name="citation_journal_title" content="Aswan Heart Centre Science &amp; Practice Series" />
  <meta name="citation_issn" content="2220-2730" />
  <meta name="citation_doi" content="10.5339/ahcsps.2011.1" />
  <meta name="citation_publication_date" content="2011/04/14" />
  <meta name="citation_date" content="2011/04/14" />
  <meta name="citation_year" content="2011" />
  <meta name="citation_online_date" content="2011/04/14" />
  <meta name="citation_title" content="A new journal is born" />
  <meta name="citation_author" content="Magdi Yacoub" />
  <meta name="citation_author_institution" content="National Heart &amp; Lung Institute, Imperial College, London, SW7 2AZ, UK" />
  <meta name="citation_volume" content="2011" />
  <meta name="citation_issue" content="1" />
  <meta name="citation_firstpage" content="1" />
  <meta name="citation_publisher" content="Hamad bin Khalifa University Press (HBKU Press)" />
  <meta name="citation_language" content="en" />
  <meta name="citation_fulltext_html_url" content="https://www.qscience.com/content/journals/10.5339/ahcsps.2011.1?crawler=true" />
  <meta name="citation_pdf_url" content="https://www.qscience.com/content/journals/10.5339/ahcsps.2011.1?crawler=true&mimetype=application/pdf" />
  <meta name="citation_xml_url" content="https://www.qscience.com/content/journals/10.5339/ahcsps.2011.1?crawler=true&mimetype=application/xml" />
  <meta name="citation_fulltext_world_readable" content="" />
   */

  public static class HBKUMetadataExtractor
          extends SimpleHtmlMetaTagMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_author", new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(",")));
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
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
