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

package org.lockss.poller;

import java.util.*;

import org.apache.commons.lang3.mutable.MutableInt;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.plugin.*;
import org.lockss.poller.v3.*;
import org.lockss.protocol.*;

/**
 * Description: Provides support for the PollManager and Polls to present
 * the current status information
 * @author Claire Griffin
 * @version 1.0
 */

public class PollerStatus {
  public static final String MANAGER_STATUS_TABLE_NAME = "PollManagerTable";
  public static final String POLL_STATUS_TABLE_NAME = "PollTable";

  protected PollManager pollManager;
  
  private static final Logger theLog = Logger.getLogger(PollerStatus.class);

  PollerStatus(PollManager pollManager) {
    this.pollManager = pollManager;
  }

  static class ManagerStatus
    extends PollerStatus implements StatusAccessor {
    
    static final String TABLE_NAME = MANAGER_STATUS_TABLE_NAME;
    static final String COL_AU_NAME = "AuName";
    static final String COL_URL = "URL";
    static final String COL_RANGE = "Range";
    static final String COL_POLL_TYPE = "PollType";
    static final String COL_STATUS = "Status";
    static final String COL_DEADLINE = "Deadline";
    static final String COL_POLL_ID = "PollID";

    private static String POLLMANAGER_TABLE_TITLE = "V1 Polls";

    static final int STRINGTYPE = ColumnDescriptor.TYPE_STRING;
    static final int DATETYPE = ColumnDescriptor.TYPE_DATE;

    private static final List sortRules =
      ListUtil.list(
		    new StatusTable.SortRule(COL_AU_NAME, CatalogueOrderComparator.SINGLETON),
//		    new StatusTable.SortRule("URL", true),
		    new StatusTable.SortRule(COL_DEADLINE, false)
		    );
    private static final List columnDescriptors =
        ListUtil.list(
		      new ColumnDescriptor(COL_AU_NAME, "AU Name", STRINGTYPE),
		      new ColumnDescriptor(COL_URL, "URL", STRINGTYPE),
		      new ColumnDescriptor(COL_RANGE, "Range", STRINGTYPE),
		      new ColumnDescriptor(COL_POLL_TYPE, "Type", STRINGTYPE),
		      new ColumnDescriptor(COL_STATUS, "Status", STRINGTYPE),
		      new ColumnDescriptor(COL_DEADLINE, "Deadline", DATETYPE),
		      new ColumnDescriptor(COL_POLL_ID, "Poll ID", STRINGTYPE)
		      );

    static final String TITLE_FOOT =
      "To see the polls for a single AU, go to the Archival Units table and " +
      "follow the Polls link from the desired AU.";

    public ManagerStatus(PollManager pollManager) {
      super(pollManager);
    }

    public String getDisplayName() {
      return POLLMANAGER_TABLE_TITLE;
    }

    public void populateTable(StatusTable table) throws StatusService.
        NoSuchTableException {
      String key = table.getKey();
      Properties props = PropUtil.canonicalEncodedStringToProps(key);
      PollCounts cnts = new PollCounts();
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(columnDescriptors);
	table.setDefaultSortRules(sortRules);
	table.setRows(processPolls(props, cnts, true));
      } else {
	// still need to process all polls to compute totals
	processPolls(props, cnts, false);
      }
      table.setTitle(getTitle(props));
      // add how-to-filter-by-AU footnote iff not already filtering by AU,
      // and more than one AU is in table
      if (!props.containsKey("AU") && cnts.getAuIds().size() > 1) {
	table.setTitleFootnote(TITLE_FOOT);
      }
      table.setSummaryInfo(getSummary(props, cnts));
    }

    public boolean requiresKey() {
      return false;
    }

    private List processPolls(Properties props, PollCounts cnts,
			      boolean generateRows)
	throws StatusService.NoSuchTableException {
      ArrayList rowL = new ArrayList();
      // XXX: V3
      Collection c = pollManager.getV1Polls();
      for (Iterator it = c.iterator(); it.hasNext(); ) {
        PollManager.PollManagerEntry entry =
	  (PollManager.PollManagerEntry)it.next();
	cnts.incrAuIdCnt(entry.getPollSpec().getAuId());
        if (matchAu(entry, props)) {
	  // include in counts if poll's AU matches filter
	  cnts.incrStatusCnt(entry.getStatusString());
	  if (generateRows && matchKey(entry, props)) {
	    // include row only if all filters match
	    rowL.add(makeRow(entry));
	  }
	}
      }
      return rowL;
    }

