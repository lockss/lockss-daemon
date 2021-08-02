/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
 * A custom crawl seed factory for HighWire Drupal
 * </p>
 * 
 * @since 1.67.5
 */
public class HighWireJCoreCrawlSeed {
  
  private static final Logger log = Logger.getLogger(HighWireJCoreCrawlSeed.class);
  
  private static final String APS_JN_JRNL = "://jn.physiology.org/";
  
  /**
   * <p>
   * The class is only needed by jn.physiology.org AUs
   * </p>
   */
  public static class APSCrawlSeed extends BaseCrawlSeed {
    
    public APSCrawlSeed(CrawlerFacade crawlerFacade) {
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
  
  /** This factory is used to create crawl seed for APS journal */
  public static class Factory implements CrawlSeedFactory {
    
    @Override
    public CrawlSeed createCrawlSeed(CrawlerFacade facade) {
      String baseUrl = facade.getAu().getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      if (baseUrl != null) {
        if (baseUrl.contains(APS_JN_JRNL)) {
          return new APSCrawlSeed(facade);
        }
      }
      return new BaseCrawlSeed(facade);
    }
  }
  
}
