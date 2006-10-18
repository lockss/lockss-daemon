/*
 * $Id: SingleCrawlStatusAccessor.java,v 1.3 2006-10-18 17:06:30 adriz Exp $
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

import org.lockss.daemon.Crawler.Status.RecordMimeTypeUrls;
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
  throws StatusService.NoSuchTableException{
    if (table == null) {
      throw new IllegalArgumentException("Called with null table");
    } else if (table.getKey() == null) {
      throw new IllegalArgumentException("SingleCrawlStatusAccessor requires a key");
    }
    String key = table.getKey();
    Crawler.Status status;
    try {
      status = statusSource.getStatus().getCrawlStatus(key);
    } catch (Exception e) {
      throw new StatusService.NoSuchTableException("Malformed table key: " +
                                                       key);
    }
    if (status == null) {
      throw new StatusService.NoSuchTableException("Status info from that crawl is no longer available");
    }
    table.setDefaultSortRules(statusSortRules);
    table.setColumnDescriptors(colDescsMimeTypes);  
    table.setTitle(getTableTitle(status));          
    table.setRows(getRows(status, key));              
}

  private String getTableTitle(Crawler.Status status) {
    ArchivalUnit au = status.getAu();
    return "Content types found during crawl of "+au.getName();
  }

  /**  iterate over the mime-types makeRow for each
   */
  private List getRows(Crawler.Status status, String key) {
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

  private Map makeRow(Crawler.Status status, String mimeType, String key) {  
    Map row = new HashMap();
    long numOfUrls =  status.getNumUrlsOfMimeType(mimeType);
    row.put(MIME_TYPE_NAME, mimeType);
    if (status.getUrlsOfMimeType(mimeType) == null) { 
      row.put(MIME_TYPE_NUM_URLS,
              new Long(numOfUrls) );
    }else{     // put a row with a refrence to list of urls
       String urlsRef =  key + "." + MIMETYPES_URLS_KEY +":"+mimeType ; 
       row.put(MIME_TYPE_NUM_URLS,
                 makeRef(numOfUrls,CRAWL_URLS_STATUS_ACCESSOR, urlsRef));
   }

    return row;
  }
  /**
   * Makes the proper reference object if value is != 0, otherwise returns a long
   */
  private Object makeRef(long value, String tableName, String key) {
    return new StatusTable.Reference(new Long(value), tableName, key);
  }

  public String getDisplayName() {
    throw new UnsupportedOperationException("No generic name for MimeTypeStatusCrawler");
  }

  public boolean requiresKey() {
    return true;
  }

}
