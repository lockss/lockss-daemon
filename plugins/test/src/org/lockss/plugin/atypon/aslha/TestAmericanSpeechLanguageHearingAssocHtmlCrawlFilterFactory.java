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


package org.lockss.plugin.atypon.aslha;

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

  FilterFactory variantCrawlFact = new AmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory();
  FilterFactory variantHashFact = new AmericanSpeechLanguageHearingAssocHtmlHashFilterFactory();
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

  private static final String manifestHashFiltered = " December 2018 (Vol. 27 Issue 4) November 2018 (Vol. 27 Issue 3S) " +
          "September 2018 (Vol. 27 Issue 3) June 2018 (Vol. 27 Issue 2) March 2018 (Vol. 27 Issue 1) ";

  private static String tocContent = getHtmlContent("tocContent.html");
  private static String tocContentCrawlFiltered = getHtmlContent("tocContentCrawlFiltered.html");

  private static String doiFullContent = getHtmlContent("doiFullContent.html");
  private static String doiFullContentCrawlFiltered = getHtmlContent("doiFullContentCrawlFiltered.html");

  private static String doiAbsContent = getHtmlContent("doiAbsContent.html");
  private static String doiAbsContentCrawlFiltered = getHtmlContent("doiAbsContentCrawlFiltered.html");

  private static String doiPdfContent = getHtmlContent("doiPdfContent.html");
  private static String doiPdfContentCrawlFiltered = getHtmlContent("doiPdfContentCrawlFiltered.html");

  private static final String tocContentHashFiltered = " Clinical Focus Clinical Focus 08 March 2018 Audiological Assessment of Word Recognition Skills in Persons With Aphasia Author1 , Author2 , Author3 , Author4 , Author5 , Author6 , and Author7 https://doi.org/part1/part2 Preview Abstract Purpose section Abstract Full text PDF issue item header section issue item body section accordion section Abstract Full text PDF issue item section issue item section Research Articles Research Articles issue item header section Research Articles issue item body section Research Articles accordion section Abstract Full text PDF Review Article Review Article issue item header section Review Article issue item body section Review Article accordion section Abstract Full text PDF ";
  private static final String doiFullContentHashFiltered = "  Full Content Purpose some content";
  private static final String doiAbsContentHashFiltered = "  Abstract Purpose some content Method some content Result some content Conclusion some content";
  private static final String doiPdfContentHashFiltered = "  PDF Content Background some content Purpose some content Method some content Results some content Conclusions some content";

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

  private static String getUTF8String(String str) {
    return java.nio.charset.StandardCharsets.UTF_8.encode(str).toString();
  }

  private static String getFilteredContent(ArchivalUnit au, FilterFactory fact, String nameToHash) {

    InputStream actIn;
    String filteredStr = "";

    try {
      actIn = fact.createFilteredInputStream(au,
              new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);

      try {
        filteredStr = StringUtil.fromInputStream(actIn);

      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (PluginException e) {
      e.printStackTrace();
    }

    return filteredStr;
  }

  private static String getHtmlContent(String fname) {
    String htmlContent = "";
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/aslha/" + fname;
     htmlContent  = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return htmlContent;
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

  public void testTocContentLengthComparision() {

    int tocContentLen = tocContent.length();
    int tocContentCrawlFilteredLen = tocContentCrawlFiltered.length();
    int tocContentHashFilteredLen = tocContentHashFiltered.length();

    assertTrue(tocContentLen > 0);
    assertTrue(tocContentCrawlFilteredLen > 0);
    assertTrue(tocContentHashFilteredLen > 0);
    assertTrue(tocContentLen > tocContentCrawlFilteredLen);
    assertTrue(tocContentLen > tocContentHashFilteredLen);

  }

  public void testDoiFullContentLengthComparision() {

    int doiFullContentLen = doiFullContent.length();
    int doiFullContentCrawlFilteredLen = doiFullContentCrawlFiltered.length();
    int doiFullContentHashFilteredLen = doiFullContentHashFiltered.length();

    assertTrue(doiFullContentLen > 0);
    assertTrue(doiFullContentCrawlFilteredLen > 0);
    assertTrue(doiFullContentHashFilteredLen > 0);
    assertTrue(doiFullContentLen > doiFullContentCrawlFilteredLen);
    assertTrue(doiFullContentLen > doiFullContentHashFilteredLen);
  }

  public void testDoiAbsContentLengthComparision() {

    int doiAbsContentLen = doiAbsContent.length();
    int doiAbsContentCrawlFilteredLen = doiAbsContentCrawlFiltered.length();
    int doiAbsContentHashFilteredLen = doiAbsContentHashFiltered.length();

    assertTrue(doiAbsContentLen > 0);
    assertTrue(doiAbsContentCrawlFilteredLen > 0);
    assertTrue(doiAbsContentHashFilteredLen > 0);
    assertTrue(doiAbsContentLen > doiAbsContentCrawlFilteredLen);
    assertTrue(doiAbsContentLen > doiAbsContentHashFilteredLen);
  }

  public void testPdfContentLengthComparision() {

    int doiPdfContentLen = doiPdfContent.length();
    int doiPdfContentCrawlFilteredLen = doiPdfContentCrawlFiltered.length();
    int doiPdfContentHashFilteredLen = doiPdfContentHashFiltered.length();

    assertTrue(doiPdfContentLen > 0);
    assertTrue(doiPdfContentCrawlFilteredLen > 0);
    assertTrue(doiPdfContentHashFilteredLen > 0);
    assertTrue(doiPdfContentLen > doiPdfContentCrawlFilteredLen);
    assertTrue(doiPdfContentLen > doiPdfContentHashFilteredLen);
  }

  public static class TestCrawl extends TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory {
    public void testTocContentFiltering() throws Exception {
      String unicodeFilteredStr = getUTF8String(getFilteredContent(mau, variantCrawlFact, tocContent));
      String unicodeExpectedStr = getUTF8String(tocContentCrawlFiltered);
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }

    public void testFullContentFiltering() throws Exception {
      String unicodeFilteredStr = getUTF8String(getFilteredContent(mau, variantCrawlFact, doiFullContent));
      String unicodeExpectedStr = getUTF8String(doiFullContentCrawlFiltered);
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }

    public void testAbsContentFiltering() throws Exception {
      String unicodeFilteredStr = getUTF8String(getFilteredContent(mau, variantCrawlFact, doiAbsContent));
      String unicodeExpectedStr = getUTF8String(doiAbsContentCrawlFiltered);
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }

    public void testPdfContentFiltering() throws Exception {
      String unicodeFilteredStr = getUTF8String(getFilteredContent(mau, variantCrawlFact, doiPdfContent));
      String unicodeExpectedStr = getUTF8String(doiPdfContentCrawlFiltered);
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }
  }

  public static class TestHash extends TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory {
    public void testManifestHash() throws Exception {
      String unicodeFilteredStr = getUTF8String(getFilteredContent(mau, variantHashFact, manifestContent));
      String unicodeExpectedStr = getUTF8String(manifestHashFiltered);
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
      //assertEquals("run this one", "run that one");
    }

    public void testTocContentHash() throws Exception {
      String unicodeFilteredStr = getUTF8String(getFilteredContent(mau, variantHashFact, tocContent));
      String unicodeExpectedStr = getUTF8String(tocContentHashFiltered);
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }

    public void testFullContentHash() throws Exception {
      String unicodeFilteredStr = getUTF8String(getFilteredContent(mau, variantHashFact, doiFullContent));
      String unicodeExpectedStr = getUTF8String(doiFullContentHashFiltered);

      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }

    public void testAbsContentHash() throws Exception {
      String unicodeFilteredStr = getUTF8String(getFilteredContent(mau, variantHashFact, doiAbsContent));
      String unicodeExpectedStr = getUTF8String(doiAbsContentHashFiltered);
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }

    public void testPdfContentHash() throws Exception {
      String unicodeFilteredStr = getUTF8String(getFilteredContent(mau, variantHashFact, doiPdfContent));
      String unicodeExpectedStr = getUTF8String(doiPdfContentHashFiltered);
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }
  }

  public static Test suite() {
    return variantSuites(new Class[] {
            TestCrawl.class,
            TestHash.class
    });
  }

}
