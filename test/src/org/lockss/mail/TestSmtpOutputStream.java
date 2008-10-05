/*
 * $Id: TestSmtpOutputStream.java,v 1.2 2005-10-06 08:21:55 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.mail;

import java.io.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.mail.SmtpOutputStream
 */
public class TestSmtpOutputStream extends LockssTestCase {

  /** Checks that SmtpOutputStream outputs expected string when input
   * string is written to it on chunks of size bufsize
   */
  void assertSmtpOutput(String exp, String in, int bufsize)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    SmtpOutputStream os = new SmtpOutputStream(baos);
    byte[] b = in.getBytes(Constants.DEFAULT_ENCODING);
    if (bufsize == 0) {
      for (int ix = 0; ix < b.length; ix++) {
	os.write(b[ix]);
      }
    } else {
      int len = b.length;
      for (int off = 0; off < len; off += bufsize) {
	os.write(b, off, Math.min(bufsize, len - off));
      }
    }
    os.flushSmtpData();
    assertEquals("bufsize = " + bufsize, exp + ".\r\n", baos.toString());
  }

  /** Checks that SmtpOutputStream outputs expected string when input
   * string is written to it with a variety of buffer sizes
   */
  void assertSmtpOutput(String exp, String in)
      throws IOException {
    assertSmtpOutput(exp, in, 0);
    for (int len = 0; len < in.length() + 1; len++) {
      assertSmtpOutput(exp, in, len);
    }
  }

  public void testXlate() throws IOException {
    assertSmtpOutput("\r\n", "");
    assertSmtpOutput("\r\n", "\n");
    assertSmtpOutput("\r\n", "\r");
    assertSmtpOutput("\r\n", "\r\n");
    assertSmtpOutput("\r\n\r\n", "\n\r");
    assertSmtpOutput("1\r\n", "1");
    assertSmtpOutput("12345\r\n", "12345");
    assertSmtpOutput("12\r\n345\r\n", "12\n345");
    assertSmtpOutput("12\r\n345\r\n", "12\r345");

    assertSmtpOutput("..\r\n", ".");
    assertSmtpOutput("..foo\r\n", ".foo");
    assertSmtpOutput("...foo\r\n", "..foo");
    assertSmtpOutput(" .foo\r\n", " .foo");
    assertSmtpOutput(" ..foo\r\n", " ..foo");
    assertSmtpOutput("foo.\r\n", "foo.");
    assertSmtpOutput("1.2\r\n..345\r\n", "1.2\r.345");
  }

}
