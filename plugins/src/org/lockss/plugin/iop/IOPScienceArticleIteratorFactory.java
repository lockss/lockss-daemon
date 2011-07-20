/*
 * $Id: IOPScienceArticleIteratorFactory.java,v 1.4 2011-07-20 23:40:33 thib_gc Exp $
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

package org.lockss.plugin.iop;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class IOPScienceArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("IOPScienceArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%s%s/%s\", base_url, journal_issn, volume_name";
  
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/%s/[^/]+/[^/]+/(fulltext|pdf/[^/]+\\.pdf)$\", base_url, journal_issn, volume_name";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new IOPScienceArticleIterator(au,
                                         new SubTreeArticleIterator.Spec()
                                             .setTarget(target)
                                             .setRootTemplate(ROOT_TEMPLATE)
                                             .setPatternTemplate(PATTERN_TEMPLATE),
                                         target);
  }

  protected static class IOPScienceArticleIterator extends SubTreeArticleIterator {

    protected Pattern HTML_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/([^/]+)/fulltext$", Pattern.CASE_INSENSITIVE);
    
    protected Pattern PDF_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/([^/]+)/pdf/[^/]+\\.pdf$", Pattern.CASE_INSENSITIVE);
    
    protected MetadataTarget target;
    
    public IOPScienceArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec,
                                     MetadataTarget target) {
      super(au, spec);
      this.target = target;
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
        guessSupplementaryMaterials(af, htmlMat);
      }
      return af;
    }
    
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      CachedUrl htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/$1/$2/$3/$4/fulltext"));
      if (htmlCu != null && htmlCu.hasContent()) {
        return null;
      }
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      if (target != MetadataTarget.Article) {
        guessAbstract(af, pdfMat);
        guessReferences(af, pdfMat);
        guessSupplementaryMaterials(af, pdfMat);
      }
      return af;
    }
    
    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
      CachedUrl htmlCu = au.makeCachedUrl(mat.replaceFirst("/$1/$2/$3/$4/pdf/$1_$2_$3_$4.pdf"));
      if (htmlCu != null && htmlCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, htmlCu);
      }
    }

    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/$1"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
      }
    }

    protected void guessReferences(ArticleFiles af, Matcher mat) {
      CachedUrl refCu = au.makeCachedUrl(mat.replaceFirst("/$1/$2/$3/$4/refs"));
      if (refCu != null && refCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_REFERENCES, refCu);
      }
    }

    protected void guessSupplementaryMaterials(ArticleFiles af, Matcher mat) {
      CachedUrl suppCu = au.makeCachedUrl(mat.replaceFirst("/$1/$2/$3/$4/media"));
      if (suppCu != null && suppCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, suppCu);
      }
    }

  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }

}
