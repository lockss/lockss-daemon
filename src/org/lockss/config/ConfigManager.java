/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;

import org.apache.commons.collections.Predicate;
import org.apache.commons.io.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.oro.text.regex.*;

import org.lockss.app.*;
import org.lockss.account.*;
import org.lockss.clockss.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.mail.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.proxy.*;
import org.lockss.remote.*;
import org.lockss.repository.*;
import org.lockss.servlet.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/** ConfigManager loads and periodically reloads the LOCKSS configuration
 * parameters, and provides services for updating locally changeable
 * configuration.
 *
 * @ParamCategory Configuration
 *
 * @ParamCategoryDoc Configuration These parameters mostly pertain to the
 * configuration mechanism itself.
 *
 * @ParamCategoryDoc Platform Parameters with the
 * <code>org.lockss.config.platform prefix</code> are set by the
 * system-dependent startup scripts, based on information gathered by
 * running hostconfig.  They should generally not be set manually except in
 * testing environments.
 */
public class ConfigManager implements LockssManager {
  /** The common prefix string of all LOCKSS configuration parameters. */
  public static final String PREFIX = Configuration.PREFIX;

  /** Common prefix of platform config params */
  public static final String PLATFORM = Configuration.PLATFORM;
  public static final String DAEMON = Configuration.DAEMON;

  static final String MYPREFIX = PREFIX + "config.";

  /** The interval at which the daemon checks the various configuration
   * files, including title dbs, for changes.
   * @ParamCategory Tuning
   * @ParamRelevance Rare
   */
  static final String PARAM_RELOAD_INTERVAL = MYPREFIX + "reloadInterval";
  static final long DEFAULT_RELOAD_INTERVAL = 30 * Constants.MINUTE;

  /** If set to <i>hostname</i>:<i>port</i>, the configuration server will
   * be accessed via the specified proxy.  Leave unset for direct
   * connection.
   * @ParamCategory Platform
   */
  public static final String PARAM_PROPS_PROXY = PLATFORM + "propsProxy";

  /** If set, the authenticity of the config server will be checked using
   * this keystore.  The value is either an internal name designating a
   * resource (e.g. <tt>&quot;lockss-ca&quot;</tt>, to use the builtin
   * keystore containing the LOCKSS signing cert (see {@link
   * #builtinServerAuthKeystores}), or the filename of a keystore. Can only
   * be set in platform config.
   * @ParamCategory Platform
   */
  public static final String PARAM_SERVER_AUTH_KEYSTORE_NAME =
    MYPREFIX + "serverAuthKeystore";

  /** If set, the daemon will authenticate itself to the config server
   * using this keystore.  The value is the name of the keystore (defined
   * by additional <tt>org.lockss.keyMgr.keystore.&lt;id&gt;.<i>xxx</i></tt>
   * parameters (see {@link org.lockss.daemon.LockssKeyStoreManager}), or
   * <tt>&quot;lockss-ca&quot;</tt>, to use the builtin keystore containing
   * the LOCKSS signing cert.  Can only be set in platform config.
   * @ParamCategory Platform
   */
  public static final String PARAM_CLIENT_AUTH_KEYSTORE_NAME =
    MYPREFIX + "clientAuthKeystore";

  /** Map of internal name to resource location of keystore to use to check
   * authenticity of the config server */
  static Map<String,String> builtinServerAuthKeystores =
    MapUtil.map("lockss-ca", "org/lockss/config/lockss-ca.keystore");

  /** Interval at which the regular GET request to the config server will
   * include an {@value Constants.X_LOCKSS_INFO} with version and other
   * information.  Used in conjunction with logging hooks on the server.
   * @ParamRelevance Rare
   */
  static final String PARAM_SEND_VERSION_EVERY = MYPREFIX + "sendVersionEvery";
  static final long DEFAULT_SEND_VERSION_EVERY = 1 * Constants.DAY;

  static final String WDOG_PARAM_CONFIG = "Config";
  static final long WDOG_DEFAULT_CONFIG = 2 * Constants.HOUR;

  /** Path to local config directory, relative to entries on diskSpacePaths.
   * @ParamRelevance Rare
   */
  public static final String PARAM_CONFIG_PATH = MYPREFIX + "configFilePath";
  public static final String DEFAULT_CONFIG_PATH = "config";

  /** When logging new or changed config, truncate val at this length.
   * @ParamRelevance Rare
   */
  static final String PARAM_MAX_LOG_VAL_LEN =
    MYPREFIX + "maxLogValLen";
  static final int DEFAULT_MAX_LOG_VAL_LEN = 2000;

  /** Config param written to local config files to indicate file version.
   * Not intended to be set manually.
   * @ParamRelevance Rare
   */
  static final String PARAM_CONFIG_FILE_VERSION =
    MYPREFIX + "fileVersion.<filename>";

  /** Set false to disable scheduler.  Used only for unit tests.
   * @ParamRelevance Never
   */
  public static final String PARAM_NEW_SCHEDULER =
    HashService.PREFIX + "use.scheduler";
  static final boolean DEFAULT_NEW_SCHEDULER = true;

  /** Maximum number of AU config changes to to save up during a batch add
   * or remove operation, before writing them to au.txt  
   * @ParamRelevance Rare
   */
  public static final String PARAM_MAX_DEFERRED_AU_BATCH_SIZE =
    MYPREFIX + "maxDeferredAuBatchSize";
  public static final int DEFAULT_MAX_DEFERRED_AU_BATCH_SIZE = 100;

  /** Root of TitleDB definitions.  This is not an actual parameter.
   * @ParamRelevance Never
   */
  public static final String PARAM_TITLE_DB = Configuration.PREFIX + "title";
  /** Prefix of TitleDB definitions.  */
  public static final String PREFIX_TITLE_DB = PARAM_TITLE_DB + ".";

  /** List of URLs of title DBs configured locally using UI.  Do not set
   * manually
   * @ParamRelevance Never
   * @ParamAuto
   */
  public static final String PARAM_USER_TITLE_DB_URLS =
    Configuration.PREFIX + "userTitleDbs";

  /** List of URLs of title DBs to load.  Normally set in lockss.xml
   * @ParamRelevance Common
   */
  public static final String PARAM_TITLE_DB_URLS =
    Configuration.PREFIX + "titleDbs";

  /** List of URLs of auxilliary config files
   * @ParamRelevance LessCommon
   */
  public static final String PARAM_AUX_PROP_URLS =
    Configuration.PREFIX + "auxPropUrls";

  /** false disables SSL SNI name checking, compatible with Java 6 and
   * misconfigured servers.
   * @ParamRelevance BackwardCompatibility
   */
  public static final String PARAM_JSSE_ENABLESNIEXTENSION =
    PREFIX + "jsse.enableSNIExtension";
  static final boolean DEFAULT_JSSE_ENABLESNIEXTENSION = true;

  /** Parameters whose values are more prop URLs */
  static final Set URL_PARAMS =
    SetUtil.set(PARAM_USER_TITLE_DB_URLS,
		PARAM_TITLE_DB_URLS,
		PARAM_AUX_PROP_URLS);

  /** Place to put temporary files and directories.  If not set,
   * java.io.tmpdir System property is used.  On a busy, capacious LOCKSS
   * box, this should be a minimum of 50-100MB.
   * @ParamCategory Platform
   * @ParamRelevance Required
   */
  public static final String PARAM_TMPDIR = PLATFORM + "tmpDir";

  /** Used only for testing.  The daemon version is normally loaded from a
   * file created during the build process.  If that file is absent, the
   * daemon version will be obtained from this param, if set.
   * @ParamRelevance Never
   */
  public static final String PARAM_DAEMON_VERSION = DAEMON + "version";

  /** Platform version string (<i>name</i>-<i>ver</i> or
   * <i>name</i>-<i>ver</i>-<i>suffix</i> . <i>Eg</i>, Linux RPM-1).
   * @ParamCategory Platform
   * @ParamRelevance Common
   */
  public static final String PARAM_PLATFORM_VERSION = PLATFORM + "version";

  /** Fully qualified host name (fqdn).  
   * @ParamCategory Platform
   */
  public static final String PARAM_PLATFORM_FQDN = PLATFORM + "fqdn";

  /** Project name (CLOCKSS or LOCKSS)
   * @ParamCategory Platform
   * @ParamRelevance Required
   */
  public static final String PARAM_PLATFORM_PROJECT = PLATFORM + "project";
  public static final String DEFAULT_PLATFORM_PROJECT = "lockss";

  /** Group names.  Boxes with at least one group in common will
   * participate in each others' polls.  Also used to evaluate group=
   * config file conditional,
   * @ParamCategory Platform
   * @ParamRelevance Required
   */
  public static final String PARAM_DAEMON_GROUPS = DAEMON + "groups";
  public static final String DEFAULT_DAEMON_GROUP = "nogroup";
  public static final List DEFAULT_DAEMON_GROUP_LIST =
    ListUtil.list(DEFAULT_DAEMON_GROUP);

  /** Local IP address
   * @ParamCategory Platform
   * @ParamRelevance Required
   */
  public static final String PARAM_PLATFORM_IP_ADDRESS =
    PLATFORM + "localIPAddress";

  /** Second IP address, for CLOCKSS subscription detection
   * @ParamCategory Platform
   * @ParamRelevance Obsolescent
   */
  public static final String PARAM_PLATFORM_SECOND_IP_ADDRESS =
    PLATFORM + "secondIP";

  /** LCAP V3 identity string.  Of the form
   * <code><i>proto</i>:[<i>ip-addr</i>:<i>port</i>]</code>; <i>eg</i>,
   * <code>TCP:[10.33.44.55:9729]</code> or
   * <code>tcp:[0:0:00:0000:0:0:0:1]:9729</code> .  Other boxes in the
   * network must be able to reach this one by connecting to the specified
   * host and port.  If behind NAT, this should be the external, routable,
   * NAT address.
   * @ParamCategory Platform
   * @ParamRelevance Required
   */
  public static final String PARAM_PLATFORM_LOCAL_V3_IDENTITY =
    PLATFORM + "v3.identity";

