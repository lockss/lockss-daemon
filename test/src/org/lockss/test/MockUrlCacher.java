/*
 * $Id: MockUrlCacher.java,v 1.12 2004-02-23 09:12:05 tlipkis Exp $
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
import java.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is a mock version of <code>UrlCacher</code> used for testing
 */

public class MockUrlCacher implements UrlCacher {
  private MockCachedUrlSet cus = null;
  private MockCachedUrl cu;
  private String url;
  private InputStream cachedIS;
  private InputStream uncachedIS;
  private Properties cachedProp;
  private Properties uncachedProp;

  private boolean shouldBeCached = false;
  private IOException cachingException = null;
  private int numTimesToThrow = 1;
  private int timesThrown = 0;


  public MockUrlCacher(String url){
    this.url = url;
  }

  public MockUrlCacher(String url, MockCachedUrlSet cus){
    this(url);
    this.cus = cus;
  }

  public String getUrl() {
    return url;
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public void setCachedUrlSet(MockCachedUrlSet cus) {
    this.cus = cus;
  }

  public boolean shouldBeCached(){
    return shouldBeCached;
  }

  public void setShouldBeCached(boolean shouldBeCached){
    this.shouldBeCached = shouldBeCached;
  }

  public CachedUrl getCachedUrl() {
    return cu;
  }

  public void setCachedUrl(MockCachedUrl cu) {
    this.cu = cu;
  }

  public void setConnectionPool(LockssUrlConnectionPool connectionPool) {
  }

  public void setupCachedUrl(String contents) {
    MockCachedUrl cu = new MockCachedUrl(url);
    cu.setProperties(getUncachedProperties());
    if (contents != null) {
      cu.setContent(contents);
    }
    setCachedUrl(cu);
  }
  
  // Read interface - used by the proxy.
  
  public InputStream openForReading(){
    return cachedIS;
  }

  public Properties getProperties(){
    return cachedProp;
  }

    // Write interface - used by the crawler.

  public void storeContent(InputStream input,
			   Properties headers) throws IOException{
    cachedIS = input;
    cachedProp = headers;
  }

  public void setCachingException(IOException e, int numTimesToThrow) {
    this.cachingException = e;
    this.numTimesToThrow = numTimesToThrow;
  }
  
  public void forceCache() throws IOException {
    if (cachingException != null) {
      throw cachingException;
    }
    if (cus != null) {
      cus.addForceCachedUrl(url);
    }
    if (cu != null) {
      cu.setExists(true);
    }
  }

  public void cache() throws IOException {
    if (cus != null) {
      cus.signalCacheAttempt(url);
    }
    if (cachingException != null && timesThrown < numTimesToThrow) {
      timesThrown++;
      throw cachingException;
    }
    if (cus != null) {
      cus.addCachedUrl(url);
    }
    if (cu != null) {
      cu.setExists(true);
    }
  }

  public InputStream getUncachedInputStream(){
    return uncachedIS;
  }

  public Properties getUncachedProperties(){
    return uncachedProp;
  }

  //mock specific acessors

  public void setCachedInputStream(InputStream is){
    cachedIS = is;
  }

  public void setUncachedInputStream(InputStream is){
    uncachedIS = is;
  }

  public void setCachedProperties(Properties prop){
    cachedProp = prop;
  }

  public void setUncachedProperties(Properties prop){
    uncachedProp = prop;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(url.length()+17);
    sb.append("[MockUrlCacher: ");
    sb.append(url);
    sb.append("]");
    return sb.toString();
  }

}
