/*
 * $Id: UrlUtil.java,v 1.5 2003-06-06 23:15:11 aalto Exp $
 *

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

/** Utilities for URLs
 */
public class UrlUtil {
  /**
   * The separator string for URLs.
   */
  public static final String URL_PATH_SEPARATOR = "/";
  /**
   * The separator char for URLs.
   */
  public static final char URL_PATH_SEPARATOR_CHAR = '/';

  /** Return input stream for url iff 200 response code, else throw.
   * In Java 1.1.7, URL.openStream() returns an InputStream in some cases
   * where it should throw, e.g., a 403 response on a filename that
   * ends in ".txt".
   * <br>In Java 1.3 and later this should not be necessary, as an
   * IOException is thrown in all the right cases.  But there's no harm
   * in continuing to use it, and it may be handy in the future.
   * @param urlString the url
   * @return an InputStream
   * @throws IOException
   */
  public static InputStream openInputStream(String urlString)
      throws IOException {
    URL url = new URL(urlString);
    URLConnection uc = url.openConnection();
    if (!(uc instanceof HttpURLConnection)) {
      return uc.getInputStream();
    }
    HttpURLConnection huc = (HttpURLConnection)uc;
    int rc = huc.getResponseCode();
    if (rc == HttpURLConnection.HTTP_OK) {
      return huc.getInputStream();
    } else {
      throw new IOException("Server returned HTTP response code: " + rc +
			    " for URL: " + urlString);
    }
  }

  /**
   * @param urlStr string representation of a url
   * @return urlStr up to but not including the path
   * @throws MalformedURLException if urlStr is not a well formed URL
   */
  public static String getUrlPrefix(String urlStr) throws MalformedURLException{
    URL url = new URL(urlStr);
    URL url2 = new URL(url.getProtocol(), url.getHost(), url.getPort(), "");
    return url2.toString();
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


}
