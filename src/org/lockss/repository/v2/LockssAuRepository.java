/**

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository.v2;

import java.io.*;
import java.net.MalformedURLException;
import java.util.List;

import org.lockss.app.LockssAuManager;
import org.lockss.protocol.PersistentPeerIdSet;
import org.lockss.repository.*;
import org.lockss.repository.v2.RepositoryFile;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.state.*;

/**
 * @author Brent E. Edwards
 * 
 */
public interface LockssAuRepository extends LockssAuManager {

  /**
   * Returns a {@link RepositoryNode} that represents the URL in question. This
   * method only returns URLs that exist in the cache. It's null otherwise.
   * 
   * Note that this returns a node.  In most cases, you are more likely to want
   * the repository file rather than the node.
   */
  public RepositoryNode getNode(String url, boolean create)
      throws MalformedURLException, LockssRepositoryException;
  
  public RepositoryFile getFile(String url, boolean create)
      throws MalformedURLException, LockssRepositoryException;

  /**
   * Traverses the node hierarchy and tests internal state consistency for each
   * node. Corrects and logs any correctable errors it encounters.
   */
  public void checkConsistency();

  /**
   * from RepositoryManager.queueSizeCalc: "engqueue a size calculation for the
   * node"
   */
  public void queueSizeCalc(RepositoryNode node);

  /**
   * Return the date/time the au was created.
   * 
   * Note from BEE: this method returns the number of seconds since the epoch.
   */
  public long getAuCreationTime() throws LockssRepositoryException;

  /**
   * Return AuStateFile for this AU
   */
  public File getAuStateFile();

  /**
   * Both loads and returns the AuState for this ArchivalUnit.
   */
  public AuState loadAuState();

  /**
   * Puts the AuState onto the disk.
   */
  public void storeAuState(AuState auState) throws LockssRepositoryException;

  /**
   * Loads and returns the damaged node set for this operation.
   * This method is a stub.
   */
  public DamagedNodeSet loadDamagedNodeSet();

  /**
   * Stores the DamagedNodeSet for this ArchivalUnit.
   * This method is a stub.
   */
  public void storeDamagedNodeSet(DamagedNodeSet dns);

  /**
   * This method returns the current Identity Agreement file. If one wasn't set,
   * this method returns NULL.
   */
  public File getIdentityAgreementFile();

  /**
   * This method loads and returns the identity list.
   * 
   * TODO: What type is the list?
   */
  public List loadIdentityAgreements()
  throws LockssRepositoryException;

  /**
   * Stores IdentityAgreement list.
   */
  public void storeIdentityAgreements(List identAgreements)
  throws LockssRepositoryException;

  /**
   * Loads the poll histories into the given NodeState.
   * This method is a stub.
   */
  public void loadPollHistories(NodeState nodeState)
      throws LockssRepositoryException;
  
  public void storeNoAuRawContents(InputStream istr)
      throws LockssRepositoryException;
  
  public InputStream getNoAuPeerSetRawContents() 
      throws LockssRepositoryException;
  
  public boolean hasNoAuPeerSet();
  
  /**
   * Returns the disk usage for this repository.
   * @throws LockssRepositoryException 
   */
  public long getRepoDiskUsage(boolean calcIfUnknown) throws LockssRepositoryException;
}
