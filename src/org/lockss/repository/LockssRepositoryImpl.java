/*
 * $Id: LockssRepositoryImpl.java,v 1.80.12.3 2009-08-12 18:46:40 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.protocol.IdentityAgreementList;
import org.lockss.util.*;
import org.lockss.repository.v2.*;
import org.lockss.state.*;

/**
 * LockssRootRepository is used to organize the urls being cached.
 * It keeps a memory cache of the most recently used nodes as a
 * least-recently-used map, and also caches weak references to the instances
 * as they're doled out.  This ensures that two instances of the same node are
 * never created, as the weak references only disappear when the object is
 * finalized (they go to null when the last hard reference is gone, then are
 * removed from the cache on finalize()).
 */
public class LockssRepositoryImpl
  extends BaseLockssDaemonManager implements LockssRepository, LockssOneAuRepository {
  private static Logger logger = Logger.getLogger("LockssRepositoryImpl");

  /**
   * Configuration parameter name for Lockss cache location.
   */
  public static final String PARAM_CACHE_LOCATION =
    Configuration.PREFIX + "cache.location";
  /**
   * Name of top directory in which the urls are cached.
   */
  public static final String CACHE_ROOT_NAME = "cache";

  // XXX This is a remnant from the single-disk days, and should go away.
  // It is used only by unit tests, which want it set to the (last)
  // individual repository dir created.
  private static String staticCacheLocation = null;

  // Maps local repository name (disk path) to LocalRepository instance
  static Map<String, LockssRepositoryManager> localRepositories = 
    new HashMap<String, LockssRepositoryManager>();

  // starts with a '#' so no possibility of clashing with a URL
  /**
   * <p>The AU state file name.</p>
   */
  public static final String AU_FILE_NAME = "#au_state.xml";
  public static final String AU_ID_FILE = "#au_id_file";
  static final String AU_ID_PROP = "au.id";
  static final String PLUGIN_ID_PROP = "plugin.id";
  static final char ESCAPE_CHAR = '#';
  static final String ESCAPE_STR = "#";
  static final char ENCODED_SEPARATOR_CHAR = 's';
  static final String INITIAL_PLUGIN_DIR = String.valueOf((char)('a'-1));
  static String lastPluginDir = INITIAL_PLUGIN_DIR;

  // this contains a '#' so that it's not defeatable by strings which
  // match the prefix in a url (like '../tmp/')
  private static final String TEST_PREFIX = "/#tmp";

  private RepositoryManager repoMgr;
  private String rootLocation;
  UniqueRefLruCache nodeCache;
  private boolean isGlobalNodeCache =
    RepositoryManager.DEFAULT_GLOBAL_CACHE_ENABLED;
  protected ArchivalUnit au;

  LockssRepositoryImpl(String rootPath, ArchivalUnit au) {
    if (rootPath.endsWith(File.separator)) {
      rootLocation = rootPath;
    } else {
      // shouldn't happen
      StringBuffer sb = new StringBuffer(rootPath.length() +
					 File.separator.length());
      sb.append(rootPath);
      sb.append(File.separator);
      rootLocation = sb.toString();
    }
    // Test code still needs this.
    nodeCache =
      new UniqueRefLruCache(RepositoryManager.DEFAULT_MAX_PER_AU_CACHE_SIZE);
    
    this.au = au;
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
    localRepositories = new HashMap();
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

  public void queueSizeCalc(RepositoryNode node) {
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
  public synchronized RepositoryNode getNode(String url, boolean create)
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

    String nodeLocation;
    if (isAuUrl) {
      // base directory of ArchivalUnit
      nodeLocation = rootLocation;
      node = new AuNodeImpl(canonUrl, nodeLocation, this, au);
    } else {
      // determine proper node location
      nodeLocation = LockssRepositoryImpl.mapUrlToFileLocation(rootLocation,
          canonUrl);
      node = new RepositoryNodeImpl(canonUrl, nodeLocation, this, au);
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

    // add to node cache
    nodeCache.put(nodeCacheKey(canonUrl), node);
    return node;
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
   * Factory method to create new LockssRootRepository instances.
   * @param au the {@link ArchivalUnit}
   * @return the new LockssRootRepository instance
   */
  public static LockssRepository createNewLockssRepository(ArchivalUnit au) {
    String root = getRepositoryRoot(au);
    if (root == null) {
      logger.error("Couldn't get " + PARAM_CACHE_LOCATION +
		   " from Configuration");
      throw new LockssRepository.RepositoryStateException(
          "Couldn't load param.");
    }
    String auDir = LockssRepositoryImpl.mapAuToFileLocation(root, au);
    logger.debug("repo: " + auDir + ", au: " + au.getName());
    staticCacheLocation = extendCacheLocation(root);
    LockssRepositoryImpl repo = new LockssRepositoryImpl(auDir, au);
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
    StringBuffer buffer = new StringBuffer(cacheDir);
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
    StringBuffer buffer = new StringBuffer(totalLength);
    buffer.append(rootLocation);
    if (!rootLocation.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(url.getHost().toLowerCase());
    int port = url.getPort();
    if (port != -1) {
      buffer.append(":");
      buffer.append(port);
    }
    buffer.append(File.separator);
    buffer.append(url.getProtocol());
    buffer.append(escapePath(StringUtil.replaceString(url.getPath(),
        UrlUtil.URL_PATH_SEPARATOR, File.separator)));
    String query = url.getQuery();
    if (query!=null) {
      buffer.append("?");
      buffer.append(escapeQuery(query));
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

  /**
   * Finds the directory for this AU.  If none found in the map, designates
   * a new dir for it.
   * @param auid AU id representing the au
   * @param repoRoot path to the root of the repository
   * @return the dir String
   */
  static String getAuDir(String auid, String repoRoot, boolean create) {
    String repoCachePath = extendCacheLocation(repoRoot);
    LockssRepositoryManager localRepo = getLocalRepository(repoRoot);
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
      String auDir = lastPluginDir;
      for (int cnt = 10000; cnt > 0; cnt--) {
	// loop through looking for an available dir
	auDir = getNextDirName(auDir);
	File testDir = new File(repoCachePath, auDir);
	if (!testDir.exists()) {
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
			       "10000 tries in " + repoCachePath);
  }

  static LockssRepositoryManager getLocalRepository(ArchivalUnit au) {
    return getLocalRepository(getRepositoryRoot(au));
  }

  static LockssRepositoryManager getLocalRepository(String repoRoot) {
    synchronized (localRepositories) {
      LockssRepositoryManager localRepo =
	localRepositories.get(repoRoot);
      if (localRepo == null) {
	logger.debug2("Creating LocalRepository(" + repoRoot + ")");
	// TODO: Determine which type the local repository is.
	localRepo = new UnixRepositoryManager(repoRoot);
	localRepositories.put(repoRoot, localRepo);
      }
      return localRepo;
    }
  }


  /** Return next string in the sequence "a", "b", ... "z", "aa", "ab", ... */
  static String getNextDirName(String old) {
    StringBuffer sb = new StringBuffer(old);
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
  
  // NOTE: LockssOneAuRepositoryImpl does NOT pass in a location.
  
  public InputStream getAuStateRawContents() 
      throws IOException {
    File fileAuState;
    FileInputStream fisAuState;
    
    fileAuState = getAuStateFile();
    
    fisAuState = new FileInputStream(fileAuState);
    
    return fisAuState;
  }
  
  
  public void setAuStateRawContents(InputStream istrAuState)
      throws IOException {
    File fileAuState;
    File fileTemp;
    OutputStream osTemp;
    
    // We're given an input stream; we don't know whether it's from a 
    // file or from something more exotic.  Therefore, I need to 
    // copy the stream to a file.
    fileTemp = FileUtil.createTempFile("AuStateRaw", null);
    osTemp = new BufferedOutputStream(new FileOutputStream(fileTemp));
    StreamUtil.copy(istrAuState, osTemp);
    
    // Atomically move the file to the correct location.
    fileAuState = getAuStateFile();
    PlatformUtil.updateAtomically(fileTemp, fileAuState);
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
    StringBuffer buffer = new StringBuffer(orig.length());
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

  // The following methods implement LockssRootRepository.
  
  public void checkConsistency() {
    // This method is a stub, and it has been intentionally left blank.
  }

  public long getAuCreationTime() {
    AuState austate;
    
    austate = loadAuState();
    return austate.getAuCreationTime();
  }

  public File getAuStateFile() {
    return new File(rootLocation + File.separator + AU_FILE_NAME);
  }
  
  File getDamagedNodeFile() {
    return new File(rootLocation, HistoryRepositoryImpl.DAMAGED_NODES_FILE_NAME);
  }

  public File getIdentityAgreementFile() {
    return new File(rootLocation, HistoryRepositoryImpl.IDENTITY_AGREEMENT_FILE_NAME);
  }

  // Most of this method comes indirectly from HistoryRepositoryImpl.
  public AuState loadAuState() {
    AuState ausReturn;
    CXSerializer obser;
    File fileAuState;
    
    // Mostly taken from HistoryRepositoryImpl.makeObjectSerializer
    obser = new CXSerializer(getDaemon(),
        CastorSerializer.getMapping(HistoryRepositoryImpl.MAPPING_FILES),
        AuStateBean.class);
    obser.setFailedDeserializationMode(ObjectSerializer.FAILED_DESERIALIZATION_RENAME);
    obser.setCompatibilityMode(CXSerializer.getCompatibilityModeFromConfiguration());

    // Mostly taken from loadAuState
    try {
      fileAuState = getAuStateFile();
      ausReturn = (AuState) obser.deserialize(fileAuState);
    } catch (InterruptedIOException e) {
      logger.debug2("Interrupted IO Exception.  Continuing: ", e);
      ausReturn = new AuState(au, getDaemon().getHistoryRepository(au));
    } catch (SerializationException e) {
      logger.debug2("Serialization Exception.  Continuing: ", e);
      if (getDaemon() == null) {
        logger.error("In LockssRepositoryImpl.loadAuState, Daemon is null; cancelling.");
        throw new LockssRepository.RepositoryStateException("Daemon is null.");
      }
      
      if (getDaemon().getHistoryRepository(au) == null) {
        logger.error("In LockssRepositoryImpl.loadAuState, history repository is null.  Cancelling.");
        throw new LockssRepository.RepositoryStateException("History repository is null.");
      }
      ausReturn = new AuState(au, getDaemon().getHistoryRepository(au));
    }
    
    return ausReturn;
  }

  // Mostly taken from HistoryRepositoryImpl.loadDamagedNodeSet.
  
  public DamagedNodeSet loadDamagedNodeSet() {
    CXSerializer obser;
    DamagedNodeSet dnsReturn;
    File fileDamagedNodeSet;

    // Mostly taken from HistoryRepositoryImpl.makeObjectSerializer
    obser = new CXSerializer(getDaemon(),
        CastorSerializer.getMapping(HistoryRepositoryImpl.MAPPING_FILES),
        DamagedNodeSet.class);
    obser.setFailedDeserializationMode(ObjectSerializer.FAILED_DESERIALIZATION_RENAME);
    obser.setCompatibilityMode(CXSerializer.getCompatibilityModeFromConfiguration());

    // Mostly taken from loadDamagedNodeSet
    try {
      fileDamagedNodeSet = getDamagedNodeFile();
      dnsReturn = (DamagedNodeSet) obser.deserialize(fileDamagedNodeSet);
    } catch (InterruptedIOException e) {
      logger.debug2("Interrupted IO Exception.  Continuing: ", e);
      dnsReturn = new DamagedNodeSet(au, getDaemon().getHistoryRepository(au));
    } catch (SerializationException e) {
      logger.debug2("Serialization Exception.  Continuing: ", e);
      dnsReturn = new DamagedNodeSet(au, getDaemon().getHistoryRepository(au));
    }
    
    return dnsReturn;
  }

  
  public List loadIdentityAgreements() {
    CXSerializer obser;
    List liIdentityAgreements;
    File fileIdentityAgreements;

    // Mostly taken from HistoryRepositoryImpl.makeObjectSerializer
    obser = new CXSerializer(getDaemon(),
        CastorSerializer.getMapping(HistoryRepositoryImpl.MAPPING_FILES),
        IdentityAgreementList.class);
    obser.setFailedDeserializationMode(ObjectSerializer.FAILED_DESERIALIZATION_RENAME);
    obser.setCompatibilityMode(CXSerializer.getCompatibilityModeFromConfiguration());

    // Mostly taken from loadDamagedNodeSet (modified for identity agreements)
    try {
      fileIdentityAgreements = getIdentityAgreementFile();
      liIdentityAgreements = (List) obser.deserialize(fileIdentityAgreements);
    } catch (InterruptedIOException e) {
      logger.debug2("Interrupted IO Exception.  Continuing: ", e);
      liIdentityAgreements = new ArrayList();
    } catch (SerializationException e) {
      logger.debug2("Serialization Exception.  Continuing: ", e);
      liIdentityAgreements = new ArrayList();
    }
    
    return liIdentityAgreements;
  }

  public NodeState loadNodeState(CachedUrlSet cus) {
    // This method is a stub; it has been intentionally left blank.
    return null;
  }

  public void loadPollHistories(NodeState nodeState) {
    // This method is a stub; it has been intentionally left blank.
    
  }

  // Note that this merely sets the stream inside a node.  It does not,
  // for example, copy all child nodes.
  
  public void setNode(String url, RepositoryNode node) throws MalformedURLException,
    LockssRepositoryException {
    RepositoryNode nodeSet;
    
    nodeSet = getNode(url);
    try {
      nodeSet.setInputStream(node.getInputStream());
    } catch (LockssRepositoryException e) {
      logger.error("Lockss Repository Exception: ", e);
      throw e;
    } catch (IOException e) {
      logger.error("IO Exception: ", e);
      throw new LockssRepositoryException(e);
    }
  }

  /** Most of this method comes from HistoryRepositoryImpl.storeAuState.
   * 
   */
  
  public void storeAuState(AuState auState) {
    CXSerializer obser;
    
    logger.debug3("Storing state for AU '" + auState.getArchivalUnit().getName() + "'");
    File file = prepareFile(rootLocation, AU_FILE_NAME);
    
    // Mostly taken from HistoryRepositoryImpl.makeObjectSerializer
    obser = new CXSerializer(getDaemon(),
        CastorSerializer.getMapping(HistoryRepositoryImpl.MAPPING_FILES),
        AuStateBean.class);
    obser.setFailedDeserializationMode(ObjectSerializer.FAILED_DESERIALIZATION_RENAME);
    obser.setCompatibilityMode(CXSerializer.getCompatibilityModeFromConfiguration());

    try {
      // CASTOR: remove wrap() when Castor is phased out
      obser.serialize(file, wrap(auState));
    }
    catch (Exception exc) {
      String errorString = "Could not store AU state for AU '" + auState.getArchivalUnit().getName() + "'";
      logger.error(errorString, exc);
      throw new RepositoryStateException(errorString, exc);
    }    
  }

  
  /** Mostly taken from HistoryRepositoryImpl.storeDamagedNodeSet.
   * 
   */
  
  public void storeDamagedNodeSet(DamagedNodeSet nodeSet) {
    CXSerializer obser;
    
    logger.debug3("Storing damaged nodes for AU '" + nodeSet.getArchivalUnit().getName() + "'");
    File file = getDamagedNodeFile();
    
    // Mostly taken from HistoryRepositoryImpl.makeObjectSerializer
    obser = new CXSerializer(getDaemon(),
        CastorSerializer.getMapping(HistoryRepositoryImpl.MAPPING_FILES),
        DamagedNodeSet.class);
    obser.setFailedDeserializationMode(ObjectSerializer.FAILED_DESERIALIZATION_RENAME);
    obser.setCompatibilityMode(CXSerializer.getCompatibilityModeFromConfiguration());
    
    try {
      // CASTOR: NO CHANGE when Castor is phased out
      obser.serialize(file, nodeSet);
    }
    catch (Exception exc) {
      String errorString = "Could not store damaged nodes for AU '" + nodeSet.getArchivalUnit().getName() + "'";
      logger.error(errorString, exc);
      throw new RepositoryStateException(errorString, exc);
    }
  }

  
  /** Mostly taken from HistoryRepositoryImpl.storeIdentityAgreements.
   * 
   */
  
  public void storeIdentityAgreements(List identAgreements) {
    CXSerializer obser;
    
    logger.debug3("Storing identity agreements for AU '" + au.getName() + "'");
    File file = prepareFile(rootLocation, HistoryRepositoryImpl.IDENTITY_AGREEMENT_FILE_NAME);

    // Mostly taken from HistoryRepositoryImpl.makeObjectSerializer
    obser = new CXSerializer(getDaemon(),
        CastorSerializer.getMapping(HistoryRepositoryImpl.MAPPING_FILES),
        IdentityAgreementList.class);
    obser.setFailedDeserializationMode(ObjectSerializer.FAILED_DESERIALIZATION_RENAME);
    obser.setCompatibilityMode(CXSerializer.getCompatibilityModeFromConfiguration());

    
    try {
      // CASTOR: remove wrap() when Castor is phased out
      obser.serialize(file, wrap(identAgreements));
    }
    catch (Exception exc) {
      String errorString = "Could not store identity agreements for AU '" + au.getName() + "'";
      logger.error(errorString, exc);
      throw new RepositoryStateException(errorString, exc);
    }
  
  }

  public void storeNodeState(NodeState nodeState) {
    // This method is a stub; it has been intentionally left blank.
  }

  public void storePollHistories(NodeState nodeState) {
    // Because loadPollHistories is a stub, I assume that this method
    // should also be a stub.
  }
  
  public void undeleteNode(String url) throws MalformedURLException {
    RepositoryNode node = getNode(url, false);
    if (node!=null) {
      node.markAsNotDeleted();
    }
  }  

  // --- Methods used by the above methods.
  
  /**
   * <p>Computes the node location from a CUS URL. Uses
   * LockssRepositoryImpl static functions.</p>
   * @param cus A CachedUrlSet instance.
   * @return The CUS' file system location.
   * @throws MalformedURLException
   */
  protected String getNodeLocation(CachedUrlSet cus)
      throws MalformedURLException {
    String urlStr = cus.getUrl();
    if (AuUrl.isAuUrl(urlStr)) {
      return rootLocation;
    } 
    
    return mapUrlToFileLocation(rootLocation, canonicalizePath(urlStr));
  }

  
  /**
   * <p>Instantiates a {@link File} instance with the given prefix and
   * suffix, creating the path of directories denoted by the prefix if
   * needed by calling {@link File#mkdirs}.</p>
   * @param parent The path prefix.
   * @param child  The file name.
   * @return A new file instance with the prefix appropriately
   *         created.
   */
  private static File prepareFile(String parent, String child) {
    File parentFile = new File(parent);
    if (!parentFile.exists()) { parentFile.mkdirs(); }
    return new File(parentFile, child);
  }

  /**
   * <p>Might wrap an AuState into an AuStateBean.</p>
   * @param auState An AuState instance.
   * @return An object suitable for serialization.
   */
  private static LockssSerializable wrap(AuState auState) {
    // CASTOR: Phase out with Castor
    if (HistoryRepositoryImpl.isCastorMode()) { return new AuStateBean(auState); }
    else                { return auState; }
  }

  /**
   * <p>Might wrap a List into an IdentityAgreementList.</p>
   * @param idList An identity agreement list.
   * @return An object suitable for serialization.
   */
  private static Serializable wrap(List idList) {
    // CASTOR: Phase out with Castor
    if (HistoryRepositoryImpl.isCastorMode()) { return new IdentityAgreementList(idList); }
    else                { return (Serializable)idList; }
  }

  /* (non-Javadoc)
   * Always returns the latest version of the node.
   * 
   * @see org.lockss.repository.v2.LockssOneAuRepository#getFile(java.lang.String, boolean)
   */
  public RepositoryFile getFile(String url, boolean create)
      throws MalformedURLException, LockssRepositoryException {
    RepositoryNode rnFile;
    
    rnFile = getNode(url, create);
    if (rnFile == null) {
      if (!create) {
        // Everything is normal.
        return null;
      } 
      
      logger.error("getFile: getNode returned null, even though it was asked to create the node.");
      throw new LockssRepositoryException("Did not successfully create the node.");
    }
    
    return rnFile;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.LockssOneAuRepository#getNoAuPeerSetRawContents()
   */
  public InputStream getNoAuPeerSetRawContents()
      throws LockssRepositoryException {
    InputStream istr;
    
    try {
      istr = new FileInputStream(getNoAuFile());
    
      return istr;
    } catch (FileNotFoundException e) {
      logger.error("File not found exception: ", e);
      return null;
    }
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.LockssOneAuRepository#hasNoAuPeerSet()
   */
  public boolean hasNoAuPeerSet() {
    return getNoAuFile().exists();
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.LockssOneAuRepository#queueSizeCalc(org.lockss.repository.v2.RepositoryNode)
   */
  public void queueSizeCalc(org.lockss.repository.v2.RepositoryNode node) {
    if (logger.isDebug3()) {
      logger.debug3("queueSizeCale for these kinds of nodes is not implemented yet.");
    }
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.LockssOneAuRepository#storeNoAuRawContents(java.io.InputStream)
   */
  public void storeNoAuRawContents(InputStream istr)
      throws LockssRepositoryException {
    OutputStream ostrNoAu;
    
    try {
      ostrNoAu = new FileOutputStream(getNoAuFile());
      StreamUtil.copy(istr, ostrNoAu);
    } catch (FileNotFoundException e) {
      logger.error("File not found exception: ", e);
    } catch (IOException e) {
      logger.error("IO Exception: ", e);
    }
    
  }

  private File getNoAuFile() {
    File fileNoAu;
    
    fileNoAu = new File(rootLocation, HistoryRepositoryImpl.NO_AU_PEER_ID_SET_FILE_NAME);
    
    return fileNoAu;
  }
}
