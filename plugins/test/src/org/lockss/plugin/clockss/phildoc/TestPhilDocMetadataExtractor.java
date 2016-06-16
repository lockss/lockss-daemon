/*
 * $Id:$
 */
/*

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.clockss.phildoc;
import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestPhilDocMetadataExtractor extends LockssTestCase {

  private static final Logger log = Logger.getLogger(TestPhilDocMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.phildoc.ClockssPhilosophyDocumentationCenterSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  private static String YEAR = "2016";
  private static String ISSUE_URL = BASE_URL + YEAR + "/pdoc/pdoc_17-1/";
  private static final String xml_url = ISSUE_URL + "pdoc_17-1.xml";
  private static final String filenum1 = "luxx_2016_0019_0001_0005_0013";
  private static final String filenum2 = "luxx_2016_0019_0001_0057_0078";
  private static final String filenum3 = "luxx_2016_0019_0001_0015_0040";
  private static final String filenum4 = "luxx_2016_0019_0001_0041_0056";
  private static final String pdf1_url = ISSUE_URL +  "pdf/" + filenum1 + ".pdf";
  private static final String pdf2_url = ISSUE_URL + "pdf/" + filenum2 + ".pdf";
  private static final String pdf3_url = ISSUE_URL + "pdf/" + filenum3 + ".pdf";
  private static final String pdf4_url = ISSUE_URL + "pdf/" + filenum4 + ".pdf";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    mau = new MockArchivalUnit();

    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau.setConfiguration(auConfig());

  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", YEAR);
    return conf;
  }

 
  private static final String realXMLFile = "test_phildoc.xml";

  public void testFromJatsPublishingXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();    
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
      // Now add all the pdf files in our AU since we check for them before emitting
      mau.addUrl(pdf1_url, true, true, xmlHeader);
      mau.addUrl(pdf2_url, true, true, xmlHeader);
      mau.addUrl(pdf3_url, true, true, xmlHeader);
      mau.addUrl(pdf4_url, true, true, xmlHeader);

      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new  PhilDocXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      // only one of the two records had a matching pdf
      assertEquals(3, mdlist.size());

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        //log.info(mdRecord.ppString(2));
        compareMetadata(mdRecord);
      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }
                   
  String[] authorlist = new String[] {"Hamilton, Alexander", "Burr, Aaron"};
  // quick and dirty, there are only two records
  private void compareMetadata(ArticleMetadata AM) {
    String fname = AM.getRaw("field[@name=\"imuse_id\"]");
    assertEquals("Luxx: A Journal of Culture", AM.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals("2016", AM.get(MetadataField.FIELD_DATE));
    assertEquals("Philosophy Documentation Center", AM.get(MetadataField.FIELD_PUBLISHER));
    assertEquals("19", AM.get(MetadataField.FIELD_VOLUME));
    assertEquals("1", AM.get(MetadataField.FIELD_ISSUE));
    assertEquals("1091-9999", AM.get(MetadataField.FIELD_ISSN));
    if (filenum1.equals(fname)) {
      assertEquals("Preface", AM.get(MetadataField.FIELD_ARTICLE_TITLE));
    } else if (filenum2.equals(fname)) {
      assertEquals("Article One Title: Exploring the Subtitle", AM.get(MetadataField.FIELD_ARTICLE_TITLE));
      assertEquals(Arrays.asList(authorlist), AM.getList(MetadataField.FIELD_AUTHOR));
    } else if (filenum3.equals(fname)) {
      assertEquals("Second Title Goes Here", AM.get(MetadataField.FIELD_ARTICLE_TITLE));
    } else {
      //fail
      log.info("The filename didn't match our test cases: " + fname);
      assertFalse(true);
    }
  }
}
