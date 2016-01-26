/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.peerj;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class PeerJ2016ArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {

  private static final Logger log = 
      Logger.getLogger(PeerJ2016ArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%sarticles/\", base_url";
  
  /*
   * In order not to destabilize the legacy PeerJPlugin, this iterator serves
   * the new PeerJ2016Plugin which takes an additional parameter - journal_id
   * and allows for a journal-id prefix to the article number.  
   * PeerJ journal: https://peerj.com/articles/250.pdf
   * PeerJ computer science: https://peerj.com/articles/cs-42.pdf
   *   since a url won't get collected unless the optional journal letters match
   *   the current journal, we can just allow any chars plus hyphen (optionally)
   * This iterator also requires a trailing "/" at the end of the full-text html/abstract  
   */
  
  /* catch all html or pdf aspects with the pattern 
   * peerj.com/articles/250/ <--ignore terminating slash, daemon does...
   * peerj.com/articles/cs-25/
   * peerj.com/articles/250.pdf
   * peerj.com/articles/cs-25.pdf
   */
  protected static final String PATTERN_TEMPLATE = 
      "\"^%sarticles/(([a-z]+-)?[0-9]+)(\\.pdf)?$\", base_url";
  
  private Pattern PDF_PATTERN = 
      Pattern.compile("/articles/(([a-z]+-)?[0-9]+)\\.pdf$", Pattern.CASE_INSENSITIVE);

  private Pattern HTML_PATTERN = 
      Pattern.compile("/articles/(([a-z]+-)?[0-9]+)$", Pattern.CASE_INSENSITIVE);

  
  // generate the following variants
  // peerj.com/articles/55.bib
  // peerj.com/articles/55.ris
  // peerj.com/articles/55.xml
  // peerj.com/articles/cs-42.xml, etc
  
  // this 2016 plugin does not collect the link rel but not accessible
  // peerj.com/articles/55.html
  // peerj.com/articles/55.rdf
  // peerj.com/articles/55.json
  // peerj.com/articles/55.unixref
  private static String PDF_REPLACEMENT = "/articles/$1.pdf";
  private static String HTML_REPLACEMENT = "/articles/$1";
  private static String XML_REPLACEMENT = "/articles/$1.xml";
  private static String BIB_REPLACEMENT = "/articles/$1.bib";
  private static String RIS_REPLACEMENT = "/articles/$1.ris";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, 
      MetadataTarget target) throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = 
        new SubTreeArticleIteratorBuilder(au);  
    
    builder.setSpec(target,ROOT_TEMPLATE, PATTERN_TEMPLATE, 
        Pattern.CASE_INSENSITIVE);
    
    // The order in which these aspects are added is important. They determine
    // which will trigger the ArticleFiles and if you are only counting 
    // articles (not pulling metadata) then the lower aspects aren't looked 
    // for, once you get a match.

    // html landing page has full-text
    builder.addAspect(HTML_PATTERN, HTML_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_HTML);

    // set up PDF to be an aspect that will trigger an ArticleFiles
    builder.addAspect(PDF_PATTERN, PDF_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_PDF);   

    builder.addAspect(XML_REPLACEMENT, ArticleFiles.ROLE_FULL_TEXT_XML);

    builder.addAspect(BIB_REPLACEMENT, ArticleFiles.ROLE_CITATION_BIBTEX);

    // Currently only a RIS metadata extractor. Every articles seems to have this.
    // could build an html extractor if it proved necessary
    builder.addAspect(RIS_REPLACEMENT, ArticleFiles.ROLE_CITATION_RIS, 
        ArticleFiles.ROLE_ARTICLE_METADATA);


    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF, ArticleFiles.ROLE_FULL_TEXT_HTML);  
        
     return builder.getSubTreeArticleIterator();
  }  
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
