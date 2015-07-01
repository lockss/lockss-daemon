/*
 * $Id: ProjectMuseArticleIteratorFactory.java 40690 2015-03-18 18:12:56Z thib_gc $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.plos;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
//import org.lockss.util.Logger;

public class PLoSArticleIteratorFactory
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
