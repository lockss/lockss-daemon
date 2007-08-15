/*
* $Id: V3PollStatus.java,v 1.15 2007-08-15 08:32:41 smorabito Exp $
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
import java.text.*;
import org.lockss.daemon.status.*;
import org.lockss.daemon.status.StatusService.*;
import org.lockss.daemon.status.StatusTable.SummaryInfo;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.PollManager.*;
import org.lockss.poller.v3.*;
import static org.lockss.poller.v3.V3Poller.*;
import static org.lockss.poller.v3.V3Voter.*;
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
  public static final String NO_QUORUM_TABLE_NAME = "V3NoQuorumURLsTable";
  public static final String TOO_CLOSE_TABLE_NAME = "V3TooCloseURLsTable";
  public static final String AGREE_TABLE_NAME = "V3AgreeURLsTable";
  public static final String DISAGREE_TABLE_NAME = "V3DisagreeURLsTable";
  public static final String ERROR_TABLE_NAME = "V3ErrorURLsTable";

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
      extends V3PollStatus implements StatusAccessor {

    static final String TABLE_TITLE = "V3 Polls (Mine)";

    private static final DecimalFormat agreementFormat =
      new DecimalFormat("0.00");
    
    /* DecimalFormat automatically applies half-even rounding to
     * values being formatted under Java < 1.6.  This is a workaround. */ 
    private static String doubleToPercent(double d) {
      int i = (int)(d * 10000);
      double pc = i / 100.0;
      return agreementFormat.format(pc);
    }

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
                    new ColumnDescriptor("Errors", "Hash Errors",
                                         ColumnDescriptor.TYPE_INT,
                                         "Errors encountered while hashing content."),
                    new ColumnDescriptor("completedRepairs", "Repairs",
                                         ColumnDescriptor.TYPE_INT,
                                         "Completed repairs."),
                    new ColumnDescriptor("agreement", "Agreement",
                                         ColumnDescriptor.TYPE_STRING),
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
      String key = table.getKey();
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(key));
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows(String key) {
      List rows = new ArrayList();
      Collection v3Pollers = pollManager.getV3Pollers();
      for (Iterator it = v3Pollers.iterator(); it.hasNext(); ) {
        V3Poller poller = (V3Poller)it.next();
        if ((key != null && key.equals(poller.getAu().getAuId())) || 
            key == null) {
          rows.add(makeRow(poller));
        }
      }
      return rows;
    }

    private Map makeRow(V3Poller poller) {
      Map row = new HashMap();
      
      row.put("auId", poller.getAu().getName());
      row.put("participants", new Integer(poller.getPollSize()));
      row.put("status", poller.getStatusString());
      row.put("talliedUrls", new Integer(poller.getTalliedUrls().size()));
      row.put("hashErrors", new Integer(poller.getErrorUrls().size()));
      row.put("completedRepairs", new Integer(poller.getCompletedRepairs().size()));
      if (poller.getStatus() == V3Poller.PEER_STATUS_COMPLETE) {
        row.put("agreement", doubleToPercent(poller.getPercentAgreement()) + "%");
      } else {
        row.put("agreement", "--");
      }
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
        extends V3PollStatus implements StatusAccessor {

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

  public static class PollOverview
    extends V3PollerStatus implements OverviewAccessor {

    public PollOverview(PollManager pollManager) {
      super(pollManager);
    }

    public Object getOverview(String tableName, BitSet options) {
      int nActive = 0;
      int nNoQuorum = 0;
      int nComplete = 0;
      int nTooBusy = 0;
      for (V3Poller poller :
	     (Collection<V3Poller>)pollManager.getV3Pollers()) {
	switch (poller.getStatus()) {
	case POLLER_STATUS_STARTING:
	case POLLER_STATUS_RESUMING:
	case POLLER_STATUS_INVITING_PEERS:
	case POLLER_STATUS_HASHING:
	case POLLER_STATUS_TALLYING:
	case POLLER_STATUS_WAITING_REPAIRS:
	  nActive++;
	  break;
	case POLLER_STATUS_COMPLETE:
	  nComplete++;
	  break;
	case POLLER_STATUS_NO_QUORUM:
	  nNoQuorum++;
	  break;
	case POLLER_STATUS_ERROR:
	case POLLER_STATUS_EXPIRED:
	  break;
	case POLLER_STATUS_NO_TIME:
	  nTooBusy++;
	  break;
	}
      }
      List lst = new ArrayList();

      lst.add(StringUtil.numberOfUnits(nActive,
				       "active poll", "active polls"));
      if (nComplete > 0) lst.add(nComplete + " complete");
      if (nNoQuorum > 0) lst.add(nNoQuorum + " no quorum");
      if (nTooBusy > 0) lst.add(nTooBusy + " too busy");
      String summ = StringUtil.separatedString(lst, ", ");
      return new StatusTable.Reference(summ, POLLER_STATUS_TABLE_NAME);
    }
  }

  public static class VoterOverview
    extends V3PollerStatus implements OverviewAccessor {

    public VoterOverview(PollManager pollManager) {
      super(pollManager);
    }

    public Object getOverview(String tableName, BitSet options) {
      int nActive = 0;
      int nError = 0;
      int nComplete = 0;
      int nTooBusy = 0;
      for (V3Voter voter :
	     (Collection<V3Voter>)pollManager.getV3Voters()) {
	switch (voter.getStatus()) {
	case STATUS_INITIALIZED:
	case STATUS_ACCEPTED_POLL:
	case STATUS_HASHING:
	case STATUS_VOTED:
	  nActive++;
	  break;
	case STATUS_NO_TIME:
	  nTooBusy++;
	  break;
	case STATUS_COMPLETE:
	  nComplete++;
	  break;
	case STATUS_EXPIRED:
	case STATUS_ERROR:
	  nError++;
	  break;
	case STATUS_DECLINED_POLL:
	case STATUS_VOTE_ACCEPTED:
	  break;
	}
      }
      List lst = new ArrayList();

      lst.add(StringUtil.numberOfUnits(nActive,
				       "active vote", "active votes"));
      if (nComplete > 0) lst.add(nComplete + " complete");
      if (nTooBusy > 0) lst.add(nTooBusy + " too busy");
      if (nError > 0) lst.add(nError + " error");
      String summ = StringUtil.separatedString(lst, ", ");
      return new StatusTable.Reference(summ, VOTER_STATUS_TABLE_NAME);
    }
  }

  /**
   * <p>The full status of an individual V3 Poll in which we are acting as a
   * participant.  Requires the PollID as a key.</p>
   *
   */
  public static class V3PollerStatusDetail
      extends V3PollStatus implements StatusAccessor {

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
        theLog.error("Expected V3Poller, but got " +
		     pollManager.getPoll(key).getClass().getName());
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
      if (poll.getPollerStateBean().getErrorDetail() != null) {
        summary.add(new SummaryInfo("Error",
                                    ColumnDescriptor.TYPE_STRING,
                                    poll.getPollerStateBean().getErrorDetail()));
      }
      summary.add(new SummaryInfo("Start Time",
                                  ColumnDescriptor.TYPE_DATE,
                                  new Long(poll.getCreateTime())));
      summary.add(new SummaryInfo("Vote Deadline",
                                  ColumnDescriptor.TYPE_DATE,
                                  new Long(poll.getVoteDeadline())));
      summary.add(new SummaryInfo("Duration",
                                  ColumnDescriptor.TYPE_TIME_INTERVAL,
                                  new Long(poll.getDuration())));
      if (poll.getErrorUrls().size() > 0) {
        summary.add(new SummaryInfo("URLs with Hash errors",
                                    ColumnDescriptor.TYPE_STRING,
                                    new StatusTable.Reference(new Integer(poll.getErrorUrls().size()),
                                                              "V3ErrorURLsTable",
                                                              poll.getKey())));
      }

      int activeRepairs = poll.getActiveRepairs().size();
      int talliedUrls = poll.getTalliedUrls().size();
      int agreeUrls = poll.getAgreedUrls().size();
      int disagreeUrls = poll.getDisagreedUrls().size();
      int noQuorumUrls = poll.getNoQuorumUrls().size();
      int tooCloseUrls = poll.getTooCloseUrls().size();
      int completedRepairs = poll.getCompletedRepairs().size();
        
      summary.add(new SummaryInfo("Total URLs In Vote",
                                  ColumnDescriptor.TYPE_INT,
                                  new Integer(talliedUrls)));
      if (agreeUrls > 0) {
        summary.add(new SummaryInfo("Agreeing URLs",
                                    ColumnDescriptor.TYPE_INT,
                                    new StatusTable.Reference(new Integer(agreeUrls),
                                                              "V3AgreeURLsTable",
                                                              poll.getKey())));
      }
      if (disagreeUrls > 0) {
        summary.add(new SummaryInfo("Disagreeing URLs",
                                    ColumnDescriptor.TYPE_INT,
                                    new StatusTable.Reference(new Integer(disagreeUrls),
                                                              "V3DisagreeURLsTable",
                                                              poll.getKey())));
      }
      if (noQuorumUrls > 0) {
        summary.add(new SummaryInfo("No Quorum URLs",
                                    ColumnDescriptor.TYPE_INT,
                                    new StatusTable.Reference(new Integer(noQuorumUrls),
                                                              "V3NoQuorumURLsTable",
                                                              poll.getKey())));
      }
      if (tooCloseUrls > 0) {
        summary.add(new SummaryInfo("Too Close URLs",
                                    ColumnDescriptor.TYPE_INT,
                                    new StatusTable.Reference(new Integer(tooCloseUrls),
                                                              "V3TooCloseURLsTable",
                                                              poll.getKey())));
      }
      if (completedRepairs > 0) {
        summary.add(new SummaryInfo("Completed Repairs",
                                    ColumnDescriptor.TYPE_INT,
                                    new StatusTable.Reference(new Integer(completedRepairs),
                                                              "V3CompletedRepairsTable",
                                                              poll.getKey())));
      }
      if (activeRepairs > 0) {
        String message = poll.isPollActive() ? "Queued Repairs" : "Incomplete Repairs";
        summary.add(new SummaryInfo("Queued Repairs",
                                    ColumnDescriptor.TYPE_INT,
                                    new StatusTable.Reference(new Integer(activeRepairs),
                                                              "V3ActiveRepairsTable",
                                                              poll.getKey())));
        
      }
    
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
      extends V3PollStatus implements StatusAccessor {
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
        theLog.error("Expected V3Voter, but got " +
		     pollManager.getPoll(key).getClass().getName());
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
        if (rp.isPublisherRepair()) {
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
      extends V3PollStatus implements StatusAccessor {
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
        theLog.error("Expected V3Poller, but got " +
		     pollManager.getPoll(key).getClass().getName());
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
        if (rp.isPublisherRepair()) {
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
  
  public static class V3NoQuorumURLs extends V3PollStatus 
      implements StatusAccessor {
    static final String TABLE_TITLE = "V3 Poll Details - No Quorum URLs";
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("url",
                                             CatalogueOrderComparator.SINGLETON));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("url", "URL",
                                         ColumnDescriptor.TYPE_STRING));
    public V3NoQuorumURLs(PollManager manager) {
      super(manager);
    }
    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      V3Poller poller = null;
      try {
        poller = (V3Poller)pollManager.getPoll(key);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Poller, but got " +
		     pollManager.getPoll(key).getClass().getName());
        return;
      }
      if (poller == null) return;
      table.setTitle("V3 Poll Details - No Quorum URLs in poll " + poller.getKey());
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(poller));
    }
    private List getRows(V3Poller poller) {
      List rows = new ArrayList();
      for (Iterator it = poller.getNoQuorumUrls().iterator(); it.hasNext(); ) {
        Map row = new HashMap();
        row.put("url", (String)it.next());
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

  public static class V3TooCloseURLs extends V3PollStatus 
      implements StatusAccessor {
    static final String TABLE_TITLE = "V3 Poll Details - Too Close URLs";
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("url",
                                             CatalogueOrderComparator.SINGLETON));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("url", "URL",
                                         ColumnDescriptor.TYPE_STRING));
    public V3TooCloseURLs(PollManager manager) {
      super(manager);
    }
    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      V3Poller poller = null;
      try {
        poller = (V3Poller)pollManager.getPoll(key);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Poller, but got " +
		     pollManager.getPoll(key).getClass().getName());
        return;
      }
      if (poller == null) return;
      table.setTitle("V3 Poll Details - Too Close URLs in poll " + poller.getKey());
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(poller));
    }
    private List getRows(V3Poller poller) {
      List rows = new ArrayList();
      for (Iterator it = poller.getTooCloseUrls().iterator(); it.hasNext(); ) {
        Map row = new HashMap();
        row.put("url", (String)it.next());
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
  
  
  public static class V3AgreeURLs extends V3PollStatus 
      implements StatusAccessor {
    static final String TABLE_TITLE = "V3 Poll Details - Agreeing URLs";
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("url",
                                             CatalogueOrderComparator.SINGLETON));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("url", "URL",
                                         ColumnDescriptor.TYPE_STRING));
    public V3AgreeURLs(PollManager manager) {
      super(manager);
    }
    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      V3Poller poller = null;
      try {
        poller = (V3Poller)pollManager.getPoll(key);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Poller, but got " +
		     pollManager.getPoll(key).getClass().getName());
        return;
      }
      if (poller == null) return;
      table.setTitle("V3 Poll Details - Agreeing URLs in poll " + poller.getKey());
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(poller));
    }
    private List getRows(V3Poller poller) {
      List rows = new ArrayList();
      for (Iterator it = poller.getAgreedUrls().iterator(); it.hasNext(); ) {
        Map row = new HashMap();
        row.put("url", (String)it.next());
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

  public static class V3DisagreeURLs extends V3PollStatus 
      implements StatusAccessor {
    static final String TABLE_TITLE = "V3 Poll Details - Disagreeing URLs";
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("url",
                                             CatalogueOrderComparator.SINGLETON));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("url", "URL",
                                         ColumnDescriptor.TYPE_STRING));
    public V3DisagreeURLs(PollManager manager) {
      super(manager);
    }
    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      V3Poller poller = null;
      try {
        poller = (V3Poller)pollManager.getPoll(key);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Poller, but got " +
		     pollManager.getPoll(key).getClass().getName());
        return;
      }
      if (poller == null) return;
      table.setTitle("V3 Poll Details - Disagreeing URLs in poll " + poller.getKey());
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(poller));
    }
    private List getRows(V3Poller poller) {
      List rows = new ArrayList();
      for (Iterator it = poller.getDisagreedUrls().iterator(); it.hasNext(); ) {
        Map row = new HashMap();
        row.put("url", (String)it.next());
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
  
  public static class V3ErrorURLs extends V3PollStatus 
      implements StatusAccessor {
    static final String TABLE_TITLE = "V3 Poll Details - URLs with Hash Errors";
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("url",
                                             CatalogueOrderComparator.SINGLETON));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor("url", "URL",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor("erorr", "Error",
                                         ColumnDescriptor.TYPE_STRING));
    public V3ErrorURLs(PollManager manager) {
      super(manager);
    }
    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      V3Poller poller = null;
      try {
        poller = (V3Poller)pollManager.getPoll(key);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Poller, but got " +
                     pollManager.getPoll(key).getClass().getName());
        return;
      }
      if (poller == null) return;
      table.setTitle("V3 Poll Details - URLs with Hash Errors in poll " + poller.getKey());
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(poller));
    }
    private List getRows(V3Poller poller) {
      List rows = new ArrayList();
      Map errorUrls = poller.getErrorUrls();
      synchronized(errorUrls) {
        for (Iterator it = errorUrls.keySet().iterator(); it.hasNext(); ) {
          String url = (String)it.next();
          String exceptionMessage = (String)errorUrls.get(url);
          Map row = new HashMap();
          row.put("url", url);
          row.put("error", exceptionMessage);
          rows.add(row);
        }
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
      extends V3PollStatus implements StatusAccessor {
    static final String TABLE_TITLE = "V3 Vote Status";

    public V3VoterStatusDetail(PollManager manager) {
      super(manager);
    }

    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      V3Voter voter = null;
      try {
        voter = (V3Voter)pollManager.getPoll(key);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Voter, but got " +
		     pollManager.getPoll(key).getClass().getName());
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
      if (voter.getVoterUserData().getErrorDetail() != null) {
        summary.add(new SummaryInfo("Error",
                                    ColumnDescriptor.TYPE_STRING,
                                    voter.getVoterUserData().getErrorDetail()));
      }
      summary.add(new SummaryInfo("Caller",
                                  ColumnDescriptor.TYPE_STRING,
                                  voter.getPollerId().getIdString()));
      summary.add(new SummaryInfo("Start Time",
                                  ColumnDescriptor.TYPE_DATE,
                                  new Long(voter.getCreateTime())));
      summary.add(new SummaryInfo("Vote Deadline",
                                  ColumnDescriptor.TYPE_DATE,
                                  voter.getVoteDeadline()));
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
