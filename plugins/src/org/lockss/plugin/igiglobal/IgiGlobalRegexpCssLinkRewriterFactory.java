package org.lockss.plugin.igiglobal;

import org.apache.commons.lang3.StringUtils;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.RegexpCssLinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IgiGlobalRegexpCssLinkRewriterFactory implements LinkRewriterFactory {

  private static final Logger log =
      Logger.getLogger(IgiGlobalRegexpCssLinkRewriterFactory.class);

  enum CssLinkRewriterUrlEncodeMode {Full, Minimal};

  /** Controls the amount of URL-encoding applied when rewriting CSS files
   * and fragments.  If <code>Full</code>, URLs embedded as query args in
   * ServeContent URLs are fully URL-encoded; if <code>Minimal</code>, only
   * the bare minimum encoding necessary is applied ("?", "&amp;", "=") */
  public static final String PARAM_URL_ENCODE =
      Configuration.PREFIX + "cssLinkRewriter.urlEncode";
  public static final CssLinkRewriterUrlEncodeMode DEFAULT_URL_ENCODE =
      CssLinkRewriterUrlEncodeMode.Full;

  private static final int MAX_URL_LENGTH = 3100;
  // Amount of CSS input to buffer up for matcher
  private static final int DEFAULT_MAX_BUF = 32 * 1024;
  // Amount at end of buffer to rescan at beginning of next bufferfull
  private static final int DEFAULT_OVERLAP = 2 * 1024;

  // This regexp is too permissive.  Using dot to match the URL chars in G2
  // allows illegal chars and mismatched quotes.  (The reluctant match
  // ensures it doesn't span multiple url() constructs.)  It's believed
  // this properly matches all non-pathological legal constructs and
  // matching some illegal constructs isn't a problem.  (Firefox 3.6
  // follows the spec and stops processing the css at the first illegal
  // syntax.)

  private static final String CSS_URI_EXTRACTOR =
      "(?i)(?:@import\\s+(?:url[(]|)|url[(])\\s*([\\\"\']?)" + // G1
          "(.{0," + MAX_URL_LENGTH + "}?)" + //G2
          "(\\1)\\s*[);]"; // G3
  // GROUPS:
  // (G1) optional ' or "
  // (G2) URI
  // (G3) = G1

  private CssLinkRewriterUrlEncodeMode urlEncodeMode =
      DEFAULT_URL_ENCODE;

  private static final int GQUOTE1 = 1;
  private static final int GURL = 2;
  private static final int GQUOTE2 = 3;

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

  public void IgiGlobalRegexpCssLinkRewriter() {
  }

  /** For testing buffer shifting */
  void IgiGlobalRegexpCssLinkRewriter(int maxBuf, int overlap) {
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
        config.getEnum(CssLinkRewriterUrlEncodeMode.class,
            PARAM_URL_ENCODE, DEFAULT_URL_ENCODE);
  }
}
