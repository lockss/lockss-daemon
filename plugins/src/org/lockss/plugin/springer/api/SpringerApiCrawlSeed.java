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

import org.apache.pdfbox.io.IOUtils;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.extractor.*;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.*;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;


public class SpringerApiCrawlSeed extends BaseCrawlSeed {
  
  private static final Logger log = Logger.getLogger(SpringerApiCrawlSeed.class);
  
  private static final String API_KEY;
  static {
    InputStream is = null;
    try {
      is = SpringerApiCrawlSeed.class.getResourceAsStream("api-key.txt");
      if (is == null) {
        throw new ExceptionInInitializerError();
      }
      API_KEY = new String(IOUtils.toByteArray(is), "US-ASCII");
      if (API_KEY == null || API_KEY.length() == 0) {
        throw new ExceptionInInitializerError();
      }
    }
    catch (UnsupportedEncodingException uee) {
      throw new ExceptionInInitializerError(uee);
    }
    catch (IOException ioe) {
      throw new ExceptionInInitializerError(ioe);
    }
    finally {
      IOUtils.closeQuietly(is);
    }
  }

  // Might become a definitional param
  protected static final String CDN_URL = "http://download.springer.com/";

  protected static final int INCREMENT = 100;
  
  // Overall: 50,000 hits per day or 1 hit per 1.728s
  // Over 100 boxes: 1 hit per 172.8s rounded to 1 hit per 173s
  protected static final CrawlRateLimiter API_CRAWL_RATE_LIMITER =
      new FileTypeCrawlRateLimiter(new RateLimiterInfo("SpringerApiCrawlSeed", "1/10s"));
  
  protected boolean initialized;
  
  protected String apiUrl;
  
  protected String issn;
  
  protected String volume;
  
  protected Crawler.CrawlerFacade crawlerFacade;

  protected List<String> urlList;
  
  public SpringerApiCrawlSeed(CrawlerFacade crawlerFacade) {
    super(crawlerFacade.getAu());
    if (au == null) {
      throw new IllegalArgumentException("Valid archival unit required for crawl seed");
    }
    try {
      initializeFromProps(au.getProperties());
    }
    catch (PluginException pe) {
      log.error("Error creating crawl seed", pe);
      // FIXME: cannot throw anything
      // throw pe;
    }
    this.crawlerFacade = crawlerFacade;
    this.urlList = null;
    this.initialized = true;
  }
  
  /**
   * Pulls needed params from the au props. Throws exceptions if
   *  expected props do not exist
   * @param props
   */
  protected void initializeFromProps(TypedEntryMap props) throws PluginException {
    this.apiUrl = getOneParam(props, "api_url");
    this.issn = getOneParam(props, ConfigParamDescr.JOURNAL_ISSN.getKey());
    this.volume = getOneParam(props, ConfigParamDescr.VOLUME_NAME.getKey());
  }
  
  protected void populateUrlList() {
    urlList = new ArrayList<String>(5 * INCREMENT);
    int index = 1;
    SpringerApiPamLinkExtractor ple = new SpringerApiPamLinkExtractor();
    while (ple.hasMore()) {
      log.debug2("Beginning at index " + index);
      String url = makeApiUrl(index);
      UrlFetcher uf = makeApiUrlFetcher(ple, url);
      try {
    	FetchResult fr = uf.fetch(); // which also consumes the URL
        if (fr != FetchResult.FETCHED) {
          log.debug2("Stopping; fetch result was " + fr);
          // FIXME How do we report crawl errors to the outside world?
          break;
        }
        index += INCREMENT;
      }
      catch (CacheException ce) {
        log.debug("Stopping; CacheException", ce);
        break;
      }
    }
    log.debug2("End; number of URLs: " + urlList.size());
  }
  
  protected UrlFetcher makeApiUrlFetcher(final LinkExtractor le,
                                      final String url) {
    UrlFetcher uf = crawlerFacade.makeUrlFetcher(url);
    BitSet permFetchFlags = uf.getFetchFlags();
    permFetchFlags.set(UrlCacher.REFETCH_FLAG);
    uf.setFetchFlags(permFetchFlags);
    uf.setCrawlRateLimiter(API_CRAWL_RATE_LIMITER);
    uf.setUrlConsumerFactory(new UrlConsumerFactory() {
      @Override
      public UrlConsumer createUrlConsumer(CrawlerFacade crawlerFacade,
                                           FetchedUrlData fud) {
        return new SimpleUrlConsumer(crawlerFacade, fud) {
          @Override
          public void consume() throws IOException {
            try {
              le.extractUrls(au,
                             fud.input,
                             AuUtil.getCharsetOrDefault(fud.headers),
                             fud.origUrl,
                             new Callback() {
                               @Override
                               public void foundLink(String url) {
                                 urlList.add(url);
                               }
                             });
            }
            catch (PluginException pe) {
              throw new IOException("Error while parsing PAM response for " + logUrl(url), pe);
            }
          }
        };
      };
    });
    return uf;
  }
  
  protected String makeApiUrl(int startingIndex) {
    String url = apiUrl + "meta/v1/pam?q=issn:" + issn + "%20volume:" + volume + "&api_key=" + API_KEY + "&p=" + INCREMENT + "&s=" + startingIndex;
    log.debug3("Request URL: " + logUrl(url));
    return url;
  }
  
  @Override
  public Collection<String> getStartUrls() throws PluginException {
    if (!initialized) {
      throw new PluginException("Crawl seed not initialized properly");
    }
    if (urlList == null) {
      populateUrlList();
      if (log.isDebug3()) {
        for (String url : urlList) {
          log.debug3(url);
        }
      }
    }
    return urlList;
  }
  
  protected static String getOneParam(TypedEntryMap props, String key) throws PluginException {
    if (!props.containsKey(key)) {
      throw new PluginException.InvalidDefinition(String.format("Crawl seed expected %s", key));
    }
    return props.getString(key);
  }
  
  public static final String logUrl(String srcUrl) {
    return srcUrl.replaceAll("&api_key=[^&]*", "");
  }
  
}
