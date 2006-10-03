/*
 * $Id: BaseArchivalUnit.java,v 1.110 2006-10-03 22:24:13 thib_gc Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.AuState;
import org.lockss.util.*;

/**
 * Abstract base class for ArchivalUnits.
 * Plugins may extend this to get some common ArchivalUnit functionality.
 */
public abstract class BaseArchivalUnit implements ArchivalUnit {
  static final String TOPLEVEL_POLL_PREFIX = Configuration.PREFIX +
      "baseau.toplevel.poll.";

  /**
   * Configuration parameter name for minimum interval, in ms, after which
   * a new top level poll should be called.  Actual interval is randomly
   * distributed between min and max.
   */
  public static final String PARAM_TOPLEVEL_POLL_INTERVAL_MIN =
      TOPLEVEL_POLL_PREFIX + "interval.min";
  static final long DEFAULT_TOPLEVEL_POLL_INTERVAL_MIN = 2 * Constants.WEEK;

  /**
   * Configuration parameter name for maximum interval, in ms, by which
   * a new top level poll should have been called.  Actual interval is randomly
   * distributed between min and max.
   */
  public static final String PARAM_TOPLEVEL_POLL_INTERVAL_MAX =
      TOPLEVEL_POLL_PREFIX + "interval.max";
  public static final long DEFAULT_TOPLEVEL_POLL_INTERVAL_MAX =
      3 * Constants.WEEK;

  /**
   * Configuration parameter name for top level poll initial probability.
   */
  public static final String PARAM_TOPLEVEL_POLL_PROB_INITIAL =
      TOPLEVEL_POLL_PREFIX + "prob.initial";
  public static final double DEFAULT_TOPLEVEL_POLL_PROB_INITIAL = .5;

  /**
   * Configuration parameter name for top level poll increment
   */
  public static final String PARAM_TOPLEVEL_POLL_PROB_INCREMENT =
      TOPLEVEL_POLL_PREFIX + "prob.increment";
  public static final double DEFAULT_TOPLEVEL_POLL_PROB_INCREMENT = .05;

  /**
   * Configuration parameter name for top level poll max probability.
   */
  public static final String PARAM_TOPLEVEL_POLL_PROB_MAX =
      TOPLEVEL_POLL_PREFIX + "prob.max";
  public static final double DEFAULT_TOPLEVEL_POLL_PROB_MAX = 1.0;


  public static final long
    DEFAULT_FETCH_DELAY = 6 * Constants.SECOND;
  public static final long
    MIN_FETCH_DELAY = 6 * Constants.SECOND;

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

  public static final String NEW_CONTENT_CRAWL_KEY = "nc_interval";
  public static final String PAUSE_TIME_KEY = "pause_time";
  static final public String AU_DEFAULT_NC_CRAWL_KEY = "au_def_new_content_crawl";
  static final public String AU_DEFAULT_PAUSE_TIME = "au_def_pause_time";

  static final public String AU_SHORT_YEAR_PREFIX = "au_short_";
  static final public String AU_HOST_SUFFIX = "_host";
  static final public String AU_PATH_SUFFIX = "_path";

  public static final long
      DEFAULT_NEW_CONTENT_CRAWL_INTERVAL = 2 * Constants.WEEK;
  protected BasePlugin plugin;
  protected CrawlSpec crawlSpec;
  protected UrlNormalizer urlNormalizer;
  static Logger logger = Logger.getLogger("BaseArchivalUnit");
  static SimpleDateFormat sdf = new SimpleDateFormat();

  protected static final long DEFAULT_AU_MAX_SIZE = 0;
  protected static final long DEFAULT_AU_MAX_FILE_SIZE = 0;

  protected long minFetchDelay = MIN_FETCH_DELAY;
  protected long defaultFetchDelay = DEFAULT_FETCH_DELAY;
  protected String startUrlString;
  protected long newContentCrawlIntv;
  protected long defaultContentCrawlIntv = DEFAULT_NEW_CONTENT_CRAWL_INTERVAL;

  protected String auName;   // the name of the AU (constructed by plugin)
  protected TitleConfig titleConfig;   // matching entry from titledb, if any
  protected String auTitle;   // the title of the AU (from titledb, if any)
  protected long nextPollInterval = -1;
  protected double curTopLevelPollProb = -1;
  protected Configuration auConfig;
  private String auId = null;

