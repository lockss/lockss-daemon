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

package org.lockss.plugin.liberquarterly;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * There are a few idiosyncracies worth mentioning here that this class introduces.
 * First, one should know that it classifies an article's abstract page as the 
 * article's access page, because it is a landing page to the html and pdf versions
 * as well as the only url of the three which can be deterministically matched and
 * distinguished from the others.  
 * 
 * The abstract page will look like:
 * 		~~~~~~~~/XXXX/0 where ~~~~~~ is some base url and XXXX is the article identifier
 * The html article page will look like:
 * 		~~~~~~~~/XXXX/YYYY where YYYY is a different identifier (it is a number)
 * The pdf article page will look like:
 * 		~~~~~~~~/XXXX/YYYY+1 where YYYY+1 = YYYY (a number) + 1 (ie not 
 * 		concatenated with YYYY but added
 * 
 * All three pages contain the article's metadata, so matching on the abstract page
 * is sufficient for the metadata extractor's needs for now.  However, if some
 * functionality is added later which deals with the ArticleFiles on a more detailed
 * level, this class may need to be changed, since strictly, the abstract page is
 * not the full text cu, although it is stored as such here for use by the 
 * metadata extractor.
 */

public class LiberQuarterlyArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("LiberQuarterlyArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%sindex.php/%s/article/view\", base_url, journal_id";
  protected static final String PATTERN_TEMPLATE = "\"%sindex.php/%s/article/view/[\\d]+/0$\", base_url, journal_id";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new LiberQuarterlyArticleIterator(au, new SubTreeArticleIterator.Spec()
                                       .setTarget(target)
                                       .setRootTemplate(ROOT_TEMPLATE)
                                       .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class LiberQuarterlyArticleIterator extends SubTreeArticleIterator {
	 
    protected static Pattern PATTERN = Pattern.compile("(index.php/[^/]+/article/view/[\\d]+/)([\\d]+)", Pattern.CASE_INSENSITIVE);
    
    protected LiberQuarterlyArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      
      Matcher mat = PATTERN.matcher(url);
      log.debug3(cu.getContentType());
      if (mat.find()) {
        return processFullText(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullText(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      
      af.setFullTextCu(cu);
      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
      
      return af;
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
	      throws PluginException {
	    return new BaseArticleMetadataExtractor();
  }
}
