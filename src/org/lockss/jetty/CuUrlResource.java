/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.jetty;

import java.io.*;
import java.net.*;
import java.util.*;

import org.mortbay.http.*;
import org.mortbay.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;

/** URLResource tailored to LOCKSS CuUrls
 */
public class CuUrlResource extends URLResource {
  public CuUrlResource() {
    super(getDummyUrl(), null);
  }

  protected CuUrlResource(URL url, URLConnection connection) {
    super(url, connection);
  }

  // Need to give super constructer a real URL, but it will never be used.
  static URL getDummyUrl() {
    try {
      return new URL("file:dummy");
    } catch (MalformedURLException e) {
      throw new RuntimeException("Couldn't create dummy URL: " + e);
    }
  }

  /**
   * Returns true if the represented resource is a container/directory.
   * LOCKSS CUs should never be treated as directories
   */
  public boolean isDirectory() {
    return false;
  }

  /**
   * Returns an output stream to the resource.  Can't write to LOCKSS CU
   */
  public OutputStream getOutputStream() throws IOException {
    throw new IOException("Output not supported");
  }

  /**
   * Returns the resource contained inside the current resource with the
   * given name.  Don't do usual context base path adding here, just make a
   * new resource from the path
   */
  public Resource addPath(String path)
      throws IOException,MalformedURLException {
    if (path==null)
      return null;

    Resource res;
    if (org.lockss.util.StringUtil.
	startsWithIgnoreCase(path, CuUrl.PROTOCOL_COLON)) {
      res = new CuUrlResource(new URL(path), null);
    } else {
      res = newResource(path);
    }
    return res;
  }

  /** Supress normal URLEncoding */
  public String encode(String uri) {
    return uri;
  }

  public boolean equals( Object o) {
    return (o instanceof CuUrlResource) &&
      UrlUtil.equalUrls(_url,((CuUrlResource)o)._url);
  }

  // Inherited method returns count from URLConnection, which (incorrently)
  // is an int.  This version handles cached files larger than 2GB
  public long length() {
    try {
      return Long.parseLong(getProperty(HttpFields.__ContentLength));
    } catch (Exception e) {
      return -1;
    }
  }

  public String getProperty(String name) {
    if (checkConnection()) {
      return _connection.getHeaderField(name);
    }
    return null;
  }

  public Map<String,List<String>> getPropertyMap() {
    if (checkConnection()) {
      return _connection.getHeaderFields();
    }
    return null;
  }
}
