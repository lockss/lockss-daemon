/*
 * $Id: LockssServletTestCase.java,v 1.6 2008-06-30 08:43:59 tlipkis Exp $
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

import java.util.*;
import java.io.*;
import junit.framework.TestCase;
import org.lockss.test.*;
import org.lockss.daemon.status.*;

import org.lockss.util.*;
import org.lockss.state.*;
// import org.lockss.plugin.*;
// import org.lockss.servlet.*;
// import org.lockss.repository.*;
import com.meterware.servletunit.*;
import com.meterware.httpunit.*;


/**
 * TestCase extension for testing servlets
 */
public class LockssServletTestCase extends LockssTestCase {

  static Logger log = Logger.getLogger("ServletTestCase");

  /** Holds the ServletRunner after a call to initServletRunner() */
  protected LockssServletRunner sRunner;
  /** Holds the ServletUnitClient after a call to initServletRunner() */
  protected ServletUnitClient sClient;

  protected MockLockssDaemon theDaemon;

  protected void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
//     theDaemon.setDaemonInited(true);
  }

  /** Establish a servlet runner and client. */
  protected void initServletRunner() {
    sRunner = new LockssServletRunner();
    sRunner.setServletContextAttribute(ServletManager.CONTEXT_ATTR_LOCKSS_APP,
				       theDaemon);
    sRunner.setServletContextAttribute(ServletManager.CONTEXT_ATTR_SERVLET_MGR,
				       theDaemon.getServletManager());
    // Tell LockssServlet to include javascript directly in the page rather
    // than linking to it, because this little servlet runner can't acess
    // other server files
    sRunner.setServletContextAttribute(LockssServlet.ATTR_INCLUDE_SCRIPT,
				       "true");
    sClient = sRunner.newClient();
  }

  protected void logHeaders(WebResponse resp) {
    String names[] = resp.getHeaderFieldNames();
    log.debug("Response code: " + resp.getResponseCode() +
	      ": " + resp.getResponseMessage());
    for (int ix = 0; ix < names.length; ix ++) {
      log.debug(names[ix] + ": " + resp.getHeaderField(names[ix]));
    }
  }

  protected void assertResponseOk(WebResponse resp) {
    assertNotNull("No response received", resp);
    if (log.isDebug()) logHeaders(resp);
//     assertEquals("Content type", "text/html", resp.getContentType());
    assertEquals("Response code", 200, resp.getResponseCode());
  }

  /** Break a response into a list of lines.  The number of lines returned
   * is equal to the number of newline characters, unless the last line
   * doesn't end with a newline, in which case it is one greater. */
  /** break into a list of lines */
  protected List getLines(WebResponse resp) throws Exception {
    return getLines(resp.getText());
  }

  /** Break a multiline string into a list of lines.  The number of lines
   * returned is equal to the number of newline characters, unless the last
   * line doesn't end with a newline, in which case it is one greater. */
  protected List getLines(String text) {
    List lines = StringUtil.breakAt(text, '\n');
    if ("".equals(lines.get(lines.size()-1))) {
      lines.remove(lines.size()-1);
    }
    return lines;
  }
}
