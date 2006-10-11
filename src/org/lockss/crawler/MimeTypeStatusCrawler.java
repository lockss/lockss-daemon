/*
 * $Id: MimeTypeStatusCrawler.java,v 1.2 2006-10-11 18:58:11 thib_gc Exp $
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

public class MimeTypeStatusCrawler implements StatusAccessor {

  private static final String IX = "ix";
  private static final String MIME_TYPE_NAME = "mime_type_name";
  private static final String MIME_TYPE_NUM_URLS = "mime_type_num_urls";
  //private static final String MIME_TYPE = "mime_type";
  private static final String MIMETYPES_TABLE_NAME = "mime-type";

  private List colDescsMimeTypes =
    ListUtil.fromArray(new ColumnDescriptor[] {
      new ColumnDescriptor(MIME_TYPE_NAME, "Mime Type Name",
                           ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor(MIME_TYPE_NUM_URLS, "URLs Found",
                           ColumnDescriptor.TYPE_INT,
                           "Number of URLs found with this Mime Type"),
    });

  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule(IX, true));

  private CrawlManager.StatusSource statusSource;

  public MimeTypeStatusCrawler(CrawlManager.StatusSource statusSource) {
    this.statusSource = statusSource;
  }

  public void populateTable(StatusTable table)
  throws StatusService.NoSuchTableException{
    if (table == null) {
      throw new IllegalArgumentException("Called with null table");
    } else if (table.getKey() == null) {
      throw new IllegalArgumentException("SingleCrawlStatus requires a key");
    }
    String key = table.getKey();
    Crawler.Status status;
    //String tableStr;
    try {
      status = statusSource.getStatus().getCrawlStatus(key);
      //no need there is only one tableStr for this class, orig:  tableStr = getTableStrFromKey(key);
    } catch (Exception e) {
      throw new StatusService.NoSuchTableException("Malformed table key: " +
                                                       key);
    }
    if (status == null) {
      throw new StatusService.NoSuchTableException("Status info from that crawl is no longer available");
    }
    table.setDefaultSortRules(statusSortRules);
    table.setColumnDescriptors(colDescsMimeTypes);  //  was     table.setColumnDescriptors(getColDescs(tableStr));
    table.setTitle(getTableTitle(status));          //was table.setTitle(getTableTitle(status, tableStr));
    table.setRows(getRows(status, key));                // was table.setTitle(getTableTitle(status, tableStr));
}

  private String getTableTitle(Crawler.Status status) {
    ArchivalUnit au = status.getAu();
    return "Content Types found during crawl of "+au.getName();
  }

  /*
   *   getRows(Crawler.Status status)
   *   1.  get status +  String key because we are here since its mime-type
   *   2.  iterate over the status-all of the mime-types - mapRecord
   *   3.  values for each mimeType-value, int (recMap.numOfUrls) will call makeRow
   */
  private List getRows(Crawler.Status status, String key) {
    Collection allMimeTypeKeys = status.getMimeTypesVals();
    List rows = new ArrayList();
    if (allMimeTypeKeys != null) {
      String keyMimeType;
      RecordMimeTypeUrls recordUrls;
      int rowNum = 0;
      for (Iterator it = allMimeTypeKeys.iterator(); it.hasNext();) {
        keyMimeType = (String)it.next();
        //recordUrls = (RecordMimeTypeUrls)status.getRecordMimeTypeUrls(keyMimeType);
        rows.add(makeRow(status, keyMimeType, rowNum++, key)); //no need for now for: RecordMimeTypeUrls recordUrls,
        /* old -out eg:
         rows.add(makeRow(crawlStat, ct, rowNum++));
        */
      }
    }
    return rows;
  }

  private Map makeRow(Crawler.Status status, String keyMimeTypeName, int rowNum, String key) {
      //no need for now for: RecordMimeTypeUrls recordUrls, nor for: String key = status.getKey(); statusMap.put(key, status);
    Map row = new HashMap();
    long numOfUrls =  status.getNumUrlsOfMimeType(keyMimeTypeName);
    //ArchivalUnit au = status.getAu();
    row.put(MIME_TYPE_NAME, keyMimeTypeName);
    //ArrayList urls = (ArrayList)status.getUrlsArrayOfMimeType(keyMimeTypeName);
    if ( status.keepMimeTypeUrls() ){
      // put a row with a refrence to get into 3rd status level which will bring a list of urls
      String urlsRef =  keyMimeTypeName +":"+ MIMETYPES_TABLE_NAME +"."+key ; // this key ise the value of the status ix forwarded
      row.put(MIME_TYPE_NUM_URLS,
                makeRef(numOfUrls,
                        "single_crawl_status", urlsRef));
    }else{      //urls.isEmpty()
      row.put(MIME_TYPE_NUM_URLS,
              new Long(status.getNumUrlsOfMimeType(keyMimeTypeName)) );
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
