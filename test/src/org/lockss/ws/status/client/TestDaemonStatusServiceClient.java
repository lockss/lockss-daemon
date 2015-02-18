/*
 * $Id$
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.ws.status.client;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.config.ConfigManager;
import org.lockss.plugin.PluginManager;
import org.lockss.servlet.AdminServletManager;
import org.lockss.servlet.ServletManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.TcpTestUtil;
import org.lockss.util.Logger;
import org.lockss.ws.status.DaemonStatusService;

/**
 * Test class for org.lockss.ws.status.DaemonStatusService
 * 
 * @author Fernando Garcia-Loygorri
 */
public class TestDaemonStatusServiceClient extends LockssTestCase {
  static Logger log = Logger.getLogger(TestDaemonStatusServiceClient.class);

  private static final String USER_NAME = "lockss-u";
  private static final String PASSWORD = "lockss-p";
  private static final String PASSWORD_SHA1 =
      "SHA1:ac4fc8fa9930a24c8d002d541c37ca993e1bc40f";
  private static final String TARGET_NAMESPACE = "http://status.ws.lockss.org/";
  private static final String SERVICE_NAME = "DaemonStatusServiceImplService";

  private MockLockssDaemon theDaemon;
  private DaemonStatusService proxy;

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

    ServletManager servletManager = theDaemon.getServletManager();
    servletManager.startService();

    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
	return new PasswordAuthentication(USER_NAME, PASSWORD.toCharArray());
      }
    });

    String addressLocation =
	"http://localhost:" + port + "/ws/DaemonStatusService?wsdl";

    Service service = Service.create(new URL(addressLocation), new QName(
	TARGET_NAMESPACE, SERVICE_NAME));

    proxy = service.getPort(DaemonStatusService.class);
  }

  /**
   * Runs the tests that verify whether the daemon is ready.
   * 
   * @throws Exception
   */
  public void testIsDaemonReady() throws Exception {
    theDaemon.setAusStarted(true);
    assertTrue(proxy.isDaemonReady());
    theDaemon.setAusStarted(false);
    assertFalse(proxy.isDaemonReady());
  }
}
