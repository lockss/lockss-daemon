/*
 * $Id: MockCachedUrlSetHasher.java,v 1.2 2002-12-17 02:06:53 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * Mock version of <code>CachedUrlSetHasher</code> used for testing
 */
public class MockCachedUrlSetHasher implements CachedUrlSetHasher {
  long duration;
  int bytes;
  Error toThrow;

  public MockCachedUrlSetHasher(int numbytes) {
    this.bytes = numbytes;
  }

  public MockCachedUrlSetHasher() {
  }

  public boolean finished() {
    return bytes <= 0;
  }

  public int hashStep(int numBytes) {
    if (toThrow != null) {
      throw toThrow;
    }
    if (finished()) {
      return 0;
    }
    numBytes = Math.max(1, Math.min(bytes, numBytes));
    bytes -= numBytes;
    return numBytes;
  }

  public int getBytesPerMsEstimate() {
    return 100;
  }

  public void setNumBytes(int n) {
    bytes = n;
  }

  public long getBytesLeft() {
    return bytes;
  }

  public void throwThis(Error e) {
    toThrow = e;
  }

}
