/*
 * $Id: LockssDaemon.java,v 1.7 2003-02-24 22:13:41 claire Exp $
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
import org.lockss.util.Logger;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.state.*;
import org.lockss.proxy.*;
import org.lockss.crawler.*;
import org.apache.commons.collections.SequencedHashMap;

/**
 * @author Claire Griffin
 * @version 1.0
 */

public class LockssDaemon {
  private static String PARAM_CACHE_LOCATION = Configuration.PREFIX+ "cacheDir";

  private static String MANAGER_PREFIX = Configuration.PREFIX + "manager.";

  /* the parameter strings that represent our managers */
  public static String HASH_SERVICE = "HashService";
  public static String COMM_MANAGER = "CommManager";
  public static String IDENTITY_MANAGER = "IdentityManager";
  public static String CRAWL_MANAGER = "CrawlManager";
  public static String PLUGIN_MANAGER = "PluginManager";
  public static String POLL_MANAGER = "PollManager";
  public static String LOCKSS_REPOSITORY = "LockssRepository";
  public static String HISTORY_REPOSITORY = "HistoryRepository";
  public static String NODE_MANAGER = "NodeManager";
  public static String PROXY_HANDLER = "ProxyHandler";

  /* the default classes that represent our managers */
  private static String DEFAULT_HASH_SERVICE = "org.lockss.hasher.HashService";
  private static String DEFAULT_COMM_MANAGER = "org.lockss.protocol.LcapComm";
  private static String DEFAULT_IDENTITY_MANAGER
      = "org.lockss.protocol.IdentityManager";
  private static String DEFAULT_CRAWL_MANAGER = "org.lockss.crawler.CrawlManagerImpl";
  private static String DEFAULT_PLUGIN_MANAGER = "org.lockss.plugin.PluginManager";
  private static String DEFAULT_POLL_MANAGER = "org.lockss.poller.PollManager";
  private static String DEFAULT_LOCKSS_REPOSITORY
      = "org.lockss.repository.LockssRepository";
  private static String DEFAULT_HISTORY_REPOSITORY
      = "org.lockss.state.HistoryRepository";
  private static String DEFAULT_NODE_MANAGER = "org.lockss.state.NodeManager";
  private static String DEFAULT_PROXY_HANDLER = "org.lockss.proxy.ProxyHandler";


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
  // which managers are initialized and started.
  private ManagerDesc[] managerDescs = {
    new ManagerDesc(HASH_SERVICE, DEFAULT_HASH_SERVICE),
    new ManagerDesc(COMM_MANAGER, DEFAULT_COMM_MANAGER),
    new ManagerDesc(IDENTITY_MANAGER, DEFAULT_IDENTITY_MANAGER),
    new ManagerDesc(POLL_MANAGER, DEFAULT_POLL_MANAGER),
    new ManagerDesc(LOCKSS_REPOSITORY, DEFAULT_LOCKSS_REPOSITORY),
    new ManagerDesc(HISTORY_REPOSITORY, DEFAULT_HISTORY_REPOSITORY),
    new ManagerDesc(NODE_MANAGER, DEFAULT_NODE_MANAGER),
    new ManagerDesc(CRAWL_MANAGER, DEFAULT_CRAWL_MANAGER),
    new ManagerDesc(PROXY_HANDLER, DEFAULT_PROXY_HANDLER),
    // PluginManager must be last
    new ManagerDesc(PLUGIN_MANAGER, DEFAULT_PLUGIN_MANAGER)
  };

  private static Logger log = Logger.getLogger("RunDaemon");
  protected List propUrls = null;
  private String cacheDir = null;
  private String configDir = null;

  // Need to preserve order so managers are started and stopped in the
  // right order.  This does not need to be synchronized.
  private static Map theManagers = new SequencedHashMap();

  boolean running = false;

  protected LockssDaemon(List propUrls){
    this.propUrls = propUrls;
  }

  public static void main(String[] args) {

    Vector urls = new Vector();

    for (int i=0; i<args.length; i++) {
      urls.add(args[i]);
    }

    try {
      LockssDaemon daemon = new LockssDaemon(urls);
      daemon.runDaemon();
      daemon.stop();
    } catch (Throwable e) {
      System.err.println("Exception thrown in main loop:");
      e.printStackTrace();
    }
  }

  /**
   * Return a lockss manager this will need to be cast to the appropriate class.
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
   * get Lockss Repository instance
   * @param au the ArchivalUnit
   * @return the LockssRepository
   * @throws IllegalArgumentException if the manager is not available.
   */
  public LockssRepository getLockssRepository(ArchivalUnit au) {
    LockssRepository repo = (LockssRepository)getManager(LOCKSS_REPOSITORY);
    return repo.repositoryFactory(au);
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
    NodeManager nodeMan = (NodeManager)getManager(NODE_MANAGER);
    return nodeMan.managerFactory(au);
  }

  /**
   * return the proxy handler instance
   * @return the ProxyHandler
   * @throws IllegalArgumentException if the manager is not available.
  */
  public ProxyHandler getProxyHandler() {
    return (ProxyHandler) getManager(PROXY_HANDLER);
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
   * get cache directory
   * @return get the location of the cache directory
   */
  public String getCacheDir() {
    return cacheDir;
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

    // initialize our properties from the urls given
    initProperties();

    // startup all services
    initManagers();
  }


  /**
   * stop the daemon
   */
  protected void stop() {

    /* stop the managers */
    // tk - should this stop the managers in the reverse order?
    Iterator it = theManagers.values().iterator();
    while(it.hasNext()) {
      LockssManager lm = (LockssManager)it.next();
      lm.stopService();
    }
  }

  /**
   * init our configuration and extract any parameters we will use locally
   */
  protected void initProperties() {
    Configuration.startHandler(propUrls);
    System.err.println("Sleeping so config can get set");
    Configuration.waitConfig();
    System.err.println("Awake");

    // get the properties we're going to store locally
    cacheDir = Configuration.getParam(PARAM_CACHE_LOCATION,
                                      DEFAULT_CACHE_LOCATION);

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
    // now start the managers in the same order in which they were created
    // (theManagers is a SequencedHashMap)
    Iterator it = theManagers.values().iterator();
    while(it.hasNext()) {
      LockssManager lm = (LockssManager)it.next();
      lm.startService();
    }

  }

  /**
   * load the managers with the manager class name
   * @param managerName the class name of the manager to load
   * @return the manager that has been loaded
   * @throws Exception if load fails
   */
  LockssManager loadManager(String managerName) throws Exception {
    try {
      Class manager_class = Class.forName(managerName);
      LockssManager mgr = (LockssManager) manager_class.newInstance();
      // call init on the service
      mgr.initService(this);
      return mgr;
    }
    catch (Exception ex) {
      System.err.println("Unable to instantiate Lockss Manager "+ managerName);
      throw(ex);
    }
  }


}
