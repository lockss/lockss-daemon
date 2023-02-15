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

package org.lockss.plugin.sjdm;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class JudgmentAndDecisionMakingArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  public static class JudgmentAndDecisionMakingArticleIterator extends SubTreeArticleIterator {

    protected static final String PATTERN_TEMPLATE = "\"^%s.*\\.pdf$\", base_url";
    
    protected static final Pattern SAME_LEVEL_PATTERN = Pattern.compile("/([^/]+)/(jdm)?\\1\\.pdf$", Pattern.CASE_INSENSITIVE);
    
    protected static final Pattern DIFFERENT_LEVEL_PATTERN = Pattern.compile("/(jdm)?([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
    
    public JudgmentAndDecisionMakingArticleIterator(ArchivalUnit au,
                                                     MetadataTarget target) {
      super(au,
            new SubTreeArticleIterator.Spec().setTarget(target)
                                             .setPatternTemplate(PATTERN_TEMPLATE));
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;

      mat = SAME_LEVEL_PATTERN.matcher(url);
      if (mat.find()) {
        return processSameLevel(cu, mat);
      }

      mat = DIFFERENT_LEVEL_PATTERN.matcher(url);
      if (mat.find()) {
        return processDifferentLevel(cu, mat);
      }

      // This PDF isn't an article, it's a PDF referenced by an article
      log.debug3(String.format("Not matched in %s: %s", au.getName(), url));
      return null;
    }
    
    protected ArticleFiles processSameLevel(CachedUrl pdfCu, Matcher pdfMat) {
      ArticleFiles af = new ArticleFiles();
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      
      CachedUrl htmlCu = null;
      
      try {
        htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/$1/$2$1.html"));
        if (htmlCu == null) {
          htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/$1/$2$1.htm"));
        }
        if (htmlCu != null && htmlCu.hasContent()) {
          af.setFullTextCu(htmlCu);
          af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
        }
        else {
          af.setFullTextCu(pdfCu);
        }
      } finally {
        AuUtil.safeRelease(htmlCu);
      }

      return af;
    }

    protected ArticleFiles processDifferentLevel(CachedUrl pdfCu, Matcher pdfMat) {
      ArticleFiles af = new ArticleFiles();
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      
      CachedUrl htmlCu = null;
      
      try {
        htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/$2/$1$2.html"));
        if (htmlCu == null) {
          htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/$2/$1$2.htm"));
        }
        if (htmlCu != null && htmlCu.hasContent()) {
          af.setFullTextCu(htmlCu);
          af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
        }
        else {
          af.setFullTextCu(pdfCu);
        }
      } finally {
        AuUtil.safeRelease(htmlCu);
      }

      return af;
    }
    
  }
  
  private static final Logger log = Logger.getLogger(JudgmentAndDecisionMakingArticleIteratorFactory.class);
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                       MetadataTarget target)
      throws PluginException {
    return new JudgmentAndDecisionMakingArticleIterator(au, target);
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor();
  }

}
