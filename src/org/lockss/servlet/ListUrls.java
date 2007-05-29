/*
 * $Id: ListUrls.java,v 1.1 2007-05-29 06:23:44 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import javax.servlet.*;
import java.io.*;
import java.util.*;
import org.mortbay.html.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/** Output a plain list of the URLs in an AU
 */
public class ListUrls extends LockssServlet {
  static final Logger log = Logger.getLogger("ListUrls");

  private String auid;
  private ArchivalUnit au;

  private PrintWriter wrtr = null;

  private PluginManager pluginMgr;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    wrtr = null;
    au = null;
    auid = null;
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
  }

  /**
   * Handle a request
   * @throws IOException
   */
  public void lockssHandleRequest() throws IOException {
    if (!pluginMgr.areAusStarted()) {
      displayNotStarted();
      return;
    }
    auid = getParameter("auid");
    au = pluginMgr.getAuFromId(auid);
    if (au == null) {
      displayError("No such AU: " + auid);
      return;
    }
    PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");
    wrtr.println("# URLs in " + au.getName());
    wrtr.println();
    for (Iterator iter = au.getAuCachedUrlSet().contentHashIterator();
	 iter.hasNext(); ) {
      CachedUrlSetNode cusn = (CachedUrlSetNode)iter.next();
      if (cusn.hasContent()) {
	wrtr.println(cusn.getUrl());
      }
    }
  }

  void displayError(String error) throws IOException {
    Page page = newPage();
    Composite comp = new Composite();
    comp.add("<center><font color=red size=+1>");
    comp.add(error);
    comp.add("</font></center><br>");
    page.add(comp);
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }
}
