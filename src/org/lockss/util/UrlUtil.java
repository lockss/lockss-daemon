/*
 * $Id$
 *

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

package org.lockss.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginBehaviorException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.urlconn.HttpClientUrlConnection;
import org.lockss.util.urlconn.JavaHttpUrlConnection;
import org.lockss.util.urlconn.JavaUrlConnection;
import org.lockss.util.urlconn.LockssUrlConnection;
import org.lockss.util.urlconn.LockssUrlConnectionPool;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/** Utilities for URLs and URLConnections
 */
public class UrlUtil {

  private static final Logger log = Logger.getLogger(UrlUtil.class);

  /**
   * The separator string for URLs.
   */
  public static final String URL_PATH_SEPARATOR = "/";
  /**
   * The separator char for URLs.
   */
  public static final char URL_PATH_SEPARATOR_CHAR = '/';

  static final String PREFIX = Configuration.PREFIX + "UrlUtil.";

  public static final int PATH_TRAVERSAL_ACTION_ALLOW = 1;
  public static final int PATH_TRAVERSAL_ACTION_REMOVE = 2;
  public static final int PATH_TRAVERSAL_ACTION_THROW = 3;

  /** Determines normalizeUrl()s action on path traversals (extra ".." path
   * components). <ul><li>PATH_TRAVERSAL_ACTION_ALLOW (1) - Allow them
   * (leave the extra ".."s in the path). <li>PATH_TRAVERSAL_ACTION_REMOVE
   * (2) Remove them (which is what browsers do)
   * <li>PATH_TRAVERSAL_ACTION_THROW (3) throw
   * MalformedURLException</ol> */
  public static final String PARAM_PATH_TRAVERSAL_ACTION =
      PREFIX + "pathTraversalAction";
  public static final int DEFAULT_PATH_TRAVERSAL_ACTION =
      PATH_TRAVERSAL_ACTION_REMOVE;

  /** If true, hex chars in URL encodings are normalized to upper case */
  public static final String PARAM_NORMALIZE_URL_ENCODING_CASE =
      PREFIX + "normalizeUrlEncodingCase";
  public static final boolean DEFAULT_NORMALIZE_URL_ENCODING_CASE = true;

  /** If true, trailing ? is removed from URLs */
  public static final String PARAM_NORMALIZE_EMPTY_QUERY =
      PREFIX + "normalizeEmptyQuery";
  public static final boolean DEFAULT_NORMALIZE_EMPTY_QUERY = true;

  /** If true, use Apache Commons HttpClient, if false use native Java
   * HttpURLConnection */
  static final String PARAM_USE_HTTPCLIENT = PREFIX + "useHttpClient";
  static final boolean DEFAULT_USE_HTTPCLIENT = true;

  /** If true, normalizeUrl replaces Akamai Resource Locator URLs (ARL) of
   * the form
   * <code>http://a123.g.akamai.net/f/123/4567/1d/www.pubsite.com/images/blip.ico</code>
   * with the embedded URL (e.g.,
   * <code>http://www.pubsite.com/images/blip.ico</code>). */
  public static final String PARAM_NORMALIZE_AKAMAI_URL =
      PREFIX + "normalizeAkamaiUrl";
  public static final boolean DEFAULT_NORMALIZE_AKAMAI_URL = false;

  /** If true, plugin-supplied URL normalizers are allowed to change the
   * URL stem (scheme, host or port), but only within the set of stems on
   * which the AU has permission to collect content.
   */
  public static final String PARAM_ALLOW_SITE_NORMALIZE_CHANGE_STEM =
      PREFIX + "allowSiteNormalizeChangeStem";
  public static final boolean DEFAULT_ALLOW_SITE_NORMALIZE_CHANGE_STEM = true;

