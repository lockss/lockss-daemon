/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.rewriter;

import java.io.*;
import java.util.*;
import java.text.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.filter.*;
import org.lockss.config.*;
import org.lockss.servlet.*;

/**
 * JavascriptHtmlLinkRewriterFactory creates link rewriters that
 * work by injecting the Internet Archive (via WERA) Javascript
 * into the HTML page.
 */
public class JavascriptHtmlLinkRewriterFactory implements LinkRewriterFactory {
  static final Logger logger =
    Logger.getLogger("JavascriptHtmlLinkRewriterFactory");

  public JavascriptHtmlLinkRewriterFactory() {
  }

  // Insert Javascript at this tag
  private static final String jsTag = "</html>";
  // Javascript to insert
  private static final String jsTextName = "urlrewriter.js";

  public InputStream createLinkRewriter(String mimeType,
					ArchivalUnit au,
					InputStream in,
					String encoding,
					String url,
					ServletUtil.LinkTransform xform)
      throws PluginException {
    logger.debug2("Rewriting " + url + " in AU " + au);
    // HTML gets default URL rewriting
    StringBuffer jsInit = initializeJs(au, url, xform);
    StringBuffer jsText = getJs();
    String replace = jsInit.toString() + jsText + "\n" + jsTag;
    StringFilter sf = new StringFilter(FilterUtil.getReader(in, encoding),
				       jsTag, replace);
    sf.setIgnoreCase(true);
    return new ReaderInputStream(sf, encoding);
  }

  private StringBuffer getJs() throws PluginException {
    InputStream in = null;
    StringBuffer ret = new StringBuffer();
    in = this.getClass().getResourceAsStream(jsTextName);
    if (in == null) {
      throw new PluginException(jsTextName + " missing");
    }
    try {
      Reader isr = new InputStreamReader(in);
      char[] cbuf = new char[4096];
      int i = 0;
      while ((i = isr.read(cbuf)) > 0) {
	ret.append(cbuf, 0, i);
      }
      return ret;
    } catch (IOException ex) {
      throw new PluginException(jsTextName + " threw " + ex);
    }
  }
    
  private StringBuffer initializeJs(ArchivalUnit au,
				    String url,
				    ServletUtil.LinkTransform xform)
      throws PluginException {
    Collection urlStems = au.getUrlStems();
    StringBuffer ret = new StringBuffer();
    int port = 0;
    try {
      port = CurrentConfig.getIntParam(ContentServletManager.PARAM_PORT);
    } catch (org.lockss.config.Configuration.InvalidParam ex) {
      throw new PluginException("No port available: " + ex);
    }
    // XXX add LOCKSS prefix to all Javascript variables
    ret.append("<SCRIPT language=\"Javascript\">\n");
    ret.append("urlPrefix = \"" + xform.rewrite("") + "\"\n");
    ret.append("urlSuffix = \"" + url + "\"\n");
    // XXX bug if more than 1 URL stem for the AU
    ret.append("urlTarget = \"" + (String)(urlStems.toArray()[0]) + "\"\n");
    ret.append("weraNotice = \"LOCKSS: external links, forms and search may not work\"\n");
    ret.append("weraHideNotice = \"hide\"\n");
    ret.append("</SCRIPT>\n");
    return ret;
  }
}


