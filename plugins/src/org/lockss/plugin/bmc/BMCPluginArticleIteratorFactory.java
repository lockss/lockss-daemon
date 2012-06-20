/*
 * $Id: BMCPluginArticleIteratorFactory.java,v 1.1.6.2 2012-06-20 00:02:58 nchondros Exp $
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

package org.lockss.plugin.bmc;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;

public class BMCPluginArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("BMCPluginArticleIteratorFactory");
  //http://www.biomedcentral.com/content/pdf/1471-2253-1-2.pdf
  protected static final String PDF_ROOT_TEMPLATE = "\"%scontent/pdf\", base_url";
  //For example: http://www.biomedcentral.com/1471-2253/1/2
  protected static final String HTML_ROOT_TEMPLATE ="\"%s%s/%s\", base_url, journal_issn, volume_name";
  protected static final String PATTERN_TEMPLATE = "\"^%s(%s/%s/[^/]+|content/pdf/%s-%s-[^/]+\\.pdf)$\", base_url, journal_issn,volume_name,journal_issn,volume_name";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new BMCPluginArticleIterator(au, new SubTreeArticleIterator.Spec()
                                              .setTarget(target)
                                              //.setRootTemplates(ListUtil.list(HTML_ROOT_TEMPLATE,PDF_ROOT_TEMPLATE))
                                             // .setRootTemplate(HTML_ROOT_TEMPLATE)
                                              .setPatternTemplate(PATTERN_TEMPLATE,Pattern.CASE_INSENSITIVE));
  }
  
    protected static class BMCPluginArticleIterator extends SubTreeArticleIterator {

    protected static final Pattern PDF_PATTERN = Pattern.compile("/content/pdf/([^/]+)-([^/]+)-([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);

    protected static final Pattern HTML_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);

    public BMCPluginArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.debug3("Entry point: " + url);
      
      // Caution: the PDF pattern is a particular case of the HTML pattern
      
      Matcher mat = PDF_PATTERN.matcher(url);
      if (mat.find()) {
        log.debug3("found a full text pdf: ");
        return processFullTextPdf(cu, mat);
      }
      
      mat = HTML_PATTERN.matcher(url);
      if (mat.find()) {
        log.debug3("found a full text html: ");
        return processFullTextHtml(cu, mat);
      }
      
      
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullTextHtml(CachedUrl htmlCu, Matcher htmlMat) {
       
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(htmlCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
      if (spec.getTarget() != MetadataTarget.Article) {
        guessFullTextPdf(af, htmlMat);
        guessAbstract(af, htmlMat);
      
      }
      return af;
    }

    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      CachedUrl htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/$1/$2/$3"));
      if (htmlCu != null && htmlCu.hasContent()) {
        AuUtil.safeRelease(htmlCu);
        return null;
      }
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF,pdfCu);
      if (spec.getTarget() != MetadataTarget.Article) {
        guessAbstract(af, pdfMat);
       }
      return af;
    }

  
    
    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/content/pdf/$1-$2-$3.pdf"));
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
        AuUtil.safeRelease(pdfCu);
      }
    }
    
    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/$1/$2/$3/abstract"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        AuUtil.safeRelease(absCu);
      }
    }
    
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }
  
}
