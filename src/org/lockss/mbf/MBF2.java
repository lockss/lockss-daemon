/*
 * $Id: MBF2.java,v 1.1 2003-08-05 00:55:30 dshr Exp $
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

/**
 */
public class MBF2 extends MemoryBoundFunction {
  private static final String algRand = "SHA1PRNG";
  private static final String algHash = "SHA1";
  private static Random rand = null;
  // We use the notation of Dwork et al.,  modified as needed because
  // Java needs us to represent things as byte arrays.  Note that
  // we fold their m,S,R,t into the nonce.

  // Public static data shared by all instances.
  // The A0 array is 256 32-bit words (1024 bytes)
  private static byte[] A0 = null;
  private static final int sizeA = 1024;
  private static BigInteger a0 = null;
  // The T array is 4M 32-bit words (16M bytes)
  private static byte[] T = null;
  private static final int sizeT = 16*1024*1024;

  // Instance data
  private byte[] A;
  // Indices into A - NB word indices not byte indices
  private int i, j;
  // Index into T
  private int c;
  // Trial number
  private int k;
  // Internal version of e
  private int ourE;
  // Index in current path
  private int pathIndex;
  // Lowest bit set in hash of A after path finishes
  private int lowBit;
  // Hasher
  private MessageDigest hasher;
  // ArrayList holding the indices to be returned if generating
  private ArrayList ret;
  // Count of paths checked
  private int numPath;
  // log2(number of entries expected in result) - should be param
  private int n;


  /**
   * Public constructor for an object that will compute a proof
   * of effort using a memory-bound function technique as
   * dscribed in "On The Cost Distribution Of A Memory Bound Function"
   * David S. H. Rosenthal
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (roundup(log2(e), where e is the
   *     number of low-order zeros in a successful path)
   *
   */
  public MBF2(byte[] nVal, int eVal, int lVal)
    throws MemoryBoundFunctionException {
    super(nVal, eVal, lVal);
    ensureConfigured();
    setup();
    ret = new ArrayList();
    n = 3; // XXX
  }

  /**
   * Public constructor for an object that will verify a proof of effort.
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (roundup(log2(e), where e is the
   *     number of low-order zeros in a successful path)
   * @param sVal a one-entry array with the starting point chosen by the prover
   * 
   */
  public MBF2(byte[] nVal, int eVal, int lVal, int[] sVal, long maxPathVal)
    throws MemoryBoundFunctionException {
    super(nVal, eVal, lVal, sVal, maxPathVal);
    if (sVal.length == 0 || sVal.length > eVal)
      throw new MemoryBoundFunctionException("bad proof length " + sVal.length);
    ensureConfigured();
    setup();
    n = 4; // XXX
  }

  private boolean match() {
    return (lowBit >= (ourE - n));
  }

  /**
   * If there is no current path,  choose a starting point and set it
   * as the current path.  Move up to "n" steps along the current path.
   * At each step, if the set length of the path "l" is exhausted,
   * unset the current path and check for a match.  If a match is found,
   * set finished.  If no match and the object is verifying,  set
   * finished.
   * @param n number of steps to move.
   * @return true if there is more work to do.
   * 
   */
  public boolean computeSteps(int n) throws MemoryBoundFunctionException {
    // If there is no current try,  create one
    if (pathIndex < 0) {
      choosePath();
      createPath();
    }
    // Move up to "n" steps along the path
    while (pathIndex < pathLen && n-- > 0) {
      stepAlongPath();
    }
    // If the current path has ended,  see if there is a match
    if (pathIndex >= pathLen) {
      finishPath();
    }
    // Return true if there is more work to do.
    return (!finished);
  }

  // Return the 32-bit word at [i..(i+3)] in array arr.
  private int wordAt(byte[] arr, int i) throws MemoryBoundFunctionException {
    if ((i & 0x3) != 0 || i < 0 || i > (arr.length-4))
      throw new MemoryBoundFunctionException("bad index " + i + "  size " +
					     arr.length);
    return (arr[i] | arr[i+1]<<8 | arr[i+2]<<16 | arr[i+3]<<24);
  }

  // Set the 32-bit word at [i..(i+3)] in array arr to b
  private void setWordAt(byte[] arr, int i, int b) {
    arr[i] = (byte)(b & 0xff);
    arr[i+1] = (byte)((b >> 8) & 0xff);
    arr[i+2] = (byte)((b >> 16) & 0xff);
    arr[i+3] = (byte)((b >> 24) & 0xff);
  }

  // Choose the next path to try
  private void choosePath() throws MemoryBoundFunctionException {
    // Set k to the index of the next path to try
    if (verify) {
      // XXX - should choose paths in random order
      k = proof[numPath];
    } else {
      k++;
    }
  }

