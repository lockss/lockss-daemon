/*
 * $Id:$
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

/**
 * <p>
 * A custom crawl seed factory for HighWirePressH20
 * </p>
 * 
 * @since 1.67.5
 */
public class HighWirePressH20CrawlSeedFactory implements CrawlSeedFactory {
  
  private static final Logger log = Logger.getLogger(HighWirePressH20CrawlSeedFactory.class);
  
  private static final String SPECIAL_JRNL = "eolj.bmj.com/";
  private static final Pattern SPECIAL_PAT = Pattern.compile(
      "lockss-manifest/vol_eolcare/", Pattern.CASE_INSENSITIVE);
  private static final String SPECIAL_REPL = "lockss-manifest/eolcare_vol_";
  
  /**
   * <p>
   * The class is only needed for eolcare AUs
   * So "lockss-manifest/vol_eolcare/1_manifest.dtl" is transformed
   * to "lockss-manifest/eolcare_vol_1_manifest.dtl"
   * </p>
   */
  public static class SpecialCrawlSeed extends BaseCrawlSeed {
    
    public SpecialCrawlSeed(CrawlerFacade crawlerFacade) {
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
        Matcher urlMat = SPECIAL_PAT.matcher(sUrl); 
        if (urlMat.find()) {
          sUrl = urlMat.replaceFirst(SPECIAL_REPL);
          if (log.isDebug2()) {
            log.debug2(sUrl);
          }
        }
        uUrls.add(sUrl);
      }
      return uUrls;
    }
  }
  
  @Override
  public CrawlSeed createCrawlSeed(CrawlerFacade facade) {
    String baseUrl = facade.getAu().getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    if (baseUrl != null && baseUrl.contains(SPECIAL_JRNL)) {
      return new SpecialCrawlSeed(facade);
    }
    return new BaseCrawlSeed(facade);
  }
  
}
