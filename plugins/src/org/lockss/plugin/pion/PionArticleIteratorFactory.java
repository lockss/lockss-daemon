/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pion;

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
 * The Pion Article Iterator supports ClockssPionPlugin: 
  Full text CU: http://www.envplan.com/epd/editorials/d12107.pdf
  Abstract:     http://www.envplan.com/abstract.cgi?id=d12107
  Metadata:     http://www.envplan.com/ris.cgi?id=d12107
  Citation_Ris: http://www.envplan.com/ris.cgi?id=d12107
  PdfFile:      http://www.envplan.com/epd/editorials/d12107.pdf
  References:  http://www.envplan.com/ref.cgi?id=d12107
  There are instances of abstracts only (no pdf or RIS file - in that case,
  default to metadata in the abstract)
 */

public class PionArticleIteratorFactory 
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(PionArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%s%s/\", base_url, journal_code";
  
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/(editorials|fulltext/%s0?%s)/[^/]+\\.pdf$\", base_url, journal_code, short_journal_code, volume_name";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new PionArticleIterator(au, new SubTreeArticleIterator.Spec()
                                       .setTarget(target)
                                       .setRootTemplate(ROOT_TEMPLATE)
                                       .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class PionArticleIterator extends SubTreeArticleIterator {
    
    protected static Pattern PATTERN = Pattern.compile("(/[^/]+/(editorials|fulltext/[^/]+)/)([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
    private static CachedUrl metadataCu = null;

    protected PionArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat = PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextPdf(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      metadataCu = null;
      if(spec.getTarget() != MetadataTarget.Article()) {
        guessAbstract(af, pdfMat);
        guessReferences(af, pdfMat);
        guessRisCitation(af, pdfMat);
        guessSupplementaryMaterials(af, pdfMat);
        log.debug("setting role_article_metadata to: "+ metadataCu);
        if (metadataCu != null) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, metadataCu);
        }
      }
      return af;
    }
    
    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/abstract.cgi?id=$3"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        metadataCu = absCu;
      }
    }
    
    protected void guessReferences(ArticleFiles af, Matcher mat) {
      CachedUrl refCu = au.makeCachedUrl(mat.replaceFirst("/ref.cgi?id=$3"));
      if (refCu != null && refCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_REFERENCES, refCu);
      }
    }
    
    protected void guessRisCitation(ArticleFiles af, Matcher mat) {
      CachedUrl risCu = au.makeCachedUrl(mat.replaceFirst("/ris.cgi?id=$3"));
      if (risCu != null && risCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_CITATION + "_" + "Ris", risCu);
        metadataCu = risCu;
      }
    }
    
    protected void guessSupplementaryMaterials(ArticleFiles af, Matcher mat) {
      CachedUrl suppCu = au.makeCachedUrl(mat.replaceFirst("/misc.cgi?id=$3"));
      if (suppCu != null && suppCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, suppCu);
      }
    }
    
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }


}
