/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import java.util.regex.Pattern;

import org.mortbay.http.*;
import org.mortbay.html.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.state.*;

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
  private AdminServletManager srvltMgr;

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
      srvltMgr = (AdminServletManager)getServletManager();
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
      displayNotFound("No such AU: " + auid);
      return;
    }
    cu = au.makeCachedUrl(url);
    if (cu == null) {
      displayNotFound("URL " + url + " not found in AU: " + au.getName());
      return;
    }
    boolean hasIncludedContent = cu.hasContent();
    cu.setOption(CachedUrl.OPTION_INCLUDED_ONLY, "false");
    String versionStr = getParameter("version");
    if (versionStr != null) {
      try {
	int version = Integer.parseInt(versionStr);
	int curVer = cu.getVersion();
	if (version != curVer) {
	  CachedUrl verCu = cu.getCuVersion(version);
	  verCu.setOption(CachedUrl.OPTION_INCLUDED_ONLY, "false");
	  if (verCu != null && verCu.hasContent()) {
	    cu = verCu;
	  } else {
	    errMsg = "Couldn't find version " + versionStr
	      + ", displaying current version";
	  }
	}
      } catch (NumberFormatException e) {
	log.error("Couldn't parse version string: " + versionStr);
	errMsg = "Illegal version: " + versionStr
	  + ", displaying current version";
      } catch (RuntimeException e) {
	log.error("Couldn't get file version", e);
	errMsg = "Couldn't find version " + versionStr
	  + ", displaying current version";
      }
    }

    if (!cu.hasContent()) {
      if (versionStr != null) {
	displayNotFound("Version " + versionStr + " of URL " + url
		     + " has no content in AU: " + au.getName());
      } else {
	displayNotFound("URL " + url + " not found in AU: " + au.getName());
      }
      return;
    }
    clen = cu.getContentSize();
    try {
      props = cu.getProperties();
      ctype = cu.getContentType();
      String frame = getParameter("frame");
      if (StringUtil.isNullString(frame)) {
	if (isFrameType(ctype)) {
	  displayFrameSet();
	} else {
	  displaySummary(false, hasIncludedContent);
	}
      } else if ("content".equalsIgnoreCase(frame)) {
	displayContent();
      } else if ("summary".equalsIgnoreCase(frame)) {
	setFramed(true);
	displaySummary(true, hasIncludedContent);
      } else {
	displayError(HttpResponse.__400_Bad_Request,
		     "Illegal frame parameter: " + frame);
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
      if (StringUtil.startsWithIgnoreCase(mimeType, (String)iter.next())) {
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

  void displaySummary(boolean contentInOtherFrame,
		      boolean hasIncludedContent) throws IOException {
    Page page = newPage();
    layoutErrorBlock(page);

    Table tbl = new Table(0, "ALIGN=CENTER CELLSPACING=2 CELLPADDING=0");
    tbl.newRow();
    tbl.newCell("align=left");
    tbl.add("AU:");
    tbl.newCell("align=left");
    tbl.add(getAuLink(au));
    tbl.newRow();
    tbl.newCell("align=left");
    tbl.add("URL:&nbsp;");
    tbl.newCell("align=left");
    tbl.add(url);
    if (!hasIncludedContent) {
      tbl.newRow();
      tbl.newCell("colspan=2 align=left");
      tbl.add("<b>Excluded by crawl rules - hidden from normal processing</b>");
    }
    page.add("<font size=+1>");
    page.add(tbl);
    page.add("</font>");
    tbl = new Table(0, "ALIGN=CENTER CELLSPACING=2 CELLPADDING=0");
//     tbl.newRow();
//     tbl.newCell("colspan=2 align=center");
    String contentTypeHeader =
      props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
    String contentType = cu.getContentType();
    if (StringUtil.equalStrings(contentType, contentTypeHeader)) {
      addPropRow(tbl, "Content Type", contentType);
    } else {
      addPropRow(tbl, "Content Type" + addFootnote("Inferred by plugin"),
		 contentType);
    }
    addPropRow(tbl, "Length", clen);
    try {
      String versionStr = Integer.toString(cu.getVersion());
      CachedUrl[] cuVersions = cu.getCuVersions(2);
      if (cuVersions.length > 1) {
	// If multiple versions, include link to version table
	Properties args =
	  PropUtil.fromArgs("table",
			    ArchivalUnitStatus.FILE_VERSIONS_TABLE_NAME,
			    "key", au.getAuId());
	args.setProperty("url", url);
	StringBuilder sb = new StringBuilder(versionStr);
	sb.append("&nbsp;&nbsp;");
	sb.append(srvLink(AdminServletManager.SERVLET_DAEMON_STATUS,
			  "Other versions",
			  args));
	versionStr = sb.toString();
      }
      addPropRow(tbl, "Version #", versionStr);
    } catch (RuntimeException e) {
      log.warning("Can't get cu version: " + cu.getUrl(), e);
    }
    try {
      long sdate =
	Long.parseLong(props.getProperty(CachedUrl.PROPERTY_FETCH_TIME));
      addPropRow(tbl, "Collected at", ServletUtil.headerDf.format(new Date(sdate)));
    } catch (NumberFormatException ignore) {
    }
    String repairFrom = props.getProperty(CachedUrl.PROPERTY_REPAIR_FROM);
    if (!StringUtil.isNullString(repairFrom)) {
      addPropRow(tbl, "Repaired from", repairFrom);
      try {
	long rdate =
	  Long.parseLong(props.getProperty(CachedUrl.PROPERTY_REPAIR_DATE));
	addPropRow(tbl, "Repair date",
		   ServletUtil.headerDf.format(new Date(rdate)));
      } catch (NumberFormatException ignore) {
      }
    }
    if (!StringUtil.isNullString(getParameter("showall"))) {
      tbl.newRow();
      tbl.newRow();
      tbl.newCell("align=left");
      tbl.add("<b>Raw Headers");
      Set<String> keys = new TreeSet(props.keySet());
      for (String key : keys) {
	addPropRow(tbl, key, props.getProperty(key));
      }
    } else {
      Properties args = getParamsAsProps();
      args.remove("frame");
      args.setProperty("showall", "1");
      if (contentInOtherFrame) {
	args.setProperty("frame", "summary");
      }

      tbl.newRow();
      tbl.newCell("align=left");
      Link lnk = new Link(srvURL(myServletDescr(), args), "Show all");
      if (contentInOtherFrame) {
	lnk.attribute("target", "CuMeta");
      }
      tbl.add(lnk);
    }

    CachedUrl cu = au.makeCachedUrl(url);
    try {
      if (cu.hasContent()) {
	if (au.getLinkExtractor(cu.getContentType()) != null) {
	  tbl.newRow();
	  tbl.newCell("align=left");
	  Link extrlnk =
	    new Link(srvURL(AdminServletManager.SERVLET_LIST_OBJECTS,
			    PropUtil.fromArgs("type", "extracturls",
					      "auid", au.getAuId(),
					      "url", url)),
		     "Extract URLs");
	  tbl.add(extrlnk);
	}
      }
    } finally {
      AuUtil.safeRelease(cu);
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
    endPageNoFooter(page);
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
    boolean isFilter = getParameter("filter") != null;
    resp.setContentType(ctype);
    // Set as inline content with name, if PDF or unframed content
    if (!isFrameType(ctype)) {
      resp.setHeader("Content-disposition", "inline; filename=" +
		     ServletUtil.getContentOriginalFilename(cu, true));
    }
    // if filtering, don't know content length
    if (!isFilter) {
      if (clen <= Integer.MAX_VALUE) {
	resp.setContentLength((int)clen);
      } else {
	resp.setHeader(HttpFields.__ContentLength, Long.toString(clen));
      }
    }
    OutputStream out = null;
    InputStream in = null;
    try {
      out = resp.getOutputStream();
      if (isFilter) {
	in = cu.openForHashing();
      } else {
	in = cu.getUncompressedInputStream();
      }
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

  void displayNotFound(String error) throws IOException {
    displayError(HttpResponse.__404_Not_Found, error);
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

  Link getAuLink(ArchivalUnit au) {
    return new Link(srvURL(AdminServletManager.SERVLET_DAEMON_STATUS,
			   PropUtil.fromArgs("table", ArchivalUnitStatus.AU_STATUS_TABLE_NAME,
					     "key", au.getAuId())),
		    au.getName());
  }
}
