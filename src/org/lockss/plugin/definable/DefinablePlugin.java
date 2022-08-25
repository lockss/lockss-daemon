/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.*;
import java.io.*;
import java.net.*;
import org.apache.commons.lang3.tuple.*;

import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.rewriter.*;
import org.lockss.config.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.crawler.*;
import org.lockss.extractor.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.plugin.definable.DefinableArchivalUnit.ConfigurableCrawlWindow;
import org.lockss.plugin.wrapper.*;

/**
 * <p>DefinablePlugin: a plugin which uses the data stored in an
*  ExternalizableMap to configure itself.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class DefinablePlugin extends BasePlugin {

  enum ParentVersionMismatchAction {Ignore, Warning, Error}

  /** If true, crawl rules in definable plugins are case-independent by
   * default.  Can override per-plugin with
   * <code>au_crawlrules_ignore_case</code> */
  static final String PARAM_PARENT_VERSION_MISMATCH_ACTION =
    Configuration.PREFIX + "plugin.parentVersionMismatchAction";
  static final ParentVersionMismatchAction
    DEFAULT_PARENT_VERSION_MISMATCH_ACTION = ParentVersionMismatchAction.Warning;


  // configuration map keys
  public static final String KEY_PLUGIN_IDENTIFIER = "plugin_identifier";
  public static final String KEY_PLUGIN_NAME = "plugin_name";
  public static final String KEY_PLUGIN_VERSION = "plugin_version";
  public static final String KEY_PLUGIN_FEATURE_VERSION_MAP =
    "plugin_feature_version_map";
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
  public static final String KEY_PLUGIN_CRAWL_URL_COMPARATOR_FACTORY =
    "plugin_crawl_url_comparator_factory";
  public static final String KEY_PLUGIN_FETCH_RATE_LIMITER_SOURCE =
    "plugin_fetch_rate_limiter_source";

  public static final String KEY_PLUGIN_BULK_CONTENT =
    "plugin_bulk_content";

  public static final String KEY_PLUGIN_ARCHIVE_FILE_TYPES =
    "plugin_archive_file_types";

  public static final String KEY_PLUGIN_ARTICLE_ITERATOR_FACTORY =
    "plugin_article_iterator_factory";
 
  public static final String KEY_PLUGIN_CRAWL_SEED_FACTORY = 
    "plugin_crawl_seed_factory";
  
  public static final String KEY_PLUGIN_ACCESS_URL_FACTORY =
    "plugin_access_url_factory";

  public static final String KEY_PLUGIN_URL_FETCHER_FACTORY = 
    "plugin_url_fetcher_factory";
  
  public static final String KEY_PLUGIN_URL_CONSUMER_FACTORY = 
    "plugin_url_consumer_factory";

  public static final String KEY_PLUGIN_ARTICLE_METADATA_EXTRACTOR_FACTORY =
    "plugin_article_metadata_extractor_factory";

  public static final String KEY_PLUGIN_SUBSTANCE_PREDICATE_FACTORY =
    "plugin_substance_predicate_factory";

  public static final String KEY_DEFAULT_ARTICLE_MIME_TYPE =
    "plugin_default_article_mime_type";

  public static final String KEY_ARTICLE_ITERATOR_ROOT =
    "plugin_article_iterator_root";

  public static final String KEY_ARTICLE_ITERATOR_PATTERN =
    "plugin_article_iterator_pattern";

  public static final String KEY_REPAIR_FROM_PUBLISHER_WHEN_TOO_CLOSE =
    "plugin_repair_from_publisher_when_too_close";

  public static final String KEY_MIN_REPLICAS_FOR_NO_QUORUM_PEER_REPAIR =
    "plugin_min_replicas_for_no_quorum_peer_repair";

  public static final String KEY_PLUGIN_DELETE_EXTRA_FILES =
    "plugin_delete_extra_files";

  public static final String KEY_PLUGIN_REWRITE_HTML_META_URLS =
    "plugin_rewrite_html_meta_urls";
  
  public static final String KEY_PLUGIN_STORE_PROBE_PERMISSION =
      "plugin_store_probe_permission";
  public static final boolean DEFAULT_PLUGIN_STORE_PROBE_PERMISSION =
      true;

  public static final String KEY_PLUGIN_SEND_REFERRER = "plugin_send_referrer";
  public static final boolean DEFAULT_PLUGIN_SEND_REFERRER = true;

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
  protected Map<Plugin.Feature,String> featureVersion;
  protected ArchiveFileTypes archiveFileSpec;

  public void initPlugin(LockssDaemon daemon, String extMapName)
      throws FileNotFoundException {
    initPlugin(daemon, extMapName, this.getClass().getClassLoader());
  }

  public void initPlugin(LockssDaemon daemon, String extMapName,
			 ClassLoader loader)
      throws FileNotFoundException {
    // convert the plugin class name to an xml file name
    // load the configuration map from jar file
    theDaemon = daemon;
    ExternalizableMap defMap = loadMap(extMapName, loader);
    this.initPlugin(daemon, extMapName, defMap, loader);
  }

  public void initPlugin(LockssDaemon daemon, String extMapName,
			 ExternalizableMap defMap,
			 ClassLoader loader) {
    mapName = extMapName;
    this.classLoader = loader;
    processObsolescentFields(defMap);
    this.definitionMap = defMap;
    super.initPlugin(daemon);
    initMimeMap();
    initFeatureVersions();
    initAuFeatureMap();
    initSubstancePatterns(DefinableArchivalUnit.KEY_AU_SUBSTANCE_URL_PATTERN);
    initSubstancePatterns(DefinableArchivalUnit.KEY_AU_NON_SUBSTANCE_URL_PATTERN);
    initArchiveFileTypes();
    checkParamAgreement();
    validate();
  }

  private ExternalizableMap loadMap(String extMapName, ClassLoader loader)
      throws FileNotFoundException {
    Configuration config = ConfigManager.getCurrentConfig();
    ParentVersionMismatchAction parentVerAct =
      (ParentVersionMismatchAction)
      config.getEnum(ParentVersionMismatchAction.class,
		     PARAM_PARENT_VERSION_MISMATCH_ACTION,
		     DEFAULT_PARENT_VERSION_MISMATCH_ACTION);
    String first = null;
    String next = extMapName;
    String nextParentVer = null;
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
      try {
	oneMap.loadMapFromResource(mapFile, loader);
      } catch (FileNotFoundException e) {
	if (next != extMapName) {
	  throw new
	    PluginException.ParentNotFoundException("Parent of " + extMapName +
						    " not found: " + next);
	}
	throw e;
      }
      if (nextParentVer != null) {

	String parentVer = oneMap.getString(KEY_PLUGIN_VERSION, null);
	if (!nextParentVer.equals(parentVer)) {
	  switch (parentVerAct) {
	  case Ignore:
	    break;
	  case Warning:
	    log.warning("Wrong parent version, expected " + nextParentVer +
			", was " + parentVer);
	    break;
	  case Error:
	    log.error("Wrong parent version, expected " + nextParentVer +
		      ", was " + parentVer);
	    throw new
	      PluginException.
	      ParentVersionMismatch("Plugin " + next +
				    " has version " + parentVer +
				    " expected " + nextParentVer);
	  }
	}
      }
      urls.add(url.toString());
      // apply overrides one plugin at a time in inheritance chain
      processOverrides(oneMap);
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
	nextParentVer = oneMap.getString(KEY_PLUGIN_PARENT_VERSION, null);
      } else {
	next = null;
      }
    }
    processDefault(res);
    loadedFromUrls = urls;
    return res;
  }

  void processDefault(TypedEntryMap map) {
    List<String> delKeys = new ArrayList<String>(4);;
    for (Map.Entry ent : map.entrySet()) {
      Object val = ent.getValue();
      if (val instanceof org.lockss.util.Default) {
	String key = (String)ent.getKey();
	log.debug(getDefaultPluginName() + ": Resetting "
		  + key + " to default value");
	delKeys.add(key);
      }
    }
    for (String key : delKeys) {
      map.removeMapElement(key);
    }
  }

  // Move any values from obsolescent keys to their official key
  void processObsolescentFields(TypedEntryMap map) {
    // au_manifest -> au_permission_url
    if (map.containsKey(DefinableArchivalUnit.KEY_AU_MANIFEST_OBSOLESCENT)) {
      map.setMapElement(DefinableArchivalUnit.KEY_AU_PERMISSION_URL,
			map.getMapElement(DefinableArchivalUnit.KEY_AU_MANIFEST_OBSOLESCENT));
      map.removeMapElement(DefinableArchivalUnit.KEY_AU_MANIFEST_OBSOLESCENT);
    }
    // au_crawl_depth -> au_refetch_depth
    if (map.containsKey(DefinableArchivalUnit.KEY_AU_CRAWL_DEPTH_OBSOLESCENT)) {
      map.setMapElement(DefinableArchivalUnit.KEY_AU_REFETCH_DEPTH,
			map.getMapElement(DefinableArchivalUnit.KEY_AU_CRAWL_DEPTH_OBSOLESCENT));
      map.removeMapElement(DefinableArchivalUnit.KEY_AU_CRAWL_DEPTH_OBSOLESCENT);
    }
  }

  /** If in testing mode FOO, copy values from FOO_override map, if any, to
   * main map */
  void processOverrides(TypedEntryMap map) {
    String testMode = getTestingMode();
    if (StringUtil.isNullString(testMode)) {
      return;
    }
    Object o =
      map.getMapElement(testMode + DefinableArchivalUnit.SUFFIX_OVERRIDE);
    if (o == null) {
      return;
    }
    if (o instanceof Map) {
      Map overrideMap = (Map)o;
      for (Map.Entry entry : (Set<Map.Entry>)overrideMap.entrySet()) {
	String key = (String)entry.getKey();
	Object val = entry.getValue();

	// If we add org.lockss.util.Parent it will do something like this
// 	if (val instanceof org.lockss.util.Default) {
// 	  log.debug(getDefaultPluginName() + ": Overriding "
// 		    + key + " with default value");
// 	  map.removeMapElement(key);
// 	} else {
	  log.debug(getDefaultPluginName() + ": Overriding "
		    + key + " with " + val);
	  map.setMapElement(key, val);
// 	}
      }
    }
  }

  // Currently just logs errors for illegal constructs.
  private void validate() {
    validateRequestCookies();
    validateRequestHeaders();
  }

  private void validateRequestCookies() {
    List<String> cookies =
      getElementList(DefinableArchivalUnit.KEY_AU_HTTP_COOKIES);
    if (cookies == null) {
      return;
    }
    for (String cookie : cookies) {
      int pos = cookie.indexOf("=");
      if (pos <= 0 || pos >= cookie.length() - 1) {
	log.error("Misformatted HTTP cookie: " + cookie);
      }
    }
  }

  private void validateRequestHeaders() {
    List<String> hdrs =
      getElementList(DefinableArchivalUnit.KEY_AU_HTTP_REQUEST_HEADERS);
    if (hdrs == null) {
      return;
    }
    for (String hdr : hdrs) {
      int pos = hdr.indexOf(":");
      if (pos <= 0 || pos >= hdr.length() - 1) {
	log.error("Misformatted HTTP request header: " + hdr);
      }
    }
  }

  String getTestingMode() {
    return theDaemon.getTestingMode();
  }

  // Used by tests

  public void initPlugin(LockssDaemon daemon, File file)
      throws PluginException {
    ExternalizableMap oneMap = new ExternalizableMap();
    oneMap.loadMap(file);
    if (oneMap.getErrorString() != null) {
      throw new PluginException(oneMap.getErrorString());
    }
    initPlugin(daemon, file.getPath(), oneMap, null);
  }

  void initPlugin(LockssDaemon daemon, ExternalizableMap defMap) {
    initPlugin(daemon, defMap, this.getClass().getClassLoader());
  }

  void initPlugin(LockssDaemon daemon,
			 ExternalizableMap defMap,
			 ClassLoader loader) {
    initPlugin(daemon, "Internal", defMap, loader);
  }



  void checkParamAgreement() {
    for (Map.Entry<String,PrintfConverter.PrintfContext> ent
	   : DefinableArchivalUnit.printfKeysContext.entrySet()) {
      checkParamAgreement(ent.getKey(), ent.getValue());
    }
  }

  void checkParamAgreement(String key, PrintfConverter.PrintfContext context) {
    Object val = definitionMap.getMapElement(key);
    for (String printf : flatten(val)) {
      if (StringUtil.isNullString(printf)) {
	log.warning("Null printf string in " + key);
	continue;
      }
      PrintfUtil.PrintfData p_data = PrintfUtil.stringToPrintf(printf);
      // Create a converter of the proper type.
      PrintfConverter converter =
	PrintfConverter.newConverterForContext(this, definitionMap, context);

      List<PrintfConverter.Expr> p_args = converter.parseArgs(this, p_data);
      for (PrintfConverter.Expr arg : p_args) {
	ConfigParamDescr descr = findAuConfigDescr(arg.getArg());
	if (descr == null) {
	  throw new
	    PluginException.InvalidDefinition("Not a declared parameter: " +
					      arg.getArg() + " in " + printf +
					      " in " + getPluginName());
	}
	AuParamType type = arg.getType();
	// XXX Ineffective because fntype defaults to String
	if (type == null) {
	  throw new
	    PluginException.InvalidDefinition("Not a declared parameter: " +
					      arg.getRaw() + " in " + printf + " in " +
					      getPluginName());
	}
	// ensure range and set params used only in legal context
	switch (context) {
	case Regexp:
	case Display:
	  // everything is legal in a regexp or a display string
	  break;
	case URL:
	  // NUM_RANGE and SET legal because can enumerate.  Can't
	  // enumerate RANGE
	  switch (type) {
	  case Range:
	    throw new
	      PluginException.InvalidDefinition("Range parameter (" + arg +
						") used in illegal context in " +
						getPluginName() + ": "
						+ key + ": " + printf);
	  default:
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

  public String getFeatureVersion(Plugin.Feature feat) {
    if (featureVersion == null) {
      return null;
    }
    return featureVersion.get(feat);
  }

  public String getRequiredDaemonVersion() {
    return definitionMap.getString(KEY_REQUIRED_DAEMON_VERSION,
				   DEFAULT_REQUIRED_DAEMON_VERSION);
  }

  public String getPublishingPlatform() {
    return definitionMap.getString(KEY_PUBLISHING_PLATFORM, null);
  }
  
  @Override
  public boolean storeProbePermission() {
    return definitionMap.getBoolean(KEY_PLUGIN_STORE_PROBE_PERMISSION,
                                    DEFAULT_PLUGIN_STORE_PROBE_PERMISSION);
  }

  @Override
  public boolean sendReferrer() {
    return definitionMap.getBoolean(KEY_PLUGIN_SEND_REFERRER,
                                    DEFAULT_PLUGIN_SEND_REFERRER);
  }

  public String getPluginNotes() {
    return definitionMap.getString(KEY_PLUGIN_NOTES, null);
  }

  public String getDefaultArticleMimeType() {
    String ret = definitionMap.getString(KEY_DEFAULT_ARTICLE_MIME_TYPE, null);
    log.debug3("DefaultArticleMimeType " + ret);
    if (ret == null) {
      ret = super.getDefaultArticleMimeType();
      log.debug3("DefaultArticleMimeType from super " + ret);
      
    }
    return ret;
  }

  public List<String> getElementList(String key) {
    return coerceToList(definitionMap.getMapElement(key));
  }

  public List<String> getElementList(String key, String mapkey) {
    Object val = definitionMap.getMapElement(key);
    if (val instanceof Map) {
      if (mapkey == null) {
	mapkey = "*";
      }
      Object mapval = ((Map)val).get(mapkey);
      if (mapval == null && !mapkey.equals("*")) {
	mapval = ((Map)val).get("*");
      }
      if (mapval == null) {
	return null;
      }
      return coerceToList(mapval);
    }
    return coerceToList(val);
  }

  List<String> flatten(final Object val) {
    if (val instanceof List) {
      return (List)val;
    } else if (val instanceof Map) {
      return new ArrayList<String>() {{
	  for (Map.Entry ent: ((Map<?,?>)val).entrySet()) {
	    addAll(flatten(ent.getValue()));
	  }
      }};
    } else if (val instanceof String) {
      return Collections.singletonList((String)val);
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  public List<String> coerceToList(Object val) {
    if (val instanceof String) {
      return Collections.singletonList((String)val);
    } else if (val instanceof List) {
      return (List)val;
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

  /** Given a key like "text/html_crawl_filter_factory", returns the MIME
   * type iff the value is one that should be processed (a String or {@link
   * org.lockss.util.None}) */
  String getMimeFromCompoundKey(String key, Object val, String suffix) {
    if (key.endsWith(suffix) &&
	(val instanceof org.lockss.util.None ||
	 val instanceof String)) {
      return stripSuffix(key, suffix);
    }
    return null;
  }

  protected void initMimeMap() throws PluginException.InvalidDefinition {
    for (Iterator iter = definitionMap.entrySet().iterator(); iter.hasNext();){
      Map.Entry ent = (Map.Entry)iter.next();
      String key = (String)ent.getKey();
      Object val = ent.getValue();
      String mime;

      if ((mime = getMimeFromCompoundKey(key, val, DefinableArchivalUnit.SUFFIX_LINK_EXTRACTOR_FACTORY)) != null) {
	MimeTypeInfo.Mutable mti = mimeMap.modifyMimeTypeInfo(mime);
	if (val instanceof org.lockss.util.None) {
	  log.debug2(mime + " no link extractor");
	  mti.setLinkExtractorFactory(null);
	} else if (val instanceof String) {
	  String factName = (String)val;
	  log.debug2(mime + " link extractor: " + factName);
	  LinkExtractorFactory fact =
	    (LinkExtractorFactory)newAuxClass(factName,
					      LinkExtractorFactory.class);
	  mti.setLinkExtractorFactory(fact);
	}
      } else if ((mime = getMimeFromCompoundKey(key, val, DefinableArchivalUnit.SUFFIX_CRAWL_FILTER_FACTORY)) != null) {
	// XXX This clause must precede the one for SUFFIX_HASH_FILTER_FACTORY
	// XXX unless/until that key is changed to not be a terminal substring
	// XXX of this one
	MimeTypeInfo.Mutable mti = mimeMap.modifyMimeTypeInfo(mime);
	if (val instanceof org.lockss.util.None) {
	  log.debug2(mime + " no crawl filter");
	  mti.setCrawlFilterFactory(null);
	} else if (val instanceof String) {
	  String factName = (String)val;
	  log.debug2(mime + " crawl filter: " + factName);
	  FilterFactory fact =
	    (FilterFactory)newAuxClass(factName, FilterFactory.class);
	  mti.setCrawlFilterFactory(fact);
	}
      } else if ((mime = getMimeFromCompoundKey(key, val, DefinableArchivalUnit.SUFFIX_HASH_FILTER_FACTORY)) != null) {
	MimeTypeInfo.Mutable mti = mimeMap.modifyMimeTypeInfo(mime);
	if (val instanceof org.lockss.util.None) {
	  log.debug2(mime + " no hash filter");
	  mti.setHashFilterFactory(null);
	} else if (val instanceof String) {
	  String factName = (String)val;
	  log.debug2(mime + " hash filter: " + factName);
	  FilterFactory fact =
	    (FilterFactory)newAuxClass(factName, FilterFactory.class);
	  mti.setHashFilterFactory(fact);
	}
      } else if ((mime = getMimeFromCompoundKey(key, val, DefinableArchivalUnit.SUFFIX_LINK_REWRITER_FACTORY)) != null) {
	MimeTypeInfo.Mutable mti = mimeMap.modifyMimeTypeInfo(mime);
	if (val instanceof org.lockss.util.None) {
	  log.debug2(mime + " no link rewriter");
	  mti.setLinkRewriterFactory(null);
	} else if (val instanceof String) {
	  String factName = (String)val;
	  log.debug2(mime + " link rewriter: " + factName);
	  LinkRewriterFactory fact =
	    (LinkRewriterFactory)newAuxClass(factName,
					     LinkRewriterFactory.class);
	  mti.setLinkRewriterFactory(fact);
	}
      } else if ((mime = getMimeFromCompoundKey(key, val, DefinableArchivalUnit.SUFFIX_CONTENT_VALIDATOR_FACTORY)) != null) {
	MimeTypeInfo.Mutable mti = mimeMap.modifyMimeTypeInfo(mime);
	if (val instanceof org.lockss.util.None) {
	  log.debug2(mime + " no content validator");
	  mti.setContentValidatorFactory(null);
	} else if (val instanceof String) {
	  String factName = (String)val;
	  log.debug2(mime + " content validator: " + factName);
	  ContentValidatorFactory fact =
	    (ContentValidatorFactory)newAuxClass(factName,
						 ContentValidatorFactory.class);
	  mti.setContentValidatorFactory(fact);
	}
      } else if (key.endsWith(DefinableArchivalUnit.SUFFIX_METADATA_EXTRACTOR_FACTORY_MAP) && ((mime = stripSuffix(key, DefinableArchivalUnit.SUFFIX_METADATA_EXTRACTOR_FACTORY_MAP)) != null)) {
	// add None processing if/when default md extractors exist
	MimeTypeInfo.Mutable mti = mimeMap.modifyMimeTypeInfo(mime);
	Map factNameMap = (Map)val;
	Map factClassMap = new HashMap();
	for (Iterator it = factNameMap.keySet().iterator(); it.hasNext(); ) {
	  String mdTypes = (String)it.next();
	  String factName = (String)factNameMap.get(mdTypes);
	  log.debug2(mime + " (" + mdTypes + ") metadata extractor: " +
		     factName);
	  for (String mdType :
		 (List<String>)StringUtil.breakAt(mdTypes, ";", true)) {
	    setMdTypeFact(factClassMap, mdType, factName);
	  }
	}
	mti.setFileMetadataExtractorFactoryMap(factClassMap);
      }
    }
  }

  private void setMdTypeFact(Map factClassMap, String mdType, String factName) {
    log.debug3("Metadata type: " + mdType + " factory " + factName);
    FileMetadataExtractorFactory fact =
      (FileMetadataExtractorFactory)newAuxClass(factName,
						FileMetadataExtractorFactory.class);
    factClassMap.put(mdType, fact);
  }

  public static Pattern RESULT_MAP_REDIR_PAT = Pattern.compile("redirto:(.*)");

  protected void initResultMap() throws PluginException.InvalidDefinition {
    HttpResultMap hResultMap = new HttpResultMap();

    // Allow a handler to initialize the result map.  (Currently unused?)
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
      // Expect a list of mappings from result code or Exception name
      // or name of result category, to CacheException name
      for (Pair<String,String> pair : getResultMapEntries()) {
        // If it's an action on a redirect pattern, ignore it here.
        // (It will be processed by DefinableArchivalUnit.foo())
        Matcher m = RESULT_MAP_REDIR_PAT.matcher(pair.getLeft());
        if (m.matches()) {
          continue;
        }
        Object val = parseResultMapRhs(pair.getRight());
        storeResultMapVal(hResultMap, pair.getLeft(), val);
        // XXX should make ResultAction from LHS, store in map
//         ResultAction act = parseResultAction(pair.getRight());
//         if (false) {
//           // throw if act not CacheException or CacheResultHandler
//         }
//         storeResultMapVal(hResultMap, pair.getLeft(), act);
      }
    }
    resultMap = hResultMap;
  }

  List<Pair<String,String>> getResultMapEntries() {
    Collection<String> mappings =
      definitionMap.getCollection(KEY_EXCEPTION_LIST, Collections.emptyList());
    if (log.isDebug2()) {
      log.debug2("resultMap: " + mappings);
    }
    try {
      return mappings.stream()
        .map(x -> StringUtil.breakAt(x, '=', 2, true, true))
        .map(x -> Pair.of(x.get(0), x.get(1)))
        .collect(Collectors.toList());
    } catch (Exception ex) {
      throw new PluginException.InvalidDefinition("Invalid syntax in result map: " + mappings, ex);
    }
  }

  /** Parse the result map RHS, which should be the name of either a
   * CacheException or CacheResultHandler class name.
   * @return either a CacheException class, or a CacheResultHandler instance
   */
  Object parseResultMapRhs(String rhs) {
    PluginFetchEventResponse resp =
      (PluginFetchEventResponse)newAuxClass(rhs, PluginFetchEventResponse.class,
                                            null);
    if (resp instanceof CacheException) {
      return resp.getClass();
    } else if (resp instanceof CacheResultHandler) {
      return WrapperUtil.wrap((CacheResultHandler)resp,
                              CacheResultHandler.class);
    } else {
      throw new PluginException.InvalidDefinition("RHS not a " +
                                                  "CacheException or " +
                                                  "CacheResultHandler class: " +
                                                  rhs + ", in " + mapName);
    }
  }

  /** Parse the  */
  public ResultAction parseResultAction(String rhs) {
    // If parseable as an integer, it's a result code.
    try {
      int code = Integer.parseInt(rhs);
      return ResultAction.remap(Integer.valueOf(code));
    } catch (Exception e) {
      log.debug3("not an int: " + rhs);
    }

    // Try as legal RHS in CacheResultMap: CacheException
    // CacheResultHandler
    try {
      PluginFetchEventResponse resp =
        newAuxClass(rhs, PluginFetchEventResponse.class, null);
      if (resp instanceof CacheException) {
        return ResultAction.exClass(resp.getClass());
      } else if (resp instanceof CacheResultHandler) {
        return ResultAction.handler(WrapperUtil.wrap((CacheResultHandler)resp,
                                                     CacheResultHandler.class));
      }
      log.debug3("not a CacheException or CacheResultHandler: " + resp);
    } catch (Exception e) {
      log.debug3("not a PluginFetchEventResponse: " + rhs);
    }

    // Try as an Exception to be remapped
    try {
      Exception resp = newAuxClass(rhs, Exception.class, null);
      return ResultAction.remap(resp.getClass());
    } catch (Exception e) {
      log.debug3("not an Exception: " + rhs);
    }
    return null;
  }

  /** Determine the type of the result map LHS, and store the RHS
   * value accordingly */
  private void storeResultMapVal(HttpResultMap hResultMap,
                                 String lhs, Object val) {
    // Try it as an integer
    try {
      int code = Integer.parseInt(lhs);

      // If parseable as an integer, it's a result code.
      hResultMap.storeMapEntry(code, val);
      return;
    } catch (NumberFormatException e) {
    }

    // Try it as a category name
    try {
      HttpResultMap.HttpResultCodeCategory cat =
        HttpResultMap.HttpResultCodeCategory.valueOf(lhs);
      hResultMap.storeResultCategoryEntries(cat, val);
      return;
    } catch (IllegalArgumentException e) {
    }

    // Try it as an Exception class name
    try {
      Class eClass = loadPluginClass(lhs, Exception.class);
      // If a class name, it should be an exception class
      if (Exception.class.isAssignableFrom(eClass)) {
        hResultMap.storeMapEntry(eClass, val);
        return;
      } else {
        throw new
          PluginException.InvalidDefinition("Lhs arg not an " +
                                            "Exception class: " +
                                            lhs + ", in " + mapName);
      }
    } catch (Exception ex) {
      throw new
        PluginException.InvalidDefinition("Lhs arg not a number, " +
                                          "exception class nor category: " +
                                          lhs + ", in " + mapName);
    } catch (LinkageError le) {
      throw new PluginException.InvalidDefinition("Can't load " +
                                                  lhs,
                                                  le);
    }
  }

  protected void initFeatureVersions()
      throws PluginException.InvalidDefinition {
    if (definitionMap.containsKey(KEY_PLUGIN_FEATURE_VERSION_MAP)) {
      Map<Plugin.Feature,String> map = new HashMap<Plugin.Feature,String>();
      Map<String,String> spec =
	(Map<String,String>)definitionMap.getMap(KEY_PLUGIN_FEATURE_VERSION_MAP);
      log.debug2("features: " + spec);
      for (Map.Entry<String,String> ent : spec.entrySet()) {
	try {
	  // Prefix version string with feature name to create separate
	  // namespace for each feature
	  String key = ent.getKey();
	  map.put(Plugin.Feature.valueOf(key), key + "_" + ent.getValue());
	} catch (RuntimeException e) {
	  log.warning(getPluginName() + " set unknown feature: "
		      + ent.getKey() + " to version " + ent.getValue(), e);
	  if (Boolean.getBoolean("org.lockss.unitTesting")) {
	    // Cause an error only during unit testing, when there's no
	    // legitimate reason for a plugin to name an unknown Feature
	    throw new PluginException.InvalidDefinition("Unknown feature: " +
							ent.getKey(),
							e);
	  }
	}
      }
      featureVersion = map;
    } else {
      featureVersion = null;
    }
  }

  protected void initAuFeatureMap() {
    if (definitionMap.containsKey(DefinableArchivalUnit.KEY_AU_FEATURE_URL_MAP)) {
      Map<String,?> featMap =
	definitionMap.getMap(DefinableArchivalUnit.KEY_AU_FEATURE_URL_MAP);
      for (Map.Entry ent : featMap.entrySet()) {
	Object val = ent.getValue();
	if (val instanceof Map) {
	  ent.setValue(MapUtil.expandAlternativeKeyLists((Map)val));
	}
      }
    }
  }

  // If substance patterns is a map, expand any multiple keys
  protected void initSubstancePatterns(String key) {
    if (definitionMap.containsKey(key)) {
      Object obj = definitionMap.getMapElement(key);
      if (obj instanceof Map) {
	definitionMap.setMapElement(key,
				    MapUtil.expandAlternativeKeyLists((Map)obj));
      }
    }
  }

  protected void initArchiveFileTypes () {
    if (definitionMap.containsKey(KEY_PLUGIN_ARCHIVE_FILE_TYPES)) {
      Object obj = 
	definitionMap.getMapElement(KEY_PLUGIN_ARCHIVE_FILE_TYPES);
      if (obj instanceof ArchiveFileTypes) {
	archiveFileSpec = (ArchiveFileTypes)obj;
	log.debug2(getPluginName() + ": ArchiveFileTypes: "
		   + archiveFileSpec.getExtMimeMap());
      } else if (obj instanceof String) {
	if ("standard".equalsIgnoreCase((String)obj)) {
	  archiveFileSpec = ArchiveFileTypes.DEFAULT;
	  log.debug2(getPluginName() + ": ArchiveFileTypes: STANDARD");
	}
      }
    }
  }

  public boolean isBulkContent() {
    return definitionMap.getBoolean(KEY_PLUGIN_BULK_CONTENT, false)
      // Temporary hack for compatibility with existing behavior; remove
      // once new versions of all bulk plugins have been distributed.
      || getPluginId().endsWith("SourcePlugin");
  }

  public ArchiveFileTypes getArchiveFileTypes() {
    return archiveFileSpec;
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

  public AuParamFunctor getAuParamFunctor() {
    String auParamFunctorClass =
      definitionMap.getString(DefinableArchivalUnit.KEY_AU_PARAM_FUNCTOR,
			      null);
    if (auParamFunctorClass != null) {
      return (AuParamFunctor)newAuxClass(auParamFunctorClass,
					 AuParamFunctor.class);
    }
    return super.getAuParamFunctor();
  }

  LoginPageChecker loginChecker;

  protected LoginPageChecker makeLoginPageChecker() {
    if (loginChecker == null) {
      String loginPageCheckerClass =
	definitionMap.getString(DefinableArchivalUnit.KEY_AU_LOGIN_PAGE_CHECKER,
				null);
      if (loginPageCheckerClass != null) {
	loginChecker = (LoginPageChecker)newAuxClass(loginPageCheckerClass,
						     LoginPageChecker.class);
      }
    }
    return loginChecker;
  }

  PermissionCheckerFactory permissionCheckerFact;

  protected PermissionCheckerFactory getPermissionCheckerFactory() {
    if (permissionCheckerFact == null) {
      String permissionCheckerFactoryClass =
	definitionMap.getString(DefinableArchivalUnit.KEY_AU_PERMISSION_CHECKER_FACTORY,
				null);
      if (permissionCheckerFactoryClass != null) {
	permissionCheckerFact =
	  (PermissionCheckerFactory)newAuxClass(permissionCheckerFactoryClass,
						PermissionCheckerFactory.class);
	log.debug2("Loaded PermissionCheckerFactory: " + permissionCheckerFact);
      }
    }
    return permissionCheckerFact;
  }

  protected UrlNormalizer urlNorm;

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

  protected ExploderHelper exploderHelper = null;

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
  
  protected CrawlSeedFactory crawlSeedFactory = null;

  protected CrawlSeedFactory getCrawlSeedFactory() {
    if (crawlSeedFactory == null) {
      String factClass =
    definitionMap.getString(DefinablePlugin.KEY_PLUGIN_CRAWL_SEED_FACTORY,
				null);
      if (factClass != null) {
    	crawlSeedFactory =
    	(CrawlSeedFactory)newAuxClass(factClass, CrawlSeedFactory.class);
      }
    }
    
    return crawlSeedFactory;
  }
  
  protected FeatureUrlHelperFactory featHelperFact = null;

  protected FeatureUrlHelperFactory getFeatureUrlHelperFactory() {
    if (featHelperFact == null) {
      String factClass =
	definitionMap.getString(DefinablePlugin.KEY_PLUGIN_ACCESS_URL_FACTORY,
				null);
      if (factClass != null) {
	featHelperFact =
    	(FeatureUrlHelperFactory)newAuxClass(factClass, FeatureUrlHelperFactory.class);
      }
    }

    return featHelperFact;
  }

  protected UrlFetcher makeUrlFetcher(CrawlerFacade facade, String url) {
	UrlFetcherFactory fact = getUrlFetcherFactory();
	  if (fact == null) {
	    return null;
	  }
	  return fact.createUrlFetcher(facade, url);
  }
  
  protected UrlFetcherFactory urlFetcherFactory = null;

  protected UrlFetcherFactory getUrlFetcherFactory() {
    if (urlFetcherFactory == null) {
      String factClass =
    definitionMap.getString(DefinablePlugin.KEY_PLUGIN_URL_FETCHER_FACTORY,
        null);
      if (factClass != null) {
      urlFetcherFactory =
      (UrlFetcherFactory)newAuxClass(factClass, UrlFetcherFactory.class);
      } else {
        return new SimpleUrlFetcherFactory();
      }
    }
    
    return urlFetcherFactory;
  }
  
  protected CrawlSeed getCrawlSeed(Crawler.CrawlerFacade crawlFacade) {
    CrawlSeedFactory fact = getCrawlSeedFactory();
    if (fact == null) {
      return null;
    }
    return fact.createCrawlSeed(crawlFacade);
  }
  
  protected FeatureUrlHelper getFeatureUrlHelper(ArchivalUnit au) {
    FeatureUrlHelperFactory fact = getFeatureUrlHelperFactory();
    if (fact == null) {
      return null;
    }
    return fact.createFeatureUrlHelper(au.getPlugin());
  }

  protected UrlConsumerFactory urlConsumerFactory = null;

  protected UrlConsumerFactory getUrlConsumerFactory() {
    if (urlConsumerFactory == null) {
      String factClass =
    definitionMap.getString(DefinablePlugin.KEY_PLUGIN_URL_CONSUMER_FACTORY,
        null);
      if (factClass != null) {
      urlConsumerFactory =
      (UrlConsumerFactory)newAuxClass(factClass, UrlConsumerFactory.class);
      } else {
        return new SimpleUrlConsumerFactory();
      }
    }
    
    return urlConsumerFactory;
  }

  protected CrawlUrlComparatorFactory crawlUrlComparatorFactory = null;

  protected CrawlUrlComparatorFactory getCrawlUrlComparatorFactory() {
    if (crawlUrlComparatorFactory == null) {
      String factClass =
	definitionMap.getString(DefinablePlugin.KEY_PLUGIN_CRAWL_URL_COMPARATOR_FACTORY,
				null);
      if (factClass != null) {
	crawlUrlComparatorFactory =
	  (CrawlUrlComparatorFactory)newAuxClass(factClass,
						 CrawlUrlComparatorFactory.class);
      }
    }
    return crawlUrlComparatorFactory;
  }

  protected Comparator<CrawlUrl> getCrawlUrlComparator(ArchivalUnit au)
      throws PluginException.LinkageError {
    CrawlUrlComparatorFactory fact = getCrawlUrlComparatorFactory();
    if (fact == null) {
      return null;
    }
    return fact.createCrawlUrlComparator(au);
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

  protected ArticleIteratorFactory articleIteratorFact = null;
  protected ArticleMetadataExtractorFactory articleMetadataFact = null;

  /**
   * Returns the plugin's article iterator factory, if any
   * @return the ArticleIteratorFactory
   */
  public ArticleIteratorFactory getArticleIteratorFactory() {
    if (articleIteratorFact == null) {
      String factClass =
	definitionMap.getString(KEY_PLUGIN_ARTICLE_ITERATOR_FACTORY,
				null);
      if (factClass != null) {
	articleIteratorFact =
	  (ArticleIteratorFactory)newAuxClass(factClass,
					      ArticleIteratorFactory.class);
      }
    }
    return articleIteratorFact;
  }

  /**
   * Returns the article iterator factory for the content type, if any
   * @param contentType the content type
   * @return the ArticleIteratorFactory
   */
  public ArticleMetadataExtractorFactory
    getArticleMetadataExtractorFactory(MetadataTarget target) {
    if (articleMetadataFact == null) {
      String factClass =
	definitionMap.getString(KEY_PLUGIN_ARTICLE_METADATA_EXTRACTOR_FACTORY,
				null);
      if (factClass != null) {
	articleMetadataFact =
	  (ArticleMetadataExtractorFactory)newAuxClass(factClass,
						       ArticleMetadataExtractorFactory.class);
      }
    }
    return articleMetadataFact;
  }

  protected SubstancePredicateFactory substancePredFact = null;

  /**
   * Returns the plugin's substance predicate factory, if any
   * @return the SubstancePredicateFactory
   */
  public SubstancePredicateFactory getSubstancePredicateFactory() {
    if (substancePredFact == null) {
      String factClass =
	definitionMap.getString(KEY_PLUGIN_SUBSTANCE_PREDICATE_FACTORY,
				null);
      if (factClass != null) {
	substancePredFact =
	  (SubstancePredicateFactory)newAuxClass(factClass,
						 SubstancePredicateFactory.class);
      }
    }
    return substancePredFact;
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
