/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americanmathematicalsociety;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class AmericanMathematicalSocietyArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {
  
  protected static Logger log = 
      Logger.getLogger(AmericanMathematicalSocietyArticleIteratorFactory.class);
  
  // params from tdb file corresponding to AU
  protected static final String ROOT_TEMPLATE =
      "\"%sjournals/%s/\", base_url, journal_id";
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%sjournals/%s/%d-[0-9-]+/\", base_url, journal_id, year";
  
  /*
    various files
      html - https://www.ams.org/journals/bull/2023-60-04/S0273-0979-2023-01805-3/viewer
      pdf - https://www.ams.org/journals/bull/2023-60-04/S0273-0979-2023-01805-3/S0273-0979-2023-01805-3.pdf
      abstract - https://www.ams.org/journals/bull/2023-60-04/S0273-0979-2023-01805-3/?active=current
   */
  
  // Identify groups in the pattern
  protected static final Pattern HTML_PATTERN = Pattern.compile(
      "/journals/([^/]+/[0-9-]+)/([^/]+)/viewer$",
      Pattern.CASE_INSENSITIVE);
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "/journals/([^/]+/[0-9-]+)/([^/]+)/\\2[.]pdf$",
      Pattern.CASE_INSENSITIVE);
  protected static final Pattern ABSTRACT_PATTERN = Pattern.compile(
      "/journals/([^/]+/[0-9-]+)/([^/]+)/\\?active=current",
      Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String HTML_REPLACEMENT = "/journals/$1/$2/viewer";
  protected static final String PDF_REPLACEMENT = "/journals/$1/$2/$2.pdf";
  protected static final String ABSTRACT_REPLACEMENT = "/journals/$1/$2/?active=current";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job, PDF then html

    // set up html to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);

    // set up PDF to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // set up abstract to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        ABSTRACT_PATTERN, ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);

    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_FULL_TEXT_HTML);
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML, ArticleFiles.ROLE_FULL_TEXT_PDF, ArticleFiles.ROLE_ABSTRACT);

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
