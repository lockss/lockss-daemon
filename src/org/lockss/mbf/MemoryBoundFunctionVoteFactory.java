/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.security.NoSuchAlgorithmException;

import org.lockss.plugin.*;
import org.lockss.protocol.*;

/**
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class MemoryBoundFunctionVoteFactory {
  /*
   * The following arrays must be edited whenever a new implementation
   * is introduced.  This should be an infrequent event.
   */
  private static String[] implNames = {
    "MBFV2",
    "MOCK"
  };
  private static String[] impls = {
    "org.lockss.mbf.MBFV2",
    "org.lockss.mbf.MockMemoryBoundFunctionVote"
  };
  private Class classToUse;
  private String implToUse;

  /**
   * Public constructor for an object that creates MemoryBoundFunction
   * objects of the requested implementation.
   * @param impl A String specifying the implementation to use.
   * @throws NoSuchAlgorithmException if no such implementation
   */
  public MemoryBoundFunctionVoteFactory(String impl)
    throws NoSuchAlgorithmException {
    classToUse = null;
    implToUse = null;
    for (int i = 0; i < implNames.length; i++) {
      if (implNames[i].equals(impl))
	implToUse = impls[i];
    }
    if (implToUse != null){
      // Found it
      try {
	classToUse = Class.forName(implToUse);
      } catch (ClassNotFoundException ex) {
	throw new NoSuchAlgorithmException(implToUse);
      }
    } else {
      throw new NoSuchAlgorithmException(impl);
    }
  }

  /**
   * Returns an instance of the type of MBF vote selected by the
   * constructor suitable for generation:
   * @param fact a MemoryBoundFunctionFactory for use by the vote
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param cusVal the CachedUrlSet to be hashed as part of the vote
   * @param pollID the byte array ID of the poll
   * @param voterID the PeerIdentity of the voter
   * @throws MemoryBoundFunctionException failure to create object
   * @throws NoSuchAlgorithmException no such implementation
   */
  public MemoryBoundFunctionVote makeGenerator(MemoryBoundFunctionFactory fact,
					       byte[] nVal,
					       int eVal,
					       CachedUrlSet cusVal,
					       byte[] pollID,
					       PeerIdentity voterID)
    throws NoSuchAlgorithmException, MemoryBoundFunctionException {
    MemoryBoundFunctionVote ret = null;
    try {
      if (classToUse == null)
	throw new NoSuchAlgorithmException();
      ret = (MemoryBoundFunctionVote) classToUse.newInstance();
      ret.setupGeneration(fact, nVal, eVal, cusVal, pollID, voterID);
    } catch (InstantiationException ex) {
      throw new MemoryBoundFunctionException(implToUse + ": " + ex.toString());
    } catch (IllegalAccessException ex) {
      throw new MemoryBoundFunctionException(implToUse + ": " + ex.toString());
    }
    return (ret);
  }

  /**
   * Returns an instance of the type of MBF vote selected by the
   * constructor suitable for verification:
   * @param fact a MemoryBoundFunctionFactory for use by the vote
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param cusVal the CachedUrlSet to be hashed as part of the vote
   * @param sVals the array of arrray of ints forming the proof
   * @param hashVals the array of hashes
   * @param pollID the byte array ID of the poll
   * @param voterID the PeerIdentity of the voter
   * @throws MemoryBoundFunctionException failure to create object
   * @throws NoSuchAlgorithmException no such implementation
   */
  public MemoryBoundFunctionVote makeVerifier(MemoryBoundFunctionFactory fact,
					      byte[] nVal,
					      int eVal,
					      CachedUrlSet cusVal,
					      int[][] sVals,
					      byte[][] hashVals,
					      byte[] pollID,
					      PeerIdentity voterID)
    throws NoSuchAlgorithmException, MemoryBoundFunctionException {
    MemoryBoundFunctionVote ret = null;
    try {
      if (classToUse == null)
	throw new NoSuchAlgorithmException();
      ret = (MemoryBoundFunctionVote) classToUse.newInstance();
      ret.setupVerification(fact, nVal, eVal, cusVal, sVals, hashVals,
			    pollID, voterID);
    } catch (InstantiationException ex) {
      throw new MemoryBoundFunctionException(implToUse + ": " + ex.toString());
    } catch (IllegalAccessException ex) {
      throw new MemoryBoundFunctionException(implToUse + ": " + ex.toString());
    }
    return (ret);
  }

}
