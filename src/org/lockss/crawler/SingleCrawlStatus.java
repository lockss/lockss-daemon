/*
 * $Id: SingleCrawlStatus.java,v 1.1 2005-01-11 01:56:42 troberts Exp $
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

package org.lockss.crawler;
import java.util.*;
import org.lockss.daemon.status.*;
import org.lockss.daemon.*;
// import org.lockss.app.*;
// import org.lockss.plugin.*;
// import org.lockss.plugin.base.*;
import org.lockss.util.*;

public class SingleCrawlStatus implements StatusAccessor {
  private CrawlManagerStatus cmStatus = null;

  private static final String URL = "url";
  private static List colDescs =
    ListUtil.list(new ColumnDescriptor(URL, "URL Fetched",
				       ColumnDescriptor.TYPE_STRING));
    
  public SingleCrawlStatus(CrawlManagerStatus cmStatus) {
    this.cmStatus = cmStatus;
  }

  public void populateTable(StatusTable table) {
    if (table == null) {
      throw new IllegalArgumentException("Called with null table");
    } else if (table.getKey() == null) {
      throw new IllegalArgumentException("SingleCrawlStatus requires a key");
    }
    table.setColumnDescriptors(colDescs);
    String key = table.getKey();
    Crawler.Status status = getStatusObj(key);
    // 	(Crawler.Status)crawlStatusMap.get(table.getKey());
    table.setRows(makeRows(status, key));
  }

  private Crawler.Status getStatusObj(String key) {
    int idx = Integer.parseInt(key.substring(key.indexOf(";")+1));
    return (Crawler.Status)cmStatus.getStatusObject(idx);
  }

  private List makeRows(Crawler.Status status, String key) {
    String tableName = key.substring(0, key.indexOf(";"));
    Set urls = null;
    if ("fetched".equals(tableName)) {
      urls = status.getUrlsFetched();
    } else if ("error".equals(tableName)) {
      urls = status.getUrlsFetched();
    }
    List rows = new ArrayList(urls.size());
    for (Iterator it = urls.iterator(); it.hasNext();) {
      Map row = new HashMap();
      String url = (String)it.next(); 
      row.put(URL, url);   
      rows.add(row);
    }
    return rows;
  }
    
  public String getDisplayName() {
    return "URLs Fetched";
  }

  public boolean requiresKey() {
    return true;
  }
}

