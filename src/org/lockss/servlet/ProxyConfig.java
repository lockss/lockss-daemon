/*
 * $Id: ProxyConfig.java,v 1.28 2008-11-08 08:17:03 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.html.*;

import org.lockss.daemon.ProxyInfo;
import org.lockss.jetty.MyTextArea;
import org.lockss.plugin.PluginManager;
import org.lockss.util.*;

/** ProxyConfig servlet supplies configuration files or fragments for
 * configuring browsers or external proxies to use the lockss cache as a
 * proxy for (at least) those URLs contained on the cache.
 */
public class ProxyConfig extends LockssServlet {

  // Used for serialization
  private static final long serialVersionUID = 1L;  // To stop an Eclipse complaint.  See "http://www.eaze.org/patrick/java/objectserialization.jsp"

  private static final String MIME_TYPE_PAC = "application/x-ns-proxy-autoconfig";
  private static final String TAG_MIME = "mime";
  private static final String TAG_SQUID = "squid";
  private static final String TAG_SQUID_CONFIG = "squidconfig";
  private static final String TAG_EZPROXY = "ezproxy";
  private static final String TAG_COMBINED_PAC = "Combined PAC";
  private static final String TAG_PAC = "pac";
  private static final String TAG_DIRECT_FIRST = "Direct First";
  private static final String TAG_FORMAT = "Action";
  private static final String TAG_DIRECT = "Direct";
  private static final String TAG_PROXY = "Proxy";

  private static Map<String,String> formDefaults =
    MapUtil.map(TAG_DIRECT_FIRST, TAG_PROXY);

  private String format;
  private PrintWriter wrtr = null;
  // private String encapsulate;
  private ProxyInfo pi;
  private Set urlStems;
  private boolean pacform;

  private PluginManager pluginMgr;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    wrtr = null;
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
    format = getParameter(TAG_FORMAT);
    if (StringUtil.isNullString(format)) {
      // remain compatible with previous PAC URL, which people may have
      // configured into browsers or stored elsewhere
      format = getParameter("format");
    }
    if (StringUtil.isNullString(format)) {
      try {
	getMultiPartRequest();
	format = getParameter(TAG_FORMAT);
      } catch (FormDataTooLongException e) {
	displayForm("Uploaded file too large: " + e.getMessage());
	return;
      }
    }
    pacform = !StringUtil.isNullString(getParameter("pacform"));
    if (StringUtil.isNullString(format)) {
      displayForm();
      return;
    } else if (format.equals("pacform")) {
      generateEncapForm(null);
      return;
    }

    // assume will send text.  Error & form display will override.
    resp.setContentType("text/plain");

    // Retrieve whether the user specified that s/he wants "direct" first or last.
    String strDirectFirst = getParameter(TAG_DIRECT_FIRST);
    boolean isDirectFirst = TAG_DIRECT.equalsIgnoreCase(strDirectFirst);

