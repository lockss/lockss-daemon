/*
 * $Id: LockssRepositoryImpl.java,v 1.51 2004-04-13 22:22:25 eaalto Exp $
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

package org.lockss.repository;

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.apache.commons.collections.LRUMap;
import org.apache.commons.collections.ReferenceMap;

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
  extends BaseLockssManager implements LockssRepository {

  /**
   * Configuration parameter name for Lockss cache location.
   */
  public static final String PARAM_CACHE_LOCATION =
    Configuration.PREFIX + "cache.location";
  /**
   * Name of top directory in which the urls are cached.
   */
  public static final String CACHE_ROOT_NAME = "cache";

  // needed only for unit tests
  static String cacheLocation = null;

  // used for name mapping
  static HashMap nameMap = null;

  // starts with a '#' so no possibility of clashing with a URL
  static final String AU_ID_FILE = "#au_id_file";
  static final String AU_ID_PROP = "au.id";
  static final String PLUGIN_ID_PROP = "plugin.id";
  static final char ESCAPE_CHAR = '#';
  static final String ESCAPE_STR = "#";
  static final char ENCODED_SEPARATOR_CHAR = 's';
  static final String INITIAL_PLUGIN_DIR = String.valueOf((char)('a'-1));
  static String lastPluginDir = INITIAL_PLUGIN_DIR;

  /**
   * Parameter for maximum number of node instances to cache.  This can be
   * fairly small because there typically isn't need for repeated access to
   * the same nodes over a short time.
   */
  public static final String PARAM_MAX_LRUMAP_SIZE = Configuration.PREFIX +
      "cache.max.lrumap.size";
  private static final int DEFAULT_MAX_LRUMAP_SIZE = 100;

  // this contains a '#' so that it's not defeatable by strings which
  // match the prefix in a url (like '../tmp/')
  private static final String TEST_PREFIX = "/#tmp";

  private String rootLocation;
  private LRUMap nodeCache;
  private int nodeCacheSize = DEFAULT_MAX_LRUMAP_SIZE;
  private ReferenceMap refMap;
  private int cacheHits = 0;
  private int cacheMisses = 0;
  private int refHits = 0;
  private int refMisses = 0;
  private static Logger logger = Logger.getLogger("LockssRepository");

  LockssRepositoryImpl(String rootPath) {
    rootLocation = rootPath;
    if (!rootLocation.endsWith(File.separator)) {
      // this shouldn't happen
      rootLocation += File.separator;
    }
    nodeCache = new LRUMap(nodeCacheSize);
    refMap = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
  }

  public void stopService() {
    // mainly important in testing to blank this
    lastPluginDir = INITIAL_PLUGIN_DIR;
    nameMap = null;
    super.stopService();
  }

  protected void setConfig(Configuration newConfig, Configuration prevConfig,
                           Set changedKeys) {
    // at some point we'll have to respond to changes in the available disk
    // space list

    nodeCacheSize = newConfig.getInt(PARAM_MAX_LRUMAP_SIZE,
				     DEFAULT_MAX_LRUMAP_SIZE);
    if (nodeCache.getMaximumSize() != nodeCacheSize) {
      nodeCache.setMaximumSize(nodeCacheSize);
    }
  }

  /** Called between initService() and startService(), then whenever the
   * AU's config changes.
   * @param auConfig the new configuration
   */
  public void setAuConfig(Configuration auConfig) {
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
  private synchronized RepositoryNode getNode(String url, boolean create)
      throws MalformedURLException {
    String urlKey;
    boolean isAuUrl = false;
    if (AuUrl.isAuUrl(url)) {
      // path information is lost here, but is unimportant if it's an AuUrl
      urlKey = AuUrl.PROTOCOL;
      isAuUrl = true;
    } else {
      // create a canonical path, handling all illegal path traversal
      urlKey = canonicalizePath(url);
    }

    // check LRUMap cache for node
    RepositoryNode node = (RepositoryNode)nodeCache.get(urlKey);
    if (node!=null) {
      cacheHits++;
      return node;
    } else {
      cacheMisses++;
    }

    // check weak reference map for node
    node = (RepositoryNode)refMap.get(urlKey);
    if (node!=null) {
      refHits++;
      nodeCache.put(urlKey, node);
      return node;
    } else {
      refMisses++;
    }

    String nodeLocation;
    if (isAuUrl) {
      // base directory of ArchivalUnit
      nodeLocation = rootLocation;
      node = new AuNodeImpl(urlKey, nodeLocation, this);
    } else {
      // determine proper node location
      nodeLocation = LockssRepositoryImpl.mapUrlToFileLocation(rootLocation,
          urlKey);
      node = new RepositoryNodeImpl(urlKey, nodeLocation, this);
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

    // add to node cache and weak reference cache
    nodeCache.put(urlKey, node);
    refMap.put(urlKey, node);
    return node;
  }

  // functions for testing
  int getCacheHits() { return cacheHits; }
  int getCacheMisses() { return cacheMisses; }
  int getRefHits() { return refHits; }
  int getRefMisses() { return refMisses; }

  /**
   * This is called when a node is in an inconsistent state.  It simply creates
   * some necessary directories and deactivates the node.  Future polls should
   * restore it properly.
   * @param node the inconsistent node
   */
  void deactivateInconsistentNode(RepositoryNodeImpl node) {
    logger.warning("Inconsistent node state found; node content deactivated.");
    if (!node.cacheLocationFile.exists()) {
      node.cacheLocationFile.mkdirs();
    }
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
  public static String canonicalizePath(String url)
      throws MalformedURLException {
    String canonUrl;
    try {
      URL testUrl = new URL(url);
      String path = testUrl.getPath();
      // look for '.' or '..' elements
      if (path.indexOf("/.")>=0) {
        // check if path traversal is legal
        if (FileUtil.isLegalPath(path)) {
          // canonicalize to remove urls including '..' and '.'
          path = TEST_PREFIX + path;
          File testFile = new File(path);
          String canonPath = testFile.getCanonicalPath();
          String sysDepPrefix = FileUtil.sysDepPath(TEST_PREFIX);
          int pathIndex = canonPath.indexOf(sysDepPrefix) +
              sysDepPrefix.length();
          // reconstruct the url
          canonUrl = testUrl.getProtocol() + "://" +
              testUrl.getHost().toLowerCase() +
              FileUtil.sysIndepPath(canonPath.substring(pathIndex));
          // restore the query, if any
          String query = testUrl.getQuery();
          if (query!=null) {
            canonUrl += "?" + query;
          }
        } else {
          logger.error("Illegal URL detected: "+url);
          throw new MalformedURLException("Illegal URL detected.");
        }
      } else {
        // clean path, no testing needed
        canonUrl = url;
      }
    } catch (IOException ie) {
      logger.error("Error testing URL: "+ie);
      throw new MalformedURLException ("Error testing URL.");
    }

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
    // XXX needs to handle multiple disks/repository locations

    cacheLocation = Configuration.getParam(PARAM_CACHE_LOCATION);
    if (cacheLocation == null) {
      logger.error("Couldn't get " + PARAM_CACHE_LOCATION +
		   " from Configuration");
      throw new LockssRepository.RepositoryStateException(
          "Couldn't load param.");
    }
    cacheLocation = extendCacheLocation(cacheLocation);

    return new LockssRepositoryImpl(
        LockssRepositoryImpl.mapAuToFileLocation(cacheLocation, au));
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
   * @param rootLocation the root for all ArchivalUnits
   * @param au the ArchivalUnit to resolve
   * @return the directory location
   */
  public static String mapAuToFileLocation(String rootLocation, ArchivalUnit au) {
    StringBuffer buffer = new StringBuffer(rootLocation);
    if (!rootLocation.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    getAuDir(au, buffer);
    buffer.append(File.separator);
    return buffer.toString();
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
   * Finds the directory for this AU.  If none found in the map, designates
   * a new dir for it.
   * @param au the AU
   * @param buffer a StringBuffer to add the dir name to.
   */
  static void getAuDir(ArchivalUnit au, StringBuffer buffer) {
    if (nameMap == null) {
      loadNameMap(buffer.toString());
    }
    String auKey = au.getAuId();
    String auDir = (String)nameMap.get(auKey);
    if (auDir == null) {
      logger.debug3("Creating new au directory for '" + auKey + "'.");
      while (true) {
        // loop through looking for an available dir
        auDir = getNewPluginDir();
        File testDir = new File(buffer.toString() + auDir);
        if (!testDir.exists()) {
          break;
        } else {
          logger.debug3("Existing directory found at '"+auDir+
                        "'.  Creating another...");
        }
      }
      logger.debug3("New au directory: "+auDir);
      nameMap.put(auKey, auDir);
      String auLocation = buffer.toString() + auDir;
      // write the new au property file to the new dir
      // XXX this data should be backed up elsewhere to avoid single-point
      // corruption
      Properties idProps = new Properties();
      idProps.setProperty(AU_ID_PROP, au.getAuId());
      saveAuIdProperties(auLocation, idProps);
    }
    buffer.append(auDir);
  }

  /**
   * Loads the name map by recursing through the current dirs and reading
   * the AU prop file at each location.
   * @param rootLocation the repository HD root location
   */
  static void loadNameMap(String rootLocation) {
    logger.debug3("Loading name map for '" + rootLocation + "'.");
    nameMap = new HashMap();
    File rootFile = new File(rootLocation);
    if (!rootFile.exists()) {
      rootFile.mkdirs();
      logger.debug3("Creating root directory at '" + rootLocation + "'.");
      return;
    }
    File[] pluginAus = rootFile.listFiles();
    for (int ii = 0; ii < pluginAus.length; ii++) {
      // loop through reading each property and storing the id with that dir
      String dirName = pluginAus[ii].getName();
      if (dirName.compareTo(lastPluginDir) == 1) {
        // adjust the 'lastPluginDir' upwards if necessary
        lastPluginDir = dirName;
      }

      Properties idProps = getAuIdProperties(pluginAus[ii].getAbsolutePath());
      if (idProps==null) {
        // if no properties were found, just continue
        continue;
      }
      // store the id, dirName pair in our map
      nameMap.put(idProps.getProperty(AU_ID_PROP), dirName);
    }
  }

  /**
   * Returns the next dir name, from 'a'->'z', then 'aa'->'az', then 'ba'->etc.
   * @return String the next dir
   */
  static String getNewPluginDir() {
    String newPluginDir = "";
    boolean charChanged = false;
    // go through and increment the first non-'z' char
    // counts back from the last char, so 'aa'->'ab', not 'ba'
    for (int ii=lastPluginDir.length()-1; ii>=0; ii--) {
      char curChar = lastPluginDir.charAt(ii);
      if (!charChanged) {
        if (curChar < 'z') {
          curChar++;
          charChanged = true;
          newPluginDir = curChar + newPluginDir;
        } else {
          newPluginDir += 'a';
        }
      } else {
        newPluginDir = curChar + newPluginDir;
      }
    }
    if (!charChanged) {
      newPluginDir += 'a';
    }
    lastPluginDir = newPluginDir;
    return newPluginDir;
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
    //XXX escaping disabled
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

}
