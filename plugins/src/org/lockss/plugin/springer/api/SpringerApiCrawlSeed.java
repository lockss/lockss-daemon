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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;

/**
 * <p>
 * A crawl seed that queries Springer's Meta API to enumerate article metadata
 * and synthesize start URLs for crawls.
 * </p>
 * <p>
 * Note that this is the newer Meta API, not the older Metadata API.
 * </p>
 * 
 * @since 1.67.5
 * @see https://dev.springer.com/
 */
public class SpringerApiCrawlSeed extends BaseCrawlSeed {

  /**
   * <p>
   * A logger for this class.
   * </p>
   * 
   * @since 1.67.5
   */
  private static final Logger log = Logger.getLogger(SpringerApiCrawlSeed.class);
  
  // Will become a definitional param
  private static final String CDN_URL = "http://download.springer.com/";

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

  /**
   * <p>
   * A crawl rate limit for requests to the API service.
   * </p>
   * 
   * @since 1.67.5
   */
  // Overall: 50,000 hits per day or 1 hit per 1.728s
  // Over 100 boxes: 1 hit per 172.8s rounded to 1 hit per 173s
  private static final String API_CRAWL_RATE_LIMIT = "1/173s";
  
  /**
   * <p>
   * A crawl rate limiter for requests to the API service.
   * </p>
   * 
   * @since 1.67.5
   */
  private static final CrawlRateLimiter API_CRAWL_RATE_LIMITER =
      new FileTypeCrawlRateLimiter(
          new RateLimiterInfo(SpringerApiCrawlSeed.class.getSimpleName(),
                              API_CRAWL_RATE_LIMIT));
  
  /**
   * <p>
   * A constant for the maximum number of records expected per response from the
   * API. If the API behaves otherwise, a site warning will be logged.
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final int EXPECTED_RECORDS_PER_RESPONSE = 100;

  /**
   * <p>
   * The API URL (<code>api_url</code>) of this crawl seed's AU.
   * </p>
   * 
   * @since 1.67.5
   */
  protected String apiUrl;
  
  /**
   * <p>
   * The journal ISSN (<code>journal_issn</code>) of this crawl seed's AU.
   * </p>
   * 
   * @since 1.67.5
   */
  protected String issn;
  
  /**
   * <p>
   * The volume name (<code>volume_name</code>) of this crawl seed's AU.
   * </p>
   * 
   * @since 1.67.5
   */
  protected String volume;

  /**
   * <p>
   * This crawl seed's crawler façade.
   * </p>
   * 
   * @since 1.67.5
   */
  protected CrawlerFacade facade;

  /**
   * <p>
   * This crawl seed's list of start URLs.
   * </p>
   * 
   * @since 1.67.5
   */
  protected List<String> urlList;

  /**
   * <p>
   * Builds a new crawl seed with the given crawler façade.
   * </p>
   * 
   * @param facade
   *          A crawler façade for this crawl seed.
   * @since 1.67.5
   */
  public SpringerApiCrawlSeed(CrawlerFacade facade) {
    super(facade);
    if (au == null) {
      throw new IllegalArgumentException("Valid archival unit required for crawl seed");
    }
    this.facade = facade;
  }

