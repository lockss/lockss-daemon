/*
 * $Id: NatureArticleIteratorFactory.java,v 1.8 2010-06-22 01:00:17 thib_gc Exp $
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

package org.lockss.plugin.nature;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;
import org.lockss.daemon.PluginException;

public class NatureArticleIteratorFactory
  implements ArticleIteratorFactory,
	     ArticleMetadataExtractorFactory {
  
  protected static final String ARTICLE_FILES_KEY_PDF = "full-text PDF";

  static Logger log = Logger.getLogger("NatureArticleIteratorFactory");

  protected static Pattern patHtml = Pattern.compile("/full/([^/]+)\\.html$", Pattern.CASE_INSENSITIVE);
  protected static Pattern patPdf = Pattern.compile("/pdf/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  
  /*
   * The Nature URL structure means that the HTML for an article is
   * at a URL like http://www.nature.com/gt/journal/v16/n5/full/gt200929a.html
   * ie <base_url>/<journal_id>/journal/v<volume> is the subtree we want.
   */
  public Iterator<ArticleFiles> createArticleIterator(final ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    String rootTpl = "\"%s%s/journal/v%s/\", base_url, journal_id, volume_name";
    String patTpl = "\"^%s%s/journal/v[^/]+/n[^/]+/(full|pdf)/[^/]+\\.(html|pdf)$\", base_url, journal_id, volume_name";
    
    return new SubTreeArticleIterator(au,
				      new SubTreeArticleIterator.Spec()
				      .setTarget(target)
				      .setRootTemplate(rootTpl)
				      .setPatternTemplate(patTpl)) {
      @Override
      protected ArticleFiles createArticleFiles(CachedUrl cu) {
        String url = cu.getUrl();
        ArticleFiles af;
        Matcher mat = null;
        
        mat = patHtml.matcher(url);
        if (mat.find()) {
          af = new ArticleFiles();
          af.setFullTextCu(cu);
          CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/pdf/$1.pdf"));
          if (pdfCu != null && pdfCu.hasContent()) {
            af.setRoleCu(ARTICLE_FILES_KEY_PDF, pdfCu);
          }
          return af;
        }

        mat = patPdf.matcher(url);
        if (mat.find()) {
          CachedUrl htmlCu = au.makeCachedUrl(mat.replaceFirst("/full/$1.html"));
          if (htmlCu != null && htmlCu.hasContent()) {
            return null;
          }
          af = new ArticleFiles();
          af.setFullTextCu(cu);
          af.setRoleCu(ARTICLE_FILES_KEY_PDF, cu);
          return af;
        }
        
        log.siteWarning("Mismatch between path and extension: " + url);
        return null;
      }
    };
  }

  public ArticleMetadataExtractor
    createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new NatureArticleMetadataExtractor();
  }

  public class NatureArticleMetadataExtractor
    implements ArticleMetadataExtractor {

    public ArticleMetadata extract(ArticleFiles af)
	throws IOException, PluginException {
      CachedUrl cu = af.getFullTextCu();
      if (cu != null) {
	FileMetadataExtractor me = cu.getFileMetadataExtractor();
	if (me != null) {
	  return me.extract(cu);
	}
      }
      return null;
    }
  }
}
