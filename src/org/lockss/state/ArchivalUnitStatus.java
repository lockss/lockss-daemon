/*
 * $Id: ArchivalUnitStatus.java,v 1.45 2006-10-07 02:01:27 smorabito Exp $
 */

/*
 Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.poller.v3.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.servlet.LockssServlet;

/**
 * Collect and report the status of the ArchivalUnits
 */
public class ArchivalUnitStatus
  extends BaseLockssDaemonManager implements ConfigurableManager {

  public static final String PREFIX = Configuration.PREFIX + "auStatus.";
  /**
   * The default maximum number of nodes to display in a single page of the ui.
   */
  public static final String PARAM_MAX_NODES_TO_DISPLAY =
    PREFIX + "nodesPerPage";
  static final int DEFAULT_MAX_NODES_TO_DISPLAY = 100;

  /**
   * Node URLs are links to cached content page if true
   */
  public static final String PARAM_CONTENT_IS_LINK =
    PREFIX + "contentUrlIsLink";
  static final boolean DEFAULT_CONTENT_IS_LINK = true;

  public static final String SERVICE_STATUS_TABLE_NAME =
      "ArchivalUnitStatusTable";
  public static final String AUIDS_TABLE_NAME = "AuIds";
  public static final String AU_STATUS_TABLE_NAME = "ArchivalUnitTable";
  public static final String PEERS_VOTE_TABLE_NAME = "PeerVoteSummary";
  public static final String PEERS_REPAIR_TABLE_NAME = "PeerRepair";

  static final OrderedObject DASH = new OrderedObject("-", new Long(-1));

  private static Logger logger = Logger.getLogger("AuStatus");
  private static int defaultNumRows = DEFAULT_MAX_NODES_TO_DISPLAY;
  private static boolean isContentIsLink = DEFAULT_CONTENT_IS_LINK;

  public void startService() {
    super.startService();

    StatusService statusServ = theDaemon.getStatusService();
    statusServ.registerStatusAccessor(SERVICE_STATUS_TABLE_NAME,
                                      new AuSummary(theDaemon));
    statusServ.registerStatusAccessor(AUIDS_TABLE_NAME,
                                      new AuIds(theDaemon));
    statusServ.registerStatusAccessor(AU_STATUS_TABLE_NAME,
                                      new AuStatus(theDaemon));
    statusServ.registerStatusAccessor(PEERS_VOTE_TABLE_NAME,
                                      new PeerVoteSummary(theDaemon));
    statusServ.registerStatusAccessor(PEERS_REPAIR_TABLE_NAME,
                                      new PeerRepair(theDaemon));
    logger.debug2("Status accessors registered.");
  }

  public void stopService() {
    // unregister our status accessors
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.unregisterStatusAccessor(SERVICE_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(AU_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(PEERS_VOTE_TABLE_NAME);
    statusServ.unregisterStatusAccessor(PEERS_REPAIR_TABLE_NAME);
    logger.debug2("Status accessors unregistered.");
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    defaultNumRows = config.getInt(PARAM_MAX_NODES_TO_DISPLAY,
                                   DEFAULT_MAX_NODES_TO_DISPLAY);
    isContentIsLink = config.getBoolean(PARAM_CONTENT_IS_LINK,
					DEFAULT_CONTENT_IS_LINK);
  }

  static class AuSummary implements StatusAccessor {
    static final String TABLE_TITLE = "Archival Units";

    static final String FOOT_STATUS = "Flags may follow status: C means the AU is complete, D means that the AU is no longer available from the publisher";

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("AuName", "Volume", ColumnDescriptor.TYPE_STRING),
//       new ColumnDescriptor("AuNodeCount", "Nodes", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("AuSize", "Content Size",
			   ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("DiskUsage", "Disk Usage (MB)",
			   ColumnDescriptor.TYPE_FLOAT),
      new ColumnDescriptor("Peers", "Peers", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("AuPolls", "Polls",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("Damaged", "Status",
                           ColumnDescriptor.TYPE_STRING,
			   FOOT_STATUS),
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
      List cols = columnDescriptors;
      if (theDaemon.isClockss()) {
	cols = new ArrayList(cols);
	cols.remove(cols.size() - 1);
	cols.add(new ColumnDescriptor("Subscribed", "Subscribed",
				      ColumnDescriptor.TYPE_STRING));
      }
      table.setColumnDescriptors(cols);
      table.setDefaultSortRules(sortRules);
      Stats stats = new Stats();
      table.setRows(getRows(table, stats));
      table.setSummaryInfo(getSummaryInfo(stats));
    }

    public boolean requiresKey() {
      return false;
    }

    class Stats {
      int aus = 0;
    }

    private List getRows(StatusTable table, Stats stats) {
      PluginManager pluginMgr = theDaemon.getPluginManager();

      boolean includeInternalAus =
	table.getOptions().get(StatusTable.OPTION_DEBUG_USER);
      List rowL = new ArrayList();
      for (Iterator iter = pluginMgr.getAllAus().iterator();
	   iter.hasNext(); ) {
        ArchivalUnit au = (ArchivalUnit)iter.next();
	if (!includeInternalAus && pluginMgr.isInternalAu(au)) {
	  continue;
	}
	try {
	  NodeManager nodeMan = theDaemon.getNodeManager(au);
	  CachedUrlSet auCus = au.getAuCachedUrlSet();
	  NodeState topNodeState = nodeMan.getNodeState(auCus);
	  rowL.add(makeRow(au, nodeMan.getAuState(), topNodeState));
	  stats.aus++;
	} catch (Exception e) {
	  logger.warning("Unexpected expection building row", e);
	}
      }
      return rowL;
    }

    private Map makeRow(ArchivalUnit au, AuState auState,
			NodeState topNodeState) {
      HashMap rowMap = new HashMap();
      PollManager.V3PollStatusAccessor v3status =
        theDaemon.getPollManager().getV3Status();
      // If this is a v3 AU, we cannot access some of the poll 
      // status through the nodestate.  Eventually, this will be totally
      // refactored.
      boolean isV3 = AuUtil.getProtocolVersion(au) == Poll.V3_PROTOCOL;
      //"AuID"
      rowMap.put("AuName", AuStatus.makeAuRef(au.getName(), au.getAuId()));
//       rowMap.put("AuNodeCount", new Integer(-1));
      rowMap.put("AuSize", new Long(AuUtil.getAuContentSize(au)));
      rowMap.put("DiskUsage", new Double(((double)AuUtil.getAuDiskUsage(au)) / (1024*1024)));
      rowMap.put("AuLastCrawl", new Long(auState.getLastCrawlTime()));
      rowMap.put("Peers", PeerRepair.makeAuRef("peers", au.getAuId()));
      rowMap.put("AuLastTreeWalk", new Long(auState.getLastTreeWalkTime()));
      rowMap.put("AuLastPoll", new Long(auState.getLastTopLevelPollTime()));
      
      Object stat;
      if (isV3) {
        rowMap.put("AuPolls",
                   new StatusTable.Reference(new Integer(v3status.getNumPolls(au.getAuId())),
                                             V3PollStatus.POLLER_STATUS_TABLE_NAME,
                                             au.getAuId()));
        // Percent damaged.  It's scary to see '0% Agreement' if there's no
        // history, so we just show a friendlier message.
        if (auState.getLastTopLevelPollTime() == -1) {
          stat = "Waiting for Poll";
        } else {
          stat = Integer.toString((int)Math.round(auState.getV3Agreement() * 100)) +
                 "% Agreement";
        }
      } else {
        rowMap.put("AuPolls",
                   theDaemon.getStatusService().
                   getReference(PollerStatus.MANAGER_STATUS_TABLE_NAME,
                                au));
	stat = topNodeState.hasDamage()
	  ? DAMAGE_STATE_DAMAGED : DAMAGE_STATE_OK;
      }

      boolean isPubDown = AuUtil.isPubDown(au);
      boolean isClosed = AuUtil.isClosed(au);
        
      if (isPubDown || isClosed) {
	List val = ListUtil.list(stat, " (");
	if (isClosed) {
	  val.add("C");
	}
	if (isPubDown) {
	  val.add("D");
	}
	val.add(")");
	stat = val;
      }

      rowMap.put("Damaged", stat);

      if (theDaemon.isClockss()) {
	rowMap.put("Subscribed",
		   AuUtil.getAuState(au).getClockssSubscriptionStatusString());
      }

      return rowMap;
    }

    private List getSummaryInfo(Stats stats) {
      String numaus = StringUtil.numberOfUnits(stats.aus, "Archival Unit",
					       "Archival Units");
      return
	ListUtil.list(new StatusTable.SummaryInfo(null,
						  ColumnDescriptor.TYPE_STRING,
						  numaus));
    }
  }

  static class AuIds implements StatusAccessor {
    static final String TABLE_TITLE = "AU Ids";

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("AuName", "Volume", ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("AuId", "AU Id", ColumnDescriptor.TYPE_STRING)
      );

    private static final List sortRules =
      ListUtil.list(new
		    StatusTable.SortRule("AuName",
					 CatalogueOrderComparator.SINGLETON));

    private LockssDaemon theDaemon;

    AuIds(LockssDaemon theDaemon) {
      this.theDaemon = theDaemon;
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      Stats stats = new Stats();
      table.setRows(getRows(table, stats));
      table.setSummaryInfo(getSummaryInfo(stats));
    }

    public boolean requiresKey() {
      return false;
    }

    class Stats {
      int aus = 0;
    }

    private List getRows(StatusTable table, Stats stats) {
      PluginManager pluginMgr = theDaemon.getPluginManager();

      boolean includeInternalAus =
	table.getOptions().get(StatusTable.OPTION_DEBUG_USER);
      List rowL = new ArrayList();
      for (Iterator iter = pluginMgr.getAllAus().iterator();
	   iter.hasNext(); ) {
        ArchivalUnit au = (ArchivalUnit)iter.next();
	if (!includeInternalAus && pluginMgr.isInternalAu(au)) {
	  continue;
	}
	try {
	  rowL.add(makeRow(au));
	  stats.aus++;
	} catch (Exception e) {
	  logger.warning("Unexpected expection building row", e);
	}
      }
      return rowL;
    }

    private Map makeRow(ArchivalUnit au) {
      HashMap rowMap = new HashMap();
      rowMap.put("AuId", au.getAuId());
      rowMap.put("AuName", au.getName());
      return rowMap;
    }

    private List getSummaryInfo(Stats stats) {
      String numaus = StringUtil.numberOfUnits(stats.aus, "Archival Unit",
					       "Archival Units");
      return
	ListUtil.list(new StatusTable.SummaryInfo(null,
						  ColumnDescriptor.TYPE_STRING,
						  numaus));
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
      try {
	ArchivalUnit au = theDaemon.getPluginManager().getAuFromId(key);
	if (au == null) {
	  throw new StatusService.NoSuchTableException("Unknown auid: " + key);
	}
	populateTable(table, au);
      } catch (StatusService.NoSuchTableException e) {
	throw e;
      } catch (Exception e) {
	logger.warning("Error building table", e);
	throw new StatusService.
	  NoSuchTableException("Error building table for auid: " + key);
      }
    }

    protected abstract void populateTable(StatusTable table, ArchivalUnit au)
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

    protected void populateTable(StatusTable table, ArchivalUnit au)
        throws StatusService.NoSuchTableException {
      LockssRepository repo = theDaemon.getLockssRepository(au);
      NodeManager nodeMan = theDaemon.getNodeManager(au);

      table.setTitle(getTitle(au.getName()));
      CachedUrlSet auCus = au.getAuCachedUrlSet();
      NodeState topNode = nodeMan.getNodeState(auCus);
      table.setSummaryInfo(getSummaryInfo(au, nodeMan.getAuState(), topNode));
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(columnDescriptors);
	table.setDefaultSortRules(sortRules);
	table.setRows(getRows(table, au, repo, nodeMan));
      }
    }


    int getIntProp(StatusTable table, String name) {
      Properties props = table.getProperties();
      if (props == null) return -1;
      String s = props.getProperty(name);
      if (StringUtil.isNullString(s)) return -1;
      try {
	return Integer.parseInt(s);
      } catch (Exception e) {
	return -1;
      }
    }

    private List getRows(StatusTable table, ArchivalUnit au,
			 LockssRepository repo, NodeManager nodeMan) {
      int startRow = Math.max(0, getIntProp(table, "skiprows"));
      int numRows = getIntProp(table, "numrows");
      if (numRows <= 0) {
	numRows = defaultNumRows;
      }

      List rowL = new ArrayList();
      Iterator cusIter = au.getAuCachedUrlSet().contentHashIterator();
      int endRow1 = startRow + numRows; // end row + 1

      if (startRow > 0) {
        // add 'previous'
        int start = startRow - defaultNumRows;
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
        if (cusn.getType() == CachedUrlSetNode.TYPE_CACHED_URL_SET) {
          cus = (CachedUrlSet)cusn;
        } else {
          CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(cusn.getUrl());
          cus = au.makeCachedUrlSet(spec);
        }
        try {
	  Map row = makeRow(au, repo.getNode(cus.getUrl()),
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

    private Map makeRow(ArchivalUnit au, RepositoryNode node,
			NodeState state) {
      String url = node.getNodeUrl();
      boolean hasContent = node.hasContent();
      Object val;
      HashMap rowMap = new HashMap();
      if (hasContent && isContentIsLink) {
	Properties args = new Properties();
	args.setProperty("auid", au.getAuId());
	args.setProperty("url", url);
	val = new StatusTable.SrvLink(url,
				      LockssServlet.SERVLET_DISPLAY_CONTENT,
				      args);
      } else {
	val = url;
      }
      rowMap.put("NodeName", val);

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
      Object versionObj = DASH;
      Object sizeObj = DASH;
      if (hasContent) {
        versionObj = new OrderedObject(new Long(node.getCurrentVersion()));
        sizeObj = new OrderedObject(new Long(node.getContentSize()));
      }
      rowMap.put("NodeHasContent", (hasContent ? "yes" : "no"));
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
	(startRow + 1) + "-" + (startRow + defaultNumRows) + ")";
      StatusTable.Reference link =
          new StatusTable.Reference(label, AU_STATUS_TABLE_NAME, auKey);
      link.setProperty("skiprows", Integer.toString(startRow));
      link.setProperty("numrows", Integer.toString(defaultNumRows));
      rowMap.put("NodeName", link);
      rowMap.put("sort", new Integer(isNext ? Integer.MAX_VALUE : -1));
      return rowMap;
    }

    private String getTitle(String key) {
      return "Status of AU: " + key;
    }

    private List getSummaryInfo(ArchivalUnit au, AuState state,
                                NodeState topNode) {
      int clockssPos = 1;
      List summaryList =  ListUtil.list(
            new StatusTable.SummaryInfo("Volume", ColumnDescriptor.TYPE_STRING,
                                        au.getName()),
//             new StatusTable.SummaryInfo("Nodes", ColumnDescriptor.TYPE_INT,
//                                         new Integer(-1)),
            new StatusTable.SummaryInfo("Content Size",
					ColumnDescriptor.TYPE_INT,
                                        new Long(AuUtil.getAuContentSize(au))),
            new StatusTable.SummaryInfo("Disk Usage (MB)",
					ColumnDescriptor.TYPE_FLOAT,
                                        new Float(AuUtil.getAuContentSize(au) /
						  (float)(1024 * 1024))),
            new StatusTable.SummaryInfo("Status",
                                        ColumnDescriptor.TYPE_STRING,
                                        (topNode.hasDamage()
					 ? DAMAGE_STATE_DAMAGED
					 : DAMAGE_STATE_OK)),
            new StatusTable.SummaryInfo("Available From Publisher",
                                        ColumnDescriptor.TYPE_STRING,
                                        (AuUtil.isPubDown(au) ? "No" : "Yes")),
//             new StatusTable.SummaryInfo("Volume Complete",
//                                         ColumnDescriptor.TYPE_STRING,
//                                         (AuUtil.isClosed(au) ? "Yes" : "No")),
	    new StatusTable.SummaryInfo("Polling Protocol Version",
					ColumnDescriptor.TYPE_INT,
					new Integer(AuUtil.getProtocolVersion(au))),
            new StatusTable.SummaryInfo("Last Crawl Time",
                                        ColumnDescriptor.TYPE_DATE,
                                        new Long(state.getLastCrawlTime())),
            new StatusTable.SummaryInfo("Last Top-level Poll",
                                        ColumnDescriptor.TYPE_DATE,
                                        new Long(state.getLastTopLevelPollTime())),
            new StatusTable.SummaryInfo("Last Treewalk",
                                        ColumnDescriptor.TYPE_DATE,
                                        new Long(state.getLastTreeWalkTime())),
            new StatusTable.SummaryInfo("Current Activity",
                                        ColumnDescriptor.TYPE_STRING,
                                        "-")
            );
      if (theDaemon.isClockss()) {
	String subStatus =
	  AuUtil.getAuState(au).getClockssSubscriptionStatusString();
	summaryList.add(clockssPos,
			new StatusTable.SummaryInfo("Subscribed",
						    ColumnDescriptor.TYPE_STRING,
						    subStatus));
      }
      return summaryList;
    }

    // utility method for making a Reference
    public static StatusTable.Reference makeAuRef(Object value,
                                                  String key) {
      StatusTable.Reference ref =
	new StatusTable.Reference(value, AU_STATUS_TABLE_NAME, key);
//       ref.setProperty("numrows", Integer.toString(defaultNumRows));
      return ref;
    }
  }

  abstract static class PeersAgreement extends PerAuTable {
    protected static final List sortRules =
      ListUtil.list(new StatusTable.SortRule("Cache", true));

    PeersAgreement(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected Map makeRow(CacheStats stats) {
      Map rowMap = new HashMap();

      PeerIdentity peer = stats.peer;
      Object id = peer.getIdString();
      if (peer.isLocalIdentity()) {
	StatusTable.DisplayedValue val =
	  new StatusTable.DisplayedValue(id);
	val.setBold(true);
	id = val;
      }
      rowMap.put("Cache", id);
      return rowMap;
    }


    static class CacheStats {
      PeerIdentity peer;
      int totalPolls = 0;
      int agreePolls = 0;
      Vote lastAgree;
      long lastAgreeTime = 0;
      Vote lastDisagree;
      long lastDisagreeTime = 0;

      CacheStats(PeerIdentity peer) {
	this.peer = peer;
      }
      boolean isLastAgree() {
	return (lastAgreeTime != 0  &&
		(lastDisagreeTime == 0 || lastAgreeTime >= lastDisagreeTime));
      }
    }
  }

  static class PeerVoteSummary extends PeersAgreement {
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

    PeerVoteSummary(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected String getTitle(ArchivalUnit au) {
      return "All caches voting on AU: " + au.getName();
    }

    protected void populateTable(StatusTable table, ArchivalUnit au)
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
	for (Iterator iter = statsMap.entrySet().iterator(); iter.hasNext();) {
	  Map.Entry entry = (Map.Entry)iter.next();
	  PeerIdentity peer = (PeerIdentity)entry.getKey();
	  CacheStats stats = (CacheStats)entry.getValue();
	  if (! peer.isLocalIdentity()) {
	    totalPeers++;
	    if (stats.isLastAgree()) {
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

    protected Map makeRow(CacheStats stats) {
      Map rowMap = super.makeRow(stats);
      rowMap.put("Last",
		 stats.isLastAgree() ? "Agree" : "Disagree");
      rowMap.put("Polls", new Long(stats.totalPolls));
      rowMap.put("Agree", new Long(stats.agreePolls));
      rowMap.put("LastAgree", new Long(stats.lastAgreeTime));
      rowMap.put("LastDisagree", new Long(stats.lastDisagreeTime));
      return rowMap;
    }

    protected List getSummaryInfo(ArchivalUnit au,
				  int totalPeers, int totalAgreement) {
      List summaryList =  ListUtil.list(
            new StatusTable.SummaryInfo("Peers voting on AU",
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
      return new StatusTable.Reference(value, PEERS_VOTE_TABLE_NAME,
                                       key);
    }
  }

  static class PeerRepair extends PeersAgreement {
    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("Cache", "Cache",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Last", "Complete Consensus",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("LastAgree",
			   "Last Complete Consensus",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("LastDisagree",
			   "Last Partial Disagreement",
                           ColumnDescriptor.TYPE_DATE)
      );

    PeerRepair(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected String getTitle(ArchivalUnit au) {
      return "Repair candidates for AU: " + au.getName();
    }
    private static final String FOOT_TITLE =
      "These caches have proven to us that they have (or had) a correct \n" +
      "copy of this AU.  We will fetch repairs from them if necessary, \n" +
      "and they may fetch repairs from us.";

    protected void populateTable(StatusTable table, ArchivalUnit au)
        throws StatusService.NoSuchTableException {
      IdentityManager idMgr = theDaemon.getIdentityManager();
      table.setTitle(getTitle(au));
      table.setTitleFootnote(FOOT_TITLE);
      int totalPeers = 0;
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(columnDescriptors);
	table.setDefaultSortRules(sortRules);
	Map statsMap = buildCacheStats(au, idMgr);
	List rowL = new ArrayList();
	for (Iterator iter = statsMap.entrySet().iterator(); iter.hasNext();) {
	  Map.Entry entry = (Map.Entry)iter.next();
	  PeerIdentity peer = (PeerIdentity)entry.getKey();
	  CacheStats stats = (CacheStats)entry.getValue();
	  if (! peer.isLocalIdentity()) {
	    totalPeers++;
	  }
	  Map row = makeRow(stats);
	  rowL.add(row);
	}
	table.setRows(rowL);
      }
      table.setSummaryInfo(getSummaryInfo(au, totalPeers));
    }

    public Map buildCacheStats(ArchivalUnit au, IdentityManager idMgr) {
      Map statsMap = new HashMap();
      for (Iterator iter = idMgr.getIdentityAgreements(au).iterator();
	   iter.hasNext(); ) {
	IdentityManager.IdentityAgreement ida =
	  (IdentityManager.IdentityAgreement)iter.next();
	try {
	  PeerIdentity pid = idMgr.stringToPeerIdentity(ida.getId());
	  if (ida.getLastAgree() > 0) {	// only add those that have agreed
	    CacheStats stats = new CacheStats(pid);
	    statsMap.put(pid, stats);
	    stats.lastAgreeTime = ida.getLastAgree();
	    stats.lastDisagreeTime = ida.getLastDisagree();
	  }
	} catch (IdentityManager.MalformedIdentityKeyException e) {
	  logger.warning("Malformed id key in IdentityAgreement", e);
	  continue;
	}
      }
      return statsMap;
    }

    protected Map makeRow(CacheStats stats) {
      Map rowMap = super.makeRow(stats);
      rowMap.put("Last", stats.isLastAgree() ? "Yes" : "No");
      rowMap.put("LastAgree", new Long(stats.lastAgreeTime));
      rowMap.put("LastDisagree", new Long(stats.lastDisagreeTime));
      return rowMap;
    }

    protected List getSummaryInfo(ArchivalUnit au,
				  int totalPeers) {
      List summaryList =  ListUtil.list(
            new StatusTable.SummaryInfo("Peers holding AU",
					ColumnDescriptor.TYPE_INT,
                                        new Integer(totalPeers)),
            new StatusTable.SummaryInfo("Peers",
					ColumnDescriptor.TYPE_STRING,
                                        PeerVoteSummary.makeAuRef("Voting on AU", au.getAuId()))
            );
      return summaryList;
    }

    // utility method for making a Reference
    public static StatusTable.Reference makeAuRef(Object value,
                                                  String key) {
      return new StatusTable.Reference(value, PEERS_REPAIR_TABLE_NAME,
                                       key);
    }
  }
}
