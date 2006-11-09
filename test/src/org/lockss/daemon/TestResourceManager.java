/*
 * $Id: TestResourceManager.java,v 1.7 2006-11-09 01:44:53 thib_gc Exp $
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

package org.lockss.daemon;

import org.lockss.util.*;
import org.lockss.test.*;

/**
 * <p>Test class for {@link ResourceManager}.</p>
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

  /**
   * <p>Tests {@link ResourceManager#isTcpPortAvailable},
   * {@link ResourceManager#reserveTcpPort} and
   * {@link ResourceManager#releaseTcpPort}.</p>
   */
  public void testTcpPort() {
    final int testPort = 5432;
    final String fooToken = "foo";
    final String barToken = "bar";

    // initially available
    assertTrue(rmgr.isTcpPortAvailable(testPort, fooToken));
    // reserve for bar
    assertTrue(rmgr.reserveTcpPort(testPort, barToken));
    // still available to bar
    assertTrue(rmgr.isTcpPortAvailable(testPort, barToken));
    // but not to foo
    assertFalse(rmgr.isTcpPortAvailable(testPort, fooToken));
    // still can be reserved by bar
    assertTrue(rmgr.reserveTcpPort(testPort, barToken));
    // but not by foo
    assertFalse(rmgr.reserveTcpPort(testPort, fooToken));
    // attempt by foo to release fails
    assertFalse(rmgr.releaseTcpPort(testPort, fooToken));
    // leaving it unchanged
    assertFalse(rmgr.isTcpPortAvailable(testPort, fooToken));
    assertTrue(rmgr.reserveTcpPort(testPort, barToken));
    // bar can release it, now available
    assertTrue(rmgr.releaseTcpPort(testPort, barToken));
    // available to anyone after released
    assertTrue(rmgr.releaseTcpPort(testPort, barToken));
    assertTrue(rmgr.releaseTcpPort(testPort, fooToken));
  }

  /**
   * <p>Tests {@link ResourceManager#isUdpPortAvailable},
   * {@link ResourceManager#reserveUdpPort} and
   * {@link ResourceManager#releaseUdpPort}.</p>
   */
  public void testUdpPort() {
    final int testPort = 5432;
    final String fooToken = "foo";
    final String barToken = "bar";

    // initially available
    assertTrue(rmgr.isUdpPortAvailable(testPort, fooToken));
    // reserve for bar
    assertTrue(rmgr.reserveUdpPort(testPort, barToken));
    // still available to bar
    assertTrue(rmgr.isUdpPortAvailable(testPort, barToken));
    // but not to foo
    assertFalse(rmgr.isUdpPortAvailable(testPort, fooToken));
    // still can be reserved by bar
    assertTrue(rmgr.reserveUdpPort(testPort, barToken));
    // but not by foo
    assertFalse(rmgr.reserveUdpPort(testPort, fooToken));
    // attempt by foo to release fails
    assertFalse(rmgr.releaseUdpPort(testPort, fooToken));
    // leaving it unchanged
    assertFalse(rmgr.isUdpPortAvailable(testPort, fooToken));
    assertTrue(rmgr.reserveUdpPort(testPort, barToken));
    // bar can release it, now available
    assertTrue(rmgr.releaseUdpPort(testPort, barToken));
    // available to anyone after released
    assertTrue(rmgr.releaseUdpPort(testPort, barToken));
    assertTrue(rmgr.releaseUdpPort(testPort, fooToken));
  }

  public void testGetUsableTcpPorts() {
    String srvr = "server";
    assertNull(rmgr.getUsableTcpPorts(srvr));
    ConfigurationUtil.setFromArgs(PlatformUtil.PARAM_UNFILTERED_TCP_PORTS,
				  "9900;1234;1235");
    assertEquals(ListUtil.list("9900", "1234", "1235"),
		 rmgr.getUsableTcpPorts(srvr));
    assertTrue(rmgr.reserveTcpPort(1234, srvr));
    assertEquals(ListUtil.list("9900", "1234", "1235"),
		 rmgr.getUsableTcpPorts(srvr));
    assertTrue(rmgr.reserveTcpPort(1235, "another service"));
    assertEquals(ListUtil.list("9900", "1234"), rmgr.getUsableTcpPorts(srvr));
    ConfigurationUtil.setFromArgs(PlatformUtil.PARAM_UNFILTERED_TCP_PORTS,
				  "9900;1234;1235;333-335");
    assertEquals(ListUtil.list("9900", "1234", "333-335"),
		 rmgr.getUsableTcpPorts(srvr));
    assertTrue(rmgr.reserveTcpPort(334, "not server"));
    assertEquals(ListUtil.list("9900", "1234", "333", "335"),
                 rmgr.getUsableTcpPorts(srvr));
  }

  public void testGetUsableUdpPorts() {
    String srvr = "server";
    assertNull(rmgr.getUsableUdpPorts(srvr));
    ConfigurationUtil.setFromArgs(PlatformUtil.PARAM_UNFILTERED_UDP_PORTS,
                                  "9900;1234;1235");
    assertEquals(ListUtil.list("9900", "1234", "1235"),
                 rmgr.getUsableUdpPorts(srvr));
    assertTrue(rmgr.reserveUdpPort(1234, srvr));
    assertEquals(ListUtil.list("9900", "1234", "1235"),
                 rmgr.getUsableUdpPorts(srvr));
    assertTrue(rmgr.reserveUdpPort(1235, "another service"));
    assertEquals(ListUtil.list("9900", "1234"), rmgr.getUsableUdpPorts(srvr));
    ConfigurationUtil.setFromArgs(PlatformUtil.PARAM_UNFILTERED_UDP_PORTS,
                                  "9900;1234;1235;333-335");
    assertEquals(ListUtil.list("9900", "1234", "333-335"),
                 rmgr.getUsableUdpPorts(srvr));
    assertTrue(rmgr.reserveUdpPort(334, "not server"));
    assertEquals(ListUtil.list("9900", "1234", "333", "335"),
                 rmgr.getUsableUdpPorts(srvr));
  }

}
