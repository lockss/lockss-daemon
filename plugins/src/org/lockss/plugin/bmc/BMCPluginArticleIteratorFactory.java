/*
 * $Id$
 */ 

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.Logger;

/* 
 * 6/11/15 - adding support for supplement issues which have a slightly different
 * url pattern for the underlying articles. 
 * - The existing iterator assumes that every article has a PDF. Leaving this
 *   assumption in place for now.
 * - The existing iterator does not use the Builder. For now I'm leaving it this
 *   way because the supplement articles have an additional variable that would 
 *   make automatic substitution challenging.
 * - Removing the html matching section in createArticleFiles because the iterator
 *   was limited to pdfs 
 */

public class BMCPluginArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(BMCPluginArticleIteratorFactory.class);
  //http://www.biomedcentral.com/content/pdf[/1472-6831-12-60.pdf]
  //supplement issue articles have additional level (supplement S1, abstract A12)
  //http://www.biomedcentral.com/content/pdf/1471-2253-14-S1-A12.pdf
  protected static final String PDF_ROOT_TEMPLATE = "\"%scontent/pdf\", base_url";

  protected static final String PDF_PATTERN_TEMPLATE = "\"%s-%s-[\\dA-Z-]+\\.pdf$\", journal_issn,volume_name,";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new BMCPluginArticleIterator(au, new SubTreeArticleIterator.Spec()
                                              .setTarget(target)
                                              .setRootTemplate(PDF_ROOT_TEMPLATE)
                                              .setPatternTemplate(PDF_PATTERN_TEMPLATE,Pattern.CASE_INSENSITIVE));
  }
  
    protected static class BMCPluginArticleIterator extends SubTreeArticleIterator {
    //http://www.biomedcentral.com/content/pdf/1472-6831-12-60.pdf
    //http://www.biomedcentral.com/content/pdf/1471-2253-14-S1-A12.pdf
    protected static final Pattern PDF_PATTERN = Pattern.compile("/content/pdf/([\\d]+-[\\dXx]+)-([\\d]+)-([\\dS]+)(-([\\dA-Z]+))?\\.pdf$", Pattern.CASE_INSENSITIVE);

    public BMCPluginArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.debug3("Entry point: " + url);
      
      Matcher mat = PDF_PATTERN.matcher(url);
      if (mat.find()) {
        log.debug3("found a full text pdf: ");
        return processFullTextPdf(cu, mat);
      }
 
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF,pdfCu);
      if (spec.getTarget() != MetadataTarget.Article()) {
        guessAbstract(af, pdfMat);
        guessHtml(af, pdfMat);
       }
      return af;
    }
  
    // abstract (if there): 
    //http://www.biomedcentral.com/1472-6831/12/60/abstract
    //or, for supplements
    //http://www.biomedcentral.com/1471-2253/14/S1/A14/abstract
    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu;
      if (mat.group(5) != null) {
        // supplement article has extra level
        absCu = au.makeCachedUrl(mat.replaceFirst("/$1/$2/$3/$5/abstract"));
      } else {
        absCu = au.makeCachedUrl(mat.replaceFirst("/$1/$2/$3/abstract"));
      }
      log.debug3("guessing abstract: "+absCu);
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);
        AuUtil.safeRelease(absCu);
      }
    }
    
    // html: 
    protected void guessHtml(ArticleFiles af, Matcher mat) {
      CachedUrl htmlCu;
      if (mat.group(5) != null) {
        // supplement article has extra level
        htmlCu = au.makeCachedUrl(mat.replaceFirst("/$1/$2/$3/$5"));
      } else {
        htmlCu = au.makeCachedUrl(mat.replaceFirst("/$1/$2/$3"));
      }
      log.debug3("guessing html: "+htmlCu);
      if (htmlCu != null && htmlCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
        //we try for abstract first, but if not make this metadata
        if (af.getRole(ArticleFiles.ROLE_ARTICLE_METADATA) == null) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, htmlCu);
        }
        AuUtil.safeRelease(htmlCu);
      }
    }  
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
