/*
 * $Id: PTestPlugin.java,v 1.2 2002-08-13 02:20:48 tal Exp $
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

  static class PCachedUrl implements CachedUrl {
    private String url;
    private String contents = null;
    private Properties props = new Properties();

    public PCachedUrl(String url) {
      this.url = url;
    }
    public PCachedUrl(String url, String type, String contents) {
      this.url = url;
      setContents(contents);
      props.setProperty("Content-Type", type);
    }

    public void setContents(String s) {
      contents = s;
      props.setProperty("Content-Length", ""+s.length());
    }

    public String toString() {
      return url;
    }

    public boolean exists() {
      return contents != null;
    }

    public boolean shouldBeCached() {
      return false;;
    }

    public InputStream openForReading() {
      return new StringInputStream(contents);
    }

    public Properties getProperties() {
      return props;
    }

    public void storeContent(InputStream input,
			     Properties headers) throws IOException{
    }

    public InputStream getUncachedInputStream() {
      return null;
    }
  
    public Properties getUncachedProperties() {
      return null;
    }
  }

  static class PCachedUrlSet implements CachedUrlSet {
    private Hashtable map = new Hashtable();

    public String toString() {
      return "[cus: " + map + "]";
    }

    void storeCachedUrl(CachedUrl cu) {
      map.put(cu.toString(), cu);
    }

    public void addToList(CachedUrlSetSpec spec) {
    }

    public boolean removeFromList(CachedUrlSetSpec spec) {
      return false;
    }

    public boolean memberOfList(CachedUrlSetSpec spec) {
      return false;
    }

    public Enumeration listEnumeration() {
      return null;
    }

    public boolean memberOfSet(String url) {
      return map.containsKey(url);
    }

    public CachedUrlSetHasher getContentHasher(MessageDigest hasher) {
      return null;
    }

    public CachedUrlSetHasher getNameHasher(MessageDigest hasher) {
      return null;
    }

    public Enumeration flatEnumeration() {
      return null;
    }

    public Enumeration treeEnumeration() {
      return null;
    }

    public long hashDuration() {
      return 0;
    }

    public long duration(long elapsed, boolean success) {
      return 0;
    }

    public CachedUrl makeCachedUrl(String url) {
      return (CachedUrl)map.get(url);
    }

    public UrlCacher makeUrlCacher(String url) {
      return (UrlCacher)map.get(url);
    }
  }

  public static CachedUrlSet makeTest() {
    PCachedUrlSet cus = new PCachedUrlSet();
    cus.storeCachedUrl(new PCachedUrl("http://foo.bar/one", "text/plain",
				      "this is one text\n"));
    cus.storeCachedUrl(new PCachedUrl("http://foo.bar/two", "text/html",
				      "<html><h3>this is two html</h3></html>"));
    org.lockss.plugin.Plugin.registerCachedUrlSet(cus);
    return cus;
  }


}
