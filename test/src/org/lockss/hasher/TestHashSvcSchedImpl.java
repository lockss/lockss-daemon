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
 * Test class for org.lockss.hasher.HashSvcQueueImpl
 */

public class TestHashSvcSchedImpl extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestHashSvcSchedImpl");

  protected MockLockssDaemon theDaemon;

  private HashService svc;
  private MockArchivalUnit au;
  MockCachedUrlSet cus;
  static final String hashAlgorithm = "SHA-1";
  static MessageDigest dig;
  List work;

  public TestHashSvcSchedImpl(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    SchedService schedSvc = this.theDaemon.getSchedService();
    svc = new HashSvcSchedImpl();
    theDaemon.setHashService(svc);
    svc.initService(theDaemon);
    schedSvc.startService();
    svc.startService();
    if (dig == null) {
      dig = MessageDigest.getInstance(hashAlgorithm);
    }
    au = new MockArchivalUnit();
    cus = new MockCachedUrlSet();
    cus.setArchivalUnit(au);
    work = new ArrayList();
  }

  public void tearDown() throws Exception {
    svc.stopService();
    super.tearDown();
  }

  boolean hashContent(String cookie, int duration, int eachStepTime,
		      long deadInOrAt, HashService.Callback cb) {
    CachedUrlSetHasher hasher = new MyMockCUSH(cus);
    //    hasher.setNumBytes(bytes);
    cus.setContentHasher(hasher);
    cus.setEstimatedHashDuration(duration);
    Deadline deadline =
      (TimeBase.isSimulated()
       ? Deadline.at(deadInOrAt) : Deadline.in(deadInOrAt));
    return svc.scheduleHash(cus.getContentHasher(dig),
			    deadline, cb, null);
  }

  public void testCancel() throws Exception {
    TimeBase.setSimulated();
    assertTrue(hashContent("1", 300, -100, 500, null));
    assertFalse(svc.isIdle());
    svc.cancelAuHashes(new MockArchivalUnit());
    assertFalse(svc.isIdle());
    svc.cancelAuHashes(au);
    assertTrue(svc.isIdle());
  }

  public void testCallback() throws Exception {
    TimeBase.setSimulated();
    final SimpleQueue q = new SimpleQueue.Fifo();

    HashService.Callback cb = new HashService.Callback() {
	public void hashingFinished(CachedUrlSet urlset,
				    long timeUsed,
				    Object cookie,
				    CachedUrlSetHasher hasher,
				    Exception e) {
	  q.put(e);
	}
      };

    assertTrue(hashContent("1", 300, -100, 500, cb));
    assertFalse(svc.isIdle());
    TimeBase.step(10000);
    Exception cbex = (Exception)q.get(TIMEOUT_SHOULDNT);
    assertClass(SchedService.Timeout.class, cbex);
  }

  public class MyMockCUSH extends MockCachedUrlSetHasher {
    CachedUrlSet cus;

    MyMockCUSH(CachedUrlSet cus) {
      super();
      this.cus = cus;
    }

    public CachedUrlSet getCachedUrlSet() {
      return cus;
    }

    public long getEstimatedHashDuration() {
      return cus.estimatedHashDuration();
    }

    public int hashStep(int numBytes) {
      // do nothing
      return 0;
    }
    public boolean finished() {
      return false;
    }
  }

}
