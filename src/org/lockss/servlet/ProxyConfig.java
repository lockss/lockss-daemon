/*
 * $Id: ProxyConfig.java,v 1.3 2004-06-01 08:30:51 tlipkis Exp $
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

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import org.mortbay.html.*;
import org.mortbay.tools.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/** ProxyConfig servlet supplies configuration files or fragments for
 * configuring browsers or external proxies to use the lockss cache as a
 * proxy for (at least) those URLs contained on the cache.
 */
public class ProxyConfig extends LockssServlet {

  private String format;
  private PrintWriter wrtr = null;
  private String encapsulate;
  private ProxyInfo pi;
  private Map urlStems;

  private PluginManager pluginMgr;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
  }

  /**
   * Handle a request
   * @throws IOException
   */
  public void lockssHandleRequest() throws IOException {
    format = req.getParameter("format");
    if (StringUtil.isNullString(format)) {
      generateHelpPage();
      return;
    }

    resp.setContentType("text/plain");

    try {
      generateProxyFile(format);
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

  void generateProxyFile(String format) throws IOException {
    pi = new ProxyInfo(getMachineName());
    urlStems = pi.getUrlStemMap();
    if (format.equalsIgnoreCase("pac")) {
      if (urlStems.isEmpty()) {
	wrtr = resp.getWriter();
	wrtr.println("// No URLs cached on this LOCKSS cache");
      } else {
	generatePacFile();
      }
      return;
    }
    if (format.equalsIgnoreCase("pac_encap")) {
      generateEncapsulatedPacFile();
      return;
    }
    if (format.equalsIgnoreCase("ezproxy")) {
      if (urlStems.isEmpty()) {
	wrtr = resp.getWriter();
	wrtr.println("# No URLs cached on this LOCKSS cache");
      } else {
	generateEZProxyFile();
      }
      return;
    }
    generateHelpPage("Unknown proxy config format: " + format);
  }

  void generatePacFile() throws IOException {
    String pac = pi.generatePacFile(urlStems);
    wrtr = resp.getWriter();

    // Serve as PAC mime type if requested
    String mime = req.getParameter("mime");
    if ("pac".equalsIgnoreCase(mime)) {
      resp.setContentType("application/x-ns-proxy-autoconfig");
    }

    wrtr.print(pac);
  }

  void generateEncapsulatedPacFile() throws IOException {
    String url = req.getParameter("encapsulated_url");
    if (StringUtil.isNullString(url)) {
      generateHelpPage("Combined PAC file requires URL");
      return;
    }
    try {
      String pac = pi.encapsulatePacFileFromURL(urlStems, url);
      wrtr = resp.getWriter();

      // Serve as PAC mime type if requested
      String mime = req.getParameter("mime");
      if ("pac".equalsIgnoreCase(mime)) {
	resp.setContentType("application/x-ns-proxy-autoconfig");
      }
      wrtr.print(pac);
    } catch (IOException e) {
      generateHelpPage("Error reading PAC file from URL: " + url +
		       ": " + e.toString());
      return;
    } catch (Exception e) {
      generateHelpPage("Error generating combined PAC file from URL: " + url +
		       ": " + e.toString());
      return;
    }
  }

  void generateEZProxyFile() throws IOException {
    String ez = pi.generateEZProxyFragment(urlStems);
    wrtr = resp.getWriter();
    resp.setContentType("text/plain");
    wrtr.print(ez);
  }

  void generateHelpPage() throws IOException {
    generateHelpPage(null);
  }

  void generateHelpPage(String error) throws IOException {
    Page page = newPage();;
    resp.setContentType("text/html");
    //    table = new Table(0, "ALIGN=CENTER CELLSPACING=2 CELLPADDING=0");
    Form frm = new Form(srvURL(myServletDescr(), null));
    // use GET so user can refresh in browser
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
    addFmtElement(frm, "EZproxy config fragment", "ezproxy",
	       "Text to insert into EZproxy config file (#)");
    addFmtElement(frm, "PAC file", "pac",
	       "Automatic proxy configuration for browsers. " +
	       "Place the contents of this file on a server for your users " +
	       "to configure their browsers (#)" +
	       srvAbsLink(myServletDescr(), ".", "format=pac&mime=pac"));
    Composite urlform = new Composite();
    urlform.add("PAC file that combines rules in an existing PAC file with the rules for this cache.<br>PAC file URL: ");
    urlform.add(new Input(Input.Text, "encapsulated_url"));
    urlform.add(new Input(Input.Submit, "format", "pac_encap"));
    addFmtElement(frm, "Combined PAC file", "pac_encap", urlform);
    page.add(frm);
    page.add(getFooter());
    page.write(resp.getWriter());
  }

  void addFmtElement(Composite comp, String title, String format,
		     String text) {
    ServletDescr desc = myServletDescr();
    String fmt = "format=" + format;
    String absUrl = "<code>" + srvAbsURL(desc, fmt) + "</code>";
    Composite elem = new Composite();
    elem.add(StringUtil.replaceString(text, "#", absUrl.toString()));
    addFmtElement(comp, title, format, elem);
  }

  void addFmtElement(Composite comp, String title, String format,
		     Element elem) {
    ServletDescr desc = myServletDescr();
    String fmt = "format=" + format;
    comp.add("<li>");
    comp.add(srvLink(desc, title, fmt));
    comp.add(". ");
    comp.add(elem);
    comp.add("</li>");
  }

}
