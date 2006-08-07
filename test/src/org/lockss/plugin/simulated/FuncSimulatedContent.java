/*
 * $Id: FuncSimulatedContent.java,v 1.74 2006-08-07 07:43:40 tlipkis Exp $
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

package org.lockss.plugin.simulated;

import java.util.*;
import java.io.*;
import java.security.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.crawler.NewContentCrawler;
import org.lockss.state.HistoryRepositoryImpl;
import junit.framework.*;

/**
 * Test class for functional tests on the content.
 */
public class FuncSimulatedContent extends LockssTestCase {
  static final Logger log = Logger.getLogger("FuncSimulatedContent");

  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;
  private String auId;
  private String auId2;

  private static String DAMAGED_CACHED_URL = "/branch2/branch2/002file.txt";

  public FuncSimulatedContent(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String tempDirPath2 = getTempDir().getAbsolutePath() + File.separator;
    String auIdStr = "org|lockss|plugin|simulated|SimulatedPlugin.root~" +
      PropKeyEncoder.encode(tempDirPath);
    String auId2Str = "org|lockss|plugin|simulated|SimulatedPlugin.root~" +
      PropKeyEncoder.encode(tempDirPath2);
    Properties props = new Properties();
    props.setProperty(SystemMetrics.PARAM_HASH_TEST_DURATION, "1000");
    props.setProperty(SystemMetrics.PARAM_HASH_TEST_BYTE_STEP, "1024");
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION,
                      tempDirPath);
    props.setProperty("org.lockss.au." + auIdStr + ".root", tempDirPath);
    props.setProperty("org.lockss.au." + auIdStr + ".depth", "2");
    props.setProperty("org.lockss.au." + auIdStr + ".branch", "2");
    props.setProperty("org.lockss.au." + auIdStr + ".numFiles", "2");

    props.setProperty("org.lockss.au." + auIdStr + ".badCachedFileLoc", "2,2");
    props.setProperty("org.lockss.au." + auIdStr + ".badCachedFileNum", "2");
    props.setProperty("org.lockss.au." + auId2Str + ".badCachedFileLoc", "2,2");
    props.setProperty("org.lockss.au." + auId2Str + ".badCachedFileNum", "2");

    props.setProperty("org.lockss.au." + auId2Str + ".root", tempDirPath2);
    props.setProperty("org.lockss.au." + auId2Str + ".depth", "2");
    props.setProperty("org.lockss.au." + auId2Str + ".branch", "2");
    props.setProperty("org.lockss.au." + auId2Str + ".numFiles", "2");

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.getHashService();
    MockSystemMetrics metrics = new MyMockSystemMetrics();
    metrics.initService(theDaemon);
    theDaemon.setSystemMetrics(metrics);

    theDaemon.setDaemonInited(true);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    // form proper ids
    auId = auIdStr.replace('.', '&');
    auId2 = auId2Str.replace('.', '&');

    sau =
      (SimulatedArchivalUnit)theDaemon.getPluginManager().getAuFromId(auId);

    theDaemon.getPluginManager().startService();

    theDaemon.getHashService().startService();
    metrics.startService();
    metrics.setHashSpeed(100);