  protected GoslingHtmlParser goslingHtmlParser = null;
  protected HashMap filterMap = new HashMap(4);

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
    paramMap.putUrl(AU_BASE_URL, baseUrl);

    // get the fetch delay
    long fetchDelay =
      (config.containsKey(PAUSE_TIME_KEY)
       ? Math.max(config.getTimeInterval(PAUSE_TIME_KEY, defaultFetchDelay),
		  minFetchDelay)
       : paramMap.getLong(AU_FETCH_DELAY,
			  Math.max(defaultFetchDelay, minFetchDelay)));
    logger.debug2("Set fetch delay to " + fetchDelay);
    paramMap.putLong(AU_FETCH_DELAY, fetchDelay);

    // get the new content crawl interval
    newContentCrawlIntv =
      (config.containsKey(NEW_CONTENT_CRAWL_KEY)
       ? config.getTimeInterval(NEW_CONTENT_CRAWL_KEY,
				defaultContentCrawlIntv)
       : paramMap.getLong(AU_NEW_CRAWL_INTERVAL, defaultContentCrawlIntv));
    logger.debug2("Setting new content crawl interval to " +
		  StringUtil.timeIntervalToString(newContentCrawlIntv));
    paramMap.putLong(AU_NEW_CRAWL_INTERVAL, newContentCrawlIntv);

    // make the start url
    startUrlString = makeStartUrl();
    paramMap.putString(AU_START_URL, startUrlString);



    // get crawl window setting
    boolean useCrawlWindow =
      (config.containsKey(USE_CRAWL_WINDOW)
       ? config.getBoolean(USE_CRAWL_WINDOW, DEFAULT_USE_CRAWL_WINDOW)
       :
       paramMap.getBoolean(AU_USE_CRAWL_WINDOW, DEFAULT_USE_CRAWL_WINDOW));
    paramMap.putBoolean(AU_USE_CRAWL_WINDOW, useCrawlWindow);


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
    paramMap.setMapElement(AU_CRAWL_SPEC, crawlSpec);

    //make our url normalizer
    urlNormalizer = makeUrlNormalizer();
    paramMap.setMapElement(AU_URL_NORMALIZER, urlNormalizer);

