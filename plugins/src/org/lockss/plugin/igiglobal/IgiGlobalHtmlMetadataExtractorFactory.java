/*
 * $Id: IgiGlobalHtmlMetadataExtractorFactory.java,v 1.1.6.2 2012-06-20 00:03:03 nchondros Exp $
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

package org.lockss.plugin.igiglobal;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * Metadata on an abstract page http://www.igi-global.com/gateway/contentowned/article.aspx?titleid=55656
 * in the form of:
 * <meta content="Journal of Organizational and End User Computing (JOEUC)" name="citation_journal_title">
 * <meta content="IGI Global" name="citation_publisher">
 * <meta content="Grudnitski, Gary; Black, Robert L." name="citation_authors">
 * <meta content="The Use of an Alternative Source of Expertise for the Development of Microcomputer Expert Systems" name="citation_title">
 * <meta content="2" name="citation_volume">
 * <meta content="1" name="citation_issue">
 * <meta content="2" name="citation_firstpage">
 * <meta content="14" name="citation_lastpage">
 * <meta content="10.4018/joeuc.1990010101" name="citation_doi">
 * <meta content="http://www.igi-global.com/Bookstore/Article.aspx?TitleId=55656" name="citation_abstract_html_url">
 * <meta content="http://www.igi-global.com/ViewTitle.aspx?TitleId=55656" name="citation_pdf_url">
 * <meta content="1546-2234" name="citation_issn">
 * <meta content="en" name="citation_language">
 * <meta name="citation_keywords">
 * <meta content="1990" name="citation_date">
 */

public class IgiGlobalHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("HighWireHtmlMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new IgiGlobalHtmlMetadataExtractor();
  }

  public static class IgiGlobalHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_authors",
              new MetadataField(MetadataField.FIELD_AUTHOR,
                                MetadataField.splitAt(";")));
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      //often blank
      tagMap.put("citation_keywords", MetadataField.FIELD_KEYWORDS);
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
