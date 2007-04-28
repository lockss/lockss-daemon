/*
 * $Id: TestIcpManager.java,v 1.14 2007-04-28 00:21:50 dshr Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
      setConfigCalled = true;
      super.setConfig(newConfig, prevConfig, changedKeys);
    }

  }

  /**
   * <p>A mock daemon.</p>
   */
  private MockLockssDaemon mockLockssDaemon;

  /**
   * <p>The next port number to try.</p>
   */
  private static int port = 2048;

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
    int port = getNewPort();
    setEnabled(true, port);
    assertTrue(testableIcpManager.isIcpServerRunning());
    assertEquals(port, testableIcpManager.getCurrentPort());
    setConfigCalled = false;
    port = getNewPort();
    setEnabled(true, port);
    assertTrue(setConfigCalled);
    assertTrue(testableIcpManager.isIcpServerRunning());
    assertEquals(port, testableIcpManager.getCurrentPort());
  }

  /**
   * <p>Tests the behavior of the manager as it starts up.</p>
   * @throws Exception
   */
  public void testStartIcpServer() throws Exception {
    assertFalse(testableIcpManager.isIcpServerRunning());
    assertNegative(testableIcpManager.getCurrentPort());
    setConfigCalled = false;
    int port = getNewPort();
    setEnabled(true, port);
    assertTrue(setConfigCalled);
    assertTrue(testableIcpManager.isIcpServerRunning());
    assertEquals(port, testableIcpManager.getCurrentPort());
  }

  public void testStopIcpServer() throws Exception {
    int port = getNewPort();
    setEnabled(true, port);
    assertTrue(testableIcpManager.isIcpServerRunning());
    assertEquals(port, testableIcpManager.getCurrentPort());
    setConfigCalled = false;
    setEnabled(false, BAD_PORT);
    assertTrue(setConfigCalled);
    assertFalse(testableIcpManager.isIcpServerRunning());
    assertNegative(testableIcpManager.getCurrentPort());
  }

  private int getNewPort() {
    return port += 7;
  }

  private void setEnabled(boolean enabled, int port) {
    ConfigurationUtil.addFromArgs(IcpManager.PARAM_ICP_ENABLED,
                                  Boolean.toString(enabled),
                                  IcpManager.PARAM_ICP_PORT,
                                  Integer.toString(enabled ? port : BAD_PORT));
  }

  private static final int BAD_PORT = -1;

}
