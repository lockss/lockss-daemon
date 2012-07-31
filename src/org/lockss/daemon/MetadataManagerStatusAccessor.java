/*
 * $Id: MetadataManagerStatusAccessor.java,v 1.3 2012-07-31 23:36:31 pgust Exp $
 */

/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.daemon;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.derby.iapi.sql.Row;
import org.lockss.daemon.MetadataManager.ReindexingStatus;
import org.lockss.daemon.MetadataManager.ReindexingTask;
import org.lockss.daemon.status.ColumnDescriptor;
import org.lockss.daemon.status.OverviewAccessor;
import org.lockss.daemon.status.StatusAccessor;
import org.lockss.daemon.status.StatusService.NoSuchTableException;
import org.lockss.daemon.status.StatusTable;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.state.ArchivalUnitStatus;
import org.lockss.util.CatalogueOrderComparator;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;

/**
 * This class is the StatusAccessor for the MetadataManager.
 * It also implements an OverviewAccessor to display on the 
 * main deamon status page.
 * 
 * @author Philip Gust
 * @version 1.0
 *
 */
public class MetadataManagerStatusAccessor implements StatusAccessor {

  final MetadataManager metadataMgr;
  
  private static final String AU_COL_NAME = "au";
  private static final String INDEX_TYPE = "crawl_type";
  private static final String START_TIME_COL_NAME = "start";
  private static final String DURATION_COL_NAME = "dur";
  private static final String INDEX_STATUS_COL_NAME = "status";
  private static final String NUM_INDEXED_COL_NAME = "num_indexed";
  // Sort keys, not visible columns
  private static final String SORT_KEY1 = "sort1";
  private static final String SORT_KEY2 = "sort2";
  
  private static int SORT_BASE_INDEXING = 0;
  private static int SORT_BASE_WAITING = 1000000;
  private static int SORT_BASE_DONE = 2000000;

