/*
 * $Id: ConfigManager.java,v 1.43 2006-11-22 00:49:26 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import java.io.*;
import java.util.*;
import java.net.URL;

import org.lockss.app.*;
import org.lockss.clockss.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.mail.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.proxy.*;
import org.lockss.repository.*;
import org.lockss.servlet.*;
import org.lockss.state.*;
import org.lockss.util.*;

/** ConfigManager loads and periodically reloads the LOCKSS configuration
 * parameters, and provides services for updating locally changeable
 * configuration.
 */
public class ConfigManager implements LockssManager {
  /** The common prefix string of all LOCKSS configuration parameters. */
  public static final String PREFIX = Configuration.PREFIX;

  static final String MYPREFIX = PREFIX + "config.";
  static final String PARAM_RELOAD_INTERVAL = MYPREFIX + "reloadInterval";
  static final long DEFAULT_RELOAD_INTERVAL = 30 * Constants.MINUTE;

  static final String WDOG_PARAM_CONFIG = "Config";
  static final long WDOG_DEFAULT_CONFIG = 2 * Constants.HOUR;

  public static final String PARAM_CONFIG_PATH = MYPREFIX + "configFilePath";
  public static final String DEFAULT_CONFIG_PATH = "config";

  /** Config param written to local config files to indicate file version */
  static final String PARAM_CONFIG_FILE_VERSION =
    MYPREFIX + "fileVersion.<filename>";

  /** Temporary param to enable new scheduler */
  public static final String PARAM_NEW_SCHEDULER =
    HashService.PREFIX + "use.scheduler";
  static final boolean DEFAULT_NEW_SCHEDULER = true;

  public static final String PARAM_TITLE_DB = Configuration.PREFIX + "title";

  /** Common prefix of platform config params */
  public static final String PLATFORM = Configuration.PLATFORM;
  public static final String DAEMON = Configuration.DAEMON;

  /** Tmp dir appropriate for platform.  If set, replaces java.io.tmpdir
   * System property */
  public static final String PARAM_TMPDIR = PLATFORM + "tmpDir";

  /** Daemon version string (i.e., 1.4.3, 1.5.0-test). */
  public static final String PARAM_DAEMON_VERSION = DAEMON + "version";
  /** Platform version string as a 36-bit integer (i.e., 135a, 136, 137-test). */
  public static final String PARAM_PLATFORM_VERSION = PLATFORM + "version";
  /** Platform host name (fqdn). */
  public static final String PARAM_PLATFORM_FQDN = PLATFORM + "fqdn";

  /** Project name (CLOCKSS or LOCKSS) */
  public static final String PARAM_PLATFORM_PROJECT = PLATFORM + "project";
  public static final String DEFAULT_PLATFORM_PROJECT = "lockss";

  /** Group name, for group= config file conditional */
  public static final String PARAM_DAEMON_GROUP = DAEMON + "group";
  public static final String DEFAULT_DAEMON_GROUP = "nogroup";

  /** Local (routable) IP address, for lcap identity */
  public static final String PARAM_PLATFORM_IP_ADDRESS =
    PLATFORM + "localIPAddress";

  /** Second IP address, for CLOCKSS subscription detection */
  public static final String PARAM_PLATFORM_SECOND_IP_ADDRESS =
    PLATFORM + "secondIP";

  /** V3 identity string */
  public static final String PARAM_PLATFORM_LOCAL_V3_IDENTITY =
    PLATFORM + "v3.identity";

  /** Local subnet set during config */
  public static final String PARAM_PLATFORM_ACCESS_SUBNET =
    PLATFORM + "accesssubnet";

  public static final String PARAM_PLATFORM_DISK_SPACE_LIST =
    PLATFORM + "diskSpacePaths";

  public static final String PARAM_PLATFORM_ADMIN_EMAIL =
    PLATFORM + "sysadminemail";
  static final String PARAM_PLATFORM_LOG_DIR = PLATFORM + "logdirectory";
  static final String PARAM_PLATFORM_LOG_FILE = PLATFORM + "logfile";

  static final String PARAM_PLATFORM_SMTP_HOST = PLATFORM + "smtphost";
  static final String PARAM_PLATFORM_SMTP_PORT = PLATFORM + "smtpport";
  static final String PARAM_PLATFORM_PIDFILE = PLATFORM + "pidfile";

  public static final String CONFIG_FILE_UI_IP_ACCESS = "ui_ip_access.txt";
  public static final String CONFIG_FILE_PROXY_IP_ACCESS = "proxy_ip_access.txt";
  public static final String CONFIG_FILE_AU_CONFIG = "au.txt";
  public static final String CONFIG_FILE_BUNDLED_TITLE_DB = "titledb.xml";
  public static final String CONFIG_FILE_ICP_SERVER = "icp_server_config.txt"; // in use
  public static final String CONFIG_FILE_AUDIT_PROXY = "audit_proxy_config.txt"; // in use
  public static final String CONFIG_FILE_ACCESS_GROUPS = "access_groups_config.txt"; // not yet in use

  /** array of local cache config file names */
  static String cacheConfigFiles[] = {
    CONFIG_FILE_UI_IP_ACCESS,
    CONFIG_FILE_PROXY_IP_ACCESS,
    CONFIG_FILE_AU_CONFIG,
    CONFIG_FILE_ICP_SERVER, // in use
    CONFIG_FILE_AUDIT_PROXY, // in use
    CONFIG_FILE_ACCESS_GROUPS, // not yet in use
  };

  // MUST pass in explicit log level to avoid recursive call back to
  // Configuration to get Config log level.  (Others should NOT do this.)
  protected static Logger log =
    Logger.getLoggerWithInitialLevel("Config",
				     Logger.getInitialDefaultLevel());

  /** A constant empty Configuration object */
  public static final Configuration EMPTY_CONFIGURATION = newConfiguration();
  static {
    EMPTY_CONFIGURATION.seal();
  }

