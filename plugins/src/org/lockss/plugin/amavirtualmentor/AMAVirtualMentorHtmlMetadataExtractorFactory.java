/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
