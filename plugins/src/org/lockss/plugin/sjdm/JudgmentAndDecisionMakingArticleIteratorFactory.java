/*
 * $Id$
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
