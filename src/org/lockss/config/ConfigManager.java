/*
 * $Id: ConfigManager.java,v 1.1 2004-09-27 22:09:15 smorabito Exp $
 */

/*

Copyright (c) 2001-2002 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.*;
import org.lockss.mail.*;
import org.lockss.hasher.*;
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
  static final String PARAM_CONFIG_PATH = MYPREFIX + "configFilePath";
  static final String DEFAULT_CONFIG_PATH = "config";

  /** Config param written to local config files to indicate file version */
  static final String PARAM_CONFIG_FILE_VERSION =
    MYPREFIX + "fileVersion.<filename>";

  /** Temporary param to enable new scheduler */
  public static final String PARAM_NEW_SCHEDULER =
    HashService.PREFIX + "use.scheduler";
  static final boolean DEFAULT_NEW_SCHEDULER = true;

  public static final String PARAM_TITLE_DB = Configuration.PREFIX + "title";

  /** Common prefix of platform config params */
  static final String PLATFORM = Configuration.PLATFORM;

  /** Local (routable) IP address, for lcap identity */
  public static final String PARAM_PLATFORM_IP_ADDRESS =
    PLATFORM + "localIPAddress";

  public static final String PARAM_PLATFORM_FQDN = PLATFORM + "fqdn";

  /** Local subnet set during config */
  public static final String PARAM_PLATFORM_ACCESS_SUBNET =
    PLATFORM + "accesssubnet";

  static final String PARAM_PLATFORM_DISK_SPACE_LIST =
    PLATFORM + "diskSpacePaths";

  static final String PARAM_PLATFORM_VERSION = PLATFORM + "version";
  public static final String PARAM_PLATFORM_ADMIN_EMAIL =
    PLATFORM + "sysadminemail";
  static final String PARAM_PLATFORM_LOG_DIR = PLATFORM + "logdirectory";
  static final String PARAM_PLATFORM_LOG_FILE = PLATFORM + "logfile";

  static final String PARAM_PLATFORM_SMTP_HOST = PLATFORM + "smtphost";
  static final String PARAM_PLATFORM_SMTP_PORT = PLATFORM + "smtpport";
  static final String PARAM_PLATFORM_PIDFILE = PLATFORM + "pidfile";

  public static String CONFIG_FILE_UI_IP_ACCESS = "ui_ip_access.txt";
  public static String CONFIG_FILE_PROXY_IP_ACCESS = "proxy_ip_access.txt";
  public static String CONFIG_FILE_AU_CONFIG = "au.txt";

  /** array of local cache config file names */
  static String cacheConfigFiles[] = {
    CONFIG_FILE_UI_IP_ACCESS,
    CONFIG_FILE_PROXY_IP_ACCESS,
    CONFIG_FILE_AU_CONFIG,
  };


  // MUST pass in explicit log level to avoid recursive call back to
  // Configuration to get Config log level.  (Others should NOT do this.)
  protected static Logger log =
    Logger.getLoggerWithInitialLevel("Config",
				     Logger.getInitialDefaultLevel());

  protected LockssApp theApp = null;

  private List configChangedCallbacks = new ArrayList();

  private List configUrlList;	// list of urls
  private String groupName;     // daemon group name

  private String recentLoadError;

  /** A constant empty Configuration object */
  public static Configuration EMPTY_CONFIGURATION = newConfiguration();
  static {
    EMPTY_CONFIGURATION.seal();
  }

  // Current configuration instance.
  // Start with an empty one to avoid errors in the static accessors.
  private Configuration currentConfig = EMPTY_CONFIGURATION;

  private OneShotSemaphore haveConfig = new OneShotSemaphore();

  private HandlerThread handlerThread; // reload handler thread

  private long reloadInterval = 10 * Constants.MINUTE;

  private ConfigCache configCache = new ConfigCache();

  public ConfigManager() {
    this(null, null);
  }

  public ConfigManager(List urls) {
    this(null, null);
  }

  public ConfigManager(List urls, String groupName) {
    if (urls != null) {
      configUrlList = new ArrayList(urls);
    }
    this.groupName = groupName;
    registerConfigurationCallback(Logger.getConfigCallback());
  }

  public ConfigCache getConfigCache() {
    return configCache;
  }

  // Used only for testing.
  public void resetConfigCache() {
    configCache = new ConfigCache();
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

  public Configuration readConfig(List urlList) {
    return readConfig(urlList, null);
  }

  /**
   * Return a new <code>Configuration</code> instance loaded from the
   * url list
   */
  public Configuration readConfig(List urlList, String groupName) {
    if (urlList == null) {
      return null;
    }
    Configuration newConfig = newConfiguration();
    try {
      boolean gotIt = newConfig.loadList(urlList);
      if (gotIt) {
	recentLoadError = null;
	if (groupName != null) {
	  newConfig.put(Configuration.PARAM_DAEMON_GROUP,
			groupName);
	}
	return newConfig;
      } else {
	recentLoadError =
	  getLoadErrorMessage(newConfig.getFirstErrorFile(urlList));
	return null;
      }
    } catch (Exception e) {
      log.error("Error loading config", e);
      recentLoadError = e.toString();
      return null;
    }
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

  boolean updateConfig() {
    Configuration newConfig = readConfig(configUrlList, groupName);
    return installConfig(newConfig);
  }

  boolean installConfig(Configuration newConfig) {
    if (newConfig == null) {
      return false;
    }
    initCacheConfig(newConfig);
    List loadedCacheFiles = loadCacheConfigInto(newConfig);
    copyPlatformParams(newConfig);
    newConfig.seal();
    Configuration oldConfig = currentConfig;
    if (!oldConfig.isEmpty() && newConfig.equals(oldConfig)) {
      if (reloadInterval >= 10 * Constants.MINUTE) {
	log.info("Config unchanged, not updated");
      }
      return false;
    }
    setCurrentConfig(newConfig);
    Configuration.Differences diffs = newConfig.differences(oldConfig);
    logConfigLoaded(newConfig, oldConfig, diffs, loadedCacheFiles);
    runCallbacks(newConfig, oldConfig, diffs);
    haveConfig.fill();
    return true;
  }

  private void logConfigLoaded(Configuration newConfig,
			       Configuration oldConfig,
			       Configuration.Differences diffs,
			       List loadedCacheFiles) {
    StringBuffer sb = new StringBuffer("Config updated");
    if (configUrlList != null || loadedCacheFiles != null) {
      sb.append(" from ");
      if (configUrlList != null) {
	sb.append(StringUtil.separatedString(configUrlList, ", "));
      }
      if (loadedCacheFiles != null) {
	sb.append("; ");
	sb.append(StringUtil.separatedString(loadedCacheFiles, ", "));
      }
    }
    log.info(sb.toString());
    if (log.isDebug()) {
      logConfig(newConfig, oldConfig, diffs);
    }
  }

  static final String PARAM_HASH_SVC = "org.lockss.manager.HashService";
  static final String DEFAULT_HASH_SVC = "org.lockss.hasher.HashSvcSchedImpl";

  private void copyPlatformParams(Configuration config) {
    copyPlatformVersionParams(config);
    // hack to make hash use new scheduler without directly setting
    // org.lockss.manager.HashService, which would break old daemons.
    // don't set if already has a value
    if (config.get(PARAM_HASH_SVC) == null &&
	config.getBoolean(PARAM_NEW_SCHEDULER, DEFAULT_NEW_SCHEDULER)) {
      config.put(PARAM_HASH_SVC, DEFAULT_HASH_SVC);
    }

    String logdir = config.get(PARAM_PLATFORM_LOG_DIR);
    String logfile = config.get(PARAM_PLATFORM_LOG_FILE);
    if (logdir != null && logfile != null) {
      platformOverride(config, FileTarget.PARAM_FILE,
		       new File(logdir, logfile).toString());
    }

    conditionalPlatformOverride(config, PARAM_PLATFORM_IP_ADDRESS,
				IdentityManager.PARAM_LOCAL_IP);

    conditionalPlatformOverride(config, PARAM_PLATFORM_SMTP_PORT,
				SmtpMailService.PARAM_SMTPPORT);
    conditionalPlatformOverride(config, PARAM_PLATFORM_SMTP_HOST,
				SmtpMailService.PARAM_SMTPHOST);

    String platformSubnet = config.get(PARAM_PLATFORM_ACCESS_SUBNET);
    appendPlatformAccess(config, ServletManager.PARAM_IP_INCLUDE,
			 platformSubnet);
    appendPlatformAccess(config, ProxyManager.PARAM_IP_INCLUDE,
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

  private void appendPlatformAccess(Configuration config, String accessParam,
				    String platformAccess) {
    if (StringUtil.isNullString(platformAccess)) {
      return;
    }
    String includeIps = config.get(accessParam);
    if (StringUtil.isNullString(includeIps)) {
      includeIps = platformAccess;
    } else {
      includeIps = platformAccess + ";" + includeIps;
    }
    config.put(accessParam, includeIps);
  }

  private void logConfig(Configuration config,
			 Configuration oldConfig,
			 Configuration.Differences diffs) {
    Set diffSet = diffs.getDifferenceSet();
    SortedSet keys = new TreeSet(diffSet != null ? diffSet : config.keySet());
    for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (log.isDebug2() || !key.startsWith(PARAM_TITLE_DB)) {
	if (config.containsKey(key)) {
	  log.debug(key + " = " + (String)config.get(key));
	} else if (oldConfig.containsKey(key)) {
	  log.debug(key + " (removed)");
	}
      }
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

  static void resetForTesting() {
    cacheConfigInited = false;
    cacheConfigDir = null;
  }

  static boolean cacheConfigInited = false;
  static String cacheConfigDir = null;

  boolean isUnitTesting() {
    return Boolean.getBoolean("org.lockss.unitTesting");
  }

  List getDiskSpacePaths() {
    String dspace = currentConfig.get(PARAM_PLATFORM_DISK_SPACE_LIST);
    return StringUtil.breakAt(dspace, ';');
  }

  public List getRepositoryList() {
    List res = new ArrayList();
    List paths = getDiskSpacePaths();
    if (paths != null) {
      for (Iterator iter = paths.iterator(); iter.hasNext(); ) {
	res.add("local:" + (String)iter.next());
      }
    }
    return res;
  }

  private void initCacheConfig(Configuration newConfig) {
    if (cacheConfigInited) return;
    String dspace = newConfig.get(PARAM_PLATFORM_DISK_SPACE_LIST);
    String relConfigPath = newConfig.get(PARAM_CONFIG_PATH,
					 DEFAULT_CONFIG_PATH);
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
	cacheConfigDir = configDir.toString();
	break;
      }
    }
    if (cacheConfigDir == null) {
      if (v.size() >= 1) {
	String path = (String)v.get(0);
	File dir = new File(path, relConfigPath);
	if (dir.mkdirs()) {
	  cacheConfigDir = dir.toString();
	}
      }
    }
    cacheConfigInited = true;
  }

  /** Return a File for the named cache config file */
  public File getCacheConfigFile(String cacheConfigFileName) {
    return new File(ConfigManager.cacheConfigDir, cacheConfigFileName);
  }

  List loadCacheConfigInto(Configuration config) {
    if (cacheConfigDir == null) {
      return null;
    }
    List res = new ArrayList();
    for (int ix = 0; ix < cacheConfigFiles.length; ix++) {
      File cfile = new File(cacheConfigDir, cacheConfigFiles[ix]);
      log.debug2("Loading cache config from " + cfile);
      boolean gotIt = config.loadList(ListUtil.list(cfile.toString()), true);
      if (gotIt) {
	res.add(cfile.toString());
      }
    }
    return res;
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
    File cfile = new File(cacheConfigDir, cacheConfigFileName);
    log.debug2("Reading cache config file: " + cfile.toString());
    InputStream is = new FileInputStream(cfile);
    Configuration res = newConfiguration();

    if (StringUtil.endsWithIgnoreCase(cacheConfigFileName, ".xml")) {
      res.loadXmlProperties(is);
    } else {
      res.loadTextProperties(is);
    }

    is.close();
    return res;
  }

  /** Return the contents of the local AU config file.
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
  public void writeCacheConfigFile(Configuration config,
				   String cacheConfigFileName,
				   String header)
      throws IOException {
    if (cacheConfigDir == null) {
      log.warning("Attempting to write cache config file: " +
		  cacheConfigFileName + ", but no cache config dir exists");
      throw new RuntimeException("No cache config dir");
    }
    File cfile = new File(cacheConfigDir, cacheConfigFileName);
    OutputStream os = new FileOutputStream(cfile);

    // make a copy and add the config file version number, write the copy
    Configuration tmpConfig = config.copy();
    tmpConfig.put(configVersionProp(cacheConfigFileName), "1");
    tmpConfig.store(os, header);
    os.close();
    log.debug2("Wrote cache config file: " + cfile.toString());
    if (handlerThread != null) {
      handlerThread.forceReload();
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
    // first remove all existing values for the AU
    if (auPropKey != null) {
      fileConfig.removeConfigTree(auPropKey);
    }
    for (Iterator iter = auConfig.keySet().iterator(); iter.hasNext();) {
      String key = (String)iter.next();
      fileConfig.put(key, auConfig.get(key));
    }
    writeCacheConfigFile(fileConfig, CONFIG_FILE_AU_CONFIG,
			 "AU Configuration");
  }


  // static convenience methods

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static String getParam(String key) {
    return getCurrentConfig().get(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static String getParam(String key, String dfault) {
    return getCurrentConfig().get(key, dfault);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static boolean getBooleanParam(String key)
      throws Configuration.InvalidParam {
    return getCurrentConfig().getBoolean(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static boolean getBooleanParam(String key, boolean dfault) {
    return getCurrentConfig().getBoolean(key, dfault);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static int getIntParam(String key)
      throws Configuration.InvalidParam {
    return getCurrentConfig().getInt(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static int getIntParam(String key, int dfault) {
    return getCurrentConfig().getInt(key, dfault);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static long getLongParam(String key)
      throws Configuration.InvalidParam {
    return getCurrentConfig().getLong(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static long getLongParam(String key, long dfault) {
    return getCurrentConfig().getLong(key, dfault);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static long getTimeIntervalParam(String key)
      throws Configuration.InvalidParam {
    return getCurrentConfig().getTimeInterval(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static long getTimeIntervalParam(String key, long dfault) {
    return getCurrentConfig().getTimeInterval(key, dfault);
  }

  /** Static convenience method to get a <code>Configuration</code>
   * subtree from the current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static Configuration paramConfigTree(String key) {
    return getCurrentConfig().getConfigTree(key);
  }

  /** Static convenience method to get key iterator from the
   * current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static Iterator paramKeyIterator() {
    return getCurrentConfig().keyIterator();
  }

  /** Static convenience method to get a node iterator from the
   * current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static Iterator paramNodeIterator(String key) {
    return getCurrentConfig().nodeIterator(key);
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

  // Handler thread, periodicially reloads config

  private class HandlerThread extends Thread {
    private long lastReload = 0;
    private boolean goOn = false;
    private Deadline nextReload;

    private HandlerThread(String name) {
      super(name);
    }

    public void run() {
      Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
      goOn = true;

      // repeat every 10ish minutes until first successful load, then
      // according to org.lockss.parameterReloadInterval, or 30 minutes.
      while (goOn) {
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
	  reloadInterval = getTimeIntervalParam(PARAM_RELOAD_INTERVAL,
						DEFAULT_RELOAD_INTERVAL);

	} else {
	  if (lastReload == 0) {
	    if (tiny == null) {
	      startTinyUi();
	    } else {
	      updateTinyData();
	    }
	  }
	}
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
