/*

 Copyright (c) 2015-2018 Board of Trustees of Leland Stanford Jr. University,
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

import static org.lockss.db.SqlConstants.*;
import static org.lockss.ws.control.AuControlServiceImpl.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.crawler.CrawlManagerImpl;
import org.lockss.daemon.ConfigParamAssignment;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.TitleConfig;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.metadata.TestMetadataManager.MySimulatedPlugin0;
import org.lockss.metadata.TestMetadataManager.MySimulatedPlugin1;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.remote.RemoteApi;
import org.lockss.servlet.AdminServletManager;
import org.lockss.servlet.LockssServlet;
import org.lockss.servlet.ServletManager;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.util.RegexpUtil;
import org.lockss.ws.cxf.AuthorizationInterceptor;
import org.lockss.ws.entities.CheckSubstanceResult;
import org.lockss.ws.entities.LockssWebServicesFault;
import org.lockss.ws.entities.RequestCrawlResult;
import org.lockss.ws.entities.RequestAuControlResult;

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

  private MockLockssDaemon theDaemon;
  private PluginManager pluginMgr;
  private MockPlugin plugin;
  private AccountManager accountManager;
  private RemoteApi remoteApi;
  private DbManager dbManager;
  private MetadataManager metadataManager;

  /** set of AuIds of AUs reindexed by the MetadataManager */
  Set<String> ausReindexed = new HashSet<String>();

  private AuControlService proxy;

  private String auId0;
  private String auId1;
  private MockArchivalUnit mau;
  private MockArchivalUnit mau0;
  private MockArchivalUnit mau1;

  private SimulatedArchivalUnit sau0, sau1/*, sau2, sau3, sau4*/;

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = setUpDiskSpace();

    int port = TcpTestUtil.findUnboundTcpPort();
    ConfigurationUtil.addFromArgs(AdminServletManager.PARAM_PORT, "" + port,
	ServletManager.PARAM_PLATFORM_USERNAME, USER_NAME,
	ServletManager.PARAM_PLATFORM_PASSWORD, PASSWORD_SHA1);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();

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

    sau0 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin0.class,
                                              simAuConfig(tempDirPath + "/0"));
    sau1 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin1.class,
                                              simAuConfig(tempDirPath + "/1"));
    PluginTestUtil.crawlSimAu(sau0);
    PluginTestUtil.crawlSimAu(sau1);

    // Reset the set of reindexed AUs.
    ausReindexed.clear();

    // Initialize the database manager.
    dbManager = getTestDbManager(tempDirPath);

    metadataManager = new MetadataManager() {
      /**
       * Notify listeners that an AU is being reindexed.
       * 
       * @param au
       */
      protected void notifyStartReindexingAu(ArchivalUnit au) {
        log.info("Start reindexing au " + au);
      }
      
      /**
       * Notify listeners that an AU is finshed being reindexed.
       * 
       * @param au
       */
      protected void notifyFinishReindexingAu(ArchivalUnit au,
	  ReindexingStatus status) {
        log.info("Finished reindexing au (" + status + ") " + au);
        if (status != ReindexingStatus.Rescheduled) {
          synchronized (ausReindexed) {
            ausReindexed.add(au.getAuId());
            ausReindexed.notifyAll();
          }
        }
      }
    };

    theDaemon.setMetadataManager(metadataManager);
    metadataManager.initService(theDaemon);
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

    PluginTestUtil.startAu(mau);
    PluginTestUtil.startAu(mau0);
    PluginTestUtil.startAu(mau1);
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
    assertEquals(MISSING_AU_ID_ERROR_MESSAGE, result.getErrorMessage());

    result = proxy.checkSubstanceById(mau.getAuId());
    assertEquals(mau.getAuId(), result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(NO_SUCH_AU_ERROR_MESSAGE, result.getErrorMessage());

    result = proxy.checkSubstanceById(mau0.getAuId());
    assertEquals(mau0.getAuId(), result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(NO_SUBSTANCE_ERROR_MESSAGE, result.getErrorMessage());

    mau0.setSubstanceUrlPatterns(RegexpUtil.compileRegexps(ListUtil.list("one",
	"two" )));

    result = proxy.checkSubstanceById(mau0.getAuId());
    log.info("result = " + result);
    assertEquals(mau0.getAuId(), result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(UNEXPECTED_SUBSTANCE_CHECKER_ERROR_MESSAGE,
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
      fail("Test should have failed for role " + LockssServlet.ROLE_AU_ADMIN);
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
    assertEquals(NO_SUCH_AU_ERROR_MESSAGE, result.getErrorMessage());
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
    assertEquals(NO_SUBSTANCE_ERROR_MESSAGE, result.getErrorMessage());

    result = results.get(1);
    assertEquals(auId1, result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(NO_SUBSTANCE_ERROR_MESSAGE, result.getErrorMessage());

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
    assertEquals(NO_SUBSTANCE_ERROR_MESSAGE, result.getErrorMessage());

    result = results.get(1);
    assertEquals(auId1, result.getId());
    assertNull(result.getOldState());
    assertNull(result.getNewState());
    assertEquals(NO_SUBSTANCE_ERROR_MESSAGE, result.getErrorMessage());
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
    assertEquals(MISSING_AU_ID_ERROR_MESSAGE, result.getErrorMessage());

    result = proxy.requestCrawlById(mau.getAuId(), new Integer(10), true);
    assertEquals(mau.getAuId(), result.getId());
    assertFalse(result.isSuccess());
    assertNull(result.getDelayReason());
    assertEquals(NO_SUCH_AU_ERROR_MESSAGE, result.getErrorMessage());

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
      fail("Test should have failed for role " + LockssServlet.ROLE_AU_ADMIN);
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
      fail("Test should have failed for role " + LockssServlet.ROLE_AU_ADMIN);
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

  /**
   * Tests the polling request of an Archival Unit.
   */
  public void testRequestPollById() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    RequestAuControlResult result = proxy.requestPollById("");
    assertEquals("", result.getId());
    assertFalse(result.isSuccess());
    assertEquals(MISSING_AU_ID_ERROR_MESSAGE, result.getErrorMessage());

    result = proxy.requestPollById(mau.getAuId());
    assertEquals(mau.getAuId(), result.getId());
    assertFalse(result.isSuccess());
    assertEquals(NO_SUCH_AU_ERROR_MESSAGE, result.getErrorMessage());

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      result = proxy.requestPollById(mau.getAuId());
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
      result = proxy.requestPollById(mau.getAuId());
      fail("Test should have failed for role " + LockssServlet.ROLE_AU_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    try {
      result = proxy.requestPollById(mau.getAuId());
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "debugRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_DEBUG);

    result = proxy.requestPollById(auId0);
    assertEquals(auId0, result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getErrorMessage());
  }

  /**
   * Tests the polling request of Archival Units by a list of their identifiers.
   */
  public void testRequestPollByIdList() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    List<String> auIds = new ArrayList<String>();
    auIds.add(auId0);
    auIds.add(auId1);

    List<RequestAuControlResult> results = proxy.requestPollByIdList(auIds);
    RequestAuControlResult result = results.get(0);
    assertEquals(auId0, result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getErrorMessage());

    result = results.get(1);
    assertEquals(auId1, result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getErrorMessage());

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      results = proxy.requestPollByIdList(auIds);
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
      results = proxy.requestPollByIdList(auIds);
      fail("Test should have failed for role " + LockssServlet.ROLE_AU_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    try {
      results = proxy.requestPollByIdList(auIds);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "debugRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_DEBUG);

    results = proxy.requestPollByIdList(auIds);
    result = results.get(0);
    assertEquals(auId0, result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getErrorMessage());

    result = results.get(1);
    assertEquals(auId1, result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getErrorMessage());
  }

  /**
   * Tests the metadata indexing operations on an Archival Unit.
   */
  public void testMdIndexingById() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    RequestAuControlResult result = proxy.requestMdIndexingById("", false);
    assertEquals("", result.getId());
    assertFalse(result.isSuccess());
    assertEquals(DISABLED_METADATA_PROCESSING_ERROR_MESSAGE,
	result.getErrorMessage());

    pluginMgr.deleteAu(mau);
    pluginMgr.deleteAu(mau0);
    pluginMgr.deleteAu(mau1);

    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEXING_ENABLED,
	"true");

    metadataManager.startService();
    
    int expectedAuCount = 2;
    assertEquals(expectedAuCount, pluginMgr.getAllAus().size());

    long maxWaitTime = expectedAuCount * 10000; // 10 sec. per au
    int ausCount = waitForReindexing(expectedAuCount, maxWaitTime);
    assertEquals(expectedAuCount, ausCount);

    result = proxy.requestMdIndexingById("", false);
    assertEquals("", result.getId());
    assertFalse(result.isSuccess());
    assertEquals(MISSING_AU_ID_ERROR_MESSAGE, result.getErrorMessage());

    result = proxy.requestMdIndexingById(mau.getAuId(), true);
    assertEquals(mau.getAuId(), result.getId());
    assertFalse(result.isSuccess());
    assertEquals(NO_SUCH_AU_ERROR_MESSAGE, result.getErrorMessage());

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      result = proxy.requestMdIndexingById(mau.getAuId(), false);
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
      result = proxy.requestMdIndexingById(mau.getAuId(), true);
      fail("Test should have failed for role " + LockssServlet.ROLE_AU_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    try {
      result = proxy.requestMdIndexingById(mau.getAuId(), false);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "debugRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_DEBUG);

    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      assertEquals(0, countDisabledAus(conn));

      result = proxy.disableMdIndexingById(sau0.getAuId());
      assertEquals(sau0.getAuId(), result.getId());
      assertTrue(result.isSuccess());
      assertNull(result.getErrorMessage());

      assertEquals(1, countDisabledAus(conn));

      result = proxy.disableMdIndexingById(sau1.getAuId());
      assertEquals(sau1.getAuId(), result.getId());
      assertTrue(result.isSuccess());
      assertNull(result.getErrorMessage());

      assertEquals(2, countDisabledAus(conn));

      result = proxy.enableMdIndexingById(sau0.getAuId());
      assertEquals(sau0.getAuId(), result.getId());
      assertTrue(result.isSuccess());
      assertNull(result.getErrorMessage());

      assertEquals(1, countDisabledAus(conn));

      result = proxy.enableMdIndexingById(sau1.getAuId());
      assertEquals(sau1.getAuId(), result.getId());
      assertTrue(result.isSuccess());
      assertNull(result.getErrorMessage());

      assertEquals(0, countDisabledAus(conn));
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    result = proxy.requestMdIndexingById(sau0.getAuId(), false);
    assertEquals(sau0.getAuId(), result.getId());
    assertFalse(result.isSuccess());
    assertTrue(result.getErrorMessage().startsWith("Unknown substance"));
    assertTrue(result.getErrorMessage().endsWith(USE_FORCE_MESSAGE));

    result = proxy.requestMdIndexingById(sau0.getAuId(), true);
    assertEquals(sau0.getAuId(), result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getErrorMessage());

    TimerUtil.guaranteedSleep(1000);
    metadataManager.stopService();
  }

  /**
   * Tests the metadata indexing operations on Archival Units by a list of their
   * identifiers.
   */
  public void testMdIndexingByIdList() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    pluginMgr.deleteAu(mau);
    pluginMgr.deleteAu(mau0);
    pluginMgr.deleteAu(mau1);

    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEXING_ENABLED,
	"true");

    metadataManager.startService();
    
    int expectedAuCount = 2;
    assertEquals(expectedAuCount, pluginMgr.getAllAus().size());

    long maxWaitTime = expectedAuCount * 10000; // 10 sec. per au
    int ausCount = waitForReindexing(expectedAuCount, maxWaitTime);
    assertEquals(expectedAuCount, ausCount);

    List<String> auIds = new ArrayList<String>();
    auIds.add(sau0.getAuId());
    auIds.add(sau1.getAuId());

    List<RequestAuControlResult> results =
	proxy.requestMdIndexingByIdList(auIds, false);
    RequestAuControlResult result = results.get(0);
    assertFalse(result.isSuccess());
    assertTrue(result.getErrorMessage().startsWith("Unknown substance"));
    assertTrue(result.getErrorMessage().endsWith(USE_FORCE_MESSAGE));

    result = results.get(1);
    assertEquals(sau1.getAuId(), result.getId());
    assertFalse(result.isSuccess());
    assertTrue(result.getErrorMessage().startsWith("Unknown substance"));
    assertTrue(result.getErrorMessage().endsWith(USE_FORCE_MESSAGE));

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      results = proxy.requestMdIndexingByIdList(auIds, true);
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
      results = proxy.requestMdIndexingByIdList(auIds, false);
      fail("Test should have failed for role " + LockssServlet.ROLE_AU_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "accessContentRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    try {
      results = proxy.requestMdIndexingByIdList(auIds, true);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ACCESS);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "debugRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_DEBUG);

    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      assertEquals(0, countDisabledAus(conn));

      results = proxy.disableMdIndexingByIdList(auIds);
      result = results.get(0);
      assertEquals(sau0.getAuId(), result.getId());
      assertTrue(result.isSuccess());
      assertNull(result.getErrorMessage());

      result = results.get(1);
      assertEquals(sau1.getAuId(), result.getId());
      assertTrue(result.isSuccess());
      assertNull(result.getErrorMessage());

      assertEquals(2, countDisabledAus(conn));

      results = proxy.enableMdIndexingByIdList(auIds);
      result = results.get(0);
      assertEquals(sau0.getAuId(), result.getId());
      assertTrue(result.isSuccess());
      assertNull(result.getErrorMessage());

      result = results.get(1);
      assertEquals(sau1.getAuId(), result.getId());
      assertTrue(result.isSuccess());
      assertNull(result.getErrorMessage());

      assertEquals(0, countDisabledAus(conn));
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    results = proxy.requestMdIndexingByIdList(auIds, true);
    result = results.get(0);
    assertEquals(sau0.getAuId(), result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getErrorMessage());

    result = results.get(1);
    assertEquals(sau1.getAuId(), result.getId());
    assertTrue(result.isSuccess());
    assertNull(result.getErrorMessage());

    TimerUtil.guaranteedSleep(1000);
    metadataManager.stopService();
  }

  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
                                SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
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

  /**
   * Waits a specified period for a specified number of AUs to finish 
   * being reindexed.  Returns the actual number of AUs reindexed.
   * 
   * @param auCount the expected AU count
   * @param maxWaitTime the maximum time to wait
   * @return the number of AUs reindexed
   */
  private int waitForReindexing(int auCount, long maxWaitTime) {
    long startTime = System.currentTimeMillis();
    synchronized (ausReindexed) {
      while ((System.currentTimeMillis()-startTime < maxWaitTime) 
             && (ausReindexed.size() < auCount)) {
        try {
          ausReindexed.wait(maxWaitTime);
        } catch (InterruptedException ex) {
        }
      }
    }
    return ausReindexed.size();
  }

  private int countDisabledAus(Connection conn)
      throws SQLException, DbException {
    int count = -1;
    PreparedStatement stmt = null;

    try {
      String query = "select count(*) from " + PENDING_AU_TABLE
	  + " where " + PRIORITY_COLUMN + " < 0";
      stmt = dbManager.prepareStatement(conn, query);
      ResultSet resultSet = dbManager.executeQuery(stmt);

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      DbManager.safeCloseStatement(stmt);
    }

    return count;
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
