/*
 * $Id: MetadataManager.java,v 1.20 2013-07-23 21:47:45 fergaloy-sf Exp $
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
package org.lockss.metadata;

import static java.sql.Types.*;
import static org.lockss.db.DbManager.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.lockss.util.TimeBase;
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

  /** Name of metadata status table */
  public static final String METADATA_STATUS_TABLE_NAME =
      "MetadataStatusTable";
  
  /** Name of metadata status table error table */
  public static final String METADATA_STATUS_ERROR_TABLE_NAME =
      "MetadataStatusErrorTable";

  /** Name of metadata status table error info table */
  public static final String METADATA_STATUS_ERROR_INFO_TABLE_NAME =
      "MetadataStatusErrorInfoTable";

  /** Map of AUID regexp to priority.  If set, AUs are assigned the
   * corresponding priority of the first regexp that their AUID matches.
   * Priority must be an integer; priorities <= -10000 mean "do not index
   * matching AUs", priorities <= -20000 mean "abort running indexes of
   * matching AUs". (Priorities are not yet implemented - only "do not
   * index" and "abort" are supported.)  */
  static final String PARAM_INDEX_PRIORITY_AUID_MAP =
    PREFIX + "indexPriorityAuidMap";
  static final List<String> DEFAULT_INDEX_PRIORITY_AUID_MAP = null;

  private static final int FAILED_INDEX_PRIORITY = -1000;
  private static final int MIN_INDEX_PRIORITY = -10000;
  private static final int ABORT_INDEX_PRIORITY = -20000;

  /** Maximum number of AUs to be re-indexed to batch before writing them to the
   * database. */
  public static final String PARAM_MAX_PENDING_TO_REINDEX_AU_BATCH_SIZE =
    PREFIX + "maxPendingToReindexAuBatchSize";
  public static final int DEFAULT_MAX_PENDING_TO_REINDEX_AU_BATCH_SIZE = 1000;

  private static final int UNKNOWN_VERSION = -1;

  /**
   * The initial value of the metadata extraction time for an AU whose metadata
   * has not been extracted yet.
   */
  public static final long NEVER_EXTRACTED_EXTRACTION_TIME = 0L;

  /**
   * The standard type of a name that is primary.
   */
  public static final String PRIMARY_NAME_TYPE = "primary";

  /**
   * The standard type of a name that is not primary.
   */
  public static final String NOT_PRIMARY_NAME_TYPE = "not_primary";

  /**
   * The standard types of ISBNs and ISSNs.
   */
  public static final String E_ISBN_TYPE = "e_isbn";
  public static final String E_ISSN_TYPE = "e_issn";
  public static final String L_ISSN_TYPE = "l_issn";
  public static final String P_ISBN_TYPE = "p_isbn";
  public static final String P_ISSN_TYPE = "p_issn";

  public static final String ACCESS_URL_FEATURE = "Access";

  // Query to find a metadata item type by its name.
  private static final String FIND_MD_ITEM_TYPE_QUERY = "select "
      + MD_ITEM_TYPE_SEQ_COLUMN
      + " from " + MD_ITEM_TYPE_TABLE
      + " where " + TYPE_NAME_COLUMN + " = ?";

  // Query to find a publisher by its name.
  private static final String FIND_PUBLISHER_QUERY = "select "
      + PUBLISHER_SEQ_COLUMN
      + " from " + PUBLISHER_TABLE
      + " where " + PUBLISHER_NAME_COLUMN + " = ?";

  // Query to add a publisher.
  private static final String INSERT_PUBLISHER_QUERY = "insert into "
      + PUBLISHER_TABLE
      + "(" + PUBLISHER_SEQ_COLUMN
      + "," + PUBLISHER_NAME_COLUMN
      + ") values (default,?)";

  // Query to find a plugin by its identifier.
  private static final String FIND_PLUGIN_QUERY = "select "
      + PLUGIN_SEQ_COLUMN
      + " from " + PLUGIN_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?";

  // Query to add a plugin.
  private static final String INSERT_PLUGIN_QUERY = "insert into "
      + PLUGIN_TABLE
      + "(" + PLUGIN_SEQ_COLUMN
      + "," + PLUGIN_ID_COLUMN
      + "," + PLATFORM_SEQ_COLUMN
      + ") values (default,?,?)";

  // Query to find an Archival Unit by its plugin and key.
  private static final String FIND_AU_QUERY = "select "
      + AU_SEQ_COLUMN
      + " from " + AU_TABLE
      + " where " + PLUGIN_SEQ_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to add an Archival Unit.
  private static final String INSERT_AU_QUERY = "insert into "
      + AU_TABLE
      + "(" + AU_SEQ_COLUMN
      + "," + PLUGIN_SEQ_COLUMN
      + "," + AU_KEY_COLUMN
      + ") values (default,?,?)";

  // Query to find the metadata entry of an Archival Unit.
  private static final String FIND_AU_MD_QUERY = "select "
      + AU_MD_SEQ_COLUMN
      + " from " + AU_MD_TABLE
      + " where " + AU_SEQ_COLUMN + " = ?";

  // Query to add an Archival Unit metadata entry.
  private static final String INSERT_AU_MD_QUERY = "insert into "
      + AU_MD_TABLE
      + "(" + AU_MD_SEQ_COLUMN
      + "," + AU_SEQ_COLUMN
      + "," + MD_VERSION_COLUMN
      + "," + EXTRACT_TIME_COLUMN
      + ") values (default,?,?,?)";

  // Query to add a metadata item.
  private static final String INSERT_MD_ITEM_QUERY = "insert into "
      + MD_ITEM_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + PARENT_SEQ_COLUMN
      + "," + MD_ITEM_TYPE_SEQ_COLUMN
      + "," + AU_MD_SEQ_COLUMN
      + "," + DATE_COLUMN
      + "," + COVERAGE_COLUMN
      + ") values (default,?,?,?,?,?)";

  // Query to add a publication.
  private static final String INSERT_PUBLICATION_QUERY = "insert into "
      + PUBLICATION_TABLE
      + "(" + PUBLICATION_SEQ_COLUMN
      + "," + MD_ITEM_SEQ_COLUMN
      + "," + PUBLISHER_SEQ_COLUMN
      + "," + PUBLICATION_ID_COLUMN
      + ") values (default,?,?,?)";

  // Query to add a metadata item name.
  private static final String INSERT_MD_ITEM_NAME_QUERY = "insert into "
      + MD_ITEM_NAME_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + NAME_COLUMN
      + "," + NAME_TYPE_COLUMN
      + ") values (?,?,?)";
	
  // Query to add an ISSN.
  private static final String INSERT_ISSN_QUERY = "insert into "
      + ISSN_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + ISSN_COLUMN
      + "," + ISSN_TYPE_COLUMN
      + ") values (?,?,?)";
	
  // Query to add an ISBN.
  private static final String INSERT_ISBN_QUERY = "insert into "
      + ISBN_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + ISBN_COLUMN
      + "," + ISBN_TYPE_COLUMN
      + ") values (?,?,?)";

  // Query to find a publication by its ISSNs.
  private static final String FIND_PUBLICATION_BY_ISSNS_QUERY = "select"
      + " p." + PUBLICATION_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE + " p,"
      + ISSN_TABLE + " i,"
      + MD_ITEM_TABLE + " m,"
      + MD_ITEM_TYPE_TABLE + " t"
      + " where p." + PUBLISHER_SEQ_COLUMN + " = ?"
      + " and m." + AU_MD_SEQ_COLUMN + " is null"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = i." + MD_ITEM_SEQ_COLUMN
      + " and (i." + ISSN_COLUMN + " = ?"
      + " or i." + ISSN_COLUMN + " = ?)"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + MD_ITEM_SEQ_COLUMN
      + " and m." + MD_ITEM_TYPE_SEQ_COLUMN + " = t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and t." + TYPE_NAME_COLUMN + " = ?";

  // Query to find a publication by its ISBNs.
  private static final String FIND_PUBLICATION_BY_ISBNS_QUERY = "select"
      + " p." + PUBLICATION_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE + " p,"
      + ISBN_TABLE + " i,"
      + MD_ITEM_TABLE + " m,"
      + MD_ITEM_TYPE_TABLE + " t"
      + " where p." + PUBLISHER_SEQ_COLUMN + " = ?"
      + " and m." + AU_MD_SEQ_COLUMN + " is null"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = i." + MD_ITEM_SEQ_COLUMN
      + " and (i." + ISBN_COLUMN + " = ?"
      + " or i." + ISBN_COLUMN + " = ?)"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + MD_ITEM_SEQ_COLUMN
      + " and m." + MD_ITEM_TYPE_SEQ_COLUMN + " = t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and t." + TYPE_NAME_COLUMN + " = ?";

  // Query to find a publication by its name.
  private static final String FIND_PUBLICATION_BY_NAME_QUERY =
      "select p." + PUBLICATION_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE + " p,"
      + MD_ITEM_TABLE + " m,"
      + MD_ITEM_NAME_TABLE + " n,"
      + MD_ITEM_TYPE_TABLE + " t"
      + " where p." + PUBLISHER_SEQ_COLUMN + " = ?"
      + " and m." + AU_MD_SEQ_COLUMN + " is null"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + MD_ITEM_SEQ_COLUMN
      + " and p." + MD_ITEM_SEQ_COLUMN + " = n." + MD_ITEM_SEQ_COLUMN
      + " and n." + NAME_COLUMN + " = ?"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + MD_ITEM_SEQ_COLUMN
      + " and m." + MD_ITEM_TYPE_SEQ_COLUMN + " = t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and t." + TYPE_NAME_COLUMN + " = ?";

  // Query to find the metadata item of a publication.
  private static final String FIND_PUBLICATION_METADATA_ITEM_QUERY = "select "
      + MD_ITEM_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?";

  // Query to find the secondary names of a metadata item.
  private static final String FIND_MD_ITEM_NAME_QUERY = "select "
      + NAME_COLUMN
      + "," + NAME_TYPE_COLUMN
      + " from " + MD_ITEM_NAME_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to find the ISSNs of a metadata item.
  private static final String FIND_MD_ITEM_ISSN_QUERY = "select "
      + ISSN_COLUMN
      + "," + ISSN_TYPE_COLUMN
      + " from " + ISSN_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to find the ISBNs of a metadata item.
  private static final String FIND_MD_ITEM_ISBN_QUERY = "select "
      + ISBN_COLUMN
      + "," + ISBN_TYPE_COLUMN
      + " from " + ISBN_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to update the extraction time of the metadata of an Archival Unit.
  private static final String UPDATE_AU_MD_EXTRACT_TIME_QUERY = "update "
      + AU_MD_TABLE
      + " set " + EXTRACT_TIME_COLUMN + " = ?"
      + " where " + AU_MD_SEQ_COLUMN + " = ?";

  // Query to count bibliographic items.
  private static final String COUNT_BIB_ITEM_QUERY = "select count(*) from "
      + BIB_ITEM_TABLE;

  // Query to get the identifier of the metadata of an AU in the database.
  private static final String FIND_AU_MD_BY_AU_ID_QUERY = "select m."
      + AU_MD_SEQ_COLUMN
      + " from " + AU_MD_TABLE + " m,"
      + AU_TABLE + " a,"
      + PLUGIN_TABLE + " p"
      + " where m." + AU_SEQ_COLUMN + " = " + "a." + AU_SEQ_COLUMN
      + " and a." + PLUGIN_SEQ_COLUMN + " = " + "p." + PLUGIN_SEQ_COLUMN
      + " and p." + PLUGIN_ID_COLUMN + " = ?"
      + " and a." + AU_KEY_COLUMN + " = ?";

  // Query to delete metadata items by Archival Unit key and plugin identifier.
  private static final String DELETE_MD_ITEM_QUERY = "delete from "
      + MD_ITEM_TABLE
      + " where "
      + AU_MD_SEQ_COLUMN + " = ?";

  // Query to get the identifier of an AU in the database.
  private static final String FIND_AU_BY_AU_ID_QUERY = "select a."
      + AU_SEQ_COLUMN
      + " from " + AU_TABLE + " a,"
      + PLUGIN_TABLE + " p"
      + " where a." + PLUGIN_SEQ_COLUMN + " = " + "p." + PLUGIN_SEQ_COLUMN
      + " and p." + PLUGIN_ID_COLUMN + " = ?"
      + " and a." + AU_KEY_COLUMN + " = ?";

  // Query to delete an AU by Archival Unit key and plugin identifier.
  private static final String DELETE_AU_QUERY = "delete from " + AU_TABLE
      + " where "
      + AU_SEQ_COLUMN + " = ?";

  // Query to add a metadata item URL.
  private static final String INSERT_URL_QUERY = "insert into "
      + URL_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + FEATURE_COLUMN
      + "," + URL_COLUMN
      + ") values (?,?,?)";

  // Query to add a metadata item DOI.
  private static final String INSERT_DOI_QUERY = "insert into "
      + DOI_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + DOI_COLUMN
      + ") values (?,?)";

  // Query to count enabled pending AUs.
  private static final String COUNT_ENABLED_PENDING_AUS_QUERY = "select "
      + "count(*) from " + PENDING_AU_TABLE
      + " where " + PRIORITY_COLUMN + " >= 0";
  
  // Query to find enabled pending AUs sorted by priority.
  private static final String FIND_PRIORITIZED_ENABLED_PENDING_AUS_QUERY =
      "select "
      + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + PRIORITY_COLUMN
      + " from " + PENDING_AU_TABLE
      + " where " + PRIORITY_COLUMN + " >= 0"
      + " order by " + PRIORITY_COLUMN;
  
  // Query to find a pending AU by its key and plugin identifier.
  private static final String FIND_PENDING_AU_QUERY = "select "
      + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + " from " + PENDING_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to add an enabled pending AU at the bottom of the current priority
  // list.
  private static final String INSERT_ENABLED_PENDING_AU_QUERY = "insert into "
      + PENDING_AU_TABLE
      + "(" + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + PRIORITY_COLUMN
      + ") values (?,?,"
      + "(select coalesce(max(" + PRIORITY_COLUMN + "), 0) + 1"
      + " from " + PENDING_AU_TABLE
      + " where " + PRIORITY_COLUMN + " >= 0))";

  // Query to add a disabled pending AU.
  private static final String INSERT_DISABLED_PENDING_AU_QUERY = "insert into "
      + PENDING_AU_TABLE
      + "(" + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + PRIORITY_COLUMN
      + ") values (?,?," + MIN_INDEX_PRIORITY + ")";

  // Query to add a pending AU with failed indexing.
  private static final String INSERT_FAILED_INDEXING_PENDING_AU_QUERY = "insert"
      + " into "
      + PENDING_AU_TABLE
      + "(" + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + PRIORITY_COLUMN
      + ") values (?,?," + FAILED_INDEX_PRIORITY + ")";

  // Query to delete a pending AU by its key and plugin identifier.
  private static final String DELETE_PENDING_AU_QUERY = "delete from "
      + PENDING_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to delete a disabled pending AU by its key and plugin identifier.
  private static final String DELETE_DISABLED_PENDING_AU_QUERY = "delete from "
      + PENDING_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?"
      + " and " + PRIORITY_COLUMN + " < 0";
  
  // Query to find pending AUs with a given priority.
  private static final String FIND_PENDING_AUS_WITH_PRIORITY_QUERY =
      "select "
      + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + " from " + PENDING_AU_TABLE
      + " where " + PRIORITY_COLUMN + " = ?";

  // Query to get the version of the metadata of an AU as is recorded in the
  // database.
  private static final String FIND_AU_METADATA_VERSION_QUERY = "select m."
      + MD_VERSION_COLUMN
      + " from " + AU_MD_TABLE + " m,"
      + AU_TABLE + " a,"
      + PLUGIN_TABLE + " p"
      + " where m." + AU_SEQ_COLUMN + " = " + " a." + AU_SEQ_COLUMN
      + " and a." + PLUGIN_SEQ_COLUMN + " = " + " p." + PLUGIN_SEQ_COLUMN
      + " and p." + PLUGIN_ID_COLUMN + " = ?"
      + " and a." + AU_KEY_COLUMN + " = ?";

  // Query to find the extraction time of an Archival Unit.
  private static final String FIND_AU_MD_EXTRACT_TIME_BY_AUSEQ_QUERY = "select "
      + EXTRACT_TIME_COLUMN
      + " from " + AU_MD_TABLE
      + " where " + AU_SEQ_COLUMN + " = ?";

  // Query to find the extraction time of an Archival Unit.
  private static final String FIND_AU_MD_EXTRACT_TIME_BY_AU_QUERY = "select m."
      + EXTRACT_TIME_COLUMN
      + " from " + AU_MD_TABLE + " m,"
      + AU_TABLE + " a,"
      + PLUGIN_TABLE + " p"
      + " where m." + AU_SEQ_COLUMN + " = " + " a." + AU_SEQ_COLUMN
      + " and a." + PLUGIN_SEQ_COLUMN + " = " + " p." + PLUGIN_SEQ_COLUMN
      + " and p." + PLUGIN_ID_COLUMN + " = ?"
      + " and a." + AU_KEY_COLUMN + " = ?";

  // Query to find a platform by its name.
  private static final String FIND_PLATFORM_QUERY = "select "
      + PLATFORM_SEQ_COLUMN
      + " from " + PLATFORM_TABLE
      + " where " + PLATFORM_NAME_COLUMN + " = ?";

  // Query to add a platform.
  private static final String INSERT_PLATFORM_QUERY = "insert into "
      + PLATFORM_TABLE
      + "(" + PLATFORM_SEQ_COLUMN
      + "," + PLATFORM_NAME_COLUMN
      + ") values (default,?)";

  // Query to find the publisher of an Archival Unit.
  private static final String FIND_AU_PUBLISHER_QUERY = "select distinct "
      + "pr." + PUBLISHER_SEQ_COLUMN
      + " from " + PUBLISHER_TABLE + " pr"
      + "," + PUBLICATION_TABLE + " p"
      + "," + MD_ITEM_TABLE + " m"
      + "," + AU_MD_TABLE + " am"
      + " where pr." + PUBLISHER_SEQ_COLUMN + " = p." + PUBLISHER_SEQ_COLUMN
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + PARENT_SEQ_COLUMN
      + " and m." + AU_MD_SEQ_COLUMN + " = am." + AU_MD_SEQ_COLUMN
      + " and am." + AU_SEQ_COLUMN + " = ?";

  // Query to get the name of a publisher.
  private static final String GET_PUBLISHER_NAME_QUERY = "select "
      + PUBLISHER_NAME_COLUMN
      + " from " + PUBLISHER_TABLE
      + " where " + PUBLISHER_SEQ_COLUMN + " = ?";
  
  // Query to find AU problems.
  private static final String FIND_AU_PROBLEMS_QUERY = "select "
      + PROBLEM_COLUMN
      + " from " + AU_PROBLEM_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to add an AU problem entry.
  private static final String INSERT_AU_PROBLEM_QUERY = "insert into "
      + AU_PROBLEM_TABLE
      + "(" + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + PROBLEM_COLUMN
      + ") values (?,?,?)";

  // Query to delete the problems of an AU.
  private static final String DELETE_AU_PROBLEMS_QUERY = "delete from "
      + AU_PROBLEM_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to delete an AU problem entry.
  private static final String DELETE_AU_PROBLEM_QUERY = "delete from "
      + AU_PROBLEM_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?"
      + " and " + PROBLEM_COLUMN + " = ?";

  // Query to find the publications of a publisher.
  private static final String FIND_PUBLISHER_PUBLICATIONS_QUERY = "select "
      + PUBLICATION_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE
      + " where " + PUBLISHER_SEQ_COLUMN + " = ?";

  // Query to find the metadata items of a publication.
  private static final String FIND_PUBLICATION_CHILD_MD_ITEMS_QUERY = "select "
      + "distinct "
      + "m." + MD_ITEM_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE + " p"
      + "," + MD_ITEM_TABLE + " m"
      + " where p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + PARENT_SEQ_COLUMN;

  // Query to find the Archival Units of a publisher.
  private static final String FIND_PUBLISHER_AUS_QUERY = "select distinct "
      + "am." + AU_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE + " p"
      + "," + MD_ITEM_TABLE + " m"
      + "," + AU_MD_TABLE + " am"
      + " where p." + PUBLISHER_SEQ_COLUMN + " = ?"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + PARENT_SEQ_COLUMN
      + " and m." + AU_MD_SEQ_COLUMN + " = am." + AU_MD_SEQ_COLUMN;

  // Query to update the parent sequence of a metadata item.
  private static final String UPDATE_MD_ITEM_PARENT_SEQ_QUERY = "update "
      + MD_ITEM_TABLE
      + " set " + PARENT_SEQ_COLUMN + " = ?"
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to find the featured URLs of a metadata item.
  private static final String FIND_MD_ITEM_FEATURED_URL_QUERY = "select "
      + FEATURE_COLUMN + "," + URL_COLUMN
      + " from " + URL_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to find the authors of a metadata item.
  private static final String FIND_MD_ITEM_AUTHOR_QUERY = "select "
      + AUTHOR_NAME_COLUMN
      + " from " + AUTHOR_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to find the keywords of a metadata item.
  private static final String FIND_MD_ITEM_KEYWORD_QUERY = "select "
      + KEYWORD_COLUMN
      + " from " + KEYWORD_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to add a metadata item author.
  private static final String INSERT_AUTHOR_QUERY = "insert into "
      + AUTHOR_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + AUTHOR_NAME_COLUMN
      + "," + AUTHOR_IDX_COLUMN
      + ") values (?,?,"
      + "(select coalesce(max(" + AUTHOR_IDX_COLUMN + "), 0) + 1"
      + " from " + AUTHOR_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?))";

  // Query to add a metadata item keyword.
  private static final String INSERT_KEYWORD_QUERY = "insert into "
      + KEYWORD_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + KEYWORD_COLUMN
      + ") values (?,?)";

  // Query to find a metadata item by its type, Archival Unit and access URL.
  private static final String FIND_MD_ITEM_QUERY = "select "
      + "m." + MD_ITEM_SEQ_COLUMN
      + " from " + MD_ITEM_TABLE + " m"
      + "," + URL_TABLE + " u"
      + " where m." + MD_ITEM_TYPE_SEQ_COLUMN + " = ?"
      + " and m." + AU_MD_SEQ_COLUMN + " = ?"
      + " and m." + MD_ITEM_SEQ_COLUMN + " = u." + MD_ITEM_SEQ_COLUMN
      + " and u." + FEATURE_COLUMN + " = '" + ACCESS_URL_FEATURE + "'"
      + " and u." + URL_COLUMN + " = ?";

  // Query to update the publication proprietary identifier.
  private static final String UPDATE_PUBLICATION_ID_QUERY = "update "
      + PUBLICATION_TABLE
      + " set " + PUBLICATION_ID_COLUMN + " = ?"
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?";

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

  // The number of AUs pending to be reindexed.
  private long pendingAusCount = 0;

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

  //private Connection pendingAusBatchConnection = null;
  //private PreparedStatement insertPendingAuBatchStatement = null;

  /** enumeration status for reindexing tasks */
  public enum ReindexingStatus {
    Running, // if the reindexing task is running
    Success, // if the reindexing task was successful
    Failed, // if the reindexing task failed
    Rescheduled // if the reindexing task was rescheduled
  };

  /**
   * Starts the MetadataManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    log.debug(DEBUG_HEADER + "Starting MetadataManager");

    pluginMgr = getDaemon().getPluginManager();
    dbManager = getDaemon().getDbManager();

    // Get a connection to the database.
    Connection conn;

    try {
      conn = dbManager.getConnection();
    } catch (DbException dbe) {
      log.error("Cannot connect to database", dbe);
      return;
    }

    // Initialize the counts of articles and pending AUs from database.
    try {
      pendingAusCount = getEnabledPendingAusCount(conn);
      metadataArticleCount = getArticleCount(conn);
    } catch (DbException dbe) {
      log.error("Cannot get pending AUs and article counts", dbe);
    }

    DbManager.safeRollbackAndClose(conn);

    StatusService statusServ = getDaemon().getStatusService();
    statusServ.registerStatusAccessor(METADATA_STATUS_TABLE_NAME,
        new MetadataManagerStatusAccessor(this));
    statusServ.registerStatusAccessor(METADATA_STATUS_ERROR_TABLE_NAME,
        new MetadataManagerErrorStatusAccessor(this));
    statusServ.registerStatusAccessor(METADATA_STATUS_ERROR_INFO_TABLE_NAME,
        new MetadataIndexingErrorInfoAccessor(this));
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
   * Provides the number of enabled pending AUs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the number of enabled pending AUs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private long getEnabledPendingAusCount(Connection conn) throws DbException {
    final String DEBUG_HEADER = "getEnabledPendingAusCount(): ";
    long rowCount = -1;

    PreparedStatement stmt =
	dbManager.prepareStatement(conn, COUNT_ENABLED_PENDING_AUS_QUERY);
    ResultSet resultSet = null;

    try {
      resultSet = dbManager.executeQuery(stmt);
      resultSet.next();
      rowCount = resultSet.getLong(1);
    } catch (SQLException sqle) {
      log.error("Cannot get the count of enabled pending AUs", sqle);
      log.error("SQL = '" + COUNT_ENABLED_PENDING_AUS_QUERY + "'.");
      throw new DbException("Cannot get the count of enabled pending AUs",
	  sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);
    }

    log.debug3(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides the number of articles in the metadata database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the number of articles in the metadata database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private long getArticleCount(Connection conn) throws DbException {
    final String DEBUG_HEADER = "getArticleCount(): ";
    long rowCount = -1;

    PreparedStatement stmt =
	dbManager.prepareStatement(conn, COUNT_BIB_ITEM_QUERY);
    ResultSet resultSet = null;
    try {
      resultSet = dbManager.executeQuery(stmt);
      resultSet.next();
      rowCount = resultSet.getLong(1);
    } catch (SQLException sqle) {
      log.error("Cannot get the count of articles", sqle);
      log.error("SQL = '" + COUNT_BIB_ITEM_QUERY + "'.");
      throw new DbException("Cannot get the count of articles", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);
    }

    log.debug3(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
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

    if (!getDaemon().isDaemonInited()) {
      log.debug(DEBUG_HEADER + "Daemon not initialized: No reindexing tasks.");
      return 0;
    }

    // Don't run reindexing tasks run if reindexing is disabled.
    if (!reindexingEnabled) {
      log.debug(DEBUG_HEADER
	  + "Metadata manager reindexing is disabled: No reindexing tasks.");
      return 0;
    }

    int reindexedTaskCount = 0;

    synchronized (activeReindexingTasks) {
      // Try to add more concurrent reindexing tasks as long as the maximum
      // number of them is not reached.
      while (activeReindexingTasks.size() < maxReindexingTasks) {
        // Get the list of pending AUs to reindex.
	List<String> auIds =
	    getPrioritizedAuIdsToReindex(conn, maxReindexingTasks
		- activeReindexingTasks.size());

	// Nothing more to do if there are no pending AUs to reindex.
        if (auIds.isEmpty()) {
          break;
        }

        // Loop through all the pending AUs. 
        for (String auId : auIds) {
          // Get the next pending AU.
          ArchivalUnit au = pluginMgr.getAuFromId(auId);

          // Check whether it does not exist.
          if (au == null) {
	    // Yes: Cancel any running tasks associated with the AU and delete
	    // the AU metadata.
            try {
              int count = deleteAu(conn, auId);
              notifyDeletedAu(auId, count);
            } catch (DbException dbe) {
	      log.error("Error removing AU for auId " + auId
		  + " from the table of pending AUs", dbe);
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
                removeFromPendingAus(conn, au.getAuId());
              } catch (DbException dbe) {
                log.error("Error removing AU " + au.getName()
                          + " from the table of pending AUs", dbe);
                break;
              }
            } else {
              // No: Schedule the pending AU.
	      log.debug3(DEBUG_HEADER + "Creating the reindexing task for AU: "
		  + au.getName());
              ReindexingTask task = new ReindexingTask(au, ae);
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
   * Provides a list of AuIds that require reindexing sorted by priority.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param maxAuIds
   *          An int with the maximum number of AuIds to return.
   * @return a List<String> with the list of AuIds that require reindexing
   *         sorted by priority.
   */
  public List<String> getPrioritizedAuIdsToReindex(Connection conn,
                                                   int maxAuIds) {
    final String DEBUG_HEADER = "getPrioritizedAuIdsToReindex(): ";
    ArrayList<String> auIds = new ArrayList<String>();

    if (pluginMgr != null) {
      PreparedStatement selectPendingAus = null;
      ResultSet results = null;
      String sql = FIND_PRIORITIZED_ENABLED_PENDING_AUS_QUERY;
      log.debug3("SQL = '" + sql + "'.");

      try {
	selectPendingAus = dbManager.prepareStatement(conn, sql);
        results = dbManager.executeQuery(selectPendingAus);

        while ((auIds.size() < maxAuIds) && results.next()) {
          String pluginId = results.getString(PLUGIN_ID_COLUMN);
          log.debug2(DEBUG_HEADER + "pluginId = " + pluginId);
          String auKey = results.getString(AU_KEY_COLUMN);
          log.debug2(DEBUG_HEADER + "auKey = " + auKey);
          log.debug2(DEBUG_HEADER + "priority = "
              + results.getString(PRIORITY_COLUMN));
          String auId = PluginManager.generateAuId(pluginId, auKey);

	  if (isEligibleForReindexing(auId)) {
	    if (!activeReindexingTasks.containsKey(auId)) {
	      auIds.add(auId);
	      log.debug2(DEBUG_HEADER + "Added auId = " + auId
		  + " to reindex list");
	    }
	  }
	}
      } catch (SQLException sqle) {
	log.error("Cannot identify the enabled pending AUs", sqle);
	log.error("SQL = '" + sql + "'.");
      } catch (DbException dbe) {
	log.error("Cannot find the enabled pending AUs", dbe);
	log.error("SQL = '" + sql + "'.");
      } finally {
        DbManager.safeCloseResultSet(results);
        DbManager.safeCloseStatement(selectPendingAus);
      }
    }

    auIds.trimToSize();
    return auIds;
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
    int articleCount = removeAuMetadataItems(conn, auId);
    log.debug3(DEBUG_HEADER + "articleCount = " + articleCount);

    removeAu(conn, auId);

    // Remove pending reindexing operations for this AU.
    removeFromPendingAus(conn, auId);

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
   * Removes an AU from the pending Aus table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void removeFromPendingAus(Connection conn, String auId) throws DbException {
    PreparedStatement deletePendingAu =
	dbManager.prepareStatement(conn, DELETE_PENDING_AU_QUERY);

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      String auKey = PluginManager.auKeyFromAuId(auId);
  
      deletePendingAu.setString(1, pluginId);
      deletePendingAu.setString(2, auKey);
      dbManager.executeUpdate(deletePendingAu);
    } catch (SQLException sqle) {
      log.error("Cannot remove AU from pending table", sqle);
      log.error("auId = '" + auId + "'.");
      log.error("SQL = '" + DELETE_PENDING_AU_QUERY + "'.");
      throw new DbException("Cannot remove AU from pending table", sqle);
    } finally {
      DbManager.safeCloseStatement(deletePendingAu);
    }

    pendingAusCount = getEnabledPendingAusCount(conn);
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
   * Removes all metadata items for an AU.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @return an int with the number of metadata items deleted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int removeAuMetadataItems(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "removeAuMetadataItems(): ";
    log.debug3(DEBUG_HEADER + "auid = " + auId);
    int count = 0;

    Long auMdSeq = findAuMdByAuId(conn, auId);
    log.debug3(DEBUG_HEADER + "auMdSeq = " + auMdSeq);

    if (auMdSeq != null) {
      log.debug3(DEBUG_HEADER + "SQL = '" + DELETE_MD_ITEM_QUERY + "'.");
      PreparedStatement deleteMetadataItems = dbManager.prepareStatement(conn,
	  DELETE_MD_ITEM_QUERY);

      try {
	deleteMetadataItems.setLong(1, auMdSeq);
	count = dbManager.executeUpdate(deleteMetadataItems);
      } catch (SQLException sqle) {
	log.error("Cannot delete AU metadata items", sqle);
	log.error("auid = " + auId);
	log.error("SQL = '" + DELETE_MD_ITEM_QUERY + "'.");
	throw new DbException("Cannot delete AU metadata items", sqle);
      } finally {
	DbManager.safeCloseStatement(deleteMetadataItems);
      }
    }

    log.debug3(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Provides the identifier of an Archival Unit metadata.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @return a Long with the identifier of the Archival Unit metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findAuMdByAuId(Connection conn, String auId)
      throws DbException {
    final String DEBUG_HEADER = "findAuMdByAuId(): ";
    log.debug3(DEBUG_HEADER + "auid = " + auId);

    Long auMdSeq = null;
    log.debug3(DEBUG_HEADER + "SQL = '" + FIND_AU_MD_BY_AU_ID_QUERY + "'.");
    PreparedStatement findAuMd =
	dbManager.prepareStatement(conn, FIND_AU_MD_BY_AU_ID_QUERY);
    ResultSet resultSet = null;

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      log.debug2(DEBUG_HEADER + "pluginId() = " + pluginId);

      String auKey = PluginManager.auKeyFromAuId(auId);
      log.debug2(DEBUG_HEADER + "auKey = " + auKey);

      findAuMd.setString(1, pluginId);
      findAuMd.setString(2, auKey);
      resultSet = dbManager.executeQuery(findAuMd);

      if (resultSet.next()) {
	auMdSeq = resultSet.getLong(AU_MD_SEQ_COLUMN);
	log.debug2(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find AU metadata identifier", sqle);
      log.error("auid = " + auId);
      log.error("SQL = '" + FIND_AU_MD_BY_AU_ID_QUERY + "'.");
      throw new DbException("Cannot find AU metadata identifier", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAuMd);
    }

    return auMdSeq;
  }

  /**
   * Removes an AU.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @return an int with the number of rows deleted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int removeAu(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "removeAu(): ";
    log.debug3(DEBUG_HEADER + "auid = " + auId);
    int count = 0;

    Long auSeq = findAuByAuId(conn, auId);
    log.debug3(DEBUG_HEADER + "auSeq = " + auSeq);

    if (auSeq != null) {
      log.debug3(DEBUG_HEADER + "SQL = '" + DELETE_AU_QUERY + "'.");
      PreparedStatement deleteAu =
	  dbManager.prepareStatement(conn, DELETE_AU_QUERY);

      try {
	deleteAu.setLong(1, auSeq);
	count = dbManager.executeUpdate(deleteAu);
      } catch (SQLException sqle) {
	log.error("Cannot delete AU", sqle);
	log.error("auid = " + auId);
	log.error("SQL = '" + DELETE_AU_QUERY + "'.");
	throw new DbException("Cannot delete AU", sqle);
      } finally {
	DbManager.safeCloseStatement(deleteAu);
      }
    }

    log.debug3(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Provides the identifier of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @return a Long with the identifier of the Archival Unit.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findAuByAuId(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "findAuByAuId(): ";
    log.debug3(DEBUG_HEADER + "auid = " + auId);

    Long auSeq = null;
    log.debug3(DEBUG_HEADER + "SQL = '" + FIND_AU_BY_AU_ID_QUERY + "'.");
    PreparedStatement findAu =
	dbManager.prepareStatement(conn, FIND_AU_BY_AU_ID_QUERY);
    ResultSet resultSet = null;

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      log.debug2(DEBUG_HEADER + "pluginId = " + pluginId);

      String auKey = PluginManager.auKeyFromAuId(auId);
      log.debug2(DEBUG_HEADER + "auKey = " + auKey);

      findAu.setString(1, pluginId);
      findAu.setString(2, auKey);
      resultSet = dbManager.executeQuery(findAu);

      if (resultSet.next()) {
	auSeq = resultSet.getLong(AU_SEQ_COLUMN);
	log.debug2(DEBUG_HEADER + "auSeq = " + auSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find AU identifier", sqle);
      log.error("auid = " + auId);
      log.error("SQL = '" + FIND_AU_BY_AU_ID_QUERY + "'.");
      throw new DbException("Cannot find AU identifier", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAu);
    }

    return auSeq;
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
  private boolean isEligibleForReindexing(String auId) {
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
   * Provides the number of distinct articles in the metadata database.
   * 
   * @return a long with the number of distinct articles in the metadata
   *         database.
   */
  public long getArticleCount() {
    return metadataArticleCount;
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
   * @return a Long with the identifier of the plugin.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreatePlugin(Connection conn, String pluginId,
      Long platformSeq) throws DbException {
    final String DEBUG_HEADER = "findOrCreatePlugin(): ";
    log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
    Long pluginSeq = findPlugin(conn, pluginId);
    log.debug3(DEBUG_HEADER + "pluginSeq = " + pluginSeq);

    // Check whether it is a new plugin.
    if (pluginSeq == null) {
      // Yes: Add to the database the new plugin.
      pluginSeq = addPlugin(conn, pluginId, platformSeq);
      log.debug3(DEBUG_HEADER + "new pluginSeq = " + pluginSeq);
    }

    return pluginSeq;
  }

  /**
   * Provides the identifier of a plugin.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginId
   *          A String with the plugin identifier.
   * @return a Long with the identifier of the plugin.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findPlugin(Connection conn, String pluginId) throws DbException {
    final String DEBUG_HEADER = "findPlugin(): ";
    log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
    Long pluginSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findPlugin = dbManager.prepareStatement(conn,
	FIND_PLUGIN_QUERY);

    try {
      findPlugin.setString(1, pluginId);

      resultSet = dbManager.executeQuery(findPlugin);
      if (resultSet.next()) {
	pluginSeq = resultSet.getLong(PLUGIN_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot find plugin", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPlugin);
    }

    log.debug3(DEBUG_HEADER + "pluginSeq = " + pluginSeq);
    return pluginSeq;
  }

  /**
   * Adds a plugin to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginId
   *          A String with the plugin identifier.
   * @param platformSeq
   *          A Long with the publishing platform identifier.
   * @return a Long with the identifier of the plugin just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long addPlugin(Connection conn, String pluginId,
      Long platformSeq) throws DbException {
    final String DEBUG_HEADER = "addPlugin(): ";
    Long pluginSeq = null;
    ResultSet resultSet = null;

    PreparedStatement insertPlugin = dbManager.prepareStatement(conn,
	INSERT_PLUGIN_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertPlugin.setString(1, pluginId);

      if (platformSeq != null) {
	insertPlugin.setLong(2, platformSeq);
      } else {
	insertPlugin.setNull(2, BIGINT);
      }

      dbManager.executeUpdate(insertPlugin);
      resultSet = insertPlugin.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create plugin table row.");
	return null;
      }

      pluginSeq = resultSet.getLong(1);
      log.debug3(DEBUG_HEADER + "Added pluginSeq = " + pluginSeq);
    } catch (SQLException sqle) {
      throw new DbException("Cannot add plugin", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertPlugin);
    }

    return pluginSeq;
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
    Long auSeq = findAu(conn, pluginSeq, auKey);
    log.debug3(DEBUG_HEADER + "auSeq = " + auSeq);

    // Check whether it is a new AU.
    if (auSeq == null) {
      // Yes: Add to the database the new AU.
      auSeq = addAu(conn, pluginSeq, auKey);
      log.debug3(DEBUG_HEADER + "new auSeq = " + auSeq);
    }

    return auSeq;
  }

  /**
   * Provides the identifier of an Archival Unit.
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
  Long findAu(Connection conn, Long pluginSeq, String auKey)
      throws DbException {
    final String DEBUG_HEADER = "findAu(): ";
    ResultSet resultSet = null;
    Long auSeq = null;

    PreparedStatement findAu = dbManager.prepareStatement(conn, FIND_AU_QUERY);

    try {
      findAu.setLong(1, pluginSeq);
      findAu.setString(2, auKey);
      resultSet = dbManager.executeQuery(findAu);
      if (resultSet.next()) {
	auSeq = resultSet.getLong(AU_SEQ_COLUMN);
	log.debug3(DEBUG_HEADER + "Found auSeq = " + auSeq);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot find AU", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAu);
    }

    return auSeq;
  }

  /**
   * Adds an Archival Unit to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginSeq
   *          A Long with the identifier of the plugin.
   * @param auKey
   *          A String with the Archival Unit key.
   * @return a Long with the identifier of the Archival Unit just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long addAu(Connection conn, Long pluginSeq, String auKey)
      throws DbException {
    final String DEBUG_HEADER = "addAu(): ";
    ResultSet resultSet = null;
    Long auSeq = null;

    PreparedStatement insertAu = dbManager.prepareStatement(conn,
	INSERT_AU_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertAu.setLong(1, pluginSeq);
      insertAu.setString(2, auKey);
      dbManager.executeUpdate(insertAu);
      resultSet = insertAu.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create AU table row for AU key " + auKey);
	return null;
      }

      auSeq = resultSet.getLong(1);
      log.debug3(DEBUG_HEADER + "Added auSeq = " + auSeq);
    } catch (SQLException sqle) {
      throw new DbException("Cannot add AU", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertAu);
    }

    return auSeq;
  }

  /**
   * Provides the identifier of an Archival Unit metadata.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auSeq
   *          A Long with the identifier of the Archival Unit.
   * @return a Long with the identifier of the Archival Unit metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findAuMd(Connection conn, Long auSeq)
      throws DbException {
    final String DEBUG_HEADER = "findAuMd(): ";
    Long auMdSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findAuMd = dbManager.prepareStatement(conn,
	FIND_AU_MD_QUERY);

    try {
      findAuMd.setLong(1, auSeq);

      resultSet = dbManager.executeQuery(findAuMd);
      if (resultSet.next()) {
	auMdSeq = resultSet.getLong(AU_MD_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot find AU metadata", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAuMd);
    }

    log.debug3(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
    return auMdSeq;
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
   * @return a Long with the identifier of the Archival Unit metadata just
   *         added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long addAuMd(Connection conn, Long auSeq, int version,
      long extractTime) throws DbException {
    final String DEBUG_HEADER = "addAuMd(): ";
    ResultSet resultSet = null;
    Long auMdSeq = null;

    PreparedStatement insertAuMd = dbManager.prepareStatement(conn,
	INSERT_AU_MD_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertAuMd.setLong(1, auSeq);
      insertAuMd.setShort(2, (short) version);
      insertAuMd.setLong(3, extractTime);
      dbManager.executeUpdate(insertAuMd);
      resultSet = insertAuMd.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create AU_MD table row for auSeq " + auSeq);
	return null;
      }

      auMdSeq = resultSet.getLong(1);
      log.debug3(DEBUG_HEADER + "Added auMdSeq = " + auMdSeq);
    } catch (SQLException sqle) {
      throw new DbException("Cannot add AU metadata", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertAuMd);
    }

    return auMdSeq;
  }

  /**
   * Updates the timestamp of the last extraction of an Archival Unit metadata.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auMdSeq
   *          A Long with the identifier of the Archival Unit metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void updateAuLastExtractionTime(Connection conn, Long auMdSeq)
      throws DbException {
    PreparedStatement updateAuLastExtractionTime =
	dbManager.prepareStatement(conn, UPDATE_AU_MD_EXTRACT_TIME_QUERY);

    try {
      updateAuLastExtractionTime.setLong(1, TimeBase.nowMs());
      updateAuLastExtractionTime.setLong(2, auMdSeq);
      dbManager.executeUpdate(updateAuLastExtractionTime);
    } catch (SQLException sqle) {
      log.error("Cannot update the AU extraction time", sqle);
      log.error("auMdSeq = '" + auMdSeq + "'.");
      log.error("SQL = '" + UPDATE_AU_MD_EXTRACT_TIME_QUERY + "'.");
      throw new DbException("Cannot update the AU extraction time", sqle);
    } finally {
      DbManager.safeCloseStatement(updateAuLastExtractionTime);
    }

    pendingAusCount = getEnabledPendingAusCount(conn);
  }

  /**
   * Provides the identifier of a publisher if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisher
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreatePublisher(Connection conn, String publisher)
      throws DbException {
    final String DEBUG_HEADER = "findOrCreatePublisher(): ";
    Long publisherSeq = findPublisher(conn, publisher);
    log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);

    // Check whether it is a new publisher.
    if (publisherSeq == null) {
      // Yes: Add to the database the new publisher.
      publisherSeq = addPublisher(conn, publisher);
      log.debug3(DEBUG_HEADER + "new publisherSeq = " + publisherSeq);
    }

    return publisherSeq;
  }

  /**
   * Provides the identifier of a publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisher
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findPublisher(Connection conn, String publisher)
      throws DbException {
    final String DEBUG_HEADER = "findPublisher(): ";
    Long publisherSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findPublisher =
	dbManager.prepareStatement(conn, FIND_PUBLISHER_QUERY);

    try {
      findPublisher.setString(1, publisher);

      resultSet = dbManager.executeQuery(findPublisher);
      if (resultSet.next()) {
	publisherSeq = resultSet.getLong(PUBLISHER_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot find publisher", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublisher);
    }

    log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    return publisherSeq;
  }

  /**
   * Adds a publisher to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisher
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long addPublisher(Connection conn, String publisher) throws DbException {
    final String DEBUG_HEADER = "addPublisher(): ";
    Long publisherSeq = null;
    ResultSet resultSet = null;
    PreparedStatement insertPublisher = dbManager.prepareStatement(conn,
	INSERT_PUBLISHER_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertPublisher.setString(1, publisher);
      dbManager.executeUpdate(insertPublisher);
      resultSet = insertPublisher.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create publisher table row.");
	return null;
      }

      publisherSeq = resultSet.getLong(1);
      log.debug3(DEBUG_HEADER + "Added publisherSeq = " + publisherSeq);
    } catch (SQLException sqle) {
      throw new DbException("Cannot add publisher", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertPublisher);
    }

    return publisherSeq;
  }

  /**
   * Provides the identifier of a publication if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pIssn
   *          A String with the print ISSN of the publication.
   * @param eIssn
   *          A String with the electronic ISSN of the publication.
   * @param pIsbn
   *          A String with the print ISBN of the publication.
   * @param eIsbn
   *          A String with the electronic ISBN of the publication.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param name
   *          A String with the name of the publication.
   * @param proprietaryId
   *          A String with the proprietary identifier of the publication.
   * @param volume
   *          A String with the bibliographic volume.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreatePublication(Connection conn, String pIssn,
      String eIssn, String pIsbn, String eIsbn, Long publisherSeq, String name,
      String proprietaryId, String volume) throws DbException {
    final String DEBUG_HEADER = "findOrCreatePublication(): ";
    Long publicationSeq = null;

    // Get the title name.
    String title = null;
    log.debug3(DEBUG_HEADER + "name = " + name);

    if (!StringUtil.isNullString(name)) {
      title = name.substring(0, Math.min(name.length(), MAX_NAME_COLUMN));
    }
    log.debug3(DEBUG_HEADER + "title = " + title);

    // Check whether it is a book series.
    if (isBookSeries(pIssn, eIssn, pIsbn, eIsbn, volume)) {
      // Yes: Find or create the book series.
      log.debug3(DEBUG_HEADER + "is book series.");
      publicationSeq = findOrCreateBookInBookSeries(conn, pIssn, eIssn, pIsbn,
	  eIsbn, publisherSeq, name, proprietaryId, volume);
      // No: Check whether it is a book.
    } else if (isBook(pIsbn, eIsbn)) {
      // Yes: Find or create the book.
      log.debug3(DEBUG_HEADER + "is book.");
      publicationSeq = findOrCreateBook(conn, pIsbn, eIsbn, publisherSeq,
	  title, null, proprietaryId);
    } else {
      // No, it is a journal article: Find or create the journal.
      log.debug3(DEBUG_HEADER + "is journal.");
      publicationSeq = findOrCreateJournal(conn, pIssn, eIssn, publisherSeq,
	  title, proprietaryId);
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
   * @param volume
   *          A String with the volume in the metadata.
   * @return <code>true</code> if the metadata set corresponds to a book series,
   *         <code>false</code> otherwise.
   */
  private boolean isBookSeries(String pIssn, String eIssn, String pIsbn,
      String eIsbn, String volume) {
    final String DEBUG_HEADER = "isBookSeries(): ";

    // If the metadata contains both ISBN and ISSN values, it is a book that is
    // part of a book series.
    boolean isBookSeries =
	isBook(pIsbn, eIsbn)
	    && (!StringUtil.isNullString(pIssn) || !StringUtil
		.isNullString(eIssn));
    log.debug3(DEBUG_HEADER + "isBookSeries = " + isBookSeries);

    // Handle book series with no ISSNs.
    if (!isBookSeries && isBook(pIsbn, eIsbn)) {
      isBookSeries = !StringUtil.isNullString(volume);
      log.debug3(DEBUG_HEADER + "isBookSeries = " + isBookSeries);
    }

    return isBookSeries;
  }

  /**
   * Provides the identifier of a book that belongs to a book series if existing
   * or after creating it otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pIssn
   *          A String with the print ISSN of the book series.
   * @param eIssn
   *          A String with the electronic ISSN of the book series.
   * @param pIsbn
   *          A String with the print ISBN of the book series.
   * @param eIsbn
   *          A String with the electronic ISBN of the book series.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param title
   *          A String with the name of the book series.
   * @param proprietaryId
   *          A String with the proprietary identifier of the book series.
   * @param volume
   *          A String with the bibliographic volume.
   * @return a Long with the identifier of the book.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findOrCreateBookInBookSeries(Connection conn, String pIssn,
      String eIssn, String pIsbn, String eIsbn, Long publisherSeq, String title,
      String proprietaryId, String volume) throws DbException {
    final String DEBUG_HEADER = "findOrCreateBookInBookSeries(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "title = " + title);
      log.debug2(DEBUG_HEADER + "proprietaryId = " + proprietaryId);
      log.debug2(DEBUG_HEADER + "volume = " + volume);
    }

    Long bookSeq = null;
    Long bookSeriesSeq = null;
    Long mdItemSeq = null;

    // Construct a title for the book in the series.
    String bookTitle = title + " Volume " + volume;
    log.debug3(DEBUG_HEADER + "bookTitle = " + bookTitle);

    // Find the book series.
    bookSeriesSeq =
	findPublication(conn, title, publisherSeq, pIssn, eIssn, pIsbn, eIsbn,
			MD_ITEM_TYPE_BOOK_SERIES);
    log.debug3(DEBUG_HEADER + "bookSeriesSeq = " + bookSeriesSeq);

    // Check whether it is a new book series.
    if (bookSeriesSeq == null) {
      // Yes: Add to the database the new book series.
      bookSeriesSeq = addPublication(conn, null, MD_ITEM_TYPE_BOOK_SERIES,
	  title, proprietaryId, publisherSeq);
      log.debug3(DEBUG_HEADER + "new bookSeriesSeq = " + bookSeriesSeq);

      // Skip it if the new book series could not be added.
      if (bookSeriesSeq == null) {
	log.error("Title for new book series '" + title
	    + "' could not be created.");
	return bookSeq;
      }

      // Get the book series metadata item identifier.
      mdItemSeq = findPublicationMetadataItem(conn, bookSeriesSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Add to the database the new book series ISSN values.
      addMdItemIssns(conn, mdItemSeq, pIssn, eIssn);
      log.debug3(DEBUG_HEADER + "added title ISSNs.");

      // Add to the database the new book.
      bookSeq = addPublication(conn, mdItemSeq, MD_ITEM_TYPE_BOOK, bookTitle,
	  proprietaryId, publisherSeq);
      log.debug3(DEBUG_HEADER + "new bookSeq = " + bookSeq);

      // Skip it if the new book could not be added.
      if (bookSeq == null) {
	log.error("Title for new book '" + bookTitle
	    + "' could not be created.");
	return bookSeq;
      }

      // Get the book metadata item identifier.
      mdItemSeq = findPublicationMetadataItem(conn, bookSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Add to the database the new book ISBN values.
      addMdItemIsbns(conn, mdItemSeq, pIsbn, eIsbn);
      log.debug3(DEBUG_HEADER + "added title ISBNs.");
    } else {
      // No: Get the book series metadata item identifier.
      mdItemSeq = findPublicationMetadataItem(conn, bookSeriesSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Add to the database the book series name in the metadata as an
      // alternate, if new.
      addNewMdItemName(conn, mdItemSeq, title);
      log.debug3(DEBUG_HEADER + "added new title name.");

      // Add to the database the ISSN values in the metadata, if new.
      addNewMdItemIssns(conn, mdItemSeq, pIssn, eIssn);
      log.debug3(DEBUG_HEADER + "added new title ISSNs.");

      // Update in the database the proprietary identifier, if not empty.
      updateNonEmptyPublicationId(conn, bookSeriesSeq, proprietaryId);

      // Find or create the book.
      bookSeq = findOrCreateBook(conn, pIsbn, eIsbn, publisherSeq, bookTitle,
	  mdItemSeq, proprietaryId);
    }

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
  private boolean isBook(String pIsbn, String eIsbn) {
    final String DEBUG_HEADER = "isBook(): ";

    // If there are ISBN values in the metadata, it is a book or a book series.
    boolean isBook =
	!StringUtil.isNullString(pIsbn) || !StringUtil.isNullString(eIsbn);
    log.debug3(DEBUG_HEADER + "isBook = " + isBook);

    return isBook;
  }

  /**
   * Provides the identifier of a book existing or after creating it otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pIsbn
   *          A String with the print ISBN of the book.
   * @param eIsbn
   *          A String with the electronic ISBN of the book.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param title
   *          A String with the name of the book.
   * @param parentSeq
   *          A Long with the publication parent publication identifier.
   * @param proprietaryId
   *          A String with the proprietary identifier of the book.
   * @return a Long with the identifier of the book.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findOrCreateBook(Connection conn, String pIsbn, String eIsbn,
      Long publisherSeq, String title, Long parentSeq, String proprietaryId)
      throws DbException {
    final String DEBUG_HEADER = "findOrCreateBook(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "title = " + title);
      log.debug2(DEBUG_HEADER + "parentSeq = " + parentSeq);
      log.debug2(DEBUG_HEADER + "proprietaryId = " + proprietaryId);
    }

    Long publicationSeq = null;
    Long mdItemSeq = null;

    // Find the book.
    publicationSeq =
	findPublication(conn, title, publisherSeq, null, null, pIsbn, eIsbn,
			MD_ITEM_TYPE_BOOK);
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    // Check whether it is a new book.
    if (publicationSeq == null) {
      // Yes: Add to the database the new book.
      publicationSeq = addPublication(conn, parentSeq, MD_ITEM_TYPE_BOOK,
	  title, proprietaryId, publisherSeq);
      log.debug3(DEBUG_HEADER + "new publicationSeq = " + publicationSeq);

      // Skip it if the new book could not be added.
      if (publicationSeq == null) {
	log.error("Publication for new book '" + title
	    + "' could not be created.");
	return publicationSeq;
      }

      // Get the book metadata item identifier.
      mdItemSeq = findPublicationMetadataItem(conn, publicationSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Add to the database the new book ISBN values.
      addMdItemIsbns(conn, mdItemSeq, pIsbn, eIsbn);
      log.debug3(DEBUG_HEADER + "added title ISBNs.");
    } else {
      // No: Get the book metadata item identifier.
      mdItemSeq = findPublicationMetadataItem(conn, publicationSeq);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Add to the database the book name in the metadata as an alternate,
      // if new.
      addNewMdItemName(conn, mdItemSeq, title);
      log.debug3(DEBUG_HEADER + "added new title name.");

      // Add to the database the ISBN values in the metadata, if new.
      addNewMdItemIsbns(conn, mdItemSeq, pIsbn, eIsbn);
      log.debug3(DEBUG_HEADER + "added new title ISBNs.");

      // Update in the database the proprietary identifier, if not empty.
      updateNonEmptyPublicationId(conn, publicationSeq, proprietaryId);
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
   * @param pIssn
   *          A String with the print ISSN of the journal.
   * @param eIssn
   *          A String with the electronic ISSN of the journal.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param title
   *          A String with the name of the journal.
   * @param proprietaryId
   *          A String with the proprietary identifier of the journal.
   * @return a Long with the identifier of the journal.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findOrCreateJournal(Connection conn, String pIssn, String eIssn,
      Long publisherSeq, String title, String proprietaryId)
      throws DbException {
    final String DEBUG_HEADER = "findOrCreateJournal(): ";
    Long publicationSeq = null;
    Long mdItemSeq = null;
    Long parentSeq = null;

    // Skip it if it no title name or ISSNs, as it will not be possible to
    // find the journal to which it belongs in the database.
    if (StringUtil.isNullString(title) && pIssn == null && eIssn == null) {
      log.error("Title for article cannot be created as it has no name or ISSN "
	  + "values.");
      return publicationSeq;
    }

    // Find the journal.
    publicationSeq =
	findPublication(conn, title, publisherSeq, pIssn, eIssn, null, null,
			MD_ITEM_TYPE_JOURNAL);
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    // Check whether it is a new journal.
    if (publicationSeq == null) {
      // Yes: Add to the database the new journal.
      publicationSeq = addPublication(conn, parentSeq, MD_ITEM_TYPE_JOURNAL,
	  title, proprietaryId, publisherSeq);
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
      addMdItemIssns(conn, mdItemSeq, pIssn, eIssn);
      log.debug3(DEBUG_HEADER + "added title ISSNs.");
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

      // Update in the database the proprietary identifier, if not empty.
      updateNonEmptyPublicationId(conn, publicationSeq, proprietaryId);
    }

    return publicationSeq;
  }

  /**
   * Provides the identifier of a publication by its title, publisher, ISSNs
   * and/or ISBNs.
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
  private Long findPublication(Connection conn, String title, Long publisherSeq,
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
      publicationSeq =
	  findPublicationByIssnsOrIsbnsOrName(conn, title, publisherSeq, pIssn,
					      eIssn, pIsbn, eIsbn, mdItemType);
    } else if (hasIssns && hasName) {
      publicationSeq =
	  findPublicationByIssnsOrName(conn, title, publisherSeq, pIssn, eIssn,
				       mdItemType);
    } else if (hasIsbns && hasName) {
      publicationSeq =
	  findPublicationByIsbnsOrName(conn, title, publisherSeq, pIsbn, eIsbn,
				       mdItemType);
    } else if (hasIssns) {
      publicationSeq =
	  findPublicationByIssns(conn, publisherSeq, pIssn, eIssn, mdItemType);
    } else if (hasIsbns) {
      publicationSeq =
	  findPublicationByIsbns(conn, publisherSeq, pIsbn, eIsbn, mdItemType);
    } else if (hasName) {
      publicationSeq =
	  findPublicationByName(conn, title, publisherSeq, mdItemType);
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
   * @param parentSeq
   *          A Long with the publication parent publication identifier.
   * @param mdItemType
   *          A String with the type of publication.
   * @param title
   *          A String with the title of the publication.
   * @param proprietaryId
   *          A String with the proprietary identifier of the publication.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @return a Long with the identifier of the publication just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long addPublication(Connection conn, Long parentSeq,
      String mdItemType, String title, String proprietaryId, Long publisherSeq)
      throws DbException {
    final String DEBUG_HEADER = "addPublication(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "parentSeq = " + parentSeq);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
      log.debug2(DEBUG_HEADER + "title = " + title);
      log.debug2(DEBUG_HEADER + "proprietaryId = " + proprietaryId);
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    }

    Long publicationSeq = null;

    Long mdItemTypeSeq = findMetadataItemType(conn, mdItemType);
    log.debug2(DEBUG_HEADER + "mdItemTypeSeq = " + mdItemTypeSeq);

    if (mdItemTypeSeq == null) {
	log.error("Unable to find the metadata item type " + mdItemType);
	return null;
    }

    Long mdItemSeq =
	addMdItem(conn, parentSeq, mdItemTypeSeq, null, null, null);
    log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    if (mdItemSeq == null) {
	log.error("Unable to create metadata item table row.");
	return null;
    }

    addMdItemName(conn, mdItemSeq, title, PRIMARY_NAME_TYPE);

    ResultSet resultSet = null;

    PreparedStatement insertPublication = dbManager.prepareStatement(conn,
	INSERT_PUBLICATION_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertPublication.setLong(1, mdItemSeq);
      insertPublication.setLong(2, publisherSeq);
      insertPublication.setString(3, proprietaryId);
      dbManager.executeUpdate(insertPublication);
      resultSet = insertPublication.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create publication table row.");
	return null;
      }

      publicationSeq = resultSet.getLong(1);
      log.debug3(DEBUG_HEADER + "Added publicationSeq = " + publicationSeq);
    } catch (SQLException sqle) {
      log.error("Cannot insert publication", sqle);
      log.error("mdItemSeq = '" + mdItemSeq + "'.");
      log.error("SQL = '" + INSERT_PUBLICATION_QUERY + "'.");
      throw new DbException("Cannot insert publication", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertPublication);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
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
    final String DEBUG_HEADER = "findPublicationMetadataItem(): ";
    Long mdItemSeq = null;
    PreparedStatement findMdItem =
	dbManager.prepareStatement(conn, FIND_PUBLICATION_METADATA_ITEM_QUERY);
    ResultSet resultSet = null;

    try {
      findMdItem.setLong(1, publicationSeq);

      resultSet = dbManager.executeQuery(findMdItem);
      if (resultSet.next()) {
	mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
	log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      }
    } catch (SQLException sqle) {
	log.error("Cannot find publication metadata item", sqle);
	log.error("publicationSeq = '" + publicationSeq + "'.");
	log.error("SQL = '" + FIND_PUBLICATION_METADATA_ITEM_QUERY + "'.");
	throw new DbException("Cannot find publication metadata item", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItem);
    }

    return mdItemSeq;
  }

  /**
   * Adds to the database the ISSNs of a metadata item.
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
  private void addMdItemIssns(Connection conn, Long mdItemSeq, String pIssn,
      String eIssn) throws DbException {
    final String DEBUG_HEADER = "addMdItemIssns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
    }

    if (pIssn == null && eIssn == null) {
      return;
    }

    PreparedStatement insertIssn =
	dbManager.prepareStatement(conn, INSERT_ISSN_QUERY);

    try {
      if (pIssn != null) {
	insertIssn.setLong(1, mdItemSeq);
	insertIssn.setString(2, pIssn);
	insertIssn.setString(3, P_ISSN_TYPE);
	int count = dbManager.executeUpdate(insertIssn);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added PISSN = " + pIssn);
	}

	insertIssn.clearParameters();
      }

      if (eIssn != null) {
	insertIssn.setLong(1, mdItemSeq);
	insertIssn.setString(2, eIssn);
	insertIssn.setString(3, E_ISSN_TYPE);
	int count = dbManager.executeUpdate(insertIssn);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added EISSN = " + eIssn);
	}
      }
    } catch (SQLException sqle) {
	log.error("Cannot add metadata item ISSNs", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("pIssn = " + pIssn);
	log.error("eIssn = " + eIssn);
	log.error("SQL = '" + INSERT_ISSN_QUERY + "'.");
	throw new DbException("Cannot add metadata item ISSNs", sqle);
    } finally {
      DbManager.safeCloseStatement(insertIssn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds to the database the ISBNs of a metadata item.
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
  private void addMdItemIsbns(Connection conn, Long mdItemSeq, String pIsbn,
      String eIsbn) throws DbException {
    final String DEBUG_HEADER = "addMdItemIsbns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
    }

    if (pIsbn == null && eIsbn == null) {
      return;
    }

    PreparedStatement insertIsbn =
	dbManager.prepareStatement(conn, INSERT_ISBN_QUERY);

    try {
      if (pIsbn != null) {
	insertIsbn.setLong(1, mdItemSeq);
	insertIsbn.setString(2, pIsbn);
	insertIsbn.setString(3, P_ISBN_TYPE);
	int count = dbManager.executeUpdate(insertIsbn);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added PISBN = " + pIsbn);
	}

	insertIsbn.clearParameters();
      }

      if (eIsbn != null) {
	insertIsbn.setLong(1, mdItemSeq);
	insertIsbn.setString(2, eIsbn);
	insertIsbn.setString(3, E_ISBN_TYPE);
	int count = dbManager.executeUpdate(insertIsbn);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added EISBN = " + eIsbn);
	}
      }
    } catch (SQLException sqle) {
	log.error("Cannot add metadata item ISBNs", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("pIssn = " + pIsbn);
	log.error("eIssn = " + eIsbn);
	log.error("SQL = '" + INSERT_ISBN_QUERY + "'.");
	throw new DbException("Cannot add metadata item ISBNs", sqle);
    } finally {
      DbManager.safeCloseStatement(insertIsbn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
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

    Map<String, String> titleNames = getMdItemNames(conn, mdItemSeq);

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
    Set<Issn> issns = getMdItemIssns(conn, mdItemSeq);

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

    addMdItemIssns(conn, mdItemSeq, pIssn, eIssn);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the ISSNs of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Set<Issn> with the ISSNs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Set<Issn> getMdItemIssns(Connection conn, Long mdItemSeq)
      throws DbException {
    final String DEBUG_HEADER = "getMdItemIssns(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    Set<Issn> issns = new HashSet<Issn>();

    PreparedStatement findIssns =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_ISSN_QUERY);

    ResultSet resultSet = null;
    Issn issn;

    try {
      // Get the metadata item ISSNs.
      findIssns.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(findIssns);

      // Loop through the results.
      while (resultSet.next()) {
	// Get the next ISSN.
	issn = new Issn(resultSet.getString(ISSN_COLUMN),
	    resultSet.getString(ISSN_TYPE_COLUMN));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Found " + issn);

	// Add it to the results.
	issns.add(issn);
      }
    } catch (SQLException sqle) {
	log.error("Cannot find metadata item ISSNs", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("SQL = '" + FIND_MD_ITEM_ISSN_QUERY + "'.");
	throw new DbException("Cannot find metadata item ISSNs", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findIssns);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "issns.size() = " + issns.size());
    return issns;
  }

  /**
   * Updates in the database the publication proprietary identifier, if not
   * empty.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the publication identifier.
   * @param publicationId
   *          A String with the publication proprietary identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void updateNonEmptyPublicationId(Connection conn, Long publicationSeq,
      String publicationId) throws DbException {
    final String DEBUG_HEADER = "updateNonEmptyPublicationId(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      log.debug2(DEBUG_HEADER + "publicationId = " + publicationId);
    }

    if (StringUtil.isNullString(publicationId)) {
      return;
    }

    PreparedStatement updatePublicationId =
	dbManager.prepareStatement(conn, UPDATE_PUBLICATION_ID_QUERY);

    try {
      updatePublicationId.setString(1, publicationId);
      updatePublicationId.setLong(2, publicationSeq);
      int count = dbManager.executeUpdate(updatePublicationId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update the publication proprietary identifier", sqle);
      log.error("publicationSeq = '" + publicationSeq + "'.");
      log.error("publicationId = '" + publicationId + "'.");
      log.error("SQL = '" + UPDATE_PUBLICATION_ID_QUERY + "'.");
      throw new DbException(
	  "Cannot update the publication proprietary identifier", sqle);
    } finally {
      DbManager.safeCloseStatement(updatePublicationId);
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
    Set<Isbn> isbns = getMdItemIsbns(conn, mdItemSeq);

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

    addMdItemIsbns(conn, mdItemSeq, pIsbn, eIsbn);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the ISBNs of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Set<Isbn> with the ISBNs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Set<Isbn> getMdItemIsbns(Connection conn, Long mdItemSeq)
      throws DbException {
    final String DEBUG_HEADER = "getMdItemIsbns(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    Set<Isbn> isbns = new HashSet<Isbn>();

    PreparedStatement findIsbns =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_ISBN_QUERY);

    ResultSet resultSet = null;
    Isbn isbn;

    try {
      // Get the metadata item ISBNs.
      findIsbns.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(findIsbns);

      // Loop through the results.
      while (resultSet.next()) {
	// Get the next ISBN.
	isbn = new Isbn(resultSet.getString(ISBN_COLUMN),
	    resultSet.getString(ISBN_TYPE_COLUMN));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Found " + isbn);

	// Add it to the results.
	isbns.add(isbn);
      }
    } catch (SQLException sqle) {
	log.error("Cannot find metadata item ISBNs", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("SQL = '" + FIND_MD_ITEM_ISBN_QUERY + "'.");
	throw new DbException("Cannot find metadata item ISBNs", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findIsbns);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "isbns.size() = " + isbns.size());
    return isbns;
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
    Long publicationSeq =
	findPublicationByIssns(conn, publisherSeq, pIssn, eIssn, mdItemType);

    if (publicationSeq == null) {
      publicationSeq =
	  findPublicationByIsbnsOrName(conn, title, publisherSeq, pIsbn, eIsbn,
				       mdItemType);
    }

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
  private Long findPublicationByIssnsOrName(Connection conn, String title,
      Long publisherSeq, String pIssn, String eIssn, String mdItemType)
	  throws DbException {
    Long publicationSeq =
	findPublicationByIssns(conn, publisherSeq, pIssn, eIssn, mdItemType);

    if (publicationSeq == null) {
      publicationSeq =
	  findPublicationByName(conn, title, publisherSeq, mdItemType);
    }

    return publicationSeq;
  }

  /**
   * Provides the identifier of a publication by its publisher and title or
   * ISBNs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param title
   *          A String with the title of the publication.
   * @param publisherSeq
   *          A Long with the publisher identifier.
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
  private Long findPublicationByIsbnsOrName(Connection conn, String title,
      Long publisherSeq, String pIsbn, String eIsbn, String mdItemType)
      throws DbException {
    Long publicationSeq =
	findPublicationByIsbns(conn, publisherSeq, pIsbn, eIsbn, mdItemType);

    if (publicationSeq == null) {
      publicationSeq =
	  findPublicationByName(conn, title, publisherSeq, mdItemType);
    }

    return publicationSeq;
  }

  /**
   * Provides the identifier of a publication by its publisher and ISSNs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
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
  private Long findPublicationByIssns(Connection conn, Long publisherSeq,
      String pIssn, String eIssn, String mdItemType) throws DbException {
    final String DEBUG_HEADER = "findPublicationByIssns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
    }

    Long publicationSeq = null;
    ResultSet resultSet = null;
    log.debug3(DEBUG_HEADER + "SQL = '" + FIND_PUBLICATION_BY_ISSNS_QUERY
	+ "'.");
    PreparedStatement findPublicationByIssns =
	dbManager.prepareStatement(conn, FIND_PUBLICATION_BY_ISSNS_QUERY);

    try {
      findPublicationByIssns.setLong(1, publisherSeq);
      findPublicationByIssns.setString(2, pIssn);
      findPublicationByIssns.setString(3, eIssn);
      findPublicationByIssns.setString(4, mdItemType);

      resultSet = dbManager.executeQuery(findPublicationByIssns);
      if (resultSet.next()) {
	publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find publication", sqle);
      log.error("publisherSeq = '" + publisherSeq + "'.");
      log.error("pIssn = " + pIssn);
      log.error("eIssn = " + eIssn);
      log.error("SQL = '" + FIND_PUBLICATION_BY_ISSNS_QUERY + "'.");
      throw new DbException("Cannot find publication", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublicationByIssns);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides the identifier of a publication by its publisher and ISBNs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
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
  private Long findPublicationByIsbns(Connection conn, Long publisherSeq,
      String pIsbn, String eIsbn, String mdItemType) throws DbException {
    final String DEBUG_HEADER = "findPublicationByIsbns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
    }

    Long publicationSeq = null;
    ResultSet resultSet = null;
    log.debug3(DEBUG_HEADER + "SQL = '" + FIND_PUBLICATION_BY_ISBNS_QUERY
	+ "'.");
    PreparedStatement findPublicationByIsbns =
	dbManager.prepareStatement(conn, FIND_PUBLICATION_BY_ISBNS_QUERY);

    try {
      findPublicationByIsbns.setLong(1, publisherSeq);
      findPublicationByIsbns.setString(2, pIsbn);
      findPublicationByIsbns.setString(3, eIsbn);
      findPublicationByIsbns.setString(4, mdItemType);

      resultSet = dbManager.executeQuery(findPublicationByIsbns);
      if (resultSet.next()) {
	publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find publication", sqle);
      log.error("publisherSeq = " + publisherSeq);
      log.error("pIsbn = " + pIsbn);
      log.error("eIsbn = " + eIsbn);
      log.error("mdItemType = " + mdItemType);
      log.error("SQL = '" + FIND_PUBLICATION_BY_ISBNS_QUERY + "'.");
      throw new DbException("Cannot find publication", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublicationByIsbns);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides the identifier of a publication by its title and publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param title
   *          A String with the title of the publication.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param mdItemType
   *          A String with the type of publication to be identified.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findPublicationByName(Connection conn, String title,
      Long publisherSeq, String mdItemType) throws DbException {
    final String DEBUG_HEADER = "findPublicationByName(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "title = " + title);
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
    }

    Long publicationSeq = null;
    ResultSet resultSet = null;
    log.debug3(DEBUG_HEADER + "SQL = '" + FIND_PUBLICATION_BY_NAME_QUERY
    		+ "'.");
    PreparedStatement findPublicationByName =
	dbManager.prepareStatement(conn, FIND_PUBLICATION_BY_NAME_QUERY);

    try {
      findPublicationByName.setLong(1, publisherSeq);
      findPublicationByName.setString(2, title);
      findPublicationByName.setString(3, mdItemType);

      resultSet = dbManager.executeQuery(findPublicationByName);
      if (resultSet.next()) {
	publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find publication", sqle);
      log.error("publisherSeq = '" + publisherSeq + "'.");
      log.error("title = " + title);
      log.error("SQL = '" + FIND_PUBLICATION_BY_NAME_QUERY + "'.");
      throw new DbException("Cannot find publication", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublicationByName);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
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
    final String DEBUG_HEADER = "findMetadataItemType(): ";
    Long mdItemTypeSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findMdItemType =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_TYPE_QUERY);

    try {
      findMdItemType.setString(1, typeName);

      resultSet = dbManager.executeQuery(findMdItemType);
      if (resultSet.next()) {
	mdItemTypeSeq = resultSet.getLong(MD_ITEM_TYPE_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find metadata item type", sqle);
      log.error("typeName = '" + typeName + "'.");
      log.error("SQL = '" + FIND_MD_ITEM_TYPE_QUERY + "'.");
      throw new DbException("Cannot find metadata item type", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItemType);
    }

    log.debug3(DEBUG_HEADER + "mdItemTypeSeq = " + mdItemTypeSeq);
    return mdItemTypeSeq;
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
   * @return a Long with the identifier of the metadata item just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long addMdItem(Connection conn, Long parentSeq,
      Long mdItemTypeSeq, Long auMdSeq, String date,
      String coverage) throws DbException {
    final String DEBUG_HEADER = "addMdItem(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "parentSeq = " + parentSeq);
      log.debug2(DEBUG_HEADER + "mdItemTypeSeq = " + mdItemTypeSeq);
      log.debug2(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
      log.debug2(DEBUG_HEADER + "date = " + date);
      log.debug2(DEBUG_HEADER + "coverage = " + coverage);
    }

    PreparedStatement insertMdItem = dbManager.prepareStatement(conn,
	INSERT_MD_ITEM_QUERY, Statement.RETURN_GENERATED_KEYS);

    ResultSet resultSet = null;
    Long mdItemSeq = null;

    try {
      // skip auto-increment key field #0
      if (parentSeq != null) {
	insertMdItem.setLong(1, parentSeq);
      } else {
	insertMdItem.setNull(1, BIGINT);
      }
      insertMdItem.setLong(2, mdItemTypeSeq);
      if (auMdSeq != null) {
	insertMdItem.setLong(3, auMdSeq);
      } else {
	insertMdItem.setNull(3, BIGINT);
      }
      insertMdItem.setString(4, date);
      insertMdItem.setString(5, coverage);
      dbManager.executeUpdate(insertMdItem);
      resultSet = insertMdItem.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create metadata item table row.");
	return null;
      }

      mdItemSeq = resultSet.getLong(1);
      log.debug3(DEBUG_HEADER + "Added mdItemSeq = " + mdItemSeq);
    } catch (SQLException sqle) {
      log.error("Cannot insert metadata item", sqle);
      log.error("SQL = '" + INSERT_MD_ITEM_QUERY + "'.");
      log.error("parentSeq = " + parentSeq + ".");
      log.error("mdItemTypeSeq = " + mdItemTypeSeq + ".");
      log.error("auMdSeq = " + auMdSeq + ".");
      log.error("date = '" + date + "'.");
      log.error("coverage = '" + coverage + "'.");
      throw new DbException("Cannot insert metadata item", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertMdItem);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
    return mdItemSeq;
  }

  /**
   * Provides the names of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Map<String, String> with the names and name types of the metadata
   *         item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Map<String, String> getMdItemNames(Connection conn, Long mdItemSeq)
      throws DbException {
    final String DEBUG_HEADER = "getMdItemNames(): ";
    Map<String, String> names = new HashMap<String, String>();
    PreparedStatement getNames =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_NAME_QUERY);
    ResultSet resultSet = null;

    try {
      getNames.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(getNames);
      while (resultSet.next()) {
	names.put(resultSet.getString(NAME_COLUMN),
		  resultSet.getString(NAME_TYPE_COLUMN));
	log.debug3(DEBUG_HEADER + "Found metadata item name = '"
	    + resultSet.getString(NAME_COLUMN) + "' of type '"
	    + resultSet.getString(NAME_TYPE_COLUMN) + "'.");
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot get the names of a metadata item", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(getNames);
    }

    return names;
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
    final String DEBUG_HEADER = "addMdItemName(): ";

    if (name == null || type == null) {
      return;
    }

    PreparedStatement insertMdItemName =
	dbManager.prepareStatement(conn, INSERT_MD_ITEM_NAME_QUERY);

    try {
      insertMdItemName.setLong(1, mdItemSeq);
      insertMdItemName.setString(2, name);
      insertMdItemName.setString(3, type);
      int count = dbManager.executeUpdate(insertMdItemName);

      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "count = " + count);
	log.debug3(DEBUG_HEADER + "Added metadata item name = " + name);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot add a metadata item name", sqle);
    } finally {
      DbManager.safeCloseStatement(insertMdItemName);
    }
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
    final String DEBUG_HEADER = "addMdItemUrl(): ";
    PreparedStatement insertMdItemUrl =
	dbManager.prepareStatement(conn, INSERT_URL_QUERY);

    try {
      insertMdItemUrl.setLong(1, mdItemSeq);
      insertMdItemUrl.setString(2, feature);
      insertMdItemUrl.setString(3, url);
      int count = dbManager.executeUpdate(insertMdItemUrl);

      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "count = " + count);
	log.debug3(DEBUG_HEADER + "Added URL = " + url);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot add a metadata item URL", sqle);
    } finally {
      DbManager.safeCloseStatement(insertMdItemUrl);
    }
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
    final String DEBUG_HEADER = "addMdItemDoi(): ";

    if (StringUtil.isNullString(doi)) {
      return;
    }

    PreparedStatement insertMdItemDoi =
	dbManager.prepareStatement(conn, INSERT_DOI_QUERY);

    try {
      insertMdItemDoi.setLong(1, mdItemSeq);
      insertMdItemDoi.setString(2, doi);
      int count = dbManager.executeUpdate(insertMdItemDoi);

      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "count = " + count);
	log.debug3(DEBUG_HEADER + "Added DOI = " + doi);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot add a metadata item DOI", sqle);
    } finally {
      DbManager.safeCloseStatement(insertMdItemDoi);
    }
  }

  /**
   * Adds an AU to the list of AUs to be reindexed. Does incremental reindexing
   * if possible.
   * 
   * @param au
   *          An ArchivalUnit with the AU to be reindexed.
   * @return <code>true</code> if au was added for reindexing
   */
  /*public boolean addAuToReindex(ArchivalUnit au) {
    Connection conn = null;
    PreparedStatement insertPendingAuBatchStatement = null;

    try {
      conn = dbManager.getConnection();

      if (conn == null) {
	log.error("Cannot connect to database"
	    + " -- cannot add aus to pending aus" + au.getName());
	return false;
      }

      insertPendingAuBatchStatement =
	  getInsertPendingAuBatchStatement(conn);

      return addAuToReindex(au, conn,
	  insertPendingAuBatchStatement, false);
    } catch (SQLException ex) {
      log.error("Cannot add au to pending AUs: " + au.getName(), ex);
      return false;
    } finally {
      DbManager.safeCloseStatement(insertPendingAuBatchStatement);
      DbManager.safeRollbackAndClose(conn);
    }
  }*/

  /**
   * Adds an AU to the list of AUs to be reindexed.
   * Does incremental reindexing if possible.
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
    return enableAndAddAuToReindex(au, conn, insertPendingAuBatchStatement,
	inBatch, false);
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
   *          Causes a full reindex by removing that AU from the database. 
   * @return <code>true</code> if au was added for reindexing
   */
  public boolean enableAndAddAuToReindex(ArchivalUnit au, Connection conn,
      PreparedStatement insertPendingAuBatchStatement, boolean inBatch,
      boolean fullReindex) {
    final String DEBUG_HEADER = "addAuToReindex(): ";

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
              insertPendingAuBatchStatement, inBatch);
        }

        // force a full reindex by removing the AU metadata from the database
        if (fullReindex) {
          removeAu(conn, au.getAuId());
        }
        
        startReindexing(conn);
        DbManager.commitOrRollback(conn, log);

        // once transaction has committed, resync AU count with database
        if (fullReindex) {
          metadataArticleCount = getArticleCount(conn);
        }
        
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
    PreparedStatement deletePendingAu =
	dbManager.prepareStatement(conn, DELETE_DISABLED_PENDING_AU_QUERY);

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      String auKey = PluginManager.auKeyFromAuId(auId);
  
      deletePendingAu.setString(1, pluginId);
      deletePendingAu.setString(2, auKey);
      dbManager.executeUpdate(deletePendingAu);
    } catch (SQLException sqle) {
      log.error("Cannot remove disabled AU from pending table", sqle);
      log.error("auId = '" + auId + "'.");
      log.error("SQL = '" + DELETE_DISABLED_PENDING_AU_QUERY + "'.");
      throw new DbException("Cannot remove disabled AU from pending table",
	  sqle);
    } finally {
      DbManager.safeCloseStatement(deletePendingAu);
    }

    pendingAusCount = getEnabledPendingAusCount(conn);
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
   * @return <code>true</code> if au was added for reindexing,
   *         <code>false</code> otherwise.
   */
  public boolean disableAuIndexing(ArchivalUnit au) {
    final String DEBUG_HEADER = "disableAuIndexing(): ";

    synchronized (activeReindexingTasks) {
      Connection conn = null;

      try {
        log.debug2(DEBUG_HEADER + "Disabing indexing for AU " + au.getName());
        conn = dbManager.getConnection();

        if (conn == null) {
          log.error("Cannot connect to database"
              + " -- cannot disable indexing for AU");
          return false;
        }

        String auId = au.getAuId();
        log.debug2(DEBUG_HEADER + "auId " + auId);

        if (activeReindexingTasks.containsKey(auId)) {
          ReindexingTask task = activeReindexingTasks.get(auId);
          task.cancel();
          activeReindexingTasks.remove(task);
        }

        // Remove the AU from the list of pending AUs if it is there.
        removeFromPendingAus(conn, auId);

        // Add it marked as disabled.
        addDisabledAuToPendingAus(conn, auId);
        DbManager.commitOrRollback(conn, log);

        return true;
      } catch (DbException dbe) {
        log.error("Cannot disable AU: " + au.getName(), dbe);
        return false;
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
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addToPendingAusIfNotThere(Connection conn, Collection<ArchivalUnit> aus)
      throws DbException {
    PreparedStatement insertPendingAuBatchStatement = null;

    try {
      insertPendingAuBatchStatement = getInsertPendingAuBatchStatement(conn);
      addToPendingAusIfNotThere(conn, aus, insertPendingAuBatchStatement,
	  false);
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
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addToPendingAusIfNotThere(Connection conn, Collection<ArchivalUnit> aus,
      PreparedStatement insertPendingAuBatchStatement, boolean inBatch)
      throws DbException {
    final String DEBUG_HEADER = "addToPendingAus(): ";
    PreparedStatement selectPendingAu =
	dbManager.prepareStatement(conn, FIND_PENDING_AU_QUERY);

    ResultSet results = null;
    log.debug2(DEBUG_HEADER + "maxPendingAuBatchSize = " + maxPendingAuBatchSize);
    log.debug2(DEBUG_HEADER + "Number of pending aus to add: " + aus.size());

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

          // Find the AU in the table.
          selectPendingAu.setString(1, pluginId);
          selectPendingAu.setString(2, auKey);
          results = dbManager.executeQuery(selectPendingAu);

          if (!results.next()) {
            // Only insert if entry does not exist.
	    log.debug3(DEBUG_HEADER + "Adding au " + au.getName()
		+ " to pending list");
            insertPendingAuBatchStatement.setString(1, pluginId);
            insertPendingAuBatchStatement.setString(2, auKey);
            insertPendingAuBatchStatement.addBatch();
            pendingAuBatchCurrentSize++;
	    log.debug3(DEBUG_HEADER + "pendingAuBatchCurrentSize = "
		+ pendingAuBatchCurrentSize);

	    // Check whether the maximum batch size has been reached.
	    if (pendingAuBatchCurrentSize >= maxPendingAuBatchSize) {
	      // Yes: Perform the insertion of all the AUs in the batch.
	      log.debug3(DEBUG_HEADER + "Executing batch...");
	      insertPendingAuBatchStatement.executeBatch();
	      pendingAuBatchCurrentSize = 0;
	      log.debug3(DEBUG_HEADER + "pendingAuBatchCurrentSize = "
		  + pendingAuBatchCurrentSize);
	    }
          } else {
            log.debug3(DEBUG_HEADER+ "Not adding au " + au.getName()
                       + " to pending list because it is already on the list");
          }

          DbManager.safeCloseResultSet(results);
	}
      }

      // Check whether there are no more AUs to be batched and the batch is not
      // empty.
      if (!inBatch && pendingAuBatchCurrentSize > 0) {
	// Yes: Perform the insertion of all the AUs in the batch.
	log.debug3(DEBUG_HEADER + "Executing batch...");
	insertPendingAuBatchStatement.executeBatch();
	pendingAuBatchCurrentSize = 0;
	log.debug3(DEBUG_HEADER + "pendingAuBatchCurrentSize = "
	    + pendingAuBatchCurrentSize);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot add pending AUs", sqle);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(selectPendingAu);
    }

    pendingAusCount = getEnabledPendingAusCount(conn);
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
        log.error("Cannot remove au to pending AUs: " + au.getName(), dbe);
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
    int auVersion = getAuMetadataVersion(au);
    log.debug(DEBUG_HEADER + "auVersion = " + auVersion);

    // Get the current version of the plugin. 
    int pVersion = getPluginMetadataVersionNumber(au.getPlugin());
    log.debug(DEBUG_HEADER + "pVersion = " + pVersion);

    return pVersion > auVersion;
  }

  /**
   * Provides the version of the metadata of an AU stored in the database.
   * 
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @return an int with the version of the metadata of the AU stored in the
   *         database.
   */
  private int getAuMetadataVersion(ArchivalUnit au) {
    int version = UNKNOWN_VERSION;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Get the version.
      version = getAuMetadataVersion(conn, au);
    } catch (DbException dbe) {
      log.error("Cannot get AU metadata version - Using " + version + ": "
	  + dbe);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    return version;
  }

  /**
   * Provides the version of the metadata of an AU stored in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @return an int with the version of the metadata of the AU stored in the
   *         database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int getAuMetadataVersion(Connection conn, ArchivalUnit au)
      throws DbException {
    final String DEBUG_HEADER = "getAuMetadataVersion(): ";
    int version = UNKNOWN_VERSION;
    PreparedStatement selectMetadataVersion = null;
    ResultSet resultSet = null;

    try {
      String auId = au.getAuId();
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      log.debug2(DEBUG_HEADER + "pluginId() = " + pluginId);

      String auKey = PluginManager.auKeyFromAuId(auId);
      log.debug2(DEBUG_HEADER + "auKey = " + auKey);

      selectMetadataVersion =
	  dbManager.prepareStatement(conn, FIND_AU_METADATA_VERSION_QUERY);
      selectMetadataVersion.setString(1, pluginId);
      selectMetadataVersion.setString(2, auKey);
      resultSet = dbManager.executeQuery(selectMetadataVersion);

      if (resultSet.next()) {
	version = resultSet.getShort(MD_VERSION_COLUMN);
	log.debug2(DEBUG_HEADER + "version = " + version);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot get AU metadata version", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(selectMetadataVersion);
    }

    return version;
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
    int auVersion = getAuMetadataVersion(conn, au);
    log.debug2(DEBUG_HEADER + "auVersion = " + auVersion);

    // Get the current version of the plugin. 
    int pVersion = getPluginMetadataVersionNumber(au.getPlugin());
    log.debug2(DEBUG_HEADER + "pVersion = " + pVersion);

    return pVersion > auVersion;
  }

  /**
   * Provides the extraction time of an Archival Unit metadata.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auSeq
   *          A Long with the identifier of the Archival Unit.
   * @return a long with the extraction time of the Archival Unit metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getAuExtractionTime(Connection conn, Long auSeq)
	throws DbException {
    final String DEBUG_HEADER = "getAuExtractionTime(): ";
    long timestamp = NEVER_EXTRACTED_EXTRACTION_TIME;
    PreparedStatement selectLastExtractionTime = null;
    ResultSet resultSet = null;

    try {
      selectLastExtractionTime = dbManager.prepareStatement(conn,
	  FIND_AU_MD_EXTRACT_TIME_BY_AUSEQ_QUERY);
      selectLastExtractionTime.setLong(1, auSeq);
      resultSet = dbManager.executeQuery(selectLastExtractionTime);

      if (resultSet.next()) {
	timestamp = resultSet.getLong(EXTRACT_TIME_COLUMN);
	log.debug2(DEBUG_HEADER + "timestamp = " + timestamp);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot get AU extraction time", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(selectLastExtractionTime);
    }

    return timestamp;
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

    long lastExtractionTime = getAuExtractionTime(conn, au);
    log.debug2(DEBUG_HEADER + "lastExtractionTime = " + lastExtractionTime);

    return lastCrawlTime > lastExtractionTime;
  }

  /**
   * Provides the extraction time of an Archival Unit metadata.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @return a long with the extraction time of the Archival Unit metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private long getAuExtractionTime(Connection conn, ArchivalUnit au)
	throws DbException {
    final String DEBUG_HEADER = "getAuExtractionTime(): ";
    long timestamp = NEVER_EXTRACTED_EXTRACTION_TIME;
    PreparedStatement selectLastExtractionTime = null;
    ResultSet resultSet = null;

    try {
      String auId = au.getAuId();
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      log.debug2(DEBUG_HEADER + "pluginId() = " + pluginId);

      String auKey = PluginManager.auKeyFromAuId(auId);
      log.debug2(DEBUG_HEADER + "auKey = " + auKey);

      selectLastExtractionTime =
	  dbManager.prepareStatement(conn, FIND_AU_MD_EXTRACT_TIME_BY_AU_QUERY);
      selectLastExtractionTime.setString(1, pluginId);
      selectLastExtractionTime.setString(2, auKey);
      resultSet = dbManager.executeQuery(selectLastExtractionTime);

      if (resultSet.next()) {
	timestamp = resultSet.getLong(EXTRACT_TIME_COLUMN);
	log.debug2(DEBUG_HEADER + "timestamp = " + timestamp);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot get AU extraction time", sqle);
    } finally {
	DbManager.safeCloseResultSet(resultSet);
	DbManager.safeCloseStatement(selectLastExtractionTime);
    }

    return timestamp;
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
      platformSeq = addPlatform(conn, platformName);
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
    final String DEBUG_HEADER = "findPlatform(): ";
    log.debug3(DEBUG_HEADER + "platformName = " + platformName);
    Long platformSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findPlatform =
	dbManager.prepareStatement(conn, FIND_PLATFORM_QUERY);

    try {
      findPlatform.setString(1, platformName);

      resultSet = dbManager.executeQuery(findPlatform);
      if (resultSet.next()) {
	platformSeq = resultSet.getLong(PLATFORM_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot find platform", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPlatform);
    }

    log.debug3(DEBUG_HEADER + "platformSeq = " + platformSeq);
    return platformSeq;
  }

  /**
   * Adds a platform to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param platformName
   *          A String with the platform name.
   * @return a Long with the identifier of the platform just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long addPlatform(Connection conn, String platformName)
      throws DbException {
    final String DEBUG_HEADER = "addPlatform(): ";
    Long platformSeq = null;
    ResultSet resultSet = null;
    PreparedStatement insertPlatform = dbManager.prepareStatement(conn,
	INSERT_PLATFORM_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // Skip auto-increment key field #0
      insertPlatform.setString(1, platformName);
      dbManager.executeUpdate(insertPlatform);
      resultSet = insertPlatform.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create platform table row.");
	return null;
      }

      platformSeq = resultSet.getLong(1);
      log.debug3(DEBUG_HEADER + "Added platformSeq = " + platformSeq);
    } catch (SQLException sqle) {
      throw new DbException("Cannot add platform", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertPlatform);
    }

    return platformSeq;
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
    return dbManager.prepareStatement(conn, INSERT_ENABLED_PENDING_AU_QUERY);
  }

  /**
   * Provides the identifier of a publication if existing, null otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pIssn
   *          A String with the print ISSN of the publication.
   * @param eIssn
   *          A String with the electronic ISSN of the publication.
   * @param pIsbn
   *          A String with the print ISBN of the publication.
   * @param eIsbn
   *          A String with the electronic ISBN of the publication.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param name
   *          A String with the name of the publication.
   * @param volume
   *          A String with the bibliographic volume.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findPublication(Connection conn, String pIssn, String eIssn,
      String pIsbn, String eIsbn, Long publisherSeq, String name, String volume)
	  throws DbException {
    final String DEBUG_HEADER = "findPublication(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "name = " + name);
      log.debug2(DEBUG_HEADER + "volume = " + volume);
    }

    Long publicationSeq = null;

    // Get the title name.
    String title = null;
    log.debug3(DEBUG_HEADER + "name = " + name);

    if (!StringUtil.isNullString(name)) {
      title = name.substring(0, Math.min(name.length(), MAX_NAME_COLUMN));
    }
    log.debug3(DEBUG_HEADER + "title = " + title);

    // Check whether it is a book series.
    if (isBookSeries(pIssn, eIssn, pIsbn, eIsbn, volume)) {
      // Yes: Find the book series.
      log.debug3(DEBUG_HEADER + "is book series.");
      publicationSeq = findBookInBookSeries(conn, pIssn, eIssn, pIsbn,
	  eIsbn, publisherSeq, name, volume);
      // No: Check whether it is a book.
    } else if (isBook(pIsbn, eIsbn)) {
      // Yes: Find the book.
      log.debug3(DEBUG_HEADER + "is book.");
      publicationSeq = findBook(conn, pIsbn, eIsbn, publisherSeq, title);
    } else {
      // No, it is a journal article: Find the journal.
      log.debug3(DEBUG_HEADER + "is journal.");
      publicationSeq = findJournal(conn, pIssn, eIssn, publisherSeq, title);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides the identifier of an existing book that belongs to a book series,
   * null otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pIssn
   *          A String with the print ISSN of the book series.
   * @param eIssn
   *          A String with the electronic ISSN of the book series.
   * @param pIsbn
   *          A String with the print ISBN of the book series.
   * @param eIsbn
   *          A String with the electronic ISBN of the book series.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param title
   *          A String with the name of the book series.
   * @param volume
   *          A String with the bibliographic volume.
   * @return a Long with the identifier of the book.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findBookInBookSeries(Connection conn, String pIssn, String eIssn,
      String pIsbn, String eIsbn, Long publisherSeq, String title,
      String volume) throws DbException {
    final String DEBUG_HEADER = "findBookInBookSeries(): ";
    Long bookSeq = null;

    // Construct a title for the book in the series.
    String bookTitle = title + " Volume " + volume;
    log.debug3(DEBUG_HEADER + "bookTitle = " + bookTitle);

    // Find the book series.
    Long bookSeriesSeq =
	findPublication(conn, title, publisherSeq, pIssn, eIssn, pIsbn, eIsbn,
			MD_ITEM_TYPE_BOOK_SERIES);
    log.debug3(DEBUG_HEADER + "bookSeriesSeq = " + bookSeriesSeq);

    // Check whether it is an existing book series.
    if (bookSeriesSeq != null) {
      // Yes: Find the book.
      bookSeq = findBook(conn, pIsbn, eIsbn, publisherSeq, bookTitle);
    }

    return bookSeq;
  }

  /**
   * Provides the identifier of an existing book, null otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pIsbn
   *          A String with the print ISBN of the book.
   * @param eIsbn
   *          A String with the electronic ISBN of the book.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param title
   *          A String with the name of the book.
   * @return a Long with the identifier of the book.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findBook(Connection conn, String pIsbn, String eIsbn,
      Long publisherSeq, String title) throws DbException {
    final String DEBUG_HEADER = "findBook(): ";

    // Find the book.
    Long publicationSeq =
	findPublication(conn, title, publisherSeq, null, null, pIsbn, eIsbn,
			MD_ITEM_TYPE_BOOK);
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    return publicationSeq;
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
  private Long findJournal(Connection conn, String pIssn, String eIssn,
      Long publisherSeq, String title) throws DbException {
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
	findPublication(conn, title, publisherSeq, pIssn, eIssn, null, null,
			MD_ITEM_TYPE_JOURNAL);
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    return publicationSeq;
  }

  /**
   * Adds a disabled AU to the list of pending AUs to reindex.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addDisabledAuToPendingAus(Connection conn, String auId)
      throws DbException {
    final String DEBUG_HEADER = "addDisabledAuToPendingAus(): ";
    PreparedStatement addPendingAuStatement =
	dbManager.prepareStatement(conn, INSERT_DISABLED_PENDING_AU_QUERY);

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      String auKey = PluginManager.auKeyFromAuId(auId);
      log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      addPendingAuStatement.setString(1, pluginId);
      addPendingAuStatement.setString(2, auKey);
      int count = dbManager.executeUpdate(addPendingAuStatement);
      log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      throw new DbException("Cannot add disabled pending AU", sqle);
    } finally {
      DbManager.safeCloseStatement(addPendingAuStatement);
    }
  }

  /**
   * Adds an AU with failed indexing to the list of pending AUs to reindex.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addFailedIndexingAuToPendingAus(Connection conn, String auId)
      throws DbException {
    final String DEBUG_HEADER = "addFailedIndexingAuToPendingAus(): ";
    PreparedStatement addPendingAuStatement =
	dbManager.prepareStatement(conn,
	    INSERT_FAILED_INDEXING_PENDING_AU_QUERY);

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      String auKey = PluginManager.auKeyFromAuId(auId);
      log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      addPendingAuStatement.setString(1, pluginId);
      addPendingAuStatement.setString(2, auKey);
      int count = dbManager.executeUpdate(addPendingAuStatement);
      log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      throw new DbException("Cannot add failed pending AU", sqle);
    } finally {
      DbManager.safeCloseStatement(addPendingAuStatement);
    }
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
    return findPendingAusWithPriority(conn, MIN_INDEX_PRIORITY);
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
    return findPendingAusWithPriority(conn, FAILED_INDEX_PRIORITY);
  }

  /**
   * Provides the identifiers of pending Archival Units with a given priority.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param priority
   *          An int with the priority of the requested Archival Units.
   * @return a Collection<String> with the identifiers of pending Archival Units
   *         with the given priority.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Collection<String> findPendingAusWithPriority(Connection conn,
      int priority) throws DbException {
    final String DEBUG_HEADER = "findPendingAusWithPriority(): ";
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "priority = " + priority);

    Collection<String> aus = new ArrayList<String>();
    String pluginId;
    String auKey;
    String auId;
    ResultSet results = null;

    PreparedStatement selectAus =
	dbManager.prepareStatement(conn, FIND_PENDING_AUS_WITH_PRIORITY_QUERY);

    try {
      selectAus.setInt(1, priority);
      results = dbManager.executeQuery(selectAus);

      while (results.next()) {
	pluginId = results.getString(PLUGIN_ID_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
	auKey = results.getString(AU_KEY_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);
	auId = PluginManager.generateAuId(pluginId, auKey);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	aus.add(auId);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot find pending AUs", sqle);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(selectAus);
    }

    return aus;
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
    final String DEBUG_HEADER = "findAuPublisher(): ";
    Long publisherSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findPublisher =
	dbManager.prepareStatement(conn, FIND_AU_PUBLISHER_QUERY);

    try {
      findPublisher.setLong(1, auSeq);

      resultSet = dbManager.executeQuery(findPublisher);
      if (resultSet.next()) {
	publisherSeq = resultSet.getLong(PUBLISHER_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find the publisher of an AU", sqle);
      log.error("auSeq = '" + auSeq + "'.");
      log.error("SQL = '" + FIND_AU_PUBLISHER_QUERY + "'.");
      throw new DbException("Cannot find the publisher of an AU", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublisher);
    }

    log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    return publisherSeq;
  }

  /**
   * Provides the name of a publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the identifier of the publisher.
   * @return a String with the name of the publisher.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  String getPublisherName(Connection conn, Long publisherSeq)
      throws DbException {
    final String DEBUG_HEADER = "getPublisherName(): ";
    String publisherName = null;
    ResultSet resultSet = null;

    PreparedStatement getPublisherNameStatement =
	dbManager.prepareStatement(conn, GET_PUBLISHER_NAME_QUERY);

    try {
      getPublisherNameStatement.setLong(1, publisherSeq);

      resultSet = dbManager.executeQuery(getPublisherNameStatement);
      if (resultSet.next()) {
	publisherName = resultSet.getString(PUBLISHER_NAME_COLUMN);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the name of a publisher", sqle);
      log.error("publisherSeq = '" + publisherSeq + "'.");
      log.error("SQL = '" + GET_PUBLISHER_NAME_QUERY + "'.");
      throw new DbException("Cannot get the name of a publisher", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(getPublisherNameStatement);
    }

    log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);
    return publisherName;
  }

  /**
   * Provides the problems found indexing an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a List<String> with the problems found indexing the Archival Unit.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  List<String> findAuProblems(Connection conn, String auId)
      throws DbException {
    final String DEBUG_HEADER = "findAuProblems(): ";
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

    List<String> problems = new ArrayList<String>();
    ResultSet results = null;
    String problem;

    PreparedStatement findProblems =
	dbManager.prepareStatement(conn, FIND_AU_PROBLEMS_QUERY);

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      String auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      findProblems.setString(1, pluginId);
      findProblems.setString(2, auKey);
      results = dbManager.executeQuery(findProblems);

      while (results.next()) {
	problem = results.getString(PROBLEM_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "problem = " + problem);

	problems.add(problem);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find AU problems", sqle);
      log.error("auId = '" + auId + "'.");
      log.error("SQL = '" + FIND_AU_PROBLEMS_QUERY + "'.");
      throw new DbException("Cannot find AU problems", sqle);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(findProblems);
    }

    return problems;
  }

  /**
   * Adds an entry to the table of AU problems.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param problem
   *          A String with the problem.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addAuProblem(Connection conn, String auId, String problem)
      throws DbException {
    final String DEBUG_HEADER = "addAuProblem(): ";
    PreparedStatement addAuProblemStatement =
	dbManager.prepareStatement(conn, INSERT_AU_PROBLEM_QUERY);

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      String auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      addAuProblemStatement.setString(1, pluginId);
      addAuProblemStatement.setString(2, auKey);
      addAuProblemStatement.setString(3, problem);
      int count = dbManager.executeUpdate(addAuProblemStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot add problem AU entry", sqle);
      log.error("auId = '" + auId + "'.");
      log.error("problem = '" + problem + "'.");
      log.error("SQL = '" + INSERT_AU_PROBLEM_QUERY + "'.");
      throw new DbException("Cannot add problem AU entry", sqle);
    } finally {
      DbManager.safeCloseStatement(addAuProblemStatement);
    }
  }

  /**
   * Removes from the table of AU problems all entries for a given AU.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archiva lUnit identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void removeAuProblems(Connection conn, String auId)throws DbException {
    final String DEBUG_HEADER = "removeAuProblems(): ";
    PreparedStatement deleteAuProblems =
	dbManager.prepareStatement(conn, DELETE_AU_PROBLEMS_QUERY);

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      String auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      deleteAuProblems.setString(1, pluginId);
      deleteAuProblems.setString(2, auKey);
      int count = dbManager.executeUpdate(deleteAuProblems);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot remove problem AU entries", sqle);
      log.error("auId = '" + auId + "'.");
      log.error("SQL = '" + DELETE_AU_PROBLEMS_QUERY + "'.");
      throw new DbException("Cannot remove problem AU entries", sqle);
    } finally {
      DbManager.safeCloseStatement(deleteAuProblems);
    }
  }

  /**
   * Removes an entry from the table of AU problems.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archiva lUnit identifier.
   * @param problem
   *          A String with the problem.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void removeAuProblem(Connection conn, String auId, String problem)
      throws DbException {
    final String DEBUG_HEADER = "removeAuProblem(): ";
    PreparedStatement deleteAuProblem =
	dbManager.prepareStatement(conn, DELETE_AU_PROBLEM_QUERY);

    try {
      String pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      String auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      deleteAuProblem.setString(1, pluginId);
      deleteAuProblem.setString(2, auKey);
      deleteAuProblem.setString(3, problem);
      int count = dbManager.executeUpdate(deleteAuProblem);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot remove problem AU entry", sqle);
      log.error("auId = '" + auId + "'.");
      log.error("problem = '" + problem + "'.");
      log.error("SQL = '" + DELETE_AU_PROBLEM_QUERY + "'.");
      throw new DbException("Cannot remove problem AU entry", sqle);
    } finally {
      DbManager.safeCloseStatement(deleteAuProblem);
    }
  }

  /**
   * Provides the identifiers of the publications of a publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the identifier of the publisher.
   * @return a Set<Long> with the identifiers of the publications.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Set<Long> findPublisherPublications(Connection conn, Long publisherSeq)
      throws DbException {
    final String DEBUG_HEADER = "findPublisherPublications(): ";
    Set<Long> publicationSeqs = new HashSet<Long>();
    Long publicationSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findPublications =
	dbManager.prepareStatement(conn, FIND_PUBLISHER_PUBLICATIONS_QUERY);

    try {
      findPublications.setLong(1, publisherSeq);

      resultSet = dbManager.executeQuery(findPublications);
      while (resultSet.next()) {
	publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

	publicationSeqs.add(publicationSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find the publications of a publisher", sqle);
      log.error("publisherSeq = '" + publisherSeq + "'.");
      log.error("SQL = '" + FIND_PUBLISHER_PUBLICATIONS_QUERY + "'.");
      throw new DbException("Cannot find the publications of a publisher",
	  sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublications);
    }

    return publicationSeqs;
  }

  /**
   * Provides the identifiers of the child metadata items (chapters, articles)
   * of a publication.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the identifier of the publication.
   * @return a Set<Long> with the identifiers of the metadata items.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Set<Long> findPublicationChildMetadataItems(Connection conn,
      Long publicationSeq) throws DbException {
    final String DEBUG_HEADER = "findPublicationChildMetadataItems(): ";
    Set<Long> mdItemSeqs = new HashSet<Long>();
    Long mdItemSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findMdItems =
	dbManager.prepareStatement(conn, FIND_PUBLICATION_CHILD_MD_ITEMS_QUERY);

    try {
      findMdItems.setLong(1, publicationSeq);

      resultSet = dbManager.executeQuery(findMdItems);
      while (resultSet.next()) {
	mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
	log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

	mdItemSeqs.add(mdItemSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find the child metadata items of a publication", sqle);
      log.error("publicationSeq = '" + publicationSeq + "'.");
      log.error("SQL = '" + FIND_PUBLICATION_CHILD_MD_ITEMS_QUERY + "'.");
      throw new DbException(
	  "Cannot find the child metadata items of a publication", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItems);
    }

    return mdItemSeqs;
  }

  /**
   * Provides the identifiers of the Archival Units of a publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the identifier of the publisher.
   * @return a Set<Long> with the identifiers of the Archival Units.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Set<Long> findPublisherAus(Connection conn, Long publisherSeq)
      throws DbException {
    final String DEBUG_HEADER = "findPublisherAus(): ";
    Set<Long> auSeqs = new HashSet<Long>();
    Long auSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findAus =
	dbManager.prepareStatement(conn, FIND_PUBLISHER_AUS_QUERY);

    try {
      findAus.setLong(1, publisherSeq);

      resultSet = dbManager.executeQuery(findAus);
      while (resultSet.next()) {
	auSeq = resultSet.getLong(AU_SEQ_COLUMN);
	log.debug3(DEBUG_HEADER + "auSeq = " + auSeq);

	auSeqs.add(auSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find the AUs of a publisher", sqle);
      log.error("publisherSeq = '" + publisherSeq + "'.");
      log.error("SQL = '" + FIND_PUBLISHER_AUS_QUERY + "'.");
      throw new DbException("Cannot find the AUs of a publisher", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAus);
    }

    return auSeqs;
  }

  /**
   * Updates the identifier of the parenet of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the identifier of the metadata item.
   * @param parentSeq
   *          A Long with the identifier of the parent metadata item.
   * @return a Set<Long> with the identifiers of the Archival Units.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void updateMdItemParentSeq(Connection conn, Long mdItemSeq, Long parentSeq)
      throws DbException {
    final String DEBUG_HEADER = "updateMdItemParentSeq(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "parentSeq = " + parentSeq);
    }

    PreparedStatement updateParentSeq =
	dbManager.prepareStatement(conn, UPDATE_MD_ITEM_PARENT_SEQ_QUERY);

    try {
      updateParentSeq.setLong(1, parentSeq);
      updateParentSeq.setLong(2, mdItemSeq);
      int count = dbManager.executeUpdate(updateParentSeq);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update the parent sequence", sqle);
      log.error("mdItemSeq = '" + mdItemSeq + "'.");
      log.error("parentSeq = '" + parentSeq + "'.");
      log.error("SQL = '" + UPDATE_MD_ITEM_PARENT_SEQ_QUERY + "'.");
      throw new DbException("Cannot update the parent sequence", sqle);
    } finally {
      DbManager.safeCloseStatement(updateParentSeq);
    }
  }

  /**
   * Adds to the database the URLs of a metadata item, if they are new.
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
  void addNewMdItemUrls(Connection conn, Long mdItemSeq, String accessUrl,
      Map<String, String> featuredUrlMap) throws DbException {
    if (StringUtil.isNullString(accessUrl) && featuredUrlMap.size() == 0) {
      return;
    }

    // Initialize the collection of URLs to be added.
    Map<String, String> newUrls = new HashMap<String, String>(featuredUrlMap);
    newUrls.put(ACCESS_URL_FEATURE, accessUrl);

    addNewMdItemUrls(conn, mdItemSeq, newUrls);
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

    Map<String, String> oldUrls = getMdItemUrls(conn, mdItemSeq);
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
   * Provides the URLs of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Map<String, String> with the URL/feature pairs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Map<String, String> getMdItemUrls(Connection conn, Long mdItemSeq)
	  throws DbException {
    final String DEBUG_HEADER = "getMdItemUrls(): ";

    Map<String, String> featuredUrlMap = new HashMap<String, String>();

    PreparedStatement findMdItemFeaturedUrl =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_FEATURED_URL_QUERY);

    ResultSet resultSet = null;
    String feature;
    String url;

    try {
      // Get the existing URLs.
      findMdItemFeaturedUrl.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(findMdItemFeaturedUrl);

      // Loop through all the URLs already linked to the metadata item.
      while (resultSet.next()) {
	feature = resultSet.getString(FEATURE_COLUMN);
	url = resultSet.getString(URL_COLUMN);
	log.debug3(DEBUG_HEADER + "Found feature = " + feature + ", URL = "
	    + url);

	featuredUrlMap.put(feature, url);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot get the URLs of a metadata item", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItemFeaturedUrl);
    }

    // Add the URLs that are new.
    return featuredUrlMap;
  }

  /**
   * Adds to the database the authors of a metadata item, if they are new.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param authors
   *          A Set<String> with the authors of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addNewMdItemAuthors(Connection conn, Long mdItemSeq,
      Set<String> authors) throws DbException {
    if (authors == null || authors.size() == 0) {
      return;
    }

    // Initialize the collection of authors to be added.
    Set<String> newAuthors = new HashSet<String>(authors);

    // Get the existing authors.
    Set<String> oldAuthors = getMdItemAuthors(conn, mdItemSeq);

    // Remove them from the collection to be added.
    newAuthors.removeAll(oldAuthors);

    // Add the authors that are new.
    addMdItemAuthors(conn, mdItemSeq, newAuthors);
  }

  /**
   * Provides the authors of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Set<String> with the authors of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Set<String> getMdItemAuthors(Connection conn, Long mdItemSeq)
      throws DbException {
    Set<String> authors = new HashSet<String>();

    PreparedStatement findMdItemAuthor =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_AUTHOR_QUERY);

    ResultSet resultSet = null;

    try {
      // Get the existing authors.
      findMdItemAuthor.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(findMdItemAuthor);

      while (resultSet.next()) {
	authors.add(resultSet.getString(AUTHOR_NAME_COLUMN));
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot get the authors of a metadata item", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItemAuthor);
    }

    return authors;
  }

  /**
   * Adds to the database the keywords of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param keywords
   *          A Set<String> with the keywords of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addNewMdItemKeywords(Connection conn, Long mdItemSeq,
      Set<String> keywords) throws DbException {
    if (keywords == null || keywords.size() == 0) {
      return;
    }

    // Initialize the collection of keywords to be added.
    Set<String> newKeywords = new HashSet<String>(keywords);

    Set<String> oldKeywords = getMdItemKeywords(conn, mdItemSeq);

    // Remove them from the collection to be added.
    newKeywords.removeAll(oldKeywords);

    // Add the keywords that are new.
    addMdItemKeywords(conn, mdItemSeq, newKeywords);
  }

  /**
   * Provides the keywords of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return A Set<String> with the keywords of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Set<String> getMdItemKeywords(Connection conn, Long mdItemSeq)
      throws DbException {
    Set<String> keywords = new HashSet<String>();

    PreparedStatement findMdItemKeyword =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_KEYWORD_QUERY);

    ResultSet resultSet = null;

    try {
      // Get the existing keywords.
      findMdItemKeyword.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(findMdItemKeyword);

      while (resultSet.next()) {
	keywords.add(resultSet.getString(KEYWORD_COLUMN));
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot get the keywords of a metadata item", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItemKeyword);
    }

    return keywords;
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
   * Adds to the database the authors of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param authors
   *          A Set<String> with the authors of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addMdItemAuthors(Connection conn, Long mdItemSeq,
      Set<String> authors) throws DbException {
    final String DEBUG_HEADER = "addMdItemAuthors(): ";

    if (authors == null || authors.size() == 0) {
      return;
    }

    PreparedStatement insertMdItemAuthor =
	dbManager.prepareStatement(conn, INSERT_AUTHOR_QUERY);

    try {
      for (String author : authors) {
	insertMdItemAuthor.setLong(1, mdItemSeq);
	insertMdItemAuthor.setString(2, author);
	insertMdItemAuthor.setLong(3, mdItemSeq);
	int count = dbManager.executeUpdate(insertMdItemAuthor);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added author = " + author);
	}
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot add metadata item authors", sqle);
    } finally {
      DbManager.safeCloseStatement(insertMdItemAuthor);
    }
  }

  /**
   * Adds to the database the keywords of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param keywords
   *          A Set<String> with the keywords of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addMdItemKeywords(Connection conn, Long mdItemSeq,
      Set<String> keywords) throws DbException {
    final String DEBUG_HEADER = "addMdItemKeywords(): ";

    if (keywords == null || keywords.size() == 0) {
      return;
    }

    PreparedStatement insertMdItemKeyword =
	dbManager.prepareStatement(conn, INSERT_KEYWORD_QUERY);

    try {
      for (String keyword : keywords) {
	insertMdItemKeyword.setLong(1, mdItemSeq);
	insertMdItemKeyword.setString(2, keyword);
	int count = dbManager.executeUpdate(insertMdItemKeyword);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added keyword = " + keyword);
	}
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot add metadata item keywords", sqle);
    } finally {
      DbManager.safeCloseStatement(insertMdItemKeyword);
    }
  }

  /**
   * Provides the identifier of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemTypeSeq
   *          A Long with the identifier of the metadata item type.
   * @param auMdSeq
   *          A Long with the identifier of the archival unit metadata.
   * @param accessUrl
   *          A String with the access URL of the metadata item.
   * @return a Long with the identifier of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findMdItem(Connection conn, Long mdItemTypeSeq, Long auMdSeq,
      String accessUrl) throws DbException {
    final String DEBUG_HEADER = "findMdItem(): ";
    Long mdItemSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findMdItem =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_QUERY);

    try {
      findMdItem.setLong(1, mdItemTypeSeq);
      findMdItem.setLong(2, auMdSeq);
      findMdItem.setString(3, accessUrl);

      resultSet = dbManager.executeQuery(findMdItem);
      if (resultSet.next()) {
	mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot find metadata item", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItem);
    }

    log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
    return mdItemSeq;
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
	getMdItemNames(conn, sourceMdItemSeq);

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

    Set<String> sourceMdItemAuthors = getMdItemAuthors(conn, sourceMdItemSeq);

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

    Set<String> sourceMdItemKeywords = getMdItemKeywords(conn, sourceMdItemSeq);

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

    Map<String, String> sourceMdItemUrls = getMdItemUrls(conn, sourceMdItemSeq);

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
    Set<Isbn> sourceMdItemIsbns = getMdItemIsbns(conn, sourceMdItemSeq);

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
    Set<Issn> sourceMdItemIssns = getMdItemIssns(conn, sourceMdItemSeq);

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
   * Normalizes an ISBN or ISSN number by removing punctuation and truncating it
   * to the maximum allowed number of characters.
   * 
   * @param number
   *          A String with the ISBN or ISSN number to be normalized.
   * @param maxColumnWidth
   *          An int with the maximum column width allowed in the database.
   * @param label
   *          A String for the warning message indicating the type of number
   *          being normalized.
   * @param name
   *          A String with the name of title to which the normalized number
   *          belongs.
   * @param publisher
   *          A String with the name of the publisher of the title.
   * @return A String with the normalized ISBN or ISSN number.
   */
  public String normalizeIsbnOrIssn(String number, int maxColumnWidth,
      String label, String name, String publisher) {
    final String DEBUG_HEADER = "normalizeIsbnOrIssn(): ";

    // Null objects are returned as-is.
    if (number == null) {
      return number;
    }

    // Remove punctuation.
    String normal = number.replaceAll("-", "");
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "normal = '" + normal + "'.");

    // Check whether the number without punctuation fits in the allowed space.
    if (normal.length() <= maxColumnWidth) {
      // Yes: Return it.
      return normal;
    }

    // No: Truncate it.
    log.warning(label + " too long '" + number + "' for title: '" + name
	+ "' publisher: " + publisher + "'");

    return DbManager.truncateVarchar(normal, maxColumnWidth);
  }
}
