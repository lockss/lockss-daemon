/*
 * $Id: NodeStateImpl.java,v 1.31 2005-12-01 23:28:05 troberts Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.util.*;

/**
 * NodeState contains the current state information for a node, as well as the
 * poll histories.  It automatically writes its state to disk when changed,
 * though this does not extend to changes in the classes it contains.
 */
public class NodeStateImpl
    implements NodeState, LockssSerializable {
  /**
   * This parameter indicates the maximum number of poll histories to store.
   */
  public static final String PARAM_POLL_HISTORY_MAX_COUNT =
      Configuration.PREFIX + "state.poll.history.max.count";

  static final int DEFAULT_POLL_HISTORY_MAX_COUNT = 200;

  /**
   * This parameter indicates the maximum age of poll histories to store.
   */
  public static final String PARAM_POLL_HISTORY_MAX_AGE =
      Configuration.PREFIX + "state.poll.history.max.age";

  static final long DEFAULT_POLL_HISTORY_MAX_AGE = 52 * Constants.WEEK;

  protected transient CachedUrlSet cus;
  protected CrawlState crawlState;
  protected List polls;
  protected transient List pollHistories = null; // CASTOR: probably unnecessary
  protected transient HistoryRepository repository;
  protected long hashDuration = -1;
  protected int curState = INITIAL;

  /**
   * Constructor to create NodeState from a NodeStateBean after unmarshalling.
   * @param cus CachedUrlSet
   * @param bean NodeStateBean
   * @param repository HistoryRepository
   */
  NodeStateImpl(CachedUrlSet cus, NodeStateBean bean,
                HistoryRepository repository) {
    // CASTOR: a priori unneeded after Castor is phased out
    this.cus = cus;
    this.crawlState = new CrawlState(bean.getCrawlStateBean());
    this.polls = new ArrayList(bean.pollBeans.size());
    for (int ii=0; ii<bean.pollBeans.size(); ii++) {
      polls.add(ii, new PollState((PollStateBean)bean.pollBeans.get(ii)));
    }
    this.hashDuration = bean.getAverageHashDuration();
    this.curState = bean.getState();
    this.repository = repository;
  }

  /**
   * Standard constructor to create a NodeState.
   * @param cus CachedUrlSet
   * @param hashDuration long
   * @param crawlState CrawlState
   * @param polls List of PollState objects
   * @param repository HistoryRepository
   */
  NodeStateImpl(CachedUrlSet cus, long hashDuration, CrawlState crawlState,
                List polls, HistoryRepository repository) {
    this.cus = cus;
    this.crawlState = crawlState;
    this.polls = polls;
    this.repository = repository;
    this.hashDuration = hashDuration;
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public CrawlState getCrawlState() {
    return crawlState;
  }

  public int getState() {
    return curState;
  }

  public String getStateString() {
    switch (curState) {
      case NodeState.NEEDS_POLL:
        return "Needs Poll";
      case NodeState.NEEDS_REPLAY_POLL:
        return "Needs Replay Poll";
      case NodeState.CONTENT_LOST:
        return "Content Lost";
      case NodeState.UNREPAIRABLE_NAMES_NEEDS_POLL:
        return "Unrepairable Names: Needs Poll";
      case NodeState.CONTENT_RUNNING:
        return "Content Poll Running";
      case NodeState.CONTENT_REPLAYING:
        return "Content Poll Replaying";
      case NodeState.NAME_RUNNING:
        return "Name Poll Running";
      case NodeState.NAME_REPLAYING:
        return "Name Poll Replaying";
      case NodeState.SNCUSS_POLL_RUNNING:
        return "SNCUSS Content Poll Running";
      case NodeState.SNCUSS_POLL_REPLAYING:
        return "SNCUSS Content Poll Replaying";
      case NodeState.WRONG_NAMES:
        return "Wrong Names";
      case NodeState.DAMAGE_AT_OR_BELOW:
        return "Damage At Or Below";
      case NodeState.POSSIBLE_DAMAGE_HERE:
        return "Possible Damage Here";
      case NodeState.UNREPAIRABLE_SNCUSS_NEEDS_POLL:
        return "Unrepairable SNCUSS Content: Needs Poll";
      case NodeState.NEEDS_REPAIR:
        return "Needs Repair";
      case NodeState.UNREPAIRABLE_SNCUSS:
        return "Unrepairable SNCUSS Content";
      case NodeState.UNREPAIRABLE_NAMES:
        return "Unrepairable Names";
      case NodeState.POSSIBLE_DAMAGE_BELOW:
        return "Possible Damage Below";
      case NodeState.INITIAL:
        return "Initial";
      case NodeState.OK:
        return "Ok";
      default:
        return Integer.toString(curState);
    }
  }

  public long getAverageHashDuration() {
    return hashDuration;
  }

  void setLastHashDuration(long newDuration) {
    hashDuration = newDuration;
    repository.storeNodeState(this);
  }

  public void setState(int newState) {
    curState = newState;
    repository.storeNodeState(this);
  }

  public Iterator getActivePolls() {
    return (new ArrayList(polls)).iterator();
  }

  public Iterator getPollHistories() {
    if (pollHistories==null) {
      repository.loadPollHistories(this);
      trimHistoriesIfNeeded(false);
    }
    return (new ArrayList(pollHistories)).iterator();
  }

  public PollHistory getLastPollHistory() {
    if (pollHistories==null) {
      repository.loadPollHistories(this);
      trimHistoriesIfNeeded(false);
    }
    // history list is sorted
    Iterator historyIt = pollHistories.iterator();
    if (historyIt.hasNext()) {
      return (PollHistory)historyIt.next();
    } else {
      return null;
    }
  }

  public boolean isInternalNode() {
    return !cus.isLeaf();
  }

  public boolean hasDamage() {
    NodeManagerImpl nodeMan =
        (NodeManagerImpl)LockssDaemon.getAuManager(LockssDaemon.NODE_MANAGER,
                                                   cus.getArchivalUnit());
    return nodeMan.hasDamage(cus);
  }

  protected void addPollState(PollState new_poll) {
    polls.add(new_poll);
    // write-through
    repository.storeNodeState(this);
  }

  protected synchronized void closeActivePoll(PollHistory finished_poll) {
    if (pollHistories==null) {
      repository.loadPollHistories(this);
      Collections.sort(pollHistories, new HistoryComparator());
    }
    // since the list is sorted, find the right place to add
    Comparator comp = new HistoryComparator();
    boolean added = false;
    for (int ii=0; ii<pollHistories.size(); ii++) {
      if (comp.compare(finished_poll, pollHistories.get(ii))<0) {
        pollHistories.add(ii, finished_poll);
        added = true;
        break;
      }
    }
    if (!added) {
      pollHistories.add(finished_poll);
    }
    // remove this poll, and any lingering PollStates for it
    while (polls.contains(finished_poll)) {
//XXX concurrent
      polls.remove(finished_poll);
    }

    // trim
    trimHistoriesIfNeeded(true);

    // checkpoint state, store histories
    repository.storeNodeState(this);
    repository.storePollHistories(this);
  }

  protected void setPollHistoryList(List new_histories) {
    pollHistories = (new_histories == null) ? new ArrayList(0) : new_histories;
    trimHistoriesIfNeeded(false);
  }

  protected List getPollHistoryList() {
    return (pollHistories == null) ? new ArrayList() : pollHistories;
  }

  /**
   * Trims histories which exceed maximum count or age.
   * Sorts the list if not sorted.
   * @param isSorted true iff sorted, for efficiency
   */
  void trimHistoriesIfNeeded(boolean isSorted) {
    if (pollHistories.size() > 0) {
      // sort if needed
      if (!isSorted) {
        Collections.sort(pollHistories, new HistoryComparator());
      }
      // trim oldest off if exceeds max size
      int maxHistoryCount =
        CurrentConfig.getIntParam(PARAM_POLL_HISTORY_MAX_COUNT,
                                  DEFAULT_POLL_HISTORY_MAX_COUNT);
      if (maxHistoryCount <= 0) {
        maxHistoryCount = DEFAULT_POLL_HISTORY_MAX_COUNT;
      }
      while (pollHistories.size() > maxHistoryCount) {
        pollHistories.remove(maxHistoryCount);
      }
      // trim any remaining which exceed max age
      long maxHistoryAge = CurrentConfig.getLongParam(PARAM_POLL_HISTORY_MAX_AGE,
                                                      DEFAULT_POLL_HISTORY_MAX_AGE);
      while (true) {
        int size = pollHistories.size();
        PollHistory history = (PollHistory)pollHistories.get(size-1);
        long pollEnd = history.getStartTime() + history.duration;
        if (TimeBase.msSince(pollEnd) > maxHistoryAge) {
          pollHistories.remove(size-1);
          if (size==1) {
            // no histories left
            break;
          }
        } else {
          break;
        }
      }
    }
  }

  /**
   * Comparator to sort PollHistory objects.  Sorts by start time, in reverse
   * order (most recent first).
   */
  static class HistoryComparator implements Comparator {
    // sorts in reverse order, with largest startTime first
    public int compare(Object o1, Object o2) {
      long startTime1;
      long startTime2;
      if ((o1 instanceof PollHistory) && (o2 instanceof PollHistory)) {
        startTime1 = ((PollHistory)o1).getStartTime();
        startTime2 = ((PollHistory)o2).getStartTime();
      } else {
        throw new IllegalStateException("Bad object in iterator: " +
                                        o1.getClass() + "," +
                                        o2.getClass());
      }
      if (startTime1>startTime2) {
        return -1;
      } else if (startTime1<startTime2) {
        return 1;
      } else {
        return 0;
      }
    }
  }

  public void setCachedUrlSet(CachedUrlSet cus) {
    this.cus = cus;
  }

}
