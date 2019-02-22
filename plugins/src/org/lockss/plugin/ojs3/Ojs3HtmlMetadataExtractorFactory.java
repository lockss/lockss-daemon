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

package org.lockss.plugin.ojs3;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * Metadata on an abstract page - there is additional 
 * <meta name="citation_journal_title" content="Economic Development in Higher Education"/>
 * <meta name="citation_author" content="G. Jason Jolley"/>
 * <meta name="citation_title" content="U.S. Economic Development Administration University Centers: Leveraging Federal Dollars Toward Best Practices"/>
 * <meta name="citation_date" content="2016/12/12"/>
 * <meta name="citation_volume" content="1"/>
 * <meta name="citation_volume" content="1"/>
 * <meta name="citation_abstract_html_url" content="https://scholarworks.iu.edu/journals/index.php/jedhe/article/view/19369"/>
 * <meta name="citation_pdf_url" content="https://scholarworks.iu.edu/journals/index.php/jedhe/article/download/19369/28943"/>
 * <meta name="DC.Creator.PersonalName" content="G. Jason Jolley"/>
 * <meta name="DC.Date.created" scheme="ISO8601" content="2016-12-12"/>
 * <meta name="DC.Identifier" content="19369"/>
 * <meta name="DC.Identifier.URI" content="https://scholarworks.iu.edu/journals/index.php/jedhe/article/view/19369"/>
 * <meta name="DC.Source" content="Economic Development in Higher Education"/>
 * <meta name="DC.Source.Volume" content="1"/>
 * <meta name="DC.Source.URI" content="https://scholarworks.iu.edu/journals/index.php/jedhe"/>
 * <meta name="DC.Title" content="U.S. Economic Development Administration University Centers: Leveraging Federal Dollars Toward Best Practices"/>
 */

public class Ojs3HtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(Ojs3HtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new Ojs3HtmlMetadataExtractor();
  }

  public static class Ojs3HtmlMetadataExtractor
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
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
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
