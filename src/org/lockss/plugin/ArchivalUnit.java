/*
 * $Id$
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

package org.lockss.plugin;
import java.util.*;

import org.apache.oro.text.regex.*;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.extractor.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.AuCacheResultMap;
import org.lockss.plugin.base.*;
import org.lockss.rewriter.*;


/**
 * An <code>ArchivalUnit</code> represents a publication unit
 * (<i>eg</i>, a journal volume).  It:
 * <ul>
 * <li>Is the nexus of the plugin
 * <li>Is separately configurable
 * <li>Has a {@link CrawlSpec} that directs the crawler
 * </ul>
 * Plugins must provide a class that implements this (possibly by extending
 * {@link BaseArchivalUnit}).
 */
public interface ArchivalUnit {
  
  // Some of these keys are used in the plugin definition map

  static final String KEY_AU_BASE_URL  = "au_base_url";
  static final String KEY_AU_FETCH_DELAY = "au_fetch_delay";
  
  /**
   * <p>
   * One of:
   * </p>
   * <ul>
   * <li><code>au</code> (each AU has its own pool)</li>
   * <li><code>plugin</code> (pool name is the AU's plugin identifier)</li>
   * <li><code>key:<i>&lt;str&gt;</i></code> (pool name is the string
   * <i>str</i>)</li>
   * <li><code>host:<i>&lt;urlparam&gt;</i></code> (pool name is
   * <code>host:</code> followed by the value of the AU's parameter named
   * <i>urlparam</i>; if there is no such parameter or the parameter is not of
   * type URL, defaults to
   * {@link BaseArchivalUnit#PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE})</li>
   * <li><code>title_attribute:<i>attr</i></code> and
   * <code>title_attribute:<i>attr</i>:<i>dflt</i></code> (pool name is
   * <code>A:B</code> where <code>A</code> is <i>attr</i> and <code>B</code> is
   * the value of the AU's attribute named <i>attr</i>; if the attribute is
   * unset, use <i>dflt</i> as the value instead if the longer form is used,
   * otherwise defaults to
   * {@link BaseArchivalUnit#PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE})</li>
   * </ul>
   * <p>
   * The longer form of <code>title_attribute:</code> with a default value was
   * added in 1.68.4.
   * </p>
   */
  static final String KEY_AU_FETCH_RATE_LIMITER_SOURCE =
    "au_fetch_rate_limiter_source";
  
  static final String KEY_AU_USE_CRAWL_WINDOW = "au_use_crawl_window";
  static final String KEY_AU_NEW_CONTENT_CRAWL_INTERVAL = "au_new_crawl_interval";
  static final String KEY_AU_CRAWL_RULE = "au_crawl_rule";
  static final String KEY_AU_URL_NORMALIZER = "au_url_normalizer";
  static final String KEY_AU_MAX_SIZE = "au_maxsize";
  static final String KEY_AU_MAX_FILE_SIZE = "au_max_file_size";
  static final String KEY_AU_TITLE = "au_title";

  // Known to be used only in plugin definition map, not in AU.  Should be
  // moved (and renamed).

  static final String KEY_AU_START_URL = "au_start_url";



  /**
   * Return the Aus properties
   * @return TypedEntryMap
   */
  public TypedEntryMap getProperties();
  /**
   * Supply (possibly changed) configuration information to an existing AU.
   * @param config the {@link Configuration}
   * @throws ArchivalUnit.ConfigurationException
   */
  public void setConfiguration(Configuration config)
      throws ArchivalUnit.ConfigurationException;

  /**
   * Return the AU's current configuration.
   * @return a Configuration
   */
  public Configuration getConfiguration();

  /**
   * Perform site-dependent URL normalization to produce a canonical form
   * for all URLs that refer to the same entity.  This is necessary if URLs
   * in links contain any non-locative information, such as session-id.
   * The host part may not be changed.
   * @param url the url to normalize
   * @return canonical form of the URL.  Should return the argument if no
   * normalization takes place.
   */
  public String siteNormalizeUrl(String url);

  /**
   * Determine whether the url falls within the CrawlSpec.
   * @param url the url to test
   * @return true if it should be cached
   */
  public boolean shouldBeCached(String url);
  
