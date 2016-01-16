/*
 * $Id: RSC2014ArticleIteratorFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.royalsocietyofchemistry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.regex.*;

import org.apache.commons.lang.StringEscapeUtils;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

public class RSCBooksArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(RSCBooksArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
      "\"%sen/content/\", base_url";
//    "\"%sen/content/chapterpdf/[0-9]+/\", base_url, year";
//  <string>1,"^%sen/content/chapter/[-a-z0-9]+/[-0-9x]+", base_url</string>
//  <string>1,"^%sen/content/chapterpdf/%d/[-a-z0-9]+\?isbn=[-0-9x]+", base_url, year</string>
  protected static final String PATTERN_TEMPLATE =
      "\"/chapter/[-a-z0-9]+/[-0-9x]+\"";
  
  /*
   * various aspects of a book chapter
   * 
   * 
   */
  
  // Identify groups in the pattern 
  protected static final Pattern ABSTRACT_PATTERN = Pattern.compile(
      "/chapter/([-a-z0-9]+)/([-0-9x]+)$",
      Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String ABSTRACT_REPLACEMENT = "/chapter/$1/$2";
  
  
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "(en/content/chapterpdf/[0-9]{4}/[^/?]+\\?isbn=[-0-9x]+)(?:\"| )",
      Pattern.CASE_INSENSITIVE);
  
  
  public Iterator<ArticleFiles> createArticleIterator(
      ArchivalUnit au, MetadataTarget target) throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);
    
    builder.setSpec(target, ROOT_TEMPLATE, PATTERN_TEMPLATE);
    
    // set up abstract to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means it is considered a FULL_TEXT_CU
    // until this fulltext concept is deprecated
    // NOTE: pdf will take precedence over abstract
    builder.addAspect(
        ABSTRACT_PATTERN, ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_ARTICLE_METADATA);
    
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ABSTRACT);
    
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
      
      //NOTE -  the full-text-pdf link is in an html page
      protected void guessPdf(ArticleFiles af) {
        CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_ABSTRACT);
        if (cu == null || !cu.hasContent()) {
          return;
        }
        
        BufferedReader bReader = null;
        try {
          bReader = new BufferedReader(new InputStreamReader(
              cu.getUnfilteredInputStream(), cu.getEncoding())
              );
          Matcher matcher;
          String baseUrl = au.getConfiguration().get("base_url");
          // go through the cached URL content line by line
          // if a match is found, look for valid url & content
          // if found then set the role for ROLE_FULL_TEXT_PDF
          for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
            matcher = PDF_PATTERN.matcher(line);
            if (matcher.find()) {
              
              String pdfUrl = baseUrl + matcher.group(1);
              // use unescapeHtml to convert &amp; to &
              CachedUrl pdfCu = au.makeCachedUrl(StringEscapeUtils.unescapeHtml(pdfUrl));
              // if makeCachedUrl using unescapeHtml did not work, try plain pdfUrl
              if (pdfCu == null || !pdfCu.hasContent()) {
                pdfCu = au.makeCachedUrl(pdfUrl);
              }
              
              if (pdfCu != null && pdfCu.hasContent()) {
                af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
              }
              break;
            }
          }
        } catch (Exception e) {
          // probably not serious, so warn
          log.warning(e + " : Looking for pdf link");
        }
        finally {
          IOUtil.safeClose(bReader);
          cu.release();
        }
      }
    };
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
