/*
 * $Id$
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
import java.util.concurrent.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.util.SerializationException.FileNotFound;
import org.lockss.remote.*;
import org.lockss.account.*;

/** A rudimentary cron facility.  Tasks are added programmatically, checked
 * at discreet interval (default one hour) and run in a thread pool.  Last
 * run times are stored persistently.
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

  private static final String THREADPOOL_PREFIX = PREFIX + "threadPool.";

  /** Max number of threads running Cron tasks */
  static final String PARAM_THREADPOOL_SIZE =
    THREADPOOL_PREFIX + "size";
  static final int DEFAULT_THREADPOOL_SIZE = 2;

  /** Priority at which Cron tasks should run.  Changing this does not
   * alter the priority of already running tasks. */
  static final String PARAM_THREADPOOL_PRIORITY =
    THREADPOOL_PREFIX + "priority";
  static final int DEFAULT_THREADPOOL_PRIORITY = 5;

  /** Duration after which idle threads will be terminated..  -1 = never */
  public static final String PARAM_THREADPOOL_KEEPALIVE =
    THREADPOOL_PREFIX + "keepAlive";
  static final long DEFAULT_THREADPOOL_KEEPALIVE = 1 * Constants.MINUTE;

  private File cronStateFile = null;
  private Cron.State state;
  private Collection<Task> tasks = new ArrayList();
  private Set<String> runningIds =
    Collections.synchronizedSet(new HashSet<String>());

  private boolean enabled = false;
  private long interval = DEFAULT_SLEEP;
  private TimerQueue.Request req;
  private ThreadPoolExecutor executor;
  private int taskPrio = DEFAULT_THREADPOOL_PRIORITY;


  public void startService() {
    super.startService();
    ConfigManager configMgr = getDaemon().getConfigManager();
    installTasks();
    cronStateFile = configMgr.getCacheConfigFile(CONFIG_FILE_CRON_STATE);
    log.debug2("State file: " + cronStateFile);
    loadState(cronStateFile);
    resetConfig();
  }

  public void stopService() {
    disable();
    super.stopService();
  }

  public void setConfig(Configuration config,
			Configuration prevConfig,
			Configuration.Differences changedKeys) {

    if (!getDaemon().isDaemonInited()) {
      return;
    }

    if (changedKeys.contains(PREFIX)) {
      boolean doEnable = config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED);
      interval = config.getTimeInterval(PARAM_SLEEP, DEFAULT_SLEEP);

      setExecutorParams(config);

      if (doEnable != enabled) {
	if (doEnable) {
	  enable();
	} else {
	  disable();
	}
	enabled = doEnable;
      }
    }

  }

  private synchronized ThreadPoolExecutor getExecutor() {
    if (executor == null) {
      Configuration config = ConfigManager.getCurrentConfig();
      int poolsize = config.getInt(PARAM_THREADPOOL_SIZE,
				   DEFAULT_THREADPOOL_SIZE);
      long keepalive = config.getTimeInterval(PARAM_THREADPOOL_KEEPALIVE,
					      DEFAULT_THREADPOOL_KEEPALIVE);
      executor = new ThreadPoolExecutor(poolsize, poolsize,
					keepalive, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());
      executor.allowCoreThreadTimeOut(true);
    }
    return executor;
  }

  private synchronized void setExecutorParams(Configuration config) {
    if (executor != null) {
      int poolsize = config.getInt(PARAM_THREADPOOL_SIZE,
				   DEFAULT_THREADPOOL_SIZE);
      long keepalive = config.getTimeInterval(PARAM_THREADPOOL_KEEPALIVE,
					      DEFAULT_THREADPOOL_KEEPALIVE);
      executor.setMaximumPoolSize(poolsize);
      executor.setCorePoolSize(poolsize);
      executor.setKeepAliveTime(keepalive, TimeUnit.MILLISECONDS);
    }
    taskPrio = config.getInt(PARAM_THREADPOOL_PRIORITY,
			     DEFAULT_THREADPOOL_PRIORITY);
  }

  /** Install standard tasks */
  void installTasks() {
    addTask(new RemoteApi.CreateBackupFile(getDaemon()));
    addTask(new SendPasswordReminder(getDaemon()));
  }

  /** Add a task */
  public void addTask(Cron.Task task) {
    synchronized (tasks) {
      tasks.add(task);
    }
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
	state.store(file);
      } catch (Exception e) {
	log.error("Couldn't store cron state, disabling cron: ", e);
	disable();
	// XXX alert
      }
  }

  private static ObjectSerializer makeObjectSerializer() {
    return new XStreamSerializer();
  }

  private synchronized void enable() {
    stopRunning();		      // First, ensure no current timer req
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
  private void checkTasks() {
    req = null;
    synchronized (tasks) {
      long now = TimeBase.nowMs();
      for (Cron.Task task : tasks) {
	if (!(task.nextTime(state.getLastTime(task.getId())) <= now)) {
	  continue;
	}
	if (runningIds.contains(task.getId())) {
	  continue;
	}
	fork(task);
      }
    }
    if (enabled) {
      schedNext();
    }
  }

  private void fork(final Task task) {
    try {
      LockssRunnable runnable =
	new LockssRunnable("Cron Task") {
	  public void lockssRun() {
	    setPriority(taskPrio);
	    executeAndRescheduleTask(task);
	    setThreadName("Cron Task: idle");
	  }};
      runningIds.add(task.getId());
      // submit() must be last, as thread may run to completion before it
      // returns
      getExecutor().submit(runnable);
    } catch (RuntimeException e) {
      log.warning("fork()", e);
    }
  }

  // For testing, called after all state is updated following task
  // execution
  protected void endExecuteHook(Task task) {
  }

  private void executeAndRescheduleTask(Task task) {
    if (executeTask(task)) {
      state.setLastTime(task.getId(), TimeBase.nowMs());
      storeState(cronStateFile);
    }
    runningIds.remove(task.getId());
    endExecuteHook(task);
  }
	
  private boolean executeTask(Task task) {
    try {
      log.debug2("Exec " + task);
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

    synchronized long getLastTime(String id) {
      Long l = (Long)times.get(id);
      if (l == null) {
	return 0;
      }
      return l.longValue();
    }

    synchronized void setLastTime(String id, long time) {
      times.put(id, new Long(time));
    }

    synchronized void store(File file)
	throws SerializationException, InterruptedIOException {
      if (log.isDebug2()) log.debug2("Storing in " + file + ": " + this);
      makeObjectSerializer().serialize(file, this);
    }
  }

  /** Task base */
  public abstract static class BaseTask implements Cron.Task {
    protected final LockssDaemon daemon;

    public BaseTask(LockssDaemon daemon) {
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
