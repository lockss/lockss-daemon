/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

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

public class BaseScArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  private static String ROOT_TEMPLATE = "\"%s%s\", base_url, journal_id";
  private static String PATTERN_TEMPLATE =  "\"%s%s/article(-abstract)?\", base_url, journal_id";
  //  //https://pubs.geoscienceworld.org/gsa/gsabulletin/article/132/1-2/113/570559/Vegetation-and-habitat-change-in-southern
  //https://academic.oup.com/psychsocgerontology/article/66B/1/109/580460
  private static Pattern HTML_PATTERN = Pattern.compile("/article/([^/]+)/(.*)$", Pattern.CASE_INSENSITIVE);
  private static String HTML_REPLACEMENT = "/article/$1/$2";
  private static String ABSTRACT_REPLACEMENT = "/article-abstract/$1/$2";
  private static String CITATION_REPLACEMENT = "/downloadcitation/$1?format=ris";
  //<meta name="citation_pdf_url" content="https://academic.oup.com/bioinformatics/article-pdf/31/1/119/6999904/btu602.pdf" />
  protected static Pattern PDF_PATTERN = Pattern.compile(
      "<meta[\\s]*name=\"citation_pdf_url\"[\\s]*content=\"(.+/article-pdf/[^.]+\\.pdf)\"", Pattern.CASE_INSENSITIVE);
  
  protected static Logger log = getLog();
  
  protected static Logger getLog() {
    return Logger.getLogger(BaseScArticleIteratorFactory.class);
  }

  protected String getPATTERN_TEMPLATE() {
    return PATTERN_TEMPLATE;
  }

  protected static Pattern getHTML_PATTERN() {
    return HTML_PATTERN;
  }

  protected static String getHTML_REPLACEMENT() {
    return HTML_REPLACEMENT;
  }

  protected static String getABSTRACT_REPLACEMENT() {
    return ABSTRACT_REPLACEMENT;
  }

  protected static String getCITATION_REPLACEMENT() {
    return CITATION_REPLACEMENT;
  }

  protected static Pattern getPDF_PATTERN() {
    return PDF_PATTERN;
  }

  protected String getRootTemplate() {
    return ROOT_TEMPLATE;
  }

  protected String getPatternTemplate() {
    return PATTERN_TEMPLATE;
  }

  protected static Pattern getHtmlPattern() {
    return HTML_PATTERN;
  }

  protected static String getHtmlReplacement() {
    return HTML_REPLACEMENT;
  }

  protected static String getAbstractReplacement() {
    return ABSTRACT_REPLACEMENT;
  }

  protected static String getCitationReplacement() {
    return CITATION_REPLACEMENT;
  }

  protected static Pattern getPdfPattern() {
    return PDF_PATTERN;
  }

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
          throws PluginException {
    ArrayList<String> ary = new ArrayList<String>();
    SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);
    if (getRootTemplate() != null && getPatternTemplate() != null) {
      builder.setSpec(target,
              Arrays.asList(getRootTemplate()),
          getPatternTemplate(), Pattern.CASE_INSENSITIVE);
    }
    if (getHtmlPattern() != null && getHtmlReplacement() != null) {
      builder.addAspect(getHtmlPattern(),
          getHtmlReplacement(),
          ArticleFiles.ROLE_FULL_TEXT_HTML);
      ary.add(ArticleFiles.ROLE_FULL_TEXT_HTML);
    }
    if (getAbstractReplacement() != null) {
      builder.addAspect(getAbstractReplacement(),
          ArticleFiles.ROLE_ABSTRACT);
      ary.add(0, ArticleFiles.ROLE_ABSTRACT);
    }
    if (getCitationReplacement() != null) {
      builder.addAspect(getCitationReplacement(),
          ArticleFiles.ROLE_CITATION);
      ary.add(0, ArticleFiles.ROLE_CITATION);
    }
    if (!ary.isEmpty()) {
      builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, ary);
//          ArticleFiles.ROLE_CITATION,
//          ArticleFiles.ROLE_ABSTRACT,
//          ArticleFiles.ROLE_FULL_TEXT_HTML
    }
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
        CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML);
        if (cu == null || !cu.hasContent() || getPdfPattern() == null) {
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
          for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
            mat = getPdfPattern().matcher(line);
            if (mat.find()) {
              String pdfUrl = mat.group(1);
              CachedUrl pdfCu = au.makeCachedUrl(pdfUrl);
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
