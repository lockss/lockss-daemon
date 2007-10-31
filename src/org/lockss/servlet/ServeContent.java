/*
 * $Id: ServeContent.java,v 1.1 2007-10-31 04:02:57 dshr Exp $
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
import org.lockss.filter.*;
import org.lockss.state.*;

/** ServeContent servlet displays cached content using Javascript
 *  link re-writing courtesy of WERA.
 */
public class ServeContent extends LockssServlet {
  static final Logger log = Logger.getLogger("ServeContent");

  private String action;
  private String verbose;
  private String url;
  private String ctype;
  private CachedUrl cu;
  private long clen;

  private PluginManager pluginMgr;
  private LocalServletManager srvltMgr;

  // Insert Javascript at this tag
  // XXX tag should be case-insensitive
  private static final String jsTag = "</html>";
  // Javascript to insert
  // XXX - must only rewrite URLs starting with host part of AU's base url
  // XXX - what was "mode" used for before excision?
  private String jsText = 
    "<SCRIPT language=\"Javascript\">\n" +
    "<!-- \n" +
    "   // This script was modified from the Internet Archive\'s\n" +
    "   // script with help from WERA\n" +
    "   \n" +
    "   function xResolveUrl(url) {\n" +
    "      var image = new Image();\n" +
    "      image.src = url;\n" +
    "      return image.src;\n" +
    "   }\n" +
    "   function xLateUrl(aCollection, sProp, mode) {\n" +
    "      var i = 0;\n" +
    "      for(i = 0; i < aCollection.length; i++) {\n" +
    "        if (typeof(aCollection[i][sProp]) == \"string\") { \n" +
    "          if (aCollection[i][sProp].indexOf(\"mailto:\") == -1 && aCollection[i][sProp].indexOf(\"javascript:\") == -1) {\n" +
    "               aCollection[i][\"target\"] = \"_top\";\n" +
    "               if(aCollection[i][sProp].indexOf(urlTarget) == 0) {\n" +
    "                 aCollection[i][sProp] = urlPrefix + encodeURIComponent(aCollection[i][sProp]);\n" +
    "               } else if (aCollection[i][sProp].indexOf(urlLocalPrefix) == 0) {\n" +
    "                 aCollection[i][sProp] = urlPrefix + urlTarget + encodeURIComponent(aCollection[i][sProp].substring(urlLocalPrefix.length+1));\n" +
    "               }\n" +
    "          }\n" +
    "        }\n" +
    "      }\n" +
    "   }\n" +
    "\n" +
    "   xLateUrl(document.getElementsByTagName(\"IMG\"),\"src\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"A\"),\"href\",\"standalone\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"AREA\"),\"href\",\"standalone\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"OBJECT\"),\"codebase\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"OBJECT\"),\"data\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"APPLET\"),\"codebase\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"APPLET\"),\"archive\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"EMBED\"),\"src\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"BODY\"),\"background\",\"inline\");\n" +
    "   var forms = document.getElementsByTagName(\"FORM\",\"inline\");\n" +
    "   if (forms) {\n" +
    "       var j = 0;\n" +
    "       for (j = 0; j < forms.length; j++) {\n" +
    "              f = forms[j];\n" +
    "              if (typeof(f.action)  == \"string\") {\n" +
    "                 if(typeof(f.method)  == \"string\") {\n" +
    "                     if(typeof(f.method) != \"post\") {\n" +
    "                        f.action = urlPrefix + encodeURIComponent(f.action);\n" +
    "                     }\n" +
    "                  }\n" +
    "              }\n" +
    "        }\n" +
    "    }\n" +
    "\n" +
    "   var interceptRunAlready = false;\n" +
    "   function intercept_js_href_iawm(destination) {\n" +
    "     if(!interceptRunAlready &&top.location.href != destination) {\n" +
    "       interceptRunAlready = true;\n" +
    "       top.location.href = urlPrefix+xResolveUrl(destination);\n" +
    "     }\n" +
    "   } \n" +
    "   // ie triggers\n" +
    "   href_iawmWatcher = document.createElement(\"a\");\n" +
    "   top.location.href_iawm = top.location.href;\n" +
    "   if(href_iawmWatcher.setExpression) {\n" +
    "     href_iawmWatcher.setExpression(\"dummy\",\"intercept_js_href_iawm(top.location.href_iawm)\");\n" +
    "   }\n" +
    "   // mozilla triggers\n" +
    "   function intercept_js_moz(prop,oldval,newval) {\n" +
    "     intercept_js_href_iawm(newval);\n" +
    "     return newval;\n" +
    "   }\n" +
    "   if(top.location.watch) {\n" +
    "     top.location.watch(\"href_iawm\",intercept_js_moz);\n" +
    "   }\n" +
    "\n" +
    "   var notice = \n" +
    "     \"<div style=\'\" +\n" +
    "     \"position:relative;z-index:99999;\"+\n" +
    "     \"border:1px solid;color:black;background-color:lightYellow;font-size:10px;font-family:sans-serif;padding:5px\'>\" + \n" +
    "     weraNotice +\n" +
    "  	 \" [ <a style=\'color:blue;font-size:10px;text-decoration:underline\' href=\\\"javascript:void(top.disclaimElem.style.display=\'none\')\\\">\" + weraHideNotice + \"</a> ]\" +\n" +
    "     \"</div>\";\n" +
    "\n" +
    "    function getFrameArea(frame) {\n" +
    "      if(frame.innerWidth) return frame.innerWidth * frame.innerHeight;\n" +
    "      if(frame.document.documentElement && frame.document.documentElement.clientHeight) return frame.document.documentElement.clientWidth * frame.document.documentElement.clientHeight;\n" +
    "      if(frame.document.body) return frame.document.body.clientWidth * frame.document.body.clientHeight;\n" +
    "      return 0;\n" +
    "    }\n" +
    "\n" +
    "    function disclaim() {\n" +
    "      if(top!=self) {\n" +
    "        largestArea = 0;\n" +
    "        largestFrame = null;\n" +
    "        for(i=0;i<top.frames.length;i++) {\n" +
    "          frame = top.frames[i];\n" +
    "          area = getFrameArea(frame);\n" +
    "          if(area > largestArea) {\n" +
    "            largestFrame = frame;\n" +
    "            largestArea = area;\n" +
    "          }\n" +
    "        }\n" +
    "        if(self!=largestFrame) {\n" +
    "          return;\n" +
    "        }\n" +
    "      }\n" +
    "     disclaimElem = document.createElement(\'div\');\n" +
    "     disclaimElem.innerHTML = notice;\n" +
    "     top.disclaimElem = disclaimElem;\n" +
    "     document.body.insertBefore(disclaimElem,document.body.firstChild);\n" +
    "    }\n" +
    "    // disclaim();\n" +
    "\n" +
    "-->\n" +
    "\n" +
    "</SCRIPT>\n";

