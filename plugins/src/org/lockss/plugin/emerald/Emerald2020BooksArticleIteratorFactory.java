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
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class Emerald2020BooksArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

  protected static Logger log =
          Logger.getLogger(Emerald2020BooksArticleIteratorFactory.class);

  /*
  Emerald books has no full book PDF, it only have chapter PDFs, so use chapters landing page as the article page
  For example:
  Chapter landing page: https://www.emerald.com/insight/publication/doi/10.1108/S1876-0562(1991)91B
  Individual Chapters:
  https://www.emerald.com/insight/content/doi/10.1108/S1876-0562(1991)000091B001
  https://www.emerald.com/insight/content/doi/10.1108/S1876-0562(1991)000091B002
  https://www.emerald.com/insight/content/doi/10.1108/S1876-0562(1991)000091B003
  https://www.emerald.com/insight/content/doi/10.1108/S1876-0562(1991)000091B004

  or
  Chapter landing page: https://www.emerald.com/insight/publication/doi/10.1108/9780080464015
  Individual Chapters:
  https://www.emerald.com/insight/content/doi/10.1108/9780080464015-001
  https://www.emerald.com/insight/content/doi/10.1108/9780080464015-002
  https://www.emerald.com/insight/content/doi/10.1108/9780080464015-003

  */

  // Limit to just journal volume items
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  // Match on only those patters that could be an article
  protected static final String PATTERN_TEMPLATE = "\"%sinsight/(publication|content)/doi/[^/]+/[^/]+\", base_url";

  public static final Pattern BOOK_CHAPTER_LANDING_PAGE_PATTERN = Pattern.compile("(.*)insight/publication/doi/([^/]+/[^/]+)$", Pattern.CASE_INSENSITIVE);
  public static final String BOOK_CHAPTER_LANDING_PAGE_REPLACEMENT =  "$1insight/publication/doi/$2";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
            ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);


    // set up BOOK_CHAPTER_LANDING_PAGE to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        BOOK_CHAPTER_LANDING_PAGE_PATTERN,
        BOOK_CHAPTER_LANDING_PAGE_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);


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
