/*
 * $Id: TestMBF1.java,v 1.7 2003-08-04 21:36:05 dshr Exp $
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
  private int pathsTried;

  protected void setUp() throws Exception {
    super.setUp();
    log = Logger.getLogger("TestMBF1");
    if (false)
      rand = new Random(100);
    else 
      rand = new Random(System.currentTimeMillis());
    if (f == null) {
      f = FileUtil.tempFile("mbf1test");
      FileOutputStream fis = new FileOutputStream(f);
      basis = new byte[16*1024*1024 + 1024];
      rand.nextBytes(basis);
      fis.write(basis);
      fis.close();
      log.info(f.getPath() + " bytes " + f.length() + " long created");
    }
    if (!f.exists())
      log.warning(f.getPath() + " doesn't exist");
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
    onePair(31, 2048);
  }

  /*
   * Test a series of generate/verify pairs
   */
  public void dontTestMultiple() throws IOException {
    for (int i = 0; i < 64; i++) {
      onePair(15, 32);
    }
  }

  /**
   * Timing test.  Verify that the MBF requirements are met, i.e:
   * * On average generate takes longer than verify.
   * * Increasing l increases the cost of both generate and verify
   * * Increasing e increases the factor by which generate is more
   *   costly than verify.
   */
  public void dontTestTimingOne() throws IOException {
    byte[] nonce = new byte[24];
    int e;
    int l;
    int[] proof;
    int numTries = 10;
    long totalGenerateTime;
    long totalVerifyTime;

    // Generate time > Verify time
    e = 7;
    l = 128;
    totalGenerateTime = totalVerifyTime = 0;
    for (int i = 0; i < numTries; i++) {
      rand.nextBytes(nonce);
      long startTime = System.currentTimeMillis();
      proof = generate(nonce, e, l, l, 100);
      long endTime = System.currentTimeMillis();
      totalGenerateTime += (endTime - startTime);
      startTime = endTime;
      verify(nonce, e, l, proof, l);
      endTime = System.currentTimeMillis();
      totalVerifyTime += (endTime - startTime);
    }
    log.info("timing(" + e + "," + l + ") test " +
	     totalGenerateTime + " > " + totalVerifyTime + " msec");
    assertTrue(totalGenerateTime > totalVerifyTime);
  }    

  public void dontTestTimingTwo() throws IOException {
    byte[] nonce = new byte[24];
    int e = 63;
    int[] l = { 8, 64, 256 };
    int[] proof;
    int numTries = 20;
    long[] totalGenerateTime = new long[l.length];
    long[] totalVerifyTime = new long[l.length];

    // Increasing l increases cost
    for (int j = 0; j < l.length; j++) {
      totalGenerateTime[j] = totalVerifyTime[j] = 0;
      for (int i = 0; i < numTries; i++) {
	rand.nextBytes(nonce);
	long startTime = System.currentTimeMillis();
	proof = generate(nonce, e, l[j], l[j], 100);
	long endTime = System.currentTimeMillis();
	totalGenerateTime[j] += (endTime - startTime);
	startTime = endTime;
	verify(nonce, e, l[j], proof, l[j]);
	endTime = System.currentTimeMillis();
	totalVerifyTime[j] += (endTime - startTime);
	log.debug("generate l " + l[j] + " " + totalGenerateTime[j] +
		 " msec verify " + totalVerifyTime[j] + " msec");
      }
      if (j > 0) {
	log.info("timing(" + e + ",[" + l[j] + "," + l[j-1] + "]) test " +
		 totalGenerateTime[j] + " > " + totalGenerateTime[j-1] + " msec");
	log.info("timing(" + e + ",[" + l[j] + "," + l[j-1] + "]) test " +
		 totalVerifyTime[j] + " > " + totalVerifyTime[j-1] + " msec");
	assertTrue(totalGenerateTime[j] > totalGenerateTime[j-1]);
	if (false) {
	  // We'd like to be able to say this but it seems we can't
	  assertTrue(totalVerifyTime[j] > totalVerifyTime[j-1]);
	}
      }
    }
  }

  public void dontTestTimingThree() throws IOException {
    byte[] nonce = new byte[24];
    int[] e = { 3, 15, 63 };
    int l = 64;
    int[] proof;
    int numTries = 10;
    long[] totalGenerateTime = new long[e.length];
    long[] totalVerifyTime = new long[e.length];

    // Increasing e increases cost & factor generate/verify
    for (int j = 0; j < e.length; j++) {
      totalGenerateTime[j] = totalVerifyTime[j] = 0;
      for (int i = 0; i < numTries; i++) {
	rand.nextBytes(nonce);
	long startTime = System.currentTimeMillis();
	proof = generate(nonce, e[j], l, 2*l, 100);
	long endTime = System.currentTimeMillis();
	totalGenerateTime[j] += (endTime - startTime);
	startTime = endTime;
	verify(nonce, e[j], l, proof, 2*l);
	endTime = System.currentTimeMillis();
	totalVerifyTime[j] += (endTime - startTime);
	log.debug("generate e " + e[j] + " " + totalGenerateTime[j] +
		 " msec verify " + totalVerifyTime[j] + " msec");
      }
      if (j > 0) {
	log.info("timing([" + e[j] + "," + e[j-1] + "]," + l + ") test " +
		 totalGenerateTime[j] + " > " + totalGenerateTime[j-1] + " msec");
	// Increasing e increases generation cost
	assertTrue(totalGenerateTime[j] > totalGenerateTime[j-1]);
	// Increasing e increases factor by which generate costs more
	// than verify
	float oldFactor = ((float) totalGenerateTime[j-1]) /
	  ((float) totalVerifyTime[j-1]);
	float newFactor = ((float) totalGenerateTime[j]) /
	  ((float) totalVerifyTime[j]);
	log.info("timing([" + e[j] + "," + e[j-1] + "]," + l + ") test " +
		 newFactor + " > " + oldFactor);
	assertTrue(newFactor > oldFactor);
      }
    }
  }    

  public void dontTestVerifyRandom() throws IOException {
    byte[] nonce = new byte[24];
    int e = 7;
    int l = 32;
    int[] proof = new int[1];
    int numTries = 2048;
    int numYes = 0;
    int numNo = 0;

    // Verifying random proofs has about 1/(2**e) chance of success

    for (int i = 0; i < numTries; i++) {
      rand.nextBytes(nonce);
      proof[0] = i;
      if (verify(nonce, e, l, proof, 64))
	numYes++;
      else
	numNo++;
    }
    assertTrue(numYes > 0);
    float factor = ((float)(numYes + numNo)) / ((float)numYes);
    log.info("random verify yes " + numYes + " no " + numNo +
	      " factor " + factor);
    assertTrue(factor > 6.5 && factor < 9.5);
  }

  public void dontTestPathlengthHistogram() throws IOException {
    long[] numberOfProofs = new long[32];
    long[] costOfProofs = new long[numberOfProofs.length];
    for (int i = 0; i < numberOfProofs.length; i++) {
      numberOfProofs[i] = 0;
      costOfProofs[i] = 0;
    }
    int sampleSize = 120; // About 80 s/sample
    byte[] nonce = new byte[24];
    int[] e = {63, 511, 4095, (4096*8-1)};
    int l = 2048;
    int steps = 2048;
    int slop = 100000;
    int tooLong = 0;
    for (int j = 0; j < e.length; j++) {
      for (int i = 0; i < sampleSize; i++) {
	rand.nextBytes(nonce);
	MemoryBoundFunction mbf = new MBF1(nonce, e[j], l);
	int limit = (e[j] * l * slop);
	pathsTried = 0;
	while (mbf.computeSteps(steps)) {
	  limit -= steps;
	  if (limit <= 0) {
	    pathsTried = -1;
	    break;
	  }
	  pathsTried += (steps / l);
	}
	if (pathsTried > 0) {
	  int bin = bin(pathsTried);
	  numberOfProofs[bin]++;
	  costOfProofs[bin] += pathsTried * l;
	} else if (pathsTried < 0)
	  tooLong++;
      }
      log.info("e " + e[j] + " l " + l + " slop " + slop);
      int totalNumber = 0;
      int totalCost = 0;
      for (int i = 0; i < numberOfProofs.length; i++) {
	totalNumber += numberOfProofs[i];
	totalCost += costOfProofs[i];
      }
      int numberBelow = 0;
      int numberAbove = 0;
      int costBelow = 0;
      int costAbove = 0;
      for (int i = 0; i < numberOfProofs.length; i++) {
	if (numberOfProofs[i] > 0)
	  log.info(i + "\t" + numberOfProofs[i] + "\t" + costOfProofs[i] +
		   "\t" + (numberOfProofs[i]*100)/totalNumber +
		   "\t" + (costOfProofs[i]*100)/totalCost);
	numberAbove += numberOfProofs[i];
	costAbove += costOfProofs[i];
      }
      log.info("too long " + tooLong);
      int median = -1;
      int mean = -1;
      for (int i = 0; i < numberOfProofs.length; i++) {
	if (mean < 0 && costBelow > costAbove)
	  mean = i - 1;
	if (median < 0 && numberBelow > numberAbove)
	  median = i - 1;
	numberBelow += numberOfProofs[i];
	numberAbove -= numberOfProofs[i];
	costBelow += costOfProofs[i];
	costAbove -= costOfProofs[i];
      }
      log.info("equal number bin " + mean + " equal cost bin " + median);
      for (int i = 0; i < numberOfProofs.length; i++) {
	numberOfProofs[i] = 0;
	costOfProofs[i] = 0;
      }
    }
  }

  private int bin(int p) {
    int ret = 0;
    if (p < 0)
      p = -p;
    while (p != 1) {
      ret++;
      p >>>= 1;
    }
    return ret;
  }


  private int[] generate(byte[] nonce, int e, int l, int steps, int slop)
    throws IOException {
    MemoryBoundFunction mbf = new MBF1(nonce, e, l);
    int limit = (e * l * slop);
    pathsTried = 0;
    while (mbf.computeSteps(steps)) {
      assertFalse(mbf.finished());
      if (limit <= 0)
	log.info("stepout e " + e + " l " + l + " steps " + steps);
      assertTrue(limit > 0);
      limit -= steps;
      pathsTried += (steps / l);
    }
    int[] res = mbf.result();
    log.debug("generate [" + res.length + "] first "  + res[0]);
    assertTrue(res[0] >= 0 && res[0] < basis.length);
    return (res);
  }

  private boolean verify(byte[] nonce, int e, int l, int[] proof, int steps)
    throws IOException {
    boolean ret = false;
    if (proof != null) {
      MemoryBoundFunction mbf2 = new MBF1(nonce, e, l, proof);
      int limit = (l * 10);
      while (mbf2.computeSteps(steps)) {
	assertFalse(mbf2.finished());
	assertTrue(limit > 0);
	limit -= steps;
      }
      int[] res2 = mbf2.result();
      if (res2 == null)
	log.debug("verify [" + proof.length + "] first " + proof[0] + " fails");
      else {
	ret = true;
	log.debug("verify [" + proof.length + "] first " + proof[0] + " succeeds");
      }
    }      
    return (ret);
  }

  /**
   * Functional test of generate/verify pair
   */
  private void onePair(int e, int l) throws IOException {
    long startTime = System.currentTimeMillis();
    byte[] nonce = new byte[64];
    rand.nextBytes(nonce);
    int[] proof = generate(nonce, e, l, 8, 100);
    boolean ret = verify(nonce, e, l, proof, 8);
    assertTrue(ret);
    if (false) {
      // XXX we'd like to be able to say this but we can't
      proof[0] += 1;
      ret = verify(nonce, e, l, proof, 8);
      assertTrue(ret);
    }
    log.debug("onePair(" + e + "," + l + ") took " +
	     (System.currentTimeMillis() - startTime) + " msec");
  }


}
