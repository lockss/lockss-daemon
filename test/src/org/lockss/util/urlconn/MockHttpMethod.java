/*
 * $Id$
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
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.util.*;

/**
 * Mock implementation of Jakarta HttpMethod
 */
public class MockHttpMethod implements HttpMethod {

  public String getName() {
    throw new UnsupportedOperationException();
  }

  /** @deprecated */
  public HostConfiguration getHostConfiguration() {
    throw new UnsupportedOperationException();
  }

  public void setPath(String path) {
    throw new UnsupportedOperationException();
  }

  public String getPath() {
    throw new UnsupportedOperationException();
  }

  public URI getURI() throws URIException {
    throw new UnsupportedOperationException();
  }

  public void setURI(URI uri) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public void setStrictMode(boolean strictMode) {
    throw new UnsupportedOperationException();
  }


  /**
   * @deprecated
   */
  public boolean isStrictMode() {
    throw new UnsupportedOperationException();
  }

  public void setRequestHeader(String headerName, String headerValue) {
    throw new UnsupportedOperationException();
  }

  public void setRequestHeader(Header header) {
    throw new UnsupportedOperationException();
  }

  public void addRequestHeader(String headerName, String headerValue) {
    throw new UnsupportedOperationException();
  }

  public void addRequestHeader(Header header) {
    throw new UnsupportedOperationException();
  }

  public Header getRequestHeader(String headerName) {
    throw new UnsupportedOperationException();
  }

  public void removeRequestHeader(String headerName) {
    throw new UnsupportedOperationException();
  }

  public void removeRequestHeader(Header header) {
    throw new UnsupportedOperationException();
  }

  public boolean getFollowRedirects() {
    throw new UnsupportedOperationException();
  }

  public void setFollowRedirects(boolean followRedirects) {
    throw new UnsupportedOperationException();
  }

  public void setQueryString(String queryString) {
    throw new UnsupportedOperationException();
  }

  public void setQueryString(NameValuePair[] params) {
    throw new UnsupportedOperationException();
  }

  public String getQueryString() {
    throw new UnsupportedOperationException();
  }

  public Header[] getRequestHeaders() {
    throw new UnsupportedOperationException();
  }

  public Header[] getRequestHeaders(String headerName) {
    throw new UnsupportedOperationException();
  }

  public boolean validate() {
    throw new UnsupportedOperationException();
  }

  public int getStatusCode() {
    throw new UnsupportedOperationException();
  }

  public String getStatusText() {
    throw new UnsupportedOperationException();
  }

  public Header[] getResponseHeaders() {
    throw new UnsupportedOperationException();
  }

  public Header getResponseHeader(String headerName) {
    throw new UnsupportedOperationException();
  }

  public Header[] getResponseHeaders(String headerName) {
    throw new UnsupportedOperationException();
  }

  public Header[] getResponseFooters() {
    throw new UnsupportedOperationException();
  }

  public Header getResponseFooter(String footerName) {
    throw new UnsupportedOperationException();
  }

  public byte[] getResponseBody() {
    throw new UnsupportedOperationException();
  }

  public String getResponseBodyAsString() {
    throw new UnsupportedOperationException();
  }

  public InputStream getResponseBodyAsStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  public boolean hasBeenUsed() {
    throw new UnsupportedOperationException();
  }

  public int execute(HttpState state, HttpConnection connection)
      throws HttpException, IOException {
    throw new UnsupportedOperationException();
  }

  /** @deprecated */
  public void recycle() {
    throw new UnsupportedOperationException();
  }

  public void abort() {
    throw new UnsupportedOperationException();
  }

  public void releaseConnection() {
    throw new UnsupportedOperationException();
  }

  public void addResponseFooter(Header footer) {
    throw new UnsupportedOperationException();
  }

  public StatusLine getStatusLine() {
    throw new UnsupportedOperationException();
  }

  public boolean getDoAuthentication() {
    throw new UnsupportedOperationException();
  }

  public void setDoAuthentication(boolean doAuthentication) {
    throw new UnsupportedOperationException();
  }

  public HttpMethodParams getParams() {
    throw new UnsupportedOperationException();
  }

  public void setParams(final HttpMethodParams params) {
    throw new UnsupportedOperationException();
  }

  public AuthState getHostAuthState() {
    throw new UnsupportedOperationException();
  }

  public AuthState getProxyAuthState() {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestSent() {
    throw new UnsupportedOperationException();
  }

}
