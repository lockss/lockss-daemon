/*
 * $Id: HttpClientUrlConnection.java,v 1.17 2005-07-25 01:21:06 tlipkis Exp $
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
import java.util.*;
import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.util.*;
import org.apache.commons.httpclient.cookie.*;

/** Encapsulates Jakarta HttpClient method as a LockssUrlConnection.
 * Mostly simple wrapper behavior, except cross-host redirects are handled
 * because HttpClient doesn't.
 */
public class HttpClientUrlConnection extends BaseLockssUrlConnection {
  private static Logger log = Logger.getLogger("HttpClientUrlConnection");

  /* If true, the InputStream returned from getResponseInputStream() will
   * be wrapped in an EofMonitoringInputStream */
  static final String PARAM_USE_WRAPPER_STREAM = PREFIX + "useWrapperStream";
  static final boolean DEFAULT_USE_WRAPPER_STREAM = true;


  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
			       Configuration oldConfig,
			       Configuration.Differences diffs) {
    if (diffs.contains(PARAM_COOKIE_POLICY)) {
      String policy = config.get(PARAM_COOKIE_POLICY, DEFAULT_COOKIE_POLICY);
      if ("rfc2109".equalsIgnoreCase(policy)) {
	CookiePolicy.setDefaultPolicy(CookiePolicy.RFC2109);
      } else if ("netscape".equalsIgnoreCase(policy)) {
	CookiePolicy.setDefaultPolicy(CookiePolicy.NETSCAPE_DRAFT);
      } else {		//  if ("compatibility".equalsIgnoreCase(policy)) {
	CookiePolicy.setDefaultPolicy(CookiePolicy.COMPATIBILITY);
      }
    }
  }

  /** Maximum number of redirects that will be followed */
  static final int MAX_REDIRECTS = 10;

  private static MethodRetryHandler retryHandler = null;

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
      throw new java.net.MalformedURLException(urlString);
    } catch (IllegalStateException e) {
      // HttpMethodBase throws IllegalArgumentException on illegal protocols
      // Canonicalize that to Java's MalformedURLException
      throw new java.net.MalformedURLException(urlString);
    }
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

  /** Execute the request.  HttpClient doesn't handle cross-host redirects,
      so handle them here. */
  public void execute() throws IOException {
    assertNotExecuted();
    mimicSunRequestHeaders();
    HostConfiguration hostConfig = null;
    if (proxyHost != null) {
      hostConfig = method.getHostConfiguration();
      hostConfig.setProxy(proxyHost, proxyPort);
    }
    isExecuted = true;
    for (int retry = 1; retry < MAX_REDIRECTS; retry++) {
      responseCode = executeOnce(hostConfig, method);
      if (!isRetryNeeded(responseCode)) {
	break;
      }
    }
  }

  private int executeOnce(HostConfiguration hostConfig, HttpMethod method)
      throws IOException {
    try {
      if (hostConfig != null) {
	return client.executeMethod(hostConfig, method);
      } else {
	return client.executeMethod(method);
      }
    } catch (HttpRecoverableException e) {
      // Information-losing wrapper for lots of different exceptions within
      // HttpClient methods.  Try to turn it back into the real exception.
      throw exceptionFromRecoverableException(e);
    } catch (HttpConnection.ConnectionTimeoutException e) {
      // Thrown by HttpClient if the connect timeout elapses before
      // socket.connect() returns.
      // Turn this into a non HttpClient-specific exception
      throw new ConnectionTimeoutException(e);
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
    }
  }

  IOException exceptionFromRecoverableException(HttpRecoverableException re) {
    String msg = re.getMessage();
    if (msg == null) {
      return re;
    }
    IOException e = getExceptionFromMsg(msg);
    if (e != null) {
      return e;
    }
    return re;
  }

  /** Create an instance of the exception named in the message.  The new
   * exception's stack trace will be from here, which is misleading, but
   * the original exception's stack trace has already been lost anyway,
   * when the HttpRecoverableException was thrown. */
  IOException getExceptionFromMsg(String msg) {
    // Pretty cheesy to do it this way, but a general mechanism would
    // require reflection to call one-arg constructor, and hopefully the
    // need for this will go away in a future release of HttpClient

    String newMsg = null;
    int pos = msg.indexOf(": ");
    if (pos >= 0 && (pos + 2) < msg.length()) {
      newMsg = msg.substring(pos + 2, msg.length());
    }
    if (msg.startsWith("java.io.InterruptedIOException")) {
      return new java.io.InterruptedIOException(newMsg);
    }
    if (msg.startsWith("java.net.BindException")) {
      return new java.net.BindException(newMsg);
    }
    if (msg.startsWith("java.net.ConnectException")) {
      return new java.net.ConnectException(newMsg);
    }
    if (msg.startsWith("java.net.NoRouteToHostException")) {
      return new java.net.NoRouteToHostException(newMsg);
    }
    if (msg.startsWith("java.net.ProtocolException")) {
      return new java.net.ProtocolException(newMsg);
    }
    if (msg.startsWith("java.net.UnknownHostException")) {
      return new java.net.UnknownHostException(newMsg);
    }
    return null;
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
      return DateParser.parseDate(datestr).getTime();
    } catch (DateParseException e) {
      log.error("Error parsing response Date: header: " + datestr, e);
      return -1;
    }
  }

  public int getResponseContentLength() {
    assertExecuted();
    if (method instanceof LockssGetMethod) {
      LockssGetMethod getmeth = (LockssGetMethod)method;
      return getmeth.getResponseContentLength();
    }
    throw new UnsupportedOperationException();
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
    if (Configuration.getBooleanParam(PARAM_USE_WRAPPER_STREAM,
				      DEFAULT_USE_WRAPPER_STREAM)) {
      return new EofMonitoringInputStream(in);
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
      URI uri = new URI(new URI(urlString), new URI(path));
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
    if (!isHeaderSet(method.getRequestHeader("Connetion"))) {
      setRequestProperty("Connection", "keep-alive");
    }
  }

  private boolean isHeaderSet(Header hdr) {
    return (hdr == null) ? false : !StringUtil.isNullString(hdr.getValue());
  }

  // Handle cross host redirections, which HttpClient doesn't.
  // Code adapted from HttpMethodBase.
  private boolean isRetryNeeded(int statusCode) {
    switch (statusCode) {
    case HttpStatus.SC_MOVED_TEMPORARILY:
    case HttpStatus.SC_MOVED_PERMANENTLY:
    case HttpStatus.SC_SEE_OTHER:
    case HttpStatus.SC_TEMPORARY_REDIRECT:
      return processRedirectResponse();
    default:
      return false;
    }
  }

  private boolean processRedirectResponse() {
    if (!method.getFollowRedirects()) {
      log.debug3("Redirect requested but followRedirects is disabled");
      return false;
    }
    //get the location header to find out where to redirect to
    String location = getResponseHeaderValue("location");
    if (location == null) {
      // got a redirect response, but no location header
      log.error("Received redirect response " + responseCode
		+ " but no location header");
      return false;
    }
    if (log.isDebug2()) {
      log.debug2("Redirect requested to location '" + location + "'");
    }
    //rfc2616 demands the location value be a complete URI
    //Location       = "Location" ":" absoluteURI
    URI redirectUri = null;
    URI currentUri = null;

    try {
      currentUri = new URI(urlString);
      redirectUri = new URI(location.toCharArray());
      if (redirectUri.isRelativeURI()) {
	if (method.isStrictMode()) {
	  log.warning("Redirected location '" + location 
		      + "' is not acceptable in strict mode");
	  return false;
	} else { 
	  //location is incomplete, use current values for defaults
	  log.debug("Redirect URI is not absolute - parsing as relative");
	  redirectUri = new URI(currentUri, redirectUri);
	}
      }
    } catch (URIException e) {
      log.warning("Redirected location '" + location + "' is malformed", e);
      return false;
    }

    //check for redirect to a different protocol, host or port
    try {
      checkValidRedirect(currentUri, redirectUri);
    } catch (HttpException ex) {
      //LOG the error and let the client handle the redirect
      log.warning(ex.getMessage());
      return false;
    }

    //update the current location with the redirect location.

    String newUrlString = redirectUri.getEscapedURI();
    HttpMethod newMeth;
    try {
      newMeth = createMethod(methodCode, newUrlString);
    } catch (IOException e) {
      log.warning("Redirected location '" + location + "' is malformed", e);
      return false;
    }

    Header[] rhdrs = method.getRequestHeaders();
    for (int ix = 0; ix < rhdrs.length; ix++) {
      Header hdr = rhdrs[ix];
      if (!"host".equalsIgnoreCase(hdr.getName())) {
	newMeth.setRequestHeader(hdr);
      }
    }
    method.releaseConnection();

    urlString = newUrlString;
    method = newMeth;
//    setQueryString(redirectUri.getEscapedQuery());

    if (log.isDebug3()) {
      log.debug3("Redirecting from '" + currentUri.getEscapedURI()
                + "' to '" + redirectUri.getEscapedURI() + "'");
    }
    
    return true;
  }

  private static void checkValidRedirect(URI currentUri, URI redirectUri)
      throws HttpException {
    String oldProtocol = currentUri.getScheme();
    String newProtocol = redirectUri.getScheme();
    if (!oldProtocol.equals(newProtocol)) {
      throw new HttpException("Redirect from protocol " + oldProtocol
			      + " to " + newProtocol + " is not supported");
    }
  }

  /**
   * Release resources associated with this request.
   */
  public void release() {
    assertExecuted();
    method.releaseConnection();
  }

  /** Return a MethodRetryHandler that says to retry even if the request
   * has been set.  (Default behavior is not to.)  This is necessary
   * because, if we try to reuse a connection that the server has closed,
   * the error won't occur until flushRequestOutputStream() is called.
   * This safe because we only handle GET requests.  (Wouldn't be safe For
   * PUT, POST, etc. because server *might* have received request.  How to
   * tell that's not what really happened?) */

  // retryHandler is static because we only need one.  No locking needed,
  // as no harm if two threads create one, but be careful to put it in the
  // right state before updating the variable.
  private static MethodRetryHandler getRetryHandler() {
    if (retryHandler == null) {
      LockssMethodRetryHandler h = new LockssMethodRetryHandler();
      h.setRequestSentRetryEnabled(true);
      retryHandler = h;
    }
    return retryHandler;
  }

  /** Subinterface adding missing method(s) to HttpMethod */
  interface LockssGetMethod extends HttpMethod {
    int getResponseContentLength();
  }

  /** Subclass used to access useful protected members of GetMethod (which
   * all seems like they should be public) */
  static class LockssGetMethodImpl
      extends GetMethod implements LockssGetMethod {

    public LockssGetMethodImpl(String url) {
      super(url);
      // Establish our retry handler
      setMethodRetryHandler(getRetryHandler());
    }

    public int getResponseContentLength() {
      return super.getResponseContentLength();
    }
  }

  /** Subclass used to access useful protected members of GetMethod (which
   * all seems like they should be public) */
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

  /** Derived from DefaultMethodRetryHandler, but suppresses retries for
   * certain exceptions */
  static class LockssMethodRetryHandler implements MethodRetryHandler {
    private int retryCount;
    private boolean requestSentRetryEnabled;
    
    public LockssMethodRetryHandler() {
      this.retryCount = 3;
      this.requestSentRetryEnabled = false;
    }
    
    public boolean retryMethod(HttpMethod method,
			       HttpConnection connection,
			       HttpRecoverableException recoverableException,
			       int executionCount,
			       boolean requestSent) {

      // Check if it's a data (socket) timeout
      String msg = recoverableException.getMessage();
      if (msg != null &&
	  msg.startsWith("java.io.InterruptedIOException")) {
	// These take a long time to happen, so retrying is probably not a
	// good idea.  Retrying may also result in a different error;
	// better to tell the user about this one.  (Wouldn't it be nice if
	// HttpClient passed the actual exception along, instead of just
	// its message?)
	return false;
      }
      return ((!requestSent || requestSentRetryEnabled) &&
	      (executionCount <= retryCount));
    }

    public void setRequestSentRetryEnabled(boolean requestSentRetryEnabled) {
      this.requestSentRetryEnabled = requestSentRetryEnabled;
    }

    public void setRetryCount(int retryCount) {
      this.retryCount = retryCount;
    }
  }

  /** Extension of ConnectionTimeoutException used as a wrapper for the
   * HttpClient-specific HttpConnection.ConnectionTimeoutException. */
  public class ConnectionTimeoutException
    extends LockssUrlConnection.ConnectionTimeoutException {
    private Exception nestedException;

    public ConnectionTimeoutException(String msg) {
      super(msg);
    }
    public ConnectionTimeoutException
      (HttpConnection.ConnectionTimeoutException e) {
      super(e.getMessage());
      nestedException = e;
    }

    /** Stack trace of nestedException is more interesting and correct than
     * ours */
    public void printStackTrace() {
      if (nestedException == null) super.printStackTrace();
      else nestedException.printStackTrace();
    }

    public void printStackTrace(java.io.PrintStream s) {
      if (nestedException == null) super.printStackTrace(s);
      else nestedException.printStackTrace(s);
    }

    public void printStackTrace(java.io.PrintWriter s) {
      if (nestedException == null) super.printStackTrace(s);
      else nestedException.printStackTrace(s);
    }
  }
  /** Stream wrapper that doesn't call the underlying stream after it has
   * signalled EOF.  HttpClient sometimes automatically closes the input
   * stream when it reaches EOF, and if a BufferedInputStream is used to
   * read from it, it might call available(), etc. after the stream has
   * been (automatically) closed. */
  static class EofMonitoringInputStream extends InputStream {
    private InputStream in;
    private boolean EOF = false;

    EofMonitoringInputStream(InputStream in) {
      this.in = in;
    }
    public int read() throws IOException {
      if (EOF) return -1;
      return check(in.read());
    }
    public int read(byte b[]) throws IOException {
      if (EOF) return -1;
      return check(read(b, 0, b.length));
    }
    public int read(byte b[], int off, int len) throws IOException {
      if (EOF) return -1;
      return check(in.read(b, off, len));
    }
    public long skip(long n) throws IOException {
      if (EOF) return 0;
      return checkSkip(in.skip(n));
    }
    public int available() throws IOException {
      if (EOF) return 0;
      return in.available();
    }
    public void close() throws IOException {
      in.close();
    }
    public synchronized void mark(int readlimit) {
    }
    public synchronized void reset() throws IOException {
      throw new IOException("mark/reset not supported");
    }
    public boolean markSupported() {
      return false;
    }
    int check(int n) {
      if (n < 0) {
 	EOF = true;
      }
      return n;
    }
    long checkSkip(long n) {
      if (n <= 0) {
 	EOF = true;
      }
      return n;
    }
  }
}
