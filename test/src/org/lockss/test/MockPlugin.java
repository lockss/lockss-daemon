/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.extractor.*;

/**
 * This is a mock version of <code>Plugin</code> used for testing
 */

public class MockPlugin extends BasePlugin implements PluginTestable {
  static Logger log = Logger.getLogger("MockPlugin");

  public static final String KEY = "org|lockss|test|MockPlugin";
  public static final String CONFIG_PROP_1 = "base_url";
  public static final String CONFIG_PROP_2 = "volume";

  private String pluginId = this.getClass().getName();
  private int initCtr = 0;
  private int stopCtr = 0;
  private Configuration auConfig;
  private String pluginVer = "MockVersion";
  private Map<Plugin.Feature,String> featureVersion;
  private String pluginName = "Mock Plugin";
  private String pubPlatform = null;
  private String requiredDaemonVersion = "0.0.0";
  private List auConfigDescrs = ListUtil.list(ConfigParamDescr.BASE_URL,
					      ConfigParamDescr.VOLUME_NUMBER);

  private ArticleMetadataExtractor metadataExtractor = null;
  private ArticleIteratorFactory articleIteratorFactory = null;
  private AuParamFunctor paramFunctor = null;
  

  public MockPlugin(){
    super();
  }

  public MockPlugin(LockssDaemon daemon){
    super();
    initPlugin(daemon);
  }

  /**
   * Called after plugin is loaded to give the plugin time to perform any
   * needed initializations.
   * @param daemon the LockssDaemon
   */
  public void initPlugin(LockssDaemon daemon) {
    super.initPlugin(daemon);
    initCtr++;
  }

  /**
   * Called when the application is stopping to allow the plugin to perform
   * any necessary tasks needed to cleanly halt
   */
  public void stopPlugin() {
    stopCtr++;
  }

  public String getPluginId() {
    return pluginId;
  }

  public MockPlugin setPluginId(String id) {
    pluginId = id;
    return this;
  }

  public String getVersion() {
    return pluginVer;
  }

  public List<String> getRequiredDaemonVersion() {
    if (requiredDaemonVersion == null) {
      return Collections.emptyList();
    }
    return ListUtil.list(requiredDaemonVersion);
  }

  public String getFeatureVersion(Plugin.Feature feat) {
    if (featureVersion == null) {
      return null;
    }
    return featureVersion.get(feat);
  }

  public MockPlugin setVersion(String ver) {
    pluginVer = ver;
    return this;
  }

  public MockPlugin setRequiredDaemonVersion(String ver) {
    requiredDaemonVersion = ver;
    return this;
  }

  public MockPlugin setFeatureVersionMap(Map<Plugin.Feature,String> featureVersion) {
    this.featureVersion = featureVersion;
    return this;
  }

  public String getPluginName() {
    return pluginName;
  }

  public String getPublishingPlatform() {
    return pubPlatform;
  }

  public MockPlugin setPublishingPlatform(String pubPlatform) {
    this.pubPlatform = pubPlatform;
    return this;
  }

  public ArticleMetadataExtractor
    getArticleMetadataExtractor(MetadataTarget target,
				ArchivalUnit au) {
    return metadataExtractor;
  }

  public MockPlugin setArticleMetadataExtractor(ArticleMetadataExtractor me) {
    metadataExtractor = me;
    return this;
  }

  public ArticleIteratorFactory getArticleIteratorFactory() {
    return articleIteratorFactory;
  }

  public MockPlugin setArticleIteratorFactory(ArticleIteratorFactory aif) {
    articleIteratorFactory = aif;
    return this;
  }

  /**
   * Return the list of names of the Archival Units and volranges supported by
   * this plugin
   * @return a List of Strings
   */
  public List getSupportedTitles() {
    if (titleConfigMap == null) {
      return ListUtil.list("MockSupportedTitle");
    } else {
      return super.getSupportedTitles();
    }
  }

  // Increase visibility for unit tests
  @Override
  public void setTitleConfigMap(Map<String, TitleConfig> titleConfigMap,
				Map<String, TitleConfig> auidMap) {
    super.setTitleConfigMap(titleConfigMap, auidMap);
  }

  public MockPlugin setPluginName(String name) {
    this.pluginName = name;
    return this;
  }

  /**
   * Return the set of configuration properties required to configure
   * an archival unit for this plugin.
   * @return a List of strings which are the names of the properties for
   * which values are needed in order to configure an AU
   */
  public List getLocalAuConfigDescrs() {
    return auConfigDescrs;
  }

  public MockPlugin setAuConfigDescrs(List descrs) {
    auConfigDescrs = descrs;
    allParamDescrs = null;		// clear cache
    return this;
  }

  public AuParamFunctor getAuParamFunctor() {
    if (paramFunctor != null) {
      return paramFunctor;
    } else {
      return super.getAuParamFunctor();
    }
  }

  public MockPlugin setAuParamFunctor(AuParamFunctor fn) {
    paramFunctor = fn;
    return this;
  }

  /**
   * Create an ArchivalUnit for the AU specified by the configuration.
   * @param auConfig Configuration object with values for all properties
   * returned by {@link #getAuConfigDescrs()}
   * @return the ArchivalUnit
   * @throws ArchivalUnit.ConfigurationException
   */
  protected ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    log.debug("createAu(" + auConfig + ")");
    MockArchivalUnit au = newMockArchivalUnit();
    au.setConfiguration(auConfig);
    au.setPlugin(this);
    return au;
  }

  // MockPlugin methods, not part of Plugin interface

  protected MockArchivalUnit newMockArchivalUnit() {
    return new MockArchivalUnit();
  }

  public int getInitCtr() {
    return initCtr;
  }

  public int getStopCtr() {
    return stopCtr;
  }

  public void registerArchivalUnit(ArchivalUnit au) {
    aus.add(au);
  }

  public void unregisterArchivalUnit(ArchivalUnit au) {
    aus.remove(au);
  }


  Map<String,RateLimiter> rateLimiterMap = new HashMap<String,RateLimiter>();

  public MockPlugin setFetchRateLimiter(String contentType, RateLimiter limit) {
    rateLimiterMap.put(contentType, limit);
    return this;
  }
}
