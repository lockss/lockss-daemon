/*
 * $Id: RegistryArchivalUnit.java,v 1.5 2004-09-01 23:38:59 smorabito Exp $
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

import java.net.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.crawler.*;

/**
 * <p>PluginArchivalUnit: The Archival Unit Class for PluginPlugin.
 * This archival unit uses a base url to define an archival unit.
 * @author Seth Morabito
 * @version 1.0
 */

public class RegistryArchivalUnit extends BaseArchivalUnit {
  private String m_registryUrl = null;
  private int m_maxRefetchDepth = -1;

  private List m_permissionCheckers;

  protected Logger log = Logger.getLogger("RegistryArchivalUnit");

  public RegistryArchivalUnit(RegistryPlugin plugin) {
    super(plugin);
    m_maxRefetchDepth =
      Configuration.getIntParam(NewContentCrawler.PARAM_MAX_CRAWL_DEPTH,
				NewContentCrawler.DEFAULT_MAX_CRAWL_DEPTH);
  }

  public void loadAuConfigDescrs(Configuration config)
      throws ConfigurationException {
    this.m_registryUrl = config.get(ConfigParamDescr.BASE_URL.getKey());
  }

  /**
   * return a string that represents the plugin registry.  This is
   * just the base URL.
   * @return The base URL.
   */
  protected String makeName() {
    return "Plugin registry at '" + m_registryUrl + "'";
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
    return new CrawlSpec(startUrls, rule);
  }

  /**
   * return the collection of crawl rules used to crawl and cache a
   * list of Plugin JAR files.
   * @return CrawlRule
   */
  protected CrawlRule makeRules() {
    return new RegistryRule();
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
