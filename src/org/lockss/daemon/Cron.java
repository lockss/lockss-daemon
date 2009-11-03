/*
 * $Id: Cron.java,v 1.9.14.1 2009-11-03 23:44:51 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.util.SerializationException.FileNotFound;
import org.lockss.remote.*;
import org.lockss.account.*;

/** A rudimentary cron facility.  Tasks are added programmatically, checked
 * at discreet interval (default one hour) to see if time to run.  Last run
 * times are stored persistently.
 */
public class Cron
  extends BaseLockssDaemonManager implements ConfigurableManager  {

  protected static Logger log = Logger.getLogger("Cron");

  static final String PREFIX = Configuration.PREFIX + "cron.";

  /** File in config dir where last-run info is kept */
  public static final String CONFIG_FILE_CRON_STATE = "cronstate.xml";

  /** Enable Cron facility */
  static final String PARAM_ENABLED = PREFIX + "enabled";
  static final boolean DEFAULT_ENABLED = true;

  /** Interval at which Cron wakes up to check for runnable tasks */
  static final String PARAM_SLEEP = PREFIX + "sleep";
  static final long DEFAULT_SLEEP = Constants.HOUR;

  private File cronStateFile = null;
  private Cron.State state;
  private Collection tasks = new ArrayList();
  private boolean enabled = false;
  private long interval = DEFAULT_SLEEP;
  private TimerQueue.Request req;

  public void startService() {
    super.startService();
    ConfigManager configMgr = getDaemon().getConfigManager();
    installTasks();
    cronStateFile = configMgr.getCacheConfigFile(CONFIG_FILE_CRON_STATE);
    log.debug2("State file: " + cronStateFile);
    loadState(cronStateFile);
    resetConfig();
  }

  public synchronized void stopService() {
    disable();
    super.stopService();
  }

  public synchronized void setConfig(Configuration config,
                                     Configuration prevConfig,
                                     Configuration.Differences changedKeys) {

    if (!getDaemon().isDaemonInited()) {
      return;
    }

    boolean doEnable = config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED);
    if (changedKeys.contains(PREFIX)) {
      interval = config.getTimeInterval(PARAM_SLEEP, DEFAULT_SLEEP);
    }
    if (doEnable != enabled) {
      if (doEnable) {
        enable();
      } else {
        disable();
      }
      enabled = doEnable;
    }
  }

  /** Install standard tasks */
  void installTasks() {
    addTask(new MailBackupFile(getDaemon()));
    addTask(new SendPasswordReminder(getDaemon()));
  }

  /** Add a task */
  public void addTask(Cron.Task task) {
    tasks.add(task);
  }

  /** Return state object containing last-run times; mostly for testing */
  Cron.State getState() {
    return state;
  }

  void loadState(File file) {
    try {
      state = (Cron.State)makeObjectSerializer().deserialize(file);
    } catch (FileNotFound fnf) {
      log.info("No cron state to load, creating a new one");
      // Default value
      state = new Cron.State();
    } catch (Exception exc) {
      log.warning("Error loading cron state, creating a new one", exc);
      // Default value
      state = new Cron.State();
    }
  }

  void storeState(File file) {
    try {
      if (log.isDebug2()) log.debug2("Storing in " + file + ": " + state);
      makeObjectSerializer().serialize(file, state);
    } catch (Exception e) {
      log.error("Couldn't store cron state, disabling cron: ", e);
      disable();
      // XXX alert
    }
  }

  private ObjectSerializer makeObjectSerializer() {
    return new XStreamSerializer();
  }

  private synchronized void enable() {
    stopRunning();                    // First, ensure no current timer req
    enabled = true;
    log.info("Enabling");
    schedNext();
  }

  private synchronized void disable() {
    if (enabled) {
      log.info("Disabling");
    }
    stopRunning();
  }

  private void stopRunning() {
    enabled = false;
    if (req != null) {
      TimerQueue.cancel(req);
    }
  }

  void schedNext() {
    req = TimerQueue.schedule(Deadline.in(interval), timerCallback, null);
  }

  // TimerQueue callback.  Not inline because don't need a new instance
  // each time.
  private TimerQueue.Callback timerCallback =
    new TimerQueue.Callback() {
      public void timerExpired(Object cookie) {
        checkTasks();
      }
      public String toString() {
        return "Cron";
      }
    };

  /** Run any tasks that need to be run.  Probably should eventually run
   * them in another thread */
  private synchronized void checkTasks() {
    log.debug3("check");
    req = null;
    if (tasks != null && !tasks.isEmpty()) {
      long now = TimeBase.nowMs();
      boolean needStore = false;
      for (Iterator iter = tasks.iterator(); iter.hasNext(); ) {
        Cron.Task task = (Cron.Task)iter.next();
        if (task.nextTime(state.getLastTime(task.getId())) <= now) {
          if (executeTask(task)) {
            state.setLastTime(task.getId(), now);
            needStore = true;
          }
        }
      }
      if (needStore) {
        storeState(cronStateFile);
      }
    }
    if (enabled) {
      schedNext();
    }
  }

  private boolean executeTask(Task task) {
    try {
      return task.execute();
    } catch (Exception e) {
      try {
        log.error("Error executing task " + task, e);
      } catch (Exception e2) {
        // in case task.toString() throws
        log.error("Error executing task", e);
      }
    }
    return true;
  }

  // return top of next hour
  public static long nextHour(long lastTime) {
    return ((lastTime + Constants.HOUR) / Constants.HOUR) * Constants.HOUR;
  }

  // return midnight the next day
  public static long nextDay(long lastTime) {
    Calendar cal = Calendar.getInstance(Constants.DEFAULT_TIMEZONE,
                                        Locale.US);
    cal.setTimeInMillis(lastTime);
    cal.add(Calendar.DAY_OF_WEEK, 1);
    cal.set(Calendar.HOUR, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    return cal.getTimeInMillis();
  }

  // return the first day of the next week
  public static long nextWeek(long lastTime) {
    int day = Calendar.MONDAY;
    // calculations below are dependent on calendar's firstDayOfWeek,
    // etc. values, which vary in different locales.  Ensure we're using
    // expected Locale
    Calendar cal = Calendar.getInstance(Constants.DEFAULT_TIMEZONE,
                                        Locale.US);
    cal.setTimeInMillis(lastTime);
    if (cal.get(Calendar.DAY_OF_WEEK) >= day) {
      cal.add(Calendar.WEEK_OF_MONTH, 1);
    }
    cal.set(Calendar.DAY_OF_WEEK, day);
    cal.set(Calendar.HOUR, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    return cal.getTimeInMillis();
  }

  // return the first day of the next month
  public static long nextMonth(long lastTime) {
    int day = 1;
    Calendar cal = Calendar.getInstance(Constants.DEFAULT_TIMEZONE,
                                        Locale.US);
    cal.setTimeInMillis(lastTime);
    if (cal.get(Calendar.DAY_OF_MONTH) >= day) {
      cal.add(Calendar.MONTH, 1);
    }
    cal.set(Calendar.DAY_OF_MONTH, day);
    cal.set(Calendar.HOUR, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    return cal.getTimeInMillis();
  }


  /** Interface that cron tasks must implement */
  public interface Task {
    String getId();
    boolean execute();
    long nextTime(long lastTime);
  }

  /** State object to record last-run times */
  static class State implements LockssSerializable {
    int version = 1;
    Map times = new HashMap();

    long getLastTime(String id) {
      Long l = (Long)times.get(id);
      if (l == null) {
        return 0;
      }
      return l.longValue();
    }

    void setLastTime(String id, long time) {
      times.put(id, new Long(time));
    }
  }

  /** Task base */
  abstract static class BaseTask implements Cron.Task {
    protected LockssDaemon daemon;

    BaseTask(LockssDaemon daemon) {
      this.daemon = daemon;
    }

    public long nextTime(long lastTime, String freq) {
      if ("hourly".equalsIgnoreCase(freq)) {
        return nextHour(lastTime);
      } else if ("daily".equalsIgnoreCase(freq)) {
        return nextDay(lastTime);
      } else if ("weekly".equalsIgnoreCase(freq)) {
        return nextWeek(lastTime);
      } else {
        return nextMonth(lastTime);
      }
    }
  }

  /** Cron.Task to periodically mail back file to cache admin.  Doesn't
   * belong here. */
  static class MailBackupFile extends BaseTask {

    MailBackupFile(LockssDaemon daemon) {
      super(daemon);
    }

    public String getId() {
      return "MailBackup";
    }

    public long nextTime(long lastTime) {
      return nextTime(lastTime,
                      CurrentConfig.getParam(RemoteApi.PARAM_BACKUP_EMAIL_FREQ,
                                             RemoteApi.DEFAULT_BACKUP_EMAIL_FREQ));
    }

    public boolean execute() {
      RemoteApi rmtApi = daemon.getRemoteApi();
      try {
        return rmtApi.sendMailBackup(false);
      } catch (IOException e) {
        log.warning("Failed to mail backup file", e);
      }
      return true;
    }
  }

  /** Cron.Task to periodically send password change reminders to users.
   * Doesn't belong here. */
  static class SendPasswordReminder extends BaseTask {

    SendPasswordReminder(LockssDaemon daemon) {
      super(daemon);
    }

    public String getId() {
      return "SendPasswordReminder";
    }

    public long nextTime(long lastTime) {
      String freq = 
        CurrentConfig.getParam(AccountManager.PARAM_PASSWORD_CHECK_FREQ,
                               AccountManager.DEFAULT_PASSWORD_CHECK_FREQ);
      return nextTime(lastTime, freq);
    }

    public boolean execute() {
      AccountManager mgr = daemon.getAccountManager();
      return mgr.checkPasswordReminders();
    }
  }
}
