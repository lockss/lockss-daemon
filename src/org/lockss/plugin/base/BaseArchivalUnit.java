/*
 * $Id: BaseArchivalUnit.java,v 1.49 2004-01-13 02:20:02 eaalto Exp $
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

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import gnu.regexp.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.filter.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Configuration.*;
import org.apache.commons.collections.LRUMap;

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
  static final long DEFAULT_TOPLEVEL_POLL_INTERVAL_MAX = 3 * Constants.WEEK;

  /**
   * Configuration parameter name for top level poll initial probability.
   */
  public static final String PARAM_TOPLEVEL_POLL_PROB_INITIAL =
      TOPLEVEL_POLL_PREFIX + "prob.initial";
  static final double DEFAULT_TOPLEVEL_POLL_PROB_INITIAL = .5;

  /**
   * Configuration parameter name for top level poll increment
   */
  public static final String PARAM_TOPLEVEL_POLL_PROB_INCREMENT =
      TOPLEVEL_POLL_PREFIX + "prob.increment";
  static final double DEFAULT_TOPLEVEL_POLL_PROB_INCREMENT = .05;

  /**
   * Configuration parameter name for top level poll max probability.
   */
  public static final String PARAM_TOPLEVEL_POLL_PROB_MAX =
      TOPLEVEL_POLL_PREFIX + "prob.max";
  static final double DEFAULT_TOPLEVEL_POLL_PROB_MAX = 1.0;


  private static final long
    DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS = 10 * Constants.SECOND;

  public static final String NEW_CONTENT_CRAWL_KEY = "nc_interval";
  public static final String PAUSE_TIME_KEY = "pause_time";

  public static final String PERMISSION_STRING =
  "LOCKSS system has permission to collect, preserve, and serve this Archival Unit";

  public static final long
      DEFAULT_NEW_CONTENT_CRAWL_INTERVAL = 2 * Constants.WEEK;
  protected Plugin plugin;
  protected CrawlSpec crawlSpec;
  static Logger logger = Logger.getLogger("BaseArchivalUnit");
  static SimpleDateFormat sdf = new SimpleDateFormat();

  protected String expectedUrlPath = "/";
  protected long minFetchDelay = 6 * Constants.SECOND;
  protected long fetchDelay;
  protected long defaultFetchDelay = DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS;
  protected String startUrlString;
  protected long newContentCrawlIntv;
  protected long defaultContentCrawlIntv = DEFAULT_NEW_CONTENT_CRAWL_INTERVAL;
  protected URL baseUrl; // the base Url for the volume
  protected String auName;

  protected long nextPollInterval = -1;
  protected double curTopLevelPollProb = -1;

  protected Configuration auConfig;
  protected ExternalizableMap configMap;

  private String auId = null;

  protected BaseArchivalUnit(Plugin myPlugin) {
    plugin = myPlugin;
    if (myPlugin != null) {
      configMap = ((BasePlugin)myPlugin).getConfigurationMap();
    }
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
    auConfig = config;
    if (config == null) {
      throw new ConfigurationException("Null Configuration");
    }
    loadDefiningConfig(config);
    setAuParams(config);
    setBaseAuParams(config);
  }

  public Configuration getConfiguration() {
    return auConfig;
  }

  private void checkLegalConfigChange(Configuration newConfig)
      throws ArchivalUnit.ConfigurationException {
    Collection defKeys = plugin.getDefiningConfigKeys();
    for (Iterator it = defKeys.iterator(); it.hasNext();) {
      String curKey = (String)it.next();
      String oldVal = auConfig.get(curKey);
      String newVal = newConfig.get(curKey);
      if (!StringUtil.equalStrings(oldVal, newVal)) {
	throw new ConfigurationException("Attempt to modify defining property "
					 +"of existing ArchivalUnit: "+curKey
					 +". old: "+oldVal+" new: "+newVal);
      }
    }
  }

  void loadDefiningConfig(Configuration config) throws ConfigurationException {
    List descrList = plugin.getAuConfigProperties();
    for (Iterator it = descrList.iterator(); it.hasNext();) {
      Object next = it.next();
      // check for simulated plugin configuration
      if (!(next instanceof ConfigParamDescr)) {
        logger.debug("AU config properties are not of type ConfigParamDescr" +
                     "- skipping configuration map storage.");
        return;
      }
      ConfigParamDescr descr = (ConfigParamDescr)next;
      String key = descr.getKey();
      switch (descr.getType()) {
        case ConfigParamDescr.TYPE_INT:
          int i_val = loadConfigInt(descr, config);
          configMap.putInt(key, i_val);
          break;
        case ConfigParamDescr.TYPE_STRING:
          String s_val = loadConfigString(descr, config);
          configMap.putString(key, s_val);
          break;
        case ConfigParamDescr.TYPE_URL:
          URL u_val = loadConfigUrl(descr, config);
          configMap.putUrl(key, u_val);
          break;
      }
    }
  }

  protected void setBaseAuParams(Configuration config)
      throws ConfigurationException {
    // get the base url string
    baseUrl = configMap.getUrl(ConfigParamDescr.BASE_URL.getKey(), null);

    // get the fetch delay
    fetchDelay = config.getTimeInterval(PAUSE_TIME_KEY, defaultFetchDelay);
    fetchDelay = Math.max(fetchDelay, minFetchDelay);
    logger.debug2("Set fetch delay to " + fetchDelay);

    // get the new content crawl interval
    newContentCrawlIntv = config.getTimeInterval(NEW_CONTENT_CRAWL_KEY,
                                                 defaultContentCrawlIntv);
    logger.debug2("Set new content crawl interval to " + newContentCrawlIntv);

    // make the start url
    startUrlString = makeStartUrl();

    // make our crawl spec
    try {
      crawlSpec = makeCrawlSpec();
    } catch (REException e) {
      throw new ConfigurationException("Illegal RE", e);
    }

    // make our name
    auName = makeName();
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
      Collection defKeys = getPlugin().getDefiningConfigKeys();
      Properties props = new Properties();
      for (Iterator it = defKeys.iterator(); it.hasNext();) {
	String curKey = (String)it.next();
	props.setProperty(curKey, auConfig.get(curKey));
      }
      auId = PluginManager.generateAuId(getPluginId(), props);
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
    return crawlSpec;
  }

  public Collection getUrlStems() {
    try {
      URL stem = new URL(baseUrl.getProtocol(), baseUrl.getHost(),
                         baseUrl.getPort(), "");
      return ListUtil.list(stem.toString());
    } catch (MalformedURLException e) {
      return Collections.EMPTY_LIST;
    }
  }

  LRUMap crawlSpecCache = new LRUMap(1000);
  int hits = 0;
  int misses = 0;

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
    return fetchDelay;
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

  public String getManifestPage() {
    return startUrlString;
  }

  public String getName() {
    return auName;
  }

  /**
   * Use the starting url and the crawl rules to make the crawl spec needed
   * to crawl this au.
   * @return the CrawlSpec need by this au.
   * @throws REException if the CrawlRules contain an invalid regular expression
   */
  protected CrawlSpec makeCrawlSpec() throws REException {

    CrawlRule rule = makeRules();
    return new CrawlSpec(startUrlString, rule);
  }

  /**
   * subclasses should use this method to add any parameters that are not part
   * of the base plugin.
   * @param config the Configuration object containing the new configuration
   * @throws ConfigurationException should be thrown if the information needed
   * is not found or is invalid.
   */
  abstract protected void setAuParams(Configuration config)
      throws ConfigurationException;

  /**
   * subclasses must implement this method to make and return the Crawl Rules
   * needed to crawl content.
   * @return CrawlRule object containing the necessary rules
   * @throws REException thrown if the rules contain an unacceptable regular
   * expression.
   */
  abstract protected CrawlRule makeRules() throws REException;

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

  public boolean checkCrawlPermission(Reader reader) {
    boolean crawl_ok = false;
    int ch;
    int p_index = 0;
    String matchstr = PERMISSION_STRING.toLowerCase();
    boolean wasWhiteSpace = false;  // last char was ws

    Reader filteredReader = getCrawlPermissionFilter(reader);

    try {
      do {
        ch = filteredReader.read();
        char nextChar = matchstr.charAt(p_index);

        if ((nextChar == Character.toLowerCase((char)ch))) {
          // match precisely, or any whitespace with any other
          if (++p_index == PERMISSION_STRING.length()) {
            return true;
          }
        } else {
          p_index = 0;
        }
      } while (ch != -1); // while not eof
    } catch (IOException ex) {
      logger.warning("Exception occured while checking for permission: "
                     + ex.toString());
    }

    return crawl_ok;
  }

  private Reader getCrawlPermissionFilter(Reader reader) {
    // convert html tags to whitespace
    Reader replaceReader = StringFilter.makeNestedFilter(reader,
        new String[][] {
        { "<br>", " " },
        { "&nbsp;", " " }
    },
        true);

    // condense whitespace
    InputStream wsIs =
        new WhiteSpaceFilter(new ReaderInputStream(replaceReader));
    return new InputStreamReader(wsIs);
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
   * @param mimeType the mime type
   * @return null, since we don't filter by default
   */
  public FilterRule getFilterRule(String mimeType) {
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
    if (!expectedUrlPath.equals(url.getPath())) {
      throw new ConfigurationException(paramString(descr) +
				       " has illegal path: " +
                                       url.getPath() + ", expected: " +
                                       expectedUrlPath);
    }

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
    } catch (InvalidParam ip) {
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
}
