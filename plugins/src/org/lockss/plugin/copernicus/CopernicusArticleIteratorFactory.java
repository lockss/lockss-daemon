/*
 * $Id: CopernicusArticleIteratorFactory.java,v 1.1 2012-11-15 21:36:52 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.copernicus;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Article lives at:  http://www.<base-url>/<volume>/<startpage#>/<year>/<alphanumericID>
 * <article>.html is the abstract
 * <article>.pdf is the full text pdf
 * * there might additionally be an <article>-supplement.pdf
 * <article>.bib, ris, xml are the citations
 */

public class CopernicusArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("CopernicusArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%s%s/\", base_url, volume_name"; // params from tdb file corresponding to AU  
  // In the pattern, the bit in parens (ending in supplement) excludes thos patterns that end in <blah>supplement.pdf, but takes other <blah>.pdf
 // protected static final String PATTERN_TEMPLATE = "\"^%s%s/[0-9]+/[0-9]+/(?![A-Za-z0-9-]+supplement\\.pdf)[A-Za-z0-9-]+\\.pdf\", base_url,volume_name";
 // pick up the abstract as the logical definition of one article
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/[0-9]+/[0-9]+/[A-Za-z0-9-]+\\.html\", base_url,volume_name";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new CopernicusArticleIterator(au,
                                         new SubTreeArticleIterator.Spec()
                                             .setTarget(target)
                                             .setRootTemplate(ROOT_TEMPLATE)
                                             .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class CopernicusArticleIterator extends SubTreeArticleIterator {
    
//  Use parens to group the base article URL 
    protected Pattern ABSTRACT_PATTERN = Pattern.compile("(/[0-9]+/[0-9]+/[A-Za-z0-9-]+)\\.html$", Pattern.CASE_INSENSITIVE);

    public CopernicusArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.info("article url?: " + url);
      
      Matcher mat = ABSTRACT_PATTERN.matcher(url);
      if (mat.find()) {
        return processAbstract(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles processAbstract(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();

      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, cu);
      guessAdditionalFiles(af, mat);
      
      return af;
      }
    
    protected void guessAdditionalFiles(ArticleFiles af, Matcher mat) {      
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("$1.pdf"));
      CachedUrl xmlCu = au.makeCachedUrl(mat.replaceFirst("$1.xml"));
      CachedUrl risCu = au.makeCachedUrl(mat.replaceFirst("$1.ris"));
      CachedUrl bibCu = au.makeCachedUrl(mat.replaceFirst("$1.bib"));
      CachedUrl supCu = au.makeCachedUrl(mat.replaceFirst("$1-supplement.pdf"));

      if (pdfCu != null && pdfCu.hasContent()) {
          af.setFullTextCu(pdfCu);
          af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
          
      if (xmlCu != null && xmlCu.hasContent()) {
/*          af.setRoleCu(ArticleFiles.ROLE_???, xmlCu);*/
      }
      
      if (risCu != null && risCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_CITATION + "Ris", risCu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, risCu);
      }
      
      if (bibCu != null && bibCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_CITATION + "Bibtex", bibCu);
    }     
      if (supCu != null && supCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, supCu);
    }     
    }
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
