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

package org.lockss.plugin.jasper;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang3.StringUtils;
import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.*;
import org.lockss.daemon.AuParamType.InvalidFormatException;
import org.lockss.plugin.*;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.plugin.base.*;
import org.lockss.state.AuState;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A crawl seed that queries Archive It's Web Archiving Systems API to enumerate
 * articles and synthesize start URLs for crawls.
 * </p>
 *
 * @since 1.67.5
 * @see https://support.archive-it.org/hc/en-us/articles/360015225051-Find-and-download-your-WARC-files-with-WASAPI
 */
public class JasperCrawlSeed extends BaseCrawlSeed {

  private static final Logger log = Logger.getLogger(JasperCrawlSeed.class);

  /**
   * <p>
   * The API URL (<code>api_url</code>) of this crawl seed's AU.
   * </p>
   *
   * @since 1.67.5
   */
  protected static final String API_URL = "https://archive.org/metadata/";

  protected Crawler.CrawlerFacade facade;

  /**
   * <p>
   * This crawl seed's list of start URLs.
   * </p>
   *
   * @since 1.67.5
   */
  protected List<String> fetchUrls;


  /**
   * <p>
   * All urls in this au, used to construct a synthetic landing page.
   * </p>
   *
   * @since 1.67.5
   */
  protected List<String> allUrls;

  protected String baseUrl;
  protected String collection;
  protected String storeUrl;

  /**
   * <p>
   * Builds a new crawl seed with the given crawler façade.
   * </p>
   *
   * @param facade
   *          A crawler façade for this crawl seed.
   * @since 1.67.5
   */
  public JasperCrawlSeed(Crawler.CrawlerFacade facade) {
    super(facade);
    this.facade = facade;
  }

  @Override
  protected void initialize() {
    this.baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    this.collection = au.getConfiguration().get(ConfigParamDescr.COLLECTION.getKey());
    this.fetchUrls = new ArrayList<>();
    this.allUrls = null;
    // synthetic url, if you want to update the pattern you must update it in all of these places
    // 1. crawl_rules & start_url
    // 2. JasperCrawlSeed.initialize()
    this.storeUrl = baseUrl
        + "ProjectJasper?collection="
        + UrlUtil.encodeUrl(collection);
  }

  @Override
  public Collection<String> doGetStartUrls() throws PluginException, IOException {
    AuState aus = AuUtil.getAuState(au);
    populateUrlList();
    if (fetchUrls.isEmpty() && !aus.hasCrawled()) {
      // maybe a deepcrawl check ?
      // facade.getCrawlerStatus().getRefetchDepth() > 2
      throw new CacheException.UnexpectedNoRetryFailException("Found no start urls");
    }
    return fetchUrls;
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
    allUrls = new ArrayList<>();
    // Initialization
    int page=1;
    JasperJsonLinkExtractor jjle = new JasperJsonLinkExtractor();
    // Query API until done
    while (!jjle.isDone()) {

      if (facade.isAborted()) {
        log.debug2("Crawl aborted");
        return;
      }

      // Make URL fetcher for this request
      String url = makeApiUrl(page);
      UrlFetcher uf = makeApiUrlFetcher(jjle, url);
      facade.getCrawlerStatus().addPendingUrl(url);

      // Make request
      UrlFetcher.FetchResult fr = null;
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
      if (fr == UrlFetcher.FetchResult.FETCHED) {
        facade.getCrawlerStatus().removePendingUrl(url);
        facade.getCrawlerStatus().signalUrlFetched(url);
      }
      else {
        log.debug2("Stopping due to fetch result " + fr);
        Map<String, String> errors = facade.getCrawlerStatus().getUrlsWithErrors();
        if (errors.containsKey(url)) {
          errors.put(url, errors.remove(url));
        }
        else {
          facade.getCrawlerStatus().signalErrorForUrl(url, "Cannot fetch seed URL");
        }
        throw new CacheException("Cannot fetch seed URL");
      }
      page+=1;

    }
    Collections.sort(allUrls);
    makeStartUrlContent(allUrls, storeUrl);
  }

