/* $Id$
 
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

package org.lockss.plugin.palgrave;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * CLOCKSS-only
 * preserving the book-length pdf/epub, ris/metadata, cover images and landing page
 * top-level (landing page) : http://www.palgraveconnect.com/pc/doifinder/10.1057/9781137283351
 * pdf (download pdf):        http://www.palgraveconnect.com/pc/econfin2012/browse/inside/download/9781137283351.pdf
 * epub (download epub):      http://www.palgraveconnect.com/pc/busman2013/browse/inside/epub/9780230389113.epub
 * citation export:           http://www.palgraveconnect.com/pc/browse/citationExport?doi=10.1057/9781137024497&WT.cg_n=eBooks&WT.cg_s=Citation%20Export
 * Metadata found in citation export ris file. 
 * 1/14/15 
 *  citation:                 http://www.palgraveconnect.com/pc/browse/citationExport?doi=10.1057/9781137304094
 *  pdf (download pdf):       http://www.palgraveconnect.com/pc/doifinder/download/10.1057/9781137023803
 * epub (download epub):      http://www.palgraveconnect.com/pc/doifinder/download/10.1057/9781137023803.epub
 */

public class PalgraveBookArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static Logger log = Logger.getLogger(PalgraveBookArticleIteratorFactory.class);
  
  // ROOT_TEMPLATE AND PATTERN_TEMPLATE required by SubTreeArticleIterator.
  // SubTreeArticleIterator returns only the URLs under ROOT_TEMPLATE, that
  // match PATTERN_TEMPLATE.
  // root: http://www.palgraveconnect.com
  protected static final String ROOT_TEMPLATE = "\"%spc/\", base_url";
/* old patterns
  protected static final String PATTERN_TEMPLATE =    
   "\"%spc/([^/]+)/browse/inside(/download|/epub)/([0-9]+)\\.(pdf|epub)$\", base_url";

  protected Pattern PDF_PATTERN = 
    Pattern.compile("/pc/([^/]+)/browse/inside/download/([0-9]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  protected Pattern EPUB_PATTERN = 
    Pattern.compile("/pc/([^/]+)/browse/inside/epub/([0-9]+)\\.epub$", Pattern.CASE_INSENSITIVE);
    
  protected static String PDF_REPLACEMENT = "/pc/$1/browse/inside/download/$2.pdf";
  protected static String EPUB_REPLACEMENT = "/pc/$1/browse/inside/epub/$2.epub";
  protected static String CITATION_RIS_REPLACEMENT = 
    "/pc/browse/citationExport?doi=10.1057/$2&WT.cg_n=eBooks&WT.cg_s=Citation%20Export";
  */
  // new patterns 1/14/15
  /*
   * top-level (landing page) : http://www.palgraveconnect.com/pc/doifinder/10.1057/9781137283351
   * 1/14/15 
   *  citation:                 http://www.palgraveconnect.com/pc/browse/citationExport?doi=10.1057/9781137304094
   *  pdf (download pdf):       http://www.palgraveconnect.com/pc/doifinder/download/10.1057/9781137023803
   *  epub (download epub):     http://www.palgraveconnect.com/pc/doifinder/download/10.1057/9781137023803.epub
   */
  protected static final String PATTERN_TEMPLATE =    
    "\"%spc/doifinder/download/10.1057/([0-9]+)(\\.epub)?$\", base_url";

   protected Pattern PDF_PATTERN = 
     Pattern.compile("/pc/doifinder/download/10.1057/([0-9]+)$", Pattern.CASE_INSENSITIVE);
   protected Pattern EPUB_PATTERN = 
     Pattern.compile("/pc/doifinder/download/10.1057/([0-9]+)\\.epub$", Pattern.CASE_INSENSITIVE);
     
   protected static String PDF_REPLACEMENT = "/pc/doifinder/download/10.1057/$1";
   protected static String EPUB_REPLACEMENT = "/pc/doifinder/download/10.1057/$1.epub";
   protected static String CITATION_RIS_REPLACEMENT = "/pc/browse/citationExport?doi=10.1057/$1";
   
  // Create PalgraveBookArticleIterator with the new object of 
  // SubTreeArticleIterator ROOT_TEMPLATE and PATTERN_TEMPLATE already set.
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
                                                          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
                    ROOT_TEMPLATE,
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
                            
    // primary roles have enough info to trigger an article.
    // the order of builder.addAspect is important.
    // the order in which we want to define full_text_cu.  
    // first one that exists will get the job
    // in this case the pdf is the first to be consider a full-text cu
    
    builder.addAspect(PDF_PATTERN,
                      PDF_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(EPUB_PATTERN,
                      EPUB_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_EPUB);
    
    // secondary roles don't have enough info to trigger an article
    
    builder.addAspect(CITATION_RIS_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION_RIS,
                      ArticleFiles.ROLE_ARTICLE_METADATA);
    
    //builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,ArticleFiles.ROLE_FULL_TEXT_PDF,ArticleFiles.ROLE_FULL_TEXT_EPUB);

    return builder.getSubTreeArticleIterator();
 
  }
  
  // Create Article Metadata Extractor
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}