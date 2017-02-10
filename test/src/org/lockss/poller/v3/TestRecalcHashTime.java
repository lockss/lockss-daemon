/*
 * $Id$
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
import java.security.*;
//import junit.framework.*;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.scheduler.*;


/**
 * Test class for org.lockss.hasher.HashService
 */

public class TestRecalcHashTime extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestRecalcHashTime");

  public static Class testedClasses[] = {
    org.lockss.poller.v3.RecalcHashTime.class,
  };

  protected MockLockssDaemon daemon;

  private HashService hashSvc;
  private SchedService schedSvc;
  private PollManager pollMgr;
  MockArchivalUnit mau;
  MyMockCUS mcus;
  List<Event> events;
  SimpleBinarySemaphore semEvtStart;
  SimpleBinarySemaphore semEvtDone;

  public void setUp() throws Exception {
    super.setUp();
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_NEW_SCHEDULER, "true");
    daemon = getMockLockssDaemon();
    hashSvc = daemon.getHashService();
    schedSvc = daemon.getSchedService();
    pollMgr = daemon.getPollManager();
    hashSvc.startService();
    schedSvc.startService();
    mau = new MockArchivalUnit(new MockPlugin());
    mcus = new MyMockCUS();
    // hash iterator not used but must be non-null
    mcus.setHashIterator(ListUtil.list("1").iterator());
    events = new ArrayList();
    semEvtStart = new SimpleBinarySemaphore();
    semEvtDone = new SimpleBinarySemaphore();
  }

  public void tearDown() throws Exception {
    schedSvc.stopService();
    hashSvc.stopService();
    super.tearDown();
  }

  public void test1() {
    TimeBase.setSimulated(1000);
    MyRecalcHashTime rht =
      new MyRecalcHashTime(daemon, mau, 2,
			   LcapMessage.DEFAULT_HASH_ALGORITHM,
			   1000, 100, 400);
    rht.recalcHashTime();
    assertTrue(semEvtDone.take(0));
    assertEquals(new Event("Sched true", 1000), lastEvent());
    semEvtStart.give();
    assertTrue(semEvtDone.take(TIMEOUT_SHOULDNT));
    assertEquals(new Event("Step", 1000), lastEvent());
    assertTrue(waitEvent(new Event("SetActual 400", 1400)));
    log.debug("Events: " + events);
    assertFalse(events.contains(new Event("Abort false")));
    assertFalse(events.contains(new Event("Abort true")));
    assertEquals(1, countEvents(new Event("Sched true")));
  }

  public void testResched() {
    TimeBase.setSimulated(1000);
    MyRecalcHashTime rht =
      new MyRecalcHashTime(daemon, mau, 2,
			   LcapMessage.DEFAULT_HASH_ALGORITHM,
			   1000, 100, 2500);
    rht.recalcHashTime();
    assertTrue(semEvtDone.take(0));
    assertEquals(new Event("Sched true", 1000), lastEvent());
    semEvtStart.give();
    assertTrue(semEvtDone.take(TIMEOUT_SHOULDNT));
    assertEquals(new Event("Step", 1000), lastEvent());
    assertTrue(waitEvent(new Event("SetActual 2500", 3500)));
    log.debug("Events: " + events);
    assertTrue(events.contains(new Event("Abort false")));
    assertFalse(events.contains(new Event("Abort true")));
    assertEquals(3, countEvents(new Event("Sched true")));
  }

  class MyRecalcHashTime extends RecalcHashTime {
    long eachStepTime;
    long totalHashTime;
    long cumTime = 0;

    public MyRecalcHashTime(LockssDaemon daemon,
			    ArchivalUnit au,
			    int nHash,
			    String hashAlg,
			    long voteDuration,
			    long eachStepTime,
			    long totalHashTime) {
      super(daemon, au, nHash, hashAlg, voteDuration);
      this.eachStepTime = eachStepTime;
      this.totalHashTime = totalHashTime;
    }

    @Override
    protected boolean schedRecalcHash() {
      boolean res = super.schedRecalcHash();
      recordEvent("Sched " + res);
      return res;
    }

    @Override
    protected CachedUrlSetHasher makeHasher() {
      return
	new MyRecalcHashTimeHasher(mcus,
				   PollUtil.createMessageDigestArray(nHash,
								     hashAlg),
				   initHasherByteArrays(nHash),
				   getRecalcDuration(voteDuration));
    }

    class MyRecalcHashTimeHasher extends RecalcHashTime.RecalcHashTimeHasher {
      public MyRecalcHashTimeHasher(CachedUrlSet cus,
				    MessageDigest[] digests,
				    byte[][] initByteArrays,
				    long estDuration) {
	super(cus, digests, initByteArrays, estDuration);
      }

      @Override
      public void abortHash() {
	super.abortHash();
	recordEvent("Abort " + isAborted);
      }

      @Override
      public boolean finished() {
	return cumTime >= totalHashTime;
      }

      @Override
      public int hashStep(int numBytes) {
	if (finished()) {
	  recordEvent("StepWhenFinished");
	  return 0;
	}
	semEvtStart.take();
	recordEvent("Step");
	TimeBase.step(eachStepTime);
	cumTime += eachStepTime;
	semEvtDone.give();
	try {
	  Thread.sleep(1);
	} catch (InterruptedException e) {
	  throw new RuntimeException(e.toString());
	}
	return 10;
      }
    }
  }

  public class MyMockCUS extends MockCachedUrlSet {
    long actualHashDuration;
    Exception actualHashException;

    public MyMockCUS() {
      super(mau, null);
    }

    @Override
    public void storeActualHashDuration(long elapsed, Exception err) {
      if (err instanceof HashService.SetEstimate) {
	recordEvent("SetActual " + elapsed);
      } else {
	recordEvent("StoreActual " + elapsed);
      }
    }
  }

  void recordEvent(String evt) {
    Event e = new Event(evt, TimeBase.nowMs());
    log.info("Event: " + e);
    events.add(e);
    semEvtDone.give();
  }

  Event lastEvent() {
    if (events.isEmpty()) return null;
    return events.get(events.size() - 1);
  }

  boolean waitEvent(Event wait) {
    while (!wait.equals(lastEvent())) {
      semEvtStart.give();
      if (!semEvtDone.take(TIMEOUT_SHOULDNT)) {
	return false;
      }
    }
    return true;
  }

  int countEvents(Event e) {
    int res = 0;
    for (Event evt : events) {
      if (evt.equals(e)) {
	res++;
      }
    }
    return res;
  }

  class Event {
    String evt;
    long time;
    Event(String evt) {
      this(evt, -1);
    }
    Event(String evt, long time) {
      this.evt = evt;
      this.time = time;
    }
    public boolean equals(Object o) {
      if (!(o instanceof Event)) {
	return false;
      }
      Event e = (Event)o;
      return evt.equals(e.evt) &&
	(time == -1 || e.time == -1 || time == e.time);
    }
    public String toString() {
      return "[" + evt + " at " + time + "]";
    }
  }

}
