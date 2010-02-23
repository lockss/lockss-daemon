/*
 * $Id: ArchivalUnitStatus.java,v 1.87.4.2 2010-02-23 06:19:39 tlipkis Exp $
 */

/*
 Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.MalformedURLException;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.crawler.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.servlet.AdminServletManager;

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

  /**
   * Include number of AUs needing recrawl in overview if true
   */
  public static final String PARAM_INCLUDE_NEEDS_RECRAWL =
    PREFIX + "includeNeedsRecrawl";
  static final boolean DEFAULT_INCLUDE_NEEDS_RECRAWL = true;

  public static final String SERVICE_STATUS_TABLE_NAME =
      "ArchivalUnitStatusTable";
  public static final String AUIDS_TABLE_NAME = "AuIds";
  public static final String AU_STATUS_TABLE_NAME = "ArchivalUnitTable";
  public static final String NO_AU_PEERS_TABLE_NAME = "NoAuPeers";
  public static final String PEERS_VOTE_TABLE_NAME = "PeerVoteSummary";
  public static final String PEERS_REPAIR_TABLE_NAME = "PeerRepair";
  public static final String FILE_VERSIONS_TABLE_NAME = "FileVersions";


  static final OrderedObject DASH = new OrderedObject("-", new Long(-1));

  private static Logger logger = Logger.getLogger("AuStatus");
  private static int defaultNumRows = DEFAULT_MAX_NODES_TO_DISPLAY;
  private static boolean isContentIsLink = DEFAULT_CONTENT_IS_LINK;
  private static boolean includeNeedsRecrawl = DEFAULT_INCLUDE_NEEDS_RECRAWL;
  
  private static final DecimalFormat agreementFormat =
    new DecimalFormat("0.00");

  /* DecimalFormat automatically applies half-even rounding to
   * values being formatted under Java < 1.6.  This is a workaround. */ 
  private static String doubleToPercent(double d) {
    int i = (int)(d * 10000);
    double pc = i / 100.0;
    return agreementFormat.format(pc);
  }

  public void startService() {
    super.startService();

    LockssDaemon theDaemon = getDaemon();
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.registerStatusAccessor(SERVICE_STATUS_TABLE_NAME,
                                      new AuSummary(theDaemon));
    statusServ.registerOverviewAccessor(SERVICE_STATUS_TABLE_NAME,
                                      new Overview(theDaemon));
    statusServ.registerStatusAccessor(AUIDS_TABLE_NAME,
                                      new AuIds(theDaemon));
    statusServ.registerStatusAccessor(AU_STATUS_TABLE_NAME,
                                      new AuStatus(theDaemon));
    statusServ.registerStatusAccessor(NO_AU_PEERS_TABLE_NAME,
                                      new NoAuPeers(theDaemon));
    statusServ.registerStatusAccessor(PEERS_VOTE_TABLE_NAME,
                                      new PeerVoteSummary(theDaemon));
    statusServ.registerStatusAccessor(PEERS_REPAIR_TABLE_NAME,
                                      new PeerRepair(theDaemon));
    statusServ.registerStatusAccessor(FILE_VERSIONS_TABLE_NAME,
                                      new FileVersions(theDaemon));
    logger.debug2("Status accessors registered.");
  }

  public void stopService() {
    // unregister our status accessors
    LockssDaemon theDaemon = getDaemon();
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.unregisterStatusAccessor(SERVICE_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(NO_AU_PEERS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(AU_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(PEERS_VOTE_TABLE_NAME);
    statusServ.unregisterStatusAccessor(PEERS_REPAIR_TABLE_NAME);
    statusServ.unregisterStatusAccessor(FILE_VERSIONS_TABLE_NAME);
    logger.debug2("Status accessors unregistered.");
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    defaultNumRows = config.getInt(PARAM_MAX_NODES_TO_DISPLAY,
                                   DEFAULT_MAX_NODES_TO_DISPLAY);
    isContentIsLink = config.getBoolean(PARAM_CONTENT_IS_LINK,
					DEFAULT_CONTENT_IS_LINK);
    includeNeedsRecrawl = config.getBoolean(PARAM_INCLUDE_NEEDS_RECRAWL,
					    DEFAULT_INCLUDE_NEEDS_RECRAWL);
  }

  static CrawlManagerStatus getCMStatus(LockssDaemon daemon) {
    CrawlManager crawlMgr = daemon.getCrawlManager();
    CrawlManager.StatusSource source = crawlMgr.getStatusSource();
    return source.getStatus();
  }


  static class AuSummary implements StatusAccessor {
    static final String TABLE_TITLE = "Archival Units";

    static final String FOOT_STATUS = "Flags may follow status: C means the AU is complete, D means that the AU is no longer available from the publisher";

    static final String FOOT_SIZE = "If blank, size has changed and is being recalculated.  Check again later.";

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("AuName", "Volume", ColumnDescriptor.TYPE_STRING),
//       new ColumnDescriptor("AuNodeCount", "Nodes", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("AuSize", "Content Size",
			   ColumnDescriptor.TYPE_INT, FOOT_SIZE),
      new ColumnDescriptor("DiskUsage", "Disk Usage (MB)",
			   ColumnDescriptor.TYPE_FLOAT, FOOT_SIZE),
      new ColumnDescriptor("Peers", "Peers", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("AuPolls", "Recent Polls",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("Damaged", "Status",
                           ColumnDescriptor.TYPE_STRING,
			   FOOT_STATUS),
      new ColumnDescriptor("AuLastPoll", "Last Poll",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("AuLastCrawlAttempt", "Last Crawl Start",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("AuLastCrawlResultMsg", "Last Crawl Result",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("AuLastCrawl", "Last Successful Crawl",
                           ColumnDescriptor.TYPE_DATE)
      );

    private static final List sortRules =
      ListUtil.list(new
		    StatusTable.SortRule("AuName",
					 CatalogueOrderComparator.SINGLETON));

    private LockssDaemon theDaemon;
    private RepositoryManager repoMgr;
    private CrawlManagerStatus cmStatus;

    AuSummary(LockssDaemon theDaemon) {
      this.theDaemon = theDaemon;
      repoMgr = theDaemon.getRepositoryManager();
      cmStatus = getCMStatus(theDaemon);
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      List cols = columnDescriptors;
      if (theDaemon.isDetectClockssSubscription()) {
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
	  rowL.add(makeRow(au, nodeMan));
	  stats.aus++;
	} catch (Exception e) {
	  logger.warning("Unexpected expection building row", e);
	}
      }
      stats.restarting = pluginMgr.getNumAusRestarting();
      return rowL;
    }

    private Map makeRow(ArchivalUnit au, NodeManager nodeMan) {
      AuState auState = nodeMan.getAuState();
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
      long contentSize = AuUtil.getAuContentSize(au, false);
      if (contentSize != -1) {
	rowMap.put("AuSize", new Long(contentSize));
      }
      long du = AuUtil.getAuDiskUsage(au, false);
      if (du != -1) {
	rowMap.put("DiskUsage", new Double(((double)du) / (1024*1024)));
      }
      long lastCrawl = auState.getLastCrawlTime();
      long lastAttempt = auState.getLastCrawlAttempt();
      int lastResultCode = auState.getLastCrawlResult();
      String lastResult = auState.getLastCrawlResultMsg();
      rowMap.put("AuLastCrawl", new Long(lastCrawl));
      // AuState files that show a successful crawl but no lastAttempt just
      // have uninitialized lastXxx fields.  Display time and status of
      // last successful instead
      if (lastCrawl > 0 && lastAttempt <= 0) {
	lastAttempt = lastCrawl;
	lastResultCode = Crawler.STATUS_SUCCESSFUL;
	lastResult = "Successful";
      }
      rowMap.put("AuLastCrawlAttempt", new Long(lastAttempt));
      String lastCrawlStatus =
	lastCrawlStatus(au, lastCrawl, lastResultCode, lastResult);
      if (lastCrawlStatus != null) {
	rowMap.put("AuLastCrawlResultMsg", lastCrawlStatus);
      }

      rowMap.put("Peers", PeerRepair.makeAuRef("peers", au.getAuId()));
      rowMap.put("AuLastPoll", new Long(auState.getLastTopLevelPollTime()));
      
      Object stat;
      if (isV3) {
	int numPolls = v3status.getNumPolls(au.getAuId());
	rowMap.put("AuPolls", pollsRef(new Integer(numPolls), au));
        // Percent damaged.  It's scary to see '0% Agreement' if there's no
        // history, so we just show a friendlier message.
        //
        if (auState.getHighestV3Agreement() < 0 ||
	    auState.getLastTopLevelPollTime() <= 0) {
	  if (cmStatus.isRunningNCCrawl(au)) {
	    stat = new OrderedObject("Crawling", STATUS_ORDER_CRAWLING);
	  } else {
	    if (auState.lastCrawlTime > 0 || AuUtil.isPubDown(au)) {
	      stat = new OrderedObject("Waiting for Poll",
				       STATUS_ORDER_WAIT_POLL);
	    } else {
	      stat = new OrderedObject("Waiting for Crawl",
				       STATUS_ORDER_WAIT_CRAWL);
	    }
          }
        } else {
          stat = agreeStatus(auState.getHighestV3Agreement());
        }
      } else {
        rowMap.put("AuPolls",
                   theDaemon.getStatusService().
                   getReference(PollerStatus.MANAGER_STATUS_TABLE_NAME,
                                au));
	CachedUrlSet auCus = au.getAuCachedUrlSet();
	NodeState topNodeState = nodeMan.getNodeState(auCus);
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

      if (theDaemon.isDetectClockssSubscription()) {
	rowMap.put("Subscribed",
		   AuUtil.getAuState(au).getClockssSubscriptionStatusString());
      }

      return rowMap;
    }

    String lastCrawlStatus(ArchivalUnit au, long lastCrawl,
			   int lastResultCode, String lastResult) {
      if (AuUtil.isPubDown(au)) {
	if (lastCrawl > 0) {
	  return "No longer crawled";
	} else {
	  return "Never crawled";
	}
      } else {
	if (lastResultCode > 0) {
	  return lastResult;
	}
      }
      return null;
    }

    Object pollsRef(Object val, ArchivalUnit au) {
      return new StatusTable.Reference(val,
				       V3PollStatus.POLLER_STATUS_TABLE_NAME,
				       au.getAuId());
    }

    private List getSummaryInfo(Stats stats) {
      List res = new ArrayList();
      String numaus = StringUtil.numberOfUnits(stats.aus, "Archival Unit",
					       "Archival Units");
      res.add(new StatusTable.SummaryInfo(null,
					  ColumnDescriptor.TYPE_STRING,
					  numaus));
      if (stats.restarting != 0) {
	res.add(new StatusTable.SummaryInfo(null,
					    ColumnDescriptor.TYPE_STRING,
					    stats.restarting + " restarting"));
      }
      int n = repoMgr.sizeCalcQueueLen();
      if (n != 0) {
	res.add(new StatusTable.SummaryInfo(null,
					    ColumnDescriptor.TYPE_STRING,
					    n + " awaiting recalc"));
      }
      return res;
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
      stats.restarting = pluginMgr.getNumAusRestarting();
      return rowL;
    }

    private Map makeRow(ArchivalUnit au) {
      HashMap rowMap = new HashMap();
      rowMap.put("AuId", au.getAuId());
      rowMap.put("AuName", AuStatus.makeAuRef(au.getName(), au.getAuId()));
      return rowMap;
    }

    private List getSummaryInfo(Stats stats) {
      List res = new ArrayList();
      String numaus = StringUtil.numberOfUnits(stats.aus, "Archival Unit",
					       "Archival Units");
      res.add(new StatusTable.SummaryInfo(null,
					  ColumnDescriptor.TYPE_STRING,
					  numaus));
      if (stats.restarting != 0) {
	res.add(new StatusTable.SummaryInfo(null,
					    ColumnDescriptor.TYPE_STRING,
					    stats.restarting + " restarting"));
      }
      return res;
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
    protected CrawlManagerStatus cmStatus;

    PerAuTable(LockssDaemon theDaemon) {
      this.theDaemon = theDaemon;
      cmStatus = getCMStatus(theDaemon);
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
                           ColumnDescriptor.TYPE_INT)
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


    private List getRows(StatusTable table, ArchivalUnit au,
			 LockssRepository repo, NodeManager nodeMan) {
      int startRow = Math.max(0, getIntProp(table, "skiprows"));
      int numRows = getIntProp(table, "numrows");
      if (numRows <= 0) {
	numRows = defaultNumRows;
      }

      CrawlSpec spec = au.getCrawlSpec();
      List startUrls;
      if (spec instanceof SpiderCrawlSpec) {
	startUrls = ((SpiderCrawlSpec)spec).getStartingUrls();
      } else {
	startUrls = Collections.EMPTY_LIST;
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
          CachedUrlSetSpec cuss = new RangeCachedUrlSetSpec(cusn.getUrl());
          cus = au.makeCachedUrlSet(cuss);
        }
	String url = cus.getUrl();

	CachedUrl cu = au.makeCachedUrl(url);
        try {
	  Map row = makeRow(au, repo.getNode(url), cu,
			    startUrls.contains(url));
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
			CachedUrl cu, boolean isStartUrl) {
      String url = node.getNodeUrl();
      boolean hasContent = node.hasContent();
      Object val = url;
      if (isStartUrl) {
	val = new StatusTable.DisplayedValue(val).setBold(true);
      }
      HashMap rowMap = new HashMap();
      if (hasContent && isContentIsLink) {
	Properties args = new Properties();
	args.setProperty("auid", au.getAuId());
	args.setProperty("url", url);
	val =
	  new StatusTable.SrvLink(val,
				  AdminServletManager.SERVLET_DISPLAY_CONTENT,
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
      } else {
//         status = "Active";
      }
      if (status != null) {
	rowMap.put("NodeStatus", status);
      }
      Object versionObj = DASH;
      Object sizeObj = DASH;
      if (hasContent) {
	int version = node.getCurrentVersion();
        versionObj = new OrderedObject(new Long(version));
	if (version > 1) {
	  CachedUrl[] cuVersions = cu.getCuVersions(2);
	  if (cuVersions.length > 1) {
	    StatusTable.Reference verLink =
	      new StatusTable.Reference(versionObj,
					FILE_VERSIONS_TABLE_NAME, au.getAuId());
	    verLink.setProperty("url", url);
	    versionObj = verLink;
	  }
	}
        sizeObj = new OrderedObject(new Long(node.getContentSize()));
      }
      rowMap.put("NodeHasContent", (hasContent ? "yes" : "no"));
      rowMap.put("NodeVersion", versionObj);
      rowMap.put("NodeContentSize", sizeObj);
      if (!node.isLeaf()) {
	rowMap.put("NodeChildCount",
		   new OrderedObject(new Long(node.getChildCount())));
	long treeSize = node.getTreeContentSize(null, false);
	if (treeSize != -1) {
	  rowMap.put("NodeTreeSize", new OrderedObject(new Long(treeSize)));
	}
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
      
      // Make the status string.
      Object stat = null;
      Object recentPollStat = null;
      if (AuUtil.getProtocolVersion(au) == Poll.V3_PROTOCOL) {
        if (state.getV3Agreement() < 0) {
          if (state.lastCrawlTime < 0) {
            stat = "Waiting for Crawl";
          } else {
            stat = "Waiting for Poll";
          }
        } else {
          stat = agreeStatus(state.getHighestV3Agreement());
	  if (state.getHighestV3Agreement() != state.getV3Agreement()) {
	    recentPollStat = agreeStatus(state.getV3Agreement());
	  }
        }
      } else {
        stat = topNode.hasDamage() ? DAMAGE_STATE_DAMAGED : DAMAGE_STATE_OK;
      }
      
      long contentSize = AuUtil.getAuContentSize(au, false);
      long du = AuUtil.getAuDiskUsage(au, false);

      StatusTable.SrvLink urlListLink =
	new StatusTable.SrvLink("URL list",
				AdminServletManager.SERVLET_LIST_OBJECTS,
				PropUtil.fromArgs("type", "urls",
						  "auid", au.getAuId()));

      StatusTable.SrvLink doiListLink =
	new StatusTable.SrvLink("DOI list",
				AdminServletManager.SERVLET_LIST_OBJECTS,
				PropUtil.fromArgs("type", "dois",
						  "auid", au.getAuId()));

      StatusTable.SrvLink articleListLink =
	new StatusTable.SrvLink("Article list",
				AdminServletManager.SERVLET_LIST_OBJECTS,
				PropUtil.fromArgs("type", "articles",
						  "auid", au.getAuId()));

      StatusTable.SrvLink fileListLink =
	new StatusTable.SrvLink("File list",
				AdminServletManager.SERVLET_LIST_OBJECTS,
				PropUtil.fromArgs("type", "files",
						  "auid", au.getAuId()));

      List res = new ArrayList();
      res.add(new StatusTable.SummaryInfo("Volume",
					  ColumnDescriptor.TYPE_STRING,
					  au.getName()));
      res.add(new StatusTable.SummaryInfo("Plugin",
					  ColumnDescriptor.TYPE_STRING,
					  au.getPlugin().getPluginName()));
      String yearStr = AuUtil.getTitleAttribute(au, "year");
      if (yearStr != null) {
        res.add(new StatusTable.SummaryInfo("Year",
                                            ColumnDescriptor.TYPE_STRING,
                                            yearStr));
      }
      AuState.AccessType atype = state.getAccessType();
      if (atype != null) {
	res.add(new StatusTable.SummaryInfo("Access Type",
					    ColumnDescriptor.TYPE_STRING,
					    atype.toString()));
      }
      if (contentSize != -1) {
	res.add(new StatusTable.SummaryInfo("Content Size",
					    ColumnDescriptor.TYPE_INT,
					    new Long(contentSize)));
      } else {
	res.add(new StatusTable.SummaryInfo("Content Size",
					    ColumnDescriptor.TYPE_STRING,
					    "Awaiting recalc"));
      }
      if (du != -1) {
	 res.add(new StatusTable.SummaryInfo("Disk Usage (MB)",
					ColumnDescriptor.TYPE_FLOAT,
                                        new Float(du / (float)(1024 * 1024))));
      } else {
	res.add(new StatusTable.SummaryInfo("Disk Usage",
					    ColumnDescriptor.TYPE_STRING,
					    "Awaiting recalc"));
      }
      AuNodeImpl auNode = AuUtil.getAuRepoNode(au);
      String spec = LockssRepositoryImpl.getRepositorySpec(au);
      String repo = LockssRepositoryImpl.mapAuToFileLocation(LockssRepositoryImpl.getLocalRepositoryPath(spec), au);

      res.add(new StatusTable.SummaryInfo("Repository",
					  ColumnDescriptor.TYPE_STRING,
					  repo));
      res.add(new StatusTable.SummaryInfo("Status",
					  ColumnDescriptor.TYPE_STRING,
					  stat));
      if (recentPollStat != null) {
	res.add(new StatusTable.SummaryInfo("Most recent poll",
					    ColumnDescriptor.TYPE_STRING,
					    recentPollStat));
      }
      String plat = au.getPlugin().getPublishingPlatform();
      if (plat != null) {
	res.add(new StatusTable.SummaryInfo("Publishing Platform",
					    ColumnDescriptor.TYPE_STRING,
					    plat));
      }
      res.add(new StatusTable.SummaryInfo("Available From Publisher",
					  ColumnDescriptor.TYPE_STRING,
					  (AuUtil.isPubDown(au)
					   ? "No" : "Yes")));
//             res.add(new StatusTable.SummaryInfo("Volume Complete",
// 						ColumnDescriptor.TYPE_STRING,
// 						(AuUtil.isClosed(au)
// 						 ? "Yes" : "No")));
// 	    res.add(new StatusTable.SummaryInfo("Polling Protocol Version",
// 						ColumnDescriptor.TYPE_INT,
// 						new Integer(AuUtil.getProtocolVersion(au))));

      res.add(new StatusTable.SummaryInfo("Created",
					  ColumnDescriptor.TYPE_DATE,
					  new Long(state.getAuCreationTime())));

      AuUtil.AuProxyInfo aupinfo = AuUtil.getAuProxyInfo(au);
      if (aupinfo.isAuOverride()) {
	String disp = (aupinfo.getHost() == null
		       ? "Direct connection"
		       : aupinfo.getHost() + ":" + aupinfo.getPort());
	res.add(new StatusTable.SummaryInfo("Crawl proxy",
					    ColumnDescriptor.TYPE_STRING,
					    disp));
      }

      CrawlWindow window = au.getCrawlSpec().getCrawlWindow();
      if (window != null) {
	String wmsg = window.toString();
	if (wmsg.length() > 140) {
	  wmsg = "(not displayable)";
	}
	if (!window.canCrawl()) {
	  wmsg = "Currently closed: " + wmsg;
	}
	res.add(new StatusTable.SummaryInfo("Crawl Window",
					    ColumnDescriptor.TYPE_STRING,
					    wmsg));
      }
      long lastCrawlAttempt = state.getLastCrawlAttempt();
      res.add(new StatusTable.SummaryInfo("Last Completed Crawl",
					  ColumnDescriptor.TYPE_DATE,
					  new Long(state.getLastCrawlTime())));
      if (lastCrawlAttempt > 0) {
	res.add(new StatusTable.SummaryInfo("Last Crawl",
					    ColumnDescriptor.TYPE_DATE,
					    new Long(lastCrawlAttempt)));
	res.add(new StatusTable.SummaryInfo("Last Crawl Result",
					    ColumnDescriptor.TYPE_STRING,
					    state.getLastCrawlResultMsg()));
      }
      long lastPollStart = state.getLastPollStart();
      res.add(new StatusTable.SummaryInfo("Last Completed Poll",
					  ColumnDescriptor.TYPE_DATE,
					  new Long(state.getLastTopLevelPollTime())));
      if (lastPollStart > 0) {
	res.add(new StatusTable.SummaryInfo("Last Poll",
					    ColumnDescriptor.TYPE_DATE,
					    new Long(lastPollStart)));
	String pollResult = state.getLastPollResultMsg();
	if (!StringUtil.isNullString(pollResult)) {
	  res.add(new StatusTable.SummaryInfo("Last Poll Result",
					      ColumnDescriptor.TYPE_STRING,
					      pollResult));
	}
      }

      PollManager pm = theDaemon.getPollManager();
      boolean isCrawling = cmStatus.isRunningNCCrawl(au);
      boolean isPolling = pm.isPollRunning(au);
      List lst = new ArrayList();
      if (isCrawling) {
	lst.add(makeCrawlRef("Crawling", au));
      }
      if (isPolling) {
	lst.add(makePollRef("Polling", au));
      }
      if (!lst.isEmpty()) {
	res.add(new StatusTable.SummaryInfo("Current Activity",
					    ColumnDescriptor.TYPE_STRING,
					    lst));
      }
      if (theDaemon.isDetectClockssSubscription()) {
	String subStatus =
	  AuUtil.getAuState(au).getClockssSubscriptionStatusString();
	res.add(clockssPos,
		new StatusTable.SummaryInfo("Subscribed",
					    ColumnDescriptor.TYPE_STRING,
					    subStatus));
      }
      Object peers = PeerRepair.makeAuRef("Repair candidates",
					      au.getAuId());
      res.add(new StatusTable.SummaryInfo(null,
					  ColumnDescriptor.TYPE_STRING,
					  peers));

      res.add(new StatusTable.SummaryInfo(null,
					  ColumnDescriptor.TYPE_STRING,
					  urlListLink));
      if (!(au.getArticleIterator() == CollectionUtil.EMPTY_ITERATOR)) {
        res.add(new StatusTable.SummaryInfo(null,
					    ColumnDescriptor.TYPE_STRING,
					    doiListLink));
        res.add(new StatusTable.SummaryInfo(null,
					    ColumnDescriptor.TYPE_STRING,
					    articleListLink));
      }
      res.add(new StatusTable.SummaryInfo(null,
					  ColumnDescriptor.TYPE_STRING,
					  fileListLink));
      return res;
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

  static class FileVersions extends PerAuTable {

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("Version", "Version", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("Size", "Size", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("DateCollected", "Date Collected",
                           ColumnDescriptor.TYPE_DATE)
      );

    private static final List sortRules =
      ListUtil.list(new StatusTable.SortRule("sort", true));

    FileVersions(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected void populateTable(StatusTable table, ArchivalUnit au)
        throws StatusService.NoSuchTableException {
      String url = getStringProp(table, "url");
      table.setTitle("Versions of " + url + " in " + au.getName());
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(table, au, url));
    }

    private List getRows(StatusTable table, ArchivalUnit au, String url)
	throws StatusService.NoSuchTableException {
      int startRow = Math.max(0, getIntProp(table, "skiprows"));
      int numRows = getIntProp(table, "numrows");
      if (numRows <= 0) {
	numRows = defaultNumRows;
      }
      CachedUrl curCu = au.makeCachedUrl(url);
      if (curCu == null) {
	throw new StatusService.NoSuchTableException("URL " + url +
						     " not found in AU: " +
						     au.getName());
      }
      // Get array of versions.  One more than we need just to determine
      // whether to add a Next link
      CachedUrl[] cuVersions = curCu.getCuVersions(startRow + numRows + 1);

      List rowL = new ArrayList();
      if (startRow > 0) {
        // add 'previous'
        int start = startRow - defaultNumRows;
        if (start < 0) {
          start = 0;
        }
        rowL.add(makeOtherRowsLink(false, start, au.getAuId(), url));
      }
      int endRow1 = startRow + numRows; // end row + 1
      int curRow = -1;
      int curVer = curCu.getVersion() + 1;
      for (CachedUrl cu : cuVersions) {
	curRow++;
	curVer--;
        if (curRow < startRow) {
          continue;
        }
	if (curRow >= numRows) {
	  // add 'next'
	  rowL.add(makeOtherRowsLink(true, endRow1, au.getAuId(), url));
	  break;
	}
	Map row = makeRow(au, cu, curVer);
	row.put("sort", curRow);
	rowL.add(row);
      }
      return rowL;
    }

    private Map makeRow(ArchivalUnit au, CachedUrl cu, int ver) {
      String url = cu.getUrl();
      HashMap rowMap = new HashMap();
      Properties args = new Properties();
      args.setProperty("auid", au.getAuId());
      args.setProperty("url", url);
      args.setProperty("version", Integer.toString(ver));
      Object val =
	new StatusTable.SrvLink(Integer.toString(ver),
				AdminServletManager.SERVLET_DISPLAY_CONTENT,
				args);
      rowMap.put("Version", val);
      rowMap.put("Size", cu.getContentSize());
      Properties cuProps = cu.getProperties();
      long collected =
	Long.parseLong(cuProps.getProperty(CachedUrl.PROPERTY_FETCH_TIME));
      rowMap.put("DateCollected", collected);
      return rowMap;
    }

    private Map makeOtherRowsLink(boolean isNext, int startRow,
				  String auKey, String url) {
      HashMap rowMap = new HashMap();
      String label = (isNext ? "Next" : "Previous");
      StatusTable.Reference link =
          new StatusTable.Reference(label, FILE_VERSIONS_TABLE_NAME, auKey);
      link.setProperty("skiprows", Integer.toString(startRow));
      link.setProperty("numrows", Integer.toString(defaultNumRows));
      link.setProperty("url", url);
      rowMap.put("Version", link);
      rowMap.put("sort", new Integer(isNext ? Integer.MAX_VALUE : -1));
      return rowMap;
    }

  }

  /** list of peers that have said they don't have the AU.  Primarily for
   * stf */
  static class NoAuPeers extends PerAuTable {

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("Peer", "Peer", ColumnDescriptor.TYPE_STRING)
      );

    NoAuPeers(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected void populateTable(StatusTable table, ArchivalUnit au)
        throws StatusService.NoSuchTableException {
      HistoryRepository historyRepo = theDaemon.getHistoryRepository(au);
      table.setTitle("Peers not holding " + au.getName());
      DatedPeerIdSet noAuSet = theDaemon.getPollManager().getNoAuPeerSet(au);
      synchronized (noAuSet) {
	try {
	  noAuSet.load();
	  table.setSummaryInfo(getSummaryInfo(au, noAuSet));
	  table.setColumnDescriptors(columnDescriptors);
	  table.setRows(getRows(table, au, noAuSet));
	} catch (IOException e) {
	  String msg = "Couldn't load NoAuSet";
	  logger.warning(msg, e);
	  throw new StatusService.NoSuchTableException(msg, e);
	} finally {
	  noAuSet.release();
	}
      }
    }


    private List getRows(StatusTable table, ArchivalUnit au,
			 DatedPeerIdSet noAuSet) {
      List rows = new ArrayList();
      try {
	logger.info("noAuSet.size(): " + noAuSet.size());
      } catch (IOException e) {
	logger.error("noAuSet.size()", e);
      }
      for (PeerIdentity pid : noAuSet) {
	logger.info("pid: " + pid);
	Map row = new HashMap();
	row.put("Peer", pid.getIdString());
        rows.add(row);
      }
      return rows;
    }
    private List getSummaryInfo(ArchivalUnit au, DatedPeerIdSet noAuSet) {
      List res = new ArrayList();
      try {
	res.add(new StatusTable.SummaryInfo("Last cleared",
					    ColumnDescriptor.TYPE_DATE,
					    noAuSet.getDate()));
      } catch (IOException e) {
	logger.warning("Couldn't get date", e);
      }
      return res;
    }
  }

  abstract static class PeersAgreement extends PerAuTable {
    protected static final List sortRules =
      ListUtil.list(new StatusTable.SortRule("Box", true));

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
      rowMap.put("Box", id);
      return rowMap;
    }


    class CacheStats {
      PeerIdentity peer;
      int totalPolls = 0;
      int agreePolls = 0;
      Vote lastAgree;
      long lastAgreeTime = 0;
      Vote lastDisagree;
      long lastDisagreeTime = 0;
      float highestAgreement = 0.0f;
      float lastAgreement = 0.0f;
      float highestAgreementHint = -1.0f;
      float lastAgreementHint = -1.0f;

      CacheStats(PeerIdentity peer) {
	this.peer = peer;
      }
      boolean isLastAgree() {
	return (lastAgreeTime != 0  &&
		(lastDisagreeTime == 0 || lastAgreeTime >= lastDisagreeTime));
      }

      boolean isV3Agree() {
	PollManager pollmgr = theDaemon.getPollManager();
	return highestAgreement >= pollmgr.getMinPercentForRepair();
      }
    }
  }

  static class PeerVoteSummary extends PeersAgreement {
    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("Box", "Box",
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
      new ColumnDescriptor("Box", "Box",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Last", "Consensus",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("HighestPercentAgreement", "Highest Agreement",
                           ColumnDescriptor.TYPE_PERCENT),
      new ColumnDescriptor("LastPercentAgreement", "Last Agreement",
                           ColumnDescriptor.TYPE_PERCENT),
      new ColumnDescriptor("HighestPercentAgreementHint",
			   "Highest Agreement Hint",
                           ColumnDescriptor.TYPE_PERCENT),
      new ColumnDescriptor("LastPercentAgreementHint", "Last Agreement Hint",
                           ColumnDescriptor.TYPE_PERCENT),
      new ColumnDescriptor("LastAgree",
			   "Last Consensus",
                           ColumnDescriptor.TYPE_DATE)
      );

    PeerRepair(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected String getTitle(ArchivalUnit au) {
      return "Repair candidates for AU: " + au.getName();
    }
    private static final String FOOT_TITLE =
      "Peers whose Highest Agreement is above a threshold have proven\n" +
      "in the past that their content for this AU is the same, and will\n" +
      "be served repairs on request. Peers whose Highest Agreement Hint\n" +
      "is above the threshold have reported that this peer has proven to\n" +
      "them that the content for the AU is the same, and are likely to\n" +
      "supply repairs on request.";

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
	  if (ida.getHighestPercentAgreement() >= 0.0 ||
	      ida.getHighestPercentAgreementHint() >= 0.0) {
	    CacheStats stats = new CacheStats(pid);
	    statsMap.put(pid, stats);
	    stats.lastAgreeTime = ida.getLastAgree();
	    stats.lastDisagreeTime = ida.getLastDisagree();
	    stats.highestAgreement = ida.getHighestPercentAgreement();
	    stats.lastAgreement = ida.getPercentAgreement();
	    stats.highestAgreementHint = ida.getHighestPercentAgreementHint();
	    stats.lastAgreementHint = ida.getPercentAgreementHint();
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
      if (stats.highestAgreement >= 0.0f) {
	rowMap.put("LastPercentAgreement",
		   new Float(stats.lastAgreement));
	rowMap.put("HighestPercentAgreement",
		   new Float(stats.highestAgreement));
      }
      if (stats.highestAgreementHint >= 0.0f) {
	rowMap.put("LastPercentAgreementHint",
		   new Float(stats.lastAgreementHint));
	rowMap.put("HighestPercentAgreementHint",
		   new Float(stats.highestAgreementHint));
      }
      rowMap.put("LastAgree", new Long(stats.lastAgreeTime));
      rowMap.put("LastDisagree", new Long(stats.lastDisagreeTime));
      return rowMap;
    }

    protected List getSummaryInfo(ArchivalUnit au,
				  int totalPeers) {
      List summaryList =  ListUtil.list(
            new StatusTable.SummaryInfo("Peers holding AU",
					ColumnDescriptor.TYPE_INT,
                                        new Integer(totalPeers)));
      return summaryList;
    }

    // utility method for making a Reference
    public static StatusTable.Reference makeAuRef(Object value,
						  String key) {
      return new StatusTable.Reference(value, PEERS_REPAIR_TABLE_NAME,
				       key);
    }
  }

  static class Overview implements OverviewAccessor {

    private LockssDaemon daemon;
    private PluginManager pluginMgr;

    public Overview(LockssDaemon daemon) {
      this.daemon = daemon;
      this.pluginMgr = daemon.getPluginManager();
    }

    public Object getOverview(String tableName, BitSet options) {
      boolean isDebug = options.get(StatusTable.OPTION_DEBUG_USER);
      List res = new ArrayList();
      int total = 0;
      int internal = 0;
      int neverCrawled = 0;
      int needsRecrawl = 0;
      for (ArchivalUnit au : pluginMgr.getAllAus()) {
	if (pluginMgr.isInternalAu(au)) {
	  internal++;
	  if (!isDebug) {
	    continue;
	  }
	}
	total++;
	AuState aus = AuUtil.getAuState(au);
	if (AuUtil.isPubDown(au)) {
	} else {
	  if (aus.getLastCrawlTime() <= 0) {
	    neverCrawled++;
	  } else if (au.shouldCrawlForNewContent(aus)) {
	    needsRecrawl++;
	  }
	}
      }
      StringBuilder sb = new StringBuilder();
      sb.append(StringUtil.numberOfUnits(total, "Archival Unit",
					 "Archival Units"));
      if (isDebug) {
	appendIfNonZero(sb, internal, " (", " internal)");
      }
      appendIfNonZero(sb, pluginMgr.getNumAusRestarting(), " restarting");
      if (isDebug) {
	appendIfNonZero(sb, pluginMgr.getNumFailedAuRestarts(),
			" failed to restart");
      }
      appendIfNonZero(sb, neverCrawled, " not collected");
      if (includeNeedsRecrawl && needsRecrawl != 0) {
	sb.append(", ");
	sb.append(StringUtil.numberOfUnits(needsRecrawl,
					   " needs recrawl", " need recrawl"));
      }
      return new StatusTable.Reference(sb.toString(),
				       SERVICE_STATUS_TABLE_NAME);
    }

    void appendIfNonZero(StringBuilder sb, int counter, String postfix) {
      appendIfNonZero(sb, counter, ", ", postfix);
    }

    void appendIfNonZero(StringBuilder sb, int counter,
			 String prefix, String postfix) {
      if (counter != 0) {
	sb.append(prefix);
	sb.append(counter);
	sb.append(postfix);
      }
    }
  }

  static final double STATUS_ORDER_AGREE_BASE = 100.0;
  static final double STATUS_ORDER_WAIT_POLL = 200.0;
  static final double STATUS_ORDER_CRAWLING = 300.0;
  static final double STATUS_ORDER_WAIT_CRAWL = 400.0;

  static OrderedObject agreeStatus(double agreement) {
    return new OrderedObject(doubleToPercent(agreement) + "% Agreement",
			     STATUS_ORDER_AGREE_BASE - agreement);
  }


  public static StatusTable.Reference makeCrawlRef(Object value,
						   ArchivalUnit au) {
    return new StatusTable.Reference(value,
				     CrawlManagerImpl.CRAWL_STATUS_TABLE_NAME,
				     au.getAuId());
  }

  public static StatusTable.Reference makePollRef(Object value,
						   ArchivalUnit au) {
    return new StatusTable.Reference(value,
				     V3PollStatus.POLLER_STATUS_TABLE_NAME,
				     au.getAuId());
  }

  static int getIntProp(StatusTable table, String name) {
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

  static String getStringProp(StatusTable table, String name) {
    Properties props = table.getProperties();
    if (props == null) return null;
    return props.getProperty(name);
  }

  static class Stats {
    int aus = 0;
    int restarting = 0;
  }


}
