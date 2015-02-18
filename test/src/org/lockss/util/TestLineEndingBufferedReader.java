/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.LockssTestCase;

public class TestLineEndingBufferedReader extends LockssTestCase {

  public void testEmptyInput() throws Exception {
    LineEndingBufferedReader r = new LineEndingBufferedReader(new StringReader(""));
    assertNull(r.readLine());
    assertNull(r.readLine());
  }
  
  public void testReadLineAcrossRefills() throws Exception {
    LineEndingBufferedReader r = new LineEndingBufferedReader(new StringReader("aaaabbbbcc"), 4);
    assertEquals(0, r.getLineCount());
    assertEquals("aaaabbbbcc", r.readLine());
    assertEquals(1, r.getLineCount());
    assertNull(r.readLine());
    assertNull(r.readLine());
    assertEquals(1, r.getLineCount());
  }
  
  public void testNewline() throws Exception {
    LineEndingBufferedReader r = new LineEndingBufferedReader(new StringReader("aaa\n" + "bb\nc" + "cccc" + "\nd\n"), 4);
    assertEquals(0, r.getLineCount());
    assertEquals("aaa\n", r.readLine()); // line ending at end of buffer
    assertEquals(1, r.getLineCount());
    assertEquals("bb\n", r.readLine()); // line ending not at end of buffer
    assertEquals(2, r.getLineCount());
    assertEquals("ccccc\n", r.readLine()); // line ending across refills
    assertEquals(3, r.getLineCount());
    assertEquals("d\n", r.readLine()); // line ending followed by EOF
    assertEquals(4, r.getLineCount());
    assertNull(r.readLine());
    assertNull(r.readLine());
    assertEquals(4, r.getLineCount());
  }
  
  public void testCarriageReturn() throws Exception {
    LineEndingBufferedReader r = new LineEndingBufferedReader(new StringReader("aaa\r" + "bb\rc" + "cccc" + "\rd\r"), 4);
    assertEquals(0, r.getLineCount());
    assertEquals("aaa\r", r.readLine()); // line ending at end of buffer
    assertEquals(1, r.getLineCount());
    assertEquals("bb\r", r.readLine()); // line ending not at end of buffer
    assertEquals(2, r.getLineCount());
    assertEquals("ccccc\r", r.readLine()); // line ending across refills
    assertEquals(3, r.getLineCount());
    assertEquals("d\r", r.readLine()); // line ending followed by EOF
    assertEquals(4, r.getLineCount());
    assertNull(r.readLine());
    assertNull(r.readLine());
    assertEquals(4, r.getLineCount());
  }
  
  public void testCarriageReturnNewline() throws Exception {
    LineEndingBufferedReader r = new LineEndingBufferedReader(new StringReader("aa\r\n" + "b\r\nc" + "ccc\r" + "\nddd" + "dddd" + "\r\nee" + "e\r\n"), 4);
    assertEquals(0, r.getLineCount());
    assertEquals("aa\r\n", r.readLine()); // line ending at end of buffer
    assertEquals(1, r.getLineCount());
    assertEquals("b\r\n", r.readLine()); // line ending not at end of buffer
    assertEquals(2, r.getLineCount());
    assertEquals("cccc\r\n", r.readLine()); // line ending across buffer refill
    assertEquals(3, r.getLineCount());
    assertEquals("ddddddd\r\n", r.readLine()); // line ending across refills
    assertEquals(4, r.getLineCount());
    assertEquals("eee\r\n", r.readLine()); // line ending followed by EOF
    assertEquals(5, r.getLineCount());
    assertNull(r.readLine());
    assertNull(r.readLine());
    assertEquals(5, r.getLineCount());
  }
  
  public void testConsecutiveLineEndings() throws Exception {
    LineEndingBufferedReader r = new LineEndingBufferedReader(new StringReader("aa\r\rbb\n\n" + "cc\r\n\r\n"), 8);
    assertEquals(0, r.getLineCount());
    assertEquals("aa\r", r.readLine()); // "\r" followed by "\r"
    assertEquals(1, r.getLineCount());
    assertEquals("\r", r.readLine());
    assertEquals(2, r.getLineCount());
    assertEquals("bb\n", r.readLine()); // "\n" followed by "\n"
    assertEquals(3, r.getLineCount());
    assertEquals("\n", r.readLine());
    assertEquals(4, r.getLineCount());
    assertEquals("cc\r\n", r.readLine()); // "\r\n" followed by "\r\n"
    assertEquals(5, r.getLineCount());
    assertEquals("\r\n", r.readLine());
    assertEquals(6, r.getLineCount());
    assertNull(r.readLine());
    assertNull(r.readLine());
    assertEquals(6, r.getLineCount());
  }
  
