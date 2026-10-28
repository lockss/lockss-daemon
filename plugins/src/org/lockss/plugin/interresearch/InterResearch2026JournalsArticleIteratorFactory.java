/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.interresearch;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Iterates article landing pages:
 *   https://www.int-res.com/journals/meps/articles/meps14992
 *
 * The full-text PDF (/articles/meps2025/774/m774p073.pdf) cannot be derived
 * from the landing-page URL (page numbers are only in the HTML), so there is
 * no PDF aspect here. The landing page is used as the full-text CU and as the
 * metadata source; the metadata extractor sets access.url to the PDF when the
 * PDF named in citation_pdf_url was collected.
 *
 * NOTE: the landing-page URL carries no volume, so this pattern matches any
 * landing page in the AU, including any that leaked in from another volume.
 * Those are dropped by the volume check in
 * InterResearch2026JournalsHtmlMetadataExtractorFactory.
 */
public class InterResearch2026JournalsArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log =
      Logger.getLogger(InterResearch2026JournalsArticleIteratorFactory.class);

  private static final String ROOT_TEMPLATE =
      "\"%sjournals/%s/articles/\", base_url, journal_id";

  private static final String PATTERN_TEMPLATE =
      "\"^%sjournals/%s/articles/%s[0-9]+$\", base_url, journal_id, journal_id";

  private static final Pattern LANDING_PATTERN =
      Pattern.compile("/journals/([^/]+)/articles/([^/?#]+)$",
                      Pattern.CASE_INSENSITIVE);

  private static final String LANDING_REPLACEMENT = "/journals/$1/articles/$2";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setRootTemplate(ROOT_TEMPLATE)
        .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    builder.addAspect(LANDING_PATTERN,
                      LANDING_REPLACEMENT,
                      ArticleFiles.ROLE_ABSTRACT,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    // No derivable PDF URL; use the landing page so every article has a
    // full-text CU.
    builder.setFullTextFromRoles(ArticleFiles.ROLE_ABSTRACT);

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
