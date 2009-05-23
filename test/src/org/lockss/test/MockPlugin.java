/*
 * $Id: MockPlugin.java,v 1.29 2009-05-23 18:06:26 dshr Exp $
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
  private String pubPlatform = null;
  private String requiredDaemonVersion = "0.0.0";
  private List auConfigDescrs = ListUtil.list(ConfigParamDescr.BASE_URL,
					      ConfigParamDescr.VOLUME_NUMBER);

  private MetadataExtractor metadataExtractor = null;
  private ArticleIteratorFactory articleIteratorFactory = null;
  

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

  public void setPluginId(String id) {
    pluginId = id;
  }

  public String getVersion() {
    return pluginVer;
  }

  public String getRequiredDaemonVersion() {
    return requiredDaemonVersion;
  }

  public void setVersion(String ver) {
    pluginVer = ver;
  }

  public void setRequiredDaemonVersion(String ver) {
    requiredDaemonVersion = ver;
  }

  public String getPluginName() {
    return "Mock Plugin";
  }

  public String getPublishingPlatform() {
    return pubPlatform;
  }

  public void setPublishingPlatform(String pubPlatform) {
    this.pubPlatform = pubPlatform;
  }

  public MetadataExtractor getMetadataExtractor(String contentType,
						ArchivalUnit au) {
    return metadataExtractor;
  }

  public void setMetadataExtractor(MetadataExtractor me) {
    metadataExtractor = me;
  }

  public ArticleIteratorFactory getArticleIteratorFactory(String contentType) {
    return articleIteratorFactory;
  }

  public void setArticleIteratorFactory(ArticleIteratorFactory aif) {
    articleIteratorFactory = aif;
  }

  /**
   * Return the list of names of the Archival Units and volranges supported by
   * this plugin
   * @return a List of Strings
   */
  public List getSupportedTitles() {
    return ListUtil.list("MockSupportedTitle");
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

  public void setAuConfigDescrs(List descrs) {
    auConfigDescrs = descrs;
    allParamDescrs = null;		// clear cache
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

  public void setFetchRateLimiter(String contentType, RateLimiter limit) {
    rateLimiterMap.put(contentType, limit);
  }

  public RateLimiter getFetchRateLimiter(String contentType) {
    return rateLimiterMap.get(contentType);
  }
}
