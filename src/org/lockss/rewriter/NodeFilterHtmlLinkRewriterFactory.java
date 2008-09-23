/*
 * $Id: NodeFilterHtmlLinkRewriterFactory.java,v 1.12 2008-09-23 00:51:03 tlipkis Exp $
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
import org.lockss.filter.html.*;
import org.lockss.config.*;
import org.lockss.servlet.*;
import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.htmlparser.filters.*;

/**
 * NodeFilterHtmlLinkRewriterFactory creates link rewriters that
 * work using HtmlNodeFilter instances to rewrite:
 * - absolute links to preserved content within the AU
 * - relative links
 * to absolute links to the ServeContent servlet.
 *
 */
public class NodeFilterHtmlLinkRewriterFactory implements LinkRewriterFactory {
  static final Logger logger =
    Logger.getLogger("NodeFilterHtmlLinkRewriterFactory");

  public NodeFilterHtmlLinkRewriterFactory() {
  }

  /*
   * These are the attributes of HTML tags that can contain URLs to rewrite.
   */
  private static final String[] attrs = {
    "href",
    "src",
    "action",
    "background",
    // "archive",  // applet
    // "codebase", // applet, object
    // "cite",     // blockquote, del, ins
    // "pluginspage", // embed
    // "longdesc", // frame
    // "longdesc", // iframe, img
    // "dynsrc",   // img
    // "lowsrc",   // img
    // "usemap",   // img, object
    // "classid",  // object
    // "data",     // object
  };

