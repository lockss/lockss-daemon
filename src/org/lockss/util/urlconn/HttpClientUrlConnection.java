/*
 * $Id: HttpClientUrlConnection.java,v 1.1 2004-02-23 09:25:49 tlipkis Exp $
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
//import java.net.*;
import java.util.*;
import org.lockss.util.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.util.*;

/** Encapsulates Jakarta HttpClient method as a LockssUrlConnection.
 * Mostly simple wrapper behavior, except cross-host redirects are handled
 * because HttpClient doesn't.
 */
public class HttpClientUrlConnection extends BaseLockssUrlConnection {
  private static Logger log = Logger.getLogger("HttpClientUrlConnection");

  /** Maximum number of redirects that will be followed */
  private static final int MAX_REDIRECTS = 10;

  private HttpClient client;
  private LockssGetMethod method;
  private int responseCode;

  public HttpClientUrlConnection(String urlString, HttpClient client) {
    this.urlString = urlString;
    this.client = client != null ? client : new HttpClient();
    method = newLockssGetMethodImpl(urlString);
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
      hostConfig = new HostConfiguration();
      hostConfig.setProxy(proxyHost, proxyPort);
    }
    isExecuted = true;
    for (int retry = 1; retry < MAX_REDIRECTS; retry++) {
      if (hostConfig != null) {
	responseCode = client.executeMethod(hostConfig, method);
      } else {
	responseCode = client.executeMethod(method);
      }
      if (!isRetryNeeded(responseCode)) {
	break;
      }
    }
  }

  public boolean canProxy() {
    return true;
  }

  public void setUserAgent(String value) {
    assertNotExecuted();
    setRequestProperty("user-agent", value);
  }

  public void setRequestProperty(String key, String value) {
    assertNotExecuted();
    method.setRequestHeader(key, value);
  }

  public void setFollowRedirects(boolean followRedirects) {
    assertNotExecuted();
    method.setFollowRedirects(followRedirects);
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
    return method.getResponseContentLength();
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
    return method.getResponseBodyAsStream();
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
	String propKey = (key != null) ? (prefix + key) : (prefix + ix);
	props.setProperty(propKey, value);
      }
    }
  }

  public String getActualUrl() {
    try {
      URI uri = new URI(new URI(urlString), new URI(method.getPath()));
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
      log.debug("Redirect requested but followRedirects is disabled");
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
      log.warning("Redirected location '" + location + "' is malformed");
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
    LockssGetMethod newMeth = newLockssGetMethodImpl(newUrlString);

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
                + "' to '" + redirectUri.getEscapedURI());
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
    }

    public int getResponseContentLength() {
      return super.getResponseContentLength();
    }
  }

}
