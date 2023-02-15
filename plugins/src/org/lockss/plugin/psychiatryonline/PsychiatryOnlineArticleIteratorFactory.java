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

package org.lockss.plugin.psychiatryonline;

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
 * Gets book once crawled.
 * Metadata found in PsychiatryOnlineMetadataExtractorFactory.java.
 */
public class PsychiatryOnlineArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(PsychiatryOnlineArticleIteratorFactory.class);
  
  // ROOT_TEMPLATE AND PATTERN_TEMPLATE required by SubTreeArticleIterator.
  // SubTreeArticleIterator returns only the URLs under ROOT_TEMPLATE, that
  // match PATTERN_TEMPLATE.
  // root: http://www.psychiatryonline.com/
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  // http://www.psychiatryonline.com/resourceTOC.aspx?resourceID=5
  protected static final String PATTERN_TEMPLATE = "\"%sresourceTOC[.]aspx[?]resourceID=[0-9]+$\", base_url";
  
  protected Pattern PUB_PATTERN = Pattern.compile(
      "/resourceTOC[.]aspx[?]resourceID=([0-9]+)$");
  protected static String PUB_REPLACEMENT = "/resourceTOC.aspx?resourceID=$1";
  
  // Create PsychiatryOnlineArticleIterator with the new object of 
  // SubTreeArticleIterator ROOT_TEMPLATE and PATTERN_TEMPLATE already set.
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
      MetadataTarget target)
          throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target, ROOT_TEMPLATE, PATTERN_TEMPLATE);
    
    // primary roles have enough info to trigger an article.
    // the order of builder.addAspect is important.
    builder.addAspect(PUB_PATTERN,
        PUB_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA, ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    return builder.getSubTreeArticleIterator();
  }
  
  // Create Article Metadata Extractor
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
