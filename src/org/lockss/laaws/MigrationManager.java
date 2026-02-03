/*

Copyright (c) 2000-2025 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.laaws;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.db.*;
import org.lockss.plugin.*;
import org.lockss.servlet.MigrateContent;
import org.lockss.servlet.LoginForm;
import org.lockss.state.AuState;
import org.lockss.state.AuState.MigrationState;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.config.*;

/** Manages V2AuMover instances and reports status to MigrateContent
 * servlet.<br>
 *
 * Migration is controlled by several modes and state vars. some
 * reflect user choices, some the current state.  All but one are
 * persistent, stored as config params in {@value
 * org.lockss.config.ConfigManager#CONFIG_FILE_MIGRATION} <ul>
 *
 * <li><b>isInMigrationMode</b> - Enables permanent migration
 * features: forwarding LCAP & content access traffic, deactivating or
 * deleting (if enabled) migrated AUs.  Set they when a migration
 * operation is started, unless in dry run or debug mode.</li>
 *
 * <li><b>isDeleteMigratedAus</b> - If true, AUs will be deleted & the
 * space reclaimed after they are copied to V2.</li>
 *
 * <li><b>isRunning</b> - true when the migrator is actively copying
 * data</li>
 *
 * <li><b>dryRunEnabled</b> - If set, migration will copy all data,
 * including system settings, accounts, misc config, and content, but
 * V1 won't enter migration mode so forwarding won't take place, and
 * all AUs will remain live in V1 and won't be deleted or
 * deactivated</li>
 *
 * <li><b>isMigrationInDebugMode</b> - Enables additional UI buttons,
 * allows combinations of actions and state that should normally be
 * prohibited, prevent AUs from being deleted. </li>
 */
