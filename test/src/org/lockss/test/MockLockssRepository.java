/*
 * $Id: MockLockssRepository.java,v 1.4 2003-06-25 21:19:54 eaalto Exp $
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

package org.lockss.test;

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
import org.lockss.repository.*;

/**
 * Mock version of LockssRepository.
 */
public class MockLockssRepository implements LockssRepository {
  private String rootLocation;
  private ArchivalUnit repoAu;
  private HashMap nodeCache = new HashMap();

  public MockLockssRepository() { }

  MockLockssRepository(String rootPath, ArchivalUnit au) {
    rootLocation = rootPath;
    repoAu = au;
    nodeCache = new HashMap();
  }

  public void initService(LockssDaemon daemon) throws LockssDaemonException {

  }

  public void startService() {
  }

  public void stopService() {
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
    String urlKey;
    boolean isAuUrl = false;
    if (AuUrl.isAuUrl(url)) {
      urlKey = AuUrl.PROTOCOL;
      isAuUrl = true;
    } else {
      // no testing in Mock
      urlKey = url;
    }
    // check LRUMap cache for node
    RepositoryNode node = (RepositoryNode)nodeCache.get(urlKey);
    if (node!=null) {
      return node;
    } else if (!create) {
      return null;
    }

    String nodeLocation;
    if (isAuUrl) {
      // base directory of ArchivalUnit
      nodeLocation = rootLocation;
      node = new MockRepositoryNode(url, nodeLocation);
    } else {
      nodeLocation = LockssRepositoryImpl.mapUrlToFileLocation(rootLocation,
          url);
      node = new MockRepositoryNode(url, nodeLocation);
    }

    // add to node cache and weak reference cache
    nodeCache.put(urlKey, node);
    return node;
  }
}
