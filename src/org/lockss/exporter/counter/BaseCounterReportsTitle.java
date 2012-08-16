/*
 * $Id: BaseCounterReportsTitle.java,v 1.1 2012-08-16 22:19:14 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * A title used in COUNTER reports.
 * 
 * @version 1.0
 * 
 */
package org.lockss.exporter.counter;

import static org.lockss.exporter.counter.CounterReportsManager.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import org.lockss.app.LockssDaemon;
import org.lockss.db.DbManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;

public abstract class BaseCounterReportsTitle implements CounterReportsTitle {
  private static final Logger log = Logger.getLogger("BaseCounterReportsTitle");

  public static final String IS_HTML_KEY = "IS_HTML_KEY";
  public static final String IS_PDF_KEY = "IS_PDF_KEY";
  public static final String IS_PUBLISHER_INVOLVED_KEY =
      "IS_PUBLISHER_INVOLVED_KEY";
  public static final String IS_SECTION_KEY = "IS_SECTION_KEY";
  public static final String PUBLICATION_YEAR_KEY = "PUBYEAR_KEY";

  // Query to insert titles used for COUNTER reports.
  protected static final String SQL_QUERY_TITLE_INSERT = "insert into "
      + SQL_TABLE_TITLES + " (" + SQL_COLUMN_LOCKSS_ID + ","
      + SQL_COLUMN_TITLE_NAME + "," + SQL_COLUMN_PUBLISHER_NAME + ","
      + SQL_COLUMN_PLATFORM_NAME + "," + SQL_COLUMN_DOI + ","
      + SQL_COLUMN_PROPRIETARY_ID + "," + SQL_COLUMN_IS_BOOK + ","
      + SQL_COLUMN_PRINT_ISSN + "," + SQL_COLUMN_ONLINE_ISSN + ","
      + SQL_COLUMN_ISBN + "," + SQL_COLUMN_BOOK_ISSN
      + ") values (?,?,?,?,?,?,?,?,?,?,?)";

  // Query to insert title requests used for COUNTER reports.
  private static final String SQL_QUERY_TITLE_REQUEST_INSERT = "insert into "
      + SQL_TABLE_REQUESTS + " (" + SQL_COLUMN_LOCKSS_ID + ","
      + SQL_COLUMN_PUBLICATION_YEAR + "," + SQL_COLUMN_IS_HTML + ","
      + SQL_COLUMN_IS_PDF + "," + SQL_COLUMN_IS_SECTION + ","
      + SQL_COLUMN_IS_PUBLISHER_INVOLVED + "," + SQL_COLUMN_REQUEST_YEAR + ","
      + SQL_COLUMN_REQUEST_MONTH + "," + SQL_COLUMN_REQUEST_DAY
      + ") values (?,?,?,?,?,?,?,?,?)";

  // Query to get the name of a title by its LOCKSS identifier.
  private static final String SQL_QUERY_TITLE_NAME_BY_LOCKSS_ID_SELECT =
      "select " + SQL_COLUMN_TITLE_NAME + " from " + SQL_TABLE_TITLES
	  + " where " + SQL_COLUMN_LOCKSS_ID + " = ?";

  /**
   * The LOCKSS identifier of the title.
   */
  protected long lockssId;

  // The DOI of the title.
  private final String doi;

  // An indication of whether the LOCKSS identifier has been computed.
  private boolean identified = false;

  // The name of the title.
  private final String name;

  // The name of the publisher of the title.
  private final String publisherName;

  // The name of the publishing platform.
  private final String publishingPlatform;

  // The proprietary identifier.
  private final String proprietaryId;

  // The ISBN of a book.
  protected String isbn = null;

  // The ISSN of a book.
  protected String issn = null;

  // The online ISSN of a journal.
  protected String onlineIssn = null;

  // The print ISSN of a journal.
  protected String printIssn = null;

