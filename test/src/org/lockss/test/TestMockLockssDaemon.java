/*
 * $Id: TestMockLockssDaemon.java,v 1.1 2005-11-08 20:29:09 tlipkis Exp $
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
import java.net.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestMockLockssDaemon extends LockssTestCase {

  MockLockssDaemon mockDaemon;

  public void setUp() throws Exception {
    super.setUp();
    mockDaemon = new MockLockssDaemon();
  }

  // ensure that isDaemonInited() and isAppInited() are the same
  public void testIsInited() {
    assertFalse(mockDaemon.isDaemonInited());
    assertFalse(mockDaemon.isAppInited());
    mockDaemon.setDaemonInited(true);
    assertTrue(mockDaemon.isDaemonInited());
    assertTrue(mockDaemon.isAppInited());
  }

  // ensure that isDaemonRunning() and isAppRunning() are the same
  public void testIsRunning() {
    assertFalse(mockDaemon.isDaemonRunning());
    assertFalse(mockDaemon.isAppRunning());
    mockDaemon.setDaemonRunning(true);
    assertTrue(mockDaemon.isDaemonRunning());
    assertTrue(mockDaemon.isAppRunning());
  }
}

