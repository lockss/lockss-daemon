/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

public class PeerJArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(PeerJArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%sarticles/\", base_url";
  
  protected static final String PATTERN_TEMPLATE = 
      "\"^%s(articles)/([0-9]+)(\\.pdf)?$\", base_url";
  
  private Pattern PDF_PATTERN = 
      Pattern.compile("/(articles)/([0-9]+)\\.pdf$", Pattern.CASE_INSENSITIVE);

  private Pattern ABSTRACT_PATTERN = 
      Pattern.compile("/(articles)/([0-9]+)$", Pattern.CASE_INSENSITIVE);
      
  private static String PDF_REPLACEMENT = "/$1/$2.pdf";
  private static String ABSTRACT_REPLACEMENT = "/$1/$2";
  private static String XML_REPLACEMENT = "/$1/$2.xml";
  private static String BIB_REPLACEMENT = "/$1/$2.bib";
  private static String RIS_REPLACEMENT = "/$1/$2.ris";
  private static String ALTERNATE_HTML_REPLACEMENT = "/$1/$2.html";
  private static String ALTERNATE_RDF_REPLACEMENT = "/$1/$2.rdf";
  private static String ALTERNATE_JSON_REPLACEMENT = "/$1/$2.json";
  // only in Archives (main) site not in Preprints site
  private static String ALTERNATE_UNIXREF_REPLACEMENT = "/$1/$2.unixref";
  
  public static final String ROLE_ALTERNATE_FULL_TEXT_HTML = 
                                                  "AlternateFullTextHtml";
  public static final String ROLE_ALTERNATE_RDF = "AlternateRdf";
  public static final String ROLE_ALTERNATE_JSON = "AlternateJson";
  public static final String ROLE_ALTERNATE_UNIXREF = "AlternateUnixref";
  
  
  // On an PeerJ publisher website, article content may look like:
  // from Archives (main) site
  // <peerjbase>.com/articles/55/
  // <peerjbase>.com/articles/55.pdf
  // <peerjbase>.com/articles/55.bib
  // <peerjbase>.com/articles/55.ris
  // <peerjbase>.com/articles/55.xml
  // <peerjbase>.com/articles/55.html
  // <peerjbase>.com/articles/55.rdf
  // <peerjbase>.com/articles/55.json
  // <peerjbase>.com/articles/55.unixref
  
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

    // html landing page has full-text and abstract,
    builder.addAspect(ABSTRACT_PATTERN, ABSTRACT_REPLACEMENT, 
        ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_FULL_TEXT_HTML);

    // set up PDF to be an aspect that will trigger an ArticleFiles
    builder.addAspect(PDF_PATTERN, PDF_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_PDF);   

    builder.addAspect(XML_REPLACEMENT, ArticleFiles.ROLE_FULL_TEXT_XML);

    builder.addAspect(BIB_REPLACEMENT, ArticleFiles.ROLE_CITATION_BIBTEX);

    builder.addAspect(RIS_REPLACEMENT, ArticleFiles.ROLE_CITATION_RIS, 
        ArticleFiles.ROLE_ARTICLE_METADATA);

    // full-text html file from <link rel> tag found from page source
    // this link is not found from web pages
    builder.addAspect(ALTERNATE_HTML_REPLACEMENT, 
        ROLE_ALTERNATE_FULL_TEXT_HTML);

    builder.addAspect(ALTERNATE_RDF_REPLACEMENT, ROLE_ALTERNATE_RDF);

    builder.addAspect(ALTERNATE_JSON_REPLACEMENT, ROLE_ALTERNATE_JSON);

    builder.addAspect(ALTERNATE_UNIXREF_REPLACEMENT, ROLE_ALTERNATE_UNIXREF);
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML, ArticleFiles.ROLE_FULL_TEXT_PDF,  
        ROLE_ALTERNATE_FULL_TEXT_HTML, ArticleFiles.ROLE_ABSTRACT); 
        
     return builder.getSubTreeArticleIterator();
  }  
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
