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

package org.lockss.plugin.highwire.sepm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

public class SepmBooksH20ArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  
  // pulled from the html and used to create a ris URL...
  // <span title="10.2110/sepmsp.104.06" class="slug-doi">10.2110/sepmsp.104.06</span>
  // try to be independent of the order of title/class attributes and just get text
  protected static final Pattern DOI_PATTERN =
      Pattern.compile(
          " class=\"slug-doi\"[^>]*>\\s*(10\\.[0-9]+)/([^ <]+)",
          Pattern.CASE_INSENSITIVE);
  
  private static final Logger log =
    Logger.getLogger(SepmBooksH20ArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%scontent/%s/\", base_url, volume_name";
  
  protected static final String PATTERN_TEMPLATE =
    "\"^%scontent/%s/((?:[^/]+/)?[^/]+)(?:.*[.]body|[.]full)(?:[.]pdf)?|[.](abstract|short|citation)$\", " +
    "base_url, volume_name";

  // I have not yet seen any full text html (".body") for books on this site yet
  // but for completeness leave it in
  protected static final Pattern HTML_PATTERN = Pattern.compile(
      "/([^/]+)[.](?:full|body)$", Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "/([^/]+)[.]body[.]pdf$", Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern ABSTRACT_PATTERN = Pattern.compile(
      "/([^/]+)[.](?:abstract|extract)$", Pattern.CASE_INSENSITIVE);
  protected static final Pattern SHORT_PATTERN = Pattern.compile(
      "/([^/]+)[.](?:short)$", Pattern.CASE_INSENSITIVE);
  protected static final Pattern CITATION_PATTERN = Pattern.compile(
      "/([^/]+)[.](?:citation)$", Pattern.CASE_INSENSITIVE);
  
  
  // how to change from one form (aspect) of article to another
  protected static final String HTML_REPLACEMENT = "/$1.full";
  protected static final String PDF_REPLACEMENT = "/$1.body.pdf";
  protected static final String PDF_LANDING_REPLACEMENT = "/$1.body.pdf+html";
  protected static final String ABSTRACT_REPLACEMENT = "/$1.abstract";
  protected static final String EXTRACT_REPLACEMENT = "/$1.extract";
  protected static final String CITATION_REPLACEMENT = "/$1.citation";
  protected static final String SHORT_REPLACEMENT = "/$1.short";
  protected static final String BODY_REPLACEMENT = "/$1.body";
  protected static final String FIGURES_REPLACEMENT = "/$1.figures-only";
  protected static final String SUPPL_REPLACEMENT = "/$1/suppl/DC1";
  
  //For SEPM books, the metadata lives at:
  // For chapter....
  //http://scnotes.sepmonline.org/content/sepsc054/1/SEC1.abstract
  // ris citation is:
  //http://scnotes.sepmonline.org/citmgr?type=procite&gcadoi=10.2110%2Fsepmscn.054.001
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    // Create a local builder in order to pull the doi (needed for RIS file) from the html
    SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up full or full.pdf to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means full is considered a FULL_TEXT_CU 
    // until this is deprecated
    builder.addAspect(HTML_PATTERN, Arrays.asList(
        BODY_REPLACEMENT, HTML_REPLACEMENT),
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up pdf landing page to be an aspect
    builder.addAspect(
        PDF_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    // set up abstract/extract to be an aspect
    builder.addAspect(ABSTRACT_PATTERN, Arrays.asList(
        ABSTRACT_REPLACEMENT, EXTRACT_REPLACEMENT),
        ArticleFiles.ROLE_ABSTRACT);
    
    // set up short as another option for abstract
    builder.addAspect(SHORT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);
    // set up citation as another option for abstract
    builder.addAspect(CITATION_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);
    
    // set up figures-only to be an aspect
    builder.addAspect(FIGURES_REPLACEMENT,
        ArticleFiles.ROLE_FIGURES);
    
    // set up suppl to be an aspect
    builder.addAspect(SUPPL_REPLACEMENT,
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
    
    // books don't have much in the meta tags,
    // and if we can guess at the RIS url (guessRis) then we'll use that, but
    // if not, it will use what it can from one of these and
    // get the rest from the TDB file
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, Arrays.asList(
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_FULL_TEXT_HTML));
    
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF, 
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ABSTRACT);
    
    return builder.getSubTreeArticleIterator();
  }
  
  /*
   * This local builder creator allows parsing of the html to pull out the doi
   * needed to pick up the RIS file.
   */
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
                guessRis(af);
              }
              return af;
            }
          };
        }
      }
      
      //NOTE -  the RIS url is based on the doi not the same artilce name
      // as is used for abstract, full & pdf
      //eg - 
      //http://scnotes.sepmonline.org/content/sepsc054/1/SEC1.abstract
      // ris citation is:
      //http://scnotes.sepmonline.org/citmgr?type=refman&gcadoi=10.2110%2Fsepmscn.054.001
      // but the DOI is available from the landing page html
      protected void guessRis(ArticleFiles af) {
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
          // if found then set the role for ROLE_ARTICLE_METADATA
          for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
            matcher = DOI_PATTERN.matcher(line);
            if (matcher.find()) {
              
              // for this url the DOI "/" is encoded as "%2F"
              String risUrl = baseUrl + "citmgr?type=procite&gcadoi=" + matcher.group(1) + "%2F" + matcher.group(2);
              CachedUrl risCu = au.makeCachedUrl(risUrl);
              // try an encoded version?
              //if (risCu == null || !risCu.hasContent()) {
                //risCu = au.makeCachedUrl(risUrl);
              //}
              
              if (risCu != null && risCu.hasContent()) {
                af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, risCu);
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
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
