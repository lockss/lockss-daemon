/*
 * $Id: HashSpeedTest.java,v 1.6 2006-07-11 17:42:24 thib_gc Exp $
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

package org.lockss.devtools;

import java.io.*;
import java.security.MessageDigest;

public class HashSpeedTest {

  public static long hashSingleFile(File src, MessageDigest hasher)
      throws FileNotFoundException, IOException {
    if (src == null) {
      throw new IllegalArgumentException("Called with null source file");
    } else if (!src.isFile()) {
      throw new IllegalArgumentException("Called with src that isn't a file");
    }
    long ret = 0;
    // Hash the content of src
    FileInputStream fis = new FileInputStream(src);
    byte buffer[] = new byte[1024*1024];
    for (int l; (l = fis.read(buffer)) > 0; ) {
      if (hasher != null)
	hasher.update(buffer, 0, l);
      ret += l;
    }
    return ret;
  }

  public static void main(String args[]) {
    String src = args[0];
    String hashAlgorithm = (args.length > 1 ? args[1] : "SHA1");

    try {
      MessageDigest hasher = MessageDigest.getInstance(hashAlgorithm);
      // Start timing
      long start = System.currentTimeMillis();
      // Hash the file
      long bytes = hashSingleFile(new File(src), null);
      // Stop timing
      long stop = System.currentTimeMillis();
      long delta1 = (stop - start);
      start = System.currentTimeMillis();
      // Hash the file
      bytes = hashSingleFile(new File(src), hasher);
      // Stop timing
      stop = System.currentTimeMillis();
      long delta2 = (stop - start);
      System.out.println(hashAlgorithm + " speed " +
			 (bytes / (delta2 - delta1)) + " byte/ms" +
			 " input speed " + (bytes / delta1) + " byte/ms");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  /*
   * Results on "blackbox" 1GHz Via, 512MB ram, file pre-read:
   * OpenBSD = 4713 bytes/ms vs. 23872 bytes/ms
   * Fedora Core 3 = 5904 bytes/ms vs. 22393 bytes/ms
   * Freesbie =
   *
   * Results on narses2:
   * Fedora Core 2 = 26111 bytes/ms vs. 61901 bytes/ms
   */
}

