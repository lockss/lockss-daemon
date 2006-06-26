/*
* $Id: V3PollStatus.java,v 1.5 2006-06-26 23:55:08 smorabito Exp $
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

package org.lockss.poller.v3;

import java.util.*;
import org.lockss.daemon.status.*;
import org.lockss.daemon.status.StatusService.*;
import org.lockss.daemon.status.StatusTable.SummaryInfo;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.PollManager.*;
import org.lockss.poller.v3.*;
import org.lockss.protocol.*;

/**
 * Provides support for the PollManager and Polls to present
 * the current status information of V3 Polls.
 */
public class V3PollStatus {
  public static final String POLLER_STATUS_TABLE_NAME = "V3PollerTable";
  public static final String VOTER_STATUS_TABLE_NAME = "V3VoterTable";
  public static final String POLLER_DETAIL_TABLE_NAME = "V3PollerDetailTable";
  public static final String VOTER_DETAIL_TABLE_NAME = "V3VoterDetailTable";
  public static final String ACTIVE_REPAIRS_TABLE_NAME = "V3ActiveRepairsTable";
  public static final String COMPLETED_REPAIRS_TABLE_NAME = "V3CompletedRepairsTable";

  protected PollManager pollManager;
  private static Logger theLog = Logger.getLogger("V3PollerStatus");

  V3PollStatus(PollManager pollManager) {
    this.pollManager = pollManager;
  }

  String makeShortPollKey(String pollKey) {
    return pollKey.substring(0, 10);
  }

