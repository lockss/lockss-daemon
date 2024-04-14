/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.db.*;
import org.lockss.plugin.*;
import org.lockss.servlet.MigrateContent;
import org.lockss.state.AuState;
import org.lockss.state.AuState.MigrationState;
import org.lockss.util.*;
import org.lockss.config.*;

/** Manages V2AuMover instances and reports status to MigrateContent
 * servlet */
public class MigrationManager extends BaseLockssDaemonManager
  implements ConfigurableManager  {

  protected static Logger log = Logger.getLogger("MigrationManager");

  public static final String PREFIX = Configuration.PREFIX + "v2.migrate.";

  static final String STATUS_RUNNING = "running";
  static final String STATUS_ACTIVE_LIST = "active_list";
//   static final String STATUS_FINISHED_LIST = "finished_list";
  static final String STATUS_FINISHED_PAGE = "finished_page";
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
    return !isMigrationInDebugMode && isInMigrationMode && !isDeleteMigratedAus;
  }

  public boolean isTargetPostgres() {
    return DbManagerSql.isTypePostgresql(dsClassName);
  }

  public boolean isSkipDbCopy() {
    return !isTargetPostgres();
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
      if (isMigrationInDebugMode()) {
        log.debug("Not setting inMigrationMode because in debug mode");
        return;
      }
    }
    Configuration mCfg = cfgMgr.newConfiguration();
    mCfg.put(MigrationManager.PARAM_IS_IN_MIGRATION_MODE,
             String.valueOf(inMigrationMode));
    cfgMgr.modifyCacheConfigFile(mCfg,
        ConfigManager.CONFIG_FILE_MIGRATION, CONFIG_FILE_MIGRATION_HEADER);
  }

  public void setIsDbMoved(boolean isDbMoved) throws IOException {
    if (isMigrationInDebugMode()) {
      log.debug("Not setting isDbMoved becuae in debug mode");
      return;
    }
    Configuration mCfg = cfgMgr.newConfiguration();
    mCfg.put(MigrationManager.PARAM_IS_DB_MOVED, String.valueOf(isDbMoved));
    cfgMgr.modifyCacheConfigFile(mCfg,
        ConfigManager.CONFIG_FILE_MIGRATION, CONFIG_FILE_MIGRATION_HEADER);
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
      stat.put(STATUS_FINISHED_PAGE, mover.getFinishedStatusPage(index, size));
    }
    return stat;
  }

  public boolean isRunning() {
    return mover != null && mover.isRunning();
  }

  public synchronized void startRunner(List<V2AuMover.Args> args) throws IOException {
    if (isRunning()) {
      throw new IOException("Migration is already running, can't start a new one");
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
    CopyAndVerify("Copy and Verify Content", COPY_BIT | VERIFY_BIT),
    VerifyOnly("Verify Content", VERIFY_BIT);

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
