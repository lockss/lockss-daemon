/*
 * $Id: MemoryBoundFunctionVote.java,v 1.1 2003-08-11 18:44:52 dshr Exp $
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
import java.util.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public abstract class MemoryBoundFunctionVote {
  protected static Logger logger = Logger.getLogger("MemoryBoundFunction");
  protected static byte[] basis = null;
  protected static File basisFile = null;

  protected byte[] nonce;
  protected int e;
  protected ArrayList proofs;  // for each block has array of proofs
  protected ArrayList hashes;  // for each block has array of hashes
  protected CachedUrlSet cus;
  protected MemoryBoundFunction mbf;
  protected boolean verify;
  protected boolean finished;
  protected int numBlocks;
  protected MemoryBoundFunctionFactory factory;

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
   * @param name String name of MBF implementaiton to use
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param cus the CachedUrlSet containing the content to be voted on
   *
   */
  public MemoryBoundFunctionVote(MemoryBoundFunctionFactory fact,
				 byte[] nVal,
				 int eVal,
				 CachedUrlSet cusVal)
    throws MemoryBoundFunctionException {
    if (fact == null)
      throw new MemoryBoundFunctionException("no factory");
    setup(fact, nVal, eVal, cusVal);
    proofs = new ArrayList();
    hashes = new ArrayList();
    verify = false;
  }

  /**
   * Public constructor for an object that will verify a vote
   * using hashing and memory bound functions.  It accepts as
   * input a nonce,  a cachedUrlSet,  and arrays of proofs
   * and hashes.
   * @param name String name of MBF implementation to use
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param cus the CachedUrlSet containing the content to be voted on
   * @param sVals the starting points chosen by the prover for each block
   * @param hashes the hashes of each block
   * 
   */
  public MemoryBoundFunctionVote(MemoryBoundFunctionFactory fact,
				 byte[] nVal,
				 int eVal,
				 CachedUrlSet cusVal,
				 int[][] sVals,
				 byte[][] hashVals)
    throws MemoryBoundFunctionException {
    if (fact == null)
      throw new MemoryBoundFunctionException("no factory");
    setup(fact, nVal, eVal, cusVal);
    verify = true;
    if (sVals.length != hashVals.length)
      throw new MemoryBoundFunctionException("length mismatch");
    numBlocks = sVals.length;
    proofs = new ArrayList(numBlocks);
    hashes = new ArrayList(numBlocks);
    for (int i = 0; i < numBlocks; i++) {
      proofs.add(i, sVals[i]);
      hashes.add(i, hashVals[i]);
    }
  }

  private void setup(MemoryBoundFunctionFactory fact,
		     byte[] nVal, int eVal, CachedUrlSet cus) {
    factory = fact;
    nonce = nVal;
    e = eVal;
    finished = false;
    mbf = null;
  }

  /**
   * Do "n" steps of the underlying hash or effort proof
   * @param n number of steps to perform
   * @return true if there is more work to do
   * 
   */
  public abstract boolean computeSteps(int n)
    throws MemoryBoundFunctionException;

  /**
   * Obtain the array of proof values that form part of the vote.
   * @return null if vote generation hasn't finished, else the array of proofs
   */
  public int[][] getProofArray() {
    int[][] ret = null;
    if (verify || finished) {
      ret = (int[][]) proofs.toArray();
    }
    return (ret);
  }

  /**
   * Obtain the array of hashes that form part of the vote.
   * @return null if vote generation hasn't finished, else the array of hashes
   */
  public byte[][] getHashArray() {
    byte[][] ret = null;
    if (verify || finished) {
      ret = (byte[][]) hashes.toArray();
    }
    return (ret);
  }
}
