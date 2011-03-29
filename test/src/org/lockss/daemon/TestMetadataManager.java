/*
 * $Id: TestMetadataManager.java,v 1.3 2011-03-29 17:43:44 pgust Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.crawler.NewContentCrawler;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.AuEventHandler;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.SubTreeArticleIterator;
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

  private SimulatedArchivalUnit sau0, sau1, sau2;
  private MockLockssDaemon theDaemon;
  private MetadataManager metadataManager;
  private static final int DEFAULT_MAX_DEPTH = 1000;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;

  public void setUp() throws Exception {
    super.setUp();
    this.setUp(DEFAULT_MAX_DEPTH);
  }

  /** set of AUs reindexed by the MetadataManager */
  Set<String> ausReindexed = new HashSet<String>();
  
  public void setUp(int max) throws Exception {

    final String tempDirPath = getTempDir().getAbsolutePath();
    String auId0 = "org|lockss|daemon|TestMetadataManager$MySimulatedPlugin0.root~" +
      PropKeyEncoder.encode(tempDirPath);
    String auId1 = "org|lockss|daemon|TestMetadataManager$MySimulatedPlugin1.root~" +
      PropKeyEncoder.encode(tempDirPath);
    String auId2 = "org|lockss|daemon|TestMetadataManager$MySimulatedPlugin2.root~" +
      PropKeyEncoder.encode(tempDirPath);
    
    Properties props = new Properties();
    props.setProperty(MetadataManager.PARAM_INDEXING_ENABLED, "true");
    
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);

    props.setProperty("org.lockss.au." + auId0 + "." +
                      SimulatedPlugin.AU_PARAM_ROOT, tempDirPath);
    // the simulated Content's depth will be (AU_PARAM_DEPTH + 1)
    props.setProperty("org.lockss.au." + auId0 + "." +
                      SimulatedPlugin.AU_PARAM_DEPTH, "3");
    props.setProperty("org.lockss.au." + auId0 + "." +
                      SimulatedPlugin.AU_PARAM_BRANCH, "3");
    props.setProperty("org.lockss.au." + auId0 + "." +
                      SimulatedPlugin.AU_PARAM_NUM_FILES, "7");
    props.setProperty("org.lockss.au." + auId0 + "." +
                      SimulatedPlugin.AU_PARAM_FILE_TYPES, "" +
                      (SimulatedContentGenerator.FILE_TYPE_PDF +
                       SimulatedContentGenerator.FILE_TYPE_HTML));
    props.setProperty("org.lockss.au." + auId0 + "." +
                      SimulatedPlugin.AU_PARAM_BIN_FILE_SIZE, ""+fileSize);

    props.setProperty("org.lockss.au." + auId1 + "." +
                      SimulatedPlugin.AU_PARAM_ROOT, tempDirPath);
    // the simulated Content's depth will be (AU_PARAM_DEPTH + 1)
    props.setProperty("org.lockss.au." + auId1 + "." +
                      SimulatedPlugin.AU_PARAM_DEPTH, "3");
    props.setProperty("org.lockss.au." + auId1 + "." +
                      SimulatedPlugin.AU_PARAM_BRANCH, "3");
    props.setProperty("org.lockss.au." + auId1 + "." +
                      SimulatedPlugin.AU_PARAM_NUM_FILES, "7");
    props.setProperty("org.lockss.au." + auId1 + "." +
                      SimulatedPlugin.AU_PARAM_FILE_TYPES, "" +
                      (SimulatedContentGenerator.FILE_TYPE_PDF +
                       SimulatedContentGenerator.FILE_TYPE_HTML));
    props.setProperty("org.lockss.au." + auId1 + "." +
                      SimulatedPlugin.AU_PARAM_BIN_FILE_SIZE, ""+fileSize);


    props.setProperty("org.lockss.au." + auId2 + "." +
                      SimulatedPlugin.AU_PARAM_ROOT, tempDirPath);
    // the simulated Content's depth will be (AU_PARAM_DEPTH + 1)
    props.setProperty("org.lockss.au." + auId2 + "." +
                      SimulatedPlugin.AU_PARAM_DEPTH, "3");
    props.setProperty("org.lockss.au." + auId2 + "." +
                      SimulatedPlugin.AU_PARAM_BRANCH, "3");
    props.setProperty("org.lockss.au." + auId2 + "." +
                      SimulatedPlugin.AU_PARAM_NUM_FILES, "7");
    props.setProperty("org.lockss.au." + auId2 + "." +
                      SimulatedPlugin.AU_PARAM_FILE_TYPES, "" +
                      (SimulatedContentGenerator.FILE_TYPE_PDF +
                       SimulatedContentGenerator.FILE_TYPE_HTML));
    props.setProperty("org.lockss.au." + auId2 + "." +
                      SimulatedPlugin.AU_PARAM_BIN_FILE_SIZE, ""+fileSize);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();

    // set derby database log 
    System.setProperty("derby.stream.error.file", new File(tempDirPath,"derby.log").getAbsolutePath());
    
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
    
    theDaemon.getCrawlManager();
    ConfigurationUtil.setCurrentConfigFromProps(props);

    sau0 =
        (SimulatedArchivalUnit)theDaemon.getPluginManager().getAllAus().get(0);
    theDaemon.getLockssRepository(sau0).startService();
    theDaemon.setNodeManager(new MockNodeManager(), sau0);

    sau1 =
      (SimulatedArchivalUnit)theDaemon.getPluginManager().getAllAus().get(1);
    theDaemon.getLockssRepository(sau1).startService();
    theDaemon.setNodeManager(new MockNodeManager(), sau1);

    sau2 =
      (SimulatedArchivalUnit)theDaemon.getPluginManager().getAllAus().get(2);
    theDaemon.getLockssRepository(sau2).startService();
    theDaemon.setNodeManager(new MockNodeManager(), sau2);

    crawlContent();

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
    // reset set of reindexed aus
    ausReindexed.clear();

    metadataManager.restartService();
    theDaemon.setAusStarted(true);
    
    DataSource ds = metadataManager.getDataSource();
    assertNotNull(ds);
    
    int expectedAuCount = 3;
    assertEquals(expectedAuCount, theDaemon.getPluginManager().getAllAus().size());
    
    Connection con = ds.getConnection();
    
    long maxWaitTime = expectedAuCount * 20000; // 20 sec. per au
    int ausCount = waitForReindexing(expectedAuCount, maxWaitTime);
    assertEquals(expectedAuCount, ausCount);
    
    assertEquals(0, metadataManager.reindexingTasks.size());
    assertEquals(0, metadataManager.getAusToReindex(con, Integer.MAX_VALUE).size());

    String query =           
      "select access_url from " + MetadataManager.METADATA_TABLE_NAME; 
    Statement stmt = con.createStatement();
    ResultSet resultSet = stmt.executeQuery(query);
    if (!resultSet.next()) {
      fail("No entries in metadata table");
    }
    String url = resultSet.getString(1);
    log.debug("url from metadata table: " + url);

    con.commit();
    con.close();
  }
  
  public void testModifyMetadata() throws Exception {
    // reset set of reindexed aus
    ausReindexed.clear();

    metadataManager.restartService();
    theDaemon.setAusStarted(true);
    
    DataSource ds = metadataManager.getDataSource();
    assertNotNull(ds);
    
    int expectedAuCount = 3;
    assertEquals(expectedAuCount, theDaemon.getPluginManager().getAllAus().size());
    
    Connection con = ds.getConnection();
    
    long maxWaitTime = expectedAuCount * 10000; // 10 sec. per au
    int ausCount = waitForReindexing(expectedAuCount, maxWaitTime);
    assertEquals(expectedAuCount, ausCount);
    
    assertEquals(0, metadataManager.reindexingTasks.size());
    assertEquals(0, metadataManager.getAusToReindex(con, Integer.MAX_VALUE).size());

    String query =           
      "select access_url from " + MetadataManager.METADATA_TABLE_NAME; 
    Statement stmt = con.createStatement();
    ResultSet resultSet = stmt.executeQuery(query);
    if (!resultSet.next()) {
      fail("No entries in metadata table");
    }
    String url = resultSet.getString(1);
    log.debug("url from metadata table: " + url);

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
    
    expectedAuCount = 1;
    maxWaitTime = 10000; // 10 sec. per au
    ausCount = waitForReindexing(expectedAuCount, maxWaitTime);
    assertEquals(ausCount, expectedAuCount);

    assertEquals(0, metadataManager.reindexingTasks.size());
    assertEquals(0, metadataManager.getAusToReindex(con, Integer.MAX_VALUE).size());

    con.commit();
    con.close();
  }
  
  private void crawlContent() {
    log.debug("Crawling tree...");
    CrawlSpec spec0 = new SpiderCrawlSpec(sau0.getNewContentCrawlUrls(), null);
    NewContentCrawler crawler0 =
      new NewContentCrawler(sau0, spec0, new MockAuState());
    //crawler0.setCrawlManager(crawlMgr);
    crawler0.doCrawl();

    CrawlSpec spec1 = new SpiderCrawlSpec(sau1.getNewContentCrawlUrls(), null);
    NewContentCrawler crawler1 =
      new NewContentCrawler(sau1, spec1, new MockAuState());
    //crawler1.setCrawlManager(crawlMgr);
    crawler1.doCrawl();

    CrawlSpec spec2 = new SpiderCrawlSpec(sau2.getNewContentCrawlUrls(), null);
    NewContentCrawler crawler2 =
      new NewContentCrawler(sau2, spec2, new MockAuState());
    //crawler2.setCrawlManager(crawlMgr);
    crawler2.doCrawl();

  }

  public static class MySimulatedPlugin extends SimulatedPlugin {
    ArticleMetadataExtractor simulatedArticleMetadataExtractor = null;
    
    public ArchivalUnit createAu0(Configuration auConfig)
        throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    /**
     * Returns the article iterator factory for the mime type, if any
     * @param contentType the content type
     * @return the ArticleIteratorFactory
     */
    @Override
    public ArticleIteratorFactory getArticleIteratorFactory() {
      MySubTreeArticleIteratorFactory ret =
          new MySubTreeArticleIteratorFactory("branch1/branch1");
      return ret;
    }
    @Override
    public ArticleMetadataExtractor getArticleMetadataExtractor(
                                        MetadataTarget target,  ArchivalUnit au) {
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
          String doi = "10.1234/12345678.2010-01." +  articleNumber;
          md.put(MetadataField.FIELD_DOI,doi);
          md.put(MetadataField.FIELD_ISSN,"1234-5678");
          md.put(MetadataField.FIELD_ISSN,"XI");
          md.put(MetadataField.FIELD_ISSUE,"1st Quarter");
          md.put(MetadataField.FIELD_DATE,"2010-01");
          md.put(MetadataField.FIELD_START_PAGE,"" + articleNumber);
          md.put(MetadataField.FIELD_JOURNAL_TITLE,"Journal[" + doi + "]");
          md.put(MetadataField.FIELD_ARTICLE_TITLE,"Title[" + doi + "]");
          md.put(MetadataField.FIELD_AUTHOR,"Author1[" + doi + "]");
          md.put(MetadataField.FIELD_AUTHOR,"Author2[" + doi + "]");
          emitter.emitMetadata(af, md);
        }
      };
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
          String doi = "10.2468/28681357.2010-06."+ articleNumber;
          md.put(MetadataField.FIELD_DOI,doi);
          md.put(MetadataField.FIELD_ISSN,"2468-1357");
          md.put(MetadataField.FIELD_VOLUME,"42");
          md.put(MetadataField.FIELD_ISSUE,"Summer 2010");
          md.put(MetadataField.FIELD_DATE,"2010-06");
          md.put(MetadataField.FIELD_START_PAGE,"" + articleNumber);
          md.put(MetadataField.FIELD_JOURNAL_TITLE,"Journal[" + doi + "]");
          md.put(MetadataField.FIELD_ARTICLE_TITLE,"Title[" + doi + "]");
