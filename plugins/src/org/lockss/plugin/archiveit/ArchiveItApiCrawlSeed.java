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

package org.lockss.plugin.archiveit;

import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.state.AuState;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * <p>
 * A crawl seed that queries Archive It's Web Archiving Systems API to enumerate
 * articles and synthesize start URLs for crawls.
 * </p>
 *
 * @since 1.67.5
 * @see https://support.archive-it.org/hc/en-us/articles/360015225051-Find-and-download-your-WARC-files-with-WASAPI
 */
public class ArchiveItApiCrawlSeed extends BaseCrawlSeed {

  private static final Logger log = Logger.getLogger(ArchiveItApiCrawlSeed.class);

  /**
   * <p>
   * Don't cause the crawl to fail if any of our potentially thousands of urls in the crawl seed fail.
   * </p>
   *
   * @since 1.67.5
   */
  @Override
  public boolean isFailOnStartUrlError() {
    return false;
  }

  /**
   * <p>
   * The API URL (<code>api_url</code>) of this crawl seed's AU.
   * </p>
   *
   * @since 1.67.5
   */
  protected static final String API_URL = "https://warcs.archive-it.org/";

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
  protected String organization;

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
  public ArchiveItApiCrawlSeed(Crawler.CrawlerFacade facade) {
    super(facade);
    if (au == null) {
      throw new IllegalArgumentException("Valid archival unit required for crawl seed");
    }
    this.facade = facade;
  }

  @Override
  protected void initialize()
      throws ArchivalUnit.ConfigurationException, PluginException, IOException {
    this.baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    this.collection = au.getConfiguration().get(ConfigParamDescr.COLLECTION.getKey());
    this.organization = au.getConfiguration().get("organization");
    this.fetchUrls = null;
    this.allUrls = null;
    // synthetic url, if you want to update the pattern you must update it in all of these places
    // 1. ArchiveItApiPlugin - crawl_rules
    // 2. ArchiveItApiCrawlSeed.initialize()
    // 3. ArchiveItApiFeatureUrlHelperFactory.getSyntheticUrl()
    storeUrl = baseUrl +
        "organization=" + UrlUtil.encodeUrl(organization) +
        "&collection=" + UrlUtil.encodeUrl(collection);
  }

