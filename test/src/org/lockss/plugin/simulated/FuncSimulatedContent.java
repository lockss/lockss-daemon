/*
 * $Id: FuncSimulatedContent.java,v 1.30 2003-04-02 00:19:40 aalto Exp $
 */

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

package org.lockss.plugin.simulated;

import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.crawler.GoslingCrawlerImpl;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.plugin.PluginManager;
import java.security.*;
import org.lockss.plugin.*;

/**
 * Test class for functional tests on the content.
 */
public class FuncSimulatedContent extends LockssTestCase {
  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon = new MockLockssDaemon();

  public FuncSimulatedContent(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    sau = new SimulatedArchivalUnit();
    sau.setRootDir(tempDirPath);
    String s = SystemMetrics.PARAM_HASH_TEST_DURATION + "=1000\n";
    String s2 = SystemMetrics.PARAM_HASH_TEST_BYTE_STEP + "=1024\n";
    String s3 = LockssRepositoryServiceImpl.PARAM_CACHE_LOCATION + "=" +
        tempDirPath;
    String configStr = s + s2 + s3;
    TestConfiguration.setCurrentConfigFromString(configStr);
    theDaemon.getLockssRepositoryService().startService();
    theDaemon.getLockssRepository(sau);
    theDaemon.getPluginManager();
  }

  public void tearDown() throws Exception {
    theDaemon.getLockssRepositoryService().stopService();
    super.tearDown();
  }

  public void testSimulatedContent() throws Exception {
    createContent();
    crawlContent();
    checkContent();
    hashContent();
  }

  public void testDualContentHash() throws Exception {
    createContent();
    crawlContent();
    CachedUrlSet set = sau.getAUCachedUrlSet();
    byte[] nameH = getHash(set, true);
    byte[] contentH = getHash(set, false);

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    sau = new SimulatedArchivalUnit();
    sau.setRootDir(tempDirPath);
    TestLockssRepositoryServiceImpl.configCacheLocation(tempDirPath);
    theDaemon.getLockssRepository(sau);

    createContent();
    crawlContent();
    set = sau.getAUCachedUrlSet();
    byte[] nameH2 = getHash(set, true);
    byte[] contentH2 = getHash(set, false);
    assertTrue(Arrays.equals(nameH, nameH2));
    assertTrue(Arrays.equals(contentH, contentH2));
  }


  private void createContent() {
    SimulatedContentGenerator scgen = sau.getContentGenerator();
    scgen.setTreeDepth(2);
    scgen.setNumBranches(2);
    scgen.setNumFilesPerBranch(2);
    scgen.setFileTypes(scgen.FILE_TYPE_HTML+scgen.FILE_TYPE_TXT);
    scgen.setAbnormalFile("1,1", 1);
    scgen.setOddBranchesHaveContent(true);

    sau.generateContentTree();
    assertTrue(scgen.isContentTree());
  }

  private void crawlContent() {
    CrawlSpec spec = new CrawlSpec(sau.SIMULATED_URL_START, null);
    Crawler crawler =
      new GoslingCrawlerImpl(sau, spec.getStartingUrls(), true);
    crawler.doCrawl(Deadline.NEVER);
  }

  private void checkContent() throws IOException {
    checkRoot();
    checkLeaf();
    checkStoredContent();
  }

  private void hashContent() throws Exception {
    measureHashSpeed();
    hashSet(true);
    hashSet(false);
  }

  private void checkRoot() {
    CachedUrlSet set = sau.getAUCachedUrlSet();
    Iterator setIt = set.flatSetIterator();
    ArrayList childL = new ArrayList(1);
    CachedUrlSet cus = null;
    while (setIt.hasNext()) {
      cus = (CachedUrlSet)setIt.next();
      childL.add(cus.getUrl());
    }

    String[] expectedA = new String[] { sau.SIMULATED_URL_ROOT };
    assertIsomorphic(expectedA, childL);

    setIt = cus.flatSetIterator();
    childL = new ArrayList(7);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }

