/*
 * $Id: FuncWrappedSimulatedContent.java,v 1.3 2004-06-10 22:03:54 tyronen Exp $
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

package org.lockss.plugin.wrapper;

import java.io.*;
import java.security.*;
import java.util.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.repository.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.util.*;
import junit.framework.*;

/**
 * Adapted from FuncSimulatedContent; repeats much of it but using the wrapped
 * classes instead
 */
public class FuncWrappedSimulatedContent extends LockssTestCase {
  private SimulatedArchivalUnit sau;
  private WrappedArchivalUnit wau;
  private WrappedPlugin wplug;
  private MockLockssDaemon theDaemon;

  public FuncWrappedSimulatedContent(String msg) {
    super(msg);
  }

  private void addToBuf(StringBuffer buf, String propname, String propval) {
    buf.append(propname);
    buf.append('=');
    buf.append(propval);
    buf.append('\n');
  }

  private void addAuConfigToBuf(StringBuffer buf, String auId, String tempDirPath) {
    String pauId = "org.lockss.au." + auId + '.';
    addToBuf(buf, pauId + "root", tempDirPath);
    addToBuf(buf, pauId + "depth", "2");
    addToBuf(buf, pauId + "branch", "2");
    addToBuf(buf, pauId + "numFiles", "2");
    addToBuf(buf, pauId + "reserved.wrapper", "true");
  }

  private String makeConfig() throws IOException {
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String tempDirPath2 = getTempDir().getAbsolutePath() + File.separator;
    String auId = "org|lockss|plugin|simulated|SimulatedPlugin.root~" +
        PropKeyEncoder.encode(tempDirPath);
    String auId2 = "org|lockss|plugin|simulated|SimulatedPlugin.root~" +
        PropKeyEncoder.encode(tempDirPath2);
    StringBuffer buf = new StringBuffer();
    addToBuf(buf, SystemMetrics.PARAM_HASH_TEST_DURATION, "1000");
    addToBuf(buf, SystemMetrics.PARAM_HASH_TEST_BYTE_STEP, "1024");
    addToBuf(buf, LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    addToBuf(buf, HistoryRepositoryImpl.PARAM_HISTORY_LOCATION, tempDirPath);
    addAuConfigToBuf(buf,auId,tempDirPath);
    addAuConfigToBuf(buf,auId2,tempDirPath);
    return buf.toString();
  }

  public void setUp() throws Exception {
    super.setUp();

    ConfigurationUtil.setCurrentConfigFromString(makeConfig());
    theDaemon = new MockLockssDaemon();
    theDaemon.setDaemonInited(true);
    init();
  }

  void init() {
   MockSystemMetrics metrics = new MockSystemMetrics();
   metrics.initService(theDaemon);
   theDaemon.setSystemMetrics(metrics);
   PluginManager pmgr = theDaemon.getPluginManager();
    Collection coll = pmgr.getAllAus();
    Iterator it = coll.iterator();
    while (it.hasNext()) {
      Object au = it.next();
      if (au instanceof WrappedArchivalUnit) {
        wau = (WrappedArchivalUnit) au;
      }
    }
    assertNotNull(wau);
    sau = (SimulatedArchivalUnit)wau.getOriginal();
    wplug = (WrappedPlugin)wau.getPlugin();
    pmgr.startService();

    theDaemon.getHistoryRepository(wau).startService();

    theDaemon.getLockssRepository(wau);
    theDaemon.getHashService().startService();
    metrics.startService();
    metrics.setHashSpeed(100);
    NodeManager nmgr = theDaemon.getNodeManager(wau);
//    nmgr.initService(theDaemon);
    nmgr.startService();
 }


  public void tearDown() throws Exception {
   theDaemon.getLockssRepository(wau).stopService();
   theDaemon.getNodeManager(wau).stopService();
   //theDaemon.getNodeManager(sau).stopService();
   wau.getPlugin().stopPlugin();
   theDaemon.getPluginManager().stopService();
   super.tearDown();
  }

  public void testSimulatedContent() throws Exception {
    createContent();
    crawlContent();
    checkContent();
    /* Is this correct?  Check how node managers etc. handle
        wrapped au's */
    NodeManager nmgr = theDaemon.getNodeManager(sau);
    nmgr.startService();
    hashContent();
    doTestDualContentHash();
  }

  private void createContent() {
    SimulatedContentGenerator scgen = sau.getContentGenerator();
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_HTML
                       + SimulatedContentGenerator.FILE_TYPE_TXT);
    scgen.setAbnormalFile("1,1", 1);
    scgen.setOddBranchesHaveContent(true);

    sau.deleteContentTree();
    sau.generateContentTree();
    assertTrue(scgen.isContentTree());
  }

