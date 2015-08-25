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

package org.lockss.plugin.bmc;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class BioMedCentralPluginHtmlMetadataExtractorFactory implements
    FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(BioMedCentralPluginHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new BioMedCentralPluginHtmlMetadataExtractor();
  }

  public static class BioMedCentralPluginHtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map BePress-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
       //general pattern for capturing start and end page number. 
      String pagenumpattern = "[pP\\. ]*([^-]+)(?:-(.+))?";
      // String authorpattern = ".*\\p{L}"; // codepoint that is a letter
      // tagMap.put("dc.creator", MetadataField.DC_FIELD_CONTRIBUTOR);
      // <meta name="citation_authors"
      // content="G Poelmans, J K Buitelaar, D L Pauls, B Franke" />
     tagMap.put("citation_authors", new MetadataField(
          MetadataField.FIELD_AUTHOR, MetadataField.splitAt(",")));
     // <meta name="DC.Date.issued" scheme="ISO860pp\\.[ ]+.*-(.*)1" content="2009-12-29" />
      tagMap.put("dc.date", MetadataField.FIELD_DATE);
      // <meta name="citation_title" 
      // content="Altered arachidonic acid cascade enzymes in postmortem 
      //brain from bipolar disorder patients"/>
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      // <meta name="citation_journal_title" content="Molecular Psychiatry" />
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      // <meta name="citation_volume" content="19" />
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      // <meta name="citation_issue" content="2" />
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      // <meta name="citation_firstpage" content="pp. 12-13|p24|13-14" />
      tagMap.put("citation_firstpage", new MetadataField(
                 MetadataField.FIELD_START_PAGE,
                 MetadataField.groupExtractor(pagenumpattern, 1)));
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      // <meta name="prism.issn" content="0955-9930" />
      // tagMap.put("prism.issn", MetadataField.FIELD_ISSN);
      // <meta name="prism.eIssn" content="1476-5489" />
      tagMap.put("citation_issn", MetadataField.FIELD_EISSN);
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
 
