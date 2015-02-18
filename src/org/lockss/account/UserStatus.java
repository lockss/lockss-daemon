/*
 * $Id$
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.account;

import java.util.*;
import org.lockss.app.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.servlet.*;

/** User status table */
public class UserStatus implements StatusAccessor {
  private static final Logger log = Logger.getLogger(UserStatus.class);

  final static String USER_STATUS_TABLE = "UserStatus";

//   public static final String PREFIX = Configuration.PREFIX + ".userStatus";

//   /** Truncate displayed values to this length */
//   static final String PARAM_MAX_DISPLAY_VAL_LEN = PREFIX + "maxDisplayValLen";
//   static final int DEFAULT_MAX_DISPLAY_VAL_LEN = 1000;

  private AdminServletManager adminMgr = null;

  public UserStatus(AdminServletManager adminMgr) {
    this.adminMgr = adminMgr;
  }

  // name, login time, idle time,

  private final List colDescs =
    ListUtil.list(new ColumnDescriptor("name", "Name",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("login", "Login",
				       ColumnDescriptor.TYPE_DATE),
		  new ColumnDescriptor("idle", "Idle",
				       ColumnDescriptor.TYPE_TIME_INTERVAL),
		  new ColumnDescriptor("running", "Running",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("reqhost", "From",
				       ColumnDescriptor.TYPE_STRING)
		  );

  public String getDisplayName() {
    return "Current Users";
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

    for (UserSession sess : adminMgr.getUserSessions()) {
      Map row = new HashMap();
      row.put("name", sess.getName());
      row.put("login", sess.getLoginTime());
      long idle = sess.getIdleTime();
      if (idle < Constants.SECOND) {
	// Current user will show a few ms idle time
	idle = 0;
      }
      row.put("idle", idle);
      String running = sess.getRunningServlet();
      if (!StringUtil.isNullString(running)) {
	row.put("running", running);
      }
      String reqHost = sess.getReqHost();
      if (!StringUtil.isNullString(reqHost)) {
	row.put("reqhost", reqHost);
      }
      rows.add(row);
    }
    return rows;
  }
    
}