  public void testNormalRead() throws Exception {
    LineEndingBufferedReader r = new LineEndingBufferedReader(new StringReader("0123456789"), 4);
    char[] ch3 = new char[3];
    // Try to get 3 characters
    assertEquals(3, r.read(ch3)); // consumes 3 characters, leaves 1 in buffer
    assertEquals("012", new String(ch3));
    // Try to get 3 characters
    assertEquals(1, r.read(ch3)); // consumes last character, doesn't attempt more
    assertEquals("3", new String(ch3, 0, 1));
    assertEquals(2, r.read(ch3, 1, 2)); // refills, consumes 2 characters, leaves 2 in buffer
    assertEquals("45", new String(ch3, 1, 2));
    // Try to get 3 characters
    assertEquals(2, r.read(ch3)); // consumes last 2 characters, doesn't attempt more
    assertEquals("67", new String(ch3, 0, 2));
    assertEquals(1, r.read(ch3, 2, 1)); // refills, consumes 1 character, leaves 1 in buffer before EOF
    assertEquals("8", new String(ch3, 2, 1));
    // Try to get 3 characters
    assertEquals(1, r.read(ch3)); // consumes last character, reaches EOF
    assertEquals("9", new String(ch3, 0, 1));
    assertEquals(-1, r.read(ch3)); // EOF
    assertEquals(-1, r.read(ch3)); // EOF again
  }
  
  public void testClose() throws Exception {
    LineEndingBufferedReader r = new LineEndingBufferedReader(new StringReader("aaa\nbbb\n"));
    assertEquals("aaa\n", r.readLine());
    r.close();
    try {
      r.readLine();
      fail("Should have thrown \"stream closed\"");
    }
    catch (IOException expected) {
      assertEquals("stream closed", expected.getMessage());
    }
    try {
      char[] ch4 = new char[4];
      r.read(ch4);
      fail("Should have thrown \"stream closed\"");
    }
    catch (IOException expected) {
      assertEquals("stream closed", expected.getMessage());
    }
    r.close(); // shouldn't throw
  }
  
  /**
   * <p>
   * The documentation of {@link LineNumberReader} (a subclass of
   * {@link BufferedReader}) that counts lines as it goes) is unclear about
   * whether {@link LineNumberReader#getLineNumber()} means the number of the
   * line that is about to be read or the number of the line that was most
   * recently read. It turns out it is the latter. In addition to documenting
   * {@link LineEndingBufferedReader} as such, the corresponding method is named
   * {@link LineEndingBufferedReader#getLineCount()} to make it a little
   * clearer.
   * </p>
   * 
   * @throws Exception
   *           if any exception occurs
   * @since 1.66
   */
  public void testAssumptionsAboutLineNumberReader() throws Exception {
    java.io.LineNumberReader lnr = null;

    // With final line ending
    lnr = new LineNumberReader(new StringReader("aaa\n"));
    assertEquals(0, lnr.getLineNumber());
    assertEquals("aaa", lnr.readLine());
    assertEquals(1, lnr.getLineNumber());
    assertEquals(null, lnr.readLine());
    assertEquals(1, lnr.getLineNumber());

    // With no final line ending
    lnr = new LineNumberReader(new StringReader("aaa"));
    assertEquals(0, lnr.getLineNumber());
    assertEquals("aaa", lnr.readLine());
    assertEquals(1, lnr.getLineNumber());
    assertEquals(null, lnr.readLine());
    assertEquals(1, lnr.getLineNumber());
  }
  
  public void testGetLineCount() throws Exception {
    LineEndingBufferedReader r = null;

    // With final line ending
    r = new LineEndingBufferedReader(new StringReader("aaa\n"));
    assertEquals(0, r.getLineCount());
    assertEquals("aaa\n", r.readLine());
    assertEquals(1, r.getLineCount());
    assertEquals(null, r.readLine());
    assertEquals(1, r.getLineCount());

    // With no final line ending
    r = new LineEndingBufferedReader(new StringReader("aaa"));
    assertEquals(0, r.getLineCount());
    assertEquals("aaa", r.readLine());
    assertEquals(1, r.getLineCount());
    assertEquals(null, r.readLine());
    assertEquals(1, r.getLineCount());
  }

}
