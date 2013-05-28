/*
 * $Id: SubscriptionManager.java,v 1.2 2013-05-28 16:31:07 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

import static org.lockss.db.DbManager.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbTitle;
import org.lockss.config.TdbUtil;
import org.lockss.db.DbManager;
import org.lockss.exporter.biblio.BibliographicUtil;
import org.lockss.metadata.MetadataManager;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.remote.RemoteApi;
import org.lockss.remote.RemoteApi.BatchAuStatus;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * Manager of serial publication subscriptions.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class SubscriptionManager extends BaseLockssDaemonManager implements
    ConfigurableManager {

  private static final Logger log = Logger.getLogger(SubscriptionManager.class);
  
  // Query to count recorded unconfigured archival units.
  private static final String UNCONFIGURED_AU_COUNT_QUERY = "select "
      + "count(*)"
      + " from " + UNCONFIGURED_AU_TABLE;
  
  // Query to find if an archival unit is in the UNCONFIGURED_AU table.
  private static final String FIND_UNCONFIGURED_AU_COUNT_QUERY = "select "
      + "count(*)"
      + " from " + UNCONFIGURED_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to add an archival unit to the UNCONFIGURED_AU table.
  private static final String INSERT_UNCONFIGURED_AU_QUERY = "insert into "
      + UNCONFIGURED_AU_TABLE
      + "(" + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + ") values (?,?)";

  // Query to remove an archival unit from the UNCONFIGURED_AU table.
  private static final String DELETE_UNCONFIGURED_AU_QUERY = "delete from "
      + UNCONFIGURED_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to find a subscription by its publication and platform.
  private static final String FIND_SUBSCRIPTION_QUERY = "select "
      + SUBSCRIPTION_SEQ_COLUMN
      + " from " + SUBSCRIPTION_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and " + PLATFORM_SEQ_COLUMN + " = ?";

  // Query to add a subscription.
  private static final String INSERT_SUBSCRIPTION_QUERY = "insert into "
      + SUBSCRIPTION_TABLE
      + "(" + SUBSCRIPTION_SEQ_COLUMN
      + "," + PUBLICATION_SEQ_COLUMN
      + "," + PLATFORM_SEQ_COLUMN
      + ") values (default,?,?)";

  // Query to find the subscription ranges of a publication.
  private static final String FIND_SUBSCRIPTION_RANGES_QUERY = "select "
      + RANGE_COLUMN
      + " from " + SUBSCRIPTION_RANGE_TABLE
      + " where " + SUBSCRIPTION_SEQ_COLUMN + " = ?"
      + " and " + SUBSCRIBED_COLUMN + " = ?";

  // Query to add a subscription range.
  private static final String INSERT_SUBSCRIPTION_RANGE_QUERY = "insert into "
      + SUBSCRIPTION_RANGE_TABLE
      + "(" + SUBSCRIPTION_SEQ_COLUMN
      + "," + RANGE_COLUMN
      + "," + SUBSCRIBED_COLUMN
      + ") values (?,?,?)";

  // Query to delete a subscription range.
  private static final String DELETE_SUBSCRIPTION_RANGE_QUERY = "delete from "
      + SUBSCRIPTION_RANGE_TABLE
      + " where " + SUBSCRIPTION_SEQ_COLUMN + " = ?"
      + " and " + RANGE_COLUMN + " = ?"
      + " and " + SUBSCRIBED_COLUMN + " = ?";

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
      + ",pl." + PLATFORM_NAME_COLUMN
      + " from " + SUBSCRIPTION_RANGE_TABLE + " sr"
      + "," + SUBSCRIPTION_TABLE + " s"
      + "," + PUBLISHER_TABLE + " pu"
      + "," + PUBLICATION_TABLE + " p"
      + "," + MD_ITEM_NAME_TABLE + " n"
      + "," + PLATFORM_TABLE + " pl"
      + " where sr." + SUBSCRIPTION_SEQ_COLUMN + " = s."
      + SUBSCRIPTION_SEQ_COLUMN
      + " and s." + PUBLICATION_SEQ_COLUMN + " = p." + PUBLICATION_SEQ_COLUMN
      + " and p." + PUBLISHER_SEQ_COLUMN + " = pu." + PUBLISHER_SEQ_COLUMN
      + " and p." + MD_ITEM_SEQ_COLUMN + " = n." + MD_ITEM_SEQ_COLUMN
      + " and s." + PLATFORM_SEQ_COLUMN + " = pl." + PLATFORM_SEQ_COLUMN
      + " order by pu." + PUBLISHER_NAME_COLUMN
      + ",n." + NAME_COLUMN
      + ",pl." + PLATFORM_NAME_COLUMN;

  // Query to find all the subscriptions and their ranges.
  private static final String FIND_ALL_SUBSCRIPTIONS_AND_RANGES_QUERY = "select"
      + " s." + SUBSCRIPTION_SEQ_COLUMN
      + ",n." + NAME_COLUMN
      + ",pr." + PUBLISHER_NAME_COLUMN
      + ",pl." + PLATFORM_NAME_COLUMN
      + ",sr." + RANGE_COLUMN
      + ",sr." + SUBSCRIBED_COLUMN
      + " from " + SUBSCRIPTION_TABLE + " s"
      + "," + PUBLICATION_TABLE + " p"
      + "," + PUBLISHER_TABLE + " pr"
      + "," + MD_ITEM_NAME_TABLE + " n"
      + "," + PLATFORM_TABLE + " pl"
      + "," + SUBSCRIPTION_RANGE_TABLE + " sr"
      + " where s." + PUBLICATION_SEQ_COLUMN + " = p." + PUBLICATION_SEQ_COLUMN
      + " and p." + PUBLISHER_SEQ_COLUMN + " = pr." + PUBLISHER_SEQ_COLUMN
      + " and p." + MD_ITEM_SEQ_COLUMN + " = n." + MD_ITEM_SEQ_COLUMN
      + " and n." + NAME_TYPE_COLUMN + " = 'primary'"
      + " and s." + PLATFORM_SEQ_COLUMN + " = pl." + PLATFORM_SEQ_COLUMN
      + " and s." + SUBSCRIPTION_SEQ_COLUMN + " = sr." + SUBSCRIPTION_SEQ_COLUMN
      + " order by pr." + PUBLISHER_NAME_COLUMN
      + ",n." + NAME_COLUMN
      + ",pl." + PLATFORM_NAME_COLUMN
      + ",sr." + RANGE_COLUMN
      + ",sr." + SUBSCRIBED_COLUMN;

  // Query to get the count of subscriptions.
  private static final String COUNT_SUBSCRIBED_PUBLICATIONS_QUERY = "select"
      + " count(*)"
      + " from " + SUBSCRIPTION_TABLE
      + " where " + SUBSCRIPTION_SEQ_COLUMN + " in ("
      + "select distinct s." + SUBSCRIPTION_SEQ_COLUMN
      + " from " + SUBSCRIPTION_TABLE + " s"
      + ", " + SUBSCRIPTION_RANGE_TABLE + " sr"
      + " where s." + SUBSCRIPTION_SEQ_COLUMN + " = sr."
      + SUBSCRIPTION_SEQ_COLUMN + ")";

  // The database manager.
  private DbManager dbManager = null;

  // The metadata manager.
  private MetadataManager mdManager = null;

  // The plugin manager.
  private PluginManager pluginManager = null;

  // The remote API.
  private RemoteApi remoteApi;

  // An indication of whether this object is ready to be used.
  private boolean ready = false;

  // The current TdbTitle when processing multiple TdbAus.
  private TdbTitle currentTdbTitle;

  // The current set of subscribed ranges when processing multiple TdbAus.
  private Collection<BibliographicPeriod> currentSubscribedRanges;

  // The current set of unsubscribed ranges when processing multiple TdbAus.
  private Collection<BibliographicPeriod> currentUnsubscribedRanges;

  // Sorter of publications.
  private static Comparator<SerialPublication> PUBLICATION_COMPARATOR =
      new Comparator<SerialPublication>() {
    public int compare(SerialPublication o1, SerialPublication o2) {
      // Sort by publication name first.
      int nameComparison = o1.getPublicationName().compareTo(
	  o2.getPublicationName());

      if (nameComparison != 0) {
	return nameComparison;
      }

      // Sort by platform name if the publication name is the same.
      return o1.getPlatformName().compareTo(o2.getPlatformName());
    }
  };

  // Sorter of subscriptions by their publications.
  private static Comparator<Subscription>
  SUBSCRIPTION_BY_PUBLICATION_COMPARATOR = new Comparator<Subscription>() {
    public int compare(Subscription o1, Subscription o2) {
      // Sort by publication name first.
      SerialPublication p1 = o1.getPublication();
      SerialPublication p2 = o2.getPublication();
      int nameComparison = p1.getPublicationName().compareTo(
	  p2.getPublicationName());

      if (nameComparison != 0) {
	return nameComparison;
      }

      // Sort by platform name if the publication name is the same.
      return p1.getPlatformName().compareTo(p2.getPlatformName());
    }
  };

  /**
   * Starts the SubscriptionManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Do nothing more if it is already initialized.
    if (ready) {
      return;
    }

    dbManager = getDaemon().getDbManager();
    pluginManager = getDaemon().getPluginManager();
    mdManager = getDaemon().getMetadataManager();
    remoteApi = getDaemon().getRemoteApi();
    ready = true;

    if (log.isDebug()) log.debug(DEBUG_HEADER
	+ "SubscriptionManager service successfully started");
  }

  /**
   * Handler of configuration changes.
   * 
   * @param newConfig
   *          A Configuration with the new configuration.
   * @param prevConfig
   *          A Configuration with the previous configuration.
   * @param changedKeys
   *          A Configuration.Differences with the keys of the configuration
   *          elements that have changed.
   */
  public void setConfig(Configuration newConfig, Configuration prevConfig,
      Configuration.Differences changedKeys) {
    final String DEBUG_HEADER = "setConfig(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // On daemon startup, perform the handling of configuration changes in its
    // own thread.
    /*if (!isReady()) {
      SubscriptionStarter starter =
	  new SubscriptionStarter(this, newConfig, prevConfig, changedKeys);
      new Thread(starter).start();
    } else {
      handleConfigurationChange(newConfig, prevConfig, changedKeys);
    }*/

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides an indication of whether this object is ready to be used.
   * 
   * @return <code>true</code> if this object is ready to be used,
   *         <code>false</code> otherwise.
   */
  public boolean isReady() {
    return ready;
  }

  /**
   * Performs the necessary work on configuration changes.
   * 
   * @param newConfig
   *          A Configuration with the new configuration.
   * @param prevConfig
   *          A Configuration with the previous configuration.
   * @param changedKeys
   *          A Configuration.Differences with the keys of the configuration
   *          elements that have changed.
   */
  void handleConfigurationChange(Configuration newConfig,
      Configuration prevConfig, Configuration.Differences changedKeys) {
    final String DEBUG_HEADER = "handleConfigurationChange(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // TODO: Revisit the default repository strategy.
    // Get the default repository.
    String defaultRepo =
	  remoteApi.findLeastFullRepository(remoteApi.getRepositoryMap());
    if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "defaultRepo = " + defaultRepo);

    Connection conn = null;
    boolean isFirstRun = false;
    String message = "Cannot connect to database";

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      message = "Cannot determine whether this is the first run of the "
	  + "subscription manager";

      // Determine whether this is the first run of the subscription manager.
      isFirstRun = countUnconfiguredAus(conn) == 0;
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "isFirstRun = " + isFirstRun);
    } catch (SQLException sqle) {
      log.error(message, sqle);
      if (log.isDebug2()) log.debug(DEBUG_HEADER + "Done.");
      return;
    }

    TdbAu tdbAu = null;
    currentTdbTitle = null;
    currentSubscribedRanges = null;
    currentUnsubscribedRanges = null;

    // Initialize the configuration used to configure the archival units.
    Configuration config = ConfigManager.newConfiguration();

    try {
      // Get access to the changed archival units.
      Iterator<TdbAu> tdbAuIterator = changedKeys.getTdbDifferences()
  	.newTdbAuIterator();

      // Loop through all the changed archival units.
      while (tdbAuIterator.hasNext()) {
	try {
	  // Get the archival unit.
	  tdbAu = tdbAuIterator.next();
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "tdbAu = " + tdbAu);

	  // Process the archival unit.
	  processNewTdbAu(tdbAu, conn, defaultRepo, isFirstRun, config);
	} catch (SQLException sqle) {
	  log.error("Error handling archival unit " + tdbAu, sqle);
	} catch (RuntimeException re) {
	  log.error("Error handling archival unit " + tdbAu, re);
	}
      }
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    // Configure the archival units.
    try {
      configureAuBatch(config);
    } catch (IOException ioe) {
      log.error("Exception caught configuring a batch of archival units. "
	  + "Config = " + config, ioe);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the count of recorded unconfigured archival units.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the count of recorded unconfigured archival units.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private long countUnconfiguredAus(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "countUnconfiguredAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    long rowCount = -1;
    ResultSet results = null;
    PreparedStatement unconfiguredAu =
	dbManager.prepareStatement(conn, UNCONFIGURED_AU_COUNT_QUERY);

    try {
      // Count the rows in the table.
      results = dbManager.executeQuery(unconfiguredAu);
      results.next();
      rowCount = results.getLong(1);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "rowCount = " + rowCount);
    } catch (SQLException sqle) {
      log.error("Cannot count unconfigured archival units", sqle);
      log.error("SQL = '" + UNCONFIGURED_AU_COUNT_QUERY + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(unconfiguredAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Performs the necessary processing for an archival unit that appears in the
   * configuration changeset.
   * 
   * @param tdbAu
   *          A TdbAu for the archival unit to be processed.
   * @param conn
   *          A Connection with the database connection to be used.
   * @param defaultRepo
   *          A String with the default repository to be used when configuring
   *          an archival unit.
   * @param isFirstRun
   *          A boolean with <code>true</code> if this is the first run of the
   *          subscription manager, <code>false</code> otherwise.
   * @param config
   *          A Configuration to which to add the archival unit configuration.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void processNewTdbAu(TdbAu tdbAu, Connection conn, String defaultRepo,
      boolean isFirstRun, Configuration config) throws SQLException {
    final String DEBUG_HEADER = "processNewTdbAu(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tdbAu = " + tdbAu);
      log.debug2(DEBUG_HEADER + "defaultRepo = " + defaultRepo);
      log.debug2(DEBUG_HEADER + "isFirstRun = " + isFirstRun);
      log.debug2(DEBUG_HEADER + "config = " + config);
    }

    // Get the archival unit identifier.
    String auId;

    try {
      auId = tdbAu.getAuId(pluginManager);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);
    } catch (IllegalStateException ise) {
      log.warning("Problem getting identifier of archival unit " + tdbAu
	  + " - Ignored", ise);
      return;
    }

    // Check whether the archival unit is already configured.
    if (pluginManager.getAuFromId(auId) != null) {
      // Yes: Nothing more to do.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "TdbAu '" + tdbAu
	  + "' is already configured.");
      return;
    }

    // Check whether this is the first run of the subscription manager.
    if (isFirstRun) {
      // Yes: Add the archival unit to the table of unconfigured archival units.
      persistUnconfiguredAu(conn, auId);
      conn.commit();
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return;
    }

    // Nothing to do if the archival unit is in the table of unconfigured
    // archival units already.
    if (isAuInUnconfiguredAuTable(conn, auId)) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return;
    }

    // Yes: Get the archival unit period.
    BibliographicPeriod period =
	new BibliographicPeriod(tdbAu.getStartYear(),
	    tdbAu.getStartVolume(), tdbAu.getStartIssue(),
	    tdbAu.getEndYear(), tdbAu.getEndVolume(), tdbAu.getEndIssue());
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "period = " + period);

    // Check whether this archival unit belongs to a different title than the
    // previous archival unit processed.
    if (!tdbAu.getTdbTitle().equals(currentTdbTitle)) {
      // Yes: Update the title data for this archival unit.
      currentTdbTitle = tdbAu.getTdbTitle();

      // Get the subscription ranges for the archival unit title.
      currentSubscribedRanges = new HashSet<BibliographicPeriod>();
      currentUnsubscribedRanges = new HashSet<BibliographicPeriod>();

      populateTitleSubscriptionRanges(conn, currentTdbTitle,
	  currentSubscribedRanges, currentUnsubscribedRanges);
    } else {
      // No: Reuse the title data from the previous archival unit.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Reusing data from title = "
	  + currentTdbTitle);
    }

    // Check whether the archival unit covers a subscribed range and it does not
    // cover any unsubscribed range.
    if (period.intersects(currentSubscribedRanges)
	&& !period.intersects(currentUnsubscribedRanges)) {
      // Yes: Add the archival unit configuration to those to be configured.
      config = addAuConfiguration(tdbAu, auId, defaultRepo, config);

      // Delete the AU from the unconfigured AU table, if it is there.
      removeFromUnconfiguredAus(conn, auId);
    } else {
      // No: Add it to the table of unconfigured archival units.
      persistUnconfiguredAu(conn, auId);
      conn.commit();
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return;
  }

  /**
   * Provides an indication of whether an Archival Unit is in the
   * UNCONFIGURED_AU table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a boolean with <code>true</code> if the Archival Unit is in the
   *         UNCONFIGURED_AU table, <code>false</code> otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private boolean isAuInUnconfiguredAuTable(Connection conn, String auId)
      throws SQLException {
    final String DEBUG_HEADER = "isAuInUnconfiguredAuTable(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    long rowCount = -1;
    ResultSet results = null;
    PreparedStatement unconfiguredAu =
	dbManager.prepareStatement(conn, FIND_UNCONFIGURED_AU_COUNT_QUERY);

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      String auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      unconfiguredAu.setString(1, pluginId);
      unconfiguredAu.setString(2, auKey);

      // Find the archival unit in the table.
      results = dbManager.executeQuery(unconfiguredAu);
      results.next();
      rowCount = results.getLong(1);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "rowCount = " + rowCount);
    } catch (SQLException sqle) {
      log.error("Cannot find archival unit in unconfigured table", sqle);
      log.error("SQL = '" + FIND_UNCONFIGURED_AU_COUNT_QUERY + "'.");
      log.error("auId = " + auId);
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(unconfiguredAu);
    }

    boolean result = rowCount > 0;
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the period covered by an archival unit in a form suitable to be
   * displayed.
   * 
   * @param au
   *          A TdbAu with the archival unit.
   * @return a String with the period covered by the archival unit in a form
   *         suitable to be displayed.
   */
  String displayableAuPeriod(TdbAu au) {
    // Get the archival unit period.
    BibliographicPeriod period = new BibliographicPeriod(au.getStartYear(),
	au.getStartVolume(), au.getStartIssue(), au.getEndYear(),
	au.getEndVolume(), au.getEndIssue());

    // Return the displayable text.
    return period.toDisplayableString();
  }

  /**
   * Populates the sets of subscribed and unsubscribed ranges for a title.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param title
   *          A TdbTitle with the title.
   * @param subscribedRanges
   *          A Collection<BibliographicPeriod> to be populated with the title
   *          subscribed ranges, if any.
   * @param unsubscribedRanges
   *          A Collection<BibliographicPeriod> to be populated with the title
   *          unsubscribed ranges, if any.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void populateTitleSubscriptionRanges(Connection conn, TdbTitle title,
      Collection<BibliographicPeriod> subscribedRanges,
      Collection<BibliographicPeriod> unsubscribedRanges) throws SQLException {
    final String DEBUG_HEADER = "populateTitleSubscriptionRanges(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "title = " + title);

    // Get the title identifier.
    String titleId = title.getId();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "titleId = " + titleId);

    // Get the title publisher.
    String publisher = title.getTdbPublisher().getName();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "publisher = " + publisher);

    // Locate the publisher database identifier.
    Long publisherSeq = mdManager.findPublisher(conn, publisher);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);

    // Check whether the publisher does not exist in the database.
    if (publisherSeq == null) {
      // Yes: There are no title subscription definitions.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return;
    }

    // Get the title name.
    String name = title.getName();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "name = " + name);

    // Get the title print ISSN.
    String pIssn = mdManager.normalizeIsbnOrIssn(title.getPrintIssn(),
	MAX_ISSN_COLUMN, "ISSN", name, publisher);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pIssn = " + pIssn);

    // Get the title electronic ISSN.
    String eIssn = mdManager.normalizeIsbnOrIssn(title.getEissn(),
	MAX_ISSN_COLUMN, "ISSN", name, publisher);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "eIssn = " + eIssn);

    // Locate the title publication in the database.
    Long publicationSeq = mdManager.findPublication(conn, pIssn, eIssn, null,
	null, publisherSeq, name, null);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    // Check whether the publication does not exist in the database.
    if (publicationSeq == null) {
      // Yes: There are no title subscription definitions.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return;
    }

    Long platformSeq;
    Long subscriptionSeq;
    
    // Loop through all the title platforms.
    for (String platform : getTitlePlatforms(title)) {
      // Find the publishing platform in the database.
      platformSeq = mdManager.findPlatform(conn, platform);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "platformSeq = " + platformSeq);

      // Check whether the platform exists in the database.
      if (platformSeq != null) {
	// Yes: Find in the database the publication subscription for the
	// platform.
	subscriptionSeq = findSubscription(conn, publicationSeq, platformSeq);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);

	// Check whether the subscription exists in the database.
	if (subscriptionSeq != null) {
	  // Yes: Find in the database the subscribed ranges and add them to the
	  // results.
	  subscribedRanges
	  	.addAll(findSubscriptionRanges(conn, subscriptionSeq, true));

	  // Find in the database the unsubscribed ranges and add them to the
	  // results.
	  unsubscribedRanges
	  	.addAll(findSubscriptionRanges(conn, subscriptionSeq, false));
	}
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Configures a batch of archival units into the system.
   * 
   * @param config
   *          A Configuration with the configuration of the archival units to be
   *          configured into the system.
   * @return a BatchAuStatus with the status of the operation.
   * @throws IOException
   *           if there are problems configuring the batch of archival units.
   */
  private BatchAuStatus configureAuBatch(Configuration config)
      throws IOException {
    final String DEBUG_HEADER = "configureAuBatch(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "config = " + config);
    BatchAuStatus status = null;

    // Check whether there are archival units to configure.
    if (!config.isEmpty()) {
      // Yes: Perform the actual configuration into the system of the archival
      // units in the configuration.
      status = remoteApi.batchAddAus(RemoteApi.BATCH_ADD_ADD, config, null);
      log.info("Successful configuration of " + status.getOkCnt() + " AUs.");

      // Check whether there are any errors.
      if (status.hasNotOk()) {
	// Yes: Report them.
	for (BatchAuStatus.Entry stat : status.getStatusList()) {
	  if (!stat.isOk()) {
	    log.error("Error configuring AU '" + stat.getName() + "': "
		+ stat.getExplanation());
	  }
	}
      }
    } else {
      // No.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "No AUs to configure.");
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
    return status;
  }

  /**
   * Adds the current configuration of an archival unit to a passed
   * configuration.
   * 
   * @param tdbAu
   *          A TdbAu with the archival unit.
   * @param auId
   *          A String with the archival unit identifier.
   * @param defaultRepo
   *          A String with the default repository.
   * @param config
   *          A Configuration to which to add the archival unit configuration.
   * @return a Configuration with the archival unit configuration added to the
   *         passed configuration.
   */
  private Configuration addAuConfiguration(TdbAu tdbAu, String auId,
      String defaultRepo, Configuration config) {
    final String DEBUG_HEADER = "addAuConfiguration(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tdbAu = " + tdbAu);
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      log.debug2(DEBUG_HEADER + "defaultRepo = " + defaultRepo);
      log.debug2(DEBUG_HEADER + "config = " + config);
    }

    Plugin plugin = tdbAu.getPlugin(pluginManager);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "pluginId = " + plugin.getPluginId());

    Map<String, String> params = tdbAu.getParams();
    Properties props = PluginManager.defPropsFromProps(plugin, params);
    Configuration auConfig = ConfigManager.fromPropertiesUnsealed(props);

    // Specify the default repository.
    auConfig.put(PluginManager.AU_PARAM_REPOSITORY, defaultRepo);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auConfig = " + auConfig);

    // Get the sub-tree prefix.
    String prefix = PluginManager.auConfigPrefix(auId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);

    // Add the archival unit configuration to the passed configuration.
    config.addAsSubTree(auConfig, prefix);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "config = " + config);
    return config;
  }

  /**
   * Removes an AU from the table of unconfigured AUs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void removeFromUnconfiguredAus(Connection conn, String auId)
      throws SQLException {
    final String DEBUG_HEADER = "removeFromUnconfiguredAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);
    PreparedStatement deleteUnconfiguredAu =
	dbManager.prepareStatement(conn, DELETE_UNCONFIGURED_AU_QUERY);

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      String auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      deleteUnconfiguredAu.setString(1, pluginId);
      deleteUnconfiguredAu.setString(2, auKey);
      int count = dbManager.executeUpdate(deleteUnconfiguredAu);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot remove AU from unconfigured table", sqle);
      log.error("SQL = '" + DELETE_UNCONFIGURED_AU_QUERY + "'.");
      log.error("auId = '" + auId + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(deleteUnconfiguredAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds an archival unit to the UNCONFIGURED_AU table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  public void persistUnconfiguredAu(Connection conn, String auId)
      throws SQLException {
    final String DEBUG_HEADER = "persistUnconfiguredAu(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);
    PreparedStatement insertUnconfiguredAu = null;

    try {
      insertUnconfiguredAu =
	  dbManager.prepareStatement(conn, INSERT_UNCONFIGURED_AU_QUERY);

      String pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      String auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      insertUnconfiguredAu.setString(1, pluginId);
      insertUnconfiguredAu.setString(2, auKey);
      int count = dbManager.executeUpdate(insertUnconfiguredAu);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot insert archival unit in unconfigured table", sqle);
      log.error("SQL = '" + INSERT_UNCONFIGURED_AU_QUERY + "'.");
      log.error("auId = " + auId);
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(insertUnconfiguredAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the platforms of a title.
   * 
   * @param title
   *          A TdbTitle with the title.
   * @return a Collection<String> with the title platforms.
   */
  private Collection<String> getTitlePlatforms(TdbTitle title) {
    final String DEBUG_HEADER = "getTitlePlatforms(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "title = " + title);

    // Get the title archival units.
    Collection<TdbAu> titleAus = title.getTdbAus();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "titleAus.size() = " + titleAus.size());

    String pluginId;
    String platform;
    Collection<String> platforms = new HashSet<String>();

    // Loop through all the title archival units.
    for (TdbAu au : titleAus) {
      // Get the plugin identifier.
      pluginId = PluginManager.pluginKeyFromName(au.getPluginId());
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);

      // Get the plugin platform.
      platform = pluginManager.getPlugin(pluginId).getPublishingPlatform();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "platform = " + platform);

      // Add it to the results if it is not null.
      if (platform != null) {
	platforms.add(platform);
      }
    }

    if (platforms.size() == 0) {
      platforms.add(NO_PLATFORM);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "platforms = " + platforms);
    return platforms;
  }

  /**
   * Provides the identifier of a subscription.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the identifier of the publication.
   * @param platformSeq
   *          A Long with the identifier of the platform.
   * @return a Long with the identifier of the subscription.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private Long findSubscription(Connection conn, Long publicationSeq,
      Long platformSeq) throws SQLException {
    final String DEBUG_HEADER = "findSubscription(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
    }

    PreparedStatement findSubscription =
	dbManager.prepareStatement(conn, FIND_SUBSCRIPTION_QUERY);
    ResultSet resultSet = null;
    Long subscriptionSeq = null;

    try {
      findSubscription.setLong(1, publicationSeq);
      findSubscription.setLong(2, platformSeq);
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
      log.error("platformSeq = " + platformSeq);
      throw sqle;
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
   * @return a Collection<BibliographicPeriod> with the ranges for the
   *         subscription.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private Collection<BibliographicPeriod> findSubscriptionRanges(
      Connection conn, Long subscriptionSeq, boolean subscribed)
      throws SQLException {
    final String DEBUG_HEADER = "findSubscriptionsRanges(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
      log.debug2(DEBUG_HEADER + "subscribed = " + subscribed);
    }

    String range;
    Collection<BibliographicPeriod> ranges = new HashSet<BibliographicPeriod>();
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
	range = resultSet.getString(RANGE_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "range = " + range);

	ranges.add(new BibliographicPeriod(range));
      }
    } catch (SQLException sqle) {
      log.error("Cannot get ranges", sqle);
      log.error("SQL = '" + query + "'.");
      log.error("subscriptionSeq = " + subscriptionSeq);
      log.error("subscribed = " + subscribed);
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(getSubscriptionRanges);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "ranges = " + ranges);
    return ranges;
  }

  /**
   * Creates subscriptions for all the archival units configured in the system.
   * 
   * @param status
   *          A SubscriptionOperationStatus through which to provide a summary
   *          of the status of the operation.
   */
  public void subscribeAllConfiguredAus(SubscriptionOperationStatus status) {
    final String DEBUG_HEADER = "subscribeAllConfiguredAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Get a connection to the database.
    Connection conn = null;

    try {
      conn = dbManager.getConnection();
    } catch (SQLException sqle) {
      log.error("Cannot obtain a database connection", sqle);
      return;
    }

    // Get the configured Archival Units.
    List<TdbAu> configuredAus = TdbUtil.getConfiguredTdbAus();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "configuredAus.size() = "
	+ configuredAus.size());

    // Get the titles with configured Archival Units.
    Collection<TdbTitle> configuredTitles = TdbUtil.getConfiguredTdbTitles();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "configuredTitles.size() = "
	    + configuredTitles.size());

    try {
      // Loop through all the titles with configured archival units.
      for (TdbTitle title : configuredTitles) {
	// Create subscriptions for all the configured archival units of this
	// title.
	subscribePublicationConfiguredAus(title, conn, configuredAus, status);
      }
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates subscriptions for the archival units of a title configured in the
   * system.
   * 
   * @param title
   *          A TdbTitle with the title.
   * @param conn
   *          A Connection with the database connection to be used.
   * @param configuredAus
   *          A List<TdbAu> with the archival units already configured in the
   *          system.
   * @param status
   *          A SubscriptionOperationStatus through which to provide a summary
   *          of the status of the operation.
   */
  private void subscribePublicationConfiguredAus(TdbTitle title,
      Connection conn, List<TdbAu> configuredAus,
      SubscriptionOperationStatus status) {
    final String DEBUG_HEADER = "subscribePublicationConfiguredAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "title = " + title);

    // Get the title publisher.
    String publisher = title.getTdbPublisher().getName();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "publisher = " + publisher);

    // Get the title name.
    String name = title.getName();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "name = " + name);

    // Get the title print ISSN.
    String pIssn = mdManager.normalizeIsbnOrIssn(title.getPrintIssn(),
	MAX_ISSN_COLUMN, "ISSN", name, publisher);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pIssn = " + pIssn);

    // Get the title electronic ISSN.
    String eIssn = mdManager.normalizeIsbnOrIssn(title.getEissn(),
	MAX_ISSN_COLUMN, "ISSN", name, publisher);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "eIssn = " + eIssn);

    // Get the title proprietary identifier.
    String proprietaryId = title.getProprietaryId();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "proprietaryId = " + proprietaryId);

    // Get the periods covered by the title currently configured archival units,
    // indexed by platform.
    Map<String, Collection<BibliographicPeriod>> periodsByPlatform =
	getTitleConfiguredPeriodsByPlatform(title, configuredAus);

    try {
      // Find the publisher in the database or create it.
      Long publisherSeq = mdManager.findOrCreatePublisher(conn, publisher);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);

      // Find the publication in the database or create it.
      Long publicationSeq = mdManager.findOrCreatePublication(conn, pIssn,
	  eIssn, null, null, publisherSeq, name, proprietaryId, null);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

      // Loop through all the platforms for which the title has archival units
      // currently configured.
      for (String platform : periodsByPlatform.keySet()) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "platform = " + platform);

	Collection<BibliographicPeriod> periods =
	    periodsByPlatform.get(platform);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "periods = " + periods);

	// Create the subscriptions for the configured archival units for the
	// publication and platform.
	subscribePublicationPlatformConfiguredAus(publicationSeq, platform,
	    periods, conn);
      }

      // Finalize all the subscription changes for this title.
      conn.commit();

      // Report the success back to the caller.
      status.addStatusEntry(name, null);
    } catch (SQLException sqle) {
      // Report the failure back to the caller.
      log.error("Cannot add/update subscription to title with Id = "
	  + title.getId(), sqle);
      status.addStatusEntry(name, false, sqle.getMessage(), null);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the periods covered by the currently configured archival units of
   * a title, indexed by platform.
   * 
   * @param title
   *          A TdbTitle with the title.
   * @param configuredAus
   *          A List<TdbAu> with the archival units already configured in the
   *          system.
   * @return a Map<String, Collection<BibliographicPeriod>> with the periods
   *         covered by the currently configured archival units of the title,
   *         indexed by platform.
   */
  private Map<String, Collection<BibliographicPeriod>>
  getTitleConfiguredPeriodsByPlatform(TdbTitle title,
      List<TdbAu> configuredAus) {
    final String DEBUG_HEADER = "getTitleConfiguredPeriodsByPlatform(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "title = " + title);

    // Get the title archival units. 
    Collection<TdbAu> titleAus = title.getTdbAus();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "titleAus.size() = " + titleAus.size());

    Map<String, Collection<BibliographicPeriod>> periodsByPlatform =
	new HashMap<String, Collection<BibliographicPeriod>>();

    String pluginId;
    String platform;
    Collection<BibliographicPeriod> periods;
    BibliographicPeriod period;

    // Loop through all the title archival units.
    for (TdbAu au : titleAus) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

      try {
	// Check whether the archival unit is configured.
	if (configuredAus.contains(au)) {
	  // Yes: Get the plugin identifier.
	  pluginId = PluginManager.pluginKeyFromName(au.getPluginId());
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);

	  // Get the plugin platform.
	  platform = pluginManager.getPlugin(pluginId).getPublishingPlatform();
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "platform = " + platform);

	  // Check whether this platform already exists in the result map.
	  if (periodsByPlatform.containsKey(platform)) {
	    // Yes: Get the collection of periods for this platform already in
	    // the result map.
	    periods = periodsByPlatform.get(platform);
	  } else {
	    // No: Initialize the collection of periods.
	    periods = new HashSet<BibliographicPeriod>();
	  }

	  // Get the archival unit period.
	  period = new BibliographicPeriod(au.getStartYear(),
	      au.getStartVolume(), au.getStartIssue(), au.getEndYear(),
	      au.getEndVolume(), au.getEndIssue());
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "period = " + period);

	  // Add it to the collection of periods.
	  periods.add(period);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "periods.size() = " + periods.size());

	  // Add the collection of periods to the result map.
	  periodsByPlatform.put(platform, periods);
	} else {
	  // No: Nothing more to do with this archival unit.
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "Unconfigured au = " + au);
	}
      } catch (RuntimeException re) {
	log.error("Cannot find the periods for AU " + au, re);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return periodsByPlatform;
  }

  /**
   * Creates subscriptions for the archival units of a title in a platform
   * configured in the system.
   * 
   * @param publicationSeq
   *          A Long with the publication identifier.
   * @param platform
   *          A String with the publication platform.
   * @param periods
   *          A Collection<BibliographicPeriod> with the periods of the archival
   *          units.
   * @param conn
   *          A Connection with the database connection to be used.
   */
  private void subscribePublicationPlatformConfiguredAus(Long publicationSeq,
      String platform, Collection<BibliographicPeriod> periods, Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "subscribePublicationPlatformConfiguredAus(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      log.debug2(DEBUG_HEADER + "platform = " + platform);
      log.debug2(DEBUG_HEADER + "periods = " + periods);
    }

    Collection<BibliographicPeriod> rangesForPeriod;

    // Find the publishing platform in the database or create it.
    Long platformSeq = mdManager.findOrCreatePlatform(conn, platform);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "platformSeq = " + platformSeq);

    // Find the subscription in the database or create it.
    Long subscriptionSeq =
	findOrCreateSubscription(conn, publicationSeq, platformSeq);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);

    // Find the subscribed ranges for this subscription.
    Collection<BibliographicPeriod> subscribedRanges =
	findSubscriptionRanges(conn, subscriptionSeq, true);

    // Find the unsubscribed ranges for this subscription.
    Collection<BibliographicPeriod> unsubscribedRanges =
	findSubscriptionRanges(conn, subscriptionSeq, false);

    // Loop through all the periods covered by the title currently configured
    // archival units for this platform.
    for (BibliographicPeriod period : periods) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "period = " + period);

      // Check whether this period is not already part of the subscribed ranges.
      if (!period.intersects(subscribedRanges)) {
	// Yes: Add it to the subscribed ranges.
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Add period.");
	subscribedRanges.add(period);
      }

      // Check whether this period is part of the unsubscribed ranges.
      if (period.intersects(unsubscribedRanges)) {
	// Yes: Find the unsubscribed ranges covering this period.
	rangesForPeriod = period.intersection(unsubscribedRanges);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "rangesForPeriod.size() = " + rangesForPeriod.size());

	// Loop through all the unsubscribed ranges covering this period.
	for (BibliographicPeriod range : rangesForPeriod) {
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "range = " + range);

	  // Delete this unsubscribed range.
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Delete range.");
	  deleteSubscriptionRange(conn, subscriptionSeq, range, false);
	  unsubscribedRanges.remove(range);
	}
      }
    }

    // Delete all the subscribed ranges.
    int deletedRangesCount =
	deleteSubscriptionTypeRanges(conn, subscriptionSeq, true);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "deletedRangesCount = " + deletedRangesCount);

    // Sort the subscribed ranges.
    List<BibliographicPeriod> sortedSubscribedRanges =
	new ArrayList<BibliographicPeriod>(subscribedRanges);
    BibliographicUtil.sortByVolumeYear(sortedSubscribedRanges);

    // Coalesce the subscribed ranges, if possible.
    List<BibliographicPeriod> coalescedSubscribedRanges =
	BibliographicPeriod.coalesce(sortedSubscribedRanges);

    // Extend the subscribed ranges into the far future.
    BibliographicPeriod.extendFuture(coalescedSubscribedRanges);

    // Loop through all the coalesced subscribed ranges covered by the title
    // currently configured archival units for this platform.
    for (BibliographicPeriod period : coalescedSubscribedRanges) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "period = " + period);

      // Persist it.
      persistSubscriptionRange(conn, subscriptionSeq, period, true);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifier of a subscription if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the identifier of the publication.
   * @param platformSeq
   *          A Long with the identifier of the platform.
   * @return a Long with the identifier of the subscription.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private Long findOrCreateSubscription(Connection conn, Long publicationSeq,
      Long platformSeq) throws SQLException {
    final String DEBUG_HEADER = "findOrCreateSubscription(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
    }

    // Locate the subscription in the database.
    Long subscriptionSeq = findSubscription(conn, publicationSeq, platformSeq);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "old subscriptionSeq = " + subscriptionSeq);

    // Check whether it is a new subscription.
    if (subscriptionSeq == null) {
      // Yes: Add to the database the new subscription.
      subscriptionSeq = persistSubscription(conn, publicationSeq, platformSeq);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "new subscriptionSeq = " + subscriptionSeq);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
    return subscriptionSeq;
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int persistSubscriptionRange(Connection conn, Long subscriptionSeq,
      BibliographicPeriod range, boolean subscribed) throws SQLException {
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

    PreparedStatement insertSubscriptionRange =
	dbManager.prepareStatement(conn, INSERT_SUBSCRIPTION_RANGE_QUERY);

    try {
      insertSubscriptionRange.setLong(1, subscriptionSeq);
      insertSubscriptionRange.setString(2, range.toDisplayableString());
      insertSubscriptionRange.setBoolean(3, subscribed);

      count = dbManager.executeUpdate(insertSubscriptionRange);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot insert subscription range", sqle);
      log.error("SQL = '" + INSERT_SUBSCRIPTION_RANGE_QUERY + "'.");
      log.error("subscriptionSeq = " + subscriptionSeq);
      log.error("range = " + range);
      log.error("subscribed = " + subscribed);
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(insertSubscriptionRange);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Deletes a subscription range from the database.
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void deleteSubscriptionRange(Connection conn, Long subscriptionSeq,
      BibliographicPeriod range, boolean subscribed) throws SQLException {
    final String DEBUG_HEADER = "deleteSubscriptionRange(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
      log.debug2(DEBUG_HEADER + "range = " + range);
      log.debug2(DEBUG_HEADER + "subscribed = " + subscribed);
    }

    int count = 0;
    PreparedStatement deleteSubscriptionRange =
	dbManager.prepareStatement(conn, DELETE_SUBSCRIPTION_RANGE_QUERY);

    try {
      deleteSubscriptionRange.setLong(1, subscriptionSeq);
      deleteSubscriptionRange.setString(2, range.toDisplayableString());
      deleteSubscriptionRange.setBoolean(3, subscribed);

      count = dbManager.executeUpdate(deleteSubscriptionRange);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete subscription range", sqle);
      log.error("SQL = '" + DELETE_SUBSCRIPTION_RANGE_QUERY + "'.");
      log.error("subscriptionSeq = " + subscriptionSeq);
      log.error("range = " + range);
      log.error("subscribed = " + subscribed);
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(deleteSubscriptionRange);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int deleteSubscriptionTypeRanges(Connection conn,
      Long subscriptionSeq, boolean subscribed) throws SQLException {
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
      throw sqle;
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
   * @param platformSeq
   *          A Long with the identifier of the platform.
   * @return a Long with the identifier of the subscription just added.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private Long persistSubscription(Connection conn, Long publicationSeq,
      Long platformSeq) throws SQLException {
    final String DEBUG_HEADER = "persistSubscription(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
    }

    PreparedStatement insertSubscription = dbManager.prepareStatement(conn,
	INSERT_SUBSCRIPTION_QUERY, Statement.RETURN_GENERATED_KEYS);

    ResultSet resultSet = null;
    Long subscriptionSeq = null;

    try {
      // Skip auto-increment key field #0
      insertSubscription.setLong(1, publicationSeq);
      insertSubscription.setLong(2, platformSeq);
      dbManager.executeUpdate(insertSubscription);
      resultSet = insertSubscription.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create SUBSCRIPTION table row: publicationSeq = "
	    + publicationSeq + ", platformSeq = " + platformSeq
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
      log.error("platformSeq = " + platformSeq);
      throw sqle;
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  public List<Subscription> findAllSubscriptionsAndRanges()
      throws SQLException {
    final String DEBUG_HEADER = "findAllSubscriptionsAndRanges(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    Long subscriptionSeq;
    String publicationName;
    String publisherName;
    String platformName;
    String ranges;
    boolean subscribed;
    SerialPublication publication;
    Subscription subscription = new Subscription();
    List<Subscription> subscriptions = new ArrayList<Subscription>();

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();
    } catch (SQLException sqle) {
      log.error("Cannot connect to database", sqle);
      throw sqle;
    }

    String query = FIND_ALL_SUBSCRIPTIONS_AND_RANGES_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "SQL = " + query);

    PreparedStatement getAllSubscriptionRanges =
	dbManager.prepareStatement(conn, query);
    ResultSet resultSet = null;

    try {
      // Get all the subscriptions and ranges from the database.
      resultSet = dbManager.executeQuery(getAllSubscriptionRanges);

      // Loop  through all the results.
      while (resultSet.next()) {
	subscriptionSeq = resultSet.getLong(SUBSCRIPTION_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);

	publicationName = resultSet.getString(NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

	publisherName = resultSet.getString(PUBLISHER_NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	platformName = resultSet.getString(PLATFORM_NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "platformName = " + platformName);

	ranges = resultSet.getString(RANGE_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "range = " + ranges);

	subscribed = resultSet.getBoolean(SUBSCRIBED_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "subscribed = " + subscribed);

	// Convert the text ranges into a collection of bibliographic periods.
	Collection<BibliographicPeriod> periods =
	    BibliographicPeriod.createCollection(ranges);

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
	  publication.setPublisherName(publisherName);
	  publication.setPlatformName(platformName);

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
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(getAllSubscriptionRanges);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "subscriptions.size() = "
	+ subscriptions.size());
    return subscriptions;
  }

  /**
   * Provides a count of the publications with subscriptions.
   *
   * @return a long with the count of the publications with subscriptions.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  public long countSubscribedPublications() throws SQLException {
    final String DEBUG_HEADER = "countSubscribedPublications(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    long result = 0;

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();
    } catch (SQLException sqle) {
      log.error("Cannot connect to database", sqle);
      throw sqle;
    }

    String query = COUNT_SUBSCRIBED_PUBLICATIONS_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "SQL = " + query);

    PreparedStatement countSubscriptions =
	dbManager.prepareStatement(conn, query);
    ResultSet resultSet = null;

    try {
      resultSet = dbManager.executeQuery(countSubscriptions);
      resultSet.next();
      result = resultSet.getLong(1);
    } catch (SQLException sqle) {
      log.error("Cannot count subscribed publications", sqle);
      log.error("SQL = '" + query + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(countSubscriptions);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the publications for which subscription decisions have not been
   * made yet.
   * 
   * @return A List<SerialPublication> with the publications for which
   *         subscription decisions have not been made yet.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  public List<SerialPublication> getUndecidedPublications()
      throws SQLException {
    final String DEBUG_HEADER = "getUndecidedPublications(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    List<SerialPublication> unsubscribedPublications =
	new ArrayList<SerialPublication>();

    // Get the existing subscriptions with publisher names.
    MultiValueMap subscriptionMap =
	mapSubscriptionsByPublisher(findAllSubscriptionsAndPublishers());

    Collection<Subscription> publisherSubscriptions = null;
    String publisherName;
    String titleName;
    Set<String> pluginIds;
    Plugin plugin;
    Set<String> platformNames;
    int platformCount = 0;
    SerialPublication publication;
    int publicationNumber = 1;

    // Loop through all the publishers.
    for (TdbPublisher publisher :
      TdbUtil.getTdb().getAllTdbPublishers().values()) {
      publisherName = publisher.getName();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

      // Get the subscribed publications that belong to the publisher.
      publisherSubscriptions =
	  (Collection<Subscription>)subscriptionMap.get(publisherName);
      if (log.isDebug3()) {
	if (publisherSubscriptions != null) {
	  log.debug3(DEBUG_HEADER + "publisherSubscriptions.size() = "
	      + publisherSubscriptions.size());
	} else {
	  log.debug3(DEBUG_HEADER + "publisherSubscriptions is null.");
	}
      }

      // Loop through all the titles (subscribed or not) of the publisher.
      for (TdbTitle title : publisher.getTdbTitles()) {
	titleName = title.getName();
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "titleName = " + titleName);

	pluginIds = new HashSet<String>();
	platformNames = new HashSet<String>();

	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "title.getTdbAus().size() = " + title.getTdbAus().size());

	// Get all the plugin identifiers for all the archival units of the
	// title.
	for (TdbAu au : title.getTdbAus()) {
	  pluginIds.add(au.getPluginId());
	}

	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "pluginIds.size() = " + pluginIds.size());

	// Get all the platforms for all the plugins used by the title.
	for (String pluginId : pluginIds) {
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);

	  plugin = pluginManager.getPlugin(pluginId);

	  if (plugin != null) {
	    platformNames.add(plugin.getPublishingPlatform());
	  }
	}

	// If no platform is found, use the dummy 'NO PLATFORM' one.
	platformCount = platformNames.size();
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "platformCount = " + platformCount);

	if (platformCount == 0) {
	  platformNames.add(NO_PLATFORM);
	}

	// Loop through all the title platforms. 
	for (String platformName : platformNames) {
	  // Check whether there is no subscription defined for this title and
	  // this platform.
	  if (publisherSubscriptions == null
	      || !matchSubscriptionTitleAndPlatform(publisherSubscriptions,
		  titleName, platformName)) {
	    // Yes: Add the publication to the list of publications with no
	    // subscriptions.
	    publication = new SerialPublication();
	    publication.setPublicationNumber(publicationNumber++);
	    publication.setPublicationName(titleName);
	    publication.setPlatformName(platformName);
	    publication.setPublisherName(publisherName);
	    publication.setPissn(title.getPrintIssn());
	    publication.setEissn(title.getEissn());
	    publication.setProprietaryId(title.getProprietaryId());
	    publication.setTdbTitle(title);

	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "publication = " + publication);

	    unsubscribedPublications.add(normalizePublication(publication));
	  }
	}
      }
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "unsubscribedPublications.size() = "
	+ unsubscribedPublications.size());

    // Sort the publications for displaying purposes.
    Collections.sort(unsubscribedPublications, PUBLICATION_COMPARATOR);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER
	+ "unsubscribedPublications.size() = "
	+ unsubscribedPublications.size());
    return unsubscribedPublications;
  }

  /**
   * Provides all the subscriptions and their publishers.
   * 
   * @return a List<Subscription> with the subscriptions and their publishers.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private List<Subscription> findAllSubscriptionsAndPublishers()
      throws SQLException {
    final String DEBUG_HEADER = "findAllSubscriptionsAndPublishers(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String publicationName;
    String platformName;
    String publisherName;
    SerialPublication publication;
    Subscription subscription;
    List<Subscription> subscriptions = new ArrayList<Subscription>();

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();
    } catch (SQLException sqle) {
      log.error("Cannot connect to database", sqle);
      throw sqle;
    }

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

	platformName = resultSet.getString(PLATFORM_NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "platformName = " + platformName);

	publication = new SerialPublication();
	publication.setPublisherName(publisherName);
	publication.setPublicationName(publicationName);
	publication.setPlatformName(platformName);

	subscription = new Subscription();
	subscription.setPublication(publication);

	subscriptions.add(subscription);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get subscriptions and publishers", sqle);
      log.error("SQL = '" + query + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(getAllSubscriptionsAndPublishers);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "subscriptions.size() = "
	+ subscriptions.size());
    return subscriptions;
  }

  /**
   * Provides the subscriptions in the system keyed by their publisher.
   * 
   * @param subscriptions
   *          A List<Subscription> with the subscriptions in the system.
   * @return a MultivalueMap with the subscriptions in the system keyed by their
   *         publisher.
   */
  private MultiValueMap mapSubscriptionsByPublisher(
      List<Subscription> subscriptions) {
    final String DEBUG_HEADER = "mapSubscriptionsByPublisher(): ";
    if (log.isDebug2()) {
      if (subscriptions != null) {
	log.debug2(DEBUG_HEADER + "subscriptions.size() = "
	    + subscriptions.size());
      } else {
	log.debug2(DEBUG_HEADER + "subscriptions is null");
      }
    }

    MultiValueMap mapByPublisher = new MultiValueMap();

    // Loop through all the subscriptions.
    for (Subscription subscription : subscriptions) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "subscription = " + subscription);

      // Get the subscription publication.
      SerialPublication publication = subscription.getPublication();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publication = " + publication);

      // Get the publication publisher name.
      String publisherName = publication.getPublisherName();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

      // Save the publisher subscription.
      mapByPublisher.put(publisherName, subscription);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Added subscription "
	  + subscription + " for publisher " + publisherName);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mapByPublisher.size() = "
	+ mapByPublisher.size());
    return mapByPublisher;
  }

  /**
   * Provides an indication of whether there is a subscription defined for this
   * title and this platform.
   * 
   * @param subscriptions
   *          A Collection<Subscription> with all the subscriptions.
   * @param titleName
   *          A String with the name of the title.
   * @param platformName
   *          A String with the name of the platform.
   * @return a boolean with <code>true</code> if the subscription for the title
   *         and platform exists, <code>false</code> otherwise.
   */
  private boolean matchSubscriptionTitleAndPlatform(
      Collection<Subscription> subscriptions, String titleName,
      String platformName) {
    final String DEBUG_HEADER = "matchSubscriptionTitleAndPlatform(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscriptions = " + subscriptions);
      log.debug2(DEBUG_HEADER + "titleName = " + titleName);
      log.debug2(DEBUG_HEADER + "platformName = " + platformName);
    }

    // Handle the case when there are no subscriptions.
    if (subscriptions == null || subscriptions.size() < 1) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Did not find match.");
      return false;
    }

    SerialPublication publication;

    // Loop through all the subscriptions.
    for (Subscription subscription : subscriptions) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "subscription = " + subscription);

      // Get the subscription publication.
      publication = subscription.getPublication();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publication = " + publication);

      // Check whether there is a match.
      if (publication.getPublicationName().equals(titleName)
	  && publication.getPlatformName().equals(platformName)) {
	// Yes: No need for further checking.
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Found match.");
	return true;
      }
    }

    // No match was found.
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Did not find match.");
    return false;
  }

  /**
   * Normalizes publication data.
   * 
   * @param publication A SerialPublication with the publication data.
   *          the ArticleMetadataInfo
   * @return a SerialPublication with the normalized publication data.
   */
  private SerialPublication normalizePublication(
      SerialPublication publication) {
    final String DEBUG_HEADER = "normalizePublication(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publication = " + publication);

    // Normalize the platform name, if necessary.
    if (!StringUtil.isNullString(publication.getPlatformName())) {
      if (publication.getPlatformName().length() > MAX_PLATFORM_COLUMN) {
	log.warning("platform too long '" + publication.getPlatformName()
	    + "' for title: '" + publication.getPublicationName()
	    + "' publisher: " + publication.getPublisherName() + "'");
	publication.setPlatformName(DbManager.truncateVarchar(
	    publication.getPlatformName(), MAX_PLATFORM_COLUMN));
      }
    }

    // Normalize the print ISSN, if necessary.
    if (!StringUtil.isNullString(publication.getPissn())) {
      String issn = publication.getPissn().replaceAll("-", "");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = '" + issn + "'.");

      if (issn.length() > MAX_ISSN_COLUMN) {
	log.warning("issn too long '" + publication.getPissn()
	    + "' for title: '" + publication.getPublicationName()
	    + "' publisher: " + publication.getPublisherName() + "'");
	publication.setPissn(DbManager.truncateVarchar(issn, MAX_ISSN_COLUMN));
      } else {
	publication.setPissn(issn);
      }
    }

    // Normalize the electronic ISSN, if necessary.
    if (!StringUtil.isNullString(publication.getEissn())) {
      String issn = publication.getEissn().replaceAll("-", "");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = '" + issn + "'.");

      if (issn.length() > MAX_ISSN_COLUMN) {
	log.warning("issn too long '" + publication.getEissn()
	    + "' for title: '" + publication.getPublicationName()
	    + "' publisher: " + publication.getPublisherName() + "'");
	publication.setEissn(DbManager.truncateVarchar(issn, MAX_ISSN_COLUMN));
      } else {
	publication.setEissn(issn);
      }
    }

    // Normalize the proprietary identifier, if necessary.
    if (!StringUtil.isNullString(publication.getProprietaryId())) {
      if (publication.getProprietaryId().length() > MAX_PUBLICATION_ID_COLUMN) {
	log.warning("proprietaryId too long '" + publication.getProprietaryId()
	    + "' for title: '" + publication.getPublicationName()
	    + "' publisher: " + publication.getPublisherName() + "'");
	publication.setProprietaryId(DbManager.truncateVarchar(
	    publication.getProprietaryId(), MAX_PUBLICATION_ID_COLUMN));
      }
    }

    // Normalize the publisher name, if necessary.
    if (!StringUtil.isNullString(publication.getPublisherName())) {
      if (publication.getPublisherName().length() > MAX_NAME_COLUMN) {
	log.warning("publisher too long '" + publication.getPublisherName()
	    + "' for title: '" + publication.getPublicationName() + "'");
	publication.setPublisherName(DbManager.truncateVarchar(
	    publication.getPublisherName(), MAX_NAME_COLUMN));
      }
    }

    // Normalize the publication name, if necessary.
    if (!StringUtil.isNullString(publication.getPublicationName())) {
      if (publication.getPublicationName().length() > MAX_NAME_COLUMN) {
	log.warning("title too long '" + publication.getPublicationName()
	    + "' for publisher: " + publication.getPublisherName() + "'");
	publication.setPublicationName(DbManager.truncateVarchar(
	    publication.getPublicationName(), MAX_NAME_COLUMN));
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publication = " + publication);
    return publication;
  }

  /**
   * Provides an indication of whether the passed subscription ranges are valid.
   * 
   * @param subscriptionRanges
   *          A Collection<BibliographicPeriod> with the subscription ranges to
   *          be validated.
   * @return a boolean with <code>true</code> if all the passed subscription
   *         ranges are valid, <code>false</code> otherwise.
   */
  public boolean areAllRangesValid(
      Collection<BibliographicPeriod> subscriptionRanges) {
    final String DEBUG_HEADER = "areAllRangesValid(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscriptionRanges = " + subscriptionRanges);

    // Loop through all  the subscription ranges.
    for (BibliographicPeriod range : subscriptionRanges) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "range = " + range);

      // Check whether this range is not valid.
      if (!range.isEmpty() && !isRangeValid(range)) {
	// Yes: Report the problem.
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is false.");
	return false;
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
    return true;
  }

  /**
   * Provides an indication of whether the passed subscription range is valid.
   * 
   * @param range
   *          A String with the subscription range to be validated.
   * @return a boolean with <code>true</code> if the passed subscription range
   *         is valid, <code>false</code> otherwise.
   */
  private boolean isRangeValid(BibliographicPeriod range) {
    final String DEBUG_HEADER = "isRangeValid(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "range = " + range);

    // Get the range textual definition.
    String text = range.toDisplayableString();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "text = " + text);

    // Check whether the range textual definition is too long for the database.
    if (range.toDisplayableString().length() > MAX_RANGE_COLUMN) {
      // Yes: Report the problem.
      log.error("Invalid length (" + range.toDisplayableString().length()
	  + ") for range '" + range.toDisplayableString() + "'.");
      return false;
    }

    // Check whether the normalized range is valid.
    if (!BibliographicUtil.isRange(range.normalize().toCanonicalString())) {
      // No: Report the problem.
      log.error("Range '" + range.toDisplayableString()
	  + "' does not fit the expected pattern.");
      return false;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
    return true;
  }

  /**
   * Adds subscriptions to the system.
   * 
   * @param subscriptions
   *          A Collection<Subscription> with the subscriptions to be added.
   * @param status
   *          A SubscriptionOperationStatus where to return the status of the
   *          operation.
   */
  public void addSubscriptions(Collection<Subscription> subscriptions,
      SubscriptionOperationStatus status) {
    final String DEBUG_HEADER = "addSubscriptions(): ";
    if (log.isDebug2()) {
      if (subscriptions != null) {
	log.debug2(DEBUG_HEADER + "subscriptions.size() = "
	    + subscriptions.size());
      } else {
	log.debug2(DEBUG_HEADER + "subscriptions is null");
      }
    }

    // Get the default repository.
    String defaultRepo =
	remoteApi.findLeastFullRepository(remoteApi.getRepositoryMap());
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "defaultRepo = " + defaultRepo);

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();
    } catch (SQLException sqle) {
      log.error("Cannot connect to database", sqle);

      for (Subscription subscription : subscriptions) {
	status.addStatusEntry(subscription.getPublication()
	    .getPublicationName(), false, sqle.getMessage(), null);
      }

      if (log.isDebug2()) log.debug(DEBUG_HEADER + "Done.");
      return;
    }

    BatchAuStatus bas;

    try {
      // Loop through all the subscriptions.
      for (Subscription subscription : subscriptions) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "subscription = " + subscription);

	try {
	  // Persist the subscription in the database.
	  persistSubscription(conn, subscription);

	  Collection<BibliographicPeriod> subscribedRanges =
	      subscription.getSubscribedRanges();
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "subscribedRanges = " + subscribedRanges);

	  // Check whether the added subscription may imply the configuration of
	  // some archival unit.
	  if (subscribedRanges != null
	      && subscribedRanges.size() > 0
	      && (subscribedRanges.size() > 1
	          || !subscribedRanges.iterator().next().isEmpty())) {
	    // Yes: Configure the archival units that correspond to this
	    // subscription.
	    bas = configureAus(conn, subscription, defaultRepo);
	  } else {
	    bas = null;
	  }

	  conn.commit();
	  status.addStatusEntry(subscription.getPublication()
	      .getPublicationName(), bas);
	} catch (IllegalStateException ise) {
	  conn.rollback();
	  log.error("Cannot add subscription " + subscription, ise);
	  status.addStatusEntry(subscription.getPublication()
	      .getPublicationName(), false, ise.getMessage(), null);
	} catch (IOException ioe) {
	  conn.rollback();
	  log.error("Cannot add subscription " + subscription, ioe);
	  status.addStatusEntry(subscription.getPublication()
	      .getPublicationName(), false, ioe.getMessage(), null);
	} catch (SQLException sqle) {
	  conn.rollback();
	  log.error("Cannot add subscription " + subscription, sqle);
	  status.addStatusEntry(subscription.getPublication()
	      .getPublicationName(), false, sqle.getMessage(), null);
	} catch (SubscriptionException se) {
	  conn.rollback();
	  log.error("Cannot add subscription " + subscription, se);
	  status.addStatusEntry(subscription.getPublication()
	      .getPublicationName(), false, se.getMessage(), null);
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot rollback the connection", sqle);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Persists a subscription in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscription
   *          A Subscription with the subscription to be persisted.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void persistSubscription(Connection conn, Subscription subscription)
      throws SQLException {
    final String DEBUG_HEADER = "persistSubscription(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscription = " + subscription);

    // Get the subscription ranges.
    Collection<BibliographicPeriod> subscribedRanges =
	subscription.getSubscribedRanges();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "subscribedRanges = " + subscribedRanges);

    Collection<BibliographicPeriod> unsubscribedRanges =
	subscription.getUnsubscribedRanges();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "unsubscribedRanges = " + unsubscribedRanges);

    // Do nothing more if there are no subscription ranges.
    if ((subscribedRanges == null || subscribedRanges.size() < 1)
	&& (unsubscribedRanges == null || unsubscribedRanges.size() < 1)) {
      return;
    }

    // Get the subscription publication.
    SerialPublication publication = subscription.getPublication();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publication = " + publication);

    // Find the publisher in the database or create it.
    Long publisherSeq =
	mdManager.findOrCreatePublisher(conn,  publication.getPublisherName());
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);

    // Find the publication in the database or create it.
    Long publicationSeq = mdManager.findOrCreatePublication(conn,
	publication.getPissn(), publication.getEissn(), null, null,
	publisherSeq, publication.getPublicationName(),
	publication.getProprietaryId(), null);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    // Find the publishing platform in the database or create it.
    Long platformSeq =
	mdManager.findOrCreatePlatform(conn, publication.getPlatformName());
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "platformSeq = " + platformSeq);

    // Find the subscription in the database or create it.
    Long subscriptionSeq =
	findOrCreateSubscription(conn, publicationSeq, platformSeq);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);

    // Persist the subscribed ranges.
    int count =
	persistSubscribedRanges(conn, subscriptionSeq, subscribedRanges);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "Added " + count + " subscribed ranges.");

    // Persist the un subscribed ranges.
    count =
	persistUnsubscribedRanges(conn, subscriptionSeq, unsubscribedRanges);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "Added " + count + " unsubscribed ranges.");

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Configures the archival units covered by a subscription.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscription
   *          A Subscription with the subscription involved.
   * @param defaultRepo
   *          A String with the default repository to be used.
   * @return a BatchAuStatus with the status of the operation.
   * @throws IOException
   *           if there are problems configuring the archival units.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   * @throws SubscriptionException
   *           if there are problems with the subscription publication.
   */
  BatchAuStatus configureAus(Connection conn, Subscription subscription,
      String defaultRepo) throws IOException, SQLException,
      SubscriptionException {
    final String DEBUG_HEADER = "configureAus(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscription = " + subscription);
      log.debug2(DEBUG_HEADER + "defaultRepo = " + defaultRepo);
    }

    // Get the subscribed ranges.
    Collection<BibliographicPeriod> subscribedRanges =
	subscription.getSubscribedRanges();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "subscribedRanges = " + subscribedRanges);

    // Do nothing more if there are no subscribed ranges.
    if (subscribedRanges == null || subscribedRanges.size() < 1) {
      return null;
    }

    // Get the susbscription publication.
    SerialPublication publication = subscription.getPublication();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publication = " + publication);

    // Check whether the publication has no TdbTitle.
    if (publication.getTdbTitle() == null) {
      // Yes: Get the publication name.
      String publicationName = publication.getPublicationName();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "publicationName = "
	  + publicationName);

      // Get the TdbTitles for the publication name.
      Collection<TdbTitle> tdbTitles =
	  TdbUtil.getTdb().getTdbTitlesByName(publicationName);

      // Check whether a TdbTitle was found.
      if (tdbTitles != null && tdbTitles.size() > 0) {
	// Yes: Populate it into the publication.
	publication.setTdbTitle(tdbTitles.iterator().next());
      } else {
	// No: Report the problem.
	String message =
	    "Cannot find tdbTitle with name '" + publicationName + "'.";
	log.error(message);
	throw new SubscriptionException(message);
      }
    }

    // Configure the archival units.
    return configureAus(conn, publication, subscribedRanges,
	subscription.getUnsubscribedRanges(), defaultRepo);
  }

  /**
   * Persists the subscribed ranges of a subscription in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscriptionSeq
   *          A Long with the subscription identifier.
   * @param subscribedRanges
   *          A Collection<BibliographicPeriod> with the subscription subscribed
   *          ranges to be persisted.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int persistSubscribedRanges(Connection conn, Long subscriptionSeq,
      Collection<BibliographicPeriod> subscribedRanges) throws SQLException {
    int count = 0;

    if (subscribedRanges != null) {
      // Persist the subscribed ranges.
      for (BibliographicPeriod range : subscribedRanges) {
	count += persistSubscriptionRange(conn, subscriptionSeq, range, true);
      }
    }

    return count;
  }

  /**
   * Persists the unsubscribed ranges of a subscription in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscriptionSeq
   *          A Long with the subscription identifier.
   * @param unsubscribedRanges
   *          A Collection<BibliographicPeriod> with the subscription
   *          unsubscribed ranges to be persisted.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int persistUnsubscribedRanges(Connection conn, Long subscriptionSeq,
      Collection<BibliographicPeriod> unsubscribedRanges) throws SQLException {
    int count = 0;

    if (unsubscribedRanges != null) {
      // Persist the unsubscribed ranges.
      for (BibliographicPeriod range : unsubscribedRanges) {
	count += persistSubscriptionRange(conn, subscriptionSeq, range, false);
      }
    }

    return count;
  }

  /**
   * Configures the archival units covered by the subscribed ranges of a
   * publication and not covered by the unsubscribed ranges.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publication
   *          A SerialPublication with the publication involved.
   * @param subscribedRanges
   *          A Collection<BibliographicPeriod> with the subscribed ranges of
   *          the publication.
   * @param unsubscribedRanges
   *          A Collection<BibliographicPeriod> with the unsubscribed ranges of
   *          the publication.
   * @param defaultRepo
   *          A String with the default repository to be used.
   * @return a BatchAuStatus with the status of the operation.
   * @throws IOException
   *           if there are problems configuring the archival units.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private BatchAuStatus configureAus(Connection conn,
      SerialPublication publication,
      Collection<BibliographicPeriod> subscribedRanges,
      Collection<BibliographicPeriod> unsubscribedRanges, String defaultRepo)
      throws IOException, SQLException {
    final String DEBUG_HEADER = "configureAus(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publication = " + publication);
      log.debug2(DEBUG_HEADER + "subscribedRanges = " + subscribedRanges);
      log.debug2(DEBUG_HEADER + "unsubscribedRanges = " + unsubscribedRanges);
      log.debug2(DEBUG_HEADER + "defaultRepo = " + defaultRepo);
    }

    // Get the publication archival units.
    Collection<TdbAu> tdbAus = publication.getTdbTitle().getTdbAus();

    // Do nothing more if the publication has no archival units.
    if (tdbAus == null || tdbAus.size() < 1) {
      return null;
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "tdbAus.size() = " + tdbAus.size());

    String auId;
    ArchivalUnit au;

    // Initialize the configuration used to configure the archival units.
    Configuration config = ConfigManager.newConfiguration();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "config = " + config);

    // Loop through all the publication TDB archival units.
    for (TdbAu tdbAu : tdbAus) {
      // Get the archival unit identifier.
      auId = tdbAu.getAuId(pluginManager);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

      // Get the archival unit.
      au = pluginManager.getAuFromId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

      // Check whether the archival unit is not active
      if (au == null || !pluginManager.isActiveAu(au)) {

	// Yes: Get the archival unit period.
	BibliographicPeriod period =
	    new BibliographicPeriod(tdbAu.getStartYear(),
		tdbAu.getStartVolume(), tdbAu.getStartIssue(),
		tdbAu.getEndYear(), tdbAu.getEndVolume(), tdbAu.getEndIssue());
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "period = " + period);

	// Check whether the period intersects any subscribed range and it does
	// not intersect any of the unsubscribed ranges.
	if (period.intersects(subscribedRanges)
	    && !period.intersects(unsubscribedRanges)) {

	  // Yes: Add the the archival unit to the configuration of those to be
	  // configured.
	  config = addAuConfiguration(tdbAu, auId, defaultRepo, config);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "config = " + config);

	  // Delete the AU from the unconfigured AU table, if it is there.
	  removeFromUnconfiguredAus(conn, auId);
	}
      }
    }

    // Configure the archival units that are covered by the subscribed ranges
    // and not covered by the unsubscribed ranges.
    return configureAuBatch(config);
  }

  /**
   * Updates existing subscriptions.
   * 
   * @param subscriptions
   *          A Collection<Subscription> with the subscriptions to be updated.
   * @param status
   *          A SubscriptionOperationStatus where to return the status of the
   *          operation.
   */
  public void updateSubscriptions(Collection<Subscription> subscriptions,
      SubscriptionOperationStatus status) {
    final String DEBUG_HEADER = "updateSubscriptions(): ";
    if (log.isDebug2()) {
      if (subscriptions != null) {
	log.debug2(DEBUG_HEADER + "subscriptions.size() = "
	    + subscriptions.size());
      } else {
	log.debug2(DEBUG_HEADER + "subscriptions is null");
      }
    }

    // Get the default repository.
    String defaultRepo =
	remoteApi.findLeastFullRepository(remoteApi.getRepositoryMap());
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "defaultRepo = " + defaultRepo);

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();
    } catch (SQLException sqle) {
      log.error("Cannot connect to database", sqle);

      for (Subscription subscription : subscriptions) {
	status.addStatusEntry(subscription.getPublication()
	    .getPublicationName(), false, sqle.getMessage(), null);
      }

      if (log.isDebug2()) log.debug(DEBUG_HEADER + "Done.");
      return;
    }

    BatchAuStatus bas;

    try {
      // Loop through all the subscriptions.
      for (Subscription subscription : subscriptions) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "subscription = " + subscription);

	try {
	  // Update the subscription in the database.
	  updateSubscription(conn, subscription);

	  Collection<BibliographicPeriod> subscribedRanges =
	      subscription.getSubscribedRanges();
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "subscribedRanges = " + subscribedRanges);

	  // Check whether the updated subscription may imply the configuration
	  // of some archival unit.
	  if (subscribedRanges != null
	      && subscribedRanges.size() > 0
	      && (subscribedRanges.size() > 1
	          || !subscribedRanges.iterator().next().isEmpty())) {
	    // Yes: Configure the archival units that correspond to this
	    // subscription.
	    bas = configureAus(conn, subscription, defaultRepo);
	  } else {
	    bas = null;
	  }

	  conn.commit();
	  status.addStatusEntry(subscription.getPublication()
	      .getPublicationName(), bas);
	} catch (IllegalStateException ise) {
	  conn.rollback();
	  log.error("Cannot update subscription " + subscription, ise);
	  status.addStatusEntry(subscription.getPublication()
	      .getPublicationName(), false, ise.getMessage(), null);
	} catch (IOException ioe) {
	  conn.rollback();
	  log.error("Cannot update subscription " + subscription, ioe);
	  status.addStatusEntry(subscription.getPublication()
	      .getPublicationName(), false, ioe.getMessage(), null);
	} catch (SQLException sqle) {
	  conn.rollback();
	  log.error("Cannot update subscription " + subscription, sqle);
	  status.addStatusEntry(subscription.getPublication()
	      .getPublicationName(), false, sqle.getMessage(), null);
	} catch (SubscriptionException se) {
	  conn.rollback();
	  log.error("Cannot update subscription " + subscription, se);
	  status.addStatusEntry(subscription.getPublication()
	      .getPublicationName(), false, se.getMessage(), null);
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot rollback the connection", sqle);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates a subscriptions in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscription
   *          A Subscription with the subscription to be persisted.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void updateSubscription(Connection conn, Subscription subscription)
      throws SQLException {
    final String DEBUG_HEADER = "updateSubscription(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscription = " + subscription);

    // Get the subscription identifier.
    Long subscriptionSeq = subscription.getSubscriptionSeq();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);

    // Delete all the subscription ranges.
    int deletedRangesCount = deleteAllSubscriptionRanges(conn, subscriptionSeq);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "deletedRangesCount = " + deletedRangesCount);

    // Get the subscribed ranges.
    Collection<BibliographicPeriod> ranges = subscription.getSubscribedRanges();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "subscribedRanges = " + ranges);

    // Persist in the database the updated subscribed ranges.
    persistSubscriptionRanges(conn, subscriptionSeq, ranges, true);

    // Get the unsubscribed ranges.
    ranges = subscription.getUnsubscribedRanges();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "unsubscribedRanges = " + ranges);

    // Persist in the database the updated unsubscribed ranges.
    persistSubscriptionRanges(conn, subscriptionSeq, ranges, false);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes all the ranges of a given subscription.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscriptionSeq
   *          A Long with the identifier of the subscription.
   * @return an int with the number of deleted rows.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int deleteAllSubscriptionRanges(Connection conn, Long subscriptionSeq)
      throws SQLException {
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
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(deleteAllSubscriptionRanges);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Persists ranges of a given subscription.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscriptionSeq
   *          A Long with the identifier of the subscription.
   * @param ranges
   *          A Collection<BibliographicPeriod> with the subscription ranges.
   * @param subscribed
   *          A boolean with the indication of whether the LOCKSS installation
   *          is subscribed to the publication range or not.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void persistSubscriptionRanges(Connection conn, Long subscriptionSeq,
      Collection<BibliographicPeriod> ranges, boolean subscribed)
      throws SQLException {
    final String DEBUG_HEADER = "persistSubscriptionRanges(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
      log.debug2(DEBUG_HEADER + "ranges = " + ranges);
      log.debug2(DEBUG_HEADER + "subscribed = " + subscribed);
    }

    // Loop through the ranges to be persisted.
    if (ranges != null) {
      for (BibliographicPeriod range : ranges) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "range = " + range);
	persistSubscriptionRange(conn, subscriptionSeq, range, subscribed);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the sorter of publications.
   * 
   * @return a Comparator<SerialPublication> with the sorter of publications.
   */
  public Comparator<SerialPublication> getPublicationComparator() {
    return PUBLICATION_COMPARATOR;
  }

  /**
   * Provides the sorter of subscriptions by their publications.
   * 
   * @return a Comparator<SerialPublication> with the sorter of subscriptions by
   *         their publications.
   */
  public Comparator<Subscription> getSubscriptionByPublicationComparator() {
    return SUBSCRIPTION_BY_PUBLICATION_COMPARATOR;
  }
}
