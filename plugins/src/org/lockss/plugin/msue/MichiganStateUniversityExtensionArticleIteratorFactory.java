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
	  Logger.getLogger(MichiganStateUniversityExtensionArticleIteratorFactory.class);

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
