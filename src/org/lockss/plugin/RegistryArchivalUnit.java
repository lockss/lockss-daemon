/*
 * $Id: RegistryArchivalUnit.java,v 1.23 2007-10-04 04:03:32 tlipkis Exp $
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

package org.lockss.plugin;

import java.io.FileNotFoundException;
import java.net.*;
import java.util.List;

import org.htmlparser.*;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.util.*;

import org.lockss.config.*;
import org.lockss.crawler.NewContentCrawler;
import org.lockss.daemon.*;
import org.lockss.plugin.base.BaseArchivalUnit;
import org.lockss.state.*;
import org.lockss.util.*;

/**
 * <p>PluginArchivalUnit: The Archival Unit Class for PluginPlugin.
 * This archival unit uses a base url to define an archival unit.
 * @author Seth Morabito
 * @version 1.0
 */

public class RegistryArchivalUnit extends BaseArchivalUnit {
  protected static final Logger log = Logger.getLogger("RegistryArchivalUnit");

  /** The interval between recrawls of the loadable plugin
      registry AUs.  */
  static final String PARAM_REGISTRY_CRAWL_INTERVAL =
    RegistryPlugin.PREFIX + "crawlInterval";
  static final long DEFAULT_REGISTRY_CRAWL_INTERVAL = Constants.DAY;

  /** If "au", registry AUs will crawl in parallel using individual
   * rate limiters; if "plugin" they'll crawl sequentially using a shared
   * rate limiter */
  static final String PARAM_REGISTRY_FETCH_RATE_LIMITER_SOURCE =
    RegistryPlugin.PREFIX + "fetchRateLimiterSource";
  static final String DEFAULT_REGISTRY_FETCH_RATE_LIMITER_SOURCE = "au";

  /** Limits fetch rate of registry crawls */
  static final String PARAM_REGISTRY_FETCH_RATE =
    RegistryPlugin.PREFIX + "fetchRate";
  static final String DEFAULT_REGISTRY_FETCH_RATE = "20/10s";

  private String m_registryUrl = null;
  private int m_maxRefetchDepth = NewContentCrawler.DEFAULT_MAX_CRAWL_DEPTH;
  private List m_permissionCheckers = null;
  private boolean recomputeRegName = true;
  private String regName = null;

  public RegistryArchivalUnit(RegistryPlugin plugin) {
    super(plugin);
  }

  // Called by RegistryPlugin iff any config below RegistryPlugin.PREFIX
  // has changed
  protected void setConfig(Configuration config,
			   Configuration prevConfig,
			   Configuration.Differences changedKeys) {
    m_maxRefetchDepth =
      config.getInt(NewContentCrawler.PARAM_MAX_CRAWL_DEPTH,
		    NewContentCrawler.DEFAULT_MAX_CRAWL_DEPTH);
    fetchRateLimiter = recomputeFetchRateLimiter(fetchRateLimiter);
  }

  public void loadAuConfigDescrs(Configuration config)
      throws ConfigurationException {
    super.loadAuConfigDescrs(config);
    this.m_registryUrl = config.get(ConfigParamDescr.BASE_URL.getKey());
    // Now we can construct a valid CC permission checker.
    m_permissionCheckers =
//       ListUtil.list(new CreativeCommonsPermissionChecker(m_registryUrl));
      ListUtil.list(new CreativeCommonsPermissionChecker());

    paramMap.putLong(KEY_AU_NEW_CONTENT_CRAWL_INTERVAL,
		     CurrentConfig
		     .getTimeIntervalParam(PARAM_REGISTRY_CRAWL_INTERVAL,
					   DEFAULT_REGISTRY_CRAWL_INTERVAL));
    if (log.isDebug2()) {
      log.debug2("Setting Registry AU recrawl interval to " +
		 StringUtil.timeIntervalToString(paramMap.getLong(KEY_AU_NEW_CONTENT_CRAWL_INTERVAL)));
    }
  }

  /**
   * return a string that represents the plugin registry.  This is
   * just the base URL.
   * @return The base URL.
   */
  protected String makeName() {
    return "Plugin registry at '" + m_registryUrl + "'";
  }