  @Override
  protected void initialize() 
      throws ConfigurationException ,PluginException ,IOException {
    super.initialize();
    this.apiUrl = au.getConfiguration().get("api_url");
    this.issn = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ISSN.getKey());
    this.volume = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey());
    this.urlList = null;
  }
  
  @Override
  public Collection<String> doGetStartUrls() throws PluginException, IOException {
    if (urlList == null) {
      populateUrlList();
    }
    return urlList;
  }

  /**
   * <p>
   * Populates the URL list with start URLs.
   * </p>
   * 
   * @throws IOException
   * @since 1.67.5
   */
  protected void populateUrlList() throws IOException {
    // Initialization
    boolean siteWarning = false; // Flag to log the potential siteWarning only once
    urlList = new ArrayList<String>();
    int index = 1; // API numbers records starting with 1
    SpringerApiPamLinkExtractor ple = new SpringerApiPamLinkExtractor();
    
    // Query API until done
    while (!ple.isDone()) {
      log.debug2("Beginning at index " + index);
      
      if (facade.isAborted()) {
        log.debug2("Crawl aborted");
        return;
      }
      
      // Make URL fetcher for this request
      String url = makeApiUrl(index);
      String loggerUrl = loggerUrl(url);
      UrlFetcher uf = makeApiUrlFetcher(ple, url, loggerUrl);
      log.debug2("Request URL: " + loggerUrl);
      facade.getCrawlerStatus().addPendingUrl(loggerUrl);

      // Make request
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
        facade.getCrawlerStatus().removePendingUrl(loggerUrl);
        facade.getCrawlerStatus().signalUrlFetched(loggerUrl);
      }
      else {
        log.debug2("Stopping due to fetch result " + fr);
        Map<String, String> errors = facade.getCrawlerStatus().getUrlsWithErrors();
        if (errors.containsKey(url)) {
          errors.put(loggerUrl, errors.remove(url));
        }
        else {
          facade.getCrawlerStatus().signalErrorForUrl(loggerUrl, "Cannot fetch seed URL");
        }
        throw new CacheException("Cannot fetch seed URL");
      }
      
      // Site warning for unexpected response length
      int records = ple.getPageLength();
      if (records != EXPECTED_RECORDS_PER_RESPONSE && !siteWarning) {
        siteWarning = true;
        log.siteWarning(String.format("Unexpected number of records per response in %s: expected %d, got %d",
                                      loggerUrl,
                                      EXPECTED_RECORDS_PER_RESPONSE,
                                      records));
      }
      
      // Next batch of records
      index += records;
    }
    log.debug2(String.format("Ending with %d URLs", urlList.size()));
    if (log.isDebug3()) {
      log.debug3("Start URLs: " + urlList.toString());
    }
  }

  /**
   * <p>
   * Assembles the query URL for a given starting index.
   * </p>
   * 
   * @param startingIndex
   *          A starting index (starts at 1).
   * @return The query URL for the given starting index.
   * @since 1.67.5
   */
  protected String makeApiUrl(int startingIndex) {
    String url = String.format("%smeta/v1/pam?q=issn:%s%%20volume:%s&api_key=%s&p=%d&s=%d",
                               apiUrl,
                               issn,
                               volume,
                               API_KEY,
                               EXPECTED_RECORDS_PER_RESPONSE,
                               startingIndex);
    return url;
  }

  /**
   * <p>
   * Makes a URL fetcher for the given API request, that will parse the result
   * using the given {@link SpringerApiPamLinkExtractor} instance.
   * </p>
   * 
   * @param ple
   *          A {@link SpringerApiPamLinkExtractor} instance to parse the API
   *          response with.
   * @param url
   *          A query URL.
   * @return A URL fetcher for the given query URL.
   * @since 1.67.5
   */
  protected UrlFetcher makeApiUrlFetcher(final SpringerApiPamLinkExtractor ple,
                                         final String url,
                                         final String loggerUrl) {
    // Make a URL fetcher
    UrlFetcher uf = facade.makeUrlFetcher(url);

    // Set refetch flag
    BitSet permFetchFlags = uf.getFetchFlags();
    permFetchFlags.set(UrlCacher.REFETCH_FLAG);
    uf.setFetchFlags(permFetchFlags);
    
    // Set custom crawl rate limiter
    uf.setCrawlRateLimiter(API_CRAWL_RATE_LIMITER);
    
    // Set custom URL consumer factory
    uf.setUrlConsumerFactory(new UrlConsumerFactory() {
      @Override
      public UrlConsumer createUrlConsumer(CrawlerFacade ucfFacade,
                                           FetchedUrlData ucfFud) {
        // Make custom URL consumer
        return new SimpleUrlConsumer(ucfFacade, ucfFud) {
          @Override
          public void consume() throws IOException {
            // Apply link extractor to URL and output results into a list
            final List<String> partial = new ArrayList<String>();
            try {
              ple.extractUrls(au,
                              fud.input,
                              AuUtil.getCharsetOrDefault(fud.headers), // FIXME
                              loggerUrl, // rather than fud.origUrl
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
              // Logging
              log.debug2(String.format("Step ending with %d URLs", partial.size()));
              if (log.isDebug3()) {
                log.debug3("URLs from step: " + partial.toString());
              }
              // Output accumulated URLs to start URL list
              urlList.addAll(partial);
            }
          }
        };
      }
    });
    return uf;
  }
  
  public static final String loggerUrl(String srcUrl) {
    return srcUrl.replaceAll("&api_key=[^&]*", "");
  }
  
}
