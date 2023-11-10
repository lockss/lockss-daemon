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

package org.lockss.state;

import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.app.*;

/**
 * NodeManagerManager is the center of the per AU NodeManagers.  It manages
 * the NodeManager config parameters and status tables.
 */
public class NodeManagerManager
  extends BaseLockssDaemonManager implements ConfigurableManager {

  private static Logger logger = Logger.getLogger(NodeManagerManager.class);

  static final String PREFIX = Configuration.PREFIX + "state.";

  static final String GLOBAL_CACHE_PREFIX = PREFIX + "globalNodeCache.";
  public static final String PARAM_MAX_GLOBAL_CACHE_SIZE =
    GLOBAL_CACHE_PREFIX + "size";
  public static final int DEFAULT_MAX_GLOBAL_CACHE_SIZE = 500;

  public static final String PARAM_GLOBAL_CACHE_ENABLED =
    GLOBAL_CACHE_PREFIX + "enabled";
  public static final boolean DEFAULT_GLOBAL_CACHE_ENABLED = false;

  /** Size of the {@link UniqueRefLruCache} used as a node cache by the
   * node manager. */
  public static final String PARAM_MAX_PER_AU_CACHE_SIZE =
    PREFIX + "cache.size";
  static final int DEFAULT_MAX_PER_AU_CACHE_SIZE = 100;

  /** Minimum delay after unsuccessful poll on a node before recalling that
   * poll. */
  public static final String PARAM_RECALL_DELAY = PREFIX + "recall.delay";
  static final long DEFAULT_RECALL_DELAY = Constants.DAY;

  /** * Determines whether polls that should be running but aren't (because
   * of restart, etc.) are restarted before their deadline has expired. */
  public static final String PARAM_RESTART_NONEXPIRED_POLLS =
    PREFIX + "restart.nonexpired.polls";
  static final boolean DEFAULT_RESTART_NONEXPIRED_POLLS = false;

  public static final String SERVICE_STATUS_TABLE_NAME = "NodeManagerService";
  public static final String MANAGER_STATUS_TABLE_NAME = "NodeManager";
  public static final String POLLHISTORY_STATUS_TABLE_NAME = "PollHistory";

  long paramRecallDelay = DEFAULT_RECALL_DELAY;
  boolean paramRestartNonexpiredPolls = DEFAULT_RESTART_NONEXPIRED_POLLS;
  int paramNodeStateCacheSize = DEFAULT_MAX_PER_AU_CACHE_SIZE;
  boolean paramIsGlobalNodeCache = DEFAULT_GLOBAL_CACHE_ENABLED;
  int paramGlobalNodeCacheSize = DEFAULT_MAX_GLOBAL_CACHE_SIZE;
  UniqueRefLruCache globalNodeCache =
      new UniqueRefLruCache(DEFAULT_MAX_GLOBAL_CACHE_SIZE);

  public void startService() {
    super.startService();

//     StatusService statusServ = getDaemon().getStatusService();
//     statusServ.registerStatusAccessor(SERVICE_STATUS_TABLE_NAME,
// 				      new ServiceStatus(this));
//     statusServ.registerStatusAccessor(MANAGER_STATUS_TABLE_NAME,
// 				      new ManagerStatus(this));
//     statusServ.registerStatusAccessor(POLLHISTORY_STATUS_TABLE_NAME,
// 				      new PollHistoryStatus(this));
    logger.debug2("Status accessors registered.");
  }

  public void stopService() {
    // unregister our status accessors
//     StatusService statusServ = getDaemon().getStatusService();
//     statusServ.unregisterStatusAccessor(SERVICE_STATUS_TABLE_NAME);
//     statusServ.unregisterStatusAccessor(MANAGER_STATUS_TABLE_NAME);
//     statusServ.unregisterStatusAccessor(POLLHISTORY_STATUS_TABLE_NAME);
    logger.debug2("Status accessors unregistered.");

    super.stopService();
  }

  public void setConfig(Configuration config,
			Configuration prevConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      paramRecallDelay = config.getTimeInterval(PARAM_RECALL_DELAY,
						DEFAULT_RECALL_DELAY);
      paramRestartNonexpiredPolls =
        config.getBoolean(PARAM_RESTART_NONEXPIRED_POLLS,
			  DEFAULT_RESTART_NONEXPIRED_POLLS);

      paramNodeStateCacheSize =
	config.getInt(PARAM_MAX_PER_AU_CACHE_SIZE,
		      DEFAULT_MAX_PER_AU_CACHE_SIZE);
      for (Iterator iter = getDaemon().getAllNodeManagers().iterator();
	   iter.hasNext(); ) {
	NodeManager nm = (NodeManager)iter.next();
	if (nm instanceof NodeManagerImpl) {
	  NodeManagerImpl nmi = (NodeManagerImpl)nm;
	  nmi.setNodeStateCacheSize(paramNodeStateCacheSize);
	}
      }
    }
    if (changedKeys.contains(GLOBAL_CACHE_PREFIX)) {
      paramIsGlobalNodeCache = config.getBoolean(PARAM_GLOBAL_CACHE_ENABLED,
						 DEFAULT_GLOBAL_CACHE_ENABLED);
      if (paramIsGlobalNodeCache) {
	paramGlobalNodeCacheSize = config.getInt(PARAM_MAX_GLOBAL_CACHE_SIZE,
						 DEFAULT_MAX_GLOBAL_CACHE_SIZE);
	logger.debug("global node cache size: " + paramGlobalNodeCacheSize);
	globalNodeCache.setMaxSize(paramGlobalNodeCacheSize);
      }
    }
  }

  public boolean isGlobalNodeCache() {
    return paramIsGlobalNodeCache;
  }

  public UniqueRefLruCache getGlobalNodeCache() {
    return globalNodeCache;
  }

  private NodeManagerImpl getNodeManagerFromKey(String key)
      throws StatusService.NoSuchTableException {
    for (Iterator iter = getDaemon().getAllNodeManagers().iterator();
	 iter.hasNext(); ) {
      NodeManager manager = (NodeManager)iter.next();
      if (manager.getAuState().getArchivalUnit().getAuId().equals(key)) {
        return (NodeManagerImpl)manager;
      }
    }
    throw new StatusService.NoSuchTableException("No NodeManager for ID " +
                                                 key);
  }

  static class ServiceStatus implements StatusAccessor {

    static final String TABLE_TITLE = "NodeManager Service Table";
    static final String COL_AU_NAME = "AuName";
    static final String COL_CRAWL_TIME = "CrawlTime";
    static final String COL_TOP_LEVEL_POLL = "TopLevelPoll";
    static final String COL_TREE_WALK = "TreeWalk";

    private static final List columnDescriptors = ListUtil.list(
         new ColumnDescriptor(COL_AU_NAME, "AU Name",
			      ColumnDescriptor.TYPE_STRING),
         new ColumnDescriptor(COL_CRAWL_TIME, "Last Crawl Time",
                              ColumnDescriptor.TYPE_DATE),
         new ColumnDescriptor(COL_TOP_LEVEL_POLL, "Last Top Level Poll",
                              ColumnDescriptor.TYPE_DATE),
         new ColumnDescriptor(COL_TREE_WALK, "Last Tree Walk",
                              ColumnDescriptor.TYPE_DATE)
         );

    private static final List sortRules =
      ListUtil.list(new StatusTable.SortRule(COL_AU_NAME, CatalogueOrderComparator.SINGLETON));

    private NodeManagerManager mgr;
    private PluginManager pluginMgr;

    ServiceStatus(NodeManagerManager mgr) {
      this.mgr = mgr;
      pluginMgr = mgr.getDaemon().getPluginManager();
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(table.getOptions().get(StatusTable.OPTION_DEBUG_USER)));
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows(boolean includeInternalAus) {
      List rowL = new ArrayList();
      for (Iterator iter = mgr.getDaemon().getAllNodeManagers().iterator();
	   iter.hasNext(); ) {
	NodeManager manager = (NodeManager)iter.next();
	AuState auState = manager.getAuState();
        if (auState != null) {
	  if (!includeInternalAus &&
	      pluginMgr.isInternalAu(auState.getArchivalUnit())) {
	    continue;
	  }
          rowL.add(makeRow(manager));
        }
      }
      return rowL;
    }

    private Map makeRow(NodeManager manager) {
      HashMap rowMap = new HashMap();
      AuState state = manager.getAuState();
      ArchivalUnit au = state.getArchivalUnit();

      //"AuID"
      rowMap.put(COL_AU_NAME, ManagerStatus.makeNodeManagerRef(au.getName(),
          au.getAuId()));

      //"Status"
      rowMap.put(COL_CRAWL_TIME, new Long(state.getLastCrawlTime()));
      rowMap.put(COL_TOP_LEVEL_POLL, new Long(state.getLastTopLevelPollTime()));
      rowMap.put(COL_TREE_WALK, new Long(state.getLastTreeWalkTime()));

      return rowMap;
    }
  }

  static class ManagerStatus implements StatusAccessor {

    static final String TABLE_TITLE = "NodeManager Status Table";
    static final String COL_URL = "URL";
    static final String COL_STATE = "State";
    static final String COL_CRAWL_TIME = "CrawlTime";
    static final String COL_CRAWL_TYPE = "CrawlType";
    static final String COL_CRAWL_STATUS = "CrawlStatus";
    static final String COL_NUM_POLLS = "NumPolls";
    static final String COL_ACTIVE_POLLS = "ActivePolls";
    static final String COL_POLL_TIME = "PollTime";
    static final String COL_POLL_TYPE = "PollType";
    static final String COL_POLL_RANGE = "PollRange";
    static final String COL_POLL_STATUS = "PollStatus";

    private static final List columnDescriptors = ListUtil.list(
        new ColumnDescriptor(COL_URL, "URL", ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(COL_STATE, "State", ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(COL_CRAWL_TIME, "Last Crawl",
                             ColumnDescriptor.TYPE_DATE),
        new ColumnDescriptor(COL_CRAWL_TYPE, "Crawl Type",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(COL_CRAWL_STATUS, "Crawl Status",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(COL_NUM_POLLS, "Polls Run",
                             ColumnDescriptor.TYPE_INT),
        new ColumnDescriptor(COL_ACTIVE_POLLS, "Active Polls",
                             ColumnDescriptor.TYPE_INT),
        new ColumnDescriptor(COL_POLL_TIME, "Last Poll",
                             ColumnDescriptor.TYPE_DATE),
        new ColumnDescriptor(COL_POLL_TYPE, "Poll Type",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(COL_POLL_RANGE, "Poll Range",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(COL_POLL_STATUS, "Poll Status",
                             ColumnDescriptor.TYPE_STRING)
        );

    private static final List sortRules = ListUtil.list(
        new StatusTable.SortRule(COL_POLL_TIME, false),
        new StatusTable.SortRule(COL_URL, true)
        );

    private NodeManagerManager mgr;

    ManagerStatus(NodeManagerManager mgr) {
      this.mgr = mgr;
    }

    public String getDisplayName() {
      throw new
	UnsupportedOperationException("Node table has no generic title");
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      NodeManagerImpl nodeManager = mgr.getNodeManagerFromKey(table.getKey());
      String auname = nodeManager.getAuState().getArchivalUnit().getName();

      table.setTitle(getTitle(auname));
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
      String auId = nodeManager.getAuState().getArchivalUnit().getAuId();
      ArrayList entriesL = new ArrayList();
      while (entries.hasNext()) {
        NodeState state = (NodeState) entries.next();
        entriesL.add(makeRow(auId, state));
      }
      return entriesL;
    }

    private String getTitle(String key) {
      return "NodeManager Cache for Archival Unit " + key;
    }

    // currently unused
    private void filterActiveNodes(List entriesList, NodeState state) {
      int status = state.getCrawlState().getStatus();
      if ((status != CrawlState.FINISHED) &&
          (status != CrawlState.NODE_DELETED)) {
        entriesList.add(state);
      }
    }

    private Map makeRow(String auId, NodeState state) {
      HashMap rowMap = new HashMap();
      String url = state.getCachedUrlSet().getUrl();
      // URL
      rowMap.put(COL_URL, url);

      // State
      rowMap.put(COL_STATE, state.getStateString());

      CrawlState crawl_state = state.getCrawlState();
      // CrawlTime
      rowMap.put(COL_CRAWL_TIME, new Long(crawl_state.getStartTime()));
      // CrawlType
      rowMap.put(COL_CRAWL_TYPE, crawl_state.getTypeString());
      // CrawlStatus
      rowMap.put(COL_CRAWL_STATUS, crawl_state.getStatusString());

      // ActivePolls with a reference to a active poll history table
      StatusTable.Reference ref = PollHistoryStatus.makeNodeRef(
          new Integer(getActivePolls(state).size()),
          auId,
          url,
          PollHistoryStatus.ACTIVE_POLLS_FILTER);
      rowMap.put(COL_ACTIVE_POLLS, ref);

      // NumPolls with a reference to a all poll history table
      ref = PollHistoryStatus.makeNodeRef(
          new Integer(getPollHistories(state).size()),
          auId,
          url,
          PollHistoryStatus.ALL_POLLS_FILTER);
      rowMap.put(COL_NUM_POLLS, ref);

      // our most recent poll data
      PollHistory poll_history = state.getLastPollHistory();
      if (poll_history != null) {
        // PollTime
        rowMap.put(COL_POLL_TIME,
                   new Long(poll_history.getStartTime()));
        // PollType
        rowMap.put(COL_POLL_TYPE, poll_history.getTypeString());
        // PollRange
        rowMap.put(COL_POLL_RANGE, poll_history.getRangeString());
        // PollStatus
        rowMap.put(COL_POLL_STATUS, poll_history.getStatusString());
      }
      else {
        rowMap.put(COL_POLL_TIME, new Long(0));
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

  static class PollHistoryStatus implements StatusAccessor {

    static final String TABLE_TITLE = "Node Poll History Table";
    static final String COL_START_TIME = "StartTime";
    static final String COL_DURATION = "Duration";
    static final String COL_TYPE = "Type";
    static final String COL_RANGE = "Range";
    static final String COL_STATUS = "Status";
    static final String COL_NUM_AGREE = "NumAgree";
    static final String COL_NUM_DISAGREE = "NumDisagree";

    public static String ALL_POLLS_FILTER = "ALLPOLLS:";
    public static String ACTIVE_POLLS_FILTER = "ACTIVEPOLLS:";

    private static final List columnDescriptors = ListUtil.list(
        new ColumnDescriptor(COL_START_TIME, "Start Time",
                             ColumnDescriptor.TYPE_DATE),
        new ColumnDescriptor(COL_DURATION, "Duration",
                             ColumnDescriptor.TYPE_TIME_INTERVAL),
        new ColumnDescriptor(COL_TYPE, "Type",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(COL_RANGE, "Range",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(COL_STATUS, "Status",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(COL_NUM_AGREE, "Agree",
                             ColumnDescriptor.TYPE_INT),
        new ColumnDescriptor(COL_NUM_DISAGREE, "Disagree",
                             ColumnDescriptor.TYPE_INT)
        );

    private static final List sortRules =
        ListUtil.list(new StatusTable.SortRule(COL_START_TIME, false));

    private NodeManagerManager mgr;

    PollHistoryStatus(NodeManagerManager mgr) {
      this.mgr = mgr;
    }

    public String getDisplayName() {
      throw new
	UnsupportedOperationException("Node table has no generic title");
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
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
                                                    String auId, String url,
                                                    String filter) {
      StringBuffer key_buf = new StringBuffer(filter);
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
      } else {
        histories = state.getPollHistories();
      }
      while (histories.hasNext()) {
        PollState history = (PollState) histories.next();
        entriesL.add(makeRow(history));
      }
      return entriesL;
    }

    private String getTitle(NodeState state) {
      return "Poll History at Node " + state.getCachedUrlSet().getUrl();
    }

    private Map makeRow(PollState history) {
      HashMap rowMap = new HashMap();
      // PollTime
      rowMap.put(COL_START_TIME, new Long(history.getStartTime()));
      long duration = 0;
      if (history instanceof PollHistory) {
        // Duration
        duration = ((PollHistory)history).getDuration();
      }
      rowMap.put(COL_DURATION, new Long(duration));
     // PollType
      rowMap.put(COL_TYPE, history.getTypeString());
      // PollRange
      rowMap.put(COL_RANGE, history.getRangeString());
      // PollStatus
      rowMap.put(COL_STATUS, history.getStatusString());

      int agree = 0;
      int disagree = 0;

      if (history instanceof PollHistory) {
        Iterator votes = ((PollHistory)history).getVotes();
        while (votes.hasNext()) {
          Vote vote = (Vote)votes.next();
          if (vote.isAgreeVote()) {
            agree++;
          } else {
            disagree++;
          }
        }
      }
      // YesVotes
      rowMap.put(COL_NUM_AGREE, new Integer(agree));
      // NoVotes
      rowMap.put(COL_NUM_DISAGREE, new Integer(disagree));

      return rowMap;
    }

    // key support

    private String getPollFilterFromKey(String key)
        throws StatusService.NoSuchTableException {
      if (key.startsWith(ALL_POLLS_FILTER)) {
        return ALL_POLLS_FILTER;
      } else if (key.startsWith(ACTIVE_POLLS_FILTER)) {
        return ACTIVE_POLLS_FILTER;
      } else {
        throw new StatusService.NoSuchTableException("Unknown filter for key: "+
                                                     key);
      }
    }

    private NodeManagerImpl getNodeManagerFromKey(String key, String filter)
        throws StatusService.NoSuchTableException {
      int pos = filter.length();

      String au_id = key.substring(pos, key.lastIndexOf("&"));
      logger.debug("getting node manager " + au_id + " from key:[" + key + "]");
      return mgr.getNodeManagerFromKey(au_id);
    }

    private NodeState getNodeStateFromKey(NodeManager nodeManager, String key)
        throws StatusService.NoSuchTableException {
      int pos = key.lastIndexOf("&");
      String url = key.substring(pos + 1);
      logger.debug("finding node state for url: " + url);
      ArchivalUnit au = nodeManager.getAuState().getArchivalUnit();
      CachedUrlSet cus =
	au.makeCachedUrlSet(new RangeCachedUrlSetSpec(url));
      NodeState state = nodeManager.getNodeState(cus);
      if (state == null) {
        logger.debug("unable to find a node state for " + url);
        throw new StatusService.NoSuchTableException("No Node State for "
            + key);
      }
      return state;
    }
  }
}
