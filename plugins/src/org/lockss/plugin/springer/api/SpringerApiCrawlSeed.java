/*
 * $Id$
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

package org.lockss.plugin.springer.api;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.IOUtils;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.*;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;

public class SpringerApiCrawlSeed extends BaseCrawlSeed {
  
  // Should become a definitional param
  public static final String CDN_URL = "http://download.springer.com/";

  public static final int EXPECTED_RECORDS_PER_RESPONSE = 100;
  
  private static final Logger log = Logger.getLogger(SpringerApiCrawlSeed.class);
  
  private static final String API_KEY;
  static {
    InputStream is = null;
    BufferedReader br = null;
    try {
      is = SpringerApiCrawlSeed.class.getResourceAsStream("api-key.txt");
      if (is == null) {
        throw new ExceptionInInitializerError("Plugin external not found");
      }
      br = new BufferedReader(new InputStreamReader(is, Constants.ENCODING_US_ASCII));
      API_KEY = br.readLine();
      if (StringUtils.isEmpty(API_KEY)) {
        throw new ExceptionInInitializerError("Plugin external not loaded");
      }
    }
    catch (IOException ioe) {
      ExceptionInInitializerError eiie = new ExceptionInInitializerError("Error reading plugin external");
      eiie.initCause(ioe);
      throw eiie;
    }
    finally {
      IOUtils.closeQuietly(br);
      IOUtils.closeQuietly(is);
    }
  }

  // Overall: 50,000 hits per day or 1 hit per 1.728s
  // Over 100 boxes: 1 hit per 172.8s rounded to 1 hit per 173s
  private static final CrawlRateLimiter API_CRAWL_RATE_LIMITER =
      new FileTypeCrawlRateLimiter(new RateLimiterInfo("SpringerApiCrawlSeed", "1/173s"));
  
  protected String apiUrl;
  
  protected String issn;
  
  protected String volume;
  
  protected CrawlerFacade facade;

  protected List<String> urlList;
  
  public SpringerApiCrawlSeed(CrawlerFacade facade) {
    super(facade);
    if (au == null) {
      throw new IllegalArgumentException("Valid archival unit required for crawl seed");
    }
    this.facade = facade;
    this.apiUrl = au.getConfiguration().get("api_url");
    this.issn = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ISSN.getKey());
    this.volume = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey());
    this.urlList = null;
  }
  
  @Override
  public Collection<String> getStartUrls() throws PluginException, IOException {
    if (urlList == null) {
      populateUrlList();
    }
    return urlList;
  }
  
  protected void populateUrlList() throws IOException {
    boolean siteWarning = false;
    urlList = new ArrayList<String>();
    int index = 1;
    SpringerApiPamLinkExtractor ple = new SpringerApiPamLinkExtractor();
    while (!ple.isDone()) {
      log.debug2("Beginning at index " + index);
      String url = makeApiUrl(index);
      UrlFetcher uf = makeApiUrlFetcher(ple, url);
      url = logUrl(url);
      facade.getCrawlerStatus().addPendingUrl(url);
      FetchResult fr = null;
      try {
        fr = uf.fetch();
      }
      catch (CacheException ce) {
        log.debug2("Stopping due to fatal CacheException", ce);
        Throwable cause = ce.getCause();
        if (cause != null && IOException.class.equals(cause.getClass())) {
          throw (IOException)cause; // Unwrap IOException
        }
        else {
          throw ce;
        }
      }
      if (fr == FetchResult.FETCHED) {
        facade.getCrawlerStatus().removePendingUrl(url);
        facade.getCrawlerStatus().signalUrlFetched(url);
      }
      else {
        log.debug2("Stopping due to fetch result " + fr);
        if (!facade.getCrawlerStatus().getUrlsWithErrors().containsKey(url)) {
          facade.getCrawlerStatus().signalErrorForUrl(url, "Cannot fetch seed URL");
        }
        throw new CacheException("Cannot fetch seed URL");
      }
      int pl = ple.getPageLength();
      if (pl != EXPECTED_RECORDS_PER_RESPONSE && !siteWarning) {
        siteWarning = true;
        log.siteWarning(String.format("Unexpected number of records per response in %s: expected %d, got %d",
                                      url,
                                      EXPECTED_RECORDS_PER_RESPONSE,
                                      pl));
      }
      index += pl;
    }
    log.debug2(String.format("Ending with %d URLs", urlList.size()));
    if (log.isDebug3()) {
      log.debug3("Start URLs: " + urlList.toString());
    }
  }
  
  protected String makeApiUrl(int startingIndex) {
    String url = String.format("%smeta/v1/pam?q=issn:%s%%20volume:%s&api_key=%s&p=%d&s=%d",
                               apiUrl,
                               issn,
                               volume,
                               API_KEY,
                               EXPECTED_RECORDS_PER_RESPONSE,
                               startingIndex);
    log.debug2("Request URL: " + logUrl(url));
    return url;
  }
  
  protected UrlFetcher makeApiUrlFetcher(final SpringerApiPamLinkExtractor ple,
                                         final String url) {
    UrlFetcher uf = facade.makeUrlFetcher(url);
    BitSet permFetchFlags = uf.getFetchFlags();
    permFetchFlags.set(UrlCacher.REFETCH_FLAG);
    uf.setFetchFlags(permFetchFlags);
    uf.setCrawlRateLimiter(API_CRAWL_RATE_LIMITER);
    uf.setUrlConsumerFactory(new UrlConsumerFactory() {
      @Override
      public UrlConsumer createUrlConsumer(CrawlerFacade ucfFacade,
                                           FetchedUrlData ucfFud) {
        return new SimpleUrlConsumer(ucfFacade, ucfFud) {
          @Override
          public void consume() throws IOException {
            final List<String> partial = new ArrayList<String>();
            try {
              ple.extractUrls(au,
                             fud.input,
                             AuUtil.getCharsetOrDefault(fud.headers), // FIXME
                             fud.origUrl,
                             new Callback() {
                               @Override
                               public void foundLink(String url) {
                                 partial.add(url);
                               }
                             });
            }
            catch (IOException ioe) {
              log.debug2("Link extractor threw", ioe);
              throw new IOException("Error while parsing PAM response for " + url, ioe);
            }
            finally {
              log.debug2(String.format("Step ending with %d URLs", partial.size()));
              if (log.isDebug3()) {
                log.debug3("URLs from step: %s"+ partial.toString());
              }
              urlList.addAll(partial);
            }
          }
        };
      }
    });
    return uf;
  }
  
  public static final String logUrl(String srcUrl) {
    return srcUrl.replaceAll("&api_key=[^&]*", "");
  }
  
}
