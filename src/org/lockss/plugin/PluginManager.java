/*
 * $Id: PluginManager.java,v 1.113 2004-10-12 22:56:56 tlipkis Exp $
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

import java.util.*;
import java.util.jar.*;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.poller.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.crawler.CrawlManager;

/**
 * Plugin global functionality
 *
 * @author  TAL
 * @version 0.0
 */
public class PluginManager
  extends BaseLockssDaemonManager implements ConfigurableManager {

  public static final String PARAM_AU_TREE = Configuration.PREFIX + "au";

  static final String PARAM_PLATFORM_DISK_SPACE_LIST =
    Configuration.PLATFORM + "diskSpacePaths";

  /** The location on the platform to store downloaded plugins once they have
      been verified for loading. */
  static final String PARAM_PLUGIN_LOCATION =
    Configuration.PLATFORM + "pluginDir";
  static final String DEFAULT_PLUGIN_LOCATION = "plugins";

  /** A list of plugins to load at startup. */
  static final String PARAM_PLUGIN_REGISTRY =
    Configuration.PREFIX + "plugin.registry";

  static final String PARAM_REMOVE_STOPPED_AUS =
    Configuration.PREFIX + "plugin.removeStoppedAus";
  static final boolean DEFAULT_REMOVE_STOPPED_AUS = true;

  /** A list of Plugin Registry URLs. */
  static final String PARAM_PLUGIN_REGISTRIES =
    Configuration.PREFIX + "plugin.registries";

  /** The location of a Java JKS keystore to use for verifying
      loadable plugins (optional). */
  static final String PARAM_KEYSTORE_LOCATION =
    Configuration.PREFIX + "plugin.keystore.location";
  static final String DEFAULT_KEYSTORE_LOCATION =
    "org/lockss/plugin/lockss.keystore";
  /** The password to use when opening the loadable plugin
      verification keystore (optional). */
  static final String PARAM_KEYSTORE_PASSWORD =
    Configuration.PREFIX + "plugin.keystore.password";
  static final String DEFAULT_KEYSTORE_PASSWORD =
    "password";

  /** The interval between recrawls of the loadable plugin
      registry AUs.  (Specified as a string, not a long).  */
  static final String PARAM_REGISTRY_CRAWL_INTERVAL =
    Configuration.PREFIX + "plugin.registries.crawlInterval";
  static final String DEFAULT_REGISTRY_CRAWL_INTERVAL =
    "1d"; // not specified as a long value because this will be passed as
	  // a string literal to the AU config.

  /** The amount of time to wait when processing loadable plugins.
      This process delays the start of AUs, so the timeout should not
      be too long. */
  public static final String PARAM_PLUGIN_LOAD_TIMEOUT =
    Configuration.PREFIX + "plugin.load.timeout";
  public static final long DEFAULT_PLUGIN_LOAD_TIMEOUT =
    Constants.MINUTE;

  /** The type of plugin we prefer to load, if both are present.
      Can be either "class" or "xml" (case insensitive) */
  public static final String PARAM_PREFERRED_PLUGIN_TYPE =
    Configuration.PREFIX + "plugin.preferredType";
  public static final String DEFAULT_PREFERRED_PLUGIN_TYPE =
    "xml";

  static final String PARAM_TITLE_DB = ConfigManager.PARAM_TITLE_DB;

  // prefix for non-plugin AU params
  public static final String AU_PARAM_RESERVED = "reserved";
  // per AU params known to and processed by daemon, not plugin
  static final String AU_PARAM_WRAPPER = AU_PARAM_RESERVED + ".wrapper";
  public static final String AU_PARAM_DISABLED = AU_PARAM_RESERVED + ".disabled";
  public static final String AU_PARAM_REPOSITORY = AU_PARAM_RESERVED + ".repository";
  public static final String AU_PARAM_DISPLAY_NAME = AU_PARAM_RESERVED + ".displayName";

  public static final List NON_USER_SETTABLE_AU_PARAMS =
    Collections.unmodifiableList(ListUtil.list(AU_PARAM_WRAPPER));

  static final String CONFIGURABLE_PLUGIN_NAME =
    DefinablePlugin.class.getName();

  private static Logger log = Logger.getLogger("PluginMgr");

  private ConfigManager configMgr;
  private StatusService statusSvc;

  private File pluginDir = null;
  private AuOrderComparator auComparator = new AuOrderComparator();

  private final Attributes.Name LOADABLE_PLUGIN_ATTR =
    new Attributes.Name("Lockss-Plugin");

  // maps plugin key(not id) to plugin
  private Map pluginMap = Collections.synchronizedMap(new HashMap());
  private Map auMap = Collections.synchronizedMap(new HashMap());
  private Map cuNodeVersionMap = Collections.synchronizedMap(new HashMap());
  private Map classloaderMap = Collections.synchronizedMap(new HashMap());
  private Set auSet = Collections.synchronizedSet(new TreeSet(auComparator));
  private Set inactiveAuIds = Collections.synchronizedSet(new HashSet());

  // Map of plugin keys to loadable plugin JAR CachedUrls, used by
  // the loadable plugin status accessor.
  private Map pluginCus = Collections.synchronizedMap(new HashMap());

  private KeyStore keystore;
  private JarValidator jarValidator;
  private boolean keystoreInited = false;
  private boolean loadablePluginsReady = false;
  private long registryTimeout = Configuration.
    getCurrentConfig().getTimeIntervalParam(PARAM_PLUGIN_LOAD_TIMEOUT,
					    DEFAULT_PLUGIN_LOAD_TIMEOUT);

  public static final int PREFER_XML_PLUGIN = 0;
  public static final int PREFER_CLASS_PLUGIN = 1;

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
    // Initialize the plugin directory.
    initPluginDir();
    statusSvc.registerStatusAccessor("LoadablePluginTable",
				     new LoadablePluginStatus());
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
    statusSvc.unregisterStatusAccessor("LoadablePluginTable");
    super.stopService();
  }

  /**
   * Start loadable plugins, ensure that they are properly loaded, and
   * then start all configured AUs.  This is called by LockssDaemon
   * after starting all the managers, and ensures everything happens
   * in the right order.
   */
  public void startLoadablePlugins() {
    if (loadablePluginsReady) {
      return;
    }

    Configuration config = Configuration.getCurrentConfig();
    log.debug("Initializing loadable plugin registries before starting AUs");
    initLoadablePluginRegistries(config.getList(PARAM_PLUGIN_REGISTRIES));
    initPluginRegistry(config.getList(PARAM_PLUGIN_REGISTRY));
    configureAllPlugins(config);

    loadablePluginsReady = true;
  }

  public void setLoadablePluginsReady(boolean val) {
    loadablePluginsReady = val;
  }

  public boolean areAusStarted() {
    return loadablePluginsReady;
  }

  Configuration currentAllPlugs = ConfigManager.EMPTY_CONFIGURATION;

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {

    if (changedKeys.contains(PARAM_PLUGIN_LOAD_TIMEOUT)) {
      registryTimeout = config.getTimeInterval(PARAM_PLUGIN_LOAD_TIMEOUT,
					       DEFAULT_PLUGIN_LOAD_TIMEOUT);
    }

    // If the keystore or password has changed, update.
    if (changedKeys.contains(PARAM_KEYSTORE_LOCATION) ||
	changedKeys.contains(PARAM_KEYSTORE_PASSWORD)) {
      initKeystore();
    }

    // Don't load or start other plugins until the daemon is running.
    if (loadablePluginsReady) {
      // Process loadable plugin registries.
      if (changedKeys.contains(PARAM_PLUGIN_REGISTRIES)) {
	initLoadablePluginRegistries(config.getList(PARAM_PLUGIN_REGISTRIES));
      }

      // Process the built-in plugin registry.
      if (changedKeys.contains(PARAM_PLUGIN_REGISTRY)) {
	initPluginRegistry(config.getList(PARAM_PLUGIN_REGISTRY));
      }

      // Process any changed AU config
      if (changedKeys.contains(PARAM_AU_TREE)) {
	configureAllPlugins(config);
      }
    }
  }

  private void configureAllPlugins(Configuration config) {
    Configuration allPlugs = config.getConfigTree(PARAM_AU_TREE);
    if (!allPlugs.equals(currentAllPlugs)) {
      for (Iterator iter = allPlugs.nodeIterator(); iter.hasNext(); ) {
	String pluginKey = (String)iter.next();
	log.debug("Configuring plugin key: " + pluginKey);
	Configuration pluginConf = allPlugs.getConfigTree(pluginKey);
	Configuration prevPluginConf =
	  currentAllPlugs.getConfigTree(pluginKey);

	configurePlugin(pluginKey, pluginConf, prevPluginConf);
      }
      currentAllPlugs = allPlugs;
    }
  }

  /**
   * Convert plugin property key to plugin class name.
   * @param key the key
   * @return the plugin name
   */
  public static String pluginNameFromKey(String key) {
    return StringUtil.replaceString(key, "|", ".");
  }

  /**
   * Convert plugin class name to key suitable for property file.
   * @param className the class name
   * @return the plugin key
   */
  public static String pluginKeyFromName(String className) {
    return StringUtil.replaceString(className, ".", "|");
  }

  /**
   * Convert plugin id to key suitable for property file.  Plugin id is
   * currently the same as plugin class name, but that may change.
   * @param id the plugin id
   * @return String the plugin key
   */
  public static String pluginKeyFromId(String id) {
    // tk - needs to do real mapping from IDs obtained from all available
    // plugins.
    return StringUtil.replaceString(id, ".", "|");
  }

  /**
   * Return a unique identifier for an au based on its plugin id and defining
   * properties.
   * @param pluginId plugin id (with . not escaped)
   * @param auDefProps Properties with values for all definitional AU params
   * @return a unique identifier for an au based on its plugin id and defining
   * properties.
   */
  public static String generateAuId(String pluginId, Properties auDefProps) {
    return generateAuId(pluginId,
			PropUtil.propsToCanonicalEncodedString(auDefProps));
  }

  public static String generateAuId(Plugin plugin, Configuration auConfig) {
    Properties props = new Properties();
    for (Iterator iter = plugin.getAuConfigDescrs().iterator();
	 iter.hasNext();) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      if (descr.isDefinitional()) {
	String key = descr.getKey();
	props.setProperty(key, auConfig.get(key));
      }
    }
    return generateAuId(plugin.getPluginId(), props);
  }

  public static String generateAuId(String pluginId, String auKey) {
    return pluginKeyFromId(pluginId)+"&"+auKey;
  }

  public static String auKeyFromAuId(String auid) {
    int pos = auid.indexOf("&");
    if (pos < 0) {
      throw new IllegalArgumentException("Illegal AuId: " + auid);
    }
    return auid.substring(pos + 1);
  }

  public static String pluginIdFromAuId(String auid) {
    int pos = auid.indexOf("&");
    if (pos < 0) {
      throw new IllegalArgumentException("Illegal AuId: " + auid);
    }
    return auid.substring(0, pos);
  }

  public static String pluginNameFromAuId(String auid) {
    return pluginNameFromKey(pluginIdFromAuId(auid));
  }

  public static String configKeyFromAuId(String auid) {
    return StringUtil.replaceFirst(auid, "&", ".");
  }

  /**
   * Return the plugin with the given key.
   * @param pluginKey the plugin key
   * @return the plugin or null
   */
  public Plugin getPlugin(String pluginKey) {
    return (Plugin)pluginMap.get(pluginKey);
  }

  /**
   * Returns true if the reserved.wrapper key is true
   * @param auConf the Configuration
   * @return true if wrapped
   */
  private boolean isAuWrapped(Configuration auConf) {
    return WrapperState.isUsingWrapping() &&
      auConf.getBoolean(AU_PARAM_WRAPPER, false);
  }

  private Configuration removeWrapper(Configuration auConf) {
    Configuration copy = auConf.copy();
    if (copy.containsKey(AU_PARAM_WRAPPER)) {
      copy.remove(AU_PARAM_WRAPPER);
    }
    return copy;
  }

  private void configurePlugin(String pluginKey, Configuration pluginConf,
			       Configuration oldPluginConf) {
    for (Iterator iter = pluginConf.nodeIterator(); iter.hasNext(); ) {
      String auKey = (String)iter.next();
      String auId = generateAuId(pluginKey, auKey);
      try {
	Configuration auConf = pluginConf.getConfigTree(auKey);
	Configuration oldAuConf = oldPluginConf.getConfigTree(auKey);
	if (auConf.getBoolean(AU_PARAM_DISABLED, false)) {
	  // tk should actually remove AU?
	  if (log.isDebug2())
	    log.debug("Not configuring disabled AU id: " + auKey);
	  if (auMap.get(auId) == null) {
	    // don't add to inactive if it's still running
	    inactiveAuIds.add(auId);
	  }
	} else if (auConf.equals(oldAuConf)) {
	  if (log.isDebug2())
	    log.debug2("AU already configured, not reconfiguring: " + auKey);
	} else {
	  log.debug("Configuring AU id: " + auKey);
	  if (!isAuWrapped(auConf)) {
	    boolean pluginOk = ensurePluginLoaded(pluginKey);
	    if (pluginOk) {
	      Plugin plugin = getPlugin(pluginKey);
	      if (WrapperState.isWrappedPlugin(plugin)) {
		throw new ArchivalUnit.ConfigurationException("An attempt was made to load unwrapped AU " + auKey + " from plugin " + pluginKey + " which is already wrapped.");
	      }
	      configureAu(plugin, auConf, auId);
	    } else {
	      log.warning("Not configuring AU " + auKey);
	    }
	  } else {
	    log.debug("Wrapping " + auKey);
	    if ((pluginMap.containsKey(pluginKey)) &&
		(!WrapperState.isWrappedPlugin(pluginMap.get(pluginKey)))) {
	      throw new ArchivalUnit.ConfigurationException("An attempt was made to wrap AU " + auKey + " from plugin " + pluginKey + " which is already loaded.");
	    }
	    Plugin wrappedPlugin =
	      WrapperState.retrieveWrappedPlugin(pluginKey, theDaemon);
	    if (wrappedPlugin==null) {
	      log.warning("Not configuring AU " + auKey);
	      log.error("Error instantiating " +
			WrapperState.WRAPPED_PLUGIN_NAME);
	    } else {
	      setPlugin(pluginKey,wrappedPlugin);
	      Configuration wrappedAuConf = removeWrapper(auConf);
	      configureAu(wrappedPlugin, wrappedAuConf, auId);
	    }
	  }
	  inactiveAuIds.remove(generateAuId(pluginKey, auKey));
	}
      } catch (ArchivalUnit.ConfigurationException e) {
	log.error("Failed to configure AU " + auKey, e);
      } catch (Exception e) {
	log.error("Unexpected exception configuring AU " + auKey, e);
      }
    }
  }

  void configureAu(Plugin plugin, Configuration auConf, String auId)
      throws ArchivalUnit.ConfigurationException {
    try {
      ArchivalUnit oldAu = (ArchivalUnit)auMap.get(auId);
      ArchivalUnit au = plugin.configureAu(auConf, oldAu);
      if (oldAu != null && oldAu != au) {
	String msg = "Plugin created new AU: " + au +
	  ", should have reconfigured old AU: " + oldAu;
	throw new ArchivalUnit.ConfigurationException(msg);
      }
      if (!auId.equals(au.getAuId())) {
	String msg = "Configured AU has unexpected AUID: " + au.getAuId() +
	  ", expected: "+ auId;
	throw new ArchivalUnit.ConfigurationException(msg);
      }
      try {
	getDaemon().startOrReconfigureAuManagers(au, auConf);
      } catch (Exception e) {
	throw new
	  ArchivalUnit.ConfigurationException("Couldn't configure AU managers",
					      e);
      }
      if (oldAu != null) {
	log.debug("Reconfigured AU " + au);
      } else {
	log.debug("Configured AU " + au);
	putAuInMap(au);
      }
    } catch (ArchivalUnit.ConfigurationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error configuring AU", e);
      throw new
	ArchivalUnit.ConfigurationException("Unexpected error creating AU", e);
    }
  }

  ArchivalUnit createAu(Plugin plugin, Configuration auConf)
      throws ArchivalUnit.ConfigurationException {
    String auid = null;
    ArchivalUnit oldAu = null;
    try {
      auid = generateAuId(plugin, auConf);
      oldAu = getAuFromId(auid);
    } catch (Exception e) {
      // no action.  Bad/missing config value might cause getAuFromId() to
      // throw.  It will be caught soon when creating the AU; for now we
      // can assume it means the AU doesn't already exist.
    }
    if (oldAu != null) {
      inactiveAuIds.remove(oldAu.getAuId());
      throw new ArchivalUnit.ConfigurationException("Cannot create that AU because it already exists: " + oldAu.getName());
    }
    try {
      ArchivalUnit au = plugin.createAu(auConf);
      inactiveAuIds.remove(au.getAuId());
      log.debug("Created AU " + au);
      try {
	getDaemon().startOrReconfigureAuManagers(au, auConf);
      } catch (Exception e) {
	log.error("Couldn't start AU processes", e);
	throw new
	  ArchivalUnit.ConfigurationException("Couldn't start AU processes",
					      e);
      }
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

  /** Stop the AU's activity and remove it, as though it had never been
   * configured.  Does not affect the AU's current repository contents.
   * @return true if the AU was removed, false if it didn't exist or is
   * stale. */
  public boolean stopAu(ArchivalUnit au) {
    String auid = au.getAuId();
    ArchivalUnit mapAu = (ArchivalUnit)auMap.get(auid);
    if (mapAu == null) {
      log.warning("stopAu(" + au.getName() + "), wasn't in map");
      return false;
    }
    if (mapAu != au) {
      log.warning("stopAu(" + au.getName() + "), but map contains " + mapAu);
      return false;
    }
    log.debug("Deactivating AU: " + au.getName());
    // remove from map first, so no new activity can start (poll messages,
    // RemoteAPI, etc.)
    auMap.remove(auid);
    auSet.remove(au);

    theDaemon.getPollManager().cancelAuPolls(au);
    theDaemon.getCrawlManager().cancelAuCrawls(au);

    try {
      //     Plugin plugin = au.getPlugin();
      //     plugin.removeAu(au);
      theDaemon.stopAuManagers(au);
    } catch (Exception e) {
      // Shouldn't happen, as stopAuManagers() catches errors in
      // stopService().  Not clear what to do anyway, if some of the
      // managers don't stop cleanly.
    }
    return true;
  }

  protected void putAuInMap(ArchivalUnit au) {
    log.debug("putAuMap(" + au.getAuId() +", " + au);
    auMap.put(au.getAuId(), au);
    auSet.add(au);
  }

  public ArchivalUnit getAuFromId(String auId) {
    ArchivalUnit au = (ArchivalUnit)auMap.get(auId);
    if (log.isDebug3()) log.debug3("getAu(" + auId + ") = " + au);
    return au;
  }

  // These don't belong here
  /**
   * Reconfigure an AU and save the new configuration in the local config
   * file.  Need to find a better place for this.
   * @param au the AU
   * @param auProps the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void setAndSaveAuConfiguration(ArchivalUnit au,
					Properties auProps)
      throws ArchivalUnit.ConfigurationException, IOException {
    setAndSaveAuConfiguration(au, ConfigManager.fromProperties(auProps));
  }

  /**
   * Reconfigure an AU and save the new configuration in the local config
   * file.  Need to find a better place for this.
   * @param au the AU
   * @param auConf the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void setAndSaveAuConfiguration(ArchivalUnit au,
					Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    log.debug("Reconfiguring AU " + au);
    au.setConfiguration(auConf);
    updateAuConfigFile(au, auConf);
  }

  private void updateAuConfigFile(ArchivalUnit au, Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    updateAuConfigFile(au.getAuId(), auConf);
  }

  public void updateAuConfigFile(String auid, Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    String prefix = PARAM_AU_TREE + "." + configKeyFromAuId(auid);
    Configuration fqConfig = auConf.addPrefix(prefix);
    configMgr.updateAuConfigFile(fqConfig, prefix);
  }


  /**
   * Create an AU and save its configuration in the local config
   * file.  Need to find a better place for this.
   * @param plugin the Plugin in which to create the AU
   * @param auProps the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @return the new AU
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public ArchivalUnit createAndSaveAuConfiguration(Plugin plugin,
						   Properties auProps)
      throws ArchivalUnit.ConfigurationException, IOException {
    return createAndSaveAuConfiguration(plugin,
					ConfigManager.fromPropertiesUnsealed(auProps));
  }

  /**
   * Create an AU and save its configuration in the local config
   * file.  Need to find a better place for this.
   * @param plugin the Plugin in which to create the AU
   * @param auConf the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @return the new AU
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public ArchivalUnit createAndSaveAuConfiguration(Plugin plugin,
						   Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    auConf.put(AU_PARAM_DISABLED, "false");
    ArchivalUnit au = createAu(plugin, auConf);
    updateAuConfigFile(au, auConf);
    return au;
  }

  /**
   * Delete AU configuration from the local config file.  Need to find a
   * better place for this.
   * @param au the ArchivalUnit to be unconfigured
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void deleteAuConfiguration(ArchivalUnit au)
      throws ArchivalUnit.ConfigurationException, IOException {
    log.debug("Deleting AU config: " + au);
    updateAuConfigFile(au, ConfigManager.EMPTY_CONFIGURATION);
  }

  /**
   * Delete AU configuration from the local config file.  Need to find a
   * better place for this.
   * @param auid the AuId
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void deleteAuConfiguration(String auid)
      throws ArchivalUnit.ConfigurationException, IOException {
    log.debug("Deleting AU config: " + auid);
    updateAuConfigFile(auid, ConfigManager.EMPTY_CONFIGURATION);
    // might be deleting an inactive au
    inactiveAuIds.remove(auid);
  }

  /**
   * Deactivate an AU in the config file.  Does not actually stop the AU.
   * @param au the ArchivalUnit to be deactivated
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void deactivateAuConfiguration(ArchivalUnit au)
      throws ArchivalUnit.ConfigurationException, IOException {
    log.debug("Deactivating AU: " + au);
    Configuration config = getStoredAuConfiguration(au);
    config.put(AU_PARAM_DISABLED, "true");
    config.put(AU_PARAM_DISPLAY_NAME, au.getName());
    updateAuConfigFile(au, config);
  }

  /**
   * Delete an AU
   * @param au the ArchivalUnit to be deleted
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void deleteAu(ArchivalUnit au)
      throws ArchivalUnit.ConfigurationException, IOException {
    deleteAuConfiguration(au);
    if (isRemoveStoppedAus()) {
      stopAu(au);
    }
  }

  /**
   * Deactivate an AU
   * @param au the ArchivalUnit to be deactivated
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void deactivateAu(ArchivalUnit au)
      throws ArchivalUnit.ConfigurationException, IOException {
    deactivateAuConfiguration(au);
    if (isRemoveStoppedAus()) {
      String auid = au.getAuId();
      stopAu(au);
      inactiveAuIds.add(auid);
    }
  }

  public boolean isRemoveStoppedAus() {
    return configMgr.getBooleanParam(PARAM_REMOVE_STOPPED_AUS,
				     DEFAULT_REMOVE_STOPPED_AUS);
  }

  /**
   * Return the stored config info for an AU (from config file, not from
   * AU instance).
   * @param au the ArchivalUnit
   * @return the AU's Configuration, with unprefixed keys.
   */
  public Configuration getStoredAuConfiguration(ArchivalUnit au) {
    return getStoredAuConfiguration(au.getAuId());
  }

  /**
   * Return the config tree for an AU id (from the local au config file,
   * not the au itself).
   * @param auid the AU's id.
   * @return the AU's Configuration, with unprefixed keys.
   */
  public Configuration getStoredAuConfiguration(String auid) {
    String aukey = configKeyFromAuId(auid);
    Configuration config = configMgr.readAuConfigFile();
    String prefix = PARAM_AU_TREE + "." + aukey;
    return config.getConfigTree(prefix);
  }

  /**
   * Return the current config info for an AU (from current configuration)
   * @param auid the AU's id.
   * @param aup the AuProxy
   * @return the AU's Configuration, with unprefixed keys.
   */
  public Configuration getCurrentAuConfiguration(String auid) {
    String aukey = configKeyFromAuId(auid);
    String prefix = PARAM_AU_TREE + "." + aukey;
    return ConfigManager.getCurrentConfig().getConfigTree(prefix);
  }

  /**
   * Retrieve a plugin from the specified classloader.  If the
   * clasloader is 'null', this method will use the default
   * classloader.
   */
  Plugin retrievePlugin(String pluginKey, ClassLoader loader)
      throws Exception {
    if (pluginMap.containsKey(pluginKey)) {
      return (Plugin)pluginMap.get(pluginKey);
    }

    if (loader == null) {
      loader = this.getClass().getClassLoader();
    }

    String pluginName = pluginNameFromKey(pluginKey);

    Plugin classPlugin = null;
    Plugin xmlPlugin = null;

    boolean foundClassPlugin = false;
    boolean foundXmlPlugin = false;

    // 1. Look for an ordinary Plugin object.
    try {
      // See if we can load the class by name.
      log.debug3(pluginName + ": Loading class.");
      Class c = Class.forName(pluginName, true, loader);
      classPlugin = (Plugin)c.newInstance();
      classPlugin.initPlugin(theDaemon);
      foundClassPlugin = true;
    } catch (ClassNotFoundException ex) {
      log.debug3(pluginName + ": Class not found on classpath.");
    }

    // 2. Look for a loadable plugin definition.
    try {
      log.debug3(pluginName + ": Loading XML definition.");
      Class c = Class.forName(getConfigurablePluginName(), true, loader);
      xmlPlugin = (DefinablePlugin)c.newInstance();
      ((DefinablePlugin)xmlPlugin).initPlugin(theDaemon, pluginName, loader);
      foundXmlPlugin = true;
    } catch (Exception ex) {
      log.debug3(pluginName + ": XML definition not found on classpath.");
    }

    // If both are found, decide which one to favor.
    if (foundClassPlugin && foundXmlPlugin) {
      // Shouldn't have both.  Log a warning, and use
      // the default (which is configurable)
      log.warning(pluginName +  ": Both a definable plugin definition " +
		  "and a plugin class file were found.");

      switch(getPreferredPluginType()) {
      case PREFER_XML_PLUGIN:
	log.debug(pluginName + ": Creating definable plugin.");
	return xmlPlugin;
      case PREFER_CLASS_PLUGIN:
	log.debug(pluginName + ": Instantiating plugin class.");
	return classPlugin;
      default:
	log.warning(pluginName + ": Unable to determine which " +
		    "to load!  Plugin not loaded.");
	return null;
      }
    } else {
      // Only one was successfully found.
      if (foundClassPlugin) {
	return classPlugin;
      } else if (foundXmlPlugin) {
	return xmlPlugin;
      } else {
	// Error -- this plugin was not found.
	log.error(pluginName + " could not be loaded.");
	return null;
      }
    }
  }

  /**
   * (package-level access for unit testing)
   */
  int getPreferredPluginType() {
    String preferredPlugin = Configuration.
      getCurrentConfig().get(PARAM_PREFERRED_PLUGIN_TYPE,
			     DEFAULT_PREFERRED_PLUGIN_TYPE);

    if (StringUtil.equalStringsIgnoreCase(preferredPlugin.trim(), "xml")) {
      return PREFER_XML_PLUGIN;
    } else if (StringUtil.equalStringsIgnoreCase(preferredPlugin.trim(), "class")) {
      return PREFER_CLASS_PLUGIN;
    } else {
      // By default, if we can't parse the configuration
      return PREFER_XML_PLUGIN;
    }
  }

  /**
   * Load a plugin with the given class name from somewhere in our classpath.
   * @param pluginKey the key for this plugin
   * @return true if loaded
   */
  public boolean ensurePluginLoaded(String pluginKey) {
    ClassLoader loader = (ClassLoader)classloaderMap.get(pluginKey);

    if (loader == null) {
      loader = this.getClass().getClassLoader();
    }

    if (pluginMap.containsKey(pluginKey)) {
      return true;
    }

    String pluginName = "";
    try {
      pluginName = pluginNameFromKey(pluginKey);
      Plugin plugin = retrievePlugin(pluginKey, loader);
      if (plugin != null) {
	setPlugin(pluginKey, plugin);
	return true;
      }
      else {
	return false;
      }
    } catch (Exception e) {
      log.error("Error instantiating " + pluginName, e);
      return false;
    }
  }

  // separate method so can be called by test code
  protected void setPlugin(String pluginKey, Plugin plugin) {
    if (log.isDebug3()) {
      log.debug3("PluginManager.setPlugin(" + pluginKey + ", " +
		 plugin.getPluginName() + ")");
    }
    pluginMap.put(pluginKey, plugin);
    resetTitles();
  }

  protected String getConfigurablePluginName() {
    return CONFIGURABLE_PLUGIN_NAME;
  }

  /**
   * Find the CachedUrlSet from a PollSpec.
   * @param spec the PollSpec (from an incoming message)
   * @return a CachedUrlSet for the plugin, au and URL in the spec, or
   * null if au not present on this cache
   */
  public CachedUrlSet findCachedUrlSet(PollSpec spec) {
    if (log.isDebug3()) log.debug3(this +".findCachedUrlSet2("+spec+")");
    String auId = spec.getAuId();
    ArchivalUnit au = getAuFromId(auId);
    if (log.isDebug3()) log.debug3("au: " + au);
    if (au == null) return null;
    String url = spec.getUrl();
    CachedUrlSet cus;
    if (AuUrl.isAuUrl(url)) {
      cus = au.getAuCachedUrlSet();
    } else if ((spec.getLwrBound()!=null) &&
	       (spec.getLwrBound().equals(PollSpec.SINGLE_NODE_LWRBOUND))) {
      cus = au.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(url));
    } else {
      RangeCachedUrlSetSpec rcuss =
	new RangeCachedUrlSetSpec(url, spec.getLwrBound(), spec.getUprBound());
      cus = au.makeCachedUrlSet(rcuss);
    }
    if (log.isDebug3()) log.debug3("ret cus: " + cus);
    return cus;
  }

  /**
   * Searches all ArchivalUnits to find the most recent CachedUrl for the URL.
   * @param url The URL to search for.
   * @return a CachedUrl, or null if URL not present in any AU
   */
  public CachedUrl findMostRecentCachedUrl(String url) {
    // We don't know what AU it might be in, so can't do plugin-dependent
    // normalization yet.  But only need to do generic normalization once.
    // XXX This is wrong, as plugin-specific normalization is normally done
    // first.
    String normUrl;
    try {
      normUrl = UrlUtil.normalizeUrl(url);
    } catch (MalformedURLException e) {
      log.warning("findMostRecentCachedUrl(" + url + ")", e);
      return null;
    }
    CachedUrl best = null;
    for (Iterator iter = getAllAus().iterator(); iter.hasNext();) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      if (au.shouldBeCached(url)) {
	try {
	  String siteUrl = UrlUtil.normalizeUrl(normUrl, au);
	  CachedUrl cu = au.makeCachedUrl(au.getAuCachedUrlSet(), siteUrl);
	  if (cu != null && cu.hasContent() && cuNewerThan(cu, best)) {
	    best = cu;
	  }
	} catch (MalformedURLException ignore) {
	  // ignored
	} catch (PluginBehaviorException ignore) {
	  // ignored
	}
      }
    }
    return best;
  }

  // return true if cu1 is newer than cu2, or cu2 is null
  // tk - no date available for comparison yet, return arbitrary order
  private boolean cuNewerThan(CachedUrl cu1, CachedUrl cu2) {
    if (cu2 == null) return true;
    CIProperties p1 = cu1.getProperties();
    CIProperties p2 = cu2.getProperties();
    // tk - this should use the crawl-date prop taht the crawler will add
    //     Long.parseLong(p1.getProperty(HttpFields.__LastModified, "-1"));
    return true;
  }

  /**
   * Return a list of all configured ArchivalUnits.
   * @return the List of aus
   */
  public List getAllAus() {
    return new ArrayList(auSet);
  }

  public Collection getInactiveAuIds() {
    return inactiveAuIds;
  }

  /** Return all the known titles from the title db, sorted by title */
  public List findAllTitles() {
    if (allTitles == null) {
      allTitles = new ArrayList(getTitleMap().keySet());
      Collections.sort(allTitles, CatalogueOrderComparator.SINGLETON);
    }
    return allTitles;
  }

  /** Find all the plugins that support the given title */
  public Collection getTitlePlugins(String title) {
    return (Collection)getTitleMap().get(title);
  }

  private Map titleMap = null;
  private List allTitles = null;

  public void resetTitles() {
    titleMap = null;
    allTitles = null;
  }

  public Map getTitleMap() {
    if (titleMap == null) {
      titleMap = buildTitleMap();
    }
    return titleMap;
  }

  Map buildTitleMap() {
    Map map = new org.apache.commons.collections.MultiHashMap();
    synchronized (pluginMap) {
      for (Iterator iter = getRegisteredPlugins().iterator();
	   iter.hasNext();) {
	Plugin p = (Plugin)iter.next();
	Collection titles = p.getSupportedTitles();
	for (Iterator iter2 = titles.iterator(); iter2.hasNext();) {
	  String title = (String)iter2.next();
	  if (title != null) {
	    map.put(title, p);
	  }
	}
      }
    }
    return map;
  }

  /** Return a SortedMap mapping (human readable) plugin name to plugin
   * instance */
  public SortedMap getPluginNameMap() {
    SortedMap pMap = new TreeMap();
    synchronized (pluginMap) {
      for (Iterator iter = getRegisteredPlugins().iterator();
	   iter.hasNext(); ) {
	Plugin p = (Plugin)iter.next();
	pMap.put(p.getPluginName(), p);
      }
    }
    return pMap;
  }

  /** @return All plugins that have been registered.  <i>Ie</i>, that are
   * either listed in org.lockss.plugin.registry, or were loaded by a
   * configured AU */
  public Collection getRegisteredPlugins() {
    return pluginMap.values();
  }

  /**
   * Load all plugin registry plugins.
   */
  void initLoadablePluginRegistries(List urls) {
    // Load the keystore if necessary
    if (!isKeystoreInited()) {
      initKeystore();
    }

    if (urls.isEmpty()) {
      // if loop below is empty, it waits on semaphore that's never posted
      return;
    }

    BinarySemaphore bs = new BinarySemaphore();

    RegistryCallback regCallback = new RegistryCallback(urls, bs);

    List loadAus = new ArrayList();
    RegistryPlugin registryPlugin = new RegistryPlugin();
    String pluginKey = pluginKeyFromName("org.lockss.plugin.RegistryPlugin");
    registryPlugin.initPlugin(theDaemon);
    setPlugin(pluginKey, registryPlugin);

    for (Iterator iter = urls.iterator(); iter.hasNext(); ) {
      String url = (String)iter.next();
      Configuration auConf = ConfigManager.newConfiguration();
      auConf.put(ConfigParamDescr.BASE_URL.getKey(), url);
      auConf.put("nc_interval",
		 Configuration.getCurrentConfig().get(PARAM_REGISTRY_CRAWL_INTERVAL,
						      DEFAULT_REGISTRY_CRAWL_INTERVAL));

      String auId = generateAuId(registryPlugin, auConf);
      String auKey = auKeyFromAuId(auId);
      if (log.isDebug2()) {
	log.debug2("Setting Registry AU " + auKey + " recrawl interval to " +
		   auConf.get("nc_interval"));
      }

      // Only process this registry if it is new.
      if (!auMap.containsKey(auId)) {

	try {
	  configureAu(registryPlugin, auConf, auId);
	} catch (ArchivalUnit.ConfigurationException ex) {
	  log.error("Failed to configure AU " + auKey, ex);
	  regCallback.processPluginsIfReady(url);
	  continue;
	}

	ArchivalUnit registryAu = getAuFromId(auId);

	loadAus.add(registryAu);

	// Trigger a new content crawl if required.
	if (registryAu.
	    shouldCrawlForNewContent(theDaemon.getNodeManager(registryAu).getAuState())) {
	  if (log.isDebug2()) {
	    log.debug2("Starting a new crawl of AU: " + registryAu.getName());
	  }
	  theDaemon.getCrawlManager().startNewContentCrawl(registryAu, regCallback,
							   url, null);

	} else {
	  // If we're not going to crawl this AU, let the callback know.
	  if (log.isDebug2()) {
	    log.debug2("Don't need to do a crawl of AU: " + registryAu.getName());
	  }
	  regCallback.processPluginsIfReady(url);
	}
      } else {
	log.debug2("We already have this AU configured, notifying callback.");
	regCallback.processPluginsIfReady(url);
      }
    }

    // Wait for the AU crawls to complete, or for the BinarySemaphore
    // to time out, then process all the registries in the load list.
    log.debug("Waiting for loadable plugins to finish loading...");
    try {
      // The registry crawls may have already completed, in which case there is
      // no need to wait.
      if (regCallback.needsWait() && !bs.take(Deadline.in(registryTimeout))) {
	log.warning("Timed out while waiting for registries to finish loading. " +
		    "Remaining registry URL list: " + regCallback.getRegistryUrls());
      }
    } catch (InterruptedException ex) {
      log.warning("Binary semaphore threw InterruptedException while waiting." +
		  "Remaining registry URL list: " + regCallback.getRegistryUrls());
    }

    processRegistryAus(loadAus);
  }


  // Synch the plugin registry with the plugins listed in names
  void initPluginRegistry(List nameList) {
    Collection newKeys = new HashSet();
    for (Iterator iter = nameList.iterator(); iter.hasNext(); ) {
      String name = (String)iter.next();
      String key = pluginKeyFromName(name);
      ensurePluginLoaded(key);
      newKeys.add(key);
    }
    // remove plugins that are no longer listed, unless they have one or
    // more configured AUs
    synchronized (pluginMap) {
      for (Iterator iter = pluginMap.keySet().iterator(); iter.hasNext(); ) {
	String name = (String)iter.next();
	String key = pluginKeyFromName(name);
	if (!classloaderMap.containsKey(key) && !newKeys.contains(key)) {
	  Configuration tree = currentAllPlugs.getConfigTree(key);
	  if (tree == null || tree.isEmpty()) {
	    iter.remove();
	  }
	}
      }
    }
  }

  /** Comparator for sorting Aus alphabetically by title.  This is used in a
   * TreeSet, so must return 0 only for identical objects. */
  static class AuOrderComparator implements Comparator {
    CatalogueOrderComparator coc = CatalogueOrderComparator.SINGLETON;

    public int compare(Object o1, Object o2) {
      if (o1 == o2) {
	return 0;
      }
      if (!((o1 instanceof ArchivalUnit)
	    && (o2 instanceof ArchivalUnit))) {
	throw new IllegalArgumentException("AuOrderComparator(" +
					   o1.getClass().getName() + "," +
					   o2.getClass().getName() + ")");
      }
      ArchivalUnit a1 = (ArchivalUnit)o1;
      ArchivalUnit a2 = (ArchivalUnit)o2;
      int res = coc.compare(a1.getName(), a2.getName());
      if (res == 0) {
	res = coc.compare(a1.getAuId(), a2.getAuId());
      }
      if (res == 0) {
	// this can happen during testing.  Don't care about order, but
	// mustn't be equal.
	res = 1;
      }
      return res;
    }
  }

  /**
   * Initialize the "blessed" loadable plugin directory.
   */
  private void initPluginDir() {
    if (pluginDir != null) {
      return;
    }

    List dSpaceList =
      ConfigManager.getCurrentConfig().getList(PARAM_PLATFORM_DISK_SPACE_LIST);
    String relPluginPath =
      ConfigManager.getCurrentConfig().get(PARAM_PLUGIN_LOCATION,
					   DEFAULT_PLUGIN_LOCATION);

    if (dSpaceList == null || dSpaceList.size() == 0) {
      log.error(PARAM_PLATFORM_DISK_SPACE_LIST +
		" not specified, not configuring plugin dir");
      return;
    }

    String pluginDisk = (String)dSpaceList.get(0);

    File dir = new File(new File(pluginDisk), relPluginPath);

    if (dir.exists() && !dir.isDirectory()) {
      // This should (hopefully) never ever happen.  Log an error and
      // return for now.
      log.error("Plugin directory " + dir + " cannot be created.  A file " +
		"already exists with that name!");
      return;
    }

    if (dir.exists() && dir.isDirectory()) {
      log.debug("Plugin directory " + dir +
		" already exists.  Cleaning up...");

      File[] dirList = dir.listFiles();
      for (int ix = 0; ix < dirList.length; ix++) {
	if (!dirList[ix].delete()) {
	  log.error("Unable to clean up plugin directory " + dir +
		    ".  Could not delete " + dirList[ix]);
	  return;
	}
      }
    } else {
      if (!dir.mkdirs()) {
	log.error("Unable to create plugin directory " + dir);
	return;
      }
    }

    pluginDir = dir;
  }


  /*
   * Helper methods for handling loadable plugins.
   */

  /**
   * Initialize the keystore.
   */
  private void initKeystore() {
    Configuration config = Configuration.getCurrentConfig();

    try {
      String keystoreLoc =
	config.get(PARAM_KEYSTORE_LOCATION,
		   DEFAULT_KEYSTORE_LOCATION);
      String keystorePass =
	config.get(PARAM_KEYSTORE_PASSWORD,
		   DEFAULT_KEYSTORE_PASSWORD);
      if (keystoreLoc == null || keystorePass == null) {
	log.error("Unable to load keystore!  Loadable plugins will " +
		  "not be available.");
      } else {
	keystore = KeyStore.getInstance("JKS", "SUN");
	if (keystoreLoc.startsWith(File.separator)) {
	  keystore.load(new FileInputStream(new File(keystoreLoc)),
			keystorePass.toCharArray());
	} else {
	  keystore.load(getClass().getClassLoader().getResourceAsStream(keystoreLoc),
			keystorePass.toCharArray());
	}
      }

    } catch (Exception ex) {
      // ensure the keystore is null.
      keystore = null;
      log.error("Unable to load keystore", ex);
      return;
    }

    log.debug("Keystore successfully initialized.");
    keystoreInited = true;
  }

  private boolean isKeystoreInited() {
    return keystoreInited;
  }

  // used by unit tests.
  public void setKeystoreInited(boolean val) {
    this.keystoreInited = val;
  }

  /**
   * Given a file representing a JAR, retrieve a list of available
   * plugin classes to load.
   */
  private List getJarPluginClasses(File blessedJar) throws IOException {
    JarFile jar = new JarFile(blessedJar);
    Manifest manifest = jar.getManifest();
    Map entries = manifest.getEntries();
    List plugins = new ArrayList();

    for (Iterator manIter = entries.keySet().iterator(); manIter.hasNext();) {
      String key = (String)manIter.next();

      Attributes attrs = manifest.getAttributes(key);

      if (attrs.containsKey(LOADABLE_PLUGIN_ATTR)) {
	String s = StringUtil.replaceString(key, "/", ".");

	String pluginName = null;

	if (StringUtil.endsWithIgnoreCase(key, ".class")) {
	  pluginName = StringUtil.replaceString(s, ".class", "");
	  log.debug2("Adding '" + pluginName + "' to plugin load list.");
	  plugins.add(pluginName);
	} else if (StringUtil.endsWithIgnoreCase(key, ".xml")) {
	  pluginName = StringUtil.replaceString(s, ".xml", "");
	  log.debug2("Adding '" + pluginName + "' to plugin load list.");
	  plugins.add(pluginName);
	}
      }

    }

    jar.close();

    return plugins;
  }

  /**
   * Run through the list of Registry AUs and verify and load any JARs
   * that need to be loaded.
   */
  public synchronized void processRegistryAus(List registryAus) {

    if (jarValidator == null) {
      jarValidator = new JarValidator(keystore, pluginDir);
    }

    // Create temporary plugin and classloader maps
    HashMap tmpMap = new HashMap();

    for (Iterator iter = registryAus.iterator(); iter.hasNext(); ) {
      ArchivalUnit au = (ArchivalUnit)iter.next();

      CachedUrlSet cus = au.getAuCachedUrlSet();
      LockssRepository repo = theDaemon.getLockssRepository(au);

      for (Iterator cusIter = cus.contentHashIterator(); cusIter.hasNext(); ) {
	CachedUrlSetNode cusn = (CachedUrlSetNode)cusIter.next();

	// TODO: Eventually this should be replaced with
	// "cusn.hasContent()", which will add another loop if it is a
	// CachedUrlSet.

	if (cusn.isLeaf()) {

	  // This CachedUrl represents a plugin JAR, validate it and
	  // process the plugins it contains.

	  CachedUrl cu = (CachedUrl)cusn;
	  String url = cu.getUrl();

	  if (StringUtil.endsWithIgnoreCase(url, ".jar")) {
	    RepositoryNode repoNode = null;
	    Integer curVersion = null;

	    try {
	      repoNode = repo.getNode(url);
	      curVersion = new Integer(repoNode.getCurrentVersion());
	    } catch (MalformedURLException ex) {
	      log.error("Malformed URL: Unable to get repository node " +
			"for cu URL " + url + ", skipping.");
	      continue;
	    }

	    if (cuNodeVersionMap.get(url) == null) {
	      cuNodeVersionMap.put(url, new Integer(-1));
	    }

	    // If we've already visited this CU, skip it unless the current
	    // repository node is a different version (older OR newer)
	    Integer oldVersion = (Integer)cuNodeVersionMap.get(url);

	    if (oldVersion.equals(curVersion)) {
	      log.debug2(url + ": JAR repository and map versions are identical.  Skipping...");
	      continue;
	    }

	    File blessedJar = null;
	    try {
	      // Validate and bless the JAR file from the CU.
	      blessedJar = jarValidator.getBlessedJar(cu);
	    } catch (IOException ex) {
	      log.error("Error processing jar file: " + url, ex);
	      continue;
	    } catch (JarValidator.JarValidationException ex) {
	      log.error("CachedUrl did not validate.", ex);
	      continue;
	    }

	    // Update the cuNodeVersion map now that we have the blessed Jar.
	    cuNodeVersionMap.put(url, curVersion);

	    if (blessedJar != null) {
	      // Get the list of plugins to load from this jar.
	      List loadPlugins = null;
	      try {
		loadPlugins = getJarPluginClasses(blessedJar);
	      } catch (IOException ex) {
		log.error("Error while getting list of plugins for " +
			  blessedJar);
		continue; // skip this CU.

	      }

	      // Although this -should- never happen, it's possible.
	      if (loadPlugins.size() == 0) {
		log.warning("Jar " + blessedJar +
			    " does not contain any plugins.  Skipping...");
		continue; // skip this CU.
	      }

	      // Load the plugin classes
	      LoadablePluginClassLoader pluginLoader = null;
	      try {
		pluginLoader = new LoadablePluginClassLoader(new URL[] { blessedJar.toURL() });
	      } catch (MalformedURLException ex) {
		log.error("Malformed URL exception attempting to create " +
			  "classloader for plugin JAR " + blessedJar);
		continue; // skip this CU.
	      }

	      String pluginName = null;

	      for (Iterator pluginIter = loadPlugins.iterator();
		   pluginIter.hasNext();) {
		pluginName = (String)pluginIter.next();
		String key = pluginKeyFromName(pluginName);

		Plugin plugin = null;

		try {
		  plugin = retrievePlugin(pluginName, pluginLoader);
		} catch (Exception ex) {
		  log.error("Unable to load plugin " + pluginName +
			    ", skipping: " + ex.getMessage());
		  continue;
		}

		PluginVersion version = null;

		try {
		  version = new PluginVersion(plugin.getVersion());
		} catch (IllegalArgumentException ex) {
		  // Don't let this runtime exception stop the daemon.  Skip the plugin.
		  log.error("Skipping plugin " + pluginName + ": " + ex.getMessage());
		  continue;
		}

		PluginInfo info = new PluginInfo(plugin, version, pluginLoader, url);

		if (pluginMap.containsKey(key)) {
		  // Plugin already exists in the global plugin map.
		  // If it has no currently configured AUs and this
		  // version is newer, replace it.  Otherwise, skip it
		  // and go on to the next plugin.
		  log.debug2("Plugin " + key + " is already in global pluginMap.");

		  if (plugin.getAllAus().size() > 0) {
		    if (log.isDebug2()) {
		      log.debug2("Plugin " + plugin.getPluginId() +
				 ": Already being used by " +
				 "configured AUs.  Skipping.");
		    }
		    continue; // skip plugin
		  } else {
		    Plugin otherPlugin = (Plugin)pluginMap.get(key);
		    PluginVersion otherVer =
		      new PluginVersion(otherPlugin.getVersion());
		    if (version.toLong() > otherVer.toLong()) {
		      if (log.isDebug2()) {
			log.debug2("No AUs currently configured for plugin " +
				   plugin.getPluginId() + " version " +
				   otherVer + ", replacing with newer version " +
				   version);
		      }
		      tmpMap.put(key, info);
		    } else {
		      if (log.isDebug2()) {
			log.debug2("No AUs currently configured for plugin " +
				   plugin.getPluginId() + ", but loadable plugin " +
				   "version is not newer.  Skipping.");
		      }
		    }
		  }
		} else if (!tmpMap.containsKey(key)) {
		  // Plugin doesn't yet exist in the temporary map, add it.
		  tmpMap.put(key, info);

		  if (log.isDebug2()) {
		    log.debug2("Plugin " + plugin.getPluginId() +
			       ": No previous version in temp map.");
		  }
		} else {
		  // Plugin already exists in the temporary map, use whichever
		  // version is higher.
		  PluginVersion otherVer = ((PluginInfo)tmpMap.get(key)).getVersion();

		  if (version.toLong() > otherVer.toLong()) {
		    if (log.isDebug2()) {
		      log.debug2("Plugin " + plugin.getPluginId() + ": version " +
				version + " is newer than version " + otherVer +
				" already in temp map, overwriting.");
		    }
		    // Overwrite old key in temp map
		    tmpMap.put(key, info);
		  }
		}
	      }
	    }
	  }
	}
      }
    }

    // After the temporary plugin map has been built, install it into
    // the global maps.
    for (Iterator pluginIter = tmpMap.keySet().iterator(); pluginIter.hasNext(); ) {
      String key = (String)pluginIter.next();
      log.debug2("Adding to plugin map: " + key);
      PluginInfo info = (PluginInfo)tmpMap.get(key);
      classloaderMap.put(key, info.getClassLoader());
      pluginCus.put(key, info.getCuUrl());
      setPlugin(key, info.getPlugin());
    }

    // Cleanup as a hint to GC.
    tmpMap.clear();
    tmpMap = null;
  }


  /**
   * CrawlManager callback that is responsible for handling Registry
   * AUs when they're finished with their initial crawls.
   */
  private static class RegistryCallback implements CrawlManager.Callback {
    private BinarySemaphore bs;

    List registryUrls = Collections.synchronizedList(new ArrayList());

    /*
     * Set the initial size of the list of registry URLs to process.
     */
    public RegistryCallback(List registryUrls, BinarySemaphore bs) {
      synchronized(registryUrls) {
	this.registryUrls.addAll(registryUrls);
      }
      this.bs = bs;
    }

    public void signalCrawlAttemptCompleted(boolean success, Object cookie) {
      String url = (String)cookie;

      processPluginsIfReady(url);
    }

    public void processPluginsIfReady(String url) {
      // Keep decrementing registryUrls.  When it's size 0 we're done
      // running crawls on all the plugin registries, and we can load
      // the plugin classes.
      synchronized(registryUrls) {
	registryUrls.remove(url);

	if (registryUrls.size() == 0) {
	  bs.give();
	}
      }
    }

    /**
     * Used only in the case that our semaphore throws an Interrupted
     * exception -- we can print this list to see what was left.
     */
    public List getRegistryUrls() {
      return registryUrls;
    }

    /**
     * Returns true if there is any reason to wait for this callback to
     * give() its binary semephore.
     */
    public boolean needsWait() {
      return (0 != registryUrls.size());
    }
  }

  /**
   * Status Accessor for Loadable Plugins.  Gives name, version, classname, and
   * registry JAR information for each loadable plugin the cache has installed.
   */
  private class LoadablePluginStatus implements StatusAccessor {
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("plugin", CatalogueOrderComparator.SINGLETON));

    private final List colDescs =
      ListUtil.list(
		    new ColumnDescriptor("plugin", "Name",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor("version", "Version",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor("id", "Plugin ID",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor("cu", "Loaded From",
					 ColumnDescriptor.TYPE_STRING)
		    );

    public String getDisplayName() {
      return "Loadable Plugins";
    }

    public boolean requiresKey() {
      return false;
    }

    public List getRows() {
      List table = new ArrayList();

      // All loadable plugins are in the classloader map.
      for (Iterator keysIter = classloaderMap.keySet().iterator(); keysIter.hasNext(); ) {
	Map row = new HashMap();

	String pluginKey = (String)keysIter.next();
	Plugin plugin = getPlugin(pluginKey);

	row.put("plugin", plugin.getPluginName());
	row.put("version", plugin.getVersion());
	row.put("id", plugin.getPluginId());
	row.put("cu", (String)pluginCus.get(pluginKey));

	table.add(row);
      }

      return table;
    }

    public void populateTable(StatusTable table) {
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows());
    }
  }

  /**
   * A simple class that wraps information about a loadable plugin,
   * used during the loading process.
   */
  private static class PluginInfo {
    private Plugin plugin;
    private PluginVersion version;
    private ClassLoader classLoader;
    private String cuUrl;

    public PluginInfo(Plugin plugin, PluginVersion version,
		      ClassLoader classLoader, String cuUrl) {
      this.plugin = plugin;
      this.version = version;
      this.classLoader = classLoader;
      this.cuUrl = cuUrl;
    }

    public Plugin getPlugin() {
      return plugin;
    }

    public PluginVersion getVersion() {
      return version;
    }

    public ClassLoader getClassLoader() {
      return classLoader;
    }

    public String getCuUrl() {
      return cuUrl;
    }
  }
}
