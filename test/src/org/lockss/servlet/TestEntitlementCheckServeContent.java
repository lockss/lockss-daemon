/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.entitlement.EntitlementRegistryClient;
import org.lockss.entitlement.PublisherWorkflow;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockLockssUrlConnection;
import org.lockss.test.MockNodeManager;
import org.lockss.test.MockPlugin;
import org.lockss.test.StringInputStream;
import org.lockss.util.urlconn.LockssUrlConnection;
import org.lockss.util.urlconn.LockssUrlConnectionPool;

import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.servletunit.InvocationContext;
import org.mockito.Mockito;
import org.apache.commons.collections.map.MultiKeyMap;

public class TestEntitlementCheckServeContent extends LockssServletTestCase {

  private MockPluginManager pluginMgr = null;
  private EntitlementRegistryClient entitlementRegistryClient = null;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    pluginMgr = new MockPluginManager(theDaemon);
    theDaemon.setPluginManager(pluginMgr);
    theDaemon.setIdentityManager(new org.lockss.protocol.MockIdentityManager());
    entitlementRegistryClient = Mockito.mock(EntitlementRegistryClient.class);
    theDaemon.setCachedEntitlementRegistryClient(entitlementRegistryClient);
    theDaemon.getServletManager();
    theDaemon.setDaemonInited(true);
    theDaemon.setAusStarted(true);
    theDaemon.getRemoteApi().startService();

    pluginMgr.initService(theDaemon);
    pluginMgr.startService();

