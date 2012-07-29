/*
 * $Id: MetadataManagerStatusAccessor.java,v 1.2 2012-07-29 15:08:47 pgust Exp $
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
import java.util.List;

import org.lockss.daemon.status.ColumnDescriptor;
import org.lockss.daemon.status.OverviewAccessor;
import org.lockss.daemon.status.StatusAccessor;
import org.lockss.daemon.status.StatusService.NoSuchTableException;
import org.lockss.daemon.status.StatusTable;
import org.lockss.util.CatalogueOrderComparator;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;

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
  private static final String END_TIME_COL_NAME = "end";
  private static final String DURATION_COL_NAME = "dur";
  private static final String INDEX_STATUS = "status";
  private static final String NUM_INDEXED = "num_indexed";
  private static final String NUM_ERRORS = "num_errors";

  final private List<ColumnDescriptor> colDescs =
      ListUtil.fromArray(new ColumnDescriptor[] {
        new ColumnDescriptor(AU_COL_NAME, "Journal Volume",
                             ColumnDescriptor.TYPE_STRING)
        .setComparator(CatalogueOrderComparator.SINGLETON),
        new ColumnDescriptor(INDEX_TYPE, "Index Type",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(START_TIME_COL_NAME, "Start Time",
                             ColumnDescriptor.TYPE_DATE),
        new ColumnDescriptor(END_TIME_COL_NAME, "End Time",
                             ColumnDescriptor.TYPE_DATE),
        new ColumnDescriptor(DURATION_COL_NAME, "Duration",
                             ColumnDescriptor.TYPE_TIME_INTERVAL),
        new ColumnDescriptor(INDEX_STATUS, "Status",
                             ColumnDescriptor.TYPE_STRING),
        new ColumnDescriptor(NUM_INDEXED, "Articles Indexed",
                                 ColumnDescriptor.TYPE_INT),
        new ColumnDescriptor(NUM_ERRORS, "Errors",
                             ColumnDescriptor.TYPE_INT,
                             "Number of articles that could not be indexed"),
      });

  private static final String SORT_KEY1 = "sort1";
  private static final String SORT_KEY2 = "sort2";

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
  
  private void addIfNonZero(List res, String head, int val) {
    if (val != 0) {
      res.add(new StatusTable.SummaryInfo(head,
                                          ColumnDescriptor.TYPE_INT,
                                          new Long(val)));
    }
  }

  private List getSummaryInfo() {
    List res = new ArrayList();
    long totalTime = 0;
    int activeOps = metadataMgr.getReindexingTaskCount();
    int pendingOps = 0; // metadataMgr.getPendingTaskCount();
    int successfulOps = 0; // metadataMgr.getSuccessfulTaskCount();
    int failedOps = 0; // metadataMgr.getFailedTaskCount();
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
        "Failed Indexing Operations",
        ColumnDescriptor.TYPE_INT,
        failedOps));

    res.add(new StatusTable.SummaryInfo("Indexing enabled",
                                          ColumnDescriptor.TYPE_STRING,
                                          indexingEnabled));
    return res;
  }

  private List getRows() {
    List rows = new ArrayList();
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
      String s =
          StringUtil.numberOfUnits(metadataMgr.getReindexingTaskCount(), 
              "active metadata indexing operation", "active metadata index operations");
        res.add(new StatusTable.Reference(s, MetadataManager.METADATA_STATUS_TABLE_NAME));

      return res;
    }
  }


}
