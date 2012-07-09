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
