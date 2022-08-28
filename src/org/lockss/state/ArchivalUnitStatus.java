/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.crawler.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.servlet.*;

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
  static final int DEFAULT_MAX_NODES_TO_DISPLAY = 1000;

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

  /**
   * If true, Peer Agreement tables will take reputation transfers into
   * account.
   */
  public static final String PARAM_PEER_ARGEEMENTS_USE_REPUTATION_TRANSFERS =
    PREFIX + "peerArgeementsUseReputationTransfers";
  public static final boolean DEFAULT_PEER_ARGEEMENTS_USE_REPUTATION_TRANSFERS =
    true;

  public static final String SERVICE_STATUS_TABLE_NAME =
      "ArchivalUnitStatusTable";
  public static final String AUIDS_TABLE_NAME = "AuIds";
  public static final String AU_STATUS_TABLE_NAME = "ArchivalUnitTable";
  public static final String NO_AU_PEERS_TABLE_NAME = "NoAuPeers";
  public static final String PEERS_VOTE_TABLE_NAME = "PeerVoteSummary";
  public static final String PEERS_REPAIR_TABLE_NAME = "PeerRepair";
  public static final String PEER_AGREEMENT_TABLE_NAME = "PeerAgreement";
  public static final String FILE_VERSIONS_TABLE_NAME = "FileVersions";
  public static final String SUSPECT_VERSIONS_TABLE_NAME = "SuspectVersions";
  public static final String AU_DEFINITION_TABLE_NAME = "AuConfiguration";
  public static final String AUS_WITH_URL_TABLE_NAME = "AusWithUrl";


  private static final Logger logger = Logger.getLogger("AuStatus");
  
  private static int defaultNumRows = DEFAULT_MAX_NODES_TO_DISPLAY;
  private static boolean isContentIsLink = DEFAULT_CONTENT_IS_LINK;
  private static boolean includeNeedsRecrawl = DEFAULT_INCLUDE_NEEDS_RECRAWL;
  private static boolean peerArgeementsUseReputationTransfers =
    DEFAULT_PEER_ARGEEMENTS_USE_REPUTATION_TRANSFERS;
  
  private static final DecimalFormat agreementFormat =
    new DecimalFormat("0.00");

  /**
   * Format 100 times a double to print as a percentage. An input
   * value of 1.0 will produce "100.00".
   * @param a value to convert.
   * @return a String representing the double. 
   */
  static String doubleToPercent(double d) {
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
    statusServ.registerStatusAccessor(PEER_AGREEMENT_TABLE_NAME,
                                      new RawPeerAgreement(theDaemon));
    statusServ.registerStatusAccessor(FILE_VERSIONS_TABLE_NAME,
                                      new FileVersions(theDaemon));
    statusServ.registerStatusAccessor(SUSPECT_VERSIONS_TABLE_NAME,
                                      new SuspectVersions(theDaemon));
    statusServ.registerStatusAccessor(AU_DEFINITION_TABLE_NAME,
                                      new AuConfiguration(theDaemon));
    statusServ.registerStatusAccessor(AUS_WITH_URL_TABLE_NAME,
                                      new AusWithUrl(theDaemon));
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
    peerArgeementsUseReputationTransfers =
      config.getBoolean(PARAM_PEER_ARGEEMENTS_USE_REPUTATION_TRANSFERS,
			DEFAULT_PEER_ARGEEMENTS_USE_REPUTATION_TRANSFERS);
  }

  static CrawlManagerStatus getCMStatus(LockssDaemon daemon) {
    CrawlManager crawlMgr = daemon.getCrawlManager();
    CrawlManager.StatusSource source = crawlMgr.getStatusSource();
    return source.getStatus();
  }


  /** By default the AuSummary table omits the size columns.  Specify
   * columns=* to include them */
  static final String DEFAULT_AU_SUMMARY_COLUMNS = "-AuSize;DiskUsage";

  static class AuSummary implements StatusAccessor {
    static final String TABLE_TITLE = "Archival Units";

    static final String FOOT_STATUS = "Flags may follow status: C means the AU is complete, D means that the AU is no longer available from the publisher, NS means the AU has no files containing substantial content.";

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

    public boolean requiresKey() {
      return false;
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
      table.setColumnDescriptors(cols, DEFAULT_AU_SUMMARY_COLUMNS);
      table.setDefaultSortRules(sortRules);
      Set<String> inclCols = new HashSet<String>();
      for (ColumnDescriptor cd : table.getColumnDescriptors()) {
	inclCols.add(cd.getColumnName());
      }
      Stats stats = new Stats();
      table.setRows(getRows(table, inclCols, stats));
      table.setSummaryInfo(getSummaryInfo(table, inclCols, stats));
    }

    private List getRows(StatusTable table, Set<String> inclCols, Stats stats)
	throws StatusService.NoSuchTableException {
      PluginManager pluginMgr = theDaemon.getPluginManager();

      String key = table.getKey();
      Plugin onlyPlug = null;
      if (key != null) {
	if (!key.startsWith("plugin:")) {
	  throw new StatusService.NoSuchTableException("Unknown selector: "
						       + key);
	}
	String[] foo = org.apache.commons.lang3.StringUtils.split(key, ":", 2);
	if (foo.length < 2 || StringUtil.isNullString(foo[1])) {
	  throw new StatusService.NoSuchTableException("Empty plugin id: "
						       + key);
	}	  
	String plugid = foo[1];
	onlyPlug = pluginMgr.getPlugin(PluginManager.pluginKeyFromId(plugid));
	if (onlyPlug == null) {
	  throw new StatusService.NoSuchTableException("Plugin not found: "
						       + plugid);
	}
	table.setTitle(TABLE_TITLE + " for plugin " + onlyPlug.getPluginName());
      }	
      boolean includeInternalAus =
	table.getOptions().get(StatusTable.OPTION_DEBUG_USER);
      List rowL = new ArrayList();
      Collection<ArchivalUnit> aus;
      if (onlyPlug != null) {
	aus = onlyPlug.getAllAus();
      } else {
	aus = pluginMgr.getAllAus();
      }
      for (ArchivalUnit au : aus) {
	if (!includeInternalAus && pluginMgr.isInternalAu(au)) {
	  continue;
	}
	try {
	  NodeManager nodeMan = theDaemon.getNodeManager(au);
	  rowL.add(makeRow(au, nodeMan, inclCols));
	  stats.aus++;
	} catch (Exception e) {
	  logger.warning("Unexpected execption building row", e);
	}
      }
      stats.restarting = pluginMgr.getNumAusRestarting();
      return rowL;
    }

    private Map makeRow(ArchivalUnit au, NodeManager nodeMan, Set inclCols) {
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
      if (inclCols.contains("AuSize")) {
	long contentSize = AuUtil.getAuContentSize(au, false);
	if (contentSize != -1) {
	  rowMap.put("AuSize", new Long(contentSize));
	}
      }
      if (inclCols.contains("DiskUsage")) {
	long du = AuUtil.getAuDiskUsage(au, false);
	if (du != -1) {
	  rowMap.put("DiskUsage", new Double(((double)du) / (1024*1024)));
	}
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
      Object lastCrawlStatus =
	lastCrawlStatus(au, lastCrawl, lastResultCode, lastResult);
      if (lastCrawlStatus != null) {
	if (lastResultCode == Crawler.STATUS_SUCCESSFUL &&
	    auState.hasNoSubstance()) {
	  lastCrawlStatus =
	    new StatusTable.DisplayedValue(lastCrawlStatus).addFootnote(SingleCrawlStatusAccessor.FOOT_NO_SUBSTANCE_CRAWL_STATUS);
	}
	rowMap.put("AuLastCrawlResultMsg", lastCrawlStatus);
      }

      rowMap.put("Peers", PeerRepair.makeAuRef("peers", au.getAuId()));
      rowMap.put("AuLastPoll", new Long(auState.getLastTimePollCompleted()));
      
      Object stat;
      if (isV3) {
	int numPolls = v3status.getNumPolls(au.getAuId());
	rowMap.put("AuPolls", pollsRef(new Integer(numPolls), au));
        // Percent damaged.  It's scary to see '0% Agreement' if there's no
        // history, so we just show a friendlier message.
        //
        if (auState.getHighestV3Agreement() < 0 ||
	    auState.getLastTimePollCompleted() <= 0) {
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
      boolean noSubstance = auState.hasNoSubstance();
        
      if (isPubDown || isClosed || noSubstance) {
	List flags = new ArrayList();
	if (isClosed) {
	  flags.add("C");
	}
	if (isPubDown) {
	  flags.add("D");
	}
	if (noSubstance) {
	  flags.add("NS");
	}
	String flagStr = StringUtil.separatedString(flags, " (", ",", ")");
	stat = ListUtil.list(stat, flagStr);
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

    private List getSummaryInfo(StatusTable table, Set<String> inclCols,
				Stats stats) {
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
      if (inclCols.contains("AuSize") || inclCols.contains("DiskUsage")) {
	int n = repoMgr.sizeCalcQueueLen();
	if (n != 0) {
	  res.add(new StatusTable.SummaryInfo(null,
					      ColumnDescriptor.TYPE_STRING,
					      n + " awaiting recalc"));
	}
	StatusTable.Reference hideSize = 
	  new StatusTable.Reference("Hide AU Sizes",
				    ArchivalUnitStatus.SERVICE_STATUS_TABLE_NAME,
				    table.getKey());
	res.add(new StatusTable.SummaryInfo(null,
					    ColumnDescriptor.TYPE_STRING,
					    hideSize));
      } else {
	StatusTable.Reference showSize = 
	  new StatusTable.Reference("Show AU Sizes",
				    ArchivalUnitStatus.SERVICE_STATUS_TABLE_NAME,
				    table.getKey());
	showSize.setProperty("columns", "*");
	res.add(new StatusTable.SummaryInfo(null,
					    ColumnDescriptor.TYPE_STRING,
					    showSize));
      }
      return res;
    }
  }

  static class AuIds implements StatusAccessor {
    static final String TABLE_TITLE = "AU Ids";

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("AuName", "Volume", ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("AuId", "AU Id", ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("CrawlPool", "Crawl Pool",
			   ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Publisher", "Publisher",
			   ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Year", "Year", ColumnDescriptor.TYPE_STRING)
      );

    private static final List<String> defaultCols =
      ListUtil.list("AuName", "AuId");

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

    public boolean requiresKey() {
      return false;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      table.setColumnDescriptors(columnDescriptors, defaultCols);
      table.setDefaultSortRules(sortRules);
      Stats stats = new Stats();
      table.setRows(getRows(table, stats));
      table.setSummaryInfo(getSummaryInfo(stats));
    }

    private List getRows(StatusTable table,
			 Stats stats) {
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
	  rowL.add(makeRow(table, au));
	  stats.aus++;
	} catch (Exception e) {
	  logger.warning("Unexpected execption building row", e);
	}
      }
      stats.restarting = pluginMgr.getNumAusRestarting();
      return rowL;
    }

    private Map makeRow(StatusTable table,
			ArchivalUnit au) {
      HashMap rowMap = new HashMap();
      rowMap.put("AuId", au.getAuId());
      rowMap.put("AuName", AuStatus.makeAuRef(au.getName(), au.getAuId()));
      if (table.isIncludeColumn("CrawlPool")) {
	String rateKey = au.getFetchRateLimiterKey();
	rowMap.put("CrawlPool", rateKey != null ? rateKey : au.getAuId());
      }
      if (table.isIncludeColumn("Publisher")) {
	String pub = AuUtil.getTitleAttribute(au, "publisher");
	if (!StringUtil.isNullString(pub)) {
	  rowMap.put("Publisher", pub);
	}
      }
      if (table.isIncludeColumn("Year")) {
	String year = AuUtil.getTitleAttribute(au, "year");
	if (!StringUtil.isNullString(year)) {
	  rowMap.put("Year", year);
	}
      }
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

  static class AusWithUrl implements StatusAccessor {
    static final String TABLE_TITLE = "AUs containing URL";

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("AuName", "AU", ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Size", "Size", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("CollectedDate", "Date Collected",
			   ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("Versions", "Versions", ColumnDescriptor.TYPE_INT)
      );

    private static final List sortRules =
      ListUtil.list(new
		    StatusTable.SortRule("AuName",
					 CatalogueOrderComparator.SINGLETON));

    private LockssDaemon theDaemon;

    AusWithUrl(LockssDaemon theDaemon) {
      this.theDaemon = theDaemon;
    }

    public String getDisplayName() {
      throw new UnsupportedOperationException("Au table has no generic title");
    }

    public boolean requiresKey() {
      return true;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      String url = table.getKey();
      table.setTitle("AUs containing URL: " + url);
      try {
	List<CachedUrl> cuLst =
	  theDaemon.getPluginManager().findCachedUrls(url);
	if (cuLst.isEmpty()) {
	  table.setSummaryInfo(getNoMatchSummaryInfo());
	} else {
	  table.setColumnDescriptors(columnDescriptors);
	  table.setDefaultSortRules(sortRules);
	  table.setRows(getRows(table, cuLst));
	}
      } catch (Exception e) {
	logger.warning("Error building table", e);
	throw new StatusService.
	  NoSuchTableException("Error building table for URL: " + url, e);
      }
//       table.setSummaryInfo(getSummaryInfo(stats));
    }

    private List getRows(StatusTable table,
			 List<CachedUrl> cuLst) {
      PluginManager pluginMgr = theDaemon.getPluginManager();

      List rowL = new ArrayList();
      for (CachedUrl cu : cuLst) {
	try {
	  rowL.add(makeRow(table, cu));
	} catch (Exception e) {
	  logger.warning("Unexpected execption building row", e);
	}
      }
      return rowL;
    }

    private Map makeRow(StatusTable table, CachedUrl cu) {
      try {
	HashMap rowMap = new HashMap();
	ArchivalUnit au = cu.getArchivalUnit();
	rowMap.put("AuName", AuStatus.makeAuRef(au.getName(), au.getAuId()));


	long size = cu.getContentSize();
	Object val =
	  new StatusTable.SrvLink(cu.getContentSize(),
				  AdminServletManager.SERVLET_DISPLAY_CONTENT,
				  PropUtil.fromArgs("auid", au.getAuId(),
						    "url", cu.getUrl()));
	rowMap.put("Size", val);

	int version = cu.getVersion();
	Object versionObj = new Long(version);
	if (version > 1) {
	  CachedUrl[] cuVersions = cu.getCuVersions(2);
	  if (cuVersions.length > 1) {
	    StatusTable.Reference verLink =
	      new StatusTable.Reference(versionObj,
					FILE_VERSIONS_TABLE_NAME, au.getAuId());
	    verLink.setProperty("url", cu.getUrl());
	    versionObj = verLink;
	  }
	}
	rowMap.put("Versions", versionObj);
	Properties props = cu.getProperties();
	try {
	  long cdate =
	    Long.parseLong(props.getProperty(CachedUrl.PROPERTY_FETCH_TIME));
	  rowMap.put("CollectedDate",
		     ServletUtil.headerDf.format(new Date(cdate)));
	} catch (NumberFormatException ignore) {
	}
	return rowMap;
      } finally {
	AuUtil.safeRelease(cu);
      }
    }

    private List getNoMatchSummaryInfo() {
      List res = new ArrayList();
      res.add(new StatusTable.SummaryInfo(null,
					  ColumnDescriptor.TYPE_STRING,
					  "No AUs contain this URL"));
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
	  throw new StatusService.NoSuchTableException("Unknown AUID: " + key);
	}
	populateTable(table, au);
      } catch (StatusService.NoSuchTableException e) {
	throw e;
      } catch (Exception e) {
	logger.warning("Error building table", e);
	throw new StatusService.
	  NoSuchTableException("Error building table for AUID: " + key);
      }
    }

    protected abstract void populateTable(StatusTable table, ArchivalUnit au)
        throws StatusService.NoSuchTableException;
  }

  static class AuStatus extends PerAuTable {

    static final String FOOT_SERVE_AU_VS_CONTENT = "Serve AU serves this AU.  Serve Content constructs an OpenURL query from the bibliographic information for this AU in the title database, which may result in a choice of AUs if the content is available from more than one source.";

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
      table.setSummaryInfo(getSummaryInfo(table, au,
					  nodeMan.getAuState(), topNode));
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

      Collection<String> startUrls = au.getStartUrls();

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
	  // XXX Remove this when we move to a repository that
	  // distinguishes "foo/" from "foo".
	  String normUrl = url;
	  if (normUrl.endsWith(UrlUtil.URL_PATH_SEPARATOR)) {
	    normUrl = normUrl.substring(0, normUrl.length() - 1);
	  }
	  Map row = makeRow(au, repo.getNode(normUrl), cu, startUrls);
	  row.put("sort", new Integer(curRow));
          rowL.add(row);
        } catch (MalformedURLException ignore) {
	} finally {
	  AuUtil.safeRelease(cu);
	}
      }

      if (cusIter.hasNext()) {
        // add 'next'
        rowL.add(makeOtherRowsLink(true, endRow1, au.getAuId()));
      }
      return rowL;
    }

    private Map makeRow(ArchivalUnit au, RepositoryNode node,
			CachedUrl cu, Collection<String> startUrls) {
      boolean hasContent = node.hasContent();
      String url = null;
      boolean isStartUrl = false;
      if (false && hasContent) {
	// Repository v1 may return a name that omits the trailing slash
	// (even without removing it above).  Use the name explicitly
	// stored with the CU if any.
	Properties cuProps = cu.getProperties();
	url = cuProps.getProperty(CachedUrl.PROPERTY_NODE_URL);
	isStartUrl = startUrls.contains(url);
      }
      if (url == null) {
	url = node.getNodeUrl();
	isStartUrl |= startUrls.contains(url);
      }
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
      Object versionObj = StatusTable.NO_VALUE;
      Object sizeObj = StatusTable.NO_VALUE;
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
	rowMap.put("NodeChildCount", StatusTable.NO_VALUE);
	rowMap.put("NodeTreeSize", StatusTable.NO_VALUE);
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

    private List getSummaryInfo(StatusTable table,
				ArchivalUnit au, AuState state,
                                NodeState topNode) {
      boolean debug = table.getOptions().get(StatusTable.OPTION_DEBUG_USER);

      int clockssPos = 1;
      
      // Make the status string.
      Object stat = null;
      Object recentPollStat = null;
      if (AuUtil.getProtocolVersion(au) == Poll.V3_PROTOCOL) {
        if (state.getV3Agreement() < 0) {
          if (state.lastCrawlTime < 0  && !AuUtil.isPubDown(au)) {
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

      List res = new ArrayList();
      res.add(new StatusTable.SummaryInfo("Volume",
					  ColumnDescriptor.TYPE_STRING,
					  au.getName()));
      TdbAu tau = au.getTdbAu();
      if (tau != null) {
	addStringIfNotNull(res, tau.getJournalTitle(), "Journal Title");
      }
      Plugin plugin = au.getPlugin();
      res.add(new StatusTable.SummaryInfo("Plugin",
					  ColumnDescriptor.TYPE_STRING,
					  PluginStatus.makePlugRef(plugin.getPluginName(),
								   plugin)));
      addStringIfNotNull(res, AuUtil.getTitleAttribute(au, "year"), "Year");
      addStringIfNotNull(res, state.getAccessType(), "Access Type");
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
      addStringIfNotNull(res, recentPollStat, "Most recent poll");
      addStringIfNotNull(res, au.getPlugin().getPublishingPlatform(),
			 "Publishing Platform");
      addStringIfNotNull(res, AuUtil.getTitleAttribute(au, "publisher"),
			 "Publisher");
      res.add(new StatusTable.SummaryInfo("Available From Publisher",
					  ColumnDescriptor.TYPE_STRING,
					  (AuUtil.isPubDown(au)
					   ? "No" : "Yes")));
      SubstanceChecker.State subState = state.getSubstanceState();

      String coverageDepth =
	AuUtil.getTitleAttribute(au,
				 DefinableArchivalUnit.AU_COVERAGE_DEPTH_ATTR);
      if (!StringUtil.isNullString(coverageDepth)) {
	res.add(new StatusTable.SummaryInfo("Coverage Depth",
					    ColumnDescriptor.TYPE_STRING,
					    coverageDepth));
      }
      if (debug) {
	if (AuUtil.hasSubstancePatterns(au)) {
	  res.add(new StatusTable.SummaryInfo("Has Substance",
					      ColumnDescriptor.TYPE_STRING,
					      subState.toString()));
	}
      } else {
	switch (subState) {
	case No:
	  res.add(new StatusTable.SummaryInfo("AU has no files containing substantial content",
					      ColumnDescriptor.TYPE_STRING,
					      null));
	}
      }
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
      if (aupinfo.isInvalidAuOverride()) {
	String disp = "Error: Invalid AU proxy spec: " + aupinfo.getAuSpec();
	res.add(new StatusTable.SummaryInfo("Crawl proxy",
					    ColumnDescriptor.TYPE_STRING,
					    disp));
      } else if (aupinfo.isAuOverride()) {
	String disp = (aupinfo.getHost() == null
		       ? "Direct connection"
		       : aupinfo.getHost() + ":" + aupinfo.getPort());
	res.add(new StatusTable.SummaryInfo("Crawl proxy",
					    ColumnDescriptor.TYPE_STRING,
					    disp));
      }

      CrawlWindow window = au.getCrawlWindow();
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
      if (debug) {
	String crawlPool = au.getFetchRateLimiterKey();
	if (crawlPool == null) {
	  crawlPool = "(none)";
	}
	res.add(new StatusTable.SummaryInfo("Crawl Pool",
					    ColumnDescriptor.TYPE_STRING,
					    crawlPool));
      }
      CrawlManager crawlMgr = theDaemon.getCrawlManager();
      int crawlPrio = crawlMgr.getAuPriority(au);
      if (crawlPrio != 0) {
	String val;
	if (crawlPrio <= CrawlManagerImpl.ABORT_CRAWL_PRIORITY) {
	  val = crawlPrio + ": DISABLED, ABORT";
	} else if (crawlPrio <= CrawlManagerImpl.MIN_CRAWL_PRIORITY) {
	  val = crawlPrio + ": DISABLED";
	} else {
	  val = Integer.toString(crawlPrio);
	}
	res.add(new StatusTable.SummaryInfo("Crawl Priority",
					    ColumnDescriptor.TYPE_STRING,
					    val));
      }
      res.add(new StatusTable.SummaryInfo("Last Completed Crawl",
					  ColumnDescriptor.TYPE_DATE,
					  new Long(state.getLastCrawlTime())));
      long lastCrawlAttempt = state.getLastCrawlAttempt();
      if (lastCrawlAttempt > 0) {
	res.add(new StatusTable.SummaryInfo("Last Crawl",
					    ColumnDescriptor.TYPE_DATE,
					    new Long(lastCrawlAttempt)));
	res.add(new StatusTable.SummaryInfo("Last Crawl Result",
					    ColumnDescriptor.TYPE_STRING,
					    state.getLastCrawlResultMsg()));
	long lastDeepCrawlAttempt = state.getLastDeepCrawlAttempt();
	if (lastDeepCrawlAttempt > 0) {
	  res.add(new StatusTable.SummaryInfo("Last Completed Deep Crawl",
					      ColumnDescriptor.TYPE_DATE,
					      new Long(state.getLastDeepCrawlTime())));
	  res.add(new StatusTable.SummaryInfo("Last Deep Crawl",
					      ColumnDescriptor.TYPE_DATE,
					      new Long(lastDeepCrawlAttempt)));
	  res.add(new StatusTable.SummaryInfo("Last Deep Crawl Result",
					      ColumnDescriptor.TYPE_STRING,
					      state.getLastDeepCrawlResultMsg()));
	  res.add(new StatusTable.SummaryInfo("Last Deep Crawl Depth",
					      ColumnDescriptor.TYPE_INT,
					      state.getLastDeepCrawlDepth()));
	}
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
      String lastPoPMsg = state.getLastPoPPollResultMsg();
      if (!StringUtil.isNullString(lastPoPMsg)) {
	res.add(new StatusTable.SummaryInfo("Last PoP Poll",
					    ColumnDescriptor.TYPE_DATE,
					    new Long(state.getLastPoPPoll())));
	res.add(new StatusTable.SummaryInfo("Last PoP Poll Result",
					    ColumnDescriptor.TYPE_STRING,
					    lastPoPMsg));
      }
      long lastLocal = state.getLastLocalHashScan();
      if (lastLocal > 0) {
	res.add(new StatusTable.SummaryInfo("Last Local Hash Scan",
					    ColumnDescriptor.TYPE_DATE,
					    new Long(lastLocal)));
      }
      long lastIndex = state.getLastMetadataIndex();
      if (lastIndex > 0) {
	res.add(new StatusTable.SummaryInfo("Last Metadata Indexing",
					    ColumnDescriptor.TYPE_DATE,
					    new Long(lastIndex)));
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
      Object audef = AuConfiguration.makeAuRef("AU configuration",
					       au.getAuId());
      res.add(new StatusTable.SummaryInfo(null,
					  ColumnDescriptor.TYPE_STRING,
					  audef));
      List serveLinks = new ArrayList();

      Object saulink =
	new StatusTable.SrvLink("Serve AU",
				AdminServletManager.SERVLET_SERVE_CONTENT,
				PropUtil.fromArgs("auid", au.getAuId()));
      serveLinks.add(saulink);

      Object sclink =
	new StatusTable.SrvLink("Serve Content",
				AdminServletManager.SERVLET_SERVE_CONTENT,
				PropUtil.fromArgs("auid", au.getAuId(),
						  "use_openurl", "true"));

      serveLinks.add(", ");
      serveLinks.add(sclink);
      StatusTable.SummaryInfo serveSum =
	new StatusTable.SummaryInfo(null, ColumnDescriptor.TYPE_STRING,
				    serveLinks);
      serveSum.setValueFootnote(FOOT_SERVE_AU_VS_CONTENT);
      res.add(serveSum);

      List peerLinks = new ArrayList();
      peerLinks.add(PeerRepair.makeAuRef("Repair candidates", au.getAuId()));
      if (debug) {
	peerLinks.add(", ");
	peerLinks.add(RawPeerAgreement.makeAuRef("Peer agreements",
						 au.getAuId()));
      }
      res.add(new StatusTable.SummaryInfo(null,
					  ColumnDescriptor.TYPE_STRING,
					  peerLinks));

      if (AuUtil.hasSuspectUrlVersions(au)) {
	StatusTable.Reference suspectRef =
	  new StatusTable.Reference("Suspect Versions",
				    SUSPECT_VERSIONS_TABLE_NAME,
				    au.getAuId());

	res.add(new StatusTable.SummaryInfo(null,
					    ColumnDescriptor.TYPE_STRING,
					    suspectRef));
      }

      List urlLinks = new ArrayList();


      addLink(urlLinks,
	      new StatusTable
	      .SrvLink("URLs",
		       AdminServletManager.SERVLET_LIST_OBJECTS,
		       PropUtil.fromArgs("type", "urls",
					 "auid", au.getAuId())));

      addLink(urlLinks,
	      new StatusTable
	      .SrvLink("Files",
		       AdminServletManager.SERVLET_LIST_OBJECTS,
		       PropUtil.fromArgs("type", "urls",
					 "auid", au.getAuId(),
					 "fields", "ContentType,Size,PollWeight")));

      if (au.getArchiveFileTypes() != null) {
	addLink(urlLinks,
		new StatusTable
		.SrvLink("URLs*",
			 AdminServletManager.SERVLET_LIST_OBJECTS,
			 PropUtil.fromArgs("type", "urlsm",
					   "auid", au.getAuId())));

	addLink(urlLinks,
		new StatusTable
		.SrvLink("Files*",
			 AdminServletManager.SERVLET_LIST_OBJECTS,
			 PropUtil.fromArgs("type", "filesm",
					   "auid", au.getAuId())));
      }
      if (AuUtil.hasSubstancePatterns(au)) {
	addLink(urlLinks,
		new StatusTable
		.SrvLink("Substance URLs",
			 AdminServletManager.SERVLET_LIST_OBJECTS,
			 PropUtil.fromArgs("type", "suburls",
					   "auid", au.getAuId())));

	addLink(urlLinks,
		new StatusTable
		.SrvLink("(detail)",
			 AdminServletManager.SERVLET_LIST_OBJECTS,
			 PropUtil.fromArgs("type", "suburlsdetail",
					   "auid", au.getAuId())));

	addLink(urlLinks,
		new StatusTable
		.SrvLink("Substance Files",
			 AdminServletManager.SERVLET_LIST_OBJECTS,
			 PropUtil.fromArgs("type", "subfiles",
					   "auid", au.getAuId())));
      }
      if (AuUtil.hasContentValidator(au)) {
	addLink(urlLinks,
		new StatusTable
		.SrvLink("Validate Files",
			 AdminServletManager.SERVLET_LIST_OBJECTS,
			 PropUtil.fromArgs("type", "auvalidate",
					   "auid", au.getAuId())));
      }
      res.add(new StatusTable.SummaryInfo(null,
					  ColumnDescriptor.TYPE_STRING,
					  urlLinks));

      List artLinks = new ArrayList();
      if (ListObjects.hasArticleList(au)) {
	addLink(artLinks,
		new StatusTable
		.SrvLink("Articles",
			 AdminServletManager.SERVLET_LIST_OBJECTS,
			 PropUtil.fromArgs("type", "articles",
					   "auid", au.getAuId())));
      }
      if (ListObjects.hasArticleMetadata(au)) {
	addLink(artLinks,
		new StatusTable
		.SrvLink("DOIs",
			 AdminServletManager.SERVLET_LIST_OBJECTS,
			 PropUtil.fromArgs("type", "dois",
					   "auid", au.getAuId())));
	addLink(artLinks,
		new StatusTable
		.SrvLink("Metadata",
			 AdminServletManager.SERVLET_LIST_OBJECTS,
			 PropUtil.fromArgs("type", "metadata",
					   "auid", au.getAuId())));
      }
      if (!artLinks.isEmpty()) {
        res.add(new StatusTable.SummaryInfo(null,
					    ColumnDescriptor.TYPE_STRING,
					    artLinks));
      }
      return res;
    }

    void addLink(List links, StatusTable.SrvLink link) {
      if (links.isEmpty()) {
	links.add("List: ");
      } else {
	links.add(", ");
      }
      links.add(link);
    }

    void addStringIfNotNull(List lst, String val, String heading) {
      if (val != null) {
        lst.add(new StatusTable.SummaryInfo(heading,
                                            ColumnDescriptor.TYPE_STRING,
                                            val));
      }
    }

    void addStringIfNotNull(List lst, Object val, String heading) {
      if (val != null) {
        lst.add(new StatusTable.SummaryInfo(heading,
                                            ColumnDescriptor.TYPE_STRING,
                                            val.toString()));
      }
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

  static class AuConfiguration
    extends PerAuTable
    implements StatusAccessor.DebugOnly {

    static final String FOOT_PARAM_TYPE = "Def: definitional config params, determine identity of AU.  NonDef: other config params, may affect AU behavior.  Attr: values and objects computed from plugin definition and AU configuration.";

    private final List sortRules =
      ListUtil.list(new StatusTable.SortRule("sort", true),
		    new StatusTable.SortRule("key", true));

    private final List colDescs =
      ListUtil.list(
		    new ColumnDescriptor("type", "Type",
					 ColumnDescriptor.TYPE_STRING,
					 FOOT_PARAM_TYPE),
		    new ColumnDescriptor("key", "Key",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor("val", "Val",
					 ColumnDescriptor.TYPE_STRING)
		    );

    AuConfiguration(LockssDaemon daemon) {
      super(daemon);
    }

    public String getDisplayName() {
      return "AU Configuration";
    }

    protected String getTitle(ArchivalUnit au) {
      return "Configuration of " + au.getName();
    }

    public boolean requiresKey() {
      return true;
    }

    protected void populateTable(StatusTable table, ArchivalUnit au) {
      table.setTitle(getTitle(au));
      table.setDefaultSortRules(sortRules);
      TypedEntryMap paramMap = null;
      paramMap = au.getProperties();
      table.setColumnDescriptors(colDescs);
      table.setRows(getRows(au, paramMap));
//       table.setSummaryInfo(getSummaryInfo(au, paramMap));
    }

    public List getRows(ArchivalUnit au, TypedEntryMap paramMap) {
      List rows = new ArrayList();
      Plugin plug = au.getPlugin();
      Set<String> def = new TreeSet<String>();
      Set<String> nondef = new TreeSet<String>();
      Set<String> other = new TreeSet<String>();
      for (Map.Entry entry : paramMap.entrySet()) {
	String key = (String)entry.getKey();
	Object val = entry.getValue();
	ConfigParamDescr descr = plug.findAuConfigDescr(key);
	Map row = new HashMap();
	row.put("key", key);
	row.put("val", valString(val, descr));
	putTypeSort(row, key, au, descr);
	rows.add(row);
      }
      TdbAu tau = au.getTdbAu();
      if (tau != null) {
	Map<String,String> tdbother = new HashMap<String,String>();
	tdbother.put("Name", tau.getName());
	tdbother.put("Publisher", tau.getPublisherName());
	addTdbRows(rows, tdbother, "Tdb", 10);
	addTdbRows(rows, tau.getAttrs(), "TdbAttr", 11);
	addTdbRows(rows, tau.getProperties(), "TdbProp", 12);
      }

      return rows;
    }

    String valString(Object val, ConfigParamDescr descr) {
      if (val == null) {
	return "(null)";
      } else if (val instanceof org.apache.oro.text.regex.Perl5Pattern) {
	return ((org.apache.oro.text.regex.Perl5Pattern)val).getPattern();
      } else if (descr == null) {
	return val.toString();
      } else {
	switch (descr.getType()) {
	case ConfigParamDescr.TYPE_USER_PASSWD:
	  if (val instanceof List) {
	    List l = (List)val;
	    return l.get(0) + ":******";
	  }
	  break;
	default:
	  return val.toString();
	}
      }
      return val.toString();
    }

    void addTdbRows(List rows, Map<String,String> tdbMap,
		    String type, int sort) {
      for (Map.Entry<String,String> ent : tdbMap.entrySet()) {
	String key = ent.getKey();
	String val = ent.getValue();
	Map row = new HashMap();
	row.put("key", key);
	row.put("val", val);
	row.put("type", type);
	row.put("sort", sort);
	rows.add(row);
      }
    }

    void putTypeSort(Map row, String key, ArchivalUnit au,
		     ConfigParamDescr descr) {
      // keys not in au config are computed, others are definitional or not
      // according to their ConfigParamDescr.
      if (descr == null || !au.getConfiguration().containsKey(key)) {
	row.put("type", "Attr");
	row.put("sort", 3);
      } else if (descr.isDefinitional()) {
	row.put("type", "Def");
	row.put("sort", 1);
      } else {
	row.put("type", "NonDef");
	row.put("sort", 2);
      }
    }

//     private List getSummaryInfo(ArchivalUnit au, TypedEntryMap paramMap) {
//       List res = new ArrayList();
//       return res;
//     }

    public static StatusTable.Reference makeAuRef(Object value,
                                                  String key) {
      StatusTable.Reference ref =
	new StatusTable.Reference(value, AU_DEFINITION_TABLE_NAME, key);
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
      try {
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
	  try {
	    curRow++;
	    curVer--;
	    if (curRow < startRow) {
	      continue;
	    }
	    if (curRow >= endRow1) {
	      // add 'next'
	      rowL.add(makeOtherRowsLink(true, endRow1, au.getAuId(), url));
	      break;
	    }
	    Map row = makeRow(au, cu, curVer);
	    row.put("sort", curRow);
	    rowL.add(row);
	  } finally {
	    AuUtil.safeRelease(cu);
	  }
	}
	return rowL;
      } finally {
	AuUtil.safeRelease(curCu);
      }
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
      try {
	long collected =
	  Long.parseLong(cuProps.getProperty(CachedUrl.PROPERTY_FETCH_TIME));
	rowMap.put("DateCollected", collected);
      } catch (NumberFormatException ignore) {
      }
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

  static class SuspectVersions extends PerAuTable {

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("Url", "Url", ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Version", "Version", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("Discovered", "Discovered",
			   ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("Computed", "Computed Hash",
			   ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Stored", "Stored Hash",
			   ColumnDescriptor.TYPE_STRING)
      );

    private static final List sortRules =
      ListUtil.list(new StatusTable.SortRule("Url", true));

    SuspectVersions(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected void populateTable(StatusTable table, ArchivalUnit au)
        throws StatusService.NoSuchTableException {
      table.setTitle("Suspect URLs in " + au.getName());
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(table, au));
    }

    private List getRows(StatusTable table, ArchivalUnit au)
	throws StatusService.NoSuchTableException {
      AuSuspectUrlVersions asuv;
      if (!AuUtil.hasSuspectUrlVersions(au) ||
	  (asuv = AuUtil.getSuspectUrlVersions(au)).isEmpty()) {
	return null;
      }
      List rowL = new ArrayList();
      for (AuSuspectUrlVersions.SuspectUrlVersion suv : asuv.getSuspectList()) {
	Map row = new HashMap();
	row.put("Url", suv.getUrl());
	row.put("Version", suv.getVersion());
	row.put("Discovered", suv.getCreated());
	row.put("Computed", suv.getComputedHash().toString());
	row.put("Stored", suv.getStoredHash().toString());
	rowL.add(row);
	}
      return rowL;
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

  abstract static class BaseAgreementTable extends PerAuTable {
    protected static final List sortRules =
      ListUtil.list(new StatusTable.SortRule("Box", true));

    BaseAgreementTable(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected Map makeRow(PeerIdentity peerId) {
      Map rowMap = new HashMap();

      Object str = peerId.getIdString();
      if (peerId.isLocalIdentity()) {
	StatusTable.DisplayedValue val =
	  new StatusTable.DisplayedValue(str);
	val.setBold(true);
	str = val;
      }
      rowMap.put("Box", str);
      return rowMap;
    }

    protected Map makeRow(CacheStats stats) {
      return makeRow(stats.peer);
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
      long highestAgreementTime = 0;
      float lastAgreement = 0.0f;
      float highestAgreementHint = -1.0f;
      float lastAgreementHint = -1.0f;

      long lastAgreementTime = 0;
      long highestAgreementHintTime = 0;
      long lastAgreementHintTime = 0;

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

  // Obsolete - V1 only
  static class PeerVoteSummary extends BaseAgreementTable {
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

  static class PeerRepair extends BaseAgreementTable {

    /** query arg specifies poll type: {pop, por, symmetric_por,
     * symmetric_pop}.  If unspecified or other, table includes all poll
     * types */
    public static final String POLL_TYPE = "polltype";

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("Box", "Box",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("HighestPercentAgreement", "Highest Agreement",
                           ColumnDescriptor.TYPE_AGREEMENT),
      new ColumnDescriptor("HighestPercentAgreementDate", "When",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("LastPercentAgreement", "Last Agreement",
                           ColumnDescriptor.TYPE_AGREEMENT),
      new ColumnDescriptor("LastPercentAgreementDate", "When",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("HighestPercentAgreementHint",
			   "Highest Agreement Hint",
                           ColumnDescriptor.TYPE_AGREEMENT),
      new ColumnDescriptor("HighestPercentAgreementHintDate", "When",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("LastPercentAgreementHint", "Last Agreement Hint",
                           ColumnDescriptor.TYPE_AGREEMENT),
      new ColumnDescriptor("LastPercentAgreementHintDate", "When",
                           ColumnDescriptor.TYPE_DATE)
      );

    PeerRepair(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected String getTitle(StatusTable table, ArchivalUnit au) {
      String polltype = getStringProp(table, POLL_TYPE);
      AgreementType at = getAgreementType(polltype);
      if (at != null) {
	return at.toString() + " agreements for AU: " + au.getName();
      }
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
      table.setTitle(getTitle(table, au));
      table.setTitleFootnote(FOOT_TITLE);
      int totalPeers = 0;
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(columnDescriptors);
	table.setDefaultSortRules(sortRules);

	String agmntName = getStringProp(table, POLL_TYPE);
	AgreementType type = getAgreementType(agmntName);
	AgreementType[] types;
	if (type != null) {
	  types = new AgreementType[] { type };
	} else {
	  types = AgreementType.primaryTypes();
	}
	Map statsMap = buildCacheStats(au, idMgr, types);
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

    AgreementType getAgreementType(String agmntName) {
      if ("pop".equalsIgnoreCase(agmntName)) {
	return AgreementType.POP;
      } else if ("por".equalsIgnoreCase(agmntName)) {
	return AgreementType.POR;
      } else if ("symmetric_por".equalsIgnoreCase(agmntName)) {
	return AgreementType.SYMMETRIC_POR;
      } else if ("symmetric_pop".equalsIgnoreCase(agmntName)) {
	return AgreementType.SYMMETRIC_POP;
      } else {
	return null;
      }
    }

    public Map buildCacheStats(ArchivalUnit au, IdentityManager idMgr,
			       AgreementType[] types) {
      Map<PeerIdentity,CacheStats> statsMap =
	new HashMap<PeerIdentity,CacheStats>();
      for (AgreementType type : types) {
	Map<PeerIdentity, PeerAgreement> amap =
	  idMgr.getAgreements(au, type);
	Map<PeerIdentity, PeerAgreement> ahintmap =
	  idMgr.getAgreements(au, AgreementType.getHintType(type));

	for (Map.Entry<PeerIdentity, PeerAgreement> ent : amap.entrySet()) {
	  PeerIdentity pid = ent.getKey();
	  CacheStats stats = statsMap.get(pid);
	  if (stats == null) {
	    stats = new CacheStats(pid);
	    statsMap.put(pid, stats);
	  }
	  PeerAgreement pa = ent.getValue();
	  if (pa.getHighestPercentAgreement() >= 0.0 &&
	      pa.getHighestPercentAgreement() > stats.highestAgreement) {
	    stats.highestAgreement = pa.getHighestPercentAgreement();
	    stats.highestAgreementTime = pa.getHighestPercentAgreementTime();
	  }
	  if (pa.getPercentAgreement() >= 0.0 &&
	      pa.getPercentAgreementTime() > stats.lastAgreementTime) {
	    stats.lastAgreementTime = pa.getPercentAgreementTime();
	    stats.lastAgreement = pa.getPercentAgreement();
	  }
	}	    
	for (Map.Entry<PeerIdentity, PeerAgreement> ent : ahintmap.entrySet()) {
	  PeerIdentity pid = ent.getKey();
	  CacheStats stats = statsMap.get(pid);
	  if (stats == null) {
	    stats = new CacheStats(pid);
	    statsMap.put(pid, stats);
	  }
	  PeerAgreement pa = ent.getValue();
	  if (pa.getHighestPercentAgreement() >= 0.0 &&
	      pa.getHighestPercentAgreement() > stats.highestAgreementHint) {
	    stats.highestAgreementHint = pa.getHighestPercentAgreement();
	    stats.highestAgreementHintTime = pa.getHighestPercentAgreementTime();
	  }
	  if (pa.getPercentAgreement() >= 0.0 &&
	      pa.getPercentAgreementTime() > stats.lastAgreementHintTime) {
	    stats.lastAgreementHintTime = pa.getPercentAgreementTime();
	    stats.lastAgreementHint = pa.getPercentAgreement();
	  }
	}	    
      }
      return statsMap;
    }

    protected Map makeRow(CacheStats stats) {
      Map rowMap = super.makeRow(stats);
      if (stats.highestAgreement >= 0.0f) {
	rowMap.put("LastPercentAgreement",
		   new Float(stats.lastAgreement));
	rowMap.put("LastPercentAgreementDate",
		   new Long(stats.lastAgreementTime));
	rowMap.put("HighestPercentAgreement",
		   new Float(stats.highestAgreement));
	rowMap.put("HighestPercentAgreementDate",
		   new Long(stats.highestAgreementTime));
      }
      if (stats.highestAgreementHint >= 0.0f) {
	rowMap.put("LastPercentAgreementHint",
		   new Float(stats.lastAgreementHint));
	rowMap.put("LastPercentAgreementHintDate",
		   new Long(stats.lastAgreementHintTime));
	rowMap.put("HighestPercentAgreementHint",
		   new Float(stats.highestAgreementHint));
	rowMap.put("HighestPercentAgreementHintDate",
		   new Long(stats.highestAgreementHintTime));
      }
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

  static class RawPeerAgreement extends BaseAgreementTable {
    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("Box", "Peer", ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Type", "Type", ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("Last", "Last",
			   ColumnDescriptor.TYPE_AGREEMENT),
      new ColumnDescriptor("LastDate", "Last Date",
			   ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("Highest", "Highest",
			   ColumnDescriptor.TYPE_AGREEMENT),
      new ColumnDescriptor("HighestDate", "Highest Date",
			   ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("LastHint", "Last Hint",
			   ColumnDescriptor.TYPE_AGREEMENT),
      new ColumnDescriptor("LastHintDate", "Last Hint Date",
			   ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("HighestHint", "Highest Hint",
			   ColumnDescriptor.TYPE_AGREEMENT),
      new ColumnDescriptor("HighestHintDate", "Highest Hint Date",
			   ColumnDescriptor.TYPE_DATE)
      );

    RawPeerAgreement(LockssDaemon theDaemon) {
      super(theDaemon);
    }

    protected String getTitle(ArchivalUnit au) {
      return "Peer Agreements for AU: " + au.getName();
    }

    protected void populateTable(StatusTable table, ArchivalUnit au)
        throws StatusService.NoSuchTableException {
      IdentityManager idMgr = theDaemon.getIdentityManager();
      table.setTitle(getTitle(au));
      int totalPeers = 0;
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(columnDescriptors);
	table.setDefaultSortRules(ListUtil.list(new StatusTable.SortRule("Box",
									 true),
						new StatusTable.SortRule("Type",
									 true)));
	List rowL = new ArrayList();
	for (Map.Entry<PeerIdentity,Map<AgreementType,PeerAgreement>> ent :
	       buildCacheStats(au, idMgr).entrySet()) {
	  PeerIdentity peer = ent.getKey();
	  for (AgreementType type : AgreementType.primaryAndWeightedTypes()) {
	    Map<AgreementType,PeerAgreement> typeMap = ent.getValue();
	    PeerAgreement pa = typeMap.get(type);
	    PeerAgreement pahint = typeMap.get(AgreementType.getHintType(type));
	    if (pa != null || pahint != null) {
	      rowL.add(makeRow(peer, type, pa, pahint));
	    }
	  }
	}
	table.setRows(rowL);
      }
    }


    public Map<PeerIdentity,Map<AgreementType,PeerAgreement>>
	  buildCacheStats(ArchivalUnit au, IdentityManager idMgr) {

      ReputationTransfers repXfer = null;
      if (peerArgeementsUseReputationTransfers) {
	repXfer = new ReputationTransfers(idMgr);
      }

      Map<PeerIdentity,Map<AgreementType,PeerAgreement>> res =
	new HashMap<PeerIdentity,Map<AgreementType,PeerAgreement>>();
      for (AgreementType type : AgreementType.allTypes()) {
	Map<PeerIdentity, PeerAgreement> peerMap =
	  idMgr.getAgreements(au, type);
	if (peerMap != null) {
	  for (Map.Entry<PeerIdentity,PeerAgreement> ent : peerMap.entrySet()) {
	    PeerIdentity pid = ent.getKey();
	    if (repXfer != null) {
	      pid = repXfer.getPeerInheritingReputation(pid);
	    }
	    Map<AgreementType,PeerAgreement> typeMap = res.get(pid);
	    if (typeMap == null) {
	      typeMap =
		new EnumMap<AgreementType,PeerAgreement>(AgreementType.class);
	      res.put(pid, typeMap);
	    }

// 	    typeMap.put(type, ent.getValue());
 	    typeMap.put(type, ent.getValue().mergeWith(typeMap.get(type)));
	  }
	}
      }
      return res;
    }

    public Map makeRow(PeerIdentity peerId, AgreementType type,
		       PeerAgreement pa, PeerAgreement pahint) {
      Map row = super.makeRow(peerId);
      row.put("Type", type.toString());
      if (pa != null) {
	if (pa.getPercentAgreement() >= 0.0) {
	  row.put("Last", new Float(pa.getPercentAgreement()));
	  row.put("LastDate", pa.getPercentAgreementTime());
	}
	if (pa.getHighestPercentAgreement() >= 0.0) {
	  row.put("Highest", new Float(pa.getHighestPercentAgreement()));
	  row.put("HighestDate", pa.getHighestPercentAgreementTime());
	}
      }
      if (pahint != null) {
	if (pahint.getPercentAgreement() >= 0.0) {
	  row.put("LastHint", new Float(pahint.getPercentAgreement()));
	  row.put("LastHintDate", pahint.getPercentAgreementTime());
	}
	if (pahint.getHighestPercentAgreement() >= 0.0) {
	  row.put("HighestHint",
		  new Float(pahint.getHighestPercentAgreement()));
	  row.put("HighestHintDate",
		  pahint.getHighestPercentAgreementTime());
	}
      }
      return row;
    }

    // utility method for making a Reference
    public static StatusTable.Reference makeAuRef(Object value, String key) {
      return new StatusTable.Reference(value, PEER_AGREEMENT_TABLE_NAME, key);
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
	try {
	  AuState aus = AuUtil.getAuState(au);
	  if (AuUtil.isPubDown(au)) {
	  } else {
	    if (!aus.hasCrawled()) {
	      neverCrawled++;
	    } else if (au.shouldCrawlForNewContent(aus)) {
	      needsRecrawl++;
	    }
	  }
	} catch (RuntimeException e) {
	  // Guard against AUs in a transient state being deleted.
	  logger.warning("AU in bad state: " + au, e);
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
