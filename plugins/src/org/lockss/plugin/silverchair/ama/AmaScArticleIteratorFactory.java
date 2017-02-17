/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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
in this Software without prior written authorization from Stanford University.
be used in advertising or otherwise to promote the sale, use or other dealings

*/

package org.lockss.plugin.silverchair.ama;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

public class AmaScArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(AmaScArticleIteratorFactory.class);
  
  private static final String ROOT_TEMPLATE = "\"%s\", base_url";
  private static final String PATTERN_TEMPLATE = "\"^%sjournals/%s/fullarticle/\\d+$\", base_url, journal_id";
  
  private static final Pattern HTML_PATTERN = Pattern.compile("/fullarticle/(\\d+)$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/fullarticle/$1";
  private static final String ABSTRACT_REPLACEMENT = "/article-abstract/$1";
  private static final String CITATION_REPLACEMENT = "/downloadcitation/$1?format=ris";
  
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "<meta name=\"citation_pdf_url\" content=\".+/(data/journals/[^/]+/[^/]+/[^/.]+[.]pdf)\"", Pattern.CASE_INSENSITIVE);
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);
    builder.setSpec(target,
                    ROOT_TEMPLATE,
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    builder.addAspect(HTML_PATTERN,
                      HTML_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_HTML);
    builder.addAspect(ABSTRACT_REPLACEMENT,
                      ArticleFiles.ROLE_ABSTRACT);
    builder.addAspect(CITATION_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION);
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                                  ArticleFiles.ROLE_ABSTRACT,
                                  ArticleFiles.ROLE_FULL_TEXT_HTML);
    return builder.getSubTreeArticleIterator();
  }
  
  protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) { 
    return new SubTreeArticleIteratorBuilder(au) {
      
      @Override
      protected BuildableSubTreeArticleIterator instantiateBuildableIterator() {
        
        
        return new BuildableSubTreeArticleIterator(au, spec) {
          
          @Override
          protected ArticleFiles createArticleFiles(CachedUrl cu) {
            // Since 1.64 createArticleFiles can return ArticleFiles
            ArticleFiles af = super.createArticleFiles(cu);
            
            if (af != null && spec.getTarget() != null && !spec.getTarget().isArticle()) {
              guessPdf(af);
            }
            return af;
          }
        };
      }
      
      protected void guessPdf(ArticleFiles af) {
        CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
        if (cu == null || !cu.hasContent()) {
          return;
        }
        BufferedReader bReader = null;
        try {
          bReader = new BufferedReader(new InputStreamReader(
              cu.getUnfilteredInputStream(), cu.getEncoding())
              );
          
          Matcher mat;
          // go through the cached URL content line by line
          // if a match is found, look for valid url & content
          // if found then set the role for ROLE_FULL_TEXT_PDF
          String baseUrl = au.getConfiguration().get("base_url");
          String journalId = au.getConfiguration().get("journal_id");
          for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
            mat = PDF_PATTERN.matcher(line);
            if (mat.find()) {
              String pdfUrl = "journals/" + journalId + "/" + mat.group(1);
              CachedUrl pdfCu = au.makeCachedUrl(baseUrl + pdfUrl);
              if (pdfCu != null && pdfCu.hasContent()) {
                af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
              }
              break;
            }
          }
        } catch (Exception e) {
          // probably not serious, so warn
          log.warning(e + " : Looking for citation_pdf_url");
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
