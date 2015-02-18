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

package org.lockss.devtools;

import java.io.*;

public class TreeReadTest {

  public static void readFiles(File srcDir)
      throws FileNotFoundException, IOException {
    if (srcDir == null) {
      throw new IllegalArgumentException("Called with null source dir");
    } else if (!srcDir.isDirectory()) {
      throw new IllegalArgumentException("Called with src that isn't a directory: "+srcDir);
    }

    File children[] = srcDir.listFiles();
    for (int ix=0; ix<children.length; ix++) {
      if (children[ix].isFile()) {
	readFile(children[ix]);
      } else if (children[ix].isDirectory()) {
	readFiles(children[ix]);
      }
      children[ix] = null;
    }
  }

  public static long readFile(File src)
      throws FileNotFoundException, IOException {
    if (src == null) {
      throw new IllegalArgumentException("Called with null source file");
    } else if (!src.isFile()) {
      throw new IllegalArgumentException("Called with src that isn't a file");
    }
    Reader reader = new FileReader(src);
    FileInputStream fis = new FileInputStream(src);
    BufferedInputStream bis = new BufferedInputStream(fis);
    long ret = 0;
    byte buffer[] = new byte[1024*1024];
    for (int l; (l = bis.read(buffer)) > 0; ) {
      ret += l;
    }
    fis.close();
    return ret;

  }

  public static void main(String args[]) {
    String src = args[0];

    try {
      // Start timing
      long start = System.currentTimeMillis();
      // Read the tree
      readFiles(new File(src));
      // Stop timing
      long stop = System.currentTimeMillis();
      System.out.println((stop - start) + "ms");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  /*
   * Results on "blackbox":
   * OpenBSD: 255137 ms vs. 1240 ms
   * Fedora Core 3: 191936 ms vs 347 ms
   * Freesbie:
   * Results on "narses2":
   * Fedora Core 2: 59152 ms vs 130 ms
   */
}


