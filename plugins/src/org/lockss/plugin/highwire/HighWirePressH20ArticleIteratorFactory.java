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

package org.lockss.plugin.highwire;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class HighWirePressH20ArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  private static final Logger log =
    Logger.getLogger(HighWirePressH20ArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%scontent/%s/\", base_url, volume_name";
  
  protected static final String PATTERN_TEMPLATE =
    "\"^%scontent/%s/((?:[^/]+/)?[^/]+)(?:.*[.]body|[.]full(?:[.]pdf)?|[.](abstract|short|citation))$\", " +
    "base_url, volume_name";
  
  // various aspects of an article
  // http://bjo.bmj.com/content/96/1/1.full
  // http://bjo.bmj.com/content/96/1/1.full.pdf
  // http://bjo.bmj.com/content/96/1/1.full.pdf+html
  // http://bjo.bmj.com/content/96/1/1.extract
  // http://bjo.bmj.com/content/96/1/1.short
  // http://bjo.bmj.com/content/96/1/1.citation (usually no links)
  // and 
  // http://aapredbook.aappublications.org/content/1/SEC70/SEC72/SEC74.body
  
  protected static final Pattern HTML_PATTERN = Pattern.compile(
      "/([^/]+)[.](?:full|body)$", Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "/([^/]+)[.]full[.]pdf$", Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern ABSTRACT_PATTERN = Pattern.compile(
      "/([^/]+)[.](?:abstract|short|citation)$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String HTML_REPLACEMENT = "/$1.full";
  protected static final String PDF_REPLACEMENT = "/$1.full.pdf";
  protected static final String PDF_LANDING_REPLACEMENT = "/$1.full.pdf+html";
  protected static final String ABSTRACT_REPLACEMENT = "/$1.abstract";
  protected static final String CITATION_REPLACEMENT = "/$1.citation";
  protected static final String SHORT_REPLACEMENT = "/$1.short";
  protected static final String BODY_REPLACEMENT = "/$1.body";
  protected static final String FIGURES_REPLACEMENT = "/$1.figures-only";
  protected static final String SUPPL_REPLACEMENT = "/$1/suppl/DC1";
  
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up full or full.pdf to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means full is considered a FULL_TEXT_CU 
    // until this is deprecated
    builder.addAspect(HTML_PATTERN, Arrays.asList(
        BODY_REPLACEMENT, HTML_REPLACEMENT),
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up pdf landing page to be an aspect
    builder.addAspect(
        PDF_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    // set up abstract/extract to be an aspect
    builder.addAspect(ABSTRACT_PATTERN, Arrays.asList(
        ABSTRACT_REPLACEMENT, SHORT_REPLACEMENT, CITATION_REPLACEMENT),
        ArticleFiles.ROLE_ABSTRACT);
    
    // set up figures-only to be an aspect
    builder.addAspect(FIGURES_REPLACEMENT,
        ArticleFiles.ROLE_FIGURES_TABLES);
    
    // set up suppl to be an aspect
    builder.addAspect(SUPPL_REPLACEMENT,
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
    
    // add metadata role from abstract, html, or pdf (NOTE: pdf metadata gets DOI from filename)
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, Arrays.asList(
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_FULL_TEXT_HTML));
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML, 
        ArticleFiles.ROLE_FULL_TEXT_PDF, 
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_ABSTRACT);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
