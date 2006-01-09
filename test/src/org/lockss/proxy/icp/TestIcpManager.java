/*
 * $Id: TestIcpManager.java,v 1.7 2006-01-09 23:08:26 thib_gc Exp $
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

package org.lockss.proxy.icp;

import java.io.IOException;
import java.net.DatagramSocket;

import org.lockss.config.*;
import org.lockss.config.Configuration.Differences;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * <p>Tests the {@link IcpManager} class.</p>
 * @author Thib Guicherd-Callin
 */
public class TestIcpManager extends LockssTestCase {

  /**
   * <p>Instruments the ICP manager.</p>
   * @author Thib Guicherd-Callin
   */
  private class TestableIcpManager extends IcpManager {

    /* Inherit documentation */
    public void setConfig(Configuration newConfig,
                          Configuration prevConfig,
                          Differences changedKeys) {
      TestIcpManager.this.setConfigCalled = true;
      super.setConfig(newConfig, prevConfig, changedKeys);
    }

    /* Inherit documentation */
    protected void startSocket(Configuration theConfig) {
      super.startSocket(theConfig);
      if (icpSocket != null) {
        logger.debug("startSocket in TestableIcpManager: waitRunning");
        icpSocket.waitRunning(Deadline.in(Constants.SECOND));
        logger.debug("startSocket in TestableIcpManager: waitRunning done");
      }
      else {
        logger.debug("startSocket in TestableIcpManager: icpSocket was null");
      }
    }

  }

  /**
   * <p>A mock daemon.</p>
   */
  private MockLockssDaemon mockLockssDaemon;

  /**
   * <p>The next port number to try.</p>
   */
  private int nextPort = 2048;

  /**
   * <p>Flag to indicate {@link IcpManager#setConfig} has been
   * called.</p>
   */
  private boolean setConfigCalled;

  /**
   * <p>An instrumented ICP manager.</p>
   */
  private TestableIcpManager testableIcpManager;

  /* Inherit documentation */
  public void setUp() throws Exception {
    super.setUp();
    ConfigurationUtil.addFromArgs("org.lockss.log.IcpManager.level",
                                  "debug3",
                                  "org.lockss.log.IcpSocketImpl.level",
                                  "debug3");

    setEnabled(false, BAD_PORT);
    setConfigCalled = false;
    mockLockssDaemon = getMockLockssDaemon();
    testableIcpManager = new TestableIcpManager();
    mockLockssDaemon.setIcpManager(testableIcpManager);
    testableIcpManager.initService(mockLockssDaemon);
    mockLockssDaemon.setDaemonInited(true);
    testableIcpManager.startService();
  }

  /* Inherit documentation */
  public void tearDown() {
    testableIcpManager.stopService();
  }

  /**
   * <p>Tests the behavior of the manager when the port number
   * changes.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testChangeIcpPort() throws Exception {
    int port = findUnboundUdpPort();
    setEnabled(true, port);
    assertTrue(testableIcpManager.isIcpServerRunning());
    assertEquals(port, testableIcpManager.getCurrentPort());
    assertNotNull(testableIcpManager.getLimiter());
    setConfigCalled = false;
    port = findUnboundUdpPort();
    setEnabled(true, port);
    assertTrue(setConfigCalled);
    assertTrue(testableIcpManager.isIcpServerRunning());
    assertEquals(port, testableIcpManager.getCurrentPort());
    assertNotNull(testableIcpManager.getLimiter());
  }

  /**
   * <p>Tests the behavior of the manager as it starts up.</p>
   * @throws Exception
   */
  public void testStartIcpServer() throws Exception {
    assertFalse(testableIcpManager.isIcpServerRunning());
    assertNegative(testableIcpManager.getCurrentPort());
    setConfigCalled = false;
    int port = findUnboundUdpPort();
    setEnabled(true, port);
    assertTrue(setConfigCalled);
    assertTrue(testableIcpManager.isIcpServerRunning());
    assertEquals(port, testableIcpManager.getCurrentPort());
    assertNotNull(testableIcpManager.getLimiter());
  }

  public void testStopIcpServer() throws Exception {
    int port = findUnboundUdpPort();
    setEnabled(true, port);
    assertTrue(testableIcpManager.isIcpServerRunning());
    assertEquals(port, testableIcpManager.getCurrentPort());
    assertNotNull(testableIcpManager.getLimiter());
    setConfigCalled = false;
    setEnabled(false, BAD_PORT);
    assertTrue(setConfigCalled);
    assertFalse(testableIcpManager.isIcpServerRunning());
    assertNegative(testableIcpManager.getCurrentPort());
  }

  private int findUnboundUdpPort() {
    for (int p = nextPort; p < 65535; p++) {
      try {
        DatagramSocket sock = new DatagramSocket(p);
        sock.close();
        nextPort = p + 1;
        return p;
      }
      catch (IOException ioe) {
        // nothing; iterate
      }
    }
    log.error("Couldn't find unused TCP port");
    return BAD_PORT;
  }

  private void setEnabled(boolean enabled, int port) {
    ConfigurationUtil.addFromArgs(IcpManager.PARAM_ICP_ENABLED,
                                  Boolean.toString(enabled),
                                  IcpManager.PARAM_ICP_PORT,
                                  Integer.toString(enabled ? port : BAD_PORT));
  }

  private static final int BAD_PORT = -1;

}
