/*
 * $Id: RemoteApi.java,v 1.12 2004-07-21 23:29:47 tlipkis Exp $
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
n
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

package org.lockss.remote;

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.apache.commons.collections.ReferenceMap;

/**
 * API for use by UIs and other remote agents.  Provides access to a
 * variety of daemon status and services using only datastructure-like
 * classes that are easy to serialize.
 */
public class RemoteApi extends BaseLockssManager {
  private static Logger log = Logger.getLogger("RemoteApi");
  static final String PARAM_AU_TREE = PluginManager.PARAM_AU_TREE;
  static final String AU_PARAM_DISPLAY_NAME =
    PluginManager.AU_PARAM_DISPLAY_NAME;

  private Comparator auProxyComparator = new AuProxyOrderComparator();

  private PluginManager pluginMgr;
  private ConfigManager configMgr;

  // cache for proxy objects
  private ReferenceMap auProxies = new ReferenceMap(ReferenceMap.WEAK,
						    ReferenceMap.WEAK);
  private ReferenceMap pluginProxies = new ReferenceMap(ReferenceMap.WEAK,
							ReferenceMap.WEAK);
  private PlatformInfo platInfo = PlatformInfo.getInstance();

  public RemoteApi() {
  }

  public void startService() {
    super.startService();
    pluginMgr = getDaemon().getPluginManager();
    configMgr = getDaemon().getConfigManager();
  }

  /** No config */
  protected void setConfig(Configuration config, Configuration oldConfig,
			   Set changedKeys) {
  }

  /** Create or return an AuProxy for the AU corresponding to the auid.
   * @param auid the auid
   * @return an AuProxy for the AU, or null if no AU exists with the given
   * id.
   */
  public AuProxy findAuProxy(String auid) {
    return findAuProxy(getAuFromId(auid));
  }

  /** Create or return an AuProxy for the AU
   * @param au the AU
   * @return an AuProxy for the AU, or null if the au is null
   */
  synchronized AuProxy findAuProxy(ArchivalUnit au) {
    if (au == null) {
      return null;
    }
    AuProxy aup = (AuProxy)auProxies.get(au);
    if (aup == null) {
      aup = new AuProxy(au, this);
      auProxies.put(au, aup);
    }
    return aup;
  }

  public synchronized InactiveAuProxy findInactiveAuProxy(String auid) {
    return new InactiveAuProxy(auid, this);
  }

  /** Create or return a PluginProxy for the Plugin corresponding to the id.
   * @param pluginid the plugin id
   * @return a PluginProxy for the Plugin, or null if no Plugin exists with
   * the given id.
   */
  public PluginProxy findPluginProxy(String pluginid) {
    PluginProxy pluginp = (PluginProxy)pluginProxies.get(pluginid);
    if (pluginp == null || pluginp.getPlugin() != getPluginFromId(pluginid)) {
      String key = pluginMgr.pluginKeyFromId(pluginid);
      pluginMgr.ensurePluginLoaded(key);
      try {
	pluginp = new PluginProxy(pluginid, this);
      } catch (PluginProxy.NoSuchPlugin e) {
	return null;
      }
      pluginProxies.put(pluginid, pluginp);
      pluginProxies.put(pluginp.getPlugin(), pluginp);
    }
    return pluginp;
  }

  /** Create or return  PluginProxy for the Plugin
   * @param plugin the Plugin
   * @return an PluginProxy for the Plugin, or null if the plugin is null
   */
  synchronized PluginProxy findPluginProxy(Plugin plugin) {
    if (plugin == null) {
      return null;
    }
    PluginProxy pluginp = (PluginProxy)pluginProxies.get(plugin);
    if (pluginp == null) {
      pluginp = new PluginProxy(plugin, this);
      pluginProxies.put(plugin.getPluginId(), pluginp);
      pluginProxies.put(plugin, pluginp);
    }
    return pluginp;
  }

