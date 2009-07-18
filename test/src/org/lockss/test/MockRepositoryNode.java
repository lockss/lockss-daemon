/*
 * $Id: MockRepositoryNode.java,v 1.15.34.2 2009-07-18 01:28:28 edwardsb1 Exp $
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
import java.util.*;

import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.state.*;
import org.lockss.util.*;

/**
 * Mock version of the RepositoryNode.
 */
public class MockRepositoryNode implements org.lockss.repository.RepositoryNode {
  public boolean newVersionOpen = false;
  public boolean newOutputCalled = false;
  public boolean newPropsSet = false;
  public boolean inactive = false;
  public boolean deleted = false;
  public Vector children = null;
  public long contentSize = 0;
  public InputStream curInput;
  public Properties curProps;
  public int currentVersion = -1;
  public HashSet agreeingPeers = new HashSet();

  public String url;
  public String nodeLocation;

  public MockRepositoryNode() {
  }

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

  public boolean isContentInactive() {
    return inactive;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void markAsDeleted() {
    deleted = true;
  }

  public void markAsNotDeleted() {
    deleted = false;
  }

  public long getContentSize() {
    return contentSize;
  }

  public long getTreeContentSize(CachedUrlSetSpec filter,
				 boolean calcIfUnknown) {
    throw new UnsupportedOperationException("Not supported.");
  }

  public int getChildCount() {
    return children.size();
  }

  public Properties getState() {
    return null;
  }

  public void storeState(Properties newProps) {
  }

  public boolean isLeaf() {
    return (children==null || children.size()>0);
  }

  public Iterator listChildren(CachedUrlSetSpec filter, boolean includeInactive) {
    if (children == null) return CollectionUtil.EMPTY_ITERATOR;
    return children.iterator();
  }

  public int getVersion() {
    return getCurrentVersion();
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

  public synchronized void deactivateContent() {
    if (newVersionOpen) {
      throw new UnsupportedOperationException("Can't deactivate while new version open.");
    }
    inactive = true;
    curProps = null;
  }

  public synchronized void restoreLastVersion() {
    throw new UnsupportedOperationException("Not supported.");
  }

  public RepositoryNodeVersion[] getNodeVersions() {
    throw new UnsupportedOperationException("Not supported.");
  }

  public RepositoryNodeVersion[] getNodeVersions(int maxVersions) {
    throw new UnsupportedOperationException("Not supported.");
  }

  public RepositoryNodeVersion getNodeVersion(int version) {
    throw new UnsupportedOperationException("Not supported.");
  }

  public synchronized org.lockss.repository.RepositoryNode.RepositoryNodeContents getNodeContents() {
    if (!hasContent()) {
      throw new UnsupportedOperationException("No content for url '"+url+"'");
    }
    return new MockRepositoryNodeContents(curInput, curProps);
  }

  public void signalAgreement(Collection ids) {
    for (Iterator it = ids.iterator(); it.hasNext(); ) {
      agreeingPeers.add((PeerIdentity)it.next());
    }
  }
  
  public boolean hasAgreement(PeerIdentity id) {
    return agreeingPeers.contains(id);
  }
  
  public OutputStream getNewOutputStream() {
    throw new UnsupportedOperationException("Not supported.");
  }

  public void setNewProperties(Properties newProps) {
    throw new UnsupportedOperationException("Not supported.");
  }

  class MockRepositoryNodeContents implements RepositoryNodeContents {
    private InputStream is;
    private Properties props;

    private MockRepositoryNodeContents(InputStream is, Properties props) {
      this.is = is;
      this.props = props;
    }

    public InputStream getInputStream() {
      return is;
    }

    public Properties getProperties() {
      return props;
    }

    public void release() {
    }
  }

  /* The following methods are stubs, used to complete the
   * RepositoryFileVersion. 
   */
  public void commit() throws IOException, LockssRepositoryException,
      NoTextException {
    // TODO Auto-generated method stub
    
  }

  public void delete() throws LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }

  public void discard() throws LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }

  public InputStream getInputStream() throws IOException,
      LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Properties getProperties() throws IOException,
      LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public void move(String strNewLocation) throws LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }

  public void setInputStream(InputStream istr) throws IOException,
      LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }

  public void setProperties(Properties prop) throws IOException,
      LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }

  public void undelete() throws LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }

  
  public void cleanDatabase() throws LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }

  
  public RepositoryFileVersion createNewVersion()
      throws LockssRepositoryException, FileNotFoundException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public RepositoryFileVersion createNewVersionBefore(
      RepositoryFileVersion rfvBefore) throws LockssRepositoryException,
      FileNotFoundException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public PersistentPeerIdSet getAgreeingPeerIdSet()
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public long getContentSize(boolean mostRecentOnly)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return 0;
  }

  
  public RepositoryFileVersion getPreferredVersion()
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public List<RepositoryFileVersion> listVersions()
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public List<RepositoryFileVersion> listVersions(int numVersions)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public void setAgreeingPeerIdSet(PersistentPeerIdSet sepi)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }

  
  public void setPollHistories(NodeState nodeState)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }

  
  public void setPreferredVersion(RepositoryFileVersion rfv)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }

  
  public List<RepositoryFile> getFileList(CachedUrlSetSpec filter)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public List<RepositoryFile> getFileList(CachedUrlSetSpec filter,
      boolean includeDeleted) throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public RepositoryFile[] getFiles() throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public RepositoryFile[] getFiles(boolean includeDeleted)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public RepositoryFile[] getFiles(int maxVersions)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public RepositoryFile[] getFiles(int maxVersions, boolean includeDeleted)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public List<PollHistory> getPollHistoryList()
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public long getTreeContentSize(CachedUrlSetSpec filter,
      boolean calcIfUnknown, boolean preferredOnly)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return 0;
  }

  
  public NodeState loadNodeState(CachedUrlSet cus)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public RepositoryFile makeNewRepositoryFile(String name)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public RepositoryNode makeNewRepositoryNode(String name)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  
  public void storeNodeState(NodeState nodeState)
      throws LockssRepositoryException {
    // TODO Auto-generated method stub
    
  }
}
