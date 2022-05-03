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

package org.lockss.plugin.ojs2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.lockss.crawler.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A custom crawl seed factory
 * </p>
 * 
 * @since 1.67.5
 */
public class OJS2CrawlSeedFactory implements CrawlSeedFactory {
  
  private static final Logger log = Logger.getLogger(OJS2CrawlSeedFactory.class);
  
  protected static final HashSet<String> dualBaseUrlHosts = new HashSet<>();
  static {
    dualBaseUrlHosts.add("ejournals.library.ualberta.ca");
    dualBaseUrlHosts.add("scholarworks.iu.edu");
  }

  protected static final HashSet<String> noIndexBaseUrlHosts = new HashSet<>();
  static {
    noIndexBaseUrlHosts.add("mulpress.mcmaster.ca");
    noIndexBaseUrlHosts.add("ijms.nmdl.org");
    noIndexBaseUrlHosts.add("www.ajpspharm.com");
    noIndexBaseUrlHosts.add("www.ijic.info");
    noIndexBaseUrlHosts.add("www.investigativesciencesjournal.org");
    noIndexBaseUrlHosts.add("www.journal-alm.org");
    noIndexBaseUrlHosts.add("www.ijtarp.org");
    noIndexBaseUrlHosts.add("www.intersticios.es");
    noIndexBaseUrlHosts.add("www.wpcjournal.com");
    noIndexBaseUrlHosts.add("www.kcajournals.com");
    noIndexBaseUrlHosts.add("www.kmuj.kmu.edu.pk");

  }

  public static class LocaleUrlCrawlSeed extends BaseCrawlSeed {

    private String jid;
    private String baseUrl;
    private String primaryLocale;
    private final String PRIMARY_LOCAL_ATTR = "primary_locale";


    public LocaleUrlCrawlSeed(CrawlerFacade crawlerFacade) {
      super(crawlerFacade);
    }

    /**
     * Add any initialization here for lazy initialization
     */
    @Override
    protected void initialize() throws ConfigurationException, PluginException, IOException {
      log.info("initializing localcrawlseed");
      baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      jid = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
      primaryLocale = AuUtil.getTitleAttribute(au, PRIMARY_LOCAL_ATTR);
      log.info("stored params: " + baseUrl + jid + primaryLocale);
    }

    @Override
    public Collection<String> doGetPermissionUrls() throws ConfigurationException,
        PluginException, IOException {
      return addLocale(super.doGetPermissionUrls());
    }

    @Override
    public Collection<String> doGetStartUrls() throws ConfigurationException,
        PluginException, IOException {
      return addLocale(super.doGetStartUrls());
    }

    private Collection<String> addLocale(Collection<String> urls) {
      log.info("In addLocale w/primaryLocale: " + primaryLocale);
      if (primaryLocale == null) {
        // nothing to do if this is missing.
        return urls;
      }
      String SET_LOCAL_QUERY = "/user/setLocale/primary_locale?source=";
      Collection<String> localeUrls = new ArrayList<>(urls.size());
      for (String url : urls) {
        // https://www.ride.org.mx/index.php/RIDE/user/setLocale/es_ES?source=%2Findex.php%2FRIDE%2Fgateway%2Flockss%3Fyear%3D2020
        String sourcePath = "/" + url.substring(baseUrl.length());
        String setLocaleUrlRoot = url.substring(0, url.lastIndexOf(jid) + jid.length());
        setLocaleUrlRoot += SET_LOCAL_QUERY.replace(PRIMARY_LOCAL_ATTR, primaryLocale);
        localeUrls.add(setLocaleUrlRoot + UrlUtil.encodeUrl(sourcePath));
        log.info("adding Url: " + setLocaleUrlRoot + UrlUtil.encodeUrl(sourcePath));

      }
      return localeUrls;
    }

  }
  
  public static class DualBaseUrlCrawlSeed extends LocaleUrlCrawlSeed {
    
    public DualBaseUrlCrawlSeed(CrawlerFacade crawlerFacade) {
      super(crawlerFacade);
    }
    
    @Override
    public Collection<String> doGetPermissionUrls() throws ConfigurationException,
        PluginException, IOException {
      return dupUrls(super.doGetPermissionUrls());
    }
    
    @Override
    public Collection<String> doGetStartUrls() throws ConfigurationException,
        PluginException, IOException {
      return dupUrls(super.doGetStartUrls());
    }

    private Collection<String> dupUrls(Collection<String> sUrls) {
      Collection<String> uUrls = new ArrayList<>(sUrls.size() * 2);
      for (String url : sUrls) {
        uUrls.add(UrlUtil.replaceScheme(url, "https", "http"));
        uUrls.add(UrlUtil.replaceScheme(url, "http", "https"));
      }
      return uUrls;
    }

  }

  public static class NoIndexBaseUrlCrawlSeed extends LocaleUrlCrawlSeed {
    
    public NoIndexBaseUrlCrawlSeed(CrawlerFacade crawlerFacade) {
      super(crawlerFacade);
    }
    
    @Override
    public Collection<String> doGetPermissionUrls() throws ConfigurationException,
        PluginException, IOException {
      return removeIndex(super.doGetPermissionUrls());
    }
    
    @Override
    public Collection<String> doGetStartUrls() throws ConfigurationException,
        PluginException, IOException {
      return removeIndex(super.doGetStartUrls());
    }

    private Collection<String> removeIndex(Collection<String> sUrls) {
      Collection<String> uUrls = new ArrayList<>(sUrls.size());
      for (String url : sUrls) {
        // we know these will contain index.php
        // remove url encoded version as well.
        uUrls.add(url.replace("index.php/", "").replace("index.php%2F", ""));
      }
      return uUrls;
    }

  }
  
  
  
  @Override
  public CrawlSeed createCrawlSeed(CrawlerFacade facade) {
    try {
      String baseUrl = facade.getAu().getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      if (baseUrl != null) {
        if (dualBaseUrlHosts.contains(UrlUtil.getHost(baseUrl))) {
          return new DualBaseUrlCrawlSeed(facade);
        } else if (noIndexBaseUrlHosts.contains(UrlUtil.getHost(baseUrl))) {
          return new NoIndexBaseUrlCrawlSeed(facade);
        }
      }
    } catch (Exception e) {
      log.warning("createCrawlSeed e= ", e);
      // Fall-thru
    }
    return new BaseCrawlSeed(facade);
  }
  
}
