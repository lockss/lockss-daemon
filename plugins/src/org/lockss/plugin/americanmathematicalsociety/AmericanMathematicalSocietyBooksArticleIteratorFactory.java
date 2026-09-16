/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.americanmathematicalsociety;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class AmericanMathematicalSocietyBooksArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {
  
  protected static Logger log = 
      Logger.getLogger(AmericanMathematicalSocietyBooksArticleIteratorFactory.class);
  
  // params from tdb file corresponding to AU
  protected static final String ROOT_TEMPLATE =
      "\"%sbooks/%s/\", to_https(base_url), collection_id";
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%sbooks/(%s)/([0-9.]+)(/\\1\\2\\.pdf)?$\", to_https(base_url), collection_id";
  
  /*
    various files
    
   */
  
  // Identify groups in the pattern
  protected static final Pattern HTML_PATTERN = Pattern.compile(
      "/books/([^/]+)/([0-9.]+)$",
      Pattern.CASE_INSENSITIVE);
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "/books/([^/]+)/([0-9.]+)/\\1\\2\\.pdf$",
      Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String HTML_REPLACEMENT = "/books/$1/$2";
  protected static final String PDF_REPLACEMENT = "/books/$1/$2/$1$2.pdf";
  
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job, PDF then html
    
    // set up PDF to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up html to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    // set up html to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
