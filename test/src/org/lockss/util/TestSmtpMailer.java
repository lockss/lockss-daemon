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
import java.net.Socket;

/**
 * Test class for SmtpMailer.
 */
public class TestSmtpMailer extends LockssTestCase {
  private SmtpMailer mailer;

  public TestSmtpMailer(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    mailer = new SmtpMailer();
  }

  public void testSendMail() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
    String responses = "220\n250\n250\n250\n354\n250\n";
    ByteArrayInputStream bais = new ByteArrayInputStream(responses.getBytes());
    BufferedReader rdr = new BufferedReader(new InputStreamReader(bais));
    mailer.useTestStreams(new PrintWriter(baos), rdr);

    assertTrue(mailer.doSMTP("target@t.com", "source@s.com",
                             "test subject", "test message", "testHost", 25,
                             "localName"));
    String expectedMessage = "HELO localName\r\n" +
                             "MAIL FROM: <source@s.com>\r\n" +
                             "RCPT TO: <target@t.com>\r\n" +
                             "DATA\r\n" +
                             "From: source@s.com\r\n" +
                             "To: target@t.com\r\n" +
                             "Subject: test subject\r\n" +
                             "X-Mailer: Smtp Mailer\r\n" +
                             "\r\n" +
                             "test message\r\n" +
                             ".\r\n" +
                             "QUIT\r\n";
    assertEquals(expectedMessage, baos.toString());
  }

  public void testCheckReply() {
    String responses = "220\n250\n354\n";
    ByteArrayInputStream bais = new ByteArrayInputStream(responses.getBytes());
    BufferedReader rdr = new BufferedReader(new InputStreamReader(bais));
    mailer.useTestStreams(null, rdr);

    assertTrue(mailer.checkReply("220"));
    System.err.println("Ignore the following error: ");
    assertFalse(mailer.checkReply("220"));
    assertTrue(mailer.checkReply("354"));
  }

}