    ConfigurationUtil.addFromArgs(ConfigManager.PARAM_PLATFORM_PROJECT, "keepsafe");
    ConfigurationUtil.addFromArgs(EntitlementCheckServeContent.PARAM_MISSING_FILE_ACTION, "Redirect");
    ConfigurationUtil.addFromArgs(EntitlementCheckServeContent.PARAM_MOCK_SCOPE, "true");
    //ConfigurationUtil.addFromArgs("org.lockss.log.default.level", "debug3");
  }

  private MockArchivalUnit makeAu() throws Exception {
    return makeAu(null);
  }

  private MockArchivalUnit makeAu(Properties override) throws Exception {
    Plugin plugin = new MockPlugin(theDaemon);
    Tdb tdb = new Tdb();

    // Tdb with values for some metadata fields
    Properties tdbProps = new Properties();
    tdbProps.setProperty("title", "Air and Space Volume 1");
    tdbProps.setProperty("journalTitle", "Air and Space");
    tdbProps.setProperty("attributes.isbn", "976-1-58562-317-7");
    tdbProps.setProperty("issn", "0740-2783");
    tdbProps.setProperty("eissn", "0740-2783");
    tdbProps.setProperty("attributes.year", "2014");
    tdbProps.setProperty("attributes.publisher", "Wiley");
    tdbProps.setProperty("attributes.provider", "Provider[10.0135/12345678]");

    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://publisher.org/test_journal/");
    tdbProps.setProperty("param.2.key", "volume");
    tdbProps.setProperty("param.2.value", "vol1");
    tdbProps.setProperty("plugin", plugin.getClass().toString());

    if(override != null) {
      tdbProps.putAll(override);
    }

    TdbAu tdbAu = tdb.addTdbAuFromProperties(tdbProps);
    TitleConfig titleConfig = new TitleConfig(tdbAu, plugin);
    MockArchivalUnit au = new MockArchivalUnit(plugin, "TestAU");
    au.setStartUrls(Arrays.asList("http://publisher.org/test_journal/"));
    au.setTitleConfig(titleConfig);
    return au;
  }

  protected void initServletRunner() {
    super.initServletRunner();
    sRunner.setServletContextAttribute(ServletManager.CONTEXT_ATTR_SERVLET_MGR, new ContentServletManager());
    sRunner.registerServlet("/EntitlementCheckServeContent", MockEntitlementCheckServeContent.class.getName() );
    sRunner.registerServlet("/test_journal/", RedirectServlet.class.getName());
  }

  public void testIndex() throws Exception {
    initServletRunner();
    pluginMgr.addAu(makeAu(), null);
    WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent" );
    InvocationContext ic = sClient.newInvocation(request);
    EntitlementCheckServeContent snsc = (EntitlementCheckServeContent) ic.getServlet();

    WebResponse resp1 = sClient.getResponse(request);
    assertResponseOk(resp1);
    assertEquals("content type", "text/html", resp1.getContentType());
    WebTable auTable = resp1.getTableStartingWith("Archival Unit");
    assertNotNull(auTable);
    assertEquals(2, auTable.getRowCount());
    assertEquals(3, auTable.getColumnCount());
    assertEquals("MockAU", auTable.getCellAsText(1, 0));
    assertEquals("/ServeContent?url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F&auid=TestAU", auTable.getTableCell(1, 1).getLinks()[0].getURLString());
  }

  public void testMissingUrl() throws Exception {
    initServletRunner();
    pluginMgr.addAu(makeAu(), null);
    sClient.setExceptionsThrownOnErrorStatus(false);
    WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent?scope=ed.ac.uk&url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F" );
    InvocationContext ic = sClient.newInvocation(request);
    EntitlementCheckServeContent snsc = (EntitlementCheckServeContent) ic.getServlet();

    WebResponse resp1 = sClient.getResponse(request);
    assertResponseOk(resp1);
    assertEquals("<html><head><title>Blah</title></head><body>Redirected content</body></html>", resp1.getText());
  }

  public void testMissingUrlExplicitAU() throws Exception {
    initServletRunner();
    pluginMgr.addAu(makeAu(), null);
    sClient.setExceptionsThrownOnErrorStatus(false);
    WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent?scope=ed.ac.uk&url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F&auid=TestAU" );
    InvocationContext ic = sClient.newInvocation(request);
    EntitlementCheckServeContent snsc = (EntitlementCheckServeContent) ic.getServlet();

    WebResponse resp1 = sClient.getResponse(request);
    assertResponseOk(resp1);
    assertEquals("<html><head><title>Blah</title></head><body>Redirected content</body></html>", resp1.getText());
  }

  public void testCachedUrlPrimaryPublisherResponse() throws Exception {
    initServletRunner();
    pluginMgr.addAu(makeAu());
    Mockito.when(entitlementRegistryClient.getInstitution("ed.ac.uk")).thenReturn("03bd5fc6-97f0-11e4-b270-8932ea886a12");
    Mockito.when(entitlementRegistryClient.isUserEntitled("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn(true);
    Mockito.when(entitlementRegistryClient.getPublisher("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn("33333333-0000-0000-0000-000000000000");
    Mockito.when(entitlementRegistryClient.getPublisherWorkflow("33333333-0000-0000-0000-000000000000")).thenReturn(PublisherWorkflow.PRIMARY_PUBLISHER);
    WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent?scope=ed.ac.uk&url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F" );
    InvocationContext ic = sClient.newInvocation(request);
    MockEntitlementCheckServeContent snsc = (MockEntitlementCheckServeContent) ic.getServlet();
    LockssUrlConnection connection = mockConnection(200, "<html><head><title>Blah</title></head><body>Fetched content</body></html>");
    snsc.expectRequest("http://publisher.org/test_journal/", connection);

    WebResponse resp1 = sClient.getResponse(request);
    assertResponseOk(resp1);
    assertEquals("<html><head><title>Blah</title></head><body>Fetched content</body></html>", resp1.getText());
    Mockito.verify(connection).addRequestProperty("X-Forwarded-For", "127.0.0.1");
    Mockito.verify(connection).addRequestProperty("X-Lockss-Institution", "ed.ac.uk");
  }

  public void testCachedUrlPrimaryPublisherError() throws Exception {
    initServletRunner();
    pluginMgr.addAu(makeAu());
    Mockito.when(entitlementRegistryClient.getInstitution("ed.ac.uk")).thenReturn("03bd5fc6-97f0-11e4-b270-8932ea886a12");
    Mockito.when(entitlementRegistryClient.isUserEntitled("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn(true);
    Mockito.when(entitlementRegistryClient.getPublisher("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn("33333333-0000-0000-0000-000000000000");
    Mockito.when(entitlementRegistryClient.getPublisherWorkflow("33333333-0000-0000-0000-000000000000")).thenReturn(PublisherWorkflow.PRIMARY_PUBLISHER);
    WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent?scope=ed.ac.uk&url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F" );
    InvocationContext ic = sClient.newInvocation(request);
    MockEntitlementCheckServeContent snsc = (MockEntitlementCheckServeContent) ic.getServlet();
    LockssUrlConnection connection = mockConnection(403, "<html><head><title>Blah</title></head><body>Content refused</body></html>");
    snsc.expectRequest("http://publisher.org/test_journal/", connection);

    WebResponse resp1 = sClient.getResponse(request);
    assertResponseOk(resp1);
    assertEquals("<html><head><title>Blah</title></head><body>Cached content</body></html>", resp1.getText());
    Mockito.verify(connection).addRequestProperty("X-Forwarded-For", "127.0.0.1");
    Mockito.verify(connection).addRequestProperty("X-Lockss-Institution", "ed.ac.uk");
  }

  public void testCachedUrlPrimaryLockss() throws Exception {
    initServletRunner();
    pluginMgr.addAu(makeAu());
    Mockito.when(entitlementRegistryClient.getInstitution("ed.ac.uk")).thenReturn("03bd5fc6-97f0-11e4-b270-8932ea886a12");
    Mockito.when(entitlementRegistryClient.isUserEntitled("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn(true);
    Mockito.when(entitlementRegistryClient.getPublisher("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn("33333333-0000-0000-0000-000000000000");
    Mockito.when(entitlementRegistryClient.getPublisherWorkflow("33333333-0000-0000-0000-000000000000")).thenReturn(PublisherWorkflow.PRIMARY_LOCKSS);
    WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent?scope=ed.ac.uk&url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F" );
    InvocationContext ic = sClient.newInvocation(request);
    EntitlementCheckServeContent snsc = (EntitlementCheckServeContent) ic.getServlet();

    WebResponse resp1 = sClient.getResponse(request);
    assertResponseOk(resp1);
    assertEquals("<html><head><title>Blah</title></head><body>Cached content</body></html>", resp1.getText());
  }

  public void testCachedUrlLibraryNotification() throws Exception {
    initServletRunner();
    pluginMgr.addAu(makeAu());
    Mockito.when(entitlementRegistryClient.getInstitution("ed.ac.uk")).thenReturn("03bd5fc6-97f0-11e4-b270-8932ea886a12");
    Mockito.when(entitlementRegistryClient.isUserEntitled("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn(true);
    Mockito.when(entitlementRegistryClient.getPublisher("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn("33333333-0000-0000-0000-000000000000");
    Mockito.when(entitlementRegistryClient.getPublisherWorkflow("33333333-0000-0000-0000-000000000000")).thenReturn(PublisherWorkflow.LIBRARY_NOTIFICATION);
    sClient.setExceptionsThrownOnErrorStatus(false);
    WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent?scope=ed.ac.uk&url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F" );
    InvocationContext ic = sClient.newInvocation(request);
    EntitlementCheckServeContent snsc = (EntitlementCheckServeContent) ic.getServlet();

    WebResponse resp1 = sClient.getResponse(request);
    assertEquals(403, resp1.getResponseCode());
    assertTrue(resp1.getText().contains("<p>You are not authorised to access the requested URL on this LOCKSS box. Select link<sup><font size=-1><a href=#foottag1>1</a></font></sup> to view it at the publisher:</p><a href=\"http://publisher.org/test_journal/\">http://publisher.org/test_journal/</a>"));
  }

  public void testUnauthorisedUrl() throws Exception {
    initServletRunner();
    pluginMgr.addAu(makeAu());
    Mockito.when(entitlementRegistryClient.getInstitution("ed.ac.uk")).thenReturn("03bd5fc6-97f0-11e4-b270-8932ea886a12");
    Mockito.when(entitlementRegistryClient.isUserEntitled("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn(false);
    sClient.setExceptionsThrownOnErrorStatus(false);
    WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent?scope=ed.ac.uk&url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F" );
    InvocationContext ic = sClient.newInvocation(request);
    EntitlementCheckServeContent snsc = (EntitlementCheckServeContent) ic.getServlet();

    WebResponse resp1 = sClient.getResponse(request);
    System.out.println(resp1.getText());
    assertEquals(403, resp1.getResponseCode());
    assertTrue(resp1.getText().contains("<p>You are not authorised to access the requested URL on this LOCKSS box. Select link<sup><font size=-1><a href=#foottag1>1</a></font></sup> to view it at the publisher:</p><a href=\"http://publisher.org/test_journal/\">http://publisher.org/test_journal/</a>"));
  }

  public void testEntitlementRegistryError() throws Exception {
    initServletRunner();
    pluginMgr.addAu(makeAu());
    Mockito.when(entitlementRegistryClient.getInstitution("ed.ac.uk")).thenReturn("03bd5fc6-97f0-11e4-b270-8932ea886a12");
    Mockito.when(entitlementRegistryClient.isUserEntitled("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenThrow(new IOException("Could not contact entitlement registry"));
    sClient.setExceptionsThrownOnErrorStatus(false);
    WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent?scope=ed.ac.uk&url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F" );
    InvocationContext ic = sClient.newInvocation(request);
    EntitlementCheckServeContent snsc = (EntitlementCheckServeContent) ic.getServlet();

    WebResponse resp1 = sClient.getResponse(request);
    assertEquals(503, resp1.getResponseCode());
    assertTrue(resp1.getText().contains("<p>An error occurred trying to access the requested URL on this LOCKSS box. This may be temporary and you may wish to report this, and try again later. Select link<sup><font size=-1><a href=#foottag1>1</a></font></sup> to view it at the publisher:</p><a href=\"http://publisher.org/test_journal/\">http://publisher.org/test_journal/</a>"));
  }

  public void testInvalidArchivalUnit() throws Exception {
    initServletRunner();
    Properties props = new Properties();
    props.setProperty("issn", "");
    props.setProperty("eissn", "");
    sClient.setExceptionsThrownOnErrorStatus(false);
    pluginMgr.addAu(makeAu(props));
    WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent?scope=ed.ac.uk&url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F" );
    InvocationContext ic = sClient.newInvocation(request);
    EntitlementCheckServeContent snsc = (EntitlementCheckServeContent) ic.getServlet();

    WebResponse resp1 = sClient.getResponse(request);
    assertEquals(404, resp1.getResponseCode());
    assertTrue(resp1.getText().contains("<p>The requested URL is not preserved on this LOCKSS box. Select link<sup><font size=-1><a href=#foottag1>1</a></font></sup> to view it at the publisher:</p><a href=\"http://publisher.org/test_journal/\">http://publisher.org/test_journal/</a>"));
  }

  // Test no longer valid due to changes to how we extract bibliographic data
  // public void testCachedUrlMultipleAUs() throws Exception {
  //   initServletRunner();
  //   pluginMgr.addAu(makeAu());
  //   Properties props = new Properties();
  //   props.setProperty("issn", "0000-0000");
  //   props.setProperty("eissn", "0000-0000");
  //   pluginMgr.addAu(makeAu(props));
  //   Mockito.when(entitlementRegistryClient.getInstitution("ed.ac.uk")).thenReturn("03bd5fc6-97f0-11e4-b270-8932ea886a12");
  //   Mockito.when(entitlementRegistryClient.isUserEntitled("0740-2783", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn(false);
  //   Mockito.when(entitlementRegistryClient.isUserEntitled("0000-0000", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn(true);
  //   Mockito.when(entitlementRegistryClient.getPublisher("0000-0000", "03bd5fc6-97f0-11e4-b270-8932ea886a12", "20140101", "20141231")).thenReturn("33333333-0000-0000-0000-000000000000");
  //   Mockito.when(entitlementRegistryClient.getPublisherWorkflow("33333333-0000-0000-0000-00000000000000")).thenReturn(PublisherWorkflow.PRIMARY_PUBLISHER);
  //   WebRequest request = new GetMethodWebRequest("http://null/EntitlementCheckServeContent?scope=ed.ac.uk&url=http%3A%2F%2Fpublisher.org%2Ftest_journal%2F" );
  //   InvocationContext ic = sClient.newInvocation(request);
  //   MockEntitlementCheckServeContent snsc = (MockEntitlementCheckServeContent) ic.getServlet();
  //   LockssUrlConnection connection = mockConnection(200, "<html><head><title>Blah</title></head><body>Fetched content</body></html>");
  //   snsc.expectRequest("http://publisher.org/test_journal/", connection);

  //   WebResponse resp1 = sClient.getResponse(request);
  //   assertResponseOk(resp1);
  //   assertEquals("<html><head><title>Blah</title></head><body>Fetched content</body></html>", resp1.getText());
  //   Mockito.verify(connection).addRequestProperty("X-Forwarded-For", "127.0.0.1");
  //   Mockito.verify(connection).addRequestProperty("X-Lockss-Institution", "ed.ac.uk");
  // }

  private static class MockPluginManager extends PluginManager {
    private Map<ArchivalUnit, List<String>> aus;
    private MockLockssDaemon theDaemon;
    private MockNodeManager nodeManager;

    public MockPluginManager(MockLockssDaemon theDaemon) {
      this.aus = new HashMap<ArchivalUnit, List<String>>();
      this.theDaemon = theDaemon;
      this.nodeManager = new MockNodeManager();
    }

    public void addAu(ArchivalUnit au) {
      aus.put(au, new ArrayList<String>(au.getStartUrls()));
      theDaemon.setNodeManager(nodeManager, au);
    }

    public void addAu(ArchivalUnit au, List<String> urls) {
      aus.put(au, urls);
      theDaemon.setNodeManager(nodeManager, au);
    }

    @Override
    public List<ArchivalUnit> getAllAus() {
      return new ArrayList<ArchivalUnit>(aus.keySet());
    }

    @Override
    public List<CachedUrl> findCachedUrls(String url, CuContentReq req) {
      return this.findCachedUrls(url);
    }

    @Override
    public List<CachedUrl> findCachedUrls(String url) {
      List<CachedUrl> cached = new ArrayList<CachedUrl>();
      for(ArchivalUnit au : aus.keySet()) {
        List<String> urls = aus.get(au);
        if(urls != null && urls.contains(url)) {
          MockCachedUrl cu = new MockCachedUrl(url, au);
          cu.addProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
          cu.setContent("<html><head><title>Blah</title></head><body>Cached content</body></html>");
          cached.add(cu);
        }
      }
      return cached;
    }
  }

  public static class RedirectServlet extends HttpServlet {
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      resp.getWriter().print("<html><head><title>Blah</title></head><body>Redirected content</body></html>");
    }
  }

  private LockssUrlConnection mockConnection(int responseCode, String response) throws IOException {
      LockssUrlConnection connection = Mockito.mock(LockssUrlConnection.class);
      Mockito.when(connection.getResponseCode()).thenReturn(responseCode);
      Mockito.when(connection.getResponseInputStream()).thenReturn(new StringInputStream(response));
      return connection;
  }

  public static class MockEntitlementCheckServeContent extends EntitlementCheckServeContent {
    private EntitlementCheckServeContent delegate = Mockito.mock(EntitlementCheckServeContent.class);

    public void expectRequest(String url, LockssUrlConnection connection) throws IOException {
      Mockito.when(delegate.doOpenConnection(Mockito.eq(url), Mockito.any(LockssUrlConnectionPool.class))).thenReturn(connection);
    }

    protected LockssUrlConnection doOpenConnection(String url, LockssUrlConnectionPool pool) throws IOException {
      return delegate.doOpenConnection(url, pool);
    }
  }
}
