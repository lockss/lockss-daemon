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

package org.lockss.proxy;

import java.io.*;
import java.util.*;
import java.net.*;
import java.security.MessageDigest;
import java.math.BigInteger;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.servlet.*;
import org.lockss.rewriter.*;
import org.lockss.extractor.*;

/**
 * Stub plugin for testing proxy.
 * Results are completely canned.
 */
public class PTestPlugin {

  static class CU implements CachedUrl {
    private String url;
    private String contents = null;
    private CIProperties props = new CIProperties();

    public CU(String url) {
      this.url = url;
    }
    public CU(String url, String type, String contents) {
      this.url = url;
      setContents(contents);
      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, type);
    }

    public ArchivalUnit getArchivalUnit() {
      return null;
    }

    private void setContents(String s) {
      contents = s;
      props.setProperty("Content-Length", ""+s.length());
    }

    public String getUrl() {
      return url;
    }

    public void setOption(String option, String val) {
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

    public CachedUrl getCuVersion(int version) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public CachedUrl[] getCuVersions() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public CachedUrl[] getCuVersions(int maxVersions) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public int getVersion() {
      return 1;
    }

    public InputStream getUnfilteredInputStream() {
      return new StringInputStream(contents);
    }

    public InputStream getUnfilteredInputStream(HashedInputStream.Hasher hasher) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public InputStream getUncompressedInputStream() {
      return getUnfilteredInputStream();
    }

    public InputStream getUncompressedInputStream(HashedInputStream.Hasher
						  hasher) {
      return getUnfilteredInputStream(hasher);
    }

    public InputStream openForHashing() {
      return getUnfilteredInputStream();
    }

    public InputStream openForHashing(HashedInputStream.Hasher hasher) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public Reader openForReading() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public void delete() throws UnsupportedOperationException, IOException {
      throw new UnsupportedOperationException("Not implemented");
    }

    public LinkRewriterFactory getLinkRewriterFactory() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public long getContentSize() {
      return contents.length();
    }

    public String getContentType(){
      return props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
    }

    public String getEncoding(){
      return Constants.DEFAULT_ENCODING; // XXX
    }

    public CIProperties getProperties() {
      return props;
    }

    public void addProperty(String key, String value) {
      props.setProperty(key, value);
    }


    public void release() {
    }

    public ArticleMetadataExtractor getArticleMetadataExtractor() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public CachedUrl getArchiveMemberCu(ArchiveMemberSpec ams) {
      return null;
    }

    @Override
    public boolean isArchiveMember() {
      return false;
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

    public org.lockss.plugin.CachedUrlSet getAuCachedUrlSet() {
      return null;
    }

    public CachedUrl makeCachedUrl(String url) {
      return (CachedUrl)map.get(url);
    }

    public UrlCacher makeUrlCacher(String url) {
      return (UrlCacher)map.get(url);
    }
  }

  public static String testUrls[] = {
    "http://foo.bar/one",
    "http://foo.bar/two",
  };

  public static String testTypes[] = {
    "text/plain",
    "text/html",
  };

  public static String testContents[] = {
    "this is one text\n",
    "<html><h3>this is two html</h3></html>",
  };

  public static ArchivalUnit makeTestAu() {
    AU au = new AU();
    for (int i = 0; i < testUrls.length; i++) {
      au.storeCachedUrl(new CU(testUrls[i], testTypes[i], testContents[i]));
    }
    return au;
  }
}