  protected LockssApp theApp = null;

  private List configChangedCallbacks = new ArrayList();

  private List cacheConfigFileList = null;
  private List configUrlList;	// list of config file urls
  // XXX needs synchronization
  private List titledbUrlList;  // list of titledb JAR urls
  private String groupName;     // daemon group name

  private String recentLoadError;

  // Platform config
  private static Configuration platformConfig =
    ConfigManager.EMPTY_CONFIGURATION;

  // Current configuration instance.
  // Start with an empty one to avoid errors in the static accessors.
  private Configuration currentConfig = EMPTY_CONFIGURATION;

  private OneShotSemaphore haveConfig = new OneShotSemaphore();

  private HandlerThread handlerThread; // reload handler thread

  private ConfigCache configCache = new ConfigCache();

  private long reloadInterval = 10 * Constants.MINUTE;

  public ConfigManager() {
    this(null, null);
  }

  public ConfigManager(List urls) {
    this(urls, null);
  }

  public ConfigManager(List urls, String groupName) {
    if (urls != null) {
      configUrlList = new ArrayList(urls);
    }
    this.titledbUrlList = new ArrayList();
    this.groupName = groupName;
    registerConfigurationCallback(Logger.getConfigCallback());
    registerConfigurationCallback(MiscConfig.getConfigCallback());
  }

  public ConfigCache getConfigCache() {
    return configCache;
  }

  public void initService(LockssApp app) throws LockssAppException {
    theApp = app;
  }

  /** Called to start each service in turn, after all services have been
   * initialized.  Service should extend this to perform any startup
   * necessary. */
  public void startService() {
    startHandler();
  }

  /** Reset to unconfigured state.  See LockssTestCase.tearDown(), where
   * this is called.)
   */
  public void stopService() {
    currentConfig = newConfiguration();
    // this currently runs afoul of Logger, which registers itself once
    // only, on first use.
    configChangedCallbacks = new ArrayList();
    configUrlList = null;
    cacheConfigInited = false;
    cacheConfigDir = null;
    // Reset the config cache.
    configCache = new ConfigCache();
    stopHandler();
    haveConfig = new OneShotSemaphore();
  }

  public LockssApp getApp() {
    return theApp;
  }
  private static ConfigManager theMgr;

  public static ConfigManager makeConfigManager() {
    theMgr = new ConfigManager();
    return theMgr;
  }

  public static ConfigManager makeConfigManager(List urls) {
    theMgr = new ConfigManager(urls);
    return theMgr;
  }

  public static ConfigManager makeConfigManager(List urls, String groupName) {
    theMgr = new ConfigManager(urls, groupName);
    return theMgr;
  }

  public static ConfigManager getConfigManager() {
    return theMgr;
  }

  /** Factory to create instance of appropriate class */
  public static Configuration newConfiguration() {
    return new ConfigurationPropTreeImpl();
  }

  /** Return current configuration */
  public static Configuration getCurrentConfig() {
    if (theMgr == null || theMgr.currentConfig == null) {
      return EMPTY_CONFIGURATION;
    }
    return theMgr.currentConfig;
  }

  void setCurrentConfig(Configuration newConfig) {
    if (newConfig == null) {
      log.warning("attempt to install null Configuration");
    }
    currentConfig = newConfig;
  }

  /** Create a sealed Configuration object from a Properties */
  public static Configuration fromProperties(Properties props) {
    Configuration config = fromPropertiesUnsealed(props);
    config.seal();
    return config;
  }

  /** Create an unsealed Configuration object from a Properties */
  public static Configuration fromPropertiesUnsealed(Properties props) {
    Configuration config = new ConfigurationPropTreeImpl();
    for (Iterator iter = props.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      config.put(key, props.getProperty(key));
    }
    return config;
  }

  /**
   * Convenience methods for getting useful platform settings.
   */
  public static DaemonVersion getDaemonVersion() {
    DaemonVersion daemon = null;

    String ver = BuildInfo.getBuildProperty(BuildInfo.BUILD_RELEASENAME);
    // If BuildInfo doesn't give us a value, see if we already have it
    // in the props.  Useful for testing.
    if (ver == null) {
      ver = getCurrentConfig().get(PARAM_DAEMON_VERSION);
    }
    return ver == null ? null : new DaemonVersion(ver);
  }

  public static Configuration getPlatformConfig() {
    Configuration res = getCurrentConfig();
    if (res.isEmpty()) {
      res = platformConfig;
    }
    return res;
  }

  private static PlatformVersion platVer = null;

  public static PlatformVersion getPlatformVersion() {
    if (platVer == null) {
      String ver = getPlatformConfig().get(PARAM_PLATFORM_VERSION);
      if (ver != null) {
	try {
	  platVer = new PlatformVersion(ver);
	} catch (RuntimeException e) {
	}
      }
    }
    return platVer;
  }

  public static String getPlatformGroup() {
    return getPlatformConfig().get(PARAM_DAEMON_GROUP, DEFAULT_DAEMON_GROUP);
  }

  public static String getPlatformHostname() {
    return getPlatformConfig().get(PARAM_PLATFORM_FQDN);
  }

  public static String getPlatformProject() {
    return getPlatformConfig().get(PARAM_PLATFORM_PROJECT,
				   DEFAULT_PLATFORM_PROJECT);
  }

  /** Wait until the system is configured.  (<i>Ie</i>, until the first
   * time a configuration has been loaded.)
   * @param timer limits the time to wait.  If null, returns immediately.
   * @return true if configured, false if timer expired.
   */
  public boolean waitConfig(Deadline timer) {
    while (!haveConfig.isFull() && !timer.expired()) {
      try {
	haveConfig.waitFull(timer);
      } catch (InterruptedException e) {
	// no action - check timer
      }
    }
    return haveConfig.isFull();
  }

