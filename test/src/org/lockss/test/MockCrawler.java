/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import org.lockss.crawler.*;
import org.lockss.daemon.Crawler;
import org.lockss.plugin.*;
import org.lockss.util.Deadline;
import org.lockss.util.urlconn.CacheException;

public class MockCrawler extends NullCrawler {
  ArchivalUnit au;
  Collection urls;
  boolean followLinks;
  boolean doCrawlCalled = false;
  Deadline deadline = null;
  boolean crawlSuccessful = true;
  Crawler.Type type = null;
  boolean isWholeAU = false;
  long startTime = -1;
  long endTime = -1;
  long numFetched = -1;
  long numParsed = -1;


  CrawlerStatus status = null;

  boolean wasAborted = false;


  public MockCrawler() {
  }

  public MockCrawler(ArchivalUnit au) {
    this.au = au;
  }

  public void abortCrawl() {
    wasAborted = true;
  }

  public boolean wasAborted() {
    return wasAborted;
  }

  public void setCrawlSuccessful(boolean crawlSuccessful) {
    this.crawlSuccessful = crawlSuccessful;
  }

  public boolean doCrawl() {
    doCrawlCalled = true;
    return crawlSuccessful;
  }

  public Deadline getDeadline() {
    return deadline;
  }

  public boolean doCrawlCalled() {
    return doCrawlCalled;
  }

  public void setDoCrawlCalled(boolean val) {
    doCrawlCalled = val;
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public void setAu(ArchivalUnit au) {
    this.au = au;
  }

  public void setUrls(Collection urls) {
    this.urls = urls;
  }

  public void setFollowLinks(boolean followLinks) {
    this.followLinks = followLinks;
  }

  public void setType(Crawler.Type type) {
    this.type = type;
  }

  public Crawler.Type getType() {
    return type;
  }

  public void setIsWholeAU(boolean val) {
    isWholeAU = val;
  }

  public boolean isWholeAU() {
    return isWholeAU;
  }

  public Collection<String> getStartUrls() {
    return urls;
  }

  public void setStartTime(long time) {
    startTime = time;
  }

//   public void setEndTime(long time) {
//     endTime = time;
//   }

//   public void setNumFetched(long num) {
//     numFetched = num;
//   }

//   public void setNumParsed(long num) {
//     numParsed = num;
//   }

  public long getStartTime() {
    return startTime;
  }

//   public long getEndTime() {
//     return endTime;
//   }

//   public long getNumFetched() {
//     return numFetched;
//   }

//   public long getNumParsed() {
//     return numParsed;
//   }

  public void setStatus(CrawlerStatus status) {
    this.status = status;
  }

  public CrawlerStatus getCrawlerStatus() {
    if (status == null) {
      status = new MockCrawlStatus();
    }
    return status;
  }
  
  public class MockCrawlerFacade implements Crawler.CrawlerFacade {
    private ArchivalUnit au;
    private CrawlerStatus cs;
    private Map<String, UrlFetcher> ufMap;
    public List<String> fetchQueue = new ArrayList<String>();
    public List<String> permProbe = new ArrayList<String>();
    private PermissionMap permissionMap;
    private List<String> globallyPermittedHosts = Collections.emptyList();
    private List<String> allowedPluginPermittedHost = Collections.emptyList();

    public MockCrawlerFacade() {
      au = new MockArchivalUnit();
    }
    
    public MockCrawlerFacade(ArchivalUnit mau) {
      au = mau;
    }
    
    public ArchivalUnit getAu() {
      return au;
    }

    public void setAu(ArchivalUnit au) {
      this.au = au;
    }
    
    public void setCrawlerStatus(CrawlerStatus status) {
      this.cs = status;
    }
    
    public CrawlerStatus getCrawlerStatus() {
      if(cs == null) {
        cs = new MockCrawlStatus();
      }
      return cs;
    }

    @Override
    public void addToFailedUrls(String url) {
      //Not used for testing
    }

    @Override
    public void addToFetchQueue(CrawlUrlData curl) {
      fetchQueue.add(curl.getUrl());
      
    }

    @Override
    public void addToParseQueue(CrawlUrlData curl) {
      throw new UnsupportedOperationException("not implemented");
      
    }

    @Override
    public void addToPermissionProbeQueue(String probeUrl, String referrerUrl) {
      permProbe.add(probeUrl);
      
    }

    @Override
    public void setPreviousContentType(String previousContentType) {
      throw new UnsupportedOperationException("not implemented");
      
    }

    @Override
    public boolean isAborted() {
      return wasAborted;
    }

    public void setUrlFetcher(UrlFetcher uf) {
      if (ufMap == null) {
        ufMap = new HashMap<String, UrlFetcher>();
      }
      ufMap.put(uf.getUrl(), uf);
    }
    
    public void setPermissionUrlFetcher(UrlFetcher uf) {
      if (ufMap == null) {
        ufMap = new HashMap<String, UrlFetcher>();
      }
      ufMap.put(uf.getUrl(), uf);
    }
    
    @Override
    public UrlFetcher makeUrlFetcher(String url) {
      return ufMap.get(url);
    }

    @Override
    public UrlFetcher makePermissionUrlFetcher(String url) {
      return ufMap.get(url);
    }

    @Override
    public UrlCacher makeUrlCacher(UrlData ud) {
      return au.makeUrlCacher(ud);
    }

    @Override
    public boolean hasPermission(String url) {
      if(permissionMap != null) {
        permissionMap.hasPermission(url);
      }
      return true;
    }
    
    public void setPermissionMap(PermissionMap permMap) {
      permissionMap = permMap;
    }
    
    @Override
    public long getRetryDelay(CacheException ce) {
      return BaseCrawler.DEFAULT_DEFAULT_RETRY_DELAY;
    }

    @Override
    public int getRetryCount(CacheException ce) {
      return BaseCrawler.DEFAULT_DEFAULT_RETRY_COUNT;
    }

    @Override
    public int permissonStreamResetMax() {
      return BaseCrawler.DEFAULT_PERMISSION_BUF_MAX;
    }

    public void setGloballyPermittedHosts(List<String> hosts) {
      globallyPermittedHosts = hosts;
    }

    public void setAllowedPluginPermittedHosts(List<String> hosts) {
      allowedPluginPermittedHost = hosts;
    }

    @Override
    public boolean isGloballyPermittedHost(String host) {
      return globallyPermittedHosts.contains(host);
    }

    @Override
    public boolean isAllowedPluginPermittedHost(String host) {
      return allowedPluginPermittedHost.contains(host);
    }

    @Override
    public void updateCdnStems(String url) {
    }

    @Override
    public CrawlUrl addChild(CrawlUrl curl, String url) {
      return null;
    }

    @Override
    public Object putStateObj(String key, Object val) {
      return null;
    }

    @Override
    public Object getStateObj(String key) {
      return null;
    }

  }
}
