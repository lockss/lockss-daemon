/*
 * $Id: MockNodeManager.java,v 1.11 2003-06-17 20:19:10 troberts Exp $
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

package org.lockss.test;

import java.util.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.app.*;

/**
 * Mock version of the NodeManager.
 */
public class MockNodeManager implements NodeManager {
  private static Logger logger = Logger.getLogger("MockNodeManager");

  private MockAuState aus = new MockAuState();
  private HashMap nodeMap = new HashMap();
  public HashMap hashCalls = new HashMap();

  public void initService(LockssDaemon daemon)
      throws LockssDaemonException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void startService() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void stopService() {
    logger.debug3("Service stopped");
  }

  public NodeManager managerFactory(ArchivalUnit au) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void startPoll(CachedUrlSet cus, PollTally state, boolean isReplay) {
    logger.debug3("starting poll for cus: " + cus);
  }

  public boolean shouldStartPoll(CachedUrlSet cus, PollTally state) {
    logger.debug3("ok to start poll");
    return true;
  }

  public void updatePollResults(CachedUrlSet cus, PollTally results) {
    logger.debug3("updating poll for cus " + cus + " with results " + results);
  }

  public NodeState getNodeState(CachedUrlSet cus) {
    return (NodeState)nodeMap.get(cus.getUrl());
  }

  public void putNodeState(NodeState state) {
    nodeMap.put(state.getCachedUrlSet().getUrl(), state);
  }

  public AuState getAuState() {
    return aus;
  }

  public void setAuState(MockAuState aus) {
    this.aus = aus;
  }

  public Iterator getActiveCrawledNodes(CachedUrlSet cus) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public Iterator getFilteredPolledNodes(CachedUrlSet cus, int filter) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public Iterator getNodeHistories(CachedUrlSet cus, int maxNumber) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public Iterator getNodeHistoriesSince(CachedUrlSet cus, Deadline since) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public long getEstimatedTreeWalkDuration() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void newContentCrawlFinished() {
    aus.newCrawlFinished();
  }

  public void hashFinished(CachedUrlSet cus, long hashDuration) {
    hashCalls.put(cus.getUrl(), new Long(hashDuration));
  }

  public void forceTreeWalk() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void forceTopLevelPoll() {
    throw new UnsupportedOperationException("Not implemented");
  }


}
