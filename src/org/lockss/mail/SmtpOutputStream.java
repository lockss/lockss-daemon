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

/**
 * Filter stream to send RFC822 data on SMTP connection - ensures network
 * end-of-line, quotes dots at beginning-of-line, terminates with line
 * containing only dot..
 */
class SmtpOutputStream extends FilterOutputStream {

  private int prev = -1;
  private boolean done = false;

  public SmtpOutputStream(OutputStream out) throws IOException {
    super(out);
  }

  private static final byte[] CRLF = { '\r', '\n' };

  public void write(int b) throws IOException {
    // convert newline to crlf
    if (b == '\n') {
      if (prev != '\r') {
	out.write(CRLF);
      }
    } else if (b == '\r') {
      out.write(CRLF);
    } else {
      // double leading dots
      if (b == '.' && (prev == '\n' || prev == '\r' || prev == -1)) {
	out.write('.');
      }
      out.write(b);
    }
    prev = b;
  }

  public void write(byte[] buf, int off, int len) throws IOException {
    if ((off | len | (buf.length - (len + off)) | (off + len)) < 0)
      throw new IndexOutOfBoundsException();

    int end = off + len;
    int seg = off;			// beginning of unsent segment
    for (int ix = seg; ix < end; ix++) {
      int b = buf[ix];
      // convert newline to crlf
      if (b == '\n') {
	if (prev != '\r') {
	  if (ix - seg > 0) out.write(buf, seg, ix - seg);
	  out.write(CRLF);
	}
	seg = ix + 1;
      } else if (b == '\r') {
	if (ix - seg > 0) out.write(buf, seg, ix - seg);
	out.write(CRLF);
	seg = ix + 1;
      } else {
	// double leading dots
	if (b == '.' && (prev == '\n' || prev == '\r' || prev == -1)) {
	  if (ix - seg > 0) out.write(buf, seg, ix - seg);
	  out.write('.');
	  seg = ix;
	}
      }
      prev = b;
    }
    if (end - seg > 0) {
      out.write(buf, seg, end - seg);
    }
  }

  /** Finish writing the data - write terminating newline-dot-newline.
   * Must be called after data written to stream.
   */
  public void flushSmtpData() throws IOException {
    if (done) return;

    // ensure ending crlf
    if (prev != '\n' && prev != '\r') {
      out.write('\r');
      out.write('\n');
    }
    out.write('.');
    out.write('\r');
    out.write('\n');

    done = true;
  }
}
