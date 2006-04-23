/*
 * $Id: MockArchivalUnit.java,v 1.66 2006-04-23 05:52:04 tlipkis Exp $
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

package org.lockss.test;

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.config.Configuration;
import org.lockss.crawler.*;
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
  private List permissionPages;
  private String urlNormalizeString = null;

  private HashSet urlsToCache = new HashSet();

  private Plugin plugin;
  private String name = "MockAU";
  private Hashtable ucHash = new Hashtable();
  private Hashtable cuHash = new Hashtable();

  private FilterRule filterRule = null;
  private ContentParser parser = null;
  private TypedEntryMap propertyMap = new TypedEntryMap();
  private static final Logger logger = Logger.getLogger("MockArchivalUnit");
  public MockArchivalUnit(){
  }

  public MockArchivalUnit(Plugin plugin) {
    this.plugin = plugin;
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

  public Collection getUrlStems() {
    return null;
  }

  public CachedUrlSet getAuCachedUrlSet() {
    if (cus != null) {
      // if someone has set the aucus, return it
      return cus;
    } else {
      // else make one
      cus = makeCachedUrlSet(new AuCachedUrlSetSpec());
      return cus;
    }
  }

  public void setAuCachedUrlSet(CachedUrlSet cus) {
    this.cus = cus;
  }

  public List getNewContentCrawlUrls() {
    return newContentUrls;
  }

  public TitleConfig getTitleConfig() {
    throw new UnsupportedOperationException("Not implemented");
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

  public void setPropertyMap(TypedEntryMap map) {
    propertyMap = map;
  }

  public TypedEntryMap getProperties() {
    return propertyMap;
  }

  public CachedUrlSet makeCachedUrlSet( CachedUrlSetSpec spec) {
    return new MockCachedUrlSet(this, spec);
  }

  public CachedUrl makeCachedUrl(String url) {
    CachedUrl cu = null;
    if (cuHash != null) {
      cu = (CachedUrl)cuHash.get(url);
      logger.debug(cu+" came from cuHash for "+url);
    } else {
      logger.debug("cuHash is null, so makeCachedUrl is returning null");
    }
    return cu;
  }

 public UrlCacher makeUrlCacher(String url) {
    UrlCacher uc = null;
    if (ucHash != null) {
      uc = (UrlCacher)ucHash.get(url);
      // MockUrlCacher checks that getUncachedInputStream() isn't called
      // more than once.  But we return the same UrlCacher multiple times
      // here, so make it ok to call getUncachedInputStream() again.  The
      // semantics of makeUrlCacher() is that it makes a new one each
      // time.)
      ((MockUrlCacher)uc).setNotExecuted();
      logger.debug(uc+" came from ucHash");
    } else {
      logger.debug("ucHash is null, so makeUrlCacher is returning null");
    }
    return uc;
  }

  public void addContent(String url, String content) {
    MockCachedUrl cu = (MockCachedUrl)makeCachedUrl(url);
    if (cu != null) {
      cu.setContent(content);
    }
  }


  protected MockUrlCacher makeMockUrlCacher(String url) {
    return new MockUrlCacher(url, this);
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
    CrawlSpec rootSpec = new SpiderCrawlSpec(ListUtil.fromArray(rootUrls), null);
    return new MockArchivalUnit(rootSpec);
  }

  /**
   * Sets up a cached url and url cacher for this url
   * @param url url for which we should set up a CachedUrl and UrlCacher
   * @param exists whether this url should act like it's already in the cache
   * @param shouldCache whether this url should say to cache it or not
   * @param props CIProperties to be associated with this url
   */
  public MockCachedUrl addUrl(String url, boolean exists,
			      boolean shouldCache, CIProperties props) {
    return addUrl(url, exists, shouldCache, props, null, 0);
  }
  /**
   * To be used when you want to set up a url that will throw an exception
   * @param url the url
   * @param cacheException the IOException to throw
   * @param timesToThrow number of times to throw the exception
   */
  public MockCachedUrl addUrl(String url,
			      Exception cacheException, int timesToThrow) {
    return addUrl(url, false, true, new CIProperties(),
		  cacheException, timesToThrow);
  }

  /**
   * Same as above, but with exists defaulting to false, shouldCache to true
   * and props to "content-type=text/html"
   * @param url the url
   */
  public MockCachedUrl addUrl(String url) {
    return addUrl(url, false, true);
  }

  /**
   * Set up a CachedUrl with content
   * @param url the url
   * @param content the content
   */
  public MockCachedUrl addUrl(String url, String content) {
    MockCachedUrl cu = addUrl(url, true, true);
    cu.setContent(content);
    return cu;
  }

  public MockCachedUrl addUrl(String url,
			      boolean exists, boolean shouldCache) {
    CIProperties props = new CIProperties();
    props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    return addUrl(url, exists, shouldCache, props);
  }


  private MockCachedUrl addUrl(String url,
			       boolean exists, boolean shouldCache,
			       CIProperties props,
			       Exception cacheException, int timesToThrow) {
    MockCachedUrl cu = new MockCachedUrl(url, this);
    cu.setProperties(props);
    cu.setExists(exists);

    MockUrlCacher uc = makeMockUrlCacher(url);
    uc.setShouldBeCached(shouldCache);
    if (shouldCache) {
      addUrlToBeCached(url);
    }
    uc.setCachedUrl(cu);
    if (cacheException != null) {
      if (cacheException instanceof IOException) {
        uc.setCachingException((IOException)cacheException, timesToThrow);
      } else if (cacheException instanceof RuntimeException) {
        uc.setCachingException((RuntimeException)cacheException, timesToThrow);
      }
    }
    logger.debug2(this + "Adding "+url+" to cuHash and ucHash");

    cuHash.put(url, cu);
    ucHash.put(url, uc);
    return cu;
  }

  public void addUrlToBeCached(String url) {
    urlsToCache.add(url);
  }

  public boolean shouldBeCached(String url) {
    return urlsToCache.contains(url);
  }

  public String siteNormalizeUrl(String url) {
    log.debug("siteNormalizeUrl(), urlNormalizeString = " + urlNormalizeString);
    if (urlNormalizeString == null) {
      return url;
    } else {
      String res = StringUtil.replaceString(url, urlNormalizeString, "");
      log.debug("siteNormalizeUrl(" + url + ") = " + res);
      return res;
    }
  }

  public void setUrlNormalizeString(String removeString) {
    log.debug("setUrlNormalizeString(" + removeString + ")");
    urlNormalizeString = removeString;
  }

  public Plugin getPlugin() {
    return this.plugin;
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
  public final String getAuId() {
    if (auId != null) {
      return auId;
    }
    if (plugin == null || config == null) {
      return defaultAUId;
    }
    Properties props = new Properties();
    for (Iterator iter = plugin.getAuConfigDescrs().iterator();
	 iter.hasNext();) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      if (descr.isDefinitional()) {
	String key = descr.getKey();
	String val = config.get(key);
	// during testing, don't allow missing config values to cause
	// NullPointerException in setProperty()
	if (val != null) {
	  props.setProperty(key, config.get(key));
	}
      }
    }
    return PluginManager.generateAuId(getPluginId(), props);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPluginId(String newId) {
    pluginId = newId;
  }

  public void setAuId(String newId) {
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

  public FilterRule getFilterRule(String mimeType) {
    return filterRule;
  }

  public void setFilterRule(FilterRule filterRule) {
    this.filterRule = filterRule;
  }

  public ContentParser getContentParser(String mimeType) {
    return parser;
  }

  public void setParser(ContentParser parser) {
    this.parser = parser;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[MockArchivalUnit: auId=");
    sb.append(auId);
    sb.append("]");
    return sb.toString();
  }

}

