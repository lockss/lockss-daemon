/*
 * $Id: TestSqlStoredProcedures.java,v 1.3 2012-08-08 07:15:45 tlipkis Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
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

package org.lockss.util;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.TitleConfig;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.SqlStoredProcedures;

/**
 * Test class for org.lockss.daemon.MetadataManager
 *
 * @author  Philip Gust
 * @version 1.0 
 */
public class TestSqlStoredProcedures extends LockssTestCase {
  static Logger log = Logger.getLogger("TestSqlStoredProcedures");

  private SimulatedArchivalUnit sau0, sau1, sau2, sau3;
  private MockLockssDaemon theDaemon;
  private PluginManager pluginManager;
  private boolean disableMetadataManager = false;
  
  public void setUp() throws Exception {
    super.setUp();

    final String tempDirPath = getTempDir().getAbsolutePath();
    ConfigurationUtil.addFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  new File(tempDirPath, "disk").toString());

    // set derby database log 
    System.setProperty("derby.stream.error.file", 
    				   new File(tempDirPath,"derby.log").getAbsolutePath());
    
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    pluginManager = theDaemon.getPluginManager();
    pluginManager.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginManager.startService();
    // need to reset cached plugin manager between runs
    SqlStoredProcedures.setPluginManager(null);
    theDaemon.getCrawlManager();
    Configuration config = ConfigManager.getCurrentConfig().copy();

    Tdb tdb = new Tdb();

    // create Tdb for testing purposes
    Properties tdbProps;

    tdbProps = simTdbAuProps(tempDirPath, 0);
    tdbProps.setProperty("title", "Title[10.1234/12345678]");
    tdbProps.setProperty("issn", "0740-2783");
    tdbProps.setProperty("attributes.volume", "XI");
    tdbProps.setProperty("journalTitle", "Journal[10.1234/12345678]");
    tdbProps.setProperty("attributes.publisher", "Publisher[10.1234/12345678]");
    tdbProps.setProperty("plugin", SimulatedPlugin.class.getName());
    tdbProps.setProperty("attributes.year", "2001-2002");
    TdbAu tdbAu0 = tdb.addTdbAuFromProperties(tdbProps);

    tdbProps = simTdbAuProps(tempDirPath, 1);
    tdbProps.setProperty("title", "Title[10.2468/24681357]");
    tdbProps.setProperty("issn", "1144-875X");
    tdbProps.setProperty("eissn", "7744-6521");
    tdbProps.setProperty("attributes.volume", "42");
    tdbProps.setProperty("journalTitle", "Journal[10.2468/24681357]");
    tdbProps.setProperty("attributes.publisher", "Publisher[10.2468/24681357]");
    tdbProps.setProperty("plugin", SimulatedPlugin.class.getName());
    TdbAu tdbAu1 = tdb.addTdbAuFromProperties(tdbProps);
    
    tdbProps = simTdbAuProps(tempDirPath, 2);
    tdbProps.setProperty("title", "Title[Manual of Clinical Psychopharmacology]");
    tdbProps.setProperty("attributes.isbn", "978-1-58562-317-4");
    tdbProps.setProperty("journalTitle", "Manual of Clinical Psychopharmacology");
    tdbProps.setProperty("attributes.publisher", "Publisher[Manual of Clinical Psychopharmacology]");
    tdbProps.setProperty("plugin", SimulatedPlugin.class.getName());
    tdbProps.setProperty("attributes.year", "1993");
    TdbAu tdbAu2 = tdb.addTdbAuFromProperties(tdbProps);
    
    tdbProps = simTdbAuProps(tempDirPath, 3);
    tdbProps.setProperty("title", "Title[10.0135/12345678]");
    tdbProps.setProperty("attributes.isbn", "976-1-58562-317-7");
    tdbProps.setProperty("journalTitle", "Journal[10.0135/12345678]");
    tdbProps.setProperty("attributes.publisher", "Publisher[10.0135/12345678]");
    tdbProps.setProperty("plugin", SimulatedPlugin.class.getName());
    TdbAu tdbAu3 = tdb.addTdbAuFromProperties(tdbProps);

