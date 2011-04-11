/*
 * $Id: MetadataManager.java,v 1.3.2.13 2011-04-11 23:21:45 pgust Exp $
 */

/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.daemon;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.derby.jdbc.ClientDataSource;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Configuration.Differences;
import org.lockss.config.TdbAu;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractor.Emitter;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuEventHandler;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.scheduler.SchedulableTask;
import org.lockss.scheduler.Schedule;
import org.lockss.scheduler.StepTask;
import org.lockss.scheduler.TaskCallback;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.TimeInterval;
import org.mortbay.util.StringUtil;

/**
 * This class implements a metadata manager that is responsible for managing an
 * index of metadata from AUs.
 * 
 * @author Philip Gust
 * @version 1.0
 */
public class MetadataManager extends BaseLockssDaemonManager implements
    ConfigurableManager {

  private static Logger log = Logger.getLogger("MetadataMgr");

  /** prefix for config properties */
  static public final String PREFIX = "org.lockss.daemon.metadataManager.";

  /**
   * Determines whether MedataExtractor specified by plugin 
   * should be used if it is available. If <code>false</code>, a 
   * MetaDataExtractor is created that returns data from the TDB rather
   * than from the content metadata. This is faster than extracting
   * metadata form content, but less complete. Use only when minimal
   * article info is required. This parameter can only be set at startup.
   */
  static public final String PARAM_USE_METADATA_EXTRACTOR = 
    PREFIX + "use_metadata_extractor";

  /**
   * Default value of MetadataManager use_metadata_extractor configuration
   * parameter; <code>true</code> to use specified MetadataExtractor.
   */
  static public final boolean DEFAULT_USE_METADATA_EXTRACTOR = true;

  /**
   * Determines whether indexing should be enabled. If indexing is not
   * enabled, AUs are queued for indexing, but the AUs are not reindexed
   * until the process is re-enabled. This parameter can be changed at
   * runtime.
   */
  static public final String PARAM_INDEXING_ENABLED = 
    PREFIX + "indexing_enabled";

  /**
   * Default value of MetadataManager indexing enabled configuration
   * parameter; <code>false</code> to disable, <code>true</code> to enable.
   */
  static public final boolean DEFAULT_INDEXING_ENABLED = false;

  /** 
   * The maximum number of concurrent reindexing tasks. This
   * property can be changed at runtime 
   */
  static public final String PARAM_MAX_REINDEXING_TASKS = 
    PREFIX + "maxReindexingTasks";

  /** Default maximum concurrent reindexing tasks */
  static public final int DEFAULT_MAX_REINDEXING_TASKS = 1;


  /** Property tree root for datasource properties */
  static public final String DATASOURCE_ROOT = PREFIX + "datasource";
  

  /**
   * Map of running reindexing tasks
   */
  final Map<ArchivalUnit, ReindexingTask> reindexingTasks = 
    new HashMap<ArchivalUnit, ReindexingTask>();

  /**
   * Metadata manager indexing enabled flag
   */
  boolean reindexingEnabled = DEFAULT_INDEXING_ENABLED;

  /**
   * Metadata manager use metadata extractor flag. Note: set this to false only
   * where specific metadata from the metadata extractor are not needed.
   */
  boolean useMetadataExtractor = DEFAULT_USE_METADATA_EXTRACTOR;

  /** Determines whether new database was created */
  boolean dbIsNew = false;

  /** Maximum number of reindexing tasks */
  int maxReindexingTasks = DEFAULT_MAX_REINDEXING_TASKS;

  /** The database data source */
  private DataSource dataSource = null;

  /** The plugin manager */
  private PluginManager pluginMgr = null;

  /** Name of DOI table */
  static final String DOI_TABLE = "DOI";

  /** Name of ISSN table */
  static final String ISSN_TABLE = "ISSN";

  /** Name of ISBN table */
  static final String ISBN_TABLE = "ISBN";

  /** Name of Metadata table */
  static final String METADATA_TABLE = "Metadata";

  /** Name of Pending AUs table */
  static final String PENDINGAUS_TABLE = "PendingAus";

  /** Name of access_url field */
  static public final String ACCESS_URL_FIELD = "access_url";
  
  /** Name of article_title field */
  static public final String ARTICLE_TITLE_FIELD = "article_title";
  
  /** Name of au_key field */
  static public final String AU_KEY_FIELD = "au_key";
  
  /** Name of author field */
  static public final String AUTHOR_FIELD = "author";
  
  /** Name of date field */
  static public final String DATE_FIELD = "date";
  
  /** Name of doi field */
  static public final String DOI_FIELD = "doi";
  
  /** Name of isbn field */
  static public final String ISBN_FIELD = "isbn";
  
  /** Name of issn field */
  static public final String ISSN_FIELD = "issn";
  
  /** Name of issue field */
  static public final String ISSUE_FIELD = "issue";
  
  /** Name of md_id foreign key field */
  static public final String MD_ID_FIELD = "md_id";
  
  /** Name of plugin_id field */
  static public final String PLUGIN_ID_FIELD = "plugin_id";
  
  /** Name of start_page field */
  static public final String START_PAGE_FIELD = "start_page";
  
  /** Name of volume field */
  static public final String VOLUME_FIELD = "volume";
  
  
  /** Length of access URL field */
  static public final int MAX_ACCESS_URL_FIELD = 4096;

  /** Length of database article title field */
  static public final int MAX_ATITLE_FIELD = 512;

  /** public of au key field */
  static private final int MAX_AU_KEY_FIELD = 512;

  /** Length of database author field -- enough for maybe first three authors */
  static public final int MAX_AUTHOR_FIELD = 512;

  /** Length of date field */
  static public final int MAX_DATE_FIELD = 16;
  
  /** Length of doi field */
  static public final int MAX_DOI_FIELD = 256;
  
  /** Length of isbn field */
  static public final int MAX_ISBN_FIELD = 13;
  
  /** Length of issn field */
  static public final int MAX_ISSN_FIELD = 8;
  
  /** Length of issue field */
  static public final int MAX_ISSUE_FIELD = 16;
  
  /** 
   * Length of plugin ID field. This field will be used as horizontal
   * partitioning field in the future, so it's length must be compatible
   * for that purpose for the database product used.
   */
  static public final int MAX_PLUGIN_ID_FIELD = 128;
  
  /** Length of start_page field */
  static public final int MAX_STARTPAGE_FIELD = 16;
  
  /** Length of volume field */
  static public final int MAX_VOLUME_FIELD = 16;
  
  private long totalCpuTime = 0;
  private long totalUserTime = 0;
  private long totalClockTime = 0;

  private static ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
  static {
    log.debug3(  "current thread CPU time supported? "
               + tmxb.isCurrentThreadCpuTimeSupported());
    if (tmxb.isCurrentThreadCpuTimeSupported()) {
      tmxb.setThreadCpuTimeEnabled(true);
    }
  }


  /**
   * Start MetadataManager service
   */
  @Override
  public void startService() {
    log.debug("Starting MetadataManager");

     if (!initializeService(ConfigManager.getCurrentConfig())) {
       log.error("Error initializing manager");
       return;
     }

    Connection conn;
    try {
      conn = newConnection();
    } catch (SQLException ex) {
      log.error("Cannot connect to database -- service not started");
      return;
    }

    // create schema and initialize tables if schema does not exist
    dbIsNew = !tableExists(conn, PENDINGAUS_TABLE);
    if (dbIsNew) {
      try {
        executeBatch(conn, createSchema);
        safeCommit(conn);
      } catch (BatchUpdateException ex) {
        // handle batch update exception
        int[] counts = ex.getUpdateCounts();
        for (int i = 0; i < counts.length; i++) {
          log.error(  "Error in schema statement " + i 
                            + "(" + counts[i] + "): "
                    + createSchema[i]);
        }
        log.error("Cannot initialize schema -- service not started", ex);
        safeClose(conn);
        return;
      } catch (SQLException ex) {
        log.error("Cannot initialize schema -- service not started", ex);
        safeClose(conn);
        return;
      }
    }

    safeClose(conn);

    // register to receive content change notifications to 
    // reindex the database content associated with the au
    pluginMgr = getDaemon().getPluginManager();
    pluginMgr.registerAuEventHandler(new AuEventHandler.Base() {

      public void auContentChanged(ArchivalUnit au, ChangeInfo info) {
        if (info.isComplete()) {
          Connection conn = null;
          try {
            log.debug2("Adding changed au to reindex: " + au.getName());
            // add pending AU
            conn = newConnection();
            if (conn == null) {
              log.error(  "Cannot connect to database"
                        + " -- cannot add changed aus to pending aus");
              return;
            }

            addToPendingAus(conn, Collections.singleton(au));
            conn.commit();

            synchronized (reindexingTasks) {
              ReindexingTask task = reindexingTasks.get(au);
              if (task != null) {
                // task cancellation will remove task and schedule next one
                log.debug2(  "Canceling pending reindexing task for au "
                           + au.getName());
                // cancel running task because previous work may be out of
                // date; canceled task is  automatically rescheduled later
                task.cancel();
              } else {
                log.debug2("Scheduling reindexing tasks");
                startReindexing(conn);
              }
            }
          } catch (SQLException ex) {
            log.error("Cannot add au to pending AUs: " + au.getName(), ex);
          } finally {
            safeClose(conn);
          }
        }
      }
    });

    // start metadata extraction
    MetadataStarter starter = new MetadataStarter();
    new Thread(starter).start();
  }

  /**
   * Handle new configuration.
   * 
   * @param config the new configuration
   * @param prevConfig the previous configuration
   * @changedKeys the configuration keys that changed
   */
  @Override
  public void setConfig(Configuration config, Configuration prevConfig,
                        Differences changedKeys) {
    useMetadataExtractor = config.getBoolean(PARAM_USE_METADATA_EXTRACTOR,
                                             DEFAULT_USE_METADATA_EXTRACTOR);
    maxReindexingTasks = config.getInt(PARAM_MAX_REINDEXING_TASKS, 
                                       DEFAULT_MAX_REINDEXING_TASKS);
    maxReindexingTasks = Math.max(0, maxReindexingTasks);
    boolean doEnable = config.getBoolean(PARAM_INDEXING_ENABLED,
                                         DEFAULT_INDEXING_ENABLED);
    setIndexingEnabled(doEnable);

  }

  /**
   * Restart the Metadata Managaer service by terminating any running 
   * reindexing tasks and then resetting its database before calling 
   * {@link #startServie()}.
   * <p>
   * This method is only used for testing.
   */
  protected void restartService() {
    if (!initializeService(ConfigManager.getCurrentConfig())) {
      log.error("Error initializing manager -- service not started");
      return;
    }

    stopReindexing();

    Connection conn;
    try {
      conn = newConnection();
    } catch (SQLException ex) {
      log.error("Cannot get database connection -- service not started");
      return;
    }

    // reset database tables
    try {
      // drop schema tables already exist
      if (tableExists(conn, PENDINGAUS_TABLE)) {
        executeBatch(conn, dropSchema);
      }
      conn.commit();
      conn.close();
    } catch (BatchUpdateException ex) {
      // handle batch update exception
      int[] counts = ex.getUpdateCounts();
      for (int i = 0; i < counts.length; i++) {
        log.error(  "Error in statement " + i + "(" + counts[i] + "): "
                  + createSchema[i]);
      }
      log.error("Cannot drop existing schema -- service not started", ex);
      safeClose(conn);
      return;
    } catch (SQLException ex) {
      log.error("Cannot drop existing schema -- service not started", ex);
      safeClose(conn);
      return;
    }

    // start the service
    startService();
  }

  /**
   * Initialize the service from the current configuration.
   * 
   * @param config the configuration
   * @return <code>true</code> if initialized
   */
  protected boolean initializeService(Configuration config) {
    // already initialized if datasource exists
    if (dataSource != null) {
      return true;  // already initialized
    }

    // determine maximum number of concurrent reindexing tasks
    // (0 disables reindexing)
    maxReindexingTasks = config.getInt(PARAM_MAX_REINDEXING_TASKS,
                                       DEFAULT_MAX_REINDEXING_TASKS);
    maxReindexingTasks = Math.max(0, maxReindexingTasks);

    // get datasource class and properties
    Configuration datasourceConfig = ConfigManager.newConfiguration();
    datasourceConfig.copyFrom(
      ConfigManager.getCurrentConfig().getConfigTree(DATASOURCE_ROOT));
    String dataSourceClassName = datasourceConfig.get("className");
    if (dataSourceClassName != null) {
      // class name not really part of data source definition.
      datasourceConfig.remove("className");
    } else {
      File databaseFile = new File(getDbRootDirectory(), "db/MetadataManager");
      String databaseName = databaseFile.getAbsolutePath();
      // use derby embedded datasource by default
      dataSourceClassName = "org.apache.derby.jdbc.EmbeddedDataSource";
      datasourceConfig.put("databaseName", databaseName);
      datasourceConfig.put("description", "Embeddded JavaDB data source");
      datasourceConfig.put("user", "LOCKSS");
//      datasourceConfig.put("password", "LOCKSS");
      datasourceConfig.put("createDatabase", "create");
    }
    Class<?> cls;
    try {
      cls = Class.forName(dataSourceClassName);
    } catch (Throwable ex) {
      log.error("Cannot locate datasource class \"" + dataSourceClassName
          + "\"", ex);
      return false;
    }

    // create datasource
    try {
      dataSource = (DataSource) cls.newInstance();
    } catch (ClassCastException ex) {
      log.error("Class not a DataSource \"" + dataSourceClassName + "\"");
      return false;
    } catch (Throwable ex) {
      log.error("Cannot create instance of datasource class \""
          + dataSourceClassName + "\"", ex);
      return false;
    }
    boolean errors = false;
    // initialize datasource properties
    for (String key : datasourceConfig.keySet()) {
      String value = datasourceConfig.get(key);
      try {
        setPropertyByName(dataSource, key, value);
      } catch (Throwable ex) {
        errors = true;
        log.error(  "Cannot set property \"" + key
                  + "\" for instance of datasource class \"" 
                  + dataSourceClassName + "\"", ex);
      }
    }
    if (errors) {
      log.error(  "Cannot initialize instance of datasource class \""
                + dataSourceClassName + "\"");
      dataSource = null;
      return false;
    }
    
    boolean ready = true;
    if (dataSource instanceof ClientDataSource) {
      // start Derby NetworkServerControl for client connection
      ClientDataSource cds = (ClientDataSource) dataSource;
      String serverName = cds.getServerName();
      int serverPort = cds.getPortNumber();
      try {
        ready = startNetworkServerControl(serverName, serverPort);
        if (!ready) {
          log.error("Cannot enable remote access to Derby database");
          dataSource = null;
        }
      } catch (UnknownHostException ex) {
        log.error("Unknown host for remote Derby database: " + serverName);
        dataSource = null;
        ready = false;
      }
    }

    return ready;
  }
  
  /**
   * Start the Derby NetworkServerConrol and wait for it to be ready.
   * 
   * @param serverName the server name
   * @param serverPort the server port
   * @throws UnknownHostException if serverName not valid
   */
  private boolean startNetworkServerControl(String serverName, int serverPort) 
    throws UnknownHostException {
    // if dataSource is a Derby ClientDataSource, enable remote access
    InetAddress inetAddr = InetAddress.getByName(serverName);

    org.apache.derby.drda.NetworkServerControl networkServerControl; 
    try {
        networkServerControl = 
          new org.apache.derby.drda.NetworkServerControl(inetAddr, serverPort);
      networkServerControl.start(null);
    } catch (Exception ex) {
      return false; // unspecified error occurred
    }
    
    // wait for network server control to be ready
    for (int i = 0; i < 40; i++) { // at most 20 seconds;
      try {
        networkServerControl.ping();
        return true;
      } catch (Exception ex) {
        // control not ready; wait and try again
        try {
          Thread.sleep(500);       // about 1/2 second
        } catch (InterruptedException ex2) {
          break;
        }
      }
    }
    return false;
  }

  /**
   * Set a bean property for an object by name. Property value string is
   * converted to the appropriate data type.
   * 
   * @param obj the object
   * @param propName the property name
   * @param propValue the property value
   * @throws Throwable if an error occurred.
   */
  private void setPropertyByName(Object obj, String propName, String propValue)
      throws Throwable {

    String setterName =   "set" + propName.substring(0, 1).toUpperCase()
                        + propName.substring(1);
    for (Method m : obj.getClass().getMethods()) {
      // find matching setter method
      if (m.getName().equals(setterName)) {
        // find single-argument method
        Class<?>[] paramTypes = m.getParameterTypes();
        if (paramTypes.length == 1) {
          try {
            // only handle argument types that have String constructors
            @SuppressWarnings("rawtypes")
            Class paramType = ClassUtils.primitiveToWrapper(paramTypes[0]);
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Constructor c = paramType.getConstructor(String.class);
            // invoke method with instance created from string
            Object val = c.newInstance(propValue);
            m.invoke(obj, val);
            return;
          } catch (Throwable ex) {
            // ignore
            log.debug2(setterName, ex);
          }
        }
      }
    }
    throw new NoSuchMethodException(  obj.getClass().getName() 
                                    + "." + setterName + "()");
  }

  /**
   * Get the root directory for creating the database files.
   * 
   * @return the root directory
   */
  protected String getDbRootDirectory() {
    Configuration config = ConfigManager.getCurrentConfig();
    String rootDir = config.get(ConfigManager.PARAM_TMPDIR);
    @SuppressWarnings("unchecked")
    List<String> dSpaceList = 
    	config.getList(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST);
    if (dSpaceList != null && !dSpaceList.isEmpty()) {
      rootDir = dSpaceList.get(0);
    }
    return rootDir;
  }

  /**
   * Determines whether a table exists.
   * 
   * @param conn the connection
   * @param tableName the table name
   * @return <code>true</code> if the named table exists
   */
  private static boolean tableExists(Connection conn, String tableName) {
    if (conn != null) {
      try {
        Statement st = conn.createStatement();
        st.setMaxRows(1);

        String sql = "select * from " + tableName;
        st.executeQuery(sql);
        st.close();
        return true;
      } catch (SQLException ex) {
      }
    }
    return false;
  }

  /**
   * Write the named table schema to the log. For debugging purposes only.
   * <p>
   * See http://www.roseindia.net/jdbc/Jdbc-meta-data-get-tables.shtml
   * 
   * @param conn the connection
   * @param tableName the table name
   */
  @SuppressWarnings("unused")
  private static void logSchema(Connection conn, String tableName) {
    if (conn != null) {
      try {
        Statement st = conn.createStatement();
        st.setMaxRows(1);

        String sql = "select * from " + tableName;
        ResultSet rs = st.executeQuery(sql);
        ResultSetMetaData metaData = rs.getMetaData();

        int rowCount = metaData.getColumnCount();

        log.info("Table Name : " + metaData.getTableName(2));
        log.info("Field  \tsize\tDataType");

        for (int i = 1; i <= rowCount; i++) {
          StringBuilder sb = new StringBuilder();
          sb.append(metaData.getColumnName(i));
          sb.append("  \t");
          sb.append(metaData.getColumnDisplaySize(i));
          sb.append(" \t");
          sb.append(metaData.getColumnTypeName(i + 1));
          log.info(sb.toString());
        }
        st.close();
        return;
      } catch (SQLException ex) {
      }
    }
    log.info("Cannot log table \"" + tableName + "\"");
  }

  /**
   * SQL statements that create the database schema
   */
  private static final String createSchema[] = {
    "create table " + PENDINGAUS_TABLE + " ("
        + PLUGIN_ID_FIELD + " VARCHAR(64) NOT NULL," 
        + AU_KEY_FIELD + " VARCHAR(" + MAX_AU_KEY_FIELD + ") NOT NULL" + ")",
    "create table " + METADATA_TABLE + " ("
        + MD_ID_FIELD + " BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,"
        + DATE_FIELD + " VARCHAR(" + MAX_DATE_FIELD + "),"
        + VOLUME_FIELD + " VARCHAR(" + MAX_VOLUME_FIELD + ")," 
        + ISSUE_FIELD + " VARCHAR(" + MAX_ISSUE_FIELD + "),"
        + START_PAGE_FIELD + " VARCHAR(" + MAX_STARTPAGE_FIELD + "),"
        + ARTICLE_TITLE_FIELD + " VARCHAR(" + MAX_ATITLE_FIELD + ")," 
        // author field is a semicolon-separated list
        + AUTHOR_FIELD + " VARCHAR(" + MAX_AUTHOR_FIELD + "),"
        + PLUGIN_ID_FIELD + " VARCHAR(" + MAX_PLUGIN_ID_FIELD + ") NOT NULL,"
        // partition by
        + AU_KEY_FIELD + " VARCHAR(" + MAX_AU_KEY_FIELD + ") NOT NULL,"
        + ACCESS_URL_FIELD + " VARCHAR(" + MAX_ACCESS_URL_FIELD + ") NOT NULL" 
        + ")",

    "create table " + DOI_TABLE + " (" 
        + DOI_FIELD + " VARCHAR(" + MAX_DOI_FIELD + ") NOT NULL,"
        + MD_ID_FIELD + " BIGINT NOT NULL REFERENCES " + METADATA_TABLE
        + "(md_id) on delete cascade" + ")",

    "create table " + ISBN_TABLE + " (" 
        + ISBN_FIELD + " VARCHAR(" + MAX_ISBN_FIELD + ") NOT NULL,"
        + MD_ID_FIELD + " BIGINT NOT NULL REFERENCES " + METADATA_TABLE
        + "(md_id) on delete cascade" + ")",

    "create table " + ISSN_TABLE + " (" 
        + ISSN_FIELD + " VARCHAR(" + MAX_ISSN_FIELD + ") NOT NULL,"
        + MD_ID_FIELD + " BIGINT NOT NULL REFERENCES " + METADATA_TABLE
        + "(md_id) on delete cascade" + ")", };

  /**
   * SQL statements that drop the database schema
   */
  private static final String[] dropSchema = new String[] {
    "drop table " + PENDINGAUS_TABLE, 
    "drop table " + DOI_TABLE,
    "drop table " + ISSN_TABLE, 
    "drop table " + ISBN_TABLE,
    "drop table " + METADATA_TABLE, };

  /**
   * Execute a batch of statements.
   * 
   * @param conn the connection
   * @param stmts the statements
   * @throws BatchUpdateException if a batch update exception occurred
   * @throws SQLException if any other SQL exception occurred
   */
  private void executeBatch(Connection conn, String[] stmts)
      throws BatchUpdateException, SQLException {
    Statement s = conn.createStatement();
    for (String stmt : stmts) {
      s.addBatch(stmt);
    }
    s.executeBatch();
  }

  /**
   * Get the DataSource for this instance.
   * 
   * @return the DataSource for this instance
   */
  DataSource getDataSource() {
    return dataSource;
  }

  /**
   * Create a new database connection using the datasource. Autocommit is
   * disabled.
   * 
   * @return the new connection
   * @throws SQLException if an error occurred while connecting
   */
  public Connection newConnection() throws SQLException {
    if (dataSource == null) {
      throw new SQLException("No datasource");
    }

    Connection conn = dataSource.getConnection();
    conn.setAutoCommit(false);
    return conn;
  }

  /**
   * Ensure that as many reindexing tasks as possible are running if manager is
   * enabled.
   * 
   * @param conn the database connection
   * @return the number of reindexing tasks started
   */
  private int startReindexing(Connection conn) {
    if (!getDaemon().isDaemonInited()) {
      log.debug("daemon not initialized: no reindexing tasks.");
      return 0;
    }

    // don't reindexing tasks run if reindexing is disabled
    if (!reindexingEnabled) {
      log.debug("metadata manager is disabled: no reindexing tasks.");
      return 0;
    }

    int reindexedTaskCount = 0;
    synchronized (reindexingTasks) {
      while (reindexingTasks.size() < maxReindexingTasks) {
        // get list of pending aus to reindex
        List<ArchivalUnit> aus =
      	  getAusToReindex(conn, maxReindexingTasks - reindexingTasks.size());
        if (aus.isEmpty()) {
        	break;
        }
  
        // schedule pending aus
        for (ArchivalUnit au : aus) {
          ArticleMetadataExtractor ae = getArticleMetadataExtractor(au);
          if (ae == null) {
            // shouldn't happen because it was checked before adding to pending
            log.debug(  "not running reindexing task for au: " + au.getName()
                      + "  because it nas no metadata extractor");
            try {
              removeFromPendingAus(conn, au);
            } catch (SQLException e) {
              log.error("Error removing au " + au.getName() 
                        + "from pending", e);
              break;
            }
          } else {
            log.debug("running reindexing task for au: " + au.getName());
            ReindexingTask task = new ReindexingTask(au, ae);
            reindexingTasks.put(au, task);
            runReindexingTask(task);
            reindexedTaskCount++;
          }
        }
      }
    }
    log.debug("Started " + reindexedTaskCount + " au reindexing tasks");
    return reindexedTaskCount;
  }

  /**
   * Ensure as many reindexing tasks as possible are running if manager is
   * enabled.
   * 
   * @return the number of reindexing tasks started
   */
  private int startReindexing() {

    Connection conn = null;
    int count = 0;
    try {
      conn = newConnection();
      count = startReindexing(conn);
    } catch (SQLException ex) {
      log.debug("Cannot connect to database -- indexing not started", ex);
    } finally {
      safeClose(conn);
    }
    return count;
  }

  /**
   * Stop any pending reindexing operations.
   */
  private void stopReindexing() {
    log.debug(  "number of reindexing tasks being stopped: "
              + reindexingTasks.size());

    // quit any running reindexing tasks
    synchronized (reindexingTasks) {
      for (MetadataManager.ReindexingTask task : reindexingTasks.values()) {
        task.cancel();
      }
      reindexingTasks.clear();
    }
  }

  /**
   * Gets indexing enabled state of this manager.
   * @return the indexing enabled state of this manager
   */
  public boolean isIndexingEnabled() {
    return reindexingEnabled;
  }

  /**
   * Set indexing enabled state of this manager.
   * 
   * @param enable new enabled state of manager
   */
  public void setIndexingEnabled(boolean enable) {
    boolean wasEnabled = reindexingEnabled;
    reindexingEnabled = enable;
    log.debug("enabled: " + reindexingEnabled);

    // start or stop reindexing if initialized
    if (dataSource != null) {
      if (!wasEnabled && reindexingEnabled) {
        // start reindexing
        startReindexing();
      } else if (wasEnabled && !reindexingEnabled) {
        // stop any pending reindexing operations
        stopReindexing();
      }
    }
  }

  /**
   * This class handles deferred initialization until AUs are available.
   * @author phil
   *
   */
  private class MetadataStarter extends LockssRunnable {
    public MetadataStarter() {
      super("MetadataStarter");
    }

    // start metadata extraction process
    public void lockssRun() {
      LockssDaemon daemon = getDaemon();

      // add all aus to list of pending aus
      if (!daemon.areAusStarted()) {
        log.debug("Waiting for aus to start");
        while (!getDaemon().areAusStarted()) {
          try {
            getDaemon().waitUntilAusStarted();
          } catch (InterruptedException ex) {
          }
        }
      }
      log.debug("Starting metadata extraction");

      Connection conn;
      try {
        conn = newConnection();
      } catch (SQLException ex) {
        log.error("Cannot connect to database -- extraction not started");
        return;
      }

      if (dbIsNew) {
        try {
          Collection<ArchivalUnit> allAus = pluginMgr.getAllAus();
          addToPendingAus(conn, allAus);
          safeCommit(conn);
          dbIsNew = false;
        } catch (SQLException ex) {
          log.error(  "Cannot add pending AUs table \"" + PENDINGAUS_TABLE
                    + "\"", ex);
          safeClose(conn);
          return;
        }
      }

      startReindexing(conn);
      safeClose(conn);
    }
  }

  /**
   * Determines whether the AU has article metadata.
   * 
   * @param au the au
   * @return <code>true</code> if the AU has article metadata
   */
  private boolean hasArticleMetadata(ArchivalUnit au) {
    if (au.getArticleIterator(MetadataTarget.Article) == null) {
      return false;
    }
    // has article metadata if there is a metadata extractor
    if (useMetadataExtractor) {
      Plugin p = au.getPlugin();
      if (p.getArticleMetadataExtractor(MetadataTarget.Article, au) != null) {
        return true;
      }
    }
    // otherwise has metadata if can create it from TitleConfig's TdbAu 
    TitleConfig tc = au.getTitleConfig();
    return (tc != null) && (tc.getTdbAu() != null);
  }
  
  /**
   * Get the ArticleMetadataExtractor for the specified au.
   * 
   * @param au the au
   * @return the article metadata extractor
   */
  private ArticleMetadataExtractor 
    getArticleMetadataExtractor(ArchivalUnit au) {
    
    ArticleMetadataExtractor ae = null;
    if (useMetadataExtractor) {
      Plugin plugin = au.getPlugin();
      ae = plugin.getArticleMetadataExtractor(MetadataTarget.Article, au);
    }
    if (ae == null) {
      TitleConfig tc = au.getTitleConfig();
      TdbAu tdbau = (tc == null) ? null : tc.getTdbAu();
      if (tdbau == null) {
        return null;
      }

      // get metadata from tdbau
      final String journalTitle = tdbau.getJournalTitle();
      final String volume = tdbau.getStartVolume(); // use start if a range
      final String year = tdbau.getStartYear(); // use start if a range
      final String issn = tdbau.getPrintIssn();
      final String eissn = tdbau.getEissn();
//      final String issnl = tdbau.getIssnL();
      final String isbn = tdbau.getIsbn();

      ae = new ArticleMetadataExtractor() {

        @Override
        public void extract(
          MetadataTarget target, ArticleFiles af, Emitter emitter) 
          throws IOException, PluginException {
          // must have a URL to be useful in the database
          String url = af.getFullTextUrl();
          if (url != null) {
            ArticleMetadata md = new ArticleMetadata();
            md.put(MetadataField.FIELD_ACCESS_URL, url);
            if (isbn != null)  md.put(MetadataField.FIELD_ISBN, isbn);
            if (issn != null) md.put(MetadataField.FIELD_ISSN, issn);
            if (eissn != null) md.put(MetadataField.FIELD_EISSN, eissn);
            if (volume != null) md.put(MetadataField.FIELD_VOLUME, volume);
            if (year != null) md.put(MetadataField.FIELD_DATE, year);
            if (journalTitle != null) md.put(MetadataField.FIELD_JOURNAL_TITLE,
                                             journalTitle);
            emitter.emitMetadata(af, md);
          }
        }
      };
    }
    return ae;
  }

  /**
   * This class implements a reindexing task that extracts metadata from 
   * all the articles in the specified AU.
   */
  class ReindexingTask extends StepTask {

    /** The archival unit for this task */
    private final ArchivalUnit au;

    /** The article metadata extractor for this task */
    private final ArticleMetadataExtractor ae;

    /** The article iterator for this task */
    private Iterator<ArticleFiles> articleIterator = null;

    /** List of log messages already emitted for this task's au */
    private final HashSet<Integer> auLogTable = new HashSet<Integer>();
        
    /** The database connection for this task */
    private Connection conn;

    /** The default number of steps for this task */
    private static final int default_steps = 10;

    /** The status of the task: successful if true */
    private boolean statusOk = true;
    
    // properties of AU and the TdbAu for this task
    private final String auid;
    private final String pluginId;
    private final String auKey;
    private final TdbAu tdbau;
    private final String tdbauStartYear;
    private final String tdbauIsbn;
    private final String tdbauIssn;
    private final String tdbauEissn;
    private final String tdbauYear;
    private final String tdbauJournalTitle;
    private final String tdbauName;


    /**
     * Create a reindexing task for the AU
     * 
     * @param theAu the archival unit
     * @param theAe the article metadata extractor to use
     */
    public ReindexingTask(ArchivalUnit theAu, ArticleMetadataExtractor theAe) {
      // NOTE: estimated window time interval duration not currently used.
      super(new TimeInterval(System.currentTimeMillis(),
                             System.currentTimeMillis() + Constants.HOUR), 
             0, /* long estimatedDuration */
             null, /* TaskCallback */
             null); /* Object cookie */

      this.au = theAu;
      this.ae = theAe;

      // initialize values used for processing every article in the AU
      auid = au.getAuId();
      pluginId = PluginManager.pluginIdFromAuId(auid);
      auKey = PluginManager.auKeyFromAuId(auid);
      TitleConfig tc = au.getTitleConfig();
      tdbau = (tc == null) ? null : tc.getTdbAu();
      tdbauName = (tdbau == null) ? null : tdbau.getName();
      tdbauStartYear = (tdbau == null) ? null : tdbau.getStartYear();
      tdbauIsbn = (tdbau == null) ? null : tdbau.getIsbn();
      tdbauIssn = (tdbau == null) ? null : tdbau.getPrintIssn();
      tdbauEissn = (tdbau == null) ? null : tdbau.getEissn();
      tdbauYear = (tdbau == null) ? null : tdbau.getYear();
      tdbauJournalTitle = (tdbau == null) ? null : tdbau.getJournalTitle();

      // set task callback after construction to ensure instance is initialized
      callback = new TaskCallback() {
        // ThreadMXBean times
        long startCpuTime = 0;
        long startUserTime = 0;
        long startClockTime = 0;

        public void taskEvent(SchedulableTask task, Schedule.EventType type) {
          long threadCpuTime = 0;
          long threadUserTime = 0;
          long currentClockTime = System.currentTimeMillis();
          if (tmxb.isCurrentThreadCpuTimeSupported()) {
            threadCpuTime = tmxb.getCurrentThreadCpuTime();
            threadUserTime = tmxb.getCurrentThreadUserTime();
          }

          // TODO: handle task success vs. failure?
          if (type == Schedule.EventType.START) {
            startCpuTime = threadCpuTime;
            startUserTime = threadUserTime;
            startClockTime = currentClockTime;
            // article iterator won't be null because only aus
            // with article iterators are queued for processing
            articleIterator = au.getArticleIterator(MetadataTarget.Article);
            log.debug2("Reindexing task starting for au: " + au.getName()
                + " has articles? " + articleIterator.hasNext());
            try {
              conn = newConnection();

              // remove old metadata before adding new for AU
              removeMetadataForAu(au);
              notifyStartReindexingAu(au);
            } catch (SQLException ex) {
              log.error("Failed to set up to reindex pending au: "
                            + au.getName(), ex);
              setFinished();
              statusOk = false;
            }
          }
          else if (type == Schedule.EventType.FINISH) {
            if (conn != null) {
              try {
                // if reindexing not complete at this point,
                // roll back current transaction, and try later
                if (statusOk) {
                  // remove the AU just reindexed from the pending list
                  removeFromPendingAus(conn, au);
                  
                  if (log.isDebug2()) {
                    long elapsedCpuTime = threadCpuTime = startCpuTime;
                    long elapsedUserTime = threadUserTime - startUserTime;
                    long elapsedClockTime = currentClockTime - startClockTime;
                    totalCpuTime += elapsedCpuTime;
                    totalUserTime += elapsedUserTime;
                    totalClockTime += elapsedClockTime;
                    log.debug2("Reindexing task finished for au: "
                        + au.getName() + " CPU time: " + elapsedCpuTime / 1.0e9
                        + " (" + totalCpuTime / 1.0e9 + "), UserTime: "
                        + elapsedUserTime / 1.0e9 + " (" + totalUserTime
                        / 1.0e9 + ") Clock time: " + elapsedClockTime / 1.0e3
                        + " (" + totalClockTime / 1.0e3 + ")");
                  }
                } else {
                  log.debug2("Reindexing task did not finished for au "
                      + au.getName());
                  conn.rollback();
                  
                  // attempt to move failed AU to end of pending list
                  removeFromPendingAus(conn, au);
                  addToPendingAus(conn, Collections.singleton(au));
                }
                conn.commit();
              } catch (SQLException ex) {
                log.error(  "error finishing processing of pending au: " 
                          + au.getName(), ex);
                statusOk = false;
              }

              synchronized (reindexingTasks) {
                reindexingTasks.remove(au);
                notifyFinishReindexingAu(au, statusOk);

                // schedule another task if available
                startReindexing(conn);
              }
              safeClose(conn);
              conn = null;
            }

            articleIterator = null;
          }
        }
      };
    }

    /**
     * Extract metadata from the next group of articles.
     * 
     * @param n the amount of work to do
     * @todo: figure out what the amount of work means
     */
    public int step(int n) {
      final MutableInt extracted = new MutableInt(0);
      int steps = (n <= 0) ? default_steps : n;
      log.debug3("step: " + steps + ", has articles: "
          + articleIterator.hasNext());

      Emitter emitter = new Emitter() {
        @Override
        public void emitMetadata(ArticleFiles af, ArticleMetadata md) {
          if (log.isDebug3()) {
            log.debug3(  "field access url: " 
                       + md.get(MetadataField.FIELD_ACCESS_URL));
          }
          if (md.get(MetadataField.FIELD_ACCESS_URL) == null) {
            // temporary -- use full text url if not set
            // (should be set by metadata extractor)
            md.put(MetadataField.FIELD_ACCESS_URL, af.getFullTextUrl());
          }
          try {
            addMetadata(md);
            extracted.increment();
          } catch (SQLException ex) {
            log.error(  "Failed to index metadata for article URL: "
                      + af.getFullTextUrl(), ex);
            setFinished();
            statusOk = false;
          }
        }
      };

      while (!isFinished() && (extracted.intValue() <= steps)
          && articleIterator.hasNext()) {
        ArticleFiles af = articleIterator.next();
        try {
          ae.extract(MetadataTarget.OpenURL, af, emitter);
        } catch (IOException ex) {
          log.error(  "Failed to index metadata for full text URL: "
                    + af.getFullTextUrl(), ex);
          setFinished();
          statusOk = false;
        } catch (PluginException ex) {
          log.error(  "Failed to index metadata for full text URL: "
                    + af.getFullTextUrl(), ex);
          setFinished();
          statusOk = false;
        }
      }

      if (!isFinished()) {
        // finished if all articles handled
        if (!articleIterator.hasNext()) {
          setFinished();
        }
      }
      return extracted.intValue();
    }

    /**
     * Issue warning this reindexing task.
     * 
     * @param s the warning messsage
     */
    private void taskWarning(String s) {
      int hashcode = s.hashCode();
      if (auLogTable.add(hashcode)) {
        log.warning(s);
      }
    }
    
    /**
     * Add metadata for this archival unit.
     * 
     * @param conn the connection
     * @param md the metadata
     * @throws SQLException if failed to add rows for metadata
     */
    private void addMetadata(ArticleMetadata md) throws SQLException {
      String journalTitle = md.get(MetadataField.FIELD_JOURNAL_TITLE);
      String isbn = md.get(MetadataField.FIELD_ISBN);
      String issn = md.get(MetadataField.FIELD_ISSN);
      String eissn = md.get(MetadataField.FIELD_EISSN);
      String volume = md.get(MetadataField.FIELD_VOLUME);
      String issue = md.get(MetadataField.FIELD_ISSUE);
      String startPage = md.get(MetadataField.FIELD_START_PAGE);
      PublicationDate pubDate = getDateField(md);
      String pubYear = 
        (pubDate == null) ? null : Integer.toString(pubDate.getYear());
      String articleTitle = getArticleTitleField(md);
      String author = getAuthorField(md);
      String doi = md.get(MetadataField.FIELD_DOI);
      String accessUrl = md.get(MetadataField.FIELD_ACCESS_URL);

      HashSet<String>isbns = new HashSet<String>();
      if (isbn != null) isbns.add(isbn);
      
      HashSet<String>issns = new HashSet<String>();
      if (issn != null) issns.add(issn);
      if (eissn != null) issns.add(eissn);
      
      // validate data against TDB information
      if (tdbau != null) {
        // validate journal title against tdb journal title
        if (journalTitle != null) {
          if (tdbauJournalTitle == null) {
            taskWarning(  "tdb title missing for " +  tdbauName
                        + " -- should be " + journalTitle);
          } else if (!journalTitle.equals(tdbauJournalTitle)) {
            taskWarning ("tdb title " + tdbauJournalTitle
                         + " for " + tdbauName
                         + " -- does not match metadata journal title "
                         + journalTitle);
          }
        } else if (tdbauJournalTitle != null) {
          taskWarning("tdb title  is " + tdbauJournalTitle 
                      + " for " + tdbauName
                      + " -- metadata title is missing");
        }
        
        // validate publication date against tdb year 
        if (pubYear != null) {
          if (!tdbau.includesYear(pubYear)) {
            if (tdbauYear != null) {
              taskWarning(  "tdb year " + tdbauYear 
                          + " for " + tdbauName
                          + " -- does not match metadata year " + pubYear);
            } else {
              taskWarning(  "tdb year missing for " + tdbauName
                          + " -- should include year " + pubYear);
            }
          }
        } else {
          pubYear = tdbauStartYear;
          if (pubYear != null) {
            taskWarning( "using tdb start year " + pubYear
                        + " for " + tdbauName
                        + " -- metadata year is missing");
          }
        }
        
        // validate isbn against tdb isbn
        if (tdbauIsbn != null) {
          if (!tdbauIsbn.equals(isbn)) {
            isbns.add(tdbauIsbn);
            if (isbn == null) {
              taskWarning(  "using tdb isbn " + tdbauIsbn  
                          + " for " + tdbauName
                          + " -- metadata isbn missing");
            } else {
              taskWarning(  "also using tdb isbn " + tdbauIsbn   
                          + " for " + tdbauName
                          + " -- different than metadata isbn: " + isbn);
            }
          } else if (isbn != null) {
            taskWarning(  "tdb isbn missing for " + tdbauName
                        + " -- should be: " + isbn);
          }
        }
        
        // validate issn against tdb issn
        if (tdbauIssn != null) {
          if (tdbauIssn.equals(eissn) && (issn == null)) {
            taskWarning(  "tdb print issn " + tdbauIssn
                        + " for " + tdbauName
                        + " -- reported by metadata as eissn");
          } else if (!tdbauIssn.equals(issn)) {
            // add both ISSNs so it can be found either way
            issns.add(tdbauIssn);
            if (issn == null) {
              taskWarning(  "using tdb print issn " + tdbauIssn   
                          + " for " + tdbauName
                          + " -- metadata print issn is missing");
            } else {
              taskWarning(  "also using tdb print issn " + tdbauIssn   
                          + " for " + tdbauName
                          + " -- different than metadata print issn: " 
                          + issn);
            }
          }
        } else if (issn != null) {
          if (issn.equals(tdbauEissn)) {
            taskWarning( "tdb eissn " + tdbauEissn
                        + " for " + tdbauName
                        + " -- reported by metadata as print issn");
          } else {
            taskWarning(  "tdb issn missing for " + tdbauName
                        + " -- should be: " + issn);
          }
        }

        // validate eissn against tdb eissn
        if (tdbauEissn != null) {
          if (tdbauEissn.equals(issn) && (eissn == null)) {
            taskWarning(  "tdb eissn " + tdbauEissn
                        + " for " + tdbauName
                        + " -- reported by metadata as print issn");
          } else if (!tdbauEissn.equals(eissn)) {
            // add both ISSNs so it can be found either way
            issns.add(tdbauEissn);
            if (eissn == null) {
              taskWarning(  "using tdb eissn " + tdbauEissn   
                          + " for " + tdbauName
                          + " -- metadata eissn is missing");
            } else {
              taskWarning(  "also using tdb eissn " + tdbauEissn   
                          + " for " + tdbauName
                          + " -- different than metadata eissn: " + eissn);
            }
          }
        } else if (eissn != null) {
          if (eissn.equals(tdbauIssn)) {
            taskWarning( "tdb print issn " + tdbauIssn
                        + " for " + tdbauName
                        + " -- reported by metadata as print eissn");
          } else {
            taskWarning(  "tdb eissn missing for " + tdbauName
                        + " -- should be: " + eissn);
          }
        }
      }

      // insert common data into metadata table
      PreparedStatement insertMetadata = 
        conn.prepareStatement("insert into " + METADATA_TABLE + " "
                              + "values (default,?,?,?,?,?,?,?,?,?)",
                              Statement.RETURN_GENERATED_KEYS);
      // TODO PJG: Keywords???
      // skip auto-increment key field #0
      insertMetadata.setString(1, pubDate == null ? null : pubDate.toString());
      insertMetadata.setString(2, volume);
      insertMetadata.setString(3, issue);
      insertMetadata.setString(4, startPage);
      insertMetadata.setString(5, articleTitle);
      insertMetadata.setString(6, author);
      insertMetadata.setString(7, pluginId);
      insertMetadata.setString(8, auKey);
      insertMetadata.setString(9, accessUrl);
      insertMetadata.executeUpdate();
      ResultSet resultSet = insertMetadata.getGeneratedKeys();
      if (!resultSet.next()) {
        log.error("Unable to create metadata entry for auid: " + auid);
        return;
      }
      int mdid = resultSet.getInt(1);
      if (log.isDebug3()) {
        log.debug3("added [accessURL:" + accessUrl + ", md_id: " + mdid
            + ", date: " + pubDate + ", vol: " + volume + ", issue: " + issue
            + ", page: " + startPage + ", pluginId:" + pluginId + "]");
      }
      
      // insert row for DOI
      if (doi != null) {
        if (StringUtil.startsWithIgnoreCase(doi, "doi:")) {
          doi = doi.substring(4);
        }

        /*
         * "doi VARCHAR(MAX_DOI_FIELD) PRIMARY KEY NOT NULL," 
         * + "md_id INTEGER NOT NULL,"
         * + "plugin_id VARCHAR(MAX_PLUGIN_ID_FIELD) NOT NULL" + // partition by
         */
        PreparedStatement insertDOI = conn.prepareStatement(
          "insert into " + DOI_TABLE + " " + "values (?,?)");
        insertDOI.setString(1, doi);
        insertDOI.setInt(2, mdid);
        insertDOI.execute();
        log.debug3(  "added [doi:" + doi + ", md_id: " + mdid + ", pluginId:"
                   + pluginId + "]");
      }

      // insert row for ISBN
      if (!isbns.isEmpty()) {
        PreparedStatement insertISBN = conn.prepareStatement(
          "insert into " + ISBN_TABLE + " " + "values (?,?)");
        insertISBN.setInt(2, mdid);
        for (String anIsbn : isbns) {
          anIsbn = anIsbn.replaceAll("-", "");
          insertISBN.setString(1, anIsbn);
          insertISBN.execute();
          log.debug3("added [isbn:" + anIsbn + ", md_id: " + mdid 
                     + ", pluginId:" + pluginId + "]");
        }
      }

      // insert rows for ISSN
      if (!issns.isEmpty()) {
        PreparedStatement insertISSN = conn.prepareStatement(  
          "insert into " + ISSN_TABLE + " " + "values (?,?)");
        insertISSN.setInt(2, mdid);
        for (String anIssn : issns) {
          anIssn = anIssn.replaceAll("-", "");
          insertISSN.setString(1, anIssn);
          insertISSN.execute();
          log.debug3(  "added [issn:" + anIssn + ", md_id: " + mdid 
                     + ", pluginId:" + pluginId + "]");
        }
      }
    }

    /**
     * Remove an AU from the pending Aus table.
     * 
     * @param au the pending AU
     * @throws SQLException if unable to delete pending AU
     */
    private void removeMetadataForAu(ArchivalUnit au)
        throws SQLException {
      PreparedStatement deletePendingAu = conn.prepareStatement(
      	"delete from "
          + METADATA_TABLE + " where " + PLUGIN_ID_FIELD +  " = ? and "
          + AU_KEY_FIELD + " = ?");
      String auid = au.getAuId();
      String pluginId = PluginManager.pluginIdFromAuId(auid);
      String auKey = PluginManager.auKeyFromAuId(auid);

      deletePendingAu.setString(1, pluginId);
      deletePendingAu.setString(2, auKey);
      deletePendingAu.execute();
    }

    /**
     * Temporary
     * 
     * @param evt
     */
    protected void callCallback(Schedule.EventType evt) {
      callback.taskEvent(this, evt);
    }
  }

  /**
   * 
   * Notify listeners that an AU is being reindexed.
   * 
   * @param au the AU
   */
  protected void notifyStartReindexingAu(ArchivalUnit au) {
  }

  /**
   * Notify listeners that an AU is finshed being reindexed.
   * 
   * @param au
   */
  protected void notifyFinishReindexingAu(ArchivalUnit au, boolean success) {
  }

  /**
   * Schedule a reindexing task for the next pending AU.
   * 
   * @return the AU of the scheduled task
   */
  List<ArchivalUnit> getAusToReindex(Connection conn, int maxAus) {
    ArrayList<ArchivalUnit> aus = new ArrayList<ArchivalUnit>();
    if (pluginMgr != null) {
      try {
        Statement selectPendingAus = conn.createStatement();
        selectPendingAus.setMaxRows(maxAus);

        ResultSet results = 
        	selectPendingAus.executeQuery("select * from PendingAus");
        while (results.next()) {
          String pluginId = results.getString(1);
          String auKey = results.getString(2);
          String auId = PluginManager.generateAuId(pluginId, auKey);
          ArchivalUnit au = pluginMgr.getAuFromId(auId);
          if (au == null) {
            log.debug("Pending au missing from plugin manager: " + auId);
          } else if (!reindexingTasks.containsKey(au)) {
            aus.add(au);
            break;
          }
        }
      } catch (SQLException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    aus.trimToSize();
    return aus;
  }

  /**
   * Run the specified reindexing task.
   * <p>
   * Temporary implementation runs as a LockssRunnable in a thread rather than
   * using the SchedService.
   * 
   * @param task the reindexing task
   */
  private void runReindexingTask(final ReindexingTask task) {
    /*
     * Temporarily running task in its own thread rather than using
     * SchedService
     * @todo Update SchedService to handle this case
     */
    LockssRunnable runnable = 
      new LockssRunnable("Reindexing: " + task.au.getName()) {

      public void lockssRun() {
        task.callCallback(Schedule.EventType.START);
        while (!task.isFinished()) {
          task.step(Integer.MAX_VALUE);
        }
        task.callCallback(Schedule.EventType.FINISH);
      }
    };
    Thread runThread = new Thread(runnable);
    runThread.start();
  }

  /**
   * Return the author field to store in database. The field is comprised of a
   * semicolon separated list of as many authors as will fit in the database
   * author field.
   * 
   * @param md the ArticleMetadata
   * @return the author or <code>null</code> if none specified
   */
  private static String getAuthorField(ArticleMetadata md) {
    StringBuilder sb = new StringBuilder();
    List<String> authors = md.getList(MetadataField.FIELD_AUTHOR);
    // create author field as semicolon-separated list of authors from metadata
    if (authors != null) {
      for (String a : authors) {
        if (sb.length() > 0) {
          // include as many authors as will fit in the field
          if (sb.length() + a.length() + 1 > MAX_AUTHOR_FIELD) {
            break;
          }
          sb.append(';');
        }
    	sb.append(a);
      }
    }
    return (sb.length() == 0) ? null : sb.toString();
  }

  /**
   * Return the article title field to store in the database. The field is
   * truncated to the size of the article title field.
   * 
   * @param md the ArticleMetadata
   * @return the articleTitleField or <code>null</code> if none specified
   */
  private static String getArticleTitleField(ArticleMetadata md) {
    // elide title field
    String articleTitle = md.get(MetadataField.FIELD_ARTICLE_TITLE);
    if ((articleTitle != null) && (articleTitle.length() > MAX_ATITLE_FIELD)) {
      articleTitle = articleTitle.substring(0, MAX_ATITLE_FIELD);
    }
    return articleTitle;
  }

  /**
   * Return the date field to store in the database. The date field can be
   * nearly anything a MetaData extractor chooses to provide, making it a near
   * certainty that this method will be unable to parse it, even with the help
   * of locale information.
   * 
   * @param md the ArticleMetadata
   * @return the publication date or <code>null</code> if none specified 
   *    or one cannot be parsed from the metadata information
   */
  private PublicationDate getDateField(ArticleMetadata md) {
    PublicationDate pubDate = null;
    String dateStr = md.get(MetadataField.FIELD_DATE);
    if (dateStr != null) {
      Locale locale = md.getLocale();
      if (locale == null) {
        locale = Locale.getDefault();
      }
      pubDate = new PublicationDate(dateStr, locale);
    }
    return pubDate;
  }

  /**
   * Close the connection and report an error if cannot close.
   * 
   * @param conn the connection
   */
  public static void safeClose(Connection conn) {
    if (conn != null) {
      try {
        // rollback if not already committed
        conn.rollback();
      } catch (SQLException ex) {
        // ignore
      }

      try {
        conn.close();
      } catch (SQLException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
  }

  /**
   * Commit the transaction if the connection is not closed.
   * 
   * @param conn the connection
   * @return <code>true</code> if the transaction was committed or the
   *         connection was already closed
   */
  public static boolean safeCommit(Connection conn) {
    if (conn != null) {
      try {
        if (!conn.isClosed()) {
          conn.commit();
        }
        return true;
      } catch (SQLException ex) {
      }
    }
    return false;
  }

  /**
   * Add AUs to list of pending AUs to reindex.
   * 
   * @param conn the connection
   * @param aus the AUs to add
   * @throws SQLException of unable to add AUs to pending AUs
   */
  private void addToPendingAus(Connection conn, Collection<ArchivalUnit> aus)
      throws SQLException {
    // prepare statement for inserting multiple AUs
    // (should this be saved forever?)
    PreparedStatement insertPendingAu = conn.prepareStatement(
      "insert into " + PENDINGAUS_TABLE + " values (?,?)");
    PreparedStatement selectPendingAu = conn.prepareStatement(
        "select * from " + PENDINGAUS_TABLE + " where " 
      + PLUGIN_ID_FIELD + " = ? and " + AU_KEY_FIELD + " = ?");

    log.debug2("number of pending aus to add: " + aus.size());

    // add an AU to the list of pending AUs
    for (ArchivalUnit au : aus) {
      // only add for extraction iff it has article metadata
      if (!hasArticleMetadata(au)) {
        log.debug3(  "not adding au " + au.getName() 
                   + " to pending list because it has no metadata");
      } else {
        String auid = au.getAuId();
        String pluginId = PluginManager.pluginIdFromAuId(auid);
        String auKey = PluginManager.auKeyFromAuId(auid);

        // only insert if entry does not exist
        selectPendingAu.setString(1, pluginId);
        selectPendingAu.setString(2, auKey);
        if (!selectPendingAu.executeQuery().next()) {
          log.debug3(  "adding au " + au.getName() + " to pending list");
          insertPendingAu.setString(1, pluginId);
          insertPendingAu.setString(2, auKey);
          insertPendingAu.addBatch();
        } else {
          log.debug3("Not adding au " + au.getName()
                     + " to pending list becuase it is already on the list");
        }
      }
    }
    insertPendingAu.executeBatch();
  }

  /**
   * Remove an AU from the pending Aus table.
   * 
   * @param conn the connection
   * @param au the pending AU
   * @throws SQLException if unable to delete pending AU
   */
  private void removeFromPendingAus(Connection conn, ArchivalUnit au)
      throws SQLException {
    PreparedStatement deletePendingAu = conn.prepareStatement(
    	  "delete from "
        + PENDINGAUS_TABLE + " where " + PLUGIN_ID_FIELD + " = ? and "
        + AU_KEY_FIELD + " = ?");
    String auid = au.getAuId();
    String pluginId = PluginManager.pluginIdFromAuId(auid);
    String auKey = PluginManager.auKeyFromAuId(auid);

    deletePendingAu.setString(1, pluginId);
    deletePendingAu.setString(2, auKey);
    deletePendingAu.execute();
  }
}