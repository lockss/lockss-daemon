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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.crawler.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A custom crawl seed factory for HighWirePressH20
 * </p>
 * 
 * @since 1.67.5
 */
public class HighWirePressH20CrawlSeedFactory implements CrawlSeedFactory {
  
  private static final Logger log = Logger.getLogger(HighWirePressH20CrawlSeedFactory.class);
  
  private static final String BMJ_EOLJ_JRNL = "eolj.bmj.com/";
  private static final Pattern BMJ_EOLJ_PAT = Pattern.compile(
      "lockss-manifest/vol_eolcare/", Pattern.CASE_INSENSITIVE);
  private static final String BMJ_EOLJ_REPL = "lockss-manifest/eolcare_vol_";
  
  private static final String OUP_JRNL = ".oxfordjournals.org/";
  
  /**
   * <p>
   * The class is only needed for eolcare AUs
   * So "lockss-manifest/vol_eolcare/1_manifest.dtl" is transformed
   * to "lockss-manifest/eolcare_vol_1_manifest.dtl"
   * </p>
   */
  public static class BmjEoljCrawlSeed extends BaseCrawlSeed {
    
    public BmjEoljCrawlSeed(CrawlerFacade crawlerFacade) {
      super(crawlerFacade);
    }
    
    @Override
    public Collection<String> doGetPermissionUrls() throws ConfigurationException,
        PluginException, IOException {
      return checkUrls(super.doGetPermissionUrls());
    }
    
    @Override
    public Collection<String> doGetStartUrls() throws ConfigurationException,
        PluginException, IOException {
      return checkUrls(super.doGetStartUrls());
    }
    
    protected Collection<String> checkUrls(Collection<String> sUrls) {
      Collection<String> uUrls = new ArrayList<String>(sUrls.size());
      for (Iterator<String> iter = sUrls.iterator(); iter.hasNext();) {
        String sUrl = iter.next();
        Matcher urlMat = BMJ_EOLJ_PAT.matcher(sUrl); 
        if (urlMat.find()) {
          sUrl = urlMat.replaceFirst(BMJ_EOLJ_REPL);
          if (log.isDebug2()) {
            log.debug2(sUrl);
          }
        }
        uUrls.add(sUrl);
      }
      return uUrls;
    }
  }
  
  public static class OUPH20CrawlSeed extends BaseCrawlSeed {
    
    public OUPH20CrawlSeed(CrawlerFacade crawlerFacade) {
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
        return new OUPH20CrawlSeed(facade);
      }
      if (baseUrl.contains(BMJ_EOLJ_JRNL)) {
        return new BmjEoljCrawlSeed(facade);
      }
    }
    return new BaseCrawlSeed(facade);
  }
  
}
