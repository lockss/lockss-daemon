/*
 * $Id: TestMBF1.java,v 1.2 2003-07-23 02:58:20 dshr Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.mbf;

import java.io.*;
import java.security.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;

/** JUnitTest case for class: org.lockss.mbf.MBF1 */
public class TestMBF1 extends LockssTestCase {
  private static Logger log = null;
  private static Random rand = null;
  private static File f = null;
  private static byte[] basis;

  protected void setUp() throws Exception {
    super.setUp();
    log = Logger.getLogger("TestMBF1");
    rand = new Random(100);
    f = FileUtil.tempFile("mbf1test");
    FileOutputStream fis = new FileOutputStream(f);
    basis = new byte[512];
    rand.nextBytes(basis);
    fis.write(basis);
    fis.close();
    if (f.exists()) {
      log.debug(f.getPath() + " bytes " + f.length() + " long");
    } else
      log.debug2(f.getPath() + " doesn't exist");
    MemoryBoundFunction.configure(f);
  }

  /** tearDown method for test case
   * @throws Exception if XXX
   */
  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test one generate/verify pair
   */
  public void testOnce() throws IOException {
    onePair(4, 32);
  }

  /*
   * Test a series of generate/verify pairs
   */
  public void testMultiple() throws IOException {
    for (int i = 0; i < 64; i++) {
      onePair(3, 64);
    }
  }

  /**
   * Timing test.  Verify that the MBF requirements are met, i.e:
   * * On average generate takes longer than verify.
   * * Increasing l increases the cost of both generate and verify
   * * Increasing e increases the factor by which generate is more
   *   costly than verify.
   */
  public void testTimingOne() throws IOException {
    byte[] nonce = new byte[24];
    int e;
    int l;
    long proof;
    int numTries = 10;
    long totalGenerateTime;
    long totalVerifyTime;

    // Generate time > Verify time
    e = 4;
    l = 32;
    totalGenerateTime = totalVerifyTime = 0;
    for (int i = 0; i < numTries; i++) {
      rand.nextBytes(nonce);
      long startTime = System.currentTimeMillis();
      proof = generate(nonce, e, l, l);
      long endTime = System.currentTimeMillis();
      totalGenerateTime += (endTime - startTime);
      startTime = endTime;
      verify(nonce, e, l, proof, l);
      endTime = System.currentTimeMillis();
      totalVerifyTime += (endTime - startTime);
    }
    log.info("generate " + totalGenerateTime + " msec verify " +
	       totalVerifyTime + " msec");
    assertTrue(totalGenerateTime > totalVerifyTime);
  }    

  public void testTimingTwo() throws IOException {
    byte[] nonce = new byte[24];
    int e = 7;
    int[] l = { 16, 64, 256 };
    long proof;
    int numTries = 20;
    long[] totalGenerateTime = new long[l.length];
    long[] totalVerifyTime = new long[l.length];

    // Increasing l increases cost
    for (int j = 0; j < l.length; j++) {
      totalGenerateTime[j] = totalVerifyTime[j] = 0;
      for (int i = 0; i < numTries; i++) {
	rand.nextBytes(nonce);
	long startTime = System.currentTimeMillis();
	proof = generate(nonce, e, l[j], l[j]);
	long endTime = System.currentTimeMillis();
	totalGenerateTime[j] += (endTime - startTime);
	startTime = endTime;
	verify(nonce, e, l[j], proof, l[j]);
	endTime = System.currentTimeMillis();
	totalVerifyTime[j] += (endTime - startTime);
	log.info("generate l " + l[j] + " " + totalGenerateTime[j] +
		 " msec verify " + totalVerifyTime[j] + " msec");
      }
      if (j > 0) {
	assertTrue(totalGenerateTime[j] > totalGenerateTime[j-1]);
	assertTrue(totalVerifyTime[j] > totalVerifyTime[j-1]);
      }
    }
  }

