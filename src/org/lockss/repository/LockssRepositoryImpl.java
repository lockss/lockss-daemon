/*
 * $Id: LockssRepositoryImpl.java,v 1.21 2003-03-01 02:01:24 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * LockssRepository is used to organize the urls being cached.
 * It keeps a memory cache of the most recently used nodes as a
 * least-recently-used map, and also caches weak references to the instances
 * as they're doled out.  This ensures that two instances of the same node are
 * never created, as the weak references only disappear when the object is
 * finalized (they go to null when the last hard reference is gone, then are
 * removed from the cache on finalize()).
 */
public class LockssRepositoryImpl implements LockssRepository {
  /**
   * Configuration parameter name for Lockss cache location.
   */
  public static final String PARAM_CACHE_LOCATION =
    Configuration.PREFIX + "cache.location";
  /**
   * Name of top directory in which the urls are cached.
   */
  public static final String CACHE_ROOT_NAME = "cache";

  /**
   * Maximum number of node instances to cache.
   */
  public static final int MAX_LRUMAP_SIZE = 12;

  private String rootLocation;
  private ArchivalUnit repoAu;
  private LRUMap nodeCache;
  private ReferenceMap refMap;
  private int cacheHits = 0;
  private int cacheMisses = 0;
  private int refHits = 0;
  private int refMisses = 0;
  private static Logger logger = Logger.getLogger("LockssRepository");
  private static LockssDaemon theDaemon = null;
  private static LockssRepository theRepository = null;
  // these are specifically non-static to allow the tests to work
  // in practice, the singleton nature of theDaemon's repository
  // allows these to act as if static
  private String baseDir = null;
  private HashMap repoMap = new HashMap();

  public LockssRepositoryImpl() { }

  private LockssRepositoryImpl(String rootPath, ArchivalUnit au) {
    rootLocation = rootPath;
    repoAu = au;
    if (!rootLocation.endsWith(File.separator)) {
      // this shouldn't happen
      rootLocation += File.separator;
    }
    nodeCache = new LRUMap(MAX_LRUMAP_SIZE);
    refMap = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
  }

  /**
   * init the plugin manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see org.lockss.app.LockssManager#initService(LockssDaemon daemon)
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if (theRepository == null) {
      theDaemon = daemon;
      theRepository = this;
    } else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
        public void configurationChanged(Configuration oldConfig,
                                         Configuration newConfig,
                                         Set changedKeys) {
          setConfig(newConfig, oldConfig);
        }
      });
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    //XXX checkpoint here.
    theRepository = null;
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
    String cacheLoc = config.get(PARAM_CACHE_LOCATION);
    if (cacheLoc!=null) {
      rootLocation = extendCacheLocation(cacheLoc);
    }
  }

  public RepositoryNode getNode(String url)
      throws MalformedURLException {
    return getNode(url, false);
  }

  public RepositoryNode createNewNode(String url)
      throws MalformedURLException {
    return getNode(url, true);
  }

  public void deleteNode(String url) throws MalformedURLException {
    RepositoryNode node = getNode(url, false);
    if (node!=null) {
      node.deactivate();
    }
  }

  public int cusCompare(CachedUrlSet cus1, CachedUrlSet cus2) {
    String url1 = cus1.getUrl();
    String url2 = cus2.getUrl();
    if (!url1.endsWith(File.separator)) {
      url1 += File.separator;
    }
    if (!url2.endsWith(File.separator)) {
      url2 += File.separator;
    }
    if (url1.equals(url2)) {
      //the urls are on the same level; check for overlap
      Iterator firstIt = cus1.flatSetIterator();
      Set secondSet = SetUtil.fromIterator(cus2.flatSetIterator());
      while (firstIt.hasNext()) {
        if (secondSet.contains(firstIt.next())) {
          return LockssRepository.SAME_LEVEL_OVERLAP;
        }
      }
      return LockssRepository.SAME_LEVEL_NO_OVERLAP;
    } else if (url1.startsWith(url2)) {
      // url1 is a sub-directory of url2
      return LockssRepository.BELOW;
    } else if (url2.startsWith(url1)) {
      // url2 is a sub-directory of url1
      return LockssRepository.ABOVE;
    } else {
      // no connection between the two urls
      return LockssRepository.NO_RELATION;
    }
  }

  private synchronized RepositoryNode getNode(String url, boolean create)
      throws MalformedURLException {
    String urlKey = url;
    boolean isAuUrl = false;
    if (AuUrl.isAuUrl(url)) {
      urlKey = AuUrl.PROTOCOL;
      isAuUrl = true;
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
      nodeLocation = RepositoryLocationUtil.mapUrlToFileLocation(rootLocation, url);
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

  /**
   * Removes a weak reference from the reference map.  Called by finalize()
   * of {@link RepositoryNodeImpl}.
   * @param urlKey the reference key url
   */
  synchronized void removeReference(String urlKey) {
    refMap.remove(urlKey);
  }

  int getCacheHits() { return cacheHits; }
  int getCacheMisses() { return cacheMisses; }
  int getRefHits() { return refHits; }
  int getRefMisses() { return refMisses; }

  public synchronized LockssRepository repositoryFactory(ArchivalUnit au) {
    if (baseDir==null) {
      baseDir = Configuration.getParam(PARAM_CACHE_LOCATION);
      if (baseDir==null) {
        logger.error("Couldn't get "+PARAM_CACHE_LOCATION+" from Configuration");
        throw new LockssRepository.RepositoryStateException("Couldn't load param.");
      }
      baseDir = extendCacheLocation(baseDir);
    }
    LockssRepository repo = (LockssRepository)repoMap.get(au);
    if (repo==null) {
      repo = new LockssRepositoryImpl(
          RepositoryLocationUtil.mapAuToFileLocation(baseDir, au), au);
      repoMap.put(au, repo);
    }
    return repo;
  }

  private static String extendCacheLocation(String cacheDir) {
    StringBuffer buffer = new StringBuffer(cacheDir);
    if (!cacheDir.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(CACHE_ROOT_NAME);
    buffer.append(File.separator);
    return buffer.toString();
  }
}