    titleDbChanged();
  }

  void addImpliedConfigParams() {
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
	    paramMap.putInt(AU_SHORT_YEAR_PREFIX + key, year);
	    if (logger.isDebug3()) {
	      logger.debug3("Inferred " + AU_SHORT_YEAR_PREFIX + key +
			    " = " + year);
	    }
	  }
	  // store separate host and path of URLs
	  if (descr.getType() == ConfigParamDescr.TYPE_URL) {
	    URL url = (URL)val;
	    if(url != null) {
	      paramMap.putString(key + AU_HOST_SUFFIX, url.getHost());
	      paramMap.putString(key + AU_PATH_SUFFIX, url.getPath());
	      if (logger.isDebug3()) {
		logger.debug3("Inferred " + key + AU_HOST_SUFFIX +
			      " = " + url.getHost());
		  logger.debug3("Inferred " + key + AU_PATH_SUFFIX +
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
    paramMap.putString(AU_TITLE, auTitle != null ? auTitle : auName);
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
   * Return the Url stems from which to begin our crawl.
   * @return a List of Urls
   */
  public Collection getUrlStems() {
    try {
      URL baseUrl = paramMap.getUrl(AU_BASE_URL,null);
      URL stem = new URL(baseUrl.getProtocol(), baseUrl.getHost(),
                         baseUrl.getPort(), "");
      return ListUtil.list(stem.toString());
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

  public String siteNormalizeUrl(String url) {
    UrlNormalizer normmalizer =
      (UrlNormalizer)paramMap.getMapElement(AU_URL_NORMALIZER);

    if (normmalizer != null) {
      return normmalizer.normalizeUrl(url, this);
    }
    return url;
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

  public void pauseBeforeFetch() {
    RateLimiter limit = findFetchRateLimiter();;
    try {
      limit.fifoWaitAndSignalEvent();
    } catch (InterruptedException ignore) {
      // no action
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
    RateLimiter limit;
    long interval = paramMap.getLong(AU_FETCH_DELAY, defaultFetchDelay);
    String defaultSource =
      CurrentConfig.getParam(PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE,
			     DEFAULT_DEFAULT_FETCH_RATE_LIMITER_SOURCE);
    String limiterSource =
      paramMap.getString(AU_FETCH_RATE_LIMITER_SOURCE, defaultSource);
    if (logger.isDebug3()) logger.debug3("Limiter source: " + limiterSource);
    if ("au".equalsIgnoreCase(limiterSource)) {
      limit = getLimiterWithRate(oldLimiter, 1, interval);
    } else {
      RateLimiter.Pool pool = RateLimiter.getPool();
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
      } else if (StringUtil.startsWithIgnoreCase(limiterSource,
						 "host:")) {
	String param = limiterSource.substring("host:".length());
	key = paramMap.getString(param + AU_HOST_SUFFIX);
	if (key != null) {
	  key = "host:" + key;
	}
      }
      if (key != null) {
	limit = pool.findNamedRateLimiter(key, 1, interval);
      } else {
	logger.warning("Rate limiter source (" + limiterSource +
		       ") is null, using AU");
	limit = getLimiterWithRate(oldLimiter, 1, interval);
      }
    }
    return limit;
  }

  private RateLimiter getLimiterWithRate(RateLimiter oldLimiter,
					 int events, long interval) {
    if (oldLimiter != null) {
      oldLimiter.setRate(1, interval);
      return oldLimiter;
    } else {
      return new RateLimiter(1, interval);
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
    return ListUtil.list(startUrlString);
  }

  public String getName() {
    return paramMap.getString(AU_TITLE, auName);
  }

  protected UrlNormalizer makeUrlNormalizer() {
    return null;
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
    return new SpiderCrawlSpec(startUrlString, rule);
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
   * subclasses must implement to make and return the url from which a crawl of
   * this au will start.
   * @return the starting url as a String
   */
  abstract protected String makeStartUrl();

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
    checkNextPollInterval();
    checkPollProb();

    logger.debug3("Deciding whether to call a top level poll");
    long lastPoll = aus.getLastTopLevelPollTime();
    if (logger.isDebug3()) {
      if (lastPoll==-1) {
	logger.debug3("No previous top level poll.");
      } else {
	logger.debug3("Last poll at " + sdf.format(new Date(lastPoll)));
      }
      logger.debug3("Poll interval: " +
		    StringUtil.timeIntervalToString(nextPollInterval));
      logger.debug3("Poll likelihood: "+curTopLevelPollProb);
    }
    if (TimeBase.msSince(lastPoll) < nextPollInterval) {
      logger.debug("Not time for poll.");
      return false;
    }
    // Choose probabilistically whether to call poll, but always reset poll
    // interval next time checkNextPollInterval() runs.
    nextPollInterval = -1;
    if (ProbabilisticChoice.choose(curTopLevelPollProb)) {
      logger.debug("Allowing poll.");
      curTopLevelPollProb = -1;
      return true;
    } else {
      logger.debug("Skipping poll.");
      // decided not to call the poll
      curTopLevelPollProb = incrementPollProb(curTopLevelPollProb);
      return false;
    }
  }

  /**
   * Currently the only ContentParser we have is GoslingHtmlParser, so this
   * gets returned for any string that starts with "test/html".  Null otherwise
   * @param contentType content type to get a content parser for
   * @return GoslingHtmlParser if contentType starts with "test/html",
   * null otherwise
   */
  public ContentParser getContentParser(String contentType) {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);
    if ("text/html".equalsIgnoreCase(mimeType)) {
      if (goslingHtmlParser == null) {
	goslingHtmlParser = new GoslingHtmlParser();
      }
      return goslingHtmlParser;
    }
    return null;
  }

  /**
   * Returns a filter rule from the cache if found, otherwise calls
   * 'constructFilterRule()' and caches the result if non-null.  Content-type
   * is converted to lowercase.  If contenttype is null, returns null.
   * @param contentType the content type
   * @return the FilterRule
   */
  public FilterRule getFilterRule(String contentType) {
    if (contentType != null) {
      Object obj = filterMap.get(contentType);
      FilterRule rule = null;
      if (obj==null) {
        rule = constructFilterRule(contentType);
        if (rule != null) {
	  if (logger.isDebug3()) logger.debug3(contentType + " filter: " +
					       rule);
          filterMap.put(contentType, rule);
        } else {
	  if (logger.isDebug3()) logger.debug3("No filter for "+contentType);
	}
      } else if (obj instanceof FilterRule) {
	rule = (FilterRule)obj;
      }
      return rule;
    }
    logger.debug3("getFilterRule: null content type");
    return null;
  }

  /**
   * Override to provide proper filter rules.
   * @param contentType content type
   * @return null, since we don't filter by default
   */
  protected FilterRule constructFilterRule(String contentType) {
    logger.debug3("constructFilterRule default: null");
    return null;
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
    if (contentType != null) {
      Object obj = filterMap.get(contentType);
      FilterFactory factory = null;
      if (obj==null) {
        factory = constructFilterFactory(contentType);
        if (factory != null) {
	  if (logger.isDebug3()) logger.debug3(contentType + " filter: " +
					       factory);
          filterMap.put(contentType, factory);
        } else {
	  if (logger.isDebug3()) logger.debug3("No filter for "+contentType);
	}
      } else if (obj instanceof FilterFactory) {
	factory = (FilterFactory)obj;
      }
      return factory;
    }
    logger.debug3("getFilterFactory: null content type");
    return null;
  }

  /**
   * Override to provide proper filter factories.
   * @param contentType content type
   * @return null, since we don't filter by default
   */
  protected FilterFactory constructFilterFactory(String contentType) {
    logger.debug3("constructFilterFactory default: null");
    return null;
  }

  public List getNewContentCrawlUrls() {
    return ListUtil.list(startUrlString);
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


  void checkNextPollInterval() {
    Configuration config = CurrentConfig.getCurrentConfig();
    long minPollInterval =
        config.getTimeInterval(PARAM_TOPLEVEL_POLL_INTERVAL_MIN,
                               DEFAULT_TOPLEVEL_POLL_INTERVAL_MIN);
    long maxPollInterval =
        config.getTimeInterval(PARAM_TOPLEVEL_POLL_INTERVAL_MAX,
                               DEFAULT_TOPLEVEL_POLL_INTERVAL_MAX);
    if (maxPollInterval <= minPollInterval) {
      maxPollInterval = 2 * minPollInterval;
    }
    if ((nextPollInterval < minPollInterval) ||
        (nextPollInterval > maxPollInterval)) {
      nextPollInterval =
          Deadline.inRandomRange(minPollInterval,
                                 maxPollInterval).getRemainingTime();
    }
  }

  void checkPollProb() {
    Configuration config = CurrentConfig.getCurrentConfig();
    double initialProb = config.getPercentage(
        PARAM_TOPLEVEL_POLL_PROB_INITIAL, DEFAULT_TOPLEVEL_POLL_PROB_INITIAL);
    double maxProb = config.getPercentage(
        PARAM_TOPLEVEL_POLL_PROB_MAX, DEFAULT_TOPLEVEL_POLL_PROB_MAX);
    if (curTopLevelPollProb < initialProb) {
      // reset to initial prob
      curTopLevelPollProb = initialProb;
    } else if (curTopLevelPollProb > maxProb) {
      curTopLevelPollProb = maxProb;
    }
  }

  double incrementPollProb(double curProb) {
    Configuration config = CurrentConfig.getCurrentConfig();
    double topLevelPollProbMax =
        config.getPercentage(PARAM_TOPLEVEL_POLL_PROB_MAX,
                             DEFAULT_TOPLEVEL_POLL_PROB_MAX);
    if (curProb < topLevelPollProbMax) {
      // if less than max prob, increment
      curProb += config.getPercentage(
          PARAM_TOPLEVEL_POLL_PROB_INCREMENT,
          DEFAULT_TOPLEVEL_POLL_PROB_INCREMENT);
    }
    if (curProb > topLevelPollProbMax) {
      curProb = topLevelPollProbMax;
    }
    return curProb;
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
