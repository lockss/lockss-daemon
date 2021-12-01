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

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
//import org.lockss.util.Logger;

public class ProjectMuseArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  // private static final Logger log = Logger.getLogger(ProjectMuseArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%sjournals/%s/\", base_url, journal_dir";
  protected static final String PATTERN_TEMPLATE = "\"^%sjournals/%s/v%03d/[^_/]+[.]html\", base_url, journal_dir, volume";
  
  // various aspects of an article
  // http://muse.jhu.edu/journals/advertising_and_society_review/v008/8.1gao.html
  // http://muse.jhu.edu/journals/advertising_and_society_review/v008/8.1intro.pdf
  // https://muse.jhu.edu/journals/ecotone/summary/v004/4.1-2.branch.html               
  
  // these kinds of urls are not used as part of the AI
  // http://muse.jhu.edu/journals/advertising_and_society_review/toc/asr8.1.html
  // http://muse.jhu.edu/journals/advertising_and_society_review/v008/8.1gao_table1.html
  // http://muse.jhu.edu/journals/advertising_and_society_review/v008/images/8.1gao_01.jpg
  // http://muse.jhu.edu/journals/advertising_and_society_review/v008/videos/8.1gao_nike.mov
  
  protected static final Pattern HTML_PATTERN = Pattern.compile(
      "/(v[0-9]+)/([^_/]+)[.]html$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String HTML_REPLACEMENT = "/$1/$2.html";
  protected static final String SUMM_REPLACEMENT = "/summary/$1/$2.html";
  protected static final String PDF_REPLACEMENT  = "/$1/$2.pdf";
  
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
        ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        SUMM_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);
    
    builder.addAspect(
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
