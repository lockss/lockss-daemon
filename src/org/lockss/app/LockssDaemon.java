/*
 * $Id: LockssDaemon.java,v 1.32 2003-06-25 21:16:34 eaalto Exp $
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
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.state.*;
import org.lockss.proxy.*;
import org.lockss.servlet.*;
import org.lockss.crawler.*;
import org.apache.commons.collections.SequencedHashMap;

/**
 * @author Claire Griffin
 * @version 1.0
 */

public class LockssDaemon {
  private static String PREFIX = Configuration.PREFIX + "daemon.";

  private static String PARAM_DAEMON_EXIT_IMM = PREFIX + "exitImmediately";
  private static boolean DEFAULT_DAEMON_EXIT_IMM = false;

  private static String PARAM_DAEMON_EXIT_AFTER = PREFIX + "exitAfter";
  private static long DEFAULT_DAEMON_EXIT_AFTER = 0;

  private static String PARAM_DAEMON_EXIT_ONCE = PREFIX + "exitOnce";
  private static boolean DEFAULT_DAEMON_EXIT_ONCE = false;

  private static String MANAGER_PREFIX = Configuration.PREFIX + "manager.";

  /* the parameter strings that represent our managers */
  public static String ACTIVITY_REGULATOR = "ActivityRegulator";
  public static String WATCHDOG_SERVICE = "WatchdogService";
  public static String HASH_SERVICE = "HashService";
  public static String COMM_MANAGER = "CommManager";
  public static String ROUTER_MANAGER = "RouterManager";
  public static String IDENTITY_MANAGER = "IdentityManager";
  public static String CRAWL_MANAGER = "CrawlManager";
  public static String PLUGIN_MANAGER = "PluginManager";
  public static String POLL_MANAGER = "PollManager";
  public static String LOCKSS_REPOSITORY = "LockssRepository";
  public static String HISTORY_REPOSITORY = "HistoryRepository";
  public static String NODE_MANAGER = "NodeManager";
  public static String PROXY_MANAGER = "ProxyManager";
  public static String SERVLET_MANAGER = "ServletManager";
  public static String STATUS_SERVICE = "StatusService";
  public static String SYSTEM_METRICS = "SystemMetrics";
  public static String URL_MANAGER = "UrlManager";

  /* the default classes that represent our managers */
  private static String DEFAULT_ACTIVITY_REGULATOR =
      "org.lockss.daemon.ActivityRegulatorImpl";
  private static String DEFAULT_HASH_SERVICE = "org.lockss.hasher.HashService";
  private static String DEFAULT_WATCHDOG_SERVICE =
    "org.lockss.daemon.WatchdogService";
  private static String DEFAULT_COMM_MANAGER = "org.lockss.protocol.LcapComm";
  private static String DEFAULT_ROUTER_MANAGER =
    "org.lockss.protocol.LcapRouter";
  private static String DEFAULT_IDENTITY_MANAGER
      = "org.lockss.protocol.IdentityManager";
  private static String DEFAULT_CRAWL_MANAGER =
      "org.lockss.crawler.CrawlManagerImpl";
  private static String DEFAULT_PLUGIN_MANAGER =
      "org.lockss.plugin.PluginManager";
  private static String DEFAULT_POLL_MANAGER = "org.lockss.poller.PollManager";
  private static String DEFAULT_LOCKSS_REPOSITORY
      = "org.lockss.repository.LockssRepositoryImpl";
  private static String DEFAULT_HISTORY_REPOSITORY
      = "org.lockss.state.HistoryRepositoryImpl";
  private static String DEFAULT_NODE_MANAGER =
      "org.lockss.state.NodeManagerImpl";
  private static String DEFAULT_PROXY_MANAGER =
    "org.lockss.proxy.ProxyManager";
  private static String DEFAULT_SERVLET_MANAGER =
    "org.lockss.servlet.ServletManager";
  private static String DEFAULT_STATUS_SERVICE =
    "org.lockss.daemon.status.StatusServiceImpl";
  private static String DEFAULT_SYSTEM_METRICS =
    "org.lockss.daemon.SystemMetrics";
  private static String DEFAULT_URL_MANAGER =
    "org.lockss.daemon.UrlManager";


