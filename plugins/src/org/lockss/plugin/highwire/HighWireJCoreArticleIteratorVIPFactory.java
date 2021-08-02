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

package org.lockss.plugin.highwire;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class HighWireJCoreArticleIteratorVIPFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(HighWireJCoreArticleIteratorVIPFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%scontent/\", base_url";
  
  // <base_url>/content/<v>/[<i>/]<pg> required vol, issue, page, then EOL
  protected static final String PATTERN_TEMPLATE =
    "\"^%scontent/%s/([^/]+/)?(?!.+?[.](toc|full|pdf|long|supplemental|data|figures-only|abstract|extract))([^/?&]+)$\", base_url, volume_name";
  
  // various aspects of a HighWire article
  // http://ajpcell.physiology.org/content/302/1/C1
  // http://ajpcell.physiology.org/content/302/1/C1.figures-only
  // http://ajpcell.physiology.org/content/302/1/C1.full.pdf+html
  // http://ajpcell.physiology.org/content/302/1/C1.full.pdf
  // http://ajpcell.physiology.org/content/302/1/C1.full
  // http://ajpcell.physiology.org/content/302/1/C1.abstract
  // http://bjo.bmj.com/content/96/1/1.extract
  // http://apt.rcpsych.org/content/21/2/74.1
  // http://msb.embopress.org/content/1/1/2005.0001
  // http://essays.biochemistry.org/content/59/1
  // http://essays.biochemistry.org/content/59/1.full.pdf
  // http://rimg.geoscienceworld.org/content/81/1/iii.2
  // http://rimg.geoscienceworld.org/content/81/1/iii.2.full.pdf
  
  // these kinds of urls are not used as part of the AI
  // http://ajpcell.physiology.org/content/302/1/C1.full-text.pdf+html (normalized)
  // http://ajpcell.physiology.org/content/302/1/C1.full-text.pdf (normalized)
  // http://ajpcell.physiology.org/content/302/1/C1.article-info
  // http://bjo.bmj.com/content/96/1/1.short
  // http://bjo.bmj.com/content/96/1/1.citation (usually no links)
  // http://bjo.bmj.com/content/96/1.toc
  
  protected static final Pattern LANDING_PATTERN = Pattern.compile(
      "/([^/?&]+)$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String LANDING_REPLACEMENT = "/$1";
  protected static final String HTML_REPLACEMENT = "/$1.full";
  protected static final String PDF_REPLACEMENT = "/$1.full.pdf";
  protected static final String PDF_LANDING_REPLACEMENT = "/$1.full.pdf+html";
  protected static final String ABSTRACT_REPLACEMENT = "/$1.abstract";
  protected static final String EXTRACT_REPLACEMENT = "/$1.extract";
  protected static final String FIGURES_REPLACEMENT = "/$1.figures-only";
  
  
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au) {
      
      @Override
      protected BuildableSubTreeArticleIterator instantiateBuildableIterator() {
        return new BuildableSubTreeArticleIterator(au, spec) {
          
          @Override
          protected ArticleFiles createArticleFiles(CachedUrl cu) {
            ArchivalUnit au = cu.getArchivalUnit();
            CachedUrl toc = au.makeCachedUrl(cu.getUrl() + ".toc");
            if ((toc != null) && toc.hasContent()) {
              return null;
            }
            return super.createArticleFiles(cu);
          }
        };
      }
    };
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up landing page to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means full is considered a FULL_TEXT_CU 
    // until this is deprecated
    // Note: Often the landing page is also the fulltext html
    builder.addAspect(
        LANDING_PATTERN, LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE);
    
    builder.addAspect(
        HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up pdf landing page to be an aspect
    builder.addAspect(
        PDF_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    // set up abstract/extract to be an aspect
    builder.addAspect(Arrays.asList(
        ABSTRACT_REPLACEMENT, EXTRACT_REPLACEMENT),
        ArticleFiles.ROLE_ABSTRACT);
    
    // set up figures-only to be an aspect
    builder.addAspect(FIGURES_REPLACEMENT,
        ArticleFiles.ROLE_FIGURES);
    
    // add metadata role from abstract, html or pdf landing page
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, Arrays.asList(
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE));
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML, 
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
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
