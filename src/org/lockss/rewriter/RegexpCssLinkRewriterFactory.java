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

package org.lockss.rewriter;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.lang3.StringUtils;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.servlet.*;

/**
 * Rewrite links in CSS by finding them with a regular expression and
 * applying a transformation.  Processes backslash-escaped URL chars
 * (parentheses, commas, whitespace, single and double quotes) but not
 * numeric escapes.  Illegal URLs are ignored.
 *
 * This implementation may miss pathological cases where huge amounts of
 * whitespace make a single statement larger than the buffer overlap.
 */
public class RegexpCssLinkRewriterFactory implements LinkRewriterFactory {
  
  private static final Logger log =
    Logger.getLogger(RegexpCssLinkRewriterFactory.class);
  
  enum CssLinkRewriterUrlEncodeMode {Full, Minimal};

  /** Controls the amount of URL-encoding applied when rewriting CSS files
   * and fragments.  If <code>Full</code>, URLs embedded as query args in
   * ServeContent URLs are fully URL-encoded; if <code>Minimal</code>, only
   * the bare minimum encoding necessary is applied ("?", "&amp;", "=") */
  public static final String PARAM_URL_ENCODE =
    Configuration.PREFIX + "cssLinkRewriter.urlEncode";
  public static final CssLinkRewriterUrlEncodeMode DEFAULT_URL_ENCODE =
    CssLinkRewriterUrlEncodeMode.Full;

  private static final int DEFAULT_MAX_URL_LENGTH = 2100;
  // Amount of CSS input to buffer up for matcher
  private static final int DEFAULT_MAX_BUF = 32 * 1024;
  // Amount at end of buffer to rescan at beginning of next bufferfull
  private static final int DEFAULT_OVERLAP = 2 * 1024;


  private CssLinkRewriterUrlEncodeMode urlEncodeMode =
    DEFAULT_URL_ENCODE;

  private static final int GQUOTE1 = 1;
  private static final int GURL = 2;
  private static final int GQUOTE2 = 3;

  // This regexp is too permissive.  Using dot to match the URL chars in G2
  // allows illegal chars and mismatched quotes.  (The reluctant match
  // ensures it doesn't span multiple url() constructs.)  It's believed
  // this properly matches all non-pathological legal constructs and
  // matching some illegal constructs isn't a problem.  (Firefox 3.6
  // follows the spec and stops processing the css at the first illegal
  // syntax.)

  private static String createCssUriExtractor(int maxUrlLen) {
    return "(?i)(?:@import\\s+(?:url[(]|)|url[(])\\s*([\\\"\']?+)" + // G1
        "(.{0," + maxUrlLen + "}?)" + //G2
        "(\\1)\\s*[);]"; // G3
    // GROUPS:
    // (G1) optional ' or "
    // (G2) URI
    // (G3) = G1
  }

  private static final String CSS_URI_EXTRACTOR = createCssUriExtractor(DEFAULT_MAX_URL_LENGTH);

  static Pattern CSS_URL_PAT = Pattern.compile(CSS_URI_EXTRACTOR);

  // Chars that need escaping in URLs in CSS
  private static final String CSS_ESCAPE_CHARS = "\\() '\"";

  // Pattern to match character escapes to be removed from URLs before
  // processing
  private static final String CSS_BACKSLASH_ESCAPE = "\\\\([,'\"\\(\\)\\s])";

  private static Pattern CSS_BACKSLASH_PAT =
    Pattern.compile(CSS_BACKSLASH_ESCAPE);

  private int maxBuf = DEFAULT_MAX_BUF;
  private int overlap = DEFAULT_OVERLAP;

  public RegexpCssLinkRewriterFactory() {
  }

  /** For testing buffer shifting */
  RegexpCssLinkRewriterFactory(int maxBuf, int overlap) {
    this.maxBuf = maxBuf;
    this.overlap = overlap;
  }
  /** For testing buffer shifting */
  RegexpCssLinkRewriterFactory(int maxBuf, int overlap, int maxUrlLen) {
    this.CSS_URL_PAT = Pattern.compile(createCssUriExtractor(maxUrlLen));
    this.maxBuf = maxBuf;
    this.overlap = overlap;
  }

