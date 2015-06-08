/*
 * $Id$
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.util.*;
import java.io.*;
import java.security.*;

import org.lockss.test.MockCrawler.MockCrawlerFacade;
import org.lockss.util.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;

/** Miscellaneous testing utilities */
public class MiscTestUtil {
  protected static Logger log = Logger.getLogger("MiscTestUtil");

  public static boolean hasPermission(List<PermissionChecker> checkers,
				      String page,
				      MockCrawlerFacade mcf)
      throws IOException {
    Reader rdr = null;
    for (PermissionChecker checker : checkers) {
      if (rdr == null) {
	rdr = newReader(page);
      }
      if (checker.checkPermission(mcf, rdr, "http://example.com/")) {
	return true;
      }
      try {
        rdr.reset();
      } catch(IOException ex) {
        //unable to reset for some reason
	rdr = null;			// force new one
      }
    }
    return false;
  }

  static Reader newReader(String page) throws IOException {
    InputStream in = new StringInputStream(page);
//     in = new StreamUtil.IgnoreCloseInputStream(in);

    Reader rdr = new InputStreamReader(in, Constants.ENCODING_UTF_8);
    int len = page.length() * 2;
    rdr = new BufferedReader(rdr, len);
//     rdr = new MonitoringReader(rdr, "foo", true);
    rdr.mark(len);
    return rdr;
  }


  // Return a SecureRandom that doesn't depend on kernel randomness.  Some
  // tests create a large number of SecureRandom instances; if each one
  // generates its own seed the kernel's entropy pool (/dev/random) gets
  // exhausted and the tests block while more is collected.  This can take
  // several minutes on an otherwise idle machine.

  public static SecureRandom getSecureRandom()
      throws NoSuchAlgorithmException, NoSuchProviderException {
    LockssRandom lrand = new LockssRandom();
    byte[] rseed = new byte[4];
    lrand.nextBytes(rseed);
    SecureRandom rng = SecureRandom.getInstance("SHA1PRNG", "SUN");
    rng.setSeed(rseed);
    return rng;
  }


}
