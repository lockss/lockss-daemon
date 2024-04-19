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

package org.lockss.plugin.europeanmathematicalsociety.api;

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

public class EuropeanMathematicalSocietyJournalsArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  //https://ems.press/journals/mag/articles/15312
  //https://ems.press/journals/mag/articles/15313
  //https://ems.press/journals/mag/articles/15314

  //https://ems.press/content/serial-article-files/10020
  //https://ems.press/content/serial-article-files/10021
  //https://ems.press/content/serial-article-files/10022

  
  private static final Logger log = Logger.getLogger(EuropeanMathematicalSocietyJournalsArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%s\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
    "\"%s(journals|content)/\", base_url, journal_id";

  // various aspects of an article
  protected static final Pattern ARTICLE_LANDING_PAGE_PATTERN = Pattern.compile(
      "/(journals/[^/]+/articles/\\d+)$", Pattern.CASE_INSENSITIVE);

  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "/(content/serial-article-files/\\d+)$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String ARTICLE_LANDING_PAGE_REPLACEMENT = "$1";
  protected static final String PDF_REPLACEMENT = "$1";
  
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    /*
    builder.addAspect(
            PDF_PATTERN, PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

     */

    // set up abstract page to be an aspect
    builder.addAspect(
            ARTICLE_LANDING_PAGE_PATTERN,
            ARTICLE_LANDING_PAGE_REPLACEMENT,
            ArticleFiles.ROLE_ABSTRACT,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
