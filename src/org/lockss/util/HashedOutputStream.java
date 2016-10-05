/*
 * $Id$
 */

/*

Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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
import java.security.MessageDigest;

/**
 * An OutputStream wrapper that feeds all the bytes written to a MessageDigest.
 */
public class HashedOutputStream extends FilterOutputStream {
  private MessageDigest md;

  /**
   * @param os the underlying OutputStream.
   * @param md the MessageDigest to which to feed the bytes written.
   */
  public HashedOutputStream(OutputStream os, MessageDigest md) {
    super(os);
    if (os == null) {
      throw new IllegalArgumentException("null OutputStream");
    }
    if (md == null) {
      throw new IllegalArgumentException("null MessageDigest");
    }
    this.md = md;
  }

  public void write(int b) throws IOException {
    byte[] buf = new byte[1];
    buf[0] = (byte)b;
    write(buf);
  }

  public void write(byte[] buf) throws IOException {
    write(buf, 0, buf.length);
  }

  public void write(byte[] buf, int off, int len) throws IOException {
    out.write(buf, off, len);
    md.update(buf, off, len);
  }

}
