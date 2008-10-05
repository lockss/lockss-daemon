/*
 * $Id: StringFilterCssLinkRewriterFactory.java,v 1.2 2008-09-18 02:10:23 dshr Exp $
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
 * StringFilterCssLinkRewriterFactory creates link rewriters that
 * rewrite links in CSS using string filters.
 */
public class StringFilterCssLinkRewriterFactory implements LinkRewriterFactory {
  static final Logger logger =
    Logger.getLogger("StringFilterCssLinkRewriterFactory");

  public StringFilterCssLinkRewriterFactory() {
  }

  public Reader createLinkRewriterReader(String mimeType,
					 ArchivalUnit au,
					 Reader in,
					 String encoding,
					 String url,
					 ServletUtil.LinkTransform xform)
      throws PluginException {
    if ("text/css".equalsIgnoreCase(mimeType)) {
      logger.debug("Rewriting " + url + " in AU " + au);

      int port = 0;
      try {
	  port = CurrentConfig.getIntParam(ContentServletManager.PARAM_PORT);
      } catch (org.lockss.config.Configuration.InvalidParam ex) {
	  throw new PluginException("No port available: " + ex);
      }
      // urlPrefix is http://host:port/ServeContent?url=
      String urlPrefix = xform.rewrite("");
      // urlBase is url made absolute up to but excluding the last /
      String urlBase = makeBase(au, url);
      // urlHost is urlBase with enough ../ to get back to site root.
      String urlHost = makeHost(au, url);
      Collection urlStems = au.getUrlStems();
      int nUrlStem = urlStems.size();
      String[][] replace1 = {
	// Rewrite absolute CSS links out of the way
	{"@import url(", "^http://", "@@@http://"},
	// Rewrite relative to absolute (prefix added later)
	{"@import url(", "^([a-z])", urlBase + "/$1"},
	// Rewrite relative to absolute (prefix added later)
	{"@import url(", "^(\\.\\./)", urlBase + "/$1"},
	// Rewrite relative to absolute with prefix
	{"@import url(", "^/", urlHost + "/"},
	// Rewrite absolute CSS links back in to the way
	{"@import url(", "^@@@http://", "http://"},
      };
      String[][] replace = new String[replace1.length + nUrlStem][3];
      int i = 0;
      for ( ; i < replace1.length; i++) {
	replace[i] = replace1[i];
      }
      for (Iterator it = urlStems.iterator(); it.hasNext(); ) {
	String urlStem = (String)it.next();
	String[] line = {"@import url(", "^" + urlStem, urlPrefix + urlStem};
	replace[i++] = line;
      }
      CssLinkFilter sf = CssLinkFilter.makeNestedFilter(in, replace, true);
      return sf;
    } else {
      throw new PluginException("StringFilterCssLinkRewriterFactory vs. " +
				mimeType);
    }
  }

  private String makeBase(ArchivalUnit au, String url) 
      throws PluginException {
    if (url.startsWith("http://")) {
      int idx = url.lastIndexOf('/');
      return url.substring(0, idx);
    } else {
      throw new PluginException("CSS base url not absolute");
    }
  }
  private String makeHost(ArchivalUnit au, String url) 
      throws PluginException {
    if (url.startsWith("http://")) {
      int lastSlash = url.lastIndexOf('/');
      if (lastSlash <= 7) {
	throw new PluginException("CSS base url malformed");
      }
      String ret = url.substring(0, lastSlash);
      int idx = url.indexOf('/', 7);
      if (idx < url.length()) {
	String fromRoot = url.substring(idx + 1);
	int numSlash = 0;
	for (int i = 0; i < fromRoot.length(); i++) {
	  if (fromRoot.charAt(i) == '/') {
	    numSlash++;
	  }
	}
	for (int i = 0; i < numSlash; i++) {
	  ret += "/..";
	}
      }
      return ret;
    } else {
      throw new PluginException("CSS base url not absolute");
    }
  }
}


