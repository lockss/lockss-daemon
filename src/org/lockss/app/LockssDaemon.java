/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.lang3.*;
import org.lockss.util.*;
import org.lockss.alert.*;
import org.lockss.daemon.*;
import org.lockss.db.DbManager;
import org.lockss.exporter.FetchTimeExportManager;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.account.*;
import org.lockss.hasher.*;
import org.lockss.scheduler.*;
import org.lockss.metadata.MetadataManager;
import org.lockss.plugin.*;
import org.lockss.truezip.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.repository.*;
import org.lockss.state.*;
import org.lockss.subscription.SubscriptionManager;
import org.lockss.proxy.*;
import org.lockss.proxy.icp.IcpManager;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.remote.*;
import org.lockss.clockss.*;
import org.lockss.safenet.*;
import org.apache.commons.collections.map.LinkedMap;

/**
 * The LOCKSS daemon application
 */
public class LockssDaemon extends LockssApp {
  
  private static final Logger log = Logger.getLogger(LockssDaemon.class);

  private static final String PREFIX = Configuration.PREFIX + "daemon.";

  public static final JavaVersion MIN_JAVA_VERSION = JavaVersion.JAVA_1_8;

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

  public final static String PARAM_TESTING_MODE = PREFIX + "testingMode";

  static final String PARAM_DAEMON_DEADLINE_REASONABLE =
    PREFIX + "deadline.reasonable.";
  static final String PARAM_DAEMON_DEADLINE_REASONABLE_PAST =
    PARAM_DAEMON_DEADLINE_REASONABLE + "past";
  static final long DEFAULT_DAEMON_DEADLINE_REASONABLE_PAST = Constants.SECOND;

  static final String PARAM_DAEMON_DEADLINE_REASONABLE_FUTURE =
    PARAM_DAEMON_DEADLINE_REASONABLE + "future";
  static final long DEFAULT_DAEMON_DEADLINE_REASONABLE_FUTURE =
    20 * Constants.WEEK;

  /** List of local IP addresses to which to bind listen sockets for
   * servers (admin ui, content, proxy).  If not set, servers listen on all
   * interfaces.  Does not affect the port on which various servers listen.
   * Changing this requires daemon restart. */
  public static final String PARAM_BIND_ADDRS = PREFIX + "bindAddrs";

  // Parameter keys for daemon managers
  public static final String RANDOM_MANAGER = "RandomManager";
  public static final String ACCOUNT_MANAGER = "AccountManager";
  public static final String KEYSTORE_MANAGER = "KeystoreManager";
  public static final String ACTIVITY_REGULATOR = "ActivityRegulator";
  public static final String ALERT_MANAGER = "AlertManager";
  public static final String HASH_SERVICE = "HashService";
  public static final String TIMER_SERVICE = "TimerService";
  public static final String DATAGRAM_COMM_MANAGER = "DatagramCommManager";
  public static final String STREAM_COMM_MANAGER = "StreamCommManager";
  public static final String ROUTER_MANAGER = "RouterManager";
  public static final String DATAGRAM_ROUTER_MANAGER = "DatagramRouterManager";
  public static final String IDENTITY_MANAGER = "IdentityManager";
  public static final String CRAWL_MANAGER = "CrawlManager";
  public static final String PLUGIN_MANAGER = "PluginManager";
  public static final String METADATA_MANAGER = "MetadataManager";
  public static final String POLL_MANAGER = "PollManager";
  public static final String PSM_MANAGER = "PsmManager";
  public static final String REPOSITORY_MANAGER = "RepositoryManager";
  public static final String LOCKSS_REPOSITORY = "LockssRepository";
  public static final String HISTORY_REPOSITORY = "HistoryRepository";
  public static final String NODE_MANAGER = "NodeManager";
  public static final String CONTENT_SERVLET_MANAGER = "ContentManager";
  public static final String PROXY_MANAGER = "ProxyManager";
  public static final String AUDIT_PROXY_MANAGER = "AuditProxyManager";
  public static final String FAIL_OVER_PROXY_MANAGER = "FailOverProxyManager";
  public static final String SYSTEM_METRICS = "SystemMetrics";
  public static final String REMOTE_API = "RemoteApi";
  public static final String MISC_MANAGER = "MiscSetupManager";
  public static final String URL_MANAGER = "UrlManager";
  public static final String NODE_MANAGER_MANAGER = "NodeManagerManager";
  public static final String REPOSITORY_STATUS = "RepositoryStatus";
  public static final String ARCHIVAL_UNIT_STATUS = "ArchivalUnitStatus";
  public static final String PLATFORM_CONFIG_STATUS = "PlatformConfigStatus";
  public static final String CONFIG_STATUS = "ConfigStatus";
  public static final String OVERVIEW_STATUS = "OverviewStatus";
  public static final String ICP_MANAGER = "IcpManager";
  public static final String CRON = "Cron";
  public static final String CLOCKSS_PARAMS = "ClockssParams";
  public static final String SAFENET_MANAGER = "SafenetManager";
  public static final String TRUEZIP_MANAGER = "TrueZipManager";
  public static final String DB_MANAGER = "DbManager";
  public static final String COUNTER_REPORTS_MANAGER = "CounterReportsManager";
  public static final String SUBSCRIPTION_MANAGER = "SubscriptionManager";
  public static final String FETCH_TIME_EXPORT_MANAGER =
      "FetchTimeExportManager";

