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
