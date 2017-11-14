/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.smallaxe;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class SmallAxeArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(SmallAxeArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
      "\"%s\", base_url";
  
  //toc is: http://smallaxe.net/sxarchipelagos/issue02.html
  // so the pattern wouldn't pick it up
  protected static final String PATTERN_TEMPLATE =
      "/(assets/)?issue[^/]+/([^/]+)[.](pdf|html)$";

  // various aspects of an article
  // http://smallaxe.net/sxarchipelagos/issue02/intervening-in-french.html
  // http://smallaxe.net/sxarchipelagos/assets/issue02/intervening-in-french.pdf 

  protected static final Pattern HTML_PATTERN = Pattern.compile(
      "/issue([^/]+)/([^/]+)[.]html$",
      Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "/assets/issue([^/]+)/([^/]+)[.]pdf$",
      Pattern.CASE_INSENSITIVE);
  
  
  // how to change from one form (aspect) of article to another
  protected static final String HTML_REPLACEMENT = "/issue$1/$2.html";
  protected static final String PDF_REPLACEMENT = "/assets/issue$1/$2.pdf";
  
  public Iterator<ArticleFiles> createArticleIterator(
      ArchivalUnit au, MetadataTarget target)
          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    

    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(
        HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ABSTRACT,
    ArticleFiles.ROLE_ARTICLE_METADATA);
    
    
    return builder.getSubTreeArticleIterator();
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
