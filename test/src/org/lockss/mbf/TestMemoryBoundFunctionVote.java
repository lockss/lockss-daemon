/*
 * $Id: TestMemoryBoundFunctionVote.java,v 1.1 2003-08-11 18:44:53 dshr Exp $
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
import java.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.util.*;

/**
 * JUnitTest case for class: org.lockss.mbf.MemoryBoundFunctionVote and
 * its implementations.
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class TestMemoryBoundFunctionVote extends LockssTestCase {
  private static Logger log = null;
  private static Random rand = null;
  private static File f = null;
  private static byte[] basis;
  private int pathsTried;
  private static String[] names = {
    "MOCK",
    "MBF1",
    "MBF2",
  };
  private static MemoryBoundFunctionFactory[] factory = null;

  protected void setUp() throws Exception {
    super.setUp();
    log = Logger.getLogger("TestMemoryBoundFunction");
    if (false)
      rand = new Random(100);
    else 
      rand = new Random(System.currentTimeMillis());
    if (f == null) {
      f = FileUtil.tempFile("mbf1test");
      FileOutputStream fis = new FileOutputStream(f);
      basis = new byte[16*1024*1024 + 1024];
      rand.nextBytes(basis);
      fis.write(basis);
      fis.close();
      log.info(f.getPath() + " bytes " + f.length() + " long created");
    }
    if (!f.exists())
      fail(f.getPath() + " doesn't exist");
    MemoryBoundFunction.configure(f);
    if (factory == null) {
      factory = new MemoryBoundFunctionFactory[names.length];
      for (int i = 0; i < factory.length; i++) {
	try {
	  factory[i] = new MemoryBoundFunctionFactory(names[i]);
	} catch (NoSuchAlgorithmException ex) {
	  fail(names[i] + " threw " + ex.toString());
	}
      }
    }
  }

  /** tearDown method for test case
   * @throws Exception if XXX
   */
  public void tearDown() throws Exception {
    super.tearDown();
  }

  /*
   * Test construct
   * XXX - change to a Factory for MBFVs
   */
  public void testConstructors() {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    CachedUrlSet cus = new MockCachedUrlSet();
    for (int i = 0; i < factory.length; i++) {
      try {
	MemoryBoundFunctionVote mbfv =
	  new MBFV2(factory[i], nonce, 3, cus);
      } catch (MemoryBoundFunctionException ex) {
	fail("factory for " + names[i] + " threw " + ex.toString());
      }
    }
    int[][] sVals = { { 1, 2 } };
    byte[][] hashVals = new byte[1][];
    hashVals[0] = nonce;
    for (int i = 0; i < factory.length; i++) {
      try {
	MemoryBoundFunctionVote mbfv =
	  new MBFV2(factory[i], nonce, 3, cus, sVals, hashVals);
      } catch (MemoryBoundFunctionException ex) {
	fail("factory for " + names[i] + " threw " + ex.toString());
      }
    }
  }

}
