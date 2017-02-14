/*
 * $Id$
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.exporter;

import java.io.File;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Cron;
import org.lockss.util.Logger;

/**
 * Service used to manage the export of metadata item fetch times.
 * 
 * @version 1.0
 */
public class FetchTimeExportManager extends BaseLockssDaemonManager implements
    ConfigurableManager {
  /**
   * Prefix for the export configuration entries.
   */
  public static final String PREFIX = Configuration.PREFIX + "export.";

  /**
   * Indication of whether the fetch time export subsystem should be enabled.
   * <p />
   * Defaults to false. If the fetch time export subsystem is not enabled, no
   * data is collected or exported.
   */
  public static final String PARAM_FETCH_TIME_EXPORT_ENABLED =
      PREFIX + "fetchTimeExportEnabled";

  /**
   * Default value of the fetch time export subsystem operation configuration
   * parameter.
   * <p />
   * <code>false</code> to disable, <code>true</code> to enable.
   */
  public static final boolean DEFAULT_FETCH_TIME_EXPORT_ENABLED = false;

  /**
   * Base directory for export files.
   * <p />
   * Defaults to <code><i>daemon_tmpdir</i>/export</code>.
   */
  public static final String PARAM_EXPORT_BASEDIR_PATH = PREFIX
      + "baseDirectoryPath";

  /**
   * Default value of the name of the base directory for export files
   * configuration parameter.
   */
  public static final String DEFAULT_EXPORT_BASEDIR = "export";

  /**
   * Default full path of the name of the base directory for export files
   * configuration parameter.
   */
  public static final String DEFAULT_EXPORT_BASEDIR_PATH = "<tmpdir>/"
      + DEFAULT_EXPORT_BASEDIR;

  /**
   * Indication of whether the fetch time exporter should be run for the first
   * time on startup, independently of the Cron task.
   * <p />
   * Defaults to false.
   */
  public static final String PARAM_FETCH_TIME_EXPORT_RUN_ON_STARTUP =
      PREFIX + "fetchTimeExportRunOnStartup";

  /**
   * Default value of the fetch time exporter run on startup configuration
   * parameter.
   * <p />
   * <code>false</code> to disable, <code>true</code> to enable.
   */
  public static final boolean DEFAULT_FETCH_TIME_EXPORT_RUN_ON_STARTUP = false;

  private static final Logger log =
      Logger.getLogger(FetchTimeExportManager.class);

  // An indication of whether this object is ready to be used.
  private boolean ready = false;

  // The base directory for the export files.
  private File exportDir = null;

  // An indication of whether the recurring task of exporting fetch data has
  // been scheduled.
  private boolean isExporterTaskScheduled = false;

  /**
   * Starts the FetchTimeExportManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    super.startService();
    resetConfig();

    if (ready && ConfigManager.getCurrentConfig()
	.getBoolean(PARAM_FETCH_TIME_EXPORT_RUN_ON_STARTUP,
	    DEFAULT_FETCH_TIME_EXPORT_RUN_ON_STARTUP)) {
      new FetchTimeExporter(getDaemon()).getCronTask().execute();
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "ready = " + ready);
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

    // Do nothing on daemon startup before the service is started.
    if (!getDaemon().isDaemonInited()) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "ready = " + ready);
      return;
    }

    // Check whether the fetch time export subsystem should be enabled.
    if (configure(newConfig)) {
      // Yes: Check whether the recurring task of exporting fetch data needs to
      // be scheduled.
      if (!isExporterTaskScheduled) {
	// Yes: Schedule the recurring task of exporting fetch data.
	scheduleTask();
      }

      ready = true;
    } else {
      // No.
      ready = false;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "ready = " + ready);
  }

  /**
   * Handles configuration parameters.
   * 
   * @return <code>true</code> if the configuration is enabled and successful,
   *         <code>false</code> otherwise.
   */
  private boolean configure(Configuration config) {
    final String DEBUG_HEADER = "configure(): ";
    // Check whether the fetch time export subsystem should be disabled.
    if (!config.getBoolean(PARAM_FETCH_TIME_EXPORT_ENABLED,
	DEFAULT_FETCH_TIME_EXPORT_ENABLED)) {
      // Yes: Do nothing more.
      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "Export of fetch times is disabled.");
      return false;
    }

    // Specify the configured base directory for the export files.
    exportDir = new File(config.get(PARAM_EXPORT_BASEDIR_PATH,
	getDefaultTempRootDirectory()));
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "exportDir = '"
	+ exportDir.getAbsolutePath() + "'.");

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return true;
  }

  /**
   * Schedules the recurring task of exporting fetch data.
   */
  private void scheduleTask() {
    final String DEBUG_HEADER = "scheduleTask(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    Cron cron = (Cron) LockssDaemon.getManager(LockssDaemon.CRON);
    cron.addTask(new FetchTimeExporter(getDaemon()).getCronTask());
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "FetchTimeExporter task added to cron.");

    isExporterTaskScheduled = true;
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "isExporterTaskScheduled = "
	+ isExporterTaskScheduled);
  }

  /**
   * Provides the base directory for the export files.
   * 
   * @return a File with the base directory for the export files.
   */
  public File getExportDir() {
    return exportDir;
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
}
