/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.MockCrawler.MockCrawlerFacade;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.UrlFetcher.RedirectScheme;
import org.lockss.plugin.base.SimpleUrlConsumerFactory;
import org.lockss.crawler.*;
import org.lockss.daemon.Crawler.CrawlerFacade;

/**
 * This is a mock version of <code>UrlCacher</code> used for testing
 */

public class MockUrlFetcher implements UrlFetcher {
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
  private IPAddr localAddr = null;
  private BitSet fetchFlags = new BitSet();
  private PermissionMapSource permissionMapSource;
  private String previousContentType;
  private byte[] storedContent;
  private CrawlRateLimiter crl;
  private RedirectScheme redirScheme;
  private CacheException infoException;
  private UrlConsumerFactory ucf;
  private CrawlerFacade cf;
  private LockssWatchdog wdog;
  private CrawlUrl curl;

  public MockUrlFetcher(CrawlerFacade cf, String url){
    this.url = url;
    this.au = (MockArchivalUnit) cf.getAu();
    this.cus = (MockCachedUrlSet)au.getAuCachedUrlSet();
    this.cf = cf;
    this.ucf = new SimpleUrlConsumerFactory();
  }

  public String getUrl() {
    return url;
  }

  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  public boolean shouldBeCached(){
    return shouldBeCached;
  }

  public void setShouldBeCached(boolean shouldBeCached){
    this.shouldBeCached = shouldBeCached;
  }

  public void setConnectionPool(LockssUrlConnectionPool connectionPool) {
  }

  public void setProxy(String proxyHost, int proxyPort) {
  }

  public void setLocalAddress(IPAddr addr) {
    localAddr = addr;
  }

  public IPAddr getLocalAddress() {
    return localAddr;
  }

  public void setCrawlUrl(CrawlUrl curl) {
    this.curl = curl;
  }
  
  public void setFetchFlags(BitSet fetchFlags) {
    this.fetchFlags = fetchFlags;
  }

  public BitSet getFetchFlags() {
    return this.fetchFlags;
  }

  public void setPreviousContentType(String previousContentType) {
    this.previousContentType = previousContentType;
  }

  public String getPreviousContentType() {
    return previousContentType;
  }

  public void setCrawlRateLimiter(CrawlRateLimiter crl) {
    this.crl = crl;
  }

  public CrawlRateLimiter getCrawlRateLimiter() {
    return crl;
  }

  public void setRequestProperty(String key, String value) {
  }

  public void setRedirectScheme(RedirectScheme scheme) {
    redirScheme = scheme;
  }

  public RedirectScheme getRedirectScheme() {
    return redirScheme;
  }

  public void setWatchdog(LockssWatchdog wdog) {
    this.wdog = wdog;
  }
  
  public LockssWatchdog getWatchdog() {
    return wdog;
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
    if (cachingException != null && numTimesToThrow > 0) {
      logger.debug3("Throwing exception " +
		    ClassUtil.getClassNameWithoutPackage(cachingException.getClass())
		    + " " +
		    StringUtil.numberOfUnits(numTimesToThrow, "more time"),
		    new Throwable());
      numTimesToThrow--;
      throw cachingException;
    } else if (cachingException != null) {
      logger.debug3("Done throwing exception");
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

  private boolean executed = false;

  void setNotExecuted() {
    executed = false;
  }

  public InputStream getUncachedInputStream() throws IOException {
    if (executed) {
      throw new IllegalStateException("getUncachedInputStream() called twice");
    }
    pauseBeforeFetch();
    executed = true;
    throwExceptionIfSet();
    if (uncachedIS == null && cu != null) {
      return new BufferedInputStream(cu.getUnfilteredInputStream());
    }
    return uncachedIS;
  }

  public CIProperties getUncachedProperties() {
    return uncachedProp;
  }

  private void pauseBeforeFetch() {
    if (crl != null) {
      crl.pauseBeforeFetch(url, previousContentType);
    }
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

  public CacheException getInfoException() {
    return infoException;
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

  @Override
  public FetchResult fetch() throws CacheException {
    pauseBeforeFetch();
    try {
      throwExceptionIfSet();
      if(ucf != null) {
        if(uncachedIS == null) {
          uncachedIS = new StringInputStream("");
        }
        if(uncachedProp == null) {
          uncachedProp = new CIProperties();
        }
        FetchedUrlData fud = 
            new FetchedUrlData(url, url, uncachedIS, uncachedProp, null, this);
        fud.setFetchFlags(fetchFlags);
        ucf.createUrlConsumer(cf, fud).consume();
      }
    } catch (IOException e) {
      throw new CacheException(e.getMessage());
    }
    return FetchResult.FETCHED;
  }

  public InputStream resetInputStream(InputStream is, 
      String lastModified) throws IOException {
    try {
      is.reset();
    } catch (IOException e) {
      return getUncachedInputStream();
    }
    return is;
  }

  @Override
  public void setUrlConsumerFactory(UrlConsumerFactory consumerFactory) {
    this.ucf = consumerFactory;
    
  }
}
