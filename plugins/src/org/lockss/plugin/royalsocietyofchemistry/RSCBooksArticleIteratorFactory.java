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
  
  protected static final String ROOT_TEMPLATE = "\"%sen/content/\", base_url";
  protected static final String PATTERN_TEMPLATE = "\"/chapter/[-a-z0-9]+/[-0-9x]+\"";
  
  /*
   * various aspects of a book chapter
   * "^%sen/content/chapter/[-a-z0-9]+/[-0-9x]+", base_url
   * "^%sen/content/chapterpdf/[0-9]{4}/[-a-z0-9]+\?isbn=[-0-9x]+", base_url
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
