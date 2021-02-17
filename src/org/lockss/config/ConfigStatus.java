/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import java.io.*;
import java.util.*;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.lockss.app.*;
import org.lockss.daemon.status.*;
import org.lockss.daemon.status.StatusService.NoSuchTableException;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;

/** Config status table */
public class ConfigStatus extends BaseLockssDaemonManager {
  static Logger log = Logger.getLogger("ConfigStatus");

  final static String CONFIG_STATUS_TABLE = "ConfigStatus";
  final static String CONFIG_FILE_STATUS_TABLE = "ConfigFileStatus";

  public static final String PREFIX = Configuration.PREFIX + "configStatus.";

  /** Truncate displayed values to this length */
  static final String PARAM_MAX_DISPLAY_VAL_LEN = PREFIX + "maxDisplayValLen";
  static final int DEFAULT_MAX_DISPLAY_VAL_LEN = 1000;

  ConfigManager configMgr;

  public ConfigStatus() {
  }

  public void startService() {
    super.startService();
    StatusService statusServ = getDaemon().getStatusService();
    statusServ.registerStatusAccessor(CONFIG_STATUS_TABLE, new Status());
    statusServ.registerStatusAccessor(CONFIG_FILE_STATUS_TABLE, new OneFile());
    configMgr = getDaemon().getConfigManager();
  }

  public void stopService() {
    StatusService statusServ = getDaemon().getStatusService();
    statusServ.unregisterStatusAccessor(CONFIG_STATUS_TABLE);
    statusServ.unregisterStatusAccessor(CONFIG_FILE_STATUS_TABLE);
  }

  /** Base class for status tables displaying a Configuration */
  abstract class BaseStatus implements StatusAccessor.DebugOnly {

    protected final List colDescs =
      ListUtil.list(new ColumnDescriptor("name", "Name",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor("value", "Value",
					 ColumnDescriptor.TYPE_STRING)
		    );

    public abstract String getDisplayName();

    public abstract boolean requiresKey();

    public abstract void populateTable(StatusTable table)
	throws NoSuchTableException;

    protected List getRows(BitSet options, Configuration config) {
      List rows = new ArrayList();

      int maxLen = config.getInt(PARAM_MAX_DISPLAY_VAL_LEN,
				 DEFAULT_MAX_DISPLAY_VAL_LEN);
      for (Iterator iter = config.keySet().iterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
	if (ConfigManager.shouldParamBeLogged(key)) {
	  Map row = new HashMap();
	  row.put("name", key);
	  row.put("value",
		  StringUtil.elideMiddleToMaxLen(config.get(key), maxLen));
	  rows.add(row);
	}
      }
      return rows;
    }
    
    void addSum(List lst, String head, String val) {
      if (val != null) {
	lst.add(new StatusTable.SummaryInfo(head,
					    ColumnDescriptor.TYPE_STRING,
					    val));
      }
    }
  }

  /** Global Configuration status table */
  class Status extends BaseStatus {
    // Map param to sources (URLs) where it's set
    private MultiValuedMap<String,String> paramSources =
      new ArrayListValuedHashMap<String,String>();
    private List<String> sourcesPresent = new ArrayList<>();

    public String getDisplayName() {
      return "Configuration";
    }

    public boolean requiresKey() {
      return false;
    }

    public void populateTable(StatusTable table) {
      table.setColumnDescriptors(colDescs);
      table.setSummaryInfo(getSummaryInfo());
      table.setRows(getRows(table.getOptions()));
      // Set the list of sources so they'll be displayed in load order, not
      // table display order
      table.setOrderedFootnotes(sourcesPresent);
    }

    protected List getRows(BitSet options) {
      Configuration config = ConfigManager.getCurrentConfig();

      // Servlet instances may be reused - ensure these are reset
      paramSources.clear();
      sourcesPresent.clear();

      // Record source(s) of each config param, in order loaded.

      // ConfigFiles are next
      List<String> urls = configMgr.getSpecUrlList();
      if (urls == null || urls.isEmpty()) {
        // fall back to un-annotated list if no config sources known
        return getRows(options, config);
      }
      for (String url : urls) {
	ConfigFile cf = configMgr.getConfigCache().get(url);
	if (cf != null) {
	  try {
	    Configuration cfConfig = cf.getConfiguration();
            recordParamSources(cfConfig, url);
	  } catch (IOException e) {
	    log.warning("Couldn't get config source: " + cf, e);
	  }
        }
      }

      // Build table rows, annotating each param with all its sources,
      // displaying the value in the final config.
      List rows = new ArrayList();
      int maxLen = config.getInt(PARAM_MAX_DISPLAY_VAL_LEN,
				 DEFAULT_MAX_DISPLAY_VAL_LEN);

      for (String key : config.keySet()) {
	if (ConfigManager.shouldParamBeLogged(key)) {
          Object keyObj = key;
	  Map row = new HashMap();
          Collection<String> pSources = paramSources.get(key);
          if (pSources != null) {
            // add footnotes for all sources of this param
            StatusTable.DisplayedValue dv = new StatusTable.DisplayedValue(key);
            for (String source : pSources) {
              dv.addFootnote(source);
            }
            keyObj = dv;
          }
	  row.put("name", keyObj);
	  row.put("value",
		  StringUtil.elideMiddleToMaxLen(config.get(key), maxLen));
	  rows.add(row);
	}
      }
      return rows;
    }

