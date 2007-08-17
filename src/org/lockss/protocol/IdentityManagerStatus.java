/*
 * $Id: IdentityManagerStatus.java,v 1.6 2007-08-17 07:37:02 smorabito Exp $
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

  private Map<PeerIdentity,PeerIdentityStatus> theIdentities;

  public IdentityManagerStatus(Map<PeerIdentity,PeerIdentityStatus> theIdentities) {
    this.theIdentities = theIdentities;
  }

  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule("ip", true));

  private static final List statusColDescs =
    ListUtil.list(
		  new ColumnDescriptor("ip", "IP",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("lastMessage", "Last Message",
				       ColumnDescriptor.TYPE_DATE,
				       "Last time a message that originated " +
				       "at IP was received."),
		  new ColumnDescriptor("lastOp", "Mesage Type",
				       ColumnDescriptor.TYPE_STRING,
				       "Last message type that " +
				       "originated at IP."),
		  new ColumnDescriptor("origTot", "Messages",
				       ColumnDescriptor.TYPE_INT,
				       "Total messages received that " +
				       "originated at IP."),
		  new ColumnDescriptor("origLastPoller", "Last Poll",
		                       ColumnDescriptor.TYPE_DATE,
		                       "Last time that IP called a poll " +
		                       "in which this cache participated " +
		                       "as a voter."),
                  new ColumnDescriptor("origLastVoter", "Last Vote",
                                       ColumnDescriptor.TYPE_DATE,
                                       "Last time that IP agreed to " +
                                       "participate as a voter in a poll " +
                                       "called by this cache."),
                  new ColumnDescriptor("origLastInvitation", "Last Invitation",
                                       ColumnDescriptor.TYPE_DATE,
                                       "Last time this peer was invited into " +
                                       "a poll."),
                  new ColumnDescriptor("origTotalInvitations", "Invitations",
                                       ColumnDescriptor.TYPE_DATE,
                                       "Total number of invitations sent to " +
                                       "this peer."),
                  new ColumnDescriptor("origPoller", "Polls Called",
                                       ColumnDescriptor.TYPE_INT,
                                       "Total number of polls in which " +
                                       "IP participated as the Poller."),
                  new ColumnDescriptor("origVoter", "Votes Cast",
                                       ColumnDescriptor.TYPE_INT,
                                       "Total number of polls in which " +
                                       "IP participated as a Voter."),
                  new ColumnDescriptor("pollsRejected", "Polls Rejected",
                                       ColumnDescriptor.TYPE_INT,
                                       "Total number of poll requests "
                                       + "rejected by this peer"),
                  new ColumnDescriptor("pollNak", "NAK Reason",
                                       ColumnDescriptor.TYPE_INT,
                                       "Reason for most recent poll request " +
                                       "rejection, if any.")
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
    for (PeerIdentity pid : theIdentities.keySet()) {
      table.add(makeRow(pid, theIdentities.get(pid)));
    }
    return table;
  }

  private Map makeRow(PeerIdentity pid, PeerIdentityStatus status) {
    Map row = new HashMap();
    if (pid.isLocalIdentity()) {
      StatusTable.DisplayedValue val =
	new StatusTable.DisplayedValue(pid.getIdString());
      val.setBold(true);
      row.put("ip", val);
    } else {
      row.put("ip", pid.getIdString());
    }
    row.put("lastMessage", new Long(status.getLastMessageTime()));
    row.put("lastOp", getMessageType(status.getLastMessageOpCode()));
    row.put("origTot", new Long(status.getTotalMessages()));
    row.put("origPoller",
            new Long(status.getTotalPollerPolls()));
    row.put("origLastPoller",
            new Long(status.getLastPollerTime()));
    row.put("origVoter",
	    new Long(status.getTotalVoterPolls()));
    row.put("origLastVoter",
            new Long(status.getLastVoterTime()));
    row.put("origLastInvitation",
            new Long(status.getLastPollInvitationTime()));
    row.put("origTotalInvitations",
            new Long(status.getTotalPollInvitatioins()));
    row.put("pollsRejected",
            new Long(status.getTotalRejectedPolls()));
    row.put("pollNak",
            status.getLastPollNak());
    return row;
  }

  private List getSummaryInfo(String key) {
    List res = new ArrayList();
    //       res.add(new StatusTable.SummaryInfo("Total bytes hashed",
    // 					  ColumnDescriptor.TYPE_INT,
    // 					  new Integer(0)));
    return res;
  }
  
  private String getMessageType(int opcode) {
    if (opcode >= V3LcapMessage.POLL_MESSAGES_BASE && 
        opcode < (V3LcapMessage.POLL_MESSAGES.length +
            V3LcapMessage.POLL_MESSAGES_BASE)) {
      return V3LcapMessage.POLL_MESSAGES[opcode - 
                                         V3LcapMessage.POLL_MESSAGES_BASE]
                                         + " (" + opcode + ")";
    } else {
      return "n/a";
    }
  }

}