  private static boolean useHttpClient = DEFAULT_USE_HTTPCLIENT;
  private static int pathTraversalAction = DEFAULT_PATH_TRAVERSAL_ACTION;
  private static boolean normalizeUrlEncodingCase =
      DEFAULT_NORMALIZE_URL_ENCODING_CASE;
  private static boolean normalizeEmptyQuery = DEFAULT_NORMALIZE_EMPTY_QUERY;
  private static boolean normalizeAkamaiUrl = DEFAULT_NORMALIZE_AKAMAI_URL;
  private static boolean allowSiteNormalizeChangeStem =
      DEFAULT_ALLOW_SITE_NORMALIZE_CHANGE_STEM;


  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
                               Configuration oldConfig,
                               Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      useHttpClient = config.getBoolean(PARAM_USE_HTTPCLIENT,
          DEFAULT_USE_HTTPCLIENT);
      pathTraversalAction = config.getInt(PARAM_PATH_TRAVERSAL_ACTION,
          DEFAULT_PATH_TRAVERSAL_ACTION);
      normalizeUrlEncodingCase =
          config.getBoolean(PARAM_NORMALIZE_URL_ENCODING_CASE,
              DEFAULT_NORMALIZE_URL_ENCODING_CASE);
      normalizeEmptyQuery = config.getBoolean(PARAM_NORMALIZE_EMPTY_QUERY,
          DEFAULT_NORMALIZE_EMPTY_QUERY);
      normalizeAkamaiUrl = config.getBoolean(PARAM_NORMALIZE_AKAMAI_URL,
          DEFAULT_NORMALIZE_AKAMAI_URL);
      allowSiteNormalizeChangeStem =
          config.getBoolean(PARAM_ALLOW_SITE_NORMALIZE_CHANGE_STEM,
              DEFAULT_ALLOW_SITE_NORMALIZE_CHANGE_STEM);
    }
  }

  private static String trimNewlinesAndLeadingWhitespace(String urlString) {
    urlString = urlString.trim();	// remove surrounding spaces
    urlString = StringUtil.trimNewlinesAndLeadingWhitespace(urlString);
    return urlString;
  }

  static Pattern AKAMAI_PATH_PAT =
    Pattern.compile("/(?:[^/]+/){4}([^/]+)(/.*)?$");

  /** Normalize URL to a canonical form: lowercase scheme and hostname,
   * normalize path.  Removes any reference part.  XXX need to add
   * character escaping
   * @throws MalformedURLException
   */
  public static String normalizeUrl(String urlString)
      throws MalformedURLException {
    return normalizeUrl(urlString, pathTraversalAction);
  }

  /** Normalize URL to a canonical form: lowercase scheme and hostname,
   * normalize path.  Removes any reference part.  XXX need to add
   * character escaping
   * @throws MalformedURLException
   */
  public static String normalizeUrl(String urlString, int pathTraversalAction)
      throws MalformedURLException {
    log.debug3("Normalizing "+urlString);
    urlString = trimNewlinesAndLeadingWhitespace(urlString);
    if ("".equals(urlString)) {		// permit empty
      return urlString;
    }
    URL url = new URL(urlString);

    String protocol = url.getProtocol(); // returns lowercase proto
    String host = url.getHost();
    int port = url.getPort();
    String path = url.getPath();
    String query = url.getQuery();
    if (log.isDebug3()) {
      log.debug3("protocol: "+protocol);
      log.debug3("host: "+host);
      log.debug3("port: "+port);
      log.debug3("path: "+path);
      log.debug3("query: "+query);
    }

    boolean changed = false;

    if (!urlString.startsWith(protocol)) { // protocol was lowercased
      changed = true;
    }

    if (normalizeAkamaiUrl &&
        StringUtil.endsWithIgnoreCase(host, ".akamai.net")) {
      Matcher m = AKAMAI_PATH_PAT.matcher(path);
      if (m.find()) {
        host = m.group(1);
        path = m.group(2);
        changed = true;
        log.debug2("Akamai rewrite newhost, newpath: " + host + ", " + path);
      }
    }

//     if ("http".equals(protocol) || "ftp".equals(protocol)) {
    if (host != null) {
      String newHost = host.toLowerCase(); // lowercase host
      if (!host.equals(newHost)) {
        host = newHost;
        changed = true;
      }
    }

    if (port == getDefaultPort(protocol)) {
      // if url includes a port that is the default port for the protocol,
      // remove it (by passing port -1 to constructor)
      port = -1;
      changed = true;
    }

    if (StringUtil.isNullString(path)) {
      path = "/";
      changed = true;
    } else {
      String normPath = normalizePath(path, pathTraversalAction);
      if (!normPath.equals(path)) {
        if (log.isDebug3()) log.debug3("Normalized "+path+" to "+normPath);
        path = normPath;
        changed = true;
      }
    }

    if (!StringUtil.isNullString(query)) {
      String normQuery = normalizeUrlEncodingCase(query);
      if (!normQuery.equals(query)) {
        if (log.isDebug3())
          log.debug3("Normalized query "+query+" to "+normQuery);
        query = normQuery;
        changed = true;
      }
    } else if (normalizeEmptyQuery && "".equals(query)) {
      query = null;
      changed = true;
    }

    if (url.getRef() != null) {		// remove the ref
      changed = true;
    }
//   }
    if (changed) {
      urlString =
          new URL(protocol, host, port,
              (StringUtil.isNullString(query) ? path : (path + "?" + query))
          ).toString();
      log.debug3("Changed, so returning "+urlString);
    }
    return urlString;
  }

  /** Normalize URL to a canonical form for the specified AU.  First
   * applies any plugin-specific normalization, then generic normalization.
   * Disallows changes to protocol, host or port on the theory that such
   * changes constitute more than "normalization".  This might be too
   * strict; transformations such as <code>publisher.com ->
   * www.publisher.com</code> might fall within the scope of normalization.
   * @param url the url to normalize
   * @param au the AU in which to look for a plugin-specific normalizer, or
   * null for no plugin-specific normalization
   * @throws MalformedURLException if the plugin's normalizer throws, or if
   * the URL it returns is malformed.
   * @throws PluginBehaviorException if the plugin changes the URL in a way
   * it shouldn't (the protocol, host or port)
   */
  public static String normalizeUrl(String url, ArchivalUnit au)
      throws MalformedURLException, PluginBehaviorException {
    String site;
    if (au == null) {
      site = url;
    } else {
      try {
        site = au.siteNormalizeUrl(url);
      } catch (RuntimeException w) {
        throw new MalformedURLException(url);
      }
      if (site != url) {
        String normSite = normalizeUrl(site);
        if (normSite.equals(url)) {
          // ensure return arg if equivalent
          return normSite;
        } else {
          // check whether stem was changed
          URL origUrl = new URL(url);
          URL siteUrl = new URL(normSite);
          if (origUrl.getProtocol().equals(siteUrl.getProtocol()) &&
              origUrl.getHost().equals(siteUrl.getHost()) &&
              isEquivalentPort(origUrl.getProtocol(),
                  origUrl.getPort(), siteUrl.getPort())) {
            return normSite;
          } else if (allowSiteNormalizeChangeStem) {
            // Illegal normalization if either the original stem or the new
            // one aren't known stems for this AU.
            // (This constraint is required by the stam -> candidate-AUs
            // mapping in PluginManager.  If the pre-normalized stem isn't
            // in the map, the AU won't be found.

            checkFrom: {
              String origStem = UrlUtil.getUrlPrefix(url);
              for (String stem : au.getUrlStems()) {
                if (origStem.equalsIgnoreCase(stem)) {
                  break checkFrom;
                }
              }
              throw new PluginBehaviorException("siteNormalizeUrl(" + url +
                                                ") changed stem from " +
                                                "one not known for AU: " +
                                                url);
            }
            checkTo: {
              String siteStem = UrlUtil.getUrlPrefix(normSite);
              for (String stem : au.getUrlStems()) {
                if (siteStem.equalsIgnoreCase(stem)) {
                  break checkTo;
                }
              }
              throw new PluginBehaviorException("siteNormalizeUrl(" + url +
                                                ") changed stem to " +
                                                "one not known for AU: " +
                                                site);
            }
          } else {
            throw new PluginBehaviorException("siteNormalizeUrl(" + url +
                                              ") changed stem to " +
                                              site);
          }
        }
      }
    }
    return normalizeUrl(site);
  }

  static boolean isEquivalentPort(String proto, int port1, int port2) {
    if (port1 == port2) {
      return true;
    }
    int def = getDefaultPort(proto);
    return (port1 == def && port2 == -1) || (port2 == def && port1 == -1);
  }

  /** Return the default port for the (already lowercase) protocol */
  // 1.3 URL doesn't expose this
  public static int getDefaultPort(String protocol) {
    if ("http".equals(protocol)) return 80;
    if ("https".equals(protocol)) return 443;
    if ("ftp".equals(protocol)) return 21;
    if ("gopher".equals(protocol)) return 70;
    return -1;
  }

  /** Normalize the path component.  Replaces multiple consecutive "/" with
   * a single "/", removes "." components and resolves ".."  components.
   * If there are extra ".." components in the path, the behavior depends
   * on the config parameter org.lockss.urlutil.pathTraversalAction, see
   * {@link #PARAM_PATH_TRAVERSAL_ACTION}.
   * @param path the path to normalize
   */
  public static String normalizePath(String path)
      throws MalformedURLException {
    return normalizePath(path, pathTraversalAction);
  }

  /** Normalize the path component.  Replaces multiple consecutive "/" with
   * a single "/", removes "." components and resolves ".."  components.
   * @param path the path to normalize
   * @param pathTraversalAction what to do if extra ".." components, see
   * {@link #PARAM_PATH_TRAVERSAL_ACTION}.
   */
  public static String normalizePath(String path, int pathTraversalAction)
      throws MalformedURLException {
    path = path.trim();
    // special case compatability with Java 1.4 URI
    if (path.equals(".") || path.equals("./")) {
      return "";
    }
    path = normalizeUrlEncodingCase(path);

    // quickly determine whether anything needs to be done
    if (! (path.endsWith("/.") || path.endsWith("/..") ||
           path.equals("..") || path.equals(".") ||
           path.startsWith("../") || path.startsWith("./") ||
           path.indexOf("/./") >= 0 || path.indexOf("/../") >= 0 ||
           path.indexOf("//") >= 0)) {
      return path;
    }

    StringTokenizer st = new StringTokenizer(path, "/");
    List names = new ArrayList();
    int dotdotcnt = 0;
    boolean prevdotdot = false;
    while (st.hasMoreTokens()) {
      String comp = st.nextToken();
      prevdotdot = false;
      if (comp.equals(".")) {
        continue;
      }
      if (comp.equals("..")) {
        if (names.size() > 0) {
          names.remove(names.size() - 1);
          prevdotdot = true;
        } else {
          switch (pathTraversalAction) {
            case PATH_TRAVERSAL_ACTION_THROW:
              throw new MalformedURLException("Illegal dir traversal: " + path);
            case PATH_TRAVERSAL_ACTION_ALLOW:
              dotdotcnt++;
              break;
            case PATH_TRAVERSAL_ACTION_REMOVE:
              break;
          }
        }
      } else {
        names.add(comp);
      }
    }

    StringBuilder sb = new StringBuilder();
    if (path.startsWith("/")) {
      sb.append("/");
    }
    for (int ix = dotdotcnt; ix > 0; ix--) {
      if (ix > 1 || !names.isEmpty()) {
        sb.append("../");
      } else {
        sb.append("..");
      }
    }
    StringUtil.separatedString(names, "/", sb);
    if ((path.endsWith("/") || (prevdotdot && !names.isEmpty())) &&
        !(sb.length() == 1 && path.startsWith("/"))) {
      sb.append("/");
    }
    return sb.toString();
  }

  public static String normalizeUrlEncodingCase(String str) {
    if (!normalizeUrlEncodingCase) {
      return str;
    }
    int pos = str.indexOf('%');
    if (pos < 0) {
      return str;
    }
    StringBuffer sb = new StringBuffer(str);
    int len = str.length();
    do {
      if (len < pos + 3) {
        break;
      }
      char ch;
      if (Character.isLowerCase(ch = sb.charAt(pos + 1))) {
        sb.setCharAt(pos + 1, Character.toUpperCase(ch));
      }
      if (Character.isLowerCase(ch = sb.charAt(pos + 2))) {
        sb.setCharAt(pos + 2, Character.toUpperCase(ch));
      }
    } while ((pos = str.indexOf('%', pos + 3)) >= 0);
    return sb.toString();
  }

  /** Compare two URLs for equality.  Unlike URL.equals(), this does not
   * cause DNS lookups.
   * @param u1 a URL
   * @param u2 a nother URL
   * @return true iff the URLs have the same protocol (case-independent),
   * the same host (case-independent), the same port number on the host,
   * and the same file and anchor on the host.
   */
  public static boolean equalUrls(URL u1, URL u2) {
    return
        u1.getPort() == u2.getPort() &&
        StringUtil.equalStringsIgnoreCase(u1.getProtocol(), u2.getProtocol()) &&
        StringUtil.equalStringsIgnoreCase(u1.getHost(), u2.getHost()) &&
        StringUtil.equalStrings(u1.getFile(), u2.getFile()) &&
        StringUtil.equalStrings(u1.getRef(), u2.getRef());
  }

  private static Pattern URL_PAT =
    Pattern.compile("^[a-zA-Z]+:/", Pattern.CASE_INSENSITIVE);

  /** Return true if the string is a url.  Very basic, just checks that the
   * string starts with &quot;scheme:/&quot;.  (So returns false for,
   * <i>eg</i>, <tt>jar:file:...</tt>)*/
  public static boolean isUrl(String str) {
    return URL_PAT.matcher(str).find();
  }

  /**
   * Returns true if an http: (but not https:) URL
   * @since 1.70
   */
  // XXX does this need to trim blanks?
  public static boolean isHttpUrl(String url) {
    return StringUtil.startsWithIgnoreCase(url, "http:");
  }

  /**
   * Returns true if an https: (but not http:) URL
   * @since 1.70
   */
  // XXX does this need to trim blanks?
  public static boolean isHttpsUrl(String url) {
    return StringUtil.startsWithIgnoreCase(url, "https:");
  }

  /** Return true if an http: or https: url */
  // XXX does this need to trim blanks?
  public static boolean isHttpOrHttpsUrl(String url) {
    return isHttpUrl(url) || isHttpsUrl(url);
  }

  /** Return true if a file: url */
  public static boolean isFileUrl(String url) {
    return StringUtil.startsWithIgnoreCase(url, "file:");
  }

  /** Return true if a jar: url */
  public static boolean isJarUrl(String url) {
    return StringUtil.startsWithIgnoreCase(url, "jar:");
  }

  /** Return a jar file URL pointing to the entry in the jar */
  public static String makeJarFileUrl(String jarPath, String entryName)
      throws MalformedURLException {
    return "jar:" + new File(jarPath).toURI().toURL() + "!/" + entryName;
  }

  /**
   * @param urlStr string representation of a url
   * @return Prefix of url including protocol and host (and port).  Ends
   * with "/", because it's not completely well-formed without it.  Returns
   * the original string if it's already the prefix
   * @throws MalformedURLException if urlStr is not a well formed URL
   */
  public static String getUrlPrefix(String urlStr)
      throws MalformedURLException{
    String ret = getUrlPrefix(new URL(urlStr));
    return ret.equals(urlStr) ? urlStr : ret;
  }

  /**
   * @param url string representation of a url
   * @return Prefix of url including protocol and host (and port).  Ends
   * with "/", because it's not completely well-formed without it.
   * @throws MalformedURLException if urlStr is not a well formed URL
   */
  public static String getUrlPrefix(URL url)
      throws MalformedURLException {
    URL url2 = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/");
    return url2.toString();
  }

  public static List<String> getUrlPrefixes(List<String> urls)
      throws MalformedURLException{
    List<String> res = new ArrayList<String>();
    for (String s : urls) {
      res.add(getUrlPrefix(s));
    }
    return res;
  }

  /**
   * @param urlStr string representation of a url
   * @return the host portion of the url
   * @throws MalformedURLException if urlStr is not a well formed URL
   */
  public static String getHost(String urlStr) throws MalformedURLException {
    URL url = new URL(urlStr);
    return url.getHost();
  }

  /**
   * @param urlStr string representation of a url
   * @return the path portion of the url
   * @throws MalformedURLException if urlStr is not a well formed URL
   */
  public static String getPath(String urlStr) throws MalformedURLException {
    URL url = new URL(urlStr);
    return url.getPath();
  }

  /**
   * @param urlStr string representation of a url
   * @return the domain portion of the hostname
   * @throws MalformedURLException if urlStr is not a well formed URL
   */
  public static String getDomain(String urlStr) throws MalformedURLException {
    String host = getHost(urlStr);
    int pos = host.indexOf('.');
    if (pos == -1 || pos == (host.length() - 1)) {
      return host;
    } else {
      return host.substring(pos + 1);
    }
  }

  /** Reconstructs the URL the client used to make the request, using
   * information in the HttpServletRequest object. The returned URL
   * contains a protocol, server name, port number, and server path, but it
   * does not include query string parameters.  This method duplicates the
   * deprecated method from javax.servlet.http.HttpUtils
   * @param req - a HttpServletRequest object containing the client's request
   * @return string containing the reconstructed URL
   */
  // http://hostname.com:80/mywebapp/servlet/MyServlet/a/b;c=123?d=789
  public static String getRequestURL(HttpServletRequest req) {
    String scheme = req.getScheme();             // http
    String serverName = req.getServerName();     // hostname.com
    int serverPort = req.getServerPort();        // 80
    String contextPath = req.getContextPath();   // /mywebapp
    String servletPath = req.getServletPath();   // /servlet/MyServlet
    String pathInfo = req.getPathInfo();         // /a/b;c=123
//     String queryString = req.getQueryString();          // d=789

    // Reconstruct original requesting URL
    StringBuffer sb = new StringBuffer(40);
    sb.append(scheme);
    sb.append("://");
    sb.append(serverName);
    sb.append(":");
    sb.append(serverPort);
    sb.append(contextPath);
    sb.append(servletPath);
    if (pathInfo != null) {
      sb.append(pathInfo);
    }
//     if (queryString != null) {
//       sb.append("?");
//       sb.append(queryString);
//     }
    return sb.toString();
  }

  /** Performs the bare minimum URL encoding */
  public static String minimallyEncodeUrl(String url) {
    int pos = url.indexOf(org.lockss.plugin.ArchiveMemberSpec.URL_SEPARATOR);
    if (pos > 0) {
      return minimallyEncodeUrl(url.substring(0, pos)) + url.substring(pos);
    }
    url = StringUtil.replaceString(url, " ", "%20");
    url = StringUtil.replaceString(url, "\"", "%22");
    url = StringUtil.replaceString(url, "|", "%7C");
    url = StringUtil.replaceString(url, "[", "%5B");
    url = StringUtil.replaceString(url, "]", "%5D");
    return url;
  }

  /** Performs the bare minimum URL encoding necessary to embed a string in
   * a query arg */
  public static String encodeQueryArg(String url) {
    url = StringUtil.replaceString(url, "?", "%3F");
    url = StringUtil.replaceString(url, "&", "%26");
    url = StringUtil.replaceString(url, "=", "%3D");
    return url;
  }

  /**
   * Encode a url as necessary according to the rules for each component
   * @param uri the url to encode
   * @param enc the character encoding to use
   * @return the encoded url string
   */
  public static String encodeUri(String uri, String enc)  {
    try {
      return UriUtils.encodeUri(uri, enc);
    } catch (UnsupportedEncodingException e) {
      // The system should always have the platform default
      throw new RuntimeException("Encoding (" + enc + ") unsupported", e);
    }
  }

  /**
   * decode a url using the java URLDecoder
   * @param url the url to encode
   * @param enc the encoding to use
   * @return  the unencoded string
   */
  public static String decodeUri(String url, String enc) {
    try {
      return UriUtils.decode(url, enc);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Encoding (" + enc + ") unsupported", e);
    }
  }

  /** URLencode a string */
  public static String encodeUrl(String url) {
    try {
      return URLEncoder.encode(url, Constants.ENCODING_UTF_8);
    } catch (UnsupportedEncodingException e) {
      // The system should always have UTF-8
      throw new RuntimeException("Default encoding (" +
                                 Constants.ENCODING_UTF_8 + ") unsupported",
          e);
    }
  }

  /** URLdecode a string */
  public static String decodeUrl(String url) {
    try {
      return URLDecoder.decode(url, Constants.ENCODING_UTF_8);
    } catch (UnsupportedEncodingException e) {
      // The system should always have UTF-8
      throw new RuntimeException("Default encoding (" +
                                 Constants.ENCODING_UTF_8 + ") unsupported",
          e);
    }
  }

  /** Resolve possiblyRelativeUrl relative to baseUrl.
   * @param baseUrl The base URL relative to which to resolve
   * @param possiblyRelativeUrl resolved relative to baseUrl
   * @return The URL formed by combining the two URLs
   */
  public static String resolveUri(String baseUrl, String possiblyRelativeUrl)
      throws MalformedURLException {
    return resolveUri(new URL(baseUrl), possiblyRelativeUrl, true);
  }

  /** Resolve possiblyRelativeUrl relative to baseUrl.
   * @param baseUrl The base URL relative to which to resolve
   * @param possiblyRelativeUrl resolved relative to baseUrl
   * @param minimallyEncode if true, possiblyRelativeUrl gets the bare
   * minimal URL encoding
   * @return The URL formed by combining the two URLs
   */
  public static String resolveUri(String baseUrl, String possiblyRelativeUrl,
                                  boolean minimallyEncode)
      throws MalformedURLException {
    return resolveUri(new URL(baseUrl), possiblyRelativeUrl, minimallyEncode);
  }

  public static String resolveUri(URL baseUrl, String possiblyRelativeUrl)
      throws MalformedURLException {
    return resolveUri(baseUrl, possiblyRelativeUrl, true);
  }

  public static String resolveUri(URL baseUrl, String possiblyRelativeUrl,
                                  boolean minimallyEncode)
      throws MalformedURLException {
    possiblyRelativeUrl =
        trimNewlinesAndLeadingWhitespace(possiblyRelativeUrl);
    if(DataUri.isDataUri(possiblyRelativeUrl)) {
      // don't encode & don't resolve just return it.
      return possiblyRelativeUrl;
    }
    String encodedChild = possiblyRelativeUrl;
    if (minimallyEncode) {
      encodedChild = minimallyEncodeUrl(encodedChild);
    }
    URL url;
    if (encodedChild.startsWith("?")) {
      url = new URL(baseUrl.getProtocol(), baseUrl.getHost(),
          baseUrl.getPort(), baseUrl.getPath() + encodedChild);
    } else {
      url = new URL(baseUrl, encodedChild);
    }
    return url.toString();
  }

  // URI-based versions.  These produce different (but more canonicalized)
  // results from URL, so can't be used unless/until we canonicalize all
  // the existing caches file names.

  /** Resolve possiblyRelativeUrl relative to baseUrl.
   * @param baseUrl The base URL relative to which to resolve
   * @param possiblyRelativeUrl resolved relative to baseUrl
   * @return The URL formed by combining the two URLs
   */
  public static String resolveUri0(String baseUrl, String possiblyRelativeUrl)
      throws MalformedURLException {
    baseUrl = trimNewlinesAndLeadingWhitespace(baseUrl);
    possiblyRelativeUrl =
        trimNewlinesAndLeadingWhitespace(possiblyRelativeUrl);
    String encodedBase = minimallyEncodeUrl(baseUrl);
    String encodedChild = minimallyEncodeUrl(possiblyRelativeUrl);
    try {
      java.net.URI base = new java.net.URI(encodedBase);
      java.net.URI child = new java.net.URI(encodedChild);
      java.net.URI res = resolveUri0(base, child);
      return res.toASCIIString();
    } catch (URISyntaxException e) {
      throw newMalformedURLException(e);
    }
  }

  private static MalformedURLException
  newMalformedURLException(Throwable cause) {

    MalformedURLException ex = new MalformedURLException();
    ex.initCause(cause);
    return ex;
  }

  public static String resolveUri0(URL baseUrl, String possiblyRelativeUrl)
      throws MalformedURLException {
    return resolveUri(baseUrl.toString(), possiblyRelativeUrl);
  }

  /** Resolve child relative to base */
  // This version is a wrapper for java.net.URI.resolve().  Java class has
  // two undesireable behaviors: it resolves ("http://foo.bar", "a.html")
  // to "http://foo.bara.html" (fails to supply missing "/" to base with no
  // path), and it resolves ("http://foo.bar/xxx.php", "?foo=bar") to
  // "http://foo.bar/?foo=bar" (in accordance with RFC 2396), while all the
  // browsers resolve it to "http://foo.bar/xxx.php?foo=bar" (in accordance
  // with RFC 1808).  This mimics enough of the logic of
  // java.net.URI.resolve(URI, URI) to detect those two cases, and defers
  // to the URI code for other cases.

  private static java.net.URI resolveUri0(java.net.URI base,
                                          java.net.URI child)
      throws MalformedURLException {

    // check if child is opaque first so that NPE is thrown
    // if child is null.
    if (child.isOpaque() || base.isOpaque()) {
      return child;
    }

    try {
      String scheme = base.getScheme();
      String authority = base.getAuthority();
      String path = base.getPath();
      String query = base.getQuery();
      String fragment = child.getFragment();

      // If base has null path, ensure authority is separated from path in
      // result.  (java.net.URI resolves ("http://foo.bar", "x.y") to
      // http://foo.barx.y)
      if (StringUtil.isNullString(base.getPath())) {
        path = "/";
        base = new java.net.URI(scheme, authority, path, query, fragment);
      }

      // Absolute child
      if (child.getScheme() != null)
        return child;

      if (child.getAuthority() != null) {
        // not relative, defer to URI
        return base.resolve(child);
      }

      // Fragment only, return base with this fragment
      if (child.getPath().equals("") && (child.getFragment() != null)
          && (child.getQuery() == null)) {
        if ((base.getFragment() != null)
            && child.getFragment().equals(base.getFragment())) {
          return base;
        }
        java.net.URI ru =
            new java.net.URI(scheme, authority, path, query, fragment);
        return ru;
      }

      query = child.getQuery();

      authority = base.getAuthority();

      if (StringUtil.isNullString(child.getPath())) {
        // don't truncate base path if child has no path
        path = base.getPath();
      } else if (child.getPath().charAt(0) == '/') {
        // Absolute child path
        path = child.getPath();
      } else {
        // normal relative path, defer to URI
        return base.resolve(child);
      }
      // create URI from relativized components
      java.net.URI ru =
          new java.net.URI(scheme, authority, path, query, fragment);
      return ru;
    } catch (URISyntaxException e) {
      throw newMalformedURLException(e);
    }
  }


  public static String[] supportedJSFunctions =
      {
          "newWindow",
          "popup"
      };

  /**
   * Takes a javascript url of the following formats:
   * javascript:newWindow('http://www.example.com/link3.html')
   * javascript:popup('http://www.example.com/link3.html')
   * and resolves it to a URL
   */
  public static String parseJavascriptUrl(String jsUrl) {

    int jsIdx = StringUtil.indexOfIgnoreCase(jsUrl, "javascript:");
    if (jsIdx < 0) {
      log.debug("Doesn't appear to be a javascript URL: "+jsUrl);
      return null;
    }

    int protocolEnd = jsIdx + "javascript:".length();
    int funcEnd = -1;

    for (int ix=0; ix<supportedJSFunctions.length && funcEnd==-1; ix++) {
      if (jsUrl.regionMatches(true, protocolEnd,
          supportedJSFunctions[ix], 0,
          supportedJSFunctions[ix].length())) {
        funcEnd = protocolEnd + supportedJSFunctions[ix].length();
        log.debug3("matched supported JS function "+supportedJSFunctions[ix]);
        break;
      }
    }

    if (funcEnd == -1) {
      // if we got here, there was no match
      log.debug("Can't parse js url: "+jsUrl);
      return null;
    }

    int urlStart = funcEnd+1;//+1 to skip the "("
    char firstChar = jsUrl.charAt(urlStart);

    if (firstChar == '\'') {
      urlStart++;
    }
    String url = jsUrl.substring(urlStart);
    return StringUtil.truncateAtAny(url, ")'");
  }

  // resolveUri() using HttpClient URI.  Allows all protocols (no
  // StreamHandler required), but is more picky about allowable characters,
  // and quite a bit slower.
