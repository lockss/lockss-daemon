/*
 * $Id: TestMemoryBoundFunction.java,v 1.14 2006-09-14 01:43:54 dshr Exp $
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

/**
 * JUnitTest case for class: org.lockss.mbf.MemoryBoundFunction and its
 * implementations.
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class TestMemoryBoundFunction extends LockssTestCase {
  private static Logger log = null;
  private static Random rand = null;
  private static byte[] basisT = null;
  private static byte[] basisA0 = null;
  private int pathsTried;
  private static String[] names = {
    "MOCK",
    // "MBF1",
    // "MBF2",
  };
  private static MemoryBoundFunctionFactory[] factory = null;

  /**
   * Set up test case by creating the basis arrays (if necessary).
   * @throws Exception should not happen
   */
  protected void setUp() throws Exception {
    super.setUp();
    log = Logger.getLogger("TestMemoryBoundFunction");
    if (false)
      rand = new Random(100);
    else
      rand = new Random(System.currentTimeMillis());
    if (basisT == null) {
      basisT = new byte[/* 16 * */1024*1024];
      rand.nextBytes(basisT);
      log.info(basisT.length + " bytes of T created");
    }
    if (basisA0 == null) {
      basisA0 = new byte[1024];
      rand.nextBytes(basisA0);
      log.info(basisA0.length + " bytes of A0 created");
    }
  }

  /** tearDown method for test case
   * @throws Exception should not happen
   */
  public void tearDown() throws Exception {
    super.tearDown();
  }

  // XXX test that calling computeSteps() when finished does nothing.
  // XXX test behavior of empty proof
  // XXX separate timing tests etc into Func

  /**
   * Test factory by attempting to create one for a BOGUS implementation.
   */
  public void testBadFactory() {
    boolean gotException = false;
    try {
      MemoryBoundFunctionFactory tmp =
	new MemoryBoundFunctionFactory("BOGUS", null, null);
    } catch (NoSuchAlgorithmException ex) {
      gotException = true;
    } catch (Exception ex) {
      fail("BOGUS threw " + ex.toString());
    }
    if (!gotException)
      fail("BOGUS didn't throw NoSuchAlgorithmException");
  }

  /**
   * Test factory by creating one for each MBF implementation
   * and for each one create a generator and a verifier.
   */
  public void testGoodFactory() {
    boolean gotException = false;
    try {
      byte[] b1 = new byte[4];
      byte[] b2 = new byte[4];
      MemoryBoundFunctionFactory tmp =
	new MemoryBoundFunctionFactory("MOCK", b1, b2);
    } catch (Exception ex) {
      fail("MOCK threw " + ex.toString());
    }
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    for (int i = 0; i < names.length; i++) {
      byte[] bad1 = new byte[4];
      byte[] bad2 = new byte[16];
      gotException = false;
      MemoryBoundFunctionFactory tmpFactory = null;
      try {
	tmpFactory = new MemoryBoundFunctionFactory(names[i], bad1, bad2);
      } catch (Exception ex) {
	fail(names[i] + " factory threw " + ex.toString());
      }
      try {
	MemoryBoundFunction tmpMBF =
	  tmpFactory.makeGenerator(nonce, 7, 256, 2);
      } catch (MemoryBoundFunctionException ex) {
	gotException = true;
      } catch (Exception ex) {
	fail(names[i] + " makeGenerator threw " + ex.toString());
      }
      if (!gotException) {
	fail(names[i] + " makeGenerator did not throw");
      }
    }
    if (factory == null) {
      factory = new MemoryBoundFunctionFactory[names.length];
      for (int i = 0; i < names.length; i++) {
	try {
	  factory[i] = new MemoryBoundFunctionFactory(names[i], basisA0, basisT);
	} catch (Exception ex) {
	  fail(names[i] + " threw " + ex.toString());
	}
      }
    }
    for (int i = 0; i < factory.length; i++) {
      try {
	MemoryBoundFunction tmp =
	  factory[i].makeGenerator(nonce, 7, 256, 2);
	if (tmp == null)
	  fail(names[i] + " (generate) returned null");
      } catch (Exception ex) {
	fail(names[i] + " (generate) threw " + ex.toString());
      }
    }
    for (int i = 0; i < factory.length; i++) {
      int[] good = new int[1];
      good[0] = 1;
      try {
	MemoryBoundFunction tmp =
	  factory[i].makeVerifier(nonce, 7, 256, 2, good, 2048);
	if (tmp == null)
	  fail(names[i] + " (verify) returned null");
      } catch (Exception ex) {
	fail(names[i] + " (verify) threw " + ex.toString());
      }
    }
  }

  /**
   * Test that an exception is thrown if the proof is too long
   */
  public void testLongProof() {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    for (int i = 0; i < factory.length; i++) {
      try {
	int[] bad = new int[12];
	MemoryBoundFunction mbf =
	  factory[i].makeVerifier(nonce, 7, 256, 2, bad, 9);
	fail(names[i] + ": didn't throw exception on too-long proof");
      } catch (MemoryBoundFunctionException ex) {
	// No action intended
      } catch (Exception ex) {
	fail(names[i] + ": threw " + ex.toString());
      }
    }
  }

  /**
   * Test that an exception is thrown for an empty proof unless the
   * implementation is MBF2.
   */
  public void testEmptyProof() {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    for (int i = 0; i < factory.length; i++) {
      try {
	int[] bad = new int[0];
	MemoryBoundFunction mbf =
	  factory[i].makeVerifier(nonce, 7, 256, 2, bad, 9);
	// Empty proofs are OK for MBF2
	if (!names[i].equals("MBF2")) {
	  fail(names[i] + ": didn't throw exception on empty proof");
	}
      } catch (MemoryBoundFunctionException ex) {
	if (names[i].equals("MBF2")) {
	  fail(names[i] + " threw " + ex.toString());
	}
      } catch (Exception ex) {
	fail(names[i] + ": threw " + ex.toString());
      }
    }
  }

  /**
   * Test one generate/verify pair with a good proof and nonce
   */
  public void testGoodProofAndNonce() throws IOException {
    for (int i = 0; i < names.length; i++)
      onePair(i, 63, 2048, 2, true, true);
  }

  /**
   * Test one generate/verify pair with a bad proof and good nonce
   */
  public void testBadProofGoodNonce() throws IOException {
    for (int i = 0; i < names.length; i++)
      onePair(i, 511, 128, 2, true, false);
  }


  /**
   * Test one generate/verify pair with a good proof and a bad nonce
   */
  public void testGoodProofBadNonce() throws IOException {
    for (int i = 0; i < names.length; i++)
      onePair(i, 511, 128, 2, false, true);
  }

  /**
   * Test one generate/verify pair with a bad proof and a bad nonce
   */
  public void testBadProofBadNonce() throws IOException {
    for (int i = 0; i < names.length; i++)
      onePair(i, 511, 128, 2, false, false);
  }


  /*
   * Test a series of generate/verify pairs
   */
  public void dontTestMultiple() throws IOException {
    for (int i = 0; i < names.length; i++)
      for (int j = 0; j < 64; j++) {
	onePair(i, 31, 32, 2, true, true);
      }
  }

  /**
   * Timing test.  Verify that the MBF requirements are met, i.e:
   * * On average generate takes longer than verify.
   * * Increasing l increases the cost of both generate and verify
   * * Increasing e increases the factor by which generate is more
   *   costly than verify.
   */
  public void TimingOne(int index) throws IOException {
    byte[] nonce = new byte[24];
    int e;
    int l;
    int n;
    int[] proof;
    int numTries = 10;
    long totalGenerateTime;
    long totalVerifyTime;

    // Generate time > Verify time
    e = 31;
    l = 2048;
    n = 6;
    totalGenerateTime = totalVerifyTime = 0;
    for (int i = 0; i < numTries; i++) {
      rand.nextBytes(nonce);
      long startTime = System.currentTimeMillis();
      proof = generate(index, nonce, e, l, n, l);
      long endTime = System.currentTimeMillis();
      totalGenerateTime += (endTime - startTime);
      startTime = endTime;
      verify(index, nonce, e, l, n, proof, l);
      endTime = System.currentTimeMillis();
      totalVerifyTime += (endTime - startTime);
    }
    log.info(names[index] + " timing(" + e + "," + l + ") test " +
	     totalGenerateTime + " > " + totalVerifyTime + " msec");
    assertTrue(totalGenerateTime > totalVerifyTime);
  }

  public void TimingTwo(int index) throws IOException {
    byte[] nonce = new byte[24];
    int e = 63;
    int[] l = { 64, 256, 1024, 4096 };
    int n = 6;
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
	proof = generate(index, nonce, e, l[j], n, l[j]);
	long endTime = System.currentTimeMillis();
	totalGenerateTime[j] += (endTime - startTime);
	startTime = endTime;
	verify(index, nonce, e, l[j], n, proof, l[j]);
	endTime = System.currentTimeMillis();
	totalVerifyTime[j] += (endTime - startTime);
	log.debug(names[index] + " generate l " + l[j] + " " + totalGenerateTime[j] +
		 " msec verify " + totalVerifyTime[j] + " msec");
      }
      if (j > 0) {
	log.info(names[index] + " timing(" + e + ",[" + l[j] + "," + l[j-1] + "]) test " +
		 totalGenerateTime[j] + " > " + totalGenerateTime[j-1] + " msec");
	log.info(names[index] + " timing(" + e + ",[" + l[j] + "," + l[j-1] + "]) test " +
		 totalVerifyTime[j] + " > " + totalVerifyTime[j-1] + " msec");
	assertTrue(totalGenerateTime[j] > totalGenerateTime[j-1]);
	if (true) {
	  // We'd like to be able to say this but it seems we can't
	  assertTrue(totalVerifyTime[j] > totalVerifyTime[j-1]);
	}
      }
    }
  }

  public void TimingThree(int index) throws IOException {
    byte[] nonce = new byte[24];
    int[] e = { 3, 15, 63 };
    int l = 64;
    int n = 6;
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
	proof = generate(index, nonce, e[j], l, n, 2*l);
	long endTime = System.currentTimeMillis();
	totalGenerateTime[j] += (endTime - startTime);
	startTime = endTime;
	verify(index, nonce, e[j], l, n, proof, 2*l);
	endTime = System.currentTimeMillis();
	totalVerifyTime[j] += (endTime - startTime);
	log.debug(names[index] + "generate e " + e[j] + " " + totalGenerateTime[j] +
		 " msec verify " + totalVerifyTime[j] + " msec");
      }
      if (j > 0) {
	log.info(names[index] + " timing([" + e[j] + "," + e[j-1] + "]," + l + ") test " +
		 totalGenerateTime[j] + " > " + totalGenerateTime[j-1] + " msec");
	// Increasing e increases generation cost
	assertTrue(totalGenerateTime[j] > totalGenerateTime[j-1]);
	// Increasing e increases factor by which generate costs more
	// than verify
	float oldFactor = ((float) totalGenerateTime[j-1]) /
	  ((float) totalVerifyTime[j-1]);
	float newFactor = ((float) totalGenerateTime[j]) /
	  ((float) totalVerifyTime[j]);
	log.info(names[index] + " timing([" + e[j] + "," + e[j-1] + "]," + l + ") test " +
		 newFactor + " > " + oldFactor);
	assertTrue(newFactor > oldFactor);
      }
    }
  }

  public void VerifyRandom(int index) throws IOException {
    byte[] nonce = new byte[24];
    int e = 7;
    int l = 32;
    int n = 4;
    int[] proof = new int[1];
    int numTries = 2048;
    int numYes = 0;
    int numNo = 0;

    // Verifying random proofs has about 1/(2**e) chance of success

    for (int i = 0; i < numTries; i++) {
      rand.nextBytes(nonce);
      proof[0] = i;
      if (verify(index, nonce, e, l, n, proof, 64))
	numYes++;
      else
	numNo++;
    }
    assertTrue(numYes > 0);
    float factor = ((float)(numYes + numNo)) / ((float)numYes);
    log.info(names[index] + " random verify yes " + numYes + " no " + numNo +
	      " factor " + factor);
    assertTrue(factor > 6.5 && factor < 9.5);
  }

  private int[] generate(int index,
			 byte[] nonce,
			 int e,
			 int l,
			 int n,
			 int steps)
    throws IOException {
    int[] res = null;
    try{
      MemoryBoundFunction mbf =
	factory[index].makeGenerator(nonce, e, l, n);
      pathsTried = 0;
      while (mbf.computeSteps(steps)) {
	assertFalse(mbf.finished());
	pathsTried += steps;
      }
      pathsTried /= l;
      res = mbf.result();
      if (res.length > 0) {
	for (int i = 0; i < res.length; i++) {
	  log.debug(names[index] + "generate [" + i + "] "  + res[i] +
		    " tries " + pathsTried);
	  if (res[i] < 0)
	    fail(names[index] + ": proof < 0");
	  if (res[i] >= basisT.length)
	    fail(names[index] + ": proof " + res[i] + " >= " + basisT.length);
	}
      } else {
	log.debug(names[index] + " generate [" + res.length + "]  tries "
		  + pathsTried);
	res = null;
      }
    } catch (Exception ex) {
      fail(names[index] + " threw " + ex.toString());
    }
    return (res);
  }

  private boolean verify(int index,
			 byte[] nonce,
			 int e,
			 int l,
			 int n,
			 int[] proof,
			 int steps)
    throws IOException {
    boolean ret = false;
    pathsTried = 0;
    if (proof != null) {
      assertTrue(proof.length >= 1);
      try {
	MemoryBoundFunction mbf2 =
	  factory[index].makeVerifier(nonce, e, l, n, proof, e);
	while (mbf2.computeSteps(steps)) {
	  assertFalse(mbf2.finished());
	  pathsTried += steps;
	}
	pathsTried /= l;
	int[] res2 = mbf2.result();
	if (res2 == null)
	  log.debug("verify [" + proof.length + "] fails" + pathsTried + " tries");
	else {
	  ret = true;
	  for (int i = 0; i < res2.length; i++) {
	    log.debug("verify [" + i + "] " + res2[i] + " OK tries " + pathsTried);
	  }
	}
      } catch (Exception ex) {
	fail(names[index] + " threw " + ex.toString());
      }
    }
    return (ret);
  }

  static int[] goodProof = {
    1, 3,
  };

  /**
   * Functional test of generate/verify pair
   */
  private void onePair(int index, int e, int l, int n,
		       boolean nonceOK,
		       boolean proofOK) throws IOException {
    // Make sure its configured
    long startTime = System.currentTimeMillis();
    MockMemoryBoundFunction.setProof(goodProof);
    int[] proof = null;
    byte[] nonce1 = null;
    // Generate a suitable proof
    for (int i = 0; i < 20 && proof == null; i++) {
      nonce1 = new byte[64];
      rand.nextBytes(nonce1);
      MockMemoryBoundFunction.setNonce(nonce1);
      proof = generate(index, nonce1, e, l, n, 8);
      // Null proofs are never suitable
      if (proof != null && !proofOK) {
	boolean butchered = false;
	// We need to convert this into an invalid proof, is this possible?
	if (proof.length == 0) {
	  // Empty proof - convert to {1}
	  proof = new int[1];
	  proof[0] = 1;
	  butchered = true;
	  log.info(names[index] + ": empty proof[0] = 1");
	} else if (proof.length == 1 && proof[0] != 1) {
	  // If x>1 convert {x} to {1}
	  proof[0] = 1;
	  butchered = true;
	  log.info(names[index] + ": proof[0] = 1");
	} else for (int j = 1; j < proof.length && butchered == false; j++) {
	  if (proof[j] > (proof[j-1] + 1)) {
	    proof[j] = proof[j-1] + 1;
	    butchered = true;
	    log.info(names[index] + ": proof[" + j + "] = " + (proof[j-1]+1));
	  }
	}
	if (!butchered) {
	  proof = null;
	}
      }
    }
    assertTrue(proof != null);
    assertTrue(proof.length > 0);
    byte[] nonce2 = new byte[64];
    rand.nextBytes(nonce2);
    if (MessageDigest.isEqual(nonce1, nonce2))
      fail(names[index] + ": nonces match");
    boolean ret = verify(index, (nonceOK ? nonce1 : nonce2), e, l, n, proof, 8);
    if (nonceOK && proofOK && !ret)
      fail(names[index] + ": Valid proof declared invalid");
    if (nonceOK && !proofOK && ret)
      fail(names[index] + ": Invalid proof declared valid");
    if (!nonceOK && proofOK && ret)
      fail(names[index] + ": Invalid nonce declared valid");
    if (!nonceOK && !proofOK && ret)
      fail(names[index] + ": Invalid nonce & proof declared valid");
    log.info(names[index] + " onePair(" + e + "," + l + "," + n + ") took " +
	      (System.currentTimeMillis() - startTime) + " msec");
  }

}
