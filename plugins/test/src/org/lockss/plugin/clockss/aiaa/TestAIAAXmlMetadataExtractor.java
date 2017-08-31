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

package org.lockss.plugin.clockss.aiaa;
import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestAIAAXmlMetadataExtractor extends LockssTestCase {

  private static final Logger log = Logger.getLogger(TestAIAAXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String BASE_URL = "http://www.source.org/";
  private static final String xml_article_url = BASE_URL + "2017/foo.zip!/miecec03/6.2003-6092/6.2003-6092.xml";
  private static final String xml_index_url = BASE_URL + "2017/foo.zip!/miecec03/miecec03/miecec03.xml";
  private static final String pdf_url = BASE_URL + "2017/foo.zip!/miecec03/6.2003-6092/6.2003-6092.pdf";

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
    conf.put("year", "2017");
    return conf;
  }


  
  private static final String realXMLFile1 = "AIAAArticleSourceTest.xml";
  private static final String realXMLFile2 = "AIAAIndexSourceTest.xml";

  public void testFromArticleXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile1);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();    
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_article_url, true, true, xmlHeader);
      // Now add all the pdf files in our AU since we check for them before emitting
      mau.addUrl(pdf_url, true, true, xmlHeader);

      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new  AIAAXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
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
        //log.info(mdRecord.ppString(2));
        assertEquals(mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE),"International Conference");
        assertEquals(mdRecord.get(MetadataField.FIELD_ARTICLE_TITLE),"Presentation Title");
        assertEquals(mdRecord.get(MetadataField.FIELD_DOI),"10.2514/6.2003-chapter");
        assertEquals(mdRecord.get(MetadataField.FIELD_DATE),"2003"); //pulled from pdf name
        assertEquals(mdRecord.get(MetadataField.FIELD_ARTICLE_TYPE),MetadataField.ARTICLE_TYPE_BOOKCHAPTER);
        assertEquals(mdRecord.get(MetadataField.FIELD_PUBLICATION_TYPE),MetadataField.PUBLICATION_TYPE_BOOK);
        assertEquals(mdRecord.get(MetadataField.FIELD_ACCESS_URL).endsWith(".pdf"),true);
      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }
  
  public void testFromVolumeXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile2);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();    
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_index_url, true, true, xmlHeader);
      // no pdf file for this one
      
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new  AIAAXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
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
        //log.info(mdRecord.ppString(2));
        assertEquals(mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE),"International Conference");
        assertEquals(mdRecord.get(MetadataField.FIELD_ARTICLE_TITLE),"International Conference");
        assertEquals(mdRecord.get(MetadataField.FIELD_ISBN),"978-1-11111-111-1");
        assertEquals(mdRecord.get(MetadataField.FIELD_DOI),"10.2514/BOOK");
        assertEquals(mdRecord.get(MetadataField.FIELD_DATE),"January 9, 2017");
        assertEquals(mdRecord.get(MetadataField.FIELD_ARTICLE_TYPE),MetadataField.ARTICLE_TYPE_BOOKVOLUME);
        assertEquals(mdRecord.get(MetadataField.FIELD_PUBLICATION_TYPE),MetadataField.PUBLICATION_TYPE_BOOK);
        assertEquals(mdRecord.get(MetadataField.FIELD_ACCESS_URL).endsWith(".pdf"),false);
      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }

}