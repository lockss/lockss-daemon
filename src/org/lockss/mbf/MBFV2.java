/*
 * $Id: MBFV2.java,v 1.2 2003-08-24 22:38:25 dshr Exp $
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
    ourProofs = getProofArray();
    ourHashes = getHashArray();
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

  /**
   * Do "n" steps of the underlying hash or effort proof generation
   * @param n number of steps to move.
   * @return true if there is more work to do
   * 
   */
  public boolean computeSteps(int n) throws MemoryBoundFunctionException {
    boolean ret = false;
    if (verify)
      ret = verifySteps(n);
    else
      ret = generateSteps(n);
    return (ret);
  }

  /**
   * Do "n" steps of the underlying hash or effort proof generation
   * @param n number of steps to move.
   * @return true if there is more work to do
   * 
   */
  private boolean generateSteps(int n) throws MemoryBoundFunctionException {
    if (finished)
      return (false);
    logger.info("generateSteps: " + index + " n " + n);
    if (mbf == null) {
      // This is the beginning of a vote block.  Is it the first?
      if (index < 0) {
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
	logger.info("generateSteps: index < 0");
	index = 0;
      } else {
	if (thisHash == null)
	  throw new MemoryBoundFunctionException("block " + index +
						 " null thisHash");
	// Second or subsequent block - nonce is previous hash and
	// previous proof.
	proofDigest.update(thisHash);
	for (int i = 0; i < thisProof.length; i++)
	  proofDigest.update(intToByteArray(thisProof[i]));
	// NB - proof  and hash have been added to ArrayLists already
	logger.info("generateSteps: index " + index);
	index++;
      }
      thisHash = null;
      thisProof = null;
      try{
	mbf = factory.make(proofDigest.digest(),
			   e,
			   (1 << index),
			   null,
			   0);
      } catch (NoSuchAlgorithmException ex) {
	throw new MemoryBoundFunctionException("factory throws " +
					       ex.toString());
      }
    }
    logger.info("generateSteps: thisHash " + (thisHash == null ? "null" : "") +
		" thisProof " + (thisProof == null ? "null" : ""));
    // We have an MBF - has it generated a proof yet?
    if (thisProof == null) {
      // No proof yet - work on proof
      mbf.computeSteps(n);
      if (mbf.done()) {
	logger.info("generateSteps: proof finished");
	// Finished
	thisProof = mbf.result();
	saveProof(index, thisProof);
	bytesToGo = (1 << index);
      } else {
	logger.info("generateSteps: " + n + " steps");
      }
    } else if (thisHash == null) try {
      int bytesHashed = hasher.hashStep(bytesToGo > n ? n : bytesToGo);
      logger.info("generateSteps: hashed " + bytesHashed + "/" + bytesToGo
		  + " bytes");
      if (bytesHashed <= 0)
	throw new MemoryBoundFunctionException("too few bytes hashed " +
					       bytesHashed);
      bytesToGo -= bytesHashed;
      boolean done = hasher.finished();
      if (bytesToGo <= 0 || done) {
	// Extract the hash of this block and reset hasher for next
	thisHash = contentDigest.digest();
	if (thisHash == null)
	  throw new MemoryBoundFunctionException("contentDigest return null");
  	saveHash(index, thisHash);
	mbf = null;
	logger.info("generateSteps: finished hashing block");
      }
      if (done) {
	finished = true;
	logger.info("generateSteps: finished hashing AU");
      }
    } catch (IOException ex) {
      throw new MemoryBoundFunctionException("content hash threw " +
					     ex.toString());
    }
    return (!finished);
  }

  /**
   * Do "n" steps of the underlying hash or effort proof verification
   * @param n number of steps to move.
   * @return true if there is more work to do
   * 
   */
  private boolean verifySteps(int n) throws MemoryBoundFunctionException {
    if (finished)
      return (false);
    logger.info("verifySteps: " + index + " n " + n);
    if (mbf == null) {
      // This is the beginning of a vote block.  Is it the first?
      if (index < 0) {
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
	logger.info("verifySteps: index < 0");
	// XXX should also hash first bytes of AU
	index = 0;
      } else {
	if (thisHash == null)
	  throw new MemoryBoundFunctionException("block " + index +
						 " null thisHash");
	// Second or subsequent block - nonce is previous hash and
	// previous proof.
	proofDigest.update(thisHash);
	for (int i = 0; i < ourProofs[index].length; i++)
	  proofDigest.update(intToByteArray(ourProofs[index][i]));
	index++;
      }
      thisHash = null;
      thisProof = ourProofs[index];
      try{
	mbf = factory.make(proofDigest.digest(),
			   e,
			   (1 << index),
			   thisProof,
			   (1 << (index + 1))); // XXX fix this
      } catch (NoSuchAlgorithmException ex) {
	throw new MemoryBoundFunctionException("factory throws " +
					       ex.toString());
      }
    }
    logger.info("verifySteps: thisHash " + (thisHash == null ? "null" : "") +
		" thisProof " + (thisProof == null ? "null" : ""));
    // We have an MBF - has it generated a proof yet?
    if (thisProof != null) {
      // No proof yet - work on proof
      mbf.computeSteps(n);
      if (mbf.done()) {
	// Finished
	if (mbf.result() == null)
	  valid = false;
      }
      thisProof = null;
      bytesToGo = (1 << index);
    } else if (thisHash == null && agreeing) try {
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
	  agreeing = false;
	}
	mbf = null;
      }
      if (done) {
	finished = true;
      }
    } catch (IOException ex) {
      throw new MemoryBoundFunctionException("content hash threw " +
					     ex.toString());
    }
    return (!finished);
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