  private void crawlContent() {
    CrawlSpec spec = new CrawlSpec(
        SimulatedArchivalUnit.SIMULATED_URL_START, null);
    Crawler crawler = new NewContentCrawler(wau, spec, new MockAuState());
    crawler.doCrawl();
  }

  private void checkContent() throws IOException {
    checkRoot();
    checkLeaf();
    checkStoredContent();
  }

  private void checkRoot() {
    WrappedCachedUrlSet set = (WrappedCachedUrlSet)wau.getAuCachedUrlSet();
    Iterator setIt = set.flatSetIterator();
    ArrayList childL = new ArrayList(1);
    WrappedCachedUrlSet cus = null;
    while (setIt.hasNext()) {
      cus = (WrappedCachedUrlSet) setIt.next();
      childL.add(cus.getUrl());
    }

    String[] expectedA = new String[] {
      SimulatedArchivalUnit.SIMULATED_URL_ROOT};
    assertIsomorphic(expectedA, childL);

    setIt = cus.flatSetIterator();
    childL = new ArrayList(7);
    while (setIt.hasNext()) {
      childL.add( ( (CachedUrlSetNode) setIt.next()).getUrl());
    }

    expectedA = new String[] {
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/branch1",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/branch2",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/file1.html",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/file1.txt",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/file2.html",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/file2.txt",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/index.html"
    };
    assertIsomorphic(expectedA, childL);
  }

  private void checkLeaf() {
    String parent = SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/branch1";
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(parent);
    WrappedCachedUrlSet set = (WrappedCachedUrlSet)
        wplug.makeCachedUrlSet(wau,spec);
    Iterator setIt = set.contentHashIterator();
    ArrayList childL = new ArrayList(16);
    while (setIt.hasNext()) {
      childL.add( ( (CachedUrlSetNode) setIt.next()).getUrl());
    }
    String[] expectedA = new String[] {
      parent,
      parent + "/branch1",
      parent + "/branch1/file1.html",
      parent + "/branch1/file1.txt",
      parent + "/branch1/file2.html",
      parent + "/branch1/file2.txt",
      parent + "/branch1/index.html",
      parent + "/branch2",
      parent + "/branch2/file1.html",
      parent + "/branch2/file1.txt",
      parent + "/branch2/file2.html",
      parent + "/branch2/file2.txt",
      parent + "/branch2/index.html",
      parent + "/file1.html",
      parent + "/file1.txt",
      parent + "/file2.html",
      parent + "/file2.txt",
      parent + "/index.html",
    };
    assertIsomorphic(expectedA, childL);
  }

  private String getUrlContent(WrappedCachedUrl url) throws IOException {
    Reader content = url.openForReading();
    CharArrayWriter baos = new CharArrayWriter();
    StreamUtil.copy(content, baos);
    content.close();
    String contentStr = baos.toString();
    baos.close();
    return contentStr;
  }

