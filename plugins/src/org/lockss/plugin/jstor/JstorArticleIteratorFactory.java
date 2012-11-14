/*
 * $Id: JstorArticleIteratorFactory.java,v 1.1 2012-11-14 23:03:59 wkwilson Exp $
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

package org.lockss.plugin.jstor;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

/*
 * PDF Full Text: http://www.igi-global.com/article/full-text-pdf/56564
 * HTML Abstract: http://www.igi-global.com/article/56564
 */

public class JstorArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("IgiArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%sstable/[\\.0-9]+\", base_url"; 
  
  protected static final String PATTERN_TEMPLATE = "\"^%sstable/[\\.0-9]+(/)?(.+\\.[0-9]{4}\\.[0-9]+.[0-9]+)?$\", base_url";

  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new JstorArticleIterator(au,
                                    new SubTreeArticleIterator.Spec()
                                      .setTarget(target)
                                      .setRootTemplate(ROOT_TEMPLATE)
                                      .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class JstorArticleIterator extends SubTreeArticleIterator {

    protected Pattern ARTICLE_PATTERN_1 = Pattern.compile("(stable)/([0-9]+(/)?)$", Pattern.CASE_INSENSITIVE);
    
    protected Pattern ARTICLE_PATTERN_2 = Pattern.compile("(stable)/([\\.0-9]+/.+\\.[0-9]{4}\\.[0-9]+.[0-9]+)$", Pattern.CASE_INSENSITIVE);
    
    public JstorArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;
      mat = ARTICLE_PATTERN_1.matcher(url);
      if (mat.find()) {
        return processArticle(cu, mat);
      }
      mat = ARTICLE_PATTERN_2.matcher(url);
      if (mat.find()) {
        return processArticle(cu, mat);
      }

      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    

    protected ArticleFiles processArticle(CachedUrl landingCu, Matcher articleMat) {
      ArticleFiles af = new ArticleFiles();
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE, landingCu);

      if (spec.getTarget().isArticle()) {
	guessRole(af, articleMat, "view info", ArticleFiles.ROLE_ARTICLE_METADATA);
        guessRole(af, articleMat, "pdfplus pdf", ArticleFiles.ROLE_FULL_TEXT_PDF);
        guessRole(af, articleMat, "full", ArticleFiles.ROLE_FULL_TEXT_HTML);
      }
      return af;
    }
    
    protected void guessRole(ArticleFiles af, Matcher mat, String tryList, String roleKey) {
      for(String replace : tryList.split(" ")) {
        String guess = mat.replaceFirst("$1/" + replace + "/$2");
        
        CachedUrl absCu = au.makeCachedUrl(guess);
        if (absCu != null && absCu.hasContent()) {
      	af.setRoleCu(roleKey, absCu);
        }
      }
    }
    
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
