/*
 * $Id: ViewContent.java,v 1.12 2006-03-16 01:41:19 thib_gc Exp $
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

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import org.mortbay.http.*;
import org.mortbay.html.*;
import org.mortbay.servlet.MultiPartRequest;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.jetty.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.repository.*;

/** ViewContent servlet displays cached content
 */
public class ViewContent extends LockssServlet {
  static final Logger log = Logger.getLogger("ViewContent");

  private String action;
  private String verbose;
  private String auid;
  private String url;
  private ArchivalUnit au;
  private CachedUrl cu;
  private long clen;
  private String ctype;

  private CIProperties props;
  private PrintWriter wrtr = null;
  private String encapsulate;

  private PluginManager pluginMgr;
  private LocalServletManager srvltMgr;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    wrtr = null;
    au = null;
    cu = null;
    url = null;
    auid = null;
    props = null;
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
    try {
      srvltMgr =
	(LocalServletManager) getLockssDaemon().getServletManager();
    } catch (RuntimeException e) {
      log.warning("Can't find LocalServletManager", e);
    }
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
    verbose = getParameter("verbose");
    auid = getParameter("auid");
    url = getParameter("url");
    if (StringUtil.isNullString(url)) {
      displayForm();
      return;
    }
    au = pluginMgr.getAuFromId(auid);
    if (au == null) {
      displayError("No such AU: " + auid);
      return;
    }
    cu = au.makeCachedUrl(url);
    if (cu == null || !cu.hasContent()) {
      displayError("URL " + url + " not found in AU: " + au.getName());
      return;
    }
    clen = cu.getContentSize();
    try {
      props = cu.getProperties();
      ctype = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
      String frame = getParameter("frame");
      if (StringUtil.isNullString(frame)) {
	if (isFrameType(ctype)) {
	  displayFrameSet();
	} else {
	  displaySummary(false);
	}
      } else if ("content".equalsIgnoreCase(frame)) {
	displayContent();
      } else if ("summary".equalsIgnoreCase(frame)) {
	setFramed(true);
	displaySummary(true);
      } else {
	displayError("Illegal frame parameter: " + frame);
      }
    } finally {
      cu.release();
    }
  }

  boolean isFrameType(String ctype) {
    if (StringUtil.isNullString(ctype)) return false;
    String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
    if (StringUtil.isNullString(mimeType)) return false;
    for (Iterator iter = srvltMgr.inFrameContentTypes().iterator();
	 iter.hasNext(); ) {
      if (StringUtil.startsWithIgnoreCase(ctype, (String)iter.next())) {
	return true;
      }
    }
    return false;
  }

  void displayFrameSet() throws IOException {
    FrameSet set = new FrameSet(getPageTitle(),"*","*,*");
    addBarePageHeading(set);
    Properties args = getParamsAsProps();
    args.setProperty("frame", "summary");
    set.frame(0,0).name("CuMeta", srvURL(myServletDescr(), args));
    args.setProperty("frame", "content");
    set.frame(0,1).name("CuContent", srvURL(myServletDescr(), args));
    set.write(resp.getWriter());
  }

  void displaySummary(boolean contentInOtherFrame) throws IOException {
    LockssRepository repo = getLockssDaemon().getLockssRepository(au);
    Page page = newPage();

    Table tbl = new Table(0, "ALIGN=CENTER CELLSPACING=2 CELLPADDING=0");
    tbl.newRow();
    tbl.newCell("align=left");
    tbl.add("AU:");
    tbl.newCell("align=left");
    tbl.add(au.getName());
    tbl.newRow();
    tbl.newCell("align=left");
    tbl.add("URL:&nbsp;");
    tbl.newCell("align=left");
    tbl.add(url);
    page.add("<font size=+1>");
    page.add(tbl);
    page.add("</font>");
    tbl = new Table(0, "ALIGN=CENTER CELLSPACING=2 CELLPADDING=0");
//     tbl.newRow();
//     tbl.newCell("colspan=2 align=center");
    addPropRow(tbl, "Content Type",
	       props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE));
    addPropRow(tbl, "Length", clen);
    try {
      RepositoryNode node = repo.getNode(cu.getUrl());
      addPropRow(tbl, "Version #", node.getCurrentVersion());
    } catch (MalformedURLException e) {
      log.warning("Can't get repo node: " + cu.getUrl(), e);
    }
    try {
      long sdate =
	Long.parseLong(props.getProperty(CachedUrl.PROPERTY_FETCH_TIME));
      addPropRow(tbl, "Collected at", ServletUtil.headerDf.format(new Date(sdate)));
    } catch (NumberFormatException ignore) {
    }
    page.add(tbl);
    page.add("<br>");
    Composite comp = new Block(Block.Center);
    if (contentInOtherFrame) {
      comp.add("Page is displayed below.  Most intra-site links will not work.");
    } else {
      Properties args = getParamsAsProps();
      args.setProperty("frame", "content");
      comp.add(srvLink(myServletDescr(),
		       "Click here to download/play content",
		       args));
    }
    page.add(comp);
//     page.add(getFooter());
    ServletUtil.writePage(resp, page);
  }

  void addPropRow(Table tbl, String prop, long val) {
    addPropRow(tbl, prop, Long.toString(val));
  }

  void addPropRow(Table tbl, String prop, String val) {
    tbl.newRow();
    tbl.newCell("align=left");
    tbl.add(prop);
    tbl.add(":&nbsp;");
    tbl.newCell("align=left");
    tbl.add(val);
  }

  void displayContent() {
    if (log.isDebug3()) {
      log.debug3("props: " + props);
      log.debug3("ctype: " + ctype);
      log.debug3("clen: " + clen);
    }
    resp.setContentType(ctype);
    if (clen <= Integer.MAX_VALUE) {
      resp.setContentLength((int)clen);
    }
    OutputStream out = null;
    InputStream in = null;
    try {
      out = resp.getOutputStream();
      in = cu.getUnfilteredInputStream();
      StreamUtil.copy(in, out);
    } catch (IOException e) {
      log.warning("Copying CU to HTTP stream", e);
    } finally {
      if (in != null) try {in.close();} catch (IOException ignore) {}
      if (out != null) try {out.close();} catch (IOException ignore) {}
    }
    cu.release();
  }

  void displayForm() throws IOException {
    displayForm(null);
  }

  void displayForm(String error) throws IOException {
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
