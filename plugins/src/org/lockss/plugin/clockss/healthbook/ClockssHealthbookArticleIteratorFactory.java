/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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
package org.lockss.plugin.clockss.healthbook;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class ClockssHealthbookArticleIteratorFactory  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(ClockssHealthbookArticleIteratorFactory.class);
  protected static final String ALL_PDF_PATTERN_TEMPLATE = "\"^%s[^/]+/(.*)\\.pdf$\",base_url";
  // e.g.
  // /24206-long-term-sustained-disease-control-with-immunotherapy-in-chemotherapy-refractory-merkel-cell-carcinoma/24206-long-term-sustained-disease-control-with-immunotherapy-in-chemotherapy-refractory-merkel-cell-carcinoma (1).pdf
  private static final Pattern PDF_PATTERN = Pattern.compile("/([^/]+)/([^/]*)\\.pdf$", Pattern.CASE_INSENSITIVE);
  private static final String PDF_REPLACEMENT = "/$1/$2.pdf";

  // PDF & Bibtex always exists. XML usually.
  // PDF rarely has ' (1)' or somesuch before '.pdf' so we can try to find by the directory path instead.
  public static final String BIBTEX_REPLACEMENT = "/$1/$1.bibtex";
  public static final String XML_REPLACEMENT = "/$1/$1.xml";
  public static final String BIBTEX_REPLACEMENT2 = "/$1/$2.bibtex";
  public static final String XML_REPLACEMENT2 = "/$1/$2.xml";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
      .setTarget(target)
      .setPatternTemplate(ALL_PDF_PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map

    // set up PDF to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(PDF_PATTERN,
      PDF_REPLACEMENT,
      ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(Arrays.asList(
        XML_REPLACEMENT,
        XML_REPLACEMENT2
      ),
      ArticleFiles.ROLE_CITATION);

    builder.addAspect(Arrays.asList(
        BIBTEX_REPLACEMENT,
        BIBTEX_REPLACEMENT2
      ),
      ArticleFiles.ROLE_CITATION_BIBTEX);

    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
      ArticleFiles.ROLE_CITATION_BIBTEX,
      ArticleFiles.ROLE_CITATION);

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