  /** Wait until the system is configured.  (<i>Ie</i>, until the first
   * time a configuration has been loaded.) */
  public boolean waitConfig() {
    return waitConfig(Deadline.MAX);
  }

  void runCallback(Configuration.Callback cb,
		   Configuration newConfig,
		   Configuration oldConfig,
		   Configuration.Differences diffs) {
    try {
      cb.configurationChanged(newConfig, oldConfig, diffs);
    } catch (Exception e) {
      log.error("callback threw", e);
    }
  }

  void runCallbacks(Configuration newConfig,
		    Configuration oldConfig,
		    Configuration.Differences diffs) {
    // run our own "callback"
    configurationChanged(newConfig, oldConfig, diffs);
    // copy the list of callbacks as it could change during the loop.
    List cblist = new ArrayList(configChangedCallbacks);
    for (Iterator iter = cblist.iterator(); iter.hasNext();) {
      try {
	Configuration.Callback cb = (Configuration.Callback)iter.next();
	runCallback(cb, newConfig, oldConfig, diffs);
      } catch (RuntimeException e) {
	throw e;
      }
    }
  }

  public Configuration readConfig(List urlList) throws IOException {
    return readConfig(urlList, null);
  }

  /**
   * Return a new <code>Configuration</code> instance loaded from the
   * url list
   */
  public Configuration readConfig(List urlList, String groupName)
      throws IOException {
    if (urlList == null) {
      return null;
    }

    Configuration newConfig = newConfiguration();

    // Add platform-like params before calling loadList() as they affect
    // conditional processing
    if (groupName != null) {
      newConfig.put(PARAM_DAEMON_GROUP, groupName);
    }
    loadList(newConfig, getConfigGenerations(urlList, true, "props"));
    return newConfig;
  }

  String getLoadErrorMessage(ConfigFile cf) {
    if (cf != null) {
      StringBuffer sb = new StringBuffer();
      sb.append("Error loading: ");
      sb.append(cf.getFileUrl());
      sb.append("<br>");
      sb.append(cf.getLoadErrorMessage());
      sb.append("<br>Last attempt: ");
      sb.append(new Date(cf.getLastAttemptTime()));
      return sb.toString();
    } else {
      return "Error loading unknown file: shouldn't happen";
    }
  }

  private Map generationMap = new HashMap();

  int getGeneration(String url) {
    Integer gen = (Integer)generationMap.get(url);
    if (gen == null) return -1;
    return gen.intValue();
  }

  void setGeneration(String url, int gen) {
    generationMap.put(url, new Integer(gen));
  }

  ConfigFile.Generation getConfigGeneration(String url, boolean required,
					    String msg)
      throws IOException {
    log.debug2("Loading " + msg + " from: " + url);
    return getConfigGeneration(configCache.find(url), required, msg);
  }

  ConfigFile.Generation getConfigGeneration(ConfigFile cf, boolean required,
					    String msg)
      throws IOException {
    try {
      cf.setNeedsReload();
      return cf.getGeneration();
    } catch (IOException e) {
      String url = cf.getFileUrl();
      if (e instanceof FileNotFoundException &&
	  StringUtil.endsWithIgnoreCase(url, ".opt")) {
	log.debug2("Not loading props from nonexistent optional file: " + url);
	return null;
      } else if (required) {
	// This load failed.  Fail the whole thing.
	log.warning("Couldn't load props from " + url, e);
	recentLoadError = getLoadErrorMessage(cf);
	throw e;
      } else {
	if (e instanceof FileNotFoundException) {
	  log.debug3("Non-required file not found " + url);
	} else {
	  log.debug3("Unexpected error loaded non-required file " + url, e);
	}
	return null;
      }
    }
  }

  boolean isChanged(ConfigFile.Generation gen) {
    boolean val = (gen.getGeneration() != getGeneration(gen.getUrl()));
    return (gen.getGeneration() != getGeneration(gen.getUrl()));
  }

  boolean isChanged(Collection gens) {
    for (Iterator iter = gens.iterator(); iter.hasNext(); ) {
      ConfigFile.Generation gen = (ConfigFile.Generation)iter.next();
      if (gen != null && isChanged(gen)) {
	return true;
      }
    }
    return false;
  }

  List getConfigGenerations(Collection urls, boolean required, String msg)
      throws IOException {
    if (urls == null) return Collections.EMPTY_LIST;
    List res = new ArrayList(urls.size());
    for (Iterator iter = urls.iterator(); iter.hasNext(); ) {
      Object o = iter.next();
      ConfigFile.Generation gen;
      if (o instanceof ConfigFile) {
	gen = getConfigGeneration((ConfigFile)o, required, msg);
      } else {
	String url = o.toString();
	gen = getConfigGeneration(url, required, msg);
      }
      if (gen != null) {
	res.add(gen);
      }
    }
    return res;
  }

  List getStandardConfigGenerations(List urls) throws IOException {
    List res = new ArrayList(20);
    res.addAll(getConfigGenerations(titledbUrlList, false, "bundled titledb"));
    List configGens = getConfigGenerations(urls, true, "props");
    res.addAll(configGens);
    initCacheConfig(configGens);
    res.addAll(getCacheConfigGenerations());
    return res;
  }

  List getCacheConfigGenerations() throws IOException {
    List localGens = getConfigGenerations(getCacheConfigFiles(), false,
					  "cache config");
    if (!localGens.isEmpty()) {
      hasLocalCacheConfig = true;
    }
    return localGens;
  }

  boolean updateConfig() {
    return updateConfig(configUrlList);
  }

