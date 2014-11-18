/*
 * $Id: TestWsInfrastructure.java,v 1.1 2014-11-18 18:22:52 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Properties;
import org.lockss.config.ConfigManager;
import org.lockss.plugin.PluginManager;
import org.lockss.servlet.AdminServletManager;
import org.lockss.servlet.ServletManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.TcpTestUtil;
import org.lockss.util.Logger;

/**
 * Test class for the web services infrastructure.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class TestWsInfrastructure extends LockssTestCase {
  static Logger log = Logger.getLogger(TestWsInfrastructure.class);

  private static final String USER_NAME = "lockss-u";
  private static final String PASSWORD = "lockss-p";
  private static final String PASSWORD_SHA1 =
      "SHA1:ac4fc8fa9930a24c8d002d541c37ca993e1bc40f";

  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath();
    int port = TcpTestUtil.findUnboundTcpPort();

    Properties props = new Properties();
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
	tempDirPath);

    props.setProperty(AdminServletManager.PARAM_PORT, "" + port);
    props.setProperty(ServletManager.PARAM_PLATFORM_USERNAME, USER_NAME);
    props.setProperty(ServletManager.PARAM_PLATFORM_PASSWORD, PASSWORD_SHA1);

    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.setDaemonInited(true);

    PluginManager pluginManager = theDaemon.getPluginManager();
    pluginManager.startService();

    try {
      theDaemon.getServletManager().startService();

      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
  	return new PasswordAuthentication(USER_NAME, PASSWORD.toCharArray());
        }
      });
    } catch (Exception e) {
      fail(e.toString());
    }
  }

  /**
   * Dummy test.
   * 
   * @throws Exception
   */
  public void testNothing() throws Exception {
  }
}
