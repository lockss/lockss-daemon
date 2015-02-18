/*
 * $Id$
 */

/*

  Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

/* This code is adapred from 
 * @(#)SystemResourceURLConnection.java	1.9 03/12/19
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 */

package org.lockss.util;

import java.net.*;
import java.io.*;
import java.security.*;
// import sun.net.www.MessageHeader;
// import sun.applet.AppletClassLoader;

/** resource://re.source.name URLs search the classpath for the named
 * resource, open it and return a proxy for that URLConnection.
 */
public class ResourceURLConnection extends URLConnection {
  Logger log = Logger.getLogger("ResourceURLConnection");

  URLConnection proxiedConnection;

  public ResourceURLConnection(URL url)
      throws MalformedURLException, IOException  {
    super(url);
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    String path = url.getPath();
    URL proxyUrl = loader.getResource(path);
    log.debug2("proxyUrl for: " + url.toString() + " is " + proxyUrl);
    if (proxyUrl == null) {
      throw new FileNotFoundException(path);
    }
    proxiedConnection = proxyUrl.openConnection();
  }
    
  public void connect() throws IOException {
    proxiedConnection.connect();
  }

  public Object getContent() throws IOException {
    return proxiedConnection.getContent();
  }

  public String getContentType() {
    return proxiedConnection.getContentType();
  }

  public InputStream getInputStream() throws IOException {
    return proxiedConnection.getInputStream();
  }

  public String getHeaderField(String name) {
    return proxiedConnection.getHeaderField(name);
  }

  public Permission getPermission() throws IOException {
    return proxiedConnection.getPermission();
  }
}
