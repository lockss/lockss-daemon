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
package org.lockss.exporter.counter;

import static org.lockss.db.SqlConstants.*;
import static org.lockss.plugin.ArticleFiles.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.lockss.daemon.Cron;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;

/**
 * Test class for org.lockss.exporter.counter.CounterReportsRequestRecorder.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
public class TestCounterReportsRequestRecorder extends LockssTestCase {
  // A URL that exists in the URL metadata table.
  private static final String RECORDABLE_URL =
      "http://example.com/fulltext.url";

  // A URL that does not exist in the URL metadata table.
  private static final String IGNORABLE_URL = "http://example.com/index.html";

  // Query to count all the rows of requests.
  private static final String SQL_QUERY_REQUEST_COUNT = "select count(*) from "
      + COUNTER_REQUEST_TABLE;

  // Query to count all the rows of requests for a given publisher involvement.
  private static final String SQL_QUERY_REQUEST_BY_INVOLVEMENT_COUNT = "select "
      + "count(*) from " + COUNTER_REQUEST_TABLE
      + " where " + IS_PUBLISHER_INVOLVED_COLUMN + " = ?";

  private DbManager dbManager;
  private MetadataManager metadataManager;
  private CounterReportsManager counterReportsManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    ConfigurationUtil.addFromArgs(DbManager.PARAM_DATASOURCE_CLASSNAME,
	"org.apache.derby.jdbc.ClientDataSource");
    ConfigurationUtil.addFromArgs(DbManager.PARAM_DATASOURCE_PASSWORD,
	"somePassword");
    ConfigurationUtil.addFromArgs(CounterReportsManager.PARAM_COUNTER_ENABLED,
	"true");
    ConfigurationUtil.addFromArgs(CounterReportsManager
	.PARAM_REPORT_BASEDIR_PATH, tempDirPath);
    ConfigurationUtil.addFromArgs(CounterReportsRequestAggregator
        .PARAM_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY, "hourly");

    MockLockssDaemon theDaemon = getMockLockssDaemon();
    theDaemon.setDaemonInited(true);

    dbManager = getTestDbManager(tempDirPath);

    metadataManager = new MetadataManager();
    theDaemon.setMetadataManager(metadataManager);
    metadataManager.initService(theDaemon);
    metadataManager.startService();

    Cron cron = new Cron();
    theDaemon.setCron(cron);
    cron.initService(theDaemon);
    cron.startService();

    counterReportsManager = new CounterReportsManager();
    theDaemon.setCounterReportsManager(counterReportsManager);
    counterReportsManager.initService(theDaemon);
    counterReportsManager.startService();

    initializeMetadata();
  }

  private void initializeMetadata() throws DbException {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Add the publisher.
      Long publisherSeq =
	  metadataManager.findOrCreatePublisher(conn, "publisher");

      // Add the publication.
      Long publicationSeq =
	  metadataManager.findOrCreateBook(conn, publisherSeq, null,
		"9876543210987", "9876543210123", "The Full Book", null);

      // Add the publishing platform.
      Long platformSeq = metadataManager.findOrCreatePlatform(conn,
	  "fullPlatform");

      // Add the plugin.
      Long pluginSeq = metadataManager.findOrCreatePlugin(conn, "fullPluginId",
	  platformSeq, false);

      // Add the AU.
      Long auSeq = metadataManager.findOrCreateAu(conn, pluginSeq, "fullAuKey");

      // Add the provider.
      Long providerSeq = dbManager.findOrCreateProvider(conn, "fullProviderId",
	  "fullProviderName");

      // Add the AU metadata.
      Long auMdSeq =
	  metadataManager.addAuMd(conn, auSeq, 1, 0L, 123L, providerSeq);

      Long parentSeq =
	  metadataManager.findPublicationMetadataItem(conn, publicationSeq);

      metadataManager.addMdItemDoi(conn, parentSeq, "10.1000/182");

      Long mdItemTypeSeq =
	  metadataManager.findMetadataItemType(conn, MD_ITEM_TYPE_BOOK);

      Long mdItemSeq = metadataManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
                                                 auMdSeq, "2009-01-01", null,
                                                 1234L);

      metadataManager.addMdItemName(conn, mdItemSeq, "TOC", PRIMARY_NAME_TYPE);

      metadataManager.addMdItemUrl(conn, mdItemSeq, "", IGNORABLE_URL);

      mdItemSeq = metadataManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
                                            auMdSeq, "2009-01-01", null, 1234L);

      metadataManager.addMdItemName(conn, mdItemSeq, "The Full Book",
	  			    PRIMARY_NAME_TYPE);

      metadataManager.addMdItemUrl(conn, mdItemSeq, ROLE_FULL_TEXT_HTML,
                                   RECORDABLE_URL);
    } finally {
      DbManager.commitOrRollback(conn, log);
      DbManager.safeCloseConnection(conn);
    }
  }

  /**
   * Tests the recording of multiple requests.
   * 
   * @throws Exception
   */
  public void testRecordMultipleRequests() throws Exception {

    CounterReportsRequestRecorder recorder =
	CounterReportsRequestRecorder.getInstance();

    recorder.recordRequest(IGNORABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkRequestRowCount(0);

    recorder.recordRequest(RECORDABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkRequestRowCount(1);
    checkRequestByPublisherInvolvementRowCount(false, 1);
    checkRequestByPublisherInvolvementRowCount(true, 0);

    recorder.recordRequest(IGNORABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkRequestRowCount(1);
    checkRequestByPublisherInvolvementRowCount(false, 1);
    checkRequestByPublisherInvolvementRowCount(true, 0);

    recorder.recordRequest(RECORDABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkRequestRowCount(2);
    checkRequestByPublisherInvolvementRowCount(false, 1);
    checkRequestByPublisherInvolvementRowCount(true, 1);

    recorder.recordRequest(IGNORABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 304);

    checkRequestRowCount(2);
    checkRequestByPublisherInvolvementRowCount(false, 1);
    checkRequestByPublisherInvolvementRowCount(true, 1);

    recorder.recordRequest(RECORDABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 304);

    checkRequestRowCount(3);
    checkRequestByPublisherInvolvementRowCount(false, 2);
    checkRequestByPublisherInvolvementRowCount(true, 1);

    recorder.recordRequest(IGNORABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 304);

    checkRequestRowCount(3);
    checkRequestByPublisherInvolvementRowCount(false, 2);
    checkRequestByPublisherInvolvementRowCount(true, 1);

    recorder.recordRequest(RECORDABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 304);

    checkRequestRowCount(4);
    checkRequestByPublisherInvolvementRowCount(false, 2);
    checkRequestByPublisherInvolvementRowCount(true, 2);

    recorder.recordRequest(IGNORABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 404);

    checkRequestRowCount(4);
    checkRequestByPublisherInvolvementRowCount(false, 2);
    checkRequestByPublisherInvolvementRowCount(true, 2);

    recorder.recordRequest(RECORDABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 404);

    checkRequestRowCount(5);
    checkRequestByPublisherInvolvementRowCount(false, 3);
    checkRequestByPublisherInvolvementRowCount(true, 2);

    recorder.recordRequest(IGNORABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 404);

    checkRequestRowCount(5);
    checkRequestByPublisherInvolvementRowCount(false, 3);
    checkRequestByPublisherInvolvementRowCount(true, 2);

    recorder.recordRequest(RECORDABLE_URL,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 404);

    checkRequestRowCount(6);
    checkRequestByPublisherInvolvementRowCount(false, 4);
    checkRequestByPublisherInvolvementRowCount(true, 2);
  }

  /**
   * Checks the expected count of rows in the request table.
   * 
   * @param expected
   *          An int with the expected number of rows in the table.
   * @throws SQLException, DbException
   */
  private void checkRequestRowCount(int expected)
      throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_REQUEST_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }

    assertEquals(expected, count);
  }

  /**
   * Checks the expected count of rows in the request table for a given
   * publisher involvement.
   * 
   * @param isPublisherInvolved
   *          A boolean with the indication of whether the publisher is
   *          involved.
   * @param expected
   *          An int with the expected number of rows in the table.
   * @throws SQLException, DbException
   */
  private void checkRequestByPublisherInvolvementRowCount(
      boolean isPublisherInvolved, int expected)
	  throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_REQUEST_BY_INVOLVEMENT_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      statement.setBoolean(1, isPublisherInvolved);
      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }

    assertEquals(expected, count);
  }
}
