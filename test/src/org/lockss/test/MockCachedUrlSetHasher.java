/*
 * $Id: MockCachedUrlSetHasher.java,v 1.8 2009-03-05 05:40:04 tlipkis Exp $
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
  int hashDelay = 0;

  public MockCachedUrlSetHasher(int numbytes) {
    this.bytes = numbytes;
  }

  public MockCachedUrlSetHasher() {
  }

  public void setFiltered(boolean val) {
  }

  public org.lockss.plugin.CachedUrlSet getCachedUrlSet() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public long getEstimatedHashDuration() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void storeActualHashDuration(long elapsed, Exception err) {
//     throw new UnsupportedOperationException("Not implemented");
  }

  public String typeString() {
    return "M";
  }

  public MessageDigest[] getDigests() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean finished() {
    return bytes <= 0;
  }

  public void abortHash() {
  }

  public int hashStep(int numBytes) {
    if (toThrow != null) {
      throw toThrow;
    }
    if (finished()) {
      return 0;
    }
    if (hashDelay>0) {
      try {
        Thread.sleep(hashDelay);
      } catch (InterruptedException ex) { }
    }
    numBytes = Math.max(1, Math.min(bytes, numBytes));
    bytes -= numBytes;
    return numBytes;
  }

  public void setNumBytes(int n) {
    bytes = n;
  }

  public long getBytesLeft() {
    return bytes;
  }

  public void setHashStepDelay(int ms) {
    hashDelay = ms;
  }

  public void throwThis(Error e) {
    toThrow = e;
  }

}
