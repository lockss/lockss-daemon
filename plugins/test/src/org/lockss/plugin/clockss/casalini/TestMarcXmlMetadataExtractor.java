/*
 * $Id$
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

package org.lockss.plugin.clockss.casalini;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestMarcXmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestMarcXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.casalini.ClockssCasaliniLibriSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  private static String YEAR = "2016";
  private static String pdfUrl1 = BASE_URL + YEAR + "/Monographs/ATENEO/2249531/2279431.pdf";
  private static String pdfUrl2 = BASE_URL + YEAR + "/Monographs/ATENEO/2249531/2279430.pdf";

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
  

  private static final String realXMLFile = "MarcXmlTest.xml";

  public void testFromMarcXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();   
      String xml_url = BASE_URL + YEAR + "/TestXML.xml";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      
      // Now add two PDF files so they can be "found" 
      // these aren't opened, so it doens't matter that they have the wrong header type
      mau.addUrl(pdfUrl1, true, true, xmlHeader);
      mau.addUrl(pdfUrl2, true, true, xmlHeader);

    FileMetadataExtractor me = new CasaliniLibriMarcXmlMetadataExtractorFactory().createFileMetadataExtractor(
        MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      assertEquals(2, mdlist.size());
      
      // leave these in for now - will remove before finalizing plugin
      //debugPrintMdListToFile(mdlist);
      //printTabSepOutput(mdlist);
      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        //log.setLevel("debug3");
        log.debug3(mdRecord.ppString(2));
        compareMetadata(mdRecord); // does nothing for the moment

      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }
 
  
  private void printTabSepOutput(List<ArticleMetadata> mdlist) {
    // check each returned md against expected values
    Iterator<ArticleMetadata> mdIt = mdlist.iterator();
    ArticleMetadata mdRecord = null;
    File file = null;
    file = new File("/Users/alexohlson/BULK/casalini/", "casinfo.csv");
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    FileOutputStream fos;
    try {
      fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        pw.print(mdRecord.get(MetadataField.FIELD_ISBN));
        pw.print("\t");
        pw.print(mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE));
        pw.print("\t");
        pw.print(mdRecord.get(MetadataField.FIELD_DATE));
        pw.print("\t");
        pw.print(mdRecord.get(MetadataField.FIELD_ARTICLE_TYPE));
        pw.print("\t");
        pw.print(StringUtil.separatedString(mdRecord.getList(MetadataField.FIELD_AUTHOR), ";"));
        pw.print("\t");
        pw.print(mdRecord.get(MetadataField.FIELD_PUBLISHER));
        pw.print("\t");
        pw.print(StringUtil.separatedString(mdRecord.getRawList(CasaliniMarcXmlSchemaHelper.MARC_publisher), ";"));
        pw.print("\t");
        pw.print(mdRecord.get(MetadataField.FIELD_ACCESS_URL));
        pw.println();
      }
      pw.flush();
      pw.close();
      fos.close();      
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private void debugPrintMdListToFile(List<ArticleMetadata> mdlist) {
    // check each returned md against expected values
    Iterator<ArticleMetadata> mdIt = mdlist.iterator();
    ArticleMetadata mdRecord = null;
    File file = null;
    file = new File("/Users/alexohlson/BULK/casalini/", "testoutmd");
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    FileOutputStream fos;
    try {
      fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        pw.print(mdRecord.ppString(2));
        pw.println();
      }
      pw.flush();
      pw.close();
      fos.close();      
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void compareMetadata(ArticleMetadata AM) {

    assertNotNull(AM);
  }
  
}