/*
 * $Id: MemoryBoundFunction.java,v 1.4 2003-08-04 21:36:05 dshr Exp $
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
  protected static byte[] basis = null;  // XXX move to implementation
  protected static File basisFile = null;

  protected byte[] nonce;
  protected long e;
  protected int[] proof;
  protected long arrayIndexStart;
  protected boolean verify;
  protected boolean finished;
  protected int pathLen;

  /**
   * Public constructor for an object that will compute a proof
   * of effort using a memory-bound function technique.
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   *
   */
  public MemoryBoundFunction(byte[] nVal, long eVal, int lVal) {
    setup(nVal, eVal, lVal);
    proof = null;
  }

  /**
   * Public constructor for an object that will verify a proof of effort.
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param sVal an array of ints containing the proof
   * 
   */
  public MemoryBoundFunction(byte[] nVal, long eVal, int lVal, int[] sVal) {
    setup(nVal, eVal, lVal);
    proof = sVal;
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
   * If there is no current path,  choose a starting point and set it
   * as the current path.  Move up to "n" steps along the current path.
   * Set finished if appropriate.
   * @param n number of steps to move.
   * 
   */
  public abstract boolean computeSteps(int n)
    throws MemoryBoundFunctionException;

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
