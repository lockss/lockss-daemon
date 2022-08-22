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

package org.lockss.plugin.base;

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.oro.text.regex.*;

import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.extractor.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.rewriter.*;
import org.lockss.state.AuState;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * Abstract base class for ArchivalUnits.
 * Plugins may extend this to get some common ArchivalUnit functionality.
 */
public abstract class BaseArchivalUnit implements ArchivalUnit {
  
  private static final Logger log = Logger.getLogger(BaseArchivalUnit.class);

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

  /** Override fetch rate limiter source for all plugins and AU no matter
   * what else they specify.  Can be "au" or "plugin"". */
  public static final String PARAM_OVERRIDE_FETCH_RATE_LIMITER_SOURCE =
    Configuration.PREFIX+"baseau.overrideFetchRateLimiterSource";
  
  private static final String SHOULD_REFETCH_ON_SET_COOKIE =
      "refetch_on_set_cookie";
  private static final boolean DEFAULT_SHOULD_REFETCH_ON_SET_COOKIE = true;

  //Short term conf parameter to get around the fact that DefinablePlugins
  //don't load crawl windows
  public static final String PARAM_USE_CRAWL_WINDOW =
    Configuration.PREFIX+"baseau.useCrawlWindowByDefault";
  public static final boolean DEFAULT_USE_CRAWL_WINDOW = true;

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

  protected final BasePlugin plugin;
  protected boolean shouldRefetchOnCookies;
  protected long defaultFetchDelay = DEFAULT_FETCH_DELAY;
  protected List<String> urlStems;
  protected long newContentCrawlIntv;
  protected long defaultContentCrawlIntv = DEFAULT_NEW_CONTENT_CRAWL_INTERVAL;

  protected String auName;   // the name of the AU (constructed by plugin)
  protected TitleConfig titleConfig;   // matching entry from titledb, if any
  protected String auTitle;   // the title of the AU (from titledb, if any)
  protected Configuration auConfig;
  protected String auId = null;
  protected CrawlRule rule;
  protected CrawlWindow window;

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
   * Checks that the configuration is legal (doesn't change any of the
   * defining properties), stores the new configuration and causes any
   * cached info to be recomputed.  If called with current config, just
   * clears cached info.
   * @param config new Configuration
   * @throws ArchivalUnit.ConfigurationException if the configuration change is
   * illegal or for other configuration errors
   */
  public void setConfiguration(Configuration config)
      throws ArchivalUnit.ConfigurationException {
    if (config == null) {
      throw new ConfigurationException("Null Configuration");
    }
    if (config.equals(auConfig)) {
      if (log.isDebug3()) log.debug3("setConfiguration (unchanged): " +
                                           config);
    } else {
      if (log.isDebug3()) log.debug3("setConfiguration: " + config);
      checkLegalConfigChange(config);
      auConfig = config.copy();
      loadAuConfigDescrs(config);
      addImpliedConfigParams();
      setBaseAuParams(config);
      fetchRateLimiter = recomputeFetchRateLimiter(fetchRateLimiter);
    }
    urlStems = null;
  }

  public Configuration getConfiguration() {
    return auConfig;
  }

