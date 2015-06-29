/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.subscription;

import static org.lockss.db.SqlConstants.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class SubscriptionManagerSql {
  private static final Logger log =
      Logger.getLogger(SubscriptionManagerSql.class);

  // Query to find a subscription by its publication and provider.
  private static final String FIND_SUBSCRIPTION_QUERY = "select "
      + SUBSCRIPTION_SEQ_COLUMN
      + " from " + SUBSCRIPTION_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and " + PROVIDER_SEQ_COLUMN + " = ?";

  // Query to add a subscription.
  private static final String INSERT_SUBSCRIPTION_QUERY = "insert into "
      + SUBSCRIPTION_TABLE
      + "(" + SUBSCRIPTION_SEQ_COLUMN
      + "," + PUBLICATION_SEQ_COLUMN
      + "," + PROVIDER_SEQ_COLUMN
      + ") values (default,?,?)";

  // Query to find the subscription ranges of a publication.
  private static final String FIND_SUBSCRIPTION_RANGES_QUERY = "select "
      + SUBSCRIPTION_RANGE_COLUMN
      + "," + RANGE_IDX_COLUMN
      + " from " + SUBSCRIPTION_RANGE_TABLE
      + " where " + SUBSCRIPTION_SEQ_COLUMN + " = ?"
      + " and " + SUBSCRIBED_COLUMN + " = ?"
      + " order by " + RANGE_IDX_COLUMN;

  // Query to add a subscription range.
  private static final String INSERT_SUBSCRIPTION_RANGE_QUERY = "insert into "
      + SUBSCRIPTION_RANGE_TABLE
      + "(" + SUBSCRIPTION_SEQ_COLUMN
      + "," + SUBSCRIPTION_RANGE_COLUMN
      + "," + SUBSCRIBED_COLUMN
      + "," + RANGE_IDX_COLUMN
      + ") values (?,?,?,"
      + "(select coalesce(max(" + RANGE_IDX_COLUMN + "), 0) + 1"
      + " from " + SUBSCRIPTION_RANGE_TABLE
      + " where " + SUBSCRIPTION_SEQ_COLUMN + " = ?"
      + " and " + SUBSCRIBED_COLUMN + " = ?))";

  // Query to add a subscription range using MySQL.
  private static final String INSERT_SUBSCRIPTION_RANGE_MYSQL_QUERY = "insert "
      + "into " + SUBSCRIPTION_RANGE_TABLE
      + "(" + SUBSCRIPTION_SEQ_COLUMN
      + "," + SUBSCRIPTION_RANGE_COLUMN
      + "," + SUBSCRIBED_COLUMN
      + "," + RANGE_IDX_COLUMN
      + ") values (?,?,?,"
      + "(select next_idx from "
      + "(select coalesce(max(" + RANGE_IDX_COLUMN + "), 0) + 1 as next_idx"
      + " from " + SUBSCRIPTION_RANGE_TABLE
      + " where " + SUBSCRIPTION_SEQ_COLUMN + " = ?"
      + " and " + SUBSCRIBED_COLUMN + " = ?) as temp_sub_range_table))";

  // Query to delete all of the ranges of one type of a subscription.
  private static final String DELETE_ALL_SUBSCRIPTION_RANGES_TYPE_QUERY =
      "delete"
      + " from " + SUBSCRIPTION_RANGE_TABLE
      + " where " + SUBSCRIPTION_SEQ_COLUMN + " = ?"
      + " and " + SUBSCRIBED_COLUMN + " = ?";

  // Query to delete all of the ranges of a subscription.
  private static final String DELETE_ALL_SUBSCRIPTION_RANGES_QUERY = "delete"
      + " from " + SUBSCRIPTION_RANGE_TABLE
      + " where " + SUBSCRIPTION_SEQ_COLUMN + " = ?";

  // Query to find all the subscriptions and their publisher.
  private static final String FIND_ALL_SUBSCRIPTIONS_AND_PUBLISHERS_QUERY =
      "select distinct"
      + " pu." + PUBLISHER_NAME_COLUMN
      + ",n." + NAME_COLUMN
      + ",pr." + PROVIDER_LID_COLUMN
      + ",pr." + PROVIDER_NAME_COLUMN
      + " from " + SUBSCRIPTION_RANGE_TABLE + " sr"
      + "," + SUBSCRIPTION_TABLE + " s"
      + "," + PUBLISHER_TABLE + " pu"
      + "," + PUBLICATION_TABLE + " p"
      + "," + MD_ITEM_NAME_TABLE + " n"
      + "," + PROVIDER_TABLE + " pr"
      + " where sr." + SUBSCRIPTION_SEQ_COLUMN + " = s."
      + SUBSCRIPTION_SEQ_COLUMN
      + " and s." + PUBLICATION_SEQ_COLUMN + " = p." + PUBLICATION_SEQ_COLUMN
      + " and p." + PUBLISHER_SEQ_COLUMN + " = pu." + PUBLISHER_SEQ_COLUMN
      + " and p." + MD_ITEM_SEQ_COLUMN + " = n." + MD_ITEM_SEQ_COLUMN
      + " and s." + PROVIDER_SEQ_COLUMN + " = pr." + PROVIDER_SEQ_COLUMN
      + " order by pu." + PUBLISHER_NAME_COLUMN
      + ",n." + NAME_COLUMN
      + ",pr." + PROVIDER_NAME_COLUMN;

  // Query to find all the subscriptions and their ranges.
  private static final String FIND_ALL_SUBSCRIPTIONS_AND_RANGES_QUERY = "select"
      + " distinct s." + SUBSCRIPTION_SEQ_COLUMN
      + ",n." + NAME_COLUMN
      + ",pi." + PROPRIETARY_ID_COLUMN
      + ",pu." + PUBLISHER_NAME_COLUMN
      + ",pr." + PROVIDER_LID_COLUMN
      + ",pr." + PROVIDER_NAME_COLUMN
      + ",sr." + SUBSCRIPTION_RANGE_COLUMN
      + ",sr." + SUBSCRIBED_COLUMN
      + ",sr." + RANGE_IDX_COLUMN
      + ",i1." + ISSN_COLUMN + " as " + P_ISSN_TYPE
      + ",i2." + ISSN_COLUMN + " as " + E_ISSN_TYPE
      + " from " + SUBSCRIPTION_TABLE + " s"
      + "," + PUBLICATION_TABLE + " p"
      + "," + PUBLISHER_TABLE + " pu"
      + "," + MD_ITEM_NAME_TABLE + " n"
      + "," + PROVIDER_TABLE + " pr"
      + "," + SUBSCRIPTION_RANGE_TABLE + " sr"
      + "," + MD_ITEM_TABLE + " mi"
      + " left outer join " + ISSN_TABLE + " i1"
      + " on mi." + MD_ITEM_SEQ_COLUMN + " = i1." + MD_ITEM_SEQ_COLUMN
      + " and i1." + ISSN_TYPE_COLUMN + " = '" + P_ISSN_TYPE + "'"
      + " left outer join " + ISSN_TABLE + " i2"
      + " on mi." + MD_ITEM_SEQ_COLUMN + " = i2." + MD_ITEM_SEQ_COLUMN
      + " and i2." + ISSN_TYPE_COLUMN + " = '" + E_ISSN_TYPE + "'"
      + " left outer join " + PROPRIETARY_ID_TABLE + " pi"
      + " on mi." + MD_ITEM_SEQ_COLUMN + " = pi." + MD_ITEM_SEQ_COLUMN
      + " where s." + PUBLICATION_SEQ_COLUMN + " = p." + PUBLICATION_SEQ_COLUMN
      + " and p." + PUBLISHER_SEQ_COLUMN + " = pu." + PUBLISHER_SEQ_COLUMN
      + " and p." + MD_ITEM_SEQ_COLUMN + " = mi." + MD_ITEM_SEQ_COLUMN
      + " and p." + MD_ITEM_SEQ_COLUMN + " = n." + MD_ITEM_SEQ_COLUMN
      + " and n." + NAME_TYPE_COLUMN + " = 'primary'"
      + " and s." + PROVIDER_SEQ_COLUMN + " = pr." + PROVIDER_SEQ_COLUMN
      + " and s." + SUBSCRIPTION_SEQ_COLUMN + " = sr." + SUBSCRIPTION_SEQ_COLUMN
      + " order by pu." + PUBLISHER_NAME_COLUMN
      + ",n." + NAME_COLUMN
      + ",pr." + PROVIDER_NAME_COLUMN
      + ",sr." + SUBSCRIPTION_RANGE_COLUMN
      + ",sr." + SUBSCRIBED_COLUMN
      + ",sr." + RANGE_IDX_COLUMN
      + ",pi." + PROPRIETARY_ID_COLUMN;

  // Query to get the subscription ranges.
  private static final String FIND_ALL_SUBSCRIPTION_RANGES_QUERY = "select "
      + SUBSCRIPTION_SEQ_COLUMN
      + " from " + SUBSCRIPTION_RANGE_TABLE;

  // Query to get the publisher subscriptions.
  private static final String FIND_ALL_PUBLISHER_SUBSCRIPTIONS_QUERY = "select "
      + PUBLISHER_SUBSCRIPTION_SEQ_COLUMN
      + " from " + PUBLISHER_SUBSCRIPTION_TABLE;

  // Query to find all the subscription data for backup purposes.
  private static final String FIND_SUBSCRIPTION_BACKUP_DATA_QUERY = "select"
      + " distinct pu." + PUBLISHER_NAME_COLUMN
      + ",pi." + PROPRIETARY_ID_COLUMN
      + ",n." + NAME_COLUMN
      + ",pr." + PROVIDER_LID_COLUMN
      + ",pr." + PROVIDER_NAME_COLUMN
      + ",i1." + ISSN_COLUMN + " as " + P_ISSN_TYPE
      + ",i2." + ISSN_COLUMN + " as " + E_ISSN_TYPE
      + ",sr." + SUBSCRIPTION_RANGE_COLUMN
      + ",sr." + SUBSCRIBED_COLUMN
      + ",sr." + RANGE_IDX_COLUMN
      + " from " + SUBSCRIPTION_RANGE_TABLE + " sr"
      + "," + SUBSCRIPTION_TABLE + " s"
      + "," + PUBLISHER_TABLE + " pu"
      + "," + PUBLICATION_TABLE + " p"
      + "," + MD_ITEM_NAME_TABLE + " n"
      + "," + PROVIDER_TABLE + " pr"
      + "," + MD_ITEM_TABLE + " mi"
      + " left outer join " + ISSN_TABLE + " i1"
      + " on mi." + MD_ITEM_SEQ_COLUMN + " = i1." + MD_ITEM_SEQ_COLUMN
      + " and i1." + ISSN_TYPE_COLUMN + " = '" + P_ISSN_TYPE + "'"
      + " left outer join " + ISSN_TABLE + " i2"
      + " on mi." + MD_ITEM_SEQ_COLUMN + " = i2." + MD_ITEM_SEQ_COLUMN
      + " and i2." + ISSN_TYPE_COLUMN + " = '" + E_ISSN_TYPE + "'"
      + " left outer join " + PROPRIETARY_ID_TABLE + " pi"
      + " on mi." + MD_ITEM_SEQ_COLUMN + " = pi." + MD_ITEM_SEQ_COLUMN
      + " where sr." + SUBSCRIPTION_SEQ_COLUMN + " = s."
      + SUBSCRIPTION_SEQ_COLUMN
      + " and s." + PUBLICATION_SEQ_COLUMN + " = p." + PUBLICATION_SEQ_COLUMN
      + " and p." + PUBLISHER_SEQ_COLUMN + " = pu." + PUBLISHER_SEQ_COLUMN
      + " and p." + MD_ITEM_SEQ_COLUMN + " = mi." + MD_ITEM_SEQ_COLUMN
      + " and mi." + MD_ITEM_SEQ_COLUMN + " = n." + MD_ITEM_SEQ_COLUMN
      + " and n." + NAME_TYPE_COLUMN + " = '" + PRIMARY_NAME_TYPE + "'"
      + " and s." + PROVIDER_SEQ_COLUMN + " = pr." + PROVIDER_SEQ_COLUMN
      + " order by pu." + PUBLISHER_NAME_COLUMN
      + ",n." + NAME_COLUMN
      + ",pr." + PROVIDER_NAME_COLUMN
      + ",sr." + SUBSCRIPTION_RANGE_COLUMN
      + ",sr." + SUBSCRIBED_COLUMN
      + ",sr." + RANGE_IDX_COLUMN
      + ",pi." + PROPRIETARY_ID_COLUMN;

  // Query to update the type of a subscription range.
  private static final String UPDATE_SUBSCRIPTION_RANGE_TYPE_QUERY = "update "
      + SUBSCRIPTION_RANGE_TABLE
      + " set " + SUBSCRIBED_COLUMN + " = ?"
      + " where " + SUBSCRIPTION_SEQ_COLUMN + " = ?"
      + " and " + SUBSCRIPTION_RANGE_COLUMN + " = ?";

  // Query to find a publisher subscription setting.
  private static final String FIND_PUBLISHER_SUBSCRIPTION_QUERY = "select "
      + SUBSCRIBED_COLUMN
      + " from " + PUBLISHER_SUBSCRIPTION_TABLE
      + " where " + PUBLISHER_SEQ_COLUMN + " = ?"
      + " and " + PROVIDER_SEQ_COLUMN + " = ?";

  // Query to delete a publisher subscription setting.
  private static final String DELETE_PUBLISHER_SUBSCRIPTION_QUERY = "delete "
      + " from " + PUBLISHER_SUBSCRIPTION_TABLE
      + " where " + PUBLISHER_SEQ_COLUMN + " = ?"
      + " and " + PROVIDER_SEQ_COLUMN + " = ?";

  // Query to update a publisher subscription.
  private static final String UPDATE_PUBLISHER_SUBSCRIPTION_QUERY = "update "
      + PUBLISHER_SUBSCRIPTION_TABLE
      + " set " + SUBSCRIBED_COLUMN + " = ?"
      + " where " + PUBLISHER_SEQ_COLUMN + " = ?"
      + " and " + PROVIDER_SEQ_COLUMN + " = ?";

  // Query to add a publisher subscription.
  private static final String INSERT_PUBLISHER_SUBSCRIPTION_QUERY = "insert "
      + "into " + PUBLISHER_SUBSCRIPTION_TABLE
      + "(" + PUBLISHER_SUBSCRIPTION_SEQ_COLUMN
      + "," + PUBLISHER_SEQ_COLUMN
      + "," + PROVIDER_SEQ_COLUMN
      + "," + SUBSCRIBED_COLUMN
      + ") values (default,?,?,?)";

  private DbManager dbManager;
  //private SubscriptionManager subscriptionManager;

  /**
   * Constructor.
   * 
   * @param dbManager
   *          A DbManager with the database manager.
   */
  SubscriptionManagerSql(DbManager dbManager) {
    this.dbManager = dbManager;
  }

  /**
   * Provides the identifier of a subscription.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the identifier of the publication.
   * @param providerSeq
   *          A Long with the identifier of the provider.
   * @return a Long with the identifier of the subscription.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findSubscription(Connection conn, Long publicationSeq, Long providerSeq)
      throws DbException {
    final String DEBUG_HEADER = "findSubscription(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    }

    PreparedStatement findSubscription =
	dbManager.prepareStatement(conn, FIND_SUBSCRIPTION_QUERY);
    ResultSet resultSet = null;
    Long subscriptionSeq = null;

    try {
      findSubscription.setLong(1, publicationSeq);
      findSubscription.setLong(2, providerSeq);
      resultSet = dbManager.executeQuery(findSubscription);
      if (resultSet.next()) {
	subscriptionSeq = resultSet.getLong(SUBSCRIPTION_SEQ_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Found subscriptionSeq = "
	      + subscriptionSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find subscription", sqle);
      log.error("SQL = '" + FIND_SUBSCRIPTION_QUERY + "'.");
      log.error("publicationSeq = " + publicationSeq);
      log.error("providerSeq = " + providerSeq);
      throw new DbException("Cannot find subscription", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findSubscription);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
    return subscriptionSeq;
  }

  /**
   * Provides the subscription ranges for a subscription.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscriptionSeq
   *          A Long with the identifier of the subscription.
   * @param subscribed
   *          A boolean with the subscribed attribute of the ranges to be
   *          provided.
   * @return a List<BibliographicPeriod> with the ranges for the subscription.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  List<BibliographicPeriod> findSubscriptionRanges(Connection conn,
      Long subscriptionSeq, boolean subscribed) throws DbException {
    final String DEBUG_HEADER = "findSubscriptionsRanges(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
      log.debug2(DEBUG_HEADER + "subscribed = " + subscribed);
    }

    String range;
    List<BibliographicPeriod> ranges = new ArrayList<BibliographicPeriod>();
    String query = FIND_SUBSCRIPTION_RANGES_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "SQL = " + query);

    PreparedStatement getSubscriptionRanges =
	dbManager.prepareStatement(conn, query);
    ResultSet resultSet = null;

    try {
      getSubscriptionRanges.setLong(1, subscriptionSeq);
      getSubscriptionRanges.setBoolean(2, subscribed);
      resultSet = dbManager.executeQuery(getSubscriptionRanges);

      while (resultSet.next()) {
	range = resultSet.getString(SUBSCRIPTION_RANGE_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "range = " + range);

	ranges.add(new BibliographicPeriod(range));
      }
    } catch (SQLException sqle) {
      log.error("Cannot get ranges", sqle);
      log.error("SQL = '" + query + "'.");
      log.error("subscriptionSeq = " + subscriptionSeq);
      log.error("subscribed = " + subscribed);
      throw new DbException("Cannot get ranges", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(getSubscriptionRanges);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "ranges = " + ranges);
    return ranges;
  }

  /**
   * Adds a subscription range to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscriptionSeq
   *          A Long with the identifier of the subscription.
   * @param range
   *          A BibliographicPeriod with the subscription range.
   * @param subscribed
   *          A boolean with the indication of whether the LOCKSS installation
   *          is subscribed to the publication range or not.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int persistSubscriptionRange(Connection conn, Long subscriptionSeq,
      BibliographicPeriod range, boolean subscribed) throws DbException {
    final String DEBUG_HEADER = "persistSubscriptionRange(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
      log.debug2(DEBUG_HEADER + "range = " + range);
      log.debug2(DEBUG_HEADER + "subscribed = " + subscribed);
    }

    int count = 0;

    // Skip an empty range that does not accomplish anything.
    if (range.isEmpty()) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
      return count;
    }

    String sql = getInsertSubscriptionRangeSql();
    PreparedStatement insertSubscriptionRange =
	dbManager.prepareStatement(conn, sql);

    try {
      insertSubscriptionRange.setLong(1, subscriptionSeq);
      insertSubscriptionRange.setString(2, range.toDisplayableString());
      insertSubscriptionRange.setBoolean(3, subscribed);
      insertSubscriptionRange.setLong(4, subscriptionSeq);
      insertSubscriptionRange.setBoolean(5, subscribed);

      count = dbManager.executeUpdate(insertSubscriptionRange);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot insert subscription range", sqle);
      log.error("SQL = '" + sql + "'.");
      log.error("subscriptionSeq = " + subscriptionSeq);
      log.error("range = " + range);
      log.error("subscribed = " + subscribed);
      throw new DbException("Cannot insert subscription range", sqle);
    } finally {
      DbManager.safeCloseStatement(insertSubscriptionRange);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Provides the SQL statement used to insert a subscription range.
   * 
   * @return a String with the SQL statement used to insert a subscription
   *         range.
   */
  private String getInsertSubscriptionRangeSql() {
    if (dbManager.isTypeMysql()) {
      return INSERT_SUBSCRIPTION_RANGE_MYSQL_QUERY;
    }

    return INSERT_SUBSCRIPTION_RANGE_QUERY;
  }

  /**
   * Deletes all ranges of a type belonging to a subscription from the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscriptionSeq
   *          A Long with the identifier of the subscription.
   * @param subscribed
   *          A boolean with the indication of whether the LOCKSS installation
   *          is subscribed to the publication range or not.
   * @return an int with the number of deleted rows.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int deleteSubscriptionTypeRanges(Connection conn, Long subscriptionSeq,
      boolean subscribed) throws DbException {
    final String DEBUG_HEADER = "deleteSubscriptionTypeRanges(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
      log.debug2(DEBUG_HEADER + "subscribed = " + subscribed);
    }

    int count = 0;
    PreparedStatement deleteSubscriptionRange = dbManager.prepareStatement(conn,
	DELETE_ALL_SUBSCRIPTION_RANGES_TYPE_QUERY);

    try {
      deleteSubscriptionRange.setLong(1, subscriptionSeq);
      deleteSubscriptionRange.setBoolean(2, subscribed);

      count = dbManager.executeUpdate(deleteSubscriptionRange);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete subscription range", sqle);
      log.error("SQL = '" + DELETE_ALL_SUBSCRIPTION_RANGES_TYPE_QUERY + "'.");
      log.error("subscriptionSeq = " + subscriptionSeq);
      log.error("subscribed = " + subscribed);
      throw new DbException("Cannot delete subscription range", sqle);
    } finally {
      DbManager.safeCloseStatement(deleteSubscriptionRange);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Adds a subscription to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the identifier of the publication.
   * @param providerSeq
   *          A Long with the identifier of the provider.
   * @return a Long with the identifier of the subscription just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long persistSubscription(Connection conn, Long publicationSeq,
      Long providerSeq) throws DbException {
    final String DEBUG_HEADER = "persistSubscription(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    }

    PreparedStatement insertSubscription = dbManager.prepareStatement(conn,
	INSERT_SUBSCRIPTION_QUERY, Statement.RETURN_GENERATED_KEYS);

    ResultSet resultSet = null;
    Long subscriptionSeq = null;

    try {
      // Skip auto-increment key field #0
      insertSubscription.setLong(1, publicationSeq);
      insertSubscription.setLong(2, providerSeq);
      dbManager.executeUpdate(insertSubscription);
      resultSet = insertSubscription.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create SUBSCRIPTION table row: publicationSeq = "
	    + publicationSeq + ", providerSeq = " + providerSeq
	    + " - No keys were generated.");
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "subscriptionSeq = null");
	return null;
      }

      subscriptionSeq = resultSet.getLong(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Added subscriptionSeq = " + subscriptionSeq);
    } catch (SQLException sqle) {
      log.error("Cannot insert subscription", sqle);
      log.error("SQL = '" + INSERT_SUBSCRIPTION_QUERY + "'.");
      log.error("publicationSeq = " + publicationSeq);
      log.error("providerSeq = " + providerSeq);
      throw new DbException("Cannot insert subscription", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertSubscription);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
    return subscriptionSeq;
  }

  /**
   * Provides all the subscriptions in the system and their ranges.
   * 
   * @return a List<Subscription> with all the subscriptions and their ranges in
   *         the system.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  List<Subscription> findAllSubscriptionsAndRanges() throws DbException {
    final String DEBUG_HEADER = "findAllSubscriptionsAndRanges(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    Long subscriptionSeq = null;
    String publicationName;
    String proprietaryId;
    String pIssn;
    String eIssn;
    String publisherName;
    String providerLid;
    String providerName;
    String ranges = null;
    boolean subscribed = false;
    SerialPublication publication;
    Subscription subscription = new Subscription();
    List<Subscription> subscriptions = new ArrayList<Subscription>();
    Set<String> proprietaryIds = null;

    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    String query = FIND_ALL_SUBSCRIPTIONS_AND_RANGES_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "SQL = " + query);

    PreparedStatement getAllSubscriptionRanges =
	dbManager.prepareStatement(conn, query);
    ResultSet resultSet = null;

    try {
      // Get all the subscriptions and ranges from the database.
      resultSet = dbManager.executeQuery(getAllSubscriptionRanges);

      // Loop through all the results.
      while (resultSet.next()) {
	// Check whether this subscription is the same as the previous one.
	if (subscriptionSeq != null
	    && resultSet.getLong(SUBSCRIPTION_SEQ_COLUMN) == subscriptionSeq
	    && ranges != null
	    && resultSet.getString(SUBSCRIPTION_RANGE_COLUMN).equals(ranges)
	    && resultSet.getBoolean(SUBSCRIBED_COLUMN) == subscribed) {
	  // Yes: This means that the publication has multiple values for some
	  // attributes. Get the proprietary identifier.
	  proprietaryId = resultSet.getString(PROPRIETARY_ID_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "proprietaryId = " + proprietaryId);

	  // Add it to the list of proprietary identifiers, if it exists.
	  if (!StringUtil.isNullString(proprietaryId)) {
	    proprietaryIds.add(proprietaryId);
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER
		+ "Added proprietaryId = '" + proprietaryId + "'.");
	  }

	  continue;
	}

	subscriptionSeq = resultSet.getLong(SUBSCRIPTION_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);

	publicationName = resultSet.getString(NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

	// Initialize the collection of proprietary identifiers.
	proprietaryIds = new LinkedHashSet<String>();

	proprietaryId = resultSet.getString(PROPRIETARY_ID_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "proprietaryId = " + proprietaryId);

	// Add it to the list of proprietary identifiers, if it exists.
	if (!StringUtil.isNullString(proprietaryId)) {
	  proprietaryIds.add(proprietaryId);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER
	      + "Added proprietaryId = '" + proprietaryId + "'.");
	}

        pIssn = resultSet.getString(P_ISSN_TYPE);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pIssn = " + pIssn);

        eIssn = resultSet.getString(E_ISSN_TYPE);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "eIssn = " + eIssn);

	publisherName = resultSet.getString(PUBLISHER_NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	providerLid = resultSet.getString(PROVIDER_LID_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "providerLid = " + providerLid);

	providerName = resultSet.getString(PROVIDER_NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "providerName = " + providerName);

	ranges = resultSet.getString(SUBSCRIPTION_RANGE_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "ranges = " + ranges);

	subscribed = resultSet.getBoolean(SUBSCRIBED_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "subscribed = " + subscribed);

	// Convert the text ranges into a list of bibliographic periods.
	List<BibliographicPeriod> periods =
	    BibliographicPeriod.createList(ranges);

	// Check whether this is another range for the same subscription as the
	// last one.
	if (subscriptionSeq.equals(subscription.getSubscriptionSeq())) {
	  // Yes: Check whether it is a subscribed range.
	  if (subscribed) {
	    // Yes: Add it to the subscription subscribed ranges.
	    subscription.addSubscribedRanges(periods);
	  } else {
	    // No: Add it to the subscription unsubscribed ranges.
	    subscription.addUnsubscribedRanges(periods);
	  }
	} else {
	  // No: Add the previous subscription to the results.
	  if (subscription.getSubscriptionSeq() != null) {
	    subscriptions.add(subscription);
	  }

	  // Initialize the new subscription publication.
	  publication = new SerialPublication();
	  publication.setPublicationName(publicationName);
	  publication.setProprietaryIds(proprietaryIds);
	  publication.setPissn(pIssn);
	  publication.setEissn(eIssn);
	  publication.setPublisherName(publisherName);
	  publication.setProviderLid(providerLid);
	  publication.setProviderName(providerName);

	  // Initialize the new subscription.
	  subscription = new Subscription();
	  subscription.setPublication(publication);
	  subscription.setSubscriptionSeq(subscriptionSeq);

	  // Check whether it is a subscribed range.
	  if (subscribed) {
	    // Yes: Add it to the subscription subscribed ranges.
	    subscription.setSubscribedRanges(periods);
	  } else {
	    // No: Add it to the subscription unsubscribed ranges.
	    subscription.setUnsubscribedRanges(periods);
	  }
	}
      }

      // Add the last subscription to the results.
      if (subscription.getSubscriptionSeq() != null) {
	subscriptions.add(subscription);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get existing subscriptions", sqle);
      log.error("SQL = '" + query + "'.");
      throw new DbException("Cannot get existing subscriptions", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(getAllSubscriptionRanges);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscriptions = " + subscriptions);
    return subscriptions;
  }

  /**
   * Provides an indication of whether there are subscription ranges.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a boolean with <code>true</code> if there are subscribed
   *         publications, <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  boolean hasSubscriptionRanges(Connection conn) throws DbException {
    final String DEBUG_HEADER = "hasSubscriptionRanges(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    boolean result = false;

    String query = FIND_ALL_SUBSCRIPTION_RANGES_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "SQL = " + query);

    PreparedStatement findAnySubscription = null;
    ResultSet resultSet = null;

    try {
      findAnySubscription = dbManager.prepareStatement(conn, query);
      findAnySubscription.setMaxRows(1);
      resultSet = dbManager.executeQuery(findAnySubscription);
      result = resultSet.next();
    } catch (SQLException sqle) {
      String message = "Cannot find any subscribed publications";
      log.error(message, sqle);
      log.error("SQL = '" + query + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAnySubscription);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides an indication of whether there are publisher subscriptions.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a boolean with <code>true</code> if there are publisher
   *         subscriptions, <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  boolean hasPublisherSubscriptions(Connection conn) throws DbException {
    final String DEBUG_HEADER = "hasPublisherSubscriptions(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    boolean result = false;

    String query = FIND_ALL_PUBLISHER_SUBSCRIPTIONS_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "SQL = " + query);

    PreparedStatement findAnyPublisherSubscription = null;
    ResultSet resultSet = null;

    try {
      findAnyPublisherSubscription = dbManager.prepareStatement(conn, query);
      findAnyPublisherSubscription.setMaxRows(1);
      resultSet = dbManager.executeQuery(findAnyPublisherSubscription);
      result = resultSet.next();
    } catch (SQLException sqle) {
      String message = "Cannot find any publisher subscriptions";
      log.error(message, sqle);
      log.error("SQL = '" + query + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAnyPublisherSubscription);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides all the subscriptions and their publishers.
   * 
   * @return a List<Subscription> with the subscriptions and their publishers.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  List<Subscription> findAllSubscriptionsAndPublishers() throws DbException {
    final String DEBUG_HEADER = "findAllSubscriptionsAndPublishers(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String publicationName;
    String providerLid;
    String providerName;
    String publisherName;
    SerialPublication publication;
    Subscription subscription;
    List<Subscription> subscriptions = new ArrayList<Subscription>();

    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    String query = FIND_ALL_SUBSCRIPTIONS_AND_PUBLISHERS_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "SQL = " + query);

    PreparedStatement getAllSubscriptionsAndPublishers =
	dbManager.prepareStatement(conn, query);
    ResultSet resultSet = null;

    try {
      resultSet = dbManager.executeQuery(getAllSubscriptionsAndPublishers);

      while (resultSet.next()) {
	publisherName = resultSet.getString(PUBLISHER_NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	publicationName = resultSet.getString(NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

	providerLid = resultSet.getString(PROVIDER_LID_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "providerLid = " + providerLid);

	providerName = resultSet.getString(PROVIDER_NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "providerName = " + providerName);

	publication = new SerialPublication();
	publication.setPublisherName(publisherName);
	publication.setPublicationName(publicationName);
	publication.setProviderLid(providerLid);
	publication.setProviderName(providerName);

	subscription = new Subscription();
	subscription.setPublication(publication);

	subscriptions.add(subscription);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get subscriptions and publishers", sqle);
      log.error("SQL = '" + query + "'.");
      throw new DbException("Cannot get subscriptions and publishers", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(getAllSubscriptionsAndPublishers);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "subscriptions.size() = "
	+ subscriptions.size());
    return subscriptions;
  }

  /**
   * Deletes all the ranges of a given subscription.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscriptionSeq
   *          A Long with the identifier of the subscription.
   * @return an int with the number of deleted rows.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int deleteAllSubscriptionRanges(Connection conn, Long subscriptionSeq)
      throws DbException {
    final String DEBUG_HEADER = "deleteAllSubscriptionRanges(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);

    int count = 0;
    PreparedStatement deleteAllSubscriptionRanges =
	dbManager.prepareStatement(conn, DELETE_ALL_SUBSCRIPTION_RANGES_QUERY);

    try {
      deleteAllSubscriptionRanges.setLong(1, subscriptionSeq);

      count = dbManager.executeUpdate(deleteAllSubscriptionRanges);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Deleted " + count + " subscription ranges.");
    } catch (SQLException sqle) {
      log.error("Cannot delete subscription ranges", sqle);
      log.error("SQL = '" + DELETE_ALL_SUBSCRIPTION_RANGES_QUERY + "'.");
      log.error("subscriptionSeq = " + subscriptionSeq);
      throw new DbException("Cannot delete subscription ranges", sqle);
    } finally {
      DbManager.safeCloseStatement(deleteAllSubscriptionRanges);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Provides the subscription data for backup purposes.
   * 
   * @return a List<Subscription> with the subscription data.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  List<Subscription> findSubscriptionDataForBackup() throws DbException {
    final String DEBUG_HEADER = "findSubscriptionDataForBackup(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String publicationName = null;
    String providerLid;
    String providerName = null;
    String publisherName = null;
    String proprietaryId;
    String pIssn;
    String eIssn;
    String range = null;
    Boolean subscribed = null;
    String previousPublicationName = null;
    String previousProviderLid = null;
    String previousProviderName = null;
    String previousPublisherName = null;
    String previousPissn = null;
    String previousEissn = null;
    SerialPublication publication;
    Subscription subscription = null;
    List<Subscription> subscriptions = new ArrayList<Subscription>();
    Set<String> proprietaryIds = null;

    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    String query = FIND_SUBSCRIPTION_BACKUP_DATA_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "SQL = " + query);

    PreparedStatement getSubscriptionDataForBackup =
	dbManager.prepareStatement(conn, query);
    ResultSet resultSet = null;

    try {
      // Get the subscriptions from the database.
      resultSet = dbManager.executeQuery(getSubscriptionDataForBackup);

      // Loop through all the results.
      while (resultSet.next()) {
	// Check whether this subscription is the same as the previous one.
	if (resultSet.getString(PUBLISHER_NAME_COLUMN).equals(publisherName)
	    && resultSet.getString(NAME_COLUMN).equals(publicationName)
	    && resultSet.getString(PROVIDER_NAME_COLUMN).equals(providerName)
	    && resultSet.getString(SUBSCRIPTION_RANGE_COLUMN).equals(range)
	    && resultSet.getBoolean(SUBSCRIBED_COLUMN) == subscribed) {
	  // Yes: This means that the publication has multiple values for some
	  // attributes. Get the proprietary identifier.
	  proprietaryId = resultSet.getString(PROPRIETARY_ID_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "proprietaryId = " + proprietaryId);

	  // Add it to the list of proprietary identifiers, if it exists.
	  if (!StringUtil.isNullString(proprietaryId)) {
	    proprietaryIds.add(proprietaryId);
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER
		+ "Added proprietaryId = '" + proprietaryId + "'.");
	  }

	  continue;
	}

	// Get the publication data.
	publisherName = resultSet.getString(PUBLISHER_NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	publicationName = resultSet.getString(NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

	providerLid = resultSet.getString(PROVIDER_LID_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "providerLid = " + providerLid);

	providerName = resultSet.getString(PROVIDER_NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "providerName = " + providerName);

	// Initialize the collection of proprietary identifiers.
	proprietaryIds = new LinkedHashSet<String>();

	proprietaryId = resultSet.getString(PROPRIETARY_ID_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "proprietaryId = " + proprietaryId);

	// Add it to the list of proprietary identifiers, if it exists.
	if (!StringUtil.isNullString(proprietaryId)) {
	  proprietaryIds.add(proprietaryId);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER
	      + "Added proprietaryId = '" + proprietaryId + "'.");
	}

	pIssn = resultSet.getString(P_ISSN_TYPE);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pIssn = " + pIssn);

	eIssn = resultSet.getString(E_ISSN_TYPE);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "eIssn = " + eIssn);

	// Check whether the publication of this result does not correspond to
	// the publication of the previous result.
	if (((publicationName == null && previousPublicationName != null)
	     || (publicationName != null
	         && !publicationName.equals(previousPublicationName)))
	    || ((providerLid == null && previousProviderLid != null)
		|| (providerLid != null
		    && !providerLid.equals(previousProviderLid)))
	    || ((providerName == null && previousProviderName != null)
		|| (providerName != null
		    && !providerName.equals(previousProviderName)))
	    || ((publisherName == null && previousPublisherName != null)
		|| (publisherName != null
		    && !publisherName.equals(previousPublisherName)))
	    || ((pIssn == null && previousPissn != null)
		|| (pIssn != null && !pIssn.equals(previousPissn)))
	    || ((eIssn == null && previousEissn != null)
		|| (eIssn != null && !eIssn.equals(previousEissn)))) {

	  // Yes: Start a new subscription.
	  publication = new SerialPublication();
	  publication.setPublisherName(publisherName);
	  publication.setPublicationName(publicationName);
	  publication.setProviderLid(providerLid);
	  publication.setProviderName(providerName);
	  publication.setProprietaryIds(proprietaryIds);
	  publication.setPissn(pIssn);
	  publication.setEissn(eIssn);

	  // Validate the publication name.
	  publication.getTdbTitle();

	  subscription = new Subscription();
	  subscription.setPublication(publication);
	  subscription.
	  setSubscribedRanges(new ArrayList<BibliographicPeriod>());
	  subscription.
	  setUnsubscribedRanges(new ArrayList<BibliographicPeriod>());

	  subscriptions.add(subscription);

	  // Remember the publication for this subscription.
	  previousPublisherName = publisherName;
	  previousPublicationName = publicationName;
	  previousProviderLid = providerLid;
	  previousProviderName = providerName;
	  previousPissn = pIssn;
	  previousEissn = eIssn;
	}

	// Get the subscription data.
	range = resultSet.getString(SUBSCRIPTION_RANGE_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "range = " + range);

	subscribed = resultSet.getBoolean(SUBSCRIBED_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "subscribed = " + subscribed);

	if (subscribed.booleanValue()) {
	  subscription.getSubscribedRanges().
	  add(new BibliographicPeriod(range));
	} else {
	  subscription.getUnsubscribedRanges().
	  add(new BibliographicPeriod(range));
	}
      }
    } catch (SQLException sqle) {
      String message = "Cannot get subscriptions for backup";
      log.error(message, sqle);
      log.error("SQL = '" + query + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(getSubscriptionDataForBackup);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "subscriptions.size() = "
	+ subscriptions.size());
    return subscriptions;
  }

  /**
   * Updates the type of a subscription range.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscriptionSeq
   *          A Long with the identifier of the subscription.
   * @param range
   *          A BibliographicPeriod with the subscription range.
   * @param subscribed
   *          A boolean with the indication of whether the LOCKSS installation
   *          is subscribed to the publication range or not.
   * @return an int with the count of rows updated in the database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int updateSubscriptionRangeType(Connection conn, Long subscriptionSeq,
      BibliographicPeriod range, boolean subscribed) throws DbException {
    final String DEBUG_HEADER = "updateSubscriptionRangeType(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
      log.debug2(DEBUG_HEADER + "range = " + range);
      log.debug2(DEBUG_HEADER + "subscribed = " + subscribed);
    }

    int count = 0;

    // Skip an empty range that does not accomplish anything.
    if (range.isEmpty()) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
      return count;
    }

    PreparedStatement updateSubscriptionRange =
	dbManager.prepareStatement(conn, UPDATE_SUBSCRIPTION_RANGE_TYPE_QUERY);

    try {
      updateSubscriptionRange.setBoolean(1, subscribed);
      updateSubscriptionRange.setLong(2, subscriptionSeq);
      updateSubscriptionRange.setString(3, range.toDisplayableString());

      count = dbManager.executeUpdate(updateSubscriptionRange);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update subscription range", sqle);
      log.error("SQL = '" + UPDATE_SUBSCRIPTION_RANGE_TYPE_QUERY + "'.");
      log.error("subscriptionSeq = " + subscriptionSeq);
      log.error("range = " + range);
      log.error("subscribed = " + subscribed);
      throw new DbException("Cannot update subscription range", sqle);
    } finally {
      DbManager.safeCloseStatement(updateSubscriptionRange);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Provides the setting of a publisher subscription.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the identifier of the publisher.
   * @param providerSeq
   *          A Long with the identifier of the provider.
   * @return a Boolean with the setting of the publisher subscription.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Boolean findPublisherSubscription(Connection conn, Long publisherSeq,
      Long providerSeq) throws DbException {
    final String DEBUG_HEADER = "findPublisherSubscription(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    }

    PreparedStatement findPublisherSubscription =
	dbManager.prepareStatement(conn, FIND_PUBLISHER_SUBSCRIPTION_QUERY);
    ResultSet resultSet = null;
    Boolean subscribed = null;

    try {
      findPublisherSubscription.setLong(1, publisherSeq);
      findPublisherSubscription.setLong(2, providerSeq);
      resultSet = dbManager.executeQuery(findPublisherSubscription);
      if (resultSet.next()) {
	subscribed = resultSet.getBoolean(SUBSCRIBED_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "Found subscribed = " + subscribed);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find publisher subscription", sqle);
      log.error("SQL = '" + FIND_PUBLISHER_SUBSCRIPTION_QUERY + "'.");
      log.error("publisherSeq = " + publisherSeq);
      log.error("providerSeq = " + providerSeq);
      throw new DbException("Cannot find publisher subscription", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublisherSubscription);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscribed = " + subscribed);
    return subscribed;
  }

  /**
   * Deletes the setting of a publisher subscription.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the identifier of the publisher.
   * @param providerSeq
   *          A Long with the identifier of the provider.
   * @return an int with the number of deleted rows.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int deletePublisherSubscription(Connection conn, Long publisherSeq,
      Long providerSeq) throws DbException {
    final String DEBUG_HEADER = "deletePublisherSubscription(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    }

    int count = 0;
    PreparedStatement deletePublisherSubscription =
	dbManager.prepareStatement(conn, DELETE_PUBLISHER_SUBSCRIPTION_QUERY);

    try {
      deletePublisherSubscription.setLong(1, publisherSeq);
      deletePublisherSubscription.setLong(2, providerSeq);

      count = dbManager.executeUpdate(deletePublisherSubscription);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Deleted " + count
	    + " publisher subscriptions.");
    } catch (SQLException sqle) {
      log.error("Cannot delete publisher subscription", sqle);
      log.error("SQL = '" + DELETE_PUBLISHER_SUBSCRIPTION_QUERY + "'.");
      log.error("publisherSeq = " + publisherSeq);
      log.error("providerSeq = " + providerSeq);
      throw new DbException("Cannot delete publisher subscription", sqle);
    } finally {
      DbManager.safeCloseStatement(deletePublisherSubscription);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Updates, if it already exists, or creates otherwise a publisher
   * subscription.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the identifier of the publisher.
   * @param providerSeq
   *          A Long with the identifier of the provider.
   * @param subscribed
   *          A boolean with the indication of the publisher subscription.
   * @return a Long with the identifier of the publisher subscription if it's just added, <code>null</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long updateOrCreatePublisherSubscription(Connection conn, Long publisherSeq,
      Long providerSeq, boolean subscribed) throws DbException {
    final String DEBUG_HEADER = "updateOrCreatePublisherSubscription(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
      log.debug2(DEBUG_HEADER + "subscribed = " + subscribed);
    }

    int count = 0;

    PreparedStatement updatePublisherSubscription =
	dbManager.prepareStatement(conn, UPDATE_PUBLISHER_SUBSCRIPTION_QUERY);

    try {
      updatePublisherSubscription.setBoolean(1, subscribed);
      updatePublisherSubscription.setLong(2, publisherSeq);
      updatePublisherSubscription.setLong(3, providerSeq);

      count = dbManager.executeUpdate(updatePublisherSubscription);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update publisher subscription", sqle);
      log.error("SQL = '" + UPDATE_PUBLISHER_SUBSCRIPTION_QUERY + "'.");
      log.error("publisherSeq = " + publisherSeq);
      log.error("providerSeq = " + providerSeq);
      throw new DbException("Cannot update publisher subscription", sqle);
    } finally {
      DbManager.safeCloseStatement(updatePublisherSubscription);
    }

    if (count == 1) {
      return null;
    }

    PreparedStatement insertSubscription = dbManager.prepareStatement(conn,
	INSERT_PUBLISHER_SUBSCRIPTION_QUERY, Statement.RETURN_GENERATED_KEYS);

    ResultSet resultSet = null;
    Long publisherSubscriptionSeq = null;

    try {
      // Skip auto-increment key field #0
      insertSubscription.setLong(1, publisherSeq);
      insertSubscription.setLong(2, providerSeq);
      insertSubscription.setBoolean(3, subscribed);
      dbManager.executeUpdate(insertSubscription);
      resultSet = insertSubscription.getGeneratedKeys();

      if (!resultSet.next()) {
	String message = "Unable to create PUBLISHER_SUBSCRIPTION table row: "
	    + "publisherSeq = " + publisherSeq + "providerSeq = " + providerSeq
	    + ", subscribed = " + subscribed + " - No keys were generated.";
	log.error(message);
	throw new DbException(message);
      }

      publisherSubscriptionSeq = resultSet.getLong(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Added publisherSubscriptionSeq = "
	    + publisherSubscriptionSeq);
    } catch (SQLException sqle) {
      log.error("Cannot insert publisher subscription", sqle);
      log.error("SQL = '" + INSERT_PUBLISHER_SUBSCRIPTION_QUERY + "'.");
      log.error("publisherSeq = " + publisherSeq);
      log.error("providerSeq = " + providerSeq);
      log.error("subscribed = " + subscribed);
      throw new DbException("Cannot insert publisher subscription", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertSubscription);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "publisherSubscriptionSeq = "
	+ publisherSubscriptionSeq);
    return publisherSubscriptionSeq;
  }
}
