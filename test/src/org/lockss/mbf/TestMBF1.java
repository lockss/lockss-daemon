/*
 * $Id: TestMBF1.java,v 1.1 2003-07-21 02:39:29 dshr Exp $
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
import org.lockss.util.*;

/** JUnitTest case for class: org.lockss.mbf.MBF1 */
public class TestMBF1 extends LockssTestCase {
  private static Logger log = null;
  private static Random rand = null;
  private static File f = null;
  private static byte[] basis;

  protected void setUp() throws Exception {
    super.setUp();
    log = Logger.getLogger("TestMBF1");
    rand = new Random(100);
    f = FileUtil.tempFile("mbf1test");
    FileOutputStream fis = new FileOutputStream(f);
    basis = new byte[512];
    rand.nextBytes(basis);
    fis.write(basis);
    fis.close();
    if (f.exists()) {
      log.debug(f.getPath() + " bytes " + f.length() + " long");
    } else
      log.debug2(f.getPath() + " doesn't exist");
    MemoryBoundFunction.configure(f, 32);
  }

  /** tearDown method for test case
   * @throws Exception if XXX
   */
  public void tearDown() throws Exception {
    super.tearDown();
    f.delete();
  }

  /** test for method configure(..) */
  public void testOnce() throws IOException {
    long verify = onePair(4);
    assertTrue(verify == 0);
  }

  public void testMultiple() throws IOException {
    for (int i = 0; i < 64; i++) {
      long verify = onePair(3);
      assertTrue(verify == 0);
    }
  }

  private long onePair(int e) throws IOException {
    byte[] nonce = new byte[64];
    rand.nextBytes(nonce);
    log.debug2("about to create mbf1");
    MemoryBoundFunction mbf = new MBF1(nonce, e);
    log.debug2("created");
    while (mbf.computeSteps(8)) {
      log.debug2("step");
      assertEquals(mbf.result(), -1);
    }
    long res = mbf.result();
    log.info("generate " + res);
    assertTrue(res >= 0 && res < basis.length);
    log.debug2("about to create mbf2");
    MemoryBoundFunction mbf2 = new MBF1(nonce, e, res);
    log.debug2("created");
    while (mbf2.computeSteps(8)) {
      log.debug2("step");
      assertEquals(mbf2.result(), -1);
    }
    long res2 = mbf2.result();
    log.info("verify " + res + " returns " + res2);
    return res2;
  }
}
