/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
