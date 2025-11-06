/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.spandidos;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class SpandidosArticleIteratorFactory
        implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

  protected static Logger log =
          Logger.getLogger(SpandidosArticleIteratorFactory.class);

  /*
  Article variant format, it is not guarantee each article has a fulltext
  https://www.spandidos-publications.com/10.3892/ol.2020.11880
  https://www.spandidos-publications.com/10.3892/ol.2020.11880/abstract
  https://www.spandidos-publications.com/10.3892/ol.2020.11880/download
  https://www.spandidos-publications.com/10.3892/ol.2020.11880?text=fulltext
  https://www.spandidos-publications.com/10.3892/ol.2020.11984
  https://www.spandidos-publications.com/10.3892/ol.2020.11984/abstract
  https://www.spandidos-publications.com/10.3892/ol.2020.11984/download
  */

  // Limit to just journal volume items
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  // Match on only those patters that could be an article
  protected static final String PATTERN_TEMPLATE = "\"https://[^/]+/([0-9\\.]+/[0-9a-zA-Z\\.]+)$\"";

  public static final Pattern ABSTRACT_PATTERN = Pattern.compile("/([^/]+/[^/]+)/abstract$", Pattern.CASE_INSENSITIVE);
  public static final Pattern PDF_PATTERN = Pattern.compile("/([^/]+/[^/]+)/download$", Pattern.CASE_INSENSITIVE);
  public static final Pattern FULLTEXT_PATTERN2 = Pattern.compile("/([^/]+/[^/]+)\\?text=fulltext$", Pattern.CASE_INSENSITIVE);
  public static final Pattern FULLTEXT_PATTERN = Pattern.compile("/([^/]+/[^/]+)$", Pattern.CASE_INSENSITIVE);

  public static final String ABSTRACT_REPLACEMENT = "/$1/abstract";
  public static final String PDF_REPLACEMENT = "/$1/download";
  public static final String FULLTEXT_REPLACEMENT2 =  "/$1?text=fulltext";
  public static final String FULLTEXT_REPLACEMENT =  "/$1";

  private static final String FULL_TEXT_ALTERNATIVE = "ROLE_FULL_TEXT_ALTERNATIVE";


  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
            ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    // set up Fulltext to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
            FULLTEXT_PATTERN,
            FULLTEXT_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_HTML,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    // set up PDF to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
            PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    // set up Abstract to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
            ABSTRACT_PATTERN,
            ABSTRACT_REPLACEMENT,
            ArticleFiles.ROLE_ABSTRACT);

    builder.addAspect(
            FULLTEXT_REPLACEMENT2,
            FULL_TEXT_ALTERNATIVE);

    return builder.getSubTreeArticleIterator();
  }

  protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) {
    return new SubTreeArticleIteratorBuilder(au);
  }
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