  private static String DEFAULT_CACHE_LOCATION = "./cache";
  private static String DEFAULT_CONFIG_LOCATION = "./config";

  private static class ManagerDesc {
    String key;				// hash key and config param name
    String defaultClass;		// default class name

    ManagerDesc(String key, String defaultClass) {
      this.key = key;
      this.defaultClass = defaultClass;
    }
  }

  // Manager descriptors.  The order of this table determines the order in
  // which managers are initialized and started.  AU specific managers are
  // started as needed.
  private ManagerDesc[] managerDescs = {
    new ManagerDesc(STATUS_SERVICE, DEFAULT_STATUS_SERVICE),
    new ManagerDesc(URL_MANAGER, DEFAULT_URL_MANAGER),
    new ManagerDesc(HASH_SERVICE, DEFAULT_HASH_SERVICE),
    new ManagerDesc(SYSTEM_METRICS, DEFAULT_SYSTEM_METRICS),
    new ManagerDesc(IDENTITY_MANAGER, DEFAULT_IDENTITY_MANAGER),
    new ManagerDesc(HISTORY_REPOSITORY, DEFAULT_HISTORY_REPOSITORY),
    new ManagerDesc(POLL_MANAGER, DEFAULT_POLL_MANAGER),
    new ManagerDesc(CRAWL_MANAGER, DEFAULT_CRAWL_MANAGER),
    // start plugin manager after generic services
    new ManagerDesc(PLUGIN_MANAGER, DEFAULT_PLUGIN_MANAGER),
    // start proxy and servlets after plugin manager
    new ManagerDesc(SERVLET_MANAGER, DEFAULT_SERVLET_MANAGER),
    new ManagerDesc(PROXY_MANAGER, DEFAULT_PROXY_MANAGER),
    // start comm layer so we don't receive a message
    new ManagerDesc(COMM_MANAGER, DEFAULT_COMM_MANAGER),
    new ManagerDesc(ROUTER_MANAGER, DEFAULT_ROUTER_MANAGER),
    new ManagerDesc(WATCHDOG_SERVICE, DEFAULT_WATCHDOG_SERVICE),
  };

  private static String[] auSpecificManagerKeys = {
      ACTIVITY_REGULATOR,
      LOCKSS_REPOSITORY,
      NODE_MANAGER
  };

  private static Logger log = Logger.getLogger("LockssDaemon");
  protected List propUrls = null;
  private String configDir = null;
  // set true after all managers have been inited
  private boolean daemonInited = false;
  private boolean daemonRunning = false;
  private Date startDate;
  private long daemonLifetime = DEFAULT_DAEMON_EXIT_AFTER;
  private Deadline timeToExit = Deadline.at(TimeBase.MAX);

  // Need to preserve order so managers are started and stopped in the
  // right order.  This does not need to be synchronized.
  protected static SequencedHashMap theManagers = new SequencedHashMap();

  // order is unimportant here.  This is a hashmap of hashmaps.
  protected static HashMap auSpecificManagers =
      new HashMap(auSpecificManagerKeys.length);

  protected LockssDaemon(List propUrls) {
    this.propUrls = propUrls;
  }

  public static void main(String[] args) {
    Vector urls = new Vector();
    LockssDaemon daemon;

    for (int i=0; i<args.length; i++) {
      urls.add(args[i]);
    }

    try {
      daemon = new LockssDaemon(urls);
      daemon.runDaemon();
    } catch (Throwable e) {
      log.error("Exception thrown in main loop", e);
      System.exit(1);
      return;				// compiler doesn't know that
					// System.exit() doesn't return
    }
    if (Configuration.getBooleanParam(PARAM_DAEMON_EXIT_IMM,
				      DEFAULT_DAEMON_EXIT_IMM)) {
      daemon.stop();
      System.exit(0);
    }
    daemon.keepRunning();
    log.info("Exiting because time to die");
    System.exit(0);
  }

  private void keepRunning() {
    while (!timeToExit.expired()) {
      try {
	log.debug("Will exit at " + timeToExit);
	timeToExit.sleep();
      } catch (InterruptedException e) {
	// no action
      }
    }
  }

  /** Return the time the daemon started running.
   * @return the time the daemon started running, as a Date
   */
  public Date getStartDate() {
    return startDate;
  }