  public void testTimingThree() throws IOException {
    byte[] nonce = new byte[24];
    int[] e = { 3, 7, 15 };
    int l = 32;
    long proof;
    int numTries = 10;
    long[] totalGenerateTime = new long[e.length];
    long[] totalVerifyTime = new long[e.length];

    // Increasing e increases cost & factor generate/verify
    for (int j = 0; j < e.length; j++) {
      totalGenerateTime[j] = totalVerifyTime[j] = 0;
      for (int i = 0; i < numTries; i++) {
	rand.nextBytes(nonce);
	long startTime = System.currentTimeMillis();
	proof = generate(nonce, e[j], l, 2*l);
	long endTime = System.currentTimeMillis();
	totalGenerateTime[j] += (endTime - startTime);
	startTime = endTime;
	verify(nonce, e[j], l, proof, 2*l);
	endTime = System.currentTimeMillis();
	totalVerifyTime[j] += (endTime - startTime);
	log.info("generate e " + e[j] + " " + totalGenerateTime[j] +
		 " msec verify " + totalVerifyTime[j] + " msec");
      }
      if (j > 0) {
	log.info("e " + e[j-1] + " times " +
		 totalGenerateTime[j-1] + "/" + totalVerifyTime[j-1] +
		 " e " + e[j] + " times " +
		 totalGenerateTime[j] + "/" + totalVerifyTime[j]);
	// Increasing e increases generation cost
	assertTrue(totalGenerateTime[j] > totalGenerateTime[j-1]);
	// Increasing e increases factor by which generate costs more
	// than verify
	float oldFactor = ((float) totalGenerateTime[j-1]) /
	  ((float) totalVerifyTime[j-1]);
	float newFactor = ((float) totalGenerateTime[j]) /
	  ((float) totalVerifyTime[j]);
	assertTrue(newFactor > oldFactor);
      }
    }
  }    

  public void testVerifyRandom() throws IOException {
    byte[] nonce = new byte[24];
    int e = 7;
    int l = 32;
    long proof;
    int numTries = 128;
    int numYes = 0;
    int numNo = 0;

    // Verifying random proofs has about 1/(2**e) chance of success

    for (int i = 0; i < numTries; i++) {
      rand.nextBytes(nonce);
      if (verify(nonce, e, l, i, 64) == 0)
	numYes++;
      else
	numNo++;
    }
    assertTrue(numYes > 0);
    int factor = (numYes + numNo) / numYes;
    log.info("random verify yes " + numYes + " no " + numNo +
	     " factor " + factor);
    assertTrue(factor > 6 && factor < 10);
  }    

  private long generate(byte[] nonce, int e, int l, int steps)
    throws IOException {
    log.debug2("about to create mbf1");
    MemoryBoundFunction mbf = new MBF1(nonce, e, l);
    log.debug2("created");
    while (mbf.computeSteps(steps)) {
      log.debug2("step");
      assertEquals(mbf.result(), -1);
    }
    long res = mbf.result();
    log.info("generate " + res);
    assertTrue(res >= 0 && res < basis.length);
    return (res);
  }

  private long verify(byte[] nonce, int e, int l, long proof, int steps)
    throws IOException {
    log.debug2("about to create mbf2");
    MemoryBoundFunction mbf2 = new MBF1(nonce, e, l, proof);
    log.debug2("created");
    while (mbf2.computeSteps(steps)) {
      log.debug2("step");
      assertEquals(mbf2.result(), -1);
    }
    long res2 = mbf2.result();
    log.info("verify " + proof + " returns " + res2);
    return res2;
  }

  /**
   * Functional test of generate/verify pair
   */
  private void onePair(int e, int l) throws IOException {
    byte[] nonce = new byte[64];
    rand.nextBytes(nonce);
    long proof = generate(nonce, e, l, 8);
    long ret = verify(nonce, e, l, proof, 8);
    assertTrue(ret == 0);
    if (false) {
      // XXX we'd like to be able to say this but we can't
      ret = verify(nonce, e, l, proof + 1, 8);
      assertTrue(ret > 0);
    }
  }


}
