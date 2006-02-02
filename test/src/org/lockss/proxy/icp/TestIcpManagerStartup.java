/*
 * $Id: TestIcpManagerStartup.java,v 1.8 2006-02-02 01:00:01 thib_gc Exp $
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

import junit.framework.Test;

import org.lockss.config.Configuration;
import org.lockss.config.Configuration.Differences;
import org.lockss.test.*;
import org.lockss.util.Logger;

/**
 * <p>Tests various combinations of startup-time parameters for the
 * ICP manager.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class TestIcpManagerStartup extends LockssTestCase {

  public static class PlatformDisabledDaemonDisabled extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = false;
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                    "false",
                                    IcpManager.PARAM_ICP_ENABLED,
                                    "false");
    }
  }

  public static class PlatformDisabledDaemonEnabled extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = false;
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                    "false",
                                    IcpManager.PARAM_ICP_ENABLED,
                                    "true",
                                    IcpManager.PARAM_ICP_PORT,
                                    Integer.toString(getNewPort()));
    }
  }

  public static class PlatformDisabledDaemonUnset extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = false;
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                    "false");
    }
  }

  public static class PlatformDisabledPortSetDaemonEnabledPortUnset extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = false;
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                    "false",
                                    IcpManager.PARAM_PLATFORM_ICP_PORT,
                                    Integer.toString(getNewPort()),
                                    IcpManager.PARAM_ICP_ENABLED,
                                    "true");
    }
  }

  public static class PlatformDisabledPortUnsetDaemonEnabledPortUnset extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = false;
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                    "false",
                                    IcpManager.PARAM_ICP_ENABLED,
                                    "true");
    }
  }

  public static class PlatformEnabledDaemonDisabled extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = false;
      int notExpectedPort = getNewPort();
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                    "true",
                                    IcpManager.PARAM_PLATFORM_ICP_PORT,
                                    Integer.toString(notExpectedPort),
                                    IcpManager.PARAM_ICP_ENABLED,
                                    "false");
    }
  }

  public static class PlatformEnabledDaemonEnabled extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = true;
      expectedPort = getNewPort();
      int notExpectedPort = getNewPort();
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                    "true",
                                    IcpManager.PARAM_PLATFORM_ICP_PORT,
                                    Integer.toString(notExpectedPort),
                                    IcpManager.PARAM_ICP_ENABLED,
                                    "true",
                                    IcpManager.PARAM_ICP_PORT,
                                    Integer.toString(expectedPort));
    }
  }

  public static class PlatformEnabledDaemonUnset extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = false;
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                    "true");
    }
  }

  public static class PlatformEnabledPortSetDaemonEnabledPortUnset extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = true;
      expectedPort = getNewPort();
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                    "true",
                                    IcpManager.PARAM_PLATFORM_ICP_PORT,
                                    Integer.toString(expectedPort),
                                    IcpManager.PARAM_ICP_ENABLED,
                                    "true");
    }
  }

  public static class PlatformEnabledPortUnsetDaemonEnabledPortUnset extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = false;
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                    "true",
                                    IcpManager.PARAM_ICP_ENABLED,
                                    "true");
    }
  }

  public static class PlatformUnsetDaemonDisabled extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = false;
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_ICP_ENABLED,
                                    "false");
    }
  }

  public static class PlatformUnsetDaemonEnabled extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = true;
      expectedPort = getNewPort();
      ConfigurationUtil.addFromArgs(IcpManager.PARAM_ICP_ENABLED,
                                    "true",
                                    IcpManager.PARAM_ICP_PORT,
                                    Integer.toString(expectedPort));
    }
  }

  public static class PlatformUnsetDaemonUnset extends TestIcpManagerStartup {
    protected void setConfig() {
      expectedRunning = false;
    }
  }

  protected int expectedPort = -1;

  protected boolean expectedRunning;

  private IcpManager icpManager;

  private MockLockssDaemon mockLockssDaemon;

  public void setUp() throws Exception {
    logger.info("BEGIN: " + getClass().getName());
    super.setUp();
    mockLockssDaemon = getMockLockssDaemon();
    icpManager = new IcpManager();
    setConfig();
    mockLockssDaemon.setIcpManager(icpManager);
    icpManager.initService(mockLockssDaemon);
    mockLockssDaemon.setDaemonInited(true);
    icpManager.startService();
  }

  public void tearDown() {
    icpManager.stopService();
    logger.info("END: " + getClass().getName());
  }

  public void testStartedAsExpected() {
    assertEquals("FAILED: " + getClass().getName(),
                 expectedRunning,
                 icpManager.isIcpServerRunning());
    if (expectedRunning) {
      assertEquals("FAILED: " + getClass().getName(),
                   expectedPort,
                   icpManager.getCurrentPort());
    }
    logger.info("PASSED: " + getClass().getName());
  }

  protected abstract void setConfig();

  private static Logger logger = Logger.getLogger("TestIcpManagerStartup");

  private static int port = 65000;

  public static Test suite() {
    return variantSuites(TestIcpManagerStartup.class);
  }

  protected static int getNewPort() {
    return port++;
  }

}
