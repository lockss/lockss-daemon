/*
 * $Id: MockCachedUrl.java,v 1.24 2004-09-01 20:14:44 smorabito Exp $
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

import java.io.*;
import java.math.*;

import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * This is a mock version of <code>CachedUrl</code> used for testing
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class MockCachedUrl implements CachedUrl {
  private CachedUrlSet cus;
  private String url;
  private InputStream cachedIS;
  private CIProperties cachedProp;

  private boolean doesExist = false;
  private String content = null;
  private Reader reader = null;
  private File cachedFile = null;

  public MockCachedUrl(String url) {
    this.url = url;
  }

  public MockCachedUrl(String url, CachedUrlSet cus) {
    this(url);
    this.cus = cus;
  }

  /**
   * Construct a mock cached URL that is backed by a file.
   */
  public MockCachedUrl(String url, String file) {
    this(url);
    cachedFile = new File(file);
    if (!cachedFile.exists()) {
      throw new RuntimeException("Unable to load file: " + file);
    }
  }

  public ArchivalUnit getArchivalUnit() {
    if (cus!=null) {
      return cus.getArchivalUnit();
    } else {
      return null;
    }
  }

  public String getUrl() {
    return url;
  }

  public Reader openForReading() {
    if (content != null) {
      return new StringReader(content);
    }
    if (reader != null) {
      return reader;
    }

    throw new UnsupportedOperationException("Not implemented");
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public boolean hasContent() {
    return doesExist;
  }

  public boolean isLeaf() {
    return true;
  }

  public int getType() {
    return CachedUrlSetNode.TYPE_CACHED_URL;
  }

  public void setExists(boolean doesExist) {
    this.doesExist = doesExist;
  }

  // Read interface - used by the proxy.

  public InputStream getUnfilteredInputStream() {
    if (cachedFile != null) {
      try {
	return new FileInputStream(cachedFile);
      } catch (IOException ex) {
	return null;
      }
    }
    if (content != null) {
      return new StringInputStream(content);
    }
    return cachedIS;
  }

  public InputStream openForHashing() {
    return getUnfilteredInputStream();
  }

  public byte[] getUnfilteredContentSize() {
    String content;
    if (this.content == null) {
      content = "";
    } else {
      content = this.content;
    }
    return (new BigInteger(Integer.toString(content.length()))).toByteArray();
  }


  public CIProperties getProperties(){
    return cachedProp;
  }

    // Write interface - used by the crawler.

  public void storeContent(InputStream input, CIProperties headers)
      throws IOException {
    cachedIS = input;
    cachedProp = headers;
  }

  //mock specific acessors

  public void setInputStream(InputStream is){
    cachedIS = is;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setReader(Reader reader) {
    this.reader = reader;
  }

  public void setProperties(CIProperties prop){
    cachedProp = prop;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(url.length()+17);
    sb.append("[MockCachedUrl: ");
    sb.append(url);
    sb.append("]");
    return sb.toString();
  }
}
