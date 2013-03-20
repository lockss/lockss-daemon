/*
 * $Id: SiamArticleIteratorFactory.java,v 1.1 2013-03-20 17:56:47 alexandraohlson Exp $
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

package org.lockss.plugin.siam;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.util.Logger;

public class SiamArticleIteratorFactory 
implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("SiamArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%sdoi/\", base_url";

  // trap both abstract and pdf but careful to only use abstract as first choice and not double count
  protected static final String PATTERN_TEMPLATE = "\"^%sdoi/(abs|pdf)/[.0-9]+/\", base_url";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    return new SiamArticleIterator(au, new SubTreeArticleIterator.Spec()
				                      .setTarget(target)
				                      .setRootTemplate(ROOT_TEMPLATE)
				                      .setPatternTemplate(PATTERN_TEMPLATE));
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ABSTRACT);
  }

  protected static class SiamArticleIterator
    extends SubTreeArticleIterator {

    protected static Pattern ABSTRACT_PATTERN =
      Pattern.compile("/doi/abs/([.0-9]+/.+)$", Pattern.CASE_INSENSITIVE);
    protected static String ABSTRACT_REPLACEMENT = "/doi/abs/$1";
    
    protected static Pattern PDF_PATTERN = 
        Pattern.compile("/doi/pdf/([.0-9]+/.+)$", Pattern.CASE_INSENSITIVE);
    protected static String PDF_REPLACEMENT = "/doi/pdf/$1";
    
    protected static String REF_REPLACEMENT = "/doi/ref/$1";

    protected SiamArticleIterator(ArchivalUnit au, SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }

    
    /*
     * This publisher seems to have article in 3 formats - abstract (html); abstract with references and pdf
     * So the "article" count and full_text_cu would be the PDF
     * but the metadata is pulled from the meta tags in the abstract (or ref)
     * @see org.lockss.plugin.SubTreeArticleIterator#createArticleFiles(org.lockss.plugin.CachedUrl)
     */
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {

      boolean articleTarget = false; /* are we just counting articles? or getting metadata? */
      ArticleFiles af = null;
      Matcher mat;
      
      log.debug3("entering SIAMcreateArticlefiles");
      String url = cu.getUrl();
      log.debug3("article url?: " + url);
      
      if ( (spec.getTarget() != null) && (spec.getTarget().isArticle())) {
        articleTarget = true;
      }

      mat = ABSTRACT_PATTERN.matcher(url);
      if (mat.find()) {
        af =  processAbstract(cu, mat);
      } else {
        /* no abstract - wierd - do we have a pdf? */
        mat = PDF_PATTERN.matcher(url);
        if (mat.find()) {
          af = processFullTextPDF(cu,mat);
        }
      }
      
      /* we have an article and we need to collect metadata */
      if ((af != null) && !articleTarget) {
        guessAdditionalFiles(af, mat);
      }
      return af;
    }

  /* abstract contains metadata information, but not full text though it represents the article*/
    protected ArticleFiles processAbstract(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();


      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
      af.setRole(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
      guessFullTextPdf(af, mat);

      return af;
    }
    
    /* 
     * This method is only called if there was an abstract and now we want to guess the pdf
     */
    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst(PDF_REPLACEMENT));
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setFullTextCu(pdfCu); /* the only full text representation of the article */
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
    }

    /* this method is called only if the original pattern caught a PDF */
    protected ArticleFiles processFullTextPDF(CachedUrl cu, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst(ABSTRACT_REPLACEMENT));

      /* If an abstract also exists, we will not create an articlefiles, because it will */
      if (absCu != null && absCu.hasContent()) {
        log.debug3("PDF found but deferring to abstract" + absCu.getUrl());
        return null;
      }
      /* no abstract, we're the representation of the article; no metadata available */
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);
      return af;
    }
    
    protected void guessAdditionalFiles(ArticleFiles af, Matcher mat) {
      CachedUrl refCu = au.makeCachedUrl(mat.replaceFirst(REF_REPLACEMENT));

      /*
       * The refCu is just an abstract with full reference information
       * it also happens to have metadata if there was no abstract (unlikely)
       */
      if (refCu != null && refCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_REFERENCES, refCu);
        if (af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA) == null) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, refCu);
        }
      }
      AuUtil.safeRelease(refCu);
    }
  }

}