  public Reader createLinkRewriterReader(String mimeType,
					 ArchivalUnit au,
					 Reader in,
					 String encoding,
					 String url,
					 ServletUtil.LinkTransform xform)
      throws PluginException {
    if ("text/html".equalsIgnoreCase(mimeType)) {
      logger.debug("Rewriting " + url + " in AU " + au);
      int port = 0;
      try {
	  port = CurrentConfig.getIntParam(ContentServletManager.PARAM_PORT);
      } catch (org.lockss.config.Configuration.InvalidParam ex) {
	  throw new PluginException("No port available: " + ex);
      }
      String targetStem = xform.rewrite("");  // XXX - should have better xform
      Collection urlStems = au.getUrlStems();
      int nUrlStem = urlStems.size();
      String defUrlStem = null;
      int l = nUrlStem;
      String[] linkRegex1 = new String[l];
      boolean[] ignCase1 = new boolean[l];
      String[] rwRegex1 = new String[l];
      String[] rwTarget1 = new String[l];
      // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
      int i = 0;
      for (Iterator it = urlStems.iterator(); it.hasNext(); ) {
	String urlStem = (String)it.next();
	if (defUrlStem == null) {
	  defUrlStem = urlStem;
	}
	linkRegex1[i] = "^" + urlStem;
	ignCase1[i] = true;
	rwRegex1[i] = urlStem;
	rwTarget1[i] = targetStem + urlStem;
	logger.debug3("if link match " + linkRegex1[i] + " replace " +
		      rwRegex1[i] + " by " + rwTarget1[i]);
	i++;
      }
      if (defUrlStem == null) {
	throw new PluginException("No default URL stem for " + url);
      }
      // Create a LinkRegexXform pipeline
      NodeFilter absLinkXform =
	HtmlNodeFilters.linkRegexYesXforms(linkRegex1, ignCase1, rwRegex1,
					   rwTarget1, attrs);
      String urlSub = url.substring(7);
      int p = urlSub.lastIndexOf("/");
      if (p < 0) {
	logger.debug(url + " has no last /");
	p = urlSub.length() - 1;
      }
      String urlPrefix = "http://" + urlSub.substring(0, p);
      logger.debug3("urlPrefix: " + urlPrefix);
      String [] linkRegex2 = {
	"^http://",
	"^http://",
	"^http://",
      };
      boolean[] ignCase2 = {
	true,
	true,
	true,
      };
      String[] rwRegex2 = {
	"^/",
	"^([a-zA-Z_?])",
	"^(\\.\\./)",
      };
      String[] rwTarget2 = {
	targetStem + defUrlStem,
	targetStem + urlPrefix + "/$1",
	targetStem + urlPrefix + "/$1",
      };

      // Rewrite relative links
      NodeFilter relLinkXform =
	HtmlNodeFilters.linkRegexNoXforms(linkRegex2, ignCase2, rwRegex2,
					  rwTarget2, attrs);

      // Rewrite CSS style imports
      String[] linkRegex3 = new String[l];
      boolean[] ignCase3 = new boolean[l];
      String[] rwRegex3 = new String[l];
      String[] rwTarget3 = new String[l];
      // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
      i = 0;
      for (Iterator it = urlStems.iterator(); it.hasNext(); ) {
	String urlStem = (String)it.next();
	if (defUrlStem == null) {
	  defUrlStem = urlStem;
	}
	linkRegex3[i] = "url\\(" + urlStem;
	ignCase3[i] = true;
	rwRegex3[i] = "url\\(" + urlStem;
	rwTarget3[i] = "url(" + targetStem + urlStem;
	logger.debug3("if css match " + linkRegex1[i] + " replace " +
		      rwRegex1[i] + " by " + rwTarget1[i]);
	i++;
      }
      NodeFilter absImportXform =
	HtmlNodeFilters.styleRegexYesXforms(linkRegex3, ignCase3,
					    rwRegex3, rwTarget3);
      String [] linkRegex4 = {
	"url\\(http://",
	"url\\(http://",
	"url\\(http://",
      };
      boolean[] ignCase4 = {
	true,
	true,
	true,
      };
      String[] rwRegex4 = {
	"url\\(/",
	"url\\(([a-zA-Z])",
	"url\\((\\.\\./)",
      };
      String[] rwTarget4 = {
	"url(" + targetStem + defUrlStem,
	"url(" + targetStem + urlPrefix + "/$1",
	"url(" + targetStem + urlPrefix + "/$1",
      };
      NodeFilter relImportXform =
	HtmlNodeFilters.styleRegexNoXforms(linkRegex4, ignCase4,
					    rwRegex4, rwTarget4);

      // Rewrite <meta http-equiv="refresh" content="url=1; url=...">
      String[] linkRegex5 = new String[l];
      boolean[] ignCase5 = new boolean[l];
      String[] rwRegex5 = new String[l];
      String[] rwTarget5 = new String[l];
      // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
      i = 0;
      for (Iterator it = urlStems.iterator(); it.hasNext(); ) {
	String urlStem = (String)it.next();
	if (defUrlStem == null) {
	  defUrlStem = urlStem;
	}
	linkRegex5[i] = ";[ \\t\\n\\f\\r\\x0b]*url=" + urlStem;
	ignCase5[i] = true;
	rwRegex5[i] = "url=" + urlStem;
	rwTarget5[i] = "url=" + targetStem + urlStem;
	logger.debug3("if meta match " + linkRegex1[i] + " replace " +
		      rwRegex1[i] + " by " + rwTarget1[i]);
	i++;
      }
      NodeFilter absRefreshXform =
	HtmlNodeFilters.refreshRegexYesXforms(linkRegex5, ignCase5,
					    rwRegex5, rwTarget5);
      String [] linkRegex6 = {
	";[ \\t\\n\\f\\r\\x0b]*url=http://",
	";[ \\t\\n\\f\\r\\x0b]*url=http://",
	";[ \\t\\n\\f\\r\\x0b]*url=http://",
      };
      boolean[] ignCase6 = {
	true,
	true,
	true,
      };
      String[] rwRegex6 = {
	"url=/",
	"url=([a-zA-Z_?])",
	"url=(\\.\\./)",
      };
      String[] rwTarget6 = {
	"url=" + targetStem + defUrlStem,
	"url=" + targetStem + urlPrefix + "/$1",
	"url=" + targetStem + urlPrefix + "/$1",
      };

      NodeFilter relRefreshXform =
	HtmlNodeFilters.refreshRegexNoXforms(linkRegex6, ignCase6,
					    rwRegex6, rwTarget6);
      // Combine the pipes
      NodeFilter[] filters = {
	absLinkXform,
	relLinkXform,
	absImportXform,
	relImportXform,
	absRefreshXform,
	relRefreshXform,
      };
      NodeFilter linkXform = filters[0];
      for (int j = 1; j < filters.length; j++) {
	logger.debug3("index " + j + " filter " + filters[j]);
	linkXform = new OrFilter(linkXform, filters[j]);
      }
      // Create a transform to apply them
      HtmlTransform htmlXform = HtmlNodeFilterTransform.exclude(linkXform);
      InputStream result = new HtmlFilterInputStream(new ReaderInputStream(in),
						     htmlXform);
      try {
        return new InputStreamReader(result, encoding);
      } catch (UnsupportedEncodingException ex) {
	logger.error(encoding + " threw " + ex);
	return in;
      }
    } else {
      throw new PluginException("NodeFilterHtmlLinkRewriterFactory vs. " +
				mimeType);
    }
  }
}
