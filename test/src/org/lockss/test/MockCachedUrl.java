/*
 * $Id: MockCachedUrl.java,v 1.43.4.1 2009-11-03 23:44:56 edwardsb1 Exp $
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

package org.lockss.test;

import java.io.*;
import java.math.*;
import java.util.*;

import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.rewriter.*;
import org.lockss.extractor.*;

/**
 * This is a mock version of <code>CachedUrl</code> used for testing
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class MockCachedUrl implements CachedUrl {
  Logger log = Logger.getLogger("MockCachedUrl");
  private ArrayList versions;
  private ArchivalUnit au;
  private String url;
  private InputStream cachedIS;
  private CIProperties cachedProp;

  private boolean isLeaf = true;

  private boolean doesExist = false;
  private String content = null;
  private long contentSize = -1;
  private Reader reader = null;
  private String cachedFile = null;
  private boolean isResource;
  private int version = 0;
  private LinkRewriterFactory lrf = null;
  private MetadataExtractor metadataExtractor = null;

  public MockCachedUrl(String url) {
    this.versions = new ArrayList();
    this.url = url;
  }

  public MockCachedUrl(String url, ArchivalUnit au) {
    this(url);
    this.au = au;
  }

  /**
   * Construct a mock cached URL that is backed by a file.
   *
   * @param url
   * @param file The name of the file to load.
   * @param isResource If true, load the file name as a
   * resource.  If false, load as a file.
   */
  public MockCachedUrl(String url, String file, boolean isResource) {
    this(url);
    this.isResource = isResource;
    cachedFile = file;
  }

  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  public String getUrl() {
    return url;
  }

  public CachedUrl getCuVersion(int version) {
    if (version == 0) {
      return this;
    } else if (versions.isEmpty()) {
      throw new UnsupportedOperationException("No versions.");
    } else {
      return (CachedUrl)versions.get(version); 
    }
  }
  
  public CachedUrl[] getCuVersions() {
    return getCuVersions(Integer.MAX_VALUE);
  }

  public CachedUrl[] getCuVersions(int maxVersions) {
    int min = Math.min(maxVersions, versions.size() + 1);
    // Always supply this as the current version
    CachedUrl[] retVal = new CachedUrl[min];
    retVal[0] = this;
    if (min > 1) {
      System.arraycopy((CachedUrl[])versions.toArray(new CachedUrl[min]),
                       0, retVal, 1, min - 1);
    }
    return retVal;
  }

  public void setVersion(int version) {
    this.version = version;
  }
  
  public int getVersion() {
    return version;
  }
  
  public CachedUrl addVersion(String content) {
    // Special case: If this is the first version, alias for 'addContent'
    if (this.content == null) {
      this.content = content;
      return this;
    } else {
      MockCachedUrl cus = new MockCachedUrl(url, au);
      cus.content = content;
      cus.version = versions.size() + 1;
      versions.add(cus);
      return cus;
    }
  }

  public Reader openForReading() {
    if (content != null) {
      return new StringReader(content);
    }
    if (reader != null) {
      return reader;
    }

    return new StringReader("");
  }

  public LinkRewriterFactory getLinkRewriterFactory() {
    return lrf;
  }

  public void setLinkRewriterFactory(LinkRewriterFactory lrf) {
    this.lrf = lrf;
  }


  public boolean hasContent() {
    return doesExist || content != null;
  }

  public boolean isLeaf() {
    return isLeaf;
  }

  public void setIsLeaf(boolean isLeaf) {
    this.isLeaf = isLeaf;
  }

  public int getType() {
    return CachedUrlSetNode.TYPE_CACHED_URL;
  }

  public void setExists(boolean doesExist) {
    this.doesExist = doesExist;
  }

  // Read interface - used by the proxy.

  public InputStream getUnfilteredInputStream() {
    try {
      if (cachedFile != null) {
        if (isResource) {
          return ClassLoader.getSystemClassLoader().
            getResourceAsStream(cachedFile);
        } else {
          return new FileInputStream(cachedFile);
        }
      }
    } catch (IOException ex) {
      return null;
    }
    if (content != null) {
      return new StringInputStream(content);
    }
    return cachedIS;
  }

  public InputStream openForHashing() {
    String contentType = getContentType();
    InputStream is = null;
    // look for a FilterFactory
    if (au != null) {
      FilterFactory fact = au.getHashFilterFactory(contentType);
      if (fact != null) {
        InputStream unfis = getUnfilteredInputStream();
        if (log.isDebug3()) {
          log.debug3("Filtering " + contentType +
                     " with " + fact.getClass().getName());
        }
        try {
          return fact.createFilteredInputStream(au, unfis, getEncoding());
        } catch (PluginException e) {
          IOUtil.safeClose(unfis);
          throw new RuntimeException(e);
        }
      }
    }
    return getUnfilteredInputStream();
  }

  public long getContentSize() {
    if (contentSize != -1) {
      return contentSize;
    }
    if (cachedFile != null) {
      if (isResource) {
        InputStream in =
          ClassLoader.getSystemClassLoader(). getResourceAsStream(cachedFile);
        try {
          return in.skip(Long.MAX_VALUE);
        } catch (IOException e) {
          return 100;
        } finally {
          IOUtil.safeClose(in);
        }
      } else {
        return new File(cachedFile).length();
      }
    }

    return content == null ? 0 : content.length();
  }

  public CIProperties getProperties(){
    return cachedProp;
  }

  public String getContentType(){
    if (cachedProp == null) return null;
    return cachedProp.getProperty(PROPERTY_CONTENT_TYPE);
  }

  public String getEncoding(){
    return Constants.DEFAULT_ENCODING;
  }

    // Write interface - used by the crawler.

  public void storeContent(InputStream input, CIProperties headers)
      throws IOException {
    cachedIS = input;
    cachedProp = headers;
  }

  public MetadataExtractor getMetadataExtractor() {
    return metadataExtractor;
  }

  //mock specific acessors

  public void setInputStream(InputStream is){
    cachedIS = is;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setContentSize(long size) {
    this.contentSize = size;
  }

  public void setReader(Reader reader) {
    this.reader = reader;
  }

  public void setProperties(CIProperties prop){
    cachedProp = prop;
  }

  public void setProperty(String key, String val) {
    if (cachedProp == null) cachedProp = new CIProperties();
    cachedProp.put(key, val);
  }

  public void setMetadataExtractor(MetadataExtractor me) {
    metadataExtractor = me;
  }

  public void release() {
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(url.length()+17);
    sb.append("[MockCachedUrl: ");
    sb.append(url);
    sb.append("]");
    return sb.toString();
  }
}