  /**
   * <p>Overview status table for all V3 polls in which we are acting as
   * the caller of the poll.</p>
   */
  public static class V3PollerStatus
      extends V3PollStatus implements StatusAccessor.DebugOnly {

    static final String TABLE_TITLE = "V3 Polls (Mine)";

    // Sort by deadline, descending
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("deadline", false));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("auId", "Volume",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor("participants", "Participants",
                                         ColumnDescriptor.TYPE_INT),
                    new ColumnDescriptor("status", "Status",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor("talliedUrls", "URLs Tallied",
                                         ColumnDescriptor.TYPE_INT,
                                         "Total number of URLs examined so " +
                                         "far in this poll."),
                    new ColumnDescriptor("activeRepairs", "Repairs (A)",
                                         ColumnDescriptor.TYPE_INT,
                                         "Active repairs."),
                    new ColumnDescriptor("completedRepairs", "Repairs (C)",
                                         ColumnDescriptor.TYPE_INT,
                                         "Completed repairs."),
                    new ColumnDescriptor("agreement", "Agreement",
                                         ColumnDescriptor.TYPE_PERCENT),
                    new ColumnDescriptor("start", "Start",
                                         ColumnDescriptor.TYPE_DATE),
                    new ColumnDescriptor("deadline", "Deadline",
                                         ColumnDescriptor.TYPE_DATE),
                    new ColumnDescriptor("pollId", "Poll ID",
                                         ColumnDescriptor.TYPE_STRING));

    public V3PollerStatus(PollManager pollManager) {
      super(pollManager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows());
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows() {
      List rows = new ArrayList();
      Collection v3Pollers = pollManager.getV3Pollers();
      for (Iterator it = v3Pollers.iterator(); it.hasNext(); ) {
        rows.add(makeRow((V3Poller)it.next()));
      }
      return rows;
    }

    private Map makeRow(V3Poller poller) {
      Map row = new HashMap();
      
      float agreeingUrls = (float)poller.getAgreedUrls().size();
      float talliedUrls = (float)poller.getTalliedUrls().size();
      float agreement;
      if (agreeingUrls > 0)
        agreement = agreeingUrls / talliedUrls;
      else
        agreement = 0;
      
      row.put("auId", poller.getAu().getName());
      row.put("participants", new Integer(poller.getPollSize()));
      row.put("status", poller.getStatusString());
      row.put("talliedUrls", new Integer(poller.getTalliedUrls().size()));
      row.put("activeRepairs", new Integer(poller.getActiveRepairs().size()));
      row.put("completedRepairs", new Integer(poller.getCompletedRepairs().size()));
      row.put("agreement", new Float(agreement));
      row.put("start", new Long(poller.getCreateTime()));
      row.put("deadline", poller.getDeadline());
      row.put("pollId",
              new StatusTable.Reference(makeShortPollKey(poller.getKey()),
                                        "V3PollerDetailTable",
                                        poller.getKey()));
      return row;
    }
  }

  /**
   * <p>Overview status table for all V3 polls in which we are acting as
   * a participant.</p>
   */
  public static class V3VoterStatus
        extends V3PollStatus implements StatusAccessor.DebugOnly {

    static final String TABLE_TITLE = "V3 Polls (Others)";

    // Sort by deadline, descending
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("deadline", false));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("auId", "Volume",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor("caller", "Caller",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor("status", "Status",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor("start", "Start",
                                         ColumnDescriptor.TYPE_DATE),
                    new ColumnDescriptor("deadline", "Deadline",
                                         ColumnDescriptor.TYPE_DATE),
                    new ColumnDescriptor("pollId", "Poll ID",
                                         ColumnDescriptor.TYPE_STRING));

    public V3VoterStatus(PollManager pollManager) {
      super(pollManager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows());
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows() {
      List rows = new ArrayList();
      Collection v3Voters = pollManager.getV3Voters();
      for (Iterator it = v3Voters.iterator(); it.hasNext(); ) {
        rows.add(makeRow((V3Voter)it.next()));
      }
      return rows;
    }

    private Map makeRow(V3Voter voter) {
      Map row = new HashMap();
      row.put("auId", voter.getAu().getName());
      row.put("caller", voter.getPollerId().getIdString());
      row.put("status", voter.getStatusString());
      row.put("start", new Long(voter.getCreateTime()));
      row.put("deadline", voter.getDeadline());
      row.put("pollId",
              new StatusTable.Reference(makeShortPollKey(voter.getKey()),
                                        "V3VoterDetailTable",
                                        voter.getKey()));
      return row;
    }
  }

  /**
   * <p>The full status of an individual V3 Poll in which we are acting as a
   * participant.  Requires the PollID as a key.</p>
   *
   */
  public static class V3PollerStatusDetail
      extends V3PollStatus implements StatusAccessor.DebugOnly {

    static final String TABLE_TITLE = "V3 Poll Status";

    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("identity",
                                             CatalogueOrderComparator.SINGLETON));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("identity", "Peer",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor("peerStatus", "Status",
                                         ColumnDescriptor.TYPE_STRING));

    public V3PollerStatusDetail(PollManager pollManager) {
      super(pollManager);
    }

    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      V3Poller poll = null;
      try {
        poll = (V3Poller)pollManager.getPoll(key);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Poller, but got " + poll.getClass().getName());
        return;
      }
      if (poll == null) return;
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setSummaryInfo(getSummary(poll));
      table.setTitle("Status for V3 Poll " + key);
      table.setRows(getRows(poll));
    }

    private List getRows(V3Poller poll) {
      List rows = new ArrayList();
      Iterator voters = poll.getParticipants();
      while (voters.hasNext()) {
        ParticipantUserData voter = (ParticipantUserData)voters.next();
        rows.add(makeRow(voter));
      }
      return rows;
    }

    private Map makeRow(ParticipantUserData voter) {
      Map row = new HashMap();
      row.put("identity", voter.getVoterId().getIdString());
      row.put("peerStatus", voter.getStatusString());
      return row;
    }

    private List getSummary(V3Poller poll) {
      List summary = new ArrayList();
      summary.add(new SummaryInfo("Volume",
                                  ColumnDescriptor.TYPE_STRING,
                                  poll.getAu().getName()));
      summary.add(new SummaryInfo("Status",
                                  ColumnDescriptor.TYPE_STRING,
                                  poll.getStatusString()));
      summary.add(new SummaryInfo("Start Time",
                                  ColumnDescriptor.TYPE_DATE,
                                  new Long(poll.getCreateTime())));
      summary.add(new SummaryInfo("Duration",
                                  ColumnDescriptor.TYPE_TIME_INTERVAL,
                                  new Long(poll.getDuration())));
      summary.add(new SummaryInfo("Total URLs In Vote",
                                  ColumnDescriptor.TYPE_INT,
                                  new Integer(poll.getTalliedUrls().size())));
      summary.add(new SummaryInfo("Agreeing URLs",
                                  ColumnDescriptor.TYPE_INT,
                                  new Integer(poll.getAgreedUrls().size())));
      summary.add(new SummaryInfo("Disagreeing URLs",
                                  ColumnDescriptor.TYPE_INT,
                                  new Integer(poll.getDisagreedUrls().size())));
      summary.add(new SummaryInfo("No Quorum URLs",
                                  ColumnDescriptor.TYPE_INT,
                                  new Integer(poll.getNoQuorumUrls().size())));
      summary.add(new SummaryInfo("Too Close URLs",
                                  ColumnDescriptor.TYPE_INT,
                                  new Integer(poll.getTooCloseUrls().size())));
      summary.add(new SummaryInfo("Active Repairs",
                                  ColumnDescriptor.TYPE_INT,
                                  new StatusTable.Reference(new Integer(poll.getActiveRepairs().size()),
                                                            "V3ActiveRepairsTable",
                                                            poll.getKey())));
      summary.add(new SummaryInfo("Completed Repairs",
                                  ColumnDescriptor.TYPE_INT,
                                  new StatusTable.Reference(new Integer(poll.getCompletedRepairs().size()),
                                                            "V3CompletedRepairsTable",
                                                            poll.getKey())));
      long remain = TimeBase.msUntil(poll.getDeadline().getExpirationTime());
      if (remain >= 0) {
        summary.add(new SummaryInfo("Remaining",
                                    ColumnDescriptor.TYPE_TIME_INTERVAL,
                                    new Long(remain)));
      }
      summary.add(new SummaryInfo("Quorum",
                                  ColumnDescriptor.TYPE_INT,
                                  poll.getQuorum()));
      return summary;
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public boolean requiresKey() {
      return true;
    }
  }

  public static class V3ActiveRepairs
      extends V3PollStatus implements StatusAccessor.DebugOnly {
    static final String TABLE_TITLE = "V3 Repairs (Active)";
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("url",
                                             CatalogueOrderComparator.SINGLETON));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("url", "URL",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor("repairFrom", "Repair From",
                                         ColumnDescriptor.TYPE_STRING));
    public V3ActiveRepairs(PollManager manager) {
      super(manager);
    }
    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      V3Poller poller = null;
      try {
        poller = (V3Poller)pollManager.getPoll(key);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Voter, but got " + poller.getClass().getName());
        return;
      }
      if (poller == null) return;
      table.setTitle("Active Repairs for Poll " + poller.getKey());
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(poller));
    }
    private List getRows(V3Poller poller) {
      List rows = new ArrayList();
      for (Iterator it = poller.getActiveRepairs().iterator(); it.hasNext(); ) {
        PollerStateBean.Repair rp = (PollerStateBean.Repair)it.next();
        Map row = new HashMap();
        row.put("url", rp.getUrl());
        if (rp.isDeletedFile()) {
          row.put("repairFrom", "N/A (Removed File)");
        } else if (rp.isRepairedFromPublisher()) {
          row.put("repairFrom", "Publisher");
        } else {
          row.put("repairFrom", rp.getRepairFrom().getIdString());
        }
        rows.add(row);
      }
      return rows;
    }
    public String getDisplayName() {
      return TABLE_TITLE;
    }
    public boolean requiresKey() {
      return true;
    }
  }

  public static class V3CompletedRepairs
      extends V3PollStatus implements StatusAccessor.DebugOnly {
    static final String TABLE_TITLE = "V3 Repairs (Completed)";
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("url",
                                             CatalogueOrderComparator.SINGLETON));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("url", "URL",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor("repairFrom", "Repaired From",
                                         ColumnDescriptor.TYPE_STRING));
    public V3CompletedRepairs(PollManager manager) {
      super(manager);
    }
    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      V3Poller poller = null;
      try {
        poller = (V3Poller)pollManager.getPoll(key);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Poller, but got " + poller.getClass().getName());
        return;
      }
      if (poller == null) return;
      table.setTitle("Completed Repairs for Poll " + poller.getKey());
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(poller));
    }
    private List getRows(V3Poller poller) {
      List rows = new ArrayList();
      for (Iterator it = poller.getCompletedRepairs().iterator(); it.hasNext(); ) {
        PollerStateBean.Repair rp = (PollerStateBean.Repair)it.next();
        Map row = new HashMap();
        row.put("url", rp.getUrl());
        if (rp.isDeletedFile()) {
          row.put("repairFrom", "N/A (Removed File)");
        } else if (rp.isRepairedFromPublisher()) {
          row.put("repairFrom", "Publisher");
        } else {
          row.put("repairFrom", rp.getRepairFrom().getIdString());
        }
        rows.add(row);
      }
      return rows;
    }
    public String getDisplayName() {
      return TABLE_TITLE;
    }
    public boolean requiresKey() {
      return true;
    }
  }

  /**
   * <p>The full status of an individual V3 Poll in which we are acting as a
   * participant.  Requires the PollID as a key.</p>
   *
   */
  public static class V3VoterStatusDetail
      extends V3PollStatus implements StatusAccessor.DebugOnly {
    static final String TABLE_TITLE = "V3 Vote Status";

    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("identity",
                                             CatalogueOrderComparator.SINGLETON));

    public V3VoterStatusDetail(PollManager manager) {
      super(manager);
    }

    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      V3Voter voter = null;
      try {
        voter = (V3Voter)pollManager.getPoll(key);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Voter, but got " + voter.getClass().getName());
        return;
      }
      if (voter == null) return;
      table.setSummaryInfo(getSummary(voter));
      table.setTitle("Status for V3 Poll " + key);
    }

    private List getSummary(V3Voter voter) {
      List summary = new ArrayList();
      summary.add(new SummaryInfo("Volume",
                                  ColumnDescriptor.TYPE_STRING,
                                  voter.getAu().getName()));
      summary.add(new SummaryInfo("Status",
                                  ColumnDescriptor.TYPE_STRING,
                                  voter.getStatusString()));
      summary.add(new SummaryInfo("Caller",
                                  ColumnDescriptor.TYPE_STRING,
                                  voter.getPollerId().getIdString()));
      summary.add(new SummaryInfo("Start Time",
                                  ColumnDescriptor.TYPE_DATE,
                                  new Long(voter.getCreateTime())));
      summary.add(new SummaryInfo("Duration",
                                  ColumnDescriptor.TYPE_TIME_INTERVAL,
                                  new Long(voter.getDuration())));
      long remain = TimeBase.msUntil(voter.getDeadline().getExpirationTime());
      if (remain >= 0) {
        summary.add(new SummaryInfo("Remaining",
                                    ColumnDescriptor.TYPE_TIME_INTERVAL,
                                    new Long(remain)));
      }
      summary.add(new SummaryInfo("Poller Nonce",
                                  ColumnDescriptor.TYPE_STRING,
                                  ByteArray.toBase64(voter.getPollerNonce())));
      summary.add(new SummaryInfo("Voter Nonce",
                                  ColumnDescriptor.TYPE_STRING,
                                  ByteArray.toBase64(voter.getVoterNonce())));
      return summary;
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public boolean requiresKey() {
      return true;
    }
  }
}
