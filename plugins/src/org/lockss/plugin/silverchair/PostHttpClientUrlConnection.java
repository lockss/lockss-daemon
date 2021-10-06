/*
 * $Id: HttpClientUrlConnection.java 43090 2015-07-08 21:07:40Z clairegriffin $
 *

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.silverchair;

import java.io.*;
import java.util.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.protocol.*;
import org.apache.commons.httpclient.util.*;
import org.apache.commons.collections4.map.*;

import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/** Encapsulates Jakarta HttpClient method as a LockssUrlConnection with POST.
 * Mostly simple wrapper behavior, except cross-host redirects are handled
 * because HttpClient doesn't.
 */
public class PostHttpClientUrlConnection extends BaseLockssUrlConnection {
  private static Logger log = Logger.getLogger("PostHttpClientUrlConnection");

  /* Accept header value.  Can be overridden by plugin. */
  static final String PARAM_ACCEPT_HEADER = PREFIX + "acceptHeader";
  static final String DEFAULT_ACCEPT_HEADER =
    "text/html, image/gif, image/jpeg; q=.2, */*; q=.2";

  /** Repeated response headers normally get combined on receipe into a
   * single header with a comma-separated value.  Headers in this list do
   * not get that treatment - the value in the last occurrence of the
   * header is used. */
  static final String PARAM_SINGLE_VALUED_HEADERS =
    PREFIX + "singleValuedHeaders";
  static final List DEFAULT_SINGLE_VALUED_HEADERS = Collections.emptyList();

  /* If true, the InputStream returned from getResponseInputStream() will
   * be wrapped in an EofBugInputStream */
  static final String PARAM_USE_WRAPPER_STREAM = PREFIX + "useWrapperStream";
  static final boolean DEFAULT_USE_WRAPPER_STREAM = true;

  /* If true, any connection on which credentials are set will preemptively
   * send the credentials.  If false they will be sent only after receiving
   * 401, which currently happens on every request. */
  static final String PARAM_USE_PREEMPTIVE_AUTH = PREFIX + "usePreemptiveAuth";
  static final boolean DEFAULT_USE_PREEMPTIVE_AUTH = true;

  /** Choices for trustworthiness required of server. */
  public enum ServerTrustLevel {Trusted, SelfSigned, Untrusted}

  /** Determines the required trustworthiness of HTTPS server certificates.
   *
   * <dl><lh>Set to one of:</lh>
   *
   * <dt>Trusted</dt><dd>Server certificate must be signed by a known CA.</dd>
   *
   * <dt>SeflSigned</dt><dd>Server certificate must be self-signed or signed by a known CA.</dd>
   *
   * <dt>Untrusted</dt><dd>Any server certificate will be accepted.</dd>
   * </dl>
   */
  static final String PARAM_SERVER_TRUST_LEVEL = PREFIX + "serverTrustLevel";
  public static final ServerTrustLevel DEFAULT_SERVER_TRUST_LEVEL =
    ServerTrustLevel.Untrusted;

  /** Send POST */
  public static final int METHOD_POST = 3;

  /** Key used in HttpParams to communicate so_keepalive value to
   * LockssDefaultProtocolSocketFactory */
  public static String SO_KEEPALIVE = "lockss_so_keepalive";

  // Set up a flexible SSL protocol factory.  It doesn't work to set the
  // Protocol in the HostConfiguration - HttpClient.executeMethod()
  // overwrites it.  So we must communicate the per-host policies to a
  // global factory.
  static DispatchingSSLProtocolSocketFactory DISP_FACT =
    new DispatchingSSLProtocolSocketFactory();
  static {
    // Install our http factory
    Protocol http_proto =
      new Protocol("http",
                   LockssDefaultProtocolSocketFactory.getSocketFactory(),
                   80);
    Protocol.registerProtocol("http", http_proto);

    // Install our https factory
    Protocol https_proto = new Protocol("https", DISP_FACT, 443);
    Protocol.registerProtocol("https", https_proto);

    DISP_FACT.setDefaultFactory(getDefaultSocketFactory(DEFAULT_SERVER_TRUST_LEVEL));
  }

  private static ServerTrustLevel serverTrustLevel;
  private static String acceptHeader = DEFAULT_ACCEPT_HEADER;
  private static Set<String> singleValuedHdrs =
    new HashSet(DEFAULT_SINGLE_VALUED_HEADERS);

  private static SecureProtocolSocketFactory
    getDefaultSocketFactory(ServerTrustLevel stl) {
    switch (stl) {
      case Trusted:
        return new SSLProtocolSocketFactory();
      case SelfSigned:
        return new EasySSLProtocolSocketFactory();
      case Untrusted:
      default:
        return new PermissiveSSLProtocolSocketFactory();
    }
  }

  private HttpClient client;
  private HttpMethod method;
  private int methodCode;
  private LockssUrlConnectionPool connectionPool;
  private int responseCode;

  /** Create a connection object, with specified method */
  public PostHttpClientUrlConnection(int methodCode, String urlString,
                                     HttpClient client,
                                     LockssUrlConnectionPool connectionPool)
    throws IOException {
    super(urlString);
    this.client = client != null ? client : new HttpClient();
    this.methodCode = methodCode;
    method = createMethod(methodCode, urlString);
    this.connectionPool = connectionPool;
  }


  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
                               Configuration oldConfig,
                               Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      acceptHeader = config.get(PARAM_ACCEPT_HEADER, DEFAULT_ACCEPT_HEADER);

