/*
 * $Id: NodeManager.java,v 1.1 2002-12-04 23:59:25 aalto Exp $
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
import org.lockss.daemon.CachedUrlSet;
import org.lockss.poller.Poll;
import org.lockss.daemon.ArchivalUnit;

/**
 * NodeManager handles all nodes in a given system.  It retrieves and stores
 * polls histories, manages tree walks, and maintains current state of the
 * system.
 */
public interface NodeManager {

  /**
   * Factory method to retrieve NodeManager.
   * @return the current NodeManager
   */
  public NodeManager getNodeManager();

  /**
   * update a node state with current poll results
   * @param cus the cached url set used to identify the node
   * @param results the poll results
   */
  public void updatePollResults(CachedUrlSet cus, Poll.PollResults results);

  /**
   * Return the node represented by a given Cached Url Set
   * @param cus the cached url set used to identify the top node
   * @return the NodeState
   */
  public NodeState getNodeState(CachedUrlSet cus);

  /**
   * Returns an iterator of all node states currently being crawled
   * @param cus the cached url set used to identify the top node
   * @return an Iterator of NodeStates
   */
  public Iterator getCrawledNodes(CachedUrlSet cus);

  /**
   * Returns an interator of all node states in which a poll is running
   * @param cus the cached url set used to identify the top node
   * @return an Iterator of NodeStates
   */
  public Iterator getPolledNodes(CachedUrlSet cus);

  /**
   * Returns the estimated time it will take to walk a given
   * {@link ArchivalUnit}.  This can be used to finetune the tree walk
   * parameter settings in the Configuration.
   * @param au the au to treewalk
   * @return estimated time in ms
   */
  public long getEstimatedTreeWalkDuration(ArchivalUnit au);

}