  /**
   * Does the associated plugin want to store probe pages
   * @return true if probe page should be stored
   */
  public boolean storeProbePermission();

  /**
   * Does the associated plugin want the crawler to send Referer headers
   * @return true if Referer header should be sent
   */
  public boolean sendReferrer();

  /**
   * Return true if the URL is that of a login page.
   * @param url the url to test
   * @return true if login page URL
   * @deprecated This method is now expensive.  In a crawl context,
   * consult the AuCacheResultMap obtained from {@link
   * org.lockss.crawler.Crawler.CrawlerFacade}.  See, e.g., {@link
   * org.lockss.plugin.base.BaseUrlFetcher#checkRedirectAction(String)}
   */
  @Deprecated
  public boolean isLoginPageUrl(String url);

  /**
   * Return the {@link CachedUrlSet} representing the entire contents
   * of this AU
   * @return the top-level {@link CachedUrlSet}
   */
  public CachedUrlSet getAuCachedUrlSet();

  /**
   * Return stems (protocol and host) of URLs in the AU.  Used for external
   * proxy configuration.  All URLs in the AU much match at least one stem;
   * it's okay for there to be matching URLs that aren't in the AU.
   * @return a Collection of URL stems
   */
  public Collection<String> getUrlStems();

  /**
   * Returns the plugin to which this AU belongs
   * @return the plugin
   */
  public Plugin getPlugin();

  /**
   * Returns a unique string identifier for the {@link Plugin}.
   * @return a unique id
   */
  public String getPluginId();

  /**
   * Returns a globally unique string identifier for the
   * <code>ArchivalUnit</code>.  This must be completely determined by
   * the subset of the AU's configuration info that's necessary to identify the
   * AU.
   * @return a unique id
   */
  public String getAuId();

  /**
   * Returns a human-readable name for the <code>ArchivalUnit</code>.  This is
   * used in messages, so it is desirable that it succinctly identify the AU,
   * but it is not essential that it be unique.
   * @return the AU name
   */
  public String getName();

  /**
   * Return the RateLimiter for page fetches from the publisher's server.
   * Will be called when AU is started or reconfigured.  May return an
   * AU-local limiter, a plugin-local limiter, or any other shared limiter.
   * @return the RateLimiter
   * @deprecated in favor of RateLimiterInfo
   */
  public RateLimiter findFetchRateLimiter();

  /**
   * If the fetch rate limiter key is non-null, all AU with the same fetch
   * rate limiter key share a fetch rate limiter.  If the key is null the
   * AU doesn't share its fetch rate limiter.
   */
  public String getFetchRateLimiterKey();

  public RateLimiterInfo getRateLimiterInfo();


  /**
   * Return the host-independent path to look for permission pages on hosts
   * not covered by getPermissionPages().  String must start with a slash
   * @return path, or null if no such rule
   */
  public String getPerHostPermissionPath();

  /**
   * Return the list of HTTP cookies that should be sent along with every
   * request, or an empty list if none.
   */
  public List<String> getHttpCookies();

  /**
   * Return the list of HTTP request headers that should be sent along with
   * every request, or an empty list if none.
   */
  public List<String> getHttpRequestHeaders();

  /**
   * Construct the mapping from URL pattern to MIME type to use if no
   * Content-Type header is present.
   */
  public PatternStringMap makeUrlMimeTypeMap();

  /**
   * Construct the mapping from URL pattern to MIME type that validator
   * should insist on.
   */
  public PatternStringMap makeUrlMimeValidationMap();

  /**
   * Construct a PatternMap mapping redirect URLs to a
   * CacheException or CacheResultHandler
   */
  public AuCacheResultMap makeAuCacheResultMap()
      throws ArchivalUnit.ConfigurationException;

  /**
   * Construct a list of Patterns of URLs that should be excluded from
   * polls.
   */
  public List<Pattern> makeExcludeUrlsFromPollsPatterns()
      throws ArchivalUnit.ConfigurationException;

  /**
   * Construct the mapping from URL pattern to weight the URL should be
   * assigned to contribute to the overall poll result agreement.  Values
   * should be between 0.0 and 1.0
   */
  public PatternFloatMap makeUrlPollResultWeightMap()
      throws ArchivalUnit.ConfigurationException;

