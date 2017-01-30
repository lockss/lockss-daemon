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

public class ScAMAArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(ScAMAArticleIteratorFactory.class);
  
  private static final String ROOT_TEMPLATE = "\"%s\", base_url";
  private static final String PATTERN_TEMPLATE = "\"^%sjournals/%s/fullarticle/\\d+$\", base_url, journal_id";
  
  private static final Pattern HTML_PATTERN = Pattern.compile("/fullarticle/(\\d+)$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/fullarticle/$1";
  private static final String ABSTRACT_REPLACEMENT = "/article-abstract/$1";
  private static final String CITATION_REPLACEMENT = "/downloadcitation/$1?format=";
  
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "<meta name=\"citation_pdf_url\" content=\".+/(data/journals/[^/]+/[^/]+/[^/.]+[.]pdf$)\"", Pattern.CASE_INSENSITIVE);
  // <meta name="citation_pdf_url" content="http://jama.jamanetwork.com/data/journals/jama/934829/jii160002.pdf" />
  
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
  
  /*  
  //http://jamanetwork.com/journals/jamainternalmedicine/data/journals/intemed/934800/iii160001.pdf
  //http://jamanetwork.com/journals/jamainternalmedicine/fullarticle/2479062
  //http://jamanetwork.com/journals/jamainternalmedicine/article-abstract/2479062
  //http://jamanetwork.com/journals/jamainternalmedicine/downloadcitation/2479062?format=

  http://jamanetwork.com/journals/jamainternalmedicine/data/journals/intemed/934800/iic150063.pdf
  http://amaprod.silverchaircdn.com/UI/app
  http://jamanetwork.com/journals/jamainternalmedicine/issue/176/1
  http://jamanetwork.com/article.aspx?doi=10.1001/jamainternmed.2015.5946
  http://jamanetwork.com/article.aspx?doi=10.1001/jama.2015.17632
  http://jamanetwork.com/data/Journals/INTEMED/934800/IOI150079supp1_prod.pdf
  http://jamanetwork.com/pdfaccess.ashx?url=/data/journals/intemed/935017  (no routename)
  http://jamanetwork.com/pdfaccess.ashx?url=/data/journals/intemed/934800/ioi150092supp1_prod.pdf
  
  http://archinte.jamanetwork.com/article.aspx?doi=10.1001/jamainternmed.2015.6324
  http://jamanetwork.com/journals/jamainternalmedicine/downloadcitation/2472944?format=
  http://jamanetwork.com/downloadimage.aspx?image=/data/Journals/INTEMED/935316/ioi160030t3.png&sec=126816453&ar=2520680&imagename=
  learning/video-player/12889420
  
  http://cdn.jsdelivr.net/chartist.js/latest/chartist.min.css
  http://d1bxh8uas1mnw7.cloudfront.net/assets/embed.js
  https://cdn.optimizely.com/js/2785501806.js (not collected)
  http://jamanetwork.com/learning/article-quiz/ (not collected)
  
  <string>1,"^https?://([^.]+\.(googleapis|gstatic)\.com|cdn\.jsdelivr\.net|ajax\.microsoft\.com)/"</string>
  <string>2,"^%s(SharedControls/)?DownloadImage\.aspx\?", base_url</string>
  <string>1,"^https?://%s/.*\.(bmp|css|eot|gif|ico|jpe?g|js|otf|png|svg|tif?f|ttf|woff)$", url_host(base_url)</string>
  <string>1,"^https?://[^.]+\.(silverchair\.netdna-cdn|silverchaircdn)\.com/.*\.(bmp|css|eot|gif|ico|jpe?g|js|otf|png|svg|tif?f|ttf|woff)(\?.*)?$"</string>
  <string>1,"^https?://[^.]+\.(silverchair\.netdna-cdn|silverchaircdn)\.com/combres\.axd/"</string>
  <string>4,"^%s", base_url</string>
  <string>2,"__EVENTVALIDATION="</string>
  <string>2,"^%sdownloadCitation\.aspx\?format=[^&amp;]+$", base_url</string>
  <string>1,"^%s(article\.aspx\?articleid=|(downloadCitation|multimediaPlayer)\.aspx\?)", base_url</string>
  <string>1,"^%sIssue\.aspx\?(.*&amp;journalid=%d|journalid=%d&amp;.*)$", base_url, resource_id, resource_id</string>
  <string>1,"^%sdata/(Journals|Multimedia)/", base_url</string>
  <string>1,"^%scombres\.axd/", base_url</string>
  <string>1,"^%sissue\.aspx/SetArticlePDFLinkBasedOnAccess", base_url</string>
  <string>1,"^%svolume\.aspx/SetPDFLinkBasedOnAccess", base_url</string>
  <string>1,"^%spdfaccess\.ashx\?ResourceID", base_url</string>
  <string>1,"^%sScriptResource\.axd\?d", base_url</string>
  */
  protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) { 
    return new SubTreeArticleIteratorBuilder(au) {
      
      @Override
      protected BuildableSubTreeArticleIterator instantiateBuildableIterator() {
        
        
        return new BuildableSubTreeArticleIterator(au, spec) {
          
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
      
      protected void guessPdf(ArticleFiles af) {
        CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
        if (cu == null || !cu.hasContent()) {
          return;
        }
        String url = cu.getUrl();
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
            mat = PDF_PATTERN.matcher(line);
            if (mat.find()) {
              String baseUrl = au.getConfiguration().get("base_url");
              String pdfUrl = mat.group(1);
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
