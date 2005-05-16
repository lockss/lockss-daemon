/*
 * $Id: TestStreamUtil.java,v 1.8 2005-05-16 21:37:33 tlipkis Exp $
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

import java.util.*;
import java.io.*;
import junit.framework.TestCase;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.StreamUtil
 */
public class TestStreamUtil extends LockssTestCase {
  public TestStreamUtil(String msg) {
    super(msg);
  }

  public void testCopyNullInputStream() throws IOException {
    OutputStream baos = new ByteArrayOutputStream(11);
    assertEquals(0, StreamUtil.copy(null, baos));
    String resultStr = baos.toString();
    baos.close();
    assertEquals("", baos.toString());
  }

  public void testCopyNullOutputStream() throws IOException {
    InputStream is = new StringInputStream("test string");
    assertEquals(0, StreamUtil.copy(is, null));
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

  public void testCopyInputStreamReturnsCount() throws IOException {
    InputStream is = new StringInputStream("test string");
    OutputStream baos = new ByteArrayOutputStream(11);
    assertEquals(11, StreamUtil.copy(is, baos));
    is.close();
    baos.close();
  }

  public void testCopyNullReader() throws IOException {
    Writer writer = new CharArrayWriter(11);
    assertEquals(0, StreamUtil.copy(null, writer));
    String resultStr = writer.toString();
    writer.close();
    assertEquals("", writer.toString());
  }

  public void testCopyNullWriter() throws IOException {
    Reader reader = new StringReader("test string");
    assertEquals(0, StreamUtil.copy(reader, null));
    reader.close();
  }

  public void testCopyReader() throws IOException {
    Reader reader = new StringReader("test string");
    Writer writer = new CharArrayWriter(11);
    assertEquals(11, StreamUtil.copy(reader, writer));
    reader.close();
    String resultStr = writer.toString();
    writer.close();
    assertTrue(resultStr.equals("test string"));
  }

  public void testReadBuf() throws IOException {
    int len = 12;
    byte[] buf = new byte[len];
    InputStream ins = new StringInputStream("01\0003456789abcdefghijklmnopq");
    StreamUtil.readBytes(ins, buf, len);
    byte[] exp = {'0', '1', 0, '3', '4', '5', '6', '7', '8', '9', 'a', 'b'};
    assertEquals(exp, buf);
  }

  public void testReadBufShortRead() throws Exception {
    byte[] snd1 = {'0', '1', 0, '3'};
    final int len = 12;
    final byte[] buf = new byte[len];
    PipedOutputStream outs = new PipedOutputStream();
    final InputStream ins = new PipedInputStream(outs);
    final Exception[] ex = {null};
    final int[] res = {0};
    Thread th = new Thread() {
	public void run() {
	  try {
	    res[0] = StreamUtil.readBytes(ins, buf, len);
	    StreamUtil.readBytes(ins, buf, len);
	  } catch (IOException e) {
	    ex[0] = e;
	  }
	}};
    th.start();
    outs.write(snd1);
    outs.close();
    th.join();    

    assertEquals(snd1.length, res[0]);
    assertEquals(null, ex[0]);
  }

  public void testReadBufMultipleRead() throws Exception {
    byte[] snd1 = {'0', '1', 0, '3'};
    byte[] snd2 = {'4', '5', '6', '7', '8', '9', 'a', 'b'};
    byte[] exp = {'0', '1', 0, '3', '4', '5', '6', '7', '8', '9', 'a', 'b'};
    final int len = exp.length;
    final byte[] buf = new byte[len];
    PipedOutputStream outs = new PipedOutputStream();
    final InputStream ins = new PipedInputStream(outs);
    final Exception[] ex = {null};
    final int[] res = {0};
    Thread th = new Thread() {
	public void run() {
	  try {
	    res[0] = StreamUtil.readBytes(ins, buf, len);
	  } catch (IOException e) {
	    ex[0] = e;
	  }
	}};
    th.start();
    outs.write(snd1);
    TimerUtil.guaranteedSleep(100);
    outs.write(snd2);
    outs.flush();
    th.join();    

    assertEquals(exp, buf);
    assertEquals(len, res[0]);
    assertNull(ex[0]);
    outs.close();
  }

  public void testCompareStreams() throws IOException {
    String s1 = "01\0003456789abcdefghijklmnopq";
    assertTrue(StreamUtil.compare(new StringInputStream(""),
				  new StringInputStream("")));
    assertTrue(StreamUtil.compare(new StringInputStream(s1),
				  new StringInputStream(s1)));
    assertFalse(StreamUtil.compare(new StringInputStream(s1),
				   new StringInputStream("")));
    assertFalse(StreamUtil.compare(new StringInputStream(""),
				   new StringInputStream(s1)));
    assertFalse(StreamUtil.compare(new StringInputStream(s1),
				   new StringInputStream(s1+"a")));
    assertFalse(StreamUtil.compare(new StringInputStream(s1+"a"),
				   new StringInputStream(s1)));
    assertFalse(StreamUtil.compare(new StringInputStream("foo"),
				   new StringInputStream("bar")));
  }


}
