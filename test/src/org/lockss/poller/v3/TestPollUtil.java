/*
 * $Id: TestPollUtil.java,v 1.2 2008-08-11 23:32:59 tlipkis Exp $
 */

/*

 Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.config.ConfigManager;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.scheduler.*;
import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.test.*;
import org.lockss.hasher.*;
import static org.lockss.poller.v3.V3Poller.*;

public class TestPollUtil extends LockssTestCase {

  private MockLockssDaemon theDaemon;

  private PollManager pm;
  private MyMockSchedService schedSvc;
  private HashService hashService;
  Properties p;

  public void setUp() throws Exception {
    super.setUp();
    p = defaultProps();
    ConfigurationUtil.setCurrentConfigFromProps(p);
    theDaemon = getMockLockssDaemon();
    schedSvc = new MyMockSchedService();
    schedSvc.initService(theDaemon);
    theDaemon.setSchedService(schedSvc);
    pm = theDaemon.getPollManager();
    TimeBase.setSimulated(2000);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  Properties defaultProps() {
    Properties p = new Properties();
    p.put(PARAM_MAX_POLL_DURATION, "10000");
    p.put(PARAM_MIN_POLL_DURATION, "100");
    p.put(PARAM_VOTE_DURATION_MULTIPLIER, "2");
    p.put(PARAM_VOTE_DURATION_PADDING, "20");
    p.put(PARAM_TALLY_DURATION_MULTIPLIER, "3");
    p.put(PARAM_TALLY_DURATION_PADDING, "30");
    p.put(PARAM_RECEIPT_PADDING, "10");
    p.put(PARAM_POLL_EXTEND_MULTIPLIER, "2");
    p.put(PARAM_MAX_POLL_EXTEND_MULTIPLIER, "20");
    return p;
  }

  public void testTargetVoteDuration() {
    assertEquals(220, PollUtil.v3TargetVoteDuration(100));
    assertEquals(1020, PollUtil.v3TargetVoteDuration(500));
  }

  public void testTargetTallyDuration() {
    assertEquals(330, PollUtil.v3TargetTallyDuration(100));
    assertEquals(1530, PollUtil.v3TargetTallyDuration(500));
  }

  public void testCalcV3Duration() {
    // succeed on first try
    schedSvc.okIntrvl = new TimeInterval(2220, 2550);
    assertEquals(560, PollUtil.calcV3Duration(100, pm));
    assertEquals(1, schedSvc.events.size());
    assertEquals(new TimeInterval(2220, 2550),
		 PollUtil.calcV3TallyWindow(100, 560));

    // succeed on third try
    schedSvc.reset();
    schedSvc.okIntrvl = new TimeInterval(2572, 3430);
    assertEquals(1440, PollUtil.calcV3Duration(100, pm));
    assertEquals(3, schedSvc.events.size());
    assertEquals(new TimeInterval(2572, 3430),
		 PollUtil.calcV3TallyWindow(100, 1440));

    // fail
    schedSvc.reset();
    schedSvc.okIntrvl = null;
    assertEquals(-1, PollUtil.calcV3Duration(100, pm));
    assertEquals(22, schedSvc.events.size());
  }

  public void testGetPollStateRoot() {
    ConfigurationUtil.setFromArgs(V3Poller.PARAM_STATE_PATH, "/tmp/path");
    assertEquals("/tmp/path",
		 PollUtil.getPollStateRoot().toString());
    ConfigurationUtil.setFromArgs(V3Poller.PARAM_REL_STATE_PATH, "path/2",
				  ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  "/mount/point");
    assertEquals("/mount/point/path/2",
		 PollUtil.getPollStateRoot().toString());
  }


  public class MyMockSchedService extends MockSchedService {

    List events = new ArrayList();
    TimeInterval okIntrvl;

    public MyMockSchedService() {
    }

    public boolean isTaskSchedulable(SchedulableTask task) {
      Interval tIntrvl =
	new Interval(task.getEarliestStart().getExpirationTime(),
		     task.getLatestFinish().getExpirationTime());
      events.add(tIntrvl);
      return tIntrvl.equals(okIntrvl);
    }

    void reset() {
      events = new ArrayList();
    }
  }
}