      Set<String> set = new HashSet();
      for (String s : (List<String>)config.getList(PARAM_SINGLE_VALUED_HEADERS,
                                                    DEFAULT_SINGLE_VALUED_HEADERS)) {
        set.add(s.toLowerCase());
      }
      singleValuedHdrs = set;

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
      ServerTrustLevel stl =
	(ServerTrustLevel)config.getEnum(ServerTrustLevel.class,
                                          PARAM_SERVER_TRUST_LEVEL,
                                          DEFAULT_SERVER_TRUST_LEVEL);
      DISP_FACT.setDefaultFactory(getDefaultSocketFactory(stl));
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

  private HttpMethod createMethod(int methodCode, String urlString)
    throws IOException {
    String u_str = urlString;
    try {
      if(log.isDebug2())
        log.debug2("in:"+ urlString + " isAscii:"+ StringUtil.isAscii(urlString));
      if(!StringUtil.isAscii(urlString)) {
        if(log.isDebug2()) log.debug2("in:" + u_str);
        u_str = UrlUtil.encodeUri(urlString, Constants.ENCODING_UTF_8);
        if(log.isDebug2()) log.debug2("out:" + u_str);
      }
      switch (methodCode) {
        case LockssUrlConnection.METHOD_GET:
          return newLockssGetMethodImpl(u_str);
        case LockssUrlConnection.METHOD_PROXY:
          return new LockssProxyGetMethodImpl(u_str);
        case METHOD_POST:
          return new LockssPostMethodImpl(u_str);
      }
      throw new RuntimeException("Unknown url method: " + methodCode);
    } catch (IllegalArgumentException e) {
      // HttpMethodBase throws IllegalArgumentException on illegal URLs
      // Canonicalize that to Java's MalformedURLException
      throw newMalformedURLException(u_str, e);
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
    } else {
      hostConfig.setProxyHost(null);
    }
    if (localAddress != null) {
      hostConfig.setLocalAddress(localAddress.getInetAddr());
    } else {
      hostConfig.setLocalAddress(null);
    }
    if (sockFact != null) {
      SecureProtocolSocketFactory hcSockFact =
        sockFact.getHttpClientSecureProtocolSocketFactory();
      String host = url.getHost();
      int port = url.getPort();
      if (port <= 0) {
        port = UrlUtil.getDefaultPort(url.getProtocol().toLowerCase());
      }
      DISP_FACT.setFactory(host, port, hcSockFact);
      // XXX Would like to check after connection is made that cert check
      // was actually done, but there's no good way to get to the socket or
      // SSLContect, etc.
      isAuthenticatedServer = sockFact.requiresServerAuth();
    }
    isExecuted = true;
    responseCode = executeOnce(method);
  }

  private int executeOnce(HttpMethod method) throws IOException {
    try {
      return client.executeMethod(method);
    } catch (ConnectTimeoutException /*| java.net.SocketTimeoutException*/ e) {
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

  public void setKeepAlive(boolean val) {
    assertNotExecuted();
    HttpParams params = method.getParams();
    params.setBooleanParameter(SO_KEEPALIVE, val);
    // method params don't current work, set in connection pool also,
    // though that won't affect already-open sockets
    connectionPool.setKeepAlive(val);
  }

  public void addCookie(String domain, String path, String name, String value) {
    assertNotExecuted();
    HttpState state = client.getState();
    Cookie cook = new Cookie(domain, name, value, path, null, false);
    state.addCookie(cook);
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

    /** method for passing through the post content */
  public void setRequestEntity(RequestEntity entity) {
    assertNotExecuted();
    if(method instanceof PostMethod) {
      ((PostMethod) method).setRequestEntity(entity);
    }
  }

  HttpClient getClient() {
    return client;
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

  public void setRequestEntity(String entity) {
    throw new UnsupportedOperationException();
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

  public long getResponseLastModified() {
    String datestr = getResponseHeaderValue("last-modified");
    if (datestr == null) {
      return -1;
    }
    try {
      return DateUtil.parseDate(datestr).getTime();
    } catch (DateParseException e) {
      log.error("Error parsing response last-modified: header: " + datestr, e);
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
    // first collect all values for any repeated headers.
    MultiValueMap<String,String> map = new MultiValueMap<String,String>();
    Header[] headers = method.getResponseHeaders();
    for (int ix = 0; ix < headers.length; ix++) {
      Header hdr = headers[ix];
      String key = hdr.getName();
      String value = hdr.getValue();
      if (value!=null) {
        // only store headers with values
        // qualify header names to avoid conflict with our properties
        if (key == null) {
          key = "header_" + ix;
        }
        String propKey = (prefix == null) ? key : prefix + key;
        if (!singleValuedHdrs.isEmpty() &&
            singleValuedHdrs.contains(key.toLowerCase())) {
          map.remove(propKey);
        }
        map.put(propKey, value);
      }
    }
    // now combine multiple values into comma-separated string
    for (String key : map.keySet()) {
      Collection<String> val = map.getCollection(key);
      props.setProperty(key, ((val.size() > 1)
                                ? StringUtil.separatedString(val, ",")
                                : CollectionUtil.getAnElement(val)));
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

  /** Mimic Java 1.3 HttpURLConnection default request header behavior */
  private void mimicSunRequestHeaders() {
    if (!isHeaderSet(method.getRequestHeader("Accept"))) {
      setRequestProperty("Accept", acceptHeader);
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

  /**
   * same as the POST method
   */
  interface LockssPostMethod extends HttpMethod {
    long getResponseContentLength();
  }

  static class LockssPostMethodImpl
    extends PostMethod implements LockssPostMethod {

    public LockssPostMethodImpl(String url) {
      super(url);
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
