/*
 * $Id: MemoryBoundFunctionFactory.java,v 1.1 2003-08-11 18:44:52 dshr Exp $
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
import java.security.NoSuchAlgorithmException;
import org.lockss.util.*;

/**
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class MemoryBoundFunctionFactory {
  private static String[] names = {
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

  /**
   * Public constructor for an object that creates MemoryBoundFunction
   * objects of the requested implementation.
   * @param impl A String specifying the implementation to use.
   * @throws NoSuchAlgorithmException if no such implementation
   */
  public MemoryBoundFunctionFactory(String impl)
    throws NoSuchAlgorithmException {
    classToUse = null;
    implToUse = null;
    for (int i = 0; i < names.length; i++) {
      if (names[i].equals(impl))
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
   * Returns an instance of the type of MBF indicated by "version"
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param sVal an array of ints containing the proof
   * @param maxPathVal maximum number of steps to verify
   * @throws MemoryBoundFunctionException failure to create object
   * @throws NoSuchAlgorithmException no such implementation
   */
  public MemoryBoundFunction make(byte[] nVal,
				  int eVal,
				  int lVal,
				  int[] sVal,
				  long  maxPathVal)
    throws NoSuchAlgorithmException, MemoryBoundFunctionException {
    MemoryBoundFunction ret = null;
    try {
      if (classToUse == null)
	throw new NoSuchAlgorithmException();
      MemoryBoundFunctionSPI spi =
	(MemoryBoundFunctionSPI) classToUse.newInstance();
      ret = new MemoryBoundFunction(spi, nVal, eVal, lVal, sVal, maxPathVal);
    } catch (InstantiationException ex) {
      throw new MemoryBoundFunctionException(implToUse + ": " + ex.toString());
    } catch (IllegalAccessException ex) {
      throw new MemoryBoundFunctionException(implToUse + ": " + ex.toString());
    }
    return (ret);
  }

}
