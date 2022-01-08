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

package org.lockss.plugin;

import java.io.IOException;
import java.net.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.daemon.*;
import org.lockss.jetty.CuResourceHandler;
import org.lockss.crawler.*;
import org.lockss.state.*;
import org.lockss.poller.*;
import org.lockss.repository.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.exploded.*;

/**
 * Static AU- and plugin-related utility methods.  These might logically
 * belong in either Plugin or ArchivalUnit, but they are defined entirely
 * in terms of already-public methods, so need not be implemented by plugin
 * writers, thus there's no need to muddy those interfaces.
 */
public class AuUtil {
	
  private static final Logger log = Logger.getLogger(AuUtil.class);
  
  /** The default poll protocol to use, unless otherwise overridden by the
   * Archival Unit's poll_protocol config param.=
   */
  public static final String PARAM_POLL_PROTOCOL_VERSION =
    Configuration.PREFIX + "poll.defaultPollProtocol";
  private static final int DEFAULT_POLL_PROTOCOL_VERSION =
    Poll.V3_PROTOCOL;

  /** The maximum number of CachedUrl versions to search to find the
   * earliert, in order to extract the earliest fetch time.
   */
  public static final String PARAM_EARLIEST_VERSION_SEARCH_MAX =
    Configuration.PREFIX + "plugin.earliestVersionSearchMax";
  private static final int DEFAULT_EARLIEST_VERSION_SEARCH_MAX = 100;

  // The parser of the formatted date in the CU property 'Date'.
  // SimpleDateFormat is not thread-safe, so this member requires synchronized
  // access.
  private static DateFormat CU_PROPERTY_DATE_PARSER =
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

