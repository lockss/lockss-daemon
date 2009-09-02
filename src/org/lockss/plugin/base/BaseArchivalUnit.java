/*
 * $Id: BaseArchivalUnit.java,v 1.134.4.3 2009-09-02 01:35:36 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.base;

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.collections.map.LRUMap;

import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.extractor.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.rewriter.*;
import org.lockss.state.AuState;
import org.lockss.util.*;

/**
 * Abstract base class for ArchivalUnits.
 * Plugins may extend this to get some common ArchivalUnit functionality.
 */
public abstract class BaseArchivalUnit implements ArchivalUnit {
  static Logger logger = Logger.getLogger("BaseArchivalUnit");

  public static final long
    DEFAULT_FETCH_DELAY = 6 * Constants.SECOND;

  /** Minimum fetch delay.  Plugin-specified fetch delay may be used only
   * to increase the delay. */
  public static final String PARAM_MIN_FETCH_DELAY =
    Configuration.PREFIX+"baseau.minFetchDelay";
  public static final long DEFAULT_MIN_FETCH_DELAY = 6 * Constants.SECOND;;

  /** Default fetch rate limiter source for plugins that don't specify
   * au_fetch_rate_limiter_source.  Can be "au" or "plugin"; default is
   * "au". */
  public static final String PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE =
    Configuration.PREFIX+"baseau.defaultFetchRateLimiterSource";
  public static final String DEFAULT_DEFAULT_FETCH_RATE_LIMITER_SOURCE = "au";

  //Short term conf parameter to get around the fact that DefinablePlugins
  //don't load crawl windows
  public static final String PARAM_USE_CRAWL_WINDOW_BY_DEFAULT =
    Configuration.PREFIX+"baseau.useCrawlWindowByDefault";
  public static final boolean DEFAULT_USE_CRAWL_WINDOW_BY_DEFAULT = true;

  public static final String USE_CRAWL_WINDOW = "use_crawl_window";
  private static final boolean DEFAULT_USE_CRAWL_WINDOW = false;

  public static final String KEY_NEW_CONTENT_CRAWL_INTERVAL = "nc_interval";
  public static final String KEY_PAUSE_TIME = "pause_time";
  static final public String KEY_AU_DEFAULT_NEW_CONTENT_CRAWL_INTERVAL = "au_def_new_content_crawl";
  static final public String KEY_AU_DEFAULT_PAUSE_TIME = "au_def_pause_time";
  static final public String KEY_AU_CONFIG_USER_MSG = "au_config_user_msg";

  static final public String PREFIX_AU_SHORT_YEAR = "au_short_";
  static final public String SUFFIX_AU_HOST = "_host";
  static final public String SUFFIX_AU_PATH = "_path";

  public static final long
      DEFAULT_NEW_CONTENT_CRAWL_INTERVAL = 2 * Constants.WEEK;

  static SimpleDateFormat sdf = new SimpleDateFormat();

  protected static final long DEFAULT_AU_MAX_SIZE = 0;
  protected static final long DEFAULT_AU_MAX_FILE_SIZE = 0;

  protected BasePlugin plugin;
  protected CrawlSpec crawlSpec;

  protected long defaultFetchDelay = DEFAULT_FETCH_DELAY;
  protected List<String> startUrls;
  protected long newContentCrawlIntv;
  protected long defaultContentCrawlIntv = DEFAULT_NEW_CONTENT_CRAWL_INTERVAL;

  protected String auName;   // the name of the AU (constructed by plugin)
  protected TitleConfig titleConfig;   // matching entry from titledb, if any
  protected String auTitle;   // the title of the AU (from titledb, if any)
  protected Configuration auConfig;
  private String auId = null;

  protected TypedEntryMap paramMap;

  protected BaseArchivalUnit(Plugin myPlugin) {
    if (!(myPlugin instanceof BasePlugin)) {
      throw new IllegalArgumentException("BaseArchivalUnit cannot be used with non-BasePlugin: " + myPlugin.getClass());
    }
    plugin = (BasePlugin)myPlugin;
    paramMap = new ParamHandlerMap();
  }

