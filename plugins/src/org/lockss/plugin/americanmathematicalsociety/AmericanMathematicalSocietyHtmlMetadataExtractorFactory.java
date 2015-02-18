/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americanmathematicalsociety;

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 Metadata on page http://www.ams.org/journals/jams/2013-26-01/S0894-0347-2012-00742-5/ in the form of:
<meta content="Journal of the American Mathematical Society" name="citation_journal_title">
<meta content="J. Amer. Math. Soc." name="citation_journal_abbrev">
<meta content="http://www.ams.org/jams/2013-26-01/S0894-0347-2012-00742-5/" name="citation_abstract_html_url">
<meta content="http://www.ams.org/jams/2013-26-01/S0894-0347-2012-00742-5/S0894-0347-2012-00742-5.pdf" name="citation_pdf_url">
<meta content="0894-0347" name="citation_issn">
<meta content="1088-6834" name="citation_issn">
<meta content="Masur, Howard" name="citation_author">
<meta content="Department of Mathematics, University of Chicago, Chicago, Illinois 60637" name="citation_author_institution">
<meta content="masur@math.uic.edu" name="citation_author_email">
<meta content="Schleimer, Saul" name="citation_author">
<meta content="Mathematics Institute, University of Warwick, Coventry, CV4 7AL, United Kingdom" name="citation_author_institution">
<meta content="s.schleimer@warwick.ac.uk" name="citation_author_email">
<meta content="The geometry of the disk complex" name="citation_title">
<meta content="2012/08/22" name="citation_online_date">
<meta content="2013" name="citation_publication_date">
<meta content="26" name="citation_volume">
<meta content="1" name="citation_issue">
<meta content="1" name="citation_firstpage">
<meta content="62" name="citation_lastpage">
<meta content="10.1090/S0894-0347-2012-00742-5" name="citation_doi">
 */

public class AmericanMathematicalSocietyHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(
      AmericanMathematicalSocietyHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
      throws PluginException {
    return new AmericanMathematicalSocietyHtmlMetadataExtractor();
  }

  public static class AmericanMathematicalSocietyHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      // tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER); // XXX
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author",
          new MetadataField(MetadataField.FIELD_AUTHOR,
                            MetadataField.splitAt(",")));
      tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
    }
    
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      
      am.cook(tagMap);
      am.putIfBetter(MetadataField.FIELD_PUBLISHER, "American Mathematical Society");
      return am;
    }
    
  }
  
}
