/*
 * $Id: EditableDefinablePlugin.java,v 1.2 2004-05-13 02:21:49 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.devtools.plugindef;

import java.lang.reflect.*;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

public class EditableDefinablePlugin
    extends DefinablePlugin {

  static final protected String PLUGIN_NAME
      = DefinablePlugin.CM_NAME_KEY;
  static final protected String PLUGIN_VERSION
      = DefinablePlugin.CM_VERSION_KEY;
  static final protected String PLUGIN_PROPS
      = DefinablePlugin.CM_CONFIG_PROPS_KEY;
  static final protected String PLUGIN_EXCEPTION_HANDLER
      = DefinablePlugin.CM_EXCEPTION_HANDLER_KEY;
  static final protected String CM_EXCEPTION_LIST_KEY
      = DefinablePlugin.CM_EXCEPTION_LIST_KEY;

  static final protected String AU_START_URL
      = DefinableArchivalUnit.CM_AU_START_URL_KEY;
  static final protected String AU_NAME
      = DefinableArchivalUnit.CM_AU_NAME_KEY;
  static final protected String AU_RULES
      = DefinableArchivalUnit.CM_AU_RULES_KEY;
  static final protected String AU_CRAWL_WINDOW
      = DefinableArchivalUnit.CM_AU_CRAWL_WINDOW_KEY;
  static final protected String AU_EXPECTED_PATH
      = DefinableArchivalUnit.CM_AU_EXPECTED_PATH;
  static final protected String AU_CRAWL_DEPTH
      = DefinableArchivalUnit.CM_AU_CRAWL_DEPTH;
  static final protected String AU_NEWCONTENT_CRAWL
      = DefinableArchivalUnit.CM_AU_DEFAULT_NC_CRAWL_KEY;
  static final protected String AU_PAUSE_TIME
      = DefinableArchivalUnit.CM_AU_DEFAULT_PAUSE_TIME;
  static final protected String AU_MANIFEST
      = DefinableArchivalUnit.CM_AU_MANIFEST_KEY;
  static final protected String AU_PARSER_SUFFIX
      = DefinableArchivalUnit.CM_AU_PARSER_SUFFIX;
  static final public String AU_FILTER_SUFFIX
      = DefinableArchivalUnit.CM_AU_FILTER_SUFFIX;

  static public String[] CONFIG_PARAM_TYPES = ConfigParamDescr.TYPE_STRINGS;

  static public Map DEFAULT_CONFIG_PARAM_DESCRS = getDefaultConfigParamDescrs();

  public EditableDefinablePlugin() {
  }
  // for reading map files
  public void loadMap(String location, String name) {
    String mapFile = ExternalizableMap.MAPPING_FILE_NAME;
    definitionMap.loadMap(location, name, mapFile);
  }

  // for writing map files
  public void writeMap(String location, String name) {
    // store the configuration map
    definitionMap.storeMap(location, name, ExternalizableMap.MAPPING_FILE_NAME);
  }

  public String getMapName() {
    return mapName;
  }

  public void setMapName(String name) {
    if (name.endsWith(MAP_SUFFIX)) {
      mapName = name;
    }
    else {
      mapName = name + MAP_SUFFIX;
    }
  }

  public void setAuStartURL(String startUrl) {
    definitionMap.putString(AU_START_URL, startUrl);
  }

  public String getAuStartUrl() {

    return definitionMap.getString(AU_START_URL, null);
  }

  public void removeAuStartURL() {
    definitionMap.removeMapElement(AU_START_URL);
  }

  public void setAuName(String name) {
    definitionMap.putString(AU_NAME, name);
  }

  public String getAuName() {
    return definitionMap.getString(AU_NAME, null);
  }

  public void removeAuName() {
    definitionMap.removeMapElement(AU_NAME);
  }

  public void setAuCrawlRules(Collection rules) {
    definitionMap.putCollection(AU_RULES, rules);
  }

  public Collection getAuCrawlRules() {
    return definitionMap.getCollection(AU_RULES, Collections.EMPTY_LIST);
  }

  public void removeAuCrawlRules() {
    definitionMap.removeMapElement(AU_RULES);
  }

  public void addCrawlRule(String rule) {
    List rules = (List) definitionMap.getCollection(AU_RULES, new ArrayList());
    for (Iterator it = rules.iterator(); it.hasNext(); ) {
      String str = (String) it.next();
      if (str.equals(rule)) {
        return;
      }
    }
    rules.add(rule);
    definitionMap.putCollection(AU_RULES, rules);
  }

  public void removeCrawlRule(String rule) {
    List rules = (List) definitionMap.getCollection(AU_RULES, null);
    if (rules == null)return;

    for (Iterator it = rules.iterator(); it.hasNext(); ) {
      String str = (String) it.next();
      if (str.equals(rule)) {
        it.remove();
      }
    }

  }

  public void setAuCrawlWindow(String crawlWindow) {

    try {
      definitionMap.putString(AU_CRAWL_WINDOW, crawlWindow);
      CrawlWindow win = (CrawlWindow) Class.forName(crawlWindow).newInstance();
    }
    catch (Exception ex) {
      throw new DefinablePlugin.InvalidDefinitionException(
          "Unable to create crawl window class: " + crawlWindow, ex);
    }
  }

  public String getAuCrawlWindow() {
    return definitionMap.getString(AU_CRAWL_WINDOW, null);
  }

  public void removeAuCrawlWindow() {
    definitionMap.removeMapElement(AU_CRAWL_WINDOW);
  }

  public void setAuFilter(String mimetype, String filter) {

    try {
      definitionMap.putString(mimetype + AU_FILTER_SUFFIX, filter);
      FilterRule rule = (FilterRule) Class.forName(filter).newInstance();
    }
    catch (Exception ex) {
      throw new DefinablePlugin.InvalidDefinitionException(
          "Unable to create filter rule class " + filter +
          "for mimetype " + mimetype, ex);
    }
  }

  public HashMap getAuFilters() {
    HashMap rules = new HashMap();
    Set keyset = definitionMap.keySet();
    for(Iterator it = keyset.iterator(); it.hasNext();) {
      String key = (String) it.next();
      if(key.endsWith(AU_FILTER_SUFFIX)) {
        String mimetype = key.substring(0,key.lastIndexOf(AU_FILTER_SUFFIX));
        rules.put(mimetype, definitionMap.getString(key, null));
      }
    }
    return rules;
  }

  public void removeAuFilter(String mimetype) {
    definitionMap.removeMapElement(mimetype + AU_FILTER_SUFFIX);
  }

  public void setAuExpectedBasePath(String path) {
    definitionMap.putString(AU_EXPECTED_PATH, path);
  }

  public String getAuExpectedBasePath() {
    return definitionMap.getString(AU_EXPECTED_PATH, null);
  }

  public void removeAuExpectedBasePath() {
    definitionMap.removeMapElement(AU_EXPECTED_PATH);
  }

  public void setNewContentCrawlIntv(long crawlIntv) {
    definitionMap.putLong(AU_NEWCONTENT_CRAWL, crawlIntv);
  }

  public long getNewContentCrawlIntv() {
    return definitionMap.getLong(AU_NEWCONTENT_CRAWL,
         DefinableArchivalUnit.DEFAULT_NEW_CONTENT_CRAWL_INTERVAL);
  }

 public  void removeNewContentCrawlIntv() {
    definitionMap.removeMapElement(AU_NEWCONTENT_CRAWL);
  }

  public void setAuCrawlDepth(int depth) {
    definitionMap.putInt(AU_CRAWL_DEPTH, depth);
  }

  public int getAuCrawlDepth() {
    return definitionMap.getInt(AU_CRAWL_DEPTH,
                                DefinableArchivalUnit.DEFAULT_AU_CRAWL_DEPTH);
  }

  public void removeAuCrawlDepth() {
    definitionMap.removeMapElement(AU_CRAWL_DEPTH);
  }

  public void setAuPauseTime(long pausetime) {
    definitionMap.putLong(AU_PAUSE_TIME, pausetime);
  }

  public long getAuPauseTime() {
    return definitionMap.getLong(AU_PAUSE_TIME,
            DefinableArchivalUnit.DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS);
  }

  public void removeAuPauseTime() {
    definitionMap.removeMapElement(AU_PAUSE_TIME);
  }

  public void setAuManifestPage(String manifest) {
    definitionMap.putString(AU_MANIFEST, manifest);
  }

  public String getAuManifestPage() {
    return definitionMap.getString(AU_MANIFEST, getAuStartUrl());
  }

  public void removeAuManifestPage() {
    definitionMap.removeMapElement(AU_MANIFEST);
  }

  public void setPluginName(String name) {
    definitionMap.putString(PLUGIN_NAME, name);
  }

  public String getPluginName() {
    return definitionMap.getString(PLUGIN_NAME, "UNKNOWN");
  }

  public void removePluginName() {
    definitionMap.removeMapElement(PLUGIN_NAME);
  }

  public void setPluginVersion(String version) {
    definitionMap.putString(PLUGIN_VERSION, version);
  }

  public String getPluginVersion() {
    return definitionMap.getString(PLUGIN_VERSION,
                                   DefinablePlugin.DEFAULT_PLUGIN_VERSION);
  }

  public void removePluginVersion() {
    definitionMap.removeMapElement(PLUGIN_VERSION);
  }

  public void setPluginConfigDescrs(HashSet descrs) {
    List descrlist = ListUtil.fromArray(descrs.toArray());

    definitionMap.putCollection(PLUGIN_PROPS, descrlist);
  }

  public HashSet getPluginConfigDescrs() {
    return (HashSet)SetUtil.fromList((List)definitionMap.getCollection(
      PLUGIN_PROPS, Collections.EMPTY_LIST));
  }

  public HashMap getPrintfDescrs() {
    HashSet pcd_set = getPluginConfigDescrs();
    HashMap pd_map = new HashMap(pcd_set.size());

    for(Iterator it = pcd_set.iterator(); it.hasNext();) {
      ConfigParamDescr cpd = (ConfigParamDescr) it.next();
      String key = cpd.getKey();
      Integer type = new Integer(cpd.getType());
      pd_map.put(key, type);
      if (type.intValue() == ConfigParamDescr.TYPE_YEAR) {
        pd_map.put(DefinableArchivalUnit.CM_AU_SHORT_YEAR_PREFIX + key, type);
      }
      else if (type.intValue() == ConfigParamDescr.TYPE_URL) {
        pd_map.put(key + DefinableArchivalUnit.CM_AU_HOST_SUFFIX, type);
        pd_map.put(key + DefinableArchivalUnit.CM_AU_PATH_SUFFIX, type);
      }
    }
    return pd_map;
  }

  public void removePluginConfigDescrs() {
    definitionMap.removeMapElement(PLUGIN_PROPS);
  }

  public void setPluginExceptionHandler(String handler) {
    try {
      definitionMap.putString(PLUGIN_EXCEPTION_HANDLER, handler);
      CacheResultHandler obj =
          (CacheResultHandler) Class.forName(handler).newInstance();
    }
    catch (Exception ex) {
      throw new DefinablePlugin.InvalidDefinitionException(
          "Unable to create exception handler " + handler, ex);
    }

  }

  public String getPluginExceptionHandler() {
    return definitionMap.getString(PLUGIN_EXCEPTION_HANDLER, null);
  }

  public void removePluginExceptionHandler() {
    definitionMap.removeMapElement(PLUGIN_EXCEPTION_HANDLER);
  }

  public void addSingleExceptionHandler(int resultCode, String exceptionClass) {
    List xlist = (List) definitionMap.getCollection(CM_EXCEPTION_LIST_KEY, null);
    if (xlist == null) {
      xlist = new ArrayList();
      definitionMap.putCollection(CM_EXCEPTION_LIST_KEY, xlist);
    }
    else {
      // we need to remove any previously assigned value.
      removeSingleExceptionHandler(resultCode);
    }
    // add the new entry...
    String entry = String.valueOf(resultCode) + "=" + exceptionClass;
    xlist.add(entry);
  }

  public HashMap getSingleExceptionHandlers() {
    HashMap handlers = new HashMap();
    List xlist = (List) definitionMap.getCollection(CM_EXCEPTION_LIST_KEY, null);
    if(xlist != null) {
      for(Iterator it = xlist.iterator(); it.hasNext();) {
        String  entry = (String) it.next();
        Vector s_vec = StringUtil.breakAt(entry, '=', 2, true, true);
        handlers.put((String)s_vec.get(0), (String) s_vec.get(1));
      }
    }
    return handlers;
  }

  public void removeSingleExceptionHandler(int resultCode) {
    List xlist = (List) definitionMap.getCollection(CM_EXCEPTION_LIST_KEY, null);
    if (xlist == null)return;

    for (Iterator it = xlist.iterator(); it.hasNext(); ) {
      String entry = (String) it.next();
      Vector s_vec = StringUtil.breakAt(entry, '=', 2, true, true);
      int code = Integer.parseInt( ( (String) s_vec.get(0)));
      if (code == resultCode) {
        it.remove();
        break;
      }
    }
    // if this was the last entry we remove the item from the definition map
    if (xlist.size() < 1) {
      definitionMap.removeMapElement(CM_EXCEPTION_LIST_KEY);
    }
  }

  public void addConfigParamDescr(ConfigParamDescr descr) {
    List descrlist = (List) definitionMap.getCollection(PLUGIN_PROPS, null);
    if (descrlist == null) {
      descrlist = new ArrayList();
      definitionMap.putCollection(PLUGIN_PROPS, descrlist);
    }
    else {
      removeConfigParamDescr(descr.getKey());
    }
    descrlist.add(descr);
  }

  public void addConfigParamDescr(String key) {
    Collection knownDescrs = getKnownConfigParamDescrs();
    for(Iterator it = knownDescrs.iterator(); it.hasNext();) {
      ConfigParamDescr cpd = (ConfigParamDescr) it.next();
      if(cpd.getKey() == key) {
        addConfigParamDescr(cpd);
      }
    }
  }

  public ConfigParamDescr getConfigParamDescr(String key) {
    Collection knownDescrs = getKnownConfigParamDescrs();
    for(Iterator it = knownDescrs.iterator(); it.hasNext();) {
      ConfigParamDescr cpd = (ConfigParamDescr) it.next();
      if(cpd.getKey() == key) {
        return cpd;
      }
    }
    return null;
  }

  public Collection getConfigParamDescrs() {
    List descrlist = (List) definitionMap.getCollection(PLUGIN_PROPS,
        Collections.EMPTY_LIST);
    return SetUtil.fromList(descrlist);
  }

  public void removeConfigParamDescr(String key) {
    List descrlist = (List) definitionMap.getCollection(PLUGIN_PROPS, null);
    if (descrlist == null)return;

    for (Iterator it = descrlist.iterator(); it.hasNext(); ) {
      ConfigParamDescr cpd = (ConfigParamDescr) it.next();
      if (cpd.getKey().equals(key)) {
        it.remove();
        break;
      }
    }
  }

  // list utils
  Collection getKnownCacheExceptions() {
    HashSet exceptions = new HashSet();
    // always add cache success
    exceptions.add("org.lockss.util.urlconn.CacheSuccess");
    Class ce = CacheException.class;
    Class[] ce_classes = ce.getDeclaredClasses();
    for (int ic = 0; ic < ce_classes.length; ic++) {
      try {
        if (ce_classes[ic].newInstance() instanceof CacheException) {
          exceptions.add(ce_classes[ic].getName());
        }
      }
      catch (IllegalAccessException ex) {
      }
      catch (InstantiationException ex) {
      }
    }
    return exceptions;
  }

  public Collection getKnownConfigParamDescrs() {
    Collection descrs = new HashSet(getDefaultConfigParamDescrs().values());
    addUserDefinedConfigParamDescrs(descrs);
    return descrs;
  }


  static Map getDefaultConfigParamDescrs() {
    HashMap descrs = new HashMap();
    Class cpd = ConfigParamDescr.class;
    ConfigParamDescr descrObj = new ConfigParamDescr();
    Field[] cpd_fields = cpd.getDeclaredFields();
    for (int ic = 0; ic < cpd_fields.length; ic++) {
      if (cpd_fields[ic].getType() == cpd) {
        try {
          ConfigParamDescr descr = (ConfigParamDescr) cpd_fields[ic].get(descrObj);
          descrs.put(descr.getKey(), descr);
        }
        catch (IllegalAccessException ex) {
        }
        catch (IllegalArgumentException ex) {
        }
      }
    }
    return Collections.unmodifiableMap(descrs);
  }


  void addUserDefinedConfigParamDescrs(Collection descrs) {
    List descrlist = (List) definitionMap.getCollection(PLUGIN_PROPS, null);
    if (descrlist != null) {
      for (Iterator it = descrlist.iterator(); it.hasNext(); ) {
        ConfigParamDescr cpd = (ConfigParamDescr) it.next();
        descrs.add(cpd);
      }
    }
  }

  // validators
  boolean isValidPrintf(String format, String[] strs) {
    ArrayList args = new ArrayList();
    try {
      for (int i = 0; i < strs.length; i++) {
        Object val = definitionMap.getMapElement(strs[i]);
        args.add(val);
      }
      PrintfFormat pf = new PrintfFormat(format);
      return pf.sprintf(args.toArray()) != null;
    } catch (Exception ex) {
      return false;
    }
  }

  // fetching the map
  ExternalizableMap getMap() {
    return definitionMap;
  }

}
