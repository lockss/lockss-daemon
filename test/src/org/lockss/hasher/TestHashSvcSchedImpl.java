/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.hasher;

import java.util.*;
import java.security.MessageDigest;

import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.scheduler.*;


/**
 * Test class for org.lockss.hasher.HashSvcQueueImpl
 */

public class TestHashSvcSchedImpl extends LockssTestCase {
  private static Logger log = Logger.getLogger(TestHashSvcSchedImpl.class);

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

  public void testPadHashEstimate() throws Exception {
    // Defaults are 10% + 10ms
    assertEquals(1110, svc.padHashEstimate(1000));
    // Change to 50% + 10ms
    ConfigurationUtil.addFromArgs(HashService.PARAM_ESTIMATE_PAD_PERCENT, "50");
    assertEquals(1510, svc.padHashEstimate(1000));
    // Change to 50% + 1 minute
    ConfigurationUtil.addFromArgs(HashService.PARAM_ESTIMATE_PAD_CONSTANT, "1m");
    assertEquals(1500 + Constants.MINUTE, svc.padHashEstimate(1000));
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