  private void checkUrlContent(String path, int fileNum, int depth,
                               int branchNum, boolean isAbnormal,
                               boolean isDamaged) throws IOException {
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(
        SimulatedArchivalUnit.SIMULATED_URL_ROOT);
    WrappedCachedUrlSet urlset = (WrappedCachedUrlSet)
    wplug.makeCachedUrlSet(wau,spec);
    WrappedCachedUrl url = (WrappedCachedUrl)wplug.makeCachedUrl(urlset,
        SimulatedArchivalUnit.SIMULATED_URL_ROOT + path);
    String content = getUrlContent(url);
    String expectedContent;
    if (path.endsWith(".html")) {
      String fn = path.substring(path.lastIndexOf("/") + 1);
      expectedContent = SimulatedContentGenerator.getHtmlFileContent(fn,
        fileNum, depth, branchNum, isAbnormal);
    }
    else {
      expectedContent = SimulatedContentGenerator.getFileContent(
        fileNum, depth, branchNum, isAbnormal);
    }
    if (isDamaged) {
      assertNotEquals(expectedContent, content);
    }
    else {
      assertEquals(expectedContent, content);
    }
  }

  private void checkStoredContent() throws IOException {
    checkUrlContent("/file1.txt", 1, 0, 0, false, false);
    checkUrlContent("/branch1/branch1/file1.txt", 1, 2, 1, true, false);
  }

  public void doTestDualContentHash() throws Exception {
    createContent();
    crawlContent();
    WrappedCachedUrlSet set = (WrappedCachedUrlSet)wau.getAuCachedUrlSet();
    byte[] nameH = getHash(set, true);
    byte[] contentH = getHash(set, false);
  }

  private void hashContent() throws Exception {
    measureHashSpeed();
    hashSet(true);
    hashSet(false);
  }

  private void measureHashSpeed() throws Exception {
    MessageDigest dig = null;
    try {
      dig = MessageDigest.getInstance("SHA-1");
    }
    catch (NoSuchAlgorithmException ex) {
      fail("No algorithm.");
    }
    WrappedCachedUrlSet set = (WrappedCachedUrlSet)wau.getAuCachedUrlSet();
    CachedUrlSetHasher hasher = set.getContentHasher(dig);
    SystemMetrics metrics = theDaemon.getSystemMetrics();
    int estimate = metrics.getBytesPerMsHashEstimate(hasher, dig);
    assertTrue(estimate > 0);
    CachedUrlSet origset = (CachedUrlSet)set.getOriginal();
    long estimatedTime = origset.estimatedHashDuration();
    long size = ( (Long) PrivilegedAccessor.getValue(origset, "totalNodeSize")).
      longValue();
    assertTrue(size > 0);
    assertEquals(estimatedTime,
                 theDaemon.getHashService().padHashEstimate(size / estimate));
  }

  private void hashSet(boolean namesOnly) throws IOException {
    WrappedCachedUrlSet set = (WrappedCachedUrlSet)wau.getAuCachedUrlSet();
    byte[] hash = getHash(set, namesOnly);
    byte[] hash2 = getHash(set, namesOnly);
    assertTrue(Arrays.equals(hash, hash2));

    String parent = SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/branch1";
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(parent);
    set = (WrappedCachedUrlSet)wplug.makeCachedUrlSet(wau,spec);
    hash2 = getHash(set, namesOnly);
    assertFalse(Arrays.equals(hash, hash2));
  }

  private byte[] getHash(CachedUrlSet set, boolean namesOnly) throws
    IOException {
    MessageDigest dig = null;
    try {
      dig = MessageDigest.getInstance("SHA-1");
    }
    catch (NoSuchAlgorithmException ex) {
      fail("No algorithm.");
    }
    hash(set, dig, namesOnly);
    return dig.digest();
  }

  private void hash(CachedUrlSet set, MessageDigest dig, boolean namesOnly)
      throws IOException {
    CachedUrlSetHasher hasher = null;
    if (namesOnly) {
      hasher = set.getNameHasher(dig);
    }
    else {
      hasher = set.getContentHasher(dig);
    }
    int bytesHashed = 0;
    long timeTaken = System.currentTimeMillis();
    while (!hasher.finished()) {
      bytesHashed += hasher.hashStep(256);
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {
      FuncWrappedSimulatedContent.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  public static Test suite() {
    return new TestSuite(FuncWrappedSimulatedContent.class);
  }

}
