/*
 * $Id: TestContentConfigurationServiceClient.java,v 1.1.2.1 2014-06-03 21:25:27 fergaloy-sf Exp $
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

package org.lockss.ws.content.client;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.remote.RemoteApi;
import org.lockss.servlet.AdminServletManager;
import org.lockss.servlet.LockssServlet;
import org.lockss.servlet.ServletManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.TcpTestUtil;
import org.lockss.ws.content.ContentConfigurationService;
import org.lockss.ws.entities.ContentConfigurationResult;

/**
 * Test class for org.lockss.ws.content.ContentConfigurationService
 * 
 * @author Fernando Garcia-Loygorri
 */
public class TestContentConfigurationServiceClient extends LockssTestCase {
  private static final String USER_NAME = "lockss-u";
  private static final String PASSWORD = "lockss-p";
  private static final String PASSWORD_SHA1 =
      "SHA1:ac4fc8fa9930a24c8d002d541c37ca993e1bc40f";
  private static final String TARGET_NAMESPACE =
      "http://content.ws.lockss.org/";
  private static final String SERVICE_NAME =
      "ContentConfigurationServiceImplService";

  private SimulatedArchivalUnit sau;
  private AccountManager accountManager;

  private ContentConfigurationService proxy;

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = setUpDiskSpace();

    int port = TcpTestUtil.findUnboundTcpPort();
    ConfigurationUtil.addFromArgs(AdminServletManager.PARAM_PORT, "" + port,
	ServletManager.PARAM_PLATFORM_USERNAME, USER_NAME,
	ServletManager.PARAM_PLATFORM_PASSWORD, PASSWORD_SHA1);

    MockLockssDaemon theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();

    PluginManager pluginManager = theDaemon.getPluginManager();
    pluginManager.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginManager.startService();

    MockIdentityManager idMgr = new MockIdentityManager();
    theDaemon.setIdentityManager(idMgr);
    idMgr.initService(theDaemon);

    RemoteApi rapi = new RemoteApi();
    theDaemon.setRemoteApi(rapi);
    rapi.initService(theDaemon);
    rapi.startService();

    accountManager = new AccountManager();
    theDaemon.setAccountManager(accountManager);
    accountManager.initService(theDaemon);
    accountManager.startService();

    sau = PluginTestUtil.createAndStartSimAu(SimulatedPlugin.class,
	simAuConfig(tempDirPath + "/0"));

    ServletManager servletManager = theDaemon.getServletManager();
    servletManager.startService();

    theDaemon.setAusStarted(true);

    // The client authentication.
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
	return new PasswordAuthentication(USER_NAME, PASSWORD.toCharArray());
      }
    });

    String addressLocation = "http://localhost:" + port
	+ "/ws/ContentConfigurationService?wsdl";

    Service service = Service.create(new URL(addressLocation), new QName(
	TARGET_NAMESPACE, SERVICE_NAME));

    proxy = service.getPort(ContentConfigurationService.class);
  }

  /**
   * Tests role authorization.
   */
  public void testRoleAuthorization() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    ContentConfigurationResult result = proxy.addAuById(sau.getAuId());
    assertTrue(result.getId().equals(sau.getAuId()));

    // User "contentAdminRole" should fail.
    try {
      userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);

      result = proxy.addAuById(sau.getAuId());
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ADMIN);
    } catch (Exception e) {
      // Expected authorization failure.
    }

    // User "auAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);

    result = proxy.addAuById(sau.getAuId());
    assertTrue(result.getId().equals(sau.getAuId()));

    // User "accessContentRole" should fail.
    try {
      userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);

      result = proxy.addAuById(sau.getAuId());
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (Exception e) {
      // Expected authorization failure.
    }
  }

  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);

    return conf;
  }
}