  /**
   * Constructor.
   * 
   * @param name
   *          A String with the name of the title.
   * @param publisherName
   *          A String with the name of the publisher of the title.
   * @param publishingPlatform
   *          A String with the name of the publishing platform.
   * @param doi
   *          A String with the DOI of the title.
   * @param proprietaryId
   *          A String with the proprietary identifier.
   * @throws IllegalArgumentException
   *           if the name of the title is empty.
   */
  protected BaseCounterReportsTitle(String name, String publisherName,
      String publishingPlatform, String doi, String proprietaryId)
      throws IllegalArgumentException {
    if (name == null || name.trim().length() == 0) {
      throw new IllegalArgumentException("Name cannot be empty.");
    }

    this.name = name;
    this.publisherName = publisherName;
    this.publishingPlatform = publishingPlatform;
    this.doi = doi;
    this.proprietaryId = proprietaryId;
  }

  /**
   * Provides the DOI of the title.
   * 
   * @return a String with the DOI of the title.
   */
  @Override
  public String getDoi() {
    return doi;
  }

  /**
   * Provides the ISBN of the title if it is a book.
   * 
   * @return a String with the ISBN of the title.
   */
  @Override
  public String getIsbn() {
    return isbn;
  }

  /**
   * Provides the ISSN of the title if it is a book.
   * 
   * @return a String with the ISSN of the title.
   */
  @Override
  public String getIssn() {
    return issn;
  }

  /**
   * Provides the LOCKSS identifier of the title.
   * 
   * @return a long with the LOCKSS identifier of the title.
   */
  @Override
  public long getLockssId() {
    if (identified) {
      return lockssId;
    }

    throw new RuntimeException(
			       "The identifier of the title has not been initialized.");
  }

  public abstract String getLockssIdPayload() throws SQLException;

  /**
   * Provides the name of the title.
   * 
   * @return a String with the name of the title.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Provides the name of a title with a given LOCKSS identifier.
   * 
   * @param conn
   *          A Connection representing the database connection.
   * @return The name of the title or <code>null</code> if the title with the
   *         LOCKSS identifier does not exist in the database.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  protected String getNameByLockssId(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "getNameByLockssId(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String sql = SQL_QUERY_TITLE_NAME_BY_LOCKSS_ID_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");
    String name = null;

    try {
      // Prepare the statement used to get the title name.
      statement = conn.prepareStatement(sql);

      // Populate the LOCKSS identifier.
      statement.setLong(1, lockssId);

      // Get the name.
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	name = resultSet.getString(SQL_COLUMN_TITLE_NAME);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the title name by LOCKSS identifier", sqle);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "name = '" + name + "'.");
    return name;
  }

  /**
   * Provides the online ISSN of the title if it is a journal.
   * 
   * @return a String with the online ISSN of the title.
   */
  @Override
  public String getOnlineIssn() {
    return onlineIssn;
  }

  /**
   * Provides the print ISSN of the title if it is a journal.
   * 
   * @return a String with the print ISSN of the title.
   */
  @Override
  public String getPrintIssn() {
    return printIssn;
  }

  /**
   * Provides the proprietary identifier of the title.
   * 
   * @return a String with the proprietary identifier of the title.
   */
  @Override
  public String getProprietaryId() {
    return proprietaryId;
  }

  /**
   * Provides the publisher name of the title.
   * 
   * @return a String with the publisher name of the title.
   */
  @Override
  public String getPublisherName() {
    return publisherName;
  }

  /**
   * Provides the publishing platform name of the title.
   * 
   * @return a String with the publishing platform name of the title.
   */
  @Override
  public String getPublishingPlatform() {
    return publishingPlatform;
  }

