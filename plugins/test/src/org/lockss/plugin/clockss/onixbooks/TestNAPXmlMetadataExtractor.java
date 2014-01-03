/*
 * $Id: TestNAPXmlMetadataExtractor.java,v 1.1 2014-01-03 16:48:57 alexandraohlson Exp $
 */
/*

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.onixbooks;

import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.definable.DefinablePlugin;


public class TestNAPXmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestNAPXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.onixbooks.ClockssNAPBooksSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";

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
    
    /* must set up plugin to get helper name */
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
        PLUGIN_NAME);
    mau.setPlugin(ap);

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
    conf.put("year", "2013");
    return conf;
  }
 
  private static final String realXMLFile = "NAPSourceTest.xml";

  // filename is based on <RecordReference>
  private static final String pdfUrl1 = "http://www.source.com/8.stamped.pdf";
  private static final ArrayList md1 = (ArrayList) ListUtil.list(
      "9999888877776",
      "Firstpart: Second, More, and Additional",
      "19840101",
      "Committee for the Study of Stuff");
  
  private static final String pdfUrl2 = "http://www.source.com/18611.stamped.pdf";
  private static final ArrayList md2 = (ArrayList) ListUtil.list(
      "9780303030303",
      "Special Report 1: Research for the Future",
      "20140604",
      null);
  
  public void testFromNAPXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();  
      String xml_url = "http://www.source.com/TestXML.xml";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      // Now add all the pdf files in our AU since we check for them before emitting
      // doesn't matter what the header type is, it's not checked
      mau.addUrl(pdfUrl1, true, true, xmlHeader);
      mau.addUrl(pdfUrl2, true, true, xmlHeader);

    FileMetadataExtractor me = new SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractor();
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      assertEquals(2, mdlist.size());

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        compareMetadata(mdRecord);
      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }
 
  private static final int ISBN_INDEX = 0;
  private static final int TITLE_INDEX = 1;
  private static final int DATE_INDEX = 2;
  private static final int AUTHOR_INDEX = 3;
  private static final int PUB_INDEX = 4;
  
  
  static private final Map<String, List> expectedMD =
      new HashMap<String,List>();
  static {
    expectedMD.put(pdfUrl1, md1);
    expectedMD.put(pdfUrl2, md2);
  }

  private void compareMetadata(ArticleMetadata AM) {
    String accessUrl = AM.get(MetadataField.FIELD_ACCESS_URL);
    ArrayList expected = (ArrayList) expectedMD.get(accessUrl);
    
    assertNotNull(expected);
    assertEquals(expected.get(ISBN_INDEX), AM.get(MetadataField.FIELD_ISBN));
    //assertEquals(AM.getList(MetadataField.FIELD_AUTHOR).toString(), expected.get(AUTHOR_INDEX));
    assertEquals(expected.get(TITLE_INDEX), AM.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(expected.get(DATE_INDEX), AM.get(MetadataField.FIELD_DATE));
    assertEquals(expected.get(AUTHOR_INDEX),AM.get(MetadataField.FIELD_AUTHOR));
    //assertEquals(expected.get(PUB_INDEX), AM.get(MetadataField.FIELD_PUBLISHER));
    
  }
}
