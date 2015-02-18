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
