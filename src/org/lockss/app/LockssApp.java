/*
 * $Id: LockssApp.java,v 1.12 2005-12-01 23:28:01 troberts Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.app;

import java.util.*;
import org.lockss.util.*;
import org.lockss.mail.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.scheduler.*;
import org.lockss.servlet.*;
import org.apache.commons.collections.map.LinkedMap;

/**
 * Abstract base class for LOCKSS applications.  Derived from original
 * LockssDaemon, and still more geared to that than it should be.
 * @author Claire Griffin
 * @version 1.0
 */

public abstract class LockssApp {
  private static Logger log = Logger.getLogger("LockssApp");

  private static String PREFIX = Configuration.PREFIX + "app.";

  public static final String PARAM_APP_EXIT_IMM = PREFIX + "exitImmediately";
  public static final boolean DEFAULT_APP_EXIT_IMM = false;

  public static final String PARAM_APP_EXIT_AFTER = PREFIX + "exitAfter";
  public static final long DEFAULT_APP_EXIT_AFTER = 0;

  public static final String PARAM_APP_EXIT_ONCE = PREFIX + "exitOnce";
  public static final boolean DEFAULT_APP_EXIT_ONCE = false;

  public static final String PARAM_DEBUG = PREFIX + "debug";
  public static final boolean DEFAULT_DEBUG = false;

  static final String PARAM_PLATFORM_VERSION =
    Configuration.PREFIX + "platform.version";

  private static final String PARAM_EXERCISE_DNS = PREFIX + "poundDns";
  private static final boolean DEFAULT_EXERCISE_DNS = false;

  public static final String MANAGER_PREFIX =
    Configuration.PREFIX + "manager.";

  // Parameter keys for common managers
  public static final String WATCHDOG_SERVICE = "WatchdogService";
  public static final String MAIL_SERVICE = "MailService";
  public static final String STATUS_SERVICE = "StatusService";
  public static final String SCHED_SERVICE = "SchedService";
  public static final String RESOURCE_MANAGER = "ResourceManager";
  public static final String SERVLET_MANAGER = "ServletManager";

  // default classes for common managers
  protected static final String DEFAULT_WATCHDOG_SERVICE =
    "org.lockss.daemon.WatchdogService";
  protected static final String DEFAULT_MAIL_SERVICE =
    "org.lockss.mail.SmtpMailService";
  protected static final String DEFAULT_STATUS_SERVICE =
    "org.lockss.daemon.status.StatusServiceImpl";
  protected static final String DEFAULT_SCHED_SERVICE =
    "org.lockss.scheduler.SchedService";
  protected static final String DEFAULT_RESOURCE_MANAGER =
    "org.lockss.daemon.ResourceManager";

  public static class ManagerDesc {
    String key;		// hash key and config param name
    String defaultClass;      // default class name (or factory class name)

    public ManagerDesc(String key, String defaultClass) {
      this.key = key;
      this.defaultClass = defaultClass;
    }

    public String getKey() {
      return key;
    }
    public String getDefaultClass() {
      return defaultClass;
    }
  }

  protected List propUrls = null;
  protected String groupName = null;

  protected boolean appInited = false;	// true after all managers inited
  protected boolean appRunning = false; // true after all managers started
  protected Date startDate;
  protected long appLifetime = DEFAULT_APP_EXIT_AFTER;
  protected Deadline timeToExit = Deadline.at(TimeBase.MAX);

  // Map of managerKey -> manager instance. Need to preserve order so
  // managers are started and stopped in the right order.  This does not
  // need to be synchronized.
  protected LinkedMap managerMap = new LinkedMap();

  private static LockssApp theApp;

  protected LockssApp() {
    theApp = this;
  }

  protected LockssApp(List propUrls) {
    this.propUrls = propUrls;
    theApp = this;
  }

  protected LockssApp(List propUrls, String groupName) {
    this.propUrls = propUrls;
    this.groupName = groupName;
    theApp = this;
  }

  /** Must be implemented by subclass to return list of managers in the
   * order they should be started */
  protected abstract ManagerDesc[] getManagerDescs();

  // General information accessors

  /**
   * True iff all managers have been inited.
   * @return true iff all managers have been inited */
  public boolean isAppInited() {
    return appInited;
  }

  /**
   * True if all managers have been started.
   * @return true iff all managers have been started */
  public boolean isAppRunning() {
    return appRunning;
  }

  /**
   * True if running in debug mode (org.lockss.app.debug=true).
   * @return true iff in debug mode */
  public static boolean isDebug() {
    return ConfigManager.getBooleanParam(PARAM_DEBUG, DEFAULT_DEBUG);
  }

  /** Return the time the app started running.
   * @return the time the app started running, as a Date
   */
  public Date getStartDate() {
    if (startDate == null) {
      // this happens during testing
      startDate = TimeBase.nowDate();
    }
    return startDate;
  }

