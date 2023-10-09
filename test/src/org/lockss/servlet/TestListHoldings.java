/*
 * $Id $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.lockss.config.TdbTestUtil;
import org.lockss.daemon.status.StatusService;
import org.lockss.daemon.status.StatusServiceImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.util.Logger;


public class TestListHoldings extends LockssServletTestCase {

  static Logger log = Logger.getLogger("TestListHoldings");

  private StatusService statSvc;
  private StatusServiceImpl ssi;

  /**
   * We have to set up the servlet testing environment, and also some TDB
   * structure in the config.
   * @throws Exception
   */
  protected void setUp() throws Exception {
    super.setUp();
    ConfigurationUtil.addFromArgs(ListHoldings.PARAM_ENABLE_HOLDINGS, "true");
    //lh = new ListHoldings();
    statSvc = theDaemon.getStatusService();
    ssi = (StatusServiceImpl)statSvc;
    ssi.startService();
  }

  // Utilities for running the servlet
  protected void initServletRunner() {
    super.initServletRunner();
    sRunner.registerServlet("/TitleList", ListHoldings.class.getName() );
    // ListHoldings wants there to be a local ip address
    ConfigurationUtil.addFromArgs(LockssServlet.PARAM_LOCAL_IP, "2.4.6.8");
  }

  /**
   * It should be possible to request a report directly via URL; for this to
   * work, it should be possible to generate a report with the minimal argument
   * of format=csv
   */
  public final void testDirectUrlAccess() throws Exception {
    initServletRunner();
    TdbTestUtil.setUpConfig();
    WebRequest request = new GetMethodWebRequest("http://null/TitleList");
    request.setParameter( "format", "html" );
    //scope, format, report and coverageNotesFormat
    WebResponse response = sClient.getResponse(request);
    response.isHTML();
  }

}
