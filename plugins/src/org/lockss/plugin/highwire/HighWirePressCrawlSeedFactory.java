/*
 * $Id:$
 */

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.lockss.crawler.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A custom crawl seed factory for HighWirePress H10a/b/c
 * </p>
 * 
 * @since 1.67.5
 */
public class HighWirePressCrawlSeedFactory implements CrawlSeedFactory {
  
  private static final Logger log = Logger.getLogger(HighWirePressCrawlSeedFactory.class);
  
  private static final String OUP_JRNL = ".oxfordjournals.org/";
  
  public static class OUPCrawlSeed extends BaseCrawlSeed {
    
    public OUPCrawlSeed(CrawlerFacade crawlerFacade) {
      super(crawlerFacade);
    }
    
    @Override
    public Collection<String> doGetStartUrls()
        throws ConfigurationException, PluginException, IOException {
      
      Collection<String> sUrls = super.doGetStartUrls();
      Collection<String> uUrls = new ArrayList<String>(sUrls.size() * 2);
      for (Iterator<String> iter = sUrls.iterator(); iter.hasNext();) {
        String url = iter.next();
        uUrls.add(UrlUtil.replaceScheme(url, "https", "http"));
        uUrls.add(UrlUtil.replaceScheme(url, "http", "https"));
      }
      return uUrls;
    }
    
  }
  
  @Override
  public CrawlSeed createCrawlSeed(CrawlerFacade facade) {
    String baseUrl = facade.getAu().getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    if (baseUrl != null) {
      if (baseUrl.contains(OUP_JRNL)) {
        return new OUPCrawlSeed(facade);
      }
    }
    return new BaseCrawlSeed(facade);
  }
  
}
