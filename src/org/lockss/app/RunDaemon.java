/*
 * $Id: RunDaemon.java,v 1.1 2003-01-31 09:47:19 claire Exp $
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

import java.util.Vector;
import org.lockss.util.Logger;
import org.lockss.daemon.Configuration;
import org.lockss.hasher.HashService;
import java.util.List;
import org.lockss.plugin.LockssPlugin;
import org.lockss.plugin.PluginManager;
import java.util.Hashtable;
import org.lockss.poller.PollManager;
import org.lockss.protocol.LcapComm;
import org.lockss.repository.*;
import org.lockss.state.*;
import org.lockss.proxy.*;
import org.lockss.crawler.CrawlManager;
import java.util.Iterator;

/**
 * @author Claire Griffin
 * @version 1.0
 */

public class RunDaemon {
  private static String PARAM_CACHE_LOCATION = Configuration.PREFIX+ "cacheDir";
  private static String PARAM_PLUGIN_LOCATION = Configuration.PREFIX+ "pluginDir";

  private static String MANAGER_PREFIX = Configuration.PREFIX + "manager.";

  /* the parameter strings that represent our managers */
  static String CRAWL_MANAGER = "CrawlManager";
  static String HASH_SERVICE = "HashService";
  static String PLUGIN_MANAGER = "PluginManager";
  static String POLL_MANAGER = "PollManager";
  static String COMM_MANAGER = "CommManager";
  static String LOCKSS_REPOSITORY = "LockssRepository";
  static String HISTORY_REPOSITORY = "HistoryRepository";
  static String NODE_MANAGER = "NodeManager";
  static String PROXY_HANDLER = "ProxyHandler";

  /* the default classes that represent our managers */
  private static String DEFAULT_CRAWL_MANAGER = "org.lockss.crawler.CrawlManager";
  private static String DEFAULT_HASH_SERVICE = "org.lockss.hasher.HashService";
  private static String DEFAULT_PLUGIN_MANAGER = "org.lockss.plugin.PluginManager";
  private static String DEFAULT_POLL_MANAGER = "org.lockss.poller.PollManager";
  private static String DEFAULT_COMM_MANAGER = "org.lockss.protocol.LcapComm";
  private static String DEFAULT_LOCKSS_REPOSITORY
      = "org.lockss.repository.LockssRepository";
  private static String DEFAULT_HISTORY_REPOSITORY
      = "org.lockss.state.HistoryRepository";
  private static String DEFAULT_NODE_MANAGER = "org.lockss.state.NodeManager";
  private static String DEFAULT_PROXY_HANDLER = "org.lockss.proxy.ProxyHandler";


  private static String DEFAULT_CACHE_LOCATION = "./cache";
  private static String DEFAULT_PLUGIN_LOCATION = "./plugins";

  private static Logger log = Logger.getLogger("RunDaemon");
  private List propUrls = null;
  private String pluginDir = null;
  private String cacheDir = null;

  private HashService hashService;
  private PollManager pollManager;
  private LcapComm    commManager;
  private LockssRepository lockssRepository;
  private HistoryRepository historyRepository;
  private NodeManager   nodeManager;
  private ProxyHandler proxyHandler;
  private CrawlManager crawlManager;
  private PluginManager pluginManager;

  private Hashtable thePlugins = new Hashtable();
  private Hashtable theManagers = new Hashtable();

  RunDaemon(List propUrls){
    this.propUrls = propUrls;
  }

  public static void main(String[] args) {
    Vector urls = new Vector();

    for (int i=0; i<args.length; i++) {
      urls.add(args[i]);
    }

    try {
      RunDaemon daemon = new RunDaemon(urls);
      daemon.runDaemon();
    } catch (Throwable e) {
      System.err.println("Exception thrown in main loop:");
      e.printStackTrace();
    }
  }

  /**
   * Return a lockss manager this will need to be cast to the appropriate class.
   * @param managerKey the name of the manager
   * @return a lockss manager
   */
  LockssManager getManager(String managerKey) {
    return (LockssManager) theManagers.get(managerKey);
  }

  /**
   * return the hash service instance
   * @return the HashService
   */
  public HashService getHashService() {
    return hashService;
  }

  /**
   * return the poll manager instance
   * @return the PollManager
   */
  public PollManager getPollManager() {
    return pollManager;
  }

  /**
   * return the communication manager instance
   * @return the LcapComm
   */
  public LcapComm getCommManager() {
    return  commManager;
  }

  /**
   * get Lockss Repository instance
   * @return the LockssRepository
   */
  public LockssRepository getLockssRepository() {
    return lockssRepository;
  }

  /**
   * return the history repository instance
   * @return the HistoryRepository
   */
  public HistoryRepository getHistoryRepository() {
    return historyRepository;
  }

  /**
   * return the node manager instance
   * @return the NodeManager
   */
  public NodeManager getNodeManager() {
    return  nodeManager;
  }

  /**
   * return the proxy handler instance
   * @return the ProxyHandler
   */
  public ProxyHandler getProxyHandler() {
    return proxyHandler;
  }

  /**
   * return the crawl manager instance
   * @return the CrawlManager
   */
  public CrawlManager getCrawlManager() {
    return crawlManager;
  }

  /**
   * return the plugin manager instance
   * @return the PluginManager
   */
  public PluginManager getPluginManager() {
    return pluginManager;
  }

  /**
   * get cache directory
   * @return get the location of the cache directory
   */
  public String getCacheDir() {
    return cacheDir;
  }

