/*
 * $Id$
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