  /* Inherit documentation */
  public InputStream createLinkRewriter(String mimeType,
					ArchivalUnit au,
					InputStream in,
					String encoding,
					String srcUrl,
					ServletUtil.LinkTransform srvLinkXform)
      throws PluginException, IOException {
    if (in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    }
    // Cause error now if illegal base url
    try {
      new URL(srcUrl);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
    log.debug("Rewriting " + srcUrl + " in AU " + au);
    setConfig();
    Collection<String> urlStems = au.getUrlStems();
    StringBuilder out = new StringBuilder();

    // This needs a regexp matcher that can match against a Reader.
    // Interim solution is to loop matching against a rolling fixed-length
    // chunk of input, with overlaps between chunks.  Can miss URLs in
    // pathological situations.

    Reader rdr = new BufferedReader(StringUtil.getLineReader(in, encoding));
    rdr = StringUtil.getLineContinuationReader(rdr);
    StringBuilder sb = new StringBuilder(maxBuf);
    try {
      while (StringUtil.fillFromReader(rdr, sb, maxBuf - sb.length())) {
	Matcher m1 = CSS_URL_PAT.matcher(sb);
	int lastAppendPosition = 0;
	while (m1.find()) {
	  String url = processUrlEscapes(m1.group(GURL));
	  String rewritten = rewrite(url, srcUrl, urlStems, srvLinkXform);
	  if (url.equals(rewritten)) {
	    out.append(sb.subSequence(lastAppendPosition, m1.end()));
	    lastAppendPosition = m1.end();
	  } else {
	    out.append(sb.subSequence(lastAppendPosition, m1.start(GQUOTE1)));
	    out.append("'");
	    out.append(urlEscape(rewritten));
	    out.append("'");
	    lastAppendPosition = m1.end(GQUOTE2);
	    out.append(sb.subSequence(lastAppendPosition, m1.end()));
	    lastAppendPosition = m1.end();
	  }
	}
	int sblen = sb.length();
	if (sblen < maxBuf) {
	  // less then full buffer means last buffer
	  out.append(sb.subSequence(lastAppendPosition, sblen));
	  break;
	}
	// Move the overlap amount to the beginning of the buffer
	int keep;
	if (lastAppendPosition == 0) {
	  // no matches, shift all but overlap
	  keep = Math.min(overlap, maxBuf / 2);
	} else {
	  // keep chars after last match, or max overlap
	  keep = Math.min(overlap, sblen - lastAppendPosition);
	}
	out.append(sb.subSequence(lastAppendPosition, sblen - keep));
	sb.delete(0, sblen - keep);
      }
    } finally {
      IOUtil.safeClose(rdr);
    }
    return new ReaderInputStream(new StringReader(out.toString()), encoding);
  }
  
  /** Rewrite absolute URLs on any of AUs hosts (urlStems), and relative
   * URLs, to appropriate servlet URL */
  String rewrite(String url,
		 String srcUrl,
		 Collection<String> urlStems,
		 ServletUtil.LinkTransform srvLinkXform) {
    if (UrlUtil.isAbsoluteUrl(url)) {
      for (String stem : urlStems) {
	if (StringUtil.startsWithIgnoreCase(url, stem)) {
	  return srvLinkXform.rewrite(encodeQueryArg(url));
	}
      }
      return url;
    } else  {
      try {
	return srvLinkXform.rewrite(encodeQueryArg(UrlUtil.resolveUri(srcUrl,
								      url,
								      urlEncodeMode == CssLinkRewriterUrlEncodeMode.Minimal)));
      } catch (MalformedURLException e) {
	log.error("Can't rewrite " + url + " in " + srcUrl);
	return url;
      }
    }
  }

  /** Remove backslashes when used as escape character in CSS URL.
   * Should probably also process hex URL encodings */
  String processUrlEscapes(String url) {
    Matcher m2 = CSS_BACKSLASH_PAT.matcher(url);
    return m2.replaceAll("$1");
  }

  /** Backslash escape special characters in URL */
  String urlEscape(String url) {
    if (!StringUtils.containsAny(url, CSS_ESCAPE_CHARS)) {
      return url;
    }
    StringBuilder sb = new StringBuilder();
    int len = url.length();
    for (int counter = 0; counter < len; counter++) {
      char c = url.charAt(counter);
      if (CSS_ESCAPE_CHARS.indexOf(c) >= 0) {
	sb.append("\\");
      }
      sb.append(c);
    }
    return sb.toString();
  }

  String encodeQueryArg(String str) {
    switch (urlEncodeMode) {
    case Full:
      return UrlUtil.encodeUrl(str);
    case Minimal:
    default:
      return UrlUtil.encodeQueryArg(str);
    }
  }

  private void setConfig() {
    Configuration config = ConfigManager.getCurrentConfig();
    urlEncodeMode =
      (CssLinkRewriterUrlEncodeMode)
      config.getEnum(CssLinkRewriterUrlEncodeMode.class,
		     PARAM_URL_ENCODE, DEFAULT_URL_ENCODE);
  }

}
