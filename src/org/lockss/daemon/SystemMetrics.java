/*
 * $Id: SystemMetrics.java,v 1.4 2002-12-31 22:44:34 aalto Exp $
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
  /**
   * Configuration parameter name for duration, in ms, for which the hash test
   * should run.
   */
  public static final String PARAM_HASH_TEST_DURATION =
      Configuration.PREFIX + "hashtest.duration";
  /**
   * Configuration parameter name for the number of bytes per step in the hash
   * test.
   */
  public static final String PARAM_HASH_TEST_BYTE_STEP =
      Configuration.PREFIX + "hashtest.bytestep";
  static final String DEFAULT_HASH_DURATION = "1000";
  static final String DEFAULT_HASH_STEP = "1024";


  private static SystemMetrics metrics = null;
  private static Hashtable estimateTable;
  private static Logger logger = Logger.getLogger("SystemMetrics");

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

  SystemMetrics() {
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
      int hashDuration =
          Integer.parseInt(Configuration.getParam(PARAM_HASH_TEST_DURATION,
          DEFAULT_HASH_DURATION));
      int hashStep =
          Integer.parseInt(Configuration.getParam(PARAM_HASH_TEST_BYTE_STEP,
          DEFAULT_HASH_STEP));

      long startTime = TimeBase.nowMs();
      Deadline deadline = Deadline.in(hashDuration);
      while (!deadline.expired() && !hasher.finished()) {
        bytesHashed += hasher.hashStep(hashStep);
      }
      timeTaken = TimeBase.nowMs() - startTime;
      if (timeTaken==0) {
        logger.error("Test finished in zero time: using bytesHashed estimate.");
        return (int)bytesHashed;
      }
      estimate = new Integer((int)(bytesHashed / timeTaken));
      estimateTable.put(digest.getAlgorithm(), estimate);
    }
    return estimate.intValue();
  }
}
