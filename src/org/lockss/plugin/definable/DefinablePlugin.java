/*
 * $Id: DefinablePlugin.java,v 1.37 2009-04-07 19:09:01 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.definable;

import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.rewriter.*;
import org.lockss.config.Configuration;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.plugin.definable.DefinableArchivalUnit.ConfigurableCrawlWindow;

import java.util.*;
import java.io.FileNotFoundException;
import java.net.*;

/**
 * <p>DefinablePlugin: a plugin which uses the data stored in an
*  ExternalizableMap to configure it self.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class DefinablePlugin extends BasePlugin {
  // configuration map keys
  public static final String KEY_PLUGIN_NAME = "plugin_name";
  public static final String KEY_PLUGIN_VERSION = "plugin_version";
  public static final String KEY_REQUIRED_DAEMON_VERSION =
    "required_daemon_version";
  public static final String KEY_PUBLISHING_PLATFORM =
    "plugin_publishing_platform";
  public static final String KEY_PLUGIN_CONFIG_PROPS = "plugin_config_props";
  public static final String KEY_EXCEPTION_HANDLER =
      "plugin_cache_result_handler";
  public static final String KEY_EXCEPTION_LIST =
      "plugin_cache_result_list";
  public static final String KEY_PLUGIN_NOTES = "plugin_notes";
  public static final String KEY_CRAWL_TYPE =
      "plugin_crawl_type";
  public static final String KEY_FOLLOW_LINKS = "plugin_follow_link";
  /** Message to be displayed when user configures an AU with this plugin */
  public static final String KEY_PLUGIN_AU_CONFIG_USER_MSG =
    "plugin_au_config_user_msg";
  public static final String KEY_PER_HOST_PERMISSION_PATH =
    "plugin_per_host_permission_path";
  public static final String KEY_PLUGIN_PARENT = "plugin_parent";
  public static final String KEY_PLUGIN_PARENT_VERSION = "plugin_parent_version";

  public static final String DEFAULT_PLUGIN_VERSION = "1";
  public static final String DEFAULT_REQUIRED_DAEMON_VERSION = "0.0.0";
  public static final String MAP_SUFFIX = ".xml";

  public static final String CRAWL_TYPE_HTML_LINKS = "HTML Links";
  public static final String CRAWL_TYPE_OAI = "OAI";
  public static final String[] CRAWL_TYPES = {
      CRAWL_TYPE_HTML_LINKS,
      CRAWL_TYPE_OAI,
  };
  public static final String DEFAULT_CRAWL_TYPE = CRAWL_TYPE_HTML_LINKS;
  
  protected String mapName = null;

  static Logger log = Logger.getLogger("DefinablePlugin");

  protected ExternalizableMap definitionMap = new ExternalizableMap();
  protected CacheResultHandler resultHandler = null;
  protected List<String> loadedFromUrls;
  protected CrawlWindow crawlWindow;

  public void initPlugin(LockssDaemon daemon, String extMapName)
      throws FileNotFoundException {
    initPlugin(daemon, extMapName, this.getClass().getClassLoader());
  }

  // Used by tests
  void initPlugin(LockssDaemon daemon, ExternalizableMap defMap) {
    initPlugin(daemon, defMap, this.getClass().getClassLoader());
  }

  public void initPlugin(LockssDaemon daemon, String extMapName,
			 ClassLoader loader)
      throws FileNotFoundException {
    // convert the plugin class name to an xml file name
    // load the configuration map from jar file
    ExternalizableMap defMap = loadMap(extMapName, loader);
    this.initPlugin(daemon, extMapName, defMap, loader);
  }

  private ExternalizableMap loadMap(String extMapName, ClassLoader loader)
      throws FileNotFoundException {
    String first = null;
    String next = extMapName;
    List<String> urls = new ArrayList<String>();
    ExternalizableMap res = null;
    while (next != null) {
      // convert the plugin class name to an xml file name
      String mapFile = next.replace('.', '/') + MAP_SUFFIX;
      URL url = loader.getResource(mapFile);
      if (url != null && urls.contains(url.toString())) {
	throw new PluginException.InvalidDefinition("Plugin inheritance loop: "
						    + next);
      }
      // load into map
      ExternalizableMap oneMap = new ExternalizableMap();
      oneMap.loadMapFromResource(mapFile, loader);
      urls.add(url.toString());
      if (res == null) {
	res = oneMap;
      } else {
	for (Map.Entry ent : oneMap.entrySet()) {
	  String key = (String)ent.getKey();
	  Object val = ent.getValue();
	  if (!res.containsKey(key)) {
	    res.setMapElement(key, val);
	  }
	}
      }
      if (oneMap.containsKey(KEY_PLUGIN_PARENT)) {
	next = oneMap.getString(KEY_PLUGIN_PARENT);
      } else {
	next = null;
      }
    }
    loadedFromUrls = urls;
    return res;
  }

  // Used by tests
  void initPlugin(LockssDaemon daemon,
			 ExternalizableMap defMap,
			 ClassLoader loader) {
    initPlugin(daemon, "Internal", defMap, loader);
  }

  public void initPlugin(LockssDaemon daemon, String extMapName,
			 ExternalizableMap defMap,
			 ClassLoader loader) {
    mapName = extMapName;
    this.classLoader = loader;
    this.definitionMap = defMap;
    // then call the overridden initializaton.
    super.initPlugin(daemon);
    initMimeMap();
    checkParamAgreement();
  }

  void checkParamAgreement() {
    for (String key : DefinableArchivalUnit.printfUrlListKeys) {
      checkParamAgreement(key, false);
    }
    for (String key : DefinableArchivalUnit.printfStringKeys) {
      checkParamAgreement(key, false);
    }
    for (String key : DefinableArchivalUnit.printfRegexpKeys) {
      checkParamAgreement(key, true);
    }
  }

  void checkParamAgreement(String key, boolean isRE) {
    List<String> printfList = getElementList(key);
    if (printfList == null) {
      return;
    }
    for (String printf : printfList) {
      if (StringUtil.isNullString(printf)) {
	log.warning("Null printf string in " + key);
	continue;
      }
      PrintfUtil.PrintfData p_data = PrintfUtil.stringToPrintf(printf);
      Collection<String> p_args = p_data.getArguments();
      for (String arg : p_args) {
	ConfigParamDescr descr = findAuConfigDescr(arg);
	if (descr == null) {
	  throw new
	    PluginException.InvalidDefinition("Not a declared parameter: " +
					      arg + " in " + printf + " in " +
					      getPluginName());
	}
	// ensure range params used only in REs
	if (!isRE) {
	  switch (descr.getType()) {
	  case ConfigParamDescr.TYPE_RANGE:
	  throw new
	    PluginException.InvalidDefinition("Range parameter (" + arg +
					      ") used in non-regexp in " +
					      getPluginName() + ": " + printf);
	  }
	}
      }
    }
  }

  public List<String> getLoadedFromUrls() {
    return loadedFromUrls;
  }

  public String getPluginName() {
    if (definitionMap.containsKey(KEY_PLUGIN_NAME)) {
      return definitionMap.getString(KEY_PLUGIN_NAME);
    } else {
      return getDefaultPluginName();
    }
  }

  protected String getDefaultPluginName() {
    return StringUtil.shortName(getPluginId());
  }
  
  public String getVersion() {
    return definitionMap.getString(KEY_PLUGIN_VERSION, DEFAULT_PLUGIN_VERSION);
  }

  public String getRequiredDaemonVersion() {
    return definitionMap.getString(KEY_REQUIRED_DAEMON_VERSION,
				   DEFAULT_REQUIRED_DAEMON_VERSION);
  }

  public String getPublishingPlatform() {
    return definitionMap.getString(KEY_PUBLISHING_PLATFORM, null);
  }

  public String getPluginNotes() {
    return definitionMap.getString(KEY_PLUGIN_NOTES, null);
  }

  public List<String> getElementList(String key) {
    Object element = definitionMap.getMapElement(key);
    List<String> lst;

    if (element instanceof String) {
      return Collections.singletonList((String)element);
    } else if (element instanceof List) {
      return (List)element;
    } else {
      return null;
    }
  }

  public List getLocalAuConfigDescrs()
      throws PluginException.InvalidDefinition {
    List auConfigDescrs =
      (List) definitionMap.getCollection(KEY_PLUGIN_CONFIG_PROPS, null);
    if (auConfigDescrs == null) {
      throw
	new PluginException.InvalidDefinition(mapName +
					      " missing ConfigParamDescrs");
    }
    return auConfigDescrs;
  }

  protected ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    DefinableArchivalUnit au =
      new DefinableArchivalUnit(this, definitionMap);
    au.setConfiguration(auConfig);
    return au;
  }

  public ExternalizableMap getDefinitionMap() {
    return definitionMap;
  }

  CacheResultHandler getCacheResultHandler() {
    return resultHandler;
  }

  String stripSuffix(String str, String suffix) {
    return str.substring(0, str.length() - suffix.length());
  }

  protected void initMimeMap() throws PluginException.InvalidDefinition {
    for (Iterator iter = definitionMap.entrySet().iterator(); iter.hasNext();){
      Map.Entry ent = (Map.Entry)iter.next();
      String key = (String)ent.getKey();
      Object val = ent.getValue();
      if (key.endsWith(DefinableArchivalUnit.SUFFIX_LINK_EXTRACTOR_FACTORY)) {
	String mime =
	  stripSuffix(key, DefinableArchivalUnit.SUFFIX_LINK_EXTRACTOR_FACTORY);
	if (val instanceof String) {
	  String factName = (String)val;
	  log.debug(mime + " link extractor: " + factName);
	  MimeTypeInfo.Mutable mti = mimeMap.modifyMimeTypeInfo(mime);
	  LinkExtractorFactory fact =
	    (LinkExtractorFactory)newAuxClass(factName,
					      LinkExtractorFactory.class);
	  mti.setLinkExtractorFactory(fact);
	}
      } else if (key.endsWith(DefinableArchivalUnit.SUFFIX_FILTER_FACTORY)) {
	String mime = stripSuffix(key,
				  DefinableArchivalUnit.SUFFIX_FILTER_FACTORY);
	if (val instanceof String) {
	  String factName = (String)val;
	  log.debug(mime + " filter: " + factName);
	  MimeTypeInfo.Mutable mti = mimeMap.modifyMimeTypeInfo(mime);
	  FilterFactory fact =
	    (FilterFactory)newAuxClass(factName, FilterFactory.class);
	  mti.setFilterFactory(fact);
	}
      } else if (key.endsWith(DefinableArchivalUnit.SUFFIX_FETCH_RATE_LIMITER)) {
	String mime =
	  stripSuffix(key, DefinableArchivalUnit.SUFFIX_FETCH_RATE_LIMITER);
	if (val instanceof String) {
	  String rate = (String)val;
	  log.debug(mime + " fetch rate: " + rate);
	  MimeTypeInfo.Mutable mti = mimeMap.modifyMimeTypeInfo(mime);
	  RateLimiter limit = mti.getFetchRateLimiter();
	  if (limit != null) {
	    limit.setRate(rate);
	  } else {
	    mti.setFetchRateLimiter(new RateLimiter(rate));
	  }
	}
      } else if (key.endsWith(DefinableArchivalUnit.SUFFIX_LINK_REWRITER_FACTORY)) {
	String mime =
	  stripSuffix(key, DefinableArchivalUnit.SUFFIX_LINK_REWRITER_FACTORY);
	String factName = (String)val;
	log.debug(mime + " link rewriter: " + factName);
	MimeTypeInfo.Mutable mti = mimeMap.modifyMimeTypeInfo(mime);
	LinkRewriterFactory fact =
	  (LinkRewriterFactory)newAuxClass(factName,
					   LinkRewriterFactory.class);
	mti.setLinkRewriterFactory(fact);
      }
    }
  }

  protected void initResultMap() throws PluginException.InvalidDefinition {
    HttpResultMap hResultMap = new HttpResultMap();
    // XXX Currently this only allows a CacheResultHandler class to
    // initialize the result map.  Instead, don't use a CacheResultMap
    // directly, use either the plugin's CacheResultHandler, if specified,
    // or a default one that wraps the CacheResultMap

    String handler_class = null;
    handler_class = definitionMap.getString(KEY_EXCEPTION_HANDLER, null);
    if (handler_class != null) {
      try {
        resultHandler =
            (CacheResultHandler)newAuxClass(handler_class,
					    CacheResultHandler.class);
        resultHandler.init(hResultMap);
      }
      catch (Exception ex) {
        throw new PluginException.InvalidDefinition(mapName
        + " has invalid Exception handler: " + handler_class, ex);
      }
      catch (LinkageError le) {
        throw new PluginException.InvalidDefinition(
            mapName + " has invalid Exception handler: " + handler_class,
	    le);

      }
    } else {
      // Expect a list of mappings from either result code or exception
      // name to CacheException name
      Collection<String> mappings =
	definitionMap.getCollection(KEY_EXCEPTION_LIST, null);
      if (mappings != null) {
        // add each entry
        for (String entry : mappings) {
	  String first;
	  String ceName;
	  Class ceClass;
          try {
            List<String> pair = StringUtil.breakAt(entry, '=', 2, true, true);
            first = pair.get(0);
            ceName = pair.get(1);
          } catch (Exception ex) {
            throw new PluginException.InvalidDefinition("Invalid syntax: " +
						    entry + "in " + mapName);
	  }
          try {
	    ceClass = Class.forName(ceName);
          } catch (Exception ex) {
            throw new
	      PluginException.InvalidDefinition("Second arg not a " +
						"CacheException class: " +
						entry + ", in " + mapName);
	  } catch (LinkageError le) {
	    throw new PluginException.InvalidDefinition("Can't load " + ceName,
							le);
	  }
	  try {
	    int code = Integer.parseInt(first);
	    // If parseable as an integer, it's a result code.
	    // Might need to make this load from plugin classpath
	    hResultMap.storeMapEntry(code, ceClass);
	  } catch (NumberFormatException e) {
	    try {
	      Class eClass = Class.forName(first);
	      // If a class name, it should be an exception class
	      // Might need to make this load ceName from plugin classpath
	      if (Exception.class.isAssignableFrom(eClass)) {
		hResultMap.storeMapEntry(eClass, ceClass);
	      } else {
		throw new
		  PluginException.InvalidDefinition("First arg not an " +
						    "Exception class: " +
						    entry + ", in " + mapName);
	      }		  
	    } catch (Exception ex) {
	      throw new
		PluginException.InvalidDefinition("First arg not a " +
						  "number or class: " +
						  entry + ", in " + mapName);
	    } catch (LinkageError le) {
	      throw new PluginException.InvalidDefinition("Can't load " +
							  first,
							  le);
	    }
	  }
	}
      }
    }
    resultMap = hResultMap;
  }

  /** Create a CrawlWindow if necessary and return it.  The CrawlWindow
   * must be thread-safe. */
  protected CrawlWindow makeCrawlWindow() {
    if (crawlWindow != null) {
      return crawlWindow;
    }
    CrawlWindow window =
      (CrawlWindow)definitionMap.getMapElement(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW_SER);
    if (window == null) {
      String window_class =
	definitionMap.getString(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW,
				null);
      if (window_class != null) {
	ConfigurableCrawlWindow ccw =
	  (ConfigurableCrawlWindow) newAuxClass(window_class,
						ConfigurableCrawlWindow.class);
	try {
	  window = ccw.makeCrawlWindow();
	} catch (PluginException e) {
	  throw new RuntimeException(e);
	}
      }
    }
    crawlWindow = window;
    return window;
  }

  protected UrlNormalizer getUrlNormalizer() {
    if (urlNorm == null) {
      String normalizerClass =
	definitionMap.getString(DefinableArchivalUnit.KEY_AU_URL_NORMALIZER,
				null);
      if (normalizerClass != null) {
	urlNorm =
	  (UrlNormalizer)newAuxClass(normalizerClass, UrlNormalizer.class);
      } else {
	urlNorm = NullUrlNormalizer.INSTANCE;
      }
    }
    return urlNorm;
  }

  protected ExploderHelper getExploderHelper() {
    if (exploderHelper == null) {
      String helperClass =
	definitionMap.getString(DefinableArchivalUnit.KEY_AU_EXPLODER_HELPER,
				null);
      if (helperClass != null) {
	exploderHelper =
	  (ExploderHelper)newAuxClass(helperClass, ExploderHelper.class);
      }
    }
    return exploderHelper;
  }

  protected FilterRule constructFilterRule(String contentType) {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);

    Object filter_el =
      definitionMap.getMapElement(mimeType +
				  DefinableArchivalUnit.SUFFIX_FILTER_RULE);

    if (filter_el instanceof String) {
      log.debug("Loading filter "+filter_el);
      return (FilterRule) newAuxClass( (String) filter_el, FilterRule.class);
    }
    else if (filter_el instanceof List) {
      if ( ( (List) filter_el).size() > 0) {
        return new DefinableFilterRule( (List) filter_el);
      }
    }
    return super.constructFilterRule(mimeType);
  }

  public String getPluginId() {
    String className;
    if(mapName != null) {
      className = mapName;
    }
    else {
      //@TODO: eliminate this when we eliminate subclasses
      className = this.getClass().getName();
    }
    return className;
  }
}
