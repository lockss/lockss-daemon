/*
 * $Id: ArchivalUnitStatus.java,v 1.6 2004-03-27 02:38:16 eaalto Exp $
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
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.repository.*;

/**
 * Collect and report the status of the ArchivalUnits
 */
public class ArchivalUnitStatus extends BaseLockssManager {
  /**
   * The maximum number of nodes to display in a single page of the ui.
   */
  public static final String PARAM_MAX_NODES_TO_DISPLAY =
      Configuration.PREFIX + "state.max.nodes.to.display";
  static final int DEFAULT_MAX_NODES_TO_DISPLAY = 100;

  public static final String SERVICE_STATUS_TABLE_NAME =
      "ArchivalUnitStatusTable";
  public static final String AU_STATUS_TABLE_NAME = "ArchivalUnitTable";

  private static Logger logger = Logger.getLogger("AuStatus");
  private static int nodesToDisplay;

  public void startService() {
    super.startService();

    StatusService statusServ = theDaemon.getStatusService();

    statusServ.registerStatusAccessor(ArchivalUnitStatus.SERVICE_STATUS_TABLE_NAME,
                                      new ArchivalUnitStatus.ServiceStatus(theDaemon));
    statusServ.registerStatusAccessor(ArchivalUnitStatus.AU_STATUS_TABLE_NAME,
                                      new ArchivalUnitStatus.AuStatus(theDaemon));
    logger.debug2("Status accessors registered.");
  }

