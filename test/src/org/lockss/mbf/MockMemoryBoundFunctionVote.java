/*
 * $Id$
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
import java.util.*;
import java.security.*;
import java.io.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.protocol.*;

/**
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class MockMemoryBoundFunctionVote extends MemoryBoundFunctionVote {
  private byte[][] mockHashes;
  private int[][] mockProofs;
  private boolean[] mockVerifies;
  private int stepLimit;

  /**
   * No-argument constructor for use with Class.newInstance().
   */
  public MockMemoryBoundFunctionVote() {
    mockHashes = null;
    mockProofs = null;
    mockVerifies = null;
    stepLimit = 0;
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
   * @param cusVal the CachedUrlSet containing the content to be voted on
   * @param pollID the byte array ID for the poll
   * @param voterID the PeerIdentity of the voter
   *
   */
  protected void setupGeneration(MemoryBoundFunctionFactory fact,
				 byte[] nVal,
				 int eVal,
				 CachedUrlSet cusVal,
				 byte[] pollID,
				 PeerIdentity voterID)
    throws MemoryBoundFunctionException {
    super.setupGeneration(fact, nVal, eVal, cusVal, pollID, voterID);
    setup(nVal, eVal, cusVal);
  }

  /**
   * Public constructor for an object that will verify a vote
   * using hashing and memory bound functions.  It accepts as
   * input a nonce,  a cachedUrlSet,  and arrays of proofs
   * and hashes.
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param cusVal the CachedUrlSet containing the content to be voted on
   * @param sVals the starting points chosen by the prover for each block
   * @param hashes the hashes of each block
   * @param pollID the byte array ID for the poll
   * @param voterID the PeerIdentity of the voter
   *
   */
  public void setupVerification(MemoryBoundFunctionFactory fact,
				byte[] nVal,
				int eVal,
				CachedUrlSet cusVal,
				int sVals[][],
				byte[][] hashes,
				byte[] pollID,
				PeerIdentity voterID)
    throws MemoryBoundFunctionException {
    super.setupVerification(fact, nVal, eVal, cusVal, sVals, hashes,
			    pollID, voterID);
    setup(nVal, eVal, cusVal);
  }

  private void setup(byte[] nVal, int eVal, CachedUrlSet cusVal) throws
    MemoryBoundFunctionException {
    finished = false;
  }

  /**
   * Do "n" steps of the underlying hash or effort proof generation
   * @param n number of steps to move.
   * @return true if there is more work to do
   *
   */
  public boolean computeSteps(int n) throws MemoryBoundFunctionException {
    stepLimit -= n;
    if (stepLimit <= 0) {
      logger.info("MockMemoryBoundFunctionVote: valid " + valid +
		  " agreeing " + agreeing);
      finished = true;
    }
    return (!finished);
  }

  protected void setStepCount(int steps) {
    stepLimit = steps;
  }
  protected void setHashes(byte[][] hshs) {
    mockHashes = hshs;
    for (int i = 0; i <mockHashes.length; i++)
      saveHash(i, mockHashes[i]);
  }
  protected void setProofs(int[][] prfs) {
    mockProofs = prfs;
    for (int i = 0; i < mockProofs.length; i++)
      saveProof(i , mockProofs[i]);
  }
  protected void setValid(boolean val) {
    valid = val;
  }
  protected void setAgreeing(boolean val) {
    agreeing = val;
  }
  protected void setFinished(boolean val) {
    finished = val;
  }
}
