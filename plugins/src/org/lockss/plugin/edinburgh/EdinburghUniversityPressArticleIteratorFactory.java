/*
 * $Id: EdinburghUniversityPressArticleIteratorFactory.java,v 1.2 2010-07-06 07:23:45 thib_gc Exp $
 */

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

package org.lockss.plugin.edinburgh;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.extractor.*;
import org.lockss.util.*;

public class EdinburghUniversityPressArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger("EdinburghUniversityPressArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%sdoi/pdfplus/\", base_url";
  
  protected static final String PATTERN_TEMPLATE = "\"^%sdoi/pdfplus/[.0-9]+/\", base_url";
  
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    return new EdinburghUniversityPressArticleIterator(au, new SubTreeArticleIterator.Spec()
				                           .setTarget(target)
				                           .setRootTemplate(ROOT_TEMPLATE)
				                           .setPatternTemplate(PATTERN_TEMPLATE));
  }

  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new EdinburghUniversityPressArticleMetadataExtractor();
  }
  
  protected static class EdinburghUniversityPressArticleIterator extends SubTreeArticleIterator {
    
    protected static Pattern PDF_PATTERN = Pattern.compile("/doi/pdfplus/([.0-9]+/.+)$", Pattern.CASE_INSENSITIVE);
    
    public EdinburghUniversityPressArticleIterator(ArchivalUnit au,
                                                   SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat = PDF_PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextPdf(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);

      CachedUrl absCu = au.makeCachedUrl(pdfMat.replaceFirst("/abs/$1"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
      }

      return af;
    }
    
  }
  
  protected static class EdinburghUniversityPressArticleMetadataExtractor implements ArticleMetadataExtractor {

    public ArticleMetadata extract(ArticleFiles af) throws IOException, PluginException {
      String url = af.getFullTextUrl();
      Matcher mat = EdinburghUniversityPressArticleIterator.PDF_PATTERN.matcher(url);
      if (!mat.find()) {
        log.warning("Mismatch between the article iterator and the article metadata extractor: " + url);
        return null;
      }
      String doi = mat.group(1);
      
      ArticleMetadata am = new ArticleMetadata();
      am.put(ArticleMetadata.KEY_ACCESS_URL, url);
      am.putDOI(doi);
      return am;
    }

  }

}