  public void stopService() {
    // unregister our status accessors
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.unregisterStatusAccessor(
      ArchivalUnitStatus.SERVICE_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(
      ArchivalUnitStatus.AU_STATUS_TABLE_NAME);
    logger.debug2("Status accessors unregistered.");

    super.stopService();
  }

  protected void setConfig(Configuration config, Configuration oldConfig,
                           Set changedKeys) {
    nodesToDisplay = config.getInt(PARAM_MAX_NODES_TO_DISPLAY,
                                   DEFAULT_MAX_NODES_TO_DISPLAY);
  }

  private static ArchivalUnit getArchivalUnit(String auId,
      LockssDaemon theDaemon) {
    return theDaemon.getPluginManager().getAuFromId(auId);
  }

  static class ServiceStatus implements StatusAccessor {
    static final String TABLE_TITLE = "ArchivalUnit Status Table";

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("AuName", "Volume", ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("AuNodeCount", "Nodes", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("AuSize", "Size", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("AuLastCrawl", "Last Crawl",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("AuLastPoll", "Last Poll",
                           ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor("AuLastTreeWalk", "Last TreeWalk",
                           ColumnDescriptor.TYPE_DATE)
      );

    private static final List sortRules =
      ListUtil.list(new StatusTable.SortRule("AuName", true));

    private static LockssDaemon theDaemon;

    ServiceStatus(LockssDaemon theDaemon) {
      this.theDaemon = theDaemon;
    }

    public String getDisplayName() {
      return TABLE_TITLE;
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows());
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows() {
      List rowL = new ArrayList();
      for (Iterator iter = theDaemon.getPluginManager().getAllAus().iterator();
	   iter.hasNext(); ) {
        ArchivalUnit au = (ArchivalUnit)iter.next();
        NodeManager nodeMan = theDaemon.getNodeManager(au);
        LockssRepository repo = theDaemon.getLockssRepository(au);
        RepositoryNode repoNode = null;
        try {
          repoNode = repo.getNode(au.getAuCachedUrlSet().getUrl());
        } catch (MalformedURLException ignore) { }
        rowL.add(makeRow(au, nodeMan.getAuState(), repoNode));
      }
      return rowL;
    }

    private Map makeRow(ArchivalUnit au, AuState state,
                        RepositoryNode repoNode) {
      HashMap rowMap = new HashMap();
      //"AuID"
      rowMap.put("AuName", AuStatus.makeAuRef(au.getName(),
          au.getAuId()));
      //XXX start caching this info
      rowMap.put("AuNodeCount", new Integer(-1));
      rowMap.put("AuSize", new Long(repoNode.getTreeContentSize(null)));
      rowMap.put("AuLastCrawl", new Long(state.getLastCrawlTime()));
      rowMap.put("AuLastPoll", new Long(state.getLastTopLevelPollTime()));
      rowMap.put("AuLastTreeWalk", new Long(state.getLastTreeWalkTime()));

      return rowMap;
    }
  }

  static class AuStatus implements StatusAccessor {
    static final String TABLE_TITLE = "AU Status Table";
    static final String KEY_SUFFIX = "&&&";

    private static final List columnDescriptors = ListUtil.list(
      new ColumnDescriptor("NodeName", "Node Url",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("NodeStatus", "Status",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("NodeHasContent", "Content",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor("NodeVersion", "Version",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("NodeContentSize", "Size",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("NodeChildCount", "Children",
                           ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("NodeTreeSize", "Tree Size",
                           ColumnDescriptor.TYPE_INT)
      );

    private static final List sortRules = ListUtil.list(
      new StatusTable.SortRule("NodeName", false)
      );

    private static LockssDaemon theDaemon;

    AuStatus(LockssDaemon theDaemon) {
      this.theDaemon = theDaemon;
    }

    public String getDisplayName() {
      throw new
	UnsupportedOperationException("Au table has no generic title");
    }

    public void populateTable(StatusTable table)
        throws StatusService.NoSuchTableException {
      String key = table.getKey();
      int index = key.lastIndexOf(KEY_SUFFIX);
      int startRow = 0;
      if (index >= 0) {
        try {
          String rowStr = key.substring(index + KEY_SUFFIX.length());
          startRow = Integer.parseInt(rowStr);
        } catch (NumberFormatException ignore) { }

        key = key.substring(0, index);
      }

      ArchivalUnit au = getArchivalUnit(key, theDaemon);
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
      table.setColumnDescriptors(columnDescriptors);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(au, repo, nodeMan, startRow));
    }

    public boolean requiresKey() {
      return true;
    }

    private List getRows(ArchivalUnit au, LockssRepository repo,
                         NodeManager nodeMan, int startRow) {
      List rowL = new ArrayList();
      Iterator cusIter = au.getAuCachedUrlSet().contentHashIterator();
      int rowCount = 0;
      int endRow = startRow + nodesToDisplay;

      if (startRow > 0) {
        // add 'previous'
        int start = startRow - nodesToDisplay;
        if (start < 0) {
          start = 0;
        }
        rowL.add(makeOtherRowsLink(false, start, au.getAuId()));
      }

      boolean hasMoreRows = false;
      while (cusIter.hasNext()) {
        if (rowCount < startRow) {
          cusIter.next();
          rowCount++;
          continue;
        }
        if (rowCount >= endRow) {
          hasMoreRows = true;
          break;
        }
        CachedUrlSetNode cusn = (CachedUrlSetNode)cusIter.next();
        CachedUrlSet cus;
        if (cusn.getType() == cusn.TYPE_CACHED_URL_SET) {
          cus = (CachedUrlSet)cusn;
        } else {
          CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(cusn.getUrl());
          cus = au.getPlugin().makeCachedUrlSet(au, spec);
        }
        try {
          rowL.add(makeRow(repo.getNode(cus.getUrl()),
                           nodeMan.getNodeState(cus)));
        } catch (MalformedURLException ignore) { }
        rowCount++;
      }

      if (hasMoreRows) {
        // add 'next'
        rowL.add(makeOtherRowsLink(true, endRow, au.getAuId()));
      }
      return rowL;
    }

    private String getTitle(String key) {
      return "Status Table for AU: " + key;
    }

    private List getSummaryInfo(ArchivalUnit au, AuState state,
                                NodeState topNode, RepositoryNode repoNode) {
      List summaryList =  ListUtil.list(
            new StatusTable.SummaryInfo("Volume" , ColumnDescriptor.TYPE_STRING,
                                        au.getName()),
            new StatusTable.SummaryInfo("Nodes", ColumnDescriptor.TYPE_INT,
                                        new Integer(-1)),
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
            new StatusTable.SummaryInfo("Has Damage",
                                        ColumnDescriptor.TYPE_STRING,
                                        (topNode.hasDamage() ? "yes" : "no")),
            new StatusTable.SummaryInfo("Current Activity",
                                        ColumnDescriptor.TYPE_STRING,
                                        "-")
            );
        return summaryList;
    }

    private Map makeRow(RepositoryNode node, NodeState state) {
      HashMap rowMap = new HashMap();
      rowMap.put("NodeName", node.getNodeUrl());

      String status;
      if (node.isDeleted()) {
        status = "Deleted";
      } else if (node.isContentInactive()) {
        status = "Inactive";
      } else if (state.hasDamage()) {
        status = "Damaged";
      } else {
        status = "Active";
      }
      rowMap.put("NodeStatus", status);
      boolean content = node.hasContent();
      Object versionObj = "-";
      Object sizeObj = "-";
      if (content) {
        versionObj = new Integer(node.getCurrentVersion());
        sizeObj = new Long(node.getContentSize());
      }
      rowMap.put("NodeHasContent", (content ? "yes" : "no"));
      rowMap.put("NodeVersion", versionObj);
      rowMap.put("NodeContentSize", sizeObj);
      Object childObj = "-";
      if (!node.isLeaf()) {
        childObj = new Integer(node.getChildCount());
      }
      rowMap.put("NodeChildCount", childObj);
      rowMap.put("NodeContentSize", new Long(node.getTreeContentSize(null)));

      return rowMap;
    }

    private Map makeOtherRowsLink(boolean isNext, int startRow, String auKey) {
      HashMap rowMap = new HashMap();
      String label = (isNext ? "Next" : "Previous");
      StatusTable.Reference link =
          new StatusTable.Reference(label, AU_STATUS_TABLE_NAME,
                                    auKey + KEY_SUFFIX + startRow);
      String rows = ""+(startRow+1)+"-"+(startRow + nodesToDisplay);
      rowMap.put("NodeName", link);
      rowMap.put("NodeStatus", rows);
      rowMap.put("NodeHasContent", "");
      rowMap.put("NodeVersion", "");
      rowMap.put("NodeContentSize", "");
      rowMap.put("NodeChildCount", "");
      rowMap.put("NodeContentSize", "");

      return rowMap;
    }

    // utility method for making a Reference
    public static StatusTable.Reference makeAuRef(Object value,
                                                  String key) {
      return new StatusTable.Reference(value, AU_STATUS_TABLE_NAME,
                                       key + KEY_SUFFIX + "0");
    }
  }
}
