/*
 * $Id: FuncHashService.java,v 1.4 2004-01-12 06:24:27 tlipkis Exp $
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

package org.lockss.hasher;

import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import junit.framework.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.scheduler.*;


/**
 * Test class for org.lockss.hasher.HashService
 */

public class FuncHashService extends LockssTestCase {
  private static Logger log = Logger.getLogger("FuncHashSvc");

  public static Class testedClasses[] = {
    org.lockss.scheduler.SchedService.class,
  };

  protected MockLockssDaemon theDaemon;

  private HashService svc;
  MockCUS cus;
  static final String hashAlgorithm = "SHA-1";
  static MessageDigest dig;
  List work;

  public FuncHashService(String name) {
    super(name);
  }

  public void setUp(String hashSvcName) throws Exception {
    super.setUp();
    theDaemon = new MockLockssDaemon();
    ConfigurationUtil.setFromArgs("org.lockss.manager.HashService",
				  hashSvcName);
    svc = theDaemon.getHashService();
    svc.startService();
    if (dig == null) {
      dig = MessageDigest.getInstance(hashAlgorithm);
    }
    cus = new MockCUS();
    work = new ArrayList();
//     TimeBase.setSimulated();
  }

  public void tearDown() throws Exception {
    svc.stopService();
    super.tearDown();
  }

  boolean hashContent(String cookie, int duration, int eachStepTime,
		      long deadInOrAt, HashService.Callback cb) {
    MockCUSH hasher = new MockCUSH(duration, eachStepTime, cookie);
    //    hasher.setNumBytes(bytes);
    cus.setContentHasher(hasher);
    cus.setEstimatedHashDuration(duration);
    Deadline deadline =
      (TimeBase.isSimulated()
       ? Deadline.at(deadInOrAt) : Deadline.in(deadInOrAt));
    return svc.hashContent(cus, dig, deadline, cb, null);
  }

  void waitUntilDone() throws Exception {
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT);
      while (!svc.isIdle()) {
	TimerUtil.guaranteedSleep(100);
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("Hasher timed out");
      }
    }
  }    

  
  HashService.Callback hashCB() {
    return new HashService.Callback() {
	public void hashingFinished(CachedUrlSet urlset,
				    Object cookie,
				    MessageDigest hasher,
				    Exception e) {
	  log.debug("Hashing finished: " + cookie);
	  //	  cookieList.add(cookie);

	}
      };
  }

  public void testCanHashBeScheduled() throws Exception {
    assertFalse(svc.canHashBeScheduledBefore(10000, Deadline.in(2000)));
    assertTrue(svc.canHashBeScheduledBefore(10000, Deadline.in(20000)));
  }

  // This fails on slow machines.  Simulated version is good enough test.