    theDaemon.getHistoryRepository(sau).startService();
    theDaemon.getLockssRepository(sau);
    theDaemon.getNodeManager(sau).startService();
  }

  public void tearDown() throws Exception {
    theDaemon.getLockssRepository(sau).stopService();
    theDaemon.getNodeManager(sau).stopService();
    theDaemon.getPluginManager().stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getSystemMetrics().stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  void enableFilter(boolean enable)
      throws ArchivalUnit.ConfigurationException {
    Configuration auConfig = sau.getConfiguration().copy();
    // no bad file when playing with filtering
    auConfig.remove("badCachedFileLoc");
    auConfig.remove("badCachedFileNum");
    if (enable) {
      auConfig.put(SimulatedPlugin.AU_PARAM_HASH_FILTER_SPEC, "true");
    } else {
      auConfig.remove(SimulatedPlugin.AU_PARAM_HASH_FILTER_SPEC);
    }
    sau.setConfiguration(auConfig);
  }

  public void testSimulatedContent() throws Exception {
    createContent();
    crawlContent();
    checkContent();
    doDamageRemoveTest();	       // must be before content read again
    checkFilter();
    hashContent();

    // this resets AU's config, do last to avoid messing up toBeDamaged set
  }

  public void testDualContentHash() throws Exception {
    createContent();
    crawlContent();
    CachedUrlSet set = sau.getAuCachedUrlSet();
    byte[] nameH = getHash(set, true);
    byte[] contentH = getHash(set, false);

    sau =
        (SimulatedArchivalUnit)theDaemon.getPluginManager().getAuFromId(auId2);
    theDaemon.getLockssRepository(sau);
    theDaemon.getNodeManager(sau).startService();

    createContent();
    crawlContent();
    set = sau.getAuCachedUrlSet();
    byte[] nameH2 = getHash(set, true);
    byte[] contentH2 = getHash(set, false);
    assertTrue(Arrays.equals(nameH, nameH2));
    assertTrue(Arrays.equals(contentH, contentH2));
  }

  private void createContent() {
    log.debug("createContent()");
    SimulatedContentGenerator scgen = sau.getContentGenerator();
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_HTML +
                       SimulatedContentGenerator.FILE_TYPE_TXT);
    scgen.setAbnormalFile("1,1", 1);
    scgen.setOddBranchesHaveContent(true);

    sau.deleteContentTree();
    sau.generateContentTree();
    assertTrue(scgen.isContentTree());
  }

  private void crawlContent() {
    log.debug("crawlContent()");
    CrawlSpec spec =
      new SpiderCrawlSpec(SimulatedArchivalUnit.SIMULATED_URL_START, null);
    Crawler crawler = new NewContentCrawler(sau, spec, new MockAuState());
    crawler.doCrawl();
  }

  private void checkContent() throws IOException {
    log.debug("checkContent()");
    checkRoot();
    checkLeaf();
    checkStoredContent();
    checkDepth();
  }

  private void checkFilter() throws Exception {
    log.debug("checkFilter()");
    CachedUrl cu = sau.makeCachedUrl(SimulatedArchivalUnit.SIMULATED_URL_ROOT
				     + "/001file.html");

    enableFilter(true);
    InputStream is = cu.openForHashing();
    String expected =
      "001file.html This is file 1, depth 0, branch 0. foobar ";
    assertEquals(expected, StringUtil.fromInputStream(is));
    is.close();
    enableFilter(false);
    cu = sau.makeCachedUrl(SimulatedArchivalUnit.SIMULATED_URL_ROOT
			   + "/001file.html");
    is = cu.openForHashing();
    expected =
      "<HTML><HEAD><TITLE>001file.html</TITLE></HEAD><BODY>\n" +
      "This is file 1, depth 0, branch 0.<br><!-- comment -->    " +
      "Citation String   foobar<br><script>" +
      "(defun fact (n) (cond ((= n 0) 1) (t (fact (sub1 n)))))</script>\n" +
      "</BODY></HTML>";
    assertEquals(expected, StringUtil.fromInputStream(is));
    is.close();
  }

  private byte[] fromHex(String hex) {
    return ByteArray.fromHexString(hex);
  }

  private void hashContent() throws Exception {
    log.debug("hashContent()");
    measureHashSpeed();

    // If any changes are made to the contents or shape of the simulated
    // content tree, these hash values will have to be changed
    checkHashSet(true, false,
		 fromHex("6AB258B4E1FFD9F9B45316B4F54111FF5E5948D2"));
    checkHashSet(true, true,
		 fromHex("6AB258B4E1FFD9F9B45316B4F54111FF5E5948D2"));
    checkHashSet(false, false,
		 fromHex("409893F1A603F4C276632694DB1621B639BD5164"));
    checkHashSet(false, true,
		 fromHex("85E6213C3771BEAC5A4602CAF7982C6C222800D5"));
  }

  private void checkDepth() {
    log.debug("checkDepth()");
    String URL_ROOT = SimulatedArchivalUnit.SIMULATED_URL_ROOT;
    assertEquals(0, sau.getLinkDepth(URL_ROOT + "/index.html"));
    assertEquals(0, sau.getLinkDepth(URL_ROOT + "/"));
    assertEquals(1, sau.getLinkDepth(URL_ROOT + "/001file.html"));
    assertEquals(1, sau.getLinkDepth(URL_ROOT + "/branch1/index.html"));
    assertEquals(1, sau.getLinkDepth(URL_ROOT + "/branch1/"));
    assertEquals(2, sau.getLinkDepth(URL_ROOT + "/branch1/001file.html"));
  }

  private void checkRoot() {
    log.debug("checkRoot()");
    CachedUrlSet set = sau.getAuCachedUrlSet();
    Iterator setIt = set.flatSetIterator();
    ArrayList childL = new ArrayList(1);
    CachedUrlSet cus = null;
    while (setIt.hasNext()) {
      cus = (CachedUrlSet) setIt.next();
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
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/001file.html",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/001file.txt",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/002file.html",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/002file.txt",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/branch1",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/branch2",
      SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/index.html"
    };
    assertIsomorphic(expectedA, childL);
  }

  private void checkLeaf() {
    log.debug("checkLeaf()");
    String parent = SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/branch1";
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(parent);
    CachedUrlSet set = sau.makeCachedUrlSet(spec);
    Iterator setIt = set.contentHashIterator();
    ArrayList childL = new ArrayList(16);
    while (setIt.hasNext()) {
      childL.add( ( (CachedUrlSetNode) setIt.next()).getUrl());
    }
    String[] expectedA = new String[] {
      parent,
      parent + "/001file.html",
      parent + "/001file.txt",
      parent + "/002file.html",
      parent + "/002file.txt",
      parent + "/branch1",
      parent + "/branch1/001file.html",
      parent + "/branch1/001file.txt",
      parent + "/branch1/002file.html",
      parent + "/branch1/002file.txt",
      parent + "/branch1/index.html",
      parent + "/branch2",
      parent + "/branch2/001file.html",
      parent + "/branch2/001file.txt",
      parent + "/branch2/002file.html",
      parent + "/branch2/002file.txt",
      parent + "/branch2/index.html",
      parent + "/index.html",
    };
    assertIsomorphic(expectedA, childL);
  }

  private void checkUrlContent(String path, int fileNum, int depth,
                               int branchNum, boolean isAbnormal,
                               boolean isDamaged) throws IOException {
    String file = SimulatedArchivalUnit.SIMULATED_URL_ROOT + path;
    CachedUrl url = sau.makeCachedUrl(file);
    String content = getUrlContent(url);
    String expectedContent;
    if (path.endsWith(".html")) {
      String fn = path.substring(path.lastIndexOf("/") + 1);
      expectedContent = SimulatedContentGenerator.getHtmlFileContent(fn,
        fileNum, depth, branchNum, isAbnormal);
    }
    else {
      expectedContent = SimulatedContentGenerator.getTxtContent(
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
    checkUrlContent("/001file.txt", 1, 0, 0, false, false);
    checkUrlContent("/branch1/branch1/001file.txt", 1, 2, 1, true, false);
    checkUrlContent(DAMAGED_CACHED_URL, 2, 2, 2, false, true);
  }

  private void doDamageRemoveTest() throws Exception {
    /* Cache the file again; this time the damage should be gone */
    String file = SimulatedArchivalUnit.SIMULATED_URL_ROOT + DAMAGED_CACHED_URL;
    UrlCacher uc = sau.makeUrlCacher(file);
    BitSet fetchFlags = new BitSet();
    fetchFlags.set(UrlCacher.REFETCH_FLAG);
    uc.setFetchFlags(fetchFlags);
    uc.cache();
    checkUrlContent(DAMAGED_CACHED_URL, 2, 2, 2, false, false);
  }

  private void measureHashSpeed() throws Exception {
    MessageDigest dig = null;
    try {
      dig = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException ex) {
      fail("No algorithm.");
    }
    CachedUrlSet set = sau.getAuCachedUrlSet();
    CachedUrlSetHasher hasher = set.getContentHasher(dig);
    SystemMetrics metrics = theDaemon.getSystemMetrics();
    int estimate = metrics.getBytesPerMsHashEstimate(hasher, dig);
    // should be protected against this being zero by MyMockSystemMetrics,
    // but otherwise use the proper calculation.  This avoids test failure
    // due to really slow machines
    assertTrue(estimate > 0);
    long estimatedTime = set.estimatedHashDuration();
    long size = ((Long)PrivilegedAccessor.getValue(set,
        "totalNodeSize")).longValue();
    assertTrue(size > 0);
    System.out.println("b/ms: " + estimate);
    System.out.println("size: " + size);
    System.out.println("estimate: " + estimatedTime);
    assertEquals(estimatedTime,
                 theDaemon.getHashService().padHashEstimate(size / estimate));
  }

  private void checkHashSet(boolean namesOnly, boolean filter,
			    byte[] expected) throws Exception {
    enableFilter(filter);
    CachedUrlSet set = sau.getAuCachedUrlSet();
    byte[] hash = getHash(set, namesOnly);
    assertEquals(expected,hash);

    String parent = SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/branch1";
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(parent);
    set = sau.makeCachedUrlSet(spec);
    byte[] hash2 = getHash(set, namesOnly);
    assertFalse(Arrays.equals(hash, hash2));
  }

  private byte[] getHash(CachedUrlSet set, boolean namesOnly) throws
    IOException {
    MessageDigest dig = null;
    try {
      dig = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException ex) {
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
    } else {
      hasher = set.getContentHasher(dig);
    }
    int bytesHashed = 0;
    long timeTaken = System.currentTimeMillis();
    while (!hasher.finished()) {
      bytesHashed += hasher.hashStep(256);
    }
    timeTaken = System.currentTimeMillis() - timeTaken;
    if ((timeTaken > 0) && (bytesHashed > 500)) {
      System.out.println("Bytes hashed: " + bytesHashed);
      System.out.println("Time taken: " + timeTaken + "ms");
      System.out.println("Bytes/sec: " + (bytesHashed * 1000 / timeTaken));
    } else {
      System.out.println("No time taken, or insufficient bytes hashed.");
      System.out.println("Bytes hashed: " + bytesHashed);
      System.out.println("Time taken: " + timeTaken + "ms");
    }
  }

  private String getUrlContent(CachedUrl url) throws IOException {
    InputStream content = url.getUnfilteredInputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamUtil.copy(content, baos);
    content.close();
    String contentStr = new String(baos.toByteArray());
    baos.close();
    return contentStr;
  }

  // this version doesn't fully override the 'measureHashSpeed()' function, but
  // protects against it returning '0' by returning the set speed
  private class MyMockSystemMetrics extends MockSystemMetrics {
    public int measureHashSpeed(CachedUrlSetHasher hasher, MessageDigest digest)
        throws IOException {
      int speed = super.measureHashSpeed(hasher, digest);
      if (speed==0) {
        speed = getHashSpeed();
        if (speed<=0) {
          throw new RuntimeException("No hash speed set.");
        }
      }
      return speed;
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {
      FuncSimulatedContent.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  public static Test suite() {
    return new TestSuite(FuncSimulatedContent.class);
  }

}
