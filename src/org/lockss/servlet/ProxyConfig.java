/*
 * $Id: ProxyConfig.java,v 1.20 2006-03-16 01:41:19 thib_gc Exp $
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

import java.io.*;
import java.net.UnknownHostException;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import org.lockss.daemon.ProxyInfo;
import org.lockss.jetty.MyTextArea;
import org.lockss.plugin.PluginManager;
import org.lockss.util.StringUtil;
import org.mortbay.html.*;
import org.mortbay.http.HttpFields;

/** ProxyConfig servlet supplies configuration files or fragments for
 * configuring browsers or external proxies to use the lockss cache as a
 * proxy for (at least) those URLs contained on the cache.
 */
public class ProxyConfig extends LockssServlet {

  private static final String TAG_SQUID = "squid";
  private static final String TAG_EZPROXY = "ezproxy";
  private static final String TAG_COMBINED_PAC = "Combined PAC";
  private static final String TAG_PAC = "pac";
  private String action;
  private String auth;
  private PrintWriter wrtr = null;
  private String encapsulate;
  private ProxyInfo pi;
  private Map urlStems;
  private boolean pacform;

  private PluginManager pluginMgr;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    wrtr = null;
    auth = null;
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
    auth = req.getHeader(HttpFields.__Authorization);
    action = getParameter("action");
    if (StringUtil.isNullString(action)) {
      // remain compatible with previous PAC URL, which people may have
      // configured into browsers or stored elsewhere
      action = getParameter("format");
    }
    if (StringUtil.isNullString(action)) {
      try {
	getMultiPartRequest();
	action = getParameter("action");
      } catch (FormDataTooLongException e) {
	displayForm("Uploaded file too large: " + e.getMessage());
	return;
      }
    }
    pacform = !StringUtil.isNullString(getParameter("pacform"));
    if (StringUtil.isNullString(action)) {
      displayForm();
      return;
    } else if (action.equals("pacform")) {
      generateEncapForm(null);
      return;
    }

    // assume will send text.  Error & form display will override.
    resp.setContentType("text/plain");

