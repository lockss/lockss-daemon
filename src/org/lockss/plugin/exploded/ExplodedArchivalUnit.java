/*
 * $Id: ExplodedArchivalUnit.java,v 1.7.18.1 2009-09-04 18:08:53 dshr Exp $
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
import java.net.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.definable.*;
import org.lockss.state.AuState;

/**
 * <p>ExplodedArchivalUnit: The Archival Unit Class for ExplodedPlugin.
 * This archival unit uses a base url to define an archival unit.
 * @author David Rosenthal
 * @version 1.0
 */

public class ExplodedArchivalUnit extends DefinableArchivalUnit {
  private String m_explodedBaseUrl = null;
  protected Logger logger = Logger.getLogger("ExplodedArchivalUnit");
  private ArrayList permissionPageUrls = new ArrayList();
  private ArrayList urlStems = new ArrayList();

  public ExplodedArchivalUnit(ExplodedPlugin plugin, ExternalizableMap defMap) {
    super(plugin, defMap);
  }

  public void loadAuConfigDescrs(Configuration config)
      throws ConfigurationException {
    super.loadAuConfigDescrs(config);
    this.m_explodedBaseUrl = config.get(ConfigParamDescr.BASE_URL.getKey());
    String pubName = config.get(ConfigParamDescr.PUBLISHER_NAME.getKey());
    if (pubName != null) {
      auName = pubName;
      // We have some config info to work with
      String journalId = config.get(ConfigParamDescr.JOURNAL_ISSN.getKey());
      if (journalId == null) {
	journalId = config.get(ConfigParamDescr.JOURNAL_ABBR.getKey());
	if (journalId == null) {
	  journalId = config.get(ConfigParamDescr.JOURNAL_DIR.getKey());
	}
      }
      if (journalId != null) {
	auName += " " + journalId;
      }
    }
    addUrlStemToAU(m_explodedBaseUrl);
  }


  /**
   * return a string that represents the plugin registry.  This is
   * just the base URL unless overridden by the config.
   * @return The base URL.
   */
  protected String makeName() {
    return (auName != null ? auName : m_explodedBaseUrl);
  }

  /**
   * return a string that points to the plugin registry page.
   * @return a string that points to the plugin registry page for
   * this registry.  This is just the base URL.
   */
  protected String makeStartUrl() {
    return m_explodedBaseUrl;
  }

  /**
   * Determine whether the url falls within the CrawlSpec.
   * @param url the url
   * @return true if it is included
   */
  public boolean shouldBeCached(String url) {
    for (Iterator it = urlStems.iterator(); it.hasNext(); ) {
      String stem = (String)it.next();
      logger.debug3("shouldBeCached(" + url + ") stem " + stem);
      if (url.startsWith(stem)) {
	return true;
      }
    }
    return false;
  }

  public Collection getUrlStems() {
    return new ArrayList(urlStems);
  }

  protected List getPermissionPages() {
    return new ArrayList(permissionPageUrls);
  }

  protected CrawlRule makeRules() {
    return new ExplodedCrawlRule();
  }

  private class ExplodedCrawlRule implements CrawlRule {
    protected ExplodedCrawlRule() {
    }

    public int match(String url) {
      for (Iterator it = urlStems.iterator(); it.hasNext(); ) {
	String stem = (String)it.next();
	logger.debug3("match(" + url + ") stem " + stem);
	if (url.startsWith(stem)) {
	  return INCLUDE;
	}
      }
      return EXCLUDE;
    }
  }
  /**
   * Return a new CrawlSpec with the appropriate collect AND redistribute
   * permissions, and with the maximum refetch depth.
   *
   * @return CrawlSpec
   */
  protected CrawlSpec makeCrawlSpec() throws LockssRegexpException {
    return new ExplodedCrawlSpec(getNewContentCrawlUrls());
  }

  /**
   * ExplodedArchivalUnits should never be crawled.
   * @param aus ignored
   * @return false
   */
  public boolean shouldCrawlForNewContent(AuState aus) {
    return false;
  }

  /**
   * Add the url to the list of "permission pages" for the AU
   * @param url
   */
  public void addUrlStemToAU(String url) {
    try {
      String stem = UrlUtil.getUrlPrefix(url);
      permissionPageUrls.add(url);
      if (urlStems.indexOf(stem) < 0) {
	urlStems.add(stem);
      }
    } catch (MalformedURLException ex) {
      logger.debug3("addUrlStemToAU(" + url + ") threw " + ex);
    }
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