public class MigrationManager extends BaseLockssDaemonManager
  implements ConfigurableManager  {
  protected static Logger log = Logger.getLogger("MigrationManager");

  public static final String PREFIX = Configuration.PREFIX + "v2.migrate.";

  static final String STATUS_RUNNING = "running";
  static final String STATUS_ACTIVE_LIST = "active_list";
//   static final String STATUS_FINISHED_LIST = "finished_list";
  static final String STATUS_FINISHED_PAGE = "finished_page";
  static final String STATUS_FINISHED_INDEX = "finished_index";
  static final String STATUS_FINISHED_COUNT = "finished_count";
  static final String STATUS_START_TIME = "start_time";
  static final String STATUS_STATUS = "status_list";
  static final String STATUS_INSTRUMENTS = "instrument_list";
  static final String STATUS_ERRORS = "errors";
  static final String STATUS_PROGRESS = "progress";

  public static final String PARAM_DRY_RUN_ENABLED = PREFIX + "dryRunEnabled";
  public static final boolean DEFAULT_DRY_RUN_ENABLED = false;
  public static final String PARAM_IS_MIGRATOR_CONFIGURED = PREFIX + "isConfigured";
  public static final boolean DEFAULT_IS_MIGRATOR_CONFIGURED = false;
  public static final String PARAM_DEBUG_MODE = PREFIX + "debug";
  public static final boolean DEFAULT_DEBUG_MODE = false;

  public static final String PARAM_IS_IN_MIGRATION_MODE =
    PREFIX + "inMigrationMode";
  public static final boolean DEFAULT_IS_IN_MIGRATION_MODE = false;

  public static final String PARAM_IS_DB_MOVED = PREFIX + "isDbMoved";
  public static final boolean DEFAULT_IS_DB_MOVED = false;

  public static final String CONFIG_FILE_MIGRATION_HEADER = "Migration configuration";

  private V2AuMover mover;
  private Runner runner;
  LockssUrlConnectionPool connectionPool;
  private String idleError;
  private long startTime = 0;

  boolean isDryRun;
  boolean isInMigrationMode;
  boolean isMigrationInDebugMode;
  boolean isDeleteMigratedAus;
  String dsClassName;;

  ConfigManager cfgMgr;
  PluginManager pluginMgr;

  public void startService() {
    super.startService();
    cfgMgr = getDaemon().getConfigManager();
    pluginMgr = getDaemon().getPluginManager();
    connectionPool = new LockssUrlConnectionPool();
    connectionPool.setConnectTimeout(15000);
  }

  public void stopService() {
    super.stopService();
  }

  public boolean isDryRun() {
    return isDryRun;
  }

  public boolean isInMigrationMode() {
    return isInMigrationMode;
  }

  public boolean isMigrationInDebugMode() {
    return isMigrationInDebugMode;
  }

  public boolean isDeleteMigratedAus() {
    return !isMigrationInDebugMode && isInMigrationMode && isDeleteMigratedAus;
  }

  public boolean isDeactivateMigratedAus() {
    return isInMigrationMode && !isDeleteMigratedAus;
  }

  public boolean isSkipDbCopy() {
    return false;
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      V2AuMover m = mover;
      if (m != null) {
        m.setConfig(config, oldConfig, changedKeys);
      }

      isDryRun = config.getBoolean(
          PARAM_DRY_RUN_ENABLED, DEFAULT_DRY_RUN_ENABLED);
      isInMigrationMode =
          config.getBoolean(PARAM_IS_IN_MIGRATION_MODE,
                            DEFAULT_IS_IN_MIGRATION_MODE);
      isMigrationInDebugMode =
          config.getBoolean(PARAM_DEBUG_MODE, DEFAULT_DEBUG_MODE);
      isDeleteMigratedAus = config.getBoolean(
          MigrateContent.PARAM_DELETE_AFTER_MIGRATION,
          MigrateContent.DEFAULT_DELETE_AFTER_MIGRATION);
      dsClassName = config.get(DbManager.PARAM_DATASOURCE_CLASSNAME);

      if (isDryRun && isDeleteMigratedAus) {
        log.warning("isDryRun is true but isDeleteMigrateAus is true; setting it to false");
        isDeleteMigratedAus = false;
      }
    }
  }

  public void setInMigrationMode(boolean inMigrationMode) throws IOException {
    if (inMigrationMode) {
      if (isDryRun) {
        log.debug("Not setting inMigrationMode because doing a dry run");
        return;
      }
    }
    Configuration mCfg = cfgMgr.newConfiguration();
    mCfg.put(MigrationManager.PARAM_IS_IN_MIGRATION_MODE,
             String.valueOf(inMigrationMode));
    updateMigrationConfigFile(mCfg);
    this.isInMigrationMode = inMigrationMode;
  }

  public void setIsDbMoved(boolean isDbMoved) throws IOException {
    if (isDryRun()) {
      log.debug("Not setting isDbMoved in dry run mode");
      return;
    }
    if (isMigrationInDebugMode()) {
      log.debug("Not setting isDbMoved because in debug mode");
      return;
    }
//     if (isMigrationInDebugMode()) {
//       log.debug("Not setting isDbMoved because in debug mode");
//       return;
//     }
    Configuration mCfg = cfgMgr.newConfiguration();
    mCfg.put(MigrationManager.PARAM_IS_DB_MOVED, String.valueOf(isDbMoved));
    updateMigrationConfigFile(mCfg);
  }

  /**
   * Writes the migration config File ({@link
   * ConfigManager#CONFIG_FILE_MIGRATION}), triggers and waits for a
   * config relead.  If, after the reload, the DB datasource has
   * changed to point a different DB (i.e., the V2 DB), restarts
   * DbManager to establish a new DB connection
   *
   * @param mCfg A {@link Configuration} containing the migration configuration.
   * @throws IOException
   */
  public boolean writeMigrationConfigFile(Configuration mCfg)
      throws IOException {
    Configuration wasDatasource = getDataSourceConfig();

    ConfigManager cfgMgr = ConfigManager.getConfigManager();
    cfgMgr.writeCacheConfigFile(mCfg, ConfigManager.CONFIG_FILE_MIGRATION,
        MigrationManager.CONFIG_FILE_MIGRATION_HEADER, true);
    return handleMigrationStateChange(wasDatasource);
  }

  public boolean updateMigrationConfigFile(Configuration mCfg)
      throws IOException {
    Configuration wasDatasource = getDataSourceConfig();

    ConfigManager cfgMgr = ConfigManager.getConfigManager();
    cfgMgr.modifyCacheConfigFile(mCfg, ConfigManager.CONFIG_FILE_MIGRATION,
        MigrationManager.CONFIG_FILE_MIGRATION_HEADER);
    return handleMigrationStateChange(wasDatasource);
  }

  private boolean handleMigrationStateChange(Configuration wasDatasource) {
    cfgMgr.reloadAndWait();
    Configuration isDatasource = getDataSourceConfig();
    // restart DbManager if the datasource config has changed
    if (!isDatasource.equals(wasDatasource)) {
      DbManager dbMgr = getDaemon().getDbManager();
      dbMgr.restartService();
      return true;
    }
    return false;
  }

  public LockssUrlConnectionPool getConnectionPool() {
    return connectionPool;
  }

  private Configuration getDataSourceConfig() {
    Configuration config = ConfigManager.getCurrentConfig();
    if (config == null) {
      return ConfigManager.EMPTY_CONFIGURATION;
    }
    return config.getConfigTree(DbManager.DATASOURCE_ROOT);
  }

  public Map getStatus() {
    Map stat = new HashMap();
    stat.put(STATUS_START_TIME, startTime);
    if (runner == null) {
      stat.put(STATUS_RUNNING, false);
      stat.put(STATUS_FINISHED_COUNT, 0);
      if (idleError != null) {
        stat.put(STATUS_ERRORS, ListUtil.list(idleError));
      } else {
        stat.put(STATUS_ERRORS, Collections.emptyList());
      }
    } else {
      stat.put(STATUS_RUNNING, mover.isRunning());
      stat.put(STATUS_STATUS, mover.getCurrentStatus());
      if (mover.isShowInstrumentation()) {
        stat.put(STATUS_INSTRUMENTS, mover.getInstruments());
      }
      if (!mover.getActiveStatusList().isEmpty()) {
        stat.put(STATUS_ACTIVE_LIST, mover.getActiveStatusList());
      }
      stat.put(STATUS_FINISHED_COUNT, mover.getFinishedStatusCount());

//       if (!mover.getFinishedStatusList().isEmpty()) {
//         stat.put(STATUS_FINISHED_LIST, mover.getFinishedStatusList());
//       }
      List<String> errs = mover.getErrors();
      if (errs != null && !errs.isEmpty()) {
        stat.put(STATUS_ERRORS, errs);
      }
    }
    return stat;
  }

  public Map getFinishedPage(int index, int size) {
    Map stat = new HashMap();
    if (runner != null && idleError == null) {
      stat.put(STATUS_FINISHED_INDEX, index);
      stat.put(STATUS_FINISHED_PAGE, mover.getFinishedStatusPage(index, size));
    }
    return stat;
  }

  public boolean isRunning() {
    return mover != null && mover.isRunning();
  }

  public synchronized void startRunner(List<V2AuMover.Args> args)
      throws IOException, IllegalStateException {
    if (isRunning()) {
      throw new IllegalStateException("Migration is already running, can't start a new one");
    }
    startTime = TimeBase.nowMs();
    mover = new V2AuMover();
    runner = new Runner(args);
    log.debug("Starting runner: " + args);
    new Thread(runner).start();
  }

  public synchronized void abortCopy() throws IOException {
    if (!isRunning()) {
      throw new IllegalStateException("Not running");
    }
    mover.abortCopy();
  }

  public void resetAllMigrationState()
      throws IllegalStateException, IOException{
    if (isRunning()) {
      throw new IllegalStateException("Migration state cannot be reset while migration is running.  Please abort the current operation first.");
    }
    for (ArchivalUnit au : pluginMgr.getAllAus()) {
      AuState aus = AuUtil.getAuState(au);
      if (aus != null) {
        aus.setMigrationState(MigrationState.NotStarted);
      }
    }
    setIsDbMoved(false);
    setInMigrationMode(false);
  }

  /**
   * Fetches and returns the Configuration Status table of a remote machine. The set of
   * parameters returned by this method does not reflect the full set of parameters on
   * the remote machine since, for example, passwords are not included.
   *
   * @return A {@link Properties} containing the configuration from the remote machine.
   * @throws IOException Thrown if there were network errors, or if the server response was
   * not a 200.
   */
  // DBMover.copyDerbyDb() relies on this having run and supplied
  // credentials, before it runs.
  public Configuration getConfigFromMigrationTarget(String hostname, int cfgUiPort,
                                                    String userName, String userPass)
      throws IOException {
    URL cfgStatUrl = new URL("http", hostname, cfgUiPort,
        "/DaemonStatus?table=ConfigStatus&output=csv");
    log.debug("V2 config GET url: " + cfgStatUrl.toString());


    LockssUrlConnection conn = UrlUtil.openConnection(cfgStatUrl.toString(),
                                                      connectionPool);
    conn.setCredentials(userName, userPass);
    conn.setUserAgent("lockss");
    conn.execute();
    conn = UrlUtil.handleLoginForm(conn, userName, userPass, connectionPool);

    if (conn.getResponseCode() == 200) {
      try (InputStream csvInput = conn.getResponseInputStream()) {
        Properties props = propsFromCsv(csvInput);
        log.debug("Read " + props.size() + " props from target");
        return ConfigManager.fromProperties(props);
      }
    }

    // 403 response message (at least) from daemon comes back
    // URL-encoded.  Running decoder here seems wrong but won't hurt
    // much on a string that hasn't been encoded
    throw new IOException("Unexpected response from migration target: " +
                          conn.getResponseCode() + ": " +
                          URLDecoder.decode(conn.getResponseMessage(),"UTF-8"));
  }

  public boolean isTargetInMigrationMode(String hostname, int cfgUiPort,
                                         String userName, String userPass) {
    try {
      Configuration v2cfg =
        getConfigFromMigrationTarget(hostname, cfgUiPort, userName, userPass);
      return isTargetInMigrationMode(v2cfg);
    } catch (IOException e) {
      log.error("Can't retrieve target configuration", e);
      return false;
    }
  }

  public boolean isTargetInMigrationMode(Configuration v2cfg) {
    return v2cfg.getBoolean(MigrationConstants.V2_PARAM_IS_IN_MIGRATION_MODE,
                            MigrationConstants.V2_DEFAULT_IS_IN_MIGRATION_MODE);
  }

  /**
   * Fetches the Platform Status page of a remote machine and returns its current
   * working directory.
   * @return A {@link String} containing the remote machine's current working directory.
   * @throws IOException Thrown if there were network errors, or if the server response
   * was not a 200.
   */
  public String getCwdOfMigrationTarget(String hostname, int cfgUiPort,
                                        String userName, String userPass)
      throws IOException {
    URL cfgStatUrl = new URL("http", hostname, cfgUiPort,
        "/DaemonStatus?table=PlatformStatus&output=xml");
    log.debug("V2 plat config GET url: " + cfgStatUrl.toString());

    LockssUrlConnection conn = UrlUtil.openConnection(cfgStatUrl.toString());
    conn.setCredentials(userName, userPass);
    conn.setUserAgent("lockss");
    conn.execute();

    if (conn.getResponseCode() == 200) {
      try (InputStream xmlInput = conn.getResponseInputStream()) {
        // FIXME: The factory and XPath expression could be constants
        InputSource inputSource = new InputSource(xmlInput);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputSource);
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression cwdExp = xpath.compile("/table/summaryinfo/title[text()='Cwd']/following-sibling::value");
        return XPathUtil.evaluateString(cwdExp, doc);
      } catch (Throwable t) {
        throw new IOException("Error parsing platform status XML", t);
      }
    }

    throw new IOException(
        "Unexpected response from migration target: " + conn.getResponseCode());
  }

  /**
   * Transforms an {@link InputStream} containing the CSV output from a Configuration Status Table
   * into a {@link Properties}.
   *
   * @param csvStream
   * @return
   * @throws IOException
   */
  public static Properties propsFromCsv(InputStream csvStream) throws IOException {
    Properties result = new Properties();
    InputStreamReader csvReader = new InputStreamReader(csvStream);
    CSVParser csvParser = new CSVParser(csvReader, CSVFormat.DEFAULT.withHeader("Name", "Value"));
    for (CSVRecord record : csvParser) {
      try {
        String key = record.get("Name");
        String val = record.get("Value");
        result.put(key, val);
      } catch (IllegalArgumentException e) {
        log.warning("Malformed CSV record: " + record);
      }
    }
    log.debug("targetprops: " + result);
    return result;
  }

  public class Runner extends LockssRunnable {
    List<V2AuMover.Args> args;

    public Runner(List<V2AuMover.Args> args) {
      super("V2AuMover");
      this.args = args;
    }

    public void lockssRun() {
      idleError = null;
      try {
        if (args.size() > 0) {
          log.debug("Starting mover");
          mover.executeRequests(args);
          log.debug("Mover returned");
        }
      } catch (Exception e) {
        log.error("V2AuMover failed to start", e);
        idleError = "V2AuMover failed to start: " + e;
        runner = null;
        mover = null;
      }
    }
  }

  private static final int COPY_BIT = 1;
  private static final int VERIFY_BIT = 2;

  public enum OpType {
    CopyDatabase("Copy Metadata & Subscription Databases", COPY_BIT),
    CopyConfig("Copy Misc. Config", COPY_BIT),
    CopySystemSettings("Copy System Settings", COPY_BIT),
    CopyOnly("Copy Content", COPY_BIT),
    CopyAndVerify("Copy and Verify", COPY_BIT | VERIFY_BIT),
    VerifyOnly("Verify Only", VERIFY_BIT);

    private String label;
    private int bits;

    OpType(String label, int bits) {
      this.label = label;
      this.bits = bits;
    }

    public boolean isCopy() {
      return (bits & COPY_BIT) != 0;
    }

    public boolean isVerify() {
      return (bits & VERIFY_BIT) != 0;
    }

    public boolean isCopyOnly() {
      return (bits == COPY_BIT);
    }

    public boolean isVerifyOnly() {
      return (bits == VERIFY_BIT);
    }


    public String toString() {
      return label;
    }
  }

  public static class UnsupportedConfigurationException extends Exception {
    public UnsupportedConfigurationException(String message) {
      super(message);
    }

    public UnsupportedConfigurationException(Throwable cause) {
      super(cause);
    }

    public UnsupportedConfigurationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
