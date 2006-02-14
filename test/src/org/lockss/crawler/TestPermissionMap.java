/*
 * $Id: TestPermissionMap.java,v 1.5 2006-02-14 05:19:49 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;
import java.util.ArrayList;
import java.io.*;

import org.lockss.daemon.Crawler;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;

public class TestPermissionMap extends LockssTestCase {
  private PermissionMap pMap;
  private String permissionUrl1 = "http://www.example.com/index.html";
  private String url1 = "http://www.example.com/link1.html";


  public void setUp() throws Exception {
    super.setUp();

    getMockLockssDaemon().getAlertManager(); //populates AlertManager

    pMap = new PermissionMap(new MockArchivalUnit(),
                             new MockPermissionHelper(),
			     new ArrayList(), null);
    pMap.putStatus(permissionUrl1, PermissionRecord.PERMISSION_OK);
  }

  public void testConstructorNullAu() {
    try {
      new PermissionMap(null, new MockPermissionHelper(), null, null);
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      //expected
    }
  }

  public void testConstructorNullCrawler() {
    try {
      new PermissionMap(new MockArchivalUnit(), null, null, null);
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      //expected
    }
  }

  // XXX unfinished
  public void testCheckPermission() throws Exception {
    MockPermissionChecker checker1 = new MockPermissionChecker(1);
    MockPermissionChecker checker2 = new MockPermissionChecker(1);
    MyPermissionHelper helper = new MyPermissionHelper();
    PermissionMap map = new PermissionMap(new MockArchivalUnit(),
					  helper,
					  ListUtil.list(checker1),
					  checker2);
  }

  // XXX unfinished
  class MyPermissionHelper extends MockPermissionHelper {
    public UrlCacher makeUrlCacher(String url) {
      
      return new MockUrlCacher("foo", new MockArchivalUnit());
    }

    public BufferedInputStream resetInputStream(BufferedInputStream is,
						String url) {
      throw new UnsupportedOperationException("not implemented");
    }

    public void refetchPermissionPage(String url) {
      throw new UnsupportedOperationException("not implemented");
    }

    public Crawler.Status getCrawlStatus() {
      return new Crawler.Status(null, null, null);
    }

  }

  public void testGetPermissionUrl() throws Exception {
    assertEquals(permissionUrl1, pMap.getPermissionUrl(url1));
  }

  public void testGetStatus()throws Exception {
    assertEquals(PermissionRecord.PERMISSION_OK, pMap.getStatus(url1));
  }

  public void testPutMoreStatus()throws Exception {
    String permissionUrl2 = "http://www.foo.com/index.html";
    String url2 = "http://www.foo.com/link2.html";

    pMap.putStatus(permissionUrl2, PermissionRecord.PERMISSION_NOT_OK);
    assertEquals(permissionUrl2, pMap.getPermissionUrl(url2));
    assertEquals(PermissionRecord.PERMISSION_NOT_OK, pMap.getStatus(url2));

    assertEquals(permissionUrl1, pMap.getPermissionUrl(url1));
    assertEquals(PermissionRecord.PERMISSION_OK, pMap.getStatus(url1));
  }

  public void testPutStatusOverwrite() throws Exception {
    pMap.putStatus(permissionUrl1, PermissionRecord.FETCH_PERMISSION_FAILED);
    assertEquals(PermissionRecord.FETCH_PERMISSION_FAILED, pMap.getStatus(url1));
  }
}
