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

package org.lockss.plugin.taylorandfrancis;

import java.io.*;
import java.net.*;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.filter.html.*;
import org.lockss.rewriter.LinkRewriterFactory;
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

  public static String PARAM_META_TAG_REWRITE_PREFIX =
    Configuration.PREFIX + "htmlRewriter.metaTagRewritePrefix";
  public static String DEFAULT_META_TAG_REWRITE_PREFIX = null;

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

  // Legal start to non-server relative path in relative URL  
  static final String relChar = "[-a-zA-Z0-9$_@.&+!*\"\'(),%?#]";
  
  // Matches protocol pattern (e.g. "http://")
  static final String protocolPat = "[^:/?#]+://+";
  
  // Matches protocol prefix of a URL (e.g. "http://")
  static final String protocolPrefixPat = "^" + protocolPat;
  
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
    final String targetStem = srvLink.rewrite("");  // XXX - should have better xform
    logger.debug2("targetStem: " + targetStem);
    Collection<String> urlStems = au.getUrlStems();
    int nUrlStem = urlStems.size();
    String defUrlStem = null;
    int l = nUrlStem;
    String[] linkRegex1 = new String[l];
    boolean[] ignCase1 = new boolean[l];
    String[] rwRegex1 = new String[l];
    String[] rwTarget1 = new String[l];
    // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
    int i = 0;
    for (String urlStem : urlStems) {
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

    HtmlBaseProcessor base = new HtmlBaseProcessor();
    List<RelXform> relXforms = new ArrayList<RelXform>();

    // Rewrite relative links
    @SuppressWarnings("serial")
    RelLinkRegexXform relLinkXforms[] = {
      // transforms site-relative link URLs
      new RelLinkRegexXform(protocolPrefixPat, // negated, matches relative URL
                            true, "^/", attrs) {
        /** Specify the "replace" property using the baseUrl param */
        public void setBaseUrl(String baseUrl)
            throws MalformedURLException {
          setReplace(srvLink.rewrite(UrlUtil.getUrlPrefix(baseUrl)));
        }},
      // transforms path-relative link URLs
      new RelLinkRegexXform(protocolPrefixPat, // negated, matches relative URL
                            true, "^(" + relChar + ")", attrs) {
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

    // Rewrite CSS style imports
    StyleXform styleXform = new StyleXform(au, encoding, url, srvLink);
    relXforms.add(styleXform);

    // Rewrite <script>s
    ScriptXform scriptXform = new ScriptXform(au, encoding, url, srvLink);
    relXforms.add(scriptXform);

    // Rewrite <meta http-equiv="refresh" content="url=1; url=...">
    String[] linkRegex5 = new String[l];
    boolean[] ignCase5 = new boolean[l];
    String[] rwRegex5 = new String[l];
    String[] rwTarget5 = new String[l];
    // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
    i = 0;
    for (String urlStem : urlStems) {
      linkRegex5[i] = refreshPat + urlStem;
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

    // Rewrite URLs in <meta name="name" content="url"> for all names
    // specified by plugin
    List<String> metaNames = getMetaNamesToRewrite(au);
    NodeFilter metaTagXform = null;
    if (metaNames != null && !metaNames.isEmpty()) {
      String[] linkRegexMeta = new String[l];
      boolean[] ignCaseMeta = new boolean[l];
      String[] rwRegexMeta = new String[l];
      String[] rwTargetMeta = new String[l];
      // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
      String metaAbsPrefix =
	CurrentConfig.getParam(PARAM_META_TAG_REWRITE_PREFIX,
			       DEFAULT_META_TAG_REWRITE_PREFIX);
      i = 0;
      for (String urlStem : urlStems) {
	linkRegexMeta[i] = "^" + urlStem;
	ignCaseMeta[i] = true;
	rwRegexMeta[i] = urlStem;
	if (metaAbsPrefix != null) {
	  rwTargetMeta[i] = metaAbsPrefix + targetStem + urlStem;
	} else {
	  rwTargetMeta[i] = targetStem + urlStem;
	}
	logger.debug3("if meta match " + linkRegexMeta[i] + " replace " +
		      rwRegexMeta[i] + " by " + rwTargetMeta[i]);
	i++;
      }
      metaTagXform =
	HtmlNodeFilters.metaTagRegexYesXforms(linkRegexMeta, ignCaseMeta,
					      rwRegexMeta, rwTargetMeta,
					      metaNames);
    }

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
    List<NodeFilter> filters =
      ListUtil.list(base,
		    relLinkXform,
		    absLinkXform,
		    styleXform,
		    scriptXform,
		    relRefreshXform,
		    absRefreshXform);
    if (metaTagXform != null) {
      filters.add(metaTagXform);
    };

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

  // Overridden by unit test
  protected List<String> getMetaNamesToRewrite(ArchivalUnit au) {
    return
      AuUtil.getPluginList(au,
			   DefinablePlugin.KEY_PLUGIN_REWRITE_HTML_META_URLS);
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

  @SuppressWarnings("serial")
  abstract class RelLinkRegexXform extends HtmlNodeFilters.LinkRegexXform
    implements RelXform {

    RelLinkRegexXform(String regex, boolean ignoreCase,
                      String target, String[] attrs) {
      super(regex, ignoreCase, target, null, attrs);
    }
  }

  @SuppressWarnings("serial")
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
  class StyleXform extends HtmlNodeFilters.StyleXformDispatch
    implements RelXform {

    public StyleXform(ArchivalUnit au,
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
