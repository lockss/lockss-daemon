/*
 * $Id: NodeFilterHtmlLinkRewriterFactory.java,v 1.18 2011-05-09 00:37:38 tlipkis Exp $
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
import java.net.*;
import java.util.*;
import java.text.*;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.config.*;
import org.lockss.servlet.*;
import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.htmlparser.nodes.*;
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

  // Legal start to relative path (in relative URL).  Maybe this should be
  // (not "^/")?
  static String relChar = "[-a-zA-Z0-9$_@.&+!*\"\'(),%?]";



  public InputStream createLinkRewriter(String mimeType,
					ArchivalUnit au,
					InputStream in,
					String encoding,
					String url,
					final ServletUtil.LinkTransform srvLink)
      throws PluginException {
    logger.debug2("Rewriting " + url + " in AU " + au);
    final String targetStem = srvLink.rewrite("");  // XXX - should have better xform
    logger.debug2("targetStem: " + targetStem);
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
    final String urlPrefix = "http://" + urlSub.substring(0, p);
    logger.debug3("urlPrefix: " + urlPrefix);

    HtmlBaseProcessor base = new HtmlBaseProcessor();
    List<RelXform> relXforms = new ArrayList<RelXform>();

    // Rewrite relative links
    RelLinkRegexXform relLinkXforms[] = {
      new RelLinkRegexXform("^http://", true,
			    "^/",
			    targetStem + defUrlStem,
			    attrs) {
	public void setBaseUrl(String baseUrl)
	    throws MalformedURLException {
	  setReplace(srvLink.rewrite(UrlUtil.getUrlPrefix(baseUrl)));
	}},
      new RelLinkRegexXform("^http://", true,
			    "^(" + relChar + ")",
			    targetStem + urlPrefix + "/$1",
			    attrs) {
	public void setBaseUrl(String baseUrl)
	    throws MalformedURLException {
	  setReplace(srvLink.rewrite(UrlUtil.resolveUri(baseUrl, "$1")));
	}}
    };
    for (RelLinkRegexXform xform : relLinkXforms) {
      xform.setNegateFilter(true);
      relXforms.add(xform);
    }
    NodeFilter relLinkXform = new OrFilter(relLinkXforms);

    // Rewrite CSS style imports
    String[] linkRegex3 = new String[l];
    boolean[] ignCase3 = new boolean[l];
    String[] rwRegex3 = new String[l];
    String[] rwTarget3 = new String[l];
    // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
    i = 0;
    for (Iterator it = urlStems.iterator(); it.hasNext(); ) {
      String urlStem = (String)it.next();
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

    RelStyleRegexXform[] relStyleXforms = {
      new RelStyleRegexXform("url\\(http://", true,
			     "url\\(/",
			     "url(" + targetStem + defUrlStem) {
	public void setBaseUrl(String baseUrl)
	    throws MalformedURLException {
	  setReplace("url(" + srvLink.rewrite(UrlUtil.getUrlPrefix(baseUrl)));
	}},
      new RelStyleRegexXform("url\\(http://", true,
			     "url\\((" + relChar + ")",
			     "url(" + targetStem + defUrlStem) {
	public void setBaseUrl(String baseUrl)
	    throws MalformedURLException {
	  setReplace("url(" +
		     srvLink.rewrite(UrlUtil.resolveUri(baseUrl, "$1")));
	}},
    };
    for (RelStyleRegexXform xform : relStyleXforms) {
      xform.setNegateFilter(true);
      relXforms.add(xform);
    }
    NodeFilter relImportXform = new OrFilter(relStyleXforms);

    // Rewrite <meta http-equiv="refresh" content="url=1; url=...">
    String[] linkRegex5 = new String[l];
    boolean[] ignCase5 = new boolean[l];
    String[] rwRegex5 = new String[l];
    String[] rwTarget5 = new String[l];
    // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
    i = 0;
    for (Iterator it = urlStems.iterator(); it.hasNext(); ) {
      String urlStem = (String)it.next();
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

    RelRefreshRegexXform[] relRefreshXforms = {
      new RelRefreshRegexXform(";[ \\t\\n\\f\\r\\x0b]*url=http://", true,
			       "url=/",
			       "url=" + targetStem + defUrlStem) {
	public void setBaseUrl(String baseUrl)
	    throws MalformedURLException {
	  setReplace("url=" + srvLink.rewrite(UrlUtil.getUrlPrefix(baseUrl)));
	}},
      new RelRefreshRegexXform(";[ \\t\\n\\f\\r\\x0b]*url=http://", true,
			       "url=(" + relChar + ")",
			       "url=" + targetStem + urlPrefix + "/$1") {
	public void setBaseUrl(String baseUrl)
	    throws MalformedURLException {
	  setReplace("url=" +
		     srvLink.rewrite(UrlUtil.resolveUri(baseUrl, "$1")));
	}},
    };
    for (RelRefreshRegexXform xform : relRefreshXforms) {
      xform.setNegateFilter(true);
      relXforms.add(xform);
    }
    NodeFilter relRefreshXform = new OrFilter(relRefreshXforms);

    for (RelXform xform : relXforms) {
      try {
	xform.setBaseUrl(url);
      } catch (MalformedURLException e) {
	throw new IllegalArgumentException(e);
      }
    }
    base.setXforms(relXforms);

    // Combine the pipes
    NodeFilter[] filters = {
      base,
      absLinkXform,
      relLinkXform,
      absImportXform,
      relImportXform,
      absRefreshXform,
      relRefreshXform,
    };

    NodeFilter linkXform = new OrFilter(filters);
    // Create a transform to apply them
    HtmlTransform htmlXform = HtmlNodeFilterTransform.exclude(linkXform);
    InputStream result = new HtmlFilterInputStream(in,
						   encoding,
						   encoding,
						   htmlXform);
    return result;
  }

  public static class HtmlBaseProcessor extends TagNameFilter {
    private List<RelXform> xforms;

    public HtmlBaseProcessor() {
      super("BASE");
    }

    public void setXforms(List<RelXform> xforms) {
      this.xforms = xforms;
    }

    public boolean accept(Node node) {
      if (!super.accept(node)) {
	return false;
      }
      TagNode tnode = (TagNode)node;
      Attribute attr = ((TagNode)node).getAttributeEx("href");
      if (attr != null) {
	String newbase = attr.getValue();
	for (RelXform xform : xforms) {
	  try {
	    xform.setBaseUrl(newbase);
	  } catch (MalformedURLException e) {
	    logger.warning("Not resetting rewriter's base URL", e);
	  }
	}
      }
      return false;
    }
  }

  interface RelXform extends NodeFilter {
    void setBaseUrl(String baseUrl) throws MalformedURLException;
  }

  abstract class RelLinkRegexXform extends HtmlNodeFilters.LinkRegexXform
    implements RelXform {

    RelLinkRegexXform(String regex, boolean ignoreCase,
		      String target, String replace, String[] attrs) {
      super(regex, ignoreCase, target, null, attrs);
    }
  }

  abstract class RelStyleRegexXform extends HtmlNodeFilters.StyleRegexXform
    implements RelXform {

    public RelStyleRegexXform(String regex, boolean ignoreCase,
			      String target, String replace) {
      super(regex, ignoreCase, target, null);
    }
  }

  abstract class RelRefreshRegexXform extends HtmlNodeFilters.RefreshRegexXform
    implements RelXform {

    public RelRefreshRegexXform(String regex, boolean ignoreCase,
				String target, String replace) {
      super(regex, ignoreCase, target, null);
    }
  }

}
