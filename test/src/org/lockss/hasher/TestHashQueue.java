/*
 * $Id: TestHashQueue.java,v 1.1 2002-09-19 20:54:12 tal Exp $
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

  static CachedUrlSet cus = HashServiceTestPlugin.getCUS();
  static final String hashAlgorithm = "SHA-1";
  static MessageDigest dig;

  public TestHashQueue(String msg) {
    super(msg);
  }

  public void setUp() {
    if (dig == null) {
      try {
	dig = MessageDigest.getInstance(hashAlgorithm);
      } catch (java.security.NoSuchAlgorithmException e) {
	fail("Couldn't get MessageDigest: " + e);
      }
    }
  }

  static HashQueue.Request simpleReq(long deadline, int duration) {
    return new HashQueue.Request(cus, dig, new Deadline(deadline),
				 null, null, null, duration);
  }
  public void testReq() {
    HashQueue q = new HashQueue();
    HashQueue.Request req1 = simpleReq(1000, 100);
    HashQueue.Request req2 = simpleReq(2000, 100);
    assertTrue(q.runBefore(req1, req2));
    assertTrue(!q.runBefore(req2, req1));
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
  public void testOrder()
      throws IllegalAccessException, NoSuchFieldException {
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
    assertIso(ord, (Collection)PrivilegedAccessor.getValue(q, "qlist"));
  }
//  	HashQueue.Request(CachedUrlSet urlset,
//  			  MessageDigest hasher,
//  			  Deadline deadline,
//  			  HashService.Callback callback,
//  			  Object cookie,
//  			  CachedUrlSetHasher urlsetHasher,
//  			  long estimatedDuration);

}
