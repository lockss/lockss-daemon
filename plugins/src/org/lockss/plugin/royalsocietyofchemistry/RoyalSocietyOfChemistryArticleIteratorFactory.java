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

package org.lockss.plugin.royalsocietyofchemistry;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class RoyalSocietyOfChemistryArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("RoyalSocietyOfChemistryArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%s\", resolver_url";
  
  protected static final String PATTERN_TEMPLATE = "\"^%s\\?DOI=\", resolver_url";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    return new RoyalSocietyOfChemistryArticleIterator(au, new SubTreeArticleIterator.Spec()
                                                          .setTarget(target)
                                                          .setRootTemplate(ROOT_TEMPLATE)
                                                          .setPatternTemplate(PATTERN_TEMPLATE));
  }

  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }
  
  protected static class RoyalSocietyOfChemistryArticleIterator extends SubTreeArticleIterator {
    
    protected static final String ROLE_ARTICLE_CODE = "ArticleCode";

    protected static final Pattern PATTERN = Pattern.compile("/\\?doi=(.+)$", Pattern.CASE_INSENSITIVE);
    
    protected String baseUrl;
    
    protected String journalCode;
    
    protected String year;
    
    public RoyalSocietyOfChemistryArticleIterator(ArchivalUnit au,
                                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      this.baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      this.journalCode = au.getConfiguration().get("journal_code");
      this.year = au.getConfiguration().get(ConfigParamDescr.YEAR.getKey());
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;
      
      mat = PATTERN.matcher(url);
      if (mat.find()) {
        return processUrl(cu, mat);
      }
        
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processUrl(CachedUrl xlinkCu, Matcher xlinkMat) {
      ArticleFiles af = new ArticleFiles();
      //af.setRoleString(ROLE_ARTICLE_CODE, xlinkMat.group(1));// causes a metadata pprint exception casting to CU and it's not used
      guessAbstract(af, xlinkMat);
//      guessFullTextPdf(af, xlinkMat);
//      guessOtherParts(af, xlinkMat);
      chooseFullTextCu(af, xlinkCu);
      return af;
    }
    
    protected void chooseFullTextCu(ArticleFiles af, CachedUrl defaultCu) {
      String[] candidates = new String[] {
          ArticleFiles.ROLE_FULL_TEXT_PDF,
          ArticleFiles.ROLE_ABSTRACT,
      };
      for (String key : candidates) {
        CachedUrl cu = af.getRoleCu(key);
        if (cu != null) {
          af.setFullTextCu(cu);
          return;
        }
      }
      af.setFullTextCu(defaultCu);
    }    
    
    protected void guessAbstract(ArticleFiles af, Matcher xlinkMat) {
      CachedUrl absCu = au.makeCachedUrl(String.format("%spublishing/journals/%s/article.asp?doi=%s", baseUrl, journalCode.toUpperCase(), xlinkMat.group(1)));
      // If more than one variant, add nested if's here; see guessFullTextPdf()
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA,  absCu); // there isn't much, but this is where we'll get it
      }
      else {
        //log.warning("Could not infer abstract URL for article code " + af.getRoleString(ROLE_ARTICLE_CODE));
        log.warning("Could not infer abstract URL for article code ");
      }
    }

//    protected void guessFullTextPdf(ArticleFiles af, Matcher xlinkMat) {
//      CachedUrl pdfCu = au.makeCachedUrl(String.format("%sej/%s/%s/%s.pdf", baseUrl, journalCode.toUpperCase(), year, xlinkMat.group(1)));
//      if (pdfCu == null || !pdfCu.hasContent()) {
//        pdfCu = au.makeCachedUrl(String.format("%sdelivery/_ArticleLinking/DisplayArticleForFree.cfm?doi=%s&JournalCode=%s", baseUrl, xlinkMat.group(1), journalCode.toUpperCase()));
//        // Add more similar nested if's for other variants
//      }
//      if (pdfCu != null && pdfCu.hasContent()) {
//        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
//      }
//      else {
//        log.warning("Could not infer full text PDF URL for article code " + af.getRoleString(ROLE_ARTICLE_CODE));
//      }
//    }
//
//    protected void guessOtherParts(ArticleFiles af, Matcher xlinkMat) {
//      guessReferences(af, xlinkMat);
//      guessSupplementaryMaterials(af, xlinkMat);
//    }
//    
//    protected void guessReferences(ArticleFiles af, Matcher xlinkMat) {
//      CachedUrl refCu = au.makeCachedUrl(String.format("%s_ArticleLinking/ArticleLinking.cfm?JournalCode=%s&Year=%s&ManuscriptID=%s&type=citonly", baseUrl, journalCode.toUpperCase(), year, xlinkMat.group(1)));
//      // If more than one variant, add nested if's here; see guessFullTextPdf()
//      if (refCu != null && refCu.hasContent()) {
//        af.setRoleCu(ArticleFiles.ROLE_REFERENCES, refCu);
//      }
//    }
//    
//    protected void guessSupplementaryMaterials(ArticleFiles af, Matcher xlinkMat) {
//      // http://www.rsc.org/suppdata/LC/b7/b703810k/index.sht
//      CachedUrl suppCu = au.makeCachedUrl(String.format("%ssuppdata/%s/%s/%s/index.sht", baseUrl, journalCode.toUpperCase(), xlinkMat.group(1).substring(0,2), xlinkMat.group(1)));
//      if (suppCu != null && suppCu.hasContent()) {
//        af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, suppCu);
//      }
//    }

  }
  
}
