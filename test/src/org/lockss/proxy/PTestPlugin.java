/*
 * $Id: PTestPlugin.java,v 1.10 2003-02-21 21:53:28 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.proxy;

import java.io.*;
import java.util.*;
import java.net.*;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

/**
 * Stub plugin for testing proxy.
 * Results are completely canned.
 */
class PTestPlugin {

  static class CU implements CachedUrl {
    private String url;
    private String contents = null;
    private Properties props = new Properties();

    public CU(String url) {
      this.url = url;
    }
    public CU(String url, String type, String contents) {
      this.url = url;
      setContents(contents);
      props.setProperty("Content-Type", type);
    }

    private void setContents(String s) {
      contents = s;
      props.setProperty("Content-Length", ""+s.length());
    }

    public String getUrl() {
      return url;
    }

    public boolean hasContent() {
      return contents != null;
    }

    public boolean isLeaf() {
      return true;
    }

    public int getType() {
      return CachedUrlSetNode.TYPE_CACHED_URL;
    }

    public InputStream openForReading() {
      return new StringInputStream(contents);
    }

    public Properties getProperties() {
      return props;
    }
  }

  static class AU extends NullPlugin.ArchivalUnit {
    private Hashtable map = new Hashtable();

    public String toString() {
      return "[au: " + map + "]";
    }

    private void storeCachedUrl(CachedUrl cu) {
      map.put(cu.getUrl(), cu);
    }

    public boolean containsUrl(String url) {
      return map.containsKey(url);
    }

    public CachedUrl makeCachedUrl(String url) {
      return (CachedUrl)map.get(url);
    }

    public UrlCacher makeUrlCacher(String url) {
      return (UrlCacher)map.get(url);
    }
  }

  public static ArchivalUnit makeTest() {
    AU au = new AU();
    au.storeCachedUrl(new CU("http://foo.bar/one", "text/plain",
				      "this is one text\n"));
    au.storeCachedUrl(new CU("http://foo.bar/two", "text/html",
				      "<html><h3>this is two html</h3></html>"));

    MockLockssDaemon daemon = new MockLockssDaemon(null);
    daemon.getPluginManager().registerArchivalUnit(au);
    return au;
  }
}
