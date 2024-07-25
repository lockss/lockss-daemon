 /*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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
import java.text.*;
import java.util.*;
import java.util.zip.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.account.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.state.*;
import org.lockss.subscription.SubscriptionManager;
import org.lockss.repository.*;
import org.lockss.servlet.ServletManager;
import org.lockss.util.*;
import org.lockss.util.CloseCallbackInputStream.DeleteFileOnCloseInputStream;
import org.lockss.mail.*;
import org.lockss.servlet.ServletUtil;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections.map.ReferenceMap;

/**
 * API for use by UIs and other remote agents.  Provides access to a
 * variety of daemon status and services using proxies object whose instance
 * identity is unimportant.
 */
public class RemoteApi
  extends BaseLockssDaemonManager implements ConfigurableManager {
  private static Logger log = Logger.getLogger("RemoteApi");

  static CatalogueOrderComparator coc = CatalogueOrderComparator.SINGLETON;
  static Comparator auProxyComparator = new AuProxyOrderComparator();

  static final String PARAM_AU_TREE = PluginManager.PARAM_AU_TREE;
  static final String AU_PARAM_DISPLAY_NAME =
    PluginManager.AU_PARAM_DISPLAY_NAME;

  static final String PREFIX = Configuration.PREFIX + "remoteApi.";

  /** Config backup file version: V1 is just AU config, V2 is zip including
   * AU config and agreement history */
  static final String PARAM_BACKUP_FILE_VERSION =
    PREFIX + "backupFileVersion";
  static final String DEFAULT_BACKUP_FILE_VERSION = "V2";

  /** Config backup file externsion, used in place of <code>.zip</code> */
  static final String PARAM_BACKUP_FILE_EXTENSION =
    PREFIX + "backupFileExtension";
  static final String DEFAULT_BACKUP_FILE_EXTENSION = "zip";

  /** Config backup file version: V1 is just AU config, V2 is zip including
   * AU config and agreement history */
  static final String PARAM_BACKUP_STREAM_MARK_SIZE =
    PREFIX + "backupStreamMarkSize";
  static final int DEFAULT_BACKUP_STREAM_MARK_SIZE = 10000;

  /** If true, include down AUs in Add AUs. */
  static final String PARAM_INCLUDE_DOWN_AUS = PREFIX + "includeDownAus";
  static final boolean DEFAULT_INCLUDE_DOWN_AUS = false;

  static final String BACK_FILE_PROPS = "cacheprops";
  static final String BACK_FILE_AU_PROPS = "auprops";
  static final String BACK_FILE_AGREE_MAP = "idagreement";
  static final String BACK_FILE_AUSTATE = "austate";

  static final String BACK_PROP_LOCAL_ID_V1 =
    Configuration.PREFIX + "localId.v1";
  static final String BACK_PROP_LOCAL_ID_V3 =
    Configuration.PREFIX + "localId.v3";
  static final String BACK_PROP_VERSION = Configuration.PREFIX + "version";

  static final String AU_BACK_PROP_AUID = "auid";
  static final String AU_BACK_PROP_REPOSPEC = "repospec";
  static final String AU_BACK_PROP_REPODIR = "repodir";

  /** "Add new" opcode for batchAddAus(), batchProcessAus() */
  public static final int BATCH_ADD_ADD = 1;
  /** "Reactivate" opcode for batchAddAus(), batchProcessAus() */
  public static final int BATCH_ADD_REACTIVATE = 2;
  /** "Restore from backup" opcode for batchAddAus(), batchProcessAus() */
  public static final int BATCH_ADD_RESTORE = 3;

  public static final String[] BATCH_ADD_OP_STRINGS = {
    "", "Batch Add", "Batch Reactivate", "Batch Restore" };

  private PluginManager pluginMgr;
  private ConfigManager configMgr;
  private IdentityManager idMgr;
  private RepositoryManager repoMgr;
  private AccountManager acctMgr;

  private String paramBackupFileVer = DEFAULT_BACKUP_FILE_VERSION;
  private int paramBackupStreamMarkSize = DEFAULT_BACKUP_STREAM_MARK_SIZE;
  private String paramBackupFileDotExtension =
    makeExtension(DEFAULT_BACKUP_FILE_EXTENSION);
  private boolean paramIncludeDownAus = DEFAULT_INCLUDE_DOWN_AUS;

  // cache for proxy objects
  private ReferenceMap auProxies = new ReferenceMap(ReferenceMap.WEAK,
						    ReferenceMap.WEAK);
  private ReferenceMap pluginProxies = new ReferenceMap(ReferenceMap.WEAK,
							ReferenceMap.WEAK);
  public RemoteApi() {
  }

  public void startService() {
    super.startService();
    pluginMgr = getDaemon().getPluginManager();
    configMgr = getDaemon().getConfigManager();
    idMgr = getDaemon().getIdentityManager();
    repoMgr = getDaemon().getRepositoryManager();
    acctMgr = getDaemon().getAccountManager();
  }

  public void setConfig(Configuration config,
			Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      paramBackupFileVer = config.get(PARAM_BACKUP_FILE_VERSION,
				      DEFAULT_BACKUP_FILE_VERSION);
      paramBackupStreamMarkSize =
	config.getInt(PARAM_BACKUP_STREAM_MARK_SIZE,
		      DEFAULT_BACKUP_STREAM_MARK_SIZE);
      paramBackupFileDotExtension =
	makeExtension(config.get(PARAM_BACKUP_FILE_EXTENSION,
				 DEFAULT_BACKUP_FILE_EXTENSION));
      paramIncludeDownAus =
	config.getBoolean(PARAM_INCLUDE_DOWN_AUS, DEFAULT_INCLUDE_DOWN_AUS);
    }
  }

  String makeExtension(String ext) {
    if (ext.startsWith(".")) {
      return ext;
    }
    return "." + ext;
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
    InactiveAuProxy aup = (InactiveAuProxy)auProxies.get(auid);
    if (aup == null) {
      aup = new InactiveAuProxy(auid, this);
      auProxies.put(auid, aup);
    }
    return aup;
  }

  /** Create or return a PluginProxy for the Plugin corresponding to the id.
   * @param pluginid the plugin id
   * @return a PluginProxy for the Plugin, or null if no Plugin exists with
   * the given id.
   */
  public synchronized PluginProxy findPluginProxy(String pluginid) {
    PluginProxy pluginp = (PluginProxy)pluginProxies.get(pluginid);
    if (pluginp == null ||
	pluginp.getPlugin() != getPluginFromId(pluginid)) {
      String key = PluginManager.pluginKeyFromId(pluginid);
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
      if (!pluginMgr.isInternalAu(au)) {
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
      if (!pluginMgr.isInternalPlugin(plugin)) {
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
   * @param aup the AuProxy
   * @return the AU's Configuration, with unprefixed keys.
   */
  public Configuration getStoredAuConfiguration(AuProxy aup) {
    return pluginMgr.getStoredAuConfiguration(aup.getAuId());
  }

  /**
   * Return the current config info for an AU (from current configuration)
   * @param aup the AuProxy
   * @return the AU's Configuration, with unprefixed keys.
   */
  public Configuration getCurrentAuConfiguration(AuProxy aup) {
    return pluginMgr.getCurrentAuConfiguration(aup.getAuId());
  }

  /**
   * Return a list of AuProxies for all configured ArchivalUnits.
   * @return the List of AuProxies
   */
  public List getAllAus() {
    return mapAusToProxies(pluginMgr.getAllAus());
  }

  public int countInactiveAus() {
    return pluginMgr.getInactiveAuIds().size();
  }

  public List getInactiveAus() {
    Collection inactiveAuIds = pluginMgr.getInactiveAuIds();
    if (inactiveAuIds == null || inactiveAuIds.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    List res = new ArrayList();
    for (Iterator iter = inactiveAuIds.iterator(); iter.hasNext(); ) {
      String auid = (String)iter.next();
      if (!pluginMgr.isInternalAu(pluginMgr.getAuFromId(auid))) {
	res.add(findInactiveAuProxy(auid));
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
    return repoMgr.getRepositoryList();
  }

  public List findExistingRepositoriesFor(String auid) {
    return repoMgr.findExistingRepositoriesFor(auid);
  }

  public PlatformUtil.DF getRepositoryDF(String repoName) {
    return repoMgr.getRepositoryDF(repoName);
  }

  public LinkedMap<String,PlatformUtil.DF> getRepositoryMap() {
    return repoMgr.getRepositoryMap();
  }

  public String findLeastFullRepository() {
    return repoMgr.findLeastFullRepository();
  }

  public String findLeastFullRepository(Map<String,PlatformUtil.DF> repoMap) {
    return repoMgr.findLeastFullRepository(repoMap);
  }

  ArchivalUnit getAuFromId(String auid) {
    return pluginMgr.getAuFromId(auid);
  }

  Plugin getPluginFromId(String pluginid) {
    return pluginMgr.getPlugin(pluginKeyFromId(pluginid));
  }

  String pluginIdFromAuId(String auid) {
    return PluginManager.pluginNameFromAuId(auid);
  }

  public InputStream openCacheConfigFile(String cacheConfigFileName)
      throws FileNotFoundException {
    File cfile = configMgr.getCacheConfigFile(cacheConfigFileName);
    InputStream res = new FileInputStream(cfile);
    log.debug3("Opened " + cfile);
    return res;
  }

  static final String AU_BACKUP_FILE_COMMENT = "# AU Configuration saved ";

  /** Open an InputStream on config/state backup file, of a version
   * determined by config  */
  public InputStream getAuConfigBackupStream(String machineName)
      throws IOException {
    if ("V1".equalsIgnoreCase(paramBackupFileVer)) {
      return getAuConfigBackupStreamV1(machineName);
    } else {
      return getAuConfigBackupStreamV2(machineName);
    }
  }

  /** Open an InputStream on the local AU config file, for backup purposes */
  public InputStream getAuConfigBackupStreamV1(String machineName)
      throws FileNotFoundException {
    InputStream fileStream =
      openCacheConfigFile(ConfigManager.CONFIG_FILE_AU_CONFIG);
    String line1 =
      AU_BACKUP_FILE_COMMENT + new Date() + " from " + machineName + "\n";
    return new SequenceInputStream(new ByteArrayInputStream(line1.getBytes()),
				   fileStream);
  }

  public InputStream getAuConfigBackupFileOrStream(String machineName,
						   boolean forceCreate)
      throws IOException {
    InputStream res = null;
    if (!forceCreate) {
      try {
	res = backupFileInputStreamOrNull();
      } catch (IOException e) {
	log.error("Couldn't open existing backup file: " + e.toString());
	// fall through
      }
    }
    if (res == null) {
      res = getAuConfigBackupStream(machineName);
    }
    return res;
  }

  InputStream backupFileInputStreamOrNull() throws IOException {
    File bfile = getBackupFile();
    if (!bfile.exists()) {
      log.debug("No existing backup file: " + bfile);
      return null;
    }
    if (bfile.length() == 0) {
      log.debug("Backup file empty: " + bfile);
      return null;
    }
    log.debug("Returning existing backup file: " + bfile);
    return new BufferedInputStream(new FileInputStream(bfile));
  }

  /** Open an InputStream on config/state backup file */
  public InputStream getAuConfigBackupStreamV2(String machineName)
      throws IOException {
    File file = createConfigBackupFile(machineName);
    return new BufferedInputStream(new DeleteFileOnCloseInputStream(file));
  }

  public File createConfigBackupFile() throws IOException {
    return createConfigBackupFile(getMachineName());
  }

  public File createConfigBackupFile(String machineName) throws IOException {
    return createConfigBackupFile(null, machineName);
  }

  public File createConfigBackupFile(File permFile,
				     String machineName) throws IOException {
    log.info("createConfigBackupFile: " + permFile);
    File file = FileUtil.createTempFile("cfgsave", ".zip",
					(permFile != null
					 ? permFile.getParentFile() : null));
    ZipOutputStream zip = null;
    try {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
      zip = new ZipOutputStream(out);
      // save platform config, plus version and local identities (not yet
      // used for anything)
      Configuration platConfig =
	ConfigManager.getPlatformConfig().getConfigTree(Configuration.PLATFORM).addPrefix(Configuration.PLATFORM);
      // remove password info
      platConfig.remove(ServletManager.PARAM_PLATFORM_USERNAME);
      platConfig.remove(ServletManager.PARAM_PLATFORM_PASSWORD);

      platConfig.put(BACK_PROP_VERSION, "2");
      platConfig.put(BACK_PROP_VERSION, "2");
      try {
	PeerIdentity p1 = idMgr.getLocalPeerIdentity(Poll.V1_PROTOCOL);
	platConfig.put(BACK_PROP_LOCAL_ID_V1, p1.getIdString());
      } catch (Exception e) {
      }
      try {
	PeerIdentity p3 = idMgr.getLocalPeerIdentity(Poll.V3_PROTOCOL);
	platConfig.put(BACK_PROP_LOCAL_ID_V3, p3.getIdString());
      } catch (Exception e) {
      }
      addPropsToZip(zip, platConfig, BACK_FILE_PROPS,
		    "Configuration and repair info for LOCKSS box " +
		    machineName);

      // add all the cache config files
      for (ConfigManager.LocalFileDescr lfd : configMgr.getLocalFileDescrs()) {
	File cfgfile = lfd.getFile();
	if (cfgfile.getName().equals(ConfigManager.CONFIG_FILE_AU_CONFIG)) {
	  addCfgFileToZip(zip, getAuConfigBackupStreamV1(machineName),
			  ConfigManager.CONFIG_FILE_AU_CONFIG);
	} else {
	  addCfgFileToZip(zip, cfgfile, null);
	}
      }
      // add identity db
      zip.putNextEntry(new ZipEntry(IdentityManager.IDDB_FILENAME));
      try {
	idMgr.writeIdentityDbTo(zip);
      } catch (FileNotFoundException e) {
	log.debug2("Couldn't write iddb", e);
      }
      zip.closeEntry();
      if (acctMgr.isEnabled()) {
	File acctDir = acctMgr.getAcctDir();
	ZipUtil.addDirToZip(zip, acctDir, "accts");
      }
      List aus = pluginMgr.getAllAus();
      // add a directory for each AU
      if (aus != null) {
	addAusToZip(zip, aus);
      }

      // Add any configured subscriptions to the zip file.
      SubscriptionManager subMgr = getDaemon().getSubscriptionManager();
      
      if (subMgr != null && subMgr.isReady()) {
	subMgr.writeSubscriptionsBackupToZip(zip);
      }

      // Add any COUNTER aggregate statistics to the zip file.
      CounterReportsManager crMgr = getDaemon().getCounterReportsManager();
      
      if (crMgr != null && crMgr.isReady()) {
	crMgr.writeAggregatesBackupToZip(zip);
      }

      zip.close();
      if (permFile != null) {
	PlatformUtil.updateAtomically(file, permFile);
	return permFile;
      } else {
	return file;
      }
    } catch (IOException e) {
      log.warning("createConfigBackupFile", e);
      IOUtil.safeClose(zip);
      file.delete();
      throw e;
    } catch (Exception e) {
      log.warning("createConfigBackupFile", e);
      IOUtil.safeClose(zip);
      file.delete();
      throw new IOException(e.toString());
    }
  }

  /** Backup only subscriptions and COUNTER data */
  public File createSubscriptionsAndCounterBackupFile(File permFile)
      throws IOException {
    log.info("createSubscriptionsAndCounterBackupFile: " + permFile);
    File file = FileUtil.createTempFile("cfgsave", ".zip",
					(permFile != null
					 ? permFile.getParentFile() : null));
    ZipOutputStream zip = null;
    try {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
      zip = new ZipOutputStream(out);

      // Add any configured subscriptions to the zip file.
      SubscriptionManager subMgr = getDaemon().getSubscriptionManager();

      if (subMgr != null && subMgr.isReady()) {
	subMgr.writeSubscriptionsBackupToZip(zip);
      }

      // Add any COUNTER aggregate statistics to the zip file.
      CounterReportsManager crMgr = getDaemon().getCounterReportsManager();

      if (crMgr != null && crMgr.isReady()) {
	crMgr.writeAggregatesBackupToZip(zip);
      }

      zip.close();
      if (permFile != null) {
	PlatformUtil.updateAtomically(file, permFile);
	return permFile;
      } else {
	return file;
      }
    } catch (IOException e) {
      log.warning("createConfigBackupFile", e);
      IOUtil.safeClose(zip);
      file.delete();
      throw e;
    } catch (Exception e) {
      log.warning("createConfigBackupFile", e);
      IOUtil.safeClose(zip);
      file.delete();
      throw new IOException(e.toString());
    }
  }

  void addAusToZip(ZipOutputStream zip, List aus) throws IOException {
    int dirn = 1;
    for (Iterator iter = aus.iterator(); iter.hasNext(); ) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      log.debug2("au: "+ au);
      if (pluginMgr.isInternalAu(au)) {
	log.debug2("internal: "+ au);
	continue;
      }
      String dir = Integer.toString(dirn) + "/";
      zip.putNextEntry(new ZipEntry(dir));
      Configuration auConfig = au.getConfiguration();
      Properties auprops = new Properties();
      auprops.setProperty(AU_BACK_PROP_AUID, au.getAuId());
      String repo = LockssRepositoryImpl.getRepositorySpec(au);
      auprops.setProperty(AU_BACK_PROP_REPOSPEC, repo);
      auprops.setProperty(AU_BACK_PROP_REPODIR,
			  LockssRepositoryImpl.mapAuToFileLocation(LockssRepositoryImpl.getLocalRepositoryPath(repo), au));

      addPropsToZip(zip, auprops, dir + BACK_FILE_AU_PROPS,
		    "AU " + au.getName());
      if (idMgr.hasAgreeMap(au)) {
	zip.putNextEntry(new ZipEntry(dir + BACK_FILE_AGREE_MAP));
	try {
	  idMgr.writeIdentityAgreementTo(au, zip);
	} catch (FileNotFoundException e) {}
	zip.closeEntry();
      }
      File auStateFile = getAuStateFile(au);

      if (auStateFile.exists()) {
	try {
	  addCfgFileToZip(zip, auStateFile, dir + BACK_FILE_AUSTATE);
	} catch (FileNotFoundException e) {}
      }
      dirn++;
    }
  }

  File getAuStateFile(ArchivalUnit au) {
    HistoryRepository hRep = getDaemon().getHistoryRepository(au);
    return hRep.getAuStateFile();
  }

  void addPropsToZip(ZipOutputStream zip, Properties props,
		     String entName, String header)
      throws IOException {
    zip.putNextEntry(new ZipEntry(entName));
    props.store(zip, header);
    zip.closeEntry();
  }

  void addPropsToZip(ZipOutputStream zip, Configuration props,
		     String entName, String header)
      throws IOException {
    zip.putNextEntry(new ZipEntry(entName));
    props.store(zip, header);
    zip.closeEntry();
  }

  void addCfgFileToZip(ZipOutputStream z, String fileName, String entName)
      throws IOException {
    addCfgFileToZip(z, new File(fileName), entName);
  }

  void addCfgFileToZip(ZipOutputStream z, File file, String entName)
      throws IOException {
    log.debug2("addCfgFileToZip: "+ file);
    try {
      ZipUtil.addFileToZip(z, file, entName);
    } catch (FileNotFoundException ignore) {}
  }

  void addCfgFileToZip(ZipOutputStream z, InputStream in, String entName)
      throws IOException {
    try {
      ZipUtil.addFileToZip(z, in, entName);
      log.debug2("added: "+ entName);
    } finally {
      IOUtil.safeClose(in);
    }
  }

  /** Batch create AUs from AU config backup file.
   * @param configBackupStream InputStream open on backup file to be restored
   * @return BatchAuStatus object describing the results.  If doCreate was
   * false, the status reflects the possibility that the AUs could be
   * created.
   * @throws RemoteApi.InvalidAuConfigBackupFile if the backup file is of
   * an unknown format, unsupported version, or contains keys this
   * operation isn't allowed to modify.
   */
  public BatchAuStatus processSavedConfig(InputStream configBackupStream)
      throws IOException, InvalidAuConfigBackupFile {
    BufferedInputStream bis = new BufferedInputStream(configBackupStream);
    try {
      if (ZipUtil.isZipFile(bis)) {
	return processSavedConfigZip(bis);
      } else {
	return processSavedConfigProps(bis);
      }
    } finally {
      IOUtil.safeClose(bis);
    }
  }

  public BatchAuStatus processSavedConfigZip(InputStream configBackupStream)
      throws IOException, InvalidAuConfigBackupFile {
    // XXX This temp dir doesn't get deleted.  It needs to stay around
    // through the entire multi-step restore interaction, so it's not clear
    // where to delete it.  Not a big problem because it's only created if
    // the user does a restore..
    File dir = FileUtil.createTempDir("locksscfg", "");
    try {
      ZipUtil.unzip(configBackupStream, dir);

      // Restore any COUNTER aggregate statistics from the zip file.
      CounterReportsManager crMgr = getDaemon().getCounterReportsManager();
      
      if (crMgr != null && crMgr.isReady()) {
	crMgr.loadAggregatesFromBackup(dir);
      }

      // Restore any subscriptions from the zip file.
      SubscriptionManager subMgr = getDaemon().getSubscriptionManager();
      
      if (subMgr != null && subMgr.isReady()) {
	subMgr.loadSubscriptionsFromBackup(dir);
      }

      File autxt = new File(dir, ConfigManager.CONFIG_FILE_AU_CONFIG);
      if (!autxt.exists()) {
	throw new InvalidAuConfigBackupFile("Uploaded file does not appear to be a saved AU configuration: no au.txt");
      }
      BufferedInputStream auin =
	new BufferedInputStream(new FileInputStream(autxt));
      try {
	BatchAuStatus bas = processSavedConfigProps(auin);
	bas.setBackupInfo(buildBackupInfo(dir));
	if (log.isDebug3()) {
	  log.debug3("processSavedConfigZip: " + bas);
	}
	return bas;
      } finally {
	IOUtil.safeClose(auin);
      }
    } catch (ZipException e) {
      FileUtil.delTree(dir);
      throw new InvalidAuConfigBackupFile("Uploaded file does not appear to be a saved AU configuration: " + e.toString());
    } catch (IOException e) {
      FileUtil.delTree(dir);
      throw e;
    }
  }

  public BatchAuStatus processSavedConfigProps(BufferedInputStream auTxtStream)
      throws IOException, InvalidAuConfigBackupFile {
    int commentLen = AU_BACKUP_FILE_COMMENT.length();
    // There is apparently hidden buffering in the InputStreamReader's
    // StreamDecoder which throws off our calculation as to how much
    // auTxtStream needs to buffer, so use a large number
    auTxtStream.mark(paramBackupStreamMarkSize);
    BufferedReader rdr =
      new BufferedReader(new InputStreamReader(auTxtStream,
					       Constants.DEFAULT_ENCODING),
			 commentLen * 2);
    // We really want rdr.readLine(), but we need to limit amount it reads
    // (in case it doesn't find newline)
    char[] buf = new char[commentLen];
    int chars = StreamUtil.readChars(rdr, buf, commentLen);
    log.debug3("chars: " + chars);
    if (chars == 0) {
      throw new InvalidAuConfigBackupFile("Uploaded file is empty");
    }
    String line1 = new String(buf);
    if (chars < commentLen ||
	!line1.startsWith(AU_BACKUP_FILE_COMMENT)) {
      log.debug("line1: " + line1);
      throw new InvalidAuConfigBackupFile("Uploaded file does not appear to be a saved AU configuration");
    }
    try {
      auTxtStream.reset();
    } catch (IOException e) {
      throw new IOException("Internal error: please report \"Insufficient buffering for restore\".");
    }
    Properties allAuProps = new Properties();
    try {
      allAuProps.load(auTxtStream);
    } catch (Exception e) {
      log.warning("Loading AU config backup file", e);
      throw new InvalidAuConfigBackupFile("Uploaded file has illegal format: "
					  + e.getMessage());
    }
    Configuration allAuConfig =
      ConfigManager.fromPropertiesUnsealed(allAuProps);
    int ver = checkLegalAuConfigTree(allAuConfig);
    return batchProcessAus(false, BATCH_ADD_RESTORE, allAuConfig, null);
  }

  /** Throw InvalidAuConfigBackupFile if the config is of an unknown
   * version or contains any keys that shouldn't be part of an AU config
   * backup, such as any keys outside the AU config subtree.
   * @return the file version number
   */
  int checkLegalAuConfigTree(Configuration config)
      throws InvalidAuConfigBackupFile {
    String verProp =
      ConfigManager.configVersionProp(ConfigManager.CONFIG_FILE_AU_CONFIG);
    if (!config.containsKey(verProp)) {
      throw new
	InvalidAuConfigBackupFile("Uploaded file has no version number");
    }
    int ver = config.getInt(verProp, 0);
    if (ver != 1) {
      throw new
	InvalidAuConfigBackupFile("Uploaded file has incompatible version " +
				  "number: " + config.get(verProp));
    }
    Configuration auConfig = config.getConfigTree(PluginManager.PARAM_AU_TREE);
    if ((config.keySet().size() - 1) != auConfig.keySet().size()) {
      String msg = "Uploaded file contains illegal keys; does not appear to be a saved AU configuration";
      log.warning(msg + ": " + config);
      throw new InvalidAuConfigBackupFile(msg);
    }
    for (Iterator iter = auConfig.keyIterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (PluginManager.NON_USER_SETTABLE_AU_PARAMS.contains(key)) {
	throw new InvalidAuConfigBackupFile("Uploaded file contains illegal key (" + key + "); does not appear to be a saved AU configuration");
      }
    }
    return ver;
  }

  /** Restore AU config from an AU config backup file.
   * @param allAuConfig the Configuration to be restored
   * @return BatchAuStatus object describing the results.
   */
  public BatchAuStatus batchAddAus(int addOp,
				   Configuration allAuConfig,
				   BackupInfo bi) {
    return batchProcessAus(true, addOp, allAuConfig, bi);
  }

  public BatchAuStatus batchProcessAus(boolean doCreate,
				       int addOp,
				       Configuration allAuConfig,
				       BackupInfo bi) {
    Configuration allPlugs = allAuConfig.getConfigTree(PARAM_AU_TREE);
    BatchAuStatus bas = new BatchAuStatus();
    BatchAuStatus.Entry lastStat = null;
    try {
      if (doCreate) {
	configMgr.startAuBatch();
      }
      for (Iterator iter = allPlugs.nodeIterator(); iter.hasNext(); ) {
	String pluginKey = (String)iter.next();
	PluginProxy pluginp = findPluginProxy(pluginKey);
	// Do not dereference pluginp before null check in batchProcessOneAu()
	Configuration pluginConf = allPlugs.getConfigTree(pluginKey);
	for (Iterator auIter = pluginConf.nodeIterator(); auIter.hasNext(); ) {
	  String auKey = (String)auIter.next();
	  Configuration auConf = pluginConf.getConfigTree(auKey);
	  String auid = PluginManager.generateAuId(pluginKey, auKey);
	  lastStat = batchProcessOneAu(doCreate, addOp,
				       pluginp, auid, auConf, bi);
	  bas.add(lastStat);
	}
      }
    } finally {
      if (doCreate) {
	try {
	  configMgr.finishAuBatch();
	} catch (IOException e) {
	  batchFinishError(lastStat, e);
	}
      }
    }	       
    if (doCreate) {
      configMgr.requestReload();
    }
    return bas;
  }

  void batchFinishError(BatchAuStatus.Entry lastStat, IOException e) {
    log.error("finishAuBatch", e);
    if (lastStat != null) {
      lastStat.setStatus("Error", STATUS_ORDER_ERROR);
      lastStat.setExplanation("Error saving AU configurations: " +
			      e.toString());
    }
  }

  /** Delete a batch of AUs
   * @param auids
   * @return BatchAuStatus object describing the results.
   */
  public BatchAuStatus deleteAus(List auids) {
    BatchAuStatus bas = new BatchAuStatus();
    BatchAuStatus.Entry lastStat = null;
    try {
      configMgr.startAuBatch();
      for (Iterator iter = auids.iterator(); iter.hasNext(); ) {
	String auid = (String)iter.next();
	BatchAuStatus.Entry stat = bas.newEntry(auid);
	ArchivalUnit au = pluginMgr.getAuFromId(auid);
	if (au != null) {
	  stat.setName(au.getName(), au.getPlugin());
	  try {
	    pluginMgr.deleteAu(au);
	    stat.setStatus("Deleted", STATUS_ORDER_NORM);
	    lastStat = stat;
	  } catch (Exception e) {
	    log.warning("Error deleting AU", e);
	    stat.setStatus("Possibly Not Deleted", STATUS_ORDER_WARN);
	    stat.setExplanation("Error deleting: " + e.getMessage());
	  }
	} else {
	  stat.setStatus("Not Found", STATUS_ORDER_WARN);
	  stat.setName(auid);
	}
	bas.add(stat);
      }
      return bas;
    } finally {
      try {
	configMgr.finishAuBatch();
      } catch (IOException e) {
	batchFinishError(lastStat, e);
      }
    }	       
  }

  /** Deactivate a batch of AUs
   * @param auids
   * @return BatchAuStatus object describing the results.
   */
  public BatchAuStatus deactivateAus(List auids) {
    BatchAuStatus bas = new BatchAuStatus();
    BatchAuStatus.Entry lastStat = null;
    try {
      configMgr.startAuBatch();
      for (Iterator iter = auids.iterator(); iter.hasNext(); ) {
	String auid = (String)iter.next();
	BatchAuStatus.Entry stat = bas.newEntry(auid);
	ArchivalUnit au = pluginMgr.getAuFromId(auid);
	if (au != null) {
	  stat.setName(au.getName(), au.getPlugin());
	  try {
	    pluginMgr.deactivateAu(au);
	    stat.setStatus("Deactivated", STATUS_ORDER_NORM);
	    lastStat = stat;
	  } catch (IOException e) {
	    stat.setStatus("Not Deactivated", STATUS_ORDER_WARN);
	    stat.setExplanation("Error deleting: " + e.getMessage());
	  }
	} else {
	  stat.setStatus("Not Found", STATUS_ORDER_WARN);
	  stat.setName(auid);
	}
	bas.add(stat);
      }
      return bas;
    } finally {
      try {
	configMgr.finishAuBatch();
      } catch (IOException e) {
	batchFinishError(lastStat, e);
      }
    }	       
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
  Configuration normalizedAuConfig(Configuration auConfig) {
    Configuration res = auConfig.copy();
    normalizeBoolean(res, PluginManager.AU_PARAM_DISABLED, false);
    res.removeConfigTree(PluginManager.AU_PARAM_RESERVED);
    return res;
  }

  void normalizeBoolean(Configuration auConfig, String param, boolean dfault) {
    auConfig.put(param, boolString(auConfig.getBoolean(param, dfault)));
  }

  String boolString(boolean b) {
    return b ? "true" : "false";
  }

  String addOpName(int addOp) {
    try {
      return BATCH_ADD_OP_STRINGS[addOp];
    } catch (Exception e) {
      return Integer.toString(addOp);
    }
  }

  BatchAuStatus.Entry batchProcessOneAu(boolean doCreate,
					int addOp,
					PluginProxy pluginp,
					String auid,
					Configuration auConfig,
					BackupInfo bi) {
    final String DEBUG_HEADER = "batchProcessOneAu(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "doCreate = " + doCreate);
      log.debug2(DEBUG_HEADER + "addOp = " + addOp);
      log.debug2(DEBUG_HEADER + "auid = " + auid);
      log.debug2(DEBUG_HEADER + "auConfig = " + auConfig);
      log.debug2(DEBUG_HEADER + "bi = " + bi);
    }

    BatchAuStatus.Entry stat = new BatchAuStatus.Entry(auid);
    stat.setRepoNames(repoMgr.findExistingRepositoriesFor(auid));
    Configuration oldConfig = pluginMgr.getStoredAuConfiguration(auid);
    String name = null;
    if (oldConfig != null) {
      name = oldConfig.get(AU_PARAM_DISPLAY_NAME);
    }
    if (name == null) {
      name = auConfig.get(AU_PARAM_DISPLAY_NAME);
    }
    stat.setName(name, pluginp);

    if (pluginp == null) {
      stat.setStatus("Error", STATUS_ORDER_ERROR);
      stat.setExplanation("Plugin not found: " +
			  PluginManager.pluginNameFromAuId(auid));
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "stat = " + stat);
      return stat;
    }

    if (addOp == BATCH_ADD_REACTIVATE) {
      // Get the indication of whether the archival unit is inactive.
      boolean isDisabled =
	  oldConfig.getBoolean(PluginManager.AU_PARAM_DISABLED, false);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "isDisabled = " + isDisabled);

      // Check whether the archival unit is already active.
      if (!isDisabled) {
	// Yes: Nothing more to do besides reporting the problem.
	ArchivalUnit au = getAuFromId(auid);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  stat.setStatus("Active", STATUS_ORDER_WARN);
	  stat.setExplanation("AU not inactive: " + au.getName());
	  stat.setConfig(au.getConfiguration());
	} else {
	  stat.setStatus("Error", STATUS_ORDER_ERROR);
	  stat.setExplanation("AU already active but not found: " + auid);
	}

	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "stat = " + stat);
	return stat;
      }

      // No: Make it look like we are just adding a new one.
      auConfig = oldConfig;
      if (auConfig.isSealed()) {
	auConfig = auConfig.copy();
      }
      auConfig.put(PluginManager.AU_PARAM_DISABLED, "false");
      oldConfig = null;
    }

    if (oldConfig != null && !oldConfig.isEmpty()) {
      // have current config, check for disagreement, never create
      stat.setConfig(oldConfig);
      Configuration normOld = normalizedAuConfig(oldConfig);
      Configuration normNew = normalizedAuConfig(auConfig);
      ArchivalUnit au = pluginMgr.getAuFromId(auid);
      if (au != null) {
	stat.setName(au.getName(), au.getPlugin());
      }
      if (normOld.equals(normNew)) {
	if (doCreate) log.debug(addOpName(addOp) + ": same config: " + auid);
	stat.setStatus("Exists", STATUS_ORDER_LOW);
	if (oldConfig.getBoolean(PluginManager.AU_PARAM_DISABLED, false)) {
	  stat.setExplanation("Already Exists (inactive)");
	} else {
	  stat.setExplanation("Already Exists");
	}
      } else {
	if (doCreate) {
	  log.debug(addOpName(addOp) + ": conflicting config: " + auid +
		    ", current: " + normOld + ", new: " + normNew);
	}
	stat.setStatus("Conflict", STATUS_ORDER_ERROR);
	Set<String> diffKeys = normNew.differentKeys(normOld);
	StringBuffer sb = new StringBuffer();
	sb.append("Conflict:<br>");
        for (Iterator iter = diffKeys.iterator(); iter.hasNext(); ) {
          String key = (String)iter.next();
	  ConfigParamDescr descr =
	    (au == null) ? null : au.getPlugin().findAuConfigDescr(key);
	  String foo;
	  if (descr != null &&
	      descr.getType() == ConfigParamDescr.TYPE_USER_PASSWD) {
	    foo = "Key: " + key + ", current=****:****" +
	      ", file=****:****" + "<br>";
	  } else {
	    foo = "Key: " + key + ", current=" + normOld.get(key) +
	      ", file=" + normNew.get(key) + "<br>";
	  }
	  sb.append(foo);
        }
	stat.setExplanation(sb.toString());
      }
    } else if (getAuFromId(auid) != null) {
      // no current config, but AU exists
      stat.setConfig(getAuFromId(auid).getConfiguration());
      stat.setStatus("Error", STATUS_ORDER_ERROR);
      stat.setExplanation("Internal inconsistency: " +
			  "AU exists but is not in config file");
    } else if (!AuUtil.isConfigCompatibleWithPlugin(auConfig,
						    pluginp.getPlugin())) {
      // no current config, new config not compatible with plugin
      stat.setStatus("Error", STATUS_ORDER_ERROR);
      stat.setExplanation("Incompatible with plugin " +
			  pluginp.getPlugin().getPluginName());
      stat.setConfig(auConfig);
    } else {
      // no current config, try to create (maybe)
      try {
	stat.setConfig(auConfig);
	if (auConfig.getBoolean(PluginManager.AU_PARAM_DISABLED, false)) {
	  if (doCreate) {
	    log.debug(addOpName(addOp) + " inactive: " + auid);
	    pluginMgr.updateAuConfigFile(auid, auConfig);
	    stat.setStatus("Added (inactive)", STATUS_ORDER_NORM);
	  } else {
	    stat.setStatus(null, STATUS_ORDER_NORM);
	  }
	} else {
	  if (doCreate) {
	    log.debug(addOpName(addOp) + ": " + auid);
	    AuProxy aup = createAndSaveAuConfiguration(pluginp, auConfig);
	    stat.setStatus("Added", STATUS_ORDER_NORM);
	    stat.setName(aup.getName(), aup.getPlugin());
	    String usrMsg = AuUtil.getConfigUserMessage(aup.getAu());
	    if (usrMsg != null) {
	      stat.setUserMessage(usrMsg);
	    }
	    restoreAuStateFiles(aup, bi);
	  } else {
	    stat.setStatus(null, STATUS_ORDER_NORM);
	  }
	}
      } catch (ArchivalUnit.ConfigurationException e) {
	log.warning("batchProcessOneAu", e);
	log.warning("batchProcessOneAu: " + auid + ", " + auConfig);
	stat.setStatus("Configuration Error", STATUS_ORDER_ERROR);
	stat.setExplanation(e.getMessage());
      } catch (IOException e) {
	stat.setStatus("I/O Error", STATUS_ORDER_ERROR);
	stat.setExplanation(e.getMessage());
      }
    }
    // If a restored AU config has no name, it's probably an old one.  Try
    // to look up the name in the title DB
    if (addOp == BATCH_ADD_RESTORE && stat.getName() == null) {
      stat.setName(titleFromDB(pluginp, auConfig), pluginp);
    }
    if (stat.getName() == null) {
      stat.setName("Unknown");
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "stat = " + stat);
    return stat;
  }

  String titleFromDB(PluginProxy pluginp, Configuration config) {
    TitleConfig tc = AuUtil.findTitleConfig(config, pluginp.getPlugin());
    if (tc != null) {
      return tc.getDisplayName();
    }
    return null;
  }

  void restoreAuStateFiles(AuProxy aup, BackupInfo bi) throws IOException {
    if (bi != null) {
      File auBackDir = bi.getAuDir(aup.getAuId());
      if (auBackDir != null) {
	File agreefile = new File(auBackDir, BACK_FILE_AGREE_MAP);
	if (agreefile.exists()) {
	  InputStream in =
	    new BufferedInputStream(new FileInputStream(agreefile));
	  try {
	    idMgr.readIdentityAgreementFrom(aup.getAu(), in);
	  } finally {
	    IOUtil.safeClose(in);
	  }
	}
      }
    }
  }


  /** Find all AUs in the union of the sets and return a BatchAuStatus with
   * a BatchAuStatus.Entry for each AU indicating whether it could be created,
   * already exists, conflicts, etc.
   */
  public BatchAuStatus findAusInSetsToAdd(Collection sets) {
    BatchAuStatus bas = new BatchAuStatus();
    Set tcs = findAusInSets(sets);
    return findAusInSetsToAdd(bas, tcs.iterator());
  }

  public BatchAuStatus findAusInSetToAdd(TitleSet ts) {
    BatchAuStatus bas = new BatchAuStatus();
    return findAusInSetsToAdd(bas, ts.getTitles().iterator());
  }

  private BatchAuStatus findAusInSetsToAdd(BatchAuStatus bas, Iterator iter) {
    while (iter.hasNext()) {
      TitleConfig tc = (TitleConfig)iter.next();
      if (!paramIncludeDownAus && AuUtil.isPubDown(tc)) {
	// Don't offer to add titles that aren't available anymore.  (This
	// does not affect restore or reactivate)
	continue;
      }
      BatchAuStatus.Entry stat;
      String plugName = tc.getPluginName();
      PluginProxy pluginp = findPluginProxy(plugName);
      if (pluginp == null) {
	stat = bas.newEntry();
	stat.setStatus("Error", STATUS_ORDER_ERROR);
	stat.setExplanation("Plugin not found: " + plugName);
      } else {
	try {
 	  String auid = tc.getAuId(pluginMgr, pluginp.getPlugin());
	  stat =
	    batchProcessOneAu(false, BATCH_ADD_ADD, pluginp,
			      auid, tc.getConfig(), null);
	  stat.setTitleConfig(tc);
	  if ("Unknown".equalsIgnoreCase(stat.getName())) {
	    stat.setName(tc.getDisplayName(), pluginp);
	  }
          bas.add(stat);
	} catch (RuntimeException e) {
	  log.warning("Can't generate auid for: " + tc, e);
	}
      }
    }
    return bas;
  }

  /** Find all AUs in the union of the sets and return a BatchAuStatus with
   * a BatchAuStatus.Entry for each AU indicating whether it could be deleted,
   * does not exist, etc.
   */
  public BatchAuStatus findAusInSetsToDelete(Collection sets) {
    BatchAuStatus bas = new BatchAuStatus();
    Set tcs = findAusInSets(sets);
    return findAusInSetsToDelete(bas, tcs.iterator());
  }

  public BatchAuStatus findAusInSetToDelete(TitleSet ts) {
    BatchAuStatus bas = new BatchAuStatus();
    return findAusInSetsToDelete(bas, ts.getTitles().iterator());
  }

  private BatchAuStatus findAusInSetsToDelete(BatchAuStatus bas,
					      Iterator iter) {
    while (iter.hasNext()) {
      TitleConfig tc = (TitleConfig)iter.next();
      BatchAuStatus.Entry stat = bas.newEntry();
      String plugName = tc.getPluginName();
      stat.setTitleConfig(tc);
      PluginProxy pluginp = findPluginProxy(plugName);
      stat.setName(tc.getDisplayName(), pluginp);
      if (pluginp == null) {
	stat.setStatus("DNE", STATUS_ORDER_LOW);
	stat.setExplanation("Does not exist");
      } else {
	try {
	  String auid = PluginManager.generateAuId(pluginp.getPlugin(),
						   tc.getConfig());
	  stat.setAuid(auid);
	  if (pluginMgr.getAuFromId(auid) == null) {
	    stat.setStatus("DNE", STATUS_ORDER_LOW);
	    stat.setExplanation("Does not exist");
	  }
	} catch (RuntimeException e) {
	  log.warning("Can't generate auid for: " + tc, e);
	  stat.setStatus("DNE", STATUS_ORDER_LOW);
	  stat.setExplanation("Does not exist");
	}
      }
      bas.add(stat);
    }
    return bas;
  }

  /** Find all AUs in the union of the sets and return a BatchAuStatus with
   * a BatchAuStatus.Entry for each AU indicating whether it could be created,
   * already exists, conflicts, etc.
   */
  public BatchAuStatus findAusInSetsToActivate(Collection sets) {
    BatchAuStatus bas = new BatchAuStatus();
    Set tcs = findAusInSets(sets);
    return findAusInSetsToActivate(bas, tcs.iterator());
  }

  public BatchAuStatus findAusInSetToActivate(TitleSet ts) {
    BatchAuStatus bas = new BatchAuStatus();
    return findAusInSetsToActivate(bas, ts.getTitles().iterator());
  }

  private BatchAuStatus findAusInSetsToActivate(BatchAuStatus bas,
						Iterator iter) {
    Collection inactiveAuids = pluginMgr.getInactiveAuIds();
    while (iter.hasNext()) {
      TitleConfig tc = (TitleConfig)iter.next();
      String plugName = tc.getPluginName();
      PluginProxy pluginp = findPluginProxy(plugName);
      if (pluginp != null) {
	try {
	  String auid = PluginManager.generateAuId(pluginp.getPlugin(),
						   tc.getConfig());
	  if (inactiveAuids.contains(auid)) {
	    BatchAuStatus.Entry stat =
	      batchProcessOneAu(false, BATCH_ADD_REACTIVATE, pluginp,
				auid, tc.getConfig(), null);
	    stat.setTitleConfig(tc);
	    if ("Unknown".equalsIgnoreCase(stat.getName())) {
	      stat.setName(tc.getDisplayName(), pluginp);
	    }
	    bas.add(stat);
	  }
	} catch (RuntimeException e) {
	  log.warning("Can't generate auid for: " + tc, e);
	}
      }
    }
    return bas;
  }

  Set findAusInSets(Collection sets) {
    Set res = new HashSet();
    for (Iterator iter = sets.iterator(); iter.hasNext(); ) {
      TitleSet ts = (TitleSet)iter.next();
      try {
	res.addAll(ts.getTitles());
      } catch (Exception e) {
	log.error("Error evaluating TitleSet", e);
      }
    }
    return res;
  }

  static int STATUS_ORDER_ERROR = 1;
  static int STATUS_ORDER_WARN = 2;
  static int STATUS_ORDER_NORM = 3;
  static int STATUS_ORDER_LOW = 4;

  /** Object describing the status of a batch AU config operation
   * (completed or potential).  Basically a list of {@link
   * RemoteApi.BatchAuStatus.Entry}, one for each AU */


  public static class BatchAuStatus {
    private List statusList = new ArrayList<BatchAuStatus.Entry>();
    private List sortedList;
    private int ok = 0;
    private BackupInfo bi;

    public Entry newEntry() {
      return new BatchAuStatus.Entry();
    }
    public Entry newEntry(String auid) {
      return new BatchAuStatus.Entry(auid);
    }

    public void setBackupInfo(BackupInfo bi) {
      this.bi = bi;
    }

    public BackupInfo getBackupInfo() {
      return bi;
    }

    public List<BatchAuStatus.Entry> getUnsortedStatusList() {
      return statusList;
    }

    public List<BatchAuStatus.Entry> getStatusList() {
      if (sortedList == null) {
	Collections.sort(statusList);
	sortedList = statusList;
      }
      return sortedList;
    }
    public int getOkCnt() {
      return ok;
    }
    public void add(BatchAuStatus.Entry status) {
      sortedList = null;
      statusList.add(status);
      if (status.order == STATUS_ORDER_NORM) {
	ok++;
      }
    }
    public boolean hasOk() {
      List lst = getStatusList();
      for (BatchAuStatus.Entry status : getStatusList()) {
	if (status.isOk()) {
	  return true;
	}
      }
      return false;
    }

    public boolean hasNotOk() {
      for (BatchAuStatus.Entry status : getStatusList()) {
	if (!status.isOk()) {
	  return true;
	}
      }
      return false;
    }

    /**
     * <p>Returns true if there are at least a given number
     * of entries that are OK.</p>
     * @param thatMany The minimum number for a "true" return value.
     * @return true if there are at least that many entries that are OK.
     */
    public boolean hasAtLeast(int thatMany) {
      if (hasNotOk()) {
        int size = 0;
	for (BatchAuStatus.Entry rs : getStatusList()) {
          if (rs.isOk()) {
            if (++size >= thatMany) return true;
          }
        }
        return false;
      } else {
        return getStatusList().size() >= thatMany;
      }
    }

    public String toString() {
      return "[bas: " + statusList + "]";
    }

    /** Object describing result or possibility of restoring a single
     * AU from a saved config or title db. */
    public static class Entry implements Comparable {
      private String auid;
      private String name;
      private String status;
      private String explanation;
      private String userMessage;
      private TitleConfig tc;
      private Configuration config;
      private List repoNames;
      private File stateFileDir;
      private int order = 0;

      Entry() {
      }
      Entry(String auid) {
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
      public boolean isOk() {
	if (status == null) {
	  return true;
	} else if (order == STATUS_ORDER_NORM) {
	  return true;
	}
	return false;
      }
      public String getExplanation() {
	return explanation;
      }
      public String getUserMessage() {
	return userMessage;
      }
      public TitleConfig getTitleConfig() {
	return tc;
      }
      public Configuration getConfig() {
	if (config != null) {
	  return config;
	}
	if (tc != null) {
	  return tc.getConfig();
	}
	return null;
      }
      public List getRepoNames() {
	return repoNames;
      }
      public void setRepoNames(List lst) {
	repoNames = lst;
      }
      void setStatus(String s, int order) {
	this.status = s;
	this.order = order;
      }
      void setName(String s) {
	this.name = s;
      }
      void setName(String s, Plugin plugin) {
	if (s != null) {
	  if (plugin == null) {
	    this.name = s;
	  } else {
	    String plat = plugin.getPublishingPlatform();
	    this.name = (plat != null) ? (s + " (" + plat + ")") : s;
	  }
	}
      }
      void setName(String s, PluginProxy pluginp) {
	if (s != null) {
	  if (pluginp == null) {
	    this.name = s;
	  } else {
	    String plat = pluginp.getPublishingPlatform();
	    this.name = (plat != null) ? (s + " (" + plat + ")") : s;
	  }
	}
      }
      void setAuid(String auid) {
	this.auid = auid;
      }
      void setExplanation(String s) {
	this.explanation = s;
      }
      void setUserMessage(String s) {
	this.userMessage = s;
      }
      void setTitleConfig(TitleConfig tc) {
	this.tc = tc;
      }
      void setConfig(Configuration config) {
	this.config = config;
      }
      void setStateFileDir(File dir) {
	this.stateFileDir = dir;
      }
      File getStateFileDir() {
	return stateFileDir;
      }
      public int compareTo(Object o) {
	Entry ostat = (Entry)o;
	int res = order - ostat.order;
	if (res == 0) {
	  res = compareTitle(getName(), ostat.getName());
	}
	return res;
      }
      private int compareTitle(String a, String b) {
 	return coc.compare(a == null ? "" : a, b == null ? "" : b);
      }
      public String toString() {
	return "[" + name + ", " + status + ", " + getConfig() +
	  ", " + getExplanation() +"]";
      }
    }
  }

  public static class BackupInfo {
    File topdir;
    Map auDirs = new HashMap();

    void setAuDir(String auid, File dir) {
      auDirs.put(auid, dir);
    }

    File getAuDir(String auid) {
      return (File)auDirs.get(auid);
    }

    void setTopDir(File topdir) {
      this.topdir = topdir;
    }

    public void delete() {
      if (topdir != null) {
	FileUtil.delTree(topdir);
      }
    }

    public String toString() {
      return "[BI: " + auDirs + "]";
    }
  }

  BackupInfo buildBackupInfo(File dir) throws IOException {
    BackupInfo bi = new BackupInfo();
    bi.setTopDir(dir);
    String[] subdirs = dir.list();
    for (int ix = 0; ix < subdirs.length; ix++) {
      File onedir = new File(dir, subdirs[ix]);
      if (onedir.isDirectory()) {
	Properties auprops = new Properties();
	try {
	  File aupropsfile = new File(onedir, BACK_FILE_AU_PROPS);

	  InputStream in =
	    new BufferedInputStream(new FileInputStream(aupropsfile));
	  try {
	    auprops.load(in);
	  } finally {
	    IOUtil.safeClose(in);
	  }
	  String auid = auprops.getProperty("auid");
	  log.debug("subdir: " + onedir + ", auid: " + auid);
	  if (!StringUtil.isNullString(auid)) {
	    bi.setAuDir(auid, onedir);
	  }
	} catch (IOException e) {
	  log.warning("Building BackupInfo", e);
	}
      }
    }
    return bi;
  }

  /** Exception thrown if the uploaded AU config backup file isn't valid */
  public static class InvalidAuConfigBackupFile extends Exception {
    public InvalidAuConfigBackupFile(String message) {
      super(message);
    }
  }

  /** Comparator for sorting AuProxy lists.  Not suitable for use in a
   * TreeSet unless changed to never return 0. */
  static class AuProxyOrderComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      AuProxy a1 = (AuProxy)o1;
      AuProxy a2 = (AuProxy)o2;
      int res = coc.compare(a1.getName(), a2.getName());
      if (res == 0) {
	res = a1.getAuId().compareTo(a2.getAuId());
      }
      return res;
    }
  }


  // Support for mailing backup file
  // XXX Need to find a place for this.  o.l.daemon.admin?

  static final String BACKUP_PREFIX =
    Configuration.PREFIX + "backup.";

  static final String BACKUP_EMAIL_PREFIX =
    Configuration.PREFIX + "backupEmail.";

  /** Frequency of creating backup file; only supports "weekly" and
   * "monthly" */
  public static final String PARAM_BACKUP_FREQ =
    BACKUP_PREFIX + "frequency";
  public static final String DEFAULT_BACKUP_FREQ = "monthly";

  /** Directory into which backup files are written.  Can be absolute, or
   * relative to daemon's cwd. */
  public static final String PARAM_BACKUP_DIR = BACKUP_PREFIX + "dir";
  static final String DEFAULT_BACKUP_DIR = "<first_disk>/backup";

  /** Backup file name. */
  static final String PARAM_BACKUP_FILENAME = BACKUP_PREFIX + "fileName";
  static final String DEFAULT_BACKUP_FILENAME = "config_backup.zip";


  public enum BackupFileDisposition {None, Mail, Keep, MailAndKeep};

  /** Backup file dispostion: None, Mail, Keep or MailAndKeep */
  static final String PARAM_BACKUP_DISPOSITION =
    BACKUP_PREFIX + "disposition";
  static final BackupFileDisposition DEFAULT_BACKUP_DISPOSITION =
    BackupFileDisposition.Mail;

  /** Enable periodic mailing of backup file */
  static final String PARAM_BACKUP_EMAIL_ENABLED =
    BACKUP_EMAIL_PREFIX + "enabled";
  static final boolean DEFAULT_BACKUP_EMAIL_ENABLED = false;

  /** Frequency of periodic mailing of backup file; only supports "weekly"
   * and "monthly".
   * @deprecated - use {@value #PARAM_BACKUP_FREQ} instead}
   */
  public static final String PARAM_BACKUP_EMAIL_FREQ =
    BACKUP_EMAIL_PREFIX + "frequency";
  public static final String DEFAULT_BACKUP_EMAIL_FREQ = "monthly";

  /** If sepcified, the recipient of backup emails.    If not specified,
   * uses the admin email adress */
  static final String PARAM_BACKUP_EMAIL_RECIPIENT =
    BACKUP_EMAIL_PREFIX + "sendTo";
  static final String PARAM_DEFAULT_BACKUP_EMAIL_RECIPIENT =
    ConfigManager.PARAM_PLATFORM_ADMIN_EMAIL;
  // Make default param name show up as default in param listing.
  static final String DEFAULT_BACKUP_EMAIL_RECIPIENT =
    "(value of " + PARAM_DEFAULT_BACKUP_EMAIL_RECIPIENT + ")";

  /** If sepcified, the sender address on backup emails.  If not specified,
   * uses the admin email adress */
  static final String PARAM_BACKUP_EMAIL_SENDER =
    BACKUP_EMAIL_PREFIX + "sender";
  static final String PARAM_DEFAULT_BACKUP_EMAIL_SENDER =
    ConfigManager.PARAM_PLATFORM_ADMIN_EMAIL;
  // Make default param name show up as default in param listing.
  static final String DEFAULT_BACKUP_EMAIL_SENDER =
    "(value of " + PARAM_DEFAULT_BACKUP_EMAIL_SENDER + ")";

  /** printf string applied to cache-name, email-sender */
  static final String PARAM_BACKUP_EMAIL_FROM =
    BACKUP_EMAIL_PREFIX + "from";
  static final String DEFAULT_BACKUP_EMAIL_FROM = "LOCKSS box %s <%s>";

  /** URL for more info about backup file */
  static final String PARAM_BACKUP_EMAIL_INFO_URL =
    BACKUP_EMAIL_PREFIX + "infoUrl";
  static final String DEFAULT_BACKUP_EMAIL_INFO_URL = null;



  DateFormat headerDf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"
					     /*, Locale.US */);
