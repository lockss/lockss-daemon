/*
 * $Id: TestLockssRepositoryServiceImpl.java,v 1.2 2003-03-05 22:55:28 aalto Exp $
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

package org.lockss.repository;

import java.io.File;
import java.util.Vector;
import org.lockss.test.*;
import org.lockss.util.Logger;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import java.io.IOException;
import org.lockss.daemon.TestConfiguration;
import java.net.*;
import java.util.*;

public class TestLockssRepositoryServiceImpl extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestLRSI");
  private MockLockssDaemon theDaemon = new MockLockssDaemon(null);
  private LockssRepositoryServiceImpl lrsi;
  private MockArchivalUnit mau;

  public TestLockssRepositoryServiceImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    configCacheLocation(tempDirPath);

    mau = new MockArchivalUnit();

    lrsi = new LockssRepositoryServiceImpl();
    lrsi.initService(theDaemon);
    lrsi.startService();
  }

  public void tearDown() throws Exception {
    lrsi.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testAuKey() {
    String expectedStr = mau.getPluginId() + ":" + mau.getAUId();
    assertEquals(expectedStr, lrsi.getAuKey(mau));
    assertEquals(expectedStr, lrsi.makeAuKey(mau.getPluginId(), mau.getAUId()));
  }

  public void testGetNewPluginDir() {
    // should start with the char before 'a'
    assertEquals(""+(char)('a'-1), lrsi.lastPluginDir);
    lrsi.getNewPluginDir();
    assertEquals("a", lrsi.lastPluginDir);
    lrsi.getNewPluginDir();
    assertEquals("b", lrsi.lastPluginDir);

    lrsi.lastPluginDir = "z";
    lrsi.getNewPluginDir();
    assertEquals("aa", lrsi.lastPluginDir);
    lrsi.getNewPluginDir();
    assertEquals("ba", lrsi.lastPluginDir);
    lrsi.lastPluginDir = "za";
    lrsi.getNewPluginDir();
    assertEquals("zb", lrsi.lastPluginDir);
    lrsi.lastPluginDir = ""+ (char)('a'-1);
  }

  public void testGetAuDirFromMap() {
    HashMap newNameMap = new HashMap();
    newNameMap.put(lrsi.getAuKey(mau), "testDir");
    lrsi.nameMap = newNameMap;
    StringBuffer buffer = new StringBuffer();
    lrsi.getAuDir(mau, buffer);
    assertEquals("testDir", buffer.toString());
  }

  public void testSaveAndLoadNames() {
    Properties newProps = new Properties();
    newProps.setProperty(lrsi.PLUGIN_ID_PROP, mau.getPluginId());
    newProps.setProperty(lrsi.AU_ID_PROP, mau.getAUId());

    HashMap newNameMap = new HashMap();
    newNameMap.put(lrsi.getAuKey(mau), "testDir");
    lrsi.nameMap = newNameMap;
    String location = lrsi.mapAuToFileLocation(lrsi.cacheLocation, mau);

    lrsi.saveAuIdProperties(location, newProps);
    File idFile = new File(location + File.separator + lrsi.AU_ID_FILE);
    assertTrue(idFile.exists());

    newProps = lrsi.getAuIdProperties(location);
    assertNotNull(newProps);
    assertEquals(mau.getPluginId(), newProps.getProperty(lrsi.PLUGIN_ID_PROP));
    assertEquals(mau.getAUId(), newProps.getProperty(lrsi.AU_ID_PROP));
  }

  public void testLoadNameMap() {
    Properties newProps = new Properties();
    newProps.setProperty(lrsi.PLUGIN_ID_PROP, mau.getPluginId());
    newProps.setProperty(lrsi.AU_ID_PROP, mau.getAUId());
    String location = lrsi.cacheLocation + File.separator + "ba";
    lrsi.saveAuIdProperties(location, newProps);

    lrsi.loadNameMap(lrsi.cacheLocation);
    assertEquals("ba", lrsi.nameMap.get(lrsi.getAuKey(mau)));
  }

  public void testLoadNameMapSkipping() {
    String location = lrsi.cacheLocation + File.separator + "ba";
    File dirFile = new File(location);
    dirFile.mkdirs();

    lrsi.loadNameMap(lrsi.cacheLocation);
    assertNull(lrsi.nameMap.get(lrsi.getAuKey(mau)));
  }


  public void testMapAuToFileLocation() {
    lrsi.lastPluginDir = "ca";
    String expectedStr = lrsi.cacheLocation+"/root/da/";
    assertEquals(expectedStr,
                 LockssRepositoryServiceImpl.mapAuToFileLocation(
        lrsi.cacheLocation+"/root", mau));
  }

  public void testGetAuDirSkipping() {
    String location = lrsi.cacheLocation + "root/ea";
    File dirFile = new File(location);
    dirFile.mkdirs();

    lrsi.lastPluginDir = "da";
    String expectedStr = lrsi.cacheLocation+"/root/fa/";
    assertEquals(expectedStr,
                 LockssRepositoryServiceImpl.mapAuToFileLocation(
        lrsi.cacheLocation+"/root", mau));
  }


  public void testMapUrlToFileLocation() throws MalformedURLException {
    String testStr = "http://www.example.com/branch1/branch2/index.html";
    String expectedStr = "root/www.example.com/http/branch1/branch2/index.html";
    assertEquals(expectedStr,
                 LockssRepositoryServiceImpl.mapUrlToFileLocation("root",
        testStr));

    try {
      testStr = ":/brokenurl.com/branch1/index/";
      LockssRepositoryServiceImpl.mapUrlToFileLocation("root", testStr);
      fail("Should have thrown MalformedURLException");
    } catch (MalformedURLException mue) {}
  }

  public void testGetLockssRepository() {
    String auId = mau.getAUId();
    lrsi.addLockssRepository(mau);
    LockssRepository repo1 = lrsi.getLockssRepository(mau);
    assertNotNull(repo1);
    mau.setAuId(auId + "test");
    LockssRepository repo2 = lrsi.addLockssRepository(mau);
    assertTrue(repo1 != repo2);
    mau.setAuId(auId);
    repo2 = lrsi.getLockssRepository(mau);
    assertEquals(repo1, repo2);

    mau.setAuId(auId + "test2");
    try {
      lrsi.getLockssRepository(mau);
      fail("Should throw IllegalArgumentException.");
    } catch (IllegalArgumentException iae) { }
  }

  public static void configCacheLocation(String location)
    throws IOException {
    String s = LockssRepositoryServiceImpl.PARAM_CACHE_LOCATION + "=" + location;
    TestConfiguration.setCurrentConfigFromString(s);
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestLockssRepositoryServiceImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
