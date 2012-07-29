/*
 * $Id: MetadataManagerStatusAccessor.java,v 1.1 2012-07-29 00:49:43 pgust Exp $
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

import org.lockss.daemon.status.OverviewAccessor;
import org.lockss.daemon.status.StatusAccessor;
import org.lockss.daemon.status.StatusService.NoSuchTableException;
import org.lockss.daemon.status.StatusTable;
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
  
  public MetadataManagerStatusAccessor(MetadataManager metadataMgr) {
    this.metadataMgr = metadataMgr;
  }
  
  @Override
  public void populateTable(StatusTable table) throws NoSuchTableException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getDisplayName() {
    // TODO Auto-generated method stub
    return "Metadata Indexing Table";
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
