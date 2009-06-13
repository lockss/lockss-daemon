/*
 * $Id: ConfigStatus.java,v 1.2.10.1 2009-06-13 08:52:22 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.app.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;

/** Config status table */
public class ConfigStatus extends BaseLockssDaemonManager {
  static Logger log = Logger.getLogger("ConfigStatus");

  final static String CONFIG_STATUS_TABLE = "ConfigStatus";

  public static final String PREFIX = Configuration.PREFIX + ".configStatus";

  /** Truncate displayed values to this length */
  static final String PARAM_MAX_DISPLAY_VAL_LEN = PREFIX + "maxDisplayValLen";
  static final int DEFAULT_MAX_DISPLAY_VAL_LEN = 1000;

  final static String PARAM_AU_TREE_DOT = PluginManager.PARAM_AU_TREE + ".";

  public ConfigStatus() {
  }

  public void startService() {
    super.startService();
    StatusService statusServ = getDaemon().getStatusService();
    statusServ.registerStatusAccessor(CONFIG_STATUS_TABLE, new Status());
  }

  public void stopService() {
    StatusService statusServ = getDaemon().getStatusService();
    statusServ.unregisterStatusAccessor(CONFIG_STATUS_TABLE);
  }

  static class Status implements StatusAccessor.DebugOnly {

    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("name", "Name",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor("value", "Value",
					 ColumnDescriptor.TYPE_STRING)
		    );

    public String getDisplayName() {
      return "Configuration";
    }

    public boolean requiresKey() {
      return false;
    }

    public void populateTable(StatusTable table) {
      table.setColumnDescriptors(colDescs);
      table.setRows(getRows(table.getOptions()));
    }

    public List getRows(BitSet options) {
      List rows = new ArrayList();

      Configuration config = ConfigManager.getCurrentConfig();
      int maxLen = config.getInt(PARAM_MAX_DISPLAY_VAL_LEN,
				 DEFAULT_MAX_DISPLAY_VAL_LEN);
      for (Iterator iter = config.keySet().iterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
	if (!excludeKey(key)) {
	  Map row = new HashMap();
	  row.put("name", key);
	  row.put("value",
		  StringUtil.elideMiddleToMaxLen(config.get(key), maxLen));
	  rows.add(row);
	}
      }
      return rows;
    }
    
    boolean excludeKey(String key) {
      return key.startsWith(ConfigManager.PARAM_TITLE_DB)
	|| key.startsWith(PARAM_AU_TREE_DOT)
	|| key.indexOf(".password") >= 0;
    }
  }
}

