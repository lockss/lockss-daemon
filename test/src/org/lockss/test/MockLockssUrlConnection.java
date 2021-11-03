/*
 * $Id$
 */

/*

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

package org.lockss.test;

import java.util.*;
import java.net.*;
import java.io.*;
import org.lockss.util.urlconn.*;


public class MockLockssUrlConnection extends BaseLockssUrlConnection {

  int responseCode;
  String responseMessage;
  Properties reqHeaders = new Properties();
  Properties respHeaders = new Properties();
  InputStream respInputStream = null;
  String respContentType;
  String respContentEncoding;
  long respDate = -1;
  long respLastModified = -1;
  boolean keepAlive = false;

  // Some tests don't need a URL
  public MockLockssUrlConnection() throws IOException {
    this("http://example.com/dummy/url");
  }

  public MockLockssUrlConnection(String url) throws IOException {
    super(url);
  }

  public void setURL(String url) throws IOException {
    this.urlString = url;
    this.url = new URL(url);
  }

  public boolean isHttp() {
    return true;
  }

  boolean followRedirects = true;
  public void setFollowRedirects(boolean follow) {
    this.followRedirects = follow;
  }

  public void execute() throws IOException {
    isExecuted = true;
  }

  public boolean canProxy() {
    return false;
  }

  public void setProxy(String host, int port) {
    throw new UnsupportedOperationException();
  }

  public void setKeepAlive(boolean val) {
    keepAlive = val;
  }

  public boolean getKeepAlive() {
    return keepAlive;
  }

  public void setHeaderCharset(String charset) {
  }

  public void setRequestProperty(String key, String value) {
    reqHeaders.setProperty(key.toLowerCase(), value);
  }

  public String getRequestProperty(String key) {
    return reqHeaders.getProperty(key.toLowerCase());
  }

  public Properties getRequestProperties() {
    return reqHeaders;
  }

  public void setRequestEntity(String entity) {
    throw new UnsupportedOperationException();
  }

  public int getResponseCode() {
    return responseCode;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  public void setResponseMessage(String message) {
    this.responseMessage = message;
  }

  public String getResponseHeaderFieldVal(int n) {
    throw new UnsupportedOperationException();
  }

  public String getResponseHeaderFieldKey(int n) {
    throw new UnsupportedOperationException();
  }

  public void setResponseDate(long date) {
    this.respDate = date;
  }

  public long getResponseDate() {
    if (respDate != -1) {
      return respDate;
    }
    throw new UnsupportedOperationException();
  }

  public long getResponseContentLength() {
    throw new UnsupportedOperationException();
  }

  public void setResponseContentType(String type) {
    this.respContentType = type;
  }

  public void setResponseContentEncoding(String encoding) {
    this.respContentEncoding = encoding;
  }

  public String getResponseContentType() {
    return respContentType;
  }

  public String getResponseContentEncoding() {
    return respContentEncoding;
  }

  public String getResponseHeaderValue(String name) {
    return respHeaders.getProperty(name.toLowerCase());
  }

  public void setResponseHeader(String name, String value) {
    respHeaders.setProperty(name.toLowerCase(), value);
  }

  public InputStream getResponseInputStream() throws IOException {
    return respInputStream;
  }

  public void setResponseInputStream(InputStream stream) {
    this.respInputStream = stream;
  }

  public void storeResponseHeaderInto(Properties props, String prefix) {
    for (Iterator iter = respHeaders.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (prefix != null) {
	key = prefix + key;
      }
      props.setProperty(key, respHeaders.getProperty(key.toLowerCase()));
    }
  }

  
  public void setResponseLastModified(long lastModified) {
    this.respLastModified = lastModified;
  }
  
  public long getResponseLastModified() {
    return respLastModified;
  }

  boolean released = false;
  public void release() {
    released = true;
  }
  public void abort() {
  }

}