    // Record the source (URL, app spec field) for each param in this
    // section of the config
    private void recordParamSources(Configuration config, String source)  {
      if (config != null && !config.isEmpty()) {
        boolean footnoteThisSource = false;
        for (String key : config.keySet()) {
          if (ConfigManager.shouldParamBeLogged(key)) {
            paramSources.put(key, source);
            footnoteThisSource = false;
          }
        }
        if (footnoteThisSource) {
          sourcesPresent.add(source);
        }
      }
    }

    protected List getSummaryInfo() {
      List res = new ArrayList();
      res.add(new StatusTable.SummaryInfo("Last Reload",
					  ColumnDescriptor.TYPE_DATE,
					  configMgr.getLastUpdateTime()));
      return res;
    }
  }

  /** Individual ConfigFile status table */
  class OneFile extends BaseStatus {

    public String getDisplayName() {
      return "Configuration File:";
    }

    public boolean requiresKey() {
      return true;
    }

    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      if (key != null) {
        if (!key.startsWith("cf:")) {
          throw new StatusService.NoSuchTableException("Unknown selector: "
						       + key);
        }
        String[] foo = org.apache.commons.lang3.StringUtils.split(key, ":", 2);
        if (foo.length < 2 || StringUtil.isNullString(foo[1])) {
          throw new StatusService.NoSuchTableException("Empty config file url: "
						       + key);
        }
        String url = foo[1];
	ConfigFile cf = configMgr.getConfigCache().get(url);
	if (cf != null) {
	  try {
	    Configuration config = cf.getConfiguration();
	    table.setTitle("Config File: " + url);
	    table.setColumnDescriptors(colDescs);
	    table.setSummaryInfo(getSummaryInfo(cf));
	    table.setRows(getRows(table.getOptions(), config));
	  } catch (IOException e) {
	    log.error("Couldn't get config for: " + cf, e);
	    throw new StatusService.NoSuchTableException("Couldn't get config for: " + cf,
							 e);
	  }
	}
      }
    }

    protected List getSummaryInfo(ConfigFile cf) {
      List res = new ArrayList();
      // Disabled code backported from lockss-core that doesn't work here
//       // Compensate for FileConfigFile's numeric Last-Modified headers
//       String last = DateTimeUtil.gmtDateOf(cf.getLastModified());
//       res.add(new StatusTable.SummaryInfo("Last Modified",
// 					  ColumnDescriptor.TYPE_DATE,
// 					  last));
//       if (cf.isLoadedFromFailover()) {
// 	res.add(new StatusTable.SummaryInfo("Loaded from Failover",
// 					    ColumnDescriptor.TYPE_STRING,
// 					    cf.getLoadedUrl()));
// 	ConfigManager.RemoteConfigFailoverInfo rcfi =
// 	  configMgr.getRcfi(cf.getFileUrl());
// 	if (rcfi != null) {
// 	res.add(new StatusTable.SummaryInfo("Failover Created",
// 					    ColumnDescriptor.TYPE_DATE,
// 					    rcfi.getDate()));
// 	}
//       }
      String err = cf.getLoadErrorMessage();
      if (err != null) {
	res.add(new StatusTable.SummaryInfo("Error",
					    ColumnDescriptor.TYPE_STRING,
					    err));
      }

      // Disabled code backported from lockss-core that doesn't work here
//       try {
// 	Tdb tdb = cf.getConfiguration().getTdb();
// 	if (tdb != null && tdb.getTdbAuCount() != 0) {
// 	  res.add(new StatusTable.SummaryInfo("TDB",
// 					      ColumnDescriptor.TYPE_STRING,
// 					      tdb.summaryString()));
// 	}
//       } catch (IOException e) {
// 	log.error("Couldn't get config for: " + cf, e);
//       }
      return res;
    }
  }
}

