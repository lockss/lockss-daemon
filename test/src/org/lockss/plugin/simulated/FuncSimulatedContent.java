/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.crawler.FollowLinkCrawler;
import org.lockss.state.HistoryRepositoryImpl;
import junit.framework.*;

/**
 * Functional tests on the simulated content generator.
 */
public class FuncSimulatedContent extends LockssTestCase {
  static final Logger log = Logger.getLogger("FuncSimulatedContent");

  private PluginManager pluginMgr;
  private Plugin simPlugin;
  private SimulatedArchivalUnit sau1;
  private SimulatedContentGenerator scgen = null;
  private MockLockssDaemon theDaemon;
  String tempDirPath;
  String tempDirPath2;

  private static String DAMAGED_CACHED_URL = "/branch2/branch2/002file.txt";

  public FuncSimulatedContent(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.getHashService();
    MockSystemMetrics metrics = new MyMockSystemMetrics();
    metrics.initService(theDaemon);
    theDaemon.setSystemMetrics(metrics);

    theDaemon.setDaemonInited(true);

    Properties props = new Properties();
    props.setProperty(SystemMetrics.PARAM_HASH_TEST_DURATION, "1000");
    props.setProperty(SystemMetrics.PARAM_HASH_TEST_BYTE_STEP, "1024");
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
		      tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    pluginMgr = theDaemon.getPluginManager();
    pluginMgr.startService();
    theDaemon.getHashService().startService();
    metrics.startService();
    metrics.setHashSpeed(100);

    simPlugin = PluginTestUtil.findPlugin(SimulatedPlugin.class);
  }

