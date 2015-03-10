/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class NatureHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(NatureHtmlMetadataExtractorFactory.class);

  @Override
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
      tagMap.put("dc.creator", MetadataField.DC_FIELD_CREATOR);
      // use DC.CREATOR because separator for citation_author varies
      tagMap.put("dc.creator", MetadataField.FIELD_AUTHOR);
      // <meta name="citation_date" content="2009-12-29" />
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      // <meta name="citation_title" content="Altered arachidonic acid cascade enzymes in postmortem brain from bipolar disorder patients" />
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      // <meta name="citation_journal_title" content="Molecular Psychiatry" />
      tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
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
      // Since we know it and since Metadata requires it, set it manually if necessary
      if (am.get(MetadataField.FIELD_PUBLISHER) == null) {
        am.put(MetadataField.FIELD_PUBLISHER, "Nature Publishing Group");
      }
      return am;
    }
  }
}