  /**
   * Construct a list of Patterns of non-substance URLs.  If all URLs in
   * the AU match one of these patterns the AU is considered to have no
   * substance.
   */
  public List<Pattern> makeNonSubstanceUrlPatterns()
      throws ArchivalUnit.ConfigurationException;

  /**
   * Construct a list of Patterns of substance URLs.  If any URL in the AU
   * matches one of these patterns the AU is considered to have substance.
   */
  public List<Pattern> makeSubstanceUrlPatterns()
      throws ArchivalUnit.ConfigurationException;

  /**
   * Create an instance of the plugin's substance predicate for this AU.
   * @return the SubstancePredicate or null if none.
   */
  public SubstancePredicate makeSubstancePredicate()
      throws ArchivalUnit.ConfigurationException, PluginException.LinkageError;

  /**
   * Construct a list of Patterns of hosts that should be granted implicit
   * permission for collection.
   */
  public List<Pattern> makePermittedHostPatterns()
      throws ArchivalUnit.ConfigurationException;

  /**
   * Construct a list of Patterns of URLs that should be repaired from 
   * a peer if missing on the poller, even if vote is too close.
   */
  public List<Pattern> makeRepairFromPeerIfMissingUrlPatterns()
      throws ArchivalUnit.ConfigurationException;

  /**
   * Query the {@link AuState} object to determine if this is the proper
   * time to do a new content crawl.
   * @param aus {@link AuState} object for this archival unit
   * @return true if we should do a new content crawl
   */
  public boolean shouldCrawlForNewContent(AuState aus);

  /**
   * Query the {@link AuState} object to determine if this is the proper time to
   * do a top level poll.
   * @param aus {@link AuState} object for this archival unit
   * @return true if we should do a top level poll
   */
  public boolean shouldCallTopLevelPoll(AuState aus);

  /**
   * Returns a Comparator<CrawlUrl> used to determine the order in which URLs
   * are fetched during a crawl.
   * @return the Comparator<CrawlUrl>, or null if none
   */
  public Comparator<CrawlUrl> getCrawlUrlComparator()
      throws PluginException.LinkageError;
  
  /**
   * Return a {@link LinkExtractor} that knows how to extract URLs from
   * content of the given MIME type
   * @param contentType content type to get a content parser for
   * @return A LinkExtractor or null
   */
  public LinkExtractor getLinkExtractor(String contentType);

