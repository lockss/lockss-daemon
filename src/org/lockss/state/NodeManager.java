/*
 * $Id: NodeManager.java,v 1.20 2003-04-02 02:20:56 aalto Exp $
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

import java.util.Iterator;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.util.*;
import org.lockss.app.LockssManager;

/**
 * NodeManager handles all nodes in a given system.  It retrieves and stores
 * polls histories, manages tree walks, and maintains current state of the
 * system.
 */
public interface NodeManager extends LockssManager {
  /**
   * Starts a new poll on a particular CachedUrlSet.
   * @param cus the CachedUrlSet being polled
   * @param state the new PollState
   */
  public void startPoll(CachedUrlSet cus, PollTally state);

  /**
   * Should we allow a poll on this cached url set
   * @param cus the cached url set that represents the poll we want
   * to run.
   * @param state the polly tally representing the poll state
   * @returns false if the poll has no matching node state or
   * the poll would include damaged content.
   */
  public boolean shouldStartPoll(CachedUrlSet cus, PollTally state);

  /**
   * Update a node state with current poll results
   * @param cus the cached url set used to identify the node
   * @param results the poll results
   */
  public void updatePollResults(CachedUrlSet cus, PollTally results);

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
   * For testing only.  Forces the NodeManager to run a treewalk, if none
   * currently running.
   */
  public void forceTreeWalk();

  /**
   * For testing only.  Forces the NodeManager to call a top level poll, if none
   * currently running.
   */
  public void forceTopLevelPoll();


}
