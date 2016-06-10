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
 * This custom link rewriter performs Ingenta  specific rewriting 
 * for both books and journals to
 * make the PDF and HTML article files available through the button links 
 * on journal article  or chapter pages. 
 *  * Ingenta has made the files available to LOCKSS
 * on another server, so the links must be rewritten to redirect to the
 * correct location on that other server.
 * 
 * @author Philip Gust
 */
public class IngentaHtmlLinkRewriterFactory 
  extends NodeFilterHtmlLinkRewriterFactory {
  
  private static final Logger log = Logger.getLogger(IngentaHtmlLinkRewriterFactory.class);

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
   * This now handles three possible cases:
   * 
   * original journals - straight href of the link
   *     <a href="/search/download?pub=infobike%3a%2f%2figsoc%2fagl%2f2011%2f00000052%2f00000057%2fart00002&mimetype=application%2fpdf&exitTargetId=1358355690460" 
   *        title="PDF download of title" class="no-underline contain" target="_blank”>
   *        
   * newer volumes of journals
   *     <a class="fulltext pdf btn btn-general icbutton" 
   *     data-popup='/search/download?pub=infobike%3a%2f%2figsoc%2fagl%2f2015%2f00000056%2f00000069%2fart00004&mimetype=application%2fpdf&exitTargetId=1449858096127' 
   *     title="PDF download of title" class="no-underline contain" >
   *     
   * books (variant of newer version)
   *      <a class="fulltext pdf btn btn-general icbutton" 
   *      onclick="javascript:popup('/search/download?pub=infobike%3a%2f%2fbkpub%2f2nk9qe%2f1999%2f00000001%2f00000001%2fart00003&mimetype=application%2fpdf&exitTargetId=1463765390340','downloadWindow','900','800')" 
   *      title="PDF download of title" class="no-underline contain" >     
   * 
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
    };

    // compose streams to normalize URLs before rewriting
    HtmlTransform htmlXfm = HtmlNodeFilterTransform.exclude(filter);
    return new HtmlFilterInputStream(in, encoding, encoding, htmlXfm);
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
   protected String rewriteUrl(String url, ArchivalUnit au) throws PluginException {
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
      String fullUrl = "javascript:popup(" + onClickMat.group(1) + newUrl + onClickMat.group(3);
      log.debug3("put newUrl back in to javascript: " + fullUrl);
      return fullUrl;
    }
    return newUrl;
  }
   
   String getRewrittenForJournal(String origUrl, Matcher matcher, ArchivalUnit au) {
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
   String getRewrittenForBook(String origUrl, Matcher matcher, ArchivalUnit au) {
     String baseUrl = au.getConfiguration().get("base_url");
     StringBuilder sb = new StringBuilder(baseUrl);
     // we normalize contentone to content so we can just set this
     sb.append("content");
     for (int i = 2; i <= 7; i++) {
       sb.append('/');
       sb.append(matcher.group(i));
     }
     sb.append("?crawler=true");
     // create version of rewritten URL with original MIME type
     sb.append("&mimetype=");
     sb.append(matcher.group(8));
     sb.append("/");
     sb.append(matcher.group(9));
     String newUrl = sb.toString();
     
     // does this url exist in the cache?
     CachedUrl cu = au.makeCachedUrl(newUrl);
     try {
       if ((cu == null) 
           || !cu.hasContent()) {
         return origUrl; // nope - don't rewrite. This is all of the original
       }
     } finally {
       AuUtil.safeRelease(cu);
     }
     return newUrl;
   }
}
