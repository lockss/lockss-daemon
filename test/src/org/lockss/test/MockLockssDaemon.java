package org.lockss.test;

import org.lockss.app.LockssDaemon;
import java.util.*;
import org.lockss.hasher.*;
import org.lockss.protocol.*;
import org.lockss.poller.*;
import org.lockss.state.*;
import org.lockss.repository.*;
import org.lockss.proxy.*;
import org.lockss.crawler.*;
import org.lockss.plugin.*;
import org.lockss.app.*;

public class MockLockssDaemon
    extends LockssDaemon {
  HashService hashService = null;
  PollManager pollManager = null;
  LcapComm commManager = null;
  LockssRepository lockssRepository = null;
  HistoryRepository historyRepository = null;
  NodeManagerService nodeManagerService = null;
  ProxyHandler proxyHandler = null;
  CrawlManager crawlManager = null;
  PluginManager pluginManager = null;
  IdentityManager identityManager = null;

  public MockLockssDaemon(List urls) {
    super(urls);
  }

  public void startDaemon() throws Exception {
  }

  public void stopDaemon() {
    hashService = null;
    pollManager = null;
    commManager = null;
    lockssRepository = null;
    historyRepository = null;
    nodeManagerService = null;
    proxyHandler = null;
    crawlManager = null;
    pluginManager = null;
    identityManager = null;
  }

  /**
   * return the hash service instance
   * @return the HashService
   */
  public HashService getHashService() {
    if (hashService == null) {
      hashService = new HashService();
      try {
        hashService.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      theManagers.put(LockssDaemon.HASH_SERVICE, hashService);
    }
    return hashService;
  }

  /**
   * return the poll manager instance
   * @return the PollManager
   */
  public PollManager getPollManager() {
    if (pollManager == null) {
      pollManager = new PollManager();
      try {
        pollManager.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      theManagers.put(LockssDaemon.POLL_MANAGER, pollManager);
    }
    return pollManager;
  }

  /**
   * return the communication manager instance
   * @return the LcapComm
   */
  public LcapComm getCommManager() {
    if (commManager == null) {
      commManager = new LcapComm();
      try {
        commManager.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      theManagers.put(LockssDaemon.COMM_MANAGER, hashService);
    }
    return commManager;
  }

  /**
   * get a Lockss Repository instance.  This is broken and not AU specific,
   * because using the proper factory method required Configuration parameters
   * which weren't always set in the tests.
   * @param au the ArchivalUnit (ignored)
   * @return the LockssRepository
   */
  public LockssRepository getLockssRepository(ArchivalUnit au) {
    if (lockssRepository == null) {
      LockssRepositoryImpl impl = new LockssRepositoryImpl();
      try {
        impl.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      lockssRepository = impl;
      theManagers.put(LockssDaemon.LOCKSS_REPOSITORY, lockssRepository);
    }
    return lockssRepository;
  }

  /**
   * return the history repository instance
   * @return the HistoryRepository
   */
  public HistoryRepository getHistoryRepository() {
    if (historyRepository == null) {
      HistoryRepositoryImpl impl = new HistoryRepositoryImpl();
      try {
        impl.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      historyRepository = impl;
      theManagers.put(LockssDaemon.HISTORY_REPOSITORY, historyRepository);
    }
    return historyRepository;
  }

  /**
   * return the node manager instance
   * @param au the ArchivalUnit
   * @return the NodeManager
   */
  public NodeManager getNodeManager(ArchivalUnit au) {
    if (nodeManagerService == null) {
      NodeManagerService nms = new NodeManagerService();
      try {
        nms.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      nodeManagerService = nms;
      theManagers.put(LockssDaemon.NODE_MANAGER_SERVICE, nodeManagerService);
    }
    return nodeManagerService.managerFactory(au);
  }

  /**
   * return the proxy handler instance
   * @return the ProxyHandler
   */
  public ProxyHandler getProxyHandler() {
    if (proxyHandler == null) {
      proxyHandler = new ProxyHandler();
      try {
        proxyHandler.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      theManagers.put(LockssDaemon.PROXY_HANDLER, proxyHandler);
    }
    return proxyHandler;
  }

  /**
   * return the crawl manager instance
   * @return the CrawlManager
   */
  public CrawlManager getCrawlManager() {
    if (crawlManager == null) {
      CrawlManagerImpl impl = new CrawlManagerImpl();
      try {
        impl.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      crawlManager = impl;
      theManagers.put(LockssDaemon.CRAWL_MANAGER, crawlManager);
    }
    return crawlManager;
  }

  /**
   * return the plugin manager instance
   * @return the PluginManager
   */
  public PluginManager getPluginManager() {
    if (pluginManager == null) {
      pluginManager = new PluginManager();
      try {
        pluginManager.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      theManagers.put(LockssDaemon.PLUGIN_MANAGER, pluginManager);
    }
    return pluginManager;
  }

  /**
   * return the Identity Manager
   * @return IdentityManager
   */

  public IdentityManager getIdentityManager() {
    if (identityManager == null) {
      identityManager = new IdentityManager();
      try {
        identityManager.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
    }
    theManagers.put(LockssDaemon.IDENTITY_MANAGER, identityManager);
    return identityManager;
  }

  /**
   * Set the CommManager
   * @param commMan the new manager
   */
  public void setCommManager(LcapComm commMan) {
    commManager = commMan;
    theManagers.put(LockssDaemon.COMM_MANAGER, hashService);
  }

  /**
   * Set the CrawlManager
   * @param crawlMan the new manager
   */
  public void setCrawlManager(CrawlManager crawlMan) {
    crawlManager = crawlMan;
    theManagers.put(LockssDaemon.CRAWL_MANAGER, crawlManager);
  }

  /**
   * Set the HashService
   * @param hashServ the new service
   */
  public void setHashService(HashService hashServ) {
    hashService = hashServ;
    theManagers.put(LockssDaemon.HASH_SERVICE, hashService);
  }

  /**
   * Set the HistoryRepository
   * @param histRepo the new repository
   */
  public void setHistoryRepository(HistoryRepository histRepo) {
    historyRepository = histRepo;
    theManagers.put(LockssDaemon.HISTORY_REPOSITORY, historyRepository);
  }

  /**
   * Set the IdentityManager
   * @param idMan the new manager
   */
  public void setIdentityManager(IdentityManager idMan) {
    identityManager = idMan;
    theManagers.put(LockssDaemon.IDENTITY_MANAGER, identityManager);
  }

  /**
   * Set the LockssRepository
   * @param lockssRepo the new repository
   */
  public void setLockssRepository(LockssRepository lockssRepo) {
    lockssRepository = lockssRepo;
    theManagers.put(LockssDaemon.LOCKSS_REPOSITORY, lockssRepository);
  }

  /**
   * Set the NodeManager
   * @param nodeManService the new manager
   */
  public void setNodeManagerService(NodeManagerService nodeManService) {
    nodeManagerService = nodeManService;
    theManagers.put(LockssDaemon.NODE_MANAGER_SERVICE, nodeManagerService);
  }

  /**
   * Set the PluginManager
   * @param pluginMan the new manager
   */
  public void setPluginManager(PluginManager pluginMan) {
    pluginManager = pluginMan;
    theManagers.put(LockssDaemon.PLUGIN_MANAGER, pluginManager);
  }

  /**
   * Set the PollManager
   * @param pollMan the new manager
   */
  public void setPollManager(PollManager pollMan) {
    pollManager = pollMan;
    theManagers.put(LockssDaemon.POLL_MANAGER, pollManager);
  }

  /**
   * Set the ProxyHandler
   * @param proxyHand the new handler
   */
  public void setProxyHandler(ProxyHandler proxyHand) {
    proxyHandler = proxyHand;
    theManagers.put(LockssDaemon.PROXY_HANDLER, proxyHandler);
  }

}
