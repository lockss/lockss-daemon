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

import org.lockss.util.*;

/**
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class MemoryBoundFunctionFactory {
  protected static Logger logger = Logger.getLogger("MemoryBoundFunction");

  /*
   * The following arrays must be edited whenever a new implementation
   * is introduced.  This should be an infrequent event.
   */
  private static String[] implNames = {
    "MBF1",
    "MBF2",
    "MOCK"
  };
  private static String[] impls = {
    "org.lockss.mbf.MBF1",
    "org.lockss.mbf.MBF2",
    "org.lockss.mbf.MockMemoryBoundFunction"
  };
  private Class classToUse;
  private String implToUse;
  private byte[] A0;
  private byte[] T;

  /**
   * Public constructor for an object that creates MemoryBoundFunction
   * objects of the requested implementation.
   * @param impl A String specifying the implementation to use.
   * @param A0array The A0 basis array
   * @param Tarray The T basis array
   * @throws NoSuchAlgorithmException if no such implementation
   */
  public MemoryBoundFunctionFactory(String impl, byte[] A0array, byte[] Tarray)
    throws NoSuchAlgorithmException,
	   ClassNotFoundException,
	   MemoryBoundFunctionException {
    classToUse = null;
    implToUse = null;
    for (int i = 0; i < implNames.length; i++) {
      if (implNames[i].equals(impl))
	implToUse = impls[i];
    }
    if (implToUse != null){
      // Found it
      if (A0array == null || Tarray == null) {
	throw new MemoryBoundFunctionException("Array is null");
      }
      classToUse = Class.forName(implToUse);
      logger.info("factory for " + impl + " size " + A0array.length +
		  " / " + Tarray.length);
      A0 = A0array;
      T = Tarray;
    } else {
      throw new NoSuchAlgorithmException(impl);
    }
  }

  /**
   * Returns an instance of the type of MBF indicated by "version"
   * intended to generate a proof of effort.
   * @param nonceVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param lVal the effort sizer (length of each path)
   * @param nVal the proof density
   * @throws NoSuchAlgorithmException no such implementation
   * @throws InstantiationException XXX
   * @throws IllegalAccessException XXX
   * @throws MemoryBoundFunctionException could not initialize instance
   */
  public MemoryBoundFunction makeGenerator(byte[] nonceVal,
					   int eVal,
					   int lVal,
					   int nVal)
    throws NoSuchAlgorithmException,
	   InstantiationException,
	   IllegalAccessException,
	   MemoryBoundFunctionException {
    MemoryBoundFunction ret =
      makeVerifier(nonceVal, eVal, lVal, nVal, null, 0);
    return (ret);
  }

  /**
   * Returns an instance of the type of MBF indicated by "version"
   * intended to verify a proof of effort.
   * @param nonceVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param lVal the effort sizer (length of each path)
   * @param nVal the proof density
   * @param sVal an array of ints containing the proof
   * @param maxPathVal maximum number of steps to verify
   * @throws NoSuchAlgorithmException no such implementation
   * @throws InstantiationException XXX
   * @throws IllegalAccessException XXX
   * @throws MemoryBoundFunctionException could not initialize instance
   */
  public MemoryBoundFunction makeVerifier(byte[] nonceVal,
					  int eVal,
					  int lVal,
					  int nVal,
					  int[] sVal,
					  long  maxPathVal)
    throws NoSuchAlgorithmException,
	   InstantiationException,
	   IllegalAccessException,
	   MemoryBoundFunctionException  {
    MemoryBoundFunction ret = null;
    if (classToUse == null)
      throw new NoSuchAlgorithmException();
    ret = (MemoryBoundFunction) classToUse.newInstance();
    ret.initialize(nonceVal, eVal, lVal, nVal, sVal, maxPathVal, A0, T);
    return (ret);
  }

}
