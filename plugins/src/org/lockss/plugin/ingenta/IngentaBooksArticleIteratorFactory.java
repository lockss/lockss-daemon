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

package org.lockss.plugin.ingenta;

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

 */

public class IngentaBooksArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(IngentaBooksArticleIteratorFactory.class);
  

  /*
   * a simplified ingenta implementation where the full-text html and pdf of the article/chapter
   * are the crawler friendly versions
   * book landing page:
   *   http://www.ingentaconnect.com/content/bkpub/2ouacs/2015/00000001/00000001/art00001
   * book citation page(s):
   *   http://www.ingentaconnect.com/content/bkpub/2ouacs/2015/00000001/00000001/art00001?format=(ris|bib)
   * full-text html & pdf:
   * currently books only have pdf, but leave in html in case it is implemented
   *   http://www.ingentaconnect.com/content/bkpub/2ouacs/2015/00000001/00000001/art00001?crawler=true&mimetype=application/pdf
   */
  protected static final String ROOT_TEMPLATE = 
      "\"%scontent/%s/\", base_url, publisher_id";
  
  protected static final String PATTERN_TEMPLATE = 
      "\"^%scontent/%s/[^/]+/[0-9]{4}/[0-9]+/.{8}/art[0-9]{5}(\\?crawler=true.*)?$\", base_url, publisher_id";

  // Groups:
  // 1. unique identifier for this chapter/book
  // don't need to be so explicit about doi syntax because PATTERN_TEMPLATE is restrictive
  protected Pattern PDF_PATTERN = 
      Pattern.compile("/content/([^/]+/[^/]+/[0-9]+/[0-9]+/[0-9]+/art[0-9]{5})\\?crawler=true&mimetype=application/pdf", Pattern.CASE_INSENSITIVE);
  protected static String PDF_REPLACEMENT = "/content/$1?crawler=true&mimetype=application/pdf";

  protected Pattern HTML_PATTERN = 
      Pattern.compile("/content/([^/]+/[^/]+/[0-9]+/[0-9]+/[0-9]+/art[0-9]{5})\\?crawler=true&mimetype=html", Pattern.CASE_INSENSITIVE);
  protected static String HTML_REPLACEMENT = "/content/$1?crawler=true&mimetype=html";

  protected Pattern LANDING_PATTERN = 
      Pattern.compile("/content/([^/]+/[^/]+/[0-9]+/[0-9]+/[0-9]+/art[0-9]{5})$", Pattern.CASE_INSENSITIVE);
  protected static String LANDING_REPLACEMENT = "/content/$1";

  // secondary aspects, get from the others $1 = journal or book; $2 is the identifying inf
  protected static String CITATION_BIB_REPLACEMENT = "/content/$1?format=bib";
  protected static String CITATION_RIS_REPLACEMENT = "/content/$1?format=ris";
   
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
                                                          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
                    ROOT_TEMPLATE,
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);                            
    
    builder.addAspect(PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    builder.addAspect(HTML_PATTERN,
        HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    builder.addAspect(LANDING_PATTERN,
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE);
        
    // secondary roles don't have enough info to trigger an article
    builder.addAspect(CITATION_BIB_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION_BIBTEX);
    // secondary roles don't have enough info to trigger an article
    builder.addAspect(CITATION_RIS_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION_RIS);
    
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ABSTRACT);

    // Until I can write a bibtex, endnote or refworks extractor
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
        //ArticleFiles.ROLE_CITATION,
        ArticleFiles.ROLE_ABSTRACT);

    return builder.getSubTreeArticleIterator();
 
  }
  
  // Create Article Metadata Extractor
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}