    config.setTdb(tdb);
    ConfigurationUtil.installConfig(config);
    
    Configuration cf = simAuConfig(tempDirPath, 0);
    cf.put("volume", "XI");
    sau0 = PluginTestUtil.createAndStartSimAu(SimulatedPlugin.class, cf);
    
    cf = simAuConfig(tempDirPath, 1);
    sau1 = PluginTestUtil.createAndStartSimAu(SimulatedPlugin.class, cf);

    cf = simAuConfig(tempDirPath, 2);
    sau2 = PluginTestUtil.createAndStartSimAu(SimulatedPlugin.class, cf);
    
    cf = simAuConfig(tempDirPath, 3);
    sau3 = PluginTestUtil.createAndStartSimAu(SimulatedPlugin.class, cf);
    
    PluginTestUtil.crawlSimAu(sau0);
    PluginTestUtil.crawlSimAu(sau1);
    PluginTestUtil.crawlSimAu(sau2);
    PluginTestUtil.crawlSimAu(sau3);

    theDaemon.setAusStarted(true);
    
  }

  /**
   * Create an AU configuration that is seeded with common parameters
   * for the simulated AU.
   * 
   * @param rootPath the root path
   * @param auNum a unique AU nnumber used to seed th eparameters
   * @return an AU config
   */
  Configuration simAuConfig(String rootPath, int auNum) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.title" + auNum + ".org/");
    conf.put("root", rootPath + "/" + auNum);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }
  
  /**
   * Create a Tdb properties that is seeded with common parameters
   * for the TdbAu.
   * 
   * @param rootPath the root path
   * @param auNum a unique AU nnumber used to seed th eparameters
   * @return an AU config
   */
  Properties simTdbAuProps(String rootPath, int auNum) {
    Properties tdbProps = new Properties();
    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://www.title" + auNum + ".org/");
    tdbProps.setProperty("param.2.key", "root");
    tdbProps.setProperty("param.2.value", rootPath + "/" + auNum);
    tdbProps.setProperty("param.3.key", "depth");
    tdbProps.setProperty("param.3.value", "2");
    tdbProps.setProperty("param.4.key", "branch");
    tdbProps.setProperty("param.4.value", "1");
    tdbProps.setProperty("param.5.key", "numFiles");
    tdbProps.setProperty("param.5.value", "3");
    tdbProps.setProperty("param.6.key", "fileTypes");
    tdbProps.setProperty("param.6.value", 
        "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
        SimulatedContentGenerator.FILE_TYPE_HTML));
    tdbProps.setProperty("param.7.key", "binFileSize");
    tdbProps.setProperty("param.7.value", "7");
    return tdbProps;
  }

  public void tearDown() throws Exception {
    sau0.deleteContentTree();
    sau1.deleteContentTree();
    sau2.deleteContentTree();
    sau3.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }


  /*
   * Test getting publisher from a journal or series ISSN
   */
  public void testGetPublisherFromIssn() {
    String publisherName = 
      SqlStoredProcedures.getPublisherFromIssn("7744-6521");
    assertEquals("Publisher[10.2468/24681357]",publisherName);
    
    publisherName = 
      SqlStoredProcedures.getPublisherFromIssn("0000-0000");
    assertNull(publisherName);
  }
  
  /*
   * Test getting publisher from a volume ISBN
   */
  public void testGetPublisherFromIsbn() {
    String pubName = 
      SqlStoredProcedures.getPublisherFromIsbn("976-1-58562-317-7");
    assertEquals("Publisher[10.0135/12345678]",pubName);
    
    pubName = 
      SqlStoredProcedures.getPublisherFromIsbn("000-0-00000-000-0");
    assertNull(pubName);
  }
  /**
   * Test getting the publisher from an article URL.
   */
  public void testGetPublisherFromArticleUrl() {
    SubTreeArticleIterator.Spec spec = 
      new SubTreeArticleIterator.Spec().setRoot("http://www.title3.org/");
    SubTreeArticleIterator it = new SubTreeArticleIterator(sau3, spec);
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      if (af != null) {
        String artUrl = af.getFullTextUrl();
        if (artUrl != null) {
          String publisherName = 
            SqlStoredProcedures.getPublisherFromArticleUrl(artUrl);
          assertEquals("Publisher[10.0135/12345678]", publisherName);
          return;
        }
      }
    }
    fail("No publisher name");
  }

  /**
   * Test getting the publisher from an auId.
   */
  public void testGetPublisherFromAuId() {
    String auId = sau3.getAuId();
    String pluginId = PluginManager.pluginIdFromAuId(auId);
    String auKey = PluginManager.auKeyFromAuId(auId);
    String publisherName = 
      SqlStoredProcedures.getPublisherFromAuId(pluginId, auKey);
    assertEquals("Publisher[10.0135/12345678]",publisherName);
    
    publisherName = 
      SqlStoredProcedures.getPublisherFromAuId("", "");
    assertNull(publisherName);
  }

  /*
   * Test getting title from a journal or series ISSN
   */
  public void testGetTitleFromIssn() {
    String titleName = 
      SqlStoredProcedures.getTitleFromIssn("7744-6521");
    assertEquals("Journal[10.2468/24681357]",titleName);
    
    titleName = 
      SqlStoredProcedures.getTitleFromIssn("0000-0000");
    assertNull(titleName);
  }
  
  /*
   * Test getting title from a journal or series ISSN
   */
  public void testGetVolumeTitleFromIsbn() {
    String titleName = 
      SqlStoredProcedures.getVolumeTitleFromIsbn("976-1-58562-317-7");
    assertEquals("Title[10.0135/12345678]",titleName);
    
    titleName = 
      SqlStoredProcedures.getVolumeTitleFromIsbn("000-0-00000-000-0");
    assertNull(titleName);
  }
  
  /*
   * Test getting title from a journal or series ISSN
   */
  public void testGetTitleFromIsbn() {
    String titleName = 
      SqlStoredProcedures.getTitleFromIsbn("976-1-58562-317-7");
    assertEquals("Journal[10.0135/12345678]",titleName);
    
    titleName = 
      SqlStoredProcedures.getTitleFromIsbn("000-0-00000-000-0");
    assertNull(titleName);
  }
  
  /**
   * Test getting the journal title from an article URL.
   */
  public void testGetTitleFromArticleUrl() {
    SubTreeArticleIterator.Spec spec = 
      new SubTreeArticleIterator.Spec().setRoot("http://www.title3.org/");
    SubTreeArticleIterator it = new SubTreeArticleIterator(sau3, spec);
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      if (af != null) {
        String artUrl = af.getFullTextUrl();
        if (artUrl != null) {
          String titleName = 
            SqlStoredProcedures.getTitleFromArticleUrl(artUrl);
          assertEquals("Journal[10.0135/12345678]", titleName);
          return;
        }
      }
    }
    fail("No publisher name");
  }
  
  /**
   * Test getting the volume title from an article URL.
   */
  public void testGetVolumeTitleFromArticleUrl() {
    SubTreeArticleIterator.Spec spec = 
      new SubTreeArticleIterator.Spec().setRoot("http://www.title3.org/");
    SubTreeArticleIterator it = new SubTreeArticleIterator(sau3, spec);
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      if (af != null) {
        String artUrl = af.getFullTextUrl();
        if (artUrl != null) {
          String titleName = 
            SqlStoredProcedures.getVolumeTitleFromArticleUrl(artUrl);
          assertEquals("Title[10.0135/12345678]", titleName);
          return;
        }
      }
    }
    fail("No publisher name");
  }
  
  /**
   * Test getting the journal title from an auId.
   */
  public void testGetTitleFromAuId() {
    String auId = sau3.getAuId();
    String pluginId = PluginManager.pluginIdFromAuId(auId);
    String auKey = PluginManager.auKeyFromAuId(auId);
    String publisherName = 
      SqlStoredProcedures.getTitleFromAuId(pluginId, auKey);
    assertEquals("Journal[10.0135/12345678]",publisherName);
    
    publisherName = 
      SqlStoredProcedures.getTitleFromAuId("", "");
    assertNull(publisherName);
  }


  /**
   * Test getting the volume title from an auId.
   */
  public void testGetVolumeTitleFromAuId() {
    String auId = sau3.getAuId();
    String pluginId = PluginManager.pluginIdFromAuId(auId);
    String auKey = PluginManager.auKeyFromAuId(auId);
    String volumeTitle = 
      SqlStoredProcedures.getVolumeTitleFromAuId(pluginId, auKey);
    assertEquals("Title[10.0135/12345678]",volumeTitle);
    
    volumeTitle = 
      SqlStoredProcedures.getVolumeTitleFromAuId("", "");
    assertNull(volumeTitle);
  }


  /**
   * Test getting the start, end year from an auId.
   */
  public void testGetStartEndYearFromAuId() {
    String auId = sau0.getAuId();
    String pluginId = PluginManager.pluginIdFromAuId(auId);
    String auKey = PluginManager.auKeyFromAuId(auId);
    String startYear = 
      SqlStoredProcedures.getStartYearFromAuId(pluginId, auKey);
    assertEquals("2001",startYear);
    String endYear = 
      SqlStoredProcedures.getEndYearFromAuId(pluginId, auKey);
    assertEquals("2002",endYear);
    
    startYear = 
      SqlStoredProcedures.getStartYearFromAuId("", "");
    assertNull(startYear);
    endYear = 
      SqlStoredProcedures.getEndYearFromAuId("", "");
    assertNull(startYear);
  }

  /**
   * Test getting the start, end volume from an auId.
   */
  public void testGetStartEndVolumeFromAuId() {
    String auId = sau0.getAuId();
    String pluginId = PluginManager.pluginIdFromAuId(auId);
    String auKey = PluginManager.auKeyFromAuId(auId);
    String startYear = 
      SqlStoredProcedures.getStartVolumeFromAuId(pluginId, auKey);
    assertEquals("XI",startYear);
    String endYear = 
      SqlStoredProcedures.getEndVolumeFromAuId(pluginId, auKey);
    assertEquals("XI",endYear);
    
    startYear = 
      SqlStoredProcedures.getStartYearFromAuId("", "");
    assertNull(startYear);
    endYear = 
      SqlStoredProcedures.getEndYearFromAuId("", "");
    assertNull(startYear);
  }

  /**
   * Test getting the ingest date from an article URL
   */
  public void _testGetIngestDateFromArticleUrl() {
    SubTreeArticleIterator.Spec spec = 
      new SubTreeArticleIterator.Spec().setRoot("http://www.title3.org/");
    SubTreeArticleIterator it = new SubTreeArticleIterator(sau3, spec);
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      if (af != null) {
        String artUrl = af.getFullTextUrl();
        if (artUrl != null) {
          String ingestYear = 
            SqlStoredProcedures.getIngestYearFromArticleUrl(artUrl);
          if (ingestYear != null) {
            assertEquals(new Date().getYear()+"", ingestYear);
            return;
          }
        }
      }
    }
    fail("No ingest year");
    
  }

  /**
   * Test getting the au date from an article URL
   */
  public void _testGetIngestYearFromArticleUrl() {
    SubTreeArticleIterator.Spec spec = 
      new SubTreeArticleIterator.Spec().setRoot("http://www.title3.org/");
    SubTreeArticleIterator it = new SubTreeArticleIterator(sau3, spec);
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      if (af != null) {
        String artUrl = af.getFullTextUrl();
        if (artUrl != null) {
          String auYear = 
            SqlStoredProcedures.getIngestYearFromArticleUrl(artUrl);
          if (auYear != null) {
            assertEquals(new Date().getYear()+"", auYear);
            return;
          }
        }
      }
    }
    fail("No au year");
    
  }
}