  /** Return a string describing the version of the app and platform */
  public String getVersionInfo() {
    String vApp = BuildInfo.getBuildInfoString();
    PlatformVersion plat = Configuration.getPlatformVersion();
    if (plat != null) {
      vApp = vApp + ", " + plat.displayString();
    }
    return vApp;
  }

  // LockssManager accessors

  /**
   * Return a lockss manager. This will need to be cast to the appropriate
   * class.
   * @param managerKey the name of the manager
   * @return a lockss manager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public static LockssManager getManager(String managerKey) {
    if (theApp == null) {
      throw new NullPointerException("App has not been created");
    }
    return theApp.getManagerByKey(managerKey);
  }

  /**
   * Return a lockss manager. This will need to be cast to the appropriate
   * class.
   * @param managerKey the name of the manager
   * @return a lockss manager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public LockssManager getManagerByKey(String managerKey) {
    LockssManager mgr = (LockssManager) managerMap.get(managerKey);
    if(mgr == null) {
      throw new IllegalArgumentException("Unavailable manager:" + managerKey);
    }
    return mgr;
  }

  /**
   * Return the config manager instance.  Special case.
   * @return the ConfigManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public ConfigManager getConfigManager() {
    return ConfigManager.getConfigManager();
  }

  /**
   * return the watchdog service instance
   * @return the WatchdogService
   * @throws IllegalArgumentException if the manager is not available.
   */
  public WatchdogService getWatchdogService() {
    return (WatchdogService)getManager(WATCHDOG_SERVICE);
  }

  /**
   * return the mail manager instance
   * @return the MailService
   * @throws IllegalArgumentException if the manager is not available.
   */
  public MailService getMailService() {
    return (MailService)getManager(MAIL_SERVICE);
  }

  /**
   * return the sched service instance
   * @return the SchedService
   * @throws IllegalArgumentException if the manager is not available.
   */
  public SchedService getSchedService() {
    return (SchedService) getManager(SCHED_SERVICE);
  }

  /**
   * return the resource manager instance
   * @return the ResourceManager
   * @throws IllegalArgumentException if the manager is not available.
  */
  public ResourceManager getResourceManager() {
    return (ResourceManager) getManager(RESOURCE_MANAGER);
  }

  /**
   * return the servlet manager instance
   * @return the ServletManager
   * @throws IllegalArgumentException if the manager is not available.
  */
  public ServletManager getServletManager() {
    return (ServletManager) getManager(SERVLET_MANAGER);
  }

  /**
   * return the {@link org.lockss.daemon.status.StatusService} instance
   * @return {@link org.lockss.daemon.status.StatusService} instance
   * @throws IllegalArgumentException if the manager is not available.
   */
  public StatusService getStatusService() {
    return (StatusService) getManager(STATUS_SERVICE);
  }

  // Manager loading, starting, stopping

  /**
   * Load and init the specified manager.  If the manager class is
   * specified by a config parameter and cannot be loaded, fall back to the
   * default class.
   * @param desc entry describing manager to load
   * @return the manager that has been loaded
   * @throws Exception if load fails
   */
  protected LockssManager initManager(ManagerDesc desc) throws Exception {
    String managerName = CurrentConfig.getParam(MANAGER_PREFIX + desc.key,
						desc.defaultClass);
    LockssManager mgr = instantiateManager(desc);
    try {
      // call init on the service
      mgr.initService(this);
      return mgr;
    } catch (Exception ex) {
      log.error("Unable to instantiate Lockss Manager "+ managerName, ex);
      throw(ex);
    }
  }

  /** Create an instance of a LockssManager, from the configured or default
   * manager class name */
  protected LockssManager instantiateManager(ManagerDesc desc)
      throws Exception {
    String managerName = CurrentConfig.getParam(MANAGER_PREFIX + desc.key,
						desc.defaultClass);
    LockssManager mgr;
    try {
      mgr = (LockssManager)makeInstance(managerName);
    } catch (ClassNotFoundException e) {
      log.warning("Couldn't load manager class " + managerName);
      if (!managerName.equals(desc.defaultClass)) {
	log.warning("Trying default manager class " + desc.defaultClass);
	mgr = (LockssManager)makeInstance(desc.defaultClass);
      } else {
	throw e;
      }
    }
    return mgr;
  }

  protected Object makeInstance(String managerClassName)
      throws ClassNotFoundException, InstantiationException,
	     IllegalAccessException {
    log.debug2("Instantiating manager class " + managerClassName);
    Class mgrClass = Class.forName(managerClassName);
    return mgrClass.newInstance();
  }

