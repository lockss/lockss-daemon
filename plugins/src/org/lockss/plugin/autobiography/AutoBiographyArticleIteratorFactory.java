/*
 * $Id$
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

package org.lockss.plugin.autobiography;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class AutoBiographyArticleIteratorFactory 
implements ArticleIteratorFactory, 
ArticleMetadataExtractorFactory {
  
  protected static Logger log = 
      Logger.getLogger(AutoBiographyArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%s\",base_url";
  
  protected static final String PATTERN_TEMPLATE = 
      "\"^%s%s_[\\d]{4}_%s_[\\d]+/(?!index[.]htm)[^/]*[.](?:htm|pdf)$\"," +
      "base_url, journal_id, volume_name";
  /*
    http://autobiography.stanford.clockss.org/aub_2004_12_3/10.1177_09675507040120030402.htm
    http://autobiography.stanford.clockss.org/aub_2004_12_3/10.1177_09675507040120030402.pdf
    http://autobiography.stanford.clockss.org/aub_2004_12_3/10.1177_09675507040120030402.xml
    http://autobiography.stanford.clockss.org/aub_2004_12_3/10.1177_09675507040120030402.txt
    NOTE: 4 digit year can change within AU for vol 14
    http://autobiography.stanford.clockss.org/aub_2006_14_3/10.1191_0967550706ab049XX.pdf
    http://autobiography.stanford.clockss.org/aub_2007_14_4/10.1177_0967550706072242.pdf
   */
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    final Pattern ABSTRACT_PATTERN = Pattern.compile(
        "(/[^_]+_[\\d]{4}_[\\d]+_[\\d]+/[^/]+)[.]htm$", Pattern.CASE_INSENSITIVE);
    final Pattern PDF_PATTERN = Pattern.compile(
        "(/[^_]+_[\\d]{4}_[\\d]+_[\\d]+/[^/]+)[.]pdf$", Pattern.CASE_INSENSITIVE);
    
    // how to change from one form (aspect) of article to another
    final String PDF_REPLACEMENT = "$1.pdf";
    final String ABSTRACT_REPLACEMENT = "$1.htm";
    final String METADATA_REPLACEMENT= "$1.xml";
    final String OCR_REPLACEMENT = "$1.txt";
    
    builder.setSpec(target,
        ROOT_TEMPLATE,
        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up pdf to be an aspect that will trigger an ArticleFiles
    builder.addAspect(PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up HTML to be an aspect that will trigger an ArticleFiles
    builder.addAspect(ABSTRACT_PATTERN,
        ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);
    
    // set up xml to be an aspect
    builder.addAspect( 
        METADATA_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    // set up txt to be an aspect
    builder.addAspect(
        OCR_REPLACEMENT,
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
    
    
    // The order in which we want to define full_text_cu.  
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}

