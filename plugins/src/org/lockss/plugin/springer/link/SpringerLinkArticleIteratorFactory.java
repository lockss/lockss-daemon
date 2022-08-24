/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.springer.link;

import java.util.*;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class SpringerLinkArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  private static final String ROOT_TEMPLATE = "\"%s\", base_url";
  private static final String PATTERN_TEMPLATE = "\"^%s(book|article)/([^/]+)/([^/]+)$\", base_url";
  
  //http://link.springer.com/book/10.1007/978-4-431-54340-4
  private static final Pattern LANDING_PATTERN = Pattern.compile("/(article|book)/([^/]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
  private static final String LANDING_REPLACEMENT = "/$1/$2/$3";

  //http://link.springer.com/article/10.1007/978-4-431-54340-4_1/fulltext.html
  private static final Pattern HTML_PATTERN = Pattern.compile("/(article|book)/([^/]+)/([^/]+)/fulltext\\.html$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/$1/$2/$3/fulltext.html";
  
  //http://link.springer.com/download/epub/10.1007/978-4-431-54340-4.epub
  private static final Pattern EPUB_PATTERN = Pattern.compile("/(download/epub/)([^/]+)/([^/]+)\\.epub$", Pattern.CASE_INSENSITIVE);
  private static final String EPUB_REPLACEMENT = "/download/epub/$2/$3.epub";

  //http://link.springer.com/content/pdf/10.1007%2F978-4-431-54340-4.pdf
  private static final Pattern PDF_PATTERN = Pattern.compile("/(content/pdf/)([^%/]+)%2F([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  private static final String PDF_REPLACEMENT = "/content/pdf/$2%2f$3.pdf";

  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    builder.setSpec(target,
                    ROOT_TEMPLATE,
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    builder.addAspect(LANDING_PATTERN,
                      LANDING_REPLACEMENT,
                      ArticleFiles.ROLE_ABSTRACT);
    builder.addAspect(HTML_PATTERN,
                      HTML_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_HTML);
    builder.addAspect(PDF_PATTERN,
                      PDF_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_PDF);
    builder.addAspect(EPUB_PATTERN,
                      EPUB_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_EPUB);

    
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                                  ArticleFiles.ROLE_ABSTRACT,
                                  ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML,
    							 ArticleFiles.ROLE_FULL_TEXT_PDF,
                                 ArticleFiles.ROLE_FULL_TEXT_EPUB,
                                 ArticleFiles.ROLE_ABSTRACT);
    
    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
