/*
 * $Id$
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
package org.lockss.ws.content;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.servlet.AdminServletManager;
import org.lockss.servlet.LockssServlet;
import org.lockss.servlet.ServletManager;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.ws.content.ContentConfigurationService;
import org.lockss.ws.cxf.AuthorizationInterceptor;
import org.lockss.ws.entities.ContentConfigurationResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * Functional test class for org.lockss.ws.content.ContentConfigurationService.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class FuncContentConfigurationService extends LockssTestCase {
  private static final String BASE_URL = "http://www.example.com/foo/";

  private static final String USER_NAME = "lockss-u";
  private static final String PASSWORD = "lockss-p";
  private static final String PASSWORD_SHA1 =
      "SHA1:ac4fc8fa9930a24c8d002d541c37ca993e1bc40f";
  private static final String TARGET_NAMESPACE =
      "http://content.ws.lockss.org/";
  private static final String SERVICE_NAME =
      "ContentConfigurationServiceImplService";

  private PluginManager pluginMgr;
  private MockPlugin plugin;
  private AccountManager accountManager;

  private ContentConfigurationService proxy;

  private String auId0;
  private String auId1;
  private String auId2;
  private String auId3;
  private String auId4;

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
    theDaemon.getRemoteApi().startService();
    theDaemon.getServletManager().startService();
    pluginMgr.startService();

    String key =
      PluginManager.pluginKeyFromName(MyMockPlugin.class.getName());
    pluginMgr.ensurePluginLoaded(key);
    plugin = (MockPlugin)pluginMgr.getPlugin(key);

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

    TitleConfig tcs[] = {
	makeTitleConfig("123"),
	makeTitleConfig("124"),
	makeTitleConfig("125"),
	makeTitleConfig("126"),
	makeTitleConfig("666"),
    };

    Tdb tdb = new Tdb();
    for (TitleConfig tc : tcs) {
      tdb.addTdbAuFromProperties(tc.toProperties());
    }

    ConfigurationUtil.setTdb(tdb);

    auId0 = tcs[0].getAuId(pluginMgr);
    auId1 = tcs[1].getAuId(pluginMgr);
    auId2 = tcs[2].getAuId(pluginMgr);
    auId3 = tcs[3].getAuId(pluginMgr);
    auId4 = tcs[4].getAuId(pluginMgr);
  }

  /**
   * Tests the addition of an Archival Unit by its identifier.
   */
  public void testAddAuById() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    ContentConfigurationResult result = proxy.addAuById(auId0);
    assertTrue(result.getIsSuccess());
    assertEquals(auId0, result.getId());
    assertNotNull(pluginMgr.getAuFromId(auId0));

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      result = proxy.addAuById(auId1);
      fail("Test should have failed for role "
	   + LockssServlet.ROLE_CONTENT_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "auAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);

    result = proxy.addAuById(auId2);
    assertTrue(result.getIsSuccess());
    assertEquals(auId2, result.getId());
    assertNotNull(pluginMgr.getAuFromId(auId0));

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    try {

      result = proxy.addAuById(auId3);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // Once more with an AU that can't be configured
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);

    result = proxy.addAuById(auId4);
    assertFalse(result.getIsSuccess());
    assertEquals("Error creating AU: bad config value", result.getMessage());
    assertNull(pluginMgr.getAuFromId(auId4));
  }

  /**
   * Tests the addition of Archival Units by a list of their identifiers.
   */
  public void testAddAusByIdList() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    List<String> auIds = new ArrayList<String>();
    auIds.add(auId0);
    auIds.add(auId1);
    auIds.add(auId2);

    List<ContentConfigurationResult> results = proxy.addAusByIdList(auIds);
    ContentConfigurationResult result = results.get(0);
    assertTrue(result.getIsSuccess());
    assertEquals(auId0, result.getId());
    assertNotNull(pluginMgr.getAuFromId(auId0));
    result = results.get(1);
    assertTrue(result.getIsSuccess());
    assertEquals(auId1, result.getId());
    assertNotNull(pluginMgr.getAuFromId(auId1));
    result = results.get(2);
    assertTrue(result.getIsSuccess());
    assertEquals(auId2, result.getId());
    assertNotNull(pluginMgr.getAuFromId(auId2));

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);

    auIds = new ArrayList<String>();
    auIds.add(auId3);
    auIds.add(auId4);

    try {
      results = proxy.addAusByIdList(auIds);
      fail("Test should have failed for role "
	   + LockssServlet.ROLE_CONTENT_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "auAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);

    results = proxy.addAusByIdList(auIds);
    result = results.get(0);
    assertTrue(result.getIsSuccess());
    assertEquals(auId3, result.getId());
    assertNotNull(pluginMgr.getAuFromId(auId3));
    result = results.get(1);
    assertFalse(result.getIsSuccess());
    assertEquals("Error creating AU: bad config value", result.getMessage());
    assertNull(pluginMgr.getAuFromId(auId4));
  }

  /**
   * Tests the removal of an Archival Unit by its identifier.
   */
  public void testDeleteAuById() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    ContentConfigurationResult result = proxy.addAuById(auId0);
    result = proxy.deleteAuById(auId0);
    assertTrue(result.getIsSuccess());
    assertEquals(auId0, result.getId());
    assertNull(pluginMgr.getAuFromId(auId0));

    result = proxy.addAuById(auId1);

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      result = proxy.deleteAuById(auId1);
      fail("Test should have failed for role "
	   + LockssServlet.ROLE_CONTENT_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "auAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);

    result = proxy.addAuById(auId2);
    result = proxy.deleteAuById(auId2);
    assertTrue(result.getIsSuccess());
    assertEquals(auId2, result.getId());
    assertNull(pluginMgr.getAuFromId(auId0));

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    try {

      result = proxy.deleteAuById(auId3);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "auAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);

    result = proxy.deleteAuById(auId3);
    assertFalse(result.getIsSuccess());
    assertEquals(auId3, result.getId());
    assertNull(pluginMgr.getAuFromId(auId3));
  }

  /**
   * Tests the removal of Archival Units by a list of their identifiers.
   */
  public void testDeleteAusByIdList() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    List<String> auIds = new ArrayList<String>();
    auIds.add(auId0);
    auIds.add(auId1);
    auIds.add(auId2);

    List<ContentConfigurationResult> results = proxy.addAusByIdList(auIds);
    results = proxy.deleteAusByIdList(auIds);
    ContentConfigurationResult result = results.get(0);
    assertTrue(result.getIsSuccess());
    assertEquals(auId0, result.getId());
    assertNull(pluginMgr.getAuFromId(auId0));
    result = results.get(1);
    assertTrue(result.getIsSuccess());
    assertEquals(auId1, result.getId());
    assertNull(pluginMgr.getAuFromId(auId1));
    result = results.get(2);
    assertTrue(result.getIsSuccess());
    assertEquals(auId2, result.getId());
    assertNull(pluginMgr.getAuFromId(auId2));

    auIds = new ArrayList<String>();
    auIds.add(auId3);
    auIds.add(auId4);

    results = proxy.addAusByIdList(auIds);

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);

    try {
      results = proxy.deleteAusByIdList(auIds);
      fail("Test should have failed for role "
	   + LockssServlet.ROLE_CONTENT_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "auAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);

    results = proxy.deleteAusByIdList(auIds);
    result = results.get(0);
    assertTrue(result.getIsSuccess());
    assertEquals(auId3, result.getId());
    assertNull(pluginMgr.getAuFromId(auId3));
    result = results.get(1);
    assertFalse(result.getIsSuccess());
    assertEquals(auId4, result.getId());
    assertNull(pluginMgr.getAuFromId(auId4));
  }

  /**
   * Tests the reactivation and deactivation of an Archival Unit by its
   * identifier.
   */
  public void testReactivateDeactivateAuById() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    ContentConfigurationResult result = proxy.addAuById(auId0);
    assertNotNull(pluginMgr.getAuFromId(auId0));
    assertFalse(pluginMgr.isInactiveAuId(auId0));
    result = proxy.deactivateAuById(auId0);
    assertTrue(result.getIsSuccess());
    assertEquals(auId0, result.getId());
    assertNull(pluginMgr.getAuFromId(auId0));
    assertTrue(pluginMgr.isInactiveAuId(auId0));
    result = proxy.reactivateAuById(auId0);
    assertTrue(result.getIsSuccess());
    assertEquals(auId0, result.getId());
    assertNotNull(pluginMgr.getAuFromId(auId0));
    assertFalse(pluginMgr.isInactiveAuId(auId0));
    result = proxy.reactivateAuById(auId0);
    assertFalse(result.getIsSuccess());
    assertEquals(auId0, result.getId());
    assertNotNull(pluginMgr.getAuFromId(auId0));
    assertFalse(pluginMgr.isInactiveAuId(auId0));

    result = proxy.addAuById(auId1);

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      result = proxy.deactivateAuById(auId1);
      fail("Test should have failed for role "
	   + LockssServlet.ROLE_CONTENT_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "auAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);

    result = proxy.addAuById(auId2);
    assertNotNull(pluginMgr.getAuFromId(auId2));
    assertFalse(pluginMgr.isInactiveAuId(auId2));
    result = proxy.deactivateAuById(auId2);
    assertTrue(result.getIsSuccess());
    assertEquals(auId2, result.getId());
    assertNull(pluginMgr.getAuFromId(auId2));
    assertTrue(pluginMgr.isInactiveAuId(auId2));
    result = proxy.deactivateAuById(auId2);
    assertFalse(result.getIsSuccess());
    assertEquals(auId2, result.getId());
    assertNull(pluginMgr.getAuFromId(auId2));
    assertTrue(pluginMgr.isInactiveAuId(auId2));

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    try {

      result = proxy.reactivateAuById(auId3);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }
  }

  /**
   * Tests the reactivation and deactivation of Archival Units by a list of
   * their identifiers.
   */
  public void testReactivateDeactivateAusByIdList() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    List<String> auIds = new ArrayList<String>();
    auIds.add(auId0);
    auIds.add(auId1);
    auIds.add(auId2);

    List<ContentConfigurationResult> results = proxy.addAusByIdList(auIds);
    assertNotNull(pluginMgr.getAuFromId(auId0));
    assertFalse(pluginMgr.isInactiveAuId(auId0));
    assertNotNull(pluginMgr.getAuFromId(auId1));
    assertFalse(pluginMgr.isInactiveAuId(auId1));
    assertNotNull(pluginMgr.getAuFromId(auId2));
    assertFalse(pluginMgr.isInactiveAuId(auId2));
    results = proxy.deactivateAusByIdList(auIds);
    ContentConfigurationResult result = results.get(0);
    assertTrue(result.getIsSuccess());
    assertEquals(auId0, result.getId());
    assertNull(pluginMgr.getAuFromId(auId0));
    assertTrue(pluginMgr.isInactiveAuId(auId0));
    result = results.get(1);
    assertTrue(result.getIsSuccess());
    assertEquals(auId1, result.getId());
    assertNull(pluginMgr.getAuFromId(auId1));
    assertTrue(pluginMgr.isInactiveAuId(auId1));
    result = results.get(2);
    assertTrue(result.getIsSuccess());
    assertEquals(auId2, result.getId());
    assertNull(pluginMgr.getAuFromId(auId2));
    assertTrue(pluginMgr.isInactiveAuId(auId2));

    auIds = new ArrayList<String>();
    auIds.add(auId3);
    auIds.add(auId4);

    results = proxy.addAusByIdList(auIds);

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);

    try {
      results = proxy.deleteAusByIdList(auIds);
      fail("Test should have failed for role "
	   + LockssServlet.ROLE_CONTENT_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "auAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);

    assertNotNull(pluginMgr.getAuFromId(auId3));
    assertFalse(pluginMgr.isInactiveAuId(auId3));
    assertNull(pluginMgr.getAuFromId(auId4));
    assertFalse(pluginMgr.isInactiveAuId(auId4));

    results = proxy.deactivateAusByIdList(auIds);
    result = results.get(0);
    assertTrue(result.getIsSuccess());
    assertEquals(auId3, result.getId());
    assertNull(pluginMgr.getAuFromId(auId3));
    assertTrue(pluginMgr.isInactiveAuId(auId3));
    result = results.get(1);
    assertFalse(result.getIsSuccess());
    assertEquals(auId4, result.getId());
    assertNull(pluginMgr.getAuFromId(auId4));
    assertFalse(pluginMgr.isInactiveAuId(auId4));
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