  /**
   * run the daemon.  Load our properties, initialize our managers, initialize
   * the plugins.
   */
  void runDaemon() {
    // initialize our properties from the urls given
    initProperties();

    // startup all services
    initManagers();

    // load in our "plugins"
    initPlugins();
  }


  /**
   * stop the daemon
   */
  void stop() {

    /* stop the plugins */
    Iterator it = thePlugins.values().iterator();
    while(it.hasNext()) {
      LockssPlugin plugin = (LockssPlugin)it.next();
      plugin.stop();
    }

    /* stop the managers */
    it = theManagers.values().iterator();
    while(it.hasNext()) {
      LockssManager lm = (LockssManager)it.next();
      lm.stop();
    }
  }

  /**
   * init our configuration and extract any parameters we will use locally
   */
  void initProperties() {
    Configuration.startHandler(propUrls);
    System.err.println("Sleeping so config can get set");
    Configuration.waitConfig();
    System.err.println("Awake");

    // get the properties we're going to store locally
    cacheDir = Configuration.getParam(PARAM_CACHE_LOCATION,
                                      DEFAULT_CACHE_LOCATION);

    pluginDir = Configuration.getParam(PARAM_PLUGIN_LOCATION,
                                       DEFAULT_PLUGIN_LOCATION);
  }


  /**
   * init all of the managers that support the daemon.
   */
  void initManagers() {
    String mgr_name;
    //HashService.start();
    //LcapComm.startComm();
    //pollManager = PollManager.getPollManager();

    // There are eight different services we provide
    mgr_name = Configuration.getParam(MANAGER_PREFIX+CRAWL_MANAGER,
                                             DEFAULT_CRAWL_MANAGER);
    crawlManager = (CrawlManager) loadManager(mgr_name);

    mgr_name = Configuration.getParam(MANAGER_PREFIX+HASH_SERVICE,
                                             DEFAULT_HASH_SERVICE);
    hashService = (HashService) loadManager(mgr_name);

    mgr_name = Configuration.getParam(MANAGER_PREFIX+PLUGIN_MANAGER,
                                             DEFAULT_PLUGIN_MANAGER);
    pluginManager = (PluginManager) loadManager(mgr_name);

    mgr_name = Configuration.getParam(MANAGER_PREFIX+POLL_MANAGER,
                                             DEFAULT_POLL_MANAGER);
    pollManager = (PollManager) loadManager(mgr_name);

    mgr_name = Configuration.getParam(MANAGER_PREFIX+COMM_MANAGER,
                                             DEFAULT_COMM_MANAGER);
    commManager = (LcapComm) loadManager(mgr_name);

    mgr_name = Configuration.getParam(MANAGER_PREFIX+LOCKSS_REPOSITORY,
                                             DEFAULT_LOCKSS_REPOSITORY);
    lockssRepository = (LockssRepository)loadManager(mgr_name);

    mgr_name = Configuration.getParam(MANAGER_PREFIX+HISTORY_REPOSITORY,
                                             DEFAULT_HISTORY_REPOSITORY);
    historyRepository = (HistoryRepository) loadManager(mgr_name);

    mgr_name = Configuration.getParam(MANAGER_PREFIX+NODE_MANAGER,
                                             DEFAULT_NODE_MANAGER);
    nodeManager = (NodeManager) loadManager(mgr_name);

    mgr_name = Configuration.getParam(MANAGER_PREFIX+PROXY_HANDLER,
                                             DEFAULT_PROXY_HANDLER);
    proxyHandler = (ProxyHandler) loadManager(mgr_name);

    /* we can now safely start our managers */
    Iterator it = theManagers.values().iterator();
    while(it.hasNext()) {
      LockssManager lm = (LockssManager)it.next();
      lm.start();
    }

  }


  /**
   * load the managers with the manager class name
   * @param managerName the class name of the manager to load
   * @return the manager that has been loaded
   */
  LockssManager loadManager(String managerName) {
    try {
      Class manager_class = Class.forName(managerName);
      LockssManager mgr = (LockssManager) manager_class.newInstance();
      // call init on the service
      mgr.init(this);
      theManagers.put(managerName, mgr);
      return mgr;
    }
    catch (Exception ex) {
      System.err.println("Unable to instantiate Lockss Manager " + managerName);
      System.exit(-1);
    }
    return null;
  }

  /**
   * init the plugins in the plugins directory
   */
  void initPlugins() {
    /* grab our 3rd party plugins and load them using security manager */
    String[] files = new java.io.File(pluginDir).list();
    for(int i= 0; i < files.length; i++) {
      if(files[i].endsWith(".jar")) {
        loadPlugin(files[i].substring(0,files[i].lastIndexOf(".jar")));
      }
      else {
        loadPlugin(files[i]);
      }
    }
  }

  /**
   * load a plugin with the given class name from somewhere in our classpath
   * @param pluginName the unique name for this plugin
   */
  void loadPlugin(String pluginName) {
    try {
      Class plugin_class = Class.forName(pluginName);
      LockssPlugin plugin = (LockssPlugin) plugin_class.newInstance();
      plugin.init();
      String id = plugin.getPluginId();
      if(thePlugins.contains(id)) {
        System.err.println("Already have plugin registered for " + id);
        return;
      }
      thePlugins.put(id,plugin);
      PluginManager.registerArchivalUnit(plugin.getArchivalUnit());
      plugin.start();
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println("Unable to load Lockss plugin " + pluginName);
    }
    catch (Exception ex) {
      System.err.println("Unable to instatiate Lockss plugin " + pluginName);
    }
  }

}