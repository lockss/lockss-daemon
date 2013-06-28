/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americaninstituteofphysics;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Full-text and metadata XML:
 *      "<base_url>/<year>/<zip_file_name>.zip!/
 *              <journal_id>/<volume_num>/<issue_num>/
 *              <article_num>/Markup/<xml_file_name>.xml"

 *      "http://clockss-ingest.lockss.org/sourcefiles/aipjats-released/
 *              2013/test_76_clockss_aip_2013-06-07_084326.zip!/
 *              JAP/v111/i11/112601_1/Markup/VOR_10.1063_1.4726155.xml"
 * 
 * Full-text PDF:
 *      "<base_url>/<year>/<zip_file_name>.zip!/
 *              <journal_id>/<volume_num>/<issue_num>/
 *              <article_num>/Page_Renditions/online.pdf"
 *
 *      "http://clockss-ingest.lockss.org/sourcefiles/aipjats-released/
 *              2013/test_76_clockss_aip_2013-06-07_084326.zip!/
 *              JAP/v111/i11/112601_1/Page_Renditions/online.pdf"
 */
public class AIPJatsSourceArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static Logger log = 
      Logger.getLogger(AIPJatsSourceArticleIteratorFactory.class);
  
  private static final String ROOT_TEMPLATE = "\"%s\", base_url";
  private static final String PATTERN_TEMPLATE = 
      "\"%s%d/[^/]+\\.zip!/[A-Z]+/v[0-9]+/i[0-9]+/[^/]+/"
          + "(Page_Renditions|Markup)/[^/]+\\.(pdf|xml)$\", base_url, year";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new AIPJatsSourceArticleIterator(au, 
                                            new SubTreeArticleIterator.Spec()
                                           .setTarget(target)
                                           .setRootTemplate(ROOT_TEMPLATE)
                                           .setPatternTemplate(PATTERN_TEMPLATE, 
                                               Pattern.CASE_INSENSITIVE));
  }
  
  private static class AIPJatsSourceArticleIterator
    extends SubTreeArticleIterator {
	 
    private static Pattern XML_PATTERN =
        Pattern.compile("/([^/]+)/Markup/[^/]+\\.xml$", 
                        Pattern.CASE_INSENSITIVE);
    
    private AIPJatsSourceArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      spec.setVisitArchiveMembers(true);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.info("artile url: " + url);
      Matcher mat;
            
      mat = XML_PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextXml(cu, mat);
      }
      
      log.warning("Mismatch between article iterator factory "
                  + "and article iterator: " + url);
      return null;
    }

    private ArticleFiles processFullTextXml(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_XML, cu);
      af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
      
      log.debug3("target: " + spec.getTarget().getPurpose());
      if(spec.getTarget() != MetadataTarget.Article()) {
	guessPdf(af, mat);
      }
      
      return af;
    }
    
    private void guessPdf(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(
          mat.replaceFirst("/$1/Page_Renditions/online.pdf"));
      
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
    }
    
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(
                                                        MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
