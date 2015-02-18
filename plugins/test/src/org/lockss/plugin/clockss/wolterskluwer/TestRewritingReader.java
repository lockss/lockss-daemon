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

package org.lockss.plugin.clockss.wolterskluwer;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.CharBuffer;

import org.apache.commons.io.IOUtils;
import org.lockss.test.LockssTestCase;
import org.lockss.util.Constants;
import org.lockss.util.LineRewritingReader;

public class TestRewritingReader extends LockssTestCase {

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
    LineRewritingReader rr = new StringRewritingReader("abc\n\n\ndef\n\n\nghi") {
      @Override
      public String rewriteLine(String line) {
        return line.toUpperCase();
      }
    };
    BufferedReader br = new BufferedReader(rr);
    assertEquals("ABC", br.readLine());
    assertEquals("", br.readLine());
    assertEquals("", br.readLine());
    assertEquals("DEF", br.readLine());
    assertEquals("", br.readLine());
    assertEquals("", br.readLine());
    assertEquals("GHI", br.readLine());
    assertEquals(null, br.readLine());
    assertEquals(null, br.readLine());
  }
  
  public void testLineTerminators() throws Exception {
    String thisPlatform = Constants.EOL;
    String otherPlatform = "\r".equals(thisPlatform) ? "\n" : "\r";
    LineRewritingReader rr = new StringRewritingReader(String.format("abc%sdef%s", otherPlatform, otherPlatform));
    
    char[] buf = new char[8];
    assertEquals(4, rr.read(buf));
    assertEquals(4, rr.read(buf, 4, 4));
    //FIX
    //assertEquals(String.format("abc%sdef%s", thisPlatform, thisPlatform), String.valueOf(buf));
    assertEquals(-1, rr.read(buf));
    assertEquals(-1, rr.read(buf));
  }
  
  public void testToleratesNullRewrite() throws Exception {
    LineRewritingReader rr = new StringRewritingReader("abc") {
      @Override
      public String rewriteLine(String line) {
        return null;
      }
    };
    char[] buf = new char[Constants.EOL.length()];
    //FIX
    //assertEquals(buf.length, rr.read(buf));
    //assertEquals(Constants.EOL, String.valueOf(buf));
    assertEquals(-1, rr.read(buf));
    assertEquals(-1, rr.read(buf));
  }
  
}
