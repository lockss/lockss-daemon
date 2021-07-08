/*
 * $Id:$
 */
/*

/*

 Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.eastview
;
import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestEastviewMetadataExtractor extends LockssTestCase {

  private static final Logger log = Logger.getLogger(TestEastviewMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.eastview.ClockssEastviewSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  private static String YEAR = "2017";
  private static String GZ_URL = BASE_URL + YEAR + "/EASTVIEW.tar.gz!/";
  private static final String xml_url1 = GZ_URL + "cdc/CDC/data/001_01/CDC-2016-5-001-46016614.xml";
  private static final String pdf_url1 = GZ_URL + "cdc/CDC/data/001_01/CDC-2016-5-001-46016614.pdf";
  private static final String xml_url2 = GZ_URL + "cdc/CDC/data/001_01/CDC-2015-4-001-42924732.xml";

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

  /*
  public void testFromRealFileWithPDF() throws Exception {
    
  }
  */

  public void testFromRealFileWithPDF() throws Exception {
    InputStream file_input = null;
    try {
      // 1. Try this with and XML and pdf file 
      file_input = getResourceAsStream("test_eastview.xml");
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();    
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url1, true, true, xmlHeader);
      // Now add all the pdf files in our AU since we check for them before emitting
      mau.addUrl(pdf_url1, true, true, xmlHeader);

      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new  EastviewXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      // only one of the two records had a matching pdf
      assertEquals(1, mdlist.size());

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        //log.info(mdRecord.ppString(2));
        compareMetadata(mdRecord);
        assertEquals(pdf_url1, mdRecord.get(MetadataField.FIELD_ACCESS_URL));

      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }

  public void testFromRealFileNoPDF() throws Exception {
    InputStream file_input = null;
    try {
      // 1. Try this with and XML and pdf file 
      file_input = getResourceAsStream("test_eastview.xml");
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();    
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url2, true, true, xmlHeader);

      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new  EastviewXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      // only one of the two records had a matching pdf
      assertEquals(1, mdlist.size());

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        //log.info(mdRecord.ppString(2));
        compareMetadata(mdRecord);
        assertEquals(xml_url2, mdRecord.get(MetadataField.FIELD_ACCESS_URL));
      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }

  
  String[] authorlist = new String[] {"Hamilton, Alexander", "Burr, Aaron"};
  // quick and dirty, there are only two records
  private void compareMetadata(ArticleMetadata AM) {
    assertEquals("Current Digest of the Chinese Press, The", AM.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals("2015-01-01", AM.get(MetadataField.FIELD_DATE));
    //assertEquals("East View Information Services", AM.get(MetadataField.FIELD_PUBLISHER));  read from tdb file in after process
    assertEquals("4", AM.get(MetadataField.FIELD_VOLUME));
    assertEquals("001", AM.get(MetadataField.FIELD_ISSUE));

  }
}
