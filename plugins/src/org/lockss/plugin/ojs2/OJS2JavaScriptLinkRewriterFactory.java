/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.ojs2;

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
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.MetaTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

/**
 * This custom link rewriter performs OJS joural specific rewriting for HTML
 * that includes javascript links of the form:
 * 
 * <pre>
 * &lt;a href="javascript:openRTWindow('http://www.xyz.com/...');"&gt;
 * </pre>
 * 
 * Only the internal URL is rewritten
 * 
 * @author Philip Gust
 */
public class OJS2JavaScriptLinkRewriterFactory implements LinkRewriterFactory {

  static final Logger logger = Logger
      .getLogger(OJS2JavaScriptLinkRewriterFactory.class);

  // Pattern for js call to a one-argument function isolates the
  // function argument and the prefix and postfix text as capture groups. 
  // A js identifier is a letter, '$' or '_' followed by any number of 
  // letters, numbers, '$' and '_' characters.
  // static final java.util.regex.Pattern jsfnlinkpat =
  // java.util.regex.Pattern.compile(
  // "([$_\\p{L}][$_\\p{L}\\p{Mn}\\p{Mc}\\p{Nd}\\p{Pc}\\u200C\\u200D]*+\\([\"'])([^\"',]+)([\"']\\);)");

  /** Pattern for js "openRTWindow('...')" */
  static final java.util.regex.Pattern openRTWindowPat = java.util.regex.Pattern
      .compile("(openRTWindow\\([\"'])([^\"',]+)([\"']\\);)");
  /** Pattern for js "newWindow('...')" */
  static final java.util.regex.Pattern newWindowPat = java.util.regex.Pattern
      .compile("(newWindow\\([\"'])([^\"',]+)([\"']\\);)");
  /** Pattern for js "popup('...')" */
  static final java.util.regex.Pattern popupPat = java.util.regex.Pattern
      .compile("(popup\\([\"'])([^\"',]+)([\"']\\);)");

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
     * @param url
     *          the incorrectly rewritten URL
     * @return the correctly written URL
     */
    public String rewrite(String url) {
      int i = url.indexOf("/ServeContent?url=");
      if (i >= 0) {
        // split the href after the "url="
        String rewritePrefix = url.substring(0, i + 18);
        String rewriteTarget = url.substring(rewritePrefix.length());
        try {
          // suffix is the baseURL followed by the "javascript:" expression
          rewriteTarget = URLDecoder.decode(rewriteTarget, encoding);
          i = rewriteTarget.indexOf("javascript:");
          if (i >= 0) {
            String jsexpr = rewriteTarget.substring(i + 11);
            if (jsUrlRewritePats != null) {
              // the "javascript:" expression
              // the base URL that the javascript expression is relative to
              String baseUrl = rewriteTarget.substring(0, i);
              // extract the host URL from the base URL
              i = baseUrl.indexOf("/", baseUrl.indexOf("//") + 2);
              String hostUrl = baseUrl.substring(0, i + 1);

              // parse the "javascript:" expression to get the function URL
              for (Pattern jsUrlRewritePat : jsUrlRewritePats) {
                Matcher matcher = jsUrlRewritePat.matcher(jsexpr);
                if (matcher.matches()) {
                  // get the URL from the javascript function
                  String newUrl = matcher.group(2);

                  // if url is relative, make it absolute using
                  // the baseUrl that preceeds "javascript:"
                  if (!absUrlPrefixPat.matcher(newUrl).matches()) {
                    if (newUrl.startsWith("/")) {
                      newUrl = hostUrl + newUrl.substring(1);
                    } else {
                      newUrl = baseUrl + newUrl;
                    }
                  }

                  StringBuilder newjsexpr = new StringBuilder("javascript:");
                  newjsexpr.append(matcher.group(1));
                  // only transform the URL if it is from the same host
                  if (newUrl.startsWith(hostUrl)
                      && !newUrl.startsWith(rewritePrefix)) {
                    newjsexpr.append(rewritePrefix);
                    newjsexpr.append(URLEncoder.encode(newUrl, encoding));
                  } else {
                    newjsexpr.append(newUrl);
                  }
                  newjsexpr.append(matcher.group(3));
                  return newjsexpr.toString();
                }
              }
            }
            // remove javascript expression without "/ServeContent?url=" prefix
            return "javascript:" + jsexpr;
          }
        } catch (UnsupportedEncodingException ex) {
          logger.siteError("bad encoding during link rewriting", ex);
        }
      }

      return url;
    }
  }
  
  /**
   * This node filter rewrites link and refresh nodes whose URL is a javascript
   * expression or function that involves another URL has been rewritten by
   * NodeFilterHtmlLinkRewriterFactory.
   * <p>
   * For example, the original URL <br>
   * javascript:openRTWindow('http://www.xyz.com/path/leaf');
   * <p>
   * is rewritten by NodeFilterHtmlLinkRewriterFactory as <br>
   * http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Ffoo%2F
   * javascript
   * %3AopenRTWindow%28%27http%3A%2F%2Fwww.xyz.com%2Fpath%2Fleaf%27%29%3B
   * <p>
   * This function transforms this link as: <br>
   * javascript:openRTWindow(
   * 'http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fpath%2Fleaf')
   * ;
   * <p>
   * Which javasript expressions have their URLs rewritten is specified by an
   * array of patterns. Each pattern has three capture groups: a prefix, the URL
   * to be rewritten, and the suffix. for example, the following pattern
   * specifes a call to the javascript "openRTWindow" function with a single URL
   * argument: <br>
   * (openRTWindow\\([\"'])([^\"',]+)([\"']\\);)
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
      if (node instanceof LinkTag) {
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
      } else if (node instanceof FormTag) {
        FormTag tag = (FormTag)node;
        Attribute attr = tag.getAttributeEx("action");
        if (attr != null) {
          String url = attr.getValue();
          String newUrl = linkXform.rewrite(url);

          if (!url.equals(newUrl)) {
            attr.setValue(newUrl);
            tag.setAttributeEx(attr);
          }
        }
      } else if (node instanceof MetaTag) {
        MetaTag tag = (MetaTag)node;
        if ("refresh".equalsIgnoreCase(tag.getAttribute("http-equiv"))) {
          Attribute attr = tag.getAttributeEx("content");
          if (attr != null) {
            String value = attr.getValue();
            int i = value.indexOf(";url=");
            if (i >= 0) {
              String tagPrefix = value.substring(0, i + 5);
              String url = value.substring(tagPrefix.length());
              String newUrl = linkXform.rewrite(url);

              if (!url.equals(newUrl)) {
                attr.setValue(tagPrefix + newUrl);
                tag.setAttributeEx(attr);
              }
            }
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
      new JavaScriptLinkTransform(encoding, new Pattern[] { openRTWindowPat });

    JavaScriptLinkRewriterNodeFilter nodeFilter = 
      new JavaScriptLinkRewriterNodeFilter(linkXform);
    
    HtmlNodeFilterTransform htmlXform = 
        HtmlNodeFilterTransform.exclude(nodeFilter);

    InputStream rewriterIn = new NodeFilterHtmlLinkRewriterFactory()
        .createLinkRewriter(mimeType, au, in, encoding, url, xform);

    InputStream result = 
        new HtmlFilterInputStream(rewriterIn, encoding, encoding, htmlXform);
    return result;
  }
  
}
