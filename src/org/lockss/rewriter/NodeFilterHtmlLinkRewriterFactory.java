/*
 * $Id: NodeFilterHtmlLinkRewriterFactory.java,v 1.2 2008-06-20 23:20:24 dshr Exp $
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

  public InputStream createLinkRewriter(String mimeType,
					ArchivalUnit au,
					InputStream in,
					String encoding,
					String url)
      throws PluginException {
    if ("text/html".equalsIgnoreCase(mimeType)) {
      logger.debug("Rewriting " + url + " in AU " + au);
      int port = 8080; // XXX get from configuration
      String targetStem = "http://" + PlatformUtil.getLocalHostname() + ":" +
	port + "/ServeContent?url=";
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
	linkRegex1[i] = urlStem;
	ignCase1[i] = true;
	rwRegex1[i] = urlStem;
	rwTarget1[i] = targetStem + urlStem;
	logger.debug3("if match " + linkRegex1[i] + " replace " + rwRegex1[i] +
		      " by " + rwTarget1[i]);
	i++;
      }
      if (defUrlStem == null) {
	throw new PluginException("No default URL stem for " + url);
      }
      // Create a LinkRegexXform pipeline
      NodeFilter absLinkXform = HtmlNodeFilters.linkRegexYesXforms(linkRegex1,
								   ignCase1,
								   rwRegex1,
								   rwTarget1);
      int p = url.lastIndexOf("/");
      if (p < 0) {
	throw new PluginException(url + " has no \"/\"");
      }
      String urlPrefix = url.substring(0, p);
      logger.debug3("urlPrefix: " + urlPrefix);
      String [] linkRegex2 = {
	"^http://",
	"^http://",
      };
      boolean[] ignCase2 = {
	true,
	true,
      };
      String[] rwRegex2 = {
	"^/",
	"^([a-z])",
      };
      String[] rwTarget2 = {
	targetStem + defUrlStem,
	targetStem + urlPrefix + "/$1",
      };

      // Rewrite relative links
      NodeFilter relLinkXform = HtmlNodeFilters.linkRegexNoXforms(linkRegex2,
								  ignCase2,
								  rwRegex2,
								  rwTarget2);
      // Combine the pipes
      NodeFilter linkXform = new OrFilter(absLinkXform, relLinkXform);
      // Create a transform to apply them
      HtmlTransform htmlXform = HtmlNodeFilterTransform.exclude(linkXform);
      return (new HtmlFilterInputStream(in, htmlXform));
    } else {
      throw new PluginException("NodeFilterHtmlLinkRewriterFactory vs. " +
				mimeType);
    }
  }
}
