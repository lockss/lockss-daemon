/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;
import java.io.*;
import java.security.*;

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.lockss.test.*;

public class TestHashedOutputStream extends LockssTestCase {

  private MockMessageDigest makeMessageDigest() {
    return new MockMessageDigest();
  }

  public void testNullArgumentsToConstructor() {
    MessageDigest md = makeMessageDigest();
    OutputStream out = new UnsynchronizedByteArrayOutputStream();
    try {
      new HashedOutputStream(null, md);
      fail("Calling the constructor with null OutputStream should throw");
    } catch (IllegalArgumentException e) {
    }
    try {
      new HashedOutputStream(out, null);
      fail("Calling the constructor with null MessageDigest should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  byte[] getBytes(String s) throws UnsupportedEncodingException {
    return s.getBytes(Constants.DEFAULT_ENCODING);
  }

  public void testWrite() throws IOException {
    UnsynchronizedByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream();
    MockMessageDigest md = makeMessageDigest();
    HashedOutputStream hos = new HashedOutputStream(out, md);
    hos.write('a');
    assertEquals(getBytes("a"), md.getUpdatedBytes());
    hos.write(getBytes("bcdefghijk"));
    hos.write(getBytes("1234567890"), 3, 4);
    assertEquals(getBytes("bcdefghijk4567"), md.getUpdatedBytes());
    assertEquals("abcdefghijk4567", out.toString());
  }

}
