/*
 * $Id: LockssDaemon.java,v 1.2 2003-02-06 05:16:06 claire Exp $
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
import org.lockss.protocol.IdentityManager;

/**
 * @author Claire Griffin
 * @version 1.0
 */

public class LockssDaemon {
  private static String PARAM_CACHE_LOCATION = Configuration.PREFIX+ "cacheDir";
  private static String PARAM_PLUGIN_LOCATION = Configuration.PREFIX+ "pluginDir";

  private static String MANAGER_PREFIX = Configuration.PREFIX + "manager.";

  /* the parameter strings that represent our managers */
  static String HASH_SERVICE = "HashService";
  static String COMM_MANAGER = "CommManager";
  static String IDENTITY_MANAGER = "IdentityManager";
  static String CRAWL_MANAGER = "CrawlManager";
  static String PLUGIN_MANAGER = "PluginManager";
  static String POLL_MANAGER = "PollManager";
  static String LOCKSS_REPOSITORY = "LockssRepository";
  static String HISTORY_REPOSITORY = "HistoryRepository";
  static String NODE_MANAGER = "NodeManager";
  static String PROXY_HANDLER = "ProxyHandler";

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
  private static String DEFAULT_PLUGIN_LOCATION = "./plugins";
  private static String DEFAULT_CONFIG_LOCATION = "./config";

  private String[] managerKeys = {HASH_SERVICE, COMM_MANAGER, IDENTITY_MANAGER,
    PLUGIN_MANAGER, POLL_MANAGER, LOCKSS_REPOSITORY, HISTORY_REPOSITORY,
    NODE_MANAGER, CRAWL_MANAGER, PROXY_HANDLER};

  private String[] defaultKeys = {DEFAULT_HASH_SERVICE, DEFAULT_COMM_MANAGER,
    DEFAULT_IDENTITY_MANAGER, DEFAULT_PLUGIN_MANAGER, DEFAULT_POLL_MANAGER,
    DEFAULT_LOCKSS_REPOSITORY, DEFAULT_HISTORY_REPOSITORY, DEFAULT_NODE_MANAGER,
    DEFAULT_CRAWL_MANAGER, DEFAULT_PROXY_HANDLER};

  private static Logger log = Logger.getLogger("RunDaemon");
  protected List propUrls = null;
  private String pluginDir = null;
  private String cacheDir = null;
  private String configDir = null;

  private Hashtable thePlugins = new Hashtable();
  private Hashtable theManagers = new Hashtable();

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
   */
  LockssManager getManager(String managerKey) {
    return (LockssManager) theManagers.get(managerKey);
  }

  /**
   * return the hash service instance
   * @return the HashService
   */
  public HashService getHashService() {
    return (HashService) getManager(HASH_SERVICE);
  }

  /**
   * return the poll manager instance
   * @return the PollManager
   */
  public PollManager getPollManager() {
    return (PollManager) getManager(POLL_MANAGER);
  }

  /**
   * return the communication manager instance
   * @return the LcapComm
   */
  public LcapComm getCommManager() {
    return (LcapComm) getManager(COMM_MANAGER);
  }

  /**
   * get Lockss Repository instance
   * @return the LockssRepository
   */
  public LockssRepository getLockssRepository() {
    return (LockssRepository) getManager(LOCKSS_REPOSITORY);
  }

  /**
   * return the history repository instance
   * @return the HistoryRepository
   */
  public HistoryRepository getHistoryRepository() {
    return (HistoryRepository) getManager(HISTORY_REPOSITORY);
  }

  /**
   * return the node manager instance
   * @return the NodeManager
   */
  public NodeManager getNodeManager() {
    return  (NodeManager) getManager(NODE_MANAGER);
  }

  /**
   * return the proxy handler instance
   * @return the ProxyHandler
   */
  public ProxyHandler getProxyHandler() {
    return (ProxyHandler) getManager(PROXY_HANDLER);
  }

  /**
   * return the crawl manager instance
   * @return the CrawlManager
   */
  public CrawlManager getCrawlManager() {
    return (CrawlManager) getManager(CRAWL_MANAGER);
  }

  /**
   * return the plugin manager instance
   * @return the PluginManager
   */
  public PluginManager getPluginManager() {
    return (PluginManager) getManager(PLUGIN_MANAGER);
  }

  /**
   * return the Identity Manager
   * @return IdentityManager
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

    // load in our "plugins"
    initPlugins();
/*
    running = true;

    while(running)
    ;
*/
  }


  /**
   * stop the daemon
   */
  protected void stop() {

    /* stop the plugins */
    Iterator it = thePlugins.values().iterator();
    while(it.hasNext()) {
      LockssPlugin plugin = (LockssPlugin)it.next();
      plugin.stopPlugin();
    }

    /* stop the managers */
    it = theManagers.values().iterator();
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

    pluginDir = Configuration.getParam(PARAM_PLUGIN_LOCATION,
                                       DEFAULT_PLUGIN_LOCATION);
  }


  /**
   * init all of the managers that support the daemon.
   * @throws Exception if initilization fails
   */
  protected void initManagers() throws Exception {
    String mgr_name;
    LockssManager mgr;

    // There are eight different services we provide
    for(int i=0; i< managerKeys.length; i++) {
      mgr_name = Configuration.getParam(MANAGER_PREFIX + managerKeys[i],
      defaultKeys[i]);
      mgr = loadManager(mgr_name);
      theManagers.put(managerKeys[i], mgr);
    }

    /* we can now safely start our managers */
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

  /**
   * init the plugins in the plugins directory
   */
  protected void initPlugins() {
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
      plugin.initPlugin();
      String id = plugin.getPluginName();
      if(thePlugins.contains(id)) {
        System.err.println("Already have plugin registered for " + id);
        return;
      }
      thePlugins.put(id,plugin);
      getPluginManager().registerArchivalUnit(plugin.getArchivalUnit());
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println("Unable to load Lockss plugin " + pluginName);
    }
    catch (Exception ex) {
      System.err.println("Unable to instatiate Lockss plugin " + pluginName);
    }
  }

}