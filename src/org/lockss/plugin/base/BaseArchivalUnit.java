/*
 * $Id: BaseArchivalUnit.java,v 1.24 2003-05-06 02:25:24 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import gnu.regexp.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.daemon.*;
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
  public static final String PARAM_TOP_LEVEL_POLL_INTERVAL_MIN =
      TOPLEVEL_POLL_PREFIX + "interval.min";
  static final long DEFAULT_TOP_LEVEL_POLL_INTERVAL_MIN = 2 * Constants.WEEK;

  /**
   * Configuration parameter name for maximum interval, in ms, by which
   * a new top level poll should have been called.  Actual interval is randomly
   * distributed between min and max.
   */
  public static final String PARAM_TOP_LEVEL_POLL_INTERVAL_MAX =
      TOPLEVEL_POLL_PREFIX + "interval.max";
  static final long DEFAULT_TOP_LEVEL_POLL_INTERVAL_MAX = 3 * Constants.WEEK;

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

  protected Plugin plugin;
  protected CrawlSpec crawlSpec;
  private String idStr = null;
  static Logger logger = Logger.getLogger("BaseArchivalUnit");

  protected long nextPollInterval = -1;
  protected double curTopLevelPollProb = -1;
  Random random = new Random();

  protected Configuration auConfig;

  private String auId = null;

  protected BaseArchivalUnit(Plugin myPlugin) {
    plugin = myPlugin;
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

  /**
   * Returns the plugin for this AU
   * @return the plugin for this AU
   */
  protected Plugin getPlugin() {
    return plugin;
  }

  // Factories that must be implemented by plugin subclass

  /**
   * Create an instance of the plugin-specific implementation of
   * CachedUrlSet, with the specified owner and CachedUrlSetSpec
   * @param owner the ArchivalUnit owner
   * @param cuss the spec
   * @return the cus
   */
  public abstract CachedUrlSet cachedUrlSetFactory(ArchivalUnit owner,
						   CachedUrlSetSpec cuss);

  /**
   * Create an instance of the plugin-specific implementation of
   * CachedUrl, with the specified owner and url
   * @param owner the CachedUrlSet owner
   * @param url the url
   * @return the CachedUrl
   */
  public abstract CachedUrl cachedUrlFactory(CachedUrlSet owner,
					     String url);

  /**
   * Create an instance of the plugin-specific implementation of
   * UrlCacher, with the specified owner and url
   * @param owner the CachedUrlSet owner
   * @param url the url
   * @return the UrlCacher
   */
  public abstract UrlCacher urlCacherFactory(CachedUrlSet owner,
					     String url);

  /**
   * Creates id by joining the plugin id to the canonical representation of
   * the defining properties as an encoded string
   *
   * @return id by joining the plugin id to the canonical representation of
   * the defining properties as an encoded string
   */
  public final String getAUId() {
    if (auId == null) {
      Collection defKeys = getPlugin().getDefiningConfigKeys();
      Properties props = new Properties();
      for (Iterator it = defKeys.iterator(); it.hasNext();) {
	String curKey = (String)it.next();
	props.setProperty(curKey, auConfig.get(curKey));
      }
      auId = PluginManager.generateAUId(getPluginId(), props);
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
   * Create a CachedUrlSet representing the content in this AU
   * that matches the CachedUrlSetSpec
   * @param cuss the spec
   * @return the CachedUrlSet
   */
  public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
    CachedUrlSet cus = cachedUrlSetFactory(this, cuss);
    return cus;
  }

  /**
   * Return the CachedUrlSet representing the entire contents
   * of this AU
   * @return the CachedUrlSet
   */
  public CachedUrlSet getAUCachedUrlSet() {
    // tk - use singleton instance?
    return makeCachedUrlSet(new AUCachedUrlSetSpec());
  }

  public void pause() {
    pause(DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS);
  }

  public String toString() {
    return "[BAU: "+getAUId()+"]";
  }

  /**
   * Simplified implementation which returns true if a crawl has never
   * been done, otherwise false
   * @param aus the {@link AuState}
   * @return true iff no crawl done
   */
  public boolean shouldCrawlForNewContent(AuState aus) {
    if (aus.getLastCrawlTime() <= 0) {
      return true;
    }
    return false;
  }

  /**
   * Simplified implementation which gets the poll interval parameter
   * and compares now vs. the last poll time.
   * @param aus the {@link AuState}
   * @return true iff a top level poll should be called
   */
  public boolean shouldCallTopLevelPoll(AuState aus) {
    Configuration config = Configuration.getCurrentConfig();
    if (nextPollInterval==-1) {
      long minPollInterval =
          config.getTimeInterval(PARAM_TOP_LEVEL_POLL_INTERVAL_MIN,
                                 DEFAULT_TOP_LEVEL_POLL_INTERVAL_MIN);
      long maxPollInterval =
          config.getTimeInterval(PARAM_TOP_LEVEL_POLL_INTERVAL_MAX,
                                 DEFAULT_TOP_LEVEL_POLL_INTERVAL_MAX);
      if (maxPollInterval <= minPollInterval) {
        maxPollInterval = 2 * minPollInterval;
      }
      nextPollInterval =
          Deadline.inRandomRange(minPollInterval,
                                 maxPollInterval).getRemainingTime();
    }
    if (curTopLevelPollProb==-1) {
      // reset to initial prob
      curTopLevelPollProb = config.getPercentage(
          PARAM_TOPLEVEL_POLL_PROB_INITIAL, DEFAULT_TOPLEVEL_POLL_PROB_INITIAL);
    }

    logger.debug("Deciding whether to call a top level poll");
    logger.debug3("Last poll at "+ aus.getLastTopLevelPollTime());
    logger.debug3("Poll interval: "+StringUtil.timeIntervalToString(
        nextPollInterval));
    logger.debug3("Poll likelihood: "+curTopLevelPollProb);
    if (TimeBase.msSince(aus.getLastTopLevelPollTime()) > nextPollInterval) {
      // reset poll interval regardless
      nextPollInterval = -1;
      // choose probabilistically whether to call
      if (random.nextDouble() < curTopLevelPollProb) {
        logger.debug("Allowing poll.");
        curTopLevelPollProb = -1;
        return true;
      } else {
        logger.debug("Skipping poll.");
        // decided not to call the poll
        double topLevelPollProbMax =
            config.getPercentage(PARAM_TOPLEVEL_POLL_PROB_MAX,
                                 DEFAULT_TOPLEVEL_POLL_PROB_MAX);
        if (curTopLevelPollProb < topLevelPollProbMax) {
          // if less than max prob, increment
          curTopLevelPollProb += config.getPercentage(
              PARAM_TOPLEVEL_POLL_PROB_INCREMENT,
              DEFAULT_TOPLEVEL_POLL_PROB_INCREMENT);
          if (curTopLevelPollProb > topLevelPollProbMax) {
            curTopLevelPollProb = topLevelPollProbMax;
          }
        }
      }
    }
    return false;
  }

  protected void pause(long milliseconds) {
    logger.debug3("Pausing for "+milliseconds+" milliseconds");
    try {
      Thread thread = Thread.currentThread();
      thread.sleep(milliseconds);
    } catch (InterruptedException ie) {
    }
  }
}
