/*
 * $Id$
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
 * PDF Full Text: http://www.igi-global.com/chapter/full-text-pdf/20212
 * HTML Abstract: http://www.igi-global.com/chapter/20212
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
      "\"^%sgateway/(?:article|chapter)(?:/full-text-(?:html|pdf))?/[0-9]+\", base_url";
  
  protected static final Pattern ABSTRACT_PATTERN = Pattern.compile(
      "(article|chapter)/([0-9]+)$", Pattern.CASE_INSENSITIVE);
  protected static final String ABSTRACT_REPLACEMENT = "$1/$2";
  
  protected static final Pattern FULLTEXT_HTML_PATTERN = Pattern.compile(
      "(article|chapter)/full-text-html/([0-9]+)$", Pattern.CASE_INSENSITIVE);
  protected static final String FULLTEXT_HTML_REPLACEMENT = "$1/full-text-html/$2";
  
  protected static final Pattern FULLTEXT_PDF_PATTERN = Pattern.compile(
      "(article|chapter)/full-text-pdf/([0-9]+)$", Pattern.CASE_INSENSITIVE);
  protected static final String FULLTEXT_PDF_REPLACEMENT = "$1/full-text-pdf/$2";
  
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "<iframe[^>]* src=\"/(pdf[.]aspx?[^\"]+)\"", Pattern.CASE_INSENSITIVE);
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);
    
    builder.setSpec(target,
        Arrays.asList(JOURNAL_ROOT_TEMPLATE, BOOK_ROOT_TEMPLATE),
        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    builder.addAspect(
        FULLTEXT_HTML_PATTERN, FULLTEXT_HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        FULLTEXT_PDF_PATTERN, FULLTEXT_PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, ArticleFiles.ROLE_ARTICLE_METADATA);
    
    // set up Abstract to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means an abstract could be considered a 
    // FULL_TEXT_CU until this is deprecated
    // the abstract would only be considered FT iff there is no pdf landing nor FT html
    builder.addAspect(
        ABSTRACT_PATTERN, ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_ARTICLE_METADATA);
    
    return builder.getSubTreeArticleIterator();
  }
  
  protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) { 
    return new SubTreeArticleIteratorBuilder(au) {
      
      @Override
      protected void maybeMakeSubTreeArticleIterator() {
        if (au != null && spec != null && iterator == null) {
          this.iterator = new BuildableSubTreeArticleIterator(au, spec) {
            
            @Override
            protected ArticleFiles createArticleFiles(CachedUrl cu) {
              // Since 1.64 createArticleFiles can return ArticleFiles
              ArticleFiles af = super.createArticleFiles(cu);
              
              if (af != null && 
                  spec.getTarget() != null && !spec.getTarget().isArticle()) {
                guessPdf(af);
              }
              return af;
            }
          };
        }
      }
      
      //NOTE -  the full-text-pdf is pdf in an html frameset so it's not
      // actually a pdf file. We pick up the pdf which lives at: 
      // http://www.igi-global.com/pdf.aspx?tid=20212&ptid=464&ctid=3&t=E-Survey+Methodology
      //<iframe src="/pdf.aspx?tid%3d20212%26ptid%3d464%26ctid%3d3%26t%3dE-Survey+Methodology">
      protected void guessPdf(ArticleFiles af) {
        CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
        if (cu == null || !cu.hasContent()) {
          return;
        }
        String pdfurl = cu.getUrl();
        Matcher mat;
        mat = FULLTEXT_PDF_PATTERN.matcher(pdfurl);
        if (!mat.find()) {
          return;
        }
        BufferedReader bReader = null;
        try {
          bReader = new BufferedReader(new InputStreamReader(
              cu.getUnfilteredInputStream(), cu.getEncoding())
              );
          Matcher matcher;
          
          // go through the cached URL content line by line
          // if a match is found, look for valid url & content
          // if found then set the role for ROLE_FULL_TEXT_PDF
          for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
            matcher = PDF_PATTERN.matcher(line);
            if (matcher.find()) {
              String baseUrl = au.getConfiguration().get("base_url");
              String pdfUrl = matcher.group(1);
              // use unescapeHtml to convert &amp; to &
              CachedUrl pdfCu = au.makeCachedUrl(baseUrl + StringEscapeUtils.unescapeHtml(pdfUrl));
              // if makeCachedUrl using unescapeHtml did not work, try plain pdfUrl
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
          cu.release();
        }
      }
    };
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    // Ask Phil how to talk to our real metadata extractor here
  }

}
