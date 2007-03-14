/*
 * $Id: IdentityManagerStatus.java,v 1.3 2007-03-14 05:53:41 tlipkis Exp $
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
package org.lockss.protocol;
import java.util.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;

public class IdentityManagerStatus
  implements StatusAccessor,  StatusAccessor.DebugOnly {

  private Map theIdentities;

  public IdentityManagerStatus(Map theIdentities) {
    this.theIdentities = theIdentities;
  }

  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule("ip", true));

  private static final List statusColDescs =
    ListUtil.list(
		  new ColumnDescriptor("ip", "IP",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("lastPkt", "Last Pkt",
				       ColumnDescriptor.TYPE_DATE,
				       "Last time a packet that originated " +
				       "at IP was received"),
		  new ColumnDescriptor("lastOp", "Last Op",
				       ColumnDescriptor.TYPE_DATE,
				       "Last time a non-NoOp packet that " +
				       "originated at IP was received"),
		  new ColumnDescriptor("origTot", "Orig Tot",
				       ColumnDescriptor.TYPE_INT,
				       "Total packets received that " +
				       "originated at IP."),
		  new ColumnDescriptor("origOp", "Orig Op",
				       ColumnDescriptor.TYPE_INT,
				       "Total non-noop packets received that "+
				       "originated at IP."),
		  new ColumnDescriptor("sendOrig", "1 Hop",
				       ColumnDescriptor.TYPE_INT,
				       "Packets arriving from originator " +
				       "in one hop."),
		  new ColumnDescriptor("sendFwd", "Fwd",
				       ColumnDescriptor.TYPE_INT,
				       "Packets forwarded by IP to us."),
		  new ColumnDescriptor("dup", "Dup",
				       ColumnDescriptor.TYPE_INT,
				       "Duplicate packets received from IP."),
		  new ColumnDescriptor("reputation", "Reputation",
				       ColumnDescriptor.TYPE_INT)
		  );

  public String getDisplayName() {
    return "Cache Identities";
  }

  public void populateTable(StatusTable table) {
    String key = table.getKey();
    table.setColumnDescriptors(statusColDescs);
    table.setDefaultSortRules(statusSortRules);
    table.setRows(getRows(key));
    //       table.setSummaryInfo(getSummaryInfo(key));
  }

  public boolean requiresKey() {
    return false;
  }

  private List getRows(String key) {
    List table = new ArrayList();
    for (Iterator iter = theIdentities.values().iterator();
	 iter.hasNext();) {
      table.add(makeRow((LcapIdentity)iter.next()));
    }
    return table;
  }

  private Map makeRow(LcapIdentity id) {
    Map row = new HashMap();
    PeerIdentity pid = id.getPeerIdentity();
    if (pid.isLocalIdentity()) {
      StatusTable.DisplayedValue val =
	new StatusTable.DisplayedValue(pid.getIdString());
      val.setBold(true);
      row.put("ip", val);
    } else {
      row.put("ip", pid.getIdString());
    }
    row.put("lastPkt", new Long(id.getLastActiveTime()));
    row.put("lastOp", new Long(id.getLastOpTime()));
    row.put("origTot", new Long(id.getEventCount(LcapIdentity.EVENT_ORIG)));
    row.put("origOp",
	    new Long(id.getEventCount(LcapIdentity.EVENT_ORIG_OP)));
    row.put("sendOrig",
	    new Long(id.getEventCount(LcapIdentity.EVENT_SEND_ORIG)));
    row.put("sendFwd",
	    new Long(id.getEventCount(LcapIdentity.EVENT_SEND_FWD)));
    row.put("dup", new Long(id.getEventCount(LcapIdentity.EVENT_DUPLICATE)));
    row.put("reputation", new Long(id.getReputation()));
    return row;
  }

  private List getSummaryInfo(String key) {
    List res = new ArrayList();
    //       res.add(new StatusTable.SummaryInfo("Total bytes hashed",
    // 					  ColumnDescriptor.TYPE_INT,
    // 					  new Integer(0)));
    return res;
  }
}
