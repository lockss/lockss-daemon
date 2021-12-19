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

package org.lockss.util.urlconn;

import java.util.*;
import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;

/** Encapsulates and abstracts a URL connection, using either Sun's
 * URLConnection or Jakarta HttpClient.  Operations not supported by a
 * particular protocol hander throw UnsupportedOperationException
 */
public interface LockssUrlConnection {
  static final String PREFIX = Configuration.PREFIX + "urlconn.";

  /** Cookie parsing policy.  One of the following strings:
   *  <ul>
   *   <li><tt>COMPATIBILITY</tt>: compatible with the common cookie management
   *   practices (even if they are not 100% standards compliant)
   *   <li><tt>NETSCAPE_DRAFT</tt>: Netscape cookie draft compliant
   *   <li><tt>RFC2109</tt>: RFC2109 compliant
   *  </ul>
   */
  static final String PARAM_COOKIE_POLICY = PREFIX + "cookiePolicy";
  static final String DEFAULT_COOKIE_POLICY = "COMPATIBILITY";

  /** All cookies sent in one header if true */
  static final String PARAM_SINGLE_COOKIE_HEADER =
    PREFIX + "singleCookieHeader";
  static final boolean DEFAULT_SINGLE_COOKIE_HEADER = true;

  /** Send GET */
  public static int METHOD_GET = 1;
  /** Send GET but don't mess with request headers */
  public static int METHOD_PROXY = 2;
  /** Send POST */
  public static int METHOD_POST = 3;

  /** Return true if an http or https connection */
  public boolean isHttp();

  /** Perform the action (connect to server, retrieve content, etc.)  Until
   * this has been called no references to the response may be made.  Once
   * this has been called no mmodifications to the request may be made.
   */
  public void execute() throws IOException;

  /** Returns false until the request has been executed, true after (even
   * if unsuccessful). */
  public boolean isExecuted();

  /** Return the URL
   * @return the URL
   */
  public String getURL();

  /** Return true if this connection can be proxied */
  public boolean canProxy();

  /** Set the proxy host and port.
   * @throws UnsupportedOperationException if canProxy() returns false
   */
  public void setProxy(String host, int port) throws CantProxyException ;

  /** Set the ProtocolSocketFactory to be used with this connection */
  public void setSecureSocketFactory(LockssSecureSocketFactory sockFact);

  /** Return true iff the server is authenticated */
  public boolean isAuthenticatedServer();

  /** Set the local address to be used when creating connections. */
  public void setLocalAddress(IPAddr localAddress);

  /** Set the charset to be used to encode and decode HTTP request and
   * response headers */
  public void setHeaderCharset(String charset);

  /** Set the user-agent request header */
  public void setUserAgent(String value);

  /** Set the If-Modified-Since request header */
  public void setIfModifiedSince(long time);

  /** Set the If-Modified-Since request header */
  public void setIfModifiedSince(String time);

  /** Set a request header to a time value */
  public void setRequestPropertyDate(String key, long time);

  /** Set a request header, overwriting any previous value */
  public void setRequestProperty(String key, String value);

  /** Add a request header */
  public void addRequestProperty(String key, String value);

  /** Set the followRedirects flag. */
  public void setFollowRedirects(boolean followRedirects);

  /** Set the cookie policy to "rfc2109", "netscape" or "compatibility". */
  public void setCookiePolicy(String policy);

  /** Set the SO_KEEPALIVE flags for the socket factory. */
  public void setKeepAlive(boolean val);

  /** Add a cookie to the request.  Cookies are normally processed
   * automatically; this is used only to send a cookie that wasn't
   * originally recieved in a Set-Cookie: response header.  The cookie's
   * domain is set to the hostname in the URL, the path is set to
   * <code>/</code>;.
   * @param name the cookie name
   * @param value the cookie value
   */
  public void addCookie(String name, String value);

  /** Add a cookie to the request.  Cookies are normally processed
   * automatically; this is used only to send a cookie that wasn't
   * originally recieved in a Set-Cookie: response header.  The cookie's
   * domain is set to the hostname in the URL.
   * @param path the cookie path
   * @param name the cookie name
   * @param value the cookie value
   */
  public void addCookie(String path, String name, String value);

  /** Add a cookie to the request.  Cookies are normally processed
   * automatically; this is used only to send a cookie that wasn't
   * originally recieved in a Set-Cookie: response header.
   * @param domain the cookie domain
   * @param path the cookie path
   * @param name the cookie name
   * @param value the cookie value
   */
  public void addCookie(String domain, String path, String name, String value);

  /** Set the username/password for the connection. */
  public void setCredentials(String username, String password);

  /** Set the request entity (e.g., for POST). */
  default public void setRequestEntity(String entity) {
    throw new UnsupportedOperationException();
  }

  /** Return numeric response code */
  public int getResponseCode();

  /** Return response message */
  public String getResponseMessage();

  /** Return the value of the nth header field */
  public String getResponseHeaderFieldVal(int n);

  /** Return the key of the nth header field */
  public String getResponseHeaderFieldKey(int n);

  /** Return the Date: from the response header */
  public long getResponseDate();

  /** Return the last-modified: from the response header */
  public long getResponseLastModified();

  /** Return the value of the content-length response header, as an int.
   * @return  the content length, or -1 if not known.
   */
  public long getResponseContentLength();

  /**
   * Returns the value of the content-type response header.
   * @return  the content type, or null if not known.
   */
  public String getResponseContentType();

  /**
   * Returns the value of the content-encoding response header.
   * @return  the content encoding, or null if not known.
   */
  public String getResponseContentEncoding();

  /**
   * Returns the value of the specified response header field.
   * @param name the name of a header field.
   * @return the value of the named header field, or null if there is no
   * such field in the header.
   */
  public String getResponseHeaderValue(String name);

  /** Return an input stream open on the response content */
  public InputStream getResponseInputStream() throws IOException;

  /** Return an input stream open on the response content, uncompressed if
   * necessary */
  public InputStream getUncompressedResponseInputStream() throws IOException;

  /** Store all the response headers into the supplied properties.
   * @param props the Properties to store into
   * @param prefix String to prepend onto each header field name to create
   * a property key.
   */
  public void storeResponseHeaderInto(Properties props, String prefix);

  /** Return the actual URL, after any redirects (if followRedirects is
   * true) */
  public String getActualUrl();



  /**
   * Release resources associated with this request.  Closing the
   * InputStream returned by getResponseInputStream() does this
   * automatically, but this should be called if no input stream is
   * obtained (<i>eg</i>, in case of error responses) */
  public void release();

  /**
   * Abort the request.
   */
  public void abort();

  /** Exception thrown by setProxy if the connection type doesn't support
   * proxying */
  static class CantProxyException extends IOException {
    public CantProxyException(String msg) {
      super(msg);
    }
    public CantProxyException() {
      super();
    }
  }

  /** Exception thrown if a timeout occured while opening the socket. */
  public class ConnectionTimeoutException extends IOException {
    public ConnectionTimeoutException(String msg) {
      super(msg);
    }
    public ConnectionTimeoutException(String msg, Throwable t) {
      super(msg);
      initCause(t);
    }
    public ConnectionTimeoutException(Throwable t) {
      super();
      initCause(t);
    }
  }
}
