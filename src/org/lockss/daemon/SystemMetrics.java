/*
 * $Id: SystemMetrics.java,v 1.2 2002-12-30 23:04:29 tal Exp $
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

package org.lockss.daemon;

import org.lockss.util.*;
import java.io.IOException;
import java.util.Hashtable;
import java.security.MessageDigest;

/**
 * A singleton class which provides access to various system calculations, such
 * as hash speed estimates.
 */
public class SystemMetrics {
  static final int HASH_TEST_DURATION = 1000;
  static final int HASH_TEST_BYTE_STEP = 1024;
  static final int MIN_BYTES_TO_HASH = 100;

  private static SystemMetrics metrics = null;
  private static Hashtable estimateTable;

  /**
   * Static factory method for the singleton.
   * @return a SystemMetrics object
   */
  public static SystemMetrics getSystemMetrics() {
    if (metrics==null) {
      metrics = new SystemMetrics();
    }
    return metrics;
  }

  private SystemMetrics() {
    estimateTable = new Hashtable();
  }

  /**
   * Returns an estimate on the hashed bytes per ms for this hasher.
   * Tests by hashing the CachedUrlSet for a small period of time.
   * @param hasher the CachedUrlSetHasher to test
   * @param digest the hashing algorithm
   * @return an int for estimated bytes/ms
   * @throws IOException
   */
  public int getBytesPerMsHashEstimate(CachedUrlSetHasher hasher,
                                       MessageDigest digest)
      throws IOException {
    Integer estimate = (Integer)estimateTable.get(digest.getAlgorithm());
    if (estimate==null) {
      long timeTaken = 0;
      long bytesHashed = 0;
      boolean earlyFinish = false;
      long startTime = TimeBase.nowMs();
      Deadline deadline = Deadline.in(HASH_TEST_DURATION);
      while (!deadline.expired()) {
        if (!hasher.finished()) {
          bytesHashed += hasher.hashStep(HASH_TEST_BYTE_STEP);
        } else {
          timeTaken = TimeBase.nowMs() - startTime;
          earlyFinish = true;
        }
      }
      if (!earlyFinish) {
        timeTaken = TimeBase.nowMs() - startTime;
      }
      estimate = new Integer((int)(bytesHashed / timeTaken));
      estimateTable.put(digest.getAlgorithm(), estimate);
    }
    return estimate.intValue();
  }
}
