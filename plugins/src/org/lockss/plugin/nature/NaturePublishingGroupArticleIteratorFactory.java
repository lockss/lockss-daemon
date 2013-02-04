/*
 * $Id: NaturePublishingGroupArticleIteratorFactory.java,v 1.12 2013-02-04 19:33:40 alexandraohlson Exp $
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

package org.lockss.plugin.nature;

import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;
import org.lockss.daemon.PluginException;

public class NaturePublishingGroupArticleIteratorFactory
    implements ArticleIteratorFactory,
	       ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger("NaturePublishingGroupArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%s%s/journal/v%s/\", base_url, journal_id, volume_name";
  
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/journal/v[^/]+/n[^/]+/(full/[^/]+\\.html|pdf/[^/]+\\.pdf)$\", base_url, journal_id, volume_name";

  /*
   * The Nature URL structure means that the HTML for an article is
   * at a URL like http://www.nature.com/gt/journal/v16/n5/full/gt200929a.html
   * ie <base_url>/<journal_id>/journal/v<volume> is the subtree we want.
   */
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    return new NaturePublishingGroupArticleIterator(au, new SubTreeArticleIterator.Spec()
                                                        .setTarget(target)
                                                        .setRootTemplate(ROOT_TEMPLATE)
                                                        .setPatternTemplate(PATTERN_TEMPLATE));
  }

  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }

  protected static class NaturePublishingGroupArticleIterator
      extends SubTreeArticleIterator {
    
    protected static Pattern HTML_PATTERN = Pattern.compile("/full/([^/]+)\\.html$", Pattern.CASE_INSENSITIVE);
    
    protected static Pattern PDF_PATTERN = Pattern.compile("/pdf/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
    
    protected static Pattern ABS_PATTERN = Pattern.compile("/abs/([^/]+)\\.html$", Pattern.CASE_INSENSITIVE);
    
    protected NaturePublishingGroupArticleIterator(ArchivalUnit au,
                                                   SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }

    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;
      ArticleFiles af = new ArticleFiles();
      boolean articleTarget = false;
      
      /* minimize the work you do if you are just counting articles */
      if ( (spec.getTarget() != null) && (spec.getTarget().isArticle())) {
        articleTarget = true;
      }
      
      mat = HTML_PATTERN.matcher(url);
      if (mat.find()) {
        if ("index".equalsIgnoreCase(mat.group(1))) {
          return null; // HTTP 404 served as HTTP 200
        }
        af = processFullTextHtml(cu, mat);
      } else {
        mat = PDF_PATTERN.matcher(url);
        if (mat.find()) {
          af = processFullTextPdf(cu, mat);
        }      
      }
      
      /* we have an article and we need to collect metadata */
      if ((af != null) && !articleTarget) {
        guessOtherParts(af, mat);
      }
      
      if (af == null) {
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      }
      return af;
    }

    /* this method only if there was .../full/<article>.html */
    protected ArticleFiles processFullTextHtml(CachedUrl htmlCu, Matcher htmlMat) {
      ArticleFiles af = new ArticleFiles();
      
      af.setFullTextCu(htmlCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
      guessFullTextPdf(af, htmlMat);
      return af;
    }
    
    /* this method if there WAS a full text html, and now checking for pdf as well */
    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/pdf/$1.pdf"));
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
    }
    
    /* this method only if there was .../pdf/<article.pdf and no .../full/<article>.html */
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      /* This check is probably unneccessary. We shouldn't get to this method
       * if the full/blah.html file exists
       */
      CachedUrl htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/full/$1.html"));
      if (htmlCu != null && htmlCu.hasContent()) {
        return null;
      }
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      return af;
    }
    
    /* this method if we have an af with full_text_cu of some sort and need metadata */
    protected void guessOtherParts(ArticleFiles af, Matcher mat) {
      guessAbstract(af, mat);
      guessFigures(af, mat);
      guessSupplementaryMaterials(af, mat);
      guessRisCitation(af, mat);
    }
    

    
    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/abs/$1.html"));
     if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        if (af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA) == null) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);
        }
      }
    }
    
    protected void guessFigures(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/fig_tab/$1_ft.html"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FIGURES_TABLES, absCu);
      }
    }
    
    protected void guessSupplementaryMaterials(ArticleFiles af, Matcher mat) {
      CachedUrl suppinfoCu = au.makeCachedUrl(mat.replaceFirst("/suppinfo/$1.html"));
      if (suppinfoCu != null && suppinfoCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, suppinfoCu);
      }
    }
    
    protected void guessRisCitation(ArticleFiles af, Matcher mat) {
      CachedUrl risCu = au.makeCachedUrl(mat.replaceFirst("/ris/$1.ris"));
      if (risCu != null && risCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_CITATION + "_" + "application/x-research-info-systems", risCu);
      }
    }
    
  }

}
