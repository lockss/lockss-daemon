/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.config.*;
import org.lockss.state.*;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.plugin.wrapper.CacheResultHandlerWrapper;

/** Base class for plugin status accessors, and static register/unregister
 */
public class PluginStatus {
  static Logger log = Logger.getLogger("PluginStatus");
  final static String PLUGIN_TABLE = "Plugins";
  final static String PLUGIN_DETAIL = "PluginDetail";
  final static String ALL_AUIDS = "AllAuids";
  final static String HTTP_RESULT_MAP = "HttpResultMap";

  /** If true the definition of definable plugins will be displayed along
   * with its details. */
  static final String PARAM_PLUGIN_SHOWDEF =
    Configuration.PREFIX + "plugin.showDef";
  static final boolean DEFAULT_PLUGIN_DHOWDEF = false;

  LockssDaemon daemon;
  PluginManager mgr;

  static void register(LockssDaemon daemon, PluginManager mgr) {
    StatusService statusServ = daemon.getStatusService();
    statusServ.registerStatusAccessor(PLUGIN_TABLE,
				      new Plugins(daemon, mgr));
    statusServ.registerStatusAccessor(PLUGIN_DETAIL,
				      new PluginDetail(daemon, mgr));
    statusServ.registerStatusAccessor(ALL_AUIDS,
				      new AllAuids(daemon, mgr));
    statusServ.registerStatusAccessor(HTTP_RESULT_MAP,
				      new HTTPResultMapping(daemon, mgr));
  }

  static void unregister(LockssDaemon daemon) {
    StatusService statusServ = daemon.getStatusService();
    statusServ.unregisterStatusAccessor(PLUGIN_TABLE);
    statusServ.unregisterStatusAccessor(PLUGIN_DETAIL);
    statusServ.unregisterStatusAccessor(ALL_AUIDS);
    statusServ.unregisterStatusAccessor(HTTP_RESULT_MAP);
  }

  PluginStatus(LockssDaemon daemon, PluginManager mgr) {
    this.daemon = daemon;
    this.mgr = mgr;
  }

  // utility method for making a Reference
  public static StatusTable.Reference makePlugRef(Object value,
						  Plugin plug) {
    String key = PluginManager.pluginKeyFromId(plug.getPluginId());
    return new StatusTable.Reference(value, PLUGIN_DETAIL, key);
  }

  public static StatusTable.Reference makeResultMapRef(Object value,
                                                       Plugin plug) {
    String key = PluginManager.pluginKeyFromId(plug.getPluginId());
    return new StatusTable.Reference(value, HTTP_RESULT_MAP, key);
  }
}

/**
 * Plugin summary.  For all plugins, lists name, version, id, and URL (if
 * loadable)
 */
class Plugins extends PluginStatus implements StatusAccessor {

  private final List sortRules =
    ListUtil.list(new StatusTable.SortRule("plugin",
					   CatalogueOrderComparator.SINGLETON));

