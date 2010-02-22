/*
 * $Id: HttpClientUrlConnection.java,v 1.30.16.1 2010-02-22 06:45:55 tlipkis Exp $
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

package org.lockss.util.urlconn;

import java.io.*;
import java.util.Properties;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.util.*;

import org.lockss.config.*;
import org.lockss.util.*;

/** Encapsulates Jakarta HttpClient method as a LockssUrlConnection.
 * Mostly simple wrapper behavior, except cross-host redirects are handled
 * because HttpClient doesn't.
 */
public class HttpClientUrlConnection extends BaseLockssUrlConnection {
  private static Logger log = Logger.getLogger("HttpClientUrlConnection");

  /* If true, the InputStream returned from getResponseInputStream() will
   * be wrapped in an EofBugInputStream */
  static final String PARAM_USE_WRAPPER_STREAM = PREFIX + "useWrapperStream";
  static final boolean DEFAULT_USE_WRAPPER_STREAM = true;

  /* If true, any connection on which credentials are set will preemptively
   * send the credentials.  If false they will be sent only after receiving
   * 401, which currently happens on every request. */
  static final String PARAM_USE_PREEMPTIVE_AUTH = PREFIX + "usePreemptiveAuth";
  static final boolean DEFAULT_USE_PREEMPTIVE_AUTH = true;

