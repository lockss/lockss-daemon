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

package org.lockss.hasher;

import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;


/**
 * Test class for org.lockss.hasher.HashService
 */

public class TestHashService extends LockssTestCase {
  private static String PARAM_ESTIMATE_PAD_CONSTANT =
    HashService.PARAM_ESTIMATE_PAD_CONSTANT;
  /** Percentage by which hash estimates are increased */
  private static String PARAM_ESTIMATE_PAD_PERCENT =
    HashService.PARAM_ESTIMATE_PAD_PERCENT;

  private MockLockssDaemon theDaemon;

  private HashService svc;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    svc = theDaemon.getHashService();
    svc.startService();
  }

  public void tearDown() throws Exception {
    svc.stopService();
    super.tearDown();
  }

  public void testPadEstimate() {
    Properties props = new Properties();
    props.put(PARAM_ESTIMATE_PAD_CONSTANT, "10");
    props.put(PARAM_ESTIMATE_PAD_PERCENT, "20");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    assertEquals(10, svc.padHashEstimate(0));
    assertEquals(11, svc.padHashEstimate(1));
    assertEquals(250, svc.padHashEstimate(200));
    assertEquals(130, svc.padHashEstimate(100));
  }
}
