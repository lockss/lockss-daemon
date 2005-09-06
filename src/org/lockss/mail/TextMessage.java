/*
 * $Id: TextMessage.java,v 1.1 2005-09-06 20:06:31 tlipkis Exp $
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
import org.lockss.util.*;
import org.lockss.daemon.*;

/**
 * Simple mail message with text body
 */
public class TextMessage implements MailMessage  {
  protected static Logger log = Logger.getLogger("TextMessage");

  private StringBuffer headers = new StringBuffer();
  private String text;

  public TextMessage() {
  }

  public MailMessage addHeader(String name, String val) {
    headers.append(name);
    headers.append(": ");
    headers.append(val);
    headers.append("\n");
    return this;
  }

  public MailMessage setText(String text) {
    this.text = text;
    return this;
  }

  public String getBody() {
    if (text == null) {
      return headers + "\n";
    }
    StringBuffer body =
      new StringBuffer(text.length() + headers.length() + 10);
    body.append(headers);
    body.append("\n");
    body.append(text);
    return body.toString();
  }

  /** Send the body, ensuring proper network end-of-line, quoting any
   * leading dots, and terminating with <nl>,<nl> */
  public void sendBody(PrintStream ostrm) throws IOException {
    String body = getBody();
    char prev = 0;
    for (int ix = 0, len = body.length(); ix < len; ix++) {
      char c = (char)body.charAt(ix);
      // double leading dots
      if (prev == '\n' && c == '.') {
	ostrm.write('.');
      }
      // convert newline to crlf
      if (c == '\n' && prev != '\r') {
	ostrm.write('\r');
      }
      ostrm.write(c);
      prev = c;
    }
    // ensure ending crlf
    if (prev != '\n') {
      ostrm.print("\r\n");
    }
    log.debug3("Body sent");
  }

  /** Does nothing */
  public void delete(boolean sentOk) {
  }
}
