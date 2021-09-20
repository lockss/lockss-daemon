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
public class ArchiveItCrawlSeed extends BaseCrawlSeed {

  private static final Logger log = Logger.getLogger(ArchiveItCrawlSeed.class);

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
  protected List<String> urlList;

  protected String baseUrl;
  protected String collection;

  /**
   * <p>
   * Builds a new crawl seed with the given crawler façade.
   * </p>
   *
   * @param facade
   *          A crawler façade for this crawl seed.
   * @since 1.67.5
   */
  public ArchiveItCrawlSeed(Crawler.CrawlerFacade facade) {
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
    this.urlList = null;
  }

  @Override
  public Collection<String> doGetStartUrls() throws PluginException, IOException {
    if (urlList == null) {
      populateUrlList();
    }
    if (urlList.isEmpty()) {
      throw new CacheException.UnexpectedNoRetryFailException("Found no start urls");
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
    AuState aus = AuUtil.getAuState(au);
    urlList = new ArrayList<String>();
    String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());
    //In order to query the metadata service less if this is a normal
    //recrawl and we think the intial crawl was good just grab all the start
    //URLs from the AU
    if (aus.hasCrawled() && au.getRefetchDepth() < 2 && !aus.hasNoSubstance()) {
      log.debug3("au hasCrawled, has Substance, and will not be refectched");

      CachedUrlSet contents = au.getAuCachedUrlSet();
      CuIterable contentIter = contents.getCuIterable();
      // https://warcs.archive-it.org/webdatafile/ARCHIVEIT-7711-TEST-JOB1295343-SEED2424423-20201009202340983-00001-h3.warc.gz
      Pattern articlePattern = Pattern.compile("/webdatafile/[^/.]+\\.warc\\.gz$", Pattern.CASE_INSENSITIVE);
      for (CachedUrl cu : contentIter) {
        String url = cu.getUrl();
        Matcher mat = articlePattern.matcher(url);
        if (mat.find()) {
          urlList.add(url);
        }
      }
    } else {

      // Initialization
      int page = 1;
      ArchiveItJsonLinkExtractor aijle = new ArchiveItJsonLinkExtractor();

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
    }
    Collections.sort(urlList);
    makeStartUrlContent(urlList, storeUrl);
    log.debug2(String.format("Ending with %d URLs", urlList.size()));
    if (log.isDebug3()) {
      log.debug3("Start URLs: " + urlList.toString());

    }
  }

  protected String makeApiUrl(int page) {
    String url = String.format("%swasapi/v1/webdata?collection=%s&page=%d",
        API_URL,
        collection,
        page);
    return url;

  };
  protected UrlFetcher makeApiUrlFetcher(final ArchiveItJsonLinkExtractor aijle,
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
    uf.setRequestProperty(
        "Accept-Encoding",
        "gzip, deflate, br"
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
            final Set<String> warcUrls = new HashSet<String>();
            try {
              String au_cset = AuUtil.getCharsetOrDefault(fud.headers);
              String cset = CharsetUtil.guessCharsetFromStream(fud.input,au_cset);
              //FIXME 1.69
              // Once guessCharsetFromStream correctly uses the hint instead of returning null
              // this local bit won't be needed.
              if (cset == null) {
                cset = au_cset;
              }
              aijle.extractUrls(au,
                  fud.input,
                  cset,
                  fud.origUrl,
                  url1 -> warcUrls.add(url1));
            }
            catch (IOException | PluginException ioe) {
              log.debug2("Link extractor threw", ioe);
              throw new IOException("Error while parsing PAM response for " + url, ioe);
            }
            finally {
              // Logging
              log.debug2(String.format("Step ending with %d URLs", warcUrls.size()));
              if (log.isDebug3()) {
                log.debug3("URLs from step: " + warcUrls.toString());
              }
              // Output accumulated URLs to start URL list
              urlList.addAll(warcUrls);
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

}
