/*
 * $Id: TestHashQueue.java,v 1.3 2002-10-01 06:16:53 tal Exp $
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

package org.lockss.hasher;

import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.hasher.HashQueue</code>
 */

public class TestHashQueue extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.hasher.HashQueue.class
  };

  static Logger log = Logger.getLogger("HashQueue");
  static HashServiceTestPlugin.CUS cus = HashServiceTestPlugin.getCUS();
  static final String hashAlgorithm = "SHA-1";
  static MessageDigest dig;

  public TestHashQueue(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    log.setLevel(Logger.LEVEL_DEBUG);
    if (dig == null) {
      dig = MessageDigest.getInstance(hashAlgorithm);
    }
  }

//  	HashQueue.Request(CachedUrlSet urlset,
//  			  MessageDigest hasher,
//  			  Deadline deadline,
//  			  HashService.Callback callback,
//  			  Object cookie,
//  			  CachedUrlSetHasher urlsetHasher,
//  			  long estimatedDuration);

  static HashQueue.Request simpleReq(long deadline, int duration) {
    return new HashQueue.Request(cus, dig, new Deadline(deadline),
				 null, null, null, duration);
  }

  static HashQueue.Request req(long deadline,
			       int duration,
			       int bytes,
			       HashService.Callback callback) {
    cus.setHashDuration(duration, bytes);
    HashQueue.Request req =
      new HashQueue.Request(cus, dig, new Deadline(deadline),
			    callback, null,
			    cus.getContentHasher(dig),
			    duration);
    req.cookie = req;
    return req;
  }

  static HashQueue.Request req(String cookie,
			       long deadline,
			       int duration,
			       int bytes,
			       HashService.Callback callback) {
    cus.setHashDuration(duration, bytes);
    HashQueue.Request req =
      new HashQueue.Request(cus, dig, new Deadline(deadline),
			    callback, cookie,
			    cus.getContentHasher(dig),
			    duration);
    return req;
  }

  public void testReq() {
    HashQueue q = new HashQueue();
    HashQueue.Request req1 = simpleReq(1000, 100);
    HashQueue.Request req2 = simpleReq(2000, 100);
    assertTrue(req1.runBefore(req2));
    assertTrue(!req2.runBefore(req1));
  }

  // test request acceptance
  public void testAcc() {
    HashQueue q = new HashQueue();
    HashQueue.Request req1 = simpleReq(-1, 100);
    HashQueue.Request req2 = simpleReq(2000, 1000);
    HashQueue.Request req3 = simpleReq(2000, 1100);
    assertEquals(null, q.head());
    assertTrue(!q.insert(req1));
    assertEquals(null, q.head());
    assertTrue(q.insert(req2));
    assertTrue(null != q.head());
    assertTrue(!q.insert(req3));

  }

  // test insert order
  public void testOrder() throws Exception {
    HashQueue q = new HashQueue();
    HashQueue.Request r1, r2, r3 ,r4, r5;
    r1 = simpleReq(2000, 0);
    r2 = simpleReq(3000, 0);
    r3 = simpleReq(5000, 0);
    r4 = simpleReq(2500, 0);
    r5 = simpleReq(200, 0);
    Object ord[] = {r5, r1, r4, r2, r3};
    assertTrue(q.insert(r1));
    assertTrue(q.insert(r2));
    assertTrue(q.insert(r3));
    assertTrue(q.insert(r4));
    assertTrue(q.insert(r5));
    assertIsomorphic(ord, (Collection)PrivilegedAccessor.getValue(q, "qlist"));
  }

  // test completion & callback
  public void testDone() throws Exception {
    HashQueue q = new HashQueue();
    final List doneList = new LinkedList();
    HashService.Callback cb = new HashService.Callback() {
	public void hashingFinished(CachedUrlSet urlset,
				    Object cookie,
				    MessageDigest hasher,
				    Exception e) {
	  doneList.add(cookie);
	}
      };
    HashQueue.Request r1, r2, r3 ,r4, r5;
    r1 = req(2000, 0, 100, cb);
    r2 = req(10000, 0, 200, cb);
    r3 = req(20000, 0, 0, cb);
    assertTrue(q.insert(r1));
    assertTrue(q.insert(r2));
    assertEquals(0, doneList.size());
    q.removeCompleted();
    assertEquals(0, doneList.size());
    r1.deadline = new Deadline(-1);
    q.removeCompleted();
    Object exp[] = {r1};
    assertIsomorphic(exp, doneList);
    r2.deadline = new Deadline(-1);
    assertTrue(q.insert(r3));
    q.removeCompleted();
    Object exp2[] = {r1, r2, r3};
    assertIsomorphic(exp2, doneList);
  }

  // test stepper
  /* needs to be finished
  public void testStep() throws Exception {
    HashQueue q = new HashQueue();
    final List doneList = new LinkedList();
    HashService.Callback cb = new HashService.Callback() {
	public void hashingFinished(CachedUrlSet urlset,
				    Object cookie,
				    MessageDigest hasher,
				    Exception e) {
	  doneList.add(cookie);
	  doneList.add(e);
	}
      };
    HashQueue.Request r1, r2, r3 ,r4, r5;
    r1 = req("1", 20000, 10000, 100, cb);
    r2 = req("2", 100000, 20000, 200, cb);
    r3 = req("3", 200000, 30000, 0, cb);
    assertTrue(q.insert(r1));
    assertTrue(q.insert(r2));
    assertTrue(q.insert(r3));
    q.runAndNotify(3, 75, Boolean.TRUE);
    q.runAndNotify(3, 75, Boolean.TRUE);
    q.runAndNotify(3, 75, Boolean.TRUE);
    q.runAndNotify(3, 75, Boolean.TRUE);
    q.runAndNotify(3, 75, Boolean.TRUE);
    q.runAndNotify(1, 75, Boolean.TRUE);
    q.runAndNotify(1, 75, Boolean.TRUE);
  }
  */
}