  public boolean updateConfig(List urls) {
    if (currentConfig.isEmpty()) {
      // first load preceded by platform config setup
      setupPlatformConfig(urls);
    }
    List gens;
    try {
      gens = getStandardConfigGenerations(urls);
    } catch (IOException e) {
      log.error("Error loading config", e);
//       recentLoadError = e.toString();
      return false;
    }

    if (!isChanged(gens)) {
      if (reloadInterval >= 10 * Constants.MINUTE) {
	log.info("Config up tp date, not updated");
      }
      return false;
    }
    Configuration newConfig = newConfiguration();
    // Add platform-like params before calling loadList() as they affect
    // conditional processing
    if (groupName != null) {
      newConfig.put(PARAM_DAEMON_GROUP, groupName);
    }
    loadList(newConfig, gens);

    return installConfig(newConfig, gens);
  }

  void loadList(Configuration intoConfig, Collection gens) {
    for (Iterator iter = gens.iterator(); iter.hasNext(); ) {
      ConfigFile.Generation gen = (ConfigFile.Generation)iter.next();
      if (gen != null) {
	intoConfig.copyFrom(gen.getConfig());
      }
    }
  }

  void setupPlatformConfig(List urls) {
    Configuration platConfig = newConfiguration();
    // Add platform-like params before calling loadList() as they affect
    // conditional processing
    if (groupName != null) {
      platConfig.put(PARAM_DAEMON_GROUP, groupName);
    }
    for (Iterator iter = urls.iterator(); iter.hasNext();) {
      Object o = iter.next();
      ConfigFile cf;
      if (o instanceof ConfigFile) {
	cf = (ConfigFile)o;
      } else {
	cf = configCache.find((String)o);
      }
      if (cf.isPlatformFile()) {
	try {
	  cf.setNeedsReload();
	  platConfig.load(cf);
	} catch (IOException e) {
	  log.warning("Couldn't preload platform file " + cf.getFileUrl(), e);
	}
      }
    }
    // do this even if no local.txt, to ensure platform-like params (e.g.,
    // group) in initial config get into platformConfig even during testing.
    platConfig.seal();
    platformConfig = platConfig;
  }


  // used by testing utilities
  boolean installConfig(Configuration newConfig) {
    return installConfig(newConfig, Collections.EMPTY_LIST);
  }

  boolean installConfig(Configuration newConfig, List gens) {
    if (newConfig == null) {
      return false;
    }
    copyPlatformParams(newConfig);
    inferMiscParams(newConfig);
    newConfig.seal();
    Configuration oldConfig = currentConfig;
    if (!oldConfig.isEmpty() && newConfig.equals(oldConfig)) {
      if (reloadInterval >= 10 * Constants.MINUTE) {
	log.info("Config unchanged, not updated");
      }
      updateGenerations(gens);
      return false;
    }
    Configuration.Differences diffs = newConfig.differences(oldConfig);
    if (diffs.contains(PARAM_TITLE_DB)) {
      newConfig.setTitleConfig(newConfig.getConfigTree(PARAM_TITLE_DB));
    } else {
      newConfig.setAllTitleConfigs(oldConfig.getAllTitleConfigs());
    }
    // XXX for test utils.  ick
    initCacheConfig(newConfig);
    setCurrentConfig(newConfig);
    updateGenerations(gens);
    logConfigLoaded(newConfig, oldConfig, diffs, gens);
    runCallbacks(newConfig, oldConfig, diffs);
    haveConfig.fill();
    return true;
  }

  void updateGenerations(List gens) {
    for (Iterator iter = gens.iterator(); iter.hasNext(); ) {
      ConfigFile.Generation gen = (ConfigFile.Generation)iter.next();
      setGeneration(gen.getUrl(), gen.getGeneration());
    }
  }


  public void configurationChanged(Configuration config,
				   Configuration oldConfig,
				   Configuration.Differences changedKeys) {

    reloadInterval = config.getTimeInterval(PARAM_RELOAD_INTERVAL,
					    DEFAULT_RELOAD_INTERVAL);
    if (changedKeys.contains(PARAM_PLATFORM_VERSION)) {
      platVer = null;
    }
  }

  private void logConfigLoaded(Configuration newConfig,
			       Configuration oldConfig,
			       Configuration.Differences diffs,
			       List gens) {
    StringBuffer sb = new StringBuffer("Config updated, ");
    sb.append(newConfig.keySet().size());
    sb.append(" keys");
    if (gens != null && !gens.isEmpty()) {
      List names = new ArrayList(gens.size());
      for (Iterator iter = gens.iterator(); iter.hasNext(); ) {
	ConfigFile.Generation gen = (ConfigFile.Generation)iter.next();
	if (gen != null) {
	  names.add(gen.getUrl());
	}
      }
      sb.append(" from ");
      sb.append(StringUtil.separatedString(names, ", "));
    }
    log.info(sb.toString());
    if (log.isDebug()) {
      logConfig(newConfig, oldConfig, diffs);
    }
  }

  static final String PARAM_HASH_SVC = "org.lockss.manager.HashService";
  static final String DEFAULT_HASH_SVC = "org.lockss.hasher.HashSvcSchedImpl";

  private void inferMiscParams(Configuration config) {
    // hack to make hash use new scheduler without directly setting
    // org.lockss.manager.HashService, which would break old daemons.
    // don't set if already has a value
    if (config.get(PARAM_HASH_SVC) == null &&
	config.getBoolean(PARAM_NEW_SCHEDULER, DEFAULT_NEW_SCHEDULER)) {
      config.put(PARAM_HASH_SVC, DEFAULT_HASH_SVC);
    }

    String tmpdir = config.get(PARAM_TMPDIR);
    if (!StringUtil.isNullString(tmpdir)) {
      System.setProperty("java.io.tmpdir", tmpdir);
    }
  }

