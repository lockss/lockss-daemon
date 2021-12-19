/*
 * $Id$
 */

/*

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

package org.lockss.plugin.georgthiemeverlag;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class GeorgThiemeVerlagArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(GeorgThiemeVerlagArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
      "\"%s\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%s(?:[^/]+/)?(?:ejournals|ebooks)/(?:abstract|html|pdf)/10[.][0-9a-z]{4,6}/[^/?&]+(?:[.]pdf|[?]issue=[^&]+)?(?:\\?articleLanguage=.*)?$\"," +
      " base_url";
  
  // various aspects of an article
  // https://www.thieme-connect.de/products/ejournals/html/10.1055/s-0029-1214947
  // https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0029-1214947.pdf
  // https://www.thieme-connect.de/ejournals/abstract/10.1055/s-0029-1214947
  // optional language argument now (12/22/17) when an article has more than one language option
  // TODO - rewrite this without using builder template to substitute variants to  
  // count all languages as one aspect - hardcode to try both 'en' and 'de'
  // FOR now - leave simple and just have the two languages be two different articles
  // and the DOI should resolve them to one for publisher counts
  // 
  // https://www.thieme-connect.de/products/ejournals/abstract/10.1055/s-0037-1608741?articleLanguage=de
  // https://www.thieme-connect.de/products/ejournals/ris/10.1055/s-0037-1608741/BIB?articleLanguage=de
  // https://www.thieme-connect.de/products/ejournals/pdf/10.1055/s-0037-1608741.pdf?articleLanguage=de
  // https://www.thieme-connect.de/products/ejournals/pdf/10.1055/s-0037-1608741.pdf?articleLanguage=en
  
  // Identify groups in the pattern "/(html|pdf|abstract)(<doi>)(.pdf)(optional_lang_arg)
  // NOTE the "or nothing" portion of the language group is to guarantee a group(2) which
  // otherwise would lead to IndexOutOfBoundsException when it wasn't present

  //https://www.thieme-connect.de/products/ejournals/pdf/10.4103/2454-6798.165082.pdf
  protected static final Pattern HTML_PATTERN = Pattern.compile(
      "/html/([^/]+/[^/?&]+)(\\?articleLanguage=.*|$)",
      Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "/pdf/([^/]+/[^/?&]+)[.]pdf(\\?articleLanguage=.*|$)",
      Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern ABSTRACT_PATTERN = Pattern.compile(
      "/abstract/([^/]+/[^/?&]+)(\\?articleLanguage=.*|$)",
      Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String HTML_REPLACEMENT = "/html/$1$2";
  protected static final String PDF_REPLACEMENT = "/pdf/$1.pdf$2";
  protected static final String ABSTRACT_REPLACEMENT = "/abstract/$1$2";
  protected static final String RIS_REPLACEMENT = "/ris/$1/BIB$2";
  
  public Iterator<ArticleFiles> createArticleIterator(
      ArchivalUnit au, MetadataTarget target)
          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up fulltext, pdf, abstract to be aspects that will trigger an ArticleFiles
    // NOTE - for the moment this also means it is considered a FULL_TEXT_CU
    // until this fulltext concept is deprecated
    // Making abstract also sufficient to be an ArticleFiles because meeting abstracts
    // only have the one aspect yet they must be counted
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(
        HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        ABSTRACT_PATTERN, ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);
    
    builder.addAspect(
        RIS_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_RIS);
    
    // add metadata role from abstract, html, or pdf (NOTE: pdf metadata gets DOI from filename)
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, Arrays.asList(
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_PDF));
    
    return builder.getSubTreeArticleIterator();
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
