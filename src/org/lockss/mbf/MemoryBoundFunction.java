/*
 * $Id: MemoryBoundFunction.java,v 1.12 2003-09-05 23:55:01 dshr Exp $
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
import java.io.*;
import java.security.NoSuchAlgorithmException;
import org.lockss.util.*;

/**
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public abstract class MemoryBoundFunction {
  protected static Logger logger = Logger.getLogger("MemoryBoundFunction");
  protected static File basisFile = null;
  protected static long basisLength = 0;

  protected byte[] nonce;
  protected long e;
  protected int[] proof;
  protected int[] signature;
  protected boolean verify;
  protected boolean finished;
  protected int pathLen;
  protected long maxPath;

  protected MemoryBoundFunction() {
    }

  /**
   * Initialize an instance - called by the factory
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param lVal the effort sizer (length of each path)
   * @param sVal an array of ints containing the proof
   * @param maxPathVal maximum number of steps to verify
   */
  protected void initialize(byte[] nVal,
			    long eVal,
			    int lVal,
			    int[] sVal,
			    long  maxPathVal)
    throws MemoryBoundFunctionException {
    setup(nVal, eVal, lVal);
    if (sVal == null) {
      // Generating
      verify = false;
      proof = null;
    } else {
      if (sVal.length > maxPathVal) {
	throw new MemoryBoundFunctionException("proof too long " +
					       sVal.length + " / " + maxPath);
      }
      proof = sVal;
      verify = true;
      maxPath = maxPathVal;
    }
    signature = null;
  }

  private void setup(byte[] nVal,
		     long eVal,
		     int lVal) {
    nonce = nVal;
    e = eVal;
    pathLen = lVal;
    finished = false;
  }

  /**
   * Return true if the proof generation or verification is finished.
   * @return true if the proof generation or verification is finished.
   */
  public boolean finished() {
    return (finished);
  }

  /**
   * Obtain the result of the MBF.  If this object has not yet finished,
   * throws MemoryBoundFunction Exception.  If this object is generating
   * a proof, the result will be an array of ints between 0 and the size
   * of the basis file.  If this object is verifying a proof,  the result
   * will be the proof array if the proof is valid, and null if it is invalid.
   * Generating an empty proof returns null,  validating null returns null
   * for invalid.
   * @return array of int containing the proof,  null if invalid
   * 
   */
  public int[] result() throws MemoryBoundFunctionException {
    if (!finished)
      throw new MemoryBoundFunctionException("not finished");
    return (proof);
  }

  /**
   * Obtain the signature of the MBF.  If this object has not yet finished,
   * throw MemoryBoundFunction Exception.
   * The array returned contains the next value to be fetched from the
   * basis array for each path included in the proof (for a non-empty
   * proof) or for each path searched (for an empty proof).
   * @return array of int containing the signature,  null if none available.
   * 
   */
  public int[] signatureArray() throws MemoryBoundFunctionException {
    if (!finished)
      throw new MemoryBoundFunctionException("not finished");
    return (signature);
  }

  /**
   * Return size of basis array.
   * @return size of the basis array
   */
  public static long basisSize() {
    return basisLength;
  }

  // configuration
  protected static void configure() {
    // XXX get from Configuration
    // configure(new File("/dev/zero"), 1024);
  }

  // test-only configuration
  protected static void configure(File f) {
    basisFile = f;
    basisLength = basisFile.length();
    logger.debug("configuration file " + basisFile.getPath() +
		   " length " + basisLength);
  }

  /**
   * If there is no current path,  choose a starting point and set it
   * as the current path.  Move up to "n" steps along the current path.
   * Set finished if appropriate.
   * @param n number of steps to move.
   * @return true if more work to do
   * 
   */
  public abstract boolean computeSteps(int n)
    throws MemoryBoundFunctionException;


}
