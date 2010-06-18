/*
 * $Id: BePressArticleIteratorFactory.java,v 1.7 2010-06-18 21:15:30 thib_gc Exp $
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

package org.lockss.plugin.bepress;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;
import org.lockss.daemon.PluginException;

public class BePressArticleIteratorFactory
  implements ArticleIteratorFactory,
	     ArticleMetadataExtractorFactory {
  static Logger log = Logger.getLogger("BePressArticleIteratorFactory");

  /*
   * The BePress URL structure means that the HTML for an article
   * is normally at a URL like http://www.bepress.com/bis/vol3/iss3/art7
   * but is sometimes at a URL like
   * http://www.bepress.com/bejte/frontiers/vol1/iss1/art1 where "frontiers"
   * is an apparently arbitrary word.  So for now we just use the journal
   * abbreviation as the subTreeRoot.
   */
  protected String subTreeRoot = "\"%s%s\",base_url, journal_abbr";

  static final String pat =
    "\"%s%s/((default/)?(vol)?%d/(iss)?[0-9]+/(art|editorial)?[0-9]+|vol%d/[A-Z][0-9]+)$\",base_url, journal_abbr, volume, volume";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    String journal_abbr =
      au.getConfiguration().get(ConfigParamDescr.JOURNAL_ABBR.getKey());
    return new SubTreeArticleIterator(au,
				      new SubTreeArticleIterator.Spec()
				      .setTarget(target)
				      .setRootTemplate(subTreeRoot)
				      .setPatternTemplate(pat));
  }
  
  public ArticleMetadataExtractor
    createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BePressArticleMetadataExtractor();
  }

  public class BePressArticleMetadataExtractor
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
