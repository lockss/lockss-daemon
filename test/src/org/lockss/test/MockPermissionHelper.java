/*
 * $Id: MockPermissionHelper.java,v 1.3 2007-01-22 22:11:27 troberts Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
nto use, copy, modify, merge, publish, distribute, sublicense, and/or sell
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

import org.lockss.daemon.Crawler;
import org.lockss.daemon.Crawler.PermissionHelper;
import org.lockss.crawler.*;
import org.lockss.plugin.*;

public class MockPermissionHelper implements PermissionHelper {

    public UrlCacher makeUrlCacher(String url) {
    throw new UnsupportedOperationException("not implemented");
  }

  public BufferedInputStream resetInputStream(BufferedInputStream is,
                                              String url) throws IOException {
    throw new UnsupportedOperationException("not implemented");
  }

  public void refetchPermissionPage(String url) {
    throw new UnsupportedOperationException("not implemented");
  }

  public CrawlerStatus getCrawlStatus() {
    return new CrawlerStatus(null, null, null);
  }

}
