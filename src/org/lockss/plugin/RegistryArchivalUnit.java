/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.htmlparser.*;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.util.*;
import org.lockss.config.*;
import org.lockss.crawler.FollowLinkCrawler;
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
	
  private static final Logger log = Logger.getLogger(RegistryArchivalUnit.class);

  /** The interval between recrawls of the loadable plugin
      registry AUs.  Changes take effect only when AU is started. */
  static final String PARAM_REGISTRY_CRAWL_INTERVAL =
    RegistryPlugin.PREFIX + "crawlInterval";
  static final long DEFAULT_REGISTRY_CRAWL_INTERVAL = Constants.DAY;

  /** The proxy to use for registry crawls, or DIRECT to override a global
   * crawl proxy.  Changes take effect at start of next crawl. */
  static final String PARAM_REGISTRY_CRAWL_PROXY =
    RegistryPlugin.PREFIX + "crawlProxy";

  /** If "au", registry AUs will crawl in parallel using individual
   * rate limiters; if "plugin" they'll crawl sequentially using a shared
   * rate limiter */
  static final String PARAM_REGISTRY_FETCH_RATE_LIMITER_SOURCE =
    RegistryPlugin.PREFIX + "fetchRateLimiterSource";
  static final String DEFAULT_REGISTRY_FETCH_RATE_LIMITER_SOURCE = "au";

  /** Limits fetch rate of registry crawls */
  static final String PARAM_REGISTRY_FETCH_RATE =
    RegistryPlugin.PREFIX + "fetchRate";
  static final String DEFAULT_REGISTRY_FETCH_RATE = "500/1s";

  /** Run polls on Plugin registry AUs */
  static final String PARAM_ENABLE_REGISTRY_POLLS =
    RegistryPlugin.PREFIX + "enablePolls";
  static final boolean DEFAULT_ENABLE_REGISTRY_POLLS = true;

  private String m_registryUrl = null;
  private int m_refetchDepth = FollowLinkCrawler.DEFAULT_MAX_CRAWL_DEPTH;
  private boolean recomputeRegName = true;
  private boolean enablePolls = DEFAULT_ENABLE_REGISTRY_POLLS;
  private String regName = null;

  public RegistryArchivalUnit(RegistryPlugin plugin) {
    super(plugin);
  }

  // Called by RegistryPlugin iff any config below RegistryPlugin.PREFIX
  // has changed
  protected void setConfig(Configuration config,
			   Configuration prevConfig,
			   Configuration.Differences changedKeys) {
    m_refetchDepth =
      config.getInt(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH,
          FollowLinkCrawler.DEFAULT_MAX_CRAWL_DEPTH);
    fetchRateLimiter = recomputeFetchRateLimiter(fetchRateLimiter);
    enablePolls = config.getBoolean(PARAM_ENABLE_REGISTRY_POLLS,
				    DEFAULT_ENABLE_REGISTRY_POLLS);
    if (changedKeys.contains(PARAM_REGISTRY_CRAWL_PROXY)) {
      String proxy = config.get(PARAM_REGISTRY_CRAWL_PROXY);
      paramMap.putString(ConfigParamDescr.CRAWL_PROXY.getKey(), proxy);
      log.debug2("Setting Registry AU crawl proxy to " + proxy);
    }

  }

  public void loadAuConfigDescrs(Configuration auConfig)
      throws ConfigurationException {
    super.loadAuConfigDescrs(auConfig);
    this.m_registryUrl = auConfig.get(ConfigParamDescr.BASE_URL.getKey());

    Configuration config = CurrentConfig.getCurrentConfig();
    paramMap.putLong(KEY_AU_NEW_CONTENT_CRAWL_INTERVAL,
		     config.getTimeInterval(PARAM_REGISTRY_CRAWL_INTERVAL,
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
      return StringEscapeUtils.unescapeHtml4(tag.getTitle());
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
  public Collection<String> getStartUrls() {
    return ListUtil.list(m_registryUrl);
  }

  /** Call top level polls iff configured to do so.
   */
  public boolean shouldCallTopLevelPoll(AuState aus) {
    if (!enablePolls) {
      return false;
    }
    return super.shouldCallTopLevelPoll(aus);
  }

  /**
   * return the collection of crawl rules used to crawl and cache a
   * list of Plugin JAR files.
   * @return CrawlRule
   */
  protected CrawlRule makeRule() {
    return new RegistryRule();
  }

  // Might need to recompute name if refetch start page
  public UrlCacher makeUrlCacher(UrlData ud) {
    if (ud.url.equals(m_registryUrl)) {
      recomputeRegName = true;
    }
    return super.makeUrlCacher(ud);
  }

  public RateLimiterInfo getRateLimiterInfo() {
    String rate = CurrentConfig.getParam(PARAM_REGISTRY_FETCH_RATE,
					 DEFAULT_REGISTRY_FETCH_RATE);
    return new RateLimiterInfo(getFetchRateLimiterKey(), rate);
  }

  protected RateLimiter recomputeFetchRateLimiter(RateLimiter oldLimiter) {
    String rate = CurrentConfig.getParam(PARAM_REGISTRY_FETCH_RATE,
					 DEFAULT_REGISTRY_FETCH_RATE);
    String limiterKey = getFetchRateLimiterKey();

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

  public List<PermissionChecker> makePermissionCheckers() {
    return null;
  }

  public int getRefetchDepth() {
    return m_refetchDepth;
  }

  public LoginPageChecker getLoginPageChecker() {
    return null;
  }

  @Override
  public String getCookiePolicy() {
    return null;
  }

}
