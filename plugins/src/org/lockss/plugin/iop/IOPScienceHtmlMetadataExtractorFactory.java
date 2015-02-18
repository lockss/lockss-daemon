/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.iop;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/**
 * One of the articles used to get the html source for this plugin is:
 * http://iopscience.iop.org/2043-6262/1/4/043003
 */
public class IOPScienceHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(IOPScienceHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new IOPScienceHtmlMetadataExtractor();
  }

  public static class IOPScienceHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {

   
    private static MultiMap tagMap = new MultiValueMap();
    static {

      //<meta name="citation_doi" content="10.1088/2043-6262/1/4/043003" />
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      //  <meta name="citation_publication_date" content="2011-01-25" />
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      //<meta name="citation_title" content="Polymer materials with spatially..." />
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      //<meta name="citation_issn" content="2043-6262"/>
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      //<meta name="citation_volume" content="1" />
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      //<meta name="citation_issue" content="4"/>
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      //<meta name="citation_firstpage" content="043003"/>
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      //  <meta name="citation_author" content="Daisuke Fujiki"/>
      tagMap.put("citation_author",MetadataField.FIELD_AUTHOR);
      //<meta name="citation_journal_title" content="Advances in Natural Sciences:
      //Nanoscience and Nanotechnology" />
      tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
      // <meta name="citation_publisher" content="IOP Publishing" />
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
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
