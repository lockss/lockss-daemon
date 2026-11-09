/*

 Copyright (c) 2026 Board of Trustees of Leland Stanford Jr. University,
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.lockss.alert.Alert;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.db.DbManager;
import org.lockss.extractor.MetadataTarget;
import org.lockss.metadata.MetadataManager.ReindexingStatus;
import org.lockss.metadata.TestMetadataManager.MySimulatedPlugin0;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.*;
import org.lockss.util.Logger;

/**
 * Verifies that the ArticleCountChanged alert is raised by the commit path of
 * a metadata reindexing task.
 */
public class TestArticleCountChangedAlert extends LockssTestCase {
  private static final Logger log =
      Logger.getLogger(TestArticleCountChangedAlert.class);

  private static final long AU_ARTICLE_COUNT = 21;

  private MockLockssDaemon theDaemon;
  private MockAlertManager alertManager;
  private SimulatedArchivalUnit sau0;
  private MetadataManager metadataManager;
  private MetadataManagerSql metadataManagerSql;
  private DbManager dbManager;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    // Tasks are run directly by the test, not by the metadata manager.
    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEXING_ENABLED,
	"false");

    theDaemon = getMockLockssDaemon();
    alertManager = new MockAlertManager();
    theDaemon.setAlertManager(alertManager);

    PluginManager pluginManager = theDaemon.getPluginManager();
    pluginManager.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginManager.startService();
    theDaemon.getCrawlManager();

    sau0 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin0.class,
	simAuConfig(tempDirPath + "/0"));
    PluginTestUtil.crawlSimAu(sau0);

    dbManager = getTestDbManager(tempDirPath);

    metadataManager = new MetadataManager();
    theDaemon.setMetadataManager(metadataManager);
    metadataManager.initService(theDaemon);
    metadataManager.startService();
    metadataManagerSql = metadataManager.getMetadataManagerSql();

    theDaemon.setAusStarted(true);
  }

  public void tearDown() throws Exception {
    sau0.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  private Configuration simAuConfig(String rootPath) {
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

  /**
   * Runs the scenarios in sequence, since each builds on the database state
   * left by the previous one.
   */
  public void testAll() throws Exception {
    runFirstIndexRaisesNoAlert();
    runUnchangedCountRaisesNoAlert();
    runChangedCountRaisesAlert();
  }

  /**
   * Full indexing of an AU with no prior metadata does not raise the alert,
   * even when the task is not marked as being for a new AU.
   */
  private void runFirstIndexRaisesNoAlert() throws Exception {
    ReindexingTask task = runFullReindexingTask();

    assertEquals(AU_ARTICLE_COUNT, task.getUpdatedArticleCount());
    assertEmpty(articleCountChangedAlerts());
  }

  private void runUnchangedCountRaisesNoAlert() throws Exception {
    ReindexingTask task = runFullReindexingTask();

    assertEquals(AU_ARTICLE_COUNT, task.getUpdatedArticleCount());
    assertEmpty(articleCountChangedAlerts());
  }

  private void runChangedCountRaisesAlert() throws Exception {
    removeOneArticle();

    ReindexingTask task = runFullReindexingTask();

    assertEquals(AU_ARTICLE_COUNT, task.getUpdatedArticleCount());
    List<Alert> alerts = articleCountChangedAlerts();
    assertEquals(1, alerts.size());

    Alert alert = alerts.get(0);
    assertEquals(sau0.getAuId(), alert.getString(Alert.ATTR_AUID));
    assertEquals(sau0.getName(), alert.getString(Alert.ATTR_AU_NAME));
    assertEquals(Alert.SEVERITY_WARNING, alert.getInt(Alert.ATTR_SEVERITY));
    assertEquals("Full metadata reindex changed the article count: "
	+ (AU_ARTICLE_COUNT - 1) + " before, " + AU_ARTICLE_COUNT + " after.",
	alert.getString(Alert.ATTR_TEXT));
  }

  private ReindexingTask runFullReindexingTask() {
    ReindexingTask task = new ReindexingTask(sau0, sau0.getPlugin()
	.getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));
    task.setWDog(new MockLockssWatchdog());
    task.setNewAu(false);
    task.setFullReindex(true);
    task.reindex();

    assertEquals(ReindexingStatus.Success, task.getReindexingStatus());
    return task;
  }

  private void removeOneArticle() throws Exception {
    Connection conn = dbManager.getConnection();

    try {
      String query = "select " + AU_MD_SEQ_COLUMN + "," + MD_ITEM_SEQ_COLUMN
	  + " from " + MD_ITEM_TABLE
	  + " where " + AU_MD_SEQ_COLUMN + " is not null";
      PreparedStatement stmt = dbManager.prepareStatement(conn, query);
      ResultSet resultSet = dbManager.executeQuery(stmt);
      assertTrue(resultSet.next());
      long auMdSeq = resultSet.getLong(1);
      long mdItemSeq = resultSet.getLong(2);
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);

      assertEquals(1, metadataManagerSql.removeAuChildMetadataItem(conn,
	  auMdSeq, mdItemSeq));
      DbManager.commitOrRollback(conn, log);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  private List<Alert> articleCountChangedAlerts() {
    List<Alert> res = new ArrayList<Alert>();
    for (Alert alert : alertManager.getAlerts()) {
      if ("ArticleCountChanged".equals(alert.getName())) {
	res.add(alert);
      }
    }
    return res;
  }
}
