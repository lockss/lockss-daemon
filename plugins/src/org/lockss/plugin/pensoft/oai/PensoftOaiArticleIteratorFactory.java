/*
 * $Id:$
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pensoft.oai;

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
import org.lockss.util.UrlUtil;

public class PensoftOaiArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(PensoftOaiArticleIteratorFactory.class);

  protected static final Pattern ABSTRACT_PATTERN = Pattern.compile(
      "article(?:/|s[.]php[?]id=)([0-9]+)/?$",
      Pattern.CASE_INSENSITIVE);
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "article/([0-9]+)/download/pdf/?$",
      Pattern.CASE_INSENSITIVE);

  //Some are full text HTML and some are abstracts
  //Abstract = http://zookeys.pensoft.net/articles.php?id=1929
  //Full Text = http://bdj.pensoft.net/articles.php?id=995
  protected static final String ABSTRACT_0_REPLACEMENT = "articles.php?id=$1";
  protected static final String ABSTRACT_1_REPLACEMENT = "article/$1";
  //http://zookeys.pensoft.net/lib/ajax_srv/article_elements_srv.php?action=download_pdf&item_id=1929
  protected static final String PDF_0_REPLACEMENT = "lib/ajax_srv/article_elements_srv.php?action=download_pdf&item_id=$1";
  protected static final String PDF_1_REPLACEMENT = "article/$1/download/pdf/";
  // PDFs can now also show up with a fileid after the terminating slash - this isn't dicsoverable from the html article pattern
  // protected static final String PDF_3_REPLACEMENT = "article/$1/download/pdf/xxxyy";
  // not clear how to proceed at this point without 
  // for example: https://biodiscovery.pensoft.net/article/8964
  // has a pdf of : https://biodiscovery.pensoft.net/article/8964/download/pdf/283658
  // We could look for any CUs that match https://biodiscovery.pensoft.net/article/8964/download/pdf/
  // but for now, just leave it not found - we know that we get the article because that's how we 
  // start from oai-pmh
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    String strip_base = UrlUtil.stripProtocol(au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()));
    final String PATTERN_TEMPLATE =
        "\"^https?://" + strip_base + "article(?:/|s[.]php[?]id=)([0-9]+)(/download/pdf)?/?$\"";
    builder.setSpec(new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    builder.addAspect(ABSTRACT_PATTERN,
        Arrays.asList(ABSTRACT_0_REPLACEMENT, ABSTRACT_1_REPLACEMENT),
        ArticleFiles.ROLE_ABSTRACT, 
        ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(PDF_PATTERN,
        Arrays.asList(PDF_1_REPLACEMENT, PDF_0_REPLACEMENT),
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ABSTRACT);

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
