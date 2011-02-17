/*
 * $Id: MetadataManager.java,v 1.3.2.1 2011-02-17 02:54:43 pgust Exp $
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
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.ClassUtils;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Configuration.Differences;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractor.Emitter;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuEventHandler;
import org.lockss.plugin.PluginManager;
import org.lockss.scheduler.SchedulableTask;
import org.lockss.scheduler.Schedule;
import org.lockss.scheduler.StepTask;
import org.lockss.scheduler.TaskCallback;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.TimeInterval;


/**
 * This class implements a metadata manager that is responsible for 
 * managing an index of metadata from AUs.
 *
 * @author  Philip Gust
 * @version 1.0
 */
public class MetadataManager
  extends BaseLockssDaemonManager implements ConfigurableManager {

  private static Logger log = Logger.getLogger("MetadataMgr");

  /**
   * Map of running reindexing tasks
   */
  final Map<ArchivalUnit, ReindexingTask> reindexingTasks = 
    new HashMap<ArchivalUnit, ReindexingTask>();

  /**
   * Metadata manager enabled flag
   */
  boolean enabled = false;
  
  /**
   * Determines whether new database was created
   */
  boolean dbIsNew = false;
  
  
  
  /**
   * Maximum number of reindexing tasks
   */
  int maxReindexingTasks = 1;
  
  /**
   * The database data source
   */
  private DataSource dataSource = null;
  
  /**
   * The plugin manager
   */
  private PluginManager pluginMgr = null;

  /**
   * Name of DOI table
   */
  static final String DOI_TABLE_NAME = "DOI";

  /**
   * Name of ISSN table
   */
  static final String ISSN_TABLE_NAME = "ISSN";
  
  /**
   * Name of ISBN table
   */
  static final String ISBN_TABLE_NAME = "ISBN";
  
  /**
   * Name of Metadata table
   */
  static final String METADATA_TABLE_NAME = "Metadata";
  
  /**
   * Name of Pending AUs table
   */
  static final String PENDINGAUS_TABLE_NAME = "PendingAus";

  static public final String PREFIX = "org.lockss.daemon.metadataManager.";
  
  /** Property name of MetadataManager indexing enabled configuration parameter */
  static public final String PARAM_INDEXING_ENABLED = PREFIX+"indexing_enabled";
  /** Default value of MetadataManager indexing enabled configuration parameter */
  static public final boolean DEFAULT_INDEXING_ENABLED = false;
  
  /** Property tree root for datasource properties */
  static public final String PARAM_DATASOURCE_ROOT = PREFIX+"datasource";
  
  /** Property name of maximum concurrent reindexing tasks */
  static public final String PARAM_MAX_REINDEXING_TASKS = PREFIX+"maxReindexingTasks";

  /** Default maximum concurrent reindexing tasks */
  static public final int DEFAULT_MAX_REINDEXING_TASKS = 1;
  
  /** Length of database author field -- enough for maybe first three authors */
  static private final int MAX_AUTHOR_FIELD = 512;
  
  /** Length of database title field */
  static private final int MAX_TITLE_FIELD = 512;
  
  /** Length of access URL field */
  static private final int MAX_ACCESS_URL_FIELD = 4096;
  
  /** Length of au key field */
  static private final int MAX_AU_KEY_FIELD = 512;
  
  /** Length of plugin ID field */
  static private final int MAX_PLUGIN_ID_FIELD = 96;
  
  /**
   * Get the root directory for creating the database files.
   * @return the root directory
   */
  protected String getDbRootDirectory() {
    String rootDir = System.getProperty("user.dir");
    @SuppressWarnings("unchecked")
    List<String> dSpaceList = ConfigManager.getCurrentConfig().getList(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST);
    if (dSpaceList != null && !dSpaceList.isEmpty()) {
      rootDir = dSpaceList.get(0);
    }
    return rootDir;
  }
  
  /**
   * Initialize the manager from the current configuration.
   */
  protected void initializeManager(Configuration config) {
    // already initialized if datasource exists
    if (dataSource != null) {
      return;
    }

    // determine maximum number of concurrent reindexing tasks (0 disables reindexing)
    maxReindexingTasks = Math.max(0, config.getInt(PARAM_MAX_REINDEXING_TASKS, DEFAULT_MAX_REINDEXING_TASKS));

    // get datasource class and properties
    Configuration datasourceConfig = ConfigManager.newConfiguration();
    datasourceConfig.copyFrom( 
      ConfigManager.getCurrentConfig().getConfigTree(PARAM_DATASOURCE_ROOT));
    String dataSourceClassName = datasourceConfig.get("className");
    if (dataSourceClassName != null) {
      // class name not really part of data source definition.
      datasourceConfig.remove("className");
    } else {
      String databaseName = new File(getDbRootDirectory(), "db/MetadataManager").getAbsolutePath();
      // use derby embedded datasource by default
      dataSourceClassName = "org.apache.derby.jdbc.EmbeddedDataSource";
      datasourceConfig.put("databaseName", databaseName);
      datasourceConfig.put("description", "Embeddded JavaDB data source");
      datasourceConfig.put("user", "APP");
      datasourceConfig.put("password","APP");
      datasourceConfig.put("createDatabase", "create");
    }
    Class<?> cls;
    try {
        cls = Class.forName(dataSourceClassName);
    } catch (Throwable ex) {
      log.error("Cannot locate datasource class \"" + dataSourceClassName + "\"", ex);
      return;
    }
    
    // create datasource
    try {
      dataSource = (DataSource)cls.newInstance();
    } catch (ClassCastException ex) {
      log.error("Class not a DataSource \"" + dataSourceClassName + "\"");
      return;
    } catch (Throwable ex) {
      log.error("Cannot create instance of datasource class \"" + dataSourceClassName + "\"", ex);
      return;
    }
    boolean errors = false;
    // initialize datasource properties
    for (String key : datasourceConfig.keySet()) {
      String value = datasourceConfig.get(key);
      try {
        setPropertyByName(dataSource, key, value);
      } catch (Throwable ex) {
        errors = true;
        log.error("Cannot set property \"" + key + "\" for instance of datasource class \"" + dataSourceClassName + "\"", ex);
      }
    }
    if (errors) {
      log.error("Cannot initialize instance of datasource class \"" + dataSourceClassName + "\"");
      dataSource = null;
    } else {
      try {
        // if dataSource is a Derby ClientDataSource, enable remote access
        org.apache.derby.jdbc.ClientDataSource cds = (org.apache.derby.jdbc.ClientDataSource)dataSource;
        InetAddress inetAddr = InetAddress.getByName(cds.getServerName());
        int jdbcPort = cds.getPortNumber();
        org.apache.derby.drda.NetworkServerControl networkServerControl =
          new org.apache.derby.drda.NetworkServerControl(inetAddr, jdbcPort);
        networkServerControl.start(null);
      } catch (ClassCastException ex) {
        // dataSource is not a Derby ClientDataSource
      } catch (Exception ex) {
        log.error("Cannot enable remote access to Derby database");
      }
    }
  }

  /**
   * Set a bean property for an object by name.  Property value
   * string is converted to the appropriate data type.
   * 
   * @param obj the object
   * @param propName the property name
   * @param propValue the property value
   * @throws Throwable if an error occurred.
   */
  private void setPropertyByName(Object obj, String propName, String propValue) throws Throwable {
    String setterName = "set" + propName.substring(0,1).toUpperCase() + propName.substring(1);
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
            log.debug(setterName, ex);
          }
        }
      }
    }
    throw new NoSuchMethodException(obj.getClass().getName() + "." + setterName + "()");
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
   * Write the named table schema to the log.
   * <p>
   * See http://www.roseindia.net/jdbc/Jdbc-meta-data-get-tables.shtml
   * 
   * @param conn the connection
   * @param tableName the table name
   */
  public static void logSchema(Connection conn, String tableName) {
    if (conn != null) {
      try {
        Statement st = conn.createStatement();
  
        String sql = "select * from " + tableName;
        ResultSet rs = st.executeQuery(sql);
        ResultSetMetaData metaData = rs.getMetaData();
  
        int rowCount = metaData.getColumnCount();
  
        log.info("Table Name : " + metaData.getTableName(2));
        log.info("Field  \tsize\tDataType");
  
        for (int i = 0; i < rowCount; i++) {
          log.info(metaData.getColumnName(i + 1) + "  \t");
          log.info(metaData.getColumnDisplaySize(i + 1) + "\t");
          log.info(metaData.getColumnTypeName(i + 1));
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
    "create table " + PENDINGAUS_TABLE_NAME + " (" +
      "plugin_id VARCHAR(64) NOT NULL," + 
      "au_key VARCHAR(128) NOT NULL" + 
    ")",

    "create table " + METADATA_TABLE_NAME + " (" +
      "md_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
      "date VARCHAR(16)," + 
      "volume VARCHAR(16)," + 
      "issue VARCHAR(32)," + 
      "start_page VARCHAR(16)," + 
      "article_title VARCHAR(" + MAX_TITLE_FIELD + ")," + 
      "author VARCHAR(" + MAX_AUTHOR_FIELD + ")," +             // semicolon-separated list 
      "plugin_id VARCHAR(" + MAX_PLUGIN_ID_FIELD + ") NOT NULL," +   // partition by 
      "au_key VARCHAR(" + MAX_AU_KEY_FIELD + ") NOT NULL," + 
      "access_url VARCHAR(" + MAX_ACCESS_URL_FIELD + ") NOT NULL" + 
    ")",
    
    "create table " + DOI_TABLE_NAME + " (" +
      "doi VARCHAR(256) NOT NULL," + 
      "md_id BIGINT NOT NULL REFERENCES " + METADATA_TABLE_NAME + "(md_id) on delete cascade" + 
    ")",

    "create table " + ISBN_TABLE_NAME + " (" +
      "isbn VARCHAR(13) NOT NULL," + 
      "md_id BIGINT NOT NULL REFERENCES " + METADATA_TABLE_NAME + "(md_id) on delete cascade" + 
    ")",

    "create table " + ISSN_TABLE_NAME + " (" +
      "issn VARCHAR(8) NOT NULL," + 
      "md_id BIGINT NOT NULL REFERENCES " + METADATA_TABLE_NAME + "(md_id) on delete cascade" + 
    ")",
  };
  
  /**
   * SQL statements that drop the database schema
   */
  private static final String[] dropSchema = new String[] {
      "drop table " + PENDINGAUS_TABLE_NAME,
      "drop table " + DOI_TABLE_NAME,
      "drop table " + ISSN_TABLE_NAME,
      "drop table " + ISBN_TABLE_NAME,
      "drop table " + METADATA_TABLE_NAME,
    };

  
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
   * Create a new database connection using the datasource, Autocommit is disabled.
   * 
   * @return the new connection
   * @throws SQLException if an error occurred while connecting
   */
  public Connection newConnection() throws SQLException {
    if (dataSource == null) {
      throw new SQLException("No datasource");
    }
    
    Connection  conn = dataSource.getConnection();
    conn.setAutoCommit(false);
    return conn;
  }
  
  /**
   * Restart the Metadata Managaer service by terminating 
   * any running reindexing tasks and then resetting its 
   * database before calling {@link #startServie()}.  
   * <p>
   * This method is only used for testing.
   */
  void restartService() {
    initializeManager(ConfigManager.getCurrentConfig());

    stopReindexing();
    
    Connection conn;
    try {
      conn = newConnection();
    } catch (SQLException ex) {
      log.error("Cannot get database connecction -- service not started");
      return;
    }
    
    // reset database tables
    try {
      // drop schema tables already exist
      if (tableExists(conn, PENDINGAUS_TABLE_NAME)) {
        executeBatch(conn, dropSchema);
      }
      conn.commit();
      conn.close();
    } catch (BatchUpdateException ex) {
      //handle batch update exception
      int[] counts = ex.getUpdateCounts();
      for (int i=0; i < counts.length; i++) {
        log.error("Error in statement "+ i +"(" + counts[i] + "): " + createSchema[i]);
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
   * Ensure as many reindexing tasks as possible are running
   * if manager is enabled.
   * 
   * @param conn the database connection
   * @return the number of reindexing tasks started
   */
  private int startReindexing(Connection conn) { 
    if (!getDaemon().isDaemonInited()) {
      log.debug("daemon not initialized: no reindexing tasks.");
      return 0;
    }

    // don't run if disabled
    if (!enabled) {
      log.debug("metadata manager is disabled: no reindexing tasks.");
      return 0;
    }
    
    synchronized (reindexingTasks) {
      // get list of pending aus to reindex
      List<ArchivalUnit> aus = getAusToReindex(conn, maxReindexingTasks - reindexingTasks.size());
      log.debug("number of au reindexing tasks being started: " + aus.size());
      
      // schedule pending aus
      for (ArchivalUnit au : aus) {
        ArticleMetadataExtractor ae = getArticleMetadataExtractor(au);
        if (ae == null) {
          log.debug("not running reindexing task for au because it nas no metadata extractor: " + au.getName());
        } else {
          log.debug("running reindexing task for au: " + au.getName());
          ReindexingTask task = new ReindexingTask(au, ae);
          reindexingTasks.put(au, task);
          runReindexingTask(task);
        }
      }
      return aus.size();
    }
  }
  
  /**
   * Ensure as many reindexing tasks as possible are running
   * if manager is enabled.
   * 
=   * @return the number of reindexing tasks started
   */
  private int startReindexing() {
    
    Connection  conn = null;
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
  private void stopReindexing()
  {
    log.debug("number of reindexing tasks being stopped: " + reindexingTasks.size());
    
    // quit any running reindexing tasks
    synchronized (reindexingTasks) {
      for (MetadataManager.ReindexingTask task : reindexingTasks.values()) {
        task.cancel();
      }
      reindexingTasks.clear();
    }
  }

  /**
   * Start MetadataManager service
   */
  public void startService() {
    log.debug("Starting MetadataManager");
    
    initializeManager(ConfigManager.getCurrentConfig());
    
    Connection  conn;
    try {
      conn = newConnection();
    } catch (SQLException ex) {
      log.error("Cannot connect to database -- service not started");
      return;
    }
    
    // create schema and initialize tables if schema does not exist
    dbIsNew = !tableExists(conn, PENDINGAUS_TABLE_NAME);
    if (dbIsNew) {
      try {
        executeBatch(conn, createSchema);
      } catch (BatchUpdateException ex) {
        //handle batch update exception
        int[] counts = ex.getUpdateCounts();
        for (int i=0; i < counts.length; i++) {
          log.error("Error in schemea statement "+ i +"(" + counts[i] + "): " + createSchema[i]);
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
    
    if (!safeCommit(conn)) {
      safeClose(conn);
      return;
    }
    safeClose(conn);
    
    
    // get the plugin manager
    pluginMgr = getDaemon().getPluginManager();

    pluginMgr.registerAuEventHandler(new AuEventHandler.Base() {
      public void auContentChanged(ArchivalUnit au, ChangeInfo info) {
        if (info.isComplete()) {
          Connection conn = null;
          try {
            log.debug("Adding changed au to reindex: " + au.getName());
            // add pending AU
            conn = newConnection();
            if (conn == null) {
              log.error("Cannot connect to database -- cannot add changed aus to pending aus");
              return;
            }

            addToPendingAus(conn, Collections.singleton(au));
            conn.commit();
            
            synchronized(reindexingTasks) {
              ReindexingTask task = reindexingTasks.get(au);
              if (task != null) {
                // task cancellation will remove task and schedule next one
                log.debug("Canceling pending reindexing task for au " + au.getName());
                task.cancel();
              } else {
                log.debug("Scheduling reindexing tasks");
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
    MetadataStarter starter = new MetadataStarter(dbIsNew);
    new Thread(starter).start();
  }

  /** 
   * Set enabled state of this manager.
   * 
   * @param enable new enabled state of manager
   */
  synchronized void setEnabled(boolean enable) {
    boolean wasEnabled = enabled;
    enabled = enable;
    log.debug("enabled: " + enabled);

    // start or stop reindexing if initialized
    if (dataSource != null) {
      if (!wasEnabled && enabled) {
        // start reindexing
        startReindexing();
      } else if (wasEnabled && !enabled) {
        // stop any pending reindexing operations
        stopReindexing();
      }
    }
  }

  @Override
  public void setConfig(Configuration config, Configuration prevConfig,
                        Differences changedKeys) {
    boolean doEnable = config.getBoolean(PARAM_INDEXING_ENABLED, DEFAULT_INDEXING_ENABLED);
    setEnabled(doEnable);

  }

  class MetadataStarter extends LockssRunnable {
    boolean initPendingAus;
    public MetadataStarter(boolean initPendingAus) {
      super("MetadataStarter");
      this.initPendingAus = initPendingAus;
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
      
      Connection  conn;
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
          log.error("Cannot add pending AUs table \"" + PENDINGAUS_TABLE_NAME + "\"", ex);
          safeClose(conn);
          return;
        }
      }

      startReindexing(conn);
      safeClose(conn);
    }
  }
  
  static long totalCpuTime = 0;
  static long totalUserTime = 0;
  static long totalClockTime = 0;
  static ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
  static {
    log.debug("current thread CPU time supported? " + tmxb.isCurrentThreadCpuTimeSupported());
    if (tmxb.isCurrentThreadCpuTimeSupported()) {
      tmxb.setThreadCpuTimeEnabled(true);
    }
  }

  /**
   * Get the ArticleMetadataExtractor for the specified au.
   * 
   * @param au the au
   * @return the article metadata extractor
   */
  private ArticleMetadataExtractor getArticleMetadataExtractor(ArchivalUnit au) {
    ArticleMetadataExtractor ae = au.getPlugin().getArticleMetadataExtractor(MetadataTarget.Article, au);
    return ae;
  }
  
  /**
   * This class implements a reindexing task that extracts
   * metadata from all the articles in the specified AU.
   */
  class ReindexingTask extends StepTask {
    ArchivalUnit au;
    ArticleMetadataExtractor  ae;
    Iterator<ArticleFiles> articleIterator = null;
    Connection conn;
    static final int default_steps = 10;

    /**
     * Create a reindexing task for the AU
     * 
     * @param theAu the archival unit
     * @param theAe the article metadata extractor to use
     */
    public ReindexingTask(ArchivalUnit theAu, ArticleMetadataExtractor theAe) { 
      super(new TimeInterval(System.currentTimeMillis(),
                             System.currentTimeMillis() + Constants.HOUR),       /* TimeInterval */
            0,          /* long estimatedDuration */
            null,       /* TaskCallback */
            null);      /* Object cookie */

      this.au = theAu;
      this.ae = theAe;

      // set task callback after construction to ensure instance is initialized
      callback =  new TaskCallback() {
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
            log.debug("Reindexing task starting for au: " + au.getName());
            articleIterator = au.getArticleIterator(MetadataTarget.Article);
            try {
              conn = newConnection();
              removeMetadataForAu(conn, au);
              notifyStartReindexingAu(au);
            } catch (SQLException ex) {
              log.error("Failed to set up to reindex pending au: " + au.getName(), ex);
              cancel();  // cancel task
            }
          } else if (type == Schedule.EventType.FINISH) {
            if (conn != null) {
              boolean success = false;
              try {
                removeFromPendingAus(conn, au);
                
                // if reindexing not complete at this point,
                // roll back current transaction, and try later
                if (articleIterator.hasNext()) {
                  log.debug("Reindexing task did not finished for au " + au.getName());
                  conn.rollback();
                  addToPendingAus(conn, Collections.singleton(au));
                } else {
                  success = true;
                  long elapsedCpuTime = threadCpuTime = startCpuTime;
                  long elapsedUserTime = threadUserTime - startUserTime;
                  long elapsedClockTime = currentClockTime - startClockTime;
                  totalCpuTime += elapsedCpuTime;
                  totalUserTime += elapsedUserTime;
                  totalClockTime += elapsedClockTime;
                  log.debug("Reindexing task finished for au: " + au.getName() + " CPU time: " + elapsedCpuTime/1.0e9 + " (" + totalCpuTime/1.0e9 + "), UserTime: " + elapsedUserTime/1.0e9 + " (" + totalUserTime/1.0e9 + ") Clock time: " + elapsedClockTime/1.0e3 + " (" + totalClockTime/1.0e3 + ")");
                }
                conn.commit();
              } catch (SQLException ex) {
                log.error("Failed to remove pending au: " + au.getName(), ex);
              }

              synchronized (reindexingTasks) {
                reindexingTasks.remove(au);
                notifyFinishReindexingAu(au, success);

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
      final int[] extracted = new int[1];
      int steps = (n <= 0) ? default_steps : n;
      log.debug("step: " + steps + ", has articles: " + articleIterator.hasNext());

      while (   !isFinished() 
          && (extracted[0] <= steps)  
          && articleIterator.hasNext()) {
        ArticleFiles af = articleIterator.next();
        try {
          Emitter emitter = new Emitter() {
            @Override
            public void emitMetadata(ArticleFiles af, ArticleMetadata md) {
              if (md.get(MetadataField.FIELD_ACCESS_URL) == null) {  
                // temporary -- use full text url if not set
                // (should be set by metadata extractor)
                md.put(MetadataField.FIELD_ACCESS_URL, af.getFullTextUrl());
                try {
                  addMetadata(conn, au, md);
                  extracted[0]++;
                } catch (SQLException ex) {
                  // TODO: should extraction be canceled at this point?
                  log.error("Failed to index metadata for CachedURL: " + af.getFullTextUrl(), ex);
                }
              }
            }
          };
          ae.extract(MetadataTarget.OpenURL, af, emitter);
        } catch (IOException ex) {
          log.error("Failed to index metadata for CachedURL: " + af.getFullTextUrl(), ex);
        } catch (PluginException ex) {
          log.error("Failed to index metadata for CachedURL: " + af.getFullTextUrl(), ex);
        }
      }
      
      if (!isFinished()) {
        // finished if all articles handled
        if (!articleIterator.hasNext()) {
          setFinished();
        }
      }
      return extracted[0];
    }
    
    /**
     * Temporary
     * @param evt
     */
    protected void callCallback(Schedule.EventType evt) {
      callback.taskEvent(this, evt);
    }
  }    
  
  /**
   * 
   * Notify listeners that an AU is being reindexed.
   * @param au
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
        ResultSet results = selectPendingAus.executeQuery("select * from PendingAus");
  
        for (int i = 0; (i < maxAus) && results.next(); i++) {
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
      } catch (IllegalStateException ex) {
        log.error(ex.getMessage());
      }
    }
    aus.trimToSize();
    return aus;
  }    

  
  /**
   * Run the specified reindexing task.
   * <p>
   * Temporary implementation runs as a LockssRunnable in a thread
   * rather than using the SchedService.
   * 
   * @param task the reindexing task
   */
  private void runReindexingTask(final ReindexingTask task) {
    /* KLUDGE: temporarily running task in its own thread
     * rather than using SchedService
     */
    LockssRunnable runnable = new LockssRunnable("Reindexing: " + task.au.getName()) {
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
   * Return the author field to store in database. The field is comprised of
   * a semicolon separated list of as many authors as will fit in the database
   * author field.
   * @param md the ArticleMetadata
   * @return the author or <code>null</code> if none specified
   */
  private static String getAuthorField(ArticleMetadata md) {
    String author = null;
    List<String> authors = md.getList(MetadataField.FIELD_AUTHOR);
    // create author field as semicolon-separated list of authors from metadata
    if (authors != null) {
      for (String a : authors) {
        if (author == null) {
          author = a;
        } else {
          // include as many authors as will fit in the field
          if (author.length()+a.length()+1 > MAX_AUTHOR_FIELD) {
            break;
          }
          author += ";" + a; 
        }
      }
    }
    return author;
  }
  
  /**
   * Return the article title field to store in the database. The field
   * is truncated to the size of the article title field.
   * @param md the ArticleMetadata
   * @return the articleTitleField or <code>null</code> if none specified
   */
  private static String getArticleTitleField(ArticleMetadata md) {
    // elide title field
    String articleTitle = md.get(MetadataField.FIELD_ARTICLE_TITLE);
    if ((articleTitle != null) && (articleTitle.length() > MAX_TITLE_FIELD)) {
      articleTitle = articleTitle.substring(0, MAX_TITLE_FIELD);
    }
    return articleTitle;
  }
  
  /**
   * Return the date field to store in the database. The date field
   * can be nearly anything a MetaData extractor chooses to provide,
   * making it a near certainty that this method will be unable to
   * parse it, even with the help of locale information.
   * @param md the ArticleMetadata
   * @return the date field or <code>null</code> if non specified or
   *   one cannot be parsed from the metadata information
   */
  private static String getDateField(ArticleMetadata md) {
    String dateStr = md.get(MetadataField.FIELD_DATE);
    if (dateStr != null) {
      Locale locale = md.getLocale();
      if (locale == null) {
        locale = Locale.getDefault();
      }
      dateStr = new PublicationDate(dateStr, locale).toString();
    }
    return dateStr;
  }
  
  /**
   * Add metadata for this archival unit.
   * 
   * @param conn the connection
   * @param md the metadata
   * @throws SQLException if failed to add rows for metadata
   */
  private static void addMetadata(Connection conn, ArchivalUnit au, ArticleMetadata md) 
    throws SQLException {
    String auid = au.getAuId();
    String pluginId = PluginManager.pluginIdFromAuId(auid);
    String auKey = PluginManager.auKeyFromAuId(auid);
    
    String doi = md.get(MetadataField.FIELD_DOI);
    String isbn = md.get(MetadataField.FIELD_ISBN);
    String issn = md.get(MetadataField.FIELD_ISSN);
    String volume = md.get(MetadataField.FIELD_VOLUME);
    String issue = md.get(MetadataField.FIELD_ISSUE);
    String startPage = md.get(MetadataField.FIELD_START_PAGE);

    // normalize date according to ISO 8601 required for metadata
    String dateStr = getDateField(md);
    String articleTitle = getArticleTitleField(md);
    String author = getAuthorField(md);

    String accessUrl = md.get(MetadataField.FIELD_ACCESS_URL);
    
    // insert common data into metadata table
    PreparedStatement insertMetadata = 
      conn.prepareStatement("insert into " + METADATA_TABLE_NAME + " " +
      		            "values (default,?,?,?,?,?,?,?,?,?)", 
      		            Statement.RETURN_GENERATED_KEYS);
//TODO PJG: Keywords???
    // skip auto-increment key field
    insertMetadata.setString(1, dateStr);
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
    log.debug("added [accessURL:" + accessUrl + ", md_id: " + mdid + ", date: " + dateStr + ", vol: " + volume + ", issue: " + issue + ", page: " + startPage + ", pluginId:" + pluginId + "]");
    
    // insert row for DOI
    if (doi != null) {
      if (doi.toLowerCase().startsWith("doi:")) {
        doi = doi.substring(4);
      }
      /*
      "doi VARCHAR(256) PRIMARY KEY NOT NULL," + 
      "md_id INTEGER NOT NULL," + 
      "plugin_id VARCHAR(96) NOT NULL" +   // partition by 
       */
      PreparedStatement insertDOI = 
        conn.prepareStatement("insert into " + DOI_TABLE_NAME + " " +
                               "values (?,?)");
      insertDOI.setString(1, doi);
      insertDOI.setInt(2, mdid);
      insertDOI.execute();
      log.debug("added [doi:" + doi + ", md_id: " + mdid + ", pluginId:" + pluginId + "]");
    }
    
    // insert row for ISBN
    if (isbn != null) {
      isbn = isbn.replaceAll("-", "");
      PreparedStatement insertISBN = 
        conn.prepareStatement("insert into " + ISBN_TABLE_NAME + " " +
                               "values (?,?)");
      insertISBN.setString(1, isbn);
      insertISBN.setInt(2, mdid);
      insertISBN.execute();
      log.debug("added [isbn:" + isbn + ", md_id: " + mdid + ", pluginId:" + pluginId + "]");
    }

    // insert row for ISSN
    if (issn != null) {
      issn = issn.replaceAll("-", "");
      PreparedStatement insertISSN = 
        conn.prepareStatement("insert into " + ISSN_TABLE_NAME + " " +
                               "values (?,?)");
      insertISSN.setString(1, issn);
      insertISSN.setInt(2, mdid);
      insertISSN.execute();
      log.debug("added [issn:" + issn + ", md_id: " + mdid + ", pluginId:" + pluginId + "]");
    }
  }
   
  /**
   * Close the connection and report an error if cannot close.
   * 
   * @param con the connection
   */
  public static void safeClose(Connection con) {
    if (con != null) {
      try {
        // rollback if not already committed
        con.rollback();
      } catch (SQLException ex) {
        // ignore
      }
      
      try {
        con.close();
      } catch (SQLException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
  }
  
  /**
   * Commit the transaction if the connection is not closed.
   * 
   * @param conn the connection
   * @return <code>true</code> if the transaction was committed
   *   or the connection was already closed
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
    PreparedStatement insertPendingAu = 
      conn.prepareStatement("insert into " + PENDINGAUS_TABLE_NAME + " values (?,?)");
    PreparedStatement selectPendingAu = 
      conn.prepareStatement("select * from PendingAus where plugin_id=? and au_key=?");

    log.debug("number of pending aus to add: " + aus.size());
    
    // add an AU to the list of pending AUs
    for (ArchivalUnit au : aus) {
      // only add for extraction of there is a 
      if (getArticleMetadataExtractor(au) == null) {
        log.debug("not adding au to pending because it has no metadata extractor: " + au.getName());
      } else {
        String auid = au.getAuId();
        String pluginId = PluginManager.pluginIdFromAuId(auid);
        String auKey = PluginManager.auKeyFromAuId(auid);
  
        // only insert if entry does not exist
        selectPendingAu.setString(1, pluginId);
        selectPendingAu.setString(2, auKey);
        if (!selectPendingAu.executeQuery().next()) {
          log.debug("adding au to pending: " + pluginId + "?" + auKey);
          insertPendingAu.setString(1, pluginId);
          insertPendingAu.setString(2, auKey);
          insertPendingAu.addBatch();
        } else {
          log.debug("au already pending: " + pluginId + "?" + auKey);
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
    PreparedStatement deletePendingAu = 
      conn.prepareStatement("delete from " + PENDINGAUS_TABLE_NAME + " where plugin_id=? and au_key=?");
    String auid = au.getAuId();
    String pluginId = PluginManager.pluginIdFromAuId(auid);
    String auKey = PluginManager.auKeyFromAuId(auid);
    
    deletePendingAu.setString(1, pluginId);
    deletePendingAu.setString(2, auKey);
    deletePendingAu.execute();
  }

  /**
   * Remove an AU from the pending Aus table.
   *
   * @param conn the connection
   * @param au the pending AU
   * @throws SQLException if unable to delete pending AU
   */
  private void removeMetadataForAu(Connection conn, ArchivalUnit au) 
      throws SQLException {
    PreparedStatement deletePendingAu = 
      conn.prepareStatement("delete from " + METADATA_TABLE_NAME + " where plugin_id=? and au_key=?");
    String auid = au.getAuId();
    String pluginId = PluginManager.pluginIdFromAuId(auid);
    String auKey = PluginManager.auKeyFromAuId(auid);
    
    deletePendingAu.setString(1, pluginId);
    deletePendingAu.setString(2, auKey);
    deletePendingAu.execute();
  }

  static public void main(String[] args) {
    MetadataManager mdm = new MetadataManager();
    mdm.startService();
  }
}
