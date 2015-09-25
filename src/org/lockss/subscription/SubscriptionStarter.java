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
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbTitle;
import org.lockss.daemon.LockssRunnable;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.plugin.ArchivalUnit;
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

  private static final String CANNOT_GET_TOTAL_SUBSCRIPTION_ERROR_MESSAGE =
      "Cannot determine the Total Subscription setting from the database";

  private static final String CANNOT_CHECK_FIRST_RUN_ERROR_MESSAGE = "Cannot "
      + "determine whether this is the first run of the subscription manager";

  private static final String CANNOT_ROLL_BACK_DB_CONNECTION_ERROR_MESSAGE =
      "Cannot rollback the connection";

  // The subscription manager.
  private final SubscriptionManager subscriptionManager;

  // The setting of the Total Subscription feature.
  private Boolean isTotalSubscription = null;

  // The indication of whether the Total Subscription feature is turned on.
  private boolean isTotalSubscriptionOn = false;

  // The size of the batch used to configure archival units.
  private int configureAuBatchSize;

  // Limiter for the rate at which archival units are configured.
  private RateLimiter configureAuRateLimiter;

  // The database manager.
  private DbManager dbManager = null;

  // The metadata manager.
  private MetadataManager mdManager = null;

  // The plugin manager.
  private PluginManager pluginManager = null;

  // The current TdbPublisher when processing multiple TdbAus.
  private TdbPublisher currentTdbPublisher;

  private Boolean currentCoveredByPublisherSubscription;

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
  private Queue<Iterator<TdbAu>> tdbAuIterators = null;

  // An indication that this thread is exiting and no more TdbAu iterators
  // should be added to it.
  private boolean exiting = false;

  // An indication of whether all the TdbAus referenced by the iterators need to
  // be configured, regardless of any other considerations.
  private final boolean mustConfigure;

  /**
   * Constructor.
   * 
   * @param subscriptionManager
   *          A SubscriptionManager with the subscription manager.
   * @param configureAuBatchSize
   *          An int with the size of the batch used to configure archival
   *          units.
   * @param configureAuRateLimiter
   *          A RateLimiter for the rate at which archival units are configured.
   * @param tdbAuIterator
   *          An Iterator<TdbAu> with the TdbAus that may need to be configured
   *          depending on applicable subscriptions.
   */
  public SubscriptionStarter(SubscriptionManager subscriptionManager,
      int configureAuBatchSize, RateLimiter configureAuRateLimiter,
      Iterator<TdbAu> tdbAuIterator, boolean mustConfigure) {
    super("SubscriptionStarter");

    this.subscriptionManager = subscriptionManager;
    this.configureAuBatchSize = configureAuBatchSize;
    this.configureAuRateLimiter = configureAuRateLimiter;

    tdbAuIterators = new LinkedList<Iterator<TdbAu>>();
    tdbAuIterators.add(tdbAuIterator);

    this.mustConfigure = mustConfigure;
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

    // Get the Total Subscription feature setting.
    message = CANNOT_GET_TOTAL_SUBSCRIPTION_ERROR_MESSAGE;

    try {
      queryTotalSubscriptionSetting(conn);
    } catch (DbException dbe) {
      log.error(message, dbe);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return;
    }

    // Nothing more to do if the Total Subscription feature is set to off.
    if (isTotalSubscription != null && !isTotalSubscription.booleanValue()) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return;
    }

    boolean isFirstRun = false;

    if (!isTotalSubscriptionOn && !mustConfigure) {
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
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + (tdbAuIterator == null ? "tdbAuIterator == null"
	      : "tdbAuIterator.hasNext() = " + tdbAuIterator.hasNext()));

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

	if (!isTotalSubscriptionOn && !mustConfigure) {
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

      // Check whether no archival units were batched to be configured.
      if (configuredAuCount == 0) {
	// Yes: Continue with the next TdbAu iterator.
	continue;
      }

      // No: Configure the batch of archival units.
      try {
	subscriptionManager.configureAuBatch(config);
      } catch (IOException ioe) {
	log.error("Exception caught configuring a batch of archival units. "
	    + "Config = " + config, ioe);
      }

      // Reset the configuration for the next batch.
      config = ConfigManager.newConfiguration();

      long startDelayTime = 0;

      if (log.isDebug3()) {
	startDelayTime = new Date().getTime();
      }

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
	log.debug3(DEBUG_HEADER + "configuredAuCountDelaySeconds = "
	    + (currentTime - startDelayTime) / 1000.00);
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
   * Determines the setting of the Total Subscription feature.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void queryTotalSubscriptionSetting(Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "queryTotalSubscriptionSetting(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Check whether the Total Subscription feature is enabled.
    if (subscriptionManager.isTotalSubscriptionEnabled()) {
      // Yes: Check whether the Subscription Manager is ready.
      if (subscriptionManager.isReady()) {
	// Yes: Get the cached setting of the Total Subscription feature.
	isTotalSubscription = subscriptionManager.isTotalSubscription();
      } else {
	// No: Get the setting of the Total Subscription feature from the
	// database. 
	isTotalSubscription = subscriptionManager.findTotalSubscription(conn);
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isTotalSubscription = "
	  + isTotalSubscription);

      if (isTotalSubscription != null) {
	if (!isTotalSubscription.booleanValue()) {
	  if (log.isDebug2())
	    log.debug2(DEBUG_HEADER + "Total Unsubscription.");
	} else {
	  isTotalSubscriptionOn = true;
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Total Subscription.");
	}
      } else {
	if (log.isDebug2())
	  log.debug2(DEBUG_HEADER + "Total Subscription not set.");
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
    currentTdbPublisher = null;
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

	  // Determine whether the archival unit needs to be configured.
	  boolean toBeConfigured =
	      isTdbAuToBeConfigured(tdbAu, conn, isFirstRun, config);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "toBeConfigured = " + toBeConfigured);

	  DbManager.commitOrRollback(conn, log);

	  // Check whether this archival unit needs to be configured.
	  if (toBeConfigured) {
	    // Yes: Count it.
	    configuredAuCount++;
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "configuredAuCount = "
		+ configuredAuCount);

	    // Record the addition of this archival unit to the batch.
	    configureAuRateLimiter.event();

	    if (log.isDebug3()) log.debug3(DEBUG_HEADER
		+ "configureAuBatchSize = " + configureAuBatchSize);

	    // Check whether the batch size limit has been reached.
	    if (configuredAuCount >= configureAuBatchSize) {
	      // Yes: The batch is complete and ready to be configured.
	      if (log.isDebug3()) log.debug3(DEBUG_HEADER
		  + "configuredAuCount (" + configuredAuCount
		  + ") matches configureAuBatchSize (" + configureAuBatchSize
		  + ")");
	      break;
	    }
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
   * Provides an indication of whether an archival unit needs to be configured.
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
  private boolean isTdbAuToBeConfigured(TdbAu tdbAu, Connection conn,
      boolean isFirstRun, Configuration config) throws DbException {
    final String DEBUG_HEADER = "processNewTdbAu(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tdbAu = " + tdbAu);
      log.debug2(DEBUG_HEADER + "isFirstRun = " + isFirstRun);
    }

    // Skip those archival units that are down.
    if (tdbAu.isDown()) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Not configured: TdbAu '"
	  + tdbAu + "' is marked down.");
      return false;
    }

    // Get the archival unit identifier.
    String auId;

    try {
      auId = tdbAu.getAuId(pluginManager);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);
    } catch (IllegalStateException ise) {
      if (log.isDebug2()) log.debug2("Not configured: Ignored '" + tdbAu
	  + "' because of problems getting its identifier: "
	  + ise.getMessage());
      return false;
    } catch (RuntimeException re) {
      if (log.isDebug2()) log.debug2("Not configured: Ignored '" + tdbAu
	  + "' because of problems getting its identifier: " + re.getMessage());
      return false;
    }

    // Get the archival unit.
    ArchivalUnit au = pluginManager.getAuFromId(auId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

    // Check whether the archival unit is already configured.
    if (au != null && pluginManager.isActiveAu(au)) {
      // Yes: Nothing more to do.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Not configured: TdbAu '"
	  + tdbAu + "' is already configured.");
      return false;
    }

    // Check whether the archival unit must be configured.
    if (isTotalSubscriptionOn || mustConfigure) {
      // Yes: Add the archival unit configuration to those to be configured.
      config = subscriptionManager.addAuConfiguration(tdbAu, auId, config);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER
	  + "Configured: isTotalSubscriptionOn = " + isTotalSubscriptionOn
	  + ", mustConfigure = " + mustConfigure);
      return true;
    }

    // No: Check whether this is the first run of the subscription manager.
    if (isFirstRun) {
      // Yes: Add the archival unit to the table of unconfigured archival units.
      mdManager.persistUnconfiguredAu(conn, auId);

      // The archival unit is not going to be configured.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER
	  + "Not configured: isFirstRun " + isFirstRun);
      return false;
    }

    // No.
    if (log.isDebug3()) {
      log.debug3(DEBUG_HEADER + "currentTdbPublisher = " + currentTdbPublisher);
      log.debug3(DEBUG_HEADER + "tdbAu.getTdbPublisher() = "
	  + tdbAu.getTdbPublisher());
    }

    // Check whether this archival unit belongs to a different publisher than
    // the previous archival unit processed.
    if (!tdbAu.getTdbPublisher().equals(currentTdbPublisher)) {
      // Yes: Update the publisher for this archival unit.
      currentTdbPublisher = tdbAu.getTdbPublisher();

      // Get the publisher subscription setting for this publisher.
      currentCoveredByPublisherSubscription = subscriptionManager
	  .findPublisherSubscription(conn, currentTdbPublisher.getName());
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "currentCoveredByPublisherSubscription = "
	  + currentCoveredByPublisherSubscription);
    } else {
      // No: Reuse the publisher from the previous archival unit.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "Reusing publisher = " + currentTdbPublisher);
    }

    // Check whether there is a publisher subscription.
    if (currentCoveredByPublisherSubscription != null) {
      // Yes: Check whether all publisher archival units are subscribed.
      if (currentCoveredByPublisherSubscription.booleanValue()) {
	// Yes: Add the archival unit configuration to those to be configured.
	config = subscriptionManager.addAuConfiguration(tdbAu, auId, config);
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Configured: TdbAu '"
	    + tdbAu + "' is covered by publisher subscription = "
	    + currentCoveredByPublisherSubscription);
	return true;
      } else {
	// No: Nothing else to do.
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Not configured: TdbAu '"
	    + tdbAu + "' is covered by publisher subscription = "
	    + currentCoveredByPublisherSubscription);
	return false;
      }
    }

    // No.
    if (log.isDebug3()) {
      log.debug3(DEBUG_HEADER + "currentTdbTitle = " + currentTdbTitle);
      log.debug3(DEBUG_HEADER + "tdbAu.getTdbTitle() = " + tdbAu.getTdbTitle());
    }

    // Check whether the archival unit is in the table of unconfigured archival
    // units already.
    if (mdManager.isAuInUnconfiguredAuTable(conn, auId)) {
      // Yes: The archival unit is not going to be configured.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Not configured: TdbAu '"
	  + tdbAu
	  + "' is in the table of unconfigured archival units already.");
      return false;
    }

    // No: Check whether this archival unit belongs to a different title than
    // the previous archival unit processed.
    if (!tdbAu.getTdbTitle().equals(currentTdbTitle)) {
      // Yes: Update the title data for this archival unit.
      currentTdbTitle = tdbAu.getTdbTitle();

      // Get the subscription ranges for the archival unit title.
      currentSubscribedRanges = new ArrayList<BibliographicPeriod>();
      currentUnsubscribedRanges = new ArrayList<BibliographicPeriod>();

      subscriptionManager.populateTitleSubscriptionRanges(conn, currentTdbTitle,
	  currentSubscribedRanges, currentUnsubscribedRanges);

      // Get the archival units covered by the subscription.
      currentCoveredTdbAus = subscriptionManager.getCoveredTdbAus(
	  currentTdbTitle, currentSubscribedRanges, currentUnsubscribedRanges);
    } else {
      // No: Reuse the title data from the previous archival unit.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "Reusing data from title = " + currentTdbTitle);
    }

    // Check whether the archival unit needs to be configured.
    if (currentCoveredTdbAus.contains(tdbAu)) {
      // Yes: Add the archival unit configuration to those to be configured.
      config = subscriptionManager.addAuConfiguration(tdbAu, auId, config);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Configured: TdbAu '"
	  + tdbAu + "' is covered by subscription.");
      return true;
    }

    // No: Add it to the table of unconfigured archival units if not already
    // there.
    if (!mdManager.isAuInUnconfiguredAuTable(conn, auId)) {
      mdManager.persistUnconfiguredAu(conn, auId);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Not configured: TdbAu '"
	  + tdbAu + "' is not covered by subscription.");
    return false;
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

	if (subscriptionManager.isTotalSubscriptionEnabled()
	    && subscriptionManager.isReady()) {
	  isTotalSubscription = subscriptionManager.isTotalSubscription();
	  isTotalSubscriptionOn =
	      isTotalSubscription != null && isTotalSubscription.booleanValue();
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isTotalSubscription = "
	      + isTotalSubscriptionOn);
	}
      }

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "return = " + !exiting);
      return !exiting;
    }
  }

  /**
   * Saves the archival unit configuration batch size.
   * 
   * @param configureAuBatchSize
   *          An int with the archival unit configuration batch size.
   */
  void setConfigureAuBatchSize(int configureAuBatchSize) {
    this.configureAuBatchSize = configureAuBatchSize;
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
