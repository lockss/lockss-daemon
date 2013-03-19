/*
 * $Id: MockArchivalUnit.java,v 1.106 2013-03-19 04:26:14 tlipkis Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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
import org.apache.oro.text.regex.*;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.rewriter.*;
import org.lockss.extractor.*;

/**
 * This is a mock version of <code>ArchivalUnit</code> used for testing
 */
public class MockArchivalUnit implements ArchivalUnit {
  private Configuration config = ConfigManager.EMPTY_CONFIGURATION;
  private CrawlSpec spec;
  private String pluginId = "mock";
  private TitleConfig tc = null;
  private String auId = null;
  private String defaultAUId = StringUtil.gensym("MockAU_");
  private CachedUrlSet cus = null;
  private List newContentUrls = null;
  private List<Pattern> excludeUrlFromPollPatterns = null;
  private List<Pattern> nonSubstanceUrlPatterns = null;
  private List<Pattern> substanceUrlPatterns = null;
  private SubstancePredicate substancePred = null;

  private boolean shouldCrawlForNewContent = true;
  private boolean shouldCallTopLevelPoll = true;
  private static Logger log = Logger.getLogger("MockArchivalUnit");
  private List permissionPages;
  private Map<String,String> urlNormalizeMap;

  private HashSet urlsToCache = new HashSet();

  private Plugin plugin = new MockPlugin();
  private String name = "MockAU";
  private Hashtable ucHash = new Hashtable();
  private Hashtable cuHash = new Hashtable();

  private FilterRule filterRule = null;
  private FilterFactory hashFilterFactory = null;
  private FilterFactory crawlFilterFactory = null;
  private LinkRewriterFactory rewriterFactory = null;
  private Iterator<ArticleFiles> articleIterator = null;
  private Map extractors = new HashMap();
  private Map fileMetadataExtractors = new HashMap();
  private Map<String,LinkRewriterFactory> rewriterMap =
    new HashMap<String,LinkRewriterFactory>();
  private TypedEntryMap propertyMap = new TypedEntryMap();
  private List urlStems = Collections.EMPTY_LIST;
  private Collection loginUrls;
  private String fetchRateLimiterKey;
  private RateLimiterInfo rateInfo = new RateLimiterInfo("foo", "unlimited");
  private List<String> cookies = new ArrayList<String>();
  private List<String> requestHeaders = new ArrayList<String>();

  private String perHostPermissionPath;
  private Comparator<CrawlUrl> crawlUrlCmp;

  boolean isBulkContent = false;
  ArchiveFileTypes aft = null;

  private static final Logger logger = Logger.getLogger("MockArchivalUnit");

  public static MockArchivalUnit newInited() {
    MockArchivalUnit mau = new MockArchivalUnit(new MockPlugin(),
						StringUtil.gensym("MockAU"));
    return mau;
  }

  public static MockArchivalUnit newInited(LockssDaemon daemon) {
    MockArchivalUnit mau = new MockArchivalUnit(new MockPlugin(daemon),
						StringUtil.gensym("MockAU"));
    return mau;
  }

  public MockArchivalUnit(){
  }

  public MockArchivalUnit(String auId) {
    this.auId = auId;
  }

  public MockArchivalUnit(Plugin plugin) {
    this.plugin = plugin;
  }

  public MockArchivalUnit(Plugin plugin, String auId) {
    this.plugin = plugin;
    this.auId = auId;
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
    return urlStems;
  }

