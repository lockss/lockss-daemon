/*
 * $Id: NatureHtmlMetadataExtractorFactory.java,v 1.5.2.1 2011-03-25 17:58:42 pgust Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.nature;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class NatureHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("NatureMetadataExtractorFactory");
 
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new NatureHtmlMetadataExtractor();
  }

  public static class NatureHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map BePress-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dc.creator", MetadataField.DC_FIELD_CONTRIBUTOR);
      // <meta name="citation_authors" content="G Poelmans, J K Buitelaar, D L Pauls, B Franke" />
      tagMap.put("citation_authors", 
                 new MetadataField(MetadataField.FIELD_AUTHOR,
                                   MetadataField.splitAt(",")));
      // <meta name="citation_date" content="2009-12-29" />
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      // <meta name="citation_title" content="Altered arachidonic acid cascade enzymes in postmortem brain from bipolar disorder patients" />
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      // <meta name="citation_journal_title" content="Molecular Psychiatry" />
      tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
      // <meta name="citation_volume" content="19" />
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      // <meta name="citation_issue" content="2" />
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      // <meta name="citation_firstpage" content="119" />
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      // <meta name="prism.issn" content="0955-9930" />
      tagMap.put("prism.issn", MetadataField.FIELD_ISSN);
      // <meta name="prism.eIssn" content="1476-5489" />
      tagMap.put("prism.eIssn", MetadataField.FIELD_EISSN);
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
