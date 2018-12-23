/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the \"Software\"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/


package org.lockss.plugin.atypon.americanspeechlanguagehearingassoc;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.*;
import org.apache.commons.io.FileUtils;


public class TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory extends LockssTestCase {

  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;

  private static final String PLUGIN_ID =
          "org.lockss.plugin.atypon.americanspeechlanguagehearingassoc.AmericanSpeechLanguageHearingAssocAtyponPlugin";

  private static final String manifestContent =
      "<html>\n" +
              "<head>\n" +
              "    <title>American Journal of Audiology 2018 CLOCKSS Manifest Page</title>\n" +
              "    <meta charset=\"UTF-8\" />\n" +
              "</head>\n" +
              "<body>\n" +
              "<h1>American Journal of Audiology 2018 CLOCKSS Manifest Page</h1>\n" +
              "<ul>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/4\">December 2018 (Vol. 27 Issue 4)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/3S\">November 2018 (Vol. 27 Issue 3S)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/3\">September 2018 (Vol. 27 Issue 3)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/2\">June 2018 (Vol. 27 Issue 2)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/1\">March 2018 (Vol. 27 Issue 1)</a></li>\n" +
              "    \n" +
              "</ul>\n" +
              "<p>\n" +
              "    <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" height=\"108\" width=\"108\" alt=\"LOCKSS logo\"/>\n" +
              "    The CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit.\n" +
              "</p>\n" +
              "</body>\n" +
              "</html>";

  private static final String manifestHashFiltered =
      "December 2018 (Vol. 27 Issue 4)" +
      "November 2018 (Vol. 27 Issue 3S)" +
      "September 2018 (Vol. 27 Issue 3)" +
      "June 2018 (Vol. 27 Issue 2)" +
      "March 2018 (Vol. 27 Issue 1)";


  // Since Java String can not exceed 64K, need to read the html content from file to test it
  private static String tocContent = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/tocContent.html";
      tocContent  = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String tocContentCrawlFiltered = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/tocContentCrawlFiltered.html";
      tocContentCrawlFiltered  = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String doiFullContent = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/doiFullContent.html";
      doiFullContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String doiFullContentCrawlFiltered = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/doiFullContentCrawlFiltered.html";
      doiFullContentCrawlFiltered  = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String doiAbsContent = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/doiAbsContent.html";
      doiAbsContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String doiAbsContentCrawlFiltered = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/doiAbsContentCrawlFiltered.html";
      doiAbsContentCrawlFiltered  = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static final String tocContentHashFiltered = "";


  private static final String art1ContentHashFiltered = "";

  private static final String citContent = "";

  private static final String citContentHashFiltered = "";

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
  }

  private Configuration thisAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_id", "abc");
    conf.put("volume_name", "99");
    return conf;
  }

  private static void doFilterTest(ArchivalUnit au, FilterFactory fact, String nameToHash, String expectedStr) {
    InputStream actIn;
    try {
      actIn = fact.createFilteredInputStream(au,
              new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);

      try {
        String filteredStr = StringUtil.fromInputStream(actIn);

        //Write to a file for easy comparision for debugging purpose

        String currentDirectory = System.getProperty("user.dir");
        String pathname = currentDirectory +
                "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/generated.html";
        FileUtils.writeStringToFile(new File(pathname),filteredStr,Constants.DEFAULT_ENCODING);

        assertEquals(expectedStr, filteredStr);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (PluginException e) {
      e.printStackTrace();
    }
  }

  public void startMockDaemon() {
    daemon = getMockLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
  }

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();
    startMockDaemon();
    mau = createAu();
  }

  public void testTocContentComparision() {

    int tocContentLen = tocContent.length();
    int tocContentCrawlFilteredLen = tocContentCrawlFiltered.length();

    int doiFullContentLen = doiFullContent.length();
    int doiFullContentCrawlFilteredLen = doiFullContentCrawlFiltered.length();

    int doiAbsContentLen = doiAbsContent.length();
    int doiAbsContentCrawlFilteredLen = doiAbsContentCrawlFiltered.length();

    assertTrue(tocContentLen > 0);
    assertTrue(tocContentCrawlFilteredLen > 0);
    assertTrue(tocContentLen > tocContentCrawlFilteredLen);

    assertTrue(doiFullContentLen > 0);
    assertTrue(doiFullContentCrawlFilteredLen > 0);
    assertTrue(doiFullContentLen > doiFullContentCrawlFilteredLen);

    assertTrue(doiAbsContentLen > 0);
    assertTrue(doiAbsContentCrawlFilteredLen > 0);
    assertTrue(doiAbsContentLen > doiAbsContentCrawlFilteredLen);
  }

  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new AmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory();
      doFilterTest(mau, variantFact, manifestContent, manifestContent);
//      doFilterTest(mau, variantFact, tocContent, tocContentCrawlFiltered);
//      doFilterTest(mau, variantFact, doiFullContent, doiFullContentCrawlFiltered);
//      doFilterTest(mau, variantFact, doiAbsContent, doiAbsContentCrawlFiltered);
    }
  }

  // Variant to test with Hash Filter
  public static class TestHash extends TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new AmericanSpeechLanguageHearingAssocHtmlHashFilterFactory();
      //doFilterTest(mau, variantFact, manifestContent, manifestHashFiltered);
      //doFilterTest(mau, variantFact, tocContent, tocContentHashFiltered);
      //doFilterTest(mau, variantFact, art1Content, art1ContentHashFiltered);
      //doFilterTest(mau, variantFact, citContent, citContentHashFiltered);
    }
  }

  public static Test suite() {
    return variantSuites(new Class[] {
            TestCrawl.class,
            TestHash.class
    });
  }

}
