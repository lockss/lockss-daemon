/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.exploded;

import java.io.IOException;

import org.lockss.crawler.ArcExploder;
import org.lockss.crawler.BaseCrawler;
import org.lockss.crawler.CrawlerStatus;
import org.lockss.crawler.Exploder;
import org.lockss.crawler.TarExploder;
import org.lockss.crawler.WarcExploder;
import org.lockss.crawler.ZipExploder;
import org.lockss.daemon.LockssWatchdog;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ExploderHelper;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.RegexpUtil;

/**
 * This is a ExplodingUrlConsumer. It iterates through an archive and stores
 * internal files.
 */ 
public class ExplodingUrlConsumer extends SimpleUrlConsumer {
  protected ExploderHelper eh;
  protected boolean storeArchive;
  protected String exploderPattern;
  
  public ExplodingUrlConsumer(CrawlerFacade crawlFacade, FetchedUrlData fud,
      String exploderPattern){
    super(crawlFacade, fud);
    this.exploderPattern = exploderPattern;
  }
  
  public void consume() throws IOException{
    if(shouldExplode(fud.fetchUrl)) {
      Exploder exploder = getExploder(fud);
      exploder.explode();
    } else {
      super.consume();
    }
  }
  
  protected boolean shouldExplode(String url) {
    return RegexpUtil.isMatchRe(url, exploderPattern);
  }
  
  protected Exploder getExploder(FetchedUrlData toExplode) {
    Exploder ret = null;
    String url = toExplode.fetchUrl;
    eh = getHelper();
    if (url.endsWith(".arc.gz")) {
      ret = new ArcExploder(toExplode, crawlFacade, getHelper());
    } else if (url.endsWith(".warc.gz")) {
      ret = new WarcExploder(toExplode, crawlFacade, getHelper());
    } else if (url.endsWith(".zip")) {
      ret = new ZipExploder(toExplode, crawlFacade, getHelper());
    } else if (url.endsWith(".tar")) {
      ret = new TarExploder(toExplode, crawlFacade, getHelper());
    }
    return ret;
  }
  
  protected ExploderHelper getHelper() {
    return eh;
  }
  
  public void setExploderHelper(ExploderHelper eh) {
    this.eh = eh;
  }
}
