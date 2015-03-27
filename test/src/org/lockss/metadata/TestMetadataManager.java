/*
 * $Id$
 */

/*

Copyright (c) 2013-2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.metadata;

import static org.lockss.db.SqlConstants.*;
import static org.lockss.metadata.MetadataManagerStatusAccessor.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import org.lockss.config.*;
import org.lockss.config.Configuration.Differences;
import org.lockss.daemon.PluginException;
import org.lockss.db.DbManager;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.metadata.MetadataManager.PrioritizedAuId;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.metadata.MetadataManager
 *
 * @author  Philip Gust
 * @version 1.0
 */
public class TestMetadataManager extends LockssTestCase {
  static Logger log = Logger.getLogger(TestMetadataManager.class);

  private SimulatedArchivalUnit sau0, sau1, sau2, sau3, sau4;
  private MockLockssDaemon theDaemon;
  private MetadataManager metadataManager;
  private MetadataManagerSql metadataManagerSql;
  private PluginManager pluginManager;
  private DbManager dbManager;

  /** set of AuIds of AUs reindexed by the MetadataManager */
  Set<String> ausReindexed = new HashSet<String>();
  
  /** number of articles deleted by the MetadataManager */
  Integer[] articlesDeleted = new Integer[] {0};
  
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEXING_ENABLED,
	"true");

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    pluginManager = theDaemon.getPluginManager();
    pluginManager.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getCrawlManager();

    sau0 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin0.class,
                                              simAuConfig(tempDirPath + "/0"));
    sau1 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin1.class,
                                              simAuConfig(tempDirPath + "/1"));
    sau2 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin2.class,
                                              simAuConfig(tempDirPath + "/2"));
    sau3 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin3.class,
                                              simAuConfig(tempDirPath + "/3"));
    sau4 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin0.class,
                                              simAuConfig(tempDirPath + "/4"));
    PluginTestUtil.crawlSimAu(sau0);
    PluginTestUtil.crawlSimAu(sau1);
    PluginTestUtil.crawlSimAu(sau2);
    PluginTestUtil.crawlSimAu(sau3);
    PluginTestUtil.crawlSimAu(sau4);

    // reset set of reindexed aus
    ausReindexed.clear();

    dbManager = getTestDbManager(tempDirPath);

    metadataManager = new MetadataManager() {
      /**
       * Notify listeners that an AU has been deleted
       * @param auId the AuId of the AU that was deleted
       * @param articleCount the number of articles deleted for the AU
       */
      protected void notifyDeletedAu(String auId, int articleCount) {
	synchronized (articlesDeleted) {
	  articlesDeleted[0] += articleCount;
	  articlesDeleted.notifyAll();
	}
      }

      /**
       * Notify listeners that an AU is being reindexed.
       * 
       * @param au
       */
      protected void notifyStartReindexingAu(ArchivalUnit au) {
        log.info("Start reindexing au " + au);
      }
      
      /**
       * Notify listeners that an AU is finshed being reindexed.
       * 
       * @param au
       */
      protected void notifyFinishReindexingAu(ArchivalUnit au,
	  ReindexingStatus status) {
        log.info("Finished reindexing au (" + status + ") " + au);
        if (status != ReindexingStatus.Rescheduled) {
          synchronized (ausReindexed) {
            ausReindexed.add(au.getAuId());
            ausReindexed.notifyAll();
          }
        }
      }
    };
    
    theDaemon.setMetadataManager(metadataManager);
    metadataManager.initService(theDaemon);
    metadataManager.startService();

    metadataManagerSql = metadataManager.getMetadataManagerSql();

    theDaemon.setAusStarted(true);
    
    int expectedAuCount = 5;
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

  public void testAll() throws Exception {
    runCreateMetadataTest();
    runTestPendingAu();
    runTestPendingAusBatch();
    runModifyMetadataTest();
    runDeleteAuMetadataTest();
    runTestPriorityPatterns();
    runTestDisabledIndexingAu();
    runTestFailedIndexingAu();
    runTestFindPublication();
    runTestGetIndexTypeDisplayString();
    runRemoveChildMetadataItemTest();
    runMetadataMonitorTest();
  }

  private void runCreateMetadataTest() throws Exception {
    Connection con = dbManager.getConnection();
    
    assertEquals(0, metadataManager.activeReindexingTasks.size());
    assertEquals(0, metadataManagerSql.getPrioritizedAuIdsToReindex(con,
	Integer.MAX_VALUE, metadataManager.isPrioritizeIndexingNewAus())
	.size());

    // 21 articles for each of four AUs
    // au0 and au1 for plugin0 have the same articles for different AUs
    long articleCount = metadataManagerSql.getArticleCount(con);
    assertEquals(105, articleCount);
    
    // one pubication for each of four plugins
    long publicationCount = metadataManagerSql.getPublicationCount(con);
    assertEquals(4, publicationCount);
    
    // one publisher for each of four plugins
    long publisherCount = metadataManagerSql.getPublisherCount(con);
    assertEquals(4, publisherCount);
    
    // one more provider than publications because
    // au0 and au4 for plugin0 have different providers
    long providerCount = metadataManagerSql.getProviderCount(con);
    assertEquals(5, providerCount);
    
    // check distinct access URLs
    String query =           
      "select distinct " + URL_COLUMN + " from " + URL_TABLE;
    PreparedStatement stmt = dbManager.prepareStatement(con, query);
    ResultSet resultSet = dbManager.executeQuery(stmt);
    int count = 0;
    while (resultSet.next()) {
      count++;
    }
    final int metadataRowCount = 105;
    assertEquals(metadataRowCount, count);

    // check unique plugin IDs
    query =           
        "select distinct " + PLUGIN_ID_COLUMN 
        + " from " + PLUGIN_TABLE; 
    stmt = dbManager.prepareStatement(con, query);
    resultSet = dbManager.executeQuery(stmt);
    Set<String> results = new HashSet<String>();
    while (resultSet.next()) {
      results.add(resultSet.getString(PLUGIN_ID_COLUMN));
    }
    assertEquals(4, results.size());
    results.
    	remove("org|lockss|metadata|TestMetadataManager$MySimulatedPlugin0");
    results.
    	remove("org|lockss|metadata|TestMetadataManager$MySimulatedPlugin1");
    results.
    	remove("org|lockss|metadata|TestMetadataManager$MySimulatedPlugin2");
    results.
    	remove("org|lockss|metadata|TestMetadataManager$MySimulatedPlugin3");
    assertEquals(0, results.size());
    
    // check DOIs
    query =           
        "select distinct " + DOI_COLUMN + " from " + DOI_TABLE; 
    stmt = dbManager.prepareStatement(con, query);
    resultSet = dbManager.executeQuery(stmt);
    results = new HashSet<String>();
    while (resultSet.next()) {
      results.add(resultSet.getString(DOI_COLUMN));
    }
    assertEquals(metadataRowCount, results.size());

    // check ISSNs
    query = "select " + ISSN_COLUMN + "," + ISSN_TYPE_COLUMN
	+ " from " + ISSN_TABLE;
    stmt = dbManager.prepareStatement(con, query);
    resultSet = dbManager.executeQuery(stmt);
    results = new HashSet<String>();
    while (resultSet.next()) {
      if (P_ISSN_TYPE.equals(resultSet.getString(ISSN_TYPE_COLUMN))) {
	String pIssn = resultSet.getString(ISSN_COLUMN);
	if (!resultSet.wasNull() && pIssn != null) {
	  results.add(pIssn);
	}
      } else if (E_ISSN_TYPE.equals(resultSet.getString(ISSN_TYPE_COLUMN))) {
	String eIssn = resultSet.getString(ISSN_COLUMN);
	if (!resultSet.wasNull() && eIssn != null) {
	  results.add(eIssn);
	}
      } 
    }
    assertEquals(3, results.size());
    results.remove("77446521");
    results.remove("1144875X");
    results.remove("07402783");
    assertEquals(0, results.size());
    
    // check ISBNs
    query = "select " + ISBN_COLUMN + "," + ISBN_TYPE_COLUMN
	+ " from " + ISBN_TABLE;
    stmt = dbManager.prepareStatement(con, query);
    resultSet = dbManager.executeQuery(stmt);
    results = new HashSet<String>();
    while (resultSet.next()) {
      if (P_ISBN_TYPE.equals(resultSet.getString(ISBN_TYPE_COLUMN))) {
	String pIsbn = resultSet.getString(ISBN_COLUMN);
	if (!resultSet.wasNull() && pIsbn != null) {
	  results.add(pIsbn);
	}
      } else if (E_ISBN_TYPE.equals(resultSet.getString(ISBN_TYPE_COLUMN))) {
	String eIsbn = resultSet.getString(ISBN_COLUMN);
	if (!resultSet.wasNull() && eIsbn != null) {
	  results.add(eIsbn);
	}
      } 
      //results.add(resultSet.getString(MetadataManager.P_ISBN_FIELD));
      //log.critical(resultSet.getString(MetadataManager.P_ISBN_FIELD));
      //results.add(resultSet.getString(MetadataManager.E_ISBN_FIELD));
      //log.critical(resultSet.getString(MetadataManager.E_ISBN_FIELD));
    }
    assertEquals(2, results.size());
    results.remove("9781585623174");
    results.remove("9761585623177");
    assertEquals(0, results.size());
    
    assertEquals(0, metadataManager.activeReindexingTasks.size());
    assertEquals(0, metadataManagerSql.getPrioritizedAuIdsToReindex(con,
	Integer.MAX_VALUE, metadataManager.isPrioritizeIndexingNewAus())
	.size());

    DbManager.safeRollbackAndClose(con);
  }

  private void runTestPendingAu() throws Exception {
    // We are only testing here the addition of AUs to the table of pending AUs,
    // so disable re-indexing.
    metadataManager.setIndexingEnabled(false);

    Connection conn = dbManager.getConnection();

    PreparedStatement insertPendingAuBatchStatement =
	metadataManager.getInsertPendingAuBatchStatement(conn);

    // Add one AU for incremental metadata indexing.
    metadataManager.enableAndAddAuToReindex(sau0, conn,
	insertPendingAuBatchStatement, false, false);
    
    // Check that the row is there.
    String countPendingAuQuery = "select count(*) from " + PENDING_AU_TABLE;
    checkRowCount(conn, countPendingAuQuery, 1);

    // Make sure that it is marked for incremental metadata indexing.
    assertFalse(metadataManagerSql.needAuFullReindexing(conn, sau0));

    // Add the same AU for full metadata indexing.
    metadataManager.enableAndAddAuToReindex(sau0, conn,
	insertPendingAuBatchStatement, false, true);
    
    // Check that the same row is there.
    checkRowCount(conn, countPendingAuQuery, 1);

    // Make sure that it is marked for incremental metadata indexing.
    assertTrue(metadataManagerSql.needAuFullReindexing(conn, sau0));

    // Add the same AU for incremental metadata indexing.
    metadataManager.enableAndAddAuToReindex(sau0, conn,
	insertPendingAuBatchStatement, false, true);
    
    // Check that the same row is there.
    checkRowCount(conn, countPendingAuQuery, 1);

    // Make sure that it is still marked for full metadata indexing.
    assertTrue(metadataManagerSql.needAuFullReindexing(conn, sau0));

    // Add another AU for full metadata indexing.
    metadataManager.enableAndAddAuToReindex(sau1, conn,
	insertPendingAuBatchStatement, false, true);
    
    // Check that the new row is there.
    checkRowCount(conn, countPendingAuQuery, 2);

    // Verify the type of metadata indexing.
    assertTrue(metadataManagerSql.needAuFullReindexing(conn, sau0));
    assertTrue(metadataManagerSql.needAuFullReindexing(conn, sau1));

    // Add a third AU for incremental metadata indexing.
    metadataManager.enableAndAddAuToReindex(sau2, conn,
	insertPendingAuBatchStatement, false, false);
    
    // Check that the row is there.
    checkRowCount(conn, countPendingAuQuery, 3);

    // Verify the type of metadata indexing.
    assertTrue(metadataManagerSql.needAuFullReindexing(conn, sau0));
    assertTrue(metadataManagerSql.needAuFullReindexing(conn, sau1));
    assertFalse(metadataManagerSql.needAuFullReindexing(conn, sau2));

    // Clear the table of pending AUs.
    checkExecuteCount(conn, "delete from " + PENDING_AU_TABLE, 3);

    conn.commit();
    DbManager.safeRollbackAndClose(conn);

    // Re-enable re-indexing.
    metadataManager.setIndexingEnabled(true);
  }

  private void checkRowCount(Connection conn, String query, int expectedCount)
      throws Exception {
    PreparedStatement stmt = dbManager.prepareStatement(conn, query);
    ResultSet resultSet = dbManager.executeQuery(stmt);
    int count = -1;

    if (resultSet.next()) {
      count = resultSet.getInt(1);
    }

    assertEquals(expectedCount, count);
  }

  private void checkExecuteCount(Connection conn, String query,
      int expectedCount) throws Exception {
    PreparedStatement stmt = dbManager.prepareStatement(conn, query);
    int count = dbManager.executeUpdate(stmt);
    assertEquals(expectedCount, count);
  }

  private void runTestPendingAusBatch() throws Exception {
    // Set to 2 the batch size for adding pending AUs.
    ConfigurationUtil
	.addFromArgs(MetadataManager.PARAM_MAX_PENDING_TO_REINDEX_AU_BATCH_SIZE,
		     "2");

    // We are only testing here the addition of AUs to the table of pending AUs,
    // so disable re-indexing.
    metadataManager.setIndexingEnabled(false);

    Connection con = dbManager.getConnection();

    PreparedStatement insertPendingAuBatchStatement =
	metadataManager.getInsertPendingAuBatchStatement(con);

    // Add one AU.
    metadataManager.enableAndAddAuToReindex(sau0, con,
	insertPendingAuBatchStatement, true);
    
    // Check that nothing has been added yet.
    String countPendingAuQuery = "select count(*) from " + PENDING_AU_TABLE;
    checkRowCount(con, countPendingAuQuery, 0);

    // Add the second AU.
    metadataManager.enableAndAddAuToReindex(sau1, con,
	insertPendingAuBatchStatement, true);
    
    // Check that one batch has been executed.
    checkRowCount(con, countPendingAuQuery, 2);
    String countFullReindexQuery = "select count(*) from " + PENDING_AU_TABLE
	+ " where " + FULLY_REINDEX_COLUMN + " = true";
    checkRowCount(con, countFullReindexQuery, 2);

    // Add the third AU.
    metadataManager.enableAndAddAuToReindex(sau2, con,
	insertPendingAuBatchStatement, true);

    // Check that the third AU has not been added yet.
    checkRowCount(con, countPendingAuQuery, 2);
    checkRowCount(con, countFullReindexQuery, 2);

    // Add the fourth AU.
    metadataManager.enableAndAddAuToReindex(sau3, con,
	insertPendingAuBatchStatement, true);
    
    // Check that the second batch has been executed.
    checkRowCount(con, countPendingAuQuery, 4);
    checkRowCount(con, countFullReindexQuery, 4);

    // Add the last AU.
    metadataManager.enableAndAddAuToReindex(sau4, con,
	insertPendingAuBatchStatement, false);

    // Check that all the AUs have been added.
    checkRowCount(con, countPendingAuQuery, 5);
    checkRowCount(con, countFullReindexQuery, 5);

    // insert another pending AU that is not also in the metadata manager
    insertPendingAuBatchStatement.setString(1, "XyzzyPlugin");
    insertPendingAuBatchStatement.setString(2, "journal_id=xyzzy");
    insertPendingAuBatchStatement.setBoolean(3, Boolean.FALSE);
    insertPendingAuBatchStatement.execute();

    // Check that the last AU has been added.
    checkRowCount(con, countPendingAuQuery, 6);
    checkRowCount(con, countFullReindexQuery, 5);

    assertTrue(metadataManager.isPrioritizeIndexingNewAus());

    // ensure that the the most recently added "new" AU is prioritized first
    List<MetadataManager.PrioritizedAuId> auids =
        metadataManagerSql.getPrioritizedAuIdsToReindex(con, Integer.MAX_VALUE,
            metadataManager.isPrioritizeIndexingNewAus());
    assertEquals(6, auids.size());
    assertTrue(auids.get(0).isNew);
    assertFalse(auids.get(0).needFullReindex);

    for (int i = 1; i <= 5; i++) {
      assertFalse(auids.get(i).isNew);
      assertTrue(auids.get(i).needFullReindex);
    }

    // modify the metadata manager to not prioritize new AUs over existing ones
    Configuration newConf = ConfigManager.newConfiguration();
    newConf.put("org.lockss.metadataManager.prioritizeIndexingNewAus", "false");
    Configuration oldConf = ConfigManager.newConfiguration();
    Differences diffs = newConf.differences(oldConf);
    metadataManager.setConfig(newConf, oldConf, diffs);

    assertFalse(metadataManager.isPrioritizeIndexingNewAus());

    // ensure that the the most recently added "new" AU is prioritized last
    auids = metadataManagerSql.getPrioritizedAuIdsToReindex(con,
	Integer.MAX_VALUE, metadataManager.isPrioritizeIndexingNewAus());
    assertEquals(6, auids.size());
    for (int i = 0; i < 5; i++) {
      assertFalse(auids.get(i).isNew);
      assertTrue(auids.get(i).needFullReindex);
    }

    assertTrue(auids.get(5).isNew);  // most recently added
    assertFalse(auids.get(5).needFullReindex);

    // Clear the table of pending AUs.
    checkExecuteCount(con, "delete from " + PENDING_AU_TABLE, 6);

    con.commit();
    DbManager.safeRollbackAndClose(con);

    // Re-enable re-indexing.
    metadataManager.setIndexingEnabled(true);
  }

  private void runModifyMetadataTest() throws Exception {
    Connection con = dbManager.getConnection();
    
    // check unique plugin IDs
    String query = "select distinct u." + URL_COLUMN 
	+ " from " + URL_TABLE + " u,"
        + MD_ITEM_TABLE + " m,"
        + AU_MD_TABLE + " am,"
        + AU_TABLE + " au,"
        + PLUGIN_TABLE + " pl"
        + " where pl." + PLUGIN_ID_COLUMN 
        + " = 'org|lockss|metadata|TestMetadataManager$MySimulatedPlugin0'" 
        + " and pl." + PLUGIN_SEQ_COLUMN 
        + " = au." + PLUGIN_SEQ_COLUMN
        + " and au." + AU_SEQ_COLUMN 
        + " = am." + AU_SEQ_COLUMN
        + " and am." + AU_MD_SEQ_COLUMN 
        + " = m." + AU_MD_SEQ_COLUMN
        + " and m." + MD_ITEM_SEQ_COLUMN 
        + " = u." + MD_ITEM_SEQ_COLUMN;
    PreparedStatement stmt = dbManager.prepareStatement(con, query);
    ResultSet resultSet = dbManager.executeQuery(stmt);
    Set<String> results = new HashSet<String>();
    while (resultSet.next()) {
      results.add(resultSet.getString(1));
    }
    final int count = results.size();
    assertEquals(42, count);

    // reset set of reindexed aus
    ausReindexed.clear();

    // simulate an au change
    pluginManager.applyAuEvent(new PluginManager.AuEventClosure() {
	public void execute(AuEventHandler hand) {
	  AuEventHandler.ChangeInfo chInfo =
	    new AuEventHandler.ChangeInfo();
	  chInfo.setAu(sau0);        // reindex simulated AU
	  chInfo.setComplete(true);   // crawl was complete
	  hand.auContentChanged(new AuEvent(AuEvent.Type.ContentChanged, false),
	                        sau0, chInfo);
	}
      });
    
    // ensure only expected number of AUs were reindexed
    final int expectedAuCount = 1;
    int maxWaitTime = 10000; // 10 sec. per au
    int ausCount = waitForReindexing(expectedAuCount, maxWaitTime);
    assertEquals(ausCount, expectedAuCount);

    // ensure AU contains as many metadata table entries as before
    resultSet = dbManager.executeQuery(stmt);
    results = new HashSet<String>();
    while (resultSet.next()) {
      results.add(resultSet.getString(1));
    }
    assertEquals(42, results.size());

    DbManager.safeRollbackAndClose(con);
  }
  
  private void runDeleteAuMetadataTest() throws Exception {
    Connection con = dbManager.getConnection();
    
    // check unique plugin IDs
    String query = "select distinct u." + URL_COLUMN 
	+ " from " + URL_TABLE + " u,"
        + MD_ITEM_TABLE + " m,"
        + AU_MD_TABLE + " am,"
        + AU_TABLE + " au,"
        + PLUGIN_TABLE + " pl"
        + " where pl." + PLUGIN_ID_COLUMN 
        + " = 'org|lockss|metadata|TestMetadataManager$MySimulatedPlugin0'"
        + " and pl." + PLUGIN_SEQ_COLUMN 
        + " = au." + PLUGIN_SEQ_COLUMN
        + " and au." + AU_SEQ_COLUMN 
        + " = am." + AU_SEQ_COLUMN
        + " and am." + AU_MD_SEQ_COLUMN 
        + " = m." + AU_MD_SEQ_COLUMN
        + " and m." + MD_ITEM_SEQ_COLUMN 
        + " = u." + MD_ITEM_SEQ_COLUMN;
    PreparedStatement stmt = dbManager.prepareStatement(con, query);
    ResultSet resultSet = dbManager.executeQuery(stmt);
    Set<String> results = new HashSet<String>();
    while (resultSet.next()) {
      results.add(resultSet.getString(1));
    }
    final int count = results.size();
    assertEquals(42, count);

    // reset set of reindexed aus
    ausReindexed.clear();

    // delete AU
    pluginManager.stopAu(sau0, new AuEvent(AuEvent.Type.Delete, false));

    int maxWaitTime = 10000; // 10 sec. per au
    int articleCount = waitForDeleted(1, maxWaitTime);
    assertEquals(21, articleCount);

    // ensure metadata table entries for the AU are deleted
    resultSet = dbManager.executeQuery(stmt);
    results = new HashSet<String>();
    while (resultSet.next()) {
      results.add(resultSet.getString(1));
    }
    assertEquals(21, results.size());

    DbManager.safeRollbackAndClose(con);
  }

  /**
   * Waits a specified period for a specified number of AUs to finish 
   * being deleted.  Returns the actual number of AUs deleted.
   * 
   * @param auCount the expected AU count
   * @param maxWaitTime the maximum time to wait
   * @return the number of AUs deleted
   */
  private int waitForDeleted(int auCount, long maxWaitTime) {
    long startTime = System.currentTimeMillis();
    synchronized (articlesDeleted) {
      while (   (System.currentTimeMillis()-startTime < maxWaitTime) 
             && (articlesDeleted[0] < auCount)) {
        try {
          articlesDeleted.wait(maxWaitTime);
        } catch (InterruptedException ex) {
        }
      }
    }
    return articlesDeleted[0];
  }

  private void runTestPriorityPatterns() {
    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEX_PRIORITY_AUID_MAP,
				  "foo(4|5),-10000;bar,5;baz,-1");
    MockArchivalUnit mau1 = new MockArchivalUnit(new MockPlugin(theDaemon));
    mau1.setAuId("other");
    assertTrue(metadataManager.isEligibleForReindexing(mau1));
    mau1.setAuId("foo4");
    assertFalse(metadataManager.isEligibleForReindexing(mau1));

    // Remove param, ensure priority map gets removed
    ConfigurationUtil.resetConfig();
    mau1.setAuId("foo4");
    assertTrue(metadataManager.isEligibleForReindexing(mau1));

  }
  
  private void runTestDisabledIndexingAu() throws Exception {
    Connection con = dbManager.getConnection();
    
    // Add a disabled AU.
    metadataManagerSql.addDisabledAuToPendingAus(con, sau0.getAuId());
    con.commit();

    // Make sure that it is there.
    assertEquals(1, metadataManager.findDisabledPendingAus(con).size());
    DbManager.safeRollbackAndClose(con);
  }
  
  private void runTestFailedIndexingAu() throws Exception {
    Connection con = dbManager.getConnection();
    
    // Add an AU with a failed indexing process.
    metadataManagerSql.addFailedIndexingAuToPendingAus(con, sau1.getAuId());
    con.commit();

    // Make sure that it is there.
    assertEquals(1, metadataManager.findFailedIndexingPendingAus(con).size());
    DbManager.safeRollbackAndClose(con);
  }

  private void runTestFindPublication() throws Exception {
    Connection conn = dbManager.getConnection();

    List<Long> journals = new ArrayList<Long>();
    List<Long> books = new ArrayList<Long>();
    Map<Long, Long> publishers = new HashMap<Long, Long>();
    Map<Long, Long> mdItems = new HashMap<Long, Long>();
    Map<Long, String> names = new HashMap<Long, String>();

    String query = "select p." + PUBLICATION_SEQ_COLUMN
	+ ", p." + PUBLISHER_SEQ_COLUMN
	+ ", p." + MD_ITEM_SEQ_COLUMN
	+ ", mt." + TYPE_NAME_COLUMN
	+ ", n." + NAME_COLUMN
	+ " from " + MD_ITEM_TYPE_TABLE + " mt"
	+ ", " + MD_ITEM_NAME_TABLE + " n"
	+ ", " + MD_ITEM_TABLE + " m"
	+ ", " + PUBLICATION_TABLE + " p"
	+ " where mt." + MD_ITEM_TYPE_SEQ_COLUMN
	+ " = m." + MD_ITEM_TYPE_SEQ_COLUMN
	+ " and m." + MD_ITEM_SEQ_COLUMN + " = p." + MD_ITEM_SEQ_COLUMN
	+ " and n." + MD_ITEM_SEQ_COLUMN + " = p." + MD_ITEM_SEQ_COLUMN;

    PreparedStatement stmt = dbManager.prepareStatement(conn, query);
    ResultSet resultSet = dbManager.executeQuery(stmt);

    while (resultSet.next()) {
      Long publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
      String typeName = resultSet.getString(TYPE_NAME_COLUMN);

      if (MD_ITEM_TYPE_JOURNAL.equals(typeName)) {
	journals.add(publicationSeq);
      } else if (MD_ITEM_TYPE_BOOK.equals(typeName)) {
	books.add(publicationSeq);
      }

      publishers.put(publicationSeq, resultSet.getLong(PUBLISHER_SEQ_COLUMN));
      mdItems.put(publicationSeq, resultSet.getLong(MD_ITEM_SEQ_COLUMN));
      names.put(publicationSeq, resultSet.getString(NAME_COLUMN));
    }

    Map<Long, String> pIssns = new HashMap<Long, String>();
    Map<Long, String> eIssns = new HashMap<Long, String>();

    query = "select p." + PUBLICATION_SEQ_COLUMN
	+ ", i." + ISSN_COLUMN
	+ ", i." + ISSN_TYPE_COLUMN
	+ " from " + ISSN_TABLE + " i"
	+ ", " + PUBLICATION_TABLE + " p"
	+ " where i." + MD_ITEM_SEQ_COLUMN + " = p." + MD_ITEM_SEQ_COLUMN;

    stmt = dbManager.prepareStatement(conn, query);
    resultSet = dbManager.executeQuery(stmt);

    while (resultSet.next()) {
      if (P_ISSN_TYPE.equals(resultSet.getString(ISSN_TYPE_COLUMN))) {
	pIssns.put(resultSet.getLong(PUBLICATION_SEQ_COLUMN),
	    resultSet.getString(ISSN_COLUMN));
      } else if (E_ISSN_TYPE.equals(resultSet.getString(ISSN_TYPE_COLUMN))) {
	eIssns.put(resultSet.getLong(PUBLICATION_SEQ_COLUMN),
	    resultSet.getString(ISSN_COLUMN));
      }
    }

    Map<Long, String> pIsbns = new HashMap<Long, String>();
    Map<Long, String> eIsbns = new HashMap<Long, String>();

    query = "select p." + PUBLICATION_SEQ_COLUMN
	+ ", i." + ISBN_COLUMN
	+ ", i." + ISBN_TYPE_COLUMN
	+ " from " + ISBN_TABLE + " i"
	+ ", " + PUBLICATION_TABLE + " p"
	+ " where i." + MD_ITEM_SEQ_COLUMN + " = p." + MD_ITEM_SEQ_COLUMN;

    stmt = dbManager.prepareStatement(conn, query);
    resultSet = dbManager.executeQuery(stmt);

    while (resultSet.next()) {
      if (P_ISBN_TYPE.equals(resultSet.getString(ISBN_TYPE_COLUMN))) {
	pIsbns.put(resultSet.getLong(PUBLICATION_SEQ_COLUMN),
	    resultSet.getString(ISBN_COLUMN));
      } else if (E_ISBN_TYPE.equals(resultSet.getString(ISBN_TYPE_COLUMN))) {
	eIsbns.put(resultSet.getLong(PUBLICATION_SEQ_COLUMN),
	    resultSet.getString(ISBN_COLUMN));
      }
    }

    runTestFindJournal(conn, journals, publishers, names, pIssns, eIssns,
	pIsbns, eIsbns);

    runTestFindBook(conn, books, publishers, names, pIssns, eIssns, pIsbns,
	eIsbns);

    runTestFindBookSeries(conn, journals, mdItems, publishers, names, pIssns,
	eIssns,	pIsbns, eIsbns);

    DbManager.safeRollbackAndClose(conn);
  }

  private void runTestFindJournal(Connection conn, List<Long> journals,
      Map<Long, Long> publishers, Map<Long, String> names,
      Map<Long, String> pIssns, Map<Long, String> eIssns,
      Map<Long, String> pIsbns, Map<Long, String> eIsbns) throws Exception {

    for (Long publicationSeq : journals) {
      // Exact match.
      Long matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_JOURNAL);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with same ISSNs and no name.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), null,
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_JOURNAL);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with same ISSNs and and alternate name.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), "Alternate Name",
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_JOURNAL);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with reversed print and electronic ISSNs.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  eIssns.get(publicationSeq), pIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_JOURNAL);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with no print ISSN.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  null, pIssns.get(publicationSeq), pIsbns.get(publicationSeq),
	  eIsbns.get(publicationSeq), MD_ITEM_TYPE_JOURNAL);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with no electronic ISSN.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  eIssns.get(publicationSeq), null, pIsbns.get(publicationSeq),
	  eIsbns.get(publicationSeq), MD_ITEM_TYPE_JOURNAL);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match by name and no ISSNs.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  null, null, pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_JOURNAL);

      assertEquals(publicationSeq, matchedPublicationSeq);

      boolean existingHasIssns = pIssns.get(publicationSeq) != null
	  || eIssns.get(publicationSeq) != null;

      // No match for new print ISSN when the existing one has an ISSN even if
      // the name matches, unless the electronic ISSN matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  "12345678", eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_JOURNAL);

      if (existingHasIssns && eIssns.get(publicationSeq) == null) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new electronic ISSN when the existing one has an ISSN even
      // if the name matches, unless the print ISSN matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), "98765432",
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_JOURNAL);

      if (existingHasIssns && pIssns.get(publicationSeq) == null) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new print ISSN when the existing one has an ISSN even if
      // the name matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  "12345678", null, pIsbns.get(publicationSeq),
	  eIsbns.get(publicationSeq), MD_ITEM_TYPE_JOURNAL);

      if (existingHasIssns) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new electronic ISBN when the existing one has an ISBN even
      // if the name matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null,
	  "98765432", pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_JOURNAL);

      if (existingHasIssns) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for different publication type.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK);

      assertNull(matchedPublicationSeq);
    }
  }

  private void runTestFindBook(Connection conn, List<Long> books,
      Map<Long, Long> publishers, Map<Long, String> names,
      Map<Long, String> pIssns, Map<Long, String> eIssns,
      Map<Long, String> pIsbns, Map<Long, String> eIsbns) throws Exception {

    for (Long publicationSeq : books) {
      // Exact match.
      Long matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with same ISBNs and no name.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), null,
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with same ISBNs and and alternate name.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), "Alternate Name",
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with reversed print and electronic ISBNs.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  eIsbns.get(publicationSeq), pIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with no print ISBN.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  null, pIsbns.get(publicationSeq), MD_ITEM_TYPE_BOOK);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with no electronic ISBN.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  eIsbns.get(publicationSeq), null, MD_ITEM_TYPE_BOOK);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match by name and no ISBNs.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  null, null, MD_ITEM_TYPE_BOOK);

      assertEquals(publicationSeq, matchedPublicationSeq);

      boolean existingHasIsbns = pIsbns.get(publicationSeq) != null
	  || eIsbns.get(publicationSeq) != null;

      // No match for new print ISBN when the existing one has an ISBN even if
      // the name matches, unless the electronic ISBN matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  "9876543210987", eIsbns.get(publicationSeq), MD_ITEM_TYPE_BOOK);

      if (existingHasIsbns && eIsbns.get(publicationSeq) == null) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new electronic ISBN when the existing one has an ISBN even
      // if the name matches, unless the print ISBN matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), "9876543210987", MD_ITEM_TYPE_BOOK);

      if (existingHasIsbns && pIsbns.get(publicationSeq) == null) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new print ISBN when the existing one has an ISBN even if
      // the name matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  "9876543210987", null, MD_ITEM_TYPE_BOOK);

      if (existingHasIsbns) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new electronic ISBN when the existing one has an ISBN even
      // if the name matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  null, "9876543210987", MD_ITEM_TYPE_BOOK);

      if (existingHasIsbns) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for different publication type.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_JOURNAL);

      assertNull(matchedPublicationSeq);
    }
  }

  private void runTestFindBookSeries(Connection conn, List<Long> journals,
      Map<Long, Long> mdItems, Map<Long, Long> publishers,
      Map<Long, String> names, Map<Long, String> pIssns,
      Map<Long, String> eIssns, Map<Long, String> pIsbns,
      Map<Long, String> eIsbns) throws Exception {

    for (Long publicationSeq : journals) {
      metadataManagerSql.addMdItemIsbns(conn, mdItems.get(publicationSeq),
	  "9781585623174", "9781585623177");
      pIsbns.put(publicationSeq, "9781585623174");
      eIsbns.put(publicationSeq, "9781585623177");

      String query = "update " + MD_ITEM_TABLE
  	+ " set " + MD_ITEM_TYPE_SEQ_COLUMN + " = 1"
  	+ " where " + MD_ITEM_SEQ_COLUMN + " = " + mdItems.get(publicationSeq);

      PreparedStatement stmt = dbManager.prepareStatement(conn, query);
      dbManager.executeUpdate(stmt);
    }

    for (Long publicationSeq : journals) {
      // Exact match.
      Long matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with same ISSNs and ISBNs and no name.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), null,
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with same ISSNs and no ISBNs and no name.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), null,
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq), null,null,
	  MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with same ISBNs and no ISSNs and no name.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), null, null,null,
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with same ISSNs and ISBNs and and alternate name.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), "Alternate Name",
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with same ISSNs and no ISBNs and no name.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), "Alternate Name",
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq), null,null,
	  MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with same ISBNs and no ISSNs and no name.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), "Alternate Name", null,null,
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with reversed print and electronic ISSNs.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  eIssns.get(publicationSeq), pIssns.get(publicationSeq), null, null,
	  MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with no print ISSN.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null,
	  eIssns.get(publicationSeq), null, null, MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with no electronic ISSN.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  eIssns.get(publicationSeq), null, null, null,
	  MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with reversed print and electronic ISBNs.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null, null,
	  eIsbns.get(publicationSeq), pIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with no print ISBN.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null, null,
	  null, eIsbns.get(publicationSeq), MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match with no electronic ISSN.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null, null,
	  pIsbns.get(publicationSeq), null, MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      // Match by name and no ISBNs and no ISSNs.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null, null,
	  null, null, MD_ITEM_TYPE_BOOK_SERIES);

      assertEquals(publicationSeq, matchedPublicationSeq);

      boolean existingHasIssns = pIssns.get(publicationSeq) != null
	  || eIssns.get(publicationSeq) != null;

      // No match for new print ISSN when the existing one has an ISSN even if
      // the name matches, unless the electronic ISSN matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  "12345678", eIssns.get(publicationSeq), null, null,
	  MD_ITEM_TYPE_BOOK_SERIES);

      if (existingHasIssns && eIssns.get(publicationSeq) == null) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new electronic ISSN when the existing one has an ISSN even
      // if the name matches, unless the print ISSN matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), "98765432", null, null,
	  MD_ITEM_TYPE_BOOK_SERIES);

      if (existingHasIssns && pIssns.get(publicationSeq) == null) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new print ISSN when the existing one has an ISSN even if
      // the name matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  "12345678", null, null, null, MD_ITEM_TYPE_BOOK_SERIES);

      if (existingHasIssns) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new electronic ISSN when the existing one has an ISSN even
      // if the name matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null,
	  "98765432", null, null, MD_ITEM_TYPE_BOOK_SERIES);

      if (existingHasIssns) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      boolean existingHasIsbns = pIsbns.get(publicationSeq) != null
	  || eIsbns.get(publicationSeq) != null;

      // No match for new print ISBN when the existing one has an ISBN even if
      // the name matches, unless the electronic ISBN matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null, null,
	  "9876543210987", eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK_SERIES);

      if (existingHasIsbns && eIsbns.get(publicationSeq) == null) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new electronic ISBN when the existing one has an ISBN even
      // if the name matches, unless the print ISBN matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null, null,
	  pIsbns.get(publicationSeq), "1234567890123",
	  MD_ITEM_TYPE_BOOK_SERIES);

      if (existingHasIsbns && pIsbns.get(publicationSeq) == null) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new print ISBN when the existing one has an ISBN even if
      // the name matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null, null,
	  "9876543210987", null, MD_ITEM_TYPE_BOOK_SERIES);

      if (existingHasIsbns) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for new electronic ISBN when the existing one has an ISBN even
      // if the name matches.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq), null, null,
	  null, "1234567890123", MD_ITEM_TYPE_BOOK_SERIES);

      if (existingHasIsbns) {
	assertNull(matchedPublicationSeq);
      } else {
	assertEquals(publicationSeq, matchedPublicationSeq);
      }

      // No match for different publication type.
      matchedPublicationSeq = metadataManager.findPublication(conn,
	  publishers.get(publicationSeq), names.get(publicationSeq),
	  pIssns.get(publicationSeq), eIssns.get(publicationSeq),
	  pIsbns.get(publicationSeq), eIsbns.get(publicationSeq),
	  MD_ITEM_TYPE_BOOK);

      assertNull(matchedPublicationSeq);
    }
  }

  private void runTestGetIndexTypeDisplayString() throws Exception {
    MetadataManagerStatusAccessor mmsa =
	new MetadataManagerStatusAccessor(metadataManager);

    PrioritizedAuId pAuId = new PrioritizedAuId();
    assertEquals(REINDEX_TEXT, mmsa.getIndexTypeDisplayString(pAuId));
    pAuId.needFullReindex = true;
    assertEquals(FULL_REINDEX_TEXT, mmsa.getIndexTypeDisplayString(pAuId));
    pAuId.isNew = true;
    assertEquals(NEW_INDEX_TEXT, mmsa.getIndexTypeDisplayString(pAuId));
  }
  
  private void runRemoveChildMetadataItemTest() throws Exception {
    Connection conn = dbManager.getConnection();
    
    // Get the existing AU child metadata items.
    String query = "select " + AU_MD_SEQ_COLUMN + "," + MD_ITEM_SEQ_COLUMN
	+ " from " + MD_ITEM_TABLE
        + " where " + PARENT_SEQ_COLUMN + " is not null"
        + " and " + AU_MD_SEQ_COLUMN + " is not null"
        + " and " + MD_ITEM_SEQ_COLUMN + " is not null";

    PreparedStatement stmt = dbManager.prepareStatement(conn, query);
    ResultSet resultSet = dbManager.executeQuery(stmt);
    Map<Long, Long> results = new HashMap<Long, Long>();

    while (resultSet.next()) {
      results.put(resultSet.getLong(2), resultSet.getLong(1));
    }

    assertEquals(84, results.size());

    // Delete them one by one.
    for (Long mdItemSeq : results.keySet()) {
      assertEquals(1, metadataManagerSql.removeAuChildMetadataItem(conn,
	  results.get(mdItemSeq), mdItemSeq));
    }

    assertFalse(dbManager.executeQuery(stmt).next());
    
    // Get the all the remaining metadata items.
    query = "select " + AU_MD_SEQ_COLUMN + "," + MD_ITEM_SEQ_COLUMN
	+ " from " + MD_ITEM_TABLE;

    stmt = dbManager.prepareStatement(conn, query);
    resultSet = dbManager.executeQuery(stmt);
    results = new HashMap<Long, Long>();

    while (resultSet.next()) {
      results.put(resultSet.getLong(2), resultSet.getLong(1));
    }

    assertEquals(4, results.size());

    // Fail to delete them because they are not children of an AU.
    for (Long mdItemSeq : results.keySet()) {
      assertEquals(0, metadataManagerSql.removeAuChildMetadataItem(conn,
	  results.get(mdItemSeq), mdItemSeq));
    }

    DbManager.safeRollbackAndClose(conn);
  }

  private void runMetadataMonitorTest() throws Exception {
    assertEquals(4, metadataManager.getPublisherNames().size());
    assertEquals(0,
	metadataManager.getPublishersWithMultipleDoiPrefixes().size());
    assertEquals(0,
	metadataManager.getDoiPrefixesWithMultiplePublishers().size());
    assertEquals(0, metadataManager.getAuNamesWithMultipleDoiPrefixes().size());
    assertEquals(0, metadataManager.getPublicationsWithMoreThan2Isbns().size());
    assertEquals(0, metadataManager.getPublicationsWithMoreThan2Issns().size());
    assertEquals(0, metadataManager.getIsbnsWithMultiplePublications().size());
    assertEquals(0, metadataManager.getIssnsWithMultiplePublications().size());
    assertEquals(0, metadataManager.getBooksWithIssns().size());
    assertEquals(0, metadataManager.getPeriodicalsWithIsbns().size());
    assertEquals(0, metadataManager.getUnknownProviderAuIds().size());
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
    int version = 2;
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

    @Override
    public String getFeatureVersion(Plugin.Feature feat) {
      if (Feature.Metadata == feat) {
	// Increment the version on every call to delete old metadata before
	// storing new metadata.
	return feat + "_" + version++;
      } else {
	return null;
      }
    }
  }

  public static class MySimulatedPlugin0 extends MySimulatedPlugin {
    public MySimulatedPlugin0() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        int articleNumber = 0;
        public void extract(MetadataTarget target, ArticleFiles af,
            Emitter emitter) throws IOException, PluginException {
          ArticleMetadata md = new ArticleMetadata();
          articleNumber++;

          // use provider based on au number from last digit of auid: 0 or 4
          String auid = af.getFullTextCu().getArchivalUnit().getAuId();
          String auNumber = auid.substring(auid.length()-1);
          md.put(MetadataField.FIELD_PROVIDER, "Provider "+auNumber);

          md.put(MetadataField.FIELD_PUBLISHER,"Publisher 0");
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
          md.put(MetadataField.FIELD_PUBLICATION_TITLE,
                 "Journal[" + doiPrefix + "]");
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
        public void extract(MetadataTarget target, ArticleFiles af,
            Emitter emitter) throws IOException, PluginException {
          articleNumber++;
          ArticleMetadata md = new ArticleMetadata();
          md.put(MetadataField.FIELD_PUBLISHER,"Publisher One");
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
          md.put(MetadataField.FIELD_PUBLICATION_TITLE,
                 "Journal[" + doiPrefix + "]");
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
        public void extract(MetadataTarget target, ArticleFiles af,
            Emitter emitter) throws IOException, PluginException {
          org.lockss.extractor.ArticleMetadata md = new ArticleMetadata();
          articleNumber++;
          md.put(MetadataField.FIELD_PUBLISHER,"Publisher Dos");
          String doi = "10.1357/9781585623174." + articleNumber; 
          md.put(MetadataField.FIELD_DOI,doi);
          md.put(MetadataField.FIELD_ISBN,"978-1-58562-317-4");
          md.put(MetadataField.FIELD_DATE,"1993");
          md.put(MetadataField.FIELD_START_PAGE,"" + articleNumber);
          md.put(MetadataField.FIELD_PUBLICATION_TITLE,
              "Manual of Clinical Psychopharmacology");
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
        public void extract(MetadataTarget target, ArticleFiles af,
            Emitter emitter) throws IOException, PluginException {
          ArticleMetadata md = new ArticleMetadata();
          articleNumber++;
          md.put(MetadataField.FIELD_PUBLISHER,"Publisher Trois");
          String doiPrefix = "10.0135/12345678.1999-11.12";
          String doi = doiPrefix + "." + articleNumber; 
          md.put(MetadataField.FIELD_DOI,doi);
          md.put(MetadataField.FIELD_ISBN,"976-1-58562-317-7");
          md.put(MetadataField.FIELD_DATE,"1999");
          md.put(MetadataField.FIELD_START_PAGE,"" + articleNumber);
          md.put(MetadataField.FIELD_PUBLICATION_TITLE,
                 "Journal[" + doiPrefix + "]");
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
