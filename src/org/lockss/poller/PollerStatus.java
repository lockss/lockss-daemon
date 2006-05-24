/*
* $Id: PollerStatus.java,v 1.23 2006-05-24 23:04:50 tlipkis Exp $
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

import java.util.*;
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
  private static Logger theLog=Logger.getLogger("PollerStatus");

  PollerStatus(PollManager pollManager) {
    this.pollManager = pollManager;
  }

  static class ManagerStatus
    extends PollerStatus implements StatusAccessor {
    static final String TABLE_NAME = MANAGER_STATUS_TABLE_NAME;
    private static String POLLMANAGER_TABLE_TITLE = "Polls";

    static final int STRINGTYPE = ColumnDescriptor.TYPE_STRING;
    static final int DATETYPE = ColumnDescriptor.TYPE_DATE;

    private static final List sortRules =
      ListUtil.list(
		    new StatusTable.SortRule("AuName", CatalogueOrderComparator.SINGLETON),
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
      rowMap.put("AuName", spec.getCachedUrlSet().getArchivalUnit().getName());
      //"URL"
      rowMap.put("URL", spec.getUrl());
      //"Range"
      rowMap.put("Range", spec.getRangeString());
      //"PollType"
      rowMap.put("PollType", getTypeCharString(entry.getType()));
      //"Status"
      rowMap.put("Status", entry.getStatusString());
      //"Deadline"
      if (entry.getPollDeadline() != null) {
        rowMap.put("Deadline", entry.getPollDeadline());
      }
      //"PollID"
      rowMap.put("PollID", PollStatus.makePollRef(entry.getShortKey(),
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
	else if (key.equals("URL")) {
	  if (!spec.getUrl().equals(val)) {
	    return false;
	  }
	}
	else if (key.equals("PollType")) {
	  if (!entry.getTypeString().equals(val)) {
	    return false;
	  }
	}
	else if (key.equals("Status")) {
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
	  combinedProps.setProperty("Status", type);
	  list.add(makeRef((cnt + " " + type), combinedProps));
	  if (iter.hasNext()) {
	    list.add(", ");
	  }
	}
      }
      Properties combinedProps = PropUtil.copy(props);
      combinedProps.remove("Status");
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
      String type = props.getProperty("PollType");
      if (type != null) {
	prefix.add(type);
      }
      String status = props.getProperty("Status");
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
      String url = props.getProperty("URL");
      if (url != null) {
	suffix.add("on " + url);
      }
      StringBuffer sb = new StringBuffer();
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
      return makeRef(value, "URL", key);
    }

    public StatusTable.Reference makePollTypeRef(Object value, String key) {
      return makeRef(value, "PollType", key);
    }

    public StatusTable.Reference makeStatusRef(Object value, String key) {
      return makeRef(value, "Status", key);
    }
  }

  static class PollCounts {
    private Map statusCnts = new HashMap();
    private Map auCnts = new HashMap();

    void incrStatusCnt(String status) {
      MutableInteger n = (MutableInteger)statusCnts.get(status);
      if (n == null) {
	n = new MutableInteger();
	statusCnts.put(status, n);
      }
      n.add(1);
    }

    void incrAuIdCnt(String auid) {
      MutableInteger n = (MutableInteger)auCnts.get(auid);
      if (n == null) {
	n = new MutableInteger();
	auCnts.put(auid, n);
      }
      n.add(1);
    }

    int getStatusCnt(String status) {
      MutableInteger n = (MutableInteger)statusCnts.get(status);
	return n == null ? 0 : n.intValue();
    }

    int getAuCnt(String auid) {
      MutableInteger n = (MutableInteger)auCnts.get(auid);
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

    static final int INTTYPE = ColumnDescriptor.TYPE_INT;
    static final int STRINGTYPE = ColumnDescriptor.TYPE_STRING;

    private static final List columnDescriptors =
        ListUtil.list(new ColumnDescriptor("Identity", "Identity", STRINGTYPE),
        new ColumnDescriptor("Reputation", "Reputation", INTTYPE),
        new ColumnDescriptor("Agree", "Agree", STRINGTYPE),
        new ColumnDescriptor("Challenge", "Challenge", STRINGTYPE),
        new ColumnDescriptor("Verifier", "Verifier", STRINGTYPE),
        new ColumnDescriptor("Hash", "Hash", STRINGTYPE)
        );

    private static final List sortRules =
        ListUtil.list(new StatusTable.SortRule("Identity", true));

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
	pollManager.getCurrentOrRecentPollEntry(poll.getKey());

      list.add(new StatusTable.SummaryInfo("Volume" , STRINGTYPE,
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
      s1.setFootnote("Actually, the identity of the first poll packet we saw." +
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
      rowMap.put("Identity", pid.getIdString());
      int reputation = pollManager.getIdentityManager().getReputation(pid);
      rowMap.put("Reputation", String.valueOf(reputation));
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

    public StatusTable.Reference getReference(Object obj, String tableName) {
      ArchivalUnit au = (ArchivalUnit)obj;
      String auid = au.getAuId();
      String keys =
	PropUtil.propsToCanonicalEncodedString(PropUtil.fromArgs("AU", auid));
      return new StatusTable.Reference(new PollsRef(howManyPollsRunning(au)),
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
