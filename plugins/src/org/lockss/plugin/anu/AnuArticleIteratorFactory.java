/*
 * $Id: $
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

public class AnuArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(AnuArticleIteratorFactory.class);
  
  // params from tdb file corresponding to AU
  protected static final String ROOT_TEMPLATE = "\"%spublications/\", base_url";
  //make the pattern after the journal id and volume number more general, we were missing content
  // could be -2015, or -issue-xy or (add more examples here)
  //in 2019 format changed and added extra layer publications/journals/journal-id/journal-id-volume-foo
  // to support both old and new, add in optional level 
  protected static final String PATTERN_TEMPLATE =
      "\"^%spublications/(?:journals/(%s/)?)?(?!.+/download$)(%s|[a-z-]+)(-(ii-number|issue|volume|no-[1-9]|winter))?-%s(-[a-z0-9].+?)?$\", base_url, journal_id,journal_id, volume_name";
  
  //we don't even get here unless the PATTERN_TEMPLATE worked and that was checkeding journal_id
  //allow option extra level of "journal-id/" just after publications/journals/
  private static final Pattern LANDING_PATTERN = Pattern.compile(
      "^((https?://[^/]+/)publications/(?:journals/)?(([^/]+/)?[^/]+))$",
      Pattern.CASE_INSENSITIVE);
  private static final String LANDING_REPLACEMENT = "$1";
  /* this link is no longer provided on the article landing page - it doesn't exist any more*/
  /* leave this in for previously preserved content */
  private static final String DOWNLOAD_REPLACEMENT = "$1/download";
  
  // https://press.anu.edu.au/publications/aboriginal-history-journal-volume-39
  // https://press.anu.edu.au/publications/aboriginal-history-journal-volume-39/download
  // https://press-files.anu.edu.au/downloads/press/p332783/pdf/book.pdf
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
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE,
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
