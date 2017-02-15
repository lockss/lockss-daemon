/*
 * $Id$
 */

/*

 Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.clockss;

import java.io.*;
import java.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * test class for org.lockss.clockss.ClockssParams
 */
public class TestClockssParams extends LockssTestCase {

  static final String INST_ADDR = "44.55.77.88";
  static final String CLOCKSS_ADDR = "3.2.1.1";

  public void testSubscriptionAddrs() throws Exception {
    MockLockssDaemon daemon = getMockLockssDaemon();
    ClockssParams mgr = new ClockssParams();
    mgr.initService(daemon);
    assertNull(mgr.getInstitutionSubscriptionAddr());
    assertNull(mgr.getClockssSubscriptionAddr());
    mgr.startService();
    assertNull(mgr.getInstitutionSubscriptionAddr());
    assertNull(mgr.getClockssSubscriptionAddr());
    Properties p = new Properties();
    p.put(ClockssParams.PARAM_INSTITUTION_SUBSCRIPTION_ADDR, INST_ADDR);
    p.put(ClockssParams.PARAM_CLOCKSS_SUBSCRIPTION_ADDR, CLOCKSS_ADDR);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    assertEquals(INST_ADDR,
		 mgr.getInstitutionSubscriptionAddr().getHostAddress());
    assertEquals(CLOCKSS_ADDR,
		 mgr.getClockssSubscriptionAddr().getHostAddress());
  }

  public void testEnable() throws Exception {
    MockLockssDaemon daemon = getMockLockssDaemon();
    ClockssParams mgr = new ClockssParams();
    mgr.initService(daemon);
    mgr.startService();
    assertFalse(mgr.isDetectSubscription());
    ConfigurationUtil.setFromArgs(ClockssParams.PARAM_ENABLE_CLOCKSS_SUBSCRIPTION_DETECTION, "true");
    assertTrue(mgr.isDetectSubscription());
  }

}
