/*
 * $Id: DefinablePlugin.java,v 1.24 2007-02-06 01:03:08 tlipkis Exp $
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
import org.lockss.plugin.wrapper.*;
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
  public static final String KEY_PLUGIN_CONFIG_PROPS = "plugin_config_props";
  public static final String KEY_EXCEPTION_HANDLER =
      "plugin_cache_result_handler";
  public static final String KEY_EXCEPTION_LIST =
      "plugin_cache_result_list";
  public static final String KEY_PLUGIN_NOTES = "plugin_notes";
  public static final String KEY_CRAWL_TYPE =
      "plugin_crawl_type";
  public static final String KEY_FOLLOW_LINKS = "plugin_follow_link";
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
  protected ClassLoader classLoader;
  protected String loadedFrom;
  protected CrawlWindow crawlWindow;

  public void initPlugin(LockssDaemon daemon, String extMapName)
      throws FileNotFoundException {
    initPlugin(daemon, extMapName, this.getClass().getClassLoader());
  }

  // Used by tests
  void initPlugin(LockssDaemon daemon, ExternalizableMap defMap)
      throws FileNotFoundException {
    initPlugin(daemon, defMap, this.getClass().getClassLoader());
  }

  public void initPlugin(LockssDaemon daemon, String extMapName,
			 ClassLoader loader)
      throws FileNotFoundException {
    // convert the plugin class name to an xml file name
    String mapFile = extMapName.replace('.', '/') + MAP_SUFFIX;
    // load the configuration map from jar file
    ExternalizableMap defMap = new ExternalizableMap();
    defMap.loadMapFromResource(mapFile, loader);
    URL url = loader.getResource(mapFile);
    if (url != null) {
      loadedFrom = url.toString();
    }
    this.initPlugin(daemon, extMapName, defMap, loader);
  }

  // Used by tests
  void initPlugin(LockssDaemon daemon,
			 ExternalizableMap defMap,
			 ClassLoader loader)
      throws FileNotFoundException {
    initPlugin(daemon, "Internal", defMap, loader);
  }

  public void initPlugin(LockssDaemon daemon, String extMapName,
			 ExternalizableMap defMap,
			 ClassLoader loader)
      throws FileNotFoundException {
    mapName = extMapName;
    this.classLoader = loader;
    this.definitionMap = defMap;
    // then call the overridden initializaton.
    super.initPlugin(daemon);
    initMimeMap();
  }

  public String getLoadedFrom() {
    return loadedFrom;
  }

  ClassLoader getClassLoader() {
    return classLoader;
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

  public String getPluginNotes() {
    return definitionMap.getString(KEY_PLUGIN_NOTES, null);
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
      if (key.endsWith(DefinableArchivalUnit.SUFFIX_LINK_EXTRACTOR_FACTORY)) {
	String mime =
	  stripSuffix(key, DefinableArchivalUnit.SUFFIX_LINK_EXTRACTOR_FACTORY);
	Object val = ent.getValue();
	if (val instanceof String) {
	  String factName = (String)val;
	  log.debug("initMime " + mime + ": " + factName);
	  MimeTypeInfo mti = mimeMap.modifyMimeTypeInfo(mime);
	  LinkExtractorFactory fact =
	    (LinkExtractorFactory)loadClass(factName,
					    LinkExtractorFactory.class);
	  mti.setLinkExtractorFactory(fact);
	}
      } else if (key.endsWith(DefinableArchivalUnit.SUFFIX_FILTER_FACTORY)) {
	String mime = stripSuffix(key,
				  DefinableArchivalUnit.SUFFIX_FILTER_FACTORY);
	Object val = ent.getValue();
	if (val instanceof String) {
	  String factName = (String)val;
	  log.debug("initMime " + mime + ": " + factName);
	  MimeTypeInfo mti = mimeMap.modifyMimeTypeInfo(mime);
	  FilterFactory fact =
	    (FilterFactory)loadClass(factName, FilterFactory.class);
	  mti.setFilterFactory(fact);
	}
      }
    }
  }

  protected void initResultMap() throws PluginException.InvalidDefinition {
    resultMap = new HttpResultMap();
    // we support two form of result handlers... either a class which handles
    // installing the numbers as well as handling any exceptions
    String handler_class = null;
    handler_class = definitionMap.getString(KEY_EXCEPTION_HANDLER, null);
    if (handler_class != null) {
      try {
        resultHandler =
            (CacheResultHandler) Class.forName(handler_class).newInstance();
        resultHandler.init(resultMap);
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
    }
    else {// or a list of individual exception remappings
      Collection results;
      results = definitionMap.getCollection(KEY_EXCEPTION_LIST, null);
      if (results != null) {
        // add each entry
        for (Iterator it = results.iterator(); it.hasNext(); ) {
          String entry = (String) it.next();
	  String class_name = null;
          try {
            Vector s_vec = StringUtil.breakAt(entry, '=', 2, true, true);
            class_name = (String) s_vec.get(1);
            int code = Integer.parseInt(((String) s_vec.get(0)));
            // now lets add the entry into the map.
            Class result_class = null;
            result_class = Class.forName(class_name);
            ( (HttpResultMap) resultMap).storeMapEntry(code, result_class);
          }
          catch (Exception ex) {
            throw new PluginException.InvalidDefinition(mapName
                                                 + " has invalid entry: "
                                                 + entry);
	  }
	  catch (LinkageError le) {
	    throw new PluginException.InvalidDefinition("Can't load " +
							class_name,
							le);
	  }
        }
      }
    }
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
	  (ConfigurableCrawlWindow) loadClass(window_class,
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
	  (UrlNormalizer)loadClass(normalizerClass, UrlNormalizer.class);
      } else {
	urlNorm = NullUrlNormalizer.INSTANCE;
      }
    }
    return urlNorm;
  }

  protected FilterRule constructFilterRule(String contentType) {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);

    Object filter_el =
      definitionMap.getMapElement(mimeType +
				  DefinableArchivalUnit.SUFFIX_FILTER_RULE);

    if (filter_el instanceof String) {
      log.debug("Loading filter "+filter_el);
      return (FilterRule) loadClass( (String) filter_el, FilterRule.class);
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


  // ---------------------------------------------------------------------
  //   CLASS LOADING SUPPORT ROUTINES
  // ---------------------------------------------------------------------

  Object loadClass(String className, Class loadedClass) {
    Object obj = null;
    try {
      obj = Class.forName(className, true, classLoader).newInstance();
    } catch (Exception ex) {
      log.error("Could not load " + className, ex);
      throw new
	PluginException.InvalidDefinition(getPluginName() + ": unable to create " +
				   loadedClass + " from " + className, ex);
    } catch (LinkageError le) {
      log.error("Could not load " + className, le);
      throw new
	PluginException.InvalidDefinition(getPluginName() + " unable to create " +
				   loadedClass + " from " + className, le);
    }
    if (!loadedClass.isInstance(obj)) {
      log.error(className + " is not a " + loadedClass.getName());
      throw new
	PluginException.InvalidDefinition(getPluginName() + ": wrong class, " +
				   className + " is " +
				   obj.getClass().getName() +
				   ", should be " + loadedClass);
    }
    return obj = WrapperUtil.wrap(obj, loadedClass);      
  }

}
