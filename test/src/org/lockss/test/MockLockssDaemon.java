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

public class MockLockssDaemon extends LockssDaemon {
  HashService hashService = null;
  PollManager pollManager = null;
  LcapComm commManager = null;
  LockssRepository lockssRepository = null;
  HistoryRepository historyRepository = null;
  NodeManager nodeManager = null;
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
  }

  /**
   * return the hash service instance
   * @return the HashService
   */
  public HashService getHashService() {
    if(hashService == null) {
      hashService = new HashService();
      try {
        hashService.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
    }
    return hashService;
  }

  /**
   * return the poll manager instance
   * @return the PollManager
   */
  public PollManager getPollManager() {
    if(pollManager == null) {
      pollManager = new PollManager();
      try {
        pollManager.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
    }
    return pollManager;
  }

  /**
   * return the communication manager instance
   * @return the LcapComm
   */
  public LcapComm getCommManager() {
    if(commManager == null) {
      commManager = new LcapComm();
      try {
        commManager.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
    }
    return commManager;
  }

  /**
   * get Lockss Repository instance
   * @return the LockssRepository
   */
  public LockssRepository getLockssRepository() {
    if(lockssRepository == null) {
      LockssRepositoryImpl impl= new LockssRepositoryImpl();
      try {
        impl.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      lockssRepository = impl;
    }
    return lockssRepository;
  }

  /**
   * return the history repository instance
   * @return the HistoryRepository
   */
  public HistoryRepository getHistoryRepository() {
    if(historyRepository == null) {
      HistoryRepositoryImpl impl = new HistoryRepositoryImpl();
      try {
        impl.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      historyRepository = impl;
    }
    return historyRepository;
  }

  /**
   * return the node manager instance
   * @return the NodeManager
   */
  public NodeManager getNodeManager() {
    if(nodeManager == null) {
      NodeManagerImpl impl = new NodeManagerImpl();
      try {
        impl.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      nodeManager = impl;
    }
    return nodeManager;
  }

  /**
   * return the proxy handler instance
   * @return the ProxyHandler
   */
  public ProxyHandler getProxyHandler() {
    if(proxyHandler == null) {
      proxyHandler = new ProxyHandler();
      try {
        proxyHandler.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
    }
    return proxyHandler;
  }

  /**
   * return the crawl manager instance
   * @return the CrawlManager
   */
  public CrawlManager getCrawlManager() {
    if(crawlManager == null) {
      CrawlManagerImpl impl = new CrawlManagerImpl();
      try {
        impl.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
      crawlManager = impl;
    }
    return crawlManager;
  }

  /**
   * return the plugin manager instance
   * @return the PluginManager
   */
  public PluginManager getPluginManager() {
    if(pluginManager == null) {
      pluginManager = new PluginManager();
      try {
        pluginManager.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
    }
    return pluginManager;
  }

  /**
   * return the Identity Manager
   * @return IdentityManager
   */

  public IdentityManager getIdentityManager() {
    if(identityManager == null) {
      identityManager = new IdentityManager();
      try {
        identityManager.initService(this);
      }
      catch (LockssDaemonException ex) {
      }
    }
    return identityManager;
  }


}