  /**
   * Return a lockss manager. This will need to be cast to the appropriate
   * class.
   * @param managerKey the name of the manager
   * @return a lockss manager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public static LockssManager getManager(String managerKey) {
    LockssManager mgr = (LockssManager) theManagers.get(managerKey);
    if(mgr == null) {
      throw new IllegalArgumentException("Unavailable manager:" + managerKey);
    }
    return mgr;
  }

  /**
   * Return an au-specific lockss manager. This will need to be cast to the
   * appropriate class.
   * @param au the ArchivalUnit
   * @param managerKey the name of the manager
   * @return an au-specific lockss manager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public static LockssManager getAUSpecificManager(String managerKey,
      ArchivalUnit au) {
    HashMap auMap = getAUSpecificManagerMap(managerKey);
    LockssManager mgr = (LockssManager)auMap.get(au);
    if (mgr == null) {
      throw new IllegalArgumentException("Unavailable manager:" + managerKey);
    }
    return mgr;
  }

  protected static HashMap getAUSpecificManagerMap(String managerKey) {
    HashMap map = (HashMap)auSpecificManagers.get(managerKey);
    if (map==null) {
      log.debug3("Initializing hashmap for "+managerKey);
      map = new HashMap();
      auSpecificManagers.put(managerKey, map);
    }
    return map;
  }

  /**
   * Starts any managers necessary to handle the ArchivalUnit.
   * @param au the ArchivalUnit
   */
  public void startAUManagers(ArchivalUnit au) {
    // this order can't be changed, as the other two use the ActivityRegulator
    // and the NodeManager uses the LockssRepository
    log.debug2("Adding au-specific managers for au '"+au+"'");
    addActivityRegulator(au);
    addLockssRepository(au);
    addNodeManager(au);
  }

  protected void addActivityRegulator(ArchivalUnit au) {
    HashMap activityRegulatorMap = getAUSpecificManagerMap(ACTIVITY_REGULATOR);
    ActivityRegulator regulator =
        (ActivityRegulator)activityRegulatorMap.get(au);
    if (regulator == null) {
      log.debug2("Adding new activity regulator...");
      regulator = ActivityRegulator.createNewActivityRegulator(au);
      activityRegulatorMap.put(au, regulator);
      regulator.initService(this);
      regulator.startService();
    }
  }

  protected void addLockssRepository(ArchivalUnit au) {
    HashMap lockssRepositoryMap = getAUSpecificManagerMap(LOCKSS_REPOSITORY);
    LockssRepository repository = (LockssRepository)lockssRepositoryMap.get(au);
    if (repository == null) {
      log.debug2("Adding new lockss repository...");
      repository = LockssRepositoryImpl.createNewLockssRepository(au);
      lockssRepositoryMap.put(au, repository);
      repository.initService(this);
      repository.startService();
    }
  }

