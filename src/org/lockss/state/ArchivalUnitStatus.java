/*
 * $Id: ArchivalUnitStatus.java,v 1.24 2004-10-12 23:44:46 smorabito Exp $
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

package org.lockss.state;

import java.util.*;
import java.net.MalformedURLException;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;

/**
 * Collect and report the status of the ArchivalUnits
 */
public class ArchivalUnitStatus
  extends BaseLockssDaemonManager implements ConfigurableManager {

  /**
   * The maximum number of nodes to display in a single page of the ui.
   */
  public static final String PARAM_MAX_NODES_TO_DISPLAY =
      Configuration.PREFIX + "state.max.nodes.to.display";
  static final int DEFAULT_MAX_NODES_TO_DISPLAY = 100;

  public static final String SERVICE_STATUS_TABLE_NAME =
      "ArchivalUnitStatusTable";
  public static final String AU_STATUS_TABLE_NAME = "ArchivalUnitTable";
  public static final String PEERS_TABLE_NAME = "PeerAgreement";

  static final OrderedObject DASH = new OrderedObject("-", new Long(-1));

  private static Logger logger = Logger.getLogger("AuStatus");
  private static int nodesToDisplay;

  public void startService() {
    super.startService();

    StatusService statusServ = theDaemon.getStatusService();
    statusServ.registerStatusAccessor(SERVICE_STATUS_TABLE_NAME,
                                      new AuSummary(theDaemon));
    statusServ.registerStatusAccessor(AU_STATUS_TABLE_NAME,
                                      new AuStatus(theDaemon));
    statusServ.registerStatusAccessor(PEERS_TABLE_NAME,
                                      new PeersAgreement(theDaemon));
    logger.debug2("Status accessors registered.");
  }

  public void stopService() {
    // unregister our status accessors
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.unregisterStatusAccessor(SERVICE_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(AU_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(PEERS_TABLE_NAME);
    logger.debug2("Status accessors unregistered.");
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    nodesToDisplay = config.getInt(PARAM_MAX_NODES_TO_DISPLAY,
                                   DEFAULT_MAX_NODES_TO_DISPLAY);
  }

  static class AuSummary implements StatusAccessor {
    static final String TABLE_TITLE = "Archival Units";

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("AuName", "Volume", ColumnDescriptor.TYPE_STRING),
//       new ColumnDescriptor("AuNodeCount", "Nodes", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("AuSize", "Size", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("Peers", "Peers", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("AuPolls", "Polls",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Damaged", "Status",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("AuLastPoll", "Last Poll",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("AuLastCrawl", "Last Crawl",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("AuLastTreeWalk", "Last TreeWalk",
                           ColumnDescriptor.TYPE_DATE)
      );

    private static final List sortRules =
      ListUtil.list(new
		    StatusTable.SortRule("AuName",
					 CatalogueOrderComparator.SINGLETON));

    private LockssDaemon theDaemon;

    AuSummary(LockssDaemon theDaemon) {
      this.theDaemon = theDaemon;
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(table));
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows(StatusTable table) {
      PluginManager pluginMgr = theDaemon.getPluginManager();

      boolean includeInternalAus =
	table.getOptions().get(StatusTable.OPTION_INCLUDE_INTERNAL_AUS);
      List rowL = new ArrayList();
      for (Iterator iter = pluginMgr.getAllAus().iterator();
	   iter.hasNext(); ) {
        ArchivalUnit au = (ArchivalUnit)iter.next();
	if (!includeInternalAus && pluginMgr.isInternalAu(au)) {
	  continue;
	}
	try {
	  NodeManager nodeMan = theDaemon.getNodeManager(au);
	  LockssRepository repo = theDaemon.getLockssRepository(au);
	  CachedUrlSet auCus = au.getAuCachedUrlSet();
	  NodeState topNodeState = nodeMan.getNodeState(auCus);
	  RepositoryNode repoNode = null;
	  try {
	    repoNode = repo.getNode(au.getAuCachedUrlSet().getUrl());
	  } catch (MalformedURLException ignore) { }
	  rowL.add(makeRow(au, nodeMan.getAuState(), topNodeState, repoNode));
	} catch (Exception e) {
	  logger.warning("Unexpected expection building row", e);
	}
      }
      return rowL;
    }

    private Map makeRow(ArchivalUnit au, AuState auState,
			NodeState topNodeState,
                        RepositoryNode repoNode) {
      HashMap rowMap = new HashMap();
      //"AuID"
      rowMap.put("AuName", AuStatus.makeAuRef(au.getName(), au.getAuId()));
//       rowMap.put("AuNodeCount", new Integer(-1));
      rowMap.put("AuSize", new Long(repoNode.getTreeContentSize(null)));
      rowMap.put("AuLastCrawl", new Long(auState.getLastCrawlTime()));
      rowMap.put("Peers", PeersAgreement.makeAuRef("peers", au.getAuId()));
      rowMap.put("AuPolls",
		 theDaemon.getStatusService().
		 getReference(PollerStatus.MANAGER_STATUS_TABLE_NAME,
			      au));
      rowMap.put("AuLastPoll", new Long(auState.getLastTopLevelPollTime()));
      rowMap.put("AuLastTreeWalk", new Long(auState.getLastTreeWalkTime()));
      rowMap.put("Damaged", (topNodeState.hasDamage()
			     ? DAMAGE_STATE_DAMAGED : DAMAGE_STATE_OK));
      return rowMap;
    }
  }

  static final StatusTable.DisplayedValue DAMAGE_STATE_OK =
    new StatusTable.DisplayedValue("Ok");
  static final StatusTable.DisplayedValue DAMAGE_STATE_DAMAGED =
    new StatusTable.DisplayedValue("Repairing");

//   static {
//     DAMAGE_STATE_OK.setColor("green");
//     DAMAGE_STATE_DAMAGED.setColor("yellow");
//   }

  abstract static class PerAuTable implements StatusAccessor {
    static final String KEY_SUFFIX = "&&&";

    protected LockssDaemon theDaemon;

    PerAuTable(LockssDaemon theDaemon) {
      this.theDaemon = theDaemon;
    }

    public boolean requiresKey() {
      return true;
    }

    public String getDisplayName() {
      throw new UnsupportedOperationException("Au table has no generic title");
    }

    public void populateTable(StatusTable table)
	throws StatusService.NoSuchTableException {
      String key = table.getKey();
      int startRow = 0;
      int index = key.lastIndexOf(KEY_SUFFIX);
      if (index >= 0) {
        try {
          String rowStr = key.substring(index + KEY_SUFFIX.length());
          startRow = Integer.parseInt(rowStr);
        } catch (NumberFormatException ignore) { }
        key = key.substring(0, index);
      }
      try {
	ArchivalUnit au = theDaemon.getPluginManager().getAuFromId(key);
	if (au == null) {
	  throw new StatusService.NoSuchTableException("Unknown auid: " + key);
	}
	populateTable(table, au, startRow);
      } catch (Exception e) {
	logger.warning("Error building table", e);
	throw new StatusService.
	  NoSuchTableException("Error building table for auid: " + key);
      }
    }

    protected abstract void populateTable(StatusTable table, ArchivalUnit au,
					  int startRow)
        throws StatusService.NoSuchTableException;
  }

  static class AuStatus extends PerAuTable {

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("NodeName", "Node Url",
                           ColumnDescriptor.TYPE_STRING),
//       new ColumnDescriptor("NodeHasContent", "Content",
//                            ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("NodeVersion", "Version",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("NodeContentSize", "Size",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("NodeTreeSize", "Tree Size",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("NodeChildCount", "Children",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("NodeStatus", "Status",
                           ColumnDescriptor.TYPE_STRING)
      );

    private static final List sortRules =
      ListUtil.list(new StatusTable.SortRule("sort", true));

    AuStatus(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected void populateTable(StatusTable table, ArchivalUnit au,
				 int startRow)
        throws StatusService.NoSuchTableException {
      LockssRepository repo = theDaemon.getLockssRepository(au);
      NodeManager nodeMan = theDaemon.getNodeManager(au);

      table.setTitle(getTitle(au.getName()));
      CachedUrlSet auCus = au.getAuCachedUrlSet();
      NodeState topNode = nodeMan.getNodeState(auCus);
      RepositoryNode repoNode = null;
      try {
        repoNode = repo.getNode(auCus.getUrl());
      } catch (MalformedURLException ignore) { }
      table.setSummaryInfo(getSummaryInfo(au, nodeMan.getAuState(), topNode,
                                          repoNode));
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(columnDescriptors);
	table.setDefaultSortRules(sortRules);
	table.setRows(getRows(au, repo, nodeMan, startRow));
      }
    }

    private List getRows(ArchivalUnit au, LockssRepository repo,
                         NodeManager nodeMan, int startRow) {
      List rowL = new ArrayList();
      Iterator cusIter = au.getAuCachedUrlSet().contentHashIterator();
     int endRow1 = startRow + nodesToDisplay; // end row + 1

      if (startRow > 0) {
        // add 'previous'
        int start = startRow - nodesToDisplay;
        if (start < 0) {
          start = 0;
        }
        rowL.add(makeOtherRowsLink(false, start, au.getAuId()));
      }

      for (int curRow = 0; (curRow < endRow1) && cusIter.hasNext(); curRow++) {
        CachedUrlSetNode cusn = (CachedUrlSetNode)cusIter.next();
        if (curRow < startRow) {
          continue;
        }
        CachedUrlSet cus;
        if (cusn.getType() == cusn.TYPE_CACHED_URL_SET) {
          cus = (CachedUrlSet)cusn;
        } else {
          CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(cusn.getUrl());
          cus = au.makeCachedUrlSet(spec);
        }
        try {
	  Map row = makeRow(repo.getNode(cus.getUrl()),
			    nodeMan.getNodeState(cus));
	  row.put("sort", new Integer(curRow));
          rowL.add(row);
        } catch (MalformedURLException ignore) { }
      }

      if (cusIter.hasNext()) {
        // add 'next'
        rowL.add(makeOtherRowsLink(true, endRow1, au.getAuId()));
      }
      return rowL;
    }

    private Map makeRow(RepositoryNode node, NodeState state) {
      HashMap rowMap = new HashMap();
      rowMap.put("NodeName", node.getNodeUrl());

      String status = null;
      if (node.isDeleted()) {
        status = "Deleted";
      } else if (node.isContentInactive()) {
        status = "Inactive";
      } else if (state.hasDamage()) {
        status = "Damaged";
      } else {
//         status = "Active";
      }
      if (status != null) {
	rowMap.put("NodeStatus", status);
      }
      boolean content = node.hasContent();
      Object versionObj = DASH;
      Object sizeObj = DASH;
      if (content) {
        versionObj = new OrderedObject(new Long(node.getCurrentVersion()));
        sizeObj = new OrderedObject(new Long(node.getContentSize()));
      }
      rowMap.put("NodeHasContent", (content ? "yes" : "no"));
      rowMap.put("NodeVersion", versionObj);
      rowMap.put("NodeContentSize", sizeObj);
      if (!node.isLeaf()) {
	rowMap.put("NodeChildCount",
		   new OrderedObject(new Long(node.getChildCount())));
	rowMap.put("NodeTreeSize",
		   new OrderedObject(new Long(node.getTreeContentSize(null))));
      } else {
	rowMap.put("NodeChildCount", DASH);
	rowMap.put("NodeTreeSize", DASH);
      }
      return rowMap;
    }

    private Map makeOtherRowsLink(boolean isNext, int startRow, String auKey) {
      HashMap rowMap = new HashMap();
      String label = (isNext ? "Next" : "Previous") + " (" +
	(startRow + 1) + "-" + (startRow + nodesToDisplay) + ")";
      StatusTable.Reference link =
          new StatusTable.Reference(label, AU_STATUS_TABLE_NAME,
                                    auKey + KEY_SUFFIX + startRow);
      rowMap.put("NodeName", link);
      rowMap.put("sort", new Integer(isNext ? Integer.MAX_VALUE : -1));
      return rowMap;
    }

    private String getTitle(String key) {
      return "Status of AU: " + key;
    }

    private List getSummaryInfo(ArchivalUnit au, AuState state,
                                NodeState topNode, RepositoryNode repoNode) {
      List summaryList =  ListUtil.list(
            new StatusTable.SummaryInfo("Volume" , ColumnDescriptor.TYPE_STRING,
                                        au.getName()),
//             new StatusTable.SummaryInfo("Nodes", ColumnDescriptor.TYPE_INT,
//                                         new Integer(-1)),
            new StatusTable.SummaryInfo("Size", ColumnDescriptor.TYPE_INT,
                                        new Long(repoNode.getTreeContentSize(null))),
            new StatusTable.SummaryInfo("Last Crawl Time",
                                        ColumnDescriptor.TYPE_DATE,
                                        new Long(state.getLastCrawlTime())),
            new StatusTable.SummaryInfo("Last Top-level Poll",
                                        ColumnDescriptor.TYPE_DATE,
                                        new Long(state.getLastTopLevelPollTime())),
            new StatusTable.SummaryInfo("Last Treewalk",
                                        ColumnDescriptor.TYPE_DATE,
                                        new Long(state.getLastTreeWalkTime())),
            new StatusTable.SummaryInfo("Status",
                                        ColumnDescriptor.TYPE_STRING,
                                        (topNode.hasDamage()
					 ? DAMAGE_STATE_DAMAGED
					 : DAMAGE_STATE_OK)),
            new StatusTable.SummaryInfo("Current Activity",
                                        ColumnDescriptor.TYPE_STRING,
                                        "-")
            );
        return summaryList;
    }

    // utility method for making a Reference
    public static StatusTable.Reference makeAuRef(Object value,
                                                  String key) {
      return new StatusTable.Reference(value, AU_STATUS_TABLE_NAME,
                                       key + KEY_SUFFIX + "0");
    }
  }

  static class PeersAgreement extends PerAuTable {
    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("Cache", "Cache",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Last", "Last",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Polls", "Polls",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("Agree", "Agree",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("LastAgree", "Last Agree",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("LastDisagree", "Last Disagree",
                           ColumnDescriptor.TYPE_DATE)
      );

    private static final List sortRules =
      ListUtil.list(new StatusTable.SortRule("Cache", true));

    PeersAgreement(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    public void populateTable(StatusTable table, ArchivalUnit au, int startRow)
        throws StatusService.NoSuchTableException {
      NodeManager nodeMan = theDaemon.getNodeManager(au);
      table.setTitle(getTitle(au));
      int totalPeers = 0;
      int totalAgreement = 0;
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(columnDescriptors);
	table.setDefaultSortRules(sortRules);
	Map statsMap = buildCacheStats(au, nodeMan);
	List rowL = new ArrayList();
	for (Iterator iter = statsMap.keySet().iterator(); iter.hasNext(); ) {
	  PeerIdentity peer = (PeerIdentity)iter.next();
	  CacheStats stats = (CacheStats)statsMap.get(peer);
	  if (! peer.isLocalIdentity()) {
	    totalPeers++;
	    if (stats.mostRecentVote.isAgreeVote()) {
	      totalAgreement++;
	    }
	  }
	  Map row = makeRow(stats);
	  rowL.add(row);
	}
	table.setRows(rowL);
      }
      table.setSummaryInfo(getSummaryInfo(au, totalPeers, totalAgreement));
    }

    public Map buildCacheStats(ArchivalUnit au, NodeManager nodeMan) {
      Map statsMap = new HashMap();
      NodeState node = nodeMan.getNodeState(au.getAuCachedUrlSet());
      for (Iterator history_it = node.getPollHistories();
	   history_it.hasNext(); ) {
	PollHistory history = (PollHistory)history_it.next();
	long histTime = history.getStartTime();
	for (Iterator votes_it = history.getVotes(); votes_it.hasNext(); ) {
	  Vote vote = (Vote)votes_it.next();
	  PeerIdentity peer = vote.getVoterIdentity();
	  CacheStats stats = (CacheStats)statsMap.get(peer);
	  if (stats == null) {
	    stats = new CacheStats(peer);
	    statsMap.put(peer, stats);
	  }
	  if (stats.mostRecentVote == null ||
	      histTime > stats.mostRecentVoteTime) {
	    stats.mostRecentVote = vote;
	    stats.mostRecentVoteTime = histTime;
	  }
	  stats.totalPolls++;
	  if (vote.isAgreeVote()) {
	    stats.agreePolls++;
	    if (stats.lastAgree == null ||
		histTime > stats.lastAgreeTime) {
	      stats.lastAgree = vote;
	      stats.lastAgreeTime = histTime;
	    }
	  } else {
	    if (stats.lastDisagree == null ||
		histTime > stats.lastDisagreeTime) {
	      stats.lastDisagree = vote;
	      stats.lastDisagreeTime = histTime;
	    }
	  }
	}
      }
      return statsMap;
    }

    static class CacheStats {
      PeerIdentity peer;
      Vote mostRecentVote;
      long mostRecentVoteTime = 0;
      int totalPolls = 0;
      int agreePolls = 0;
      Vote lastAgree;
      long lastAgreeTime = 0;
      Vote lastDisagree;
      long lastDisagreeTime = 0;

      CacheStats(PeerIdentity peer) {
	this.peer = peer;
      }
    }

    private Map makeRow(CacheStats stats) {
      HashMap rowMap = new HashMap();

      PeerIdentity peer = stats.peer;
      Object id = peer.getIdString();
      if (peer.isLocalIdentity()) {
	StatusTable.DisplayedValue val =
	  new StatusTable.DisplayedValue(id);
	val.setBold(true);
	id = val;
      }
      rowMap.put("Cache", id);

      rowMap.put("Last",
		 stats.mostRecentVote.isAgreeVote() ? "Agree" : "Disagree");
      rowMap.put("Polls", new Long(stats.totalPolls));
      rowMap.put("Agree", new Long(stats.agreePolls));
      rowMap.put("LastAgree", new Long(stats.lastAgreeTime));
      rowMap.put("LastDisagree", new Long(stats.lastDisagreeTime));
      return rowMap;
    }

    private String getTitle(ArchivalUnit au) {
      return "All caches voting on AU: " + au.getName();
    }

    private List getSummaryInfo(ArchivalUnit au,
				int totalPeers, int totalAgreement) {
      List summaryList =  ListUtil.list(
            new StatusTable.SummaryInfo("Peers holding AU",
					ColumnDescriptor.TYPE_INT,
                                        new Integer(totalPeers)),
            new StatusTable.SummaryInfo("Agreeing peers",
					ColumnDescriptor.TYPE_INT,
                                        new Integer(totalAgreement))
            );
      return summaryList;
    }

    // utility method for making a Reference
    public static StatusTable.Reference makeAuRef(Object value,
                                                  String key) {
      return new StatusTable.Reference(value, PEERS_TABLE_NAME,
                                       key);
    }
  }
}