  public void setUrlStems(List stems) {
    urlStems = stems;
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

  public List<Pattern> makeExcludeUrlsFromPollsPatterns()
      throws ArchivalUnit.ConfigurationException {
    return excludeUrlFromPollPatterns;
  }

  public List<Pattern> makeNonSubstanceUrlPatterns() {
    return nonSubstanceUrlPatterns;
  }

  public List<Pattern> makeSubstanceUrlPatterns() {
    return substanceUrlPatterns;
  }

  public SubstancePredicate makeSubstancePredicate() {
    return substancePred;
  }

  public void setExcludeUrlsFromPollsPatterns(List<Pattern> pats) {
    excludeUrlFromPollPatterns = pats;
  }

  public void setNonSubstanceUrlPatterns(List<Pattern> pats) {
    nonSubstanceUrlPatterns = pats;
  }

  public void setSubstanceUrlPatterns(List<Pattern> pats) {
    substanceUrlPatterns = pats;
  }

  public void setSubstancePredicate(SubstancePredicate pred) {
    substancePred = pred;
  }

  public TitleConfig getTitleConfig() {
    return tc;
  }

  public TdbAu getTdbAu() {
    return tc == null ? null : tc.getTdbAu();
  }

  public void setTitleConfig(TitleConfig tc) {
    this.tc = tc;
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

  public String getPerHostPermissionPath() {
    return perHostPermissionPath;
  }

  public void setPerHostPermissionPath(String path) {
    perHostPermissionPath = path;
  }

  public List<String> getHttpCookies() {
    return cookies;
  }

  public void setHttpCookies(List<String> cookies) {
    this.cookies = cookies;
  }

  public List<String> getHttpRequestHeaders() {
    return requestHeaders;
  }

  public void setHttpRequestHeaders(List<String> requestHeaders) {
    this.requestHeaders = requestHeaders;
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

  /**
   * @deprecated
   */
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

  /* allow setting the  content-type /Mime type to any strin-value while adding URL */
  public MockCachedUrl addUrlContype(String url, boolean exists, 
                                     boolean shouldCache, String contentType) {
    MockCachedUrl cu = addUrl(url,  exists, shouldCache);
    CIProperties props = new CIProperties();
    props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, contentType);
    cu.setProperties(props);
    return cu;
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

  public void removeUrlToBeCached(String url) {
    urlsToCache.remove(url);
  }

  public boolean shouldBeCached(String url) {
    return urlsToCache.contains(url);
  }

  public boolean isLoginPageUrl(String url) {
    if (loginUrls == null) return false;
    return loginUrls.contains(url);
  }

  public void setLoginPageUrls(Collection urls) {
    loginUrls = urls;
  }

  public String siteNormalizeUrl(String url) {
    if (urlNormalizeMap != null) {
      String res = urlNormalizeMap.get(url);
      if (res == null) {
	log.debug("siteNormalizeUrl(" + url + ") unchanged");
	return url;
      } else {
	log.debug("siteNormalizeUrl(" + url + ") = " + res);
	return res;
      }
    }
    return url;
  }

  public void setUrlNormalizeMap(Map map) {
    urlNormalizeMap = map;
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
    if (plugin == null || config == null  || config.isEmpty()) {
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

  public RateLimiter findFetchRateLimiter() {
    return RateLimiter.UNLIMITED;
  }

  public String getFetchRateLimiterKey() {
    return fetchRateLimiterKey;
  }

  public void setFetchRateLimiterKey(String key) {
    fetchRateLimiterKey = key;
  }

  public RateLimiterInfo getRateLimiterInfo() {
    return rateInfo;
  }

  public void setRateLimiterInfo(RateLimiterInfo info) {
    rateInfo = info;
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

  public FilterFactory getHashFilterFactory(String contentType) {
    return hashFilterFactory;
  }

  public void setHashFilterFactory(FilterFactory filterFactory) {
    this.hashFilterFactory = filterFactory;
  }

  public FilterFactory getCrawlFilterFactory(String contentType) {
    return crawlFilterFactory;
  }

  public void setCrawlFilterFactory(FilterFactory filterFactory) {
    this.crawlFilterFactory = filterFactory;
  }

  public LinkRewriterFactory getLinkRewriterFactory(String contentType) {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);
    LinkRewriterFactory res = rewriterMap.get(mimeType);
    if (res == null) {
      res = rewriterMap.get("*");
    }      
    if (res == null) {
      res = rewriterFactory;
    }      
    return res;
  }

  public void setLinkRewriterFactory(LinkRewriterFactory rewriterFactory) {
    this.rewriterFactory = rewriterFactory;
  }

  public void setLinkRewriterFactory(String mimeType,
				     LinkRewriterFactory rewriterFactory) {
    rewriterMap.put(mimeType, rewriterFactory);
  }

  public Iterator<ArticleFiles> getArticleIterator() {
    return articleIterator;
  }

  public Iterator<ArticleFiles> getArticleIterator(MetadataTarget target) {
    return articleIterator;
  }

  public void setArticleIterator(Iterator<ArticleFiles> iter) {
    this.articleIterator = iter;
  }

  public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target,
							String contentType) {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);
    FileMetadataExtractor res = (FileMetadataExtractor)fileMetadataExtractors.get(mimeType);
    if (res == null) {
      res = (FileMetadataExtractor)fileMetadataExtractors.get("*");
    }      
    return res;
  }

  public void setFileMetadataExtractor(String mimeType,
				       FileMetadataExtractor extractor) {
    fileMetadataExtractors.put(mimeType, extractor);
  }


  public LinkExtractor getLinkExtractor(String contentType) {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);
    LinkExtractor res = (LinkExtractor)extractors.get(mimeType);
    if (res == null) {
      res = (LinkExtractor)extractors.get("*");
    }      
    return res;
  }

  public void setLinkExtractor(String mimeType, LinkExtractor extractor) {
    extractors.put(mimeType, extractor);
  }

  public Comparator<CrawlUrl> getCrawlUrlComparator() {
    return crawlUrlCmp;
  }

  public void setCrawlUrlComparator(Comparator<CrawlUrl> cmprtr) {
    crawlUrlCmp = cmprtr;
  }

  public List<String> getAuFeatureUrls(String auFeature) {
    return null;
  }

  public boolean isBulkContent() {
    return isBulkContent;
  }

  public void setBulkContent(boolean val) {
    isBulkContent = val;
  }

  public ArchiveFileTypes getArchiveFileTypes() {
    return aft;
  }

  public void setArchiveFileTypes(ArchiveFileTypes aft) {
    this.aft = aft;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[MockArchivalUnit: auId=");
    sb.append(getAuId());
    sb.append("]");
    return sb.toString();
  }

}

