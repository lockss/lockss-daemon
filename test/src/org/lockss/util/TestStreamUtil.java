/*
 * $Id: TestStreamUtil.java,v 1.3 2003-05-29 00:56:35 troberts Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.*;
import junit.framework.TestCase;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.ListUtil
 */
public class TestStreamUtil extends LockssTestCase {
  public TestStreamUtil(String msg) {
    super(msg);
  }

  public void testCopyNullInputStream() throws IOException {
    OutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(null, baos);
    String resultStr = baos.toString();
    baos.close();
    assertEquals("", baos.toString());
  }

  public void testCopyNullOutputStream() throws IOException {
    InputStream is = new StringInputStream("test string");
    StreamUtil.copy(is, null);
    is.close();
  }

  public void testCopyInputStream() throws IOException {
    InputStream is = new StringInputStream("test string");
    OutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(is, baos);
    is.close();
    String resultStr = baos.toString();
    baos.close();
    assertTrue(resultStr.equals("test string"));
  }

  public void testCopyNullReader() throws IOException {
    Writer writer = new CharArrayWriter(11);
    StreamUtil.copy(null, writer);
    String resultStr = writer.toString();
    writer.close();
    assertEquals("", writer.toString());
  }

  public void testCopyNullWriter() throws IOException {
    Reader reader = new StringReader("test string");
    StreamUtil.copy(reader, null);
    reader.close();
  }

  public void testCopyReader() throws IOException {
    Reader reader = new StringReader("test string");
    Writer writer = new CharArrayWriter(11);
    StreamUtil.copy(reader, writer);
    reader.close();
    String resultStr = writer.toString();
    writer.close();
    assertTrue(resultStr.equals("test string"));
  }
}
