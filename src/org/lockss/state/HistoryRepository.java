/*
 * $Id: HistoryRepository.java,v 1.6 2003-05-30 23:27:53 aalto Exp $
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


package org.lockss.state;

import java.util.List;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.app.LockssManager;

/**
 * HistoryRepository is an inner layer of the NodeManager which handles the actual
 * storage of NodeStates.
 */
public interface HistoryRepository extends LockssManager {

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
   * Stores the AuState for a particular ArchivalUnit
   * @param auState the state to store
   */
  public void storeAuState(AuState auState);

  /**
   * Loads the AuState for a particular ArchivalUnit
   * @param au the ArchivalUnit state to load
   * @return the {@link AuState}
   */
  public AuState loadAuState(ArchivalUnit au);

  /**
   * Stores the DamagedNodeSet for a particular ArchivalUnit
   * @param nodeSet the set to store
   */
  public void storeDamagedNodeSet(DamagedNodeSet nodeSet);

  /**
   * Loads the DamagedNodeSet for a particular ArchivalUnit
   * @param au the ArchivalUnit state to load
   * @return the {@link DamagedNodeSet}
   */
  public DamagedNodeSet loadDamagedNodeSet(ArchivalUnit au);

}