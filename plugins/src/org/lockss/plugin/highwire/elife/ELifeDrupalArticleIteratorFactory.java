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

package org.lockss.plugin.highwire.elife;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ELifeDrupalArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(ELifeDrupalArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%scontent/\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
    "\"^%scontent/(elife/)?[0-9]+/e(?:[^./?&]+)$\", base_url";
  // various aspects of an article
  // http://elifesciences.org/content/1/e00311
  // http://elifesciences.org/content/elife/1/e00311.full.pdf
  // http://elifesciences.org/content/1/e00311/article-data
  
  // these kinds of urls are not used as part of the AI
  // http://elifesciences.org/content/1/e00311.source.xml
  // http://elifesciences.org/highwire/citation/422/ris  
  // http://elifesciences.org/content/1/e00311/article-info
  
  protected static final Pattern LANDING_PATTERN = Pattern.compile(
      "/([0-9]+)/([^./?&]+)$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String LANDING_REPLACEMENT = "/$1/$2";
  protected static final String PDF_REPLACEMENT = "/elife/$1/$2.full.pdf";
  protected static final String PDF_REPLACEMENT2 = "/$1/$2.full.pdf";
  protected static final String FIGURES_REPLACEMENT = "/$1/$2/article-data";
  
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up landing page to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means full is considered a FULL_TEXT_CU 
    // until this is deprecated
    builder.addAspect(
        LANDING_PATTERN, LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    builder.addAspect(
        Arrays.asList(PDF_REPLACEMENT, PDF_REPLACEMENT2),
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up figures-only to be an aspect
    builder.addAspect(FIGURES_REPLACEMENT,
        ArticleFiles.ROLE_FIGURES_TABLES);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