//PJG: need to test multiple authors!
          md.put(MetadataField.FIELD_AUTHOR,"Author1[" + doi + "]");
          md.put(MetadataField.FIELD_AUTHOR,"Author2[" + doi + "]");
          emitter.emitMetadata(af, md);
        }
      };
    }
  }
  
  public static class MySimulatedPlugin2 extends MySimulatedPlugin {
    public MySimulatedPlugin2() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        int articleNumber = 0;
        public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
          throws IOException, PluginException {
          ArticleMetadata md = new ArticleMetadata();
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
          emitter.emitMetadata(af, md);
        }
      };
    }
  }
  
  public static class MySubTreeArticleIteratorFactory
      implements ArticleIteratorFactory {
    String pat;
    MySubTreeArticleIteratorFactory(String pat) {
      this.pat = pat;
    }
    
    /**
     * Create an Iterator that iterates through the AU's articles, pointing
     * to the appropriate CachedUrl of type mimeType for each, or to the plugin's
     * choice of CachedUrl if mimeType is null
     * @param au the ArchivalUnit to iterate through
     * @return the ArticleIterator
     */
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
        throws PluginException {
      Iterator<ArticleFiles> ret;
      SubTreeArticleIterator.Spec spec = 
        new SubTreeArticleIterator.Spec().setTarget(target);
      
      if (pat != null) {
       spec.setPattern(pat);
      }
      
      ret = new MySubTreeArticleIterator(au, spec);
      log.debug("***creating article iterator for au " + au.getName() + " hasNext: " + ret.hasNext());
      return ret;
    }
  }

  public static class MySubTreeArticleIterator extends SubTreeArticleIterator {
    MySubTreeArticleIterator(ArchivalUnit au, Spec spec) {
      super(au, spec);
    }

    @Override
    protected boolean isArticleCu(CachedUrl cu) {
      boolean result = super.isArticleCu(cu);
      return result;
    }
  }
}
