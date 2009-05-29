/*
 * $Id: NatureArticleIteratorFactory.java,v 1.1 2009-05-29 03:30:39 dshr Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.config.*;
import org.lockss.daemon.PluginException;

public class NatureArticleIteratorFactory implements ArticleIteratorFactory {
  static Logger log = Logger.getLogger("NatureArticleIteratorFactory");

  /*
   * The Nature URL structure means that the HTML for an article is
   * at a URL like http://www.nature.com/gt/journal/v16/n5/full/gt200929a.html
   * ie <base_url>/<journal_id>/journal/v<volume> is the subtree we want.
   */
  protected String subTreeRoot;

  public NatureArticleIteratorFactory() {
  }
  /**
   * Create an Iterator that iterates through the AU's articles, pointing
   * to the appropriate CachedUrl of type mimeType for each, or to the plugin's
   * choice of CachedUrl if mimeType is null
   * @param mimeType the MIME type desired for the CachedUrls
   * @param au the ArchivalUnit to iterate through
   * @return the ArticleIterator
   */
  public Iterator createArticleIterator(String mimeType, ArchivalUnit au)
      throws PluginException {
    String jid;
    jid = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
    int vol;
    try {
      vol = au.getConfiguration().getInt(ConfigParamDescr.VOLUME_NUMBER.getKey());
    } catch (Configuration.InvalidParam ex) {
       throw new PluginException("NatureArticleIteratorFactory: " + ex);
    }
    subTreeRoot = jid + "/journal/v" + vol;
    log.debug("createArticleIterator(" + mimeType + "," + au.toString() +
              ") " + subTreeRoot);
    return new SubTreeArticleIterator(mimeType, au, subTreeRoot);
  }
}
