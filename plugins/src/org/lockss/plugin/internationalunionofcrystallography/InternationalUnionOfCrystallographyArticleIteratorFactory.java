/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.internationalunionofcrystallography;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class InternationalUnionOfCrystallographyArticleIteratorFactory 
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("InternationalUnionOfCrystallographyArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%s%s/issues/%d/%s/00/\", base_url, journal_id, year, issue";
  
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/issues/%d/%s/00/[^/]+/index\\.html$\", base_url, journal_id, year, issue";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new InternationalUnionOfCrystallographyArticleIterator(au, new SubTreeArticleIterator.Spec()
                                                                      .setTarget(target)
                                                                      .setRootTemplate(ROOT_TEMPLATE)
                                                                      .setPatternTemplate(PATTERN_TEMPLATE));
  }
  
  protected static class InternationalUnionOfCrystallographyArticleIterator extends SubTreeArticleIterator {

    protected static Pattern PATTERN = Pattern.compile("/([^/]+)/index\\.html$", Pattern.CASE_INSENSITIVE);
    
    public InternationalUnionOfCrystallographyArticleIterator(ArchivalUnit au,
                                                              SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat = PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextHtml(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles processFullTextHtml(CachedUrl htmlCu, Matcher htmlMat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(htmlCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
      af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, htmlCu);
      guessFullTextPdf(af, htmlMat);
      guessAbstract(af, htmlMat);
      guessSupplementaryMaterials(af, htmlMat);
      return af;
    }

    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/$1/$1.pdf"));
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
    }
    
    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/$1/$1abs.html"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
      }
    }
    
    protected void guessSupplementaryMaterials(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/$1/$1sup0.html"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, absCu);
      }
    }
    
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(
      MetadataTarget target) throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
