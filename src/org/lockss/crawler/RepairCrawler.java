/*
 * $Id: RepairCrawler.java,v 1.6 2004-02-13 02:34:20 troberts Exp $
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

package org.lockss.crawler;
import java.util.*;
import java.net.*;
import java.io.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.protocol.*;
import org.lockss.proxy.ProxyManager;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.state.*;

public class RepairCrawler extends CrawlerImpl {
  
  private static Logger logger = Logger.getLogger("RepairCrawler");

  private IdentityManager idMgr = null;

  protected Collection repairUrls = null;

  public static final String PARAM_FETCH_FROM_OTHER_CACHE =
      Configuration.PREFIX + "crawler.fetch_from_other_caches";

  private static final String HEADER_PREFIX = "_header";

  private float percentFetchFromCache = 0;

  public RepairCrawler(ArchivalUnit au, CrawlSpec spec,
		       AuState aus, Collection repairUrls,
		       float percentFetchFromCache) {
    super(au, spec, aus);
    if (repairUrls == null) {
      throw new IllegalArgumentException("Called with null repairUrls");
    } else if (repairUrls.size() == 0) {
      throw new IllegalArgumentException("Called with empty repairUrls list");
    }
    this.repairUrls = repairUrls;
    this.percentFetchFromCache = percentFetchFromCache;
    //XXX hack, since crawlStatus will get set twice
    crawlStatus = new Crawler.Status(au, repairUrls, getType());
  }

  public int getType() {
    return Crawler.REPAIR;
  }

  protected Iterator getStartingUrls() {
    return repairUrls.iterator();
  }


  protected boolean doCrawl0(Deadline deadline) {
    boolean windowClosed = false;
    logger.info("Beginning crawl of "+au);
    crawlStatus.signalCrawlStarted();
    CachedUrlSet cus = au.getAuCachedUrlSet();

    Iterator it = getStartingUrls();
    
    while (it.hasNext() && !deadline.expired() && !crawlAborted) {
      String url = (String)it.next();
      //catch and warn if there's a url in the start urls
      //that we shouldn't cache
      // check crawl window during crawl
      if ((spec!=null) && (!spec.canCrawl())) {
	logger.debug("Crawl canceled: outside of crawl window");
	windowClosed = true;
	// break from while loop
	break;
      }
      if (spec.isIncluded(url)) {
	if (!doCrawlLoop(url, cus)) {
	  if (crawlStatus.getCrawlError() == 0) {
	    crawlStatus.setCrawlError(Crawler.STATUS_ERROR);
	  }
	}
      } else {
	logger.warning("Called with a starting url we aren't suppose to "+
		       "cache: "+url);
      }
      if (windowClosed) {
	// break from for loop
	break;
      }
    }
    // unsuccessful crawl if window closed
    if (windowClosed) {
      crawlStatus.setCrawlError(Crawler.STATUS_WINDOW_CLOSED);
    }
    if (crawlStatus.getCrawlError() != 0) {
      logger.info("Finished crawl (errors) of "+au);
    } else {
      logger.info("Finished crawl of "+au);
    }
    
    if (au instanceof BaseArchivalUnit) {
      BaseArchivalUnit bau = (BaseArchivalUnit)au;
      long cacheHits = bau.getCrawlSpecCacheHits();
      long cacheMisses = bau.getCrawlSpecCacheMisses();
      double per = ((float)cacheHits /
		    ((float)cacheHits + (float)cacheMisses));
      logger.info("Had "+cacheHits+" cache hits, with a percentage of "+per);
    }
    return (crawlStatus.getCrawlError() == 0);
  }

  protected boolean doCrawlLoop(String url, CachedUrlSet cus) {
    int error = 0;
    logger.debug2("Dequeued url from list: "+url);
    Plugin plugin = au.getPlugin();
    UrlCacher uc = plugin.makeUrlCacher(cus, url);

    // don't cache if already cached, unless overwriting
    try {
      if (wdog != null) {
	wdog.pokeWDog();
      }
      if (shouldFetchFromCache()) {
	fetchFromCache(uc);
      } else {
	cache(uc);
      }
      numUrlsFetched++;
    } catch (FileNotFoundException e) {
      logger.warning(uc+" not found on publisher's site");
    } catch (IOException ioe) {
      //XXX handle this better.  Requeue?
      logger.error("Problem caching "+uc+". Ignoring", ioe);
      error = Crawler.STATUS_FETCH_ERROR;
    }
    return (error == 0);
  }

  private boolean shouldFetchFromCache() {
    return ProbabilisticChoice.choose(percentFetchFromCache);
  }
  
  protected void fetchFromCache(UrlCacher uc) throws IOException {
    IdentityManager idm = getIdentityManager();
    Map map = idm.getAgreed(au);
    Iterator it = map.keySet().iterator();
    if (it.hasNext()) {
      fetchFromCache(uc, (String)it.next());
    }
  }

  protected void fetchFromCache(UrlCacher uc, String id)
      throws IOException {
    ProxyManager proxyMan = 
      (ProxyManager)LockssDaemon.getManager(LockssDaemon.PROXY_MANAGER);
    int proxyPort = proxyMan.getProxyPort();

    URL url = new URL(null, uc.getUrl(), new MyHandler(id, proxyPort));

    
    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    conn.setRequestProperty("user-agent", LockssDaemon.getUserAgent());
    conn.connect();
    logger.debug("Trying to fetch from "+id);
    uc.storeContent(conn.getInputStream(),
		    getPropertiesFromConn(conn, uc.getUrl()));
  }

  private Properties getPropertiesFromConn(HttpURLConnection conn, String url)
      throws IOException {
    Properties props = new Properties();
    // set header properties in which we have interest
    props.setProperty("content-type", conn.getContentType());
    props.setProperty("date", Long.toString(conn.getDate()));
    props.setProperty("content-url", url);

    // store all header properties (this is the only way to iterate)
    int index = 0;
    while (true) {
      String key = conn.getHeaderFieldKey(index);
      String value = conn.getHeaderField(index);
      if ((key==null) && (value==null)) {
        // the first header field has a null key, so we can't break just on key
        break;
      }
      if (value!=null) {
        // only store headers with values
        // qualify header names to avoid conflict with our properties
        if (key!=null) {
          props.setProperty(HEADER_PREFIX + key, value);
        } else {
          // the first header field has a null key
          props.setProperty(HEADER_PREFIX + index, value);
        }
      }
      index++;
    }
    return props;
  }

  private void cache(UrlCacher uc) throws IOException {
    try {
      uc.forceCache();
    } catch (IOException e) {
      logger.debug("Exception when trying to cache "+uc, e);
    }
  }


  private IdentityManager getIdentityManager() {
    if (idMgr == null) {
      idMgr =
	(IdentityManager)LockssDaemon.getManager(LockssDaemon.IDENTITY_MANAGER);
    }
    return idMgr;
  }

}

//XXX mighty hack.  FIXME.
class MyHandler extends sun.net.www.protocol.http.Handler {
  public MyHandler (String proxy, int port) {
    super(proxy, port);
  }

  protected java.net.URLConnection openConnection(URL u) throws IOException {
    return new MyHttpURLConnection(u, this);
  }

  public String getProxyHost() {
    return proxy;
  }

  public int getProxyPort() {
    return proxyPort;
  }
}

class MyHttpURLConnection extends sun.net.www.protocol.http.HttpURLConnection {
  protected MyHttpURLConnection(URL u, MyHandler handler) throws IOException {
    super(u, handler);
  }

  public void connect() throws IOException {
    if (connected) {
      return;
    }
    try {
      // Always create new connection when proxying
      MyHandler h = (MyHandler)handler;
      http = getProxiedClient(url, h.getProxyHost(), h.getProxyPort());
      ps = (PrintStream)http.getOutputStream();
    } catch (IOException e) {
      throw e;
    }
    // constructor to HTTP client calls openserver
    connected = true;
  }
}
