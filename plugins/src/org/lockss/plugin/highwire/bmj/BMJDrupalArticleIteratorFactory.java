/*
 * $Id$
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

package org.lockss.plugin.highwire.bmj;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class BMJDrupalArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(BMJDrupalArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%scontent/\", base_url";
  
  // Cannot use volume_name as the BMJ mixes articles (ie. /346/bmj.f4217 in vol 347, issue 7915)
  protected static final String PATTERN_TEMPLATE =
    "\"^%scontent/([^/]{1,4}/bmj[.](?:[^./?&]+)|%s/[^/]+/(?![^/]+[.]full)[^/?]+)$\", base_url, volume_name";
  
  // various aspects of an article
  // http://www.bmj.com/content/345/bmj.e7558
  // http://www.bmj.com/content/345/bmj.e7558.full.pdf
  // http://www.bmj.com/content/345/bmj.f7558.full.pdf+html
  
  // http://www.bmj.com/content/325/7373/1156
  // http://www.bmj.com/content/325/7373/1156.full.pdf+html
  // http://www.bmj.com/content/325/7373/1156.full-text.print (not preserved)
  
  
  protected static final Pattern LANDING_PATTERN = Pattern.compile(
      "/content/([^/]{1,4})/(bmj[.][^./?&]+|[0-9]{4,}/[0-9.]+)$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String LANDING_REPLACEMENT = "/content/$1/$2";
  protected static final String PDF_REPLACEMENT = "/content/$1/$2.full.pdf";
  protected static final String PDF_LANDING_REPLACEMENT = "/content/$1/$2.full.pdf+html";
  
  protected static final Pattern VIP_ARTICLE_PATTERN = Pattern.compile(
      "/content/([^/]+/[^/]+)/(?!.*[.]full)([^/?]+)$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String VIP_ARTICLE_REPLACEMENT = "/content/$1/$2";
  
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    // set up landing page to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means full is considered a FULL_TEXT_CU 
    // until this is deprecated
    builder.addAspect(
        LANDING_PATTERN, LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE);
    
    builder.addAspect(
        VIP_ARTICLE_PATTERN, VIP_ARTICLE_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        PDF_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
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
