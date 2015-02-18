/*
 * $Id$
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

import static org.lockss.metadata.MetadataManager.METADATA_STATUS_TABLE_NAME;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.lockss.daemon.status.OverviewAccessor;
import org.lockss.daemon.status.StatusTable;
import org.lockss.util.StringUtil;


/**
 * This OverviewAccessor sorts the list into three groups: active, by 
 * descending start time; pending, in queue order, and done, by descending 
 * end time.
 * 
 * @author Philip GUst
 *
 */
class MetadataIndexingOverviewAccessor implements OverviewAccessor {
  final MetadataManager metadataMgr;
  
  public MetadataIndexingOverviewAccessor(MetadataManager metadataMgr) {
    this.metadataMgr = metadataMgr;
  }

  @Override
  public Object getOverview(String tableName, BitSet options) {
    List<StatusTable.Reference> res = new ArrayList<StatusTable.Reference>();
    String s;
    if (metadataMgr.isIndexingEnabled()) {
      long activeCount = metadataMgr.getActiveReindexingCount();
      long pendingCount = metadataMgr.getPendingAusCount();
      s =   StringUtil.numberOfUnits(
              activeCount, 
              "active metadata indexing operation", 
              "active metadata index operations") + ", "
          + StringUtil.numberOfUnits(
              pendingCount-activeCount, "pending", "pending");
    } else {
      s = "Metadata Indexing Disabled";
    }
    res.add(new StatusTable.Reference(s, METADATA_STATUS_TABLE_NAME));

    return res;
  }
}

