/*
 * $Id: LockssDaemon.java,v 1.55 2004-06-15 21:43:42 tlipkis Exp $
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
import org.lockss.scheduler.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.state.*;
import org.lockss.proxy.*;
import org.lockss.servlet.*;
import org.lockss.crawler.*;
import org.lockss.remote.*;
import org.apache.commons.collections.SequencedHashMap;

/**
 * @author Claire Griffin
 * @version 1.0
 */

public class LockssDaemon {
  private static Logger log = Logger.getLogger("LockssDaemon");

  private static String PREFIX = Configuration.PREFIX + "daemon.";

  private static String PARAM_DAEMON_EXIT_IMM = PREFIX + "exitImmediately";
  private static boolean DEFAULT_DAEMON_EXIT_IMM = false;

  private static String PARAM_DAEMON_EXIT_AFTER = PREFIX + "exitAfter";
  private static long DEFAULT_DAEMON_EXIT_AFTER = 0;

  private static String PARAM_DAEMON_EXIT_ONCE = PREFIX + "exitOnce";
  private static boolean DEFAULT_DAEMON_EXIT_ONCE = false;

  private static String PARAM_DEBUG = PREFIX + "debug";

  static final String PARAM_PLATFORM_VERSION =
    Configuration.PREFIX + "platform.version";

  static final String PARAM_EXERCISE_DNS = PREFIX + "poundDns";
  private static boolean DEFAULT_EXERCISE_DNS = false;

  public static final String MANAGER_PREFIX =
    Configuration.PREFIX + "manager.";

/**
 * LOCKSS is a trademark of Stanford University.  Stanford hereby grants you
 * limited permission to use the LOCKSS trademark only in connection with
 * this software, including in the User-Agent HTTP request header generated
 * by the software and provided to web servers, provided the software or any
 * output of the software is used solely for the purpose of populating a
 * certified LOCKSS cache from a web server that has granted permission for
 * the LOCKSS system to collect material.  You may not remove or delete any
 * reference to LOCKSS in the software indicating that LOCKSS is a mark owned
 * by Stanford University.  No other permission is granted you to use the
 * LOCKSS trademark or any other trademark of Stanford University.  Without
 * limiting the foregoing, if you adapt or use the software for any other
 * purpose, you must delete all references to or uses of the LOCKSS mark from
 * the software.  All good will associated with your use of the LOCKSS mark
 * shall inure to the benefit of Stanford University.
 */
private final static String LOCKSS_USER_AGENT = "LOCKSS cache";

  /* the parameter strings that represent our managers */
  public static String ACTIVITY_REGULATOR = "ActivityRegulator";
  public static String WATCHDOG_SERVICE = "WatchdogService";
  public static String HASH_SERVICE = "HashService";
  public static String SCHED_SERVICE = "SchedService";
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
  public static String AUDIT_PROXY_MANAGER = "AuditProxyManager";
  public static String SERVLET_MANAGER = "ServletManager";
  public static String STATUS_SERVICE = "StatusService";
  public static String SYSTEM_METRICS = "SystemMetrics";
  public static String REMOTE_API = "RemoteApi";
  public static String URL_MANAGER = "UrlManager";
  public static String NODE_MANAGER_STATUS = "NodeManagerStatus";
  public static String REPOSITORY_STATUS = "RepositoryStatus";
  public static String ARCHIVAL_UNIT_STATUS = "ArchivalUnitStatus";

