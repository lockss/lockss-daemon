/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.ingenta;

import java.io.InputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlTransform;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;

/**
 * This custom link rewriter performs Ingenta joural specific rewriting to
 * make the PDF and HTML article files available through the button links 
 * on journal article pages. Ingenta has made the files available to LOCKSS
 * on another server, so the links must be rewritten to redirect to the
 * correct location on that other server.
 * 
 * @author Philip Gust
 */
public class IngentaHtmlLinkRewriterFactory 
  extends NodeFilterHtmlLinkRewriterFactory {
  
  static final Logger logger = Logger.getLogger(IngentaHtmlLinkRewriterFactory.class);

  /** Pattern that matches the article PDF link */
  static Pattern infobikePat = null;
  static {
    try {
      infobikePat = Pattern.compile(
          "^([a-zA-Z0-9]+://?[^/]+)?"  // optional protocol
        + "/search/download\\?pub=infobike%3a%2[fF]%2[fF]" // search prefix
        + "([^%]+)%2[fF]" // capture journal code
        + "([^%]+)%2[fF]" // capture title code
        + "([^%]+)%2[fF]" // capture year
        + "([^%]+)%2[fF]" // capture volume
        + "([^%]+)%2[fF]" // capture issue
        + "([^&]+)&" // capture article
        + "mimetype=([^%]+)%2[fF]([^&]+)" // capture MIME type
        + ".*");
    } catch (PatternSyntaxException ex) {
      // shouldn't happen!
      logger.error("Bad rewriter pattern", ex);
    }
  }
  
  /**
   * This link rewriter does special processing for article PDF and HTML 
   * links before handing off to the base NodeFilterHtmlLinkRewriterFactory.
   * 
   * @param mimeType the MIME type for the URL
   * @param au  the AU containing the URL
   * @param in the input stream for the URL
   * @param encoding the encoding for the input stream
   * @param url the URL for the input stream
   * @param xfm the link transform for the default rewriter
   */
  @Override
  public InputStream createLinkRewriter(
      String mimeType, ArchivalUnit au, InputStream in,
      String encoding, String url,
      ServletUtil.LinkTransform xfm)
      throws PluginException, IOException {
    InputStream rwIn = 
        createIngentaHtmlLinkRewriter(au, in, encoding);
    return super.createLinkRewriter(mimeType, au, rwIn, encoding, url, xfm);
  }
  
  /**
   * This link rewriter does only Ingenta-specific rewriting for 
   * artlcle PDF and HTML links.
   * 
   * @param au  the AU containing the URL
   * @param in the input stream for the URL
   * @param encoding the encoding for the input stream
   */
  InputStream createIngentaHtmlLinkRewriter(
      final ArchivalUnit au, InputStream in, String encoding)
  throws PluginException {
    // no rewriting if infobikePat was reported bad
    if (infobikePat == null) {
      return in;
    }

    // this filter rewrites PDF and HTML file link hrefs
    @SuppressWarnings("serial")
    NodeFilter filter = new NodeFilter() {
      @Override
      public boolean accept(Node node) {
        try {
          if (node instanceof LinkTag) {
            Attribute attribute = ((TagNode)node).getAttributeEx("href");
            if (attribute != null) {
              String url = attribute.getValue();
              String newUrl = rewriteUrl(url, au);
              if (!newUrl.equals(url)) {
                attribute.setValue(newUrl);
                ((TagNode)node).setAttributeEx(attribute);
                if (logger.isDebug3()) {
                  logger.debug3("rewrote: " + url + " to " + newUrl);
                }
              }
            }
          }
        } catch (Throwable ex) {
          logger.error(node.toHtml(), ex);
        }
        return false;
      }
    };

    // compose streams to normalize URLs before rewriting
    HtmlTransform htmlXfm = HtmlNodeFilterTransform.exclude(filter);
    return new HtmlFilterInputStream(in, encoding, encoding, htmlXfm);
  }
  
  /**
   * Rewrite URLs that link to the article PDF and HTML files.
   * This is similar to the "normalization" that (should) take place
   * while crawling the article landing page. It rewrites the original
   * link to a location on the KEY_BASE_URL server that is not collected,
   * to a location on the KEY_API_URL server where the content is collected.
   * <p>
   * The algorithm tries to find the file on the KEY_AIP_URL server that 
   * corresponds to the MIME type specified in the original link. However, 
   * some content may have already been collected with a URL in the metadata 
   * that does not include the MIME type, so the algorithm first tries 
   * rewriting it with the MIME type, then without it and uses the one whose
   * content corresponds to the MIME type on the original link.
   * <p>
   * For example, the URL:
   * <br>
   * http://www.ingentaconnect.com/search/download?pub=infobike%3a%2f%2fmanup%2fvcb%2f2006%2f00000007%2f00000002%2fart00001&mimetype=application%2fpdf&exitTargetId=1356711030492
   * <br>
   * would normally be rewritten as:
   * <br>
   * http://api.ingentaconnect.com/content/manup/vcb/2006/00000007/00000002/art00001?crawler=true&mimetype=application/pdf
   * <br>
   * but is actually rewritten as:
   * <br>
   * http://api.ingentaconnect.com/content/manup/vcb/2006/00000007/00000002/art00001?crawler=true
   * <br>
   * because that AU was already collected with a URL that does not include the 
   * MIME type.
   * 
   * @param url the original URL to rewrite
   * @param au the Archival Unit for the URL
   * @return the normalized URL
   */
  String rewriteUrl(String url, ArchivalUnit au) {

    // ignore URLs that don't match the infobike pattern
    Matcher matcher = infobikePat.matcher(url);
    if (!matcher.matches()) {
      return url;
    }
    
    // rewrite matching URL to one on the "api_url" server
    // where the file is actually stored
    String apiUrl = au.getConfiguration().get("api_url");
    StringBuilder sb = new StringBuilder(apiUrl);
    sb.append("content");
    for (int i = 2; i <= 7; i++) {
      sb.append('/');
      sb.append(matcher.group(i));
    }
    sb.append("?crawler=true");
    // create version of rewritten URL without MIME type as fall-back
    // in case the URL we ingested didn't have the MIME type
    String newUrlNoMime = sb.toString();
    
    // create version of rewritten URL with original MIME type
    String mimeType = matcher.group(8)+ "/"+ matcher.group(9);
    String newUrl = newUrlNoMime + "&mimetype=" + mimeType;
    
    // see if a matching URL with the original MIME type param
    // exists matches original MIME type, e.g.:
    // http://api.ingentaconnect.com/...?crawler=true&mimetype=application/pdf
    CachedUrl cu = au.makeCachedUrl(newUrl);
    try {
      if ((cu == null) 
          || !cu.hasContent()
          || !mimeType.equalsIgnoreCase(
            HeaderUtil.getMimeTypeFromContentType(cu.getContentType()))) {
        // see if matching URL without the original MIME type param 
        // exists and matches original MIME type, e.g.
        // http://api.ingentaconnect.com/...?crawler=true 
        AuUtil.safeRelease(cu);
        cu = au.makeCachedUrl(newUrlNoMime);
        if ((cu == null) 
            || !cu.hasContent()
            || !mimeType.equalsIgnoreCase(
              HeaderUtil.getMimeTypeFromContentType(cu.getContentType()))) {
          return url; 
        }
        newUrl = newUrlNoMime;
      }
    } finally {
      AuUtil.safeRelease(cu);
    }
    
    return newUrl;
  }
}
