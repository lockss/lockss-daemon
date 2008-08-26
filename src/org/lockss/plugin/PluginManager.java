/*
 * $Id: PluginManager.java,v 1.191.2.1 2008-08-26 06:11:19 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import java.security.KeyStore;
import java.util.*;
import java.util.jar.*;

import org.apache.commons.collections.*;
import org.apache.commons.collections.map.*;
import org.apache.commons.collections.map.LRUMap;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.poller.PollSpec;
import org.lockss.state.*;
import org.lockss.util.*;

/**
 * Plugin global functionality
 *
 * @author  TAL
 * @version 0.0
 */
public class PluginManager
  extends BaseLockssDaemonManager implements ConfigurableManager {

  public static final String PREFIX = Configuration.PREFIX + "plugin.";

  public static final String PARAM_AU_TREE = Configuration.PREFIX + "au";

  /** The location on the platform to store downloaded plugins once they have
      been verified for loading. */
  static final String PARAM_PLUGIN_LOCATION =
    Configuration.PLATFORM + "pluginDir";
  static final String DEFAULT_PLUGIN_LOCATION = "plugins";

  /** A list of plugins to load at startup. */
  static final String PARAM_PLUGIN_REGISTRY = PREFIX + "registry";

  /** List of plugins not to load, or to remove if already loaded. */
  static final String PARAM_PLUGIN_RETRACT = PREFIX + "retract";

  static final String PARAM_REMOVE_STOPPED_AUS = PREFIX + "removeStoppedAus";
  static final boolean DEFAULT_REMOVE_STOPPED_AUS = true;

  /** Global list of plugin registry URLs. */
  static final String PARAM_PLUGIN_REGISTRIES = PREFIX + "registries";

  /** List of user-specified plugin registry URLs. */
  public static final String PARAM_USER_PLUGIN_REGISTRIES =
    PREFIX + "userRegistries";

  /** If true, default list of plugin registries from prop server is used
   * in addition to user-specified registries */
  public static final String PARAM_USE_DEFAULT_PLUGIN_REGISTRIES =
    PREFIX + "useDefaultRegistries";
  public static final boolean DEFAULT_USE_DEFAULT_PLUGIN_REGISTRIES = true;

  /** If true, default plugin signature keystore is used in addition to
   * user-specified keystore */
  public static final String PARAM_USE_DEFAULT_KEYSTORE =
    PREFIX + "useDefaultKeystore";
  public static final boolean DEFAULT_USE_DEFAULT_KEYSTORE = true;

  /** If true (the default), plugins that appear both in a loadable plugin
   * registry jar and on the local classpath will be loaded from the
   * downloaded registry jar.  If false such plugins will be loaded from
   * the local classpath.  This is useful in development, where
   * lockss-plugins.jar is on the classpath.  Normal true setting simulates
   * production behavior, loading plugins from registry.  Set false to test
   * plugins from the local build. */
  static final String PARAM_PREFER_LOADABLE_PLUGIN =
    PREFIX + "preferLoadable";
  static final boolean DEFAULT_PREFER_LOADABLE_PLUGIN = true;

  /** Common prefix of plugin keystore params */
  static final String KEYSTORE_PREFIX = PREFIX + "keystore.";

  /** The location of a Java JKS keystore to use for verifying
      loadable plugins (optional). */
  static final String PARAM_KEYSTORE_LOCATION = KEYSTORE_PREFIX + "location";
  static final String DEFAULT_KEYSTORE_LOCATION =
    "org/lockss/plugin/lockss.keystore";
  /** The password to use when opening the loadable plugin
      verification keystore (optional). */
  static final String PARAM_KEYSTORE_PASSWORD = KEYSTORE_PREFIX + "password";
  static final String DEFAULT_KEYSTORE_PASSWORD = "password";

  /** Common prefix of user-specified plugin keystore params */
  static final String USER_KEYSTORE_PREFIX = PREFIX + "userKeystore.";

  /** The location of a Java JKS keystore to use for verifying
      loadable plugins (optional). */
  public static final String PARAM_USER_KEYSTORE_LOCATION =
    USER_KEYSTORE_PREFIX + "location";
  /** The password to use when opening the loadable plugin
      verification keystore (optional). */
  public static final String PARAM_USER_KEYSTORE_PASSWORD =
    USER_KEYSTORE_PREFIX + "password";

  /** If true, accept plugins signed by otherwise-valid certificates that
   * are expired or not yet valid. */
  static final String PARAM_ACCEPT_EXPIRED_CERTS = 
    PREFIX + "acceptExpiredCertificates";
  static final boolean DEFAULT_ACCEPT_EXPIRED_CERTS = true;

  /** The amount of time to wait when processing loadable plugins.
      This process delays the start of AUs, so the timeout should not
      be too long. */
  public static final String PARAM_PLUGIN_LOAD_TIMEOUT =
    PREFIX + "load.timeout";
  public static final long DEFAULT_PLUGIN_LOAD_TIMEOUT =
    Constants.MINUTE;

  /** The type of plugin we prefer to load, if both are present.
      Can be either "class" or "xml" (case insensitive) */
  public static final String PARAM_PREFERRED_PLUGIN_TYPE =
    PREFIX + "preferredType";
  public static final String DEFAULT_PREFERRED_PLUGIN_TYPE =
    "xml";

  /** Root of TitleSet definitions.  */
  public static final String PARAM_TITLE_SETS =
    Configuration.PREFIX + "titleSet";

  static final String TITLE_SET_PARAM_CLASS = "class";
  static final String TITLE_SET_PARAM_NAME = "name";

  static final String TITLE_SET_CLASS_XPATH = "xpath";
  static final String TITLE_SET_XPATH_XPATH = "xpath";

  static final String TITLE_SET_CLASS_ALL_TITLES = "AllTitles";
  static final String TITLE_SET_CLASS_ACTIVE_AUS = "ActiveAus";
  static final String TITLE_SET_CLASS_INACTIVE_AUS = "InactiveAus";


  // prefix for non-plugin AU params
  public static final String AU_PARAM_RESERVED = "reserved";
  // per AU params known to and processed by daemon, not plugin
  public static final String AU_PARAM_DISABLED = AU_PARAM_RESERVED + ".disabled";
  public static final String AU_PARAM_REPOSITORY = AU_PARAM_RESERVED + ".repository";
  public static final String AU_PARAM_DISPLAY_NAME = AU_PARAM_RESERVED + ".displayName";

  public static final List NON_USER_SETTABLE_AU_PARAMS =
    Collections.unmodifiableList(new ArrayList());

  static final String CONFIGURABLE_PLUGIN_NAME =
    DefinablePlugin.class.getName();

  private static Logger log = Logger.getLogger("PluginMgr");

  private boolean useDefaultPluginRegistries =
    DEFAULT_USE_DEFAULT_PLUGIN_REGISTRIES;

  private ConfigManager configMgr;

  private File pluginDir = null;
  private AuOrderComparator auComparator = new AuOrderComparator();

  private final Attributes.Name LOADABLE_PLUGIN_ATTR =
    new Attributes.Name("Lockss-Plugin");

  // List of names of plugins not to load
  List retract = null;

  // maps plugin key(not id) to plugin
  private Map pluginMap = Collections.synchronizedMap(new HashMap());
  // maps auid to AU
  private Map auMap = Collections.synchronizedMap(new HashMap());
  // A set of all aus sorted by title.  The UI relies on this behavior.
  private Set auSet = new TreeSet(auComparator);
  private List<ArchivalUnit> auList = null;

  // maps host to collections of AUs.  Used to quickly locate candidate AUs
  // for incoming URLs.  Each collection is sorted in AU order (for proxy
  // manifest index display).
  private MultiMap hostAus = new HostAuMap();

  private static class HostAuMap extends MultiValueMap {
    HostAuMap() {
      super(new HashMap(),
	    new org.apache.commons.collections.Factory() {
	      public Object create() {
		return new ArrayList(3);
	      }});
    }
  }

  private Set inactiveAuIds = Collections.synchronizedSet(new HashSet());

  private List auEventHandlers = new ArrayList();

  // Plugin registry processing
  private Map cuNodeVersionMap = Collections.synchronizedMap(new HashMap());
  // Map of plugin key to PluginInfo
  private Map pluginfoMap = Collections.synchronizedMap(new HashMap());
  private RegistryPlugin regPlugin;

  private KeyStore keystore;
  private JarValidator jarValidator;
  private boolean keystoreInited = false;
  private boolean loadablePluginsReady = false;
  private boolean preferLoadablePlugin = DEFAULT_PREFER_LOADABLE_PLUGIN;
  private long registryTimeout = DEFAULT_PLUGIN_LOAD_TIMEOUT;
  private boolean acceptExpiredCertificates = DEFAULT_ACCEPT_EXPIRED_CERTS;

  private Map titleMap = null;
  private List allTitles = null;
  private List allTitleConfigs = null;
  private Map titleSetMap;
  private TreeSet titleSets;

  private boolean explodedPluginLoaded = false;
  private Plugin explodedPlugin = null;

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
    // Initialize the plugin directory.
    initPluginDir();
    PluginStatus.register(getDaemon(), this);
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
    auEventHandlers = new ArrayList();
    PluginStatus.unregister(getDaemon());
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

    Configuration config = CurrentConfig.getCurrentConfig();
    log.debug("Initializing loadable plugin registries before starting AUs");
    initLoadablePluginRegistries(getPluginRegistryUrls(config));
    initPluginRegistry(config);
    Class explodedPluginClass = null;
    String name = "org.lockss.plugin.exploded.ExplodedPlugin";
    if (!explodedPluginLoaded) {
      explodedPluginLoaded = true;
      try {
	explodedPluginClass = Class.forName(name);
      } catch (ClassNotFoundException ex) {
	log.warning("No class " + name);
      }
      if (explodedPluginClass != null) {
	explodedPlugin = loadBuiltinPlugin(explodedPluginClass);
      }
    }
    configureAllPlugins(config);
    loadablePluginsReady = true;
  }

  public void setLoadablePluginsReady(boolean val) {
    loadablePluginsReady = val;
  }

  List getPluginRegistryUrls(Configuration config) {
    if (useDefaultPluginRegistries) {
      return ListUtil.append(config.getList(PARAM_PLUGIN_REGISTRIES),
			     config.getList(PARAM_USER_PLUGIN_REGISTRIES));
    } else {
      return config.getList(PARAM_USER_PLUGIN_REGISTRIES);
    }
  }

  public boolean areAusStarted() {
    return getDaemon().areAusStarted();
  }

  Configuration currentAllPlugs = ConfigManager.EMPTY_CONFIGURATION;

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {

    if (changedKeys.contains(PREFIX)) {
      registryTimeout = config.getTimeInterval(PARAM_PLUGIN_LOAD_TIMEOUT,
					       DEFAULT_PLUGIN_LOAD_TIMEOUT);

      preferLoadablePlugin = config.getBoolean(PARAM_PREFER_LOADABLE_PLUGIN,
					       DEFAULT_PREFER_LOADABLE_PLUGIN);

      acceptExpiredCertificates =
	config.getBoolean(PARAM_ACCEPT_EXPIRED_CERTS,
			  DEFAULT_ACCEPT_EXPIRED_CERTS);

      // must set retract before loadablePluginsReady is true as
      // retrievePlugin() may be called before that
      if (changedKeys.contains(PARAM_PLUGIN_RETRACT)) {
	retract = config.getList(PARAM_PLUGIN_RETRACT);
      }

      // If the keystore or password has changed, update.
      if (changedKeys.contains(KEYSTORE_PREFIX) ||
	  changedKeys.contains(USER_KEYSTORE_PREFIX)) {
	keystoreInited = false;
	initKeystore(configMgr.getCurrentConfig());
      }

      useDefaultPluginRegistries =
	config.getBoolean(PARAM_USE_DEFAULT_PLUGIN_REGISTRIES,
			  DEFAULT_USE_DEFAULT_PLUGIN_REGISTRIES);

    }

    // Process any changed TitleSets
    if (changedKeys.contains(PARAM_TITLE_SETS)) {
      configureTitleSets(config);
    }

    // Don't load or start other plugins until the daemon is running.
    if (loadablePluginsReady) {
      // Process loadable plugin registries.
      if (changedKeys.contains(PARAM_PLUGIN_REGISTRIES) ||
	  changedKeys.contains(PARAM_USER_PLUGIN_REGISTRIES) ||
	  changedKeys.contains(PARAM_USE_DEFAULT_PLUGIN_REGISTRIES)) {
	initLoadablePluginRegistries(getPluginRegistryUrls(config));
      }

      // Process the built-in plugin registry.
      if (changedKeys.contains(PARAM_PLUGIN_REGISTRY) ||
	  changedKeys.contains(PARAM_PLUGIN_RETRACT)) {
	initPluginRegistry(config);
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
      List plugList = ListUtil.fromIterator(allPlugs.nodeIterator());
      plugList = CollectionUtil.randomPermutation(plugList);
      for (Iterator iter = plugList.iterator(); iter.hasNext(); ) {
	String pluginKey = (String)iter.next();
	log.debug2("Configuring plugin key: " + pluginKey);
	Configuration pluginConf = allPlugs.getConfigTree(pluginKey);
	Configuration prevPluginConf =
	  currentAllPlugs.getConfigTree(pluginKey);

	configurePlugin(pluginKey, pluginConf, prevPluginConf);
      }
      currentAllPlugs = allPlugs;
    }
  }

  private void configureTitleSets(Configuration config) {
    Map map = new HashMap();
    TreeSet list = new TreeSet();
    Configuration allSets = config.getConfigTree(PARAM_TITLE_SETS);
    for (Iterator iter = allSets.nodeIterator(); iter.hasNext(); ) {
      String id = (String)iter.next();
      Configuration setDef = allSets.getConfigTree(id);
      try {
	TitleSet ts = createTitleSet(setDef);
	if (ts != null) {
	  if (log.isDebug2()) {
	    log.debug2("Adding TitleSet: " + ts);
	  }
	  list.add(ts);
	} else {
	  log.warning("Null TitleSet created from: " + setDef);
	}
      } catch (RuntimeException e) {
	log.warning("Error creating TitleSet from: " + setDef, e);
      }
    }
    for (Iterator iter = list.iterator(); iter.hasNext(); ) {
      TitleSet ts = (TitleSet)iter.next();
      map.put(ts.getName(), ts);
    }
    titleSets = list;
    titleSetMap = map;
  }

  private TitleSet createTitleSet(Configuration config) {
    String cls = config.get(TITLE_SET_PARAM_CLASS);
    String name = config.get(TITLE_SET_PARAM_NAME);
    try {
      if (cls.equalsIgnoreCase(TITLE_SET_CLASS_XPATH)) {
	return new TitleSetXpath(getDaemon(), name,
				 config.get(TITLE_SET_XPATH_XPATH));
      }
      if (cls.equalsIgnoreCase(TITLE_SET_CLASS_ALL_TITLES)) {
	return new TitleSetAllTitles(getDaemon());
      }
      if (cls.equalsIgnoreCase(TITLE_SET_CLASS_ACTIVE_AUS)) {
	return new TitleSetActiveAus(getDaemon());
      }
      if (cls.equalsIgnoreCase(TITLE_SET_CLASS_INACTIVE_AUS)) {
	return new TitleSetInactiveAus(getDaemon());
      }
    } catch (Exception e) {
      log.error("Error creating TitleSet", e);
    }
    return null;
  }

  /** Return the TitleSet name to {@link org.lockss.daemon.TitleSet}
   * mapping */
  public Map getTitleSetMap() {
    return titleSetMap;
  }

  /** Return the TitleSets, in display order */
  public Collection getTitleSets() {
    return titleSets;
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
   * Return the plugin's key
   */
  public static String getPluginKey(Plugin plugin) {
    return pluginKeyFromId(plugin.getPluginId());
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
	String val = auConfig.get(key);
	if (val == null) {
	  throw new NullPointerException(key + " is null in: " + auConfig);
	}
	props.setProperty(key, val);
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

  private void configurePlugin(String pluginKey, Configuration pluginConf,
			       Configuration oldPluginConf) {
    List auList = ListUtil.fromIterator(pluginConf.nodeIterator());
    auList = CollectionUtil.randomPermutation(auList);
    nextAU:
    for (Iterator iter = auList.iterator(); iter.hasNext(); ) {
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
	  if (log.isDebug3())
	    log.debug3("AU already configured, not reconfiguring: " + auKey);
	} else {
	  log.debug("Configuring AU id: " + auKey);
	  boolean pluginOk = ensurePluginLoaded(pluginKey);
	  if (pluginOk) {
	    Plugin plugin = getPlugin(pluginKey);
	    try {
	      String genAuid = generateAuId(plugin, auConf);
	      if (!auId.equals(genAuid)) {
		log.warning("Generated AUID " + genAuid +
			    " does not match stored AUID " + auId +
			    ". Proceeding anyway.");
	      }
	    } catch (RuntimeException e) {
	      log.warning("Not configuring probable non-AU.  " +
			  "Can't generate AUID from config: " + auConf);
	      continue nextAU;
	    }
	    configureAu(plugin, auConf, auId);
	    inactiveAuIds.remove(generateAuId(pluginKey, auKey));
	  } else {
	    log.warning("Not configuring AU " + auKey);
	  }
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
    Configuration oldConfig = null;
    try {
      ArchivalUnit oldAu = (ArchivalUnit)auMap.get(auId);
      if (oldAu != null) {
	oldConfig = oldAu.getConfiguration();
	if (auConf.equals(oldConfig)) {
	  // Don't bother if the config is the same.  (This happens the
	  // first time config is loaded after AU created via UI.)
	  return;
	}
      }
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
	signalAuEvent(au, AU_CHANGE_RECONFIG, oldConfig);
      } else {
	log.debug("Configured AU " + au);
	putAuInMap(au);
	signalAuEvent(au, AU_CHANGE_CREATED, null);
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
      signalAuEvent(au, AU_CHANGE_CREATED, null);
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
    synchronized (auSet) {
      auMap.remove(auid);
      auSet.remove(au);
      auList = null;
    }
    delHostAus(au);

    signalAuEvent(au, AU_CHANGE_DELETED, null);

    try {
      Plugin plugin = au.getPlugin();
      plugin.stopAu(au);
      getDaemon().stopAuManagers(au);
    } catch (Exception e) {
      log.warning("Unexpected stopping AU", e);
      // Shouldn't happen, as stopAuManagers() catches errors in
      // stopService().  Not clear what to do anyway, if some of the
      // managers don't stop cleanly.
    }
    return true;
  }

  static final int AU_CHANGE_CREATED = 1;
  static final int AU_CHANGE_DELETED = 2;
  static final int AU_CHANGE_RECONFIG = 3;

  /**
   * Register a handler for AU events: create, delete, reconfigure.  May be
   * called after this manager's initService() (before startService()).
   * @param aueh AuEventHandler to add
   */
  public void registerAuEventHandler(AuEventHandler aueh) {
    log.debug3("registering " + aueh);
    if (!auEventHandlers.contains(aueh)) {
      auEventHandlers.add(aueh);
    }
  }

  /**
   * Unregister an AuEventHandler
   * @param aueh AuEventHandler to remove
   */
  public void unregisterAuEventHandler(AuEventHandler aueh) {
    log.debug3("unregistering " + aueh);
    auEventHandlers.remove(aueh);
  }

  void signalAuEvent(ArchivalUnit au, int how, Configuration oldAuConfig) {
    if (log.isDebug2()) log.debug2("AuEvent " + how + ": " + au);
    // copy the list of handler as it could change during the loop.
    List handlers = new ArrayList(auEventHandlers);
    for (Iterator iter = handlers.iterator(); iter.hasNext();) {
      try {
	AuEventHandler hand = (AuEventHandler)iter.next();
	switch (how) {
	case AU_CHANGE_CREATED:
	  hand.auCreated(au);
	  break;
	case AU_CHANGE_DELETED:
	  hand.auDeleted(au);
	  break;
	case AU_CHANGE_RECONFIG:
	  hand.auReconfigured(au, oldAuConfig);
	  break;
	}
      } catch (Exception e) {
	log.error("AuEventHandler threw", e);
      }
    }
  }

  protected void putAuInMap(ArchivalUnit au) {
    log.debug2("putAuMap(" + au.getAuId() +", " + au);
    synchronized (auSet) {
      auMap.put(au.getAuId(), au);
      auSet.add(au);
      auList = null;
    }
    addHostAus(au);
  }

  public ArchivalUnit getAuFromId(String auId) {
    ArchivalUnit au = (ArchivalUnit)auMap.get(auId);
    if (log.isDebug3()) log.debug3("getAu(" + auId + ") = " + au);
    return au;
  }

  public void addHostAus(ArchivalUnit au) {
    try {
      Collection stems = au.getUrlStems();
      if (stems != null) {
	synchronized (hostAus) {
	  for (Iterator iter = stems.iterator(); iter.hasNext();) {
	    String stem = (String)iter.next();
	    stem = UrlUtil.getUrlPrefix(UrlUtil.normalizeUrl(stem));
	    log.debug2("Adding stem: " + stem + ", " + au);
	    hostAus.put(stem, au);
	  }
	}
      }
    } catch (Exception e) {
      log.warning("addHostAus()", e);
    }
  }

  private void delHostAus(ArchivalUnit au) {
    try {
      Collection stems = au.getUrlStems();
      if (stems != null) {
	synchronized (hostAus) {
	  for (Iterator iter = stems.iterator(); iter.hasNext();) {
	    String stem = (String)iter.next();
	    stem = UrlUtil.getUrlPrefix(UrlUtil.normalizeUrl(stem));
	    log.debug2("Removing stem: " + stem + ", " + au);
	    hostAus.remove(stem, au);
	  }
	}
      }
    } catch (Exception e) {
      log.warning("delHostAus()", e);
    }
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
    setAndSaveAuConfiguration(au,
			      ConfigManager.fromPropertiesUnsealed(auProps));
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
      throws IOException {
    if (!auConf.isEmpty()) {
      if (!auConf.isSealed()) {
	auConf.put(AU_PARAM_DISPLAY_NAME, au.getName());
      } else if (StringUtil.isNullString(auConf.get(AU_PARAM_DISPLAY_NAME))) {
	log.debug("Can't add name to sealed AU config: " + auConf,
		  new Throwable());
      }
    }
    updateAuConfigFile(au.getAuId(), auConf);
  }

  public void updateAuConfigFile(String auid, Configuration auConf)
      throws IOException {
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
   * @throws IOException
   */
  public void deleteAuConfiguration(ArchivalUnit au) throws IOException {
    log.debug("Deleting AU config: " + au);
    updateAuConfigFile(au, ConfigManager.EMPTY_CONFIGURATION);
  }

  /**
   * Delete AU configuration from the local config file.  Need to find a
   * better place for this.
   * @param auid the AuId
   * @throws IOException
   */
  public void deleteAuConfiguration(String auid) throws IOException {
    log.debug("Deleting AU config: " + auid);
    updateAuConfigFile(auid, ConfigManager.EMPTY_CONFIGURATION);
    // might be deleting an inactive au
    inactiveAuIds.remove(auid);
  }

  /**
   * Deactivate an AU in the config file.  Does not actually stop the AU.
   * @param au the ArchivalUnit to be deactivated
   * @throws IOException
   */
  public void deactivateAuConfiguration(ArchivalUnit au) throws IOException {
    log.debug("Deactivating AU: " + au);
    Configuration config = getStoredAuConfiguration(au);
    if (config.isSealed()) {
      config = config.copy();
    }
    config.put(AU_PARAM_DISABLED, "true");
    updateAuConfigFile(au, config);
  }

  /**
   * Delete an AU
   * @param au the ArchivalUnit to be deleted
   * @throws IOException
   */
  public void deleteAu(ArchivalUnit au) throws IOException {
    deleteAuConfiguration(au);
    if (isRemoveStoppedAus()) {
      stopAu(au);
    }
  }

  /**
   * Deactivate an AU
   * @param au the ArchivalUnit to be deactivated
   * @throws IOException
   */
  public void deactivateAu(ArchivalUnit au) throws IOException {
    deactivateAuConfiguration(au);
    if (isRemoveStoppedAus()) {
      String auid = au.getAuId();
      stopAu(au);
      inactiveAuIds.add(auid);
    }
  }

  public boolean isRemoveStoppedAus() {
    return CurrentConfig.getBooleanParam(PARAM_REMOVE_STOPPED_AUS,
					 DEFAULT_REMOVE_STOPPED_AUS);
  }

  /**
   * Return true if the specified Archival Unit is used internally
   * by the LOCKSS daemon.  Currently this is only registry AUs
   */
  public boolean isInternalAu(ArchivalUnit au) {
    return au instanceof RegistryArchivalUnit;
  }

  /**
   * Return true if the specified Archival Unit is a plugin registry AU
   */
  public boolean isRegistryAu(ArchivalUnit au) {
    return au instanceof RegistryArchivalUnit;
  }

  /**
   * Return true if the specified Plugin is used internally
   * by the LOCKSS daemon.
   */
  public boolean isInternalPlugin(Plugin plugin) {
    return plugin instanceof RegistryPlugin;
  }

  /**
   * Return true if the specified Plugin is a Loadable plugin
   */
  public boolean isLoadablePlugin(Plugin plugin) {
    PluginInfo info = (PluginInfo)pluginfoMap.get(getPluginKey(plugin));
    if (info == null) {
      return false;
    }
    return info.isOnLoadablePath();
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
   * @return the AU's Configuration, with unprefixed keys.
   */
  public Configuration getCurrentAuConfiguration(String auid) {
    String aukey = configKeyFromAuId(auid);
    String prefix = PARAM_AU_TREE + "." + aukey;
    return ConfigManager.getCurrentConfig().getConfigTree(prefix);
  }

  // Loadable Plugin Support

  /**
   * Retrieve a plugin from the specified classloader.  If the
   * clasloader is 'null', this method will use the default
   * classloader.
   */
  PluginInfo retrievePlugin(String pluginKey, ClassLoader loader)
      throws Exception {
    if (pluginfoMap.containsKey(pluginKey)) {
      return (PluginInfo)pluginfoMap.get(pluginKey);
    }
    if (pluginMap.containsKey(pluginKey)) {
      return new PluginInfo((Plugin)pluginMap.get(pluginKey), loader, null);
    }
    String pluginName = pluginNameFromKey(pluginKey);
    if (retract != null && !retract.isEmpty()) {
      if (retract.contains(pluginName)) {
	log.debug3("Not loading " + pluginName +
		   " because it's on the retract list");
	return null;
      }
    }
    if (loader == null) {
      loader = this.getClass().getClassLoader();
    }

    try {
      return loadPlugin(pluginKey, loader);
    } catch (PluginException.PluginNotFound e) {
      log.error("Plugin not found: " + pluginName);
    } catch (PluginException.LinkageError e) {
      log.error("Can't load plugin: " + pluginName, e);
    } catch (PluginException.IncompatibleDaemonVersion e) {
      log.error("Incompatible Plugin: " + e.getMessage());
    } catch (Exception e) {
      log.error("Can't load plugin: " + pluginName, e);
    }
    return null;
  }

  /**
   * Load a plugin from the specified classloader.  If the
   * clasloader is 'null', this method will use the default
   * classloader.
   */
  public PluginInfo loadPlugin(String pluginKey, ClassLoader loader)
      throws Exception {
    String pluginName = pluginNameFromKey(pluginKey);
    if (loader == null) {
      loader = this.getClass().getClassLoader();
    }

    // First look for a loadable plugin definition.
    try {
      log.debug3(pluginName + ": Looking for XML definition.");
      Class c = Class.forName(getConfigurablePluginName(), true, loader);
      DefinablePlugin xmlPlugin = (DefinablePlugin)c.newInstance();
      xmlPlugin.initPlugin(getDaemon(), pluginName, loader);
      if (isCompatible(xmlPlugin)) {
	// found a compatible plugin, return it
	String url = xmlPlugin.getLoadedFrom();
	PluginInfo info = new PluginInfo(xmlPlugin, loader, url);
	return info;
      } else {
	xmlPlugin.stopPlugin();
	log.warning("Plugin " + pluginName +
		    " not started because it requires daemon version " +
		    xmlPlugin.getRequiredDaemonVersion());
      }
    } catch (FileNotFoundException ex) {
      log.debug2("No XML plugin: " + pluginName + ": " + ex);
    }
    // throw any other exception

    // If didn't find an XML plugin look for a Plugin class.
    try {
      log.debug3(pluginName + ": Looking for class.");
      Class c = Class.forName(pluginName, true, loader);
      Plugin classPlugin = (Plugin)c.newInstance();
      classPlugin.initPlugin(getDaemon());
      if (isCompatible(classPlugin)) {
	String path = pluginName.replace('.', '/').concat(".class");
	URL url = loader.getResource(path);
	PluginInfo info = new PluginInfo(classPlugin, loader, url.toString());
	return info;
      } else {
	classPlugin.stopPlugin();
	String req = " requires daemon version "
	  + classPlugin.getRequiredDaemonVersion();
	throw new PluginException.IncompatibleDaemonVersion("Plugin " +
							    pluginName + req);
      }
    } catch (ClassNotFoundException ex) {
      throw new PluginException.PluginNotFound("Plugin " + pluginName
					       + " could not be found");
    } catch (LinkageError e) {
      throw new PluginException.LinkageError("Plugin " + pluginName
					       + " could not be loaded", e);
    }
  }

  boolean isCompatible(Plugin plug) {
    boolean res;
    DaemonVersion dver = getDaemonVersion();
    if (dver == null) {
      res = true; // don't break things during testing
    } else {
      DaemonVersion preq = new DaemonVersion(plug.getRequiredDaemonVersion());
      res = dver.compareTo(preq) >= 0;
    }
    if (log.isDebug3())
      log.debug3("Plugin is " + (res ? "" : "not ") +
		 "compatible with daemon " + dver);
    return res;
  }

  // overridable for testing
  protected DaemonVersion getDaemonVersion() {
    return ConfigManager.getDaemonVersion();
  }

  /**
   * (package-level access for unit testing)
   */
  int getPreferredPluginType() {
    String preferredPlugin =
      CurrentConfig.getCurrentConfig().get(PARAM_PREFERRED_PLUGIN_TYPE,
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
    if (pluginMap.containsKey(pluginKey)) {
      return true;
    }

    ClassLoader loader = null;
    PluginInfo info = (PluginInfo)pluginfoMap.get(pluginKey);
    if (info != null) {
      loader = info.getClassLoader();
    }
    if (loader == null) {
      loader = this.getClass().getClassLoader();
    }

    String pluginName = "";
    try {
      pluginName = pluginNameFromKey(pluginKey);
      log.debug3("Trying to retrieve "+pluginKey);
      info = retrievePlugin(pluginKey, loader);
      if (info != null) {
	setPlugin(pluginKey, info.getPlugin());
	pluginfoMap.put(pluginKey, info);
	return true;
      } else {
	log.debug("Couldn't retrieve "+pluginKey);
	return false;
      }
    } catch (Exception e) {
      log.error("Error instantiating " + pluginName, e);
      return false;
    }
  }

  public Plugin loadBuiltinPlugin(Class pluginClass) {
    return loadBuiltinPlugin(pluginClass.getName());
  }

  protected Plugin loadBuiltinPlugin(String pluginClassName) {
    String pluginKey = pluginKeyFromName(pluginClassName);
    if (ensurePluginLoaded(pluginKey)) {
      return (Plugin)pluginMap.get(pluginKey);
    }
    return null;
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

  void removePlugin(String key) {
    log.debug("Removing plugin " + key);
    pluginMap.remove(key);
    pluginfoMap.remove(key);
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
    } else if ((spec.getLwrBound() != null) &&
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
   * Find an AU's top level CachedUrlSet.
   *
   * @param auId
   * @return top level <code>CachedUrlSet</code> for the specified auId
   */
  public CachedUrlSet findCachedUrlSet(String auId) {
    ArchivalUnit au = getAuFromId(auId);
    if (au == null) return null;
    return au.getAuCachedUrlSet();
  }

  /**
   * Searches for an AU that contains the URL and returns the corresponding
   * CachedUrl.
   * @param url The URL to search for.
   * @return a CachedUrl, or null if URL not present in any AU
   */
  public CachedUrl findCachedUrl(String url) {
    return findTheCachedUrl(url, true);
  }

  /**
   * Searches for an AU that contains the URL and returns the corresponding
   * CachedUrl.
   * @param url The URL to search for.
   * @param withContent true if the CachedUrl must have content
   * @return a CachedUrl, or null if URL not present in any AU
   */
  public CachedUrl findCachedUrl(String url, boolean withContent) {
    return findTheCachedUrl(url, withContent);
  }

  /** Return a collection of all AUs that have content on the host of this
   * url, sorted in AU title order.  */
  // This method needs to copy the list anyway, to avoid CME, so this is as
  // good a place as any to sort it.
  //  XXX Should do something about the redundant normalization involved in
  // calling more than one of these methods
  public Collection getCandidateAus(String url) throws MalformedURLException {
    String normStem = UrlUtil.getUrlPrefix(UrlUtil.normalizeUrl(url));
    return getCandidateAusFromStem(normStem);
  }

  public Collection getCandidateAusFromStem(String normStem) {
    synchronized (hostAus) {
      Collection cand = (Collection)hostAus.get(normStem);
      if (cand != null) {
	Set res = new TreeSet(new AuOrderComparator());
	res.addAll(cand);
	cand = res;
      }
      return cand;
    }
  }  

  /** Return a collection of all AUs that have content on the host of this
   * url, sorted in AU title order.  */
  // This method needs to copy the list anyway, to avoid CME, so this is as
  // good a place as any to sort it.
  //  XXX Should do something about the redundant normalization involved in
  // calling more than one of these methods
  public SortedSet getAllStems() {
    synchronized (hostAus) {
      return new TreeSet(hostAus.keySet());
    }
  }

  // Return actual list of candiate AUs, used only for testing
  List getRawCandidateAus(String url) throws MalformedURLException {
    String normStem = UrlUtil.getUrlPrefix(UrlUtil.normalizeUrl(url));
    synchronized (hostAus) {
      return (List)hostAus.get(normStem);
    }
  }

  private Map recentCuMap = Collections.synchronizedMap(new LRUMap(20));


  /** Find a CachedUrl for the URL.  
   */
  private CachedUrl findTheCachedUrl(String url, boolean withContent) {
    // Maintain a small cache of URL -> CU.  When ICP is in use, each URL
    // will likely be looked up twice in quick succession

    CachedUrl res = (CachedUrl)recentCuMap.get(url);
    if (log.isDebug3()) {
      if (res != null) {
	log.debug3("cache hit " + res.toString() +
		   (res.hasContent() ? "with" : "without") + " content.");
      } else {
	log.debug3("cache miss for " + url);
      }
    }
    if (res == null || (withContent && !res.hasContent())) {
      res = findTheCachedUrl0(url, withContent);
      if (res != null) {
	recentCuMap.put(url, res);
      }
    }
    return res;
  }

  private CachedUrl findTheCachedUrl0(String url, boolean withContent) {
    // We don't know what AU it might be in, so can't do plugin-dependent
    // normalization yet.  But only need to do generic normalization once.
    // XXX This is wrong, as plugin-specific normalization is normally done
    // first.
    //
    // XXX There is a problem with this when used by *Exploder() classes.
    // In the CLOCKSS case,  we expect huge numbers of AUs to share
    // the same stem,  eg. http://www.elsevier.com/ and each archive
    // that is exploded to include URLs for a large number of them.
    // The optimization that returns the most recent one if it matches
    // will help,  but perhaps not enough.  Ideally we want to search
    // for AUs on the basis of their base_url,  which for
    // ExplodedArchiveUnits is their sole definitional parameter,  so
    // is known unique.
    String normUrl;
    String normStem;
    try {
      normUrl = UrlUtil.normalizeUrl(url);
      normStem = UrlUtil.getUrlPrefix(normUrl);
    } catch (MalformedURLException e) {
      log.warning("findTheCachedUrl(" + url + ")", e);
      return null;
    }
    synchronized (hostAus) {
      ArrayList candidateAus = (ArrayList)hostAus.get(normStem);
      if (candidateAus == null) {
	log.debug3("findTheCachedUrl: No AUs for " + normStem);
	return null;
      }
      CachedUrl bestCu = null;
      int bestAuIx = -1;
      int bestScore = 8;
      int auIx = 0;
      for (Iterator iter = candidateAus.iterator(); iter.hasNext(); auIx++) {
	ArchivalUnit au = (ArchivalUnit)iter.next();
	log.debug3("findTheCachedUrl: " + normUrl + " check " + au.toString());
	if (au.shouldBeCached(normUrl)) {
	  log.debug3("findTheCachedUrl: " + normUrl + " should be in "
		     + au.getAuId());
	  try {
	    String siteUrl = UrlUtil.normalizeUrl(normUrl, au);
	    CachedUrl cu = au.makeCachedUrl(siteUrl);
	    log.debug3("findTheCachedUrl: " + siteUrl + " got " +
		       (cu == null ? "no cu" : cu.toString()));
	    if (cu != null && (!withContent || cu.hasContent())) {
	      int score = score(au, cu);
	      if (score == 0) {
		makeFirstCandidate(candidateAus, auIx);
		log.debug3("findTheCachedUrl: " + siteUrl + " is it");
		return cu;
	      }
	      if (score < bestScore) {
		AuUtil.safeRelease(bestCu);
		bestCu = cu;
		bestAuIx = auIx;
		bestScore = score;
	      } else {
		cu.release();
	      }
	    }
	  } catch (MalformedURLException ignore) {
	    // ignored
	  } catch (PluginBehaviorException ignore) {
	    // ignored
	  }
	}
      }
      makeFirstCandidate(candidateAus, bestAuIx);
      log.debug3("bestCu was " + (bestCu == null ? "null" : bestCu.toString()));
      return bestCu;
    }
  }

  // Move the AU in which we found the CU to the head of the list, as it's
  // likely next request will be for the same AU.
  private void makeFirstCandidate(List lst, int ix) {
    if (ix > 0) {
      synchronized (hostAus) {
	Collections.swap(lst, ix, 0);
      }
    }
  }

  // Combine the various elements of desirability into a single score;
  // lower is better, zero is best.
  private int score(ArchivalUnit au, CachedUrl cu) {
    if (cu == null) return 4;
    int res = 0;
    if (isUnsubscribed(au)) res += 2;
    if (isDamaged(cu)) res += 1;
    return res;
  }

  private boolean isUnsubscribed(ArchivalUnit au) {
    return (getDaemon().isDetectClockssSubscription() &&
	    (AuUtil.getAuState(au).getClockssSubscriptionStatus() !=
	     AuState.CLOCKSS_SUB_YES));
  }

  // XXX
  private boolean isDamaged(CachedUrl cu) {
    return false;
  }

//   // return true if cu1 is newer than cu2, or cu2 is null
//   // XXX - should compare last-modified times, or crawl times?
//   private boolean cuNewerThan(CachedUrl cu1, CachedUrl cu2) {
//     if (cu2 == null) return true;
//     return false;
// //     CIProperties p1 = cu1.getProperties();
// //     CIProperties p2 = cu2.getProperties();
//     //     Long.parseLong(p1.getProperty(HttpFields.__LastModified, "-1"));
//   }

  /**
   * Return a list of all configured ArchivalUnits.  The UI relies on
   * this being sorted by au title, and so we return a copy of auSet,
   * which is kept in the right order.
   *
   * @return the List of aus
   */
  public List<ArchivalUnit> getAllAus() {
    synchronized (auSet) {
      if (auList == null) {
	auList = new ArrayList<ArchivalUnit>(auSet);
      }
      return auList;
    }
  }

  /**
   * Return a randomly ordered list of all AUs.
   */
  // putting this here in PluginManager saves having to make an extra copy
  // of auSet
  public List getRandomizedAus() {
    synchronized (auSet) {
      return CollectionUtil.randomPermutation(auSet);
    }
  }

  public Collection getInactiveAuIds() {
    return inactiveAuIds;
  }

  /** Return a collection of all RegistryArchivalUnits.  This is a subset
   * of getAllAus() */
  public Collection<ArchivalUnit> getAllRegistryAus() {
    return getRegistryPlugin().getAllAus();
  }

  Object titleMonitor = new Object();

  /** Return all the known titles from the title db, sorted by title */
  public List findAllTitles() {
    synchronized (titleMonitor) {
      if (allTitles == null) {
	allTitles = new ArrayList(getTitleMap().keySet());
	Collections.sort(allTitles, CatalogueOrderComparator.SINGLETON);
      }
      return allTitles;
    }
  }

  /** Find all the plugins that support the given title */
  public Collection getTitlePlugins(String title) {
    Collection res = (Collection)getTitleMap().get(title);
    if (res != null) {
      return res;
    }
    return Collections.EMPTY_LIST;
  }

  /** Return all known TitleConfigs */
  public List findAllTitleConfigs() {
    synchronized (titleMonitor) {
      if (allTitleConfigs == null) {
	List titles = findAllTitles();
	List res = new ArrayList(titles.size());
	for (Iterator titer = titles.iterator(); titer.hasNext();) {
	  String title = (String)titer.next();
	  for (Iterator piter = getTitlePlugins(title).iterator();
	       piter.hasNext();) {
	    Plugin plugin = (Plugin)piter.next();
	    TitleConfig tc = plugin.getTitleConfig(title);
	    if (tc != null) {
	      res.add(tc);
	    } else {
	      log.warning("getTitleConfig(" + plugin + ", " + title + ") = null");
	    }
	  }
	}
	allTitleConfigs = res;
      }
      return allTitleConfigs;
    }
  }

  public void resetTitles() {
    synchronized (titleMonitor) {
      titleMap = null;
      allTitles = null;
      allTitleConfigs = null;
    }
  }

  public Map getTitleMap() {
    synchronized (titleMonitor) {
      if (titleMap == null) {
	titleMap = buildTitleMap();
      }
      return titleMap;
    }
  }

  Map buildTitleMap() {
    Map map = new MultiValueMap();
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

  /** @return loadable PluginInfo for plugin, or null */
  PluginInfo getLoadablePluginInfo(Plugin plugin) {
    return (PluginInfo)pluginfoMap.get(getPluginKey(plugin));
  }

  /**
   * Load all plugin registry plugins.
   */
  void initLoadablePluginRegistries(List urls) {
    // Load the keystore if necessary
    initKeystore(configMgr.getCurrentConfig());

    if (urls.isEmpty()) {
      return;
    }

    BinarySemaphore bs = new BinarySemaphore();

    InitialRegistryCallback regCallback =
      new InitialRegistryCallback(urls, bs);

    List loadAus = new ArrayList();

    for (Iterator iter = urls.iterator(); iter.hasNext(); ) {
      String url = (String)iter.next();
      Configuration auConf = ConfigManager.newConfiguration();
      auConf.put(ConfigParamDescr.BASE_URL.getKey(), url);
      String auId = generateAuId(getRegistryPlugin(), auConf);
      String auKey = auKeyFromAuId(auId);

      // Only process this registry if it is new.
      if (!auMap.containsKey(auId)) {

	try {
	  configureAu(getRegistryPlugin(), auConf, auId);
	} catch (ArchivalUnit.ConfigurationException ex) {
	  log.error("Failed to configure AU " + auKey, ex);
	  regCallback.crawlCompleted(url);
	  continue;
	}

	ArchivalUnit registryAu = getAuFromId(auId);

	loadAus.add(registryAu);

	// Trigger a new content crawl if required.
	possiblyStartRegistryAuCrawl(registryAu, url, regCallback);
      } else {
	log.debug2("We already have this AU configured, notifying callback.");
	regCallback.crawlCompleted(url);
      }
    }

    // Wait for a while for the AU crawls to complete, then process all the
    // registries in the load list.
    log.debug("Waiting for loadable plugins to finish loading...");
    try {
      if (!bs.take(Deadline.in(registryTimeout))) {
	log.warning("Timed out while waiting for registries to finish loading. " +
		    "Remaining registry URL list: " + regCallback.getRegistryUrls());
      }
    } catch (InterruptedException ex) {
      log.warning("Binary semaphore threw InterruptedException while waiting." +
		  "Remaining registry URL list: " + regCallback.getRegistryUrls());
    }

    processRegistryAus(loadAus);
  }

  private synchronized RegistryPlugin getRegistryPlugin() {
    if (regPlugin == null) {
      regPlugin = new RegistryPlugin();
      String pluginKey = pluginKeyFromName("org.lockss.plugin.RegistryPlugin");
      regPlugin.initPlugin(getDaemon());
      setPlugin(pluginKey, regPlugin);
    }
    return regPlugin;
  }

  // Trigger a new content crawl on the registry AU if required.
  protected void possiblyStartRegistryAuCrawl(ArchivalUnit registryAu,
					      String url,
					      InitialRegistryCallback cb) {
    if (registryAu.shouldCrawlForNewContent(AuUtil.getAuState(registryAu))) {
      if (log.isDebug2()) log.debug2("Starting new crawl:: " + registryAu);
      getDaemon().getCrawlManager().startNewContentCrawl(registryAu, cb,
							 url, null);
    } else {
      if (log.isDebug2()) log.debug2("No crawl needed: " + registryAu);

      // If we're not going to crawl this AU, let the callback know.
      cb.crawlCompleted(url);
    }
  }

  // Synch the plugin registry with the plugins listed in names
  void initPluginRegistry(Configuration config) {
    List nameList = config.getList(PARAM_PLUGIN_REGISTRY);
    Collection newKeys = new HashSet();
    for (Iterator iter = nameList.iterator(); iter.hasNext(); ) {
      String name = (String)iter.next();
      String key = pluginKeyFromName(name);
      ensurePluginLoaded(key);
      newKeys.add(key);
    }

    // remove plugins on retract list, unless they have one or more
    // configured AUs
    synchronized (pluginMap) {
      if (retract != null) {
	for (Iterator iter = retract.iterator(); iter.hasNext(); ) {
	  String name = (String)iter.next();
	  String key = pluginKeyFromName(name);
	  Plugin plug = getPlugin(key);
	  if (plug != null && !isInternalPlugin(plug)) {
	    Configuration tree = currentAllPlugs.getConfigTree(key);
	    if (tree == null || tree.isEmpty()) {
	      removePlugin(key);
	    }
	  }
	}
      }
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
      ConfigManager.getCurrentConfig().getList(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST);
    String relPluginPath =
      ConfigManager.getCurrentConfig().get(PARAM_PLUGIN_LOCATION,
					   DEFAULT_PLUGIN_LOCATION);

    if (dSpaceList == null || dSpaceList.size() == 0) {
      log.error(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST +
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
   * Initialize and return the keystore.
   */
  KeyStore initKeystore(String keystoreLoc, String keystorePass) {
    KeyStore ks = null;
    try {
      if (keystoreLoc == null || keystorePass == null) {
	log.error("Unable to load keystore!  Loadable plugins will " +
		  "not be available.");
      } else {
	log.debug("Loading keystore: " + keystoreLoc);
        ks = KeyStore.getInstance("JKS", "SUN");
	if (keystoreLoc.startsWith(File.separator)) {
	  InputStream kin = new FileInputStream(new File(keystoreLoc));
	  try {
	    ks.load(kin, keystorePass.toCharArray());
	  } finally {
	    IOUtil.safeClose(kin);
	  }
	} else if (UrlUtil.isHttpUrl(keystoreLoc) ||
                   UrlUtil.isFileUrl(keystoreLoc)) {
	  URL keystoreUrl = new URL(keystoreLoc);
          ks.load(keystoreUrl.openStream(), keystorePass.toCharArray());
        } else {
	  InputStream kin =
	    getClass().getClassLoader().getResourceAsStream(keystoreLoc);
	  if (kin == null) {
	    throw new IOException("Keystore reousrce not found: " +
				  keystoreLoc);
	  }
	  ks.load(kin, keystorePass.toCharArray());
	}
      }

    } catch (Exception ex) {
      // ensure the keystore is null.
      log.error("Unable to load keystore from " + keystoreLoc, ex);
      return null;
    }

    log.debug("Keystore successfully initialized.");
    return ks;
  }

  private boolean isKeystoreInited() {
    return keystoreInited;
  }

  private void initKeystore(Configuration config) {
    String keystoreLoc;
    String keystorePass;

    if (!isKeystoreInited()) {
      keystoreLoc = config.get(PARAM_USER_KEYSTORE_LOCATION);
      if (!StringUtil.isNullString(keystoreLoc)) {
	keystorePass = config.get(PARAM_USER_KEYSTORE_PASSWORD,
				  DEFAULT_KEYSTORE_PASSWORD);
      } else {
	keystoreLoc = config.get(PARAM_KEYSTORE_LOCATION,
				 DEFAULT_KEYSTORE_LOCATION);
	keystorePass = config.get(PARAM_KEYSTORE_PASSWORD,
				  DEFAULT_KEYSTORE_PASSWORD);
      }
      keystore = initKeystore(keystoreLoc, keystorePass);
      if (keystore != null) {
        keystoreInited = true;
      }
    }
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
    jarValidator.allowExpired(acceptExpiredCertificates);

    // Create temporary plugin and classloader maps
    HashMap tmpMap = new HashMap();

    for (Iterator iter = registryAus.iterator(); iter.hasNext(); ) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      try {
	processOneRegistryAu(au, tmpMap);
      } catch (RuntimeException e) {
	log.error("Error processing plugin registry AU: " + au, e);
      }
    }

    // After the temporary plugin map has been built, install it into
    // the global maps.
    List classloaders = new ArrayList();
    for (Iterator pluginIter = tmpMap.entrySet().iterator();
	 pluginIter.hasNext(); ) {
      Map.Entry entry = (Map.Entry)pluginIter.next();
      String key = (String)entry.getKey();
      log.debug2("Adding to plugin map: " + key);
      PluginInfo info = (PluginInfo)entry.getValue();
      pluginfoMap.put(key, info);
      classloaders.add(info.getClassLoader());
      setPlugin(key, info.getPlugin());
    }

    // Add the JAR's bundled titledb config (if any) to the ConfigManager.
    // Do this once at the end so as not to trigger more than one config
    // update & reload.
    configMgr.addTitleDbConfigFrom(classloaders);

    // Cleanup as a hint to GC.
    tmpMap.clear();
    tmpMap = null;
  }

  protected void processOneRegistryAu(ArchivalUnit au, Map tmpMap) {
    CachedUrlSet cus = au.getAuCachedUrlSet();

    for (Iterator cusIter = cus.contentHashIterator(); cusIter.hasNext(); ) {
      CachedUrlSetNode cusn = (CachedUrlSetNode)cusIter.next();

      // TODO: Eventually this should be replaced with
      // "cusn.hasContent()", which will add another loop if it is a
      // CachedUrlSet.

      String url = cusn.getUrl();
	if (StringUtil.endsWithIgnoreCase(url, ".jar") &&
	    cusn.isLeaf()) {

	// This CachedUrl represents a plugin JAR, validate it and
	// process the plugins it contains.

	CachedUrl cu = (CachedUrl)cusn;
	try {
	  processOneRegistryJar(cu, url, au, tmpMap);
	} catch (RuntimeException e) {
	  log.error("Error processing plugin jar: " + cu, e);
	}
      }
    }
  }

  protected void processOneRegistryJar(CachedUrl cu, String url,
				       ArchivalUnit au, Map tmpMap) {
    Integer curVersion = new Integer(cu.getVersion());

    if (cuNodeVersionMap.get(url) == null) {
      cuNodeVersionMap.put(url, new Integer(-1));
    }

    // If we've already visited this CU, skip it unless the current
    // repository node is a different version (older OR newer)
    Integer oldVersion = (Integer)cuNodeVersionMap.get(url);

    if (oldVersion.equals(curVersion)) {
      log.debug2(url + ": JAR repository and map versions are identical.  Skipping...");
      return;
    }

    File blessedJar = null;
    if (cu.getContentSize() == 0) {
      log.debug("Empty plugin jar: " + cu);
      return;
    } else {
      try {
	// Validate and bless the JAR file from the CU.
	blessedJar = jarValidator.getBlessedJar(cu);
	log.debug2("Plugin jar: " + cu.getUrl() + " -> " + blessedJar);
      } catch (IOException ex) {
	log.error("Error processing jar file: " + url, ex);
	return;
      } catch (JarValidator.JarValidationException ex) {
	log.error("CachedUrl did not validate: " + cu, ex);
	return;
      }
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
	return; // skip this CU.

      }
      log.debug2("Blessed jar: " + blessedJar + ", plugins: " + loadPlugins);

      // Although this -should- never happen, it's possible.
      if (loadPlugins.size() == 0) {
	log.warning("Jar " + blessedJar +
		    " does not contain any plugins.  Skipping...");
	return; // skip this CU.
      }

      // Load the plugin classes
      ClassLoader pluginLoader = null;
      URL blessedUrl;
      try {
	blessedUrl = blessedJar.toURL();
	URL[] urls = new URL[] { blessedUrl };
	pluginLoader =
	  preferLoadablePlugin
	  ? new LoadablePluginClassLoader(urls)
	  : new URLClassLoader(urls);
      } catch (MalformedURLException ex) {
	log.error("Malformed URL exception attempting to create " +
		  "classloader for plugin JAR " + blessedJar);
	return; // skip this CU.
      }

      String pluginName = null;

      for (Iterator pluginIter = loadPlugins.iterator();
	   pluginIter.hasNext();) {
	pluginName = (String)pluginIter.next();
	String key = pluginKeyFromName(pluginName);

	Plugin plugin;
	PluginInfo info;
	try {
	  info = retrievePlugin(pluginName, pluginLoader);
	  info.setCuUrl(url);
	  info.setRegistryAu(au);
	  String jar = info.getJarUrl();
	  if (jar != null) {
	    // If the blessed jar path is a substring of the jar:
	    // url from which the actual plugin resource or class
	    // was loaded, then it is a loadable plugin.
	    boolean isLoadable =
	      jar.indexOf(blessedUrl.getFile()) > 0;
	    info.setIsOnLoadablePath(isLoadable);
	  }
	  plugin = info.getPlugin();
	} catch (Exception ex) {
	  log.error("Unable to load plugin " + pluginName +
		    ", skipping: " + ex.getMessage());
	  return;
	}

	PluginVersion version = null;

	try {
	  version = new PluginVersion(plugin.getVersion());
	  info.setVersion(version);
	} catch (IllegalArgumentException ex) {
	  // Don't let this runtime exception stop the daemon.  Skip the plugin.
	  log.error("Skipping plugin " + pluginName + ": " + ex.getMessage());
	  return;
	}

	if (pluginMap.containsKey(key)) {
	  // Plugin already exists in the global plugin map.
	  // Replace it with a new version if one is available.
	  log.debug2("Plugin " + key + " is already in global pluginMap.");
	  Plugin otherPlugin = getPlugin(key);
	  PluginVersion otherVer =
	    new PluginVersion(otherPlugin.getVersion());
	  if (version.toLong() > otherVer.toLong()) {
	    if (log.isDebug2()) {
	      log.debug2("Existing plugin " + plugin.getPluginId() +
			 ": Newer version " + version + " found.");
	    }
	    tmpMap.put(key, info);
	  } else {
	    if (log.isDebug2()) {
	      log.debug2("Existing plugin " + plugin.getPluginId() +
			 ": No newer version found.");
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

  /**
   * CrawlManager callback that is responsible for handling Registry
   * AUs when they're finished with their initial crawls.
   */
  static class InitialRegistryCallback implements CrawlManager.Callback {
    private BinarySemaphore bs;

    List registryUrls;

    /*
     * Set the initial size of the list of registry URLs to process.
     */
    public InitialRegistryCallback(List registryUrls, BinarySemaphore bs) {
      this.registryUrls =
	Collections.synchronizedList(new ArrayList(registryUrls));
      this.bs = bs;
      if (log.isDebug2()) log.debug2("InitialRegistryCallback: " +
				     registryUrls);
      if (registryUrls.isEmpty()) {
	bs.give();
      }
    }

    public void signalCrawlAttemptCompleted(boolean success,
					    Object cookie,
					    CrawlerStatus status) {
      String url = (String)cookie;

      crawlCompleted(url);
    }

    public void crawlCompleted(String url) {
      // Remove urls from registryUrls as they finish crawling (or it is
      // determine that they don't need to be crawled).  When registryUrls
      // is empty signal the waiting process that it may proceed to load
      // plugins we're done running crawls on all the plugin registries,
      // and we can load the plugin classes.
      registryUrls.remove(url);
      if (log.isDebug2()) log.debug2("Registry crawl complete: " + url +
				     ", " + registryUrls.size() + " left");
      if (registryUrls.isEmpty()) {
	if (log.isDebug2()) log.debug2("Registry crawls complete");
	bs.give();
      }
    }

    /**
     * Used only in the case that our semaphore throws an Interrupted
     * exception -- we can print this list to see what was left.
     */
    public List getRegistryUrls() {
      return registryUrls;
    }
  }

  /**
   * CrawlManager callback that causes a check for new plugins.  Meant to
   * be used for asynchronous registry crawls
   */
  public static class RegistryCallback implements CrawlManager.Callback {
    private PluginManager pluginMgr;
    private ArchivalUnit registryAu;

    public RegistryCallback(PluginManager pluginMgr, ArchivalUnit au) {
      this.pluginMgr = pluginMgr;
      this.registryAu = au;
    }

    public void signalCrawlAttemptCompleted(boolean success,
					    Object cookie,
					    CrawlerStatus status) {
      if (success) {
	log.debug2("Registry crawl completed successfully, checking for new plugins");
	pluginMgr.processRegistryAus(ListUtil.list(registryAu));
      }
    }
  }

  /**
   * A simple class that wraps information about a loadable plugin,
   * used during the loading process.
   */
  static class PluginInfo {
    private Plugin plugin;
    private ArchivalUnit registryAu;
    private PluginVersion version;
    private ClassLoader classLoader;
    private String cuUrl;
    private String jarUrl;
    private boolean isOnLoadablePath = false;

    public PluginInfo(Plugin plugin, ClassLoader classLoader, String jarUrl) {
      this.plugin = plugin;
      this.classLoader = classLoader;
      this.jarUrl = jarUrl;
    }

    public Plugin getPlugin() {
      return plugin;
    }

    public PluginVersion getVersion() {
      return version;
    }

    public void setVersion(PluginVersion version) {
      this.version = version;
    }

    public ClassLoader getClassLoader() {
      return classLoader;
    }

    public String getCuUrl() {
      return cuUrl;
    }

    public void setCuUrl(String cuUrl) {
      this.cuUrl = cuUrl;
    }

    public ArchivalUnit getRegistryAu() {
      return registryAu;
    }

    public void setRegistryAu(ArchivalUnit registryAu) {
      this.registryAu = registryAu;
    }

    public String getJarUrl() {
      return jarUrl;
    }

    public boolean isOnLoadablePath() {
      return isOnLoadablePath;
    }

    public void setIsOnLoadablePath(boolean val) {
      this.isOnLoadablePath = val;
    }
  }
}