  @Override
  public Collection<String> doGetStartUrls() throws PluginException, IOException {
    AuState aus = AuUtil.getAuState(au);
    populateUrlList();
    if (fetchUrls.isEmpty() && !aus.hasCrawled()) {
      throw new CacheException.UnexpectedNoRetryFailException("Found no start urls, and AU has not crawled before.");
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
    fetchUrls = new ArrayList<>();
    allUrls = new ArrayList<>();
    // Initialization
    int page=1;
    ArchiveItApiJsonLinkExtractor aijle = new ArchiveItApiJsonLinkExtractor();

    // Query API until done
    while (!aijle.isDone()) {

      if (facade.isAborted()) {
        log.debug2("Crawl aborted");
        return;
      }

      // Make URL fetcher for this request
      String url = makeApiUrl(page);
      UrlFetcher uf = makeApiUrlFetcher( aijle, url);
      facade.getCrawlerStatus().addPendingUrl(url);

      // Make request
      UrlFetcher.FetchResult fr;
      try {
        fr = uf.fetch();
      }
      catch (CacheException ce) {
        log.debug2("Stopping due to fatal CacheException", ce);
        Throwable cause = ce.getCause();
        if (cause != null && IOException.class.equals(cause.getClass())) {
          throw (IOException)cause; // Unwrap IOException
        } else {
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
    return String.format("%swasapi/v1/webdata?collection=%s&page=%d",
      API_URL,
      collection,
      page);
  }

  protected UrlFetcher makeApiUrlFetcher(final ArchiveItApiJsonLinkExtractor aijle,
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
            List<Map.Entry<String, String>> warcUrlsAndTimes = new ArrayList<>();
            try {
              String au_cset = AuUtil.getCharsetOrDefault(fud.headers);
              String cset = CharsetUtil.guessCharsetFromStream(fud.input, au_cset);
              aijle.extractUrls(au,
                  fud.input,
                  cset,
                  fud.origUrl,
                  urlAndTime -> warcUrlsAndTimes.add(urlAndTime)
              );
            }
            catch (IOException | PluginException ioe) {
              log.debug2("Link extractor threw", ioe);
              throw new IOException("Error while parsing PAM response for " + url, ioe);
            }
            finally {
              // Logging
              log.debug2(String.format("Step ending with %d URLs", warcUrlsAndTimes.size()));
              List<String> toFetchWarcs = getOnlyNeedFetchedUrls(warcUrlsAndTimes);
              log.debug2(String.format("Needing to fetch or refetch %d URLS", toFetchWarcs.size()));
              List<String> allWarcs = getAllUrls(warcUrlsAndTimes);
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
      sb.append("<h1>").append(au.getTdbAu().getName()).append("</h1>");
    } catch (NullPointerException npe) {
      log.debug2("could not get name from tdb au");
      sb.append("<h1>").append(au.getName()).append("</h1>");
    }
    sb.append("<h3>Collected and preserved urls:</h3>");
    for (String u : urlList) {
      sb.append("<a href=\"/ViewContent?url=")
        .append(URLEncoder.encode(u, Constants.ENCODING_UTF_8))
        .append("&frame=content&auid=")
        .append(URLEncoder.encode(au.getAuId(), Constants.ENCODING_UTF_8))
        .append("\">")
        .append(u)
        .append("</a><br/>\n");
    }
    sb.append("</html>");
    CIProperties headers = new CIProperties();
    //Should use a constant here
    headers.setProperty("content-type", "text/html; charset=utf-8");
    UrlData ud = new UrlData(
      new ByteArrayInputStream(sb.toString().getBytes(Constants.ENCODING_UTF_8)),
      headers,
      url
    );
    UrlCacher cacher = facade.makeUrlCacher(ud);
    cacher.storeContent();
  }

  protected List<String> getOnlyNeedFetchedUrls(List<Map.Entry<String, String>> urlsAndTimes) {
    boolean storeIt;
    // lets check if this url is already stored
    CachedUrlSet cachedUrls = au.getAuCachedUrlSet();
    List<String> needFetched = new ArrayList<>();
    String url;
    String crawlTime;
    for (Map.Entry<String, String> urlAndTime : urlsAndTimes) {
      url = urlAndTime.getKey();
      crawlTime = urlAndTime.getValue();
      storeIt = true;
      if (cachedUrls.containsUrl(url)) {
        CachedUrl cu = au.makeCachedUrl(url);
        // hasContent() check accesses disk, so we need to close the cu afterwards!
        try {
          if (cu.hasContent()) {
            // lets check if the WASAPI content has been updated since the last time this url was collected
            ZonedDateTime crawlTimeDT = ZonedDateTime.parse(crawlTime, DateTimeFormatter.ISO_DATE_TIME);
            String lastModified = cu.getProperties().getProperty(CachedUrl.PROPERTY_LAST_MODIFIED);
            // Thu, 26 Aug 2021 18:21:55 GMT
            ZonedDateTime lastModifiedDT = ZonedDateTime.parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME);

            if (crawlTimeDT.isBefore(lastModifiedDT)) {
              // the content on WASAPI is older than the stored content, no need to refetch
              log.debug2("Archive It file is older than stored content. Skipping.");
              storeIt = false;
            }
            if (log.isDebug3()) {
              log.debug3("warc file found in CachedUrls: " + url);
              log.debug3("  with       WASAPI crawlTime: " + crawlTimeDT );
              log.debug3("  and CachedUrl Last-Modified: " + lastModifiedDT );
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

  protected List<String> getAllUrls(List<Map.Entry<String, String>> urlsAndTimes) {
    List<String> urls = new ArrayList<>();
    for (Map.Entry<String, String> urlAndTime : urlsAndTimes) {
      urls.add(urlAndTime.getKey());
    }
    return urls;
  }

}