  // Manager descriptors.  The order of this table determines the order in
  // which managers are initialized and started.
  protected final ManagerDesc[] managerDescs = {
    new ManagerDesc(MISC_MANAGER, "org.lockss.daemon.MiscSetupManager"),
    new ManagerDesc(RANDOM_MANAGER, "org.lockss.daemon.RandomManager"),
    new ManagerDesc(RESOURCE_MANAGER, DEFAULT_RESOURCE_MANAGER),
    new ManagerDesc(MAIL_SERVICE, DEFAULT_MAIL_SERVICE),
    new ManagerDesc(ALERT_MANAGER, "org.lockss.alert.AlertManagerImpl"),
    new ManagerDesc(STATUS_SERVICE, DEFAULT_STATUS_SERVICE),
    new ManagerDesc(TRUEZIP_MANAGER, "org.lockss.truezip.TrueZipManager"),
    new ManagerDesc(URL_MANAGER, "org.lockss.daemon.UrlManager"),
    new ManagerDesc(TIMER_SERVICE, "org.lockss.util.TimerQueue$Manager"),
    new ManagerDesc(SCHED_SERVICE, DEFAULT_SCHED_SERVICE),
    new ManagerDesc(HASH_SERVICE, "org.lockss.hasher.HashSvcQueueImpl"),
    new ManagerDesc(SYSTEM_METRICS, "org.lockss.daemon.SystemMetrics"),
    // keystore manager must be started before any others that need to
    // access managed keystores
    new ManagerDesc(KEYSTORE_MANAGER,
                    "org.lockss.daemon.LockssKeyStoreManager"),
    new ManagerDesc(ACCOUNT_MANAGER, "org.lockss.account.AccountManager"),
    new ManagerDesc(IDENTITY_MANAGER,
                    "org.lockss.protocol.IdentityManagerImpl"),
    new ManagerDesc(PSM_MANAGER, "org.lockss.protocol.psm.PsmManager"),
    new ManagerDesc(POLL_MANAGER, "org.lockss.poller.PollManager"),
    new ManagerDesc(CRAWL_MANAGER, "org.lockss.crawler.CrawlManagerImpl"),
    new ManagerDesc(REPOSITORY_MANAGER,
                    "org.lockss.repository.RepositoryManager"),
    // start plugin manager after generic services
    new ManagerDesc(PLUGIN_MANAGER, "org.lockss.plugin.PluginManager"),
    // start database manager before any manager that uses it.
    new ManagerDesc(DB_MANAGER, "org.lockss.db.DbManager"),
    // start metadata manager after pluggin manager and database manager.
    new ManagerDesc(METADATA_MANAGER, "org.lockss.metadata.MetadataManager"),
    // start proxy and servlets after plugin manager
    new ManagerDesc(REMOTE_API, "org.lockss.remote.RemoteApi"),
    // Start the COUNTER reports manager.
    new ManagerDesc(COUNTER_REPORTS_MANAGER,
	"org.lockss.exporter.counter.CounterReportsManager"),
    // Start the subscription manager.
    new ManagerDesc(SUBSCRIPTION_MANAGER,
	"org.lockss.subscription.SubscriptionManager"),
    // Start the COUNTER reports manager.
    new ManagerDesc(FETCH_TIME_EXPORT_MANAGER,
	"org.lockss.exporter.FetchTimeExportManager"),
    // NOTE: Any managers that are needed to decide whether a servlet is to be
    // enabled or not (through ServletDescr.isEnabled()) need to appear before
    // the AdminServletManager on the next line.
    new ManagerDesc(SERVLET_MANAGER, "org.lockss.servlet.AdminServletManager"),
    new ManagerDesc(CONTENT_SERVLET_MANAGER,
                    "org.lockss.servlet.ContentServletManager"),
    new ManagerDesc(PROXY_MANAGER, "org.lockss.proxy.ProxyManager"),
    new ManagerDesc(AUDIT_PROXY_MANAGER, "org.lockss.proxy.AuditProxyManager"),
    new ManagerDesc(FAIL_OVER_PROXY_MANAGER ,
                    "org.lockss.proxy.FailOverProxyManager"),
    // comm after other major services so don't process messages until
    // they're ready
    new ManagerDesc(DATAGRAM_COMM_MANAGER,
                    "org.lockss.protocol.LcapDatagramComm"),
    new ManagerDesc(STREAM_COMM_MANAGER,
                    "org.lockss.protocol.BlockingStreamComm"),
    new ManagerDesc(DATAGRAM_ROUTER_MANAGER,
                    "org.lockss.protocol.LcapDatagramRouter"),
    new ManagerDesc(ROUTER_MANAGER,
                    "org.lockss.protocol.LcapRouter"),
    new ManagerDesc(NODE_MANAGER_MANAGER,
                    "org.lockss.state.NodeManagerManager"),
    new ManagerDesc(ICP_MANAGER,
                    "org.lockss.proxy.icp.IcpManager"),
    new ManagerDesc(PLATFORM_CONFIG_STATUS,
                    "org.lockss.config.PlatformConfigStatus"),
    new ManagerDesc(CONFIG_STATUS,
                    "org.lockss.config.ConfigStatus"),
    new ManagerDesc(ARCHIVAL_UNIT_STATUS,
                    "org.lockss.state.ArchivalUnitStatus"),
    new ManagerDesc(REPOSITORY_STATUS,
                    "org.lockss.repository.LockssRepositoryStatus"),
    new ManagerDesc(OVERVIEW_STATUS,
                    "org.lockss.daemon.status.OverviewStatus"),
    new ManagerDesc(CRON, "org.lockss.daemon.Cron"),
    new ManagerDesc(CLOCKSS_PARAMS, "org.lockss.clockss.ClockssParams") {
      public boolean shouldStart() {
        return isClockss();
      }},
    new ManagerDesc(SAFENET_MANAGER, "org.lockss.safenet.CachingEntitlementRegistryClient") {
      public boolean shouldStart() {
        return isSafenet();
      }},
    // watchdog last
    new ManagerDesc(WATCHDOG_SERVICE, DEFAULT_WATCHDOG_SERVICE)
  };

