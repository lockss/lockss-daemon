/*
 * $Id$
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
import java.util.*;
import java.net.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.mail.TextMessage
 */
public class TestTextMessage extends LockssTestCase {

  public void testNull() throws IOException {
    TextMessage msg = new TextMessage();
    assertEquals("\n", msg.getBody());
  }

  public void testGetBody() throws IOException {
    TextMessage msg = new TextMessage();
    msg.addHeader("From", "me");
    msg.addHeader("To", "you");
    msg.addHeader("Subject", "topic");
    msg.setText("Message\ntext");
    String exp = "From: me\nTo: you\nSubject: topic\n\nMessage\ntext";
    assertEquals(exp, msg.getBody());
  }

  public void testWriteData() throws IOException {
    UnsynchronizedByteArrayOutputStream baos = new UnsynchronizedByteArrayOutputStream(128);
    TextMessage msg = new TextMessage();
    msg.addHeader("From", "me");
    msg.addHeader("To", "you");
    msg.setText("foo\nbar\rbaz\r\nzot.\n.\n.foo");
    msg.writeData(baos);
    assertEquals("From: me\nTo: you\n\nfoo\nbar\rbaz\r\nzot.\n.\n.foo",
		 baos.toString());
  }

}
