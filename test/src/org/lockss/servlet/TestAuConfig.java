/*
 * $Id: TestAuConfig.java,v 1.1 2003-08-06 06:29:45 tlipkis Exp $
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
public class TestAuConfig extends LockssTestCase {
  static Logger log = Logger.getLogger("TestAuConfig");

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau = null;
  private LockssServletRunner sr;
  private ServletUnitClient sc;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = new MockLockssDaemon();
    PluginManager mgr = new PluginManager();
    theDaemon.setPluginManager(mgr);
    theDaemon.setDaemonInited(true);
    mgr.initService(theDaemon);
    mgr.startService();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION,
                      tempDirPath);
//     props.setProperty(TreeWalkHandler.PARAM_TREEWALK_INTERVAL, "100");
//     props.setProperty(NodeManagerImpl.PARAM_RECALL_DELAY, "5s");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    mau = new MockArchivalUnit();

    sr = new LockssServletRunner();
    sr.registerServlet("/AuConfig", AuConfig.class.getName() );
    sr.setServletContextAttribute("LockssDaemon", theDaemon);
    sc = sr.newClient();
  }

  public void test1() throws Exception {
    WebRequest request =
      new GetMethodWebRequest("http://null/AuConfig" );
//     request.setParameter( "color", "red" );
    WebResponse response = sc.getResponse(request);
    assertNotNull("No response received", response);
    assertEquals("content type", "text/html", response.getContentType());
    WebForm auForm = response.getFormWithID("AuSummaryForm");
    WebTable auTable = response.getTableWithID("AuSummaryTable");
    assertNotNull("No form named AuSummaryForm", auForm);
    assertNotNull("No table named AuSummaryTable", auTable);
    assertEquals(1, auTable.getRowCount());
    assertEquals(2, auTable.getColumnCount());
    assertEquals("", auTable.getCellAsText(0,0));
    assertEquals("Add new Volume", auTable.getCellAsText(0,1));
    TableCell cell = auTable.getTableCell(0,0);
    HTMLElement elem = cell.getElementWithID("lsb.1");
    Button btn = (Button)elem;
    assertEquals("Add", btn.getValue());
    btn.click();
    WebRequest req2 = auForm.getRequest();
    WebResponse resp2 = sc.getResponse(req2);
//     log.info(resp2.getText());
  }
}
