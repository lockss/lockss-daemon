/*
 * $Id: PluginManager.java,v 1.43 2003-08-02 00:16:05 eaalto Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.poller.*;
import org.lockss.poller.PollSpec;

/**
 * Plugin global functionality
 *
 * @author  TAL
 * @version 0.0
 */
public class PluginManager extends BaseLockssManager {
  public static final String PARAM_AU_TREE = Configuration.PREFIX + "au";
  private static String PARAM_PLUGIN_LOCATION =
    Configuration.PREFIX + "platform.pluginDir";

  private static Logger log = Logger.getLogger("PluginMgr");

  private ConfigManager configMgr;
  private StatusService statusSvc;
  private String pluginDir = null;

  private Map pluginMap = new HashMap(); //maps plugin keys(not ids) to plugins
  private Map auMap = new HashMap();

  public PluginManager() {
  }

  /* ------- LockssManager implementation ------------------ */
  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    configMgr = getDaemon().getConfigManager();
    statusSvc = getDaemon().getStatusService();
    statusSvc.registerStatusAccessor("AUS", new Status(this));
    initPluginRegistry();
    resetConfig();
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // tk - checkpoint if nec.
    for (Iterator iter = pluginMap.values().iterator(); iter.hasNext(); ) {
      Plugin plugin = (Plugin)iter.next();
      plugin.stopPlugin();
    }
    super.stopService();
  }

  Configuration prevAllPlugs = ConfigManager.EMPTY_CONFIGURATION;

  protected void setConfig(Configuration config, Configuration oldConfig,
			   Set changedKeys) {
    pluginDir = config.get(PARAM_PLUGIN_LOCATION);
    // Don't load and start plugins until the daemon is running.
    // Because we don't necessarily process the config, we must keep track
    // of the previous config ourselves.
    if (isDaemonInited()) {
      Configuration allPlugs = config.getConfigTree(PARAM_AU_TREE);
      for (Iterator iter = allPlugs.nodeIterator(); iter.hasNext(); ) {
	String pluginKey = (String)iter.next();
	log.debug("Configuring plugin key: " + pluginKey);
	Configuration pluginConf = allPlugs.getConfigTree(pluginKey);
	Configuration prevPluginConf = prevAllPlugs.getConfigTree(pluginKey);

	configurePlugin(pluginKey, pluginConf, prevPluginConf);
      }
      prevAllPlugs = allPlugs;
    }
  }

  /** Convert plugin property key to plugin class name. */
  public static String pluginNameFromKey(String key) {
    return StringUtil.replaceString(key, "|", ".");
  }

  /** Convert plugin class name to key suitable for property file. */
  public static String pluginKeyFromName(String className) {
    return StringUtil.replaceString(className, ".", "|");
  }

  /** Convert plugin id to key suitable for property file.  Plugin id is
   * currently the same as plugin class name, but that may change. */
  public static String pluginKeyFromId(String id) {
    // tk - needs to do real mapping from IDs obtained from all available
    // plugins.
    return StringUtil.replaceString(id, ".", "|");
  }

  /**
   * Return a unique identifier for an au based on its plugin id and defining
   * properties.
   *
   * @return a unique identifier for an au based on its plugin id and defining
   * properties.
   * @param pluginId plugin id (with . not escaped)
   * @param auDefiningProps defining properties for the au
   * {@see Plugin.getDefiningConfigKeys}
   */
  public static String generateAUId(String pluginId, Properties auDefProps) {
    return generateAUId(pluginId,
			PropUtil.propsToCanonicalEncodedString(auDefProps));
  }

  static String generateAUId(String pluginId, String auKey) {
    return pluginKeyFromId(pluginId)+"&"+auKey;
  }

  static String configKeyFromAUId(String auid) {
    return StringUtil.replaceString(auid, "&", ".");

  }

  /**
   * Return the plugin with the given id.
   * @param pluginId the plugin id
   * @return the plugin or null
   */
  public Plugin getPlugin(String pluginKey) {
    return (Plugin)pluginMap.get(pluginKey);
  }

  private void configurePlugin(String pluginKey, Configuration pluginConf,
			       Configuration oldPluginConf) {
    boolean pluginOk = ensurePluginLoaded(pluginKey);
    Plugin plugin = getPlugin(pluginKey);
    for (Iterator iter = pluginConf.nodeIterator(); iter.hasNext(); ) {
      String auKey = (String)iter.next();
      if (pluginOk) {
	try {
	  Configuration auConf = pluginConf.getConfigTree(auKey);
	  Configuration oldAuConf = oldPluginConf.getConfigTree(auKey);
	  if (!auConf.equals(oldAuConf)) {
	    log.debug("Configuring AU id: " + auKey);
	    configureAU(plugin, auConf, generateAUId(pluginKey, auKey));
	  } else {
	    log.debug("Not configuring AU id: " + auKey +
		      ", already configured");
	  }
	} catch (ArchivalUnit.ConfigurationException e) {
	  log.error("Failed to configure AU " + auKey, e);
	} catch (Exception e) {
	  log.error("Unexpected exception configuring AU " + auKey, e);
	}
      } else {
	log.warning("Not configuring AU " + auKey);
      }
    }
  }

  void configureAU(Plugin plugin, Configuration auConf, String auId)
      throws ArchivalUnit.ConfigurationException {
    try {
      ArchivalUnit au = plugin.configureAU(auConf,
					   (ArchivalUnit)auMap.get(auId));
      log.debug("Configured AU " + au);
      try {
	getDaemon().startAUManagers(au);
      } catch (Exception e) {
	throw new
	  ArchivalUnit.ConfigurationException("Couldn't start AU processes",
					      e);
      }
      log.debug("putAuMap(" + au.getAUId() +", " + au);
      if (!auId.equals(au.getAUId())) {
	throw new ArchivalUnit.ConfigurationException("Configured AU has "
						      +"unexpected AUId, "
						      +"is: "+au.getAUId()
						      +" expected: "+auId);
      }
      putAuInMap(au);
    } catch (ArchivalUnit.ConfigurationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error configuring AU", e);
      throw new
	ArchivalUnit.ConfigurationException("Unexpected error creating AU", e);
    }
  }

  ArchivalUnit createAU(Plugin plugin, Configuration auConf)
      throws ArchivalUnit.ConfigurationException {
    try {
      ArchivalUnit au = plugin.createAU(auConf);
      log.debug("Created AU " + au);
      try {
	getDaemon().startAUManagers(au);
      } catch (Exception e) {
	log.error("Couldn't start AU processes", e);
	throw new
	  ArchivalUnit.ConfigurationException("Couldn't start AU processes",
					      e);
      }
      log.debug("putAuMap(" + au.getAUId() +", " + au);
      putAuInMap(au);
      return au;
    } catch (ArchivalUnit.ConfigurationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error creating AU", e);
      throw new
	ArchivalUnit.ConfigurationException("Unexpected error creating AU", e);
    }
  }

  private void putAuInMap(ArchivalUnit au) {
    auMap.put(au.getAUId(), au);
  }

  public ArchivalUnit getAuFromId(String auId) {
    ArchivalUnit au = (ArchivalUnit)auMap.get(auId);
    log.debug3("getAu(" + auId + ") = " + au);
    return au;
  }

  // These don't belong here
  /** Reconfigure an AU and save the new configuration in the local config
   * file.  Need to find a better place for this.
   * @param au the AU
   * @param auProps the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   */
  public void setAndSaveAUConfiguration(ArchivalUnit au,
					Properties auProps)
      throws ArchivalUnit.ConfigurationException, IOException {
    setAndSaveAUConfiguration(au, ConfigManager.fromProperties(auProps));
  }

  /** Reconfigure an AU and save the new configuration in the local config
   * file.  Need to find a better place for this.
   * @param au the AU
   * @param auConf the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   */
  public void setAndSaveAUConfiguration(ArchivalUnit au,
					Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    log.debug("Reconfiguring AU " + au);
    au.setConfiguration(auConf);
    updateAuConfigFile(au, auConf);
  }

  private void updateAuConfigFile(ArchivalUnit au, Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    String auid = au.getAUId();
    String prefix = PARAM_AU_TREE + "." + configKeyFromAUId(auid);
    Configuration fqConfig = auConf.addPrefix(prefix);
    configMgr.updateAuConfigFile(fqConfig, prefix);
  }


  /** Create an AU and save its configuration in the local config
   * file.  Need to find a better place for this.
   * @param plugin the Plugin in which to create the AU
   * @param auProps the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @return the new AU
   */
  public ArchivalUnit createAndSaveAUConfiguration(Plugin plugin,
						   Properties auProps)
      throws ArchivalUnit.ConfigurationException, IOException {
    return createAndSaveAUConfiguration(plugin,
					ConfigManager.fromProperties(auProps));
  }

  /** Create an AU and save its configuration in the local config
   * file.  Need to find a better place for this.
   * @param plugin the Plugin in which to create the AU
   * @param auConf the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @return the new AU
   */
  public ArchivalUnit createAndSaveAUConfiguration(Plugin plugin,
						   Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    ArchivalUnit au = createAU(plugin, auConf);
    updateAuConfigFile(au, auConf);
    return au;
  }

  /** Delete AU configuration from the local config file.  Need to find a
   * better place for this.
   * @param plugin the Plugin in which to create the AU
   * @param auConf the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @return the new AU
   */
  public void deleteAUConfiguration(ArchivalUnit au)
      throws ArchivalUnit.ConfigurationException, IOException {
    log.debug("Deleting AU config: " + au);
    updateAuConfigFile(au, ConfigManager.EMPTY_CONFIGURATION);
  }

  /** Return the stored config info for an AU (from config file, not from
   * AU instance).
    * @return the AU's Configuration, with unprefixed keys.
   */
  public Configuration getStoredAuConfiguration(ArchivalUnit au) {
    Configuration config = configMgr.readAuConfigFile();
    String auid = au.getAUId();
    String prefix = PARAM_AU_TREE + "." + configKeyFromAUId(auid);
    return config.getConfigTree(prefix);
  }

  /**
   * load a plugin with the given class name from somewhere in our classpath
   * @param pluginKey the key for this plugin
   * @return true if loaded
   */
  public boolean ensurePluginLoaded(String pluginKey) {
    if (pluginMap.containsKey(pluginKey)) {
      return true;
    }
    String pluginName = pluginNameFromKey(pluginKey);
    Class pluginClass;
    try {
      pluginClass = Class.forName(pluginName);
    } catch (ClassNotFoundException e) {
      log.debug(pluginName + " not on classpath");
      // not on classpath
      try {
	// tk - search plugin dir for signed jar, try loading again
	throw e;	       // for now, simulate failure of that process
      } catch (ClassNotFoundException e1) {
	// plugin is really not available
	log.error(pluginName + " not found");
	return false;
      }
    } catch (Exception e) {
      // any other exception while loading class if not recoverable
      log.error("Error loading " + pluginName, e);
      return false;
    }
    try {
      log.debug("Instantiating " + pluginClass);
      Plugin plugin = (Plugin)pluginClass.newInstance();
      plugin.initPlugin(theDaemon);
      setPlugin(pluginKey, plugin);
      return true;
    } catch (Exception e) {
      log.error("Error instantiating " + pluginName, e);
      return false;
    }
  }

  // separate method so can be called by test code
  private void setPlugin(String pluginKey, Plugin plugin) {
    log.debug3(this +".setPlugin(" + pluginKey + ", " + plugin + ")");
    pluginMap.put(pluginKey, plugin);
    titleMap = null;
  }

  /**
   * Searches all ArchivalUnits to find the most recent CachedUrl for the URL.
   * @param url The URL to search for.
   * @return a CachedUrl, or null if URL not present in any AU
   */
  public CachedUrl findMostRecentCachedUrl(String url) {
    CachedUrl best = null;
    for (Iterator iter = getAllAUs().iterator(); iter.hasNext();) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      if (au.shouldBeCached(url)) {
	CachedUrl cu = au.makeCachedUrl(au.getAUCachedUrlSet(), url);
	if (cu != null && cu.hasContent() && cuNewerThan(cu, best)) {
	  best = cu;
	}
      }
    }
    return best;
  }

  // return true if cu1 is newer than cu2, or cu2 is null
  // tk - no date available for comparison yet, return arbitrary order
  private boolean cuNewerThan(CachedUrl cu1, CachedUrl cu2) {
    if (cu2 == null) return true;
    Properties p1 = cu1.getProperties();
    Properties p2 = cu2.getProperties();
    // tk - this should use the crawl-date prop taht the crawler will add
//     Long.parseLong(p1.getProperty(HttpFields.__LastModified, "-1"));
    return true;
  }

  /**
   * Return a list of all configured ArchivalUnits.
   * @return the List of aus
   */
  public List getAllAUs() {
    return new ArrayList(auMap.values());
  }

  public Collection findAllTitles() {
    return getTitleMap().keySet();
  }

  public Collection getTitlePlugins(String title) {
    return (Collection)getTitleMap().get(title);
  }

  private Map titleMap = null;

  public Map getTitleMap() {
    if (titleMap == null) {
      titleMap = buildTitleMap();
    }
    return titleMap;
  }

  Map buildTitleMap() {
    Map map = new org.apache.commons.collections.MultiHashMap();
    Collection plugs = getRegisteredPlugins();
    for (Iterator iter = plugs.iterator(); iter.hasNext();) {
      Plugin p = (Plugin)iter.next();
      Collection titles = p.getSupportedTitles();
      for (Iterator iter2 = titles.iterator(); iter2.hasNext();) {
	String title = (String)iter2.next();
	map.put(title, p);
      }
    }
    return map;
  }

  /**
   * Find the CachedUrlSet from a PollSpec.
   * @param spec the PollSpec (from an incoming message)
   * @return a CachedUrlSet for the plugin, au and URL in the spec, or
   * null if au not present on this cache
   */
  public CachedUrlSet findCachedUrlSet(PollSpec spec) {
    if (log.isDebug3()) log.debug3(this +".findCachedUrlSet2("+spec+")");
    String auId = spec.getAUId();
    ArchivalUnit au = getAuFromId(auId);
    if (log.isDebug3()) log.debug3("au: " + au);
    if (au == null) return null;
    String url = spec.getUrl();
    CachedUrlSet cus;
    if (AuUrl.isAuUrl(url)) {
      cus = au.getAUCachedUrlSet();
    } else if ((spec.getLwrBound()!=null) &&
               (spec.getLwrBound().equals(PollSpec.SINGLE_NODE_LWRBOUND))) {
      cus = au.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(url));
    } else {
      cus = au.makeCachedUrlSet(new RangeCachedUrlSetSpec(url,
          spec.getLwrBound(), spec.getUprBound()));
    }
    if (log.isDebug3()) log.debug3("ret cus: " + cus);
    return cus;
  }

  // Plugin registry
  private String builtinPluginNames[] = {
    "org.lockss.plugin.highwire.HighWirePlugin",
    "org.lockss.plugin.simulated.SimulatedPlugin",
  };

  public Collection getRegisteredPlugins() {
    return pluginMap.values();
  }

  void initPluginRegistry() {
    for (int ix = 0; ix < builtinPluginNames.length; ix++) {
      ensurePluginLoaded(pluginKeyFromName(builtinPluginNames[ix]));
    }
  }

  private class Status implements StatusAccessor {
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("au", true));

    private final List colDescs =
      ListUtil.list(
		    new ColumnDescriptor("au", "Journal Volume",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor("auid", "AUID",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor("poll", "Poll Status",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor("crawl", "Crawl Status",
					 ColumnDescriptor.TYPE_STRING)
		    );

    // need this because the statics above require this to be a nested,
    // not inner class.
    PluginManager mgr;

    Status(PluginManager mgr) {
      this.mgr = mgr;
    }

    public List getColumnDescriptors(String key) {
      return colDescs;
    }

    public List getRows(String key) {
      List table = new ArrayList();
      for (Iterator iter = mgr.getAllAUs().iterator(); iter.hasNext();) {
	Map row = new HashMap();
	ArchivalUnit au = (ArchivalUnit)iter.next();
	row.put("au", au.getName());
	row.put("auid", au.getAUId());
	row.put("poll",
		statusSvc.getReference(PollerStatus.MANAGER_STATUS_TABLE_NAME,
				       au));
	table.add(row);
      }
      return table;
    }

    public List getDefaultSortRules(String key) {
      return sortRules;
    }

    public boolean requiresKey() {
      return false;
    }

    public String getTitle(String key) {
      return "Archival Units";
    }

    public void populateTable(StatusTable table) {
      String key = table.getKey();
      table.setTitle(getTitle(key));
      table.setColumnDescriptors(getColumnDescriptors(key));
      table.setDefaultSortRules(getDefaultSortRules(key));
      table.setRows(getRows(key));
    }
  }
//   protected void initPlugins() {
//     /* grab our 3rd party plugins and load them using security manager */
//     String[] files = new java.io.File(pluginDir).list();
//     for(int i= 0; i < files.length; i++) {
//       if(files[i].endsWith(".jar")) {
//         loadPlugin(files[i].substring(0,files[i].lastIndexOf(".jar")));
//       }
//       else {
//         loadPlugin(files[i]);
//       }
//     }
//   }

}