    private String getTypeCharString(int pollType) {
      switch(pollType) {
        case Poll.V1_NAME_POLL:
          return "N";
        case Poll.V1_CONTENT_POLL:
          return "C";
        case Poll.V1_VERIFY_POLL:
          return "V";
        default:
          return "Unknown";
      }
    }

    private Map makeRow(PollManager.PollManagerEntry entry) {
      HashMap rowMap = new HashMap();
      PollSpec spec = entry.getPollSpec();
      //"AuName"
      rowMap.put(COL_AU_NAME, spec.getCachedUrlSet().getArchivalUnit().getName());
      //"URL"
      rowMap.put(COL_URL, spec.getUrl());
      //"Range"
      rowMap.put(COL_RANGE, spec.getRangeString());
      //"PollType"
      rowMap.put(COL_POLL_TYPE, getTypeCharString(entry.getType()));
      //"Status"
      rowMap.put(COL_STATUS, entry.getStatusString());
      //"Deadline"
      if (entry.getPollDeadline() != null) {
        rowMap.put(COL_DEADLINE, entry.getPollDeadline());
      }
      //"PollID"
      rowMap.put(COL_POLL_ID, PollStatus.makePollRef(entry.getShortKey(),
						  entry.getKey()));
      return rowMap;
    }

    private boolean matchAu(PollManager.PollManagerEntry entry,
			    Properties props) {
      PollSpec spec = entry.getPollSpec();
      String val = props.getProperty("AU");
      return (val == null || spec.getAuId().equals(val));
    }

    private boolean matchKey(PollManager.PollManagerEntry entry,
			     Properties props) {
      PollSpec spec = entry.getPollSpec();
      for (Iterator iter = props.keySet().iterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
	String val = props.getProperty(key);
	if (key.equals("AU")) {
	  if (!spec.getAuId().equals(val)) {
	    return false;
	  }
	}
	else if (key.equals(COL_URL)) {
	  if (!spec.getUrl().equals(val)) {
	    return false;
	  }
	}
	else if (key.equals(COL_POLL_TYPE)) {
	  if (!entry.getTypeString().equals(val)) {
	    return false;
	  }
	}
	else if (key.equals(COL_STATUS)) {
	  if (!entry.getStatusString().equals(val)) {
	    return false;
	  }
	}
      }
      return true;
    }

    private List getSummary(Properties props, PollCounts cnts) {
      List res = new ArrayList();
      List statusTypes = new ArrayList(cnts.getStatusTypes());
      if (statusTypes.isEmpty()) {
	return null;
      }
      Collections.sort(statusTypes);
      LinkedList list = new LinkedList();
      int total = 0;
      for (Iterator iter = statusTypes.iterator(); iter.hasNext(); ) {
	String type = (String)iter.next();
	int cnt = cnts.getStatusCnt(type);
	if (cnt > 0) {
	  total += cnt;
	  Properties combinedProps = PropUtil.copy(props);
	  combinedProps.setProperty(COL_STATUS, type);
	  list.add(makeRef((cnt + " " + type), combinedProps));
	  if (iter.hasNext()) {
	    list.add(", ");
	  }
	}
      }
      Properties combinedProps = PropUtil.copy(props);
      combinedProps.remove(COL_STATUS);
      list.addFirst(": ");
      list.addFirst(makeRef((total + " Total"), combinedProps));
      res.add(new StatusTable.SummaryInfo("Poll Summary",
					  ColumnDescriptor.TYPE_STRING,
					  list));
      return res;
    }

    public String getTitle(Properties props) {
      if (props.isEmpty()) {
	return "All Recent Polls";
      }
      // generate string: {type}, {status} Polls {for AU}, {on URL}
      List prefix = new ArrayList();
      List suffix = new ArrayList();
      String type = props.getProperty(COL_POLL_TYPE);
      if (type != null) {
	prefix.add(type);
      }
      String status = props.getProperty(COL_STATUS);
      if (status != null) {
	prefix.add(status);
      }
      String auid = props.getProperty("AU");
      if (auid != null) {
	String name = auid;
	LockssDaemon daemon = pollManager.getDaemon();
	if (daemon != null) {
	  ArchivalUnit au = daemon.getPluginManager().getAuFromId(auid);
	  if (au != null) {
	    name = au.getName();
	  }
	}
	suffix.add("for " + name);
      }
      String url = props.getProperty(COL_URL);
      if (url != null) {
	suffix.add("on " + url);
      }
      StringBuilder sb = new StringBuilder();
      StringUtil.separatedString(prefix, "", ", ", " ", sb);
      sb.append("Polls");
      StringUtil.separatedString(suffix, " ", ", ", "", sb);
      return sb.toString();
    }

