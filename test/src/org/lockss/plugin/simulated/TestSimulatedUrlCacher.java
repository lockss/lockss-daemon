/*
 * $Id: TestSimulatedUrlCacher.java,v 1.15 2003-04-02 00:19:40 aalto Exp $
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

import java.io.*;
import java.util.Properties;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.repository.TestLockssRepositoryServiceImpl;
import org.lockss.plugin.*;
import org.lockss.util.StreamUtil;

/**
 * This is the test class for org.lockss.plugin.simulated.SimulatedUrlCacher
 *
 * @author  Emil Aalto
 * @version 0.0
 */


public class TestSimulatedUrlCacher extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;
  private String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestLockssRepositoryServiceImpl.configCacheLocation(tempDirPath);

    theDaemon = new MockLockssDaemon();
    theDaemon.getLockssRepositoryService().startService();

    mau = new MockArchivalUnit();
    theDaemon.getLockssRepository(mau);
  }

  public void tearDown() throws Exception {
    theDaemon.getLockssRepositoryService().stopService();
    super.tearDown();
  }

  public void testHtmlProperties() throws Exception {
    String testStr = "http://www.example.com/index.html";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(
        new MockCachedUrlSet(mau, null), testStr, "");
    Properties prop = suc.getUncachedProperties();
    assertEquals("text/html", prop.getProperty("content-type"));
    assertEquals(testStr, prop.getProperty("content-url"));
  }
  public void testTextProperties() throws Exception {
    String testStr = "http://www.example.com/file.txt";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(
        new MockCachedUrlSet(mau, null), testStr, "");
    Properties prop = suc.getUncachedProperties();
    assertEquals("text/plain", prop.getProperty("content-type"));
    assertEquals(testStr, prop.getProperty("content-url"));
  }
  public void testPdfProperties() throws Exception {
    String testStr = "http://www.example.com/file.pdf";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(
        new MockCachedUrlSet(mau, null), testStr, "");
    Properties prop = suc.getUncachedProperties();
    assertEquals("application/pdf", prop.getProperty("content-type"));
    assertEquals(testStr, prop.getProperty("content-url"));
  }
  public void testJpegProperties() throws Exception {
    String testStr = "http://www.example.com/image.jpg";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(
        new MockCachedUrlSet(mau, null), testStr, "");
    Properties prop = suc.getUncachedProperties();
    assertEquals("image/jpeg", prop.getProperty("content-type"));
    assertEquals(testStr, prop.getProperty("content-url"));
  }

  public void testNoBranchContent() throws Exception {
    File branchFile = new File(tempDirPath, "simcontent/branch1");
    branchFile.mkdirs();

    String testStr = "http://www.example.com/branch1";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(
        new MockCachedUrlSet(mau, null), testStr, tempDirPath);
    assertNull(suc.getUncachedInputStream());
  }

  public void testBranchContent() throws Exception {
    File branchFile = new File(tempDirPath,
                               "simcontent/branch1");
    branchFile.mkdirs();
    File contentFile = new File(branchFile, "branch_content");
    FileOutputStream fos = new FileOutputStream(contentFile);
    StringInputStream sis = new StringInputStream("test stream");
    StreamUtil.copy(sis, fos);
    fos.close();
    sis.close();

    String testStr = "http://www.example.com/branch1";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(
        new MockCachedUrlSet(mau, null), testStr, tempDirPath);
    InputStream is = suc.getUncachedInputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(is, baos);
    is.close();
    assertEquals("test stream", baos.toString());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestSimulatedUrlCacher.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
