/*
 * $Id: TestNAPXmlMetadataExtractor.java,v 1.1 2014-06-20 16:05:05 alexandraohlson Exp $
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

package org.lockss.plugin.clockss.nap;

import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestNAPXmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestNAPXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.nap.ClockssNAPBooksSourcePlugin";
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

  //TODO - 
  // add a test to check the splitting of an author
  // add a test to check title: subtitle permutations
  // ??
  
  public void testFromNAPXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();  
      String xml_url = "http://www.source.com/10001.xml";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      // Now add all the pdf files in our AU since we check for them before emitting
      // doesn't matter what the header type is, it's not checked
      mau.addUrl(pdfUrl1, true, true, xmlHeader);

    FileMetadataExtractor me = new NAPSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      assertEquals(1, mdlist.size());

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
  
  /* The order of expected values is:
   *  isbn
   *  title
   *  date
   *  author
   *  pub
   */
  // filename is based on <RecordReference>
  private static final String pdfUrl1 = "http://www.source.com/10001.stamped.pdf";
  private static final ArrayList md1 = (ArrayList) ListUtil.list(
      "0111111110",
      "Advanced Stuff for Future Wizards, Gremlins, and Physicists: Seventh Lecture International Dark Arts Series",
      "2000-11-28",
      "Arthur P. Somebody, University of California at Hogwarts, Organized by the National Research Council and the Office of the Dark Arts", // a "corporate name" author
      "National Academies Press");
  

  
  static private final Map<String, List> expectedMD =
      new HashMap<String,List>();
  static {
    expectedMD.put(pdfUrl1, md1);
  }

  private void compareMetadata(ArticleMetadata AM) {
    String accessUrl = AM.get(MetadataField.FIELD_ACCESS_URL);
    ArrayList expected = (ArrayList) expectedMD.get(accessUrl);
    
    assertNotNull(expected);
    assertEquals(expected.get(ISBN_INDEX), AM.get(MetadataField.FIELD_ISBN));
    assertEquals(expected.get(TITLE_INDEX), AM.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(expected.get(DATE_INDEX), AM.get(MetadataField.FIELD_DATE));
    assertEquals(expected.get(AUTHOR_INDEX),AM.get(MetadataField.FIELD_AUTHOR));
    // This isn't in the metadata and can come from the TDB file
    //    assertEquals(expected.get(PUB_INDEX), AM.get(MetadataField.FIELD_PUBLISHER)); // Though it gets overridden by tdb value
    
  }
}