  private static int earliestVersionSearchMax =
    DEFAULT_EARLIEST_VERSION_SEARCH_MAX;

  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
                               Configuration oldConfig,
                               Configuration.Differences diffs) {
    if (diffs.contains(PARAM_EARLIEST_VERSION_SEARCH_MAX)) {
      earliestVersionSearchMax =
	config.getInt(PARAM_EARLIEST_VERSION_SEARCH_MAX,
		      DEFAULT_EARLIEST_VERSION_SEARCH_MAX);
    }
  }

  public static LockssDaemon getDaemon(ArchivalUnit au) {
    return au.getPlugin().getDaemon();
  }

  /**
   * Return the AuState object for the AU
   * @param au the AU
   * @return the AuState
   */
  public static AuState getAuState(ArchivalUnit au) {
    NodeManager nodeManager = getDaemon(au).getNodeManager(au);
    return nodeManager.getAuState();
  }

  /**
   * Convenience method to return a {@link CuIterable? over the {@link
   * CachedUrl}s in the AU.
   * @param au the AU
   * @return a CuIterable
   */
  public static CuIterable getCuIterable(ArchivalUnit au) {
    return au.getAuCachedUrlSet().getCuIterable();
  }

  /**
   * Convenience method to return a {@link CuIterator? over the {@link
   * CachedUrl}s in the AU.
   * @param au the AU
   * @return a CuIterator
   */
  public static CuIterator getCuIterator(ArchivalUnit au) {
    return au.getAuCachedUrlSet().getCuIterator();
  }

  /**
   * Return the AuSuspectUrlVersions object for the AU
   * @param au the AU
   * @return the AuSuspectUrlVersions
   */
  public static AuSuspectUrlVersions getSuspectUrlVersions(ArchivalUnit au) {
    LockssRepository repo = getDaemon(au).getLockssRepository(au);
    return repo.getSuspectUrlVersions(au);
  }

  /**
   * Update the stored record of suspect versions for the AU
   * @param au the AU
   * @param asuv the AuSuspectUrlVersions object to store
   */
  public static void saveSuspectUrlVersions(ArchivalUnit au,
					    AuSuspectUrlVersions asuv)
      throws SerializationException {
    LockssRepository repo = getDaemon(au).getLockssRepository(au);
    repo.storeSuspectUrlVersions(au, asuv);
  }

  /**
   * Return the AuSuspectUrlVersions object for the AU
   * @param au the AU
   * @return the AuSuspectUrlVersions
   */
  public static boolean hasSuspectUrlVersions(ArchivalUnit au) {
    LockssRepository repo = getDaemon(au).getLockssRepository(au);
    return repo.hasSuspectUrlVersions(au);
  }

  public static AuNodeImpl getAuRepoNode(ArchivalUnit au) {
    LockssDaemon daemon = getDaemon(au);
    LockssRepository repo = daemon.getLockssRepository(au);
    try {
      return(AuNodeImpl)repo.getNode(au.getAuCachedUrlSet().getUrl());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * @param au An ArchivalUnit
   * @param url A URL
   * @return The RepositoryNode representing the URL in the given AU.
   * 
   * @throws MalformedURLException if the URL cannot be parsed.
   */
  public static RepositoryNode getRepositoryNode(ArchivalUnit au, String url) 
      throws MalformedURLException {
    LockssDaemon daemon = getDaemon(au);
    LockssRepository repo = daemon.getLockssRepository(au);
    return repo.getNode(url);
  }

  /**
   * Return the size of the AU, calculating it if necessary.
   * @param au the AU
   * @return the AU's total content size.
   */
  public static long getAuContentSize(ArchivalUnit au,
				      boolean calcIfUnknown) {
    LockssDaemon daemon = getDaemon(au);
    RepositoryNode repoNode = getAuRepoNode(au);
    return repoNode.getTreeContentSize(null, calcIfUnknown);
  }

  /**
   * Return the disk space used by the AU, including all overhead,
   * calculating it if necessary.
   * @param au the AU
   * @param calcIfUnknown if true, disk usage will calculated if unknown
   * (time consumeing)
   * @return the AU's disk usage in bytes.
   */
  public static long getAuDiskUsage(ArchivalUnit au, boolean calcIfUnknown) {
    LockssDaemon daemon = getDaemon(au);
    AuNodeImpl repoNode = getAuRepoNode(au);
    return repoNode.getDiskUsage(calcIfUnknown);
  }

  /** Return a string appropriate to use as a thread name for the specified
   * process working on the au. */
  public static String getThreadNameFor(String procName, ArchivalUnit au) {
    StringBuffer sb = new StringBuffer();
    sb.append(procName);
    sb.append(": ");
    sb.append(StringUtil.toUnaccented(au.getName()));
    return sb.toString();
  }

  public static String getConfigUserMessage(ArchivalUnit au) {
    // XXX change this to not require string to be copied into each AU
    TypedEntryMap map = au.getProperties();
    String str =
      map.getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null);
    if (str == null) {
      return null;
    }
    return str;
  }

  /** Return true if the supplied AU config appears to be compatible with
   * the plugin.  Checks only that all required (definitional) parameters
   * have values. */
  public static boolean isConfigCompatibleWithPlugin(Configuration config,
						     Plugin plugin) {
    for (Iterator iter = plugin.getAuConfigDescrs().iterator();
	 iter.hasNext();) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      String key = descr.getKey();
      String val = config.get(key);
      if (val == null) {
	if (descr.isDefinitional()) {
	  log.debug(descr + " is definitional, absent from " + config);
	  return false;
	}
      } else {
	if (!descr.isValidValueOfType(val)) {
	  log.debug(val + " is not a valid value of type " + descr);
	  return false;
	}
      }
    }
    return true;
  }

  /** Search titles belonging to <i>plugin</i> in the title DB for one that
   * matches the config.
   * @param config an AU config (unqualified)
   * @param plugin a plugin
   * @return the matching TitleConfig, or null if none found
   */
  // Unit test for this is in TestBaseArchivalUnit
  public static TitleConfig findTitleConfig(Configuration config,
					    Plugin plugin) {
    if (plugin.getSupportedTitles() == null)  {
      return null;
    }
    for (Iterator iter = plugin.getSupportedTitles().iterator();
	 iter.hasNext(); ) {
      String title = (String)iter.next();
      TitleConfig tc = plugin.getTitleConfig(title);
      if (tc != null && tc.matchesConfig(config) && tc.isSingleAu(plugin)) {
	return tc;
      }
    }
    return null;
  }

  // XXX Giving clients access to the plugin definition map allows them to
  // modify it, should be replaced by something safer.

  private static TypedEntryMap EMPTY_DEF_MAP = new TypedEntryMap();

  public static TypedEntryMap getPluginDefinition(ArchivalUnit au) {
    return getPluginDefinition(au.getPlugin());
  }

  public static TypedEntryMap getPluginDefinition(Plugin plug) {
    if (plug instanceof DefinablePlugin) {
      return ((DefinablePlugin)plug).getDefinitionMap();
    } else {
      return EMPTY_DEF_MAP;
    }
  }

  public static boolean isClosed(ArchivalUnit au) {
    return getBoolValue(getAuParamOrTitleDefault(au,
						 ConfigParamDescr.AU_CLOSED),
			false);
  }

  /**
   * Returns true if the AU has ever successfully completed a new content
   * crawl
   */
  public static boolean hasCrawled(ArchivalUnit au) {
    return getAuState(au).getLastCrawlTime() >= 0;
  }

  public static boolean okDeleteExtraFiles(ArchivalUnit au) {
    return !(au instanceof ExplodedArchivalUnit);
  }

  public static boolean isDeleteExtraFiles(ArchivalUnit au,
					   boolean dfault) {
    return getPluginDefinition(au)
      .getBoolean(DefinablePlugin.KEY_PLUGIN_DELETE_EXTRA_FILES, dfault);
  }

  public static boolean isRepairFromPublisherWhenTooClose(ArchivalUnit au,
							  boolean dfault) {
    return getPluginDefinition(au)
      .getBoolean(DefinablePlugin.KEY_REPAIR_FROM_PUBLISHER_WHEN_TOO_CLOSE,
		  dfault);
  }

  public static int minReplicasForNoQuorumPeerRepair(ArchivalUnit au,
						     int dfault) {
    return getPluginDefinition(au)
      .getInt(DefinablePlugin.KEY_MIN_REPLICAS_FOR_NO_QUORUM_PEER_REPAIR,
	      dfault);
  }

  public static boolean isPubDown(ArchivalUnit au) {
    return isPubNever(au) ||
      getBoolValue(getAuParamOrTitleDefault(au, ConfigParamDescr.PUB_DOWN),
		   false);
  }

  public static boolean isPubNever(ArchivalUnit au) {
    return getBoolValue(getAuParamOrTitleDefault(au,
						 ConfigParamDescr.PUB_NEVER),
			false);
  }

  public static boolean isPubDown(TitleConfig tc) {
    return isPubNever(tc) ||
      getBoolValue(getTitleDefault(tc, ConfigParamDescr.PUB_DOWN),
		   false);
  }

  public static boolean isPubNever(TitleConfig tc) {
    return getBoolValue(getTitleDefault(tc, ConfigParamDescr.PUB_NEVER),
			false);
  }

  public static boolean isParamUrlHttp(ArchivalUnit au, String paramKey) {
    String url = au.getConfiguration().get(paramKey);
    return url != null && UrlUtil.isHttpUrl(url);
  }
  
  public static boolean isParamUrlHttps(ArchivalUnit au, String paramKey) {
    String url = au.getConfiguration().get(paramKey);
    return url != null && UrlUtil.isHttpsUrl(url);
  }
  
  public static boolean isBaseUrlHttp(ArchivalUnit au) {
    return isParamUrlHttp(au, ConfigParamDescr.BASE_URL.getKey());
  }
  
  public static boolean isBaseUrlHttps(ArchivalUnit au) {
    return isParamUrlHttps(au, ConfigParamDescr.BASE_URL.getKey());
  }

  public static String normalizeHttpHttpsFromParamUrl(ArchivalUnit au,
                                                      String paramKey,
                                                      String url) {
    if (isParamUrlHttp(au, paramKey) && UrlUtil.isHttpsUrl(url)) {
      return UrlUtil.replaceScheme(url, "https", "http");
    }
    if (isParamUrlHttps(au, paramKey) && UrlUtil.isHttpUrl(url)) {
      return UrlUtil.replaceScheme(url, "http", "https");
    }
    return url;
  }
  
  public static String normalizeHttpHttpsFromBaseUrl(ArchivalUnit au,
                                                     String url) {
    return normalizeHttpHttpsFromParamUrl(au, ConfigParamDescr.BASE_URL.getKey(), url);
  }
  
  public static String getPollVersion(ArchivalUnit au) {
    Plugin plugin = au.getPlugin();
    String res = plugin.getFeatureVersion(Plugin.Feature.Poll);
    if (res == null) {
      res = plugin.getVersion();
    }
    return res;
  }

  public static int getProtocolVersion(ArchivalUnit au) {
    return getIntValue(getAuParamOrTitleDefault(au, ConfigParamDescr.PROTOCOL_VERSION),
                       CurrentConfig.getIntParam(PARAM_POLL_PROTOCOL_VERSION,
                                                 DEFAULT_POLL_PROTOCOL_VERSION));
  }

  public static List<String> getPluginList(ArchivalUnit au, String key) {
    Plugin plug = au.getPlugin();
    if (plug instanceof DefinablePlugin) {
      List<String> lst = ((DefinablePlugin)plug).getElementList(key);
      if (lst != null) {
	return Collections.unmodifiableList(lst);
      } else {
	return Collections.EMPTY_LIST;
      }
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  /** Return true iff the AU's plugin's implementation version of the named
   * Feature is the same as that recorded in the AU's AuState the last time
   * the feature was invoked.  (I.e., if false, the plugin has changed
   * since the AU was processed, so the AU may need to be reprocessed */
  public static boolean isCurrentFeatureVersion(ArchivalUnit au,
						Plugin.Feature feat) {
    Plugin plugin = au.getPlugin();
    AuState aus = AuUtil.getAuState(au);
    return StringUtil.equalStrings(plugin.getFeatureVersion(feat),
				   aus.getFeatureVersion(feat));
  }

  // support methods for json conversion of
  /** Serialize an AuAgreements to a json string
   * @param auaBean the AuAgreements
   */
  public static String jsonFromAuAgreements(AuAgreementsBean auaBean)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    setFieldsOnly(mapper);
    return mapper.writer().writeValueAsString(auaBean);
  }

  /**
   * Serialize an AuSuspectUrlVersions object into a JSON string
   *
   * @param ausuvBean
   *          An AuSuspectUrlVersions with the object to be serialized.
   * @return a String with the AuSuspectUrlVersions object serialized as a JSON
   *         string.
   * @throws IOException
   *           if any problem occurred during the serialization.
   */
  public static String jsonFromAuSuspectUrlVersionsBean(AuSuspectUrlVersionsBean ausuvBean)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    setFieldsOnly(mapper);
    return mapper.writer().writeValueAsString(ausuvBean);
  }

  /**
   * Serialize a DatedPeerIdSetImpl object into a JSON string.
   *
   * @param dpisBean
   *          A DatedPeerIdSetImpl as Bean to be serialized.
   * @return a String with the DatedPeerIdSetImpl object serialized as a
   *         JSON string.
   * @throws IOException
   *           if any problem occurred during the serialization.
   */
  public static String jsonFromDatedPeerIdSetBean(
      DatedPeerIdSetBean dpisBean)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    setFieldsOnly(mapper);
    return mapper.writer().writeValueAsString(dpisBean);
  }

  /**
   * Serialize a PersistentPeerIdSetImpl object into a JSON string.
   *
   * @param ppis
   *          A PersistentPeerIdSetImpl with the object to be serialized.
   * @return a String with the PersistentPeerIdSetImpl object serialized as a
   *         JSON string.
   * @throws IOException
   *           if any problem occurred during the serialization.
   */
  public static String jsonFromPersistentPeerIdSetImpl(
      PersistentPeerIdSetImpl ppis)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    setFieldsOnly(mapper);
    return mapper.writer().writeValueAsString(ppis);
  }

  static ObjectMapper setFieldsOnly(ObjectMapper mapper) {
    mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    return mapper;
  }

  public static class AuProxyInfo {
    String host = null;
    int port;
    String auSpec;
    boolean isAuOverride = false;
    boolean isInvalidAuOverride = false;

    public String getAuSpec() {
      return auSpec;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    public boolean isAuOverride() {
      return isAuOverride;
    }

    public boolean isInvalidAuOverride() {
      return isInvalidAuOverride;
    }

    public boolean equals(Object o) {
      if (o instanceof AuProxyInfo) {
	AuProxyInfo other = (AuProxyInfo)o;
	return StringUtil.equalStringsIgnoreCase(host, other.host)
	  && port == other.port;
      }
      return false;
    }
  }

  /**
   * Returns the proxy info specified by au's proxy spec if it is 
   * of the form "host:port" for the proxy host and port. Otherwise,
   * return the proxy host and port from the global configuration.
   *  
   * @param au the AU for the proxy spec from an AU of the form "host:port"
   *   or <code>null</code> if no override proxy is specified.
   * @return the proxy info from either the override or the global config 
   *   proxy settings
   */
  public static AuProxyInfo getAuProxyInfo(ArchivalUnit au) {
    return getAuProxyInfo(au, ConfigManager.getCurrentConfig());
  }

  /**
   * Returns the proxy info specified by auProxySpec if it is 
   * of the form "host:port" for the proxy host and port. Otherwise,
   * return the proxy host and port from the global configuration.
   *  
   * @param auProxySpec the proxy spec from an AU of the form "host:port"
   *   or <code>null</code> if no override proxy is specified.
   * @return the proxy info from either the override or the global proxy
   *   settings
   */
  public static AuProxyInfo getAuProxyInfo(String auProxySpec) {
    return getAuProxyInfo(auProxySpec, ConfigManager.getCurrentConfig());
  }

  /**
   * Returns the proxy info specified by au's proxy spec if it is 
   * of the form "host:port" for the proxy host and port. Otherwise,
   * return the proxy host and port from the specified configuration.
   *  
   * @param au the AU for the proxy spec from an AU of the form "host:port"
   *   or <code>null</code> if no override proxy is specified.
   * @param config the configuration specifying the global proxy host and port
   * @return the proxy info from either the override or the config proxy
   *   settings
   */
  public static AuProxyInfo getAuProxyInfo(ArchivalUnit au,
					   Configuration config) {
    // In RegistryArchivalUnit, CRAWL_PROXY is set in au.getProperties(),
    // not in au.getConfiguration().  Unless/until
    // getAuParamOrTitleDefault() is changed, we need to look in
    // au.getProperties() ourselves
    String auProxySpec =
        getStringValue(getAuParamOrTitleDefault(au, ConfigParamDescr.CRAWL_PROXY),
                       null);
    auProxySpec =
        au.getProperties().getString(ConfigParamDescr.CRAWL_PROXY.getKey(),
                                     auProxySpec);
    return getAuProxyInfo(auProxySpec, config);
  }
  
  /**
   * Returns the proxy info specified by auProxySpec if it is 
   * of the form "host:port" for the proxy host and port. Otherwise,
   * return the proxy host and port from the specified configuration.
   *  
   * @param auProxySpec the proxy spec from an AU of the form "host:port"
   *   or <code>null</code> if no override proxy is specified.
   * @param config the configuration specifying the global proxy host and port
   * @return the proxy info from either the override or the config proxy
   *   settings
   */
  public static AuProxyInfo getAuProxyInfo(String auProxySpec,
                                           Configuration config) {
    AuProxyInfo global = new AuProxyInfo();
    if (config.getBoolean(BaseCrawler.PARAM_PROXY_ENABLED,
			  BaseCrawler.DEFAULT_PROXY_ENABLED)) {
      global.host = config.get(BaseCrawler.PARAM_PROXY_HOST);
      global.port = config.getInt(BaseCrawler.PARAM_PROXY_PORT,
				  BaseCrawler.DEFAULT_PROXY_PORT);
      if (StringUtil.isNullString(global.host) || global.port <= 0) {
	global.host = null;
	global.port = 0;
      }
    }

    if (!StringUtil.isNullString(auProxySpec)) {
      AuProxyInfo res = new AuProxyInfo();
      res.auSpec = auProxySpec;
      try {
	HostPortParser hpp = new HostPortParser(auProxySpec);
	res.host = hpp.getHost();
	if (res.host != null) {
	  res.port = hpp.getPort();
	}
      } catch (HostPortParser.InvalidSpec e) {
	log.warning("Illegal AU crawl_proxy: " + auProxySpec, e);
	global.isInvalidAuOverride = true;
	global.auSpec = auProxySpec;
	return global;
      }
      res.isAuOverride = !res.equals(global);
      return res;
    }
    return global;
  }

  /** Return an attribute value from the AU's title DB entry, if any */
  public static String getTitleAttribute(ArchivalUnit au, String key) {
    TitleConfig tc = au.getTitleConfig();
    if (tc != null) {
      Map attrs = tc.getAttributes();
      if (attrs != null) {
	return (String)attrs.get(key);
      }
    }
    return null;
  }

  /** Return an attribute value from the AU's title DB entry, if any */
  public static String getTitleAttribute(ArchivalUnit au, String key,
					 String dfault) {
    String res = getTitleAttribute(au, key);
    return (res != null) ? res : dfault;
  }

  public static boolean hasSubstancePatterns(ArchivalUnit au) {
    TypedEntryMap map = getPluginDefinition(au);
    return
      (map.getMapElement(DefinableArchivalUnit.KEY_AU_SUBSTANCE_URL_PATTERN)
       != null) ||
      (map.getMapElement(DefinableArchivalUnit.KEY_AU_NON_SUBSTANCE_URL_PATTERN)
       != null) ||
      (map.getString(DefinablePlugin.KEY_PLUGIN_SUBSTANCE_PREDICATE_FACTORY,
		     null)
       != null);
  }

  public static int getSubstanceTestThreshold(ArchivalUnit au) {
    String key = ConfigParamDescr.CRAWL_TEST_SUBSTANCE_THRESHOLD.getKey();
    String thresh = getTitleAttribute(au, key);
    if (!StringUtil.isNullString(thresh)) {
      try {
	return Integer.parseInt(thresh);
      } catch (NumberFormatException e) {
	log.error("Illegal crawl test threshold: " + thresh
		  + ", performing regular crawl");
      }
    }
    Configuration auConfig = au.getConfiguration();
    if (auConfig.containsKey(key)) {
      try {
	return auConfig.getInt(key);
      } catch (Configuration.InvalidParam e) {
	log.error("Illegal crawl test threshold: " + auConfig.get(key)
		  + ", performing regular crawl");
      }
    }
    return -1;
  }

  public static boolean hasContentValidator(ArchivalUnit au) {
    MimeTypeMap mtm = au.getPlugin().getMimeTypeMap();
    return
      mtm.hasAnyThat(new Predicate<MimeTypeInfo>() {
	  public boolean evaluate(MimeTypeInfo mti) {
	    return mti.getContentValidatorFactory() != null;
	  }});
  }


  public static boolean getBoolValue(Object value, boolean dfault) {
    if (value instanceof Boolean) {
      return ((Boolean)value).booleanValue();
    }
    return dfault;
  }

  public static int getIntValue(Object value, int dfault) {
    if (value instanceof Integer) {
      return ((Integer)value).intValue();
    }
    return dfault;
  }

  public static String getStringValue(Object value, String dfault) {
    if (value instanceof String) {
      return (String)value;
    }
    return dfault;
  }

  /** Return the value of a config param either from the AU config or from
   * a default value in its TitleConfig */

  // This should probably look in au.getProperties() instead of
  // au.getConfiguration() so it sees inferred params.  That's a little
  // harder because it's typed.
  public static Object getAuParamOrTitleDefault(ArchivalUnit au,
						ConfigParamDescr cpd) {
    String key = cpd.getKey();
    String val = null;
    Configuration auConfig = au.getConfiguration();
    if (auConfig != null) {
      val = auConfig.get(key);
      if (!StringUtil.isNullString(val)) {
	return getValueOfType(val, cpd);
      }
    }
    TitleConfig tc = au.getTitleConfig();
    if (tc != null) {
      return getTitleDefault(tc, cpd);
    }
    return null;
  }

  public static Object getValueOfType(String valstr, ConfigParamDescr cpd) {
    if (valstr == null) {
      return null;
    }
    try {
      return cpd.getValueOfType(valstr);
    } catch (AuParamType.InvalidFormatException e) {
      return null;
    }
  }

  public static Object getTitleDefault(TitleConfig tc, ConfigParamDescr cpd) {
    ConfigParamAssignment cpa = tc.findCpa(cpd);
    if (cpa != null) {
      return getValueOfType(cpa.getValue(), cpd);
    }
    return null;
  }

  /** Call release() on the CachedUrl, ignoring any errors */
  public static void safeRelease(CachedUrl cu) {
    try {
      cu.release();
    } catch (Exception e) {}
  }

  /** Return the CachedUrl for a content node, or null if not a content
   * node */
  public static CachedUrl getCu(CachedUrlSetNode node) {
    switch (node.getType()) {
    case CachedUrlSetNode.TYPE_CACHED_URL_SET:
      CachedUrlSet cus = (CachedUrlSet)node;
      return cus.getArchivalUnit().makeCachedUrl(cus.getUrl());
    case CachedUrlSetNode.TYPE_CACHED_URL:
      return (CachedUrl)node;
    }
    return null;
  }

  public static List<String> getRedirectChain(CachedUrl cu) {
    ArchivalUnit au = cu.getArchivalUnit();
    List<String> res = new ArrayList<String>(3);
    res.add(cu.getUrl());
    Properties props = cu.getProperties();
    String redirUrl = props.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO);
    if (redirUrl != null) {
      do {
	res.add(redirUrl);
	CachedUrl redirCu = au.makeCachedUrl(redirUrl);
	if (redirCu == null || !redirCu.hasContent()) {
	  break;
	}
	try {
	  Properties redirProps = redirCu.getProperties();
	  redirUrl = redirProps.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO);
	} finally {
	  AuUtil.safeRelease(redirCu);
	}
      } while (redirUrl != null);
    }
    return res;
  }


  /** Return the charset specified in the UC's response headers, or the
   * default charset.  Never returns null. */
  public static String getCharsetOrDefault(CIProperties props) {
    if (props == null) {
      return Constants.DEFAULT_ENCODING;
    }
    String ctype = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
    return HeaderUtil.getCharsetOrDefaultFromContentType(ctype);
  }

  /** Return true iff the AU specifies archive file types whose memebers
   * should be accessible as CachedUrls */
  public static boolean hasArchiveFileTypes(ArchivalUnit au) {
    return au.getArchiveFileTypes() != null;
  }

  public static CacheException mapException(ArchivalUnit au,
					    LockssUrlConnection connection,
					    Exception fetchException,
					    String message) {
    CacheResultMap map = au.getPlugin().getCacheResultMap();
    return map.mapException(au, connection, fetchException, message);
  }

  /**
   * Provides the creation time of an Archival Unit.
   * 
   * @param au
   *          An ArchivalUnit with the Archival Unit.
   * @return a long with the creation time of the Archival Unit.
   */
  public static long getAuCreationTime(ArchivalUnit au) {
    final String DEBUG_HEADER = "getAuCreationTime(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "au = " + au);

    // Check whether the Archival Unit does not exist.
    if (au == null) {
      // Yes: Report the problem.
      throw new NullPointerException("Archival Unit is null");
    }

    long auCreationTime = 0;

    // Get the Archival Unit state.
    AuState auState = AuUtil.getAuState(au);

    // Check that the Archival Unit state exists.
    if (auState != null) {
      // Yes: Get the Archival Unit creation time.
      auCreationTime = auState.getAuCreationTime();
    } else {
      // No: Report the problem.
      throw new IllegalArgumentException("Archival Unit state is null");
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "auCreationTime = " + auCreationTime);
    return auCreationTime;
  }

  /**
   * Provides the earliest fetch time of a collection of URLs of an Archival
   * Unit.
   * 
   * @param au
   *          An ArchivalUnit with the Archival Unit.
   * @param urls
   *          A Collection<String> with the URLs.
   * @return a long with the the earliest fetch time of any of the passed URLs.
   */
  public static long getAuUrlsEarliestFetchTime(ArchivalUnit au,
      Collection<String> urls) {
    final String DEBUG_HEADER = "getAuUrlsEarliestFetchTime(): ";

    // Check whether the Archival Unit does not exist.
    if (au == null) {
      // Yes: Report the problem.
      throw new NullPointerException("Archival Unit is null");
    }

    // Check whether the Archival Unit does not exist.
    if (urls == null) {
      // Yes: Report the problem.
      throw new NullPointerException("No URLs");
    }

    long fetchTime = 0L;

    // Loop through all the URLs.
    for (String url : urls) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "url = " + url);

      try {
	// Get the fetch time of this URL.
	long newFetchTime = getUrlFetchTime(au, url);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "newFetchTime = " + newFetchTime);

	// Check whether it is earlier than any of the previously found fetch
	// times.
	if (newFetchTime > 0 && (fetchTime == 0 || newFetchTime < fetchTime)) {
	  // Yes: Remember it.
	  fetchTime = newFetchTime;
	}
      } catch (Exception e) {
	log.info("Exception caught getting fetch time for AU = '" + au
	    + "', URL = '" + url + "'", e);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "fetchTime = " + fetchTime);
    return fetchTime;
  }

  /**
   * Provides the fetch time of a URL.
   * 
   * @param au
   *          An ArchivalUnit with the Archival Unit.
   * @param url
   *          A String with the URL.
   * @return a long with the fetch time of the URL.
   */
  public static long getUrlFetchTime(ArchivalUnit au, String url) {
    final String DEBUG_HEADER = "getUrlFetchTime(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "au = " + au);
      log.debug2(DEBUG_HEADER + "url = " + url);
    }

    CachedUrl cachedUrl = null;
    long fetchTime = 0;

    try {
      // Get the cached URL.
      cachedUrl = au.makeCachedUrl(url);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cachedUrl = " + cachedUrl);

      // Check whether it is not a member of an archive.
      if (!cachedUrl.isArchiveMember()) {
	// Yes: Get the earlist version of the cached URL.
	// If this is version 1 don't need to check for earlier version.
	if (cachedUrl.getVersion() != 1) {
	  // XXX There's no good way to do this because the array is sorted
	  // most to least recent.  Must limit the number of versions
	  // returned for sanity, but might not get earliest
	  CachedUrl[] vers = cachedUrl.getCuVersions(earliestVersionSearchMax);
	  cachedUrl = vers[vers.length - 1];
	}
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "cachedUrl = " + cachedUrl);
      }
      // Get its properties.
      CIProperties cuProperties = cachedUrl.getProperties();
      if (log.isDebug3())
        log.debug3(DEBUG_HEADER + "cuProperties = " + cuProperties);

      // Try to get the best fetch time.
      String origFetchTimeAsString =
	  cuProperties.getProperty(CuResourceHandler.ORIG_HEADER_PREFIX
	      + CachedUrl.PROPERTY_FETCH_TIME);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "origFetchTimeAsString = "
	  + origFetchTimeAsString);

      // Check whether a fetch time was obtained.
      if (origFetchTimeAsString != null) {
	try {
	  // Yes: Try to parse it as a number.
	  fetchTime = Long.parseLong(origFetchTimeAsString);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "fetchTime = " + fetchTime);
	} catch (NumberFormatException nfe) {
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "origFetchTimeAsString is not a number");
	}
      }

      // Check whether a fetch time was not obtained.
      if (fetchTime == 0) {
	// Yes: Try to use the fetch time property.
	String fetchTimeAsString =
	    cuProperties.getProperty(CachedUrl.PROPERTY_FETCH_TIME);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "fetchTimeAsString = " + fetchTimeAsString);

	// Check whether a fetch time was obtained.
	if (fetchTimeAsString != null) {
	  try {
	    // Yes: Try to parse it as a number.
	    fetchTime = Long.parseLong(fetchTimeAsString);
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "fetchTime = " + fetchTime);
	  } catch (NumberFormatException nfe) {
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "fetchTimeAsString is not a number");
	  }
	}

	// Try to use the 'Date' property.
	String dateAsString = cuProperties.getProperty("Date");
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "dateAsString = " + dateAsString);

	// Check whether a fetch time was obtained.
	if (dateAsString != null) {
	  try {
	    // Yes: Try to parse it as a number.
	    long date = parseStringDate(dateAsString).getTime();
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "date = " + date);

	    // Use it if it's the only time we have or it's lower than the other
	    // one.
	    if (fetchTime == 0 || date < fetchTime) {
	      fetchTime = date;
	      if (log.isDebug3())
		log.debug3(DEBUG_HEADER + "fetchTime = " + fetchTime);
	    }
	  } catch (ParseException nfe) {
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "dateAsString is not a number");
	  }
	}
      }
    } finally {
      // Release the cached URL.
      safeRelease(cachedUrl);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "fetchTime = " + fetchTime);
    return fetchTime;
  }

  /**
   * Parses a text date in a thread-safe way.
   * 
   * @param dateAsString
   *          A String with the date to be parsed.
   * @return a Date with the parsed date.
   * @throws ParseException
   *           if there are problems parsing the date.
   */
  private static Date parseStringDate(String dateAsString)
      throws ParseException {
    synchronized (CU_PROPERTY_DATE_PARSER) {
      return CU_PROPERTY_DATE_PARSER.parse(dateAsString);
    }
  }
}