  // AU-specific manager descriptors.  As each AU is created its managers
  // are started in this order.
  protected static final ManagerDesc[] auManagerDescs = {
    new ManagerDesc(ACTIVITY_REGULATOR,
                    "org.lockss.daemon.ActivityRegulator$Factory"),
    // LockssRepository uses ActivityRegulator
    new ManagerDesc(LOCKSS_REPOSITORY,
                    "org.lockss.repository.LockssRepositoryImpl$Factory"),
    // HistoryRepository needs no extra managers
    new ManagerDesc(HISTORY_REPOSITORY,
                    "org.lockss.state.HistoryRepositoryImpl$Factory"),
    // NodeManager uses LockssRepository, HistoryRepository, and
    // ActivityRegulator
    new ManagerDesc(NODE_MANAGER, "org.lockss.state.NodeManagerImpl$Factory"),
  };

  // Maps au to sequenced map of managerKey -> manager instance
  protected HashMap<ArchivalUnit,Map<String,LockssAuManager>> auManagerMaps = 
      new HashMap<ArchivalUnit,Map<String,LockssAuManager>>();

  // Maps managerKey -> LockssAuManager.Factory instance
  protected HashMap<String,LockssAuManager.Factory> auManagerFactoryMap = 
      new HashMap<String,LockssAuManager.Factory>();

