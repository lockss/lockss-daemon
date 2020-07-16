/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.filter.html.*;
import org.lockss.filter.html.HtmlNodeFilters.LinkRegexXform;
import org.lockss.filter.html.HtmlNodeFilters.RefreshRegexXform;
import org.lockss.filter.html.HtmlNodeFilters.MetaTagRegexXform;
import org.lockss.servlet.*;
import org.htmlparser.*;
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

  public static final String PARAM_META_TAG_REWRITE_PREFIX =
    Configuration.PREFIX + "htmlRewriter.metaTagRewritePrefix";
  public static String DEFAULT_META_TAG_REWRITE_PREFIX = null;

  public NodeFilterHtmlLinkRewriterFactory() {
  }

  /*
   * These are the attributes of HTML tags that can contain URLs to rewrite.
   */
  private static final String[] DEFAULT_ATTRS = {
    "href",
    "src",
    "action",
    "background",
    "onclick",
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

  private List<String> attrList = null;
  private String[] attrs = DEFAULT_ATTRS;
  private boolean executed = false;
  private List<NodeFilter> preXforms = new ArrayList<NodeFilter>();
  private List<NodeFilter> postXforms = new ArrayList<NodeFilter>();;

  // Legal start to non-server relative path in relative URL  
  static final String relChar = "[-a-zA-Z0-9$_@.&+!*\"\'(),%?#]";
  
  // Matches protocol pattern (e.g. "http://")
  static final String protocolPat = "[^:/?#]+://+";
  
  // Matches protocol prefix of a URL (e.g. "http://")
  static final String protocolPrefixPat = "^" + protocolPat;
  
  // Matches protocol prefix of a URL (e.g. "http://") OR ref ("#...")
  // Used negated to find relative URLs, excluding those that are just a
  // #ref
  static final String protocolOrRefPrefixPat = "^(" + protocolPat + "|#)";

  // Matches HTML refresh attribute 
  static final String refreshPat = ";[ \\t\\n\\f\\r\\x0b]*url=";

  // Matches HTML refresh attribute with a relative URL (no protocol pattern)
  static final String relRefreshPat = refreshPat + "(?!" + protocolPat + ")";

  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        final ServletUtil.LinkTransform srvLink)
      throws PluginException, IOException {
    logger.debug2("Rewriting " + url + " in AU " + au);
    executed = true;
    final String targetStem = srvLink.rewrite("");  // XXX - should have better xform
    logger.debug2("targetStem: " + targetStem);
    Collection<String> urlStems = au.getUrlStems();
    int nUrlStem = urlStems.size();
    String defUrlStem = null;
    // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
    List<LinkRegexXform> absLinkXforms = new ArrayList<>();
    for (String urlStem : urlStems) {
      if (defUrlStem == null) {
        defUrlStem = urlStem;
      }
      absLinkXforms.add(new LinkRegexXform("^" + urlStem, true, urlStem,
				       targetStem + urlStem,
				       getAttrsToRewrite()));
    }
    if (defUrlStem == null) {
      throw new PluginException("No default URL stem for " + url);
    }

    // Transform protocol-relative link URLs.  These are essentially abs
    // links with no scheme.
    for (String urlStem : urlStems) {
      int colon = urlStem.indexOf("://");
      if (colon < 0) continue;
      String proto = urlStem.substring(0, colon);
      String hostPort = urlStem.substring(colon + 3);
      absLinkXforms.add(new LinkRegexXform("^//" + hostPort, true, "^//",
				       targetStem + proto + "://",
				       getAttrsToRewrite()));
    }
    logger.debug3("Abs Xforms: " + absLinkXforms);

    HtmlBaseProcessor base = new HtmlBaseProcessor(url);

    // Rewrite relative links
    List<RelXform> relXforms = new ArrayList<RelXform>();
    @SuppressWarnings("serial")
    RelLinkRegexXform relLinkXforms[] = {
      // transforms site-relative link URLs
      new RelLinkRegexXform(protocolOrRefPrefixPat, // negated, matches relative URL
                            true,
			    "^/($|(?!/))", // match site rel but not proto rel
			    getAttrsToRewrite()) {
        /** Specify the "replace" property using the baseUrl param */
        public void setBaseUrl(String baseUrl)
            throws MalformedURLException {
          setReplace(srvLink.rewrite(UrlUtil.getUrlPrefix(baseUrl)));
        }},
      // transforms path-relative link URLs
      new RelLinkRegexXform(protocolOrRefPrefixPat, // negated, matches relative URL
                            true, "^(" + relChar + ")", getAttrsToRewrite()) {
        /** Specify the "replace" property using the baseUrl param */
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

    // Rewrite CSS style segments
    StyleTagXform styleTagXform =
      new StyleTagXform(au, encoding, url, srvLink);
    relXforms.add(styleTagXform);

    // Rewrite CSS style attributes
    StyleAttrXform styleAttrXform =
      new StyleAttrXform(au, encoding, url, srvLink);
    relXforms.add(styleAttrXform);

    // Rewrite <script>s
    ScriptXform scriptXform = new ScriptXform(au, encoding, url, srvLink);
    relXforms.add(scriptXform);

    // Rewrite <meta http-equiv="refresh" content="url=1; url=...">
    // Rewrite absolute links to urlStem/... to targetStem + urlStem/...

    List<RefreshRegexXform> absRefreshXforms = new ArrayList<>();
    for (String urlStem : urlStems) {
      absRefreshXforms.add(new RefreshRegexXform(refreshPat + urlStem, true,
						 "url=" + urlStem,
						 "url=" + targetStem + urlStem));
    }
    logger.debug3("Abs refresh Xforms: " + absRefreshXforms);

    // Rewrite URLs in <meta name="name" content="url"> for all names
    // specified by plugin
    List<String> metaNames = getMetaNamesToRewrite(au);
    List<MetaTagRegexXform> metaTagXforms = new ArrayList<>();
    NodeFilter metaTagXform = null;
    if (metaNames != null && !metaNames.isEmpty()) {
      // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
      String metaAbsPrefix =
	CurrentConfig.getParam(PARAM_META_TAG_REWRITE_PREFIX,
			       DEFAULT_META_TAG_REWRITE_PREFIX);
      for (String urlStem : urlStems) {
	metaTagXforms.add(new MetaTagRegexXform("^" + urlStem, true,
						urlStem,
						(metaAbsPrefix != null
						 ? metaAbsPrefix + targetStem + urlStem
						 : targetStem + urlStem),
						metaNames));

      }
    }
    logger.debug3("Meta xforms: " + metaTagXforms);

    @SuppressWarnings("serial")
    RelRefreshRegexXform[] relRefreshXforms = {
      // transforms site relative HTML refresh URLs
      new RelRefreshRegexXform(relRefreshPat, 
                               true, "(" + refreshPat + ")/") { 
        /** Specify the "replace" property using the baseUrl param */
        public void setBaseUrl(String baseUrl)
            throws MalformedURLException {
          setReplace("$1" + srvLink.rewrite(UrlUtil.getUrlPrefix(baseUrl)));
        }},
        // transforms path-relative HTML refresh URLs
        new RelRefreshRegexXform(relRefreshPat, 
                                 true, "("+refreshPat+")(" + relChar + ")") {
        /** Specify the "replace" property using the baseUrl param */
        public void setBaseUrl(String baseUrl)
            throws MalformedURLException {
          setReplace("$1"+srvLink.rewrite(UrlUtil.resolveUri(baseUrl, "$2")));
        }},
    };
    for (RelRefreshRegexXform xform : relRefreshXforms) {
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
    List<NodeFilter> filters = new ArrayList<NodeFilter>();
    filters.addAll(preXforms);
    filters.add(base);
    filters.add(new OrFilter(relLinkXforms));
    filters.addAll(absLinkXforms);
    filters.add(styleTagXform);
    filters.add(styleAttrXform);
    filters.add(scriptXform);
    filters.add(relRefreshXform);
    filters.addAll(absRefreshXforms);
    filters.addAll(metaTagXforms);

    filters.addAll(postXforms);

    NodeFilter linkXform = new OrFilter(filters.toArray(new NodeFilter[0]));
    // Create a transform to apply them
    HtmlTransform htmlXform = HtmlNodeFilterTransform.exclude(linkXform);

    // If the input stream knows the charset used to encode it, use it.
    if (in instanceof EncodedThing) {
      String inCharset = ((EncodedThing)in).getCharset();
      if (!StringUtil.isNullString(inCharset)) {
	logger.debug2("InputStream encoded with " + inCharset +
		      ", overriding " + encoding);
	encoding = inCharset;
      }
    }

    InputStream result = new HtmlFilterInputStream(in,
                                                   encoding,
                                                   encoding,
                                                   htmlXform);
    return result;
  }

  
 List<String> getAttrList() {
    if (attrList == null) {
      attrList = new ArrayList(Arrays.asList(DEFAULT_ATTRS));
    }
    return attrList;
  }

  protected String[] getAttrsToRewrite() {
    if (attrs == null) {
      attrs = attrList.toArray(new String[0]);
    }
    return attrs;
  }

  private void checkExecuted() {
    if (executed) {
      throw new IllegalStateException("Can't alter xforms or attributes once rewriting has begun");
    }
  }

  /** Add to the default list of tag attributes whose values are rewritten
   * using the standard rules. */
  public void addAttrToRewrite(String attr) {
    checkExecuted();
    if (!getAttrList().contains(attr)) {
      attrs = null;
      getAttrList().add(attr);
    }
  }

  /** Remove one of the default tag attributes whose values are rewritten
   * using the standard rules. */
  public boolean removeAttrToRewrite(String attr) {
    checkExecuted();
    attrs = null;
    return getAttrList().remove(attr);
  }

  /** Replace the list of tag attributes whose values are rewritten using
   * the standard rules. */
  public void setAttrsToRewrite(List<String> newAttrs) {
    checkExecuted();
    attrs = null;
    attrList = newAttrs;
  }

  /** Add a NodeFilter to be applied to each node before the standard set */
  public void addPreXform(NodeFilter xform) {
    checkExecuted();
    preXforms.add(xform);
  }

  /** Add a NodeFilter to be applied to each node after the standard set */
  public void addPostXform(NodeFilter xform) {
    checkExecuted();
    postXforms.add(xform);
  }


  // Overridden by unit test
  protected List<String> getMetaNamesToRewrite(ArchivalUnit au) {
    return
      AuUtil.getPluginList(au,
			   DefinablePlugin.KEY_PLUGIN_REWRITE_HTML_META_URLS);
  }    

  public static class HtmlBaseProcessor extends TagNameFilter {
    private String origBaseUrl;
    private List<RelXform> xforms;
    private boolean hasBaseBeenSet = false;

    public HtmlBaseProcessor(String baseUrl) {
      super("BASE");
      origBaseUrl = baseUrl;
    }

    public void setXforms(List<RelXform> xforms) {
      this.xforms = xforms;
    }

    public boolean accept(Node node) {
      if (!super.accept(node)) {
        return false;
      }
      if (hasBaseBeenSet) {
	logger.siteWarning("Ignoring 2nd (or later) base tag: " + node);
	return false;
      }
      Attribute attr = ((TagNode)node).getAttributeEx("href");
      if (attr != null) {
        String baseHref = attr.getValue();
	try {
	  String newBase = UrlUtil.resolveUri(origBaseUrl, baseHref);
	  for (RelXform xform : xforms) {
	    try {
	      xform.setBaseUrl(newBase);
	    } catch (MalformedURLException e) {
	      logger.warning("Not resetting rewriter's base URL", e);
	    }
	  }
	} catch (MalformedURLException e) {
	  logger.warning("Not resetting rewriter's base URL", e);
	}

      }
      hasBaseBeenSet = true;
      return false;
    }
  }

  interface RelXform extends NodeFilter {
    void setBaseUrl(String baseUrl) throws MalformedURLException;
  }

  @SuppressWarnings("serial")
  abstract class RelLinkRegexXform extends HtmlNodeFilters.LinkRegexXform
    implements RelXform {

    RelLinkRegexXform(String regex, boolean ignoreCase,
                      String target, String[] attrs) {
      super(regex, ignoreCase, target, null, attrs);
    }
  }

  @SuppressWarnings("serial")
  @Deprecated
  abstract class RelStyleRegexXform extends HtmlNodeFilters.StyleRegexXform
    implements RelXform {

    public RelStyleRegexXform(String regex, boolean ignoreCase,
                              String target, String replace) {
      super(regex, ignoreCase, target, null);
    }

    abstract public void setBaseUrl(String baseUrl)
      throws MalformedURLException;
  }

  @SuppressWarnings("serial")
  class StyleTagXform extends HtmlNodeFilters.StyleTagXformDispatch
    implements RelXform {

    public StyleTagXform(ArchivalUnit au,
			 String charset,
			 String baseUrl,
			 ServletUtil.LinkTransform xform) {
      super(au, charset, baseUrl, xform);
    }

    public void setBaseUrl(String baseUrl) {
      super.setBaseUrl(baseUrl);
    }
  }

  @SuppressWarnings("serial")
  class StyleAttrXform extends HtmlNodeFilters.StyleAttrXformDispatch
    implements RelXform {

    public StyleAttrXform(ArchivalUnit au,
			  String charset,
			  String baseUrl,
			  ServletUtil.LinkTransform xform) {
      super(au, charset, baseUrl, xform);
    }

    public void setBaseUrl(String baseUrl) {
      super.setBaseUrl(baseUrl);
    }
  }

  @SuppressWarnings("serial")
  class ScriptXform extends HtmlNodeFilters.ScriptXformDispatch
    implements RelXform {

    public ScriptXform(ArchivalUnit au,
                       String charset,
                       String baseUrl,
                       ServletUtil.LinkTransform xform) {
      super(au, charset, baseUrl, xform);
    }

    public void setBaseUrl(String baseUrl) {
      super.setBaseUrl(baseUrl);
    }
  }

  @SuppressWarnings("serial")
  abstract class RelRefreshRegexXform extends HtmlNodeFilters.RefreshRegexXform
    implements RelXform {

    public RelRefreshRegexXform(String regex, boolean ignoreCase,
                                String target) {
      super(regex, ignoreCase, target, null);
    }

    abstract public void setBaseUrl(String baseUrl)
      throws MalformedURLException;
  }

}
