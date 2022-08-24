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

package org.lockss.plugin.springer.link;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.lockss.state.AuState;
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
public abstract class BaseSpringerLinkCrawlSeed extends BaseCrawlSeed {

  /**
   * <p>
   * A logger for this class.
   * </p>
   * 
   * @since 1.67.5
   */
  private static final Logger log = Logger.getLogger(BaseSpringerLinkCrawlSeed.class);

  private static String API_KEY;

  private void loadApiKey() throws PluginException {
    try (InputStream is =
         BaseSpringerLinkCrawlSeed.class.getResourceAsStream("api-key.txt")) {
      if (is == null) {
        throw new PluginException("Plugin external not found");
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(is, Constants.ENCODING_US_ASCII));
      API_KEY = br.readLine();
      if (StringUtils.isEmpty(API_KEY)) {
        throw new PluginException("Plugin external not loaded");
      }
    }
    catch (IOException ioe) {
      throw new PluginException("Error reading plugin external", ioe);
    }
  }

  protected String getApiKey() throws PluginException {
    if (API_KEY == null) {
      loadApiKey();
    }
    return API_KEY;
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
  protected static final CrawlRateLimiter API_CRAWL_RATE_LIMITER =
      new FileTypeCrawlRateLimiter(
          new RateLimiterInfo(BaseSpringerLinkCrawlSeed.class.getSimpleName(),
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
   * <p>
   * The protocol was changed to HTTPS on 2016-03-18.
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final String API_URL = "https://api.springer.com/";
    
  protected CrawlerFacade facade;

  /**
   * <p>
   * This crawl seed's list of start URLs.
   * </p>
   * 
   * @since 1.67.5
   */
  protected List<String> urlList;
  
  protected String baseUrl;

  /**
   * <p>
   * Builds a new crawl seed with the given crawler façade.
   * </p>
   * 
   * @param facade
   *          A crawler façade for this crawl seed.
   * @since 1.67.5
   */
  public BaseSpringerLinkCrawlSeed(CrawlerFacade facade) {
    super(facade);
    if (au == null) {
      throw new IllegalArgumentException("Valid archival unit required for crawl seed");
    }
    this.facade = facade;
  }

  @Override
  protected void initialize()
      throws ConfigurationException, PluginException, IOException {
    this.baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
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
  protected void populateUrlList() throws IOException, PluginException {
	AuState aus = AuUtil.getAuState(au);
	urlList = new ArrayList<String>();
    String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());
	//In order to query the metadata service less if this is a normal
	//recrawl and we think the intial crawl was good just grab all the start 
	//URLs from the AU
	if(aus.hasCrawled() && au.getRefetchDepth() < 2 && !aus.hasNoSubstance()) {
		CachedUrlSet contents = au.getAuCachedUrlSet();
		CuIterable contentIter = contents.getCuIterable();
		Pattern articlePattern = Pattern.compile("/article/[^/]+/[^/.]+$", Pattern.CASE_INSENSITIVE);
		for(CachedUrl cu : contentIter) {
			String url = cu.getUrl();
			Matcher mat = articlePattern.matcher(url);
			if(mat.find()) {
				urlList.add(url);
			}
		}
	} else {
	
	    // Initialization
	    boolean siteWarning = false; // Flag to log the potential siteWarning only once
	    int index = 1; // API numbers records starting with 1
	    SpringerLinkPamLinkExtractor ple = new SpringerLinkPamLinkExtractor();
	    
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
	        if(ce.getCause() != null && ce.getCause().getMessage().contains("LOCKSS")) {
                  log.debug("OAI result errored due to LOCKSS audit proxy. Trying alternate start Url", ce);
                  urlList.add(storeUrl);
                  return;
                } else {
      	        log.debug2("Stopping due to fatal CacheException", ce);
      	        Throwable cause = ce.getCause();
      	        if (cause != null && IOException.class.equals(cause.getClass())) {
      	          throw (IOException)cause; // Unwrap IOException
      	        }
      	        else {
      	          throw ce;
      	        }
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
	}
	Collections.sort(urlList);
	storeStartUrls(urlList, storeUrl);
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
  protected abstract String makeApiUrl(int startingIndex) throws PluginException;

  /**
   * <p>
   * Makes a URL fetcher for the given API request, that will parse the result
   * using the given {@link SpringerLinkPamLinkExtractor} instance.
   * </p>
   * 
   * @param ple
   *          A {@link SpringerLinkPamLinkExtractor} instance to parse the API
   *          response with.
   * @param url
   *          A query URL.
   * @return A URL fetcher for the given query URL.
   * @since 1.67.5
   */
  protected UrlFetcher makeApiUrlFetcher(final SpringerLinkPamLinkExtractor ple,
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
            final Set<String> partial = new HashSet<String>();
            try {
              String au_cset = AuUtil.getCharsetOrDefault(fud.headers);
              String cset = CharsetUtil.guessCharsetFromStream(fud.input,au_cset);
              //FIXME 1.69 
              // Once guessCharsetFromStream correctly uses the hint instead of returning null
              // this local bit won't be needed.
              if (cset == null) {
                cset = au_cset;
              }
              //
              ple.extractUrls(au,
                              fud.input,
                              cset,
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
              throw new IOException("Error while parsing PAM response for " + loggerUrl, ioe);
            }
            finally {
              // Logging
              log.debug2(String.format("Step ending with %d URLs", partial.size()));
              if (log.isDebug3()) {
                log.debug3("URLs from step: " + partial.toString());
              }
              // Output accumulated URLs to start URL list
              urlList.addAll(convertDoisToUrls(partial));
            }
          }
        };
      }
    });
    return uf;
  }
  
  protected void storeStartUrls(Collection<String> urlList, String url) throws IOException {
      StringBuilder sb = new StringBuilder();
      sb.append("<html>\n");
      for (String u : urlList) {
              sb.append("<a href=\"" + u + "\">" + u + "</a><br/>\n");
      }
      sb.append("</html>");
      CIProperties headers = new CIProperties();
      //Should use a constant here
      headers.setProperty("content-type", "text/html; charset=utf-8");
      UrlData ud = new UrlData(new ByteArrayInputStream(sb.toString().getBytes(Constants.ENCODING_UTF_8)), headers, url);
      UrlCacher cacher = facade.makeUrlCacher(ud);
      cacher.storeContent();
  }
  
  public boolean isFailOnStartUrlError() {
    return false;
  }
  
  /**
   * <p>
   * Encode a DOI for use in URLs, using the encoding of
   * <code>application/x-www-form-urlencoded</code> and {@link URLEncoder},
   * except that a space (<code>' '</code>) is encoded as <code>"%20"</code>
   * rather than <code>'+'</code>.
   * </p>
   * 
   * @param doi
   *          A DOI.
   * @return An encoded DOI (URL-encoded with <code>"%20"</code> for a space).
   * @since 1.67.5
   */
  public static String encodeDoi(String doi) {
    try {
      return URLEncoder.encode(doi, Constants.ENCODING_UTF_8).replace("+", "%20");
    }
    catch (UnsupportedEncodingException uee) {
      throw new ShouldNotHappenException("Could not URL-encode '" + doi + "' as UTF-8");
    }
  }
  
  public static final String loggerUrl(String srcUrl) {
    return srcUrl.replaceAll("&api_key=[^&]*", "");
  }
  

  protected abstract List<String> convertDoisToUrls(Collection<String> dois);
  
}