  // Set up an SSL protocol factory that accepts self-signed certificates
  static {
    Protocol easyhttps = new Protocol("https",
				      new EasySSLProtocolSocketFactory(),
				      443);
    Protocol.registerProtocol("https", easyhttps);
  }


  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
			       Configuration oldConfig,
			       Configuration.Differences diffs) {
    if (diffs.contains(PARAM_COOKIE_POLICY) ||
	diffs.contains(PARAM_SINGLE_COOKIE_HEADER)) {
      HttpParams params = DefaultHttpParams.getDefaultParams();
      if (diffs.contains(PARAM_COOKIE_POLICY)) {
	String policy = config.get(PARAM_COOKIE_POLICY, DEFAULT_COOKIE_POLICY);
	params.setParameter(HttpMethodParams.COOKIE_POLICY,
			    getCookiePolicy(policy));
      }
      if (diffs.contains(PARAM_SINGLE_COOKIE_HEADER)) {
	boolean val = config.getBoolean(PARAM_SINGLE_COOKIE_HEADER,
					DEFAULT_SINGLE_COOKIE_HEADER);
	params.setBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER,
				   val);
      }
    }
  }

  static String getCookiePolicy(String policy) {
    if (Constants.COOKIE_POLICY_RFC_2109.equalsIgnoreCase(policy)) {
      return CookiePolicy.RFC_2109;
    } else if (Constants.COOKIE_POLICY_NETSCAPE.equalsIgnoreCase(policy)) {
      return CookiePolicy.NETSCAPE;
    } else if (Constants.COOKIE_POLICY_IGNORE.equalsIgnoreCase(policy)) {
      return CookiePolicy.IGNORE_COOKIES;
    } else if (Constants.COOKIE_POLICY_COMPATIBILITY.equalsIgnoreCase(policy)) {
      return CookiePolicy.BROWSER_COMPATIBILITY;
    } else {
      log.warning("Unknown cookie policy: " + policy +
		  ", using BROWSER_COMPATIBILITY");
      return CookiePolicy.BROWSER_COMPATIBILITY;
    }
  }

  private HttpClient client;
  private HttpMethod method;
  private int methodCode;
  private int responseCode;

  /** Create a connection object, defaulting to GET method */
  public HttpClientUrlConnection(String urlString, HttpClient client)
      throws IOException {
    this(LockssUrlConnection.METHOD_GET, urlString, client);
  }

  /** Create a connection object, with specified method */
  public HttpClientUrlConnection(int methodCode, String urlString,
				 HttpClient client)
      throws IOException {
    this.urlString = urlString;
    this.client = client != null ? client : new HttpClient();
    this.methodCode = methodCode;
    method = createMethod(methodCode, urlString);
  }

  private HttpMethod createMethod(int methodCode, String urlString)
      throws IOException {
    try {
      switch (methodCode) {
      case LockssUrlConnection.METHOD_GET:
	return newLockssGetMethodImpl(urlString);
      case LockssUrlConnection.METHOD_PROXY:
	return new LockssProxyGetMethodImpl(urlString);
      }
      throw new RuntimeException("Unknown url method: " + methodCode);
    } catch (IllegalArgumentException e) {
      // HttpMethodBase throws IllegalArgumentException on illegal URLs
      // Canonicalize that to Java's MalformedURLException
      throw newMalformedURLException(urlString, e);
    } catch (IllegalStateException e) {
      // HttpMethodBase throws IllegalArgumentException on illegal protocols
      // Canonicalize that to Java's MalformedURLException
      throw newMalformedURLException(urlString, e);
    }
  }

  java.net.MalformedURLException newMalformedURLException(String msg,
 							  Throwable cause) {
    java.net.MalformedURLException e = new java.net.MalformedURLException(msg);
    e.initCause(cause);
    return e;
  }
 
  /** for testing */
  protected HttpClientUrlConnection(String urlString, HttpClient client,
				    LockssGetMethod method) {
    this.urlString = urlString;
    this.client = client;
    this.method = method;
  }

  protected LockssGetMethod newLockssGetMethodImpl(String urlString) {
      return new LockssGetMethodImpl(urlString);
  }

  public boolean isHttp() {
    return true;
  }

  /** Execute the request. */
  public void execute() throws IOException {
    assertNotExecuted();
    if (methodCode != LockssUrlConnection.METHOD_PROXY) {
      mimicSunRequestHeaders();
    }
    HostConfiguration hostConfig = client.getHostConfiguration();
    if (proxyHost != null) {
      hostConfig.setProxy(proxyHost, proxyPort);
    }
    if (localAddress != null) {
      hostConfig.setLocalAddress(localAddress.getInetAddr());
    }
    isExecuted = true;
    responseCode = executeOnce(method);
  }

  private int executeOnce(HttpMethod method) throws IOException {
    try {
      return client.executeMethod(method);
    } catch (ConnectTimeoutException e) {
      // Thrown by HttpClient if the connect timeout elapses before
      // socket.connect() returns.
      // Turn this into a non HttpClient-specific exception
      throw new ConnectionTimeoutException("Host did not respond", e);
      // XXX If socket.connect() returns an error because the underlying
      // socket connect times out, the behavior is platform dependent.  On
      // Linux, java.net.ConnectException is thrown (same as for connection
      // refused, and distunguishable only by the exception message).  On
      // OpenBSD, java.net.SocketException is thrown with a message like
      // "errno: 22, error: Invalid argument for fd: 3".  In lieu of a way
      // to reliably determine when to turn these into a
      // ConnectionTimeoutException, the solution for now is to use
      // java-level connect timeouts that are shorter than the underlying
      // socket connect timeout.
    } catch (NoHttpResponseException e) {
      // Thrown by HttpClient if the server closes the connection before
      // sending a response.
      // Turn this into a non HttpClient-specific exception
      java.net.SocketException se =
	new java.net.SocketException("Connection reset by peer");
      se.initCause(e);
      throw se;
    }
  }

  public boolean canProxy() {
    return true;
  }

  public void setRequestProperty(String key, String value) {
    assertNotExecuted();
    method.setRequestHeader(key, value);
  }

  public void addRequestProperty(String key, String value) {
    assertNotExecuted();
    method.addRequestHeader(key, value);
  }

  public void setFollowRedirects(boolean followRedirects) {
    assertNotExecuted();
    method.setFollowRedirects(followRedirects);
  }

  public void setCookiePolicy(String policy) {
    assertNotExecuted();
    HttpParams params = method.getParams();
    params.setParameter(HttpMethodParams.COOKIE_POLICY,
			getCookiePolicy(policy));
  }

  public void setCredentials(String username, String password) {
    assertNotExecuted();
    Credentials credentials = new UsernamePasswordCredentials(username,
							      password);
    HttpState state = client.getState();
    state.setCredentials(AuthScope.ANY, credentials);
    if (CurrentConfig.getBooleanParam(PARAM_USE_PREEMPTIVE_AUTH,
                                      DEFAULT_USE_PREEMPTIVE_AUTH)) {
      HttpClientParams params = client.getParams();
      params.setAuthenticationPreemptive(true);
    }
  }

  public String getResponseHeaderFieldVal(int n) {
    assertExecuted();
    try {
      return method.getResponseHeaders()[n].getValue();
    } catch (ArrayIndexOutOfBoundsException e) {
      return null;
    }
  }

  public String getResponseHeaderFieldKey(int n) {
    assertExecuted();
    try {
      return method.getResponseHeaders()[n].getName();
    } catch (ArrayIndexOutOfBoundsException e) {
      return null;
    }
  }

  public int getResponseCode() {
    assertExecuted();
    return responseCode;
  }

  public String getResponseMessage() {
    assertExecuted();
    return method.getStatusText();
  }

  public long getResponseDate() {
    assertExecuted();
    String datestr = getResponseHeaderValue("date");
    if (datestr == null) {
      return -1;
    }
    try {
      return DateUtil.parseDate(datestr).getTime();
    } catch (DateParseException e) {
      log.error("Error parsing response Date: header: " + datestr, e);
      return -1;
    }
  }

  public long getResponseContentLength() {
    assertExecuted();
    if (method instanceof LockssGetMethod) {
      LockssGetMethod getmeth = (LockssGetMethod)method;
      return getmeth.getResponseContentLength();
    }
    throw new UnsupportedOperationException(method.getClass().toString());
  }

  public String getResponseContentType() {
    return getResponseHeaderValue("Content-Type");
  }

  public String getResponseContentEncoding() {
    return getResponseHeaderValue("content-encoding");
  }

  public String getResponseHeaderValue(String name) {
    assertExecuted();
    Header header = method.getResponseHeader(name);
    return (header != null) ? header.getValue() : null;
  }

  public InputStream getResponseInputStream() throws IOException {
    assertExecuted();
    InputStream in = method.getResponseBodyAsStream();
    if (in == null) {
      // this is a normal occurrence (e.g., with 304 response)
      log.debug2("Returning null input stream");
      return null;
    }
    if (CurrentConfig.getBooleanParam(PARAM_USE_WRAPPER_STREAM,
                                      DEFAULT_USE_WRAPPER_STREAM)) {
      return new EofBugInputStream(in);
    }
    return in;
  }

  public void storeResponseHeaderInto(Properties props, String prefix) {
    // store all header properties (this is the only way to iterate)
    Header[] headers = method.getResponseHeaders();
    for (int ix = 0; ix < headers.length; ix++) {
      Header hdr = headers[ix];
      String key = hdr.getName();
      String value = hdr.getValue();
      if (value!=null) {
        // only store headers with values
        // qualify header names to avoid conflict with our properties
	String propKey = (key != null) ? key : ("header_" + ix);
	if (prefix != null) {
	  propKey = prefix + propKey;
	}
	props.setProperty(propKey, value);
      }
    }
  }

  public String getActualUrl() {
    try {
      String path = method.getPath();
      String query = method.getQueryString();
      if (!StringUtil.isNullString(query)) {
	path = path + "?" + query;
      }
      URI uri = new URI(new URI(urlString, false), path, true);
      return uri.toString();
    } catch(URIException e) {
      log.warning("getActualUrl(): ", e);
      return urlString;
    }
  }

  /** Copied from sun.net.www.protocol.http.HttpURLConnection */
  static final String ACCEPT_STRING =
    "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2";

  /** Mimic Java 1.3 HttpURLConnection default request header behavior */
  private void mimicSunRequestHeaders() {
    if (!isHeaderSet(method.getRequestHeader("Accept"))) {
      setRequestProperty("Accept", ACCEPT_STRING);
    }
    if (!isHeaderSet(method.getRequestHeader("Connection"))) {
      setRequestProperty("Connection", "keep-alive");
    }
  }

  private boolean isHeaderSet(Header hdr) {
    return (hdr == null) ? false : !StringUtil.isNullString(hdr.getValue());
  }

  /**
   * Release resources associated with this request.
   */
  public void release() {
    assertExecuted();
    method.releaseConnection();
  }

  /**
   * Abort the request.
   */
  public void abort() {
    method.abort();
  }

  /** Common interface for our methods makes testing more convenient */
  interface LockssGetMethod extends HttpMethod {
    long getResponseContentLength();
  }

  /** Same as GET method
   */
  static class LockssGetMethodImpl
      extends GetMethod implements LockssGetMethod {

    public LockssGetMethodImpl(String url) {
      super(url);
      // Establish our retry handler
//       setMethodRetryHandler(getRetryHandler());
    }
  }

  /** Extends GET method to not add any default request headers
   */
  static class LockssProxyGetMethodImpl extends LockssGetMethodImpl {

    public LockssProxyGetMethodImpl(String url) {
      super(url);
    }

    protected void addRequestHeaders(HttpState state, HttpConnection conn)
	throws IOException, HttpException {
      // Suppress this - don't want any automatic header processing when
      // acting as a proxy.
    }
  }

  /** Extension of ConnectionTimeoutException used as a wrapper for the
   * HttpClient-specific HttpConnection.ConnectionTimeoutException. */
  public class ConnectionTimeoutException
    extends LockssUrlConnection.ConnectionTimeoutException {

    public ConnectionTimeoutException(String msg) {
      super(msg);
    }
    public ConnectionTimeoutException(String msg, Throwable t) {
      super(msg, t);
    }
    public ConnectionTimeoutException(Throwable t) {
      super(t.getMessage(), t);
    }
  }
}
