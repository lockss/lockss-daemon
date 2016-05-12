/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dove;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * Metadata on an abstract page 
 * https://www.dovepress.com/treating-melanoma-in-adolescents-and-young-adults-challenges-and-solut-peer-reviewed-article-COAYA 
 *
 * <meta name="citation_title" content="Treating melanoma in adolescents and young adults: challenges and solutions">
 * <meta name="citation_publication_date" content="2015/09/15">
 * <meta name="citation_journal_title" content="Clinical Oncology in Adolescents and Young Adults">
 * <meta name="citation_journal_issn" content="2230-2263">
 * <meta name="citation_journal_abbrev" content="COAYA">
 * <meta name="citation_publisher" content="Dove Press">
 * <meta name="citation_volume" content="Volume 5">
 * <meta name="citation_firstpage" content="75">
 * <meta name="citation_lastpage" content="86">
 * <meta name="citation_doi" content="10.2147/COAYA.S90563">
 * <meta name="citation_pdf_url" content="https://www.dovepress.com/getfile.php?fileID=27054">
 * <meta name="citation_author" content="Damon Reed">
 * <meta name="citation_author" content="Jane  L. Messina">
 * <meta name="citation_author" content="Vernon K. Sondak">
 * <meta name="citation_author" content="Radhika Sreeraman Kumar">
 */

public class DoveHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(DoveHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new DoveHtmlMetadataExtractor();
  }

  public static class DoveHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_author",
              new MetadataField(MetadataField.FIELD_AUTHOR,
                                MetadataField.splitAt(";")));
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_doi",  MetadataField.FIELD_DOI);
      tagMap.put("citation_issn",  MetadataField.FIELD_ISSN);
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