  /**
   * Computes the LOCKSS identifier of the title and persists the title if
   * necessary.
   * 
   * @throws SQLException
   */
  public void identify() throws SQLException {
    DbManager dbManager = LockssDaemon.getLockssDaemon().getDbManager();
    Connection conn = null;
    boolean success = false;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Compute the candidate LOCKSS identifier.
      lockssId = StringUtil.hash64(getLockssIdPayload());

      // Validate the computed LOCKSS identifier to avoid collisions.
      validateLockssId(conn);
      success = true;
    } finally {
      if (success) {
	try {
	  conn.commit();
	  DbManager.safeCloseConnection(conn);
	  identified = true;
	} catch (SQLException sqle) {
	  log.error("Exception caught committing the connection", sqle);
	  DbManager.safeRollbackAndClose(conn);
	}
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }
  }

  /**
   * Validates the LOCKSS identifier of the title.
   * 
   * @param conn
   *          A Connection representing the database connection.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  protected void validateLockssId(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "validateLockssId(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    // Get the name of a title in the database for this LOCKSS identifier.
    String dbName = getNameByLockssId(conn);
    log.debug2(DEBUG_HEADER + "dbName = '" + dbName + "'.");

    // Do nothing more if this LOCKSS identifier does not exist in the database.
    if (dbName == null) {
      log.debug2(DEBUG_HEADER + "Not persisted.");
      persist(conn);
      return;
    } else if (name.equals(dbName)) {
      // Do nothing more if this same title already exists in the database.
      log.debug2(DEBUG_HEADER + "Already persisted.");
      return;
    }

    // Avoid the collision with another title that was found in the database
    // with the same LOCKSS identifier.
    lockssId++;
    log.debug2(DEBUG_HEADER + "Try LOCKSS id = " + lockssId);
    validateLockssId(conn);
    log.debug2(DEBUG_HEADER + "Success with LOCKSS id = " + lockssId);
  }

  /**
   * Persists the title in the database.
   * 
   * @param conn
   *          A Connection representing the database connection.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  protected void persist(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "persist(): ";
    log.debug(DEBUG_HEADER + "Starting...");

    PreparedStatement insertTitle = null;
    String sql = SQL_QUERY_TITLE_INSERT;
    log.debug(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      insertTitle = conn.prepareStatement(sql);

      short index = 1;

      // Populate the LOCKSS identifier.
      insertTitle.setLong(index++, lockssId);

      // Populate the title name.
      insertTitle.setString(index++, name);

      // Populate the publisher name.
      insertTitle.setString(index++, publisherName);

      // Populate the publishing platform.
      insertTitle.setString(index++, publishingPlatform);

      // Populate the DOI.
      insertTitle.setString(index++, doi);

      // Populate the proprietary identifier.
      insertTitle.setString(index++, proprietaryId);

      // Populate the indication that this record corresponds to a book.
      insertTitle.setBoolean(index++, isBook());

      // Populate the journal print ISSN.
      insertTitle.setString(index++, printIssn);

      // Populate the journal online ISSN.
      insertTitle.setString(index++, onlineIssn);

      // Populate the book ISBN.
      insertTitle.setString(index++, isbn);

      // Populate the book ISSN.
      insertTitle.setString(index++, issn);

      // Insert the record.
      int count = insertTitle.executeUpdate();
      log.debug(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot insert title", sqle);
      StringBuilder sb = new StringBuilder();
      appendToStringBuilder(sb);
      log.error("CounterReportsTitle = '" + sb.toString() + "'.");
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(insertTitle);
    }

    log.debug(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides an indication of whether the title is a book.
   * 
   * @return a boolean with the indication.
   */
  public abstract boolean isBook();

