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

package org.lockss.plugin.springer;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class SpringerSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(SpringerSourceArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%s%d\",base_url,year";
  // find anyfiles that end in "BodyRef/PDF/XXX.pdf"
  // We do not know how many levels down because the book/journal might be at various levels
  // book series
  //BSE=0304/BOK=978-3-540-35043-9/CHP=10_10.1007BFb0103161/BodyRef/PDF/978-3-540-35043-9_Chapter_10.pdf
  //BSE=8913/BOK=978-94-6265-114-2/PRT=1/CHP=7_10.1007978-94-6265-114-2_7/BodyRef/PDF/978-94-6265-114-2_Chapter_7.pdf
  // book
  //BOK=978-981-10-0886-3/PRT=4/CHP=12_10.1007978-981-10-0886-3_12/BodyRef/PDF/978-981-10-0886-3_Chapter_12.pdf
  //BOK=978-981-10-0886-3/CHP=1_10.1007978-981-10-0886-3_1/BodyRef/PDF/978-981-10-0886-3_Chapter_1.pdf
  // journal
  //JOU=13678/VOL=2012.1/ISU=1/ART=12/BodyRef/PDF/13678_2012_Article_12.pdf
  protected static final String PATTERN_TEMPLATE = "\"%s%d/[^/]+\\.zip!/[A-Z]+=.*/BodyRef/PDF/[^/]+\\.pdf$\",base_url,year";
  protected static final String NESTED_ARCHIVE_PATTERN_TEMPLATE = "\"%s%d/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$\",base_url,year";
 
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new SpringerArticleIterator(au, new SubTreeArticleIterator.Spec()
                                       .setTarget(target)
                                       .setVisitArchiveMembers(true)
                                       .setRootTemplate(ROOT_TEMPLATE)
                                       .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
                                       .setExcludeSubTreePatternTemplate(NESTED_ARCHIVE_PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class SpringerArticleIterator extends SubTreeArticleIterator {
	
    // break it in to three parts in order to find the corresponding xml.Meta file
    protected static Pattern PATTERN = Pattern.compile("(/[^/]+\\.zip!/[A-Z]+=.*/)(BodyRef/PDF/)([^/]+)(\\.pdf)$", Pattern.CASE_INSENSITIVE);
    
    protected SpringerArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      
      Matcher mat = PATTERN.matcher(url);
      if (mat.find()) {
        return processFullText(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullText(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      log.debug3("setting role FullTextCu: " + cu.getUrl());
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);
      log.debug3("setting ROLE_FULL_TEXT_PDF: " + cu.getUrl());
      
      log.debug3("target: " + spec.getTarget().getPurpose());
      if(spec.getTarget() != MetadataTarget.Article)
	guessAdditionalFiles(af, mat);
      
      return af;
    }
    
    protected void guessAdditionalFiles(ArticleFiles af, Matcher mat) {
      CachedUrl metadataCu = au.makeCachedUrl(mat.replaceFirst("$1$3.xml.Meta"));
      log.debug3("guessAdditionalFiles metadataCu: " + metadataCu);
      CachedUrl xmlCu = au.makeCachedUrl(mat.replaceFirst("$1$3.xml"));

      boolean setMD = false;
      if ((metadataCu != null) && (metadataCu.hasContent())) {
        setMD = true;
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, metadataCu);
        log.debug3("setting ROLE_ARTICLE_METADATA: " + metadataCu.getUrl());
      }
      
      if ((xmlCu != null) && (xmlCu.hasContent())) {
        // this will be our backup if the ".xml.Meta wasn't there
        if (!setMD) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, xmlCu);
          log.debug3("setting ROLE_ARTICLE_METADATA: " + xmlCu.getUrl());
        }
        // and make it your full-text xml
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, xmlCu);
        log.debug3("setting ROLE_FULL_TEXT_HTML: " + xmlCu.getUrl());
      }
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
	      throws PluginException {
    // the custom ArticleMetadataExtractor wasn't adding unique value over Base... 
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