  private void copyPlatformParams(Configuration config) {
    copyPlatformVersionParams(config);

    String logdir = config.get(PARAM_PLATFORM_LOG_DIR);
    String logfile = config.get(PARAM_PLATFORM_LOG_FILE);
    if (logdir != null && logfile != null) {
      platformOverride(config, FileTarget.PARAM_FILE,
		       new File(logdir, logfile).toString());
    }

    conditionalPlatformOverride(config, PARAM_PLATFORM_IP_ADDRESS,
				IdentityManager.PARAM_LOCAL_IP);

    conditionalPlatformOverride(config, PARAM_PLATFORM_IP_ADDRESS,
				ClockssParams.PARAM_INSTITUTION_SUBSCRIPTION_ADDR);
    conditionalPlatformOverride(config, PARAM_PLATFORM_SECOND_IP_ADDRESS,
				ClockssParams.PARAM_CLOCKSS_SUBSCRIPTION_ADDR);

    conditionalPlatformOverride(config, PARAM_PLATFORM_LOCAL_V3_IDENTITY,
				IdentityManager.PARAM_LOCAL_V3_IDENTITY);

    conditionalPlatformOverride(config, PARAM_PLATFORM_SMTP_PORT,
				SmtpMailService.PARAM_SMTPPORT);
    conditionalPlatformOverride(config, PARAM_PLATFORM_SMTP_HOST,
				SmtpMailService.PARAM_SMTPHOST);

    // Add platform access subnet to access lists if it hasn't already been
    // accounted for
    String platformSubnet = config.get(PARAM_PLATFORM_ACCESS_SUBNET);
    appendPlatformAccess(config,
			 ServletManager.PARAM_IP_INCLUDE,
			 ServletManager.PARAM_IP_PLATFORM_SUBNET,
			 platformSubnet);
    appendPlatformAccess(config,
			 ProxyManager.PARAM_IP_INCLUDE,
			 ProxyManager.PARAM_IP_PLATFORM_SUBNET,
			 platformSubnet);

    String space = config.get(PARAM_PLATFORM_DISK_SPACE_LIST);
    if (!StringUtil.isNullString(space)) {
      String firstSpace =
	((String)StringUtil.breakAt(space, ';', 1).elementAt(0));
      platformOverride(config,
		       LockssRepositoryImpl.PARAM_CACHE_LOCATION,
		       firstSpace);
      platformOverride(config, HistoryRepositoryImpl.PARAM_HISTORY_LOCATION,
		       firstSpace);
      platformOverride(config, IdentityManager.PARAM_IDDB_DIR,
		       new File(firstSpace, "iddb").toString());
    }
  }

  private void copyPlatformVersionParams(Configuration config) {
    String platformVer = config.get(PARAM_PLATFORM_VERSION);
    if (platformVer == null) {
      return;
    }
    Configuration versionConfig = config.getConfigTree(platformVer);
    if (!versionConfig.isEmpty()) {
     for (Iterator iter = versionConfig.keyIterator(); iter.hasNext(); ) {
       String key = (String)iter.next();
       platformOverride(config, key, versionConfig.get(key));
     }
    }
  }

  private void platformOverride(Configuration config, String key, String val) {
    if (config.get(key) != null) {
      log.warning("Overriding param: " + key + "= " + config.get(key));
      log.warning("with platform-derived value: " + val);
    }
    config.put(key, val);
  }

  private void conditionalPlatformOverride(Configuration config,
					   String platformKey, String key) {
    String value = config.get(platformKey);
    if (value != null) {
      platformOverride(config, key, value);
    }
  }

  // If the current platform access (subnet) value is different from the
  // value it had the last time the local config file was written, add it
  // to the access list.
  private void appendPlatformAccess(Configuration config, String accessParam,
				    String oldPlatformAccessParam,
				    String platformAccess) {
    String oldPlatformAccess = config.get(oldPlatformAccessParam);
    if (StringUtil.isNullString(platformAccess) ||
	platformAccess.equals(oldPlatformAccess)) {
      return;
    }
    String includeIps = config.get(accessParam);
    includeIps = IpFilter.unionFilters(platformAccess, includeIps);
    config.put(accessParam, includeIps);
  }