  private static LockssDaemon theDaemon;
  private boolean isClockss;
  private boolean isSafenet;
  protected String testingMode;

  protected LockssDaemon(List<String> propUrls) {
    super(propUrls);
    theDaemon = this;
  }

  protected LockssDaemon(List<String> propUrls, String groupNames) {
    super(propUrls, groupNames);
    theDaemon = this;
  }

  protected ManagerDesc[] getManagerDescs() {
    return managerDescs;
  }

  // General information accessors

  /**
   * True iff all managers have been inited.
   * @return true iff all managers have been inited */
  public boolean isDaemonInited() {
    return isAppInited();
  }

  /**
   * True if all managers have been started.
   * @return true iff all managers have been started */
  public boolean isDaemonRunning() {
    return isAppRunning();
  }

  /**
   * Return the LockssDaemon instance
   */
  public static LockssDaemon getLockssDaemon() {
    return (LockssDaemon)theApp;
  }

  /** Return the LOCKSS user-agent string.
   * @return the LOCKSS user-agent string. */
  public static String getUserAgent() {
    return LOCKSS_USER_AGENT;
  }

  /** Return the current testing mode. */
  public String getTestingMode() {
    return testingMode;
  }

  /**
   * True if running as a CLOCKSS daemon
   */
  public boolean isClockss() {
    return isClockss;
  }

  /**
   * Convenience method returns {@link
   * ClockssParams#isDetectSubscription()}
   */
  public boolean isDetectClockssSubscription() {
    return isClockss() && getClockssParams().isDetectSubscription();
  }

  /**
   * True if running as a Safenet daemon
   */
  public boolean isSafenet() {
    return isSafenet;
  }

  /** Stop the daemon.  Currently only used in testing. */
  public void stopDaemon() {
    stopApp();
  }

  // LockssManager accessors

  /**
   * return the alert manager instance
   * @return the AlertManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public AlertManager getAlertManager() {
    return (AlertManager)getManager(ALERT_MANAGER);
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
   * return the psm manager instance
   * @return the PsmManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public PsmManager getPsmManager() {
    return (PsmManager) getManager(PSM_MANAGER);
  }

  /**
   * return the datagram communication manager instance
   * @return the LcapDatagramComm
   * @throws IllegalArgumentException if the manager is not available.
   */
  public LcapDatagramComm getDatagramCommManager()  {
    return (LcapDatagramComm) getManager(DATAGRAM_COMM_MANAGER);
  }

  /**
   * return the stream communication manager instance
   * @return the LcapStreamComm
   * @throws IllegalArgumentException if the manager is not available.
   */
  public LcapStreamComm getStreamCommManager()  {
    return (LcapStreamComm) getManager(STREAM_COMM_MANAGER);
  }

