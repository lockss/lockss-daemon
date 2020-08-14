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

package org.lockss.plugin.clockss.warc;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;

import com.google.common.io.Files;

import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.extractor.FileMetadataExtractor.Emitter;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrl;
import org.lockss.plugin.clockss.warc.WarcXmlMetadataExtractorFactory.WarcJatsPublishingSourceXmlMetadataExtractor;


public class TestWarcXmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestWarcXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.warc.ClockssWarcPlugin";
  private static String BASE_URL = "http://www.warc.org/";
  private static String YEAR = "2015";


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
  

  private static final String realXMLFile = "WarcJatsTest.xml";

  public void testFromWarcJatsXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();   
      // mock up coming from a zip arhive
      String xml_url = BASE_URL + YEAR + "TestXML.xml";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

    FileMetadataExtractor me = new WarcXmlMetadataExtractorFactory().createFileMetadataExtractor(
        MetadataTarget.Any(), "text/xml");
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
        //log.setLevel("debug3");
        log.debug3(mdRecord.ppString(2));
        compareMetadata(mdRecord);

      }
    }finally {
      IOUtil.safeClose(file_input);
    }
  }
    
    private static final String realOnixXMLFile = "WarcOnixTest.xml";

    public void testFromWarcOnixXMLFile() throws Exception {
      InputStream file_input = null;
      try {
        file_input = getResourceAsStream(realOnixXMLFile);
        String string_input = StringUtil.fromInputStream(file_input);
        IOUtil.safeClose(file_input);

        CIProperties xmlHeader = new CIProperties();   
        // mock up coming from a zip arhive
        String xml_url = BASE_URL + YEAR + "TestXML.xml";
        xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
        MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
        mcu.setContent(string_input);
        mcu.setContentSize(string_input.length());
        mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new WarcXmlMetadataExtractorFactory().createFileMetadataExtractor(
          MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
            new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(4, mdlist.size());

        // check each returned md against expected values
        //debugPrintMdListToFile(mdlist);
        Iterator<ArticleMetadata> mdIt = mdlist.iterator();
        ArticleMetadata mdRecord = null;
        while (mdIt.hasNext()) {
          mdRecord = (ArticleMetadata) mdIt.next();
          //log.setLevel("debug3");
          log.debug3(mdRecord.ppString(2));
          compareMetadataBooks(mdRecord);
        }
      }finally {
        IOUtil.safeClose(file_input);
      }
    
  }
 
    /*
  private static final String testXMLFile = "enternamehere.xml";

    public void testNewWarcJatsXMLFile() throws Exception {
      InputStream file_input = null;
      try {
        file_input = getResourceAsStream(testXMLFile);
        String string_input = StringUtil.fromInputStream(file_input);
        IOUtil.safeClose(file_input);

        CIProperties xmlHeader = new CIProperties();   
        // mock up coming from a zip arhive
        String xml_url = BASE_URL + YEAR + "TestXML.xml";
        xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
        MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
        mcu.setContent(string_input);
        mcu.setContentSize(string_input.length());
        mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new WarcXmlMetadataExtractorFactory().createFileMetadataExtractor(
          MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
            new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        debugPrintMdListToFile(mdlist);
      }finally {
        IOUtil.safeClose(file_input);
      }
    }
    */
  
  private void compareMetadata(ArticleMetadata AM) {

    assertNotNull(AM);
    assertEquals(AM.get(MetadataField.FIELD_ARTICLE_TITLE),"Article about Foo");
    assertEquals(AM.get(MetadataField.FIELD_PUBLICATION_TITLE),"Journal of Foo");
    assertEquals(AM.get(MetadataField.FIELD_DOI),"10.9999/12-34-56");
    assertEquals(AM.get(MetadataField.FIELD_VOLUME),"12");
    assertEquals(AM.get(MetadataField.FIELD_ISSUE),"34");
  }
  
  
  private static final String isbn1 = "9780111111010"; 
  private static final String isbn2 = "9780222222010"; 
  private static final String isbn3 = "9780333333010"; 
  private static final String isbn4 = "9780444444010"; 

  private static final String date1 = "1974-01-01";
  private static final String date2 ="1975";
  private static final String date3 ="1976-01-01";
  private static final String date4 ="1977-01-01";

  private static final String title1 = "Book Title One";
  private static final String title2 = "Book The Second";
  private static final String title3 = "Third Book: A Title";
  private static final String title4 = "And tHis IS the FINAL TITLE";

  private void compareMetadataBooks(ArticleMetadata AM) {

    assertNotNull(AM);
    String bookisbn = AM.get(MetadataField.FIELD_ISBN);
    log.debug3(bookisbn);
    assertEquals(AM.get(MetadataField.FIELD_PUBLISHER),"Mineralogical Society of America");
    if ( isbn1.equals(bookisbn) ) {
      assertEquals(AM.get(MetadataField.FIELD_PUBLICATION_TITLE),title1);
      assertEquals(AM.get(MetadataField.FIELD_DATE), date1);
    } else if (isbn2.equals(bookisbn) ) {
      assertEquals(AM.get(MetadataField.FIELD_PUBLICATION_TITLE),title2);
      assertEquals(AM.get(MetadataField.FIELD_DATE), date2);      
    } else if (isbn3.equals(bookisbn) ) {
      assertEquals(AM.get(MetadataField.FIELD_PUBLICATION_TITLE),title3);
      assertEquals(AM.get(MetadataField.FIELD_DATE), date3);      
    } else if (isbn4.equals(bookisbn) ) {
      assertEquals(AM.get(MetadataField.FIELD_PUBLICATION_TITLE),title4);
      assertEquals(AM.get(MetadataField.FIELD_DATE), date4);      
    } else {
      // we didn't recognize the isbn
      assertEquals(false,true);
    }
  }

  
  /* 
   * Useful method for initial testing of a new warc. Saves the MD output to a file
   */
  private void debugPrintMdListToFile(List<ArticleMetadata> mdlist) {
    // check each returned md against expected values
    Iterator<ArticleMetadata> mdIt = mdlist.iterator();
    ArticleMetadata mdRecord = null;
    File file = null;
    file = new File("/tmp", "warc_md.txt");
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
  
  public static void main(String[] args) throws Exception {
    final PrintStream out = System.out;
    final PrintStream err = System.err;
    if (args.length != 1) {
      err.format("Expected one argument but got %d%n", args.length);
      System.exit(1);
    }
    String jatsFileStr = args[0];
    if (!new File(jatsFileStr).exists()) {
      err.format("File not found: %s%n", jatsFileStr);
      System.exit(1);
    }
    MockCachedUrl cu = new MockCachedUrl("file://" + jatsFileStr, jatsFileStr, false);
    WarcJatsPublishingSourceXmlMetadataExtractor me = new WarcJatsPublishingSourceXmlMetadataExtractor();
    me.extract(MetadataTarget.Any(),
               cu,
               new Emitter() {
                 @Override
                 public void emitMetadata(CachedUrl cu,
                                          ArticleMetadata metadata) {
                   out.format("%s%n", metadata.ppString(0));
                 }      
               });
  }
  
}
