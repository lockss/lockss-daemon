/*
 * $Id: TestMetadataManager.java,v 1.2.2.4 2011-04-04 21:21:59 pgust Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import javax.sql.DataSource;

import org.lockss.config.*;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.daemon.MetadataManager
 *
 * @author  Philip Gust
 * @version 1.0
 */
public class TestMetadataManager extends LockssTestCase {
  static Logger log = Logger.getLogger("TestMetadataManager");

  private SimulatedArchivalUnit sau0, sau1, sau2, sau3;
  private MockLockssDaemon theDaemon;
  private MetadataManager metadataManager;
  private PluginManager pluginManager;
  private String tempDirPath;

  /** set of AUs reindexed by the MetadataManager */
  Set<String> ausReindexed = new HashSet<String>();
  
  public void setUp() throws Exception {
    super.setUp();

    tempDirPath = getTempDir().getAbsolutePath();

    // set derby database log 
    System.setProperty("derby.stream.error.file",
                       new File(tempDirPath,"derby.log").getAbsolutePath());

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    pluginManager = theDaemon.getPluginManager();
    pluginManager.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginManager.startService();
    theDaemon.getCrawlManager();

    Properties props = new Properties();
    props.setProperty(MetadataManager.PARAM_INDEXING_ENABLED, "true");
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    Configuration config = ConfigurationUtil.fromProps(props);
    ConfigurationUtil.installConfig(config);

    sau0 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin0.class,
                                              simAuConfig(tempDirPath + "/0"));
    sau1 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin1.class,
                                              simAuConfig(tempDirPath + "/1"));
    sau2 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin2.class,
                                              simAuConfig(tempDirPath + "/2"));
    sau3 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin3.class,
                                          simAuConfig(tempDirPath + "/3"));
    PluginTestUtil.crawlSimAu(sau0);
    PluginTestUtil.crawlSimAu(sau1);
    PluginTestUtil.crawlSimAu(sau2);
    PluginTestUtil.crawlSimAu(sau3);
    
    // reset set of reindexed aus
    ausReindexed.clear();

    metadataManager = new MetadataManager() {
      /**
       * Get the db root directory for testing.
       * @return the db root directory
       */
      protected String getDbRootDirectory() {
        return tempDirPath;
      }
      
      /**
       * Notify listeners that an AU is being reindexed.
       * 
       * @param au
       */
      protected void notifyStartReindexingAu(ArchivalUnit au) {
        log.debug("Start reindexing au " + au);
      }
      
      /**
       * Notify listeners that an AU is finshed being reindexed.
       * 
       * @param au
       */
      protected void notifyFinishReindexingAu(ArchivalUnit au, boolean success) {
        log.debug("Finished reindexing au (" + success + ") " + au);
        synchronized (ausReindexed) {
          ausReindexed.add(au.getAuId());
          ausReindexed.notifyAll();
        }
      }
    };
    theDaemon.setMetadataManager(metadataManager);
    metadataManager.initService(theDaemon);
    metadataManager.startService();

    theDaemon.setAusStarted(true);
    
    int expectedAuCount = 4;
    assertEquals(expectedAuCount, pluginManager.getAllAus().size());
    
    long maxWaitTime = expectedAuCount * 10000; // 10 sec. per au
    int ausCount = waitForReindexing(expectedAuCount, maxWaitTime);
    assertEquals(expectedAuCount, ausCount);
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
                                SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }


  public void tearDown() throws Exception {
    sau0.deleteContentTree();
    sau1.deleteContentTree();
    sau2.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Waits a specified period for a specified number of AUs to finish 
   * being reindexed.  Returns the actual number of AUs reindexed.
   * 
   * @param auCount the expected AU count
   * @param maxWaitTime the maximum time to wait
   * @return the number of AUs reindexed
   */
  private int waitForReindexing(int auCount, long maxWaitTime) {
    long startTime = System.currentTimeMillis();
    synchronized (ausReindexed) {
      while (   (System.currentTimeMillis()-startTime < maxWaitTime) 
             && (ausReindexed.size() < auCount)) {
        try {
          ausReindexed.wait(maxWaitTime);
        } catch (InterruptedException ex) {
        }
      }
    }
    return ausReindexed.size();
  }

  public void testCreateMetadata() throws Exception {
    DataSource ds = metadataManager.getDataSource();
    assertNotNull(ds);
    
    Connection con = ds.getConnection();
    
    assertEquals(0, metadataManager.reindexingTasks.size());
    assertEquals(0, metadataManager.getAusToReindex(con, Integer.MAX_VALUE).size());

    // check distinct access URLs
    String query =           
      "select distinct " + MetadataManager.ACCESS_URL_FIELD 
      + " from " + MetadataManager.METADATA_TABLE; 
    Statement stmt = con.createStatement();
    ResultSet resultSet = stmt.executeQuery(query);
    int count = 0;
    while (resultSet.next()) {
        count++;
    }
    final int metadataRowCount = 84;
    assertEquals(metadataRowCount, count);

    // check unique plugin IDs
    query =           
        "select distinct " + MetadataManager.PLUGIN_ID_FIELD 
        + " from " + MetadataManager.METADATA_TABLE; 
    stmt = con.createStatement();
    resultSet = stmt.executeQuery(query);
    Set<String> results = new HashSet<String>();
    while (resultSet.next()) {
      results.add(resultSet.getString(1));
    }
    assertEquals(4, results.size());
    results.remove("org|lockss|daemon|TestMetadataManager$MySimulatedPlugin0");
    results.remove("org|lockss|daemon|TestMetadataManager$MySimulatedPlugin1");
    results.remove("org|lockss|daemon|TestMetadataManager$MySimulatedPlugin2");
    results.remove("org|lockss|daemon|TestMetadataManager$MySimulatedPlugin3");
    assertEquals(0, results.size());
    
    // check DOIs
    query =           
        "select distinct " + MetadataManager.DOI_FIELD 
        + " from " + MetadataManager.DOI_TABLE; 
    stmt = con.createStatement();
    resultSet = stmt.executeQuery(query);
    results = new HashSet<String>();
    while (resultSet.next()) {
        results.add(resultSet.getString(1));
    }
    assertEquals(metadataRowCount, results.size());

    // check ISSNs
    query =           
        "select distinct " + MetadataManager.ISSN_FIELD 
        + " from " + MetadataManager.ISSN_TABLE; 
    stmt = con.createStatement();
    resultSet = stmt.executeQuery(query);
    results = new HashSet<String>();
    while (resultSet.next()) {
        results.add(resultSet.getString(1));
    }
    assertEquals(3, results.size());
    results.remove("77446521");
    results.remove("1144875X");
    results.remove("07402783");
    assertEquals(0, results.size());
    
    // check ISBNs
    query =           
        "select distinct " + MetadataManager.ISBN_FIELD 
        + " from " + MetadataManager.ISBN_TABLE; 
    stmt = con.createStatement();
    resultSet = stmt.executeQuery(query);
    results = new HashSet<String>();
    while (resultSet.next()) {
        results.add(resultSet.getString(1));
      log.critical(resultSet.getString(1));
    }
    assertEquals(2, results.size());
    results.remove("9781585623174");
    results.remove("9761585623177");
    assertEquals(0, results.size());
    
    assertEquals(0, metadataManager.reindexingTasks.size());
    assertEquals(0, metadataManager.getAusToReindex(con, Integer.MAX_VALUE).size());

    con.rollback();
    con.commit();
  }
  
  public void testModifyMetadata() throws Exception {
    DataSource ds = metadataManager.getDataSource();
    assertNotNull(ds);
    Connection con = ds.getConnection();
    
    // check unique plugin IDs
    String query =           
        "select distinct " + MetadataManager.ACCESS_URL_FIELD 
        + " from " + MetadataManager.METADATA_TABLE
        + " where " + MetadataManager.PLUGIN_ID_FIELD 
        + " =  'org|lockss|daemon|TestMetadataManager$MySimulatedPlugin0'"; 
    Statement stmt = con.createStatement();
    ResultSet resultSet = stmt.executeQuery(query);
    Set<String> results = new HashSet<String>();
    while (resultSet.next()) {
      results.add(resultSet.getString(1));
    }
    final int count = results.size();
    assertEquals(21, count);

        // reset set of reindexed aus
    ausReindexed.clear();

    // simulate an au change
    theDaemon.getPluginManager().applyAuEvent(new PluginManager.AuEventClosure() {
      public void execute(AuEventHandler hand) {
        AuEventHandler.ChangeInfo chInfo = new AuEventHandler.ChangeInfo();
        chInfo.setAu(sau0);        // reindex simulated AU
        chInfo.setComplete(true);   // crawl was complete
        hand.auContentChanged(sau0, chInfo);
      }
    });
    
    // ensure only expected number of AUs were reindexed
    final int expectedAuCount = 1;
    int maxWaitTime = 10000; // 10 sec. per au
    int ausCount = waitForReindexing(expectedAuCount, maxWaitTime);
    assertEquals(ausCount, expectedAuCount);

    // ensure AU contains as many metadata table entries as before
    resultSet = stmt.executeQuery(query);
    results = new HashSet<String>();
    while (resultSet.next()) {
      results.add(resultSet.getString(1));
    }
    assertEquals(count, results.size());
  }
  
  
  public static class MySubTreeArticleIteratorFactory
      implements ArticleIteratorFactory {
    String pat;
    public MySubTreeArticleIteratorFactory(String pat) {
      this.pat = pat;
    }
    
    /**
     * Create an Iterator that iterates through the AU's articles, pointing
     * to the appropriate CachedUrl of type mimeType for each, or to the
     * plugin's choice of CachedUrl if mimeType is null
     * @param au the ArchivalUnit to iterate through
     * @return the ArticleIterator
     */
    @Override
    public Iterator<ArticleFiles> createArticleIterator(
        ArchivalUnit au, MetadataTarget target) throws PluginException {
      Iterator<ArticleFiles> ret;
      SubTreeArticleIterator.Spec spec = 
        new SubTreeArticleIterator.Spec().setTarget(target);
      
      if (pat != null) {
       spec.setPattern(pat);
      }
      
      ret = new SubTreeArticleIterator(au, spec);
      log.debug(  "creating article iterator for au " + au.getName() 
                    + " hasNext: " + ret.hasNext());
      return ret;
    }
  }

  public static class MySimulatedPlugin extends SimulatedPlugin {
    ArticleMetadataExtractor simulatedArticleMetadataExtractor = null;
    
    /**
     * Returns the article iterator factory for the mime type, if any
     * @param contentType the content type
     * @return the ArticleIteratorFactory
     */
    @Override
    public ArticleIteratorFactory getArticleIteratorFactory() {
      MySubTreeArticleIteratorFactory ret =
          new MySubTreeArticleIteratorFactory(null); //"branch1/branch1");
      return ret;
    }
    @Override
    public ArticleMetadataExtractor 
      getArticleMetadataExtractor(MetadataTarget target, ArchivalUnit au) {
      return simulatedArticleMetadataExtractor;
    }
  }

  public static class MySimulatedPlugin0 extends MySimulatedPlugin {
    public MySimulatedPlugin0() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        int articleNumber = 0;
        public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
          throws IOException, PluginException {
          ArticleMetadata md = new ArticleMetadata();
          articleNumber++;
          md.put(MetadataField.FIELD_ISSN,"0740-2783");
          md.put(MetadataField.FIELD_VOLUME,"XI");
          if (articleNumber < 10) {
            md.put(MetadataField.FIELD_ISSUE,"1st Quarter");
            md.put(MetadataField.FIELD_DATE,"2010-Q1");
            md.put(MetadataField.FIELD_START_PAGE,"" + articleNumber);
          } else {
                    md.put(MetadataField.FIELD_ISSUE,"2nd Quarter");
            md.put(MetadataField.FIELD_DATE,"2010-Q2");
            md.put(MetadataField.FIELD_START_PAGE,"" + (articleNumber-9));
          }
          String doiPrefix = "10.1234/12345678";
          String doi = doiPrefix + "."
                        + md.get(MetadataField.FIELD_DATE) + "."
                        + md.get(MetadataField.FIELD_START_PAGE); 
          md.put(MetadataField.FIELD_DOI, doi);
          md.put(MetadataField.FIELD_JOURNAL_TITLE,"Journal[" + doiPrefix + "]");
          md.put(MetadataField.FIELD_ARTICLE_TITLE,"Title[" + doi + "]");
          md.put(MetadataField.FIELD_AUTHOR,"Author[" + doi + "]");
          md.put(MetadataField.FIELD_ACCESS_URL, 
                 "http://www.title0.org/plugin0/XI/"
             +  md.get(MetadataField.FIELD_DATE) 
             +"/p" + md.get(MetadataField.FIELD_START_PAGE));
          emitter.emitMetadata(af, md);
        }
      };
    }
    public ExternalizableMap getDefinitionMap() {
      ExternalizableMap map = new ExternalizableMap();
      map.putString("au_start_url", "\"%splugin0/%s\", base_url, volume");
      return map;
    }
  }
          
  public static class MySimulatedPlugin1 extends MySimulatedPlugin {
    public MySimulatedPlugin1() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        int articleNumber = 0;
        public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
          throws IOException, PluginException {
          articleNumber++;
          ArticleMetadata md = new ArticleMetadata();
          md.put(MetadataField.FIELD_ISSN,"1144-875X");
          md.put(MetadataField.FIELD_EISSN, "7744-6521");
          md.put(MetadataField.FIELD_VOLUME,"42");
          if (articleNumber < 10) {
            md.put(MetadataField.FIELD_ISSUE,"Summer");
            md.put(MetadataField.FIELD_DATE,"2010-S2");
            md.put(MetadataField.FIELD_START_PAGE,"" + articleNumber);
          } else {
            md.put(MetadataField.FIELD_ISSUE,"Fall");
            md.put(MetadataField.FIELD_DATE,"2010-S3");
            md.put(MetadataField.FIELD_START_PAGE, "" + (articleNumber-9));
          }
          String doiPrefix = "10.2468/28681357";
          String doi = doiPrefix + "."
                        + md.get(MetadataField.FIELD_DATE) + "."
                        + md.get(MetadataField.FIELD_START_PAGE); 
          md.put(MetadataField.FIELD_DOI, doi);
          md.put(MetadataField.FIELD_JOURNAL_TITLE, "Journal[" + doiPrefix + "]");
          md.put(MetadataField.FIELD_ARTICLE_TITLE, "Title[" + doi + "]");
          md.put(MetadataField.FIELD_AUTHOR, "Author1[" + doi + "]");
          md.put(MetadataField.FIELD_ACCESS_URL, 
              "http://www.title1.org/plugin1/v_42/"
                +  md.get(MetadataField.FIELD_DATE) 
                +"/p" + md.get(MetadataField.FIELD_START_PAGE));
          emitter.emitMetadata(af, md);
        }
      };
    }
    public ExternalizableMap getDefinitionMap() {
      ExternalizableMap map = new ExternalizableMap();
      map.putString("au_start_url", "\"%splugin1/v_42\", base_url");
      return map;
    }
  }
  
  public static class MySimulatedPlugin2 extends MySimulatedPlugin {
    public MySimulatedPlugin2() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        int articleNumber = 0;
        public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
          throws IOException, PluginException {
          org.lockss.extractor.ArticleMetadata md = new ArticleMetadata();
          articleNumber++;
          String doi = "10.1357/9781585623174." + articleNumber; 
          md.put(MetadataField.FIELD_DOI,doi);
          md.put(MetadataField.FIELD_ISBN,"978-1-58562-317-4");
          md.put(MetadataField.FIELD_DATE,"1993");
          md.put(MetadataField.FIELD_START_PAGE,"" + articleNumber);
          md.put(MetadataField.FIELD_JOURNAL_TITLE,"Manual of Clinical Psychopharmacology");
          md.put(MetadataField.FIELD_ARTICLE_TITLE,"Title[" + doi + "]");
          md.put(MetadataField.FIELD_AUTHOR,"Author1[" + doi + "]");
          md.put(MetadataField.FIELD_AUTHOR,"Author2[" + doi + "]");
          md.put(MetadataField.FIELD_AUTHOR,"Author3[" + doi + "]");
          md.put(MetadataField.FIELD_ACCESS_URL, 
             "http://www.title2.org/plugin2/1993/p"+articleNumber);
          emitter.emitMetadata(af, md);
        }
      };
    }
    public ExternalizableMap getDefinitionMap() {
      ExternalizableMap map = new ExternalizableMap();
      map.putString("au_start_url", "\"%splugin2/1993\", base_url");
      return map;
    }
  }
  
  public static class MySimulatedPlugin3 extends MySimulatedPlugin {
    public MySimulatedPlugin3() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        int articleNumber = 0;
        public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
          throws IOException, PluginException {
          ArticleMetadata md = new ArticleMetadata();
          articleNumber++;
          String doiPrefix = "10.0135/12345678.1999-11.12";
          String doi = doiPrefix + "." + articleNumber; 
          md.put(MetadataField.FIELD_DOI,doi);
          md.put(MetadataField.FIELD_ISBN,"976-1-58562-317-7");
          md.put(MetadataField.FIELD_DATE,"1999");
          md.put(MetadataField.FIELD_START_PAGE,"" + articleNumber);
          md.put(MetadataField.FIELD_JOURNAL_TITLE,"Journal[" + doiPrefix + "]");
          md.put(MetadataField.FIELD_ARTICLE_TITLE,"Title[" + doi + "]");
          md.put(MetadataField.FIELD_AUTHOR,"Author1[" + doi + "]");
          md.put(MetadataField.FIELD_ACCESS_URL, 
                  "http://www.title3.org/plugin3/1999/p"+articleNumber);
          emitter.emitMetadata(af, md);
        }
      };
    }
    public ExternalizableMap getDefinitionMap() {
      ExternalizableMap map = new ExternalizableMap();
      map.putString("au_start_url", "\"%splugin3/1999\", base_url");
      return map;
    }
  }
          
}