  // Path initialization
  private void createPath() throws MemoryBoundFunctionException {
    // Set up path index k
    i = 0;
    j = 0;
    lowBit = -1;
    // Hash the nonce and the try count - we can always assume the
    // hasher is reset because we always leave it that way
    hasher.update(nonce);
    hasher.update((byte)(k & 0xff ));
    hasher.update((byte)((k >> 8) & 0xff));
    hasher.update((byte)((k >> 16) & 0xff));
    hasher.update((byte)((k >> 24) & 0xff));
    byte[] hashOfNonceAndIndex = hasher.digest();
    byte[] B = new byte[sizeA];
    for (int p = 0; p < sizeA; )
      for (int q = 0; q < 16; ) // NB length of SHA1 = 160 bits not 128
	B[p++] = hashOfNonceAndIndex[q++];
    BigInteger b1 = new BigInteger(B);
    BigInteger b2 = b1.xor(a0);
    A = new byte[1024];
    byte[] ba = b2.toByteArray();
    for (int m = 0; m < sizeA; m++)
      if (m < ba.length)
	A[m] = ba[m];
      else
	A[m] = 0;
    c = wordAt(A, 0) & 0x00fffffc; // "Bottom" 22 bits of A
  }

  private int cyclicRightShift11(int a) {
    return ((a >>> 11) | ((a & 0x7ff) << 21));
  }

  // Path step
  private void stepAlongPath() throws MemoryBoundFunctionException {
    // update indices into A and wrap them
    i += 4; // i is a word index into a byte array
    i &= 0x3fc;
    j += wordAt(A, i);
    j &= 0x3fc;
    // logger.info("Step at " + c + " indices [" + i + "," + j + "]");
    // feed bits from T into A[i] and rotate them
    int tmp1 = wordAt(T, c);
    int tmp2 = cyclicRightShift11(wordAt(A, i) + tmp1);
    setWordAt(A, i, tmp2);
    // swap A[i] and A[j]
    tmp2 = wordAt(A, i);
    int tmp3 = wordAt(A, j);
    setWordAt(A, i, tmp3);
    setWordAt(A, j, tmp2);
    // update c
    c = (tmp1 ^ wordAt(A, (tmp2 + tmp3) & 0x3fc)) & 0x00fffffc;
    if (c < 0 || c > (T.length-4) || ( c & 0x3 ) != 0)
      throw new MemoryBoundFunctionException("bad c " + c + " T[" +
					     T.length + "]");
    pathIndex++;
  }

  // Path termination
  private void finishPath() {
    numPath++;
    // XXX actually only need to look at bottom 32 bits of A
    BigInteger hashOfA = new BigInteger(hasher.digest(A));
    lowBit = hashOfA.getLowestSetBit();
    logger.debug("Finish " + k + " at " + c + " lowBit " + lowBit +
		" >= " + ourE);
    if (verify) {
      // We are verifying - any mis-match means invalid.
      if (!match()) {
	// This is supposed to be a match but isn't,
	// verification failed.
	finished = true;
	proof = null;
      } else if (numPath >= maxPath || numPath >= proof.length) {
	// XXX should check inter-proof spaces too
	finished = true;
      } else
	pathIndex = -1;
    } else {
      // We are generating - accumulate matches
      if (match()) {
	// Its a match.
	ret.add(new Integer(k));
      }
      if (k >= e) {
	finished = true;
	Object[] tmp = ret.toArray();
	proof = new int[tmp.length];
	for (int i = 0; i < tmp.length; i++) {
	  proof[i] = ((Integer)tmp[i]).intValue();
	}
      } else
	pathIndex = -1;
    }
  }

  // Instance initialization
  private void setup() throws MemoryBoundFunctionException {
    A = null;
    i = -1;
    j= -1;
    c = -1;
    k = 0;
    ourE = 1;
    long tmp = (e < 0 ? -e : e);
    while (tmp != 1) {
      ourE++;
      tmp >>>= 1;
    }
    pathIndex = -1;
    numPath = 0;
    try {
      hasher = MessageDigest.getInstance(algHash);
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException(algHash + " not found");
    }
  }

  // Class initialization
  private void ensureConfigured() throws MemoryBoundFunctionException {
    try {
      logger.debug2("ensureConfigured " + basisFile.getPath() +
		     " length " + basisFile.length());
      FileInputStream fis = new FileInputStream(basisFile);
      if (A0 == null) {
	A0 = new byte[sizeA];
        int readSize = fis.read(A0);
	if (readSize != sizeA)
	  throw new MemoryBoundFunctionException(basisFile.getPath() +
						 " short read " + readSize);
	// We keep a second representation of A0 as a BigInteger
	a0 = new BigInteger(A0);
	// XXX
	if (a0 == null)
	  throw new MemoryBoundFunctionException("a0 is null");
      }
      if (T == null) {
	T = new byte[sizeT];
	int readSize = fis.read(T);
	if (readSize != sizeT)
	  throw new MemoryBoundFunctionException(basisFile.getPath() +
						 " short read " + readSize);
      }
    } catch (IOException e) {
      basis = null;
      throw new MemoryBoundFunctionException(basisFile.getPath() + " throws " +
					     e.toString());
    }
  }
}
