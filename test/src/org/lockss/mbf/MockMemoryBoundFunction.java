/*
 * $Id: MockMemoryBoundFunction.java,v 1.3 2003-08-25 23:58:49 dshr Exp $
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
import java.security.NoSuchAlgorithmException.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class MockMemoryBoundFunction extends MemoryBoundFunctionSPI {
  private long stepsDone;
  private long stepsToDo;
  private static int[] mockProof = null;

  /**
   * No-argument constructor for use with Class.newInstance()
   */
  protected MockMemoryBoundFunction() {
    mbf = null;
    stepsToDo = 0;
    stepsDone = 0;
  }

  /**
   * Initialize an object that will generate or verify a proof of effort
   * David S. H. Rosenthal
   * @param wrapper the MemoryBoundFunction object being implemented
   */
  protected void initialize(MemoryBoundFunction wrapper)
    throws MemoryBoundFunctionException {
    mbf = wrapper;
    mbf.basisLength = 16*1024*1024;
    stepsToDo = mbf.e * mbf.pathLen;
    if (mbf.verify) {
      if (mbf.proof == null ||
	  mbf.proof.length == 0 ||
	  mbf.proof.length > mbf.e)
	throw new MemoryBoundFunctionException("bad proof");
      if (mbf.maxPath < 1)
	throw new MemoryBoundFunctionException("too few paths");
    }
    logger.info("e " + mbf.e + " pathLen " + mbf.pathLen + " verify " +
		mbf.verify + " steps " + stepsToDo);
  }

  /**
   * If there is no current path,  choose a starting point and set it
   * as the current path.  Move up to "n" steps along the current path.
   * Set finished if appropriate.
   * @param n number of steps to move.
   * 
   */
  public boolean computeSteps(int n) throws MemoryBoundFunctionException {
    stepsDone += n;
    if (stepsDone >= stepsToDo) {
      mbf.finished = true;
      if (mbf.verify) {
	// We're verifying
	boolean match = (mockProof != null &&
			 mockProof.length == mbf.proof.length);
	for (int i = 0; i < mockProof.length; i++)
	  if (match && mockProof[i] != mbf.proof[i])
	    match = false;
	if (!match)
	  mbf.proof = null;  // Proof invalid
      } else {
	// We're generating
	mbf.proof = new int[mockProof.length];
	for (int i = 0; i < mbf.proof.length; i++)
	  mbf.proof[i] = mockProof[i];
      }
    }
    return (!mbf.finished);
  }

  /**
   * Set the proof to be returned
   * @param p an array of int containing the proof to be returned
   */
  public static void setProof(int[] p) {
    mockProof = p;
  }
  
}