    expectedA = new String[] {
      sau.SIMULATED_URL_ROOT+"/branch1",
      sau.SIMULATED_URL_ROOT+"/branch2",
      sau.SIMULATED_URL_ROOT+"/file1.html",
      sau.SIMULATED_URL_ROOT+"/file1.txt",
      sau.SIMULATED_URL_ROOT+"/file2.html",
      sau.SIMULATED_URL_ROOT+"/file2.txt",
      sau.SIMULATED_URL_ROOT+"/index.html"
      };
    assertIsomorphic(expectedA, childL);
  }

  private void checkLeaf() {
    String parent = sau.SIMULATED_URL_ROOT + "/branch1";
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(parent);
    CachedUrlSet set = sau.cachedUrlSetFactory(sau, spec);
    Iterator setIt = set.treeIterator();
    ArrayList childL = new ArrayList(15);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }
    String[] expectedA = new String[] {
      parent+"/branch1",
      parent+"/branch1/file1.html",
      parent+"/branch1/file1.txt",
      parent+"/branch1/file2.html",
      parent+"/branch1/file2.txt",
      parent+"/branch1/index.html",
      parent+"/branch2",
      parent+"/branch2/file1.html",
      parent+"/branch2/file1.txt",
      parent+"/branch2/file2.html",
      parent+"/branch2/file2.txt",
      parent+"/branch2/index.html",
      parent+"/file1.html",
      parent+"/file1.txt",
      parent+"/file2.html",
      parent+"/file2.txt",
      parent+"/index.html",
      };
    assertIsomorphic(expectedA, childL);
  }

  private void checkStoredContent() throws IOException {
    String file = sau.SIMULATED_URL_ROOT + "/file1.txt";
    CachedUrl url = sau.cachedUrlFactory(sau.getAUCachedUrlSet(), file);
    String content = getUrlContent(url);
    String expectedContent = sau.getContentGenerator().getFileContent(1, 0, 0, false);
    assertEquals(expectedContent, content);

    file = sau.SIMULATED_URL_ROOT + "/branch1/branch1/file1.txt";
    url = sau.cachedUrlFactory(sau.getAUCachedUrlSet(), file);
    content = getUrlContent(url);
    expectedContent = sau.getContentGenerator().getFileContent(1, 2, 1, true);
    assertEquals(expectedContent, content);
  }

  private void measureHashSpeed() throws Exception {
    MessageDigest dig = null;
    try {
      dig = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException ex) {
      fail("No algorithm.");
    }
    CachedUrlSet set = sau.getAUCachedUrlSet();
    CachedUrlSetHasher hasher = set.getContentHasher(dig);
    SystemMetrics metrics = SystemMetrics.getSystemMetrics();
    int estimate = metrics.getBytesPerMsHashEstimate(hasher, dig);
    assertTrue(estimate > 0);
    long size = ((Long)PrivilegedAccessor.getValue(set, "totalNodeSize")).longValue();
    assertTrue(size > 0);
    long estimatedTime = set.estimatedHashDuration();
    System.out.println("b/ms: "+estimate);
    System.out.println("size: "+size);
    System.out.println("estimate: "+estimatedTime);
    assertEquals(size / estimate, estimatedTime);
  }

  private void hashSet(boolean namesOnly) throws IOException {
    CachedUrlSet set = sau.getAUCachedUrlSet();
    byte[] hash = getHash(set, namesOnly);
    byte[] hash2 = getHash(set, namesOnly);
    assertTrue(Arrays.equals(hash, hash2));

    String parent = sau.SIMULATED_URL_ROOT + "/branch1";
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(parent);
    set = sau.cachedUrlSetFactory(sau, spec);
    hash2 = getHash(set, namesOnly);
    assertFalse(Arrays.equals(hash, hash2));
  }

  private byte[] getHash(CachedUrlSet set, boolean namesOnly) throws IOException {
    MessageDigest dig = null;
    try {
      dig = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException ex) { fail("No algorithm."); }
    hash(set, dig, namesOnly);
    return dig.digest();
  }

  private void hash(CachedUrlSet set, MessageDigest dig, boolean namesOnly) throws IOException {
    CachedUrlSetHasher hasher = null;
    if (namesOnly) {
      hasher = set.getNameHasher(dig);
    } else {
      hasher = set.getContentHasher(dig);
    }
    int bytesHashed = 0;
    long timeTaken = System.currentTimeMillis();
    while (!hasher.finished()) {
      bytesHashed += hasher.hashStep(256);
    }
    timeTaken = System.currentTimeMillis() - timeTaken;
    if ((timeTaken>0)&&(bytesHashed>500)) {
      System.out.println("Bytes hashed: "+bytesHashed);
      System.out.println("Time taken: "+timeTaken+"ms");
      System.out.println("Bytes/sec: "+(bytesHashed*1000/timeTaken));
    }
  }

  private String getUrlContent(CachedUrl url) throws IOException {
    InputStream content = url.openForReading();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamUtil.copy(content, baos);
    content.close();
    String contentStr = new String(baos.toByteArray());
    baos.close();
    return contentStr;
  }

  public static void main(String[] argv) {
    String[] testCaseList = {FuncSimulatedContent.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }


}
