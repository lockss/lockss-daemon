/*
 * $Id: LockssRepositoryImpl.java,v 1.9 2002-12-02 00:35:50 tal Exp $
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

    String nodeLocation = mapUrlToCacheLocation(rootLocation, url);
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
   * mapUrlToCacheFileName() is the name mapping method used by the
   * LockssRepository. This maps a given url to a cache file location, using
   * the cache root as the base.  It creates directories under a CACHE_ROOT_NAME
   * directory which mirror the html string. So
   * 'http://www.journal.org/issue1/index.html' would be cached in the file:
   * cacheRoot/CACHE_ROOT_NAME/www.journal.org/http/issue1/index.html
   * @param cacheRoot the file root of the cache
   * @param urlStr the url to translate
   * @return the file cache location
   * @throws java.net.MalformedURLException
   */
  public static String mapUrlToCacheLocation(String cacheRoot, String urlStr)
      throws MalformedURLException {
    int totalLength = cacheRoot.length() + urlStr.length();
    URL url = new URL(urlStr);
    StringBuffer buffer = new StringBuffer(totalLength);
    buffer.append(cacheRoot);
    if (!cacheRoot.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(url.getHost());
    buffer.append(File.separator);
    buffer.append(url.getProtocol());
    buffer.append(StringUtil.replaceString(url.getPath(), "/", File.separator));
    return buffer.toString();
  }

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
    buffer.append(au.getPluginId());
    if (!au.getAUId().equals("")) {
      buffer.append(File.separator);
      buffer.append(au.getAUId());
    }
    buffer.append(File.separator);
    return new LockssRepositoryImpl(buffer.toString());
  }
}
