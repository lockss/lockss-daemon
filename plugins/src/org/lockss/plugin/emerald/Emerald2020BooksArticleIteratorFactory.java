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
