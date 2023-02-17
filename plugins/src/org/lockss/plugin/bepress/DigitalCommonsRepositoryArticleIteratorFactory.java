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

package org.lockss.plugin.bepress;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * There is no correlation in url patterns for the article aspects,
 * pdfs and abstracts. Hence, we can not guess the abstract urls from pdf urls.
 * The abstract pages contain metadata; however, since the abstract urls can
 * not be guessed, ROLE_ARTICLE_METADATA can not be set.
 * 
 * abstract - <dcrbase>.edu/statistics/122
 * pdf - <dcrbase>.edu/cgi/viewcontent.cgi?article=1108&context=statistics
 */
public class DigitalCommonsRepositoryArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(DigitalCommonsRepositoryArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%scgi/viewcontent\\.cgi[^/]+context=%s$\", base_url, collection";
  
  private Pattern PDF_PATTERN = 
      Pattern.compile("/(viewcontent\\.cgi[^/]+)$", Pattern.CASE_INSENSITIVE);

  private static String PDF_REPLACEMENT = "/$1";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, 
      MetadataTarget target) throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = 
                                        new SubTreeArticleIteratorBuilder(au);    
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
 
    // full text pdf - aspect that will trigger an ArticleFiles
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT, ArticleFiles.ROLE_FULL_TEXT_PDF);
                            
    return builder.getSubTreeArticleIterator();
  }  
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(
      MetadataTarget target) throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }

}
