/*
 * $Id: TestAuConfig.java,v 1.10 2008-07-11 08:21:38 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.servlet.*;
import org.lockss.repository.*;
import com.meterware.servletunit.*;
import com.meterware.httpunit.*;

/**
 * This is the test class for org.lockss.servlet.AuConfig
 */
public class TestAuConfig extends LockssServletTestCase {
  static Logger log = Logger.getLogger("TestAuConfig");

  private MockArchivalUnit mau = null;
  private PluginManager pluginMgr = null;

  public void setUp() throws Exception {
    super.setUp();
    pluginMgr = new PluginManager();
    theDaemon.setPluginManager(pluginMgr);
    theDaemon.setIdentityManager(new org.lockss.protocol.MockIdentityManager());
    theDaemon.getServletManager();
    theDaemon.setDaemonInited(true);
    theDaemon.getRemoteApi().startService();
    pluginMgr.initService(theDaemon);
    pluginMgr.startService();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION,
                      tempDirPath);
    props.setProperty(AdminServletManager.PARAM_START, "false");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    mau = new MockArchivalUnit();
  }

  protected void initServletRunner() {
    super.initServletRunner();
    sRunner.registerServlet("/AuConfig", AuConfig.class.getName() );
  }


  public void testBeforeAusLoaded() throws Exception {
    initServletRunner();
    WebRequest request =
      new GetMethodWebRequest("http://null/AuConfig" );
    WebResponse resp1 = sClient.getResponse(request);
    log.debug2("Response 1: " + resp1.getText());
    assertResponseOk(resp1);
    assertEquals("Content type", "text/html", resp1.getContentType());

    WebForm auForm = resp1.getFormWithID("AuSummaryForm");
    WebTable auTable = resp1.getTableWithID("AuSummaryTable");
    assertNull("Form named AuSummaryForm should not appear " +
	       "until PluginManager has started all AUs", auForm);
    assertNull("Table named AuSummaryTable should not appear " +
	       "until PluginManager has started all AUs", auTable);
  }


  public void testAfterAusLoaded() throws Exception {
    // Force PluginManager to think all AUs have started.
    theDaemon.setAusStarted(true);
    initServletRunner();
    WebRequest request =
      new GetMethodWebRequest("http://null/AuConfig" );
//     request.setParameter( "color", "red" );
    WebResponse resp1 = sClient.getResponse(request);
    log.debug2("Response 1: " + resp1.getText());
    assertResponseOk(resp1);
    assertEquals("Content type", "text/html", resp1.getContentType());

    WebForm auForm = resp1.getFormWithID("AuSummaryForm");
    WebTable auTable = resp1.getTableWithID("AuSummaryTable");
    assertNotNull("No form named AuSummaryForm", auForm);
    assertNotNull("No table named AuSummaryTable", auTable);
    assertEquals(1, auTable.getRowCount());
    assertEquals(2, auTable.getColumnCount());
    assertEquals("", auTable.getCellAsText(0,0));
    assertEquals("Add new Archival Unit", auTable.getCellAsText(0,1));
    TableCell cell = auTable.getTableCell(0,0);
    HTMLElement elem = cell.getElementWithID("lsb.1");
    Button btn = (Button)elem;
    assertEquals("Add", btn.getValue());
    // This form must be submitted via the javascript invoked by this button,
    btn.click();
    WebResponse resp2 = sClient.getCurrentPage();
    log.debug2("Response 2: " + resp2.getText());
    assertResponseOk(resp2);
  }
}
