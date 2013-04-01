/*
 * $Id: IUMJArticleIteratorFactory.java,v 1.3 2013-04-01 16:34:03 aishizaki Exp $
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

package org.lockss.plugin.iumj;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class IUMJArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log =
    Logger.getLogger("IUMJArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE =
    "\"%sIUMJ/FTDLOAD/%d/\", base_url, year";
  
  protected static final String PATTERN_TEMPLATE =
    "\"%sIUMJ/FTDLOAD/(%d)/%s/([^/]+)/pdf\", base_url, year, volume_name";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new
      IUMJArticleIterator(au,
              new SubTreeArticleIterator.Spec()
              .setTarget(target)
              .setRootTemplate(ROOT_TEMPLATE)
              .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class IUMJArticleIterator
    extends SubTreeArticleIterator {
    
    protected static Pattern PDF_PATTERN =
      Pattern.compile("IUMJ/FTDLOAD/([^/]+)/[^/]+/([^/]+)/pdf", Pattern.CASE_INSENSITIVE);
    
    public IUMJArticleIterator(ArchivalUnit au,
                                           SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;
      
      mat = PDF_PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextPdf(cu, mat);
      }
      
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      
      ArticleFiles af = new ArticleFiles();
      CachedUrl xmlCu = au.makeCachedUrl(pdfMat.replaceFirst("META/$1/$2\\.xml"));
      
      if (xmlCu != null && xmlCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, xmlCu);
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
        af.setFullTextCu(pdfCu);
      }
      
      return af;
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
