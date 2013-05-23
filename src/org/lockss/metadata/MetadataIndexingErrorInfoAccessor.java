/*
 * $Id: MetadataIndexingErrorInfoAccessor.java,v 1.3 2013-05-23 12:46:16 pgust Exp $
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.lockss.daemon.status.ColumnDescriptor;
import org.lockss.daemon.status.StatusAccessor;
import org.lockss.daemon.status.StatusService;
import org.lockss.daemon.status.StatusTable;
import org.lockss.daemon.status.StatusService.NoSuchTableException;
import org.lockss.metadata.ArticleMetadataBuffer.ArticleMetadataInfo;
import org.lockss.metadata.MetadataManager.ReindexingStatus;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginStatus;
import org.lockss.state.ArchivalUnitStatus;

/**
 * This class displays the error detail information for a
 * failed reindexing task.
 * 
 * @author Philip Gust
 *
 */
public class MetadataIndexingErrorInfoAccessor implements StatusAccessor {
  final MetadataManager metadataMgr;
  
  public MetadataIndexingErrorInfoAccessor(MetadataManager metadataMgr) {
    this.metadataMgr = metadataMgr;
  }

  @Override
  public void populateTable(StatusTable table) throws NoSuchTableException {
    String key = table.getKey();
    ReindexingTask task = null;
    List<StatusTable.SummaryInfo> res =
        new ArrayList<StatusTable.SummaryInfo>();

    try {
      long taskTime = Long.parseLong(key);
      List<ReindexingTask> tasks = metadataMgr.getFailedReindexingTasks();
      for (ReindexingTask t : tasks) {
        if (taskTime == t.getStartTime()) {
          task = t;
          break;
        }
      }
    } catch (NumberFormatException ex) {
      // fall through
    }
    if ( task == null) {
      throw new StatusService.NoSuchTableException(
          "Error info from that reindexing task no longer available");
    }
    
    res.add(new StatusTable.SummaryInfo(
        "Volume",
        ColumnDescriptor.TYPE_STRING,
        task.getAuName()));
    ArchivalUnit au = task.getAu();
    Plugin plugin = au.getPlugin();
    res.add(new StatusTable.SummaryInfo(
        "Plugin",
        ColumnDescriptor.TYPE_STRING,
        PluginStatus.makePlugRef(plugin.getPluginName(), plugin)));
    res.add(new StatusTable.SummaryInfo(
        "Index Type",
        ColumnDescriptor.TYPE_STRING,
        task.isNewAu() ? "New Index" : "Reindex"));

    // show reindexing status string
    String status;
    ReindexingStatus reindexingStatus = task.getReindexingStatus();
    switch (reindexingStatus) {
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
        status = reindexingStatus.toString();
    }

    res.add(new StatusTable.SummaryInfo(
        "Status",
        ColumnDescriptor.TYPE_STRING,
        status));

    res.add(new StatusTable.SummaryInfo(
        "Has substance",
        ColumnDescriptor.TYPE_STRING,
        task.hasNoAuSubstance() ? "No" : "Yes"));

    res.add(new StatusTable.SummaryInfo(
        "Start time",
        ColumnDescriptor.TYPE_DATE,
        task.getStartTime()));
    
    res.add(new StatusTable.SummaryInfo(
        "Index duration",
        ColumnDescriptor.TYPE_TIME_INTERVAL,
        task.getStartUpdateTime() - task.getStartTime()));
    
    res.add(new StatusTable.SummaryInfo(
        "Articles indexed",
        ColumnDescriptor.TYPE_INT,
        task.getIndexedArticleCount()));

    res.add(new StatusTable.SummaryInfo(
        "Update duration",
        ColumnDescriptor.TYPE_TIME_INTERVAL,
        task.getEndTime() - task.getStartUpdateTime()));
    
    res.add(new StatusTable.SummaryInfo(
        "Articles updated",
        ColumnDescriptor.TYPE_INT,
        task.getUpdatedArticleCount()));
    
    res.add(new StatusTable.SummaryInfo(
        null,
        ColumnDescriptor.TYPE_STRING,
        new StatusTable.Reference("AU configuration", 
            ArchivalUnitStatus.AU_DEFINITION_TABLE_NAME, 
            au.getAuId())));


    Exception taskException = task.getException();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    taskException.printStackTrace(pw);
    res.add(new StatusTable.SummaryInfo(
        "Exception",
        ColumnDescriptor.TYPE_STRING,
        sw.toString()));
    // show metadata info for a MedataException
    if (taskException instanceof MetadataException) {
      ArticleMetadataInfo info = 
          ((MetadataException)taskException).getArticleMetadataInfo();
      if (info != null) {
        res.add(new StatusTable.SummaryInfo(
            "MetadataInfo",
            ColumnDescriptor.TYPE_STRING,
            info.toString()));
      }
    }

    table.setSummaryInfo(res);
  }

  @Override
  public String getDisplayName() {
    return "Metadata Indexing Error Information";
  }

  @Override
  public boolean requiresKey() {
    return true;
  }

}

