/*
 Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.cloudpublish;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class CloudPublishArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(CloudPublishArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";

  private static final String PATTERN_TEMPLATE =
      "\"^%s(journals/.*article/|read/\\?item_type=journal_article&item_id=)[0-9]+(|&mode=download)$\", base_url";

  // http://dev-liverpoolup.cloudpublish.co.uk/journals/article/20098
  private static final Pattern ART_LANDING_PATTERN = Pattern.compile("/journals/.*article/([0-9]+)$", Pattern.CASE_INSENSITIVE);
  // http://dev-liverpoolup.cloudpublish.co.uk/read/?item_type=journal_article&item_id=20098&mode=download
  private static final Pattern ART_PDF_PATTERN = Pattern.compile("/read/\\?item_type=journal_article&item_id=([0-9]+)&mode=download$", Pattern.CASE_INSENSITIVE);

  // how to get from one of the above to the other
  private final String LANDING_REPLACEMENT = "/journals/article/$1";
  private final String PDF_REPLACEMENT = "/read/?item_type=journal_article&item_id=$1&mode=download";
  private final String PDF_LANDING_REPLACEMENT = "/read/?item_type=journal_article&item_id=$1";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setRootTemplate(ROOT_TEMPLATE)
        .setPatternTemplate(PATTERN_TEMPLATE));

    builder.addAspect(ART_LANDING_PATTERN,
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(
        PDF_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);

    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_HTML);

    return builder.getSubTreeArticleIterator();
  }


  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
