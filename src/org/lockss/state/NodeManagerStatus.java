/*
 * $Id: NodeManagerStatus.java,v 1.4 2003-04-17 02:16:58 troberts Exp $
 */

/*
 Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.state;

import java.util.*;
import org.lockss.daemon.status.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.daemon.RangeCachedUrlSetSpec;

/**
 * Collect and report the status of the NodeManager
 * @author Claire Griffin
 * @version 1.0
 */
public class NodeManagerStatus {
  public static final String SERVICE_STATUS_TABLE_NAME =
      "NodeManagerServiceTable";
  public static final String MANAGER_STATUS_TABLE_NAME = "NodeManagerTable";
  public static final String POLLHISTORY_STATUS_TABLE_NAME = "PollHistoryTable";

  private static NodeManagerServiceImpl managerService;

  NodeManagerStatus(NodeManagerServiceImpl impl) {
    managerService = impl;
  }

  private static NodeManagerImpl getNodeManager(String key) throws
      StatusService.
      NoSuchTableException {

    Iterator entries = managerService.getEntries();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry) entries.next();
      NodeManager manager = (NodeManager) entry.getValue();
      if (manager.getAuState().au.getAUId().equals(key)) {
        return (NodeManagerImpl) manager;
      }
    }
    throw new StatusService.NoSuchTableException("No NodeManager for ID " +
                                                 key);
  }

  static class ServiceStatus
      implements StatusAccessor {
    static final String TABLE_TITLE = "NodeManager Service Table";

    private static final List columnDescriptors = ListUtil.list(
         new ColumnDescriptor("AuID", "AU ID", ColumnDescriptor.TYPE_STRING),
         new ColumnDescriptor("CrawlTime", "Last Crawl Time",
                              ColumnDescriptor.TYPE_DATE),
         new ColumnDescriptor("TopLevelPoll", "Last Top Level Poll",
                              ColumnDescriptor.TYPE_DATE),
         new ColumnDescriptor("TreeWalk", "Last Tree Walk",
                              ColumnDescriptor.TYPE_DATE)
         );

    private static final List sortRules = 
      ListUtil.list(new StatusTable.SortRule("AuID", true));

    public void populateTable(StatusTable table) throws StatusService.
        NoSuchTableException {
      String key = table.getKey();
      table.setTitle(TABLE_TITLE);
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(key));
    }

    public boolean requiresKey() {
      return false;
    }

    private boolean matchKey(NodeManager manager, String key) {
      return true;
    }

    private List getRows(String key) {
      List rowL = new ArrayList();
      Iterator entries = managerService.getEntries();
      while (entries.hasNext()) {
        Map.Entry entry = (Map.Entry) entries.next();
        NodeManager manager = (NodeManager) entry.getValue();
        if (key == null || matchKey(manager, key)) {
          if(manager.getAuState() != null) {
            rowL.add(makeRow(manager));
          }
        }
      }
      return rowL;
    }

    private Map makeRow(NodeManager manager) {
      HashMap rowMap = new HashMap();
      AuState state = manager.getAuState();
      ArchivalUnit au = state.getArchivalUnit();

      //"AuID"
      rowMap.put("AuID", ManagerStatus.makeNodeManagerRef(au.getAUId(),
          au.getAUId()));

      //"Status"
      rowMap.put("CrawlTime", new Long(state.getLastCrawlTime()));
      rowMap.put("TopLevelPoll", new Long(state.getLastTopLevelPollTime()));
      rowMap.put("TreeWalk", new Long(state.getLastTreeWalkTime()));

      return rowMap;
    }
  }

  static class ManagerStatus
      implements StatusAccessor {

    static final String TABLE_TITLE = "NodeManager Status Table";

    private static final List columnDescriptors = ListUtil.list(
        new ColumnDescriptor("URL", "URL", ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor("CrawlTime", "Last Crawl",
                             ColumnDescriptor.TYPE_DATE),
        new ColumnDescriptor("CrawlType", "Crawl Type",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor("CrawlStatus", "Crawl Status",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor("NumPolls", "Polls Run",
                             ColumnDescriptor.TYPE_INT),
        new ColumnDescriptor("ActivePolls", "Active Polls",
                             ColumnDescriptor.TYPE_INT),
        new ColumnDescriptor("PollTime", "Last Poll",
                             ColumnDescriptor.TYPE_DATE),
        new ColumnDescriptor("PollType", "Poll Type",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor("PollRange", "Poll Range",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor("PollStatus", "Poll Status",
                             ColumnDescriptor.TYPE_STRING)
        );

    private static final List sortRules = ListUtil.list
        (new StatusTable.SortRule("PollTime", false),
         new StatusTable.SortRule("URL", true)
         );

    public void populateTable(StatusTable table) throws StatusService.
        NoSuchTableException {
      String key = table.getKey();
      NodeManagerImpl nodeManager = getNodeManager(key);

      table.setTitle(getTitle(key));
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(nodeManager));
    }

    public boolean requiresKey() {
      return true;
    }

    // utility methods for making a Reference

    public static StatusTable.Reference makeNodeManagerRef(Object value,
        String key) {
      return new StatusTable.Reference(value, MANAGER_STATUS_TABLE_NAME, key);
    }

    private List getRows(NodeManagerImpl nodeManager) {
      Iterator entries = nodeManager.getCacheEntries();
      String auId = nodeManager.getAuState().getArchivalUnit().getAUId();
      ArrayList entriesL = new ArrayList();
      while (entries.hasNext()) {
        NodeState state = (NodeState) entries.next();
        entriesL.add(makeRow(auId, state));
      }
      return entriesL;
    }

    private String getTitle(String key) {
      return "NodeManager Table for Archival Unit " + key;
    }

    private void filterActiveNodes(List entriesList, NodeState state) {
      int status = state.getCrawlState().getStatus();
      if ( (status != CrawlState.FINISHED) &&
          (status != CrawlState.NODE_DELETED)) {
        entriesList.add(state);
      }
    }

    private Map makeRow(String auId, NodeState state) {
      HashMap rowMap = new HashMap();
      String url = state.getCachedUrlSet().getUrl();
      // URL
      rowMap.put("URL", url);

      CrawlState crawl_state = state.getCrawlState();
      // CrawlTime
      rowMap.put("CrawlTime", new Long(crawl_state.getStartTime()));
      // CrawlType
      rowMap.put("CrawlType", crawl_state.getTypeString());
      // CrawlStatus
      rowMap.put("CrawlStatus", crawl_state.getStatusString());

      // ActivePolls with a reference to a active poll history table
      StatusTable.Reference ref = PollHistoryStatus.makeNodeRef(
          new Integer(getActivePolls(state).size()),
          auId,
          url,
          PollHistoryStatus.ACTIVE_POLLS_FILTER);
      rowMap.put("ActivePolls", ref);

      // NumPolls with a reference to a all poll history table
      ref = PollHistoryStatus.makeNodeRef(
          new Integer(getPollHistories(state).size()),
          auId,
          url,
          PollHistoryStatus.ALL_POLLS_FILTER);
      rowMap.put("NumPolls", ref);

      // our most recent poll data
      PollHistory poll_history = state.getLastPollHistory();
      if (poll_history != null) {
        // PollTime
        rowMap.put("PollTime",
                   new Long(poll_history.getStartTime()));
        // PollType
        rowMap.put("PollType", poll_history.getTypeString());
        // PollRange
        rowMap.put("PollRange", poll_history.getRangeString());
        // PollStatus
        rowMap.put("PollStatus", poll_history.getStatusString());
      }
      else {
        rowMap.put("PollTime", new Long(0));
      }

      return rowMap;
    }

    private ArrayList getActivePolls(NodeState state) {
      Iterator polls = state.getActivePolls();
      ArrayList activeL = new ArrayList();
      while (polls.hasNext()) {
        PollState poll_state = (PollState) polls.next();
        activeL.add(poll_state);
      }
      return activeL;
    }

    private ArrayList getPollHistories(NodeState state) {
      Iterator pollHistories = state.getPollHistories();
      ArrayList historiesL = new ArrayList();
      while (pollHistories.hasNext()) {
        PollHistory history = (PollHistory) pollHistories.next();
        historiesL.add(history);
      }
      return historiesL;
    }
  }

  static class PollHistoryStatus
      implements StatusAccessor {
    static final String TABLE_TITLE = "Node Poll History Table";

    public static String ALL_POLLS_FILTER = "ALLPOLLS:";
    public static String ACTIVE_POLLS_FILTER = "ACTIVEPOLLS:";

    private static final List columnDescriptors = ListUtil.list(
        new ColumnDescriptor("StartTime", "Start Time",
                             ColumnDescriptor.TYPE_DATE),
        new ColumnDescriptor("Duration", "Duration",
                             ColumnDescriptor.TYPE_TIME_INTERVAL),
        new ColumnDescriptor("Type", "Type",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor("Range", "Range",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor("Status", "Status",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor("NumAgree", "Agree",
                             ColumnDescriptor.TYPE_INT),
        new ColumnDescriptor("NumDisagree", "Disagree",
                             ColumnDescriptor.TYPE_INT)
        );

    private static final List sortRules = ListUtil.list
        (new StatusTable.SortRule("StartTime", false)
         );

    public void populateTable(StatusTable table) throws StatusService.
        NoSuchTableException {
      String key = table.getKey();
      String filter = getPollFilterFromKey(key);
      NodeManagerImpl nodeManager = getNodeManagerFromKey(key, filter);
      NodeState nodeState = getNodeStateFromKey(nodeManager, key);

      table.setTitle(getTitle(nodeState));
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(nodeState, filter));
    }

    public boolean requiresKey() {
      return true;
    }

    // utility methods for making a Reference

    public static StatusTable.Reference makeNodeRef(Object value,
        String auId, String url, String filter) {
      StringBuffer key_buf = new StringBuffer(ACTIVE_POLLS_FILTER);
      key_buf.append(auId);
      key_buf.append("&");
      key_buf.append(url);
      return new StatusTable.Reference(value,
                                       POLLHISTORY_STATUS_TABLE_NAME,
                                       key_buf.toString());
    }

    // support for completing the table
    private List getRows(NodeState state, String filter) {
      Iterator histories;
      ArrayList entriesL = new ArrayList();

      if (filter.equals(ACTIVE_POLLS_FILTER)) {
        histories = state.getActivePolls();
      }
      else {
        histories = state.getPollHistories();
        while (histories.hasNext()) {
          PollState history = (PollState) histories.next();
          entriesL.add(makeRow(history));
        }
      }
      return entriesL;
    }

    private String getTitle(NodeState state) {
      return "Poll History at Node " + state.getCachedUrlSet().getUrl();
    }

    private Map makeRow(PollState history) {
      HashMap rowMap = new HashMap();
      // PollTime
      rowMap.put("StartTime", new Long(history.getStartTime()));
      long duration = 0;
      if(history instanceof PollHistory) {
        // Duration
        duration = ((PollHistory)history).getDuration();
      }
      rowMap.put("Duration", new Long(duration));
     // PollType
      rowMap.put("Type", history.getTypeString());
      // PollRange
      rowMap.put("Range", history.getRangeString());
      // PollStatus
      rowMap.put("Status", history.getStatusString());

      int agree = 0;
      int disagree = 0;

      if(history instanceof PollHistory) {
        Iterator votes = ((PollHistory)history).getVotes();
        while (votes.hasNext()) {
          Vote vote = (Vote) votes.next();
          if (vote.isAgreeVote()) {
            agree++;
          }
          else {
            disagree++;
          }
        }
     }
     // YesVotes
     rowMap.put("NumAgree", new Integer(agree));
     // NoVotes
     rowMap.put("NumDisagree", new Integer(disagree));

      return rowMap;
    }

    // key support

    private String getPollFilterFromKey(String key) throws
        StatusService.NoSuchTableException {
      if (key.startsWith(ALL_POLLS_FILTER)) {
        return ALL_POLLS_FILTER;
      }
      else if (key.startsWith(ACTIVE_POLLS_FILTER)) {
        return ACTIVE_POLLS_FILTER;
      }
      else {
        throw new StatusService.NoSuchTableException("Unknown filter for key: " + key);
      }
    }

    private NodeManagerImpl getNodeManagerFromKey(String key, String filter) throws
        StatusService.NoSuchTableException {
      int pos = filter.length();

      String au_id = key.substring(pos, key.lastIndexOf("&"));

      return getNodeManager(au_id);
    }

    private NodeState getNodeStateFromKey(NodeManager nodeManager, String key) throws
        StatusService.NoSuchTableException {
      int pos = key.lastIndexOf("&");
      String url = key.substring(pos + 1);
      ArchivalUnit au = nodeManager.getAuState().getArchivalUnit();
      CachedUrlSet cus = au.makeCachedUrlSet(new RangeCachedUrlSetSpec(url));
      NodeState state = nodeManager.getNodeState(cus);
      if (state == null) {
        throw new StatusService.NoSuchTableException("No Node State for "
            + key);
      }
      return state;
    }
  }
}
