/*
 * $Id$
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

package org.lockss.filter;
import java.util.*;
import java.io.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestWhiteSpaceFilter extends LockssTestCase {


  /** Check that the filtered string matches expected.  Test with varying
   * buffer lengths */
  private void assertFilterString(String expected, String input)
      throws IOException {
    for (int len = 1; len <= input.length() * 2; len++) {
      Reader reader = new WhiteSpaceFilter(new StringReader(input));
      assertReaderMatchesString(expected, reader, len);
      assertEquals(-1, reader.read());
    }
    for (int len = 1; len <= input.length() * 2; len++) {
      Reader reader = new WhiteSpaceFilter(new StringReader(input));
      assertOffsetReaderMatchesString(expected, reader, len);
      assertEquals(-1, reader.read());
    }
    Reader reader = new WhiteSpaceFilter(new StringReader(input));
    assertReaderMatchesStringSlow(expected, reader);
    assertEquals(-1, reader.read());
  }

  public void testReadReturnsNegOneWhenEmpty() throws IOException {
    Reader reader = new WhiteSpaceFilter(new StringReader(""));
    char[] buf = new char[10];
    assertEquals(-1, reader.read());
    assertEquals(-1, reader.read(buf));
    reader = new WhiteSpaceFilter(new StringReader("Test  test"));
    assertEquals("Test test", StringUtil.fromReader(reader));
    assertEquals(-1, reader.read());
    assertEquals(-1, reader.read(buf));
  }

  public void testCollapseWhiteSpace() throws IOException {
    Reader reader = new WhiteSpaceFilter(new StringReader("Test  test"));
    assertFilterString("Test frob", "Test  frob");
    assertFilterString(" Test frob ", "   Test  frob   ");
  }

  public void testDoesntCollapseSingleSpace() throws IOException {
    assertFilterString("Test frob", "Test frob");
    assertFilterString(" Test frob ", " Test frob ");
  }

  public void testOtherChars() throws IOException {
    String testString = "Test   frob \t  \177      hop\n     skip";
    assertFilterString("Test frob hop skip", testString);
    assertFilterString("Test frob foo bar ", "Test frob\tfoo\b\tbar\n");
  }

  public void testTrailingWhitespace() throws IOException {
    assertFilterString(" Test frob jump ",
		       "  Test       frob\n     jump      ");
  }

  public void testClose() throws IOException {
    String testStr = "Test string";
    InstrumentedStringReader reader = new InstrumentedStringReader(testStr);
    Reader filt = new WhiteSpaceFilter(reader);
    assertEquals('T', filt.read());
    assertFalse(reader.isClosed());
    filt.close();
    assertTrue(reader.isClosed());
    try {
      int c = filt.read();
      fail("StringFilter shouldn't be readable after close()");
    } catch (IOException e) {
    }
  }

  public void testCloseAtEof() throws IOException {
    String testStr = "Test string";
    InstrumentedStringReader reader = new InstrumentedStringReader(testStr);
    Reader filt = new WhiteSpaceFilter(reader);
    assertReaderMatchesString("Test string", filt);
    assertFalse(reader.isClosed());
    filt.close();
    assertTrue(reader.isClosed());
    try {
      int c = filt.read();
      fail("StringFilter shouldn't be readable after close()");
    } catch (IOException e) {
    }
  }
}
