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
import java.io.*;
import java.security.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class MockMemoryBoundFunction extends MemoryBoundFunction {
  private long stepsDone;
  private long stepsToDo;
  private static byte[] mockNonce = null;
  private static int[] mockProof = null;

  /**
   * No-argument constructor for use with Class.newInstance()
   */
  protected MockMemoryBoundFunction() {
    stepsToDo = 0;
    stepsDone = 0;
  }

  /**
   * Initialize an object that will generate or verify a proof of effort
   * @param nonceVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param lVal the effort sizer (length of each path)
   * @param sVal an array of ints containing the proof
   * @param nVal the proof density
   * @param maxPathVal maximum number of steps to verify
   * @param A0array basis byte array A0
   * @param Tarray basis byte array T
   */
  protected void initialize(byte[] nonceVal,
			    long eVal,
			    int lVal,
			    int nVal,
			    int[] sVal,
			    long  maxPathVal,
			    byte[] A0array,
			    byte[] Tarray)
    throws MemoryBoundFunctionException {
    switch (Tarray.length) {
    case 16*1024*1024:
    case 1024*1024:
      if (A0array.length != 1024) {
	throw new MemoryBoundFunctionException(A0array.length + "/" +
					       Tarray.length + " bad length");
      }
      break;
    default:
      throw new MemoryBoundFunctionException(A0array.length + "/" +
					     Tarray.length + " bad length");
    }
    super.initialize(nonceVal, eVal, lVal, nVal, sVal, maxPathVal,
		     A0array, Tarray);
    stepsToDo = e * pathLen;
    if (verify) {
      if (proof == null || proof.length > e || proof.length == 0)
	throw new MemoryBoundFunctionException("bad proof");
      if (maxPath < 1)
	throw new MemoryBoundFunctionException("too few paths");
    }
    logger.debug("e " + e + " pathLen " + pathLen + " verify " +
		verify + " steps " + stepsToDo);
  }

  /**
   * If there is no current path,  choose a starting point and set it
   * as the current path.  Move up to "n" steps along the current path.
   * Set finished if appropriate.
   * @param n number of steps to move.
   * @return true if there is still work to do
   *
   */
  public boolean computeSteps(int n) throws MemoryBoundFunctionException {
    stepsDone += n;
    if (stepsDone >= stepsToDo) {
      finished = true;
      if (verify) {
	// We're verifying
	boolean match = (mockProof != null &&
			 mockProof.length == proof.length);
	if (mockNonce != null && !MessageDigest.isEqual(mockNonce, nonce))
	  match = false;
	for (int i = 0; i < mockProof.length; i++)
	  if (match && mockProof[i] != proof[i]) {
	    match = false;
	  } else {
	    logger.debug("proof check " + i + " " + mockProof[i] +
			" = " + proof[i]);
	  }
	if (!match)
	  proof = null;  // Proof invalid
	logger.debug("proof is " + (proof != null ? " valid" : "invalid"));
      } else {
	// We're generating
	proof = new int[mockProof.length];
	for (int i = 0; i < proof.length; i++)
	  proof[i] = mockProof[i];
      }
    }
    return (!finished);
  }

  /**
   * Set the proof to be returned
   * @param p an array of int containing the proof to be returned
   */
  public static void setProof(int[] p) {
    mockProof = p;
  }

  /**
   * Set the nonce to be compared with nonce supplied
   * @param p an array of byte containing the nonce
   */
  public static void setNonce(byte[] p) {
    mockNonce = p;
  }

}
