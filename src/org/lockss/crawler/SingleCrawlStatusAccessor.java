/*
 * $Id: SingleCrawlStatusAccessor.java,v 1.5 2006-11-14 19:21:28 tlipkis Exp $
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
import org.lockss.plugin.*;
import org.lockss.util.*;

public class SingleCrawlStatusAccessor implements StatusAccessor {

  private static final String IX = "ix";
  private static final String MIME_TYPE_NAME = "mime_type_name";
  private static final String MIME_TYPE_NUM_URLS = "mime_type_num_urls";
  private static final String MIMETYPES_URLS_KEY = "mime-type";
  private static final String CRAWL_URLS_STATUS_ACCESSOR =  
                                CrawlManagerImpl.CRAWL_URLS_STATUS_TABLE; 

  private List colDescsMimeTypes =
    ListUtil.fromArray(new ColumnDescriptor[] {
      new ColumnDescriptor(MIME_TYPE_NAME, "Mime Type",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor(MIME_TYPE_NUM_URLS, "URLs Found",
                           ColumnDescriptor.TYPE_INT,
                           "Number of URLs found with this Mime Type"),
    });

  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule(IX, true));

  private CrawlManager.StatusSource statusSource;

  public SingleCrawlStatusAccessor(CrawlManager.StatusSource statusSource) {
    this.statusSource = statusSource;
  }

  
  public void populateTable(StatusTable table)
      throws StatusService.NoSuchTableException {
    if (table == null) {
      throw new IllegalArgumentException("Called with null table");
    } else if (table.getKey() == null) {
      throw new IllegalArgumentException("SingleCrawlStatusAccessor requires a key");
    }
    String key = table.getKey();
    CrawlerStatus status = statusSource.getStatus().getCrawlStatus(key);
    if (status == null) {
      throw new StatusService.NoSuchTableException("Status info from that crawl is no longer available");
    }
    table.setDefaultSortRules(statusSortRules);
    table.setColumnDescriptors(colDescsMimeTypes);  
    table.setTitle(getTableTitle(status));          
    table.setRows(getRows(status, key));              
    table.setSummaryInfo(getSummaryInfo(status));
  }

  private String getTableTitle(CrawlerStatus status) {
    ArchivalUnit au = status.getAu();
    return "Status of crawl of " + au.getName();
  }

  /**  iterate over the mime-types makeRow for each
   */
  private List getRows(CrawlerStatus status, String key) {
    Collection mimeTypes = status.getMimeTypes();
    List rows = new ArrayList();
    if (mimeTypes != null) {
      String mimeType;
      for (Iterator it = mimeTypes.iterator(); it.hasNext();) {
        mimeType = (String)it.next();
        rows.add(makeRow(status, mimeType, key)); 
      }
    }
    return rows;
  }

  private Map makeRow(CrawlerStatus status, String mimeType, String key) {  
    Map row = new HashMap();
    row.put(MIME_TYPE_NAME, mimeType);
    row.put(MIME_TYPE_NUM_URLS,
	    makeRefIfColl(status.getMimeTypeCtr(mimeType), key,
			  MIMETYPES_URLS_KEY +":"+mimeType));
    return row;
  }

  /**
   * Return a reference object to the table, displaying the value
   */
  private Object makeRef(long value, String tableName, String key) {
    return new StatusTable.Reference(new Long(value), tableName, key);
  }

  /**
   * If the UrlCounter has a collection, return a reference to it, else
   * just the count
   */
  Object makeRefIfColl(CrawlerStatus.UrlCount ctr, String crawlKey,
		       String subkey) {
    if (ctr.hasCollection()) {
      return makeRef(ctr.getCount(),
		     CRAWL_URLS_STATUS_ACCESSOR, crawlKey + "." + subkey);
    }
    return new Long(ctr.getCount());
  }

  public String getDisplayName() {
    throw new UnsupportedOperationException("No generic name for MimeTypeStatusCrawler");
  }

  public boolean requiresKey() {
    return true;
  }

  private List getSummaryInfo(CrawlerStatus status) {
    List res = new ArrayList();
    Collection startUrls = status.getStartUrls();
    res.add(new StatusTable.SummaryInfo("Status",
					ColumnDescriptor.TYPE_STRING,
					status.getCrawlStatus()));
    res.add(new StatusTable.SummaryInfo("Source",
					ColumnDescriptor.TYPE_STRING,
					status.getSources()));
    res.add(new StatusTable.SummaryInfo("Starting Url(s)",
					ColumnDescriptor.TYPE_STRING,
					startUrls));
//     res.add(new StatusTable.SummaryInfo("",
// 					ColumnDescriptor.TYPE_STRING,
// 					));


//     addIfNonZero(res, "Active Crawls", ct.active);
    return res;
  }

  private void addIfNonZero(List res, String head, int val) {
    if (val != 0) {
      res.add(new StatusTable.SummaryInfo(head,
					  ColumnDescriptor.TYPE_INT,
					  new Long(val)));
    }
  }


}
