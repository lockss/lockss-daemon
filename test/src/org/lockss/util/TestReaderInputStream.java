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

package org.lockss.util;
import java.io.*;
import org.lockss.test.*;

public class TestReaderInputStream extends LockssTestCase {

  public void testNullReaderInConstructor() {
    try {
      ReaderInputStream is = new ReaderInputStream(null);
      fail("Calling the constructor with a null Reader should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testEmptyReaderYieldsEmptyStream() throws IOException {
    Reader reader = new StringReader("");
    InputStream is = new ReaderInputStream(reader);
    assertEquals(-1, is.read());
  }

  public void testGetReader() throws IOException {
    Reader reader = new StringReader("");
    ReaderInputStream is = new ReaderInputStream(reader);
    assertSame(reader, is.getReader());
  }

  public void testBytifiedString() throws IOException {
    String testStr = "Test string";
    byte bytes[] = testStr.getBytes();
    Reader reader = new StringReader(testStr);
    InputStream is = new ReaderInputStream(reader);
    for (int ix = 0; ix < bytes.length; ix++) {
      assertEquals(bytes[ix], is.read());
    }
    assertEquals(-1, is.read());
  }

  public void testReadIntoArray() throws IOException {
    String testStr = "Test string";
    byte expected[] = testStr.getBytes();
    Reader reader = new StringReader(testStr);
    InputStream is = new ReaderInputStream(reader);
    byte actual[] = new byte[expected.length];
    assertEquals(expected.length, is.read(actual));
    assertEquals(expected, actual);
    assertEquals(-1, is.read());
  }

  public void testReadIntoArrayWithOffset() throws IOException {
    String testStr = "Test string";
    byte expected[] = testStr.getBytes();
    Reader reader = new StringReader(testStr);
    InputStream is = new ReaderInputStream(reader);
    byte actual[] = new byte[expected.length];
    assertEquals(5, is.read(actual, 2, 5));
    for (int ix = 0; ix < 5; ix++) {
      assertEquals(expected[ix], actual[ix+2]);
    }
    assertEquals(expected[5], is.read());
  }

  public void testClose() throws IOException {
    String testStr = "Test string";
    InstrumentedStringReader reader = new InstrumentedStringReader(testStr);
    InputStream is = new ReaderInputStream(reader);
    assertEquals('T', is.read());
    assertFalse(reader.isClosed());
    is.close();
    assertTrue(reader.isClosed());
    try {
      int c = is.read();
      fail("Stream shouldn't be readable after close()");
    } catch (IOException e) {
    }
  }

  public void test8859() throws Exception {
    compareBytes("a", "iso-8859-1");
    compareBytes("abc", "iso-8859-1");
    compareBytes("abcé", "iso-8859-1");
    compareWithWriter("abc", "iso-8859-1");
    compareEncodeDecode("abéèôfoo", "iso-8859-1");
  }

  public void testUtf8() throws Exception {
    compareBytes("a", "utf-8");
    compareBytes("abc", "utf-8");
    compareBytes("abcé", "utf-8");
    compareWithWriter("abc", "utf-8");
    compareEncodeDecode("abéèôfoo", "utf-8");
  }

  public void test16() throws Exception {
    compareBytes("a", "utf-16");
    compareWithWriter("a", "utf-16");
    // Encoding produced by UTF-16 depends on buffer sizes so can't compare
    // directly
    compareEncodeDecode("ab", "utf-16");
    compareEncodeDecode("abéèôfoo", "utf-16");
  }

  public void testReadZero() throws Exception {
    ReaderInputStream r = new ReaderInputStream(new StringReader("abc"));
    byte[] bytes = new byte[30];
    // First read in zero bytes
    r.read(bytes, 0, 0);
    // Now read in the string
    int readin = r.read(bytes, 0, 10);
    // Make sure that the counts are the same
    assertEquals("abc".getBytes().length, readin);
  }

  private void compareBytes(String s, String encoding) throws Exception {
    byte[] expected = s.getBytes(encoding);
        
    ReaderInputStream r = new ReaderInputStream(
						new StringReader(s), encoding);
    for (int i = 0; i < expected.length; ++i) {
      int expect = expected[i] & 0xFF;
      int read = r.read();
      if (expect != read) {
	fail("Mismatch in ReaderInputStream at index " + i
	     + " expecting " + expect + " got " + read + " for string "
	     + s + " with encoding " + encoding);
      }
    }
    if (r.read() != -1) {
      fail("Mismatch in ReaderInputStream - EOF not seen for string "
	   + s + " with encoding " + encoding);
    }
  }

  private void compareWithWriter(String s, String encoding) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Writer wrtr = new OutputStreamWriter(baos, encoding);
    wrtr.write(s);
    wrtr.flush();
    byte[] expected = baos.toByteArray();
    log.info("String: " + s + ", " + encoding + " writer len: " + expected.length);
        
    ReaderInputStream r = new ReaderInputStream(new StringReader(s), encoding);
    for (int i = 0; i < expected.length; ++i) {
      int expect = expected[i] & 0xFF;
      int read = r.read();
      if (expect != read) {
	fail("Mismatch in ReaderInputStream at index " + i
	     + " expecting " + expect + " got " + read + " for string "
	     + s + " with encoding " + encoding);
      }
    }
    if (r.read() != -1) {
      fail("Mismatch in ReaderInputStream - EOF not seen for string "
	   + s + " with encoding " + encoding);
    }
  }

  private void compareEncodeDecode(String s, String encoding) throws Exception {
    Reader rdr =
      new InputStreamReader(new ReaderInputStream(new StringReader(s),
						  encoding),
			    encoding);
    String s2 = StringUtil.fromReader(rdr);
    assertEquals(s, s2);
  }

}
