/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.File;
import java.net.MalformedURLException;
import org.lockss.daemon.*;
import org.lockss.test.LockssTestCase;
import org.lockss.plugin.GenericFileCachedUrlSet;
import org.lockss.repository.TestLockssRepositoryImpl;

public class TestHighWireArchivalUnit extends LockssTestCase {
  private static final String cStart =
    "http://shadow1.stanford.edu/lockss-volume322.shtml";
  private static final String cRoot = "http://shadow1.stanford.edu/";

  public TestHighWireArchivalUnit(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestLockssRepositoryImpl.configCacheLocation(tempDirPath);
  }

  public void testGetUrlVolumeNumberNullUrl() {
    try {
      HighWireArchivalUnit.getUrlVolumeNumber(null);
      fail("Should have thrown MalformedURLException");
    } catch(MalformedURLException mue) { }
  }

  public void testGetUrlvolumeNumberNonRootUrl() throws MalformedURLException {
    String url = "http://shadow8.stanford.edu/";
    assertEquals(-1, HighWireArchivalUnit.getUrlVolumeNumber(url));
  }

  public void testGetUrlvolumeNumberRootUrl() throws MalformedURLException {
    String url = "http://shadow8.stanford.edu/lockss-volume327.shtml";
    assertEquals(327, HighWireArchivalUnit.getUrlVolumeNumber(url));
  }

  public void testShouldCacheRootPage() throws Exception {
    ArchivalUnit hwAu = new HighWireArchivalUnit(cStart);
    CachedUrlSetSpec spec = new RECachedUrlSetSpec(cRoot);
    GenericFileCachedUrlSet cus = new GenericFileCachedUrlSet(hwAu, spec);
    UrlCacher uc =
      cus.makeUrlCacher("http://shadow1.stanford.edu/lockss-volume322.shtml");
    assertTrue(uc.shouldBeCached());
  }

  public void testShouldNotCachePageFromOtherSite() throws Exception {
    ArchivalUnit hwAu = new HighWireArchivalUnit(cStart);
    CachedUrlSetSpec spec = new RECachedUrlSetSpec(cRoot);
    GenericFileCachedUrlSet cus = new GenericFileCachedUrlSet(hwAu, spec);
    UrlCacher uc =
      cus.makeUrlCacher("http://shadow2.stanford.edu/lockss-volume322.shtml");
    assertTrue(!uc.shouldBeCached());
  }
}
