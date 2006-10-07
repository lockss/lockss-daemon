/*
 * $Id: MockUrlCacher.java,v 1.35 2006-10-07 23:13:38 tlipkis Exp $
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
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.crawler.PermissionMap;

/**
 * This is a mock version of <code>UrlCacher</code> used for testing
 */

public class MockUrlCacher implements UrlCacher {
  private static Logger logger = Logger.getLogger("MockUrlCacher");

  private MockArchivalUnit au = null;
  private MockCachedUrlSet cus = null;
  private MockCachedUrl cu;
  private String url;
  private InputStream uncachedIS;
  private CIProperties uncachedProp;

  private boolean shouldBeCached = false;
  private IOException cachingException = null;
  private RuntimeException cachingRuntimException = null;
  private int numTimesToThrow = 1;
  private BitSet fetchFlags = new BitSet();
  private PermissionMapSource permissionMapSource;

  public MockUrlCacher(String url, MockArchivalUnit au){
    this.url = url;
    this.au = au;
    this.cus = (MockCachedUrlSet)au.getAuCachedUrlSet();
  }

  public String getUrl() {
    return url;
  }

  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  /** @deprecated */
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

  public void setProxy(String proxyHost, int proxyPort) {
  }

  public void setLocalAddress(IPAddr addr) {
  }

  public void setFetchFlags(BitSet fetchFlags) {
    this.fetchFlags = fetchFlags;
  }

  public BitSet getFetchFlags() {
    return this.fetchFlags;
  }

  public void setRequestProperty(String key, String value) {
  }

  public void setRedirectScheme(RedirectScheme scheme) {
  }

  public void setWatchdog(LockssWatchdog wdog) {
  }

  public void setupCachedUrl(String contents) {
    MockCachedUrl cu = new MockCachedUrl(url);
    cu.setProperties(getUncachedProperties());
    if (contents != null) {
      cu.setContent(contents);
    }
    setCachedUrl(cu);
  }

    // Write interface - used by the crawler.

  public void storeContent(InputStream input,
			   CIProperties headers) throws IOException{
    MockCachedUrl cu = (MockCachedUrl)getCachedUrl();
    if (cu != null) {
      cu.setExists(true);
    }
    if (cus != null) {
      if (fetchFlags.get(UrlCacher.REFETCH_FLAG)) {
	cus.addForceCachedUrl(url);
      } else {
	cus.addCachedUrl(url);
      }
    }
  }

  public void setCachingException(IOException e, int numTimesToThrow) {
    this.cachingException = e;
    this.numTimesToThrow = numTimesToThrow;
  }

  public void setCachingException(RuntimeException e, int numTimesToThrow) {
    this.cachingRuntimException = e;
    this.numTimesToThrow = numTimesToThrow;
  }

  private void throwExceptionIfSet() throws IOException {
    logger.debug3("Deciding whether to throw an exception");
    if (cachingException != null && numTimesToThrow > 0) {
      numTimesToThrow--;
      throw cachingException;
    } else {
      logger.debug3("No cachingException set");
    }
    if (cachingRuntimException != null && numTimesToThrow > 0) {
      // Get a stack trace from here, not from the test case where the
      // exception was created
      cachingRuntimException.fillInStackTrace();
      numTimesToThrow--;
      throw cachingRuntimException;
    } else {
      logger.debug3("No cachingRuntimeException set");
    }
  }

  public int cache() throws IOException {
    int resultCode;

    if(cus == null) System.out.println("Warning cache() called with null cus");
    if (cus != null) {
      cus.signalCacheAttempt(url);
    }

    getUncachedInputStream();

    if (cus != null) {
      if (fetchFlags.get(UrlCacher.REFETCH_FLAG)) {
	cus.addForceCachedUrl(url);
      } else {
	cus.addCachedUrl(url);
      }
    }


    //XXX messy
    //content already there, so we should be doing a not modified response
    if (cu.hasContent()) {
      return CACHE_RESULT_NOT_MODIFIED;
    }

    //otherwise, mark that there is content and send a fetched response

    if (cu != null) {
      cu.setExists(true);
    }
    return CACHE_RESULT_FETCHED;
  }

  private boolean executed = false;

  void setNotExecuted() {
    executed = false;
  }

  public InputStream getUncachedInputStream() throws IOException {
    if (executed) {
      throw new IllegalStateException("getUncachedInputStream() called twice");
    }
    executed = true;
    throwExceptionIfSet();
    if (uncachedIS == null && cu != null) {
      return cu.getUnfilteredInputStream();
    }
    return uncachedIS;
  }

  public CIProperties getUncachedProperties() {
    return uncachedProp;
  }

  public void reset() {
  }

 //mock specific acessors

  public void setUncachedInputStream(InputStream is){
    uncachedIS = is;
  }

  public void setUncachedProperties(CIProperties prop){
    uncachedProp = prop;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(url.length()+17);
    sb.append("[MockUrlCacher: ");
    sb.append(url);
    sb.append("]");
    return sb.toString();
  }

  /**
   * setPermissionMap
   *
   * @param pmSource PermissionMap source
   */
  public void setPermissionMapSource(PermissionMapSource pmSource) {
    this.permissionMapSource = pmSource;
  }

  public PermissionMapSource getPermissionMapSource() {
    return permissionMapSource;
  }
}
