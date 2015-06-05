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
package org.lockss.subscription;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbTitle;
import org.lockss.daemon.LockssRunnable;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.plugin.PluginManager;
import org.lockss.util.Logger;
import org.lockss.util.RateLimiter;

/**
 * Processes in the background archival units that may need to be configured
 * depending on applicable subscriptions.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class SubscriptionStarter extends LockssRunnable {
  private static Logger log = Logger.getLogger(SubscriptionStarter.class);

  private static final String CANNOT_CONNECT_TO_DB_ERROR_MESSAGE =
      "Cannot connect to the database";

  private static final String CANNOT_CHECK_FIRST_RUN_ERROR_MESSAGE = "Cannot "
      + "determine whether this is the first run of the subscription manager";

  private static final String CANNOT_ROLL_BACK_DB_CONNECTION_ERROR_MESSAGE =
      "Cannot rollback the connection";

  // The subscription manager.
  private final SubscriptionManager subscriptionManager;

  // The indication of whether the Total Subscription option is in operation.
  private final boolean isTotalSubscription;

  // Limiter for the rate at which archival units are configured.
  private RateLimiter configureAuRateLimiter;

  // The database manager.
  private DbManager dbManager = null;

  // The metadata manager.
  private MetadataManager mdManager = null;

  // The plugin manager.
  private PluginManager pluginManager = null;

  // The current TdbTitle when processing multiple TdbAus.
  private TdbTitle currentTdbTitle;

  // The current list of subscribed ranges when processing multiple TdbAus.
  private List<BibliographicPeriod> currentSubscribedRanges;

  // The current list of unsubscribed ranges when processing multiple TdbAus.
  private List<BibliographicPeriod> currentUnsubscribedRanges;

  // The current set of TdbAus matched by the subscribed ranges and not matched
  // by the unsubscribed ranges when processing multiple TdbAus.
  private Set<TdbAu> currentCoveredTdbAus;

  // The iterators pointing to the TdbAus that may need to be configured
  // depending on the current subscriptions.
  private Queue<Iterator<TdbAu>> tdbAuIterators;

  // An indication that this thread is exiting and no more TdbAu iterators
  // should be added to it.
  private boolean exiting = false;

  /**
   * Constructor.
   * 
   * @param subscriptionManager
   *          A SubscriptionManager with the subscription manager.
   * @param isTotalSubscription
   *          A boolean with the indication of whether the Total Subscription
   *          option is in operation.
   * @param configureAuRateLimiter
   *          A RateLimiter for the rate at which archival units are configured.
   * @param tdbAuIterator
   *          An Iterator<TdbAu> with the TdbAus that may need to be configured
   *          depending on applicable subscriptions.
   */
  public SubscriptionStarter(SubscriptionManager subscriptionManager,
      boolean isTotalSubscription, RateLimiter configureAuRateLimiter,
      Iterator<TdbAu> tdbAuIterator) {
    super("SubscriptionStarter");

    this.subscriptionManager = subscriptionManager;
    this.isTotalSubscription = isTotalSubscription;
    this.configureAuRateLimiter = configureAuRateLimiter;

    tdbAuIterators = new LinkedList<Iterator<TdbAu>>();
    tdbAuIterators.add(tdbAuIterator);
  }

  /**
   * Entry point to start the process to handle TdbAus that may need to be
   * configured depending on applicable subscriptions.
   */
  public void lockssRun() {
    final String DEBUG_HEADER = "lockssRun(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    LockssDaemon daemon = LockssDaemon.getLockssDaemon();
    dbManager = daemon.getDbManager();
    mdManager = daemon.getMetadataManager();
    pluginManager = daemon.getPluginManager();

    // Wait until the archival units have been started.
    if (!daemon.areAusStarted()) {
      log.debug(DEBUG_HEADER + "Waiting for aus to start");

      while (!daemon.areAusStarted()) {
	try {
	  daemon.waitUntilAusStarted();
	} catch (InterruptedException ex) {
	}
      }
    }

    // Sanity check.
    if (dbManager == null || !dbManager.isReady()) {
      if (log.isDebug()) log.debug(DEBUG_HEADER + "DbManager is not ready.");
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return;
    }

    // Get a connection to the database.
    Connection conn = null;
    String message = CANNOT_CONNECT_TO_DB_ERROR_MESSAGE;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "conn = " + conn);
    } catch (DbException dbe) {
      log.error(message, dbe);
    }

    if (conn == null) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return;
    }

    boolean isFirstRun = false;

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "isTotalSubscription = " + isTotalSubscription);

    if (!isTotalSubscription) {
      message = CANNOT_CHECK_FIRST_RUN_ERROR_MESSAGE;

      try {
	// Determine whether this is the first run of the subscription manager.
	isFirstRun = mdManager.countUnconfiguredAus(conn) == 0;
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "isFirstRun = " + isFirstRun);
      } catch (DbException dbe) {
	log.error(message, dbe);
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
	return;
      }
    }

    Iterator<TdbAu> tdbAuIterator = null;
    boolean afterFirstIterator = false;
    int overallConfiguredAuCount = 0;
    long overallConfiguredAuCountStartTime = 0;

    if (log.isDebug3()) {
      overallConfiguredAuCountStartTime = new Date().getTime();
    }

    // Initialize the configuration used to configure the archival units.
    Configuration config = ConfigManager.newConfiguration();

    // Keep running until there are no more TdbAus to be processed.
    while (true) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "tdbAuIterator = " + tdbAuIterator);

      // Check whether a new TdbAu iterator needs to be processed.
      if (tdbAuIterator == null || !tdbAuIterator.hasNext()) {
	synchronized (tdbAuIterators) {
	  // Yes: Get it.
	  tdbAuIterator = tdbAuIterators.poll();

	  // Determine whether the thread needs to exit.
	  exiting = tdbAuIterator == null;
	}

	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "exiting = " + exiting);

	// Check whether there are no more TdbAus to be processed.
	if (exiting) {
	  // Yes: Configure the last partial batch of archival units.
	  try {
	    subscriptionManager.configureAuBatch(config);
	  } catch (IOException ioe) {
	    log.error("Exception caught configuring a batch of archival units. "
		+ "Config = " + config, ioe);
	  }

	  // Clean up and exit.
	  DbManager.safeRollbackAndClose(conn);

	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
	  return;
	}

	if (!isTotalSubscription) {
	  // Make sure the flag that indicates this is the first run of the
	  // subscription manager is reset after the first iterator.
	  if (afterFirstIterator) {
	    isFirstRun = false;
	  } else {
	    afterFirstIterator = true;
	  }
	}
      }

      long configuredAuCountStartTime = 0;

      if (log.isDebug3()) {
	configuredAuCountStartTime = new Date().getTime();
      }

      // Prepare a batch of archival units to be configured, if any.
      int configuredAuCount =
	  prepareBatch(tdbAuIterator, conn, isFirstRun, config);

      // Configure the batch of archival units.
      try {
	subscriptionManager.configureAuBatch(config);
      } catch (IOException ioe) {
	log.error("Exception caught configuring a batch of archival units. "
	    + "Config = " + config, ioe);
      }

      // Reset the configuration for the next batch.
      config = ConfigManager.newConfiguration();

      // Wait until the next batch can be started in order to keep the
      // appropriate configuration rate.
      try {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "Waiting until the next batch is allowed to proceed...");

	configureAuRateLimiter.waitUntilEventOk();
      } catch (InterruptedException ie) {
      }

      if (log.isDebug3()) {
	long currentTime = new Date().getTime();
	log.debug3(DEBUG_HEADER + "configuredAuCountRate = "
	    + (configuredAuCount * 60000.0D)
	    / (currentTime - configuredAuCountStartTime));

	overallConfiguredAuCount += configuredAuCount;
	log.debug3(DEBUG_HEADER + "overallConfiguredAuCount = "
	    + overallConfiguredAuCount);
	log.debug3(DEBUG_HEADER + "overallConfiguredAuCountRate = "
	    + (overallConfiguredAuCount * 60000.0D)
	    / (currentTime - overallConfiguredAuCountStartTime));
      }
    }
  }

  /**
   * Prepares a batch of archival units to be configured, if any.
   * 
   * @param tdbAuIterator
   *          An Iterator<TdbAu> with the TdbAus that may need to be configured
   *          depending on applicable subscriptions.
   * @param conn
   *          A Connection with the database connection to be used.
   * @param isFirstRun
   *          A boolean with <code>true</code> if this is the first run of the
   *          subscription manager, <code>false</code> otherwise.
   * @param config
   *          A Configuration to which to add the archival unit configuration.
   * @return an int with the count of archival units added to the batch.
   */
  private int prepareBatch(Iterator<TdbAu> tdbAuIterator, Connection conn,
      boolean isFirstRun, Configuration config) {
    final String DEBUG_HEADER = "prepareBatch(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "isFirstRun = " + isFirstRun);

    int configuredAuCount = 0;
    TdbAu tdbAu = null;
    currentTdbTitle = null;
    currentSubscribedRanges = null;
    currentUnsubscribedRanges = null;

    try {
      // Loop through all the changed archival units.
      while (tdbAuIterator.hasNext()) {
	try {
	  // Get the archival unit.
	  tdbAu = tdbAuIterator.next();
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "tdbAu = " + tdbAu);

	  // Process the archival unit.
	  boolean toBeConfigured =
	      processNewTdbAu(tdbAu, conn, isFirstRun, config);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "toBeConfigured = " + toBeConfigured);

	  DbManager.commitOrRollback(conn, log);

	  // Check whether this archival unit needs to be configured.
	  if (toBeConfigured) {
	    // Yes: Count it.
	    configuredAuCount++;

	    // Check whether the rate limit has been reached.
	    if (!configureAuRateLimiter.isEventOk()) {
	      // Yes: The batch is complete and ready to be configured.
	      break;
	    }

	    // No: Record the addition of this archival unit to the batch.
	    configureAuRateLimiter.event();
	  }
	} catch (DbException dbe) {
	  log.error("Error handling archival unit " + tdbAu, dbe);
	  conn.rollback();
	} catch (RuntimeException re) {
	  log.error("Error handling archival unit " + tdbAu, re);
	  conn.rollback();
	}
      }
    } catch (SQLException sqle) {
      log.error(CANNOT_ROLL_BACK_DB_CONNECTION_ERROR_MESSAGE, sqle);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "configuredAuCount = "
	+ configuredAuCount);
    return configuredAuCount;
  }

  /**
   * Performs the necessary processing for an archival unit that may need to be
   * configured.
   * 
   * @param tdbAu
   *          A TdbAu for the archival unit to be processed.
   * @param conn
   *          A Connection with the database connection to be used.
   * @param isFirstRun
   *          A boolean with <code>true</code> if this is the first run of the
   *          subscription manager, <code>false</code> otherwise.
   * @param config
   *          A Configuration to which to add the archival unit configuration.
   * @return a boolean with <code>true</code> if the archival unit has been
   *         added to the batch to be configured, <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private boolean processNewTdbAu(TdbAu tdbAu, Connection conn,
      boolean isFirstRun, Configuration config) throws DbException {
    final String DEBUG_HEADER = "processNewTdbAu(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tdbAu = " + tdbAu);
      log.debug2(DEBUG_HEADER + "isFirstRun = " + isFirstRun);
    }

    boolean toBeConfigured = false;

    // Get the archival unit identifier.
    String auId;

    try {
      auId = tdbAu.getAuId(pluginManager);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);
    } catch (IllegalStateException ise) {
      log.debug2("Ignored " + tdbAu
	  + " because of problems getting its identifier: " + ise.getMessage());
      return toBeConfigured;
    } catch (RuntimeException re) {
      log.error("Ignored " + tdbAu
	  + " because of problems getting its identifier: " + re.getMessage());
      return toBeConfigured;
    }

    // Check whether the archival unit is already configured.
    if (pluginManager.getAuFromId(auId) != null) {
      // Yes: Nothing more to do.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "TdbAu '" + tdbAu
	  + "' is already configured.");
      return toBeConfigured;
    }

    if (!isTotalSubscription) {
      // Check whether this is the first run of the subscription manager.
      if (isFirstRun) {
	// Yes: Add the archival unit to the table of unconfigured archival
	// units.
	mdManager.persistUnconfiguredAu(conn, auId);
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
	return toBeConfigured;
      }

      // Nothing to do if the archival unit is in the table of unconfigured
      // archival units already.
      if (mdManager.isAuInUnconfiguredAuTable(conn, auId)) {
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
	return toBeConfigured;
      }

      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "currentTdbTitle = " + currentTdbTitle);
	log.debug3(DEBUG_HEADER + "tdbAu.getTdbTitle() = "
	    + tdbAu.getTdbTitle());
      }

      // Check whether this archival unit belongs to a different title than the
      // previous archival unit processed.
      if (!tdbAu.getTdbTitle().equals(currentTdbTitle)) {
	// Yes: Update the title data for this archival unit.
	currentTdbTitle = tdbAu.getTdbTitle();

	// Get the subscription ranges for the archival unit title.
	currentSubscribedRanges = new ArrayList<BibliographicPeriod>();
	currentUnsubscribedRanges = new ArrayList<BibliographicPeriod>();

	subscriptionManager.populateTitleSubscriptionRanges(conn,
	    currentTdbTitle, currentSubscribedRanges,
	    currentUnsubscribedRanges);

	// Get the archival units covered by the subscription.
	currentCoveredTdbAus = subscriptionManager.getCoveredTdbAus(
	    currentTdbTitle, currentSubscribedRanges,
	    currentUnsubscribedRanges);
      } else {
	// No: Reuse the title data from the previous archival unit.
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "Reusing data from title = " + currentTdbTitle);
      }
    }

    // Check whether the archival unit needs to be configured.
    if (isTotalSubscription || currentCoveredTdbAus.contains(tdbAu)) {
      // Yes: Add the archival unit configuration to those to be configured.
      config = subscriptionManager.addAuConfiguration(tdbAu, auId, config);
      toBeConfigured = true;
    } else {
      // No: Add it to the table of unconfigured archival units.
      mdManager.persistUnconfiguredAu(conn, auId);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "toBeConfigured = " + toBeConfigured);
    return toBeConfigured;
  }

  /**
   * Adds an iterator of archival units that may need to be configured depending
   * on applicable subscriptions.
   * 
   * @param tdbAuIterator
   *          An Iterator<TdbAu> with the TdbAus that may need to be configured
   *          depending on applicable subscriptions.
   * @return a boolean with <code>true</code> if the iterator was added,
   * <code>false</code> otherwise.
   */
  boolean addTdbAuIterator(Iterator<TdbAu> tdbAuIterator) {
    final String DEBUG_HEADER = "addTdbAuIterator(): ";
    synchronized (tdbAuIterators) {
      if (!exiting) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "Before: tdbAuIterators.size() = " + tdbAuIterators.size());
	tdbAuIterators.add(tdbAuIterator);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "After: tdbAuIterators.size() = " + tdbAuIterators.size());
      }

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "return = " + !exiting);
      return !exiting;
    }
  }

  /**
   * Saves the archival unit configuration rate limiter.
   * 
   * @param configureAuRateLimiter
   *          A RateLimiter with the archival unit configuration rate limiter.
   */
  void setConfigureAuRateLimiter(RateLimiter configureAuRateLimiter) {
    this.configureAuRateLimiter = configureAuRateLimiter;
  }
}
