/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.lang3.SystemUtils;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * LockssRepository is used to organize the urls being cached.
 * It keeps a memory cache of the most recently used nodes as a
 * least-recently-used map, and also caches weak references to the instances
 * as they're doled out.  This ensures that two instances of the same node are
 * never created, as the weak references only disappear when the object is
 * finalized (they go to null when the last hard reference is gone, then are
 * removed from the cache on finalize()).
 */
public class LockssRepositoryImpl
  extends BaseLockssDaemonManager implements LockssRepository {
  
  private static final Logger logger = Logger.getLogger("LockssRepository");

  /**
   * Configuration parameter name for Lockss cache location.
   */
  public static final String PARAM_CACHE_LOCATION =
    Configuration.PREFIX + "cache.location";

  /** Restores pre repo rescanning bugfix behavior (4050) */
  public static final String PARAM_CLEAR_DIR_MAP =
    RepositoryManager.PREFIX + "clearDirMapOnAuStop";
  public static final boolean DEFAULT_CLEAR_DIR_MAP = false;

  /**
   * Name of top directory in which the urls are cached.
   */
  public static final String CACHE_ROOT_NAME = "cache";

  // XXX This is a remnant from the single-disk days, and should go away.
  // It is used only by unit tests, which want it set to the (last)
  // individual repository dir created.
  private static String staticCacheLocation = null;

  // Maps local repository name (disk path) to LocalRepository instance
  static Map<String,LocalRepository> localRepositories =
    new HashMap<String,LocalRepository>();

  // starts with a '#' so no possibility of clashing with a URL
  public static final String AU_ID_FILE = "#au_id_file";
  public static final String SUSPECT_VERSIONS_FILE = "#suspect_versions";
  static final String AU_ID_PROP = "au.id";
  static final String PLUGIN_ID_PROP = "plugin.id";
  static final char ESCAPE_CHAR = '#';
  static final String ESCAPE_STR = "#";
  static final char ENCODED_SEPARATOR_CHAR = 's';
  static final String INITIAL_PLUGIN_DIR = String.valueOf((char)('a'-1));
  static String lastPluginDir = INITIAL_PLUGIN_DIR;
  // PJG: Windows prohibits use of ':' in file name -- replace with '~' for development 
  static final String PORT_SEPARATOR = SystemUtils.IS_OS_WINDOWS ? "%" : ":";
  
  // this contains a '#' so that it's not defeatable by strings which
  // match the prefix in a url (like '../tmp/')
  private static final String TEST_PREFIX = "/#tmp";

  private RepositoryManager repoMgr;
  private String rootLocation;
  UniqueRefLruCache nodeCache;
  private boolean isGlobalNodeCache =
    RepositoryManager.DEFAULT_GLOBAL_CACHE_ENABLED;

  LockssRepositoryImpl(String rootPath) {
    if (rootPath.endsWith(File.separator)) {
      rootLocation = rootPath;
    } else {
      // shouldn't happen
      StringBuilder sb = new StringBuilder(rootPath.length() +
					 File.separator.length());
      sb.append(rootPath);
      sb.append(File.separator);
      rootLocation = sb.toString();
    }
    // Test code still needs this.
    nodeCache =
      new UniqueRefLruCache(RepositoryManager.DEFAULT_MAX_PER_AU_CACHE_SIZE);
  }

  public void startService() {
    super.startService();
    repoMgr = getDaemon().getRepositoryManager();
    isGlobalNodeCache = repoMgr.isGlobalNodeCache();
    if (isGlobalNodeCache) {
      nodeCache = repoMgr.getGlobalNodeCache();
    } else {
//       nodeCache =
// 	new UniqueRefLruCache(repoMgr.paramNodeCacheSize);
      setNodeCacheSize(repoMgr.paramNodeCacheSize);
    }
  }

  public void stopService() {
    // mainly important in testing to blank this
    lastPluginDir = INITIAL_PLUGIN_DIR;
    if (CurrentConfig.getBooleanParam(PARAM_CLEAR_DIR_MAP,
				      DEFAULT_CLEAR_DIR_MAP)) {
      // This should be in RepositoryManager.stopService()
      localRepositories = new HashMap<String,LocalRepository>();
    }
    super.stopService();
  }

  public void setNodeCacheSize(int size) {
    if (nodeCache != null && !isGlobalNodeCache &&
	nodeCache.getMaxSize() != size) {
      nodeCache.setMaxSize(size);
    }
  }

  /** Called between initService() and startService(), then whenever the
   * AU's config changes.
   * @param auConfig the new configuration
   */
  public void setAuConfig(Configuration auConfig) {
  }

  void queueSizeCalc(RepositoryNode node) {
    repoMgr.queueSizeCalc(node);
  }

  public RepositoryNode getNode(String url) throws MalformedURLException {
    return getNode(url, false);
  }

  public RepositoryNode createNewNode(String url) throws MalformedURLException {
    return getNode(url, true);
  }

  public void deleteNode(String url) throws MalformedURLException {
    RepositoryNode node = getNode(url, false);
    if (node!=null) {
      node.markAsDeleted();
    }
  }

  public void deactivateNode(String url) throws MalformedURLException {
    RepositoryNode node = getNode(url, false);
    if (node!=null) {
      node.deactivateContent();
    }
  }

  /**
   * This function returns a RepositoryNode with a canonicalized path.
   * @param url the url in String form
   * @param create true iff the node should be created if absent
   * @return RepositoryNode the node
   * @throws MalformedURLException
   */
  private RepositoryNode getNode(String url, boolean create)
      throws MalformedURLException {
    String canonUrl;
    boolean isAuUrl = false;
    if (AuUrl.isAuUrl(url)) {
      // path information is lost here, but is unimportant if it's an AuUrl
      canonUrl = AuUrl.PROTOCOL;
      isAuUrl = true;
    } else {
      // create a canonical path, handling all illegal path traversal
      canonUrl = canonicalizePath(url);
    }

    // check LRUMap cache for node
    RepositoryNode node = (RepositoryNode)nodeCache.get(nodeCacheKey(canonUrl));
    if (node!=null) {
      return node;
    }

    // Create a new node.  Don't change any other state yet so don't have
    // to synchronize.
    String nodeLocation;
    if (isAuUrl) {
      // base directory of ArchivalUnit
      nodeLocation = rootLocation;
      node = new AuNodeImpl(canonUrl, nodeLocation, this);
    } else {
      // determine proper node location
      nodeLocation = LockssRepositoryImpl.mapUrlToFileLocation(rootLocation,
          canonUrl);
      node = new RepositoryNodeImpl(canonUrl, nodeLocation, this);
    }

    if (!create) {
      // if not creating, check for existence
      File nodeDir = new File(nodeLocation);
      if (!nodeDir.exists()) {
        // return null if the node doesn't exist and shouldn't be created
        return null;
      }
      if (!nodeDir.isDirectory()) {
        logger.error("Cache file not a directory: "+nodeLocation);
        throw new LockssRepository.RepositoryStateException("Invalid cache file.");
      }
    }

    // add to node cache, or discard and return existing node if someone
    // added one while we were creating ours
    return (RepositoryNode)nodeCache.putIfNew(nodeCacheKey(canonUrl), node);
  }

  Object nodeCacheKey(String canonUrl) {
    if (isGlobalNodeCache) {
      return new KeyPair(this, canonUrl);
    }
    return canonUrl;
  }

  // functions for testing
  int getCacheHits() { return nodeCache.getCacheHits(); }
  int getCacheMisses() { return nodeCache.getCacheMisses(); }
  int getRefHits() { return nodeCache.getRefHits(); }
  int getRefMisses() { return nodeCache.getRefMisses(); }

  public void nodeConsistencyCheck() {
    // traverse the node tree from the top
    RepositoryNode topNode;
    try {
      topNode = getNode(AuUrl.PROTOCOL_COLON);
      recurseConsistencyCheck((RepositoryNodeImpl)topNode);
    } catch (MalformedURLException ignore) { }
  }

  /**
   * Checks the consistency of the node, and continues with its children
   * if it's consistent.
   * @param node RepositoryNodeImpl the node to check
   */
  private void recurseConsistencyCheck(RepositoryNodeImpl node) {
    logger.debug2("Checking node '"+node.getNodeUrl()+"'...");
    // check consistency at each node
    // correct/deactivate as necessary
    // 'checkNodeConsistency()' will repair if possible
    if (node.checkNodeConsistency()) {
      logger.debug3("Node consistent; recursing on children...");
      List children = node.getNodeList(null, false);
      Iterator iter = children.iterator();
      while (iter.hasNext()) {
        RepositoryNodeImpl child = (RepositoryNodeImpl)iter.next();
        recurseConsistencyCheck(child);
      }
    } else {
      logger.debug3("Node inconsistent; deactivating...");
      deactivateInconsistentNode(node);
    }
  }

  /**
   * This is called when a node is in an inconsistent state.  It simply creates
   * some necessary directories and deactivates the node.  Future polls should
   * restore it properly.
   * @param node the inconsistent node
   */
  void deactivateInconsistentNode(RepositoryNodeImpl node) {
    logger.warning("Deactivating inconsistent node.");
    FileUtil.ensureDirExists(node.contentDir);
    // manually deactivate
    node.deactivateContent();
  }

  /**
   * A method to remove any non-canonical '..' or '.' elements in the path,
   * as well as protecting against illegal path traversal.
   * @param url the raw url
   * @return String the canonicalized url
   * @throws MalformedURLException
   */
  public String canonicalizePath(String url)
      throws MalformedURLException {
    String canonUrl =
      UrlUtil.normalizeUrl(url, UrlUtil.PATH_TRAVERSAL_ACTION_THROW);
    // canonicalize "dir" and "dir/"
    // XXX if these are ever two separate nodes, this is wrong
    if (canonUrl.endsWith(UrlUtil.URL_PATH_SEPARATOR)) {
      canonUrl = canonUrl.substring(0, canonUrl.length()-1);
    }

    return canonUrl;
  }

  // static calls

  /**
   * Factory method to create new LockssRepository instances.
   * @param au the {@link ArchivalUnit}
   * @return the new LockssRepository instance
   */
  public static LockssRepository createNewLockssRepository(ArchivalUnit au) {
    String root = getRepositoryRoot(au);
    if (root == null || root.equals("null")) {
      logger.error("No repository dir set in config");
      throw new LockssRepository.RepositoryStateException("No repository dir set in config");
    }
    String auDir = LockssRepositoryImpl.mapAuToFileLocation(root, au);
    if (logger.isDebug2()) {
      logger.debug2("repo: " + auDir + ", au: " + au.getName());
    }
    staticCacheLocation = extendCacheLocation(root);
    LockssRepositoryImpl repo = new LockssRepositoryImpl(auDir);
    Plugin plugin = au.getPlugin();
    if (plugin != null) {
      LockssDaemon daemon = plugin.getDaemon();
      if (daemon != null) {
	RepositoryManager mgr = daemon.getRepositoryManager();
	if (mgr != null) {
	  mgr.setRepositoryForPath(auDir, repo);
	}
      }
    }
    return repo;
  }

  public static String getRepositorySpec(ArchivalUnit au) {
    Configuration auConfig = au.getConfiguration();
    if (auConfig != null) {		// can be null in unit tests
      String repoSpec = auConfig.get(PluginManager.AU_PARAM_REPOSITORY);
      if (repoSpec != null && repoSpec.startsWith("local:")) {
	return repoSpec;
      }
    }
    return "local:" + CurrentConfig.getParam(PARAM_CACHE_LOCATION);
  }

  public static String getRepositoryRoot(ArchivalUnit au) {
    return getLocalRepositoryPath(getRepositorySpec(au));
  }

  public static String getLocalRepositoryPath(String repoSpec) {
    if (repoSpec != null) {
      if (repoSpec.startsWith("local:")) {
	return repoSpec.substring(6);
      }
    }
    return null;
  }

  /**
   * Return the AuSuspectUrlVersions object for the AU, that can
   * be used to mark versions of urls within the AU as suspect,
   * and to determine whether a version of a url within the AU has
   * been so marked.
   * @param au the AU
   * @return AuSuspectUrlversions object for the AU
   */
  public AuSuspectUrlVersions getSuspectUrlVersions(ArchivalUnit au) {
    UniqueRefLruCache cache = repoMgr.getSuspectVersionsCache();
    AuSuspectUrlVersions asuv = (AuSuspectUrlVersions)cache.get(au);
    if (asuv != null) {
      return asuv;
    }
    // not in cache.  Load from file or create a new one and insert in cache.
    synchronized (this) {
      asuv = loadSuspectUrlVersions(au);
      return (AuSuspectUrlVersions)cache.putIfNew(au, asuv);
    }
  }

  /**
   * Return true if the AU has a record of suspect URL versions, even if
   * that record is empty.  Does not put anything in the cache so can be
   * used in iteration over AUs.
   */
  public boolean hasSuspectUrlVersions(ArchivalUnit au) {
    AuSuspectUrlVersions asuv =
      (AuSuspectUrlVersions)repoMgr.getSuspectVersionsCache().get(au);
    if (asuv != null) {
      return !asuv.isEmpty();
    } else {
      return getAsuvFile().exists();
    }
  }

  private AuSuspectUrlVersions loadSuspectUrlVersions(ArchivalUnit au) {
    ObjectSerializer deserializer = makeObjectSerializer();
    try {
      if (logger.isDebug3())
	logger.debug3("Loading suspect versions for " + au);
      File file = getAsuvFile();
      AuSuspectUrlVersions asuv =
	(AuSuspectUrlVersions)deserializer.deserialize(file);
      return asuv;
    } catch (SerializationException.FileNotFound fnf) {
      if (logger.isDebug2())
	logger.debug2("Creating new suspect versions for AU " + au);
      // fall through to return new AuSuspectUrlVersions
    } catch (SerializationException e) {
      logger.error("Loading suspect versions for " + au, e);
      // fall through to return new AuSuspectUrlVersions
    } catch (InterruptedIOException e) {
      logger.error("Loading suspect versions for " + au, e);
      throw new RuntimeException("Could not load suspect versions", e);
    }
    // Return new object
    return new AuSuspectUrlVersions();
  }

  private ObjectSerializer makeObjectSerializer() {
    return new XStreamSerializer();
  }

  public void storeSuspectUrlVersions(ArchivalUnit au,
				      AuSuspectUrlVersions asuv)
      throws SerializationException {
    try {
      File file = getAsuvFile();
      makeObjectSerializer().serialize(file, asuv);
    } catch (SerializationException e) {
      logger.error("Could not store suspect versions", e);
      throw e;
    } catch (InterruptedIOException e) {
      logger.error("Could not store suspect versions", e);
      throw new SerializationException(e);
    }
  }

  File getAsuvFile() {
    return new File(rootLocation, SUSPECT_VERSIONS_FILE);
  }


  // The OpenBSD platform has renamed the first disk from /cache to
  // /cache.wd0, leaving behind a symbolic link in /cache .  This is
  // transparent everywhere except the repository status table, which needs
  // to match AU configs with AUs it finds when enumerating the repository.
  // Existing AU configs have repository=local:/cache, so the relative link
  // needs to be resolved to detect that that's the same as
  // local:/cache.wd0

  private static Map canonicalRoots = new HashMap();

  public static boolean isDirInRepository(String dir, String repoRoot) {
    if (dir.startsWith(repoRoot)) {
      return true;
    }
    return canonRoot(dir).startsWith(canonRoot(repoRoot));
  }

  static String canonRoot(String root) {
    synchronized (canonicalRoots) {
      String canon = (String)canonicalRoots.get(root);
      if (canon == null) {
	try {
	  canon = new File(root).getCanonicalPath();
	  canonicalRoots.put(root, canon);
	} catch (IOException e) {
	  logger.warning("Can't canonicalize: " + root, e);
	  return root;
	}
      }
      return canon;
    }
  }

  static String getCacheLocation() {
    return staticCacheLocation;
  }

  /**
   * Adds the 'cache' directory to the HD location.
   * @param cacheDir the root location.
   * @return String the extended location
   */
  static String extendCacheLocation(String cacheDir) {
    StringBuilder buffer = new StringBuilder(cacheDir);
    if (!cacheDir.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(CACHE_ROOT_NAME);
    buffer.append(File.separator);
    return buffer.toString();
  }

  /**
   * mapAuToFileLocation() is the method used to resolve {@link ArchivalUnit}s
   * into directory names. This maps a given au to directories, using the
   * cache root as the base.  Given an au with PluginId of 'plugin' and AuId
   * of 'au', it would return the string '<rootLocation>/plugin/au/'.
   * @param repoRoot the root of a LOCKSS repository
   * @param au the ArchivalUnit to resolve
   * @return the directory location
   */
  public static String mapAuToFileLocation(String repoRoot, ArchivalUnit au) {
    return getAuDir(au, repoRoot, true);
  }

  /**
   * mapUrlToFileLocation() is the method used to resolve urls into file names.
   * This maps a given url to a file location, using the au top directory as
   * the base.  It creates directories which mirror the html string, so
   * 'http://www.journal.org/issue1/index.html' would be cached in the file:
   * <rootLocation>/www.journal.org/http/issue1/index.html
   * @param rootLocation the top directory for ArchivalUnit this URL is in
   * @param urlStr the url to translate
   * @return the url file location
   * @throws java.net.MalformedURLException
   */
  public static String mapUrlToFileLocation(String rootLocation, String urlStr)
      throws MalformedURLException {
    int totalLength = rootLocation.length() + urlStr.length();
    URL url = new URL(urlStr);
    StringBuilder buffer = new StringBuilder(totalLength);
    buffer.append(rootLocation);
    if (!rootLocation.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(url.getHost().toLowerCase());
    int port = url.getPort();
    if (port != -1) {
      buffer.append(PORT_SEPARATOR);
      buffer.append(port);
    }
    buffer.append(File.separator);
    buffer.append(url.getProtocol());
    if (RepositoryManager.isEnableLongComponents()) {
      String escapedPath = escapePath(StringUtil.replaceString(url.getPath(), UrlUtil.URL_PATH_SEPARATOR, File.separator));
      String query = url.getQuery();
      if (query!=null) {
	escapedPath = escapedPath + "?" + escapeQuery(query);
      }
      String encodedPath = RepositoryNodeImpl.encodeUrl(escapedPath);
      // encodeUrl strips leading / from path
      buffer.append(File.separator);
      buffer.append(encodedPath);
    } else {
      buffer.append(escapePath(StringUtil.replaceString(url.getPath(),
							UrlUtil.URL_PATH_SEPARATOR,
							File.separator)));
      String query = url.getQuery();
      if (query!=null) {
	buffer.append("?");
	buffer.append(escapeQuery(query));
      }
    }
    return buffer.toString();
  }

  // name mapping functions

  /**
   * Return true iff a repository for the auid exists under the root
   * @param auid
   * @param repoRoot the repository root
   * @return true iff a repository for the auid exists
   */
  static boolean doesAuDirExist(String auid, String repoRoot) {
    return null != getAuDir(auid, repoRoot, false);
  }

  /**
   * Finds the directory for this AU.  If none found in the map, designates
   * a new dir for it.
   * @param au the AU
   * @param repoRoot root of the repository
   * @return the dir {@link String}
   */
  static String getAuDir(ArchivalUnit au, String repoRoot, boolean create) {
    return getAuDir(au.getAuId(), repoRoot, create);
  }

  static void removeAuDirEntry(PluginManager pluginMgr,
                               String auid, String repoRoot) {
    LocalRepository localRepo = getLocalRepository(repoRoot);
    Map aumap = localRepo.getAuMap();
    if (aumap.remove(auid) == null) {
      logger.warning("AUID disappeared from map: "  + auid);
    }
  }

  /**
   * Finds the directory for this AU.  If none found in the map, designates
   * a new dir for it.
   * @param auid AU id representing the au
   * @param repoRoot path to the root of the repository
   * @return the dir String
   */
  public static String getAuDir(String auid, String repoRoot, boolean create) {
    String repoCachePath = extendCacheLocation(repoRoot);
    LocalRepository localRepo = getLocalRepository(repoRoot);
    synchronized (localRepo) {
      Map aumap = localRepo.getAuMap();
      String auPathSlash = (String)aumap.get(auid);
      if (auPathSlash != null) {
	return auPathSlash;
      }
      if (!create) {
	return null;
      }
      logger.debug3("Creating new au directory for '" + auid + "'.");
      String auDir = localRepo.getPrevAuDir();
      for (int cnt = RepositoryManager.getMaxUnusedDirSearch();
	   cnt > 0; cnt--) {
	// loop through looking for an available dir
	auDir = getNextDirName(auDir);
	File testDir = new File(repoCachePath, auDir);
	if (logger.isDebug3()) logger.debug3("Probe for unused: " + testDir);
	if (!testDir.exists()) {
	  if (RepositoryManager.isStatefulUnusedDirSearch()) {
	    localRepo.setPrevAuDir(auDir);
	  }
	  String auPath = testDir.toString();
	  logger.debug3("New au directory: "+auPath);
	  auPathSlash = auPath + File.separator;
	  // write the new au property file to the new dir
	  // XXX this data should be backed up elsewhere to avoid single-point
	  // corruption
	  Properties idProps = new Properties();
	  idProps.setProperty(AU_ID_PROP, auid);
	  saveAuIdProperties(auPath, idProps);
	  aumap.put(auid, auPathSlash);
	  return auPathSlash;
	} else {
	  if (logger.isDebug3()) {
	    logger.debug3("Existing directory found at '"+auDir+
			  "'.  Checking next...");
	  }
	}
      }
    }
    throw new RuntimeException("Can't find unused repository dir after " +
			       RepositoryManager.getMaxUnusedDirSearch() +
			       " tries in " + repoCachePath);
  }

  static LocalRepository getLocalRepository(ArchivalUnit au) {
    return getLocalRepository(getRepositoryRoot(au));
  }

  static LocalRepository getLocalRepository(String repoRoot) {
    synchronized (localRepositories) {
      LocalRepository localRepo = localRepositories.get(repoRoot);
      if (localRepo == null) {
	logger.debug2("Creating LocalRepository(" + repoRoot + ")");
	localRepo = new LocalRepository(repoRoot);
	localRepositories.put(repoRoot, localRepo);
      }
      return localRepo;
    }
  }


  /** Return next string in the sequence "a", "b", ... "z", "aa", "ab", ... */
  static String getNextDirName(String old) {
    StringBuilder sb = new StringBuilder(old);
    // go through and increment the first non-'z' char
    // counts back from the last char, so 'aa'->'ab', not 'ba'
    for (int ii=sb.length()-1; ii>=0; ii--) {
      char curChar = sb.charAt(ii);
      if (curChar < 'z') {
	sb.setCharAt(ii, (char)(curChar+1));
	return sb.toString();
      }
      sb.setCharAt(ii, 'a');
    }
    sb.insert(0, 'a');
    return sb.toString();
  }

  public static File getAuIdFile(String location) {
    return new File(location + File.separator + AU_ID_FILE);
  }

  static Properties getAuIdProperties(String location) {
    File propFile = new File(location + File.separator + AU_ID_FILE);
    try {
      InputStream is = new BufferedInputStream(new FileInputStream(propFile));
      Properties idProps = new Properties();
      idProps.load(is);
      is.close();
      return idProps;
    } catch (Exception e) {
      logger.warning("Error loading au id from " + propFile.getPath() + ".");
      return null;
    }
  }

  static void saveAuIdProperties(String location, Properties props) {
    //XXX these AU_ID_FILE entries need to be backed up elsewhere to avoid
    // single-point corruption
    File propDir = new File(location);
    if (!propDir.exists()) {
      logger.debug("Creating directory '"+propDir.getAbsolutePath()+"'");
      propDir.mkdirs();
    }
    File propFile = new File(propDir, AU_ID_FILE);
    try {
      logger.debug3("Saving au id properties at '" + location + "'.");
      OutputStream os = new BufferedOutputStream(new FileOutputStream(propFile));
      props.store(os, "ArchivalUnit id info");
      os.close();
      propFile.setReadOnly();
    } catch (IOException ioe) {
      logger.error("Couldn't write properties for " + propFile.getPath() + ".",
                   ioe);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't write au id properties file.");
    }
  }

  // lockss filename-specific encoding methods

  /**
   * Escapes instances of the ESCAPE_CHAR from the path.  This avoids name
   * conflicts with the repository files, such as '#nodestate.xml'.
   * @param path the path
   * @return the escaped path
   */
  static String escapePath(String path) {
    //XXX escaping disabled because of URL encoding
    if (false && path.indexOf(ESCAPE_CHAR) >= 0) {
      return StringUtil.replaceString(path, ESCAPE_STR, ESCAPE_STR+ESCAPE_STR);
    } else {
      return path;
    }
  }

  /**
   * Escapes instances of File.separator from the query.  These are safe from
   * filename overlap, but can't convert into extended paths and directories.
   * @param query the query
   * @return the escaped query
   */
  static String escapeQuery(String query) {
    if (query.indexOf(File.separator) >= 0) {
      return StringUtil.replaceString(query, File.separator, ESCAPE_STR +
                                      ENCODED_SEPARATOR_CHAR);
    } else {
      return query;
    }
  }
  
  /**
   * Extracts '#x' encoding and converts back to 'x'.
   * @param orig the original
   * @return the unescaped version.
   */
  static String unescape(String orig) {
    if (orig.indexOf(ESCAPE_CHAR) < 0) {
      // fast treatment of non-escaped strings
      return orig;
    }
    int index = -1;
    StringBuilder buffer = new StringBuilder(orig.length());
    String oldStr = orig;
    while ((index = oldStr.indexOf(ESCAPE_CHAR)) >= 0) {
      buffer.append(oldStr.substring(0, index));
      buffer.append(convertCode(oldStr.substring(index, index+2)));
      if (oldStr.length() > 2) {
        oldStr = oldStr.substring(index + 2);
      } else {
        oldStr = "";
      }
    }
    buffer.append(oldStr);
    return buffer.toString();
  }

  /**
   * Returns the second char in the escaped segment, unless it is 's', which
   * is a stand-in for the File.separatorChar.
   * @param code the code segment (length 2)
   * @return the encoded char
   */
  static char convertCode(String code) {
    char encodedChar = code.charAt(1);
    if (encodedChar == ENCODED_SEPARATOR_CHAR) {
      return File.separatorChar;
    } else {
      return encodedChar;
    }
  }

  public static class Factory implements LockssAuManager.Factory {
    public LockssAuManager createAuManager(ArchivalUnit au) {
      return createNewLockssRepository(au);
    }
  }

  /** Maintains state for a local repository root dir (<i>eg</i>, auid of
   * each au subdir).  */
  static class LocalRepository {
    String repoPath;
    File repoCacheFile;
    Map<String,String> auMap;
    String prevAuDir;

    LocalRepository(String repoPath) {
      this.repoPath = repoPath;
      repoCacheFile = new File(repoPath, CACHE_ROOT_NAME);
    }

    public String getRepositoryPath() {
      return repoPath;
    }

    public String getPrevAuDir() {
      if (prevAuDir == null) {
	prevAuDir = lastPluginDir;
      }
      return prevAuDir;
    }

    public void setPrevAuDir(String dir) {
      prevAuDir = dir;
    }

    /** Return the auid -> au-subdir-path mapping.  Enumerating the
     * directories if necessary to initialize the map */
    Map<String,String> getAuMap() {
      if (auMap == null) {
	auMap = buildAuMap();
      }
      return auMap;
    }

    /** Return the auid -> au-subdir-path mapping.  Enumerating the
     * directories if necessary to initialize the map */
    Map<String,String> buildAuMap() {
      logger.debug3("Loading name map for '" + repoCacheFile + "'.");
      Map<String,String> map = new HashMap<String,String>();
      if (!repoCacheFile.exists()) {
	logger.debug3("Creating cache dir:" + repoCacheFile + "'.");
	if (!repoCacheFile.mkdirs()) {
	  logger.critical("Couldn't create directory, check owner/permissions: "
			  + repoCacheFile);
	  // return empty map
	  return map;
	}
      } else {
	// read each dir's property file and store mapping auid -> dir
	File[] auDirs = repoCacheFile.listFiles();
	for (int ii = 0; ii < auDirs.length; ii++) {
	  String dirName = auDirs[ii].getName();
	  //       if (dirName.compareTo(lastPluginDir) == 1) {
	  //         // adjust the 'lastPluginDir' upwards if necessary
	  //         lastPluginDir = dirName;
	  //       }

	  String path = auDirs[ii].getAbsolutePath();
	  Properties idProps = getAuIdProperties(path);
	  if (idProps != null) {
	    // XXX intern auid?
	    String auid = idProps.getProperty(AU_ID_PROP);
	    StringBuilder sb = new StringBuilder(path.length() +
						 File.separator.length());
	    sb.append(path);
	    sb.append(File.separator);
	    map.put(auid, sb.toString());
	    logger.debug3("Mapping to: " + map.get(auid) + ": " + auid);
	  } else {
	    logger.debug3("Not mapping " + path + ", no auid file.");
	  }
	}

      }
      return map;
    }

    public String toString() {
      return "[LR: " + repoPath + "]";
    }
  }
  
  String getRootLocation() {
    return rootLocation;
  }
}
