/*
 * $Id$
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

package org.lockss.plugin.msue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class MichiganStateUniversityExtensionArticleIteratorFactory
  implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log =
	  Logger.getLogger("MichiganStateUniversityExtensionArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
	  "\"^%sBulletins/Bulletin/PDF/Historical/finished_pubs/e[0-9]+/e[a-zA-Z0-9]+\\.pdf\", base_url";

  protected static HashSet<String> emittedSet = new HashSet<String>();
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new MichiganStateUniversityExtensionArticleIterator(au,
                                        new SubTreeArticleIterator.Spec()
                                        .setTarget(target)
                                        .setRootTemplate(ROOT_TEMPLATE)
                                        .setPatternTemplate(PATTERN_TEMPLATE),
                                        target);
  }
 
  protected static class MichiganStateUniversityExtensionArticleIterator
    extends SubTreeArticleIterator {
    
    protected static Pattern PDF_PATTERN =
      Pattern.compile("e[a-zA-Z0-9]+\\.pdf$",	Pattern.CASE_INSENSITIVE);
    
    public MichiganStateUniversityExtensionArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec,
                                  MetadataTarget target) {
      super(au, spec);
      emittedSet = new HashSet<String>();
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {   
      Matcher mat = PDF_PATTERN.matcher(cu.getUrl());
      if (mat.find()) {
        return processFullTextPdf(cu, mat);
      }
      
      log.warning("Mismatch between article iterator factory" +
        " and article iterator: " + cu);
      return null;
    }

    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {  
      ArticleFiles af = new ArticleFiles();
      
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setFullTextCu(pdfCu);
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
      
      guessMetadataFile(af, pdfMat);
      return af;
    }
    
    private void guessMetadataFile(ArticleFiles af, Matcher mat) {
      CachedUrl metadataCu = au.makeCachedUrl(mat.replaceFirst("index.html"));
      if(metadataCu != null && metadataCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, metadataCu);
      }
    }
  }

  @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
        throws PluginException {
      return new MichiganStateUniversityExtensionArticleMetadataExtractor(emittedSet);
    }
}
