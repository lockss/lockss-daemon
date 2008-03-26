/*
 * $Id: PlatformConfigStatus.java,v 1.3 2008-03-26 04:50:07 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import static org.lockss.config.ConfigManager.*;

/** Display Platform Configuration */
public class PlatformConfigStatus extends BaseLockssDaemonManager {
  static Logger log = Logger.getLogger("PlatformConfigStatus");
  final static String PLATFORM_STATUS_TABLE = "PlatformStatus";

  public PlatformConfigStatus() {
  }

  public void startService() {
    super.startService();
    StatusService statusServ = getDaemon().getStatusService();
    statusServ.registerStatusAccessor(PLATFORM_STATUS_TABLE,
				      new PCStatus(getDaemon()));
  }

  public void stopService() {
    StatusService statusServ = getDaemon().getStatusService();
    statusServ.unregisterStatusAccessor(PLATFORM_STATUS_TABLE);
  }

  static class PCStatus implements StatusAccessor {

    private LockssDaemon daemon;

    PCStatus(LockssDaemon daemon) {
      this.daemon = daemon;
    }

    public String getDisplayName() {
      return "Platform Configuration";
    }

    public boolean requiresKey() {
      return false;
    }

    public void populateTable(StatusTable table) {
      Configuration config = ConfigManager.getCurrentConfig();
      table.setSummaryInfo(getSummaryInfo(config));
    }


    void addSum(List lst, String head, String val) {
      if (val != null) {
	lst.add(new StatusTable.SummaryInfo(head,
					    ColumnDescriptor.TYPE_STRING,
					    val));
      }
    }

    String seplist(Collection c) {
      return StringUtil.separatedString(c, ", ");
    }

    private List getSummaryInfo(Configuration config) {
      List res = new ArrayList();
      addSum(res, "Hostname", config.get(PARAM_PLATFORM_FQDN));
      addSum(res, "IP Address", config.get(PARAM_PLATFORM_IP_ADDRESS));
      if (daemon.isClockss()) {
	addSum(res, "IP Address",
	       config.get(PARAM_PLATFORM_SECOND_IP_ADDRESS));
      }
      List<String> groups = config.getPlatformGroupList();
      if (groups != null) {
	if (groups.size() == 1) {
	  addSum(res, "Group", groups.get(0));
	} else {
	  addSum(res, "Groups", seplist(groups));
	}
      }
      addSum(res, "Project", config.get(PARAM_PLATFORM_PROJECT));
      addSum(res, "V3 Identity", config.get(PARAM_PLATFORM_LOCAL_V3_IDENTITY));
      String smtpHost = config.get(PARAM_PLATFORM_SMTP_HOST);
      if (smtpHost != null) {
	int smtpPort =
	  config.getInt(PARAM_PLATFORM_SMTP_PORT,
			org.lockss.mail.SmtpMailService.DEFAULT_SMTPPORT);
	addSum(res, "Mail Relay", smtpHost + ":" + smtpPort);
      }
      addSum(res, "Admin Email", config.get(PARAM_PLATFORM_ADMIN_EMAIL));
      addSum(res, "Disks",
	     seplist(config.getList(PARAM_PLATFORM_DISK_SPACE_LIST)));
      addSum(res, "Cwd",
	     PlatformUtil.getInstance().getCwd());

      return res;
    }
    
  }
}