  private final List colDescs =
    ListUtil.list(
		  new ColumnDescriptor("plugin", "Name",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("version", "Version",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("aus", "# AUs",
				       ColumnDescriptor.TYPE_INT),
		  new ColumnDescriptor("type", "Type",
				       ColumnDescriptor.TYPE_STRING),
// 		  new ColumnDescriptor("id", "Plugin ID",
// 				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("registry", "Registry",
				       ColumnDescriptor.TYPE_STRING)
// 		  new ColumnDescriptor("cu", "Loaded From",
// 				       ColumnDescriptor.TYPE_STRING)
		  );

  Plugins(LockssDaemon daemon, PluginManager mgr) {
    super(daemon, mgr);
  }

  public String getDisplayName() {
    return "Publisher Plugins";
  }

  public boolean requiresKey() {
    return false;
  }

  public void populateTable(StatusTable table) {
    table.setColumnDescriptors(colDescs);
    table.setDefaultSortRules(sortRules);
    table.setRows(getRows(table.getOptions().get(StatusTable.OPTION_DEBUG_USER)));
  }

  public List getRows(boolean includeInternalAus) {
    List rows = new ArrayList();

    Collection plugins = mgr.getRegisteredPlugins();
    synchronized (plugins) {
      for (Iterator iter = plugins.iterator(); iter.hasNext(); ) {
	Plugin plugin = (Plugin)iter.next();
	if (!includeInternalAus && mgr.isInternalPlugin(plugin)) {
	  continue;
	}
	Map row = new HashMap();
	row.put("plugin", makePlugRef(plugin.getPluginName(), plugin));

	int numaus = plugin.getAllAus().size();
	if (numaus > 0) {
	  StatusTable.Reference auslink = 
	    new StatusTable.Reference(numaus,
				      ArchivalUnitStatus.SERVICE_STATUS_TABLE_NAME,
				      "plugin:" + plugin.getPluginId());
	  row.put("aus", auslink);
	}
	row.put("version", plugin.getVersion());
	row.put("id", plugin.getPluginId());
	row.put("type", mgr.getPluginType(plugin));
	if (mgr.isLoadablePlugin(plugin)) {
	  PluginManager.PluginInfo info = mgr.getLoadablePluginInfo(plugin);
	  if (info != null) {
// 	    row.put("cu", info.getCuUrl());
	    ArchivalUnit au = info.getRegistryAu();
	    if (au != null) {
	      row.put("registry", au.getName());
	    }
	  }
	}
	rows.add(row);
      }
    }
    return rows;
  }
}

/**
 * Details of single plugin
 */
class PluginDetail extends PluginStatus implements StatusAccessor {

  private final List sortRules =
    ListUtil.list(new StatusTable.SortRule("key", true));

  private final List colDescs =
    ListUtil.list(
		  new ColumnDescriptor("key", "Key",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("val", "Val",
				       ColumnDescriptor.TYPE_STRING)
		  );

  PluginDetail(LockssDaemon daemon, PluginManager mgr) {
    super(daemon, mgr);
  }

  public String getDisplayName() {
    return "Plugin Details";
  }

  private String getTitle(Plugin plug) {
    return "Plugin " + plug.getPluginName();
  }

  public boolean requiresKey() {
    return true;
  }

  public void populateTable(StatusTable table)
      throws StatusService.NoSuchTableException {
    String key = table.getKey();
    Plugin plug = mgr.getPlugin(key);
    if (plug == null) {
      throw new StatusService.NoSuchTableException("Unknown plugin: " + key);
    }
    populateTable(table, plug);
  }

  public void populateTable(StatusTable table, Plugin plug) {
    table.setTitle(getTitle(plug));
    table.setDefaultSortRules(sortRules);
    ExternalizableMap plugDef = null;
    boolean enableShowdef = false;
    if (plug instanceof DefinablePlugin) {
      DefinablePlugin dplug = (DefinablePlugin)plug;
      plugDef = dplug.getDefinitionMap();
      Properties tprops = table.getProperties();
      if ((tprops != null &&
	  !StringUtil.isNullString(tprops.getProperty("showdef")))) {
	table.setColumnDescriptors(colDescs);
	table.setRows(getRows(dplug, plugDef));
      } else {
	enableShowdef =
	  table.getOptions().get(StatusTable.OPTION_DEBUG_USER) ||
	  CurrentConfig.getBooleanParam(PARAM_PLUGIN_SHOWDEF,
					DEFAULT_PLUGIN_DHOWDEF);
      }
    }
    table.setSummaryInfo(getSummaryInfo(plug, plugDef, enableShowdef));
  }

  public List getRows(DefinablePlugin plug, ExternalizableMap plugDef) {
    List rows = new ArrayList();
    for (Iterator iter = plugDef.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry entry = (Map.Entry)iter.next();
      String key = (String)entry.getKey();
      String val = entry.getValue().toString();
      Map row = new HashMap();
      row.put("key", key);
      row.put("val", val);
      rows.add(row);
    }
    return rows;
  }

  private List getSummaryInfo(Plugin plug, ExternalizableMap plugDef,
			      boolean showDefLink) {
    List res = new ArrayList();
    res.add(new StatusTable.SummaryInfo("Name",
					ColumnDescriptor.TYPE_STRING,
					plug.getPluginName()));

    res.add(new StatusTable.SummaryInfo("Id",
					ColumnDescriptor.TYPE_STRING,
					plug.getPluginId()));

    res.add(new StatusTable.SummaryInfo("Version",
					ColumnDescriptor.TYPE_STRING,
					plug.getVersion()));

    if (plugDef != null) {
      String notes = plugDef.getString(DefinablePlugin.KEY_PLUGIN_NOTES, null);
      if (notes != null) {
	res.add(new StatusTable.SummaryInfo("Notes",
					    ColumnDescriptor.TYPE_STRING,
					    notes));
      }
    }
    res.add(new StatusTable.SummaryInfo("Type",
					ColumnDescriptor.TYPE_STRING,
					mgr.getPluginType(plug)));
    StatusTable.Reference auslink = 
      new StatusTable.Reference(plug.getAllAus().size(),
				ArchivalUnitStatus.SERVICE_STATUS_TABLE_NAME,
				"plugin:" + plug.getPluginId());
    res.add(new StatusTable.SummaryInfo("# AUs",
					ColumnDescriptor.TYPE_STRING,
					auslink));
    if (mgr.isLoadablePlugin(plug)) {
      PluginManager.PluginInfo info = mgr.getLoadablePluginInfo(plug);
      if (info != null) {
	String url = info.getCuUrl();
	if (url != null) {
	  ArchivalUnit au = info.getRegistryAu();
	  res.add(new StatusTable.SummaryInfo("Plugin Registry",
					      ColumnDescriptor.TYPE_STRING,
					      au.getName()));
	  res.add(new StatusTable.SummaryInfo("URL",
					      ColumnDescriptor.TYPE_STRING,
					      url));
// 	  res.add(new StatusTable.SummaryInfo("Loaded from",
// 					      ColumnDescriptor.TYPE_STRING,
// 					      info.getJarUrl()));
	}
      }
    }
    if (plugDef != null) {
      if (showDefLink) {
        StatusTable.Reference deflink = makePlugRef("Definition", plug);
        deflink.setProperty("showdef", "1");
        res.add(new StatusTable.SummaryInfo(null,
                                            ColumnDescriptor.TYPE_STRING,
                                            deflink));
      }
      StatusTable.Reference resMapLink = makeResultMapRef("Result Map", plug);
      res.add(new StatusTable.SummaryInfo(null,
                                          ColumnDescriptor.TYPE_STRING,
                                          resMapLink));
    }
    return res;
  }

}

/**
 * Plugin's HTTP result mapping
 */
class HTTPResultMapping extends PluginStatus implements StatusAccessor {

