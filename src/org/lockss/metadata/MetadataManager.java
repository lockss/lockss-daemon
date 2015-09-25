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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.config.Configuration;
import org.lockss.config.Configuration.Differences;
import org.lockss.daemon.LockssRunnable;
import org.lockss.daemon.status.StatusService;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.Plugin.Feature;
import org.lockss.plugin.PluginManager;
import org.lockss.scheduler.Schedule;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.PatternIntMap;

/**
 * This class implements a metadata manager that is responsible for managing an
 * index of metadata from AUs.
 * 
 * @author Philip Gust
 * @version 1.0
 */
public class MetadataManager extends BaseLockssDaemonManager implements
    ConfigurableManager {

  private static Logger log = Logger.getLogger(MetadataManager.class);

  /** prefix for config properties */
  public static final String PREFIX = Configuration.PREFIX + "metadataManager.";

  /**
   * Determines whether MedataExtractor specified by plugin should be used if it
   * is available. If <code>false</code>, a MetaDataExtractor is created that
   * returns data from the TDB rather than from the content metadata. This is
   * faster than extracting metadata form content, but less complete. Use only
   * when minimal article info is required.
   */
  public static final String PARAM_USE_METADATA_EXTRACTOR = PREFIX
      + "use_metadata_extractor";

  /**
   * Default value of MetadataManager use_metadata_extractor configuration
   * parameter; <code>true</code> to use specified MetadataExtractor.
   */
  public static final boolean DEFAULT_USE_METADATA_EXTRACTOR = true;

  /**
   * Determines whether indexing should be enabled. If indexing is not enabled,
   * AUs are queued for indexing, but the AUs are not reindexed until the
   * process is re-enabled. This parameter can be changed at runtime.
   */
  public static final String PARAM_INDEXING_ENABLED = PREFIX
      + "indexing_enabled";

  /**
   * Default value of MetadataManager indexing enabled configuration parameter;
   * <code>false</code> to disable, <code>true</code> to enable.
   */
  public static final boolean DEFAULT_INDEXING_ENABLED = false;

  /**
   * The maximum number of concurrent reindexing tasks. This property can be
   * changed at runtime
   */
  public static final String PARAM_MAX_REINDEXING_TASKS = PREFIX
      + "maxReindexingTasks";

  /** Default maximum concurrent reindexing tasks */
  public static final int DEFAULT_MAX_REINDEXING_TASKS = 1;

  /** Disable allowing crawl to interrupt reindexing tasks */
  public static final String PARAM_DISABLE_CRAWL_RESCHEDULE_TASK = PREFIX
      + "disableCrawlRescheduleTask";

  /** Default disable allowing crawl to interrupt reindexing tasks */
  public static final boolean DEFAULT_DISABLE_CRAWL_RESCHEDULE_TASK = false;

  /**
   * The maximum number reindexing task history. This property can be changed at
   * runtime
   */
  public static final String PARAM_HISTORY_MAX = PREFIX + "historySize";

  /** Indexing task watchdog name */
  static final String WDOG_PARAM_INDEXER = "MetadataIndexer";
  /** Indexing task watchdog default timeout */
  static final long WDOG_DEFAULT_INDEXER = 6 * Constants.HOUR;

  /** Default maximum reindexing tasks history */
  public static final int DEFAULT_HISTORY_MAX = 200;

  /**
   * The maximum size of pending AUs list returned by 
   * {@link #getPendingReindexingAus()}.
   */
  public static final String PARAM_PENDING_AU_LIST_SIZE = PREFIX
      + "maxPendingAuListSize";

  /** 
   * The default maximum size of pending AUs list returned by 
   * {@link #getPendingReindexingAus()}.
   */
  public static final int DEFAULT_PENDING_AU_LIST_SIZE = 200;
  
  /**
   * Determines whether indexing new AUs is prioritized ahead of 
   * reindexing exiting AUs.
   */
  public static final String PARAM_PRIORTIZE_INDEXING_NEW_AUS = PREFIX
      + "prioritizeIndexingNewAus";

  /**
   * The default for prioritizing indexing of new AUs ahead of 
   * reindexing existing AUs
   */
  public static final boolean DEFAULT_PRIORTIZE_INDEXING_NEW_AUS = true;
  
  /** Name of metadata status table */
  public static final String METADATA_STATUS_TABLE_NAME =
      "MetadataStatusTable";

  /** Map of AUID regexp to priority.  If set, AUs are assigned the
   * corresponding priority of the first regexp that their AUID matches.
   * Priority must be an integer; priorities <= -10000 mean "do not index
   * matching AUs", priorities <= -20000 mean "abort running indexes of
   * matching AUs". (Priorities are not yet implemented - only "do not
   * index" and "abort" are supported.)  */
  static final String PARAM_INDEX_PRIORITY_AUID_MAP =
    PREFIX + "indexPriorityAuidMap";
  static final List<String> DEFAULT_INDEX_PRIORITY_AUID_MAP = null;

  static final int FAILED_INDEX_PRIORITY = -1000;
  static final int MIN_INDEX_PRIORITY = -10000;
  private static final int ABORT_INDEX_PRIORITY = -20000;

  /** Maximum number of AUs to be re-indexed to batch before writing them to the
   * database. */
  public static final String PARAM_MAX_PENDING_TO_REINDEX_AU_BATCH_SIZE =
    PREFIX + "maxPendingToReindexAuBatchSize";
  public static final int DEFAULT_MAX_PENDING_TO_REINDEX_AU_BATCH_SIZE = 1000;

  /**
   * The initial value of the metadata extraction time for an AU whose metadata
   * has not been extracted yet.
   */
  public static final long NEVER_EXTRACTED_EXTRACTION_TIME = 0L;

  public static final String ACCESS_URL_FEATURE = "Access";

  static final String UNKNOWN_TITLE_NAME_ROOT = "UNKNOWN_TITLE";
  static final String UNKNOWN_SERIES_NAME_ROOT = "UNKNOWN_SERIES";

  /**
   * Map of running reindexing tasks keyed by their AuIds
   */
  final Map<String, ReindexingTask> activeReindexingTasks =
      new HashMap<String, ReindexingTask>();

  /**
   * List of reindexing tasks in order from most recent (0) to least recent.
   */
  final List<ReindexingTask> reindexingTaskHistory =
      new LinkedList<ReindexingTask>();
  
  /**
   * List of reindexing tasks that have failed or been rescheduled,
   * from most recent (0) to least recent. Only the most recent failed
   * task for a given AU is retained
   */
  final List<ReindexingTask> failedReindexingTasks = 
      new LinkedList<ReindexingTask>();

  /**
   * Metadata manager indexing enabled flag.  Initial value should always
   * be false, independent of DEFAULT_INDEXING_ENABLED, so
   * setIndexingEnabled() sees a transition.
   */
  boolean reindexingEnabled = false;

  /**
   * XXX temporary one-time startup
   */
  boolean everEnabled = false;

  /**
   * Metadata manager use metadata extractor flag. Note: set this to false only
   * where specific metadata from the metadata extractor are not needed.
   */
  boolean useMetadataExtractor = DEFAULT_USE_METADATA_EXTRACTOR;

  /** Maximum number of reindexing tasks */
  int maxReindexingTasks = DEFAULT_MAX_REINDEXING_TASKS;

  /** Disable crawl completion rescheduling a running task for same AU */
  boolean disableCrawlRescheduleTask = DEFAULT_DISABLE_CRAWL_RESCHEDULE_TASK;

  // The number of articles currently in the metadata database.
  private long metadataArticleCount = 0;

  // The number of publications currently in the metadata database
  // (-1 indicates needs recalculation)
  private long metadataPublicationCount = -1;
  
  // The number of publishers currently in the metadata database
  // (-1 indicates needs recalculation)
  private long metadataPublisherCount = -1;
  
  // The number of providers currently in the metadata database
  // (-1 indicates needs recalculation)
  private long metadataProviderCount = -1;
  
  // The number of AUs pending to be reindexed.
  private long pendingAusCount = 0;

  // the maximum size of the pending AUs list returned by 
  // {@link #getPendingReindexingAus()}
  private int pendingAuListSize = DEFAULT_PENDING_AU_LIST_SIZE;
  
  private boolean prioritizeIndexingNewAus = 
      DEFAULT_PRIORTIZE_INDEXING_NEW_AUS;
  
  // The number of successful reindexing operations.
  private long successfulReindexingCount = 0;

  // The number of failed reindexing operations.
  private long failedReindexingCount = 0;

  private int maxReindexingTaskHistory = DEFAULT_HISTORY_MAX;

  // The plugin manager.
  private PluginManager pluginMgr = null;

  // The database manager.
  private DbManager dbManager = null;

  private PatternIntMap indexPriorityAuidMap;

  private int maxPendingAuBatchSize =
      DEFAULT_MAX_PENDING_TO_REINDEX_AU_BATCH_SIZE;

  private int pendingAuBatchCurrentSize = 0;

  /** enumeration status for reindexing tasks */
  public enum ReindexingStatus {
    Running, // if the reindexing task is running
    Success, // if the reindexing task was successful
    Failed, // if the reindexing task failed
    Rescheduled // if the reindexing task was rescheduled
  };

  // The SQL code executor.
  private MetadataManagerSql mdManagerSql;

  /**
   * Starts the MetadataManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    log.debug(DEBUG_HEADER + "Starting MetadataManager");

    pluginMgr = getDaemon().getPluginManager();
    dbManager = getDaemon().getDbManager();

    try {
      mdManagerSql = new MetadataManagerSql(dbManager, this);
    } catch (DbException dbe) {
      log.error("Cannot obtain MetadataManagerSql", dbe);
      return;
    }

    // Initialize the counts of articles and pending AUs from database.
    try {
      pendingAusCount = mdManagerSql.getEnabledPendingAusCount();
      metadataArticleCount = mdManagerSql.getArticleCount();
      metadataPublicationCount = mdManagerSql.getPublicationCount();
      metadataPublisherCount = mdManagerSql.getPublisherCount();
      metadataProviderCount = mdManagerSql.getProviderCount();
    } catch (DbException dbe) {
      log.error("Cannot get pending AUs and counts", dbe);
    }

    StatusService statusServ = getDaemon().getStatusService();
    statusServ.registerStatusAccessor(METADATA_STATUS_TABLE_NAME,
        new MetadataManagerStatusAccessor(this));
    statusServ.registerOverviewAccessor(METADATA_STATUS_TABLE_NAME,
        new MetadataIndexingOverviewAccessor(this));

    resetConfig();
    log.debug(DEBUG_HEADER + "MetadataManager service successfully started");
  }

  /** Start the starter thread, which waits for AUs to be started,
   * registers AuEvent handler and performs initial scan of AUs
   */
  void startStarter() {
    MetadataStarter starter = new MetadataStarter(dbManager, this, pluginMgr);
    new Thread(starter).start();
  }

  /**
   * Handles new configuration.
   * 
   * @param config
   *          the new configuration
   * @param prevConfig
   *          the previous configuration
   * @param changedKeys
   *          the configuration keys that changed
   */
  @Override
  public void setConfig(Configuration config, Configuration prevConfig,
      Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      useMetadataExtractor =
	  config.getBoolean(PARAM_USE_METADATA_EXTRACTOR,
			    DEFAULT_USE_METADATA_EXTRACTOR);
      maxReindexingTasks =
	  Math.max(0, config.getInt(PARAM_MAX_REINDEXING_TASKS,
				    DEFAULT_MAX_REINDEXING_TASKS));
      disableCrawlRescheduleTask =
	  config.getBoolean(PARAM_DISABLE_CRAWL_RESCHEDULE_TASK,
			    DEFAULT_DISABLE_CRAWL_RESCHEDULE_TASK);
      pendingAuListSize = 
          Math.max(0, config.getInt(PARAM_PENDING_AU_LIST_SIZE, 
                                    DEFAULT_PENDING_AU_LIST_SIZE));
      prioritizeIndexingNewAus =
          config.getBoolean(PARAM_PRIORTIZE_INDEXING_NEW_AUS,
                            DEFAULT_PRIORTIZE_INDEXING_NEW_AUS);

      if (isDaemonInited()) {
	boolean doEnable =
	  config.getBoolean(PARAM_INDEXING_ENABLED, DEFAULT_INDEXING_ENABLED);
	setIndexingEnabled(doEnable);
      }

      if (changedKeys.contains(PARAM_HISTORY_MAX)) {
	int histSize = config.getInt(PARAM_HISTORY_MAX, DEFAULT_HISTORY_MAX);
	setMaxHistory(histSize);
      }

      if (changedKeys.contains(PARAM_INDEX_PRIORITY_AUID_MAP)) {
	installIndexPriorityAuidMap((List<String>) (config
	    .getList(PARAM_INDEX_PRIORITY_AUID_MAP,
		     DEFAULT_INDEX_PRIORITY_AUID_MAP)));

	if (isInited()) {
	  processAbortPriorities();
	  // process queued AUs in case any are newly eligible
	  startReindexing();
	}
      }

      if (changedKeys.contains(PARAM_MAX_PENDING_TO_REINDEX_AU_BATCH_SIZE)) {
	maxPendingAuBatchSize =
	    config.getInt(PARAM_MAX_PENDING_TO_REINDEX_AU_BATCH_SIZE,
			  DEFAULT_MAX_PENDING_TO_REINDEX_AU_BATCH_SIZE);
      }
    }
  }

  /**
   * Sets the indexing enabled state of this manager.
   * 
   * @param enable
   *          A boolean with the new enabled state of this manager.
   */
  void setIndexingEnabled(boolean enable) {
    final String DEBUG_HEADER = "setIndexingEnabled(): ";
    log.debug(DEBUG_HEADER + "enabled: " + enable);

    // Start or stop reindexing if initialized.
    if (dbManager != null) {
      if (!reindexingEnabled && enable) {
	if (everEnabled) {
        // Start reindexing
	  startReindexing();
	} else {
	  // start first-time startup thread (XXX needs to be periodic?)
	  startStarter();
	  everEnabled = true;
	}
	reindexingEnabled = enable;
      } else if (reindexingEnabled && !enable) {
        // Stop any pending reindexing operations
        stopReindexing();
      }
      reindexingEnabled = enable;
    }
  }

  /**
   * Sets the maximum reindexing task history list size.
   * 
   * @param maxSize
   *          An int with the maximum reindexing task history list size.
   */
  private void setMaxHistory(int maxSize) {
    maxReindexingTaskHistory = maxSize;

    synchronized (reindexingTaskHistory) {
      while (reindexingTaskHistory.size() > maxReindexingTaskHistory) {
        reindexingTaskHistory.remove(maxReindexingTaskHistory);
      }
    }
    synchronized(failedReindexingTasks) {
      while (failedReindexingTasks.size() > maxReindexingTaskHistory) {
        failedReindexingTasks.remove(maxReindexingTaskHistory);
      }
    }
  }

  /**
   * Sets up the index priority map.
   * 
   * @param patternPairs A List<String> with the patterns.
   */
  private void installIndexPriorityAuidMap(List<String> patternPairs) {
    if (patternPairs == null) {
      log.debug("Installing empty index priority map");
      indexPriorityAuidMap = PatternIntMap.EMPTY;
    } else {
      try {
	indexPriorityAuidMap = new PatternIntMap(patternPairs);
	log.debug("Installing index priority map: " + indexPriorityAuidMap);
      } catch (IllegalArgumentException e) {
	log.error("Illegal index priority map, ignoring", e);
	log.error("Index priority map unchanged: " + indexPriorityAuidMap);
      }
    }
  }

  /**
   * For all the entries in indexPriorityAuidMap whose priority is less than
   * ABORT_INDEX_PRIORITY, collect those that are new (not in
   * prevIndexPriorityAuidMap), then abort all indexes matching any of those
   * patterns.
   */
  private void processAbortPriorities() {
    List<ArchivalUnit> abortAus = new ArrayList<ArchivalUnit>();

    synchronized (activeReindexingTasks) {
      for (ReindexingTask task : activeReindexingTasks.values()) {
	ArchivalUnit au = task.getAu();

	if (indexPriorityAuidMap.getMatch(au.getAuId()) <
	    ABORT_INDEX_PRIORITY) {
	  abortAus.add(au);
	}
      }
    }

    for (ArchivalUnit au : abortAus) {
      log.info("Aborting indexing: " + au);
      cancelAuTask(au.getAuId());
    }
  }

  /**
   * Ensures that as many reindexing tasks as possible are running if the
   * manager is enabled.
   * 
   * @return an int with the number of reindexing tasks started.
   */
  private int startReindexing() {
    final String DEBUG_HEADER = "startReindexing(): ";

    Connection conn = null;
    int count = 0;
    try {
      conn = dbManager.getConnection();
      count = startReindexing(conn);
      DbManager.commitOrRollback(conn, log);
    } catch (DbException dbe) {
      log.error("Cannot start reindexing", dbe);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    log.debug3(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Stops any pending reindexing operations.
   */
  private void stopReindexing() {
    final String DEBUG_HEADER = "stopReindexing(): ";
    log.debug(DEBUG_HEADER + "Number of reindexing tasks being stopped: "
        + activeReindexingTasks.size());

    // Quit any running reindexing tasks.
    synchronized (activeReindexingTasks) {
      for (ReindexingTask task : activeReindexingTasks.values()) {
        task.cancel();
      }

      activeReindexingTasks.clear();
    }
  }

  /**
   * Cancels the reindexing task for the specified AU.
   * 
   * @param auId
   *          A String with the AU identifier.
   * @return a boolean with <code>true</code> if task was canceled,
   *         <code>false</code> otherwise.
   */
  private boolean cancelAuTask(String auId) {
    final String DEBUG_HEADER = "cancelAuTask(): ";
    ReindexingTask task = activeReindexingTasks.get(auId);

    if (task != null) {
      // task cancellation will remove task and schedule next one
      log.debug2(DEBUG_HEADER + "Canceling pending reindexing task for auId "
	  + auId);
      task.cancel();
      return true;
    }

    return false;
  }

  /**
   * Ensures that as many re-indexing tasks as possible are running if the
   * manager is enabled.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return an int with the number of reindexing tasks started.
   */
  int startReindexing(Connection conn) {
    final String DEBUG_HEADER = "startReindexing(): ";
    if (log.isDebug2()) log.debug2("Starting...");

    if (!getDaemon().isDaemonInited()) {
      if (log.isDebug()) log.debug(DEBUG_HEADER
	  + "Daemon not initialized: No reindexing tasks.");
      return 0;
    }

    // Don't run reindexing tasks run if reindexing is disabled.
    if (!reindexingEnabled) {
      if (log.isDebug()) log.debug(DEBUG_HEADER
	  + "Metadata manager reindexing is disabled: No reindexing tasks.");
      return 0;
    }

    int reindexedTaskCount = 0;

    synchronized (activeReindexingTasks) {
      // Try to add more concurrent reindexing tasks as long as the maximum
      // number of them is not reached.
      while (activeReindexingTasks.size() < maxReindexingTasks) {
	if (log.isDebug3()) {
	  log.debug3("activeReindexingTasks.size() = "
	      + activeReindexingTasks.size());
	  log.debug3("maxReindexingTasks = " + maxReindexingTasks);
	}
        // Get the list of pending AUs to reindex.
	List<PrioritizedAuId> auIdsToReindex = new ArrayList<PrioritizedAuId>();

	if (pluginMgr != null) {
	  auIdsToReindex = mdManagerSql.getPrioritizedAuIdsToReindex(conn,
	      maxReindexingTasks - activeReindexingTasks.size(),
	      prioritizeIndexingNewAus);
	  if (log.isDebug3()) log.debug3("auIdsToReindex.size() = "
	      + auIdsToReindex.size());
	}

	// Nothing more to do if there are no pending AUs to reindex.
        if (auIdsToReindex.isEmpty()) {
          break;
        }

        // Loop through all the pending AUs. 
        for (PrioritizedAuId auIdToReindex : auIdsToReindex) {
	  if (log.isDebug3())
	    log.debug3("auIdToReindex.auId = " + auIdToReindex.auId);

          // Get the next pending AU.
          ArchivalUnit au = pluginMgr.getAuFromId(auIdToReindex.auId);
	  if (log.isDebug3()) log.debug3("au = " + au);

          // Check whether it does not exist.
          if (au == null) {
	    // Yes: Cancel any running tasks associated with the AU and delete
	    // the AU metadata.
            try {
              deleteAu(conn, auIdToReindex.auId);
            } catch (DbException dbe) {
	      log.error("Error removing AU for auId " + auIdToReindex.auId
		  + " from the database", dbe);
            }
          } else {
            // No: Get the metadata extractor.
            ArticleMetadataExtractor ae = getMetadataExtractor(au);

            // Check whether it does not exist.
            if (ae == null) {
	      // Yes: It shouldn't happen because it was checked before adding
	      // the AU to the pending AUs list.
	      log.debug(DEBUG_HEADER + "Not running reindexing task for AU '"
		  + au.getName() + "' because it nas no metadata extractor");

	      // Remove it from the table of pending AUs.
              try {
        	pendingAusCount =
        	    mdManagerSql.removeFromPendingAus(conn, au.getAuId());
              } catch (DbException dbe) {
                log.error("Error removing AU " + au.getName()
                          + " from the table of pending AUs", dbe);
                break;
              }
            } else {
              // No: Schedule the pending AU.
              if (log.isDebug3()) log.debug3(DEBUG_HEADER
        	  + "Creating the reindexing task for AU: " + au.getName());
              ReindexingTask task = new ReindexingTask(au, ae);

              // Get the AU database identifier, if any.
              Long auSeq = null;

              try {
        	auSeq = findAuSeq(conn, au.getAuId());
        	if (log.isDebug3())
        	  log.debug3(DEBUG_HEADER + "auSeq = " + auSeq);
              } catch (DbException dbe) {
                log.error("Error trying to locate in the database AU "
                    + au.getName(), dbe);
                break;
              }

              task.setNewAu(auSeq == null);

              if (auSeq != null) {
                // Only allow incremental extraction if not doing full reindex
                boolean fullReindex = true;

        	try {
        	  fullReindex = mdManagerSql.needAuFullReindexing(conn, au);
        	  if (log.isDebug3())
        	    log.debug3(DEBUG_HEADER + "fullReindex = " + fullReindex);
                } catch (DbException dbe) {
                  log.warning("Error getting from the database the full "
                      + "re-indexing flag for AU " + au.getName()
                      + ": Doing full re-index", dbe);
                }

        	if (fullReindex) {
        	  task.setFullReindex(fullReindex);
        	} else {
        	  long lastExtractTime = 0;

        	  try {
        	    lastExtractTime =
        		mdManagerSql.getAuExtractionTime(conn, au);
        	    if (log.isDebug3()) log.debug3(DEBUG_HEADER
        		+ "lastExtractTime = " + lastExtractTime);
        	  } catch (DbException dbe) {
        	    log.warning("Error getting the last extraction time for AU "
        		+ au.getName() + ": Doing a full re-index", dbe);
        	  }

        	  task.setLastExtractTime(lastExtractTime);
        	}
              }

              activeReindexingTasks.put(au.getAuId(), task);

              // Add the reindexing task to the history; limit history list
              // size.
              addToIndexingTaskHistory(task);

	      log.debug(DEBUG_HEADER + "Running the reindexing task for AU: "
		  + au.getName());
              runReindexingTask(task);
              reindexedTaskCount++;
            }
          }
        }
      }
    }

    log.debug(DEBUG_HEADER + "Started " + reindexedTaskCount
              + " AU reindexing tasks");
    return reindexedTaskCount;
  }

  /**
   * This class returns the information about an AU to reindex.
   */
  public static class PrioritizedAuId {
    public String auId;
    long priority;
    boolean isNew;
    boolean needFullReindex;
  }
  
  /**
   * Provides a list of AuIds that require reindexing sorted by priority.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param maxAuIds
   *          An int with the maximum number of AuIds to return.
   * @return a List<String> with the list of AuIds that require reindexing
   *         sorted by priority.
   */
  public List<PrioritizedAuId> getPrioritizedAuIdsToReindex(Connection conn,
      int maxAuIds, boolean prioritizeIndexingNewAus) {
    return mdManagerSql.getPrioritizedAuIdsToReindex(conn, maxAuIds,
	prioritizeIndexingNewAus);
  }

  /**
   * Cancels any running tasks associated with an AU and deletes the AU
   * metadata.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @return an int with the number of articles deleted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int deleteAu(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "deleteAu(): ";
    log.debug3(DEBUG_HEADER + "auid = " + auId);
    cancelAuTask(auId);

    // Remove from the history list
    removeFromIndexingTaskHistory(auId);
    removeFromFailedIndexingTasks(auId);

    // Remove the metadata for this AU.
    int articleCount = mdManagerSql.removeAuMetadataItems(conn, auId);
    log.debug3(DEBUG_HEADER + "articleCount = " + articleCount);

    mdManagerSql.removeAu(conn, auId);

    // Remove pending reindexing operations for this AU.
    pendingAusCount = mdManagerSql.removeFromPendingAus(conn, auId);

    notifyDeletedAu(auId, articleCount);

    return articleCount;
  }

  /**
   * Notify listeners that an AU has been removed.
   * 
   * @param auId the AuId of the AU that was removed
   * @param articleCount the number of articles deleted
   */
  protected void notifyDeletedAu(String auId, int articleCount) {
  }

  /**
   * Provides the ArticleMetadataExtractor for the specified AU.
   * 
   * @param au
   *          An ArchivalUnit with the AU.
   * @return an ArticleMetadataExtractor with the article metadata extractor.
   */
  private ArticleMetadataExtractor getMetadataExtractor(ArchivalUnit au) {
    ArticleMetadataExtractor ae = null;

    if (useMetadataExtractor) {
      Plugin plugin = au.getPlugin();
      ae = plugin.getArticleMetadataExtractor(MetadataTarget.OpenURL(), au);
    }

    if (ae == null) {
      ae = new BaseArticleMetadataExtractor(null);
    }

    return ae;
  }

  /**
   * Adds a task to the history.
   * 
   * @param task
   *          A ReindexingTask with the task.
   */
  private void addToIndexingTaskHistory(ReindexingTask task) {
    synchronized (reindexingTaskHistory) {
      reindexingTaskHistory.add(0, task);
      setMaxHistory(maxReindexingTaskHistory);
    }
  }

  /**
   * Runs the specified reindexing task.
   * <p>
   * Temporary implementation runs as a LockssRunnable in a thread rather than
   * using the SchedService.
   * 
   * @param task A ReindexingTask with the reindexing task.
   */
  private void runReindexingTask(final ReindexingTask task) {
    /*
     * Temporarily running task in its own thread rather than using SchedService
     * 
     * @todo Update SchedService to handle this case
     */
    LockssRunnable runnable =
	new LockssRunnable(AuUtil.getThreadNameFor("Reindexing",
	                                           task.getAu())) {
	  public void lockssRun() {
	    startWDog(WDOG_PARAM_INDEXER, WDOG_DEFAULT_INDEXER);
	    task.setWDog(this);

	    task.handleEvent(Schedule.EventType.START);

	    while (!task.isFinished()) {
	      task.step(Integer.MAX_VALUE);
	    }

	    task.handleEvent(Schedule.EventType.FINISH);
	    stopWDog();
	  }
	};

    Thread runThread = new Thread(runnable);
    runThread.start();
  }

  /**
   * Removes from history indexing tasks for a specified AU.
   * 
   * @param auId
   *          A String with the AU identifier.
   * @return an int with the number of items removed.
   */
  private int removeFromIndexingTaskHistory(String auId) {
    int count = 0;

    synchronized (reindexingTaskHistory) {
      // Remove tasks with this auid from task history list.
      for (Iterator<ReindexingTask> itr = reindexingTaskHistory.iterator();
	  itr.hasNext();) {
        ReindexingTask task = itr.next();

        if (auId.equals(task.getAu().getAuId())) {
          itr.remove();
          count++;
        }
      }
    }

    return count;
  }

  /**
   * Removes from failed reindexing tasks for a specified AU.
   * 
   * @param auId
   *          A String with the AU identifier.
   * @return an int with the number of items removed.
   */
  private int removeFromFailedIndexingTasks(String auId) {
    int count = 0;

    synchronized (failedReindexingTasks) {
      // Remove tasks with this auid from task history list.
      for (Iterator<ReindexingTask> itr = failedReindexingTasks.iterator();
          itr.hasNext();) {
        ReindexingTask task = itr.next();

        if (auId.equals(task.getAu().getAuId())) {
          itr.remove();
          count++;
        }
      }
    }

    return count;
  }

  /**
   * Restarts the Metadata Managaer service by terminating any running
   * reindexing tasks and then resetting its database before calling
   * {@link #startService()}
   * .
   * <p>
   * This method is only used for testing.
   */
  public void restartService() {
    stopReindexing();

    // Start the service
    startService();
  }

  /**
   * Provides an indication of whether an Archival Unit is eligible for
   * reindexing.
   * 
   * @param au
   *          An ArchivalUnit with the Archival Unit involved.
   * @return a boolean with <code>true</code> if the Archival Unit is eligible
   *         for reindexing, <code>false</code> otherwise.
   */
  boolean isEligibleForReindexing(ArchivalUnit au) {
    return isEligibleForReindexing(au.getAuId());
  }

  /**
   * Provides an indication of whether an Archival Unit is eligible for
   * reindexing.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a boolean with <code>true</code> if the Archival Unit is eligible
   *         for reindexing, <code>false</code> otherwise.
   */
  boolean isEligibleForReindexing(String auId) {
    return indexPriorityAuidMap == null
      || indexPriorityAuidMap.getMatch(auId, 0) >= 0;
  }

  /**
   * Provides the number of active reindexing tasks.
   * 
   * @return a long with the number of active reindexing tasks.
   */
  public long getActiveReindexingCount() {
    return activeReindexingTasks.size();
  }

  /**
   * Provides the number of succesful reindexing operations.
   * 
   * @return a long with the number of successful reindexing operations.
   */
  public long getSuccessfulReindexingCount() {
    return this.successfulReindexingCount;
  }

  /**
   * Provides the number of unsuccesful reindexing operations.
   * 
   * @return a long the number of unsuccessful reindexing operations.
   */
  public long getFailedReindexingCount() {
    return this.failedReindexingCount;
  }

  /**
   * Provides the list of reindexing tasks.
   * 
   * @return a List<ReindexingTask> of reindexing tasks.
   */
  public List<ReindexingTask> getReindexingTasks() {
    return new ArrayList<ReindexingTask>(reindexingTaskHistory);
  }

  /**
   * Provides a collection of the most recent failed reindexing task
   * for each task AU.
   * 
   * @return a Collection<ReindexingTask> of failed reindexing tasks
   */
  public List<ReindexingTask> getFailedReindexingTasks() {
    return new ArrayList<ReindexingTask>(failedReindexingTasks);
  }

  /**
   * Provides a collection of auids for AUs pending reindexing.
   * The number of elements returned is controlled by a definable
   * parameter {@link #PARAM_PENDING_AU_LIST_SIZE}.
   * 
   * @return default auids for AUs pending reindexing
   */
  public List<PrioritizedAuId> getPendingReindexingAus() { 
    return getPendingReindexingAus(pendingAuListSize);
  }

  /**
   * Provides a collection of auids for AUs pending reindexing.
   * 
   * @param maxAuIds
   *          An int with the maximum number of auids to return.
   * @return a List<PrioritizedAuId> with the auids for AUs pending reindexing.
   */
  public List<PrioritizedAuId> getPendingReindexingAus(int maxAuIds) {
    final String DEBUG_HEADER = "getPendingReindexingAus(): ";
    Connection conn = null;
    List<PrioritizedAuId> auidsToReindex = new ArrayList<PrioritizedAuId>();
    if (pluginMgr != null) {
      try {
	conn = dbManager.getConnection();

	auidsToReindex = mdManagerSql.getPrioritizedAuIdsToReindex(conn,
	    maxAuIds, prioritizeIndexingNewAus);
	DbManager.commitOrRollback(conn, log);
      } catch (DbException dbe) {
	log.error("Cannot get pending AU ids for reindexing", dbe);
      } finally {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    log.debug3(DEBUG_HEADER + "count = " + auidsToReindex.size());
    return auidsToReindex;
  }
  
  /**
   * Provides the number of distinct articles in the metadata database.
   * 
   * @return a long with the number of distinct articles in the metadata
   *         database.
   */
  public long getArticleCount() {
    return metadataArticleCount;
  }
  
  /**
   * Provides the number of distict publications in the metadata database.
   * 
   * @return the number of distinct publications in the metadata database
   */
  public long getPublicationCount() {
    if (metadataPublicationCount < 0) {
      try {
        metadataPublicationCount = mdManagerSql.getPublicationCount();
      } catch (DbException ex) {
        log.error("getPublicationCount", ex);
      }
    }
    return (metadataPublicationCount < 0) ? 0 : metadataPublicationCount;
  }

  /**
   * Provides the number of distict publishers in the metadata database.
   * 
   * @return the number of distinct publishers in the metadata database
   */
  public long getPublisherCount() {
    if (metadataPublisherCount < 0) {
      try {
        metadataPublisherCount = mdManagerSql.getPublisherCount();
      } catch (DbException ex) {
        log.error("getPublisherCount", ex);
      }
    }
    return (metadataPublisherCount < 0) ? 0 : metadataPublisherCount;
  }

  /**
   * Provides the number of distict providers in the metadata database.
   * 
   * @return the number of distinct providers in the metadata database
   */
  public long getProviderCount() {
    if (metadataProviderCount < 0) {
      try {
        metadataProviderCount = mdManagerSql.getProviderCount();
      } catch (DbException ex) {
        log.error("getPublisherCount", ex);
      }
    }
    return (metadataProviderCount < 0) ? 0 : metadataProviderCount;
  }

  // The number of AUs pending to be reindexed.
  /**
   * Provides the number of AUs pending to be reindexed.
   * 
   * @return a long with the number of AUs pending to be reindexed.
   */
  public long getPendingAusCount() {
    return pendingAusCount;
  }

  /**
   * Re-calculates the number of AUs pending to be reindexed.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void updatePendingAusCount(Connection conn) throws DbException {
    final String DEBUG_HEADER = "updatePendingAusCount(): ";
    pendingAusCount = mdManagerSql.getEnabledPendingAusCount(conn);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "pendingAusCount = " + pendingAusCount);
  }

  /**
   * Provides the indexing enabled state of this manager.
   * 
   * @return a boolean with the indexing enabled state of this manager.
   */
  public boolean isIndexingEnabled() {
    return reindexingEnabled;
  }

  /**
   * Provides the identifier of a plugin if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginId
   *          A String with the plugin identifier.
   * @param platformSeq
   *          A Long with the publishing platform identifier.
   * @param isBulkContent
   *          A boolean with the indication of bulk content for the plugin.
   * @return a Long with the identifier of the plugin.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreatePlugin(Connection conn, String pluginId,
      Long platformSeq, boolean isBulkContent) throws DbException {
    return mdManagerSql.findOrCreatePlugin(conn, pluginId, platformSeq,
	isBulkContent);
  }
  
  /**
   * Provides the identifier of an Archival Unit if existing or after creating
   * it otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginSeq
   *          A Long with the identifier of the plugin.
   * @param auKey
   *          A String with the Archival Unit key.
   * @return a Long with the identifier of the Archival Unit.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreateAu(Connection conn, Long pluginSeq, String auKey)
      throws DbException {
    final String DEBUG_HEADER = "findOrCreateAu(): ";
    Long auSeq = mdManagerSql.findAu(conn, pluginSeq, auKey);
    log.debug3(DEBUG_HEADER + "auSeq = " + auSeq);

    // Check whether it is a new AU.
    if (auSeq == null) {
      // Yes: Add to the database the new AU.
      auSeq = mdManagerSql.addAu(conn, pluginSeq, auKey);
      log.debug3(DEBUG_HEADER + "new auSeq = " + auSeq);
    }

    return auSeq;
  }

  /**
   * Adds an Archival Unit metadata to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auSeq
   *          A Long with the identifier of the Archival Unit.
   * @param version
   *          An int with the metadata version.
   * @param extractTime
   *          A long with the extraction time of the metadata.
   * @param creationTime
   *          A long with the creation time of the archival unit.
   * @param providerSeq
   *          A Long with the identifier of the Archival Unit provider.
   * @return a Long with the identifier of the Archival Unit metadata just
   *         added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long addAuMd(Connection conn, Long auSeq, int version,
      long extractTime, long creationTime, Long providerSeq)
	  throws DbException {
    return mdManagerSql.addAuMd(conn, auSeq, version, extractTime, creationTime,
	providerSeq);
  }
  /**
   * Updates the timestamp of the last extraction of an Archival Unit metadata.
   * 
   * @param au
   *          The ArchivalUnit whose time to update.
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auMdSeq
   *          A Long with the identifier of the Archival Unit metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void updateAuLastExtractionTime(ArchivalUnit au, Connection conn,
				  Long auMdSeq)
      throws DbException {
    mdManagerSql.updateAuLastExtractionTime(au, conn, auMdSeq);
    pendingAusCount = mdManagerSql.getEnabledPendingAusCount(conn);
  }

  /**
   * Provides the identifier of a publication if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIssn
   *          A String with the print ISSN of the publication.
   * @param eIssn
   *          A String with the electronic ISSN of the publication.
   * @param pIsbn
   *          A String with the print ISBN of the publication.
   * @param eIsbn
   *          A String with the electronic ISBN of the publication.
   * @param pubType
   *          The type of publication: "journal", "book", or "bookSeries"
   * @param seriesName
   *          A string with the name of the book series.
   * @param proprietarySeriesId
   *          A String with the proprietary series identifier of the publication.
   * @param pubName
   *          A String with the name of the publication.
   * @param proprietaryId
   *          A String with the proprietary identifier of the publication.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreatePublication(Connection conn, Long publisherSeq, 
      String pIssn, String eIssn, String pIsbn, String eIsbn, 
      String pubType, String seriesName, String proprietarySeriesId, 
      String pubName, String proprietaryId) throws DbException {
    final String DEBUG_HEADER = "findOrCreatePublication(): ";
    Long publicationSeq = null;

    // Get the title name.
    String pubTitle = null;
    log.debug3(DEBUG_HEADER + "name = " + pubName);

    if (!StringUtil.isNullString(pubName)) {
      pubTitle = 
          pubName.substring(0, Math.min(pubName.length(), MAX_NAME_COLUMN));
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pubTitle = " + pubTitle);

    // Check whether it is a book series.
    if (   MetadataField.PUBLICATION_TYPE_BOOKSERIES.equals(pubType)
        || !StringUtil.isNullString(seriesName)) {
      // Yes: Find or create the book series.
      log.debug3(DEBUG_HEADER + "is book series.");
      String seriesTitle = 
          seriesName.substring(
              0, Math.min(seriesName.length(), MAX_NAME_COLUMN));
      
      publicationSeq = findOrCreateBookInBookSeries(conn, publisherSeq, 
          pIssn, eIssn, pIsbn, eIsbn, seriesTitle, proprietarySeriesId, 
	  pubTitle, proprietaryId);
      // No: Check whether it is a book.
    } else if (   MetadataField.PUBLICATION_TYPE_BOOK.equals(pubType)
               || isBook(pIsbn, eIsbn)) {
      // Yes: Find or create the book.
      log.debug3(DEBUG_HEADER + "is book.");
      publicationSeq = findOrCreateBook(conn, publisherSeq, null, 
          pIsbn, eIsbn, pubTitle, proprietaryId);
    } else {
      // No, it is a journal article: Find or create the journal.
      log.debug3(DEBUG_HEADER + "is journal.");
      publicationSeq = findOrCreateJournal(conn, publisherSeq, 
          pIssn, eIssn, pubTitle, proprietaryId);
    }

    return publicationSeq;
  }

  /**
   * Provides an indication of whether a metadata set corresponds to a book
   * series.
   * 
   * @param pIssn
   *          A String with the print ISSN in the metadata.
   * @param eIssn
   *          A String with the electronic ISSN in the metadata.
   * @param pIsbn
   *          A String with the print ISBN in the metadata.
   * @param eIsbn
   *          A String with the electronic ISBN in the metadata.
   * @param seriesName
   *          A String with the series name in the metadata.
   * @param volume
   *          A String with the volume in the metadata.
   * @return <code>true</code> if the metadata set corresponds to a book series,
   *         <code>false</code> otherwise.
   */
  static public boolean isBookSeries(
      String pIssn, String eIssn, String pIsbn, String eIsbn, 
      String seriesName, String volume) {
    final String DEBUG_HEADER = "isBookSeries(): ";

    boolean isBookSeries = isBook(pIsbn, eIsbn)
        && (   !StringUtil.isNullString(seriesName) 
            || !StringUtil.isNullString(volume)
            || !StringUtil.isNullString(pIssn)
            || !StringUtil.isNullString(eIssn));
    log.debug3(DEBUG_HEADER + "isBookSeries = " + isBookSeries);
    return isBookSeries;
  }

  /**
   * Provides the identifier of a book series if existing
   * or after creating it otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIssn
   *          A String with the print ISSN of the book series.
   * @param eIssn
   *          A String with the electronic ISSN of the book series.
   * @param seriesTitle
   *          A String with the name of the book series
   * @param proprietarySeriesId
   *          A String with the proprietary identifier of the book series.
   * @return a Long with the identifier of the book series.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreateBookSeries(Connection conn, Long publisherSeq, 
      String pIssn, String eIssn, String seriesTitle,
      String proprietarySeriesId) throws DbException {
    final String DEBUG_HEADER = "findOrCreateBookInBookSeries(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "seriesTitle = " + seriesTitle);
      log.debug2(DEBUG_HEADER + "proprietarySeriesId = " + proprietarySeriesId);
    }

    // Construct a title for the book in the series.
    // Find the book series.
    Long bookSeriesSeq = findPublication(conn, publisherSeq, seriesTitle, 
	pIssn, eIssn, null, null, MD_ITEM_TYPE_BOOK_SERIES);
    log.debug3(DEBUG_HEADER + "bookSeriesSeq = " + bookSeriesSeq);

    // Check whether it is a new book series.
    if (bookSeriesSeq == null) {
      // Yes: Add to the database the new book series.
      bookSeriesSeq = addPublication(conn, publisherSeq, null, 
          MD_ITEM_TYPE_BOOK_SERIES, seriesTitle);
      log.debug3(DEBUG_HEADER + "new bookSeriesSeq = " + bookSeriesSeq);

      // Skip it if the new book series could not be added.
      if (bookSeriesSeq == null) {
        log.error("Title for new book series '" + seriesTitle
            + "' could not be created.");
        return null;
      }

      // Get the book series metadata item identifier.
      Long mdItemSeq = findPublicationMetadataItem(conn, bookSeriesSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Add to the database the new book series ISSN values.
      mdManagerSql.addMdItemIssns(conn, mdItemSeq, pIssn, eIssn);
      log.debug3(DEBUG_HEADER + "added title ISSNs.");

      // Add to the database the new book series proprietary identifier.
      addMdItemProprietaryIds(conn, mdItemSeq,
	  Collections.singleton(proprietarySeriesId));

    } else {
      // No: Get the book series metadata item identifier.
      Long mdItemSeq = findPublicationMetadataItem(conn, bookSeriesSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Add to the database the book series name in the metadata as an
      // alternate, if new.
      addNewMdItemName(conn, mdItemSeq, seriesTitle);
      log.debug3(DEBUG_HEADER + "added new title name.");

      // Add to the database the ISSN values in the metadata, if new.
      addNewMdItemIssns(conn, mdItemSeq, pIssn, eIssn);
      log.debug3(DEBUG_HEADER + "added new title ISSNs.");

      // Add to the database the proprietary identifier, if new.
      addNewMdItemProprietaryIds(conn, mdItemSeq,
	  Collections.singleton(proprietarySeriesId));
    }

    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "bookSeriesSeq = " + bookSeriesSeq);
    }
    return bookSeriesSeq;
  }

  /**
   * Provides the identifier of a book that belongs to a book series if existing
   * or after creating it otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIssn
   *          A String with the print ISSN of the book series.
   * @param eIssn
   *          A String with the electronic ISSN of the book series.
   * @param pIsbn
   *          A String with the print ISBN of the book.
   * @param eIsbn
   *          A String with the electronic ISBN of the book.
   * @param seriesTitle
   *          A String with the name of the book series
   * @param proprietarySeriesId
   *          A String with the proprietary identifier of the book series.
   * @param bookTitle
   *          A String with the name of the book
   * @param proprietaryId
   *          A String with the proprietary identifier of the book.
   * @return a Long with the identifier of the book.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreateBookInBookSeries(Connection conn, Long publisherSeq,
      String pIssn, String eIssn, String pIsbn, String eIsbn,
      String seriesTitle, String proprietarySeriesId,
      String bookTitle, String proprietaryId) 
      throws DbException {
    final String DEBUG_HEADER = "findOrCreateBookInBookSeries(): ";

    // Find or create the book series
    Long bookSeriesSeq = findOrCreateBookSeries(conn, publisherSeq,  
        pIssn, eIssn, seriesTitle, proprietarySeriesId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER 
                                   + "bookSeriesSeq = " + bookSeriesSeq);
    
    // Get the book series metadata item identifier.
    Long mdItemSeq = findPublicationMetadataItem(conn, bookSeriesSeq);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    // Find or create the book in the series
    Long bookSeq = findOrCreateBook(conn, publisherSeq, mdItemSeq, 
        pIsbn, eIsbn, bookTitle, proprietaryId);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "bookSeq = " + bookSeq);
    return bookSeq;
  }

  /**
   * Provides an indication of whether a metadata set corresponds to a book.
   * 
   * @param pIsbn
   *          A String with the print ISBN in the metadata.
   * @param eIsbn
   *          A String with the electronic ISBN in the metadata.
   * @return <code>true</code> if the metadata set corresponds to a book,
   *         <code>false</code> otherwise.
   */
  static public boolean isBook(String pIsbn, String eIsbn) {
    final String DEBUG_HEADER = "isBook(): ";

    // If there are ISBN values in the metadata, it is a book or a book series.
    boolean isBook =    !StringUtil.isNullString(pIsbn) 
                     || !StringUtil.isNullString(eIsbn);
    log.debug3(DEBUG_HEADER + "isBook = " + isBook);

    return isBook;
  }

  /**
   * Provides the identifier of a book existing or after creating it otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param seriesMdItemSeq
   *          A Long with the publication parent metadata item parent identifier.
   * @param pIsbn
   *          A String with the print ISBN of the book.
   * @param eIsbn
   *          A String with the electronic ISBN of the book.
   * @param title
   *          A String with the name of the book.
   * @param proprietaryId
   *          A String with the proprietary identifier of the book.
   * @return a Long with the identifier of the book.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreateBook(Connection conn, 
      Long publisherSeq, Long seriesMdItemSeq,  
      String pIsbn, String eIsbn, 
      String title, String proprietaryId)
      throws DbException {
    final String DEBUG_HEADER = "findOrCreateBook(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "seriesMdItemSeq = " + seriesMdItemSeq);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
      log.debug2(DEBUG_HEADER + "title = " + title);
      log.debug2(DEBUG_HEADER + "proprietaryId = " + proprietaryId);
    }

    // Find the book.
   Long publicationSeq =
	findPublication(conn, publisherSeq, title, null, null, pIsbn, eIsbn,
			MD_ITEM_TYPE_BOOK);
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    // Check whether it is a new book.
    if (publicationSeq == null) {
      // Yes: Add to the database the new book.
      publicationSeq = addPublication(conn, publisherSeq, seriesMdItemSeq, 
          MD_ITEM_TYPE_BOOK, title);
      log.debug3(DEBUG_HEADER + "new publicationSeq = " + publicationSeq);

      // Skip it if the new book could not be added.
      if (publicationSeq == null) {
	log.error("Publication for new book '" + title
	    + "' could not be created.");
	return publicationSeq;
      }

      // Get the book metadata item identifier.
      Long mdItemSeq = findPublicationMetadataItem(conn, publicationSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Add to the database the new book ISBN values.
      mdManagerSql.addMdItemIsbns(conn, mdItemSeq, pIsbn, eIsbn);
      log.debug3(DEBUG_HEADER + "added title ISBNs.");

      // Add to the database the new book proprietary identifier.
      addMdItemProprietaryIds(conn, mdItemSeq,
	  Collections.singleton(proprietaryId));
    } else {
      // No: Get the book metadata item identifier.
      Long mdItemSeq = findPublicationMetadataItem(conn, publicationSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Add to the database the book name in the metadata as an alternate,
      // if new.
      addNewMdItemName(conn, mdItemSeq, title);
      log.debug3(DEBUG_HEADER + "added new title name.");

      // Add to the database the ISBN values in the metadata, if new.
      addNewMdItemIsbns(conn, mdItemSeq, pIsbn, eIsbn);
      log.debug3(DEBUG_HEADER + "added new title ISBNs.");

      // Add to the database the proprietary identifier, if not there already.
      addNewMdItemProprietaryIds(conn, mdItemSeq,
	  Collections.singleton(proprietaryId));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides the identifier of a journal if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIssn
   *          A String with the print ISSN of the journal.
   * @param eIssn
   *          A String with the electronic ISSN of the journal.
   * @param title
   *          A String with the name of the journal.
   * @param proprietaryId
   *          A String with the proprietary identifier of the journal.
   * @return a Long with the identifier of the journal.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreateJournal(Connection conn, Long publisherSeq, 
      String pIssn, String eIssn, String title, String proprietaryId)
      throws DbException {
    final String DEBUG_HEADER = "findOrCreateJournal(): ";
    Long publicationSeq = null;
    Long mdItemSeq = null;

    // Skip it if it no title name or ISSNs, as it will not be possible to
    // find the journal to which it belongs in the database.
    if (StringUtil.isNullString(title) && pIssn == null && eIssn == null) {
      log.error("Title for article cannot be created as it has no name or ISSN "
	  + "values.");
      return publicationSeq;
    }

    // Find the journal.
    publicationSeq =
	findPublication(conn, publisherSeq, title, pIssn, eIssn, null, null,
			MD_ITEM_TYPE_JOURNAL);
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    // Check whether it is a new journal.
    if (publicationSeq == null) {
      // Yes: Add to the database the new journal.
      publicationSeq = addPublication(conn, publisherSeq, null, 
          MD_ITEM_TYPE_JOURNAL, title);
      log.debug3(DEBUG_HEADER + "new publicationSeq = " + publicationSeq);

      // Skip it if the new journal could not be added.
      if (publicationSeq == null) {
	log.error("Publication for new journal '" + title
	    + "' could not be created.");
	return publicationSeq;
      }

      // Get the journal metadata item identifier.
      mdItemSeq = findPublicationMetadataItem(conn, publicationSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Add to the database the new journal ISSN values.
      mdManagerSql.addMdItemIssns(conn, mdItemSeq, pIssn, eIssn);
      log.debug3(DEBUG_HEADER + "added title ISSNs.");

      // Add to the database the new book proprietary identifier.
      addMdItemProprietaryIds(conn, mdItemSeq,
	  Collections.singleton(proprietaryId));
    } else {
      // No: Get the journal metadata item identifier.
      mdItemSeq = findPublicationMetadataItem(conn, publicationSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // No: Add to the database the journal name in the metadata as an
      // alternate, if new.
      addNewMdItemName(conn, mdItemSeq, title);
      log.debug3(DEBUG_HEADER + "added new title name.");

      // Add to the database the ISSN values in the metadata, if new.
      addNewMdItemIssns(conn, mdItemSeq, pIssn, eIssn);
      log.debug3(DEBUG_HEADER + "added new title ISSNs.");

      // Add to the database the proprietary identifier, if not there already.
      addNewMdItemProprietaryIds(conn, mdItemSeq,
	  Collections.singleton(proprietaryId));
    }

    return publicationSeq;
  }

  /**
   * Provides the identifier of a publication by its title, publisher, ISSNs
   * and/or ISBNs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param title
   *          A String with the title of the publication.
   * @param pIssn
   *          A String with the print ISSN of the publication.
   * @param eIssn
   *          A String with the electronic ISSN of the publication.
   * @param pIsbn
   *          A String with the print ISBN of the publication.
   * @param eIsbn
   *          A String with the electronic ISBN of the publication.
   * @param mdItemType
   *          A String with the type of publication to be identified.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findPublication(Connection conn, Long publisherSeq, String title,
      String pIssn, String eIssn, String pIsbn, String eIsbn, String mdItemType)
      throws DbException {
    final String DEBUG_HEADER = "findPublication(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "title = " + title);
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
    }

    Long publicationSeq = null;
    boolean hasIssns = pIssn != null || eIssn != null;
    log.debug3(DEBUG_HEADER + "hasIssns = " + hasIssns);
    boolean hasIsbns = pIsbn != null || eIsbn != null;
    log.debug3(DEBUG_HEADER + "hasIsbns = " + hasIsbns);
    boolean hasName = !StringUtil.isNullString(title);
    log.debug3(DEBUG_HEADER + "hasName = " + hasName);

    if (!hasIssns && !hasIsbns && !hasName) {
      log.debug3(DEBUG_HEADER + "Cannot find publication with no name, ISSNs"
	  + " or ISBNs.");
      return null;
    }

    if (hasIssns && hasIsbns && hasName) {
      publicationSeq = findPublicationByIssnsOrIsbnsOrName(conn, title,
	  publisherSeq, pIssn, eIssn, pIsbn, eIsbn, mdItemType);
    } else if (hasIssns && hasName) {
      publicationSeq = findPublicationByIssnsOrName(conn, publisherSeq, title,
	  pIssn, eIssn, mdItemType);
    } else if (hasIsbns && hasName) {
      publicationSeq = findPublicationByIsbnsOrName(conn, publisherSeq, title,
	  pIsbn, eIsbn, mdItemType, false);
    } else if (hasIssns) {
      publicationSeq = mdManagerSql.findPublicationByIssns(conn, publisherSeq,
	  pIssn, eIssn, mdItemType);
    } else if (hasIsbns) {
      publicationSeq = mdManagerSql.findPublicationByIsbns(conn, publisherSeq,
	  pIsbn, eIsbn, mdItemType);
    } else if (hasName) {
      publicationSeq = mdManagerSql.findPublicationByName(conn, publisherSeq,
	  title, mdItemType);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Adds a publication to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param parentMdItemSeq
   *          A Long with the publication parent metadata item parent identifier.
   * @param mdItemType
   *          A String with the type of publication.
   * @param title
   *          A String with the title of the publication.
   * @return a Long with the identifier of the publication just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long addPublication(Connection conn, Long publisherSeq,
      Long parentMdItemSeq, String mdItemType, String title)
	  throws DbException {
    return mdManagerSql.addPublication(conn, publisherSeq, parentMdItemSeq,
	mdItemType, title);
  }

  /**
   * Provides the identifier of the metadata item of a publication.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the identifier of the publication.
   * @return a Long with the identifier of the metadata item of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findPublicationMetadataItem(Connection conn, Long publicationSeq)
      throws DbException {
    return mdManagerSql.findPublicationMetadataItem(conn, publicationSeq);
  }
  
  public Long findParentMetadataItem(Connection conn, Long mditemSeq)
    throws DbException {
    return mdManagerSql.findParentMetadataItem(conn, mditemSeq);
  }

  /**
   * Adds to the database the name of a metadata item, if it does not exist
   * already.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param mdItemName
   *          A String with the name to be added, if new.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void addNewMdItemName(Connection conn, Long mdItemSeq,
      String mdItemName) throws DbException {
    final String DEBUG_HEADER = "addNewMdItemName(): ";

    if (mdItemName == null) {
      return;
    }

    Map<String, String> titleNames =
	mdManagerSql.getMdItemNames(conn, mdItemSeq);

    for (String name : titleNames.keySet()) {
      if (name.equals(mdItemName)) {
	log.debug3(DEBUG_HEADER + "Title name = " + mdItemName
	    + " already exists.");
	return;
      }
    }

    addMdItemName(conn, mdItemSeq, mdItemName, NOT_PRIMARY_NAME_TYPE);
  }

  /**
   * Adds to the database the ISSNs of a metadata item, if they do not exist
   * already.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param pIssn
   *          A String with the print ISSN of the metadata item.
   * @param eIssn
   *          A String with the electronic ISSN of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void addNewMdItemIssns(Connection conn, Long mdItemSeq, String pIssn,
      String eIssn) throws DbException {
    final String DEBUG_HEADER = "addNewMdItemIssns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
    }

    if (pIssn == null && eIssn == null) {
      return;
    }

    String issnType;
    String issnValue;

    // Find the existing ISSNs for the current metadata item.
    Set<Issn> issns = mdManagerSql.getMdItemIssns(conn, mdItemSeq);

    // Loop through all the ISSNs found.
    for (Issn issn : issns) {
      // Get the ISSN value.
      issnValue = issn.getValue();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issnValue = " + issnValue);

      // Get the ISSN type.
      issnType = issn.getType();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issnType = " + issnType);

      // Check whether this ISSN matches the passed print ISSN.
      if (pIssn != null
	  && pIssn.equals(issnValue)
	  && P_ISSN_TYPE.equals(issnType)) {
	// Yes: Skip it as it is already stored.
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "Skipped storing already existing pIssn = " + pIssn);
	pIssn = null;
	continue;
      }

      // Check whether this ISSN matches the passed electronic ISSN.
      if (eIssn != null
	  && eIssn.equals(issnValue)
	  && E_ISSN_TYPE.equals(issnType)) {
	// Yes: Skip it as it is already stored.
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "Skipped storing already existing eIssn = " + eIssn);
	eIssn = null;
      }
    }

    mdManagerSql.addMdItemIssns(conn, mdItemSeq, pIssn, eIssn);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds to the database publication proprietary identifiers, if not already
   * there.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param proprietaryIds
   *          A Collection<String> with the proprietary identifiers of the
   *          metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void addNewMdItemProprietaryIds(Connection conn, Long mdItemSeq,
      Collection<String> proprietaryIds) throws DbException {
    final String DEBUG_HEADER = "addNewMdItemProprietaryIds(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "proprietaryIds = " + proprietaryIds);
    }

    // Initialize the collection of proprietary identifiers to be added.
    ArrayList<String> newProprietaryIds = new ArrayList<String>(proprietaryIds);

    Collection<String> oldProprietaryIds =
	mdManagerSql.getMdItemProprietaryIds(conn, mdItemSeq);

    // Remove them from the collection to be added.
    newProprietaryIds.removeAll(oldProprietaryIds);

    // Add the proprietary identifiers that are new.
    addMdItemProprietaryIds(conn, mdItemSeq, newProprietaryIds);
  }

  /**
   * Adds to the database the proprietary identifiers of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param proprietaryIds
   *          A Collection<String> with the proprietary identifiers of the
   *          metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void addMdItemProprietaryIds(Connection conn, Long mdItemSeq,
      Collection<String> proprietaryIds) throws DbException {
    final String DEBUG_HEADER = "addMdItemProprietaryIds(): ";

    if (proprietaryIds == null || proprietaryIds.size() == 0) {
      return;
    }

    for (String proprietaryId : proprietaryIds) {
      dbManager.addMdItemProprietaryId(conn, mdItemSeq, proprietaryId);

      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "Added proprietary identifier = " + proprietaryId);
    }
  }

  /**
   * Adds to the database the ISBNs of a metadata item, if they do not exist
   * already.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param pIsbn
   *          A String with the print ISBN of the metadata item.
   * @param eIsbn
   *          A String with the electronic ISBN of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void addNewMdItemIsbns(Connection conn, Long mdItemSeq, String pIsbn,
      String eIsbn) throws DbException {
    final String DEBUG_HEADER = "addNewMdItemIsbns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
    }

    if (pIsbn == null && eIsbn == null) {
      return;
    }

    String isbnType;
    String isbnValue;

    // Find the existing ISBNs for the current metadata item.
    Set<Isbn> isbns = mdManagerSql.getMdItemIsbns(conn, mdItemSeq);

    // Loop through all the ISBNs found.
    for (Isbn isbn : isbns) {
      // Get the ISBN value.
      isbnValue = isbn.getValue();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbnValue = " + isbnValue);

      // Get the ISBN type.
      isbnType = isbn.getType();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbnType = " + isbnType);

      // Check whether this ISBN matches the passed print ISBN.
      if (pIsbn != null
	  && pIsbn.equals(isbnValue)
	  && P_ISBN_TYPE.equals(isbnType)) {
	// Yes: Skip it as it is already stored.
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "Skipped storing already existing pIsbn = " + pIsbn);
	pIsbn = null;
      }

      // Check whether this ISBN matches the passed electronic ISBN.
      if (eIsbn != null
	  && eIsbn.equals(isbnValue)
	  && E_ISBN_TYPE.equals(isbnType)) {
	// Yes: Skip it as it is already stored.
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "Skipped storing already existing eIsbn = " + eIsbn);
	eIsbn = null;
      }
    }

    mdManagerSql.addMdItemIsbns(conn, mdItemSeq, pIsbn, eIsbn);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifier of a publication by its title, publisher, ISSNs and
   * ISBNs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param title
   *          A String with the title of the publication.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIssn
   *          A String with the print ISSN of the publication.
   * @param eIssn
   *          A String with the electronic ISSN of the publication.
   * @param pIsbn
   *          A String with the print ISBN of the publication.
   * @param eIsbn
   *          A String with the electronic ISBN of the publication.
   * @param mdItemType
   *          A String with the type of publication to be identified.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findPublicationByIssnsOrIsbnsOrName(Connection conn,
      String title, Long publisherSeq, String pIssn, String eIssn, String pIsbn,
      String eIsbn, String mdItemType) throws DbException {
    final String DEBUG_HEADER = "findPublicationByIssnsOrIsbnsOrName(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "title = " + title);
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
    }

    Long publicationSeq = mdManagerSql.findPublicationByIssns(conn,
	publisherSeq, pIssn, eIssn, mdItemType);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    if (publicationSeq == null) {
      publicationSeq = findPublicationByIsbnsOrName(conn, publisherSeq, title,
	  pIsbn, eIsbn, mdItemType, true);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides the identifier of a publication by its publisher and title or
   * ISSNs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param title
   *          A String with the title of the publication.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIssn
   *          A String with the print ISSN of the publication.
   * @param eIssn
   *          A String with the electronic ISSN of the publication.
   * @param mdItemType
   *          A String with the type of publication to be identified.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findPublicationByIssnsOrName(Connection conn, Long publisherSeq, 
      String title, String pIssn, String eIssn, String mdItemType)
	  throws DbException {
    final String DEBUG_HEADER = "findPublicationByIssnsOrName(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "title = " + title);
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
    }

    Long publicationSeq = mdManagerSql.findPublicationByIssns(conn,
	publisherSeq, pIssn, eIssn, mdItemType);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    if (publicationSeq == null) {
      publicationSeq = mdManagerSql.findPublicationByName(conn, publisherSeq,
	  title, mdItemType);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

      // Disqualify this matched publication if it already has some ISSN.
      if (publicationSeq != null && publicationHasIssns(conn, publicationSeq)) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "Potential match of publicationSeq = "
	      + publicationSeq + " disqualified - publicationHasIssns = "
	      + publicationHasIssns(conn, publicationSeq));
	publicationSeq = null;
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides the identifier of a publication by its publisher and title or
   * ISBNs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param title
   *          A String with the title of the publication.
   * @param pIsbn
   *          A String with the print ISBN of the publication.
   * @param eIsbn
   *          A String with the electronic ISBN of the publication.
   * @param mdItemType
   *          A String with the type of publication to be identified.
   * @param newHasIssns
   *          A boolean with the indication of whether the data of the
   *          publication to be matched contains also some ISSN.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findPublicationByIsbnsOrName(Connection conn, Long publisherSeq, 
      String title, String pIsbn, String eIsbn, String mdItemType,
      boolean newHasIssns) throws DbException {
    final String DEBUG_HEADER = "findPublicationByIsbnsOrName(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "title = " + title);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
      log.debug2(DEBUG_HEADER + "newHasIssns = " + newHasIssns);
    }

    Long publicationSeq = mdManagerSql.findPublicationByIsbns(conn,
	publisherSeq, pIsbn, eIsbn, mdItemType);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    if (publicationSeq == null) {
      publicationSeq = mdManagerSql.findPublicationByName(conn, publisherSeq,
	  title, mdItemType);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

      // Disqualify this matched publication if it already has some ISBN or if
      // it has some ISSN and the incoming data also contained some ISSN.
      if (publicationSeq != null
	  && (publicationHasIsbns(conn, publicationSeq)
	      || (newHasIssns && publicationHasIssns(conn, publicationSeq)))) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "Potential match of publicationSeq = "
	      + publicationSeq + " disqualified - publicationHasIsbns = "
	      + publicationHasIsbns(conn, publicationSeq) + ", newHasIssns = "
	      + newHasIssns + ", publicationHasIssns = "
	      + publicationHasIssns(conn, publicationSeq));
	publicationSeq = null;
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides an indication of whether a publication has ISBNs in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the publication identifier.
   * @return a boolean with <code>true</code> if the publication has ISBNs,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public boolean publicationHasIsbns(Connection conn, Long publicationSeq)
      throws DbException {
    return mdManagerSql.publicationHasIsbns(conn, publicationSeq);
  }

  /**
   * Provides an indication of whether a publication has ISSNs in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the publication identifier.
   * @return a boolean with <code>true</code> if the publication has ISSNs,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public boolean publicationHasIssns(Connection conn, Long publicationSeq)
      throws DbException {
    return mdManagerSql.publicationHasIssns(conn, publicationSeq);
  }

  /**
   * Provides the identifier of a metadata item type by its name.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param typeName
   *          A String with the name of the metadata item type.
   * @return a Long with the identifier of the metadata item type.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findMetadataItemType(Connection conn, String typeName)
      throws DbException {
    return mdManagerSql.findMetadataItemType(conn, typeName);
  }

  /**
   * Adds a metadata item to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param parentSeq
   *          A Long with the metadata item parent identifier.
   * @param auMdSeq
   *          A Long with the identifier of the Archival Unit metadata.
   * @param mdItemTypeSeq
   *          A Long with the identifier of the type of metadata item.
   * @param date
   *          A String with the publication date of the metadata item.
   * @param coverage
   *          A String with the metadata item coverage.
   * @param fetchTime
   *          A long with the fetch time of metadata item.
   * @return a Long with the identifier of the metadata item just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long addMdItem(Connection conn, Long parentSeq, Long mdItemTypeSeq,
      Long auMdSeq, String date, String coverage, long fetchTime)
	  throws DbException {
    return mdManagerSql.addMdItem(conn, parentSeq, mdItemTypeSeq, auMdSeq,
	date, coverage, fetchTime);
  }

  /**
   * Adds a metadata item name to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param name
   *          A String with the name of the metadata item.
   * @param type
   *          A String with the type of name of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void addMdItemName(Connection conn, Long mdItemSeq, String name,
      String type) throws DbException {
    mdManagerSql.addMdItemName(conn, mdItemSeq, name, type);
  }

  /**
   * Adds to the database a metadata item URL.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param feature
   *          A String with the feature of the metadata item URL.
   * @param url
   *          A String with the metadata item URL.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void addMdItemUrl(Connection conn, Long mdItemSeq, String feature,
      String url) throws DbException {
    mdManagerSql.addMdItemUrl(conn, mdItemSeq, feature, url);
  }

  /**
   * Adds to the database a metadata item DOI.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param doi
   *          A String with the DOI of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void addMdItemDoi(Connection conn, Long mdItemSeq, String doi)
      throws DbException {
    mdManagerSql.addMdItemDoi(conn, mdItemSeq, doi);
  }

  /**
   * Adds an AU to the list of AUs to be reindexed.
   * Does incremental reindexing if possible, unless full reindexing
   * is required because the plugin metadata version has changed.
   * 
   * @param au
   *          An ArchivalUnit with the AU to be reindexed.
   * @param conn
   *          A Connection with the database connection to be used.
   * @param insertPendingAuBatchStatement
   *          A PreparedStatement with the prepared statement used to insert
   *          pending AUs.
   * @param inBatch
   *          A boolean indicating whether the reindexing of this AU should be
   *          performed as part of a batch.
   * @return <code>true</code> if au was added for reindexing
   */
  public boolean enableAndAddAuToReindex(ArchivalUnit au, Connection conn,
      PreparedStatement insertPendingAuBatchStatement, boolean inBatch) {
    boolean fullReindex = isAuMetadataForObsoletePlugin(au);
    return enableAndAddAuToReindex(au, conn, insertPendingAuBatchStatement,
	inBatch, fullReindex);
  }
  
  /**
   * Adds an AU to the list of AUs to be reindexed. Optionally causes
   * full reindexing by removing the AU from the database.
   * 
   * @param au
   *          An ArchivalUnit with the AU to be reindexed.
   * @param conn
   *          A Connection with the database connection to be used.
   * @param insertPendingAuBatchStatement
   *          A PreparedStatement with the prepared statement used to insert
   *          pending AUs.
   * @param inBatch
   *          A boolean indicating whether the reindexing of this AU should be
   *          performed as part of a batch.
   * @param fullReindex
   *          Causes a full reindex by ignoring the last extraction time and
   *          removing from the database the metadata of that AU. 
   * @return <code>true</code> if au was added for reindexing
   */
  public boolean enableAndAddAuToReindex(ArchivalUnit au, Connection conn,
      PreparedStatement insertPendingAuBatchStatement, boolean inBatch,
      boolean fullReindex) {
    final String DEBUG_HEADER = "enableAndAddAuToReindex(): ";

    synchronized (activeReindexingTasks) {

      try {
        // If disabled crawl completion rescheduling
        // a running task, have this function report false;
        if (disableCrawlRescheduleTask
            && activeReindexingTasks.containsKey(au.getAuId())) {
          log.debug2(DEBUG_HEADER + "Not adding AU to reindex: "
              + au.getName());
          return false;
        }

        log.debug2(DEBUG_HEADER + "Adding AU to reindex: " + au.getName());

        // Remove it from the list if it was marked as disabled.
        removeDisabledFromPendingAus(conn, au.getAuId());

        // If it's not possible to reschedule the current task, add the AU to
        // the pending list.
        if (!rescheduleAuTask(au.getAuId())) {
          addToPendingAusIfNotThere(conn, Collections.singleton(au),
              insertPendingAuBatchStatement, inBatch, fullReindex);
        }
        
        startReindexing(conn);

        DbManager.commitOrRollback(conn, log);
        return true;
      } catch (DbException dbe) {
        log.error("Cannot add au to pending AUs: " + au.getName(), dbe);
        return false;
      }
    }
  }

  /**
   * Removes an AU with disabled indexing from the table of pending AUs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archiva lUnit identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void removeDisabledFromPendingAus(Connection conn, String auId)
      throws DbException {
    mdManagerSql.removeDisabledFromPendingAus(conn, auId);
    pendingAusCount = mdManagerSql.getEnabledPendingAusCount(conn);
  }

  /**
   * Reschedules a reindexing task for a specified AU.
   * 
   * @param auId
   *          A String with the Archiva lUnit identifier.
   * @return <code>true</code> if task was rescheduled, <code>false</code>
   *         otherwise.
   */
  private boolean rescheduleAuTask(String auId) {
    final String DEBUG_HEADER = "rescheduleAuTask(): ";
    ReindexingTask task = activeReindexingTasks.get(auId);

    if (task != null) {
      log.debug2(DEBUG_HEADER
	  + "Rescheduling pending reindexing task for auId " + auId);
      // Task rescheduling will remove the task, and cause it to be rescheduled.
      task.reschedule();
      return true;
    }

    return false;
  }

  /**
   * Disables the indexing of an AU.
   * 
   * @param au
   *          An ArchivalUnit with the AU for which indexing is to be disabled.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void disableAuIndexing(ArchivalUnit au) throws DbException {
    final String DEBUG_HEADER = "disableAuIndexing(): ";

    synchronized (activeReindexingTasks) {
      Connection conn = null;

      try {
        log.debug2(DEBUG_HEADER + "Disabling indexing for AU " + au.getName());
        conn = dbManager.getConnection();

        if (conn == null) {
          log.error("Cannot disable indexing for AU '" + au.getName()
              + "' - Cannot connect to database");
          throw new DbException("Cannot connect to database");
        }

        String auId = au.getAuId();
        log.debug2(DEBUG_HEADER + "auId " + auId);

        if (activeReindexingTasks.containsKey(auId)) {
          ReindexingTask task = activeReindexingTasks.get(auId);
          task.cancel();
          activeReindexingTasks.remove(task);
        }

        // Remove the AU from the list of pending AUs if it is there.
        pendingAusCount = mdManagerSql.removeFromPendingAus(conn, auId);

        // Add it marked as disabled.
        mdManagerSql.addDisabledAuToPendingAus(conn, auId);
        DbManager.commitOrRollback(conn, log);
      } catch (DbException dbe) {
        String errorMessage = "Cannot disable indexing for AU '"
            + au.getName() +"'";
        log.error(errorMessage, dbe);
        throw dbe;
      } finally {
        DbManager.safeRollbackAndClose(conn);
      }
    }
  }

  /**
   * Adds AUs to the table of pending AUs to reindex if they are not there
   * already.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param aus
   *          A Collection<ArchivalUnit> with the AUs to add.
   * @param fullReindex
   *          A boolean indicating whether a full reindex of the Archival Unit
   *          is required.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addToPendingAusIfNotThere(Connection conn, Collection<ArchivalUnit> aus,
      boolean fullReindex) throws DbException {
    PreparedStatement insertPendingAuBatchStatement = null;

    try {
      insertPendingAuBatchStatement =
	  mdManagerSql.getInsertPendingAuBatchStatement(conn);
      addToPendingAusIfNotThere(conn, aus, insertPendingAuBatchStatement, false,
	  fullReindex);
    } finally {
      DbManager.safeCloseStatement(insertPendingAuBatchStatement);
    }
  }

  /**
   * Adds AUs to the table of pending AUs to reindex if they are not there
   * already.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param aus
   *          A Collection<ArchivalUnit> with the AUs to add.
   * @param insertPendingAuBatchStatement
   *          A PreparedStatement with the prepared statement used to insert
   *          pending AUs.
   * @param inBatch
   *          A boolean indicating whether adding these AUs to the list of
   *          pending AUs to reindex should be performed as part of a batch.
   * @param fullReindex
   *          A boolean indicating whether a full reindex of the Archival Unit
   *          is required.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addToPendingAusIfNotThere(Connection conn, Collection<ArchivalUnit> aus,
      PreparedStatement insertPendingAuBatchStatement, boolean inBatch,
      boolean fullReindex) throws DbException {
    final String DEBUG_HEADER = "addToPendingAusIfNotThere(): ";

    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "Number of pending aus to add: " + aus.size());
      log.debug2(DEBUG_HEADER + "inBatch = " + inBatch);
      log.debug2(DEBUG_HEADER + "fullReindex = " + fullReindex);
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "maxPendingAuBatchSize = "
	  + maxPendingAuBatchSize);

    try {
      // Loop through all the AUs.
      for (ArchivalUnit au : aus) {
        // Only add for extraction iff it has article metadata.
        if (!hasArticleMetadata(au)) {
          log.debug3(DEBUG_HEADER + "Not adding au " + au.getName()
              + " to pending list because it has no metadata");
        } else {
          String auid = au.getAuId();
          String pluginId = PluginManager.pluginIdFromAuId(auid);
          String auKey = PluginManager.auKeyFromAuId(auid);

          if (!mdManagerSql.isAuPending(conn, pluginId, auKey)) {
            // Only insert if entry does not exist.
	    log.debug3(DEBUG_HEADER + "Adding au " + au.getName()
		+ " to pending list");
            mdManagerSql.addAuToPendingAusBatch(pluginId, auKey, fullReindex,
        	insertPendingAuBatchStatement);
            pendingAuBatchCurrentSize++;
	    log.debug3(DEBUG_HEADER + "pendingAuBatchCurrentSize = "
		+ pendingAuBatchCurrentSize);

	    // Check whether the maximum batch size has been reached.
	    if (pendingAuBatchCurrentSize >= maxPendingAuBatchSize) {
	      // Yes: Perform the insertion of all the AUs in the batch.
	      addAuBatchToPendingAus(insertPendingAuBatchStatement);
	    }
          } else {
            if (fullReindex) {
              mdManagerSql.updateAuFullReindexing(conn, au, true);
            } else {
              log.debug3(DEBUG_HEADER+ "Not adding au " + au.getName()
        	  + " to pending list because it is already on the list");
            }
          }
	}
      }

      // Check whether there are no more AUs to be batched and the batch is not
      // empty.
      if (!inBatch && pendingAuBatchCurrentSize > 0) {
	// Yes: Perform the insertion of all the AUs in the batch.
	addAuBatchToPendingAus(insertPendingAuBatchStatement);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot add pending AUs", sqle);
    }

    pendingAusCount = mdManagerSql.getEnabledPendingAusCount(conn);
  }

  /**
   * Provides an indication of whether an AU has article metadata.
   * 
   * @param au
   *          An ArchivalUnit with the Archival Unit.
   * @return <code>true</code> if the AU has article metadata,
   *         <code>false</code> otherwise.
   */
  private boolean hasArticleMetadata(ArchivalUnit au) {
    if (au.getArticleIterator(MetadataTarget.OpenURL()) == null) {
      return false;
    }

    // It has article metadata if there is a metadata extractor.
    if (useMetadataExtractor) {
      Plugin p = au.getPlugin();

      if (p.getArticleMetadataExtractor(MetadataTarget.OpenURL(), au) != null) {
        return true;
      }
    }

    // Otherwise, it has metadata if it can be created from the TdbAu.
    return (au.getTdbAu() != null);
  }

  private void addAuBatchToPendingAus(PreparedStatement
      insertPendingAuBatchStatement) throws SQLException {
    final String DEBUG_HEADER = "addAuBatchToPendingAus(): ";
    mdManagerSql.addAuBatchToPendingAus(insertPendingAuBatchStatement);
    pendingAuBatchCurrentSize = 0;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pendingAuBatchCurrentSize = "
	+ pendingAuBatchCurrentSize);
  }

  /**
   * Notifies listeners that an AU is being reindexed.
   * 
   * @param au
   *          An ArchivalUnit with the Archival Unit.
   */
  protected void notifyStartReindexingAu(ArchivalUnit au) {
  }

  /**
   * Notifies listeners that an AU is finished being reindexed.
   * 
   * @param au
   *          An ArchivalUnit with the Archival Unit.
   * @param status
   *          A ReindexingStatus with the status of the reindexing process.
   */
  protected void notifyFinishReindexingAu(ArchivalUnit au,
      ReindexingStatus status) {
  }

  /**
   * Deletes an AU and starts the next reindexing task.
   * 
   * @param au
   *          An ArchivalUnit with the Archival Unit.
   * @return <code>true</code> if the AU was deleted, <code>false</code>
   *         otherwise.
   */
  boolean deleteAuAndReindex(ArchivalUnit au) {
    final String DEBUG_HEADER = "deleteAuAndReindex(): ";

    synchronized (activeReindexingTasks) {
      Connection conn = null;

      try {
        log.debug2(DEBUG_HEADER + "Removing au to reindex: " + au.getName());
        // add pending AU
        conn = dbManager.getConnection();

        if (conn == null) {
          log.error("Cannot connect to database"
              + " -- cannot add aus to pending aus");
          return false;
        }

        deleteAu(conn, au.getAuId());

        // Force reindexing to start next task.
        startReindexing(conn);
        DbManager.commitOrRollback(conn, log);

        return true;
      } catch (DbException dbe) {
        log.error("Cannot remove au: " + au.getName(), dbe);
        return false;
      } finally {
        DbManager.safeRollbackAndClose(conn);
      }
    }
  }

  /**
   * Provides the metadata version of a plugin.
   * 
   * @param plugin
   *          A Plugin with the plugin.
   * @return an int with the plugin metadata version.
   */
  int getPluginMetadataVersionNumber(Plugin plugin) {
    final String DEBUG_HEADER = "getPluginMetadataVersionNumber(): ";
  
    int version = 1;
    String pluginVersion = plugin.getFeatureVersion(Feature.Metadata);
    if (log.isDebug3()) {
      log.debug3(DEBUG_HEADER + "Metadata Feature version: " + pluginVersion
		 + " for " + plugin.getPluginName());
    }
    if (StringUtil.isNullString(pluginVersion)) {
      log.debug2("Plugin version not found: Using " + version);
      return version;
    }

    String prefix = Feature.Metadata + "_";

    if (!pluginVersion.startsWith(prefix)) {
      log.error("Plugin version '" + pluginVersion + "' does not start with '"
	  + prefix + "': Using " + version);
      return version;
    }

    try {
      version = Integer.valueOf(pluginVersion.substring(prefix.length()));
    } catch (NumberFormatException nfe) {
      log.error("Plugin version '" + pluginVersion + "' does not end with a "
	  + "number after '" + prefix + "': Using " + version);
    }
    
    log.debug3(DEBUG_HEADER + "version = " + version);
    return version;
  }

  /**
   * Receives notification that a reindexing task was succeessful. 
   * 
   * @param task the reindexing task
   */
  void addToSuccessfulReindexingTasks(ReindexingTask task) {
    this.successfulReindexingCount++;
  }

  synchronized void addToMetadataArticleCount(long count) {
    this.metadataArticleCount += count;
    this.metadataPublicationCount = -1;  // needs recalculation
    this.metadataPublisherCount = -1;    // needs recalculation
    this.metadataProviderCount = -1;     // needs recalculation
  }

  /**
   * Receives notification that a reindexing task has failed 
   * or has been rescheduled.
   * 
   * @param task the reindexing task
   */
  void addToFailedReindexingTasks(ReindexingTask task) {
    failedReindexingCount++;
    
    String taskAuId = task.getAuId();
    synchronized (failedReindexingTasks) {
      removeFromFailedIndexingTasks(taskAuId);
      failedReindexingTasks.add(0, task);
      setMaxHistory(maxReindexingTaskHistory);
    }
  }

  /**
   * Provides an indication of whether the version of the metadata of an AU
   * stored in the database has been obtained with an obsolete version of the
   * plugin.
   * 
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @return <code>true</code> if the metadata was obtained with a version of
   *         the plugin previous to the current version, <code>false</code>
   *         otherwise.
   */
  boolean isAuMetadataForObsoletePlugin(ArchivalUnit au) {
    final String DEBUG_HEADER = "isAuMetadataForObsoletePlugin(): ";

    // Get the plugin version of the stored AU metadata. 
    int auVersion = mdManagerSql.getAuMetadataVersion(au);
    log.debug(DEBUG_HEADER + "auVersion = " + auVersion);

    // Get the current version of the plugin. 
    int pVersion = getPluginMetadataVersionNumber(au.getPlugin());
    log.debug(DEBUG_HEADER + "pVersion = " + pVersion);

    return pVersion > auVersion;
  }

  /**
   * Provides an indication of whether the version of the metadata of an AU
   * stored in the database has been obtained with an obsolete version of the
   * plugin.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @return <code>true</code> if the metadata was obtained with a version of
   *         the plugin previous to the current version, <code>false</code>
   *         otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  boolean isAuMetadataForObsoletePlugin(Connection conn, ArchivalUnit au)
      throws DbException {
    final String DEBUG_HEADER = "isAuMetadataForObsoletePlugin(): ";

    // Get the plugin version of the stored AU metadata. 
    int auVersion = mdManagerSql.getAuMetadataVersion(conn, au);
    log.debug2(DEBUG_HEADER + "auVersion = " + auVersion);

    // Get the current version of the plugin. 
    int pVersion = getPluginMetadataVersionNumber(au.getPlugin());
    log.debug2(DEBUG_HEADER + "pVersion = " + pVersion);

    return pVersion > auVersion;
  }

  /**
   * Provides an indication of whether the metadata of an AU has not been saved
   * in the database after the last successful crawl of the AU.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @return <code>true</code> if the metadata of the AU has not been saved in
   *         the database after the last successful crawl of the AU,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  boolean isAuCrawledAndNotExtracted(Connection conn, ArchivalUnit au)
      throws DbException {
    final String DEBUG_HEADER = "isAuCrawledAndNotExtracted(): ";

    // Get the time of the last successful crawl of the AU. 
    long lastCrawlTime = AuUtil.getAuState(au).getLastCrawlTime();
    log.debug2(DEBUG_HEADER + "lastCrawlTime = " + lastCrawlTime);

    long lastExtractionTime = mdManagerSql.getAuExtractionTime(conn, au);
    log.debug2(DEBUG_HEADER + "lastExtractionTime = " + lastExtractionTime);

    return lastCrawlTime > lastExtractionTime;
  }

  /**
   * Utility method to provide the database manager.
   * 
   * @return a DbManager with the database manager.
   */
  DbManager getDbManager() {
    return dbManager;
  }

  /**
   * Provides the identifier of a publishing platform if existing or after
   * creating it otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param platformName
   *          A String with the platform name.
   * @return a Long with the identifier of the publishing platform.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreatePlatform(Connection conn, String platformName)
      throws DbException {
    final String DEBUG_HEADER = "findOrCreatePlatform(): ";
    log.debug3(DEBUG_HEADER + "platformName = " + platformName);
    
    if (platformName == null) {
      platformName = NO_PLATFORM;
    }

    Long platformSeq = findPlatform(conn, platformName);
    log.debug3(DEBUG_HEADER + "platformSeq = " + platformSeq);

    // Check whether it is a new platform.
    if (platformSeq == null) {
      // Yes: Add to the database the new platform.
      platformSeq = mdManagerSql.addPlatform(conn, platformName);
      log.debug3(DEBUG_HEADER + "new platformSeq = " + platformSeq);
    }

    return platformSeq;
  }

  /**
   * Provides the identifier of a platform.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param platformName
   *          A String with the platform identifier.
   * @return a Long with the identifier of the platform.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findPlatform(Connection conn, String platformName)
      throws DbException {
    return mdManagerSql.findPlatform(conn, platformName);
  }

  /**
   * Provides the prepared statement used to insert pending AUs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a PreparedStatement with the prepared statement used to insert
   *         pending AUs.
   */
  public PreparedStatement getInsertPendingAuBatchStatement(Connection conn)
      throws DbException {
    return mdManagerSql.getInsertPendingAuBatchStatement(conn);
  }

  /**
   * Provides the prepared statement used to insert pending AUs with the
   * highest priority.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a PreparedStatement with the prepared statement used to insert
   *         pending AUs with the highest priority.
   */
  public PreparedStatement getPrioritizedInsertPendingAuBatchStatement(
      Connection conn) throws DbException {
    return mdManagerSql.getPrioritizedInsertPendingAuBatchStatement(conn);
  }

  /**
   * Provides the publication identifier of an existing book in a book series,
   * null otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIssn
   *          A String with the print ISSN of the book series.
   * @param eIssn
   *          A String with the electronic ISSN of the book series.
   * @param pIsbn
   *          A String with the print ISBN of the book series.
   * @param eIsbn
   *          A String with the electronic ISBN of the book series.
   * @param seriesTitle
   *          A String with the name of the book series.
   * @param bookTitle
   *          A String with the name of the book.
   * @return a Long with the identifier of the book.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findBookInBookSeries(Connection conn, Long publisherSeq, 
      String pIssn, String eIssn,
      String pIsbn, String eIsbn, String seriesTitle,
      String bookTitle) throws DbException {
    final String DEBUG_HEADER = "findBookInBookSeries(): ";
    Long bookSeq = null;

    // Find the book series.
    Long bookSeriesSeq =
	findBookSeries(conn, publisherSeq, pIssn, eIssn, seriesTitle);
    log.debug3(DEBUG_HEADER + "bookSeriesSeq = " + bookSeriesSeq);

    // Check whether it is an existing book series.
    if (bookSeriesSeq != null) {
      // Yes: Find the book.
      bookSeq = findBook(conn, publisherSeq, pIsbn, eIsbn, bookTitle);
    }

    return bookSeq;
  }

  /**
   * Provides the publication identifier of an existing book, null otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIsbn
   *          A String with the print ISBN of the book.
   * @param eIsbn
   *          A String with the electronic ISBN of the book.
   * @param title
   *          A String with the name of the book.
   * @return a Long with the identifier of the book.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findBook(Connection conn, Long publisherSeq, 
      String pIsbn, String eIsbn, String title) throws DbException {
    final String DEBUG_HEADER = "findBook(): ";

    // Find the book.
    Long publicationSeq =
	findPublication(conn, publisherSeq, 
	    title, null, null, pIsbn, eIsbn, MD_ITEM_TYPE_BOOK);
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    return publicationSeq;
  }

  /**
   * Provides the publication identifier of an existing book series, 
   * null otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIssn
   *          A String with the print ISSN of the series.
   * @param eIssn
   *          A String with the electronic ISSN of the series.
   * @param seriesName
   *          A String with the name of the series.
   * @return a Long with the identifier of the series publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findBookSeries(Connection conn, Long publisherSeq,
      String pIssn, String eIssn, String seriesName) throws DbException {
    Long seriesSeq =
        findPublication(conn, publisherSeq, seriesName, 
                        pIssn, eIssn, null, null, MD_ITEM_TYPE_BOOK_SERIES);
    return seriesSeq;
  }
  
  /**
   * Provides the identifier of an existing journal, null otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pIssn
   *          A String with the print ISSN of the journal.
   * @param eIssn
   *          A String with the electronic ISSN of the journal.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param title
   *          A String with the name of the journal.
   * @return a Long with the identifier of the journal.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findJournal(Connection conn, Long publisherSeq, 
      String pIssn, String eIssn, String title) throws DbException {
    final String DEBUG_HEADER = "findJournal(): ";
    Long publicationSeq = null;

    // Skip it if it no title name or ISSNs, as it will not be possible to
    // find the journal to which it belongs in the database.
    if (StringUtil.isNullString(title) && pIssn == null && eIssn == null) {
      log.error("Title for article cannot be created as it has no name or ISSN "
	  + "values.");
      return publicationSeq;
    }

    // Find the journal.
    publicationSeq =
	findPublication(conn, publisherSeq, 
	                title, pIssn, eIssn, null, null, MD_ITEM_TYPE_JOURNAL);
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    return publicationSeq;
  }

  /**
   * Provides the identifiers of pending Archival Units that have been disabled.
   * 
   * @return a Collection<String> with the identifiers of disabled pending
   *         Archival Units.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Collection<String> findDisabledPendingAus() throws DbException {
    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    return findDisabledPendingAus(conn);
  }

  /**
   * Provides the identifiers of pending Archival Units that have been disabled.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a Collection<String> with the identifiers of disabled pending
   *         Archival Units.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Collection<String> findDisabledPendingAus(Connection conn)
      throws DbException {
    return mdManagerSql.findPendingAusWithPriority(conn, MIN_INDEX_PRIORITY);
  }

  /**
   * Provides the identifiers of pending Archival Units that failed during
   * metadata indexing.
   * 
   * @return a Collection<String> with the identifiers of pending Archival Units
   *         with failed metadata indexing processes.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Collection<String> findFailedIndexingPendingAus() throws DbException {
    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    return findFailedIndexingPendingAus(conn);
  }

  /**
   * Provides the identifiers of pending Archival Units that failed during
   * metadata indexing.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a Collection<String> with the identifiers of pending Archival Units
   *         with failed metadata indexing processes.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Collection<String> findFailedIndexingPendingAus(Connection conn)
      throws DbException {
    return mdManagerSql.findPendingAusWithPriority(conn, FAILED_INDEX_PRIORITY);
  }

  /**
   * Provides the identifier of the publisher of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auSeq
   *          A Long with the identifier of the Archival Unit.
   * @return a Long with the identifier of the publisher.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findAuPublisher(Connection conn, Long auSeq) throws DbException {
    return mdManagerSql.findAuPublisher(conn, auSeq);
  }

  /**
   * Adds to the database the URLs of a metadata item, if they are new.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param featuredUrlMap
   *          A Map<String, String> with the URL/feature pairs to be added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void addNewMdItemUrls(Connection conn, Long mdItemSeq,
      Map<String, String> featuredUrlMap) throws DbException {
    final String DEBUG_HEADER = "addNewMdItemUrls(): ";

    // Initialize the collection of URLs to be added.
    Map<String, String> newUrls = new HashMap<String, String>(featuredUrlMap);

    Map<String, String> oldUrls = dbManager.getMdItemUrls(conn, mdItemSeq);
    String url;

    // Loop through all the URLs already linked to the metadata item.
    for (String feature : oldUrls.keySet()) {
      url = oldUrls.get(feature);
      log.debug3(DEBUG_HEADER + "Found feature = " + feature + ", URL = "
	  + url);

      // Remove it from the collection to be added if it exists already.
      if (newUrls.containsKey(feature) && newUrls.get(feature).equals(url)) {
	log.debug3(DEBUG_HEADER + "Feature = " + feature + ", URL = " + url
	    + " already exists: Not adding it.");

	newUrls.remove(feature);
      }
    }

    // Add the URLs that are new.
    addMdItemUrls(conn, mdItemSeq, null, newUrls);
  }

  /**
   * Adds to the database the authors of a metadata item, if they are new.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param authors
   *          A Collection<String> with the authors of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void addNewMdItemAuthors(Connection conn, Long mdItemSeq,
      Collection<String> authors) throws DbException {
    if (authors == null || authors.size() == 0) {
      return;
    }

    // Initialize the collection of authors to be added.
    List<String> newAuthors = new ArrayList<String>(authors);

    // Get the existing authors.
    Collection<String> oldAuthors =
	mdManagerSql.getMdItemAuthors(conn, mdItemSeq);

    // Remove them from the collection to be added.
    newAuthors.removeAll(oldAuthors);

    // Add the authors that are new.
    mdManagerSql.addMdItemAuthors(conn, mdItemSeq, newAuthors);
  }

  /**
   * Adds to the database the keywords of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param keywords
   *          A Collection<String> with the keywords of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void addNewMdItemKeywords(Connection conn, Long mdItemSeq,
      Collection<String> keywords) throws DbException {
    if (keywords == null || keywords.size() == 0) {
      return;
    }

    // Initialize the collection of keywords to be added.
    ArrayList<String> newKeywords = new ArrayList<String>(keywords);

    Collection<String> oldKeywords =
	mdManagerSql.getMdItemKeywords(conn, mdItemSeq);

    // Remove them from the collection to be added.
    newKeywords.removeAll(oldKeywords);

    // Add the keywords that are new.
    mdManagerSql.addMdItemKeywords(conn, mdItemSeq, newKeywords);
  }

  /**
   * Adds to the database the URLs of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param accessUrl
   *          A String with the access URL to be added.
   * @param featuredUrlMap
   *          A Map<String, String> with the URL/feature pairs to be added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addMdItemUrls(Connection conn, Long mdItemSeq, String accessUrl,
      Map<String, String> featuredUrlMap) throws DbException {
    final String DEBUG_HEADER = "addMdItemUrls(): ";

    if (!StringUtil.isNullString(accessUrl)) {
      // Add the access URL.
      addMdItemUrl(conn, mdItemSeq, ACCESS_URL_FEATURE, accessUrl);
      log.debug3(DEBUG_HEADER + "Added feature = " + ACCESS_URL_FEATURE
	  + ", URL = " + accessUrl);
    }

    // Loop through all the featured URLs.
    for (String feature : featuredUrlMap.keySet()) {
      // Add the featured URL.
      addMdItemUrl(conn, mdItemSeq, feature,
			     featuredUrlMap.get(feature));
      log.debug3(DEBUG_HEADER + "Added feature = " + feature + ", URL = "
	  + featuredUrlMap.get(feature));
    }
  }

  /**
   * Merges the properties of a child metadata item into another child metadata
   * item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sourceMdItemSeq
   *          A Long with the identifier of the source metadata item.
   * @param targetMdItemSeq
   *          A Long with the identifier of the target metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void mergeChildMdItemProperties(Connection conn, Long sourceMdItemSeq,
      Long targetMdItemSeq) throws DbException {
    final String DEBUG_HEADER = "mergeChildMdItemProperties(): ";
    log.debug3(DEBUG_HEADER + "sourceMdItemSeq = " + sourceMdItemSeq);
    log.debug3(DEBUG_HEADER + "targetMdItemSeq = " + targetMdItemSeq);

    // Do not merge a metadata item into itself.
    if (sourceMdItemSeq != targetMdItemSeq) {
      // Merge the names.
      mergeMdItemNames(conn, sourceMdItemSeq, targetMdItemSeq);

      // Merge the authors.
      mergeMdItemAuthors(conn, sourceMdItemSeq, targetMdItemSeq);

      // Merge the keywords.
      mergeMdItemKeywords(conn, sourceMdItemSeq, targetMdItemSeq);

      // Merge the URLs.
      mergeMdItemUrls(conn, sourceMdItemSeq, targetMdItemSeq);
    }

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Merges the names of a metadata item into another metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sourceMdItemSeq
   *          A Long with the identifier of the source metadata item.
   * @param targetMdItemSeq
   *          A Long with the identifier of the target metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void mergeMdItemNames(Connection conn, Long sourceMdItemSeq,
      Long targetMdItemSeq) throws DbException {
    final String DEBUG_HEADER = "mergeMdItemNames(): ";
    log.debug3(DEBUG_HEADER + "sourceMdItemSeq = " + sourceMdItemSeq);
    log.debug3(DEBUG_HEADER + "targetMdItemSeq = " + targetMdItemSeq);

    Map<String, String> sourceMdItemNames =
	mdManagerSql.getMdItemNames(conn, sourceMdItemSeq);

    for (String mdItemName : sourceMdItemNames.keySet()) {
      addNewMdItemName(conn, targetMdItemSeq, mdItemName);
    }

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Merges the authors of a metadata item into another metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sourceMdItemSeq
   *          A Long with the identifier of the source metadata item.
   * @param targetMdItemSeq
   *          A Long with the identifier of the target metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void mergeMdItemAuthors(Connection conn, Long sourceMdItemSeq,
      Long targetMdItemSeq) throws DbException {
    final String DEBUG_HEADER = "mergeMdItemAuthors(): ";
    log.debug3(DEBUG_HEADER + "sourceMdItemSeq = " + sourceMdItemSeq);
    log.debug3(DEBUG_HEADER + "targetMdItemSeq = " + targetMdItemSeq);

    Collection<String> sourceMdItemAuthors =
	mdManagerSql.getMdItemAuthors(conn, sourceMdItemSeq);

    addNewMdItemAuthors(conn, targetMdItemSeq, sourceMdItemAuthors);

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Merges the keywords of a metadata item into another metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sourceMdItemSeq
   *          A Long with the identifier of the source metadata item.
   * @param targetMdItemSeq
   *          A Long with the identifier of the target metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void mergeMdItemKeywords(Connection conn, Long sourceMdItemSeq,
      Long targetMdItemSeq) throws DbException {
    final String DEBUG_HEADER = "mergeMdItemKeywords(): ";
    log.debug3(DEBUG_HEADER + "sourceMdItemSeq = " + sourceMdItemSeq);
    log.debug3(DEBUG_HEADER + "targetMdItemSeq = " + targetMdItemSeq);

    Collection<String> sourceMdItemKeywords = 
	mdManagerSql.getMdItemKeywords(conn, sourceMdItemSeq);

    addNewMdItemKeywords(conn, targetMdItemSeq, sourceMdItemKeywords);

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Merges the URLs of a metadata item into another metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sourceMdItemSeq
   *          A Long with the identifier of the source metadata item.
   * @param targetMdItemSeq
   *          A Long with the identifier of the target metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void mergeMdItemUrls(Connection conn, Long sourceMdItemSeq,
      Long targetMdItemSeq) throws DbException {
    final String DEBUG_HEADER = "mergeMdItemUrls(): ";
    log.debug3(DEBUG_HEADER + "sourceMdItemSeq = " + sourceMdItemSeq);
    log.debug3(DEBUG_HEADER + "targetMdItemSeq = " + targetMdItemSeq);

    Map<String, String> sourceMdItemUrls =
	dbManager.getMdItemUrls(conn, sourceMdItemSeq);

    addNewMdItemUrls(conn, targetMdItemSeq, sourceMdItemUrls);

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Merges the properties of a parent metadata item into another parent
   * metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sourceMdItemSeq
   *          A Long with the identifier of the source metadata item.
   * @param targetMdItemSeq
   *          A Long with the identifier of the target metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void mergeParentMdItemProperties(Connection conn, Long sourceMdItemSeq,
      Long targetMdItemSeq) throws DbException {
    final String DEBUG_HEADER = "mergeParentMdItemProperties(): ";
    log.debug3(DEBUG_HEADER + "sourceMdItemSeq = " + sourceMdItemSeq);
    log.debug3(DEBUG_HEADER + "targetMdItemSeq = " + targetMdItemSeq);

    // Do not merge a metadata item into itself.
    if (sourceMdItemSeq != targetMdItemSeq) {
      // Merge the names.
      mergeMdItemNames(conn, sourceMdItemSeq, targetMdItemSeq);

      // Merge the ISBNs.
      mergeMdItemIsbns(conn, sourceMdItemSeq, targetMdItemSeq);

      // Merge the ISSNs.
      mergeMdItemIssns(conn, sourceMdItemSeq, targetMdItemSeq);

      // Merge the proprietary identifiers.
      mergeMdItemProprietaryIds(conn, sourceMdItemSeq, targetMdItemSeq);
    }

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Merges the ISBNs of a metadata item into another metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sourceMdItemSeq
   *          A Long with the identifier of the source metadata item.
   * @param targetMdItemSeq
   *          A Long with the identifier of the target metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void mergeMdItemIsbns(Connection conn, Long sourceMdItemSeq,
      Long targetMdItemSeq) throws DbException {
    final String DEBUG_HEADER = "mergeMdItemIsbns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "sourceMdItemSeq = " + sourceMdItemSeq);
      log.debug2(DEBUG_HEADER + "targetMdItemSeq = " + targetMdItemSeq);
    }

    // Find the existing ISBNs for the source metadata item.
    Set<Isbn> sourceMdItemIsbns =
	mdManagerSql.getMdItemIsbns(conn, sourceMdItemSeq);

    String isbnType;
    String isbnValue;

    // Loop through all the ISBNs found.
    for (Isbn isbn : sourceMdItemIsbns) {
      // Get the ISBN value.
      isbnValue = isbn.getValue();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbnValue = " + isbnValue);

      // Get the ISBN type.
      isbnType = isbn.getType();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbnType = " + isbnType);

      if (P_ISBN_TYPE.equals(isbnType)) {
	addNewMdItemIsbns(conn, targetMdItemSeq, isbnValue, null);
      } else if (E_ISBN_TYPE.equals(isbnType)) {
	addNewMdItemIsbns(conn, targetMdItemSeq, null, isbnValue);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Merges the ISSNs of a metadata item into another metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sourceMdItemSeq
   *          A Long with the identifier of the source metadata item.
   * @param targetMdItemSeq
   *          A Long with the identifier of the target metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void mergeMdItemIssns(Connection conn, Long sourceMdItemSeq,
      Long targetMdItemSeq) throws DbException {
    final String DEBUG_HEADER = "mergeMdItemIssns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "sourceMdItemSeq = " + sourceMdItemSeq);
      log.debug2(DEBUG_HEADER + "targetMdItemSeq = " + targetMdItemSeq);
    }

    // Find the existing ISSNs for the source metadata item.
    Set<Issn> sourceMdItemIssns =
	mdManagerSql.getMdItemIssns(conn, sourceMdItemSeq);

    String issnType;
    String issnValue;

    // Loop through all the ISSNs found.
    for (Issn issn : sourceMdItemIssns) {
      // Get the ISSN value.
      issnValue = issn.getValue();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issnValue = " + issnValue);

      // Get the ISSN type.
      issnType = issn.getType();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issnType = " + issnType);

      if (P_ISSN_TYPE.equals(issnType)) {
	addNewMdItemIssns(conn, targetMdItemSeq, issnValue, null);
      } else if (E_ISSN_TYPE.equals(issnType)) {
	addNewMdItemIssns(conn, targetMdItemSeq, null, issnValue);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Merges the propritary identifiers of a metadata item into another metadata
   * item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sourceMdItemSeq
   *          A Long with the identifier of the source metadata item.
   * @param targetMdItemSeq
   *          A Long with the identifier of the target metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void mergeMdItemProprietaryIds(Connection conn, Long sourceMdItemSeq,
      Long targetMdItemSeq) throws DbException {
    final String DEBUG_HEADER = "mergeMdItemProprietaryIds(): ";
    log.debug3(DEBUG_HEADER + "sourceMdItemSeq = " + sourceMdItemSeq);
    log.debug3(DEBUG_HEADER + "targetMdItemSeq = " + targetMdItemSeq);

    Collection<String> sourceMdItemProprietaryIds = 
        mdManagerSql.getMdItemProprietaryIds(conn, sourceMdItemSeq);

    addNewMdItemProprietaryIds(conn, targetMdItemSeq,
	sourceMdItemProprietaryIds);

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds an Archival Unit to the table of unconfigured Archival Units.
   * 
   * @param au
   *          An ArchivalUnit with the Archival Unit.
   */
  void persistUnconfiguredAu(ArchivalUnit au) {
    final String DEBUG_HEADER = "persistUnconfiguredAu(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "au = " + au);

    Connection conn = null;
    String auId = null;

    try {
      conn = dbManager.getConnection();

      if (conn == null) {
	log.error("Cannot connect to database - Cannot insert archival unit "
	    + au + " in unconfigured table");
	return;
      }

      auId = au.getAuId();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

      if (!isAuInUnconfiguredAuTable(conn, auId)) {
	persistUnconfiguredAu(conn, auId);
	DbManager.commitOrRollback(conn, log);
      }
    } catch (DbException dbe) {
      log.error("Cannot insert archival unit in unconfigured table", dbe);
      log.error("auId = " + auId);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Adds an Archival Unit to the table of unconfigured Archival Units.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void persistUnconfiguredAu(Connection conn, String auId)
      throws DbException {
    mdManagerSql.persistUnconfiguredAu(conn, auId);
  }

  /**
   * Provides the count of recorded unconfigured archival units.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the count of recorded unconfigured archival units.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public long countUnconfiguredAus(Connection conn) throws DbException {
    return mdManagerSql.countUnconfiguredAus(conn);
  }

  /**
   * Provides an indication of whether an Archival Unit is in the table of
   * unconfigured Archival Units.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a boolean with <code>true</code> if the Archival Unit is in the
   *         UNCONFIGURED_AU table, <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public boolean isAuInUnconfiguredAuTable(Connection conn, String auId)
      throws DbException {
    return mdManagerSql.isAuInUnconfiguredAuTable(conn, auId);
  }

  /**
   * Provides the SQL code executor.
   * 
   * @return a MetadataManagerSql with the SQL code executor.
   */
  public MetadataManagerSql getMetadataManagerSql() {
    return mdManagerSql;
  }

  public boolean isPrioritizeIndexingNewAus() {
    return prioritizeIndexingNewAus;
  }

  /**
   * Provides the names of the publishers in the database.
   * 
   * @return a Collection<String> with the publisher names.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Collection<String> getPublisherNames() throws DbException {
    return getMetadataManagerSql().getPublisherNames();
  }

  /**
   * Provides the DOI prefixes for the publishers in the database with multiple
   * DOI prefixes.
   * 
   * @return a Map<String, Collection<String>> with the DOI prefixes keyed by
   *         the publisher name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Map<String, Collection<String>> getPublishersWithMultipleDoiPrefixes()
      throws DbException {
    return getMetadataManagerSql().getPublishersWithMultipleDoiPrefixes();
  }

  /**
   * Provides the publisher names linked to DOI prefixes in the database that
   * are linked to multiple publishers.
   * 
   * @return a Map<String, Collection<String>> with the publisher names keyed by
   *         the DOI prefixes to which they are linked.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Map<String, Collection<String>> getDoiPrefixesWithMultiplePublishers()
      throws DbException {
    return getMetadataManagerSql().getDoiPrefixesWithMultiplePublishers();
  }

  /**
   * Provides the DOI prefixes linked to the Archival Unit name for the Archival
   * Units in the database with multiple DOI prefixes.
   * 
   * @return a Map<String, Collection<String>> with the DOI prefixes keyed by
   *         the Archival Unit name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Map<String, Collection<String>> getAuNamesWithMultipleDoiPrefixes()
      throws DbException {
    final String DEBUG_HEADER = "getAuNamesWithMultipleDoiPrefixes(): ";

    // The Archival Units that have multiple DOI prefixes, sorted by name.
    Map<String, Collection<String>> auNamesWithPrefixes =
	new TreeMap<String, Collection<String>>();

    // Get the DOI prefixes linked to the Archival Units.
    Map<String, Collection<String>> ausDoiPrefixes =
	getAuIdsWithMultipleDoiPrefixes();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "ausDoiPrefixes.size() = " + ausDoiPrefixes.size());

    // Loop through the Archival Units.
    for (String auId : ausDoiPrefixes.keySet()) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

      ArchivalUnit au = pluginMgr.getAuFromId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

      if (au != null) {
	auNamesWithPrefixes.put(au.getName(), ausDoiPrefixes.get(auId));
      } else {
	auNamesWithPrefixes.put(auId, ausDoiPrefixes.get(auId));
      }
    }

    return auNamesWithPrefixes;
  }

  /**
   * Provides the DOI prefixes linked to the Archival Unit identifier for the
   * Archival Units in the database with multiple DOI prefixes.
   * 
   * @return a Map<String, Collection<String>> with the DOI prefixes keyed by
   *         the Archival Unit identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Map<String, Collection<String>> getAuIdsWithMultipleDoiPrefixes()
      throws DbException {
    return getMetadataManagerSql().getAuIdsWithMultipleDoiPrefixes();
  }

  /**
   * Provides the ISBNs for the publications in the database with more than two
   * ISBNS.
   * 
   * @return a Map<String, Collection<Isbn>> with the ISBNs keyed by the
   *         publication name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Map<String, Collection<Isbn>> getPublicationsWithMoreThan2Isbns()
      throws DbException {
    return getMetadataManagerSql().getPublicationsWithMoreThan2Isbns();
  }

  /**
   * Provides the ISSNs for the publications in the database with more than two
   * ISSNS.
   * 
   * @return a Map<String, Collection<Issn>> with the ISSNs keyed by the
   *         publication name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Map<String, Collection<Issn>> getPublicationsWithMoreThan2Issns()
      throws DbException {
    return getMetadataManagerSql().getPublicationsWithMoreThan2Issns();
  }

  /**
   * Provides the publication names linked to ISBNs in the database that are
   * linked to multiple publications.
   * 
   * @return a Map<String, Collection<String>> with the publication names keyed
   *         by the ISBNs to which they are linked.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Map<String, Collection<String>> getIsbnsWithMultiplePublications()
      throws DbException {
    return getMetadataManagerSql().getIsbnsWithMultiplePublications();
  }

  /**
   * Provides the publication names linked to ISSNs in the database that are
   * linked to multiple publications.
   * 
   * @return a Map<String, Collection<String>> with the publication names keyed
   *         by the ISSNs to which they are linked.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Map<String, Collection<String>> getIssnsWithMultiplePublications()
      throws DbException {
    return getMetadataManagerSql().getIssnsWithMultiplePublications();
  }

  /**
   * Provides the ISSNs for books in the database.
   * 
   * @return a Map<String, Collection<String>> with the ISSNs keyed by the
   *         publication name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Map<String, Collection<String>> getBooksWithIssns()
      throws DbException {
    return getMetadataManagerSql().getBooksWithIssns();
  }

  /**
   * Provides the ISBNs for periodicals in the database.
   * 
   * @return a Map<String, Collection<String>> with the ISBNs keyed by the
   *         publication name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Map<String, Collection<String>> getPeriodicalsWithIsbns()
      throws DbException {
    return getMetadataManagerSql().getPeriodicalsWithIsbns();
  }

  /**
   * Provides the Archival Units in the database with an unknown provider.
   * 
   * @return a Collection<String> with the sorted Archival Unit names.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Collection<String> getUnknownProviderAuIds() throws DbException {
    return getMetadataManagerSql().getUnknownProviderAuIds();
  }

  /**
   * Provides the database identifier of an AU.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a Long with the identifier of the AU.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findAuSeq(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "findAuSeq(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    Long auSeq = null;

    // Find the plugin.
    Long pluginSeq =
	  mdManagerSql.findPlugin(conn, PluginManager.pluginIdFromAuId(auId));

    // Check whether the plugin exists.
    if (pluginSeq != null) {
      // Yes: Get the database identifier of the AU.
      String auKey = PluginManager.auKeyFromAuId(auId);

      auSeq = mdManagerSql.findAu(conn, pluginSeq, auKey);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auSeq = " + auSeq);
    return auSeq;
  }

  /**
   * Enables the indexing of an AU.
   * 
   * @param au
   *          An ArchivalUnit with the AU for which indexing is to be enabled.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void enableAuIndexing(ArchivalUnit au) throws DbException {
    final String DEBUG_HEADER = "disableAuIndexing(): ";

    Connection conn = null;

    try {
      log.debug2(DEBUG_HEADER + "Enabling indexing for AU " + au.getName());
      conn = dbManager.getConnection();

      if (conn == null) {
	log.error("Cannot enable indexing for AU '" + au.getName()
	    + "' - Cannot connect to database");
	throw new DbException("Cannot connect to database");
      }

      String auId = au.getAuId();
      log.debug2(DEBUG_HEADER + "auId " + auId);

      // Remove it from the list if it was marked as disabled.
      removeDisabledFromPendingAus(conn, auId);
      DbManager.commitOrRollback(conn, log);
    } catch (DbException dbe) {
      String errorMessage = "Cannot enable indexing for AU '" + au.getName()
	  + "'";
      log.error(errorMessage, dbe);
      throw dbe;
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Provides the journal articles in the database whose parent is not a
   * journal.
   * 
   * @return a Collection<Map<String, String>> with the mismatched journal
   *         articles sorted by Archival Unit, parent name and child name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Collection<Map<String, String>> getMismatchedParentJournalArticles()
      throws DbException {
    return getMetadataManagerSql().getMismatchedParentJournalArticles();
  }

  /**
   * Provides the book chapters in the database whose parent is not a book or a
   * book series.
   * 
   * @return a Collection<Map<String, String>> with the mismatched book chapters
   *         sorted by Archival Unit, parent name and child name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Collection<Map<String, String>> getMismatchedParentBookChapters()
      throws DbException {
    return getMetadataManagerSql().getMismatchedParentBookChapters();
  }

  /**
   * Provides the book volumes in the database whose parent is not a book or a
   * book series.
   * 
   * @return a Collection<Map<String, String>> with the mismatched book volumes
   *         sorted by Archival Unit, parent name and child name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Collection<Map<String, String>> getMismatchedParentBookVolumes()
      throws DbException {
    return getMetadataManagerSql().getMismatchedParentBookVolumes();
  }
}
