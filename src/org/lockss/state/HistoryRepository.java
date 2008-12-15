/*
 * $Id: HistoryRepository.java,v 1.18 2008-12-15 19:33:23 edwardsb1 Exp $
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

package org.lockss.state;

import java.io.*;
import java.util.List;
import org.lockss.plugin.*;
import org.lockss.protocol.DatedPeerIdSet;
import org.lockss.repository.jcr.*;
import org.lockss.app.LockssAuManager;

/**
 * HistoryRepository is an inner layer of the NodeManager which handles the actual
 * storage of NodeStates.
 */
public interface HistoryRepository extends LockssAuManager {

  /**
   * Stores the current node state info, except the histories.
   * @param nodeState the NodeState
   */
  public void storeNodeState(NodeState nodeState);

  /**
   * Loads the current node state info, except the histories.  Returns a new
   * NodeState if none found.
   * @param cus the CachedUrlSet
   * @return a {@link NodeState}
   */
  public NodeState loadNodeState(CachedUrlSet cus);

  /**
   * Stores PollHistories for a given NodeState.
   * @param nodeState to store
   */
  public void storePollHistories(NodeState nodeState);

  /**
   * Loads the poll histories into the given NodeState.
   * @param nodeState the NodeState
   */
  public void loadPollHistories(NodeState nodeState);

  /**
   * Stores IdentityAgreement list
   * @param list the list of {@link
   * org.lockss.protocol.IdentityManager.IdentityAgreement}s
   */
  public void storeIdentityAgreements(List list) throws LockssRepositoryException;

  /**
   * Loads and returns IdentityAgreement list
   * @return List the list {@link
   * org.lockss.protocol.IdentityManager.IdentityAgreement}s
   */
  public List loadIdentityAgreements() throws LockssRepositoryException;

  /** Return identity agreement File for this AU
   */
  public File getIdentityAgreementFile();

  /** Return AuState File for this AU
   */
  public File getAuStateFile();

  /**
   * Return the date/time the au was created.
   */
  public long getAuCreationTime() throws LockssRepositoryException;

  /**
   * Return the associated DatedPeerIdSet
   */
  public DatedPeerIdSet getNoAuPeerSet();
  
  /**
   * Stores the AuState for this ArchivalUnit
   * @param auState the state to store
   */
  public void storeAuState(AuState auState);

  /**
   * Loads the AuState for this ArchivalUnit
   * @return the {@link AuState}
   */
  public AuState loadAuState();

  /**
   * Stores the DamagedNodeSet for this ArchivalUnit
   * @param nodeSet the set to store
   */
  public void storeDamagedNodeSet(DamagedNodeSet nodeSet);

  /**
   * Loads the DamagedNodeSet for this ArchivalUnit
   * @return the {@link DamagedNodeSet}
   */
  public DamagedNodeSet loadDamagedNodeSet();

}
