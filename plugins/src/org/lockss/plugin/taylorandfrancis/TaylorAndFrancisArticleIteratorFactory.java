/*
 * $Id: TaylorAndFrancisArticleIteratorFactory.java,v 1.4 2013-03-14 19:32:16 alexandraohlson Exp $
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

package org.lockss.plugin.taylorandfrancis;

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
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.util.Logger;


public class TaylorAndFrancisArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("TaylorAndFrancisArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%sdoi\", base_url"; // params from tdb file corresponding to AU
  // base_url/doi/

  protected static final String PATTERN_TEMPLATE = "\"^%sdoi/(full|pdf)/\", base_url";


  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new TaylorAndFrancisArticleIterator(au,
                                         new SubTreeArticleIterator.Spec()
                                             .setTarget(target)
                                             .setRootTemplate(ROOT_TEMPLATE)
                                             .setPatternTemplate(PATTERN_TEMPLATE),
                                         target);
  }

  protected static class TaylorAndFrancisArticleIterator extends SubTreeArticleIterator {

    /*
     * The content lives under a DOI: eg. doi/full/10.1080/blahblah
     *   but should not be any lower than that. So check for this....
     */
    protected Pattern HTML_PATTERN = Pattern.compile("/doi/full/([0-9.]*/[^/]*)$", Pattern.CASE_INSENSITIVE);

    protected Pattern PDF_PATTERN = Pattern.compile("/doi/pdf/([0-9.]*/[^/]*)$", Pattern.CASE_INSENSITIVE);

    protected MetadataTarget target;

    public TaylorAndFrancisArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec,
                                     MetadataTarget target /* unnecessary, see below */) {
      super(au, spec);
      this.target = target; // Unnecessary: access via spec.getTarget() visible from parent class
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
      if (target != MetadataTarget.Article) {
        guessFullTextPdf(af, htmlMat);
        guessAbstract(af, htmlMat);
        guessReferences(af, htmlMat);
      }
      return af;
    }

    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      CachedUrl htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/doi/full/$1"));
      if (htmlCu != null && htmlCu.hasContent()) {
        return null;
      }
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      if (target != MetadataTarget.Article) {
        guessAbstract(af, pdfMat);
        guessReferences(af, pdfMat);
      }
      return af;
    }

    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/doi/pdf/$1"));
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
    }

    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/doi/abs/$1"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
//      If there is no abstract it will default to whatever the FULL_TEXT_CU is (probably full html)
        if (af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA) == null) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);
        }
      }
    }

    protected void guessReferences(ArticleFiles af, Matcher mat) {
      CachedUrl refCu = au.makeCachedUrl(mat.replaceFirst("/doi/ref/$1"));
      if (refCu != null && refCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_REFERENCES, refCu);
      }
    }
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    // Ask Phil how to talk to our real metadata extractor here
  }

}
