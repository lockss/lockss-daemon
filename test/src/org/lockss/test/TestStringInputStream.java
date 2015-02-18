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
import junit.framework.TestCase;

/**
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class TestStringInputStream extends LockssTestCase {

  public TestStringInputStream(String msg) {
    super(msg);
  }

  public void testRead() {
    StringInputStream sis = new StringInputStream("test");
    byte bytes[] = "test".getBytes();
    for (int ix=0; ix<bytes.length; ix++){
      assertEquals(bytes[ix], sis.read());
    }
    assertEquals(-1, sis.read());
  }

  public void testToString() {
    StringInputStream sis = new StringInputStream("test");
    assertEquals("[StringInputStream: test]", sis.toString());
  }

  public void testMarkSupported() {
    StringInputStream sis = new StringInputStream("test");
    assertTrue(sis.markSupported());
  }

  public void testReset() throws IOException {
    StringInputStream sis = new StringInputStream("test");
    byte bytes[] = "test".getBytes();
    for (int ix=0; ix<bytes.length; ix++){
      assertEquals(bytes[ix], sis.read());
    }
    sis.reset();
    for (int ix=0; ix<bytes.length; ix++){
      assertEquals(bytes[ix], sis.read());
    }
    assertEquals(-1, sis.read());
  }

  public void testMark() throws IOException {
    StringInputStream sis = new StringInputStream("test");
    sis.read();
    sis.mark(1024);
    byte bytes[] = "est".getBytes();
    for (int ix=0; ix<bytes.length; ix++){
      assertEquals(bytes[ix], sis.read());
    }
    sis.reset();
    for (int ix=0; ix<bytes.length; ix++){
      assertEquals(bytes[ix], sis.read());
    }
    assertEquals(-1, sis.read());
  }

}
