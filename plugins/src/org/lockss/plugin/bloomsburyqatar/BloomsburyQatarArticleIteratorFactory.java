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

package org.lockss.plugin.bloomsburyqatar;

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

public class BloomsburyQatarArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("BloomsburyQatarArticleIteratorFactory");
  
  // make root more general in case different AUs have different variety of types (full, pdf, pdfplus, abstract...)
  protected static final String ROOT_TEMPLATE = "\"%sdoi\", base_url"; 
  
  // be moe specific about what we're looking for in the pattern template 
  // NOTE - the DOI portion should be generalized to (pdf|pdfplus)/[.0-9]+/\", base_url"; 
  // not doing this right now because it does then require analysis to ensure that we don't need a crawl filter
  protected static final String PATTERN_TEMPLATE = "\"^%sdoi/(pdf|pdfplus)/10\\.5339/%s[\\.\\d+\\.]?\\.\", base_url, journal_dir";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
	  log.debug3("BloomsburyQatarArticleIteratorFactory running");
    return new BloomsburyQatarArticleIterator(au, new SubTreeArticleIterator.Spec()
                                       .setTarget(target)
                                       .setRootTemplate(ROOT_TEMPLATE)
                                       .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class BloomsburyQatarArticleIterator extends SubTreeArticleIterator {
	 
    protected Pattern pattern;
    protected Pattern pluspattern;
    protected Boolean noAbstract = true;
        
    protected BloomsburyQatarArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      // might have pdfplus and/or pdf
      pattern = Pattern.compile(String.format("(%sdoi/)pdf(/10.5339/%s)",
          au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()),
          au.getConfiguration().get(ConfigParamDescr.JOURNAL_DIR.getKey())),
          Pattern.CASE_INSENSITIVE);
      pluspattern = Pattern.compile(String.format("(%sdoi/)pdfplus(/10.5339/%s)",
          au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()),
          au.getConfiguration().get(ConfigParamDescr.JOURNAL_DIR.getKey())),
          Pattern.CASE_INSENSITIVE);
        }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();

      Matcher mat = pattern.matcher(url); // do we match pdf?
      if (mat.find()) {
        return processPdfFullText(cu, mat);
      } else {
        mat = pluspattern.matcher(url); // do we match a pdfplus?
        if (mat.find()) {
          return processPlusFullText(cu,mat);
        }
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processPdfFullText(CachedUrl cu, Matcher mat) {
      // if the pdfplus exists, then we'll let that trigger the article files, not the pdf
      
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("$1pdfplus$2"));
      if (pdfCu != null && pdfCu.hasContent()) {
        return null; // the pdf will get found when we process the pdfplus
      }
      // there is no pdfplus, so go ahead and use the pdf as the article trigger
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu); // this is only the full text if there is no pdfplus

      return guessAdditionalFiles(af, mat);
    }
    
    protected ArticleFiles processPlusFullText(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu); // even if there is a pdf, this will represent the full text
      
      return guessAdditionalFiles(af, mat);
    }
    
    protected ArticleFiles guessAdditionalFiles(ArticleFiles af, Matcher mat) {
      if(spec.getTarget() != MetadataTarget.Article())
      {
                guessAbstract(af, mat);
                guessFullTextHtml(af, mat); // do abstract before full text
                guessPdf(af, mat);
                guessSupplementaryMaterials(af, mat);
      }
      return af;
    }
    
    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("$1abs$2"));
      if (absCu != null && absCu.hasContent()) {
        noAbstract = false;
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        af.setRoleCu(ArticleFiles.ROLE_REFERENCES, absCu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);
      }
    }
    
    protected void guessFullTextHtml(ArticleFiles af, Matcher mat) {
      CachedUrl htmlCu = au.makeCachedUrl(mat.replaceFirst("$1full$2"));
      if (htmlCu != null && htmlCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
        if (noAbstract) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA,htmlCu);
        }
      }
      
    }

    
    protected void guessPdf(ArticleFiles af, Matcher mat) {
      // you might have gotten here having matched EITHER pdf or pdfplus - you can't assume
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("$1pdfplus$2"));
      CachedUrl pdfCu2 = au.makeCachedUrl(mat.replaceFirst("$1pdf$2"));
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu); // take the pdfplus as first coice
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
	    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
	  }

}
