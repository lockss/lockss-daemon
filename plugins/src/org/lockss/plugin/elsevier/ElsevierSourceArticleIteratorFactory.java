/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.elsevier;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ElsevierSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(ElsevierSourceArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%s%d\",base_url,year";
  
  protected static final String PATTERN_TEMPLATE = "\"%s%d/[^/]+/[\\d]+\\.tar!/[\\d]+/[\\dX]+/main.pdf$\",base_url,year";

//  protected static final String INCLUDE_SUBTREE_TEMPLATE = "\"%s%d/[^/]+/[\\d]+\\.tar!/[\\d]+/[\\d]+/main.pdf$\",base_url,year";
  protected static final String NESTED_ARCHIVE_PATTERN_TEMPLATE = "\"%s%d/[^/]+/[\\d]+\\.tar!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$\",base_url,year";

  
  // example file names:
  //http://clockss-ingest.lockss.org/sourcefiles/elsevier-released/2012/OXM30010/dataset.toc
  //http://clockss-ingest.lockss.org/sourcefiles/elsevier-released/2012/OXM30010/00029343.tar!/01250008/12000332/main.pdf
  //http://clockss-ingest.lockss.org/sourcefiles/elsevier-released/2012/OXM30010/00029343.tar!/01250008/12000332/main.xml
  // could be ...tar!/00220004/1400079X/main.pdf - with X in 2nd to last directory
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new ElsevierSourceArticleIterator(au, new SubTreeArticleIterator.Spec()
                                       .setTarget(target)
                                       .setVisitArchiveMembers(true)
                                       .setRootTemplate(ROOT_TEMPLATE)
                                       .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
                                       .setExcludeSubTreePatternTemplate(NESTED_ARCHIVE_PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
//                                       .setIncludeSubTreePatternTemplate(INCLUDE_SUBTREE_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  /*
   * The file metadata extractor is called when a 
   * base_url/year/TAR_DIR/TARNUM.tar!/[\\d]+/[\\dX]+/main.pdf is found
   * The first one will use the URL pattern to discover the 
   * base_url/year/TAR_DIR/dataset.toc
   * and will extract all necessary metadata information from this unarchived file
   * for every file within every tar archive living in the TAR_DIR subdirectory.
   */
  
  protected static class ElsevierSourceArticleIterator extends SubTreeArticleIterator {
	 
    protected static Pattern PATTERN = Pattern.compile("([^/]+[\\d]+)(/[\\d]+[^/]+/[\\d]+/[\\dX]+/main.)pdf$", Pattern.CASE_INSENSITIVE);
    
    protected ElsevierSourceArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    // Duplicate of definition in ArticleFiles for use here until 1.59 is ready (PJG).
    // now using ArticleFiles.ROLE_FULL_TEXT_XML

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

    protected ArticleFiles processFullText(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      
      if(spec.getTarget() != MetadataTarget.Article())
      {
		af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);
		guessXml(af,mat);
      }
      
      return af;
    }
    
    protected void guessXml(ArticleFiles af, Matcher mat) {
        CachedUrl xmlCu = au.makeCachedUrl(mat.replaceFirst("$1$2xml"));
       
        if (xmlCu != null && xmlCu.hasContent()) {
          af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_XML, xmlCu);
        }
      } 
    
    // Now "guessing" the dataset.toc file to set the ROLE_ARTICLE_METADATA
    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("$1/dataset.toc"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);
      }
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
	      throws PluginException {
	    return new ElsevierSourceArticleMetadataExtractor();
  }
}
