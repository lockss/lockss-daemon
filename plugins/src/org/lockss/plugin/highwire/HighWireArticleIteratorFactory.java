/*
 * $Id: HighWireArticleIteratorFactory.java,v 1.13 2010-06-18 21:15:31 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;
import org.lockss.daemon.PluginException;

public class HighWireArticleIteratorFactory
  implements ArticleIteratorFactory,
	     ArticleMetadataExtractorFactory {
  static Logger log = Logger.getLogger("HighWireArticleIterator");

  /*
   * The HighWire URL structure means that the PDF for an article
   * is at a URL like http://apr.sagepub.com/cgi/reprint/34/2/135
   * where 34 is a volume name, 2 an issue name and 135 a page name.
   * In the best of cases all three are integers but they can all be
   * strings, e.g. OUP's English Historical Review uses Roman numerals
   * for volume names, many HighWire titles have supplementary issues
   * named supp_1, supp_2, etc. and most APS journals have page names
   * prepended with a letter reminiscent of the journal title's main
   * keyword.
   */
  protected String root = "\"%scgi/reprint\",base_url";
  protected Pattern pat = Pattern.compile("/[^/]+/[^/]+/[^/]+",
					  Pattern.CASE_INSENSITIVE);

  private final String reprintPrefix = "/cgi/reprint/";
  private final String reprintframedPrefix = "/cgi/reprintframed/";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    return new SubTreeArticleIterator(au,
				      new SubTreeArticleIterator.Spec()
				      .setTarget(target)
				      .setRootTemplate(root)
				      .setPattern(pat)) {
      protected ArticleFiles createArticleFiles(CachedUrl cu) {
	ArticleFiles res = new ArticleFiles();
	res.setFullTextCu(cu);
	// cu points to a file whose name is .../main.pdf
	// but the DOI we want is in a file whose name is .../main.xml
	// The rest of the metadata is in the dataset.toc file that
	// describes the package in which the article was delivered.

	String reprintUrl = cu.getUrl();
	if (reprintUrl.contains(reprintPrefix)) {
	  String reprintframedUrl =
	    reprintUrl.replaceFirst(reprintPrefix, reprintframedPrefix);
	  CachedUrl reprintframedCu =
	    cu.getArchivalUnit().makeCachedUrl(reprintframedUrl);
	  try {
	    if (reprintframedCu != null && reprintframedCu.hasContent()) {
	      res.setRoleCu("reprintFramed", reprintframedCu);
	    }
	  } finally {
	    AuUtil.safeRelease(reprintframedCu);
	  }
	}
	return res;
      }
    };
  }

  public ArticleMetadataExtractor
    createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new HighWireArticleMetadataExtractor();
  }

  public static class HighWireArticleMetadataExtractor
    implements ArticleMetadataExtractor {

    public ArticleMetadata extract(ArticleFiles af)
	throws IOException, PluginException {
      CachedUrl cu = af.getRoleCu("reprintFramed");
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
