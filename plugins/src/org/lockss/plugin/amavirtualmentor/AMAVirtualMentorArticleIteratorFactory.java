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

package org.lockss.plugin.amavirtualmentor;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class AMAVirtualMentorArticleIteratorFactory implements ArticleIteratorFactory {

  protected static Logger log = Logger.getLogger("AMAVirtualMentorArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%s%d/\", base_url, year";
  
  protected static final String PATTERN_TEMPLATE = "\"^%s%d/[0-9]{2}/(pdf/)?[^/]+-%02d[0-9]{2}\\.(html|pdf)$\", base_url, year, au_short_year";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new AMAVirtualMentorArticleIterator(au, new SubTreeArticleIterator.Spec()
                                                   .setTarget(target)
                                                   .setRootTemplate(ROOT_TEMPLATE)
                                                   .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class AMAVirtualMentorArticleIterator extends SubTreeArticleIterator {
    
    protected static Pattern HTML_PATTERN = Pattern.compile("/([0-9]{2})/(([^/]+)-[0-9]{4})\\.html$", Pattern.CASE_INSENSITIVE);
    
    protected static Pattern PDF_PATTERN = Pattern.compile("/([0-9]{2})/pdf/(([^/]+)-[0-9]{4})\\.pdf$", Pattern.CASE_INSENSITIVE);
    
    protected AMAVirtualMentorArticleIterator(ArchivalUnit au,
                                              SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }

    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;
      
      mat = HTML_PATTERN.matcher(url);
      if (mat.find()) {
        if ("toc".equalsIgnoreCase(mat.group(3))) {
          return null; // Skip table of contents
        }
        return processFullTextHtml(cu, mat);
      }
        
      mat = PDF_PATTERN.matcher(url);
      if (mat.find()) {
        if ("vm".equalsIgnoreCase(mat.group(3))) {
          return null; // Skip full-issue PDF
        }
        return processFullTextPdf(cu, mat);
      }
      
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullTextHtml(CachedUrl htmlCu, Matcher htmlMat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(htmlCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
//      guessFullTextPdf(af, htmlMat);
      return af;
    }
    
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      CachedUrl htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/$1/$2.html"));
      if (htmlCu != null && htmlCu.hasContent()) {
        return null;
      }
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      return af;
    }
    
//    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
//      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/$1/pdf/$2.pdf"));
//      if (pdfCu != null && pdfCu.hasContent()) {
//        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
//      }
//    }
    
  }

  public ArticleMetadataExtractor
  createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
  return new BaseArticleMetadataExtractor(null);
}

}
