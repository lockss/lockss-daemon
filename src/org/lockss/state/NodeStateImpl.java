/*
 * $Id: NodeStateImpl.java,v 1.2 2002-12-18 00:11:59 aalto Exp $
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
import org.lockss.daemon.CachedUrlSet;

/**
 * NodeState contains the current state information for a node, as well as the
 * poll histories.
 */
public class NodeStateImpl implements NodeState {
  CachedUrlSet cus;
  CrawlState crawlState;
  List polls;
  List pollHistories = null;
  StateRepository repository;

  NodeStateImpl(CachedUrlSet cus, CrawlState crawlState, List polls,
                StateRepository repository) {
    this.cus = cus;
    this.crawlState = crawlState;
    this.polls = polls;
    this.repository = repository;
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public CrawlState getCrawlState() {
    return crawlState;
  }

  public Iterator getActivePolls() {
    return Collections.unmodifiableList(polls).iterator();
  }

  public Iterator getPollHistories() {
    if (pollHistories==null) {
      repository.loadPollHistories(this);
    }
    return Collections.unmodifiableList(pollHistories).iterator();
  }

  void addPollState(PollState new_poll) {
    polls.add(new_poll);
  }

  void closeActivePoll(PollHistory finished_poll) {
    if (pollHistories==null) {
      repository.loadPollHistories(this);
    }
    pollHistories.add(finished_poll);
    polls.remove(finished_poll);
  }
}