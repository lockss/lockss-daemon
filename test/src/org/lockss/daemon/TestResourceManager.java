/*
 * $Id: TestResourceManager.java,v 1.2 2005-04-21 07:22:33 tlipkis Exp $
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

package org.lockss.daemon;

import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.daemon.ResourceManager
 */
public class TestResourceManager extends LockssTestCase {
  private ResourceManager rmgr;

  public void setUp() throws Exception {
    super.setUp();
    rmgr = new ResourceManager();
    rmgr.initService(getMockLockssDaemon());
    rmgr.startService();
  }

  public void tearDown() throws Exception {
    rmgr.stopService();
    super.tearDown();
  }

  public void testTcpPort() {
    // initially available
    assertTrue(rmgr.isTcpPortAvailable(5432, "foo"));
    // reserve for bar
    assertTrue(rmgr.reserveTcpPort(5432, "bar"));
    // still available to bar
    assertTrue(rmgr.isTcpPortAvailable(5432, "bar"));
    // but not to foo
    assertFalse(rmgr.isTcpPortAvailable(5432, "foo"));
    // still can be reserved by bar
    assertTrue(rmgr.reserveTcpPort(5432, "bar"));
    // but not by foo
    assertFalse(rmgr.reserveTcpPort(5432, "foo"));
    // attempt by foo to release fails
    assertFalse(rmgr.releaseTcpPort(5432, "foo"));
    // leaving it unchanged
    assertFalse(rmgr.isTcpPortAvailable(5432, "foo"));
    assertTrue(rmgr.reserveTcpPort(5432, "bar"));
    // bar can release it, now available
    assertTrue(rmgr.releaseTcpPort(5432, "bar"));
    // available to anyone after released
    assertTrue(rmgr.releaseTcpPort(5432, "bar"));
    assertTrue(rmgr.releaseTcpPort(5432, "foo"));
  }

  public void testGetUsableTcpPorts() {
    String srvr = "server";
    assertNull(rmgr.getUsableTcpPorts(srvr));
    ConfigurationUtil.setFromArgs(PlatformInfo.PARAM_UNFILTERED_PORTS,
				  "9900;1234;1235");
    assertEquals(ListUtil.list("9900", "1234", "1235"),
		 rmgr.getUsableTcpPorts(srvr));
    assertTrue(rmgr.reserveTcpPort(1234, srvr));
    assertEquals(ListUtil.list("9900", "1234", "1235"),
		 rmgr.getUsableTcpPorts(srvr));
    assertTrue(rmgr.reserveTcpPort(1235, "another service"));
    assertEquals(ListUtil.list("9900", "1234"), rmgr.getUsableTcpPorts(srvr));
    ConfigurationUtil.setFromArgs(PlatformInfo.PARAM_UNFILTERED_PORTS,
				  "9900;1234;1235;333-444");
    assertEquals(ListUtil.list("9900", "1234", "333-444"),
		 rmgr.getUsableTcpPorts(srvr));
  }
}
