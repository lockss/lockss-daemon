/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import junit.framework.TestCase;
import org.lockss.util.*;

/** Functional tests to ensure that the filesystem behaves as we expect
 */
public class FuncFilesystem extends LockssTestCase {
  static final Logger log = Logger.getLogger("FuncFilesystem");

  // Running out of file descriptors should cause FileNotFoundException
  // with "Too many open files" in the message.
  public void testTooManyOpenFiles() throws IOException {
    int nfiles = 1;
    int nstreams = 100000;
    File[] files = new File[nfiles];
    for (int ix = 0; ix < nfiles; ix++) {
      files[ix] = FileTestUtil.writeTempFile("test", "foobar");
    }
    InputStream oneStream = new FileInputStream(files[0]);
    // Ensure IOUtil class is loaded as there may be no file descriptors
    // available when it's called below.  Happens when running from
    // unjarred .class files, e.g., during coverage tests.
    IOUtil.safeClose(oneStream);
    List lst = new LinkedList();
    int c;
    for (int ix = 0; ix < nfiles; ix++) {
      File file = files[ix];
	for (int iy = 0; iy < nstreams; iy++) {
	  int iter = ix * nfiles + iy + 1;
	  try {
	    InputStream in =new FileInputStream(file);
// 	    c = in.read();
	    lst.add(in);
	  } catch (FileNotFoundException e) {
	    log.debug("FileNotFoundException on iteration " + iter);
	    assertMatchesRE("Too many open files", e.getMessage());
	    closeStreams(lst);
	    return;
	  }
	}
    }
    fail("opened " + (nfiles * nstreams) + " streams without error");
    closeStreams(lst);
  }

  void closeStreams(List lst) {
    for (Iterator iter = lst.iterator(); iter.hasNext(); ) {
      IOUtil.safeClose((InputStream)iter.next());
    }
  }

}
