/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.emerald;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class EmeraldArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("EmeraldArticleIteratorFactory");
  
  // example: http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle
  protected static final String ROOT_TEMPLATE = "\"%s\",base_url";
  protected static final String PATTERN_TEMPLATE = "\"%s(journals|books)\\.htm\\?issn=%s&volume=%s.*&(articleid|chapterid)=[^&]*&show=(html.*$)\", base_url, journal_issn, volume_name";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new EmeraldArticleIterator(au, new SubTreeArticleIterator.Spec()
                                      .setTarget(target)
                                      .setRootTemplate(ROOT_TEMPLATE)
                                      .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class EmeraldArticleIterator extends SubTreeArticleIterator {
    // example: http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle
    // ending pattern matches html and optional '&view=printarticle'
    protected static Pattern PATTERN = Pattern.compile("((journals|books).htm\\?issn=[\\d]+-[\\d]+\\&volume=[\\d]+(\\&issue=[\\d]+)?\\&(articleid|chapterid)=[^\\&]+\\&show=)(html.*$)", Pattern.CASE_INSENSITIVE);
    
    protected EmeraldArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
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

    /**
     * Fill in full-text HTML and full-text roles if an html article page exists like:
     * http://www.emeraldinsight.com/journals.htm?articleid=1677014&show=html
     * 
     * @param cu the CachedUrl
     * @param mat the matcher
     */
    protected ArticleFiles processFullText(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, cu);
   
      // fill in other roles if not merely counting articles
      if(spec.getTarget() != MetadataTarget.Article) {
	guessPdfFile(af, mat);
	guessAbstractFile(af, mat);
      }
      
      return af;
    }
    
    /**
     * Fill in full-text PDF role if a related pdf article page exists like:
     * http://www.emeraldinsight.com/journals.htm?articleid=1677014&show=pdf
     * 
     * @param af the ArticleFiles
     * @param mat the matcher
     */
    protected void guessPdfFile(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("$1pdf"));
      
      if (pdfCu != null && pdfCu.hasContent()) {
          af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
    }

    /**
     * Fill in abstract role if a related abstract page exists like:
     * http://www.emeraldinsight.com/journals.htm?articleid=1677014&show=abstract
     * 
     * @param af the ArticleFiles
     * @param mat the Matcher
     */
    protected void guessAbstractFile(ArticleFiles af, Matcher mat) {
      CachedUrl abstractCu = au.makeCachedUrl(mat.replaceFirst("$1abstract"));
      
      if (abstractCu != null && abstractCu.hasContent()) {
    	  af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, abstractCu);
      }
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
	      throws PluginException {
	    return new BaseArticleMetadataExtractor();
  }
}
