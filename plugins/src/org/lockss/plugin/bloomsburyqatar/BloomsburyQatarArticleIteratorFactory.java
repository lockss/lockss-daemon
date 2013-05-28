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
