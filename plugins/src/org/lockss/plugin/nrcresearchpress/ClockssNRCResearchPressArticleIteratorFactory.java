/*
 * $Id: ClockssNRCResearchPressArticleIteratorFactory.java,v 1.2 2013-04-11 20:15:02 aishizaki Exp $

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

package org.lockss.plugin.nrcresearchpress;

import java.util.Iterator;
import java.util.regex.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ClockssNRCResearchPressArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger("ClockssNRCResearchPressArticleIteratorFactory");
  //http://www.nrcresearchpress.com/
  protected static final String ROOT_TEMPLATE = "\"%s\",base_url";
  //http://www.nrcresearchpress.com/doi/abs/10.1139/h11-070
  //http://www.nrcresearchpress.com/doi/pdf/10.1139/h11-070
  protected static final String PATTERN_TEMPLATE = "\"^%sdoi/pdf/\\d+\\.\\d+/\\w+[\\d-]+\", base_url";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new ClockssNRCResearchArticleIterator(au, new SubTreeArticleIterator.Spec()
                                       .setTarget(target)
                                       .setRootTemplate(ROOT_TEMPLATE)
                                       .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
                                       );
  }
  
  protected static class ClockssNRCResearchArticleIterator extends SubTreeArticleIterator {
    //http://www.nrcresearchpress.com/doi/abs/10.1139/h11-070
    protected Pattern ABS_PATTERN = Pattern.compile(
              String.format("(%sdoi/)abs(/\\d+\\.\\d+/\\w[\\d-]+)",            
              au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey())),
              Pattern.CASE_INSENSITIVE);
    //http://www.nrcresearchpress.com/doi/pdf/10.1139/h11-070
    //http://www.nrcresearchpress.com/doi/pdfplus/10.1139/xxy11-070
    protected Pattern PDF_PATTERN = Pattern.compile(
              String.format("(%sdoi/)pdf(/\\d+\\.\\d+/\\w+[\\d-]+)", 
              au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey())),
              Pattern.CASE_INSENSITIVE);
    
    public ClockssNRCResearchArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      this.au = au;
    }
    /*
     *  NRC Research Press (an atypon publisher) has an abstract, html, pdf and pdfplus
     */
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.debug("createArticleFiles("+url+")");
      /*
      Matcher mat = ABS_PATTERN.matcher(url);
      if (mat.find()) {
        return processAbstractHtml(cu, mat);
      }
      */
      Matcher pmat = PDF_PATTERN.matcher(url);
      if (pmat.find()){
        return processPdf(cu, pmat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processAbstractHtml(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      log.debug("processAbstractHtml("+cu+")");
      
      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
      af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
      return af;
    }
    protected void guessFullHtml(ArticleFiles af, Matcher mat) {
      CachedUrl fullCu = au.makeCachedUrl(mat.replaceFirst("$1full$2"));
      log.debug("guessFullHtml("+fullCu+")");

      if (fullCu != null && fullCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, fullCu);
      }
      AuUtil.safeRelease(fullCu);
    }
    
    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("$1abs$2"));
      log.debug("guessAbstract("+absCu+")");

      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);
      }
      AuUtil.safeRelease(absCu);
    }
    protected void guessPdfPlus(ArticleFiles af, Matcher mat) {
      CachedUrl cu = au.makeCachedUrl(mat.replaceFirst("$1pdfplus$2"));
      log.debug("guessPdfPlus("+cu+")");

      if (cu != null && cu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, cu);
        af.setRoleCu(AtyponArticleFiles.ROLE_FULL_TEXT_PDFPLUS, cu);
      } 
      AuUtil.safeRelease(cu);
    }    
    protected ArticleFiles processPdf(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      log.debug("processPdf("+cu+")");
      af.setFullTextCu(cu);
      // roles only need to be set for getting metadata
      if (spec.getTarget() != null && !(spec.getTarget().isArticle())){
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);
        guessAbstract(af, mat);
        guessFullHtml(af, mat);
        guessPdfPlus(af, mat);
     }
      
      return af;
    }
    
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
              throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
          
  }
  // maybe this is a silly way to do this, when just a string will do...
  // this reminds me that perhaps will add this new role to ArticleFiles, if
  // more publishers start using it.
  protected class AtyponArticleFiles extends ArticleFiles {
    public static final String ROLE_FULL_TEXT_PDFPLUS = "FullTextPdfPlusfile";
     
  }
}