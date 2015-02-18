/*
 * $Id$
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.app.LockssDaemon;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.util.Logger;
import org.mortbay.http.HttpResponse;

/**
 * Persists the request data necessary to create COUNTER reports.
 * 
 * @version 1.0
 */
public class CounterReportsRequestRecorder {
  private static final Logger log = Logger
      .getLogger(CounterReportsRequestRecorder.class);

  private static final String SQL_QUERY_MD_ITEM_ID_FROM_URL = "select "
      + MD_ITEM_SEQ_COLUMN
      + " from " + URL_TABLE
      + " where " + URL_COLUMN + " = ?"
      + " and (" + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_HTML
      + "' or " + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_PDF
      + "')";

  // The singleton instance of this class.
  private static final CounterReportsRequestRecorder instance =
      new CounterReportsRequestRecorder();

  /**
   * The indication of whether the publisher was contacted or not.
   */
  public static enum PublisherContacted {
    TRUE, FALSE
  }

  /**
   * Constructor.
   * 
   * It is private so it cannot be instantiated by client code.
   */
  private CounterReportsRequestRecorder() {
  }

  /**
   * Provides the singleton instance.
   * 
   * @return a CounterReportsRequestRecorder with the singleton instance.
   */
  public static CounterReportsRequestRecorder getInstance() {
    return instance;
  }

  /**
   * Records a request that is the subject of a report.
   * 
   * @param url
   *          A String with the URL being requested.
   * @param contacted
   *          A PublisherContacted with the indication of whether the publisher
   *          was contacted while processing the request.
   * @param publisherCode
   *          An int with the publisher involvement code.
   */
  public void recordRequest(String url, PublisherContacted contacted,
      int publisherCode) {
    try {
      final String DEBUG_HEADER = "recordRequest(): ";
      CounterReportsManager counterReportsManager =
	  LockssDaemon.getLockssDaemon().getCounterReportsManager();
      
      // Check whether the COUNTER reports manager is disabled.
      if (!counterReportsManager.isReady()) {
	// Yes: Do nothing.
	log.debug2(DEBUG_HEADER + "Done: COUNTER reports manager is disabled.");
	return;
      }

      // No: Get the metadata identifier of the URL.
      log.debug2(DEBUG_HEADER + "url = '" + url + "'.");
      Long mdItemId = findMatchingFullTextMdItemId(url);

      // Do nothing more if it is not a request needed for any report.
      if (mdItemId == null) {
	return;
      }

      // Get an indication of whether the publisher is involved in serving the
      // content.
      log.debug2("publisherCode = " + publisherCode);
      boolean isPublisherInvolved = contacted == PublisherContacted.TRUE
	  && (publisherCode == HttpResponse.__200_OK
	      || publisherCode == HttpResponse.__304_Not_Modified);
      log.debug2("isPublisherInvolved = " + isPublisherInvolved);

      // Persist the request data.
      counterReportsManager.persistRequest(url, isPublisherInvolved);
      log.debug2(DEBUG_HEADER + "Done.");
    } catch (DbException sqle) {
      log.error("Cannot persist request - Statistics not collected", sqle);
    }
  }

  /**
   * Provides the metadata item identifier that corresponds to a full-text URL.
   * 
   * @param url
   *          A String with the URL.
   * @return a Long with the metadata item identifier, if any.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private Long findMatchingFullTextMdItemId(String url) throws DbException {
    final String DEBUG_HEADER = "findMatchingFullTextMdItemId(): ";

    Connection conn = null;
    PreparedStatement getUrlMdItemId = null;
    ResultSet results = null;
    Long mdItemId = null;
    DbManager dbManager = LockssDaemon.getLockssDaemon().getDbManager();

    try {
      // Get the database connection.
      conn = dbManager.getConnection();

      // Prepare the query.
      getUrlMdItemId =
	  dbManager.prepareStatement(conn, SQL_QUERY_MD_ITEM_ID_FROM_URL);
      getUrlMdItemId.setString(1, url);

      // Get any results.
      results = dbManager.executeQuery(getUrlMdItemId);

      // Get the metadata item identifier.
      if (results.next()) {
	mdItemId = results.getLong(MD_ITEM_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      throw new DbException(
	  "Cannot find full-text URL metadata item identifier", sqle);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(getUrlMdItemId);
      DbManager.safeRollbackAndClose(conn);
    }

    log.debug2(DEBUG_HEADER + "mdItemId = '" + mdItemId + "'.");
    return mdItemId;
  }
}
