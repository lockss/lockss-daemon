/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.emerald;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class Emerald2020ArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

  protected static Logger log =
          Logger.getLogger(Emerald2020ArticleIteratorFactory.class);

  /*
    https://www.emerald.com/insight/content/doi/10.1108/IJPPM-02-2021-0062/full/html
    https://www.emerald.com/insight/content/doi/10.1108/IJPPM-02-2021-0062/full/pdf?title=a-holistic-model-for-measuring-continuous-innovation-capability-of-manufacturing-industry-a-case-study
    https://www.emerald.com/insight/content/doi/10.1108/IJPPM-02-2021-0063/full/html
    https://www.emerald.com/insight/content/doi/10.1108/IJPPM-02-2021-0063/full/link=resource/id/urn:emeraldgroup.com:asset:id:article:10_1108_IJPPM-02-2021-0063/urn:emeraldgroup.com:asset:id:binary:IJPPM-02-2021-0063001.tif
    https://www.emerald.com/insight/content/doi/10.1108/IJPPM-02-2021-0063/full/link=resource/id/urn:emeraldgroup.com:asset:id:article:10_1108_IJPPM-02-2021-0063/urn:emeraldgroup.com:asset:id:binary:IJPPM-02-2021-0063002.tif
   */

  // Limit to just journal volume items
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  // Match on only those patters that could be an article
  protected static final String PATTERN_TEMPLATE = "\"%sinsight/content/doi/[^/]+/[^/]+/full/(html|pdf)\", base_url";

  public static final Pattern PDF_PATTERN = Pattern.compile("/(.*)/full/(pdf\\?title=[^/]+)", Pattern.CASE_INSENSITIVE);
  public static final Pattern FULLTEXT_PATTERN = Pattern.compile("/(.*)/full/(html)$", Pattern.CASE_INSENSITIVE);

  public static final String PDF_REPLACEMENT = "/$1/full/$2";
  public static final String FULLTEXT_REPLACEMENT =  "/$1/full/html";



  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
            ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    // set up PDF to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
            PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    // set up Fulltext to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
            FULLTEXT_PATTERN,
            FULLTEXT_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_HTML,
            ArticleFiles.ROLE_ARTICLE_METADATA);

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
