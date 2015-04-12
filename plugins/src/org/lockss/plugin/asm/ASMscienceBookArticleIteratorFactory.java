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

public class ASMscienceBookArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(ASMscienceBookArticleIteratorFactory.class);
  

  /*
   * We don't have full text for the entire book, so an 'article' is only the subsections
   * This is a little bit complicated - use the PDF, which has a uniquely complicated URL as the trigger
   *  because you can generate the other aspects from that.
   *  There is not enough information in the full-text html or landing page to generate the PDF aspect's URL
   * But this is okay because we will have full-text PDF in all cases where we have content  
   *
   * The pdf URL:
   * /deliver/fulltext/10.1128/9781555818289/9781555811303_Chap01.pdf \\
   *       ?itemId=/content/book/10.1128/9781555818289.chap1&mimeType=pdf&isFastTrackArticle=
   *  note that the PDF filename uses the print issn/isbn and unique chapter naming
   *  but the itemID uses the eissn/eisbn and the chapter naming used by the rest of the aspects     
   *       
   * chapter landing page:
   *   http://www.asmscience.org/content/book/10.1128/9781555818289.chap1
   * chapter citation page(s):
   *   http://www.asmscience.org/content/book/10.1128/9781555818289.chap1/cite/(bibtex|endnote|plaintext|ris)
   * full-text html:
   *   /deliver/fulltext/10.1128/9781555818289/chap1.html\\
   *       ?itemId=/content/book/10.1128/9781555818289.chap1&mimeType=html&isFastTrackArticle=
   * full-text xml:
   *   /deliver/fulltext/10.1128/9781555818289/chap1.xml\\
   *       ?itemId=/content/book/10.1128/9781555818289.chap1&mimeType=xml&isFastTrackArticle=
   *
   */
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  
  /* catch book section html or book section pdf */
  protected static final String PATTERN_TEMPLATE =    
    "\"%sdeliver/fulltext/%s/[^./?]+\\.pdf\", base_url, doi";

  // PDF uses print version of isbn and unique chapter naming convention so can't be created by others
  // capture the initial DOI which is the root for the rest of the book aspects
  // capture the entirety of the PDF args (to use for simple PDF-PDF replacement
  // capture the portion of the itemId that represents this specific portion of the book (eg "chap1")
  protected Pattern PDF_PATTERN = 
      Pattern.compile("/deliver/fulltext/([0-1]{2}\\.[0-9]{4}/[0-9]{13})/([^./?]+\\.pdf\\?itemId=/content/book/[^/]+/[^./&]+\\.([^&]+)\\&mimeType=.*)", Pattern.CASE_INSENSITIVE);
  
   protected static String PDF_REPLACEMENT = "/deliver/fulltext/$1/$2";
   
   // these can be created from the PDF url
   protected static String HTML_ABSTRACT_REPLACEMENT = "/content/book/$1.$3";
   protected static String HTML_REPLACEMENT = "/content/book/$1.$3?crawler=true";
   protected static String CITATION_REF_REPLACEMENT = "/content/book/$1.$3/cite/refworks";
   protected static String CITATION_BIB_REPLACEMENT = "/content/book/$1.$3/cite/bibtex";
   protected static String CITATION_END_REPLACEMENT = "/content/book/$1.$3/cite/endnote";
   
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