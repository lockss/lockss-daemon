/*
 * $Id: CuUrlResource.java,v 1.3 2004-01-22 02:07:40 tlipkis Exp $
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

package org.lockss.jetty;

import java.io.*;
import java.net.*;
import java.security.Permission;
import org.mortbay.util.*;
import org.lockss.util.*;

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
   * Returns true if the respresenetd resource is a container/directory.
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

    Resource res = newResource(path);
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
}
