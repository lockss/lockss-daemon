/*
 * $Id: ArchivalUnit.java,v 1.36 2006-09-16 22:55:22 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.config.Configuration;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.plugin.base.*;


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
  public String AU_BASE_URL  = "au_base_url";
  public String AU_FETCH_DELAY = "au_fetch_delay";
  public String AU_FETCH_RATE_LIMITER_SOURCE = "au_fetch_rate_limiter_source";
  public String AU_USE_CRAWL_WINDOW = "au_use_crawl_window";
  public String AU_NEW_CRAWL_INTERVAL = "au_new_crawl_interval";
  public String AU_START_URL = "au_start_url";
  public String AU_CRAWL_SPEC = "au_crawl_spec";
  public String AU_URL_NORMALIZER = "au_url_normalizer";
  public String AU_MAX_SIZE_KEY = "au_maxsize";
  public String AU_MAX_FILE_SIZE_KEY = "au_max_file_size";
  public String AU_TITLE = "au_title";


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
   * Return the {@link CachedUrlSet} representing the entire contents
   * of this AU
   * @return the top-level {@link CachedUrlSet}
   */
  public CachedUrlSet getAuCachedUrlSet();

  /**
   * Return the {@link CrawlSpec}
   * @return the {@link CrawlSpec} for the AU
   */
  public CrawlSpec getCrawlSpec();

  /**
   * Return stems (protocol and host) of URLs in the AU.  Used for external
   * proxy configuration.  All URLs in the AU much match at least one stem;
   * it's okay for there to be matching URLs that aren't in the AU.
   * @return a Collection of URL stems
   */
  public Collection getUrlStems();

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
   * Sleeps for the interval needed between requests to the server
   */
  public void pauseBeforeFetch();

  /**
   * Return the RateLimiter for page fetches from the publisher's server.
   * Will be called when AU is started or reconfigured.  May return an
   * AU-local limiter, a plugin-local limiter, or any other shared limiter.
   * @return the RateLimiter
   */
  public RateLimiter findFetchRateLimiter();

  /**
   * Return a list of urls which need to be recrawled during a new content
   * crawl.
   * @return the {@link List} of urls to crawl
   */
  public List getNewContentCrawlUrls();

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
   * Get a {@link org.lockss.crawler.ContentParser} that knows how to parse
   * the given content type
   * @param contentType content type to get a content parser for
   * @return a content parser for contentType if we have one, null otherwise
   */
  public ContentParser getContentParser(String contentType);

  /**
   * Return the {@link FilterRule} for the given contentType or null if there
   * is none
   * @param contentType content type of the content we are going to filter
   * @return {@link FilterRule} for the given contentType or null if there
   * is none
   */
  public FilterRule getFilterRule(String contentType);

  /**
   * Return the {@link FilterFactory} for the given contentType or null if
   * there is none
   * @param contentType content type of the content we are going to filter
   * @return {@link FilterFactory} for the given contentType or null if
   * there is none
   */
  public FilterFactory getFilterFactory(String contentType);

  /**
   * Create a {@link CachedUrlSet}representing the content
   * with a specific {@link CachedUrlSetSpec}.
   * @param spec the {@link CachedUrlSetSpec}
   * @return the created {@link CachedUrlSet}
   */
  public CachedUrlSet makeCachedUrlSet( CachedUrlSetSpec spec);

  /**
   * Create a {@link CachedUrl} object within the set.
   * @param url the url of interest
   * @return a {@link CachedUrl} object representing the url.
   */
  public CachedUrl makeCachedUrl(String url);

  /**
   * Create a {@link UrlCacher} object within the set.
   * @param url the url of interest
   * @return a {@link UrlCacher} object representing the url.
   */
  public UrlCacher makeUrlCacher(String url);

  /**
   * Return the {@link TitleConfig} that was (or might have been) used to
   * configure this AU.
   * @return the TitleConfig, or null if this AU's configuration does not
   * match any TitleCOnfig in the title db.
   */
  public TitleConfig getTitleConfig();

  public class ConfigurationException extends Exception {
    private Throwable nestedException;

    public ConfigurationException(String msg) {
      super(msg);
    }

    public ConfigurationException(String msg, Throwable e) {
      super(msg + (e.getMessage() == null ? "" : (": " + e.getMessage())));
      this.nestedException = e;
    }

    public Throwable getNestedException() {
      return nestedException;
    }
  }
}
