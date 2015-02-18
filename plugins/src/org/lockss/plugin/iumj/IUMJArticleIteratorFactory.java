/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.iumj;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class IUMJArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  protected static Logger log =
      Logger.getLogger(IUMJArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
      "\"%sIUMJ/FTDLOAD/%d/\", base_url, year";
  
  protected static final String PATTERN_TEMPLATE =
      "\"%sIUMJ/FTDLOAD/(%d)/(%s)/([^/]+)/pdf\", base_url, year, volume_name";
  
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // various aspects of an article
    // http://www.iumj.indiana.edu/IUMJ/FTDLOAD/1984/33/33001/pdf
    // http://www.iumj.indiana.edu/IUMJ/ABS/1984/33001
    // http://www.iumj.indiana.edu/IUMJ/FULLTEXT/1984/33/33001
    // http://www.iumj.indiana.edu/META/1984/33001.xml
    // http://www.iumj.indiana.edu/oai/1984/33/33008/33001.html
    // http://www.iumj.indiana.edu/IUMJ/FTDLOAD/1984/33/33001/djvu
    // http://www.iumj.indiana.edu/oai/1984/33/33001/33001_abs.pdf
    // 
    
    final Pattern PDF_PATTERN = Pattern.compile(
        "IUMJ/FTDLOAD/([^/]+)/([^/]+)/([^/]+)/pdf", 
        Pattern.CASE_INSENSITIVE);
    
    // how to change from one form (aspect) of article to another
    final String PDF_REPLACEMENT = "IUMJ/FTDLOAD/$1/$2/$3/pdf";
    final String ABSTRACT_REPLACEMENT = "IUMJ/ABS/$1/$3";
    final String FT_LANDING_REPLACEMENT = "IUMJ/FULLTEXT/$1/$2/$3";
    final String METADATA_REPLACEMENT = "META/$1/$3.xml";
    final String OIA_CITE_REPLACEMENT = "oai/$1/$2/$3/$3.html";
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up pdf to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up abstract to be an aspect
    builder.addAspect(
        ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);
    
    // set up abstract to be an aspect
    builder.addAspect(
        FT_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    // set up metadata to be an aspect
    builder.addAspect(
        METADATA_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    // set up oia metadata to be an aspect
    builder.addAspect(
        OIA_CITE_REPLACEMENT,
        ArticleFiles.ROLE_CITATION);
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    // In this case, there are two jobs, one for counting articles (abstract is 
    // good) and the other for metadata (PDF is correct)
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF, 
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