  // don't hold onto objects after request finished
  protected void resetLocals() {
    cu = null;
    ctype = null;
    url = null;
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
    // XXX initialize the Javascript based on the config
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
    url = getParameter("url");
    if (StringUtil.isNullString(url)) {
      log.warning("url is null");
      displayError("ServeContent needs a non-null URL to display");
      return;
    }
    log.debug("url " + url);
    // Get the CachedUrl for the URL, only if it has content.
    cu = pluginMgr.findCachedUrl(url, true);
    if (cu == null || !cu.hasContent()) {
      log.debug(url + " not found");
      // XXX the right thing to do here is to forward the request
      // XXX to the original URL,  at least if configured to do so
      displayError("URL " + url + " not found");
      return;
    }
    clen = cu.getContentSize();
    try {
      CIProperties props = cu.getProperties();
      ctype = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
      log.debug(url + " type " + ctype + " size " + clen);
      displayContent();
    } finally {
      cu.release();
    }
  }

  void displayContent() {
    if (log.isDebug3()) {
      log.debug3("url: " + url);
      log.debug3("ctype: " + ctype);
      log.debug3("clen: " + clen);
    }
    resp.setContentType(ctype);
    if (clen <= Integer.MAX_VALUE) {
      resp.setContentLength((int)clen);
    }
    Writer out = null;
    Reader in = null;
    try {
      out = resp.getWriter();
      // Does the plugin provide URL rewriting?
      in = cu.openWithUrlRewriting();
      if (in == null) {
	// No plugin URL rewriting, is it HTML?
	if (ctype.startsWith("text/html")) {
	  log.debug(url + " default rewriting");
	  // HTML gets default URL rewriting
	  StringBuffer jsInit = initializeJs();
	  in = new StringFilter(cu.openForReading(), jsTag,
				jsInit.toString() + jsText);
	} else {
	  // Non-HTML not rewritten
	  log.debug(url + " no rewriting");
	  in = cu.openForReading();
	}
      } else {
	log.debug(url + " plugin rewriting");
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

  void displayError(String error) throws IOException {
    // XXX this should be a 404
    Page page = newPage();
    Composite comp = new Composite();
    comp.add("<center><font color=red size=+1>");
    comp.add(error);
    comp.add("</font></center><br>");
    page.add(comp);
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  private StringBuffer initializeJs() {
    Collection urlStems = cu.getArchivalUnit().getUrlStems();
    StringBuffer ret = new StringBuffer();
    String myAdminPort = "8081"; // XXX get from config
    // XXX add LOCKSS prefix to all Javascript variables
    ret.append("<SCRIPT language=\"Javascript\">\n");
    ret.append("urlLocalPrefix = \"http://" +
	       PlatformUtil.getLocalHostname() + ":" +
	       myAdminPort + "\"\n");
    ret.append("urlPrefix = urlLocalPrefix + \"/ServeContent?url=\"\n");
    ret.append("urlSuffix = \"" + url + "\"\n");
    ret.append("urlTarget = \"" + (String)(urlStems.toArray()[0]) + "\"\n");
    ret.append("weraNotice = \"LOCKSS: external links, forms and search may not work\"\n");
    ret.append("weraHideNotice = \"hide\"\n");
    ret.append("</SCRIPT>\n");
    return ret;
  }

}
