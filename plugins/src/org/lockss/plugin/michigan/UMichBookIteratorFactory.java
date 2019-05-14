/*
 * $Id: $
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.michigan;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class UMichBookIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(UMichBookIteratorFactory.class);
  

    protected static final String PATTERN_TEMPLATE = "\"^%s%s$\", base_url, book_uri";
  
  private static final Pattern LANDING_PATTERN = Pattern.compile(
      "^(https?://[^/]+/)(.*/[^/]+)$",
      Pattern.CASE_INSENSITIVE);
  private static final String LANDING_REPLACEMENT = "$1/$2";
  

    /*
     *    Book landing page is this:
     *    https://www.fulcrum.org/concern/monographs/q811kk505
     *    and there is some html metadata there so use this file 
     *    At least in some cases, the full-text access urls can be found here too
     *    as download links. 
     *    
     *    If not, on the page that is the "read book" link of the form
     *    https://www.fulcrum.org/epubs/bz60cx371
     *    is an html page that may have hidden "download_links" that we extract
     *    but they'll match the "download" menu on the landing page if that's there.
     *    
     *    We could parse the landing page to try to identify the PDF and EPUB full text cu
     *    but for now this is sufficient.  
     */

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
        
    builder.setSpec(builder.newSpec()
            .setTarget(target)
            .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));    

    builder.addAspect(
        LANDING_PATTERN,
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT, 
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    
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
