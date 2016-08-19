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

package org.lockss.plugin.acsess;

import java.util.Iterator;
import java.util.regex.Pattern;
import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;

/*
 Article files:
 */
public class ACSESSBooksArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final String ROOT_TEMPLATE = "\"%spublications/books/\", base_url";
  
  // pattern template must include all primary aspects
  // abstracts, preview pdf abstracts, html full text, and pdf full text  
  private static final String PATTERN_TEMPLATE = 
      "\"^%spublications/books/(abstracts|articles|pdfs)/(%s)/(%s)([^?]*)(/preview)?$\", base_url, series_id, book_id";
  
  // primary aspects need their own patterns
  private Pattern HTML_PATTERN = Pattern.compile(      
      "/publications/books/articles/([^/]+)/([^/?]+)((?:/)?[^/?]*)$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/publications/books/articles/$1/$2$3";
  private Pattern ABSTRACT_PATTERN = Pattern.compile(      
      "/publications/books/abstracts/([^/]+)/([^/?]+)((?:/)?[^/?]*)$", Pattern.CASE_INSENSITIVE);
  private static final String ABSTRACT_REPLACEMENT = "/publications/books/abstracts/$1/$2$3";    
  private Pattern PREVIEW_PDF_ABSTRACT_PATTERN = Pattern.compile(      
      "/publications/books/abstracts/([^/]+)/([^/?]+)((?:/)?[^/?]*)/preview$", Pattern.CASE_INSENSITIVE);
  private static final String PREVIEW_HTML_LANDING_REPLACEMENT = "/publications/books/abstracts/$1/$2$3/preview";
  private Pattern PDF_PATTERN = Pattern.compile(      
      "/publications/books/pdfs/([^/]+)/([^/?]+)((?:/)?[^/?]*)$", Pattern.CASE_INSENSITIVE);
  private static final String PDF_REPLACEMENT = "/publications/books/pdfs/$1/$2$3";
 
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, 
      MetadataTarget target) throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = 
        new SubTreeArticleIteratorBuilder(au);  
    
    builder.setSpec(target,
                    ROOT_TEMPLATE, 
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    builder.addAspect(HTML_PATTERN, 
                      HTML_REPLACEMENT, 
                      ArticleFiles.ROLE_FULL_TEXT_HTML);
    builder.addAspect(PDF_PATTERN,
                      PDF_REPLACEMENT, 
                      ArticleFiles.ROLE_FULL_TEXT_PDF);     
    builder.addAspect(ABSTRACT_PATTERN,
                      ABSTRACT_REPLACEMENT,
                      ArticleFiles.ROLE_ABSTRACT);
    builder.addAspect(PREVIEW_PDF_ABSTRACT_PATTERN,
                      PREVIEW_HTML_LANDING_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);  
    
    
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML,
                                 ArticleFiles.ROLE_FULL_TEXT_PDF);
    
   
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                                  ArticleFiles.ROLE_ABSTRACT,
                                  ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    return builder.getSubTreeArticleIterator();
  }  
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