  private final List sortRules =
    ListUtil.list(new StatusTable.SortRule("trigger", true));

  private final List colDescs =
    ListUtil.list(
		  new ColumnDescriptor("trigger", "Fetch Result",
				       ColumnDescriptor.TYPE_STRING,
                                       "The HTTP response status that triggers this action. Bold items are Plugin customizations."),
		  new ColumnDescriptor("retries", "Retries",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("action", "Action",
				       ColumnDescriptor.TYPE_STRING,
                                       "The action the crawler takes when this error is signalled. Abort: the crawl fails immediately. Fail: the crawl continues, but will be marked unsuccessful when it finishes. (blank): no error is signaled and the crawl continues."));

  HTTPResultMapping(LockssDaemon daemon, PluginManager mgr) {
    super(daemon, mgr);
  }

  public String getDisplayName() {
    return "Fetch Result Handling";
  }

  private String getTitle(Plugin plug) {
    return "Fetch Result Handling of plugin " + plug.getPluginName();
  }

  public boolean requiresKey() {
    return true;
  }

  public void populateTable(StatusTable table)
      throws StatusService.NoSuchTableException {
    String key = table.getKey();
    Plugin plug = mgr.getPlugin(key);
    if (plug == null) {
      throw new StatusService.NoSuchTableException("Unknown plugin: " + key);
    }
    populateTable(table, plug);
  }

  public void populateTable(StatusTable table, Plugin plug)
      throws StatusService.NoSuchTableException {
    table.setTitle(getTitle(plug));
    table.setDefaultSortRules(sortRules);
    CacheResultMap resultMap = plug.getCacheResultMap();
    if (! (resultMap instanceof HttpResultMap)) {
      throw new StatusService.NoSuchTableException("CacheResultMap in "
                                                   + plug.getPluginName()
                                                   + " is not inspectable");
    }
    HttpResultMap hrMap = (HttpResultMap)resultMap;
    HttpResultMap defaultMap = new HttpResultMap();
    table.setColumnDescriptors(colDescs);
    try {
      table.setRows(getRows(hrMap, defaultMap));
    } catch (Exception e) {
      throw new StatusService.NoSuchTableException(e.getMessage());
    }
  }

  public List getRows(HttpResultMap hrMap, HttpResultMap defaultResMap)
      throws Exception {
    List rows = new ArrayList();
    Map<Object,ResultAction> defMap = defaultResMap.getExceptionMap();
    for (Map.Entry<Object,ResultAction> ent
           : hrMap.getExceptionMap().entrySet()) {
      Object lhs = ent.getKey();
      ResultAction ei = ent.getValue();
      Map row = new HashMap();
      StatusTable.DisplayedValue trigger =
        new StatusTable.DisplayedValue(shortName(lhs));
      if (ei == null) {
      row.put("trigger", trigger);
        row.put("action", "Missing");
        continue;
      }
      if (!ei.equals(defMap.get(lhs))) {
        trigger.setBold(true);
      }
      if (!shortName(lhs).equals(lhs.toString())) {
        trigger.setHoverText(lhs.toString());
      }
      row.put("trigger", trigger);
      switch (ei.getType()) {
      case Class:
        Class ex = ei.getExceptionClass();
	CacheException cex = (CacheException)ex.newInstance();
        if (cex.isAttributeSet(CacheException.ATTRIBUTE_RETRY)) {
          row.put("retries", cex.getRetryCount() + " @ " +
                  StringUtil.timeIntervalToString(cex.getRetryDelay()));
        }
        if (cex.isAttributeSet(CacheException.ATTRIBUTE_FATAL)) {
          row.put("action", "Abort");
        } else if (cex.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
          row.put("action", "Fail");
        }
        break;
      case Handler:
        CacheResultHandler crh = ei.getHandler();
        if (crh instanceof CacheResultHandlerWrapper) {
          crh = (CacheResultHandler)((CacheResultHandlerWrapper)crh).getWrappedObj();
        }
        StatusTable.DisplayedValue action =
          new StatusTable.DisplayedValue(shortName(crh.getClass()));
        action.setHoverText(crh.getClass().toString());

        row.put("action", action);
        break;
      }
      row.put(StatusTable.ROW_SEPARATOR, "");
      rows.add(row);
    }
    return rows;
  }

  Object shortName(Object o) {
    return StringUtil.shortName(o);
  }

}

/**
 * List of AUID of all currently defined titles.
 */
class AllAuids extends PluginStatus implements StatusAccessor.DebugOnly {

