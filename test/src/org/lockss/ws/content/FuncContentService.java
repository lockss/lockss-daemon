/*
 * $Id$
 */

/*

 Copyright (c) 2014-2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.content;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.servlet.AdminServletManager;
import org.lockss.servlet.LockssServlet;
import org.lockss.servlet.ServletManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockAuState;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.NoCrawlEndActionsFollowLinkCrawler;
import org.lockss.test.TcpTestUtil;
import org.lockss.ws.cxf.AuthorizationInterceptor;
import org.lockss.ws.entities.ContentResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * Functional test class for org.lockss.ws.content.ContentService.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class FuncContentService extends LockssTestCase {
  private static final String USER_NAME = "lockss-u";
  private static final String PASSWORD = "lockss-p";
  private static final String PASSWORD_SHA1 =
      "SHA1:ac4fc8fa9930a24c8d002d541c37ca993e1bc40f";
  private static final String TARGET_NAMESPACE =
      "http://content.ws.lockss.org/";
  private static final String SERVICE_NAME = "ContentServiceImplService";

  private String tempDirPath;
  private PluginManager pluginMgr;
  private AccountManager accountManager;
  private ContentService proxy;
  private SimulatedArchivalUnit sau;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();

    int port = TcpTestUtil.findUnboundTcpPort();
    ConfigurationUtil.addFromArgs(AdminServletManager.PARAM_PORT, "" + port,
	ServletManager.PARAM_PLATFORM_USERNAME, USER_NAME,
	ServletManager.PARAM_PLATFORM_PASSWORD, PASSWORD_SHA1);

    MockLockssDaemon theDaemon = getMockLockssDaemon();

    accountManager = theDaemon.getAccountManager();
    accountManager.startService();

    MockIdentityManager idMgr = new MockIdentityManager();
    theDaemon.setIdentityManager(idMgr);
    idMgr.initService(theDaemon);

    pluginMgr = theDaemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getRemoteApi().startService();
    theDaemon.getServletManager().startService();
    pluginMgr.startService();

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    sau.generateContentTree();

    Crawler crawler =
	new NoCrawlEndActionsFollowLinkCrawler(sau, new MockAuState());
    crawler.doCrawl();

    theDaemon.setAusStarted(true);

    // The client authentication.
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
	return new PasswordAuthentication(USER_NAME, PASSWORD.toCharArray());
      }
    });

    String addressLocation =
	"http://localhost:" + port + "/ws/ContentService?wsdl";

    Service service = Service.create(new URL(addressLocation), new QName(
	TARGET_NAMESPACE, SERVICE_NAME));

    proxy = service.getPort(ContentService.class);
  }

  /**
   * Tests the fetching of a file.
   */
  public void testFetchFile() throws Exception {
    String auId = sau.getAuId();

    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    String url = "http://www.example.com/001file.bin";
    String outputFileSpec = tempDirPath + "/001file.bin";
    ContentResult result = proxy.fetchFile(url, null);
    assertEquals(256, result.writeContentToFile(outputFileSpec).length());

    Properties properties = result.getProperties();
    assertEquals("1", properties.get("org.lockss.version.number"));
    assertEquals("text/plain", properties.get("x-lockss-content-type"));
    assertEquals("http://www.example.com/001file.bin",
	properties.get("x-lockss-node-url"));

    result = proxy.fetchVersionedFile(url, auId, null);
    assertEquals(256, result.writeContentToFile(outputFileSpec).length());

    properties = result.getProperties();
    assertEquals("1", properties.get("org.lockss.version.number"));
    assertEquals("text/plain", properties.get("x-lockss-content-type"));
    assertEquals("http://www.example.com/001file.bin",
	properties.get("x-lockss-node-url"));

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);

    try {
      result = proxy.fetchVersionedFile(url, null, null);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "auAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);

    try {
      result = proxy.fetchFile(url, null);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "accessContentRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);

    url = "http://www.example.com/branch1/branch1/index.html";
    outputFileSpec = tempDirPath + "/index.html";
    result = proxy.fetchVersionedFile(url, null, null);
    assertEquals(398, result.writeContentToFile(outputFileSpec).length());

    properties = result.getProperties();
    assertEquals("1", properties.get("org.lockss.version.number"));
    assertEquals("text/html", properties.get("x-lockss-content-type"));
    assertEquals("http://www.example.com/branch1/branch1/index.html",
	properties.get("x-lockss-node-url"));

    result = proxy.fetchFile(url, auId);
    assertEquals(398, result.writeContentToFile(outputFileSpec).length());

    properties = result.getProperties();
    assertEquals("1", properties.get("org.lockss.version.number"));
    assertEquals("text/html", properties.get("x-lockss-content-type"));
    assertEquals("http://www.example.com/branch1/branch1/index.html",
	properties.get("x-lockss-node-url"));

    result = proxy.fetchVersionedFile(url, auId, 1);
    assertEquals(398, result.writeContentToFile(outputFileSpec).length());

    properties = result.getProperties();
    assertEquals("1", properties.get("org.lockss.version.number"));
    assertEquals("text/html", properties.get("x-lockss-content-type"));
    assertEquals("http://www.example.com/branch1/branch1/index.html",
	properties.get("x-lockss-node-url"));

    // Once more with a bad AU identifier.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    try {
      auId = "wrongAuId";
      result = proxy.fetchFile(url, auId);
      fail("Test should have failed for auId '" + auId + "'");
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals("org.lockss.ws.entities.LockssWebServicesFault: "
	  + "Missing AU with auid 'wrongAuId'", lwsf.getMessage());
    }

    // Once more with a bad URL.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);

    try {
      url = "wrongURL";
      result = proxy.fetchVersionedFile(url, null, null);
      fail("Test should have failed for url '" + url + "'");
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals("org.lockss.ws.entities.LockssWebServicesFault: "
	  + "Missing CachedUrl for url 'wrongURL'", lwsf.getMessage());
    }

    // Non-existent version.
    try {
      url = "http://www.example.com/branch1/branch1/index.html";
      auId = sau.getAuId();
      result = proxy.fetchVersionedFile(url, auId, 2);
      fail("Test should have failed for version " + 2);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertTrue(lwsf.getMessage().startsWith("java.lang.Exception: Version 2 "
	  + "of http://www.example.com/branch1/branch1/index.html for the "
	  + "requested Archival Unit "
	  + "'org|lockss|plugin|simulated|SimulatedPlugin&"));
      assertTrue(lwsf.getMessage().endsWith(" has no content"));
    }
  }

  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + SimulatedContentGenerator.FILE_TYPE_BIN);
    conf.put(SimulatedPlugin.AU_PARAM_ODD_BRANCH_CONTENT, "true");
    return conf;
  }
}
