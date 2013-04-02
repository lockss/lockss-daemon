/*
 * $Id: ASCEArticleIteratorFactory.java,v 1.1 2013-04-02 21:16:22 ldoan Exp $
 */

/*

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

package org.lockss.plugin.americansocietyofcivilengineers;

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
 * Gets articles once crawled.
 * toc: http://ascelibrary.org/toc/jaeied/18/1
 * abs: http://ascelibrary.org/doi/abs/10.1061/%28ASCE%29AE.1943-5568.0000042
 * full text: http://ascelibrary.org/doi/full/10.1061/%28ASCE%29AE.1943-5568.0000082
 * ref: http://ascelibrary.org/doi/ref/10.1061/%28ASCE%29AE.1943-5568.0000042
 * pdf: http://ascelibrary.org/doi/pdf/10.1061/%28ASCE%29AE.1943-5568.0000042
 * 
 * Metadata found in abtract and full text html pages.
 */
public class ASCEArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(ASCEArticleIteratorFactory.class);
  
  // ROOT_TEMPLATE AND PATTERN_TEMPLATE required by SubTreeArticleIterator.
  // SubTreeArticleIterator returns only the URLs under ROOT_TEMPLATE, that
  // match PATTERN_TEMPLATE.  In this case, only URLs ending with 'type=0'
  // are returned. 'type=0' is the abstract which also contains medatata. */
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  protected static final String PATTERN_TEMPLATE = "\"^%sdoi\", base_url";
  
  // Create ASCEArticleIterator with the new object of 
  // SubTreeArticleIterator ROOT_TEMPLATE and PATTERN_TEMPLATE already set.
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
                                                          throws PluginException {
    return new ASCEArticleIterator(au,
                                      new SubTreeArticleIterator.Spec()
                                          .setTarget(target)
                                          .setRootTemplate(ROOT_TEMPLATE)
                                          .setPatternTemplate(PATTERN_TEMPLATE,
                                              Pattern.CASE_INSENSITIVE)
                                      );
  }
  
  // Create article files.  Use abstract url as the base, and get other file
  // types from it, full-text html, pdf, epub, mobile.
  protected static class ASCEArticleIterator extends SubTreeArticleIterator {
    
    // this pattern is derived from PATTERN_TEMPLATE,
    // to create regex 'capturing groups', used for guessing other file types.
    protected Pattern ABSTRACT_PATTERN = Pattern.compile("(doi)/abs", Pattern.CASE_INSENSITIVE);
    protected ASCEArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    // Start to match abstract url, the guess other file types from it.
    // Abstrqct url: "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;
    // volume=9;issue=1;spage=1;epage=2;aulast=Rutz;tyep=0"
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.debug("createArticleFiles: " + url);
      Matcher abstractMat = ABSTRACT_PATTERN.matcher(url);
      if (abstractMat.find()) {
        return processAbstract(cu, abstractMat);
      }
      // Incompatible between ArticleIteratorFactor and ArtileIterator objects
      // Should not happen.
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles processAbstract(CachedUrl abstractCu, Matcher absMat) {
      ArticleFiles af = new ArticleFiles();
      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, abstractCu);
      af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, abstractCu);
      guessAdditionalFiles(af, absMat);
      return af;
    }

    protected ArticleFiles guessAdditionalFiles(ArticleFiles af, Matcher absMat) {
      CachedUrl htmlCu = au.makeCachedUrl(absMat.replaceFirst("$1/full"));
      setArticleRole(af, htmlCu, ArticleFiles.ROLE_FULL_TEXT_HTML);
      CachedUrl pdfCu = au.makeCachedUrl(absMat.replaceFirst("$1/pdf"));
      setArticleRole(af, pdfCu, ArticleFiles.ROLE_FULL_TEXT_PDF);
      CachedUrl refCu = au.makeCachedUrl(absMat.replaceFirst("/$1/ref"));
      setArticleRole(af, refCu, ArticleFiles.ROLE_REFERENCES);
      chooseFullTextCu(af);
      return af;
    }
    
    protected ArticleFiles chooseFullTextCu(ArticleFiles af) {
      final String[] ORDER = new String[] {
          ArticleFiles.ROLE_FULL_TEXT_HTML,
          ArticleFiles.ROLE_FULL_TEXT_PDF,
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
    }
   
    // Set article role for each file type.
    private void setArticleRole(ArticleFiles af, CachedUrl cu, String role) {
      if ((cu != null) && (cu.hasContent())) {
        if (role != null) {
          af.setRoleCu(role, cu);
        }
      }
      AuUtil.safeRelease(cu);
    }
  }

  // Create Article Metadata Extractor
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
