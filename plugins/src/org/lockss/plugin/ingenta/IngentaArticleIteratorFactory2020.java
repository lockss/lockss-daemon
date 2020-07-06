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

package org.lockss.plugin.ingenta;

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

public class IngentaArticleIteratorFactory2020 implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(IngentaArticleIteratorFactory2020.class);

  /*
  The Senior Care Pharmacist
  www.ingentaconnect.com/content/ascp/tscp
  journal id: tscp
  ISSN - 2639-9636
  eISSN - 2639-9644
  publisher id: ascp
  vol. 34, No. 1, 2019
   */
  // Original link: https://api.ingentaconnect.com/content/ascp/tscp/2019/00000034/00000001/art00002
  // Article:       https://api.ingentaconnect.com/content/ascp/tscp/2019/00000034/00000001/art00002?crawler=true&mimetype=text/html
  // PDF:           https://api.ingentaconnect.com/content/ascp/tscp/2019/00000034/00000001/art00002?crawler=true&mimetype=application/pdf


  protected static final String ROOT_TEMPLATE = "\"%scontent/%s/%s\", api_url, publisher_id, journal_id";
  private static final String PATTERN_TEMPLATE = "\"^%scontent/%s/%s/([0-9]{4}/[^/]+/[^/]+/art[0-9]{5})\\?crawler=true\", " +
          "api_url, publisher_id, journal_id";

  public static final Pattern HTML_PATTERN = Pattern.compile("/([0-9]{4}/[^/]+/[^/]+/art[0-9]{5})\\?crawler=true&mimetype=text/html", Pattern.CASE_INSENSITIVE);
  public static final Pattern PDF_PATTERN = Pattern.compile("/([0-9]{4}/[^/]+/[^/]+/art[0-9]{5})\\?crawler=true&mimetype=application/pdf", Pattern.CASE_INSENSITIVE);
  public static final String HTML_REPLACEMENT = "/$1?crawler=true&mimetype=text/html";
  private static final String PDF_REPLACEMENT = "/$1?crawler=true&mimetype=application/pdf";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
            ROOT_TEMPLATE,
            PATTERN_TEMPLATE,
            Pattern.CASE_INSENSITIVE);

    builder.addAspect(HTML_PATTERN,
            HTML_REPLACEMENT,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
