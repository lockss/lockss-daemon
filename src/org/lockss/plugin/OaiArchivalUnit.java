/*
 * $Id: OaiArchivalUnit.java,v 1.4 2006-12-06 05:19:02 tlipkis Exp $
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

import java.util.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.base.*;

/**
 * <p>PluginArchivalUnit: The Archival Unit Class for PluginPlugin.
 * This archival unit uses a base url to define an archival unit.
 * @author Seth Morabito
 * @version 1.0
 */

public class OaiArchivalUnit extends BaseArchivalUnit {
  private String m_oaiHandlerUrl = null;
  //private int m_maxRefetchDepth = NewContentCrawler.DEFAULT_MAX_CRAWL_DEPTH;

  protected Logger logger = Logger.getLogger("OaiArchivalUnit");

  public OaiArchivalUnit(OaiPlugin plugin) {
    super(plugin);
//     m_maxRefetchDepth =
//       Configuration.getIntParam(NewContentCrawler.PARAM_MAX_CRAWL_DEPTH,
// 				NewContentCrawler.DEFAULT_MAX_CRAWL_DEPTH);
  }

  public void loadAuConfigDescrs(Configuration config)
      throws ConfigurationException {
    super.loadAuConfigDescrs(config);
    this.m_oaiHandlerUrl = config.get(ConfigParamDescr.BASE_URL.getKey());
  }

  /**
   * return a string that represents the plugin registry.  This is
   * just the base URL.
   * @return The base URL.
   */
  protected String makeName() {
    return "Oai Test Repository, handler at '" + m_oaiHandlerUrl + "'";
  }

  /**
   * return a string that points to the plugin registry page.
   * @return a string that points to the plugin registry page for
   * this registry.  This is just the base URL.
   */
  protected String makeStartUrl() {
    return m_oaiHandlerUrl;
  }

  /** This AU should never call a top level poll.
   */
//   public boolean shouldCallTopLevelPoll(AuState aus) {
//     return false;
//   }

  /**
   * Return a new CrawlSpec with the appropriate collect AND redistribute
   * permissions, and with the maximum refetch depth.
   *
   * @return CrawlSpec
   */
  protected CrawlSpec makeCrawlSpec() throws LockssRegexpException {
    CrawlRule rule = makeRules();
    //XXXOAI return OaiCrawlSpec
    logger.debug("making OaiCrawlSpec");
    //    return new OaiCrawlSpec(startUrls,rule);
    return new OaiCrawlSpec(startUrlString, rule);
  }

  /**
   * return the collection of crawl rules used to crawl and cache a
   * list of Plugin JAR files.
   * @return CrawlRule
   */
  protected CrawlRule makeRules() {
    return null;
    //    return new OaiRule();
  }

  // Oai AU crawl rule implementation
  private class OaiRule implements CrawlRule {
    //change this
    public int match(String url) {
      if (StringUtil.equalStringsIgnoreCase(url, m_oaiHandlerUrl) ||
	  StringUtil.endsWithIgnoreCase(url, ".jar")) {
	return CrawlRule.INCLUDE;
      } else {
	return CrawlRule.EXCLUDE;
      }
    }
  }
}
