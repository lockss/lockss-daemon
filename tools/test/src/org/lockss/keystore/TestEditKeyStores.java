/*
 * $Id: TestEditKeyStores.java,v 1.1.2.2 2009-11-03 23:52:03 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.keystore;

import java.io.*;

import org.lockss.test.*;

public class TestEditKeyStores extends LockssTestCase {

  static String HOST1 = "host1.org";
  static String HOST2 = "host2.edu";

  static String SUFF_KS = ".jceks";
  static String SUFF_PASS = ".pass";

  // EditKeyStores uses some system utilities but doesn't run in a daemon
  // context, so is susceptible to inadvertent daemon dependencies in the
  // utilities.  Ensure that it runs without error.

  public void testEditKeyStores() throws Exception {
    File indir = new File(getTempDir(), "int");
    File outdir = new File(getTempDir(), "oot");
    EditKeyStores.setTestOnlySecureRandom(MiscTestUtil.getSecureRandom());
    String[] args = {"-i", indir.toString(),
		     "-o", outdir.toString(),
		     "-t",
		     HOST1, HOST2};
    EditKeyStores.main(args);
    // Check that files were created.  Should try to load them.
    assertTrue("Output dir " + outdir + " wasn't created", outdir.exists());
    assertFiles(outdir, HOST1);
    assertFiles(outdir, HOST2);
  }

  void assertFiles(File dir, String host) {
    File ks = new File(dir, host + SUFF_KS);
    File pass = new File(dir, host + SUFF_PASS);
    assertTrue("Keystore for " + host + " wasn't created", ks.exists());
    assertTrue("Password for " + host + " wasn't created", pass.exists());
  }

}

