/*
 * $Id: MBF1.java,v 1.1 2003-07-21 02:39:29 dshr Exp $
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
public class MBF1 extends MemoryBoundFunction {
  private static final String algRand = "SHA1PRNG";
  private static final String algHash = "SHA1";
  private static Random rand = null;
  private static BigInteger bigArraySize = BigInteger.ZERO;
  private int pathIndex;
  private long arrayIndex;
  private MessageDigest hasher;

  /**
   * Public constructor for an object that will compute a proof
   * of effort using a memory-bound function technique due to
   * Cynthia Dwork, Andrew Goldberg and Moni Naor, "On Memory-
   * Bound Functions for Fighting Spam", in "Advances in Cryptology
   * (CRYPTO 2003)".
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   *
   */
  public MBF1(byte[] nVal, int eVal)
    throws MemoryBoundFunctionException {
    super(nVal, eVal);
    pathIndex = 0;
    arrayIndex = -1;
    ensureConfigured();
  }

  /**
   * Public constructor for an object that will verify a proof of effort.
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param sVal the starting point chosen by the prover
   * 
   */
  public MBF1(byte[] nVal, int eVal, long sVal)
    throws MemoryBoundFunctionException {
    super(nVal, eVal, sVal);
    pathIndex = 0;
    arrayIndex = sVal;
    ensureConfigured();
    logger.debug("verify " + arrayIndex);
  }

  protected boolean match() throws MemoryBoundFunctionException {
    // Turn the hasher into a BigInteger.
    try {
      MessageDigest h = (MessageDigest) hasher.clone();
      BigInteger b = new BigInteger(h.digest());
      // Find the lowest set bit.
      int lowBit = b.getLowestSetBit();
      logger.info("match " + b.toString(16) + " lowBit " + lowBit + " e " + e);
      return ((1 << lowBit) > e);
    } catch(CloneNotSupportedException e) {
      throw new MemoryBoundFunctionException(algHash + " threw " +
					     e.toString());
    }
  }

  /**
   * If there is no current path,  choose a starting point and set it
   * as the current path.  Move up to "n" steps along the current path.
   * At each step, if the set length of the path "l" is exhausted,
   * unset the current path and check for a match.  If a match is found,
   * set finished.  If no match and the object is verifying,  set
   * finished.
   * @param n number of steps to move.
   * 
   */
  public boolean computeSteps(int n) throws MemoryBoundFunctionException {
    logger.debug2("computeSteps(" + n + ") at " + arrayIndex);
    if (basis.length <= 0)
      throw new MemoryBoundFunctionException("no basis");
    // Is there a current path?
    if (arrayIndexStart < 0) {
      // No,  create one
      long r = rand.nextLong();
      if (r < 0) {
	arrayIndexStart = (-r) % basis.length;
      } else {
	arrayIndexStart = r % basis.length;
      }
      pathIndex = 0;
      arrayIndex = arrayIndexStart;
      logger.info("starting at index " + arrayIndex);
    }
    if (hasher == null) try {
      hasher = MessageDigest.getInstance(algHash);
      hasher.update(nonce);
      logger.debug2("new hasher");
    } catch (NoSuchAlgorithmException e) {
      hasher = null;
      throw new MemoryBoundFunctionException(algHash + " throws " +
					     e.toString());
    }
    // Move up to "n" steps along the path
    for (int i = 0; i < n && pathIndex < pathLen; i++) {
      // Do a step - update the hasher with the byte at arrayIndex
      hasher.update(basis[(int)arrayIndex]);
      try {
	// Then update arrayIndex to be the current result of the hash
	MessageDigest h = (MessageDigest) hasher.clone();
	BigInteger b = (new BigInteger(h.digest())).mod(bigArraySize);
	arrayIndex = b.longValue();
	pathIndex++;
	logger.info("step " + pathIndex + " at " + arrayIndex);
      } catch(CloneNotSupportedException e) {
	throw new MemoryBoundFunctionException(algHash + " threw " +
					       e.toString());
      }
    }
    // Have we finished a path?
    boolean matchHere = false;
    if (pathIndex >= pathLen) {
      // Yes - do we have a match?
      matchHere = match();
      if (matchHere || verify) {
	finished = true;
	logger.info("path ended at index " + pathIndex + " at " +
		       arrayIndex + (matchHere ? " match" : ""));
      } else {
	logger.info("path ended at index " + pathIndex + " at " + arrayIndex);
	arrayIndex = arrayIndexStart = -1;
	hasher = null;
	pathIndex = 0;
      }
    }
    return (!finished);
  }

  private void ensureConfigured() throws MemoryBoundFunctionException {
    logger.debug2("ensureConfigured");
    if (rand == null) try {
      rand = SecureRandom.getInstance(algRand);
      logger.debug2("new random");
    } catch (NoSuchAlgorithmException e) {
      rand = null;
      throw new MemoryBoundFunctionException(algRand + " throws " +
					     e.toString());
    }
    if (basis == null) try {
      logger.debug2("ensureConfigured " + basisFile.getPath() +
		     " length " + basisFile.length());
      FileInputStream fis = new FileInputStream(basisFile);
      basis = new byte[(int)basisFile.length()];
      fis.read(basis);
      bigArraySize = BigInteger.valueOf(basis.length);
      logger.debug2("new basis " + ((int)basisFile.length()) +
		     "/" + basis.length + " bytes");
    } catch (IOException e) {
      basis = null;
      throw new MemoryBoundFunctionException(basisFile.getPath() + " throws " +
					     e.toString());
    }
    hasher = null;
  }
}
