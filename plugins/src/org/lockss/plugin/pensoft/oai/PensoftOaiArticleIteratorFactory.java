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
