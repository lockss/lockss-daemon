/*
* $Id: PollerStatus.java,v 1.8 2003-07-09 19:25:19 clairegriffin Exp $
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

package org.lockss.poller;

import org.lockss.daemon.status.*;
import org.lockss.util.*;
import java.util.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;

/**
 * <p>Description: Provides support for the PollManager and Polls to present
 * the current status information</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class PollerStatus {
  public static final String MANAGER_STATUS_TABLE_NAME = "PollManagerTable";
  public static final String POLL_STATUS_TABLE_NAME = "PollTable";

  static PollManager pollManager;
  private static Logger theLog=Logger.getLogger("PollerStatus");

  PollerStatus(PollManager pollManager) {
    this.pollManager = pollManager;
  }

  static class ManagerStatus
      implements StatusAccessor {
    static final String TABLE_NAME = MANAGER_STATUS_TABLE_NAME;
    private static String POLLMANAGER_TABLE_TITLE = "Poll Manager Table";

    static final int STRINGTYPE = ColumnDescriptor.TYPE_STRING;
    static final int DATETYPE = ColumnDescriptor.TYPE_DATE;

    private static final List sortRules =
      ListUtil.list(
		    new StatusTable.SortRule("AuName", true),
//		    new StatusTable.SortRule("URL", true),
		    new StatusTable.SortRule("Deadline", false)
		    );
    private static final List columnDescriptors =
        ListUtil.list(
		      new ColumnDescriptor("AuName", "Volume", STRINGTYPE),
		      new ColumnDescriptor("URL", "URL", STRINGTYPE),
		      new ColumnDescriptor("Range", "Range", STRINGTYPE),
		      new ColumnDescriptor("PollType", "Type", STRINGTYPE),
		      new ColumnDescriptor("Status", "Status", STRINGTYPE),
		      new ColumnDescriptor("Deadline", "Deadline", DATETYPE),
		      new ColumnDescriptor("PollID", "Poll ID", STRINGTYPE)
		      );
    private static String[] allowedKeys = {
      "AU:", "URL:", "PollType:", "Status:"};

    public void populateTable(StatusTable table) throws StatusService.
        NoSuchTableException {
      checkKey(table.getKey());
      table.setTitle(POLLMANAGER_TABLE_TITLE);
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(table.getKey()));
    }

    public boolean requiresKey() {
      return false;
    }

    // utility methods for making a Reference

    public StatusTable.Reference makeAURef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, "AU:" + key);
    }

    public StatusTable.Reference makeURLRef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, "URL:" + key);
    }

    public StatusTable.Reference makePollTypeRef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, "PollType:" + key);
    }

    public StatusTable.Reference makeStatusRef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, "Status:" + key);
    }

    // routines to make a row
    private List getRows(String key) throws StatusService.NoSuchTableException {
      ArrayList rowL = new ArrayList();
      Iterator it = pollManager.getPolls();
      while (it.hasNext()) {
        PollManager.PollManagerEntry entry = (PollManager.PollManagerEntry) it.next();

        if (key == null || matchKey(entry, key)) {
          rowL.add(makeRow(entry));
        }
      }
      return rowL;
    }

    private String getTypeCharString(int pollType) {
      switch(pollType) {
        case 0:
          return "N";
        case 1:
          return "C";
        case 2:
          return "V";
        default:
          return "Unknown";
      }
    }

    private Map makeRow(PollManager.PollManagerEntry entry) {
      HashMap rowMap = new HashMap();
      PollSpec spec = entry.spec;
      //"AuName"
      rowMap.put("AuName", spec.getCachedUrlSet().getArchivalUnit().getName());
      //"URL"
      rowMap.put("URL", spec.getUrl());
      //"Range"
      rowMap.put("Range", spec.getRangeString());
      //"PollType"
      rowMap.put("PollType", getTypeCharString(entry.type));
      //"Status"
      rowMap.put("Status", entry.getStatusString());
      //"Deadline"
      if (entry.pollDeadline != null) {
        rowMap.put("Deadline", new Long(entry.pollDeadline.getExpirationTime()));
      }
      //"PollID"
      rowMap.put("PollID", PollStatus.makePollRef(entry.getShortKey(),
          entry.key));
      return rowMap;
    }

    // key support routines
    private void checkKey(String key) throws StatusService.NoSuchTableException {
      if (key != null && !allowableKey(allowedKeys, key)) {
        throw new StatusService.NoSuchTableException("unknonwn key: " + key);
      }
    }

    private boolean allowableKey(String[] keyArray, String key) {
      for (int i = 0; i < keyArray.length; i++) {
        if (key.startsWith(keyArray[i]))
          return true;
      }
      return false;
    }

    private boolean matchKey(PollManager.PollManagerEntry entry, String key) {
      boolean isMatch = false;
      PollSpec spec = entry.spec;
      String keyValue = key.substring(key.indexOf(':') + 1);
      if (key.startsWith("AU:")) {
        if (spec.getAUId().equals(keyValue)) {
          isMatch = true;
        }
      }
      else if (key.startsWith("URL:")) {
        if (spec.getUrl().equals(keyValue)) {
          isMatch = true;
        }
      }
      else if (key.startsWith("PollType:")) {
        if (entry.getTypeString().equals(keyValue)) {
          isMatch = true;
        }
      }
      else if (key.startsWith("Status:")) {
        if (entry.getStatusString().equals(keyValue)) {
          isMatch = true;
        }
      }

      return isMatch;
    }

  }

  static class PollStatus implements StatusAccessor {
    static final String TABLE_NAME = POLL_STATUS_TABLE_NAME;

    static final int IPTYPE = ColumnDescriptor.TYPE_IP_ADDRESS;
    static final int INTTYPE = ColumnDescriptor.TYPE_INT;
    static final int STRINGTYPE = ColumnDescriptor.TYPE_STRING;

    private static final List columnDescriptors =
        ListUtil.list(new ColumnDescriptor("Identity", "Identity", IPTYPE),
        new ColumnDescriptor("Reputation", "Reputation", INTTYPE),
        new ColumnDescriptor("Agree", "Agree", STRINGTYPE),
        new ColumnDescriptor("Challenge", "Challenge", STRINGTYPE),
        new ColumnDescriptor("Verifier", "Verifier", STRINGTYPE),
        new ColumnDescriptor("Hash", "Hash", STRINGTYPE)
        );

    private static final List sortRules =
        ListUtil.list(new StatusTable.SortRule("Identity", true));



    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      String key = table.getKey();
      BasePoll poll = getPoll(key);
      table.setTitle(getTitle(key));
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(poll));
      table.setSummaryInfo(getSummary(poll));
    }

    public boolean requiresKey() {
      return true;
    }

    public String getTitle(String key) {
      return "Table for poll " + key;
    }

    // poll summary info

    private List getSummary(BasePoll poll){
      PollTally tally = poll.getVoteTally();
      List summaryList =  ListUtil.list(
          new StatusTable.SummaryInfo("Target" , STRINGTYPE,
          getPollDescription(poll)),
          new StatusTable.SummaryInfo("Caller", IPTYPE,
          poll.m_caller.getAddress()),
          new StatusTable.SummaryInfo("Start Time", ColumnDescriptor.TYPE_DATE,
          new Long(poll.m_createTime)),
          new StatusTable.SummaryInfo("Duration",
          ColumnDescriptor.TYPE_TIME_INTERVAL,
          new Long(tally.duration)),
          new StatusTable.SummaryInfo("Quorum", INTTYPE,
          new Integer(tally.quorum)),
          new StatusTable.SummaryInfo("Agree Votes", INTTYPE,
          new Integer(tally.numAgree)),
          new StatusTable.SummaryInfo("Disagree Votes", INTTYPE,
          new Integer(tally.numDisagree))
          );
      return summaryList;
    }

    private String getPollDescription(BasePoll poll) {
      StringBuffer sb = new StringBuffer();
      sb.append(Poll.PollName[poll.getVoteTally().getType()]);
      sb.append(" poll on ");
      sb.append(poll.getPollSpec().getUrl());
      sb.append("[");
      sb.append(poll.getPollSpec().getRangeString());
      sb.append("]");
      return sb.toString();
    }
    // row building methods
    private List getRows(BasePoll poll) {
      PollTally tally = poll.getVoteTally();

      ArrayList l = new ArrayList();
      Iterator it = tally.pollVotes.iterator();
      while(it.hasNext()) {
        Vote vote = (Vote)it.next();
        l.add(makeRow(vote));
      }
      return l;
    }

    private Map makeRow(Vote vote) {
      HashMap rowMap = new HashMap();

      rowMap.put("Identity", vote.getIDAddress());
      LcapIdentity id = pollManager.getIdentityManager().findIdentity(
          vote.getIDAddress());
      rowMap.put("Reputation", String.valueOf(id.getReputation()));
      rowMap.put("Agree", String.valueOf(vote.agree));
      rowMap.put("Challenge", vote.getChallengeString());
      rowMap.put("Verifier",vote.getVerifierString());
      rowMap.put("Hash",vote.getHashString());

      return rowMap;
    }


    // utility methods for making a Reference

    public static StatusTable.Reference makePollRef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, key);
    }


    // key support routines
    private BasePoll getPoll(String key) throws StatusService.NoSuchTableException {
      BasePoll poll = pollManager.getPoll(key);
      if(poll == null) {
        throw new StatusService.NoSuchTableException("unknown poll key: " + key);
      }
      return poll;
    }

  }

  static class ManagerStatusAURef implements ObjectReferenceAccessor {

    int howManyPollsRunning(ArchivalUnit au) {
      ManagerStatus ms = new ManagerStatus();
      try {
        return ms.getRows("AU:" + au.getAUId()).size();
      } catch (StatusService.NoSuchTableException e) {
        theLog.debug("no table", e);
        return 0;
      }
    }

    public StatusTable.Reference getReference(Object obj, String tableName) {
      ArchivalUnit au = (ArchivalUnit)obj;
      return new StatusTable.Reference(howManyPollsRunning(au) + " polls",
                                       tableName, "AU:" + au.getAUId());
    }
  }
}
