/*
 * $Id: BaseArchivalUnit.java,v 1.78 2004-09-20 17:47:38 clairegriffin Exp $
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

package org.lockss.plugin.base;

import java.net.*;
import java.text.*;
import java.util.*;

import org.apache.commons.collections.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.state.*;
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
    DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS = 6 * Constants.SECOND;

  public static final String USE_CRAWL_WINDOW = "use_crawl_window";
  private static final boolean DEFAULT_USE_CRAWL_WINDOW = false;

  public static final String NEW_CONTENT_CRAWL_KEY = "nc_interval";
  public static final String PAUSE_TIME_KEY = "pause_time";
  static final public String AU_DEFAULT_NC_CRAWL_KEY = "au_def_new_content_crawl";
  static final public String AU_DEFAULT_PAUSE_TIME = "au_def_pause_time";

  public static final long
      DEFAULT_NEW_CONTENT_CRAWL_INTERVAL = 2 * Constants.WEEK;
  protected Plugin plugin;
  protected CrawlSpec crawlSpec;
  protected UrlNormalizer urlNormalizer;
  static Logger logger = Logger.getLogger("BaseArchivalUnit");
  static SimpleDateFormat sdf = new SimpleDateFormat();

  protected static final long DEFAULT_AU_MAX_SIZE = 0;
  protected static final long DEFAULT_AU_MAX_FILE_SIZE = 0;

  protected long minFetchDelay = 6 * Constants.SECOND;
  protected long defaultFetchDelay = DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS;
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

  // crawl spec support
  protected LRUMap crawlSpecCache = new LRUMap(1000);
  protected int hits = 0;
  protected int misses = 0;
  protected TypedEntryMap paramMap;

  protected BaseArchivalUnit(Plugin myPlugin) {
    plugin = myPlugin;
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
    if (auConfig != null) {
      checkLegalConfigChange(config);
    }
    if (config == null) {
      throw new ConfigurationException("Null Configuration");
    }
    auConfig = config;
    loadAuConfigDescrs(config);
    setBaseAuParams(config);
  }

  public Configuration getConfiguration() {
    return auConfig;
  }

  private void checkLegalConfigChange(Configuration newConfig)
      throws ArchivalUnit.ConfigurationException {
    for (Iterator iter = plugin.getAuConfigDescrs().iterator();
	 iter.hasNext();) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      if (descr.isDefinitional()) {
	String key = descr.getKey();
	String oldVal = auConfig.get(key);
	String newVal = newConfig.get(key);
	if (!StringUtil.equalStrings(oldVal, newVal)) {
	  throw new ConfigurationException(
              "Attempt to modify defining property " +
              "of existing ArchivalUnit: " + key +
              ". old: "+oldVal+" new: "+newVal
              );
	}
      }
    }
  }

  protected void setBaseAuParams(Configuration config)
      throws ConfigurationException {

    // get the base url
    URL baseUrl = loadConfigUrl(ConfigParamDescr.BASE_URL, config);
    paramMap.putUrl(AU_BASE_URL, baseUrl);

    // get the fetch delay
    long fetchDelay = config.getTimeInterval(PAUSE_TIME_KEY, defaultFetchDelay);
    fetchDelay = Math.max(fetchDelay, minFetchDelay);
    logger.debug2("Set fetch delay to " + fetchDelay);
    paramMap.putLong(AU_FETCH_DELAY, fetchDelay);

    // get crawl window setting
    boolean useCrawlWindow = config.getBoolean(USE_CRAWL_WINDOW,
                                       DEFAULT_USE_CRAWL_WINDOW);
    paramMap.putBoolean(AU_USE_CRAWL_WINDOW, useCrawlWindow);

    // get the new content crawl interval
    newContentCrawlIntv = config.getTimeInterval(NEW_CONTENT_CRAWL_KEY,
                                                 defaultContentCrawlIntv);
    logger.debug2("Setting new content crawl interval to " +
		  StringUtil.timeIntervalToString(newContentCrawlIntv));
    paramMap.putLong(AU_NEW_CRAWL_INTERVAL, newContentCrawlIntv);

    // make the start url
    startUrlString = makeStartUrl();
    paramMap.putString(AU_START_URL, startUrlString);
    // make our crawl spec
    try {
      crawlSpec = makeCrawlSpec();
      if (useCrawlWindow) {
        CrawlWindow window = makeCrawlWindow();
        crawlSpec.setCrawlWindow(window);
      }
    } catch (LockssRegexpException e) {
      throw new ConfigurationException("Illegal RE", e);
    }
    paramMap.setMapElement(AU_CRAWL_SPEC, crawlSpec);


    //make our url normalizer
    urlNormalizer = makeUrlNormalizer();
    paramMap.setMapElement(AU_URL_NORMALIZER, urlNormalizer);

    // make our name
    titleConfig = findTitleConfig(config);
    if (titleConfig != null) {
      auTitle = titleConfig.getDisplayName();
    }
    auName = makeName();
    paramMap.putString(AU_TITLE, auTitle != null ? auTitle : auName);

  }

  TitleConfig findTitleConfig(Configuration config) {
    if(plugin.getSupportedTitles() == null)  {
      return null;
    }
    for (Iterator iter = plugin.getSupportedTitles().iterator();
	 iter.hasNext(); ) {
      String title = (String)iter.next();
      TitleConfig tc = plugin.getTitleConfig(title);
      if (tc != null && tc.matchesConfig(config)) {
	return tc;
      }
    }
    return null;
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
      return Collections.EMPTY_LIST;
    }
  }

  /**
   * Determine whether the url falls within the CrawlSpec.
   * @param url the url
   * @return true if it is included
   */
  public boolean shouldBeCached(String url) {
    Boolean cachedVal = (Boolean)crawlSpecCache.get(url);
    if (cachedVal != null) {
      hits++;
      return cachedVal.booleanValue();
    }
    misses++;
    boolean val = getCrawlSpec().isIncluded(url);
    crawlSpecCache.put(url, val ? Boolean.TRUE : Boolean.FALSE);
    return val;
  }

  public int getCrawlSpecCacheHits() {
    return hits;
  }

  public int getCrawlSpecCacheMisses() {
    return misses;
  }

  public String siteNormalizeUrl(String url) {
    UrlNormalizer urlNormalizer =
      (UrlNormalizer)paramMap.getMapElement(AU_URL_NORMALIZER);

    if (urlNormalizer != null) {
      return urlNormalizer.normalizeUrl(url, this);
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
    return getPlugin().makeCachedUrlSet(this, new AuCachedUrlSetSpec());
  }

  private Deadline nextFetchTime = Deadline.in(0);

  public void pauseBeforeFetch() {
    if (!nextFetchTime.expired()) {
      try {
	nextFetchTime.sleep();
      } catch (InterruptedException ignore) {
	// no action
      }
    }
    nextFetchTime.expireIn(getFetchDelay());
  }

  public long getFetchDelay() {
    return paramMap.getLong(AU_FETCH_DELAY,
                            DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS);
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
   * @return the CrawlSpec need by this au.
   * @throws LockssRegexpException if the CrawlRules contain an invalid
   * regular expression
   */
  protected CrawlSpec makeCrawlSpec() throws LockssRegexpException {
    CrawlRule rule = makeRules();
    return new CrawlSpec(startUrlString, rule);
  }

  protected CrawlWindow makeCrawlWindow() {
    return null;
  }

  abstract protected void loadAuConfigDescrs(Configuration config) throws
      ConfigurationException;


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
   * Simplified implementation which returns true if a crawl has never
   * been done, otherwise false
   * @param aus the {@link AuState}
   * @return true iff no crawl done
   */
  public boolean shouldCrawlForNewContent(AuState aus) {
    long timeDiff = TimeBase.msSince(aus.getLastCrawlTime());
    logger.debug("Deciding whether to do new content crawl for "+aus);
    if (aus.getLastCrawlTime() == 0 || timeDiff > (newContentCrawlIntv)) {
      return true;
    }
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

    logger.debug("Deciding whether to call a top level poll");
    long lastPoll = aus.getLastTopLevelPollTime();
    if (lastPoll==-1) {
      logger.debug3("No previous top level poll.");
    } else {
      logger.debug3("Last poll at " + sdf.format(new Date(lastPoll)));
    }
    logger.debug3("Poll interval: "+StringUtil.timeIntervalToString(
        nextPollInterval));
    logger.debug3("Poll likelihood: "+curTopLevelPollProb);
    if (TimeBase.msSince(lastPoll) >= nextPollInterval) {
      // reset poll interval regardless
      nextPollInterval = -1;
      // choose probabilistically whether to call
      if (ProbabilisticChoice.choose(curTopLevelPollProb)) {
        logger.debug("Allowing poll.");
        curTopLevelPollProb = -1;
        return true;
      } else {
        logger.debug("Skipping poll.");
        // decided not to call the poll
        curTopLevelPollProb = incrementPollProb(curTopLevelPollProb);
      }
    }
    return false;
  }

  /**
   * Currently the only ContentParser we have is GoslingHtmlParser, so this
   * gets returned for any string that starts with "test/html".  Null otherwise
   * @param mimeType mime type to get a content parser for
   * @return GoslingHtmlParser if mimeType starts with "test/html",
   * null otherwise
   */
  public ContentParser getContentParser(String mimeType) {
    if (mimeType != null) {
      if (StringUtil.startsWithIgnoreCase(mimeType, "text/html")) {
	if (goslingHtmlParser == null) {
	  goslingHtmlParser = new GoslingHtmlParser();
	}
	return goslingHtmlParser;
      }
    }
    return null;
  }


  /**
   * Returns a filter rule from the cache if found, otherwise calls
   * 'constructFilterRule()' and caches the result if non-null.  Mime-type
   * is converted to lowercase.  If mimetype is null, returns null.
   * @param mimeType the mime type
   * @return the FilterRule
   */
  public FilterRule getFilterRule(String mimeType) {
    if (mimeType!=null) {
      FilterRule rule = (FilterRule)filterMap.get(mimeType);
      if (rule==null) {
        rule = constructFilterRule(mimeType);
        if (rule != null) {
          filterMap.put(mimeType, rule);
        }
      }
      return rule;
    }
    return null;
  }

  /**
   * Override to provide proper filter rules.
   * @param mimeType the mime type
   * @return null, since we don't filter by default
   */
  protected FilterRule constructFilterRule(String mimeType) {
    logger.debug3("BaseArchivalUnit - returning default value of null");
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
    Configuration config = Configuration.getCurrentConfig();
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
    Configuration config = Configuration.getCurrentConfig();
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
    Configuration config = Configuration.getCurrentConfig();
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

    private ParamHandlerMap() {
      super();
    }

    protected void addParamHandler(String paramKey, ParamHandler handler) {
      handlerMap.put(paramKey, handler);
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
