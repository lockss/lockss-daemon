/*
 * $Id: HashServiceTestPlugin.java,v 1.3 2002-10-16 04:50:54 tal Exp $
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

import java.io.*;
import java.util.*;
import java.net.*;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

/**
 * Mock plugin for testing hash service.  Provides (settable) duration
 * estimate and hashers that take (settable) time.
 */
public class HashServiceTestPlugin {

  public static class CUS extends NullPlugin.CachedUrlSet {
    private long duration = 0;
    private int bytes = 0;

    /** Set the duration that will be returned by estimatedHashDuration() */
    public void setHashEstimate(long duration) {
      this.duration = duration;
    }

    public long estimatedHashDuration() {
      return duration;
    }

    /** Set the actual hash duration and size.
     * @param duration the number of milliseconds the hash will take
     * @param bytes the number of bytes that must be hashed
     */
    public void setHashDuration(long duration, int bytes) {
      this.duration = duration;
      this.bytes = bytes;
    }

    public CachedUrlSetHasher getContentHasher(MessageDigest hasher) {
      return new CUSHasher(duration, bytes);
    }

    public CachedUrlSetHasher getNameHasher(MessageDigest hasher) {
      return new CUSHasher(duration, bytes);
    }
  }

/**
 * CachedUrlSetHasher that takes a programmable time to process a
 * programmable number of bytes.
 */
  static class CUSHasher extends NullPlugin.CachedUrlSetHasher {

    long duration;
    int bytes;

    public CUSHasher(long duration, int bytes) {
      this.duration = duration;
      this.bytes = bytes;
    }

    public boolean finished() {
      return bytes <= 0;
    }

    public int hashStep(int numBytes) {
      if (finished()) {
	return 0;
      }
      numBytes = Math.max(1, Math.min(bytes, numBytes));
      TimerUtil.guaranteedSleep(duration / (bytes / numBytes));

      bytes -= numBytes;
      return numBytes;
    }
  }

  public static CUS getCUS() {
    return new CUS();
  }
}