  protected String makeApiUrl(int page) {
    String url = String.format("%s%s",
        API_URL,
        collection
    );
    return url;

  };
  protected UrlFetcher makeApiUrlFetcher(final JasperJsonLinkExtractor jjle,
                                         final String url) {
    // Make a URL fetcher
    UrlFetcher uf = facade.makeUrlFetcher(url);

    // Set refetch flag // markom- not sure this is necessary?
    BitSet permFetchFlags = uf.getFetchFlags();
    permFetchFlags.set(UrlCacher.REFETCH_FLAG);
    uf.setFetchFlags(permFetchFlags);
    // set header to application/json and encoding per WASAPI recommendation
    uf.setRequestProperty(
        "Accept",
        "application/json"
    );
    // Set custom URL consumer factory, using custom link extractor
    uf.setUrlConsumerFactory(new UrlConsumerFactory() {
      @Override
      public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade ucfFacade,
                                           FetchedUrlData ucfFud) {
        // Make custom URL consumer
        return new SimpleUrlConsumer(ucfFacade, ucfFud) {
          @Override
          public void consume() throws IOException {
            // Apply link extractor to URL and output results into a list
            List<Map.Entry<String, Integer>> urlsAndTimes = new ArrayList<>();
            try {
              String au_cset = AuUtil.getCharsetOrDefault(fud.headers);
              String cset = CharsetUtil.guessCharsetFromStream(fud.input, au_cset);
              jjle.extractUrls(au,
                  fud.input,
                  cset,
                  fud.origUrl,
                  url1 -> urlsAndTimes.add(url1));
            }
            catch (IOException | PluginException ioe) {
              log.debug2("Link extractor threw", ioe);
              throw new IOException("Error while parsing PAM response for " + url, ioe);
            }
            finally {
              // Logging
              log.debug2(String.format("Step ending with %d URLs", urlsAndTimes.size()));
              List allWarcs = getAllUrls(urlsAndTimes);
              List toFetchWarcs = getOnlyNeedFetchedUrls(urlsAndTimes);
              log.debug2(String.format("Needing to fetch or refetch %d URLS", toFetchWarcs.size()));
              // Output accumulated URLs to start URL list
              if (toFetchWarcs != null) {
                fetchUrls.addAll(toFetchWarcs);
              }
              if (allWarcs != null) {
                allUrls.addAll(allWarcs);
              }
            }
          }
        };
      }
    });
    return uf;
  }

  /**
   * <p>
   *  Makes an HTML page of the list of 'start urls.'
   *  This functions as the start_url.
   * </p>
   * @param urlList
   * @param url
   * @throws IOException
   */
  protected void makeStartUrlContent(Collection<String> urlList,
                                     String url)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("<html>\n");
    try {
      sb.append("<h1>" + au.getTdbAu().getName() + "</h1>");
    } catch (NullPointerException npe) {
      log.debug2("could not get name from tdb au");
      sb.append("<h1>" + au.getName() + "</h1>");
    }
    sb.append("<h3>Collected and preserved urls:</h3>");
    for (String u : urlList) {
      sb.append("<a href=\"" + u + "\">" + u + "</a><br/>\n");
    }
    sb.append("</html>");
    CIProperties headers = new CIProperties();
    //Should use a constant here
    headers.setProperty("content-type", "text/html; charset=utf-8");
    UrlData ud = new UrlData(new ByteArrayInputStream(
                                  sb.toString().getBytes(
                                      Constants.ENCODING_UTF_8
                                  )
                             ),
                             headers,
                             url
    );
    UrlCacher cacher = facade.makeUrlCacher(ud);
    cacher.storeContent();
  }

  protected List<String> getOnlyNeedFetchedUrls(List<Map.Entry<String, Integer>> urlsAndTimes) {
    boolean storeIt;
    // lets check if this url is already stored
    CachedUrlSet cachedUrls = au.getAuCachedUrlSet();
    List<String> needFetched = new ArrayList<>();
    String url;
    Integer crawlTime;
    for (Map.Entry<String, Integer> urlAndTime : urlsAndTimes) {
      url = urlAndTime.getKey();
      crawlTime = urlAndTime.getValue();
      storeIt = true;
      if (cachedUrls.containsUrl(url)) {
        CachedUrl cu = au.makeCachedUrl(url);
        // hasContent() check accesses disk, so we need to close the cu afterwards!
        try {
          if (cu.hasContent()) {
            // lets check if the archived content has been updated since the last time this url was collected
            // convert epoch seconds to zoneddatetime
            ZonedDateTime crawlTimeDT = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(crawlTime.longValue()), ZoneOffset.UTC);
            String lastModified = cu.getProperties().getProperty(CachedUrl.PROPERTY_LAST_MODIFIED);
            // Thu, 26 Aug 2021 18:21:55 GMT
            ZonedDateTime lastModifiedDT = ZonedDateTime.parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME);

            if (crawlTimeDT.isBefore(lastModifiedDT) || crawlTimeDT.isEqual(lastModifiedDT)) {
              // the content on Archive It has not been updated, no need to refetch
              log.debug2("Archive It file has not been updated since last crawl. Skipping.");
              storeIt = false;
            }
            if (log.isDebug3()) {
              log.debug3("file found in CachedUrls: " + url);
              log.debug3(" with          crawlTime: " + crawlTimeDT );
              log.debug3(" CachedUrl Last-Modified: " + lastModifiedDT );
            }
          }
        } finally {
          // close the cu
          cu.release();
        }
      }
      if (storeIt) {
        needFetched.add(url);
      }
    }
    return needFetched;
  }
  protected List<String> getAllUrls(List<Map.Entry<String, Integer>> urlsAndTimes) {
    List<String> urls = new ArrayList<>();
    for (Map.Entry<String, Integer> urlAndTime : urlsAndTimes) {
      urls.add(urlAndTime.getKey());
    }
    return urls;
  }

  /**
   * Since we intentionally do not provide starturls for all
   * crawl requests, we return false here.
   */
  public boolean isFailOnStartUrlError() {
    return false;
  }
}
