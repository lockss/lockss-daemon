/*
 * $Id: MBFV2.java,v 1.3 2003-08-25 17:46:37 dshr Exp $
 */

/*

Copyright (c) 2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.math.*;
import java.io.*;
import java.util.*;
import java.security.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;

/**
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class MBFV2 extends MemoryBoundFunctionVote {
  private static final String algHash = "SHA1";
  private static final int Finished = 0;
  private static final int StartingGeneration = 1;
  private static final int StartingVerification = 2;
  private static final int FirstBlockProofGeneration = 3;
  private static final int BlockHashGeneration = 4;
  private static final int BlockProofGeneration = 5;
  private static final int FirstBlockProofVerification = 6;
  private static final int BlockHashVerification = 7;
  private static final int BlockProofVerification = 8;
  private static final int Bad = 9;
  private static final String[] stateName = {
    "Finished",
    "StartingGeneration",
    "StartingVerification",
    "FirstBlockProofGeneration",
    "BlockHashGeneration",
    "BlockProofGeneration",
    "FirstBlockProofVerification",
    "BlockHashVerification",
    "BlockProofVerification",
    "Bad",
  };

  private int currentState;
  private MessageDigest proofDigest;
  private MemoryBoundFunction mbf;
  private MessageDigest contentDigest;
  private CachedUrlSetHasher hasher;
  private int[] thisProof;
  private byte[] thisHash;
  private int index;
  private int bytesToGo;
  private int[][] ourProofs;
  private byte[][] ourHashes;
  private int stepLimit;
  
  /**
   * No-argument constructor for use with Class.newInstance().
   */
  public MBFV2() {
    proofDigest = null;
    mbf = null;
    contentDigest = null;
    hasher = null;
    thisProof = null;
    thisHash = null;
    index = -1;
    bytesToGo = -1;
    stepLimit = 4096;
    currentState = Bad;
  }

  /**
   * Public constructor for an object that will compute a vote
   * using hashing and memory bound functions.
   * It accepts as input a nonce and a CachedUrlSet.  It divides the
   * content into i blocks of length <= 2**(i+1) and for each computes
   * the MBF proof and the hash.  The first proof depends on the nonce
   * the AU name and the first bytes of the AU.  Subsequent proofs
   * depend on the preceeding hash and proof.  The effort sizer e
   * is constant for all rounds.  The path length l is set equal to
   * the block size for that round.
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param cus the CachedUrlSet containing the content to be voted on
   *
   */
  protected void setupGeneration(MemoryBoundFunctionFactory fact,
				 byte[] nVal,
				 int eVal,
				 CachedUrlSet cusVal) 
    throws MemoryBoundFunctionException {
    super.setupGeneration(fact, nVal, eVal, cusVal);
    setup(nVal, eVal, cusVal);
    setState(StartingGeneration);
  }

  /**
   * Public constructor for an object that will verify a vote
   * using hashing and memory bound functions.  It accepts as
   * input a nonce,  a cachedUrlSet,  and arrays of proofs
   * and hashes.
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param cus the CachedUrlSet containing the content to be voted on
   * @param sVals the starting points chosen by the prover for each block
   * @param hashes the hashes of each block
   * 
   */
  public void setupVerification(MemoryBoundFunctionFactory fact,
				byte[] nVal,
				int eVal,
				CachedUrlSet cusVal,
				int sVals[][],
				byte[][] hashes)
    throws MemoryBoundFunctionException {
    super.setupVerification(fact, nVal, eVal, cusVal, sVals, hashes);
    setup(nVal, eVal, cusVal);
    setState(StartingVerification);
    ourProofs = getProofArray();
    ourHashes = getHashArray();
    if (ourProofs.length != ourHashes.length)
      throw new MemoryBoundFunctionException("proofs " + ourProofs.length +
					     " != hashes " + ourHashes.length);
  }

  private void setup(byte[] nVal, int eVal, CachedUrlSet cusVal) throws
    MemoryBoundFunctionException {
    finished = false;
    if (proofDigest == null) try {
      proofDigest = MessageDigest.getInstance(algHash);
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException(algHash + " throws " +
					     ex.toString());
    }
    if (contentDigest == null) try {
      contentDigest = MessageDigest.getInstance(algHash);
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException(algHash + " throws " +
					     ex.toString());
    }
    hasher = cusVal.getContentHasher(contentDigest);
    mbf = null;
    thisProof = null;
    thisHash = null;
    index = -1;
  }

  private void setState(int newState) {
    logger.info("Change state from " + stateName[currentState] + " to " +
		stateName[newState]);
    currentState = newState;
  }

  /**
   * Do "n" steps of the underlying hash or effort proof generation
   * @param n number of steps to move.
   * @return true if there is more work to do
   * 
   */
  public boolean computeSteps(int n) throws MemoryBoundFunctionException {
    logger.info("computeSteps: block " + index + " steps " + n + " state " +
		stateName[currentState]);
    stepLimit -= n;
    if (stepLimit <= 0)
      throw new MemoryBoundFunctionException("no more steps");
    switch (currentState) {
    case Finished:
      return false;
    case StartingGeneration:
      startingGeneration(n);
      break;
    case StartingVerification:
      startingVerification(n);
      break;
    case FirstBlockProofGeneration:
      firstBlockProofGeneration(n);
      break;
    case BlockHashGeneration:
      blockHashGeneration(n);
      break;
    case BlockProofGeneration:
      blockProofGeneration(n);
      break;
    case FirstBlockProofVerification:
      firstBlockProofVerification(n);
      break;
    case BlockHashVerification:
      blockHashVerification(n);
      break;
    case BlockProofVerification:
      blockProofVerification(n);
      break;
    default:
      throw new MemoryBoundFunctionException("bad state " + currentState);
    }
    if (currentState == Finished)
      finished = true;
    return (!finished);
  }

  private void startingGeneration(int n) throws MemoryBoundFunctionException {
    if (verify || currentState != StartingGeneration)
      throw new MemoryBoundFunctionException("bad state");
    if (index >= 0)
      throw new MemoryBoundFunctionException("bad index " + index + " in " +
					     stateName[currentState]);
    // The first proof uses the nonce and the AU name [XXX and
    // also the first bytes of the AU].
    proofDigest.update(nonce);
    if (cus != null) {
      ArchivalUnit au = cus.getArchivalUnit();
      String AUid = au.getAUId();
      proofDigest.update(AUid.getBytes());
    } else {
      throw new MemoryBoundFunctionException("no CUS");
    }
    // XXX should also hash first bytes of AU
    thisHash = null;
    thisProof = null;
    setState(FirstBlockProofGeneration);
  }

  private void startingVerification(int n) throws MemoryBoundFunctionException {
    if (!verify || currentState != StartingVerification)
      throw new MemoryBoundFunctionException("bad state");
    if (index >= 0)
      throw new MemoryBoundFunctionException("bad index " + index + " in " +
					     stateName[currentState]);
    logger.info("starting verification " + ourProofs.length + "/" +
		ourHashes.length);
    // The first proof uses the nonce and the AU name [XXX and
    // also the first bytes of the AU].
    proofDigest.update(nonce);
    if (cus != null) {
      ArchivalUnit au = cus.getArchivalUnit();
      String AUid = au.getAUId();
      proofDigest.update(AUid.getBytes());
    } else {
      throw new MemoryBoundFunctionException("no CUS");
    }
    // XXX should also hash first bytes of AU
    logger.info("verifySteps: index < 0");
    thisHash = null;
    thisProof = null;
    setState(FirstBlockProofVerification);
  }

  private void firstBlockProofGeneration(int n) throws MemoryBoundFunctionException {
    if (verify || currentState != FirstBlockProofGeneration)
      throw new MemoryBoundFunctionException("bad state");
    if (thisHash != null)
      throw new MemoryBoundFunctionException("thisHash should be null" + " in " +
					     stateName[currentState]);
    if (thisProof != null)
      throw new MemoryBoundFunctionException("thisProof should be null" + " in " +
					     stateName[currentState]);
    if (index >= 0)
      throw new MemoryBoundFunctionException("index should be -1" + " in " +
					     stateName[currentState]);
    if (mbf != null)
      throw new MemoryBoundFunctionException("mbf should be null" + " in " +
					     stateName[currentState]);
    try{
      mbf = factory.make(proofDigest.digest(),
			 e,
			 1,
			 null,
			 0);
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString());
    }
    index = 0;
    setState(BlockProofGeneration);
  }

  private void blockHashGeneration(int n) throws MemoryBoundFunctionException {
    if (verify || currentState != BlockHashGeneration)
      throw new MemoryBoundFunctionException("bad state");
    if (thisHash != null)
      throw new MemoryBoundFunctionException("thisHash should be null" + " in " +
					     stateName[currentState]);
    if (thisProof == null)
      throw new MemoryBoundFunctionException("thisProof should not be null" + " in " +
					     stateName[currentState]);
    if (bytesToGo <= 0)
      throw new MemoryBoundFunctionException("bytesToGo should be > 0" + " in " +
					     stateName[currentState]);
    if (index < 0)
      throw new MemoryBoundFunctionException("index should be >= 0" + " in " +
					     stateName[currentState]);
    try {
      int bytesHashed = hasher.hashStep(bytesToGo > n ? n : bytesToGo);
      logger.info("generateSteps: hashed " + bytesHashed + "/" + bytesToGo
		  + " bytes");
      if (bytesHashed <= 0)
	throw new MemoryBoundFunctionException("too few bytes hashed " +
					       bytesHashed + " in " +
					     stateName[currentState]);
      bytesToGo -= bytesHashed;
      boolean done = hasher.finished();
      if (bytesToGo <= 0 || done) {
	// Extract the hash of this block and reset hasher for next
	thisHash = contentDigest.digest();
	if (thisHash == null)
	  throw new MemoryBoundFunctionException("contentDigest return null" + " in " +
					     stateName[currentState]);
  	saveHash(index, thisHash);
	logger.info("generateSteps: finished hashing block");
      }
      if (done) {
	finished = true;
	logger.info("generateSteps: finished hashing AU");
	setState(Finished);
      } else if (bytesToGo <= 0) {
	index++;
	setState(BlockProofGeneration);
      }
    } catch (IOException ex) {
      throw new MemoryBoundFunctionException("content hash threw " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    }
  }

  private void blockProofGeneration(int n) throws MemoryBoundFunctionException {
    if (verify || currentState != BlockProofGeneration)
      throw new MemoryBoundFunctionException("bad state");
    if (index < 0)
      throw new MemoryBoundFunctionException("index should be >= 0" + " in " +
					     stateName[currentState]);
    if (mbf == null) try {
      if (thisHash == null)
	throw new MemoryBoundFunctionException("thisHash should not be null in " +
					       stateName[currentState]);
      if (thisProof == null)
	throw new MemoryBoundFunctionException("thisProof should not be null in " +
					     stateName[currentState]);
      // Second or subsequent block - nonce is previous hash and
      // previous proof.
      proofDigest.update(thisHash);
      for (int i = 0; i < thisProof.length; i++)
	proofDigest.update(intToByteArray(thisProof[i]));
      logger.info("generateSteps: index " + index);
      thisHash = null;
      thisProof = null;
      mbf = factory.make(proofDigest.digest(),
			 e,
			 (1 << index),
			 null,
			 0);
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    } else {
      if (thisProof != null)
	throw new MemoryBoundFunctionException("thisProof should be null in " +
					     stateName[currentState]);
      if (thisHash != null)
	throw new MemoryBoundFunctionException("thisHash should be null in " +
					       stateName[currentState]);
    }
    // Work on generating the proof
    mbf.computeSteps(n);
    if (mbf.done()) {
      logger.info("generateSteps: proof finished");
      // Finished
      thisProof = mbf.result();
      saveProof(index, thisProof);
      bytesToGo = (1 << index);
      setState(BlockHashGeneration);
      mbf = null;
    }
  }

  private void firstBlockProofVerification(int n) throws MemoryBoundFunctionException {
    if (!verify || currentState != FirstBlockProofVerification)
      throw new MemoryBoundFunctionException("bad state");
    if (thisHash != null)
      throw new MemoryBoundFunctionException("thisHash should be null" + " in " +
					     stateName[currentState]);
    if (thisProof != null)
      throw new MemoryBoundFunctionException("thisProof should be null" + " in " +
					     stateName[currentState]);
    if (index > 0)
      throw new MemoryBoundFunctionException("index should be <= 0" + " in " +
					     stateName[currentState]);
    if (mbf != null)
      throw new MemoryBoundFunctionException("mbf should be null" + " in " +
					     stateName[currentState]);
    index = 0;
    thisProof = ourProofs[index];
    thisHash = null;
    try{
      mbf = factory.make(proofDigest.digest(),
			 e,
			 (1 << index),
			 thisProof,
			 (1 << (index + 1))); // XXX fix this
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    }
    setState(BlockProofVerification);
  }

  private void blockHashVerification(int n) throws MemoryBoundFunctionException {
    if (!verify || currentState != BlockHashVerification)
      throw new MemoryBoundFunctionException("bad state");
    if (thisHash != null)
      throw new MemoryBoundFunctionException("thisHash should be null in " +
					     stateName[currentState]);
    if (thisProof != null)
      throw new MemoryBoundFunctionException("thisProof should be null" + " in " +
					     stateName[currentState]);
    if (bytesToGo <= 0)
      throw new MemoryBoundFunctionException("bytesToGo should be > 0" + " in " +
					     stateName[currentState]);
    if (index < 0)
      throw new MemoryBoundFunctionException("index should be >= 0" + " in " +
					     stateName[currentState]);
    /* if (agreeing) */ try {
      int bytesHashed = hasher.hashStep(bytesToGo > n ? n : bytesToGo);
      logger.info("verifySteps: hashed " + bytesHashed + "/" + bytesToGo
		  + " bytes");
      bytesToGo -= bytesHashed;
      boolean done = hasher.finished();
      if (bytesToGo <= 0 || done) {
	// Extract the hash of this block and reset hasher for next
	thisHash = ourHashes[index];
	if (MessageDigest.isEqual(thisHash, contentDigest.digest())) {
	  // Hashes don't match
	  logger.info("verifySteps: hashes don't match");
	  agreeing = false;
	}
	logger.info("verifySteps: finished hashing block");
	mbf = null;
	thisHash = null;
	thisProof = null;
      }
      if (done) {
	finished = true;
	logger.info("verifySteps: finished hashing AU");
	setState(Finished);
      } else if (bytesToGo <= 0) {
	index++;
	setState(BlockProofVerification);
      }
    } catch (IOException ex) {
      throw new MemoryBoundFunctionException("content hash threw " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    }
  }

  private void blockProofVerification(int n) throws MemoryBoundFunctionException {
    if (!verify || currentState != BlockProofVerification)
      throw new MemoryBoundFunctionException("bad state");
    if (index < 0)
      throw new MemoryBoundFunctionException("index should be >= 0" + " in " +
					     stateName[currentState]);
    if (thisHash != null)
      throw new MemoryBoundFunctionException("thisHash should be null" + " in " +
					     stateName[currentState]);
    if (mbf == null) try {
      // Second or subsequent block - nonce is previous hash and
      // previous proof.
      proofDigest.update(ourHashes[index]);
      for (int i = 0; i < ourProofs[index].length; i++)
	proofDigest.update(intToByteArray(ourProofs[index][i]));
      thisProof = ourProofs[index];
      mbf = factory.make(proofDigest.digest(),
			 e,
			 (1 << index),
			 thisProof,
			 (1 << (index + 1))); // XXX fix this
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    }
    if (thisProof == null)
      throw new MemoryBoundFunctionException("thisProof should not be null in " +
					     stateName[currentState]);
    // No proof yet - work on proof
    mbf.computeSteps(n);
    if (mbf.done()) {
      // Finished
      if (mbf.result() == null) {
	logger.info("verifySteps: proof invalid");
	valid = false;
      }
      thisProof = null;
      bytesToGo = (1 << index);
      setState(BlockHashVerification);
    }
  }

  private byte[] intToByteArray(int b) {
    byte[] ret = new byte[4];
    ret[0] = (byte)(b & 0xff);
    ret[1] = (byte)((b >> 8) & 0xff);
    ret[2] = (byte)((b >> 16) & 0xff);
    ret[3] = (byte)((b >> 24) & 0xff);
    return (ret);
  }
}