//   headerDf.setTimeZone(TimeZone.getTimeZone("GMT"));

  static final String BACK_MAIL_TEXT =
    "The attached file is a backup of configuration and state information\n" +
    "from LOCKSS box %s (%s).\n" +
    "This file will be helpful if the cache ever suffers a disk crash or\n" +
    "other serious loss of content.\n\n" +
    "(If your mail system blocked the attachment, you may retrieve the file\n"+
    "at %s )\n";

  static final String BACK_MAIL_MORE_INFO =
    "\nFor more information see %s\n";


  protected void doPeriodicBackupFile() throws IOException {
    Configuration config = ConfigManager.getCurrentConfig();
    BackupFileDisposition bfd =
      (BackupFileDisposition)config.getEnum(BackupFileDisposition.class,
					    PARAM_BACKUP_DISPOSITION,
					    DEFAULT_BACKUP_DISPOSITION);
    createConfigBackupFile(bfd);
  }

  public void createConfigBackupFile(BackupFileDisposition bfd)
      throws IOException {
    log.debug2("createConfigBackupFile: " + bfd);
    Configuration config = ConfigManager.getCurrentConfig();
    File permFile = null;
    boolean delete = true;

    switch (bfd) {
    case None:
      return;
    case Keep:
    case MailAndKeep:
      permFile = getBackupFile(config);
      delete = false;
    }
    File bfile = createConfigBackupFile(permFile, getMachineName());
    switch (bfd) {
    case MailAndKeep:
    case Mail:
      mailBackupFile(bfile, getBackEmailRecipient(config), delete);
    }
  }

  public File getBackupFile() throws IOException {
    return getBackupFile(ConfigManager.getCurrentConfig());
  }

  public File getBackupFile(Configuration config) throws IOException {
    File dir = getBackupDir(config);
    if (dir == null) {
      return null;
    }
    String fname = config.get(PARAM_BACKUP_FILENAME, DEFAULT_BACKUP_FILENAME);
    return new File(dir, fname);
  }

  File getBackupDir(Configuration config) throws IOException {
    String dirname = config.get(PARAM_BACKUP_DIR);
    if (dirname != null) {
      File dir = new File(dirname);
      if (!dir.exists()) {
	if (!dir.mkdirs()) {
	  throw new IOException("Can't create backup dir: " + dir);
	}
      }
      if (!dir.isDirectory()) {
	throw new IOException("Backup dir is an existing non-directory file: " +
			      dir);
      }
      if (!dir.canWrite()) {
	throw new IOException("Can't write to backup dir: " + dir);
      }
      return dir;
    }
    return null;
  }


  String getMachineName() {
    String machineName = PlatformUtil.getLocalHostname();
    if (StringUtil.isNullString(machineName)) {
      machineName = "Unknown";
    }
    return machineName;
  }

  public void mailBackupFile(File file, String to, boolean deleteFile)
      throws IOException {
    MailService mailSvc = getDaemon().getMailService();
    MimeMessage msg = new MimeMessage();
    Configuration config = ConfigManager.getCurrentConfig();

    String id =
      CurrentConfig.getParam(ConfigManager.PARAM_PLATFORM_IP_ADDRESS,
			     "unknown");
    String machineName = getMachineName();
    String text =
      String.format(BACK_MAIL_TEXT,
		    machineName, id, ServletUtil.backupFileUrl(machineName));
    String moreInfoUrl = config.get(PARAM_BACKUP_EMAIL_INFO_URL,
				    DEFAULT_BACKUP_EMAIL_INFO_URL);
    if (!StringUtil.isNullString(moreInfoUrl)) {
      text += String.format(BACK_MAIL_MORE_INFO, moreInfoUrl);
    }
    msg.addTextPart(text);
    if (deleteFile) {
      msg.addTmpFile(file, getAuConfigBackupFileName());
    } else {
      msg.addFile(file, getAuConfigBackupFileName());
    }
    msg.addHeader("From", getBackEmailFrom(config, machineName));
    msg.addHeader("To", to);
    msg.addHeader("Date", headerDf.format(TimeBase.nowDate()));
    msg.addHeader("Subject",
		  "Backup file for LOCKSS box " + machineName);
    //     msg.addHeader("X-Mailer", getXMailer());
    mailSvc.sendMail(getBackEmailSender(config), to, msg);
    log.info("sent");
  }

  public String getAuConfigBackupFileName() {
    String machineName = PlatformUtil.getLocalHostname();
    if (StringUtil.isNullString(machineName)) {
      machineName = "Unknown";
    }
    return getAuConfigBackupFileName(machineName);
  }

  public String getAuConfigBackupFileName(String machineName) {
    return "LOCKSS_Backup_" + machineName + paramBackupFileDotExtension;
  }

  private String getBackEmailSender(Configuration config) {
    String defaultSender = config.get(PARAM_DEFAULT_BACKUP_EMAIL_SENDER);
    String res = config.get(PARAM_BACKUP_EMAIL_SENDER, defaultSender);
    return StringUtil.isNullString(res) ? "Unknown" : res;
  }

  private String getBackEmailRecipient(Configuration config) {
    String defaultRecipient = config.get(PARAM_DEFAULT_BACKUP_EMAIL_RECIPIENT);
    String res = config.get(PARAM_BACKUP_EMAIL_RECIPIENT, defaultRecipient);
    return StringUtil.isNullString(res) ? "Unknown" : res;
  }

  private String getBackEmailFrom(Configuration config, String machineName) {
    try {
      String fmt = config.get(PARAM_BACKUP_EMAIL_FROM,
			      DEFAULT_BACKUP_EMAIL_FROM);
      return String.format(fmt, machineName, getBackEmailSender(config));
    } catch (Exception e) {
      log.warning("getBackEmailFrom()", e);
      return "LOCKSS box";
    }
  }

  /** Cron.Task to periodically generate and optionally mail a backup
   * file. */
  public static class CreateBackupFile extends Cron.BaseTask {

    public CreateBackupFile(LockssDaemon daemon) {
      super(daemon);
    }

    public String getId() {
      return "CreateBackup";
    }

    public long nextTime(long lastTime) {
      return nextTime(lastTime,
		      CurrentConfig.getParam(PARAM_BACKUP_FREQ,
					     DEFAULT_BACKUP_FREQ));
    }

    public boolean execute() {
      RemoteApi rmtApi = daemon.getRemoteApi();
      try {
	rmtApi.doPeriodicBackupFile();
	return true;
      } catch (IOException e) {
	log.warning("Failed to create config backup", e);
      }
      return true;
    }
  }

  /**
   * Configures an archival unit without additional parameters.
   *
   * @param auId A String with the archival unit identifier.
   * @return a BatchAuStatus with the result of the operation.
   */
  public BatchAuStatus addByAuId(String auId) {
    return addByAuId(auId, null);
  }

  /**
   * Configures an archival unit with additional parameters.
   *
   * @param auId A String with the archival unit identifier.
   * @param addConfig A Configuration with additional parameters.
   * @return a BatchAuStatus with the result of the operation.
   */
  public BatchAuStatus addByAuId(String auId, Configuration addConfig) {
    final String DEBUG_HEADER = "addByAuId(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      log.debug2(DEBUG_HEADER + "addConfig = " + addConfig);
    }

    // The status object to be returned.
    BatchAuStatus status = new BatchAuStatus();
    BatchAuStatus.Entry statusEntry = status.newEntry(auId);

    // Check whether no archival unit identifier was passed.
    if (StringUtil.isNullString(auId)) {
      // Yes: Report the problem.
      statusEntry.setName(auId);
      statusEntry.setStatus("Not Configured", STATUS_ORDER_WARN);
      statusEntry.setExplanation("AU identifier not supplied");
      status.add(statusEntry);

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
      return status;
    }

    // Get the identified archival unit.
    ArchivalUnit au = pluginMgr.getAuFromId(auId);

    // Check whether the archival unit already exists.
    if (au != null) {
      // Yes: Report the problem.
      statusEntry.setName(au.getName(), au.getPlugin());
      statusEntry.setStatus("Configured", STATUS_ORDER_WARN);
      statusEntry.setExplanation("AU already exists: " + au.getName());
      status.add(statusEntry);

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
      return status;
    }

    // Get the title configuration of the archival unit.
    TitleConfig tc = findTitleConfig(auId);

    // Check whether no title configuration exists.
    if (tc == null) {
      // Yes: Report the problem.
      statusEntry.setName(auId);
      statusEntry.setStatus("Not Configured", STATUS_ORDER_WARN);
      statusEntry.setExplanation("No matching AU definition found in title db: "
	  + auId);
      status.add(statusEntry);

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
      return status;
    }

    try {
      // Get the plugin.
      Plugin plugin =
	pluginMgr.getPlugin(PluginManager.pluginKeyFromId(tc.getPluginName()));

      // Get the archival unit configuration.
      Configuration auConfig = tc.getConfig();

      // Check whether there are additional parameters.
      if (addConfig != null && !addConfig.isEmpty()) {
	// Yes: Merge them.
	auConfig = auConfig.copy();
	auConfig.copyFrom(addConfig);
      }

      // Add the archival unit.
      au = pluginMgr.createAndSaveAuConfiguration(plugin, auConfig);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Created Archival Unit " + au.getName());

      statusEntry.setName(au.getName(), au.getPlugin());
      statusEntry.setExplanation("Created Archival Unit:\n" + au.getName());
      status.add(statusEntry);
    } catch (ArchivalUnit.ConfigurationException ce) {
      log.error("Couldn't create AU", ce);
      statusEntry.setName(auId);
      statusEntry.setStatus("Not Configured", STATUS_ORDER_WARN);
      statusEntry.setExplanation("Error creating AU: " + ce.getMessage());
      status.add(statusEntry);
    } catch (IOException ioe) {
      log.error("Couldn't create AU", ioe);
      statusEntry.setName(auId);
      statusEntry.setStatus("Not Configured", STATUS_ORDER_WARN);
      statusEntry.setExplanation("Error creating AU: " + ioe.getMessage());
      status.add(statusEntry);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
    return status;
  }

  /**
   * Provides the title configuration of an archival unit.
   *
   * @param auId A String with the archival unit identifier.
   * @return a TitleConfig with the title configuration of the archival unit.
   */
  public TitleConfig findTitleConfig(String auId) {
    // Loop  through all the title configurations.
    for (TitleConfig tc : pluginMgr.findAllTitleConfigs()) {
      // Check whether this is the title configuration of the archival unit.
      if (auId.equals(tc.getAuId(pluginMgr))) {
	// Yes: Done.
	return tc;
      }
    }

    // No title configuration was found.
    return null;
  }

  /**
   * Adds or reactivates archival units.
   * 
   * @param addOp
   *          An int with the operation to be performed.
   * @param auIds
   *          A String[] with the identifiers of the archival units.
   * @param repoMap
   *          A LinkedMap with the repositories.
   * @param defaultRepoIndex
   *          A String with the index of the default repository.
   * @param auConfigs
   *          A Map<String, Configuration> with the configurations of the
   *          archival units.
   * @param auRepoIndices
   *          A Map<String, String> with the repository indices of the archival
   *          units.
   * @param bi
   *          A BackupInfo with the backup information.
   * @return a BatchAuStatus object describing the results.
   */
  public BatchAuStatus batchAddAus(int addOp, String[] auIds, LinkedMap repoMap,
      String defaultRepoIndex, Map<String, Configuration> auConfigs,
      Map<String, String> auRepoIndices, BackupInfo bi) {
    final String DEBUG_HEADER = "batchAddAus(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "addOp = " + addOp);
      log.debug2(DEBUG_HEADER + "auIds = " + Arrays.asList(auIds));
      log.debug2(DEBUG_HEADER + "repoMap = " + repoMap);
      log.debug2(DEBUG_HEADER + "defaultRepoIndex = " + defaultRepoIndex);
      log.debug2(DEBUG_HEADER + "auConfigs = " + auConfigs);
      log.debug2(DEBUG_HEADER + "auRepoIndices = " + auRepoIndices);
      log.debug2(DEBUG_HEADER + "bi = " + bi);
    }

    // The default repository, used for those archival units that do not have
    // their own repository defined.
    String defaultRepo = null;

    // Check whether no default repository index was passed.
    if (StringUtil.isNullString(defaultRepoIndex)) {
      // Yes: Use the repository least full as the default one.
      defaultRepo = findLeastFullRepository(getRepositoryMap());
      // No: Check whether a repostory map was passed.
    } else if (repoMap != null) {
      // Yes: Get the default repository by the passed index.
      try {
	int n = Integer.parseInt(defaultRepoIndex);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "n = " + n);

	defaultRepo = (String)repoMap.get(n - 1);
      } catch (NumberFormatException e) {
	log.warning("Invalid default repository index: " + defaultRepoIndex, e);
      } catch (IndexOutOfBoundsException e) {
	log.warning("Illegal default repository index: " + defaultRepoIndex, e);
      }
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "defaultRepo = " + defaultRepo);

    // Initialize the configuration of the archival units to be processed.
    Configuration createConfig = ConfigManager.newConfiguration();

    // Loop through all the archival units.
    for (int i = 0; i < auIds.length; i++) {
      String auId = auIds[i];
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

      Configuration tcConfig = (Configuration)auConfigs.get(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auRepository = "
	  + tcConfig.get(PluginManager.AU_PARAM_REPOSITORY));

      // Remove the repository from the archival unit configuration.
      tcConfig.remove(PluginManager.AU_PARAM_REPOSITORY);

      // Try to use the passed repository.
      String repoIndex = auRepoIndices.get(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "repoIndex = " + repoIndex);

      if (!StringUtil.isNullString(repoIndex) && repoMap != null) {
	try {
	  int n = Integer.parseInt(repoIndex);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "n = " + n);

	  tcConfig.put(PluginManager.AU_PARAM_REPOSITORY,
	      (String)repoMap.get(n - 1));
	} catch (NumberFormatException e) {
	  log.warning("Invalid AU repository index: " + repoIndex, e);
	} catch (IndexOutOfBoundsException e) {
	  log.warning("Illegal AU repository index: " + repoIndex, e);
	}
      }

      // If the passed repository could not be used, use the default one.
      if (defaultRepo != null &&
	  !tcConfig.containsKey(PluginManager.AU_PARAM_REPOSITORY)) {
	tcConfig.put(PluginManager.AU_PARAM_REPOSITORY, defaultRepo);
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auRepository = "
	  + tcConfig.get(PluginManager.AU_PARAM_REPOSITORY));

      // Add the archival unit configuration to the overall configuration.
      String prefix = PluginManager.auConfigPrefix(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);

      createConfig.addAsSubTree(tcConfig, prefix);
    }

    if (log.isDebug3()) log.debug3("createConfig: " + createConfig);

    // Add or reactivate the archival units.
    BatchAuStatus bas = batchAddAus(addOp, createConfig, bi);
    if (log.isDebug2()) log.debug2("bas = " + bas);

    return bas;
  }

  /**
   * Reactivates archival units.
   * 
   * @param auIds
   *          A List<String> with the identifiers of the archival units.
   * @return a BatchAuStatus object describing the results.
   */
  public BatchAuStatus reactivateAus(List<String> auIds) {
    final String DEBUG_HEADER = "reactivateAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auIds = " + auIds);

    Map<String, Configuration> auConfigs = new HashMap<String, Configuration>();

    // Loop through all the identifiers of the archival units to be reactivated.
    for (String auId : auIds) {
      // Store the archival unit configuration in the map.
      Configuration auConfig = pluginMgr.getStoredAuConfiguration(auId);

      if (auConfig.isSealed()) {
	auConfig = auConfig.copy();
      }

      auConfig.put(PluginManager.AU_PARAM_DISABLED, "false");
      auConfigs.put(auId, auConfig);
    }

    // Reactivate the archival units.
    BatchAuStatus bas = batchAddAus(BATCH_ADD_REACTIVATE,
	auIds.toArray(new String[auIds.size()]), null, null, auConfigs,
	new HashMap<String, String>(), null);

    if (log.isDebug2()) log.debug2("bas = " + bas);
    return bas;
  }
}
