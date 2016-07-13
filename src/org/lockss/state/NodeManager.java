/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.app.*;
import org.lockss.daemon.ActivityRegulator;

/**
 * NodeManager handles all nodes in a given system.  It retrieves and stores
 * polls histories, manages tree walks, and maintains current state of the
 * system.
 */
public interface NodeManager extends LockssAuManager {

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
   * Alerts the NodeManager that a new top-level content crawl has finished.
   */
  public void newContentCrawlFinished(int result, String msg);

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
}
