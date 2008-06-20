/*
 * $Id: NodeFilterHtmlLinkRewriterFactory.java,v 1.1 2008-06-20 18:53:51 dshr Exp $
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
 * NB - this link rewriter is only suitable for AUs that contain
 * the whole content of a site.
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
      int l = 1; // XXX
      String[] linkRegex = new String[l];
      boolean[] ignoreCase = new boolean[l];
      String[] rewriteRegex = new String[l];
      String[] rewriteTarget = new String[l];
      for (int i = 0; i < linkRegex.length; i++) {
	linkRegex[i] = "XXX";
	ignoreCase[i] = true;
	rewriteRegex[i] = "XXX";
	rewriteTarget[i] = "XXX";
      }
      // Create a LinkRegexXform pipeline
      NodeFilter linkXform = HtmlNodeFilters.linkRegexXforms(linkRegex,
								ignoreCase,
								rewriteRegex,
								rewriteTarget);
      // Create a transform to apply them
      HtmlTransform htmlXform = HtmlNodeFilterTransform.exclude(linkXform);
      return (new HtmlFilterInputStream(in, htmlXform));
    } else {
      throw new PluginException("NodeFilterHtmlLinkRewriterFactory vs. " +
				mimeType);
    }
  }
}
