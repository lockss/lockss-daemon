/*
 * $Id: MBFV2.java,v 1.1 2003-08-11 18:44:51 dshr Exp $
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
  public MBFV2(MemoryBoundFunctionFactory fact,
	       byte[] nVal,
	       int eVal,
	       CachedUrlSet cusVal) 
    throws MemoryBoundFunctionException {
    super(fact, nVal, eVal, cusVal);
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
  public MBFV2(MemoryBoundFunctionFactory fact,
	       byte[] nVal,
	       int eVal,
	       CachedUrlSet cusVal,
	       int sVals[][],
	       byte[][] hashes) throws MemoryBoundFunctionException {
    super(fact, nVal, eVal, cusVal, sVals, hashes);
    setup(nVal, eVal, cusVal);
  }

  private void setup(byte[] nVal, int eVal, CachedUrlSet cusVal) throws
    MemoryBoundFunctionException {
    finished = false;
    try {
      proofDigest = MessageDigest.getInstance(algHash);
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
  private boolean verifySteps(int n) throws MemoryBoundFunctionException {
    //XXX
    return (false);
  }

  /**
   * Do "n" steps of the underlying hash or effort proof generation
   * @param n number of steps to move.
   * @return true if there is more work to do
   * 
   */
  private boolean generateSteps(int n) throws MemoryBoundFunctionException {
    if (finished)
      return (true);
    if (mbf == null) {
      // This is the beginning of a vote block.  Is it the first?
      if (index < 0) {
	// The first proof uses the nonce and the AU name [XXX and
	// also the first bytes of the AU].
	proofDigest.update(nonce);
	String AUid = cus.getArchivalUnit().getAUId();
	proofDigest.update(AUid.getBytes());
	// XXX should also hash first bytes of AU
	index = 0;
      } else {
	// Second or subsequent block - nonce is previous hash and
	// previous proof.
	proofDigest.update(thisHash);
	for (int i = 0; i < thisProof.length; i++)
	  proofDigest.update(intToByteArray(thisProof[i]));
	// NB - proof  and hash have been added to ArrayLists already
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
    // We have an MBF - has it generated a proof yet?
    if (thisProof == null) {
      // No proof yet - work on proof
      mbf.computeSteps(n);
      if (mbf.done()) {
	// Finished
	thisProof = mbf.result();
	proofs.add(index, thisProof);
      }
      bytesToGo = (1 << index);
    } else if (thisHash == null) try {
      int bytesHashed = hasher.hashStep(bytesToGo > n ? n : bytesToGo);
      bytesToGo -= bytesHashed;
      if (bytesToGo <= 0) {
	// Extract the hash of this block and reset hasher for next
	thisHash = contentDigest.digest();
	hashes.add(index, thisHash);
      }
    } catch (IOException ex) {
      throw new MemoryBoundFunctionException("content hash threw " +
					     ex.toString());
    }
    return (finished);
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