  /** Initial value of ACL controlling access to the admin UI.  Normally
   * set by hostconfig.  Once the access list is edited in the UI, that
   * list takes precedence over this one.  However, any changes to this one
   * after that point (<i>ie</i>, by rerunning hostconfig), will take
   * effect and be reflected in the list visible in the UI.
   * @ParamCategory Platform
   * @ParamRelevance Common
   */
  public static final String PARAM_PLATFORM_ACCESS_SUBNET =
    PLATFORM + "accesssubnet";

  /** List of filesystem paths to space available to store content and
   * other files
   * @ParamCategory Platform
   * @ParamRelevance Required
   */
  public static final String PARAM_PLATFORM_DISK_SPACE_LIST =
    PLATFORM + "diskSpacePaths";

  /** Email address to which various alerts, reports and backup file may be
   * sent.
   * @ParamCategory Platform
   * @ParamRelevance Common
   */
  public static final String PARAM_PLATFORM_ADMIN_EMAIL =
    PLATFORM + "sysadminemail";
  public static final String PARAM_PLATFORM_LOG_DIR = PLATFORM + "logdirectory";
  static final String PARAM_PLATFORM_LOG_FILE = PLATFORM + "logfile";

  /** SMTP relay host that will accept mail from this host.
   * @ParamCategory Platform
   * @ParamRelevance Common
   */
  public static final String PARAM_PLATFORM_SMTP_HOST = PLATFORM + "smtphost";

  /** SMTP relay port, if not the default
   * @ParamCategory Platform
   * @ParamRelevance Rare
   */
  public static final String PARAM_PLATFORM_SMTP_PORT = PLATFORM + "smtpport";

  /** If true, local copies of remote config files will be maintained, to
   * allow daemon to start when config server isn't available.
   * @ParamCategory Platform
   * @ParamRelevance Rare
   */
  public static final String PARAM_REMOTE_CONFIG_FAILOVER = PLATFORM +
    "remoteConfigFailover";
  public static final boolean DEFAULT_REMOTE_CONFIG_FAILOVER = true;

  /** Dir in which to store local copies of remote config files.
   * @ParamCategory Platform
   * @ParamRelevance Rare
   */
  public static final String PARAM_REMOTE_CONFIG_FAILOVER_DIR = PLATFORM +
    "remoteConfigFailoverDir";
  public static final String DEFAULT_REMOTE_CONFIG_FAILOVER_DIR = "remoteCopy";

  /** Maximum acceptable age of a remote config failover file, specified as
   * an integer followed by h, d, w or y for hours, days, weeks and years.
   * Zero means no age limit.
   * @ParamCategory Platform
   * @ParamRelevance Rare
   */
  public static final String PARAM_REMOTE_CONFIG_FAILOVER_MAX_AGE = PLATFORM +
    "remoteConfigFailoverMaxAge";
  public static final long DEFAULT_REMOTE_CONFIG_FAILOVER_MAX_AGE = 0;

  /** Checksum algorithm used to verify remote config failover file
   * @ParamCategory Platform
   * @ParamRelevance Rare
   */
  public static final String PARAM_REMOTE_CONFIG_FAILOVER_CHECKSUM_ALGORITHM =
    PLATFORM + "remoteConfigFailoverChecksumAlgorithm";
  public static final String DEFAULT_REMOTE_CONFIG_FAILOVER_CHECKSUM_ALGORITHM =
    "SHA-256";

  /** Failover file not accepted unless it has a checksum.
   * @ParamCategory Platform
   * @ParamRelevance Rare
   */
  public static final String PARAM_REMOTE_CONFIG_FAILOVER_CHECKSUM_REQUIRED =
    PLATFORM + "remoteConfigFailoverChecksumRequired";
  public static final boolean DEFAULT_REMOTE_CONFIG_FAILOVER_CHECKSUM_REQUIRED =
    true;


  public static final String CONFIG_FILE_UI_IP_ACCESS = "ui_ip_access.txt";
  public static final String CONFIG_FILE_PROXY_IP_ACCESS =
    "proxy_ip_access.txt";
  public static final String CONFIG_FILE_PLUGIN_CONFIG = "plugin.txt";
  public static final String CONFIG_FILE_AU_CONFIG = "au.txt";
  public static final String CONFIG_FILE_BUNDLED_TITLE_DB = "titledb.xml";
  public static final String CONFIG_FILE_CONTENT_SERVERS =
    "content_servers_config.txt";
  public static final String CONFIG_FILE_ACCESS_GROUPS =
    "access_groups_config.txt"; // not yet in use
  public static final String CONFIG_FILE_CRAWL_PROXY = "crawl_proxy.txt";
  public static final String CONFIG_FILE_EXPERT = "expert_config.txt";

  /** Obsolescent - replaced by CONFIG_FILE_CONTENT_SERVERS */
  public static final String CONFIG_FILE_ICP_SERVER = "icp_server_config.txt";
  /** Obsolescent - replaced by CONFIG_FILE_CONTENT_SERVERS */
  public static final String CONFIG_FILE_AUDIT_PROXY =
    "audit_proxy_config.txt";

  public static final String REMOTE_CONFIG_FAILOVER_FILENAME =
    "remote_config_failover_info.xml";

  /** If set to a list of regexps, matching parameter names will be allowed
   * to be set in expert config, and loaded from expert_config.txt
   * @ParamRelevance Rare
   */
  public static final String PARAM_EXPERT_ALLOW = MYPREFIX + "expert.allow";
  public static final List DEFAULT_EXPERT_ALLOW = null;

  /** If set to a list of regexps, matching parameter names will not be
   * allowed to be set in expert config, and loaded from expert_config.txt.
   * The default prohibits using expert config to subvert platform
   * settings, change passwords or keystores, or cause the daemon to exit
   * @ParamRelevance Rare
   */
  public static final String PARAM_EXPERT_DENY = MYPREFIX + "expert.deny";
  static String ODLD = "^org\\.lockss\\.";
  public static final List DEFAULT_EXPERT_DENY =
    ListUtil.list("[pP]assword\\b",
		  ODLD +"platform\\.",
		  ODLD +"keystore\\.",
		  ODLD +"app\\.exit(Once|After|Immediately)$",
		  Perl5Compiler.quotemeta(PARAM_DAEMON_GROUPS),
		  Perl5Compiler.quotemeta(PARAM_AUX_PROP_URLS),
		  Perl5Compiler.quotemeta(IdentityManager.PARAM_LOCAL_IP),
		  Perl5Compiler.quotemeta(IdentityManager.PARAM_LOCAL_V3_IDENTITY),
		  Perl5Compiler.quotemeta(IdentityManager.PARAM_LOCAL_V3_PORT),
		  Perl5Compiler.quotemeta(IpAccessControl.PARAM_ERROR_BELOW_BITS),
		  Perl5Compiler.quotemeta(PARAM_EXPERT_ALLOW),
		  Perl5Compiler.quotemeta(PARAM_EXPERT_DENY),
		  Perl5Compiler.quotemeta(LockssDaemon.PARAM_TESTING_MODE)
		  );

  /** Describes a config file stored on the local disk, normally maintained
   * by the daemon. See writeCacheConfigFile(), et al. */
  public static class LocalFileDescr {
    String name;
    File file;
    KeyPredicate keyPred;
    Predicate includePred;
    boolean needReloadAfterWrite = true;

    LocalFileDescr(String name) {
      this.name = name;
    }

    String getName() {
      return name;
    }

    public File getFile() {
      return file;
    }

    public void setFile(File file) {
      this.file = file;
    }

    KeyPredicate getKeyPredicate() {
      return keyPred;
    }

    LocalFileDescr setKeyPredicate(KeyPredicate keyPred) {
      this.keyPred = keyPred;
      return this;
    }

    Predicate getIncludePredicate() {
      return includePred;
    }

    LocalFileDescr setIncludePredicate(Predicate pred) {
      this.includePred = pred;
      return this;
    }

    boolean isNeedReloadAfterWrite() {
      return needReloadAfterWrite;
    }

    LocalFileDescr setNeedReloadAfterWrite(boolean val) {
      this.needReloadAfterWrite = val;
      return this;
    }
  }

  /** KeyPredicate determines legal keys, and whether illegal keys cause
   * file to fail or are just ignored */
  public interface KeyPredicate extends Predicate {
    public boolean failOnIllegalKey();
  }

  /** Always true predicate */
  KeyPredicate trueKeyPredicate = new KeyPredicate() {
      public boolean evaluate(Object obj) {
	return true;
      }
      public boolean failOnIllegalKey() {
	return false;
      }
      public String toString() {
	return "trueKeyPredicate";
      }};


  /** Allow only params below o.l.title and o.l.titleSet .  For use with
   * title DB files */
  static final String PREFIX_TITLE_SETS_DOT =
    PluginManager.PARAM_TITLE_SETS + ".";

  KeyPredicate titleDbOnlyPred = new KeyPredicate() {
      public boolean evaluate(Object obj) {
	if (obj instanceof String) {
	  return ((String)obj).startsWith(PREFIX_TITLE_DB) ||
	    ((String)obj).startsWith(PREFIX_TITLE_SETS_DOT);
	}
	return false;
      }
      public boolean failOnIllegalKey() {
	return true;
      }
      public String toString() {
	return "titleDbOnlyPred";
      }};

  /** Disallow keys prohibited in expert config file, defined by {@link
   * #PARAM_EXPERT_ALLOW} and {@link #PARAM_EXPERT_DENY} */
  public KeyPredicate expertConfigKeyPredicate = new KeyPredicate() {
      public boolean evaluate(Object obj) {
	if (obj instanceof String) {
	  return isLegalExpertConfigKey((String)obj);
	}
	return false;
      }
      public boolean failOnIllegalKey() {
	return false;
      }
      public String toString() {
	return "expertConfigKeyPredicate";
      }};

  /** Argless predicate that's true if expert config file should be
   * loaded */
  Predicate expertConfigIncludePredicate = new Predicate() {
      public boolean evaluate(Object obj) {
	return enableExpertConfig;
      }
      public String toString() {
	return "expertConfigIncludePredicate";
      }};

  public boolean isExpertConfigEnabled() {
    return enableExpertConfig;
  }