  private void logConfig(Configuration config,
			 Configuration oldConfig,
			 Configuration.Differences diffs) {
    Set diffSet = diffs.getDifferenceSet();
    SortedSet keys = new TreeSet(diffSet != null ? diffSet : config.keySet());
    int numDiffs = keys.size();
    for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (numDiffs <= 40 || log.isDebug3() ||
	  (!key.startsWith(PARAM_TITLE_DB) &&
	   !key.startsWith(PluginManager.PARAM_AU_TREE))) {
	if (config.containsKey(key)) {
	  log.debug(key + " = " + config.get(key));
	} else if (oldConfig.containsKey(key)) {
	  log.debug(key + " (removed)");
	}
      }
    }
  }

  /**
   * Add a collection of bundled titledb config jar URLs to
   * the titledbUrlList.
   */
  public void addTitleDbConfigFrom(Collection classloaders) {
    boolean needReload = false;

    for (Iterator it = classloaders.iterator(); it.hasNext(); ) {
      ClassLoader cl = (ClassLoader)it.next();
      URL titleDbUrl = cl.getResource(CONFIG_FILE_BUNDLED_TITLE_DB);
      if (titleDbUrl != null) {
	titledbUrlList.add(titleDbUrl);
	needReload = true;
      }
    }
    // Force a config reload -- this is required to make the bundled
    // title configs immediately available, otherwise they will not be
    // available until the next config reload.
    if (needReload) {
      requestReload();
    }
  }

  public void requestReload() {
    if (handlerThread != null) {
      handlerThread.forceReload();
    }
  }

  /**
   * Register a {@link Configuration.Callback}, which will be called
   * whenever the current configuration has changed.  If a configuration is
   * present when a callback is registered, the callback will be called
   * immediately.
   * @param c <code>Configuration.Callback</code> to add.  */
  public void registerConfigurationCallback(Configuration.Callback c) {
    log.debug3("registering " + c);
    if (!configChangedCallbacks.contains(c)) {
      configChangedCallbacks.add(c);
      if (!currentConfig.isEmpty()) {
	runCallback(c, currentConfig, EMPTY_CONFIGURATION,
		    Configuration.DIFFERENCES_ALL);
      }
    }
  }

  /**
   * Unregister a <code>Configuration.Callback</code>.
   * @param c <code>Configuration.Callback</code> to remove.
   */
  public void unregisterConfigurationCallback(Configuration.Callback c) {
    log.debug3("unregistering " + c);
    configChangedCallbacks.remove(c);
  }

  boolean cacheConfigInited = false;
  File cacheConfigDir = null;
  boolean hasLocalCacheConfig = false;

  boolean isUnitTesting() {
    return Boolean.getBoolean("org.lockss.unitTesting");
  }

  private String getFromGenerations(List configGenerations, String param,
				    String dfault) {
    for (Iterator iter = configGenerations.iterator(); iter.hasNext(); ) {
      ConfigFile.Generation gen = (ConfigFile.Generation)iter.next();
      if (gen != null) {
	String val = gen.getConfig().get(param);
	if (val != null) {
	  return val;
	}
      }
    }
    return dfault;
  }

  private void initCacheConfig(String dspace, String relConfigPath) {
    if (cacheConfigInited) return;
    Vector v = StringUtil.breakAt(dspace, ';');
    if (!isUnitTesting() && v.size() == 0) {
      log.error(PARAM_PLATFORM_DISK_SPACE_LIST +
		" not specified, not configuring local cache config dir");
      return;
    }
    for (Iterator iter = v.iterator(); iter.hasNext(); ) {
      String path = (String)iter.next();
      File configDir = new File(path, relConfigPath);
      if (configDir.exists()) {
	cacheConfigDir = configDir;
	break;
      }
    }
    if (cacheConfigDir == null) {
      if (v.size() >= 1) {
	String path = (String)v.get(0);
	File dir = new File(path, relConfigPath);
	if (dir.mkdirs()) {
	  cacheConfigDir = dir;
	}
      }
    }
    cacheConfigInited = true;
  }

  private void initCacheConfig(List configGenerations) {
    if (cacheConfigInited) return;
    String dspace = getFromGenerations(configGenerations,
				       PARAM_PLATFORM_DISK_SPACE_LIST,
				       null);
    String relConfigPath = getFromGenerations(configGenerations,
					      PARAM_CONFIG_PATH,
					      DEFAULT_CONFIG_PATH);
    initCacheConfig(dspace, relConfigPath);
  }

  private void initCacheConfig(Configuration newConfig) {
    if (cacheConfigInited) return;
    String dspace = newConfig.get(PARAM_PLATFORM_DISK_SPACE_LIST);
    String relConfigPath = newConfig.get(PARAM_CONFIG_PATH,
					 DEFAULT_CONFIG_PATH);
    initCacheConfig(dspace, relConfigPath);
  }

  /**
   * <p>Return a directory under the platform disk space list.  Does
   * not create the directory, client code is expected to handle that.</p>
   *
   * <p>This currently only returns directories on the first available
   * platform disk.</p>
   *
   * @param relDir The relative pathname of the directory to request.
   * @return A File object representing the requested directory.
   */
  public File getPlatformDir(String relDir) {
    List dspacePaths =
      ConfigManager.getCurrentConfig().getList(PARAM_PLATFORM_DISK_SPACE_LIST);
    if (dspacePaths.size() == 0) {
      throw new RuntimeException("No platform dir available");
    }
    File dir = new File((String)dspacePaths.get(0), relDir);
    return dir;
  }

  /** Return the list of cache config file names */
  public List getCacheConfigFiles() {
    if (cacheConfigFileList == null) {
      List res = new ArrayList();
      for (int ix = 0; ix < cacheConfigFiles.length; ix++) {
	File cfile = new File(cacheConfigDir, cacheConfigFiles[ix]);
	res.add(cfile);
      }
      cacheConfigFileList = res;
    }
    return cacheConfigFileList;
  }

  /** Return a File for the named cache config file */
  public File getCacheConfigFile(String cacheConfigFileName) {
    return new File(cacheConfigDir, cacheConfigFileName);
  }

  /** Return true if any daemon config has been done on this machine */
  public boolean hasLocalCacheConfig() {
    return hasLocalCacheConfig;
  }

  /**
   * @param url The Jar URL of a bundled title db file.
   * @return Configuration with parameters from the bundled file,
   *         or an empty configuration if it could not be loaded.
   */
  public Configuration readTitledbConfigFile(URL url) {
    log.debug2("Loading bundled titledb from URL: " + url);
    ConfigFile cf = configCache.find(url.toString());
    try {
      return cf.getConfiguration();
    } catch (FileNotFoundException ex) {
      // expected if no bundled title db
    } catch (IOException ex) {
      log.debug("Unexpected exception loading bundled titledb", ex);
    }
    return EMPTY_CONFIGURATION;
  }

  /** Read the named local cache config file from the previously determined
   * cache config directory.
   * @param cacheConfigFileName filename, no path
   * @return Configuration with parameters from file
   */
  public Configuration readCacheConfigFile(String cacheConfigFileName)
      throws IOException {

    if (cacheConfigDir == null) {
      log.warning("Attempting to read cache config file: " +
		  cacheConfigFileName + ", but no cache config dir exists");
      throw new RuntimeException("No cache config dir");
    }

    String cfile = new File(cacheConfigDir, cacheConfigFileName).toString();
    ConfigFile cf = configCache.find(cfile);
    Configuration res = cf.getConfiguration();
    return res;
  }

  /**
   * Return the contents of the local AU config file.
   * @return the Configuration from the AU config file, or an empty config
   * if no config file found
   */
  public Configuration readAuConfigFile() {
    Configuration auConfig;
    try {
      auConfig = readCacheConfigFile(CONFIG_FILE_AU_CONFIG);
    } catch (IOException e) {
      log.warning("Couldn't read AU config file: " + e.getMessage());
      auConfig = newConfiguration();
    }
    return auConfig;
  }

  /** Write the named local cache config file into the previously determined
   * cache config directory.
   * @param props properties to write
   * @param cacheConfigFileName filename, no path
   * @param header file header string
   */
  public void writeCacheConfigFile(Properties props,
				   String cacheConfigFileName,
				   String header)
      throws IOException {
    writeCacheConfigFile(fromProperties(props), cacheConfigFileName, header);
  }

  /** Write the named local cache config file into the previously determined
   * cache config directory.
   * @param config Configuration with properties to write
   * @param cacheConfigFileName filename, no path
   * @param header file header string
   */
  public synchronized void writeCacheConfigFile(Configuration config,
						String cacheConfigFileName,
						String header)
      throws IOException {
    writeCacheConfigFile(config, cacheConfigFileName, header, false, false);
  }

  /** Write the named local cache config file into the previously determined
   * cache config directory.
   * @param config Configuration with properties to write
   * @param cacheConfigFileName filename, no path
   * @param header file header string
   */
  public synchronized void writeCacheConfigFile(Configuration config,
						String cacheConfigFileName,
						String header,
						boolean suppressReload,
						boolean mayAlterConfig)
      throws IOException {
    if (cacheConfigDir == null) {
      log.warning("Attempting to write cache config file: " +
		  cacheConfigFileName + ", but no cache config dir exists");
      throw new RuntimeException("No cache config dir");
    }
    File cfile = new File(cacheConfigDir, cacheConfigFileName);
    // Write to a temp file and rename
    File tempfile = File.createTempFile("tmp_config", ".tmp", cacheConfigDir);
    OutputStream os = new FileOutputStream(tempfile);

    Configuration tmpConfig;
    if (mayAlterConfig && !config.isSealed()) {
      tmpConfig = config;
    } else {
      // make a copy and add the config file version number, write the copy
      tmpConfig = config.copy();
    }
    tmpConfig.put(configVersionProp(cacheConfigFileName), "1");
    tmpConfig.seal();
    tmpConfig.store(os, header);
    os.close();
    if (!PlatformUtil.updateAtomically(tempfile, cfile)) {
      throw new RuntimeException("Couldn't rename temp file: " +
				 tempfile + " to: " + cfile);
    }
    log.debug2("Wrote cache config file: " + cfile);
    ConfigFile cf = configCache.get(cfile.toString());
    if (cf instanceof FileConfigFile) {
      ((FileConfigFile)cf).storedConfig(tmpConfig);
    } else {
      log.warning("Not a FileConfigFile: " + cf);
    }
    if (!suppressReload) {
      requestReload();
    }
  }

  /** Return the config version prop key for the named config file */
  public static String configVersionProp(String cacheConfigFileName) {
    String noExt = StringUtil.upToFinal(cacheConfigFileName, ".");
    return StringUtil.replaceString(PARAM_CONFIG_FILE_VERSION,
				    "<filename>", noExt);
  }

  /** Replace one AU's config keys in the local AU config file.
   * @param auProps new properties for AU
   * @param auPropKey the common initial part of all keys in the AU's config
   */
  public void updateAuConfigFile(Properties auProps, String auPropKey)
      throws IOException {
    updateAuConfigFile(fromProperties(auProps), auPropKey);
  }

  /** Replace one AU's config keys in the local AU config file.
   * @param auConfig new config for AU
   * @param auPropKey the common initial part of all keys in the AU's config
   */
  public void updateAuConfigFile(Configuration auConfig, String auPropKey)
      throws IOException {
    Configuration fileConfig;
    try {
      fileConfig = readCacheConfigFile(CONFIG_FILE_AU_CONFIG);
    } catch (FileNotFoundException e) {
      fileConfig = newConfiguration();
    }
    if (fileConfig.isSealed()) {
      fileConfig = fileConfig.copy();
    }
    // first remove all existing values for the AU
    if (auPropKey != null) {
      fileConfig.removeConfigTree(auPropKey);
    }
    for (Iterator iter = auConfig.keySet().iterator(); iter.hasNext();) {
      String key = (String)iter.next();
      fileConfig.put(key, auConfig.get(key));
    }
    writeCacheConfigFile(fileConfig, CONFIG_FILE_AU_CONFIG,
			 "AU Configuration", isDoingAuBatch, true);
  }

  /**
   * <p>Calls {@link #modifyCacheConfigFile(Configuration, Set, String, String)}
   * with a <code>null</code> delete set.</p>
   * @param updateConfig        A {@link Configuration} instance
   *                            containing keys that will be added or
   *                            updated in the file (see above). Can
   *                            be <code>null</code> if no keys are to
   *                            be added or updated.
   * @param cacheConfigFileName A config file name (without path).
   * @param header              A file header string.
   * @throws IOException if an I/O error occurs.
   * @see #modifyCacheConfigFile(Configuration, Set, String, String)
   */
  public synchronized void modifyCacheConfigFile(Configuration updateConfig,
                                                 String cacheConfigFileName,
                                                 String header)
      throws IOException {
    modifyCacheConfigFile(updateConfig, null, cacheConfigFileName, header);
  }

  /**
   * <p>Modifies configuration values in a cache config file.</p>
   * <table>
   *  <thead>
   *   <tr>
   *    <td>Precondition</td>
   *    <td>Postcondition</td>
   *   </tr>
   *  </thead>
   *  <tbody>
   *   <tr>
   *    <td><code>deleteConfig</code> contains key <code>k</code></td>
   *    <td>The file does not contain key <code>k</code></td>
   *   </tr>
   *   <tr>
   *    <td>
   *     <code>updateConfig</code> maps key <code>k</code> to a value
   *     <code>v</code>, and <code>deleteConfig</code> does not
   *     contain key <code>k</code>
   *    </td>
   *    <td>The file maps <code>k</code> to <code>v</code></td>
   *   </tr>
   *   <tr>
   *    <td>
   *     <code>updateConfig</code> and <code>deleteConfig</code> do
   *     not contain key <code>k</code>
   *    </td>
   *    <td>
   *     The file does not contain <code>k</code> if it did not
   *     originally contain <code>k</code>, or maps <code>k</code> to
   *     <code>w</code> if it originally mapped <code>k</code> to
   *     <code>w</code>
   *    </td>
   *   </tr>
   *  </tbody>
   * </table>
   * @param updateConfig        A {@link Configuration} instance
   *                            containing keys that will be added or
   *                            updated in the file (see above). Can
   *                            be <code>null</code> if no keys are to
   *                            be added or updated.
   * @param deleteSet        A set of keys that will be deleted
   *                            in the file (see above). Can be
   *                            <code>null</code> if no keys are to be
   *                            deleted.
   * @param cacheConfigFileName A config file name (without path).
   * @param header              A file header string.
   * @throws IOException              if an I/O error occurs.
   * @throws IllegalArgumentException if a key appears both in
   *                                  <code>updateConfig</code> and
   *                                  in <code>deleteSet</code>.
   * @see #cacheConfigFiles
   */
  public synchronized void modifyCacheConfigFile(Configuration updateConfig,
                                                 Set deleteSet,
                                                 String cacheConfigFileName,
                                                 String header)
      throws IOException {
    Configuration fileConfig;

    // Get config from file
    try {
      fileConfig = readCacheConfigFile(cacheConfigFileName);
    }
    catch (FileNotFoundException fnfeIgnore) {
      fileConfig = newConfiguration();
    }
    if (fileConfig.isSealed()) {
      fileConfig = fileConfig.copy();
    }

    // Add or update
    if (updateConfig != null && !updateConfig.isEmpty()) {
      for (Iterator iter = updateConfig.keyIterator() ; iter.hasNext() ; ) {
        String key = (String)iter.next();
        fileConfig.put(key, updateConfig.get(key));
      }
    }

    // Delete
    if (deleteSet != null && !deleteSet.isEmpty()) {
      if (updateConfig == null) {
        updateConfig = ConfigManager.newConfiguration();
      }
      for (Iterator iter = deleteSet.iterator() ; iter.hasNext() ; ) {
        String key = (String)iter.next();
        if (updateConfig.containsKey(key)) {
          throw new IllegalArgumentException("The following key appears in the update set and in the delete set: " + key);
        }
        else {
          fileConfig.remove(key);
        }
      }
    }

    // Write out file
    writeCacheConfigFile(fileConfig, cacheConfigFileName, header);
  }

  private boolean isDoingAuBatch = false;

  /** hack to allow batch AU config operations to prevent a config reload
   * from being triggered each time the AU config file is rewritten.
   * Clients who set this true <b>must</b> reset it false when they are
   * done (in a <code>finally</code>), and call requestReload() if
   * appropriate */
  public void doingAuBatch(boolean flg) {
    isDoingAuBatch = flg;
  }

  TinyUi tiny = null;
  String[] tinyData = new String[1];

  void startTinyUi() {
    TinyUi t = new TinyUi(tinyData);
    updateTinyData();
    t.startTiny();
    tiny = t;
  }

  void stopTinyUi() {
    if (tiny != null) {
      tiny.stopTiny();
      tiny = null;
      // give listener socket a little time to close
      try {
	Deadline.in(2 * Constants.SECOND).sleep();
      } catch (InterruptedException e ) {
      }
    }
  }

  void updateTinyData() {
    tinyData[0] = recentLoadError;
  }

  // Reload thread

  void startHandler() {
    if (handlerThread != null) {
      log.warning("Handler already running; stopping old one first");
      stopHandler();
    } else {
      log.info("Starting handler");
    }
    handlerThread = new HandlerThread("ConfigHandler");
    handlerThread.start();
  }

  void stopHandler() {
    if (handlerThread != null) {
      log.info("Stopping handler");
      handlerThread.stopHandler();
      handlerThread = null;
    } else {
//       log.warning("Attempt to stop handler when it isn't running");
    }
  }

  // Handler thread, periodically reloads config

  private class HandlerThread extends LockssThread {
    private long lastReload = 0;
    private volatile boolean goOn = true;
    private Deadline nextReload;

    private HandlerThread(String name) {
      super(name);
    }

    public void lockssRun() {
      Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
      startWDog(WDOG_PARAM_CONFIG, WDOG_DEFAULT_CONFIG);
      triggerWDogOnExit(true);

      // repeat every 10ish minutes until first successful load, then
      // according to org.lockss.parameterReloadInterval, or 30 minutes.
      while (goOn) {
	pokeWDog();
	if (updateConfig()) {
	  if (tiny != null) {
	    stopTinyUi();
	  }
	  // true iff loaded config has changed
	  if (!goOn) {
	    break;
	  }
	  lastReload = TimeBase.nowMs();
	  //	stopAndOrStartThings(true);
	} else {
	  if (lastReload == 0) {
	    if (tiny == null) {
	      startTinyUi();
	    } else {
	      updateTinyData();
	    }
	  }
	}
	pokeWDog();			// in case update took a long time
	long reloadRange = reloadInterval/4;
	nextReload = Deadline.inRandomRange(reloadInterval - reloadRange,
					    reloadInterval + reloadRange);
	log.debug2(nextReload.toString());
	if (goOn) {
	  try {
	    nextReload.sleep();
	  } catch (InterruptedException e) {
	    // just wakeup and check for exit
	  }
	}
      }
    }

    private void stopHandler() {
      goOn = false;
      this.interrupt();
    }

    void forceReload() {
      if (nextReload != null) {
	nextReload.expire();
      }
    }
  }
}
