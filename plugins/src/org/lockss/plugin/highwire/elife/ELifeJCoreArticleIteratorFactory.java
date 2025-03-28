/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.highwire.elife;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ELifeJCoreArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(ELifeJCoreArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%scontent/\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
    "\"^%scontent/(elife/)?[0-9]+/e(?:[^./?&]+)$\", base_url";
  // various aspects of an article
  // http://elifesciences.org/content/1/e00311
  // http://elifesciences.org/content/elife/1/e00311.full.pdf
  // http://elifesciences.org/content/1/e00311/article-data
  
  // these kinds of urls are not used as part of the AI
  // http://elifesciences.org/content/1/e00311.source.xml
  // http://elifesciences.org/highwire/citation/422/ris  
  // http://elifesciences.org/content/1/e00311/article-info
  
  protected static final Pattern LANDING_PATTERN = Pattern.compile(
      "/([0-9]+)/([^./?&]+)$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String LANDING_REPLACEMENT = "/$1/$2";
  protected static final String PDF_REPLACEMENT = "/elife/$1/$2.full.pdf";
  protected static final String PDF_REPLACEMENT2 = "/$1/$2.full.pdf";
  protected static final String FIGURES_REPLACEMENT = "/$1/$2/article-data";
  
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up landing page to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means full is considered a FULL_TEXT_CU 
    // until this is deprecated
    builder.addAspect(
        LANDING_PATTERN, LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    builder.addAspect(
        Arrays.asList(PDF_REPLACEMENT, PDF_REPLACEMENT2),
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up figures-only to be an aspect
    builder.addAspect(FIGURES_REPLACEMENT,
        ArticleFiles.ROLE_FIGURES_TABLES);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
