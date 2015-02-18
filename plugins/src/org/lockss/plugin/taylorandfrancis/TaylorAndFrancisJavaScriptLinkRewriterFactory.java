/*
 * $Id$
 */

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ParagraphTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

/**
 * This custom link rewriter performs Taylor and Francis specific 
 * rewriting for HTML that includes javascript links of the form:
 * <pre>
 * &lt;p onclick="window.open(url,...');"&gt;
 * </pre>
 * 
 * Only the internal URL is rewritten
 * 
 * @author Philip Gust
 */
public class TaylorAndFrancisJavaScriptLinkRewriterFactory 
  implements LinkRewriterFactory {

  static final Logger logger = Logger
      .getLogger(TaylorAndFrancisJavaScriptLinkRewriterFactory.class);

  /** Pattern for "window.open('...')" to get first parameter and punctuation */ 
  // example: window.open('ef1.htm', ...)
  static final java.util.regex.Pattern openWindowPat = java.util.regex.Pattern
      .compile(".*(window\\.open\\([\"'])([^\"',]+)([\"'].*\\))");
  
  /** pattern for the javascript:window.close() function */
  static final java.util.regex.Pattern closeWindowPat = java.util.regex.Pattern
      .compile(".*(javascript:window\\.close\\(\\))");

  /**
   * Transforms a javascript: link that was previously rewritten 
   * incorrectly for use with ServeContent.
   * 
   * @author Philip Gust
   */
  static class JavaScriptLinkTransform implements LinkTransform {
    // Matches the prefix of an absolute URL
    static final java.util.regex.Pattern absUrlPrefixPat = 
      java.util.regex.Pattern.compile("^[^:/?#]+://+.*$");

    /** The encoding to decode and re-encode rewritten text. */
    final String encoding;

    /** The JS pattern with a URL to rewrite */
    final Pattern[] jsUrlRewritePats;

    /**
     * Create a new instance for the encoding and rewrite patterns.
     * 
     * @param encoding the encoding
     * @param jsUrlRewritePats the patterns of javascript URLs to rewrite
     */
    JavaScriptLinkTransform(String encoding, Pattern[] jsUrlRewritePats) {
      this.encoding = encoding;
      this.jsUrlRewritePats = jsUrlRewritePats;
    }
    
    /**
     * Rewrite a previously rewritten URL that involves a javascript expression.
     * The rewriting was done incorrectly, and needs to be rewritten again to
     * correct the problem.
     * 
     * @param url the incorrectly rewritten URL
     * @return the correctly written URL
     */
    public String rewrite(String url) {
      int i = url.indexOf("/ServeContent?url=");
      if (i >= 0) {
        // split the href after the "url="
        String rewritePrefix = url.substring(0, i + 18);
        String rewriteTarget = url.substring(rewritePrefix.length());
        try {
          rewriteTarget = URLDecoder.decode(rewriteTarget, encoding);
          
          if (jsUrlRewritePats != null) {
            // parse the javascript expression to get the function URL
            for (Pattern jsUrlRewritePat : jsUrlRewritePats) {
              Matcher matcher = jsUrlRewritePat.matcher(rewriteTarget);
              if (matcher.matches()) {
                // function with no arguments -- the first one
                if (matcher.groupCount() == 1) {
                  return matcher.group(1);
                } 
                
                // funciton of one or more arguments -- replace first one
                if (matcher.groupCount() == 3) {
                  String prefix = matcher.group(1);
                  String newUrl = matcher.group(2);
                  String suffix = matcher.group(3);
                  
                  // start index of pattern matched
                  i = matcher.start(1);
                  
                  // the javascript expression
                  // the base URL that the javascript expression is relative to
                  String baseUrl = rewriteTarget.substring(0, i);
                  // extract the host URL from the base URL
                  i = baseUrl.indexOf("/", baseUrl.indexOf("//") + 2);
                  String hostUrl = baseUrl.substring(0, i + 1);
  
                  // if url is relative, make it absolute using
                  // the baseUrl that preceeds "javascript:"
                  if (!absUrlPrefixPat.matcher(newUrl).matches()) {
                    if (newUrl.startsWith("/")) {
                      newUrl = hostUrl + newUrl.substring(1);
                    } else {
                      newUrl = baseUrl + newUrl;
                    }
                  }
  
                  StringBuilder newjsexpr = new StringBuilder(prefix);
                  // only transform the URL if it is from the same host
                  if (newUrl.startsWith(hostUrl)
                      && !newUrl.startsWith(rewritePrefix)) {
                    newjsexpr.append(rewritePrefix);
                    newjsexpr.append(URLEncoder.encode(newUrl, encoding));
                  } else {
                    newjsexpr.append(newUrl);
                  }
                  newjsexpr.append(suffix);
                  return newjsexpr.toString();
                }
              }
            }
          }

        } catch (UnsupportedEncodingException ex) {
          logger.siteError("bad encoding during link rewriting", ex);
        }
      }

      return url;
    }
  }
  
  /**
   * This node filter rewrites links on pagagraph tag implemented
   * by "onclick" attribute, where the target matches the patterns.
   * 
   * @author Philip Gust
   * 
   */
  @SuppressWarnings("serial")
  static class JavaScriptLinkRewriterNodeFilter implements NodeFilter {
    final LinkTransform linkXform;
    
    /**
     * Create an instance with the specified link transform.
     * 
     * @param linkXform  the link transform to apply
     */
    JavaScriptLinkRewriterNodeFilter(LinkTransform linkXform) {
      this.linkXform = linkXform;
    }

    /**
     * Visitor for each node; only process nodes for tags
     * that have links to rewrite.
     * 
     * @param node the visited node
     */
    @Override
    public boolean accept(Node node) {
      if (node instanceof ParagraphTag) {
        ParagraphTag tag = (ParagraphTag)node;
        Attribute attr = tag.getAttributeEx("onclick");
        if (attr != null) {
          String url = attr.getValue();
          String newUrl = linkXform.rewrite(url);
          
          if (!url.equals(newUrl)) {
            attr.setValue(newUrl);
            tag.setAttributeEx(attr);
          }
        }
      } else if (node instanceof LinkTag) {
        LinkTag tag = (LinkTag)node;
        Attribute attr = tag.getAttributeEx("href");
        if (attr != null) {
          String url = attr.getValue();
          String newUrl = linkXform.rewrite(url);
          
          if (!url.equals(newUrl)) {
            attr.setValue(newUrl);
            tag.setAttributeEx(attr);
          }
        }
      }

      return false;
    }
  }

  /**
   * Create a link rewrite that post-processes the input stream returned by
   * NodeFilterHtmlLinkRewriterFactor to repair the (incorrect) link rewriting
   * that it does for javascript: URL expressions. This is a temporary measure
   * until the base HTML link rewriter understands how to handle javascript:
   * expressions by delegating to a JavaScript link rewriter.
   */
  @Override
  public InputStream createLinkRewriter(String mimeType, ArchivalUnit au,
      InputStream in, String encoding, String url, LinkTransform xform)
      throws PluginException, IOException {
    
    LinkTransform linkXform = 
        new JavaScriptLinkTransform(encoding, 
            new Pattern[] { openWindowPat, closeWindowPat });

    JavaScriptLinkRewriterNodeFilter nodeFilter = 
      new JavaScriptLinkRewriterNodeFilter(linkXform);
    
    HtmlNodeFilterTransform htmlXform = 
        HtmlNodeFilterTransform.exclude(nodeFilter);

    InputStream rewriterIn = 
      new org.lockss.plugin.taylorandfrancis.NodeFilterHtmlLinkRewriterFactory()
        .createLinkRewriter(mimeType, au, in, encoding, url, xform);

    InputStream result = 
        new HtmlFilterInputStream(rewriterIn, encoding, encoding, htmlXform);
    return result;
  }
  
}
