/*
 * $Id: MockRepositoryNode.java,v 1.4 2003-04-22 01:02:02 aalto Exp $
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

package org.lockss.test;

import java.io.*;
import java.util.*;
import java.net.MalformedURLException;
import org.lockss.util.*;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.repository.*;

/**
 * Mock version of the RepositoryNode.
 */
public class MockRepositoryNode implements RepositoryNode {
  public boolean newVersionOpen = false;
  public boolean newOutputCalled = false;
  public boolean newPropsSet = false;
  public boolean inactive = false;
  public Vector children = null;
  public long contentSize = 0;
  public InputStream curInput;
  public Properties curProps;
  public int currentVersion = -1;

  public String url;
  public String nodeLocation;

  MockRepositoryNode(String url, String nodeLocation) {
    this.url = url;
    this.nodeLocation = nodeLocation;
  }

  public String getNodeUrl() {
    return url;
  }

  public boolean hasContent() {
    return currentVersion>0;
  }

  public boolean isInactive() {
    return inactive;
  }

  public long getContentSize() {
    return contentSize;
  }

  public long getTreeContentSize(CachedUrlSetSpec filter) {
    throw new UnsupportedOperationException("Not supported.");
  }

  public Properties getState() {
    return null;
  }

  public void storeState(Properties newProps) {
  }

  public boolean isLeaf() {
    return (children==null || children.size()>0);
  }

  public Iterator listNodes(CachedUrlSetSpec filter, boolean includeInactive) {
    return children.iterator();
  }

  public int getCurrentVersion() {
    return currentVersion;
  }

  public synchronized void makeNewVersion() {
    if (newVersionOpen) {
      throw new UnsupportedOperationException("New version already initialized.");
    }
    newVersionOpen = true;
  }

  public synchronized void sealNewVersion() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    currentVersion++;
    curProps = null;
    newOutputCalled = false;
    newPropsSet = false;
    newVersionOpen = false;
  }

  public synchronized void abandonNewVersion() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    newOutputCalled = false;
    newPropsSet = false;
    newVersionOpen = false;
  }

  public synchronized void deactivate() {
    if (newVersionOpen) {
      throw new UnsupportedOperationException("Can't deactivate while new version open.");
    }
    inactive = true;
    curProps = null;
  }

  public synchronized void restoreLastVersion() {
    throw new UnsupportedOperationException("Not supported.");
  }

  public synchronized RepositoryNode.RepositoryNodeContents getNodeContents() {
    if (!hasContent()) {
      throw new UnsupportedOperationException("No content for url '"+url+"'");
    }
    return new RepositoryNodeContents(curInput, curProps);
  }

  public OutputStream getNewOutputStream() {
    throw new UnsupportedOperationException("Not supported.");
  }

  public void setNewProperties(Properties newProps) {
    throw new UnsupportedOperationException("Not supported.");
  }

}