  public String getName() {
    if (recomputeRegName) {
      regName = recomputeRegName();
    }
    if (regName != null) {
      return regName;
    } else {
      return super.getName();
    }
  }

  // If there is a <title> element on the start page, use that as our AU
  // name.
  String recomputeRegName() {
    if (!isStarted()) {
      // This can get invoked (seveeral times, mostly from logging) before
      // enough mechanism has started to make it possible to resolve the CuUrl
      // below.
      return null;
    }
    try {
      CachedUrl cu = makeCachedUrl(m_registryUrl);
      if (cu == null) return null;
      URL cuUrl = CuUrl.fromCu(cu);
      Parser parser = new Parser(cuUrl.toString());
      NodeList nodelst =
	parser.extractAllNodesThatMatch(new NodeClassFilter(TitleTag.class));
      Node nodes [] = nodelst.toNodeArray();
      recomputeRegName = false;
      if (nodes.length < 1) return null;
      // Get the first title found
      TitleTag tag = (TitleTag)nodes[0];
      if (tag == null) return null;
      return tag.getTitle();
    } catch (MalformedURLException e) {
      log.warning("recomputeRegName", e);
      return null;
    } catch (ParserException e) {
      if (e.getThrowable() instanceof FileNotFoundException) {
	log.warning("recomputeRegName: " + e.getThrowable().toString());
      } else {
	log.warning("recomputeRegName", e);
      }
      return null;
    }
  }

  boolean isStarted() {
    return getPlugin().getDaemon().getPluginManager().getAuFromId(getAuId())
      != null;
  }

  /**
   * return a string that points to the plugin registry page.
   * @return a string that points to the plugin registry page for
   * this registry.  This is just the base URL.
   */
  protected String makeStartUrl() {
    return m_registryUrl;
  }

  /** This AU should never call a top level poll.
   */
  public boolean shouldCallTopLevelPoll(AuState aus) {
    return false;
  }

  /**
   * Return a new CrawlSpec with the appropriate collect AND redistribute
   * permissions, and with the maximum refetch depth.
   *
   * @return CrawlSpec
   */
  protected CrawlSpec makeCrawlSpec() throws LockssRegexpException {
    CrawlRule rule = makeRules();
    List startUrls = ListUtil.list(startUrlString);
    return new SpiderCrawlSpec(startUrls, startUrls, rule,
			       m_maxRefetchDepth, null, null);
  }

  /**
   * return the collection of crawl rules used to crawl and cache a
   * list of Plugin JAR files.
   * @return CrawlRule
   */
  protected CrawlRule makeRules() {
    return new RegistryRule();
  }

  // Might need to recompute name if refetch start page
  public UrlCacher makeUrlCacher(String url) {
    if (url.equals(m_registryUrl)) {
      recomputeRegName = true;
    }
    return super.makeUrlCacher(url);
  }

  protected RateLimiter recomputeFetchRateLimiter(RateLimiter oldLimiter) {
    String rate = CurrentConfig.getParam(PARAM_REGISTRY_FETCH_RATE,
					 DEFAULT_REGISTRY_FETCH_RATE);
    Object limiterKey = getFetchRateLimiterKey();

    if (limiterKey == null) {
      return RateLimiter.getRateLimiter(oldLimiter, rate,
					DEFAULT_REGISTRY_FETCH_RATE);
    } else {
      RateLimiter.Pool pool = RateLimiter.getPool();
      return pool.findNamedRateLimiter(limiterKey, rate,
				       DEFAULT_REGISTRY_FETCH_RATE);
    }
  }

  protected String getFetchRateLimiterSource() {
    return CurrentConfig.getParam(PARAM_REGISTRY_FETCH_RATE_LIMITER_SOURCE,
				  DEFAULT_REGISTRY_FETCH_RATE_LIMITER_SOURCE);
  }

  // Registry AU crawl rule implementation
  private class RegistryRule implements CrawlRule {
    public int match(String url) {
      if (StringUtil.equalStringsIgnoreCase(url, m_registryUrl) ||
	  StringUtil.endsWithIgnoreCase(url, ".jar")) {
	return CrawlRule.INCLUDE;
      } else {
	return CrawlRule.EXCLUDE;
      }
    }
  }
}
