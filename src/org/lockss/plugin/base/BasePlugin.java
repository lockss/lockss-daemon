/*
 * $Id: BasePlugin.java,v 1.15 2004-01-03 06:22:26 tlipkis Exp $
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

package org.lockss.plugin.base;

import java.util.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * Abstract base class for Plugins.  Plugins are encouraged to extend this
 * class to get some common Plugin functionality.
 */
public abstract class BasePlugin
    implements Plugin {
  static Logger log = Logger.getLogger("BasePlugin");

  static final String PARAM_TITLE_DB = ConfigManager.PARAM_TITLE_DB;

  // Below org.lockss.title.xxx.
  static final String TITLE_PARAM_TITLE = "title";
  static final String TITLE_PARAM_PLUGIN = "plugin";
  static final String TITLE_PARAM_PARAM = "param";
  // Below org.lockss.title.xxx.param.n.
  static final String TITLE_PARAM_PARAM_KEY = "key";
  static final String TITLE_PARAM_PARAM_VALUE = "value";
  static final String TITLE_PARAM_PARAM_DEFAULT = "default";

  static final protected String CM_NAME_KEY = "plugin_name";
  static final protected String CM_VERSION_KEY = "plugin_version";

  static final protected String CM_TITLE_MAP_KEY = "title_map";
  static final protected String CM_CONFIG_PROPS_KEY = "au_config_props";
  static final protected String CM_DEFINING_CONFIG_PROPS_KEY =
      "au_defining_props";

  protected LockssDaemon theDaemon;
  protected PluginManager pluginMgr;
  protected Collection aus = new ArrayList();
  protected Map titleConfigMap;
  protected ExternalizableMap configurationMap = new ExternalizableMap();

  /**
   * Must invoke this constructor in plugin subclass.
   */
  protected BasePlugin() {
  }

  public void initPlugin(LockssDaemon daemon) {
    theDaemon = daemon;
    pluginMgr = theDaemon.getPluginManager();

    Configuration.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration prevConfig,
					 Set changedKeys) {
	  setConfig(newConfig, prevConfig, changedKeys);
	}});

  }

  public void stopPlugin() {
  }

  public String getPluginName() {
    return configurationMap.getString(CM_NAME_KEY, "NO_NAME");
  }

  public String getVersion() {
    return configurationMap.getString(CM_VERSION_KEY, "UNKNOWN VERSION");
  }

  public List getAuConfigProperties() {
    return (List) configurationMap.getCollection(CM_CONFIG_PROPS_KEY, null);
  }

  public Collection getDefiningConfigKeys() {
    return configurationMap.getCollection(CM_DEFINING_CONFIG_PROPS_KEY, null);
  }

  /**
   * Default implementation collects keys from titleConfigMap.
   * @return a List
   */
  public List getSupportedTitles() {
    if (titleConfigMap == null) {
      return Collections.EMPTY_LIST;
    }
    return new ArrayList(titleConfigMap.keySet());
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

  /** Set up our titleConfigMap from the title definitions in the
   * Configuration.  Each title config looks like:<pre>
   * org.lockss.title.uid.title=Sample Title
   * org.lockss.title.uid.plugin=org.lockss.plugin.sample.SamplePlugin
   * org.lockss.title.uid.param.1.key=base_url
   * org.lockss.title.uid.param.1.value=http\://sample.org/
   * org.lockss.title.uid.param.2.key=year
   * org.lockss.title.uid.param.2.value=2003
   * org.lockss.title.uid.param.2.default=true</pre> where <code>uid</code>
   * is an identifier that is unique for each title.  Parameters for which
   * <code>default</code> is true (<i>eg</i>, <code>year</code>) are
   * expected to be edited by the user to select a related AU.  <br>See
   * TitleParams (and test/scripts/title-params) for an easy way to create
   * these property files.
   */
  protected void setConfig(Configuration newConfig,
			   Configuration prevConfig,
			   Set changedKeys) {
    setTitleConfigFromConfig(newConfig.getConfigTree(PARAM_TITLE_DB));
  }

  private void setTitleConfigFromConfig(Configuration allTitles) {
    String myName = getPluginId();
    Map titleMap = new HashMap();
    for (Iterator iter = allTitles.nodeIterator(); iter.hasNext(); ) {
      String titleKey = (String)iter.next();
      Configuration titleConfig = allTitles.getConfigTree(titleKey);
      log.debug3("titleKey: " + titleKey);
      log.debug3("titleConfig: " + titleConfig);
      String pluginName = titleConfig.get(TITLE_PARAM_PLUGIN);
      if (myName.equals(pluginName)) {
	String title = titleConfig.get(TITLE_PARAM_TITLE);
	TitleConfig tc = initOneTitle(titleConfig);
	titleMap.put(title, tc);
      }
    }
    if (titleMap.isEmpty()) {
      // XXX find better way
      // allow plugin to specify its own titles if none in config
      setTitleConfigFromPluginConfigMap();
    } else {
      setTitleConfigMap(titleMap);
    }
  }

  TitleConfig initOneTitle(Configuration titleConfig) {
    String pluginName = titleConfig.get(TITLE_PARAM_PLUGIN);
    String title = titleConfig.get(TITLE_PARAM_TITLE);
    TitleConfig tc = new TitleConfig(title, this);
    List params = new ArrayList();
    Configuration allParams = titleConfig.getConfigTree(TITLE_PARAM_PARAM);
    for (Iterator iter = allParams.nodeIterator(); iter.hasNext(); ) {
      Configuration oneParam = allParams.getConfigTree((String)iter.next());
      String key = oneParam.get(TITLE_PARAM_PARAM_KEY);
      String val = oneParam.get(TITLE_PARAM_PARAM_VALUE);
      ConfigParamDescr descr = findParamDescr(key);
      if (descr != null) {
	ConfigParamAssignment cpa = new ConfigParamAssignment(descr, val);
	if (oneParam.getBoolean(TITLE_PARAM_PARAM_DEFAULT, false)) {
	  cpa.setDefault(true);
	}
	params.add(cpa);
      } else {
	log.warning("Unknown parameter key: " + key + " in title: " + title);
	log.debug("   title config: " + titleConfig);
      }
    }
    tc.setParams(params);
    return tc;

  }

  private void setTitleConfigFromPluginConfigMap() {
    Map map = (Map)configurationMap.getMapElement(CM_TITLE_MAP_KEY);
    if (map != null) {
      setTitleConfigMap(map);
    }
  }

  protected void setTitleConfigMap(Map titleConfigMap) {
    this.titleConfigMap = titleConfigMap;
    pluginMgr.resetTitles();
  }

  /**
   * Find the ConfigParamDescr that this plugin uses for the specified key.
   * @return the element of {@link #getAuConfigProperties()} whose key
   * matches <code>key</code>, or null if none.
   */
  protected ConfigParamDescr findParamDescr(String key) {
    List descrs = getAuConfigProperties();
    for (Iterator iter = descrs.iterator(); iter.hasNext(); ) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      if (descr.getKey().equals(key)) {
	return descr;
      }
    }
    return null;
  }

  protected ExternalizableMap getConfigurationMap() {
    return configurationMap;
  }

  // for now use the plugin's class name
  // tk - this will have to change to account for versioning
  public String getPluginId() {
    return this.getClass().getName();
  }

  public Collection getAllAus() {
    log.debug2("getAllAus: aus: " + aus);
    return aus;
  }

  public ArchivalUnit configureAu(Configuration config, ArchivalUnit au) throws
      ArchivalUnit.ConfigurationException {
    if (au != null) {
      au.setConfiguration(config);
    }
    else {
      au = createAu(config);
      aus.add(au);
    }
    return au;
  }

  /**
   * Return the LockssDaemon instance
   * @return the LockssDaemon instance
   */
  public LockssDaemon getDaemon() {
    return theDaemon;
  }

  public CachedUrlSet makeCachedUrlSet(ArchivalUnit owner,
                                       CachedUrlSetSpec cuss) {
    return new BaseCachedUrlSet(owner, cuss);
  }

  public CachedUrl makeCachedUrl(CachedUrlSet owner, String url) {
    return new BaseCachedUrl(owner, url);
  }

  public UrlCacher makeUrlCacher(CachedUrlSet owner, String url) {
    return new BaseUrlCacher(owner, url);
  }
}