  /**
   * return the datagram router manager instance
   * @return the LcapDatagramRouter
   * @throws IllegalArgumentException if the manager is not available.
   */
  public LcapDatagramRouter getDatagramRouterManager()  {
    return (LcapDatagramRouter) getManager(DATAGRAM_ROUTER_MANAGER);
  }

  /**
   * return the communication router manager instance
   * @return the LcapDatagramRouter
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
   * return the crawl manager instance
   * @return the CrawlManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public CrawlManager getCrawlManager() {
    return (CrawlManager) getManager(CRAWL_MANAGER);
  }

  /**
   * return the repository manager instance
   * @return the RepositoryManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public RepositoryManager getRepositoryManager()  {
    return (RepositoryManager)getManager(REPOSITORY_MANAGER);
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
   * return the metadata manager instance
   * @return the MetadataManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public MetadataManager getMetadataManager() {
    return (MetadataManager) getManager(METADATA_MANAGER);
  }

  /**
   * return the Account Manager
   * @return AccountManager
   * @throws IllegalArgumentException if the manager is not available.
   */

  public AccountManager getAccountManager() {
    return (AccountManager) getManager(ACCOUNT_MANAGER);
  }

  /**
   * return the Random Manager
   * @return RandomManager
   * @throws IllegalArgumentException if the manager is not available.
   */

  public RandomManager getRandomManager() {
    return (RandomManager) getManager(RANDOM_MANAGER);
  }

  /**
   * return the Keystore Manager
   * @return KeystoreManager
   * @throws IllegalArgumentException if the manager is not available.
   */

