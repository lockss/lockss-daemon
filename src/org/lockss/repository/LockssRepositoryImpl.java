/*
 * $Id: LockssRepositoryImpl.java,v 1.13 2003-02-05 23:33:29 aalto Exp $
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
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.daemon.ArchivalUnit;
import org.lockss.daemon.Configuration;
import org.apache.commons.collections.LRUMap;
import org.apache.commons.collections.ReferenceMap;
import org.lockss.util.FileLocationUtil;
import org.lockss.daemon.*;
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
  private LRUMap nodeCache;
  private ReferenceMap refMap;
  private int cacheHits = 0;
  private int cacheMisses = 0;
  private int refHits = 0;
  private int refMisses = 0;
  private static Logger logger = Logger.getLogger("LockssRepository");

  private LockssRepositoryImpl(String rootPath) {
    rootLocation = rootPath;
    if (!rootLocation.endsWith(File.separator)) {
      // this shouldn't happen
      rootLocation += File.separator;
    }
    nodeCache = new LRUMap(MAX_LRUMAP_SIZE);
    refMap = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
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
    String url1 = cus1.getPrimaryUrl();
    String url2 = cus2.getPrimaryUrl();
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
    // check LRUMap cache for node
    RepositoryNode node = (RepositoryNode)nodeCache.get(url);
    if (node!=null) {
      cacheHits++;
      return node;
    } else {
      cacheMisses++;
    }

    // check weak reference map for node
    node = (RepositoryNode)refMap.get(url);
    if (node!=null) {
      refHits++;
      nodeCache.put(url, node);
      return node;
    } else {
      refMisses++;
    }

    String nodeLocation = FileLocationUtil.mapUrlToFileLocation(rootLocation, url);
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
    node = new RepositoryNodeImpl(url, nodeLocation, this);

    // add to node cache and weak reference cache
    nodeCache.put(url, node);
    refMap.put(url, node);
    return node;
  }

  /**
   * Removes a weak reference from the reference map.  Called by finalize()
   * of {@link RepositoryNodeImpl}.
   * @param url the reference key url
   */
  synchronized void removeReference(String url) {
    refMap.remove(url);
  }

  int getCacheHits() { return cacheHits; }
  int getCacheMisses() { return cacheMisses; }
  int getRefHits() { return refHits; }
  int getRefMisses() { return refMisses; }

  /**
   * Creates a LockssRepository for the given {@link ArchivalUnit} at
   * a cache location specific to that archive.
   * @param au ArchivalUnit to be cached
   * @return a repository for the archive
   */
  public static LockssRepository repositoryFactory(ArchivalUnit au) {
    String rootLocation = Configuration.getParam(PARAM_CACHE_LOCATION);
    if (rootLocation==null) {
      logger.error("Couldn't get "+PARAM_CACHE_LOCATION+" from Configuration");
      throw new LockssRepository.RepositoryStateException("Couldn't load param.");
    }
    StringBuffer buffer = new StringBuffer(rootLocation);
    if (!rootLocation.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(CACHE_ROOT_NAME);
    buffer.append(File.separator);
    return new LockssRepositoryImpl(FileLocationUtil.mapAuToFileLocation(buffer.toString(), au));
  }
}