    try {
      generateProxyFile(action);
    } catch (IOException e) {
      if (wrtr != null) {
	// Error occurred after we created writer, default error page won't
	// work
	wrtr.println("Error generating proxy config:");
	wrtr.println(e.toString());
	wrtr.println(StringUtil.
		     trimStackTrace(e.toString(),
				    StringUtil.stackTraceString(e)));
      } else {
	// use default error page
	throw e;
      }
    }
  }

  void displayForm() throws IOException {
    displayForm(null);
  }

  void displayForm(String error) throws IOException {
    if (pacform) {
      generateEncapForm(error);
    } else {
      generateHelpPage(error);
    }
  }

  void generateProxyFile(String format) throws IOException {
    pi = new ProxyInfo(getMachineName());
    urlStems = pi.getUrlStemMap();

    if (!pluginMgr.areAusStarted()) {
      resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }

    if (format.equalsIgnoreCase(TAG_PAC)) {
      if (urlStems.isEmpty()) {
	wrtr = resp.getWriter();
	wrtr.println("// No URLs cached on this LOCKSS cache");
      } else {
	generatePacFile();
      }
      return;
    }

    if (format.equalsIgnoreCase(TAG_COMBINED_PAC)) {
      generateEncapsulatedPacFile();
      return;
    }

    if (format.equalsIgnoreCase(TAG_EZPROXY)) {
      if (urlStems.isEmpty()) {
	wrtr = resp.getWriter();
	wrtr.println("# No URLs cached on this LOCKSS cache");
      } else {
	generateEZProxyFile();
      }
      return;
    }

    if (format.equalsIgnoreCase(TAG_SQUID)) {
      generateSquidFile();
      return;
    }

    displayForm("Unknown proxy config format: " + format);
  }

  void generatePacFile() throws IOException {
    String pac = pi.generatePacFile(urlStems);
    wrtr = resp.getWriter();

    // Serve as PAC mime type if requested
    String mime = getParameter("mime");
    if (TAG_PAC.equalsIgnoreCase(mime)) {
      resp.setContentType("application/x-ns-proxy-autoconfig");
    }

    wrtr.print(pac);
  }

  void generateEncapsulatedPacFile() throws IOException {
    String url = getParameter("encapsulated_url");
    String pac;
    try {
      if (StringUtil.isNullString(url)) {
	if (!pacform) {
	  displayForm("Combined PAC file requires URL");
	  return;
	}
	String encap = getParameter("pac_contents1");
	if (StringUtil.isNullString(encap)) {
	  encap = getParameter("pac_contents2");
	}
	if (StringUtil.isNullString(encap)) {
	  displayForm("Please provide an existing PAC file in one of the three fields below");
	  return;
	}
	pac = pi.encapsulatePacFile(urlStems, encap, null);
      } else {
	try {
// 	  pac = pi.encapsulatePacFileFromURL(urlStems, url, auth);
	  pac = pi.encapsulatePacFileFromURL(urlStems, url);
	} catch (UnknownHostException e) {
	  displayForm("Error reading PAC file from URL: " + url +
		      "<br>No such host: " + e.getMessage());
	  return;
	} catch (IOException e) {
	  log.warning("Error reading PAC file from URL: " + url, e);
	  displayForm("Error reading PAC file from URL: " + url +
		      "<br>" + e.toString());
	  return;
	}
      }
      wrtr = resp.getWriter();

      // Serve as PAC mime type if requested
      String mime = getParameter("mime");
      if (TAG_PAC.equalsIgnoreCase(mime)) {
	resp.setContentType("application/x-ns-proxy-autoconfig");
      }
      wrtr.print(pac);
    } catch (Exception e) {
      displayForm("Error generating combined PAC file from URL: " + url +
		  "<br>" + e.toString());
      return;
    }
  }

  void generateEZProxyFile() throws IOException {
    String ez = pi.generateEZProxyFragment(urlStems);
    wrtr = resp.getWriter();
    wrtr.print(ez);
  }

  void generateSquidFile() throws IOException {
    String sq = pi.generateSquidFile(urlStems);
    wrtr = resp.getWriter();
    wrtr.print(sq);
  }

  void generateHelpPage(String error) throws IOException {
    Page page = newPage();

    //    table = new Table(0, "ALIGN=CENTER CELLSPACING=2 CELLPADDING=0");
    Form frm = new Form(srvURL(myServletDescr()));
    // use GET so user can copy parameterized URL
    frm.method("GET");
    frm.add("<p>This page is used to obtain proxy configuration " +
	     "information for browsers and other proxies, " +
	     "to inform them which URLs " +
	     "should be proxied through this cache.");
    if (error != null) {
      frm.add("<p><font color=red>");
      frm.add(error);
      frm.add("</font>");
    }
    frm.add("<p>Choose a supported format: ");
    frm.add("<ul>");
    addFmtElement(frm, "EZproxy config fragment", TAG_EZPROXY,
	       "Generate text to insert into an EZproxy config file (#)");
    addFmtElement(frm, "Generate a dstdomain file for Squid", TAG_SQUID,
               "Generate text that can be used to create a file for a Squid \"dstdomain\" rule (#)");
    addFmtElement(frm, "PAC file", TAG_PAC,
	       "Automatic proxy configuration for browsers. " +
	       "Place the contents of this file on a server for your users " +
	       "to configure their browsers (#)" +
	       srvAbsLink(myServletDescr(), ".", "action=pac&mime=pac"));
    Composite urlform = new Composite();
    urlform.add("PAC file that combines rules in an existing PAC file with the rules for this cache.<br>PAC file URL: ");
    String url = getParameter("encapsulated_url");
    Input urlin = new Input(Input.Text, "encapsulated_url",
			    (url != null ? url : ""));
    urlin.setSize(40);
    urlform.add(urlin);
    urlform.add(new Input(Input.Submit, "action", TAG_COMBINED_PAC));

    addFmtElement(frm, "Combined PAC file", "pacform", urlform);
    page.add(frm);
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  void addFmtElement(Composite comp, String title, String format,
		     String text) {
    ServletDescr desc = myServletDescr();
    String fmt = "action=" + format;
    String absUrl = "<code>" + srvAbsURL(desc, fmt) + "</code>";
    Composite elem = new Composite();
    elem.add(StringUtil.replaceString(text, "#", absUrl));
    addFmtElement(comp, title, format, elem);
  }

  void addFmtElement(Composite comp, String title, String format,
		     Element elem) {
    ServletDescr desc = myServletDescr();
    String fmt = "action=" + format;
    comp.add("<li>");
    comp.add(srvLink(desc, title, fmt));
    comp.add(". ");
    comp.add(elem);
    comp.add("</li>");
  }

  void generateEncapForm(String error) throws IOException {
    Page page = newPage();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
    frm.attribute("enctype", "multipart/form-data");
    frm.add(new Input(Input.Hidden, "pacform", "1"));
    frm.add(new Input(Input.Hidden, "action", TAG_COMBINED_PAC));
//     frm.add(new Input(Input.Hidden, ACTION_TAG));
    Table tbl = new Table(0, "align=center cellspacing=16 cellpadding=0");
    tbl.newRow();
    tbl.newCell("align=center");
    tbl.add("Generate a PAC file that combines the rules from an existing PAC file with the rules for this cache.");
    if (error != null) {
      tbl.newRow();
      tbl.newCell("align=center");
      tbl.add("<font color=red>");
      tbl.add(error);
      tbl.add("</font>");
    }
    tbl.newRow();
    tbl.newCell("align=center");

    String url = getParameter("encapsulated_url");
    Input urlin = new Input(Input.Text, "encapsulated_url",
			    (url != null ? url : ""));
    urlin.setSize(40);
    tbl.add("Enter the URL of a remote PAC file:<br>");
    tbl.add(urlin);

    tbl.newRow();
    tbl.newCell("align=center");
    tbl.add("or the name of a local PAC file:<br>");
    tbl.add(new Input(Input.File, "pac_contents1"));

    tbl.newRow();
    tbl.newCell("align=center");
    tbl.add(new Input(Input.Submit, "dummy", "Generate Combined PAC"));

    tbl.newRow();
    tbl.newCell("align=center");
    tbl.add("or enter PAC file contents here:<br>");
    TextArea txt = new MyTextArea("pac_contents2");
    txt.setSize(80, 20);
    tbl.add(txt);

    tbl.newRow();
    tbl.newCell("align=center");
    tbl.add(new Input(Input.Submit, "dummy", "Generate Combined PAC"));

    frm.add(tbl);
    page.add(frm);
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
    }

}
