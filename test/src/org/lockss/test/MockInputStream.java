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

package org.lockss.test;

import java.io.*;
import java.util.*;
import org.lockss.util.*;

public class MockInputStream extends InputStream {
  private String content;
  private InputStream is = null;
  private boolean isClosed = false;
  protected static Logger log = Logger.getLogger("MockInputStream");

  //interval at which to return 0 when read() is called
  private int zeroInterval = 0;
  private int curInterval = 0;

  /**
   * Signals that the InputStream should reset
   */
  public void regenerate() {
    is = null;
    isClosed = false;
  }

  public void setZeroInterval(int zeroInterval) {
    this.zeroInterval = zeroInterval;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public boolean isClosed() {
    return isClosed;
  }

  public int read() throws IOException {
    if (is == null) {
      is = new StringInputStream(content);
    }
    return is.read();
  }

  public int read(byte b[], int off, int len) throws IOException {
    if (zeroInterval > 0 && curInterval == zeroInterval) {
      log.debug3("Returning zero");
      curInterval=0;
      return 0;
    } else {
      log.debug3("Not returning zero: "+curInterval+" "+zeroInterval);
      curInterval++;
    }
    return super.read(b, off, len);
  }

  public long skip(long n) throws IOException {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public int available() throws IOException {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void close() throws IOException {
    isClosed = true;
  }

  public synchronized void mark(int readlimit) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public synchronized void reset() throws IOException {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public boolean markSupported() {
    throw new UnsupportedOperationException("Not Implemented");
  }
}
