/*
 * $Id: ExplodedArchivalUnit.java,v 1.1.2.2 2007-09-11 22:24:19 dshr Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.exploded;

import java.util.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.base.*;

/**
 * <p>ExplodedArchivalUnit: The Archival Unit Class for ExplodedPlugin.
 * This archival unit uses a base url to define an archival unit.
 * @author David Rosenthal
 * @version 1.0
 */

public class ExplodedArchivalUnit extends BaseArchivalUnit {
  private String m_explodedHandlerUrl = null;
  protected Logger logger = Logger.getLogger("ExplodedArchivalUnit");

  public ExplodedArchivalUnit(ExplodedPlugin plugin) {
    super(plugin);
  }

  public void loadAuConfigDescrs(Configuration config)
      throws ConfigurationException {
    super.loadAuConfigDescrs(config);
    this.m_explodedHandlerUrl = config.get(ConfigParamDescr.BASE_URL.getKey());
  }


  // Called by RegistryPlugin iff any config below RegistryPlugin.PREFIX
  // has changed
  protected void setConfig(Configuration config,
			   Configuration prevConfig,
			   Configuration.Differences changedKeys) {
    // XXX
  }

  /**
   * return a string that represents the plugin registry.  This is
   * just the base URL.
   * @return The base URL.
   */
  protected String makeName() {
    return "Exploded Test Repository, handler at '" + m_explodedHandlerUrl + "'";
  }

  /**
   * return a string that points to the plugin registry page.
   * @return a string that points to the plugin registry page for
   * this registry.  This is just the base URL.
   */
  protected String makeStartUrl() {
    return m_explodedHandlerUrl;
  }

  /**
   * Determine whether the url falls within the CrawlSpec.
   * @param url the url
   * @return true if it is included
   */
  public boolean shouldBeCached(String url) {
    return url.startsWith(m_explodedHandlerUrl);
  }

  protected CrawlRule makeRules() {
    return new ExplodedCrawlRule();
  }

  private class ExplodedCrawlRule implements CrawlRule {
    protected ExplodedCrawlRule() {
    }

    public int match(String url) {
      if (url.startsWith(m_explodedHandlerUrl)) {
	return INCLUDE;
      }
      return IGNORE;
    }
  }
  /**
   * Return a new CrawlSpec with the appropriate collect AND redistribute
   * permissions, and with the maximum refetch depth.
   *
   * @return CrawlSpec
   */
  protected CrawlSpec makeCrawlSpec() throws LockssRegexpException {
    ArrayList startUrls = new ArrayList();
    startUrls.add(startUrlString);
    return new ExplodedCrawlSpec(startUrls);
  }

  // Exploded AU crawl spec implementation
  private class ExplodedCrawlSpec extends BaseCrawlSpec {
    ExplodedCrawlSpec(List startUrls) {
      // null CrawlRule is always INCLUDE
      // null PermissionChecker is XXX
      // null loginPageChecker is always false
      super(startUrls, null, null, null);
    }
  }
    
}
