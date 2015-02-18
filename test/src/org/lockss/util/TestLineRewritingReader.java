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
import java.nio.CharBuffer;

import org.apache.commons.io.IOUtils;
import org.lockss.test.LockssTestCase;

public class TestLineRewritingReader extends LockssTestCase {

  public static class StringRewritingReader extends LineRewritingReader {
    
    public StringRewritingReader(String str) {
      super(new StringReader(str));
    }
    
    @Override
    public String rewriteLine(String line) {
      return line;
    }
    
  }

  public void testEmptyReader() throws Exception {
    // Also test that asking for input after EOF doesn't throw
    LineRewritingReader rr = null;
    char[] buf = new char[64];
    CharBuffer cb = CharBuffer.allocate(64);
    
    // Using read()
    try {
      rr = new StringRewritingReader("");
      assertEquals(-1, rr.read());
      assertEquals(-1, rr.read());
    }
    finally {
      IOUtils.closeQuietly(rr);
    }
    
    // Using read(char[])
    try {
      rr = new StringRewritingReader("");
      assertEquals(-1, rr.read(buf));
      assertEquals(-1, rr.read(buf));
    }
    finally {
      IOUtils.closeQuietly(rr);
    }

    // Using read(char[],int,int)
    try {
      rr = new StringRewritingReader("");
      assertEquals(-1, rr.read(buf, 12, 34));
      assertEquals(-1, rr.read(buf, 12, 34));
    }
    finally {
      IOUtils.closeQuietly(rr);
    }

    // Using read(CharBuffer)
    try {
      rr = new StringRewritingReader("");
      assertEquals(-1, rr.read(cb));
      assertEquals(-1, rr.read(cb));
    }
    finally {
      IOUtils.closeQuietly(rr);
    }
  }
  
  public void testUppercase() throws Exception {
    LineRewritingReader lrr = new StringRewritingReader("abc\n\n\ndef\n\n\nghi") {
      @Override
      public String rewriteLine(String line) {
        return line.toUpperCase();
      }
    };
    StringWriter sw = new StringWriter();
    IOUtils.copy(lrr, sw);
    assertEquals("ABC\n\n\nDEF\n\n\nGHI", sw.toString());
  }
  
  public void testSkipsNullLines() throws Exception {
    LineRewritingReader lrr = new StringRewritingReader("abc\ndef\nghi\n") {
      @Override
      public String rewriteLine(String line) {
        return "def\n".equals(line) ? null : line;
      }
    };
    StringWriter sw = new StringWriter();
    IOUtils.copy(lrr, sw);
    assertEquals("abc\nghi\n", sw.toString());
  }
  
  public void testClose() throws Exception {
    LineRewritingReader lrr = new StringRewritingReader("abc\ndef\nghi\n");
    char[] buf = new char[4];
    lrr.read(buf);
    lrr.close();
    try {
      lrr.read(buf);
      fail("Should have thrown \"stream closed\"");
    }
    catch (IOException expected) {
      assertEquals("stream closed", expected.getMessage());
    }
    lrr.close(); // shouldn't throw
  }
  
  public void testMaxLineLength() throws Exception {
    LineRewritingReader lrr = new LineRewritingReader(new StringReader("a\nbb\nccc\ndddd\n"), 4) {
      @Override
      public String rewriteLine(String line) {
        return line;
      }
    };
    char[] buf = new char[32];
    try {
      do {
        // no-op
      } while (lrr.read(buf) != -1);
      fail("Should have thrown because maximum line length exceeded");
    }
    catch (IOException expected) {
      // Expected
    }
  }
  
  public void testDoubleWrapping() throws Exception {
    class LineRewritingReader2 extends LineRewritingReader {
      public LineRewritingReader2(Reader underlyingReader) {
        super(underlyingReader);
      }
      public Reader getUnderlyingReader() {
        return underlyingReader;
      }
      @Override
      public String rewriteLine(String line) {
        return line;
      }
    }
    Reader underlyingSR = new StringReader("foo");
    System.out.println(new LineRewritingReader2(underlyingSR).getUnderlyingReader().getClass());
    assertTrue(new LineRewritingReader2(underlyingSR).getUnderlyingReader() instanceof LineEndingBufferedReader);
    LineEndingBufferedReader underlyingLEBR = new LineEndingBufferedReader(new StringReader("foo"));
    assertSame(underlyingLEBR, new LineRewritingReader2(underlyingLEBR).getUnderlyingReader());
  }
  
}
