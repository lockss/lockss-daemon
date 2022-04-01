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
import org.lockss.util.*;
import org.lockss.config.*;

/** Manages V2AuMover instances and reports status to MigrateContent
 * servlet */
public class MigrationManager extends BaseLockssManager
  implements ConfigurableManager  {

  protected static Logger log = Logger.getLogger("MigrationManager");

  public static final String PREFIX = Configuration.PREFIX + "foo.";

  public static final String PARAM_CACHE_MAX_MB =
    PREFIX + "cacheMaxMb";
  public static final long DEFAULT_CACHE_MAX_MB = 100;

  static final String STATUS_RUNNING = "running";
  static final String STATUS_ACTIVE_LIST = "active_list";
  static final String STATUS_FINISHED_LIST = "finished_list";
  static final String STATUS_STATUS = "status";
  static final String STATUS_INSTRUMENTS = "instrument_list";
  static final String STATUS_ERRORS = "errors";
  static final String STATUS_PROGRESS = "progress";

  private V2AuMover mover;
  private Runner runner;
  private String idleError;

  public void startService() {
    super.startService();
  }

  public void stopService() {
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
    }
  }

  public Map getStatus() {
    Map stat = new HashMap();
    if (runner == null) {
      stat.put(STATUS_RUNNING, false);
      if (idleError != null) {
        stat.put(STATUS_ERRORS, ListUtil.list(idleError));
      }
    } else {
      stat.put(STATUS_RUNNING, mover.isRunning());
      stat.put(STATUS_STATUS, mover.getCurrentStatus());
      stat.put(STATUS_INSTRUMENTS, mover.getInstruments());
      if (!mover.getActiveStatusList().isEmpty()) {
        stat.put(STATUS_ACTIVE_LIST, mover.getActiveStatusList());
      }
      if (!mover.getFinishedStatusList().isEmpty()) {
        stat.put(STATUS_FINISHED_LIST, mover.getFinishedStatusList());
      }
      List<String> errs = mover.getErrors();
      if (errs != null && !errs.isEmpty()) {
        stat.put(STATUS_ERRORS, errs);
      }
    }
    return stat;
  }

  public synchronized void startRunner(V2AuMover.Args args) throws IOException {
    if (mover != null && mover.isRunning()) {
      throw new IOException("Migration is already running, can't start a new one");
    }
    mover = new V2AuMover();
    runner = new Runner(args);
    log.debug("Starting runner: " + args);
    new Thread(runner).start();
  }

  public class Runner extends LockssRunnable {
    V2AuMover.Args args;

    public Runner(V2AuMover.Args args) {
      super("V2AuMover");
      this.args = args;
    }

    public void lockssRun() {
      idleError = null;
      try {
        log.debug("Starting mover");
        mover.executeRequest(args);
        log.debug("Mover returned");
      } catch (Exception e) {
        log.error("V2AuMover failed to start", e);
        idleError = "V2AuMover failed to start: " + e;
        runner = null;
        mover = null;
      }
    }
  }

}
