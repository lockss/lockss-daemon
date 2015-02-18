/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import org.mortbay.html.*;
import org.mortbay.http.*;
import org.lockss.app.*;
import org.lockss.util.*;

/** Raise an alert on demand.  For testing alerts
 */
public class DisabledServlet extends LockssServlet {
  static Logger log = Logger.getLogger("DisabledServlet");

  private LockssDaemon daemon;

//   public void init(ServletConfig config) throws ServletException {
//     super.init(config);
//     daemon = getLockssDaemon();
//     alertMgr = daemon.getAlertManager();
//     pluginMgr = daemon.getPluginManager();
//   }

  protected ServletDescr myServletDescr() {
    ServletDescr res = super.myServletDescr();
    if (res != null) {
      return res;
    }
    String servletPath=req.getServletPath();
    String pathInfo=req.getPathInfo();
    return ((BaseServletManager)getServletManager()).findServletDescrFromPath(servletPath);
  }

  public void lockssHandleRequest() throws IOException {
    ServletDescr descr = myServletDescr();
    if (descr.isEnabled(getLockssDaemon())) {
      String msg = descr.getNavHeading(this)
	+ " has been enabled; daemon restart is required to activate";
      resp.sendError(HttpResponse.__404_Not_Found, msg);
    } else {
      resp.sendError(HttpResponse.__404_Not_Found);
    }
  }

  void displayError(int result, String error) throws IOException {
    Page page = newPage();
    Composite comp = new Composite();
    comp.add("<center><font color=red size=+1>");
    comp.add(error);
    comp.add("</font></center><br>");
    page.add(comp);
    endPage(page);
  }

}
