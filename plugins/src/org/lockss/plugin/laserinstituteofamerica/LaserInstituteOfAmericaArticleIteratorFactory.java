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

package org.lockss.plugin.laserinstituteofamerica;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class LaserInstituteOfAmericaArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("LaserInstituteOfAmerica");
  
  protected static final String ROOT_TEMPLATE = "\"%s%d\",base_url,year";
  
  protected static final String PATTERN_TEMPLATE = "\"%s%d/LIA_xml_[\\d]+\\.tar\\.gz!/[^/]+/vol_[\\d]+/iss_[\\d]+/[\\d]+_1.xml$\",base_url,year";
  
  protected static final String INCLUDE_SUBTREE_TEMPLATE = "\"%s%d/LIA_xml_[\\d]+\\.tar\\.gz!/[^/]+/vol_[\\d]+/iss_[\\d]+/[\\d]+_1.xml$\",base_url,year";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new LaserInstituteOfAmericaArticleIterator(au, 
              new SubTreeArticleIterator.Spec()
                    .setTarget(target)
                    .setRootTemplate(ROOT_TEMPLATE)
                    .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
                    .setIncludeSubTreePatternTemplate(INCLUDE_SUBTREE_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class LaserInstituteOfAmericaArticleIterator extends SubTreeArticleIterator {
	 
    protected static Pattern PATTERN = Pattern.compile("(/LIA_)(xml)(_\\d+\\.tar\\.gz!/[^/]+/vol_\\d+/iss_\\d+/)(\\d+)(_1.xml)$", Pattern.CASE_INSENSITIVE);
    
    protected LaserInstituteOfAmericaArticleIterator(ArchivalUnit au,
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
