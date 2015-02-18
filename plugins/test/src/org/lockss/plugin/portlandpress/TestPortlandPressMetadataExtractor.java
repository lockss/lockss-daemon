/*
 * $Id$
 */

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.portlandpress;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;


public class TestPortlandPressMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestPortlandPressMetadataExtractor");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau;
  private static String PLUGIN_NAME = "org.lockss.plugin.portlandpress.PortlandPressPlugin";
  private static String BASE_URL = "http://www.basejournal.org/";
  private static String SIM_ROOT = BASE_URL + "bj/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,	simAuConfig(tempDirPath));
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, auConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("depth", "1");
    conf.put("branch", "2");
    conf.put("numFiles", "2");
    conf.put("fileTypes",""	+ (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "text/html");
    return conf;
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "bj");
    conf.put("volume_name","399");
    return conf;
  }

  // the metadata that should be extracted
  String goodDate = "2006-10-01";
  ArrayList<String> goodAuthors = new ArrayList<String>();
  ArrayList<String> goodKeywords = new ArrayList<String>();
  String goodTitle = "This is the Title of the Article";
  String goodPublisher = "Portland Press Ltd.";

  // a chunk of html similar to that from the publisher's site  
  String goodContent = 
"<html> " +
"<head> " +
"<meta name=\"Keywords\" content=\"cell biology\" /> " +
"<META NAME=\"DC.Publisher\" CONTENT=\"Portland Press Ltd.\"> " +
"<META NAME=\"DC.Date\" CONTENT=\"2006-10-01\"> " +
"<META NAME=\"DC.Identifier\" CONTENT=\"http://www.basejournal.org/bj/399/bj3990029.htm\"> " +
"<META NAME=\"DC.Language\" CONTENT=\"en\"> " +
"<META NAME=\"DC.Rights\" CONTENT=\"http://www.biochemj.org/bj/licence.htm\"> " +
"<META NAME=\"PPL.Volume\" CONTENT=\"399\"> " +
"<META NAME=\"PPL.FirstPage\" CONTENT=\"29\"> " +
"<META NAME=\"PPL.DOI\" CONTENT=\"10.1042/BJ20060579\"> " +
"<META NAME=\"PPL.LastPage\" CONTENT=\"35\"> " +
"<META NAME=\"PPL.DocType\" CONTENT=\"Research article\"> " +
"<META NAME=\"DC.Title\" CONTENT=\"This is the Title of the Article\"> " +
"<META NAME=\"DC.Creator\" CONTENT=\"Author A\"> " +
"<META NAME=\"DC.Creator\" CONTENT=\"Author2 B\"> " +
"<META NAME=\"DC.Keyword\" CONTENT=\"collagen\"> " +
"<!--- PARAMETERS --->";	
   

  public void testExtractFromGoodContent() throws Exception {
    goodAuthors.add("Author A");
    goodAuthors.add("Author2 B");
    goodKeywords.add("collagen");
    goodKeywords.add("cell biology");
    
    String url = "http://www.example.com/bj/vol/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new PortlandPressHtmlMetadataExtractorFactory.PortlandPressHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodTitle, md.get(MetadataField.DC_FIELD_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodAuthors, md.getList(MetadataField.DC_FIELD_CREATOR));
    assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
  }


  /**
   * Inner class that where a number of Archival Units can be created
   *
   */
  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
	throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class to create a html source code simulated content
   *
   */
  public static class MySimulatedContentGenerator extends	SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

  }
}