  /**
   * init all of the managers that support the app.
   * @throws Exception if initialization fails
   */
  protected void initManagers() throws Exception {
    ManagerDesc[] managerDescs = getManagerDescs();

    for(int i=0; i< managerDescs.length; i++) {
      ManagerDesc desc = managerDescs[i];
      if (managerMap.get(desc.key) != null) {
	throw new RuntimeException("Duplicate manager key: " + desc.key);
      }
      LockssManager mgr = initManager(desc);
      managerMap.put(desc.key, mgr);
    }
    appInited = true;
    // now start the managers in the same order in which they were created
    // (managerMap is a LinkedMap)
    Iterator it = managerMap.values().iterator();
    while(it.hasNext()) {
      LockssManager lm = (LockssManager)it.next();
      try {
	lm.startService();
      } catch (Exception e) {
	log.error("Couldn't start service " + lm, e);
	// don't try to start remaining managers
	throw e;
      }
    }

    appRunning = true;
  }

  /** Stop the app.  Currently only used in testing. */
  public void stopApp() {
    stop();
  }

  /**
   * Stop the app, by stopping the managers in the reverse order of
   * starting.
   */
  protected void stop() {
    appRunning = false;

    // stop all single managers
    List rkeys = ListUtil.reverseCopy(managerMap.asList());
    for (Iterator it = rkeys.iterator(); it.hasNext(); ) {
      String key = (String)it.next();
      LockssManager lm = (LockssManager)managerMap.get(key);
      try {
	lm.stopService();
	managerMap.remove(key);
      } catch (Exception e) {
	log.warning("Couldn't stop service " + lm, e);
      }
    }
  }

  // App start, stop

  /**
   * run the app.  Load our properties, initialize our managers, initialize
   * the plugins.
   * @throws Exception if the initialization fails
   */
  protected void startApp() throws Exception {

    startDate = TimeBase.nowDate();

    log.info(getVersionInfo() + ": starting");

    // initialize our properties from the urls given
    initProperties();

    // repeat the version info, as we may now be logging to a different target
    // (And to include the platform version, which wasn't availabe before the
    // config was loaded.)
    log.info(getVersionInfo() + ": starting managers");

    // startup all services
    initManagers();

    log.info("Started");
  }

  /**
   * init our configuration and extract any parameters we will use locally
   */
  protected void initProperties() {
    ConfigManager configMgr = ConfigManager.makeConfigManager(propUrls, groupName);
    configMgr.initService(this);
    configMgr.startService();
    log.info("Waiting for config");
    configMgr.waitConfig();
    log.info("Config loaded");

    prevExitOnce = CurrentConfig.getBooleanParam(PARAM_APP_EXIT_ONCE,
						 DEFAULT_APP_EXIT_ONCE);

    Configuration.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration prevConfig,
					 Configuration.Differences changedKeys) {
	  setConfig(newConfig, prevConfig, changedKeys);
	}
      });
  }

  boolean prevExitOnce = false;

  protected void setConfig(Configuration config, Configuration prevConfig,
			   Configuration.Differences changedKeys) {

    // temporary while debugging jvm DNS problem
    if (changedKeys.contains(PARAM_EXERCISE_DNS)) {
      IPAddr.setExerciseDNS(config.getBoolean(PARAM_EXERCISE_DNS,
					      DEFAULT_EXERCISE_DNS));
    }

    long life = config.getTimeInterval(PARAM_APP_EXIT_AFTER,
				       DEFAULT_APP_EXIT_AFTER);
    if (life != appLifetime) {
      // lifetime changed
      appLifetime = life;
      if (life == 0) {
	// zero is forever
	timeToExit.expireAt(TimeBase.MAX);
      } else {
	// compute new randomized deadline relative to start time
	long start = getStartDate().getTime();
	long min = start + life - life/4;
	long max = start + life + life/4;
	long prevExp = timeToExit.getExpirationTime();
	if (!(min <= prevExp && prevExp <= max)) {
	  // previous end of life is not within new range, so change timer
	  if (min <= TimeBase.nowMs()) {
	    // earliest time is earlier than now.  make random interval at
	    // least an hour long to prevent all daemons from exiting too
	    // close to each other.
	    min = TimeBase.nowMs();
	    max = Math.max(max, min + Constants.HOUR);
	  }
	  Deadline tmp = Deadline.atRandomRange(min, max);
	  timeToExit.expireAt(tmp.getExpirationTime());
	}
      }
    }

    // THIS MUST BE LAST IN THIS ROUTINE
    boolean exitOnce = config.getBoolean(PARAM_APP_EXIT_ONCE,
					 DEFAULT_APP_EXIT_ONCE);
    if (!prevExitOnce && exitOnce) {
      timeToExit.expire();
    } else {
      prevExitOnce = exitOnce;
    }
  }

  protected void keepRunning() {
    while (!timeToExit.expired()) {
      try {
	log.debug("Will exit at " + timeToExit);
	timeToExit.sleep();
      } catch (InterruptedException e) {
	// no action
      }
    }
  }

}
