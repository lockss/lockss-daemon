/*
 * $Id: SimpleUrlConsumer.java,v 1.2 2014-11-20 01:53:01 wkwilson Exp $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.base;

import java.io.IOException;

import org.lockss.crawler.CrawlerStatus;
import org.lockss.daemon.LockssWatchdog;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlConsumer;

/**
 * This is a basic UrlConsumer. It stores the fetched url.
 * It does no processing.
 */
public class SimpleUrlConsumer implements UrlConsumer {
  protected UrlCacher cacher;
  protected ArchivalUnit au;
  protected CrawlerFacade crawlFacade;
  protected CrawlerStatus crawlStatus;
  protected FetchedUrlData fud;
  protected LockssWatchdog wdog;
  
  public SimpleUrlConsumer(CrawlerFacade crawlFacade, FetchedUrlData fud){
    this.crawlFacade = crawlFacade;
    this.au = crawlFacade.getAu();
    this.crawlStatus = crawlFacade.getCrawlerStatus();
    this.fud = fud;
  }
  
  public void consume() throws IOException {
    if (cacher == null) {
      cacher = au.makeUrlCacher(fud.getUrlData());
    }
    if (fud.fetchUrl != null) {
      cacher.setFetchUrl(fud.fetchUrl);
    }
    if (fud.storeRedirects()) {
      cacher.setRedirectUrls(fud.redirectUrls);
    }
    cacher.setFetchFlags(fud.getFetchFlags());
    cacher.storeContent();
  }
  
  public void setWatchdog(LockssWatchdog wdog) {
    this.wdog = wdog;
  }
  
}
