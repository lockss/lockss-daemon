/*
 * $Id: MockArchivalUnit.java,v 1.27 2003-06-12 23:46:43 tyronen Exp $
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

package org.lockss.test;

import java.util.*;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.plugin.*;

/**
 * This is a mock version of <code>ArchivalUnit</code> used for testing
 */

public class MockArchivalUnit implements ArchivalUnit {
  private Configuration config;
  private CrawlSpec spec;
  private String pluginId = "mock";
  private String auId = null;
  private String defaultAUId = StringUtil.gensym("MockAU_");
  private CachedUrlSet cus = null;
  private MockObjectCallback pauseCallback = null;
  private List newContentUrls = null;
  private boolean shouldCrawlForNewContent = true;
  private boolean shouldCallTopLevelPoll = true;
  private static Logger log = Logger.getLogger("MockArchivalUnit");

  private HashSet urlsToCache = new HashSet();

  private Plugin plugin;

  public MockArchivalUnit(){
  }

  public MockArchivalUnit(CrawlSpec spec) {
    this.spec = spec;
  }

  public CrawlSpec getCrawlSpec() {
    return spec;
  }

  public void setCrawlSpec(CrawlSpec spec) {
    this.spec = spec;
  }

  public CachedUrlSet getAUCachedUrlSet() {
    if (cus != null) {
      // if someone has set the aucus, return it
      return cus;
    } else {
      // else make one
      return makeCachedUrlSet(new AUCachedUrlSetSpec());
    }
  }

  public void setAUCachedUrlSet(CachedUrlSet cus) {
    this.cus = cus;
  }

  public List getNewContentCrawlUrls() {
    return newContentUrls;
  }

  public void setNewContentCrawlUrls(List urls) {
    newContentUrls = urls;
  }

  public void setConfiguration(Configuration config)
      throws ArchivalUnit.ConfigurationException {
    this.config = config;
  }

  public Configuration getConfiguration() {
    return config;
  }

  /**
   * Make a new MockArchivalUnit object with a list populated with
   * the urls specified in rootUrls (and no reg expressions)
   *
   * @param rootUrls list of string representation of the urls to
   * add to the new MockArchivalUnit's list
   * @return MockArchivalUnit with urls in rootUrls in its list
   */
  public static MockArchivalUnit createFromListOfRootUrls(String[] rootUrls){
    CrawlSpec rootSpec = new CrawlSpec(ListUtil.fromArray(rootUrls), null);
    return new MockArchivalUnit(rootSpec);
  }

  // Methods used by the crawler

  public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
    return new MockCachedUrlSet(this, cuss);
  }

  protected void addUrlToBeCached(String url) {
    urlsToCache.add(url);
  }

  public boolean shouldBeCached(String url) {
    return urlsToCache.contains(url);
  }

  public String getPluginId() {
    if (plugin != null) {
      return plugin.getPluginId();
    }
    return pluginId;
  }


  public void setPlugin(Plugin plugin) {
    this.plugin = plugin;
  }

  //XXX remove me
  public final String getAUId() {
    if (auId != null) {
      return auId;
    }
    if (plugin == null) {
      return defaultAUId;
    }
    Collection defKeys = plugin.getDefiningConfigKeys();
    Properties props = new Properties();
    for (Iterator it = defKeys.iterator(); it.hasNext();) {
      String curKey = (String)it.next();
      props.setProperty(curKey, config.get(curKey));
    }
    return PluginManager.generateAUId(getPluginId(), props);
  }

  public String getName() {
    return "MockAU";
  }

  public void setPluginId(String newId) {
    pluginId = newId;
  }

  public void setAuId(String newId) {
    //    log.warning("*********************************"+newId, new RuntimeException("blah"));
    auId = newId;
  }

  public void pauseBeforeFetch() {
    if (pauseCallback != null) {
      pauseCallback.callback();
    }
  }

  public long getFetchDelay() {
    return 0;
  }

  public void setPauseCallback(MockObjectCallback callback) {
    this.pauseCallback = callback;
  }

  public boolean shouldCrawlForNewContent(AuState aus) {
    return shouldCrawlForNewContent;
  }

  public void setShouldCrawlForNewContent(boolean val) {
    shouldCrawlForNewContent = val;
  }

  public boolean shouldCallTopLevelPoll(AuState aus) {
    return shouldCallTopLevelPoll;
  }

  public void setShouldCallTopLevelPoll(boolean val) {
    shouldCallTopLevelPoll = val;
  }

  public boolean isDamaged(String url) {
    return false;
  }

}

