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
//HC3 import org.apache.commons.httpclient.*;
//HC3 import org.apache.commons.httpclient.methods.*;
//HC3 import org.apache.commons.httpclient.params.*;
//HC3 import org.apache.commons.httpclient.auth.*;
//HC3 import org.apache.commons.httpclient.util.*;

/**
 * Mock implementation of Jakarta HttpMethod
 */
//HC3 public class MockHttpMethod implements HttpMethod {
public class MockHttpMethod {

//HC3   public String getName() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   /** @deprecated */
//HC3   public HostConfiguration getHostConfiguration() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void setPath(String path) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public String getPath() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public URI getURI() throws URIException {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void setURI(URI uri) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   /**
//HC3    * @deprecated
//HC3    */
//HC3   public void setStrictMode(boolean strictMode) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3 
//HC3   /**
//HC3    * @deprecated
//HC3    */
//HC3   public boolean isStrictMode() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void setRequestHeader(String headerName, String headerValue) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void setRequestHeader(Header header) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void addRequestHeader(String headerName, String headerValue) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void addRequestHeader(Header header) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public Header getRequestHeader(String headerName) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void removeRequestHeader(String headerName) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void removeRequestHeader(Header header) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public boolean getFollowRedirects() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void setFollowRedirects(boolean followRedirects) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void setQueryString(String queryString) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void setQueryString(NameValuePair[] params) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public String getQueryString() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public Header[] getRequestHeaders() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public Header[] getRequestHeaders(String headerName) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public boolean validate() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public int getStatusCode() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public String getStatusText() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public Header[] getResponseHeaders() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public Header getResponseHeader(String headerName) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public Header[] getResponseHeaders(String headerName) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public Header[] getResponseFooters() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public Header getResponseFooter(String footerName) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public byte[] getResponseBody() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public String getResponseBodyAsString() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public InputStream getResponseBodyAsStream() throws IOException {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public boolean hasBeenUsed() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public int execute(HttpState state, HttpConnection connection)
//HC3       throws HttpException, IOException {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   /** @deprecated */
//HC3   public void recycle() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void abort() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void releaseConnection() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void addResponseFooter(Header footer) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public StatusLine getStatusLine() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public boolean getDoAuthentication() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void setDoAuthentication(boolean doAuthentication) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public HttpMethodParams getParams() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public void setParams(final HttpMethodParams params) {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public AuthState getHostAuthState() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public AuthState getProxyAuthState() {
//HC3     throw new UnsupportedOperationException();
//HC3   }
//HC3 
//HC3   public boolean isRequestSent() {
//HC3     throw new UnsupportedOperationException();
//HC3   }

}
