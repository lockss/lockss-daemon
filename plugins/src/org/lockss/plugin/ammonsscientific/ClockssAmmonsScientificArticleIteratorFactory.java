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

package org.lockss.plugin.ammonsscientific;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ClockssAmmonsScientificArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("ClockssAmmonsScientificArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%s\",base_url";
  
  protected static final String PATTERN_TEMPLATE = "\"^%sdoi/abs/10.2446/\\d+\\.\\d+\\.\\d+\\.%s\\.%s\\.\\d+\\.\\d+-\\d+\", base_url, journal_abbr, volume_name";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new ClockssAmmonsArticleIterator(au, new SubTreeArticleIterator.Spec()
                                       .setTarget(target)
                                       .setRootTemplate(ROOT_TEMPLATE)
                                       .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class ClockssAmmonsArticleIterator extends SubTreeArticleIterator {
	 
    protected Pattern pattern;
    
    protected ClockssAmmonsArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      pattern = Pattern.compile(String.format("(%sdoi/)abs(/10.2446/[0-9]*.[0-9]*.[0-9]*.%s.%s.)",
      		au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()),
      		au.getConfiguration().get(ConfigParamDescr.JOURNAL_ABBR.getKey()),
      		au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey())),
      		Pattern.CASE_INSENSITIVE);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat = pattern.matcher(url);
      if (mat.find()) {
        return processFullText(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullText(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      
      if(spec.getTarget() != MetadataTarget.Article)
      {
		guessPdf(af, mat);
		guessSupplementaryMaterials(af, mat);
      }
      return af;
    }
    
    protected void guessPdf(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("$1pdfplus$2"));
      CachedUrl pdfCu2 = au.makeCachedUrl(mat.replaceFirst("$1pdf$2"));

      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
      else if (pdfCu2 != null && pdfCu2.hasContent()) {
    	  af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu2);
      }
    }
    
    protected void guessSupplementaryMaterials(ArticleFiles af, Matcher mat) {
      CachedUrl suppCu = au.makeCachedUrl(mat.replaceFirst("$1suppl$2"));
      if (suppCu != null && suppCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, suppCu);
      }
    }
    
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
	      throws PluginException {
	    return new BaseArticleMetadataExtractor(null);
	  }

}
