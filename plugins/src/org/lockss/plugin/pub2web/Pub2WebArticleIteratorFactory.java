/* $Id:$
 
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

package org.lockss.plugin.pub2web;

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

public class Pub2WebArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(Pub2WebArticleIteratorFactory.class);
  

  /*
   * a simplified pub2web implementation where the full-text html and pdf of the article/chapter
   * are the crawler friendly versions
   * article landing page:
   *   http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/jgv.0.000003
   * article citation page(s):
   *   http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/jgv.0.000003/cite/(bibtex|endnote|plaintext|ris)
   * full-text html & pdf:
   *   http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/jgv.0.000003?crawler=true&mimetype=(html|application/pdf)
   *   
   *   The PDF urls that have the deliver/fulltext/...?itemId= are only the 
   *   pdf of the TOCs and supplementary data...
   */
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  

  // match pdf, html and article landing page for each article
  // make sure the first part of the doi is 2 numbers - dot - 4 numbers so you 
  // don't pick up the toc pages
  // yes: http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.071845-0
  // no:  http://jgv.microbiologyresearch.org/content/journal/jgv/96/1

  // the journal option also has an extra subdir (journal_id)
  protected static final String PATTERN_TEMPLATE =    
    "\"%scontent/(book|journal/[^/]+)/[0-9]{2}\\.[0-9]{4}/[^/?]+(\\?crawler=true.*)?$\", base_url";

  // Groups:
  // 1. book or "journal/jid"
  // 2. identifier (doi) portion of article/chapter/book url
  // don't need to be so explicit about doi syntax because PATTERN_TEMPLATE is restrictive
  protected Pattern PDF_PATTERN = 
      Pattern.compile("/content/(book|journal/[^/]+)/([0-9.]+/[^/?]+)\\?crawler=true&mimetype=application/pdf", Pattern.CASE_INSENSITIVE);
  protected static String PDF_REPLACEMENT = "/content/$1/$2?crawler=true&mimetype=application/pdf";

  protected Pattern HTML_PATTERN = 
      Pattern.compile("/content/(book|journal/[^/]+)/([0-9.]+/[^/?]+)\\?crawler=true&mimetype=html", Pattern.CASE_INSENSITIVE);
  protected static String HTML_REPLACEMENT = "/content/$1/$2?crawler=true&mimetype=html";

  protected Pattern LANDING_PATTERN = 
      Pattern.compile("/content/(book|journal/[^/]+)/([0-9.]+/[^/?]+)$", Pattern.CASE_INSENSITIVE);
  protected static String LANDING_REPLACEMENT = "/content/$1/$2";

  // secondary aspects, get from the others $1 = journal or book; $2 is the identifying inf
  protected static String CITATION_BIB_REPLACEMENT = "/content/$1/$2/cite/bibtex";
  protected static String CITATION_END_REPLACEMENT = "/content/$1/$2/cite/endnote";
  protected static String CITATION_REF_REPLACEMENT = "/content/$1/$2/cite/refworks";
   
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
    builder.addAspect(CITATION_END_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION_ENDNOTE);
    // secondary roles don't have enough info to trigger an article
    builder.addAspect(CITATION_REF_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION);
    
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