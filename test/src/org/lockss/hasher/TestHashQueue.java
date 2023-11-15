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


/**
 * Test class for <code>org.lockss.hasher.HashQueue</code>
 */

public class TestHashQueue extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.hasher.HashQueue.class
  };

  static Logger log = Logger.getLogger("HashQueue");
//    static HashServiceTestPlugin.CUS cus = HashServiceTestPlugin.getCUS();
  MockArchivalUnit mau = null;
  MockCachedUrlSet cus;
  static final String hashAlgorithm = "SHA-1";
  static MessageDigest dig;

  public TestHashQueue(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    log.setLevel(Logger.LEVEL_DEBUG);
    if (dig == null) {
      dig = MessageDigest.getInstance(hashAlgorithm);
    }
    mau = new MockArchivalUnit(new MockPlugin());
    cus = new MockCachedUrlSet(mau, null);
    cus.setHashItSource(Collections.EMPTY_LIST);
    TimeBase.setSimulated();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  HashQueue.Request simpleReq(long deadlineIn, int duration) {
    return new HashQueue.Request(cus, Deadline.in(deadlineIn),
				 null, null,
				 new GenericContentHasher(cus, dig),
				 duration);
  }

  HashQueue.Request req(long deadlineIn,
			       int duration,
			       int bytes,
			       HashService.Callback callback) {
    HashQueue.Request req = req(null, deadlineIn, duration, bytes, callback);
    req.cookie = req;
    return req;
  }

  HashQueue.Request req(Object cookie,
			long deadlineIn,
			int duration,
			int bytes,
			HashService.Callback callback) {
    MockCachedUrlSetHasher hasher = new MockCachedUrlSetHasher();
    hasher.setNumBytes(bytes);
    cus.setContentHasher(hasher);
//      cus.setHashDuration(duration, bytes);
    HashQueue.Request req =
      new HashQueue.Request(cus, Deadline.in(deadlineIn),
			    callback, cookie,
			    hasher,
			    duration);
    return req;
  }

  public void testReqOrder() {
    HashQueue q = new HashQueue();
    HashQueue.Request req1 = simpleReq(1000, 100);
    HashQueue.Request req2 = simpleReq(2000, 100);
    HashQueue.Request req3 = simpleReq(2000, 100);
    assertTrue(req1.runBefore(req2));
    assertFalse(req2.runBefore(req1));
    // 2 and 3 expire at the same time, so runBefore should be
    // false in both directions
    assertFalse(req2.runBefore(req3));
    assertFalse(req3.runBefore(req2));
  }

  public void testOverrunReqOrder() {
    HashQueue q = new HashQueue();
    HashQueue.Request req1 = simpleReq(1000, 100);
    HashQueue.Request req2 = simpleReq(2000, 100);
    HashQueue.Request reqO1 = simpleReq(500, 100);
    reqO1.timeUsed = 600;
    HashQueue.Request reqO2 = simpleReq(1500, 100);
    reqO2.timeUsed = 1600;
    assertTrue(reqO1.runBefore(reqO2));
    assertFalse(reqO2.runBefore(reqO1));
    assertTrue(req1.runBefore(reqO1));
    assertTrue(req2.runBefore(reqO1));
    assertTrue(req1.runBefore(reqO2));
    assertTrue(req2.runBefore(reqO2));
  }

  // test request acceptance
  public void testAccept() {
    HashQueue q = new HashQueue();
    HashQueue.Request r1, r2, r3 ,r4, r5, r6;
    r1 = simpleReq(-1, 100);
    r2 = simpleReq(2000, 1000);
    r3 = simpleReq(3000, 2900);
    assertEquals(null, q.head());
    assertFalse(q.insert(r1));
    assertTrue(q.insert(r2));
    assertFalse(q.insert(r3));
    // change r2 to overrun
    r2.timeUsed = 1200;
    // r3 should now be accepted.  It would prevent r2 from finishing in
    // time, but sr2 should be ignored as it has overrun.
    assertTrue(q.insert(r3));
  }

  // test insert order
  public void testInsertOrder() throws Exception {
    HashQueue q = new HashQueue();
    HashQueue.Request r1, r2, r3, r4, r5, r6, r7;
    r1 = simpleReq(2000, 0);
    r2 = simpleReq(3000, 0);
    r3 = simpleReq(5000, 0);
    r4 = simpleReq(2500, 0);
    r5 = simpleReq(200, 0);
    r6 = simpleReq(200, 0);		// identical to r5, inserted before it
					// so should go before it in queue
    // One that has overrun, should end up last
    r7 = simpleReq(200, 0);
    r7.timeUsed = 201;
    Object ord[] = {r6, r5, r1, r4, r2, r3, r7};
    assertTrue(q.insert(r1));
    assertTrue(q.insert(r2));
    assertTrue(q.insert(r3));
    assertTrue(q.insert(r6));
    assertTrue(q.insert(r4));
    assertTrue(q.insert(r5));
    assertTrue(q.insert(r7));
    assertIsomorphic(ord, (Collection)PrivilegedAccessor.getValue(q, "qlist"));
  }

  // test completion & callback
  public void testDone() throws Exception {
    HashQueue q = new HashQueue();
    final List cookieList = new LinkedList();
    final List eList = new LinkedList();
    HashService.Callback cb = new HashService.Callback() {
	public void hashingFinished(CachedUrlSet urlset,
				    long timeUsed,
				    Object cookie,
				    CachedUrlSetHasher hasher,
				    Exception e) {
	  cookieList.add(cookie);
	  eList.add(e);
	}
      };
    HashQueue.Request r1, r2, r3 ,r4, r5;
    r1 = req(2000, 0, 100, cb);
    r2 = req(10000, 0, 200, cb);
    r3 = req(20000, 0, 0, cb);
    r4 = req(50000, 0, 1, cb);
    assertTrue(q.insert(r1));
    assertTrue(q.insert(r2));
    assertTrue(q.insert(r4));
    assertEquals(0, cookieList.size());
    q.removeCompleted();
    assertEquals(0, cookieList.size());
    // make r1 timeout
    r1.deadline.expire();
    q.removeCompleted();
    List exp = ListUtil.list(r1);
    assertEquals(exp, cookieList);
    assertEquals(exp, q.getCompletedSnapshot());
    // make r2 timeout
    TimeBase.step(11000);
    // r3 is finished
    assertTrue(q.insert(r3));
    Exception r4e = new Exception();
    // make r4 error
    r4.e = r4e;
    q.removeCompleted();
    // check that they all finished, and in the right order
    Object exp2[] = {r1, r2, r3, r4};
    assertIsomorphic(exp2, cookieList);
    assertIsomorphic(exp2, q.getCompletedSnapshot());
    // check their exceptions
    assertTrue(eList.get(0) instanceof HashService.Timeout);
    assertTrue(eList.get(1) instanceof HashService.Timeout);
    assertSame(null, eList.get(2));
    assertSame(r4e, eList.get(3));
  }

  // test stepper
  public void testStep() throws Exception {
    HashQueue q = new HashQueue();
    final List cookieList = new LinkedList();
    HashService.Callback cb = new HashService.Callback() {
	public void hashingFinished(CachedUrlSet urlset,
				    long timeUsed,
				    Object cookie,
				    CachedUrlSetHasher hasher,
				    Exception e) {
	  cookieList.add(cookie);
	}
      };
    HashQueue.Request r1, r2, r3 ,r4, r5;
    r1 = req("1", 20000, 10000, 10000, cb);
    r2 = req("2", 100000, 20000, 20000, cb);
    r3 = req("3", 200000, 30000, 40000, cb);
    assertTrue(q.insert(r2));
    q.runAndNotify(3, 75, Boolean.TRUE);
    long n2 = 20000-3*75;
    assertEquals(n2, getBytesLeft(r2));
    assertTrue(q.insert(r1));
    assertTrue(q.insert(r3));
    q.runAndNotify(3, 75, Boolean.TRUE);
//      assertEquals(n2, getBytesLeft(r2));
//      assertEquals(n2, getBytesLeft(r1));
  }

  public void testGetAvailableHashTimeBefore() {
    HashQueue q = new HashQueue();
    assertEquals(500, q.getAvailableHashTimeBefore(Deadline.in(500)));
    HashQueue.Request r1, r2, r3, r4, r5, r6, r7;
    r1 = simpleReq(200, 100);
    r2 = simpleReq(2000, 1200);
    r3 = simpleReq(3000, 500);
    assertTrue(q.insert(r1));
    assertTrue(q.insert(r2));
    assertTrue(q.insert(r3));
    assertEquals(100, q.getAvailableHashTimeBefore(Deadline.in(100)));
    assertEquals(400, q.getAvailableHashTimeBefore(Deadline.in(500)));
    assertEquals(700, q.getAvailableHashTimeBefore(Deadline.in(1000)));
    assertEquals(700, q.getAvailableHashTimeBefore(Deadline.in(2000)));
    assertEquals(1200, q.getAvailableHashTimeBefore(Deadline.in(3000)));
    assertEquals(2200, q.getAvailableHashTimeBefore(Deadline.in(4000)));
    // this will fully commit first 200 ms
    r4 = simpleReq(200, 100);
    assertTrue(q.insert(r4));
    assertEquals(0, q.getAvailableHashTimeBefore(Deadline.in(100)));
    assertEquals(0, q.getAvailableHashTimeBefore(Deadline.in(0)));
  }

  private long getBytesLeft(HashQueue.Request req) {
    MockCachedUrlSetHasher hasher = (MockCachedUrlSetHasher)req.urlsetHasher;
    return hasher.getBytesLeft();
  }
}
