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

package org.lockss.plugin.clockss.aofoundation;
import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestCrossRefXmlMetadataExtractor extends LockssTestCase {

  private static final Logger log = Logger.getLogger(TestCrossRefXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String BASE_URL = "http://www.source.org/";
  private static final String xml_url = BASE_URL + "2016/vol01/cr4a337132155c63964d2-283c.xml ";
  private static final String pdf_url1 = BASE_URL + "2016/vol01/vxxxxx1.pdf";
  private static final String pdf_url2 = BASE_URL + "2016/vol01/vxxxxx2.pdf";
  private static final String pdf_url3 = BASE_URL + "2016/vol01/vxxxxx3.pdf";

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
    conf.put("year", "2016");
    return conf;
  }


  
  private static final String realXMLFile = "CrossRefTest.xml";

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
      mau.addUrl(pdf_url1, true, true, xmlHeader);
      mau.addUrl(pdf_url2, true, true, xmlHeader);
      mau.addUrl(pdf_url3, true, true, xmlHeader);

      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new  AOFoundationSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      assertEquals(3, mdlist.size());

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        log.debug3(mdRecord.ppString(2));
        //compareMetadata(mdRecord);
      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }

}