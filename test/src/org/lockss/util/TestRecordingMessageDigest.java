/*
 * $Id: TestRecordingMessageDigest.java,v 1.3 2006-11-27 06:33:35 tlipkis Exp $
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
import java.util.*;
import java.security.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestRecordingMessageDigest extends LockssTestCase {

  private MessageDigest dig;
  private RecordingMessageDigest rmd;
  private File file;

  protected void setUp() throws Exception {
    dig = MessageDigest.getInstance("SHA-1");
    file = FileTestUtil.tempFile("digrec");
    rmd = new RecordingMessageDigest(newDigest(), file);
  }

  MessageDigest newDigest() throws NoSuchAlgorithmException {
    return MessageDigest.getInstance("SHA-1");
  }

  void assertSameDigest(String input) {
    byte[] bytes = input.getBytes();
    dig.update(bytes);
    rmd.update(bytes);
    assertEquals(dig.digest(), rmd.digest());
  }

  public void testEquiv0() {
    assertSameDigest("");
  }
  public void testEquiv1() {
    assertSameDigest("foo");
  }
  public void testEquiv2() {
    assertSameDigest("sdlkjasd;fklasd;lkjasdf1239812349871234897");
  }

  public void testEquiv3() {
    dig.update((byte)47);
    rmd.update((byte)47);
    assertEquals(dig.digest(), rmd.digest());
  }

  public void testRecordFile() throws Exception {
    String testStr = "asdf;klsdf;lkajsdf;jl";
    rmd.update(testStr.getBytes());
    rmd.digest();
    assertEquals(testStr, StringUtil.fromFile(file));
  }

  public void testRecordStream() throws Exception {
    OutputStream outs = new BufferedOutputStream(new FileOutputStream(file));
    rmd = new RecordingMessageDigest(newDigest(), outs);
    String testStr = "asdf;klsdf;lkajsdf;jl";
    rmd.update(testStr.getBytes());
    rmd.digest();
    // make sure it didn't close the stream
    outs.write("append this".getBytes());
    outs.close();
    assertEquals(testStr + "append this", StringUtil.fromFile(file));
  }

  public void testMaxRecord0() throws Exception {
    rmd = new RecordingMessageDigest(newDigest(), file, 3);
    rmd.update((byte)'a');
    rmd.update((byte)'b');
    rmd.update((byte)'c');
    rmd.update((byte)'d');
    rmd.digest();
    assertEquals("abc", StringUtil.fromFile(file));
  }

  public void testMaxRecord1() throws Exception {
    rmd = new RecordingMessageDigest(newDigest(), file, 11);
    String testStr = "asdf;klsdf;lkajsdf;jl";
    rmd.update(testStr.getBytes());
    rmd.digest();
    assertEquals(testStr.substring(0,11), StringUtil.fromFile(file));
  }
}