  private final List colDescs =
    ListUtil.list(
		  new ColumnDescriptor("name", "Name",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("auid", "AUID",
				       ColumnDescriptor.TYPE_STRING)
		  );

  private final List sortRules =
    ListUtil.list(new StatusTable.SortRule("name",
					   CatalogueOrderComparator.SINGLETON));

  AllAuids(LockssDaemon daemon, PluginManager mgr) {
    super(daemon, mgr);
  }

  public String getDisplayName() {
    return "All Title AUIDs";
  }

  public boolean requiresKey() {
    return false;
  }

  public void populateTable(StatusTable table) {
    table.setColumnDescriptors(colDescs);
    table.setDefaultSortRules(sortRules);
    table.setRows(getRows(table.getOptions().get(StatusTable.OPTION_DEBUG_USER)));
  }

  public List getRows(boolean isDebug) {
    PluginManager pmgr = daemon.getPluginManager();
    List<TitleConfig> tcs = pmgr.findAllTitleConfigs();
    List rows = new ArrayList();
    for (TitleConfig tc : tcs) {
      Map row = new HashMap();
      try {
	row.put("auid", tc.getAuId(pmgr));
      } catch (RuntimeException e) {
	row.put("auid", "(Can't compute AUID)");
      }
      row.put("name", tc.getDisplayName());
      rows.add(row);
    }
    return rows;
  }
}
