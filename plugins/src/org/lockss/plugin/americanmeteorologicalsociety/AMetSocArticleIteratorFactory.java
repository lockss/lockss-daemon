/*
 * $Id: AMetSocArticleIteratorFactory.java,v 1.1 2013-02-08 00:19:42 alexandraohlson Exp $
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

package org.lockss.plugin.americanmeteorologicalsociety;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.extractor.*;
import org.lockss.util.*;

public class AMetSocArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  protected static Logger log = 
    Logger.getLogger("AMetSocArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%sdoi/abs/\", base_url";
  
  protected static final String PATTERN_TEMPLATE = 
    "\"^%sdoi/abs/[.0-9]+/\", base_url";
  
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    return new AMetSocArticleIterator(
        au, new SubTreeArticleIterator.Spec()
				                      .setTarget(target)
				                      .setRootTemplate(ROOT_TEMPLATE)
				                      .setPatternTemplate(PATTERN_TEMPLATE));
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ABSTRACT);
  }
  
  protected static class AMetSocArticleIterator 
    extends SubTreeArticleIterator {
    
    protected static Pattern ABSTRACT_PATTERN = 
      Pattern.compile("/doi/abs/([.0-9]+/.+)$", Pattern.CASE_INSENSITIVE);
    
    public AMetSocArticleIterator(
        ArchivalUnit au, SubTreeArticleIterator.Spec spec) {
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
      /* if there is no "full" then we'll use this to pull out basic meta data */
      af.setRole(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
      guessAdditionalFiles(af, mat);
      
      return af;  
    }
    
    protected void guessAdditionalFiles(ArticleFiles af, Matcher mat) {      
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/doi/pdf/$1"));
      CachedUrl fullCu = au.makeCachedUrl(mat.replaceFirst("/doi/full/$1"));
      Boolean setFTCU = false;
      
      if (fullCu != null && fullCu.hasContent()) {
        af.setFullTextCu(fullCu);
        setFTCU = true;
        // this will now be a better bet than the abstract
        af.setRole(ArticleFiles.ROLE_ARTICLE_METADATA, fullCu);
      }
      if (pdfCu != null && pdfCu.hasContent()) {
        if (!setFTCU) {af.setFullTextCu(pdfCu);}
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
      AuUtil.safeRelease(pdfCu);
      AuUtil.safeRelease(fullCu);
    }
  }
  
}
