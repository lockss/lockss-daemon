/*
 * $Id: AuUtil.java,v 1.37 2013-03-14 06:38:49 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.net.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
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
  private static Logger log = Logger.getLogger("AuUtil");
  
  /** The default poll protocol to use, unless otherwise overridden by the
   * Archival Unit's poll_protocol config param.=
   */
  public static final String PARAM_POLL_PROTOCOL_VERSION =
    Configuration.PREFIX + "poll.defaultPollProtocol";
  private static final int DEFAULT_POLL_PROTOCOL_VERSION =
    Poll.V3_PROTOCOL;

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

  public static class AuProxyInfo {
    String host = null;
    int port;
    boolean isAuOverride = false;

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    public boolean isAuOverride() {
      return isAuOverride;
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
      try {
	HostPortParser hpp = new HostPortParser(auProxySpec);
	res.host = hpp.getHost();
	if (res.host != null) {
	  res.port = hpp.getPort();
	}
      } catch (HostPortParser.InvalidSpec e) {
	log.warning("Illegal AU crawl_proxy: " + auProxySpec, e);
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
    } catch (ConfigParamDescr.InvalidFormatException e) {
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

  /** Return the charset specified in the UC's response headers, or the
   * default charset.  Never returns null. */
  public static String getCharsetOrDefault(UrlCacher uc) {
    CIProperties props = uc.getUncachedProperties();
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

}