//   /** Resolve possiblyRelativeUrl relative to baseUrl.
//    * @param baseUrl The base URL relative to which to resolve
//    * @param possiblyRelativeUrl resolved relative to baseUrl
//    * @return The URL formed by combining the two URLs
//    */
//   public static String resolveUri(String baseUrl, String possiblyRelativeUrl)
//       throws MalformedURLException {
//     try {
//       String encodedUri = minimallyEncodeUrl(possiblyRelativeUrl);
//       org.apache.commons.httpclient.URI resultURI =
// 	new org.apache.commons.httpclient.URI(encodedUri.toCharArray());
//       if (resultURI.isRelativeURI()) {
// 	//location is incomplete, use base values for defaults
// 	org.apache.commons.httpclient.URI baseURI =
// 	  new org.apache.commons.httpclient.URI(baseUrl.toCharArray());
// 	resultURI = new org.apache.commons.httpclient.URI(baseURI, resultURI);
//       }
//       return resultURI.toString();
//     } catch (URIException e) {
//       throw new MalformedURLException(e.toString());
//     }
//   }

  public static boolean isMalformedUrl(String url) {
    try {
      new URL(url);
      return false;
    } catch (MalformedURLException ex) {
      log.warning("Malformed URL "+url, ex);
      return true;
    }
  }

  public static boolean isAbsoluteUrl(String url) {
    if (url != null) {
      try {
        org.apache.commons.httpclient.URI resultURI =
            new org.apache.commons.httpclient.URI(url, true);
        return resultURI.isAbsoluteURI();
      } catch (URIException e) {
      }
    }
    return false;
  }

  /** Return true if both URLs have the same host part */
  public static boolean isSameHost(String url1, String url2) {
    try {
      return getHost(url1).equalsIgnoreCase(getHost(url2));
    } catch (MalformedURLException e) {
      log.warning("isSameHost", e);
      return false;
    }
  }

  /** Return true if <code>to</code> looks like a directory redirection
   * from <code>from</code>; <i>ie</i>, that path has had a slash appended
   * to it. */
  // XXX does this need to be insensitive to differently encoded URLs?
  public static boolean isDirectoryRedirection(String from, String to) {
    if (to.length() != (from.length() + 1)) return false;
    try {
      URL ufrom = new URL(from);
      URL uto = new URL(to);
      String toPath = uto.getPath();
      String fromPath = ufrom.getPath();
      int len = fromPath.length();
      return (
          toPath.length() == (len + 1) &&
          toPath.charAt(len) == '/' &&
          toPath.startsWith(fromPath) &&
          ufrom.getHost().equalsIgnoreCase(uto.getHost()) &&
          ufrom.getProtocol().equalsIgnoreCase(uto.getProtocol()) &&
          ufrom.getPort() == uto.getPort() &&
          StringUtil.equalStringsIgnoreCase(ufrom.getQuery(),
              uto.getQuery())

      );
    } catch (MalformedURLException e) {
      return false;
    }
  }

  /**
   * Strips the query string off of a url and returns the rest
   * @param url url string from which to remove the query
   * @return url with the query string stripped, or null if url isn't absolute
   * @throws MalformedURLException if we can't parse the url
   */
  public static String stripQuery(String url) throws MalformedURLException {
    if (url != null) {
      try {
        org.apache.commons.httpclient.URI uri =
            new org.apache.commons.httpclient.URI(url, true);
        if (uri.isAbsoluteURI()) {
          StringBuffer sb = new StringBuffer();
          sb.append(uri.getScheme().toLowerCase());
          sb.append("://");
          sb.append(uri.getHost());
          sb.append(uri.getPath());
          return sb.toString();
        }
      } catch (URIException e) {
        throw newMalformedURLException(e);
      }
    }
    return null;
  }

  /**
   * <p>Removes the protocol and its <code>://</code> component
   * from a URL stem.</p>
   * <p>Assumption: url stems always start with a protocol.</p>
   * @param url A URL stem.
   * @return A new URL stem with the protocol removed.
   */
  public static String stripProtocol(String url) {
    final String PROTOCOL_SUBSTRING = "://";
    if (url == null) return null;
    int pos = url.indexOf(PROTOCOL_SUBSTRING);
    if (pos >= 0) {
      return url.substring(pos + PROTOCOL_SUBSTRING.length());
    }
    return url;
  }

  /** Return the extension of the filename in the URL, if possible */
  public static String getFileExtension(String urlString)
      throws MalformedURLException {
    URL url = new URL(urlString);
    String filename = url.getPath();
    if (StringUtil.isNullString(filename)) {
      return null;
    }
    return FileUtil.getExtension(filename);
  }

  /** Match the scheme and host:port part of a URL */
  static Pattern SCHEME_HOST_PAT = Pattern.compile("^(\\w+://)([^/]+)");

  /** Add a subdomain to the host part of a URL
   * @param url the URL string
   * @param subdomain the subdomain to add (no trailing dot)
   * @return the URL string with the host prefixed with the subdomain
   */
  public static String addSubDomain(String url, String subdomain) {
    Matcher m = SCHEME_HOST_PAT.matcher(url);
    if (m.find()) {
      String host = m.group(2);
      if (StringUtil.startsWithIgnoreCase(host, subdomain) &&
	  '.' == host.charAt(subdomain.length())) {
	// subdomain already present
	return url;
      }
      StringBuffer sb = new StringBuffer();
      m.appendReplacement(sb, "$1" + subdomain + ".$2");
      m.appendTail(sb);
      return sb.toString();
    }
    return url;
  }

  /** Remove a subdomain from the host part of a URL
   * @param url the URL string
   * @param subdomain the (case insensitive) subdomain to remove (no
   * trailing dot)
   * @return the URL string with the subdomain removed from the beginning
   * of the host
   */
  public static String delSubDomain(String url, String subdomain) {
    Matcher m = SCHEME_HOST_PAT.matcher(url);
    if (m.find()) {
      String host = m.group(2);
      if (StringUtil.startsWithIgnoreCase(host, subdomain) &&
	  '.' == host.charAt(subdomain.length())) {
	StringBuffer sb = new StringBuffer();
	m.appendReplacement(sb, "$1" + host.substring(subdomain.length() + 1));
	m.appendTail(sb);
	return sb.toString();
      }
    }
    return url;
  }

  /**
   * <p>
   * Replaces a URL's scheme (e.g. <code>http</code> with another (e.g.
   * <code>https</code>), unless the given URL does not begin with the given
   * original scheme or is insufficiently long.
   * </p>
   *
   * @param url
   *          A URL.
   * @param from
   *          An original protocol (e.g. <code>"http"</code>)
   * @param to
   *          A destination protocol (e.g. <code>"https"</code>)
   * @return A URL string with the original protocol replaced by the destination
   *         protocol, or the original string if the given URL does not begin
   *         with the original protocol or is insufficiently long
   */
  public static String replaceScheme(String url, String from, String to) {
    int flen = from.length();
    if (StringUtil.startsWithIgnoreCase(url, from) &&
	url.length() > flen &&
	url.charAt(flen) == ':') {
      return to + url.substring(flen);
    }
    return url;
  }

  /**
   * <p>Returns a URL for a resource in a .jar on the classpath</p>
   * @param resourceName name of the resource
   * @return A new URL for the resource
   */
  public static URL getResourceUrl(String resourceName) {
    URL url = null;
    if (resourceName != null) {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      url = loader.getResource(resourceName);
      log.debug3(resourceName + " maps to " + url.toString());
    } else {
      log.error("null resourceName");
    }
    return url;
  }

  /**
   * Return a list of header fields (in the format "key;fieldValue") for conn
   * @param conn URLConnection to get headers from
   * @return list of header fields (in the format "key;fieldValue") for conn
   * @throws IllegalArgumentException if a null conn is supplied
   */
  public static List getHeaders(URLConnection conn) {
    if (conn == null) {
      throw new IllegalArgumentException("Called with null URLConnection");
    }
    List returnList = new ArrayList();
    boolean done = false;
    for(int ix=0; !done; ix++) {
      String headerField = conn.getHeaderField(ix);
      String headerFieldKey = conn.getHeaderFieldKey(ix);
      done = (headerField == null && headerFieldKey == null);
      if (!done) {
        returnList.add(headerFieldKey+";"+headerField);
      }
    }
    return returnList;
  }

  /** Return input stream for url iff 200 response code, else throw.
   * @param urlString the url
   * @return an InputStream
   * @throws IOException
   */
  public static InputStream openHttpClientInputStream(String urlString)
      throws IOException {
    log.debug2("openInputStream(\"" + urlString + "\")");
    LockssUrlConnection conn = openConnection(urlString);
    conn.setUserAgent("lockss");
    conn.execute();
    int statusCode = conn.getResponseCode();
    if(statusCode == 200) {
      return conn.getResponseInputStream();
    }
    else {
      throw new IOException("Server returned HTTP response code: " +
                            statusCode + " for URL: " + urlString);
    }
    /*
    HttpClient client = new HttpClient();
    HttpMethod method = new GetMethod(urlString);

    // Execute the method.
    int statusCode = -1;
    // execute the method.
    method.addRequestHeader(new Header("user-agent", "lockss"));
    statusCode = client.executeMethod(method);
    if (statusCode == 200) {
      InputStream ins = method.getResponseBodyAsStream();
      return ins;
    } else {
      throw new IOException("Server returned HTTP response code: " +
                            statusCode + " for URL: " + urlString);
    }
    */
  }

  /** Return input stream for url.  If url is http or https, uses Jakarta
   * HttpClient, else Java URLConnection.
   * @param urlString the url
   * @return an InputStream
   * @throws IOException
   */
  public static InputStream openInputStream(String urlString)
      throws IOException {
    if (isHttpOrHttpsUrl(urlString)) {
      return openHttpClientInputStream(urlString);
    } else {
      URL url = new URL(urlString);
      URLConnection uc = url.openConnection();
      return uc.getInputStream();
    }

  }

  /** Create and Return a LockssUrlConnection appropriate for the url
   * protocol.  If url is http or https, uses Jakarta HttpClient, else Java
   * URLConnection.
   * @param urlString the url
   * @return a LockssUrlConnection wrapper for the actual url connection
   * @throws IOException
   */
  public static LockssUrlConnection openConnection(String urlString)
      throws IOException {
    return openConnection(urlString, null);
  }

  /** Create and Return a LockssUrlConnection appropriate for the url
   * protocol.  If url is http or https, uses Jakarta HttpClient, else Java
   * URLConnection.
   * @param urlString the url
   * @param connectionPool optional connection pool
   * @return a LockssUrlConnection wrapper for the actual url connection
   * @throws IOException
   */
  public static LockssUrlConnection
  openConnection(String urlString, LockssUrlConnectionPool connectionPool)
      throws IOException {
    return openConnection(LockssUrlConnection.METHOD_GET, urlString,
        connectionPool);
  }

  /** Create and Return a LockssUrlConnection appropriate for the url
   * protocol.  If url is http or https, uses Jakarta HttpClient, else Java
   * URLConnection.
   * @param urlString the url
   * @param connectionPool optional connection pool
   * @return a LockssUrlConnection wrapper for the actual url connection
   * @throws IOException
   */
  public static LockssUrlConnection
  openConnection(int methodCode, String urlString,
                 LockssUrlConnectionPool connectionPool)
      throws IOException {
    LockssUrlConnection luc;
    if (isHttpOrHttpsUrl(urlString)) {
      if (useHttpClient) {
        HttpClient client = null;
        if (connectionPool != null) {
          client = connectionPool.getHttpClient();
        } else {
          client = new HttpClient();
        }
        luc = new HttpClientUrlConnection(methodCode, urlString, client,
            connectionPool);
      } else {
        luc = new JavaHttpUrlConnection(urlString);
      }
    } else {
      luc = new JavaUrlConnection(urlString);
    }
    return luc;
  }

//   /** Return input stream for url iff 200 response code, else throw.
//    * In Java 1.1.7, URL.openStream() returns an InputStream in some cases
//    * where it should throw, e.g., a 403 response on a filename that
//    * ends in ".txt".
//    * <br>In Java 1.3 and later this should not be necessary, as an
//    * IOException is thrown in all the right cases.  But there's no harm
//    * in continuing to use it, and it may be handy in the future.
//    * @param urlString the url
//    * @return an InputStream
//    * @throws IOException
//    */
//   public static InputStream openInputStream(String urlString)
//       throws IOException {
//     URL url = new URL(urlString);
//     URLConnection uc = url.openConnection();
//     if (!(uc instanceof HttpURLConnection)) {
//       return uc.getInputStream();
//     }
//     HttpURLConnection huc = (HttpURLConnection)uc;
//     int rc = huc.getResponseCode();
//     if (rc == HttpURLConnection.HTTP_OK) {
//       return huc.getInputStream();
//     } else {
//       throw new IOException("Server returned HTTP response code: " + rc +
// 			    " for URL: " + urlString);
//     }
//   }

}
