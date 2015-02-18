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

package org.lockss.plugin.amavirtualmentor;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
/**
 * One of the articles used to get the html source for this plugin is:
 * http://virtualmentor.ama-assn.org/2011/06/ccas1-1106.html
 */

public class AMAVirtualMentorHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("AMAVirtualMentorHtmlMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new AMAVirtualMentorHtmlMetadataExtractor();
  }

  public static class AMAVirtualMentorHtmlMetadataExtractor 
    implements FileMetadataExtractor {

    // Map AMAVirtualMentor's Google Scholar HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
//      <meta name="citation_doi" content="10.1001/virtualmentor.2011.13.6.ccas1-1106">
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
//    <meta name="citation_date" content="06/01/2011">
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
//      <meta name="citation_volume" content="13">
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
//      <meta name="citation_issue" content="6">
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
//      <meta name="citation_firstpage" content="336">
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
//      <meta name="citation_authors" content="Fisch, Michael J.;Lee, Richard T.">
      tagMap.put("citation_authors",
                 new MetadataField(MetadataField.FIELD_AUTHOR,
                                   MetadataField.splitAt(";")));
//      <meta name="citation_title" content="When Patients Choose CAM over EBM—How to Negotiate Treatment">
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
//      <meta name="citation_journal_title" content="Virtual Mentor">
      tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
    throws IOException {
      ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      emitter.emitMetadata(cu,am);
    }
  }
}
