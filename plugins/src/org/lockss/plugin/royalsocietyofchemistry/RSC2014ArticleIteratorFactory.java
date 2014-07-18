/*
 * $Id: RSC2014ArticleIteratorFactory.java,v 1.2.2.2 2014-07-18 15:56:31 wkwilson Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.royalsocietyofchemistry;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class RSC2014ArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(RSC2014ArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%sen/content/\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%sen/content/article(?:landing|html|pdf)/%d/%s/\", " +
      "base_url, year, journal_code";
  
  /*
   * various aspects of an article
   * http://pubs.rsc.org/en/content/articlelanding/2009/gc/b906831g
   * http://pubs.rsc.org/en/content/articlehtml/2009/gc/b906831g
   * http://pubs.rsc.org/en/content/articlepdf/2009/gc/b906831g
   */
  
  // Identify groups in the pattern "/article(landing|html|pdf)/(<year>/<journalcode>)/(<doi>).*
  static final Pattern ABSTRACT_PATTERN = Pattern.compile(
      "/articlelanding/([0-9]{4}/[^/]+)/([^/?&]+)$",
      Pattern.CASE_INSENSITIVE);
  
  static final Pattern HTML_PATTERN = Pattern.compile(
      "/articlehtml/([0-9]{4}/[^/]+)/([^/?&]+)$",
      Pattern.CASE_INSENSITIVE);
  
  static final Pattern PDF_PATTERN = Pattern.compile(
      "/articlepdf/([0-9]{4}/[^/]+)/([^/?&]+)$",
      Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  static final String ABSTRACT_REPLACEMENT = "/articlelanding/$1/$2";
  static final String HTML_REPLACEMENT = "/articlehtml/$1/$2";
  static final String PDF_REPLACEMENT = "/articlepdf/$1/$2";
  
  public Iterator<ArticleFiles> createArticleIterator(
      ArchivalUnit au, MetadataTarget target) throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up pdf or html fulltext to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(
        HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    // set up abstract to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means it is considered a FULL_TEXT_CU
    // until this fulltext concept is deprecated
    // NOTE: pdf or html full text will take precedence over abstract
    builder.addAspect(
        ABSTRACT_PATTERN, ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);
    
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    return builder.getSubTreeArticleIterator();
  }

  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
