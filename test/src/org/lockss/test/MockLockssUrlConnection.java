/*
 * $Id: MockLockssUrlConnection.java,v 1.1 2004-02-23 09:25:49 tlipkis Exp $
 */

/*

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

package org.lockss.test;

import java.util.*;
import java.net.*;
import java.io.*;
import org.lockss.util.urlconn.*;


public class MockLockssUrlConnection extends BaseLockssUrlConnection {

  String url;
  List headerFieldKeys = null;
  List headerFields = null;

  public MockLockssUrlConnection() {
  }

  public boolean isHttp() {
    return false;
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

  public void setUserAgent(String value) {
    throw new UnsupportedOperationException();
  }

  public void setRequestProperty(String key, String value) {
    throw new UnsupportedOperationException();
  }

  public int getResponseCode() {
    throw new UnsupportedOperationException();
  }

  public String getResponseMessage() {
    throw new UnsupportedOperationException();
  }

  public long getResponseDate() {
    throw new UnsupportedOperationException();
  }

  public int getResponseContentLength() {
    throw new UnsupportedOperationException();
  }

  public String getResponseContentType() {
    throw new UnsupportedOperationException();
  }

  public String getResponseContentEncoding() {
    throw new UnsupportedOperationException();
  }

  public String getResponseHeaderValue(String name) {
    throw new UnsupportedOperationException();
  }

  public InputStream getResponseInputStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  public void storeResponseHeaderInto(Properties props, String prefix) {
    throw new UnsupportedOperationException();
  }

  public void release() {
    throw new UnsupportedOperationException();
  }
}
