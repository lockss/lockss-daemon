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

package org.lockss.plugin.maffey;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * Metadata on an abstract page http://www.la-press.com/the-use-of-ion-chromatography-for-the-determination--of-clean-in-place-article-a5
 *
<meta content="Cancer Informatics, Evolutionary Bioinformatics Online, bioinformatics, biomarkers, computational biology, cancer, informatics, computational biology, phylogeny, phylogenetics, evolutionary biology, science news articles, science articles, proteomics, caBIG, biomedical informatics, informatics, proteomics, genomics, biomarkers, pathology, pathology informatics, radiology, radiology informatics, cancer genes, open access review, open access, biology, microarray, Libertas Academica, biological, evidence-bas...rch papers, review articles, scientific journal, science journals, medical journal, biology, journal publisher, biology, disease, journals, peer-reviewed journals, scientific, research papers, review articles, science journals, journal publisher, international science journal, subscribe, libertas academica, peer-reviewed Open Access journals, Cancer Informatics, Evolutionary Bioinformatics Online, open access journals, peer-reviewed journals, scientific, research papers, review articles, original research" name="keywords">
<meta content="The Use of Ion Chromatography for the Determination of Clean-In-Place-200 (CIP-200) Detergent Traces" name="citation_title">
<meta content="Wilfredo Resto" name="citation_author">
<meta content="Joan Roque" name="citation_author">
<meta content="Rosamil Rey" name="citation_author">
<meta content="Héctor Colón" name="citation_author">
<meta content="" name="citation_author">
<meta content="José Zayas" name="citation_author">
<meta content="2007/02/27" name="citation_publication_date">
<meta content="Analytical Chemistry Insights" name="citation_journal_title">
<meta content="2006" name="citation_volume">
<meta content="1" name="citation_issue">
<meta content="5" name="citation_firstpage">
<meta content="12" name="citation_lastpage">
*/

public class MaffeyHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("MaffeyHtmlMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new MaffeyHtmlMetadataExtractor();
  }

  public static class MaffeyHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
      tagMap.put("citation_author",
              new MetadataField(MetadataField.FIELD_AUTHOR,
                                MetadataField.splitAt(";")));
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_keywords", MetadataField.FIELD_KEYWORDS);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
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
