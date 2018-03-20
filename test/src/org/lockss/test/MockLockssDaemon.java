/*
 * $Id$
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.util.List;

import org.apache.commons.collections.map.LinkedMap;

import org.lockss.alert.AlertManager;
import org.lockss.account.AccountManager;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.crawler.CrawlManager;
import org.lockss.daemon.*;
import org.lockss.daemon.status.StatusService;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.hasher.HashService;
import org.lockss.mail.MailService;
import org.lockss.metadata.MetadataManager;
import org.lockss.plugin.*;
import org.lockss.truezip.*;
import org.lockss.poller.PollManager;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.proxy.ProxyManager;
import org.lockss.proxy.icp.IcpManager;
import org.lockss.remote.RemoteApi;
import org.lockss.repository.*;
import org.lockss.scheduler.SchedService;
import org.lockss.servlet.*;
import org.lockss.state.*;
import org.lockss.subscription.SubscriptionManager;
import org.lockss.util.*;
import org.lockss.clockss.*;
import org.lockss.entitlement.*;

public class MockLockssDaemon extends LockssDaemon {
  private static Logger log = Logger.getLogger("MockLockssDaemon");

  ResourceManager resourceManager = null;
  WatchdogService wdogService = null;
  MailService mailService = null;
  AlertManager alertManager = null;
  AccountManager accountManager = null;
  RandomManager randomManager = null;
  LockssKeyStoreManager keystoreManager = null;
  HashService hashService = null;
  SchedService schedService = null;
  SystemMetrics systemMetrics = null;
  PollManager pollManager = null;
  PsmManager psmManager = null;
  LcapDatagramComm commManager = null;
  LcapStreamComm scommManager = null;
  LcapDatagramRouter datagramRouterManager = null;
  LcapRouter routerManager = null;
  ProxyManager proxyManager = null;
  ServletManager servletManager = null;
  CrawlManager crawlManager = null;
  RepositoryManager repositoryManager = null;
  NodeManagerManager nodeManagerManager = null;
  PluginManager pluginManager = null;
  MetadataManager metadataManager = null;
  IdentityManager identityManager = null;
  TrueZipManager tzipManager = null;
  StatusService statusService = null;
  RemoteApi remoteApi = null;
  IcpManager icpManager = null;
  ClockssParams clockssParams = null;
  DbManager dbManager = null;
  CounterReportsManager counterReportsManager = null;
  SubscriptionManager subscriptionManager = null;
  Cron cron = null;
  EntitlementRegistryClient entitlementRegistryClient = null;
  private boolean suppressStartAuManagers = true;

  /** Unit tests that need a MockLockssDaemon should use {@link
   * LockssTestCase#getMockLockssDaemon()} rather than calling this
   * directly.  Some utilities (not descended from LockssTestCase) also
   * need one, so this constructor is protected to allow them to directly
   * create an instance (of their own subclass). */
  protected MockLockssDaemon() {
    this(null);
  }

  private MockLockssDaemon(List<String> urls) {
    super(urls);
    ConfigManager mgr = ConfigManager.getConfigManager();
    mgr.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration prevConfig,
					 Configuration.Differences changedKeys) {
	  setConfig(newConfig, prevConfig, changedKeys);
	}
      });
  }

  protected void setConfig(Configuration config, Configuration prevConfig,
			   Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
  }


  /** Does nothing */
  public void startDaemon() throws Exception {
  }

  public void stopDaemon() {
    auManagerMaps.clear();

    wdogService = null;
    hashService = null;
    schedService = null;
    pollManager = null;
    psmManager = null;
    commManager = null;
    scommManager = null;
    proxyManager = null;
    crawlManager = null;
    pluginManager = null;
    metadataManager = null;
    identityManager = null;
    statusService = null;
    icpManager = null;
    dbManager = null;
    counterReportsManager = null;
    subscriptionManager = null;
    cron = null;

    //super.stopDaemon();
  }

  /** Set the testing mode.  (Normally done through config and daemon
   * startup.) */
  public void setTestingMode(String mode) {
    testingMode = mode;
  }

  ManagerDesc findManagerDesc(String key) {
    return findDesc(managerDescs, key);
  }

  ManagerDesc findAuManagerDesc(String key) {
    return findDesc(getAuManagerDescs(), key);
  }

  ManagerDesc findDesc(ManagerDesc[] descs, String key) {
    for(int i=0; i< descs.length; i++) {
      ManagerDesc desc = descs[i];
      if (key.equals(desc.getKey())) {
	return desc;
      }
    }
    return null;
  }

  /** Create a manager instance, mimicking what LockssDaemon does */
  LockssManager newManager(String key) {
    log.debug2("Loading manager: " + key);
    ManagerDesc desc = findManagerDesc(key);
    if (desc == null) {
      throw new LockssAppException("No ManagerDesc for: " + key);
    }
    if (log.isDebug2()) {
      log.debug2("Manager class: " + getManagerClassName(desc));
    }
    try {
      return initManager(desc);
    } catch (Exception e) {
      log.error("Error creating manager", e);
      throw new LockssAppException("Can't load manager: " + e.toString());
    }
  }

  /**
   * return the watchdog service instance
   * @return the WatchdogService
   */
  public WatchdogService getWatchdogService() {
    if (wdogService == null) {
      wdogService = (WatchdogService)newManager(LockssDaemon.WATCHDOG_SERVICE);
      managerMap.put(LockssDaemon.WATCHDOG_SERVICE, wdogService);
    }
    return wdogService;
  }

  /**
   * return the mail manager instance
   * @return the MailService
   */
  public MailService getMailService() {
    if (mailService == null) {
      mailService = new NullMailService();
      managerMap.put(LockssDaemon.MAIL_SERVICE, mailService);
    }
    return mailService;
  }

  /**
   * return the resource manager instance
   * @return the ResourceManager
   */
  public ResourceManager getResourceManager() {
    if (resourceManager == null) {
      resourceManager = (ResourceManager)newManager(RESOURCE_MANAGER);
      managerMap.put(RESOURCE_MANAGER, resourceManager);
    }
    return resourceManager;
  }

  /**
   * return the alert manager instance
   * @return the AlertManager
   */
  public AlertManager getAlertManager() {
    if (alertManager == null) {
      alertManager = new NullAlertManager();
      managerMap.put(LockssDaemon.ALERT_MANAGER, alertManager);
    }
    return alertManager;
  }

  /**
   * return the account manager instance
   * @return the AccountManager
   */
  public AccountManager getAccountManager() {
    if (accountManager == null) {
      accountManager = (AccountManager)newManager(ACCOUNT_MANAGER);
      managerMap.put(LockssDaemon.ACCOUNT_MANAGER, accountManager);
    }
    return accountManager;
  }

  /**
   * return the random manager instance
   * @return the RandomManager
   */
  public RandomManager getRandomManager() {
    if (randomManager == null) {
      randomManager = (RandomManager)newManager(RANDOM_MANAGER);
      managerMap.put(LockssDaemon.RANDOM_MANAGER, randomManager);
    }
    return randomManager;
  }

  /**
   * return the keystore manager instance
   * @return the KeystoreManager
   */
  public LockssKeyStoreManager getKeystoreManager() {
    if (keystoreManager == null) {
      keystoreManager = (LockssKeyStoreManager)newManager(KEYSTORE_MANAGER);
      managerMap.put(LockssDaemon.KEYSTORE_MANAGER, keystoreManager);
    }
    return keystoreManager;
  }

  /**
   * return the hash service instance
   * @return the HashService
   */
  public HashService getHashService() {
    if (hashService == null) {
      hashService = (HashService)newManager(LockssDaemon.HASH_SERVICE);
      managerMap.put(LockssDaemon.HASH_SERVICE, hashService);
    }
    return hashService;
  }

  /**
   * return the sched service instance
   * @return the SchedService
   */
  public SchedService getSchedService() {
    if (schedService == null) {
      schedService = (SchedService)newManager(LockssDaemon.SCHED_SERVICE);
      managerMap.put(LockssDaemon.SCHED_SERVICE, schedService);
    }
    return schedService;
  }

  /**
   * return the SystemMetrics instance
   * @return the SystemMetrics
   */
  public SystemMetrics getSystemMetrics() {
    if (systemMetrics == null) {
      systemMetrics = (SystemMetrics)newManager(LockssDaemon.SYSTEM_METRICS);
      managerMap.put(LockssDaemon.SYSTEM_METRICS, systemMetrics);
    }
    return systemMetrics;
  }

  /**
   * return the poll manager instance
   * @return the PollManager
   */
  public PollManager getPollManager() {
    if (pollManager == null) {
      pollManager = (PollManager)newManager(LockssDaemon.POLL_MANAGER);
      managerMap.put(LockssDaemon.POLL_MANAGER, pollManager);
    }
    return pollManager;
  }

  /**
   * return the psm manager instance
   * @return the PsmManager
   */
  public PsmManager getPsmManager() {
    if (psmManager == null) {
      psmManager = (PsmManager)newManager(LockssDaemon.PSM_MANAGER);
      managerMap.put(LockssDaemon.PSM_MANAGER, psmManager);
    }
    return psmManager;
  }

  /**
   * return the datagram communication manager instance
   * @return the LcapDatagramComm
   */
  public LcapDatagramComm getDatagramCommManager() {
    if (commManager == null) {
      commManager =
	(LcapDatagramComm)newManager(LockssDaemon.DATAGRAM_COMM_MANAGER);
      managerMap.put(LockssDaemon.DATAGRAM_COMM_MANAGER, commManager);
    }
    return commManager;
  }

  /**
   * return the stream communication manager instance
   * @return the LcapStreamComm
   */
  public LcapStreamComm getStreamCommManager() {
    if (scommManager == null) {
      scommManager =
	(LcapStreamComm)newManager(LockssDaemon.STREAM_COMM_MANAGER);
      managerMap.put(LockssDaemon.STREAM_COMM_MANAGER, scommManager);
    }
    return scommManager;
  }

  /**
   * return the datagram router manager instance
   * @return the LcapDatagramRouter
   */
  public LcapDatagramRouter getDatagramRouterManager() {
    if (datagramRouterManager == null) {
      datagramRouterManager =
	(LcapDatagramRouter)newManager(LockssDaemon.DATAGRAM_ROUTER_MANAGER);
      managerMap.put(LockssDaemon.DATAGRAM_ROUTER_MANAGER,
		     datagramRouterManager);
    }
    return datagramRouterManager;
  }

  /**
   * return the router manager instance
   * @return the LcapRouter
   */
  public LcapRouter getRouterManager() {
    if (routerManager == null) {
      routerManager =
	(LcapRouter)newManager(LockssDaemon.ROUTER_MANAGER);
      managerMap.put(LockssDaemon.ROUTER_MANAGER, routerManager);
    }
    return routerManager;
  }

  /**
   * return the proxy manager instance
   * @return the ProxyManager
   */
  public ProxyManager getProxyManager() {
    if (proxyManager == null) {
      proxyManager = (ProxyManager)newManager(LockssDaemon.PROXY_MANAGER);
      managerMap.put(LockssDaemon.PROXY_MANAGER, proxyManager);
    }
    return proxyManager;
  }

  /**
   * return the servlet manager instance
   * @return the ServletManager
   */
  public ServletManager getServletManager() {
    if (servletManager == null) {
      servletManager = (ServletManager)newManager(LockssDaemon.SERVLET_MANAGER);
      managerMap.put(LockssDaemon.SERVLET_MANAGER, servletManager);
    }
    return servletManager;
  }

  /**
   * return the TrueZip manager instance
   * @return the TrueZipManager
   */
  public TrueZipManager getTrueZipManager() {
    if (tzipManager == null) {
      tzipManager = (TrueZipManager)newManager(LockssDaemon.TRUEZIP_MANAGER);
      managerMap.put(LockssDaemon.TRUEZIP_MANAGER, tzipManager);
    }
    return tzipManager;
  }

  /**
   * return the crawl manager instance
   * @return the CrawlManager
   */
  public CrawlManager getCrawlManager() {
    if (crawlManager == null) {
      crawlManager = (CrawlManager)newManager(LockssDaemon.CRAWL_MANAGER);
      managerMap.put(LockssDaemon.CRAWL_MANAGER, crawlManager);
    }
    return crawlManager;
  }

  /**
   * return the node manager status instance
   * @return the TreewalkManager
   */
  public NodeManagerManager getNodeManagerManager() {
    if (nodeManagerManager == null) {
      nodeManagerManager =
	(NodeManagerManager)newManager(LockssDaemon.NODE_MANAGER_MANAGER);
      managerMap.put(LockssDaemon.NODE_MANAGER_MANAGER, nodeManagerManager);
    }
    return nodeManagerManager;
  }

  /**
   * return the repository manager instance
   * @return the RepositoryManager
   */
  public RepositoryManager getRepositoryManager() {
    if (repositoryManager == null) {
      repositoryManager =
	(RepositoryManager)newManager(LockssDaemon.REPOSITORY_MANAGER);
      managerMap.put(LockssDaemon.REPOSITORY_MANAGER, repositoryManager);
    }
    return repositoryManager;
  }

  /**
   * return the plugin manager instance
   * @return the PluginManager
   */
  public PluginManager getPluginManager() {
    if (pluginManager == null) {
      pluginManager = (PluginManager)newManager(LockssDaemon.PLUGIN_MANAGER);
      managerMap.put(LockssDaemon.PLUGIN_MANAGER, pluginManager);
    }
    return pluginManager;
  }

  /**
   * return the metadata manager instance
   * @return the MetadataManager
   */
  public MetadataManager getMetadataManager() {
    if (metadataManager == null) {
      metadataManager = (MetadataManager)newManager(LockssDaemon.METADATA_MANAGER);
      managerMap.put(LockssDaemon.METADATA_MANAGER, metadataManager);
    }
    return metadataManager;
  }

  /**
   * return the Identity Manager
   * @return IdentityManager
   */
  public IdentityManager getIdentityManager() {
    if (identityManager == null) {
      identityManager =
	(IdentityManager)newManager(LockssDaemon.IDENTITY_MANAGER);
      managerMap.put(LockssDaemon.IDENTITY_MANAGER, identityManager);
    }
    return identityManager;
  }

  public boolean hasIdentityManager() {
    return identityManager != null;
  }

  /**
   * return the database manager instance
   * @return the DbManager
   */
  public DbManager getDbManager() {
    if (dbManager == null) {
      dbManager = (DbManager)newManager(LockssDaemon.DB_MANAGER);
      managerMap.put(LockssDaemon.DB_MANAGER, dbManager);
    }
    return dbManager;
  }

  /**
   * return the COUNTER reports manager instance
   * @return the CounterReportsManager
   */
  public CounterReportsManager getCounterReportsManager() {
    if (counterReportsManager == null) {
      counterReportsManager = (CounterReportsManager)newManager(LockssDaemon.COUNTER_REPORTS_MANAGER);
      managerMap.put(LockssDaemon.COUNTER_REPORTS_MANAGER, counterReportsManager);
    }
    return counterReportsManager;
  }

  /**
   * return the subscription manager instance
   * @return the SusbcriptionManager
   */
  public SubscriptionManager getSusbcriptionManager() {
    if (subscriptionManager == null) {
      subscriptionManager = (SubscriptionManager)newManager(LockssDaemon.SUBSCRIPTION_MANAGER);
      managerMap.put(LockssDaemon.SUBSCRIPTION_MANAGER, subscriptionManager);
    }
    return subscriptionManager;
  }

  /**
   * return the cron instance
   * @return the Cron
   */
  public Cron getCron() {
    if (cron == null) {
      cron = (Cron)newManager(LockssDaemon.CRON);
      managerMap.put(LockssDaemon.CRON, cron);
    }
    return cron;
  }

  public StatusService getStatusService() {
    if (statusService == null) {
      statusService = (StatusService)newManager(LockssDaemon.STATUS_SERVICE);
      managerMap.put(LockssDaemon.STATUS_SERVICE, statusService);
    }
    return statusService;
  }

  /**
   * return the RemoteApi instance
   * @return the RemoteApi
   */
  public RemoteApi getRemoteApi() {
    if (remoteApi == null) {
      remoteApi = (RemoteApi)newManager(LockssDaemon.REMOTE_API);
      managerMap.put(LockssDaemon.REMOTE_API, remoteApi);
    }
    return remoteApi;
  }

  /**
   * return the ClockssParams instance
   * @return the ClockssParams
   */
  public ClockssParams getClockssParams() {
    if (clockssParams == null) {
      clockssParams = (ClockssParams)newManager(LockssDaemon.CLOCKSS_PARAMS);
      managerMap.put(LockssDaemon.CLOCKSS_PARAMS, clockssParams);
    }
    return clockssParams;
  }

  private boolean forceIsClockss = false;

  public void setClockss(boolean val) {
    forceIsClockss = val;
  }

  public boolean isClockss() {
    return forceIsClockss || super.isClockss();
  }

  /**
   * Set the datagram CommManager
   * @param commMan the new manager
   */
  public void setDatagramCommManager(LcapDatagramComm commMan) {
    commManager = commMan;
    managerMap.put(LockssDaemon.DATAGRAM_COMM_MANAGER, commManager);
  }

  /**
   * Set the stream CommManager
   * @param scommMan the new manager
   */
  public void setStreamCommManager(LcapStreamComm scommMan) {
    scommManager = scommMan;
    managerMap.put(LockssDaemon.STREAM_COMM_MANAGER, scommManager);
  }

  /**
   * Set the DatagramRouterManager
   * @param datagramRouterMan the new manager
   */
  public void setDatagramRouterManager(LcapDatagramRouter datagramRouterMan) {
    datagramRouterManager = datagramRouterMan;
    managerMap.put(LockssDaemon.DATAGRAM_ROUTER_MANAGER,
		   datagramRouterManager);
  }

  /**
   * Set the RouterManager
   * @param routerMan the new manager
   */
  public void setRouterManager(LcapRouter routerMan) {
    routerManager = routerMan;
    managerMap.put(LockssDaemon.ROUTER_MANAGER, routerManager);
  }

  /**
   * Set the CrawlManager
   * @param crawlMan the new manager
   */
  public void setCrawlManager(CrawlManager crawlMan) {
    crawlManager = crawlMan;
    managerMap.put(LockssDaemon.CRAWL_MANAGER, crawlManager);
  }

  /**
   * Set the RepositoryManager
   * @param repositoryMan the new manager
   */
  public void setRepositoryManager(RepositoryManager repositoryMan) {
    repositoryManager = repositoryMan;
    managerMap.put(LockssDaemon.REPOSITORY_MANAGER, repositoryManager);
  }

  /**
   * Set the NodeManagerManager
   * @param nodeManMan the new manager
   */
  public void setNodeManagerManager(NodeManagerManager nodeManMan) {
    nodeManagerManager = nodeManMan;
    managerMap.put(LockssDaemon.NODE_MANAGER_MANAGER, nodeManMan);
  }

  /**
   * Set the WatchdogService
   * @param wdogService the new service
   */
  public void setWatchdogService(WatchdogService wdogService) {
    this.wdogService = wdogService;
    managerMap.put(LockssDaemon.WATCHDOG_SERVICE, wdogService);
  }

  /**
   * Set the MailService
   * @param mailMan the new manager
   */
  public void setMailService(MailService mailMan) {
    mailService = mailMan;
    managerMap.put(LockssDaemon.MAIL_SERVICE, mailService);
  }

  /**
   * Set the AlertManager
   * @param alertMan the new manager
   */
  public void setAlertManager(AlertManager alertMan) {
    alertManager = alertMan;
    managerMap.put(LockssDaemon.ALERT_MANAGER, alertManager);
  }

  /**
   * Set the AccountManager
   * @param accountMan the new manager
   */
  public void setAccountManager(AccountManager accountMan) {
    accountManager = accountMan;
    managerMap.put(LockssDaemon.ACCOUNT_MANAGER, accountManager);
  }

  /**
   * Set the RandomManager
   * @param randomMan the new manager
   */
  public void setRandomManager(RandomManager randomMan) {
    randomManager = randomMan;
    managerMap.put(LockssDaemon.RANDOM_MANAGER, randomManager);
  }

  /**
   * Set the KeystoreManager
   * @param keystoreMan the new manager
   */
  public void setKeystoreManager(LockssKeyStoreManager keystoreMan) {
    keystoreManager = keystoreMan;
    managerMap.put(LockssDaemon.KEYSTORE_MANAGER, keystoreManager);
  }

  /**
   * Set the HashService
   * @param hashServ the new service
   */
  public void setHashService(HashService hashServ) {
    hashService = hashServ;
    managerMap.put(LockssDaemon.HASH_SERVICE, hashService);
  }

  /**
   * Set the SchedService
   * @param schedServ the new service
   */
  public void setSchedService(SchedService schedServ) {
    schedService = schedServ;
    managerMap.put(LockssDaemon.SCHED_SERVICE, schedService);
  }

  /**
   * Set the IdentityManager
   * @param idMan the new manager
   */
  public void setIdentityManager(IdentityManager idMan) {
    identityManager = idMan;
    managerMap.put(LockssDaemon.IDENTITY_MANAGER, identityManager);
  }

  /**
   * Set the MetadataManager
   * @param metadataMan the new manager
   */
  public void setMetadataManager(MetadataManager metadataMan) {
    metadataManager = metadataMan;
    managerMap.put(LockssDaemon.METADATA_MANAGER, metadataManager);
  }

  /**
   * Set the PluginManager
   * @param pluginMan the new manager
   */
  public void setPluginManager(PluginManager pluginMan) {
    pluginManager = pluginMan;
    managerMap.put(LockssDaemon.PLUGIN_MANAGER, pluginManager);
  }

  /**
   * Set the PollManager
   * @param pollMan the new manager
   */
  public void setPollManager(PollManager pollMan) {
    pollManager = pollMan;
    managerMap.put(LockssDaemon.POLL_MANAGER, pollManager);
  }

  /**
   * Set the ProxyManager
   * @param proxyMgr the new manager
   */
  public void setProxyManager(ProxyManager proxyMgr) {
    proxyManager = proxyMgr;
    managerMap.put(LockssDaemon.PROXY_MANAGER, proxyManager);
  }

  /**
   * Set the ServletManager
   * @param servletMgr the new manager
   */
  public void setServletManager(ServletManager servletMgr) {
    servletManager = servletMgr;
    managerMap.put(LockssDaemon.SERVLET_MANAGER, servletManager);
  }

  /**
   * Set the TrueZipManager
   * @param tzMgr the new manager
   */
  public void setTrueZipManager(TrueZipManager tzMgr) {
    tzipManager = tzMgr;
    managerMap.put(LockssDaemon.TRUEZIP_MANAGER, tzipManager);
  }

  /**
   * Set the DbManager
   * @param dbMan the new manager
   */
  public void setDbManager(DbManager dbMan) {
    dbManager = dbMan;
    managerMap.put(LockssDaemon.DB_MANAGER, dbManager);
  }

  /**
   * Set the CounterReportsManager
   * @param counterReportsMan the new manager
   */
  public void setCounterReportsManager(CounterReportsManager counterReportsMan) {
    counterReportsManager = counterReportsMan;
    managerMap.put(LockssDaemon.COUNTER_REPORTS_MANAGER, counterReportsManager);
  }

  /**
   * Set the SubscriptionManager
   * @param subscriptionMan the new manager
   */
  public void setSubscriptionManager(SubscriptionManager subscriptionMan) {
    subscriptionManager = subscriptionMan;
    managerMap.put(LockssDaemon.SUBSCRIPTION_MANAGER, subscriptionManager);
  }

  /**
   * Set the SystemMetrics
   * @param sysMetrics the new metrics
   */
  public void setSystemMetrics(SystemMetrics sysMetrics) {
    systemMetrics = sysMetrics;
    managerMap.put(LockssDaemon.SYSTEM_METRICS, sysMetrics);
  }

  /**
   * Set the RemoteApi
   * @param sysMetrics the new metrics
   */
  public void setRemoteApi(RemoteApi sysMetrics) {
    remoteApi = sysMetrics;
    managerMap.put(LockssDaemon.REMOTE_API, sysMetrics);
  }

  /**
   * Set the Cron
   * @param cron the new cron
   */
  public void setCron(Cron cron) {
    this.cron = cron;
    managerMap.put(LockssDaemon.CRON, cron);
  }

  /**
   * Set the EntitlementRegistryClient
   * @param pluginMan the new manager
   */
  public void setEntitlementRegistryClient(EntitlementRegistryClient entitlementRegistryClient) {
    this.entitlementRegistryClient = entitlementRegistryClient;
    managerMap.put(LockssDaemon.ENTITLEMENT_REGISTRY_CLIENT, entitlementRegistryClient);
  }

  /**
   * Set the EntitlementRegistryClient
   * @param pluginMan the new manager
   */
  public void setCachedEntitlementRegistryClient(EntitlementRegistryClient entitlementRegistryClient) {
    this.entitlementRegistryClient = entitlementRegistryClient;
    managerMap.put(LockssDaemon.CACHED_ENTITLEMENT_REGISTRY_CLIENT, entitlementRegistryClient);
  }

  // AU managers

  /** Create an AU manager instance, mimicking what LockssDaemon does */
  public LockssAuManager newAuManager(String key, ArchivalUnit au) {
    ManagerDesc desc = findAuManagerDesc(key);
    if (desc == null) {
      throw new LockssAppException("No AU ManagerDesc for: " + key);
    }
    log.debug2("Loading manager: " + desc.getKey() + " for " + au);
    try {
      LockssAuManager mgr = initAuManager(desc, au);
      setAuManager(desc, au, mgr);
      return mgr;
    } catch (Exception e) {
      log.error("Error starting au manager", e);
      throw new LockssAppException("Can't load au manager: " + e.toString());
    }
  }

  public void setAuManager(String key, ArchivalUnit au, LockssAuManager mgr) {
    setAuManager(findAuManagerDesc(key), au, mgr);
  }

  void setAuManager(ManagerDesc desc, ArchivalUnit au, LockssAuManager mgr) {
    LinkedMap auMgrMap = (LinkedMap)auManagerMaps.get(au);
    if (auMgrMap == null) {
      auMgrMap = new LinkedMap();
      auManagerMaps.put(au, auMgrMap);
    }
    auMgrMap.put(desc.getKey(), mgr);
  }

  /** AU managers are normally not started on AU creation.  Call this with
   * false to cause them to be started. */
  public void suppressStartAuManagers(boolean val) {
    suppressStartAuManagers = val;
  }

  /** Overridden to prevent managers from being started.  See {@link
   * #suppressStartAuManagers(boolean)} to cause them to be started. */
  public void startOrReconfigureAuManagers(ArchivalUnit au,
					   Configuration auConfig)
      throws Exception {
    if (!suppressStartAuManagers) {
      super.startOrReconfigureAuManagers(au, auConfig);
    }
  }

  /** For tests that override startOrReconfigureAuManagers and want to
   * conditionally start them. */
  public void reallyStartOrReconfigureAuManagers(ArchivalUnit au,
						 Configuration auConfig)
      throws Exception {
    super.startOrReconfigureAuManagers(au, auConfig);
  }

  /** Return ActivityRegulator for AU */
  public ActivityRegulator getActivityRegulator(ArchivalUnit au) {
    try {
      return super.getActivityRegulator(au);
    } catch (IllegalArgumentException e) {
      return (ActivityRegulator)newAuManager(LockssDaemon.ACTIVITY_REGULATOR,
					     au);
    }
  }

  /** Return LockssRepository for AU */
  public LockssRepository getLockssRepository(ArchivalUnit au) {
    try {
      return super.getLockssRepository(au);
    } catch (IllegalArgumentException e) {
      return (LockssRepository)newAuManager(LockssDaemon.LOCKSS_REPOSITORY,
					    au);
    }
  }

  /** Return NodeManager for AU */
  public NodeManager getNodeManager(ArchivalUnit au) {
    try {
      return super.getNodeManager(au);
    } catch (IllegalArgumentException e) {
      return (NodeManager)newAuManager(LockssDaemon.NODE_MANAGER, au);
    }
  }

  /** Return HistoryRepository for AU */
  public HistoryRepository getHistoryRepository(ArchivalUnit au) {
    try {
      return super.getHistoryRepository(au);
    } catch (IllegalArgumentException e) {
      return (HistoryRepository)newAuManager(LockssDaemon.HISTORY_REPOSITORY,
          au);
    }
  }

  /**
   * Set the ActivityRegulator for a given AU.
   * @param actReg the new regulator
   * @param au the ArchivalUnit
   */
  public void setActivityRegulator(ActivityRegulator actReg, ArchivalUnit au) {
    setAuManager(ACTIVITY_REGULATOR, au, actReg);
  }

  /**
   * Set the LockssRepository for a given AU.
   * @param repo the new repository
   * @param au the ArchivalUnit
   */
  public void setLockssRepository(LockssRepository repo, ArchivalUnit au) {
    setAuManager(LOCKSS_REPOSITORY, au, repo);
  }

  /**
   * Set the NodeManager for a given AU.
   * @param nodeMan the new manager
   * @param au the ArchivalUnit
   */
  public void setNodeManager(NodeManager nodeMan, ArchivalUnit au) {
    setAuManager(NODE_MANAGER, au, nodeMan);
  }

  /**
   * Set the HistoryRepository for a given AU.
   * @param histRepo the new repository
   * @param au the ArchivalUnit
   */
  public void setHistoryRepository(HistoryRepository histRepo, ArchivalUnit au) {
    setAuManager(HISTORY_REPOSITORY, au, histRepo);
  }

  /**
   * <p>Forcibly sets the ICP manager to a new value.</p>
   * @param icpManager A new ICP manager to use.
   */
  public void setIcpManager(IcpManager icpManager) {
    this.icpManager = icpManager;
    managerMap.put(LockssDaemon.ICP_MANAGER, icpManager);
  }

  private boolean daemonInited = false;
  private boolean daemonRunning = false;

  /**
   * @return true iff all managers have been inited
   */
  public boolean isDaemonInited() {
    return daemonInited;
  }

  // need to override this one too, inherited from LockssApp
  public boolean isAppInited() {
    return isDaemonInited();
  }

  /**
   * @return true iff all managers have been started
   */
  public boolean isDaemonRunning() {
    return daemonRunning;
  }

  // need to override this one too, inherited from LockssApp
  public boolean isAppRunning() {
    return isDaemonRunning();
  }

  /** set daemonInited
   * @param val true if inited
   */
  public void setDaemonInited(boolean val) {
    daemonInited = val;
  }

  /** set daemonRunning
   * @param val true if running
   */
  public void setDaemonRunning(boolean val) {
    daemonRunning = val;
  }

  public void setAusStarted(boolean val) {
    if (val) {
      ausStarted.fill();
    } else {
      ausStarted = new OneShotSemaphore();
    }
  }
}
