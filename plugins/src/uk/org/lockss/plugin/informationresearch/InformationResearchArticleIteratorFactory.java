/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package uk.org.lockss.plugin.informationresearch;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class InformationResearchArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(InformationResearchArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%sdoi\", base_url"; // params from tdb file corresponding to AU
  // base_url/doi/
  
  protected static final String PATTERN_TEMPLATE = "\"^%sdoi/(full|pdf)/\", base_url";

  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new InformationResearchArticleIterator(au,
                                         new SubTreeArticleIterator.Spec()
                                             .setTarget(target)
                                             .setRootTemplate(ROOT_TEMPLATE)
                                             .setPatternTemplate(PATTERN_TEMPLATE),
                                         target);
  }

  protected static class InformationResearchArticleIterator extends SubTreeArticleIterator {

    protected Pattern HTML_PATTERN = Pattern.compile("/doi/full/(.*)$", Pattern.CASE_INSENSITIVE);
    
    protected Pattern PDF_PATTERN = Pattern.compile("/doi/pdf/(.*)$", Pattern.CASE_INSENSITIVE);
    
    protected MetadataTarget target;
    
    public InformationResearchArticleIterator(ArchivalUnit au,
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
    return new BaseArticleMetadataExtractor(null);
    // Ask Phil how to talk to our real metadata extractor here
  }

}
