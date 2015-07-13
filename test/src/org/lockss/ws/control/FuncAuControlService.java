/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.control;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.crawler.CrawlManagerImpl;
import org.lockss.daemon.ConfigParamAssignment;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.remote.RemoteApi;
import org.lockss.servlet.AdminServletManager;
import org.lockss.servlet.LockssServlet;
import org.lockss.servlet.ServletManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockPlugin;
import org.lockss.test.TcpTestUtil;
import org.lockss.util.ListUtil;
import org.lockss.util.RegexpUtil;
import org.lockss.ws.cxf.AuthorizationInterceptor;
import org.lockss.ws.entities.CheckSubstanceResult;
import org.lockss.ws.entities.LockssWebServicesFault;
import org.lockss.ws.entities.RequestCrawlResult;

/**
 * Functional test class for org.lockss.ws.control.AuControlService.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class FuncAuControlService extends LockssTestCase {
  private static final String BASE_URL = "http://www.example.com/foo/";

  private static final String USER_NAME = "lockss-u";
  private static final String PASSWORD = "lockss-p";
  private static final String PASSWORD_SHA1 =
      "SHA1:ac4fc8fa9930a24c8d002d541c37ca993e1bc40f";
  private static final String TARGET_NAMESPACE =
      "http://control.ws.lockss.org/";
  private static final String SERVICE_NAME = "AuControlServiceImplService";

  private PluginManager pluginMgr;
  private MockPlugin plugin;
  private AccountManager accountManager;
  private RemoteApi remoteApi;

  private AuControlService proxy;

  private String auId0;
  private String auId1;
  private MockArchivalUnit mau;
  private MockArchivalUnit mau0;
  private MockArchivalUnit mau1;

  public void setUp() throws Exception {
    super.setUp();

    setUpDiskSpace();

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
    theDaemon.getServletManager().startService();
    pluginMgr.startService();

    String key =
      PluginManager.pluginKeyFromName(MyMockPlugin.class.getName());
    pluginMgr.ensurePluginLoaded(key);
    plugin = (MockPlugin)pluginMgr.getPlugin(key);

    CrawlManagerImpl crawlManager =
	(CrawlManagerImpl)theDaemon.getCrawlManager();
    crawlManager.startService();

    remoteApi = theDaemon.getRemoteApi();
    remoteApi.startService();

    theDaemon.setAusStarted(true);

    // The client authentication.
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
	return new PasswordAuthentication(USER_NAME, PASSWORD.toCharArray());
      }
    });

    String addressLocation =
	"http://localhost:" + port + "/ws/AuControlService?wsdl";

    Service service = Service.create(new URL(addressLocation), new QName(
	TARGET_NAMESPACE, SERVICE_NAME));

    proxy = service.getPort(AuControlService.class);

    TitleConfig tcs[] = {
	makeTitleConfig("123"),
	makeTitleConfig("124")
    };

    Tdb tdb = new Tdb();
    for (TitleConfig tc : tcs) {
      tdb.addTdbAuFromProperties(tc.toProperties());
    }

    ConfigurationUtil.setTdb(tdb);

    auId0 = tcs[0].getAuId(pluginMgr);
    auId1 = tcs[1].getAuId(pluginMgr);

    // Add the archival units.
    remoteApi.addByAuId(auId0);
    remoteApi.addByAuId(auId1);

    mau = MockArchivalUnit.newInited(theDaemon);
    mau0 = (MockArchivalUnit)pluginMgr.getAuFromId(auId0);
    mau1 = (MockArchivalUnit)pluginMgr.getAuFromId(auId1);
  }

  /**
   * Tests the checking of substance of an Archival Unit by its identifier.
   */
  public void testCheckSubstanceById() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    CheckSubstanceResult result = proxy.checkSubstanceById("");
    assertEquals("", result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(AuControlServiceImpl.MISSING_AU_ID_ERROR_MESSAGE,
	result.getErrorMessage());

    result = proxy.checkSubstanceById(mau.getAuId());
    assertEquals(mau.getAuId(), result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(AuControlServiceImpl.NO_SUCH_AU_ERROR_MESSAGE,
	result.getErrorMessage());

    result = proxy.checkSubstanceById(mau0.getAuId());
    assertEquals(mau0.getAuId(), result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(AuControlServiceImpl.NO_SUBSTANCE_ERROR_MESSAGE,
	result.getErrorMessage());
    
    mau0.setSubstanceUrlPatterns(RegexpUtil.compileRegexps(ListUtil.list("one",
	"two" )));

    result = proxy.checkSubstanceById(mau0.getAuId());
    log.info("result = " + result);
    assertEquals(mau0.getAuId(), result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(AuControlServiceImpl.UNEXPECTED_ERROR_MESSAGE,
	result.getErrorMessage());

    mau0.addUrl("http://four/", false, true);
    mau0.addUrl("http://three/", false, true);
    mau0.populateAuCachedUrlSet();

    result = proxy.checkSubstanceById(mau0.getAuId());
    log.info("result = " + result);
    assertEquals(mau0.getAuId(), result.getId());
    assertNull(result.getOldState());
    assertEquals(CheckSubstanceResult.State.No, result.getNewState());
    assertNull(result.getErrorMessage());

    mau1.setSubstanceUrlPatterns(RegexpUtil.compileRegexps(ListUtil.list("one",
	"two" )));
    mau1.addUrl("http://two/", true, true);
    mau1.addUrl("http://three/", false, true);
    mau1.populateAuCachedUrlSet();

    result = proxy.checkSubstanceById(mau1.getAuId());
    log.info("result = " + result);
    assertEquals(mau1.getAuId(), result.getId());
    assertNull(result.getOldState());
    assertEquals(CheckSubstanceResult.State.Yes, result.getNewState());
    assertNull(result.getErrorMessage());

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      result = proxy.checkSubstanceById(mau.getAuId());
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
      result = proxy.checkSubstanceById(mau.getAuId());
      fail("Test should have failed for role "
	   + LockssServlet.ROLE_AU_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    try {
      result = proxy.checkSubstanceById(mau.getAuId());
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "debugRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_DEBUG);

    result = proxy.checkSubstanceById(mau.getAuId());
    assertEquals(mau.getAuId(), result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(AuControlServiceImpl.NO_SUCH_AU_ERROR_MESSAGE,
	result.getErrorMessage());
  }

  /**
   * Tests the checking of substance of Archival Units by a list of their
   * identifiers.
   */
  public void testCheckSubstanceByIdList() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    List<String> auIds = new ArrayList<String>();
    auIds.add(auId0);
    auIds.add(auId1);

    List<CheckSubstanceResult> results = proxy.checkSubstanceByIdList(auIds);
    CheckSubstanceResult result = results.get(0);
    assertEquals(auId0, result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(AuControlServiceImpl.NO_SUBSTANCE_ERROR_MESSAGE,
	result.getErrorMessage());

    result = results.get(1);
    assertEquals(auId1, result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(AuControlServiceImpl.NO_SUBSTANCE_ERROR_MESSAGE,
	result.getErrorMessage());

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);

    try {
      results = proxy.checkSubstanceByIdList(auIds);
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
      results = proxy.checkSubstanceByIdList(auIds);
      fail("Test should have failed for role " + LockssServlet.ROLE_AU_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);

    try {
      results = proxy.checkSubstanceByIdList(auIds);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "debugRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_DEBUG);

    results = proxy.checkSubstanceByIdList(auIds);
    result = results.get(0);
    assertEquals(auId0, result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(AuControlServiceImpl.NO_SUBSTANCE_ERROR_MESSAGE,
	result.getErrorMessage());

    result = results.get(1);
    assertEquals(auId1, result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(AuControlServiceImpl.NO_SUBSTANCE_ERROR_MESSAGE,
	result.getErrorMessage());
  }

  /**
   * Tests the crawl request of an Archival Unit.
   */
  public void testRequestCrawlById() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    RequestCrawlResult result = proxy.requestCrawlById("", null, false);
    assertEquals("", result.getId());
    assertFalse(result.isSuccess());
    assertNull(result.getDelayReason());
    assertEquals(AuControlServiceImpl.MISSING_AU_ID_ERROR_MESSAGE,
	result.getErrorMessage());

    result = proxy.requestCrawlById(mau.getAuId(), new Integer(10), true);
    assertEquals(mau.getAuId(), result.getId());
    assertFalse(result.isSuccess());
    assertNull(result.getDelayReason());
    assertEquals(AuControlServiceImpl.NO_SUCH_AU_ERROR_MESSAGE,
	result.getErrorMessage());

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      result = proxy.requestCrawlById(mau.getAuId(), null, false);
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
      result = proxy.requestCrawlById(mau.getAuId(), new Integer(10), true);
      fail("Test should have failed for role "
	   + LockssServlet.ROLE_AU_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    try {
      result = proxy.requestCrawlById(mau.getAuId(), null, false);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "debugRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_DEBUG);

    result = proxy.requestCrawlById(auId0, new Integer(10), true);
    assertEquals(auId0, result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getDelayReason());
    assertNull(result.getErrorMessage());
  }

  /**
   * Tests the crawl request of Archival Units by a list of their identifiers.
   */
  public void testRequestCrawlByIdList() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    List<String> auIds = new ArrayList<String>();
    auIds.add(auId0);
    auIds.add(auId1);

    List<RequestCrawlResult> results =
	proxy.requestCrawlByIdList(auIds, null, false);
    RequestCrawlResult result = results.get(0);
    assertEquals(auId0, result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getDelayReason());
    assertNull(result.getErrorMessage());

    result = results.get(1);
    assertEquals(auId1, result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getDelayReason());
    assertNull(result.getErrorMessage());

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      results = proxy.requestCrawlByIdList(auIds, null, false);
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
      results = proxy.requestCrawlByIdList(auIds, null, false);
      fail("Test should have failed for role "
	   + LockssServlet.ROLE_AU_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    try {
      results = proxy.requestCrawlByIdList(auIds, null, false);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "debugRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_DEBUG);

    results = proxy.requestCrawlByIdList(auIds, null, false);
    result = results.get(0);
    assertEquals(auId0, result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getDelayReason());
    assertNull(result.getErrorMessage());

    result = results.get(1);
    assertEquals(auId1, result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getDelayReason());
    assertNull(result.getErrorMessage());
  }

  private TitleConfig makeTitleConfig(String vol) {
    ConfigParamDescr d1 = new ConfigParamDescr("base_url");
    ConfigParamDescr d2 = new ConfigParamDescr("volume");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, BASE_URL);
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, vol);
    a1.setEditable(false);
    a2.setEditable(false);
    TitleConfig tc1 = new TitleConfig("a" + vol, plugin.getPluginId());
    tc1.setParams((List<ConfigParamAssignment>)ListUtil.list(a1, a2));
    tc1.setJournalTitle("jt");
    return tc1;
  }

  public static class MyMockPlugin extends MockPlugin {
    @Override
    protected ArchivalUnit createAu0(Configuration auConfig)
	throws ArchivalUnit.ConfigurationException {
      if ("666".equals(auConfig.get("volume"))) {
	throw new ArchivalUnit.ConfigurationException("bad config value");
      }
      return super.createAu0(auConfig);
    }
  }
}
