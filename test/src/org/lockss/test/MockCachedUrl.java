/*
 * $Id: MockCachedUrl.java,v 1.11 2003-02-25 22:07:50 troberts Exp $
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

package org.lockss.test;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Properties;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

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
  private Properties cachedProp;

  private boolean doesExist = false;
  private String content = null;

  public MockCachedUrl(String url) {
    this.url = url;
  }

  public MockCachedUrl(String url, CachedUrlSet cus) {
    this(url);
    this.cus = cus;
  }

  public String getUrl() {
    return url;
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

  public InputStream openForReading() {
    if (content != null) {
      return new StringInputStream(content);
    }
    return cachedIS;
  }

  public Properties getProperties(){
    return cachedProp;
  }

    // Write interface - used by the crawler.

  public void storeContent(InputStream input, Properties headers)
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

  public void setProperties(Properties prop){
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
