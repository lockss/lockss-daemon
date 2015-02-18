/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ExploderHelper;
import org.lockss.rewriter.*;
import org.lockss.plugin.wrapper.*;
import org.lockss.crawler.*;
import org.lockss.extractor.*;

/**
 * Abstract base class for Plugins.  Plugins are encouraged to extend this
 * class to get some common Plugin functionality.
 */
public abstract class BasePlugin
    implements Plugin {
  static Logger log = Logger.getLogger("BasePlugin");

  // Below org.lockss.title.xxx.
  static final String TITLE_PARAM_TITLE = "title";
  static final String TITLE_PARAM_JOURNAL = "journalTitle";
  public static final String TITLE_PARAM_PLUGIN = "plugin";
  static final String TITLE_PARAM_PLUGIN_VERSION = "pluginVersion";
  static final String TITLE_PARAM_EST_SIZE = "estSize";
  static final String TITLE_PARAM_ATTRIBUTES = "attributes";
  public static final String TITLE_PARAM_PARAM = "param";
  // Below org.lockss.title.xxx.param.n.
  public static final String TITLE_PARAM_PARAM_KEY = "key";
  public static final String TITLE_PARAM_PARAM_VALUE = "value";
  static final String TITLE_PARAM_PARAM_EDITABLE = "editable";

  protected LockssDaemon theDaemon;
  protected PluginManager pluginMgr;
  protected Collection<ArchivalUnit> aus = new ArrayList<ArchivalUnit>();
  // Title -> TitleConfig
  protected Map<String, TitleConfig> titleConfigMap;
  // auid -> TitleConfig
  protected Map<String, TitleConfig> auidTitleConfigMap;
  // XXX need to generalize this
  protected CacheResultMap resultMap;
  protected MimeTypeMap mimeMap;
  protected HashMap<String,FilterRule> filterMap = new HashMap<String,FilterRule>(4);
  // ClassLoader used to load plugin.  null if not loadable plugin
  protected ClassLoader classLoader;
  protected List<ConfigParamDescr> allParamDescrs;
  protected Map<String,ConfigParamDescr> paramDescrMap;
  protected boolean stopped = false;


  Configuration.Callback configCb = new Configuration.Callback() {
      public void configurationChanged(Configuration newConfig,
				       Configuration prevConfig,
				       Configuration.Differences diffs) {
	setConfig(newConfig, prevConfig, diffs);
      }
      public String toString() {
	return getPluginId();
      }
    };

  /**
   * Must invoke this constructor in plugin subclass.
   */
  protected BasePlugin() {
  }

  public void initPlugin(LockssDaemon daemon) {
    theDaemon = daemon;
    pluginMgr = theDaemon.getPluginManager();
    mimeMap = new MimeTypeMap(MimeTypeMap.DEFAULT);

    theDaemon.getConfigManager().registerConfigurationCallback(configCb);
    initResultMap();
  }

  public void stopPlugin() {
    stopped = true;
    // Don't actually perform stop actions unless/until no AUs
    if (!aus.isEmpty()) {
      return;
    }
    theDaemon.getConfigManager().unregisterConfigurationCallback(configCb);
    if (classLoader instanceof LoadablePluginClassLoader) {
      LoadablePluginClassLoader lploader =
	(LoadablePluginClassLoader)classLoader;
      if (lploader != null) {
	lploader.close();
      }
    }
    classLoader = null;
  }

  public void stopAu(ArchivalUnit au) {
    // Is there any reason to notify the AU itself?
    synchronized (aus) {
      aus.remove(au);
    }
    // If that was the last AU and we're supposed to be stopped, perform
    // stop actions now
    if (stopped && aus.isEmpty()) {
      stopPlugin();
    }
  }

  /** Subclasses should override this if they want to require a minimum
   * daemon version in order to run
   */
  public String getRequiredDaemonVersion() {
    return "0.0.0";
  }

  public String getFeatureVersion(Plugin.Feature feat) {
    return null;
  }

  /** Subclasses should override this if they want to supply a publishing
   * platform name
   */
  public String getPublishingPlatform() {
    return null;
  }
  
  /**
   * Should we store the probe permission page
   * @return true
   */
  public boolean storeProbePermission() {
    return true;
  }

  /**
   * Default implementation collects keys from titleConfigMap.
   * @return a List
   */
  public List<String> getSupportedTitles() {
    if (titleConfigMap == null) {
      return Collections.<String>emptyList();
    }
    return new ArrayList<String>(titleConfigMap.keySet());
  }

  /**
   * Default implementation looks in titleConfigMap.
   */
  public TitleConfig getTitleConfig(String title) {
    if (titleConfigMap == null) {
      return null;
    }
    return (TitleConfig)titleConfigMap.get(title);
  }

  /**
   * Default implementation looks in titleConfigMap.
   */
  public TitleConfig getTitleConfigFromAuId(String auid) {
    if (auidTitleConfigMap == null) {
      return null;
    }
    return auidTitleConfigMap.get(auid);
  }

  /** Set up our titleConfigMap from the title definitions in the
   * Configuration.  Each title config looks like:<pre>
   * org.lockss.title.uid.title=Sample Title
   * org.lockss.title.uid.plugin=org.lockss.plugin.sample.SamplePlugin
   * org.lockss.title.uid.param.1.key=base_url
   * org.lockss.title.uid.param.1.value=http\://sample.org/
   * org.lockss.title.uid.param.2.key=year
   * org.lockss.title.uid.param.2.value=2003
   * org.lockss.title.uid.param.2.editable=true</pre> where
   * <code>uid</code> is an identifier that is unique for each title.
   * Parameters for which <code>editable</code> is true (<i>eg</i>,
   * <code>year</code>) may be edited by the user to select a related AU.
   * <br>See TitleParams (and test/scripts/title-params) for an easy way to
   * create these property files.
   */
  protected void setConfig(Configuration newConfig,
			   Configuration prevConfig,
			   Configuration.Differences diffs) {
    // PJG: should we get this from the changedKeys?
    if (diffs.containsTdbPluginId(getPluginId())) {
      setTitleConfigs(newConfig.getTdb());
    }
  }

  /**
   * Set TitleConfigs from the title database
   * @param tdb the Tdb used to set TitleConfigs.
   */
  protected void setTitleConfigs(Tdb tdb) {
    String myId = getPluginId();
    Map<String, TitleConfig> titleMap = new HashMap<String,TitleConfig>();
    Map<String, TitleConfig> auidMap = new HashMap<String,TitleConfig>();
    for (TdbAu.Id tdbAuId : tdb.getTdbAuIds(myId)) {
      TdbAu tdbAu = tdbAuId.getTdbAu();
      // Prevent tdb errors from aborting this process
      try {
	String pluginId = tdbAu.getPluginId();
	if (myId.equals(pluginId)) {
	  if (log.isDebug2()) {
	    log.debug2("my titleConfig: " + tdbAu);
	  }
	  String title = tdbAu.getName();
	  TitleConfig tc = new TitleConfig(tdbAu, this);
	  TitleConfig oldTc = titleMap.get(title);
	  if (oldTc != null && !tc.equals(oldTc)) {
	    log.warning("Duplicate title: " + tc);
	    log.warning("Previous def   : " + oldTc);
	  }
	  // Compute auid before updating any maps, as it might throw
	  String auid = tc.getAuId(pluginMgr, this);
	  auidMap.put(auid, tc);
	  titleMap.put(title, tc);
	} else {
	  if (log.isDebug3()) {
	    log.debug3("titleConfig: " + tdbAu.getName());
	  }
	}
      } catch (RuntimeException e) {
	log.warning("Error processing Tdb entry: " + tdbAu.getName(), e);
      }
    }
    //TODO: decide on how to support plug-ins which do not use the title registry
    if (!titleMap.isEmpty()) {
      setTitleConfigMap(titleMap, auidMap);
      notifyAusTitleDbChanged();
    }
  }

  protected void setTitleConfigMap(Map<String, TitleConfig> titleMap,
				   Map<String, TitleConfig> auidMap) {
    this.titleConfigMap = titleMap;
    this.auidTitleConfigMap = auidMap;
    pluginMgr.resetTitles();
  }

  protected void notifyAusTitleDbChanged() {
    for (Iterator<ArchivalUnit> iter = getAllAus().iterator(); iter.hasNext(); ) {
      //  They should all be BaseArchivalUnits, but just in case...
      try {
	BaseArchivalUnit au = (BaseArchivalUnit)iter.next();
	au.titleDbChanged();
      } catch (ClassCastException e) {
	log.warning("notifyAusTitleDbChanged: " + this, e);
      }
    }
  }

  public List<ConfigParamDescr> getAuConfigDescrs() {
    if (allParamDescrs == null) {
      List<ConfigParamDescr> local = getLocalAuConfigDescrs();
      ArrayList<ConfigParamDescr> res = new ArrayList<ConfigParamDescr>(local);
      for (ConfigParamDescr descr : local) {
	switch (descr.getTypeEnum()) {
	case Year:
	  res.add(descr.getDerivedDescr(BaseArchivalUnit.PREFIX_AU_SHORT_YEAR
					+ descr.getKey()));
	  break;
	case Url:
	  ConfigParamDescr derived;
	  derived = descr.getDerivedDescr(descr.getKey()
					  + BaseArchivalUnit.SUFFIX_AU_HOST);
	  derived.setType(AuParamType.String);
	  res.add(derived);
	  derived = descr.getDerivedDescr(descr.getKey()
					  + BaseArchivalUnit.SUFFIX_AU_PATH);
	  derived.setType(AuParamType.String);
	  res.add(derived);
	  break;
	}
      }

      if (!res.isEmpty()) {
	// Don't add internal params if plugin has no params.  (testing)
	res.add(ConfigParamDescr.AU_CLOSED);
	res.add(ConfigParamDescr.PUB_DOWN);
	res.add(ConfigParamDescr.PUB_NEVER);
	res.add(ConfigParamDescr.PROTOCOL_VERSION);
 	res.add(ConfigParamDescr.CRAWL_PROXY);
 	res.add(ConfigParamDescr.CRAWL_INTERVAL);
      }
      res.trimToSize();
      Map<String,ConfigParamDescr> map = new HashMap<String,ConfigParamDescr>();
      for (ConfigParamDescr descr : res) {
	map.put(descr.getKey(), descr);
      }
      paramDescrMap = map;
      allParamDescrs = res;
    }
    return allParamDescrs;
  }

  abstract protected List<ConfigParamDescr> getLocalAuConfigDescrs();

  /**
   * Find the ConfigParamDescr that this plugin uses for the specified key.
   * @return the element of {@link #getAuConfigDescrs()} whose key
   * matches <code>key</code>, or null if none.
   */
  public ConfigParamDescr findAuConfigDescr(String key) {
    getAuConfigDescrs();
    return paramDescrMap.get(key);
  }


  // for now use the plugin's class name
  // tk - this will have to change to account for versioning
  public String getPluginId() {
    return this.getClass().getName();
  }

  public Collection<ArchivalUnit> getAllAus() {
    if (log.isDebug2()) log.debug2("getAllAus: aus: " + aus);
    synchronized (aus) {
      return new ArrayList<ArchivalUnit>(aus);
    }
  }

  public ArchivalUnit configureAu(Configuration config, ArchivalUnit au) throws
      ArchivalUnit.ConfigurationException {
    if(config == null) {
      throw new  ArchivalUnit.ConfigurationException("Null Configuration");
    }
    if (au != null) {
      au.setConfiguration(config);
    }
    else {
      au = createAu(config);
    }
    return au;
  }

  /** Create an AU and add it to our list.  Subclasses should implement
   * {@link #createAu0(Configuration)} to create the actual AU */
  public final ArchivalUnit createAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    ArchivalUnit au = createAu0(auConfig);
    synchronized (aus) {
      aus.add(au);
    }
    return au;
  }
    
  /** Create an AU.  Subclasses should implement this */
  protected abstract ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException;

  /**
   * Return the LockssDaemon instance
   * @return the LockssDaemon instance
   */
  public LockssDaemon getDaemon() {
    return theDaemon;
  }

  /**
   * return the CacheResultMap to use with this plugin
   * @return CacheResultMap
   */
  public CacheResultMap getCacheResultMap() {
    return resultMap;
  }

  protected void initResultMap() {
    resultMap = new HttpResultMap();
  }

  String siteNormalizeUrl(String url, ArchivalUnit au) {
    try {
      return getUrlNormalizer().normalizeUrl(url, au);
    } catch (PluginException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return a {@link LinkExtractor} that knows how to extract URLs from
   * content of the given MIME type
   * @param contentType content type to get a content parser for
   * @return A LinkExtractor or null
   */
  public LinkExtractor getLinkExtractor(String contentType)
      throws PluginException.InvalidDefinition {
    MimeTypeInfo mti = getMimeTypeInfo(contentType);
    LinkExtractorFactory fact = mti.getLinkExtractorFactory();
    if (fact != null) {
      try {
	LinkExtractor extractor = fact.createLinkExtractor(contentType);
	// Wrap the result iff it came from a different Classloader,
	// indicating it's part of a loadable plugin.
	if (extractor.getClass().getClassLoader() !=
	    this.getClass().getClassLoader()) {
	  extractor = WrapperUtil.wrap(extractor, LinkExtractor.class);
	}
	return extractor;
      } catch (PluginException e) {
	throw new RuntimeException(e);
      }
    }
    return null;
  }

  protected MimeTypeInfo getMimeTypeInfo(String contentType) {
    if (contentType == null) {
      log.debug3("getMimeTypeInfo: null content type");
      return MimeTypeInfo.NULL_INFO;
    }
    MimeTypeInfo mti = mimeMap.getMimeTypeInfo(contentType);
    if (mti == null) {
      return MimeTypeInfo.NULL_INFO;
    }
    return mti;
  }

  protected UrlNormalizer getUrlNormalizer() {
    return NullUrlNormalizer.INSTANCE;
  }

  public static class NullUrlNormalizer implements UrlNormalizer {
    public static final UrlNormalizer INSTANCE = new NullUrlNormalizer();

    public String normalizeUrl (String url, ArchivalUnit au) {
      return url;
    }
  }

  protected ExploderHelper getExploderHelper() {
    return null;
  }

  public boolean isBulkContent() {
    return false;
  }

  public ArchiveFileTypes getArchiveFileTypes() {
    return null;
  }

  public AuParamFunctor getAuParamFunctor() {
    return BaseAuParamFunctor.SINGLETON;
  }

  protected Comparator<CrawlUrl> getCrawlUrlComparator(ArchivalUnit au)
      throws PluginException.LinkageError {
    return null;
  }

  /**
   * Returns a filter rule from the cache if found, otherwise calls
   * 'constructFilterRule()' and caches the result if non-null.  Content-type
   * is converted to lowercase.  If contenttype is null, returns null.
   * @param contentType the content type
   * @return the FilterRule
   */
  public FilterRule getFilterRule(String contentType) {
    if (contentType != null) {
      Object obj = filterMap.get(contentType);
      FilterRule rule = null;
      if (obj==null) {
        rule = constructFilterRule(contentType);
        if (rule != null) {
	  if (log.isDebug3()) log.debug3(contentType + " filter: " + rule);
          filterMap.put(contentType, rule);
        } else {
	  if (log.isDebug3()) log.debug3("No filter for "+contentType);
	}
      } else if (obj instanceof FilterRule) {
	rule = (FilterRule)obj;
      }
      return rule;
    }
    log.debug3("getFilterRule: null content type");
    return null;
  }

  /**
   * Override to provide proper filter rules.
   * @param contentType content type
   * @return null, since we don't filter by default
   */
  protected FilterRule constructFilterRule(String contentType) {
    log.debug3("constructFilterRule default: null");
    return null;
  }

  /**
   * Returns the hash filter factory for the mime type, if any
   * @param contentType the content type
   * @return the FilterFactory
   */
  public FilterFactory getHashFilterFactory(String contentType) {
    MimeTypeInfo mti = getMimeTypeInfo(contentType);
    if (mti == null) {
      return null;
    }
    if (log.isDebug3())
      log.debug3(contentType + " filter: " + mti.getHashFilterFactory());
    return mti.getHashFilterFactory();
  }

  /**
   * Returns the crawl filter factory for the mime type, if any
   * @param contentType the content type
   * @return the FilterFactory
   */
  public FilterFactory getCrawlFilterFactory(String contentType) {
    MimeTypeInfo mti = getMimeTypeInfo(contentType);
    if (mti == null) {
      return null;
    }
    if (log.isDebug3())
      log.debug3(contentType + " crawl filter: " + mti.getCrawlFilterFactory());
    return mti.getCrawlFilterFactory();
  }

  /**
   * Returns the link rewriter factory for the mime type, if any
   * @param contentType the content type
   * @return the LinkRewriterFactory
   */
  public LinkRewriterFactory getLinkRewriterFactory(String contentType) {
    MimeTypeInfo mti = getMimeTypeInfo(contentType);
    if (mti == null) {
      return null;
    }
    if (log.isDebug3())
      log.debug3(contentType + " rewriter: " + mti.getLinkRewriterFactory());
    return mti.getLinkRewriterFactory();
  }

  /**
   * Returns the plugin's article iterator factory, if any
   * @return the ArticleIteratorFactory
   */
  public ArticleIteratorFactory getArticleIteratorFactory() {
    return null;
  }

  /**
   * Returns the article iterator factory for the content type, if any
   * @param contentType the content type
   * @return the ArticleIteratorFactory
   */
  public ArticleMetadataExtractorFactory
    getArticleMetadataExtractorFactory(MetadataTarget target) {
    return null;
  }

  /**
   * Return a {@link ArticleMetadataExtractor} that knows how to extract
   * metadata from ArticleFiles generated by this plugin's ArticleIterator
   * @param contentType content type to get a metadata extractor for
   * @param au the AU in question
   * @return An ArticleMetadataExtractor or null
   */
    public ArticleMetadataExtractor
      getArticleMetadataExtractor(MetadataTarget target, ArchivalUnit au) {
    ArticleMetadataExtractorFactory fact =
      getArticleMetadataExtractorFactory(target);
    if (fact != null) {
      try {
	return WrapperUtil.wrap(fact.createArticleMetadataExtractor(target),
				ArticleMetadataExtractor.class);
      } catch (PluginException e) {
	throw new RuntimeException(e);
      }
    }
    return null;
  }

  /**
   * Return a {@link FileMetadataExtractor} that knows how to extract
   * metadata from content of the given content type
   * @param target the purpose for which metadata is being extracted
   * @param contentType content type to get a metadata extractor for
   * @param au the AU in question
   * @return A FileMetadataExtractor or null
   */
  public FileMetadataExtractor
    getFileMetadataExtractor(MetadataTarget target,
			     String contentType,
			     ArchivalUnit au) {
    if (contentType == null) {
      return null;
    }
    MimeTypeInfo mti = getMimeTypeInfo(contentType);
    if (mti == null) {
      if (log.isDebug3())
	log.debug3("No mimeTypeInfo for " +
		   (contentType== null ? "null" : contentType));
      return null;
    }
    FileMetadataExtractorFactory fact = mti.getFileMetadataExtractorFactory();
    if (fact != null) {
      try {
	return WrapperUtil.wrap(fact.createFileMetadataExtractor(target,
								 contentType),
				FileMetadataExtractor.class);
      } catch (PluginException e) {
	throw new RuntimeException(e);
      }
    }
    return null;
  }

  /** Returns null - plugins do not have a default article mime type unless
   * they explicitly declare one */
  public String getDefaultArticleMimeType() {
    return null;
  }

  // ---------------------------------------------------------------------
  //   CLASS LOADING SUPPORT ROUTINES
  // ---------------------------------------------------------------------

  /** Retuen the ClassLoader that was used to load the plugin, or null if
   * not a loadable plugin or loaded from system classpath */
  ClassLoader getClassLoader() {
    return classLoader;
  }

  PluginException.InvalidDefinition auxErr(String msg, Throwable t) {
    log.error(msg, t);
    return new PluginException.InvalidDefinition(msg, t);
  }

  /** Create and return a new instance of a plugin auxilliary class.
   * @param className the name of the auxilliary class
   * @param expectedType Type (class or interface) of expected rexult
   */
  public <T> T newAuxClass(String className, Class<T> expectedType) {
    return newAuxClass(className, expectedType, expectedType);
  }

  /** Create and return a new instance of a plugin auxilliary class.
   * @param className the name of the auxilliary class
   * @param expectedType Type (class or interface) of expected rexult
   * @param wrapperType class to lookup wrapper, or null to not wrap the
   * result
   */
  public <T> T newAuxClass(String className, Class<T> expectedType,
			   Class<T> wrapperType) {
    T obj = null;
    try {
      if (classLoader != null) {
	obj = ((Class<T>)Class.forName(className, true, classLoader)).newInstance();
      } else {
	obj = ((Class<T>)Class.forName(className)).newInstance();
      }
    } catch (ExceptionInInitializerError e) {
      throw auxErr("Initializer error in dynamically loaded class "
		   + className,
		   e);
    } catch (LinkageError e) {
      throw auxErr("Linkage error in dynamically loaded class " + className,
		   e);
    } catch (ClassNotFoundException e) {
      throw auxErr("Dynamically loadable class not found " + className,
		   e);
    } catch (IllegalAccessException e) {
      throw auxErr("Class " + className
		   + " (or its no-argument constructor) is not public",
		   e);
    } catch (InstantiationException e) {
      throw auxErr("Error instantiating dynamically loaded class " + className,
		   e);
    } catch (ClassCastException e) {
      // can't happen
      throw auxErr("Class " + className + " is not of type "
		   + expectedType.getName(),
		   e);
    } catch (Exception e) {
      throw auxErr("Error loading class " + className, e);
    }
    if (!expectedType.isInstance(obj)) {
      throw auxErr(className + " is not a " + expectedType.getName(), null);
    }
    if (wrapperType != null) {
      return WrapperUtil.wrap(obj, wrapperType);
    } else {
      return obj;
    }
  }


}