  final private List<ColumnDescriptor> colDescs =
      ListUtil.fromArray(new ColumnDescriptor[] {
        new ColumnDescriptor(AU_COL_NAME, "Journal Volume",
                             ColumnDescriptor.TYPE_STRING)
        .setComparator(CatalogueOrderComparator.SINGLETON),
        new ColumnDescriptor(INDEX_TYPE, "Index Type",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(START_TIME_COL_NAME, "Start Time",
                             ColumnDescriptor.TYPE_DATE),
        new ColumnDescriptor(DURATION_COL_NAME, "Duration",
                             ColumnDescriptor.TYPE_TIME_INTERVAL),
        new ColumnDescriptor(INDEX_STATUS_COL_NAME, "Status",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(NUM_INDEXED_COL_NAME, "Articles Indexed",
                             ColumnDescriptor.TYPE_INT),
      });

  // ascending by category, descending start or end time
  private final List sortRules =
    ListUtil.list(new StatusTable.SortRule(SORT_KEY1, true),
                  new StatusTable.SortRule(SORT_KEY2, false));

  /**
   * Create new instance for metadata manager
   * @param metadataMgr the metadata manager
   */
  public MetadataManagerStatusAccessor(MetadataManager metadataMgr) {
    this.metadataMgr = metadataMgr;
  }
  
  private List getSummaryInfo() {
    List res = new ArrayList();
    long activeOps = metadataMgr.getActiveReindexingCount();
    long pendingOps = metadataMgr.getPendingAusCount() - activeOps;
    long successfulOps = metadataMgr.getSuccessfulReindexingCount();
    long failedOps = metadataMgr.getFailedReindexingCount();
    long articleCount = metadataMgr.getArticleCount();
    boolean indexingEnabled = metadataMgr.isIndexingEnabled();
    
    res.add(new StatusTable.SummaryInfo(
        "Active Indexing Operations",
        ColumnDescriptor.TYPE_INT,
        activeOps));

    res.add(new StatusTable.SummaryInfo(
        "Pending Indexing Operations",
        ColumnDescriptor.TYPE_INT,
        pendingOps));

    res.add(new StatusTable.SummaryInfo(
        "Successful Indexing Operations",
        ColumnDescriptor.TYPE_INT,
        successfulOps));

    res.add(new StatusTable.SummaryInfo(
        "Failed/Rescheduled Indexing Operations",
        ColumnDescriptor.TYPE_INT,
        failedOps));

    res.add(new StatusTable.SummaryInfo(
        "Total Articles in Index",
        ColumnDescriptor.TYPE_INT,
        articleCount));

    res.add(new StatusTable.SummaryInfo("Indexing Enabled",
                                          ColumnDescriptor.TYPE_STRING,
                                          indexingEnabled));
    return res;
  }

  private List getRows() {
    List<Map<String,Object>> rows = new ArrayList<Map<String,Object>>();
    int rowNum = 0;
    for (ReindexingTask task : metadataMgr.getReindexingTasks()) {
      ArchivalUnit au = task.getAu();
      long startTime = task.getStartTime();
      long endTime = task.getEndTime();
      ReindexingStatus indexStatus = task.getReindexingStatus();
      long numIndexed = task.getIndexedArticleCount();
      boolean isNewAu = task.isNewAu();      
      long curTime = TimeBase.nowMs();
      
      Map<String,Object> row = new HashMap<String,Object>();
      row.put(AU_COL_NAME,
              new StatusTable.Reference(au.getName(),
                                        ArchivalUnitStatus.AU_STATUS_TABLE_NAME,
                                        au.getAuId()));
      if (isNewAu) {
        row.put(INDEX_TYPE, "New Index");
      } else {
        row.put(INDEX_TYPE, "Reindex");
      }
      
      if (startTime == 0) {
        // task hasn't started yet
        row.put(START_TIME_COL_NAME, "");
        row.put(DURATION_COL_NAME, "");
        row.put(INDEX_STATUS_COL_NAME, "Waiting");
        row.put(NUM_INDEXED_COL_NAME, "");
        // invisible keys for sorting
        row.put(SORT_KEY1, SORT_BASE_WAITING);
        row.put(SORT_KEY2, rowNum);
      } else if (endTime == 0) {
        // task is running but hasn't finished yet
        row.put(START_TIME_COL_NAME, startTime);
        row.put(DURATION_COL_NAME, curTime-startTime);
        row.put(INDEX_STATUS_COL_NAME, "Indexing");
        row.put(NUM_INDEXED_COL_NAME, numIndexed);
        // invisible keys for sorting
        row.put(SORT_KEY1, SORT_BASE_INDEXING);
        row.put(SORT_KEY2, startTime);

      } else {
        // task is finished
        row.put(START_TIME_COL_NAME, startTime);
        row.put(DURATION_COL_NAME, endTime-startTime);
        String status;
        switch (indexStatus) {
          case success:
            status = "Success";
            break;
          case failed:
            status = "Failed";
            break;
          case rescheduled:
            status = "Rescheduled";
            break;
          default:
            status = indexStatus.toString();
        }
        row.put(INDEX_STATUS_COL_NAME, status);
        row.put(NUM_INDEXED_COL_NAME, numIndexed);
        // invisible keys for sorting
        row.put(SORT_KEY1, SORT_BASE_DONE);
        row.put(SORT_KEY2, endTime);
      }
      rows.add(row);
      rowNum++;
    }
    
    return rows;
  }

  private List getColDescs() {
    List res = new ArrayList(colDescs.size());
    for (ColumnDescriptor desc : colDescs) {
      res.add(desc);
    }
    return res;
  }

  @Override
  public void populateTable(StatusTable table) throws NoSuchTableException {
    table.setRows(getRows());
    table.setDefaultSortRules(sortRules);
    table.setColumnDescriptors(getColDescs());
    table.setSummaryInfo(getSummaryInfo());
  }

  @Override
  public String getDisplayName() {
    // TODO Auto-generated method stub
    return "Metadata Indexing Status";
  }

  @Override
  public boolean requiresKey() {
    // TODO Auto-generated method stub
    return false;
  }
  
  // Sort into three groups:
  // 1: Active, by descending start time
  // 2: Pending, in queue order
  // 3: Done, by descending end time

  static class IndexingOverview implements OverviewAccessor {
    final MetadataManager metadataMgr;
    
    public IndexingOverview(MetadataManager metadataMgr) {
      this.metadataMgr = metadataMgr;
    }

    @Override
    public Object getOverview(String tableName, BitSet options) {
      List<StatusTable.Reference> res = new ArrayList<StatusTable.Reference>();
      String s;
      if (metadataMgr.isIndexingEnabled()) {
        s = StringUtil.numberOfUnits(
              metadataMgr.getActiveReindexingCount(), 
              "active metadata indexing operation", 
              "active metadata index operations");
      } else {
        s = "Metadata Indexing Disabled";
      }
      res.add(new StatusTable.Reference(s, MetadataManager.METADATA_STATUS_TABLE_NAME));

      return res;
    }
  }


}
