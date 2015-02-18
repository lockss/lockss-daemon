/*
 * $Id$
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

import java.net.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.repository.*;

/**
 * Mock version of LockssRepository.
 */
public class MockLockssRepository implements LockssRepository {
  private String rootLocation;
  private ArchivalUnit repoAu;
  private HashMap nodeCache = new HashMap();

  public MockLockssRepository() { }

  public MockLockssRepository(String rootPath, ArchivalUnit au) {
    rootLocation = rootPath;
    repoAu = au;
    nodeCache = new HashMap();
  }

  public void initService(LockssApp app) throws LockssAppException {

  }

  public void startService() {
  }

  public void stopService() {
  }

  public LockssApp getApp() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setAuConfig(Configuration auConfig) {
    throw new UnsupportedOperationException("Not implemented");
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

  public void nodeConsistencyCheck() {
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

  public AuSuspectUrlVersions getSuspectUrlVersions(ArchivalUnit au) {
    return null;
  }

  public void storeSuspectUrlVersions(ArchivalUnit au,
				      AuSuspectUrlVersions asuv) {
  }

  public boolean hasSuspectUrlVersions(ArchivalUnit au) {
    return false;
  }
}
