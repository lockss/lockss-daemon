/*
 * $Id: UrlUtil.java,v 1.18 2004-03-16 23:01:40 tlipkis Exp $
 *

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.*;
import java.net.*;
import javax.servlet.http.HttpServletRequest;
import org.lockss.util.urlconn.*;
import org.lockss.daemon.Configuration;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

/** Utilities for URLs and URLConnections
 */
public class UrlUtil {
  private static Logger log = Logger.getLogger("UrlUtil");
  /**
   * The separator string for URLs.
   */
  public static final String URL_PATH_SEPARATOR = "/";
  /**
   * The separator char for URLs.
   */
  public static final char URL_PATH_SEPARATOR_CHAR = '/';

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

  /** Return true if an http: or https: url */
  // XXX does this need to trim blanks?
  public static boolean isHttpUrl(String url) {
    return StringUtil.startsWithIgnoreCase(url, "http:") ||
      StringUtil.startsWithIgnoreCase(url, "https:");
  }

  /**
   * @param urlStr string representation of a url
   * @return urlStr up to but not including the path
   * @throws MalformedURLException if urlStr is not a well formed URL
   */
  public static String getUrlPrefix(String urlStr)
      throws MalformedURLException{
    URL url = new URL(urlStr);
    URL url2 = new URL(url.getProtocol(), url.getHost(), url.getPort(), "");
    return url2.toString();
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

  /** Resolve possiblyRelativeUrl relative to baseUrl.
   * @param baseUrl The base URL relative to which to resolve
   * @param possiblyRelativeUrl resolved relative to baseUrl
   * @return The URL formed by combining the two URLs
   */
  public static String resolveUri(String baseUrl, String possiblyRelativeUrl)
      throws MalformedURLException {
    possiblyRelativeUrl =
      StringUtil.replaceString(possiblyRelativeUrl.trim(), " ", "%20");
    possiblyRelativeUrl =
      StringUtil.replaceString(possiblyRelativeUrl.trim(), "\"", "%22");
    try {
      org.apache.commons.httpclient.URI resultURI =
	new org.apache.commons.httpclient.URI(possiblyRelativeUrl.toCharArray());
      if (resultURI.isRelativeURI()) {
	//location is incomplete, use base values for defaults
	org.apache.commons.httpclient.URI baseURI =
	  new org.apache.commons.httpclient.URI(baseUrl.toCharArray());
	resultURI = new org.apache.commons.httpclient.URI(baseURI, resultURI);
      }
      return resultURI.toString();
    } catch (URIException e) {
      throw new MalformedURLException(e.toString());
    }
  }

  public static boolean isAbsoluteUrl(String url) {
    if (url != null) {
      try {
	org.apache.commons.httpclient.URI resultURI =
	  new org.apache.commons.httpclient.URI(url.toCharArray());
	return resultURI.isAbsoluteURI();
      } catch (URIException e) {
      }
    }
    return false;
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
	  new org.apache.commons.httpclient.URI(url.toCharArray());
	if (uri.isAbsoluteURI()) {
	  StringBuffer sb = new StringBuffer();
	  sb.append(uri.getScheme());
	  sb.append("://");
	  sb.append(uri.getHost());
	  sb.append(uri.getPath());
	  return sb.toString();
	} 
      } catch (URIException e) {
	throw new MalformedURLException(e.toString());
      }
    }
    return null;
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
    HttpClient client = new HttpClient();
    HttpMethod method = new GetMethod(urlString);

    // Execute the method.
    int statusCode = -1;
    // retry up to 2 times.
    int attempt = 1;
    while (true) {
      log.debug3("try " + attempt);
      try {
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
      } catch (HttpRecoverableException e) {
	if (attempt++ < 2) {
	  log.warning("Recoverable error: " + e.getMessage());
	} else {
	  log.warning("Too many recoverable errors, giving up: " +
		      e.getMessage());
	  throw e;
	}
      } catch (IOException e) {
	throw e;
      }
    }
  }

  /** Return input stream for url.  If url is http or https, uses Jakarta
   * HttpClient, else Java URLConnection.
   * @param urlString the url
   * @return an InputStream
   * @throws IOException
   */
  public static InputStream openInputStream(String urlString)
      throws IOException {
    if (isHttpUrl(urlString)) {
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
    if (isHttpUrl(urlString)) {
      boolean useHttpClient =
	Configuration.getBooleanParam("org.lockss.UrlUtil.useHttpClient",
				      true);
      if (useHttpClient) {
	HttpClient client = null;
	if (connectionPool != null) {
	  client = connectionPool.getHttpClient();
	} else {
	  client = new HttpClient();
	}
	luc = new HttpClientUrlConnection(methodCode, urlString, client);
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
