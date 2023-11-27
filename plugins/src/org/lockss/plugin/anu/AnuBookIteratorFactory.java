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

package org.lockss.plugin.anu;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class AnuBookIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(AnuBookIteratorFactory.class);
  
  // params from tdb file corresponding to AU
  protected static final String ROOT_TEMPLATE = "\"%spublications/\", base_url";
  protected static final String PATTERN_TEMPLATE = "\"^%spublications/%s$\", base_url, book_uri";
  
  private static final Pattern LANDING_PATTERN = Pattern.compile(
      "^((https?://[^/]+/)publications/(.+))$",
      Pattern.CASE_INSENSITIVE);
  private static final String LANDING_REPLACEMENT = "$1";
  /* this link is no longer provided on the article landing page - it doesn't exist any more*/
  /* leave this in for previously preserved content */  
  private static final String DOWNLOAD_REPLACEMENT = "$1/download";
  
  // https://press.anu.edu.au/publications/service-company-vol-2
  // https://press.anu.edu.au/publications/service-company-vol-2/download
  // https://press.anu.edu.au/node/249/download
  // http://press-files.anu.edu.au/downloads/press/p25701/pdf/book.pdf
  // Later if needed, we can add scraping to get the full text PDF link
  // NOTE - the raw metadata has: citation_pdf_url which we could use to get a PDF url 
  
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE,
        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    builder.addAspect(
        LANDING_PATTERN,
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT, 
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    builder.addAspect(
        DOWNLOAD_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_ABSTRACT);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
