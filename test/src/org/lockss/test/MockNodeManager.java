/*
 * $Id: MockNodeManager.java,v 1.6 2003-03-26 23:39:40 claire Exp $
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

import java.util.Iterator;
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

  private MockAuState aus;

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

  public boolean startPoll(CachedUrlSet cus, PollTally state) {
    logger.debug3("starting poll for cus: " + cus);
    return true;
  }

  public void updatePollResults(CachedUrlSet cus, PollTally results) {
    logger.debug3("updating poll for cus " + cus + " with results " + results);
  }

  public NodeState getNodeState(CachedUrlSet cus) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public AuState getAuState() {
    if (aus == null) {
      aus = new MockAuState();
    }
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


  public void startTreeWalk() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void newContentCrawlFinished() {
    aus.newCrawlFinished();
  }
}
