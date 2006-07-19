/*
 * $Id: BasePlugin.java,v 1.39 2006-07-19 18:01:00 tlipkis Exp $
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
import org.lockss.util.urlconn.*;
import org.lockss.app.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
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
  static final String TITLE_PARAM_JOURNAL = "journalTitle";
  public static final String TITLE_PARAM_PLUGIN = "plugin";
  static final String TITLE_PARAM_PLUGIN_VERSION = "pluginVersion";
  static final String TITLE_PARAM_EST_SIZE = "estSize";
  static final String TITLE_PARAM_ATTRIBUTES = "attributes";
  static final String TITLE_PARAM_PARAM = "param";
  // Below org.lockss.title.xxx.param.n.
  static final String TITLE_PARAM_PARAM_KEY = "key";
  static final String TITLE_PARAM_PARAM_VALUE = "value";
  static final String TITLE_PARAM_PARAM_EDITABLE = "editable";

  protected LockssDaemon theDaemon;
  protected PluginManager pluginMgr;
  protected Collection aus = new ArrayList();
  protected Map titleConfigMap;
  // XXX need to generalize this
  protected CacheResultMap resultMap;

  /**
   * Must invoke this constructor in plugin subclass.
   */
  protected BasePlugin() {
  }

  public void initPlugin(LockssDaemon daemon) {
    theDaemon = daemon;
    pluginMgr = theDaemon.getPluginManager();

    theDaemon.getConfigManager().registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration prevConfig,
					 Configuration.Differences changedKeys) {
	  setConfig(newConfig, prevConfig, changedKeys);
	}
	public String toString() {
	  return getPluginId();
	}
      });
    initResultMap();
  }

  public void stopPlugin() {
  }

  public void stopAu(ArchivalUnit au) {
    // Is there any reason to notify the AU itself?
    aus.remove(au);
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
   * org.lockss.title.uid.param.2.editable=true</pre> where
   * <code>uid</code> is an identifier that is unique for each title.
   * Parameters for which <code>editable</code> is true (<i>eg</i>,
   * <code>year</code>) may be edited by the user to select a related AU.
   * <br>See TitleParams (and test/scripts/title-params) for an easy way to
   * create these property files.
   */
  protected void setConfig(Configuration newConfig,
			   Configuration prevConfig,
			   Configuration.Differences changedKeys) {
    if (changedKeys.contains(PARAM_TITLE_DB)) {
      setTitleConfig(newConfig);
    }
  }

  private void setTitleConfig(Configuration config) {
    String myName = getPluginId();
    Map titleMap = new HashMap();
    Collection myTitles = config.getTitleConfigs(myName);
    if (myTitles != null) {
      for (Iterator iter = myTitles.iterator(); iter.hasNext(); ) {
	Configuration titleConfig = (Configuration)iter.next();
	String pluginName = titleConfig.get(TITLE_PARAM_PLUGIN);
	if (myName.equals(pluginName)) {
	  if (log.isDebug2()) {
	    log.debug2("my titleConfig: " + titleConfig);
	  }
	  String title = titleConfig.get(TITLE_PARAM_TITLE);
	  TitleConfig tc = initOneTitle(titleConfig);
	  titleMap.put(title, tc);
	} else {
	  if (log.isDebug3()) {
	    log.debug3("titleConfig: " + titleConfig);
	  }
	}
      }
    }
    //TODO: decide on how to support plug-ins which do not use the title registry
    if (!titleMap.isEmpty()) {
      setTitleConfigMap(titleMap);
      notifyAusTitleDbChanged();
    }
  }

  TitleConfig initOneTitle(Configuration titleConfig) {
    String title = titleConfig.get(TITLE_PARAM_TITLE);
    TitleConfig tc = new TitleConfig(title, this);
    tc.setPluginVersion(titleConfig.get(TITLE_PARAM_PLUGIN_VERSION));
    tc.setJournalTitle(titleConfig.get(TITLE_PARAM_JOURNAL));
    if (titleConfig.containsKey(TITLE_PARAM_EST_SIZE)) {
      tc.setEstimatedSize(titleConfig.getSize(TITLE_PARAM_EST_SIZE, 0));
    }

    Configuration attrs = titleConfig.getConfigTree(TITLE_PARAM_ATTRIBUTES);
    if (!attrs.isEmpty()) {
      Map attrMap = new HashMap();
      for (Iterator iter = attrs.nodeIterator(); iter.hasNext(); ) {
	String attr = (String)iter.next();
	String val = attrs.get(attr);
	attrMap.put(attr, val);
      }
      tc.setAttributes(attrMap);
    }
    List params = new ArrayList();
    Configuration allParams = titleConfig.getConfigTree(TITLE_PARAM_PARAM);
    for (Iterator iter = allParams.nodeIterator(); iter.hasNext(); ) {
      Configuration oneParam = allParams.getConfigTree((String)iter.next());
      String key = oneParam.get(TITLE_PARAM_PARAM_KEY);
      String val = oneParam.get(TITLE_PARAM_PARAM_VALUE);
      ConfigParamDescr descr = findParamDescr(key);
      if (descr != null) {
	ConfigParamAssignment cpa = new ConfigParamAssignment(descr, val);
	if (oneParam.containsKey(TITLE_PARAM_PARAM_EDITABLE)) {
	  cpa.setEditable(oneParam.getBoolean(TITLE_PARAM_PARAM_EDITABLE,
					      cpa.isEditable()));
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


  protected void setTitleConfigMap(Map titleConfigMap) {
    this.titleConfigMap = titleConfigMap;
    pluginMgr.resetTitles();
  }

  protected void notifyAusTitleDbChanged() {
    for (Iterator iter = getAllAus().iterator(); iter.hasNext(); ) {
      //  They should all be BaseArchivalUnits, but just in case...
      try {
	BaseArchivalUnit au = (BaseArchivalUnit)iter.next();
	au.titleDbChanged();
      } catch (Exception e) {
	log.warning("notifyAusTitleDbChanged: " + this, e);
      }
    }
  }

  public List getAuConfigDescrs() {
    List res = new ArrayList(getLocalAuConfigDescrs());
    if (res.isEmpty()) {
      // Don't add internal params if plugin has no params.  (testing)
      return res;
    }
    res.add(ConfigParamDescr.AU_CLOSED);
    res.add(ConfigParamDescr.PUB_DOWN);
    res.add(ConfigParamDescr.PROTOCOL_VERSION);
    return res;
  }

  abstract protected List getLocalAuConfigDescrs();

  /**
   * Find the ConfigParamDescr that this plugin uses for the specified key.
   * @return the element of {@link #getAuConfigDescrs()} whose key
   * matches <code>key</code>, or null if none.
   */
  protected ConfigParamDescr findParamDescr(String key) {
    List descrs = getAuConfigDescrs();
    for (Iterator iter = descrs.iterator(); iter.hasNext(); ) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      if (descr.getKey().equals(key)) {
	return descr;
      }
    }
    return null;
  }


  // for now use the plugin's class name
  // tk - this will have to change to account for versioning
  public String getPluginId() {
    return this.getClass().getName();
  }

  public Collection getAllAus() {
    if (log.isDebug2()) log.debug2("getAllAus: aus: " + aus);
    return aus;
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
    aus.add(au);
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
}
