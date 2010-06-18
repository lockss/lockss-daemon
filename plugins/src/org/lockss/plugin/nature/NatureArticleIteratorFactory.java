/*
 * $Id: NatureArticleIteratorFactory.java,v 1.7 2010-06-18 21:15:31 thib_gc Exp $
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
  static Logger log = Logger.getLogger("NatureArticleIteratorFactory");

  /*
   * The Nature URL structure means that the HTML for an article is
   * at a URL like http://www.nature.com/gt/journal/v16/n5/full/gt200929a.html
   * ie <base_url>/<journal_id>/journal/v<volume> is the subtree we want.
   */
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    String rootPat = "\"%s%s/journal/v%s\", base_url, journal_id, volume_name";
    Pattern pat = Pattern.compile("journal/v[^/]+/n[^/]+/full/",
				  Pattern.CASE_INSENSITIVE);
    
    return new SubTreeArticleIterator(au,
				      new SubTreeArticleIterator.Spec()
				      .setTarget(target)
				      .setRootTemplate(rootPat)
				      .setPattern(pat));
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
