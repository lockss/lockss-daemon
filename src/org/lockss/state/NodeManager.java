/*
 * $Id: NodeManager.java,v 1.32 2006-03-01 02:50:13 smorabito Exp $
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
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.app.*;
import org.lockss.daemon.ActivityRegulator;

/**
 * NodeManager handles all nodes in a given system.  It retrieves and stores
 * polls histories, manages tree walks, and maintains current state of the
 * system.
 */
public interface NodeManager extends LockssAuManager {
  /**
   * Starts a new poll on a particular CachedUrlSet.
   * @param cus the CachedUrlSet being polled
   * @param state the new PollState
   * @param isReplayPoll true if we are replaying the previous poll.
   */
  public void startPoll(CachedUrlSet cus, Tallier state, boolean isReplayPoll);

  /**
   * Should we allow a poll on this cached url set
   * @param cus the cached url set that represents the poll we want
   * to run.
   * @param pollState the polly tally representing the poll state
   * @return false if the poll has no matching node state or
   * the poll would include damaged content.
   */
  public boolean shouldStartPoll(CachedUrlSet cus, Tallier pollState);

  /**
   * Update a node state with current poll results
   * @param cus the cached url set used to identify the node
   * @param results the poll results
   */
  public void updatePollResults(CachedUrlSet cus, Tallier results);

  /**
   * Return the node represented by a given CachedUrlSet
   * @param cus the cached url set used to identify the top node
   * @return the NodeState
   */
  public NodeState getNodeState(CachedUrlSet cus);

  /**
   * Returns the au state for the managed ArchivalUnit
   * @return the au state
   */
  public AuState getAuState();

  /**
   * Alerts the NodeManager that a new top-level content crawl has finished.
   */
  public void newContentCrawlFinished();

  /**
   * Alerts the NodeManager that a hash finished on a specific
   * {@link CachedUrlSet} with the specified duration.
   * @param cus the {@link CachedUrlSet}
   * @param hashDuration the duration, in ms
   */
  public void hashFinished(CachedUrlSet cus, long hashDuration);

  /**
   * Returns true if the node manager has scheduled repairs which were never
   * completed.
   * @return boolean iff repairs needed
   */
  public boolean repairsNeeded();

  /**
   * Schedules any necessary repairs.  Takes an AU-level lock and converts
   * it into the necessary CUS-level repair locks.
   * @param auLock the Activity lock for the whole AU
   */
  public void scheduleRepairs(ActivityRegulator.Lock auLock);


  /**
   * Returns the set of damaged nodes for the given AU
   */
  public DamagedNodeSet getDamagedNodes();

  /**
   * Mark the given CachedUrlSet deleted.
   * 
   * @param cus The CUS to delete.
   */
  public void deleteNode(CachedUrlSet cus) throws IOException;
  
  /**
   * Looks at the state of the node, and indicates if a poll needs to be called.
   * It does not schedule polls, which should be done via
   * 'callNecessaryPolls()'.  Called from the treewalk
   * @param lastOrCurrentPoll the most recent poll (could be active)
   * @param nodeState the {@link NodeState}
   * @return true if action should be taken
   * @throws IOException
   */
  boolean checkCurrentState(PollState lastOrCurrentPoll, NodeState nodeState)
      throws IOException;

  /**
   * Looks at the state of the node, and takes appropriate action.  Called
   * from the treewalk.
   * @param lastOrCurrentPoll the most recent poll (could be active)
   * @param nodeState the {@link NodeState}
   * @throws IOException
   */
  void callNecessaryPolls(PollState lastOrCurrentPoll, NodeState nodeState)
      throws IOException;
}