  /**
   * getProperties
   *
   * @return TypedEntryMap
   */
  public TypedEntryMap getProperties() {
    return paramMap;
  }

  protected ParamHandlerMap getParamMap() {
    return (ParamHandlerMap) paramMap;
  }

  /**
   * Checks that the configuration is legal (doesn't change any of the defining
   * properties), and stores the configuration
   * @param config new Configuration
   * @throws ArchivalUnit.ConfigurationException if the configuration change is
   * illegal or for other configuration errors
   */
  public void setConfiguration(Configuration config)
      throws ArchivalUnit.ConfigurationException {
    if (config == null) {
      throw new ConfigurationException("Null Configuration");
    }
    checkLegalConfigChange(config);
    auConfig = config;
    loadAuConfigDescrs(config);
    addImpliedConfigParams();
    setBaseAuParams(config);
    fetchRateLimiter = recomputeFetchRateLimiter(fetchRateLimiter);
  }

  public Configuration getConfiguration() {
    return auConfig;
  }

  public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
    return new BaseCachedUrlSet(this, cuss);
  }

  public CachedUrl makeCachedUrl(String url) {
    return new BaseCachedUrl(this, url);
  }

  public UrlCacher makeUrlCacher(String url) {
    return new BaseUrlCacher(this, url);
  }

  private void checkLegalConfigChange(Configuration newConfig)
      throws ArchivalUnit.ConfigurationException {
    for (Iterator iter = plugin.getAuConfigDescrs().iterator();
	 iter.hasNext();) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      if (descr.isDefinitional()) {
	String key = descr.getKey();
	String newVal = newConfig.get(key);
	if (newVal == null) {
	    throw new ConfigurationException("Missing required parameter: " +
					     key);
	}
	if (auConfig != null) {
	  String oldVal = auConfig.get(key);
	  if (!StringUtil.equalStrings(oldVal, newVal)) {
	    throw new
	      ConfigurationException("Attempt to modify defining property " +
				     "of existing ArchivalUnit: " + key +
				     ". old: "+oldVal+" new: "+newVal);
	  }
	}
      }
    }
  }

  protected void loadAuConfigDescrs(Configuration config) throws
      ConfigurationException {
    for (Iterator it = plugin.getAuConfigDescrs().iterator(); it.hasNext() ;) {
      ConfigParamDescr descr = (ConfigParamDescr) it.next();
      String key = descr.getKey();
      if (config.containsKey(key)) {
	try {
	  Object val = descr.getValueOfType(config.get(key));
	  paramMap.setMapElement(key, val);
	} catch (Exception ex) {
	  throw new ConfigurationException("Error configuring: " + key, ex);
	}
      }
    }
  }

  // This pattern
  //   T value =
  //     (config.containsKey(CONFIG_KEY)
  //      ? config.getT(CONFIG_KEY, defaultValue)
  //      : paramMap.getT(PARAM_MAP_KEY, defaultValue));
  //
  // is used to give precedence to the first of these values that is present:
  //  - value in AU's config
  //  - value already in paramMap (presumably stored by loadAuConfigDescrs)
  //  - default value

  protected void setBaseAuParams(Configuration config)
      throws ConfigurationException {

    // get the base url
    URL baseUrl = loadConfigUrl(ConfigParamDescr.BASE_URL, config);
    paramMap.putUrl(KEY_AU_BASE_URL, baseUrl);

    // get the fetch delay
    long minFetchDelay = CurrentConfig.getLongParam(PARAM_MIN_FETCH_DELAY,
						    DEFAULT_MIN_FETCH_DELAY);
    long fetchDelay =
      Math.max(minFetchDelay,
	       (config.containsKey(KEY_PAUSE_TIME)
		? config.getTimeInterval(KEY_PAUSE_TIME, defaultFetchDelay)
		: paramMap.getLong(KEY_AU_FETCH_DELAY, defaultFetchDelay)));
    logger.debug2("Set fetch delay to " + fetchDelay);
    paramMap.putLong(KEY_AU_FETCH_DELAY, fetchDelay);

    // get the new content crawl interval
    newContentCrawlIntv =
      (config.containsKey(KEY_NEW_CONTENT_CRAWL_INTERVAL)
       ? config.getTimeInterval(KEY_NEW_CONTENT_CRAWL_INTERVAL,
				defaultContentCrawlIntv)
       : paramMap.getLong(KEY_AU_NEW_CONTENT_CRAWL_INTERVAL, defaultContentCrawlIntv));
    logger.debug2("Setting new content crawl interval to " +
		  StringUtil.timeIntervalToString(newContentCrawlIntv));
    paramMap.putLong(KEY_AU_NEW_CONTENT_CRAWL_INTERVAL, newContentCrawlIntv);

    // make the start urls
    startUrls = makeStartUrls();


    // get crawl window setting
    boolean useCrawlWindow =
      (config.containsKey(USE_CRAWL_WINDOW)
       ? config.getBoolean(USE_CRAWL_WINDOW, DEFAULT_USE_CRAWL_WINDOW)
       :
       paramMap.getBoolean(KEY_AU_USE_CRAWL_WINDOW, DEFAULT_USE_CRAWL_WINDOW));
    paramMap.putBoolean(KEY_AU_USE_CRAWL_WINDOW, useCrawlWindow);


    if (CurrentConfig.getBooleanParam(PARAM_USE_CRAWL_WINDOW_BY_DEFAULT,
                                      DEFAULT_USE_CRAWL_WINDOW_BY_DEFAULT)) {
      logger.debug3(PARAM_USE_CRAWL_WINDOW_BY_DEFAULT+
                    " set to true, so using as default.");
      useCrawlWindow = true; //XXX hack for now
    }

    // make our crawl spec
    try {
      crawlSpec = makeCrawlSpec();
      if (useCrawlWindow) {
        CrawlWindow window = makeCrawlWindow();
	//XXX need to get rid of setCrawlWindow and set that in constructor
        logger.debug3("Setting crawl window to "+window);
        crawlSpec.setCrawlWindow(window);
      }
    } catch (LockssRegexpException e) {
      throw new ConfigurationException("Illegal RE", e);
    }
    paramMap.setMapElement(KEY_AU_CRAWL_SPEC, crawlSpec);

    titleDbChanged();
  }

  protected void addImpliedConfigParams()
      throws ArchivalUnit.ConfigurationException {
    for (Iterator it = plugin.getAuConfigDescrs().iterator();
	 it.hasNext() ; ) {
      ConfigParamDescr descr = (ConfigParamDescr)it.next();
      String key = descr.getKey();
      try {
	if (auConfig.containsKey(key)) {
	  Object val = descr.getValueOfType(auConfig.get(key));
	  // we store years in two formats - short and long
	  if (descr.getType() == ConfigParamDescr.TYPE_YEAR) {
	    int year = ((Integer)val).intValue() % 100;
	    paramMap.putInt(PREFIX_AU_SHORT_YEAR + key, year);
	    if (logger.isDebug3()) {
	      logger.debug3("Inferred " + PREFIX_AU_SHORT_YEAR + key +
			    " = " + year);
	    }
	  }
	  // store separate host and path of URLs
	  if (descr.getType() == ConfigParamDescr.TYPE_URL) {
	    URL url = (URL)val;
	    if(url != null) {
	      paramMap.putString(key + SUFFIX_AU_HOST, url.getHost());
	      paramMap.putString(key + SUFFIX_AU_PATH, url.getPath());
	      if (logger.isDebug3()) {
		logger.debug3("Inferred " + key + SUFFIX_AU_HOST +
			      " = " + url.getHost());
		  logger.debug3("Inferred " + key + SUFFIX_AU_PATH +
				" = " + url.getPath());
	      }
	    }
	  }
	}
      } catch (Exception ex) {
	logger.error("Adding implied config params", ex);
      }
    }
  }


  TitleConfig findTitleConfig(Configuration config) {
    return AuUtil.findTitleConfig(config, plugin);
  }

  /** Set up titledb-related data */
  protected void titleDbChanged() {
    TitleConfig tc = findTitleConfig(auConfig);
    if (tc != null) {
      titleConfig = tc;
      auTitle = titleConfig.getDisplayName();
    }
    auName = makeName();
    paramMap.putString(KEY_AU_TITLE, auTitle != null ? auTitle : auName);
  }

  public TitleConfig getTitleConfig() {
    return titleConfig;
  }

  /**
   * Returns the plugin for this AU
   * @return the plugin for this AU
   */
  public Plugin getPlugin() {
    return plugin;
  }

  /**
   * Creates id by joining the plugin id to the canonical representation of
   * the defining properties as an encoded string
   *
   * @return id by joining the plugin id to the canonical representation of
   * the defining properties as an encoded string
   */
  public final String getAuId() {
    if (auId == null) {
      auId = PluginManager.generateAuId(getPlugin(), auConfig);
    }
    return auId;
  }

  /**
   * Return the Plugin's ID.
   * @return the Plugin's ID.
   */
  public String getPluginId() {
    return plugin.getPluginId();
  }

  /**
   * Return the CrawlSpec.
   * @return the spec
   */
  public CrawlSpec getCrawlSpec() {
    // for speed we return the cached value
    return crawlSpec;
    //return (CrawlSpec)paramMap.getMapElement(AU_CRAWL_SPEC);
  }

  /**
   * Return the Url stems (proto, host & port) of potential content within
   * this AU
   * @return a List of Urls
   */
  public Collection getUrlStems() {
    try {
      List perms = getPermissionPages();
      ArrayList res = new ArrayList(perms.size());
      for (Iterator it = perms.iterator(); it.hasNext();) {
	String url = (String)it.next();
	String stem = UrlUtil.getUrlPrefix(url);
	res.add(stem);
      }
      res.trimToSize();
      return res;
    } catch (Exception e) {
      // TODO: This should throw an exception. ProxyInfo assumes that a
      // collection will be returned and makes no attempt to catch exceptions
      return Collections.EMPTY_LIST;
    }
  }

  /**
   * Determine whether the url falls within the CrawlSpec.
   * @param url the url
   * @return true if it is included
   */
  public boolean shouldBeCached(String url) {
    boolean val = getCrawlSpec().isIncluded(url);
    return val;
  }

  public boolean isLoginPageUrl(String url) {
    return false;
  }

  public String siteNormalizeUrl(String url) {
    return plugin.siteNormalizeUrl(url, this);
  }

  public Comparator<CrawlUrl> getCrawlUrlComparator()
      throws PluginException.LinkageError {
    return plugin.getCrawlUrlComparator(this);
  }

  /**
   * Return the CachedUrlSet representing the entire contents
   * of this AU
   * @return the CachedUrlSet
   */
  public CachedUrlSet getAuCachedUrlSet() {
    // tk - use singleton instance?
    return makeCachedUrlSet(new AuCachedUrlSetSpec());
  }

  public void pauseBeforeFetch(String previousContentType) {
    RateLimiter limit = findFetchRateLimiter();;
    try {
      if (logger.isDebug3()) logger.debug3("Pausing: " + limit.rateString());
      limit.fifoWaitAndSignalEvent();
    } catch (InterruptedException ignore) {
      // no action
    }
    if (previousContentType != null) {
      RateLimiter mimeLimit = plugin.getFetchRateLimiter(previousContentType);
      if (mimeLimit != null) {
	try {
	  if (logger.isDebug3()) {
	    logger.debug3("Pausing (" + previousContentType
			  + "): " + mimeLimit.rateString());
	  }
	  mimeLimit.fifoWaitAndSignalEvent();
	} catch (InterruptedException ignore) {
	  // no action
	}
      }
    }
  }

  protected RateLimiter fetchRateLimiter;

  public synchronized RateLimiter findFetchRateLimiter() {
    if (fetchRateLimiter == null) {
      fetchRateLimiter = recomputeFetchRateLimiter(null);
    }
    return fetchRateLimiter;
  }

  protected RateLimiter recomputeFetchRateLimiter(RateLimiter oldLimiter) {
    long interval = paramMap.getLong(KEY_AU_FETCH_DELAY, defaultFetchDelay);
    Object limiterKey = getFetchRateLimiterKey();
    if (limiterKey == null) {
      return getLimiterWithRate(oldLimiter, 1, interval);
    } else {
      RateLimiter.Pool pool = RateLimiter.getPool();
      return pool.findNamedRateLimiter(limiterKey, 1, interval);
    }
  }

  public final Object getFetchRateLimiterKey() {
    String limiterSource = getFetchRateLimiterSource();
    if (logger.isDebug3()) logger.debug3("Limiter source: " + limiterSource);
    if ("au".equalsIgnoreCase(limiterSource)) {
      return null;
    } else {
      Object key = null;
      if ("plugin".equalsIgnoreCase(limiterSource)) {
	key = plugin;
      } else if (StringUtil.startsWithIgnoreCase(limiterSource,
						 "title_attribute:")) {
	String attr = limiterSource.substring("title_attribute:".length());
	key = AuUtil.getTitleAttribute(this, attr);
	if (key != null) {
	  key = attr + ":" + key;
	}
      } else if (StringUtil.startsWithIgnoreCase(limiterSource, "host:")) {
	String param = limiterSource.substring("host:".length());
	key = paramMap.getString(param + SUFFIX_AU_HOST);
	if (key != null) {
	  key = "host:" + key;
	}
      }
      if (key == null) {
	logger.warning("Rate limiter source (" + limiterSource +
		       ") is null, using AU");
      }
      return key;
    }
  }

  protected String getFetchRateLimiterSource() {
    String defaultSource =
      CurrentConfig.getParam(PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE,
			     DEFAULT_DEFAULT_FETCH_RATE_LIMITER_SOURCE);
    return paramMap.getString(KEY_AU_FETCH_RATE_LIMITER_SOURCE, defaultSource);
  }

  private RateLimiter getLimiterWithRate(RateLimiter oldLimiter,
					 int events, long interval) {
    if (oldLimiter != null) {
      oldLimiter.setRate(events, interval);
      return oldLimiter;
    } else {
      return new RateLimiter(events, interval);
    }
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(80);
    sb.append("[AU: ");
    if (StringUtil.isNullString(auName)) {
      sb.append(getAuId());
    } else {
      sb.append(auName);
    }
    sb.append("]");
    return sb.toString();
  }

  protected List getPermissionPages() {
    return startUrls;
  }

  /** Override to provide permission path */
  public String getPerHostPermissionPath() {
    return null;
  }

  public String getName() {
    return paramMap.getString(KEY_AU_TITLE, auName);
  }


  /**
   * Use the starting url and the crawl rules to make the crawl spec needed
   * to crawl this au.
   *
   * By default, it is assumed we are doing a new content crawl, thus this method
   * will return SpiderCrawlSpec which implements CrawlSpec.
   * If people are using the plugin tools and configurate it to do an Oai Crawl
   * the DefinableArchivalUnit is overriding this method to return an OaiCrawlSpec
   * which also implements CrawlSpec.
   * If one is not using plugin tools, but want to do an Oai crawl, he should
   * change this method to return OaiCrawlSpec instead of SpiderCrawlSpec
   * @return the CrawlSpec need by this au.
   * @throws LockssRegexpException if the CrawlRules contain an invalid
   * regular expression
   */
  protected CrawlSpec makeCrawlSpec() throws LockssRegexpException {
    CrawlRule rule = makeRules();
    return new SpiderCrawlSpec(startUrls, rule);
  }

  protected CrawlWindow makeCrawlWindow() {
    return null;
  }

  /**
   * subclasses must implement this method to make and return the Crawl Rules
   * needed to crawl content.
   * @return CrawlRule object containing the necessary rules
   * @throws LockssRegexpException if the rules contain an unacceptable
   * regular expression.
   */
  abstract protected CrawlRule makeRules() throws LockssRegexpException;

  /**
   * Compute the AU's single start URL.  (Subclasses must implement either
   * makeStartUrl() or makeStartUrls().)
   * @return the URL from which a crawl of this au should start
   */
  protected String makeStartUrl() throws ConfigurationException {
    throw new UnsupportedOperationException("Plugin must implement makeStartUrl() or makeStartUrls()");
  }

  /**
   * Compute the AU's starting URL list.  (Subclasses must implement either
   * makeStartUrl() or makeStartUrls().)
   * @return the list of URLs from which a crawl of this au should start
   */
  protected List<String> makeStartUrls() throws ConfigurationException {
    ArrayList res = new ArrayList(1);
    res.add(makeStartUrl());
    res.trimToSize();
    return res;
  }

  /**
   * subclasses must implement to make and return the name for this au
   * @return the au name as a String
   */
  abstract protected String makeName();

  /**
   * Returns true if there has been no treewalk within the last
   * newContentCrawlIntv
   *
   * @param aus the {@link AuState}
   * @return true iff no crawl has happened in the last newContentCrawlIntv
   */
  public boolean shouldCrawlForNewContent(AuState aus) {
    if (AuUtil.isPubDown(this)) {
      logger.debug2("Pub down: no new content crawl possible for "+aus);
      return false;
    }
    long timeDiff = TimeBase.msSince(aus.getLastCrawlTime());
    if (logger.isDebug2()) {
      logger.debug2("Deciding whether to do new content crawl for "+aus);
    }
    if (aus.getLastCrawlTime() == 0 || timeDiff > (newContentCrawlIntv)) {
      logger.debug2("New content crawl needed for "+aus);
      return true;
    }
    logger.debug2("No new content crawl needed for "+aus);
    return false;
  }


  /**
   * Simplified implementation which gets the poll interval parameter
   * and determine whether that much time has elapsed since the last poll time.
   * @param aus the {@link AuState}
   * @return true iff a top level poll should be called
   */
  public boolean shouldCallTopLevelPoll(AuState aus) {
    String flags = AuUtil.getTitleAttribute(this, "flags");
    if (flags != null && StringUtil.indexOfIgnoreCase(flags, "nopoll") >= 0) {
      return false;
    }
    return true;
  }

  /**
   * Return a LinkExtractor for the MIME type, or null.
   * @param contentType content type to get a content parser for
   */
  public LinkExtractor getLinkExtractor(String contentType)
      throws PluginException.InvalidDefinition {
    return plugin.getLinkExtractor(contentType);
  }

  /**
   * Returns a filter rule from the cache if found, otherwise calls
   * 'constructFilterRule()' and caches the result if non-null.  Content-type
   * is converted to lowercase.  If contenttype is null, returns null.
   * @param contentType the content type
   * @return the FilterRule
   */
  public FilterRule getFilterRule(String contentType) {
    return plugin.getFilterRule(contentType);
  }

  /**
   * Returns a filter factory from the cache if found, otherwise calls
   * 'constructFilterFactory()' and caches the result if non-null.
   * Content-type is converted to lowercase.  If contenttype is null,
   * returns null.
   * @param contentType the content type
   * @return the FilterFactory
   */
  public FilterFactory getFilterFactory(String contentType) {
    return plugin.getFilterFactory(contentType);
  }

  /**
   * Returns a link rewriter factory from the cache if found, otherwise calls
   * 'constructLinkFactory()' and caches the result if non-null.
   * Content-type is converted to lowercase.  If contenttype is null,
   * returns null.
   * @param contentType the content type
   * @return the LinkFactory
   */
  public LinkRewriterFactory getLinkRewriterFactory(String contentType) {
    return plugin.getLinkRewriterFactory(contentType);
  }

  /**
   * Returns an article iterator from the AU's plugin.  If there isn't
   * one, an empty iterator will be returned.
   * @return the Iterator for the AU's articles.
   */
  public Iterator getArticleIterator(String contentType) {
    Iterator ret = CollectionUtil.EMPTY_ITERATOR;
    ArticleIteratorFactory aif = plugin.getArticleIteratorFactory(contentType);
    if (aif != null) try {
      Iterator it = aif.createArticleIterator(contentType, this);
      if (it != null) {
	ret = it;
      }
    } catch (PluginException ex) {
      logger.warning("createArticleIterator(" + contentType + ") threw " + ex);
    }
    return ret;
  }
  
  /**
   * Returns the article iterator for the default content type.
   * @return the Iterator for the AU's articles.
   */
  public Iterator getArticleIterator() {
    return getArticleIterator(null);
  }


  public long getArticleCount() {
    long ret = 0;
    for (Iterator it = getArticleIterator(); it.hasNext(); ) {
      CachedUrl cu = (CachedUrl)it.next();
      ret++;
    }
    return ret;
  }


  public List<String> getNewContentCrawlUrls() {
    return startUrls;
  }

  // utility methods for configuration management
  /**
   * Get the URL for a key from the configuration, check for validity and return
   * it.
   * @param descr the ConfigurationParmDescr of the url to extract
   * @param config the Configuration object from which to extract the url
   * @return a URL for the key
   * @throws ConfigurationException thrown if there is no matching entry or
   * the url is malformed or the url does not match the expectedUrlPath.
   */
  protected URL loadConfigUrl(ConfigParamDescr descr, Configuration config)
    throws ConfigurationException {
    String key = descr.getKey();
    URL url = null;

    String urlStr = config.get(key);
    if (urlStr == null) {
      throw new ConfigurationException("No configuration value for " +
				       paramString(descr));
    }
    try {
      url = new URL(urlStr);
    } catch (MalformedURLException murle) {
      throw new ConfigurationException("Bad URL for " + paramString(descr), murle);
    }
    if (url == null) {
      throw new ConfigurationException("Null url for " + paramString(descr));
    }
    // TODO: We need to come up with a way to handle expected path

    return url;
  }

  /**
   * Get the integer from the configuration, check for validity and return it.
   * @param descr the ConfigurationParamDescr of the integer to extract.
   * @param config the Configuration from which to extract the integer
   * @return an int
   * @throws ConfigurationException thrown if the key is not found or the entry
   * is not an int.
   */
  protected int loadConfigInt(ConfigParamDescr descr, Configuration config)
      throws ConfigurationException {
    String key = descr.getKey();
    int value = -1;
    try {
      value = config.getInt(key);
    } catch (Configuration.InvalidParam ip) {
      throw new ConfigurationException("Invalid value for " +
				       paramString(descr) +
				       ": " + ip.getMessage());
    }
    return value;
  }

  /**
   * Get the string from the configuration.
   * @param descr the ConfigurationParamDescr of the string to extract
   * @param config the Configuration from which to extract the string
   * @return the String
   * @throws ConfigurationException thrown if the configuration does not contain
   * the key.
   */
  protected String loadConfigString(ConfigParamDescr descr,
      Configuration config) throws ConfigurationException {
    String key = descr.getKey();
    String value = null;
    value = config.get(key);
    if (value == null) {
      throw new ConfigurationException("Null String for " +
				       paramString(descr));
    }
    return value;
  }

  String paramString(ConfigParamDescr descr) {
    return "\"" + descr.getDisplayName() + "\"";
  }

  protected static class ParamHandlerMap extends TypedEntryMap {
    HashMap handlerMap = new HashMap();

    protected ParamHandlerMap() {
      super();
    }

    protected void addParamHandler(String paramKey, ParamHandler handler) {
      handlerMap.put(paramKey, handler);
    }

    protected ParamHandler removeParamHandler(String paramKey) {
      synchronized (handlerMap) {
        return (ParamHandler) handlerMap.remove(paramKey);
      }
    }

    public Object getMapElement(String paramKey) {
      synchronized (handlerMap) {
        ParamHandler handler = (ParamHandler)handlerMap.get(paramKey);
        if(handler != null) {
          return handler.getParamValue(paramKey);
        }
      }
      return super.getMapElement(paramKey);
    }
  }

  public interface ParamHandler {
    public Object getParamValue(String paramKey);
  }
}
