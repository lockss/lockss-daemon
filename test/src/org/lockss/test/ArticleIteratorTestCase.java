/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.io.*;
import java.util.*;
import java.net.*;
import java.util.regex.*;

import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.util.CIProperties;
import org.lockss.extractor.*;

/** Framework for ArticleIterator tests. */
public abstract class ArticleIteratorTestCase extends LockssTestCase {

  protected ArchivalUnit au;
  protected String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();
    MockLockssDaemon daemon = getMockLockssDaemon();
    PluginManager pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
  }

  public void tearDown() throws Exception {
    getMockLockssDaemon().stopDaemon();
    super.tearDown();
  }

  protected SubTreeArticleIterator createSubTreeIter() {
    Iterator<ArticleFiles> iter =  au.getArticleIterator(MetadataTarget.Any());
    assertNotNull("ArticleIterator is null", iter);
    if (iter instanceof SubTreeArticleIterator) {
      return (SubTreeArticleIterator)iter;
    }
    fail("ArticleIterator isn't a SubTreeArticleIterator: " + iter.getClass());
    return null;
  }

  protected Pattern getPattern(SubTreeArticleIterator iter) {
    try {
      return (Pattern)PrivilegedAccessor.getValue(iter, "pat");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected Collection<String> getRootUrls(SubTreeArticleIterator iter) {
    try {
      List<CachedUrlSet> roots =
	(List<CachedUrlSet>)PrivilegedAccessor.getValue(iter, "roots");
      List<String> res = new ArrayList<String>();
      for (CachedUrlSet cus : roots) {
	res.add(cus.getUrl());
      }
      return res;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected ArticleFiles createArticleFiles(SubTreeArticleIterator artIter,
					    CachedUrl cu) {
    try {
      return (ArticleFiles)PrivilegedAccessor.invokeMethod(artIter,
							   "createArticleFiles",
							   cu);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }
  
  protected void storeContent(InputStream input, CIProperties props, String url) throws IOException {
    UrlData ud = new UrlData(input, props, url);
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.storeContent();
  }

}
