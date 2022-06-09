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

import java.io.*;
import java.util.Properties;

import org.lockss.test.*;
import org.lockss.test.MockCrawler.MockCrawlerFacade;
import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.plugin.simulated.SimulatedUrlCacher
 *
 * @author  Emil Aalto
 * @version 0.0
 */


public class TestSimulatedUrlFetcher extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau;
  private String tempDirPath;
  MockCrawlerFacade mcf;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tempDirPath);

    theDaemon = getMockLockssDaemon();
    theDaemon.getPluginManager();
    Plugin simPlugin = PluginTestUtil.findPlugin(SimulatedPlugin.class);

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    sau = new SimulatedArchivalUnit(simPlugin);
    Configuration auConfig = ConfigurationUtil.fromArgs("root", tempDirPath);
    sau.setConfiguration(auConfig);
    mcf = new MockCrawler().new MockCrawlerFacade();
    mcf.setAu(sau);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testHtmlProperties() throws Exception {
    String testStr = "http://www.example.com/index.html";
    SimulatedUrlFetcher suf = new SimulatedUrlFetcher(mcf, testStr, "");
    Properties prop = suf.getUncachedProperties();
    assertEquals("text/html",
		 prop.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE));
    assertEquals(testStr, prop.getProperty(CachedUrl.PROPERTY_ORIG_URL));
  }

  public void testTextProperties() throws Exception {
    String testStr = "http://www.example.com/file.txt";
    SimulatedUrlFetcher suf = new SimulatedUrlFetcher(mcf, testStr, "");
    Properties prop = suf.getUncachedProperties();
    assertEquals("text/plain",
		 prop.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE));
    assertEquals(testStr, prop.getProperty(CachedUrl.PROPERTY_ORIG_URL));
  }

  public void testPdfProperties() throws Exception {
    String testStr = "http://www.example.com/file.pdf";
    SimulatedUrlFetcher suf = new SimulatedUrlFetcher(mcf, testStr, "");
    Properties prop = suf.getUncachedProperties();
    assertEquals("application/pdf",
		 prop.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE));
    assertEquals(testStr, prop.getProperty(CachedUrl.PROPERTY_ORIG_URL));
  }

  public void testJpegProperties() throws Exception {
    String testStr = "http://www.example.com/image.jpg";
    SimulatedUrlFetcher suf = new SimulatedUrlFetcher(mcf, testStr, "");
    Properties prop = suf.getUncachedProperties();
    assertEquals("image/jpeg",
		 prop.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE));
    assertEquals(testStr, prop.getProperty(CachedUrl.PROPERTY_ORIG_URL));
  }

  public void testNoBranchContent() throws Exception {
    File branchFile = new File(tempDirPath, "simcontent/branch1");
    branchFile.mkdirs();

    String testStr = "http://www.example.com/branch1";
    SimulatedUrlFetcher suf = new SimulatedUrlFetcher(mcf, testStr, tempDirPath);
    assertNull(suf.getUncachedInputStream());
  }

  public void testNoBranchContentWithRedirToAutoIndex() throws Exception {
    Configuration auConfig = sau.getConfiguration();
    auConfig.put("redirectDirToIndex", "true");
    auConfig.put("autoGenIndexHtml", "true");
    sau.setConfiguration(auConfig);

    File branchFile = new File(tempDirPath, "simcontent/branch1");
    branchFile.mkdirs();

    String testStr = "http://www.example.com/branch1";
    SimulatedUrlFetcher suf = new SimulatedUrlFetcher(mcf, testStr, tempDirPath);
    String cont = StringUtil.fromInputStream(suf.getUncachedInputStream());
    assertMatchesRE("In " + suf, "<A HREF=\"branch1/index.html\">", cont);
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
    SimulatedUrlFetcher suf = new SimulatedUrlFetcher(mcf, testStr, tempDirPath);
    InputStream is = suf.getUncachedInputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(is, baos);
    is.close();
    assertEquals("test stream", baos.toString());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestSimulatedUrlFetcher.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