  /** A config param is:<ul><li>allowed if it matches a pattern in
   * <code>org.lockss.config.expert.allow</code> (if set), else</li>
   * <li>disallowed if it matches a pattern in
   * <code>org.lockss.config.expert.deny</code> (if set), else</li>
   * <li>Allowed.</li></ul>. */
  public boolean isLegalExpertConfigKey(String key) {
    if (!expertConfigAllowPats.isEmpty()) {
      for (Pattern pat : expertConfigAllowPats) {
	if (RegexpUtil.getMatcher().contains(key, pat)) {
	  return true;
	}
      }
    }
    if (expertConfigDenyPats.isEmpty()) {
      // If no deny pats, return true iff there are also no allow pats
      return expertConfigAllowPats == null;
    } else {
      for (Pattern pat : expertConfigDenyPats) {
	if (RegexpUtil.getMatcher().contains(key, pat)) {
	  return false;
	}
      }
    }
    // Didn't match either, and there are deny pats.
    return true;
  }

  /** Array of local cache config file names.  Do not use this directly,
   * call {@link #getLocalFileDescrs()} or {@link
   * #getLocalFileDescrMap()}. */
  LocalFileDescr cacheConfigFiles[] = {
    new LocalFileDescr(CONFIG_FILE_UI_IP_ACCESS),
    new LocalFileDescr(CONFIG_FILE_PROXY_IP_ACCESS),
    new LocalFileDescr(CONFIG_FILE_PLUGIN_CONFIG),
    // au.txt updates correspond to changes already made to running
    // structures, so needn't cause a config reload.
    new LocalFileDescr(CONFIG_FILE_AU_CONFIG)
    .setNeedReloadAfterWrite(false),
    new LocalFileDescr(CONFIG_FILE_ICP_SERVER), // obsolescent
    new LocalFileDescr(CONFIG_FILE_AUDIT_PROXY),	// obsolescent
    // must follow obsolescent icp server and audit proxy files
    new LocalFileDescr(CONFIG_FILE_CONTENT_SERVERS),
    new LocalFileDescr(CONFIG_FILE_ACCESS_GROUPS), // not yet in use
    new LocalFileDescr(CONFIG_FILE_CRAWL_PROXY),
    new LocalFileDescr(CONFIG_FILE_EXPERT)
    .setKeyPredicate(expertConfigKeyPredicate)
    .setIncludePredicate(expertConfigIncludePredicate),
  };

  // MUST pass in explicit log level to avoid recursive call back to
  // Configuration to get Config log level.  (Others should NOT do this.)
  private static final Logger log =
    Logger.getLoggerWithInitialLevel("Config",
				     Logger.getInitialDefaultLevel());

  /** A constant empty Configuration object */
  public static final Configuration EMPTY_CONFIGURATION = newConfiguration();
  static {
    EMPTY_CONFIGURATION.seal();
  }

  protected LockssApp theApp = null;

  private List configChangedCallbacks = new ArrayList();

  private LinkedHashMap<String,LocalFileDescr> cacheConfigFileMap = null;

  private List configUrlList;		// list of config file urls
  // XXX needs synchronization
  private List titledbUrlList;		// global titledb urls
  private List auxPropUrlList;		// auxilliary prop files
  private List pluginTitledbUrlList;	// list of titledb urls (usually
					// jar:) specified by plugins
  private List userTitledbUrlList;	// titledb urls added from UI

  private List<String> loadedUrls = Collections.EMPTY_LIST;
  private List<String> specUrls = Collections.EMPTY_LIST;


  private String groupNames;		// daemon group names

  // Maps name of params holding included config URLs to the URL of the
  // file in which it was set, to facilitate relative URL resolution
  private Map<String,String> urlParamFile = new HashMap<String,String>();

  private String recentLoadError;

  // Platform config
  private static Configuration platformConfig =
    ConfigManager.EMPTY_CONFIGURATION;
  // Config of keystore used for loading configs.
  private Configuration platKeystoreConfig;

  // Current configuration instance.
  // Start with an empty one to avoid errors in the static accessors.
  private volatile Configuration currentConfig = EMPTY_CONFIGURATION;

  private OneShotSemaphore haveConfig = new OneShotSemaphore();

  private HandlerThread handlerThread; // reload handler thread

  private ConfigCache configCache;
  private volatile boolean needImmediateReload = false;
  private LockssUrlConnectionPool connPool = new LockssUrlConnectionPool();
  private LockssSecureSocketFactory secureSockFact;

  private long reloadInterval = 10 * Constants.MINUTE;
  private long sendVersionEvery = DEFAULT_SEND_VERSION_EVERY;
  private int maxDeferredAuBatchSize = DEFAULT_MAX_DEFERRED_AU_BATCH_SIZE;

  private List<Pattern> expertConfigAllowPats;
  private List<Pattern> expertConfigDenyPats;
  private boolean enableExpertConfig;

  public ConfigManager() {
    this(null, null);
  }

  public ConfigManager(List urls) {
    this(urls, null);
  }

  public ConfigManager(List urls, String groupNames) {
    if (urls != null) {
      configUrlList = new ArrayList(urls);
    }
    this.groupNames = groupNames;
    configCache = new ConfigCache(this);
    registerConfigurationCallback(Logger.getConfigCallback());
    registerConfigurationCallback(MiscConfig.getConfigCallback());
  }

  public ConfigCache getConfigCache() {
    return configCache;
  }

  LockssUrlConnectionPool getConnectionPool() {
    return connPool;
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
    configCache = null;
    stopHandler();
    haveConfig = new OneShotSemaphore();
  }

  public LockssApp getApp() {
    return theApp;
  }
  protected static ConfigManager theMgr;

  public static ConfigManager makeConfigManager() {
    theMgr = new ConfigManager();
    return theMgr;
  }

  public static ConfigManager makeConfigManager(List urls) {
    theMgr = new ConfigManager(urls);
    return theMgr;
  }

  public static ConfigManager makeConfigManager(List urls, String groupNames) {
    theMgr = new ConfigManager(urls, groupNames);
    return theMgr;
  }

  public static ConfigManager getConfigManager() {
    return theMgr;
  }

  /** Factory to create instance of appropriate class */
  public static Configuration newConfiguration() {
    return new ConfigurationPropTreeImpl();
  }

  Configuration initNewConfiguration() {
    Configuration newConfig = newConfiguration();

    // Add platform-like params before calling loadList() as they affect
    // conditional processing
    if (groupNames != null) {
      newConfig.put(PARAM_DAEMON_GROUPS, groupNames.toLowerCase());
    }
    return newConfig;
  }

  /** Return current configuration, or an empty configuration if there is
   * no current configuration. */
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
      if (props.getProperty(key) == null) {
	log.error(key + " has no value");
	throw new RuntimeException("no value for " + key);
      }
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

  public static void setPlatformConfig(Configuration config) {
    if (platformConfig.isEmpty()) {
      platformConfig = config;
    } else {
      throw
	new IllegalStateException("Can't override existing  platform config");
    }
  }

  private static PlatformVersion platVer = null;

  public static PlatformVersion getPlatformVersion() {
    if (platVer == null) {
      String ver = getPlatformConfig().get(PARAM_PLATFORM_VERSION);
      if (ver != null) {
	try {
	  platVer = new PlatformVersion(ver);
	} catch (RuntimeException e) {
	  log.warning("Illegal platform version: " + ver, e);
	}
      }
    }
    return platVer;
  }

  public static String getPlatformGroups() {
    return getPlatformConfig().get(PARAM_DAEMON_GROUPS, DEFAULT_DAEMON_GROUP);
  }

