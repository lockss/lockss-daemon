/*
 * $Id: NodeStateImpl.java,v 1.17 2003-05-09 20:53:30 aalto Exp $
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

import java.util.*;
import org.lockss.plugin.CachedUrlSet;
import org.apache.commons.collections.TreeBag;

/**
 * NodeState contains the current state information for a node, as well as the
 * poll histories.  It automatically writes its state to disk when changed,
 * though this does not extend to changes in the classes it contains.
 */
public class NodeStateImpl implements NodeState {
  protected CachedUrlSet cus;
  protected CrawlState crawlState;
  protected List polls;
  protected List pollHistories = null;
  protected HistoryRepository repository;
  protected long hashDuration = -1;

  // for marshalling only
  NodeStateImpl() { }

  NodeStateImpl(CachedUrlSet cus, NodeStateBean bean,
                HistoryRepository repository) {
    this.cus = cus;
    this.crawlState = new CrawlState(bean.getCrawlStateBean());
    this.polls = new ArrayList(bean.pollBeans.size());
    for (int ii=0; ii<bean.pollBeans.size(); ii++) {
      polls.add(ii, new PollState((PollStateBean)bean.pollBeans.get(ii)));
    }
    this.hashDuration = bean.getAverageHashDuration();
    this.repository = repository;
  }

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

  public long getAverageHashDuration() {
    return hashDuration;
  }

  void setLastHashDuration(long newDuration) {
    hashDuration = newDuration;
    repository.storeNodeState(this);
  }

  public Iterator getActivePolls() {
    return Collections.unmodifiableList(polls).iterator();
  }

  public Iterator getPollHistories() {
    if (pollHistories==null) {
      repository.loadPollHistories(this);
      Collections.sort(pollHistories, new HistoryComparator());
    }
    return Collections.unmodifiableList(pollHistories).iterator();
  }

  public PollHistory getLastPollHistory() {
    if (pollHistories==null) {
      repository.loadPollHistories(this);
      Collections.sort(pollHistories, new HistoryComparator());
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

  protected void addPollState(PollState new_poll) {
    polls.add(new_poll);
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
      polls.remove(finished_poll);
    }
    // checkpoint state, store histories
    repository.storeNodeState(this);
    repository.storePollHistories(this);
  }

  protected void setPollHistoryBeanList(List new_histories) {
    // create new sorted list
    pollHistories = new ArrayList(new_histories.size());
    Iterator beanIter = new_histories.iterator();
    while (beanIter.hasNext()) {
      PollHistoryBean bean = (PollHistoryBean)beanIter.next();
      pollHistories.add(bean.getPollHistory());
    }
    Collections.sort(pollHistories, new HistoryComparator());
  }

  protected List getPollHistoryBeanList() {
    if (pollHistories==null) {
      return Collections.EMPTY_LIST;
    }
    List histBeans = new ArrayList(pollHistories.size());
    Iterator histIter = pollHistories.iterator();
    while (histIter.hasNext()) {
      PollHistory history = (PollHistory)histIter.next();
      histBeans.add(new PollHistoryBean(history));
    }
    return histBeans;
  }

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

}