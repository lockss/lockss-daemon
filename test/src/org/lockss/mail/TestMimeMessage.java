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
import org.apache.oro.text.regex.*;
import javax.mail.*;
import javax.mail.internet.*;
// import javax.activation.*;
import org.lockss.test.*;
import org.lockss.mail.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

public class TestMimeMessage extends LockssTestCase {

  // The generated message is checked against these patterns, in a way that
  // is too brittle (order dependent, etc.).  If the format of the message
  // changes innocuously the patterns and/or tests may have to be changed

  static final String PAT_MESSAGE_ID = "^Message-ID: ";
  static final String PAT_MIME_VERSION = "^MIME-Version: 1.0";
  static final String PAT_CONTENT_TYPE = "^Content-Type: ";
  static final String PAT_FROM = "^From: ";
  static final String PAT_TO = "^To: ";
  static final String PAT_BOUNDARY = "^------=_Part";
  static final String PAT_CONTENT_TRANSFER_ENCODING =
    "^Content-Transfer-Encoding: ";
  static final String PAT_CONTENT_DISPOSITION = "^Content-Disposition: ";

  String toString(MailMessage msg) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      BufferedOutputStream out = new BufferedOutputStream(baos);
      msg.writeData(out);
      out.flush();
      return baos.toString();
    } catch (IOException e) {
      throw new RuntimeException("Error converting MimeMessage to string", e);
    }
  }

  /** Return the header part (lines preceding first blank line) */
  String getHeader(String body) {
    List lst = StringUtil.breakAt(body, "\r\n\r\n");
    if (lst.size() < 1) return null;
    return (String)lst.get(0);
  }

  /** Return the header part (lines preceding first blank line) */
  String getHeader(MimeMessage msg) {
    return getHeader(toString(msg));
  }

  /** Return the body part (lines following first blank line) */
  String getBody(String body) {
    int pos = body.indexOf("\r\n\r\n");
    if (pos < 0) return null;
    return body.substring(pos+4);
  }

  /** Return the body part (lines following first blank line) */
  String getBody(MimeMessage msg) {
    return getBody(toString(msg));
  }

  /** Break a string at <crlf>s into an array of lines */
  String[] getLines(String body) {
    List lst = StringUtil.breakAt(body, "\r\n");
    return (String[])lst.toArray(new String[0]);
  }

  String[] getHeaderLines(MimeMessage msg) {
    return getLines(getHeader(msg));
  }

  String[] getBodyLines(MimeMessage msg) {
    return getLines(getBody(msg));
  }

  /** assert that the pattern exists in the string, interpreting the string
   * as having multiple lines */
  void assertHeaderLine(String expPat, String hdr) {
    Pattern pat = RegexpUtil.uncheckedCompile(expPat,
					      Perl5Compiler.MULTILINE_MASK);
    assertMatchesRE(pat, hdr);
  }

  /** assert that the message starts with the expected header */
  void assertHeader(MimeMessage msg) {
    assertHeader(null, null, msg);
  }

  /** assert that the message starts with the expected header */
  void assertHeader(String expFrom, String expTo, MimeMessage msg) {
    String hdr = getHeader(msg);
    assertHeaderLine(PAT_MESSAGE_ID, hdr);
    assertHeaderLine(PAT_MIME_VERSION, hdr);
    assertHeaderLine(PAT_CONTENT_TYPE, hdr);
    if (expFrom != null) {
      assertHeaderLine(PAT_FROM + expFrom, hdr);
    }
    if (expTo != null) {
      assertHeaderLine(PAT_TO + expTo, hdr);
    }
  }

  /** assert that the array of lines is a MIME text-part with the expected
   * text */
  void assertTextPart(String expText, String[]lines, int idx) {
    assertMatchesRE(PAT_BOUNDARY, lines[idx + 0]);
    assertMatchesRE(PAT_CONTENT_TYPE + "text/plain; charset=us-ascii",
		    lines[idx + 1]);
    assertMatchesRE(PAT_CONTENT_TRANSFER_ENCODING + "7bit",
		    lines[idx + 2]);
    assertEquals("", lines[idx + 3]);
    assertEquals(expText, lines[idx + 4]);
    assertMatchesRE(PAT_BOUNDARY, lines[idx + 5]);
  }

  public void testNull() throws IOException {
    MimeMessage msg = new MimeMessage();
    assertHeader(msg);
    assertEquals(2, getBodyLines(msg).length);
    assertMatchesRE(PAT_BOUNDARY, getBody(msg));
    assertEquals("", getBodyLines(msg)[1]);
  }

  public void testGetHeader() throws IOException {
    MimeMessage msg = new MimeMessage();
    msg.addHeader("From", "me");
    msg.addHeader("To", "you");
    msg.addHeader("To", "and you");
    assertEquals("me", msg.getHeader("From"));
    assertEquals("you, and you", msg.getHeader("To"));
    assertEquals(null, msg.getHeader("xxxx"));
  }

  public void testText() throws IOException {
    MimeMessage msg = new MimeMessage();
    msg.addHeader("From", "me");
    msg.addHeader("To", "you");
    msg.addHeader("Subject", "topic");
    msg.addTextPart("Message\ntext");
    assertHeader("me", "you", msg);

    String[] blines = getBodyLines(msg);
    log.debug2("msg: " + StringUtil.separatedString(blines, ", "));
    assertTextPart("Message\ntext", blines, 0);
  }

  public void testDot() throws IOException {
    MimeMessage msg = new MimeMessage();
    msg.addHeader("From", "me");
    msg.addHeader("To", "you");
    msg.addTextPart("one\n.\ntwo\n");
    assertHeader("me", "you", msg);

    String[] blines = getBodyLines(msg);
    log.debug2("msg: " + StringUtil.separatedString(blines, ", "));
    assertTextPart("one\n.\ntwo\n", blines, 0);
  }

  public void testTextAndFile() throws IOException {
    File file = FileTestUtil.writeTempFile("XXX",
					   "\000\001\254\255this is a test");
    MimeMessage msg = new MimeMessage();
    msg.addHeader("From", "us");
    msg.addHeader("To", "them");
    msg.addHeader("Subject", "object");
    msg.addTextPart("Explanatory text");
    msg.addFile(file, "file.foo");
    assertHeader("us", "them", msg);

    String[] blines = getBodyLines(msg);
    log.debug2("msg: " + StringUtil.separatedString(blines, ", "));
    assertTextPart("Explanatory text", blines, 0);

    assertMatchesRE(PAT_CONTENT_TYPE +
		    "application/octet-stream; name=file.foo", blines[6]);
    assertMatchesRE(PAT_CONTENT_TRANSFER_ENCODING + "base64", blines[7]);
    assertMatchesRE(PAT_CONTENT_DISPOSITION, blines[8]);
    assertEquals("", blines[9]);
    assertEquals("AAGsrXRoaXMgaXMgYSB0ZXN0", blines[10]);
    assertMatchesRE(PAT_BOUNDARY, blines[11]);

    assertTrue(file.exists());
    msg.delete(true);
    assertTrue(file.exists());
  }

  public void testGetParts() throws Exception {
    File file = FileTestUtil.writeTempFile("XXX",
					   "\000\001\254\255this is a test");
    MimeMessage msg = new MimeMessage();
    msg.addTextPart("foo text");
    msg.addFile(file, "file.foo");
    MimeBodyPart[] parts = msg.getParts();
    assertEquals(2, parts.length);
    assertEquals("foo text", parts[0].getContent());
    assertEquals("file.foo", parts[1].getFileName());
  }

  public void testTmpFile() throws IOException {
    File file = FileTestUtil.writeTempFile("XXX",
					   "\000\001\254\255this is a test");
    MimeMessage msg = new MimeMessage();
    msg.addTmpFile(file, "file.foo");
    assertTrue(file.exists());
    msg.delete(true);
    assertFalse(file.exists());
  }
}
