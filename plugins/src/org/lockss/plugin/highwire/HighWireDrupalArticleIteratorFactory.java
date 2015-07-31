/*
 * $Id$
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

package org.lockss.plugin.highwire;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class HighWireDrupalArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(HighWireDrupalArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%scontent/%s/\", base_url, volume_name";
  
  // <base_url>/content/<v>/<i>/<pg>
  protected static final String PATTERN_TEMPLATE =
    "\"^%scontent/%s/(?:[^/]+/)(?:[^./?&]+)$\", base_url, volume_name";
  
  // various aspects of a HighWire article
  // http://ajpcell.physiology.org/content/302/1/C1
  // http://ajpcell.physiology.org/content/302/1/C1.figures-only
  // http://ajpcell.physiology.org/content/302/1/C1.full.pdf+html
  // http://ajpcell.physiology.org/content/302/1/C1.full.pdf
  // http://ajpcell.physiology.org/content/302/1/C1.full
  // http://ajpcell.physiology.org/content/302/1/C1.abstract
  // http://bjo.bmj.com/content/96/1/1.extract
  
  // these kinds of urls are not used as part of the AI
  // http://ajpcell.physiology.org/content/302/1/C1.full-text.pdf+html (normalized)
  // http://ajpcell.physiology.org/content/302/1/C1.full-text.pdf (normalized)
  // http://ajpcell.physiology.org/content/302/1/C1.article-info
  // http://bjo.bmj.com/content/96/1/1.short
  // http://bjo.bmj.com/content/96/1/1.citation (usually no links)
  
  protected static final Pattern LANDING_PATTERN = Pattern.compile(
      "/([^./?&]+)$", Pattern.CASE_INSENSITIVE);
  
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
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
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
        ArticleFiles.ROLE_FIGURES_TABLES);
    
    // add metadata role from abstract, html, or pdf (NOTE: pdf metadata gets DOI from filename)
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