    // utility methods for making a Reference

    public StatusTable.Reference makeRef(Object value,
					 String keyName, String key) {
      Properties props = PropUtil.fromArgs(keyName, key);
      return makeRef(value, props);
    }

    public StatusTable.Reference makeRef(Object value, Properties props) {
      String propstr = PropUtil.propsToCanonicalEncodedString(props);
      return new StatusTable.Reference(value, TABLE_NAME, propstr);
    }

    public StatusTable.Reference makeAURef(Object value, String key) {
      return makeRef(value, "AU", key);
    }

    public StatusTable.Reference makeURLRef(Object value, String key) {
      return makeRef(value, COL_URL, key);
    }

    public StatusTable.Reference makePollTypeRef(Object value, String key) {
      return makeRef(value, COL_POLL_TYPE, key);
    }

    public StatusTable.Reference makeStatusRef(Object value, String key) {
      return makeRef(value, COL_STATUS, key);
    }
  }

  static class PollCounts {
    private Map statusCnts = new HashMap();
    private Map auCnts = new HashMap();

    void incrStatusCnt(String status) {
      MutableInt n = (MutableInt)statusCnts.get(status);
      if (n == null) {
	n = new MutableInt();
	statusCnts.put(status, n);
      }
      n.add(1);
    }

    void incrAuIdCnt(String auid) {
      MutableInt n = (MutableInt)auCnts.get(auid);
      if (n == null) {
	n = new MutableInt();
	auCnts.put(auid, n);
      }
      n.add(1);
    }

    int getStatusCnt(String status) {
      MutableInt n = (MutableInt)statusCnts.get(status);
	return n == null ? 0 : n.intValue();
    }

    int getAuCnt(String auid) {
      MutableInt n = (MutableInt)auCnts.get(auid);
	return n == null ? 0 : n.intValue();
    }

    Set getStatusTypes() {
      return statusCnts.keySet();
    }

    Set getAuIds() {
      return auCnts.keySet();
    }
  }

  static class PollStatus extends PollerStatus implements StatusAccessor {

    static final String TABLE_NAME = POLL_STATUS_TABLE_NAME;
    static final String COL_IDENTITY = "Identity";
    static final String COL_REPUTATION = "Reputation";
    static final String COL_AGREE = "Agree";
    static final String COL_CHALLENGE = "Challenge";
    static final String COL_VERIFIER = "Verifier";
    static final String COL_HASH = "Hash";

    static final int INTTYPE = ColumnDescriptor.TYPE_INT;
    static final int STRINGTYPE = ColumnDescriptor.TYPE_STRING;

    private static final List columnDescriptors =
        ListUtil.list(new ColumnDescriptor(COL_IDENTITY, "Identity", STRINGTYPE),
        new ColumnDescriptor(COL_REPUTATION, "Reputation", INTTYPE),
        new ColumnDescriptor(COL_AGREE, "Agree", STRINGTYPE),
        new ColumnDescriptor(COL_CHALLENGE, "Challenge", STRINGTYPE),
        new ColumnDescriptor(COL_VERIFIER, "Verifier", STRINGTYPE),
        new ColumnDescriptor(COL_HASH, "Hash", STRINGTYPE)
        );

    private static final List sortRules =
        ListUtil.list(new StatusTable.SortRule(COL_IDENTITY, true));

    public PollStatus(PollManager pollManager) {
      super(pollManager);
    }

    public String getDisplayName() {
      throw new
	UnsupportedOperationException("Poll table has no generic title");
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      String key = table.getKey();
      BasePoll poll = getPoll(key);
      table.setTitle(getTitle(key));
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(columnDescriptors);
	table.setDefaultSortRules(sortRules);
	table.setRows(getRows(poll));
      }
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
      List list = new ArrayList();
      PollManager.PollManagerEntry entry =
	pollManager.getCurrentOrRecentV1PollEntry(poll.getKey());

      list.add(new StatusTable.SummaryInfo("AU Name" , STRINGTYPE,
					   tally.getArchivalUnit().getName()));
      if (entry != null) {
	list.add(new StatusTable.SummaryInfo("Status" , STRINGTYPE,
 					   entry.getStatusString()));
      }
      list.add(new StatusTable.SummaryInfo("Type" , STRINGTYPE,
					   getPollType(poll)));
      list.add(new StatusTable.SummaryInfo("Target" , STRINGTYPE,
					   getPollSpecString(poll)));

      StatusTable.SummaryInfo s1 =
	new StatusTable.SummaryInfo("Caller", STRINGTYPE,
				    poll.getCallerID());
      s1.setHeaderFootnote("Actually, the identity of the first poll packet we saw." +
			   "  This is not necessarily the original poll caller.");
      list.add(s1);
      list.add(new StatusTable.SummaryInfo("Start Time",
					   ColumnDescriptor.TYPE_DATE,
					   new Long(poll.getCreateTime())));
      list.add(new StatusTable.SummaryInfo("Duration",
					   ColumnDescriptor.TYPE_TIME_INTERVAL,
					   new Long(tally.duration)));
      if (entry != null && entry.getPollDeadline() != null) {
	long remain = TimeBase.msUntil(entry.getPollDeadline().getExpirationTime());
	if (remain >= 0) {
	  list.add(new StatusTable.SummaryInfo("Remaining",
					       ColumnDescriptor.TYPE_TIME_INTERVAL,
					       new Long(remain)));
	}
      }
      list.add(new StatusTable.SummaryInfo("Quorum", INTTYPE,
					   new Integer(tally.quorum)));
      list.add(new StatusTable.SummaryInfo("Agree Votes", INTTYPE,
					   new Integer(tally.numAgree)));
      list.add(new StatusTable.SummaryInfo("Disagree Votes", INTTYPE,
					   new Integer(tally.numDisagree)));
      return list;
    }

    private String getPollType(BasePoll poll) {
      if (poll instanceof V1Poll) {
        V1PollTally tally = (V1PollTally)poll.getVoteTally();
        return V1Poll.POLL_NAME[tally.getType()];
      } else if (poll instanceof V3Poller) {
        return "V3 Poll";
      }
      theLog.error("Not a V1Poll or V3Poll.");
      return "Unknown poll type";
    }

    private String getPollSpecString(BasePoll poll) {
      PollSpec spec = poll.getPollSpec();
      String range = spec.getRangeString();
      if (range == null) {
	return spec.getUrl();
      } else {
	return spec.getUrl() + "[" + range + "]";
      }
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

      PeerIdentity pid = vote.getVoterIdentity();
      rowMap.put(COL_IDENTITY, pid.getIdString());
      int reputation = pollManager.getIdentityManager().getReputation(pid);
      rowMap.put(COL_REPUTATION, String.valueOf(reputation));
      rowMap.put(COL_AGREE, String.valueOf(vote.agree));
      rowMap.put(COL_CHALLENGE, vote.getChallengeString());
      rowMap.put(COL_VERIFIER,vote.getVerifierString());
      rowMap.put(COL_HASH,vote.getHashString());

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

  static class ManagerStatusAuRef
    extends PollerStatus implements ObjectReferenceAccessor {

    public ManagerStatusAuRef(PollManager pollManager) {
      super(pollManager);
    }

    int howManyPollsRunning(ArchivalUnit au) {
      String auid = au.getAuId();
      int cnt = 0;
      for (Iterator iter = pollManager.getV1Polls().iterator(); iter.hasNext(); ) {
        PollManager.PollManagerEntry entry =
	  (PollManager.PollManagerEntry)iter.next();
	PollSpec spec = entry.getPollSpec();
	if (auid.equals(spec.getAuId())) {
	  cnt++;
	}
      }
      return cnt;
    }

    public StatusTable.Reference getReference(String tableName, Object obj) {
      ArchivalUnit au = (ArchivalUnit)obj;
      String auid = au.getAuId();
      String keys =
	PropUtil.propsToCanonicalEncodedString(PropUtil.fromArgs("AU", auid));
      return new StatusTable.Reference(new Integer(howManyPollsRunning(au)),
				       tableName, keys);
    }
  }

  /** Object whose toString() returns "<i>number</i> polls", but which
   * sorts numerically  */
  static class PollsRef implements Comparable {
    private int num;
    private String label;
    PollsRef(int n) {
      num = n;
      label = n + " polls";
    }
    public String toString() {
      return label;
    }
    public int getNum() {
      return num;
    }
    public int compareTo(Object o) {
      return num - ((PollsRef)o).getNum();
    }
  }
}
