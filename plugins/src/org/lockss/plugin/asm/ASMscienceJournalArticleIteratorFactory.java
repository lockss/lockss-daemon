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

package org.lockss.plugin.asm;

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

public class ASMscienceJournalArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(ASMscienceJournalArticleIteratorFactory.class);
  

  /*
   * We don't have full text for the entire book, so an 'article' is only the subsections
   * This is a little bit complicated - use the PDF, which has a uniquely complicated URL as the trigger
   *  because you can generate the other aspects from that.
   *  There is not enough information in the full-text html or landing page to generate the PDF aspect's URL
   * But this is okay because we will have full-text PDF in all cases where we have content  
   *
   * The pdf URL:
   *  http://www.asmscience.org/deliver/fulltext/microbiolspec/2/6/AID-0022-2014.pdf
   *       ?itemId=/content/journal/microbiolspec/10.1128/microbiolspec.AID-0022-2014&mimeType=pdf&isFastTrackArticle=   
   *       
   * article landing page:
   *   http://www.asmscience.org/content/journal/microbiolspec/10.1128/microbiolspec.AID-0022-2014
   * article citation page(s):
   *   http://www.asmscience.org/content/journal/microbiolspec/10.1128/microbiolspec.AID-0022-2014/cite/(bibtex|endnote|plaintext|ris)
   * full-text html:
   *   /deliver/fulltext/
   * full-text xml:
   *   /deliver/fulltext/
   *
   */
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  
  /* catch book section html or book section pdf */
  //deliver/fulltext/<journal_id>/<volume>/<issue>/identifier.pdf
  protected static final String PATTERN_TEMPLATE =    
    "\"%sdeliver/fulltext/%s/%s/[0-9]+/[^./?]+\\.pdf\", base_url, journal_id, volume_name";

  // Groups:
  // 1. journalid/vol/issue combo
  // 2. pdf tail - file + arguments
  // 3. journal_id
  // 4. doi portion 1
  // 5. doi portion 2 (full filename)
  protected Pattern PDF_PATTERN = 
      Pattern.compile("/deliver/fulltext/([^/]+/[^/]+/[^/]+)/([^/?]+\\.pdf\\?itemId=/content/journal/([^/]+)/([0-9]{2}\\.[0-9]{4})/([^/&]+)&mimeType=.*)", Pattern.CASE_INSENSITIVE);
  
   protected static String PDF_REPLACEMENT = "/deliver/fulltext/$1/$2";
   
   // these can be created from the PDF url
   protected static String HTML_ABSTRACT_REPLACEMENT = "/content/journal/$3/$4/$5";
   protected static String HTML_REPLACEMENT = "/content/journal/$3/$4/$5?crawler=true";
   protected static String CITATION_BIB_REPLACEMENT = "/content/journal/$3/$4/$5/cite/bibtex";
   protected static String CITATION_END_REPLACEMENT = "/content/journal/$3/$4/$5/cite/endnote";
   protected static String CITATION_REF_REPLACEMENT = "/content/journal/$3/$4/$5/cite/refworks";
   
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
    
    /* 
     * this only works for sections of the book because a plain landing page 
     * does not guarantee content and does not have a pdf
     * make this secondary.... pdf uses args that we can't guess
     */
    builder.addAspect(HTML_ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);

    // This isn't viewable because of a doctype declaration, but all the html is there
    builder.addAspect(HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
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
        ArticleFiles.ROLE_FULL_TEXT_HTML);

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