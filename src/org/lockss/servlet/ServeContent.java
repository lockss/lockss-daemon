/*
 * $Id: ServeContent.java,v 1.3 2007-11-20 23:18:46 dshr Exp $
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
  private String doi;
  private String issn;
  private String volume;
  private String issue;
  private String spage;
  private String ctype;
  private CachedUrl cu;
  private long clen;

  private PluginManager pluginMgr;
  private LocalServletManager srvltMgr;

  // If we can't resolve a DOI, here is where to send it
  private static final String DOI_LOOKUP_URL = "http://dx.doi.org/";
  // If we can't resolve an OpenURL, here is where to send it
  // XXX find the place to send it
  private static final String OPENURL_LOOKUP_URL = "http://www.lockss.org/";

  // Insert Javascript at this tag
  private static final String jsTag = "</html>";
  // Javascript to insert
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
    "        var prop = aCollection[i][sProp];\n" +
    "        if (typeof(prop) == \"string\") { \n" +
    "          if (prop.indexOf(\"mailto:\") == -1 && prop.indexOf(\"javascript:\") == -1) {\n" +
    "            // aCollection[i][\"target\"] = \"_top\";\n" +
    "            if(prop.indexOf(\"/cgi/reprint\") >= 0) {\n" +
    "              //alert(\"PDF(\" + prop + \"): \" + sProp);\n" +
    "              prop = prop + \".pdf\";\n" +
    "            }\n" +
    "            if(prop.indexOf(urlTarget) == 0) {\n" +
    "              //alert(\"Journal(\" + prop + \"): \" + sProp);\n" +
    "              prop = urlPrefix + encodeURIComponent(prop);\n" +
    "            } else if (prop.indexOf(urlPrefix+urlTarget) == 0) {\n" +
    "              //alert(\"Munged(\" + prop + \"): \" + sProp);\n" +
    "              //prop = urlPrefix + urlTarget + encodeURIComponent(prop.substring(urlPrefix.length+urlTarget.length+1));\n" +
    "            } else if (prop.indexOf(urlPrefix) == 0) {\n" +
    "              //alert(\"Local(\" + prop + \"): \" + sProp);\n" +
    "              //prop = encodeURIComponent(prop);\n" +
    "            } else if (prop.indexOf(urlLocalPrefix) == 0) {\n" +
    "              //alert(\"Local(\" + prop + \"): \" + sProp);\n" +
    "              prop = urlPrefix + urlTarget + encodeURIComponent(prop.substring(urlLocalPrefix.length+1));\n" +
    "            } else if (prop.indexOf(\"/\") == 0) {\n" +
    "              //alert(\"Relative(\" + prop + \"): \" + sProp);\n" +
    "              prop = urlPrefix + urlTarget + encodeURIComponent(prop.substring(1));\n" +
    "            } else if (prop.indexOf(\"#\") == 0) {\n" +
    "              //alert(\"Relative2(\" + prop + \"): \" + sProp);\n" +
    "              prop = urlPrefix + encodeURIComponent(urlSuffix + prop);\n" +
    "            } else if (prop.indexOf(\"http\") != 0) {\n" +
    "              //alert(\"Relative3(\" + prop + \"): \" + sProp);\n" +
    "              prop = urlPrefix + urlTarget + encodeURIComponent(prop);\n" +
    "            }\n" +
    "            //alert(\"xLatedUrl(\" + prop + \"): \" + sProp);\n" +
    "            aCollection[i][sProp] = prop;\n" +
    "          }\n" +
    "        }\n" +
    "      }\n" +
    "   }\n" +
    "\n" +
    "   //debugger;\n" +
    "   xLateUrl(document.getElementsByTagName(\"IMG\"),\"src\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"A\"),\"href\",\"standalone\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"AREA\"),\"href\",\"standalone\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"LINK\"),\"href\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"OBJECT\"),\"codebase\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"OBJECT\"),\"data\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"APPLET\"),\"codebase\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"APPLET\"),\"archive\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"EMBED\"),\"src\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"BODY\"),\"background\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"SCRIPT\"),\"src\",\"inline\");\n" +
    "   xLateUrl(document.getElementsByTagName(\"FRAME\"),\"src\",\"inline\");\n" +
    "   var frames = document.getElementsByTagName(\"FRAME\",\"inline\");\n" +
    "   if (frames) {\n" +
    "       for (k = 0; k < frames.length; k++) {\n" +
    "           if (typeof(frames[i][\"src\"]) == \"string\") {\n" +
    "               var l = frames[i][\"src\"].indexOf(\"frameset_url=\");\n" +
    "               if (l > -1) {\n" +
    "                   frames[i][\"src\"] = frames[i][\"src\"].substring(0,l+13) +\n" +
    "                       urlPrefix + xLateUrl(frames[i][\"src\"].substring(l+14));\n" +
    "               }\n" +
    "           }\n" +
    "       }\n" +
    "   }\n" +
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
    doi = null;
    issn = null;
    volume = null;
    issue = null;
    spage = null;
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
    url = getParameter("url");
    if (!StringUtil.isNullString(url)) {
      handleUrlRequest();
      return;
    }
    doi = getParameter("doi");
    if (!StringUtil.isNullString(doi)) {
      handleDoiRequest();
      return;
    }
    issn = getParameter("issn");
    volume = getParameter("volume");
    issue = getParameter("issue");
    spage = getParameter("spage");
    log.debug("issn " + issn + " volume " + volume + " issue " + issue + " spage " + spage);
    if (!StringUtil.isNullString(issn) &&
	!StringUtil.isNullString(volume) &&
	!StringUtil.isNullString(issue) &&
	!StringUtil.isNullString(spage)) {
      handleOpenUrlRequest();
      return;
    }
    log.warning("url, doi, openUrl all null");
    displayError("ServeContent needs a non-null URL, DOI or OpenURL to display");
  }

  protected void handleUrlRequest() throws IOException {
    log.debug("url " + url);
    // Get the CachedUrl for the URL, only if it has content.
    cu = pluginMgr.findCachedUrl(url, true);
    if (cu == null || !cu.hasContent()) {
      log.debug(url + " not found");
      handleMissingUrlRequest(url);
      return;
    }
    handleCuRequest();
  }

  protected void handleCuRequest() {
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

  protected void handleDoiRequest() throws IOException {
    log.debug("doi " + doi);
    // find the URL for the DOI
    url = Metadata.doiToUrl(doi);
    log.debug(doi + " = " + (url == null ? "null" : url));
    if (url == null) {
      handleMissingUrlRequest(DOI_LOOKUP_URL + doi);
    } else {
      handleUrlRequest();
    }
  }

  protected void handleOpenUrlRequest() throws IOException {
    String openUrl = issn + "/" + volume + "/" + issue + "/" + spage;
    log.debug("OpenUrl " + openUrl);
    // find the URL for the OpenURL
    url = Metadata.openUrlToUrl(openUrl);
    log.debug(openUrl + " = " + (url == null ? "null" : url));
    if (url == null) {
      handleMissingUrlRequest(OPENURL_LOOKUP_URL + openUrl);
    } else {
      handleUrlRequest();
    }
  }

  protected void handleMissingUrlRequest(String missingUrl)
      throws IOException {
    // XXX the right thing to do here is to forward the request
    // XXX to the original URL,  at least if configured to do so
    displayError("URL " + missingUrl + " not found");
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
    Writer outW = null;
    Reader inR = null;
    OutputStream outS = null;
    InputStream inS = null;
    try {
      // XXX need interface to plugin for URL rewriting
      if (ctype.startsWith("text/html")) {
        outW = resp.getWriter();
        inR = cu.openWithUrlRewriting();
        if (inR == null) {
          log.debug(url + " default rewriting");
          // HTML gets default URL rewriting
          StringBuffer jsInit = initializeJs();
          StringFilter sf = new StringFilter(cu.openForReading(), jsTag,
                                             jsInit.toString() + jsText + "\n" + jsTag);
          sf.setIgnoreCase(true);
          inR = sf;
        }
	StreamUtil.copy(inR, outW);
      } else {
	// Non-HTML not rewritten
	log.debug(url + " no rewriting");
	inS = cu.getUnfilteredInputStream();
	outS = resp.getOutputStream();
	StreamUtil.copy(inS, outS);
      }
    } catch (IOException e) {
      log.warning("Copying CU to HTTP stream", e);
    } finally {
      IOUtil.safeClose(outW);
      IOUtil.safeClose(inR);
      IOUtil.safeClose(outS);
      IOUtil.safeClose(inS);
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
    // XXX bug if more than 1 URL stem for the AU
    ret.append("urlTarget = \"" + (String)(urlStems.toArray()[0]) + "\"\n");
    ret.append("weraNotice = \"LOCKSS: external links, forms and search may not work\"\n");
    ret.append("weraHideNotice = \"hide\"\n");
    ret.append("</SCRIPT>\n");
    return ret;
  }

}
