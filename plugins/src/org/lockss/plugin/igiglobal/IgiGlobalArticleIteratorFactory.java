/*
 * $Id: IgiGlobalArticleIteratorFactory.java,v 1.11 2013-10-09 23:14:21 etenbrink Exp $
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

package org.lockss.plugin.igiglobal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;


import org.apache.commons.lang.StringEscapeUtils;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

/*
 * For journals:
 * PDF Full Text: http://www.igi-global.com/article/full-text-pdf/56564
 * HTML Abstract: http://www.igi-global.com/article/56564
 * For books:
 * PDF Full Text: http://www.igi-global.com/chapter/20212
 * HTML Abstract: http://www.igi-global.com/chapter/full-text-pdf/20212
 */

public class IgiGlobalArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(IgiGlobalArticleIteratorFactory.class);
  
  protected static final String JOURNAL_ROOT_TEMPLATE =
      "\"%sgateway/article/\", base_url"; // params from tdb file corresponding to AU
  protected static final String BOOK_ROOT_TEMPLATE =
      "\"%sgateway/chapter/\", base_url"; // params from tdb file corresponding to AU

  protected static final String PATTERN_TEMPLATE =
      "\"^%sgateway/(article|chapter)/[0-9]+\", base_url";

  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new IgiGlobalArticleIterator(au,
        new SubTreeArticleIterator.Spec()
            .setTarget(target)
            .setRootTemplates(Arrays.asList(JOURNAL_ROOT_TEMPLATE, BOOK_ROOT_TEMPLATE))
            .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class IgiGlobalArticleIterator extends SubTreeArticleIterator {

    protected Pattern ABSTRACT_PATTERN = Pattern.compile(
        "(article|chapter)/([0-9]+)$", Pattern.CASE_INSENSITIVE);
    
    public IgiGlobalArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;
      mat = ABSTRACT_PATTERN.matcher(url);
      if (mat.find()) {
        return processAbstract(cu, mat);
      }

      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    

    protected ArticleFiles processAbstract(CachedUrl absCu, Matcher absMat) {
      ArticleFiles af = new ArticleFiles();
      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
      af.setFullTextCu(absCu);
      
      // minimize the work you do if you are just counting articles
      if ((spec.getTarget() == null) || spec.getTarget().isAny()) {
        guessPdf(af, absMat);
      }
      guessFullText(af, absMat);
      
      return af;
    }
    
    protected void guessFullText(ArticleFiles af, Matcher mat) {
      String htmlUrlBase = mat.replaceFirst("$1/full-text-html/$2");
      CachedUrl htmlCu = au.makeCachedUrl(htmlUrlBase);
      if (htmlCu != null && htmlCu.hasContent()) {
        af.setFullTextCu(htmlCu);
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
      } 
    }
    
    //NOTE -  the full-text-pdf is pdf in an html frameset so it's not
    // actually a pdf file. 
    // we pick up the pdf which lives at: 
    // http://www.igi-global.com/pdf.aspx?tid=20212&ptid=464&ctid=3&t=E-Survey+Methodology
    //<iframe src="/pdf.aspx?tid%3d20212%26ptid%3d464%26ctid%3d3%26t%3dE-Survey+Methodology">
    protected void guessPdf(ArticleFiles af, Matcher mat) {
      String pdfUrlBase = mat.replaceFirst("$1/full-text-pdf/$2");
      CachedUrl pdfCu = au.makeCachedUrl(pdfUrlBase);
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setFullTextCu(pdfCu);
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, pdfCu);
        BufferedReader bReader = null;
        try {
          bReader = new BufferedReader(new InputStreamReader(
              pdfCu.getUnfilteredInputStream(), pdfCu.getEncoding())
              );
          Matcher matcher;
          Pattern patternPdf = Pattern.compile(
              "<iframe[^>]* src=\"/(pdf.aspx?[^\"]+)\"", Pattern.CASE_INSENSITIVE);
          
          // go through the cached URL content line by line
          for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
            matcher = patternPdf.matcher(line);
            if (matcher.find()) {
              String baseUrl = au.getConfiguration().get("base_url");
              String pdfUrl = matcher.group(1);
              // use unescapeHtml to convert &amp; to &
              pdfCu = au.makeCachedUrl(baseUrl + StringEscapeUtils.unescapeHtml(pdfUrl));
              if (pdfCu == null || !pdfCu.hasContent()) {
                pdfCu = au.makeCachedUrl(baseUrl + pdfUrl);
              }
              if (pdfCu != null && pdfCu.hasContent()) {
                af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
              }
              break;
            }
          }
        } catch (Exception e) {
          // probably not serious, so warn
          log.warning(e + " : Looking for /pdf.aspx");
        }
        finally {
          IOUtil.safeClose(bReader);
        }
      }
    }
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ABSTRACT);
    // Ask Phil how to talk to our real metadata extractor here
  }

}
