/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.poller.v3;

import java.util.*;
import java.text.*;
import java.io.*;

import org.apache.commons.collections4.ListUtils;

import org.lockss.daemon.status.*;
import org.lockss.daemon.status.StatusService.*;
import org.lockss.daemon.status.StatusTable.SummaryInfo;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.PollManager.*;
import org.lockss.hasher.LocalHashResult;
import org.lockss.state.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.protocol.V3LcapMessage.PollNak;

import static org.lockss.poller.v3.V3Poller.*;
import static org.lockss.poller.v3.V3Voter.*;

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

  public static final String PEER_AGREE_URLS_TABLE_NAME =
    "V3PeerAgreeUrlsTable";
  public static final String PEER_DISAGREE_URLS_TABLE_NAME =
    "V3PeerDisagreeUrlsTable";
  public static final String PEER_POLLER_ONLY_URLS_TABLE_NAME =
    "V3PeerPollerOnlyUrlsTable";
  public static final String PEER_VOTER_ONLY_URLS_TABLE_NAME =
    "V3PeerVoterOnlyUrlsTable";

  protected PollManager pollManager;
  private static Logger theLog = Logger.getLogger("V3PollerStatus");

  V3PollStatus(PollManager pollManager) {
    this.pollManager = pollManager;
  }

  private static final DecimalFormat agreementFormat =
    new DecimalFormat("0.00");
    
  private static StatusTable.Reference makeAuRef(ArchivalUnit au,
						 String table) {
    return new StatusTable.Reference(au.getName(),
				     ArchivalUnitStatus.AU_STATUS_TABLE_NAME,
				     au.getAuId());
  }

  private static StatusTable.Reference makePollRef(Object value,
						   PeerIdentity pid,
						   String pollKey) {
    return new StatusTable.Reference(value,
				     pid,
				     POLLER_DETAIL_TABLE_NAME,
				     pollKey);
  }

  private static StatusTable.Reference makeVoteRef(Object value,
						   PeerIdentity pid,
						   String pollKey) {
    return new StatusTable.Reference(value,
				     pid,
				     VOTER_DETAIL_TABLE_NAME,
				     pollKey);
  }

  // Sort keys, not visible
  private static final String SORT_KEY1 = "sort1";
  private static final String SORT_KEY2 = "sort2";

  private static int SORT_BASE_ACTIVE = 0;
  private static int SORT_BASE_PENDING = 1;
  private static int SORT_BASE_DONE = 2;


  /**
   * <p>Overview status table for all V3 polls in which we are acting as
   * the caller of the poll.</p>
   */
  public static class V3PollerStatus
      extends V3PollStatus implements StatusAccessor {

    static final String TABLE_TITLE = "Polls";
    static final String COL_AU_ID = "auId"; // FIXME
    static final String COL_VARIANT = "variant";
    static final String COL_PARTICIPANTS = "participants";
    static final String COL_STATUS = "status";
    static final String COL_TALLIED_URLS = "talliedUrls";
    static final String COL_ERRORS = "Errors";
    static final String COL_COMPLETED_REPAIRS = "completedRepairs";
    static final String COL_AGREEMENT = "agreement";
    static final String COL_START = "start";
    static final String COL_DEADLINE = "deadline";
    static final String COL_END = "end";
    static final String COL_POLL_ID = "pollId";
    static final String COL_HASH_ERRORS = "hashErrors";

    // Sort by (status, suborder):
    // (active, descending start time)
    // (pending, queue order)
    // (done, descending end time)

    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule(SORT_KEY1, true),
		    new StatusTable.SortRule(SORT_KEY2, false));

    private final List colDescs =
      ListUtil.list(new ColumnDescriptor(COL_AU_ID, "AU Name",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_VARIANT, "Type",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_PARTICIPANTS, "Participants",
                                         ColumnDescriptor.TYPE_INT),
                    new ColumnDescriptor(COL_STATUS, "Status",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_TALLIED_URLS, "URLs Tallied",
                                         ColumnDescriptor.TYPE_INT,
                                         "Total number of URLs examined so " +
                                         "far in this poll."),
                    new ColumnDescriptor(COL_ERRORS, "Hash Errors",
                                         ColumnDescriptor.TYPE_INT,
                                         "Errors encountered while hashing content."),
                    new ColumnDescriptor(COL_COMPLETED_REPAIRS, "Repairs",
                                         ColumnDescriptor.TYPE_INT,
                                         "Completed repairs."),
                    new ColumnDescriptor(COL_AGREEMENT, "Agreement",
                                         ColumnDescriptor.TYPE_AGREEMENT),
                    new ColumnDescriptor(COL_START, "Start",
                                         ColumnDescriptor.TYPE_DATE),
                    new ColumnDescriptor(COL_DEADLINE, "Deadline",
                                         ColumnDescriptor.TYPE_DATE),
                    new ColumnDescriptor(COL_END, "End",
                                         ColumnDescriptor.TYPE_DATE),
                    new ColumnDescriptor(COL_POLL_ID, "Poll ID",
                                         ColumnDescriptor.TYPE_STRING));

    private static final List<String> defaultCols =
      ListUtil.list(
		    COL_AU_ID,
		    COL_VARIANT,
		    COL_PARTICIPANTS,
		    COL_STATUS,
		    COL_TALLIED_URLS,
		    COL_ERRORS,
		    COL_COMPLETED_REPAIRS,
		    COL_AGREEMENT,
		    COL_START,
		    COL_DEADLINE,
		    COL_POLL_ID
		    );

    private static final List<String> pollPolicyOnlyCols =
      ListUtil.list(COL_VARIANT);

    public V3PollerStatus(PollManager pollManager) {
      super(pollManager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      String key = table.getKey();
      table.setColumnDescriptors(colDescs, getDefaultCols(table));
      table.setSummaryInfo(getSummary(pollManager, table));
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(key));
    }
    
    private List<String> getDefaultCols(StatusTable table) {
      if (pollManager.isV3PollPolicyEnabled()) {
	return defaultCols;
      } else {
	return ListUtils.subtract(defaultCols, pollPolicyOnlyCols);
      }
    }

    private List getSummary(PollManager pollManager, StatusTable table) {
      boolean isDebug = table.getOptions().get(StatusTable.OPTION_DEBUG_USER);
      List summary = new ArrayList();

      if (isDebug) {
	StringBuilder sb = new StringBuilder();
	sb.append(pollManager.getEventCount(EventCtr.Polls));
	sb.append(" started");
	addEndStatus(sb, V3Poller.POLLER_STATUS_COMPLETE);
	addEndStatus(sb, V3Poller.POLLER_STATUS_NO_QUORUM);
	addEndStatus(sb, V3Poller.POLLER_STATUS_ERROR);
	summary.add(new StatusTable.SummaryInfo("Polls",
						ColumnDescriptor.TYPE_STRING,
						sb.toString()));
      }
      V3PollStatusAccessor status = pollManager.getV3Status();
      if (!CurrentConfig.getBooleanParam(V3PollFactory.PARAM_ENABLE_V3_POLLER,
					 V3PollFactory.DEFAULT_ENABLE_V3_POLLER)) { 
	summary.add(new StatusTable.SummaryInfo("Polling is disabled",
						ColumnDescriptor.TYPE_STRING,
						null));
      }
      if (status.getNextPollStartTime() != null) {
        long remainingTime = status.getNextPollStartTime().getRemainingTime();
        String timeStr = remainingTime > 0 ?
            StringUtil.timeIntervalToString(remainingTime) : "running";
	Object val = new StatusTable.DisplayedValue(remainingTime, timeStr);
        summary.add(new SummaryInfo("Poll Starter",
                                    ColumnDescriptor.TYPE_TIME_INTERVAL,
				    val));
      }
//       List<ArchivalUnit> queue = pollManager.getPendingQueueAus();
//       if (!queue.isEmpty()) {
//         summary.add(new SummaryInfo("Queued",
//                                     ColumnDescriptor.TYPE_INT,
//                                     queue.size()));
// 	ArchivalUnit au = queue.get(0);
//         summary.add(new SummaryInfo("Next",
//                                     ColumnDescriptor.TYPE_STRING,
// 				    au.getName()));
//       }
      return summary;
    }

    void addEndStatus(StringBuilder sb, int status) {
      addEndStatus(sb, status, null);
    }

    void addEndStatus(StringBuilder sb, int status, String msg) {
      int cnt = pollManager.getPollEndEventCount(status);
      if (cnt != 0) {
	sb.append(", ");
	sb.append(cnt);
	sb.append(" ");
	sb.append(msg != null ? msg : V3Poller.POLLER_STATUS_STRINGS[status]);
      }
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows(String key) {
      List rows = new ArrayList();
      Collection v3Pollers = pollManager.getV3Pollers();
      for (Iterator it = v3Pollers.iterator(); it.hasNext(); ) {
        V3Poller poller = (V3Poller)it.next();
        if (key == null || key.equals(poller.getAu().getAuId())) {
          rows.add(makeRow(poller));
        }
      }
      int rowNum = 0;
      for (ArchivalUnit au : pollManager.getPendingQueueAus()) {
        if (key == null || key.equals(au.getAuId())) {
	  rows.add(makePendingRow(au, rowNum++));
	}
      }

      return rows;
    }

    private Map makeRow(V3Poller poller) {
      Map row = new HashMap();
      ArchivalUnit au = poller.getAu();
      row.put(COL_AU_ID, makeAuRef(au, ArchivalUnitStatus.AU_STATUS_TABLE_NAME));
      row.put(COL_VARIANT, poller.getPollVariant().shortName());
      row.put(COL_STATUS, poller.getStatusString());
      if (poller.isLocalPoll()) {
	LocalHashResult lhr = poller.getLocalHashResult();
	if (lhr != null) {
	  row.put(COL_TALLIED_URLS, new Integer(lhr.getTotalUrls()));
	}
      } else {
	row.put(COL_PARTICIPANTS, new Integer(poller.getPollSize()));
	row.put(COL_TALLIED_URLS, new Integer(poller.getTalliedUrls().size()));
	if (poller.getErrorUrls() != null) {
	  row.put(COL_HASH_ERRORS, new Integer(poller.getErrorUrls().size()));
	} else {
	  row.put(COL_HASH_ERRORS, "--");
	}
	row.put(COL_COMPLETED_REPAIRS, new Integer(poller.getCompletedRepairs().size()));
	Object agmt = (poller.getStatus() == V3Poller.POLLER_STATUS_COMPLETE)
	  ? poller.getPercentAgreement()
	  : new StatusTable.DisplayedValue(StatusTable.NO_VALUE, "--");
	row.put(COL_AGREEMENT, agmt);
      }
      row.put(COL_START, new Long(poller.getCreateTime()));
      row.put(COL_DEADLINE, poller.getDeadline());
      if (poller.isPollActive()) {
	row.put(SORT_KEY1, SORT_BASE_ACTIVE);
	row.put(SORT_KEY2, row.get(COL_START));
      } else {
	row.put(COL_END, poller.getEndTime());
	row.put(SORT_KEY1, SORT_BASE_DONE);
	row.put(SORT_KEY2, row.get(COL_END));
      }
      String skey = PollUtil.makeShortPollKey(poller.getKey());
      row.put(COL_POLL_ID, new StatusTable.Reference(skey,
						  POLLER_DETAIL_TABLE_NAME,
						  poller.getKey()));
      return row;
    }

    private Map makePendingRow(ArchivalUnit au, int rowNum) {
      Map row = new HashMap();
      row.put(COL_AU_ID, makeAuRef(au, ArchivalUnitStatus.AU_STATUS_TABLE_NAME));
      row.put(COL_STATUS, "Pending");
      row.put(SORT_KEY1, SORT_BASE_PENDING);
      row.put(SORT_KEY2, Integer.MAX_VALUE - rowNum);
      return row;
    }
  }

  /**
   * <p>Overview status table for all V3 polls in which we are acting as
   * a participant.</p>
   */
  public static class V3VoterStatus
        extends V3PollStatus implements StatusAccessor {

    static final String TABLE_TITLE = "Votes";
    static final String COL_AU_ID = "auId"; // FIXME
    static final String COL_CALLER = "caller";
    static final String COL_STATUS = "status";
    static final String COL_START = "start";
    static final String COL_DEADLINE = "deadline";
    static final String COL_POLL_ID = "pollId";

  // Sort by (status, suborder):
  // (active, descending start time)
  // (done, descending end time)

    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule(SORT_KEY1, true),
		    new StatusTable.SortRule(SORT_KEY2, false));

    private final List colDescs =
      ListUtil.list(new ColumnDescriptor(COL_AU_ID, "AU Name",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_CALLER, "Caller",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_STATUS, "Status",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_START, "Start",
                                         ColumnDescriptor.TYPE_DATE),
                    new ColumnDescriptor(COL_DEADLINE, "Deadline",
                                         ColumnDescriptor.TYPE_DATE),
                    new ColumnDescriptor(COL_POLL_ID, "Poll ID",
                                         ColumnDescriptor.TYPE_STRING));

    public V3VoterStatus(PollManager pollManager) {
      super(pollManager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      boolean isDebug = table.getOptions().get(StatusTable.OPTION_DEBUG_USER);
      table.setColumnDescriptors(colDescs);
      if (isDebug) {
	table.setSummaryInfo(getSummary(pollManager));
      }
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
      ArchivalUnit au = voter.getAu();
      row.put(COL_AU_ID, makeAuRef(au, ArchivalUnitStatus.AU_STATUS_TABLE_NAME));
      row.put(COL_CALLER, voter.getPollerId().getIdString());
      row.put(COL_STATUS, voter.getStatusString());
      row.put(COL_START, voter.getCreateTime());
      row.put(COL_DEADLINE, voter.getDeadline());
      String skey = PollUtil.makeShortPollKey(voter.getKey());
      row.put(COL_POLL_ID, new StatusTable.Reference(skey,
						  VOTER_DETAIL_TABLE_NAME,
						  voter.getKey()));
      if (voter.isPollActive()) {
	row.put(SORT_KEY1, SORT_BASE_ACTIVE);
	row.put(SORT_KEY2, voter.getCreateTime());
      } else {
	row.put(SORT_KEY1, SORT_BASE_DONE);
	row.put(SORT_KEY2, voter.getDeadline());
      }
      return row;
    }

    private List getSummary(PollManager pollManager) {
      List summary = new ArrayList();
      if (!CurrentConfig.getBooleanParam(V3PollFactory.PARAM_ENABLE_V3_VOTER,
					 V3PollFactory.DEFAULT_ENABLE_V3_VOTER)) { 
	summary.add(new StatusTable.SummaryInfo("Voting is disabled",
						ColumnDescriptor.TYPE_STRING,
						null));
      }
      StringBuilder sb = new StringBuilder();
      sb.append(pollManager.getEventCount(EventCtr.Accepted));
      sb.append(" accepted, ");
      int declined = pollManager.getEventCount(EventCtr.Declined);
      sb.append(declined);
      sb.append(" declined");
      if (declined != 0) {
	sb.append(" (");
	addNak(sb, PollNak.NAK_NO_AU, true);
	addNak(sb, PollNak.NAK_NOT_CRAWLED);
	addNak(sb, PollNak.NAK_PLUGIN_VERSION_MISMATCH);
	addNak(sb, PollNak.NAK_NO_TIME);
	addNak(sb, PollNak.NAK_TOO_MANY_VOTERS);
	addNak(sb, PollNak.NAK_HAVE_SUFFICIENT_REPAIRERS);
	sb.append(")");
      }
      summary.add(new StatusTable.SummaryInfo("Invitations",
					      ColumnDescriptor.TYPE_STRING,
					      sb.toString()));

      sb = new StringBuilder();
      int votes = pollManager.getEventCount(EventCtr.Voted);
      sb.append(votes);
      int noReceipt =
	votes - pollManager.getEventCount(EventCtr.ReceivedVoteReceipt);
      if (noReceipt != 0) {
	sb.append(" (");
	sb.append(noReceipt);
	sb.append(" no receipt)");
      }
      summary.add(new StatusTable.SummaryInfo("Votes",
					      ColumnDescriptor.TYPE_STRING,
					      sb.toString()));

      return summary;
    }

    void addNak(StringBuilder sb, PollNak nak) {
      addNak(sb, nak, false);
    }

    void addNak(StringBuilder sb, PollNak nak, boolean first) {
      int cnt = pollManager.getVoterNakEventCount(nak);
      if (cnt != 0) {
	if (!first) sb.append(", ");
	sb.append(cnt);
	sb.append(" ");
	sb.append(nak);
      }
    }
  }



  public static class PollOverview
    extends V3PollerStatus implements OverviewAccessor {

    public PollOverview(PollManager pollManager) {
      super(pollManager);
    }

    public Object getOverview(String tableName, BitSet options) {
      if (!CurrentConfig.getBooleanParam(V3PollFactory.PARAM_ENABLE_V3_POLLER,
					 V3PollFactory.DEFAULT_ENABLE_V3_POLLER)) { 
	return "Polling Disabled";
      }
      StringBuilder sb = new StringBuilder();
      sb.append(StringUtil.numberOfUnits(pollManager.getNumActiveV3Polls(),
					 "active poll", "active polls"));
      addEndStatus(sb, V3Poller.POLLER_STATUS_COMPLETE);
      addEndStatus(sb, V3Poller.POLLER_STATUS_NO_QUORUM);
      addEndStatus(sb, V3Poller.POLLER_STATUS_NO_TIME, "too busy");
      return new StatusTable.Reference(sb.toString(), POLLER_STATUS_TABLE_NAME);
    }
  }

  public static class VoterOverview
    extends V3PollerStatus implements OverviewAccessor {

    public VoterOverview(PollManager pollManager) {
      super(pollManager);
    }

    public Object getOverview(String tableName, BitSet options) {
      if (!CurrentConfig.getBooleanParam(V3PollFactory.PARAM_ENABLE_V3_VOTER,
					 V3PollFactory.DEFAULT_ENABLE_V3_VOTER)) { 
	return "Voting disabled";
      }
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
	case STATUS_ABORTED:
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
   * poller.  Requires the PollID as a key.</p>
   *
   */
  public static class V3PollerStatusDetail
      extends V3PollStatus implements StatusAccessor {

    static final String TABLE_TITLE = "V3 Poll Status";
    static final String COL_IDENTITY = "identity";
    static final String COL_PEER_STATUS = "peerStatus";
    static final String COL_AGREEMENT = "agreement";
    static final String COL_W_AGREEMENT = "w.agreement";
    static final String COL_NUM_AGREE = "numagree";
    static final String COL_W_NUM_AGREE = "w.numagree";
    static final String COL_NUM_DISAGREE = "numdisagree";
    static final String COL_W_NUM_DISAGREE = "w.numdisagree";
    static final String COL_NUM_POLLER_ONLY = "numpolleronly";
    static final String COL_W_NUM_POLLER_ONLY = "w.numpolleronly";
    static final String COL_NUM_VOTER_ONLY = "numvoteronly";
    static final String COL_W_NUM_VOTER_ONLY = "w.numvoteronly";
    static final String COL_BYTES_HASHED = "byteshashed";
    static final String COL_BYTES_READ = "bytesread";
    static final String COL_STATE = "state";
    static final String COL_WHEN = "when";

    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("sort", true),
		    new StatusTable.SortRule(COL_IDENTITY,
                                             CatalogueOrderComparator.SINGLETON));
    private final static String FOOT_AGREE_PRE_REPAIR =
      "Agreement values and URL counts are not updated to reflect repairs. " +
      "See org.lockss.poll.v3.recordPeerUrlLists.";

    private final static String FOOT_AGREE_POST_REPAIR =
      "Agreement values and URL counts/lists are updated to reflect any repairs.";

    private ColumnDescriptor AGREE_COLDESC_PRE_REPAIR =
      new ColumnDescriptor(COL_AGREEMENT, "Agreement",
			   ColumnDescriptor.TYPE_AGREEMENT,
			   FOOT_AGREE_PRE_REPAIR);

    private ColumnDescriptor AGREE_COLDESC_POST_REPAIR =
      new ColumnDescriptor(COL_AGREEMENT, "Agreement",
			   ColumnDescriptor.TYPE_AGREEMENT,
			   FOOT_AGREE_POST_REPAIR);

    private final List<ColumnDescriptor> colDescs =
      ListUtil.list(new ColumnDescriptor(COL_IDENTITY, "Peer",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_PEER_STATUS, "Status",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_AGREEMENT, "Agreement",
                                         ColumnDescriptor.TYPE_AGREEMENT),
                    new ColumnDescriptor(COL_W_AGREEMENT, "WAgreement",
                                         ColumnDescriptor.TYPE_AGREEMENT),
                    new ColumnDescriptor(COL_NUM_AGREE, "Agreeing URLs",
                                         ColumnDescriptor.TYPE_INT),
                    new ColumnDescriptor(COL_W_NUM_AGREE, "WAgreeing URLs",
                                         ColumnDescriptor.TYPE_FLOAT),
                    new ColumnDescriptor(COL_NUM_DISAGREE, "Disagreeing URLs",
                                         ColumnDescriptor.TYPE_INT),
                    new ColumnDescriptor(COL_W_NUM_DISAGREE, "WDisagreeing URLs",
                                         ColumnDescriptor.TYPE_FLOAT),
                    new ColumnDescriptor(COL_NUM_POLLER_ONLY, "Poller-only URLs",
                                         ColumnDescriptor.TYPE_INT),
                    new ColumnDescriptor(COL_W_NUM_POLLER_ONLY, "WPoller-only URLs",
                                         ColumnDescriptor.TYPE_FLOAT),
                    new ColumnDescriptor(COL_NUM_VOTER_ONLY, "Voter-only URLs",
                                         ColumnDescriptor.TYPE_INT),
                    new ColumnDescriptor(COL_W_NUM_VOTER_ONLY, "WVoter-only URLs",
                                         ColumnDescriptor.TYPE_FLOAT),
                    new ColumnDescriptor(COL_BYTES_HASHED, "Bytes Hashed",
                                         ColumnDescriptor.TYPE_INT),
                    new ColumnDescriptor(COL_BYTES_READ, "Bytes Read",
                                         ColumnDescriptor.TYPE_INT),
                    new ColumnDescriptor(COL_STATE, "PSM State",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_WHEN, "When",
                                         ColumnDescriptor.TYPE_DATE));

    private static final List<String> defaultCols =
      ListUtil.list(COL_IDENTITY,
		    COL_PEER_STATUS,
		    COL_AGREEMENT,
		    COL_W_AGREEMENT,
		    COL_NUM_AGREE,
		    COL_W_NUM_AGREE,
		    COL_NUM_DISAGREE,
		    COL_W_NUM_DISAGREE,
		    COL_NUM_POLLER_ONLY,
		    COL_W_NUM_POLLER_ONLY,
		    COL_NUM_VOTER_ONLY,
		    COL_W_NUM_VOTER_ONLY);


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
      table.setSummaryInfo(getSummary(poll, table));
      table.setTitle("Status of Poll " + key);
      if (!poll.isLocalPoll()) {
	table.setColumnDescriptors(getColDescs(poll),
				   getDefaultCols(table, poll));
	table.setDefaultSortRules(sortRules);
	table.setRows(getRows(table, poll));
      }
    }

    private List<ColumnDescriptor> getColDescs(V3Poller poller) {
      List<ColumnDescriptor> res = new ArrayList<ColumnDescriptor>();
      for (ColumnDescriptor desc : colDescs) {
	switch (desc.getColumnName()) {
	case COL_AGREEMENT:
	  if (poller.isRecordPeerUrlLists()) {
	    res.add(AGREE_COLDESC_POST_REPAIR);
	  } else {
	    res.add(AGREE_COLDESC_PRE_REPAIR);
	  }
	  break;
	default:
	  res.add(desc);
	}
      }
      return res;
    }


    private List<String> getDefaultCols(StatusTable table, V3Poller poll) {
      List<String> res = new LinkedList<String>();
      res.addAll(defaultCols);
      if (!poll.hasResultWeightMap()) {
	for (ListIterator<String> iter = res.listIterator(); iter.hasNext();) {
	  if (iter.next().startsWith("w.")) {
	    iter.remove();
	  }	    
	}
      }
      if (poll.isEnableHashStats()) {
	res.add(COL_BYTES_HASHED);
	res.add(COL_BYTES_READ);
      }
      if (table.getOptions().get(StatusTable.OPTION_DEBUG_USER)) {
	res.add(COL_STATE);
	res.add(COL_WHEN);
      }
      return res;
    }

    private List getRows(StatusTable table, V3Poller poll) {
      boolean isDebug = table.getOptions().get(StatusTable.OPTION_DEBUG_USER);
      List rows = new ArrayList();
      for (ParticipantUserData voter : poll.getParticipants()) {
        rows.add(makeRow(poll, voter, "0", isDebug));
      }
      for (ParticipantUserData voter : poll.getExParticipants()) {
        rows.add(makeRow(poll, voter, "1", isDebug));
      }
      return rows;
    }

    private Map makeRow(V3Poller poll, ParticipantUserData voter,
			Object sort, boolean isDebug) {
      Map row = new HashMap();
      PeerIdentity peer = voter.getVoterId();
      row.put(COL_IDENTITY,
	      isDebug ? makeVoteRef(peer.getIdString(), peer, poll.getKey())
	      : peer.getIdString());
      row.put(COL_PEER_STATUS, voter.getStatusString());
      row.put("sort", sort);
      if (voter.hasVoted()) {
	ParticipantUserData.VoteCounts voteCounts = voter.getVoteCounts();
	row.put(COL_AGREEMENT, voteCounts.getPercentAgreement());
	row.put(COL_NUM_AGREE,
		participantDataRef(voteCounts.getAgreedVotes(),
				   poll, voter,
				   PEER_AGREE_URLS_TABLE_NAME));
	row.put(COL_NUM_DISAGREE,
		participantDataRef(voteCounts.getDisagreedVotes(),
				   poll, voter,
				   PEER_DISAGREE_URLS_TABLE_NAME));
	row.put(COL_NUM_POLLER_ONLY,
		participantDataRef(voteCounts.getPollerOnlyVotes(),
				   poll, voter,
				   PEER_POLLER_ONLY_URLS_TABLE_NAME));
	row.put(COL_NUM_VOTER_ONLY,
		participantDataRef(voteCounts.getVoterOnlyVotes(),
				   poll, voter,
				   PEER_VOTER_ONLY_URLS_TABLE_NAME));
	row.put(COL_BYTES_HASHED, voter.getBytesHashed());
	row.put(COL_BYTES_READ, voter.getBytesRead());
	if (poll.hasResultWeightMap()) {
	  row.put(COL_W_AGREEMENT, voteCounts.getWeightedPercentAgreement());
	  row.put(COL_W_NUM_AGREE, voteCounts.getWeightedAgreedVotes());
	  row.put(COL_W_NUM_DISAGREE, voteCounts.getWeightedDisagreedVotes());
	  row.put(COL_W_NUM_POLLER_ONLY, voteCounts.getWeightedPollerOnlyVotes());
	  row.put(COL_W_NUM_VOTER_ONLY, voteCounts.getWeightedVoterOnlyVotes());
	}
      }
      PsmInterp interp = voter.getPsmInterp();
      if (interp != null) {
	PsmState state = interp.getCurrentState();
	if (state != null) {
	  row.put(COL_STATE, state.getName());
	  long when = interp.getLastStateChange();
	  if (when > 0) {
	    row.put(COL_WHEN, when);
	  }
	}
      }	
      return row;
    }


    private static Object participantDataRef(Object value,
					     V3Poller poll,
					     ParticipantUserData voter,
					     String table) {
      if (voter.getVoteCounts().hasPeerUrlLists()) {
	return new StatusTable.Reference(value, table,
					 poll.getKey() + "|" + voter.getVoterId());
      } else {
	return value;
      }
    }

    private List getSummary(V3Poller poll, StatusTable table) {
      boolean isDebug = table.getOptions().get(StatusTable.OPTION_DEBUG_USER);
      PollerStateBean pollerState = poll.getPollerStateBean();
      List summary = new ArrayList();
      summary.add(new SummaryInfo("AU Name",
                                  ColumnDescriptor.TYPE_STRING,
				  makeAuRef(poll.getAu(),
					    ArchivalUnitStatus.AU_STATUS_TABLE_NAME)));
      summary.add(new SummaryInfo("Type",
                                  ColumnDescriptor.TYPE_STRING,
                                  poll.getPollVariant()));
      summary.add(new SummaryInfo("Status",
                                  ColumnDescriptor.TYPE_STRING,
                                  poll.getStatusString()));
      if (pollerState.getErrorDetail() != null) {
        summary.add(new SummaryInfo("Error",
                                    ColumnDescriptor.TYPE_STRING,
                                    pollerState.getErrorDetail()));
      }
      if (poll.getStatus() == POLLER_STATUS_COMPLETE) {
	if (!poll.isLocalPoll()) {
	  summary.add(new SummaryInfo("Agreement",
				      ColumnDescriptor.TYPE_AGREEMENT,
				      poll.getPercentAgreement()));
	  if (poll.hasResultWeightMap()) {
	    summary.add(new SummaryInfo("Weighted Agreement",
					ColumnDescriptor.TYPE_AGREEMENT,
					poll.getWeightedPercentAgreement()));
	  }
	}
      }
      if (isDebug && pollerState.getAdditionalInfo() != null) {
        summary.add(new SummaryInfo("Info",
                                    ColumnDescriptor.TYPE_STRING,
                                    pollerState.getAdditionalInfo()));
      }
      summary.add(new SummaryInfo("Start Time",
                                  ColumnDescriptor.TYPE_DATE,
                                  new Long(poll.getCreateTime())));
      if (!poll.isLocalPoll()) {
	summary.add(new SummaryInfo("Vote Deadline",
				    ColumnDescriptor.TYPE_DATE,
				    new Long(poll.getVoteDeadline())));
      }
      summary.add(new SummaryInfo("Duration",
                                  ColumnDescriptor.TYPE_TIME_INTERVAL,
                                  new Long(poll.getDuration())));
      if (poll.isPollActive()) {
	long remain = TimeBase.msUntil(poll.getDeadline().getExpirationTime());
	if (remain >= 0) {
	  summary.add(new SummaryInfo("Remaining",
				      ColumnDescriptor.TYPE_TIME_INTERVAL,
				      new Long(remain)));
	}
      } else if (!poll.getDeadline().equals(poll.getEndTime())) {
	summary.add(new SummaryInfo("Actual End",
				    ColumnDescriptor.TYPE_DATE,
				    poll.getEndTime()));
      }
      if (poll.getErrorUrls() != null && poll.getErrorUrls().size() > 0) {
        summary.add(new SummaryInfo("URLs with Hash errors",
                                    ColumnDescriptor.TYPE_STRING,
                                    new StatusTable.Reference(new Integer(poll.getErrorUrls().size()),
                                                              ERROR_TABLE_NAME,
                                                              poll.getKey())));
      }
      if (isDebug) {
	File stateDir = poll.getStateDir();
        if (stateDir != null) {
	  summary.add(new SummaryInfo("State Directory",
				      ColumnDescriptor.TYPE_STRING,
				      stateDir));
	}
      }
      if (!poll.isLocalPoll()) {
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
								AGREE_TABLE_NAME,
								poll.getKey())));
	}
	if (disagreeUrls > 0) {
	  summary.add(new SummaryInfo("Disagreeing URLs",
				      ColumnDescriptor.TYPE_INT,
				      new StatusTable.Reference(new Integer(disagreeUrls),
								DISAGREE_TABLE_NAME,
								poll.getKey())));
	}
	if (noQuorumUrls > 0) {
	  summary.add(new SummaryInfo("No Quorum URLs",
				      ColumnDescriptor.TYPE_INT,
				      new StatusTable.Reference(new Integer(noQuorumUrls),
								NO_QUORUM_TABLE_NAME,
								poll.getKey())));
	}
	if (tooCloseUrls > 0) {
	  summary.add(new SummaryInfo("Too Close URLs",
				      ColumnDescriptor.TYPE_INT,
				      new StatusTable.Reference(new Integer(tooCloseUrls),
								TOO_CLOSE_TABLE_NAME,
								poll.getKey())));
	}
	if (completedRepairs > 0) {
	  summary.add(new SummaryInfo("Completed Repairs",
				      ColumnDescriptor.TYPE_INT,
				      new StatusTable.Reference(new Integer(completedRepairs),
								COMPLETED_REPAIRS_TABLE_NAME,
								poll.getKey())));
	}
	if (activeRepairs > 0) {
	  String message = poll.isPollActive() ? "Queued Repairs" : "Incomplete Repairs";
	  summary.add(new SummaryInfo("Queued Repairs",
				      ColumnDescriptor.TYPE_INT,
				      new StatusTable.Reference(new Integer(activeRepairs),
								ACTIVE_REPAIRS_TABLE_NAME,
								poll.getKey())));
        
	}
      }
      LocalHashResult lhr = poll.getLocalHashResult();
      if (lhr != null) {
	int matchingUrls = lhr.getMatchingUrls();
	int newlySuspectUrls = lhr.getNewlySuspectUrls();
	int newlyHashedUrls = lhr.getNewlyHashedUrls();
	int skippedUrls = lhr.getSkippedUrls();
	summary.add(new SummaryInfo("LocalHash Checked URLs",
				    ColumnDescriptor.TYPE_INT,
				    new Integer(lhr.getTotalUrls())));
	if (matchingUrls > 0) {
	  summary.add(new SummaryInfo("LocalHash Matching URLs",
				      ColumnDescriptor.TYPE_INT,
				      new Integer(matchingUrls)));
	}
	if (newlySuspectUrls > 0) {
	  summary.add(new SummaryInfo("LocalHash Newly Suspect URLs",
				      ColumnDescriptor.TYPE_INT,
				      new Integer(newlySuspectUrls)));
	}
	if (newlyHashedUrls > 0) {
	  summary.add(new SummaryInfo("LocalHash Newly Hashed URLs",
				      ColumnDescriptor.TYPE_INT,
				      new Integer(newlyHashedUrls)));
	}
	if (skippedUrls > 0) {
	  summary.add(new SummaryInfo("LocalHash Already Suspect URLs",
				      ColumnDescriptor.TYPE_INT,
				      new Integer(skippedUrls)));
	}
      }
      if (poll.isEnableHashStats()) {
        summary.add(new SummaryInfo("Bytes Hashed",
                                    ColumnDescriptor.TYPE_INT,
                                    poll.getBytesHashed()));
        
        summary.add(new SummaryInfo("Bytes Read",
                                    ColumnDescriptor.TYPE_INT,
                                    poll.getBytesRead()));
        
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

  public static abstract class V3UrlList extends V3PollStatus
      implements StatusAccessor {
    
    static final String COL_URL = "url";
    
    protected final List sortRules =
      ListUtil.list(new StatusTable.SortRule(COL_URL,
                                             CatalogueOrderComparator.SINGLETON));
    protected static final List colDescs =
      ListUtil.list(new ColumnDescriptor(COL_URL, "URL",
                                         ColumnDescriptor.TYPE_STRING));
    public V3UrlList(PollManager manager) {
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
      table.setTitle(getTitle(poller));
      table.setColumnDescriptors(getColDescs());
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(poller));
    }

    protected List getRows(V3Poller poller) {
      List rows = new ArrayList();
      for (String url : getUrlList(poller)) {
        Map row = new HashMap();
        row.put(COL_URL, url);
        rows.add(row);
      }
      return rows;
    }

    protected String getTitle(V3Poller poller) {
      return getDisplayName() + " in poll " + poller.getKey();
    }

    protected List getColDescs() {
      return colDescs;
    }

    protected abstract Collection<String> getUrlList(V3Poller poller);

    public boolean requiresKey() {
      return true;
    }
  }

  public static abstract class V3RepairUrlList extends V3UrlList {

    static final String COL_REPAIR_FROM = "repairFrom";
    
    protected final List colDescs =
      ListUtil.list(new ColumnDescriptor(V3UrlList.COL_URL, "URL",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_REPAIR_FROM, "Repaired From",
                                         ColumnDescriptor.TYPE_STRING));

    public V3RepairUrlList(PollManager manager) {
      super(manager);
    }

    protected List getRows(V3Poller poller) {
      List rows = new ArrayList();
      for (PollerStateBean.Repair rp: getRepairBeans(poller)) {
        Map row = new HashMap();
        row.put(V3UrlList.COL_URL, rp.getUrl());
        if (rp.isPublisherRepair()) {
          row.put(COL_REPAIR_FROM, "Publisher");
        } else {
          row.put(COL_REPAIR_FROM, rp.getRepairFrom().getIdString());
        }
        rows.add(row);
      }
      return rows;
    }

    protected List getColDescs() {
      return colDescs;
    }

    protected Collection<String> getUrlList(V3Poller poller) {
      throw new UnsupportedOperationException("getUrlList shouldn't be called on repair list");
    }

    protected abstract Collection<PollerStateBean.Repair> getRepairBeans(V3Poller poller);
  }

  public static abstract class V3PeerUrlList extends V3UrlList {

    public V3PeerUrlList(PollManager manager) {
      super(manager);
    }

    public void populateTable(StatusTable table) throws NoSuchTableException {
      String key = table.getKey();
      List<String> keypair = StringUtil.breakAt(key, "|");
      String pollid = keypair.get(0);
      String peer = keypair.get(1);

      V3Poller poller = null;
      try {
        poller = (V3Poller)pollManager.getPoll(pollid);
      } catch (ClassCastException ex) {
        theLog.error("Expected V3Poller, but got " +
		     pollManager.getPoll(key).getClass().getName());
        return;
      }
      if (poller == null) return;

      PeerIdentity pid = null;
      try {
	pid = pollManager.getIdentityManager().findPeerIdentity(peer);
      } catch (IdentityManager.MalformedIdentityKeyException e) {
	theLog.warning("Malformed PID in peer URL table request: " + peer);
      }
      if (pid == null) return;

      table.setTitle(getTitle(poller, pid));
      table.setColumnDescriptors(colDescs);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(poller, pid));
    }

    protected List getRows(V3Poller poller, PeerIdentity pid) {
      List rows = new ArrayList();
      for (String url : getUrlList(poller, pid)) {
        Map row = new HashMap();
        row.put(V3UrlList.COL_URL, url);
        rows.add(row);
      }
      return rows;
    }

    protected String getTitle(V3Poller poller, PeerIdentity pid) {
      return getDisplayName() + " for peer " + pid +
	" in poll " + poller.getKey();
    }

    protected Collection<String> getUrlList(V3Poller poller) {
      throw new UnsupportedOperationException("getUrlList(V3Poller) shouldn't be called on peer list");
    }

    protected abstract Collection<String> getUrlList(V3Poller poller,
						     PeerIdentity pid);

  }

  public static class V3PeerAgreeURLs extends V3PeerUrlList {
    static final String TABLE_TITLE = "V3 Poll Details - Agreeing URLs";

    public V3PeerAgreeURLs(PollManager manager) {
      super(manager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    protected Collection<String> getUrlList(V3Poller poller, PeerIdentity pid) {
      ParticipantUserData voter = poller.getParticipant(pid);
      return voter.getVoteCounts().getAgreedUrls();
    }
  }

  public static class V3PeerDisagreeURLs extends V3PeerUrlList {
    static final String TABLE_TITLE = "V3 Poll Details - Disagreeing URLs";

    public V3PeerDisagreeURLs(PollManager manager) {
      super(manager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    protected Collection<String> getUrlList(V3Poller poller, PeerIdentity pid) {
      ParticipantUserData voter = poller.getParticipant(pid);
      return voter.getVoteCounts().getDisagreedUrls();
    }
  }

  public static class V3PeerPollerOnlyURLs extends V3PeerUrlList {
    static final String TABLE_TITLE = "V3 Poll Details - Poller Only URLs";

    public V3PeerPollerOnlyURLs(PollManager manager) {
      super(manager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    protected Collection<String> getUrlList(V3Poller poller, PeerIdentity pid) {
      ParticipantUserData voter = poller.getParticipant(pid);
      return voter.getVoteCounts().getPollerOnlyUrls();
    }
  }
  
  public static class V3PeerVoterOnlyURLs extends V3PeerUrlList {
    static final String TABLE_TITLE = "V3 Poll Details - Voter Only URLs";

    public V3PeerVoterOnlyURLs(PollManager manager) {
      super(manager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    protected Collection<String> getUrlList(V3Poller poller, PeerIdentity pid) {
      ParticipantUserData voter = poller.getParticipant(pid);
      return voter.getVoteCounts().getVoterOnlyUrls();
    }
  }
  
  public static class V3ActiveRepairs extends V3RepairUrlList {
    static final String TABLE_TITLE = "V3 Repairs (Active)";

    public V3ActiveRepairs(PollManager manager) {
      super(manager);
    }

    protected Collection<PollerStateBean.Repair> getRepairBeans(V3Poller poller) {
      return poller.getActiveRepairs();
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }
  }

  public static class V3CompletedRepairs extends V3RepairUrlList {
    static final String TABLE_TITLE = "V3 Repairs (Completed)";

    public V3CompletedRepairs(PollManager manager) {
      super(manager);
    }

    protected Collection<PollerStateBean.Repair> getRepairBeans(V3Poller poller) {
      return poller.getCompletedRepairs();
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }
  }

  public static class V3AgreeURLs extends V3UrlList {
    static final String TABLE_TITLE = "V3 Poll Details - Agreeing URLs";

    public V3AgreeURLs(PollManager manager) {
      super(manager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    protected Collection<String> getUrlList(V3Poller poller) {
      return poller.getAgreedUrls();
    }
  }

  public static class V3DisagreeURLs extends V3UrlList {
    static final String TABLE_TITLE = "V3 Poll Details - Disagreeing URLs";

    public V3DisagreeURLs(PollManager manager) {
      super(manager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    protected Collection<String> getUrlList(V3Poller poller) {
      return poller.getDisagreedUrls();
    }
  }

  public static class V3NoQuorumURLs extends V3UrlList {
    static final String TABLE_TITLE = "V3 Poll Details - No Quorum URLs";

    public V3NoQuorumURLs(PollManager manager) {
      super(manager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    protected Collection<String> getUrlList(V3Poller poller) {
      return poller.getNoQuorumUrls();
    }
  }

  public static class V3TooCloseURLs extends V3UrlList {
    static final String TABLE_TITLE = "V3 Poll Details - Too Close URLs";

    public V3TooCloseURLs(PollManager manager) {
      super(manager);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    protected Collection<String> getUrlList(V3Poller poller) {
      return poller.getTooCloseUrls();
    }
  }

  public static class V3ErrorURLs extends V3PollStatus 
      implements StatusAccessor {
    static final String TABLE_TITLE = "V3 Poll Details - URLs with Hash Errors";
    static final String COL_ERROR = "erorr"; // FIXME
    
    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule(V3UrlList.COL_URL,
                                             CatalogueOrderComparator.SINGLETON));
    private final List colDescs =
      ListUtil.list(new ColumnDescriptor(V3UrlList.COL_URL, "URL",
                                         ColumnDescriptor.TYPE_STRING),
                    new ColumnDescriptor(COL_ERROR, "Error",
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
      table.setTitle("V3 Poll Details - URLs with Hash Errors in Poll " + poller.getKey());
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
          row.put(V3UrlList.COL_URL, url);
          row.put(COL_ERROR, exceptionMessage);
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
      table.setSummaryInfo(getSummary(voter, table));
      table.setTitle("Status of Vote in Poll " + key);
    }

    private List getSummary(V3Voter voter, StatusTable table) {
      boolean isDebug = table.getOptions().get(StatusTable.OPTION_DEBUG_USER);
      VoterUserData userData = voter.getVoterUserData();
      List summary = new ArrayList();
      summary.add(new SummaryInfo("AU Name",
                                  ColumnDescriptor.TYPE_STRING,
				  makeAuRef(voter.getAu(),
					    ArchivalUnitStatus.AU_STATUS_TABLE_NAME)));
      summary.add(new SummaryInfo("Status",
                                  ColumnDescriptor.TYPE_STRING,
                                  voter.getStatusString()));

      if (isDebug) {
	File stateDir = voter.getStateDir();
        if (stateDir != null) {
	  summary.add(new SummaryInfo("State Directory",
				      ColumnDescriptor.TYPE_STRING,
				      stateDir));
        }
	PsmInterp interp = voter.getPsmInterp();
	if (interp != null) {
	  PsmState state = interp.getCurrentState();
	  if (state != null) {
	    summary.add(new SummaryInfo("PSM State",
					ColumnDescriptor.TYPE_STRING,
					state.getName()));
	  }
	}
      }
      if (userData.getErrorDetail() != null) {
        summary.add(new SummaryInfo("Error",
                                    ColumnDescriptor.TYPE_STRING,
                                    userData.getErrorDetail()));
      }
      PeerIdentity peer = voter.getPollerId();
      Object caller = isDebug
	? makePollRef(peer.getIdString(), peer, table.getKey())
	: peer.getIdString();
      summary.add(new SummaryInfo("Caller",
                                  ColumnDescriptor.TYPE_STRING,
				  caller));
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
      if (voter.getStatus() == STATUS_COMPLETE) {
	if (userData.hasReceivedHint()) {
	  summary.add(new SummaryInfo("Agreement",
				      ColumnDescriptor.TYPE_AGREEMENT,
				      userData.getAgreementHint()));
	}
	if (userData.hasReceivedWeightedHint()) {
	  summary.add(new SummaryInfo("Weighted Agreement",
				      ColumnDescriptor.TYPE_AGREEMENT,
				      userData.getWeightedAgreementHint()));
	}
	if (userData.hasReceivedSymmetricAgreement()) {
	  summary.add(new SummaryInfo("Symmetric Agreement",
				      ColumnDescriptor.TYPE_AGREEMENT,
				      userData.getSymmetricAgreement()));
	}
	if (userData.hasReceivedSymmetricWeightedAgreement()) {
	  summary.add(new SummaryInfo("Symmetric Weighted Agreement",
				      ColumnDescriptor.TYPE_AGREEMENT,
				      userData.getSymmetricWeightedAgreement()));
	}
      }
//       if (voter.hasResultWeightMap()) {
// 	summary.add(new SummaryInfo("Weighted Agreement",
// 				    ColumnDescriptor.TYPE_AGREEMENT,
// 				    userData.getWeightedAgreementHint()));
//       }
      summary.add(new SummaryInfo("Poller Nonce",
                                  ColumnDescriptor.TYPE_STRING,
                                  ByteArray.toBase64(voter.getPollerNonce())));
      summary.add(new SummaryInfo("Voter Nonce",
                                  ColumnDescriptor.TYPE_STRING,
                                  ByteArray.toBase64(voter.getVoterNonce())));
      if (userData.isSymmetricPoll()) {
	// Its a symmetric poll
	byte[] nonce2 = userData.getVoterNonce2();
	summary.add(new SummaryInfo("Voter Nonce2",
				    ColumnDescriptor.TYPE_STRING,
				    ByteArray.toBase64(nonce2)));
	if (voter.getStatus() == STATUS_COMPLETE) {
	  summary.add(new SummaryInfo("Agreeing URLs",
				      ColumnDescriptor.TYPE_INT,
				      userData.getNumAgreeUrl()));
	  summary.add(new SummaryInfo("Disagreeing URLs",
				      ColumnDescriptor.TYPE_INT,
				      userData.getNumDisagreeUrl()));
	  summary.add(new SummaryInfo("Voter only URLs",
				      ColumnDescriptor.TYPE_INT,
				      userData.getNumVoterOnlyUrl()));
	  summary.add(new SummaryInfo("Poller only URLs",
				      ColumnDescriptor.TYPE_INT,
				      userData.getNumPollerOnlyUrl()));
	}
      }
	
	
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