//   public void testRealTimeStep() throws Exception {
//     log.info("testOneStepWorkl");
//     assertTrue(hashContent("1", 300, 100, 10 * Constants.SECOND, hashCB()));
//     waitUntilDone();
//     assertEquals(ListUtil.list(new Work("1", stepBytes(), 1000),
// 			       new Work("1", stepBytes(), 1000),
// 			       new Work("1", stepBytes(), 1000)),
// 		 work);
//   }

  public void testSimulatedTimeStep() throws Exception {
    TimeBase.setSimulated();
    hashContent("1", 300, -100, 500, hashCB());
    waitUntilDone();
    assertEquals(ListUtil.list(new Work(0, "1", stepBytes(), 1000),
			       new Work(100, "1", stepBytes(), 1000),
			       new Work(200, "1", stepBytes(), 1000)),
		 work);
    assertEquals(300, cus.actualHashDuration);
    assertNull(cus.actualHashException);
  }

  int stepBytes() {
    return ConfigManager.getIntParam(HashService.PARAM_STEP_BYTES,
				     HashService.DEFAULT_STEP_BYTES);
  }


  // hash step work record
  class Work {
    Deadline when;
    String cookie;
    int numBytes;
    int eachStepBytes;
    Work(Deadline when, String cookie,
	 int numBytes, int eachStepBytes) {
      this.when = when;
      this.cookie = cookie;
      this.numBytes = numBytes;
      this.eachStepBytes = eachStepBytes;
    }

    Work(long when, String cookie,
	 int numBytes, int eachStepBytes) {
      this(Deadline.at(when), cookie, numBytes, eachStepBytes);
    }

    Work(String cookie, int numBytes, int eachStepBytes) {
      this(Deadline.EXPIRED, cookie, numBytes, eachStepBytes);
    }

    public boolean equals(Object obj) {
      if (obj instanceof Work) {
	Work o = (Work)obj;
	return (!TimeBase.isSimulated() || when.equals(o.when)) &&
	  cookie.equals(o.cookie) &&
	  numBytes == o.numBytes &&
	  eachStepBytes == o.eachStepBytes;
      }
      return false;
    }
    public String toString() {
      return "[Work: \"" + cookie + "\", n=" + numBytes + ", e = " +
	eachStepBytes + " , " + when + "]";
    }
  }

  void recordWork(Deadline when, String cookie,
		  int numBytes, int eachStepBytes) {
    work.add(new Work(when, cookie, numBytes, eachStepBytes));
  }


  public class MockCUS extends MockCachedUrlSet {
    long actualHashDuration;
    Exception actualHashException;

    public MockCUS() {
      super(null, null);
    }

    public void storeActualHashDuration(long elapsed, Exception err) {
      actualHashDuration = elapsed;
      actualHashException = err;
    }
  }

  public class MockCUSH implements CachedUrlSetHasher {
    int eachStepTime;
    int eachStepBytes = 1000;
    String cookie;

    Deadline finishedAt;
    Error toThrow;

    /** Make a CachedUrlSetHasher that takes duration ms to finish, with
     * each step taking eachStepTime ms.  If eachStepTime is positive, it
     * waits that long; if negative, it advances simulated time by that
     * amount. */
    public MockCUSH(int duration, int eachStepTime, String cookie) {
      this.finishedAt = Deadline.in(duration);
      this.eachStepTime = eachStepTime;
      this.cookie = cookie;
    }

    public boolean finished() {
      return finishedAt.expired();
    }

    public int hashStep(int numBytes) {
      if (toThrow != null) {
	throw toThrow;
      }
      if (finished()) {
	return 0;
      }
      recordWork(Deadline.in(0), cookie, numBytes, eachStepBytes);
      if (eachStepTime > 0) {
	Deadline time = Deadline.in(eachStepTime);
	while (!time.expired()) {
	  try {
	    Thread.sleep(1);
	  }catch (InterruptedException e) {
	    throw new RuntimeException(e.toString());
	  }
	}
      } else {
	TimeBase.step(-eachStepTime);
	try {
	  Thread.sleep(1);
	} catch (InterruptedException e) {
	  throw new RuntimeException(e.toString());
	}
      }
      return eachStepBytes;
    }

    public void throwThis(Error e) {
      toThrow = e;
    }
  }

  // Harness to run all tests above with two different configurations:
  // "Queue" and "Sched".

  public static class Queue extends FuncHashService {
    public Queue(String name) {
      super(name);
    }
    public void setUp() throws Exception {
      setUp("org.lockss.hasher.HashSvcQueueImpl");
    }
  }

  public static class Sched extends FuncHashService {
    public Sched(String name) {
      super(name);
    }
    public void setUp() throws Exception {
      setUp("org.lockss.hasher.HashSvcSchedImpl");
      SchedService svc = this.theDaemon.getSchedService();
      svc.startService();
    }
  }

  public static Test suite() {
    TestSuite refSuite = new TestSuite(FuncHashService.class);
    TestSuite suite = new TestSuite();
    for (Enumeration enum = refSuite.tests(); enum.hasMoreElements(); ) {
      LockssTestCase o = (LockssTestCase)enum.nextElement();
      suite.addTest(new Queue(o.getName()));
      suite.addTest(new Sched(o.getName()));
    }
    return suite;
  }
}