  public LockssKeyStoreManager getKeystoreManager() {
    return (LockssKeyStoreManager) getManager(KEYSTORE_MANAGER);
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
   * <p>Retrieves the ICP manager.</p>
   * @return The ICP manager instance.
   * @throws IllegalArgumentException if the manager is not available.
   */
  public IcpManager getIcpManager() {
    return (IcpManager)getManager(ICP_MANAGER);
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
   * return the NodeManagerManager instance.
   * @return NodeManagerManager instance.
   * @throws IllegalArgumentException if the manager is not available.
   */
  public NodeManagerManager getNodeManagerManager() {
    return (NodeManagerManager) getManager(NODE_MANAGER_MANAGER);
  }

  /**
   * return the ArchivalUnitStatus instance.
   * @return ArchivalUnitStatus instance.
   * @throws IllegalArgumentException if the manager is not available.
   */
  public ArchivalUnitStatus getArchivalUnitStatus() {
    return (ArchivalUnitStatus) getManager(ARCHIVAL_UNIT_STATUS);
  }

  /**
   * return TrueZipManager instance
   * @return the TrueZipManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public TrueZipManager getTrueZipManager() {
    return (TrueZipManager)getManager(TRUEZIP_MANAGER);
  }

  /**
   * Provides the database manager instance.
   * 
   * @return a DbManager with the database manager instance.
   * @throws IllegalArgumentException
   *           if the manager is not available.
   */
  public DbManager getDbManager() {
    return (DbManager) getManager(DB_MANAGER);
  }

  /**
   * Provides the COUNTER reports manager.
   * 
   * @return a CounterReportsManager with the COUNTER reports manager.
   * @throws IllegalArgumentException
   *           if the manager is not available.
   */
  public CounterReportsManager getCounterReportsManager() {
    return (CounterReportsManager) getManager(COUNTER_REPORTS_MANAGER);
  }

  /**
   * Provides the subscription manager.
   * 
   * @return a SubscriptionManager with the subscription manager.
   * @throws IllegalArgumentException
   *           if the manager is not available.
   */
  public SubscriptionManager getSubscriptionManager() {
    return (SubscriptionManager) getManager(SUBSCRIPTION_MANAGER);
  }

  /**
   * Provides the fetch time export manager.
   * 
   * @return a FetchTimeExportManager with the fetch time export manager.
   * @throws IllegalArgumentException
   *           if the manager is not available.
   */
  public FetchTimeExportManager getFetchTimeExportManager() {
    return (FetchTimeExportManager) getManager(FETCH_TIME_EXPORT_MANAGER);
  }

  /**
   * return the ClockssParams instance.
   * @return ClockssParams instance.
   * @throws IllegalArgumentException if the manager is not available.
   */
  public ClockssParams getClockssParams() {
    return (ClockssParams) getManager(CLOCKSS_PARAMS);
  }

  /**
   * return the EntitlementRegistryClient instance.
   * @return EntitlementRegistryClient instance.
   * @throws IllegalArgumentException if the manager is not available.
   */
  public EntitlementRegistryClient getEntitlementRegistryClient() {
    return (EntitlementRegistryClient) getManager(SAFENET_MANAGER);
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
  public static LockssAuManager getStaticAuManager(String key,
                                                   ArchivalUnit au) {
    return theDaemon.getAuManager(key, au);
  }

  /**
   * Return an AU-specific lockss manager. This will need to be cast to the
   * appropriate class.
   * @param key the name of the manager
   * @param au the AU
   * @return a LockssAuManager
   * @throws IllegalArgumentException if the manager is not available.
   */
  public LockssAuManager getAuManager(String key, ArchivalUnit au) {
    LockssAuManager mgr = null;
    LinkedMap auMgrMap =
      (LinkedMap)auManagerMaps.get(au);
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
  public List<ActivityRegulator> getAllActivityRegulators() {
    return getAuManagersOfType(ACTIVITY_REGULATOR);
  }

  /**
   * Return all LockssRepositories.
   * @return a list of all LockssRepositories for all AUs
   */
  public List<LockssRepository> getAllLockssRepositories() {
    return getAuManagersOfType(LOCKSS_REPOSITORY);
  }

  /**
   * Return all NodeManagers.
   * @return a list of all NodeManagers for all AUs
   */
  public List<NodeManager> getAllNodeManagers() {
    return getAuManagersOfType(NODE_MANAGER);
  }

  // AU specific manager loading, starting, stopping

  /**
   * Start or reconfigure all managers necessary to handle the ArchivalUnit.
   * @param au the ArchivalUnit
   * @param auConfig the AU's confignuration
   */
  @SuppressWarnings("unchecked")
  public void startOrReconfigureAuManagers(ArchivalUnit au,
                                           Configuration auConfig)
      throws Exception {
    LinkedMap auMgrMap = (LinkedMap)auManagerMaps.get(au);
    if (auMgrMap != null) {
      // If au has a map it's been created, just set new config
      configAuManagers(au, auConfig, auMgrMap);
    } else {
      // create a new map, init, configure and start managers
      auMgrMap = new LinkedMap();
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
    LinkedMap auMgrMap =
      (LinkedMap)auManagerMaps.get(au);
    if (auMgrMap != null) {
      @SuppressWarnings("unchecked")
      List<String> rkeys = ListUtil.reverseCopy(auMgrMap.asList());
      for (String key : rkeys) {
        LockssAuManager mgr = (LockssAuManager)auMgrMap.get(key);
        try {
          mgr.stopService();
        } catch (Exception e) {
          log.warning("Couldn't stop au manager " + mgr, e);
          // continue to try to stop other managers
        }
      }
    }
    auManagerMaps.remove(au);
  }

  /** Create and init all AU managers for the AU, and associate them with
   * their keys in auMgrMap. */
  private void initAuManagers(ArchivalUnit au, LinkedMap auMgrMap)
      throws Exception {
    ManagerDesc descs[] = getAuManagerDescs();
    for (int ix = 0; ix < descs.length; ix++) {
      ManagerDesc desc = descs[ix];
      if (desc.shouldStart()) {
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
  }

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
  protected void startAuManagers(ArchivalUnit au, Map<String,? extends LockssAuManager> auMgrMap)
      throws Exception {
    for (LockssAuManager mgr : auMgrMap.values()) {
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
                                Map<String,? extends LockssAuManager> auMgrMap) {
    for (LockssAuManager mgr : auMgrMap.values()) {
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
    String managerName = CurrentConfig.getParam(MANAGER_PREFIX + desc.key,
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
    ArchivalUnit au;
    while ((au = CollectionUtil.getAnElement(auManagerMaps.keySet())) != null) {
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
  @SuppressWarnings("unchecked")
  <T extends LockssAuManager> List<T> getAuManagersOfType(String managerKey) {
    List<T> res = new ArrayList<T>(auManagerMaps.size());
    for (Map<String,LockssAuManager> auMgrMap : auManagerMaps.values()) {
      Object auMgr = auMgrMap.get(managerKey);
      if (auMgr != null) {
        res.add((T)auMgr);
      }
    }
    return res;
  }

  // Daemon start, stop

  protected OneShotSemaphore ausStarted = new OneShotSemaphore();

  protected void startDaemon() throws Exception {
    startApp();

    // Install loadable plugin support
    getPluginManager().startLoadablePlugins();

    log.info("Started");
    ausStarted.fill();

    AlertManager alertMgr = getAlertManager();
    alertMgr.raiseAlert(Alert.cacheAlert(Alert.DAEMON_STARTED),
			"LOCKSS daemon " +
			ConfigManager.getDaemonVersion().displayString() +
			" started");
  }


  /**
   * Stop the daemon, by stopping the managers in the reverse order of
   * starting.
   */
  protected void stop() {
    appRunning = false;

    // stop all au-specific managers
    stopAllAuManagers();

    super.stop();
  }

  /** Wait until the initial set of AUs have been started.  This must be
   * called only from your own thread (<i>eg</i>, not the startup
   * thread.) */
  public void waitUntilAusStarted() throws InterruptedException {
    ausStarted.waitFull(Deadline.MAX);
  }

  /** Return true if all AUs have been started */
  public boolean areAusStarted() {
    return ausStarted.isFull();
  }

  protected void setConfig(Configuration config, Configuration prevConfig,
                           Configuration.Differences changedKeys) {

    if (changedKeys.contains(PARAM_DAEMON_DEADLINE_REASONABLE)) {
      long maxInPast =
        config.getTimeInterval(PARAM_DAEMON_DEADLINE_REASONABLE_PAST,
                               DEFAULT_DAEMON_DEADLINE_REASONABLE_PAST);
      long maxInFuture =
        config.getTimeInterval(PARAM_DAEMON_DEADLINE_REASONABLE_FUTURE,
                               DEFAULT_DAEMON_DEADLINE_REASONABLE_FUTURE);
      Deadline.setReasonableDeadlineRange(maxInPast, maxInFuture);
    }
    testingMode = config.get(PARAM_TESTING_MODE);
    String proj = ConfigManager.getPlatformProject();
    isClockss = "clockss".equalsIgnoreCase(proj);
    isSafenet = "safenet".equalsIgnoreCase(proj);

    super.setConfig(config, prevConfig, changedKeys);
  }

  /**
   * Parse and handle command line arguments.
   */
  protected static StartupOptions getStartupOptions(String[] args) {
    List<String> propUrls = new ArrayList<String>();
    String groupNames = null;

    // True if named command line arguments are being passed to
    // the daemon at startup.  Otherwise, just treat the command
    // line arguments as if they were a list of URLs, for backward
    // compatibility and testing.
    boolean useNewSyntax = false;

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals(StartupOptions.OPTION_GROUP)
	  && i < args.length - 1) {
        groupNames = args[++i];
        useNewSyntax = true;
      }
      else if (args[i].equals(StartupOptions.OPTION_PROPURL)
	       && i < args.length - 1) {
        // TODO: If not available, keep selecting prop files to load
        // until one is loaded, or the list is exhausted.
        // For now, just select one at random.
        Vector<String> v = StringUtil.breakAt(args[++i], ';', -1, true, true);
        int idx = (int)(Math.random() * v.size());
        propUrls.add(v.get(idx));
        useNewSyntax = true;
      }
      else if (args[i].equals("-s")) {
	SslUtil.logCryptoProviders(true);
      }
    }

    if (!useNewSyntax) {
      propUrls = Arrays.asList(args);
    }

    return new StartupOptions(propUrls, groupNames);
  }


  /** ImageIO gets invoked on user-supplied content (by (nyi) format
   * conversion and PDFBox).  Disable native code libraries to avoid any
   * possibility of exploiting vulnerabilities */
  public static String IMAGEIO_DISABLE_NATIVE_CODE =
    "com.sun.media.imageio.disableCodecLib";

  // static so can run before instantiating this class, which causes more
  // classes to be loaded
  protected static void setSystemProperties() {
    System.setProperty(IMAGEIO_DISABLE_NATIVE_CODE, "true");
  }

  /**
   * Main entry to the daemon.  Startup arguments:
   *
   * -p url1
   *     Load properties from url1
   * -p url1 -p url2;url3;url4
   *     Load properties from url1 AND from one of
   *     (url2 | url3 | url4)
   * -g group_name[;group_2;group_3]
   *     Set the daemon groups.  Multiple groups separated by semicolon.
   */
  public static void main(String[] args) {
    LockssDaemon daemon;
    if (!SystemUtils.isJavaVersionAtLeast(MIN_JAVA_VERSION)) {
      System.err.println("LOCKSS requires at least Java " + MIN_JAVA_VERSION +
                         ", this is " + SystemUtils.JAVA_VERSION +
                         ", exiting.");
      System.exit(Constants.EXIT_CODE_JAVA_VERSION);
    }
    if (!TimeZoneUtil.isBasicTimeZoneDataAvailable()) {
      System.err.println("Basic time zone data unavailable, exiting.");
      System.exit(Constants.EXIT_INVALID_TIME_ZONE_DATA);
    }

    StartupOptions opts = getStartupOptions(args);

    setSystemProperties();

    try {
      daemon = new LockssDaemon(opts.getPropUrls(),
                                opts.getGroupNames());
      daemon.startDaemon();
      // raise priority after starting other threads, so we won't get
      // locked out and fail to exit when told.
      Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

    } catch (ResourceUnavailableException e) {
      log.error("Exiting because required resource is unavailable", e);
      System.exit(Constants.EXIT_CODE_RESOURCE_UNAVAILABLE);
      return;                           // compiler doesn't know that
                                        // System.exit() doesn't return
    } catch (Throwable e) {
      log.error("Exception thrown in main loop", e);
      System.exit(Constants.EXIT_CODE_EXCEPTION_IN_MAIN);
      return;                           // compiler doesn't know that
                                        // System.exit() doesn't return
    }
    if (CurrentConfig.getBooleanParam(PARAM_APP_EXIT_IMM,
                                      DEFAULT_APP_EXIT_IMM)) {
      try {
        daemon.stop();
      } catch (RuntimeException e) {
        // ignore errors stopping daemon
      }
      System.exit(Constants.EXIT_CODE_NORMAL);
    }
    daemon.keepRunning();
    log.info("Exiting because time to die");
    System.exit(Constants.EXIT_CODE_NORMAL);
  }

  /**
   * Command line startup options container.
   * Currently supports propUrl (-p) and daemon groups (-g)
   * parameters.
   */
  static class StartupOptions {

    public static final String OPTION_PROPURL = "-p";
    public static final String OPTION_GROUP = "-g";

    private String groupNames;
    private List<String> propUrls;

    public StartupOptions(List<String> propUrls, String groupNames) {
      this.propUrls = propUrls;
      this.groupNames = groupNames;
    }

    public List<String> getPropUrls() {
      return propUrls;
    }

    public String getGroupNames() {
      return groupNames;
    }
  }
}