  /**
   * Return a {@link FileMetadataExtractor} that knows how to extract URLs
   * from content of the given MIME type
   * @param target the purpose for which metadata is being extracted
   * @param contentType content type to get a content parser for
   * @return A FileMetadataExtractor or null
   */
  public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target,
							String contentType);

  /**
   * Return the {@link FilterRule} for the given contentType or null if there
   * is none
   * @param contentType content type of the content we are going to filter
   * @return {@link FilterRule} for the given contentType or null if there
   * is none
   */
  public FilterRule getFilterRule(String contentType);

  /**
   * Return the {@link FilterFactory} to be used before hashing the given
   * contentType, or null if there is none
   * @param contentType content type of the content we are going to filter
   * @return hash {@link FilterFactory} for the given contentType or null
   * if there is none
   */
  public FilterFactory getHashFilterFactory(String contentType);

  /**
   * Return the {@link FilterFactory} to be used before extracting links
   * from the given contentType, or null if there is none
   * @param contentType content type of the content we are going to filter
   * @return crawl {@link FilterFactory} for the given contentType or null
   * if there is none
   */
  public FilterFactory getCrawlFilterFactory(String contentType);

  /**
   * Return the {@link LinkRewriterFactory} for the given contentType or
   *  null if there is none
   * @param contentType content type of the content we are going to filter
   * @return {@link LinkRewriterFactory} for the given contentType or null if
   * there is none
   */
  public LinkRewriterFactory getLinkRewriterFactory(String contentType);

  /**
   * Returns the ContentValidatorFactory specified by the plugin, or null
   * @param contentType the content type
   * @return the ContentValidatorFactory for that content type
   */
  public ContentValidatorFactory getContentValidatorFactory(String contentType);

  /** Return true if the AU contains bulk content.  <i>Ie</i>, the content
   * belongs to several logical collections (titles, volumes, etc.), not
   * described by the AU's Tdb info.
   */
  public boolean isBulkContent();

  /**
   * Return the {@link ArchiveFileTypes} describing which archive (zip,
   * etc.) files should have their members exposed as pseudo-CachedUrls.
   * @return an {@link ArchiveFileTypes} or null if none
   */
  public ArchiveFileTypes getArchiveFileTypes();

  /**
   * Returns an Iterator for articles from the AU's plugin. If there isn't
   * one, an empty iterator will be returned.
   * @return the ArticleIterator
   */
  public Iterator<ArticleFiles> getArticleIterator();
  
  /**
   * Returns an Iterator for articles from the AU's plugin. If there isn't
   * one, an empty iterator will be returned.
   * @param target the purpose for which metadata is being extracted
   * @return the ArticleIterator
   */
  public Iterator<ArticleFiles> getArticleIterator(MetadataTarget target);
  
  /**
   * Create a {@link CachedUrlSet}representing the content
   * with a specific {@link CachedUrlSetSpec}.
   * @param spec the {@link CachedUrlSetSpec}
   * @return the created {@link CachedUrlSet}
   */
  public CachedUrlSet makeCachedUrlSet( CachedUrlSetSpec spec);

  /**
   * Create a {@link CachedUrl} object representing the URL within this AU
   * (even if the URL doesn't exist or is excluded by the crawl rules).
   * @param url the url of interest
   * @return a {@link CachedUrl} object representing the url.
   */
  public CachedUrl makeCachedUrl(String url);

  /**
   * Create a {@link UrlCacher} object within the set.
   * @param ud the url data of interest
   * @return a {@link UrlCacher} object representing the url.
   */
  public UrlCacher makeUrlCacher(UrlData ud);

  /**
   * Return the {@link TitleConfig} that was (or might have been) used to
   * configure this AU.
   * @return the TitleConfig, or null if this AU's configuration does not
   * match any TitleCOnfig in the title db.
   */
  public TitleConfig getTitleConfig();

  /**
   * Return the {@link TdbAu} corresponding to this AU.
   * @return the TdbAu, or null if this AU's configuration does not
   * match any TdbAu in the Tdb.
   */
  public TdbAu getTdbAu();

  /**
   * Returns a list of URLs that may contain the desired feature (e.g.,
   * au_title, au_volume, au_issue) */
  public List<String> getAuFeatureUrls(String auFeature);
  
  /**
   * Return genaric or plugin specific crawl seed to begin crawl from
   */
  public CrawlSeed makeCrawlSeed(Crawler.CrawlerFacade crawlFacade);
  
  public UrlFetcher makeUrlFetcher(CrawlerFacade facade, String url);
  
  /**
   * Return URLs suitable for browsing the AU.  Defaults to start URLs
   * unless plugin sets (@value
   * DefinablePlugin.KEY_PLUGIN_ACCESS_URL_FACTORY} to the name of a {@link
   * FeatureUrlHelperFactory}.  Plugins that synthesize and store index pages
   * should include their URLs as access URLs only if they actually exist:
   * as they don't exist on the publisher, they're useful for browsing only
   * if they exist locally.
   */
  public Collection<String> getAccessUrls();

  public boolean inCrawlWindow();
  
  public List<PermissionChecker> makePermissionCheckers();
  
  /**
   * Collection of start urls may be null
   */
  public Collection<String> getStartUrls();
  
  /**
   * Collection of permission urls may be null 
   */
  public Collection<String> getPermissionUrls();
  
  public int getRefetchDepth();
  
  public LoginPageChecker getLoginPageChecker();
  
  public String getCookiePolicy();
  
  public boolean shouldRefetchOnCookies();
  
  public CrawlWindow getCrawlWindow();
  
  public UrlConsumerFactory getUrlConsumerFactory();
  
  @SuppressWarnings("serial")
  public class ConfigurationException extends Exception {

    public ConfigurationException(String msg) {
      super(msg);
    }

    public ConfigurationException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }
}
