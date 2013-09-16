/*
 * $Id: HighWirePressH20ArticleIteratorFactory.java,v 1.12 2013-09-16 22:10:00 etenbrink Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

  protected static Logger log =
    Logger.getLogger("HighWirePressH20ArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE =
    "\"%scontent/%s/\", base_url, volume_name";
  
  protected static final String PATTERN_TEMPLATE =
    "\"^%scontent/%s/((?:[^/]+/)?[^/]+)(?:.*[.]body|[.]full(?:[.]pdf)?)$\", " +
    "base_url, volume_name";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // various aspects of an article
    // http://bjo.bmj.com/content/96/1/1.full
    // http://bjo.bmj.com/content/96/1/1.full.pdf
    // http://bjo.bmj.com/content/96/1/1.full.pdf+html
    // http://bjo.bmj.com/content/96/1/1.extract
    // http://bjo.bmj.com/content/96/1/1.short
    // http://bjo.bmj.com/content/96/1/1.citation (usually no links)
    // and 
    // http://aapredbook.aappublications.org/content/1/SEC70/SEC72/SEC74.body
    
    final Pattern BODY_PATTERN = Pattern.compile(
        "/([^/]+)[.]body$", Pattern.CASE_INSENSITIVE);
    
    final Pattern HTML_PATTERN = Pattern.compile(
        "/([^/]+)[.]full$", Pattern.CASE_INSENSITIVE);
    
    final Pattern PDF_PATTERN = Pattern.compile(
        "/([^/]+)[.]full[.]pdf$", Pattern.CASE_INSENSITIVE);
    
    // how to change from one form (aspect) of article to another
    final String HTML_REPLACEMENT = "/$1.full";
    final String PDF_REPLACEMENT = "/$1.full.pdf";
    final String PDF_LANDING_REPLACEMENT = "/$1.full.pdf+html";
    final String ABSTRACT_REPLACEMENT = "/$1.abstract";
    final String EXTRACT_REPLACEMENT = "/$1.extract";
    final String BODY_REPLACEMENT = "/$1.body";
    final String FIGURES_REPLACEMENT = "/$1.figures-only";
    final String SUPPL_REPLACEMENT = "/$1/suppl/DC1";
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    // set up full or full.pdf to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means full is considered a FULL_TEXT_CU 
    // until this is deprecated
    builder.addAspect(
        BODY_PATTERN, BODY_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
        HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // set up pdf landing page to be an aspect
    builder.addAspect(
        PDF_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    // set up abstract/extract to be an aspect
    builder.addAspect(Arrays.asList(
        ABSTRACT_REPLACEMENT, EXTRACT_REPLACEMENT),
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    // set up figures-only to be an aspect
    builder.addAspect(FIGURES_REPLACEMENT,
        ArticleFiles.ROLE_FIGURES_TABLES);

    // set up suppl to be an aspect
    builder.addAspect(SUPPL_REPLACEMENT,
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);

    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML, 
        ArticleFiles.ROLE_FULL_TEXT_PDF, 
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_ABSTRACT);
    
    return builder.getSubTreeArticleIterator();
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