  /* the default classes that represent our managers */
  private static String DEFAULT_CONFIG_MANAGER =
    "org.lockss.daemon.ConfigManager";
  private static String DEFAULT_HASH_SERVICE =
    "org.lockss.hasher.HashSvcQueueImpl";
  private static String DEFAULT_SCHED_SERVICE =
    "org.lockss.scheduler.SchedService";
  private static String DEFAULT_WATCHDOG_SERVICE =
    "org.lockss.daemon.WatchdogService";
  private static String DEFAULT_COMM_MANAGER = "org.lockss.protocol.LcapComm";
  private static String DEFAULT_ROUTER_MANAGER =
    "org.lockss.protocol.LcapRouter";
  private static String DEFAULT_IDENTITY_MANAGER =
    "org.lockss.protocol.IdentityManager";
  private static String DEFAULT_CRAWL_MANAGER =
    "org.lockss.crawler.CrawlManagerImpl";
  private static String DEFAULT_PLUGIN_MANAGER =
    "org.lockss.plugin.PluginManager";
  private static String DEFAULT_POLL_MANAGER = "org.lockss.poller.PollManager";
  private static String DEFAULT_PROXY_MANAGER =
    "org.lockss.proxy.ProxyManager";
  private static String DEFAULT_AUDIT_PROXY_MANAGER =
    "org.lockss.proxy.AuditProxyManager";
  private static String DEFAULT_SERVLET_MANAGER =
    "org.lockss.servlet.ServletManager";
  private static String DEFAULT_STATUS_SERVICE =
    "org.lockss.daemon.status.StatusServiceImpl";
  private static String DEFAULT_SYSTEM_METRICS =
    "org.lockss.daemon.SystemMetrics";
  private static String DEFAULT_URL_MANAGER =
    "org.lockss.daemon.UrlManager";
  private static String DEFAULT_REMOTE_API =
    "org.lockss.remote.RemoteApi";
  private static String DEFAULT_NODE_MANAGER_STATUS =
    "org.lockss.state.NodeManagerStatus";
  private static String DEFAULT_REPOSITORY_STATUS =
    "org.lockss.repository.LockssRepositoryStatus";
  private static String DEFAULT_ARCHIVAL_UNIT_STATUS =
    "org.lockss.state.ArchivalUnitStatus";

  // default AU specific manager factories
  private static String DEFAULT_ACTIVITY_REGULATOR =
    "org.lockss.daemon.ActivityRegulator$Factory";
  private static String DEFAULT_LOCKSS_REPOSITORY =
    "org.lockss.repository.LockssRepositoryImpl$Factory";
  private static String DEFAULT_NODE_MANAGER =
    "org.lockss.state.NodeManagerImpl$Factory";
  private static String DEFAULT_HISTORY_REPOSITORY =
    "org.lockss.state.HistoryRepositoryImpl$Factory";


  protected static class ManagerDesc {
    String key;		// hash key and config param name
    String defaultClass;      // default class name (or factory class name)