  protected void addNodeManager(ArchivalUnit au) {
    HashMap nodeManagerMap = getAUSpecificManagerMap(NODE_MANAGER);
    NodeManager manager = (NodeManager)nodeManagerMap.get(au);
    if (manager == null) {
      log.debug2("Adding new node manager...");
      manager = NodeManagerImpl.createNewNodeManager(au);
      nodeManagerMap.put(au, manager);
      manager.initService(this);
      manager.startService();
    }
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
   * return the hash service instance
   * @return the HashService
   * @throws IllegalArgumentException if the manager is not available.
   */
  public HashService getHashService() {
    return (HashService) getManager(HASH_SERVICE);
  }

  /**
   * return the poll manager instance
   * @return the PollManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public PollManager getPollManager() {
    return (PollManager) getManager(POLL_MANAGER);
  }

  /**
   * return the communication manager instance
   * @return the LcapComm
   * @throws IllegalArgumentException if the manager is not available.
   */
  public LcapComm getCommManager()  {
    return (LcapComm) getManager(COMM_MANAGER);
  }

  /**
   * return the communication router manager instance
   * @return the LcapRouter
   * @throws IllegalArgumentException if the manager is not available.
   */
  public LcapRouter getRouterManager()  {
    return (LcapRouter) getManager(ROUTER_MANAGER);
  }

  /**
   * get Lockss Repository instance
   * @param au the ArchivalUnit
   * @return the LockssRepository
   * @throws IllegalArgumentException if the manager is not available.
   */
  public LockssRepository getLockssRepository(ArchivalUnit au) {
    HashMap lockssRepositoryMap = getAUSpecificManagerMap(LOCKSS_REPOSITORY);
    LockssRepository repository = (LockssRepository)lockssRepositoryMap.get(au);
    if (repository==null) {
      log.error("LockssRepository not found for au: " + au);
      throw new IllegalArgumentException("LockssRepository not found for au.");
    }
    return repository;
  }

  /**
   * return the history repository instance
   * @return the HistoryRepository
   * @throws IllegalArgumentException if the manager is not available.
   */
  public HistoryRepository getHistoryRepository() {
    return (HistoryRepository) getManager(HISTORY_REPOSITORY);
  }

  /**
   * return the node manager instance
   * @param au the ArchivalUnit
   * @return the NodeManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public NodeManager getNodeManager(ArchivalUnit au) {
    HashMap nodeManagerMap = getAUSpecificManagerMap(NODE_MANAGER);
    NodeManager manager = (NodeManager)nodeManagerMap.get(au);
    if (manager==null) {
      log.error("NodeManager not found for au: " + au);
      throw new IllegalArgumentException("NodeManager not found for au.");
    }
    return manager;
  }

  /**
   * return the proxy handler instance
   * @return the ProxyManager
   * @throws IllegalArgumentException if the manager is not available.
  */
  public ProxyManager getProxyManager() {
    return (ProxyManager) getManager(PROXY_MANAGER);
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
   * return the crawl manager instance
   * @return the CrawlManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public CrawlManager getCrawlManager() {
    return (CrawlManager) getManager(CRAWL_MANAGER);
  }

  /**
   * return the {@link org.lockss.daemon.status.StatusService} instance
   * @returns {@link org.lockss.daemon.status.StatusService} instance
   * @throws IllegalArgumentException if the manager is not available.
   */
  public StatusService getStatusService() {
    return (StatusService) getManager(STATUS_SERVICE);
  }

  /**
   * return the SystemMetrics instance.
   * @returns SystemMetrics instance.
   * @throws IllegalArgumentException if the manager is not available.
   */
  public SystemMetrics getSystemMetrics() {
    return (SystemMetrics) getManager(SYSTEM_METRICS);
  }

  /**
   * return the plugin manager instance
   * @return the PluginManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public PluginManager getPluginManager() {
    return (PluginManager) getManager(PLUGIN_MANAGER);
  }

  /**
   * return the Identity Manager
   * @return IdentityManager
   * @throws IllegalArgumentException if the manager is not available.
   */

  public IdentityManager getIdentityManager() {
    return (IdentityManager) getManager(IDENTITY_MANAGER);
  }

  /**
   * get ActivityRegulator instance
   * @param au the ArchivalUnit
   * @return the ActivityRegulator
   * @throws IllegalArgumentException if the manager is not available.
   */
  public ActivityRegulator getActivityRegulator(ArchivalUnit au) {
    HashMap activityRegulatorMap = getAUSpecificManagerMap(ACTIVITY_REGULATOR);
    ActivityRegulator regulator = (ActivityRegulator) activityRegulatorMap.get(au);
    if (regulator==null) {
      log.error("ActivityRegulator not found for au: " + au);
      throw new IllegalArgumentException("ActivityRegulator not found for au.");
    }
    return regulator;
  }

  /**
   * The ActivityRegulator entries.
   * @return an Iterator of Map.Entry objects
   */
  public Iterator getActivityRegulatorEntries() {
    return getAUSpecificManagerMap(ACTIVITY_REGULATOR).entrySet().iterator();
  }

  /**
   * The LockssRepository entries.
   * @return an Iterator of Map.Entry objects
   */
  public Iterator getLockssRepositoryEntries() {
    return getAUSpecificManagerMap(LOCKSS_REPOSITORY).entrySet().iterator();
  }

  /**
   * The NodeManager entries.
   * @return an Iterator of Map.Entry objects
   */
  public Iterator getNodeManagerEntries() {
    return getAUSpecificManagerMap(NODE_MANAGER).entrySet().iterator();
  }

  public void stopDaemon() {
    stop();
  }

  /**
   * run the daemon.  Load our properties, initialize our managers, initialize
   * the plugins.
   * @throws Exception if the initialization fails
   */
  protected void runDaemon() throws Exception {

    startDate = TimeBase.nowDate();

    // initialize our properties from the urls given
    initProperties();

    // startup all services
    initManagers();
  }

  /**
   * init our configuration and extract any parameters we will use locally
   */
  protected void initProperties() {
    Configuration.startHandler(propUrls);
    log.info("Waiting for config");
    Configuration.waitConfig();
    log.info("Config loaded");

    prevExitOnce = Configuration.getBooleanParam(PARAM_DAEMON_EXIT_ONCE,
						 DEFAULT_DAEMON_EXIT_ONCE);

    Configuration.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration prevConfig,
					 Set changedKeys) {
	  setConfig(newConfig, prevConfig, changedKeys);
	}
      });
  }

  boolean prevExitOnce = false;

  protected void setConfig(Configuration config, Configuration prevConfig,
			   Set changedKeys) {
    long life = config.getTimeInterval(PARAM_DAEMON_EXIT_AFTER,
				       DEFAULT_DAEMON_EXIT_AFTER);
    if (life != daemonLifetime) {
      // lifetime changed
      daemonLifetime = life;
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
    boolean exitOnce = config.getBoolean(PARAM_DAEMON_EXIT_ONCE,
					 DEFAULT_DAEMON_EXIT_ONCE);
    if (!prevExitOnce && exitOnce) {
      timeToExit.expire();
    } else {
      prevExitOnce = exitOnce;
    }
  }

  /**
   * init all of the managers that support the daemon.
   * @throws Exception if initilization fails
   */
  protected void initManagers() throws Exception {
    for(int i=0; i< managerDescs.length; i++) {
      ManagerDesc desc = managerDescs[i];
      String mgr_name = Configuration.getParam(MANAGER_PREFIX + desc.key,
					       desc.defaultClass);
      LockssManager mgr = loadManager(mgr_name);
      theManagers.put(desc.key, mgr);
    }
    daemonInited = true;
    // now start the managers in the same order in which they were created
    // (theManagers is a SequencedHashMap)
    Iterator it = theManagers.values().iterator();
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

    daemonRunning = true;
  }

  /**
   * Stop the daemon, by stopping the managers in the reverse order of
   * starting.
   */
  protected void stop() {
    daemonRunning = false;

    // stop all au-specific managers
    stopAllAuSpecificManagers();

    // stop all single managers
    List rkeys = ListUtil.reverseCopy(theManagers.sequence());
    for (Iterator it = rkeys.iterator(); it.hasNext(); ) {
      String key = (String)it.next();
      LockssManager lm = (LockssManager)theManagers.get(key);
      try {
	lm.stopService();
      } catch (Exception e) {
	log.warning("Couldn't stop service " + lm, e);
      }
    }
  }

  private void stopAllAuSpecificManagers() {
    Iterator managerEntries = auSpecificManagers.entrySet().iterator();
    while (managerEntries.hasNext()) {
      Map.Entry entry = (Map.Entry)managerEntries.next();
      log.debug2("Stopping all " + entry.getKey() + " instances...");
      HashMap auMap = (HashMap)entry.getValue();
      Iterator values = auMap.values().iterator();
      while (values.hasNext()) {
        LockssManager manager = (LockssManager) values.next();
        manager.stopService();
      }
    }
    auSpecificManagers.clear();
  }


  /**
   * Load the managers with the manager class name
   * @param managerName the class name of the manager to load
   * @return the manager that has been loaded
   * @throws Exception if load fails
   */
  LockssManager loadManager(String managerName) throws Exception {
    log.debug("Loading manager " + managerName);
    try {
      Class manager_class = Class.forName(managerName);
      LockssManager mgr = (LockssManager) manager_class.newInstance();
      // call init on the service
      mgr.initService(this);
      return mgr;
    }
    catch (Exception ex) {
      log.error("Unable to instantiate Lockss Manager "+ managerName, ex);
      throw(ex);
    }
  }

  /**
   * True iff all managers were inited.
   * @return true iff all managers have been inited */
  public boolean isDaemonInited() {
    return daemonInited;
  }

  /**
   * True if all managers were started.
   * @return true iff all managers have been started */
  public boolean isDaemonRunning() {
    return daemonRunning;
  }
}
