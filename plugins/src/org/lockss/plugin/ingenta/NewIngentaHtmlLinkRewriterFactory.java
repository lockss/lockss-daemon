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
package org.lockss.plugin.ingenta;

import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;

/**
 * This custom link rewriter performs Ingenta  specific rewriting 
 * for both books and journals to make the PDF and HTML article files available 
 * through the page button actions on journal article  or chapter pages. 
 * Here are the various rewrites:
 * - For journals, Ingenta has made the files available to LOCKSS
 * on another server, so the links must be rewritten to redirect to the
 * correct location on that other server. This does not need to happen for books
 * because the url normalizer is sufficient.
 * - The default link rewriter does not catch the link attr "data-popup"
 * - Finally, for javascript:popup methods, ServeContent incorrectly rewrote the link and this 
 * class catches and fixes that after the default rewrite
 * 
 */
public class NewIngentaHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  private static final Logger log =
    Logger.getLogger(NewIngentaHtmlLinkRewriterFactory.class);
  
  /** Pattern that matches the article full-text link set on an appropriate attr*/
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
      log.error("Bad rewriter pattern", ex);
    }
  }
  
  //javascript:popup('/sea...',...)
  // group1 is whichever quote is being used just inside the open-paren
  // group2 will be everything between the first quotemark and the matching quotemark
  // group3 will be everything after the first argument of the javascript:popup call
  // which is the actual link
   static final Pattern onClickPdfPattern =
    Pattern.compile("^javascript:popup\\(([\"'])([^\"',]*)(\\1.*)", 
		    Pattern.CASE_INSENSITIVE);
   
     
  /**
   * This link rewriter does special processing for article PDF and HTML
   * links before the standard ServeContent rewriting in
   * NodeFilterHtmlLinkRewriterFactory, then cleans up mis-rewritten
   * javascript links.
   * 
   * Normally you could just rely on a url-normalizer to rewrite the link after
   * it was served but in the case of legacy Ingenta journals, the normalization
   * wasn't consistent.  So this rewrites the url for full-text PDF and html
   * to use a crawler-stable version and tries to see which variant of this is in the cache
   * and then rewrites the link to that one.
   * 
   * Note that this link rewriter follows three steps
   *  1) it creates a filter to be applied first does the rewriting as per the comment above by using the first filter
   *  2) it calls NodeFilterHtmlLinkRewriterFactory the base class filter to add in the http://foobar.org/ServeContent?url= part
   *  3) it calls another link rewriter to fix the fact that the base class will 
   *      incorrectly turn <a onclick="javascript:method('link')" to 
   *                       <a onclick="http://foobar.org/ServeContent?url=<base>/javascript:method('link')"
   *      so that instead it becomes
   *                       <a onclick="javascript:method('http://foobar.org/ServeContent?url=link')"
   * 
   * @param mimeType the MIME type for the URL
   * @param au  the AU containing the URL
   * @param in the input stream for the URL
   * @param encoding the encoding for the input stream
   * @param url the URL for the input stream
   * @param xfm the link transform for the default rewriter
   */
  @Override
  public InputStream createLinkRewriter(String mimeType,
					ArchivalUnit au,
					InputStream in,
					String encoding,
					String url,
					LinkTransform xfm)
      throws PluginException, IOException {
    
    
    NodeFilterHtmlLinkRewriterFactory fact =
      new NodeFilterHtmlLinkRewriterFactory();

    // Pre filter rewrites the internal links before converting to
    // ServeContent links. This is needed only for Journals; currently does
    // nothing for books - leave in place for future expansion

    if (infobikePat != null) {
      fact.addPreXform(new PreFilter(au));
    }
    fact.addAttrToRewrite("data-popup");

    // Post filter cleans up problems with the "b" rewrite which messes up
    // the "onclick=" and puts the 'ServeContent?url=' before the
    // 'javascript:popup(' instead of inside the link section of the
    // javascript method Books do not have an "api_url", so just pass in
    // base_url which is the only base host

    String plugId = au.getPluginId();
    String linkHost = au.getConfiguration().get("base_url");
    if (plugId.contains("IngentaJournalPlugin")) {
      linkHost = au.getConfiguration().get("api_url");
    }
    LinkTransform linkXform = 
        new IngentaJavaScriptLinkTransform(encoding,
					   new Pattern[] { popupPat },
					   linkHost);
    NodeFilter rwrOnClik = new RewriteOnClickTransform(linkXform);
    fact.addPostXform(rwrOnClik);

    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
  }
 

  /**
   * This filter does only Ingenta-specific rewriting for 
   * Article PDF and HTML links in Journals. Books links will get handled by
   * the base link rewriter (followed by cleanup of the javascript)
   * 
   * This now handles three possible cases:
   * 
   * original journals - straight href of the link
   *     <a href="/search/download?pub=infobike%3a%2f%2figsoc%2fagl%2f2011%2f00000052%2f00000057%2fart00002&mimetype=application%2fpdf&exitTargetId=1358355690460" 
   *        title="PDF download of title" class="no-underline contain" target="_blank”>
   *        
   * NOTE: for now this one won't get the transform ServeContent? put on by the base class
   * because it doesn't know to look for data-popup      
   * some volumes of journals
   *     <a class="fulltext pdf btn btn-general icbutton" 
   *     data-popup='/search/download?pub=infobike%3a%2f%2figsoc%2fagl%2f2015%2f00000056%2f00000069%2fart00004&mimetype=application%2fpdf&exitTargetId=1449858096127' 
   *     title="PDF download of title" class="no-underline contain" >
   *     
   * books and newer journals (variant of newer version)
   *      <a class="fulltext pdf btn btn-general icbutton" 
   *      onclick="javascript:popup('/search/download?pub=infobike%3a%2f%2fbkpub%2f2nk9qe%2f1999%2f00000001%2f00000001%2fart00003&mimetype=application%2fpdf&exitTargetId=1463765390340','downloadWindow','900','800')" 
   *      title="PDF download of title" class="no-underline contain" >     
   * 
   */
  static class PreFilter implements NodeFilter {
    ArchivalUnit au;

    PreFilter(ArchivalUnit au) {
      this.au = au;
    }

    public boolean accept(Node node) {
      try {
	if (node instanceof LinkTag) {
	  Attribute attribute = ((TagNode)node).getAttributeEx("href");
	  // try the simplest case first. Then fall through to the other options
	  // they do not co-exist 
	  if (attribute == null) {
	    attribute = ((TagNode)node).getAttributeEx("data-popup");
	  }
	  if (attribute == null) {
	    attribute = ((TagNode)node).getAttributeEx("onclick");
	  }
	  if (attribute != null) {
	    // simplest case is a straight href
	    String url = attribute.getValue();
	    //rewriteUrl is extended for the books rewriter
	    String newUrl = rewriteUrl(url, au);
	    if (!newUrl.equals(url)) {
	      // It puts it on the attribute it came from
	      attribute.setValue(newUrl);
	      ((TagNode)node).setAttributeEx(attribute);
	      if (log.isDebug3()) {
		log.debug3("rewrote: " + url + " to " + newUrl);
		log.debug3(node.toHtml());
	      }
	    }
	  }
	}
      } catch (Throwable ex) {
	log.error(node.toHtml(), ex);
      }
      return false;
    }

  
    /**
     * The way we rewrite must be specific to the type (books vs. journals) of
     * plugin because of params. 
     * 
     * Rewrite URLs that link to the article PDF and HTML files.
     * This is similar to the "normalization" that (should) take place
     * while crawling the article landing page. 
     * 
     * For Journals: 
     * It rewrites the original
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
     * For books, there is only one host - the base_url
     * and since the plugin came later, all the crawler versions have been
     * collected with the mimetype as part of the url
     * 
     * 
     * @param url the original URL to rewrite
     * @param au the Archival Unit for the URL
     * @return the normalized URL
     * @throws PluginException 
     */
    protected String rewriteUrl(String url, ArchivalUnit au)
	throws PluginException {
      String linkUrl;

      //
      //  The url we need to rewrite is either coming in directly or its
      //  embedded in a javascript call and must go back inside the same call.
      //  Currently onClick is only seen in books but leave it here in case 
      //  this usage moves to the journals platform as well
      //
      boolean hadOnClick=false;
      Matcher onClickMat = onClickPdfPattern.matcher(url);
      if (onClickMat.matches()) {
	hadOnClick = true;
	linkUrl = onClickMat.group(2);  // just the link portion
	log.debug3("pulled the link out of javascript:popup()");
      } else {
	linkUrl = url; 
	log.debug3("link came directly off attribute");
      }

      // ignore URLs that don't match the infobike pattern
      // even books use this pattern - with a volume/issue of 1
      Matcher matcher = infobikePat.matcher(linkUrl);
      if (!matcher.matches()) {
	return url; // not match - the original link (including javascript if there)
      }

      String plugId = au.getPluginId();
      String newUrl;
      if (plugId.contains("IngentaBooksPlugin")) {
	// currently does nothing - leave in place for future expansion as needed
	newUrl = getRewrittenForBook(linkUrl, matcher, au);
      } else if (plugId.contains("IngentaJournalPlugin")) {
	newUrl = getRewrittenForJournal(linkUrl, matcher, au);
      } else {
	throw new PluginException("Cannot identify the plugin type in Ingenta link rewriter: " + plugId);
      }
    
      if (linkUrl.equals(newUrl)){
        // same as went in, didn't match or some other issue
        return url; // the original link (including javascript if there)
      }
    
      // We have the modified url but if it was in a javascript, put it back
      if (hadOnClick) {
	// (1) = first quote (of whatever type) (3) = endquote that followed in the onClick
	String fullUrl =
	  "javascript:popup(" +
	  onClickMat.group(1) +
	  newUrl +
	  onClickMat.group(3);
	log.debug3("put newUrl back in to javascript: " + fullUrl);
	return fullUrl;
      }
      return newUrl;
    }
   
    String getRewrittenForJournal(String origUrl, Matcher matcher,
				  ArchivalUnit au) {
      String apiUrl = au.getConfiguration().get("api_url");
      if (apiUrl == null) {
	log.warning("IngentaHtmlLinkRewriter had no api_url");
	return origUrl; // did the books plugin inadvertently call this?
      }
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
	    || !mimeType.equalsIgnoreCase(HeaderUtil.getMimeTypeFromContentType(cu.getContentType()))) {
	  // see if matching URL without the original MIME type param 
	  // exists and matches original MIME type, e.g.
	  // http://api.ingentaconnect.com/...?crawler=true 
	  AuUtil.safeRelease(cu);
	  cu = au.makeCachedUrl(newUrlNoMime);
	  if ((cu == null) 
	      || !cu.hasContent()
	      || !mimeType.equalsIgnoreCase(HeaderUtil.getMimeTypeFromContentType(cu.getContentType()))) {
	    return origUrl; 
	  }
	  newUrl = newUrlNoMime;
	}
      } finally {
	AuUtil.safeRelease(cu);
      }
      return newUrl;
    }

    // guaranteed to be a book plugin
    String getRewrittenForBook(String origUrl, Matcher matcher,
			       ArchivalUnit au) {
      // books do not need to rewrite the link to use the api_url and the
      // change from a one-time url to the final crawler stable version is handled
      // by the url normalizer later in the process - just send it back unchanged
      return origUrl;
    }
  }

  static class RewriteOnClickTransform implements NodeFilter {
    private LinkTransform linkXform;

    RewriteOnClickTransform(LinkTransform linkXform) {
      this.linkXform = linkXform;
    }

    @Override
    public boolean accept(Node node) {
      if (node instanceof LinkTag) {
	LinkTag tag = (LinkTag)node;
	Attribute attr = tag.getAttributeEx("onclick");
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


  // Pattern for js call to a three-argument function isolates the
  // function argument and the prefix and postfix text as capture groups. 

  static final java.util.regex.Pattern popupPat = 
    Pattern.compile("(popup\\([\"'])([^\"',]+)([\"'][^)]+\\)(;)?)");
  // group1 = popup('
  // group2 = link
  // group3 = ', more, args);
  // why the semi-colon??

  /**
   * Transforms a javascript: link that was previously rewritten 
   * incorrectly for use with ServeContent.
   * 
   * @author Philip Gust
   */
  static class IngentaJavaScriptLinkTransform implements LinkTransform {
    // Matches the prefix of an absolute URL
    static final java.util.regex.Pattern absUrlPrefixPat = 
      java.util.regex.Pattern.compile("^[^:/?#]+://+.*$");

    /** The encoding to decode and re-encode rewritten text. */
    final String encoding;
     
    final String link_host; 

    /** The JS pattern with a URL to rewrite */
    final Pattern[] jsUrlRewritePats;

    /**
     * Create a new instance for the encoding and rewrite patterns.
     * 
     * @param encoding the encoding
     * @param jsUrlRewritePats the patterns of javascript URLs to rewrite
     */
    IngentaJavaScriptLinkTransform(String encoding,
				   Pattern[] jsUrlRewritePats,
				   String link_host) {
      this.encoding = encoding;
      this.link_host = link_host;
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
	// split the onclick after the "url="
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
		  // only transform the URL if it is from the appropriate linkHost 
		  // for journals it is api_host and for books base_host, but 
		  // it is passed in
		  if (newUrl.startsWith(link_host)
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
	  log.siteError("bad encoding during link rewriting", ex);
	}
      }

      return url;
    }
  }

}
