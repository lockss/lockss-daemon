/*
 * $Id: MemoryBoundFunction.java,v 1.2 2003-07-23 02:58:20 dshr Exp $
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
import org.lockss.util.*;

/**
 */
public abstract class MemoryBoundFunction {
  protected static Logger logger = Logger.getLogger("MemoryBoundFunction");
  protected static byte[] basis = null;
  protected static File basisFile = null;

  protected byte[] nonce;
  protected long e;
  protected long arrayIndexStart;
  protected boolean verify;
  protected boolean finished;
  protected int pathLen;

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
  public MemoryBoundFunction(byte[] nVal, long eVal, int lVal) {
    setup(nVal, eVal, lVal);
    arrayIndexStart = -1;
  }

  /**
   * Public constructor for an object that will verify a proof of effort.
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param sVal the starting point chosen by the prover
   * 
   */
  public MemoryBoundFunction(byte[] nVal, long eVal, int lVal, long sVal) {
    setup(nVal, eVal, lVal);
    arrayIndexStart = sVal;
    verify = true;
  }

  private void setup(byte[] nVal, long eVal, int lVal) {
    nonce = nVal;
    e = eVal;
    pathLen = lVal;
    finished = false;
    verify = false;
  }

  /**
   * Obtain the result of the MBF.  If this object has not yet finished,
   * the result will be -1.  If this object is generating a proof,
   * the result will be an integer between 0 and the size of the basis file.
   * If this object is verifying a proof,  the result will be 0 if the
   * proof is valid, and >0 if it is invalid.
   */
  public long result() throws MemoryBoundFunctionException {
    long ret = -1;
    if (finished) {
      logger.debug2("result - finished");
      if (verify) {
	if (match())
	  ret = 0;
	else
	  ret = 1;
      } else {
	logger.debug2("result " + arrayIndexStart);
	ret = arrayIndexStart;
      }
    }
    logger.debug("result " + ret);
    return (ret);
  }

  /**
   * If there is no current path,  choose a starting point and set it
   * as the current path.  Move up to "n" steps along the current path.
   * At each step, if the set length of the path "l" is exhausted,
   * unset the current path and check for a match.  If a match is found,
   * set finished.
   * @param n number of steps to move.
   * 
   */
  public abstract boolean computeSteps(int n)
    throws MemoryBoundFunctionException;

  // Return true if the low log2(e) bits of value are zero.
  protected abstract boolean match() throws MemoryBoundFunctionException;

  // configuration
  protected static void configure() {
    // XXX get from Configuration
    // configure(new File("/dev/zero"), 1024);
  }

  // test-only configuration
  protected static void configure(File f) {
    basisFile = f;
    logger.debug("configuration file " + basisFile.getPath() +
		   " length " + basisFile.length());
  }
}