  public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
    return new BaseCachedUrlSet(this, cuss);
  }

  /** Return a CachedUrl for the specified url in this AU.  If the url
   * specifies an archive member (<tt><i>URL</i>!/<i>member</i></tt>), the
   * returned CU accesses the contents of the named archive member. */
  public CachedUrl makeCachedUrl(String url) {
    ArchiveMemberSpec ams = ArchiveMemberSpec.fromUrl(this, url);
    if (ams != null) {
      CachedUrl cu = new BaseCachedUrl(this, ams.getUrl());
      return cu.getArchiveMemberCu(ams);
    }
    return new BaseCachedUrl(this, url);
  }

  public UrlCacher makeUrlCacher(UrlData ud) {
    ArchiveMemberSpec ams = ArchiveMemberSpec.fromUrl(this, ud.url);
    if (ams != null) {
      throw new IllegalArgumentException("Cannot make a UrlCacher for an"
          + " archive member: " + ud.url);
    }
    return new DefaultUrlCacher(this, ud);
  }
  
  public CrawlSeed makeCrawlSeed(Crawler.CrawlerFacade crawlFacade) {
    return new BaseCrawlSeed(this);
  }
  
  public UrlFetcher makeUrlFetcher(CrawlerFacade facade, String url) {
    return new BaseUrlFetcher(facade, url);
  }
  
  public UrlConsumerFactory getUrlConsumerFactory() {
    return new SimpleUrlConsumerFactory();
  }

  private void checkLegalConfigChange(Configuration newConfig)
      throws ArchivalUnit.ConfigurationException {
    for (ConfigParamDescr descr : plugin.getAuConfigDescrs()) {
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
    for (ConfigParamDescr descr : plugin.getAuConfigDescrs()) {
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
    log.debug2("Set fetch delay to " + fetchDelay);
    paramMap.putLong(KEY_AU_FETCH_DELAY, fetchDelay);

    // get the new content crawl interval
    newContentCrawlIntv =
      (config.containsKey(KEY_NEW_CONTENT_CRAWL_INTERVAL)
       ? config.getTimeInterval(KEY_NEW_CONTENT_CRAWL_INTERVAL,
                                defaultContentCrawlIntv)
       : paramMap.getLong(KEY_AU_NEW_CONTENT_CRAWL_INTERVAL, defaultContentCrawlIntv));
    log.debug2("Setting new content crawl interval to " +
                  StringUtil.timeIntervalToString(newContentCrawlIntv));
    paramMap.putLong(KEY_AU_NEW_CONTENT_CRAWL_INTERVAL, newContentCrawlIntv);
    
    rule = makeRule();
    paramMap.setMapElement(KEY_AU_CRAWL_RULE, rule);
    
    shouldRefetchOnCookies = paramMap.getBoolean(SHOULD_REFETCH_ON_SET_COOKIE,
        DEFAULT_SHOULD_REFETCH_ON_SET_COOKIE);
    window = makeCrawlWindow();
    
    titleDbChanged();
  }

  protected void addImpliedConfigParams()
      throws ArchivalUnit.ConfigurationException {
    StringPool pool = StringPool.AU_CONFIG_PROPS;
    for (ConfigParamDescr descr : plugin.getAuConfigDescrs()) {
      String key = descr.getKey();
      try {
        if (auConfig.containsKey(key)) {
          Object val = descr.getValueOfType(auConfig.get(key));
          // we store years in two formats - short and long
          if (descr.getTypeEnum() == AuParamType.Year) {
            int year = ((Integer)val).intValue() % 100;
            paramMap.putInt(pool.intern(PREFIX_AU_SHORT_YEAR + key),
                            year);
            if (log.isDebug3()) {
              log.debug3("Inferred " + PREFIX_AU_SHORT_YEAR + key +
                            " = " + year);
            }
          }
          // store separate host and path of URLs
          if (descr.getTypeEnum() == AuParamType.Url) {
            URL url = (URL)val;
            if(url != null) {
              paramMap.putString(pool.intern(key + SUFFIX_AU_HOST),
                                 url.getHost());
              paramMap.putString(pool.intern(key + SUFFIX_AU_PATH),
                                 url.getPath());
              if (log.isDebug3()) {
                log.debug3("Inferred " + key + SUFFIX_AU_HOST +
                              " = " + url.getHost());
                log.debug3("Inferred " + key + SUFFIX_AU_PATH +
                              " = " + url.getPath());
              }
            }
          }
        }
      } catch (Exception ex) {
        log.error("Adding implied config params", ex);
      }
    }
  }


  TitleConfig findTitleConfig() {
    return plugin.getTitleConfigFromAuId(getAuId());
  }

  /** Set up titledb-related data */
  protected void titleDbChanged() {
    TitleConfig tc = findTitleConfig();
    if (tc != null) {
      titleConfig = tc;
      auTitle = titleConfig.getDisplayName();
    }
    auName = makeName();
    if (log.isDebug3()) {
      log.debug3("auTitle: " + auTitle + ", auConfig: "
                    + (auConfig != null
                       ? auConfig.get(PluginManager.AU_PARAM_DISPLAY_NAME)
                       : "(null)")
                    + ", auName: " + auName);
    }
    paramMap.putString(KEY_AU_TITLE,
                       (auTitle != null
                        ? auTitle
                        : (auConfig != null
                           ? auConfig.get(PluginManager.AU_PARAM_DISPLAY_NAME,
                                          auName)
                           : auName)));
  }

  public TitleConfig getTitleConfig() {
    return titleConfig;
  }

  public TdbAu getTdbAu() {
    return titleConfig == null ? null : titleConfig.getTdbAu();
  }

  /** AUs have no feature URLs by default */
  public List<String> getAuFeatureUrls(String auFeature) {
    return null;
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

  public Collection<String> getPermissionUrls() {
    return getStartUrls();
  }
  
  @Override
  public Collection<String> getAccessUrls() {
    return getStartUrls();
  }

  void addUrlParamStems(Set toSet) {
    for (ConfigParamDescr descr : plugin.getAuConfigDescrs()) {
      if (descr.getTypeEnum() == AuParamType.Url) {
        String key = descr.getKey();
        try {
          String url = auConfig.get(key);
          if (!StringUtil.isNullString(url)) {
            String stem = UrlUtil.getUrlPrefix(url);
            toSet.add(stem);
          }
        } catch (MalformedURLException ex) {
          log.error("addUrlParamStems key: " + key);
        }
      }
    }
  }

  /**
   * Return the Url stems (proto, host & port) of potential content within
   * this AU
   * @return a List of Urls
   */
  public Collection<String> getUrlStems() {
    if (urlStems == null) {
      try {
        Collection<String> perms = getPermissionUrls();
        Set<String> set = new HashSet<String>();
        if (perms != null) {
          for (String url : perms) {
            String stem = UrlUtil.getUrlPrefix(url);
            set.add(stem);
          }
        }
        addUrlParamStems(set);
        AuState aus = AuUtil.getAuState(this);
        // XXX Many plugin tests don't set up AuState
        List cdnStems = (aus != null
                         ? aus.getCdnStems() : Collections.EMPTY_LIST);
        ArrayList<String> res = new ArrayList<String>(set.size() +
                                                      cdnStems.size());
        set.addAll(cdnStems);
        set.addAll(getAdditionalUrlStems());
        res.addAll(set);
        urlStems = res;
      } catch (MalformedURLException e) {
        log.error("getUrlStems(" + getName() + ")", e);
        // XXX should throw
        urlStems = Collections.<String>emptyList();
      } catch (RuntimeException e) {
        log.error("getUrlStems(" + getName() + ")", e);
        // XXX should throw
        urlStems = Collections.<String>emptyList();
      }
    }
    return urlStems;
  }

  protected Collection<String> getAdditionalUrlStems()
      throws MalformedURLException {
    return Collections.EMPTY_LIST;
  }

  /**
   * Determine whether the url falls within the CrawlSpec.
   * @param url the url
   * @return true if it is included
   * @throws LockssRegexpException 
   */
  public boolean shouldBeCached(String url) {
    return (rule == null) ? true : (rule.match(url) == CrawlRule.INCLUDE);
  }
  
  public boolean storeProbePermission() {
    return plugin.storeProbePermission();
  }
  
  public boolean sendReferrer() {
    return plugin.sendReferrer();
  }

  public boolean shouldRefetchOnCookies() {
    return shouldRefetchOnCookies;
  }
  
  @Deprecated
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

  public final String getFetchRateLimiterKey() {
    String limiterSource = getFetchRateLimiterSource();
    if (log.isDebug3()) log.debug3("Limiter source: " + limiterSource);

    String key = null;
    if ("au".equalsIgnoreCase(limiterSource)) {
      key = null;
    }
    else if ("plugin".equalsIgnoreCase(limiterSource)) {
      key = plugin.getPluginId();
    }
    else if (StringUtil.startsWithIgnoreCase(limiterSource, "title_attribute:")) {
      // "title_attribute:attr" or "title_attribute:attr:dflt"
      String attr = limiterSource.substring("title_attribute:".length());
      String dflt = null;
      int ix = attr.indexOf(':');
      if (ix >= 0) {
        dflt = attr.substring(ix + 1);
        attr = attr.substring(0, ix);
      }
      key = AuUtil.getTitleAttribute(this, attr, dflt);
      if (key != null) {
        key = attr + ":" + key;
      }
    }
    else if (StringUtil.startsWithIgnoreCase(limiterSource, "key:")) {
      key = limiterSource.substring("key:".length());
    }
    else if (StringUtil.startsWithIgnoreCase(limiterSource, "host:")) {
      String param = limiterSource.substring("host:".length());
      key = paramMap.getString(param + SUFFIX_AU_HOST);
      if (key != null) {
        key = "host:" + key;
      }
    }
    
    if (key == null && log.isDebug()) {
      log.warning("Rate limiter source (" + limiterSource + ") is null, using AU");
    }
    if (log.isDebug3()) {
      log.debug3("Final rate limiter source is " + key);
    }
    return key;
  }

  protected String getFetchRateLimiterSource() {
    String defaultSource =
      CurrentConfig.getParam(PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE,
                             DEFAULT_DEFAULT_FETCH_RATE_LIMITER_SOURCE);
    String auSrc =
      paramMap.getString(KEY_AU_FETCH_RATE_LIMITER_SOURCE, defaultSource);
    return CurrentConfig.getParam(PARAM_OVERRIDE_FETCH_RATE_LIMITER_SOURCE,
                                  auSrc);
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

  public RateLimiterInfo getRateLimiterInfo() {
    long interval = paramMap.getLong(KEY_AU_FETCH_DELAY, defaultFetchDelay);
    return new RateLimiterInfo(getFetchRateLimiterKey(), interval);
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

  /** Override to provide permission path */
  public String getPerHostPermissionPath() {
    return null;
  }

  public List<String> getHttpCookies() {
    return Collections.emptyList();
  }

  public List<String> getHttpRequestHeaders() {
    return Collections.emptyList();
  }

  public String getName() {
    return paramMap.getString(KEY_AU_TITLE, auName);
  }

  public List<Pattern> makeExcludeUrlsFromPollsPatterns()
      throws ArchivalUnit.ConfigurationException {
    return null;
  }

  public AuCacheResultMap makeAuCacheResultMap()
      throws ArchivalUnit.ConfigurationException {
    return new AuHttpResultMap(plugin.getCacheResultMap(),
                               PatternMap.EMPTY);
  }

  public PatternStringMap makeUrlMimeTypeMap() {
    return PatternStringMap.EMPTY;
  }

  public PatternStringMap makeUrlMimeValidationMap() {
    return PatternStringMap.EMPTY;
  }

  public PatternFloatMap makeUrlPollResultWeightMap()
      throws ArchivalUnit.ConfigurationException {
    return PatternFloatMap.EMPTY;
  }

  public List<Pattern> makeNonSubstanceUrlPatterns()
      throws ArchivalUnit.ConfigurationException {
    return null;
  }

  public List<Pattern> makeSubstanceUrlPatterns()
      throws ArchivalUnit.ConfigurationException {
    return null;
  }

  public SubstancePredicate makeSubstancePredicate()
      throws ArchivalUnit.ConfigurationException, PluginException.LinkageError {
    return null;
  }

  public List<Pattern> makePermittedHostPatterns()
      throws ArchivalUnit.ConfigurationException {
    return null;
  }

  public List<Pattern> makeRepairFromPeerIfMissingUrlPatterns()
      throws ArchivalUnit.ConfigurationException {
    return null;
  }

  /**
   * Returns the CrawlWindow by default null which means crawl anytime
   * @return CrawlWindow or null
   */
  protected CrawlWindow makeCrawlWindow() {
          return null;
  }

  public boolean inCrawlWindow() {
    return (window == null) ? true : window.canCrawl();
  }
  
  public CrawlWindow getCrawlWindow() {
    return window;
  }

  
  /**
   * subclasses must implement this method to make and return the Crawl Rules
   * needed to crawl content.
   * @return CrawlRule object containing the necessary rules
   * @throws LockssRegexpException if the rules contain an unacceptable
   * regular expression.
   */
  abstract protected CrawlRule makeRule() throws ConfigurationException;
  
  public CrawlRule getRule() {
    return rule;
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
      if (log.isDebug2()) {
        log.debug2("Pub down: no new content crawl possible for "+aus);
      }
      return false;
    }
    long timeDiff = TimeBase.msSince(aus.getLastCrawlTime());
    boolean res = !aus.hasCrawled() || timeDiff > (newContentCrawlIntv);
    if (log.isDebug2()) {
      log.debug2("New content crawl" + (res ? "" : " not") +
                    " needed for "+ aus);
    }
    return res;
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
   * Returns the hash rule specified by the plugin, or null
   * @param contentType the content type
   * @return the FilterRule
   * @deprecated
   */

  public FilterRule getFilterRule(String contentType) {
    return plugin.getFilterRule(contentType);
  }

  /**
   * Returns the hash filter factory specified by the plugin, or null
   * @param contentType the content type
   * @return the FilterFactory
   */
  public FilterFactory getHashFilterFactory(String contentType) {
    return plugin.getHashFilterFactory(contentType);
  }

  /**
   * Returns the crawl filter factory specified by the plugin, or null
   * @param contentType the content type
   * @return the FilterFactory
   */
  public FilterFactory getCrawlFilterFactory(String contentType) {
    return plugin.getCrawlFilterFactory(contentType);
  }

  /**
   * Returns the link rewriter factory specified by the plugin, or null
   * @param contentType the content type
   * @return the LinkFactory
   */
  public LinkRewriterFactory getLinkRewriterFactory(String contentType) {
    return plugin.getLinkRewriterFactory(contentType);
  }

  /**
   * Returns the ContentValidatorFactory specified by the plugin, or null
   * @param contentType the content type
   * @return the ContentValidatorFactory
   */
  public ContentValidatorFactory
    getContentValidatorFactory(String contentType) {
    return plugin.getContentValidatorFactory(contentType);
  }

  public boolean isBulkContent() {
    return plugin.isBulkContent();
  }

  public ArchiveFileTypes getArchiveFileTypes() {
    return plugin.getArchiveFileTypes();
  }

  /**
   * Returns an article iterator from the AU's plugin.  If there isn't
   * one, return an empty iterator.
   * @return an Iterator over the AU's ArticleFiles.
   */
  public Iterator<ArticleFiles> getArticleIterator(MetadataTarget target) {
    Iterator<ArticleFiles> ret = CollectionUtil.emptyIterator();
    ArticleIteratorFactory aif = plugin.getArticleIteratorFactory();
    if (aif != null) try {
      Iterator<ArticleFiles> it = aif.createArticleIterator(this, target);
      if (it != null) {
        ret = it;
      }
    } catch (PluginException ex) {
      log.warning("createArticleIterator(" + target + ") threw " + ex);
    }
    return ret;
  }
  
  /**
   * Return a {@link FileMetadataExtractor} that knows how to extract URLs
   * from content of the given MIME type
   * @param target the purpose for which metadata is being extracted
   * @param contentType content type to get a content parser for
   * @return A FileMetadataExtractor or null
   */
  public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target,
                                                        String contentType) {
    return plugin.getFileMetadataExtractor(target, contentType, this);
  }

  /**
   * Returns the article iterator for the default content type.
   * @return the Iterator for the AU's articles.
   */
  public Iterator<ArticleFiles> getArticleIterator() {
    return getArticleIterator(null);
  }


  public long getArticleCount() {
    long ret = 0;
    for (Iterator<ArticleFiles> it = getArticleIterator(); it.hasNext(); ) {
      it.next();
      ret++;
    }
    return ret;
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
    HashMap<String,ParamHandler> handlerMap = new HashMap<String,ParamHandler>();

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
