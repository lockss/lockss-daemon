/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ojs3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.lockss.crawler.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A custom crawl seed factory -it adds an optional attr "start_stem" to the path
 * defined as a start_url in the plugin 
 * https://foo.com/<journal_id>/gateway/clockss?year=1234
 * becomes
 * https://foo.com/<start_stem>/<journal_id>/gateway/clockss?year=1234
 * and if there is no start_stem then just stays
 * https://foo.com/<journal_id>/gateway/clockss?year=1234
 * </p>
 * 
 * @since 1.67.5
 */
public class Ojs3CrawlSeedFactory implements CrawlSeedFactory {

  private static final Logger log = Logger.getLogger(Ojs3CrawlSeedFactory.class);

  public static class AddStemCrawlSeed extends BaseCrawlSeed {

    private String jid;
    private String baseUrl;
    private String primaryLocale;
    private final String PRIMARY_LOCAL_ATTR = "primary_locale";

    public AddStemCrawlSeed(CrawlerFacade crawlerFacade) {
      super(crawlerFacade);
    }

    /**
     * Add any initialization here for lazy initialization
     */
    @Override
    protected void initialize() throws ConfigurationException, PluginException, IOException {
      baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      jid = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
      primaryLocale = AuUtil.getTitleAttribute(au, PRIMARY_LOCAL_ATTR);
      log.debug3("stored params: " + baseUrl + jid + primaryLocale);
    }

    @Override
    public Collection<String> doGetPermissionUrls() throws ConfigurationException,
        PluginException, IOException {
      return Ojs3StartStemHelper.addStartStem(au, super.doGetPermissionUrls());
    }    

	@Override
    public Collection<String> doGetStartUrls() throws ConfigurationException,
        PluginException, IOException {
      return addLocale(Ojs3StartStemHelper.addStartStem(au, super.doGetStartUrls()));
    }

    private Collection<String> addLocale(Collection<String> urls) {
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
        log.debug3("adding Url: " + setLocaleUrlRoot + UrlUtil.encodeUrl(sourcePath));
      }
      return localeUrls;
    }
  }
  
  @Override
  public CrawlSeed createCrawlSeed(CrawlerFacade facade) {
	  return new AddStemCrawlSeed(facade);
  }
}
