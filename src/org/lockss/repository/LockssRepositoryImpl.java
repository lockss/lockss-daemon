/*
 * $Id: LockssRepositoryImpl.java,v 1.43 2003-12-23 00:26:14 tlipkis Exp $
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
import java.lang.ref.WeakReference;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.apache.commons.collections.LRUMap;
import org.apache.commons.collections.ReferenceMap;
import java.net.MalformedURLException;

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
  static String lastPluginDir = ""+(char)('a'-1);

  /**
   * Maximum number of node instances to cache.
   */
  public static final int MAX_LRUMAP_SIZE = 12;

  // this contains a '#' so that it's not defeatable by strings which
  // match the prefix in a url (like '../tmp/')
  private static final String TEST_PREFIX = "/#tmp";

  private String rootLocation;
  private ArchivalUnit repoAu;
  private LRUMap nodeCache;
  private ReferenceMap refMap;
  private int cacheHits = 0;
  private int cacheMisses = 0;
  private int refHits = 0;
  private int refMisses = 0;
  private static Logger logger = Logger.getLogger("LockssRepository");
  private String baseDir = null;

  LockssRepositoryImpl(String rootPath, ArchivalUnit au) {
    rootLocation = rootPath;
    repoAu = au;
    if (!rootLocation.endsWith(File.separator)) {
      // this shouldn't happen
      rootLocation += File.separator;
    }
    nodeCache = new LRUMap(MAX_LRUMAP_SIZE);
    refMap = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
  }

  public void stopService() {
    // mainly important in testing to blank this
    lastPluginDir = ""+(char)('a'-1);
    nameMap = null;
  }

  protected void setConfig(Configuration config, Configuration oldConfig,
                           Set changedKeys) {
    // at some point we'll have to respond to changes in the available disk
    // space list
  }

  /** Called between initService() and startService(), then whenever the
   * AU's config changes.
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

  public int cusCompare(CachedUrlSet cus1, CachedUrlSet cus2) {
    // check that they're in the same AU
    if (!cus1.getArchivalUnit().equals(cus2.getArchivalUnit())) {
      return LockssRepository.NO_RELATION;
    }
    CachedUrlSetSpec spec1 = cus1.getSpec();
    CachedUrlSetSpec spec2 = cus2.getSpec();
    String url1 = cus1.getUrl();
    String url2 = cus2.getUrl();

    // check for top-level urls
    if (spec1.isAu() || spec2.isAu()) {
      if (spec1.equals(spec2)) {
        return LockssRepository.SAME_LEVEL_OVERLAP;
      } else if (spec1.isAu()) {
        return LockssRepository.ABOVE;
      } else {
        return LockssRepository.BELOW;
      }
    }

    if (!url1.endsWith(UrlUtil.URL_PATH_SEPARATOR)) {
      url1 += UrlUtil.URL_PATH_SEPARATOR;
    }
    if (!url2.endsWith(UrlUtil.URL_PATH_SEPARATOR)) {
      url2 += UrlUtil.URL_PATH_SEPARATOR;
    }
    if (url1.equals(url2)) {
      //the urls are on the same level; check for overlap
      if (spec1.isDisjoint(spec2)) {
        return LockssRepository.SAME_LEVEL_NO_OVERLAP;
      } else {
        return LockssRepository.SAME_LEVEL_OVERLAP;
      }
    } else if (url1.startsWith(url2)) {
      // url1 is a sub-directory of url2
      if (spec2.isSingleNode()) {
        return LockssRepository.SAME_LEVEL_NO_OVERLAP;
      } else if (cus2.containsUrl(url1)) {
        // child
        return LockssRepository.BELOW;
      } else {
        // cus2 probably has a range which excludes url1
        return LockssRepository.NO_RELATION;
      }
    } else if (url2.startsWith(url1)) {
      // url2 is a sub-directory of url1
      if (spec1.isSingleNode()) {
        return LockssRepository.SAME_LEVEL_NO_OVERLAP;
      } else if (cus1.containsUrl(url2)) {
        // parent
        return LockssRepository.ABOVE;
      } else {
        // cus1 probably has a range which excludes url2
        return LockssRepository.NO_RELATION;
      }
    } else {
      // no connection between the two urls
      return LockssRepository.NO_RELATION;
    }
  }

  private synchronized RepositoryNode getNode(String url, boolean create)
      throws MalformedURLException {
    String urlKey;
    boolean isAuUrl = false;
    if (AuUrl.isAuUrl(url)) {
      urlKey = AuUrl.PROTOCOL;
      isAuUrl = true;
    } else {
      try {
        URL testUrl = new URL(url);
        String path = testUrl.getPath();
        if (path.indexOf("/.")>=0) {
          if (FileUtil.isLegalPath(path)) {
            // canonicalize to remove urls including '..' and '.'
            path = TEST_PREFIX + path;
            File testFile = new File(path);
            String canonPath = testFile.getCanonicalPath();
            String sysDepPrefix = FileUtil.sysDepPath(TEST_PREFIX);
            int pathIndex = canonPath.indexOf(sysDepPrefix) +
                sysDepPrefix.length();
            urlKey = testUrl.getProtocol() + "://" +
                testUrl.getHost().toLowerCase() +
                FileUtil.sysIndepPath(canonPath.substring(pathIndex));
          } else {
            logger.error("Illegal URL detected: "+url);
            throw new MalformedURLException("Illegal URL detected.");
          }
          url = urlKey;
        } else {
          // clean path, no testing needed
          urlKey = url;
        }
      } catch (IOException ie) {
        logger.error("Error testing URL: "+ie);
        throw new MalformedURLException ("Error testing URL.");
      }
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
      node = new AuNodeImpl(url, nodeLocation, this);
    } else {
      nodeLocation = LockssRepositoryImpl.mapUrlToFileLocation(rootLocation,
          url);
      node = new RepositoryNodeImpl(url, nodeLocation, this);
    }
    if (!create) {
      // if not creating, check for existence
      File nodeDir = new File(nodeLocation);
      if (!nodeDir.exists()) {
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

  int getCacheHits() { return cacheHits; }
  int getCacheMisses() { return cacheMisses; }
  int getRefHits() { return refHits; }
  int getRefMisses() { return refMisses; }

  void consistencyCheck(RepositoryNodeImpl node) {
    logger.warning("Inconsistent node state found; node content deactivated.");
    if (!node.cacheLocationFile.exists()) {
      node.cacheLocationFile.mkdirs();
    }
    // manually deactivate
    node.deactivateContent();
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
        LockssRepositoryImpl.mapAuToFileLocation(cacheLocation, au),
        au);
  }

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
  public static String mapUrlToFileLocation(String rootLocation, String urlStr) throws
      MalformedURLException {
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
    buffer.append(StringUtil.replaceString(url.getPath(), "/", File.separator));
    return buffer.toString();
  }

  // name mapping functions

  static void getAuDir(ArchivalUnit au, StringBuffer buffer) {
    if (nameMap == null) {
      loadNameMap(buffer.toString());
    }
    String auKey = getAuKey(au);
    String auDir = (String)nameMap.get(auKey);
    if (auDir == null) {
      logger.debug3("Creating new au directory for '" + auKey + "'.");
      while (true) {
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
      Properties idProps = new Properties();
      idProps.setProperty(AU_ID_PROP, au.getAuId());
      saveAuIdProperties(auLocation, idProps);
    }
    buffer.append(auDir);
  }

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
      String dirName = pluginAus[ii].getName();
      if (dirName.compareTo(lastPluginDir) == 1) {
        lastPluginDir = dirName;
      }

      Properties idProps = getAuIdProperties(pluginAus[ii].getAbsolutePath());
      if (idProps==null) {
        // if no properties were found, just continue
        continue;
      }
      nameMap.put(idProps.getProperty(AU_ID_PROP), dirName);
    }
  }

  static String getNewPluginDir() {
    String newPluginDir = "";
    boolean charChanged = false;
    // go through and increment the first non-'z' char
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

  static String getAuKey(ArchivalUnit au) {
    return au.getAuId();
  }

  public static class Factory implements LockssAuManager.Factory {
    public LockssAuManager createAuManager(ArchivalUnit au) {
      return createNewLockssRepository(au);
    }
  }

}
