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

package org.lockss.plugin.elsevier;

import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinablePlugin;


public class TestElsevierDTD5XmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestElsevierDTD5XmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.elsevier.ClockssElsevierDTD5SourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  private static String TAR_A_BASE = BASE_URL + "CLKS003A.tar";
  private static String TAR_B_BASE = BASE_URL + "CLKS003B.tar";
  private static String SUBDIR = "!/CLKS003/"; 


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
    TypedEntryMap mau_props = mau.getProperties();                                                                                                     
    mau_props.putString("year", "2014");                                                                                        
    mau_props.putString("base_url", BASE_URL);                                                                                        
    mau.setPropertyMap(mau_props);


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
    conf.put("year", "2014");
    return conf;
  }


  private static final String realXMLFile = "ElsevierDTD5.xml";
  //private static final String realXMLFile = "ElsevierDTD5_main.xml";

  public void testFromDTD5XMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();   
      // mock up coming from a zip arhive
      String xml_url = TAR_A_BASE + SUBDIR + "dataset.xml";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new ElsevierDTD5XmlSourceMetadataExtractorFactory().createFileMetadataExtractor(
          MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      /*
       * For the moment, this does nothing because we can't confirmt he existence of the matching pdf file
       */
      /*
      assertNotEmpty(mdlist);

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        log.info("Next record:");
        log.info(mdRecord.toString());
      }
      */
    }finally {
      IOUtil.safeClose(file_input);
    }
  }

  private static final String realmainXMLFile = "ElsevierDTD5_main.xml";
/*
  public void testMiniXmlSchemaFromFile() throws Exception {
    InputStream file_input = null;
    log.setLevel("debug3");
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();   
      // mock up coming from a zip arhive
      String xml_url = TAR_A_BASE + SUBDIR + "foo/blah/main.xml";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      log.debug3("FOO");
      MINI_ARTICLE_SCHEMA_HELPER myMiniSchema = new MINI_ARTICLE_SCHEMA_HELPER();
      // Use the mini-schema defined in this class to extract limited information
      List<ArticleMetadata> amList = 
          new XPathXmlMetadataParser(null, 
              myMiniSchema.top_node_xpath, 
              myMiniSchema.articleLevelMDMap,
              false).extractMetadata(MetadataTarget.Any(), mcu);
      // We should have one and only one AM from this file
      if (amList.size() > 0) {
        log.debug3("found article level metadata...");
        ArticleMetadata oneAM = amList.get(0);
        log.info("Next record:");
        log.info(oneAM.toString());
      } else {
        log.debug3("no md extracted from test main.xml file");
      }
    } catch (XPathExpressionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  */


  private static final String realTARFile_A = "CLKS003A.tar";
  private static final String realTARFile_B = "CLKS003B.tar";

/*  
  public void testFromTarFiles() throws Exception {
    InputStream file_inputA = null;
    InputStream file_inputB = null;
    try {
//      file_inputA = getResourceAsStream(realTARFile_A);
//      file_inputB = getResourceAsStream(realTARFile_B);
      CIProperties tar_props = new CIProperties();   
      tar_props.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/x-tar");
      
      MockCachedUrl mcuA = new MockCachedUrl(TAR_A_BASE, realTARFile_A, true, mau);
      MockCachedUrl mcuB = new MockCachedUrl(TAR_B_BASE, realTARFile_B, true, mau);
      mcuA.setProperties(tar_props);
      mcuB.setProperties(tar_props);
      mcuA.setExists(true);
      mcuB.setExists(true);
      
      mau.setArticleIterator(new ElsevierDTD5XmlSourceArticleIteratorFactory().createArticleIterator(mau, MetadataTarget.Any()));
      Iterator<ArticleFiles> it = mau.getArticleIterator(MetadataTarget.Any());
      log.info("foo");
      while (it.hasNext()) {
        ArticleFiles af = it.next();
        log.info("iterator returns " + af.toString());
        CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
        if (cu != null) {
          log.info("metadata role is " + cu.toString());
        }
      }
      log.info("foo3");
      
      FileMetadataExtractor me = new ElsevierDTD5XmlSourceMetadataExtractorFactory().createFileMetadataExtractor(
        MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcuA);
      assertNotEmpty(mdlist);

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        log.info("Next record:");
        log.info(mdRecord.toString());
      }
    }finally {
      IOUtil.safeClose(file_inputA);
      IOUtil.safeClose(file_inputB);
    }

  }
*/  

}