  /**
   * Persists a title request.
   * 
   * @param requestData
   *          A Map<String, Object> with properties of the request to be
   *          persisted.
   * @param conn
   *          A Connection representing the database connection.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  @Override
  public void persistRequest(Map<String, Object> requestData, Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "persistRequest(): ";

    // Get the date of the request.
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(TimeBase.nowDate());

    int requestYear = calendar.get(Calendar.YEAR);
    log.debug(DEBUG_HEADER + "requestYear = " + requestYear);
    int requestMonth = (calendar.get(Calendar.MONTH) + 1);
    log.debug(DEBUG_HEADER + "requestMonth = " + requestMonth);
    int requestDay = calendar.get(Calendar.DAY_OF_MONTH);
    log.debug(DEBUG_HEADER + "requestDay = " + requestDay);

    // Get the request properties.
    Boolean isHtml = Boolean.FALSE;
    Boolean isPdf = Boolean.FALSE;
    Boolean isSection = Boolean.FALSE;
    Boolean isPublisherInvolved = Boolean.FALSE;
    String publicationYear = null;

    if (requestData != null) {
      if (requestData.get(IS_HTML_KEY) != null) {
	isHtml = (Boolean) requestData.get(IS_HTML_KEY);
      }
      if (requestData.get(IS_PDF_KEY) != null) {
	isPdf = (Boolean) requestData.get(IS_PDF_KEY);
      }
      if (requestData.get(IS_SECTION_KEY) != null) {
	isSection = (Boolean) requestData.get(IS_SECTION_KEY);
      }
      if (requestData.get(IS_PUBLISHER_INVOLVED_KEY) != null) {
	isPublisherInvolved =
	    (Boolean) requestData.get(IS_PUBLISHER_INVOLVED_KEY);
      }
      if (requestData.get(PUBLICATION_YEAR_KEY) != null) {
	publicationYear = (String) requestData.get(PUBLICATION_YEAR_KEY);
      }
    }

    log.debug(DEBUG_HEADER + "isHtml = " + isHtml);
    log.debug(DEBUG_HEADER + "isPdf = " + isPdf);
    log.debug(DEBUG_HEADER + "isPublisherInvolved = " + isPublisherInvolved);
    log.debug(DEBUG_HEADER + "isSection = " + isSection);
    log.debug(DEBUG_HEADER + "publicationYear = " + publicationYear);

    String sql = SQL_QUERY_TITLE_REQUEST_INSERT;
    log.debug(DEBUG_HEADER + "SQL = '" + sql + "'.");
    PreparedStatement insertRequest = null;

    try {
      // Prepare the statement used to persist the title request.
      insertRequest = conn.prepareStatement(sql);

      short index = 1;

      // Populate the LOCKSS identifier.
      insertRequest.setLong(index++, lockssId);

      // Populate the publication year.
      insertRequest.setString(index++, publicationYear);

      // Populate the indication of whether this record corresponds to the
      // serving of HTML.
      insertRequest.setBoolean(index++, isHtml);

      // Populate the indication of whether this record corresponds to the
      // serving of PDF.
      insertRequest.setBoolean(index++, isPdf);

      // Populate the indication of whether this record corresponds to the
      // serving of a section of the full text.
      insertRequest.setBoolean(index++, isSection);

      // Populate the indication of whether this record corresponds to the
      // serving of the title by the publisher.
      insertRequest.setBoolean(index++, isPublisherInvolved);

      // Populate the year of the request.
      insertRequest.setShort(index++, (short) requestYear);

      // Populate the month of the request.
      insertRequest.setShort(index++, (short) requestMonth);

      // Populate the day of the request.
      insertRequest.setShort(index++, (short) requestDay);

      // Insert the record.
      int count = insertRequest.executeUpdate();
      log.debug(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot persist title request", sqle);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(insertRequest);
    }
  }

  /**
   * Appends the properties of the object to a StringBuilder.
   * 
   * @param sb
   *          A StringBuilder where to append the properties of the object.
   */
  public void appendToStringBuilder(StringBuilder sb) {
    sb.append("name = '").append(name).append("', publisherName = '")
	.append(publisherName).append("', publishingPlatform = '")
	.append(publishingPlatform).append("', doi = '").append(doi)
	.append("', proprietaryId = '").append(proprietaryId)
	.append(", isbn = '").append(isbn).append("', issn = '").append(issn)
	.append("'").append(", printIssn = '").append(printIssn)
	.append("', onlineIssn = '").append(onlineIssn).append("'");
  }
}
