/**
 * $Id$
 */

/**

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.medknow;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * Medknow's Abstract page has links to Full-Text HTML and PDF, and also
 * contains metatdata. Medknow's file type:
 *      type=0  - Abstract
 *      no type - Full-Text HTML
 *      type=2  - Full-text PDF
 *      type=3  - Full-Text Mobile
 *      type=4  - EPUB 
 *      
 * also contains citation URLs:
 * "http://www.afrjpaedsurg.org/citation.asp?issn=0189-6725;year=2012;volume=9;
 * issue=1;spage=13;epage=16;aulast=Kothari;
 * aid=AfrJPaediatrSurg_2012_9_1_13_93295"
 *      
 * full-text HTML URL:
 * "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;
 *  issue=1;spage=1;epage=2;aulast=Rutz"
 */
public class MedknowArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(MedknowArticleIteratorFactory.class);
  
  /** ROOT_TEMPLATE AND PATTERN_TEMPLATE required by SubTreeArticleIterator.
    SubTreeArticleIterator returns only the URLs under ROOT_TEMPLATE, that
    match PATTERN_TEMPLATE.  In this case, only URLs ending with 'type=0'
    are returned. 'type=0' is the abstract which also contains medatata. */
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  protected static final String PATTERN_TEMPLATE = "\"^%sarticle\\.asp\\?issn=%s;year=%d;volume=%s;.*;type=0$\", base_url, journal_issn, year, volume_name";
  
  /**
   * Create MedknowArticleIterator with the new object of 
   * SubTreeArticleIterator ROOT_TEMPLATE and PATTERN_TEMPLATE already set.
   */
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
                                                          throws PluginException {
      
    return new MedknowArticleIterator(au,
                                      new SubTreeArticleIterator.Spec()
                                          .setTarget(target)
                                          .setRootTemplate(ROOT_TEMPLATE)
                                          .setPatternTemplate(PATTERN_TEMPLATE,
                                              Pattern.CASE_INSENSITIVE)
                                      );
  } /** end createArticleIterator */     
  
  
  /**
   * Create article files.  Use abstract url as the base, and get other file
   * types from it, full-text html, pdf, epub, mobile.
   */
  protected static class MedknowArticleIterator extends SubTreeArticleIterator {
 
    /** this pattern is derived from PATTERN_TEMPLATE,
      to create regex 'capturing groups', used for guessing other file types. */
    protected Pattern ABSTRACT_PATTERN = Pattern.compile("/(article\\.asp)(\\?.*);type=0$", Pattern.CASE_INSENSITIVE);

    protected MedknowArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec) {
        super(au, spec);
    }
    
    /**
     * Start to match abstract url, the guess other file types from it.
     * Abstrqct url: "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;
     *  volume=9;issue=1;spage=1;epage=2;aulast=Rutz;tyep=0"
     */
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {

      String url = cu.getUrl();
      log.debug("createArticleFiles: " + url);

      Matcher abstractMat = ABSTRACT_PATTERN.matcher(url);
      if (abstractMat.find()) {
        return processAbstract(cu, abstractMat);
      }
      
      /** Incompatible between ArticleIteratorFactor and ArtileIterator objects
        Should not happen. */
      log.warning("Mismatch between article iterator factory and article iterator: " + url);

      return null;

    } /** end createArticleFiles */
    
    protected ArticleFiles processAbstract(CachedUrl abstractCu, Matcher absMat) {

      ArticleFiles af = new ArticleFiles();
      
      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, abstractCu);
      af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, abstractCu);
      guessAdditionalFiles(af, absMat);

      return af;
      
    } /** end processAbstract */

    protected ArticleFiles guessAdditionalFiles(ArticleFiles af, Matcher absMat) {
      
      CachedUrl htmlCu = au.makeCachedUrl(absMat.replaceFirst("/$1$2"));
      //af.setFullTextCu(htmlCu);
      setArticleRole(af, htmlCu, ArticleFiles.ROLE_FULL_TEXT_HTML);
      
      CachedUrl pdfCu = au.makeCachedUrl(absMat.replaceFirst("/$1$2;type=2"));
      setArticleRole(af, pdfCu, ArticleFiles.ROLE_FULL_TEXT_PDF);

      CachedUrl mobileCu = au.makeCachedUrl(absMat.replaceFirst("/$1$2;type=3"));
      setArticleRole(af, mobileCu, ArticleFiles.ROLE_FULL_TEXT_MOBILE);

      CachedUrl epubCu = au.makeCachedUrl(absMat.replaceFirst("/$1$2;type=4"));
      setArticleRole(af, epubCu, ArticleFiles.ROLE_FULL_TEXT_EPUB);                              
    
      CachedUrl citationCu = au.makeCachedUrl(absMat.replaceFirst("/citation.asp$2"));
      setArticleRole(af, citationCu, ArticleFiles.ROLE_CITATION);

      chooseFullTextCu(af);
      
      return af;
      
    } /** end guessAdditionalFiles */
    
    protected ArticleFiles chooseFullTextCu(ArticleFiles af) {
      
      final String[] ORDER = new String[] {
          ArticleFiles.ROLE_FULL_TEXT_HTML,
          ArticleFiles.ROLE_FULL_TEXT_PDF,
          ArticleFiles.ROLE_FULL_TEXT_MOBILE,
          ArticleFiles.ROLE_FULL_TEXT_EPUB
      };
      
      for (String role : ORDER) {
        CachedUrl cu = af.getRoleCu(role);
        if (cu != null) {
          af.setFullTextCu(cu);
          return af;
        }
      }
      
      log.debug2("No full-text CU");
      return af; // af probably has other data in it, don't want to return null
      
    } /** end chooseFullTextCu */
   
    
    /**
     * Set article role for each file type. Also set ROLE_ARTICLE_METADATA
     * for Abstract HTML file. Param role2 is ArticleFiles.ROLE_ARTICLE_METADATA.
     */
    private void setArticleRole(ArticleFiles af, CachedUrl cu, String role) {

      if ((cu != null) && (cu.hasContent())) {
        if (role != null) {
          af.setRoleCu(role, cu);
        }
      }
      AuUtil.safeRelease(cu);

    } /** end setArticleRole */
      
  } /** end class MedknowArticleIterator */
   
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

} /** end class MedknowArticleIteratorFactory */
