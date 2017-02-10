/*
 * $Id$
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

import java.util.Iterator;
import org.lockss.plugin.CachedUrlSet;

/**
 * NodeState contains the current state information for a node, as well as the
 * poll histories.
 */
public interface NodeState {
  /**
   * Integer representing the initial state.
   */
  public static final int INITIAL = 0;
  /**
   * Integer representing the ok state.  Nothing needs doing.
   */
  public static final int OK = 1;
  /**
   * Integer representing the 'needs a content poll' state.
   */
  public static final int NEEDS_POLL = 2;
  /**
   * Integer representing the 'needs a content poll' state.
   */
  public static final int NEEDS_REPLAY_POLL = 3;
  /**
   * Integer representing the 'content poll is running' state.
   */
  public static final int CONTENT_RUNNING = 4;
  /**
   * Integer representing the 'content poll is replaying' state.
   */
  public static final int CONTENT_REPLAYING = 5;
  /**
   * Integer representing the 'content poll is lost, needs a name poll' state.
   */
  public static final int CONTENT_LOST = 6;
  /**
   * Integer representing the 'name poll is running' state.
   */
  public static final int NAME_RUNNING = 7;
  /**
   * Integer representing the 'name poll is replaying' state.
   */
  public static final int NAME_REPLAYING = 8;
  /**
   * Integer representing the 'failed a name poll, have a list of wrong names' state.
   */
  public static final int WRONG_NAMES = 9;
  /**
   * Integer representing the 'there is non-name damage here' state.
   */
  public static final int DAMAGE_AT_OR_BELOW = 10;
  /**
   * Integer representing the 'should check own content' state.
   */
  public static final int POSSIBLE_DAMAGE_HERE = 11;
  /**
   * Integer representing the 'sncus poll is running' state.
   */
  public static final int SNCUSS_POLL_RUNNING = 12;
  /**
   * Integer representing the 'sncus poll is replaying' state.
   */
  public static final int SNCUSS_POLL_REPLAYING = 13;
  /**
   * Integer representing the 'repair is needed here' state.
   */
  public static final int NEEDS_REPAIR = 14;
  /**
   * Integer representing the 'possible damage below here' state.
   */
  public static final int POSSIBLE_DAMAGE_BELOW = 15;
  /**
   * Integer representing the 'own content is unrepairable' state.
   */
  public static final int UNREPAIRABLE_SNCUSS = 16;
  /**
   * Integer representing the 'own content is unrepairable, time to try again'
   * state.
   */
  public static final int UNREPAIRABLE_SNCUSS_NEEDS_POLL = 17;
  /**
   * Integer representing the 'list of names is unrepairable' state.
   */
  public static final int UNREPAIRABLE_NAMES = 18;
  /**
   * Integer representing the 'list of names is unrepairable, time to try again' state.
   */
  public static final int UNREPAIRABLE_NAMES_NEEDS_POLL = 19;

  /**
   * Integer representing the 'content lost' state for a ranged poll.
   */
  public static final int RANGED_CONTENT_LOST = 20;

  /**
   * Integer representing the 'wrong names' state for a ranged poll.
   */
  public static final int RANGED_WRONG_NAMES = 21;
  /**
   * Integer representing the 'waiting' state for an unrepairable sncuss poll.
   */
  public static final int UNREPAIRABLE_SNCUSS_WAITING = 22;
  /**
   * Integer representing the 'waiting' state for an unrepairable name poll.
   */
  public static final int UNREPAIRABLE_NAMES_WAITING = 23;

  /**
   * Returns the last hash duration.
   * @return the hash duration, or -1 if no hash yet.
   */
  public long getAverageHashDuration();

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

  /**
   * Returns an int representing the overall state of the node.
   * @return the state
   */
  public int getState();

  /**
   * Sets the state.
   * @param newState the state
   */
  public void setState(int newState);

  /**
   * Returns a string from of the state of the node, for display.
   * @return the state as a string
   */
  public String getStateString();

  /**
   * Returns true iff the node or one of its children has damage.
   * @return true iff damage present
   */
  public boolean hasDamage();

}
