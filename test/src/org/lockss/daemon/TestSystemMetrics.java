/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.Properties;
import java.io.IOException;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for SystemMetrics.
 */
public class TestSystemMetrics extends LockssTestCase {
  private SystemMetrics metrics;
  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    Properties props = new Properties();
    props.setProperty(SystemMetrics.PARAM_HASH_TEST_DURATION,
                      Long.toString(SystemMetrics.DEFAULT_HASH_TEST_DURATION));
    props.setProperty(SystemMetrics.PARAM_HASH_TEST_BYTE_STEP,
                      Integer.toString(SystemMetrics.DEFAULT_HASH_TEST_BYTE_STEP));
    props.setProperty(SystemMetrics.PARAM_DEFAULT_HASH_SPEED,
                      Integer.toString(SystemMetrics.DEFAULT_DEFAULT_HASH_SPEED));
    ConfigurationUtil.setCurrentConfigFromProps(props);
    theDaemon.getHashService().startService();
    metrics = theDaemon.getSystemMetrics();
    metrics.startService();
  }

  public void testHashEstimation() throws IOException {
    int byteCount = SystemMetrics.DEFAULT_HASH_TEST_BYTE_STEP * 10;
    int estimate = byteCount;
    long duration;
    int expectedMin;

    while (true) {
      MockCachedUrlSetHasher hasher = new MockCachedUrlSetHasher(byteCount);
      hasher.setHashStepDelay(10);

      // wipe out cached estimate
      metrics.estimateTable.clear();
      long startTime = TimeBase.nowMs();
      estimate = metrics.measureHashSpeed(hasher, new MockMessageDigest());
      duration = TimeBase.msSince(startTime);
      expectedMin =
          (byteCount * 10) / SystemMetrics.DEFAULT_HASH_TEST_BYTE_STEP;

      if ((estimate!=byteCount) && (duration != expectedMin)) {
        // non-zero hash time
        break;
      } else {
        // if hash time is 0, estimate equals # of bytes
        // we want to run until hashing takes actual time
        // increase byte count and try again
        byteCount *= 10;
      }
    }

    assertTrue(estimate < byteCount);
    // minimum amount of time would be delay * number of hash steps
    assertTrue(duration > expectedMin);
  }

  public void testBytesPerMsHashEstimate() throws IOException {
    assertEquals(250, metrics.getBytesPerMsHashEstimate());
    ConfigurationUtil.setFromArgs(SystemMetrics.PARAM_DEFAULT_HASH_SPEED,
				  "4437");
    // wipe out cached estimate
    assertEquals(4437, metrics.getBytesPerMsHashEstimate());
  }

  public void testEstimationCaching() throws IOException {
    MockCachedUrlSetHasher hasher = new MockCachedUrlSetHasher(10000);
    int estimate = metrics.getBytesPerMsHashEstimate(hasher, new MockMessageDigest());
    assertEquals(SystemMetrics.DEFAULT_DEFAULT_HASH_SPEED, estimate);
    hasher = new MockCachedUrlSetHasher(10);
    int estimate2 = metrics.getBytesPerMsHashEstimate(hasher, new MockMessageDigest());
    assertEquals(estimate, estimate2);
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestSystemMetrics.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
