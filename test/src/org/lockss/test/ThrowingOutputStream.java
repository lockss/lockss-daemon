/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;

/** An output stream that can throw an exception on demand. */
public class ThrowingOutputStream extends FilterOutputStream {
  private IOException throwOnWrite;
  private IOException throwOnClose;

  public ThrowingOutputStream(OutputStream out,
			      IOException throwOnWrite,
			      IOException throwOnClose) throws IOException {
    super(out);
    this.throwOnWrite = throwOnWrite;
    this.throwOnClose = throwOnClose;
  }

  public void write(int b) throws IOException {
    if (throwOnWrite != null) {
      throw throwOnWrite;
    } else {
      out.write(b);
    }
  }

  public void write(byte[] b, int off, int len) throws IOException {
    if (throwOnWrite != null) {
      throw throwOnWrite;
    }
    out.write(b, off, len);
  }

  public void write(byte[] b) throws IOException {
    if (throwOnWrite != null) {
      throw throwOnWrite;
    }
    out.write(b);
  }

  public void close() throws IOException {
    if (throwOnClose != null) {
      throw throwOnClose;
    } else {
      out.close();
    }
  }
}