  public void tearDown() throws Exception {
    theDaemon.getLockssRepository(sau1).stopService();
    theDaemon.getNodeManager(sau1).stopService();
    theDaemon.getPluginManager().stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getSystemMetrics().stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  SimulatedArchivalUnit setupSimAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    ArchivalUnit au = PluginTestUtil.createAndStartAu(simPlugin, auConfig);
    return (SimulatedArchivalUnit)au;
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "2");
    conf.put("badCachedFileLoc", "2,2");
    conf.put("badCachedFileNum", "2");
    return conf;
  }
    
  void enableFilter(SimulatedArchivalUnit sau, boolean enable)
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
    sau1 = setupSimAu(simAuConfig(tempDirPath));
    createContent(sau1);
    crawlContent(sau1);
    checkContent(sau1);
    checkFilter(sau1);
    hashContent(sau1);

  }

  public void testDualContentHash() throws Exception {
    sau1 = setupSimAu(simAuConfig(tempDirPath));
    createContent(sau1);
    crawlContent(sau1);
    CachedUrlSet set = sau1.getAuCachedUrlSet();
    byte[] nameH = getHash(set, true);
    byte[] contentH = getHash(set, false);

    tempDirPath2 = getTempDir().getAbsolutePath() + File.separator;
    SimulatedArchivalUnit sau2 = setupSimAu(simAuConfig(tempDirPath2));

    createContent(sau2);
    crawlContent(sau2);
    set = sau2.getAuCachedUrlSet();
    byte[] nameH2 = getHash(set, true);
    byte[] contentH2 = getHash(set, false);
    assertEquals(nameH, nameH2);
    assertEquals(contentH, contentH2);
  }

  public void testBaseUrl() throws Exception {
    sau1 = setupSimAu(simAuConfig(tempDirPath));
    createContent(sau1);
    crawlContent(sau1);
    CachedUrlSet cus1 = sau1.getAuCachedUrlSet();

    tempDirPath2 = getTempDir().getAbsolutePath() + File.separator;
    Configuration config2 = simAuConfig(tempDirPath2);
    config2.put("base_url", "http://anotherhost.org/");
    SimulatedArchivalUnit sau2 = setupSimAu(config2);
    createContent(sau2);
    crawlContent(sau2);
    CachedUrlSet cus2 = sau1.getAuCachedUrlSet();
    List urls1 = auUrls(sau1);
    List urls2 = auUrls(sau2);

    Pattern pat = Pattern.compile("http://([^/]+)(/.*)$");
    List<String> l1 = auUrls(sau1);
    List<String> l2 = auUrls(sau2);
    assertEquals(l1.size(), l2.size());
    for (int ix = 0; ix < l1.size(); ix++) {
      Matcher m1 = pat.matcher(l1.get(ix));
      assertTrue(m1.matches());
      Matcher m2 = pat.matcher(l2.get(ix));
      assertTrue(m2.matches());
      assertEquals("www.example.com", m1.group(1));
      assertEquals("anotherhost.org", m2.group(1));
      assertEquals(m1.group(2), m2.group(2));
    }
  }

  public void testBaseUrlPath() throws Exception {
    sau1 = setupSimAu(simAuConfig(tempDirPath));
    createContent(sau1);
    crawlContent(sau1);
    CachedUrlSet cus1 = sau1.getAuCachedUrlSet();

    tempDirPath2 = getTempDir().getAbsolutePath() + File.separator;
    Configuration config2 = simAuConfig(tempDirPath2);
    config2.put("base_url", "http://anotherhost.org/some/path/");
    SimulatedArchivalUnit sau2 = setupSimAu(config2);
    createContent(sau2);
    crawlContent(sau2);
    CachedUrlSet cus2 = sau1.getAuCachedUrlSet();
    List urls1 = auUrls(sau1);
    List urls2 = auUrls(sau2);

    Pattern pat1 = Pattern.compile("http://www\\.example\\.com(/.*)$");
    Pattern pat2 = Pattern.compile("http://anotherhost\\.org/some/path(/.*)$");
    List<String> l1 = auUrls(sau1);
    List<String> l2 = auUrls(sau2);
    assertEquals(l1.size(), l2.size());
    for (int ix = 0; ix < l1.size(); ix++) {
      Matcher m1 = pat1.matcher(l1.get(ix));
      assertTrue(m1.matches());
      Matcher m2 = pat2.matcher(l2.get(ix));
      assertTrue(m2.matches());
      assertEquals(m1.group(1), m2.group(1));
    }
  }

  List<String> auUrls(ArchivalUnit au) {
    return PluginTestUtil.urlsOf(au.getAuCachedUrlSet().getCuIterable());
  }

  protected void createContent(SimulatedArchivalUnit sau) {
    log.debug("createContent()");
    scgen = sau.getContentGenerator();
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_HTML +
                       SimulatedContentGenerator.FILE_TYPE_TXT);
    scgen.setAbnormalFile("1,1", 1);
    scgen.setOddBranchesHaveContent(true);

    sau.deleteContentTree();
    sau.generateContentTree();
    assertTrue(scgen.isContentTree());
  }

  protected void crawlContent(SimulatedArchivalUnit sau) {
    log.debug("crawlContent()");
    Crawler crawler =
      new NoCrawlEndActionsFollowLinkCrawler(sau, new MockAuState());
    crawler.doCrawl();
  }

  protected void checkContent(SimulatedArchivalUnit sau) throws IOException {
    log.debug("checkContent()");
    checkRoot(sau);
    checkLeaf(sau);
    checkStoredContent(sau);
    checkDepth(sau);
  }

  protected void checkFilter(SimulatedArchivalUnit sau) throws Exception {
    log.debug("checkFilter()");
    CachedUrl cu = sau.makeCachedUrl(sau.getUrlRoot() + "/001file.html");

    enableFilter(sau, true);
    InputStream is = cu.openForHashing();
    String expected =
      "001file.html This is file 1, depth 0, branch 0. foobar ";
    assertEquals(expected, StringUtil.fromInputStream(is));
    is.close();
    enableFilter(sau, false);
    cu = sau.makeCachedUrl(sau.getUrlRoot() + "/001file.html");
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

  protected void hashContent(SimulatedArchivalUnit sau) throws Exception {
    log.debug("hashContent()");
    measureHashSpeed(sau);

    // If any changes are made to the contents or shape of the simulated
    // content tree, these hash values will have to be changed
    checkHashSet(sau, true, false,
		 fromHex("6AB258B4E1FFD9F9B45316B4F54111FF5E5948D2"));
    checkHashSet(sau, true, true,
		 fromHex("6AB258B4E1FFD9F9B45316B4F54111FF5E5948D2"));
    checkHashSet(sau, false, false,
		 fromHex("409893F1A603F4C276632694DB1621B639BD5164"));
    checkHashSet(sau, false, true,
		 fromHex("85E6213C3771BEAC5A4602CAF7982C6C222800D5"));
  }

  protected void checkDepth(SimulatedArchivalUnit sau) {
    log.debug("checkDepth()");
    String URL_ROOT = sau.getUrlRoot();
    assertEquals(0, sau.getLinkDepth(URL_ROOT + "/index.html"));
    assertEquals(0, sau.getLinkDepth(URL_ROOT + "/"));
    assertEquals(1, sau.getLinkDepth(URL_ROOT + "/001file.html"));
    assertEquals(1, sau.getLinkDepth(URL_ROOT + "/branch1/index.html"));
    assertEquals(1, sau.getLinkDepth(URL_ROOT + "/branch1/"));
    assertEquals(2, sau.getLinkDepth(URL_ROOT + "/branch1/001file.html"));
  }

  protected void checkRoot(SimulatedArchivalUnit sau) {
    log.debug("checkRoot()");
    CachedUrlSet set = sau.getAuCachedUrlSet();
    Iterator setIt = set.flatSetIterator();
    ArrayList childL = new ArrayList(1);
    CachedUrlSet cus = null;
    while (setIt.hasNext()) {
      cus = (CachedUrlSet) setIt.next();
      childL.add(cus.getUrl());
    }

    String urlRoot = sau.getUrlRoot();

    String[] expectedA = new String[1];
    expectedA[0] = urlRoot;
    assertIsomorphic(expectedA, childL);

    setIt = cus.flatSetIterator();
    childL = new ArrayList(7);
    while (setIt.hasNext()) {
      childL.add( ( (CachedUrlSetNode) setIt.next()).getUrl());
    }

    expectedA = new String[] {
      urlRoot + "/001file.html",
      urlRoot + "/001file.txt",
      urlRoot + "/002file.html",
      urlRoot + "/002file.txt",
      urlRoot + "/branch1",
      urlRoot + "/branch2",
      urlRoot + "/index.html"
    };
    assertIsomorphic(expectedA, childL);
  }

  protected void checkLeaf(SimulatedArchivalUnit sau) {
    log.debug("checkLeaf()");
    String parent = sau.getUrlRoot() + "/branch1";
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

  protected void checkUrlContent(SimulatedArchivalUnit sau,
				 String path, int fileNum, int depth,
				 int branchNum, boolean isAbnormal,
				 boolean isDamaged) throws IOException {
    String file = sau.getUrlRoot() + path;
    CachedUrl url = sau.makeCachedUrl(file);
    String content = getUrlContent(url);
    String expectedContent;
    if (path.endsWith(".html")) {
      String fn = path.substring(path.lastIndexOf("/") + 1);
      expectedContent = scgen.getHtmlFileContent(fn, fileNum, depth,
                                                 branchNum, isAbnormal);
    }
    else {
      expectedContent = scgen.getTxtContent(fileNum, depth, branchNum,
                                            isAbnormal);
    }
    if (isDamaged) {
      assertNotEquals(expectedContent, content);
    }
    else {
      assertEquals(expectedContent, content);
    }
  }

  protected void checkStoredContent(SimulatedArchivalUnit sau)
      throws IOException {
    checkUrlContent(sau, "/001file.txt", 1, 0, 0, false, false);
    checkUrlContent(sau, "/branch1/branch1/001file.txt", 1, 2, 1, true, false);
    checkUrlContent(sau, DAMAGED_CACHED_URL, 2, 2, 2, false, false);
  }

  private void measureHashSpeed(SimulatedArchivalUnit sau) throws Exception {
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

  private void checkHashSet(SimulatedArchivalUnit sau,
			    boolean namesOnly, boolean filter,
			    byte[] expected) throws Exception {
    enableFilter(sau, filter);
    CachedUrlSet set = sau.getAuCachedUrlSet();
    byte[] hash = getHash(set, namesOnly);
    assertEquals(expected,hash);

    String parent = sau.getUrlRoot() + "/branch1";
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