    ManagerDesc(String key, String defaultClass) {
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

  // Manager descriptors.  The order of this table determines the order in
  // which managers are initialized and started.
  protected static final ManagerDesc[] managerDescs = {
    new ManagerDesc(STATUS_SERVICE, DEFAULT_STATUS_SERVICE),
    new ManagerDesc(URL_MANAGER, DEFAULT_URL_MANAGER),
    new ManagerDesc(SCHED_SERVICE, DEFAULT_SCHED_SERVICE),
    new ManagerDesc(HASH_SERVICE, DEFAULT_HASH_SERVICE),
    new ManagerDesc(SYSTEM_METRICS, DEFAULT_SYSTEM_METRICS),
    new ManagerDesc(IDENTITY_MANAGER, DEFAULT_IDENTITY_MANAGER),
    new ManagerDesc(POLL_MANAGER, DEFAULT_POLL_MANAGER),
    new ManagerDesc(CRAWL_MANAGER, DEFAULT_CRAWL_MANAGER),
    // start plugin manager after generic services
    new ManagerDesc(PLUGIN_MANAGER, DEFAULT_PLUGIN_MANAGER),
    // start proxy and servlets after plugin manager
    new ManagerDesc(REMOTE_API, DEFAULT_REMOTE_API),
    new ManagerDesc(SERVLET_MANAGER, DEFAULT_SERVLET_MANAGER),
    new ManagerDesc(PROXY_MANAGER, DEFAULT_PROXY_MANAGER),
    new ManagerDesc(AUDIT_PROXY_MANAGER, DEFAULT_AUDIT_PROXY_MANAGER),
    // comm layer at end so don't process messages until other services ready
    new ManagerDesc(COMM_MANAGER, DEFAULT_COMM_MANAGER),
    new ManagerDesc(ROUTER_MANAGER, DEFAULT_ROUTER_MANAGER),
    new ManagerDesc(WATCHDOG_SERVICE, DEFAULT_WATCHDOG_SERVICE),
    new ManagerDesc(NODE_MANAGER_STATUS, DEFAULT_NODE_MANAGER_STATUS),
    new ManagerDesc(ARCHIVAL_UNIT_STATUS, DEFAULT_ARCHIVAL_UNIT_STATUS),
    new ManagerDesc(REPOSITORY_STATUS, DEFAULT_REPOSITORY_STATUS),
  };

  // AU-specific manager descriptors.  As each AU is created its managers
  // are started in this order.
  protected static final ManagerDesc[] auManagerDescs = {
    new ManagerDesc(ACTIVITY_REGULATOR, DEFAULT_ACTIVITY_REGULATOR),
    // LockssRepository uses ActivityRegulator
    new ManagerDesc(LOCKSS_REPOSITORY, DEFAULT_LOCKSS_REPOSITORY),
    // HistoryRepository needs no extra managers
    new ManagerDesc(HISTORY_REPOSITORY, DEFAULT_HISTORY_REPOSITORY),
    // NodeManager uses LockssRepository, HistoryRepository, and ActivityRegulator
    new ManagerDesc(NODE_MANAGER, DEFAULT_NODE_MANAGER),
  };

  protected List propUrls = null;

  private boolean daemonInited = false;	// true after all managers inited
  private boolean daemonRunning = false; // true after all managers dtarted
  private Date startDate;
  private long daemonLifetime = DEFAULT_DAEMON_EXIT_AFTER;
  private Deadline timeToExit = Deadline.at(TimeBase.MAX);

  // Map of managerKey -> manager instance. Need to preserve order so
  // managers are started and stopped in the right order.  This does not
  // need to be synchronized.
  protected static SequencedHashMap theManagers = new SequencedHashMap();

  // Maps au to sequenced map of managerKey -> manager instance
  protected static HashMap auManagerMaps = new HashMap();

  // Maps managerKey -> LockssAuManager.Factory instance
  protected static HashMap auManagerFactoryMap = new HashMap();

  protected LockssDaemon(List propUrls) {
    this.propUrls = propUrls;
  }

  // General information accessors

  /**
   * True iff all managers have been inited.
   * @return true iff all managers have been inited */
  public boolean isDaemonInited() {
    return daemonInited;
  }

  /**
   * True if all managers have been started.
   * @return true iff all managers have been started */
  public boolean isDaemonRunning() {
    return daemonRunning;
  }

  /**
   * True if running in debug mode (org.lockss.daemon.debug=true).
   * @return true iff in debug mode */
  public static boolean isDebug() {
    return ConfigManager.getBooleanParam(PARAM_DEBUG, false);
  }

  /** Return the LOCKSS user-agent string.
   * @return the LOCKSS user-agent string. */
  public static String getUserAgent() {
    return LOCKSS_USER_AGENT;
  }


  /** Return the time the daemon started running.
   * @return the time the daemon started running, as a Date
   */
  public Date getStartDate() {
    if (startDate == null) {
      // this happens during testing
      startDate = TimeBase.nowDate();
    }
    return startDate;
  }

  /** Stop the daemon.  Currently only used in testing. */
  public void stopDaemon() {
    stop();
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
    LockssManager mgr = (LockssManager) theManagers.get(managerKey);
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
   * return the hash service instance
   * @return the HashService
   * @throws IllegalArgumentException if the manager is not available.
   */
  public HashService getHashService() {
    return (HashService) getManager(HASH_SERVICE);
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
   * @return {@link org.lockss.daemon.status.StatusService} instance
   * @throws IllegalArgumentException if the manager is not available.
   */
  public StatusService getStatusService() {
    return (StatusService) getManager(STATUS_SERVICE);
  }

  /**
   * return the SystemMetrics instance.
   * @return SystemMetrics instance.
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
   * return the RemoteApi instance.
   * @return RemoteApi instance.
   * @throws IllegalArgumentException if the manager is not available.
   */
  public RemoteApi getRemoteApi() {
    return (RemoteApi) getManager(REMOTE_API);
  }

  /**
   * return the NodeManagerStatus instance.
   * @return NodeManagerStatus instance.
   * @throws IllegalArgumentException if the manager is not available.
   */
  public NodeManagerStatus getNodeManagerStatus() {
    return (NodeManagerStatus) getManager(NODE_MANAGER_STATUS);
  }

  /**
   * return the ArchivalUnitStatus instance.
   * @return ArchivalUnitStatus instance.
   * @throws IllegalArgumentException if the manager is not available.
   */
  public ArchivalUnitStatus getArchivalUnitStatus() {
    return (ArchivalUnitStatus) getManager(ARCHIVAL_UNIT_STATUS);
  }

  // LockssAuManager accessors

  /**
   * Return an AU-specific lockss manager. This will need to be cast to the
   * appropriate class.
   * @param key the name of the manager
   * @param au the AU
   * @return a LockssAuManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public static LockssAuManager getAuManager(String key, ArchivalUnit au) {
    LockssAuManager mgr = null;
    SequencedHashMap auMgrMap =
      (SequencedHashMap)auManagerMaps.get(au);
    if (auMgrMap != null) {
      mgr = (LockssAuManager)auMgrMap.get(key);
    }
    if (mgr == null) {
      log.error(key + " not found for au: " + au);
      throw new IllegalArgumentException("Unavailable au manager:" + key);
    }
    return mgr;
  }

  /**
   * Get Lockss Repository instance
   * @param au the ArchivalUnit
   * @return the LockssRepository
   * @throws IllegalArgumentException if the manager is not available.
   */
  public LockssRepository getLockssRepository(ArchivalUnit au) {
    return (LockssRepository)getAuManager(LOCKSS_REPOSITORY, au);
  }

  /**
   * Return the NodeManager instance
   * @param au the ArchivalUnit
   * @return the NodeManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public NodeManager getNodeManager(ArchivalUnit au) {
    return (NodeManager)getAuManager(NODE_MANAGER, au);
  }


  /**
   * Return the HistoryRepository instance
   * @param au the ArchivalUnit
   * @return the HistoryRepository
   * @throws IllegalArgumentException if the manager is not available.
   */
  public HistoryRepository getHistoryRepository(ArchivalUnit au) {
    return (HistoryRepository)getAuManager(HISTORY_REPOSITORY, au);
  }
  /**
   * Return ActivityRegulator instance
   * @param au the ArchivalUnit
   * @return the ActivityRegulator
   * @throws IllegalArgumentException if the manager is not available.
   */
  public ActivityRegulator getActivityRegulator(ArchivalUnit au) {
    return (ActivityRegulator)getAuManager(ACTIVITY_REGULATOR, au);
  }

  /**
   * Return all ActivityRegulators.
   * @return a list of all ActivityRegulators for all AUs
   */
  public List getAllActivityRegulators() {
    return getAuManagersOfType(ACTIVITY_REGULATOR);
  }

  /**
   * Return all LockssRepositories.
   * @return a list of all LockssRepositories for all AUs
   */
  public List getAllLockssRepositories() {
    return getAuManagersOfType(LOCKSS_REPOSITORY);
  }

  /**
   * Return all NodeManagers.
   * @return a list of all NodeManagers for all AUs
   */
  public List getAllNodeManagers() {
    return getAuManagersOfType(NODE_MANAGER);
  }

  // Daemon start, stop

  /**
   * run the daemon.  Load our properties, initialize our managers, initialize
   * the plugins.
   * @throws Exception if the initialization fails
   */
  protected void runDaemon() throws Exception {

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

  /** Return a string describing the version of the daemon and platform */
  public String getVersionInfo() {
    String vDaemon = BuildInfo.getBuildInfoString();
    String vPlatform = Configuration.getParam(PARAM_PLATFORM_VERSION);
    if (vPlatform != null) {
      vDaemon = vDaemon + ", CD " + vPlatform;
    }
    return vDaemon;
  }

  /**
   * init our configuration and extract any parameters we will use locally
   */
  protected void initProperties() {
    ConfigManager configMgr = ConfigManager.makeConfigManager(propUrls);
    configMgr.initService(this);
    configMgr.startService();
    log.info("Waiting for config");
    configMgr.waitConfig();
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

    // temporary while debugging jvm DNS problem
    if (changedKeys.contains(PARAM_EXERCISE_DNS)) {
      IPAddr.setExerciseDNS(config.getBooleanParam(PARAM_EXERCISE_DNS,
						   DEFAULT_EXERCISE_DNS));
    }

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
      LockssManager mgr = initManager(desc);
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
    stopAllAuManagers();

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
    String managerName = Configuration.getParam(MANAGER_PREFIX + desc.key,
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
    String managerName = Configuration.getParam(MANAGER_PREFIX + desc.key,
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

  // AU specific manager loading, starting, stopping

  /**
   * Start or reconfigure all managers necessary to handle the ArchivalUnit.
   * @param au the ArchivalUnit
   * @param auConfig the AU's confignuration
   */
  public void startOrReconfigureAuManagers(ArchivalUnit au,
					   Configuration auConfig)
      throws Exception {
    SequencedHashMap auMgrMap = (SequencedHashMap)auManagerMaps.get(au);
    if (auMgrMap != null) {
      // If au has a map it's been created, just set new config
      configAuManagers(au, auConfig, auMgrMap);
    } else {
      // create a new map, init, configure and start managers
      auMgrMap = new SequencedHashMap();
      initAuManagers(au, auMgrMap);
      // Store map once all managers inited
      auManagerMaps.put(au, auMgrMap);
      configAuManagers(au, auConfig, auMgrMap);
      try {
	startAuManagers(au, auMgrMap);
      } catch (Exception e) {
	log.warning("Stopping managers for " + au);
	stopAuManagers(au);
	throw e;
      }
    }
  }

  /** Stop the managers for the AU in the reverse order in which they
   * appear in the map */
  public void stopAuManagers(ArchivalUnit au) {
    SequencedHashMap auMgrMap =
      (SequencedHashMap)auManagerMaps.get(au);
    List rkeys = ListUtil.reverseCopy(auMgrMap.sequence());
    for (Iterator iter = rkeys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      LockssAuManager mgr = (LockssAuManager)auMgrMap.get(key);
      try {
	mgr.stopService();
      } catch (Exception e) {
	log.warning("Couldn't stop au manager " + mgr, e);
	// continue to try to stop other managers
      }
    }
    auManagerMaps.remove(au);
  }

  /** Create and init all AU managers for the AU, and associate them with
   * their keys in auMgrMap. */
  private void initAuManagers(ArchivalUnit au, SequencedHashMap auMgrMap)
      throws Exception {
    ManagerDesc descs[] = getAuManagerDescs();
    for (int ix = 0; ix < descs.length; ix++) {
      ManagerDesc desc = descs[ix];
      try {
	LockssAuManager mgr = initAuManager(desc, au);
	auMgrMap.put(desc.key, mgr);
      } catch (Exception e) {
	log.error("Couldn't init AU manager " + desc.key + " for " + au,
		  e);
	// don't try to init remaining managers
	throw e;
      }
    }
  }

  // can be overridden for tests
  protected ManagerDesc[] getAuManagerDescs() {
    return auManagerDescs;
  }

  /** Create and init an AU manager. */
  protected LockssAuManager initAuManager(ManagerDesc desc, ArchivalUnit au)
      throws Exception {
    LockssAuManager mgr = instantiateAuManager(desc, au);
    mgr.initService(this);
    return mgr;
  }

  /** Start the managers for the AU in the order in which they appear in
   * the map.  protected so MockLockssDaemon can override to suppress
   * startService() */
  protected void startAuManagers(ArchivalUnit au, SequencedHashMap auMgrMap)
      throws Exception {
    for (Iterator iter = auMgrMap.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      LockssAuManager mgr = (LockssAuManager)auMgrMap.get(key);
      try {
	mgr.startService();
      } catch (Exception e) {
	log.error("Couldn't start AU manager " + mgr + " for " + au,
		  e);
	// don't try to start remaining managers
	throw e;
      }
    }
  }

  /** (re)configure the au managers */
  private void configAuManagers(ArchivalUnit au, Configuration auConfig,
				SequencedHashMap auMgrMap) {
    for (Iterator iter = auMgrMap.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      LockssAuManager mgr = (LockssAuManager)auMgrMap.get(key);
      try {
	mgr.setAuConfig(auConfig);
      } catch (Exception e) {
	log.error("Error configuring AU manager " + mgr + " for " + au, e);
	// continue after config errors
      }
    }
  }

  /** Instantiate a LockssAuManager, using a LockssAuManager.Factory, which
   * is created is necessary */
  private LockssAuManager instantiateAuManager(ManagerDesc desc,
					       ArchivalUnit au)
      throws Exception {
    String key = desc.key;
    LockssAuManager.Factory factory =
      (LockssAuManager.Factory)auManagerFactoryMap.get(key);
    if (factory == null) {
      factory = instantiateAuFactory(desc);
      auManagerFactoryMap.put(key, factory);
    }
    LockssAuManager mgr = factory.createAuManager(au);
    return mgr;
  }

  /** Instantiate a LockssAuManager.Factory, which is used to create
   * instances of a LockssAuManager */
  private LockssAuManager.Factory instantiateAuFactory(ManagerDesc desc)
      throws Exception {
    String managerName = Configuration.getParam(MANAGER_PREFIX + desc.key,
						desc.defaultClass);
    LockssAuManager.Factory factory;
    try {
      factory = (LockssAuManager.Factory)makeInstance(managerName);
    } catch (ClassNotFoundException e) {
      log.warning("Couldn't load au manager factory class " + managerName);
      if (!managerName.equals(desc.defaultClass)) {
	log.warning("Trying default factory class " + desc.defaultClass);
	factory = (LockssAuManager.Factory)makeInstance(desc.defaultClass);
      } else {
	throw e;
      }
    }
    return factory;
  }

  /**
   * Calls 'stopService()' on all AU managers for all AUs,
   */
  public void stopAllAuManagers() {
    for (Iterator iter = auManagerMaps.keySet().iterator();
	 iter.hasNext(); ) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      log.debug2("Stopping all managers for " + au);
      stopAuManagers(au);
    }
    auManagerMaps.clear();
  }

  /**
   * Return the LockssAuManagers of a particular type.
   * @param managerKey the manager type
   * @return a list of LockssAuManagers
   */
  List getAuManagersOfType(String managerKey) {
    List res = new ArrayList(auManagerMaps.size());
    for (Iterator iter = auManagerMaps.values().iterator();
	 iter.hasNext(); ) {
      SequencedHashMap auMgrMap = (SequencedHashMap)iter.next();
      Object auMgr = auMgrMap.get(managerKey);
      if (auMgr != null) {
	res.add(auMgr);
      }
    }
    return res;
  }

  // Main entry to daemon

  public static void main(String[] args) {
    Vector urls = new Vector();
    LockssDaemon daemon;

    for (int i=0; i<args.length; i++) {
      urls.add(args[i]);
    }

    try {
      daemon = new LockssDaemon(urls);
      daemon.runDaemon();
      // raise priority after starting other threads, so we won't get
      // locked out and fail to exit when told.
      Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

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

}
