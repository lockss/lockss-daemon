/*
 * $Id: MetadataManagerStatusAccessor.java,v 1.2 2013-03-19 20:20:30 pgust Exp $
 */

/*

 Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lockss.daemon.status.ColumnDescriptor;
import org.lockss.daemon.status.StatusAccessor;
import org.lockss.daemon.status.StatusService.NoSuchTableException;
import org.lockss.daemon.status.StatusTable;
import org.lockss.metadata.MetadataManager.ReindexingStatus;
import org.lockss.state.ArchivalUnitStatus;
import org.lockss.util.CatalogueOrderComparator;
import org.lockss.util.ListUtil;
import org.lockss.util.TimeBase;

/**
 * This class displays the MetadataManager status for the current
 * and most recently run indexing operations.
 * 
 * @author Philip Gust
 * @version 1.0
 *
 */
public class MetadataManagerStatusAccessor implements StatusAccessor {

  final MetadataManager metadataMgr;
  
  private static final String AU_COL_NAME = "au";
  private static final String INDEX_TYPE = "index_type";
  private static final String START_TIME_COL_NAME = "start";
  private static final String INDEX_DURATION_COL_NAME = "index_dur";
  private static final String UPDATE_DURATION_COL_NAME = "update_dur";
  private static final String INDEX_STATUS_COL_NAME = "status";
  private static final String NUM_INDEXED_COL_NAME = "num_indexed";
  private static final String NUM_UPDATED_COL_NAME = "num_updated";
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
                             ColumnDescriptor.TYPE_STRING,
                             "Indicates whether new content is being indexed "
                             + " or existing content index is being updated"),
        new ColumnDescriptor(INDEX_STATUS_COL_NAME, "Status",
                             ColumnDescriptor.TYPE_STRING,
                             "Status of indexing operation."),
        new ColumnDescriptor(START_TIME_COL_NAME, "Start Time",
                             ColumnDescriptor.TYPE_DATE,
                             "Start date and time of indexing operation"),
        new ColumnDescriptor(INDEX_DURATION_COL_NAME, "Index Duration",
                             ColumnDescriptor.TYPE_TIME_INTERVAL,
                             "Duration of metadata indexing, including"
                             + " scanning articles and extracting metadata."),
        new ColumnDescriptor(NUM_INDEXED_COL_NAME, "Articles Indexed",
                             ColumnDescriptor.TYPE_INT),
        new ColumnDescriptor(UPDATE_DURATION_COL_NAME, "Update Duration",
                             ColumnDescriptor.TYPE_TIME_INTERVAL,
                             "Duration of updating stored metadata."),
        new ColumnDescriptor(NUM_UPDATED_COL_NAME, "Articles Updated",
                             ColumnDescriptor.TYPE_INT),
      });

  // ascending by category, descending start or end time
  private final List<StatusTable.SortRule> sortRules =
      (List<StatusTable.SortRule>) ListUtil
	  .list(new StatusTable.SortRule(SORT_KEY1, true),
		new StatusTable.SortRule(SORT_KEY2, false));

  /**
   * Create new instance for metadata manager
   * @param metadataMgr the metadata manager
   */
  public MetadataManagerStatusAccessor(MetadataManager metadataMgr) {
    this.metadataMgr = metadataMgr;
  }
  
  private List<StatusTable.SummaryInfo> getSummaryInfo() {
    List<StatusTable.SummaryInfo> res =
	new ArrayList<StatusTable.SummaryInfo>();
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

    if (failedOps > 0) {
      res.add(new StatusTable.SummaryInfo(
        "Failed/Rescheduled Indexing Operations",
        ColumnDescriptor.TYPE_INT,
        new StatusTable.Reference(failedOps,
            MetadataManager.METADATA_STATUS_ERROR_TABLE_NAME)));
    } else {
      res.add(new StatusTable.SummaryInfo(
          "Failed/Rescheduled Indexing Operations",
          ColumnDescriptor.TYPE_INT,
          failedOps));
    }

    res.add(new StatusTable.SummaryInfo(
        "Total Articles in Index",
        ColumnDescriptor.TYPE_INT,
        articleCount));

    res.add(new StatusTable.SummaryInfo(
        "Indexing Enabled",
        ColumnDescriptor.TYPE_STRING,
        indexingEnabled));
    
    return res;
  }

  List<Map<String,Object>> getRows() {
    return getRows(metadataMgr.getReindexingTasks());
  }
  
  List<Map<String,Object>> getRows(Collection<ReindexingTask> tasks) {
    List<Map<String,Object>> rows = new ArrayList<Map<String,Object>>();
    int rowNum = 0;
    for (ReindexingTask task : tasks) {
      String auName = task.getAuName();
      String auId = task.getAuId();
      boolean auNoSubstance = task.hasNoAuSubstance();
      long startTime = task.getStartTime();
      long startUpdateTime = task.getStartUpdateTime();
      long endTime = task.getEndTime();
      ReindexingStatus indexStatus = task.getReindexingStatus();
      long numIndexed = task.getIndexedArticleCount();
      long numUpdated = task.getUpdatedArticleCount();
      boolean isNewAu = task.isNewAu();      
      long curTime = TimeBase.nowMs();
      
      Map<String,Object> row = new HashMap<String,Object>();
      row.put(AU_COL_NAME,
              new StatusTable.Reference(auName,
                                        ArchivalUnitStatus.AU_STATUS_TABLE_NAME,
                                        auId));
      if (isNewAu) {
        row.put(INDEX_TYPE, "New Index");
      } else {
        row.put(INDEX_TYPE, "Reindex");
      }
      
      if (startTime == 0) {
        // task hasn't started yet
        row.put(INDEX_STATUS_COL_NAME, "Waiting");
        // invisible keys for sorting
        row.put(SORT_KEY1, SORT_BASE_WAITING);
        row.put(SORT_KEY2, rowNum);
      } if (startUpdateTime == 0) {
        // task is running but hasn't finished indexing yet
        row.put(START_TIME_COL_NAME, startTime);
        row.put(INDEX_DURATION_COL_NAME, curTime-startTime);
        row.put(INDEX_STATUS_COL_NAME, "Indexing");
        row.put(NUM_INDEXED_COL_NAME, numIndexed);
        // invisible keys for sorting
        row.put(SORT_KEY1, SORT_BASE_INDEXING);
        row.put(SORT_KEY2, startTime);
      } else if (endTime == 0) {
        // task is finished indexing but hasn't finished updating yet
        row.put(START_TIME_COL_NAME, startTime);
        row.put(INDEX_DURATION_COL_NAME, startUpdateTime-startTime);
        row.put(UPDATE_DURATION_COL_NAME, curTime-startUpdateTime);
        row.put(INDEX_STATUS_COL_NAME, "Updating");
        row.put(NUM_INDEXED_COL_NAME, numIndexed);
        row.put(NUM_UPDATED_COL_NAME, numUpdated);
        // invisible keys for sorting
        row.put(SORT_KEY1, SORT_BASE_INDEXING);
        row.put(SORT_KEY2, startTime);

      } else {
        // task is finished
        row.put(START_TIME_COL_NAME, startTime);
        row.put(INDEX_DURATION_COL_NAME, startUpdateTime-startTime);
        row.put(UPDATE_DURATION_COL_NAME, endTime-startUpdateTime);
        Object status;
        switch (indexStatus) {
          case Success:
            status = "Success";
            break;
          case Failed:
            status = "Failed";
            break;
          case Rescheduled:
            status = "Rescheduled";
            break;
          default:
            status = indexStatus.toString();
        }
        if (auId != null) {
          if ((indexStatus == ReindexingStatus.Success) && auNoSubstance) {
            status =
              new StatusTable.DisplayedValue(status).setFootnote(
                "Though metadata indexing finished successfully, no"
              + " article files containing substantial content were found");
          }
        }
        if (task.getException() != null) {
          row.put(INDEX_STATUS_COL_NAME,
                  new StatusTable.Reference(status,
                      MetadataManager.METADATA_STATUS_ERROR_INFO_TABLE_NAME,
                      Long.toString(task.getStartTime())));
          
        } else {
          row.put(INDEX_STATUS_COL_NAME, status);
        }

        row.put(NUM_INDEXED_COL_NAME, numIndexed);
        row.put(NUM_UPDATED_COL_NAME, numUpdated);

        // invisible keys for sorting
        row.put(SORT_KEY1, SORT_BASE_DONE);
        row.put(SORT_KEY2, endTime);
      }
      rows.add(row);
      rowNum++;
    }
    
    return rows;
  }

  private List<ColumnDescriptor> getColDescs() {
    List<ColumnDescriptor> res =
	new ArrayList<ColumnDescriptor>(colDescs.size());

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
    return "Metadata Indexing Status";
  }

  @Override
  public boolean requiresKey() {
    return false;
  }
  
}