  /** Find or create an AuProxy for each au in the collection */
  List mapAusToProxies(Collection aus) {
    List res = new ArrayList();
    for (Iterator iter = aus.iterator(); iter.hasNext(); ) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      if (!(au instanceof RegistryArchivalUnit)) {
	AuProxy aup = findAuProxy(au);
	res.add(aup);
      }
    }
    return res;
  }

  /** Find or create a PluginProxy for each Plugin in the collection */
  List mapPluginsToProxies(Collection plugins) {
    List res = new ArrayList();
    for (Iterator iter = plugins.iterator(); iter.hasNext(); ) {
      Plugin plugin = (Plugin)iter.next();
      if (!(plugin instanceof RegistryPlugin)) {
	PluginProxy pluginp = findPluginProxy(plugin);
	res.add(pluginp);
      }
    }
    return res;
  }

  // Forward useful PluginManager methods, translating between real objects
  // and proxies as appropriate.

  /**
   * Convert plugin id to key suitable for property file.  Plugin id is
   * currently the same as plugin class name, but that may change.
   * @param id the plugin id
   * @return String the plugin key
   */
  public static String pluginKeyFromId(String id) {
    return PluginManager.pluginKeyFromId(id);
  }

  /**
   * Reconfigure an AU and save the new configuration in the local config
   * file.
   * @param aup the AuProxy
   * @param auConf the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void setAndSaveAuConfiguration(AuProxy aup,
					Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    ArchivalUnit au = aup.getAu();
    pluginMgr.setAndSaveAuConfiguration(au, auConf);
  }

  /**
   * Create an AU and save its configuration in the local config
   * file.
   * @param pluginp the PluginProxy in which to create the AU
   * @param auConf the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @return the new AuProxy
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public AuProxy createAndSaveAuConfiguration(PluginProxy pluginp,
					     Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    Plugin plugin = pluginp.getPlugin();
    ArchivalUnit au = pluginMgr.createAndSaveAuConfiguration(plugin, auConf);
    return findAuProxy(au);
  }

  /**
   * Delete AU configuration from the local config file.
   * @param aup the AuProxy
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void deleteAu(AuProxy aup)
      throws ArchivalUnit.ConfigurationException, IOException {
    if (aup.isActiveAu()) {
      ArchivalUnit au = aup.getAu();
      pluginMgr.deleteAu(au);
    } else {
      pluginMgr.deleteAuConfiguration(aup.getAuId());
    }
  }

  /**
   * Deactivate an AU
   * @param aup the AuProxy
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void deactivateAu(AuProxy aup)
      throws ArchivalUnit.ConfigurationException, IOException {
    ArchivalUnit au = aup.getAu();
    pluginMgr.deactivateAu(au);
  }

  // temporary
  public boolean isRemoveStoppedAus() {
    return pluginMgr.isRemoveStoppedAus();
  }

  /**
   * Return the stored config info for an AU (from config file, not from
   * AU instance).
   * @param auid the id of the AU to be deactivated
   * @return the AU's Configuration, with unprefixed keys.
   */
  public Configuration getStoredAuConfiguration(AuProxy aup) {
    return pluginMgr.getStoredAuConfiguration(aup.getAuId());
  }

  /**
   * Return a list of AuProxies for all configured ArchivalUnits.
   * @return the List of AuProxies
   */
  public List getAllAus() {
    return mapAusToProxies(pluginMgr.getAllAus());
  }

  public List getInactiveAus() {
    Collection inactiveAuIds = pluginMgr.getInactiveAuIds();
    if (inactiveAuIds == null || inactiveAuIds.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    List res = new ArrayList();
    for (Iterator iter = inactiveAuIds.iterator(); iter.hasNext(); ) {
      String auid = (String)iter.next();
      if (!(pluginMgr.getAuFromId(auid) instanceof RegistryArchivalUnit)) {
	res.add(new InactiveAuProxy(auid, this));
      }
    }
    Collections.sort(res, auProxyComparator);
    return res;
  }

  /** Return all the known titles from the title db */
  public List findAllTitles() {
    return pluginMgr.findAllTitles();
  }

  /** Find all the plugins that support the given title */
  public Collection getTitlePlugins(String title) {
    return mapPluginsToProxies(pluginMgr.getTitlePlugins(title));
  }

  /** @return Collection of PluginProxies for all plugins that have been
   * registered.  <i>Ie</i>, that are either listed in
   * org.lockss.plugin.registry, or were loaded by a configured AU */
  public Collection getRegisteredPlugins() {
    return mapPluginsToProxies(pluginMgr.getRegisteredPlugins());
  }

  /** Return list of repository specs for all available repositories */
  public List getRepositoryList() {
    return configMgr.getRepositoryList();
  }

  public PlatformInfo.DF getRepositoryDF(String repo) {
    String path = LockssRepositoryImpl.getLocalRepositoryPath(repo);
    log.debug("path: " + path);
    try {
      return platInfo.getDF(path);
    } catch (PlatformInfo.UnsupportedException e) {
      return null;
    }
  }

  ArchivalUnit getAuFromId(String auid) {
    return pluginMgr.getAuFromId(auid);
  }

  Plugin getPluginFromId(String pluginid) {
    return pluginMgr.getPlugin(pluginKeyFromId(pluginid));
  }

  String pluginIdFromAuId(String auid) {
    return pluginMgr.pluginNameFromAuId(auid);
  }

  public InputStream openCacheConfigFile(String cacheConfigFileName)
      throws FileNotFoundException {
    File cfile = configMgr.getCacheConfigFile(cacheConfigFileName);
    return new FileInputStream(cfile);
  }

  static final String AU_BACKUP_FILE_COMMENT = "# AU Configuration saved ";

  public InputStream getAuConfigBackupStream(String machineName)
      throws FileNotFoundException {
    InputStream fileStream =
      openCacheConfigFile(ConfigManager.CONFIG_FILE_AU_CONFIG);
    String line1 =
      AU_BACKUP_FILE_COMMENT + new Date() + " from " + machineName + "\n";
    return new SequenceInputStream(new ByteArrayInputStream(line1.getBytes()),
				   fileStream);
  }

  /** Restore AU config from an AU config backup file.
   * @param configBackupStream InputStream open on backup fir to be restored
   * @return RestoreAllStatus object describing the results.
   * @throws RemoteApi.InvalidAuConfigBackupFile if the backup file is of
   * an unknown format, unsupported version, or contains keys this
   * operation isn't allowed to modify.
   */
  public RestoreAllStatus restoreAllAus(InputStream configBackupStream)
      throws IOException, InvalidAuConfigBackupFile {
    BufferedInputStream bis = new BufferedInputStream(configBackupStream);
    bis.mark(10000);
    BufferedReader rdr =
      new BufferedReader(new InputStreamReader(bis,
					       Constants.DEFAULT_ENCODING));
    String line1 = rdr.readLine();
    if (line1 == null) {
      throw new InvalidAuConfigBackupFile("Uploaded file is empty");
    }
    if (!line1.startsWith(AU_BACKUP_FILE_COMMENT)) {
      log.debug("line1: " + line1);
      throw new InvalidAuConfigBackupFile("Uploaded file does not appear to be a saved AU configuration");
    }
    bis.reset();
    Properties allAuProps = new Properties();
    try {
      allAuProps.load(bis);
    } catch (Exception e) {
      log.warning("Loading AU config backup file", e);
      throw new InvalidAuConfigBackupFile("Uploaded file has illegal format: "
					  + e.getMessage());
    }
    return restoreAllAus(ConfigManager.fromPropertiesUnsealed(allAuProps));
  }

  /** Restore AU config from an AU config backup file.
   * @param allAuConfig the Configuration to be restored
   * @return RestoreAllStatus object describing the results.
   * @throws RemoteApi.InvalidAuConfigBackupFile if the backup file is of
   * an unknown format, unsupported version, or contains keys this
   * operation isn't allowed to modify.
   */
  RestoreAllStatus restoreAllAus(Configuration allAuConfig)
      throws InvalidAuConfigBackupFile {
    checkLegalProps(allAuConfig);
    Configuration allPlugs = allAuConfig.getConfigTree(PARAM_AU_TREE);
    RestoreAllStatus status = new RestoreAllStatus();
    for (Iterator iter = allPlugs.nodeIterator(); iter.hasNext(); ) {
      String pluginKey = (String)iter.next();
      PluginProxy pluginp = findPluginProxy(pluginKey);
      Configuration pluginConf = allPlugs.getConfigTree(pluginKey);
      for (Iterator auIter = pluginConf.nodeIterator(); auIter.hasNext(); ) {
	String auKey = (String)auIter.next();
	Configuration auConf = pluginConf.getConfigTree(auKey);
	String auid = PluginManager.generateAuId(pluginKey, auKey);
	restoreOneAu(pluginp, auid, auConf, status);
      }

    }
    return status;
  }

  /** Canonicalize a configuration so we can check it for equality with
   * another Configuration.  This is necessary both to handle parameters
   * whose value is the default (but which might be missing from the other
   * Configuration), and values that are equivalent but not equal (such as
   * case differences).  (This canonicalization would make more sense, and
   * could be moved into Configuration, where it would be less
   * out-of-place, if configuration parameters had an associated type and
   * default.  They do have type in the context of AU config params
   * (ConfigParamDescr), but we don't have that information here. */
  Configuration canonicalizeAuConfig(Configuration auConfig) {
    canonicalizeBoolean(auConfig, PluginManager.AU_PARAM_DISABLED, false);
    return auConfig;
  }

  void canonicalizeBoolean(Configuration auConfig, String param,
			   boolean dfault) {
    auConfig.put(param, boolString(auConfig.getBoolean(param, dfault)));
  }

  String boolString(boolean b) {
    return b ? "true" : "false";
  }

  void restoreOneAu(PluginProxy pluginp, String auid,
		    Configuration auConfig, RestoreAllStatus status) {
    RestoreStatus stat = new RestoreStatus(auid);
    status.add(stat);
    Configuration currentConfig = pluginMgr.getStoredAuConfiguration(auid);
    String name = currentConfig.get(AU_PARAM_DISPLAY_NAME);
    if (name == null) {
      name = auConfig.get(AU_PARAM_DISPLAY_NAME);
    }
    stat.setName(name);

    if (currentConfig != null && !currentConfig.isEmpty()) {
      currentConfig = canonicalizeAuConfig(currentConfig);
      auConfig = canonicalizeAuConfig(auConfig);
      ArchivalUnit au = pluginMgr.getAuFromId(auid);
      if (au != null) {
	stat.setName(au.getName());
      }
      if (currentConfig.equals(auConfig)) {
	log.debug("Restore: same config: " + auid);
	stat.setStatus("Unchanged");
      } else {
	log.debug("Restore: conflicting config: " + auid +
		  ", current: " + currentConfig + ", new: " + auConfig);
	stat.setStatus("Conflict");
	Set diffKeys = auConfig.differentKeys(currentConfig);
	StringBuffer sb = new StringBuffer();
	for (Iterator iter = diffKeys.iterator(); iter.hasNext(); ) {
	  String key = (String)iter.next();
	  String foo = "Key: " + key + ", current=" + currentConfig.get(key) +
	    ", file=" + auConfig.get(key) + "<br>";
	  sb.append(foo);
	}
	stat.setExplanation(sb.toString());
      }
    } else {
      try {
	if (auConfig.getBoolean(PluginManager.AU_PARAM_DISABLED, false)) {
	  log.debug("Restore: inactive: " + auid);
	  pluginMgr.updateAuConfigFile(auid, auConfig);
	  stat.setStatus("Restored (inactive)");
	  status.incrOk();
	} else {
	  log.debug("Restore: active: " + auid);
	  AuProxy aup = createAndSaveAuConfiguration(pluginp, auConfig);
	  stat.setStatus("Restored");
	  stat.setName(aup.getName());
	  status.incrOk();
	}
      } catch (ArchivalUnit.ConfigurationException e) {
	stat.setStatus("Configuration Error");
	stat.setExplanation(e.getMessage());
      } catch (IOException e) {
	stat.setStatus("I/O Error");
	stat.setExplanation(e.getMessage());
      }
    }
    if (stat.getName() == null) {
      stat.setName("Unknown");
    }
  }

  /** Throw InvalidAuConfigBackupFile if the config is of an unknown
   * version or contains any keys that shouldn't be part of an AU config
   * backup, such as any keys outside the AU config subtree. */
  void checkLegalProps(Configuration config)
      throws InvalidAuConfigBackupFile {
    String verProp =
      ConfigManager.configVersionProp(ConfigManager.CONFIG_FILE_AU_CONFIG);
    int ver = config.getInt(verProp, 0);
    if (ver != 1) {
      throw new InvalidAuConfigBackupFile("Uploaded file has incompatbile version number");
    }
    Configuration auConfig = config.getConfigTree(PluginManager.PARAM_AU_TREE);
    if (auConfig.keySet().size() != (config.keySet().size() - 1)) {
      throw new InvalidAuConfigBackupFile("Uploaded file contains illegal keys; does not appear to be a saved AU configuration");
    }
    for (Iterator iter = auConfig.keyIterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (PluginManager.NON_USER_SETTABLE_AU_PARAMS.contains(key)) {
	throw new InvalidAuConfigBackupFile("Uploaded file contains illegal key (" + key + "); does not appear to be a saved AU configuration");
      }
    }
  }

  /** Object describing results of AU config restore operation.  Basically
   * a list of {@link RestoreStatus}, one for each AU restore attempted */
  public static class RestoreAllStatus {
    static Comparator statusComparator = new RestoreStatusOrderComparator();
    private List statusList = new ArrayList();
    private int ok = 0;
    public List getStatusList() {
      Collections.sort(statusList, statusComparator);
      return statusList;
    }
    public int getOkCnt() {
      return ok;
    }
    void add(RestoreStatus status) {
      statusList.add(status);
    }
    void incrOk() {
      ok++;
    }
  }
  static class RestoreStatusOrderComparator implements Comparator {
    CatalogueOrderComparator coc = CatalogueOrderComparator.SINGLETON;
    public int compare(Object o1, Object o2) {
      if (!((o1 instanceof RestoreStatus)
	   && (o2 instanceof RestoreStatus))) {
	throw new IllegalArgumentException("RestoreStatusOrderComparator(" +
					   o1.getClass().getName() + "," +
					   o2.getClass().getName() + ")");
      }
      RestoreStatus rs1 = (RestoreStatus)o1;
      RestoreStatus rs2 = (RestoreStatus)o2;
      int res = rs1.order - rs2.order;
      if (res == 0) {
	res = coc.compare(rs1.getName(), rs2.getName());
      }
      return res;
    }

  }
  /** Object describing result of attempting to restore a single saved AU
   * configuration. */
  public static class RestoreStatus {
    private String auid;
    private String name;
    private String status;
    private String explanation;
    private int order = 0;

    RestoreStatus(String auid) {
      this.auid = auid;
    }
    public String getAuId() {
      return auid;
    }
    public String getName() {
      return name;
    }
    public String getStatus() {
      return status;
    }
    public String getExplanation() {
      return explanation;
    }
    void setStatus(String s) {
      this.status = s;
      if (StringUtil.startsWithIgnoreCase(s, "Conflict")) {
	order = 1;
      } else if (StringUtil.startsWithIgnoreCase(s, "Restored")) {
	order = 2;
      } else {
	order = 3;
      }
    }
    void setName(String s) {
      this.name = s;
    }
    void setExplanation(String s) {
      this.explanation = s;
    }
  }

  /** Exception thrown if the uploaded AU config backup file isn't valid */
  public class InvalidAuConfigBackupFile extends Exception {
    public InvalidAuConfigBackupFile(String message) {
      super(message);
    }
  }


  /** Comparator for sorting AuProxy lists.  Not suitable for use in a
   * TreeSet unless changed to never return 0. */
  class AuProxyOrderComparator implements Comparator {
    CatalogueOrderComparator coc = CatalogueOrderComparator.SINGLETON;
    public int compare(Object o1, Object o2) {
      if (!((o1 instanceof AuProxy)
	   && (o2 instanceof AuProxy))) {
	throw new IllegalArgumentException("AuProxyOrderComparator(" +
					   o1.getClass().getName() + "," +
					   o2.getClass().getName() + ")");
      }
      AuProxy a1 = (AuProxy)o1;
      AuProxy a2 = (AuProxy)o2;
      int res = coc.compare(a1.getName(), a2.getName());
      if (res == 0) {
	res = coc.compare(a1.getAuId(), a2.getAuId());
      }
      return res;
    }
  }

}
