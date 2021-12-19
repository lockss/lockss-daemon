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

package org.lockss.plugin.projmuse;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ProjectMuse2017ArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(ProjectMuse2017ArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%sarticle/\", base_url";
  protected static final String PATTERN_TEMPLATE = "/article/[0-9]+(/pdf|/summary)?$";
  
  // various aspects of an article
  //   https://muse.jhu.edu/article/634157
  //   https://muse.jhu.edu/article/634157/pdf
  //   https://muse.jhu.edu/article/634157/summary
  
  // these kinds of urls are not used as part of the AI
  //   https://muse.jhu.edu/journal/457
  //   https://muse.jhu.edu/lockss?vid=12150
  //   https://muse.jhu.edu/issue/35186
  //   https://muse.jhu.edu/article/634152/image/tab01
  //   https://muse.jhu.edu/article/634152/image/tab01?format=thumb
  //   https://muse.jhu.edu/article/634152/figure/tab01
  //   https://muse.jhu.edu/article/634155/image/fig01
  //   https://muse.jhu.edu/article/634155/image/fig01?format=thumb
  
  protected static final Pattern HTML_PATTERN = Pattern.compile(
	      "([0-9]+)$", Pattern.CASE_INSENSITIVE);
  //4/12/19 - seeing some articles that only have /summary and /pdf
  protected static final Pattern SUMMARY_PATTERN = Pattern.compile(
	      "([0-9]+)/summary$", Pattern.CASE_INSENSITIVE);
  protected static final Pattern PDF_PATTERN = Pattern.compile(
	      "([0-9]+)/pdf$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String HTML_REPLACEMENT = "$1";
  protected static final String SUMM_REPLACEMENT = "$1/summary";
  protected static final String PDF_REPLACEMENT  = "$1/pdf";
  
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up html page to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means full is considered a FULL_TEXT_CU 
    // until this is deprecated
    // Note: Often the html page is also the fulltext html
    builder.addAspect(
        HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(SUMMARY_PATTERN,
        SUMM_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);
    
    builder.addAspect(PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // add metadata role from abstract or html
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, Arrays.asList(
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML));
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
