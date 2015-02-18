/* 
 * $Id$
 */

/*
Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