    // Generate the proxy file!
    try {
      generateProxyFile(format, isDirectFirst);
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

  void generateProxyFile(String format, boolean isDirectFirst) throws IOException {
    pi = new ProxyInfo(getMachineName());
    urlStems = pi.getUrlStems();

    if (!pluginMgr.areAusStarted()) {
      resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }
    else if (format.equalsIgnoreCase(TAG_PAC)) {
      generatePacFile(isDirectFirst);
    }
    else if (format.equalsIgnoreCase(TAG_COMBINED_PAC)) {
      generateEncapsulatedPacFile(isDirectFirst);
    }
    else if (format.equalsIgnoreCase(TAG_EZPROXY)) {
      generateEZProxyFile();
    }
    else if (format.equalsIgnoreCase(TAG_SQUID)) {
      generateExternalSquidFragment();
    }
    else if (format.equalsIgnoreCase(TAG_SQUID_CONFIG)) {
      generateSquidConfigFragment(isDirectFirst);
    }
    else {
      displayForm("Unknown proxy config format: " + format);
    }
  }

  void generateExternalSquidFragment()
      throws IOException {
    wrtr = resp.getWriter();
    wrtr.print(pi.generateExternalSquidFragment(urlStems, req.getRequestURI()));
  }

  void generateSquidConfigFragment(boolean isDirectFirst)
      throws IOException {
    wrtr = resp.getWriter();
    wrtr.print(pi.generateSquidConfigFragment(urlStems, req.getRequestURI(), isDirectFirst));
  }

  void generatePacFile(boolean isDirectFirst) throws IOException {
    wrtr = resp.getWriter();

    // Serve as PAC mime type if requested
    String mime = getParameter(TAG_MIME);
    if (TAG_PAC.equalsIgnoreCase(mime)) {
      resp.setContentType(MIME_TYPE_PAC);
    }

    wrtr.print(pi.generatePacFile(urlStems, req.getRequestURI(), isDirectFirst));
  }

  void generateEncapsulatedPacFile(boolean isDirectFirst) throws IOException {
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
	pac = pi.generateEncapsulatedPacFile(urlStems, encap, null, req.getRequestURI(), isDirectFirst);
      } else {
	try {
	  pac = pi.generateEncapsulatedPacFileFromURL(urlStems, url, isDirectFirst);
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
      String mime = getParameter(TAG_MIME);
      if (TAG_PAC.equalsIgnoreCase(mime)) {
	resp.setContentType(MIME_TYPE_PAC);
      }
      wrtr.print(pac);
    } catch (Exception e) {
      displayForm("Error generating combined PAC file from URL: " + url +
		  "<br>" + e.toString());
      return;
    }
  }

  void generateEZProxyFile()
      throws IOException {
    wrtr = resp.getWriter();
    wrtr.print(pi.generateEZProxyFragment(urlStems, req.getRequestURI()));
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
	     "should be proxied through this LOCKSS box.");

    if (error != null) {
      frm.add("<p><font color=red>");
      frm.add(error);
      frm.add("</font>");
    }

    frm.add("<p>Choose a supported format:<br /> \n");
    addChoice(frm, TAG_FORMAT, TAG_EZPROXY, "EZproxy config fragment",
	"Generate text to insert into an EZproxy config file");
    addChoice(frm, TAG_FORMAT, TAG_SQUID, "Generate a dstdomain file for Squid",
        "Generate a text file that can be used for a Squid \"dstdomain\" rule");
    addChoice(frm, TAG_FORMAT, TAG_SQUID_CONFIG, "Generate a configuration fragment for Squid",
        "Generate text to insert into a Squid configuration file");
    addChoice(frm, TAG_FORMAT, TAG_PAC, "PAC file",
	"Automatic proxy configuration for browsers. " +
	"Place the contents of this file on a server for your users " +
	"to configure their browsers" +
	      srvAbsLink(myServletDescr(), ".",
			 PropUtil.fromArgs(TAG_FORMAT, "pac",
					   "mime", "pac")));

    String url = getParameter("encapsulated_url");
    Input urlin = new Input(Input.Text, "encapsulated_url",
			    (url != null ? url : ""));
    urlin.setSize(40);

    Composite comp = new Composite();
    comp.add("PAC file that combines rules in an existing PAC file with the rules for this LOCKSS box.<br>PAC file URL: ");
    comp.add(urlin);
    addChoice(frm, TAG_FORMAT, TAG_COMBINED_PAC, "Combined PAC file", comp);

    frm.add("<p>Select preferred source (PAC files only):<br />\n");
    addChoice(frm, TAG_DIRECT_FIRST, TAG_PROXY, "Proxy first",
	      "Connect to the LOCKSS proxy first; if no response try the origin server.");
    addChoice(frm, TAG_DIRECT_FIRST, TAG_DIRECT, "Direct first",
	      "Connect to the origin server first; if no response try the LOCKSS proxy.");


    frm.add("<BR />");

    Input submit = new Input(Input.Submit, "submit", "Submit");
    frm.add(submit);

    page.add(frm);
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }


  void addChoice(Composite comp, String fieldName, String value,
		 String title, Object text) {
    // Preselect radio buttons that were selected in the subbmitted form,
    // or in the defaults if not in form
    boolean select = StringUtil.equalStrings(getParamOrDefault(fieldName),
					     value);
    comp.add(ServletUtil.radioButton(this, fieldName, value,
				     "<b>" + title + "</b>: " + text,
				     select));
    comp.add("<br />\n");
  }

  String getParamOrDefault(String key) {
    String val = getParameter(key);
    if (val == null) {
      val = formDefaults.get(key);
    }
    return val;
  }

  void generateEncapForm(String error) throws IOException {
    Page page = newPage();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
    frm.attribute("enctype", "multipart/form-data");
    frm.add(new Input(Input.Hidden, "pacform", "1"));
    frm.add(new Input(Input.Hidden, TAG_FORMAT, TAG_COMBINED_PAC));
    Table tbl = new Table(0, "align=center cellspacing=16 cellpadding=0");
    tbl.newRow();
    tbl.newCell("align=center");
    tbl.add("Generate a PAC file that combines the rules from an existing PAC file with the rules for this LOCKSS box.");
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
