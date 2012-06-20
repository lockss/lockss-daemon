/*
 * $Id: HighWirePressH20ArticleIteratorFactory.java,v 1.7.8.1 2012-06-20 00:03:03 nchondros Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class HighWirePressH20ArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log =
    Logger.getLogger("HighWirePressH20ArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE =
    "\"%scontent/%s/\", base_url, volume_name";
  
  protected static final String PATTERN_TEMPLATE =
    "\"^%scontent/%s/([^/]+/)?[^/]+\\.full(\\.pdf)?$\", base_url, volume_name";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new
      HighWirePressH20ArticleIterator(au,
				      new SubTreeArticleIterator.Spec()
				      .setTarget(target)
				      .setRootTemplate(ROOT_TEMPLATE)
				      .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class HighWirePressH20ArticleIterator
    extends SubTreeArticleIterator {
    
    protected static Pattern HTML_PATTERN =
      Pattern.compile("/([^/]+)\\.full$", Pattern.CASE_INSENSITIVE);
    
    protected static Pattern PDF_PATTERN =
      Pattern.compile("/([^/]+)\\.full\\.pdf$", Pattern.CASE_INSENSITIVE);
    
    public HighWirePressH20ArticleIterator(ArchivalUnit au,
                                           SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;
      
      mat = HTML_PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextHtml(cu, mat);
      }
        
      mat = PDF_PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextPdf(cu, mat);
      }
      
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullTextHtml(CachedUrl htmlCu,
					       Matcher htmlMat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(htmlCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
      af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, htmlCu);
//      guessFullTextPdf(af, htmlMat);
//      guessOtherParts(af, htmlMat);
      return af;
    }
    
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      CachedUrl htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/$1.full"));
      if (htmlCu != null && htmlCu.hasContent()) {
        return null;
      }
      ArticleFiles af = new ArticleFiles();
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);

      CachedUrl pdfLandCu = au.makeCachedUrl(pdfMat.replaceFirst("/$1.full.pdf+html"));
      if (pdfLandCu != null && pdfLandCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, pdfLandCu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, pdfLandCu);
        af.setFullTextCu(pdfLandCu);
      }
      else {
        af.setFullTextCu(pdfCu);
      }
//      guessOtherParts(af, pdfMat);
      return af;
    }
    
//    protected void guessOtherParts(ArticleFiles af, Matcher mat) {
//      guessAbstract(af, mat);
//      guessFigures(af, mat);
//      guessSupplementaryMaterials(af, mat);
//    }
//    
//    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
//      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/$1.pdf"));
//      if (pdfCu != null && pdfCu.hasContent()) {
//        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
//        CachedUrl pdfLandCu = au.makeCachedUrl(mat.replaceFirst("/$1.full.pdf+html"));
//        if (pdfLandCu != null && pdfLandCu.hasContent()) {
//          af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, pdfLandCu);
//        }
//      }
//    }
//
//    protected void guessAbstract(ArticleFiles af, Matcher mat) {
//      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/$1.abstract"));
//      if (absCu != null && absCu.hasContent()) {
//        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
//      }
//    }
//    
//    protected void guessFigures(ArticleFiles af, Matcher mat) {
//      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/$1.figures-only"));
//      if (absCu != null && absCu.hasContent()) {
//        af.setRoleCu(ArticleFiles.ROLE_FIGURES_TABLES, absCu);
//      }
//    }
//    
//    protected void guessSupplementaryMaterials(ArticleFiles af, Matcher mat) {
//      CachedUrl suppinfoCu = au.makeCachedUrl(mat.replaceFirst("/$1/suppl/DCSupplemental"));
//      if (suppinfoCu != null && suppinfoCu.hasContent()) {
//        af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, suppinfoCu);
//      }
//    }
//    
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
