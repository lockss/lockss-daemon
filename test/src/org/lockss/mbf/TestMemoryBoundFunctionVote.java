/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.net.UnknownHostException;
import java.io.*;
import java.security.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.app.LockssDaemon;

/**
 * JUnitTest case for class: org.lockss.mbf.MemoryBoundFunctionVote and
 * its implementations.
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class TestMemoryBoundFunctionVote extends LockssTestCase {
  private static Logger log = null;
  private static Random rand = null;
  private static File f = null;
  private static byte[] basis;
  private int pathsTried;
  private static String[] MBFnames = {
    "MOCK",  // NB - must be first
    // "MBF1",
    // "MBF2",
  };
  private static MemoryBoundFunctionFactory[] MBFfactory = null;
  private static String[] MBFVnames = {
    "MOCK",  // NB - must be first
    // "MBFV2",
  };
  private static MemoryBoundFunctionVoteFactory[] MBFVfactory = null;
  private static final int numSteps = 16;
  private static int[] goodProof = {
    1, 2,
  };
  private static int[] badProof = {
    0,
  };
  private static byte[] goodContent = null;
  private static byte[] badContent = null;
  private static byte[] pollID = null;
  private static PeerIdentity voterID = null;
  private static byte[] basisT = null;
  private static byte[] basisA0 = null;
  private static IdentityManager idmgr = null;

  /**
   * Set up the test case by creating the two basis arrays and a good
   * and a bad content array (if necessary),  and creating the necessary
   * factory instances, poll ID and voter ID.
   *
   * @throws Exception should not happen
   */
  protected void setUp() throws Exception {
    super.setUp();
    log = Logger.getLogger("TestMemoryBoundFunction");
    MockLockssDaemon theDaemon = getMockLockssDaemon();
    String tempDirPath = null;
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    catch (IOException ex) {
      fail("unable to create a temporary directory");
    }
    Properties p = new Properties();
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    theDaemon.setIdentityManager(new MockIdentityManager());
    idmgr = theDaemon.getIdentityManager();
    if (false)
      rand = new Random(100);
    else
      rand = new Random(System.currentTimeMillis());
    if (basisT == null) {
      basisT = new byte[/* 16* */ 1024*1024];
      rand.nextBytes(basisT);
      log.info(basisT.length + " bytes of T created");
    }
    if (basisA0 == null) {
      basisA0 = new byte[1024];
      rand.nextBytes(basisA0);
      log.info(basisA0.length + " bytes of A0 created");
    }
    if (goodContent == null) {
      goodContent = new byte[256*1024];
      rand.nextBytes(goodContent);
      log.info(goodContent.length + " bytes of good synthetic content created");
    }
    if (badContent == null) {
      badContent = new byte[256*1024];
      rand.nextBytes(badContent);
      log.info(badContent.length + " bytes of bad synthetic content created");
    }
    if (MBFfactory == null) {
      MBFfactory = new MemoryBoundFunctionFactory[MBFnames.length];
      for (int i = 0; i < MBFfactory.length; i++) {
	try {
	  MBFfactory[i] = new MemoryBoundFunctionFactory(MBFnames[i],
							 basisA0, basisT);
	} catch (NoSuchAlgorithmException ex) {
	  fail(MBFnames[i] + " threw " + ex.toString());
	}
      }
    }
    if (MBFVfactory == null) {
      MBFVfactory = new MemoryBoundFunctionVoteFactory[MBFVnames.length];
      for (int i = 0; i < MBFVfactory.length; i++) {
	try {
	  MBFVfactory[i] = new MemoryBoundFunctionVoteFactory(MBFVnames[i]);
	} catch (NoSuchAlgorithmException ex) {
	  fail(MBFVnames[i] + " threw " + ex.toString());
	}
      }
    }
    pollID = new byte[20];
    rand.nextBytes(pollID);
//     try {
      voterID = new MockPeerIdentity("127.0.0.1");
//       voterID = idmgr.stringToPeerIdentity("127.0.0.1");
//     } catch (IdentityManager.MalformedIdentityKeyException ex) {
//       fail("PeerIdentity throws: " + ex.toString());
//     }
  }

  /** tearDown method for test case
   * @throws Exception should not happen
   */
  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test factory by trying to create a bogus one.
   */
  public void testBadFactory() {
    boolean gotException = false;
    try {
      MemoryBoundFunctionVoteFactory tmp =
	new MemoryBoundFunctionVoteFactory("BOGUS");
    } catch (NoSuchAlgorithmException ex) {
      gotException = true;
    } catch (Exception ex) {
      fail("BOGUS threw " + ex.toString());
    }
    if (!gotException)
      fail("BOGUS didn't throw NoSuchAlgorithmException");
  }

  /**
   * Test factories by creating a generator and a verifier for each
   * possible combination of MBF Vote and MBF implementations.
   */
  public void testGoodFactory() {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    CachedUrlSet cus = new MockCachedUrlSet();
    for (int j = 0; j < MBFVfactory.length; j++) {
      for (int i = 0; i < MBFfactory.length; i++) {
	try {
	  MemoryBoundFunctionVote mbfv =
	    MBFVfactory[j].makeGenerator(MBFfactory[i],
					 nonce,
					 3,
					 cus,
					 pollID,
					 voterID);
	} catch (MemoryBoundFunctionException ex) {
	  fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	       " threw " + ex.toString());
	} catch (NoSuchAlgorithmException ex) {
	  fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	       " threw " + ex.toString());
	}
      }
    }
    int[][] sVals = { { 1, 2 } };
    byte[][] hashVals = new byte[1][];
    hashVals[0] = nonce;
    for (int j = 0; j < MBFVfactory.length; j++) {
      for (int i = 0; i < MBFfactory.length; i++) {
	try {
	  MemoryBoundFunctionVote mbfv =
	    MBFVfactory[j].makeVerifier(MBFfactory[i],
					nonce,
					3,
					cus,
					sVals,
					hashVals,
					pollID,
					voterID);
	} catch (MemoryBoundFunctionException ex) {
	  fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	       " threw " + ex.toString());
	} catch (NoSuchAlgorithmException ex) {
	  fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	       " threw " + ex.toString());
	}
      }
    }
  }

  /**
   * Test an agreeing vote for each combination of MBF Vote and MBF
   * implementation.
   */
  public void testAgreeingVote() {
    for (int j = 0; j < MBFVfactory.length; j++)
      for (int i = 0; i < MBFfactory.length; i++)
	agreeingVote(i, j);
  }

  /**
   * Test a disagreeing vote for each combination of MBF Vote and MBF
   * implementation.  The vote should not agree because the CUS content
   * is different.
   */
  public void testDisagreeingVote() {
    for (int j = 0; j < MBFVfactory.length; j++)
      for (int i = 0; i < MBFfactory.length; i++)
	disagreeingVote(i, j);
  }

  /**
   * Test a vote whose CUS is half the length it is supposed to be,
   * which should not agree.
   */
  public void testShortVote() {
    for (int j = 0; j < MBFVfactory.length; j++)
      for (int i = 0; i < MBFfactory.length; i++)
	shortVote(i, j);
  }

  /**
   * Test verifying a vote with a nonce different from the one
   * used to generate it, which should not agree.
   */
  public void testInvalidNonce() {
    for (int j = 0; j < MBFVfactory.length; j++)
      for (int i = 0; i < MBFfactory.length; i++)
	invalidNonce(i, j);
  }

  /*
   * Test verifying a vote which should agree.
   */
  private void agreeingVote(int i, int j) {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    CachedUrlSet cus1 = goodCus(128);
    if (cus1 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    CachedUrlSet cus2 = goodCus(128);
    if (cus2 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    onePair(i, j, cus1, cus2, nonce, nonce, true, true, 4096);
  }

  /*
   * Test verifying a vote which should not agree because the CUS content
   * is different.
   */
  private void disagreeingVote(int i, int j) {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    CachedUrlSet cus1 = badCus(128);
    if (cus1 == null)
      fail("badCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    CachedUrlSet cus2 = goodCus(128);
    if (cus2 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    onePair(i, j, cus1, cus2, nonce, nonce, false, true, 4096);
  }

  private void shortVote(int i, int j) {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    CachedUrlSet cus1 = goodCus(128);
    if (cus1 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    CachedUrlSet cus2 = shortCus(128);
    if (cus2 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    onePair(i, j, cus1, cus2, nonce, nonce, false, true, 4096);
  }

  private void invalidNonce(int i, int j) {
    byte[] nonce1 = new byte[4];
    rand.nextBytes(nonce1);
    byte[] nonce2 = new byte[4];
    rand.nextBytes(nonce2);
    assertFalse(MessageDigest.isEqual(nonce1,nonce2));
    CachedUrlSet cus1 = goodCus(50000);
    if (cus1 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    CachedUrlSet cus2 = goodCus(50000);
    if (cus2 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    int fail = 0;
    onePair(i, j, cus1, cus2, nonce1, nonce2, true, false, 1000000);
  }

  private void onePair(int i, int j,
		       CachedUrlSet cus1,
		       CachedUrlSet cus2,
		       byte[] nonce1,
		       byte[] nonce2,
		       boolean shouldBeAgreeing,
		       boolean shouldBeValid,
		       int stepLimit) {
    // Make a generator
    MemoryBoundFunctionVote gen = generator(i, j, nonce1, cus1);
    assertFalse(gen==null);
    if (i == 0) {
      // Set the proof array that the MockMemoryBoundFunction will return
      // for generation and use for verification
      MockMemoryBoundFunction.setProof(goodProof);
    }
    // Generate the vote
    try {
      int s = stepLimit;
      while (s > 0 && gen.computeSteps(numSteps)) {
	assertFalse(gen.finished());
	s -= numSteps;
      }
    } catch (MemoryBoundFunctionException ex) {
      fail("generator.computeSteps() threw " + ex.toString() + " for "  + MBFnames[i] + "," + MBFVnames[j]);
    }
    assertTrue(gen.finished());
    // Recover the vote
    int[][] proofArray = gen.getProofArray();
    if (proofArray == null)
      fail("generated a null proof array " + MBFnames[i] + "," + MBFVnames[j]);
    // Recover the hashes
    byte[][] hashArray = gen.getHashArray();
    if (hashArray == null)
      fail("generated a null proof array " + MBFnames[i] + "," + MBFVnames[j]);
    if (proofArray.length != hashArray.length)
      fail("proof " + proofArray.length + " hash " +
	   hashArray.length + " not equal " + MBFnames[i] + "," + MBFVnames[j]);
    // Make a verifier
    MemoryBoundFunctionVote ver = verifier(i, j, nonce2, cus2,
					   proofArray, hashArray);
    assertFalse(ver==null);
    if (i == 0) {
      // Set the proof array that the MockMemoryBoundFunction will return
      // for generation and use for verification
      MockMemoryBoundFunction.setProof(shouldBeValid ? goodProof : badProof);
    }
    if (j == 0) {
      ((MockMemoryBoundFunctionVote)ver).setValid(shouldBeValid);
      ((MockMemoryBoundFunctionVote)ver).setAgreeing(shouldBeAgreeing);
    }
    // Verify the vote
    try {
      int s = stepLimit;
      while (s > 0 && ver.computeSteps(numSteps)) {
	assertFalse(ver.finished());
	s -= numSteps;
      }
    } catch (MemoryBoundFunctionException ex) {
      fail("verifier.computeSteps() threw " + ex.toString() + " for " + MBFnames[i] + "," + MBFVnames[j]);
    }
    assertTrue(ver.finished());
    // Get valid/invalid
    boolean valid = false;
    try {
      valid = ver.valid();
      if (shouldBeValid && !valid)
	fail("verifier declared valid vote invalid " + MBFnames[i] + "," + MBFVnames[j]);
      else if (!shouldBeValid && valid) {
	log.warning("verifier declared invalid vote valid (can happen)" +
		       MBFnames[i] + "," + MBFVnames[j]);
      }
    } catch (MemoryBoundFunctionException ex) {
      fail("verifier.valid() threw " + ex.toString());
    }
    // Get agreeing/disagreeing
    if (valid) try {
      boolean agreeing = ver.agreeing();
      if (shouldBeAgreeing && !agreeing)
	fail("verifier declared agreeing vote disgreeing " + MBFnames[i] + "," + MBFVnames[j]);
      else if (!shouldBeAgreeing && agreeing)
	fail("verifier declared disagreeing vote agreeing " + MBFnames[i] + "," + MBFVnames[j]);
    } catch (MemoryBoundFunctionException ex) {
      fail("verifier.agreeing() threw " + ex.toString() + " for " + MBFnames[i] + "," + MBFVnames[j]);
    }
  }

  MemoryBoundFunctionVote generator(int i, int j,
				    byte[] nonce,
				    CachedUrlSet cus) {
    if (cus == null)
      fail("generator() - no CUS");
    MemoryBoundFunctionVote ret = null;
    try {
      ret = MBFVfactory[j].makeGenerator(MBFfactory[i],
					 nonce,
					 3,
					 cus,
					 pollID,
					 voterID);
    } catch (MemoryBoundFunctionException ex) {
      fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	   " threw " + ex.toString());
    } catch (NoSuchAlgorithmException ex) {
      fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	   " threw " + ex.toString());
    }
    if (ret instanceof MockMemoryBoundFunctionVote) {
      int[][] prfs = {
	{1},
	{1,2},
	{1,2,3},
	{1,2,3,4},
	{1,2,3,4,5},
	{1,2,3,4,5,6},
	{1,2,3,4,5,6,7},
	{1,2,3,4,5,6,7,8},
	{1,2,3,4,5,6,7,8,9},
	{1,2,3,4,5,6,7,8,9,10}
      };
      ((MockMemoryBoundFunctionVote)ret).setProofs(prfs);
      byte[][] hashes = {
	{1},
	{1,2},
	{1,2,3},
	{1,2,3,4},
	{1,2,3,4,5},
	{1,2,3,4,5,6},
	{1,2,3,4,5,6,7},
	{1,2,3,4,5,6,7,8},
	{1,2,3,4,5,6,7,8,9},
	{1,2,3,4,5,6,7,8,9,10}
      };
      ((MockMemoryBoundFunctionVote)ret).setHashes(hashes);
      ((MockMemoryBoundFunctionVote)ret).setStepCount(256);
    }
    {
      ArchivalUnit au = cus.getArchivalUnit();
      if (au == null)
	fail("generator() - null AU");
      String auid = au.getAuId();
      if (auid == null)
	fail("generator() - null auID " + MBFnames[i] + "," + MBFVnames[j]);
      log.info("generator() " + MBFVnames[j] + ":" + MBFnames[i] +
	       " for " + auid);
    }
    return ret;
  }

  MemoryBoundFunctionVote verifier(int i, int j,
				   byte[] nonce,
				   CachedUrlSet cus,
				   int[][] proofArray,
				   byte[][] hashArray) {
    MemoryBoundFunctionVote ret = null;
    try {
      ret = MBFVfactory[j].makeVerifier(MBFfactory[i],
					nonce,
					3,
					cus,
					proofArray,
					hashArray,
					pollID,
					voterID);
    } catch (MemoryBoundFunctionException ex) {
      fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	   " threw " + ex.toString());
    } catch (NoSuchAlgorithmException ex) {
      fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	   " threw " + ex.toString());
    }
    if (ret instanceof MockMemoryBoundFunctionVote) {
      int[][] prfs = {
	{1},
	{1,2},
	{1,2,3},
	{1,2,3,4},
	{1,2,3,4,5},
	{1,2,3,4,5,6},
	{1,2,3,4,5,6,7},
	{1,2,3,4,5,6,7,8},
	{1,2,3,4,5,6,7,8,9},
	{1,2,3,4,5,6,7,8,9,10}
      };
      ((MockMemoryBoundFunctionVote)ret).setProofs(prfs);
      byte[][] hashes = {
	{1},
	{1,2},
	{1,2,3},
	{1,2,3,4},
	{1,2,3,4,5},
	{1,2,3,4,5,6},
	{1,2,3,4,5,6,7},
	{1,2,3,4,5,6,7,8},
	{1,2,3,4,5,6,7,8,9},
	{1,2,3,4,5,6,7,8,9,10}
      };
      ((MockMemoryBoundFunctionVote)ret).setHashes(hashes);
      ((MockMemoryBoundFunctionVote)ret).setStepCount(256);
    }
    {
      ArchivalUnit au = cus.getArchivalUnit();
      if (au == null)
	fail("verifier() - null AU");
      String auid = au.getAuId();
      if (auid == null)
	fail("verifier() - null auID " + MBFnames[i] + "," + MBFVnames[j]);
      log.info("verifier() " + MBFVnames[j] + ":" + MBFnames[i] +
	       " for " + auid);
    }
    return ret;
  }

  private CachedUrlSet goodCus(int bytes) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId("TestMemoryBoundFunctionVote:goodAU");  // XXX
    MockCachedUrlSetHasher hash = new MockCachedUrlSetHasher(bytes);
    MockCachedUrlSet ret = new MockCachedUrlSet();
    byte[] content = new byte[bytes];
    for (int i = 0; i < content.length; i++)
      content[i] = goodContent[ i % goodContent.length];
    ret.setContentToBeHashed(content);
    ret.setArchivalUnit(au);
    ret.setContentHasher(hash);
    return (ret);
  }

  private CachedUrlSet badCus(int bytes) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId("TestMemoryBoundFunctionVote:goodAU");  // XXX
    MockCachedUrlSetHasher hash = new MockCachedUrlSetHasher(bytes);
    MockCachedUrlSet ret = new MockCachedUrlSet();
    byte[] content = new byte[bytes];
    for (int i = 0; i < content.length; i++)
      content[i] = badContent[ i % badContent.length];
    ret.setContentToBeHashed(content);
    ret.setArchivalUnit(au);
    ret.setContentHasher(hash);
    return (ret);
  }

  private CachedUrlSet shortCus(int bytes) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId("TestMemoryBoundFunctionVote:goodAU");  // XXX
    MockCachedUrlSetHasher hash = new MockCachedUrlSetHasher(bytes);
    MockCachedUrlSet ret = new MockCachedUrlSet();
    byte[] nonce = new byte[bytes/2];
    for (int i = 0; i < nonce.length; i++)
      nonce[i] = goodContent[i % goodContent.length];
    ret.setContentToBeHashed(nonce);
    ret.setArchivalUnit(au);
    ret.setContentHasher(hash);
    return (ret);
  }

  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {
        TestMemoryBoundFunctionVote.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