  public static List getPlatformGroupList() {
    return getPlatformConfig().getList(PARAM_DAEMON_GROUPS,
				       DEFAULT_DAEMON_GROUP_LIST);
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

  /** Return true if the first config load has completed. */
  public boolean haveConfig() {
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
    // It's tempting to do
    //     if (needImmediateReload) return;
    // here, as there's no point in running the callbacks yet if we're
    // going to do another config load immediately.  But that optimization
    // requires calculating diffs that encompass both loads.

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
  public Configuration readConfig(List urlList, String groupNames)
      throws IOException {
    if (urlList == null) {
      return null;
    }

    Configuration newConfig = initNewConfiguration();
    loadList(newConfig, getConfigGenerations(urlList, true, true, "props"));
    return newConfig;
  }

  String getLoadErrorMessage(ConfigFile cf) {
    if (cf != null) {
      StringBuffer sb = new StringBuffer();
      sb.append("Error loading: ");
      sb.append(cf.getFileUrl());
      sb.append("<br>");
      sb.append(HtmlUtil.htmlEncode(cf.getLoadErrorMessage()));
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

  /**
   * @return a List of the urls from which the config is loaded.
   */
  public List getConfigUrlList() {
    return configUrlList;
  }

  /**
   * @return the List of config urls, including auxilliary files (e.g.,
   * specified by {@value PARAM_TITLE_DB_URLS}).
   */
  public List getSpecUrlList() {
    return specUrls;
  }

  /**
   * @return the List of urls from which the config was actually loaded.
   * This differs from {@link #getSpecUrlList()} in that it reflects any
   * failover to local copies.
   */
  public List getLoadedUrlList() {
    return loadedUrls;
  }

  ConfigFile.Generation getConfigGeneration(String url, boolean required,
					    boolean reload, String msg)
      throws IOException {
    return getConfigGeneration(url, required, reload, msg, null);
  }

  ConfigFile.Generation getConfigGeneration(String url, boolean required,
					    boolean reload, String msg,
					    KeyPredicate keyPred)
      throws IOException {
    log.debug2("Loading " + msg + " from: " + url);
    return getConfigGeneration(configCache.find(url),
			       required, reload, msg, keyPred);
  }

  ConfigFile.Generation getConfigGeneration(ConfigFile cf, boolean required,
					    boolean reload, String msg)
      throws IOException {
    return getConfigGeneration(cf, required, reload, msg, null);
  }

  ConfigFile.Generation getConfigGeneration(ConfigFile cf, boolean required,
					    boolean reload, String msg,
					    KeyPredicate keyPred)
      throws IOException {
    try {
      cf.setConnectionPool(connPool);
      if (sendVersionInfo != null && "props".equals(msg)) {
	cf.setProperty(Constants.X_LOCKSS_INFO, sendVersionInfo);
      } else {
	cf.setProperty(Constants.X_LOCKSS_INFO, null);
      }
      if (reload) {
	cf.setNeedsReload();
      }
      if (keyPred != null) {
	cf.setKeyPredicate(keyPred);
      }
      ConfigFile.Generation gen = cf.getGeneration();
      return gen;
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
	  log.debug3("Unexpected error loading non-required file " + url, e);
	}
	return null;
      }
    }
  }

  public Configuration loadConfigFromFile(String url)
      throws IOException {
    ConfigFile cf = new FileConfigFile(url);
    ConfigFile.Generation gen = cf.getGeneration();
    return gen.getConfig();
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

  List getConfigGenerations(Collection urls, boolean required,
			    boolean reload, String msg)
      throws IOException {
    return getConfigGenerations(urls, required, reload, msg,
				trueKeyPredicate);
  }

  List getConfigGenerations(Collection urls, boolean required,
			    boolean reload, String msg,
			    KeyPredicate keyPred)
      throws IOException {

    if (urls == null) return Collections.EMPTY_LIST;
    List res = new ArrayList(urls.size());
    for (Object o : urls) {
      ConfigFile.Generation gen;
      if (o instanceof ConfigFile) {
	gen = getConfigGeneration((ConfigFile)o, required, reload, msg,
				  keyPred);
      } else if (o instanceof LocalFileDescr) {
	LocalFileDescr lfd = (LocalFileDescr)o;
	String filename = lfd.getFile().toString();
	Predicate includePred = lfd.getIncludePredicate();
	if (includePred != null && !includePred.evaluate(filename)) {
	  continue;
	}
	KeyPredicate pred = keyPred;
	if (lfd.getKeyPredicate() != null) {
	  pred = lfd.getKeyPredicate();
	}
	gen = getConfigGeneration(filename, required, reload, msg, pred);
      } else {
	String url = o.toString();
	gen = getConfigGeneration(url, required, reload, msg, keyPred);
      }
      if (gen != null) {
	res.add(gen);
      }
    }
    return res;
  }

  List getStandardConfigGenerations(List urls, boolean reload)
      throws IOException {
    List res = new ArrayList(20);

    List configGens = getConfigGenerations(urls, true, reload, "props");
    res.addAll(configGens);

    res.addAll(getConfigGenerations(auxPropUrlList, false, reload,
				    "auxilliary props"));
    res.addAll(getConfigGenerations(titledbUrlList, false, reload,
				    "global titledb", titleDbOnlyPred));
    res.addAll(getConfigGenerations(pluginTitledbUrlList, false, reload,
				    "plugin-bundled titledb",
				    titleDbOnlyPred));
    res.addAll(getConfigGenerations(userTitledbUrlList, false, reload,
				    "user title DBs", titleDbOnlyPred));
    initCacheConfig(configGens);
    res.addAll(getCacheConfigGenerations(reload));
    return res;
  }

  List getCacheConfigGenerations(boolean reload) throws IOException {
    List localGens = getConfigGenerations(getLocalFileDescrs(), false, reload,
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
    needImmediateReload = false;
    boolean res = updateConfigOnce(urls, true);
    if (res && needImmediateReload) {
      updateConfigOnce(urls, false);
    }
    if (res) {
      haveConfig.fill();
    }
    connPool.closeIdleConnections(0);
    updateRemoteConfigFailover();

    return res;
  }

  private String sendVersionInfo;
  private long lastSendVersion;
  private long startUpdateTime;
  private long lastUpdateTime;
  private long startCallbacksTime;

  public long getLastUpdateTime() {
    return lastUpdateTime;
  }

  public boolean updateConfigOnce(List urls, boolean reload) {
    startUpdateTime = TimeBase.nowMs();
    if (currentConfig.isEmpty()) {
      // first load preceded by platform config setup
      setupPlatformConfig(urls);
    }
    if (currentConfig.isEmpty() ||
	TimeBase.msSince(lastSendVersion) >= sendVersionEvery) {
      LockssApp app = getApp();
      sendVersionInfo = getVersionString();
      lastSendVersion = TimeBase.nowMs();
    } else {
      sendVersionInfo = null;
    }
    List gens;
    try {
      gens = getStandardConfigGenerations(urls, reload);
    } catch (SocketException | UnknownHostException | FileNotFoundException e) {
      log.error("Error loading config: " + e.toString());
//       recentLoadError = e.toString();
      return false;
    } catch (IOException e) {
      log.error("Error loading config", e);
//       recentLoadError = e.toString();
      return false;
    }

    if (!isChanged(gens)) {
      if (reloadInterval >= 10 * Constants.MINUTE) {
	log.info("Config up to date, not updated");
      }
      return false;
    }
    Configuration newConfig = initNewConfiguration();
    loadList(newConfig, gens);

    boolean did = installConfig(newConfig, gens);
    long tottime = TimeBase.msSince(startUpdateTime);
    long cbtime = TimeBase.msSince(startCallbacksTime);
    if (did) {
      lastUpdateTime = startUpdateTime;
    }
    if (log.isDebug2() || tottime > Constants.SECOND) {
      if (did) {
	log.debug("Reload time: "
		  + StringUtil.timeIntervalToString(tottime - cbtime)
		  + ", cb time: " + StringUtil.timeIntervalToString(cbtime));
      } else {
	log.debug("Reload time: " + StringUtil.timeIntervalToString(tottime));
      }
    }
    return did;
  }

  Properties getVersionProps() {
    Properties p = new Properties();
    PlatformVersion pver = getPlatformVersion();
    if (pver != null) {
      putIf(p, "platform", pver.displayString());
    }
    DaemonVersion dver = getDaemonVersion();
    if (dver != null) {
      putIf(p, "daemon", dver.displayString());
    } else {
      putIf(p, "built",
	    BuildInfo.getBuildProperty(BuildInfo.BUILD_TIMESTAMP));
      putIf(p, "built_on", BuildInfo.getBuildProperty(BuildInfo.BUILD_HOST));
    }
    putIf(p, "groups",
	  StringUtil.separatedString(getPlatformGroupList(), ";"));
    putIf(p, "host", getPlatformHostname());
    putIf(p, "peerid",
	  currentConfig.get(IdentityManager.PARAM_LOCAL_V3_IDENTITY));
    return p;
  }

  void putIf(Properties p, String key, String val) {
    if (val != null) {
      p.put(key, val);
    }
  }

  String getVersionString() {
    StringBuilder sb = new StringBuilder();
    for (Iterator iter = getVersionProps().entrySet().iterator();
	 iter.hasNext(); ) {
      Map.Entry ent = (Map.Entry)iter.next();
      sb.append(ent.getKey());
      sb.append("=");
      sb.append(StringUtil.ckvEscape((String)ent.getValue()));
      if (iter.hasNext()) {
	sb.append(",");
      }
    }
    return sb.toString();
  }

  void loadList(Configuration intoConfig,
		Collection<ConfigFile.Generation> gens) {
    for (ConfigFile.Generation gen : gens) {
      if (gen != null) {
	// Remember the URL of the file in which any parameter whose value
	// might be a (list of) relative URL(s) is found
	final String url = gen.getUrl();
	Configuration.ParamCopyEvent pse = null;
	if (UrlUtil.isUrl(url)) {
	  pse = new Configuration.ParamCopyEvent() {
	      public void paramCopied(String name,
				      String val){
		if (URL_PARAMS.contains(name)) {
		  urlParamFile.put(name, url);
		}
	      }};
	    }
	intoConfig.copyFrom(gen.getConfig(), pse);
      }
    }
  }

  void setupPlatformConfig(List urls) {
    Configuration platConfig = initNewConfiguration();
    for (Iterator iter = urls.iterator(); iter.hasNext();) {
      Object o = iter.next();
      ConfigFile cf;
      if (o instanceof ConfigFile) {
	cf = (ConfigFile)o;
      } else {
	cf = configCache.find(o.toString());
      }
      if (cf.isPlatformFile()) {
	try {
	  if (log.isDebug3()) {
	    log.debug3("Loading platform file: " + cf);
	  }
	  cf.setNeedsReload();
	  platConfig.load(cf);
	} catch (IOException e) {
	  log.warning("Couldn't preload platform file " + cf.getFileUrl(), e);
	}
      }
    }
    // init props keystore before sealing, as it may add to the config
    initSocketFactory(platConfig);

    // do this even if no local.txt, to ensure platform-like params (e.g.,
    // group) in initial config get into platformConfig even during testing.
    platConfig.seal();
    platformConfig = platConfig;
    initCacheConfig(platConfig);
    setUpRemoteConfigFailover();
  }

  // If a keystore was specified for 
  void initSocketFactory(Configuration platconf) {
    String serverAuthKeystore =
      platconf.getNonEmpty(PARAM_SERVER_AUTH_KEYSTORE_NAME);
    if (serverAuthKeystore != null) {
      platKeystoreConfig = newConfiguration();
      String resource = builtinServerAuthKeystores.get(serverAuthKeystore);
      if (resource != null) {
	// Set up keystore params to point to internal keystore resource.
	String pref = keystorePref(serverAuthKeystore);
	platKeystoreConfig.put(pref + LockssKeyStoreManager.KEYSTORE_PARAM_NAME,
			       serverAuthKeystore);
	platKeystoreConfig.put(pref + LockssKeyStoreManager.KEYSTORE_PARAM_RESOURCE,
			       resource);
      } else {
	// if props keystore name isn't builtin, it's a filename.  Set up
	// keystore params to point to it.
	String ksname = "propserver";
	String pref = keystorePref(ksname);
	platKeystoreConfig.put(pref + LockssKeyStoreManager.KEYSTORE_PARAM_NAME,
			       ksname);
	platKeystoreConfig.put(pref + LockssKeyStoreManager.KEYSTORE_PARAM_FILE,
			       serverAuthKeystore);
	serverAuthKeystore = ksname;
      }
      platconf.copyFrom(platKeystoreConfig);
    }
    String clientAuthKeystore =
      platconf.getNonEmpty(PARAM_CLIENT_AUTH_KEYSTORE_NAME);
    if (serverAuthKeystore != null || clientAuthKeystore != null) {
      log.debug("initSocketFactory: " + serverAuthKeystore +
		", " + clientAuthKeystore);
      secureSockFact = new LockssSecureSocketFactory(serverAuthKeystore,
						     clientAuthKeystore);
    }
  }

  LockssSecureSocketFactory getSecureSocketFactory() {
    return secureSockFact;
  }

  String keystorePref(String name) {
    return LockssKeyStoreManager.PARAM_KEYSTORE
      + "." + StringUtil.sanitizeToIdentifier(name.toLowerCase()) + ".";
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
    setConfigMacros(newConfig);
    setCompatibilityParams(newConfig);
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
    // XXX for test utils.  ick
    initCacheConfig(newConfig);
    setCurrentConfig(newConfig);
    updateGenerations(gens);
    recordConfigLoaded(newConfig, oldConfig, diffs, gens);
    startCallbacksTime = TimeBase.nowMs();
    runCallbacks(newConfig, oldConfig, diffs);
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

    if (changedKeys.contains(MYPREFIX)) {
      reloadInterval = config.getTimeInterval(PARAM_RELOAD_INTERVAL,
					      DEFAULT_RELOAD_INTERVAL);
      sendVersionEvery = config.getTimeInterval(PARAM_SEND_VERSION_EVERY,
						DEFAULT_SEND_VERSION_EVERY);
      maxDeferredAuBatchSize =
	config.getInt(PARAM_MAX_DEFERRED_AU_BATCH_SIZE,
		      DEFAULT_MAX_DEFERRED_AU_BATCH_SIZE);
    }

    if (changedKeys.contains(PARAM_PLATFORM_VERSION)) {
      platVer = null;
    }

    // check for presence of title db url keys on first load, as
    // changedKeys.containsKey() is true of all keys then and would lead to
    // always reloading the first time.
    if (oldConfig.isEmpty()
	? (config.containsKey(PARAM_USER_TITLE_DB_URLS)
	   || config.containsKey(PARAM_TITLE_DB_URLS)
	   || config.containsKey(PARAM_AUX_PROP_URLS))
	: (changedKeys.contains(PARAM_USER_TITLE_DB_URLS)
	   || changedKeys.contains(PARAM_TITLE_DB_URLS)
	   || changedKeys.contains(PARAM_AUX_PROP_URLS))) {
      userTitledbUrlList =
	resolveConfigUrls(config, PARAM_USER_TITLE_DB_URLS);
      titledbUrlList = resolveConfigUrls(config, PARAM_TITLE_DB_URLS);
      auxPropUrlList = resolveConfigUrls(config, PARAM_AUX_PROP_URLS);
      log.debug("titledbUrlList: " + titledbUrlList +
		", userTitledbUrlList: " + userTitledbUrlList +
		", auxPropUrlList: " + auxPropUrlList);
      // Currently this requires a(nother immediate) reload.
      needImmediateReload = true;
    }
  }

  List<String> resolveConfigUrls(Configuration config, String param) {
    List<String> urls = config.getList(param);
    if (urls.isEmpty()) {
      return urls;
    }
    String base = urlParamFile.get(param);
    if (base != null) {
      ArrayList res = new ArrayList(urls.size());
      for (String url : urls) {
	res.add(resolveConfigUrl(base, url));
      }
      return res;
    } else {
      log.error("URL param has no base URL: " + param);
      return urls;
    }
  }

  String resolveConfigUrl(String base, String configUrl) {
    try {
      return UrlUtil.resolveUri(base, configUrl);
    } catch (MalformedURLException e) {
      log.error("Malformed props base URL: " + base + ", rel: " + configUrl, e);
      return configUrl;
    }
  }

  private void buildLoadedFileLists(List<ConfigFile.Generation> gens) {
    if (gens != null && !gens.isEmpty()) {
      List<String> specNames = new ArrayList<String>(gens.size());
      List<String> loadedNames = new ArrayList<String>(gens.size());
      for (ConfigFile.Generation gen : gens) {
	if (gen != null) {
	  loadedNames.add(gen.getConfigFile().getLoadedUrl());
	  specNames.add(gen.getConfigFile().getFileUrl());
	}
      }
      loadedUrls = loadedNames;
      specUrls = specNames;
    } else {
      loadedUrls = Collections.EMPTY_LIST;
      specUrls = Collections.EMPTY_LIST;
    }
  }

  private void recordConfigLoaded(Configuration newConfig,
				  Configuration oldConfig,
				  Configuration.Differences diffs,
				  List gens) {
    buildLoadedFileLists(gens);    
    logConfigLoaded(newConfig, oldConfig, diffs, loadedUrls);
  }

  

  private void logConfigLoaded(Configuration newConfig,
			       Configuration oldConfig,
			       Configuration.Differences diffs,
			       List<String> names) {
    StringBuffer sb = new StringBuffer("Config updated, ");
    sb.append(newConfig.keySet().size());
    sb.append(" keys");
    if (!names.isEmpty()) {
      sb.append(" from ");
      sb.append(StringUtil.separatedString(names, ", "));
    }
    log.info(sb.toString());
    if (log.isDebug()) {
      logConfig(newConfig, oldConfig, diffs);
    } else {
      log.info("New TdbAus: " + diffs.getTdbAuDifferenceCount());
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

    // If we were given a temp dir, create a subdir and use that.  This
    // ensures that * expansion in rundaemon won't exceed the maximum
    // command length.

    String tmpdir = config.get(PARAM_TMPDIR);
    if (!StringUtil.isNullString(tmpdir)) {
      File javaTmpDir = new File(tmpdir, "dtmp");
      if (FileUtil.ensureDirExists(javaTmpDir)) {
	FileUtil.setOwnerRWX(javaTmpDir);
	System.setProperty("java.io.tmpdir", javaTmpDir.toString());
      } else {
	log.warning("Can't create/access temp dir: " + javaTmpDir +
		    ", using default: " + System.getProperty("java.io.tmpdir"));
      }
    }
    System.setProperty("jsse.enableSNIExtension",
		       Boolean.toString(config.getBoolean(PARAM_JSSE_ENABLESNIEXTENSION,
							  DEFAULT_JSSE_ENABLESNIEXTENSION)));
    String fromParam = LockssDaemon.PARAM_BIND_ADDRS;
    setIfNotSet(config, fromParam, AdminServletManager.PARAM_BIND_ADDRS);
    setIfNotSet(config, fromParam, ContentServletManager.PARAM_BIND_ADDRS);
    setIfNotSet(config, fromParam, ProxyManager.PARAM_BIND_ADDRS);
    setIfNotSet(config, fromParam, AuditProxyManager.PARAM_BIND_ADDRS);
//     setIfNotSet(config, fromParam, IcpManager.PARAM_ICP_BIND_ADDRS);

    org.lockss.poller.PollManager.processConfigMacros(config);
  }

  // Backward compatibility for param settings

  /** Obsolete, use org.lockss.ui.contactEmail (daemon 1.32) */
  static final String PARAM_OBS_ADMIN_CONTACT_EMAIL =
    "org.lockss.admin.contactEmail";
  /** Obsolete, use org.lockss.ui.helpUrl (daemon 1.32) */
  static final String PARAM_OBS_ADMIN_HELP_URL = "org.lockss.admin.helpUrl";

  private void setIfNotSet(Configuration config,
			   String fromKey, String toKey) {
    if (config.containsKey(fromKey) && !config.containsKey(toKey)) {
      config.put(toKey, config.get(fromKey));
    }
  }

  private void setCompatibilityParams(Configuration config) {
    setIfNotSet(config,
		PARAM_OBS_ADMIN_CONTACT_EMAIL,
		AdminServletManager.PARAM_CONTACT_ADDR);
    setIfNotSet(config,
		PARAM_OBS_ADMIN_HELP_URL,
		AdminServletManager.PARAM_HELP_URL);
    setIfNotSet(config,
		RemoteApi.PARAM_BACKUP_EMAIL_FREQ,
		RemoteApi.PARAM_BACKUP_FREQ);
  }

  private void setConfigMacros(Configuration config) {
    String acctPolicy = config.get(AccountManager.PARAM_POLICY,
				   AccountManager.DEFAULT_POLICY);
    if ("lc".equalsIgnoreCase(acctPolicy)) {
      setParamsFromPairs(config, AccountManager.POLICY_LC);
    }
    if ("ssl".equalsIgnoreCase(acctPolicy)) {
      setParamsFromPairs(config, AccountManager.POLICY_SSL);
    }
    if ("form".equalsIgnoreCase(acctPolicy)) {
      setParamsFromPairs(config, AccountManager.POLICY_FORM);
    }
    if ("basic".equalsIgnoreCase(acctPolicy)) {
      setParamsFromPairs(config, AccountManager.POLICY_BASIC);
    }
    if ("compat".equalsIgnoreCase(acctPolicy)) {
      setParamsFromPairs(config, AccountManager.POLICY_COMPAT);
    }
  }

  private void setParamsFromPairs(Configuration config, String[] pairs) {
    for (int ix = 0; ix < pairs.length; ix += 2) {
      config.put(pairs[ix], pairs[ix + 1]);
    }
  }

  private void copyPlatformParams(Configuration config) {
    copyPlatformVersionParams(config);
    if (platKeystoreConfig != null) {
      config.copyFrom(platKeystoreConfig);
    }
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
			 AdminServletManager.PARAM_IP_INCLUDE,
			 AdminServletManager.PARAM_IP_PLATFORM_SUBNET,
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
      platformDefault(config,
		      org.lockss.truezip.TrueZipManager.PARAM_CACHE_DIR,
		      new File(firstSpace, "tfile").toString());
      platformDefault(config,
		      RemoteApi.PARAM_BACKUP_DIR,
		      new File(firstSpace, "backup").toString());
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

  /** Override current config value with val */
  private void platformOverride(Configuration config, String key, String val) {
    String oldval = config.get(key);
    if (oldval != null && !StringUtil.equalStrings(oldval, val)) {
      log.warning("Overriding param: " + key + "= " + oldval);
      log.warning("with platform-derived value: " + val);
    }
    config.put(key, val);
  }

  /** Store val in config iff key currently not set */
  private void platformDefault(Configuration config, String key, String val) {
    if (!config.containsKey(key)) {
      platformOverride(config, key, val);
    }
  }

  /** Copy value of platformKey in config iff key currently not set */
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
    int maxLogValLen = config.getInt(PARAM_MAX_LOG_VAL_LEN,
				     DEFAULT_MAX_LOG_VAL_LEN);
    Set<String> diffSet = diffs.getDifferenceSet();
    SortedSet<String> keys = new TreeSet<String>(diffSet);
    int elided = 0;
    int numDiffs = keys.size();
    // keys includes param name prefixes that aren't actual params, so
    // numDiffs is inflated by several.
    for (String key : keys) {
      if (numDiffs <= 40 || log.isDebug3() || shouldParamBeLogged(key)) {
	if (config.containsKey(key)) {
	  String val = config.get(key);
	  log.debug("  " +key + " = " + StringUtils.abbreviate(val, maxLogValLen));
	} else if (oldConfig.containsKey(key)) {
	  log.debug("  " + key + " (removed)");
	}
      } else {
	elided++;
      }
    }
    if (elided > 0) log.debug(elided + " keys elided");
    log.debug("New TdbAus: " + diffs.getTdbAuDifferenceCount());
    if (log.isDebug3()) {
      log.debug3("TdbDiffs: " + diffs.getTdbDifferences());
    }

    if (log.isDebug2()) {
      Tdb tdb = config.getTdb();
      if (tdb != null) {
	log.debug2(StringPool.AU_CONFIG_PROPS.toStats());

	Histogram hist1 = new Histogram(15);
	Histogram hist2 = new Histogram(15);
	Histogram hist3 = new Histogram(15);

	for (TdbAu.Id id : tdb.getAllTdbAuIds()) {
	  TdbAu tau = id.getTdbAu();
	  hist1.addDataPoint(tau.getParams().size());
	  hist2.addDataPoint(tau.getAttrs().size());
	  hist3.addDataPoint(tau.getProperties().size());
	}
	logHist("Tdb Params", hist1);
	logHist("Tdb Attrs", hist2);
	logHist("Tdb Props", hist3);
      }
    }
  }

  private void logHist(String name, Histogram hist) {
    int[] freqs = hist.getFreqs();
    log.debug2(name + " histogram");
    log.debug2("size  number");
    for (int ix = 0; ix <= hist.getMax(); ix++) {
      log.debug(String.format("%2d   %6d", ix, freqs[ix]));
    }
  }

  public static boolean shouldParamBeLogged(String key) {
    return !(key.startsWith(PREFIX_TITLE_DB)
 	     || key.startsWith(PREFIX_TITLE_SETS_DOT)
  	     || key.startsWith(PluginManager.PARAM_AU_TREE + ".")
  	     || StringUtils.endsWithIgnoreCase(key, "password"));
  }

  /**
   * Add a collection of bundled titledb config jar URLs to
   * the pluginTitledbUrlList.
   */
  public void addTitleDbConfigFrom(Collection classloaders) {
    boolean needReload = false;

    for (Iterator it = classloaders.iterator(); it.hasNext(); ) {
      ClassLoader cl = (ClassLoader)it.next();
      URL titleDbUrl = cl.getResource(CONFIG_FILE_BUNDLED_TITLE_DB);
      if (titleDbUrl != null) {
	if (pluginTitledbUrlList == null) {
	  pluginTitledbUrlList = new ArrayList();
	}
	pluginTitledbUrlList.add(titleDbUrl);
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
    requestReloadIn(0);
  }

  public void requestReloadIn(long millis) {
    if (handlerThread != null) {
      handlerThread.forceReloadIn(millis);
    }
  }

  /**
   * Register a {@link Configuration.Callback}, which will be called
   * whenever the current configuration has changed.  If a configuration is
   * present when a callback is registered, the callback will be called
   * immediately.
   * @param c <code>Configuration.Callback</code> to add.  */
  public void registerConfigurationCallback(Configuration.Callback c) {
    log.debug2("registering " + c);
    if (!configChangedCallbacks.contains(c)) {
      configChangedCallbacks.add(c);
      if (!currentConfig.isEmpty()) {
	runCallback(c, currentConfig, ConfigManager.EMPTY_CONFIGURATION,
		    currentConfig.differences(null));  // all differences
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

  List<Pattern> compilePatternList(List<String> patterns)
      throws MalformedPatternException {
    if (patterns == null) {
      return Collections.EMPTY_LIST;
    }
    int flags = Perl5Compiler.READ_ONLY_MASK;
    List<Pattern> res = new ArrayList<Pattern>(patterns.size());

    for (String pat : patterns) {
      res.add(RegexpUtil.getCompiler().compile(pat, flags));
    }
    return res;
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

  private List getListFromGenerations(List configGenerations, String param,
				      List dfault) {
    for (Iterator iter = configGenerations.iterator(); iter.hasNext(); ) {
      ConfigFile.Generation gen = (ConfigFile.Generation)iter.next();
      if (gen != null) {
	Configuration config = gen.getConfig();
	if (config.containsKey(param)) {
	  return config.getList(param);
	}
      }
    }
    return dfault;
  }

  private void initCacheConfig(String dspace, String relConfigPath) {
    if (cacheConfigInited) return;
    Vector v = StringUtil.breakAt(dspace, ';');
    if (v.size() == 0) {
      log.error(PARAM_PLATFORM_DISK_SPACE_LIST +
		" not specified, not configuring local cache config dir");
      return;
    }
    cacheConfigDir = findRelDataDir(v, relConfigPath, true);
    cacheConfigInited = true;
  }

  private void initCacheConfig(List configGenerations) {
    if (!cacheConfigInited || isChanged(configGenerations)) {
      List<String> expertAllow =
	getListFromGenerations(configGenerations,
			       PARAM_EXPERT_ALLOW, DEFAULT_EXPERT_ALLOW);
      List<String> expertDeny =
	getListFromGenerations(configGenerations,
			       PARAM_EXPERT_DENY, DEFAULT_EXPERT_DENY);
      processExpertAllowDeny(expertAllow, expertDeny);
    }
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
    List<String> expertAllow =
      newConfig.getList(PARAM_EXPERT_ALLOW, DEFAULT_EXPERT_ALLOW);
    List<String> expertDeny =
      newConfig.getList(PARAM_EXPERT_DENY, DEFAULT_EXPERT_DENY);
    processExpertAllowDeny(expertAllow, expertDeny);

    if (cacheConfigInited) return;
    String dspace = newConfig.get(PARAM_PLATFORM_DISK_SPACE_LIST);
    String relConfigPath = newConfig.get(PARAM_CONFIG_PATH,
					 DEFAULT_CONFIG_PATH);
    initCacheConfig(dspace, relConfigPath);
  }

  private void processExpertAllowDeny(List<String> expertAllow,
				      List<String> expertDeny) {
    log.debug("processExpertAllowDeny("+expertAllow+", "+expertDeny+")");
    try {
      expertConfigAllowPats = compilePatternList(expertAllow);
      expertConfigDenyPats =  compilePatternList(expertDeny);
      enableExpertConfig = true;
    } catch (MalformedPatternException e) {
      log.error("Expert config allow/deny error", e);
      enableExpertConfig = false;
    }
  }

  /**
   * Find or create a directory specified by a config param and default.
   * If value (the param value or default) is an absolute path, that
   * directory is returned (and created if necessary).  If the value is
   * relative, it specifies a directory relative to (one of) the paths on
   * {@value #PARAM_PLATFORM_DISK_SPACE_LIST}.  If one of these directories
   * exists, it (the first one) is returned, else the directory is created
   * relative to the first element of {@value
   * #PARAM_PLATFORM_DISK_SPACE_LIST}
   * @param dataDirParam Name of config param whose value specifies the
   * absolute or relative path of the directory
   * @param dataDirDefault Default absolute or relative path of the
   * directory
   * @return A File object representing the requested directory.
   * @throws RuntimeException if neither the config param nor the default
   * have a non-empty value, or (for relative paths) if {@value
   * #PARAM_PLATFORM_DISK_SPACE_LIST} is not set, or if a directory can't
   * be created.
   */
  public File findConfiguredDataDir(String dataDirParam,
				    String dataDirDefault) {
    return findConfiguredDataDir(dataDirParam, dataDirDefault, true);
  }

  /**
   * Find or create a directory specified by a config param and default.
   * If value (the param value or default) is an absolute path, that
   * directory is returned (and created if necessary).  If the value is
   * relative, it specifies a directory relative to (one of) the paths on
   * {@value #PARAM_PLATFORM_DISK_SPACE_LIST}.  If one of these directories
   * exists, it (the first one) is returned, else the directory is created
   * relative to the first element of {@value
   * #PARAM_PLATFORM_DISK_SPACE_LIST}
   * @param dataDirParam Name of config param whose value specifies the
   * absolute or relative path of the directory
   * @param dataDirDefault Default absolute or relative path of the
   * directory
   * @param create If false and the directory doesn't already exist, the
   * path to where it would be created is returned, but it is not actually
   * created.
   * @return A File object representing the requested directory.
   * @throws RuntimeException if neither the config param nor the default
   * have a non-empty value, or (for relative paths) if {@value
   * #PARAM_PLATFORM_DISK_SPACE_LIST} is not set, or if a directory can't
   * be created.
   */
  public File findConfiguredDataDir(String dataDirParam,
				    String dataDirDefault,
				    boolean create) {
    String dataDirName = getCurrentConfig().get(dataDirParam, dataDirDefault);
    if (StringUtil.isNullString(dataDirName)) {
      throw new RuntimeException("No value or default for " + dataDirParam);
    }
    File dir = new File(dataDirName);
    if (dir.isAbsolute()) {
      if (FileUtil.ensureDirExists(dir)) {
	return dir;
      } else {
	throw new RuntimeException("Could not create data dir: " + dir);
      }
    } else {
      return findRelDataDir(dataDirName, create);
    }
  }

  /**
   * Find or create a directory relative to a path on the platform disk
   * space list.  If not found, it is created under the first element on
   * the platform disk space list.
   *
   * @param relPath Relative pathname of the directory to find or create.
   * @return A File object representing the requested directory.
   * @throws RuntimeException if {@value #PARAM_PLATFORM_DISK_SPACE_LIST}
   * is not set, or if a directory can't be created.
   */
  public File findRelDataDir(String relPath, boolean create) {
    List<String> diskPaths =
      getCurrentConfig().getList(PARAM_PLATFORM_DISK_SPACE_LIST);
    return findRelDataDir(diskPaths, relPath, create);
  }

  private File findRelDataDir(List<String> diskPaths, String relPath,
			      boolean create) {
    if (diskPaths.size() == 0) {
      throw new RuntimeException("No platform disks specified. " +
				 PARAM_PLATFORM_DISK_SPACE_LIST +
				 " must be set.");
    }
    File best = null;
    for (String path : diskPaths) {
      File candidate = new File(path, relPath);
      if (candidate.exists()) {
	if (best == null) {
	best = candidate;
	} else {
	  log.warning("Duplicate data dir found: " +
		      candidate + ", using " + best);
	}
      }
    }
    if (best != null) {
      return best;
    }
    File newDir = new File(diskPaths.get(0), relPath);
    if (create) {
      if (!FileUtil.ensureDirExists(newDir)) {
	throw new RuntimeException("Could not create data dir: " + newDir);
      }
    }
    return newDir;
  }

  /** Return a map from local config file name (sans dir) to its
   * descriptor. */
  public LinkedHashMap<String,LocalFileDescr> getLocalFileDescrMap() {
    if (cacheConfigFileMap == null) {
      LinkedHashMap<String,LocalFileDescr> res =
	new LinkedHashMap<String,LocalFileDescr>();
      for (LocalFileDescr ccf: cacheConfigFiles) {
 	ccf.setFile(new File(cacheConfigDir, ccf.getName()));
	res.put(ccf.getName(), ccf);
      }
      cacheConfigFileMap = res;
    }
    return cacheConfigFileMap;
  }

  /** Return the list of cache config file decrs, in same order in which
   * they were declared. */
  public Collection<LocalFileDescr> getLocalFileDescrs() {
    return getLocalFileDescrMap().values();
  }

  /** Return the LocalFileDescr the named cache config file, or null if
   * none. */
  public LocalFileDescr getLocalFileDescr(String cacheConfigFileName) {
    return getLocalFileDescrMap().get(cacheConfigFileName);
  }

  /** Return a File for the named cache config file */
  public File getCacheConfigFile(String cacheConfigFileName) {
    return new File(cacheConfigDir, cacheConfigFileName);
  }

  /** Return the cache config dir */
  public File getCacheConfigDir() {
    return cacheConfigDir;
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
      throw new IOException("No cache config dir");
    }

    String cfile = new File(cacheConfigDir, cacheConfigFileName).toString();
    ConfigFile cf = configCache.find(cfile);
    Configuration res = cf.getConfiguration();
    return res;
  }

  private boolean didWarnNoAuConfig = false;

  /**
   * Return the contents of the local AU config file.
   * @return the Configuration from the AU config file, or an empty config
   * if no config file found
   */
  public Configuration readAuConfigFile() {
    Configuration auConfig;
    try {
      auConfig = readCacheConfigFile(CONFIG_FILE_AU_CONFIG);
      didWarnNoAuConfig = false;
    } catch (IOException e) {
      if (!didWarnNoAuConfig) {
	log.warning("Couldn't read AU config file: " + e.getMessage());
	didWarnNoAuConfig = true;
      }
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
    writeCacheConfigFile(config, cacheConfigFileName, header, false);
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
						boolean suppressReload)
      throws IOException {
    if (cacheConfigDir == null) {
      log.warning("Attempting to write cache config file: " +
		  cacheConfigFileName + ", but no cache config dir exists");
      throw new RuntimeException("No cache config dir");
    }
    // Write to a temp file and rename
    File tempfile = FileUtil.createTempFile("tmp_config", ".tmp", cacheConfigDir);
    OutputStream os = new FileOutputStream(tempfile);
    // Add fileversion iff it's not already there.
    Properties addtl = null;
    String verProp = configVersionProp(cacheConfigFileName);
    String verVal = "1";
    if (!verVal.equals(config.get(verProp))) {
      addtl = new Properties();
      addtl.put(verProp, verVal);
    }
    config.store(os, header, addtl);
    os.close();
    File cfile = new File(cacheConfigDir, cacheConfigFileName);
    if (!PlatformUtil.updateAtomically(tempfile, cfile)) {
      throw new RuntimeException("Couldn't rename temp file: " +
				 tempfile + " to: " + cfile);
    }
    log.debug2("Wrote cache config file: " + cfile);
    ConfigFile cf = configCache.get(cfile.toString());
    if (cf instanceof FileConfigFile) {
      ((FileConfigFile)cf).storedConfig(config);
    } else {
      log.warning("Not a FileConfigFile: " + cf);
    }
    LocalFileDescr descr = getLocalFileDescr(cacheConfigFileName);
    if (!suppressReload) {
      if (descr == null || descr.isNeedReloadAfterWrite()) {
	requestReload();
      }
    }
  }

  /** Write the named local cache config file into the previously determined
   * cache config directory.
   * @param config Configuration with properties to write
   * @param cacheConfigFileName filename, no path
   * @param header file header string
   */
  public synchronized void writeCacheConfigFile(String text,
						String cacheConfigFileName,
						boolean suppressReload)
      throws IOException {
    if (cacheConfigDir == null) {
      log.warning("Attempting to write cache config file: " +
		  cacheConfigFileName + ", but no cache config dir exists");
      throw new RuntimeException("No cache config dir");
    }
    // Write to a temp file and rename
    File tempfile = FileUtil.createTempFile("tmp_config", ".tmp", cacheConfigDir);
    StringUtil.toFile(tempfile, text);
    File cfile = new File(cacheConfigDir, cacheConfigFileName);
    if (!PlatformUtil.updateAtomically(tempfile, cfile)) {
      throw new RuntimeException("Couldn't rename temp file: " +
				 tempfile + " to: " + cfile);
    }
    log.debug2("Wrote cache config file: " + cfile);
    ConfigFile cf = configCache.get(cfile.toString());
    if (!suppressReload) {
      requestReload();
    }
  }

  /** Delete the named local cache config file from the cache config
   * directory */
  public synchronized boolean deleteCacheConfigFile(String cacheConfigFileName)
      throws IOException {
    if (cacheConfigDir == null) {
      log.warning("Attempting to write delete config file: " +
		  cacheConfigFileName + ", but no cache config dir exists");
      throw new RuntimeException("No cache config dir");
    }
    File cfile = new File(cacheConfigDir, cacheConfigFileName);
    return cfile.delete();
  }

  /** Return the config version prop key for the named config file */
  public static String configVersionProp(String cacheConfigFileName) {
    String noExt = StringUtil.upToFinal(cacheConfigFileName, ".");
    return StringUtil.replaceString(PARAM_CONFIG_FILE_VERSION,
				    "<filename>", noExt);
  }

  /* Support for batching changes to au.txt, and to prevent a config
   * reload from being triggered each time the AU config file is rewritten.
   * Clients who call startAuBatch() <b>must</b> call finishAuBatch() when
   * done (in a <code>finally</code>), then call requestReload() if
   * appropriate */

  private int auBatchDepth = 0;
  private Configuration deferredAuConfig;
  private List<String> deferredAuDeleteKeys;
  private int deferredAuBatchSize;

  /** Called before a batch of calls to {@link
   * #updateAuConfigFile(Properties, String)} or {@link
   * #updateAuConfigFile(Configuration, String)}, causes updates to be
   * accumulated in memory, up to a maximum of {@link
   * #PARAM_MAX_DEFERRED_AU_BATCH_SIZE}, before they are all written to
   * disk.  {@link #finishAuBatch()} <b>MUST</b> be called at the end of
   * the batch, to ensure the final batch is written.  All removals
   * (<code>auPropKey</code> arg to updateAuConfigFile) in a batch are
   * performed before any additions, so the result of the same sequence of
   * updates in batched and non-batched mode is not necessarily equivalent.
   * It is guaranteed to be so if no AU is updated more than once in the
   * batch.  <br>This speeds up batch AU addition/deletion by a couple
   * orders of magnitude, which will suffice until the AU config is moved
   * to a database.
   */
  public synchronized void startAuBatch() {
    auBatchDepth++;
  }

  public synchronized void finishAuBatch() throws IOException {
    executeDeferredAuBatch();
    if (--auBatchDepth < 0) {
      log.warning("auBatchDepth want negative, resetting to zero",
		  new Throwable("Marker"));
      auBatchDepth = 0;
    }
  }

  private void executeDeferredAuBatch() throws IOException {
    if (deferredAuConfig != null &&
	(!deferredAuConfig.isEmpty() || !deferredAuDeleteKeys.isEmpty())) {
      updateAuConfigFile(deferredAuConfig, deferredAuDeleteKeys);
      deferredAuConfig = null;
      deferredAuDeleteKeys = null;
      deferredAuBatchSize = 0;
    }
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
  public synchronized void updateAuConfigFile(Configuration auConfig,
					      String auPropKey)
      throws IOException {
    if (auBatchDepth > 0) {
      if (deferredAuConfig == null) {
	deferredAuConfig = newConfiguration();
	deferredAuDeleteKeys = new ArrayList<String>();
	deferredAuBatchSize = 0;
      }
      deferredAuConfig.copyFrom(auConfig);
      if (auPropKey != null) {
	deferredAuDeleteKeys.add(auPropKey);
      }
      if (++deferredAuBatchSize >= maxDeferredAuBatchSize) {
	executeDeferredAuBatch();
      }
    } else {
      updateAuConfigFile(auConfig,
			 auPropKey == null ? null : ListUtil.list(auPropKey));
    }
  }

  /** Replace one or more AUs' config keys in the local AU config file.
   * @param auConfig new config for the AUs
   * @param auPropKeys list of au subtree roots to remove
   */
  private void updateAuConfigFile(Configuration auConfig,
				  List<String> auPropKeys)
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
    // first remove all existing values for the AUs
    if (auPropKeys != null) {
      for (String key : auPropKeys) {
	fileConfig.removeConfigTree(key);
      }
    }
    // then add the new config
    for (Iterator iter = auConfig.keySet().iterator(); iter.hasNext();) {
      String key = (String)iter.next();
      fileConfig.put(key, auConfig.get(key));
    }
    // seal it so FileConfigFile.storedConfig() won't have to make a copy
    fileConfig.seal();
    writeCacheConfigFile(fileConfig, CONFIG_FILE_AU_CONFIG,
			 "AU Configuration", auBatchDepth > 0);
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

  // Remote config failover mechanism maintain local copy of remote config
  // files, uses them on daemon startup if origin file not available

  File remoteConfigFailoverDir;			// Copies written to this dir
  File remoteConfigFailoverInfoFile;		// State file
  RemoteConfigFailoverMap rcfm;
  long remoteConfigFailoverMaxAge = DEFAULT_REMOTE_CONFIG_FAILOVER_MAX_AGE;

  /** Records state of one config failover file */
  static class RemoteConfigFailoverInfo implements LockssSerializable {
    final String url;
    String filename;
    String chksum;
    long date;
    transient File dir;
    transient File tempfile;
    transient int seq;

    RemoteConfigFailoverInfo(String url, File dir, int seq) {
      this.dir = dir;
      this.url = url;
      this.seq = seq;
    }

    void setRemoteConfigFailoverDir(File dir) {
      this.dir = dir;
    }

    String getChksum() {
      return chksum;
    }

    void setChksum(String chk) {
      this.chksum = chk;
    }

    String getUrl() {
      return url;
    }

    String getFilename() {
      return filename;
    }

    boolean exists() {
      return filename != null && getPermFileAbs().exists();
    }

    File getTempFile() {
      return tempfile;
    }

    void setTempFile(File tempfile) {
      this.tempfile = tempfile;
    }

    boolean update() {
      if (tempfile != null) {
	String pname = getOrMakePermFilename();
	File pfile = new File(dir, pname);
	log.debug2("Rename " + tempfile + " -> " + pfile);
	PlatformUtil.updateAtomically(tempfile, pfile);
	tempfile = null;
	date = TimeBase.nowMs();
	filename = pname;
	return true;
      } else {
	return false;
      }
    }

    File getPermFileAbs() {
      if (filename == null) {
	return null;
      }
      return new File(dir, filename);
    }

    long getDate() {
      return date;
    }

    String getOrMakePermFilename() {
      if (filename != null) {
	return filename;
      }
      try {
	log.debug2("Making perm filename from: " + url);
	String path = UrlUtil.getPath(url);
	String name = FilenameUtils.getBaseName(path);
	String ext = FilenameUtils.getExtension(path);
	return String.format("%02d-%s.%s.gz", seq, name, ext);
      } catch (MalformedURLException e) {
	log.warning("Error building fialover filename", e);
	return String.format("%02d-config-file.gz", seq);
      }
    }
  }

  /** Maps URL to rel filename */
  static class RemoteConfigFailoverMap implements LockssSerializable {
    Map<String,RemoteConfigFailoverInfo> map =
      new HashMap<String,RemoteConfigFailoverInfo>();
    int seq;

    RemoteConfigFailoverInfo put(String url, RemoteConfigFailoverInfo rcfi) {
      return map.put(url, rcfi);
    }

    RemoteConfigFailoverInfo get(String url) {
      return map.get(url);
    }

    int nextSeq() {
      return ++seq;
    }

    Collection<RemoteConfigFailoverInfo> getColl() {
      return map.values();
    }

    boolean update() {
      boolean isModified = false;
      for (RemoteConfigFailoverInfo rcfi : getColl()) {
	isModified |= rcfi.update();
      }
      return isModified;
    }

    void setRemoteConfigFailoverDir(File dir) {
      for (RemoteConfigFailoverInfo rcfi : getColl()) {
	rcfi.setRemoteConfigFailoverDir(dir);
      }
    }
  }

  void setUpRemoteConfigFailover() {
    Configuration plat = getPlatformConfig();
    if (plat.getBoolean(PARAM_REMOTE_CONFIG_FAILOVER,
			DEFAULT_REMOTE_CONFIG_FAILOVER)) {
      remoteConfigFailoverDir =
	new File(cacheConfigDir, plat.get(PARAM_REMOTE_CONFIG_FAILOVER_DIR,
					  DEFAULT_REMOTE_CONFIG_FAILOVER_DIR));
      if (FileUtil.ensureDirExists(remoteConfigFailoverDir)) {
	if (remoteConfigFailoverDir.canWrite()) {
	  remoteConfigFailoverInfoFile =
	    new File(cacheConfigDir, REMOTE_CONFIG_FAILOVER_FILENAME);
	  rcfm = loadRemoteConfigFailoverMap();
	  remoteConfigFailoverMaxAge =
	    plat.getTimeInterval(PARAM_REMOTE_CONFIG_FAILOVER_MAX_AGE,
				 DEFAULT_REMOTE_CONFIG_FAILOVER_MAX_AGE);
	} else {
	  log.error("Can't write to remote config failover dir: " +
		    remoteConfigFailoverDir);
	  remoteConfigFailoverDir = null;
	  rcfm = null;
	}
      } else {
	log.error("Can't create remote config failover dir: " +
		  remoteConfigFailoverDir);
	remoteConfigFailoverDir = null;
	rcfm = null;
      }
    } else {
      remoteConfigFailoverDir = null;
      rcfm = null;
    }
  }

  public boolean isRemoteConfigFailoverEnabled() {
    return rcfm != null;
  }

  RemoteConfigFailoverInfo getRcfi(String url) {
    if (!isRemoteConfigFailoverEnabled()) return null;
    RemoteConfigFailoverInfo rcfi = rcfm.get(url);
    if (rcfi == null) {
      rcfi = new RemoteConfigFailoverInfo(url,
					  remoteConfigFailoverDir,
					  rcfm.nextSeq());
      rcfm.put(url, rcfi);
    }
    return rcfi;
  }

  public File getRemoteConfigFailoverFile(String url) {
    if (!isRemoteConfigFailoverEnabled()) return null;
    RemoteConfigFailoverInfo rcfi = getRcfi(url);
    if (rcfi == null || !rcfi.exists()) {
      return null;
    }
    if (remoteConfigFailoverMaxAge > 0 &&
	TimeBase.msSince(rcfi.getDate()) > remoteConfigFailoverMaxAge) {
      log.error("Remote config failover file is too old (" +
		StringUtil.timeIntervalToString(TimeBase.msSince(rcfi.getDate())) +
		" > " + StringUtil.timeIntervalToString(remoteConfigFailoverMaxAge) +
		"): " + url);
      return null;
    }
    return rcfi.getPermFileAbs();
  }    

  public File getRemoteConfigFailoverTempFile(String url) {
    if (!isRemoteConfigFailoverEnabled()) return null;
    RemoteConfigFailoverInfo rcfi = getRcfi(url);
    if (rcfi == null) {
      return null;
    }
    File tempfile = rcfi.getTempFile();
    if (tempfile != null) {
      log.warning("getRemoteConfigFailoverTempFile: temp file already exists for " + url);
      FileUtil.safeDeleteFile(tempfile);
      rcfi.setTempFile(null);
    }
    try {
      tempfile =
	FileUtil.createTempFile("remote_config", ".tmp",
				remoteConfigFailoverDir);
    } catch (IOException e) {
      log.error("Can't create temp file for remote config failover copy of "
		+ url + " in " + remoteConfigFailoverDir, e);
    }
    rcfi.setTempFile(tempfile);
    return tempfile;
  }

  void updateRemoteConfigFailover() {
    if (!isRemoteConfigFailoverEnabled()) return;
    if (rcfm.update()) {
      try {
	storeRemoteConfigFailoverMap(remoteConfigFailoverInfoFile);
      } catch (IOException | SerializationException e) {
	log.error("Error storing remote config failover map", e);
      }
    }
  }

  void storeRemoteConfigFailoverMap(File file)
      throws IOException, SerializationException {
    log.debug2("storeRemoteConfigFailoverMap: " + file);
    try {
      ObjectSerializer serializer = new XStreamSerializer();
      serializer.serialize(file, rcfm);
    } catch (Exception e) {
      log.error("Could not store remote config failover map", e);
      throw e;
    }
  }

  /**
   * Load RemoteConfigFailoverMap from a file
   * @param file         A source file.
   * @return RemoteConfigFailoverMap instance loaded from file (or a default
   *         value).
   */
  RemoteConfigFailoverMap loadRemoteConfigFailoverMap() {
    try {
      log.debug2("Loading RemoteConfigFailoverMap");
      ObjectSerializer deserializer = new XStreamSerializer();
      RemoteConfigFailoverMap map =
	(RemoteConfigFailoverMap)deserializer.deserialize(remoteConfigFailoverInfoFile);
      map.setRemoteConfigFailoverDir(remoteConfigFailoverDir);
      return map;
    } catch (SerializationException.FileNotFound se) {
      log.debug("No RemoteConfigFailoverMap, creating new one");
      return new RemoteConfigFailoverMap();
    } catch (SerializationException se) {
      log.error("Marshalling exception for RemoteConfigFailoverMap", se);
      return new RemoteConfigFailoverMap();
    } catch (Exception e) {
      log.error("Could not load RemoteConfigFailoverMap", e);
      throw new RuntimeException("Could not load RemoteConfigFailoverMap", e);
    }
  }


  // Testing assistance

  void setGroups(String groups) {
    this.groupNames = groups;
  }

  // TinyUI comes up on port 8081 if can't complete initial props load

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
    private volatile boolean running = false;
    private volatile boolean goAgain = false;

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
	running = true;
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
	running = false;
	if (goOn && !goAgain) {
	  try {
	    nextReload.sleep();
	  } catch (InterruptedException e) {
	    // just wakeup and check for exit
	  }
	}
	goAgain = false;
      }
    }

    private void stopHandler() {
      goOn = false;
      this.interrupt();
    }

    void forceReloadIn(long millis) {
      if (running) {
	// can be called from reload thread, in which case an immediate
	// repeat is necessary
	goAgain = true;
      }
      if (nextReload != null) {
	nextReload.expireIn(millis);
      }
    }
  }
}
