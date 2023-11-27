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
