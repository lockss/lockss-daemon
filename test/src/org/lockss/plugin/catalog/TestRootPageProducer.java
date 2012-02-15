/*
 * $Id: TestRootPageProducer.java,v 1.1.2.2 2012-02-15 17:51:34 nchondros Exp $
 */

/*

 Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.catalog;

import java.util.*;
import java.security.*;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestRootPageProducer extends LockssTestCase {
  private static final String checksumAlgorithm = "SHA-1";

  static String mockPlugKey = PluginManager.pluginKeyFromName(MockPlugin.class.getName());
  private MockLockssDaemon theDaemon;
  private ArchivalUnit testau[], catalogAU;
  private MessageDigest checksumProducer = null;

  private static final String[] BASE_URL = { "http://www.test0.org/",
      "http://www.test1.org/", "http://www.test2.org/" };

  // BASE_URL
  private static String[] urls = { "lockssau:", "%s", "%sindex.html",
      "%sfile1.html", "%sfile2.html", "%sbranch1/", "%sbranch1/index.html",
      "%sbranch1/file1.html", "%sbranch1/file2.html", "%sbranch2/",
      "%sbranch2/index.html", "%sbranch2/file1.html", "%sbranch2/file2.html", };

  public void setUp() throws Exception {
    super.setUp();
    checksumProducer = MessageDigest.getInstance(checksumAlgorithm);
    theDaemon = getMockLockssDaemon();
    theDaemon.getPluginManager();
    theDaemon.setDaemonInited(true);

    TimeBase.setSimulated();
    testau = new ArchivalUnit[BASE_URL.length];
    for (int i = 0; i < BASE_URL.length; i++) {
      testau[i] = setupAu(String.format("mock%d", i),
          String.format("MockAU%d", i), BASE_URL[i]);
      setupRepo(testau[i], i, BASE_URL[i]);
    }
    catalogAU = setupAu("catalog", "CatalogAU", "");
    setupRepo(catalogAU, BASE_URL.length, "");
  }

  private MockArchivalUnit setupAu(String id, String name, String base_url) {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId(id);
    mau.setName(name);
    PluginTestUtil.registerArchivalUnit(mau);

    MockCachedUrlSet cus = (MockCachedUrlSet) mau.getAuCachedUrlSet();
    cus.setEstimatedHashDuration(1000);
    List files = new ArrayList();
    if (!base_url.isEmpty()) {
      for (int ix = 0; ix < urls.length; ix++) {
        String url = String.format(urls[ix], base_url);
        MockCachedUrl cu = (MockCachedUrl) mau.addUrl(url,
            "This is content for CUS file " + url);
        cu.getProperties().put(CachedUrl.PROPERTY_CHECKSUM, checksum(url));
        files.add(cu);
      }
    }
    cus.setHashItSource(files);
    return mau;
  }

  private void setupRepo(ArchivalUnit au, int i, String base_url)
      throws Exception {
    MockLockssRepository repo = new MockLockssRepository(String.format(
        "/foo%d", i), au);
    if (!base_url.isEmpty()) {
      for (int ix = 0; ix < urls.length; ix++) {
        String url = String.format(urls[ix], base_url);
        repo.createNewNode(url);
      }
    }
    ((MockLockssDaemon) theDaemon).setLockssRepository(repo, au);
  }

  public void tearDown() throws Exception {
    for (ArchivalUnit au : testau)
      theDaemon.getLockssRepository(au).stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  public void testProducer() throws Exception {
    // RootPageProducer.produce(theDaemon, catalogAU, null);
  }

  private String checksum(String url) {
    // for lack of content, just do a checksum of the url itself
    checksumProducer.reset();
    checksumProducer.update(url.getBytes());
    byte[] bchecksum = checksumProducer.digest();
    return ByteArray.toHexString(bchecksum);
  }
}
