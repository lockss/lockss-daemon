/*
 * $Id: LockssRepositoryImpl.java,v 1.6 2002-11-15 02:48:20 aalto Exp $
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
import java.util.*;
import java.net.*;
import java.lang.ref.WeakReference;
import org.lockss.util.StringUtil;
import org.apache.commons.collections.LRUMap;
import org.apache.commons.collections.ReferenceMap;
import org.lockss.daemon.*;

/**
 * LockssRepository is used to organize the urls being cached.
 * It keeps a memory cache of the most recently used nodes.
 */
public class LockssRepositoryImpl implements LockssRepository {
  /**
   * Name of top directory in which the urls are cached.
   */
  public static final String CACHE_ROOT_NAME = "cache";

  /**
   * Maximum number of node instances to cache.
   */
  public static final int MAX_LRUMAP_SIZE = 12;

  private String rootLocation;
  private LRUMap lruMap;
  private ReferenceMap refMap;


  private LockssRepositoryImpl(String rootPath) {
    rootLocation = rootPath;
    if (!rootLocation.endsWith(File.separator)) {
      rootLocation += File.separator;
    }
    lruMap = new LRUMap(MAX_LRUMAP_SIZE);
    refMap = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
  }

  public synchronized RepositoryNode getRepositoryNode(String url)
      throws MalformedURLException {
    RepositoryNode node = (RepositoryNode)lruMap.get(url);
    if (node!=null) return node;

    node = (RepositoryNode)refMap.get(url);
    if (node!=null) {
      lruMap.put(url, node);
      return node;
    }

    String nodeLocation = rootLocation + mapUrlToCacheLocation(url);
    File nodeDir = new File(nodeLocation);
    if (!nodeDir.exists() || !nodeDir.isDirectory()) {
      return null;
    }
    node = new RepositoryNodeImpl(url, nodeLocation, this);

    lruMap.put(url, node);
    refMap.put(url, node);
    return node;
  }

  public synchronized RepositoryNode createNewNode(String url)
      throws MalformedURLException {
    RepositoryNode node = null;
    node = (RepositoryNode)lruMap.get(url);
    if (node!=null) {
      return node;
    }

    node = (RepositoryNode)refMap.get(url);
    if (node!=null) {
      lruMap.put(url, node);
      return node;
    }

    String nodeLocation = rootLocation + mapUrlToCacheLocation(url);
    node = new RepositoryNodeImpl(url, nodeLocation, this);
    lruMap.put(url, node);
    refMap.put(url, node);
    return node;
  }

  synchronized void removeReference(String url) {
    refMap.remove(url);
  }

  /**
   * mapUrlToCacheFileName() is the name mapping method used by the
   * LockssRepository. This maps a given url to a cache file location.
   * It creates directories under a CACHE_ROOT location which mirror the
   * html string. So 'http://www.journal.org/issue1/index.html' would be
   * cached in the file:
   * CACHE_ROOT/www.journal.org/http/issue1/index.html
   * @param urlStr the url to translate
   * @return the file cache location
   * @throws java.net.MalformedURLException
   */
  public static String mapUrlToCacheLocation(String urlStr)
      throws MalformedURLException {
    URL url = new URL(urlStr);
    StringBuffer buffer = new StringBuffer(CACHE_ROOT_NAME);
    buffer.append(File.separator);
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
    StringBuffer buffer = new StringBuffer(Configuration.PREFIX);
    buffer.append("cache.location");
    String rootLocation = Configuration.getParam(buffer.toString(), "/tmp");
    buffer = new StringBuffer(rootLocation);
    if (!rootLocation.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(au.getPluginId());
    if (!au.getAUId().equals("")) {
      buffer.append(File.separator);
      buffer.append(au.getAUId());
    }
    return new LockssRepositoryImpl(buffer.toString());
  }
}
