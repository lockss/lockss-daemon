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

 
// This article iterator is neither complete nor QAed.  This is the initial 
// development using the JATS source file samples from AIP.  AIP's production
// is estimated to be released in Mar/Apr 2013.  Issue4582.
 
package org.lockss.plugin.aipjats;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class AIPJatsArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("AIPJatsArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%s%d\",base_url,year";
  
  protected static final String PATTERN_TEMPLATE = "\"%s%d/AIP_xml_[\\d]+\\.tar\\.gz!/[^/]+/vol_[\\d]+/iss_[\\d]+/[\\d]+_1.xml$\",base_url,year";
  
  protected static final String INCLUDE_SUBTREE_TEMPLATE = "\"%s%d/AIP_xml_[\\d]+\\.tar\\.gz!/[^/]+/vol_[\\d]+/iss_[\\d]+/[\\d]+_1.xml$\",base_url,year";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new AIPJatsArticleIterator(au, new SubTreeArticleIterator.Spec()
                                .setTarget(target)
                                .setRootTemplate(ROOT_TEMPLATE)
                                .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
                                .setIncludeSubTreePatternTemplate(INCLUDE_SUBTREE_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class AIPJatsArticleIterator extends SubTreeArticleIterator {
	 
    protected static Pattern PATTERN = Pattern.compile("(/AIP_)(xml)(_\\d+\\.tar\\.gz!/[^/]+/vol_\\d+/iss_\\d+/)(\\d+)(_1.xml)$", Pattern.CASE_INSENSITIVE);
    
    protected AIPJatsArticleIterator(ArchivalUnit au,
                                           SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      spec.setVisitArchiveMembers(true);
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
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, cu);
      
// temporary: no need to iterate pdf, webimages, printimages for now (PJG)
//      if(spec.getTarget() != MetadataTarget.Article) {
//        guessAdditionalFiles(af, mat);
//      }
      return af;
    }
    
    protected void guessAdditionalFiles(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("$1pdf$3$4.pdf"));
      CachedUrl webImagesCu = au.makeCachedUrl(mat.replaceFirst("$1webimages$3$4_1.zip"));
      CachedUrl printImagesCu = au.makeCachedUrl(mat.replaceFirst("$1printimages$3$4_1.zip"));
      
      if (pdfCu != null && pdfCu.hasContent()) 
    	  af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      if (webImagesCu != null && webImagesCu.hasContent())
    	  af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, webImagesCu);
      if (printImagesCu != null && printImagesCu.hasContent())
    	  af.setRoleCu(ArticleFiles.ROLE_FIGURES_TABLES, printImagesCu);
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
	      throws PluginException {
	    return new BaseArticleMetadataExtractor(null);
  }
}
