/*
 * $Id: NodeStateImpl.java,v 1.37.8.1 2007-08-30 09:55:33 smorabito Exp $
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

import org.lockss.app.*;
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
  static Logger log = Logger.getLogger("NodeState");

  /**
   * The maximum number of poll histories to store.
   */
  public static final String PARAM_POLL_HISTORY_MAX_COUNT =
      Configuration.PREFIX + "state.pollHistory.maxSize";

  static final int DEFAULT_POLL_HISTORY_MAX_COUNT = 200;

  /**
   * The size to which to trim the poll history list when it exceeds
   * maxSize.  Default is same as maxSize.
   */
  public static final String PARAM_POLL_HISTORY_TRIM_TO =
      Configuration.PREFIX + "state.pollHistory.trimTo";

  /**
   * This parameter indicates the maximum age of poll histories to store.
   */
  public static final String PARAM_POLL_HISTORY_MAX_AGE =
      Configuration.PREFIX + "state.pollHistory.maxAge";

  static final long DEFAULT_POLL_HISTORY_MAX_AGE = 52 * Constants.WEEK;

  /**
   * If true, poll history files are rewritten if trimmed after loading
   */
  public static final String PARAM_POLL_HISTORY_TRIM_REWRITE =
      Configuration.PREFIX + "state.pollHistory.trimRewrite";
  static final boolean DEFAULT_POLL_HISTORY_TRIM_REWRITE = false;



  protected transient CachedUrlSet cus;
  protected CrawlState crawlState;
  protected List polls;
  protected List activeV3Polls;
  protected List completedV3Polls;
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
    this.activeV3Polls = new ArrayList();
    this.completedV3Polls = new ArrayList();
    this.repository = repository;
    this.hashDuration = hashDuration;
  }
  
  /**
   * When unmarshalling this object, check for any pre-Daemon 1.22
   * nodestate objects and initialize null fields.
   */
  protected void postUnmarshal(LockssApp context) {
    if (activeV3Polls == null) {
      activeV3Polls = new ArrayList();
    }
    if (completedV3Polls == null) {
      completedV3Polls = new ArrayList();
    }
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

  private synchronized List findPollHistories() {
    if (pollHistories==null) {
      repository.loadPollHistories(this);
      // repository.loadPollHistories() calls setPollHistoryList(), which
      // calls trimHistoriesIfNeeded()
//       trimHistoriesIfNeeded(false);
    }
    return pollHistories;
  }

  public Iterator getPollHistories() {
    return (new ArrayList(findPollHistories())).iterator();
  }

  public PollHistory getLastPollHistory() {
    findPollHistories();
    if (pollHistories.isEmpty()) {
      return null;
    }
    return (PollHistory)pollHistories.get(0);
  }

  public boolean isInternalNode() {
    return !cus.isLeaf();
  }

  public boolean hasDamage() {
    NodeManagerImpl nodeMan = (NodeManagerImpl)
      LockssDaemon.getStaticAuManager(LockssDaemon.NODE_MANAGER,
				      cus.getArchivalUnit());
    return nodeMan.hasDamage(cus);
  }

  protected void addPollState(PollState new_poll) {
    polls.add(new_poll);
    // write-through
    repository.storeNodeState(this);
  }

  protected synchronized void closeActivePoll(PollHistory finished_poll) {
    findPollHistories();
    // the list is sorted, find the right place to add
    int pos = Collections.binarySearch(pollHistories,
					  finished_poll,
					  new HistoryComparator());
    if (pos >= 0) {
      pollHistories.add(pos, finished_poll);
    } else {
      pollHistories.add(-(pos + 1), finished_poll);
    }
    // remove this poll, and any lingering PollStates for it
    while (polls.contains(finished_poll)) {
//XXX concurrent
      polls.remove(finished_poll);
    }

    // trim
    trimHistoriesIfNeeded();

    // checkpoint state, store histories
    repository.storeNodeState(this);
    repository.storePollHistories(this);
  }

  protected void setPollHistoryList(List new_histories) {
    pollHistories = (new_histories == null) ? new ArrayList(0) : new_histories;
    sortPollHistories();
    if (trimHistoriesIfNeeded()) {
      if (CurrentConfig.getBooleanParam(PARAM_POLL_HISTORY_TRIM_REWRITE,
					DEFAULT_POLL_HISTORY_TRIM_REWRITE)) {
	log.debug("Rewriting trimmed poll history for " + cus.getUrl());
	repository.storePollHistories(this);
      }
    }
  }

  void sortPollHistories() {
    Collections.sort(pollHistories, new HistoryComparator());
  }

  protected List getPollHistoryList() {
    return (pollHistories == null) ? new ArrayList() : pollHistories;
  }

  /**
   * Trims histories which exceed maximum count or age.
   * Assumes list is sorted
   * @return true iff list was trimmed
   */
  synchronized boolean trimHistoriesIfNeeded() {
    boolean changed = false;

    int max = CurrentConfig.getIntParam(PARAM_POLL_HISTORY_MAX_COUNT,
                                        DEFAULT_POLL_HISTORY_MAX_COUNT);
    long maxHistoryAge =
        CurrentConfig.getLongParam(PARAM_POLL_HISTORY_MAX_AGE,
                                   DEFAULT_POLL_HISTORY_MAX_AGE);

    if (pollHistories.size() > 0) {
      // trim any that are too old.
      int size;
      int cnt = 0;
      while ((size = pollHistories.size()) > 0) {
        PollHistory history = (PollHistory)pollHistories.get(size-1);
        long pollEnd = history.getStartTime() + history.duration;
        if (TimeBase.msSince(pollEnd) > maxHistoryAge) {
          pollHistories.remove(size-1);
	  changed = true;
	  cnt++;
        } else {
          break;
        }
      }
      if (cnt > 0) {
	log.debug2("Poll history trimmed " + cnt + " polls due to age");
      }

      // trim oldest off if exceeds max size
      if (pollHistories.size() > max) {
	int trimTo =
	  CurrentConfig.getIntParam(PARAM_POLL_HISTORY_TRIM_TO, max);
	log.debug2("Poll history size (" + pollHistories.size() +
		   ") exceeds " + max + ", trimming to " + trimTo);
	while (pollHistories.size() > trimTo) {
	  pollHistories.remove(pollHistories.size() - 1);
	  changed = true;
	}
      }
    }

    // XXX: Hacked on for V3 history.  Trim V3 list for age and length.

    if (completedV3Polls != null && completedV3Polls.size() > 0) {
      // trim any that are too old.
      for (int ix = 0; ix < completedV3Polls.size() - 1; ix++ ) {
        V3PollState ps = (V3PollState)completedV3Polls.get(ix);
        // Remove based on poll end time, not start time.  Polls are only
        // appended to the completed list when they end.
        if (TimeBase.msSince(ps.getEndTime()) > maxHistoryAge) {
          completedV3Polls.remove(ix);
          changed = true;
        } else {
          break; // Stop looking.
        }
      }
      
      // trim off oldest polls if the list exceeds max size
      if (completedV3Polls.size() > max) {
        int trimTo =
          CurrentConfig.getIntParam(PARAM_POLL_HISTORY_TRIM_TO, max);
        while (completedV3Polls.size() > trimTo) {
          completedV3Polls.remove(completedV3Polls.size() - 1);
          changed = true;
        }
      }
    }

    return changed;
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
  
  /*
   * XXX: Hacked on for V3, and not an example of proper design.  The Node
   * Manager and probably the entire Repository are getting a major overhaul
   * in the near future, and we're moving away from V1, so an entire
   * re-design seems worthwhile.
   */

  public Collection getActiveV3Polls() {
    return activeV3Polls;
  }
  
  public Collection getCompletedV3Polls() {
    return completedV3Polls;
  }
  
  public synchronized void addV3PollState(V3PollState state) {
    activeV3Polls.add(state);
  }
  
  public synchronized V3PollState getLastV3PollState() {
    return (V3PollState)completedV3Polls.get(completedV3Polls.size() - 1);
  }
  
  public synchronized void closeV3Poll(String key) {
    for (int idx = 0; idx < activeV3Polls.size(); idx++) { 
      V3PollState state = (V3PollState)activeV3Polls.get(idx);
      if (key.equals(state.getKey())) {
        activeV3Polls.remove(idx);
        completedV3Polls.add(state);
      }
    }
  }
  
}
