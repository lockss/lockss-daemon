/*
 * $Id: NodeState.java,v 1.6 2003-03-20 00:01:35 aalto Exp $
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
import org.lockss.plugin.CachedUrlSet;

/**
 * NodeState contains the current state information for a node, as well as the
 * poll histories.
 */
public interface NodeState {
  /**
   * Get the {@link CachedUrlSet} with which this NodeState is associated.
   * @return the {@link CachedUrlSet}
   */
  public CachedUrlSet getCachedUrlSet();

  /**
   * Returns the current crawl state.  If the node has been deleted, it will
   * have a {@link CrawlState} with type 'NODE_DELETED'.
   * @return a {@link CrawlState}
   */
  public CrawlState getCrawlState();

  /**
   * Returns an {@link Iterator} of the polls active on this node, if any.
   * @return an {@link Iterator} of {@link PollState}s
   */
  public Iterator getActivePolls();

  /**
   * Returns an {@link Iterator} of the polls histories for this node, if any.
   * @return an {@link Iterator} of {@link PollHistory} objects.
   */
  public Iterator getPollHistories();

  /**
   * Returns the most recent history.
   * @return the most recent {@link PollHistory}
   */
  public PollHistory getLastPollHistory();

  /**
   * Returns true if it's an internal node.
   * @return true if an internal node
   */
  public boolean isInternalNode();

}